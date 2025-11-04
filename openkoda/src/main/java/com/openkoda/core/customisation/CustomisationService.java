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

package com.openkoda.core.customisation;

import com.openkoda.core.audit.PropertyChangeListener;
import com.openkoda.core.flow.PageAttr;
import com.openkoda.core.form.CRUDControllerConfiguration;
import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.core.repository.common.ProfileSettingsRepository;
import com.openkoda.core.repository.common.ScopedSecureRepository;
import com.openkoda.core.service.event.AbstractApplicationEvent;
import com.openkoda.core.service.event.EventConsumer;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.common.AuditableEntity;
import com.openkoda.model.common.SearchableEntity;
import com.openkoda.model.module.Module;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Defines the runtime extension and customization API for the OpenKoda platform.
 * <p>
 * This interface provides the primary extension points for module developers to register custom functionality,
 * integrate with the audit system, register event listeners and consumers, configure frontend mappings,
 * and expose CRUD controllers. It enables dynamic platform customization without modifying core code.
 * <p>
 * The CustomisationService acts as the central registry for runtime extensions and is primarily implemented
 * by {@link BasicCustomisationService}, which orchestrates application startup, dependency injection, and
 * lifecycle event coordination. Modules typically interact with this service during the bootstrap phase
 * via {@link #registerOnApplicationStartListener(Consumer)} callbacks.
 * <p>
 * Key capabilities provided by this interface:
 * <ul>
 *   <li><b>Audit Integration:</b> Register entity classes for automatic property change auditing via
 *       {@link #registerAuditableClass(Class, String)}</li>
 *   <li><b>Event System:</b> Register event listeners, consumers, and custom event classes for application-wide
 *       event broadcasting via {@link #registerEventListener(AbstractApplicationEvent, Consumer)} and related methods</li>
 *   <li><b>Module Lifecycle:</b> Register modules and hook into application startup via
 *       {@link #registerModule(Module)} and {@link #onApplicationStart()}</li>
 *   <li><b>Frontend Integration:</b> Register frontend mappings and settings forms via
 *       {@link #registerFrontendMapping(FrontendMappingDefinition, ScopedSecureRepository)}</li>
 *   <li><b>CRUD Automation:</b> Register HTML and API CRUD controllers with privilege-based access control via
 *       {@link #registerHtmlCrudController(FrontendMappingDefinition, ScopedSecureRepository, PrivilegeBase, PrivilegeBase)}</li>
 * </ul>
 * <p>
 * Example usage - typical module registration workflow:
 * <pre>{@code
 * // Register module during application startup
 * customisationService.registerOnApplicationStartListener(service -> {
 *     // Register module
 *     Module myModule = new Module();
 *     myModule.setName("CustomModule");
 *     service.registerModule(myModule);
 *     
 *     // Register auditable entity
 *     service.registerAuditableClass(CustomEntity.class, "Custom Entity");
 *     
 *     // Register event listener
 *     service.registerEventListener(ApplicationEvent.USER_CREATED, 
 *         user -> System.out.println("New user: " + user));
 *     
 *     // Register frontend mapping
 *     FrontendMappingDefinition mapping = new FrontendMappingDefinition(...);
 *     service.registerFrontendMapping(mapping, customRepository);
 * });
 * }</pre>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see BasicCustomisationService
 * @see Module
 * @see AbstractApplicationEvent
 * @see FrontendMappingDefinition
 */
public interface CustomisationService {

    /**
     * Registers an entity class for automatic property change auditing.
     * <p>
     * When registered, all property modifications on instances of this class will be tracked via the
     * {@link com.openkoda.core.audit.AuditInterceptor} and stored in the audit trail. This enables
     * comprehensive change tracking for regulatory compliance and debugging purposes.
     * 
     * <p>
     * The classLabel provides a human-readable name displayed in audit logs and reports. It should be
     * descriptive and localized where appropriate (e.g., "User Account", "Organization Settings").
     * 
     *
     * @param <T> the entity type, must extend {@link AuditableEntity}
     * @param c the entity class to register for auditing; must not be null
     * @param classLabel human-readable label for this entity type in audit logs; must not be null or empty
     * @return a {@link PropertyChangeListener} configured for this entity class
     * @see AuditableEntity
     * @see com.openkoda.core.audit.AuditInterceptor
     * @see #unregisterAuditableClass(Class)
     */
    <T extends AuditableEntity> PropertyChangeListener registerAuditableClass(Class<T> c, String classLabel);

    /**
     * Unregisters an entity class from automatic property change auditing.
     * <p>
     * After unregistration, property modifications on instances of this class will no longer be
     * captured in the audit trail. Use this method to remove audit tracking for classes that no
     * longer require change monitoring or when disabling a module.
     * 
     *
     * @param c the entity class to unregister from auditing; must not be null
     * @see #registerAuditableClass(Class, String)
     */
    void unregisterAuditableClass(Class c);

    /**
     * Checks whether an entity class is registered for automatic property change auditing.
     * <p>
     * This method queries the audit registry to determine if the specified class is currently
     * configured for change tracking. Use this to verify registration status before attempting
     * to audit entity modifications.
     * 
     *
     * @param c the entity class to check; must not be null
     * @return true if the class is registered for auditing, false otherwise
     * @see #registerAuditableClass(Class, String)
     */
    boolean isAuditableClass(Class c);

    /**
     * Registers a simple event listener for a specific application event type.
     * <p>
     * The listener is invoked whenever the specified event is broadcast throughout the application.
     * This provides a lightweight mechanism for modules to react to system events without complex
     * infrastructure. The listener receives the event payload as its sole parameter.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * service.registerEventListener(ApplicationEvent.USER_CREATED, 
     *     user -> emailService.sendWelcomeEmail(user));
     * }</pre>
     * 
     *
     * @param <T> the type of event data expected by the listener
     * @param event the application event type to listen for; must not be null
     * @param eventListener the consumer to invoke when the event is broadcast; must not be null
     * @return true if registration succeeded, false if listener was already registered
     * @see AbstractApplicationEvent
     * @see #registerEventConsumer(Class, EventConsumer)
     */
    <T> boolean registerEventListener(AbstractApplicationEvent event, Consumer<T> eventListener);

    /**
     * Registers an event listener with static data parameters for advanced event handling.
     * <p>
     * This overload allows listeners to receive both the event payload and up to four static string
     * parameters that are bound at registration time. This is useful for parameterized event handling
     * where the listener behavior depends on configuration values known at startup.
     * 
     * <p>
     * The listener is invoked with the event data and a concatenated string of static parameters.
     * Use this when event handlers need context beyond the event payload itself.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * service.registerEventListener(ApplicationEvent.ORDER_PLACED,
     *     (order, config) -> notificationService.send(order, config),
     *     "email", "sms", "webhook", "push");
     * }</pre>
     * 
     *
     * @param <T> the type of event data expected by the listener
     * @param event the application event type to listen for; must not be null
     * @param eventListener the bi-consumer to invoke with event data and static parameters; must not be null
     * @param staticData1 first static parameter value; may be null
     * @param staticData2 second static parameter value; may be null
     * @param staticData3 third static parameter value; may be null
     * @param staticData4 fourth static parameter value; may be null
     * @return true if registration succeeded, false if listener was already registered
     * @see #registerEventListener(AbstractApplicationEvent, Consumer)
     */
    <T> boolean registerEventListener(AbstractApplicationEvent event, BiConsumer<T, String> eventListener, String staticData1, String staticData2, String staticData3, String staticData4);

    /**
     * Registers a typed event consumer for a specific event class.
     * <p>
     * Unlike simple listeners, event consumers provide richer integration with the event system,
     * including transaction boundaries, error handling, and retry logic. Consumers are typically
     * used for critical business logic that must execute reliably when events occur.
     * 
     * <p>
     * The event consumer is registered for all events matching the specified class type. Built-in
     * consumers registered during application startup include EmailService, BackupService,
     * ServerJSRunner, and various integration handlers (PushNotification, Slack, Webhook).
     * 
     *
     * @param <T> the event class type
     * @param eventClass the class of events this consumer handles; must not be null
     * @param eventConsumer the consumer implementation; must not be null
     * @return true if registration succeeded, false if consumer was already registered for this event class
     * @see EventConsumer
     * @see BasicCustomisationService#registerApplicationConsumers()
     */
    <T> boolean registerEventConsumer(Class<T> eventClass, EventConsumer<T> eventConsumer);

    /**
     * Registers a module with the platform for lifecycle management and dependency tracking.
     * <p>
     * Modules represent logical units of functionality that extend the platform's capabilities.
     * Registration enables the module to participate in the application lifecycle, receive
     * configuration updates, and integrate with platform services. Once registered, the module
     * is persisted and automatically loaded on subsequent application starts.
     * 
     * <p>
     * Typical registration occurs during application startup via
     * {@link #registerOnApplicationStartListener(Consumer)} callbacks, allowing modules to
     * initialize resources and register their extension points.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * Module customModule = new Module();
     * customModule.setName("PaymentIntegration");
     * customModule.setVersion("1.0.0");
     * Module registered = service.registerModule(customModule);
     * }</pre>
     * 
     *
     * @param module the module to register; must not be null and must have a unique name
     * @return the registered module instance, potentially with updated metadata
     * @see Module
     * @see #registerOnApplicationStartListener(Consumer)
     */
    Module registerModule(Module module);

    /**
     * Signals completion of the application startup phase and triggers post-bootstrap actions.
     * <p>
     * This method is invoked by {@link BasicCustomisationService} after all Spring beans are
     * initialized and registered listeners have executed. It represents the final stage of
     * application initialization before the platform is ready to serve requests.
     * 
     * <p>
     * Implementations typically emit {@link CoreSettledEvent} to notify interested parties that
     * core services are fully available. Module developers should not invoke this method directly;
     * instead, use {@link #registerOnApplicationStartListener(Consumer)} to execute code during
     * startup.
     * 
     *
     * @see BasicCustomisationService#onApplicationEvent(org.springframework.context.event.ContextRefreshedEvent)
     * @see CoreSettledEvent
     */
    void onApplicationStart();

    /**
     * Registers a custom event class for use with the platform's event system.
     * <p>
     * This method makes the specified event class available for listener registration and
     * consumer binding. Event classes are typically registered from the
     * {@code application.classes.event} configuration property during application startup,
     * but modules can register additional event types programmatically.
     * 
     * <p>
     * Registered event classes must follow platform conventions for event serialization
     * and deserialization. They become available for scheduling, persistence, and
     * cross-service event broadcasting.
     * 
     *
     * @param <T> the event class type
     * @param eventClass the event class to register; must not be null
     * @see AbstractApplicationEvent
     * @see com.openkoda.core.service.event.EventListenerService#registerEventClasses(Class[])
     */
    <T> void registerApplicationEventClass(Class<T> eventClass);

    /**
     * Registers a settings form for display in the administrative settings interface.
     * <p>
     * This method integrates custom configuration forms into the platform's settings UI,
     * allowing administrators to manage module-specific settings through the standard
     * administrative interface. The form is bound to a repository for persistence and
     * uses Thymeleaf fragments for rendering.
     * 
     * <p>
     * The registration process creates a mapping between the entity type, its form representation,
     * the repository for data access, and the UI fragment for display. Forms registered via this
     * method appear in the settings menu and follow the platform's security and validation rules.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * service.registerSettingsForm(
     *     customSettingsRepository,
     *     CustomSettings::toForm,
     *     PageAttr.customSettings,
     *     "settings/custom-settings",
     *     "customSettingsForm"
     * );
     * }</pre>
     * 
     *
     * @param <SE> the searchable entity type representing the settings data model
     * @param <SF> the form type used for UI binding and validation
     * @param repository the repository for persisting and retrieving settings entities; must not be null
     * @param formConstructor function to convert entity to form representation; must not be null
     * @param formPageAttribute the page attribute key for accessing the form in templates; must not be null
     * @param formFragmentFile the Thymeleaf template file path containing the form fragment; must not be null
     * @param formFragmentName the name of the fragment within the template file; must not be null
     * @see ProfileSettingsRepository
     * @see SearchableEntity
     * @see PageAttr
     */
    <SE extends SearchableEntity, SF> void registerSettingsForm(
            ProfileSettingsRepository<SE> repository,
            Function<SE, SF> formConstructor,
            PageAttr<SF> formPageAttribute,
            String formFragmentFile,
            String formFragmentName);

    /**
     * Registers a callback to execute after Spring context initialization completes.
     * <p>
     * This is the primary hook for module initialization logic. Callbacks registered via this
     * method are invoked during the {@link BasicCustomisationService#onApplicationStart()} phase,
     * after all Spring beans are wired but before the application serves requests. This timing
     * is ideal for registering modules, auditable classes, event listeners, and frontend mappings.
     * 
     * <p>
     * Multiple listeners can be registered and will execute in registration order. Each listener
     * receives a reference to the CustomisationService, enabling further registrations and
     * platform queries during startup.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * service.registerOnApplicationStartListener(customService -> {
     *     customService.registerModule(myModule);
     *     customService.registerAuditableClass(MyEntity.class, "My Entity");
     *     customService.registerEventListener(ApplicationEvent.STARTUP_COMPLETE,
     *         event -> initializeResources());
     * });
     * }</pre>
     * 
     *
     * @param c the callback consumer receiving the CustomisationService; must not be null
     * @see BasicCustomisationService#onApplicationStart()
     */
    void registerOnApplicationStartListener(Consumer<CustomisationService> c);

    /**
     * Registers a frontend mapping definition with its associated repository for dynamic UI generation.
     * <p>
     * Frontend mappings define the structure and behavior of dynamically generated UI components,
     * including forms, tables, and detail views. When registered, the mapping becomes available
     * for use by controllers and templates to render entity-specific interfaces without hardcoded
     * HTML or JSP files.
     * 
     * <p>
     * The mapping is stored in the {@link FrontendMappingMap} singleton registry and paired with
     * the provided repository for data access. This registration is synchronized via
     * {@link BasicCustomisationService#registerFrontendMapping(FrontendMappingDefinition, ScopedSecureRepository)}
     * to ensure thread-safe access during concurrent module initialization.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * FrontendMappingDefinition mapping = FrontendMappingDefinition.builder()
     *     .name("customEntityMapping")
     *     .entityClass(CustomEntity.class)
     *     .fields(fieldDefinitions)
     *     .build();
     * service.registerFrontendMapping(mapping, customEntityRepository);
     * }</pre>
     * 
     *
     * @param definition the frontend mapping definition describing UI structure; must not be null
     * @param repository the scoped secure repository for entity data access; must not be null
     * @see FrontendMappingDefinition
     * @see ScopedSecureRepository
     * @see FrontendMappingMap
     * @see BasicCustomisationService#registerFrontendMapping(FrontendMappingDefinition, ScopedSecureRepository)
     */
    void registerFrontendMapping(FrontendMappingDefinition definition, ScopedSecureRepository repository);

    /**
     * Unregisters a frontend mapping definition by its unique key.
     * <p>
     * Removes the mapping from the {@link FrontendMappingMap} registry, making it unavailable
     * for UI generation. Use this method when disabling modules or removing entity types from
     * the platform. After unregistration, any references to the mapping key will fail.
     * 
     *
     * @param key the unique mapping name/key to unregister; must not be null
     * @see #registerFrontendMapping(FrontendMappingDefinition, ScopedSecureRepository)
     * @see FrontendMappingMap
     */
    void unregisterFrontendMapping(String key);

    /**
     * Registers an HTML CRUD controller with default privilege requirements.
     * <p>
     * Creates a generic CRUD controller for HTML endpoints, providing standard create, read, update,
     * and delete operations for the entity type described by the frontend mapping. The controller
     * uses default privilege checks based on the entity configuration.
     * 
     * <p>
     * The generated controller handles standard HTTP GET and POST requests for entity management,
     * integrates with the privilege system for access control, and renders responses using the
     * provided frontend mapping definition.
     * 
     *
     * @param definition the frontend mapping definition describing entity structure; must not be null
     * @param repository the scoped secure repository for data access; must not be null
     * @return the CRUD controller configuration for further customization or reference
     * @see #registerHtmlCrudController(FrontendMappingDefinition, ScopedSecureRepository, PrivilegeBase, PrivilegeBase)
     * @see CRUDControllerConfiguration
     */
    CRUDControllerConfiguration registerHtmlCrudController(FrontendMappingDefinition definition, ScopedSecureRepository repository);

    /**
     * Registers an HTML CRUD controller with explicit privilege-based access control.
     * <p>
     * Creates a generic CRUD controller for HTML endpoints with fine-grained privilege enforcement.
     * The readPrivilege controls access to list and detail views, while writePrivilege controls
     * access to create, update, and delete operations. This enables role-based access control
     * for entity management interfaces.
     * 
     * <p>
     * Privileges are evaluated against the current user's role and organization context via
     * the {@link com.openkoda.core.security.UserProvider} and
     * {@link com.openkoda.service.user.BasicPrivilegeService}. Unauthorized access attempts result
     * in HTTP 403 Forbidden responses.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * service.registerHtmlCrudController(
     *     customerMapping,
     *     customerRepository,
     *     PrivilegeBase.readOrgData,
     *     PrivilegeBase.manageOrgData
     * );
     * }</pre>
     * 
     *
     * @param definition the frontend mapping definition describing entity structure; must not be null
     * @param repository the scoped secure repository for data access; must not be null
     * @param readPrivilege the privilege required for read operations; must not be null
     * @param writePrivilege the privilege required for write operations; must not be null
     * @return the CRUD controller configuration for further customization or reference
     * @see PrivilegeBase
     * @see CRUDControllerConfiguration
     */
    CRUDControllerConfiguration registerHtmlCrudController(FrontendMappingDefinition definition, ScopedSecureRepository repository, PrivilegeBase readPrivilege, PrivilegeBase writePrivilege);

    /**
     * Registers an HTML CRUD controller with string-based privilege names (convenience method).
     * <p>
     * This default method provides a convenient overload accepting privilege names as strings,
     * automatically converting them to {@link PrivilegeBase} instances via
     * {@link PrivilegeHelper#valueOfString(String)}. Use this when privilege names are determined
     * dynamically or loaded from configuration.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * service.registerHtmlCrudController(
     *     productMapping,
     *     productRepository,
     *     "readOrgData",
     *     "manageOrgData"
     * );
     * }</pre>
     * 
     *
     * @param definition the frontend mapping definition describing entity structure; must not be null
     * @param repository the scoped secure repository for data access; must not be null
     * @param readPrivilege the privilege name required for read operations; must not be null
     * @param writePrivilege the privilege name required for write operations; must not be null
     * @return the CRUD controller configuration for further customization or reference
     * @see PrivilegeHelper#valueOfString(String)
     * @see #registerHtmlCrudController(FrontendMappingDefinition, ScopedSecureRepository, PrivilegeBase, PrivilegeBase)
     */
    default CRUDControllerConfiguration registerHtmlCrudController(FrontendMappingDefinition definition, ScopedSecureRepository repository, String readPrivilege, String writePrivilege) {
        return registerHtmlCrudController(definition, repository, PrivilegeHelper.valueOfString(readPrivilege), PrivilegeHelper.valueOfString(writePrivilege));
    }

    /**
     * Unregisters an HTML CRUD controller by its unique key.
     * <p>
     * Removes the controller from the {@link com.openkoda.controller.HtmlCRUDControllerConfigurationMap}
     * registry, making its endpoints unavailable. Use this when disabling modules or removing
     * entity types. After unregistration, requests to the controller's URLs will return HTTP 404.
     * 
     *
     * @param key the unique controller key/name to unregister; must not be null
     * @see #registerHtmlCrudController(FrontendMappingDefinition, ScopedSecureRepository)
     */
    void unregisterHtmlCrudController(String key);

    /**
     * Registers a REST API CRUD controller with default privilege requirements.
     * <p>
     * Creates a generic REST API controller providing standard create, read, update, and delete
     * operations for the entity type described by the frontend mapping. The controller exposes
     * JSON endpoints following RESTful conventions and uses default privilege checks based on
     * the entity configuration.
     * 
     * <p>
     * The generated API endpoints support standard HTTP methods (GET, POST, PUT, DELETE) and
     * return JSON responses with appropriate HTTP status codes. Content negotiation and error
     * handling follow platform conventions.
     * 
     *
     * @param definition the frontend mapping definition describing entity structure; must not be null
     * @param repository the scoped secure repository for data access; must not be null
     * @return the CRUD controller configuration for further customization or reference
     * @see #registerApiCrudController(FrontendMappingDefinition, ScopedSecureRepository, PrivilegeBase, PrivilegeBase)
     * @see CRUDControllerConfiguration
     */
    CRUDControllerConfiguration registerApiCrudController(FrontendMappingDefinition definition, ScopedSecureRepository repository);

    /**
     * Registers a REST API CRUD controller with explicit privilege-based access control.
     * <p>
     * Creates a generic REST API controller with fine-grained privilege enforcement for JSON
     * endpoints. The readPrivilege controls access to GET operations (list and detail), while
     * writePrivilege controls access to POST, PUT, and DELETE operations. This enables role-based
     * access control for programmatic entity management.
     * 
     * <p>
     * Privileges are evaluated against the current user's API credentials or session context.
     * Unauthorized access attempts result in HTTP 403 Forbidden responses with JSON error details.
     * The controller supports both session-based and token-based authentication.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * service.registerApiCrudController(
     *     orderMapping,
     *     orderRepository,
     *     PrivilegeBase.readOrgData,
     *     PrivilegeBase.manageOrgData
     * );
     * }</pre>
     * 
     *
     * @param definition the frontend mapping definition describing entity structure; must not be null
     * @param repository the scoped secure repository for data access; must not be null
     * @param readPrivilege the privilege required for read operations; must not be null
     * @param writePrivilege the privilege required for write operations; must not be null
     * @return the CRUD controller configuration for further customization or reference
     * @see PrivilegeBase
     * @see CRUDControllerConfiguration
     */
    CRUDControllerConfiguration registerApiCrudController(FrontendMappingDefinition definition, ScopedSecureRepository repository, PrivilegeBase readPrivilege, PrivilegeBase writePrivilege);

    /**
     * Registers a REST API CRUD controller with string-based privilege names (convenience method).
     * <p>
     * This default method provides a convenient overload accepting privilege names as strings,
     * automatically converting them to {@link PrivilegeBase} instances via
     * {@link PrivilegeHelper#valueOfString(String)}. Use this when privilege names are determined
     * dynamically or loaded from configuration.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * service.registerApiCrudController(
     *     inventoryMapping,
     *     inventoryRepository,
     *     "readOrgData",
     *     "manageOrgData"
     * );
     * }</pre>
     * 
     *
     * @param definition the frontend mapping definition describing entity structure; must not be null
     * @param repository the scoped secure repository for data access; must not be null
     * @param readPrivilege the privilege name required for read operations; must not be null
     * @param writePrivilege the privilege name required for write operations; must not be null
     * @return the CRUD controller configuration for further customization or reference
     * @see PrivilegeHelper#valueOfString(String)
     * @see #registerApiCrudController(FrontendMappingDefinition, ScopedSecureRepository, PrivilegeBase, PrivilegeBase)
     */
    default CRUDControllerConfiguration registerApiCrudController(FrontendMappingDefinition definition, ScopedSecureRepository repository, String readPrivilege, String writePrivilege) {
        return registerApiCrudController(definition, repository, PrivilegeHelper.valueOfString(readPrivilege), PrivilegeHelper.valueOfString(writePrivilege));
    }

    /**
     * Unregisters a REST API CRUD controller by its unique key.
     * <p>
     * Removes the controller from the {@link com.openkoda.controller.ApiCRUDControllerConfigurationMap}
     * registry, making its REST endpoints unavailable. Use this when disabling modules or removing
     * entity types. After unregistration, requests to the controller's API URLs will return HTTP 404.
     * 
     *
     * @param key the unique controller key/name to unregister; must not be null
     * @see #registerApiCrudController(FrontendMappingDefinition, ScopedSecureRepository)
     */
    void unregisterApiCrudController(String key);

}
