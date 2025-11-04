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

import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.core.service.event.ApplicationEvent;
import com.openkoda.dto.OrganizationDto;
import com.openkoda.form.GlobalOrgRoleForm;
import com.openkoda.form.InviteUserForm;
import com.openkoda.form.OrganizationForm;
import com.openkoda.model.Organization;
import com.openkoda.model.Privilege;
import com.openkoda.repository.specifications.UserSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.BindingResult;
import reactor.util.function.Tuples;

import static com.openkoda.model.Privilege.readUserData;

/**
 * Abstract base controller providing multi-tenant organization management operations.
 * <p>
 * This controller provides comprehensive organization lifecycle management including:
 * <ul>
 *   <li>Organization creation with initial admin user provisioning</li>
 *   <li>Organization editing with property bag customization</li>
 *   <li>Organization switching for multi-tenant users</li>
 *   <li>Member management (add/remove users with role assignment)</li>
 *   <li>Organization deletion with cascade cleanup</li>
 * </ul>
 * <p>
 * Enforces organization-scoped privileges via secure repositories. Subclasses provide concrete endpoint mappings
 * for different access types (HTML, API). This abstract controller handles the actual business logic implementation
 * using Flow pipelines for transactional orchestration.
 * 
 * <p>
 * Uses {@link com.openkoda.service.organization.OrganizationService} for tenant provisioning and schema management,
 * {@link com.openkoda.service.user.UserService} for member operations, and secure repositories for
 * privilege-enforced data access.
 * 
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.service.organization.OrganizationService
 * @see Organization
 * @see OrganizationControllerHtml
 */
public class AbstractOrganizationController extends AbstractController implements HasSecurityRules {

    /**
     * Searches and retrieves organizations with pagination and privilege enforcement.
     * <p>
     * Performs a paginated search across organizations using the provided search term and specification.
     * Default sort order is by createdOn descending if no sort is specified. All results are filtered
     * through secure repositories to enforce organization-scoped read privileges.
     * 
     *
     * @param aSearchTerm search string applied to organization name and properties, may be null
     * @param aSpecification JPA Specification for additional filtering criteria, may be null
     * @param aPageable pagination and sorting parameters, sort defaults to createdOn DESC if null
     * @return PageModelMap containing 'organizationPage' with privilege-filtered search results
     */
    protected PageModelMap findOrganizationsFlow(
            String aSearchTerm,
            Specification<Organization> aSpecification,
            Pageable aPageable) {
        debug("[findOrganizationFlow] search {}", aSearchTerm);
        if(aPageable.getSort() == null) {
            aPageable = PageRequest.of(aPageable.getPageNumber(), aPageable.getPageSize(), Sort.Direction.DESC, "createdOn");
        }
        Pageable finalAPageable = aPageable;
        return Flow.init()
                .thenSet(organizationPage, a -> repositories.secure.organization.search(
                        aSearchTerm, null, aSpecification, finalAPageable))
                .execute();
    }

    /**
     * Loads an organization by ID and prepares its form representation.
     * <p>
     * Retrieves the organization entity using unsecure repository (privilege checks handled by caller)
     * and populates an OrganizationForm with the entity data. Returns a Flow for further chaining.
     * 
     *
     * @param id organization identifier
     * @return Flow containing 'organizationEntity' and 'organizationForm' for further processing
     */
    protected Flow<Long, OrganizationForm, AbstractOrganizationController> findOrganizationFlow(Long id) {
        debug("[findOrganizationFlow] id {}", id);
        return Flow.init(this, id)
                .thenSet(organizationEntity, a -> repositories.unsecure.organization.findOne(id))
                .thenSet(organizationForm, a -> new OrganizationForm(id, a.result));
    }

    /**
     * Retrieves organization settings flow by delegating to findOrganizationFlow.
     * <p>
     * Provides a base Flow for loading organization data, intended to be extended
     * with additional settings retrieval in calling methods.
     * 
     *
     * @param id organization identifier
     * @return Flow containing organization entity and form for settings operations
     */
    protected Flow findOrganizationWithSettingsFlow(Long id) {
        debug("[findOrganizationWithSettingsFlow] id {}", id);
        return findOrganizationFlow(id);
    }

    /**
     * Executes the organization settings flow and returns the complete PageModelMap.
     * <p>
     * Retrieves organization entity and form by executing findOrganizationWithSettingsFlow.
     * 
     *
     * @param id organization identifier
     * @return PageModelMap with organization entity and form data
     */
    protected PageModelMap findOrganizationWithSettings(Long id) {
        debug("[findOrganizationWithSettings] id {}");
        return findOrganizationWithSettingsFlow(id)
                .execute();
    }

