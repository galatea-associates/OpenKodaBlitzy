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

import com.openkoda.repository.admin.SecureAuditRepository;
import com.openkoda.repository.ai.SecureQueryReportRepository;
import com.openkoda.repository.event.SecureEventListenerRepository;
import com.openkoda.repository.event.SecureSchedulerRepository;
import com.openkoda.repository.file.SecureFileRepository;
import com.openkoda.repository.notifications.SecureNotificationRepository;
import com.openkoda.repository.organization.SecureOrganizationRepository;
import com.openkoda.repository.user.SecureDynamicPrivilegeRepository;
import com.openkoda.repository.user.SecureRoleRepository;
import com.openkoda.repository.user.SecureUserRepository;
import com.openkoda.repository.user.SecureUserRoleRepository;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

/**
 * Privilege-enforcing repository aggregator exposing SecureRepository instances for access-controlled data operations.
 * <p>
 * This component aggregates 14 SecureRepository field instances that apply privilege enforcement on all data access operations.
 * All repositories apply USER security scope by default, validating that the current user possesses required read or write
 * privileges before executing queries or modifications. Operations throw {@code AccessDeniedException} if the user lacks
 * the necessary privileges as defined by entity {@code @Formula} annotations (requiredReadPrivilege, requiredWritePrivilege).

 * <p>
 * The class is annotated with {@code @Component("SecureRepositories")} for Spring bean registration and uses field injection
 * to aggregate repository instances. Each repository field wraps a corresponding domain repository with the SecureRepository
 * interface, providing privilege-checked alternatives to standard Spring Data JPA repository operations.

 * <p>
 * Typical usage pattern:
 * <pre>{@code
 * repositories.secure.user.findOne(userId); // finds user only if current user has read privilege
 * repositories.secure.organization.save(org); // saves only if current user has write privilege
 * }</pre>

 * <p>
 * Access this aggregator via the {@code Repositories.secure} field for privilege-checked entity access throughout the application.
 * Privilege requirements are computed from entity annotations and enforced at the repository layer, providing a centralized
 * access control mechanism that integrates with OpenKoda's Role-Based Access Control (RBAC) system.

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SecureRepository
 * @see Repositories
 * @see com.openkoda.core.security.HasSecurityRules
 */
@Component("SecureRepositories")
public class SecureRepositories {

    /**
     * Secure FrontendResource repository with privilege checks for UI component access.
     * <p>
     * Enforces privilege requirements for accessing frontend resource definitions used in visual development tools.

     */
    @Inject public SecureFrontendResourceRepository frontendResource;

    /**
     * Secure Role repository enforcing RBAC privilege requirements for role management.
     * <p>
     * Validates privileges before allowing role entity queries or modifications, ensuring proper access control
     * over role definitions and privilege assignments.

     */
    @Inject public SecureRoleRepository role;

    /**
     * Secure DynamicPrivilege repository for runtime privilege definition access control.
     * <p>
     * Enforces privilege checks on dynamic privilege entities, controlling who can view or modify
     * runtime-generated privilege definitions.

     */
    @Inject public SecureDynamicPrivilegeRepository privilege;

    /**
     * Secure EventListener repository with privilege validation for event configuration.
     * <p>
     * Applies access control to event listener entity operations, restricting access to event configuration
     * based on user privileges.

     */
    @Inject public SecureEventListenerRepository eventListener;

    /**
     * Secure Scheduler repository enforcing access control on scheduled job definitions.
     * <p>
     * Validates privileges before allowing access to scheduler entity data, controlling who can view or
     * modify scheduled job configurations.

     */
    @Inject public SecureSchedulerRepository scheduler;

    /**
     * Secure UserRole repository for privilege-checked user-role assignment operations.
     * <p>
     * Enforces access control on UserRole association entities, ensuring only authorized users can
     * query or modify user-role assignments.

     */
    @Inject public SecureUserRoleRepository userRole;

    /**
     * Secure User repository with privilege enforcement for user entity operations.
     * <p>
     * Applies privilege validation to all user entity access, restricting queries and modifications
     * based on the current user's privileges and organizational scope.

     */
    @Inject public SecureUserRepository user;

    /**
     * Secure Organization repository enforcing tenant-level privilege requirements.
     * <p>
     * Validates privileges before allowing access to organization (tenant) entities, ensuring proper
     * multi-tenancy isolation and access control.

     */
    @Inject public SecureOrganizationRepository organization;

    /**
     * Secure Notification repository with privilege checks for notification access.
     * <p>
     * Enforces access control on notification entities, restricting who can view or modify
     * notification records based on privilege requirements.

     */
    @Inject public SecureNotificationRepository notification;

    /**
     * Secure Audit repository for privilege-controlled audit log access.
     * <p>
     * Applies privilege validation to audit log queries, ensuring only authorized users can
     * access audit trail data for security and compliance monitoring.

     */
    @Inject public SecureAuditRepository audit;

    /**
     * Secure File repository enforcing file storage access control.
     * <p>
     * Validates privileges before allowing access to file entity records, controlling who can
     * view or modify file metadata and storage references.

     */
    @Inject public SecureFileRepository file;

    /**
     * Secure ServerJs repository with privilege validation for JavaScript code access.
     * <p>
     * Enforces access control on ServerJs entities containing GraalVM JavaScript code definitions,
     * restricting access to server-side script management based on user privileges.

     */
    @Inject public SecureServerJsRepository serverJs;

    /**
     * Secure Form repository enforcing privilege requirements for dynamic form definitions.
     * <p>
     * Applies privilege checks to form entity operations, controlling who can view or modify
     * dynamic form definitions used in the visual development environment.

     */
    @Inject public SecureFormRepository form;

    /**
     * Secure QueryReport repository with privilege checks for AI-generated report access.
     * <p>
     * Validates privileges before allowing access to QueryReport entities, restricting who can
     * view or execute AI-generated report definitions.

     */
    @Inject public SecureQueryReportRepository queryReport;

}
