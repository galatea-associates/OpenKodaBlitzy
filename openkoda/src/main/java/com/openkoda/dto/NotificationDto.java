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

package com.openkoda.dto;

import com.openkoda.model.notification.Notification;
import org.apache.commons.lang3.StringUtils;

/**
 * Data Transfer Object for notification payloads supporting multi-channel notification delivery.
 * <p>
 * This DTO encapsulates notification data for transmission across service boundaries, supporting
 * three notification scope variants: global notifications (no userId or organizationId),
 * organization-scoped notifications (organizationId set), and user-scoped notifications
 * (userId set, optionally within an organization context).
 * </p>
 * <p>
 * Used extensively by notification services, event listeners, and messaging subsystems
 * to construct and deliver notifications via email, in-app messaging, or other channels.
 * Implements {@link CanonicalObject} for standardized notification message formatting.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Notification
 * @see CanonicalObject
 */
public class NotificationDto implements CanonicalObject {

    /**
     * Optional URL to an attachment file associated with this notification.
     * <p>
     * May be null if no attachment is present. When set, notification renderers
     * should provide a download link or embedded content reference.
     * </p>
     */
    public String attachmentURL;
    
    /**
     * Notification body text containing the primary message content.
     * <p>
     * This field is required for all notifications and should contain the main
     * notification payload suitable for display in the target channel (email body,
     * in-app notification text, etc.).
     * </p>
     */
    public String message;
    
    /**
     * Notification subject line or title.
     * <p>
     * When blank or null, {@link #getSubject()} returns "Notification" as the default.
     * Used primarily for email notifications and in-app notification headers.
     * </p>
     */
    public String subject;
    
    /**
     * Enum classifying the notification delivery channel and type.
     * <p>
     * Determines how the notification should be rendered and delivered
     * (e.g., EMAIL, IN_APP, SMS). See {@link Notification.NotificationType}
     * for available types.
     * </p>
     */
    public Notification.NotificationType notificationType;
    
    /**
     * Target user ID for user-specific notifications.
     * <p>
     * When null, the notification is organization-wide (if organizationId is set)
     * or global (if both userId and organizationId are null). When set, the
     * notification is delivered only to the specified user.
     * </p>
     */
    public Long userId;
    
    /**
     * Target organization ID for organization-scoped notifications.
     * <p>
     * When null, the notification is either user-specific (if userId is set)
     * or global (if both are null). When set, the notification is scoped to
     * the specified organization's context.
     * </p>
     */
    public Long organizationId;
    
    /**
     * Privilege string required to view this notification.
     * <p>
     * When null or blank, the notification is unrestricted and visible to all
     * recipients. When set, only users with the specified privilege can view
     * the notification content. Format matches {@link com.openkoda.model.Privilege}
     * canonical names.
     * </p>
     */
    public String requiredPrivilege;
    
    /**
     * Flag controlling whether this notification propagates to child entities.
     * <p>
     * When true, the notification may be delivered to related child entities
     * (e.g., child organizations, team members). When false, delivery is
     * restricted to the exact target specified by userId/organizationId.
     * </p>
     */
    public boolean propagate;

    /**
     * Default no-argument constructor for framework and serialization use.
     * <p>
     * Creates an empty NotificationDto with all fields uninitialized. Intended
     * for use by Jackson deserialization, JPA, and other frameworks requiring
     * a default constructor.
     * </p>
     */
    public NotificationDto(){
    }

    /**
     * Constructs a basic notification with message and type only.
     * <p>
     * Creates an unrestricted, global notification with no organization or user
     * targeting. Suitable for system-wide announcements or maintenance notices.
     * </p>
     *
     * @param message the notification body text (required)
     * @param type the notification delivery channel and type (required)
     */
    public NotificationDto(String message, Notification.NotificationType type) {
        this.message = message;
        this.notificationType = type;
    }
    
