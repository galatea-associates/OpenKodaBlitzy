/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 and associated documentation files (the "Software"), to deal in the Software without restriction, 
including without limitation the rights to use, copy, modify, merge, publish, distribute, 
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software 
is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice 
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE 
AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS 
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES 
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.core.security;


import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.core.form.FrontendMappingFieldDefinition;
import com.openkoda.core.multitenancy.TenantResolver;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.*;
import com.openkoda.model.common.EntityWithRequiredPrivilege;
import com.openkoda.model.common.LongIdEntity;
import com.openkoda.model.common.OrganizationRelatedEntity;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import static com.openkoda.model.PrivilegeNames.*;

/**
 * Central security rule surface providing Spring Security {@code @PreAuthorize} expressions, JPQL security predicates,
 * and JPA Criteria API predicate builders for the OpenKoda platform.
 * <p>
 * This interface integrates with Spring Security SpEL evaluation, method-level security annotations,
 * and SecureRepository privilege enforcement. It provides three types of security rules:
 * </p>
 * <ul>
 *   <li><b>PreAuthorize strings</b>: SpEL expressions for method-level security annotations</li>
 *   <li><b>JPQL query fragments</b>: Security predicates for {@code @Query} repository methods</li>
 *   <li><b>Programmatic BiFunction checks</b>: Lambda-based privilege validators for form lifecycle and runtime checks</li>
 * </ul>
 * <p>
 * Security rules are evaluated against the {@link OrganizationUser} principal stored in Spring Security's
 * SecurityContext. Privilege checks support both global privileges (across all organizations) and
 * organization-specific privileges for multi-tenant authorization.
 * </p>
 * <p>
 * <b>Thread-safety</b>: SecurityContext-based privilege resolution is thread-safe via ThreadLocal storage.
 * Multiple threads can safely evaluate security rules concurrently.
 * </p>
 * <p>
 * Example usage with {@code @PreAuthorize} annotation:
 * </p>
 * <pre>
 * {@code @PreAuthorize}(CHECK_CAN_READ_ORG_DATA)
 * public Organization getOrganization(Long organizationId) { ... }
 * </pre>
 *
 * @see OrganizationUser
 * @see PrivilegeBase
 * @see com.openkoda.repository.SecureRepository
 * @see UserProvider
 * @since 1.7.1
 * @author OpenKoda Team
 */
public interface HasSecurityRules extends LoggingComponentWithRequestId {

    /**
     * SpEL expression building blocks for constructing security rule strings.
     * These constants are combined to create {@code @PreAuthorize} and JPQL security expressions.
     */
    
    /** Closing parenthesis for SpEL expressions. */
    String BB_CLOSE = ")";
    
    /** Opening parenthesis for SpEL expressions. */
    String BB_OPEN = "(";
    
    /** Closing single-quoted string with parenthesis for privilege name parameters. */
    String BB_STRING_CLOSE = "')";
    
    /** Opening single-quoted string with parenthesis for privilege name parameters. */
    String BB_STRING_OPEN = "('";
    
    /** Opening JPQL SpEL expression with parameter placeholder syntax {@code (?#{...})}. */
    String BB_JPQL_OPEN = "(?#{";
    
    /** Closing JPQL SpEL expression placeholder. */
    String BB_JPQL_CLOSE = "})";
    
    /** Single-quoted string closing with comma separator for multi-parameter expressions. */
    String STRING_CLOSE_COMMA = "' ,";
    
    /** Logical OR operator for combining security predicates. */
    String OR = " OR ";
    
    /** SQL IN operator for collection membership checks. */
    String IN = " IN ";
    
    /** Boolean TRUE comparison for JPQL expressions. */
    String IS_TRUE = " = TRUE";
    
    /** Combined JPQL close with TRUE comparison for security check expressions. */
    String JPQL_CLOSE_IS_TRUE = BB_JPQL_CLOSE + IS_TRUE;
    
    /** SpEL parameter reference for organizationId method parameter. */
    String HASH_ORGANIZATION_ID = "#organizationId";

    /**
     * SpEL expression prefix for global privilege checks: {@code principal.hasGlobalPrivilege('...')}.
     * Used to verify user has privilege across all organizations.
     */
    String HAS_GLOBAL_PRIVILEGE_STRING_OPEN = "principal.hasGlobalPrivilege" + BB_STRING_OPEN;
    
    /**
     * SpEL expression prefix for organization-specific privilege checks: {@code principal.hasOrgPrivilege('...', organizationId)}.
     * Verifies user has privilege within a specific organization scope.
     */
    String HAS_ORG_PRIVILEGE_OPEN = "principal.hasOrgPrivilege" + BB_STRING_OPEN;
    
    /**
     * SpEL expression prefix for compound privilege checks: {@code principal.hasGlobalOrOrgPrivilege('...', organizationId)}.
     * Returns true if user has either global privilege or organization-specific privilege.
     */
    String HAS_GLOBAL_OR_ORG_PRIVILEGE_STRING_OPEN = "principal.hasGlobalOrOrgPrivilege" + BB_STRING_OPEN;
    
    /**
     * SpEL expression suffix for closing organization privilege checks with organizationId parameter.
     */
    String HAS_ORG_PRIVILEGE_CLOSE = STRING_CLOSE_COMMA + HASH_ORGANIZATION_ID + BB_CLOSE;
    
    /**
     * JPQL parameter placeholder for principal's global privilege set.
     * Evaluates to {@code Set<String>} of privilege names the user holds globally.
     */
    String GLOBAL_PRIVILEGES = "?#{principal.globalPrivileges}";
    
    /**
     * JPQL parameter placeholder for organization IDs where user has membership.
     * Evaluates to {@code Set<Long>} of organization identifiers.
     */
    String ORGANIZATION_IDS = "?#{principal.organizationIds}";

    /**
     * Single-privilege {@code @PreAuthorize} expressions for method-level security guards.
     * These constants can be used directly in Spring Security annotations to enforce access control.
     */
    
    /**
     * Checks if current user session is in spoof (impersonation) mode.
     * Use for audit logging and restricting actions during impersonation.
     */
    String CHECK_IS_SPOOFED =                       "(principal.isSpoofed())";
    
    /**
     * Verifies current principal matches the specified userId parameter.
     * Use for user-specific operations where users can only access their own data.
     */
    String CHECK_IS_THIS_USERID =                   "(principal.getUserId() == #userId)";
    
