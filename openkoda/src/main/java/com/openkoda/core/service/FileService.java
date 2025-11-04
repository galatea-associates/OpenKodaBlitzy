/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR
A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.core.service;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.flow.ValidationException;
import com.openkoda.model.file.File;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.*;

import static com.openkoda.core.service.FileService.StorageType.database;
import static com.openkoda.core.service.FileService.StorageType.filesystem;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * Central file persistence and streaming service supporting multiple storage backends.
 * <p>
 * This service provides file management capabilities with flexible storage options including
 * database Blob storage via Hibernate, filesystem storage with failover support, and Amazon S3
 * (placeholder implementation). It handles file uploads, storage, retrieval, and streaming to
 * HTTP responses with appropriate headers and caching controls.

 * <p>
 * Key capabilities include:
 * <ul>
 * <li>Multi-backend storage support (database, filesystem, S3)</li>
 * <li>Configurable maximum upload size with SpEL expression parsing (GB/MB/kB units)</li>
 * <li>Filesystem storage with primary and failover paths for resilience</li>
 * <li>In-memory image scaling with proportional resizing</li>
 * <li>Resilient I/O operations with 10-second timeout protection via {@link CompletableFuture}</li>
 * <li>HTTP streaming with Content-Disposition, Content-Type, and caching headers</li>
 * </ul>

 * <p>
 * The service extends {@link ComponentProvider} for access to repositories and other services.
 * Storage configuration is controlled via Spring properties, with automatic failover for
 * filesystem operations when the failover path is writable.

 * <p>
 * Example usage:
 * <pre>{@code
 * File f = fileService.saveAndPrepareFileEntity(orgId, UUID.randomUUID().toString(), "doc.pdf", bytes);
 * repositories.unsecure.file.save(f);
 * }</pre>

 * <p>
 * <strong>Note:</strong> The Amazon S3 storage type is not fully implemented and should be
 * considered a placeholder. The {@code tryIOOperation} timeout is hardcoded to 10 seconds,
 * which may be insufficient for large files over slow I/O systems.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see File
 * @see StorageType
 * @see BlobProxy
 * @see MultipartFile
 * @see CompletableFuture
 */
@Service("file")
public class FileService extends ComponentProvider {

    /**
     * Enumeration of supported storage backend types.
     * <p>
     * Defines the available storage mechanisms for file persistence:
     * <ul>
     * <li>{@code database} - Files stored as Hibernate Blob in database tables</li>
     * <li>{@code filesystem} - Files stored as disk files with optional failover path</li>
     * <li>{@code amazon} - Amazon S3 storage (placeholder, not fully implemented)</li>
     * </ul>

     * <p>
     * The storage type is configured via the {@code file.storage.type} property with
     * {@code database} as the default.

     *
     * @since 1.7.1
     */
    public enum StorageType {
        /** Database Blob storage via Hibernate BlobProxy */
        database,
        /** Filesystem storage with primary and failover paths */
        filesystem,
        /** Amazon S3 storage (not fully implemented) */
        amazon
    }

    /**
     * Raw Spring property expression for maximum upload file size.
     * <p>
     * This expression string is parsed during initialization to support human-readable
     * size units (GB, MB, kB). The expression is transformed by replacing unit suffixes
     * with multiplication factors before evaluation via SpEL.

     *
     * @see #init()
     * @see #maxUploadSizeInBytes
     */
    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxUploadSizeInBytesExpression;

    /**
     * Parsed maximum upload size in bytes.
     * <p>
     * This value is computed from {@link #maxUploadSizeInBytesExpression} during the
     * {@link #init()} lifecycle callback by evaluating the SpEL expression after replacing
     * GB/MB/kB tokens with their numeric equivalents.

     *
     * @see #getMaxUploadSizeInBytes()
     */
    private Long maxUploadSizeInBytes;

    /**
     * Configured storage backend type.
     * <p>
     * Determines which storage mechanism is used for file persistence. Defaults to
     * {@link StorageType#database} if not explicitly configured. Valid values are
     * {@code database}, {@code filesystem}, or {@code amazon}.

     *
     * @see StorageType
     * @see #getStorageType()
     */
    @Value("${file.storage.type:database}")
    StorageType storageType;

    /**
     * Primary filesystem directory path for file storage.
     * <p>
     * When {@link #storageType} is set to {@link StorageType#filesystem}, files are
     * persisted to this directory. The path is concatenated with organization ID,
     * UUID, and filename to construct unique file paths. Defaults to {@code /tmp}
     * if not configured.

     *
     * @see #prepareStoredFileName(Long, String, String)
     * @see #failoverStorageFilesystemPath
     */
    @Value("${file.storage.filesystem.path:/tmp}")
    private String storageFilesystemPath;

