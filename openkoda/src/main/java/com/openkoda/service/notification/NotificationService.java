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

package com.openkoda.service.notification;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.service.event.ApplicationEvent;
import com.openkoda.dto.NotificationDto;
import com.openkoda.model.User;
import com.openkoda.model.notification.Notification;
import com.openkoda.model.notification.ReadNotification;
import com.openkoda.repository.notifications.NotificationKeeper;
import com.openkoda.repository.notifications.NotificationRepository;
import com.openkoda.repository.notifications.SecureNotificationRepository;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.openkoda.repository.specifications.NotificationSepcifications.allUnreadForUser;

/**
 * Central notification lifecycle management service for creating, persisting, and delivering application notifications.
 * <p>
 * This service orchestrates the complete notification workflow:

 * <ol>
 *   <li>Create notification entity with message, type, and recipient scope</li>
 *   <li>Persist to database via NotificationRepository</li>
 *   <li>Emit ApplicationEvent.NOTIFICATION_CREATED for WebSocket delivery</li>
 *   <li>Track read state in ReadNotification junction table</li>
 *   <li>Query with pagination and secure filtering by recipient</li>
 * </ol>
 * <p>
 * Notifications are organization-scoped via recipient User entity, supporting multi-tenancy isolation.
 * The service emits ApplicationEvent.NOTIFICATION_CREATED synchronously for WebSocket broadcasting to connected clients.

 * <p>
 * All persistence operations are transactional (inherited from ComponentProvider) for atomic database updates.
 * Security is enforced through secure repositories with privilege checks for querying notifications.

 * <p>
 * Key dependencies: NotificationRepository, ReadNotificationRepository, UserRepository, ApplicationEventService

 * <p>
 * Example usage:
 * <pre>{@code
 * Notification notification = notificationService.createOrganizationNotification(
 *     NotificationType.INFO, "Welcome message", organizationId, null, null);
 * }</pre>

 * <p>
 * Design patterns: Service layer pattern, event-driven architecture, pagination for large result sets

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Notification
 * @see ReadNotification
 * @see ApplicationEvent#NOTIFICATION_CREATED
 * @see NotificationKeeper
 * @see Pageable
 */
@Service
public class NotificationService extends ComponentProvider {

    @Inject
    SecureNotificationRepository secureNotificationRepository;
    @Inject
    NotificationRepository notificationRepository;

    /**
     * Returns the count of all unread notifications for a specific user across specified organizations.
     * <p>
     * This method uses secure repository access with the allUnreadForUser specification to filter
     * notifications by user ID and organization scope. The count is converted from long to int
     * using Math.toIntExact for safe downcasting.

     *
     * @param userId the unique identifier of the user to count unread notifications for
     * @param organizationIds set of organization IDs the user belongs to for multi-tenant filtering
     * @return the count of unread notifications as an integer
     * @throws ArithmeticException if the notification count exceeds Integer.MAX_VALUE
     * @see SecureNotificationRepository#count
     * @see com.openkoda.repository.specifications.NotificationSepcifications#allUnreadForUser
     */
    public int getUsersUnreadNotificationsNumber(Long userId, Set<Long> organizationIds) {
        debug("[getUsersUnreadNotificationsNumber]");
        return Math.toIntExact(secureNotificationRepository.count(allUnreadForUser(userId, organizationIds)));
    }

    /**
     * Retrieves paginated list of unread notifications for a specific user.
     * <p>
     * This method queries NotificationKeeper projections with pagination support, then filters
     * results to include only unread notifications (where readNotificationId is null). The method
     * uses Spring Data pagination to limit result set size for performance.

     *
     * @param userId the unique identifier of the user to retrieve notifications for
     * @param organizationIds set of organization IDs the user belongs to for filtering
     * @param notificationPageable pagination parameters including page number, size, and sort order
     * @return list of unread Notification entities filtered from NotificationKeeper projections
     * @see NotificationKeeper
     * @see Pageable
     * @see NotificationRepository#findAll(Long, Set, Pageable)
     */
    public List<Notification> getUsersUnreadNotifications(Long userId, Set<Long> organizationIds, Pageable notificationPageable) {
        debug("[getUsersUnreadNotifications]");

        Page<NotificationKeeper> keepers = notificationRepository.findAll(userId, organizationIds, notificationPageable);

        List<Notification> unreadNotificationsList = new ArrayList<>();
        for (NotificationKeeper k : keepers.getContent()) {
            if (k.getReadNotificationId() == null) {
                unreadNotificationsList.add(k.getNotification());
            }
        }
        return unreadNotificationsList;
    }

