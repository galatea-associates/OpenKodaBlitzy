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

import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.model.notification.ReadNotification;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository managing ReadNotification entities for tracking notification read status.
 * <p>
 * This repository provides queries for checking if notifications have been read by users. The ReadNotification
 * entity acts as a marker indicating that a specific notification has been viewed or acknowledged by a user.
 * This repository is used by notification services to mark notifications as read and check read status.
 * </p>
 * <p>
 * Runtime proxy created by Spring Data at application startup with automatic exception translation. The interface
 * extends UnsecuredFunctionalRepositoryWithLongId to provide standard CRUD operations plus custom query methods.
 * Callers should provide transactional context for consistency when performing read status updates.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * boolean isRead = readNotificationRepository.existsByNotificationId(notificationId);
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ReadNotification
 * @see NotificationRepository
 */
@Repository
public interface ReadNotificationRepository extends UnsecuredFunctionalRepositoryWithLongId<ReadNotification> {
    
    /**
     * Checks if a notification has been marked as read by checking for ReadNotification existence.
     * <p>
     * This method uses a Spring Data derived query that implements an efficient existence check without
     * loading the entity into memory. The generated SQL performs a SELECT 1 query:
     * {@code SELECT 1 FROM read_notification WHERE notification_id = ?}
     * </p>
     * <p>
     * This is the recommended approach for checking read status before marking notifications as read,
     * as it avoids the overhead of loading full entity instances when only existence information is needed.
     * </p>
     *
     * @param notificationId the notification ID to check for read status, must not be null
     * @return true if a ReadNotification record exists for the given notification ID (notification was read),
     *         false otherwise
     */
    boolean existsByNotificationId(Long notificationId);
}
