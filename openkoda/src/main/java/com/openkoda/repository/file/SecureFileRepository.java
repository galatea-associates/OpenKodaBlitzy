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

import com.openkoda.controller.file.AbstractFileController;
import com.openkoda.model.common.SearchableRepositoryMetadata;
import com.openkoda.model.file.File;
import com.openkoda.repository.SecureRepository;
import org.springframework.stereotype.Repository;

import static com.openkoda.model.common.ModelConstants.DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA;

/**
 * Secure repository interface for File entities with privilege enforcement and search indexing metadata.
 * <p>
 * This interface serves as a marker combining security capabilities via {@link com.openkoda.repository.SecureRepository}
 * and searchability configuration through {@link SearchableRepositoryMetadata} annotation.
 * It extends {@code SecureRepository<File>} which adds privilege checks to all repository operations,
 * preventing unauthorized file access. The interface body is intentionally empty, with all metadata
 * provided via declarative annotations.

 * <p>
 * Runtime implementations are supplied by Spring Data JPA framework proxy generation combined with
 * custom security abstraction layers. All CRUD operations automatically enforce privilege checks
 * before execution.

 * <p>
 * The {@code @SearchableRepositoryMetadata} annotation configures full-text search indexing:
 * <ul>
 *   <li><b>entityKey</b>: Bound to {@code AbstractFileController.fileUrl} constant, tying the search
 *       index key to controller URL routing for file access</li>
 *   <li><b>descriptionFormula</b>: {@code "(''||filename)"} - Simple textual description derived
 *       from {@code File.filename} field for display in search results</li>
 *   <li><b>entityClass</b>: {@code File.class} - Specifies the JPA entity type being indexed</li>
 *   <li><b>searchIndexFormula</b>: Builds normalized lowercase search index including filename,
 *       organization ID scoping via {@code " orgid:"} token concatenated with
 *       {@code COALESCE(CAST(organization_id AS text), '')}, and additional organization-related
 *       fields via {@code DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA}</li>
 * </ul>
 * This enables searches by filename and organization-level references with case-insensitive matching.

 * <p>
 * <b>Warning</b>: Changing annotation formulas, referenced constants
 * ({@code AbstractFileController.fileUrl}, {@code ModelConstants}), or {@code File} entity schema
 * can desynchronize search indexes requiring manual index rebuilding.

 * <p>
 * Example usage:
 * <pre>{@code
 * Optional<File> file = secureFileRepository.findOne(fileId);
 * }</pre>

 * <p>
 * <b>Design Notes</b>: This follows the marker interface pattern with declarative annotation metadata,
 * combining raw database operations (via {@code FileRepository}) with security enforcement and search
 * indexing. Spring Data JPA generates the implementation at runtime as a thread-safe proxy.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SecureRepository
 * @see FileRepository
 * @see File
 * @see SearchableRepositoryMetadata
 * @see AbstractFileController
 */
@Repository
@SearchableRepositoryMetadata(
        entityKey = AbstractFileController.fileUrl,
        descriptionFormula = "(''||filename)",
        entityClass = File.class,
        searchIndexFormula = "lower(filename || "
                + " ' orgid:' || COALESCE(CAST (organization_id as text), '') || ' ' || "
                + DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA + ")"
)
public interface SecureFileRepository extends SecureRepository<File> {



}