    /**
     * Converts a list of Notification entities to a comma-separated string of IDs.
     * <p>
     * This utility method serializes notification IDs into CSV format for bulk operations
     * and easier transmission to controllers. Brackets and whitespace are removed from
     * the string representation.

     * <p>
     * Example output: "123,456,789" for notifications with IDs 123, 456, and 789

     *
     * @param notifications list of Notification entities to serialize
     * @return comma-separated string of notification IDs with brackets and spaces removed
     */
    public String getIdListAsString(List<Notification> notifications) {
        debug("[getIdListAsString]");

        List<Long> usersUnreadNotificationsIdList = new ArrayList<>();
        for (Notification n : notifications) {
            usersUnreadNotificationsIdList.add(n.getId());
        }
        String idString = usersUnreadNotificationsIdList.toString().replace("[", "").replace("]", "").replaceAll("\\s+", "");

        return idString;
    }

    /**
     * Creates a global-scope notification visible to all users across all organizations.
     * <p>
     * This method creates a system-wide notification that is not scoped to any specific organization
     * or user. The notification is persisted to the database and an ApplicationEvent.NOTIFICATION_CREATED
     * event is emitted for WebSocket broadcasting.

     *
     * @param type the notification type (INFO, WARNING, ERROR, SUCCESS)
     * @param message the notification text content
     * @param requiredPrivilege the privilege required to view this notification, or null for no restriction
     * @param attachmentURL optional URL for notification attachment, or null if no attachment
     * @return true if notification was created and event emitted successfully
     * @see Notification.NotificationType
     * @see ApplicationEvent#NOTIFICATION_CREATED
     */
    public boolean createGlobalNotification(Notification.NotificationType type, String message, String requiredPrivilege, String attachmentURL) {
        debug("[createGlobalNotification]");
        Notification notification = new Notification(message, type, requiredPrivilege);
        notification.setAttachmentURL(attachmentURL);
        notificationRepository.save(notification);

        services.applicationEvent.emitEvent(ApplicationEvent.NOTIFICATION_CREATED, new NotificationDto(notification));
        return true;
    }

    /**
     * Creates an organization-scoped notification visible only to members of the specified organization.
     * <p>
     * This method creates a tenant-isolated notification that is only visible to users belonging
     * to the specified organization. The notification is persisted to the database and an
     * ApplicationEvent.NOTIFICATION_CREATED event is emitted for WebSocket delivery.

     *
     * @param type the notification type (INFO, WARNING, ERROR, SUCCESS)
     * @param message the notification text content
     * @param organizationId the target organization ID for tenant-scoped visibility
     * @param requiredPrivilege the privilege required to view this notification, or null for no restriction
     * @param attachmentURL optional URL for notification attachment, or null if no attachment
     * @return the created Notification entity with generated ID
     * @see Notification
     * @see ApplicationEvent#NOTIFICATION_CREATED
     */
    public Notification createOrganizationNotification(Notification.NotificationType type, String message, Long organizationId, String requiredPrivilege, String attachmentURL) {
        debug("[createOrganizationNotification]");
        Notification notification = new Notification(message, type, organizationId, requiredPrivilege);
        notification.setAttachmentURL(attachmentURL);
        Notification n = notificationRepository.save(notification);

        services.applicationEvent.emitEvent(ApplicationEvent.NOTIFICATION_CREATED, new NotificationDto(notification));
        return n;
    }

