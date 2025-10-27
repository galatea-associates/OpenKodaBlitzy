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

package com.openkoda.repository.organization;

import com.openkoda.model.Organization;
import com.openkoda.model.common.SearchableRepositoryMetadata;
import com.openkoda.repository.SecureRepository;
import org.springframework.stereotype.Repository;

import static com.openkoda.controller.common.URLConstants.*;
import static com.openkoda.model.common.ModelConstants.ID_PATH_FORMULA;

/**
 * Secure repository marker interface for Organization entities with tenant-aware privilege enforcement and search metadata configuration.
 * <p>
 * This interface extends {@link SecureRepository} to provide privilege-checked access to Organization entities
 * in multi-tenant operations. All repository methods inherited from the parent interface enforce read and write
 * privilege validation before database access, ensuring proper tenant isolation and access control.
 * </p>
 * <p>
 * <b>Security Model:</b><br>
 * All operations inherited from {@code SecureRepository} enforce privilege checks automatically. When any
 * repository method is invoked, the framework validates that the current user has the required privileges
 * before executing the database operation. This provides an additional security layer beyond the explicit
 * security JPQL queries used in {@link OrganizationRepository}.
 * </p>
 * <p>
 * <b>SearchableRepositoryMetadata Configuration:</b><br>
 * This interface is annotated with {@link SearchableRepositoryMetadata} to enable full-text search and
 * indexing capabilities. The configuration includes:
 * <ul>
 *   <li>{@code entityKey}: Set to {@code ORGANIZATION} constant for routing and URL generation</li>
 *   <li>{@code descriptionFormula}: SQL formula {@code "(''||name)"} generates organization descriptions</li>
 *   <li>{@code organizationRelatedPathFormula}: URL path construction using {@code _HTML + _ORGANIZATION + ID_PATH_FORMULA}</li>
 *   <li>{@code entityClass}: Bound to {@link Organization}.class for type binding</li>
 *   <li>{@code searchIndexFormula}: Full-text search indexing with pattern {@code "lower(name || ' orgid:' || COALESCE(CAST (id as text), ''))"} 
 *       for organization discovery by name or ID</li>
 * </ul>
 * </p>
 * <p>
 * <b>Tenant Isolation:</b><br>
 * Ensures access control on organization and tenant operations via the privilege system. Each operation
 * verifies that users can only access organizations they have privileges for, maintaining strict
 * multi-tenant boundaries.
 * </p>
 * <p>
 * <b>Usage Context:</b><br>
 * Used by {@code OrganizationService} and multi-tenant components requiring privilege-enforced organization
 * access. This interface differs from {@link OrganizationRepository} by applying automatic privilege checks
 * to all operations, while the unsecured repository requires explicit security JPQL in custom queries.
 * </p>
 * <p>
 * <b>Methods:</b><br>
 * This is a marker interface with no methods declared. All repository methods are inherited from
 * {@code SecureRepository<Organization>} with automatic privilege enforcement applied to standard
 * Spring Data JPA operations like {@code findById}, {@code findAll}, {@code save}, and {@code delete}.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see SecureRepository for privilege enforcement mechanics and inherited repository methods
 * @see OrganizationRepository for unsecured repository operations with explicit security JPQL
 * @see Organization for the tenant entity model with properties and branding fields
 * @see SearchableRepositoryMetadata for indexing configuration details and search capabilities
 */
@Repository
@SearchableRepositoryMetadata(
        entityKey = ORGANIZATION,
        descriptionFormula = "(''||name)",
        organizationRelatedPathFormula = "'" + _HTML + _ORGANIZATION + ID_PATH_FORMULA,
        entityClass = Organization.class,
        searchIndexFormula = "lower(name || ' orgid:' || COALESCE(CAST (id as text), '')) "
)
public interface SecureOrganizationRepository extends SecureRepository<Organization> {


}
