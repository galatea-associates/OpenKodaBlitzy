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

package com.openkoda.controller.organization;

import com.openkoda.form.GlobalOrgRoleForm;
import com.openkoda.form.InviteUserForm;
import com.openkoda.form.OrganizationForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import static com.openkoda.controller.common.URLConstants._HTML_ORGANIZATION;
import static com.openkoda.core.service.FrontendResourceService.frontendResourceTemplateNamePrefix;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;

/**
 * Concrete HTML organization management controller providing multi-tenant UI for organization CRUD operations.
 * <p>
 * Provides organization list with search/filter, create form with property editor, edit form with member management,
 * organization switcher, and delete confirmation. Routes under /organizations. Extends AbstractOrganizationController
 * with HTML response handling and Flow pipeline integration.
 * </p>
 * <p>
 * This controller acts as a thin HTTP adapter layer that:
 * <ul>
 *   <li>Resolves HTTP bindings (path variables, request parameters, form objects)</li>
 *   <li>Delegates business logic to AbstractOrganizationController flow methods</li>
 *   <li>Converts Flow/PageModelMap results to ModelAndView for server-side rendered HTML</li>
 *   <li>Enforces organization-scoped security via @PreAuthorize annotations</li>
 *   <li>Returns Thymeleaf template fragments for HTMX-based UI updates</li>
 * </ul>
 * </p>
 * <p>
 * Key endpoints include organization dashboard, settings page, member management (invite/remove/change role),
 * audit history view, and complete organization deletion with cascade.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractOrganizationController
 */
@Controller
@ResponseBody
@RequestMapping(_HTML_ORGANIZATION)
public class OrganizationControllerHtml extends AbstractOrganizationController {

    /**
     * Renders the organization dashboard view with organization details and member list.
     * <p>
     * Endpoint: GET /organizations/{organizationId}/dashboard
     * </p>
     * <p>
     * Displays organization information including name, properties, and associated users with pagination.
     * Delegates to {@link AbstractOrganizationController#findOrganizationWithSettings(Long)} flow and renders
     * the dashboard template.
     * </p>
     *
     * @param organizationId the ID of the organization to display
     * @param userPageable pagination parameters for the user list (qualified as "user")
     * @param search optional search term for filtering users (defaults to empty string)
     * @param request the HTTP servlet request for context
     * @return ModelAndView with organization dashboard template containing organization entity and user list
     * @see AbstractOrganizationController#findOrganizationWithSettings(Long)
     */
    @PreAuthorize(CHECK_CAN_READ_ORG_DATA)
    @GetMapping(_ORGANIZATIONID + _DASHBOARD)
    public Object getOrganization(@PathVariable(ORGANIZATIONID) Long organizationId,
                      @Qualifier("user") Pageable userPageable,
                      @RequestParam(required = false, defaultValue = "", name = "user_search") String search,
                      HttpServletRequest request) {
        debug("[getOrganization] orgId {} search {}", organizationId, search);
        Object result = findOrganizationWithSettings(organizationId)
                .mav(request, frontendResourceTemplateNamePrefix + DASHBOARD, (a) -> a.get(organizationEntity));
        return result;
    }

