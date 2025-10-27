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

import com.openkoda.controller.common.SessionData;
import com.openkoda.controller.common.URLConstants;
import com.openkoda.core.flow.Tuple;
import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.core.service.SessionService;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.User;
import com.openkoda.repository.user.UserRepository;
import com.openkoda.service.user.BasicPrivilegeService.PrivilegeChangeEvent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Primary Spring Security UserDetailsService implementation that loads User entities from the database
 * and constructs OrganizationUser principals with role and privilege mappings.
 * <p>
 * This service is marked with @Primary, making it the default UserDetailsService for authentication flows.
 * The LoginByPasswordOrTokenAuthenticationProvider uses this service during authentication to retrieve
 * user credentials and authorization data.
 * </p>
 * <p>
 * The service loads users via {@link UserRepository#findByEmailLowercase(String)}, queries UserRole
 * associations with {@link UserRepository#getUserRolesAndPrivileges(Long)}, and aggregates privileges
 * into global and per-organization sets. Privileges are stored in the Role table as joined strings
 * and parsed using {@link PrivilegeHelper}.
 * </p>
 * <p>
 * This service implements a privilege change subscription pattern. It maintains a ConcurrentHashMap
 * of subscribedUsers (email to List of Tuple pairs containing OrganizationUser and User) to enable
 * live privilege reloading. When a PrivilegeChangeEvent is published, subscribed users have their
 * privileges reloaded from the database without requiring reauthentication.
 * </p>
 * <p>
 * All database operations are executed within a transaction boundary via the @Transactional annotation,
 * ensuring consistent reads during authentication.
 * </p>
 * <p>
 * Example usage during Spring Security form login:
 * <pre>
 * OrganizationUser user = (OrganizationUser) userDetailsService.loadUserByUsername("user@example.com");
 * // Returns OrganizationUser with privileges from user_role and role tables
 * </pre>
 * </p>
 *
 * @see OrganizationUser
 * @see UserProvider
 * @see PrivilegeChangeEvent
 * @see UserRepository#getUserRolesAndPrivileges(Long)
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Primary
@Service("customUserDetailsService")
@Transactional
public class OrganizationUserDetailsService implements UserDetailsService, URLConstants, LoggingComponentWithRequestId {

    @Inject
    private UserRepository userRepository;

    /**
     * ConcurrentHashMap mapping user email addresses to lists of Tuple pairs containing
     * OrganizationUser and User instances. This map enables privilege change notifications
     * to reload principals without requiring reauthentication. The map is thread-safe to
     * support concurrent authentication requests.
     */
    private final Map<String, List<Tuple>> subscribedUsers = new ConcurrentHashMap<>();

    /**
     * Loads a User by email address and constructs an OrganizationUser principal with full
     * role and privilege mappings. This method implements the Spring Security UserDetailsService
     * contract and is called during authentication flows.
     * <p>
     * The method performs the following steps:
     * </p>
     * <ol>
     * <li>Queries the User entity via {@link UserRepository#findByEmailLowercase(String)} for case-insensitive lookup</li>
     * <li>Throws UsernameNotFoundException if no User is found (Spring Security standard exception)</li>
     * <li>Queries UserRole associations via {@link UserRepository#getUserRolesAndPrivileges(Long)} returning a List of Tuples
     *     with structure: (userRoleId, roleName, privilegesString, organizationId, organizationName)</li>
     * <li>Calls {@link #setUserDetails(OrganizationUser, User, List)} to aggregate privileges and build OrganizationUser</li>
     * <li>Registers the user in the subscribedUsers map for privilege change notifications</li>
     * </ol>
     * <p>
     * This method is thread-safe. The ConcurrentHashMap allows concurrent loadUserByUsername calls
     * without synchronization overhead.
     * </p>
     *
     * @param email the user email address for case-insensitive lookup
     * @return an OrganizationUser fully populated with global and organization-specific privileges, roles, and authorities
     * @throws UsernameNotFoundException if no User exists with the given email address
     */
    @Override
    public UserDetails loadUserByUsername(String email) {
        debug("[loadUserByUsername] {}", email);
        User user = userRepository.findByEmailLowercase(email);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        List<Tuple> info = userRepository.getUserRolesAndPrivileges(user.getId());
        
        OrganizationUser organizationUser = setUserDetails(null, user, info);
        if(!subscribedUsers.containsKey(email)) {
            subscribedUsers.put(email, new ArrayList<>());
        }

        subscribedUsers.get(email).add(new Tuple(organizationUser, user));
        
        return organizationUser;
    }
    
    /**
     * Reloads user privileges from the database after role or privilege modifications.
     * This method preserves the existing OrganizationUser instance and updates its
     * privilege sets with current database values.
     * <p>
     * This method is used by {@link UserProvider#wasModifiedSince(java.util.Date)} when
     * detecting privilege changes via the User.updatedOn timestamp. It enables live
     * privilege updates without requiring user reauthentication.
     * </p>
     *
     * @param organizationUser the existing OrganizationUser principal to reload
     * @param user the persistent User entity to query for UserRole associations
     * @return the updated OrganizationUser with current database privileges
     */
    public UserDetails reloadUserByUsername(OrganizationUser organizationUser, User user) {
        debug("[reloadUserByUsername] {}", user.getEmail());
        List<Tuple> info = userRepository.getUserRolesAndPrivileges(user.getId());
        return setUserDetails(organizationUser, user, info);
    }

    /**
     * Creates a new OrganizationUser from database information and applies privilege narrowing
     * from a base OrganizationUser. This method is used for token-based authentication where
     * privileges need to be restricted based on the token's allowed privileges.
     * <p>
     * The method creates a new OrganizationUser with full database privileges, then narrows
     * them to only those privileges retained in the base OrganizationUser. It also preserves
     * authentication metadata such as singleRequestAuth flag and authMethod from the base.
     * </p>
     * <p>
     * This is used by LoginByPasswordOrTokenAuthenticationProvider for token-based authentication
     * with privilege restrictions.
     * </p>
     *
     * @param user the persistent User entity with email, password, and enabled status
     * @param info the List of Tuples from getUserRolesAndPrivileges query with structure:
     *             (userRoleId, roleName, privilegesString, organizationId, organizationName)
     * @param base the base OrganizationUser providing retainedPrivileges, singleRequestAuth flag,
     *             and authMethod for privilege narrowing
     * @return an OrganizationUser with privileges from the database narrowed to base.retainedPrivileges
     */
    public OrganizationUser setUserDetails(User user, List<Tuple> info, OrganizationUser base) {
        OrganizationUser newOrganizationUser = setUserDetails(null, user, info);
        newOrganizationUser.retainPrivileges(base.getRetainedPrivileges());
        newOrganizationUser.setSingleRequestAuth(base.isSingleRequestAuth());
        newOrganizationUser.setAuthMethod(base.getAuthMethod());
        return newOrganizationUser;
    }
    
    public OrganizationUser setUserDetails(User user, List<Tuple> info) {
        return setUserDetails(null, user, info);
    }
    
    /**
     * Core privilege aggregation logic that constructs an OrganizationUser from database Tuple data.
     * This method aggregates role and privilege information from UserRole associations into global
     * and organization-specific privilege sets.
     * <p>
     * The method implements the following aggregation algorithm:
     * </p>
     * <ol>
     * <li>Iterates through the Tuple list extracting roleName, privilegesString (joined with
     *     {@link PrivilegeHelper#PRIVILEGES_JOINER}), organizationId, and organizationName</li>
     * <li>For null organizationId: adds privileges to globalPrivileges Set and adds roleName to globalRoles Set</li>
     * <li>For non-null organizationId: adds privileges to organizationPrivileges Map under organizationId key,
     *     adds roleName to organizationRoles, and stores organizationName</li>
     * <li>Converts globalRoles to Collection of GrantedAuthority with SimpleGrantedAuthority wrapper</li>
     * <li>Creates new OrganizationUser if organizationUser parameter is null, otherwise calls resetPrivileges()</li>
     * <li>Preserves authMethod and isSpoofed flags from SecurityContext if available</li>
     * <li>Sets session Locale from user.language (defaults to "en")</li>
     * </ol>
     * <p>
     * The privilegesString format is parsed by {@link PrivilegeHelper#fromJoinedStringToStringSet(String)}
     * which handles the "privilege1;privilege2;privilege3" format from the Role.privileges column.
     * </p>
     *
     * @param organizationUser the existing OrganizationUser to update, or null to create a new instance
     * @param user the persistent User entity with email, password, and enabled status
     * @param info the List of Tuples with structure: (userRoleId:Long, roleName:String, privilegesString:String,
     *             organizationId:Long, organizationName:String) from {@link UserRepository#getUserRolesAndPrivileges(Long)}
     * @return an OrganizationUser with aggregated privileges including globalPrivileges (organizationId==null),
     *         organizationPrivileges (organizationId!=null), and authorities derived from globalRoles
     */
    public OrganizationUser setUserDetails(final OrganizationUser organizationUser, final User user, List<Tuple> info) {
        Set<String> globalPrivileges = new HashSet<>();
        Set<String> globalRoles = new HashSet<>();
        Map<Long, Set<String>> organizationPrivileges = new HashMap<>();
        Map<Long, Set<String>> organizationRoles = new HashMap<>();
        Map<Long, String> organizationNames = new LinkedHashMap<>();
        Long firstOrganizationId = null;
        
        for (Tuple t : info) {
            Long userRoleId = t.v(Long.class, 0);
            String roleName = t.v(String.class, 1);
            String privilegesString = t.v(String.class, 2);
            Long organizationId = t.v(Long.class, 3);
            String organizationName = t.v(String.class, 4);

            if (organizationId == null) {
                globalPrivileges.addAll(PrivilegeHelper.fromJoinedStringToStringSet(privilegesString));
                globalRoles.add(roleName);
            } else {
                Set<String> roles = organizationRoles.get(organizationId);
                if (roles == null) {
                    roles = new HashSet<>();
                    organizationRoles.put(organizationId, roles);
                }
                Set<String> privileges = organizationPrivileges.get(organizationId);
                if (privileges == null) {
                    privileges = new HashSet<>();
                    organizationPrivileges.put(organizationId, privileges);
                }
                roles.add(roleName);
                privileges.addAll(PrivilegeHelper.fromJoinedStringToStringSet(privilegesString));
                organizationNames.put(organizationId, organizationName);

                if (firstOrganizationId == null) {
                    firstOrganizationId = organizationId;
                }
            }
        }

        Collection<? extends GrantedAuthority> authorities = globalRoles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        OrganizationUser newOrganizationUser = organizationUser;
        if(organizationUser == null) {
            newOrganizationUser = new OrganizationUser(
                    //User's email is username
                    user.getEmail(),
    
                    //if there is LoginAndPassword then use the password, default form authentication will need it
                    user.getLoginAndPassword() == null ? "" : user.getLoginAndPassword().getPassword(),
    
                    user.isEnabled(), true, true, true,
                    authorities, globalPrivileges, globalRoles, organizationPrivileges, organizationRoles, user, organizationNames);
            if(SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof OrganizationUser) {
                OrganizationUser principal = (OrganizationUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                newOrganizationUser.setAuthMethod(principal.getAuthMethod());
                newOrganizationUser.setSpoofed(principal.isSpoofed());
            }
        } else {
            organizationUser.resetPrivileges(globalPrivileges, globalRoles, organizationPrivileges, organizationRoles);

        }

        Locale userLocale = Locale.forLanguageTag(StringUtils.defaultIfBlank(user.getLanguage(), "en"));

        SessionService ss = SessionService.getInstance();
        ss.setAttributeIfSessionExists(SessionData.LOCALE, userLocale);

        return newOrganizationUser;
    }
    
    /**
     * Removes a user from the privilege change subscription registry. This method prevents
     * memory leaks from stale subscriptions by removing user entries from the subscribedUsers map.
     * <p>
     * This method is typically called during logout or session expiration to clean up
     * subscription data.
     * </p>
     *
     * @param email the user email address to unsubscribe from privilege change notifications
     * @return true always, indicating successful removal (even if user was not subscribed)
     */
    public boolean unsubscribeUser(String email) {
        subscribedUsers.remove(email);
        return true;
    }
    
    /**
     * Handles PrivilegeChangeEvent broadcasts and reloads subscribed OrganizationUser principals
     * with updated database privileges. This event listener enables live privilege propagation
     * without requiring user reauthentication.
     * <p>
     * The method performs the following steps:
     * </p>
     * <ol>
     * <li>Iterates through the subscribedUsers map extracting Tuple pairs of (OrganizationUser, User)</li>
     * <li>Calls {@link #reloadUserByUsername(OrganizationUser, User)} for each subscribed user
     *     to refresh their privileges from the database</li>
     * <li>Updates the SecurityContext principal via {@link UserProvider#getFromContext()} to
     *     reflect new privileges in the current session</li>
     * </ol>
     * <p>
     * This method is triggered by {@link com.openkoda.service.user.BasicPrivilegeService#publishPrivilegeChangeEvent()}
     * after role or privilege modifications. It prevents users from needing to re-login after
     * privilege changes by automatically updating their active sessions.
     * </p>
     */
    @EventListener(classes = PrivilegeChangeEvent.class)
    protected void onPrivilegesChanged( ) {
        subscribedUsers.entrySet().stream().flatMap( e -> e.getValue().stream()).forEach( u -> {
            debug("[onPrivilegesChanged] Privileges have changed, handling OrganizationUser {}", ((OrganizationUser)u.getV0()).getUsername());
            reloadUserByUsername((OrganizationUser)u.getV0(), (User)u.getV1());
            reloadUserByUsername(UserProvider.getFromContext().get(), (User)u.getV1());
        });
    }
}
