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

package com.openkoda.service.user;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.DynamicPrivilege;
import com.openkoda.model.PrivilegeGroup;
import com.openkoda.repository.user.DynamicPrivilegeRepository;
import jakarta.inject.Inject;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Optional;

/**
 * Cache-aware service for managing dynamic privilege entities with Spring Cache integration.
 * <p>
 * This service provides CRUD operations for {@link DynamicPrivilege} entities with automatic
 * caching to improve performance. It should be used instead of direct {@link DynamicPrivilegeRepository}
 * access to leverage the caching mechanism. The service uses Spring's {@code @Cacheable}, {@code @CachePut},
 * and {@code @CacheEvict} annotations to manage three separate caches for optimized lookups.
 * </p>
 * <p>
 * Cache Strategy:
 * </p>
 * <ul>
 *   <li>{@link #DYNAMIC_PRIVILEGES_ALL_CACHE} - Caches aggregate queries (findAll operations)</li>
 *   <li>{@link #DYNAMIC_PRIVILEGES_NAME_CACHE} - Caches lookups by privilege name</li>
 *   <li>{@link #DYNAMIC_PRIVILEGES_CACHE} - Caches lookups by privilege ID</li>
 * </ul>
 * <p>
 * Self-Proxy Pattern: This service uses a self-reference ({@code self}) to enable internal method calls
 * to route through the Spring proxy, activating AOP annotations like {@code @Cacheable}. Internal calls
 * should use {@code self.methodName()} to ensure cache behavior is applied.
 * </p>
 * <p>
 * Profile Configuration: This service is excluded from the development profile ({@code @Profile("!development")})
 * to allow different behavior during development.
 * </p>
 * <p>
 * Event Publishing: The service publishes {@link PrivilegeChangeEvent} when privileges are modified,
 * allowing other components to react to privilege changes.
 * </p>
 * <p>
 * Extension Design: Many methods throw {@link NotImplementedException} with message "no implementation in core Openkoda".
 * These are intended extension points for custom implementations in derived projects.
 * </p>
 *
 * @author mboronski
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.0.0
 * @see DynamicPrivilege
 * @see DynamicPrivilegeRepository
 * @see org.springframework.cache.annotation.Cacheable
 */

@Service
@Profile("!development")
public class BasicPrivilegeService extends ComponentProvider implements HasSecurityRules {

    /**
     * Cache name for storing aggregate privilege queries (findAll operations).
     * Used for caching complete lists and paginated results of dynamic privileges.
     * Evicted when any privilege is saved to maintain consistency.
     */
    protected static final String DYNAMIC_PRIVILEGES_ALL_CACHE = "dynamicPrivilegesAll";
    
    /**
     * Cache name for storing privilege lookups by name.
     * Provides fast access to privileges using their unique name as the key.
     * Updated via {@code @CachePut} on save operations.
     */
    protected static final String DYNAMIC_PRIVILEGES_NAME_CACHE = "dynamicPrivilegesName";
    
    /**
     * Cache name for storing privilege lookups by ID.
     * Provides fast access to privileges using their primary key.
     * Updated via {@code @CachePut} on save operations.
     */
    protected static final String DYNAMIC_PRIVILEGES_CACHE = "dynamicPrivileges";
    
    /**
     * Standard message for methods not implemented in core OpenKoda.
     * These methods are extension points for custom implementations.
     */
    protected static final String NO_IMPLEMENTATION_IN_CORE_OPENKODA = "no implementation in core Openkoda";
    
    /** Event publisher for notifying system components of privilege changes. */
    @Inject protected ApplicationEventPublisher applicationEventPublisher;
    
    /** Repository for direct database access to dynamic privilege entities. */
    @Inject protected DynamicPrivilegeRepository dynamicPrivilegeRepository;