    /**
     * Checks global privilege to read frontend resources (UI components, templates).
     * Required for viewing frontend resource definitions and configurations.
     */
    String CHECK_CAN_READ_FRONTEND_RESOURCES =                     HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _readFrontendResource + BB_STRING_CLOSE;
//    String CHECK_CAN_READ_ORG_DATA =                HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _readOrgData               + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to manage frontend resources (create, update, delete).
     * Required for modifying frontend resource definitions, templates, and UI components.
     */
    String CHECK_CAN_MANAGE_FRONTEND_RESOURCES =    HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _manageFrontendResource    + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to edit entity attributes (dynamic fields).
     * Required for modifying attribute values on entities.
     */
    String CHECK_CAN_EDIT_ATTRIBUTES =              HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _canEditAttributes         + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to view entity attributes.
     * Required for reading attribute definitions and values.
     */
    String CHECK_CAN_SEE_ATTRIBUTES =               HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _canSeeAttributes          + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to define new entity attributes (attribute schema management).
     * Required for creating attribute definitions and configuring attribute metadata.
     */
    String CHECK_CAN_DEFINE_ATTRIBUTES =            HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _canSeeAttributes          + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to manage event listeners (backend automation hooks).
     * Required for configuring system event handlers and lifecycle callbacks.
     */
    String CHECK_CAN_MANAGE_EVENT_LISTENERS =       HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _canManageBackend          + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to read support/maintenance data.
     * Required for viewing system diagnostics, logs, and support information.
     */
    String CHECK_CAN_READ_SUPPORT_DATA =            HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _canReadSupportData        + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to manage support data (modify system configurations).
     * Required for changing system settings, maintenance mode, and support configurations.
     */
    String CHECK_CAN_MANAGE_SUPPORT_DATA =          HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _canManageSupportData      + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to read role definitions and assignments.
     * Required for viewing role configurations and user role memberships.
     */
    String CHECK_CAN_READ_ROLES =                   HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _canReadBackend            + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to manage roles (create, update, delete role definitions).
     * Required for modifying role privileges and role metadata.
     */
    String CHECK_CAN_MANAGE_ROLES =                 HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _canManageBackend          + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to read user data across all organizations.
     * Required for viewing user profiles, authentication details, and user settings.
     */
    String CHECK_CAN_READ_USER_DATA =               HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _readUserData              + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to read organization audit logs.
     * Required for viewing audit trail, entity change history, and security events.
     */
    String CHECK_CAN_READ_ORG_AUDIT =               HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _readOrgAudit              + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to assign/remove roles for users.
     * Required for managing user role associations and permissions.
     */
    String CHECK_CAN_MANAGE_USER_ROLES =            HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _manageUserRoles           + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to impersonate other users (spoof mode).
     * Required for support personnel to troubleshoot user-specific issues.
     */
    String CHECK_CAN_IMPERSONATE =                  HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _canImpersonate            + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to access system-wide global settings.
     * Required for viewing and modifying platform-wide configuration.
     */
    String CHECK_CAN_ACCESS_GLOBAL_SETTINGS =       HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _canAccessGlobalSettings   + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to read backend system configurations.
     * Required for viewing backend settings, integrations, and system metadata.
     */
    String CHECK_CAN_READ_BACKEND =                 HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _canReadBackend            + BB_STRING_CLOSE;
    
    /**
     * Checks global privilege to manage backend system configurations.
     * Required for modifying backend settings, enabling integrations, and system administration.
     */
    String CHECK_CAN_MANAGE_BACKEND =               HAS_GLOBAL_PRIVILEGE_STRING_OPEN + _canManageBackend          + BB_STRING_CLOSE;

    /**
     * Compound {@code @PreAuthorize} expressions combining global and organization-specific privilege checks.
     * These rules support multi-tenant authorization by checking privileges at both global and organization scopes.
     */
    
    /**
     * Checks privilege to manage organization data (create, update, delete org entities).
     * Returns true if user has global manageOrgData privilege OR organization-specific privilege.
     * Requires organizationId parameter in method signature.
     */
    String CHECK_CAN_MANAGE_ORG_DATA =              HAS_GLOBAL_OR_ORG_PRIVILEGE_STRING_OPEN + _manageOrgData  + HAS_ORG_PRIVILEGE_CLOSE;
    
    /**
     * Checks privilege to read organization data.
     * Returns true if user has global readOrgData privilege OR organization-specific privilege.
     * Requires organizationId parameter in method signature.
     */
    String CHECK_CAN_READ_ORG_DATA =                HAS_GLOBAL_OR_ORG_PRIVILEGE_STRING_OPEN + _readOrgData  + HAS_ORG_PRIVILEGE_CLOSE;
    
    /**
     * Checks privilege to use AI features (ChatGPT integration, reporting AI).
     * Returns true if user has global useReportingAI privilege OR organization-specific privilege.
     * Requires organizationId parameter in method signature.
     */
    String CHECK_CAN_USE_AI =                       HAS_GLOBAL_OR_ORG_PRIVILEGE_STRING_OPEN + _useReportingAI + HAS_ORG_PRIVILEGE_CLOSE;
    
    /**
     * Checks privilege to create reports.
     * Returns true if user has global createReports privilege OR organization-specific privilege.
     * Requires organizationId parameter in method signature.
     */
    String CHECK_CAN_CREATE_REPORTS =               HAS_GLOBAL_OR_ORG_PRIVILEGE_STRING_OPEN + _createReports  + HAS_ORG_PRIVILEGE_CLOSE;
    
    /**
     * Checks privilege to read reports.
     * Returns true if user has global readReports privilege OR organization-specific privilege.
     * Requires organizationId parameter in method signature.
     */
    String CHECK_CAN_READ_REPORTS =                 HAS_GLOBAL_OR_ORG_PRIVILEGE_STRING_OPEN + _readReports  + HAS_ORG_PRIVILEGE_CLOSE;
    
    /**
     * Checks privilege to read user settings with ownership fallback.
     * Returns true if user has readUserData privilege (global or org-specific) OR is accessing their own data.
     * Requires organizationId and userId parameters.
     */
    String CHECK_CAN_READ_USER_SETTINGS =           BB_OPEN +  HAS_GLOBAL_OR_ORG_PRIVILEGE_STRING_OPEN + _readUserData + HAS_ORG_PRIVILEGE_CLOSE + OR  + CHECK_IS_THIS_USERID + BB_CLOSE;
    
    /**
     * Checks privilege to manage user settings with ownership fallback.
     * Returns true if user has manageUserData privilege (global or org-specific) OR is modifying their own settings.
     * Requires organizationId and userId parameters.
     */
    String CHECK_CAN_MANAGE_USER_SETTINGS =         BB_OPEN +  HAS_GLOBAL_OR_ORG_PRIVILEGE_STRING_OPEN + _manageUserData + HAS_ORG_PRIVILEGE_CLOSE + OR  + CHECK_IS_THIS_USERID + BB_CLOSE;
    
    /**
     * Checks if user can impersonate OR is already in spoof mode.
     * Used for operations that require elevated privileges or are performed during impersonation.
     */
    String CHECK_CAN_IMPERSONATE_OR_IS_SPOOFED =    BB_OPEN + CHECK_CAN_IMPERSONATE         + OR + CHECK_IS_SPOOFED                                                 + BB_CLOSE;
    
    /**
     * Checks privilege to read Facebook user integration data with ownership check.
     * Returns true if user has global readUserData privilege OR is accessing their own Facebook integration.
     * Requires facebookId parameter.
     */
    String CHECK_CAN_READ_FACEBOOK_USER =           BB_OPEN + CHECK_CAN_READ_USER_DATA + OR + "principal.user.facebookUser.facebookId == #facebookId"          + BB_CLOSE;
    
    /**
     * Checks privilege to read Google user integration data with ownership check.
     * Returns true if user has global readUserData privilege OR is accessing their own Google integration.
     * Requires googleId parameter.
     */
    String CHECK_CAN_READ_GOOGLE_USER =             BB_OPEN + CHECK_CAN_READ_USER_DATA + OR + "principal.user.googleUser.googleId == #googleId"                + BB_CLOSE;
    
