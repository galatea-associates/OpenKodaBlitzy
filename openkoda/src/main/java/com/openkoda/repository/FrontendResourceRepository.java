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

import com.openkoda.core.flow.Tuple;
import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.OpenkodaModule;
import com.openkoda.model.component.FrontendResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Repository managing FrontendResource entities representing UI components and dynamic pages.
 * <p>
 * Manages FrontendResource entities defining dynamic UI pages, forms, and components within the OpenKoda platform.
 * Provides derived finders for resource lookup by URL path, type, resource type, and organization. Supports JPQL 
 * projections to {@link Tuple} and streaming finders ({@code Stream<T>}) for large result sets. Includes 
 * module-scoped bulk delete ({@link #deleteByModule(OpenkodaModule)}) for component cleanup operations.
 * 
 * <p>
 * This repository extends {@link UnsecuredFunctionalRepositoryWithLongId} for functional CRUD operations,
 * {@link HasSecurityRules} for privilege-based security expressions, and {@link ComponentEntityRepository}
 * for module-scoped persistence semantics. It is used by frontend rendering controllers and 
 * {@code FrontendMappingDefinition} integration for dynamic page resolution and content delivery.
 * 
 * <p>
 * Key features include:
 * 
 * <ul>
 *   <li>Sitemap generation support via {@link #getEntriesToSitemap()}</li>
 *   <li>Name-based lookups with privilege enforcement</li>
 *   <li>URL path resolution with access level and organization scoping</li>
 *   <li>Streaming queries for draft resources and content existence checks</li>
 *   <li>Type and resource type filtering with pagination</li>
 *   <li>Direct content updates with native SQL for performance</li>
 *   <li>Dashboard definition queries by ID and name</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FrontendResource
 * @see ComponentEntityRepository
 * @see UnsecuredFunctionalRepositoryWithLongId
 * @see com.openkoda.core.form.FrontendMappingDefinition
 */
@Repository
public interface FrontendResourceRepository extends UnsecuredFunctionalRepositoryWithLongId<FrontendResource>, HasSecurityRules, ComponentEntityRepository<FrontendResource> {

    /**
     * Constant key for frontend resources collection in page models and result maps.
     * <p>
     * Used throughout controllers and services to reference frontend resource collections
     * in {@code PageModelMap} and {@code ResultAndModel} carriers. Provides consistent
     * key naming for resource lookups in Flow pipeline contexts.
     * 
     */
    String FRONTEND_RESOURCES = "frontendResources";

    /**
     * Retrieves all frontend resources eligible for sitemap inclusion.
     * <p>
     * Finds HTML pages that are unsecured (publicly accessible), marked as pages, and 
     * explicitly included in sitemap generation. Used by sitemap controllers to generate 
     * XML sitemaps for SEO optimization. Only resources with {@code type = 'HTML'} are returned.
     * 
     *
     * @return collection of FrontendResource entities meeting sitemap criteria, empty if none found
     * @see FrontendResource#isPage()
     * @see FrontendResource#getIncludeInSitemap()
     */
    @Query("select c from FrontendResource c where c.unsecured = TRUE and c.isPage = TRUE and c.includeInSitemap = TRUE and c.type = 'HTML'")
    Collection<FrontendResource> getEntriesToSitemap();

    /**
     * Finds a frontend resource by name with privilege enforcement.
     * <p>
     * Retrieves a frontend resource matching the specified name. Access is granted if the resource
     * is marked as unsecured (publicly accessible) OR if the current user has frontend resource 
     * management privileges or the required privilege specified in the resource definition.
     * 
     * <p>
     * Security check uses {@code CHECK_CAN_MANAGE_FRONTEND_RESOURCES_OR_HAS_REQUIRED_PRIVILEGE_JPQL}
     * to validate user permissions before returning the resource.
     * 
     *
     * @param name the unique name identifier of the frontend resource, must not be null
     * @return the matching FrontendResource entity, or null if not found or access denied
     * @see HasSecurityRules#CHECK_CAN_MANAGE_FRONTEND_RESOURCES_OR_HAS_REQUIRED_PRIVILEGE_JPQL
     */
    @Query("select dbFrontendResource from FrontendResource dbFrontendResource where dbFrontendResource.name = :name and " +
            "( dbFrontendResource.unsecured = TRUE OR " + CHECK_CAN_MANAGE_FRONTEND_RESOURCES_OR_HAS_REQUIRED_PRIVILEGE_JPQL + ")")
    FrontendResource findByName(@Param("name") String name);

    /**
     * Finds a frontend resource by URL path, organization, and access level with privilege validation.
     * <p>
     * Retrieves a frontend resource matching the specified URL path with organization-scoped and 
     * access-level filtering. Supports both global resources (organizationId null) and 
     * organization-specific resources. Access control is enforced through unsecured flag or
     * privilege verification.
     * 
     * <p>
     * Organization matching logic:
     * 
     * <ul>
     *   <li>If orgId is null: matches only resources with organizationId null (global resources)</li>
     *   <li>If orgId is not null: matches only resources with matching organizationId (tenant-scoped)</li>
     * </ul>
     *
     * @param urlPath the URL path or name of the resource, must not be null
     * @param organizationId the organization identifier for tenant scoping, nullable for global resources
     * @param accessLevel the required access level (PUBLIC, ORGANIZATION, GLOBAL), must not be null
     * @return the matching FrontendResource entity, or null if not found or access denied
     * @see FrontendResource.AccessLevel
     */
    @Query("select dbFrontendResource from FrontendResource dbFrontendResource " +
            "where dbFrontendResource.name = :urlPath and " +
            "(:orgId is null and dbFrontendResource.organizationId is null or :orgId is not null and dbFrontendResource.organizationId = :orgId) and " +
            "dbFrontendResource.accessLevel = :accessLevel and " +
            "( dbFrontendResource.unsecured = TRUE OR " + CHECK_CAN_MANAGE_FRONTEND_RESOURCES_OR_HAS_REQUIRED_PRIVILEGE_JPQL + ")")
    FrontendResource findByNameOrUrlPathAndOrganizationId(@Param("urlPath") String urlPath,
                                                          @Param("orgId") Long organizationId,
                                                          @Param("accessLevel") FrontendResource.AccessLevel accessLevel);

    /**
     * Finds a frontend resource by name without privilege enforcement.
     * <p>
     * Retrieves a frontend resource by name bypassing all privilege checks. This method should
     * only be used in system-level contexts where access control has already been validated
     * externally, such as background jobs, migrations, or administrative operations.
     * 
     * <p>
     * <b>Security Warning:</b> This method does not enforce privilege checks. Do not expose
     * this method directly in user-facing controllers or services.
     * 
     *
     * @param name the unique name identifier of the frontend resource, must not be null
     * @return the matching FrontendResource entity, or null if not found
     */
    @Query("select c from FrontendResource c where c.name = :name")
    FrontendResource findByNameUnsecured(@Param("name") String name);

    /**
     * Finds frontend resources by URL path or ID with priority-based organization and access level resolution.
     * <p>
     * Executes a complex JPQL query that retrieves page resources matching either URL path or resource ID,
     * with sophisticated priority ranking to handle organization-specific and global resource fallbacks.
     * Returns an {@code Object[]} containing the FrontendResource entity and its computed priority score.
     * 
     * <p>
     * Priority computation logic (lower values = higher priority):
     * 
     * <ul>
     *   <li>Priority 1: Organization-specific resources matching access level (PUBLIC, GLOBAL, ORGANIZATION)</li>
     *   <li>Priority 2: Global resources (organizationId null) matching access level</li>
     *   <li>Priority 3: GLOBAL resources when requesting ORGANIZATION access (org-specific fallback)</li>
     *   <li>Priority 4: GLOBAL resources when requesting ORGANIZATION access (global fallback)</li>
     *   <li>Priority 10: Default (no match)</li>
     * </ul>
     * <p>
     * Security enforcement uses Spring Security expressions to validate:
     * 
     * <ul>
     *   <li>Resource marked as unsecured, OR</li>
     *   <li>User has readFrontendResource or manageFrontendResource global privilege, OR</li>
     *   <li>Resource's requiredPrivilege is in user's global privileges</li>
     * </ul>
     *
     * @param urlPath the URL path name of the resource, nullable if frontendResourceId provided
     * @param frontendResourceId the resource identifier, nullable if urlPath provided
     * @param accessLevel the required access level (PUBLIC, ORGANIZATION, GLOBAL), must not be null
     * @param organizationId the organization identifier for tenant scoping, nullable for global resources
     * @param pageable pagination and sorting parameters, must not be null
     * @return Page of Object arrays where index 0 is FrontendResource and index 1 is priority Integer
     * @see FrontendResource.AccessLevel
     * @see org.springframework.data.domain.Pageable
     */
    @Query("""
            select dbFrontendResource,
            case 
                when dbFrontendResource.accessLevel = 'PUBLIC' and dbFrontendResource.organizationId is not null then 1
                when dbFrontendResource.accessLevel = 'PUBLIC' and dbFrontendResource.organizationId is null then 2
                when dbFrontendResource.accessLevel = 'GLOBAL' and dbFrontendResource.organizationId is not null then 1
                when dbFrontendResource.accessLevel = 'GLOBAL' and dbFrontendResource.organizationId is null then 2
                when dbFrontendResource.accessLevel = 'ORGANIZATION' and dbFrontendResource.organizationId is not null then 1
                when dbFrontendResource.accessLevel = 'ORGANIZATION' and dbFrontendResource.organizationId is null then 2
                when dbFrontendResource.accessLevel = 'GLOBAL' and 'ORGANIZATION' = cast(:accessLevel as text) and dbFrontendResource.organizationId is not null then 3
                when dbFrontendResource.accessLevel = 'GLOBAL' and 'ORGANIZATION' = cast(:accessLevel as text) and dbFrontendResource.organizationId is null then 4
                else 10 end as priority
            from FrontendResource dbFrontendResource where dbFrontendResource.isPage = TRUE and
                ((:urlPath is not null and dbFrontendResource.name = :urlPath) or (:frontendResourceId is not null and dbFrontendResource.id = :frontendResourceId))
                and (dbFrontendResource.accessLevel = :accessLevel or dbFrontendResource.accessLevel = 'GLOBAL' and 'ORGANIZATION' = :#{#accessLevel.name()})
                and (dbFrontendResource.organizationId = :orgId or dbFrontendResource.organizationId is null ) and
                ( dbFrontendResource.unsecured = TRUE OR ((?#{principal.hasGlobalPrivilege('readFrontendResource') OR principal.hasGlobalPrivilege('manageFrontendResource')}) = TRUE OR dbFrontendResource.requiredPrivilege IN ?#{principal.globalPrivileges}))
            """)
    Page<Object[]> findByUrlPathAndAccessLevelAndOrganizationId(@Param("urlPath") String urlPath,
                                                                        @Param("frontendResourceId") Long frontendResourceId,
                                                                        @Param("accessLevel") FrontendResource.AccessLevel accessLevel,
                                                                        @Param("orgId") Long organizationId,
                                                                        Pageable pageable);

    /**
     * Streams all draft frontend resources for batch processing.
     * <p>
     * Returns a {@code Stream<FrontendResource>} of resources marked as drafts. Streaming is memory-efficient
     * for processing large result sets without loading all entities into memory. The stream must be closed
     * after use to release database resources (use try-with-resources).
     * 
     * <p>
     * <b>Usage:</b>
     * 
     * <pre>
     * try (Stream&lt;FrontendResource&gt; drafts = repository.findAllAsStreamByIsDraftTrue()) {
     *     drafts.forEach(resource -&gt; processDraft(resource));
     * }
     * </pre>
     *
     * @return Stream of draft FrontendResource entities, must be closed after use
     * @see FrontendResource#isDraft()
     */
    @Query("select fr from FrontendResource fr where fr.draft = TRUE")
    Stream<FrontendResource> findAllAsStreamByIsDraftTrue();

    /**
     * Streams all frontend resources with existing content for batch processing.
     * <p>
     * Returns a {@code Stream<FrontendResource>} of resources where content has been populated.
     * Useful for content migration, validation, or export operations. Stream must be closed
     * after use to release database resources.
     * 
     *
     * @return Stream of FrontendResource entities with contentExists = true, must be closed after use
     * @see FrontendResource#isContentExists()
     */
    @Query("select fr from FrontendResource fr where fr.contentExists = TRUE")
    Stream<FrontendResource> findAllAsStreamByContentExists();

    /**
     * Streams all frontend resources without filtering for batch processing.
     * <p>
     * Returns a {@code Stream<FrontendResource>} of all resources in the repository. Use with caution
     * for large datasets. Stream must be closed after use to release database resources.
     * Best suited for export operations, full reindexing, or global content analysis.
     * 
     *
     * @return Stream of all FrontendResource entities, must be closed after use
     */
    @Query("select fr from FrontendResource fr")
    Stream<FrontendResource> findAllAsStream();

    /**
     * Finds frontend resources by type with pagination.
     * <p>
     * Retrieves resources matching the specified type (HTML, CSS, JS) with pagination support.
     * Results can be sorted using {@code Pageable} sort parameters.
     * 
     *
     * @param type the resource type to filter by, must not be null
     * @param pageable pagination and sorting parameters, must not be null
     * @return Page of FrontendResource entities matching the type
     * @see FrontendResource.Type
     */
    Page<FrontendResource> findByType(FrontendResource.Type type, Pageable pageable);

    /**
     * Finds frontend resources by resource type with pagination.
     * <p>
     * Retrieves resources matching the specified resource type (RESOURCE, UI_COMPONENT, DASHBOARD)
     * with pagination support.
     * 
     *
     * @param resourceType the resource type classification, must not be null
     * @param pageable pagination and sorting parameters, must not be null
     * @return Page of FrontendResource entities matching the resource type
     * @see FrontendResource.ResourceType
     */
    Page<FrontendResource> findByResourceType(FrontendResource.ResourceType resourceType, Pageable pageable);

    /**
     * Finds frontend resources by resource type with case-insensitive search filtering.
     * <p>
     * Retrieves resources matching the specified resource type where the indexString field
     * contains the search term (case-insensitive). Supports full-text search across indexed
     * content with pagination.
     * 
     *
     * @param resourceType the resource type classification, must not be null
     * @param search the search term to match against indexString field, must not be null
     * @param pageable pagination and sorting parameters, must not be null
     * @return Page of matching FrontendResource entities
     * @see FrontendResource#getIndexString()
     */
    Page<FrontendResource> findByResourceTypeAndIndexStringContainingIgnoreCase(FrontendResource.ResourceType resourceType, String search, Pageable pageable);

    /**
     * Finds frontend resources by type ordered by creation date descending.
     * <p>
     * Retrieves all resources of the specified type sorted by creation timestamp (newest first).
     * Returns a complete list without pagination. Use for recent content displays and
     * chronological listings.
     * 
     *
     * @param type the resource type to filter by, must not be null
     * @return List of FrontendResource entities ordered by createdOn descending, empty if none found
     */
    List<FrontendResource> findByTypeOrderByCreatedOnDesc(FrontendResource.Type type);

    /**
     * Updates the content of a frontend resource using native SQL for performance.
     * <p>
     * Executes a direct database UPDATE bypassing JPA entity lifecycle for efficient bulk content updates.
     * Requires user to have frontend resource management privileges (enforced via {@code @PreAuthorize}).
     * Additionally validates user has read access to the specific resource via JPQL expression.
     * 
     * <p>
     * <b>Note:</b> This operation bypasses JPA dirty checking and does not update audit timestamps
     * automatically. Consider using {@code saveOne()} if full entity lifecycle is required.
     * 
     *
     * @param id the frontend resource identifier, must not be null
     * @param content the new content to set, nullable to clear content
     * @return the number of rows updated (0 or 1), 0 if not found or access denied
     * @see HasSecurityRules#CHECK_CAN_MANAGE_FRONTEND_RESOURCES
     * @see HasSecurityRules#CHECK_CAN_READ_FRONTEND_RESOURCES_JPQL
     */
    @Modifying
    @PreAuthorize(CHECK_CAN_MANAGE_FRONTEND_RESOURCES)
        @Query(value = "update frontend_resource set content = :content where id = :id and ( " + CHECK_CAN_READ_FRONTEND_RESOURCES_JPQL + ") ", nativeQuery = true)
    Integer updateContent(@Param("id") Long id, @Param("content") String content);

    /**
     * Evicts and reloads a frontend resource from the database, bypassing first-level cache.
     * <p>
     * Forces a fresh database query for the specified resource, useful for ensuring cache consistency
     * after external modifications or concurrent updates. Delegates to {@link UnsecuredFunctionalRepositoryWithLongId#findOne(Long)} which
     * may trigger cache invalidation in the underlying repository implementation.
     * 
     *
     * @param id the frontend resource identifier, must not be null
     * @return the freshly loaded FrontendResource entity, or null if not found
     */
    default FrontendResource evictOne(Long id) {
        return findOne(id);
    }

    /**
     * Retrieves all frontend resources as lightweight Tuple projections (id, name).
     * <p>
     * Returns JPQL constructor expressions projecting only id and name fields for memory-efficient
     * lookups and dropdown population. Results are ordered alphabetically by name.
     * 
     *
     * @return List of Tuple instances containing (id, name) pairs, empty if no resources exist
     * @see Tuple
     */
    @Query("select new com.openkoda.core.flow.Tuple(fr.id, fr.name) FROM FrontendResource fr order by fr.name")
    List<Tuple> findAllAsTuple();

    /**
     * Retrieves embeddable resources as Tuple projections (id, name) ordered by name.
     * <p>
     * Finds resources marked as embeddable with resourceType = 'RESOURCE', suitable for
     * embedding in other pages or components. Returns lightweight projections for UI dropdown
     * and autocomplete functionality.
     * 
     *
     * @return List of Tuple instances containing (id, name) of embeddable resources, empty if none found
     * @see FrontendResource#isEmbeddable()
     * @see FrontendResource.ResourceType
     */
    @Query("select new com.openkoda.core.flow.Tuple(fr.id, fr.name) FROM FrontendResource fr where fr.embeddable = TRUE and fr.resourceType = 'RESOURCE' order by fr.name")
    List<Tuple> findAllEmbeddableResources();

    /**
     * Retrieves embeddable UI components as Tuple projections (id, name) ordered by name.
     * <p>
     * Finds UI components marked as embeddable with resourceType = 'UI_COMPONENT', suitable for
     * composition into larger page structures. Returns lightweight projections for component
     * selection interfaces.
     * 
     *
     * @return List of Tuple instances containing (id, name) of embeddable UI components, empty if none found
     * @see FrontendResource#isEmbeddable()
     * @see FrontendResource.ResourceType
     */
    @Query("select new com.openkoda.core.flow.Tuple(fr.id, fr.name) FROM FrontendResource fr where fr.embeddable = TRUE and fr.resourceType = 'UI_COMPONENT' order by fr.name")
    List<Tuple> findAllEmbeddableUiComponents();

    /**
     * Retrieves names of all non-embeddable resources as an array.
     * <p>
     * Finds resources with resourceType = 'RESOURCE' that are not marked as embeddable,
     * returning only their name strings. Useful for listing standalone resources that
     * cannot be embedded in other pages. Results are ordered alphabetically by name.
     * 
     *
     * @return Object array containing resource name strings, empty array if none found
     * @see FrontendResource#isEmbeddable()
     */
    @Query("select fr.name FROM FrontendResource fr where fr.embeddable = FALSE and fr.resourceType = 'RESOURCE' order by fr.name")
    Object[] findAllNonEmbeddableResourcesNames();

    /**
     * Finds a dashboard definition by identifier.
     * <p>
     * Retrieves a frontend resource with resourceType = 'DASHBOARD' matching the specified ID.
     * Dashboards are specialized frontend resources defining dashboard layouts and widget
     * configurations. Returns null if ID not found or resource is not a dashboard.
     * 
     *
     * @param id the dashboard resource identifier, must not be null
     * @return the FrontendResource dashboard definition, or null if not found
     * @see FrontendResource.ResourceType
     */
    @Query("select fr FROM FrontendResource fr where fr.id = :id and fr.resourceType = 'DASHBOARD'")
    FrontendResource findDashboardDefinition(@Param("id") Long id);

    /**
     * Finds a dashboard definition by name.
     * <p>
     * Retrieves a dashboard frontend resource by its unique name identifier. Dashboards
     * are specialized resources defining dashboard layouts. Returns null if name not found
     * or resource is not a dashboard type.
     * 
     *
     * @param name the dashboard name identifier, must not be null
     * @return the FrontendResource dashboard definition, or null if not found
     * @see FrontendResource.ResourceType
     */
    @Query("select fr FROM FrontendResource fr where fr.name = :name and fr.resourceType = 'DASHBOARD'")
    FrontendResource findDashboardDefinitionByName(@Param("name") String name);

    /**
     * Finds a frontend resource by name, access level, and organization using Spring Data query derivation.
     * <p>
     * Retrieves a resource matching all three criteria: unique name, access level constraint
     * (PUBLIC, ORGANIZATION, GLOBAL), and organization scoping. Useful for exact resource
     * resolution in multi-tenant contexts with access level verification.
     * 
     *
     * @param name the resource name identifier, must not be null
     * @param accessLevel the required access level, must not be null
     * @param organizationId the organization identifier for tenant scoping, nullable for global resources
     * @return the matching FrontendResource entity, or null if not found
     * @see FrontendResource.AccessLevel
     */
    FrontendResource findByNameAndAccessLevelAndOrganizationId(String name, FrontendResource.AccessLevel accessLevel, Long organizationId);

    /**
     * Bulk deletes all frontend resources belonging to the specified module.
     * <p>
     * Executes a JPQL bulk delete operation removing all resources associated with the given
     * {@link OpenkodaModule}. Used during module uninstallation and cleanup operations.
     * This operation requires a transaction and bypasses JPA entity lifecycle callbacks.
     * 
     * <p>
     * <b>Warning:</b> Bulk delete does not trigger JPA cascade operations or audit logging.
     * Ensure dependent entities (e.g., ControllerEndpoint) are cleaned up separately.
     * 
     *
     * @param module the OpenkodaModule whose resources should be deleted, must not be null
     * @see OpenkodaModule
     * @see ComponentEntityRepository#deleteByModule(OpenkodaModule)
     */
    @Modifying
    @Query("delete from FrontendResource where module = :module")
    void deleteByModule(OpenkodaModule module);

}
