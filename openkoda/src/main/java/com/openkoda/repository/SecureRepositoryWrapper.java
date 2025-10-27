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

package com.openkoda.repository;

import com.openkoda.core.form.AbstractEntityForm;
import com.openkoda.core.form.FrontendMappingFieldDefinition;
import com.openkoda.core.repository.common.ScopedSecureRepository;
import com.openkoda.core.repository.common.SearchableFunctionalRepositoryWithLongId;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.common.SearchableEntity;
import com.openkoda.model.common.SearchableRepositoryMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import reactor.util.function.Tuple3;

import java.util.List;
import java.util.Set;

/**
 * Concrete wrapper implementation binding a security scope to a searchable repository for privilege-enforced operations.
 * <p>
 * Implements {@link ScopedSecureRepository} to provide scoped repository access with privilege enforcement.
 * This wrapper binds any {@link SearchableFunctionalRepositoryWithLongId} instance with a fixed {@link HasSecurityRules.SecurityScope},
 * forwarding all method calls to the wrapped repository while pre-applying the bound scope.
 * </p>
 * <p>
 * Created via {@code SecureRepository.scoped(SecurityScope)} for temporary scope elevation scenarios.
 * The scope determines the privilege level for all operations:
 * <ul>
 *   <li><b>USER</b> (default): Standard user-level access with entity-level privilege checks</li>
 *   <li><b>ORGANIZATION</b>: Organization-admin level access for tenant-scoped operations</li>
 *   <li><b>GLOBAL</b>: System-admin level access for cross-tenant operations</li>
 * </ul>
 * All CRUD operations (find, save, delete, search, count) enforce privilege checks based on the bound scope level.
 * </p>
 * <p>
 * The wrapper is <b>immutable</b> after construction - the scope cannot be changed once set.
 * Thread-safety is guaranteed as long as the wrapped repository is thread-safe.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * ScopedSecureRepository&lt;Organization&gt; orgScoped = secureRepo.scoped(ORGANIZATION);
 * List&lt;Organization&gt; orgs = orgScoped.findAll(); // Privilege-checked
 * </pre>
 * </p>
 *
 * @param <T> Searchable entity type with privilege annotations extending {@link SearchableEntity}
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SecureRepository
 * @see SearchableFunctionalRepositoryWithLongId
 * @see HasSecurityRules.SecurityScope
 * @see ScopedSecureRepository
 */
public class SecureRepositoryWrapper<T extends SearchableEntity> implements ScopedSecureRepository<T> {
    
    /**
     * Underlying repository instance receiving all forwarded calls with the bound scope parameter.
     * <p>
     * This repository performs the actual data access operations and privilege enforcement.
     * All method invocations on this wrapper are delegated to this repository with the pre-configured scope.
     * </p>
     */
    private final SearchableFunctionalRepositoryWithLongId<T> wrapped;
    
    /**
     * Fixed security scope (USER/ORGANIZATION/GLOBAL) applied to all operations.
     * <p>
     * This scope determines the privilege level for all repository operations and cannot be changed
     * after construction. The scope is automatically passed as the first parameter to all wrapped
     * repository method calls.
     * </p>
     */
    private final HasSecurityRules.SecurityScope scope;

    /**
     * Constructs wrapper binding the repository to a specific security scope.
     * <p>
     * The provided scope is permanently bound to this wrapper instance and will be
     * applied to all subsequent repository operations. Both parameters are required and
     * must not be null.
     * </p>
     *
     * @param wrapped Repository instance to wrap, must not be null
     * @param scope Security scope (USER/ORGANIZATION/GLOBAL) to apply to all operations, must not be null
     */
    public SecureRepositoryWrapper(SearchableFunctionalRepositoryWithLongId<T> wrapped, HasSecurityRules.SecurityScope scope) {
        this.wrapped = wrapped;
        this.scope = scope;
    }

    /**
     * Searches for entities matching the specified field and value within the bound security scope.
     * <p>
     * Performs a field-equality search with privilege enforcement based on the wrapper's bound scope.
     * Only entities accessible within the configured scope level are returned.
     * </p>
     *
     * @param fieldName Field name to search on, must be a valid entity field
     * @param value Value to match, null searches for null values
     * @return List of entities matching the criteria within bound scope, empty list if none found
     */
    public List<T> search(String fieldName, Object value) {
        return wrapped.search(scope, fieldName, value);
    }

    /**
     * Searches for entities matching the JPA specification within the bound security scope.
     * <p>
     * All search method variants follow the same privilege-enforcement pattern: forwarding to the
     * wrapped repository with the bound scope parameter prepended. Results are filtered based on
     * the scope's privilege level (USER/ORGANIZATION/GLOBAL).
     * </p>
     *
     * @param specification JPA specification defining search criteria
     * @return List of entities matching the specification within bound scope
     */
    public List<T> search(Specification<T> specification) {
        return wrapped.search(scope, specification);
    }

