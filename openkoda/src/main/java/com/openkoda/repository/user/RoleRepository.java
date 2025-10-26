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
import com.openkoda.model.GlobalOrganizationRole;
import com.openkoda.model.Role;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Spring Data JPA repository managing Role entities with single-table inheritance hierarchy.
 * <p>
 * This interface extends {@link UnsecuredFunctionalRepositoryWithLongId} and implements {@link HasSecurityRules}
 * to provide repository operations for the Role entity hierarchy. Role uses single-table inheritance
 * with type discriminator to support GlobalRole, OrganizationRole, and GlobalOrganizationRole subtypes
 * stored in the same 'roles' table.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>Derived finder {@link #findByName(String)} for role lookup across all types</li>
 *   <li>Guarded bulk delete {@link #deleteRole(Long)} with removable flag and CHECK_CAN_MANAGE_ROLES_JPQL security</li>
 *   <li>JPQL query {@link #findAllGlobalRoles()} returning GlobalOrganizationRole instances</li>
 *   <li>Native SQL {@link #renamePrivilege(String, String)} for bulk privilege renaming via string replacement</li>
 *   <li>Privileges stored as joined string, transient privilegesSet via PrivilegeHelper serialization</li>
 * </ul>
 * </p>
 * <p>
 * Note: Native SQL operations bypass JPA lifecycle and rely on database string replacement semantics.
 * Privileges column format: '(PRIVILEGE_NAME_1)(PRIVILEGE_NAME_2)(PRIVILEGE_NAME_3)'.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see Role
 * @see com.openkoda.model.GlobalRole
 * @see com.openkoda.model.OrganizationRole
 * @see com.openkoda.model.GlobalOrganizationRole
 * @see UnsecuredFunctionalRepositoryWithLongId
 * @see HasSecurityRules
 */
@Repository
public interface RoleRepository extends UnsecuredFunctionalRepositoryWithLongId<Role>, HasSecurityRules {

    /**
     * Finds Role entity by unique role name across all role types.
     * <p>
     * Uses Spring Data query derivation to search single-table inheritance hierarchy.
     * Returns any Role subtype (GlobalRole, OrganizationRole, GlobalOrganizationRole) matching name.
     * Name comparison is case-sensitive.
     * </p>
     *
     * @param name Unique role name to search for, must not be null
     * @return Role (or subtype) with matching name, null if not found
     */
    Role findByName(String name);

    /**
     * Deletes a Role entity if removable and user has role management privileges.
     * <p>
     * This guarded bulk delete operation executes JPQL:
     * {@code DELETE FROM Role WHERE id = :id AND removable = true AND CHECK_CAN_MANAGE_ROLES_JPQL}
     * </p>
     * <p>
     * Security enforcement:
     * <ul>
     *   <li>Only deletes roles with {@code removable = true} flag (protects system roles)</li>
     *   <li>Enforces CHECK_CAN_MANAGE_ROLES_JPQL authorization check (requires role management privilege)</li>
     *   <li>Returns 0 if role not found, not removable, or user lacks privilege</li>
     *   <li>Annotated with @Modifying and @Transactional for write operation lifecycle</li>
     * </ul>
     * </p>
     * <p>
     * Note: Bulk delete bypasses JPA entity lifecycle callbacks (no @PreRemove execution).
     * UserRole associations should be handled separately via cascade or explicit deletion.
     * </p>
     *
     * @param aLong Role entity ID to delete, must not be null
     * @return Number of deleted rows (0 or 1)
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_MANAGE_ROLES privilege
     */
    @Modifying
    @Transactional
    @Query("delete from Role r where r.id = :id AND r.removable = true AND " + CHECK_CAN_MANAGE_ROLES_JPQL)
    int deleteRole(@Param("id") Long aLong);

    /**
     * Retrieves all GlobalOrganizationRole instances from the role hierarchy.
     * <p>
     * Executes JPQL query: {@code SELECT gor FROM GlobalOrganizationRole gor}
     * Filters single-table inheritance to return only GlobalOrganizationRole type discriminator rows.
     * </p>
     * <p>
     * Note: Despite method name "findAllGlobalRoles", this returns GlobalOrganizationRole subtype,
     * not GlobalRole. Used for cross-tenant role enumeration.
     * </p>
     *
     * @return List of all GlobalOrganizationRole entities, empty list if none exist
     */
    @Query("SELECT gor FROM GlobalOrganizationRole gor")
    List<GlobalOrganizationRole> findAllGlobalRoles();

    /**
     * Renames a privilege across all roles via native SQL string replacement in privileges column.
     * <p>
     * Executes native SQL:
     * {@code UPDATE roles SET privileges = replace(privileges, '(:oldName)', '(:newName)')}
     * </p>
     * <p>
     * Critical behavior notes:
     * <ul>
     *   <li>Bypasses JPA entity lifecycle - no @PreUpdate or dirty checking</li>
     *   <li>Relies on database REPLACE function for string substitution</li>
     *   <li>Privileges stored as joined string format: '(PRIV1)(PRIV2)(PRIV3)'</li>
     *   <li>Updates ALL roles containing the old privilege name</li>
     *   <li>Database-specific - uses PostgreSQL/MySQL REPLACE syntax</li>
     *   <li>No automatic cache invalidation - roles must be reloaded from database</li>
     * </ul>
     * </p>
     * <p>
     * Use case: System-wide privilege refactoring when renaming canonical privilege names.
     * Should be used sparingly and followed by cache flush.
     * </p>
     *
     * @param oldName Current privilege name to replace (e.g., 'OLD_PRIVILEGE'), must not be null
     * @param newName New privilege name to substitute (e.g., 'NEW_PRIVILEGE'), must not be null
     */
    @Modifying
    @Query(value = "UPDATE roles SET privileges = replace(\"privileges\", '(:oldName)', '(:newName)')"
            , nativeQuery = true)
    void renamePrivilege(@Param("oldName") String oldName, @Param("newName") String newName);

}