    /**
     * Creates an organization-scoped notification with subject line, using default propagation settings.
     * <p>
     * This convenience method delegates to the full implementation with propagate=false and
     * hiddenFromAuthor=false. Use this when you need a subject line but don't require
     * propagation control or author visibility filtering.

     *
     * @param type the notification type (INFO, WARNING, ERROR, SUCCESS)
     * @param subject the notification subject line
     * @param message the notification text content
     * @param organizationId the target organization ID for tenant-scoped visibility
     * @param requiredPrivilege the privilege required to view this notification, or null for no restriction
     * @param attachmentURL optional URL for notification attachment, or null if no attachment
     * @return the created Notification entity with generated ID
     * @see #createOrganizationNotificationWithSubject(Notification.NotificationType, String, String, Long, String, String, boolean, boolean)
     */
    public Notification createOrganizationNotificationWithSubject(Notification.NotificationType type, String subject, String message, Long organizationId, String requiredPrivilege, String attachmentURL) {
        debug("[createOrganizationNotificationWithSubject]");
        return createOrganizationNotificationWithSubject(type, subject, message, organizationId, requiredPrivilege, attachmentURL, false, false);
    }

    /**
     * Creates an organization-scoped notification with full control over subject, propagation, and author visibility.
     * <p>
     * This method provides complete control over notification creation including subject line,
     * propagation to child organizations, and visibility filtering for the notification author.
     * The notification is persisted to the database and an ApplicationEvent.NOTIFICATION_CREATED
     * event is emitted for WebSocket delivery.

     *
     * @param type the notification type (INFO, WARNING, ERROR, SUCCESS)
     * @param subject the notification subject line
     * @param message the notification text content
     * @param organizationId the target organization ID for tenant-scoped visibility
     * @param requiredPrivilege the privilege required to view this notification, or null for no restriction
     * @param attachmentURL optional URL for notification attachment, or null if no attachment
     * @param propagate if true, notification propagates to child organizations in the hierarchy
     * @param hiddenFromAuthor if true, notification is hidden from the creating user
     * @return the created Notification entity with generated ID
     * @see Notification
     * @see ApplicationEvent#NOTIFICATION_CREATED
     */
    public Notification createOrganizationNotificationWithSubject(Notification.NotificationType type, String subject, String message, Long organizationId, String requiredPrivilege, String attachmentURL, boolean propagate, boolean hiddenFromAuthor) {
        debug("[createOrganizationNotificationWithSubject]");
        Notification notification = new Notification(subject, message, type, organizationId, requiredPrivilege, propagate, hiddenFromAuthor);
        notification.setAttachmentURL(attachmentURL);
        Notification n = notificationRepository.save(notification);

        services.applicationEvent.emitEvent(ApplicationEvent.NOTIFICATION_CREATED, new NotificationDto(notification));
        return n;
    }

    /**
     * Creates a user-scoped notification visible only to a specific user.
     * <p>
     * This method creates the most specific notification type targeted at a single user.
     * The notification is persisted to the database and an ApplicationEvent.NOTIFICATION_CREATED
     * event is emitted for WebSocket delivery to the target user.

     *
     * @param type the notification type (INFO, WARNING, ERROR, SUCCESS)
     * @param message the notification text content
     * @param requiredPrivilege the privilege required to view this notification, or null for no restriction
     * @param userId the target user ID for direct user notification
     * @param attachmentURL optional URL for notification attachment, or null if no attachment
     * @return the created Notification entity with generated ID
     * @see Notification
     * @see User
     * @see ApplicationEvent#NOTIFICATION_CREATED
     */
    public Notification createUserNotification(Notification.NotificationType type, String message, String requiredPrivilege, Long userId, String attachmentURL) {
        debug("[createUserNotification]");
        Notification notification = new Notification(message, type, requiredPrivilege, userId);
        notification.setAttachmentURL(attachmentURL);
        Notification n = notificationRepository.save(notification);

        services.applicationEvent.emitEvent(ApplicationEvent.NOTIFICATION_CREATED, new NotificationDto(notification));
        return n;
    }

