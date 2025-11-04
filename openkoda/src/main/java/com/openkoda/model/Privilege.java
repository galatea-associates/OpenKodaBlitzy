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

import static com.openkoda.model.PrivilegeGroup.*;

/**
 * Canonical enumeration of all system privileges defining fine-grained access control permissions across OpenKoda platform.
 * <p>
 * Defines the complete privilege vocabulary for role-based access control (RBAC). Each enum constant represents a distinct 
 * permission with unique ID (multiplied by idOffset for privilege value range), PrivilegeGroup categorization for UI grouping, 
 * display label, and visibility flag. Implements PrivilegeBase interface for uniform privilege handling and PrivilegeNames for 
 * string constant validation. Privilege IDs are used as database foreign keys in privilege assignment tables. Startup validation 
 * via checkName() ensures enum constant names match PrivilegeNames string constants to prevent configuration drift.
 * 
 * <p>
 * Privilege categories: Organized into PrivilegeGroups: GLOBAL_SETTINGS (system-wide), ORGANIZATION (tenant data), 
 * USER/USER_ROLE (user management), SUPPORT (logs/diagnostics), FRONTEND_RESOURCE (UI resources), TOKEN (authentication), 
 * BACKEND (configuration), HISTORY (audit trails).
 * 
 * <p>
 * ID allocation: Each privilege ID = enumOrdinalId * idOffset (1), creating unique database values. 
 * Example: isUser (id=1), canAccessGlobalSettings (id=2)
 * 
 * <p>
 * Hidden privileges: Some privileges marked hidden=true (e.g., canRecoverPassword, canVerifyAccount) are internal-only 
 * and not exposed in UI privilege management screens.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see PrivilegeBase
 * @see PrivilegeGroup
 * @see PrivilegeNames
 * @see Role
 * @see com.openkoda.core.helper.PrivilegeHelper
 */
public enum Privilege implements PrivilegeBase, PrivilegeNames {

    /** Base privilege indicating authenticated user status. Automatically granted to all logged-in users. Part of GLOBAL_SETTINGS group. */
    isUser(GLOBAL_SETTINGS, 1, "Is User", _isUser),
    
    /** Grants access to system-wide administrative settings. Required for platform administration. Part of GLOBAL_SETTINGS group. */
    canAccessGlobalSettings(GLOBAL_SETTINGS, 2, "Access", _canAccessGlobalSettings),

    /** Allows reading organization-scoped data. Required for viewing tenant information and configuration. Part of ORGANIZATION group. */
    readOrgData(ORGANIZATION, 3, "Read", _readOrgData),
    
    /** Enables full management of organization data including creation, updates, and deletion. Part of ORGANIZATION group. */
    manageOrgData(ORGANIZATION, 4, "Manage", _manageOrgData),
    
    /** Permits changing entity ownership between organizations. High-security privilege for multi-tenant operations. Part of ORGANIZATION group. */
    canChangeEntityOrganization(ORGANIZATION, 5, "Change Entity", _canChangeEntityOrganization),
    
    /** Enables AI-powered reporting features for organization. Part of ORGANIZATION group. */
    canUseReportingAI(ORGANIZATION, 6, "Use Reporting AI", _useReportingAI),
    
    /** Allows creation of new reports within organization. Part of ORGANIZATION group. */
    canCreateReports(ORGANIZATION, 7, "Create Reports", _createReports),
    
    /** Grants access to read existing reports within organization. Part of ORGANIZATION group. */
    canReadReports(ORGANIZATION, 8, "Read Reports", _readReports),

    /** Hidden privilege for password recovery flow. Internal-only, not exposed in UI. Part of USER group. */
    canRecoverPassword(USER, 9, "Recover Password", _canRecoverPassword, true),
    
    /** Allows viewing user email addresses. Required for user management and communication features. Part of USER group. */
    canSeeUserEmail(USER, 10, "See Email", _canSeeUserEmail),

    /** Permits user impersonation for support and debugging. High-security privilege requiring careful assignment. Part of USER group. */
    canImpersonate(USER, 11, "Impersonate", _canImpersonate),

    /** Enables administrative password reset for other users. Part of USER group. */
    canResetPassword(USER, 12, "Reset Passwords", _canResetPassword),
    
    /** Hidden privilege for account verification flow. Internal-only, not exposed in UI. Part of USER group. */
    canVerifyAccount(USER, 13, "Verify Password", _canVerifyAccount, true),

    /** Grants access to read user profile data within organization scope. Part of USER group. */
    readUserData(USER, 14, "Read Profiles", _readUserData),
    
    /** Enables modification of user profile data within organization. Part of USER group. */
    manageUserData(USER, 15, "Manage Profiles Data", _manageUserData),

    /** Allows reading user role assignments within organization. Part of USER_ROLE group. */
    readUserRole(USER_ROLE, 16, "Read", _readUserRole),
    
