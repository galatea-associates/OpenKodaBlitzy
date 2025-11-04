package com.openkoda.service.export.dto;

import static com.openkoda.core.lifecycle.BaseDatabaseInitializer.CORE_MODULE;

/**
 * Base DTO for component exports providing shared module identifier and organization scope.
 * <p>
 * This class serves as the foundational mutable JavaBean POJO for all component export DTOs
 * within the OpenKoda export subsystem. It provides {@code module} and {@code organizationId}
 * fields that are inherited by all subclasses, enabling consistent module identification and
 * tenant-scoping across the export framework.
 * 
 * <p>
 * The {@code module} field defaults to the canonical {@link com.openkoda.core.lifecycle.BaseDatabaseInitializer#CORE_MODULE}
 * constant, ensuring repository-wide consistent core module identification. This default supports
 * the common case where components belong to the core OpenKoda module without requiring explicit
 * configuration.
 * 
 * <p>
 * This DTO is designed for YAML and JSON serialization during component export and import operations.
 * It contains no validation annotations or framework-specific annotations, maintaining its role as
 * a pure data transfer object. The class is not thread-safe and should not be shared across threads
 * without external synchronization.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.core.lifecycle.BaseDatabaseInitializer#CORE_MODULE
 */
public class ComponentDto {

    /**
     * Module identifier for the component.
     * <p>
     * Defaults to the {@link com.openkoda.core.lifecycle.BaseDatabaseInitializer#CORE_MODULE}
     * constant, providing consistent core module identification across all components.
     * This field can be explicitly set to a different module identifier for components
     * belonging to custom modules or extensions.
     * 
     * <p>
     * Nullable after explicit assignment via {@link #setModule(String)}.
     * 
     */
    private String module = CORE_MODULE;
    
    /**
     * Foreign key reference to the owning organization for tenant-scoped components.
     * <p>
     * This field establishes the tenant scope for components that belong to specific
     * organizations in OpenKoda's multi-tenancy architecture. When set, the component
     * is scoped to the identified organization. When null, the component is treated
     * as a global component accessible across all tenants.
     * 
     * <p>
     * Nullable for global components that are not organization-specific.
     * 
     */
    private Long organizationId;

    /**
     * Returns the module identifier for this component.
     * <p>
     * The returned value defaults to {@link com.openkoda.core.lifecycle.BaseDatabaseInitializer#CORE_MODULE}
     * if not explicitly set via {@link #setModule(String)}. This method returns the module field
     * directly without additional validation.
     * 
     * <p>
     * Note: The commented-out code on the following line shows a previous implementation that
     * included a StringUtils.isNotEmpty check before returning CORE_MODULE as a fallback.
     * This validation was removed to simplify the logic and rely on field initialization.
     * 
     *
     * @return the module identifier, defaults to CORE_MODULE if not explicitly set
     */
    public String getModule() {
//        return StringUtils.isNotEmpty(module) ? module : CORE_MODULE;
        return module;
    }

    /**
     * Sets the module identifier for this component.
     * <p>
     * Allows explicit configuration of the module identifier, overriding the default
     * {@link com.openkoda.core.lifecycle.BaseDatabaseInitializer#CORE_MODULE} value.
     * This is typically used when defining components for custom modules or extensions.
     * 
     *
     * @param module the module identifier to set, may be null
     */
    public void setModule(String module) {
        this.module = module;
    }

    /**
     * Returns the organization ID for tenant scope.
     * <p>
     * When this method returns a non-null value, the component is scoped to the specified
     * organization within OpenKoda's multi-tenancy architecture. When it returns null,
     * the component is considered global and accessible across all organizations.
     * 
     *
     * @return the organization ID for tenant scope, or null for global components
     */
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the organization ID for tenant scope.
     * <p>
     * Configures the tenant scope for this component. Setting a non-null organization ID
     * restricts the component to the specified organization in OpenKoda's multi-tenancy
     * architecture. Setting null designates the component as global, making it accessible
     * across all organizations.
     * 
     *
     * @param organizationId the organization ID to set, may be null for global components
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }
}