    /**
     * Retrieves comprehensive organization settings including member list and invitation forms.
     * <p>
     * Loads organization data, searches for users with organization-scoped filtering, prepares
     * invitation forms, and retrieves global organization roles. Results include paginated
     * user list for member management.
     * 
     *
     * @param organizationId organization identifier
     * @param userSearch search term for filtering users, may be null
     * @param userPageable pagination parameters for user list
     * @return PageModelMap containing organizationEntity, organizationForm, userPage, inviteUserForm, globalOrgRoleForm
     */
    protected PageModelMap getOrganizationSettings(Long organizationId, String userSearch, Pageable userPageable){
        return findOrganizationWithSettingsFlow(organizationId)
                .thenSet(organizationEntityId, a -> organizationId)
                .thenSet(userPage, a -> repositories.secure.user.search(userSearch, null, UserSpecifications.searchSpecification(organizationId), userPageable))
                .thenSet(inviteUserForm, a -> new InviteUserForm())
                .thenSet(globalOrgRoleForm, a -> new GlobalOrgRoleForm(services.organization.getNamesOfGlobalOrgRolesInOrganization(organizationId)))
                .execute();
    }

    /**
     * Prepares settings for new organization creation form.
     * <p>
     * Initializes empty organization form with ID -1, loads user list with empty search,
     * and prepares invitation form for adding initial members during organization creation.
     * 
     *
     * @param userPageable pagination parameters for user list
     * @return PageModelMap containing empty organizationForm, userPage, and inviteUserForm
     */
    protected PageModelMap getNewOrganizationSettings(Pageable userPageable) {
        return findOrganizationWithSettingsFlow(-1L)
                .thenSet(userPage, a -> repositories.secure.user.search(
                        "",  null, UserSpecifications.searchSpecification(-1L), userPageable))
                .thenSet(inviteUserForm, a -> new InviteUserForm())
                .execute();
    }
    /**
     * Deletes an organization by ID using repository delete operation.
     * <p>
     * Performs soft or hard delete based on repository configuration. Does not handle
     * schema cleanup - use {@link #removeOrganization(long)} for complete removal.
     * 
     *
     * @param id organization identifier to delete
     * @return PageModelMap with deletion result
     * @see #removeOrganization(long)
     */
    protected PageModelMap deleteOrganization(Long id) {
        debug("[deleteOrganization] id {}", id);
        return Flow.init(this, id)
                .then(a -> repositories.unsecure.organization.deleteOne(a.result))
                .execute();
    }

    /**
     * Invites a user to join an organization with role assignment.
     * <p>
     * Validates that the user does not already have a role in the organization, then handles
     * both existing and new user scenarios. For existing users, associates them with the organization.
     * For new users, creates account and sends invitation. Uses tuple resolution to load
     * user and organization entities concurrently.
     * 
     *
     * @param form InviteUserForm containing user email and role assignment details
     * @param organizationId target organization identifier
     * @param br BindingResult for validation error accumulation
     * @return PageModelMap with invitation result and updated inviteUserForm
     */
    protected PageModelMap inviteUser(InviteUserForm form, Long organizationId, BindingResult br) {
        debug("[inviteUser] orgId {}", organizationId);
        return Flow.init(inviteUserForm, form)
                .thenSet(inviteUserForm, a -> form)
                .then(a -> services.user.validateIfUserDoesNotHaveRoleInOrganization(form.dto.email, organizationId, br))
                .then(a -> services.validation.validate(form, br))
                .then(a -> Tuples.of(
                        repositories.unsecure.user.findByEmailLowercase(form.dto.email),
                        repositories.unsecure.organization.findOne(organizationId)))
                .then(a -> services.user.inviteNewOrExistingUser(form, a.result.getT1(), a.result.getT2()))
                .execute();
    }

    /**
     * Updates global organization role assignments for the specified organization.
     * <p>
     * Retrieves all global roles and current user roles for the organization, then
     * updates organization membership to match the selected global roles from the form.
     * Synchronizes role assignments with the organization's current configuration.
     * 
     *
     * @param form GlobalOrgRoleForm containing selected global organization roles
     * @param organizationId organization identifier for role assignment
     * @return PageModelMap with updated globalOrgRoleForm and synchronization result
     */
    protected PageModelMap globalOrgRole(GlobalOrgRoleForm form, Long organizationId){
        return Flow.init()
                .thenSet(globalOrgRoleForm, a -> form)
                .then(a -> Tuples.of(repositories.unsecure.role.findAllGlobalRoles(),
                        services.userRole.getUserRolesForOrganization(organizationId)))
                .then(a -> services.organization.updateGlobalOrgRolesInOrganization(organizationId, a.result.getT1(),form.dto.getGlobalOrganizationRoles(), a.result.getT2()))
                .execute();
    }

    /**
     * Removes a user role assignment by userRoleId.
     * <p>
     * Deletes the UserRole entity, effectively removing the user's role within the associated
     * organization. Does not delete the user account itself.
     * 
     *
     * @param userRoleId identifier of the UserRole association to remove
     * @return PageModelMap with deletion result
     */
    protected PageModelMap removeUserRole(long userRoleId) {
        debug("[removeUserRole] userRoleId {}", userRoleId);
        return Flow.init()
                .then(a -> repositories.unsecure.userRole.deleteUserRole(userRoleId))
                .execute();
    }

