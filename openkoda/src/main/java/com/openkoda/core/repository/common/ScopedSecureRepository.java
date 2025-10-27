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
import com.openkoda.model.common.SearchableRepositoryMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import reactor.util.function.Tuple3;

import java.util.List;
import java.util.Set;

/**
 * Generic scope-aware repository interface for organization-scoped entity search and CRUD operations.
 * <p>
 * This interface centralizes multi-tenant data access patterns by providing search methods that automatically
 * scope results to specific organizations. Supports both single organization ID (Long) and multiple organization
 * IDs (Set&lt;Long&gt;) for flexible tenant isolation across the OpenKoda platform.
 * </p>
 * <p>
 * The repository provides multiple search capabilities:
 * <ul>
 *   <li>Specification-based queries using JPA Criteria API predicates</li>
 *   <li>Free-text search across entity fields</li>
 *   <li>Frontend filter tuples for dynamic UI-driven queries</li>
 *   <li>Paginated results via Spring Data {@code Page<T>}</li>
 *   <li>Unpaginated results as {@code List<T>}</li>
 * </ul>
 * </p>
 * <p>
 * Key methods include {@link #getSearchableRepositoryMetadata()} for entity metadata access
 * and {@link #getNew()} for factory-based entity instantiation.
 * </p>
 * <p>
 * Implementation note: Parameter types use flexible Object identifiers to support various lookup strategies.
 * Implementers must document expected parameter types (Long IDs, entity instances, or Specification objects).
 * </p>
 *
 * @param <T> the entity type managed by this repository
 * @since 1.7.1
 * @author OpenKoda Team
 * @see Specification
 * @see SearchableRepositoryMetadata
 * @see Page
 */
public interface ScopedSecureRepository<T> {

    /**
     * Searches entities by matching a specific field name to a given value.
     * <p>
     * This method performs a field-level equality search without explicit organization scoping.
     * Implementations may apply default organization context based on current security principal.
     * </p>
     *
     * @param fieldName the entity field name to search against
     * @param value the value to match (type should align with field type)
     * @return list of matching entities, or empty list if no matches found
     */
    List<T> search(String fieldName, Object value);
    
    /**
     * Searches entities using a JPA Specification predicate.
     * <p>
     * Allows custom query logic via the Criteria API without explicit organization scoping.
     * Implementations may inject organization context into the specification evaluation.
     * </p>
     *
     * @param specification the JPA Specification defining query predicates
     * @return list of matching entities, or empty list if no matches found
     */
    List<T> search(Specification<T> specification);
    
    /**
     * Searches all entities within a specific organization scope.
     * <p>
     * Returns all entities associated with the given organization ID for multi-tenant data isolation.
     * </p>
     *
     * @param organizationId the organization ID to scope the search (must not be null)
     * @return list of entities within organization scope, or empty list if none found
     */
    List<T> search(Long organizationId);
    
    /**
     * Searches entities within organization scope using a JPA Specification predicate.
     * <p>
     * Combines organization-based multi-tenant filtering with custom query predicates.
     * </p>
     *
     * @param organizationId the organization ID to scope the search (must not be null)
     * @param specification the JPA Specification defining additional query predicates
     * @return list of matching entities within organization scope, or empty list if no matches found
     */
    List<T> search(Long organizationId, Specification<T> specification);
    
    /**
     * Searches entities with pagination and sorting support.
     * <p>
     * Returns a page of results without explicit organization scoping.
     * Implementations may apply default organization context.
     * </p>
     *
     * @param page the zero-based page number
     * @param size the number of entities per page
     * @param sortField the entity field name to sort by
     * @param sortDirection the sort direction (asc or desc)
     * @return page containing matching entities with pagination metadata, or empty page if no results
     */
    Page<T> search(int page, int size, String sortField, String sortDirection);
    
