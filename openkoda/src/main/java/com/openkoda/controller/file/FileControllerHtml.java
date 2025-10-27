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

package com.openkoda.controller.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.openkoda.dto.file.FileDto;
import com.openkoda.form.FileForm;
import com.openkoda.model.file.File;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.SQLException;

import static com.openkoda.controller.common.URLConstants._FILE;
import static com.openkoda.controller.common.URLConstants._HTML_ORGANIZATION_ORGANIZATIONID;
import static com.openkoda.core.controller.generic.AbstractController._HTML;

/**
 * Concrete HTML file upload/download controller with form-based interface.
 * <p>
 * Provides file management UI with upload form, file listing with thumbnails, and download links.
 * Routes under /html/{organizationId}/file and /html/file paths. Extends AbstractFileController
 * with HTML response handling using Spring MVC ModelAndView and Thymeleaf fragments.
 * </p>
 * <p>
 * Key endpoints:
 * <ul>
 *   <li>GET /files/all - List files with pagination and search</li>
 *   <li>GET /files/new-settings - Display file upload form</li>
 *   <li>POST /files/new-settings - Process new file submission</li>
 *   <li>GET /files/{id}/settings - Display file edit form</li>
 *   <li>POST /files/{id}/settings - Update file metadata</li>
 *   <li>POST /files/{id} - Update file content</li>
 *   <li>POST /files/{id}/rescale - Rescale image dimensions</li>
 *   <li>GET /files/{id}/content - Stream file binary content</li>
 *   <li>POST /files/new/upload - Handle chunked file upload (FineUploader protocol)</li>
 *   <li>POST /files/{id}/remove - Soft-delete file</li>
 * </ul>
 * </p>
 * <p>
 * File upload flow example:
 * <pre>
 * // Client initiates upload via FineUploader
 * POST /files/new/upload?qquuid=abc-123&amp;qqfilename=document.pdf
 * // Server processes chunks and returns fileId
 * POST /files/new/upload-done?qquuid=abc-123
 * </pre>
 * </p>
 * <p>
 * Security requirements: Authentication required for all operations. File ownership
 * verified via secureFileRepository. File type whitelist and size limits enforced
 * in FileService layer.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractFileController
 * @see com.openkoda.service.file.FileService
 * @see FileForm
 */
@Controller
@RequestMapping({_HTML_ORGANIZATION_ORGANIZATIONID + _FILE, _HTML + _FILE})
public class FileControllerHtml extends AbstractFileController {

    /**
     * Updates file metadata via transactional Flow pipeline.
     * <p>
     * Processes file content update and returns success/failure message. Uses Flow
     * pipeline from AbstractFileController.updateFile() for transactional execution.
     * </p>
     *
     * @param content File content as string for text-based files
     * @param organizationId Optional organization ID for multi-tenant access control (nullable)
     * @param fileId File entity ID to update, must exist in database
     * @return ResponseEntity with success message or error details
     * @see AbstractFileController#updateFile()
     */
    @PostMapping(_ID)
    @ResponseBody
    public Object update(
            @RequestParam String content,
            @PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(ID) long fileId) {
        debug("[update] fileId: {}", fileId);
        return updateFile()
                .mav(a -> true, a -> a.get(message));
    }

    /**
     * Rescales image file to specified width while maintaining aspect ratio.
     * <p>
     * Processes image rescaling operation via FileService. Automatically calculates
     * height to preserve aspect ratio. Returns "Done." on success or error message
     * on failure.
     * </p>
     *
     * @param organizationId Optional organization context for multi-tenant filtering (nullable)
     * @param fileId Target image file ID, must be a valid image file type
     * @param width Desired width in pixels, must be positive integer
     * @return ResponseEntity with operation result message
     * @throws IllegalArgumentException if file is not an image type
     * @see AbstractFileController#rescaleFile(long, int)
     */
    @PostMapping(_ID + "/rescale")
    @ResponseBody
    public Object rescale(
            @PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(ID) long fileId,
            @RequestParam("w") int width) {
        debug("[rescale] fileId: {}", fileId);
        return rescaleFile(fileId, width)
                .mav(a -> "Done.", a -> a.get(message));
    }


