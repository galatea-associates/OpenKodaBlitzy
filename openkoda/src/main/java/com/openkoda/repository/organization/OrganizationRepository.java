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

package com.openkoda.repository.organization;

import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.Organization;
import com.openkoda.model.common.ModelConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Spring Data JPA repository managing Organization entities for multi-tenant operations with security-aware queries and hierarchical organization support.
 * <p>
 * This repository provides comprehensive data access operations for the {@code Organization} entity, which represents
 * tenants in OpenKoda's multi-tenant architecture. It implements both secured and unsecured query methods to support
 * different access control requirements across the application.

 * <p>
 * <b>Multi-Tenant Operations:</b> Provides queries for organization/tenant lookup by name, status, parent relationships,
 * and datasource assignments. Organizations can be hierarchically structured with parent-child relationships for
 * organizational hierarchy management.

 * <p>
 * <b>Security Model:</b> Implements both secured methods (using {@code CHECK_CAN_READ_ORG_DATA_OR_IS_ORG_MEMEBER_JPQL}
 * fragment) and unsecured direct lookup methods. The security fragment from {@code HasSecurityRules} enforces row-level
 * access control, ensuring users can only read/delete organizations where they have appropriate privileges or membership.

 * <p>
 * <b>Properties Bag Persistence:</b> Persists to 'organizations' table with JSONB properties column for flexible
 * tenant configuration. Each organization can store arbitrary key-value properties for customization.

 * <p>
 * <b>Datasource Mapping:</b> Provides native PostgreSQL query for organization-to-datasource assignment aggregation,
 * enabling multi-database tenant isolation strategies.

 * <p>
 * <b>Usage by OrganizationService:</b> Primary repository used by {@code OrganizationService} for tenant provisioning,
 * management, and removal operations.

 * <p>
 * Example usage:
 * <pre>
 * Organization org = organizationRepository.findByName("TenantCo");
 * Map&lt;Long, Integer&gt; datasourceMap = organizationRepository.findOrganizationToDatasourceIndexMap();
 * </pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.Organization
 * @see com.openkoda.service.organization.OrganizationService
 * @see UnsecuredFunctionalRepositoryWithLongId
 * @see HasSecurityRules
 */
@Repository
public interface OrganizationRepository extends UnsecuredFunctionalRepositoryWithLongId<Organization>, ModelConstants, HasSecurityRules {

    /**
     * Retrieves a paginated list of Organization entities with privilege-based access control.
     * <p>
     * This method uses the {@code CHECK_CAN_READ_ORG_DATA_OR_IS_ORG_MEMEBER_JPQL} security fragment to filter
     * results based on the current user's privileges and organization memberships. Only organizations that the
     * user has permission to read or is a member of will be included in the results.

     *
     * @param page Pagination parameters including page number, size, and sort criteria
     * @return Page of Organization entities accessible to the current user based on privilege checks
     */
    @Override
    @Query("select dbOrganization from Organization dbOrganization WHERE " + CHECK_CAN_READ_ORG_DATA_OR_IS_ORG_MEMEBER_JPQL)
    Page<Organization> findAll(Pageable page);

    /**
     * Retrieves a single Organization by ID with privilege enforcement.
     * <p>
     * Returns the organization only if the current user has appropriate read privileges or is a member of the
     * organization. Returns {@code null} if the organization doesn't exist or the user lacks access permissions.

     *
     * @param organizationId Organization entity identifier
     * @return Organization entity if found and accessible, {@code null} otherwise
     */
    @Override
    @Query("select dbOrganization from Organization dbOrganization where dbOrganization.id = :id AND " + CHECK_CAN_READ_ORG_DATA_OR_IS_ORG_MEMEBER_JPQL)
    Organization findOne(@Param("id") Long organizationId);

    /**
     * Retrieves Organization by ID without privilege checks.
     * <p>
     * <b>Warning:</b> This method bypasses security checks. Use only in trusted contexts or with manual privilege
     * validation. For privilege-enforced queries, use {@code findOne(Long)} instead.

     *
     * @param id Organization identifier
     * @return Organization entity if found, {@code null} otherwise
     * @see #findOne(Long)
     */
    @Query("select o from Organization o where o.id = :id")
    Organization findById(@Param("id") long id);

    /**
     * Retrieves Organization by unique name without privilege enforcement.
     * <p>
     * <b>Warning:</b> No security fragment applied. Validate privileges manually if needed. The name comparison
     * is case-sensitive.

     *
     * @param name Organization name (case-sensitive)
     * @return Organization entity if found, {@code null} if no organization with given name exists
     */
    @Query("select o from Organization o where o.name = :name")
    Organization findByName(@Param("name") String name);

