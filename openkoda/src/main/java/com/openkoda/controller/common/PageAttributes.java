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

package com.openkoda.controller.common;

import com.openkoda.controller.PageBuilderController;
import com.openkoda.core.audit.SystemHealthStatus;
import com.openkoda.core.flow.BasePageAttributes;
import com.openkoda.core.flow.PageAttr;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.flow.mbean.LoggingEntriesStack;
import com.openkoda.core.form.AbstractForm;
import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.core.form.FrontendMappingFieldDefinition;
import com.openkoda.core.helper.ReadableCode;
import com.openkoda.core.repository.common.ProfileSettingsRepository;
import com.openkoda.dto.CanonicalObject;
import com.openkoda.dto.OrganizationDto;
import com.openkoda.dto.OrganizationRelatedObject;
import com.openkoda.dto.ServerJsThreadDto;
import com.openkoda.dto.web.OrganizationWebPageDto;
import com.openkoda.dto.web.WebPage;
import com.openkoda.form.*;
import com.openkoda.integration.model.configuration.IntegrationModuleOrganizationConfiguration;
import com.openkoda.model.*;
import com.openkoda.model.authentication.ApiKey;
import com.openkoda.model.common.Audit;
import com.openkoda.model.common.SearchableOrganizationRelatedEntity;
import com.openkoda.model.component.ControllerEndpoint;
import com.openkoda.model.component.Form;
import com.openkoda.model.component.FrontendResource;
import com.openkoda.model.component.Scheduler;
import com.openkoda.model.component.event.EventListenerEntry;
import com.openkoda.model.file.File;
import com.openkoda.model.notification.Notification;
import com.openkoda.repository.notifications.NotificationKeeper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import reactor.util.function.Tuple5;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Centralized repository of type-safe page attribute keys for OpenKoda MVC controllers and views.
 * <p>
 * This interface provides compile-time validated constants for accessing model attributes in Spring MVC
 * controllers and Thymeleaf templates. By using {@link PageAttr} typed constants instead of string keys,
 * the framework ensures type safety when populating {@link PageModelMap} and accessing data in views.
 * </p>
 * <p>
 * The PageAttr pattern prevents common runtime errors such as typos in attribute names, incorrect type
 * casting, and missing model attributes. Each constant encapsulates both the attribute name and its
 * expected type, enabling IDE autocomplete and refactoring support.
 * </p>
 * <p>
 * <b>Usage in Controllers:</b>
 * <pre>{@code
 * pageModel.put(PageAttributes.organizationEntity, organization);
 * pageModel.put(PageAttributes.userPage, userRepository.findAll(pageable));
 * }</pre>
 * </p>
 * <p>
 * <b>Usage in Thymeleaf Templates:</b>
 * <pre>{@code
 * <div th:text="${organizationEntity.name}">Organization Name</div>
 * <table th:each="user : ${userPage.content}">...</table>
 * }</pre>
 * </p>
 * <p>
 * Constants are grouped by type:
 * <ul>
 *   <li><b>Entity attributes</b> - Single entity instances (organizationEntity, userEntity, roleEntity)</li>
 *   <li><b>Page attributes</b> - Paginated collections (organizationPage, userPage, auditPage)</li>
 *   <li><b>Form attributes</b> - Form binding objects with default constructors (organizationForm, roleForm)</li>
 *   <li><b>DTO attributes</b> - Data transfer objects (organizationDto, webPageDto)</li>
 *   <li><b>Collection attributes</b> - Maps and lists (organizationAttributes, unreadNotificationsList)</li>
 *   <li><b>Configuration attributes</b> - System settings (emailConfig, systemHealthStatus)</li>
 *   <li><b>Request attributes</b> - Request-scoped data (requestId, clientToken)</li>
 *   <li><b>View attributes</b> - UI rendering data (modelAndView, defaultLayout)</li>
 * </ul>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see PageAttr
 * @see PageModelMap
 * @see BasePageAttributes
 */
public interface PageAttributes extends BasePageAttributes, ReadableCode {

