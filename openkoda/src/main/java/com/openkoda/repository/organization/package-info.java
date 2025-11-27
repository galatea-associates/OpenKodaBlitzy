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

/**
 * Repositories for managing Organization entities in OpenKoda's multi-tenant architecture.
 * <p>
 * This package provides both secure and unsecured repository interfaces for tenant management,
 * organization hierarchy operations, and properties bag persistence. The repositories enforce
 * tenant boundaries through security-aware JPQL queries and privilege validation.
 * 
 *
 * <b>Key Concepts</b>
 *
 * <b>Tenant Isolation</b>
 * <p>
 * Repositories enforce tenant boundaries using security JPQL fragments that filter queries
 * based on user privileges and organization context. This ensures data isolation between
 * different tenants while allowing authorized cross-tenant operations where needed.
 * 
 *
 * <b>Organization Hierarchy</b>
 * <p>
 * Supports parent-child organization relationships for modeling enterprise structures.
 * Hierarchical queries enable operations across organization trees while maintaining
 * proper access control at each level.
 * 
 *
 * <b>Properties Bag Pattern</b>
 * <p>
 * Organizations use a JSONB properties column for flexible configuration storage. This
 * allows dynamic attributes without schema changes, supporting customization per tenant.
 * Repositories persist and retrieve these properties as Map structures.
 * 
 *
 * <b>Security Enforcement</b>
 * <p>
 * The package distinguishes between secure and unsecured operations. Secure repositories
 * apply privilege checks before data access, while unsecured variants support internal
 * system operations that bypass normal authorization rules.
 * 
 *
 * <b>Key Classes</b>
 * <ul>
 *   <li><b>OrganizationRepository</b> - Primary Spring Data JPA repository with security-aware
 *       queries for finding organizations by name, ID, or active status. Provides custom
 *       JPQL methods for hierarchical queries and bulk operations.</li>
 *   <li><b>SecureOrganizationRepository</b> - Secure marker interface implementing
 *       SearchableRepositoryMetadata for privilege-checked access. Used by application
 *       controllers and services requiring authorization enforcement.</li>
 * </ul>
 *
 * <b>Integration Points</b>
 * <ul>
 *   <li><b>OrganizationService</b> - Business logic layer using these repositories for tenant
 *       provisioning, organization lifecycle management, and schema operations.</li>
 *   <li><b>TenantResolver</b> - Uses organization data to resolve multi-tenancy context from
 *       request parameters, establishing the active organization for the current session.</li>
 *   <li><b>SearchableRepositoryMetadata</b> - Integrates with the indexing subsystem to enable
 *       full-text search across organization names and properties.</li>
 * </ul>
 *
 * <b>Usage Example</b>
 * <pre><code>
 * Organization org = organizationRepository.findByName("TenantName");
 * List&lt;Long&gt; activeIds = organizationRepository.findActiveOrganizationIdsAsList();
 * </code></pre>
 *
 * @author OpenKoda Team
 * @since 1.7.1
 */
package com.openkoda.repository.organization;