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
import com.openkoda.model.OrganizationRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository managing OrganizationRole entities for tenant-scoped role definitions.
 * <p>
 * This interface extends {@link FunctionalRepositoryWithLongId} and implements {@link HasSecurityRules}
 * to provide repository operations for OrganizationRole entities. OrganizationRole represents
 * tenant-scoped roles that apply only within specific organizations (e.g., ROLE_ORG_ADMIN,
 * ROLE_ORG_USER) in the single-table Role inheritance hierarchy.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>Derived finder {@link #findByName(String)} for role lookup by unique name</li>
 *   <li>JPQL projection {@link #findAllAsTupleWithLabelName()} returning Tuple(name, label) for UI lists</li>
 *   <li>Override {@link #save(OrganizationRole)} for explicit type safety in role persistence</li>
 *   <li>Used by organization management, tenant-specific user assignment, and role administration</li>
 * </ul>
 * </p>
 * <p>
 * Persists to 'roles' table (single-table inheritance) with type discriminator 'OrganizationRole'.
 * Shares table with {@link com.openkoda.model.GlobalRole} and {@link com.openkoda.model.GlobalOrganizationRole}.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see OrganizationRole
 * @see com.openkoda.model.GlobalRole
 * @see com.openkoda.model.GlobalOrganizationRole
 * @see FunctionalRepositoryWithLongId
 * @see HasSecurityRules
 */
@Repository
public interface OrganizationRoleRepository extends FunctionalRepositoryWithLongId<OrganizationRole>, HasSecurityRules {


    /**
     * Saves OrganizationRole entity with explicit type safety.
     * <p>
     * Overrides generic save method from {@link FunctionalRepositoryWithLongId} to provide
     * typed return signature. Delegates to standard JPA persist/merge operations.
     * </p>
     *
     * @param organizationRole OrganizationRole entity to save (insert or update), must not be null
     * @return Saved OrganizationRole with generated ID if new, or merged instance if existing
     */
    @Override
    OrganizationRole save(OrganizationRole organizationRole);

    /**
     * Finds OrganizationRole by unique role name.
     * <p>
     * Uses Spring Data query derivation to generate query with single-table inheritance filtering:
     * {@code SELECT * FROM roles WHERE name = ? AND dtype = 'OrganizationRole'}
     * Name comparison is case-sensitive.
     * </p>
     *
     * @param name Unique organization role name to search for (e.g., 'ROLE_ORG_ADMIN'), must not be null
     * @return OrganizationRole with matching name, null if not found
     */
    OrganizationRole findByName(String name);

    /**
     * Retrieves all OrganizationRole entities as Tuple projections with localized label keys.
     * <p>
     * Executes JPQL constructor expression:
     * {@code SELECT new Tuple(name, 'label.role.' || name) FROM OrganizationRole ORDER BY name}
     * </p>
     * <p>
     * Returns Tuple instances with role name and i18n label key (e.g., 'label.role.ROLE_ORG_ADMIN').
     * Ordered alphabetically by name for consistent UI display.
     * </p>
     * <p>
     * Usage: Populate role dropdowns in organization user management UI with localized labels.
     * Label key should resolve via MessageSource to display name.
     * </p>
     *
     * @return List of Tuple(name, labelKey) for all OrganizationRoles, empty list if none exist
     */
    @Query("select new com.openkoda.core.flow.Tuple(dbOrganizationRole.name, 'label.role.' || dbOrganizationRole.name) FROM OrganizationRole dbOrganizationRole order by name")
    List<Tuple> findAllAsTupleWithLabelName();


}