    /** String constant for organization entity attribute name. */
    String ORGANIZATION_ENTITY = "organizationEntity";
    
    /** String constant for organization entity ID attribute name. */
    String ORGANIZATION_ENTITY_ID = "organizationEntityId";
    
    /** String constant for organization custom attributes map. */
    String ORGANIZATION_ATTRIBUTES = "organizationAttributes";
    
    /** String constant for organization dictionaries JSON serialization. */
    String ORGANIZATION_DICTIONARIES_JSON = "organizationDictionariesJson";
    
    /** String constant for global user attributes accessible across all organizations. */
    String GLOBAL_USER_ATTRIBUTES = "globalUserAttributes";

    /** String constant for common dictionaries shared across the application. */
    String COMMON_DICTIONARIES = "commonDictionaries";
    
    /** String constant for names of available common dictionaries. */
    String COMMON_DICTIONARIES_NAMES = "commonDictionariesNames";
    
    /** String constant for organization-scoped user attributes. */
    String ORGANIZATION_USER_ATTRIBUTES = "organizationUserAttributes";

    // ============================================
    // Request and Session Attributes
    // ============================================

    /** Unique request identifier for tracing and correlation across logs. */
    PageAttr<String > requestId = new PageAttr<>("requestId");
    
    // ============================================
    // Dictionary and Configuration Attributes
    // ============================================
    
    /** Common application-wide dictionaries as JSON string. */
    PageAttr<String > commonDictionaries = new PageAttr<>(COMMON_DICTIONARIES);
    
    /** Set of names for available common dictionaries. */
    PageAttr<Set<String> > commonDictionariesNames = new PageAttr<>(COMMON_DICTIONARIES_NAMES);
    
    /** Organization-specific dictionaries serialized as JSON. */
    PageAttr<String > organizationDictionariesJson = new PageAttr<>(ORGANIZATION_DICTIONARIES_JSON);
    
    /** Custom key-value attributes for organization configuration. */
    PageAttr<Map<String, String> > organizationAttributes = new PageAttr<>(ORGANIZATION_ATTRIBUTES);
    
    /** Organization-scoped custom attributes for the current user. */
    PageAttr<Map<String, String> > organizationUserAttributes = new PageAttr<>(ORGANIZATION_USER_ATTRIBUTES);
    
    /** Global user attributes accessible across all organizations. */
    PageAttr<Map<String, String> > globalUserAttributes = new PageAttr<>(GLOBAL_USER_ATTRIBUTES);
    
    /** Search results mapped by entity ID to display string. */
    PageAttr<Map<Long, String> > searchResultPage = new PageAttr<>("searchResultPage");
    
    // ============================================
    // Entity ID Attributes
    // ============================================
    
    /** Plan entity identifier for subscription or billing context. */
    PageAttr<String> planEntityId = new PageAttr<>("planEntityId");
    
    /** Primary key of the current organization entity. */
    PageAttr<Long> organizationEntityId = new PageAttr<>(ORGANIZATION_ENTITY_ID);
    
    /** Primary key of the current user entity. */
    PageAttr<Long> userEntityId = new PageAttr<>("userEntityId");
    
    // ============================================
    // Organization Entity and DTO Attributes
    // ============================================
    
    /** Current organization entity instance for multi-tenant operations. */
    PageAttr<Organization> organizationEntity = new PageAttr<>(ORGANIZATION_ENTITY);
    
    /** Organization data transfer object for view rendering. */
    PageAttr<OrganizationDto> organizationDto = new PageAttr<>("organizationDto");
    
    /** Form for creating or editing organization details with default constructor. */
    PageAttr<OrganizationForm> organizationForm = new PageAttr<>("organizationForm", () -> new OrganizationForm());
    
    // ============================================
    // Email and Integration Configuration
    // ============================================
    
    /** Form for configuring organization email settings with default constructor. */
    PageAttr<EmailConfigForm> emailConfigForm = new PageAttr<>("emailConfigForm", () -> new EmailConfigForm());
    
