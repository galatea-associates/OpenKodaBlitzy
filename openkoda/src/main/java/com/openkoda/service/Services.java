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

package com.openkoda.service;

import com.openkoda.core.customisation.CustomisationService;
import com.openkoda.core.customisation.ServerJSRunner;
import com.openkoda.core.flow.TransactionalExecutor;
import com.openkoda.core.helper.UrlHelper;
import com.openkoda.core.multitenancy.TenantResolver;
import com.openkoda.core.security.RunAsService;
import com.openkoda.core.service.*;
import com.openkoda.core.service.email.EmailConstructor;
import com.openkoda.core.service.email.EmailSender;
import com.openkoda.core.service.email.EmailService;
import com.openkoda.core.service.event.ApplicationEventService;
import com.openkoda.core.service.event.ClusterEventSenderService;
import com.openkoda.core.service.event.EventListenerService;
import com.openkoda.core.service.event.SchedulerService;
import com.openkoda.core.service.form.FormService;
import com.openkoda.core.service.module.ModuleService;
import com.openkoda.core.service.pdf.PdfConstructor;
import com.openkoda.core.service.system.DatabaseValidationService;
import com.openkoda.core.service.system.SystemHealthStatusService;
import com.openkoda.service.captcha.CaptchaService;
import com.openkoda.service.csv.CsvService;
import com.openkoda.service.dynamicentity.DynamicEntityRegistrationService;
import com.openkoda.service.dynamicentity.DynamicEntityService;
import com.openkoda.service.export.ComponentExportService;
import com.openkoda.service.export.ZipComponentImportService;
import com.openkoda.service.notification.NotificationService;
import com.openkoda.service.openai.ChatGPTService;
import com.openkoda.service.organization.OrganizationService;
import com.openkoda.service.user.*;
import com.openkoda.uicomponent.JsParser;
import com.openkoda.uicomponent.UtilServices;
import com.openkoda.uicomponent.live.LiveDataServices;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

/**
 * Convenience aggregator exposing 50+ service beans for injection as single dependency.
 * <p>
 * Legacy pattern enabling controllers and components to inject a single Services bean instead of many individual 
 * service interfaces. Exposes all application services as public {@code @Inject} fields for programmatic access. 
 * Used extensively in older code and integration points that predate constructor injection patterns.
 * </p>
 * <p>
 * This aggregator provides access to:
 * </p>
 * <ul>
 * <li><b>Core Services:</b> applicationEvent, transactionalExecutor, validation, sessionService, systemStatus</li>
 * <li><b>User Management:</b> user, role, privilege, apiKey, token, userRole</li>
 * <li><b>Multi-Tenancy:</b> organization, tenantResolver</li>
 * <li><b>Communication:</b> email, emailConstructor, emailService, notification, websocket</li>
 * <li><b>Content Management:</b> frontendResource, file, form, frontendMappingDefinition, thymeleaf</li>
 * <li><b>Dynamic Entities:</b> dynamicEntityRegistration, dynamicEntity</li>
 * <li><b>Component Management:</b> module, customisation, componentExport, zipComponentImport</li>
 * <li><b>Integrations:</b> serverJSRunner, jsParser, restClient, chatGPTService, captcha</li>
 * <li><b>Utilities:</b> url, zipService, csv, pdfConstructor, logConfig, databaseValidation</li>
 * <li><b>Events &amp; Scheduling:</b> scheduler, eventListener, clusterEventSender</li>
 * <li><b>Security:</b> runAs (privilege elevation)</li>
 * <li><b>UI Components:</b> data (LiveDataServices), util (UtilServices)</li>
 * </ul>
 * <p>
 * <b>Design Pattern:</b>
 * Passive holder with no business logic. All fields are public and non-final for Jakarta {@code @Inject} field 
 * injection. Spring component scanning automatically populates fields at application startup.
 * </p>
 * <p>
 * <b>Known Issues:</b>
 * Contains duplicate DatabaseValidationService injection (fields {@code databaseValidationService} and 
 * {@code databaseValidation}) - both reference the same bean. This duplication is maintained for backward 
 * compatibility with existing code.
 * </p>
 * <p>
 * <b>Thread Safety:</b>
 * Thread-safe as a stateless Spring singleton. All exposed service beans manage their own thread safety.
 * </p>
 * <p>
 * <b>Usage Example:</b>
 * <pre>
 * &#64;Inject
 * private Services services;
 * 
 * Organization org = services.organization.createOrganization(name);
 * User user = services.user.createUser(email, password);
 * </pre>
 * </p>
 * <p>
 * <b>Migration Note:</b>
 * Modern code should inject specific service interfaces directly rather than using this aggregator. 
 * Constructor injection is preferred over field injection for testability.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.0.0
 * @see jakarta.inject.Inject
 * @see org.springframework.stereotype.Component
 */
@Component("allServices")
public class Services {
    @Inject public ApplicationEventService applicationEvent;
    @Inject public RoleService role;
    @Inject public BasicPrivilegeService privilege;
    @Inject public OrganizationService organization;
    @Inject public TransactionalExecutor transactionalExecutor;
    @Inject public EmailSender email;
    @Inject public EmailConstructor emailConstructor;
    @Inject public ModuleService module;
    @Inject public UserService user;
    @Inject public ValidationService validation;
    @Inject public FrontendResourceService frontendResource;
    @Inject public ServerJSRunner serverJSRunner;
    @Inject public UrlHelper url;
    @Inject public TokenService token;
    @Inject public ZipService zipService;
    @Inject public RunAsService runAs;
    @Inject public EventListenerService eventListener;
    @Inject public SessionService sessionService;
    @Inject public SchedulerService scheduler;
    @Inject public EmailService emailService;
    @Inject public LogConfigService logConfig;
    @Inject public PdfConstructor pdfConstructor;
    @Inject public NotificationService notification;
    @Inject public SystemHealthStatusService systemStatus;
    @Inject public DatabaseValidationService databaseValidationService;
    @Inject public ApiKeyService apiKey;
    @Inject public ClusterEventSenderService clusterEventSender;
    @Inject public ThymeleafService thymeleaf;
    @Inject public FileService file;
    @Inject public CaptchaService captcha;
    @Inject public RestClientService restClient;
    @Inject public TenantResolver tenantResolver;
    @Inject public FrontendMappingDefinitionService frontendMappingDefinition;
    @Inject public WebsocketService websocket;
    @Inject public UserRoleService userRole;
    @Inject public LiveDataServices data;
    @Inject public UtilServices util;
    @Inject public CustomisationService customisation;
    @Inject public FormService form;
    @Inject public ComponentExportService componentExport;
    @Inject public ZipComponentImportService zipComponentImport;
    @Inject public DatabaseValidationService databaseValidation;
    @Inject public DynamicEntityRegistrationService dynamicEntityRegistration;
    @Inject public JsParser jsParser;
    @Inject public DynamicEntityService dynamicEntity;
    @Inject public CsvService csv;
    @Inject public ChatGPTService chatGPTService;
}
