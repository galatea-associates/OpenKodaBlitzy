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

import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.OpenkodaModule;
import com.openkoda.model.component.ControllerEndpoint;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository managing ControllerEndpoint entities representing dynamic HTTP endpoint configurations.
 * <p>
 * Extends {@link UnsecuredFunctionalRepositoryWithLongId} for functional CRUD operations without
 * automatic privilege enforcement. Implements {@link HasSecurityRules} to define custom security rules
 * and {@link ComponentEntityRepository} for module-scoped operations enabling component lifecycle management.
 * 
 * <p>
 * Manages ControllerEndpoint entities defining dynamic HTTP routes with path, method, and handler configurations.
 * Provides derived query methods for finding endpoints by frontend resource ID, sub-path, and HTTP method combinations.
 * Includes JPQL bulk delete operation for module cleanup scenarios.
 * 
 * <p>
 * Annotated with {@code @Repository} for Spring component scanning, bean detection, and exception translation
 * to Spring's DataAccessException hierarchy.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ControllerEndpoint
 * @see UnsecuredFunctionalRepositoryWithLongId
 * @see ComponentEntityRepository
 * @see OpenkodaModule
 */
@Repository
public interface ControllerEndpointRepository extends UnsecuredFunctionalRepositoryWithLongId<ControllerEndpoint>, HasSecurityRules, ComponentEntityRepository<ControllerEndpoint> {

    /**
     * Finds all controller endpoints associated with specified frontend resource.
     * <p>
     * Returns endpoints configured for the given frontend resource ID, enabling
     * retrieval of all dynamic routes bound to a specific resource.
     * 
     *
     * @param frontendResourceId frontend resource identifier, must be positive
     * @return list of endpoints for the resource, empty if none configured
     */
    List<ControllerEndpoint> findByFrontendResourceId(long frontendResourceId);

    /**
     * Finds unique endpoint matching resource ID, sub-path, and HTTP method.
     * <p>
     * Locates a specific controller endpoint by combining frontend resource ID,
     * URL sub-path segment, and HTTP method. Used for endpoint resolution and
     * route lookup in dynamic request handling.
     * 
     *
     * @param frontendResourceId frontend resource identifier
     * @param subPath URL sub-path segment (e.g., '/list', '/edit/{id}')
     * @param httpMethod HTTP method enum (GET, POST, PUT, DELETE, PATCH)
     * @return matching endpoint or null if not found
     */
    ControllerEndpoint findByFrontendResourceIdAndSubPathAndHttpMethod(long frontendResourceId, String subPath, ControllerEndpoint.HttpMethod httpMethod);
    
    /**
     * Finds organization-scoped endpoint matching resource, path, method, and tenant.
     * <p>
     * Locates endpoint with multi-tenant scoping by combining frontend resource ID,
     * sub-path, HTTP method, and organization ID. Enables tenant-specific endpoint
     * resolution in multi-tenancy scenarios.
     * 
     *
     * @param frontendResourceId frontend resource ID
     * @param subPath URL sub-path segment
     * @param httpMethod HTTP method
     * @param organizationId organization ID for multi-tenant scoping, nullable for global endpoints
     * @return matching endpoint or null if not found
     */
    ControllerEndpoint findByFrontendResourceIdAndSubPathAndHttpMethodAndOrganizationId(long frontendResourceId, String subPath, ControllerEndpoint.HttpMethod httpMethod, Long organizationId);

    /**
     * Bulk deletes all controller endpoints belonging to specified module.
     * <p>
     * Executes JPQL bulk delete query removing all ControllerEndpoint entities
     * associated with the given module. Used for module uninstallation and cleanup
     * scenarios to remove all dynamic endpoints registered by the module.
     * 
     * <p>
     * JPQL query: {@code "delete from ControllerEndpoint where module = :module"}
     * 
     * <p>
     * {@code @Modifying} annotation indicates write operation requiring transaction.
     * Bulk delete bypasses JPA lifecycle callbacks and cascades. Ensure dependent
     * entities are handled separately if cascade rules do not apply.
     * 
     *
     * @param module OpenkodaModule whose endpoints should be deleted, must not be null
     */
    @Modifying
    @Query("delete from ControllerEndpoint where module = :module")
    void deleteByModule(OpenkodaModule module);
}
