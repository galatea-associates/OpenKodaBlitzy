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
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base entity providing automatic audit timestamp functionality for all subclasses.
 * <p>
 * This class serves as a mapped superclass that enables automatic tracking of entity creation
 * and modification information using Spring Data JPA auditing. Entities extending this class
 * automatically receive four audit fields that are populated by the framework without requiring
 * manual intervention.

 * <p>
 * The class uses {@code @MappedSuperclass} with {@code TABLE_PER_CLASS} inheritance strategy,
 * meaning that subclasses create their own database tables containing all inherited fields.
 * The {@code @EntityListeners(AuditingEntityListener.class)} annotation enables Spring Data JPA
 * auditing to automatically populate the audit fields during entity lifecycle events.

 * <p>
 * <b>Audit Fields:</b>

 * <ul>
 * <li>{@code createdBy} - Embeddable UID containing the user name/email and ID who created the entity.
 * Populated automatically via {@code @CreatedBy} annotation.</li>
 * <li>{@code createdOn} - LocalDateTime timestamp when the entity was created.
 * Database default is CURRENT_TIMESTAMP, marked as non-insertable and non-updatable.
 * Populated automatically via {@code @CreatedDate} annotation.</li>
 * <li>{@code modifiedBy} - Embeddable UID containing the user name/email and ID who last modified the entity.
 * Populated automatically via {@code @LastModifiedBy} annotation.</li>
 * <li>{@code updatedOn} - LocalDateTime timestamp when the entity was last modified.
 * Updated automatically via {@code @LastModifiedDate} annotation and {@code @PostUpdate} lifecycle callback.</li>
 * </ul>
 * <p>
 * All timestamp fields use {@code TIMESTAMP WITH TIME ZONE} column definition to ensure proper
 * timezone handling across different database instances. The database manages default timestamps
 * via {@code CURRENT_TIMESTAMP}, with columns configured as non-insertable to delegate timestamp
 * generation to the database layer.

 * <p>
 * Subclasses must implement the abstract {@code getId()} method to return their entity identifier,
 * which is used by the default {@code toString()} implementation.

 * <p>
 * Example usage:
 * <pre>{@code
 * @Entity
 * public class Organization extends TimestampedEntity {
 *     @Id private Long id;
 *     public Long getId() { return id; }
 * }
 * }</pre>

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see AuditingEntityListener
 * @see OpenkodaEntity
 * @see ModelConstants
 */
@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@EntityListeners(AuditingEntityListener.class)
public abstract class TimestampedEntity implements ModelConstants, Serializable {

    /**
     * Embeddable composite user identifier for audit fields.
     * <p>
     * This class encapsulates both the user name/email and user ID to provide complete
     * audit information about who created or modified an entity. It is used by Spring Data
     * JPA auditing to automatically populate {@code createdBy} and {@code modifiedBy} fields
     * in {@link TimestampedEntity}.

     * <p>
     * The class is marked as {@code @Embeddable}, allowing it to be embedded directly into
     * the parent entity's database table. When used with {@code @AttributeOverrides}, the
     * field names can be customized (e.g., mapping to modifiedBy/modifiedById columns).

     * <p>
     * Default values are "UNKNOWN" for the user name and -1L for the user ID, representing
     * cases where the audit information is not available or not yet populated.

     *
     * @see AuditingEntityListener
     * @see TimestampedEntity#createdBy
     * @see TimestampedEntity#modifiedBy
     */
    @Embeddable
    public static class UID implements Serializable {
        /**
         * User name or email address of the user who created or modified the entity.
         * Defaults to "UNKNOWN" if not populated by Spring Data auditing.
         */
        String createdBy;
        
        /**
         * User ID of the user who created or modified the entity.
         * Defaults to -1L if not populated by Spring Data auditing.
         */
        Long createdById;