    /**
     * Renders the organization settings page with editable organization properties and member management.
     * <p>
     * Endpoint: GET /organizations/{organizationId}/settings
     * </p>
     * <p>
     * Displays organization settings form with property bag editor, user membership list with search/pagination,
     * and module configuration. Supports multi-pageable with separate pagination for users and modules.
     * Delegates to {@link AbstractOrganizationController#getOrganizationSettings(Long, String, Pageable)} flow.
     * </p>
     *
     * @param organizationId the ID of the organization to display settings for
     * @param userPageable pagination parameters for the user list (qualified as "user")
     * @param userSearch optional search term for filtering users (defaults to empty string)
     * @param modulePageable pagination parameters for the module list (qualified as "module")
     * @param moduleSearch optional search term for filtering modules (defaults to empty string)
     * @param request the HTTP servlet request for context
     * @return ModelAndView with organization-settings template containing forms and lists
     * @see AbstractOrganizationController#getOrganizationSettings(Long, String, Pageable)
     */
    @PreAuthorize(CHECK_CAN_READ_ORG_DATA)
    @GetMapping(_ORGANIZATIONID + _SETTINGS)
    public Object settings(@PathVariable(ORGANIZATIONID) Long organizationId,
                           @Qualifier("user") Pageable userPageable,
                           @RequestParam(required = false, defaultValue = "", name = "user_search") String userSearch,
                           @Qualifier("module") Pageable modulePageable,
                           @RequestParam(required = false, defaultValue = "", name = "module_search") String moduleSearch,
                           HttpServletRequest request) {
        debug("[settings] orgId {} userSearch {} moduleSearch {}", organizationId, userSearch, moduleSearch);
        return getOrganizationSettings(organizationId, userSearch, userPageable)
                .mav(ORGANIZATION + "-settings");
    }

    /**
     * Changes the role of an existing user within the organization.
     * <p>
     * Endpoint: POST /organizations/{organizationId}/member
     * </p>
     * <p>
     * Updates the role assignment for a user in the specified organization. Delegates to
     * {@link AbstractOrganizationController#changeUserOrganizationRole(long, long, String)} flow
     * and returns boolean success/failure result.
     * </p>
     * <p>
     * Security: Requires CHECK_CAN_SAVE_USER_ROLES privilege.
     * </p>
     *
     * @param organizationId the ID of the organization
     * @param userId the ID of the user whose role is being changed
     * @param userRoleName the name of the new role to assign
     * @return Boolean true if role change succeeded, false otherwise
     * @see AbstractOrganizationController#changeUserOrganizationRole(long, long, String)
     */
    @PreAuthorize(CHECK_CAN_SAVE_USER_ROLES)
    @PostMapping(_ORGANIZATIONID + _MEMBER)
    public Object setUserRole(@PathVariable(ORGANIZATIONID) long organizationId,
                           @RequestParam(name = "userId") long userId, String userRoleName) {
        debug("[setUserRole] organizationId {} userId {}", organizationId, userId);
        return changeUserOrganizationRole(organizationId, userId, userRoleName)
                .mav(a -> true, a -> false);
    }

