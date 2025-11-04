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

package com.openkoda.uicomponent;

import com.openkoda.model.file.File;
import com.openkoda.uicomponent.annotation.Autocomplete;

import java.io.InputStream;
import java.util.Map;

/**
 * Provides for media creation, ie. files, images, movies, documents
 * <p>
 * Service contract for templated PDF generation and file entity creation.
 * PDF generation uses Thymeleaf templates with variable substitution.
 * File creation handles storage, content-type detection, and database persistence.
 * All operations are tenant-aware (organization-scoped).
 * 
 * <p>
 * Implementation: LiveMediaServices
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * // PdfConstructor
 * @see com.openkoda.core.service.FileService
 * @see com.openkoda.model.file.File
 */
public interface MediaServices {

    /**
     * <p>Creates a pdf file based on templateName template and returns it as byte array.</p>
     * <p>Many pdf pages can be created, with separate pages for each model provided</p>
     * <p>When models is null or empty, the template will be fed with empty model and one pdf will be generated</p>
     * <p>
     * Delegates to PdfConstructor.writePdfToByteArray. Each model Map in models varargs produces separate PDF page.
     * Template variables accessed as ${key} in Thymeleaf. If models is null/empty, generates single page with empty model.
     * 
     * <p>
     * Example: {@code writePdfToByteArray("invoice", Map.of("total", 100, "customer", "Acme"))} generates single-page PDF
     * 
     *
     * @param templateName Thymeleaf template name (path relative to template directory, e.g., 'reports/invoice.html')
     * @param models Varargs array of model Maps for template variable substitution, each Map generates one PDF page
     * @return PDF document as byte array (ready for download, attachment, or storage)
     * // PdfConstructor
     */
    @Autocomplete
    byte[] writePdfToByteArray(String templateName, Map<String, Object> ... models);

    /**
     * <p>Creates a pdf file based on templateName template and returns it input stream</p>
     * <p>Many pdf pages can be created, with separate pages for each model provided</p>
     * <p>When models is null or empty, the template will be fed with empty model and one pdf will be generated</p>
     * <p>
     * Similar to writePdfToByteArray but returns InputStream for streaming scenarios. Suitable for large PDFs or
     * direct HTTP response streaming. Caller responsible for closing stream.
     * 
     * <p>
     * Example: {@code writePdfToStream("report", modelMap1, modelMap2)} generates two-page PDF as stream
     * 
     * <p>
     * Note: InputStream backed by ByteArrayInputStream - entire PDF in memory, not true streaming
     * 
     *
     * @param templateName Thymeleaf template name for PDF generation
     * @param models Varargs array of model Maps, each Map generates one PDF page
     * @return InputStream containing PDF document bytes (stream-friendly for large PDFs)
     * // PdfConstructor
     */
    @Autocomplete
    InputStream writePdfToStream(String templateName, Map<String, Object> ... models);

    /**
     * <p>Creates a file based from input stream</p>
     * <p>It ties to determine the content type of the file automatically</p>
     * <p>The method requires to provide content size which may be difficult in some scenarios.</p>
     * <p>
     * Resolves organizationId via TenantResolver, detects contentType from fileName extension (Apache Tika),
     * reads inputStream bytes, calls fileService.saveAndPrepareFileEntity, persists via fileRepository.save.
     * File stored according to STORAGE_TYPE configuration (filesystem or database BLOB).
     * 
     * <p>
     * Example: {@code createFileFromStream(inputStream, 1024, "doc.pdf")} creates File entity with contentType='application/pdf'
     * 
     * <p>
     * Note: Throws RuntimeException if organizationId unavailable or stream read fails
     * 
     *
     * @param inputStream Source stream containing file bytes (caller must close stream)
     * @param totalFileSize Total file size in bytes (used for storage validation and progress tracking)
     * @param fileName Original filename with extension (used for content-type detection and display)
     * @return Persisted File entity with generated ID, organizationId, contentType, and storage reference
     * @see com.openkoda.core.service.FileService
     * @see com.openkoda.model.file.File
     */
    @Autocomplete
    File createFileFromStream(InputStream inputStream, long totalFileSize, String fileName);

    /**
     * <p>Creates a file based from byte array</p>
     * <p>It ties to determine the content type of the file automatically</p>
     * <p>
     * Similar to createFileFromStream but accepts byte array directly. Detects contentType from fileName,
     * resolves organizationId, calls fileService.saveAndPrepareFileEntity, persists. More convenient than
     * stream variant when bytes already in memory.
     * 
     * <p>
     * Example: {@code createFileFromByteArray(pdfBytes, "report.pdf")} creates File with detected contentType
     * 
     * <p>
     * Note: Entire byte array held in memory - use createFileFromStream for large files
     * 
     *
     * @param input File content as byte array
     * @param fileName Original filename with extension for content-type detection
     * @return Persisted File entity with generated ID, organizationId, contentType, and storage reference
     * @see com.openkoda.core.service.FileService
     * @see com.openkoda.model.file.File
     */
    @Autocomplete
    File createFileFromByteArray(byte[] input, String fileName);

}
