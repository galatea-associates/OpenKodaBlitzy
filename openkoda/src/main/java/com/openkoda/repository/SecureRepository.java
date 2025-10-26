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
import com.openkoda.core.repository.common.SearchableFunctionalRepositoryWithLongId;
import com.openkoda.model.common.SearchableEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;
import reactor.util.function.Tuple3;

import java.util.List;
import java.util.Set;

import static com.openkoda.core.security.HasSecurityRules.SecurityScope.USER;

/**
 * Repository wrapper interface adding privilege-based access control to standard Spring Data operations for searchable entities.
 * <p>
 * This interface extends {@link SearchableFunctionalRepositoryWithLongId} to inherit functional repository contracts
 * and provides secure access to entity operations. It is annotated with {@link NoRepositoryBean} to prevent
 * Spring Data from creating a proxy implementation, as this interface serves as a contract for secure repository wrappers.
 * </p>
 * <p>
 * All default methods inject {@link #DEFAULT_SCOPE} (USER) into underlying repository calls, enforcing
 * privilege verification before executing CRUD and search operations. Privilege requirements are computed
 * from entity @Formula annotations (requiredReadPrivilege, requiredWritePrivilege). Method calls throw
 * AccessDeniedException if the user lacks the required privilege.
 * </p>
 * <p>
 * The {@link #scoped(SecurityScope)} method provides temporary scope override capability, allowing operations
 * to be executed with ORGANIZATION or GLOBAL scope when needed.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * SecureRepository<Organization> secureRepo = secureRepositories.organization;
 * Optional<Organization> org = secureRepo.findOne(orgId); // privilege-checked
 * }</pre>
 * </p>
 *
 * @param <T> Searchable entity type with privilege annotations, must extend {@link SearchableEntity}
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SearchableFunctionalRepositoryWithLongId
 * @see com.openkoda.repository.SecureRepositoryWrapper
 * @see SearchableEntity
 * @see com.openkoda.core.security.HasSecurityRules
 */
@NoRepositoryBean
public interface SecureRepository<T extends SearchableEntity> extends SearchableFunctionalRepositoryWithLongId<T> {

    /**
     * Default security scope USER applied to all repository operations unless overridden via {@link #scoped(SecurityScope)} wrapper.
     */
    SecurityScope DEFAULT_SCOPE = USER;
    
    /**
     * Searches for entities matching the specified field name and value with USER privilege check.
     *
     * @param fieldName Field name to search
     * @param value Value to match
     * @return List of entities matching criteria with USER privilege, empty list if none found or access denied
     */
    default List<T> search(String fieldName, Object value) {
        return SearchableFunctionalRepositoryWithLongId.super.search(DEFAULT_SCOPE, fieldName, value);
    }

    /**
     * Searches for entities matching the JPA Specification with USER privilege check.
     *
     * @param specification JPA Specification for dynamic query construction
     * @return List of entities matching specification with privilege check, empty list if none accessible
     */
    default List<T> search(Specification<T> specification) {
        return SearchableFunctionalRepositoryWithLongId.super.search(DEFAULT_SCOPE, specification);
    }

    /**
     * Searches for entities within the specified organization scope with USER privilege check.
     *
     * @param organizationId Organization scope for multi-tenant queries
     * @return List of organization-scoped entities accessible to user, empty list if none found
     */
    default List<T> search(Long organizationId) {
        return SearchableFunctionalRepositoryWithLongId.super.search(DEFAULT_SCOPE, organizationId);
    }