    /**
     * Renders the audit history view for an organization with paginated log entries.
     * <p>
     * Endpoint: GET /organizations/{organizationId}/history
     * </p>
     * <p>
     * Displays paginated audit log entries for the organization with optional search filtering.
     * Delegates to {@link AbstractOrganizationController#getHistory(Long, Pageable, String)} flow
     * and renders the organization-history template.
     * </p>
     * <p>
     * Security: Requires CHECK_CAN_MANAGE_ORG_DATA privilege.
     * </p>
     *
     * @param organizationId the ID of the organization to retrieve history for
     * @param auditPageable pagination parameters for the audit log (qualified as "audit")
     * @param search optional search term for filtering audit entries (defaults to empty string)
     * @param request the HTTP servlet request for context
     * @return ModelAndView with organization-history template containing paginated audit entries
     * @see AbstractOrganizationController#getHistory(Long, Pageable, String)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ORG_DATA)
    @GetMapping(_ORGANIZATIONID + _HISTORY)
    public Object history(@PathVariable(ORGANIZATIONID) Long organizationId,
                          @Qualifier("audit") Pageable auditPageable,
                          @RequestParam(required = false, defaultValue = "", name = "audit_search") String search,
                          HttpServletRequest request) {
        debug("[history] orgId {}, ");
        return getHistory(organizationId, auditPageable, search)
                .mav(ORGANIZATION + "-" + HISTORY);
    }

    /**
     * Renders the new organization creation form.
     * <p>
     * Endpoint: GET /organizations/new/settings
     * </p>
     * <p>
     * Displays an empty organization form for creating a new organization. Delegates to
     * {@link AbstractOrganizationController#getNewOrganizationSettings(Pageable)} flow
     * and renders the organization-new template.
     * </p>
     * <p>
     * Security: Requires CHECK_CAN_MANAGE_ORG_DATA privilege.
     * </p>
     *
     * @param userPageable pagination parameters for user selection (qualified as "user")
     * @return ModelAndView with organization-new template containing empty organization form
     * @see AbstractOrganizationController#getNewOrganizationSettings(Pageable)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ORG_DATA)
    @GetMapping(_NEW + _SETTINGS)
    public Object newOrganization(@Qualifier("user") Pageable userPageable) {
        debug("[newOrganization]");
        return getNewOrganizationSettings(userPageable)
                .mav(ORGANIZATION + '-' + NEW);
    }

    /**
     * Creates a new organization from submitted form data.
     * <p>
     * Endpoint: POST /organizations/new/settings
     * </p>
     * <p>
     * Validates the organization form and creates a new organization entity with initial configuration.
     * Delegates to {@link AbstractOrganizationController#createOrganization(OrganizationForm, BindingResult)} flow.
     * Returns success or error fragment based on validation and creation result.
     * </p>
     * <p>
     * Security: Requires CHECK_CAN_MANAGE_ORG_DATA privilege.
     * </p>
     *
     * @param organizationForm the validated organization form data containing name and properties
     * @param br binding result containing validation errors if any
     * @return ModelAndView with success fragment (entity-forms::organization-settings-form-success) on success,
     *         or error fragment (entity-forms::organization-settings-form-error) on validation failure
     * @see AbstractOrganizationController#createOrganization(OrganizationForm, BindingResult)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ORG_DATA)
    @PostMapping(_NEW + _SETTINGS)
    public Object saveNew(@Valid OrganizationForm organizationForm, BindingResult br) {
        debug("[saveNew]");
        return createOrganization(organizationForm, br)
                .mav(ENTITY + '-' + FORMS + "::organization-settings-form-success",
                        ENTITY + '-' + FORMS + "::organization-settings-form-error");
    }

    /**
     * Deletes an organization by ID.
     * <p>
     * Endpoint: DELETE /organizations/{organizationId}
     * </p>
     * <p>
     * Marks the organization for deletion. Delegates to
     * {@link AbstractOrganizationController#deleteOrganization(Long)} flow.
     * Uses HTTP DELETE method for RESTful semantics.
     * </p>
     * <p>
     * Security: Requires CHECK_CAN_MANAGE_ORG_DATA privilege.
     * </p>
     *
     * @param organizationId the ID of the organization to delete
     * @return Object result from the delete operation flow
     * @see AbstractOrganizationController#deleteOrganization(Long)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ORG_DATA)
    @RequestMapping(value = _ORGANIZATIONID, method = DELETE)
    public Object delete(@PathVariable(ORGANIZATIONID) Long organizationId) {
        debug("[delete] orgId {}", organizationId);
        return deleteOrganization(organizationId);
    }

    /**
     * Lists all organizations with pagination and optional search filtering.
     * <p>
     * Endpoint: GET /organizations/all
     * </p>
     * <p>
     * Displays a paginated list of all organizations with optional search filter. Delegates to
     * {@link AbstractOrganizationController#findOrganizationsFlow(String, org.springframework.data.jpa.domain.Specification, Pageable)}
     * and renders the organization-all template.
     * </p>
     * <p>
     * Note: TODO - Missing @PreAuthorize annotation per Rule 1.4.
     * </p>
     *
     * @param organizationPageable pagination parameters for the organization list (qualified as "organization")
     * @param search optional search term for filtering organizations (defaults to empty string)
     * @return ModelAndView with organization-all template containing paginated organization list
     * @see AbstractOrganizationController#findOrganizationsFlow(String, org.springframework.data.jpa.domain.Specification, Pageable)
     */
    @GetMapping(_ALL)
    //TODO Rule 1.4 All methods in non-public controllers must have @PreAuthorize
    public Object getAll(
            @Qualifier("organization") Pageable organizationPageable,
            @RequestParam(required = false, defaultValue = "", name = "organization_search") String search) {
        debug("[getAll] search {}", search);
        return findOrganizationsFlow(search, null, organizationPageable)
                .mav(ORGANIZATION + "-" + ALL);
    }

