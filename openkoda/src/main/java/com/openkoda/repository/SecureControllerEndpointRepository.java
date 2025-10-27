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
import com.openkoda.model.component.ControllerEndpoint;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.openkoda.controller.common.URLConstants.CONTROLLER_ENDPOINT;
import static com.openkoda.model.common.ModelConstants.DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA;

/**
 * Secure repository marker interface for ControllerEndpoint with SearchableRepositoryMetadata configuration.
 * <p>
 * This interface extends {@link SecureRepository} to provide privilege-enforced data access operations
 * for {@link ControllerEndpoint} entities. All repository methods automatically enforce read and write
 * privileges based on the current security context, ensuring that only authorized users can access
 * or modify controller endpoint definitions.
 * </p>
 * <p>
 * The interface is annotated with {@link SearchableRepositoryMetadata} to enable dynamic repository
 * discovery and search indexing. This metadata configuration defines:
 * <ul>
 *   <li>Entity key for URL routing and identification (CONTROLLER_ENDPOINT)</li>
 *   <li>Description formula for generating human-readable entity descriptions (sub_path)</li>
 *   <li>Search index formula for organization-scoped search functionality</li>
 * </ul>
 * These metadata attributes are used by SearchableRepositories at runtime to build entity metadata
 * maps and enable generic search and discovery capabilities across the application.
 * </p>
 * <p>
 * As a Spring Data JPA repository interface, this provides standard CRUD operations through the
 * SecureRepository parent interface, along with custom query methods defined in this interface.
 * All operations are automatically transactional and privilege-checked.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SecureRepository
 * @see ControllerEndpoint
 * @see SearchableRepositoryMetadata
 */
@Repository
@SearchableRepositoryMetadata(
        entityKey = CONTROLLER_ENDPOINT,
        descriptionFormula = "(''||sub_path)",
        entityClass = ControllerEndpoint.class,
        searchIndexFormula = DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA
)
public interface SecureControllerEndpointRepository extends SecureRepository<ControllerEndpoint> {

    /**
     * Finds a ControllerEndpoint by its frontend resource ID, HTTP method, and subpath.
     * <p>
     * This method performs a global-scope search to locate a controller endpoint matching
     * the specified criteria. The search uses JPA Criteria API with three equality conditions
     * combined with AND logic. The HTTP method string is converted to the
     * {@link ControllerEndpoint.HttpMethod} enum value for comparison.
     * </p>
     * <p>
     * The search operates with {@link SecurityScope#GLOBAL}, meaning it searches across all
     * accessible organizations based on the current user's privileges. If multiple endpoints
     * match the criteria (which should not occur due to uniqueness constraints), only the
     * first result is returned.
     * </p>
     *
     * @param frontendResourceId the ID of the frontend resource to which the endpoint belongs
     * @param httpMethod the HTTP method as a string (e.g., "GET", "POST"), which will be
     *                   converted to {@link ControllerEndpoint.HttpMethod} enum value
     * @param subPath the subpath of the endpoint (e.g., "/api/users")
     * @return the matching ControllerEndpoint, or {@code null} if no endpoint matches the criteria
     *         or if the result list is empty
     */
    default ControllerEndpoint findByFrontendResourceAndHttpMethodAndSubPath(Long frontendResourceId, String httpMethod, String subPath) {
        List<ControllerEndpoint> result = search(SecurityScope.GLOBAL, (root, query, builder) ->
                builder.and(
                        builder.equal(root.get("frontendResourceId"), frontendResourceId),
                        builder.equal(root.get("httpMethod"), ControllerEndpoint.HttpMethod.valueOf(httpMethod)),
                        builder.equal(root.get("subPath"), subPath)));
        return result == null || result.isEmpty() ? null : result.get(0);
    }

}