    /** Email configuration entity for the current organization. */
    PageAttr<EmailConfig> emailConfig = new PageAttr<>("emailConfig");
    
    /** System health status for third-party integrations. */
    PageAttr<SystemHealthStatus> integrations = new PageAttr<>("integrations");
    
    // ============================================
    // User Management Forms
    // ============================================
    
    /** Form for editing existing user account details. */
    PageAttr<EditUserForm> editUserForm = new PageAttr<>("editUserForm");
    
    /** Form for inviting new users to the organization. */
    PageAttr<InviteUserForm> inviteUserForm = new PageAttr<>("inviteUserForm");
    
    /** Form for assigning global and organization-specific roles. */
    PageAttr<GlobalOrgRoleForm> globalOrgRoleForm = new PageAttr<>("globalOrgRoleForm");
    
    // ============================================
    // Paginated Entity Collections
    // ============================================
    
    /** Paginated collection of organizations for list views. */
    PageAttr<Page<Organization>> organizationPage = new PageAttr<>("organizationPage");
    
    /** Single audit log entry entity for detail views. */
    PageAttr<Audit> auditEntity = new PageAttr<>("auditEntity");
    
    /** Current user entity instance for profile and permissions. */
    PageAttr<User> userEntity = new PageAttr<>("userEntity");
    
    /** Paginated collection of users for administration views. */
    PageAttr<Page<User>> userPage = new PageAttr<>("userPage");
    
    /** Paginated collection of audit log entries. */
    PageAttr<Page<Audit>> auditPage = new PageAttr<>("auditPage");
    
    // ============================================
    // Generic Query Parameters
    // ============================================
    
    /** Generic Long entity identifier for flexible entity operations. */
    PageAttr<Long> longEntityId = new PageAttr<>("longEntityId");
    
    /** Pagination parameters for query results (page number, size, sort). */
    PageAttr<Pageable> pageable = new PageAttr<>("pageable");
    
    /** Search query term for filtering entity lists. */
    PageAttr<String> searchTerm = new PageAttr<>("searchTerm");
    
    /** JPA specification for dynamic query construction. */
    PageAttr<Specification> specification = new PageAttr<>("specification");
    
    // ============================================
    // Authentication and Account Management URLs
    // ============================================
    
    /** URL for password recovery email links. */
    PageAttr<String> passwordRecoveryLink = new PageAttr<>("passwordRecoveryLink");
    
    /** URL for account email verification links. */
    PageAttr<String> accountVerificationLink = new PageAttr<>("accountVerificationLink");
    
    /** Base website URL for generating absolute links. */
    PageAttr<String> websiteUrl = new PageAttr<>("websiteUrl");
    
    // ============================================
    // Role-Based Access Control (RBAC) Attributes
    // ============================================
    
    /** Paginated collection of roles for administration views. */
    PageAttr<Page<Role>> rolePage = new PageAttr<>("rolePage");
    
    /** Paginated collection of system privileges. */
    PageAttr<Page<PrivilegeBase>> privilegePage = new PageAttr<>("privilegePage");
    
    /** Single role entity instance for detail and edit views. */
    PageAttr<Role> roleEntity = new PageAttr<>("roleEntity");
    
    /** List of all available privilege enumerations for assignment. */
    PageAttr<List<PrivilegeBase>> rolesEnum = new PageAttr<>("rolesEnum");
    
    /** Form for creating or editing role definitions. */
    PageAttr<RoleForm> roleForm = new PageAttr<>("roleForm");
    
    /** Form for creating or editing privilege assignments. */
    PageAttr<PrivilegeForm> privilegeForm = new PageAttr<>("privilegeForm");
    
    /** Single privilege entity instance for detail views. */
    PageAttr<PrivilegeBase> privilegeEntity = new PageAttr<>("privilegeEntity");
    
    /** List of dynamically generated privileges for custom entities. */
    PageAttr<List<DynamicPrivilege>> dynamicPrivilegesList = new PageAttr<>("dynamicPrivilegesList");
    
    // ============================================
    // Frontend Resource and Page Builder Attributes
    // ============================================
    
