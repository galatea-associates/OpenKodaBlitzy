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

package com.openkoda.uicomponent.live;

import com.openkoda.core.multitenancy.TenantResolver;
import com.openkoda.core.service.FileService;
import com.openkoda.core.service.pdf.PdfConstructor;
import com.openkoda.model.file.File;
import com.openkoda.repository.file.SecureFileRepository;
import com.openkoda.uicomponent.MediaServices;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;


/**
 * Live implementation of {@link MediaServices} providing PDF generation and file creation operations for UI components.
 * <p>
 * This service acts as a bridge between the UI component layer and core file management services.
 * It delegates PDF generation to {@link PdfConstructor} and file persistence to {@link FileService},
 * ensuring tenant-aware file storage through {@link TenantResolver}. All file operations automatically
 * detect content types and persist files securely via {@link SecureFileRepository}.
 * 
 * <p>
 * Exception handling: IOException and SQLException from underlying services are caught and wrapped
 * into RuntimeException to simplify error handling in UI component JavaScript contexts.
 * 
 * <p>
 * Thread-safety: This class is stateless and thread-safe as it delegates to thread-safe Spring services.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see MediaServices
 * @see PdfConstructor
 * @see FileService
 * @see SecureFileRepository
 */
@Component
public class LiveMediaServices implements MediaServices {
    /**
     * PDF document generator used to create PDF files from templates and data models.
     * Provides methods to generate PDFs as byte arrays or input streams.
     */
    @Inject
    PdfConstructor pdfConstructor;

    /**
     * File service for persisting files and preparing file entities with metadata.
     * Handles tenant-aware file storage and content-type detection.
     */
    @Inject
    FileService fileService;

    /**
     * Secure repository for file entity persistence with privilege enforcement.
     * Ensures files are stored with appropriate tenant and security context.
     */
    @Inject
    SecureFileRepository fileRepository;

    /**
     * Generates a PDF document from a template and returns it as a byte array.
     * <p>
     * Delegates to {@link PdfConstructor#writeDocumentToByteArray(String, Map[])} to render
     * the specified template with the provided data models. Useful for generating PDFs for
     * download or further processing in memory.
     * 
     *
     * @param templateName the path to the PDF template (relative to template directory)
     * @param models variable number of data model maps containing values to render in the template
     * @return byte array containing the complete PDF document content
     * @see PdfConstructor#writeDocumentToByteArray(String, Map[])
     */
    @Override
    public byte[] writePdfToByteArray(String templateName, Map<String, Object> ... models) {
        return pdfConstructor.writeDocumentToByteArray(templateName, models);
    }

    /**
     * Generates a PDF document from a template and returns it as an input stream.
     * <p>
     * Delegates to {@link PdfConstructor#writeDocumentToStream(String, Map[])} to render
     * the specified template with the provided data models. Useful for streaming PDFs directly
     * to HTTP responses or other output destinations without loading entire content into memory.
     * 
     *
     * @param templateName the path to the PDF template (relative to template directory)
     * @param models variable number of data model maps containing values to render in the template
     * @return InputStream containing the PDF document content for streaming
     * @see PdfConstructor#writeDocumentToStream(String, Map[])
     */
    @Override
    public InputStream writePdfToStream(String templateName, Map<String, Object> ... models) {
        return pdfConstructor.writeDocumentToStream(templateName, models);
    }

    /**
     * Creates a file entity from byte array content with tenant-aware persistence.
     * <p>
     * This method persists the provided byte array as a file in the current tenant's context.
     * It automatically detects content type, stores the file content, and creates a File entity
     * with appropriate metadata. The organization ID is resolved from {@link TenantResolver} to
     * ensure tenant isolation.
     * 
     *
     * @param input byte array containing the complete file content to persist
     * @param fileName the name for the file including extension (used for display and content-type detection)
     * @return persisted File entity with generated ID and metadata
     * @throws RuntimeException wrapping IOException if file storage fails, or SQLException if database persistence fails
     * @see com.openkoda.core.service.FileService#saveAndPrepareFileEntity(Long, String, String, String, byte[])
     * @see TenantResolver#getTenantedResource()
     */
    @Override
    public File createFileFromByteArray(byte[] input, String fileName) {
        File f = null;
        try {
            f = fileService.saveAndPrepareFileEntity(
                    TenantResolver.getTenantedResource().organizationId,
                    null, fileName, fileName, input);
            f = fileRepository.save(f);
            return f;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a file entity from input stream content with tenant-aware persistence.
     * <p>
     * This method persists the provided input stream as a file in the current tenant's context.
     * It stores the stream content efficiently, creates a File entity with size and metadata,
     * and ensures tenant isolation through {@link TenantResolver}. Useful for handling large files
     * or streaming uploads without loading entire content into memory.
     * 
     *
     * @param inputStream the input stream containing file content to persist (will be consumed)
     * @param totalFileSize the total size of the file in bytes (required for storage allocation)
     * @param fileName the name for the file including extension (used for display and content-type detection)
     * @return persisted File entity with generated ID and metadata
     * @throws RuntimeException wrapping IOException if stream reading or file storage fails, or SQLException if database persistence fails
     * @see com.openkoda.core.service.FileService#saveAndPrepareFileEntity(Long, String, String, long, String, InputStream)
     * @see TenantResolver#getTenantedResource()
     */
    @Override
    public File createFileFromStream(InputStream inputStream, long totalFileSize, String fileName) {
        try {
            File f = fileService.saveAndPrepareFileEntity(
                    TenantResolver.getTenantedResource().organizationId,
                    null, fileName, totalFileSize, fileName, inputStream);
            f = fileRepository.save(f);
            return f;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}