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

package com.openkoda.model.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openkoda.model.User;
import com.openkoda.model.common.ModelConstants;
import com.openkoda.model.common.TimestampedEntity;
import jakarta.persistence.*;


/**
 * Tracks read status for notifications, acting as a many-to-many join entity between {@link User} and {@link Notification}.
 * <p>
 * This entity represents the read status tracking for notifications in a multi-tenant environment.
 * Each ReadNotification record indicates that a specific user has marked a specific notification as read.
 * The unique constraint on (user_id, notification_id) enforces idempotency, preventing duplicate read markers.

 * <p>
 * JPA Mapping Details:

 * <ul>
 *   <li>Table: {@code read_notification}</li>
 *   <li>Primary Key: {@code id} generated via ORGANIZATION_RELATED_ID_GENERATOR sequence</li>
 *   <li>Unique Constraint: {@code (user_id, notification_id)} for idempotent mark-as-read operations</li>
 *   <li>Foreign Keys: {@code user_id} references users table, {@code notification_id} references notification table</li>
 * </ul>
 * <p>
 * Extends {@link TimestampedEntity} to automatically track creation and update timestamps.
 * The entity enforces immutability on foreign key columns through {@code insertable=false} and
 * {@code updatable=false} mappings on association fields, with raw FK columns being authoritative.

 * <p>
 * Operational Usage:
 * Services create ReadNotification instances when users mark notifications as read. The unique constraint
 * ensures that multiple mark-as-read operations are idempotent. No setters exist for FK columns to
 * reinforce immutability and data integrity.

 * <p>
 * Example usage:
 * <pre>{@code
 * ReadNotification readNotif = new ReadNotification(userId, notificationId);
 * readNotificationRepository.save(readNotif);
 * }</pre>

 *
 * @author Michał Nowak (mnowak@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see Notification
 * @see User
 * @see TimestampedEntity
 */
@Entity
@Table(
        name = "read_notification",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "notification_id"})}
)
public class ReadNotification extends TimestampedEntity {

    /**
     * Lazy-loaded association to the {@link User} entity who marked the notification as read.
     * <p>
     * This field enables object graph navigation from ReadNotification to User. The {@code @JsonIgnore}
     * annotation prevents serialization to avoid circular references. The mapping uses
     * {@code insertable=false} and {@code updatable=false} because the raw {@code userId} column
     * is authoritative for persistence operations.

     * <p>
     * Accessing this field may trigger a database query if the User entity is not already loaded
     * in the persistence context.

     */
    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, insertable = false, updatable = false, name = "user_id")
    private User user;
    /**
     * Immutable foreign key column referencing the user who read the notification.
     * <p>
     * This column is the authoritative source for the user relationship during insert and update operations.
     * The {@code updatable=false} constraint enforces immutability after creation, ensuring data integrity.
     * No public setter exists for this field, reinforcing immutability at the API level.

     * <p>
     * References: {@code users.id}

     */
    @Column(nullable = false, updatable = false, name = "user_id")
    private Long userId;

    /**
     * Lazy-loaded association to the {@link Notification} entity that was marked as read.
     * <p>
     * This field enables object graph navigation from ReadNotification to Notification for accessing
     * notification details. The {@code @JsonIgnore} annotation prevents serialization to avoid
     * circular references. The mapping uses {@code insertable=false} and {@code updatable=false}
     * because the raw {@code notificationId} column is authoritative for persistence.

     * <p>
     * May trigger a database query if the Notification entity is not already loaded.

     */
    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, insertable = false, updatable = false, name = "notification_id")
    private Notification notification;
    /**
     * Immutable foreign key column referencing the notification that was read.
     * <p>
     * This column is the authoritative source for the notification relationship during persistence operations.
     * The {@code updatable=false} constraint enforces immutability, ensuring that once a read marker is created,
     * it cannot be modified. No public setter exists to reinforce data integrity.

     * <p>
     * References: {@code notification.id}

     */
    @Column(nullable = false, updatable = false, name = "notification_id")
    private Long notificationId;

    /**
     * Primary key identifier for this read notification record.
     * <p>
     * Generated using the ORGANIZATION_RELATED_ID_GENERATOR sequence with an allocation size of 10
     * for performance optimization under concurrent operations. The initial value is set from
     * {@link ModelConstants#INITIAL_ORGANIZATION_RELATED_VALUE}.

     * <p>
     * This field is {@code null} before the entity is persisted and automatically assigned upon
     * successful database insertion.

     */
    @Id
    @SequenceGenerator(name = ORGANIZATION_RELATED_ID_GENERATOR, sequenceName = ORGANIZATION_RELATED_ID_GENERATOR, initialValue = ModelConstants.INITIAL_ORGANIZATION_RELATED_VALUE, allocationSize = 10)
    @GeneratedValue(generator = ORGANIZATION_RELATED_ID_GENERATOR, strategy = GenerationType.SEQUENCE)
    private Long id;


    /**
     * Default no-argument constructor required by JPA for entity instantiation.
     * <p>
     * This constructor is used by Hibernate during entity loading from the database.
     * Application code should typically use the parameterized constructor to ensure
     * all required fields are initialized.

     */
    public ReadNotification() {
    }

    /**
     * Creates a read marker for the specified user and notification.
     * <p>
     * This constructor enforces immutability by accepting userId and notificationId at construction time.
     * The unique constraint on (user_id, notification_id) prevents duplicate read markers, ensuring
     * idempotent mark-as-read operations.

     *
     * @param userId the ID of the user who read the notification (cannot be null)
     * @param notificationId the ID of the notification that was read (cannot be null)
     */
    public ReadNotification(Long userId, Long notificationId) {
        this.userId = userId;
        this.notificationId = notificationId;
    }

    /**
     * Returns the lazy-loaded User entity association.
     * <p>
     * Accessing this method may trigger a database query if the User entity is not already loaded
     * in the persistence context. Use {@link #getUserId()} for identifier-only access without
     * triggering proxy initialization.

     *
     * @return the {@link User} entity who read the notification, or null if not yet loaded
     */
    public User getUser() {
        return user;
    }

    /**
     * Returns the immutable user identifier.
     * <p>
     * This method provides safe identifier-only access without triggering lazy-loading of the
     * User entity association. The value is always non-null for persisted entities and serves
     * as the authoritative foreign key to the users table.

     *
     * @return the user ID (foreign key to users table), never null for persisted entities
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Returns the lazy-loaded Notification entity association.
     * <p>
     * This method enables traversal to notification details. Accessing this method may trigger
     * a database query if the Notification entity is not already loaded. Use {@link #getNotificationId()}
     * for identifier-only access without proxy initialization.

     *
     * @return the {@link Notification} entity that was marked as read, or null if not yet loaded
     */
    public Notification getNotification() {
        return notification;
    }

    /**
     * Returns the immutable notification identifier.
     * <p>
     * This method provides the authoritative foreign key value without loading the full Notification
     * entity. Enables queries and operations using notification IDs without triggering lazy-loading.
     * The value is always non-null for persisted entities.

     *
     * @return the notification ID (foreign key to notification table), never null for persisted entities
     */
    public Long getNotificationId() {
        return notificationId;
    }

    /**
     * Returns the primary key identifier.
     * <p>
     * Generated by the ORGANIZATION_RELATED_ID_GENERATOR sequence with allocation size of 10
     * for optimal performance under concurrent operations. This value is null before the entity
     * is persisted and automatically assigned upon successful database insertion.

     *
     * @return the primary key ID, or null if entity not yet persisted
     */
    public Long getId() {
        return id;
    }
}