    /** Paginated collection of frontend resources (JavaScript, CSS, HTML). */
    PageAttr<Page<FrontendResource>> frontendResourcePage = new PageAttr<>("frontendResourcePage");
    
    /** Single frontend resource entity for editing or viewing. */
    PageAttr<FrontendResource> frontendResourceEntity = new PageAttr<>("frontendResourceEntity");
    
    /** Form for creating or editing frontend resource definitions. */
    PageAttr<FrontendResourceForm> frontendResourceForm = new PageAttr<>("frontendResourceForm");
    
    /** Form for visual page builder with drag-and-drop components. */
    PageAttr<PageBuilderForm> pageBuilderForm = new PageAttr<>("pageBuilderForm");
    
    /** Form for AI-assisted data operations and queries. */
    PageAttr<PageBuilderForm> dataAiForm = new PageAttr<>("dataAiForm");
    
    /** Form for defining frontend resource page metadata. */
    PageAttr<FrontendResourcePageForm> frontendResourcePageForm = new PageAttr<>("frontendResourcePageForm");
    
    // ============================================
    // Dynamic Controller and UI Component Attributes
    // ============================================
    
    /** Single controller endpoint definition for dynamic routing. */
    PageAttr<ControllerEndpoint> controllerEndpoint = new PageAttr<>("controllerEndpoint");
    
    /** List of all registered controller endpoints. */
    PageAttr<List<ControllerEndpoint>> controllerEndpoints = new PageAttr<>("controllerEndpoints");
    
    /** Model data for UI component rendering. */
    PageAttr<PageModelMap> uiComponentModel = new PageAttr<>("uiComponentModel");
    
    /** Result object from dynamic controller endpoint execution. */
    PageAttr<Object> controllerEndpointResult = new PageAttr<>("controllerEndpointResult");
    
    /** Preview URL for UI component development mode. */
    PageAttr<String> uiComponentPreviewUrl = new PageAttr<>("uiComponentPreviewUrl");
    
    /** Production URL for deployed UI component. */
    PageAttr<String> uiComponentUrl = new PageAttr<>("uiComponentUrl");
    
    // ============================================
    // Event-Driven Architecture Attributes
    // ============================================
    
    /** Form for creating or editing event listener configurations. */
    PageAttr<EventListenerForm> eventListenerForm = new PageAttr<>("eventListenerForm");
    
    /** Generic form for sending custom events to the event bus. */
    PageAttr<AbstractForm<?>> sendEventForm = new PageAttr<AbstractForm<?>>("sendEventForm");
    
    /** Form for creating or editing scheduled job configurations. */
    PageAttr<SchedulerForm> schedulerForm = new PageAttr<>("schedulerForm");
    
    /** Paginated collection of registered event listeners. */
    PageAttr<Page<EventListenerEntry>> eventListenerPage = new PageAttr<>("eventListenerPage");
    
    /** Paginated collection of scheduled jobs. */
    PageAttr<Page<Scheduler>> schedulerPage = new PageAttr<>("schedulerPage");
    
    /** Single event listener entry for detail or edit views. */
    PageAttr<EventListenerEntry> eventListenerEntity = new PageAttr<>("eventListenerEntity");
    
    /** Event listener entity scheduled for unregistration. */
    PageAttr<EventListenerEntry> eventListenerEntityToUnregister = new PageAttr<>("eventListenerEntityToUnregister");
    
    /** Single scheduler entity for job configuration. */
    PageAttr<Scheduler> schedulerEntity = new PageAttr<>("schedulerEntity");
    
    // ============================================
    // Dynamic Form and Token Attributes
    // ============================================
    
    /** Dynamic form definition entity for runtime form generation. */
    PageAttr<Form> formEntity = new PageAttr<>("formEntity");
    
    /** Client-side token for CSRF protection or session validation. */
    PageAttr<String> clientToken = new PageAttr<>("clientToken");
    
    /** Authentication token entity for API access. */
    PageAttr<Token> tokenEntity = new PageAttr<>("tokenEntity");
    