    /**
     * Self-reference to this service bean for enabling cache behavior on internal method calls.
     * Internal calls should use {@code self.methodName()} to route through the Spring proxy
     * and activate AOP annotations such as {@code @Cacheable}, {@code @CachePut}, and {@code @CacheEvict}.
     * Direct calls (this.methodName()) bypass the proxy and disable caching.
     */
    @Inject protected BasicPrivilegeService self;
    
    /**
     * Application event published when dynamic privileges are created, updated, or deleted.
     * <p>
     * Listener components can subscribe to this event to react to privilege changes,
     * such as refreshing caches, updating security configurations, or auditing changes.
     * The event is published via {@link ApplicationEventPublisher} after privilege modifications.
     * </p>
     * <p>
     * Usage example:
     * <pre>
     * {@code @EventListener}
     * public void handlePrivilegeChange(PrivilegeChangeEvent event) {
     *     // React to privilege changes
     * }
     * </pre>
     * </p>
     *
     * @see ApplicationEvent
     * @see ApplicationEventPublisher
     * @since 1.0.0
     */
    public static class PrivilegeChangeEvent extends ApplicationEvent {

        private static final long serialVersionUID = -8745580408895611463L;

        /**
         * Constructs a new PrivilegeChangeEvent.
         *
         * @param source the component that published the event (typically the service instance)
         */
        public PrivilegeChangeEvent(Object source) {
            super(source);
            
        }
    }
    
    /**
     * Creates or updates a dynamic privilege with the specified attributes.
     * <p>
     * Extension point for custom implementations. Core OpenKoda does not provide
     * an implementation for this method signature.
     * </p>
     *
     * @param id the privilege ID for updates, or null for new privileges
     * @param name the unique privilege name (must follow naming conventions)
     * @param label the human-readable privilege label
     * @param category the privilege category for grouping
     * @param group the {@link PrivilegeGroup} this privilege belongs to
     * @param removable whether this privilege can be deleted by users
     * @return the created or updated {@link DynamicPrivilege}
     * @throws NotImplementedException always thrown in core OpenKoda (extension point)
     * @see #createOrUpdateDynamicPrivilege(DynamicPrivilege)
     */
    public DynamicPrivilege createOrUpdateDynamicPrivilege(Long id, String name, String label, String category, PrivilegeGroup group, boolean removable) {
        throw new NotImplementedException(NO_IMPLEMENTATION_IN_CORE_OPENKODA);
    }
    
    /**
     * Creates a new dynamic privilege with the specified attributes.
     * <p>
     * Extension point for custom implementations. Core OpenKoda does not provide
     * an implementation for this method. Use {@link #createOrUpdateDynamicPrivilege(DynamicPrivilege)}
     * for the implemented variant.
     * </p>
     *
     * @param name the unique privilege name (e.g., "canReadReports")
     * @param label the human-readable privilege label displayed in UI
     * @param category the privilege category for organizational grouping
     * @param group the {@link PrivilegeGroup} this privilege belongs to
     * @return the newly created {@link DynamicPrivilege}
     * @throws NotImplementedException always thrown in core OpenKoda (extension point)
     * @see #createOrUpdateDynamicPrivilege(DynamicPrivilege)
     */
    public DynamicPrivilege createPrivilege(String name, String label, String category, PrivilegeGroup group) {
        throw new NotImplementedException(NO_IMPLEMENTATION_IN_CORE_OPENKODA);
    }
    
