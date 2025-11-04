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

package com.openkoda.model.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openkoda.core.service.FileService;
import com.openkoda.dto.file.FileDto;
import com.openkoda.model.PrivilegeNames;
import com.openkoda.model.common.OpenkodaEntity;
import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Formula;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * JPA entity representing file metadata and providing storage-agnostic content access.
 * <p>
 * This entity is persisted to the 'file' table and stores metadata about uploaded files
 * in a multi-tenant OpenKoda application. The architecture separates metadata storage
 * (always in database) from content storage (filesystem OR database Blob).

 * <p>
 * <b>Storage Architecture:</b><br>
 * Files support two storage backends controlled by {@link FileService.StorageType}:
 * <ul>
 *   <li><b>filesystem</b>: Content stored on disk at {@code filesystemPath}, metadata in database</li>
 *   <li><b>database</b>: Both content (as Blob) and metadata stored in database</li>
 * </ul>

 * <p>
 * <b>Multi-Tenancy:</b><br>
 * This entity extends {@link OpenkodaEntity}, providing organization-scoped file isolation.
 * Each file belongs to exactly one organization, enforcing tenant boundaries for file access.

 * <p>
 * <b>Access Control:</b><br>
 * Read and write privileges are computed dynamically via {@code @Formula} annotations:
 * <ul>
 *   <li>{@code requiredReadPrivilege}: {@link PrivilegeNames#_readOrgData}</li>
 *   <li>{@code requiredWritePrivilege}: {@link PrivilegeNames#_manageOrgData}</li>
 * </ul>

 * <p>
 * <b>Content Access:</b><br>
 * Use {@link #getContentStream()} to obtain an InputStream for reading file content.
 * <b>Callers MUST close returned InputStreams</b> to prevent resource leaks.

 * <p>
 * <b>Important Constraints:</b>
 * <ul>
 *   <li>Blob streaming requires an active JDBC transaction context</li>
 *   <li>Filesystem storage requires read permissions on {@code filesystemPath}</li>
 *   <li>The {@code uploadUuid} field has a unique constraint for upload tracking</li>
 * </ul>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FileService.StorageType
 * @see OpenkodaEntity
 * @see com.openkoda.dto.file.FileDto
 */
@Entity
@Table(name = "file")
public class File extends OpenkodaEntity {

    /**
     * Original filename of the uploaded file.
     * <p>
     * Stores the filename as provided during upload, preserving the original
     * name for display and download purposes.

     */
    @Column
    private String filename;

    /**
     * Storage backend type determining where file content is stored.
     * <p>
     * This non-null field indicates whether content is stored in the filesystem
     * or as a database Blob. Persisted as a STRING enum value.

     * <p>
     * Valid values:
     * <ul>
     *   <li>{@link FileService.StorageType#filesystem}: Content in file at {@code filesystemPath}</li>
     *   <li>{@link FileService.StorageType#database}: Content in {@code content} Blob field</li>
     * </ul>

     *
     * @see FileService.StorageType
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileService.StorageType storageType;

    /**
     * Absolute path to file on the filesystem when using filesystem storage.
     * <p>
     * This field is populated only when {@code storageType} is {@link FileService.StorageType#filesystem}.
     * For database storage, this field remains {@code null}.

     * <p>
     * The path should be accessible with appropriate read permissions for the
     * application process to open InputStreams via {@link #getContentStream()}.

     */
    @Column
    private String filesystemPath;

    /**
     * Unique identifier for tracking file uploads.
     * <p>
     * This UUID is generated during the upload process and enforced as unique
     * at the database level via a unique constraint. Used to correlate upload
     * sessions with persisted file entities.

     */
    @Column(unique = true)
    private String uploadUuid;

    /**
     * MIME type of the file content.
     * <p>
     * Standard MIME type string indicating the nature and format of the file.
     * Examples include:
     * <ul>
     *   <li>{@code "image/png"} for PNG images</li>
     *   <li>{@code "application/pdf"} for PDF documents</li>
     *   <li>{@code "text/plain"} for text files</li>
     *   <li>{@code "video/mp4"} for MP4 videos</li>
     * </ul>

     * <p>
     * Used by {@link #isImage()} and {@link #isVideo()} helper methods to
     * determine file type categories.

     */
    @Column
    private String contentType;

    /**
     * File size in bytes.
     * <p>
     * Stores the total byte count of the file content as a primitive {@code long}.
     * This value is set during upload and used for storage calculations and
     * user-facing display of file sizes.

     */
    @Column
    private long size;

    /**
     * Flag indicating whether this file is publicly accessible.
     * <p>
     * When {@code true}, the file can be accessed without authentication.
     * When {@code false} (default), standard privilege checks apply.
     * Database default value is {@code false}.

     */
    @Column(columnDefinition = "boolean default false")
    private boolean publicFile;

    /**
     * Binary content stored in database as a Blob (Large Object).
     * <p>
     * This field is populated only when {@code storageType} is {@link FileService.StorageType#database}.
     * For filesystem storage, this field remains {@code null}.

     * <p>
     * The {@code @Lob} annotation indicates this is a Large Object persisted
     * in the database. The {@code @JsonIgnore} annotation prevents accidental
     * serialization of potentially large binary data in JSON responses.

     * <p>
     * <b>Important:</b> Accessing the Blob's binary stream requires an active
     * JDBC transaction. Use {@link #getContentStream()} which handles the
     * appropriate streaming call based on storage type.

     *
     * @see java.sql.Blob
     * @see #getContentStream()
     */
    @Lob
    @JsonIgnore
    private Blob content;

    /**
     * Computed field returning the required privilege for reading this file.
     * <p>
     * This field is computed at query time via Hibernate {@code @Formula} annotation
     * and always returns {@link PrivilegeNames#_readOrgData}.

     * <p>
     * The value is not stored in the database but calculated by Hibernate when
     * loading the entity, enabling uniform privilege checking across file operations.

     *
     * @see PrivilegeNames#_readOrgData
     */
    @Formula("( '" + PrivilegeNames._readOrgData + "' )")
    private String requiredReadPrivilege;

    /**
     * Computed field returning the required privilege for modifying this file.
     * <p>
     * This field is computed at query time via Hibernate {@code @Formula} annotation
     * and always returns {@link PrivilegeNames#_manageOrgData}.

     * <p>
     * The value is not stored in the database but calculated by Hibernate when
     * loading the entity, enabling uniform privilege checking for file updates
     * and deletions.

     *
     * @see PrivilegeNames#_manageOrgData
     */
    @Formula("( '" + PrivilegeNames._manageOrgData + "' )")
    private String requiredWritePrivilege;

    /**
     * Returns an InputStream for reading the file's binary content.
     * <p>
     * This method provides storage-agnostic content access by switching on
     * {@link #storageType}:
     * <ul>
     *   <li><b>filesystem</b>: Returns a {@link FileInputStream} opened on {@link #filesystemPath}</li>
     *   <li><b>database</b>: Returns the binary stream from the {@link #content} Blob</li>
     * </ul>

     * <p>
     * <b>Caller Responsibilities:</b>
     * <ul>
     *   <li><b>MUST close the returned InputStream</b> to prevent resource leaks</li>
     *   <li>For database storage, ensure method is called within an active JDBC transaction</li>
     *   <li>For filesystem storage, ensure read permissions on the file path</li>
     * </ul>

     *
     * @return InputStream for reading file content; caller must close this stream
     * @throws IOException if filesystem read fails or path is inaccessible
     * @throws SQLException if database Blob streaming fails or no active transaction
     * @throws RuntimeException if storageType is neither filesystem nor database
     */
    public InputStream getContentStream() throws IOException, SQLException, RuntimeException {
        switch (storageType) {
            case filesystem:
                return new FileInputStream(filesystemPath);
            case database:
                return content.getBinaryStream();
            default:
                throw new RuntimeException("Method File.getContentStream() used on file " +
                        "not stored in filesystem or database");
        }
    }

    /**
     * Default no-argument constructor.
     * <p>
     * Creates a File entity with no organization association (organizationId is null).
     * Typically used by JPA for entity instantiation during query result mapping.

     */
    public File() {
        super(null);
    }

    /**
     * Constructor for organization-scoped file.
     * <p>
     * Creates a File entity associated with the specified organization,
     * enforcing multi-tenant isolation.

     *
     * @param organizationId the ID of the organization that owns this file; may be null
     */
    public File(Long organizationId) {
        super(organizationId);
    }

    /**
     * Constructor for organization-scoped file with UUID parameter.
     * <p>
     * Creates a File entity associated with the specified organization.
     * Note: The uuid parameter is currently not used in initialization.

     *
     * @param organizationId the ID of the organization that owns this file
     * @param uuid upload UUID (parameter accepted but not used in this constructor)
     */
    public File(Long organizationId, String uuid) {
        super(organizationId);
    }

    /**
     * Constructor initializing file with complete metadata.
     * <p>
     * Creates a File entity with all essential file properties for upload processing.
     * This constructor is commonly used during file upload workflows to capture
     * metadata before persisting.

     *
     * @param organizationId the ID of the organization that owns this file
     * @param filename original name of the uploaded file
     * @param contentType MIME type of the file (e.g., "image/png", "application/pdf")
     * @param size file size in bytes
     * @param uploadUuid unique identifier for tracking this upload
     * @param storageType storage backend (filesystem or database)
     * @see FileService.StorageType
     */
    public File(Long organizationId, String filename, String contentType, long size, String uploadUuid,
                FileService.StorageType storageType) {
        super(organizationId);
        this.filename = filename;
        this.uploadUuid = uploadUuid;
        this.contentType = contentType;
        this.size = size;
        this.storageType = storageType;
    }

    /**
     * Constructor for file without organization association.
     * <p>
     * Creates a File entity with essential metadata but no organization scoping.
     * Typically used for temporary or system-level files not associated with
     * a specific tenant.

     *
     * @param filename original name of the file
     * @param contentType MIME type of the file
     * @param storageType storage backend (filesystem or database)
     */
    public File(String filename, String contentType, FileService.StorageType storageType) {
        super(null);
        this.filename = filename;
        this.contentType = contentType;
        this.storageType = storageType;
    }

    /**
     * Constructor initializing file with metadata and filesystem path.
     * <p>
     * Creates a File entity with all metadata properties and the filesystem path
     * for files stored on disk. This constructor extends the base metadata constructor
     * by adding filesystem storage location.

     *
     * @param organizationId the ID of the organization that owns this file
     * @param filename original name of the uploaded file
     * @param contentType MIME type of the file (e.g., "image/png", "application/pdf")
     * @param size file size in bytes
     * @param uploadUuid unique identifier for tracking this upload
     * @param storageType storage backend (should be filesystem when filesystemPath is provided)
     * @param filesystemPath absolute path to the file on disk
     * @see FileService.StorageType#filesystem
     */
    public File(Long organizationId, String filename, String contentType, long size, String uploadUuid,
                FileService.StorageType storageType, String filesystemPath) {
        this(organizationId, filename, contentType, size, uploadUuid, storageType);
        this.filesystemPath = filesystemPath;
    }

    /**
     * Returns the original filename of the uploaded file.
     *
     * @return filename as provided during upload
     */
    public String getFilename() {
        return this.filename;
    }

    /**
     * Returns the MIME type of the file.
     *
     * @return content type (e.g., "image/png", "application/pdf")
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Returns the database Blob containing file content.
     * <p>
     * This field is populated only for database storage. Returns {@code null}
     * for filesystem storage.

     *
     * @return Blob with binary content, or null for filesystem storage
     * @see #getContentStream()
     */
    public Blob getContent() {
        return this.content;
    }

    /**
     * Sets the filename for this file.
     *
     * @param filename the original filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Sets the MIME content type for this file.
     *
     * @param contentType MIME type string (e.g., "image/png")
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Sets the database Blob for file content.
     * <p>
     * Used when {@code storageType} is database. Should remain null for
     * filesystem storage.

     *
     * @param content Blob containing binary file data
     */
    public void setContent(Blob content) {
        this.content = content;
    }

    /**
     * Returns the audit string representation for this file.
     * <p>
     * Currently not implemented for File entities - returns {@code null}.

     *
     * @return null (audit string not implemented for File)
     */
    @Override
    public String toAuditString() {
        return null;
    }

    /**
     * Returns the unique upload identifier for this file.
     *
     * @return upload UUID used to track this file upload
     */
    public String getUploadUuid() {
        return uploadUuid;
    }

    /**
     * Sets the unique upload identifier.
     *
     * @param uploadUuid unique identifier for tracking this upload
     */
    public void setUploadUuid(String uploadUuid) {
        this.uploadUuid = uploadUuid;
    }

    /**
     * Returns the file size in bytes.
     *
     * @return size in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the file size in bytes.
     *
     * @param size file size in bytes
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Static factory method converting a File entity to FileDto.
     * <p>
     * Creates a data transfer object containing the file's ID, organization ID,
     * filename, and content type. Returns an empty FileDto if the input is null.

     *
     * @param a File entity to convert, or null
     * @return FileDto with id, organizationId, filename, and contentType populated;
     *         empty DTO if input is null
     * @see FileDto
     */
    public static FileDto toFileDto(File a) {
        if (a == null) {
            return new FileDto();
        }
        return new FileDto(a.getId(), a.getOrganizationId(), a.getFilename(), a.getContentType());
    }

    /**
     * Instance method converting this File entity to FileDto.
     * <p>
     * Delegates to the static {@link #toFileDto(File)} factory method.

     *
     * @return FileDto representation of this file
     * @see #toFileDto(File)
     */
    public FileDto toFileDto() {
        return toFileDto(this);
    }

    /**
     * Returns the download URL for this file.
     * <p>
     * Converts this entity to a FileDto and returns its download URL property.
     * The URL is constructed based on the file's ID and organization context.

     *
     * @return download URL for accessing this file
     * @see FileDto#downloadUrl
     */
    public String getUrl() {
        return toFileDto().downloadUrl;
    }

    /**
     * Checks if this file is an image based on its content type.
     * <p>
     * Returns {@code true} if the content type starts with "image/",
     * indicating any image MIME type (e.g., "image/png", "image/jpeg").

     *
     * @return true if content type indicates an image file
     */
    public boolean isImage() {
        return StringUtils.startsWith(contentType, "image/");
    }

    /**
     * Checks if this file is a video based on its content type.
     * <p>
     * Returns {@code true} if the content type starts with "video/",
     * indicating any video MIME type (e.g., "video/mp4", "video/mpeg").

     *
     * @return true if content type indicates a video file
     */
    public boolean isVideo() {
        return StringUtils.startsWith(contentType, "video/");
    }

    /**
     * Returns the storage backend type for this file.
     *
     * @return storage type (filesystem or database)
     * @see FileService.StorageType
     */
    public FileService.StorageType getStorageType() {
        return storageType;
    }

    /**
     * Sets the storage backend type for this file.
     *
     * @param storageType storage type (filesystem or database)
     * @see FileService.StorageType
     */
    public void setStorageType(FileService.StorageType storageType) {
        this.storageType = storageType;
    }

    /**
     * Returns the filesystem path where file content is stored.
     * <p>
     * Returns {@code null} if storage type is database.

     *
     * @return absolute filesystem path, or null for database storage
     */
    public String getFilesystemPath() {
        return filesystemPath;
    }

    /**
     * Sets the filesystem path for file content storage.
     * <p>
     * Should only be set when storage type is filesystem.

     *
     * @param filesystemPath absolute path to file on disk
     */
    public void setFilesystemPath(String filesystemPath) {
        this.filesystemPath = filesystemPath;
    }

    /**
     * Returns whether this file is publicly accessible.
     *
     * @return true if file can be accessed without authentication
     */
    public boolean isPublicFile() {
        return publicFile;
    }

    /**
     * Sets the public accessibility flag for this file.
     *
     * @param publicFile true to allow public access, false to require authentication
     */
    public void setPublicFile(boolean publicFile) {
        this.publicFile = publicFile;
    }

    /**
     * Returns the privilege required to read this file.
     * <p>
     * Overrides parent method to return the computed privilege from the
     * {@code @Formula} field.

     *
     * @return privilege name required for read access ({@link PrivilegeNames#_readOrgData})
     */
    @Override
    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    /**
     * Returns the privilege required to modify this file.
     * <p>
     * Overrides parent method to return the computed privilege from the
     * {@code @Formula} field.

     *
     * @return privilege name required for write access ({@link PrivilegeNames#_manageOrgData})
     */
    @Override
    public String getRequiredWritePrivilege() {
        return requiredWritePrivilege;
    }

    /**
     * Returns a tab-delimited string representation of this file.
     * <p>
     * Format: {@code id\tcontentType\tfilename}

     *
     * @return string containing ID, content type, and filename separated by tabs
     */
    @Override
    public String toString() {
        return id + "\t" + contentType + "\t" + filename;
    }
}