    /**
     * Searches for entities within organization scope matching the specification with USER privilege check.
     *
     * @param organizationId Organization scope for tenant isolation
     * @param specification JPA Specification for additional filtering
     * @return List of matching organization-scoped entities accessible to user, empty list if none found
     */
    default List<T> search(Long organizationId, Specification<T> specification) {
        return SearchableFunctionalRepositoryWithLongId.super.search(DEFAULT_SCOPE, organizationId, specification);
    }

    
    /**
     * Searches for entities with pagination and sorting, applying USER privilege check.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param sortField Field name for sorting
     * @param sortDirection Sort direction (ASC or DESC)
     * @return Page containing entities with totalElements and content accessible to user
     */
    default Page<T> search(int page, int size, String sortField, String sortDirection) {
        return SearchableFunctionalRepositoryWithLongId.super.search(DEFAULT_SCOPE, page, size, sortField, sortDirection);
    }

    
    /**
     * Searches for entities matching specification with pagination and sorting, applying USER privilege check.
     *
     * @param specification JPA Specification for query filtering
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param sortField Field name for sorting
     * @param sortDirection Sort direction (ASC or DESC)
     * @return Page containing matching entities with totalElements and content accessible to user
     */
    default Page<T> search(Specification<T> specification, int page, int size, String sortField, String sortDirection) {
        return SearchableFunctionalRepositoryWithLongId.super.search(DEFAULT_SCOPE, specification, page, size, sortField, sortDirection);
    }

    
    /**
     * Searches for organization-scoped entities matching specification with pagination and sorting.
     *
     * @param organizationId Organization scope for tenant isolation
     * @param specification JPA Specification for additional filtering
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param sortField Field name for sorting
     * @param sortDirection Sort direction (ASC or DESC)
     * @return Page containing matching organization-scoped entities accessible to user
     */
    default Page<T> search(Long organizationId, Specification<T> specification, int page, int size, String sortField, String sortDirection) {
        return SearchableFunctionalRepositoryWithLongId.super.search(DEFAULT_SCOPE, organizationId, specification, page, size, sortField, sortDirection);
    }

    
    /**
     * Searches for entities using full-text search term with pagination and sorting.
     *
     * @param searchTerm Full-text search term for indexString matching
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param sortField Field name for sorting
     * @param sortDirection Sort direction (ASC or DESC)
     * @return Page containing entities matching search term accessible to user
     */
    default Page<T> search(String searchTerm, int page, int size, String sortField, String sortDirection) {
        return SearchableFunctionalRepositoryWithLongId.super.search(DEFAULT_SCOPE, searchTerm, page, size, sortField, sortDirection);
    }

    
    /**
     * Searches for entities using full-text search with specification, pagination, and sorting.
     *
     * @param searchTerm Full-text search term for indexString matching
     * @param specification JPA Specification for additional filtering
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param sortField Field name for sorting
     * @param sortDirection Sort direction (ASC or DESC)
     * @return Page containing entities matching search term and specification accessible to user
     */
    default Page<T> search(String searchTerm, Specification<T> specification, int page, int size, String sortField, String sortDirection) {
        return SearchableFunctionalRepositoryWithLongId.super.search(DEFAULT_SCOPE, searchTerm, specification, page, size, sortField, sortDirection);
    }

    
    /**
     * Searches for organization-scoped entities using full-text search with pagination and sorting.
     *
     * @param searchTerm Full-text search term for indexString matching
     * @param organizationId Organization scope for tenant isolation
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param sortField Field name for sorting
     * @param sortDirection Sort direction (ASC or DESC)
     * @return Page containing matching organization-scoped entities accessible to user
     */
    default Page<T> search(String searchTerm, Long organizationId, int page, int size, String sortField, String sortDirection) {
        return SearchableFunctionalRepositoryWithLongId.super.search(DEFAULT_SCOPE, searchTerm, organizationId, page, size, sortField, sortDirection);
    }

    
    /**
     * Searches for organization-scoped entities using full-text search with specification and Spring Data Pageable.
     *
     * @param searchTerm Full-text search term for indexString matching
     * @param organizationId Organization scope for tenant isolation
     * @param specification JPA Specification for additional filtering
     * @param pageable Spring Data Pageable for pagination and sorting configuration
     * @return Page containing matching organization-scoped entities accessible to user
     */
    default Page<T> search(String searchTerm, Long organizationId, Specification<T> specification, Pageable pageable) {
        return SearchableFunctionalRepositoryWithLongId.super.search(DEFAULT_SCOPE, searchTerm, organizationId, specification, pageable);
    }

