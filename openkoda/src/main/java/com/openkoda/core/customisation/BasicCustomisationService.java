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

import com.openkoda.controller.ApiCRUDControllerConfigurationMap;
import com.openkoda.controller.ComponentProvider;
import com.openkoda.controller.HtmlCRUDControllerConfigurationMap;
import com.openkoda.core.audit.AuditInterceptor;
import com.openkoda.core.audit.PropertyChangeListener;
import com.openkoda.core.flow.PageAttr;
import com.openkoda.core.form.CRUDControllerConfiguration;
import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.core.form.ReflectionBasedEntityForm;
import com.openkoda.core.helper.SpringProfilesHelper;
import com.openkoda.core.lifecycle.BaseDatabaseInitializer;
import com.openkoda.core.lifecycle.SearchViewCreator;
import com.openkoda.core.multitenancy.MultitenancyService;
import com.openkoda.core.repository.common.ProfileSettingsRepository;
import com.openkoda.core.repository.common.ScopedSecureRepository;
import com.openkoda.core.security.UserProvider;
import com.openkoda.core.service.BackupService;
import com.openkoda.core.service.FrontendMappingDefinitionService;
import com.openkoda.core.service.GenericWebhookService;
import com.openkoda.core.service.SlackService;
import com.openkoda.core.service.email.EmailService;
import com.openkoda.core.service.event.AbstractApplicationEvent;
import com.openkoda.core.service.event.ApplicationEvent;
import com.openkoda.core.service.event.EventConsumer;
import com.openkoda.core.service.event.EventConsumerCategory;
import com.openkoda.dto.CanonicalObject;
import com.openkoda.dto.NotificationDto;
import com.openkoda.dto.OrganizationRelatedObject;
import com.openkoda.dto.system.ScheduledSchedulerDto;
import com.openkoda.integration.service.PushNotificationService;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.User;
import com.openkoda.model.common.AuditableEntity;
import com.openkoda.model.common.SearchableEntity;
import com.openkoda.model.module.Module;
import com.openkoda.repository.SearchableRepositories;
import com.openkoda.service.role.RoleModificationsConsumers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple5;
import reactor.util.function.Tuples;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.openkoda.controller.common.URLConstants._LOGIN;
import static com.openkoda.core.helper.NameHelper.getClasses;
import static com.openkoda.core.helper.SpringProfilesHelper.isInitializationProfile;


/**
 * Central Spring {@code @Service} bootstrap and orchestration hub for OpenKoda application lifecycle management.
 * <p>
 * This service extends {@link ComponentProvider} and implements {@link CustomisationService} to serve as the primary
 * coordination point for application initialization, module registration, event consumer setup, and custom extension
 * registration. It orchestrates a deterministic bootstrap sequence that initializes all core subsystems in the correct
 * order during application startup.
 * </p>
 * <p>
 * Key responsibilities:
 * <ul>
 * <li>Orchestrate application bootstrap via {@link #onApplicationStart()} with deterministic initialization sequence</li>
 * <li>Register and manage dynamic entity repositories and searchable repositories</li>
 * <li>Initialize database with seed data based on active Spring profiles</li>
 * <li>Register built-in event consumers for messaging, backup, code execution, and role management</li>
 * <li>Provide extension points for custom modules via {@link #registerModule(Module)}</li>
 * <li>Manage frontend mapping definitions and CRUD controller registrations</li>
 * <li>Configure auditable entity tracking via {@link AuditInterceptor}</li>
 * </ul>
 * </p>
 * <p>
 * Bootstrap Sequence (executed in {@link #onApplicationStart()}):
 * <ol>
 * <li>Set cron job authentication context</li>
 * <li>Discover searchable repositories from entity metadata</li>
 * <li>Register dynamic entity repositories (runtime-generated entities)</li>
 * <li>Load initial/seed data into database</li>
 * <li>Register built-in application event consumers (email, backup, JS execution, webhooks, etc.)</li>
 * <li>Register event classes from configuration</li>
 * <li>Catalog all available application events and consumers</li>
 * <li>Register event listeners from database definitions</li>
 * <li>Schedule background jobs from database</li>
 * <li>Load form definitions from database</li>
 * <li>Prepare search views for searchable entities</li>
 * <li>Execute custom {@code onApplicationStartListeners}</li>
 * <li>Emit APPLICATION_STARTED event</li>
 * </ol>
 * </p>
 * <p>
 * For initialization profiles (drop_and_init_database), the service prints admin credentials and exits after bootstrap.
 * </p>
 * <p>
 * Usage example for module registration:
 * <pre>
 * &#64;Autowired
 * private CustomisationService customisationService;
 * 
 * Module myModule = new Module("my-module", "1.0.0");
 * customisationService.registerModule(myModule);
 * customisationService.registerOnApplicationStartListener(service -> {
 *     // Custom initialization logic
 * });
 * </pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CustomisationService
 * @see ComponentProvider
 * @see ContextRefreshedEvent
 */