    /**
     * Completely removes an organization including schema cleanup and cascade deletion.
     * <p>
     * Executes a three-phase removal process:
     * <ol>
     *   <li>Marks the organization's schema as deleted in the assigned datasource</li>
     *   <li>Drops schema constraints to prepare for cleanup</li>
     *   <li>Removes the organization entity and all associated data</li>
     * </ol>
     * This operation is irreversible and cascades to all organization-scoped data.
     * 
     *
     * @param organizationId organization identifier to remove
     * @return PageModelMap with removal completion status
     */
    protected PageModelMap removeOrganization(long organizationId) {
        debug("[removeOrganization] organizationId {}", organizationId);
        return Flow.init()
                .thenSet(organizationEntity, a -> repositories.unsecure.organization.findOne(organizationId))
                .then(a -> services.organization.markSchemaAsDeleted(organizationId, a.model.get(organizationEntity).getAssignedDatasource()))
                .then(a -> services.organization.dropSchemaConstraints(organizationId, a.result, a.model.get(organizationEntity).getAssignedDatasource()))
                .then(a -> services.organization.removeOrganization(organizationId))
                .execute();
    }

    /**
     * Updates an existing organization with validated form data.
     * <p>
     * Validates the OrganizationForm, populates changes to the existing organization entity,
     * persists updates, and emits ORGANIZATION_MODIFIED event for downstream listeners.
     * Property bag customizations are preserved and merged.
     * 
     *
     * @param organizationId identifier of the organization to update
     * @param form OrganizationForm containing updated organization data
     * @param br BindingResult for validation error accumulation
     * @return PageModelMap with updated organizationEntity and organizationForm
     */
    protected PageModelMap saveOrganization(
            Long organizationId,
            OrganizationForm form,
            BindingResult br) {
        debug("[saveOrganization] orgId {}", organizationId);
        return Flow.init()
                .thenSet(organizationForm, a -> form)
                .then(a -> repositories.unsecure.organization.findOne(organizationId))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br,a.result))
                .thenSet(organizationEntity, a -> repositories.unsecure.organization.save(a.result))
                .then(a -> services.applicationEvent.emitEvent(ApplicationEvent.ORGANIZATION_MODIFIED, new OrganizationDto(a.result)))
                .execute();
    }

    /**
     * Creates a new organization with initial provisioning and admin user setup.
     * <p>
     * Executes a transactional flow that validates the OrganizationForm and delegates to
     * {@link com.openkoda.service.organization.OrganizationService} for organization entity creation.
     * The service handles tenant schema provisioning, initial admin user creation, and default
     * privilege assignment. Form is reset to default state after successful creation.
     * 
     *
     * @param form OrganizationForm containing organization name and datasource assignment
     * @param br BindingResult for validation error accumulation
     * @return PageModelMap with newly created organizationEntity and reset organizationForm
     * @see com.openkoda.service.organization.OrganizationService#createOrganization(String, String)
     */
    protected PageModelMap createOrganization(
            OrganizationForm form,
            BindingResult br) {
        debug("[createOrganization]");
        return Flow.init(transactional)
                .thenSet(organizationForm, a -> form)
                .then(a -> services.validation.validate(form, br))
                .thenSet(organizationEntity, a -> services.organization.createOrganization(form.dto.name, form.dto.assignedDatasource))
                .thenSetDefault(organizationForm)
                .execute();
    }

    /**
     * Retrieves audit history for an organization with pagination and search filtering.
     * <p>
     * Loads organization entity and queries audit log entries scoped to the organization.
     * Supports search term filtering across audit event details. Results are paginated
     * for performance with large audit trails.
     * 
     *
     * @param organizationId organization identifier for audit history
     * @param auditPageable pagination parameters for audit entries
     * @param search search term for filtering audit events, may be null
     * @return PageModelMap containing organizationEntity and auditPage with filtered history
     */
    protected PageModelMap getHistory(
            Long organizationId,
            Pageable auditPageable,
            String search) {
        debug("[getHistory] orgId {}, search {}", organizationId, search);
        return Flow.init(this, organizationId)
                .thenSet(organizationEntity, a -> repositories.unsecure.organization.findOne(a.result))
                .thenSet(auditPage, a -> repositories.unsecure.audit.findAllByOrganizationId(organizationId, search, auditPageable))
                .execute();
    }

    /**
     * Changes a user's role within a specific organization.
     * <p>
     * Executes a transactional flow that loads the user by ID and updates their role assignment
     * for the specified organization. Role change is performed by the user service which handles
     * privilege reconciliation and user role entity updates.
     * 
     *
     * @param organizationId organization identifier where role change occurs
     * @param userId user identifier whose role is being changed
     * @param roleName new role name to assign to the user
     * @return PageModelMap with role change result
     */
    protected PageModelMap changeUserOrganizationRole(long organizationId, long userId, String roleName){
        debug("[changeUserRole] organizationId {}, userId {}, roleName {}", organizationId, userId, roleName);
        return Flow.init(transactional)
                .then(a -> repositories.unsecure.user.findById(userId))
                .then(a -> services.user.changeUserOrganizationRole(a.result, organizationId, roleName))
                .execute();
    }

}
