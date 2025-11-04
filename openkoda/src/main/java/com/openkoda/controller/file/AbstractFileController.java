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

import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageAttr;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.form.FileForm;
import com.openkoda.model.file.File;
import com.openkoda.repository.file.FileRepository;
import com.openkoda.repository.file.SecureFileRepository;
import jakarta.inject.Inject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.BindingResult;

/**
 * Abstract base controller providing file upload, download, and management operations.
 * <p>
 * Implements common file handling patterns: multipart upload processing, secure file download 
 * with content-type detection, file deletion with privilege checks. Subclasses implement specific 
 * file storage strategies (filesystem, S3, database). Uses {@code services.file} for storage abstraction. 
 * Extends {@link AbstractController} for Flow-based orchestration and implements {@link HasSecurityRules} 
 * for security rule discovery.
 * 
 * <p>
 * Design Pattern: Separation of permission-aware reads ({@code secureFileRepository}) from 
 * unconstrained writes ({@code unsecureFileRepository}). Flow-scoped {@link PageModelMap} 
 * avoids per-request mutable fields.
 * 
 * <p>
 * Usage: Subclass this controller to create concrete file management endpoints (HTML, REST, etc.).
 * 
 * <p>
 * Example:
 * <pre>{@code
 * public class FileControllerHtml extends AbstractFileController {
 *     // Implement specific HTTP endpoints
 * }
 * }</pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.core.service.FileService
 * @see com.openkoda.repository.file.FileRepository
 * @see com.openkoda.repository.file.SecureFileRepository
 * @see com.openkoda.form.FileForm
 * @see com.openkoda.core.flow.Flow
 */
public class AbstractFileController extends AbstractController implements HasSecurityRules {

    /**
     * Privilege-enforcing repository for file reads with organization-scoped access control.
     * <p>
     * Used for secure file retrieval operations where permission checks are required.
     * Read operations through this repository enforce privilege validation before
     * returning file entities.
     * 
     */
    @Inject
    SecureFileRepository secureFileRepository;

    /**
     * Direct repository for file writes and administrative operations bypassing privilege checks.
     * <p>
     * Used for file persistence operations after validation and privilege checks have been
     * performed. Write operations use this repository to avoid redundant permission validation.
     * 
     */
    @Inject
    FileRepository unsecureFileRepository;

    /**
     * Flow context key for carrying {@link File} entity across pipeline stages.
     * <p>
     * Used to store and retrieve File entities within Flow execution context,
     * enabling type-safe access in controller methods.
     * 
     */
    static PageAttr<File> fileEntity = new PageAttr<>("fileEntity");
    
    /**
     * Flow context key for carrying {@link FileForm} data between validation and persistence.
     * <p>
     * Stores form data during Flow execution to enable validation and entity population
     * operations.
     * 
     */
    static PageAttr<FileForm> fileForm = new PageAttr<>("fileForm");
    
    /**
     * Flow context key for carrying paginated search results ({@link Page}<{@link File}>).
     * <p>
     * Used to store paginated file listings for display in views, enabling
     * type-safe access to search results.
     * 
     */
    static PageAttr<Page<File>> filePage = new PageAttr<>("filePage");

    /**
     * Canonical URL path segment 'file' for routing and link generation.
     * <p>
     * Used consistently across controllers for building file-related URLs.
     * 
     */
    public final static String fileUrl = "file";

    /**
     * Performs privilege-aware paginated file search with optional filtering.
     * <p>
     * Uses {@code secureFileRepository.search()} to enforce read privileges,
     * stores {@link Page}<{@link File}> in Flow context under {@code filePage} attribute.
     * All search operations respect organization-scoped access control and apply
     * privilege validation before returning results.
     * 
     * <p>
     * Example:
     * <pre>{@code
     * PageModelMap result = searchFile(orgId, "invoice", null, pageable);
     * }</pre>
     * 
     *
     * @param organizationId Organization scope for multi-tenant filtering (null for global search)
     * @param aSearchTerm Search term applied to filename field (empty string for no filtering)
     * @param aSpecification JPA Specification for advanced query criteria (null for no specification)
     * @param aPageable Spring Data pagination parameters (page number, size, sort)
     * @return PageModelMap with {@code filePage} attribute containing search results
     */
    protected PageModelMap searchFile(
            Long organizationId,
            String aSearchTerm,
            Specification<File> aSpecification,
            Pageable aPageable) {
        debug("[searchFile]");
        return Flow.init()
            .thenSet(filePage, a -> secureFileRepository.search(aSearchTerm, organizationId, aSpecification, aPageable))
            .execute();
    }

    /**
     * Loads single file by ID and prepares {@link FileForm} for view/edit operations.
     * <p>
     * Security: Enforces read privilege via {@code secureFileRepository.findOne()}.
     * Returns {@link PageModelMap} with {@code fileEntity} and {@code fileForm} attributes
     * populated for form rendering or modification.
     * 
     * <p>
     * Example:
     * <pre>{@code
     * PageModelMap result = findFile(orgId, 123L);
     * }</pre>
     * 
     *
     * @param organizationId Organization context for form initialization
     * @param fileId File entity ID to retrieve
     * @return PageModelMap with {@code fileEntity} and {@code fileForm} attributes
     * @throws com.openkoda.core.exception.ResourceNotFoundException if file does not exist
     */
    protected PageModelMap findFile(Long organizationId, long fileId) {
        debug("[findFile] fileId: {}", fileId);
        return Flow.init(fileId)
            .thenSet(fileEntity, a -> secureFileRepository.findOne(fileId))
            .thenSet(fileForm, a -> new FileForm(organizationId, a.result))
            .execute();
    }