@Service
public class BasicCustomisationService extends ComponentProvider implements CustomisationService {

    /**
     * Hibernate session-scoped audit interceptor for tracking entity property changes.
     * Used to register auditable entity classes and enable change tracking for compliance and debugging.
     */
    @Autowired
    private AuditInterceptor auditInterceptor;

    /**
     * Spring application context providing access to the IoC container.
     * Used primarily for controlled application shutdown via {@link SpringApplication#exit(ApplicationContext, org.springframework.boot.ExitCodeGenerator)}
     * when running in initialization profiles.
     */
    @Autowired
    private ApplicationContext appContext;

    /**
     * Database initializer responsible for loading seed data and initial schema setup.
     * Executes different initialization logic based on active Spring profiles (initialization vs. runtime).
     */
    @Autowired
    private BaseDatabaseInitializer initialDataLoader;

    /**
     * Creator service for database search views supporting full-text and entity search capabilities.
     * Prepares PostgreSQL-backed search infrastructure for {@link SearchableEntity} implementations.
     */
    @Autowired
    private SearchViewCreator searchViewCreator;

    /**
     * Multi-tenancy service managing organization-scoped operations and tenant isolation.
     * Provides context for organization-aware data access and resource scoping.
     */
    @Autowired
    private MultitenancyService multitenancyService;

    /**
     * Registry map for HTML-based CRUD controller configurations.
     * Maps entity types to their corresponding web UI CRUD endpoints and form definitions.
     */
    @Autowired
    private HtmlCRUDControllerConfigurationMap htmlCrudControllerConfigurationMap;

    /**
     * Registry map for REST API CRUD controller configurations.
     * Maps entity types to their corresponding REST API endpoints for programmatic access.
     */
    @Autowired
    private ApiCRUDControllerConfigurationMap apiCrudControllerConfigurationMap;

    /**
     * Central registry for frontend mapping definitions.
     * Stores entity-to-form mappings used by both HTML and API controllers for CRUD operations.
     */
    @Autowired
    private FrontendMappingMap frontendMappingMap;

    /**
     * Service for managing and persisting frontend mapping definitions to database.
     * Handles CRUD operations for form definitions stored as database entities.
     */
    @Autowired
    private FrontendMappingDefinitionService frontendMappingDefinitionService;

    /**
     * Spring Environment abstraction providing access to application properties and active profiles.
     * Used for profile detection and configuration value retrieval during bootstrap.
     */
    @Autowired
    private Environment env;

    /**
     * Initial administrator username from application configuration.
     * Printed to console during initialization profile bootstrap to provide first-login credentials.
     * Configured via {@code init.admin.username} property in application.properties.
     */
    @Value("${init.admin.username}")
    private String initAdminUsername;

    /**
     * Initial administrator password from application configuration.
     * Printed to console during initialization profile bootstrap to provide first-login credentials.
     * Configured via {@code init.admin.password} property in application.properties.
     * Should be changed immediately after first login in production environments.
     */
    @Value("${init.admin.password}")
    private String initAdminPassword;

    /**
     * Base URL for the application used in login URL construction and external link generation.
     * Defaults to {@code http://localhost:8080} if not specified.
     * Configured via {@code base.url} property in application.properties.
     * Should be set to actual public URL in production (e.g., {@code https://app.example.com}).
     */
    @Value("${base.url:http://localhost:8080}")
    private String baseUrlString;

    /**
     * Array of fully-qualified event class names to register at application startup.
     * Allows external configuration of custom event types beyond built-in {@link ApplicationEvent} types.
     * Configured via {@code application.classes.event} property as comma-separated class names.
     * Example: {@code com.example.MyCustomEvent,com.example.AnotherEvent}
     * Defaults to empty array if not specified.
     */
    @Value("${application.classes.event:}")
    private String[] eventClasses;
    /**
     * Registers an entity class for Hibernate-level property change auditing.
     * <p>
     * Enables automatic tracking of field modifications for the specified entity type via {@link AuditInterceptor}.
     * Property changes are captured at the Hibernate session level and can be persisted to audit log tables.
     * </p>
     *
     * @param <T> entity type extending {@link AuditableEntity}
     * @param c the entity class to enable auditing for
     * @param classLabel human-readable label for the entity type used in audit log displays
     * @return PropertyChangeListener instance for the registered entity class
     * @see AuditInterceptor#registerAuditableClass(Class, String)
     * @see AuditableEntity
     */
    public final <T extends AuditableEntity> PropertyChangeListener registerAuditableClass(Class<T> c, String classLabel) {
        return auditInterceptor.registerAuditableClass(c, classLabel);
    }

