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

package com.openkoda.core.service.module;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.controller.common.PageAttributes;
import com.openkoda.core.helper.UserHelper;
import com.openkoda.core.service.event.ApplicationEvent;
import com.openkoda.dto.OrganizationDto;
import com.openkoda.dto.user.BasicUser;
import com.openkoda.dto.user.UserRoleDto;
import com.openkoda.integration.model.configuration.IntegrationModuleOrganizationConfiguration;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.module.Module;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central in-memory module registry and lifecycle orchestration service.
 * <p>
 * This service manages OpenKoda module registration, name-based lookup, and ordered iteration
 * by {@link Module#getOrdinal()}. It orchestrates event-driven configuration creation for users,
 * organizations, and roles through application event listeners.

 * <p>
 * <b>Core Responsibilities:</b>
 * <ul>
 *   <li>Module registration via {@link #registerModule(Module)}</li>
 *   <li>Name-based module lookup with O(1) performance</li>
 *   <li>Ordered iteration over modules sorted by ordinal</li>
 *   <li>Event-driven lifecycle hooks for user/organization/role events</li>
 *   <li>Integration configuration management</li>
 * </ul>

 * <p>
 * <b>Architecture:</b> Extends {@link ComponentProvider} for repositories/services access and
 * implements {@link PageAttributes} for debug() helper. Registered as Spring bean named "modules"
 * via {@code @Service("modules")}. Depends on ApplicationContextProvider being initialized first
 * via {@code @DependsOn("applicationContextProvider")} to ensure ApplicationContext availability
 * before {@link #init()} executes.

 * <p>
 * <b>Data Structures:</b> Maintains a {@code TreeSet<Module>} sorted by ordinal for ordered iteration
 * and a {@code HashMap<String, Module>} for O(1) name-based lookups. The service intentionally returns
 * direct views (not defensive copies) to minimize allocations.

 * <p>
 * <b>Thread Safety:</b> {@link #registerModule(Module)} is synchronized for thread-safe concurrent
 * registration. However, returned iterators and direct map access present concurrent modification risks.
 * Iterator usage during concurrent registration will throw {@code ConcurrentModificationException}.

 * <p>
 * <b>Lifecycle:</b> {@link #init()} method annotated with {@code @PostConstruct} wires event listeners
 * during Spring initialization for six {@link ApplicationEvent} types:
 * USER_CREATED, USER_DELETED, ORGANIZATION_CREATED, ORGANIZATION_DELETED,
 * USER_ROLE_CREATED, USER_ROLE_DELETED.

 * <p>
 * <b>Usage Examples:</b>
 * <pre>{@code
 * // Module registration
 * modules.registerModule(new Module("myModule", 100));
 * 
 * // Name-based lookup
 * Module m = modules.getModuleForName("myModule");
 * 
 * // Ordered iteration
 * modules.getIterator().forEachRemaining(module -> process(module));
 * }</pre>

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @since 1.7.1
 * @see Module
 * @see ComponentProvider
 * @see PageAttributes
 * @see ApplicationContext
 * @see ApplicationEvent
 */
@Service("modules")
@DependsOn("applicationContextProvider")
public class ModuleService extends ComponentProvider implements PageAttributes {

    /**
     * Injected ApplicationContext providing access to Spring beans and application metadata.
     * <p>
     * Used to access Spring-managed components and application configuration during module lifecycle operations.

     */
    @Inject
    ApplicationContext applicationContext;

    /**
     * Final TreeSet maintaining registered modules sorted by {@link Module#getOrdinal()} using
     * {@link Comparator#comparingInt(java.util.function.ToIntFunction)}.
     * <p>
     * Provides ordered iteration over modules. The direct iterator returned by {@link #getIterator()}
     * is not thread-safe for concurrent modifications during iteration.

     */
    private final SortedSet<Module> modules = new TreeSet<>(Comparator.comparingInt(Module::getOrdinal));
    
    /**
     * Final HashMap providing O(1) name-based module lookup.
     * <p>
     * Populated by {@link #registerModule(Module)} and directly exposed via {@link #getModuleForName(String)}.

     */
    private final Map<String, Module> modulesByName = new HashMap<>();

    /**
     * Registers a module with thread-safe insertion into both the modules TreeSet and modulesByName HashMap.
     * <p>
     * This synchronized method ensures concurrent registration safety by preventing race conditions during
     * insertion. However, synchronization only protects the registration operation itself; post-registration
     * access to the collections is not protected.

     * <p>
     * <b>Behavior:</b> The module is added to the sorted modules TreeSet and indexed in modulesByName map
     * by its name. Debug logging records the registration via {@code debug("[registerModule] {}", module.getName())}.

     * <p>
     * <b>Duplicate Handling:</b> If a module with the same name already exists, it will be replaced in the
     * modulesByName map but will create a duplicate entry in the modules TreeSet (ordinal allows duplicates).

     *
     * @param module Module instance to register with name, ordinal, and configuration. Must not be null.
     * @return the registered Module instance (same as input parameter)
     * @see #getModuleForName(String)
     * @see #getIterator()
     * @see Module
     */
    synchronized public Module registerModule(Module module) {
        debug("[registerModule] {}", module.getName());
        modules.add(module);
        modulesByName.put(module.getName(), module);
        return module;
    }

    /**
     * Injected UserHelper for user-related operations in event handlers.
     * <p>
     * Provides utility methods for user management operations triggered by application events.

     */
    @Inject
    UserHelper userHelper;

    /**
     * Lifecycle initialization method that registers event listeners for module configuration management.
     * <p>
     * This {@code @PostConstruct} method is invoked by Spring after dependency injection completes.
     * It wires six {@link ApplicationEvent} handlers for entity lifecycle events:

     * <ul>
     *   <li><b>USER_CREATED</b> → {@link #createConfigurationsForUser(BasicUser)}</li>
     *   <li><b>USER_DELETED</b> → {@link #deleteConfigurationsForUser(BasicUser)}</li>
     *   <li><b>ORGANIZATION_CREATED</b> → {@link #createConfigurationsForOrganization(OrganizationDto)}</li>
     *   <li><b>ORGANIZATION_DELETED</b> → {@link #deleteConfigurationsForOrganization(OrganizationDto)}</li>
     *   <li><b>USER_ROLE_CREATED</b> → {@link #createConfigurationsForOrganizationUser(UserRoleDto)}</li>
     *   <li><b>USER_ROLE_DELETED</b> → {@link #deleteConfigurationsForOrganizationUser(UserRoleDto)}</li>
     * </ul>
     * <p>
     * This event-driven architecture enables modules to react to entity lifecycle changes and maintain
     * configuration consistency across user, organization, and role operations.

     *
     * @see ApplicationEvent
     * @see jakarta.annotation.PostConstruct
     */
    @PostConstruct
    void init() {
        services.applicationEvent.registerEventListener(ApplicationEvent.USER_CREATED,
                this::createConfigurationsForUser);
        services.applicationEvent.registerEventListener(ApplicationEvent.USER_DELETED,
                this::deleteConfigurationsForUser);
        services.applicationEvent.registerEventListener(ApplicationEvent.ORGANIZATION_CREATED,
                this::createConfigurationsForOrganization);
        services.applicationEvent.registerEventListener(ApplicationEvent.ORGANIZATION_DELETED,
                this::deleteConfigurationsForOrganization);
        services.applicationEvent.registerEventListener(ApplicationEvent.USER_ROLE_CREATED,
                this::createConfigurationsForOrganizationUser);
        services.applicationEvent.registerEventListener(ApplicationEvent.USER_ROLE_DELETED,
                this::deleteConfigurationsForOrganizationUser);

    }

    /**
     * Returns a direct iterator over the modules TreeSet for ordered traversal.
     * <p>
     * <b>Iteration Order:</b> Modules are returned sorted by {@link Module#getOrdinal()} in ascending order.

     * <p>
     * <b>Thread Safety Warning:</b> The returned iterator is NOT thread-safe. Concurrent modifications
     * (such as calling {@link #registerModule(Module)} during iteration) will cause
     * {@code ConcurrentModificationException}.

     *
     * @return Iterator providing ordered traversal of registered modules
     * @see Module
     * @see #registerModule(Module)
     */
    public Iterator<Module> getIterator() {
        return modules.iterator();
    }

    /**
     * Returns a filtered list of modules matching the provided names, preserving ordinal order.
     * <p>
     * <b>Implementation:</b> Streams the modules TreeSet, filters by {@code moduleNames.contains(name)},
     * and collects results to a List preserving the ordinal-based sort order.

     * <p>
     * <b>Performance Note:</b> Uses {@code contains()} on the moduleNames List which is O(n) per module.
     * For large name lists, consider passing a Set for O(1) lookup performance.

     *
     * @param moduleNames List of module name strings to retrieve. Must not be null.
     * @return List containing only modules with names in moduleNames list, ordered by ordinal.
     *         Returns empty list if no matches found.
     * @see #getModuleForName(String)
     * @see Module
     */
    public List<Module> getModulesForNames(@NotNull List<String> moduleNames) {
        return modules.stream().filter(a -> moduleNames.contains(a.getName())).collect(Collectors.toList());
    }

    /**
     * Performs O(1) name-based module lookup using the modulesByName HashMap.
     * <p>
     * Retrieves the module registered with the specified name. Lookup is case-sensitive
     * and uses the exact module name string.

     *
     * @param moduleName Unique module name string (case-sensitive). Must not be null as enforced by {@code @NotNull}.
     * @return Module instance if found, null if no module is registered with that name
     * @see #registerModule(Module)
     * @see #getModulesForNames(List)
     */
    public Module getModuleForName(@NotNull String moduleName) {
        return modulesByName.get(moduleName);
    }


    /**
     * Event handler for USER_CREATED events to create per-user module configurations.
     * <p>
     * <b>Current Implementation:</b> Placeholder that logs debug message and returns true.
     * Actual per-user configuration logic is not yet implemented.

     * <p>
     * <b>Intended Purpose:</b> Future module-specific user initialization such as per-user
     * integration credentials, preferences, or default settings.

     *
     * @param user BasicUser DTO containing user ID and metadata for the newly created user
     * @return boolean - currently always returns true (placeholder implementation)
     * @see BasicUser
     * @see ApplicationEvent#USER_CREATED
     */
    public boolean createConfigurationsForUser(BasicUser user) {
        debug("[createConfigurationsForUser] {}", user);
        return true;
    }

    /**
     * Event handler for USER_DELETED events to clean up per-user module configurations.
     * <p>
     * <b>Current Implementation:</b> Placeholder that logs debug message and returns true.
     * Cleanup logic for user-specific configurations is not yet implemented.

     *
     * @param user BasicUser DTO of the deleted user
     * @return boolean - currently always returns true (placeholder implementation)
     * @see BasicUser
     * @see ApplicationEvent#USER_DELETED
     */
    public boolean deleteConfigurationsForUser(BasicUser user) {
        debug("[deleteConfigurationsForUser] {}", user);
        return true;
    }

    /**
     * Event handler for ORGANIZATION_CREATED events to initialize organization-level integration configurations.
     * <p>
     * <b>Implementation:</b> Creates an {@link IntegrationModuleOrganizationConfiguration} entity with
     * the organization ID and persists it via {@code repositories.unsecure.integration.save()}, bypassing
     * privilege checks since this is a platform-level system operation.

     * <p>
     * <b>Note:</b> Uses unsecure repository as this is automatic platform initialization, not a user-triggered action.

     *
     * @param organization OrganizationDto containing organizationId and metadata for the newly created organization
     * @return boolean - always returns true after successful persistence
     * @see IntegrationModuleOrganizationConfiguration
     * @see OrganizationDto
     * @see ApplicationEvent#ORGANIZATION_CREATED
     */
    public boolean createConfigurationsForOrganization(OrganizationDto organization) {
        debug("[createConfigurationsForOrganization] {}", organization);
        repositories.unsecure.integration.save(new IntegrationModuleOrganizationConfiguration(organization.getOrganizationId()));
        return true;
    }

    /**
     * Event handler for ORGANIZATION_DELETED events to clean up organization-level configurations.
     * <p>
     * <b>Current Implementation:</b> Placeholder that returns true with no operation performed.
     * Cleanup logic for {@link IntegrationModuleOrganizationConfiguration} is not yet implemented.

     *
     * @param organization OrganizationDto of the deleted organization
     * @return boolean - always returns true (no-op currently)
     * @see OrganizationDto
     * @see ApplicationEvent#ORGANIZATION_DELETED
     */
    public boolean deleteConfigurationsForOrganization(OrganizationDto organization) {
        return true;
    }

    /**
     * Event handler for USER_ROLE_CREATED events to create user-role-specific configurations for organization assignments.
     * <p>
     * <b>Implementation:</b> Short-circuits for global roles via {@code ur.isGlobal()} check, returning immediately
     * without configuration creation. For organization-scoped roles, the method currently returns true as a placeholder.

     * <p>
     * <b>Note:</b> Configuration logic for non-global user roles is not yet implemented.

     *
     * @param ur UserRoleDto containing user ID, role ID, organization ID, and global flag
     * @return boolean - always returns true (placeholder for organization-scoped roles)
     * @see UserRoleDto
     * @see ApplicationEvent#USER_ROLE_CREATED
     */
    public boolean createConfigurationsForOrganizationUser(UserRoleDto ur) {
        debug("[createConfigurationsForOrganizationUser] user role: {}", ur);
        if (ur.isGlobal()) return true;
        return true;

    }

    /**
     * Event handler for USER_ROLE_DELETED events to clean up user-role-specific configurations.
     * <p>
     * <b>Current Implementation:</b> Placeholder that logs debug message and returns true.
     * Cleanup logic for user-role configurations is not yet implemented.

     *
     * @param ur UserRoleDto of the deleted user role assignment
     * @return boolean - always returns true (no-op currently)
     * @see UserRoleDto
     * @see ApplicationEvent#USER_ROLE_DELETED
     */
    public boolean deleteConfigurationsForOrganizationUser(UserRoleDto ur) {
        debug("[deleteConfigurationsForOrganizationUser] user role: {}", ur);
        return true;

    }

    /**
     * Assigns a set of privileges to a role, enabling modules to register required privileges during initialization.
     * <p>
     * <b>Implementation:</b> Delegates privilege assignment to {@code services.role.addPrivilegesToRole(roleName, privileges)}
     * for actual persistence and role privilege management. Debug logs the operation before delegation.

     * <p>
     * <b>Usage:</b> Modules invoke this method during registration to ensure roles have appropriate permissions
     * for module-specific operations.

     *
     * @param roleName Role name string to which privileges will be granted
     * @param privileges Set of {@link PrivilegeBase} privileges to assign to the role
     * @return boolean - always returns true after delegation to role service
     * @see PrivilegeBase
     */
    public boolean addModulePrivilegesToRole(String roleName, Set<PrivilegeBase> privileges) {
        debug("[addModulePrivilegesToRole] role: {} privilages: {}", roleName, privileges);
        services.role.addPrivilegesToRole(roleName, privileges);
        return true;
    }

}