    /**
     * Searches entities using JPA Specification with pagination and sorting.
     * <p>
     * Combines custom query predicates with paginated result delivery.
     * </p>
     *
     * @param specification the JPA Specification defining query predicates
     * @param page the zero-based page number
     * @param size the number of entities per page
     * @param sortField the entity field name to sort by
     * @param sortDirection the sort direction (asc or desc)
     * @return page containing matching entities, or empty page if no matches found
     */
    Page<T> search(Specification<T> specification, int page, int size, String sortField, String sortDirection);
    
    /**
     * Searches entities within organization scope using Specification with pagination and sorting.
     * <p>
     * Combines multi-tenant filtering, custom predicates, and paginated results.
     * </p>
     *
     * @param organizationId the organization ID to scope the search (must not be null)
     * @param specification the JPA Specification defining additional query predicates
     * @param page the zero-based page number
     * @param size the number of entities per page
     * @param sortField the entity field name to sort by
     * @param sortDirection the sort direction (asc or desc)
     * @return page containing matching entities within organization scope, or empty page if no matches
     */
    Page<T> search(Long organizationId, Specification<T> specification, int page, int size, String sortField, String sortDirection);
    
    /**
     * Searches entities using free-text search term with pagination and sorting.
     * <p>
     * Performs text-based search across searchable entity fields (typically indexed strings).
     * Search behavior depends on implementation-specific field configuration.
     * </p>
     *
     * @param searchTerm the free-text search term to match against entity fields (may be null or empty)
     * @param page the zero-based page number
     * @param size the number of entities per page
     * @param sortField the entity field name to sort by
     * @param sortDirection the sort direction (asc or desc)
     * @return page containing matching entities, or empty page if no matches found
     */
    Page<T> search(String searchTerm, int page, int size, String sortField, String sortDirection);
    
    /**
     * Searches entities using free-text search and JPA Specification with pagination and sorting.
     * <p>
     * Combines text-based search across searchable fields with custom query predicates.
     * Both text search and specification predicates must match for results to be included.
     * </p>
     *
     * @param searchTerm the free-text search term (may be null or empty to skip text filtering)
     * @param specification the JPA Specification defining additional query predicates
     * @param page the zero-based page number
     * @param size the number of entities per page
     * @param sortField the entity field name to sort by
     * @param sortDirection the sort direction (asc or desc)
     * @return page containing matching entities, or empty page if no matches found
     */
    Page<T> search(String searchTerm, Specification<T> specification, int page, int size, String sortField, String sortDirection) ;
    
    /**
     * Searches entities within organization scope using free-text search with pagination and sorting.
     * <p>
     * Performs text-based search within a specific organization for multi-tenant data isolation.
     * </p>
     *
     * @param searchTerm the free-text search term (may be null or empty)
     * @param organizationId the organization ID to scope the search (must not be null)
     * @param page the zero-based page number
     * @param size the number of entities per page
     * @param sortField the entity field name to sort by
     * @param sortDirection the sort direction (asc or desc)
     * @return page containing matching entities within organization scope, or empty page if no matches
     */
    Page<T> search(String searchTerm, Long organizationId, int page, int size, String sortField, String sortDirection);
    
    /**
     * Searches entities within organization scope using text search, Specification, and Spring Data Pageable.
     * <p>
     * Provides flexible pagination using Spring Data Pageable abstraction with multi-tenant filtering.
     * </p>
     *
     * @param searchTerm the free-text search term (may be null or empty)
     * @param organizationId the organization ID to scope the search (must not be null)
     * @param specification the JPA Specification defining additional query predicates
     * @param pageable the Spring Data Pageable containing page, size, and sort information
     * @return page containing matching entities within organization scope, or empty page if no matches
     */
    Page<T> search(String searchTerm, Long organizationId, Specification<T> specification, Pageable pageable);
    
