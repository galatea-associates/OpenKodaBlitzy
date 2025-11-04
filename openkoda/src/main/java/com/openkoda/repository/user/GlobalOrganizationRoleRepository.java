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
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.GlobalOrganizationRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository managing GlobalOrganizationRole entities for cross-tenant role definitions.
 * <p>
 * This interface extends {@link FunctionalRepositoryWithLongId} and implements {@link HasSecurityRules}
 * to provide repository operations for GlobalOrganizationRole entities. GlobalOrganizationRole represents
 * roles that can be assigned across multiple organizations (tenants), combining aspects of both global
 * and organization-scoped roles in the single-table Role inheritance hierarchy.

 * <p>
 * Key features:
 * <ul>
 *   <li>Derived finder {@link #findByName(String)} for role lookup by unique name</li>
 *   <li>JPQL projection {@link #findAllAsTuple()} returning lightweight Tuple(name, name) for UI dropdowns</li>
 *   <li>Override {@link #save(GlobalOrganizationRole)} for explicit type safety in role persistence</li>
 *   <li>Used by role management UI and cross-tenant user assignment workflows</li>
 * </ul>

 * <p>
 * Persists to 'roles' table (single-table inheritance) with type discriminator 'GlobalOrganizationRole'.
 * Shares table with {@link com.openkoda.model.GlobalRole} and {@link com.openkoda.model.OrganizationRole}.

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see GlobalOrganizationRole
 * @see com.openkoda.model.GlobalRole
 * @see com.openkoda.model.OrganizationRole
 * @see FunctionalRepositoryWithLongId
 * @see HasSecurityRules
 */
@Repository
public interface GlobalOrganizationRoleRepository extends FunctionalRepositoryWithLongId<GlobalOrganizationRole>, HasSecurityRules {


    /**
     * Saves GlobalOrganizationRole entity with explicit type safety.
     * <p>
     * Overrides generic save method from {@link FunctionalRepositoryWithLongId} to provide
     * typed return signature. Delegates to standard JPA persist/merge operations.

     *
     * @param organizationRole GlobalOrganizationRole entity to save (insert or update), must not be null
     * @return Saved GlobalOrganizationRole with generated ID if new, or merged instance if existing
     */
    @Override
    GlobalOrganizationRole save(GlobalOrganizationRole organizationRole);

    /**
     * Finds GlobalOrganizationRole by unique role name.
     * <p>
     * Uses Spring Data query derivation to generate query with single-table inheritance filtering.
     * Name must match exactly (case-sensitive).

     *
     * @param name Unique role name to search for, must not be null
     * @return GlobalOrganizationRole with matching name, null if not found
     */
    GlobalOrganizationRole findByName(String name);

    /**
     * Retrieves all GlobalOrganizationRole entities as lightweight Tuple projections for UI rendering.
     * <p>
     * Executes JPQL constructor expression:
     * {@code SELECT new Tuple(name, name) FROM GlobalOrganizationRole ORDER BY name}

     * <p>
     * Returns Tuple instances with both components set to role name (id and label same).
     * Ordered alphabetically by name for consistent UI display. Requires {@link Tuple} constructor
     * accepting two String arguments.

     * <p>
     * Usage: Populate dropdown lists and autocomplete fields without loading full Role entities.

     *
     * @return List of Tuple(name, name) for all GlobalOrganizationRoles, empty list if none exist
     */
    @Query("select new com.openkoda.core.flow.Tuple(dbGlobalOrganizationRole.name, dbGlobalOrganizationRole.name) FROM GlobalOrganizationRole dbGlobalOrganizationRole order by name")
    List<Tuple> findAllAsTuple();


}
