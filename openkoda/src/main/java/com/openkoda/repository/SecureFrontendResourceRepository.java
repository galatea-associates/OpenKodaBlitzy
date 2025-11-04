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

package com.openkoda.repository;

import com.openkoda.model.common.SearchableRepositoryMetadata;
import com.openkoda.model.component.FrontendResource;
import org.springframework.stereotype.Repository;

import static com.openkoda.controller.common.URLConstants.FRONTENDRESOURCE;
import static com.openkoda.model.common.ModelConstants.DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA;

/**
 * Secure repository marker interface for FrontendResource entities with SearchableRepositoryMetadata.
 * <p>
 * Extends SecureRepository to provide privilege-enforced data access operations for UI frontend resources.
 * This interface enables dynamic repository discovery through SearchableRepositoryMetadata annotation,
 * facilitating frontend component indexing and search functionality across the application.
 * 
 * <p>
 * The repository automatically enforces security privileges for all CRUD operations on FrontendResource
 * entities, ensuring that only authorized users can access, create, modify, or delete frontend resources.
 * Search indexing includes resource name, type, and organization-related reference fields for
 * comprehensive discoverability.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SecureRepository
 * @see FrontendResource
 * @see SearchableRepositoryMetadata
 */
@Repository
@SearchableRepositoryMetadata(
        entityKey = "frontendResource",
        descriptionFormula = "(''||name)",
        entityClass = FrontendResource.class,
        searchIndexFormula = "lower(name || ' ' || type) || ' ' ||"
                + DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA
)
public interface SecureFrontendResourceRepository extends SecureRepository<FrontendResource> {


}
