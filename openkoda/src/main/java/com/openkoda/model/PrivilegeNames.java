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

package com.openkoda.model;

/**
 * String constant definitions for all system privilege names used in @Formula annotations,
 * privilege checks, and privilege name validation.
 * <p>
 * Centralizes privilege name strings as constants to ensure consistency across @Formula-based
 * computed fields, privilege serialization, and authorization checks. Each constant matches the
 * name() of corresponding Privilege enum constant with underscore prefix convention (e.g.,
 * _readOrgData matches Privilege.readOrgData). Used by Privilege enum checkName() validation
 * to prevent enum-constant naming mismatches. Constants referenced in @Formula annotations
 * for EntityWithRequiredPrivilege implementations to define access control requirements at
 * entity level.

 * <p>
 * <b>Naming Convention:</b> All constants prefixed with underscore (_) and match Privilege enum
 * constant names. Example: '_canAccessGlobalSettings' for Privilege.canAccessGlobalSettings

 * <p>
 * <b>Usage Example:</b>
 * <pre>
 * &#64;Formula("( '" + PrivilegeNames._readOrgData + "' )")
 * private String requiredReadPrivilege;
 * </pre>
 * Used in PrivilegeHelper string serialization/deserialization.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Privilege for privilege enum consuming these constants
 * @see PrivilegeBase for checkName() validation method
 */
public interface PrivilegeNames {

    /**
     * String constant for isUser privilege name.
     * Base privilege indicating authenticated user status, automatically granted to all logged-in users.
     * Used in @Formula privilege requirements.
     */
    public static final String _isUser = "isUser";

    // ORGANIZATION DATA
    
    /**
     * String constant for readOrgData privilege name.
     * Allows reading organization-scoped data including tenant information and configuration.
     * Used in @Formula privilege requirements for entity-level read access control.
     */
    public static final String _readOrgData = "readOrgData";
    
    /**
     * String constant for manageOrgData privilege name.
     * Enables full management of organization data including creation, updates, and deletion.
     * Used in @Formula privilege requirements for entity-level write access control.
     */
    public static final String _manageOrgData = "manageOrgData";
    
    /**
     * String constant for canUseReportingAI privilege name.
     * Grants access to AI-powered reporting and analytics features.
     * Used in @Formula privilege requirements for AI reporting functionality.
     */
    public static final String _useReportingAI = "canUseReportingAI";
    
    /**
     * String constant for canCreateReports privilege name.
     * Permits creation and configuration of custom reports within organization scope.
     * Used in @Formula privilege requirements for report creation access.
     */
    public static final String _createReports = "canCreateReports";
    
    /**
     * String constant for canReadReports privilege name.
     * Allows viewing and accessing reports within organization scope.
     * Used in @Formula privilege requirements for report read access.
     */
    public static final String _readReports = "canReadReports";

    // ADMIN
    
    /**
     * String constant for canAccessGlobalSettings privilege name.
     * Grants access to system-wide administrative settings and platform configuration.
     * Required for platform administration tasks.
     * Used in @Formula privilege requirements for global settings access.
     */
    public static final String _canAccessGlobalSettings = "canAccessGlobalSettings";
    
    /**
     * String constant for canImpersonate privilege name.
     * Permits user impersonation for support and debugging scenarios.
     * High-security privilege requiring careful assignment.
     * Used in @Formula privilege requirements for impersonation functionality.
     */
    public static final String _canImpersonate = "canImpersonate";
    
    /**
     * String constant for canChangeEntityOrganization privilege name.
     * Allows reassignment of entities between different organizations.
     * Used for data migration and organizational restructuring.
     * Used in @Formula privilege requirements for entity organization changes.
     */
    public static final String _canChangeEntityOrganization = "canChangeEntityOrganization";
    
    /**
     * String constant for canImportData privilege name.
     * Enables bulk data import operations including CSV and other formats.
     * Used in @Formula privilege requirements for data import functionality.
     */
    public static final String _canImportData = "canImportData";
    
    /**
     * String constant for canSeeUserEmail privilege name.
     * Grants access to view user email addresses in the system.
     * Used for privacy-controlled user information access.
     * Used in @Formula privilege requirements for user email visibility.
     */
    public static final String _canSeeUserEmail = "canSeeUserEmail";
    
    /**
     * String constant for canResetPassword privilege name.
     * Permits administrative password reset operations for user accounts.
     * Used in @Formula privilege requirements for password management.
     */
    public static final String _canResetPassword = "canResetPassword";

    // USER DATA
    
    /**
     * String constant for readUserData privilege name.
     * Grants access to read user profile data within organization scope.
     * Used in @Formula privilege requirements for user data read access.
     */
    public static final String _readUserData = "readUserData";
    
    /**
     * String constant for manageUserData privilege name.
     * Enables full management of user profile data including creation, updates, and deletion.
     * Used in @Formula privilege requirements for user data write access.
     */
    public static final String _manageUserData = "manageUserData";

