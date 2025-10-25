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

import com.openkoda.form.EditUserForm;
import com.openkoda.repository.specifications.UserSpecifications;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import static com.openkoda.controller.common.URLConstants._HTML_USER;
import static com.openkoda.core.security.HasSecurityRules.*;


/**
 * Concrete HTML user management controller providing server-side rendered user interface.
 * <p>
 * Provides HTML-based user CRUD interface with Thymeleaf templates including user list with search and filter
 * capabilities, profile viewing with additional settings, profile editing with validation, user impersonation
 * (spoofing) for administrators, and API key management. All routes are under /html/user base path.
 * </p>
 * <p>
 * Uses Spring Security {@code @PreAuthorize} annotations with HasSecurityRules constants for fine-grained
 * access control. Extends {@link AbstractUserController} to delegate business logic via Flow pipelines while
 * handling HTTP bindings, request parameters, and view rendering.
 * </p>
 * <p>
 * General contract: resolve HTTP bindings, delegate to AbstractUserController flows, and return ModelAndView
 * with Thymeleaf template.
 * </p>
 * <p>
 * Thymeleaf template conventions:
 * <ul>
 * <li>Template paths use dash-separated format: 'user-all', 'user-profile', 'user-settings'</li>
 * <li>Fragment syntax: 'template::fragment-name' for AJAX responses</li>
 * <li>Success/error fragments: Dual view pattern with '::fragment-success' and '::fragment-error'</li>
 * <li>Navigation fragments: 'generic-forms::go-to(url=...)' for client-side redirects</li>
 * </ul>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractUserController
 * @see EditUserForm
 * @see UserSpecifications
 */
@Controller
@RequestMapping(_HTML_USER)
public class UserControllerHtml extends AbstractUserController {

    /**
     * Default redirect page after successful authentication.
     * <p>
     * Configured via {@code page.after.auth} property with default value of
     * {@code /html/organization/all}. Used as fallback redirect target when
     * exiting user impersonation (spoof mode) if no organization-specific
     * dashboard is available.
     * </p>
     */
    @Value("${page.after.auth:/html/organization/all}")
    private String pageAfterAuth;

    /**
     * Lists all users with pagination and search filtering.
     * <p>
     * Delegates to {@code findUsers} with {@link UserSpecifications#searchSpecification(String)} for filtering.
     * Returns Thymeleaf view with user list table, pagination controls, and search form.
     * </p>
     * <p>
     * URL: {@code GET /html/user/all?user_search={term}&page={page}&size={size}}
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * GET /html/user/all?user_search=john&page=0&size=20
     * </pre>
     * </p>
     *
     * @param userPageable Pagination parameters qualified with 'user' (page number, size, sort order)
     * @param search Search term for filtering users by name or email (default: empty string)
     * @param request HTTP request for context
     * @return ModelAndView with 'user-all' template containing paginated user list
     * @see UserSpecifications#searchSpecification(String)
     */
    @PreAuthorize(CHECK_CAN_READ_USER_DATA)
    @GetMapping(_ALL)
    public Object getAll(
            @Qualifier("user") Pageable userPageable,
            @RequestParam(required = false, defaultValue = "", name = "user_search") String search,
            HttpServletRequest request) {
        debug("[getAll] search {}", search);
        return findUsers(search, UserSpecifications.searchSpecification(null), userPageable)
            .mav(USER + "-" + ALL);
    }

    /**
     * Displays user profile with modules and additional settings.
     * <p>
     * Delegates to {@code getUsersProfile} which loads user entity, creates {@link EditUserForm},
     * and prepares additionalSettingsForms from customisationService. Returns profile page with
     * user info, modules list, and pluggable settings sections.
     * </p>
     * <p>
     * URL: {@code GET /html/user/{userId}/profile?module_search={term}}
     * </p>
     *
     * @param userId ID of user whose profile to display
     * @param modulePageable Pagination for user's modules (qualified with 'module')
     * @param search Search term for filtering modules (default: empty string)
     * @return ModelAndView with 'user-profile' template showing user details and modules
     */
    @PreAuthorize(CHECK_CAN_READ_USER_SETTINGS)
    @GetMapping(_ID + _PROFILE)
    public Object profile(@PathVariable(ID) Long userId,
                          @Qualifier("module") Pageable modulePageable,
                          @RequestParam(required = false, defaultValue = "", name = "module_search") String search) {
        debug("[profile] userId {} search {}", userId, search);
        return getUsersProfile(userId)
                .mav(USER + '-' + PROFILE);
    }

    /**
     * Displays user settings edit form.
     * <p>
     * Delegates to {@code getUsersProfile} to load user and settings forms. Returns Thymeleaf
     * fragment for settings editing with form fields.
     * </p>
     * <p>
     * URL: {@code GET /html/user/{userId}/settings}
     * </p>
     *
     * @param userId ID of user whose settings to edit
     * @return ModelAndView with 'user-settings' template containing {@link EditUserForm}
     */
    @PreAuthorize(CHECK_CAN_READ_USER_SETTINGS)
    @GetMapping(_ID_SETTINGS)
    public Object settings(@PathVariable(ID) Long userId) {
        debug("[settings] userId {}", userId);
        return getUsersProfile(userId)
                .mav("user-settings");
    }

