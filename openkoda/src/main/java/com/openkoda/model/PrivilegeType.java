package com.openkoda.model;

/**
 * Coarse-grained privilege classification enum categorizing permissions by scope and administrative level.
 * <p>
 * Provides high-level privilege categorization used for privilege filtering, authorization logic branching,
 * and scope-based permission checks. Distinguishes between user-specific actions (USER), organization-scoped
 * administrative operations (ORG_ADMIN), platform-wide administrative functions (ADMIN), and miscellaneous
 * privileges (OTHER).
 * </p>
 * <p>
 * This enum complements functional categorization systems by organizing privileges according to their
 * scope and multi-tenancy implications. Used throughout the authorization framework to determine
 * the appropriate context for privilege evaluation.
 * </p>
 * <p>
 * Design note: While PrivilegeGroup organizes by feature area, PrivilegeType categorizes by access
 * scope and administrative hierarchy, enabling efficient privilege filtering and authorization decisions.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Privilege for individual privilege definitions
 * @see PrivilegeGroup for functional categorization
 * @see Role for role-based privilege assignments
 */
public enum PrivilegeType {
    
    /**
     * User-specific privileges for personal account operations.
     * <p>
     * Privileges scoped to individual user context, covering personal account management,
     * profile operations, and user-specific actions. Examples include password recovery,
     * account verification, and reading personal user data.
     * </p>
     * <p>
     * Authorization checks for USER privileges evaluate against the authenticated user's
     * identity and do not require organization or global administrative permissions.
     * </p>
     */
    USER,
    
    /**
     * Organization administrator privileges for tenant-scoped operations.
     * <p>
     * Privileges bound to specific organization context, enabling administrative functions
     * within a single tenant. Examples include managing organization data, configuring
     * organization settings, and administering organization-specific resources.
     * </p>
     * <p>
     * Authorization checks for ORG_ADMIN privileges require organization ID in the context
     * and validate that the user has administrative rights within that specific tenant.
     * Multi-tenancy isolation is enforced at this privilege level.
     * </p>
     */
    ORG_ADMIN,
    
    /**
     * Global administrator privileges for system-wide operations.
     * <p>
     * Platform-level privileges not tied to any specific organization, enabling system-wide
     * administrative functions. Examples include accessing global settings, managing system
     * configuration, reading support data, and performing cross-tenant operations.
     * </p>
     * <p>
     * Authorization checks for ADMIN privileges apply across all tenants and require
     * elevated system-level permissions. These privileges should be restricted to
     * platform administrators and support personnel.
     * </p>
     */
    ADMIN,
    
    /**
     * Miscellaneous privileges not fitting standard classification.
     * <p>
     * Catch-all category for privileges that do not clearly belong to USER, ORG_ADMIN,
     * or ADMIN classifications. Used for specialized permissions or transitional privilege
     * definitions during system evolution.
     * </p>
     * <p>
     * Authorization context and scope for OTHER privileges are determined on a case-by-case
     * basis by the specific privilege definition.
     * </p>
     */
    OTHER 
}
