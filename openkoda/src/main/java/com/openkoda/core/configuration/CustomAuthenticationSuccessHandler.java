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

package com.openkoda.core.configuration;

import com.openkoda.core.security.OrganizationUser;
import com.openkoda.core.security.UserProvider;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.Privilege;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Security authentication success handler that manages post-login redirection based on user privileges and organization membership.
 * <p>
 * Extends {@link SavedRequestAwareAuthenticationSuccessHandler} to customize redirect logic after successful authentication.
 * Implements intelligent routing: honors saved requests (intercepted unauthenticated URLs), redirects global admins to admin dashboard,
 * single-organization users to their organization home, and multi-organization users to organization selector page.
 * Also persists SecurityContext to configured repository for session management.
 * </p>
 * <p>
 * Redirect decision flow:
 * <ol>
 *     <li>If saved request exists → redirect to original URL</li>
 *     <li>If user has canAccessGlobalSettings privilege → admin dashboard</li>
 *     <li>If user in single organization → organization home</li>
 *     <li>Else → organization selector</li>
 * </ol>
 * </p>
 * <p>
 * Uses {@link UserProvider#getFromContext()} to access authenticated {@link OrganizationUser}.
 * <b>WARNING:</b> Code uses {@link Optional#get()} without presence check which may throw NoSuchElementException
 * if security context is not properly initialized.
 * </p>
 * <p>
 * Registered in WebSecurityConfig via formLogin().successHandler(customAuthenticationSuccessHandler).
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SavedRequestAwareAuthenticationSuccessHandler
 * @see SecurityContextRepository
 * @see OrganizationUser
 * @see UserProvider
 */
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler implements LoggingComponentWithRequestId {

    /**
     * default target url when user is in multiple organizations
     */
    private String pageAfterAuthForMultipleOrganizations;

    /**
     * default target url when user is in single organizations
     */
    private String pageAfterAuthForOneOrganization;

    /**
     * default target url when user is global admin
     */
    private String pageAfterAuthForGlobalAdmin;

    /**
     * context repository for saving the logged users context
     */
    private SecurityContextRepository securityContextRepository;

    /**
     * Request Cache, where unauthenticated request are stored and waiting for user login
     */
    private RequestCache requestCache = new HttpSessionRequestCache();

    /**
     * Constructs authentication success handler with configured redirect URLs and security context repository.
     *
     * @param pageAfterAuthForMultipleOrganizations redirect URL for users belonging to multiple organizations (e.g., '/html/organization')
     * @param pageAfterAuthForOneOrganization redirect URL pattern for users in single organization (e.g., '/html/organization/%s/dashboard' with %s for organization ID)
     * @param pageAfterAuthForGlobalAdmin redirect URL for users with global admin privileges (e.g., '/html/admin/home')
     * @param securityContextRepository Spring Security repository for persisting authentication context between requests
     */
    public CustomAuthenticationSuccessHandler(String pageAfterAuthForMultipleOrganizations, String pageAfterAuthForOneOrganization, String pageAfterAuthForGlobalAdmin, SecurityContextRepository securityContextRepository) {
        this.pageAfterAuthForMultipleOrganizations = pageAfterAuthForMultipleOrganizations;
        this.pageAfterAuthForOneOrganization = pageAfterAuthForOneOrganization;
        this.pageAfterAuthForGlobalAdmin = pageAfterAuthForGlobalAdmin;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Handles successful authentication by persisting security context and determining appropriate redirect target.
     * <p>
     * Invoked by Spring Security after successful authentication. First persists SecurityContext to repository.
     * Then checks for saved request (original URL that triggered authentication). If saved request exists,
     * delegates to parent class. Otherwise, analyzes user's organization membership and privileges to determine
     * redirect: global admins to admin dashboard, single-org users to organization home, multi-org users to
     * organization selector.
     * </p>
     * <p>
     * Uses {@link RequestCache} to retrieve saved request. Accesses {@link OrganizationUser} via
     * {@link UserProvider#getFromContext()}.get() which assumes security context is populated
     * (may throw NoSuchElementException if not). Uses String.format() for single-organization URL
     * with organization ID substitution.
     * </p>
     *
     * @param httpServletRequest the HTTP request that triggered authentication
     * @param httpServletResponse the HTTP response used for redirect
     * @param authentication the Authentication object containing authenticated principal (OrganizationUser)
     * @throws IOException if redirect fails due to I/O error
     * @throws ServletException if servlet error occurs during authentication handling
     * @see SavedRequest
     * @see OrganizationUser
     * @see Privilege#canAccessGlobalSettings
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        debug("[onAuthenticationSuccess]");
        SavedRequest savedRequest = this.requestCache.getRequest(httpServletRequest, httpServletResponse);
        this.securityContextRepository.saveContext(SecurityContextHolder.getContext(), httpServletRequest, httpServletResponse);
        if (savedRequest != null) {
            debug("[onAuthenticationSuccess] SavedRequest is {}", savedRequest.getRedirectUrl());
            super.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);
        } else {
            Optional<OrganizationUser> user = UserProvider.getFromContext();
            OrganizationUser authenticatedUser = user.get();
            Set<Long> organizationIds = authenticatedUser.getOrganizationIds();

            Long onlyOrgId = organizationIds.size() == 1 ? organizationIds.iterator().next() : -1L;

            debug("[onAuthenticationSuccess] user orgsIds {}, primary {}", organizationIds == null ? "[]" : organizationIds.toString(), onlyOrgId);

            if(authenticatedUser.hasGlobalPrivilege(Privilege.canAccessGlobalSettings)) {
                debug("[onAuthenticationSuccess] redirecting to admin dashboard {}", pageAfterAuthForGlobalAdmin);
                httpServletResponse.sendRedirect(pageAfterAuthForGlobalAdmin);
            } else if (onlyOrgId != -1L) {
                String redirectUrl = String.format(pageAfterAuthForOneOrganization, onlyOrgId);
                debug("[onAuthenticationSuccess] redirecting to single {}", redirectUrl);
                httpServletResponse.sendRedirect(redirectUrl);
            } else {
                debug("[onAuthenticationSuccess] redirecting to multiple {}", pageAfterAuthForMultipleOrganizations);
                httpServletResponse.sendRedirect(pageAfterAuthForMultipleOrganizations);
            }
        }
    }
}