    /**
     * Updates an existing organization with submitted form data.
     * <p>
     * Endpoint: POST /organizations/{organizationId}/settings
     * </p>
     * <p>
     * Validates and saves changes to organization properties including name, property bag customization,
     * and other editable fields. Delegates to {@link AbstractOrganizationController#saveOrganization(Long, OrganizationForm, BindingResult)}
     * flow. Returns success or error fragment based on validation and save result.
     * </p>
     * <p>
     * Security: Requires CHECK_CAN_MANAGE_ORG_DATA privilege.
     * </p>
     *
     * @param organizationId the ID of the organization to update
     * @param form the organization form data containing updated values
     * @param br binding result containing validation errors if any
     * @return ModelAndView with success fragment (entity-forms::organization-settings-form-success) on success,
     *         or error fragment (entity-forms::organization-settings-form-error) on validation failure
     * @see AbstractOrganizationController#saveOrganization(Long, OrganizationForm, BindingResult)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ORG_DATA)
    @PostMapping(_ORGANIZATIONID +_SETTINGS)
    public Object post(
            @PathVariable(ORGANIZATIONID) Long organizationId,
            OrganizationForm form,
            BindingResult br) {
        debug("[post] orgId {}", organizationId);
        Object result = saveOrganization(organizationId, form, br)
                .mav(ENTITY + "-" + FORMS + "::organization-settings-form-success",
                ENTITY + "-" + FORMS + "::organization-settings-form-error");
        return result;
    }


    /**
     * Invites a new or existing user to join the organization with a specified role.
     * <p>
     * Endpoint: POST /organizations/{organizationId}/invite
     * </p>
     * <p>
     * Validates the invite form and adds the user to the organization. If the user does not exist,
     * creates a new user account and sends an invitation email. Delegates to
     * {@link AbstractOrganizationController#inviteUser(InviteUserForm, Long, BindingResult)} flow.
     * On success, redirects to organization settings page.
     * </p>
     * <p>
     * Security: Requires CHECK_CAN_MANAGE_ORG_DATA privilege.
     * </p>
     *
     * @param organizationId the ID of the organization to invite the user to
     * @param userFormData the validated invite form containing user email and role information
     * @param br binding result containing validation errors if any
     * @return ModelAndView with redirect fragment (generic-forms::go-to) on success directing to organization settings,
     *         or error fragment (entity-forms::invite-user-form-error) on validation failure
     * @see AbstractOrganizationController#inviteUser(InviteUserForm, Long, BindingResult)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ORG_DATA)
    @PostMapping(_ORGANIZATIONID + _INVITE)
    public Object inviteUser(@PathVariable(ORGANIZATIONID) Long organizationId, @Valid InviteUserForm userFormData, BindingResult
            br) {
        debug("[inviteUser] orgId {}", organizationId);
        return inviteUser(userFormData, organizationId, br)
                .mav(String.format("generic-forms::go-to(url='%s')", services.url.organizationSettings(organizationId)),
                        ENTITY + '-' + FORMS + "::invite-user-form-error");
    }

    /**
     * Adds a global organization role to the specified organization.
     * <p>
     * Endpoint: POST /organizations/{organizationId}/addGlobalOrgRole
     * </p>
     * <p>
     * Validates and assigns a global role that applies across the organization. Delegates to
     * {@link AbstractOrganizationController#globalOrgRole(GlobalOrgRoleForm, Long)} flow.
     * Returns success or error fragment based on validation and assignment result.
     * </p>
     * <p>
     * Security: Requires CHECK_CAN_ACCESS_GLOBAL_SETTINGS privilege (elevated permission).
     * </p>
     *
     * @param organizationId the ID of the organization to add the global role to
     * @param globalOrgRoleForm the validated global role form containing role configuration
     * @return ModelAndView with success fragment (entity-forms::global-org-role-form-success) on success,
     *         or error fragment (entity-forms::global-org-role-form-error) on validation failure
     * @see AbstractOrganizationController#globalOrgRole(GlobalOrgRoleForm, Long)
     */
    @PreAuthorize(CHECK_CAN_ACCESS_GLOBAL_SETTINGS)
    @PostMapping(_ORGANIZATIONID + "/addGlobalOrgRole")
    public Object addGlobalOrgRole(@PathVariable(ORGANIZATIONID) Long organizationId, @Valid GlobalOrgRoleForm globalOrgRoleForm){
        return globalOrgRole(globalOrgRoleForm, organizationId)
                .mav(ENTITY + '-' + FORMS + "::global-org-role-form-success",
                ENTITY + '-' + FORMS + "::global-org-role-form-error");
    }

