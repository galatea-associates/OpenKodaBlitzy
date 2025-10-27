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

import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.form.RoleForm;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import static com.openkoda.controller.common.URLConstants._HTML_ROLE;

/**
 * Concrete HTML role management controller with RBAC privilege selection UI.
 * <p>
 * Provides web-based role CRUD interface: role list with user count and pagination, create form with
 * privilege checkboxes grouped by category (Organization, User, Admin, Integration, Reports, Files),
 * edit form with current privilege assignments, role-user assignment capabilities, and delete confirmation
 * with dependency warnings. All routes mapped under _HTML_ROLE base path.
 * </p>
 * <p>
 * Extends AbstractRoleController to inherit business logic while providing HTTP endpoint bindings and
 * ModelAndView response generation. Uses Flow pipeline pattern for request orchestration and privilege-based
 * authorization via @PreAuthorize annotations.
 * </p>
 * <p>
 * <b>Request Mapping:</b> Base path: _HTML_ROLE constant (typically /html/role)
 * </p>
 * <p>
 * <b>Authorization:</b> Read endpoints require CHECK_CAN_READ_BACKEND privilege. Mutating endpoints require
 * CHECK_CAN_MANAGE_BACKEND privilege.
 * </p>
 * <p>
 * <b>Role Types:</b>
 * <ul>
 *   <li>GlobalRole: Platform-wide permissions applying across all organizations (e.g., Super Admin)</li>
 *   <li>OrganizationRole: Organization-scoped permissions limited to specific tenant (e.g., Org Admin, Org User)</li>
 *   <li>GlobalOrganizationRole: Global permissions within organization context - hybrid scope</li>
 * </ul>
 * </p>
 * <p>
 * <b>Privilege Categories (UI Groupings):</b>
 * <ul>
 *   <li>Organization: canReadOrgData, canManageOrgData, canManageOrganizations</li>
 *   <li>User: canReadUsers, canManageUsers, canImpersonateUsers</li>
 *   <li>Admin: canAccessGlobalSettings, canReadLogs, canManageIntegrations</li>
 *   <li>Integration: canUseIntegrations, canManageIntegrations</li>
 *   <li>Reports: canCreateReports, canScheduleReports</li>
 *   <li>Files: canUploadFiles, canManageFiles</li>
 * </ul>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractRoleController
 * @see com.openkoda.form.RoleForm
 * @see com.openkoda.model.Role
 */
@Controller
@ResponseBody
@RequestMapping(_HTML_ROLE)
public class RoleControllerHtml extends AbstractRoleController implements HasSecurityRules {

    /**
     * Displays paginated list of all roles with optional text search filtering.
     * <p>
     * <b>Endpoint:</b> GET {_HTML_ROLE}/_ALL - Lists all roles accessible to current user
     * </p>
     * <p>
     * Delegates to AbstractRoleController.findRolesFlow(). Applies privilege-based filtering via secure
     * repository to ensure only roles visible to current user are returned.
     * </p>
     *
     * @param rolePageable Pagination configuration (page, size, sort) qualified as 'role' bean
     * @param search Optional search term for filtering roles by name (defaults to empty string)
     * @return ModelAndView rendering 'role-all' template with 'rolePage' containing search results
     */
    @PreAuthorize(CHECK_CAN_READ_BACKEND)
    @GetMapping(_ALL)
    public Object getAll(
            @Qualifier("role") Pageable rolePageable,
            @RequestParam(required = false, defaultValue = "", name = "role_search") String search) {
        debug("[getAll] search {}", search);
        return findRolesFlow(search, null, rolePageable)
                .mav(ROLE + "-" + ALL);
    }

    /**
     * Displays role details and edit form with current privilege assignments.
     * <p>
     * <b>Endpoint:</b> GET {_HTML_ROLE}/{id}/settings - Role detail view
     * </p>
     * <p>
     * <b>UI Components:</b> Privilege checkboxes organized by category:
     * Organization (canReadOrgData, canManageOrgData), User (canReadUsers, canManageUsers),
     * Admin (canAccessGlobalSettings), Integration (canUseIntegrations),
     * Reports (canCreateReports), Files (canUploadFiles)
     * </p>
     *
     * @param roleId Role identifier to display (path variable 'id')
     * @return ModelAndView rendering 'role-settings' template with 'roleEntity', 'roleForm' pre-populated
     *         with current values, 'rolesEnum' containing all available privileges grouped by category
     */
    @PreAuthorize(CHECK_CAN_READ_BACKEND)
    @GetMapping(_ID_SETTINGS)
    public Object settings(@PathVariable(ID) Long roleId) {
        debug("[settings] roleId {}", roleId);
        return findRole(roleId)
                .mav("role-settings");
    }