    /**
     * Updates existing file metadata via form submission with validation.
     * <p>
     * Flow: Retrieves entity via {@code secureFileRepository}, validates form, populates entity,
     * persists via {@code unsecureFileRepository}.
     * 
     * <p>
     * Implementation note: Uses {@code unsecureFileRepository.save()} after privilege check
     * to avoid double-validation.
     * 
     * <p>
     * Example:
     * <pre>{@code
     * PageModelMap result = updateFile(orgId, 123L, form, bindingResult);
     * }</pre>
     * 
     *
     * @param organizationId Organization context (currently unused in implementation)
     * @param fileId File entity ID to update
     * @param formData {@link FileForm} containing updated metadata fields
     * @param br Spring validation {@link BindingResult} for error accumulation
     * @return PageModelMap with updated {@code fileEntity} or validation errors
     */
    protected PageModelMap updateFile(Long organizationId, long fileId, FileForm formData, BindingResult br) {
        debug("[updateFile] fileId: {}", fileId);
        return Flow.init(fileForm, formData)
            .then(a -> secureFileRepository.findOne(fileId))
            .then(a -> services.validation.validateAndPopulateToEntity(formData, br,a.result))
            .thenSet(fileEntity, a -> unsecureFileRepository.save(a.result))
            .execute();
    }

    /**
     * Creates new file entity from form submission with validation.
     * <p>
     * Flow: Creates new {@link File}(organizationId), validates/populates from form,
     * persists, resets form for next upload.
     * 
     * <p>
     * Example:
     * <pre>{@code
     * PageModelMap result = createFile(orgId, form, bindingResult);
     * }</pre>
     * 
     *
     * @param organizationId Organization ID for new file association
     * @param formData {@link FileForm} with file metadata
     * @param br Validation {@link BindingResult}
     * @return PageModelMap with reset {@code fileForm} on success or validation errors on failure
     */
    protected PageModelMap createFile(Long organizationId, FileForm formData, BindingResult br) {
        debug("[createFile]");
        return Flow.init(fileForm, formData)
            .then(a -> services.validation.validateAndPopulateToEntity(formData, br,new File(organizationId)))
            .then(a -> unsecureFileRepository.save(a.result))
            .thenSet(fileForm, a -> new FileForm())
            .execute();
    }

    /**
     * Transactionally deletes file with reference cleanup.
     * <p>
     * Transaction: Wrapped in {@code Flow.init(transactional)} for atomic execution.
     * Flow: First removes external file references via {@code removeFileReference()},
     * then deletes entity record via {@code removeFile()}.
     * 
     * <p>
     * Security: Caller must enforce privilege checks before invoking this method.
     * 
     * <p>
     * Example:
     * <pre>{@code
     * PageModelMap result = removeFile(123L);
     * }</pre>
     * 
     *
     * @param fileId File entity ID to remove
     * @return PageModelMap with operation result
     */
    protected PageModelMap removeFile(long fileId) {
        debug("[removeFile] fileId: {}", fileId);
        return Flow.init(transactional)
            .then(a -> unsecureFileRepository.removeFileReference(fileId))
            .then(a -> unsecureFileRepository.removeFile(fileId))
            .execute();
    }

    /**
     * Returns initialized transactional Flow placeholder for subclass extension.
     * <p>
     * Purpose: Extension point for subclasses to inject custom update logic within
     * transaction boundary.
     * 
     * <p>
     * Usage: Override and call {@code super.updateFile().then(a -> customLogic)}.
     * 
     * <p>
     * Example:
     * <pre>{@code
     * return super.updateFile().then(a -> customProcessing());
     * }</pre>
     * 
     *
     * @return PageModelMap with empty Flow context
     */
    protected PageModelMap updateFile(){
        return Flow.init(transactional)
                .execute();
    }

    /**
     * Rescales image file to specified width within transactional context.
     * <p>
     * Transaction: Wrapped in {@code Flow.init(transactional)} for atomic execution.
     * Flow: Loads file via {@code secureFileRepository}, invokes {@code services.file.scaleImage()},
     * persists updated entity.
     * 
     * <p>
     * Implementation: Uses {@code repositories.unsecure.file.saveAndFlush()} to ensure
     * immediate persistence of rescaled image.
     * 
     * <p>
     * Example:
     * <pre>{@code
     * PageModelMap result = rescaleFile(123L, 800);
     * }</pre>
     * 
     *
     * @param fileId Image file entity ID
     * @param width Target width in pixels (height computed to preserve aspect ratio)
     * @return PageModelMap with rescaled file entity
     * @throws IllegalArgumentException if file is not an image type
     */
    protected PageModelMap rescaleFile(long fileId, int width){
        return Flow.init(transactional)
                .then(a -> secureFileRepository.findOne(fileId))
                .then(a -> services.file.scaleImage(a.result, width))
                .then(a -> repositories.unsecure.file.saveAndFlush(a.result))
                .execute();
    }

}