    /**
     * Searches entities within organization scope with text search, Specification, Pageable, and frontend filters.
     * <p>
     * Applies dynamic UI-driven filters from frontend mapping field definitions in addition to text search
     * and custom predicates. Filter tuples contain field name, field definition, and filter value.
     * </p>
     *
     * @param searchTerm the free-text search term (may be null or empty)
     * @param organizationId the organization ID to scope the search (must not be null)
     * @param specification the JPA Specification defining additional query predicates
     * @param pageable the Spring Data Pageable for pagination and sorting
     * @param filters list of filter tuples (field name, field definition, value) for dynamic filtering
     * @return page containing matching entities within organization scope, or empty page if no matches
     */
    Page<T> search(String searchTerm, Long organizationId, Specification<T> specification, Pageable pageable, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters);
    
    /**
     * Searches entities across multiple organizations with text search, Specification, Pageable, and filters.
     * <p>
     * Supports multi-organization queries for scenarios requiring cross-tenant data access
     * (e.g., admin views, reporting). Entities matching any organization ID in the set are included.
     * </p>
     *
     * @param searchTerm the free-text search term (may be null or empty)
     * @param organizationId set of organization IDs to scope the search (must not be null or empty)
     * @param specification the JPA Specification defining additional query predicates
     * @param pageable the Spring Data Pageable for pagination and sorting
     * @param filters list of filter tuples (field name, field definition, value) for dynamic filtering
     * @return page containing matching entities across specified organizations, or empty page if no matches
     */
    Page<T> search(String searchTerm, Set<Long> organizationId, Specification<T> specification, Pageable pageable, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters);
    
    /**
     * Searches entities within organization scope using text search, Specification, and filters without pagination.
     * <p>
     * Returns all matching entities as a list without pagination constraints.
     * Use with caution for potentially large result sets.
     * </p>
     *
     * @param searchTerm the free-text search term (may be null or empty)
     * @param organizationId the organization ID to scope the search (must not be null)
     * @param specification the JPA Specification defining additional query predicates
     * @param filters list of filter tuples (field name, field definition, value) for dynamic filtering
     * @return list of all matching entities within organization scope, or empty list if no matches
     */
    List<T> search(String searchTerm, Long organizationId, Specification<T> specification, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters);
    
    /**
     * Finds a single entity by ID, entity instance, or Specification.
     * <p>
     * Flexible lookup method supporting multiple parameter types:
     * <ul>
     *   <li>Long ID - looks up entity by primary key</li>
     *   <li>Entity instance - retrieves managed version or refreshes state</li>
     *   <li>Specification - finds first entity matching predicate</li>
     * </ul>
     * Organization scoping is applied implicitly based on security context or entity association.
     * </p>
     *
     * @param idOrEntityOrSpecification the entity ID (Long), entity instance, or Specification to find by
     * @return the found entity, or null if not found or access denied
     */
    T findOne(Object idOrEntityOrSpecification);
    
    /**
     * Finds all entities accessible within the current security context.
     * <p>
     * Returns all entities the current user has access to based on implicit organization scoping.
     * Use with caution for large datasets - consider using paginated search methods instead.
     * </p>
     *
     * @return list of all accessible entities, or empty list if none found
     */
    List<T> findAll();
    
    /**
     * Saves a single entity with implicit organization context validation.
     * <p>
     * Persists a new entity or updates an existing entity. Organization association is validated
     * based on current security context to enforce multi-tenant data isolation.
     * </p>
     *
     * @param <S> the entity type (subtype of T)
     * @param entity the entity to save (must not be null)
     * @return the saved entity with generated ID or updated state
     */
    <S extends T> S saveOne(S entity);
    
    /**
     * Saves entity populated from form data with validation and organization context.
     * <p>
     * Applies form data to entity via {@code form.populateTo(entity)} after validation.
     * Organization context is enforced during persistence.
     * </p>
     *
     * @param <S> the entity type (subtype of T)
     * @param entity the entity to populate and save (must not be null)
     * @param form the form containing validated data to apply (must not be null)
     * @return the saved entity with form data applied
     */
    <S extends T> S saveForm(S entity, AbstractEntityForm form);
    