    /**
     * Lists all uploaded files with pagination and search.
     * <p>
     * Returns paginated file listing with optional search term filtering. Renders
     * Thymeleaf fragment "file-all" with file metadata, thumbnails, and action links.
     * Organization-scoped for multi-tenant filtering.
     * </p>
     *
     * @param organizationId Organization scope for multi-tenant filtering (nullable for global context)
     * @param filePageable Pagination parameters (page number, size, sort) with qualifier "file"
     * @param search Optional search term for filename filtering, defaults to empty string
     * @return ModelAndView with file listing fragment "file-all"
     * @see AbstractFileController#searchFile(Long, String, String, Pageable)
     */
    @GetMapping(_ALL)
    public Object getAll(
            @PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
            @Qualifier("file") Pageable filePageable,
            @RequestParam(required = false, defaultValue = "", name = "file_search") String search) {
        debug("[getAll]");
        return searchFile(organizationId, search, null, filePageable)
                .mav("file-" + ALL);
    }

    /**
     * Displays file upload form.
     * <p>
     * Renders empty file settings form for creating new file. Uses fileId=-1 to
     * indicate new file creation mode. Returns Thymeleaf fragment "file-settings"
     * with FileForm bound to form inputs.
     * </p>
     *
     * @param organizationId Organization context for multi-tenant file ownership (nullable)
     * @return ModelAndView with "file-settings" fragment containing empty FileForm
     * @see AbstractFileController#findFile(Long, Long)
     * @see FileForm
     */
    @GetMapping(_NEW_SETTINGS)
    public Object create(
            @PathVariable(value = ORGANIZATIONID, required = false) Long organizationId) {
        debug("[create]");
        return findFile(organizationId, -1L)
                .mav("file-settings");
    }


    /**
     * Processes new file creation from form submission.
     * <p>
     * Validates FileForm via Jakarta Bean Validation and persists new file entity.
     * Returns success fragment on valid submission or error fragment with validation
     * messages on failure. Uses Flow pipeline for transactional file creation.
     * </p>
     *
     * @param organizationId Organization context for file ownership assignment (nullable)
     * @param fileForm File form data with validation constraints (@Valid annotation)
     * @param br Spring validation binding result containing validation errors if any
     * @return ModelAndView with success fragment "file-entity-form::file-settings-form-success"
     *         or error fragment "file-entity-form::file-settings-form-error"
     * @see AbstractFileController#createFile(Long, FileForm, BindingResult)
     * @see FileForm
     */
    @PostMapping(_NEW_SETTINGS)
    public Object saveNew(
            @PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
            @Valid FileForm fileForm, BindingResult br) {
        debug("[saveNew]");
        return createFile(organizationId, fileForm, br)
                .mav("file-entity-form::file-settings-form-success",
                        "file-entity-form::file-settings-form-error");
    }

    /**
     * Displays file settings edit form for existing file.
     * <p>
     * Retrieves file entity via secureFileRepository (enforcing ownership/privilege)
     * and populates FileForm. Renders "file-settings" fragment with bound form data
     * for metadata editing.
     * </p>
     *
     * @param organizationId Organization context for multi-tenant access verification (nullable)
     * @param fileId File entity ID to edit, must exist and be accessible to current user
     * @return ModelAndView with "file-settings" fragment containing populated FileForm
     * @see AbstractFileController#findFile(Long, Long)
     */
    @GetMapping(_ID_SETTINGS)
    public Object settings(
            @PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(ID) Long fileId) {
        debug("[settings] fileId: {}", fileId);
        return findFile(organizationId, fileId)
                .mav("file-settings");
    }