    /**
     * String constant for readUserRole privilege name.
     * Allows viewing user role assignments within organization context.
     * Used in @Formula privilege requirements for user role read access.
     */
    public static final String _readUserRole = "readUserRole";
    
    /**
     * String constant for manageUserRoles privilege name.
     * Enables assignment and modification of user roles within organization scope.
     * Used in @Formula privilege requirements for user role management.
     */
    public static final String _manageUserRoles = "manageUserRoles";

    // PAYMENTS
    
    /**
     * String constant for administratePayments privilege name.
     * Grants access to payment administration and financial transaction management.
     * Used in @Formula privilege requirements for payment operations.
     */
    public static final String _administratePayments = "administratePayments";

    // AUDIT
    
    /**
     * String constant for readOrgAudit privilege name.
     * Allows viewing audit trails and historical data for organization-scoped entities.
     * Used in @Formula privilege requirements for audit log access.
     */
    public static final String _readOrgAudit = "readOrgAudit";

    // FRONTEND RESOURCE
    
    /**
     * String constant for readFrontendResource privilege name.
     * Grants access to view frontend UI resources and components.
     * Used in @Formula privilege requirements for frontend resource read access.
     */
    public static final String _readFrontendResource = "readFrontendResource";
    
    /**
     * String constant for manageFrontendResource privilege name.
     * Enables creation and modification of frontend UI resources and components.
     * Used in @Formula privilege requirements for frontend resource management.
     */
    public static final String _manageFrontendResource = "manageFrontendResource";

    // ATTRIBUTES
    
    /**
     * String constant for canSeeAttributes privilege name.
     * Grants access to view entity attributes and custom fields.
     * Used in @Formula privilege requirements for attribute visibility.
     */
    public static final String _canSeeAttributes = "canSeeAttributes";
    
    /**
     * String constant for canEditAttributes privilege name.
     * Enables modification of entity attributes and custom field values.
     * Used in @Formula privilege requirements for general attribute editing.
     */
    public static final String _canEditAttributes = "canEditAttributes";
    
    /**
     * String constant for canEditUserAttributes privilege name.
     * Permits editing of user-specific custom attributes.
     * Used in @Formula privilege requirements for user attribute management.
     */
    public static final String _canEditUserAttributes = "canEditUserAttributes";
    
    /**
     * String constant for canEditOrgAttributes privilege name.
     * Allows modification of organization-specific custom attributes.
     * Used in @Formula privilege requirements for organization attribute management.
     */
    public static final String _canEditOrgAttributes = "canEditOrgAttributes";
    
    /**
     * String constant for canDefineAttributes privilege name.
     * Enables creation and configuration of custom attribute definitions.
     * Used in @Formula privilege requirements for attribute schema management.
     */
    public static final String _canDefineAttributes = "canDefineAttributes";

    // AUTHENTICATION AND TOKENS
    
    /**
     * String constant for canRecoverPassword privilege name.
     * Grants access to password recovery functionality.
     * Internal privilege typically assigned via temporary tokens.
     * Used in @Formula privilege requirements for password reset flows.
     */
    public static final String _canRecoverPassword = "canRecoverPassword";
    
    /**
     * String constant for canVerifyAccount privilege name.
     * Permits email verification and account activation operations.
     * Internal privilege typically assigned via temporary tokens.
     * Used in @Formula privilege requirements for account verification.
     */
    public static final String _canVerifyAccount = "canVerifyAccount";
    
    /**
     * String constant for canRefreshTokens privilege name.
     * Allows refreshing authentication tokens for extended sessions.
     * Used in @Formula privilege requirements for token management.
     */
    public static final String _canRefreshTokens = "canRefreshTokens";
    
    // BACKEND MANAGEMENT
    
    /**
     * String constant for canManageBackend privilege name.
     * Enables full backend configuration management including roles, server JS, schedulers, and event listeners.
     * High-level administrative privilege for backend system configuration.
     * Used in @Formula privilege requirements for backend write access.
     */
    public static final String _canManageBackend = "canManageBackend";
    
    /**
     * String constant for canReadBackend privilege name.
     * Grants access to view backend configuration including roles and system settings.
     * Used in @Formula privilege requirements for backend read access.
     */
    public static final String _canReadBackend = "canReadBackend";

    // SUPPORT AND DIAGNOSTICS
    
    /**
     * String constant for canReadSupportData privilege name.
     * Allows viewing system logs, diagnostics, and support information.
     * Used in @Formula privilege requirements for support data read access.
     */
    public static final String _canReadSupportData = "canReadSupportData";
    
    /**
     * String constant for canManageSupportData privilege name.
     * Enables management of support-related data and diagnostic tools.
     * Used in @Formula privilege requirements for support data management.
     */
    public static final String _canManageSupportData = "canManageSupportData";

}
