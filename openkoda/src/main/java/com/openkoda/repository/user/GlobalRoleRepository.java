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
import com.openkoda.core.repository.common.FunctionalRepositoryWithLongId;
import com.openkoda.model.GlobalRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository managing GlobalRole entities for system-wide role definitions.
 * <p>
 * This interface extends {@link FunctionalRepositoryWithLongId} to provide repository operations
 * for GlobalRole entities. GlobalRole represents system-wide roles that apply across all organizations
 * (e.g., ROLE_ADMIN, ROLE_SYSTEM_ADMIN) in the single-table Role inheritance hierarchy.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>Derived finder {@link #findByName(String)} for role lookup by unique name</li>
 *   <li>JPQL projection {@link #findAllAsTupleWithLabelName()} returning Tuple(name, label) for UI lists</li>
 *   <li>Filters out '%UNAUTHENTICATED%' roles in tuple projection for user-facing displays</li>
 *   <li>Override {@link #save(GlobalRole)} for explicit type safety in role persistence</li>
 *   <li>Used by system initialization, role management, and global privilege assignment</li>
 * </ul>
 * </p>
 * <p>
 * Persists to 'roles' table (single-table inheritance) with type discriminator 'GlobalRole'.
 * Shares table with {@link com.openkoda.model.OrganizationRole} and {@link com.openkoda.model.GlobalOrganizationRole}.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see GlobalRole
 * @see com.openkoda.model.OrganizationRole
 * @see com.openkoda.model.GlobalOrganizationRole
 * @see FunctionalRepositoryWithLongId
 */
@Repository
public interface GlobalRoleRepository extends FunctionalRepositoryWithLongId<GlobalRole> {

    /**
     * Finds GlobalRole by unique role name.
     * <p>
     * Uses Spring Data query derivation to generate query with single-table inheritance filtering:
     * {@code SELECT * FROM roles WHERE name = ? AND dtype = 'GlobalRole'}
     * Name comparison is case-sensitive.
     * </p>
     *
     * @param name Unique global role name to search for (e.g., 'ROLE_ADMIN'), must not be null
     * @return GlobalRole with matching name, null if not found
     */
    GlobalRole findByName(String name);

    /**
     * Saves GlobalRole entity with explicit type safety.
     * <p>
     * Overrides generic save method from {@link FunctionalRepositoryWithLongId} to provide
     * typed return signature. Delegates to standard JPA persist/merge operations.
     * </p>
     *
     * @param globalRole GlobalRole entity to save (insert or update), must not be null
     * @return Saved GlobalRole with generated ID if new, or merged instance if existing
     */
    @Override
    GlobalRole save(GlobalRole globalRole);

    /**
     * Retrieves all user-assignable GlobalRole entities as Tuple projections with localized label keys.
     * <p>
     * Executes JPQL constructor expression:
     * {@code SELECT new Tuple(name, 'label.globalRole.' || name) FROM GlobalRole 
     *        WHERE name NOT LIKE '%UNAUTHENTICATED%' ORDER BY name}
     * </p>
     * <p>
     * Returns Tuple instances with role name and i18n label key (e.g., 'label.globalRole.ROLE_ADMIN').
     * Filters out internal UNAUTHENTICATED role not meant for user assignment. Ordered alphabetically
     * by name for consistent UI display.
     * </p>
     * <p>
     * Usage: Populate role dropdowns in user management UI with localized labels.
     * Label key should resolve via MessageSource to display name.
     * </p>
     *
     * @return List of Tuple(name, labelKey) for assignable GlobalRoles, empty list if none exist
     */
    @Query("select new com.openkoda.core.flow.Tuple(dbGlobalRole.name, 'label.globalRole.' || dbGlobalRole.name) FROM GlobalRole dbGlobalRole " +
            "where dbGlobalRole.name not like '%UNAUTHENTICATED%' order by dbGlobalRole.name")
    List<Tuple> findAllAsTupleWithLabelName();
}