    /**
     * Removes an entity class from audit tracking, disabling property change monitoring.
     *
     * @param c the entity class to disable auditing for
     * @see AuditInterceptor#unregisterAuditableClass(Class)
     */
    @Override
    public void unregisterAuditableClass(Class c) {
        auditInterceptor.unregisterAuditableClass(c);
    }

    /**
     * Checks whether an entity class is currently registered for audit tracking.
     *
     * @param c the entity class to check
     * @return true if the class is auditable, false otherwise
     * @see AuditInterceptor#isAuditableClass(Class)
     */
    @Override
    public boolean isAuditableClass(Class c) {
        return auditInterceptor.isAuditableClass(c);
    }

    /**
     * Collection of custom lifecycle hooks executed during application bootstrap.
     * <p>
     * Listeners are invoked near the end of {@link #onApplicationStart()} sequence, after core initialization
     * completes but before the APPLICATION_STARTED event is emitted. This provides extension point for modules
     * to perform custom initialization logic with full access to initialized services.
     * </p>
     * <p>
     * Register listeners via {@link #registerOnApplicationStartListener(Consumer)}.
     * </p>
     */
    private List<Consumer<CustomisationService>> onApplicationStartListeners = new ArrayList<>();

    /**
     * Registers a simple event listener consumer for the specified application event type.
     * <p>
     * The listener is invoked with the event data when the specified event is emitted via
     * {@code services.applicationEvent.emitEvent()}.
     * </p>
     *
     * @param <T> type of event data object
     * @param event the application event type to listen for
     * @param eventListener consumer function invoked when event is emitted, receives event data
     * @return true if registration succeeded, false if listener already registered
     * @see AbstractApplicationEvent
     */
    @Override
    public final <T> boolean registerEventListener(AbstractApplicationEvent event, Consumer<T> eventListener) {
        return services.applicationEvent.registerEventListener(event, eventListener);
    }

    /**
     * Registers an event listener with static parameter support for parameterized event handling.
     * <p>
     * The BiConsumer listener receives both the event data object and concatenated static parameters,
     * enabling event handlers to be configured with additional context at registration time.
     * </p>
     *
     * @param <T> type of event data object
     * @param event the application event type to listen for
     * @param eventListener bi-consumer function receiving event data and concatenated static parameters
     * @param staticData1 first static parameter passed to listener
     * @param staticData2 second static parameter passed to listener
     * @param staticData3 third static parameter passed to listener
     * @param staticData4 fourth static parameter passed to listener
     * @return true if registration succeeded, false if listener already registered
     * @see AbstractApplicationEvent
     */
    @Override
    public final <T> boolean registerEventListener(AbstractApplicationEvent event, BiConsumer<T, String> eventListener, String staticData1, String staticData2, String staticData3, String staticData4) {
        return services.applicationEvent.registerEventListener(event, eventListener,  staticData1,  staticData2,  staticData3,  staticData4);
    }

    /**
     * Registers a typed event consumer for a specific event class.
     * <p>
     * Event consumers provide more structure than simple listeners, encapsulating event handling logic
     * in dedicated {@link EventConsumer} implementations with category metadata.
     * </p>
     *
     * @param <T> type of event class
     * @param eventClass the event class to consume
     * @param eventConsumer the consumer implementation handling events of this type
     * @return true if registration succeeded, false if consumer already registered
     * @see EventConsumer
     * @see EventConsumerCategory
     */
    @Override
    public final <T> boolean registerEventConsumer(Class<T> eventClass, EventConsumer<T> eventConsumer) {
        return services.applicationEvent.registerEventConsumer(eventClass, eventConsumer);
    }

    /**
     * Registers a custom module in the OpenKoda module registry.
     * <p>
     * Modules encapsulate reusable functionality bundles with their own entities, services, controllers,
     * and initialization logic. Registration makes the module available for lifecycle management and
     * enables module-specific configuration and extension points.
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * Module insuranceModule = new Module("insurance-module", "1.0.0");
     * customisationService.registerModule(insuranceModule);
     * </pre>
     * </p>
     *
     * @param module the module definition to register
     * @return the registered module instance (may be enriched with runtime metadata)
     * @see Module
     */
    @Override
    public Module registerModule(Module module) {
        return services.module.registerModule(module);
    }

