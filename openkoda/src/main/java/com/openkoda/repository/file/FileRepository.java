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

package com.openkoda.repository.file;

import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.file.File;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Spring Data JPA repository managing File entities for document storage and attachments.
 * <p>
 * This repository provides the persistence layer for file upload, download, and attachment handling operations.
 * It extends {@link UnsecuredFunctionalRepositoryWithLongId} to provide standard CRUD operations and
 * implements {@link HasSecurityRules} for security-aware query construction.
 * 
 * <p>
 * The repository includes custom query methods for file lookup by upload UUID, content type filtering,
 * and public file access. It also provides native DELETE queries for deterministic cleanup operations
 * that bypass JPA lifecycle callbacks and auditing.
 * 
 * <p>
 * <b>Usage Pattern:</b> For file deletion, the typical call sequence is {@code removeFileReference}
 * followed by {@code removeFile} to satisfy foreign-key constraints in the file_reference table.
 * 
 * <p>
 * <b>Thread-Safety:</b> Implemented as a thread-safe Spring Data proxy at runtime with proper
 * transaction management through Spring's declarative transactions.
 * 
 * <p>
 * <b>Transaction Requirements:</b> Native DELETE operations must run within transactional boundaries.
 * Callers are responsible for transaction demarcation and persistence context flushing.
 * 
 * <p>
 * File storage location is configured via FILE_STORAGE_FILESYSTEM_PATH environment variable or
 * application property.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see File
 * @see UnsecuredFunctionalRepositoryWithLongId
 * @see HasSecurityRules
 */
@Repository
public interface FileRepository extends UnsecuredFunctionalRepositoryWithLongId<File>, HasSecurityRules {

    /**
     * Finds a File entity by its unique upload UUID.
     * <p>
     * The upload UUID is assigned during file upload operations and serves as an external identifier
     * for retrieving file records. This method is commonly used to locate files by their upload token
     * in file download and attachment retrieval workflows.
     * 
     *
     * @param uploadUuid the unique upload identifier assigned during file upload, must not be null
     * @return the File entity matching the upload UUID, or null if not found
     */
    File findByUploadUuid(String uploadUuid);

    /**
     * Finds all File entities whose content type starts with the specified prefix.
     * <p>
     * This method is useful for locating groups of files by MIME type category, such as all images
     * (prefix "image/"), all PDFs (prefix "application/pdf"), or all text files (prefix "text/").
     * 
     * <p>
     * Example usage:
     * <pre>
     * List&lt;File&gt; images = fileRepository.findByContentTypeStartsWith("image/");
     * </pre>
     * 
     *
     * @param contentTypePrefix the MIME type prefix to match (e.g., "image/", "application/pdf"), must not be null
     * @return list of File entities with matching content types, empty list if none found
     */
    List<File> findByContentTypeStartsWith(String contentTypePrefix);

    /**
     * Finds a public File entity by its ID.
     * <p>
     * This method locates files that are marked as publicly accessible via the publicFile boolean flag.
     * It is used to retrieve files for unauthenticated access scenarios where the file has been
     * explicitly designated as public.
     * 
     * <p>
     * <b>Security:</b> Only returns files where the publicFile flag is true, ensuring that non-public
     * files are not inadvertently exposed through this query method.
     * 
     *
     * @param fileId the file identifier, must not be null
     * @return the File entity if it exists and publicFile flag is true, null otherwise
     */
    File findByIdAndPublicFileTrue(Long fileId);

    /**
     * Removes all file_reference table entries pointing to the specified file ID.
     * <p>
     * This native DELETE query bypasses JPA entity lifecycle callbacks and auditing, providing
     * deterministic cleanup of file reference associations. It is typically invoked before
     * {@link #removeFile(Long)} to satisfy foreign-key constraints.
     * 
     * <p>
     * <b>Warning:</b> Must be called BEFORE {@code removeFile} to avoid foreign-key constraint
     * violations in databases with referential integrity enabled.
     * 
     * <p>
     * <b>Transaction Requirements:</b> This method requires an active transaction. Callers must
     * ensure proper transaction demarcation and manage persistence context flushing as needed.
     * 
     * <p>
     * SQL: {@code DELETE FROM file_reference WHERE file_id = :fileId}
     * 
     *
     * @param fileId the file identifier whose references should be deleted, must not be null
     * @return the number of file_reference rows deleted
     */
    @Modifying
    @Query( value = "delete from file_reference where file_id = :fileId", nativeQuery = true)
    int removeFileReference(@Param("fileId") Long fileId);

    /**
     * Removes the File entity row from the file table.
     * <p>
     * This native DELETE query bypasses JPA entity lifecycle events and audit trails, providing
     * direct database deletion. For proper cleanup, {@link #removeFileReference(Long)} should be
     * called first to remove dependent file_reference entries and avoid foreign-key violations.
     * 
     * <p>
     * <b>Warning:</b> Call {@code removeFileReference} FIRST to avoid foreign-key constraint
     * violations. This method does not cascade deletions through JPA relationships.
     * 
     * <p>
     * <b>Transaction Requirements:</b> Requires an active transaction for proper persistence
     * context handling. The transaction ensures atomic deletion and rollback capability.
     * 
     * <p>
     * <b>Security:</b> No privilege checks are applied by this native query. Use SecureFileRepository
     * for secured file deletion operations that enforce access control rules.
     * 
     * <p>
     * SQL: {@code DELETE FROM file WHERE id = :fileId}
     * 
     *
     * @param fileId the file identifier to delete, must not be null
     * @return the number of file rows deleted (0 or 1)
     */
    @Modifying
    @Query( value = "delete from file where id = :fileId", nativeQuery = true)
    int removeFile(@Param("fileId") Long fileId);

}