    /**
     * Marks specified notifications as read for a user by creating ReadNotification junction entities.
     * <p>
     * This method parses a comma-separated string of notification IDs and creates ReadNotification
     * entries to track read state. The operation is idempotent - marking already-read notifications
     * again is safe. Uses unsecure repository for bulk persistence without privilege checks.

     *
     * @param unreadNotifications comma-separated string of notification IDs to mark as read (e.g., "123,456,789")
     * @param userId the user ID marking notifications as read
     * @return true if notifications were marked successfully, false if input string is blank
     * @throws NumberFormatException if CSV contains malformed ID tokens that cannot be parsed
     * @see ReadNotification
     * @see com.openkoda.repository.notifications.ReadNotificationRepository
     */
    public boolean markAsRead(String unreadNotifications, Long userId) {
        debug("[markAsRead] userId: {}", userId);
        if (StringUtils.isNotBlank(unreadNotifications)) {
            List<String> idStringList = Arrays.asList(unreadNotifications.split(","));
            repositories.unsecure.readNotification.saveAll(idStringList.stream().map(idString -> new ReadNotification(userId, Long.valueOf(idString))).collect(Collectors.toSet()));
            return true;
        }
        return false;
    }

    /**
     * Marks all unread notifications as read for a user, optionally scoped to a specific organization.
     * <p>
     * This method validates user membership, queries all unread notifications using the allUnreadForUser
     * specification, and creates ReadNotification markers in bulk. If organizationId is provided and
     * the user belongs to that organization, scope is limited to that organization only. Otherwise,
     * all notifications across all user's organizations are marked as read.

     *
     * @param userId the user ID marking all notifications as read
     * @param organizationId optional organization ID to limit scope, or null for all organizations
     * @return true if user was found and notifications were marked, false if user not found
     * @see ReadNotification
     * @see User#getOrganizationIds()
     * @see com.openkoda.repository.specifications.NotificationSepcifications#allUnreadForUser
     */
    public boolean markAllAsRead(Long userId, Long organizationId) {
        debug("[markAllAsRead] userId: {} orgId: {}", userId, organizationId);
        User user = repositories.unsecure.user.findOne(userId);
        if (user != null) {
            Set<Long> orgsId = new HashSet<>();
            if(organizationId != null && Arrays.asList(user.getOrganizationIds()).contains(organizationId)) {
                orgsId = Collections.singleton(organizationId);
            } else if(user.getOrganizationIds() != null) {
                orgsId = Set.of(user.getOrganizationIds());
            }
            List<Notification> allUnreadForUser = repositories.secure.notification.search(allUnreadForUser(userId, orgsId));
            repositories.unsecure.readNotification.saveAll(allUnreadForUser.stream().map(notification -> new ReadNotification(userId, notification.getId())).collect(Collectors.toSet()));
            return true;
        }
        return false;
    }

    /**
     * Checks if a notification is global-scoped (visible to all users across all organizations).
     * <p>
     * A notification is global when both organizationId and userId are null.

     *
     * @param notification the NotificationDto to check scope for
     * @return true if notification is global-scoped, false otherwise
     * @see NotificationDto
     */
    public boolean isGlobal(NotificationDto notification) {
        debug("[isGlobal]");
        return notification.getOrganizationId() == null && notification.getUserId() == null;
    }

    /**
     * Checks if a notification is organization-scoped (visible to organization members only).
     * <p>
     * A notification is organization-scoped when organizationId is set but userId is null.

     *
     * @param notification the NotificationDto to check scope for
     * @return true if notification is organization-scoped, false otherwise
     * @see NotificationDto
     */
    public boolean isOrganization(NotificationDto notification) {
        debug("[isOrganization]");
        return notification.getOrganizationId() != null && notification.getUserId() == null;
    }

    /**
     * Checks if a notification is user-scoped (visible to a specific user only).
     * <p>
     * A notification is user-scoped when userId is set but organizationId is null.

     *
     * @param notification the NotificationDto to check scope for
     * @return true if notification is user-scoped, false otherwise
     * @see NotificationDto
     */
    public boolean isUsers(NotificationDto notification) {
        debug("[isUsers]");
        return notification.getUserId() != null && notification.getOrganizationId() == null;
    }

}
