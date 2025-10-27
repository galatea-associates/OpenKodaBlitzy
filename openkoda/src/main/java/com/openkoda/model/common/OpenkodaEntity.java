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

package com.openkoda.model.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openkoda.model.Organization;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Formula;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Core {@code @MappedSuperclass} providing organization-scoped entity foundation for OpenKoda.
 * <p>
 * This abstract base class establishes the fundamental structure for all organization-related entities
 * in the system. It provides multi-tenancy through organization scoping, automatic audit trail tracking,
 * flexible key-value property storage, and full-text search capabilities.
 * </p>
 * <p>
 * The class uses JPA {@code @Inheritance} with {@code TABLE_PER_CLASS} strategy, meaning each concrete
 * entity subclass gets its own dedicated database table containing all inherited fields plus its own.
 * This approach provides clear table separation and simplifies querying individual entity types.
 * </p>
 * <p>
 * Spring Data JPA auditing is enabled via {@code @EntityListeners(AuditingEntityListener.class)},
 * automatically populating {@code @CreatedBy}, {@code @CreatedDate}, {@code @LastModifiedBy}, and
 * {@code @LastModifiedDate} fields on entity persistence and updates.
 * </p>
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 *   <li><b>Organization Scope:</b> Each entity is associated with an {@link Organization} for multi-tenant isolation</li>
 *   <li><b>Automatic Timestamps:</b> Created and updated timestamps are managed by Spring Data auditing</li>
 *   <li><b>Properties Map:</b> Flexible {@code Map<String, String>} for key-value storage without schema changes</li>
 *   <li><b>Search Index:</b> {@code indexString} field supports full-text search capabilities</li>
 *   <li><b>Reference String:</b> Computed {@code referenceString} field via {@code @Formula} for entity identification</li>
 *   <li><b>Privilege Control:</b> Integration with privilege system through {@link EntityWithRequiredPrivilege}</li>
 * </ul>
 * <p>
 * <b>Usage Example:</b>
 * </p>
 * <pre>
 * public class CustomEntity extends OpenkodaEntity {
 *     public CustomEntity(Long organizationId) { super(organizationId); }
 * }
 * </pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Organization
 * @see SearchableOrganizationRelatedEntity
 * @see AuditableEntityOrganizationRelated
 * @see EntityWithRequiredPrivilege
 * @see ModelConstants
 * @see AuditingEntityListener
 */
@MappedSuperclass
@Inheritance(
        strategy = InheritanceType.TABLE_PER_CLASS
)
@EntityListeners({AuditingEntityListener.class})
public abstract class OpenkodaEntity implements ModelConstants, Serializable, SearchableOrganizationRelatedEntity, AuditableEntityOrganizationRelated, EntityWithRequiredPrivilege {

    /**
     * Unique entity identifier generated via database sequence.
     * <p>
     * Uses {@link ModelConstants#ORGANIZATION_RELATED_ID_GENERATOR} sequence with allocation size of 10
     * for performance optimization. The sequence starts from {@link ModelConstants#INITIAL_ORGANIZATION_RELATED_VALUE}.
     * Batch allocation reduces database roundtrips during bulk entity creation.
     * </p>
     *
     * @see ModelConstants#ORGANIZATION_RELATED_ID_GENERATOR
     */
    @Id
    @SequenceGenerator(name = ORGANIZATION_RELATED_ID_GENERATOR, sequenceName = ORGANIZATION_RELATED_ID_GENERATOR, initialValue = ModelConstants.INITIAL_ORGANIZATION_RELATED_VALUE, allocationSize = 10)
    @GeneratedValue(generator = ORGANIZATION_RELATED_ID_GENERATOR, strategy = GenerationType.SEQUENCE)
    protected Long id;

    /**
     * Lazy-loaded navigation property to the associated {@link Organization} entity.
     * <p>
     * This field provides object-oriented access to the organization but is non-insertable and
     * non-updatable. The actual foreign key is managed by {@link #organizationId}.
     * Marked with {@code @JsonIgnore} to prevent circular references during JSON serialization.
     * Uses {@code FetchType.LAZY} to avoid unnecessary database queries.
     * </p>
     *
     * @see Organization
     * @see #organizationId
     */
    @JsonIgnore
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(nullable = true, insertable = false, updatable = false, name = ORGANIZATION_ID)
    protected Organization organization;

    /**
     * Foreign key to the {@link Organization} entity for multi-tenant scope.
     * <p>
     * This field establishes tenant isolation by associating each entity with a specific organization.
     * The value is set once during entity creation via constructor and is immutable thereafter
     * ({@code updatable = false}). Nullable to support global entities not tied to specific organizations.
     * </p>
     *
     * @see Organization
     * @see #setOrganizationId(Long)
     */
    @Column(nullable = true, name = ORGANIZATION_ID, updatable = false)
    protected Long organizationId;

