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

import com.openkoda.repository.admin.AuditRepository;
import com.openkoda.repository.event.EventListenerRepository;
import com.openkoda.repository.event.SchedulerRepository;
import com.openkoda.repository.file.FileRepository;
import com.openkoda.repository.notifications.NotificationRepository;
import com.openkoda.repository.notifications.ReadNotificationRepository;
import com.openkoda.repository.organization.OrganizationRepository;
import com.openkoda.repository.task.EmailRepository;
import com.openkoda.repository.task.HttpRequestTaskRepository;
import com.openkoda.repository.user.*;
import com.openkoda.repository.user.external.*;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

/**
 * Unsecured repository aggregator exposing direct repository access bypassing privilege enforcement.
 * <p>
 * This component aggregates unsecured repository instances without privilege validation,
 * bypassing SecureRepository privilege checks for system-level operations. It is designed
 * for use in internal processing contexts where access control has been pre-validated externally.

 * <p>
 * This class is annotated with {@code @Component} for Spring bean registration and uses field
 * injection to aggregate 25+ repository instances for organization, user, role, file, and other
 * domain entities. Access is typically performed via the {@code Repositories.unsecure} field
 * for convenience in system operations, background jobs, and batch processing.

 * <p>
 * <b>SECURITY WARNING:</b> Only use unsecure repository operations for:
 * <ul>
 *   <li>System-level operations (initialization, migrations, cleanup)</li>
 *   <li>Background jobs and scheduled tasks</li>
 *   <li>Batch processing where access control validated externally</li>
 *   <li>Internal administrative operations</li>
 * </ul>
 * Never expose unsecure repository operations directly to user requests. For user-facing
 * operations, always use {@link SecureRepositories} which enforces privilege-based access control.

 * <p>
 * Usage example (system context only):
 * <pre>
 * Organization org = repositories.unsecure.organization.findById(id).orElse(null);
 * </pre>

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Repositories
 * @see SecureRepositories
 */
@Component("UnsecureRepositories")
public class UnsecureRepositories {
//    USER
    /**
     * Direct access to user repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system operations such as user migrations,
     * batch user provisioning, or background synchronization tasks.

     */
    @Inject public UserRepository user;
    
    /**
     * Direct access to user-role association repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system role assignments, bulk role updates,
     * or administrative role reconciliation operations.

     */
    @Inject public UserRoleRepository userRole;
    
    /**
     * Direct access to Facebook user integration repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for OAuth callback processing or external user synchronization.

     */
    @Inject public FacebookUserRepository facebookUser;
    
    /**
     * Direct access to Google user integration repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for OAuth callback processing or external user synchronization.

     */
    @Inject public GoogleUserRepository googleUser;
    
    /**
     * Direct access to LDAP user integration repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for LDAP synchronization or directory service integration tasks.

     */
    @Inject public LDAPUserRepository ldapUser;
    
    /**
     * Direct access to Salesforce user integration repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for Salesforce OAuth processing or external CRM synchronization.

     */
    @Inject public SalesforceUserRepository salesforceUser;
    
    /**
     * Direct access to LinkedIn user integration repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for OAuth callback processing or professional network synchronization.

     */
    @Inject public LinkedinUserRepository linkedinUser;
    
    /**
     * Direct access to API key repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system API key generation, rotation, or cleanup operations.

     */
    @Inject public ApiKeyRepository apiKey;
    
    /**
     * Direct access to login and password repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for authentication processing, password reset operations,
     * or credential migration tasks.

     */
    @Inject public LoginAndPasswordRepository loginAndPassword;

//    ROLES
    /**
     * Direct access to role repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system role management, role migrations,
     * or administrative role provisioning operations.

     */
    @Inject public RoleRepository role;
    
    /**
     * Direct access to dynamic privilege repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system privilege creation, privilege synchronization,
     * or administrative access control configuration.

     */
    @Inject public DynamicPrivilegeRepository privilege;
    
    /**
     * Direct access to global role repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system-wide role management or global role initialization.

     */
    @Inject public GlobalRoleRepository globalRole;
    
    /**
     * Direct access to organization-scoped role repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for tenant role provisioning or organization-level role migrations.

     */
    @Inject public OrganizationRoleRepository organizationRole;
    
