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

package com.openkoda.controller.user;

import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.customisation.BasicCustomisationService;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageAttr;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.flow.ResultAndModel;
import com.openkoda.core.helper.UserHelper;
import com.openkoda.core.repository.common.ProfileSettingsRepository;
import com.openkoda.core.security.UserProvider;
import com.openkoda.core.service.event.ApplicationEvent;
import com.openkoda.form.EditUserForm;
import com.openkoda.model.Role;
import com.openkoda.model.User;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuple5;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.function.Function;

import static com.openkoda.controller.common.SessionData.SPOOFING_USER;


/**
 * Abstract base controller providing user account management operations.
 * <p>
 * Implements user lifecycle operations including user search with pagination, profile viewing with additional 
 * settings forms, user impersonation (spoofing), profile updates, user deletion, and API key reset. Enforces 
 * user-scoped and organization-scoped privileges through Flow pipeline integration. Subclasses provide concrete 
 * HTTP endpoint mappings (HTML, API). Uses services.user for authentication integration, services.email for 
 * verification, services.runAs for impersonation, and services.validation for form validation.
 * </p>
 * <p>
 * Controller that provides actual User related functionality for different type of access (eg. API, HTML).
 * Implementing classes should take over http binding and forming a result whereas this controller should take 
 * care of actual implementation.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.service.user.UserService
 * @see com.openkoda.model.User
 * @see com.openkoda.form.EditUserForm
 */
public class AbstractUserController extends AbstractController {

    @Inject
    private BasicCustomisationService customisationService;

    @Inject
    private UserHelper userHelper;

    /**
     * Searches and lists users with pagination and filtering.
     * <p>
     * Executes secure search via repositories.secure.user.search enforcing privilege checks. Search term applies 
     * to user name and email fields.
     * </p>
     *
     * @param aSearchTerm Search term for filtering users by name or email (can be null for no text search)
     * @param aSpecification JPA Specification for additional filtering criteria (organization scope, status filters)
     * @param aPageable Pagination parameters including page number, size, and sort order
     * @return PageModelMap containing 'userPage' with paginated user results
     */
    protected PageModelMap findUsers(String aSearchTerm, Specification<User> aSpecification, Pageable
            aPageable) {
        debug("[findUsers] search {}", aSearchTerm);
        return Flow.init()
                .thenSet( userPage, a -> repositories.secure.user.search(aSearchTerm, null, aSpecification, aPageable))
                .execute();
    }

    /**
     * Retrieves user profile with additional customizable settings forms.
     * <p>
     * Composes findUser flow with prepareAdditionalForms to populate customizable profile sections registered 
     * via BasicCustomisationService.additionalSettingsForms.
     * </p>
     *
     * @param userId ID of user whose profile to retrieve
     * @return PageModelMap containing user entity, editUserForm, and additionalSettingsForms list
     * @throws org.springframework.security.access.AccessDeniedException if current user lacks privilege to read user profile
     */
    protected PageModelMap getUsersProfile(Long userId){
        return findUser(userId)
                .thenSet(additionalSettingsForms, a -> prepareAdditionalForms(a, userId))
                .execute();
    }

    /**
     * Loads user entity and creates EditUserForm.
     * <p>
     * Uses repositories.secure.user.findOne with privilege enforcement, validates entity existence via 
     * services.validation.assertNotNull.
     * </p>
     *
     * @param id User ID to load
     * @return Flow pipeline containing userEntity and editUserForm
     * @throws com.openkoda.core.exception.EntityNotFoundException if user with given ID doesn't exist (HTTP 401)
     */
    protected Flow<Long, EditUserForm, AbstractUserController> findUser(Long id) {
        debug("[findUser] userId {}", id);
        return Flow.init(this, id)
                .thenSet(userEntity, a -> repositories.secure.user.findOne(id))
                .then( a -> services.validation.assertNotNull(a.result, HttpStatus.UNAUTHORIZED))
                .thenSet(editUserForm, a -> new EditUserForm(a.result));

    }

    /**
     * Prepares additional customizable profile settings forms.
     * <p>
     * Iterates customisationService.additionalSettingsForms, loads entities via ProfileSettingsRepository.findOneForUserId, 
     * applies form mapper, and adds to model. Enables pluggable profile sections for extensions.
     * </p>
     *
     * @param a ResultAndModel from Flow pipeline
     * @param userId User ID for loading profile-specific settings
     * @return List of tuples describing repository, mapper, PageAttr key for each additional form
     */
    private List<Tuple5<ProfileSettingsRepository, Function, PageAttr, String, String>> prepareAdditionalForms(ResultAndModel a, Long userId) {

        for (Tuple4<ProfileSettingsRepository, Function, PageAttr, String> t : customisationService.additionalSettingsForms) {
            Object entity = t.getT1().findOneForUserId(userId);
            Object form = t.getT2().apply(entity);
            a.model.put(t.getT3(), form);
        }
        return customisationService.additionalSettingsForms;

    }
    