    /**
     * Checks privilege to read LDAP user integration data with ownership check.
     * Returns true if user has global readUserData privilege OR is accessing their own LDAP integration.
     * Requires uid parameter.
     */
    String CHECK_CAN_READ_LDAP_USER =               BB_OPEN + CHECK_CAN_READ_USER_DATA + OR + "principal.user.ldapUser.uid == #uid"                            + BB_CLOSE;
    
    /**
     * Checks privilege to read Salesforce user integration data with ownership check.
     * Returns true if user has global readUserData privilege OR is accessing their own Salesforce integration.
     * Requires salesforceId parameter.
     */
    String CHECK_CAN_READ_SALESFORCE_USER =         BB_OPEN + CHECK_CAN_READ_USER_DATA + OR + "principal.user.salesforceUser.salesforceId == #salesforceId"    + BB_CLOSE;
    
    /**
     * Checks privilege to read LinkedIn user integration data with ownership check.
     * Returns true if user has global readUserData privilege OR is accessing their own LinkedIn integration.
     * Requires linkedinId parameter.
     */
    String CHECK_CAN_READ_LINKEDIN_USER =           BB_OPEN + CHECK_CAN_READ_USER_DATA + OR + "principal.user.linkedinUser.linkedinId == #linkedinId"          + BB_CLOSE;
    
    /**
     * Checks privilege to invite users to an organization.
     * Returns true if user has global manageOrgData privilege OR organization-specific privilege for the target organization.
     * Requires organization parameter (extracts organization.id).
     */
    String CHECK_CAN_INVITE_USER_TO_ORG =           HAS_GLOBAL_OR_ORG_PRIVILEGE_STRING_OPEN + _manageOrgData               + "', #organization.id)";
    
    /**
     * Checks privilege to save user role assignments in an organization.
     * Returns true if user has global manageUserRoles privilege OR organization-specific privilege.
     * Requires organizationId parameter.
     */
    String CHECK_CAN_SAVE_USER_ROLES =              HAS_GLOBAL_OR_ORG_PRIVILEGE_STRING_OPEN + _manageUserRoles       + "', #organizationId)";

    /**
     * Checks privilege to edit user-specific attributes.
     * Returns true if user has general attribute editing privilege OR specific user attribute editing privilege.
     */
    String CHECK_CAN_EDIT_USER_ATTRIBUTES =         BB_OPEN + CHECK_CAN_EDIT_ATTRIBUTES     + OR +  HAS_GLOBAL_PRIVILEGE_STRING_OPEN    + _canEditUserAttributes    + BB_STRING_CLOSE       + BB_CLOSE;
    
    /**
     * Checks privilege to edit organization-specific attributes.
     * Returns true if user has general attribute editing privilege OR organization-specific attribute editing privilege.
     * Requires organizationId parameter.
     */
    String CHECK_CAN_EDIT_ORG_ATTRIBUTES =          BB_OPEN + CHECK_CAN_EDIT_ATTRIBUTES     + OR +  HAS_ORG_PRIVILEGE_OPEN              + _canEditOrgAttributes     + HAS_ORG_PRIVILEGE_CLOSE + BB_CLOSE;

    /**
     * JPQL security predicates for use in {@code @Query} repository method annotations.
     * These expressions are embedded in JPQL WHERE clauses to enforce row-level security.
     */
    
    /**
     * JPQL predicate checking global privilege to read frontend resources.
     * Use in repository {@code @Query} WHERE clauses to filter results by privilege.
     */
    String CHECK_CAN_READ_FRONTEND_RESOURCES_JPQL = BB_JPQL_OPEN + CHECK_CAN_READ_FRONTEND_RESOURCES + JPQL_CLOSE_IS_TRUE;
    
    /**
     * JPQL predicate checking global privilege to read roles.
     * Filters query results to only roles the user has permission to view.
     */
    String CHECK_CAN_READ_ROLES_JPQL =              BB_JPQL_OPEN + CHECK_CAN_READ_ROLES       + JPQL_CLOSE_IS_TRUE;
    
    /**
     * JPQL predicate checking global privilege to manage roles.
     * Filters query results to only roles the user can modify.
     */
    String CHECK_CAN_MANAGE_ROLES_JPQL =            BB_JPQL_OPEN + CHECK_CAN_MANAGE_ROLES     + JPQL_CLOSE_IS_TRUE;
    
    /**
     * JPQL predicate checking global privilege to read organization audit logs.
     * Filters audit entries based on user's audit access privileges.
     */
    String CHECK_CAN_READ_ORG_AUDIT_JPQL =          BB_JPQL_OPEN + CHECK_CAN_READ_ORG_AUDIT   + JPQL_CLOSE_IS_TRUE;

    /**
     * Complex JPQL security predicates combining multiple authorization checks.
     * These expressions support entity-specific requiredPrivilege fields and organization membership.
     */
    
    /**
     * JPQL predicate for frontend resources with entity-specific required privileges.
     * Returns true if user can read/manage frontend resources OR has the specific privilege required by the resource.
     * Checks dbFrontendResource.requiredPrivilege against user's global privileges.
     */
    String CHECK_CAN_MANAGE_FRONTEND_RESOURCES_OR_HAS_REQUIRED_PRIVILEGE_JPQL =  BB_OPEN + BB_JPQL_OPEN + CHECK_CAN_READ_FRONTEND_RESOURCES + OR + CHECK_CAN_MANAGE_FRONTEND_RESOURCES + JPQL_CLOSE_IS_TRUE + OR + "dbFrontendResource.requiredPrivilege" + IN + GLOBAL_PRIVILEGES + BB_CLOSE;
    
    /**
     * JPQL predicate for organization data with membership fallback.
     * Returns true if user has readOrgData privilege OR is a member of the organization.
     * Checks dbOrganization.id against user's organization membership set.
     */
    String CHECK_CAN_READ_ORG_DATA_OR_IS_ORG_MEMEBER_JPQL =       BB_OPEN + BB_JPQL_OPEN + CHECK_CAN_READ_ORG_DATA                                  + JPQL_CLOSE_IS_TRUE + OR + "dbOrganization.id" + IN + ORGANIZATION_IDS             + BB_CLOSE;
    
    /**
     * JPQL predicate for user role data with ownership check.
     * Returns true if user has readUserData privilege OR is viewing their own role assignments.
     * Checks dbUserRole.userId against principal's user ID.
     */
    String CHECK_CAN_READ_USER_OR_OWNER_JPQL =                    BB_OPEN + BB_JPQL_OPEN + CHECK_CAN_READ_USER_DATA + JPQL_CLOSE_IS_TRUE + OR + "dbUserRole.userId = ?#{principal.user.id}"             + BB_CLOSE;
    
    /**
     * JPQL predicate for managing user roles with organization membership fallback.
     * Returns true if user has manageUserRoles privilege OR user is member of role's organization.
     * Checks dbUserRole.organizationId against user's organization membership set.
     */
    String CHECK_CAN_MANAGE_USER_ROLES_JPQL =                     BB_OPEN + BB_JPQL_OPEN + CHECK_CAN_MANAGE_USER_ROLES                              + JPQL_CLOSE_IS_TRUE + OR + "dbUserRole.organizationId" + IN + ORGANIZATION_IDS     + BB_CLOSE;

    /**
     * Lambda-based privilege validators for programmatic security checks in form lifecycle and runtime validation.
     * These {@link BiFunction} instances accept an {@link OrganizationUser} and {@link LongIdEntity}
     * and return {@code Boolean} indicating access permission.
     */
    