    /**
     * Direct access to global organization role repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for cross-organization role management or global tenant role operations.

     */
    @Inject public GlobalOrganizationRoleRepository globalOrganizationRole;

//    TASK
    /**
     * Direct access to email task repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for background email processing, scheduled email tasks,
     * or system notification operations.

     */
    @Inject public EmailRepository email;
    
    /**
     * Direct access to HTTP request task repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for background HTTP task processing, webhook execution,
     * or scheduled external API calls.

     */
    @Inject public HttpRequestTaskRepository httpRequest;

//    EVENT LISTENERS & SCHEDULERS
    /**
     * Direct access to scheduler repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system scheduler management, scheduled job configuration,
     * or background task orchestration operations.

     */
    @Inject public SchedulerRepository scheduler;
    
    /**
     * Direct access to event listener repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system event listener registration, event processing,
     * or internal event-driven architecture operations.

     */
    @Inject public EventListenerRepository eventListener;

    //    NOTIFICATIONS
    /**
     * Direct access to read notification tracking repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system notification cleanup, read status migrations,
     * or administrative notification tracking operations.

     */
    @Inject
    public ReadNotificationRepository readNotification;
    
    /**
     * Direct access to notification repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system notification generation, bulk notification processing,
     * or background notification delivery operations.

     */
    @Inject
    public NotificationRepository notification;

//    OTHER
    /**
     * Direct access to organization repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system organization provisioning, tenant migrations,
     * or administrative multi-tenancy operations.

     */
    @Inject public OrganizationRepository organization;
    
    /**
     * Direct access to audit trail repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system audit log processing, compliance reporting,
     * or administrative audit trail analysis.

     */
    @Inject public AuditRepository audit;
    
    /**
     * Direct access to frontend resource repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system frontend resource deployment, UI component migrations,
     * or administrative resource management operations.

     */
    @Inject public FrontendResourceRepository frontendResource;
    
    /**
     * Direct access to controller endpoint repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system endpoint registration, API route discovery,
     * or administrative endpoint configuration operations.

     */
    @Inject public ControllerEndpointRepository controllerEndpoint;
    
    /**
     * Direct access to server-side JavaScript code repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system script deployment, JavaScript code migrations,
     * or administrative GraalVM script management.

     */
    @Inject public ServerJsRepository serverJs;
    
    /**
     * Direct access to token repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system token generation, token cleanup operations,
     * or administrative authentication token management.

     */
    @Inject public TokenRepository token;
    
    /**
     * Direct access to global search repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system search index rebuilding, search migrations,
     * or administrative full-text search operations.

     */
    @Inject public GlobalSearchRepository search;
    
    /**
     * Direct access to map entity repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system geospatial data processing, map entity migrations,
     * or administrative geographic information operations.

     */
    @Inject public MapEntityRepository mapEntity;
    
    /**
     * Direct access to file repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system file storage operations, file migrations,
     * or administrative file cleanup and management tasks.

     */
    @Inject public FileRepository file;
    
    /**
     * Direct access to integration configuration repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system integration provisioning, third-party service setup,
     * or administrative integration configuration operations.

     */
    @Inject public IntegrationRepository integration;
    
    /**
     * Direct access to form definition repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system form deployment, form migrations,
     * or administrative dynamic form management operations.

     */
    @Inject public FormRepository form;
    
    /**
     * Direct access to native SQL query utilities bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system database operations, data migrations,
     * or administrative SQL execution tasks requiring direct database access.

     */
    @Inject public NativeQueries nativeQueries;

    /**
     * Direct access to OpenKoda module repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system module registration, module initialization,
     * or administrative module configuration operations.

     */
    @Inject public OpenkodaModuleRepository openkodaModule;
    
    /**
     * Direct access to email configuration repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system email setup, SMTP configuration migrations,
     * or administrative email service management operations.

     */
    @Inject public EmailConfigRepository emailConfig;
    
    /**
     * Direct access to dynamic entity definition repository bypassing privilege enforcement.
     * <p>
     * <b>UNSECURED</b> - Use only for system dynamic entity registration, runtime entity migrations,
     * or administrative Byte Buddy entity generation operations.

     */
    @Inject public DynamicEntityRepository dynamicEntity;
}