    /**
     * Processes user settings update with validation.
     * <p>
     * Delegates to {@code saveUser} for transactional update. Returns
     * 'entity-forms::user-settings-form-success' fragment on success or
     * 'entity-forms::user-settings-form-error' on validation failure.
     * Success triggers USER_MODIFIED event.
     * </p>
     * <p>
     * URL: {@code POST /html/user/{userId}/settings}
     * </p>
     * <p>
     * Flow: Validates form → Updates user entity → Changes global role if specified →
     * Emits event → Returns fragment
     * </p>
     *
     * @param userId ID of user to update
     * @param userFormData Form containing updated user details (validated with {@code @Valid})
     * @param br BindingResult containing validation errors if any
     * @return ModelAndView with success or error fragment
     */
    @PreAuthorize(CHECK_CAN_MANAGE_USER_SETTINGS)
    @PostMapping(_ID_SETTINGS)
    public Object update(@PathVariable(ID) Long userId, @Valid EditUserForm userFormData, BindingResult br) {
        debug("[update] userId {}", userId);
        return saveUser(userId, userFormData, br)
            .mav(ENTITY + '-' + FORMS + "::user-settings-form-success",
                    ENTITY + '-' + FORMS + "::user-settings-form-error");
    }

    /**
     * Initiates user impersonation allowing admin to access system as another user.
     * <p>
     * Delegates to {@code spoofUser} which stores current admin ID in session, switches
     * security context via {@code services.runAs.startRunAsUser}, and returns
     * 'generic-forms::go-to' fragment with redirect URL. Redirects to user's organization
     * dashboard if available, otherwise to {@code pageAfterAuth}.
     * </p>
     * <p>
     * URL: {@code GET /html/user/{userId}/spoof}
     * </p>
     * <p>
     * Use case: Allows administrators to troubleshoot issues by viewing system as specific
     * user would see it.
     * </p>
     *
     * @param userId ID of user to impersonate
     * @param session HTTP session for storing original admin user ID
     * @param request HTTP request for authentication context switch
     * @param response HTTP response for authentication cookies
     * @return ModelAndView with navigation fragment redirecting to organization dashboard or default page
     */
    @PreAuthorize(CHECK_CAN_IMPERSONATE)
    @GetMapping(_ID + _SPOOF)
    public Object spoof(@PathVariable(ID) Long userId, HttpSession session, HttpServletRequest request,
                        HttpServletResponse response) {
        debug("[spoof] userId {}", userId);
        return spoofUser(userId, session, request, response)
                .mav(a -> "generic-forms::go-to(url='"
                        + (a.get(organizationEntityId) != null ? services.url.organizationDashboard(a.get(organizationEntityId)) : pageAfterAuth)
                        + "')");
    }

    /**
     * Exits user impersonation and restores original admin session.
     * <p>
     * Delegates to {@code stopSpoofingUser} to restore admin security context via
     * {@code services.runAs.exitRunAsUser}. Returns 'generic-forms::go-to' fragment
     * redirecting to admin dashboard.
     * </p>
     * <p>
     * URL: {@code GET /html/user/spoof/exit}
     * </p>
     *
     * @param session HTTP session containing SPOOFING_USER attribute with admin ID
     * @param request HTTP request for authentication context restoration
     * @param response HTTP response for authentication cookies
     * @return ModelAndView with navigation fragment redirecting to admin dashboard
     */
    @PreAuthorize(CHECK_IS_SPOOFED)
    @GetMapping(_SPOOF + _EXIT)
    public Object exitSpoof(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        debug("[exitSpoof]");
        return stopSpoofingUser(session, request, response)
                .mav("generic-forms::go-to(url='" + services.url.adminDashboard() + "')");
    }

    /**
     * Resets user's API key generating new credentials.
     * <p>
     * Delegates to {@code doResetApiKey} which generates new API key via
     * {@code services.apiKey.resetApiKey}, persists updated entities, and returns
     * Thymeleaf fragment with plainApiKeyString for one-time display.
     * </p>
     * <p>
     * Security: User can only reset their own API key. New API key is displayed only
     * once; user must save it immediately.
     * </p>
     * <p>
     * URL: {@code POST /html/user/{userId}/settings/apikey}
     * </p>
     *
     * @param userId ID of user whose API key to reset (must match current user)
     * @return ModelAndView with 'snippets::apiKey' fragment displaying new plain API key
     */
    @PreAuthorize(CHECK_IS_THIS_USERID)
    @PostMapping(_ID_SETTINGS + _APIKEY)
    public Object resetApiKey(@PathVariable(ID) Long userId) {
        debug("[resetApiKey]");
        return doResetApiKey()
                .mav("snippets::apiKey");
    }
}