    /**
     * Deletes Organization by ID with privilege enforcement.
     * <p>
     * Uses {@code CHECK_CAN_READ_ORG_DATA_OR_IS_ORG_MEMEBER_JPQL} to enforce delete permissions. The organization
     * will only be deleted if the current user has appropriate privileges or membership.

     *
     * @param aLong Organization identifier to delete
     * @return {@code true} if organization deleted successfully, {@code false} if not found or access denied
     */
    @Override
    @Query("delete from Organization dbOrganization where dbOrganization.id = :id AND " + CHECK_CAN_READ_ORG_DATA_OR_IS_ORG_MEMEBER_JPQL)
    boolean deleteOne(@Param("id") Long aLong);

    /**
     * Streams all organization IDs for batch processing and memory-efficient iteration.
     * <p>
     * Returns a stream of organization IDs suitable for processing large result sets without loading all entities
     * into memory. Ideal for bulk operations, reporting, and background job processing.

     *
     * @return Stream of organization IDs for processing large result sets
     */
    @Query("select id from Organization o")
    Stream<Long> findAllIdsAsStream();

    /**
     * Retrieves IDs of active organizations excluding disabled tenants.
     * <p>
     * Disabled organizations are identified by naming convention: names starting with '(disabled)' prefix.
     * This method filters out such organizations to return only active tenant IDs.

     *
     * @return List of organization IDs where name does not start with '(disabled)' prefix
     */
    @Query("select o.id from Organization o where o.name not like '(disabled)%'")
    List<Long> findActiveOrganizationIdsAsList();

    /**
     * Checks if Organization with given ID exists.
     *
     * @param id Organization identifier
     * @return {@code true} if organization exists, {@code false} otherwise
     */
    boolean existsById(Long id);


    /**
     * Persists or updates an Organization entity within a transaction.
     * <p>
     * Annotated with {@code @Transactional} to ensure atomic write operation. If the entity is new (no ID),
     * it will be persisted with a generated ID. If it already exists, it will be updated.

     *
     * @param entity Organization entity to save or update
     * @param <S> Organization or subtype
     * @return Saved Organization entity with generated ID if new
     */
    @Transactional
    @Override
    <S extends Organization> S save(S entity);

    /**
     * Executes native PostgreSQL query to aggregate organization IDs by assigned datasource.
     * <p>
     * <b>Warning:</b> PostgreSQL-specific query using {@code string_agg} and {@code ::} cast syntax - not portable
     * to other databases. Returns raw aggregation suitable for processing by {@code findOrganizationToDatasourceIndexMap()}.

     *
     * @return List of Object[] rows where [0]=datasource_index (Number), [1]=comma-separated org IDs (String)
     * @see #findOrganizationToDatasourceIndexMap()
     */
    @Query(nativeQuery = true, value = "select assigned_datasource, string_agg(id\\:\\:varchar, ',') from organization group by assigned_datasource")
    List<Object[]> findDatasourceAssignments();
    /**
     * Builds in-memory map of organization IDs to datasource indices for routing queries.
     * <p>
     * Executes {@code findDatasourceAssignments()}, parses comma-separated IDs, skips blank values, and populates
     * a map from organization ID to datasource index. This map is used for multi-database tenant isolation strategies.

     * <p>
     * <b>Thread-safety:</b> Single-thread safe (allocates fresh collections) but not synchronized for concurrent access.

     * <p>
     * <b>Memory:</b> Materializes all mapped IDs into memory - suitable for moderate organization counts.

     * <p>
     * Example usage:
     * <pre>
     * Map&lt;Long, Integer&gt; dsMap = organizationRepository.findOrganizationToDatasourceIndexMap();
     * Integer datasourceIndex = dsMap.get(organizationId);
     * </pre>

     *
     * @return HashMap mapping organization ID (Long) to datasource index (Integer)
     * @throws NumberFormatException If database contains invalid (non-numeric) organization ID tokens in aggregated string
     * @see #findDatasourceAssignments()
     */
    default Map<Long, Integer > findOrganizationToDatasourceIndexMap() {
        List<Object[]> datasourceAssignments = findDatasourceAssignments();
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] o: datasourceAssignments) {
            int datasourceIndex = ((Number)o[0]).intValue();
            String organizationsString = "" + o[1];
            if (StringUtils.isBlank(organizationsString)) {
                continue;
            }
            String[] organizations = organizationsString.split(",");
            for (String org : organizations) {
                result.put(Long.parseLong(org), datasourceIndex);
            }
        }
        return result;
    }


}
