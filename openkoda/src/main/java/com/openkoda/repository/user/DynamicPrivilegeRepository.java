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
import com.openkoda.model.DynamicPrivilege;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JPA repository managing DynamicPrivilege entities for runtime-defined privilege extension.
 * <p>
 * This interface extends {@link UnsecuredFunctionalRepositoryWithLongId} and implements {@link HasSecurityRules}
 * to provide runtime privilege definitions beyond the canonical Privilege enum. It enables plugin-based
 * privilege systems where modules can register custom privileges at runtime without modifying core enum.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>Derived finder {@link #findByName(String)} for privilege lookup by unique name</li>
 *   <li>Guarded bulk delete {@link #deletePrivilege(Long)} with removable flag and CHECK_CAN_MANAGE_ROLES_JPQL security</li>
 *   <li>Used by privilege system for dynamic privilege resolution alongside Privilege enum</li>
 *   <li>Supports extensibility via {@link com.openkoda.core.customisation.CustomisationService} module registration</li>
 * </ul>
 * </p>
 * <p>
 * Persists to 'dynamic_privilege' table with columns: id (PK), name (unique), category, removable (boolean).
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see DynamicPrivilege
 * @see com.openkoda.model.Privilege
 * @see UnsecuredFunctionalRepositoryWithLongId
 * @see HasSecurityRules
 */
@Repository
public interface DynamicPrivilegeRepository extends UnsecuredFunctionalRepositoryWithLongId<DynamicPrivilege>, HasSecurityRules {

    /**
     * Finds DynamicPrivilege entity by unique privilege name.
     * <p>
     * Uses Spring Data query derivation to generate query: {@code SELECT * FROM dynamic_privilege WHERE name = ?}.
     * Name must match exactly (case-sensitive).
     * </p>
     *
     * @param name Unique privilege name to search for, must not be null
     * @return DynamicPrivilege with matching name, null if not found
     */
    DynamicPrivilege findByName(String name);

    /**
     * Deletes a DynamicPrivilege entity if removable and user has role management privileges.
     * <p>
     * This guarded bulk delete operation executes JPQL:
     * {@code DELETE FROM DynamicPrivilege WHERE id = :id AND removable = true AND CHECK_CAN_MANAGE_ROLES_JPQL}
     * </p>
     * <p>
     * Security enforcement:
     * <ul>
     *   <li>Only deletes privileges with {@code removable = true} flag</li>
     *   <li>Enforces CHECK_CAN_MANAGE_ROLES_JPQL authorization check (requires role management privilege)</li>
     *   <li>Returns 0 if privilege not found, not removable, or user lacks privilege</li>
     *   <li>Annotated with @Modifying and @Transactional for write operation lifecycle</li>
     * </ul>
     * </p>
     * <p>
     * Note: Bulk delete bypasses JPA entity lifecycle callbacks (no @PreRemove execution).
     * </p>
     *
     * @param aLong DynamicPrivilege entity ID to delete, must not be null
     * @return Number of deleted rows (0 or 1)
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_MANAGE_ROLES privilege
     */
    @Modifying
    @Transactional
    @Query("delete from DynamicPrivilege r where r.id = :id AND r.removable = true AND " + CHECK_CAN_MANAGE_ROLES_JPQL)
    int deletePrivilege(@Param("id") Long aLong);
}