    /**
     * Executes the deterministic application bootstrap sequence orchestrating all core subsystem initialization.
     * <p>
     * This method is the central coordination point for OpenKoda startup, executing initialization steps in a
     * carefully ordered sequence to ensure dependencies are satisfied and services are available when needed.
     * All operations execute within a cron job authentication context to bypass security checks during bootstrap.
     * </p>
     * <p>
     * Bootstrap sequence:
     * <ol>
     * <li><b>setCronJobAuthentication</b> - Establish system-level security context for bootstrap operations</li>
     * <li><b>discoverSearchableRepositories</b> - Scan classpath for {@link SearchableEntity} implementations</li>
     * <li><b>registerDynamicRepositories</b> - Register runtime-generated entity repositories (Byte Buddy)</li>
     * <li><b>loadInitialData</b> - Populate database with seed data (behavior varies by profile)</li>
     * <li><b>registerApplicationConsumers</b> - Register built-in event consumers (email, backup, JS, webhooks)</li>
     * <li><b>registerEventClasses</b> - Register custom event types from configuration</li>
     * <li><b>setAllAvailableAppEvents</b> - Catalog all available application event types</li>
     * <li><b>setAllAvailableAppConsumers</b> - Catalog all registered event consumer implementations</li>
     * <li><b>registerAllEventListenersFromDb</b> - Load and register event listeners persisted in database</li>
     * <li><b>scheduleAllFromDb</b> - Schedule background jobs defined in database</li>
     * <li><b>loadAllFormsFromDb</b> - Load form definitions from database (skip in initialization profile)</li>
     * <li><b>prepareSearchableRepositories</b> - Create database search views for full-text search</li>
     * <li><b>onApplicationStartListeners execution</b> - Execute custom module initialization hooks</li>
     * <li><b>emit APPLICATION_STARTED</b> - Broadcast application ready event to listeners</li>
     * </ol>
     * </p>
     * <p>
     * <b>Initialization Profile Behavior:</b> When running with initialization profile (drop_and_init_database),
     * the method prints admin credentials to console, then gracefully exits the application via
     * {@link SpringApplication#exit(ApplicationContext, org.springframework.boot.ExitCodeGenerator)}. This allows
     * database initialization without starting the full application server.
     * </p>
     * <p>
     * <b>Thread Safety:</b> This method is invoked once during application lifecycle by Spring's
     * {@link ContextRefreshedEvent} handler. Not thread-safe for concurrent invocation.
     * </p>
     * <p>
     * Console output example for initialization profile:
     * <pre>
     * *********************************************************************
     *  Application initialized successfully.
     *  Start application without the drop_and_init_database profile
     *  and go to http://localhost:8080/login
     *  Credentials (u/p): admin / admin123
     * *********************************************************************
     * </pre>
     * </p>
     *
     * @see UserProvider#setCronJobAuthentication()
     * @see SearchableRepositories#discoverSearchableRepositories()
     * @see CustomisationService
     * @see ContextRefreshedEvent
     */
    @Override
    public void onApplicationStart() {
        try {
            UserProvider.setCronJobAuthentication();
            SearchableRepositories.discoverSearchableRepositories();
            services.dynamicEntityRegistration.registerDynamicRepositories(not(isInitializationProfile()));
            initialDataLoader.loadInitialData(isInitializationProfile());
            this.registerApplicationConsumers();
            services.eventListener.registerEventClasses((Class<AbstractApplicationEvent>[]) getClasses(eventClasses));
            services.eventListener.setAllAvailableAppEvents();
            services.eventListener.setAllAvailableAppConsumers();
            services.eventListener.registerAllEventListenersFromDb();
            services.scheduler.scheduleAllFromDb();
            services.form.loadAllFormsFromDb(not(isInitializationProfile()));
            searchViewCreator.prepareSearchableRepositories();
            for (Consumer<CustomisationService> c : onApplicationStartListeners) {
                c.accept(this);
            }
            services.applicationEvent.emitEvent(ApplicationEvent.APPLICATION_STARTED, LocalDateTime.now());
        } finally {
            UserProvider.clearAuthentication();
        }
        if (isInitializationProfile()) {
            System.out.println("*********************************************************************");
            System.out.println(" Application initialized successfully.");
            System.out.println(String.format(" Start application without the %s profile", SpringProfilesHelper.INITIALIZATION_PROFILE));
            System.out.println(String.format(" and go to %s%s", baseUrlString, _LOGIN));
            System.out.println(String.format(" Credentials (u/p): %s / %s", initAdminUsername, initAdminPassword));
            System.out.println("*********************************************************************");
            SpringApplication.exit(appContext, () -> 0);
            System.exit(0);
        }
    }

