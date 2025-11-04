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
import com.openkoda.core.security.HasSecurityRules.SecurityScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;
import reactor.util.function.Tuple3;

import java.util.List;
import java.util.Set;

/**
 * General secure repository interface requiring explicit {@link SecurityScope} parameter for all operations.
 * <p>
 * This interface implements a privilege-enforcing repository wrapper pattern that wraps standard Spring Data
 * repositories to enforce authorization checks. Privilege enforcement occurs at method invocation time before
 * delegating to the underlying repository implementation, ensuring that all data access operations are subject
 * to security validation.
 * 
 * <p>
 * Key method categories include:
 * 
 * <ul>
 *   <li>Search operations - field-based, Specification-driven, free-text, pageable searches</li>
 *   <li>CRUD operations - findOne, findAll, saveOne, saveForm, saveAll, deleteOne, deleteAll</li>
 *   <li>Existence checks - existsOne, existsAny</li>
 *   <li>Count utilities - counting entities with various filters</li>
 *   <li>Factory method - getNew() for creating new entity instances</li>
 * </ul>
 * <p>
 * The interface integrates with frontend filter tuples ({@code List<Tuple3<String, FrontendMappingFieldDefinition, String>>})
 * and Spring Data types ({@link Page}, {@link Pageable}, {@link Specification}) to provide flexible query capabilities.
 * 
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * SecureRepository<Organization> secureRepo = secureRepositories.organization;
 * Optional<Organization> org = secureRepo.findOne(scope, orgId);
 * }</pre>
 *
 * @param <T> the entity type managed by this repository
 * @since 1.7.1
 * @author OpenKoda Team
 * @see SecurityScope
 * @see Specification
 * @see Page
 */
@NoRepositoryBean
public interface UnscopedSecureRepository<T> {

