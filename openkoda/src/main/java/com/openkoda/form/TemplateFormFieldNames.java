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

/**
 * Centralized string key constants for form field names across the OpenKoda application.
 * <p>
 * This interface serves as an application-wide constant repository used by form adapters
 * and controllers for consistent field mapping. The field name constants correspond to
 * entity properties and DTO fields, ensuring uniform binding between request parameters,
 * form objects, and domain models. These constants are referenced in form {@code populateFrom()}
 * and {@code populateTo()} mapping logic and in {@code FrontendMappingDefinition} implementations.
 * 
 * <p>
 * Constants are organized by functional domain: user management, organization settings,
 * frontend resources, event scheduling, email configuration, third-party integrations,
 * and billing/subscription management. Using these constants instead of string literals
 * prevents typos and enables refactoring support across the codebase.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public interface TemplateFormFieldNames {
    /** Field name for generic name property used across multiple entity types. */
    String NAME_= "name";
    /** Field name for label property used in UI component definitions. */
    String LABEL_= "label";
    /** Field name for type discriminator used in polymorphic entities. */
    String TYPE_ = "type";
    /** Field name for role type enumeration used in role management forms. */
    String ROLE_TYPES_ = "roleTypes";
    /** Field name for privilege collection used in role and user privilege assignment. */
    String PRIVILEGES_ = "privileges";
    /** Field name for privilege group selection used in privilege management UI. */
    String PRIVILEGE_GROUPS_ = "privilegeGroups";
    
    /** Field name for user email address used in authentication and user management forms. */
    String EMAIL_ = "email";
    /** Field name for user first name used in user profile and registration forms. */
    String FIRST_NAME_ = "firstName";
    /** Field name for user last name used in user profile and registration forms. */
    String LAST_NAME_ = "lastName";
    /** Field name for role name used in organization role assignment forms. */
    String ROLE_NAME_ = "roleName";
    /** Field name for global role name used in system-wide role assignment. */
    String GLOBAL_ROLE_NAME_ = "globalRoleName";
    /** Field name for organization-specific roles collection in user management. */
    String ORGANIZATION_ROLES_ = "organizationRoles";
    /** Field name for global user roles collection spanning all organizations. */
    String GLOBAL_USER_ROLES_ = "globalRoles";
    /** Field name for enabled/disabled boolean flag used across multiple entities. */
    String ENABLED_ = "enabled";
    /** Field name for single field update indicator used in partial update operations. */
    String SINGLE_FIELD_TO_UPDATE = "singleFieldToUpdate";

    /** Field name for boolean value options used in dropdown selections. */
    String BOOLEAN_VALUES_ = "booleanValues";
    
    /** Field name for organization identifier used in multi-tenancy and organization-scoped operations. */
    String ORGANIZATION_ID_ = "organizationId";
    /** Field name for server-side JavaScript code identifier used in dynamic script execution. */
    String SERVER_JS_ID_ = "serverJsId";
    /** Field name for server-side JavaScript code content used in script editing forms. */
    String SERVER_JS_ = "serverJs";
    
    /** Field name for cron expression used in scheduled job configuration. */
    String CRON_EXPRESSION_ = "cronExpression";
    /** Field name for event payload data used in event publishing and handling. */
    String EVENT_DATA_ = "eventData";
    /** Field name for master-only execution flag used in clustered scheduler configuration. */
    String ON_MASTER_ONLY_ = "onMasterOnly";
    /** Field name for buffer size configuration used in logging and data processing. */
    String BUFFER_SIZE_FIELD_ = "bufferSizeField";
    /** Field name for logging class collection used in dynamic logging configuration. */
    String LOGGING_CLASSES_ = "loggingClasses";
    /** Field name for all available loggers listing used in logging management UI. */
    String ALL_LOGGERS_ = "allLoggers";
    
    /** Field name for single event object used in event handling forms. */
    String EVENT_ = "event";
    /** Field name for event collection used in event subscription and filtering. */
    String EVENTS_ = "events";
    /** Field name for event class types used in event listener configuration. */
    String EVENTS_CLASSES_ = "eventClasses";
    /** Field name for integration consumer instance used in third-party API integration. */
    String CONSUMER_ = "consumer";
    /** Field name for consumer collection used in integration management. */
    String CONSUMERS_ = "consumers";

    /** Field name for static data slot 1 used in generic configuration storage. */
    String STATIC_DATA_1_ = "staticData1";
    /** Field name for static data slot 2 used in generic configuration storage. */
    String STATIC_DATA_2_ = "staticData2";
    /** Field name for static data slot 3 used in generic configuration storage. */
    String STATIC_DATA_3_ = "staticData3";
    /** Field name for static data slot 4 used in generic configuration storage. */
    String STATIC_DATA_4_ = "staticData4";

    /** Field name for URL path used in frontend resource routing and controller mapping. */
    String URL_PATH_ = "urlPath";
    /** Field name for required privilege used in access control configuration. */
    String REQUIRED_PRIVILEGE_ = "requiredPrivilege";
    /** Field name for read privilege used in granular permission management. */
    String READ_PRIVILEGE = "readPrivilege";
    /** Field name for write privilege used in granular permission management. */
    String WRITE_PRIVILEGE = "writePrivilege";
    /** Field name for frontend resource type discriminator (page, template, component). */
    String FRONTEND_RESOURCE_TYPE_ = "frontendResourceType";
    /** Field name for title used in content management and SEO metadata. */
    String TITLE= "title";
    /** Field name for category classification used in content organization. */
    String CATEGORY= "category";
    /** Field name for category selection dropdown used in content editing forms. */
    String CATEGORY_SELECT= "categorySelect";
    /** Field name for full-size image URL used in content media management. */
    String IMAGE_URL= "imageUrl";
    /** Field name for thumbnail image URL used in content previews. */
    String MIN_IMAGE_URL= "minImageUrl";
    /** Field name for SEO image alternative text used in accessibility and search optimization. */
    String SEO_IMAGE_ALT= "seoImageAlt";
    /** Field name for SEO meta description used in search engine metadata. */
    String SEO_META_DESCRIPTION= "seoMetaDescription";
    /** Field name for author collection used in content attribution. */
    String AUTHORS= "authors";
    /** Field name for author selection dropdown used in content editing. */
    String AUTHORS_SELECT= "authorsSelect";
    /** Field name for estimated reading time used in blog posts and articles. */
    String READING_TIME = "readingTime";
    /** Field name for published status flag used in content workflow. */
    String PUBLISHED = "published";
    /** Field name for sitemap inclusion flag used in SEO configuration. */
    String INCLUDE_IN_SITEMAP_ = "includeInSitemap";
    /** Field name for embeddable flag indicating content can be embedded in iframes. */
    String EMBEDDABLE_ = "embeddable";
    
    /** Field name for primary content body used in frontend resource and template editing. */
    String CONTENT_ = "content";
    /** Field name for draft content version used in content workflow management. */
    String DRAFT_CONTENT_ = "draftContent";
    /** Field name for content editable flag controlling inline editing capabilities. */
    String CONTENT_EDITABLE_ = "contentEditable";
    /** Field name for JavaScript code used in dynamic frontend resource scripting. */
    String JS_CODE_ = "jsCode";
    /** Field name for test data used in script testing and validation. */
    String TEST_DATA_ = "testData";
    /** Field name for test button action trigger used in development interfaces. */
    String TEST_BUTTON_ = "testButton";
    /** Field name for default value used in field configuration and form initialization. */
    String DEFAULT_VALUE_ = "defaultValue";
    /** Field name for level indicator used in hierarchical data structures. */
    String LEVEL_ = "level";
    /** Field name for attribute-specific level used in permission granularity. */
    String ATTRIBUTE_LEVEL_ = "attributeLevel";
    /** Field name for read-only flag controlling field editability. */
    String READ_ONLY_ = "readOnly";
    /** Field name for generic entity identifier used across all entity types. */
    String ID_ = "id";
    /** Field name for user entity identifier used in user references. */
    String USER_ID_ = "userId";
    /** Field name for role entity identifier used in role references. */
    String ROLE_ID_ = "roleId";
    /** Field name for generic value property used in key-value pair structures. */
    String VALUE_ = "value";
    /** Field name for total amount including tax used in financial calculations. */
    String TOTAL_AMOUNT_ = "totalAmount";
    /** Field name for net amount before tax used in financial calculations. */
    String NET_AMOUNT_ = "netAmount";
    /** Field name for tax amount used in financial calculations. */
    String TAX_AMOUNT_ = "taxAmount";
    /** Field name for image collection used in gallery and media management. */
    String IMAGES_ = "images";
    /** Field name for single access level used in permission assignment. */
    String ACCESS_LEVEL = "accessLevel";
    /** Field name for access level options used in permission configuration UI. */
    String ACCESS_LEVELS = "accessLevels";

    /** Field name for subscription plan identifier used in billing and subscription management. */
    String PLAN_ID_ = "planId";
    /** Field name for subscription plan name used in plan selection and display. */
    String PLAN_NAME_ = "planName";
    /** Field name for generic description text used across multiple entity types. */
    String DESCRIPTION = "description";
    /** Field name for subscription start date used in billing period tracking. */
    String STARTED_ON_ = "startedOn";
    /** Field name for payment completion date used in invoice and payment tracking. */
    String PAID_ON = "paidOn";
    /** Field name for generic status indicator used across multiple workflows. */
    String STATUS_ = "status";
    /** Field name for currency code used in financial transactions and pricing. */
    String CURRENCY_ = "currency";
    /** Field name for scheduled execution time used in job and task scheduling. */
    String SCHEDULED_AT_ = "scheduledAt";
    /** Field name for attachment URL used in notification and document management. */
    String ATTACHMENT_URL_ = "attachmentURL";
    /** Field name for message content used in notification and communication forms. */
    String MESSAGE_ = "message";
    /** Field name for email subject used in notification configuration. */
    String SUBJECT_ = "subject";
    /** Field name for notification type discriminator used in notification routing. */
    String NOTIFICATION_TYPE_ = "notificationType";
    /** Field name for propagation flag used in event and notification distribution. */
    String PROPAGATE = "propagate";

    /** Field name for subscription entity identifier used in subscription management. */
    String SUBSCRIPTION_ID_ = "subscriptionId";
    /** Field name for next billing cycle indicator used in subscription lifecycle. */
    String NEXT_BILLING_ = "nextBilling";
    /** Field name for current billing period start date used in subscription tracking. */
    String CURRENT_BILLING_START = "currentBillingStart";
    /** Field name for current billing period end date used in subscription tracking. */
    String CURRENT_BILLING_END = "currentBillingEnd";
    /** Field name for next billing date used in payment scheduling. */
    String NEXT_BILLING_DATE = "nextBillingDate";
    /** Field name for billing period start date used in invoice generation. */
    String BILLING_START_DATE = "billingStartDate";
    /** Field name for billing period end date used in invoice generation. */
    String BILLING_END_DATE = "billingEndDate";
    /** Field name for next billing amount used in subscription forecasting. */
    String NEXT_AMOUNT_ = "nextAmount";
    /** Field name for price value used in subscription plans and payment processing. */
    String PRICE_ = "price"; //PaymentSubscriptionController.java
    /** Field name for full subscription plan name used in billing display. */
    String PLAN_FULL_NAME_ = "planFullName";
    /** Field name for subscription status (active, cancelled, suspended) used in lifecycle management. */
    String SUBSCRIPTION_STATUS_ = "subscriptionStatus";

    /** Field name for seller company name used in invoice generation. */
    String SELLER_COMPANY_NAME_ = "sellerCompanyName";
    /** Field name for seller company address line 1 used in invoice billing information. */
    String SELLER_COMPANY_ADDRESS_LINE_ = "sellerCompanyAddressLine1";
    /** Field name for seller company address line 2 used in invoice billing information. */
    String SELLER_COMPANY_ADDRESS_LINE_2 = "sellerCompanyAddressLine2";
    /** Field name for seller company country used in invoice and tax calculations. */
    String SELLER_COMPANY_COUNTRY_ = "sellerCompanyCountry";
    /** Field name for seller company tax number used in invoice compliance. */
    String SELLER_COMPANY_TAX_NO_ = "sellerCompanyTaxNo";

    /** Field name for buyer company name used in invoice generation. */
    String BUYER_COMPANY_NAME_ = "buyerCompanyName";
    /** Field name for buyer company address line 1 used in invoice shipping information. */
    String BUYER_COMPANY_ADDRESS_LINE_1_ = "buyerCompanyAddressLine1";
    /** Field name for buyer company address line 2 used in invoice shipping information. */
    String BUYER_COMPANY_ADDRESS_LINE_2_ = "buyerCompanyAddressLine2";
    /** Field name for buyer company country used in invoice and tax calculations. */
    String BUYER_COMPANY_COUNTRY_ = "buyerCompanyCountry";
    /** Field name for buyer company tax number used in invoice compliance. */
    String BUYER_COMPANY_TAX_NO_ = "buyerCompanyTaxNo";

    /** Field name for invoice unique identifier used in invoice tracking and references. */
    String INVOICE_IDENTIFIER_ = "invoiceIdentifier";
    /** Field name for invoice line item used in invoice detail management. */
    String ITEM_ = "item";
    /** Field name for tax rate or amount used in financial calculations. */
    String TAX_ = "tax";
    /** Field name for entity creation timestamp used in audit tracking. */
    String CREATED_ON_ = "createdOn";

    /** Field name for organization name used in tenant management and display. */
    String ORGANIZATION_NAME_ = "organizationName";
    /** Field name for todo list URL used in external task management integrations. */
    String TODO_LIST_URL_ = "toDoListUrl";
    /** Field name for webhook callback URL used in event notification integrations. */
    String WEBHOOK_URL_ = "webhookUrl";
    /** Field name for GitHub repository owner used in GitHub integration configuration. */
    String GITHUB_REPO_OWNER_ = "gitHubRepoOwner";
    /** Field name for GitHub repository name used in GitHub integration configuration. */
    String GITHUB_REPO_NAME_ = "gitHubRepoName";

    /** Field name for project name used in project management integrations. */
    String PROJECT_NAME_ = "projectName";

    /** Field name for Trello API key used in Trello integration authentication. */
    String TRELLO_API_KEY_ = "trelloApiKey";
    /** Field name for Trello API token used in Trello integration authorization. */
    String TRELLO_API_TOKEN_ = "trelloApiToken";
    /** Field name for Trello board name used in Trello integration task management. */
    String TRELLO_BOARD_NAME_ = "trelloBoardName";
    /** Field name for Trello list name used in Trello integration task organization. */
    String TRELLO_LIST_NAME_ = "trelloListName";

    /** Field name for generic URL used in external resource references and integrations. */
    String URL_ = "url";

    /** Field name for language code used in internationalization and localization. */
    String LANGUAGE = "language";
    /** Field name for language collection used in multi-language configuration. */
    String LANGUAGES = "languages";
    /** Field name for assigned datasource identifier used in database routing. */
    String ASSIGNED_DATASOURCE_ = "assignedDatasource";
    /** Field name for trial setup flag used in subscription provisioning. */
    String SETUP_TRIAL = "setupTrial";

    /** Field name for URL sub-path used in routing and resource mapping. */
    String SUB_PATH = "subPath";
    /** Field name for generic code value used in lookup tables and reference data. */
    String CODE = "code";
    /** Field name for HTTP headers collection used in API request configuration. */
    String HTTP_HEADERS = "httpHeaders";
    /** Field name for HTTP method (GET, POST, PUT, DELETE) used in endpoint configuration. */
    String HTTP_METHOD = "httpMethod";
    /** Field name for model attributes map used in view rendering. */
    String MODEL_ATTRIBUTES = "modelAttributes";
    /** Field name for response type (JSON, HTML) used in content negotiation. */
    String RESPONSE_TYPE = "responseType";
    /** Field name for frontend resource identifier used in dynamic entity form configuration. */
    String FRONTEND_RESOURCE_ID = "frontendResourceId";
    /** Field name for auditable registration flag used in dynamic entity audit trail setup. */
    String REGISTER_AS_AUDITABLE = "registerAsAuditable";
    /** Field name for API CRUD controller registration flag used in dynamic entity REST endpoint generation. */
    String REGISTER_API_CRUD_CONTROLLER = "registerApiCrudController";
    /** Field name for HTML CRUD controller registration flag used in dynamic entity UI generation. */
    String REGISTER_HTML_CRUD_CONTROLLER = "registerHtmlCrudController";
    /** Field name for organization dashboard visibility flag used in widget configuration. */
    String SHOW_ON_ORGANIZATION_DASHBOARD = "showOnOrganizationDashboard";
    /** Field name for table column configuration used in dynamic entity table rendering. */
    String TABLE_COLUMNS = "tableColumns";
    /** Field name for filter column selection used in search and filtering UI. */
    String FILTER_COLUMNS = "filterColumns";
    /** Field name for available filter columns listing used in filter configuration. */
    String FILTER_AVAILABLE_COLUMNS = "filterAvailableColumns";

    /** Field name for email SMTP host address used in email server configuration. */
    String EMAIL_HOST = "host";
    /** Field name for email SMTP port number used in email server connection. */
    String EMAIL_PORT = "port";
    /** Field name for email SMTP username used in email server authentication. */
    String EMAIL_USERNAME = "username";
    /** Field name for email protocol (SMTP, SMTPS) used in email transport configuration. */
    String EMAIL_PROTOCOL = "protocol";
    /** Field name for email SMTP password used in email server authentication. */
    String EMAIL_PASSWORD = "password";
    /** Field name for email sender address used in outgoing email configuration. */
    String EMAIL_FROM = "from";
    /** Field name for email SSL/TLS flag used in secure email connection setup. */
    String EMAIL_SSL = "ssl";
    /** Field name for email SMTP authentication flag used in email server configuration. */
    String EMAIL_SMTP_AUTH = "smtpAuth";
    /** Field name for email STARTTLS flag used in secure email connection upgrade. */
    String EMAIL_STARTTLS = "starttls";
    /** Field name for email reply-to address used in email header configuration. */
    String EMAIL_REPLY_TO = "replyTo";
    /** Field name for Mailgun API key used in Mailgun email service integration. */
    String EMAIL_MAILGUN_API_KEY = "mailgunApiKey";

    /** Field name for search query string used in search and filter operations. */
    String QUERY = "query";
    /** Field name for logo image identifier used in organization branding. */
    String LOGO_ID = "logoId";
    /** Field name for dashboard personalization flag used in user preferences. */
    String PERSONALIZE_DASHBOARD = "personalizeDashboard";
    /** Field name for primary brand color used in organization theme customization. */
    String MAIN_BRAND_COLOR = "mainBrandColor";
    /** Field name for secondary brand color used in organization theme customization. */
    String SECOND_BRAND_COLOR = "secondBrandColor";
}
