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

package com.openkoda.repository.user;

import com.openkoda.core.flow.Tuple;
import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.Organization;
import com.openkoda.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository managing User entities for authentication, authorization, and user profile data.
 * <p>
 * This is the central repository for user management in the OpenKoda platform. It extends
 * {@link UnsecuredFunctionalRepositoryWithLongId} and implements {@link HasSecurityRules} to provide
 * comprehensive user lookup methods including authentication (login/email), OAuth provider integration
 * (Google, Facebook, LDAP, Salesforce, LinkedIn), role/privilege queries, and organization membership.

 * <p>
 * Key features:
 * <ul>
 *   <li>Authentication queries: findByLogin, findByEmail variants with case-insensitive matching</li>
 *   <li>OAuth provider lookups: findByGoogleId, findByFacebookId, findByLDAPId, findBySalesforceId, findByLinkedinId</li>
 *   <li>Organization membership: findOrganizations returning list of user's organizations</li>
 *   <li>Role/privilege projections: getUserGlobalRolePrivileges, getUserRolesAndPrivileges, getUserOrganizationRolePrivileges with Tuple DTOs</li>
 *   <li>Specification+Pageable listing with @PreAuthorize CHECK_CAN_READ_USER_DATA security</li>
 *   <li>SpEL-based findAll(Pageable) with dynamic ROLE_ADMIN and organization scope filtering</li>
 *   <li>Modification tracking: wasModifiedSince, setUserAsModified for optimistic locking</li>
 * </ul>

 * <p>
 * Security: Several methods use SpEL expressions (?#{hasRole()}, ?#{principal.user.id}, ?#{principal.organizationIds})
 * for dynamic query filtering based on current authentication context. CHECK_CAN_READ_USER_OR_OWNER_JPQL fragment
 * enforces privilege checks or ownership verification.

 * <p>
 * Persists to 'users' table with associations to LoginAndPassword, OAuth user tables, UserRole, and Organization.

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see User
 * @see com.openkoda.model.authentication.LoginAndPassword
 * @see com.openkoda.repository.user.UserRoleRepository
 * @see Organization
 * @see UnsecuredFunctionalRepositoryWithLongId
 * @see HasSecurityRules
 */
@Repository
public interface UserRepository extends UnsecuredFunctionalRepositoryWithLongId<User>, HasSecurityRules {

    /**
     * Retrieves all organizations associated with the user via UserRole assignments.
     * <p>
     * Executes JPQL: {@code SELECT ur.organization FROM UserRole ur WHERE ur.organizationId IS NOT NULL AND ur.userId = :id}
     * Returns only organization-scoped UserRole entries (excludes global roles with null organizationId).

     *
     * @param userId User ID to query organizations for, must not be null
     * @return List of Organization entities user is member of, empty list if no organization memberships
     */
    @Query("select ur.organization from UserRole ur where ur.organizationId is not null and ur.userId = :id")
    List<Organization> findOrganizations(@Param("id") Long userId);

    /**
     * Finds all User entities matching specification with pagination and privilege enforcement.
     * <p>
     * Secured with @PreAuthorize annotation enforcing CHECK_CAN_READ_USER_DATA privilege.
     * Executes dynamic JPA Criteria query built from Specification parameter.
     * Throws AccessDeniedException if user lacks required privilege.

     *
     * @param specification JPA Specification for dynamic query construction, may be null for all users
     * @param pageable Pagination and sorting parameters, must not be null
     * @return Page of User entities matching specification, empty page if none found
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_READ_USER_DATA privilege
     */
    @PreAuthorize(CHECK_CAN_READ_USER_DATA)
    Page<User> findAll(Specification specification, Pageable pageable);

    /**
     * Finds User by LoginAndPassword login field (username).
     * <p>
     * Executes JPQL: {@code SELECT u FROM User u WHERE u.loginAndPassword.login = :login}
     * Login comparison is case-sensitive. Used for username/password authentication.

     *
     * @param login Username string from LoginAndPassword entity, must not be null
     * @return User with matching login, null if not found
     */
    @Query("SELECT u FROM User u WHERE u.loginAndPassword.login = :login ")
    User findByLogin(@Param("login") String login);

    /**
     * Finds User by email who does NOT have LoginAndPassword credentials (OAuth-only users).
     * <p>
     * Executes JPQL: {@code SELECT u FROM User u WHERE u.email = :email AND NOT EXISTS (SELECT lap FROM LoginAndPassword lap WHERE lap.user = u)}
     * Filters out users with local username/password authentication. Used for OAuth user lookups.

     *
     * @param email User email address, must not be null
     * @return User with matching email and no LoginAndPassword record, null if not found or has login
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND NOT EXISTS (SELECT lap FROM LoginAndPassword lap WHERE lap.user = u)")
    User findByEmailWithoutLogin(@Param("email") String email);

    /**
     * Retrieves lowercase email for enabled user by login (username).
     * <p>
     * Executes JPQL: {@code SELECT u.email FROM User u WHERE u.loginAndPassword.login = LOWER(:login) AND u.loginAndPassword.enabled = true}
     * Returns email string projection only (not full User entity). Filters by enabled flag.

     *
     * @param login Username to search for (converted to lowercase in query), must not be null
     * @return User's email address in lowercase if login enabled, null if not found or disabled
     */
    @Query("SELECT u.email FROM User u WHERE u.loginAndPassword.login = LOWER(:login) AND u.loginAndPassword.enabled = true")
    String findUsernameLowercaseByLoginIsEnabled(@Param("login") String login);

    /**
     * Retrieves lowercase email for user by login (username) regardless of enabled status.
     * <p>
     * Executes JPQL: {@code SELECT u.email FROM User u WHERE u.loginAndPassword.login = LOWER(:login)}
     * Returns email string projection only. Does not filter by enabled flag.

     *
     * @param login Username to search for (converted to lowercase in query), must not be null
     * @return User's email address in lowercase, null if not found
     */
    @Query("SELECT u.email FROM User u WHERE u.loginAndPassword.login = LOWER(:login)")
    String findUsernameLowercaseByLogin(@Param("login") String login);

    /**
     * Finds User by email with case-insensitive matching.
     * <p>
     * Executes JPQL: {@code SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)}
     * Applies LOWER function to both email and parameter for case-insensitive search.

     *
     * @param email Email address to search for (case-insensitive), must not be null
     * @return User with matching email (case-insensitive), null if not found
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) ")
    User findByEmailLowercase(@Param("email") String email);

    /**
     * Retrieves User ID by email with case-insensitive matching.
     * <p>
     * Executes JPQL: {@code SELECT u.id FROM User u WHERE LOWER(u.email) = LOWER(:email)}
     * Returns Long ID projection only (not full User entity). Case-insensitive email matching.

     *
     * @param email Email address to search for (case-insensitive), must not be null
     * @return User ID if matching email found, null otherwise
     */
    @Query("SELECT u.id FROM User u WHERE LOWER(u.email) = LOWER(:email) ")
    Long findIdByEmailLowercase(@Param("email") String email);

    /**
     * Finds User entity by primary key ID.
     * <p>
     * Overrides inherited findById to use JPQL explicitly: {@code SELECT u FROM User u WHERE u.id = :id}

     *
     * @param id User primary key ID, must be positive
     * @return User with matching ID, null if not found
     */
    @Query("SELECT u FROM User u WHERE u.id = :id")
    User findById(@Param("id") long id);

    /**
     * Retrieves updatedOn timestamp for User without loading full entity.
     * <p>
     * Executes JPQL: {@code SELECT u.updatedOn FROM User u WHERE u.id = :userId}
     * Returns LocalDateTime projection only. Used for optimistic locking and modification tracking.

     *
     * @param id User ID to query timestamp for, must be positive
     * @return User's updatedOn timestamp, null if user not found
     */
    @Query("SELECT u.updatedOn FROM User u WHERE u.id = :userId")
    LocalDateTime getUpdatedOn(@Param("userId") long id);

    /**
     * Finds User by Google OAuth identifier for Google Sign-In integration.
     * <p>
     * Executes JPQL: {@code SELECT u FROM User u WHERE u.googleUser.googleId = :googleId}
     * Joins to GoogleUser association. Used by Google OAuth callback controller.

     *
     * @param googleId Google user identifier from OAuth response, must not be null
     * @return User with matching Google ID, null if not found
     */
    @Query("SELECT u FROM User u WHERE u.googleUser.googleId = :googleId")
    User findByGoogleId(@Param("googleId") String googleId);

    /**
     * Finds User by Facebook OAuth identifier for Facebook Login integration.
     * <p>
     * Executes JPQL: {@code SELECT u FROM User u WHERE u.facebookUser.facebookId = :facebookId}
     * Joins to FacebookUser association. Used by Facebook OAuth callback controller.

     *
     * @param facebookId Facebook user identifier from OAuth response, must not be null
     * @return User with matching Facebook ID, null if not found
     */
    @Query("SELECT u FROM User u WHERE u.facebookUser.facebookId = :facebookId ")
    User findByFacebookId(@Param("facebookId") String facebookId);

    /**
     * Finds User by LDAP uid for LDAP/Active Directory integration.
     * <p>
     * Executes JPQL: {@code SELECT u FROM User u WHERE u.ldapUser.uid = :ldapId}
     * Joins to LDAPUser association. Used by LDAP authentication provider.

     *
     * @param ldapId LDAP uid from directory server, must not be null
     * @return User with matching LDAP uid, null if not found
     */
    @Query("SELECT u FROM User u WHERE u.ldapUser.uid = :ldapId ")
    User findByLDAPId(@Param("ldapId") String ldapId);

    /**
     * Finds User by Salesforce OAuth identifier for Salesforce integration.
     * <p>
     * Executes JPQL: {@code SELECT u FROM User u WHERE u.salesforceUser.salesforceId = :salesforceId}
     * Joins to SalesforceUser association. Used by Salesforce OAuth callback controller.

     *
     * @param salesforceId Salesforce user identifier from OAuth response, must not be null
     * @return User with matching Salesforce ID, null if not found
     */
    @Query("SELECT u FROM User u WHERE u.salesforceUser.salesforceId = :salesforceId ")
    User findBySalesforceId(@Param("salesforceId") String salesforceId);

    /**
     * Finds User by LinkedIn OAuth identifier for LinkedIn Sign-In integration.
     * <p>
     * Executes JPQL: {@code SELECT u FROM User u WHERE u.linkedinUser.linkedinId = :linkedinId}
     * Joins to LinkedinUser association. Used by LinkedIn OAuth callback controller.

     *
     * @param linkedinId LinkedIn user identifier from OAuth response, must not be null
     * @return User with matching LinkedIn ID, null if not found
     */
    @Query("SELECT u FROM User u WHERE u.linkedinUser.linkedinId = :linkedinId ")
    User findByLinkedinId(@Param("linkedinId") String linkedinId);

    /**
     * Finds all User entities with dynamic SpEL-based security filtering and pagination.
     * <p>
     * Overrides inherited findAll(Pageable) with JPQL using Spring Security SpEL expressions:
     * {@code SELECT DISTINCT ur.user FROM UserRole ur WHERE ?#{hasRole('ROLE_ADMIN')} = true OR ur.userId = ?#{principal.user.id} OR ur.organizationId IN ?#{principal.organizationIds}}

     * <p>
     * Security filtering:
     * <ul>
     *   <li>ROLE_ADMIN: Returns ALL users if current user has ROLE_ADMIN</li>
     *   <li>Self: Returns current user (ur.userId = principal.user.id)</li>
     *   <li>Organization scope: Returns users in current user's organizations (ur.organizationId IN principal.organizationIds)</li>
     * </ul>
     * Uses DISTINCT to avoid duplicates from multiple UserRole entries per user.

     *
     * @param page Pagination and sorting parameters, must not be null
     * @return Page of User entities accessible to current user based on role and organization scope
     */
    @Override
    @Query("select DISTINCT ur.user from UserRole ur where ?#{hasRole('ROLE_ADMIN')} = true OR ur.userId = ?#{principal.user.id} OR ur.organizationId in ?#{principal.organizationIds} ")
    Page<User> findAll(Pageable page);

    /**
     * Retrieves global role names and privileges for user as Tuple projections.
     * <p>
     * Executes JPQL: {@code SELECT new Tuple(dbUserRole.role.name, dbUserRole.role.privileges) FROM UserRole WHERE userId = :id AND organizationId IS NULL AND CHECK_CAN_READ_USER_OR_OWNER_JPQL}
     * Returns only global UserRole entries (organizationId null). Enforces privilege or ownership check.

     * <p>
     * Tuple structure: (roleName:String, privileges:String)
     * Privileges stored as joined string: '(PRIVILEGE1)(PRIVILEGE2)(PRIVILEGE3)'

     *
     * @param userId User ID to query global roles for, must not be null
     * @return List of Tuple(roleName, privileges) for global roles, empty list if no global roles
     */
    @Query("SELECT new com.openkoda.core.flow.Tuple(dbUserRole.role.name, dbUserRole.role.privileges) from UserRole dbUserRole where dbUserRole.userId = :id and dbUserRole.organizationId is null AND "
            + CHECK_CAN_READ_USER_OR_OWNER_JPQL)
    List<Tuple> getUserGlobalRolePrivileges(@Param("id") Long userId);

    /**
     * Retrieves all role names, privileges, organization IDs, and organization names for user.
     * <p>
     * Executes JPQL: {@code SELECT new Tuple(roleName, privileges, organizationId, organizationName) FROM UserRole WHERE userId = :id AND CHECK_CAN_READ_USER_OR_OWNER_JPQL ORDER BY organizationName}
     * Returns both global (organizationId null) and organization-scoped roles. Ordered by organization name.

     * <p>
     * Tuple structure: (roleName:String, privileges:String, organizationId:Long, organizationName:String)

     *
     * @param userId User ID to query roles for, must not be null
     * @return List of Tuple(roleName, privileges, orgId, orgName) ordered by organization name, empty list if no roles
     */
    @Query("SELECT new com.openkoda.core.flow.Tuple(dbUserRole.role.name, dbUserRole.role.privileges, dbUserRole.organizationId, dbUserRole.organization.name) from UserRole dbUserRole where dbUserRole.userId = :id AND "
            + CHECK_CAN_READ_USER_OR_OWNER_JPQL + " order by dbUserRole.organization.name")
    List<Tuple> getUserRolesAndPrivileges2(@Param("id") Long userId);

    /**
     * Retrieves comprehensive user and default organization roles with privileges using LEFT JOIN.
     * <p>
     * Executes complex JPQL text block with LEFT JOIN:
     * <ul>
     *   <li>Returns UserRole entries WHERE userId = :id (user-specific roles)</li>
     *   <li>OR UserRole entries WHERE userId IS NULL AND organizationId IN user's organizations (default org roles)</li>
     *   <li>Uses LEFT JOIN to Organization for name (COALESCE empty string if global role)</li>
     *   <li>Ordered by UserRole.id for stable result ordering</li>
     * </ul>

     * <p>
     * Tuple structure: (userRoleId:Long, roleName:String, privileges:String, organizationId:Long, organizationName:String)
     * Includes default organization roles (userId null) inherited by users in those organizations.

     *
     * @param userId User ID to query roles for, must not be null
     * @return List of Tuple(userRoleId, roleName, privileges, orgId, orgName) including defaults, empty list if no roles
     */
    @Query("""
        SELECT
            new com.openkoda.core.flow.Tuple(
            dbUserRole.id,
            dbUserRole.role.name,
            dbUserRole.role.privileges,
            dbUserRole.organizationId,
            COALESCE(dbOrganization.name, ''))
        FROM UserRole dbUserRole
        LEFT JOIN dbUserRole.organization dbOrganization
        WHERE dbUserRole.userId = :id
            OR ((dbUserRole.userId is null)
                AND (dbUserRole.organizationId in
                    (select dbUserRole2.organizationId from UserRole dbUserRole2 where dbUserRole2.userId = :id)))
        ORDER BY dbUserRole.id
        """)
    List<Tuple> getUserRolesAndPrivileges(@Param("id") Long userId);

    /**
     * Retrieves organization IDs and privileges for user's organization-scoped roles only.
     * <p>
     * Executes JPQL: {@code SELECT new Tuple(organizationId, privileges) FROM UserRole WHERE userId = :id AND organizationId IS NOT NULL AND CHECK_CAN_READ_USER_OR_OWNER_JPQL}
     * Filters to organization-scoped roles only (excludes global roles with null organizationId).

     * <p>
     * Tuple structure: (organizationId:Long, privileges:String)

     *
     * @param userId User ID to query organization roles for, must not be null
     * @return List of Tuple(orgId, privileges) for organization-scoped roles, empty list if no org roles
     */
    @Query("SELECT new com.openkoda.core.flow.Tuple(dbUserRole.organizationId, dbUserRole.role.privileges) from UserRole dbUserRole where dbUserRole.userId = :id and dbUserRole.organizationId is not null AND "
            + CHECK_CAN_READ_USER_OR_OWNER_JPQL)
    List<Tuple> getUserOrganizationRolePrivileges(@Param("id") Long userId);

    /**
     * Checks if User was modified after specified timestamp.
     * <p>
     * Executes JPQL: {@code SELECT u.updatedOn > :since FROM User u WHERE u.id = :id}
     * Returns Boolean comparison result wrapped in Optional. Used for optimistic locking checks.

     *
     * @param userId User ID to check modification for, must not be null
     * @param since Timestamp to compare against, must not be null
     * @return Optional containing true if modified after since, false if not modified, empty if user not found
     */
    @Query("SELECT u.updatedOn > :since from User u where u.id = :id")
    Optional<Boolean> wasModifiedSince(@Param("id") Long userId, @Param("since") LocalDateTime since);

    /**
     * Updates User's updatedOn timestamp to current time without loading full entity.
     * <p>
     * Executes JPQL bulk update: {@code UPDATE User u SET u.updatedOn = CURRENT_TIMESTAMP WHERE u.id = :id}
     * Annotated with @Modifying and @Transactional for write operation. Bypasses JPA lifecycle callbacks.
     * Used to trigger modification tracking without changing other fields.

     *
     * @param userId User ID to mark as modified, must not be null
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.updatedOn = CURRENT_TIMESTAMP where u.id = :id")
    void setUserAsModified(@Param("id") Long userId);

    /**
     * Retrieves User if they have specified privilege in specified organization.
     * <p>
     * Executes JPQL: {@code SELECT ur.user FROM UserRole WHERE userId = :userId AND organizationId = :organizationId AND role.privileges LIKE %:privilege% AND CHECK_CAN_READ_USER_OR_OWNER_JPQL}
     * Uses LIKE operator to search privilege in joined string format '(PRIVILEGE1)(PRIVILEGE2)'.
     * Enforces privilege or ownership check via CHECK_CAN_READ_USER_OR_OWNER_JPQL.

     *
     * @param userId User ID to query, must not be null
     * @param organizationId Organization ID to check privilege in, must not be null
     * @param privilege Privilege name to search for (e.g., 'PRIVILEGE_READ'), must not be null
     * @return User if they have specified privilege in organization, null otherwise
     */
    @Query(value = "SELECT dbUserRole.user from UserRole dbUserRole where dbUserRole.userId = :userId and dbUserRole.organizationId = :organizationId and dbUserRole.role.privileges like %:privilege% AND "
            + CHECK_CAN_READ_USER_OR_OWNER_JPQL)
    User getUserByIdInOrganizationWithPrivilege(@Param("userId") Long userId,
                                                @Param("organizationId") Long organizationId,
                                                @Param("privilege") String privilege);

    /**
     * Retrieves User email by user ID without loading full entity.
     * <p>
     * Executes JPQL: {@code SELECT u.email FROM User u WHERE u.id = :id}
     * Returns String email projection only.

     *
     * @param id User ID to query email for, must not be null
     * @return User's email address, null if user not found
     */
    @Query("SELECT u.email FROM User u WHERE u.id = :id")
    String findUserEmailByUserId(Long id);
}