    /**
     * Creates a new dynamic privilege or updates an existing one based on the privilege name.
     * <p>
     * This method uses the self-proxy pattern to ensure cache behavior is applied. It first checks
     * if a privilege with the given name exists using {@link #findByName(String)} (cache-aware).
     * If found, it updates the existing privilege; otherwise, it creates a new one. After saving,
     * a {@link PrivilegeChangeEvent} is published to notify listeners of the change.
     * </p>
     * <p>
     * Security: Requires {@code CHECK_CAN_MANAGE_ROLES} permission via {@code @PreAuthorize}.
     * </p>
     * <p>
     * Cache Behavior: Uses {@code self.findByName()} and {@code self.save()} to ensure caching
     * annotations are activated. The save operation updates both ID and name caches and evicts
     * the aggregate cache.
     * </p>
     *
     * @param privilege the privilege entity containing the data (name must be unique)
     * @return the created or updated {@link DynamicPrivilege} with database-assigned ID
     * @throws org.springframework.security.access.AccessDeniedException if user lacks permission
     * @see #save(DynamicPrivilege)
     * @see #findByName(String)
     * @see PrivilegeChangeEvent
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ROLES)
    public DynamicPrivilege createOrUpdateDynamicPrivilege(DynamicPrivilege privilege) {
        debug("[createOrUpdateDynamicPrivilege] Creating or updating privilege {} ", privilege.toString());
        DynamicPrivilege newPrivilege = self.findByName(privilege.getName());
        if (newPrivilege == null) {
            newPrivilege = new DynamicPrivilege();
            newPrivilege.setId(privilege.getId());
        }

        newPrivilege.setName(privilege.getName());
        newPrivilege.setRemovable(privilege.getRemovable());
        newPrivilege.setGroup(privilege.getGroup());
        newPrivilege.setLabel(privilege.getLabel());
        newPrivilege.setCategory(privilege.getCategory());
        newPrivilege = self.save(newPrivilege);
        
        applicationEventPublisher.publishEvent(new PrivilegeChangeEvent(this));
        return newPrivilege;
    }

    /**
     * Validates whether a privilege with the given name already exists in the database.
     * <p>
     * Extension point for custom implementations. Typically used in form validation
     * to prevent duplicate privilege names. If a duplicate is found, validation errors
     * should be added to the provided {@link BindingResult}.
     * </p>
     *
     * @param name the privilege name to check for uniqueness
     * @param br the {@link BindingResult} to add validation errors if duplicate exists
     * @return {@code true} if a privilege with this name exists, {@code false} otherwise
     * @throws NotImplementedException always thrown in core OpenKoda (extension point)
     * @see #findByName(String)
     */
    public boolean checkIfPrivilegeNameAlreadyExists(String name, BindingResult br) {
        throw new NotImplementedException(NO_IMPLEMENTATION_IN_CORE_OPENKODA);
    }

    /**
     * Publishes a {@link PrivilegeChangeEvent} to notify system components of privilege modifications.
     * <p>
     * This method can be called to manually trigger privilege change notifications when privileges
     * are modified through means other than the standard service methods. Listeners can subscribe
     * to {@link PrivilegeChangeEvent} to react to privilege changes by refreshing caches or
     * updating security configurations.
     * </p>
     *
     * @return {@code null} (return value not used, method may be invoked in Flow pipelines)
     * @see PrivilegeChangeEvent
     * @see ApplicationEventPublisher
     */
    public Object notifyOnPrivilagesChange() {
        debug("[notifyOnPrivilagesChange] Privileges have changed, notifying");
        applicationEventPublisher.publishEvent(new PrivilegeChangeEvent(this));
        return null;
    }
    /**
     * Saves a dynamic privilege entity to the database with cache updates.
     * <p>
     * Cache Behavior: This method uses {@code @Caching} with multiple operations:
     * </p>
     * <ul>
     *   <li>{@code @CachePut} to {@link #DYNAMIC_PRIVILEGES_CACHE} using the entity ID as key</li>
     *   <li>{@code @CachePut} to {@link #DYNAMIC_PRIVILEGES_NAME_CACHE} using the entity name as key</li>
     *   <li>{@code @CacheEvict} to clear all entries in {@link #DYNAMIC_PRIVILEGES_ALL_CACHE} (aggregate queries)</li>
     * </ul>
     * <p>
     * This ensures that both ID-based and name-based lookups return the latest data, while aggregate
     * queries are invalidated and will be recomputed on next access.
     * </p>
     * <p>
     * Note: This method should be called via the self-proxy ({@code self.save()}) from other methods
     * in this class to activate cache behavior.
     * </p>
     *
     * @param <S> the type extending {@link DynamicPrivilege}
     * @param entity the privilege entity to save (may be new or existing)
     * @return the saved entity with database-assigned ID (if new) and updated fields
     * @see DynamicPrivilegeRepository#save(Object)
     */
    @Caching(put = {
            @CachePut(cacheNames = {DYNAMIC_PRIVILEGES_CACHE}, key = "#result.getId()"),
            @CachePut(cacheNames = {DYNAMIC_PRIVILEGES_NAME_CACHE}, key = "#result.getName()"),
    }, evict = { @CacheEvict(cacheNames = {DYNAMIC_PRIVILEGES_ALL_CACHE}, allEntries = true) })
    public <S extends DynamicPrivilege> S save(S entity) {
        entity = dynamicPrivilegeRepository.save(entity);
        return entity;
    }
    
