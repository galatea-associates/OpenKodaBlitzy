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
 * JPA Criteria API Specification factory utilities for dynamic query construction across OpenKoda domain entities.
 * <p>
 * This package contains stateless, static-factory utility classes that produce Spring Data JPA 
 * {@link org.springframework.data.jpa.domain.Specification} predicates for various domain entities. 
 * Specifications centralize Criteria API predicate construction logic used across repository and service layers, 
 * promoting code reuse and type-safe query building. All specification factories return 
 * {@link org.springframework.data.jpa.domain.Specification} instances that can be composed via 
 * {@code and()}/{@code or()} operators for complex filtering scenarios.
 * </p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link FrontendResourceSpecifications} - Factory methods for FrontendResource and File entity queries 
 *       (name search, time-based filtering, resource type matching)</li>
 *   <li>{@link GlobalSearchSpecifications} - Privilege-aware cross-entity text search with authorization 
 *       enforcement via UserProvider context</li>
 *   <li>{@link NotificationSepcifications} - Notification visibility and read-status filtering with 
 *       subquery-based unread detection (note filename typo)</li>
 *   <li>{@link ServerJsSpecification} - Simple name-based ServerJs entity lookup</li>
 *   <li>{@link UserRoleSpecification} - UserRole filtering distinguishing organization-level vs 
 *       user-specific role assignments</li>
 *   <li>{@link UserSpecifications} - Complex authorization-aware User queries with privilege checks 
 *       and organization scoping</li>
 * </ul>
 *
 * <h2>Usage Patterns</h2>
 * <p>
 * Specifications are designed for composition with repository methods such as 
 * {@code JpaRepository.findAll(Specification)} and {@code JpaSpecificationExecutor.findOne(Specification)}. 
 * Compose multiple specifications using: {@code Specification.where(spec1).and(spec2).or(spec3)} 
 * for complex query logic.
 * </p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * Specification<User> spec = UserSpecifications.searchSpecification(orgId)
 *     .and(UserSpecifications.searchUserRoleSpecification(orgId));
 * List<User> users = userRepository.findAll(spec);
 * }</pre>
 *
 * <h2>Authorization Considerations</h2>
 * <p>
 * Several specifications (GlobalSearchSpecifications, UserSpecifications) enforce row-level security 
 * by consulting {@link com.openkoda.core.security.UserProvider#getFromContext()} to filter results 
 * based on current user privileges. Unauthenticated requests typically return {@code disjunction()} 
 * (always-false predicates).
 * </p>
 *
 * <h2>Technical Limitations</h2>
 * <p>
 * Most specification factories use string-based attribute names (e.g., 'name', 'userId', 'organizationId') 
 * via {@code root.get(String)}, making them fragile to entity refactoring. Consider migrating to the 
 * type-safe JPA metamodel ({@code EntityName_}) to eliminate string literals and enable compile-time validation.
 * </p>
 *
 * <h2>Recommended Maintenance Actions</h2>
 * <p>
 * Add unit and integration tests for authorization branches, time-sensitive predicates (searchNotOlderThan), 
 * and null/empty input handling. Remove unused imports (e.g., java.util.List in ServerJsSpecification). 
 * Replace null-returning placeholder methods (UserSpecifications.dict()) with proper implementations or 
 * remove from API.
 * </p>
 *
 * @see org.springframework.data.jpa.domain.Specification
 * @see jakarta.persistence.criteria.CriteriaBuilder
 * @see jakarta.persistence.criteria.Root
 * @see jakarta.persistence.criteria.Predicate
 * @see com.openkoda.core.security.UserProvider
 * @since 1.7.1
 * @version 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.repository.specifications;