    /**
     * Registers all built-in application event consumers for core OpenKoda functionality.
     * <p>
     * This private method is invoked during {@link #onApplicationStart()} to configure event-driven integrations
     * for messaging, backup, server-side code execution, role management, and webhook dispatching. Each consumer
     * is registered with its event type, target service class, method name, description, and static parameters.
     * </p>
     * <p>
     * Registered event consumers by category:
     * <ul>
     * <li><b>MESSAGE (EmailService):</b>
     *   <ul>
     *   <li>User → sendAndSaveEmail - Send email to User with template parameter</li>
     *   <li>CanonicalObject → sendAndSaveEmail - Send email to specified address with template parameters</li>
     *   </ul>
     * </li>
     * <li><b>BACKUP (BackupService):</b>
     *   <ul>
     *   <li>ScheduledSchedulerDto → doFullBackup - Perform full backup when event data matches "backup" parameter</li>
     *   <li>File → copyBackupFile - Copy backup archive to remote host or local path</li>
     *   </ul>
     * </li>
     * <li><b>SERVER_SIDE_CODE (ServerJSRunner):</b>
     *   <ul>
     *   <li>CanonicalObject → runScriptJS - Execute named SERVER_JS script</li>
     *   <li>ScheduledSchedulerDto → startScheduledServerJs - Execute scheduled JS with 4 static parameters</li>
     *   <li>LocalDateTime → startCustomisationServerJs - Execute JS with customisationService access</li>
     *   </ul>
     * </li>
     * <li><b>ROLE_MODIFICATION (RoleModificationsConsumers):</b>
     *   <ul>
     *   <li>OrganizationRelatedObject → modifyRoleForAllUsersInOrganization - Modify roles for all users via JS</li>
     *   <li>OrganizationRelatedObject → modifyGlobalRoleForOrganization - Add/remove global roles via JS</li>
     *   </ul>
     * </li>
     * <li><b>PUSH_NOTIFICATION (PushNotificationService):</b>
     *   <ul>
     *   <li>NotificationDto → createSlackPostMessageRequest - Generate Slack message HTTP request</li>
     *   <li>NotificationDto → createMsTeamsPostMessageRequest - Generate MS Teams message HTTP request</li>
     *   <li>NotificationDto → createEmailNotification - Generate email notification for scheduled delivery</li>
     *   </ul>
     * </li>
     * <li><b>MESSAGE (SlackService):</b>
     *   <ul>
     *   <li>CanonicalObject → sendToSlackWithCanonical - Send message via webhook with 2 parameters</li>
     *   <li>CanonicalObject → sendToSlackWithCanonical - Send message via webhook with 4 parameters (channel, username)</li>
     *   </ul>
     * </li>
     * <li><b>MESSAGE (GenericWebhookService):</b>
     *   <ul>
     *   <li>CanonicalObject → sendToUrlWithCanonical - Send JSON to URL with custom headers</li>
     *   </ul>
     * </li>
     * </ul>
     * </p>
     * <p>
     * All consumers are registered via {@code services.applicationEvent.registerEventConsumerWithMethod()}
     * with reflection-based method invocation for loose coupling.
     * </p>
     *
     * @see EmailService
     * @see BackupService
     * @see ServerJSRunner
     * @see RoleModificationsConsumers
     * @see PushNotificationService
     * @see SlackService
     * @see GenericWebhookService
     */
    private void registerApplicationConsumers() {
        services.applicationEvent.registerEventConsumerWithMethod(
                User.class,
                EmailService.class,
                "sendAndSaveEmail",
                "Consumer that sends an email to User (event object), based on template specified by the second parameter.",
                EventConsumerCategory.MESSAGE,
                String.class
        );
        services.applicationEvent.registerEventConsumerWithMethod(
                CanonicalObject.class,
                EmailService.class,
                "sendAndSaveEmail",
                "Consumer that sends an email to the email provided as second parameter, based on template specified by the first static parameter.",
                EventConsumerCategory.MESSAGE,
                String.class,
                String.class);
        services.applicationEvent.registerEventConsumerWithMethod(
                ScheduledSchedulerDto.class,
                BackupService.class, "doFullBackup",
                "The consumer is supposed to do back-up. It will proceed only if the event parameter of ScheduledSchedulerDto object == consumer.parameter.backup " +
                        "property (default == 'backup').",
                EventConsumerCategory.BACKUP
        );
        services.applicationEvent.registerEventConsumerWithMethod(
                File.class,
                BackupService.class,
                "copyBackupFile",
                "This consumer will perform a secure copy of created backup archive file to remote host. If remote host isn't specified file will be copied into local path.",
                EventConsumerCategory.BACKUP
        );
        services.applicationEvent.registerEventConsumerWithMethod(
                CanonicalObject.class,
                ServerJSRunner.class,
                "runScriptJS",
                "This consumer will run Javascript that is defined as \"SERVER_JS\". Consumer is parametrized by the name of script.",
                EventConsumerCategory.SERVER_SIDE_CODE,
                String.class
        );
        services.applicationEvent.registerEventConsumerWithMethod(
                OrganizationRelatedObject.class,
                RoleModificationsConsumers.class,
                "modifyRoleForAllUsersInOrganization",
                "This consumer will run Javascript that is defined in Server-side Js. And modify all users role given by the script. Consumer is parametrized by the name of script.",
                EventConsumerCategory.ROLE_MODIFICATION,
                String.class);
        services.applicationEvent.registerEventConsumerWithMethod(
                OrganizationRelatedObject.class,
                RoleModificationsConsumers.class,
                "modifyGlobalRoleForOrganization",
                "This consumer will run Javascript that is defined as \"SERVER_JS\". And add or remove roles given by the script. Consumer is parametrized by the name of script.",
                EventConsumerCategory.ROLE_MODIFICATION,
                String.class);
        services.applicationEvent.registerEventConsumerWithMethod(
                NotificationDto.class,
                PushNotificationService.class,
                "createSlackPostMessageRequest",
                "This consumer generates a HttpRequest object which would then be found by a scheduled job and pushed as a message to the organization's Slack channel.",
                EventConsumerCategory.PUSH_NOTIFICATION
        );
        services.applicationEvent.registerEventConsumerWithMethod(
                NotificationDto.class,
                PushNotificationService.class,
                "createMsTeamsPostMessageRequest",
                "This consumer generates a HttpRequest object which would then be found by a scheduled job and pushed as a message to the organization's Ms Teams channel.",
                EventConsumerCategory.PUSH_NOTIFICATION
        );
        services.applicationEvent.registerEventConsumerWithMethod(
                NotificationDto.class,
                PushNotificationService.class,
                "createEmailNotification",
                "This consumer generates a notification Email which would then be send to a recipient by a scheduled job.",
                EventConsumerCategory.PUSH_NOTIFICATION
        );
        services.applicationEvent.registerEventConsumerWithMethod(
                CanonicalObject.class,
                SlackService.class,
                "sendToSlackWithCanonical",
                "Sends message generated in FrontendResource(1st param) to slack via webHook(2nd param)",
                EventConsumerCategory.MESSAGE,
                String.class,
                String.class);
        
        services.applicationEvent.registerEventConsumerWithMethod(
                CanonicalObject.class,
                SlackService.class,
                "sendToSlackWithCanonical",
                "Sends message generated in FrontendResource(1st param) to slack via webHook(2nd param), channel name (3rd param, optional), username (4rd param, optional) ",
                EventConsumerCategory.MESSAGE,
                String.class,
                String.class,
                String.class,
                String.class);
        
        services.applicationEvent.registerEventConsumerWithMethod(
                ScheduledSchedulerDto.class,
                ServerJSRunner.class,
                "startScheduledServerJs",
                "Executes Server-side Js on Scheduler Event. Param1: Scheduler event data must match Static Parameter 1 in order to run. Param2: Name of the Server-side JS to run. All 4 Static Parameters are passed as arguments to Server js (arguments.length == 4)",
                EventConsumerCategory.SERVER_SIDE_CODE,
                String.class,
                String.class,
                String.class,
                String.class);
        services.applicationEvent.registerEventConsumerWithMethod(
                CanonicalObject.class,
                GenericWebhookService.class,
                "sendToUrlWithCanonical",
                "Sends message to url (first param) generated as JSON in FrontendResource(second param), with headers as JSON in FrontendResource(third param).",
                EventConsumerCategory.MESSAGE,
                String.class,
                String.class,
                String.class);
        services.applicationEvent.registerEventConsumerWithMethod(
                LocalDateTime.class,
                ServerJSRunner.class,
                "startCustomisationServerJs",
                "Executes ServerJs with customisation service. Param1: Name of the ServerJS to run. Server js can access 'customisationService' object. All 4 Static Parameters are passed as arguments to Server js (arguments.length == 4)",
                EventConsumerCategory.SERVER_SIDE_CODE,
                String.class,
                String.class,
                String.class,
                String.class);
    }