    /**
     * Deletes a dynamic privilege by its ID.
     * <p>
     * Extension point for custom implementations. Implementations should evict relevant caches
     * and publish a {@link PrivilegeChangeEvent} after deletion.
     * </p>
     *
     * @param privilegeId the ID of the privilege to delete
     * @return the deleted {@link DynamicPrivilege} entity
     * @throws NotImplementedException always thrown in core OpenKoda (extension point)
     * @see #delete(DynamicPrivilege)
     */
    public DynamicPrivilege deletePrivilege(long privilegeId) {
        throw new NotImplementedException(NO_IMPLEMENTATION_IN_CORE_OPENKODA);
    }

    /**
     * Updates an existing dynamic privilege with new attribute values.
     * <p>
     * Extension point for custom implementations. Implementations should load the privilege,
     * update its fields, save using {@link #save(DynamicPrivilege)}, and return the result.
     * </p>
     *
     * @param privilegeId the ID of the privilege to update
     * @param name the new privilege name
     * @param label the new privilege label
     * @param category the new privilege category
     * @param privilegeGroup the new {@link PrivilegeGroup}
     * @return the updated privilege entity or operation result
     * @throws NotImplementedException always thrown in core OpenKoda (extension point)
     * @see #createOrUpdateDynamicPrivilege(DynamicPrivilege)
     */
    public Object updatePrivilege(long privilegeId, String name, String label, String category, PrivilegeGroup privilegeGroup) {
        throw new NotImplementedException(NO_IMPLEMENTATION_IN_CORE_OPENKODA);
    }
    
    /**
     * Deletes a dynamic privilege entity from the database.
     * <p>
     * Extension point for custom implementations. Implementations should evict relevant caches
     * using {@code @CacheEvict} annotations and publish a {@link PrivilegeChangeEvent}.
     * </p>
     *
     * @param <S> the type extending {@link DynamicPrivilege}
     * @param entity the privilege entity to delete
     * @throws NotImplementedException always thrown in core OpenKoda (extension point)
     * @see DynamicPrivilegeRepository#delete(Object)
     */
    public <S extends DynamicPrivilege> void delete(S entity) {
        throw new NotImplementedException(NO_IMPLEMENTATION_IN_CORE_OPENKODA);
    }

    /**
     * Saves multiple dynamic privilege entities in a batch operation.
     * <p>
     * Extension point for custom implementations. Implementations should iterate and save
     * each entity using {@link #save(DynamicPrivilege)} to ensure cache updates, or use
     * repository batch save with manual cache eviction.
     * </p>
     *
     * @param <S> the type extending {@link DynamicPrivilege}
     * @param entities the collection of privilege entities to save
     * @return the list of saved entities with updated IDs
     * @throws NotImplementedException always thrown in core OpenKoda (extension point)
     * @see #save(DynamicPrivilege)
     */
    public <S extends DynamicPrivilege> List<S> saveAll(Iterable<S> entities) {
        throw new NotImplementedException(NO_IMPLEMENTATION_IN_CORE_OPENKODA);
    }

