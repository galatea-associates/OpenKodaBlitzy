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

package com.openkoda.core.security;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.flow.Tuple;
import com.openkoda.model.User;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service enabling user impersonation for administrators with canImpersonate privilege.
 * <p>
 * This service allows administrators to authenticate as other users for support and debugging purposes.
 * Core operations include {@code startRunAsUser()} which switches the SecurityContext to a target user
 * with the isSpoofed flag set, and {@code exitRunAsUser()} which restores the original user by loading
 * the backToUserId from the database.
 * <p>
 * Security model: Methods are guarded by {@code @PreAuthorize(CHECK_CAN_IMPERSONATE_OR_IS_SPOOFED)},
 * ensuring only administrators with {@code Privilege.canImpersonate} or already-spoofed users can call
 * these methods. The isSpoofed flag is set on the OrganizationUser principal to distinguish impersonated
 * sessions from normal authentication, enabling UI indicators and audit logging.
 * <p>
 * Thread-safety: This service modifies the SecurityContextHolder (ThreadLocal), so changes affect only
 * the current HTTP request thread. The impersonated authentication is persisted to the HttpSession via
 * SecurityContextRepository, allowing it to survive across multiple requests.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Admin controller begins impersonation
 * runAsService.startRunAsUser(targetUser, request, response);
 * // UI shows "impersonating user@example.com" banner
 * 
 * // Later, admin exits impersonation
 * runAsService.exitRunAsUser(originalUserId, request, response);
 * }</pre>
 *
 * @see OrganizationUser#isSpoofed
 * @see HasSecurityRules#CHECK_CAN_IMPERSONATE_OR_IS_SPOOFED
 * @see OrganizationUserDetailsService
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @since 2019-03-08
 */
@Service
public class RunAsService extends ComponentProvider implements HasSecurityRules {

    /**
     * SecurityContextRepository for persisting SecurityContext to HttpSession.
     * <p>
     * Enables impersonated authentication to survive across requests. The @Lazy annotation
     * is used to avoid circular dependencies during bean initialization.
     * 
     */
    @Autowired
    @Lazy
    SecurityContextRepository securityContextRepository;
    
    /**
     * Service to load User entity and build OrganizationUser principal.
     * <p>
     * Used to construct the OrganizationUser principal with roles and privileges for the
     * impersonated user during the run-as operation.
     * 
     */
    @Inject private OrganizationUserDetailsService organizationUserDetailsService;

    /**
     * Begins a user impersonation session, switching the SecurityContext to the target user.
     * <p>
     * This method replaces the current SecurityContext authentication with a new
     * UsernamePasswordAuthenticationToken containing the target user's OrganizationUser principal,
     * with the isSpoofed flag set to true. The SecurityContext is persisted to the HttpSession via
     * securityContextRepository.saveContext(), making the impersonation survive across HTTP requests.
     * 
     * <p>
     * Access control: This method requires {@code @PreAuthorize(CHECK_CAN_IMPERSONATE_OR_IS_SPOOFED)},
     * allowing only users with Privilege.canImpersonate or users who are already in a spoofed session.
     * 
     *
     * @param user the target User entity to impersonate (loaded from database by controller)
     * @param request the HttpServletRequest for saving SecurityContext to session
     * @param response the HttpServletResponse for session cookie management
     * @return true if impersonation started successfully, false if user is null
     */
    @PreAuthorize(CHECK_CAN_IMPERSONATE_OR_IS_SPOOFED)
    public boolean startRunAsUser(User user, HttpServletRequest request, HttpServletResponse response) {
        return authRunAsUser(user, true, request, response);
    }

    /**
     * Exits the impersonation session, restoring the SecurityContext to the original user.
     * <p>
     * This method queries the User entity via repositories.unsecure.user.findOne(backToUserId) and
     * delegates to authRunAsUser(user, false, request, response), setting the isSpoofed flag to false.
     * The controller is responsible for tracking the original userId before calling startRunAsUser(),
     * typically stored in a session attribute.
     * 
     * <p>
     * Access control: This method requires {@code @PreAuthorize(CHECK_CAN_IMPERSONATE_OR_IS_SPOOFED)},
     * allowing only users who are already in a spoofed session or have the canImpersonate privilege.
     * 
     *
     * @param backToUserId the database ID of the original user to restore (stored before impersonation)
     * @param request the HttpServletRequest for saving restored SecurityContext to session
     * @param response the HttpServletResponse for session cookie management
     * @return true if exit successful, false if backToUserId not found in database
     */
    @PreAuthorize(CHECK_CAN_IMPERSONATE_OR_IS_SPOOFED)
    public boolean exitRunAsUser(long backToUserId, HttpServletRequest request, HttpServletResponse response) {
        User user = repositories.unsecure.user.findOne(backToUserId);
        return authRunAsUser(user, false, request, response);
    }

    /**
     * Internal method implementing impersonation logic.
     * <p>
     * This method performs the core authentication switch by loading user privileges, building an
     * OrganizationUser principal with the isSpoofed flag, and replacing the current SecurityContext.
     * The authentication flow follows these steps:
     * 
     * <ol>
     * <li>Queries UserRole associations via repositories.unsecure.user.getUserRolesAndPrivileges(user.getId())</li>
     * <li>Builds OrganizationUser via organizationUserDetailsService.setUserDetails(user, info)</li>
     * <li>Sets userDetails.setSpoofed(isSpoofed) to mark the principal as impersonated or not</li>
     * <li>Creates UsernamePasswordAuthenticationToken(userDetails, null, authorities) - an authenticated token with null credentials</li>
     * <li>Replaces SecurityContextHolder.getContext().setAuthentication(auth) to update the current thread's SecurityContext</li>
     * <li>Persists via securityContextRepository.saveContext() making impersonation survive across requests in the HttpSession</li>
     * </ol>
     * <p>
     * Note: The commented-out SecurityContextHolder.clearContext() line is intentionally disabled
     * to preserve the existing context for chaining operations. The unsecure repository is used to
     * bypass SecureRepository privilege checks, as impersonation must work regardless of the target
     * user's organization membership.
     * 
     *
     * @param user the target User entity to authenticate as (can be impersonation target or original user for exit flow)
     * @param isSpoofed flag indicating impersonation status: true for startRunAsUser() (entering impersonation),
     *                  false for exitRunAsUser() (returning to original user)
     * @param request the HttpServletRequest for SecurityContext persistence
     * @param response the HttpServletResponse for SecurityContext persistence
     * @return true if authentication switched successfully, false if user is null
     */
    public boolean authRunAsUser(User user, boolean isSpoofed, HttpServletRequest request, HttpServletResponse response) {
        debug("[authRunAsUser] user: {} isSpoofed: {}", user, isSpoofed);
        if(user != null) {
//            SecurityContextHolder.clearContext();
            List<Tuple> info = repositories.unsecure.user.getUserRolesAndPrivileges(user.getId());
            OrganizationUser userDetails = (OrganizationUser) organizationUserDetailsService.setUserDetails(user, info);
            userDetails.setSpoofed(isSpoofed);

            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContextRepository.saveContext(securityContext, request, response);

            return true;
        }
        debug("[authRunAsUser] user is null");
        return false;
    }
}