    /** Organization-specific configuration for integration modules. */
    PageAttr<IntegrationModuleOrganizationConfiguration> organizationIntegrationModuleConfiguration = new PageAttr<>
            ("organizationIntegrationModuleConfiguration");
    
    // ============================================
    // Logging and Debug Attributes
    // ============================================
    
    /** List of captured log entries as key-value pairs. */
    PageAttr<List<Map.Entry<String, String>>> logsEntryList = new PageAttr<>("logsEntryList");
    
    /** Form for configuring logger levels and filters. */
    PageAttr<LoggerForm> loggerForm = new PageAttr<>("loggerForm");
    
    /** List of class names available for log level configuration. */
    PageAttr<List<String>> logClassNamesList = new PageAttr<>("logClassNamesList");
    
    /** List of command-line or method arguments for debugging. */
    PageAttr<List<String>> arguments = new PageAttr<>("arguments");
    
    /** Buffer size for log or data stream operations. */
    PageAttr<Integer> bufferSize = new PageAttr<>("bufferSize");
    
    // ============================================
    // Search and Organization-Related Entities
    // ============================================
    
    /** Paginated global search results across all entity types. */
    PageAttr<Page<GlobalEntitySearch>> searchPage = new PageAttr<>("searchPage");
    
    /** Single organization-related object for multi-tenant operations. */
    PageAttr<OrganizationRelatedObject> organizationRelatedObject = new PageAttr<>("organizationRelatedObject");
    
    /** Map representation of organization-related object fields. */
    PageAttr<Map<String, Object>> organizationRelatedObjectMap = new PageAttr<>("organizationRelatedObjectMap");
    
    /** Unique key for identifying organization-related objects. */
    PageAttr<String> organizationRelatedObjectKey = new PageAttr<>("organizationRelatedObjectKey");
    
    /** Single searchable entity scoped to an organization. */
    PageAttr<SearchableOrganizationRelatedEntity> organizationRelatedEntity = new PageAttr<>("organizationRelatedEntity");
    
    /** Paginated collection of organization-scoped entities. */
    PageAttr<Page<SearchableOrganizationRelatedEntity>> organizationRelatedEntityPage = new PageAttr<>("organizationRelatedEntityPage");
    
    // ============================================
    // View Rendering and Layout Attributes
    // ============================================
    
    /** Spring MVC ModelAndView for programmatic view resolution. */
    PageAttr<ModelAndView> modelAndView = new PageAttr<>("modelAndView");
    
    /** Default Thymeleaf layout template for page rendering. */
    PageAttr<String> defaultLayout = new PageAttr<>("defaultLayout");
    
    /** Version string for cache-busting static resources. */
    PageAttr<String> resourcesVersion = new PageAttr<>("resourcesVersion");
    
    /** Web page data transfer object for content management. */
    PageAttr<WebPage> webPageDto = new PageAttr<>("webPageDto");
    
    /** Organization-scoped web page DTO for tenant-specific content. */
    PageAttr<OrganizationWebPageDto> organizationWebPageDto = new PageAttr<>("organizationWebPageDto");
    
    // ============================================
    // Notification System Attributes
    // ============================================
    
    /** List of notifications already marked as read by the user. */
    PageAttr<List<Notification>> readNotificationsList = new PageAttr<>("readNotificationsList");
    
    /** List of unread notifications for the current user. */
    PageAttr<List<Notification>> unreadNotificationsList = new PageAttr<>("unreadNotificationsList");
    
    /** Map of server-side JavaScript threads with their logging stacks. */
    PageAttr<Map<ServerJsThreadDto, LoggingEntriesStack<String>>> serverJsThreads = new PageAttr<>("serverJsThreads");
    
    /** Comma-separated string of unread notification IDs. */
    PageAttr<String> unreadNotificationsIdListString = new PageAttr<>("unreadNotificationsIdListString");
    
    /** Flash message for user notification display. */
    PageAttr<String> notificationMessage = new PageAttr<>("notificationMessage");
    
    /** Active menu item identifier for navigation highlighting. */
    PageAttr<String> menuItem = new PageAttr<>("menuItem");
    