    /**
     * Registers a custom application event class for use in the event system.
     * <p>
     * Allows modules to define custom event types beyond built-in {@link ApplicationEvent} types.
     * Registered event classes can then have listeners and consumers attached via
     * {@link #registerEventListener} or {@link #registerEventConsumer}.
     * </p>
     *
     * @param <T> the event class type
     * @param eventClass the custom event class to register
     * @see AbstractApplicationEvent
     */
    @Override
    public <T> void registerApplicationEventClass(Class<T> eventClass) {
        services.eventListener.registerEventClass(eventClass);
    }

    /**
     * Collection of additional settings forms for profile/organization configuration UI.
     * <p>
     * Each tuple contains: (ProfileSettingsRepository, form constructor Function, PageAttr for form placement,
     * Thymeleaf fragment file path, fragment name). These forms are dynamically rendered in settings pages
     * to allow modules to contribute custom configuration interfaces.
     * </p>
     * <p>
     * Register forms via {@link #registerSettingsForm(ProfileSettingsRepository, Function, PageAttr, String, String)}.
     * </p>
     */
    public final List<Tuple5<ProfileSettingsRepository, Function, PageAttr, String, String>> additionalSettingsForms = new ArrayList<>();

    /**
     * Registers an additional settings form for organization/profile configuration UI.
     * <p>
     * This method allows modules to contribute custom configuration forms that appear in the settings pages.
     * The form is integrated into the settings UI via Thymeleaf fragment inclusion, with automatic data
     * binding through the provided repository and form constructor.
     * </p>
     *
     * @param <SE> searchable entity type stored in settings
     * @param <SF> form type for entity editing
     * @param repository the repository for persisting settings entities
     * @param formConstructor function to construct form from entity instance
     * @param formPageAttribute page attribute identifier for form placement in UI
     * @param formFragmentFile Thymeleaf template file path containing the form fragment
     * @param formFragmentName Thymeleaf fragment name within the template file
     * @see ProfileSettingsRepository
     * @see PageAttr
     */
    @Override
    public <SE extends SearchableEntity, SF> void registerSettingsForm(
            ProfileSettingsRepository<SE> repository,
            Function<SE, SF> formConstructor,
            PageAttr<SF> formPageAttribute,
            String formFragmentFile,
            String formFragmentName) {
        additionalSettingsForms.add(Tuples.of(repository, formConstructor, formPageAttribute, formFragmentFile, formFragmentName));
    }