    /**
     * Saves a dynamic privilege entity and immediately flushes changes to the database.
     * <p>
     * Extension point for custom implementations. Similar to {@link #save(DynamicPrivilege)}
     * but forces immediate persistence without waiting for transaction commit.
     * </p>
     *
     * @param <S> the type extending {@link DynamicPrivilege}
     * @param entity the privilege entity to save and flush
     * @return the saved entity with updated fields
     * @throws NotImplementedException always thrown in core OpenKoda (extension point)
     * @see #save(DynamicPrivilege)
     */
    public <S extends DynamicPrivilege> S saveAndFlush(S entity) {
        throw new NotImplementedException(NO_IMPLEMENTATION_IN_CORE_OPENKODA);
    }

    /**
     * Saves multiple dynamic privilege entities and immediately flushes changes to the database.
     * <p>
     * Extension point for custom implementations. Combines batch save with immediate flush.
     * </p>
     *
     * @param <S> the type extending {@link DynamicPrivilege}
     * @param entities the collection of privilege entities to save and flush
     * @return the list of saved entities with updated IDs
     * @throws NotImplementedException always thrown in core OpenKoda (extension point)
     * @see #saveAll(Iterable)
     */
    public <S extends DynamicPrivilege> List<S> saveAllAndFlush(Iterable<S> entities) {
        throw new NotImplementedException(NO_IMPLEMENTATION_IN_CORE_OPENKODA);
    }

    /**
     * Deletes a dynamic privilege by its ID and returns the count of deleted records.
     * <p>
     * Extension point for custom implementations. Typically returns 1 if deleted, 0 if not found.
     * </p>
     *
     * @param aLong the ID of the privilege to delete
     * @return the number of privileges deleted (0 or 1)
     * @throws NotImplementedException always thrown in core OpenKoda (extension point)
     * @see #deletePrivilege(long)
     */
    public int deletePrivilege(Long aLong) {
        throw new NotImplementedException(NO_IMPLEMENTATION_IN_CORE_OPENKODA);
    }
    /**
     * Retrieves all dynamic privileges from the database with custom sorting.
     * <p>
     * Cache Behavior: Results are cached in {@link #DYNAMIC_PRIVILEGES_ALL_CACHE} unless the
     * result is {@code null}. The cache is evicted when any privilege is saved via {@link #save(DynamicPrivilege)}.
     * </p>
     *
     * @param sort the {@link Sort} specification defining sort order (e.g., by name, by ID)
     * @return a list of all {@link DynamicPrivilege} entities in the specified order
     * @see DynamicPrivilegeRepository#findAll(Sort)
     */
    @Cacheable(cacheNames = DYNAMIC_PRIVILEGES_ALL_CACHE, unless = "#result == null")
    public List<DynamicPrivilege> findAll(Sort sort) {
        return dynamicPrivilegeRepository.findAll(sort);
    }

    /**
     * Retrieves a page of dynamic privileges from the database.
     * <p>
     * Cache Behavior: Results are cached in {@link #DYNAMIC_PRIVILEGES_ALL_CACHE} unless the
     * result is {@code null}. Useful for paginated UI displays of privileges.
     * </p>
     *
     * @param pageable the {@link Pageable} specification (page number, size, sort)
     * @return a {@link Page} containing the requested slice of {@link DynamicPrivilege} entities
     * @see DynamicPrivilegeRepository#findAll(Pageable)
     */
    @Cacheable(cacheNames = DYNAMIC_PRIVILEGES_ALL_CACHE, unless = "#result == null")
    public Page<DynamicPrivilege> findAll(Pageable pageable) {
        return dynamicPrivilegeRepository.findAll(pageable);
    }
    
