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

package com.openkoda.core.repository.common;

import com.openkoda.core.form.AbstractEntityForm;
import com.openkoda.core.form.FrontendMappingFieldDefinition;
import com.openkoda.core.multitenancy.TenantResolver;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.core.security.OrganizationUser;
import com.openkoda.core.security.UserProvider;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.common.*;
import com.openkoda.repository.SearchableRepositories;
import jakarta.persistence.criteria.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.function.Tuple3;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Feature-rich base repository interface for searchable entities with Long primary keys.
 * <p>
 * This {@code @NoRepositoryBean} interface provides comprehensive default implementations for building 
 * JPA Criteria API {@link Specification} objects that support various search and filter scenarios.
 * It is designed to simplify repository development by combining functional programming patterns,
 * security-aware data access, and multi-tenancy support into a single cohesive interface.
 * </p>
 * <p>
 * Most entities in OpenKoda use Long IDs and repositories are frequently used in chained lambda expressions.
 * This interface assumes both functional and searchable repository patterns with Long ID entities,
 * eliminating boilerplate code in concrete repository implementations.
 * </p>
 * 
 * <h3>Key Capabilities</h3>
 * <ul>
 * <li><b>Free-Text Search</b>: Builds LIKE clauses on {@code indexString} field for full-text matching</li>
 * <li><b>Frontend Filter Translation</b>: Converts {@link Tuple3} filter definitions into typed predicates 
 *     supporting text, number, date, dropdown, and boolean filters</li>
 * <li><b>Organization Constraints</b>: Composes specifications with organization ID constraints for multi-tenancy</li>
 * <li><b>Security Integration</b>: Wraps specifications with security checks using {@link #secureSpecification}
 *     and {@link #toSecurePredicate}</li>
 * <li><b>Write Privilege Enforcement</b>: Validates write operations via {@link #hasWritePrivilegeForEntity}
 *     using {@link HasSecurityRules}, {@link EntityWithRequiredPrivilege}, {@link OrganizationRelatedEntity},
 *     and {@link UserProvider}/{@link OrganizationUser} principal</li>
 * <li><b>Transactional Mutations</b>: Delete operations are {@code @Transactional} for consistency</li>
 * <li><b>Utility Helpers</b>: Provides {@link #extractEntityId}, {@link #toIdList}, and {@link #getNew}
 *     (uses {@link SearchableRepositoryMetadata} + {@link TenantResolver} + reflection)</li>
 * <li><b>Pagination Support</b>: Creates {@link PageRequest} with {@code Sort.Direction} and sort field</li>
 * </ul>
 * 
 * <h3>Security Model</h3>
 * <p>
 * All query methods use {@link #secureSpecification} to enforce access control. Mutating methods
 * ({@link #saveOne}, {@link #saveAll}, {@link #saveForm}) enforce write privilege checks and throw
 * {@link AccessDeniedException} on privilege denial.
 * </p>
 * 
 * <h3>Multi-Tenancy Support</h3>
 * <p>
 * Organization-scoped operations automatically compose specifications with {@link #organizationIdSpecification}
 * to isolate data by tenant. The {@link #getNew} method uses {@link TenantResolver} to initialize
 * new entities with the current tenant's organization ID.
 * </p>
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Build secure specification with organization constraint
 * Specification<User> spec = secureSpecification(
 *     searchSpecification("john").and(fieldSpecification("active", true)),
 *     null,
 *     organizationId
 * );
 * Page<User> results = findAll(spec, pageable);
 * }</pre>
 * 
 * <p>
 * <b>Important</b>: Changes to this interface are highly breaking as concrete repositories inherit
 * all security-aware behavior. All methods should maintain {@code secureSpecification} usage.
 * </p>
 *
 * @param <T> the searchable entity type extending {@link SearchableEntity}
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Specification
 * @see UserProvider
 * @see TenantResolver
 * @see SearchableRepositoryMetadata
 * @see HasSecurityRules
 * @see EntityWithRequiredPrivilege
 * @see FunctionalRepositoryWithLongId
 * @see UnscopedSecureRepository
 * @see ScopedSecureRepository
 */
@NoRepositoryBean
public interface SearchableFunctionalRepositoryWithLongId<T extends SearchableEntity> extends FunctionalRepositoryWithLongId<T>, UnscopedSecureRepository<T>, ScopedSecureRepository<T>, JpaSpecificationExecutor<T>, ModelConstants, HasSecurityRules, LoggingComponentWithRequestId {

    /**
     * Checks whether the current user has write privilege for the given entity.
     * <p>
     * This method enforces privilege-based access control for entity mutations. It evaluates whether
     * the entity requires privileges (via {@link EntityWithRequiredPrivilege#getRequiredWritePrivilege}),
     * retrieves the current user from {@link UserProvider#getFromContext}, and checks for global or
     * organization-specific privileges.
     * </p>
     * <p>
     * Privilege evaluation logic:
     * </p>
     * <ul>
     * <li>If entity class does not implement {@link EntityWithRequiredPrivilege}, returns {@code true}</li>
     * <li>If {@code getRequiredWritePrivilege()} returns {@code null}, returns {@code true}</li>
     * <li>If user has global privilege, returns {@code true}</li>
     * <li>If entity is {@link OrganizationRelatedEntity} and user has organization privilege, returns {@code true}</li>
     * <li>Otherwise, returns {@code false}</li>
     * </ul>
     *
     * @param <S> the entity type extending {@code T}
     * @param scope the security scope for privilege evaluation (not currently used in implementation)
     * @param s the entity to check write privileges for
     * @return {@code true} if write operation is allowed, {@code false} otherwise
     * @see EntityWithRequiredPrivilege
     * @see OrganizationRelatedEntity
     * @see UserProvider
     * @see OrganizationUser#hasGlobalPrivilege
     * @see OrganizationUser#hasOrgPrivilege
     */
    default <S extends T> boolean hasWritePrivilegeForEntity(SecurityScope scope, S s){
        if(requiresPrivilege(s.getClass())){
            String requiredPrivilege = ((EntityWithRequiredPrivilege) s).getRequiredWritePrivilege();
            if (requiredPrivilege == null) {
                return true;
            }
            Optional<OrganizationUser> userOptional = UserProvider.getFromContext();
            if(userOptional.isPresent()){
                OrganizationUser user = userOptional.get();
                if(user.hasGlobalPrivilege(requiredPrivilege)){
                    return true;
                } else if(isOrganizationRelated(s.getClass())){
                    Long organizationId = ((OrganizationRelatedEntity) s).getOrganizationId();
                    return user.hasOrgPrivilege(requiredPrivilege, organizationId);
                }
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * Constructs search specification using {@code LIKE} clauses on the {@code indexString} field.
     * <p>
     * This method delegates to {@link #searchSpecificationFactory} to build a {@link Specification}
     * that performs case-insensitive partial matching on the {@code indexString} field for each
     * search term provided. All search terms are combined with AND logic.
     * </p>
     * <p>
     * Example: {@code ["John", "Smith"]} produces:
     * {@code (indexString LIKE '%john%') AND (indexString LIKE '%smith%')}
     * </p>
     *
     * @param searchTerm array of search terms to match (case-insensitive, partial match)
     * @return a {@link Specification} combining all search terms with AND, or a conjunction if empty
     * @see #searchSpecificationFactory
     */
    default Specification<T> searchSpecification(String ... searchTerm) {
        return searchSpecificationFactory(searchTerm);
    }

    /**
     * Factory method that constructs a JPA Criteria API specification for free-text search.
     * <p>
     * This static factory builds a {@link Specification} that applies case-insensitive {@code LIKE}
     * clauses on the {@code indexString} field for each search term. Empty or {@code null} terms
     * are converted to empty strings. All predicates are combined with AND logic.
     * </p>
     * <p>
     * Search behavior:
     * </p>
     * <ul>
     * <li>Empty array: returns conjunction (matches all records)</li>
     * <li>Non-empty array: each term generates {@code LOWER(indexString) LIKE '%term%'}</li>
     * <li>Terms are lowercased and wrapped with wildcard {@code %} for partial matching</li>
     * </ul>
     *
     * @param searchTerm array of search terms (supports {@code null} elements)
     * @return a {@link Specification} with combined search predicates, or conjunction if empty
     */
    static Specification searchSpecificationFactory(String ... searchTerm) {
        if (ArrayUtils.isEmpty(searchTerm)) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
             Predicate[] searchPredicates = new Predicate[searchTerm.length];
             for (int i = 0; i < searchTerm.length; i++) {
                 searchPredicates[i] = cb.like(cb.lower(root.get("indexString")), "%" + StringUtils.lowerCase(StringUtils.defaultString(searchTerm[i], "")) + "%");
             }
             return cb.and(searchPredicates);
        };
    }

    /**
     * Translates frontend filter definitions into JPA Criteria API predicates.
     * <p>
     * This method converts {@link Tuple3} filter tuples (field name, field definition, filter value)
     * into typed {@link Predicate} objects based on the field type. Supported field types include
     * text, number, date, dropdown, and boolean filters.
     * </p>
     * <p>
     * Filter type mappings:
     * </p>
     * <ul>
     * <li><b>text/textarea</b>: Case-insensitive {@code LIKE '%value%'} on field</li>
     * <li><b>number</b>: Exact equality comparison using {@link BigDecimal}</li>
     * <li><b>dropdown/many_to_one</b>: String equality comparison</li>
     * <li><b>date/datetime</b>: Date range comparison ({@code _to} suffix = less than or equal,
     *     otherwise greater than or equal) using {@code yyyy-MM-dd} format</li>
     * <li><b>boolean</b>: Boolean equality comparison</li>
     * </ul>
     *
     * @param filters list of filter tuples containing (field name, field definition, filter value)
     * @return a {@link Specification} with all filters combined using AND logic, or conjunction if empty
     * @see FrontendMappingFieldDefinition
     * @see Tuple3
     */
    default Specification<T> filterSpecification(List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters) {
        if (filters.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            Predicate[] searchPredicates = new Predicate[filters.size()];
            int i = 0;
            for (Tuple3<String, FrontendMappingFieldDefinition, String> filter : filters) {
                switch (filter.getT2().getType()) {
                    case text, textarea:
                        searchPredicates[i] = cb.like(cb.lower(root.get(filter.getT1())), "%" + StringUtils.lowerCase(filter.getT3()) + "%");
                        break;
                    case number:
                        searchPredicates[i] = cb.equal(root.get(filter.getT1()), new BigDecimal(filter.getT3()));
                        break;
                    case dropdown, many_to_one:
                        searchPredicates[i] = cb.equal(root.get(filter.getT1()), filter.getT3());
                        break;
                    case date, datetime:
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        searchPredicates[i] = filter.getT1().endsWith("_to") ? cb.lessThanOrEqualTo(root.get(filter.getT2().getName()), LocalDate.parse(filter.getT3(), formatter))
                                : cb.greaterThanOrEqualTo(root.get(filter.getT2().getName()), LocalDate.parse(filter.getT3(), formatter));
                        break;
                    default:
                        switch (filter.getT2().getType().getDbType()) {
                            case BOOLEAN:
                                searchPredicates[i] = cb.equal(root.get(filter.getT1()), Boolean.valueOf(filter.getT3()));
                                break;
                        }
                }
                i++;
            }
            return cb.and(searchPredicates);
        };
    }

    /**
     * Constructs a specification that filters entities by organization ID for multi-tenancy.
     * <p>
     * This method builds a {@link Specification} that constrains results to entities belonging
     * to the specified organization. If {@code organizationId} is {@code null}, returns a
     * conjunction (matches all records).
     * </p>
     * <p>
     * This specification is fundamental for multi-tenant data isolation, ensuring users only
     * access data within their organization scope.
     * </p>
     *
     * @param organizationId the organization ID to filter by, or {@code null} to match all
     * @return a {@link Specification} constraining results to the specified organization
     * @see OrganizationRelatedEntity
     */
    default Specification<T> organizationIdSpecification(Long organizationId) {
        return (root, query, cb) -> organizationId == null ? cb.conjunction() : cb.equal(root.get("organizationId"), organizationId);
    }
    
    /**
     * Constructs a specification that filters entities by multiple organization IDs.
     * <p>
     * This method builds a {@link Specification} that constrains results to entities belonging
     * to any of the specified organizations using an {@code IN} clause. If {@code organizationIds}
     * is {@code null} or empty, returns a conjunction (matches all records).
     * </p>
     *
     * @param organizationIds set of organization IDs to filter by, or {@code null}/{@code empty} to match all
     * @return a {@link Specification} constraining results to the specified organizations
     */
    default Specification<T> organizationIdSpecification(Set<Long> organizationIds) {
        return (root, query, cb) -> (organizationIds == null || organizationIds.isEmpty()) ? cb.conjunction() : cb.in(root.get("organizationId")).value(organizationIds);
    }

    /**
     * Constructs a specification that matches entities by their primary key ID.
     * <p>
     * This method builds a {@link Specification} that filters for a single entity with the
     * specified Long ID using exact equality comparison.
     * </p>
     *
     * @param id the entity ID to match
     * @return a {@link Specification} matching the entity with the specified ID
     */
    default Specification<T> idSpecification(Long id) {
        return (root, query, cb) -> cb.equal(root.get("id"), id);
    }

    /**
     * Constructs a specification that matches entities by a collection of IDs.
     * <p>
     * This method builds a {@link Specification} using an {@code IN} clause to match entities
     * with IDs in the provided collection. If the collection is {@code null} or empty, returns
     * a disjunction (matches no records).
     * </p>
     *
     * @param ids collection of entity IDs to match, or {@code null}/{@code empty} for no matches
     * @return a {@link Specification} matching entities with IDs in the collection
     */
    default Specification<T> idsSpecification(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return (root, query, cb) -> cb.disjunction();
        }
        return (root, query, cb) -> root.get("id").in(ids);
    }

    /**
     * Constructs a specification that matches entities by a specific field value.
     * <p>
     * This method builds a {@link Specification} that filters entities where the specified
     * field equals the provided value. String values are automatically converted using
     * {@code toString()} for comparison.
     * </p>
     *
     * @param fieldName the entity field name to filter on
     * @param value the value to match (supports String conversion for non-String values)
     * @return a {@link Specification} matching entities where field equals value
     */
    default Specification<T> fieldSpecification(String fieldName, Object value) {
        return (root, query, cb) -> {
            Expression<String> rr = (value instanceof String) ? cb.toString(root.get(fieldName)) : root.get(fieldName);
            return cb.equal(rr, value);
        };
    }

    /**
     * Wraps a specification with security checks to enforce privilege-based access control.
     * <p>
     * This method creates a security-aware {@link Specification} that delegates to
     * {@link HasSecurityRules#toSecurePredicate} to apply privilege constraints. All repository
     * queries should use this method to ensure data access complies with the security model.
     * </p>
     * <p>
     * The security predicate evaluates user privileges and organizational scope to determine
     * which records the current user can access.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param specification the base specification to wrap, or {@code null} for no additional filtering
     * @param requiredPrivilege the privilege required to access results, or {@code null} for no privilege check
     * @return a {@link Specification} with security constraints applied
     * @see HasSecurityRules#toSecurePredicate
     * @see UserProvider
     * @see OrganizationUser
     */
    default Specification<T> secureSpecification(SecurityScope scope, Specification<T> specification, Enum requiredPrivilege) {

        return new Specification<T>() {
            @Override
            public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                return toSecurePredicate(specification, requiredPrivilege, root, query, cb, scope);
            }
        };

    }

    /**
     * Wraps a specification with security checks and organization constraints.
     * <p>
     * This method combines the base specification with {@link #organizationIdSpecification}
     * to enforce multi-tenant data isolation, then wraps the result with security checks.
     * It ensures users only access records within their authorized organization scope.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param specification the base specification to wrap, or {@code null} for no additional filtering
     * @param requiredPrivilege the privilege required to access results, or {@code null} for no privilege check
     * @param organizationId the organization ID to constrain results to, or {@code null} for no constraint
     * @return a {@link Specification} with security and organization constraints applied
     * @see #organizationIdSpecification(Long)
     */
    default Specification<T> secureSpecification(SecurityScope scope, Specification<T> specification, Enum requiredPrivilege, Long organizationId){
        return secureSpecification(scope, Specification.where(specification).and(organizationIdSpecification(organizationId)), requiredPrivilege);
    }
    
    /**
     * Wraps a specification with security checks and multiple organization constraints.
     * <p>
     * This method combines the base specification with {@link #organizationIdSpecification(Set)}
     * to enforce multi-tenant data isolation across multiple organizations, then wraps the result
     * with security checks.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param specification the base specification to wrap, or {@code null} for no additional filtering
     * @param requiredPrivilege the privilege required to access results, or {@code null} for no privilege check
     * @param organizationIds set of organization IDs to constrain results to
     * @return a {@link Specification} with security and organization constraints applied
     */
    default Specification<T> secureSpecification(SecurityScope scope, Specification<T> specification, Enum requiredPrivilege, Set<Long> organizationIds){
        return secureSpecification(scope, Specification.where(specification).and(organizationIdSpecification(organizationIds    )), requiredPrivilege);
    }

    /**
     * Searches for entities matching a specific field value with security constraints.
     * <p>
     * This method builds a {@link #fieldSpecification} for the provided field name and value,
     * wraps it with {@link #secureSpecification}, and returns all matching entities.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param fieldName the entity field name to filter on
     * @param value the value to match
     * @return list of entities matching the field criteria within authorized scope
     */
    @Override
    default List<T> search(SecurityScope scope, String fieldName, Object value) {
        Specification<T> s = fieldSpecification(fieldName, value);
        return this.findAll(secureSpecification(scope, s, null));
    }

    /**
     * Searches for entities using a custom specification with security constraints.
     * <p>
     * This method wraps the provided specification with {@link #secureSpecification} and
     * returns all matching entities.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param specification the search specification to apply
     * @return list of entities matching the specification within authorized scope
     */
    @Override
    default List<T> search(SecurityScope scope, Specification<T> specification) {
        return this.findAll(secureSpecification(scope, specification, null));
    }

    /**
     * Searches for entities within a specific organization with security constraints.
     * <p>
     * This method constrains results to entities belonging to the specified organization
     * and applies security checks via {@link #secureSpecification}.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param organizationId the organization ID to filter by
     * @return list of entities within the specified organization and authorized scope
     */
    @Override
    default List<T> search(SecurityScope scope, Long organizationId) {
        return this.findAll(secureSpecification(scope, null, null, organizationId));
    }

    /**
     * Searches for entities using a custom specification within a specific organization.
     * <p>
     * This method combines the provided specification with organization constraints and
     * applies security checks via {@link #secureSpecification}.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param organizationId the organization ID to filter by
     * @param specification the search specification to apply
     * @return list of entities matching criteria within the specified organization
     */
    @Override
    default List<T> search(SecurityScope scope, Long organizationId, Specification<T> specification) {
        return this.findAll(secureSpecification(scope, specification, null, organizationId));
    }

    /**
     * Searches for entities with pagination and sorting.
     * <p>
     * This method returns a paginated result set with the specified page size, sorting field,
     * and sort direction. Security constraints are applied via {@link #secureSpecification}.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param page the page number (zero-based)
     * @param size the number of records per page
     * @param sortField the entity field name to sort by
     * @param sortDirection the sort direction ("ASC" or "DESC")
     * @return a {@link Page} of entities within authorized scope
     */
    @Override
    default Page<T> search(SecurityScope scope, int page, int size, String sortField, String sortDirection) {
        return this.findAll(secureSpecification(scope, null, null), PageRequest.of(page, size, Sort.Direction.valueOf(sortDirection), sortField));
    }

    /**
     * Searches for entities using a custom specification with pagination and sorting.
     * <p>
     * This method combines the provided specification with security constraints and returns
     * a paginated result set with the specified sorting.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param specification the search specification to apply
     * @param page the page number (zero-based)
     * @param size the number of records per page
     * @param sortField the entity field name to sort by
     * @param sortDirection the sort direction ("ASC" or "DESC")
     * @return a {@link Page} of entities matching criteria within authorized scope
     */
    @Override
    default Page<T> search(SecurityScope scope, Specification<T> specification, int page, int size, String sortField, String sortDirection) {
        return this.findAll(secureSpecification(scope, specification, null), PageRequest.of(page, size, Sort.Direction.valueOf(sortDirection), sortField));
    }

    /**
     * Searches for entities using a custom specification within an organization with pagination.
     * <p>
     * This method combines the specification with organization constraints, applies security
     * checks, and returns a paginated result set with the specified sorting.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param organizationId the organization ID to filter by
     * @param specification the search specification to apply
     * @param page the page number (zero-based)
     * @param size the number of records per page
     * @param sortField the entity field name to sort by
     * @param sortDirection the sort direction ("ASC" or "DESC")
     * @return a {@link Page} of entities matching criteria within the organization
     */
    @Override
    default Page<T> search(SecurityScope scope, Long organizationId, Specification<T> specification, int page, int size, String sortField, String sortDirection) {
        return this.findAll(secureSpecification(scope, specification, null, organizationId), PageRequest.of(page, size, Sort.Direction.valueOf(sortDirection), sortField));
    }

    /**
     * Performs free-text search with pagination and sorting.
     * <p>
     * This method uses {@link #searchSpecification} to build LIKE clauses on the
     * {@code indexString} field, applies security constraints, and returns a paginated result set.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param searchTerm the search term for free-text matching
     * @param page the page number (zero-based)
     * @param size the number of records per page
     * @param sortField the entity field name to sort by
     * @param sortDirection the sort direction ("ASC" or "DESC")
     * @return a {@link Page} of entities matching the search term within authorized scope
     */
    @Override
    default Page<T> search(SecurityScope scope, String searchTerm, int page, int size, String sortField, String sortDirection) {
        return this.findAll(secureSpecification(scope, searchSpecification(searchTerm), null), PageRequest.of(page, size, Sort.Direction.valueOf(sortDirection), sortField));
    }

    /**
     * Performs free-text search combined with a custom specification with pagination.
     * <p>
     * This method combines {@link #searchSpecification} with the provided specification,
     * applies security constraints, and returns a paginated result set.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param searchTerm the search term for free-text matching
     * @param specification additional search specification to apply
     * @param page the page number (zero-based)
     * @param size the number of records per page
     * @param sortField the entity field name to sort by
     * @param sortDirection the sort direction ("ASC" or "DESC")
     * @return a {@link Page} of entities matching all criteria within authorized scope
     */
    @Override
    default Page<T> search(SecurityScope scope, String searchTerm, Specification<T> specification, int page, int size, String sortField, String sortDirection) {
        return this.findAll(secureSpecification(scope, searchSpecification(searchTerm).and(specification), null), PageRequest.of(page, size, Sort.Direction.valueOf(sortDirection), sortField));
    }

    /**
     * Performs free-text search within an organization with pagination and sorting.
     * <p>
     * This method combines {@link #searchSpecification} with organization constraints,
     * applies security checks, and returns a paginated result set.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param searchTerm the search term for free-text matching
     * @param organizationId the organization ID to filter by
     * @param page the page number (zero-based)
     * @param size the number of records per page
     * @param sortField the entity field name to sort by
     * @param sortDirection the sort direction ("ASC" or "DESC")
     * @return a {@link Page} of entities matching criteria within the organization
     */
    @Override
    default Page<T> search(SecurityScope scope, String searchTerm, Long organizationId, int page, int size, String sortField, String sortDirection) {
        return this.findAll(secureSpecification(scope, searchSpecification(searchTerm), null, organizationId), PageRequest.of(page, size, Sort.Direction.valueOf(sortDirection), sortField));
    }

    /**
     * Performs comprehensive search with free-text, specification, organization, and pagination.
     * <p>
     * This method combines {@link #searchSpecification}, custom specification, and organization
     * constraints, then returns a paginated result set using the provided {@link Pageable}.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param searchTerm the search term for free-text matching
     * @param organizationId the organization ID to filter by
     * @param specification additional search specification to apply
     * @param pageable pagination and sorting configuration
     * @return a {@link Page} of entities matching all criteria within the organization
     */
    @Override
    default Page<T> search(SecurityScope scope, String searchTerm, Long organizationId, Specification<T> specification, Pageable pageable) {
        return this.findAll(secureSpecification(scope, searchSpecification(searchTerm).and(specification), null, organizationId), pageable);
    }

    /**
     * Performs comprehensive search with free-text, specification, organization, filters, and pagination.
     * <p>
     * This is the most feature-rich search method, combining {@link #searchSpecification},
     * custom specification, {@link #filterSpecification} for frontend filters, and organization
     * constraints. Returns a paginated result set.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param searchTerm the search term for free-text matching
     * @param organizationId the organization ID to filter by
     * @param specification additional search specification to apply
     * @param pageable pagination and sorting configuration
     * @param filters list of frontend filter tuples to apply
     * @return a {@link Page} of entities matching all criteria within the organization
     * @see #filterSpecification
     */
    @Override
    default Page<T> search(SecurityScope scope, String searchTerm, Long organizationId, Specification<T> specification, Pageable pageable, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters) {
        return this.findAll(secureSpecification(scope, searchSpecification(searchTerm).and(specification).and(filterSpecification(filters)), null, organizationId), pageable);
    }
    
    /**
     * Performs comprehensive search across multiple organizations with filters and pagination.
     * <p>
     * This method is similar to the single-organization variant but supports filtering across
     * multiple organization IDs. Combines {@link #searchSpecification}, custom specification,
     * {@link #filterSpecification}, and multi-organization constraints.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param searchTerm the search term for free-text matching
     * @param organizationIds set of organization IDs to filter by
     * @param specification additional search specification to apply
     * @param pageable pagination and sorting configuration
     * @param filters list of frontend filter tuples to apply
     * @return a {@link Page} of entities matching all criteria across specified organizations
     */
    @Override
    default Page<T> search(SecurityScope scope, String searchTerm, Set<Long> organizationIds, Specification<T> specification, Pageable pageable, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters) {
        return this.findAll(secureSpecification(scope, searchSpecification(searchTerm).and(specification).and(filterSpecification(filters)), null, organizationIds), pageable);
    }

    /**
     * Performs comprehensive search returning a non-paginated list with filters.
     * <p>
     * This method combines {@link #searchSpecification}, custom specification,
     * {@link #filterSpecification}, and organization constraints, returning all matching
     * entities as a {@link List} rather than a paginated result.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param searchTerm the search term for free-text matching
     * @param organizationId the organization ID to filter by
     * @param specification additional search specification to apply
     * @param filters list of frontend filter tuples to apply
     * @return list of entities matching all criteria within the organization
     */
    @Override
    default List<T> search(SecurityScope scope, String searchTerm, Long organizationId, Specification<T> specification, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters) {
        return this.findAll(secureSpecification(scope, searchSpecification(searchTerm).and(specification).and(filterSpecification(filters)), null, organizationId));
    }

    /**
     * Finds a single entity by ID, entity reference, or specification with security constraints.
     * <p>
     * This polymorphic method accepts three types of input:
     * </p>
     * <ul>
     * <li>{@link Number} or {@link String}: Treated as entity ID</li>
     * <li>{@link LongIdEntity}: Entity reference (extracts ID)</li>
     * <li>{@link Specification}: Custom specification</li>
     * </ul>
     * <p>
     * All lookups apply security constraints via {@link #secureSpecification}.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param idOrEntityOrSpecification the ID, entity, or specification to search by
     * @return the matching entity within authorized scope, or {@code null} if not found or input is {@code null}
     * @see #extractEntityId
     */
    @Override
    default T findOne(SecurityScope scope, Object idOrEntityOrSpecification) {
        if (idOrEntityOrSpecification == null) { return null; }
        Long id = extractEntityId(idOrEntityOrSpecification);
        if (idOrEntityOrSpecification instanceof Specification s) {
            return findOne(secureSpecification(scope, (Specification<T>)s, null)).orElse(null);
        }
        return findOne(secureSpecification(scope, idSpecification(id), null)).orElse(null);
    }

    /**
     * Extracts a Long ID from various input types.
     * <p>
     * This utility method supports extracting entity IDs from:
     * </p>
     * <ul>
     * <li>{@link Number}: Converts to {@code Long} via {@code longValue()}</li>
     * <li>{@link String}: Parses as {@code Long}</li>
     * <li>{@link LongIdEntity}: Extracts via {@code getId()}</li>
     * </ul>
     *
     * @param idOrEntityOrSpecification the input object
     * @return the extracted Long ID, or {@code null} if extraction fails
     */
    @Nullable
    private static Long extractEntityId(Object idOrEntityOrSpecification) {
        Long id = null;
        if (idOrEntityOrSpecification instanceof Number n) {
            id = n.longValue();
        } else if (idOrEntityOrSpecification instanceof String s) {
            id = Long.parseLong(s);
        } else if (idOrEntityOrSpecification instanceof LongIdEntity e) {
            id = e.getId();
        }
        return id;
    }

    /**
     * Finds all entities accessible within the authorized security scope.
     * <p>
     * This method applies security constraints via {@link #secureSpecification} to return
     * only entities the current user is authorized to access.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @return list of all entities within authorized scope
     */
    @Override
    default List<T> findAll(SecurityScope scope) {
        return findAll(secureSpecification(scope, null, null));
    }

    /**
     * Saves a single entity after validating write privileges.
     * <p>
     * This method enforces privilege checks via {@link #hasWritePrivilegeForEntity} before
     * persisting the entity. If the current user lacks required write privileges, an
     * {@link AccessDeniedException} is thrown.
     * </p>
     *
     * @param <S> the entity type extending {@code T}
     * @param scope the security scope for privilege evaluation
     * @param entity the entity to save
     * @return the saved entity
     * @throws AccessDeniedException if the user lacks required write privileges
     * @see #hasWritePrivilegeForEntity
     */
    @Override
    default <S extends T> S saveOne(SecurityScope scope, S entity) {
        if(hasWritePrivilegeForEntity(scope, entity)){
            return save(entity);
        }
        throw new AccessDeniedException("Operation not allowed");
    }

    /**
     * Populates entity from form data and saves after validating write privileges.
     * <p>
     * This method delegates to {@link AbstractEntityForm#populateToEntity} to transfer form
     * data to the entity, then calls {@link #saveOne} to persist with privilege validation.
     * </p>
     *
     * @param <S> the entity type extending {@code T}
     * @param scope the security scope for privilege evaluation
     * @param entity the entity to populate and save
     * @param form the form containing data to populate into the entity
     * @return the saved entity with form data
     * @throws AccessDeniedException if the user lacks required write privileges
     * @see AbstractEntityForm#populateToEntity
     */
    @Override
    default <S extends T> S saveForm(SecurityScope scope, S entity, AbstractEntityForm form) {
        form.populateToEntity((LongIdEntity) entity);
        return saveOne(scope, entity);
    }

    /**
     * Saves multiple entities after validating write privileges for each.
     * <p>
     * This method accepts entities as an array or {@link Iterable}, validates write privileges
     * for each entity via {@link #hasWritePrivilegeForEntity}, and persists all entities.
     * If any entity fails privilege validation, an {@link AccessDeniedException} is thrown
     * before any entities are saved.
     * </p>
     *
     * @param <S> the entity type extending {@code T}
     * @param scope the security scope for privilege evaluation
     * @param entitiesCollection array or iterable collection of entities to save
     * @return list of saved entities
     * @throws AccessDeniedException if the user lacks required write privileges for any entity
     * @see #hasWritePrivilegeForEntity
     */
    @Override
    default <S extends T> List<S> saveAll(SecurityScope scope, Object entitiesCollection) {
        Iterable<S> iterable = null;
        if (entitiesCollection == null) {
            return null;
        }
        if (entitiesCollection.getClass().isArray()) {
            iterable = Arrays.asList((S[]) entitiesCollection);
        } else if (entitiesCollection instanceof Iterable i) {
            iterable = i;
        }
        for (S s : iterable) {
            if(!hasWritePrivilegeForEntity(scope, s)){
                throw new AccessDeniedException("Operation not allowed");
            }
        }
        return saveAll(iterable);
    }

    /**
     * Deletes a single entity by ID or entity reference within a transaction.
     * <p>
     * This {@code @Transactional} method extracts the entity ID via {@link #extractEntityId},
     * deletes using {@link #idSpecification}, and returns {@code true} if any records were deleted.
     * </p>
     * <p>
     * <b>Note</b>: Write privilege validation is currently not implemented (see TODO comment in code).
     * Future versions should enforce privilege checks before deletion.
     * </p>
     *
     * @param scope the security scope for privilege evaluation (not currently enforced)
     * @param idOrEntity the entity ID or entity reference to delete
     * @return {@code true} if entity was deleted, {@code false} if not found or input is {@code null}
     * @see #extractEntityId
     */
    @Override
    @Transactional
    default boolean deleteOne(SecurityScope scope, Object idOrEntity) {
        if (idOrEntity == null) {return false;}
        Long entityId = extractEntityId(idOrEntity);
        //TODO: check writePrivilege
//        if(hasWritePrivilegeForEntity(t)){
        long deletedCount = delete(idSpecification(entityId));
        return deletedCount > 0;
//            return true;
//        }
//        throw new AccessDeniedException("Operation not allowed");
    }

    /**
     * Deletes multiple entities by IDs, entity references, or specification.
     * <p>
     * This method accepts three types of input:
     * </p>
     * <ul>
     * <li>{@link Specification}: Deletes all entities matching the specification</li>
     * <li>Array or {@link Iterable} of IDs/entities: Converts to ID list via {@link #toIdList}
     *     and deletes using {@link #idsSpecification}</li>
     * </ul>
     * <p>
     * Security constraints are applied via {@link #secureSpecification} when using specifications.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param idsOrEntitiesCollectionOrSpecification IDs, entities, or specification to delete by
     * @return the number of entities deleted
     * @see #toIdList
     */
    @Override
    default long deleteAll(SecurityScope scope, Object idsOrEntitiesCollectionOrSpecification) {
        if (idsOrEntitiesCollectionOrSpecification == null) {
            return 0;
        }
        if (idsOrEntitiesCollectionOrSpecification instanceof Specification<?> s) {
            return delete(secureSpecification(scope, (Specification<T>) s, null));
        }
        List<Long> ids = toIdList(idsOrEntitiesCollectionOrSpecification);
        return delete(secureSpecification(scope, idsSpecification(ids), null));
    }

    /**
     * Checks whether an entity exists by ID or entity reference with security constraints.
     * <p>
     * This method extracts the entity ID via {@link #extractEntityId}, builds a specification
     * via {@link #idSpecification}, applies security constraints via {@link #secureSpecification},
     * and checks for existence within authorized scope.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param idOrEntity the entity ID or entity reference to check
     * @return {@code true} if entity exists within authorized scope, {@code false} if not found or input is {@code null}
     */
    @Override
    default boolean existsOne(SecurityScope scope, Object idOrEntity) {
        if (idOrEntity == null) {return false;}
        Long entityId = extractEntityId(idOrEntity);
        boolean entityExists = exists(secureSpecification(scope, idSpecification(entityId), null));
        return entityExists;
    }

    /**
     * Checks whether any entities exist by IDs, entity references, or specification.
     * <p>
     * This method accepts two types of input:
     * </p>
     * <ul>
     * <li>{@link Specification}: Checks for entities matching the specification</li>
     * <li>Array or {@link Iterable} of IDs/entities: Converts to ID list via {@link #toIdList}
     *     and checks existence using {@link #idsSpecification}</li>
     * </ul>
     * <p>
     * Security constraints are applied via {@link #secureSpecification}.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param idsOrEntitiesCollectionOrSpecification IDs, entities, or specification to check
     * @return {@code true} if any matching entities exist within authorized scope, {@code false} otherwise
     * @see #toIdList
     */
    @Override
    default boolean existsAny(SecurityScope scope, Object idsOrEntitiesCollectionOrSpecification) {
        if (idsOrEntitiesCollectionOrSpecification == null) {
            return false;
        }
        if (idsOrEntitiesCollectionOrSpecification instanceof Specification<?> s) {
            return exists(secureSpecification(scope, (Specification<T>) s, null));
        }
        List<Long> ids = toIdList(idsOrEntitiesCollectionOrSpecification);
        return exists(secureSpecification(scope, idsSpecification(ids), null));
    }

    /**
     * Converts a collection of IDs or entities to a list of Long IDs.
     * <p>
     * This utility method supports extracting IDs from:
     * </p>
     * <ul>
     * <li>Arrays: Converts to list and extracts IDs</li>
     * <li>{@link Iterable}: Iterates and extracts IDs via {@link #extractEntityId}</li>
     * </ul>
     *
     * @param idsOrEntitiesCollection array or iterable of IDs or entities
     * @return list of extracted Long IDs, or {@code null} if input is not array or iterable
     * @see #extractEntityId
     */
    private static List<Long> toIdList(Object idsOrEntitiesCollection) {
        Iterable iterable = null;
        if (idsOrEntitiesCollection.getClass().isArray()) {
            iterable = Arrays.asList((Object[]) idsOrEntitiesCollection);
        } else if (idsOrEntitiesCollection instanceof Iterable i) {
            iterable = i;
        }
        if (iterable != null) {
            List<Long> ids = new ArrayList<>();
            for (Object i : iterable) {
                Long id = extractEntityId(i);
                if (id != null) { ids.add(id); }
            }
            return ids;
        }
        return null;
    }

    /**
     * Counts all entities accessible within the authorized security scope.
     * <p>
     * This method applies security constraints via {@link #secureSpecification} to count
     * only entities the current user is authorized to access.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @return the number of entities within authorized scope
     */
    @Override
    default long count(SecurityScope scope) {
        return count(secureSpecification(scope, null, null));
    }

    /**
     * Counts entities matching a specific field value with security constraints.
     * <p>
     * This method builds a {@link #fieldSpecification} for the provided field name and value,
     * applies security constraints, and returns the count of matching entities.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param fieldName the entity field name to filter on
     * @param value the value to match
     * @return the number of entities matching the field criteria within authorized scope
     */
    @Override
    default long count(SecurityScope scope, String fieldName, Object value) {
        return count(secureSpecification(scope, fieldSpecification(fieldName, value), null));
    }

    /**
     * Counts entities using a custom specification with security constraints.
     * <p>
     * This method wraps the provided specification with {@link #secureSpecification} and
     * returns the count of matching entities.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param specification the search specification to apply
     * @return the number of entities matching the specification within authorized scope
     */
    @Override
    default long count(SecurityScope scope, Specification<T> specification) {
        return count(secureSpecification(scope, specification, null));
    }

    /**
     * Counts entities using a custom specification within a specific organization.
     * <p>
     * This method combines the specification with organization constraints via
     * {@link #secureSpecification} and returns the count of matching entities.
     * </p>
     *
     * @param scope the security scope for privilege evaluation
     * @param organizationId the organization ID to filter by
     * @param specification the search specification to apply
     * @return the number of entities matching criteria within the specified organization
     */
    @Override
    default long count(SecurityScope scope, Long organizationId, Specification<T> specification) {
        return count(secureSpecification(scope, specification, null, organizationId));
    }
    
    /**
     * Creates a new entity instance initialized with the current tenant's organization ID.
     * <p>
     * This factory method uses reflection to instantiate the entity class retrieved from
     * {@link SearchableRepositoryMetadata}. The entity is initialized with the organization ID
     * from {@link TenantResolver#getTenantedResource}, ensuring proper multi-tenant isolation.
     * </p>
     * <p>
     * This method requires the entity class to have a constructor accepting a single {@code Long}
     * parameter for the organization ID.
     * </p>
     *
     * @return a new entity instance initialized with the current tenant's organization ID
     * @throws RuntimeException if reflection fails or required constructor is not found
     * @see SearchableRepositoryMetadata
     * @see TenantResolver
     */
    @Override
    default T getNew() {
        try {
            Class entityClass = SearchableRepositories.getGlobalSearchableRepositoryAnnotation(this).entityClass();
            Constructor c = entityClass.getConstructor(Long.class);
            T obj = (T) c.newInstance(TenantResolver.getTenantedResource().organizationId);
            return obj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the {@link SearchableRepositoryMetadata} annotation from this repository interface.
     * <p>
     * This method uses reflection to inspect the first interface implemented by this repository
     * and returns the {@link SearchableRepositoryMetadata} annotation if present. This metadata
     * contains information about the entity class, display name, and other repository characteristics.
     * </p>
     * <p>
     * This metadata is used by various framework components including {@link #getNew} for entity
     * instantiation and the global repository registry.
     * </p>
     *
     * @return the {@link SearchableRepositoryMetadata} annotation, or {@code null} if not found
     * @see SearchableRepositoryMetadata
     * @see #getNew
     */
    @Override
    default SearchableRepositoryMetadata getSearchableRepositoryMetadata() {
        Class<?>[] interfaces = this.getClass().getInterfaces();
        if (interfaces == null || interfaces.length == 0) { return null; }
        SearchableRepositoryMetadata gsa = interfaces[0].getAnnotation(SearchableRepositoryMetadata.class);
        return gsa;
    }
}