    /**
     * Registers a lifecycle hook executed during application bootstrap.
     * <p>
     * The provided consumer is invoked near the end of {@link #onApplicationStart()} after core initialization
     * completes, providing an extension point for modules to perform custom initialization with full access
     * to initialized services and repositories.
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * customisationService.registerOnApplicationStartListener(service -> {
     *     // Custom module initialization logic
     *     service.registerModule(myModule);
     * });
     * </pre>
     * </p>
     *
     * @param c consumer function receiving this CustomisationService instance for initialization
     * @see #onApplicationStart()
     */
    @Override
    public void registerOnApplicationStartListener(Consumer<CustomisationService> c) {
        onApplicationStartListeners.add(c);
    }

    /**
     * Registers a frontend mapping definition with its associated repository in the central mapping registry.
     * <p>
     * This method provides thread-safe access to the {@link FrontendMappingMap} for concurrent module registration
     * during application startup or runtime module loading. The mapping associates an entity's form definition with
     * its data access repository, enabling automatic CRUD UI and API generation.
     * </p>
     * <p>
     * Thread Safety: Synchronized to prevent race conditions when multiple modules register mappings concurrently.
     * The {@code frontendMappingMap} is not thread-safe by itself, requiring external synchronization.
     * </p>
     *
     * @param definition the frontend mapping definition containing entity metadata, form fields, and UI configuration
     * @param repository the scoped secure repository providing privilege-aware data access for the entity
     * @see FrontendMappingDefinition
     * @see ScopedSecureRepository
     * @see FrontendMappingMap
     */
    @Override
    public synchronized void registerFrontendMapping(FrontendMappingDefinition definition, ScopedSecureRepository repository) {

        String uniqueName = definition.name;
        frontendMappingMap.put(uniqueName, new FrontendMapping(definition, repository));
    }

    /**
     * Removes a frontend mapping from the central registry by name.
     *
     * @param name unique name of the frontend mapping to unregister
     * @see FrontendMappingMap
     */
    @Override
    public void unregisterFrontendMapping(String name) {
        frontendMappingMap.remove(name);
    }

    /**
     * Registers an HTML CRUD controller for the specified entity with default privilege checking.
     * <p>
     * Creates a web UI controller at {@code /html/{entityName}} with standard CRUD operations (list, view, create,
     * edit, delete) using {@link ReflectionBasedEntityForm} for automatic form generation from entity metadata.
     * </p>
     *
     * @param definition frontend mapping definition describing entity structure and UI configuration
     * @param repository scoped secure repository for privilege-aware data access
     * @return configuration object for the registered CRUD controller
     * @see HtmlCRUDControllerConfigurationMap
     * @see ReflectionBasedEntityForm
     */
    @Override
    public CRUDControllerConfiguration registerHtmlCrudController(FrontendMappingDefinition definition, ScopedSecureRepository repository) {
        return htmlCrudControllerConfigurationMap.registerAndExposeCRUDController(definition, repository, ReflectionBasedEntityForm.class);
    }

