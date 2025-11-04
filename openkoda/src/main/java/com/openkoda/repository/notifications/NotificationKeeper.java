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

package com.openkoda.repository.notifications;

import com.openkoda.model.notification.Notification;

/**
 * Lightweight DTO for JPQL SELECT NEW projections carrying notification data with read status.
 * <p>
 * This class serves as a target for JPQL queries that combine notification data with read status
 * information from a LEFT JOIN with ReadNotification. It carries a Notification entity plus an
 * optional read-marker ID to indicate whether the notification has been read.

 * <p>
 * This is a mutable POJO with no validation or defensive copying. It is intended for in-memory
 * use only and has no persistence mappings. Callers are responsible for the object lifecycle.

 * <p>
 * Example usage in JPQL:
 * <pre>
 * SELECT NEW NotificationKeeper(n, rn.notificationId)
 * FROM Notification n LEFT JOIN n.readNotifications rn
 * </pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Notification
 * @see com.openkoda.repository.notifications.NotificationRepository
 */
public class NotificationKeeper {
    /**
     * The Notification entity being carried. May be null in default constructor.
     */
    private Notification notification;
    
    /**
     * The ReadNotification ID if notification was read, null if unread.
     */
    private Long readNotificationId;

    /**
     * Creates empty NotificationKeeper. Used by frameworks and JPQL projections.
     */
    public NotificationKeeper() {
    }

    /**
     * Creates NotificationKeeper with notification and read status.
     *
     * @param notification the notification entity, may be null
     * @param readNotificationId the read notification ID, null if notification is unread
     */
    public NotificationKeeper(Notification notification, Long readNotificationId) {
        this.notification = notification;
        this.readNotificationId = readNotificationId;
    }

    /**
     * Returns the notification entity.
     *
     * @return the notification, may be null
     */
    public Notification getNotification() {
        return notification;
    }

    /**
     * Sets the notification entity.
     *
     * @param notification the notification to set, may be null
     */
    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    /**
     * Returns the read notification ID.
     *
     * @return the read notification ID, null if notification is unread
     */
    public Long getReadNotificationId() {
        return readNotificationId;
    }

    /**
     * Sets the read notification ID.
     *
     * @param readNotificationId the read notification ID, null for unread notifications
     */
    public void setReadNotificationId(Long readNotificationId) {
        this.readNotificationId = readNotificationId;
    }
}