        /**
         * Default constructor initializing with placeholder values.
         * <p>
         * Creates a UID with user name "UNKNOWN" and user ID -1L, representing
         * cases where audit information is not available.

         */
        public UID() {
            this.createdBy = "UNKNOWN";
            this.createdById = -1L;
        }

        /**
         * Parameterized constructor for creating a UID with specific user information.
         *
         * @param name the user name or email address
         * @param id the user ID
         */
        public UID(String name, Long id) {
            this.createdById = id;
            this.createdBy = name;
        }

        /**
         * Returns the user name or email address.
         *
         * @return the user name or email, or "UNKNOWN" if not set
         */
        public String getCreatedBy() {
            return createdBy;
        }

        /**
         * Sets the user name or email address.
         *
         * @param createdBy the user name or email to set
         */
        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        /**
         * Returns the user ID.
         *
         * @return the user ID, or -1L if not set
         */
        public Long getCreatedById() {
            return createdById;
        }

        /**
         * Sets the user ID.
         *
         * @param createdById the user ID to set
         */
        public void setCreatedById(Long createdById) {
            this.createdById = createdById;
        }

        /**
         * Returns a string representation of this UID in the format "name, ID=id".
         *
         * @return formatted string containing user name and ID
         */
        @Override
        public String toString() {
            return createdBy + ", ID=" + createdById;
        }

        /**
         * Compares this UID with another object for equality.
         * <p>
         * Two UID objects are considered equal if both their {@code createdBy} and
         * {@code createdById} fields are equal.

         *
         * @param o the object to compare with
         * @return true if the objects are equal, false otherwise
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UID uid = (UID) o;
            return Objects.equals(createdBy, uid.createdBy) &&
                    Objects.equals(createdById, uid.createdById);
        }

        /**
         * Returns the hash code for this UID.
         * <p>
         * The hash code is computed based on both {@code createdBy} and {@code createdById} fields.

         *
         * @return hash code value for this UID
         */
        @Override
        public int hashCode() {
            return Objects.hash(createdBy, createdById);
        }
    }

    /**
     * User identifier who created this entity.
     * <p>
     * This field is automatically populated by Spring Data JPA auditing via the {@code @CreatedBy}
     * annotation when the entity is first persisted. The UID embeddable contains both the user
     * name/email and user ID for complete audit tracking.

     * <p>
     * Marked with {@code @JsonIgnore} to exclude from JSON serialization for security reasons.

     *
     * @see UID
     * @see AuditingEntityListener
     */
    @CreatedBy
    @JsonIgnore
    private UID createdBy;

