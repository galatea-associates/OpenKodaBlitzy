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

package com.openkoda.service.organization;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.multitenancy.MultitenancyService;
import com.openkoda.core.service.event.ApplicationEvent;
import com.openkoda.dto.OrganizationDto;
import com.openkoda.model.GlobalOrganizationRole;
import com.openkoda.model.Organization;
import com.openkoda.model.UserRole;
import jakarta.inject.Inject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Multi-tenant organization provisioning and lifecycle management service.
 * <p>
 * This service provides core functionality for tenant operations in OpenKoda's multi-tenancy architecture,
 * including organization creation, tenant provisioning, schema management, cascade deletion, role 
 * synchronization, and event emission. Organizations represent isolated data tenants with their own
 * database schemas, roles, and privileges.
 * </p>
 * <p>
 * Multi-tenancy implementation uses organization_id foreign key pattern throughout the data model,
 * with dynamic entities registered per tenant and secure repository enforcement at the data access layer.
 * Tenant resolution occurs via subdomain mapping or authenticated user context.
 * </p>
 * <p>
 * Organization lifecycle workflow:
 * <pre>
 * createOrganization → provision admin → create roles → assign privileges → 
 * initialize properties → create tenant tables → emit events
 * </pre>
 * </p>
 * <p>
 * Service methods are stateless and thread-safe via Spring transactional isolation. Multi-tenancy
 * behavior is controlled via configuration properties: multitenancy.enabled and 
 * multitenancy.schema.strategy.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Organization
 * @see MultitenancyService
 * @see com.openkoda.core.multitenancy
 */

@Service
public class OrganizationService extends ComponentProvider {

    /**
     * Autowired DataSource for explicit JDBC operations including stored procedure calls.
     * <p>
     * Used for direct database operations such as invoking the remove_organizations_by_id
     * stored procedure for cascade deletion of tenant data.
     * </p>
     */
    @Autowired
    private DataSource dataSource;

    /**
     * Injected MultitenancyService for tenant lifecycle operations.
     * <p>
     * Handles tenant schema creation, deletion, constraint management, and schema marking
     * operations across multiple datasources in multi-tenant deployments.
     * </p>
     *
     * @see MultitenancyService
     */
    @Inject
    protected MultitenancyService multitenancyService;

    /**
     * Creates a new organization and provisions tenant infrastructure with assigned datasource.
     * <p>
     * This method persists a new Organization entity, creates the tenant schema via MultitenancyService,
     * and emits an ORGANIZATION_CREATED event for downstream listeners to complete provisioning
     * (admin user creation, role initialization, privilege assignment).
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * Organization org = organizationService.createOrganization("acmecorp", 1);
     * }</pre>
     * </p>
     *
     * @param organizationName unique name for the organization, used for subdomain and identification
     * @param assignedDatasource datasource ID for tenant schema placement in multi-datasource deployments
     * @return persisted Organization entity with generated ID and tenant infrastructure
     * @throws RuntimeException if tenant schema creation fails or organization name is not unique
     * @see MultitenancyService#createTenant(Long)
     */
    public Organization createOrganization(String organizationName, Integer assignedDatasource) {
        debug("[createOrganization] {}", organizationName);
        Organization result = repositories.unsecure.organization.save(new Organization(organizationName, assignedDatasource));
        multitenancyService.createTenant(result.getId());
        services.applicationEvent.emitEvent(ApplicationEvent.ORGANIZATION_CREATED, new OrganizationDto(result));
        return result;
    }

    /**
     * Creates a new organization with optional trial setup flag.
     * <p>
     * This overloaded method persists a new Organization entity using the default datasource
     * and emits an ORGANIZATION_CREATED event with the trial flag. Event listeners use this
     * flag to provision trial-specific features, limitations, or expiration policies.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * Organization trial = organizationService.createOrganization("trial-org", true);
     * }</pre>
     * </p>
     *
     * @param organizationName unique name for the organization
     * @param setupTrial if true, provision organization with trial features and limitations
     * @return persisted Organization entity with generated ID
     * @throws RuntimeException if organization name is not unique
     */
    public Organization createOrganization(String organizationName, boolean setupTrial){
        debug("[createOrganization] {}", organizationName);
        Organization result = repositories.unsecure.organization.save(new Organization(organizationName));
        services.applicationEvent.emitEvent(ApplicationEvent.ORGANIZATION_CREATED, new OrganizationDto(result, setupTrial));
        return result;
    }

