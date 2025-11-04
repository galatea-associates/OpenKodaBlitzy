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
 * Module lifecycle management services supporting modular application architecture in the OpenKoda platform.
 * <p>
 * This package provides centralized module registry, module metadata management, and event-driven configuration
 * creation for modules across users, organizations, and roles. The core abstraction is the Module entity, which
 * represents pluggable application modules with name, ordinal for ordering, version, dependencies, and lifecycle hooks.
 * 
 *
 * <b>Key Components</b>
 *
 * <b>ModuleService</b>
 * <p>
 * Central in-memory module registry maintaining a {@code TreeSet<Module>} sorted by ordinal and
 * {@code HashMap<String, Module>} for name lookups. Provides synchronized {@code registerModule()} for thread-safe
 * registration, exposes {@code getIterator()} for ordered traversal and {@code getModuleForName()} for O(1) lookups.
 * Implements event-driven architecture listening to {@code USER_CREATED}, {@code USER_DELETED},
 * {@code ORGANIZATION_CREATED}, {@code ORGANIZATION_DELETED}, {@code USER_ROLE_CREATED}, {@code USER_ROLE_DELETED}
 * ApplicationEvent tokens. Delegates to handler methods creating or deleting configurations, such as
 * {@code createConfigurationsForOrganization} which persists IntegrationModuleOrganizationConfiguration.
 * Supports privilege assignment via {@code addModulePrivilegesToRole} delegation to RoleService.
 * Extends ComponentProvider for repositories and services access, implements PageAttributes for debug logging.
 * 
 *
 * <b>ApplicationArea</b>
 * <p>
 * Type-safe enum defining seven UI insertion points: {@code CONTENT_TOP}, {@code CONTENT_BOTTOM}, {@code BODY_TOP},
 * {@code BODY_BOTTOM}, {@code SIDEBAR_TOP}, {@code SIDEBAR_BOTTOM}, {@code NAVBAR_PROFILE} for positioning dynamic
 * module content in page layouts. {@code STANDARD_AREAS} constant provides ordered canonical array of all areas.
 * Enum constants are breaking API changes requiring coordinated updates.
 * 
 *
 * <b>Module Registration Workflow</b>
 * <ol>
 *   <li>Module instance created with name, ordinal (ordering), version, dependencies</li>
 *   <li>{@code ModuleService.registerModule()} called to insert into registry (synchronized for thread-safety)</li>
 *   <li>Module available via {@code getModuleForName(name)} for O(1) lookup or {@code getIterator()} for ordered traversal</li>
 *   <li>ApplicationEvent listeners trigger configuration creation (e.g., IntegrationModuleOrganizationConfiguration on {@code ORGANIZATION_CREATED})</li>
 *   <li>Modules can register privileges via {@code addModulePrivilegesToRole} during initialization</li>
 * </ol>
 *
 * <b>Integration with Platform</b>
 * <ul>
 *   <li><b>CustomisationService</b>: Module registration integrates with customization and extension registration (indirect reference, actual integration in parent service package)</li>
 *   <li><b>OpenkodaModule entity</b>: Database persistence of module metadata including name, version, enabled status, initialization state (note: ModuleService maintains in-memory registry, not JPA repository)</li>
 *   <li><b>ApplicationEventService</b>: Event bus for lifecycle notifications, ModuleService registers listeners in {@code @PostConstruct init()}</li>
 *   <li><b>ComponentProvider</b>: Base class providing repositories and services aggregators</li>
 *   <li><b>IntegrationModuleOrganizationConfiguration</b>: Per-organization integration configuration entity created by ModuleService for new organizations</li>
 * </ul>
 *
 * <b>Event-Driven Architecture</b>
 * <p>
 * {@code ModuleService.init()} {@code @PostConstruct} wires six event handlers:
 * 
 * <ul>
 *   <li>{@code USER_CREATED} → {@code createConfigurationsForUser} (placeholder, logs only)</li>
 *   <li>{@code USER_DELETED} → {@code deleteConfigurationsForUser} (placeholder)</li>
 *   <li>{@code ORGANIZATION_CREATED} → {@code createConfigurationsForOrganization} (persists IntegrationModuleOrganizationConfiguration via unsecure repository)</li>
 *   <li>{@code ORGANIZATION_DELETED} → {@code deleteConfigurationsForOrganization} (placeholder)</li>
 *   <li>{@code USER_ROLE_CREATED} → {@code createConfigurationsForOrganizationUser} (short-circuits for global roles, placeholder for org-scoped)</li>
 *   <li>{@code USER_ROLE_DELETED} → {@code deleteConfigurationsForOrganizationUser} (placeholder)</li>
 * </ul>
 * <p>
 * Handlers return boolean (always true in current implementation).
 * 
 *
 * <b>Thread-Safety Considerations</b>
 * <ul>
 *   <li>{@code ModuleService.registerModule()} synchronized for safe concurrent registration</li>
 *   <li>modules TreeSet and modulesByName HashMap reads not synchronized - caller responsibility</li>
 *   <li>{@code getIterator()} returns direct iterator - {@code ConcurrentModificationException} if {@code registerModule()} called during iteration</li>
 *   <li>Service returns direct views (not defensive copies) for performance</li>
 *   <li>ApplicationArea enum constants inherently thread-safe and immutable</li>
 * </ul>
 *
 * <b>Concurrency Notes</b>
 * <ul>
 *   <li>Event handlers executed sequentially by single ApplicationEventService thread</li>
 *   <li>Module registration typically happens during application startup before concurrent access</li>
 *   <li>Post-startup lookups are read-only and safe if no concurrent {@code registerModule()}</li>
 * </ul>
 *
 * <b>Design Patterns</b>
 * <ul>
 *   <li><b>Registry pattern</b>: ModuleService maintains central module registry with name-based and ordered access</li>
 *   <li><b>Event listener pattern</b>: ModuleService subscribes to entity lifecycle events for configuration creation</li>
 *   <li><b>Enum constant pattern</b>: ApplicationArea provides type-safe vocabulary for UI placement</li>
 * </ul>
 *
 * <b>Usage Examples</b>
 * <pre>
 * // Module registration (typically in @PostConstruct)
 * Module myModule = new Module("integration-module", 100);
 * moduleService.registerModule(myModule);
 *
 * // Module lookup
 * Module m = moduleService.getModuleForName("integration-module");
 *
 * // Ordered iteration
 * moduleService.getIterator().forEachRemaining(module -&gt; {
 *     // Process modules in ordinal order
 * });
 *
 * // Privilege assignment
 * moduleService.addModulePrivilegesToRole("ADMIN",
 *     Set.of(Privilege.canReadIntegrations, Privilege.canWriteIntegrations));
 *
 * // ApplicationArea usage in module registration
 * Module module = new Module("widget-module", 200);
 * module.registerWidgetForArea(ApplicationArea.SIDEBAR_TOP, widgetRenderer);
 * </pre>
 *
 * <b>Related Packages</b>
 * <ul>
 *   <li>{@code com.openkoda.model.module}: Module entity definition with name, ordinal, version fields</li>
 *   <li>{@code com.openkoda.core.service.event}: ApplicationEventService, ApplicationEvent enum tokens</li>
 *   <li>{@code com.openkoda.core.customisation}: CustomisationService for module extension hooks (parent package)</li>
 *   <li>{@code com.openkoda.integration.model.configuration}: IntegrationModuleOrganizationConfiguration entity</li>
 *   <li>{@code com.openkoda.controller}: ComponentProvider base class</li>
 * </ul>
 *
 * <b>Best Practices</b>
 * <ul>
 *   <li>Register modules during application startup ({@code @PostConstruct} initialization)</li>
 *   <li>Use ordinal values with gaps (e.g., 100, 200, 300) to allow insertion of future modules</li>
 *   <li>Avoid concurrent {@code registerModule()} calls during iteration</li>
 *   <li>Prefer {@code getModuleForName()} for single lookups (O(1) vs iteration)</li>
 *   <li>Use {@code getModulesForNames()} with Set parameter for large batch lookups</li>
 * </ul>
 *
 * <b>Known Limitations</b>
 * <ul>
 *   <li>Module registry is in-memory only - not persisted across restarts</li>
 *   <li>No unregister or deregister operation - modules remain in registry for application lifetime</li>
 *   <li>Duplicate module names allowed in TreeSet (only last wins in HashMap)</li>
 *   <li>Event handler cleanup logic (delete operations) mostly placeholder implementations</li>
 *   <li>No module dependency resolution or initialization ordering based on dependencies</li>
 * </ul>
 *
 * @since 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.core.service.module;