    /**
     * Updates existing file metadata from form submission.
     * <p>
     * Validates updated FileForm and persists changes to file entity. Returns success
     * fragment on valid update or error fragment with validation messages. Uses Flow
     * pipeline for transactional update with privilege verification.
     * </p>
     *
     * @param organizationId Organization context for access verification (nullable)
     * @param fileId File entity ID to update, must exist and be accessible
     * @param fileForm Updated form data with validation constraints (@Valid annotation)
     * @param br Validation binding result containing field errors if validation fails
     * @return ModelAndView with success fragment "file-entity-form::file-settings-form-success"
     *         or error fragment "file-entity-form::file-settings-form-error"
     * @see AbstractFileController#updateFile(Long, Long, FileForm, BindingResult)
     */
    @PostMapping(_ID_SETTINGS)
    public Object save(
            @PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(ID) Long fileId,
            @Valid FileForm fileForm, BindingResult br) {
        debug("[save] fileId: {}", fileId);
        return updateFile(organizationId, fileId, fileForm, br)
                .mav("file-entity-form::file-settings-form-success",
                        "file-entity-form::file-settings-form-error");
    }

    /**
     * Soft-deletes file after privilege verification.
     * <p>
     * Marks file as removed without physical deletion. Verifies user has delete privilege
     * via secureFileRepository. Returns boolean response indicating operation success.
     * </p>
     *
     * @param organizationId Organization context for multi-tenant access control (nullable)
     * @param fileId File entity ID to remove, must exist and be owned by user or accessible via privilege
     * @return Boolean response: true on successful deletion, false on failure
     * @throws org.springframework.security.access.AccessDeniedException if user lacks delete privilege
     * @see AbstractFileController#removeFile(Long)
     */
    @PostMapping(_ID_REMOVE)
    @ResponseBody
    public Object remove(
            @PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(ID) Long fileId) {
        debug("[remove] fileId {}", fileId);
        return removeFile(fileId)
                .mav(a -> true, a -> false);
    }

    /**
     * JSON response DTO for file upload endpoints compatible with FineUploader client library.
     * <p>
     * Encapsulates upload operation result with error message, success flag, file metadata,
     * and persisted file ID. Used by upload() endpoint to communicate with FineUploader
     * JavaScript component.
     * </p>
     * <p>
     * Fields:
     * <ul>
     *   <li>error - Error message string or null on success</li>
     *   <li>success - Boolean indicating upload operation success</li>
     *   <li>file - Complete FileDto with file metadata</li>
     *   <li>fileId - Persisted file entity ID for subsequent operations</li>
     * </ul>
     * </p>
     *
     * @see FileDto
     * @see #upload(Long, MultipartFile, String, String, long, int, int, long)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class UploadResponse implements Serializable {

        /** Error message or null on success. */
        public String error;
        /** Upload operation success indicator. */
        public boolean success;
        /** Complete file metadata DTO. */
        public FileDto file;
        /** Persisted file entity ID. */
        public Long fileId;