    /** Enables assignment and modification of user roles within organization. Part of USER_ROLE group. */
    manageUserRoles(USER_ROLE, 17, "Manage", _manageUserRoles),

    /** Grants access to read system logs, diagnostics, and support data. Part of SUPPORT group. */
    canReadSupportData(SUPPORT, 18, "Read", _canReadSupportData),
    
    /** Enables management of support data and diagnostic tools. Part of SUPPORT group. */
    canManageSupportData(SUPPORT, 19, "Manage", _canManageSupportData),

    /** Allows reading organization audit history. Part of HISTORY group. */
    readOrgAudit(HISTORY, 20, "Read Organization History", _readOrgAudit),

    /** Grants access to read frontend resources and UI components. Part of FRONTEND_RESOURCE group. */
    readFrontendResource(FRONTEND_RESOURCE, 21, "Read", _readFrontendResource),
    
    /** Enables management of frontend resources and UI component configuration. Part of FRONTEND_RESOURCE group. */
    manageFrontendResource(FRONTEND_RESOURCE, 22, "Manage", _manageFrontendResource),

    /** Permits refresh of authentication tokens. Part of TOKEN group. */
    canRefreshTokens(TOKEN, 23, "Refresh", _canRefreshTokens),

    /** Grants read access to backend configuration including roles, server JS, schedulers, and event listeners. Part of BACKEND group. */
    canReadBackend(BACKEND, 24, "Read", _canReadBackend),
    
    /** Enables management of backend configuration including roles, server JS, schedulers, and event listeners. Part of BACKEND group. */
    canManageBackend(BACKEND, 25, "Manage", _canManageBackend),
    
    /** Allows importing data into organization. Enables bulk data operations and CSV import features. Part of ORGANIZATION group. */
    canImportData(ORGANIZATION, 26, "Import Data", _canImportData)
    ;

    /** Database privilege ID value, calculated as enumOrdinalId * idOffset. Used as foreign key in privilege assignment tables and for privilege lookups. */
    private Long id;
    
    /** Privilege category for additional grouping. Defaults to "General". */
    private String category = "General";
    
    /** PrivilegeGroup categorization for UI grouping and organization. Determines where privilege appears in management interface. */
    private PrivilegeGroup group;
    
    /** Human-readable display label for UI presentation in privilege selection screens. */
    private String label;
    
    /** If true, privilege is internal-only and hidden from UI privilege management screens. Used for system privileges not intended for manual assignment. */
    private boolean hidden;

    /**
     * Creates privilege with specified group, ID, label, and name check. Hidden flag defaults to false (visible in UI).
     *
     * @param group the PrivilegeGroup categorization for this privilege
     * @param id the unique privilege ID (will be multiplied by idOffset for database value)
     * @param label the human-readable display label
     * @param nameCheck the expected privilege name from PrivilegeNames for validation
     */
    Privilege(PrivilegeGroup group, long id, String label, String nameCheck) {
        this(group, id, label, nameCheck,false);
    }

    /**
     * Creates privilege with full configuration including hidden flag. Validates enum constant name matches nameCheck parameter 
     * via checkName() to ensure consistency with PrivilegeNames.
     *
     * @param group the PrivilegeGroup categorization for this privilege
     * @param id the unique privilege ID (will be multiplied by idOffset for database value)
     * @param label the human-readable display label
     * @param nameCheck the expected privilege name from PrivilegeNames for validation
     * @param hidden if true, privilege is internal-only and hidden from UI management screens
     */
    Privilege(PrivilegeGroup group, long id, String label, String nameCheck, boolean hidden) {
        this.id = id * this.idOffset();
        this.label = label;
        this.group = group;
        this.hidden = hidden;
        checkName(nameCheck);
    }

    /**
     * Returns the human-readable display label for UI presentation.
     *
     * @return the display label for privilege selection screens
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * Returns the PrivilegeGroup categorization for privilege organization.
     *
     * @return the privilege group determining UI grouping and organization
     */
    @Override
    public PrivilegeGroup getGroup() {
        return group;
    }

    /**
     * Returns the privilege category for additional grouping.
     *
     * @return the category string, defaults to "General"
     */
    @Override
    public String getCategory() {
        return category;
    }

    /**
     * Returns true if privilege is internal-only and should not appear in UI management screens.
     *
     * @return true if hidden from UI, false if visible
     */
    @Override
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Returns the database privilege ID value used for persistence and lookups.
     *
     * @return the privilege ID calculated as enumOrdinalId * idOffset
     */
    @Override
    public Long getId() {
        return this.id;
    }

    /**
     * Returns ID multiplier offset (1) for privilege ID calculation. Implements PrivilegeBase interface method.
     *
     * @return the offset value of 1
     */
    @Override
    public int idOffset() { return 1; }
}