    /**
     * Saves multiple entities in a batch operation with organization context validation.
     * <p>
     * Accepts a collection of entities (List, Set, or array) and persists them with
     * organization association enforcement.
     * </p>
     *
     * @param <S> the entity type (subtype of T)
     * @param entitiesCollection collection of entities to save (List, Set, or array; must not be null)
     * @return list of saved entities with generated IDs or updated state
     */
    <S extends T> List<S> saveAll(Object entitiesCollection);
    
    /**
     * Deletes a single entity by ID or entity instance with organization context validation.
     * <p>
     * Accepts either entity ID (Long) or entity instance. Organization association is validated
     * before deletion to prevent unauthorized cross-tenant deletions.
     * </p>
     *
     * @param idOrEntity the entity ID (Long) or entity instance to delete (must not be null)
     * @return true if entity was deleted, false if not found or access denied
     */
    boolean deleteOne(Object idOrEntity);
    
    /**
     * Deletes multiple entities by IDs, entity instances, or Specification.
     * <p>
     * Supports batch deletion using:
     * <ul>
     *   <li>Collection of IDs (List&lt;Long&gt;, Set&lt;Long&gt;)</li>
     *   <li>Collection of entity instances</li>
     *   <li>Specification defining entities to delete</li>
     * </ul>
     * Organization context is validated for all deletions.
     * </p>
     *
     * @param idsOrEntitiesCollectionOrSpecification IDs, entities, or Specification (must not be null)
     * @return number of entities deleted
     */
    long deleteAll(Object idsOrEntitiesCollectionOrSpecification);
    
    /**
     * Checks if an entity exists by ID or entity instance within organization context.
     * <p>
     * Accepts entity ID (Long) or entity instance. Organization scoping is applied
     * to verify existence within accessible scope only.
     * </p>
     *
     * @param idOrEntity the entity ID (Long) or entity instance to check (must not be null)
     * @return true if entity exists and is accessible, false otherwise
     */
    boolean existsOne(Object idOrEntity);
    
    /**
     * Checks if any entities exist matching IDs, entity instances, or Specification.
     * <p>
     * Returns true if at least one entity exists within organization context matching the criteria.
     * </p>
     *
     * @param idsOrEntitiesCollectionOrSpecification IDs, entities, or Specification (must not be null)
     * @return true if any matching entities exist within accessible scope, false otherwise
     */
    boolean existsAny(Object idsOrEntitiesCollectionOrSpecification);
    
    /**
     * Counts all entities accessible within the current security context.
     * <p>
     * Returns total number of entities within implicit organization scope based on current user.
     * </p>
     *
     * @return total count of accessible entities
     */
    long count();
    
    /**
     * Counts entities matching a specific field name and value.
     * <p>
     * Performs field-level equality match within organization context.
     * </p>
     *
     * @param fieldName the entity field name to match against
     * @param value the value to match (type should align with field type)
     * @return count of matching entities within accessible scope
     */
    long count(String fieldName, Object value);
    
    /**
     * Counts entities within organization scope matching a JPA Specification.
     * <p>
     * Combines organization-based filtering with custom query predicates for counting.
     * </p>
     *
     * @param organizationId the organization ID to scope the count (must not be null)
     * @param specification the JPA Specification defining query predicates
     * @return count of matching entities within organization scope
     */
    long count(Long organizationId, Specification<T> specification);
    
    /**
     * Factory method to create a new entity instance with default initialization.
     * <p>
     * Returns a new entity instance ready for population. Implementations may apply
     * default organization context or other initialization logic.
     * </p>
     *
     * @return a new entity instance of type T
     */
    T getNew();

    /**
     * Provides metadata about the repository and its searchable entity.
     * <p>
     * Returns metadata including entity class, primary key type, searchable fields,
     * and other configuration used for dynamic query construction and UI generation.
     * </p>
     *
     * @return metadata object describing the repository and entity structure
     * @see SearchableRepositoryMetadata
     */
    SearchableRepositoryMetadata getSearchableRepositoryMetadata();
}