    /**
     * Secondary failover filesystem directory path.
     * <p>
     * This path serves as a fallback location when I/O operations to the primary
     * {@link #storageFilesystemPath} fail or timeout. Failover is only attempted
     * when {@link #writableFailoverStoreageFilesystem} is enabled. Defaults to
     * {@code /tmp2} if not configured.

     *
     * @see #writableFailoverStoreageFilesystem
     * @see #handleFilesystemWrite
     * @see #handleFilesystemRead
     */
    @Value("${file.storage.filesystem.failover:/tmp2}")
    private String failoverStorageFilesystemPath;

    /**
     * Flag enabling failover writes to secondary filesystem path.
     * <p>
     * When {@code true}, filesystem write operations that fail on the primary path
     * automatically retry using {@link #failoverStorageFilesystemPath}. Defaults to
     * {@code false}, meaning failover is disabled and write failures are logged
     * without retry.

     *
     * @see #handleFilesystemWrite
     */
    @Value("${file.storage.filesystem.failover.writable:false}")
    private boolean writableFailoverStoreageFilesystem;

    /**
     * Amazon S3 bucket name for S3 storage.
     * <p>
     * Specifies the target S3 bucket when {@link #storageType} is set to
     * {@link StorageType#amazon}. Defaults to {@code bucket-name}. Note that S3
     * storage is not fully implemented and should be considered a placeholder.

     *
     * @see #prepareFileNameForS3Storage(Long, String, String)
     */
    @Value("${file.storage.amazon.bucket:bucket-name}")
    private String storageAmazonBucket;

    /**
     * Presigned URL expiry time in seconds for S3 operations.
     * <p>
     * Determines the validity period for generated S3 presigned URLs. Defaults to
     * 10 seconds if not configured. This setting is only relevant when using
     * {@link StorageType#amazon}, which is currently not fully implemented.

     */
    @Value("${file.storage.amazon.presigned-url.expiry.time.seconds:10}")
    private long amazonPresignedUrlExpiryTimeInSeconds;

    /**
     * Lifecycle callback to parse maximum upload size expression.
     * <p>
     * This method executes after dependency injection completes. It transforms the
     * {@link #maxUploadSizeInBytesExpression} by replacing human-readable size units
     * (GB, MB, kB) with their multiplication factors, then evaluates the resulting
     * SpEL expression to compute {@link #maxUploadSizeInBytes}.

     * <p>
     * Transformation examples:
     * <ul>
     * <li>{@code "5MB"} becomes {@code "5 * 1024 * 1024"}</li>
     * <li>{@code "2GB"} becomes {@code "2 * 1024 * 1024 * 1024"}</li>
     * </ul>

     *
     * @since 1.7.1
     * @see #maxUploadSizeInBytesExpression
     * @see #maxUploadSizeInBytes
     */
    @PostConstruct
    private void init() {
        String fixedExpression = maxUploadSizeInBytesExpression
                .replace("GB", " * 1024 * 1024 * 1024")
                .replace("MB", " * 1024 * 1024")
                .replace("kB", " * 1024");
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(fixedExpression);
        maxUploadSizeInBytes = exp.getValue(Long.class);
    }

    /**
     * Returns the parsed maximum upload size in bytes.
     * <p>
     * This value is computed during initialization from the Spring property
     * {@code spring.servlet.multipart.max-file-size} after parsing size units.

     *
     * @return maximum upload size in bytes
     * @since 1.7.1
     * @see #init()
     */
    public Long getMaxUploadSizeInBytes() {
        return maxUploadSizeInBytes;
    }

    /**
     * Returns the configured storage backend type.
     * <p>
     * The storage type determines whether files are persisted to database Blob,
     * filesystem, or Amazon S3 (placeholder). This value is set via the Spring
     * property {@code file.storage.type} with {@code database} as the default.

     *
     * @return the configured {@link StorageType}
     * @since 1.7.1
     * @see StorageType
     */
    public StorageType getStorageType() {
        return storageType;
    }

    /**
     * Constructs a filesystem path for storing a file.
     * <p>
     * Generates a unique filesystem path by concatenating {@link #storageFilesystemPath}
     * with a pattern combining organization ID, UUID, and filename. This ensures file
     * uniqueness across organizations and prevents naming collisions.

     * <p>
     * Path pattern: {@code <storageFilesystemPath>/<orgId>-<uuid>-<fileName>}

     * <p>
     * If {@code orgId} is {@code null}, it defaults to {@code 0} to maintain consistent
     * path structure.

     *
     * @param orgId organization identifier, or {@code null} for default (0)
     * @param uuid unique identifier for the file (typically from UUID.randomUUID())
     * @param fileName original filename including extension
     * @return full filesystem path for storing the file
     * @since 1.7.1
     * @see #storageFilesystemPath
     * @see #handleFilesystemWrite
     */
    public String prepareStoredFileName(Long orgId, String uuid, String fileName) {
        debug("[prepareStoredFileName]");
        String name = (orgId == null ? 0L : orgId) + "-" + uuid + "-" + fileName;
        return FilenameUtils.concat(storageFilesystemPath, name);
    }