    /**
     * Enables user impersonation (admin spoofing another user account).
     * <p>
     * Captures current user ID as spoofingUserId, loads target user via repositories.unsecure.user, calls 
     * services.runAs.startRunAsUser to switch security context, stores original ID in session under SPOOFING_USER 
     * key, sets organizationEntityId to first organization of target user for dashboard redirect.
     * </p>
     *
     * @param userId ID of user to impersonate
     * @param session HTTP session for storing original user ID
     * @param request HTTP request for authentication context update
     * @param response HTTP response for setting authentication cookies
     * @return PageModelMap with impersonated user and organizationEntityId for navigation
     */
    protected PageModelMap spoofUser(Long userId, HttpSession session, HttpServletRequest request, HttpServletResponse response){
        // spoofingUserId is an ID of a User who is a global admin or someone that can impersonate other users
        // on spoofing exit this spoofingUserId will be used to log back into one's account
        long spoofingUserId = userHelper.getUserId();
        return Flow.init()
                .thenSet(userEntity, a -> repositories.unsecure.user.findOne(userId))
                .then(a -> services.runAs.startRunAsUser(a.result, request, response))
                .then(a -> {
                    if(a.result) {
                        session.setAttribute(SPOOFING_USER, spoofingUserId);
                    }
                    return a.result;
                })
                .thenSet(organizationEntityId, a -> a.model.get(userEntity).getOrganizationIds() != null && a.model.get(userEntity).getOrganizationIds().length > 0 ?
                        a.model.get(userEntity).getOrganizationIds()[0] : null)
                .execute();
    }
    
    /**
     * Exits user impersonation and restores original admin session.
     * <p>
     * Retrieves spoofingUserId from session, calls services.runAs.exitRunAsUser to restore original security context.
     * </p>
     *
     * @param session HTTP session containing SPOOFING_USER attribute with original user ID
     * @param request HTTP request for authentication context restoration
     * @param response HTTP response for authentication cookies
     * @return PageModelMap after session restoration
     */
    protected PageModelMap stopSpoofingUser(HttpSession session, HttpServletRequest request, HttpServletResponse response){
        return Flow.init()
                .then(a -> services.runAs.exitRunAsUser((Long) session.getAttribute(SPOOFING_USER), request, response))
                .execute();
    }
    
    /**
     * Updates user profile with form data and role changes.
     * <p>
     * Transactional Flow: loads roleEntity by name, finds userEntity via repositories.secure.user, validates with 
     * services.validation.validateAndPopulateToEntity, persists via repositories.unsecure.user.save, optionally 
     * changes global role via services.user.changeUserGlobalRole, emits ApplicationEvent.USER_MODIFIED, rebuilds 
     * EditUserForm with updated data.
     * </p>
     *
     * @param id User ID to update
     * @param userFormData Form containing updated user details (name, email, roles)
     * @param br BindingResult for validation errors
     * @return PageModelMap with updated userEntity and editUserForm
     * @throws org.springframework.security.access.AccessDeniedException if current user lacks privilege to modify user
     * @throws jakarta.validation.ValidationException if form validation fails
     */
    protected PageModelMap saveUser(Long id, EditUserForm userFormData, BindingResult br) {
        debug("[saveUser] userId {}", id);
        return Flow.init(transactional)
                .thenSet( editUserForm, a -> userFormData)
                .thenSet( roleEntity, a -> repositories.unsecure.role.findByName(userFormData.dto.getGlobalRoleName()))
                .thenSet( userEntity, a -> repositories.secure.user.findOne(id))
                .then( a -> services.validation.assertNotNull(a.result, HttpStatus.UNAUTHORIZED))
                .then( a -> services.validation.validateAndPopulateToEntity(userFormData, br,a.result))
                .then( a -> repositories.unsecure.user.save(a.result))
                .then( a -> userFormData.dto.globalRoleName != null ? services.user.changeUserGlobalRole(a.result, userFormData.dto.getGlobalRoleName()) : null)
                .then( a -> services.applicationEvent.emitEvent(ApplicationEvent.USER_MODIFIED, a.model.get(userEntity).getBasicUser()))
                .thenSet(editUserForm, a -> {
                    EditUserForm form = new EditUserForm(a.model.get(userEntity));
                    Role role = a.model.get(roleEntity);
                    if(role != null) {
                        form.dto.setGlobalRoleName(role.getName());
                    }
                    return form;
                })
                .execute();
    }

    /**
     * Soft-deletes user account.
     * <p>
     * Delegates to repositories.unsecure.user.deleteOne which marks user as deleted, anonymizes email, removes 
     * organization memberships while preserving audit trail and historical data.
     * </p>
     *
     * @param id User ID to delete
     * @return PageModelMap after deletion
     */
    protected PageModelMap deleteUser(Long id) {
        debug("[deleteUser] userId {}", id);
        return Flow.init(this, id)
                .then(a -> repositories.unsecure.user.deleteOne(a.result)).execute();
    }

    /**
     * Resets current user's API key generating new credentials.
     * <p>
     * Loads current user via UserProvider.getUserIdOrNotExistingId, calls services.apiKey.resetApiKey to generate 
     * new key, persists updated user and apiKey entities via unsecure repositories, returns plain API key string 
     * for one-time display to user.
     * </p>
     *
     * @return PageModelMap containing userEntity, apiKeyEntity, and plainApiKeyString
     */
    protected PageModelMap doResetApiKey(){
        return Flow.init(userEntity, repositories.secure.user.findOne(UserProvider.getUserIdOrNotExistingId()))
                .thenSet(userEntity, apiKeyEntity, plainApiKeyString, a -> services.apiKey.resetApiKey(a.result))
                .then(a -> Tuples.of(
                        repositories.unsecure.user.save(a.result.getT1()),
                        repositories.unsecure.apiKey.save(a.result.getT2())))
                .execute();
    }

}