    /**
     * Searches for entities within specified organization matching the specification.
     *
     * @param organizationId Organization ID for tenant-scoped search
     * @param specification JPA specification defining search criteria
     * @return List of entities matching criteria within organization and bound scope
     */
    public List<T> search(Long organizationId, Specification<T> specification) {
        return wrapped.search(scope, organizationId, specification);
    }

    /**
     * Searches for all entities within specified organization.
     *
     * @param organizationId Organization ID for tenant-scoped search
     * @return List of entities within organization and bound scope
     */
    public List<T> search(Long organizationId) {
        return wrapped.search(scope, organizationId);
    }

    /**
     * Searches for entities with pagination and sorting within bound scope.
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortField Field name for sorting
     * @param sortDirection Sort direction ("asc" or "desc")
     * @return Page of entities matching criteria within bound scope
     */
    public Page<T> search(int page, int size, String sortField, String sortDirection) {
        return wrapped.search(scope, page, size, sortField, sortDirection);
    }

    /**
     * Searches with specification, pagination and sorting within bound scope.
     *
     * @param specification JPA specification defining search criteria
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortField Field name for sorting
     * @param sortDirection Sort direction ("asc" or "desc")
     * @return Page of entities matching criteria within bound scope
     */
    public Page<T> search(Specification<T> specification, int page, int size, String sortField, String sortDirection) {
        return wrapped.search(scope, specification, page, size, sortField, sortDirection);
    }

    /**
     * Searches within organization with specification, pagination and sorting.
     *
     * @param organizationId Organization ID for tenant-scoped search
     * @param specification JPA specification defining search criteria
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortField Field name for sorting
     * @param sortDirection Sort direction ("asc" or "desc")
     * @return Page of entities matching criteria within organization and bound scope
     */
    public Page<T> search(Long organizationId, Specification<T> specification, int page, int size, String sortField, String sortDirection) {
        return wrapped.search(scope, organizationId, specification, page, size, sortField, sortDirection);
    }

    /**
     * Full-text searches with pagination and sorting within bound scope.
     *
     * @param searchTerm Text search term applied to searchable fields
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortField Field name for sorting
     * @param sortDirection Sort direction ("asc" or "desc")
     * @return Page of entities matching search term within bound scope
     */
    public Page<T> search(String searchTerm, int page, int size, String sortField, String sortDirection) {
        return wrapped.search(scope, searchTerm, page, size, sortField, sortDirection);
    }

    /**
     * Full-text searches with specification, pagination and sorting.
     *
     * @param searchTerm Text search term applied to searchable fields
     * @param specification JPA specification for additional criteria
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortField Field name for sorting
     * @param sortDirection Sort direction ("asc" or "desc")
     * @return Page of entities matching search term and criteria within bound scope
     */
    public Page<T> search(String searchTerm, Specification<T> specification, int page, int size, String sortField, String sortDirection) {
        return wrapped.search(scope, searchTerm, specification, page, size, sortField, sortDirection);
    }

    /**
     * Full-text searches within organization with pagination and sorting.
     *
     * @param searchTerm Text search term applied to searchable fields
     * @param organizationId Organization ID for tenant-scoped search
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortField Field name for sorting
     * @param sortDirection Sort direction ("asc" or "desc")
     * @return Page of entities matching search term within organization and bound scope
     */
    public Page<T> search(String searchTerm, Long organizationId, int page, int size, String sortField, String sortDirection) {
        return wrapped.search(scope, searchTerm, organizationId, page, size, sortField, sortDirection);
    }

    /**
     * Full-text searches within organization with specification and Spring Data Pageable.
     *
     * @param searchTerm Text search term applied to searchable fields
     * @param organizationId Organization ID for tenant-scoped search
     * @param specification JPA specification for additional criteria
     * @param pageable Spring Data pageable containing page, size and sort information
     * @return Page of entities matching all criteria within bound scope
     */
    public Page<T> search(String searchTerm, Long organizationId, Specification<T> specification, Pageable pageable) {
        return wrapped.search(scope, searchTerm, organizationId, specification, pageable);
    }

    /**
     * Full-text searches within organization with specification, Pageable, and frontend filters.
     *
     * @param searchTerm Text search term applied to searchable fields
     * @param organizationId Organization ID for tenant-scoped search
     * @param specification JPA specification for additional criteria
     * @param pageable Spring Data pageable containing page, size and sort information
     * @param filters List of frontend field filters as tuples of (field path, definition, filter value)
     * @return Page of entities matching all criteria and filters within bound scope
     */
    @Override
    public Page<T> search(String searchTerm, Long organizationId, Specification<T> specification, Pageable pageable, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters) {
        return wrapped.search(scope, searchTerm, organizationId, specification, pageable, filters);
    }
    