    /**
     * Finds a dynamic privilege by its unique name.
     * <p>
     * Cache Behavior: Results are cached in {@link #DYNAMIC_PRIVILEGES_NAME_CACHE} using the
     * name as the key (SpEL expression {@code #p0}). Returns {@code null} if not found, and
     * {@code null} results are not cached ({@code unless = "#result == null"}).
     * </p>
     * <p>
     * This method is frequently used for privilege lookups by name and benefits significantly
     * from caching. Updated automatically via {@code @CachePut} when privileges are saved.
     * </p>
     *
     * @param name the unique privilege name to search for
     * @return the {@link DynamicPrivilege} with the specified name, or {@code null} if not found
     * @see DynamicPrivilegeRepository#findByName(String)
     */
    @Cacheable(cacheNames = DYNAMIC_PRIVILEGES_NAME_CACHE, key = "#p0", unless = "#result == null")
    public DynamicPrivilege findByName(String name) {
        return dynamicPrivilegeRepository.findByName(name);
    }
    
    /**
     * Retrieves all dynamic privileges from the database without sorting.
     * <p>
     * Cache Behavior: Results are cached in {@link #DYNAMIC_PRIVILEGES_ALL_CACHE}. Unlike the
     * sorted variant, this method caches even {@code null} results. The cache is evicted when
     * any privilege is saved.
     * </p>
     *
     * @return a list of all {@link DynamicPrivilege} entities in database order
     * @see DynamicPrivilegeRepository#findAll()
     */
    @Cacheable(cacheNames = DYNAMIC_PRIVILEGES_ALL_CACHE)
    public List<DynamicPrivilege> findAll() {
        return dynamicPrivilegeRepository.findAll();
    }

    /**
     * Retrieves multiple dynamic privileges by their IDs.
     * <p>
     * Note: This method is NOT cached. For cached individual lookups, use {@link #findById(Long)}
     * in a loop. This method is useful for batch retrieval when cache benefits are minimal.
     * </p>
     *
     * @param ids the collection of privilege IDs to retrieve
     * @return a list of {@link DynamicPrivilege} entities matching the provided IDs (may be fewer if some IDs not found)
     * @see DynamicPrivilegeRepository#findAllById(Iterable)
     */
    public List<DynamicPrivilege> findAllById(Iterable<Long> ids) {
        return dynamicPrivilegeRepository.findAllById(ids);
    }

    /**
     * Finds a dynamic privilege by its ID.
     * <p>
     * Cache Behavior: Results are cached in {@link #DYNAMIC_PRIVILEGES_CACHE} using the ID
     * as the key (SpEL expression {@code #p0}). Empty {@link Optional} results are not cached
     * ({@code unless = "#result == null"}). Cache is updated via {@code @CachePut} when privileges are saved.
     * </p>
     *
     * @param id the privilege ID to search for
     * @return an {@link Optional} containing the {@link DynamicPrivilege} if found, or empty if not found
     * @see DynamicPrivilegeRepository#findById(Object)
     */
    @Cacheable(cacheNames = DYNAMIC_PRIVILEGES_CACHE, key = "#p0", unless = "#result == null")
    public Optional<DynamicPrivilege> findById(Long id) {
        return dynamicPrivilegeRepository.findById(id);
    }

    /**
     * Counts the total number of dynamic privileges in the database.
     * <p>
     * Cache Behavior: The count is cached in {@link #DYNAMIC_PRIVILEGES_ALL_CACHE}. The cache
     * is evicted when any privilege is saved, ensuring the count remains accurate.
     * </p>
     *
     * @return the total count of {@link DynamicPrivilege} entities
     * @see DynamicPrivilegeRepository#count()
     */
    @Cacheable(cacheNames = DYNAMIC_PRIVILEGES_ALL_CACHE)
    public long count() {
        return dynamicPrivilegeRepository.count();
    }
}