    /**
     * Searches for organization-scoped entities with full-text search, specification, pagination, and frontend filters.
     *
     * @param searchTerm Full-text search term for indexString matching
     * @param organizationId Organization scope for tenant isolation
     * @param specification JPA Specification for additional filtering
     * @param pageable Spring Data Pageable for pagination and sorting
     * @param filters List of frontend mapping field filters as Tuple3 (field name, field definition, filter value)
     * @return Page containing matching organization-scoped entities with applied filters accessible to user
     */
    @Override
    default Page<T> search(String searchTerm, Long organizationId, Specification<T> specification, Pageable pageable, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters) {
        return SearchableFunctionalRepositoryWithLongId.super.search(DEFAULT_SCOPE, searchTerm, organizationId, specification, pageable, filters);
    }
    
    /**
     * Searches for entities across multiple organizations with full-text search, specification, pagination, and filters.
     *
     * @param searchTerm Full-text search term for indexString matching
     * @param organizationIds Set of organization IDs for multi-tenant queries
     * @param specification JPA Specification for additional filtering
     * @param pageable Spring Data Pageable for pagination and sorting
     * @param filters List of frontend mapping field filters as Tuple3 (field name, field definition, filter value)
     * @return Page containing matching entities from specified organizations accessible to user
     */
    @Override
    default Page<T> search(String searchTerm, Set<Long> organizationIds, Specification<T> specification,
            Pageable pageable, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters) {
        return SearchableFunctionalRepositoryWithLongId.super.search(DEFAULT_SCOPE, searchTerm, organizationIds, specification, pageable, filters);
    }

    /**
     * Searches for organization-scoped entities with full-text search, specification, and frontend filters without pagination.
     *
     * @param searchTerm Full-text search term for indexString matching
     * @param organizationId Organization scope for tenant isolation
     * @param specification JPA Specification for additional filtering
     * @param filters List of frontend mapping field filters as Tuple3 (field name, field definition, filter value)
     * @return List of matching organization-scoped entities with applied filters accessible to user
     */
    default List<T> search(String searchTerm, Long organizationId, Specification<T> specification, List<Tuple3<String, FrontendMappingFieldDefinition, String>> filters) {
        return SearchableFunctionalRepositoryWithLongId.super.search(DEFAULT_SCOPE, searchTerm, organizationId, specification, filters);
    }