    /**
     * Constructs an S3 key for hierarchical bucket organization.
     * <p>
     * Generates an S3 object key with organization-based folder structure. This method
     * supports the Amazon S3 storage backend (currently not fully implemented).

     * <p>
     * Key pattern: {@code <orgId>/<uuid>-<fileName>}

     * <p>
     * The organization-based prefix allows bucket-level access control and logical
     * grouping of files. If {@code orgId} is {@code null}, it defaults to {@code 0}.

     *
     * @param orgId organization identifier, or {@code null} for default (0)
     * @param uuid unique identifier for the file
     * @param fileName original filename including extension
     * @return S3 object key with hierarchical structure
     * @since 1.7.1
     * @see #storageAmazonBucket
     * @see StorageType#amazon
     */
    public String prepareFileNameForS3Storage(Long orgId, String uuid, String fileName) {
        debug("[prepareFileNameForS3Storage]");
        return (orgId == null ? 0L : orgId) + "/" + uuid + "-" + fileName;
    }

    /**
     * Scales an image to the specified width with proportional height.
     * <p>
     * Performs in-memory image resizing using {@link Image#SCALE_SMOOTH} for quality.
     * The method reads the image from the provided {@link File} entity, validates dimensions,
     * scales proportionally, and returns a new {@link File} entity with scaled content wrapped
     * in a {@link MockMultipartFile}.

     * <p>
     * Key operations:
     * <ul>
     * <li>Validates input is an image type via {@link File#isImage()}</li>
     * <li>Reads {@link BufferedImage} from file content stream with automatic failover retry</li>
     * <li>Validates target width does not exceed original width</li>
     * <li>Calculates proportional height maintaining aspect ratio</li>
     * <li>Uses {@link FastByteArrayOutputStream} to avoid temporary disk files (Docker-safe)</li>
     * <li>Infers output format from file extension</li>
     * <li>Appends {@code -<width>} suffix to filename</li>
     * </ul>

     * <p>
     * If the initial read fails, the method automatically retries using the failover path by
     * replacing {@link #storageFilesystemPath} with {@link #failoverStorageFilesystemPath}.

     * <p>
     * Example:
     * <pre>{@code
     * File scaled = fileService.scaleImage(originalImage, 800);
     * }</pre>

     *
     * @param in the source {@link File} entity containing the original image
     * @param width target width in pixels for the scaled image
     * @return new {@link File} entity with scaled image content, or {@code null} if failover read fails
     * @throws ValidationException if {@code in} is {@code null}, not an image, or target width exceeds original
     * @throws RuntimeException wrapping {@link IOException} or {@link SQLException} from I/O operations
     * @since 1.7.1
     * @see #saveAndPrepareFileEntity(Long, String, String, long, String, InputStream)
     * @see #tryInputOutput(ThrowableSupplier)
     * @see MockMultipartFile
     */
    public File scaleImage(File in, int width) {
        debug("[scaleImage]");
        if (in == null || !in.isImage()) {
            throw new ValidationException("Null or not an image");
        }
        try {
            BufferedImage bi = tryInputOutput(() -> ImageIO.read(in.getContentStream()));
            if (bi == null) {
                error("[saveAndPrepareFileEntity] Error while attempting to read and scale image [{}] ", in.toAuditString());
                String failoverPath = in.getFilesystemPath().replaceFirst(storageFilesystemPath, failoverStorageFilesystemPath);
                bi = tryInputOutput(() -> ImageIO.read(new FileInputStream(failoverPath)));
                if (bi == null) {
                    error("[saveAndPrepareFileEntity] Error while attempting failover to read and scale image [{}] ", in.toAuditString());
                    return null;
                }
            }
            
            int originalWidth = bi.getWidth();
            int originalHeight = bi.getHeight();
            if (width > originalWidth) {
                throw new ValidationException("Failed. Image's width is less than " + width + ", so there is no point to scale.");
            }
            int height = (int) ((((double) originalHeight) / originalWidth) * width);

            Image resultImage = bi.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            BufferedImage buffered = new BufferedImage(width, height, TYPE_INT_RGB);
            buffered.getGraphics().drawImage(resultImage, 0, 0, null);
            
            //create in memory only input/outptu streams to avoid dangling unused temporary images in containerized (docker) context
            FastByteArrayOutputStream os = new FastByteArrayOutputStream(buffered.getData().getDataBuffer().getSize() / 8);
            String ext = in.getFilename().substring(in.getFilename().lastIndexOf('.') + 1).toLowerCase();
            ImageIO.write(buffered, ext, os);

            long fileSize = os.size();
            int lastDotIndex = in.getFilename().lastIndexOf(".");
            String filename = in.getFilename().substring(0, lastDotIndex) + "-" + width + in.getFilename().substring(lastDotIndex);
            MultipartFile multipartFile = new MockMultipartFile(filename, os.toByteArrayUnsafe());
            return saveAndPrepareFileEntity(in.getOrganizationId(), null, filename, fileSize, filename, multipartFile.getInputStream());
            
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convenience overload for saving file from byte array.
     * <p>
     * Converts the byte array to a {@link ByteArrayInputStream} and delegates to the
     * main {@link #saveAndPrepareFileEntity(Long, String, String, long, String, InputStream)}
     * method. The file size is automatically determined from the array length.

     *
     * @param orgId organization identifier for multi-tenant file isolation
     * @param uuid unique identifier for the file
     * @param fileName storage filename (used for filesystem path construction)
     * @param originalFilename original filename from upload (preserved in File entity)
     * @param input file content as byte array
     * @return prepared {@link File} entity (not persisted - caller must save to repository)
     * @throws IOException if I/O operation fails during filesystem write
     * @throws SQLException if database Blob creation fails
     * @since 1.7.1
     * @see #saveAndPrepareFileEntity(Long, String, String, long, String, InputStream)
     */
    public File saveAndPrepareFileEntity(Long orgId, String uuid, String fileName, String originalFilename, byte[] input) throws IOException, SQLException {
        return saveAndPrepareFileEntity(orgId, uuid, fileName, Long.valueOf(input.length), originalFilename, new ByteArrayInputStream(input));
    }

    /**
     * Core file persistence method supporting multiple storage backends.
     * <p>
     * Saves file content to the configured {@link #storageType} backend and prepares a
     * {@link File} entity with metadata. The method probes MIME type from the original
     * filename and delegates to backend-specific handlers.

     * <p>
     * Backend behaviors:
     * <ul>
     * <li>{@link StorageType#filesystem} - Delegates to {@link #handleFilesystemWrite}
     *     with primary path and optional failover</li>
     * <li>{@link StorageType#database} - Creates Hibernate {@link BlobProxy} with specified
     *     length and stores in File entity content field</li>
     * <li>{@link StorageType#amazon} - Not implemented (no handler)</li>
     * </ul>

     * <p>
     * <strong>Important:</strong> This method returns an unpersisted {@link File} entity.
     * The caller must save it to the repository to persist metadata.

     * <p>
     * Example:
     * <pre>{@code
     * File f = fileService.saveAndPrepareFileEntity(orgId, UUID.randomUUID().toString(), "doc.pdf", bytes);
     * repositories.unsecure.file.save(f);
     * }</pre>

     *
     * @param orgId organization identifier for multi-tenant file isolation
     * @param uuid unique identifier for the file
     * @param fileName storage filename (used for filesystem path or S3 key construction)
     * @param totalFileSize file size in bytes (required for BlobProxy length parameter)
     * @param originalFilename original filename from upload (used for MIME type detection)
     * @param inputStream file content stream (consumed during save operation)
     * @return prepared {@link File} entity (not persisted - caller must save to repository)
     * @throws IOException if I/O operation fails during filesystem write or stream reading
     * @throws SQLException if database Blob creation fails
     * @since 1.7.1
     * @see #handleFilesystemWrite
     * @see #storageType
     * @see File
     * @see BlobProxy#generateProxy(InputStream, long)
     */
    public File saveAndPrepareFileEntity(Long orgId, String uuid, String fileName, long totalFileSize, String originalFilename, InputStream inputStream) throws IOException, SQLException {
        debug("[saveAndPrepareFileEntity]");
        File f = null;
        Path path = new java.io.File(originalFilename).toPath();
        String mimeType = Files.probeContentType(path);
        StorageType actualStorageType = getStorageType();
        if (actualStorageType == filesystem) {
            f = handleFilesystemWrite(orgId, uuid, fileName, totalFileSize, originalFilename, inputStream, f, mimeType,
                    actualStorageType);
        } else if (actualStorageType == database) {
            Blob b = BlobProxy.generateProxy(inputStream, totalFileSize);
            f = new File(orgId, originalFilename, mimeType, totalFileSize, uuid, actualStorageType);
            f.setContent(b);
        }
        return f;
    }

    /**
     * Private handler for filesystem-based file writes with failover support.
     * <p>
     * Constructs the target filesystem path using {@link #prepareStoredFileName}, then
     * attempts to copy the input stream to a {@link FileOutputStream} using the resilient
     * {@link #tryIOOperation} method with 10-second timeout protection.

     * <p>
     * Failover behavior:
     * <ul>
     * <li>If the primary write fails or times out, logs an error</li>
     * <li>If {@link #writableFailoverStoreageFilesystem} is {@code true}, retries write
     *     to failover path by replacing {@link #storageFilesystemPath} with
     *     {@link #failoverStorageFilesystemPath}</li>
     * <li>Returns {@code null} if both primary and failover writes fail</li>
     * </ul>

     * <p>
     * On successful write, creates a {@link File} entity with the filesystem path reference
     * stored in the {@code filesystemPath} field.

     *
     * @param orgId organization identifier for path construction
     * @param uuid unique identifier for path construction
     * @param fileName storage filename for path construction
     * @param totalFileSize file size in bytes for File entity metadata
     * @param originalFilename original filename for File entity metadata
     * @param inputStream file content stream to copy
     * @param f currently unused parameter (legacy signature)
     * @param mimeType MIME type for File entity metadata
     * @param actualStorageType storage type for File entity metadata
     * @return prepared {@link File} entity with filesystem path, or {@code null} if write failed
     * @since 1.7.1
     * @see #prepareStoredFileName(Long, String, String)
     * @see #tryIOOperation(ThrowableRunnable)
     * @see #writableFailoverStoreageFilesystem
     * @see #failoverStorageFilesystemPath
     */
    private File handleFilesystemWrite(Long orgId, String uuid, String fileName, long totalFileSize,
            String originalFilename, InputStream inputStream, File f, String mimeType, StorageType actualStorageType) {
        String targetFileName = prepareStoredFileName(orgId, uuid, fileName);
        var ioResult = tryIOOperation(() -> {
            try (FileOutputStream fileOnDisk = new FileOutputStream(targetFileName)) {
                IOUtils.copy(inputStream, fileOnDisk);
            }
        });
                
        if(!ioResult) {
            error("[saveAndPrepareFileEntity] Error while attempting to write [{}, {}, {}] [{}]", orgId, uuid, fileName, targetFileName);
            
            if(writableFailoverStoreageFilesystem) {
                String failoverPath = targetFileName.replaceFirst(storageFilesystemPath, failoverStorageFilesystemPath);
                ioResult = tryIOOperation(() -> {
                    try (FileOutputStream fileOnDisk = new FileOutputStream(failoverPath)) {
                        IOUtils.copy(inputStream, fileOnDisk);
                    }
                });
                
                if(!ioResult) {
                    error("[saveAndPrepareFileEntity] Error while attempting to write to failover path [{}, {}, {}] [{}]}", orgId, uuid, fileName, targetFileName);
                }
            }
        }
        
        if(ioResult) {
            f = new File(orgId, originalFilename, mimeType, totalFileSize, uuid, actualStorageType, targetFileName);
        }
        return f;
    }
    
    /**
     * Streams file content to HTTP response with appropriate headers.
     * <p>
     * Prepares an {@link HttpServletResponse} with file content and metadata headers including
     * Content-Disposition (attachment vs inline), Content-Type, Content-Length, Last-Modified,
     * and Cache-Control. The method handles both database Blob and filesystem storage backends
     * with automatic failover support for filesystem reads.

     * <p>
     * Response headers set:
     * <ul>
     * <li>{@code Content-Type} - MIME type from File entity</li>
     * <li>{@code Content-Length} - File size in bytes</li>
     * <li>{@code Content-Disposition} - "attachment" if download=true, otherwise inline</li>
     * <li>{@code Last-Modified} - File updated timestamp or current time</li>
     * <li>{@code Accept-Ranges} - "bytes" for filesystem/database backends</li>
     * <li>{@code Cache-Control} - "max-age=604800, public" if allowed, otherwise "no-store, no-cache"</li>
     * </ul>

     * <p>
     * For filesystem storage, delegates to {@link #handleFilesystemRead} which provides
     * automatic failover to {@link #failoverStorageFilesystemPath} on read failures.

     * <p>
     * <strong>Note:</strong> This method is public for test access. Test classes use this
     * directly to avoid issues with setting {@link #storageType} in test contexts.

     *
     * @param f the {@link File} entity containing content and metadata
     * @param download if {@code true}, sets Content-Disposition to "attachment" forcing download
     * @param allowCache if {@code true}, enables HTTP caching with max-age=604800 (1 week)
     * @param response the {@link HttpServletResponse} to populate with content and headers
     * @return the modified {@link HttpServletResponse} with headers set and content streamed
     * @throws IOException if I/O operation fails during filesystem read or stream copy
     * @throws SQLException if database Blob read fails
     * @since 1.7.1
     * @see #handleFilesystemRead(File, OutputStream)
     * @see File#getContentStream()
     */
    public HttpServletResponse getFileContentAndPrepareResponse(File f, boolean download, boolean allowCache, HttpServletResponse response) throws IOException, SQLException {
        debug("[getFileContentAndPrepareResponse] fileId: {}", f.getId());
        response.addHeader("Content-Type", f.getContentType());
        response.addHeader("Content-Length", Long.toString(f.getSize()));
        if (download) response.addHeader("Content-Disposition", "attachment; filename=\"" + f.getFilename() + "\"");
        LocalDateTime updatedOn = f.getUpdatedOn() == null ? LocalDateTime.now() : f.getUpdatedOn();
        response.addDateHeader("Last-Modified", updatedOn.toEpochSecond(ZoneOffset.UTC) * 1000);
        OutputStream os = response.getOutputStream();
        StorageType storageType = f.getStorageType();
        if (storageType == filesystem || storageType == database) {
            response.addHeader("Accept-Ranges", "bytes");
            response.addHeader("Cache-Control", allowCache ? "max-age=604800, public" : "no-store, no-cache, must-revalidate");
        }
        
        if (storageType == filesystem) {
            handleFilesystemRead(f, os);
        } else if(storageType == database) {
            try (InputStream is = f.getContentStream()) {
                IOUtils.copy(is, os);
            }   
        }
              
        os.flush();
        return response;
    }

    /**
     * Private handler for filesystem-based file reads with failover support.
     * <p>
     * Attempts to read file content from the filesystem path stored in the {@link File}
     * entity and copy it to the provided {@link OutputStream} using the resilient
     * {@link #tryIOOperation} method with 10-second timeout protection.

     * <p>
     * Failover behavior:
     * <ul>
     * <li>If the primary read fails or times out, logs an error</li>
     * <li>Automatically retries read from failover path by replacing
     *     {@link #storageFilesystemPath} with {@link #failoverStorageFilesystemPath}
     *     in the file's filesystem path</li>
     * <li>Logs error if both primary and failover reads fail</li>
     * </ul>

     * <p>
     * This method is used by {@link #getFileContentAndPrepareResponse} to stream file
     * content to HTTP responses.

     *
     * @param f the {@link File} entity containing the filesystem path reference
     * @param os the {@link OutputStream} to which file content is copied
     * @since 1.7.1
     * @see #tryIOOperation(ThrowableRunnable)
     * @see #failoverStorageFilesystemPath
     * @see File#getContentStream()
     */
    private void handleFilesystemRead(File f, OutputStream os) {
         var ioResult = tryIOOperation( () -> {
            try (InputStream is = f.getContentStream()) {
                IOUtils.copy(is, os);
            }
        });
        
        if(!ioResult) {
            error("[saveAndPrepareFileEntity] Error while attempting to read [{}] ", f.toAuditString());
            String failoverPath = f.getFilesystemPath().replaceFirst(storageFilesystemPath, failoverStorageFilesystemPath);
            ioResult = tryIOOperation( () -> {
                try (InputStream failoverStream = new FileInputStream(failoverPath)) {
                    IOUtils.copy(failoverStream, os);
                }
            });
            
            if(!ioResult) {
                error("[saveAndPrepareFileEntity] Error while attempting failover to read [{}] ", f.toAuditString());
            }
        }
    }
    
    /**
     * Functional interface for I/O operations that may throw checked exceptions.
     * <p>
     * Used by {@link #tryIOOperation(ThrowableRunnable)} to wrap I/O actions with
     * timeout protection via {@link CompletableFuture}. This interface allows lambdas
     * that throw {@link IOException}, {@link SQLException}, or {@link RuntimeException}
     * without explicit exception handling.

     * <p>
     * Example:
     * <pre>{@code
     * boolean success = tryIOOperation(() -> {
     *     try (FileOutputStream fos = new FileOutputStream(path)) {
     *         IOUtils.copy(inputStream, fos);
     *     }
     * });
     * }</pre>

     *
     * @since 1.7.1
     * @see #tryIOOperation(ThrowableRunnable)
     */
    private interface ThrowableRunnable {
        /**
         * Executes the I/O operation.
         *
         * @throws IOException if I/O operation fails
         * @throws SQLException if database operation fails
         * @throws RuntimeException for other failures
         */
        public abstract void run() throws IOException, SQLException, RuntimeException;
    }

    /**
     * Functional interface for I/O suppliers that return typed values and may throw checked exceptions.
     * <p>
     * Used by {@link #tryInputOutput(ThrowableSupplier)} to wrap I/O operations that produce
     * results with timeout protection via {@link CompletableFuture}. This interface allows lambdas
     * that throw {@link IOException}, {@link SQLException}, or {@link RuntimeException} without
     * explicit exception handling.

     * <p>
     * Example:
     * <pre>{@code
     * BufferedImage image = tryInputOutput(() -> ImageIO.read(file.getContentStream()));
     * }</pre>

     *
     * @param <E> the type of result produced by this supplier
     * @since 1.7.1
     * @see #tryInputOutput(ThrowableSupplier)
     */
    private interface ThrowableSupplier<E> {
        /**
         * Executes the I/O operation and returns the result.
         *
         * @return the result of the I/O operation
         * @throws IOException if I/O operation fails
         * @throws SQLException if database operation fails
         * @throws RuntimeException for other failures
         */
        public abstract E get() throws IOException, SQLException, RuntimeException;
    }
    
    /**
     * Protected resilient I/O executor with timeout protection.
     * <p>
     * Executes the provided I/O action asynchronously via {@link CompletableFuture} with a
     * hardcoded 10-second timeout. This prevents indefinite blocking when system I/O operations
     * hang due to network issues, disk failures, or resource exhaustion.

     * <p>
     * Operation behavior:
     * <ul>
     * <li>Returns {@code true} if action completes successfully within 10 seconds</li>
     * <li>Returns {@code false} if {@link IOException} occurs during execution</li>
     * <li>Throws {@link RuntimeException} wrapping {@link InterruptedException},
     *     {@link ExecutionException}, or {@link TimeoutException} on timeout or async failure</li>
     * </ul>

     * <p>
     * <strong>Note:</strong> The 10-second timeout is hardcoded and may be insufficient
     * for large files over slow I/O systems.

     *
     * @param action the I/O operation to execute
     * @return {@code true} if operation succeeded, {@code false} if IOException occurred
     * @throws RuntimeException wrapping timeout or async execution exceptions
     * @since 1.7.1
     * @see CompletableFuture#supplyAsync
     * @see #handleFilesystemWrite
     * @see #handleFilesystemRead
     */
    protected boolean tryIOOperation(ThrowableRunnable action) {
        boolean result = false;
        Future<Boolean> future = CompletableFuture.supplyAsync( () -> {
            try {
                action.run();
                return true;
            } catch (IOException iexc) {
                error("[tryIOOperation] IO Error {}", iexc.toString());
                return false;
            } catch (SQLException | RuntimeException e) {
                // can't happen for fs storage
                return false;
            }
        });
        
        try {
            // actual system I/O timeout may occur after several seconds or even can cause Java thread be indefinitely blocked 
            result = future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            error("[tryInputOutput] {}", e);
            throw new RuntimeException(e);
        }
        
        return result;
    }
    
    /**
     * Protected resilient I/O supplier with timeout protection.
     * <p>
     * Executes the provided I/O supplier asynchronously via {@link CompletableFuture} with a
     * hardcoded 10-second timeout, returning the typed result. This prevents indefinite blocking
     * when system I/O operations hang due to network issues, disk failures, or resource exhaustion.

     * <p>
     * Operation behavior:
     * <ul>
     * <li>Returns the supplier's result if execution completes successfully within 10 seconds</li>
     * <li>Returns {@code null} if {@link IOException} occurs during execution</li>
     * <li>Throws {@link RuntimeException} wrapping {@link InterruptedException},
     *     {@link ExecutionException}, or {@link TimeoutException} on timeout or async failure</li>
     * </ul>

     * <p>
     * <strong>Note:</strong> The 10-second timeout is hardcoded and may be insufficient
     * for large files over slow I/O systems. Null return indicates I/O failure, not a
     * valid null result from the supplier.

     *
     * @param <E> the type of result produced by the supplier
     * @param supplier the I/O operation that produces a typed result
     * @return the result from the supplier, or {@code null} if IOException occurred
     * @throws RuntimeException wrapping timeout or async execution exceptions
     * @since 1.7.1
     * @see CompletableFuture#supplyAsync
     * @see #scaleImage(File, int)
     */
    protected <E> E  tryInputOutput(ThrowableSupplier<E> supplier) {
        E result = null;
        Future<E> future = CompletableFuture.supplyAsync( () -> {
            try {
                return supplier.get();
            } catch (IOException iexc) {
                error("[tryIOOperation] IO Error {}", iexc.toString());
                return null;
            } catch (SQLException | RuntimeException e) {
                // can't happen for fs storage
                return null;
            }
        });
        
        try {
            // actual system I/O timeout may occur after several seconds or even can cause Java thread be indefinitely blocked 
            result = future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            error("[tryInputOutput] {}", e);
            throw new RuntimeException(e);
        }
        
        return result;
    }
    
    /**
     * Minimal {@link MultipartFile} implementation wrapping byte array content.
     * <p>
     * This inner class provides a lightweight in-memory implementation of Spring's
     * {@link MultipartFile} interface for internal use. It wraps byte array content
     * without requiring actual HTTP multipart form data.

     * <p>
     * Primary use case is in {@link #scaleImage(File, int)} where scaled image content
     * needs to be wrapped as a MultipartFile for passing to
     * {@link #saveAndPrepareFileEntity(Long, String, String, long, String, InputStream)}.

     * <p>
     * All required interface methods are implemented. The {@link #transferTo(java.io.File)}
     * method copies content to the destination file using {@link FileCopyUtils}.

     *
     * @since 1.7.1
     * @see MultipartFile
     * @see #scaleImage(File, int)
     */
    private class MockMultipartFile implements MultipartFile {

        /** Form field name for this file */
        private final String name;

        /** Original filename from upload */
        private final String originalFilename;

        /** MIME content type, may be null */
        @Nullable
        private final String contentType;

        /** File content as byte array */
        private final byte[] content;

        /**
         * Constructs a MockMultipartFile with name and content.
         * <p>
         * Original filename defaults to empty string, content type to null.

         *
         * @param name form field name (must not be empty)
         * @param content file content, or {@code null} for empty array
         */
        public MockMultipartFile(String name, @Nullable byte[] content) {
            this(name, "", null, content);
        }

        /**
         * Constructs a MockMultipartFile from an input stream.
         * <p>
         * Reads the entire stream into memory using {@link FileCopyUtils#copyToByteArray}.
         * Original filename defaults to empty string, content type to null.

         *
         * @param name form field name (must not be empty)
         * @param contentStream input stream to read (fully consumed)
         * @throws IOException if stream reading fails
         */
        public MockMultipartFile(String name, InputStream contentStream) throws IOException {
            this(name, "", null, FileCopyUtils.copyToByteArray(contentStream));
        }

        /**
         * Constructs a MockMultipartFile with all parameters.
         * <p>
         * This is the primary constructor delegated to by other overloads. Validates
         * that name is not empty, defaulting null parameters to empty string or array.

         *
         * @param name form field name (must not be empty)
         * @param originalFilename original filename, or {@code null} for empty string
         * @param contentType MIME type, or {@code null}
         * @param content file content, or {@code null} for empty array
         * @throws IllegalArgumentException if name is empty
         */
        public MockMultipartFile(
                String name, @Nullable String originalFilename, @Nullable String contentType, @Nullable byte[] content) {

            Assert.hasLength(name, "Name must not be empty");
            this.name = name;
            this.originalFilename = (originalFilename != null ? originalFilename : "");
            this.contentType = contentType;
            this.content = (content != null ? content : new byte[0]);
        }

        /**
         * Constructs a MockMultipartFile from an input stream with metadata.
         * <p>
         * Reads the entire stream into memory using {@link FileCopyUtils#copyToByteArray}.

         *
         * @param name form field name (must not be empty)
         * @param originalFilename original filename, or {@code null} for empty string
         * @param contentType MIME type, or {@code null}
         * @param contentStream input stream to read (fully consumed)
         * @throws IOException if stream reading fails
         * @throws IllegalArgumentException if name is empty
         */
        public MockMultipartFile(
                String name, @Nullable String originalFilename, @Nullable String contentType, InputStream contentStream)
                throws IOException {

            this(name, originalFilename, contentType, FileCopyUtils.copyToByteArray(contentStream));
        }

        /**
         * Returns the form field name for this file.
         *
         * @return the form field name
         */
        @Override
        public String getName() {
            return this.name;
        }

        /**
         * Returns the original filename from the upload.
         *
         * @return the original filename (never null, defaults to empty string)
         */
        @Override
        @NonNull
        public String getOriginalFilename() {
            return this.originalFilename;
        }

        /**
         * Returns the MIME content type of the file.
         *
         * @return the content type, or {@code null} if not specified
         */
        @Override
        @Nullable
        public String getContentType() {
            return this.contentType;
        }

        /**
         * Checks if the file content is empty.
         *
         * @return {@code true} if content length is zero
         */
        @Override
        public boolean isEmpty() {
            return (this.content.length == 0);
        }

        /**
         * Returns the size of the file content in bytes.
         *
         * @return content length in bytes
         */
        @Override
        public long getSize() {
            return this.content.length;
        }

        /**
         * Returns the file content as a byte array.
         *
         * @return the content byte array
         * @throws IOException declared by interface, but never thrown in this implementation
         */
        @Override
        public byte[] getBytes() throws IOException {
            return this.content;
        }

        /**
         * Returns an input stream for reading the file content.
         * <p>
         * Creates a new {@link ByteArrayInputStream} wrapping the content array.
         * Multiple calls create independent streams.

         *
         * @return new {@link InputStream} for reading content
         * @throws IOException declared by interface, but never thrown in this implementation
         */
        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(this.content);
        }

        /**
         * Transfers file content to the specified destination file.
         * <p>
         * Copies the content byte array to the destination file using
         * {@link FileCopyUtils#copy(byte[], java.io.File)}.

         *
         * @param dest the destination file
         * @throws IOException if file writing fails
         * @throws IllegalStateException declared by interface, but never thrown in this implementation
         */
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            FileCopyUtils.copy(this.content, dest);
        }

    }
}
