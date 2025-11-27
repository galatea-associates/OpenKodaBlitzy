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

import com.openkoda.core.helper.collections.UnmodifiableMapWithRemove;
import com.openkoda.core.helper.collections.UnmodifiableSetWithRemove;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.Privilege;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.authentication.LoggedUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Custom Spring Security principal holding global and per-organization privileges and roles for multi-tenant authorization.
 * <p>
 * This class extends Spring Security's {@link User} and implements {@link OAuth2User} for OAuth authentication flows
 * and {@link HasSecurityRules} for privilege checking API. It stores privilege and role information in multiple structures:
 * global privileges (Set), organization-scoped privileges (Map from organizationId to privilege Set), global roles,
 * and organization-scoped roles. This design enables Role-Based Access Control (RBAC) evaluation in a multi-tenant environment
 * where users can have different privilege sets in different organizations.
 * <p>
 * The collections are wrapped with {@link UnmodifiableSetWithRemove} and {@link UnmodifiableMapWithRemove} for thread-safety
 * while allowing privilege narrowing via {@code retainAll} operations. This immutability pattern prevents privilege escalation
 * while supporting token-based authentication that requires privilege subset restrictions.
 * <p>
 * Special flags tracked by this class include:
 * <ul>
 *   <li>{@code isSpoofed} - Indicates impersonation mode when RunAsService is active</li>
 *   <li>{@code isSingleRequestAuth} - Marks single-use token authentication where token is invalidated after use</li>
 *   <li>{@code authMethod} - Tracks authentication method (PASSWORD, TOKEN, or OAUTH)</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * OrganizationUser user = OrganizationUserDetailsService.loadUser(email);
 * boolean canRead = user.hasGlobalOrOrgPrivilege(Privilege.readData, orgId);
 * }</pre>
 *
 * @see OrganizationUserDetailsService
 * @see UserProvider
 * @see HasSecurityRules
 * @see com.openkoda.model.UserRole
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class OrganizationUser extends User implements OAuth2User, HasSecurityRules, LoggingComponentWithRequestId {

    /** Set of PrivilegeBase instances retained after privilege narrowing for token-based authentication. */
    private Set<PrivilegeBase> retainedPrivileges;
    
    /** Unmodifiable Set of privilege names granted globally across all organizations. */
    private Set<String> globalPrivileges;
    
    /** Unmodifiable Set of global role names assigned to this user. */
    private Set<String> globalRoles;
    
    /** Unmodifiable Map from organizationId to Set of privilege names granted in that organization for tenant-scoped privileges. */
    private Map<Long, Set<String>> organizationPrivileges;
    
    /** Map from organizationId to Set of role names assigned in that organization. */
    private Map<Long, Set<String>> organizationRoles;
    
    /** Map from organizationId to organization name for display purposes. */
    private final Map<Long, String> organizationNames;
    
    /** Reference to the persistent User entity from database. */
    private final com.openkoda.model.User user;
    
    /** First organization ID from organizationNames used as fallback for organization context. */
    private final Long defaultOrganizationId;
    
    /** Flag indicating impersonation mode when RunAsService is active. */
    private boolean isSpoofed;
    
    /** Flag for single-use token authentication where token is invalidated after use. */
    private boolean isSingleRequestAuth;
    
    /** Enum tracking authentication method: PASSWORD, TOKEN, or OAUTH. */
    private LoggedUser.AuthenticationMethods authMethod = LoggedUser.AuthenticationMethods.PASSWORD;
    
    /** OAuth2User instance for OAuth authentication flows. */
    private OAuth2User oauth2User;

    /**
     * Sentinel value -1L for JPQL IN operator to prevent crashes on empty collections.
     * This value MUST NOT match any existing Organization ID.
     */
    public static final Long nonExistingOrganizationId = -1L;
    
    /** Sentinel value -2L for user ID when no user is present. */
    public static final Long nonExistingUserId = -2L;
    
    /** Set containing only the nonExistingOrganizationId sentinel for empty organization queries. */
    private static final Set<Long> nonExistingOrganizationIds = Collections.singleton(nonExistingOrganizationId);

    /** Sentinel privilege name that cannot match any real privilege for empty privilege sets. */
    private static final String nonExistingPrivilege = " does not exist ";

    /**
     * Constructs an OrganizationUser with Spring Security principal data, privileges, and roles.
     * <p>
     * This constructor builds the custom principal from user authentication data and privilege/role mappings
     * collected from UserRole associations. The privilege and role collections are wrapped with unmodifiable
     * wrappers that support removal (for privilege narrowing) but block addition operations. A sentinel value
     * is added to privilege sets to ensure JPQL IN queries work correctly with empty collections.
     * 
     *
     * @param username User email used as Spring Security username
     * @param password Encrypted password (BCrypt hash from database)
     * @param enabled Account enabled flag from User entity
     * @param accountNonExpired Account expiration status
     * @param credentialsNonExpired Password expiration status
     * @param accountNonLocked Account lock status
     * @param authorities Collection of GrantedAuthority instances derived from privileges
     * @param globalPrivileges Set of privilege names granted globally across all organizations
     * @param globalRoles Set of global role names
     * @param organizationPrivileges Map from organizationId to Set of privilege names for tenant-scoped privileges
     * @param organizationRoles Map from organizationId to Set of role names
     * @param user Persistent User entity reference from database
     * @param organizationNames Map of organization IDs to organization names for display
     */
    public OrganizationUser(String username, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities, Set<String> globalPrivileges, Set<String> globalRoles, Map<Long, Set<String>> organizationPrivileges, Map<Long, Set<String>> organizationRoles, com.openkoda.model.User user, Map<Long, String> organizationNames) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);

        this.globalPrivileges = prepareImmutableSet(globalPrivileges, nonExistingPrivilege);
        this.globalRoles = prepareImmutableSet(globalRoles, null);
        this.organizationPrivileges = prepareImmutableSetsMap(organizationPrivileges, nonExistingPrivilege);
        this.organizationRoles = prepareImmutableSetsMap(organizationRoles, null);
        this.user = user;
        this.organizationNames = Collections.unmodifiableMap(organizationNames);
        this.isSpoofed = false;
        defaultOrganizationId = organizationNames.isEmpty() ? nonExistingOrganizationId : organizationNames.keySet().iterator().next();
    }

    /**
     * Wraps a Map of Sets with UnmodifiableMapWithRemove for thread-safe mutability control.
     * <p>
     * Each Set value in the map is wrapped with {@link #prepareImmutableSet(Set, String)} to allow
     * removal operations (for privilege narrowing) while blocking addition of new elements. This pattern
     * prevents privilege escalation while supporting token-based authentication with restricted privileges.
     * 
     *
     * @param map Map from organizationId to Set of privilege or role names
     * @param additionalValue Optional sentinel value to add to each Set (null to skip)
     * @return UnmodifiableMapWithRemove wrapping the transformed map
     */
    private Map<Long, Set<String>> prepareImmutableSetsMap(Map<Long, Set<String>> map, String additionalValue) {
        debug("[prepareImmutableSetsMap]");
        Map<Long, Set<String>> result = new HashMap<>(map.size());

        for ( Map.Entry<Long, Set<String>> e : map.entrySet()) {
            result.put(e.getKey(), prepareImmutableSet(e.getValue(), additionalValue));
        }

        return new UnmodifiableMapWithRemove(result);

    }

    /**
     * Wraps a Set with UnmodifiableSetWithRemove and optionally adds a sentinel value.
     * <p>
     * The wrapped Set allows removal operations via {@code retainAll} (for privilege narrowing) but blocks
     * {@code add} and {@code addAll} operations to prevent privilege escalation. If additionalValue is provided,
     * it is added to the Set before wrapping (typically a sentinel value like nonExistingPrivilege).
     * 
     *
     * @param set Set of privilege or role names to wrap
     * @param additionalValue Optional sentinel value to add before wrapping (null to skip)
     * @return UnmodifiableSetWithRemove wrapping the transformed set
     */
    private Set<String> prepareImmutableSet(Set<String> set, String additionalValue) {
        debug("[prepareImmutableSet]");
        boolean noAdditional = (additionalValue == null);
        if (noAdditional) { return new UnmodifiableSetWithRemove(set); }

        Set<String> result = new HashSet(set.size() + 1);
        result.addAll(set);
        result.add(additionalValue);
        return new UnmodifiableSetWithRemove(result);
    }

    /**
     * Retrieves the current OrganizationUser from Spring Security context.
     * <p>
     * This static convenience method delegates to {@link UserProvider#getFromContext()} to extract
     * the authenticated principal from SecurityContextHolder.
     * 
     *
     * @return Optional containing OrganizationUser if authenticated, empty Optional otherwise
     * @see UserProvider#getFromContext()
     */
    public static Optional<OrganizationUser> getFromContext() {
        return UserProvider.getFromContext();
    }

    /**
     * Checks if user has the specified global privilege by name.
     * <p>
     * Global privileges apply across all organizations and are typically granted to administrators
     * or system-wide roles.
     * 
     *
     * @param p Privilege name to check
     * @return true if user has the global privilege, false otherwise
     */
    public boolean hasGlobalPrivilege(String p) {
        return hasGlobalPrivilege(p, globalPrivileges);
    }

    /**
     * Checks if user has the specified organization-scoped privilege for the given organization.
     * <p>
     * Organization-scoped privileges are tenant-specific and only apply within the context of
     * a particular organization.
     * 
     *
     * @param p Privilege name to check
     * @param orgId Organization ID to check privilege for
     * @return true if user has the privilege in the specified organization, false otherwise
     */
    public boolean hasOrgPrivilege(String p, Long orgId) {
        return hasOrgPrivilege(p, orgId, organizationPrivileges);
    }

    /**
     * Checks if user has the specified global privilege by enum.
     * <p>
     * This method accepts a Privilege enum value instead of a String name for type-safe
     * privilege checking.
     * 
     *
     * @param p Privilege enum to check
     * @return true if user has the global privilege, false otherwise
     * @see Privilege
     */
    public boolean hasGlobalPrivilege(Privilege p) {
        return hasGlobalPrivilege(p, globalPrivileges);
    }

    /**
     * Checks if user has the specified organization-scoped privilege by enum.
     * <p>
     * This method accepts a Privilege enum value for type-safe privilege checking within
     * a specific organization context.
     * 
     *
     * @param p Privilege enum to check
     * @param orgId Organization ID to check privilege for
     * @return true if user has the privilege in the specified organization, false otherwise
     * @see Privilege
     */
    public boolean hasOrgPrivilege(Privilege p, Long orgId) {
        return hasOrgPrivilege(p, orgId, organizationPrivileges);
    }

    /**
     * Checks if user has the specified privilege either globally or in the given organization.
     * <p>
     * This compound check returns true if the user has the privilege as a global privilege
     * OR as an organization-scoped privilege for the specified organization. This is commonly
     * used for authorization checks where either global or organization-level access is sufficient.
     * 
     *
     * @param privilege Privilege enum to check
     * @param orgId Organization ID to check organization-scoped privilege for
     * @return true if user has global privilege or organization privilege for the specified organization
     */
    public boolean hasGlobalOrOrgPrivilege(Privilege privilege, Long orgId) {
        return hasGlobalOrOrgPrivilege(privilege, orgId, globalPrivileges, organizationPrivileges);
    }

    /**
     * Checks if user has the specified privilege either globally or in the given organization by name.
     * <p>
     * This compound check returns true if the user has the privilege as a global privilege
     * OR as an organization-scoped privilege for the specified organization.
     * 
     *
     * @param privilegeName Privilege name to check
     * @param orgId Organization ID to check organization-scoped privilege for
     * @return true if user has global privilege or organization privilege for the specified organization
     */
    public boolean hasGlobalOrOrgPrivilege(String privilegeName, Long orgId) {
        return hasGlobalOrOrgPrivilege(privilegeName, orgId, globalPrivileges, organizationPrivileges);
    }

    /**
     * Returns the Set of organization IDs where user has any privileges.
     * <p>
     * This method returns the keyset from organizationPrivileges map. If the user has no
     * organization-scoped privileges, returns a sentinel set containing nonExistingOrganizationId
     * to prevent JPQL IN operator crashes on empty collections.
     * 
     *
     * @return Set of organization IDs where user has privileges, or sentinel set if none
     */
    public Set<Long> getOrganizationIds() {
        trace("[getOrganizationIds]");
        Set<Long> result = organizationPrivileges.keySet();
        return result.isEmpty() ? nonExistingOrganizationIds : result;
    }

    /**
     * Returns the Set of organization IDs where user has the specific privilege.
     * <p>
     * This method filters organizationPrivileges to find only organizations where the user
     * has the specified privilege name. Used for queries that need to restrict results to
     * organizations where the user has specific access rights.
     * 
     *
     * @param privilegeName Privilege name to filter by
     * @return Set of organization IDs where user has the specified privilege, or sentinel set if none
     */
    public Set<Long> getOrganizationIdsWithPrivilege(String privilegeName) {
        debug("[getOrganizationIdsWithPrivilege] {}", privilegeName);
        Set<Long> result = organizationPrivileges.entrySet().stream().filter( a -> a.getValue().contains(privilegeName)).map( a -> a.getKey() ).collect(Collectors.toSet());
        return result.isEmpty() ? nonExistingOrganizationIds : result;
    }
    
    /**
     * Returns List of concatenated "organizationId+privilegeName" strings for JPA Criteria API IN predicates.
     * <p>
     * This method generates strings by concatenating each organization ID with each privilege name
     * the user has in that organization. The resulting strings are used in JPA Criteria API queries
     * for efficient multi-value filtering. Returns a list containing an empty string if the user
     * has no organization privileges.
     * 
     *
     * @return List of "orgId+privilegeName" concatenations for JPA IN predicates
     */
    public List<String> getOrganizationWithPrivilegePairs() {
        debug("[getOrganizationWithPrivilegePairs]");
        List<String> result = new ArrayList<>();
        if(organizationPrivileges.entrySet().isEmpty()) {
            result.add("");
        }
        for (Map.Entry<Long, Set<String>> e : organizationPrivileges.entrySet()) {
            for (String s: e.getValue()) {
                result.add(e.getKey() + s);
            }
        }
        return result;
    }

    /**
     * Returns the first organization ID or sentinel value if user has no organizations.
     * <p>
     * This method returns the first organization ID from organizationNames map, or
     * nonExistingOrganizationId sentinel if the user has no organization associations.
     * Used as a fallback organization context when no specific organization is specified.
     * 
     *
     * @return First organization ID or nonExistingOrganizationId sentinel
     */
    public Long getDefaultOrganizationId() {
        return defaultOrganizationId;
    }

    /**
     * Returns Collection containing global roles and organization roles maps.
     * <p>
     * This method returns a list with two elements: the globalRoles Set and the
     * organizationRoles Map. Used for displaying role information in administrative interfaces.
     * 
     *
     * @return Collection containing [globalRoles Set, organizationRoles Map]
     */
    public Collection<?> getRolesInfo() {
        return Arrays.asList(globalRoles, organizationRoles);
    }

    /**
     * Returns the persistent User entity reference from the database.
     * <p>
     * This method returns the User entity that was loaded during authentication. May return null
     * for synthetic principals like the anonymous user or OAuth-only authentication flows.
     * 
     *
     * @return User entity or null if no persistent user exists
     * @see com.openkoda.model.User
     */
    public com.openkoda.model.User getUser() {
        return user;
    }

    /**
     * Returns the user database ID or sentinel value if no user exists.
     * <p>
     * This method returns the ID from the User entity, or nonExistingUserId sentinel if
     * the user reference is null. The sentinel value prevents null pointer exceptions and
     * enables safe usage in JPQL queries.
     * 
     *
     * @return User database ID or nonExistingUserId sentinel
     */
    public long getUserId() {
        return user == null ? nonExistingUserId : user.getId();
    }

    /**
     * Returns unmodifiable Map of organization IDs to organization names.
     * <p>
     * This map contains the names of all organizations where the user has any privileges.
     * Used for display purposes in user interfaces and organization selection dropdowns.
     * 
     *
     * @return Unmodifiable Map from organizationId to organization name
     */
    public Map<Long, String> getOrganizationNames() {
        return organizationNames;
    }

    /**
     * Returns the Set of global privilege names.
     *
     * @return Unmodifiable Set of global privilege names
     */
    public Set<String> getGlobalPrivileges() { return globalPrivileges;  }

    /**
     * Returns true if this user is in impersonation mode (RunAsService active).
     * <p>
     * When a user is spoofed, an administrator is impersonating them to diagnose issues
     * or perform operations on their behalf.
     * 
     *
     * @return true if user is being impersonated, false otherwise
     * @see RunAsService
     */
    public boolean isSpoofed() {
        return isSpoofed;
    }

    /**
     * Sets the impersonation flag for RunAsService.
     *
     * @param spoofed true to mark user as impersonated, false otherwise
     */
    public void setSpoofed(boolean spoofed) {
        isSpoofed = spoofed;
    }

    /**
     * Returns the Set of privileges retained after privilege narrowing.
     * <p>
     * When token-based authentication restricts privileges to a subset, this field stores
     * the PrivilegeBase instances that were retained via {@link #retainPrivileges(Set)}.
     * 
     *
     * @return Set of retained PrivilegeBase instances or null if no narrowing occurred
     */
    public Set<PrivilegeBase> getRetainedPrivileges() {
        return retainedPrivileges;
    }

    /**
     * Returns true if this authentication is for a single request only.
     * <p>
     * Single-request authentication is used for temporary tokens that are invalidated
     * immediately after use, such as password reset tokens or email verification links.
     * 
     *
     * @return true if token is single-use, false otherwise
     */
    public boolean isSingleRequestAuth() {
        return isSingleRequestAuth;
    }

    /**
     * Sets the single-request authentication flag.
     *
     * @param singleRequestAuth true to mark authentication as single-use
     */
    public void setSingleRequestAuth(boolean singleRequestAuth) {
        isSingleRequestAuth = singleRequestAuth;
    }

    /**
     * Returns the authentication method used for this session.
     * <p>
     * Possible values: PASSWORD (form login), TOKEN (API token or temporary token), OAUTH (OAuth2 provider).
     * 
     *
     * @return Authentication method enum value
     */
    public LoggedUser.AuthenticationMethods getAuthMethod() {
        return authMethod;
    }

    /**
     * Sets the authentication method for this session.
     *
     * @param authMethod Authentication method enum (PASSWORD, TOKEN, or OAUTH)
     */
    public void setAuthMethod(LoggedUser.AuthenticationMethods authMethod) {
        this.authMethod = authMethod;
    }
    
    /**
     * Returns the Set of global role names.
     *
     * @return Set of global role names
     */
    public Set<String> getGlobalRoles() {
        return globalRoles;
    }

    /**
     * Sets the global roles for this user.
     * <p>
     * Used by {@link UserProvider} when reloading privileges after database changes.
     * 
     *
     * @param globalRoles Set of global role names to assign
     */
    public void setGlobalRoles(Set<String> globalRoles) {
        this.globalRoles = globalRoles;
    }

    /**
     * Returns the Map of organization-scoped privileges.
     *
     * @return Map from organizationId to Set of privilege names
     */
    public Map<Long, Set<String>> getOrganizationPrivileges() {
        return organizationPrivileges;
    }

    /**
     * Sets the organization-scoped privileges for this user.
     * <p>
     * Used by {@link UserProvider} when reloading privileges after database changes.
     * 
     *
     * @param organizationPrivileges Map from organizationId to Set of privilege names
     */
    public void setOrganizationPrivileges(Map<Long, Set<String>> organizationPrivileges) {
        this.organizationPrivileges = organizationPrivileges;
    }

    /**
     * Returns the Map of organization-scoped roles.
     *
     * @return Map from organizationId to Set of role names
     */
    public Map<Long, Set<String>> getOrganizationRoles() {
        return organizationRoles;
    }

    /**
     * Sets the organization-scoped roles for this user.
     * <p>
     * Used by {@link UserProvider} when reloading privileges after database changes.
     * 
     *
     * @param organizationRoles Map from organizationId to Set of role names
     */
    public void setOrganizationRoles(Map<Long, Set<String>> organizationRoles) {
        this.organizationRoles = organizationRoles;
    }

    /**
     * Sets the global privileges for this user.
     * <p>
     * Used by {@link UserProvider} when reloading privileges after database changes.
     * 
     *
     * @param globalPrivileges Set of global privilege names to assign
     */
    public void setGlobalPrivileges(Set<String> globalPrivileges) {
        this.globalPrivileges = globalPrivileges;
    }

    /**
     * Narrows privileges to the specified subset for token-based authentication with restricted access.
     * <p>
     * While privileges should be immutable to prevent escalation, certain authentication scenarios
     * (like API tokens with limited scope) require privilege narrowing. This method uses {@code retainAll}
     * on the unmodifiable collections that support removal operations to restrict privileges to the
     * specified subset. The collections block addition of new privileges to prevent escalation.
     * 
     *
     * @param privilegesToLeave Set of PrivilegeBase instances to retain (null to skip narrowing)
     */
    void retainPrivileges(Set<PrivilegeBase> privilegesToLeave) {
        debug("[retainPrivileges]");
        if (privilegesToLeave == null) {
            return;
        }
        retainedPrivileges = privilegesToLeave;
        Set<String> privileges = privilegesToLeave.stream().map(s -> s.name()).collect(Collectors.toSet());
        globalPrivileges.retainAll(privileges);
        organizationPrivileges.forEach( (k, v) -> v.retainAll(privileges));
    }

    /**
     * Singleton empty OrganizationUser instance for unauthenticated contexts.
     * <p>
     * This instance has username "_anonymous_", empty password, all account flags set to true,
     * and a single "void privilege" that cannot match any real privilege. Used as a safe default
     * principal when no authenticated user is present.
     * 
     */
    public static final OrganizationUser empty = new OrganizationUser(
            "_anonymous_",
            "",
            true,
            true,
            true,
            true,
            Collections.emptySet(), Collections.singleton(" void privilege "), Collections.emptySet(), Collections
            .emptyMap(),
            Collections.emptyMap(), null, Collections.emptyMap());

    /**
     * Returns OAuth2 user attributes map.
     * <p>
     * This method implements the {@link OAuth2User} interface and delegates to the oauth2User
     * field. Returns attributes provided by the OAuth2 provider (e.g., email, name, profile picture).
     * 
     *
     * @return Map of OAuth2 attribute names to values
     * @throws NullPointerException if oauth2User is null
     */
    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    /**
     * Returns OAuth2 user name attribute.
     * <p>
     * This method implements the {@link OAuth2User} interface and retrieves the "name" attribute
     * from the OAuth2 provider's user info response.
     * 
     *
     * @return User name from OAuth2 provider
     * @throws NullPointerException if oauth2User is null
     */
    @Override
    public String getName() {
        return oauth2User.getAttribute("name");
    }

    /**
     * Sets the OAuth2User instance for OAuth authentication flows.
     * <p>
     * This method is called by the OAuth2 authentication success handler to attach the
     * OAuth2User data to the OrganizationUser principal.
     * 
     *
     * @param oauth2User OAuth2User instance from authentication provider
     */
    public void setOauth2User(OAuth2User oauth2User) {
        this.oauth2User = oauth2User;
    }
    
    /**
     * Replaces all privilege and role maps with new values.
     * <p>
     * This method is used by {@link UserProvider} to reload privileges after database changes
     * (e.g., role assignment modifications). Unlike setters for individual maps, this method
     * replaces all four privilege/role structures atomically.
     * 
     *
     * @param globalPrivileges New Set of global privilege names
     * @param globalRoles New Set of global role names
     * @param organizationPrivileges New Map from organizationId to privilege names
     * @param organizationRoles New Map from organizationId to role names
     * @return true (always succeeds)
     */
    public boolean resetPrivileges(Set<String> globalPrivileges, Set<String> globalRoles, Map<Long, Set<String>> organizationPrivileges, Map<Long, Set<String>> organizationRoles) {
        this.globalPrivileges = globalPrivileges;
        this.globalRoles = globalRoles;
        this.organizationPrivileges = organizationPrivileges;
        this.organizationRoles = organizationRoles;
        return true;
    }
}
