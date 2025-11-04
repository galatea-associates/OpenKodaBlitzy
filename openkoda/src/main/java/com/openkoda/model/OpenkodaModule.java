package com.openkoda.model;

import com.openkoda.model.common.ModelConstants;
import com.openkoda.model.common.OpenkodaEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;

/**
 * Entity representing application modules for modular feature organization with privilege-based access control.
 * <p>
 * Persisted to openkoda_module table. Represents logical application modules for feature grouping and access control.
 * Each module has unique name (primary identifier without public setter to enforce immutability after creation)
 * and {@link Formula @Formula}-derived privilege requirements. Used for modular architecture, feature enablement,
 * and module-level authorization. Enables plugin-style extensibility where modules can be enabled/disabled or
 * access-controlled via privileges.
 * 
 * <p>
 * Design: name field has no public setter - enforces module name immutability after entity creation.
 * Prevents accidental module renaming which would break module references.
 * 
 * <p>
 * {@link Formula @Formula} fields: requiredReadPrivilege and requiredWritePrivilege computed from
 * {@link PrivilegeNames} constants for module configuration access control.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see OpenkodaEntity
 * @see PrivilegeNames
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
public class OpenkodaModule extends OpenkodaEntity {

    /**
     * Unique module identifier. Must be unique across all modules.
     * <p>
     * No public setter - name is immutable after entity creation to prevent breaking module references.
     * Module name serves as the primary business identifier for feature grouping and module lookup.
     * 
     */
    @Column
    private String name;

    /**
     * {@link Formula @Formula}-derived privilege token required to modify module configuration.
     * <p>
     * Computed from {@link PrivilegeNames#_canAccessGlobalSettings} constant.
     * Enforces access control for module write operations.
     * 
     */
    @Formula("( '" + PrivilegeNames._canAccessGlobalSettings + "' )")
    private String requiredWritePrivilege;
    
    /**
     * {@link Formula @Formula}-derived privilege token required to view module configuration.
     * <p>
     * Computed from {@link PrivilegeNames#_canAccessGlobalSettings} constant.
     * Enforces access control for module read operations.
     * 
     */
    @Formula("( '" + PrivilegeNames._canAccessGlobalSettings + "' )")
    private String requiredReadPrivilege;

    /**
     * Constructs OpenkodaModule with organization scope.
     * <p>
     * Creates module associated with specific organization for multi-tenant scenarios.
     * 
     *
     * @param organizationId the organization ID to associate with this module, or null for global modules
     */
    public OpenkodaModule(Long organizationId) {
        super(organizationId);
    }

    /**
     * Constructs OpenkodaModule with default values.
     * <p>
     * Creates global module (not associated with specific organization).
     * Module name must be set separately after construction.
     * 
     */
    public OpenkodaModule() {
        super(null);
    }

    /**
     * Constructs OpenkodaModule with specified name as global module.
     * <p>
     * Creates global module (not associated with specific organization) with immutable name.
     * Name cannot be changed after construction due to lack of public setter.
     * 
     *
     * @param name the unique module identifier, must not be null
     */
    public OpenkodaModule(String name) {
        super(null);
        this.name = name;
    }

    /**
     * Returns module identifier name.
     * <p>
     * Immutable after creation - no public setter available.
     * Serves as primary business identifier for module lookup and feature grouping.
     * 
     *
     * @return the unique module name, or null if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Returns {@link Formula @Formula}-computed privilege token required to view module configuration.
     * <p>
     * Computed from {@link PrivilegeNames#_canAccessGlobalSettings} constant.
     * Used for privilege-based access control on module read operations.
     * 
     *
     * @return the privilege token string required for read access
     */
    @Override
    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    /**
     * Returns {@link Formula @Formula}-computed privilege token required to modify module configuration.
     * <p>
     * Computed from {@link PrivilegeNames#_canAccessGlobalSettings} constant.
     * Used for privilege-based access control on module write operations.
     * 
     *
     * @return the privilege token string required for write access
     */
    @Override
    public String getRequiredWritePrivilege() {
        return requiredWritePrivilege;
    }
}