    // ============================================
    // System Health and Maintenance Attributes
    // ============================================
    
    /** Current system health status with integration checks. */
    PageAttr<SystemHealthStatus> systemHealthStatus = new PageAttr<>("systemHealthStatus");
    
    /** SQL script for database schema updates or migrations. */
    PageAttr<String> databaseUpdateScript = new PageAttr<>("databaseUpdateScript");
    
    /** Canonical object representation for SEO and metadata. */
    PageAttr<CanonicalObject> canonicalObject = new PageAttr<>("canonicalObject");

    /** Count of unread notifications for badge display. */
    PageAttr<Integer> unreadNotificationsNumber = new PageAttr<>("unreadNotificationsNumber");
    
    /** Paginated collection of notification keeper records. */
    PageAttr<Page<NotificationKeeper>> notificationPage = new PageAttr<>("notificationPage");
    
    // ============================================
    // Script Execution and API Access Attributes
    // ============================================
    
    /** Result object from server-side script execution (JavaScript, Groovy). */
    PageAttr<Object> scriptResult = new PageAttr<>("scriptResult");
    
    /** Paginated collection of organization DTOs for list views. */
    PageAttr<Page<OrganizationDto>> organizationDtoPage = new PageAttr<>("organizationDtoPage");
    
    /** API key entity for programmatic access authentication. */
    PageAttr<ApiKey> apiKeyEntity = new PageAttr<>("apiKeyEntity");
    
    /** Plain text API key string shown once at creation. */
    PageAttr<String> plainApiKeyString = new PageAttr<>("plainApiKeyString");
    
    /** Base application URL for constructing absolute links. */
    PageAttr<String> baseUrl = new PageAttr<>("baseUrl");
    
    // ============================================
    // Error Handling Attributes
    // ============================================
    
    /** User-friendly error message for display. */
    PageAttr<String> errorMessage = new PageAttr<>("errorMessage");
    
    /** HTTP response headers for custom error responses. */
    PageAttr<HttpHeaders> httpHeaders = new PageAttr<>("httpHeaders");
    
    /** HTTP status code for error responses. */
    PageAttr<HttpStatus> errorHttpStatus = new PageAttr<>("errorHttpStatus");
    
    /** List of error messages for validation or batch operations. */
    PageAttr<List<String>> errorList = new PageAttr<>("errorList");
    
    // ============================================
    // Generic Form and Dynamic View Attributes
    // ============================================
    
    /** Form for organization-related entities with tenant scoping. */
    PageAttr<AbstractOrganizationRelatedEntityForm> organizationRelatedForm = new PageAttr<>("organizationRelatedForm");
    
    /** Frontend mapping definition for dynamic form and view generation. */
    PageAttr<FrontendMappingDefinition> frontendMappingDefinition = new PageAttr<>("frontendMappingDefinition");
    
    /** Generic table data as list of rows (each row is a list of values). */
    PageAttr<List<List<Object>>> genericTableViewList = new PageAttr<>("genericTableViewList");
    
    /** Generic table data as list of maps (each row is a key-value map). */
    PageAttr<List<Map<String,Object>>> genericTableViewMap = new PageAttr<>("genericTableViewMap");
    
    /** Generic report data with preserved column order via LinkedHashMap. */
    PageAttr<List<LinkedHashMap<String,Object>>> genericReportViewLinkedHashMap = new PageAttr<>("genericReportViewLinkedHashMap");
    
    /** Column header definitions for generic table views. */
    PageAttr<List<FrontendMappingFieldDefinition>> genericTableViewHeaders = new PageAttr<>("genericTableViewHeaders");
    
    /** Simple string array of table header names. */
    PageAttr<String[]> genericTableHeaders = new PageAttr<>("genericTableHeaders");
    
    /** Filter definitions for generic table views. */
    PageAttr<List<FrontendMappingFieldDefinition>> genericTableFilters = new PageAttr<>("genericTableFilters");
    