    /**
     * Finds a single entity by ID, entity instance, or specification with USER privilege check.
     *
     * @param idOrEntityOrSpecification Entity ID (Long), entity instance, or JPA Specification
     * @return Entity if found and user has read privilege, null if not found or access denied
     */
    default T findOne(Object idOrEntityOrSpecification) {
        return SearchableFunctionalRepositoryWithLongId.super.findOne(DEFAULT_SCOPE, idOrEntityOrSpecification);
    }

    
    /**
     * Retrieves all entities that the user has read privilege for.
     *
     * @return List of all entities accessible to user, empty list if none accessible
     */
    default List<T> findAll() {
        return SearchableFunctionalRepositoryWithLongId.super.findAll(DEFAULT_SCOPE);
    }

    
    /**
     * Saves a single entity with USER privilege check.
     *
     * @param <S> Entity type extending T
     * @param entity Entity to persist
     * @return Saved entity after persistence
     * @throws org.springframework.security.access.AccessDeniedException if user lacks write privilege
     */
    default <S extends T> S saveOne(S entity) {
        return SearchableFunctionalRepositoryWithLongId.super.saveOne(DEFAULT_SCOPE, entity);
    }

    
    /**
     * Saves an entity after populating it from a form with USER privilege check.
     *
     * @param <S> Entity type extending T
     * @param entity Target entity to update
     * @param form Form containing updates to apply via populateTo
     * @return Updated entity after form population and persistence
     * @throws org.springframework.security.access.AccessDeniedException if user lacks write privilege
     */
    default <S extends T> S saveForm(S entity, AbstractEntityForm form) {
        return SearchableFunctionalRepositoryWithLongId.super.saveForm(DEFAULT_SCOPE, entity, form);
    }

    
    /**
     * Saves multiple entities with USER privilege check.
     *
     * @param <S> Entity type extending T
     * @param entitiesCollection Collection or array of entities to persist
     * @return List of saved entities after persistence
     * @throws org.springframework.security.access.AccessDeniedException if user lacks write privilege for any entity
     */
    default <S extends T> List<S> saveAll(Object entitiesCollection) {
        return SearchableFunctionalRepositoryWithLongId.super.saveAll(DEFAULT_SCOPE, entitiesCollection);
    }

    
    /**
     * Deletes a single entity by ID or entity instance with USER privilege check.
     *
     * @param idOrEntity Entity ID (Long) or entity instance to delete
     * @return true if entity was deleted, false if not found or access denied
     * @throws org.springframework.security.access.AccessDeniedException if user lacks write privilege
     */
    default boolean deleteOne(Object idOrEntity) {
        return SearchableFunctionalRepositoryWithLongId.super.deleteOne(DEFAULT_SCOPE, idOrEntity);
    }

    
    /**
     * Deletes multiple entities by IDs, entity instances, or specification with USER privilege check.
     *
     * @param idsOrEntitiesCollectionOrSpecification Collection of IDs, collection of entities, or JPA Specification
     * @return Count of deleted entities
     * @throws org.springframework.security.access.AccessDeniedException if user lacks write privilege
     */
    default long deleteAll(Object idsOrEntitiesCollectionOrSpecification) {
        return SearchableFunctionalRepositoryWithLongId.super.deleteAll(DEFAULT_SCOPE, idsOrEntitiesCollectionOrSpecification);
    }

    
    /**
     * Checks if an entity exists by ID or entity instance with USER privilege check.
     *
     * @param idOrEntity Entity ID (Long) or entity instance
     * @return true if entity exists and is accessible to user, false if not found or access denied
     */
    default boolean existsOne(Object idOrEntity) {
        return SearchableFunctionalRepositoryWithLongId.super.existsOne(DEFAULT_SCOPE, idOrEntity);
    }

    
    /**
     * Checks if any entities exist matching IDs, entity instances, or specification with USER privilege check.
     *
     * @param idsOrEntitiesCollectionOrSpecification Collection of IDs, collection of entities, or JPA Specification
     * @return true if any matching entities are accessible to user, false if none found or access denied
     */
    default boolean existsAny(Object idsOrEntitiesCollectionOrSpecification) {
        return SearchableFunctionalRepositoryWithLongId.super.existsAny(DEFAULT_SCOPE, idsOrEntitiesCollectionOrSpecification);
    }

    
    /**
     * Counts total number of entities that the user has read privilege for.
     *
     * @return Total count of entities accessible to user
     */
    default long count() {
        return SearchableFunctionalRepositoryWithLongId.super.count(DEFAULT_SCOPE);
    }

    
    /**
     * Counts entities matching the specified field name and value with USER privilege check.
     *
     * @param fieldName Field name to match
     * @param value Field value to match
     * @return Count of matching entities accessible to user
     */
    default long count(String fieldName, Object value) {
        return SearchableFunctionalRepositoryWithLongId.super.count(DEFAULT_SCOPE, fieldName, value);
    }

    /**
     * Counts organization-scoped entities matching the specification with USER privilege check.
     *
     * @param organizationId Organization scope for tenant isolation
     * @param specification JPA Specification for filtering
     * @return Count of matching organization-scoped entities accessible to user
     */
    default long count(Long organizationId, Specification<T> specification) {
        return SearchableFunctionalRepositoryWithLongId.super.count(DEFAULT_SCOPE, organizationId, specification);
    }

    /**
     * Creates a wrapper applying the specified security scope to all repository operations.
     * <p>
     * This method allows temporary override of the default USER scope for operations requiring
     * ORGANIZATION or GLOBAL scope access.
     * </p>
     *
     * @param scope Security scope override (USER, ORGANIZATION, or GLOBAL)
     * @return SecureRepositoryWrapper applying the specified scope to all subsequent operations
     */
    default SecureRepositoryWrapper<T> scoped(SecurityScope scope) {
        return new SecureRepositoryWrapper<>(this, scope);
    }

}