    /**
     * Computed reference string for entity identification via database formula.
     * <p>
     * This field is not persisted but calculated dynamically using the SQL expression defined in
     * {@link ModelConstants#DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA}.
     * Provides a human-readable identifier combining entity type and key attributes.
     * Read-only and computed at query time by the database.
     * </p>
     *
     * @see ModelConstants#DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA
     */
    @Formula(DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA)
    protected String referenceString;

    /**
     * Full-text search index string for searchable entity content.
     * <p>
     * This field stores searchable text extracted from the entity for full-text search capabilities.
     * Database column defaults to empty string and is non-insertable, typically populated by triggers
     * or database functions. Maximum length defined by {@link ModelConstants#INDEX_STRING_COLUMN_LENGTH}.
     * </p>
     *
     * @see SearchableOrganizationRelatedEntity
     * @see ModelConstants#INDEX_STRING_COLUMN
     */
    @Column(name = INDEX_STRING_COLUMN, length = INDEX_STRING_COLUMN_LENGTH, insertable = false)
    @ColumnDefault("''")
    protected String indexString;

    /**
     * User identifier who created this entity, automatically populated by Spring Data auditing.
     * <p>
     * Uses {@code @CreatedBy} annotation to capture the current authenticated user's identity
     * at entity creation time. The value is a {@link TimestampedEntity.UID} embeddable containing
     * both username and user ID. Marked {@code @JsonIgnore} to exclude from JSON responses.
     * </p>
     *
     * @see TimestampedEntity.UID
     * @see AuditingEntityListener
     */
    @CreatedBy
    @JsonIgnore
    protected TimestampedEntity.UID createdBy;
    