    /**
     * Full-text searches across multiple organizations with specification, Pageable, and frontend filters.
     *
     * @param searchTerm Text search term applied to searchable fields
     * @param organizationIds Set of organization IDs for multi-tenant search
     * @param specification JPA specification for additional criteria
     * @param pageable Spring Data pageable containing page, size and sort information
     * @param filters List of frontend field filters as tuples of (field path, definition, filter value)
     * @return Page of entities matching all criteria across specified organizations within bound scope
     */
    @Override
    public Page<T> search(String searchTerm, Set<Long> organizationIds, Specification<T> specification, Pageable pageable, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters) {
        return wrapped.search(scope, searchTerm, organizationIds, specification, pageable, filters);
    }

    /**
     * Full-text searches within organization with specification and frontend filters (unpaged).
     *
     * @param searchTerm Text search term applied to searchable fields
     * @param organizationId Organization ID for tenant-scoped search
     * @param specification JPA specification for additional criteria
     * @param filters List of frontend field filters as tuples of (field path, definition, filter value)
     * @return List of all entities matching criteria and filters within bound scope
     */
    @Override
    public List<T> search(String searchTerm, Long organizationId, Specification<T> specification, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters) {
        return wrapped.search(scope, searchTerm, organizationId, specification, filters);
    }

    /**
     * Finds a single entity by ID, entity instance, or specification within the bound security scope.
     * <p>
     * Accepts multiple input types for flexible retrieval. Returns the entity if found and accessible
     * within the bound scope, null otherwise. Write operations with insufficient privilege throw
     * {@code AccessDeniedException}.
     * </p>
     *
     * @param idOrEntityOrSpecification Entity ID (Long), entity instance, or JPA specification
     * @return Entity if found and accessible within scope, null otherwise
     */
    public T findOne(Object idOrEntityOrSpecification) {
        return wrapped.findOne(scope, idOrEntityOrSpecification);
    }

    /**
     * Finds all entities accessible within the bound security scope.
     * <p>
     * Returns all entities the current user can access based on the wrapper's scope level.
     * Use with caution on large datasets - consider paginated search methods instead.
     * </p>
     *
     * @return List of all accessible entities within bound scope, empty list if none found
     */
    public List<T> findAll() {
        return wrapped.findAll(scope);
    }

    /**
     * Saves a single entity with privilege enforcement based on the bound security scope.
     * <p>
     * Persists a new entity or updates an existing one. Write operations require appropriate
     * privileges at the bound scope level. Throws {@code AccessDeniedException} if the scope
     * is insufficient for the write operation.
     * </p>
     *
     * @param <S> Entity subtype extending T
     * @param entity Entity to persist, must not be null
     * @return Saved entity with generated ID and updated fields
     * @throws org.springframework.security.access.AccessDeniedException if scope insufficient for write
     */
    public <S extends T> S saveOne(S entity) {
        return wrapped.saveOne(scope, entity);
    }

    /**
     * Saves entity after populating it from form data with privilege enforcement.
     * <p>
     * Applies form values to the entity via {@code form.populateTo(entity)} before saving.
     * Useful for MVC controller scenarios where form validation has already occurred.
     * </p>
     *
     * @param <S> Entity subtype extending T
     * @param entity Entity to update and save
     * @param form Form containing validated data to apply to entity
     * @return Saved entity with form data applied
     * @throws org.springframework.security.access.AccessDeniedException if scope insufficient for write
     */
    public <S extends T> S saveForm(S entity, AbstractEntityForm form) {
        return wrapped.saveForm(scope, entity, form);
    }

    /**
     * Saves multiple entities in a batch operation with privilege enforcement.
     * <p>
     * Accepts any collection type containing entities. All entities must be accessible
     * within the bound scope for the save operation to succeed.
     * </p>
     *
     * @param <S> Entity subtype extending T
     * @param entitiesCollection Collection (List, Set, etc.) of entities to save
     * @return List of saved entities with generated IDs and updated fields
     * @throws org.springframework.security.access.AccessDeniedException if scope insufficient for write
     */
    public <S extends T> List<S> saveAll(Object entitiesCollection) {
        return wrapped.saveAll(scope, entitiesCollection);
    }

    /**
     * Deletes a single entity by ID or entity instance with privilege enforcement.
     * <p>
     * Accepts either an entity ID (Long) or entity instance. Returns false if entity not found
     * or not accessible within bound scope. Throws {@code AccessDeniedException} if scope
     * insufficient for delete operation.
     * </p>
     *
     * @param idOrEntity Entity ID (Long) or entity instance to delete
     * @return true if entity was deleted, false if not found or not accessible within scope
     * @throws org.springframework.security.access.AccessDeniedException if scope insufficient for delete
     */
    public boolean deleteOne(Object idOrEntity) {
        return wrapped.deleteOne(scope, idOrEntity);
    }