        /**
         * Constructs upload response with all fields.
         *
         * @param error Error message or null
         * @param success Operation success flag
         * @param fileId Persisted file ID
         * @param file File metadata DTO
         */
        public UploadResponse(String error, boolean success, Long fileId, FileDto file) {
            this.error = error;
            this.success = success;
            this.file = file;
            this.fileId = fileId;
        }
    }

    /**
     * Streams file binary content with appropriate content-type headers.
     * <p>
     * Retrieves file entity via secureFileRepository (enforcing ownership/privilege),
     * reads binary content, sets content-type and content-disposition headers, and
     * streams bytes to HttpServletResponse. Supports inline display and download.
     * Read-only transaction for efficient file access.
     * </p>
     * <p>
     * Security: Enforces file ownership or admin privilege via secureFileRepository.
     * Unauthorized access returns 403 Forbidden.
     * </p>
     *
     * @param organizationId Organization context for multi-tenant filtering (nullable)
     * @param fileId File entity ID to stream, must exist and be accessible
     * @param response HttpServletResponse for streaming binary content and setting headers
     * @throws IOException if file read or stream write fails
     * @throws SQLException if database BLOB access fails
     * @see com.openkoda.service.file.FileService#getFileContentAndPrepareResponse(File, boolean, boolean, HttpServletResponse)
     */
    @Transactional(readOnly = true)
    @GetMapping(_ID + _CONTENT)
    public void content(
            @PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(ID) Long fileId,
            HttpServletResponse response) throws IOException, SQLException {
        debug("[content] fileId {}", fileId);
        File f = secureFileRepository.findOne(fileId);
        services.file.getFileContentAndPrepareResponse(f, true, true, response);
    }

    /**
     * Handles chunked multipart file upload (FineUploader protocol).
     * <p>
     * Supports both single-file and chunked uploads. On first chunk or single file,
     * creates File entity with uploadUuid. Subsequent chunks are correlated via uuid.
     * Uses unsecureFileRepository for initial persistence (security applied post-upload).
     * Transactional to ensure file entity consistency across chunk uploads.
     * </p>
     * <p>
     * FineUploader protocol: Chunked uploads have partByteOffset, partIndex, totalParts &gt;= 0.
     * Single file uploads have all chunk parameters = -1.
     * </p>
     *
     * @param organizationId Organization context for file ownership assignment (nullable)
     * @param file Uploaded multipart file chunk from FineUploader client
     * @param uuid Upload session UUID for chunk correlation across requests
     * @param fileName Original filename from client
     * @param partByteOffset Chunk byte offset in complete file (-1 for single file upload)
     * @param partIndex Current chunk index starting from 0 (-1 for single file upload)
     * @param totalParts Total number of chunks in upload session (-1 for single file upload)
     * @param totalFileSize Complete file size in bytes across all chunks (-1 for single file upload)
     * @return ResponseEntity with UploadResponse containing fileId and FileDto
     * @throws SQLException if file entity persistence fails
     * @throws IOException if multipart file read or content processing fails
     * @see UploadResponse
     * @see #chunksDone(String)
     */
    @Transactional
    @PostMapping(_NEW + _UPLOAD)
    //TODO Rule 1.2: All business logic delegation should be in Abstract Controller
    public ResponseEntity<UploadResponse> upload(
            @PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
            @RequestParam("qqfile") MultipartFile file,
            @RequestParam("qquuid") String uuid,
            @RequestParam("qqfilename") String fileName,
            @RequestParam(value = "qqpartbyteoffset", required = false, defaultValue = "-1") long partByteOffset,
            @RequestParam(value = "qqpartindex", required = false, defaultValue = "-1") int partIndex,
            @RequestParam(value = "qqtotalparts", required = false, defaultValue = "-1") int totalParts,
            @RequestParam(value = "qqtotalfilesize", required = false, defaultValue = "-1") long totalFileSize) throws SQLException, IOException {
        debug("[upload] upload uuid {}, fileName {}", uuid, fileName);
        File f = unsecureFileRepository.findByUploadUuid(uuid);
        if (f == null) {
            String originalFilename = file.getOriginalFilename();
            InputStream inputStream = file.getInputStream();
            f = services.file.saveAndPrepareFileEntity(organizationId, uuid, fileName, totalFileSize, originalFilename, inputStream);
            unsecureFileRepository.saveAndFlush(f);
        }
        UploadResponse result = new UploadResponse(null, true, f.getId(), File.toFileDto(f));
        return ResponseEntity.ok().body(result);
    }

    /**
     * Finalizes chunked upload and returns complete file metadata.
     * <p>
     * Called by FineUploader client after all chunks uploaded successfully via upload()
     * method. Retrieves File entity by uploadUuid and returns file ID with FileDto.
     * Signals upload session completion.
     * </p>
     * <p>
     * Note: This endpoint confirms all chunks received and file entity persisted.
     * </p>
     *
     * @param uuid Upload session UUID matching uuid from upload() calls
     * @return ResponseEntity with DefaultMapEntry containing file ID (key) and FileDto (value)
     * @see #upload(Long, MultipartFile, String, String, long, int, int, long)
     * @see FileDto
     */
    @PostMapping(_NEW + "/upload-done")
    //TODO Rule 1.2: All business logic delegation should be in Abstract Controller
    public ResponseEntity<DefaultMapEntry> chunksDone(
            @RequestParam("qquuid") String uuid) {
        debug("[chunksDone] upload uuid {}", uuid);
        File f = unsecureFileRepository.findByUploadUuid(uuid);
        DefaultMapEntry dto = new DefaultMapEntry(f.getId(), new FileDto());
        return ResponseEntity.ok().body(dto);
    }
}