    /**
     * Searches for entities where a specific field matches the given value.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param fieldName the name of the entity field to search on
     * @param value the value to match against the specified field
     * @return list of entities matching the field value, or empty list if no matches found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    List<T> search(SecurityScope scope, String fieldName, Object value);
    
    /**
     * Searches for entities scoped to a specific organization.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param organizationId the organization ID to scope the search to
     * @return list of entities belonging to the organization, or empty list if no matches found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    List<T> search(SecurityScope scope, Long organizationId);
    
    /**
     * Searches for entities matching a JPA Specification.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param specification the JPA Specification defining query criteria
     * @return list of entities matching the specification, or empty list if no matches found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    List<T> search(SecurityScope scope, Specification<T> specification);
    
    /**
     * Searches for entities scoped to an organization and matching a JPA Specification.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param organizationId the organization ID to scope the search to
     * @param specification the JPA Specification defining additional query criteria
     * @return list of entities matching both organization and specification, or empty list if no matches found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    List<T> search(SecurityScope scope, Long organizationId, Specification<T> specification);
    
    /**
     * Searches for entities with pagination and sorting.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param page the zero-based page number to retrieve
     * @param size the number of entities per page
     * @param sortField the field name to sort results by
     * @param sortDirection the sort direction (ASC or DESC)
     * @return page of entities with pagination metadata, or empty page if no matches found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    Page<T> search(SecurityScope scope, int page, int size, String sortField, String sortDirection);
    
    /**
     * Searches for entities matching a Specification with pagination and sorting.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param specification the JPA Specification defining query criteria
     * @param page the zero-based page number to retrieve
     * @param size the number of entities per page
     * @param sortField the field name to sort results by
     * @param sortDirection the sort direction (ASC or DESC)
     * @return page of matching entities with pagination metadata, or empty page if no matches found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    Page<T> search(SecurityScope scope, Specification<T> specification, int page, int size, String sortField, String sortDirection);
    
    /**
     * Searches for entities scoped to an organization and matching a Specification with pagination and sorting.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param organizationId the organization ID to scope the search to
     * @param specification the JPA Specification defining additional query criteria
     * @param page the zero-based page number to retrieve
     * @param size the number of entities per page
     * @param sortField the field name to sort results by
     * @param sortDirection the sort direction (ASC or DESC)
     * @return page of matching entities with pagination metadata, or empty page if no matches found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    Page<T> search(SecurityScope scope, Long organizationId, Specification<T> specification, int page, int size, String sortField, String sortDirection);
    
    /**
     * Performs free-text search across entity fields with pagination and sorting.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param searchTerm the text to search for across searchable entity fields
     * @param page the zero-based page number to retrieve
     * @param size the number of entities per page
     * @param sortField the field name to sort results by
     * @param sortDirection the sort direction (ASC or DESC)
     * @return page of entities matching the search term, or empty page if no matches found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    Page<T> search(SecurityScope scope, String searchTerm, int page, int size, String sortField, String sortDirection);
    
    /**
     * Performs free-text search combined with a Specification filter with pagination and sorting.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param searchTerm the text to search for across searchable entity fields
     * @param specification the JPA Specification defining additional query criteria
     * @param page the zero-based page number to retrieve
     * @param size the number of entities per page
     * @param sortField the field name to sort results by
     * @param sortDirection the sort direction (ASC or DESC)
     * @return page of entities matching both search term and specification, or empty page if no matches found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    Page<T> search(SecurityScope scope, String searchTerm, Specification<T> specification, int page, int size, String sortField, String sortDirection) ;
    
    /**
     * Performs free-text search scoped to an organization with pagination and sorting.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param searchTerm the text to search for across searchable entity fields
     * @param organizationId the organization ID to scope the search to
     * @param page the zero-based page number to retrieve
     * @param size the number of entities per page
     * @param sortField the field name to sort results by
     * @param sortDirection the sort direction (ASC or DESC)
     * @return page of entities matching search term within organization scope, or empty page if no matches found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    Page<T> search(SecurityScope scope, String searchTerm, Long organizationId, int page, int size, String sortField, String sortDirection);
    
    /**
     * Performs free-text search scoped to an organization with Specification and Pageable support.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param searchTerm the text to search for across searchable entity fields
     * @param organizationId the organization ID to scope the search to
     * @param specification the JPA Specification defining additional query criteria
     * @param pageable the Spring Data Pageable defining page, size, and sort parameters
     * @return page of entities matching all criteria, or empty page if no matches found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    Page<T> search(SecurityScope scope, String searchTerm, Long organizationId, Specification<T> specification, Pageable pageable);
    
    /**
     * Performs free-text search with organization scoping, Specification, Pageable, and frontend filters.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param searchTerm the text to search for across searchable entity fields
     * @param organizationId the organization ID to scope the search to
     * @param specification the JPA Specification defining additional query criteria
     * @param pageable the Spring Data Pageable defining page, size, and sort parameters
     * @param filters list of frontend filter tuples containing field name, field definition, and filter value
     * @return page of entities matching all criteria including frontend filters, or empty page if no matches found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    Page<T> search(SecurityScope scope, String searchTerm, Long organizationId, Specification<T> specification, Pageable pageable, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters);
    
    /**
     * Performs free-text search scoped to multiple organizations with Specification, Pageable, and frontend filters.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param searchTerm the text to search for across searchable entity fields
     * @param organizationId set of organization IDs to scope the search to
     * @param specification the JPA Specification defining additional query criteria
     * @param pageable the Spring Data Pageable defining page, size, and sort parameters
     * @param filters list of frontend filter tuples containing field name, field definition, and filter value
     * @return page of entities matching all criteria across multiple organizations, or empty page if no matches found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    Page<T> search(SecurityScope scope, String searchTerm, Set<Long> organizationId, Specification<T> specification, Pageable pageable, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters);
    
    /**
     * Performs free-text search with organization scoping, Specification, and frontend filters returning unpaged results.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param searchTerm the text to search for across searchable entity fields
     * @param organizationId the organization ID to scope the search to
     * @param specification the JPA Specification defining additional query criteria
     * @param filters list of frontend filter tuples containing field name, field definition, and filter value
     * @return list of all entities matching all criteria, or empty list if no matches found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    List<T> search(SecurityScope scope, String searchTerm, Long organizationId, Specification<T> specification, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters);
    
    /**
     * Finds a single entity by ID, entity instance, or Specification.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param idOrEntityOrSpecification the entity ID (Long), entity instance, or JPA Specification to find by
     * @return the entity matching the criteria, or null if not found or access denied
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    T findOne(SecurityScope scope, Object idOrEntityOrSpecification);
    
    /**
     * Finds all entities accessible within the given security scope.
     *
     * @param scope the security scope defining access permissions for this operation
     * @return list of all accessible entities, or empty list if no entities are accessible
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    List<T> findAll(SecurityScope scope);
    
    /**
     * Saves a single entity after enforcing write privilege checks.
     *
     * @param <S> the entity type (must extend T)
     * @param scope the security scope defining access permissions for this operation
     * @param entity the entity instance to save
     * @return the saved entity (with generated ID if new)
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies write access
     */
    <S extends T> S saveOne(SecurityScope scope, S entity);
    