    /**
     * Constructs a global privileged notification.
     * <p>
     * Creates a notification visible to all users who possess the specified privilege,
     * regardless of organization membership. Suitable for admin-level announcements
     * or security alerts requiring elevated permissions to view.
     * </p>
     *
     * @param message the notification body text (required)
     * @param type the notification delivery channel and type (required)
     * @param requiredPrivilege the privilege required to view this notification (nullable for unrestricted)
     */
    public NotificationDto(String message, Notification.NotificationType type, String requiredPrivilege) {
        this.message = message;
        this.notificationType = type;
        this.requiredPrivilege = requiredPrivilege;
    }
    
    /**
     * Constructs a user-targeted notification with optional privilege restriction.
     * <p>
     * Creates a notification delivered exclusively to the specified user. When
     * requiredPrivilege is set, the user must possess that privilege to view the
     * notification content. Suitable for personal alerts, task assignments, or
     * user-specific messages.
     * </p>
     *
     * @param message the notification body text (required)
     * @param type the notification delivery channel and type (required)
     * @param requiredPrivilege the privilege required to view this notification (nullable for unrestricted)
     * @param userId the target user ID (required for user-scoped delivery)
     */
    public NotificationDto(String message, Notification.NotificationType type, String requiredPrivilege, Long userId) {
        this.message = message;
        this.notificationType = type;
        this.userId = userId;
        this.requiredPrivilege = requiredPrivilege;
    }

    /**
     * Constructs an organization-scoped notification with optional privilege restriction.
     * <p>
     * Creates a notification delivered to all users within the specified organization.
     * When requiredPrivilege is set, only organization members with that privilege
     * can view the notification. Suitable for organization-wide announcements,
     * policy updates, or tenant-specific alerts.
     * </p>
     *
     * @param message the notification body text (required)
     * @param type the notification delivery channel and type (required)
     * @param organizationId the target organization ID (required for organization-scoped delivery)
     * @param requiredPrivilege the privilege required to view this notification (nullable for unrestricted)
     */
    public NotificationDto(String message, Notification.NotificationType type, Long organizationId, String requiredPrivilege) {
        this.message = message;
        this.notificationType = type;
        this.organizationId = organizationId;
        this.requiredPrivilege = requiredPrivilege;
    }

    /**
     * Constructs an organization-user notification with optional privilege restriction.
     * <p>
     * Creates a notification delivered to a specific user within an organization context.
     * This is the most targeted notification variant, combining organization scope with
     * user-specific delivery. When requiredPrivilege is set, the user must possess that
     * privilege within the organization context to view the notification. Suitable for
     * organization-specific task assignments, role-based alerts, or tenant-user messages.
     * </p>
     *
     * @param message the notification body text (required)
     * @param type the notification delivery channel and type (required)
     * @param organizationId the target organization ID providing notification context (required)
     * @param requiredPrivilege the privilege required to view this notification (nullable for unrestricted)
     * @param userId the target user ID within the organization (required)
     */
    public NotificationDto(String message, Notification.NotificationType type, Long organizationId, String requiredPrivilege, Long userId) {
        this.message = message;
        this.notificationType = type;
        this.organizationId = organizationId;
        this.userId = userId;
        this.requiredPrivilege = requiredPrivilege;
    }

    /**
     * Constructs a NotificationDto by mapping from a Notification entity.
     * <p>
     * Creates a DTO representation of a persisted Notification entity, copying all
     * fields including message, subject, type, targeting (userId/organizationId),
     * privilege restrictions, attachment URL, and propagation flag. Used for
     * transferring notification data from the persistence layer to service or
     * presentation layers.
     * </p>
     *
     * @param notification the source Notification entity to map from (required, must not be null)
     */
    public NotificationDto(Notification notification) {
        this.message = notification.getMessage();
        this.subject = notification.getSubject();
        this.organizationId = notification.getOrganizationId();
        this.requiredPrivilege = notification.getRequiredPrivilege();
        this.notificationType = notification.getType();
        this.userId = notification.getUserId();
        this.attachmentURL = notification.getAttachmentURL();
        this.propagate = notification.isPropagate();
    }