    /**
     * Removes an organization and cascades deletion to all associated tenant data.
     * <p>
     * This method invokes the PostgreSQL stored procedure remove_organizations_by_id to
     * cascade delete all tenant-scoped data including users, roles, privileges, dynamic entities,
     * files, events, and schema objects. This operation is irreversible and requires the
     * ORGANIZATION_DELETE privilege.
     * </p>
     * <p>
     * Uses explicit JDBC connection management to execute the stored procedure with organization
     * ID array parameter. Connection is properly released in finally block to prevent leaks.
     * </p>
     *
     * @param orgId the organization ID to delete
     * @return true if deletion succeeded, false otherwise
     * @throws RuntimeException wrapping SQLException if stored procedure execution fails
     * @see com.openkoda.model.Organization
     */
    public boolean removeOrganization(Long orgId) {
        debug("[removeOrganization] OrgId: {}", orgId);
        Connection connection = DataSourceUtils.getConnection(dataSource);
        boolean deleted = true;
        try (PreparedStatement preparedStatement = connection.prepareStatement("call remove_organizations_by_id(?)")) {
            Array org_ids = connection.createArrayOf("bigint", new Long[]{orgId});
            preparedStatement.setArray(1, org_ids);
            preparedStatement.execute();
        } catch (SQLException throwables) {
            deleted = false;
            throw new RuntimeException("sql exception in [removeOrganization]", throwables);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
        return true;
    }

    /**
     * Marks the tenant schema as deleted without immediate removal.
     * <p>
     * This method delegates to MultitenancyService to mark the organization's schema for
     * deferred deletion, allowing graceful shutdown of tenant operations before physical
     * schema removal. Useful for multi-phase organization deletion workflows.
     * </p>
     *
     * @param orgId the organization ID whose schema should be marked
     * @param assignedDatasource datasource ID where the tenant schema resides
     * @return the schema name that was marked for deletion
     * @see MultitenancyService#markSchemaAsDeleted(long, int)
     */
    public String markSchemaAsDeleted(long orgId, int assignedDatasource) {
        debug("[markSchemeAsRemoved] OrgId: {}", orgId);
        return multitenancyService.markSchemaAsDeleted(orgId, assignedDatasource);
    }

    /**
     * Drops foreign key constraints from the tenant schema to prepare for deletion.
     * <p>
     * This method delegates to MultitenancyService to remove schema-level constraints,
     * enabling subsequent schema drop operations without constraint violation errors.
     * Typically used as a pre-deletion step in organization removal workflows.
     * </p>
     *
     * @param orgId the organization ID whose schema constraints should be dropped
     * @param schemaName the name of the tenant schema
     * @param assignedDatasource datasource ID where the tenant schema resides
     * @return true if constraints were successfully dropped, false otherwise
     * @see MultitenancyService#dropSchemaConstraints(long, String, int)
     */
    public Boolean dropSchemaConstraints(long orgId, String schemaName, int assignedDatasource) {
        debug("[dropSchemaConstraints] OrgId: {}", orgId);
        return multitenancyService.dropSchemaConstraints(orgId, schemaName, assignedDatasource);
    }

    /**
     * Reconciles global organization roles within a specific organization.
     * <p>
     * This method synchronizes UserRole assignments for global organization roles by creating
     * missing role associations and deleting obsolete ones. Compares the desired state 
     * (dtoGlobalOrgRoles) with existing state (existingGlobalOrgRolesInOrganization) and
     * applies necessary create/delete operations to achieve consistency.
     * </p>
     * <p>
     * Used during organization configuration updates to ensure proper role provisioning
     * when global roles are enabled or disabled for a tenant.
     * </p>
     *
     * @param organizationId the organization ID to update roles for
     * @param allGlobalOrgRoles complete list of available GlobalOrganizationRole entities
     * @param dtoGlobalOrgRoles list of role names that should be active in the organization
     * @param existingGlobalOrgRolesInOrganization current UserRole assignments in the organization
     * @return true when reconciliation completes successfully
     * @see GlobalOrganizationRole
     * @see UserRole
     */
    public boolean updateGlobalOrgRolesInOrganization(Long organizationId, List<GlobalOrganizationRole> allGlobalOrgRoles,
                                                  List<String> dtoGlobalOrgRoles, List<UserRole> existingGlobalOrgRolesInOrganization){
        for(GlobalOrganizationRole gor : allGlobalOrgRoles){
            if(dtoGlobalOrgRoles.contains(gor.getName())){
                if(!has(existingGlobalOrgRolesInOrganization, gor)){
                    repositories.secure.userRole.saveOne(new UserRole(null, null, gor.getId(), organizationId));
                }
            } else {
                if(has(existingGlobalOrgRolesInOrganization,gor)){
                    repositories.secure.userRole.deleteOne(get(existingGlobalOrgRolesInOrganization, gor));
                }
            }
        }
        return true;
    }

    /**
     * Checks if a GlobalOrganizationRole exists in the list of UserRole assignments.
     * <p>
     * Helper method that performs null-safe comparison of role IDs to determine if
     * a specific global organization role is already assigned within the organization.
     * </p>
     *
     * @param existingGlobalOrgRolesInOrganization list of current UserRole assignments
     * @param gor the GlobalOrganizationRole to search for
     * @return true if the role is found in the list, false otherwise
     */
    private boolean has(List<UserRole> existingGlobalOrgRolesInOrganization, GlobalOrganizationRole gor) {
        for(UserRole ur : existingGlobalOrgRolesInOrganization){
            if(Objects.equals(ur.getRoleId(), gor.getId())){
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the UserRole entity matching a GlobalOrganizationRole from the assignment list.
     * <p>
     * Helper method that performs null-safe search for a UserRole by comparing role IDs.
     * Returns the first matching UserRole or null if no match is found.
     * </p>
     *
     * @param existingGlobalOrgRolesInOrganization list of current UserRole assignments
     * @param gor the GlobalOrganizationRole to find
     * @return matching UserRole entity, or null if not found
     */
    private UserRole get(List<UserRole> existingGlobalOrgRolesInOrganization, GlobalOrganizationRole gor) {
        for(UserRole ur : existingGlobalOrgRolesInOrganization){
            if(Objects.equals(ur.getRoleId(), gor.getId())){
                return ur;
            }
        }
        return null;
    }

    /**
     * Retrieves names of all global organization roles assigned to a specific organization.
     * <p>
     * This method queries UserRole records for the given organization and extracts role names,
     * providing a list of active global organization role names for display or validation purposes.
     * </p>
     *
     * @param organizationId the organization ID to query roles for
     * @return list of role names (strings) assigned to the organization, may be empty if no roles assigned
     * @see UserRole#getRoleName()
     */
    public List<String> getNamesOfGlobalOrgRolesInOrganization(Long organizationId){
        return services.userRole.getUserRolesForOrganization(organizationId)
                .stream()
                .map(UserRole::getRoleName)
                .collect(Collectors.toList());
    }


}
