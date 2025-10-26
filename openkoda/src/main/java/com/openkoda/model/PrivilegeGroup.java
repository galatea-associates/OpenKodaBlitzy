package com.openkoda.model;

/**
 * Enumeration of privilege categories for UI organization and functional grouping in RBAC administration interface.
 * <p>
 * Groups related privileges into logical categories for presentation in role management screens. Each group has a 
 * display label shown in privilege selection UI. Enables hierarchical privilege organization and simplifies privilege 
 * discovery for administrators. Used by Privilege enum and DynamicPrivilege to categorize individual permissions.
 * </p>
 * <p>
 * Groups appear as sections or tabs in role configuration interface, with privileges listed under their respective groups.
 * This categorization improves usability when managing complex role definitions with dozens of individual privileges.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Privilege
 * @see PrivilegeBase
 */
public enum PrivilegeGroup implements OptionWithLabel {
    /** System-wide administrative settings and platform configuration. */
    GLOBAL_SETTINGS("Global Settings"),
    /** Tenant data management and organizational operations. */
    ORGANIZATION("Organization"),
    /** User profile and account management. */
    USER("User"),
    /** User role assignment and authorization configuration. */
    USER_ROLE("User Role"),
    /** System logs, diagnostics, and support tools. */
    SUPPORT("Support"),
    /** Audit trails and historical data access. */
    HISTORY("History"),
    /** UI resources and frontend component management. */
    FRONTEND_RESOURCE("Frontend Resource"),
    /** Authentication token operations. */
    TOKEN("Token"),
    /** Backend configuration including roles, server JS, schedulers, event listeners. */
    BACKEND("Backend")
    ;

    /** Human-readable display label shown in UI privilege management screens. */
    String label;

    PrivilegeGroup(String label) {
        this.label = label;
    }

    /**
     * Returns display label for UI presentation.
     *
     * @return human-readable label for privilege group
     */
    public String getLabel() {
        return label;
    }
}
