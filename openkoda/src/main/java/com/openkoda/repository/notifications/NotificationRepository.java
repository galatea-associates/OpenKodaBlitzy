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
import com.openkoda.model.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

/**
 * Spring Data JPA repository managing Notification entities for user notifications, push notifications and in-app alerts.
 * <p>
 * This repository provides paginated queries for retrieving notifications with read status, supporting three notification
 * visibility scopes:
 * </p>
 * <ul>
 *   <li>User-specific notifications (organizationId=null, userId set)</li>
 *   <li>Organization-specific notifications (userId=null, organizationId set)</li>
 *   <li>Global notifications (both userId and organizationId null)</li>
 * </ul>
 * <p>
 * The repository uses LEFT JOIN with ReadNotification to efficiently determine read/unread status without N+1 queries.
 * Results are ordered to prioritize unread notifications (rn.notificationId DESC) and recent notifications (n.id DESC).
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Notification
 * @see NotificationKeeper
 * @see com.openkoda.model.notification.ReadNotification
 * @see com.openkoda.repository.SecureNotificationRepository
 */
public interface NotificationRepository extends UnsecuredFunctionalRepositoryWithLongId<Notification> {

    /**
     * Retrieves paginated notifications for a user with read status included.
     * <p>
     * This method uses JPQL SELECT NEW to create NotificationKeeper instances containing the Notification entity
     * and the read notification ID. The query returns notifications matching three conditions:
     * </p>
     * <ol>
     *   <li>User-specific: null organizationId and matching userId</li>
     *   <li>Organization-specific: null userId and organizationId in user's organizations</li>
     *   <li>Global: both userId and organizationId null</li>
     * </ol>
     * <p>
     * Notifications marked hiddenFromAuthor=true are excluded for their creator to prevent self-notification display.
     * The LEFT JOIN with ReadNotification ensures unread notifications (with null rn.notificationId) are included in results.
     * </p>
     * <p>
     * Ordering strategy: Results are ordered by rn.notificationId DESC (unread notifications first with null sorting last in DESC),
     * then by n.id DESC (recent notifications first).
     * </p>
     * <p>
     * Typical usage:
     * <pre>
     * notificationRepository.findAll(currentUser.getId(), userOrganizationIds, PageRequest.of(0, 20));
     * </pre>
     * </p>
     *
     * @param userId the ID of the user requesting notifications, must not be null
     * @param organizationIds the set of organization IDs the user belongs to, may be empty for users without organizations
     * @param pageable pagination and sorting parameters from Spring Data, must not be null
     * @return Page of NotificationKeeper instances containing notification entities with read status, empty page if no notifications match
     */
    @Query("SELECT new com.openkoda.repository.notifications.NotificationKeeper(n, rn.notificationId) FROM Notification n " +
            "LEFT JOIN n.readNotifications rn WHERE " +
            "NOT((n.hiddenFromAuthor = TRUE) AND (:userId = n.createdBy.createdById)) AND" +
            "(((n.organizationId IS NULL AND n.userId=:userId) OR " +
            "(n.userId IS NULL AND n.organizationId IN :organizationIds) OR " +
            "(n.userId IS NULL AND n.organizationId IS NULL)) " +
            ") ORDER BY rn.notificationId DESC, n.id DESC")
    Page<NotificationKeeper> findAll(@Param("userId") Long userId, @Param("organizationIds") Set<Long> organizationIds, Pageable pageable);

}
