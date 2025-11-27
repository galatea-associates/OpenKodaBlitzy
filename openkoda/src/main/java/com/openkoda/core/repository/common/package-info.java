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
 * Provides canonical Spring Data repository contracts and helper base interfaces used across the OpenKoda application.
 * This package defines the foundation for implementing searchable, scoped, and security-aware persistence layers
 * that enforce multi-tenant isolation and privilege-based access control. These interfaces are designed to be
 * extended by concrete repository interfaces throughout the application, providing consistent patterns for
 * data access, specification-based filtering, and organization-scoped operations.
 *
 * <b>Key Interfaces</b>
 * <ul>
 *   <li>{@link com.openkoda.core.repository.common.FunctionalRepository} - Marker interface for compile-time
 *       repository grouping and type-safe classification</li>
 *   <li>{@link com.openkoda.core.repository.common.FunctionalRepositoryWithLongId} - Base repository interface
 *       for Long-keyed entities, extending JpaRepository with convenience methods</li>
 *   <li>{@link com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId} - Unsecured repository
 *       base with Specification execution support and ergonomic adapters</li>
 *   <li>{@link com.openkoda.core.repository.common.ProfileSettingsRepository} - Specialized contract for decoupling
 *       UI settings forms from persistence logic</li>
 *   <li>{@link com.openkoda.core.repository.common.UnscopedSecureRepository} - Secure repository interface requiring
 *       explicit SecurityScope parameters for privilege enforcement</li>
 *   <li>{@link com.openkoda.core.repository.common.ScopedSecureRepository} - Organization-scoped repository interface
 *       with built-in multi-tenant isolation</li>
 *   <li>{@link com.openkoda.core.repository.common.SearchableFunctionalRepositoryWithLongId} - Feature-rich base
 *       providing searchable, filterable, and security-aware repository operations</li>
 * </ul>
 *
 * <b>Design Patterns</b>
 * <p>
 * This package implements several key design patterns:
 * 
 * <ul>
 *   <li><b>Abstract Repository Pattern</b>: Uses {@code @NoRepositoryBean} annotation to mark base interfaces
 *       that should not be instantiated as Spring Data repositories</li>
 *   <li><b>Privilege Enforcement</b>: Integrates SecurityScope and HasSecurityRules to enforce write-privilege
 *       checks before mutating operations</li>
 *   <li><b>Specification Composition</b>: Leverages JPA Criteria API via Specification for type-safe, composable
 *       query predicates</li>
 *   <li><b>Organization Scoping</b>: Provides multi-tenancy support through organization ID filtering in queries
 *       and security checks</li>
 * </ul>
 *
 * <b>Usage Example</b>
 * <pre>{@code
 * // Define a concrete repository by extending searchable base
 * public interface OrganizationRepository
 *         extends SearchableFunctionalRepositoryWithLongId<Organization> {
 *     // Inherits searchable, secure, filterable operations
 *     // Custom query methods can be added here
 * }
 * }</pre>
 *
 * <b>Relationships</b>
 * <p>
 * The interfaces in this package serve as base contracts extended by concrete repository interfaces in the
 * {@code com.openkoda.repository} package. They integrate with core framework components including
 * SecurityScope, UserProvider, TenantResolver, and SearchableRepositoryMetadata to provide comprehensive
 * data access capabilities with built-in security and multi-tenancy support.
 * 
 *
 * @see org.springframework.data.jpa.repository.JpaRepository
 * @see org.springframework.data.jpa.repository.JpaSpecificationExecutor
 * @see com.openkoda.core.repository.common.SearchableFunctionalRepositoryWithLongId
 * @see com.openkoda.core.repository.common.ScopedSecureRepository
 * @see com.openkoda.core.security.HasSecurityRules
 * @since 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.core.repository.common;