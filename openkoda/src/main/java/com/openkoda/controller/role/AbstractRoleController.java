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

package com.openkoda.controller.role;

import com.openkoda.core.cache.RequestSessionCacheService;
import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.form.RoleForm;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.Role;
import jakarta.inject.Inject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Abstract base controller providing Role-Based Access Control (RBAC) role management operations.
 * <p>
 * Implements role lifecycle operations including role creation with privilege assignment, role editing
 * with privilege modification, role-user assignment and unassignment, and role deletion with dependency checks.
 * Supports three role types: GlobalRole (platform-wide permissions), OrganizationRole (organization-scoped permissions),
 * and GlobalOrganizationRole (global permissions within organization context). Uses single-table inheritance
 * for Role entities.
 * </p>
 * <p>
 * Subclasses provide concrete endpoint mappings for different access methods (HTML, API).
 * Delegates to {@code services.role} for role reconciliation and privilege synchronization.
 * Uses Flow pipeline pattern for request orchestration.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * PageModelMap result = findRole(roleId);
 * Role role = result.get("roleEntity");
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.service.role.RoleService
 * @see com.openkoda.model.Role
 * @see com.openkoda.model.Privilege
 */
public class AbstractRoleController extends AbstractController {

    /**
     * Request-scoped cache service for memoizing frequently-accessed data within a single HTTP request lifecycle.
     */
    @Inject private RequestSessionCacheService cacheService;
    
    /**
     * Lists roles within organization or globally with optional search filtering and pagination.
     * <p>
     * Uses secure repository to enforce privilege-based access control. Only roles visible to the current user
     * are returned based on their permissions. Search is performed on role names when aSearchTerm is provided.
     * </p>
     *
     * @param aSearchTerm optional text search term for filtering roles by name, null for no filtering
     * @param aSpecification JPA Specification for additional filtering criteria, null for no specification
     * @param aPageable pagination parameters including page number, size, and sort order
     * @return PageModelMap containing 'rolePage' with paginated search results of roles matching criteria
     */
    protected PageModelMap findRolesFlow(
            String aSearchTerm,
            Specification<Role> aSpecification,
            Pageable aPageable) {
        debug("[findRolesFlow] search {}", aSearchTerm);
        return Flow.init()
                .thenSet(rolePage, a -> repositories.secure.role.search(aSearchTerm, null, aSpecification, aPageable))
                .execute();
    }

    /**
     * Loads a single role by ID with form preparation and privilege enumeration.
     * <p>
     * Uses unsecure repository for internal operations. Pre-populates form with role data for edit scenarios.
     * For roleId of -1, initializes an empty form for creation workflow.
     * </p>
     *
     * @param roleId role identifier to retrieve, use -1 for new role form initialization
     * @return PageModelMap containing 'roleEntity' (Role), 'roleForm' (RoleForm pre-populated), 
     *         and 'rolesEnum' (all available privileges)
     */
    protected PageModelMap findRole(long roleId) {
        debug("[findRole] roleId {}", roleId);
        return Flow.init()
                .thenSet(roleEntity, a -> repositories.unsecure.role.findOne(roleId))
                .thenSet(roleForm, a -> new RoleForm(a.result))
                .thenSet(rolesEnum, a -> PrivilegeHelper.allEnumsToList())
                .execute();
    }

    /**
     * Creates new role with specified name, type, and privilege set.
     * <p>
     * Flow: Validates form → Checks role name uniqueness → Converts privilege identifier strings to
     * PrivilegeBase instances via {@code PrivilegeHelper.valueOfString()} → Delegates to 
     * {@code services.role.createRole()} for persistence → Resets form on success.
     * </p>
     * <p>
     * Privilege identifiers are mapped using PrivilegeHelper. Falls back to empty privilege set if 
     * dto.privileges is null. Validation failures are recorded in BindingResult without exception throwing.
     * </p>
     *
     * @param roleFormData form containing role name, type (GlobalRole/OrganizationRole/GlobalOrganizationRole), 
     *                     and selected privilege identifiers
     * @param br BindingResult for capturing validation errors and form-level failures
     * @return PageModelMap with validation results. On success: empty 'roleForm' for next creation. 
     *         On failure: original form with errors in BindingResult
     */
    protected PageModelMap createRole(RoleForm roleFormData, BindingResult br) {
        debug("[createRole]");
        return Flow.init(roleForm, roleFormData)
                .thenSet(rolesEnum, a -> PrivilegeHelper.allEnumsToList())
                .then(a -> services.role.checkIfRoleNameAlreadyExists(roleFormData.dto.name, roleFormData.dto.type, br))
                .then(a -> services.validation.validate(roleFormData, br))
                .then(a -> roleFormData.dto.privileges != null ?
                        roleFormData.dto.privileges.stream().map(PrivilegeHelper::valueOfString).collect(Collectors.toSet())
                        : new HashSet<PrivilegeBase>()
                )
                .then(a -> services.role.createRole(roleFormData.dto.name, roleFormData.dto.type, a.result))
                .thenSet(roleForm, a -> new RoleForm())
                .execute();
    }

    /**
     * Deletes role with atomic cleanup of user-role associations.
     * <p>
     * Cascade deletion pattern: First removes UserRole associations via 
     * {@code repositories.unsecure.userRole.deleteUserRoleByRoleId()}, then deletes Role entity.
     * This prevents orphaned UserRole records. Transaction ensures atomicity with rollback on any failure.
     * </p>
     * <p>
     * Warning: No validation for roles in use. Caller must implement business rule checks 
     * (e.g., prevent deleting last admin role).
     * </p>
     *
     * @param roleId role identifier to delete
     * @return PageModelMap indicating operation completion
     */
    @Transactional
    public PageModelMap deleteRole(long roleId) {
        debug("[deleteRole] roleId {}", roleId);
        return Flow.init()
                .then(a -> repositories.unsecure.userRole.deleteUserRoleByRoleId(roleId))
                .then(a -> repositories.unsecure.role.deleteRole(roleId))
                .execute();
    }

    /**
     * Updates existing role with modified name and privilege set.
     * <p>
     * Flow: Loads existing role via {@code repositories.unsecure.role.findOne()} → Validates and merges
     * form data via {@code services.validation.validateAndPopulateToEntity()} → Persists updated role →
     * Triggers privilege change notification via {@code services.privilege.notifyOnPrivilagesChange()}.
     * </p>
     * <p>
     * Privilege change notification triggers recalculation of effective privileges for all users with this role.
     * Updates UserRole effective privilege caches.
     * </p>
     *
     * @param roleId role identifier to update
     * @param roleFormData form containing updated role name and privilege selections
     * @param br BindingResult for validation error capture
     * @return PageModelMap containing updated 'roleEntity'. Validation errors recorded in BindingResult
     */
    protected PageModelMap updateRole(long roleId, RoleForm roleFormData, BindingResult br) {
        debug("[updateRole] roleId {}", roleId);
        return Flow.init(roleForm, roleFormData)
                .thenSet(rolesEnum, a -> PrivilegeHelper.allEnumsToList())
                .then(a -> repositories.unsecure.role.findOne(roleId))
                .then(a -> services.validation.validateAndPopulateToEntity(roleFormData, br,a.result))
                .thenSet(roleEntity, a -> repositories.unsecure.role.save(a.result))
                .then(a -> services.privilege.notifyOnPrivilagesChange())
                .execute();
    }
}
