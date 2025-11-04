package com.openkoda.model.common;

import com.openkoda.model.OpenkodaModule;
import jakarta.persistence.*;

import static com.openkoda.core.lifecycle.BaseDatabaseInitializer.CORE_MODULE;

/**
 * Abstract JPA {@link MappedSuperclass} for module-scoped component entities within the OpenKoda platform.
 * <p>
 * ComponentEntity extends {@link OpenkodaEntity} and adds module association capabilities, enabling entities
 * to be organized and scoped to specific {@link OpenkodaModule} instances. This base class is designed for
 * entities representing platform components such as forms, workflows, UI components, frontend resources,
 * and other module-specific artifacts.

 * <p>
 * Inherits from OpenkodaEntity:
 * <ul>
 *   <li>Organization scope (organizationId) for multi-tenancy</li>
 *   <li>Automatic audit timestamps (createdOn, updatedOn, createdBy, modifiedBy)</li>
 *   <li>Properties Map for flexible key-value storage</li>
 *   <li>Search indexString for full-text search capabilities</li>
 *   <li>Computed referenceString for entity identification</li>
 * </ul>

 * <p>
 * JPA Inheritance Strategy: This class uses {@code @MappedSuperclass} with {@code TABLE_PER_CLASS}
 * inheritance (inherited from OpenkodaEntity). Each concrete subclass will have its own database table
 * containing all fields from the inheritance hierarchy.

 * <p>
 * Dual-Field Module Association Pattern: ComponentEntity maintains both a navigable {@code module}
 * relationship (lazy-loaded ManyToOne to OpenkodaModule) and a persisted {@code moduleName} column
 * storing the module identifier string. The {@code module} field is read-only (non-insertable,
 * non-updatable) to ensure the persisted {@code moduleName} remains the source of truth.

 * <p>
 * Usage Pattern:
 * <pre>
 * &#64;Entity
 * public class Form extends ComponentEntity {
 *     // Inherits moduleName, automatically defaults to CORE_MODULE
 *     // Can override to associate with custom modules
 * }
 * </pre>

 * <p>
 * Thread Safety: Entity instances are not thread-safe per JPA specification. Entities should not be
 * shared across threads without external synchronization.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see OpenkodaEntity
 * @see OpenkodaModule
 * @see com.openkoda.core.lifecycle.BaseDatabaseInitializer#CORE_MODULE
 */
@MappedSuperclass
public abstract class ComponentEntity extends OpenkodaEntity {

    /**
     * Lazy-loaded many-to-one relationship to the {@link OpenkodaModule} entity.
     * <p>
     * This field provides navigable access to the associated module entity but is marked as
     * non-insertable and non-updatable (read-only) to ensure the persisted {@link #moduleName}
     * column remains the authoritative source. The relationship is joined via the module's
     * {@code name} column, which serves as the natural key.

     * <p>
     * JPA Configuration:
     * <ul>
     *   <li>{@code @ManyToOne(optional = false)}: Association is required (cannot be null)</li>
     *   <li>{@code fetch = FetchType.LAZY}: Module is loaded on-demand to optimize performance</li>
     *   <li>{@code @JoinColumn}: References OpenkodaModule.name column</li>
     *   <li>{@code insertable = false, updatable = false}: Read-only navigation property</li>
     * </ul>

     *
     * @see OpenkodaModule
     * @see #moduleName
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "module", referencedColumnName = "name", insertable = false, updatable = false)
    protected OpenkodaModule module;

    /**
     * Persisted string column storing the module identifier (natural key).
     * <p>
     * This field holds the actual persisted module name in the database and defaults to
     * {@link com.openkoda.core.lifecycle.BaseDatabaseInitializer#CORE_MODULE}. The moduleName
     * serves as the foreign key value for the {@link #module} relationship and is the
     * authoritative source for the module association.

     * <p>
     * Column Configuration:
     * <ul>
     *   <li>{@code name = "module"}: Shares column name with the join column of {@link #module}</li>
     *   <li>{@code nullable = false}: Module association is required for all component entities</li>
     *   <li>Default value: {@code CORE_MODULE} (the platform's built-in module identifier)</li>
     * </ul>

     * <p>
     * Typical Values:
     * <ul>
     *   <li>{@code "CORE"}: Platform core module (default)</li>
     *   <li>Custom module names: e.g., {@code "CustomReporting"}, {@code "AdvancedWorkflow"}</li>
     * </ul>

     *
     * @see com.openkoda.core.lifecycle.BaseDatabaseInitializer#CORE_MODULE
     * @see OpenkodaModule
     * @see #module
     */
    @Column(name = "module", nullable = false)
    protected String moduleName = CORE_MODULE;
    
    /**
     * Constructs a ComponentEntity with the specified organization scope.
     * <p>
     * This constructor initializes the entity with the provided organization ID for multi-tenancy
     * isolation and sets the moduleName to {@link com.openkoda.core.lifecycle.BaseDatabaseInitializer#CORE_MODULE}
     * by default. The organization ID establishes the tenant scope for this component entity.

     *
     * @param organizationId the organization ID for tenant isolation, may be null for global entities
     * @see OpenkodaEntity#OpenkodaEntity(Long)
     */
    public ComponentEntity(Long organizationId) {
        super(organizationId);
    }

    /**
     * No-argument constructor required by JPA for entity instantiation.
     * <p>
     * This constructor creates a ComponentEntity with organizationId set to null and moduleName
     * defaulted to {@link com.openkoda.core.lifecycle.BaseDatabaseInitializer#CORE_MODULE}.
     * The organization ID should be set explicitly after instantiation for tenant-scoped entities.

     *
     * @see OpenkodaEntity#OpenkodaEntity(Long)
     */
    public ComponentEntity() {
        super(null);
    }

    /**
     * Returns the lazy-loaded {@link OpenkodaModule} entity associated with this component.
     * <p>
     * This method provides navigable access to the module entity. Note that accessing this field
     * may trigger a database query if the module has not been previously loaded (lazy fetch).
     * The returned module is the entity referenced by the {@link #moduleName} natural key.

     *
     * @return the associated OpenkodaModule entity, never null for persisted entities
     * @see OpenkodaModule
     * @see #moduleName
     */
    public OpenkodaModule getModule() {
        return module;
    }

    /**
     * Returns the persisted module identifier string.
     * <p>
     * This method returns the module name value stored in the database, which serves as the
     * natural key for the {@link #module} relationship. The module name identifies which
     * OpenKoda module this component belongs to.

     *
     * @return the module identifier string, typically {@link com.openkoda.core.lifecycle.BaseDatabaseInitializer#CORE_MODULE}
     *         or a custom module name, never null
     * @see #module
     * @see com.openkoda.core.lifecycle.BaseDatabaseInitializer#CORE_MODULE
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Sets the module identifier for this component entity.
     * <p>
     * This method updates the persisted moduleName column, which determines the module association
     * for this component. The module name must correspond to an existing {@link OpenkodaModule}
     * entity's name field.

     * <p>
     * Typical usage:
     * <pre>
     * componentEntity.setModuleName("CustomReportingModule");
     * </pre>

     *
     * @param moduleName the module identifier string, must not be null and should reference an existing module
     * @see #moduleName
     * @see OpenkodaModule
     */
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
}
