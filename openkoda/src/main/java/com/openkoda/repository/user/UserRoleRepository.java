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

import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.User;
import com.openkoda.model.UserRole;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Data JPA repository managing UserRole association entities linking users to roles with optional organization scope.
 * <p>
 * This interface extends {@link UnsecuredFunctionalRepositoryWithLongId} and implements {@link HasSecurityRules}
 * to provide repository operations for UserRole associations. UserRole is the join entity in the many-to-many
 * relationship between User and Role, with additional organizationId field for multi-tenant scoping.

 * <p>
 * Key features:
 * <ul>
 *   <li>Guarded bulk deletes with CHECK_CAN_MANAGE_USER_ROLES_JPQL security enforcement</li>
 *   <li>Organization-scoped queries returning UserRole lists or User ID/User entity sets</li>
 *   <li>Role-filtered queries (findAll*WithRoles methods accepting role name sets)</li>
 *   <li>Derived finders by userId, organizationId, roleId combinations</li>
 *   <li>Modification tracking: wasModifiedSince, getLastUpdatedOn for optimistic locking</li>
 *   <li>Distinction between user-specific roles (userId NOT NULL) and default organization roles (userId NULL)</li>
 * </ul>

 * <p>
 * Security: Most queries use CHECK_CAN_MANAGE_USER_ROLES_JPQL or CHECK_CAN_READ_ROLES_JPQL fragments
 * for privilege enforcement. SpEL expressions (?#{principal.organizationIds}) used for dynamic scope filtering.

 * <p>
 * Persists to 'user_role' table with columns: id (PK), user_id (FK, nullable for default org roles),
 * role_id (FK), organization_id (FK, nullable for global roles), created_on, updated_on.

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see UserRole
 * @see User
 * @see com.openkoda.model.Role
 * @see com.openkoda.model.Organization
 * @see UnsecuredFunctionalRepositoryWithLongId
 * @see HasSecurityRules
 */
@Repository
public interface UserRoleRepository extends UnsecuredFunctionalRepositoryWithLongId<UserRole>, HasSecurityRules {

//    @Override
//    @PreAuthorize(CHECK_CAN_SAVE_USER_ROLES)
//    UserRole save(@Param("userRole") UserRole userRole);

//    default UserRole unsecureSave(@Param("userRole") UserRole userRole) {
//        return saveAndFlush(userRole);
//    }

    /**
     * Deletes UserRole by ID if user has role management privileges.
     * <p>
     * Executes guarded JPQL: {@code DELETE FROM UserRole WHERE CHECK_CAN_MANAGE_USER_ROLES_JPQL AND id = :id}
     * Returns 0 if UserRole not found or user lacks privilege.

     *
     * @param aLong UserRole entity ID to delete, must not be null
     * @return Number of deleted rows (0 or 1)
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_MANAGE_USER_ROLES privilege
     */
    @Modifying
    @Transactional
    @Query("delete from UserRole dbUserRole where " + CHECK_CAN_MANAGE_USER_ROLES_JPQL + " AND dbUserRole.id = :id ")
    int deleteUserRole(@Param("id") Long aLong);

    /**
     * Deletes all UserRole associations for a removable Role if user has role management privileges.
     * <p>
     * Executes JPQL: {@code DELETE FROM UserRole WHERE CHECK_CAN_MANAGE_USER_ROLES_JPQL AND roleId IN (SELECT r.id FROM Role r WHERE r.id = :id AND r.removable = true)}
     * Only deletes if Role has removable=true flag (protects system roles). Returns 0 if role not removable or user lacks privilege.

     *
     * @param aLong Role entity ID whose UserRole associations should be deleted, must not be null
     * @return Number of deleted UserRole rows
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_MANAGE_USER_ROLES privilege
     */
    @Modifying
    @Transactional
    @Query("delete from UserRole dbUserRole where " + CHECK_CAN_MANAGE_USER_ROLES_JPQL + " AND dbUserRole.roleId IN (select r.id from Role r where r.id = :id AND r.removable = true) ")
    int deleteUserRoleByRoleId(@Param("id") Long aLong);

    /**
     * Deletes default organization roles (userId NULL) matching organization and role names if user has management privileges.
     * <p>
     * Executes JPQL: {@code DELETE FROM UserRole WHERE CHECK_CAN_MANAGE_USER_ROLES_JPQL AND userId IS NULL AND organizationId = :id AND roleId IN (SELECT r.id FROM Role WHERE r.name IN :role_names)}
     * Only deletes default organization roles (userId NULL), not user-specific assignments. Returns 0 if none match or user lacks privilege.

     *
     * @param aLong Organization ID to delete default roles in, must not be null
     * @param roleNames Set of role names to delete (e.g., {"ROLE_ORG_USER", "ROLE_ORG_ADMIN"}), must not be null or empty
     * @return Number of deleted default UserRole rows
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_MANAGE_USER_ROLES privilege
     */
    @Modifying
    @Transactional
    @Query("delete from UserRole dbUserRole where " + CHECK_CAN_MANAGE_USER_ROLES_JPQL + " AND dbUserRole.userId IS NULL AND dbUserRole.organizationId = :organization_id AND dbUserRole.roleId IN (select r.id from Role r where r.name IN :role_names) ")
    int deleteUserRoleByOrganizationIdAndRoleName(@Param("organization_id") Long aLong, @Param("role_names") Set<String> roleNames);

    /**
     * Deletes UserRole by exact role, user, and organization ID match.
     * <p>
     * Uses Spring Data derived delete method. No security fragment - assumes pre-authorized context.

     *
     * @param roleId Role ID, must be positive
     * @param userId User ID, must be positive
     * @param organizationId Organization ID, must be positive
     * @return Number of deleted rows (0 or 1)
     */
    @Modifying
    int deleteByRoleIdAndUserIdAndOrganizationId(long roleId, long userId, long organizationId);

    /**
     * Finds UserRole by exact role, user, and organization ID match.
     * <p>
     * Uses Spring Data derived finder. No security fragment - assumes pre-authorized context.

     *
     * @param roleId Role ID, must be positive
     * @param userId User ID, must be positive
     * @param organizationId Organization ID, must be positive
     * @return UserRole matching all three IDs, null if not found
     */
    UserRole findByRoleIdAndUserIdAndOrganizationId(long roleId, long userId, long organizationId);

    /**
     * Retrieves all UserRole associations in specified organization if user has management privileges.
     * <p>
     * Executes JPQL: {@code SELECT ur FROM UserRole ur WHERE CHECK_CAN_MANAGE_USER_ROLES_JPQL AND organizationId = :id}
     * Includes both user-specific (userId NOT NULL) and default organization roles (userId NULL).

     *
     * @param aLong Organization ID to query roles in, must not be null
     * @return List of UserRole entities in organization, empty list if none or user lacks privilege
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_MANAGE_USER_ROLES privilege
     */
    @Query("select dbUserRole from UserRole dbUserRole where " + CHECK_CAN_MANAGE_USER_ROLES_JPQL + " AND dbUserRole.organizationId = :organization_id")
    List<UserRole> findAllUserRolesInOrganization(@Param("organization_id") Long aLong);

    /**
     * Retrieves user-specific UserRole associations in organization filtered by role names.
     * <p>
     * Executes JPQL: {@code SELECT ur FROM UserRole ur WHERE CHECK_CAN_MANAGE_USER_ROLES_JPQL AND userId IS NOT NULL AND organizationId = :id AND role.name IN :role_names}
     * Only returns user-specific roles (userId NOT NULL), excludes default organization roles.

     *
     * @param aLong Organization ID to query, must not be null
     * @param roleNames Set of role names to filter by, must not be null or empty
     * @return List of user-specific UserRole entities matching filters, empty list if none found
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_MANAGE_USER_ROLES privilege
     */
    @Query("select dbUserRole from UserRole dbUserRole where " + CHECK_CAN_MANAGE_USER_ROLES_JPQL + " AND dbUserRole.userId IS NOT NULL AND dbUserRole.organizationId = :organization_id AND (dbUserRole.role.name IN :role_names)")
    List<UserRole> findAllUserRolesInOrganizationWithRoles(@Param("organization_id") Long aLong, @Param("role_names") Set<String> roleNames);

    /**
     * Retrieves all UserRole associations for user filtered by role names.
     * <p>
     * Executes JPQL: {@code SELECT ur FROM UserRole ur WHERE CHECK_CAN_MANAGE_USER_ROLES_JPQL AND userId = :user_id AND role.name IN :role_names}
     * Returns roles across all organizations and global roles (organizationId NULL or NOT NULL).

     *
     * @param aLong User ID to query roles for, must not be null
     * @param roleNames Set of role names to filter by, must not be null or empty
     * @return List of UserRole entities for user matching role names, empty list if none found
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_MANAGE_USER_ROLES privilege
     */
    @Query("select dbUserRole from UserRole dbUserRole where " + CHECK_CAN_MANAGE_USER_ROLES_JPQL + " AND dbUserRole.userId = :user_id AND (dbUserRole.role.name IN :role_names)")
    List<UserRole> findAllUserRolesForUserWithRoles(@Param("user_id") Long aLong, @Param("role_names") Set<String> roleNames);

    /**
     * Retrieves default organization roles (userId NULL) in organization filtered by role names.
     * <p>
     * Executes JPQL: {@code SELECT ur FROM UserRole ur WHERE CHECK_CAN_MANAGE_USER_ROLES_JPQL AND userId IS NULL AND organizationId = :id AND role.name IN :role_names}
     * Only returns default organization roles (userId NULL) that all organization members inherit.

     *
     * @param aLong Organization ID to query default roles in, must not be null
     * @param roleNames Set of role names to filter by, must not be null or empty
     * @return List of default UserRole entities matching filters, empty list if none found
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_MANAGE_USER_ROLES privilege
     */
    @Query("select dbUserRole from UserRole dbUserRole where " + CHECK_CAN_MANAGE_USER_ROLES_JPQL + " AND dbUserRole.userId IS NULL AND dbUserRole.organizationId = :organization_id AND (dbUserRole.role.name IN :role_names)")
    List<UserRole> findAllGlobalRolesInOrganizationWithRoles(@Param("organization_id") Long aLong, @Param("role_names") Set<String> roleNames);

    /**
     * Retrieves DISTINCT user IDs in organization if user has management privileges.
     * <p>
     * Executes JPQL: {@code SELECT DISTINCT userId FROM UserRole WHERE CHECK_CAN_MANAGE_USER_ROLES_JPQL AND organizationId = :id}
     * Returns Set to eliminate duplicates (users may have multiple roles in same organization).

     *
     * @param aLong Organization ID to query user IDs in, must not be null
     * @return Set of unique User IDs in organization, empty set if none or user lacks privilege
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_MANAGE_USER_ROLES privilege
     */
    @Query("select DISTINCT dbUserRole.userId from UserRole dbUserRole where " + CHECK_CAN_MANAGE_USER_ROLES_JPQL + " AND dbUserRole.organizationId = :organization_id")
    Set<Long> findAllUserIdsInOrganization(@Param("organization_id") Long aLong);

    /**
     * Retrieves DISTINCT user IDs in organization having specified role.
     * <p>
     * Executes JPQL: {@code SELECT DISTINCT userId FROM UserRole WHERE CHECK_CAN_MANAGE_USER_ROLES_JPQL AND organizationId = :id AND role.name = :role_name}
     * Filters to users with specific role. Uses DISTINCT to eliminate duplicates.

     *
     * @param aLong Organization ID to query, must not be null
     * @param roleName Role name to filter by (e.g., 'ROLE_ORG_ADMIN'), must not be null
     * @return Set of unique User IDs with specified role in organization, empty set if none found
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_MANAGE_USER_ROLES privilege
     */
    @Query("select DISTINCT dbUserRole.userId from UserRole dbUserRole where " + CHECK_CAN_MANAGE_USER_ROLES_JPQL + " AND dbUserRole.organizationId = :organization_id AND dbUserRole.role.name = :role_name")
    Set<Long> findAllUserIdsInOrganizationWithRole(@Param("organization_id") Long aLong, @Param("role_name") String roleName);

    /**
     * Retrieves DISTINCT User entities in organization if user has read roles privilege.
     * <p>
     * Executes JPQL: {@code SELECT DISTINCT ur.user FROM UserRole ur WHERE organizationId = :id AND CHECK_CAN_READ_ROLES_JPQL}
     * Returns Set of User entities (not just IDs). Enforces READ privilege (less restrictive than MANAGE).

     *
     * @param organizationId Organization ID to query users in, must not be null
     * @return Set of unique User entities in organization, empty set if none or user lacks privilege
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_READ_ROLES privilege
     */
    @Query(value = "SELECT DISTINCT dbUserRole.user from UserRole dbUserRole where dbUserRole.organizationId = :organizationId AND "
            + CHECK_CAN_READ_ROLES_JPQL)
    Set<User> getUsersInOrganization(@Param("organizationId") Long organizationId);

    /**
     * Retrieves DISTINCT User entities in organization having specified role.
     * <p>
     * Executes JPQL: {@code SELECT DISTINCT ur.user FROM UserRole ur WHERE organizationId = :id AND role.name = :role_name AND CHECK_CAN_READ_ROLES_JPQL}
     * Returns Set of User entities with specific role. Enforces READ privilege.

     *
     * @param organizationId Organization ID to query, must not be null
     * @param roleName Role name to filter by, must not be null
     * @return Set of unique User entities with role in organization, empty set if none found
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_READ_ROLES privilege
     */
    @Query(value = "SELECT DISTINCT dbUserRole.user from UserRole dbUserRole where dbUserRole.organizationId = :organizationId AND dbUserRole.role.name = :role_name AND "
            + CHECK_CAN_READ_ROLES_JPQL)
    Set<User> getUsersInOrganizationWithRole(@Param("organizationId") Long organizationId, @Param("role_name") String roleName);

    /**
     * Checks if any UserRole for user or in user's organizations was modified after specified timestamp.
     * <p>
     * Executes JPQL with SpEL: {@code SELECT count(*) > 0 FROM UserRole WHERE (userId = :id OR organizationId IN ?#{principal.organizationIds}) AND updatedOn > :since}
     * Uses SpEL expression for dynamic organization scope. Returns Boolean comparison wrapped in Optional.

     *
     * @param userId User ID to check modifications for, must not be null
     * @param since Timestamp to compare against, must not be null
     * @return Optional containing true if any related UserRole modified after since, false if not, empty if no UserRoles
     */
    @Query("SELECT count(*) > 0 from UserRole ur where (ur.userId = :id or ur.organizationId in ?#{principal.organizationIds}) and ur.updatedOn > :since")
    Optional<Boolean> wasModifiedSince(@Param("id") Long userId, @Param("since") LocalDateTime since);

    /**
     * Retrieves most recent updatedOn timestamp for UserRole entries related to user.
     * <p>
     * Executes JPQL with SpEL: {@code SELECT max(ur.updatedOn) FROM UserRole WHERE userId = :id OR organizationId IN ?#{principal.organizationIds}}
     * Uses SpEL for dynamic organization scope. Returns null if no related UserRoles.

     *
     * @param userId User ID to query latest modification for, must not be null
     * @return Most recent updatedOn timestamp, null if no related UserRoles exist
     */
    @Query("SELECT max(ur.updatedOn) from UserRole ur where (ur.userId = :id or ur.organizationId in ?#{principal.organizationIds})")
    LocalDateTime getLastUpdatedOn(@Param("id") Long userId);

    /**
     * Finds all UserRole entities matching exact user, organization, and role ID combination.
     * <p>
     * Uses Spring Data derived finder. May return multiple results if duplicates exist (database constraint should prevent).

     *
     * @param userId User ID, must not be null
     * @param organizationId Organization ID, must not be null
     * @param roleId Role ID, must not be null
     * @return List of matching UserRole entities (typically 0 or 1), empty list if not found
     */
    List<UserRole> findByUserIdAndOrganizationIdAndRoleId(Long userId, Long organizationId, Long roleId);

    /**
     * Checks if any UserRole in organization has role containing specified privilege.
     * <p>
     * Uses Spring Data derived exists query. Searches role.privileges string for privilege substring.
     * Privileges stored as joined string: '(PRIVILEGE1)(PRIVILEGE2)'.

     *
     * @param organizationId Organization ID to check, must not be null
     * @param privilege Privilege name to search for, must not be null
     * @return true if any UserRole in organization has role with privilege, false otherwise
     */
    Boolean existsByOrganizationIdAndRolePrivilegesContaining(Long organizationId, String privilege);

    /**
     * Finds default organization roles (userId NULL) for specified organization.
     * <p>
     * Uses Spring Data derived finder. Returns roles inherited by all organization members.

     *
     * @param organizationId Organization ID to query default roles for, must not be null
     * @return List of default UserRole entities (userId NULL), empty list if none configured
     */
    List<UserRole> findByOrganizationIdAndUserIdIsNull(Long organizationId);

    /**
     * Finds all UserRole associations for specific user in organization.
     * <p>
     * Uses Spring Data derived finder. Returns user-specific organization roles.

     *
     * @param organizationId Organization ID, must not be null
     * @param userId User ID, must not be null
     * @return List of UserRole entities for user in organization, empty list if none
     */
    List<UserRole> findByOrganizationIdAndUserId(Long organizationId, Long userId);
}