    /**
     * Removes a user from the organization by deleting their user role association.
     * <p>
     * Endpoint: POST /organizations/{organizationId}/remove
     * </p>
     * <p>
     * Removes the user's membership in the organization by deleting the user role record.
     * Delegates to {@link AbstractOrganizationController#removeUserRole(long)} flow.
     * Returns boolean success/failure result.
     * </p>
     * <p>
     * Security: Requires CHECK_CAN_MANAGE_ORG_DATA privilege.
     * </p>
     *
     * @param organizationId the ID of the organization (used for routing and logging)
     * @param userRoleId the ID of the user role association to remove
     * @return Boolean true if user role removal succeeded, false otherwise
     * @see AbstractOrganizationController#removeUserRole(long)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ORG_DATA)
    @PostMapping(_ORGANIZATIONID + _REMOVE)
    public Object removeUser(@PathVariable(ORGANIZATIONID) Long organizationId,
                             @RequestParam(name = "userRoleId", required = true) long userRoleId
                             ) {
        debug("[removeUser] orgId {}", organizationId);
        return removeUserRole(userRoleId)
                .mav( a -> true, a -> false);
    }

    /**
     * Performs complete organization removal including all associated data and schema cleanup.
     * <p>
     * Endpoint: POST /organizations/{organizationId}/entity/remove
     * </p>
     * <p>
     * Executes cascade deletion of the organization including marking schema as deleted,
     * dropping schema constraints, and removing the organization entity. Delegates to
     * {@link AbstractOrganizationController#removeOrganization(Long)} flow which orchestrates
     * schema-level teardown operations. Returns boolean success/failure result.
     * </p>
     * <p>
     * Security: Requires CHECK_CAN_MANAGE_ORG_DATA privilege.
     * </p>
     *
     * @param organizationId the ID of the organization to completely remove
     * @return Boolean true if organization removal succeeded, false otherwise
     * @see AbstractOrganizationController#removeOrganization(Long)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ORG_DATA)
    @PostMapping(_ORGANIZATIONID + _ENTITY + _REMOVE)
    public Object removeOrganizationData(@PathVariable(ORGANIZATIONID) Long organizationId){
        debug("[removeOrganizationData] organizationId {}", organizationId);
        return removeOrganization(organizationId)
                .mav( a -> true, a -> false);
    }

    /**
     * Renders a dynamic rule form fragment for building conditional logic statements.
     * <p>
     * Endpoint: GET /organizations/{organizationId}/rule-line/{type}
     * </p>
     * <p>
     * Generates a Thymeleaf fragment for a rule editor component used in dynamic form construction.
     * The fragment supports various rule types and configurations including field selection, operators,
     * and value inputs. Returns a parameterized forms::rule-part fragment.
     * </p>
     * <p>
     * Security: Requires CHECK_CAN_MANAGE_ORG_DATA privilege.
     * </p>
     *
     * @param organizationId the ID of the organization (used for routing and context)
     * @param type the type of rule statement to generate
     * @param index the index position of this rule line in the parent form
     * @param datalistId the ID of the datalist for autocomplete options
     * @param fieldName the name of the field this rule applies to
     * @param key the unique key identifier for this rule
     * @param disabled flag indicating if the rule should be disabled ("true"/"false")
     * @param advanced flag indicating if advanced options should be shown ("true"/"false")
     * @param indexForKey the index for key field binding
     * @param indexToDisplay the index for display value binding
     * @param indexForImgUrl the index for image URL binding
     * @return ModelAndView with forms::rule-part fragment containing parameterized rule form component
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ORG_DATA)
    @GetMapping(_ORGANIZATIONID + _RULE_LINE + "/{type}")
    public Object getStatementLineForm(@PathVariable(ORGANIZATIONID) Long organizationId, @PathVariable("type") String type,
                                       @RequestParam("index") Long index, @RequestParam("datalistId") String datalistId,
                                       @RequestParam("fieldName") String fieldName, @RequestParam("key") String key,
                                       @RequestParam("disabled") String disabled, @RequestParam("advanced") String advanced,
                                       @RequestParam("indexForKey") String indexForKey, @RequestParam("indexToDisplay") String indexToDisplay,
                                       @RequestParam("indexForImgUrl") String indexForImgUrl) {
        debug("[getStatementLineForm] orgId {}", organizationId);
        return new ModelAndView("forms::rule-part(index=" + index + ", type=" + type + ", rule=null, datalistId=" + datalistId
                + ", disabled=" + disabled + ", key=" + key + ", name=" + fieldName + ", advanced=" + advanced + ", indexForKey=" + indexForKey
                + ", indexToDisplay=" + indexToDisplay + ", indexForImgUrl=" + indexForImgUrl + ")");
    }


    /**
     * Renders a selected item box fragment for searchable rule components.
     * <p>
     * Endpoint: GET /organizations/{organizationId}/rule/search/selected
     * </p>
     * <p>
     * Generates a Thymeleaf fragment displaying a selected item in a searchable dropdown component
     * used within rule editors. The fragment shows the selected item with its label, image, and
     * provides a link for further actions. Returns a parameterized forms::selected-searchable fragment.
     * </p>
     * <p>
     * Security: Requires CHECK_CAN_MANAGE_ORG_DATA privilege.
     * </p>
     *
     * @param organizationId the ID of the organization (used for routing and context)
     * @param fieldName the name of the field this selected item belongs to
     * @param selectedId the ID of the selected item
     * @param imgUrl the URL of the image to display for the selected item
     * @param label the display label text for the selected item
     * @param url the action URL associated with the selected item
     * @return ModelAndView with forms::selected-searchable fragment containing parameterized selected item display
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ORG_DATA)
    @GetMapping(_ORGANIZATIONID + _RULE + _SEARCH + _SELECTED)
    public Object getStatementSearchSelectedBox(@PathVariable(ORGANIZATIONID) Long organizationId,
                                                @RequestParam("fieldName") String fieldName, @RequestParam("selectedId") String selectedId,
                                                @RequestParam("imgUrl") String imgUrl, @RequestParam("label") String label,
                                                @RequestParam("url") String url) {
        debug("[getStatementSearchSelectedBox] orgId {}", organizationId);
        return new ModelAndView("forms::selected-searchable(selectedId=" + selectedId + ", fieldName=" + fieldName
                + ", imgUrl='" + imgUrl + "', label='" + label + "', url='" + url + "')");
    }

}