    /**
     * Returns the notification body text.
     *
     * @return the notification message content, may be null if not yet initialized
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the notification body text.
     *
     * @param message the notification message content to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the notification subject line with automatic default.
     * <p>
     * When the subject field is blank or null, this method returns "Notification"
     * as the default subject. This ensures notifications always have a subject
     * for display in email headers and notification summaries.
     * </p>
     *
     * @return the subject line, or "Notification" if subject is blank or null
     */
    public String getSubject() {
        return StringUtils.isNotBlank(subject) ? subject : "Notification";
    }

    /**
     * Sets the notification subject line.
     *
     * @param subject the subject line to set, may be null (will use default in getSubject())
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Returns the notification type determining the delivery channel.
     *
     * @return the notification type enum (EMAIL, IN_APP, etc.), may be null if not yet initialized
     */
    public Notification.NotificationType getNotificationType() {
        return notificationType;
    }

    /**
     * Sets the notification type determining the delivery channel.
     *
     * @param notificationType the notification type enum to set
     */
    public void setNotificationType(Notification.NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    /**
     * Returns the target user ID for user-scoped notifications.
     *
     * @return the user ID, or null for organization-wide or global notifications
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Sets the target user ID for user-scoped notifications.
     *
     * @param userId the user ID to target, or null for organization-wide or global notifications
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Returns the target organization ID for organization-scoped notifications.
     *
     * @return the organization ID, or null for user-only or global notifications
     */
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the target organization ID for organization-scoped notifications.
     *
     * @param organizationId the organization ID to target, or null for user-only or global notifications
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Returns the privilege required to view this notification.
     *
     * @return the privilege string matching canonical privilege names, or null for unrestricted access
     */
    public String getRequiredPrivilege() {
        return requiredPrivilege;
    }

    /**
     * Sets the privilege required to view this notification.
     *
     * @param requiredPrivilege the privilege string to require, or null for unrestricted access
     */
    public void setRequiredPrivilege(String requiredPrivilege) {
        this.requiredPrivilege = requiredPrivilege;
    }

    /**
     * Returns the attachment file URL if present.
     *
     * @return the attachment URL, or null if no attachment is associated
     */
    public String getAttachmentURL() {
        return attachmentURL;
    }

    /**
     * Sets the attachment file URL for this notification.
     *
     * @param attachmentURL the attachment URL to set, or null to remove attachment reference
     */
    public void setAttachmentURL(String attachmentURL) {
        this.attachmentURL = attachmentURL;
    }

    /**
     * Returns whether this notification propagates to child entities.
     *
     * @return true if notification should propagate to related entities, false otherwise
     */
    public Boolean getPropagate() {
        return propagate;
    }

    /**
     * Sets whether this notification propagates to child entities.
     * <p>
     * Note: This method accepts a Boolean wrapper but assigns to a primitive boolean field.
     * Passing null will cause a NullPointerException during unboxing. Callers should ensure
     * a non-null value is provided.
     * </p>
     *
     * @param propagate true to enable propagation, false to restrict to exact target (must not be null)
     * @throws NullPointerException if propagate is null during unboxing to primitive boolean
     */
    public void setPropagate(Boolean propagate) {
        this.propagate = propagate;
    }

    /**
     * Generates a standardized notification message for logging and auditing.
     * <p>
     * Implements {@link CanonicalObject#notificationMessage()} to produce a formatted
     * string representation suitable for audit logs, system notifications, and debugging.
     * The format varies based on notification targeting:
     * </p>
     * <ul>
     *   <li>User + Organization: "Notification for {userId}(UID), within {organizationId}(OrgID) of type {type}: "{message}""</li>
     *   <li>Other scopes: "Notification of type {type}: "{message}""</li>
     * </ul>
     *
     * @return formatted notification message string with targeting context and content
     */
    @Override
    public String notificationMessage() {
        StringBuilder sb = new StringBuilder("Notification ");
        if(userId != null && organizationId != null) {
            sb.append(String.format("for %d(UID), within %d(OrgID) ", userId, organizationId));
        }
        sb.append(String.format("of type %s: \"%s\"", notificationType, message));
        return sb.toString();
    }
}
