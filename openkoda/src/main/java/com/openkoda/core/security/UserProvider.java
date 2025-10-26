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
import com.openkoda.core.cache.RequestSessionCacheService;
import com.openkoda.core.flow.Tuple;
import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.dto.user.BasicUser;
import com.openkoda.model.User;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.openkoda.core.service.event.ApplicationEvent.USER_MODIFIED;
import static com.openkoda.model.PrivilegeNames.*;

/**
 * Singleton service providing centralized access to the current authenticated OrganizationUser
 * with request-scoped caching and automatic privilege reload.
 * <p>
 * This class serves as the primary gateway for retrieving authentication information throughout
 * the OpenKoda application. It retrieves OrganizationUser from SecurityContextHolder, caches
 * the principal per-request via RequestSessionCacheService to avoid repeated SecurityContext
 * lookups, and detects privilege changes via User.updatedOn timestamp comparison.
 * </p>
 * <p>
 * Core responsibilities include:
 * </p>
 * <ul>
 *   <li>Retrieving the current authenticated OrganizationUser from Spring SecurityContextHolder</li>
 *   <li>Caching the principal per HTTP request to prevent repeated database queries</li>
 *   <li>Detecting privilege changes by comparing User.updatedOn timestamps</li>
 *   <li>Rebuilding principal with OrganizationUserDetailsService when roles are modified</li>
 *   <li>Providing synthetic principals for scheduled jobs, OAuth callbacks, and REST consumers</li>
 * </ul>
 * <p>
 * The class uses a static API pattern that provides static getFromContext() method accessible
 * from any code layer without Spring injection. The instance field is initialized via @PostConstruct
 * to enable static method delegation.
 * </p>
 * <p>
 * Synthetic principal creators:
 * </p>
 * <ul>
 *   <li>setCronJobAuthentication() - Creates principal for scheduled jobs with admin privileges</li>
 *   <li>setOAuthAuthentication() - Creates principal for OAuth callbacks with readUserData and manageUserRoles</li>
 *   <li>setConsumerAuthentication() - Creates principal for REST consumers with canReadBackend and manageUserRoles</li>
 * </ul>
 * <p>
 * Usage example in Flow pipelines:
 * </p>
 * <pre>
 * Optional&lt;OrganizationUser&gt; user = UserProvider.getFromContext();
 * </pre>
 *
 * @see OrganizationUser
 * @see OrganizationUserDetailsService
 * @see RequestSessionCacheService
 * @see SecurityContextHolder
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Service
@Scope("singleton")
public class UserProvider extends ComponentProvider {
    private final static Logger logger = LoggerFactory.getLogger(UserProvider.class);

    /**
     * RequestSessionCacheService providing per-request memoization for getFromContext().
     * Prevents repeated SecurityContext lookups and database queries by caching the
     * OrganizationUser principal for the duration of a single HTTP request.
     */
    @Inject
    private RequestSessionCacheService cacheService;
    
    /**
     * Service to rebuild OrganizationUser after privilege changes are detected.
     * Used to refresh the principal with current database roles and privileges when
     * User.updatedOn timestamp indicates modifications.
     */
    @Inject private OrganizationUserDetailsService organizationUserDetailsService;

    /**
     * Singleton UserProvider instance enabling static method delegation.
     * Initialized via @PostConstruct init() callback and used by all static methods
     * to access injected Spring beans.
     */
    @Inject
    private static UserProvider instance;

    /**
     * PostConstruct lifecycle callback that initializes the static instance field
     * and registers USER_MODIFIED event listener.
     * <p>
     * This method sets the static instance field to enable static method delegation
     * and registers markUserAsModified(BasicUser) as an event listener. When a USER_MODIFIED
     * event is fired, the listener updates User.updatedOn timestamp, triggering privilege
     * reload in getFromContext() on the next call.
     * </p>
     */
    @PostConstruct
    private void init() {
        instance = this;
        services.applicationEvent.registerEventListener(USER_MODIFIED, this::markUserAsModified);
    }

    /**
     * Event listener callback that marks a user as modified by updating their updatedOn timestamp.
     * <p>
     * Triggers UserRepository.setUserAsModified(userId) to update the User.updatedOn timestamp
     * to the current time. This causes getFromContext() to reload privileges on the next call,
     * ensuring users see role and privilege changes immediately without requiring re-login.
     * </p>
     *
     * @param u BasicUser DTO containing the userId to mark as modified
     */
    private void markUserAsModified(BasicUser u) {
        repositories.unsecure.user.setUserAsModified(u.getId());
    }

    /**
     * Returns the current authenticated OrganizationUser from SecurityContextHolder with
     * request-scoped caching and automatic privilege reload detection.
     * <p>
     * This method uses RequestSessionCacheService.tryGet() to memoize the principal per HTTP
     * request, avoiding repeated SecurityContext reads and database queries. It delegates to
     * getFromContext(true) which checks the User.updatedOn timestamp and rebuilds the principal
     * if role or privilege changes are detected.
     * </p>
     * <p>
     * Thread-safety: SecurityContextHolder uses ThreadLocal, so each thread has an independent
     * SecurityContext. This method is safe to call from multiple threads.
     * </p>
     * <p>
     * Usage example in controllers:
     * </p>
     * <pre>
     * Optional&lt;OrganizationUser&gt; user = UserProvider.getFromContext();
     * </pre>
     *
     * @return Optional containing the authenticated OrganizationUser if present, empty for anonymous requests
     */
    public static final Optional<OrganizationUser> getFromContext() {
        OrganizationUser user = instance.cacheService.tryGet(OrganizationUser.class, () -> getFromContext(true).orElse(null));
        if(user == null) {
            return Optional.empty();
        }
        
        return Optional.of(user);
    }

    /**
     * Checks if the current request has an authenticated user (not anonymous).
     * <p>
     * Returns true if SecurityContext.getAuthentication().isAuthenticated() returns true,
     * false for null context or anonymous authentication. Used in templates and controllers
     * for conditional rendering based on authentication status.
     * </p>
     *
     * @return true if user is authenticated, false for anonymous or null authentication
     */
    public static boolean isAuthenticated() {
        Optional<SecurityContext> context = Optional.ofNullable(SecurityContextHolder.getContext());
        return context.map( a -> a.getAuthentication() ).map( a -> a.isAuthenticated() ).orElse(false);
    }

    /**
     * Checks if the current request is anonymous (no authentication or AnonymousAuthenticationToken).
     * <p>
     * Returns true for null SecurityContext, null Authentication, or AnonymousAuthenticationToken.
     * Returns false for authenticated users. This is the inverse of isAuthenticated() but handles
     * AnonymousAuthenticationToken explicitly.
     * </p>
     *
     * @return true for anonymous requests, false for authenticated users
     */
    public static boolean isAnonymous() {
        SecurityContext c = SecurityContextHolder.getContext();
        return (c == null || c.getAuthentication() == null || c.getAuthentication() instanceof AnonymousAuthenticationToken);
    }

    /**
     * Internal method implementing privilege reload logic via User.updatedOn timestamp comparison.
     * <p>
     * This method extracts Authentication from SecurityContextHolder.getContext() and returns
     * empty Optional if authentication is null or principal is not an OrganizationUser. If
     * checkWasModified is false, returns the cached principal immediately.
     * </p>
     * <p>
     * Privilege reload algorithm when checkWasModified is true:
     * </p>
     * <ol>
     *   <li>Queries UserRepository.wasModifiedSince(userId, user.updatedOn) to check if User entity timestamp updated</li>
     *   <li>Queries UserRoleRepository.wasModifiedSince(userId, user.updatedOn) to check if UserRole associations changed</li>
     *   <li>If either returns true, reloads User entity from database</li>
     *   <li>Queries getUserRolesAndPrivileges() for current role/privilege mappings</li>
     *   <li>Calls organizationUserDetailsService.setUserDetails() to rebuild OrganizationUser with current database privileges</li>
     *   <li>Creates PreAuthenticatedAuthenticationToken with new OrganizationUser and replaces SecurityContext.authentication</li>
     *   <li>Recursively calls getFromContext() to return fresh principal</li>
     * </ol>
     * <p>
     * This prevents stale privileges: users see role and privilege changes immediately without
     * re-login, enabling live privilege propagation.
     * </p>
     *
     * @param checkWasModified if true, queries database for User/UserRole updates since principal creation and rebuilds OrganizationUser if modified
     * @return Optional containing OrganizationUser from SecurityContext principal, rebuilt if privileges changed
     */
    private static final Optional<OrganizationUser> getFromContext(boolean checkWasModified) {
        logger.trace("[getFromContext]");
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            return Optional.empty();
        }

        Authentication authentication = context.getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();


        if (principal instanceof OrganizationUser) {
            Optional<OrganizationUser> p = Optional.ofNullable((OrganizationUser) principal);

            if (!p.map(a -> a.getUser()).isPresent()) {
                return p;
            }

            if (!(checkWasModified)) {
                return p;
            }

            User u = p.get().getUser();
            Long userId = u.getId();

            logger.trace("[getFromContext] >>> checking for modified");
            Optional<Boolean> wasModified = instance.repositories.unsecure.user.wasModifiedSince(userId, u.getUpdatedOn());
            Optional<Boolean> rolesWereModified = instance.repositories.unsecure.userRole.wasModifiedSince(userId, u.getUpdatedOn());
            logger.trace("[getFromContext] <<< checking for modified");
            boolean wasModifiedValue = wasModified.orElse(false);
            boolean rolesWereModifiedValue = rolesWereModified.orElse(false);
            if (wasModifiedValue || rolesWereModifiedValue) {

                User user = instance.repositories.unsecure.user.findOne(userId);
                LocalDateTime updatedOn = wasModifiedValue ? instance.repositories.unsecure.user.getUpdatedOn(userId) : instance.repositories.unsecure.userRole.getLastUpdatedOn(userId);

                user.setUpdatedOn(updatedOn);

                List<Tuple> info = instance.repositories.unsecure.user.getUserRolesAndPrivileges(user.getId());

                OrganizationUser userDetails = instance.organizationUserDetailsService.setUserDetails(user, info, p.get());

                Authentication a = new PreAuthenticatedAuthenticationToken(
                        userDetails, "N/A", userDetails.getAuthorities());
                context.setAuthentication(a);
                SecurityContextHolder.setContext(context);

                return getFromContext();
            }

            return p;


        }
        return Optional.empty();
    }

    /**
     * Returns the current user's database ID or OrganizationUser.nonExistingUserId sentinel (-2L) for anonymous requests.
     * <p>
     * Used in audit logging and repository queries where userId must not be null. Returns the authenticated
     * user's ID if present, otherwise returns -2L to indicate anonymous or unauthenticated access.
     * </p>
     *
     * @return User ID (Long) if authenticated, -2L for anonymous/unauthenticated requests
     */
    public static final long getUserIdOrNotExistingId() {
        long userId =
                getFromContext(false)
                .map( a -> a.getUserId() )
                .orElse( OrganizationUser.nonExistingUserId );
        return userId;
    }

    /**
     * Returns the user ID as a String for logging and display purposes.
     * <p>
     * Calls getUserIdOrNotExistingId() and converts the result to String. Returns "-2" for
     * anonymous requests.
     * </p>
     *
     * @return String representation of getUserIdOrNotExistingId(), "-2" for anonymous
     */
    public static final String getUserIdOrNotExistingIdAsString() {
        return Long.toString(getUserIdOrNotExistingId());
    }

    /**
     * Clears the SecurityContext authentication, effectively logging out the current user.
     * <p>
     * Sets SecurityContextHolder.getContext().setAuthentication(null) to remove the principal.
     * Typically called after session invalidation during the logout flow to ensure complete
     * cleanup of authentication state.
     * </p>
     */
    public static final void clearAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    /**
     * Creates a synthetic OrganizationUser with admin privileges for scheduled job execution.
     * <p>
     * Creates a principal with username "_job_", null User entity, and all admin privileges via
     * PrivilegeHelper.getAdminPrivilegeStrings(). This enables scheduled jobs to bypass privilege
     * checks, allowing them to perform administrative tasks such as cleanup operations, report
     * generation, and data maintenance.
     * </p>
     * <p>
     * Sets PreAuthenticatedAuthenticationToken in SecurityContextHolder for the job thread duration.
     * No password or credentials are required as this uses a pre-authenticated token.
     * </p>
     */
    public static final void setCronJobAuthentication() {

        Set<String> globalPrivileges = PrivilegeHelper.getAdminPrivilegeStrings();
        Set<String> globalRoles = new HashSet<>();
        Map<Long, Set<String>> organizationPrivileges = new HashMap<>();
        Map<Long, Set<String>> organizationRoles = new HashMap<>();
        Map<Long, String> organizationNames = new LinkedHashMap<>();
        Collection<? extends GrantedAuthority> authorities = new ArrayList<>();

        UserDetails userDetails = new OrganizationUser(
                "_job_", "",
                true, true, true, true,
                authorities, globalPrivileges, globalRoles, organizationPrivileges, organizationRoles, null,
                organizationNames);

        Authentication a = new PreAuthenticatedAuthenticationToken(
                userDetails, "N/A", userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(a);

    }

    /**
     * Creates a synthetic OrganizationUser for OAuth callback processing.
     * <p>
     * Creates a principal with username "_oauth_" and minimal privileges: readUserData and
     * manageUserRoles. This is used by OAuth controllers to create and update User entities
     * during the OAuth login flow, restricting privileges to prevent OAuth callbacks from
     * escalating beyond user management operations.
     * </p>
     * <p>
     * Sets PreAuthenticatedAuthenticationToken in SecurityContextHolder for the OAuth callback
     * thread duration. No password or credentials are required.
     * </p>
     */
    public static final void setOAuthAuthentication() {

        Set<String> globalPrivileges = Stream.of(_readUserData, _manageUserRoles).collect(Collectors.toSet());

        UserDetails userDetails = new OrganizationUser(
                "_oauth_", "",
                true, true, true, true,
                Collections.EMPTY_LIST, globalPrivileges, Collections.EMPTY_SET, Collections.EMPTY_MAP, Collections.EMPTY_MAP, null,
                Collections.EMPTY_MAP);

        Authentication a = new PreAuthenticatedAuthenticationToken(
                userDetails, "N/A", userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(a);

    }

    /**
     * Creates a synthetic OrganizationUser for REST API consumer requests (integration APIs).
     * <p>
     * Creates a principal with username "_consumer_" and privileges: canReadBackend and
     * manageUserRoles. This is used by integration consumers (Trello, GitHub, Jira) to read
     * backend configuration and manage user associations. The privileges allow backend reads
     * but not full admin access, restricting consumer operations to integration-specific tasks.
     * </p>
     * <p>
     * Sets PreAuthenticatedAuthenticationToken in SecurityContextHolder for the consumer
     * request thread duration. No password or credentials are required.
     * </p>
     */
    public static final void setConsumerAuthentication() {

        Set<String> globalPrivileges = Stream.of(_canReadBackend, _manageUserRoles).collect(Collectors.toSet());

        UserDetails userDetails = new OrganizationUser(
                "_consumer_", "",
                true, true, true, true,
                Collections.EMPTY_LIST, globalPrivileges, Collections.EMPTY_SET, Collections.EMPTY_MAP, Collections.EMPTY_MAP, null,
                Collections.EMPTY_MAP);


        Authentication a = new PreAuthenticatedAuthenticationToken(
                userDetails, "N/A", userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(a);

    }

}