    /**
     * Updates existing role with modified name and privilege selections.
     * <p>
     * <b>Endpoint:</b> POST {_HTML_ROLE}/{id}/settings - Submits role updates
     * </p>
     * <p>
     * <b>Flow:</b> Delegates to AbstractRoleController.updateRole() → Triggers
     * services.privilege.notifyOnPrivilagesChange() → Recalculates effective privileges for affected users
     * </p>
     * <p>
     * Returns Thymeleaf fragments for AJAX form submission. Success fragment shows confirmation,
     * error fragment displays validation messages.
     * </p>
     *
     * @param roleId Role identifier to update (path variable 'id')
     * @param roleForm Validated form with updated role name and selected privilege identifiers
     * @param br BindingResult capturing validation errors
     * @return Partial view response: 'role-settings-form-success' fragment on success,
     *         'role-settings-form-error' fragment with validation errors on failure
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @PostMapping(_ID_SETTINGS)
    public Object update(@PathVariable(ID) Long roleId, @Valid RoleForm roleForm, BindingResult br) {
        debug("[update] roleId {}", roleId);
        return updateRole(roleId, roleForm, br)
                .mav(ENTITY + '-' + FORMS + "::role-settings-form-success",
                        ENTITY + '-' + FORMS + "::role-settings-form-error");
    }

    /**
     * Displays new role creation form with empty fields and privilege selector.
     * <p>
     * <b>Endpoint:</b> GET {_HTML_ROLE}/new/settings - New role form
     * </p>
     * <p>
     * <b>Implementation Detail:</b> Calls findRole(-1L) to initialize empty form. Magic value -1 signals
     * new role creation rather than edit.
     * </p>
     *
     * @return ModelAndView rendering 'role-settings' template with empty 'roleForm', 'rolesEnum' for
     *         privilege selection
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @GetMapping(_NEW_SETTINGS)
    public Object create() {
        debug("[create]");
        return findRole(-1L)
                .mav("role-settings");
    }

    /**
     * Creates new role with specified name, type, and privilege set.
     * <p>
     * <b>Endpoint:</b> POST {_HTML_ROLE}/new/settings - Submits new role creation
     * </p>
     * <p>
     * <b>Validation:</b> Checks role name uniqueness via services.role.checkIfRoleNameAlreadyExists().
     * Validates form via services.validation.validate()
     * </p>
     * <p>
     * <b>Flow:</b> Form validation → Privilege string-to-enum conversion via PrivilegeHelper.valueOfString()
     * → services.role.createRole() persistence → Form reset for next creation
     * </p>
     *
     * @param roleForm Validated form containing role name, type (GlobalRole/OrganizationRole/GlobalOrganizationRole),
     *                 selected privilege identifiers
     * @param br BindingResult for validation error capture
     * @return Partial view: 'role-settings-form-success' on successful creation, 'role-settings-form-error'
     *         with errors on validation failure
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @PostMapping(_NEW_SETTINGS)
    public Object saveNew(@Valid RoleForm roleForm, BindingResult br) {
        debug("[saveNew]");
        return createRole(roleForm, br)
                .mav(ENTITY + '-' + FORMS + "::role-settings-form-success",
                        ENTITY + '-' + FORMS + "::role-settings-form-error");
    }

    /**
     * Deletes role with atomic cleanup of user-role associations.
     * <p>
     * <b>Endpoint:</b> POST {_HTML_ROLE}/{id}/remove - Deletes role
     * </p>
     * <p>
     * <b>Transaction:</b> Inherited from AbstractRoleController.deleteRole(). Atomic operation: deletes
     * UserRole associations first, then Role entity
     * </p>
     * <p>
     * <b>Cascade:</b> Removes all UserRole associations via repositories.unsecure.userRole.deleteUserRoleByRoleId()
     * before role deletion
     * </p>
     * <p>
     * <b>Warning:</b> No validation for roles in use. Business logic should prevent deleting roles assigned to
     * users or system-critical roles. UI should confirm deletion with dependency warnings.
     * </p>
     *
     * @param roleId Role identifier to delete (path variable 'id')
     * @return Boolean response: true on success, false on failure
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @PostMapping(_ID_REMOVE)
    public Object delete(@PathVariable(ID) Long roleId) {
        debug("[delete] roleId {}", roleId);
        return deleteRole(roleId)
                .mav( a -> true, a -> false);
    }
}