    /**
     * Checks if entity is new (null) OR user owns it OR user has required read privilege.
     * Used in form populateFrom() lifecycle to determine if user can read entity data.
     * Returns true if:
     * <ul>
     *   <li>Entity is null (new entity creation scenario)</li>
     *   <li>Entity has no requiredReadPrivilege set (publicly readable)</li>
     *   <li>User has global privilege matching entity's requiredReadPrivilege</li>
     * </ul>
     */
    BiFunction<OrganizationUser, LongIdEntity, Boolean> CHECK_IS_NEW_USER_OR_OWNER =
            (u, a) -> (a) == null
            || ((EntityWithRequiredPrivilege) a).getRequiredReadPrivilege() == null
            || u.hasGlobalPrivilege(((EntityWithRequiredPrivilege) a).getRequiredReadPrivilege());
    
    /**
     * Checks if user can write to entity based on requiredWritePrivilege field.
     * Used in form populateTo() lifecycle to determine if user can save entity modifications.
     * Returns true if:
     * <ul>
     *   <li>Entity is null (new entity creation allowed)</li>
     *   <li>Entity has no requiredWritePrivilege set (publicly writable)</li>
     *   <li>User has global privilege matching entity's requiredWritePrivilege</li>
     * </ul>
     */
    BiFunction<OrganizationUser, LongIdEntity, Boolean> CHECK_IF_CAN_WRITE_USER =
            (u, a) -> (a) == null
            || ((EntityWithRequiredPrivilege) a).getRequiredWritePrivilege() == null
            || u.hasGlobalPrivilege(((EntityWithRequiredPrivilege) a).getRequiredWritePrivilege());


    /**
     * Core security rule evaluation methods.
     * These methods check privilege possession at global and organization scopes.
     */
    
    /**
     * Checks if a privilege name exists in the global privileges set.
     *
     * @param p privilege name to check (null-safe)
     * @param globalPrivileges set of privilege names the user holds globally
     * @return true if privilege name is in global privileges set, false otherwise
     */
    default boolean hasGlobalPrivilege(String p, Set<String> globalPrivileges) {
        if (p == null) { return false; }
        return globalPrivileges.contains(p);
    }

    /**
     * Checks if a privilege name exists in organization-specific privileges for given organization.
     *
     * @param p privilege name to check (null-safe)
     * @param orgId organization identifier (null-safe)
     * @param organizationPrivileges map of organization ID to privilege name sets
     * @return true if user has privilege in specified organization, false otherwise
     */
    default boolean hasOrgPrivilege(String p, Long orgId, Map<Long, Set<String>> organizationPrivileges) {
        if (p == null || orgId == null) { return false; }
        return organizationPrivileges.containsKey(orgId) && organizationPrivileges.get(orgId).contains(p);
    }

    /**
     * Checks if a privilege enum exists in the global privileges set.
     *
     * @param p privilege enum instance (null-safe)
     * @param globalPrivileges set of privilege names the user holds globally
     * @return true if privilege enum name is in global privileges set, false otherwise
     */
    default boolean hasGlobalPrivilege(PrivilegeBase p, Set<String> globalPrivileges) {
        if (p == null) { return false; }
        return globalPrivileges.contains(p.name());
    }

    /**
     * Checks if a privilege enum exists in organization-specific privileges for given organization.
     *
     * @param p privilege enum instance (null-safe)
     * @param orgId organization identifier (null-safe)
     * @param organizationPrivileges map of organization ID to privilege name sets
     * @return true if user has privilege in specified organization, false otherwise
     */
    default boolean hasOrgPrivilege(PrivilegeBase p, Long orgId, Map<Long, Set<String>> organizationPrivileges) {
        if (p == null || orgId == null) { return false; }
        return organizationPrivileges.containsKey(orgId) && organizationPrivileges.get(orgId).contains(p.name());
    }

    /**
     * Checks if user has privilege globally OR in specific organization (compound check).
     *
     * @param privilege privilege enum to check (null-safe)
     * @param orgId organization identifier (null-safe)
     * @param globalPrivileges set of global privilege names
     * @param organizationPrivileges map of organization ID to privilege name sets
     * @return true if user has global privilege OR organization-specific privilege
     */
    default boolean hasGlobalOrOrgPrivilege(PrivilegeBase privilege, Long orgId, Set<String> globalPrivileges, Map<Long, Set<String>> organizationPrivileges) {
        return hasGlobalPrivilege(privilege, globalPrivileges) || hasOrgPrivilege(privilege, orgId, organizationPrivileges);
    }

    /**
     * Checks if user has privilege (by name) globally OR in specific organization (compound check).
     *
     * @param privilegeName privilege name string to check (null-safe)
     * @param orgId organization identifier (null-safe)
     * @param globalPrivileges set of global privilege names
     * @param organizationPrivileges map of organization ID to privilege name sets
     * @return true if user has global privilege OR organization-specific privilege
     */
    default boolean hasGlobalOrOrgPrivilege(String privilegeName, Long orgId, Set<String> globalPrivileges, Map<Long, Set<String>> organizationPrivileges) {
        return hasGlobalPrivilege(privilegeName, globalPrivileges) || hasOrgPrivilege(privilegeName, orgId, organizationPrivileges);
    }

    /**
     * Checks if current user from SecurityContext has global privilege (by enum).
     *
     * @param p privilege enum instance (null-safe)
     * @return true if current user has global privilege, false if no user or privilege not granted
     */
    default boolean hasGlobalPrivilege(PrivilegeBase p) {
        if (p == null) { return false; }
        return hasGlobalPrivilege(p.name());
    }