    /** Report identifier for retrieving saved report definitions. */
    PageAttr<Long> reportId = new PageAttr<>("reportId");
    
    /** Object filters as key-value pairs for dynamic queries. */
    PageAttr<Map<String,String>> objFilters = new PageAttr<>("objFilters");
    
    /** Navigation fragment for generic view breadcrumbs. */
    PageAttr<String> genericViewNavigationFragment = new PageAttr<>("genericViewNavigationFragment");
    
    /** Flag indicating whether entity is a MapEntity with key-value storage. */
    PageAttr<Boolean> isMapEntity = new PageAttr<>("isMapEntity");
    
    /** Flag indicating whether page editor mode is active. */
    PageAttr<Boolean> isPageEditor = new PageAttr<>("isPageEditor");
    
    /** Selected elements in rule builder interface. */
    PageAttr<Map<Object, Object[]>> rulesSelectedElements = new PageAttr<>("rulesSelectedElements");
    
    /** List of additional settings forms for plugin configuration. */
    PageAttr<List<Tuple5<ProfileSettingsRepository, Function, PageAttr, String, String>>> additionalSettingsForms = new PageAttr<>("additionalSettingsForms");
    
    // ============================================
    // Form Validation and Navigation Attributes
    // ============================================
    
    /** Flag indicating form validation success status. */
    PageAttr<Boolean> isValid = new PageAttr<>("isValid");
    
    /** URL for post-submission redirect. */
    PageAttr<String> redirectUrl = new PageAttr<>("redirectUrl");
    
    /** Flag indicating whether to reload the page after operation. */
    PageAttr<String> reload = new PageAttr<>("reload");
    
    /** Collection of embeddable UI components for page builder. */
    PageAttr<PageBuilderController.EmbeddableComponents> embeddableComponents = new PageAttr<>("embeddableComponents");
    
    /** Log output from import operations for user feedback. */
    PageAttr<String> importLog = new PageAttr<>("importLog");
    
    // ============================================
    // File and Query Attributes
    // ============================================
    
    /** File entity for upload, download, or attachment operations. */
    PageAttr<File> file = new PageAttr<>("file");
    
    /** Query string for search or filter operations. */
    PageAttr<String> query = new PageAttr<>("query");
    
    /** Name of the file for display or download headers. */
    PageAttr<String> fileName = new PageAttr<>("fileName");
    
    /** Build information map with version, timestamp, and commit details. */
    PageAttr<Map<String, Object>> buildInfo = new PageAttr<>("buildInfo");
    
    /** Flag indicating whether entity supports audit trail tracking. */
    PageAttr<Boolean> isAuditable = new PageAttr<>("isAuditable");

    // ============================================
    // URL Parameter Attributes
    // ============================================
    
    /** Remaining unprocessed URL parameters as string. */
    PageAttr<String> remainingParameters = new PageAttr<>("remainingParameters");
    
    /** Remaining URL parameters parsed into key-value map. */
    PageAttr<Map<String, String>> remainingParametersMap = new PageAttr<>("remainingParametersMap");

    /**
     * Creates a function that converts paginated collections from one type to another.
     * <p>
     * This utility method transforms Spring Data {@link Page} objects by applying an element
     * converter function to each item in the page content. The pagination metadata (page number,
     * size, total elements) is preserved while converting the content elements.
     * </p>
     * <p>
     * Common use case is converting entity pages to DTO pages for view rendering:
     * <pre>{@code
     * Page<Organization> entityPage = organizationRepository.findAll(pageable);
     * Page<OrganizationDto> dtoPage = entityPage.map(OrganizationDto::new);
     * }</pre>
     * </p>
     *
     * @param <S> source element type in the original page
     * @param <T> target element type in the converted page
     * @param elementConverter function to convert individual elements from source to target type
     * @return function that converts {@code Page<S>} to {@code Page<T>}
     * @see Page#map(Function)
     * @see PageModelMap
     */
    default <S, T> Function<Page<S>, Page<T>> pageConverter(Function<? super S, ? extends T> elementConverter) {
        return (page) -> page.map(elementConverter);
    }
}
