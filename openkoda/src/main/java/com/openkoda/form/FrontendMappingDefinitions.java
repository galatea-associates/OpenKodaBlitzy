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

package com.openkoda.form;

import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.core.form.ReflectionBasedEntityForm;
import com.openkoda.core.multitenancy.MultitenancyService;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.core.service.event.EventConsumerCategory;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.component.FrontendResource;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

import static com.openkoda.controller.common.URLConstants.FRONTENDRESOURCEREGEX;
import static com.openkoda.controller.common.URLConstants.ORGANIZATION;
import static com.openkoda.core.form.FrontendMappingDefinition.createFrontendMappingDefinition;
import static com.openkoda.core.form.Validator.notBlank;
import static com.openkoda.model.Privilege.*;

/**
 * Authoritative large DSL-style collection of {@link FrontendMappingDefinition} instances.
 * <p>
 * This interface centralizes field builders, per-field validators, value suppliers and privilege 
 * predicates used application-wide by form adapters and controllers. All mappings are created at 
 * class-load time and reused across requests, providing pre-built definitions for user, role, 
 * privilege, organization, file, scheduler, email config, event listener, frontend resource, 
 * page builder, and other forms.
 * </p>
 * <p>
 * <strong>DSL-Style Construction Pattern:</strong><br>
 * FrontendMappingDefinition instances are constructed using {@code createFrontendMappingDefinition}
 * with field builder lambdas. Each mapping specifies:
 * <ul>
 *   <li>Form name constant (e.g., {@code USER_FORM}, {@code ROLE_FORM})</li>
 *   <li>Read privilege predicate ({@link PrivilegeBase} for data access)</li>
 *   <li>Write privilege predicate ({@link PrivilegeBase} for data modification)</li>
 *   <li>Field builder lambda with DSL methods: {@code text()}, {@code checkbox()}, {@code select()}, 
 *       {@code multiselect()}, {@code textarea()}, {@code date()}, {@code dropdown()}, {@code organizationSelect()}, 
 *       {@code colorPicker()}, {@code image()}, {@code number()}, etc.</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Privilege-Based Access Control:</strong><br>
 * Each mapping declares read/write privilege predicates using {@link PrivilegeBase} instances from 
 * {@link com.openkoda.model.Privilege}. Additional field-level privileges can be specified via 
 * {@code additionalPrivileges()} for fine-grained control.
 * </p>
 * <p>
 * <strong>Value Suppliers:</strong><br>
 * Fields can specify {@code valueSupplier} functions for dynamic field population from entity state. 
 * Example: {@code .valueSupplier(f -> ((Entity)f.entity).getComputedValue())}
 * </p>
 * <p>
 * <strong>Custom Validators:</strong><br>
 * Per-field validators using {@code validate()} lambda provide business rule enforcement.
 * Example: {@code .text(NAME_).validate(v -> v.matches(REGEX) ? null : "error.code")}
 * </p>
 * <p>
 * <strong>Thread-Safety and Initialization:</strong><br>
 * All {@link FrontendMappingDefinition} constants are initialized at class-load time and are 
 * effectively immutable after initialization. They are safe to reference from multiple threads 
 * and form instances.
 * </p>
 * <p>
 * <strong>Usage by Form Classes:</strong><br>
 * Form classes extending {@link com.openkoda.core.form.AbstractForm} or 
 * {@link com.openkoda.core.form.AbstractEntityForm} reference these pre-built definitions in their 
 * constructors. Example: {@code new UserForm(entity, FrontendMappingDefinitions.userForm)}
 * </p>
 *
 * @see FrontendMappingDefinition
 * @see com.openkoda.core.form.AbstractForm
 * @see com.openkoda.core.form.AbstractEntityForm
 * @see BasicUserForm
 * @see RoleForm
 * @see PrivilegeForm
 * @see OrganizationForm
 * @see EmailConfigForm
 * @see SchedulerForm
 * @see FrontendResourceForm
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public interface FrontendMappingDefinitions extends HasSecurityRules, TemplateFormFieldNames {

    /** Form identifier constant for user entity form. */
    String USER_FORM = "userForm";
    
    /** Form identifier constant for scheduler entity form with cron expression validation. */
    String SCHEDULER_FORM = "schedulerForm";
    
    /** Form identifier constant for role entity form with privilege set management. */
    String ROLE_FORM = "roleForm";
    
    /** Form identifier constant for dynamic privilege entity form. */
    String PRIVILEGE_FORM = "privilegeForm";
    
    /** Form identifier constant for organization tenant entity form with branding. */
    String ORGANIZATION_FORM = "organizationForm";
    
    /** Form identifier constant for logger configuration form. */
    String LOGGER_FORM = "loggerForm";
    
    /** Form identifier constant for frontend resource form. */
    String FRONTEND_RESOURCE_FORM = "frontendResource";
    
    /** Form identifier constant for page builder form. */
    String PAGE_BUILDER_FORM = "pageBuilder";
    
    /** Form identifier constant for module configuration form. */
    String MODULE_FORM = "moduleForm";
    
    /** Form identifier constant for frontend resource page form with URL path validation. */
    String FRONTEND_RESOURCE_PAGE_FORM = "frontendResourcePageForm";
    
    /** Form identifier constant for query report form. */
    String QUERY_REPORT_FORM = "queryReport";
    
    /** Form identifier constant for UI component frontend resource form. */
    String UI_COMPONENT_FRONTEND_RESOURCE_FORM = "uiComponentFrontendResource";
    
    /** Form identifier constant for controller endpoint form. */
    String CONTROLLER_ENDPOINT_FORM = "controllerEndpointForm";
    
    /** Form identifier constant for event listener form with consumer/event descriptor validation. */
    String EVENT_LISTENER_FORM = "eventListenerForm";
    
    /** Form identifier constant for generic event dispatch form. */
    String SEND_EVENT_FORM = "sendEventForm";
    
    /** Form identifier constant for custom event creation form. */
    String SEND_CUSTOM_EVENT_FORM = "sendCustomEventForm";
    
    /** Form identifier constant for event definition form. */
    String CREATE_EVENT_FORM = "createEventForm";
    
    /** Form identifier constant for attribute definition form. */
    String ATTRIBUTE_DEFINITION_FORM = "attributeDefinitionForm";
    
    /** Form identifier constant for user editing form with enabled/language fields. */
    String EDIT_USER_FORM = "editUserForm";
    
    /** Form identifier constant for global organization role assignment form. */
    String GLOBAL_ORG_ROLE_FORM = "globalOrgRoleForm";
    
    /** Form identifier constant for user invitation form with role name validation. */
    String INVITE_USER_FORM_NAME = "inviteUserForm";
    
    /** CSS class constant for compact table layout in checkbox lists. */
    String TABLE_COMPACT_CSS = "table-compact ";
    
    /** Form identifier constant for email configuration form with SMTP/Mailgun validation. */
    String EMAIL_CONFIG_FORM = "emailConfigForm";

    /**
     * Pre-built {@link FrontendMappingDefinition} for Role entity form.
     * <p>
     * Provides field definitions for role entity management with name, type selection, and 
     * privilege set assignment. Fields include:
     * <ul>
     *   <li>{@code NAME_} - Text field for role name</li>
     *   <li>{@code TYPE_} - Dropdown for role type selection (GlobalRole, OrganizationRole, GlobalOrganizationRole)</li>
     *   <li>{@code PRIVILEGES_} - Grouped checkbox list for privilege set assignment with compact CSS</li>
     * </ul>
     * Access controlled by {@code canReadBackend} and {@code canManageBackend} privileges.
     * </p>
     * 
     * @see RoleForm
     * @see com.openkoda.model.Role
     */
    FrontendMappingDefinition roleForm = createFrontendMappingDefinition(ROLE_FORM, canReadBackend, canManageBackend,
        a -> a  .text(NAME_)
                .dropdown(TYPE_, ROLE_TYPES_)
                .checkboxListGrouped(PRIVILEGES_, "privilegesGrouped").additionalCss(TABLE_COMPACT_CSS)
                );
    
    /**
     * Pre-built {@link FrontendMappingDefinition} for dynamic Privilege entity form.
     * <p>
     * Provides field definitions for dynamic privilege management including name, label, category,
     * and privilege group assignment. Fields include:
     * <ul>
     *   <li>{@code ID_} - Text field for privilege ID (disabled for existing, hidden for new)</li>
     *   <li>{@code NAME_} - Text field for privilege name (enabled only for new privileges)</li>
     *   <li>{@code LABEL_} - Text field for human-readable privilege label</li>
     *   <li>{@code category} - Text field for privilege category classification</li>
     *   <li>{@code privilegeGroup} - Dropdown for privilege group selection</li>
     * </ul>
     * ID field visibility controlled by conditional logic: visible only when entity ID exists.
     * Access controlled by {@code canReadBackend} and {@code canManageBackend} privileges.
     * </p>
     * 
     * @see PrivilegeForm
     * @see com.openkoda.model.Privilege
     * @see com.openkoda.model.DynamicPrivilege
     */
    FrontendMappingDefinition privilegeForm = createFrontendMappingDefinition(PRIVILEGE_FORM, canReadBackend, canManageBackend,
            a -> a  .text(ID_)
                        .enabled((c, entityId) -> false)
                        .visible((c, entityId) -> !(entityId == null || entityId.getId() == null || entityId.getId() == 0))
                    .text(NAME_)
                        .enabled((c, entityId) -> entityId == null || entityId.getId() == null || entityId.getId() == 0)
                    .text(LABEL_)
                    .text("category")
                    .dropdown("privilegeGroup", PRIVILEGE_GROUPS_)
                    .additionalCss(TABLE_COMPACT_CSS));

    /**
     * Pre-built {@link FrontendMappingDefinition} for User entity form.
     * <p>
     * Provides basic field definitions for user entity management with email validation and 
     * role assignment capabilities. Fields include:
     * <ul>
     *   <li>{@code EMAIL_} - Text field for user email with additional privileges {@code CHECK_IS_NEW_USER_OR_OWNER} 
     *       and {@code CHECK_IF_CAN_WRITE_USER}</li>
     *   <li>{@code FIRST_NAME_} - Text field for user first name with {@code CHECK_IS_NEW_USER_OR_OWNER} privileges</li>
     *   <li>{@code LAST_NAME_} - Text field for user last name with {@code CHECK_IS_NEW_USER_OR_OWNER} privileges</li>
     * </ul>
     * No default read/write privileges specified (null), relying on field-level additional privileges 
     * for access control.
     * </p>
     * 
     * @see BasicUserForm
     * @see EditUserForm
     * @see InviteUserForm
     * @see com.openkoda.model.User
     */
    FrontendMappingDefinition userForm = createFrontendMappingDefinition(USER_FORM, null, (PrivilegeBase) null,
        a -> a  .text(EMAIL_).additionalPrivileges(CHECK_IS_NEW_USER_OR_OWNER, CHECK_IF_CAN_WRITE_USER)
                .text(FIRST_NAME_).additionalPrivileges(CHECK_IS_NEW_USER_OR_OWNER, CHECK_IS_NEW_USER_OR_OWNER)
                .text(LAST_NAME_).additionalPrivileges(CHECK_IS_NEW_USER_OR_OWNER, CHECK_IS_NEW_USER_OR_OWNER)
    );

    FrontendMappingDefinition inviteForm = createFrontendMappingDefinition(INVITE_USER_FORM_NAME, null, null, FrontendMappingDefinitions.userForm.fields,
        a -> a  .dropdown(ROLE_NAME_, ORGANIZATION_ROLES_).additionalPrivileges(CHECK_IS_NEW_USER_OR_OWNER, CHECK_IS_NEW_USER_OR_OWNER)
    );

    FrontendMappingDefinition globalOrgRoleForm = createFrontendMappingDefinition(GLOBAL_ORG_ROLE_FORM, canAccessGlobalSettings, canAccessGlobalSettings,
            a -> a.checkboxList("globalOrganizationRoles", "globalOrganizationRoles"));

    FrontendMappingDefinition editUserForm = createFrontendMappingDefinition(EDIT_USER_FORM, readUserData, manageUserData, FrontendMappingDefinitions.userForm.fields,
        a -> a  .dropdown(ENABLED_, BOOLEAN_VALUES_)
                .dropdown(LANGUAGE, LANGUAGES).additionalPrivileges(CHECK_IS_NEW_USER_OR_OWNER, CHECK_IS_NEW_USER_OR_OWNER)
                .dropdown(GLOBAL_ROLE_NAME_, GLOBAL_USER_ROLES_).additionalPrivileges(canAccessGlobalSettings, canAccessGlobalSettings)
            );

    /**
     * Pre-built {@link FrontendMappingDefinition} for Scheduler entity form.
     * <p>
     * Provides field definitions for scheduled task configuration with cron expression validation
     * and event data specification. Fields include:
     * <ul>
     *   <li>{@code ORGANIZATION_ID_} - Organization selection dropdown for tenant-scoped scheduling</li>
     *   <li>{@code CRON_EXPRESSION_} - Text field for cron expression (validated via {@link org.springframework.scheduling.support.CronSequenceGenerator})</li>
     *   <li>{@code EVENT_DATA_} - Text field for event data payload (JSON or string)</li>
     *   <li>{@code ON_MASTER_ONLY_} - Checkbox to restrict execution to master node in clustered deployments</li>
     * </ul>
     * Access controlled by {@code canReadBackend} and {@code canManageBackend} privileges.
     * </p>
     * 
     * @see SchedulerForm
     * @see com.openkoda.model.Scheduler
     */
    FrontendMappingDefinition schedulerForm = createFrontendMappingDefinition(SCHEDULER_FORM, canReadBackend, canManageBackend,
            a -> a  .organizationSelect(ORGANIZATION_ID_)
                    .text(CRON_EXPRESSION_)
                    .text(EVENT_DATA_)
                    .checkbox(ON_MASTER_ONLY_)
    );

    /**
     * Pre-built {@link FrontendMappingDefinition} for Organization tenant entity form.
     * <p>
     * Provides comprehensive field definitions for organization (tenant) management including branding
     * configuration and datasource assignment. Fields include:
     * <ul>
     *   <li>{@code NAME_} - Text field for organization name</li>
     *   <li>{@code ASSIGNED_DATASOURCE_} - Dropdown for datasource selection (visible only in multitenancy mode with 
     *       {@code canAccessGlobalSettings} privilege)</li>
     *   <li>{@code LOGO_ID} - Image upload field for organization logo</li>
     *   <li>{@code PERSONALIZE_DASHBOARD} - Checkbox to enable dashboard personalization</li>
     *   <li>{@code MAIN_BRAND_COLOR} - Color picker for primary brand color</li>
     *   <li>{@code SECOND_BRAND_COLOR} - Color picker for secondary brand color</li>
     * </ul>
     * Datasource selection field dynamically enabled based on {@link MultitenancyService#isMultitenancy()} 
     * and global settings privilege. Access controlled by {@code readOrgData} and {@code manageOrgData} privileges.
     * </p>
     * 
     * @see OrganizationForm
     * @see com.openkoda.model.Organization
     */
    FrontendMappingDefinition organizationForm = createFrontendMappingDefinition(ORGANIZATION_FORM, readOrgData, manageOrgData,
            a -> a  .text(NAME_)
                    .dropdown(ASSIGNED_DATASOURCE_, "datasources", false)
                        .additionalPrivileges(
                                (u, e) -> MultitenancyService.isMultitenancy() && u.hasGlobalPrivilege(canAccessGlobalSettings),
                                (u, e) -> MultitenancyService.isMultitenancy() && u.hasGlobalPrivilege(canAccessGlobalSettings))
                        .additionalCss(!MultitenancyService.isMultitenancy() ? "d-none" : "")
// Replace with this lines to disable datasource selection in Organization creation form
//                            (u, e) -> e != null && HybridMultiTenantConnectionProvider.isMultitenancy() && u.hasGlobalPrivilege(canAccessGlobalSettings),
//                            (u, e) -> e != null && HybridMultiTenantConnectionProvider.isMultitenancy() && u.hasGlobalPrivilege(canAccessGlobalSettings))
                    .image(LOGO_ID)
                    .checkbox(PERSONALIZE_DASHBOARD)
                    .colorPicker(MAIN_BRAND_COLOR)
                    .colorPicker(SECOND_BRAND_COLOR)
    );

    /**
     * Pre-built {@link FrontendMappingDefinition} for EmailConfig entity form.
     * <p>
     * Provides field definitions for email configuration with conditional validation supporting both 
     * SMTP and Mailgun delivery methods. Fields include:
     * <ul>
     *   <li>{@code EMAIL_MAILGUN_API_KEY} - Text field for Mailgun API key (alternative to SMTP)</li>
     *   <li>{@code EMAIL_HOST} - Text field for SMTP server hostname</li>
     *   <li>{@code EMAIL_PORT} - Number field for SMTP server port</li>
     *   <li>{@code EMAIL_USERNAME} - Text field for SMTP authentication username</li>
     *   <li>{@code EMAIL_PASSWORD} - Text field for SMTP authentication password</li>
     *   <li>{@code EMAIL_FROM} - Text field for sender email address</li>
     *   <li>{@code EMAIL_REPLY_TO} - Text field for reply-to email address</li>
     *   <li>{@code EMAIL_SSL} - Checkbox to enable SSL/TLS encryption</li>
     *   <li>{@code EMAIL_SMTP_AUTH} - Checkbox to enable SMTP authentication</li>
     *   <li>{@code EMAIL_STARTTLS} - Checkbox to enable STARTTLS upgrade</li>
     * </ul>
     * Form validation enforces either SMTP configuration (host + username + password + from) or 
     * Mailgun configuration (mailgunApiKey). Access controlled by {@code canReadBackend} and 
     * {@code canManageBackend} privileges.
     * </p>
     * 
     * @see EmailConfigForm
     * @see com.openkoda.model.EmailConfig
     */
    FrontendMappingDefinition emailConfigForm = createFrontendMappingDefinition(EMAIL_CONFIG_FORM, canReadBackend, canManageBackend,
            a -> a  .text(EMAIL_MAILGUN_API_KEY)
                    .text(EMAIL_HOST)
                    .number(EMAIL_PORT)
                    .text(EMAIL_USERNAME)
                    .text(EMAIL_PASSWORD)
                    .text(EMAIL_FROM)
                    .text(EMAIL_REPLY_TO)
                    .checkbox(EMAIL_SSL)
                    .checkbox(EMAIL_SMTP_AUTH)
                    .checkbox(EMAIL_STARTTLS)
                    
    );
    
    FrontendMappingDefinition loggerForm = createFrontendMappingDefinition(LOGGER_FORM, canReadSupportData, canManageSupportData,
            a -> a  .text(BUFFER_SIZE_FIELD_)
                    .checkboxList(LOGGING_CLASSES_, (f, d) -> d.getLoggersDictionary()).additionalCss(TABLE_COMPACT_CSS)
    );

    /**
     * Pre-built {@link FrontendMappingDefinition} for EventListener entity form.
     * <p>
     * Provides field definitions for event listener configuration with consumer/event descriptor
     * validation and static parameter support. Fields include:
     * <ul>
     *   <li>{@code ORGANIZATION_ID_} - Organization selection dropdown for tenant-scoped listeners</li>
     *   <li>{@code EVENT_} - Dropdown for event type selection</li>
     *   <li>{@code consumerCategories} - Datalist with {@link EventConsumerCategory} enum values</li>
     *   <li>{@code consumerCategory} - Dropdown for consumer category selection</li>
     *   <li>{@code CONSUMER_} - Dropdown for consumer class selection (validated via reflection)</li>
     *   <li>{@code STATIC_DATA_1_} through {@code STATIC_DATA_4_} - Text fields for static parameter data</li>
     * </ul>
     * Form performs reflection-based type checking using {@link Class#forName(String)} to validate 
     * consumer/event compatibility and parameter count matching. Access controlled by {@code canReadBackend} 
     * and {@code canManageBackend} privileges.
     * </p>
     * 
     * @see EventListenerForm
     * @see com.openkoda.model.EventListener
     * @see EventConsumerCategory
     */
    FrontendMappingDefinition eventListenerForm = createFrontendMappingDefinition(EVENT_LISTENER_FORM, canReadBackend, canManageBackend,
            a -> a  .organizationSelect(ORGANIZATION_ID_)
                    .dropdown(EVENT_, EVENTS_)
                    .datalist("consumerCategories", r -> r.enumsToMapWithLabels(EventConsumerCategory.values()))
                    .dropdown("consumerCategory", "consumerCategories")
                    .dropdown(CONSUMER_, CONSUMERS_)
                    .text(STATIC_DATA_1_)
                    .text(STATIC_DATA_2_)
                    .text(STATIC_DATA_3_)
                    .text(STATIC_DATA_4_)

    );

    /**
     * Pre-built {@link FrontendMappingDefinition} for FrontendResource entity form.
     * <p>
     * Provides comprehensive field definitions for frontend resource management including content editing,
     * URL path configuration, privilege requirements, and type-specific validation. Fields include:
     * <ul>
     *   <li>{@code NAME_} - Text field for resource name (validated against {@code FRONTENDRESOURCEREGEX})</li>
     *   <li>{@code ORGANIZATION_ID_} - Organization selection dropdown for tenant-scoped resources</li>
     *   <li>{@code REQUIRED_PRIVILEGE_} - Dropdown for privilege requirement selection</li>
     *   <li>{@code TYPE_} - Section with dropdown for frontend resource type (HTML, CSS, JS, etc.)</li>
     *   <li>{@code INCLUDE_IN_SITEMAP_} - Checkbox to include resource in sitemap</li>
     *   <li>{@code EMBEDDABLE_} - Checkbox to enable embedding in other pages</li>
     *   <li>{@code ACCESS_LEVEL} - Dropdown for access level selection with enum datalist</li>
     *   <li>{@code DRAFT_CONTENT_} - Custom field type with code editor based on resource type (HTML/CSS/JS syntax highlighting)</li>
     * </ul>
     * Draft content field uses value supplier to extract content or draft content from entity based on draft status.
     * Form-level validation enforces type/name extension compatibility (CSS files must end with .css, JS files with .js).
     * Access controlled by {@code readFrontendResource} and {@code manageFrontendResource} privileges.
     * </p>
     * 
     * @see FrontendResourceForm
     * @see FrontendResource
     * @see FrontendResource.Type
     * @see FrontendResource.AccessLevel
     */
    FrontendMappingDefinition frontendResourceForm = createFrontendMappingDefinition(FRONTEND_RESOURCE_FORM, readFrontendResource, manageFrontendResource,
            a -> a  .text(NAME_)
                        .validate(v -> v.matches(FRONTENDRESOURCEREGEX) ? null : "not.matching.name")
                    .organizationSelect(ORGANIZATION_ID_)
                    .dropdown(REQUIRED_PRIVILEGE_, PRIVILEGES_, true)
                    .sectionWithDropdown(TYPE_, FRONTEND_RESOURCE_TYPE_)
                        //.valueType(FrontendResource.Type.class)
                        .additionalCss("frontendResourceType").validate(notBlank())
                    .checkbox(INCLUDE_IN_SITEMAP_)
                    .checkbox(EMBEDDABLE_)
                    .datalist(ACCESS_LEVELS, d -> d.enumDictionary(FrontendResource.AccessLevel.values()))
                    .dropdown(ACCESS_LEVEL, ACCESS_LEVELS)
                    .customFieldType(DRAFT_CONTENT_,  f -> FrontendResourceForm.getCodeType(((ReflectionBasedEntityForm)f).dto.get(TYPE_)))
                    .valueSupplier(f -> {
                        FrontendResource ce = (FrontendResource) ((ReflectionBasedEntityForm) f).entity;
                        return ce == null ? "" : (ce.isDraft() ? ce.getDraftContent() : ce.getContent());
                    })
                    .validateForm((ReflectionBasedEntityForm f) ->
                        (f.dto.get(TYPE_).toString().equals(FrontendResource.Type.CSS.name()) && !f.dto.get(NAME_).toString().endsWith(FrontendResource.Type.CSS.getExtension())) ||
                        (f.dto.get(NAME_).toString().endsWith(FrontendResource.Type.CSS.getExtension()) && !f.dto.get(TYPE_).toString().equals(FrontendResource.Type.CSS.name())) ||
                        ((f.dto.get(TYPE_).toString().equals(FrontendResource.Type.JS.name())) && !f.dto.get(NAME_).toString().endsWith(FrontendResource.Type.JS.getExtension())) ||
                        (f.dto.get(NAME_).toString().endsWith(FrontendResource.Type.JS.getExtension()) && (!f.dto.get(TYPE_).toString().equals(FrontendResource.Type.JS.name()))) ?
                                Map.of(TYPE_, "incompatible.frontend-resource.types", NAME_, "incompatible.frontend-resource.types") : null)
    );

    FrontendMappingDefinition queryReportForm = createFrontendMappingDefinition(QUERY_REPORT_FORM, canUseReportingAI, canUseReportingAI,
            a -> a
                    .text(NAME_).validate(v -> StringUtils.isNotEmpty(v) ? null : "not.empty")
                    .hidden(QUERY)
    );

    FrontendMappingDefinition frontendResourcePageForm = createFrontendMappingDefinition(FRONTEND_RESOURCE_PAGE_FORM, readFrontendResource, manageFrontendResource,
            a -> a
                    .text(URL_PATH_)
                    .textarea(CONTENT_EDITABLE_)
    );

    FrontendMappingDefinition sendEventForm = createFrontendMappingDefinition(SEND_EVENT_FORM, canReadBackend, canManageBackend,
            a -> a  .dropdownNonDto(EVENT_, EVENTS_)
    );
    
    FrontendMappingDefinition createEventForm = createFrontendMappingDefinition(CREATE_EVENT_FORM, canReadBackend, canManageBackend,
            a -> a  .text(NAME_)
                    .dropdown("className", EVENTS_CLASSES_)   
                    .hidden("eventName")
                    
    );

    FrontendMappingDefinition sendCustomEventForm = createFrontendMappingDefinition(SEND_CUSTOM_EVENT_FORM, canReadBackend, canManageBackend,
            a -> a  .text(NAME_)
                    .text("eventData")
                    .hidden("eventName")
                    .hidden("className")
    );

    FrontendMappingDefinition sendEventInvoiceDto = createFrontendMappingDefinition(SEND_EVENT_FORM, canReadBackend, canManageBackend,
            a -> a  .dropdownNonDto(EVENT_, EVENTS_)
                    .text(SELLER_COMPANY_NAME_)
                    .text(SELLER_COMPANY_ADDRESS_LINE_)
                    .text(SELLER_COMPANY_ADDRESS_LINE_2)
                    .text(SELLER_COMPANY_COUNTRY_)
                    .text(SELLER_COMPANY_TAX_NO_)
                    .text(BUYER_COMPANY_NAME_)
                    .text(BUYER_COMPANY_ADDRESS_LINE_1_)
                    .text(BUYER_COMPANY_ADDRESS_LINE_2_)
                    .text(BUYER_COMPANY_COUNTRY_)
                    .text(BUYER_COMPANY_TAX_NO_)
                    .text(INVOICE_IDENTIFIER_)
                    .text(ITEM_)
                    .text(CURRENCY_)
                    .text(VALUE_)
                    .text(TAX_)
                    .text(CREATED_ON_)
                    .text(ORGANIZATION_ID_)
    );

    FrontendMappingDefinition sendEventPaymentDto = createFrontendMappingDefinition(SEND_EVENT_FORM, canReadBackend, canManageBackend,
            a -> a  .dropdownNonDto(EVENT_, EVENTS_)
                    .text(TOTAL_AMOUNT_)
                    .text(NET_AMOUNT_)
                    .text(TAX_AMOUNT_)
                    .text(PLAN_ID_)
                    .text(PLAN_NAME_)
                    .text(DESCRIPTION)
                    .text(STATUS_)
                    .text(CURRENCY_)
                    .text(ORGANIZATION_ID_)
    );

    FrontendMappingDefinition sendEventPlanDto = createFrontendMappingDefinition(SEND_EVENT_FORM, canReadBackend, canManageBackend,
            a -> a  .dropdownNonDto(EVENT_, EVENTS_)
                    .text(ORGANIZATION_ID_)
                    .text(PLAN_NAME_)
    );

    FrontendMappingDefinition sendEventSubscriptionDto = createFrontendMappingDefinition(SEND_EVENT_FORM, canReadBackend, canManageBackend,
            a -> a  .dropdownNonDto(EVENT_, EVENTS_)
                    .text(SUBSCRIPTION_ID_)
                    .text(NEXT_BILLING_)
                    .text(CURRENT_BILLING_START)
                    .text(CURRENT_BILLING_END)
                    .text(NEXT_AMOUNT_)
                    .text(PRICE_)
                    .text(PLAN_NAME_)
                    .text(PLAN_FULL_NAME_)
                    .text(SUBSCRIPTION_STATUS_)
                    .text(CURRENCY_)
                    .text(ORGANIZATION_ID_)
    );

    FrontendMappingDefinition sendEventFrontendResourceDto = createFrontendMappingDefinition(SEND_EVENT_FORM, canReadBackend, canManageBackend,
            a -> a  .dropdownNonDto(EVENT_, EVENTS_)
                    .text(NAME_)
                    .text(ORGANIZATION_ID_)
                    .text(CONTENT_)
                    .text(TEST_DATA_)
                    .text(REQUIRED_PRIVILEGE_)
                    .text(INCLUDE_IN_SITEMAP_)
                    .text(TYPE_)
    );

    FrontendMappingDefinition sendEventScheduledSchedulerDto = createFrontendMappingDefinition(SEND_EVENT_FORM, canReadBackend, canManageBackend,
            a -> a  .dropdownNonDto(EVENT_, EVENTS_)
                    .text(SCHEDULED_AT_)
                    .text(CRON_EXPRESSION_)
                    .text(EVENT_DATA_)
                    .text(ORGANIZATION_ID_)
    );

    FrontendMappingDefinition sendEventBasicUser = createFrontendMappingDefinition(SEND_EVENT_FORM, canReadBackend, canManageBackend,
            a -> a  .dropdownNonDto(EVENT_, EVENTS_)
                    .text(ID_)
                    .text(FIRST_NAME_)
                    .text(LAST_NAME_)
                    .text(EMAIL_)
    );

    FrontendMappingDefinition sendEventUserRoleDto = createFrontendMappingDefinition(SEND_EVENT_FORM, canReadBackend, canManageBackend,
            a -> a  .dropdownNonDto(EVENT_, EVENTS_)
                    .text(ID_)
                    .text(USER_ID_)
                    .text(ROLE_ID_)
                    .text(ORGANIZATION_ID_)
    );

    FrontendMappingDefinition sendEventNotificationDto = createFrontendMappingDefinition(SEND_EVENT_FORM, canReadBackend, canManageBackend,
            a -> a  .dropdownNonDto(EVENT_, EVENTS_)
                    .text(ATTACHMENT_URL_)
                    .text(MESSAGE_)
                    .text(SUBJECT_)
                    .text(NOTIFICATION_TYPE_)
                    .text(USER_ID_)
                    .text(ORGANIZATION_ID_)
                    .text(REQUIRED_PRIVILEGE_)
                    .checkbox(PROPAGATE)
    );

    FrontendMappingDefinition sendEventOrganizationDto = createFrontendMappingDefinition(SEND_EVENT_FORM, canReadBackend, canManageBackend,
            a -> a  .dropdownNonDto(EVENT_, EVENTS_)
                    .text(ID_)
                    .text(NAME_)
                    .checkbox(SETUP_TRIAL)
    );

    FrontendMappingDefinition organizationsApi = createFrontendMappingDefinition(ORGANIZATION, readOrgData, manageOrgData,
            a -> a.hidden(ID_)
                    .text(NAME_)
    );
}