    /**
     * Deletes multiple entities by IDs, collection, or specification with privilege enforcement.
     * <p>
     * Accepts collection of IDs, collection of entities, or JPA specification.
     * All matching entities must be accessible within the bound scope.
     * </p>
     *
     * @param idsOrEntitiesCollectionOrSpecification IDs, entities collection, or specification defining entities to delete
     * @return Count of entities actually deleted
     * @throws org.springframework.security.access.AccessDeniedException if scope insufficient for delete
     */
    public long deleteAll(Object idsOrEntitiesCollectionOrSpecification) {
        return wrapped.deleteAll(scope, idsOrEntitiesCollectionOrSpecification);
    }

    /**
     * Checks if an entity exists by ID or instance within the bound security scope.
     * <p>
     * Returns true only if the entity exists and is accessible within the bound scope level.
     * Entities outside the scope are treated as non-existent.
     * </p>
     *
     * @param idOrEntity Entity ID (Long) or entity instance to check
     * @return true if entity exists and is accessible within scope, false otherwise
     */
    public boolean existsOne(Object idOrEntity) {
        return wrapped.existsOne(scope, idOrEntity);
    }

    /**
     * Checks if any entities exist matching IDs, collection, or specification within bound scope.
     * <p>
     * Returns true if at least one matching entity is accessible within the bound scope.
     * </p>
     *
     * @param idsOrEntitiesCollectionOrSpecification IDs, entities collection, or specification to check
     * @return true if at least one matching entity exists within scope, false otherwise
     */
    public boolean existsAny(Object idsOrEntitiesCollectionOrSpecification) {
        return wrapped.existsAny(scope, idsOrEntitiesCollectionOrSpecification);
    }

    /**
     * Counts all entities accessible within the bound security scope.
     * <p>
     * Returns the count of entities the current user can access based on the wrapper's scope level.
     * </p>
     *
     * @return Count of entities accessible within bound scope
     */
    public long count() {
        return wrapped.count(scope);
    }

    /**
     * Counts entities matching specified field and value within bound scope.
     *
     * @param fieldName Field name to match
     * @param value Value to match, null counts null values
     * @return Count of matching entities accessible within bound scope
     */
    public long count(String fieldName, Object value) {
        return wrapped.count(scope, fieldName, value);
    }

    /**
     * Counts entities matching JPA specification within bound scope.
     *
     * @param specification JPA specification defining criteria to count
     * @return Count of entities matching specification within bound scope
     */
    public long count(Specification<T> specification) {
        return wrapped.count(scope, specification);
    }

    /**
     * Counts entities within organization matching specification and bound scope.
     *
     * @param organizationId Organization ID for tenant-scoped count
     * @param specification JPA specification defining criteria to count
     * @return Count of entities matching criteria within organization and bound scope
     */
    public long count(Long organizationId, Specification<T> specification) {
        return wrapped.count(scope, organizationId, specification);
    }

    /**
     * Creates a new uninitialized entity instance of the repository's entity type.
     * <p>
     * Delegates to the wrapped repository's factory method. Useful for creating
     * entity instances without direct constructor invocation.
     * </p>
     *
     * @return New entity instance, never null
     */
    @Override
    public T getNew() {
        return wrapped.getNew();
    }

    /**
     * Extracts {@link SearchableRepositoryMetadata} annotation from the wrapped repository interface.
     * <p>
     * This method introspects the wrapped repository's class to find the {@code SearchableRepositoryMetadata}
     * annotation on the first implemented interface. The metadata contains configuration for search operations,
     * including searchable field definitions and display properties.
     * </p>
     * <p>
     * Implementation note: Reads annotation from the first interface of the wrapped repository class.
     * Returns null if the wrapped repository has no annotated interfaces.
     * </p>
     *
     * @return Metadata annotation if present on wrapped repository's first interface, null otherwise
     */
    @Override
    public SearchableRepositoryMetadata getSearchableRepositoryMetadata() {
        Class<?>[] interfaces = wrapped.getClass().getInterfaces();
        if (interfaces == null || interfaces.length == 0) { return null; }
        SearchableRepositoryMetadata gsa = interfaces[0].getAnnotation(SearchableRepositoryMetadata.class);
        return gsa;
    }

    /**
     * Checks if the wrapped repository reference is non-null.
     * <p>
     * Utility method for validating that this wrapper has been properly initialized
     * with a repository instance.
     * </p>
     *
     * @return true if wrapped repository is non-null, false otherwise
     */
    public boolean isSet() {
        return this.wrapped != null;
    }
}