    /**
     * Timestamp when this entity was created.
     * <p>
     * This field is automatically populated by Spring Data JPA auditing via the {@code @CreatedDate}
     * annotation when the entity is first persisted. The database default value is CURRENT_TIMESTAMP,
     * and the field is configured as non-insertable and non-updatable to delegate timestamp generation
     * to the database layer.

     * <p>
     * Uses {@code TIMESTAMP WITH TIME ZONE} column definition for proper timezone handling.

     *
     * @see ModelConstants#CREATED_ON
     */
    @CreatedDate
    @Column(name = CREATED_ON, columnDefinition = "TIMESTAMP WITH TIME ZONE", insertable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdOn;

    /**
     * User identifier who last modified this entity.
     * <p>
     * This field is automatically populated by Spring Data JPA auditing via the {@code @LastModifiedBy}
     * annotation whenever the entity is updated. The {@code @AttributeOverrides} annotation maps the
     * UID embeddable fields to the database columns "modifiedBy" and "modifiedById".

     * <p>
     * Marked with {@code @JsonIgnore} to exclude from JSON serialization for security reasons.

     *
     * @see UID
     * @see AuditingEntityListener
     */
    @LastModifiedBy
    @AttributeOverrides(value = {
            @AttributeOverride(name = "createdBy", column = @Column(name = "modifiedBy")),
            @AttributeOverride(name = "createdById", column = @Column(name = "modifiedById"))
    })
    @JsonIgnore
    private UID modifiedBy;

    /**
     * Timestamp when this entity was last modified.
     * <p>
     * This field is automatically updated by Spring Data JPA auditing via the {@code @LastModifiedDate}
     * annotation whenever the entity is updated. Additionally, the {@code @PostUpdate} lifecycle callback
     * {@link #postUpdate()} refreshes this field to the current timestamp after each update operation.

     * <p>
     * The database default value is CURRENT_TIMESTAMP. The field is configured as non-insertable but
     * updatable, allowing the application to update the timestamp while delegating initial value to the database.
     * Uses {@code TIMESTAMP WITH TIME ZONE} column definition for proper timezone handling.

     *
     * @see ModelConstants#UPDATED_ON
     * @see #postUpdate()
     */
    @LastModifiedDate
    @Column(name = UPDATED_ON, columnDefinition = "TIMESTAMP WITH TIME ZONE", insertable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    protected LocalDateTime updatedOn;

    /**
     * Returns the entity identifier.
     * <p>
     * Subclasses must implement this method to return their primary key value,
     * which is used by the {@link #toString()} method to generate a meaningful
     * string representation of the entity.

     *
     * @return the entity ID, never null for persisted entities
     */
    public abstract Long getId();

    /**
     * Returns a string representation of this entity in the format "ClassName id: X".
     * <p>
     * The implementation uses the simple class name of the concrete subclass and the
     * entity ID returned by {@link #getId()} to provide a concise, readable representation
     * suitable for logging and debugging purposes.

     *
     * @return formatted string containing class name and entity ID
     */
    @Override
    public String toString() {
        return new StringBuilder().append(this.getClass().getSimpleName()).append(" id: ").append(getId()).toString();
    }

    /**
     * JPA lifecycle callback that refreshes the {@code updatedOn} timestamp after entity update.
     * <p>
     * This method is automatically invoked by the JPA persistence provider after an entity
     * update operation via the {@code @PostUpdate} annotation. It sets {@code updatedOn} to
     * the current timestamp, ensuring the modification time is always accurate even if the
     * database trigger or Spring Data auditing does not update the field.

     * <p>
     * This provides a dual mechanism for timestamp updates: Spring Data auditing via
     * {@code @LastModifiedDate} and explicit refresh via this lifecycle callback.

     *
     * @see #updatedOn
     */
    @PostUpdate
    protected void postUpdate() {
        updatedOn = LocalDateTime.now();
    }

    /**
     * Returns the timestamp when this entity was created.
     * <p>
     * The value is automatically populated by Spring Data JPA auditing when the entity
     * is first persisted. The timestamp reflects the database server time in the configured
     * timezone.

     *
     * @return the creation timestamp, or null if not yet persisted
     */
    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    /**
     * Returns the timestamp when this entity was last modified.
     * <p>
     * The value is automatically updated by Spring Data JPA auditing and the {@code @PostUpdate}
     * lifecycle callback whenever the entity is modified. For newly created entities that have
     * not been updated, this may be null or equal to {@code createdOn}.

     *
     * @return the last modification timestamp, or null if not yet modified
     * @see #postUpdate()
     */
    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }

    /**
     * Returns the user identifier who created this entity.
     * <p>
     * The UID contains both the user name/email and user ID, providing complete audit information.
     * The value is automatically populated by Spring Data JPA auditing.

     *
     * @return the UID of the creator, or a UID with "UNKNOWN" values if not populated
     * @see UID
     */
    public UID getCreatedBy() {
        return createdBy;
    }

    /**
     * Returns the user identifier who last modified this entity.
     * <p>
     * The UID contains both the user name/email and user ID, providing complete audit information.
     * The value is automatically populated by Spring Data JPA auditing whenever the entity is updated.

     *
     * @return the UID of the last modifier, or a UID with "UNKNOWN" values if not yet modified
     * @see UID
     */
    public UID getModifiedBy() {
        return modifiedBy;
    }
}