    /**
     * Timestamp when this entity was created, automatically set by Spring Data auditing.
     * <p>
     * Uses {@code @CreatedDate} annotation to capture entity creation time. Database column
     * defaults to {@code CURRENT_TIMESTAMP} with timezone support. Non-insertable and non-updatable
     * to ensure immutability. Formatted as ISO 8601 date-time for JSON serialization.
     * </p>
     *
     * @see AuditingEntityListener
     */
    @CreatedDate
    @Column(
            name = "created_on",
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP",
            insertable = false,
            updatable = false
    )
    @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
    )
    protected LocalDateTime createdOn;
    
    /**
     * User identifier who last modified this entity, automatically updated by Spring Data auditing.
     * <p>
     * Uses {@code @LastModifiedBy} annotation to capture the current authenticated user's identity
     * on each entity update. Uses {@code @AttributeOverrides} to map the {@link TimestampedEntity.UID}
     * embeddable fields to {@code modifiedBy} and {@code modifiedById} columns.
     * Marked {@code @JsonIgnore} to exclude from JSON responses.
     * </p>
     *
     * @see TimestampedEntity.UID
     * @see AuditingEntityListener
     */
    @LastModifiedBy
    @AttributeOverrides({@AttributeOverride(
            name = "createdBy",
            column = @Column(
                    name = "modifiedBy"
            )
    ), @AttributeOverride(
            name = "createdById",
            column = @Column(
                    name = "modifiedById"
            )
    )})
    @JsonIgnore
    protected TimestampedEntity.UID modifiedBy;
    
    /**
     * Timestamp when this entity was last modified, automatically updated by Spring Data auditing.
     * <p>
     * Uses {@code @LastModifiedDate} annotation to capture the time of each entity update.
     * Database column defaults to {@code CURRENT_TIMESTAMP} with timezone support. Non-insertable
     * to ensure the database sets the initial value. Formatted as ISO 8601 date-time for JSON serialization.
     * </p>
     *
     * @see AuditingEntityListener
     */
    @LastModifiedDate
    @Column(
            name = "updated_on",
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP",
            insertable = false
    )
    @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
    )
    protected LocalDateTime updatedOn;


    /**
     * Returns a simple string representation of this entity for audit trail purposes.
     * <p>
     * Provides the entity's simple class name for logging and audit records.
     * Subclasses may override to include additional identifying information.
     * </p>
     *
     * @return the simple class name of this entity
     * @see AuditableEntityOrganizationRelated#toAuditString()
     */
    @Override
    public String toAuditString() {
        return this.getClass().getSimpleName();
    }

    /**
     * Returns the privilege required to read this entity instance.
     * <p>
     * Default implementation returns {@code null}, indicating no specific privilege is required.
     * Subclasses should override this method to enforce per-instance read access control
     * by returning the appropriate privilege name from {@code PrivilegeNames}.
     * </p>
     *
     * @return {@code null} in base implementation; privilege name in subclasses
     * @see EntityWithRequiredPrivilege#getRequiredReadPrivilege()
     */
    @Override
    public String getRequiredReadPrivilege() {
        return null;
    }

    /**
     * Returns the privilege required to modify this entity instance.
     * <p>
     * Default implementation returns {@code null}, indicating no specific privilege is required.
     * Subclasses should override this method to enforce per-instance write access control
     * by returning the appropriate privilege name from {@code PrivilegeNames}.
     * </p>
     *
     * @return {@code null} in base implementation; privilege name in subclasses
     * @see EntityWithRequiredPrivilege#getRequiredWritePrivilege()
     */
    @Override
    public String getRequiredWritePrivilege() {
        return null;
    }

    /**
     * Returns the organization ID for multi-tenant scope.
     * <p>
     * Provides the tenant identifier this entity belongs to. Used by the security
     * and multi-tenancy subsystems to enforce organization-based data isolation.
     * May be {@code null} for global entities.
     * </p>
     *
     * @return the organization ID, or {@code null} for global entities
     * @see Organization
     * @see #organizationId
     */
    @Override
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the organization ID for multi-tenant scope (write-once operation).
     * <p>
     * Assigns the tenant identifier only if not already set, ensuring immutability
     * after initial assignment. This prevents accidental reassignment that could
     * violate tenant isolation boundaries.
     * </p>
     *
     * @param organizationId the organization ID to set; ignored if already set
     * @see Organization
     * @see #organizationId
     */
    public void setOrganizationId(Long organizationId) {
        if (this.organizationId == null)
            this.organizationId = organizationId;
    }

    /**
     * Returns the computed reference string for entity identification.
     * <p>
     * Provides a human-readable identifier calculated by the database via the
     * {@code @Formula} annotation. The value combines entity type and key attributes
     * to create a unique reference string.
     * </p>
     *
     * @return the computed reference string, or {@code null} if not yet loaded
     * @see #referenceString
     * @see ModelConstants#DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA
     */
    @Override
    public String getReferenceString() {
        return referenceString;
    }

    /**
     * Returns the unique entity identifier.
     * <p>
     * Provides the database-generated primary key value. May be {@code null}
     * for transient entities not yet persisted.
     * </p>
     *
     * @return the entity ID, or {@code null} for transient entities
     * @see #id
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique entity identifier.
     * <p>
     * Typically managed by JPA and should not be called directly in application code.
     * Used by persistence framework during entity hydration.
     * </p>
     *
     * @param id the entity ID to set
     * @see #id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the full-text search index string.
     * <p>
     * Provides access to the searchable text content extracted from this entity.
     * Typically populated by database triggers or functions for full-text search capabilities.
     * Returns empty string by default if not yet indexed.
     * </p>
     *
     * @return the search index string, empty string if not indexed
     * @see SearchableOrganizationRelatedEntity#getIndexString()
     * @see #indexString
     */
    @Override
    public String getIndexString() {
        return indexString;
    }

    /**
     * Constructs an entity with organization scope.
     * <p>
     * Initializes the entity with the specified organization ID for multi-tenant isolation.
     * The organization ID is immutable after construction.
     * </p>
     *
     * @param organizationId the organization ID for tenant scope, may be {@code null} for global entities
     * @see Organization
     * @see #organizationId
     */
    public OpenkodaEntity(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Returns the timestamp when this entity was last modified.
     * <p>
     * Automatically maintained by Spring Data auditing. Returns the time of the most recent
     * update operation, or the creation time if never modified.
     * </p>
     *
     * @return the last modification timestamp, or {@code null} for transient entities
     * @see #updatedOn
     * @see AuditingEntityListener
     */
    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }

    /**
     * Returns the timestamp when this entity was created.
     * <p>
     * Automatically set by Spring Data auditing at entity creation time.
     * Immutable after initial persistence.
     * </p>
     *
     * @return the creation timestamp, or {@code null} for transient entities
     * @see #createdOn
     * @see AuditingEntityListener
     */
    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    /**
     * Flexible key-value property storage for dynamic entity attributes.
     * <p>
     * Provides a {@code Map<String, String>} persisted to the {@code entity_property} join table
     * for storing custom attributes without schema modifications. Uses {@code @ElementCollection}
     * for JPA mapping with {@code entity_id} foreign key. Foreign key constraint is disabled
     * ({@code NO_CONSTRAINT}) to support polymorphic entity references.
     * </p>
     * <p>
     * <b>Usage Example:</b>
     * </p>
     * <pre>
     * entity.setProperty("customField", "customValue");
     * String value = entity.getProperty("customField");
     * </pre>
     *
     * @see #getProperty(String)
     * @see #setProperty(String, String)
     */
    @ElementCollection
    @CollectionTable(name = "entity_property",
            joinColumns = {
                    @JoinColumn(name = "entity_id", referencedColumnName = "id")
            },
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    private Map<String, String> properties = new HashMap<>();

    /**
     * Retrieves a custom property value by name.
     * <p>
     * Provides access to flexible key-value storage without schema changes.
     * Returns {@code null} if the property does not exist.
     * </p>
     *
     * @param name the property name key
     * @return the property value, or {@code null} if not found
     * @see #properties
     * @see #setProperty(String, String)
     */
    public String getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Sets a custom property value by name.
     * <p>
     * Stores a key-value pair in the flexible property storage.
     * Returns the previous value if the property was already set.
     * </p>
     *
     * @param name the property name key
     * @param value the property value to set
     * @return the previous value associated with the name, or {@code null} if none
     * @see #properties
     * @see #getProperty(String)
     */
    public String setProperty(String name, String value) {
        return properties.put(name, value);
    }


}