    /**
     * Saves an entity populated from a form after enforcing write privilege checks.
     *
     * @param <S> the entity type (must extend T)
     * @param scope the security scope defining access permissions for this operation
     * @param entity the entity instance to save
     * @param form the form containing validated data to populate the entity
     * @return the saved entity with form data applied
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies write access
     */
    <S extends T> S saveForm(SecurityScope scope, S entity, AbstractEntityForm form);
    
    /**
     * Saves multiple entities after enforcing write privilege checks on each entity.
     *
     * @param <S> the entity type (must extend T)
     * @param scope the security scope defining access permissions for this operation
     * @param entitiesCollection collection of entities to save (List, Set, or array)
     * @return list of saved entities
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies write access to any entity
     */
    <S extends T> List<S> saveAll(SecurityScope scope, Object entitiesCollection);
    
    /**
     * Deletes a single entity after enforcing write privilege checks.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param idOrEntity the entity ID (Long) or entity instance to delete
     * @return true if the entity was successfully deleted, false if not found
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies write access
     */
    boolean deleteOne(SecurityScope scope, Object idOrEntity);
    
    /**
     * Deletes multiple entities after enforcing write privilege checks on each entity.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param idsOrEntitiesCollectionOrSpecification collection of IDs, entities, or a JPA Specification defining entities to delete
     * @return the number of entities successfully deleted
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies write access to any entity
     */
    long deleteAll(SecurityScope scope, Object idsOrEntitiesCollectionOrSpecification);
    
    /**
     * Checks if a single entity exists and is accessible within the security scope.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param idOrEntity the entity ID (Long) or entity instance to check existence for
     * @return true if the entity exists and is accessible, false otherwise
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    boolean existsOne(SecurityScope scope, Object idOrEntity);
    
    /**
     * Checks if any entities exist matching the criteria within the security scope.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param idsOrEntitiesCollectionOrSpecification collection of IDs, entities, or a JPA Specification to check existence for
     * @return true if at least one matching entity exists and is accessible, false otherwise
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    boolean existsAny(SecurityScope scope, Object idsOrEntitiesCollectionOrSpecification);
    
    /**
     * Counts all entities accessible within the security scope.
     *
     * @param scope the security scope defining access permissions for this operation
     * @return the total number of accessible entities
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    long count(SecurityScope scope);
    
    /**
     * Counts entities where a specific field matches the given value within the security scope.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param fieldName the name of the entity field to filter by
     * @param value the value to match against the specified field
     * @return the number of matching entities
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    long count(SecurityScope scope, String fieldName, Object value);
    
    /**
     * Counts entities matching a JPA Specification within the security scope.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param specification the JPA Specification defining query criteria
     * @return the number of entities matching the specification
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    long count(SecurityScope scope, Specification<T> specification);
    
    /**
     * Counts entities scoped to an organization and matching a JPA Specification.
     *
     * @param scope the security scope defining access permissions for this operation
     * @param organizationId the organization ID to scope the count to
     * @param specification the JPA Specification defining additional query criteria
     * @return the number of entities matching both organization and specification
     * @throws org.springframework.security.access.AccessDeniedException if the security scope denies access
     */
    long count(SecurityScope scope, Long organizationId, Specification<T> specification);
    
    /**
     * Creates a new instance of the entity type managed by this repository.
     * <p>
     * This factory method provides a convenient way to instantiate new entity instances
     * with appropriate initialization based on the current context (e.g., tenant resolver).
     * 
     *
     * @return a new entity instance ready for population and persistence
     */
    T getNew();
}