    /**
     * Checks if a User entity has privilege globally OR in specific organization by examining UserRole associations.
     * This method directly inspects User's role collection rather than using SecurityContext.
     *
     * @param u User entity to check
     * @param p privilege name string
     * @param orgId organization identifier
     * @return true if user has global role with privilege OR organization role with privilege
     */
    default boolean hasGlobalOrOrgPrivilege(User u, String p, Long orgId) {
        Collection<UserRole> urs = u.getRoles();
        for( UserRole ur : urs ) {
            if (ur.isGlobal() && ur.getRole().hasPrivilege(p)) {
                return true;
            }
            if (!ur.isGlobal() && ur.getOrganizationId().equals(orgId) && ur.getRole().hasPrivilege(p)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if current user from SecurityContext has global privilege (by name).
     * Retrieves {@link OrganizationUser} principal from Spring Security context and delegates to principal's privilege check.
     *
     * @param p privilege name string (null-safe)
     * @return true if current user has global privilege, false if no authenticated user or privilege not granted
     */
    default boolean hasGlobalPrivilege(String p) {
        Optional<OrganizationUser> user = UserProvider.getFromContext();
        if (user.isPresent()) {
            return user.get().hasGlobalPrivilege(p);
        }
        return false;
    }

    /**
     * Checks if current user has organization-specific privilege (by enum).
     * Retrieves current user from SecurityContext and checks organization privilege.
     *
     * @param p privilege enum instance (null-safe)
     * @param orgId organization identifier (null-safe)
     * @return true if current user has privilege in organization, false otherwise
     */
    default boolean hasOrgPrivilege(PrivilegeBase p, Long orgId) {
        if (p == null) { return false; }
        return hasOrgPrivilege(p.name(), orgId);
    }

    /**
     * Checks if current user from SecurityContext has organization-specific privilege (by name).
     * Retrieves {@link OrganizationUser} principal and delegates to principal's organization privilege check.
     *
     * @param p privilege name string (null-safe)
     * @param orgId organization identifier (null-safe)
     * @return true if current user has privilege in organization, false if no user or privilege not granted
     */
    default boolean hasOrgPrivilege(String p, Long orgId) {
        Optional<OrganizationUser> user = UserProvider.getFromContext();
        if (user.isPresent()) {
            return user.get().hasOrgPrivilege(p, orgId);
        }
        return false;
    }

    /**
     * Checks if current user has privilege globally OR in specific organization (by name).
     * Compound check combining global and organization-specific privilege evaluation.
     *
     * @param p privilege name string (null-safe)
     * @param orgId organization identifier (null-safe)
     * @return true if current user has global OR organization-specific privilege
     */
    default boolean hasGlobalOrOrgPrivilege(String p, Long orgId) {
        return hasGlobalPrivilege(p) || hasOrgPrivilege(p, orgId);
    }

    /**
     * Checks if current user has privilege globally OR in specific organization (by enum).
     * Compound check combining global and organization-specific privilege evaluation.
     *
     * @param p privilege enum instance (null-safe)
     * @param orgId organization identifier (null-safe)
     * @return true if current user has global OR organization-specific privilege
     */
    default boolean hasGlobalOrOrgPrivilege(PrivilegeBase p, Long orgId) {
        return hasGlobalOrOrgPrivilege(p.name(), orgId);
    }

    /**
     * Checks if current user can access admin panel UI.
     * Returns true if user has ANY of the admin-related global privileges (OR logic).
     *
     * @return true if user has at least one administrative privilege
     */
    default boolean canSeeAdminPanel(){
        return hasGlobalPrivilege(Privilege.canAccessGlobalSettings) || hasGlobalPrivilege(Privilege.canReadBackend) || hasGlobalPrivilege(Privilege.canReadSupportData)
                || hasGlobalPrivilege(Privilege.readUserData) || hasGlobalPrivilege(Privilege.readOrgData) || hasGlobalPrivilege(Privilege.readFrontendResource);
    }
    
    /**
     * Checks if current user is a super user with full administrative privileges.
     * Returns true ONLY if user has ALL specified global privileges (AND logic).
     * Super users have complete control over system configuration, users, organizations, and backend settings.
     *
     * @return true if user has all super user privileges, false otherwise
     */
    default boolean isSuperUser(){
        return hasGlobalPrivilege(Privilege.canAccessGlobalSettings) && hasGlobalPrivilege(Privilege.canReadBackend)
                && hasGlobalPrivilege(Privilege.readUserData) && hasGlobalPrivilege(Privilege.readOrgData)
                && hasGlobalPrivilege(Privilege.canManageBackend)
                && hasGlobalPrivilege(Privilege.manageUserData) && hasGlobalPrivilege(Privilege.manageOrgData)
                && hasGlobalPrivilege(Privilege.canChangeEntityOrganization);
    }
    
    /**
     * Retrieves the currently authenticated {@link OrganizationUser} principal from Spring Security context.
     *
     * @return Optional containing OrganizationUser if authenticated, empty Optional otherwise
     */
    default Optional<OrganizationUser> getLoggedOrganizationUser() {
        return UserProvider.getFromContext();
    }

    /**
     * Form field security integration methods.
     * These methods check privileges for FrontendMappingFieldDefinition fields during form lifecycle.
     */
    
    /**
     * Checks if current user can read form field value in specified organization context.
     * Uses field's readPrivilege and canReadCheck lambda for validation.
     *
     * @param field frontend mapping field definition with security configuration
     * @param entity entity instance being accessed (may be null for new entities)
     * @param organizationId organization context for privilege check
     * @return true if user can read field value, false otherwise
     */
    default boolean canReadFieldInOrganization(FrontendMappingFieldDefinition field, LongIdEntity entity, Long organizationId) {
        return hasFieldPrivileges(field.readPrivilege, field.canReadCheck, entity, organizationId);
    }

    /**
     * Checks if current user can read form field value considering entity-specific privileges.
     * Evaluates field's strictReadAccess flag to determine privilege enforcement mode.
     *
     * @param field frontend mapping field definition with security configuration
     * @param entity entity instance being accessed (may be null for new entities)
     * @return true if user can read field value, false otherwise
     */
    default boolean canReadField(FrontendMappingFieldDefinition field, LongIdEntity entity) {
        return hasFieldPrivileges(field.readPrivilege, field.canReadCheck, entity, null, field.isStrictReadAccess());
    }

    /**
     * Checks if current user can read an option with privilege requirement.
     * Used for dropdown options and select fields with privilege-based visibility.
     *
     * @param option option with privilege requirement
     * @param orgId organization context for privilege check
     * @return true if user has required privilege (global or org-specific)
     */
    default boolean canReadOption(OptionWithPrivilege option, Long orgId) {
        return hasOrgPrivilege(option.getPrivilege(), orgId) || hasGlobalPrivilege(option.getPrivilege());
    }

    /**
     * Checks if current user has global read privilege for form field.
     * Simpler check without organization context or entity-specific validation.
     *
     * @param field frontend mapping field definition
     * @return true if user has global read privilege
     */
    default boolean canReadGlobalField(FrontendMappingFieldDefinition field) {
        return hasGlobalPrivilege(field.readPrivilege);
    }

    /**
     * Checks if current user has read privilege (global or org-specific) for form field.
     * Organization context required for compound privilege evaluation.
     *
     * @param field frontend mapping field definition
     * @param organizationId organization context for privilege check
     * @return true if user has global OR organization-specific read privilege
     */
    default boolean canReadGlobalOrOrgField(FrontendMappingFieldDefinition field, Long organizationId) {
        return hasGlobalOrOrgPrivilege(field.readPrivilege, organizationId);
    }
    
    /**
     * Checks if current user can write to form field value in specified organization context.
     * Uses field's writePrivilege and canWriteCheck lambda for validation.
     *
     * @param field frontend mapping field definition with security configuration
     * @param entity entity instance being modified (may be null for new entities)
     * @param organizationId organization context for privilege check
     * @return true if user can write field value, false otherwise
     */
    default boolean canWriteFieldInOrganization(FrontendMappingFieldDefinition field, LongIdEntity entity, Long organizationId) {
        return hasFieldPrivileges(field.writePrivilege, field.canWriteCheck, entity, organizationId);
    }

    /**
     * Checks if current user can write to form field value considering entity-specific privileges.
     * Evaluates field's strictWriteAccess flag to determine privilege enforcement mode.
     *
     * @param field frontend mapping field definition with security configuration
     * @param entity entity instance being modified (may be null for new entities)
     * @return true if user can write field value, false otherwise
     */
    default boolean canWriteField(FrontendMappingFieldDefinition field, LongIdEntity entity) {
        return hasFieldPrivileges(field.writePrivilege, field.canWriteCheck, entity, null, field.isStrictWriteAccess());
    }

    /**
     * Checks if current user has global write privilege for form field.
     * Simpler check without organization context or entity-specific validation.
     *
     * @param field frontend mapping field definition
     * @return true if user has global write privilege
     */
    default boolean canWriteGlobalField(FrontendMappingFieldDefinition field) {
        return hasGlobalPrivilege(field.writePrivilege);
    }
    
    /**
     * Type checking utility methods for entity classification.
     * Used to determine security scope and privilege requirements for entities.
     */
    
    /**
     * Checks if entity class implements {@link OrganizationRelatedEntity} interface.
     * Organization-related entities have organizationId field and support tenant-aware queries.
     *
     * @param cls entity class to check
     * @return true if class implements OrganizationRelatedEntity
     */
    default boolean isOrganizationRelated(Class<?> cls){
        return OrganizationRelatedEntity.class.isAssignableFrom(cls);
    }
    
    /**
     * Checks if entity class implements {@link EntityWithRequiredPrivilege} interface.
     * These entities have requiredReadPrivilege and requiredWritePrivilege fields for row-level security.
     *
     * @param cls entity class to check
     * @return true if class implements EntityWithRequiredPrivilege
     */
    default boolean requiresPrivilege(Class<?> cls){
        return EntityWithRequiredPrivilege.class.isAssignableFrom(cls);
    }
    
    /**
     * Checks if entity class implements {@link IsManyOrganizationsRelatedEntity} interface.
     * These entities can be associated with multiple organizations via organizationIds array field.
     *
     * @param cls entity class to check
     * @return true if class implements IsManyOrganizationsRelatedEntity
     */
    default boolean isManyOrganizationRelated(Class<?> cls){
        return IsManyOrganizationsRelatedEntity.class.isAssignableFrom(cls);
    }

    /**
     * Comprehensive field privilege evaluation with entity context and custom check function.
     * This method performs multi-level privilege checking:
     * <ol>
     *   <li>Check global privilege</li>
     *   <li>If entity exists and is organization-related, check org-specific privilege</li>
     *   <li>If entity is new, check privilege in specified organization</li>
     *   <li>Apply custom BiFunction check if provided and explicitPrivileges mode matches</li>
     * </ol>
     *
     * @param fieldPrivilege base privilege required (e.g., readOrgData, manageOrgData)
     * @param check optional lambda function for additional validation (null-safe)
     * @param entity entity instance being accessed (null for new entities)
     * @param organizationId organization context when entity is null
     * @param explicitPrivileges if true, applies check only when privilege granted; if false, applies when privilege denied
     * @return true if user has required privilege and passes custom check, false otherwise
     */
    default boolean hasFieldPrivileges(PrivilegeBase fieldPrivilege,
            BiFunction<OrganizationUser, LongIdEntity, Boolean> check,
            LongIdEntity entity,
            Long organizationId,
            boolean explicitPrivileges) {
        Optional<OrganizationUser> u = UserProvider.getFromContext();

        if (!u.isPresent()) {
            return false;
        }

        OrganizationUser user = u.get();

        //if user has global privilege, then he has access to the field
        boolean canDo = user.hasGlobalPrivilege(fieldPrivilege);
        boolean entityExists = entity != null;


        if (!canDo) {

            if (entityExists) {
                //if entity exists and is organization related, then check if user has privilege in that organization
                if (!canDo && isOrganizationRelated(entity.getClass())) {
                    OrganizationRelatedEntity e = (OrganizationRelatedEntity) entity;
                    if (e.getOrganizationId() != null) {
                        canDo = user.hasOrgPrivilege(fieldPrivilege, e.getOrganizationId());
                    }
                }

            } else {

                //if the entity does not exist (and user has no global privilege), then it's a new entity
                //check against organization
                if (organizationId != null) {
                    canDo = user.hasOrgPrivilege(fieldPrivilege, organizationId);
                }

            }

        }

        //if still has access but there is check function, check it against the entity (entity can be null)
        if ((explicitPrivileges && canDo || !explicitPrivileges && !canDo) && check != null) {
            Boolean checkResult = check.apply(user, entity);
            canDo = (checkResult != null && checkResult);
        }

        return canDo;
    }
    
    /**
     * Simplified field privilege check with default non-explicit privilege mode.
     * Calls comprehensive hasFieldPrivileges with explicitPrivileges=false.
     *
     * @param fieldPrivilege base privilege required
     * @param check optional lambda function for additional validation (null-safe)
     * @param entity entity instance being accessed (null for new entities)
     * @param organizationId organization context when entity is null
     * @return true if user has required privilege and passes custom check
     */
    default boolean hasFieldPrivileges(
            PrivilegeBase fieldPrivilege,
            BiFunction<OrganizationUser, LongIdEntity, Boolean> check,
            LongIdEntity entity,
            Long organizationId) {
        return hasFieldPrivileges(fieldPrivilege, check, entity, null, false);
    }

    /**
     * Checks if provided userId matches the current authenticated user's ID.
     * Used for ownership validation in user-specific operations.
     *
     * @param userId user identifier to compare with current user
     * @return true if userId matches current user, false if no authenticated user or IDs don't match
     */
    default boolean isItYou(Long userId) {
        return UserProvider.getFromContext()
                .map(OrganizationUser::getUser)
                .map(User::getId)
                .map(a -> a.equals(userId))
                .orElse(false);
    }


    /**
     * Specialized convenience methods for common privilege checks.
     * These methods provide readable names for frequently-used authorization scenarios.
     */
    
    /**
     * Checks if current user can reset user passwords (administrative function).
     *
     * @return true if user has canResetPassword global privilege
     */
    default boolean canResetPassword() {
        return hasGlobalPrivilege(Privilege.canResetPassword);
    }

    /**
     * Checks if current user can edit user data globally.
     *
     * @return true if user has manageUserData global privilege
     */
    default boolean canEditUserData() {
        return hasGlobalPrivilege(Privilege.manageUserData);
    }

    /**
     * Checks if current user can impersonate other users (spoof mode).
     *
     * @return true if user has canImpersonate global privilege
     */
    default boolean canImpersonate() {
        return hasGlobalPrivilege(Privilege.canImpersonate);
    }

    /**
     * Checks if current session is in spoof mode (user is impersonating another user).
     * Used for audit logging and restricting sensitive operations during impersonation.
     *
     * @return true if current user is spoofed (impersonating), false otherwise
     */
    default boolean isSpoofMode() {
        Optional<OrganizationUser> user = getLoggedOrganizationUser();
        return user.map(OrganizationUser::isSpoofed).orElse(false);
    }

    /**
     * Checks if current user is administrator of specified organization.
     * Organization admins have manageOrgData privilege within their organization scope.
     *
     * @param org organization to check (null-safe)
     * @return true if user has manageOrgData privilege in organization
     */
    default boolean isOrgAdmin(Organization org) {
        if (org == null) {
            return false;
        }
        return hasOrgPrivilege(Privilege.manageOrgData, org.getId());
    }

    /**
     * Checks if provided User entity represents the current authenticated user.
     *
     * @param user User entity to compare with current user (null-safe)
     * @return true if User's ID matches current user
     */
    default boolean isItYou(User user) {
        return user != null && isItYou(user.getId());
    }

    /**
     * Checks if current user can see email address of specified User entity.
     * Returns true if viewing own email OR has canSeeUserEmail privilege.
     *
     * @param user User entity whose email visibility is checked (null-safe)
     * @return true if user can see email address
     */
    default boolean canSeeEmail(User user) {
        return canSeeEmail(user.getId());
    }

    /**
     * Checks if current user can see email address of user with specified ID.
     * Returns true if viewing own email OR has canSeeUserEmail privilege.
     *
     * @param userId user identifier whose email visibility is checked
     * @return true if user can see email address
     */
    default boolean canSeeEmail(Long userId) {
        return isItYou(userId) || hasGlobalPrivilege(Privilege.canSeeUserEmail);
    }

    /**
     * JPA Criteria API utility to determine if search predicate building should continue.
     * Returns false if predicate is empty OR expression (would return no results).
     *
     * @param search current JPA Predicate being built
     * @return true if predicate building should continue, false if empty OR detected
     */
    default boolean continueBuildingSearchPredicate(Predicate search) {
        return not(search.getExpressions().isEmpty()
                && Predicate.BooleanOperator.OR.equals(search.getOperator()));
    }


    /**
     * Security scope enumeration for JPA Criteria API predicate building.
     * Defines different levels of data filtering based on organization context and user privileges.
     * <p>
     * Example usage in repository:
     * </p>
     * <pre>
     * toSecurePredicate(specification, Privilege.readOrgData, root, query, cb, SecurityScope.USER_IN_ORGANIZATION);
     * </pre>
     */
    enum SecurityScope {
        /**
         * Returns all data without security filtering.
         * No user authentication required. No organization or privilege checks applied.
         * Use for public data or system-level operations.
         */
        ALL(false, false, false), // all data
        
        /**
         * Returns only global entities (organizationId is null).
         * Requires authenticated user. No privilege checks applied.
         * Use for platform-wide entities not scoped to any organization.
         */
        GLOBAL(true, false, false), // orgId == null
        
        /**
         * Returns entities belonging to current organization from tenant context.
         * Requires authenticated user. Filters by organizationId == current organization.
         * Use for organization-scoped operations without privilege filtering.
         */
        ORGANIZATION(true, false, true), //orgId == current organization
        
        /**
         * Returns entities where user has required privileges (global or organization-specific).
         * Checks user's privilege set against entity's requiredPrivilege field.
         * Includes both global entities and organization entities user has access to.
         * Use for privilege-based data access across all organizations.
         */
        USER(true, true,false), // all entities (GLOBAL and ORGANIZATION) where user has privileges
        
        /**
         * Returns entities in current organization where user has required privileges.
         * Combines organization filtering with privilege checking.
         * Most restrictive scope: organizationId == current organization AND user has privilege.
         * Use for tenant-aware privilege-based operations (most common scenario).
         */
        USER_IN_ORGANIZATION(true, true, true); // orgId == current organization and where user has privileges


        /**
         * Constructs SecurityScope with specified security check flags.
         *
         * @param requiresUser if true, authenticated user required
         * @param checkUserPrivileges if true, applies privilege filtering
         * @param currentOrganization if true, filters by current organization from tenant context
         */
        SecurityScope(boolean requiresUser ,boolean checkUserPrivileges, boolean currentOrganization) {
            this.checkUserPrivileges = checkUserPrivileges;
            this.currentOrganization = currentOrganization;
        }

        private boolean requiresUser;
        private boolean checkUserPrivileges;
        private boolean currentOrganization;


    }

    /**
     * JPA Criteria API secure predicate builder methods.
     * These methods construct privilege-filtered predicates for repository queries.
     */
    
    /**
     * Builds secure JPA Criteria predicate combining specification with privilege-based filtering.
     * This method is the primary entry point for SecureRepository query construction.
     * <p>
     * Processing flow:
     * </p>
     * <ol>
     *   <li>Apply base specification predicate</li>
     *   <li>Filter by SecurityScope (ALL, GLOBAL, ORGANIZATION, USER, USER_IN_ORGANIZATION)</li>
     *   <li>Apply external requiredPrivilege check if provided</li>
     *   <li>Apply entity-based requiredReadPrivilege field check if entity implements EntityWithRequiredPrivilege</li>
     * </ol>
     *
     * @param <T> entity type
     * @param specification optional JPA Specification for base filtering (null-safe)
     * @param requiredPrivilege external privilege requirement (null if not required)
     * @param root JPA Criteria Root for entity
     * @param query JPA CriteriaQuery being built
     * @param cb JPA CriteriaBuilder for predicate construction
     * @param scope SecurityScope defining filtering level (ALL, GLOBAL, ORGANIZATION, USER, USER_IN_ORGANIZATION)
     * @return JPA Predicate combining all security filters
     */
    default <T> Predicate toSecurePredicate(Specification<T> specification, Enum requiredPrivilege, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb, SecurityScope scope) {
        debug("[toPredicate] for Entity:{} ", root.getModel().getName());

        Optional<OrganizationUser> optionalUser = UserProvider.getFromContext();
        Class<?> rootJavaType = root.getModel().getJavaType();

        Predicate search = specification == null ? cb.conjunction() : specification.toPredicate(root, query, cb);

        //security scope is ALL, returning all by predicate
       if (scope == SecurityScope.ALL) {
            return search;
        }

        //all security scopes except ALL require user
        if(!optionalUser.isPresent()) {
            debug("[toPredicate] No user");
            search = cb.disjunction();
        }

        boolean isOrganizationEntity = isOrganizationRelated(rootJavaType);
        if (scope == SecurityScope.GLOBAL) {
            return isOrganizationEntity ? cb.and(search, cb.isNull(root.get("organizationId"))) : search;
        }

        if (scope.currentOrganization) {
            if(isOrganizationEntity){
                Long currentOrganizationId = TenantResolver.getTenantedResource().organizationId;
                if(currentOrganizationId != null){
                    search = cb.and(search, cb.equal(root.get("organizationId"), currentOrganizationId));
                } else {
                    search = cb.and(search, cb.isNull(root.get("organizationId")));
                }
            }
            if (scope == SecurityScope.ORGANIZATION) {
                return search;
            }
        }

        if(continueBuildingSearchPredicate(search)) {
            //the code below provide basic security for search
            boolean isEntityWithRequiredPrivilege = requiresPrivilege(rootJavaType);
            boolean isExternalRequiredPrivilege = requiredPrivilege != null;
            boolean hasManyOrganizationsEntity = isManyOrganizationRelated(rootJavaType);
            OrganizationUser user = optionalUser.get();

            Path orgId = isOrganizationEntity ? root.get("organizationId") : null;
            Path orgsId = hasManyOrganizationsEntity ? root.get("organizationIds") : null;
            Path privilege = isEntityWithRequiredPrivilege ? root.get("requiredReadPrivilege") : null;

            debug("[toPredicate] isEntityWithRequiredPrivilege:{} isExternalRequiredPrivilege:{} isOrganizationEntity:{} hasManyOrganizationsEntity:{}",
                    isEntityWithRequiredPrivilege, isExternalRequiredPrivilege, isOrganizationEntity, hasManyOrganizationsEntity);

            if (isExternalRequiredPrivilege) {
                search = getSearchPredicateForExternallyProvidedPrivilege(cb, search, isOrganizationEntity, hasManyOrganizationsEntity, user, orgId, orgsId, requiredPrivilege);
            }

            if (isEntityWithRequiredPrivilege && continueBuildingSearchPredicate(search)) {
                search = getSearchPredicateForEntityBasedPrivilege(cb, search, isOrganizationEntity, hasManyOrganizationsEntity, user, orgId, orgsId, privilege);
            }
        }


        return search;
    }

    /**
     * Repository predicate builder methods for entity-based and external privilege enforcement.
     * Called by toSecurePredicate to construct specific privilege checks.
     */
    
    /**
     * Builds JPA predicate for entity-based privilege checking using entity's requiredReadPrivilege field.
     * Supports three entity types:
     * <ul>
     *   <li>OrganizationRelatedEntity: Checks global privilege OR organization-specific privilege (orgId+privilege pair)</li>
     *   <li>IsManyOrganizationsRelatedEntity: Uses PostgreSQL arrays_overlap function to check multiple organizations</li>
     *   <li>Global entities: Checks only global privilege</li>
     * </ul>
     * Returns true if requiredReadPrivilege is null (no restriction) OR user has required privilege.
     *
     * @param cb JPA CriteriaBuilder for predicate construction
     * @param search existing predicate to combine with privilege check
     * @param isOrganizationEntity true if entity implements OrganizationRelatedEntity
     * @param hasManyOrganizationsEntity true if entity implements IsManyOrganizationsRelatedEntity
     * @param user current OrganizationUser with privilege sets
     * @param organizationIdPath JPA Path to entity's organizationId field (null if not organization-related)
     * @param organizationIdsPath JPA Path to entity's organizationIds array field (null if not many-organizations-related)
     * @param requiredPrivilegePath JPA Path to entity's requiredReadPrivilege field
     * @return combined Predicate with entity-based privilege filtering
     */
    default Predicate getSearchPredicateForEntityBasedPrivilege(CriteriaBuilder cb, Predicate search, boolean isOrganizationEntity, boolean hasManyOrganizationsEntity, OrganizationUser user, Expression organizationIdPath, Expression organizationIdsPath, Path requiredPrivilegePath) {

        Predicate entityWithRequiredPrivilegeCheck;
        if (isOrganizationEntity) {

            // check global privilege
            Predicate globalEntityCheck = cb.and(
                    requiredPrivilegePath.isNotNull(),
                    requiredPrivilegePath.in(user.getGlobalPrivileges()));

            // check check org level privilege
            Expression orgPrivilegePair = cb.concat(organizationIdPath, requiredPrivilegePath);
            Predicate organizationEntityCheck = cb.and(
                    organizationIdPath.isNotNull(),
                    requiredPrivilegePath.isNotNull(),
                    orgPrivilegePair.in(user.getOrganizationWithPrivilegePairs()));

            //check passes if has either global or org-level privilege
            entityWithRequiredPrivilegeCheck = cb.or(globalEntityCheck, organizationEntityCheck);
            debug("[getSearchPredicateForExternallyProvidedPrivilege] (entityPrivilege != null AND entityPrivilege in {}) OR " +
                    "(orgId != null AND entityPrivilege != null AND orgId+entityPrivilege in {})", user.getGlobalPrivileges(), user.getOrganizationWithPrivilegePairs());

        } else if (hasManyOrganizationsEntity) {
            // check global privilege
            Predicate globalEntityCheck = cb.and(
                    requiredPrivilegePath.isNotNull(),
                    requiredPrivilegePath.in(user.getGlobalPrivileges()));

            // check org level privilege
            Expression orgsWithPrivilege = cb.function("arrays_suffix", Array.class, organizationIdsPath, requiredPrivilegePath);
            Predicate inAnyOrgCheck = cb.isTrue(cb.function("arrays_overlap", Boolean.class, orgsWithPrivilege, cb.literal(user.getOrganizationWithPrivilegePairs())));
            Predicate organizationEntityCheck = cb.and(organizationIdsPath.isNotNull(), inAnyOrgCheck);

            //check passes if has either global or org-level privilege
            entityWithRequiredPrivilegeCheck = cb.or(globalEntityCheck, organizationEntityCheck);


        } else {
            entityWithRequiredPrivilegeCheck = cb.and(
                    requiredPrivilegePath.isNotNull(),
                    requiredPrivilegePath.in(user.getGlobalPrivileges()));
        }

        search = cb.and(cb.or(requiredPrivilegePath.isNull(), entityWithRequiredPrivilegeCheck), search);
        return search;

    }

    /**
     * Builds JPA predicate for external (method-parameter) privilege requirement.
     * This method handles privilege checks when requiredPrivilege is passed as method parameter
     * rather than stored in entity field.
     * <p>
     * Logic:
     * </p>
     * <ul>
     *   <li>If user has global privilege: returns original search unchanged (all entities accessible)</li>
     *   <li>If user lacks global privilege:
     *     <ul>
     *       <li>OrganizationRelatedEntity: Filters to organizations where user has privilege</li>
     *       <li>IsManyOrganizationsRelatedEntity: Filters using arrays_overlap to match any organization</li>
     *       <li>Global entity: Returns disjunction (no results) since user lacks global privilege</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param cb JPA CriteriaBuilder for predicate construction
     * @param search existing predicate to combine with privilege check
     * @param isOrganizationEntity true if entity implements OrganizationRelatedEntity
     * @param hasManyOrganizationsEntity true if entity implements IsManyOrganizationsRelatedEntity
     * @param user current OrganizationUser with privilege sets
     * @param organizationIdPath JPA Path to entity's organizationId field (null if not organization-related)
     * @param organizationIdsPath JPA Path to entity's organizationIds array field (null if not many-organizations-related)
     * @param requiredPrivilege external privilege enum requirement
     * @return combined Predicate with external privilege filtering
     */
    default Predicate getSearchPredicateForExternallyProvidedPrivilege(CriteriaBuilder cb, Predicate search, boolean isOrganizationEntity, boolean hasManyOrganizationsEntity, OrganizationUser user,
                           Expression organizationIdPath, Expression organizationIdsPath, Enum requiredPrivilege) {

        boolean hasGlobalPrivilegeForExternalRequiredPrivilege;
        hasGlobalPrivilegeForExternalRequiredPrivilege = user.hasGlobalPrivilege(requiredPrivilege.name());

        // from here user has NOT the global privilege
        if (not(hasGlobalPrivilegeForExternalRequiredPrivilege)) {
            debug("[getSearchPredicateForExternallyProvidedPrivilege] user has NOT the global privilege");
            Set<Long> orgs = user.getOrganizationIdsWithPrivilege(requiredPrivilege.name());
            if (isOrganizationEntity) {
                //user has NOT the global privilege and the entity is organization related
                // check if user has org privilege
                search = cb.and(
                        organizationIdPath.isNotNull(),
                        organizationIdPath.in(orgs),
                        search);
                debug("[getSearchPredicateForExternallyProvidedPrivilege] org != null AND org id in {}", orgs);
            } else if (hasManyOrganizationsEntity) {
                //user has NOT the global privilege and the entity is assigned to many organizations
                // check if user has org privilege in any of related organizations
                Predicate inAnyOrgCheck = cb.isTrue(cb.function("arrays_overlap", Boolean.class, organizationIdsPath, cb.literal(orgs)));
                search = cb.and(
                        organizationIdsPath.isNotNull(),
                        inAnyOrgCheck,
                        search);
                debug("[getSearchPredicateForExternallyProvidedPrivilege] org != null AND any org id in {}", orgs);
            } else {
                // the entity is global, and user has no global privilege, return empty result
                debug("[getSearchPredicateForExternallyProvidedPrivilege] empty result");
                search = cb.disjunction();
            }

        }
        return search;

    }

    /**
     * Hook for form consistency validation (currently no-op).
     * Can be overridden to add custom validation logic for organization-related entity forms.
     *
     * @param f form instance to validate
     */
    default void assertFormConsistency(AbstractOrganizationRelatedEntityForm f) {

    }

}