    /**
     * Registers an HTML CRUD controller with explicit privilege requirements for read and write operations.
     *
     * @param definition frontend mapping definition describing entity structure
     * @param repository scoped secure repository for data access
     * @param readPrivilege required privilege for read operations (list, view)
     * @param writePrivilege required privilege for write operations (create, edit, delete)
     * @return configuration object for the registered CRUD controller
     * @see PrivilegeBase
     */
    @Override
    public CRUDControllerConfiguration registerHtmlCrudController(FrontendMappingDefinition definition, ScopedSecureRepository repository, PrivilegeBase readPrivilege, PrivilegeBase writePrivilege) {
        return htmlCrudControllerConfigurationMap.registerAndExposeCRUDController(definition, repository, ReflectionBasedEntityForm.class, readPrivilege, writePrivilege);
    }

    /**
     * Unregisters an HTML CRUD controller by entity key, removing all associated routes.
     *
     * @param key entity key of the CRUD controller to unregister
     */
    @Override
    public void unregisterHtmlCrudController(String key) {
        htmlCrudControllerConfigurationMap.unregisterCRUDController(key);
    }

    /**
     * Registers a REST API CRUD controller for the specified entity with default privilege checking.
     * <p>
     * Creates REST API endpoints at {@code /api/{entityName}} with standard operations (GET, POST, PUT, DELETE)
     * using JSON request/response format with {@link ReflectionBasedEntityForm} for data binding.
     * </p>
     *
     * @param definition frontend mapping definition describing entity structure
     * @param repository scoped secure repository for data access
     * @return configuration object for the registered API CRUD controller
     * @see ApiCRUDControllerConfigurationMap
     */
    @Override
    public CRUDControllerConfiguration registerApiCrudController(FrontendMappingDefinition definition, ScopedSecureRepository repository) {
        return apiCrudControllerConfigurationMap.registerCRUDController(definition, repository, ReflectionBasedEntityForm.class);
    }

    /**
     * Registers a REST API CRUD controller with explicit privilege requirements.
     *
     * @param definition frontend mapping definition describing entity structure
     * @param repository scoped secure repository for data access
     * @param readPrivilege required privilege for GET operations
     * @param writePrivilege required privilege for POST, PUT, DELETE operations
     * @return configuration object for the registered API CRUD controller
     */
    @Override
    public CRUDControllerConfiguration registerApiCrudController(FrontendMappingDefinition definition, ScopedSecureRepository repository, PrivilegeBase readPrivilege, PrivilegeBase writePrivilege) {
        return apiCrudControllerConfigurationMap.registerCRUDController(definition, repository, ReflectionBasedEntityForm.class, readPrivilege, writePrivilege);
    }

    /**
     * Unregisters a REST API CRUD controller by entity key, removing all associated endpoints.
     *
     * @param key entity key of the API CRUD controller to unregister
     */
    @Override
    public void unregisterApiCrudController(String key) {
        apiCrudControllerConfigurationMap.unregisterCRUDController(key);
    }

    /**
     * Spring event listener triggered when the ApplicationContext is refreshed or initialized.
     * <p>
     * This method serves as the entry point for application bootstrap, responding to Spring's
     * {@link ContextRefreshedEvent} which is published when the ApplicationContext is initialized or refreshed.
     * It delegates to {@link #onApplicationStart()} to execute the full bootstrap sequence, then emits a
     * {@link CoreSettledEvent} to signal that core initialization has completed.
     * </p>
     * <p>
     * The class equality check {@code this.getClass().equals(BasicCustomisationService.class)} ensures that
     * bootstrap only executes for the base implementation, preventing duplicate initialization if subclasses
     * override this method.
     * </p>
     * <p>
     * Event Flow:
     * <ol>
     * <li>Spring ApplicationContext initialization completes</li>
     * <li>ContextRefreshedEvent published by Spring container</li>
     * <li>This @EventListener method invoked</li>
     * <li>{@link #onApplicationStart()} executes full bootstrap</li>
     * <li>CoreSettledEvent returned to signal readiness</li>
     * </ol>
     * </p>
     *
     * @param event the Spring context refresh event triggering application bootstrap
     * @return CoreSettledEvent if bootstrap was executed, null if skipped due to subclass override
     * @see ContextRefreshedEvent
     * @see CoreSettledEvent
     * @see EventListener
     */
    @EventListener(ContextRefreshedEvent.class)
    public CoreSettledEvent onApplicationEvent(ContextRefreshedEvent event) {
        if(this.getClass().equals(BasicCustomisationService.class)) {
            onApplicationStart();
            return new CoreSettledEvent();
        }
        return null;
    }

    /**
     * Returns the service for managing and persisting frontend mapping definitions.
     *
     * @return the frontend mapping definition service instance
     * @see FrontendMappingDefinitionService
     */
    public FrontendMappingDefinitionService getFrontendMappingDefinitionService() {
        return frontendMappingDefinitionService;
    }
}
