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

package com.openkoda.controller.notification;

import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.security.OrganizationUser;
import com.openkoda.core.security.UserProvider;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract base controller providing notification listing, marking read/unread, and deletion operations.
 * <p>
 * Implements common notification management patterns including user-scoped notification listing with pagination,
 * bulk mark-as-read, single notification read status toggle, and notification deletion. Subclasses provide
 * concrete endpoint mappings. Uses {@code services.notification} for persistence and delivery orchestration.
 * </p>
 * <p>
 * All methods are {@code protected} and intended for reuse by concrete controllers rather than direct exposure.
 * The class declares no instance fields (stateless) and relies on thread-safe injected services/repositories
 * provided by the inherited {@link AbstractController}.
 * </p>
 * <p>
 * Implementation uses the Flow pipeline DSL to compose operations: {@code Flow.init()} followed by fluent
 * {@code thenSet}/{@code then} and {@code execute()} to produce a {@link PageModelMap} containing model
 * attributes (notably the model key 'notificationPage').
 * </p>
 *
 * <h3>Important Notes:</h3>
 * <ul>
 *   <li>Contains unguarded {@code Optional.get()} after {@code UserProvider.getFromContext()}; callers must
 *       ensure a valid security context to avoid {@code NoSuchElementException}</li>
 *   <li>Changes in service/repository signatures, the Flow API, or the 'notificationPage' model key will
 *       break downstream controllers and views</li>
 *   <li>All methods delegate persistence to services/repositories and use Flow for transactional execution</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractController
 * @see com.openkoda.core.flow.Flow
 * @see com.openkoda.core.flow.PageModelMap
 * @see com.openkoda.service.notification.NotificationService
 * @see com.openkoda.repository.notifications.NotificationRepository
 */
public class AbstractNotificationController extends AbstractController {

    /**
     * Lists all user-specific notifications across all organizations with pagination.
     * <p>
     * Retrieves notifications for the authenticated user from all organizations they belong to.
     * Results are ordered by creation date descending. Delegates to
     * {@code repositories.unsecure.notification.findAll(userId, organizationIds, notificationPageable)}
     * to fetch a paginated notification page.
     * </p>
     * <p>
     * The method logs invocation, calls {@link UserProvider#getFromContext()}, and invokes
     * {@code Optional.get()} to obtain the {@link OrganizationUser} and its organization IDs.
     * </p>
     *
     * @param userId the user ID for filtering notifications, must match authenticated user
     * @param notificationPageable pagination parameters (page, size, sort) for notification results
     * @return {@link PageModelMap} containing 'notificationPage' attribute with paginated notification data
     * @throws java.util.NoSuchElementException if {@code UserProvider.getFromContext()} returns empty Optional
     *         (user not authenticated)
     * @see UserProvider#getFromContext()
     * @see OrganizationUser#getOrganizationIds()
     */
    protected PageModelMap getAllNotifications(Long userId, Pageable notificationPageable) {
        debug("[getAllNotifications] UserId: {}", userId);
        Optional<OrganizationUser> user = UserProvider.getFromContext();
        Set<Long> organizationIds = user.get().getOrganizationIds();
        return Flow.init()
                .thenSet(notificationPage, a -> repositories.unsecure.notification.findAll(userId, organizationIds, notificationPageable))
                .execute();
    }

    /**
     * Lists organization-scoped notifications with pagination.
     * <p>
     * Retrieves notifications for the authenticated user within a specific organization context.
     * Wraps the provided organizationId into {@code Collections.singleton(organizationId)} before
     * calling the repository API. Results are ordered by creation date descending.
     * </p>
     *
     * @param userId the user ID for filtering notifications, must match authenticated user
     * @param organizationId the organization ID to scope notification results, wrapped into singleton set
     * @param notificationPageable pagination parameters (page, size, sort) for notification results
     * @return {@link PageModelMap} containing 'notificationPage' attribute with paginated notification data
     * @see java.util.Collections#singleton(Object)
     */
    protected PageModelMap getAllNotifications(Long userId, Long organizationId, Pageable notificationPageable) {
        debug("[getAllNotifications] UserId: {} orgId: {}", userId, organizationId);
        return Flow.init()
                .thenSet(notificationPage, a -> repositories.unsecure.notification.findAll(userId, Collections.singleton(organizationId), notificationPageable))
                .execute();
    }

    /**
     * Marks specified notifications as read.
     * <p>
     * Accepts a comma-separated string of notification IDs and delegates to
     * {@code services.notification.markAsRead(unreadNotifications, userId)} inside a Flow.then step
     * to persist read-state changes. Updates readAt timestamp to current time for each notification.
     * </p>
     *
     * @param unreadNotifications comma-separated string of notification IDs to mark as read (e.g., "123,456,789")
     * @param userId the user ID owning the notifications, used for authorization
     * @return {@link PageModelMap} result of the Flow pipeline execution
     * @see com.openkoda.service.notification.NotificationService#markAsRead(String, Long)
     */
    protected PageModelMap markAsRead(String unreadNotifications, Long userId) {
        debug("[markAsRead] UserId: {}", userId);
        return Flow.init()
                .then(a -> services.notification.markAsRead(unreadNotifications, userId))
                .execute();
    }

    /**
     * Bulk marks all user notifications as read within organization context.
     * <p>
     * Marks all unread notifications for the authenticated user as read. When organizationId is provided,
     * only marks notifications within that organization context. Delegates to
     * {@code services.notification.markAllAsRead(userId, organizationId)} for batch update execution.
     * Sets readAt timestamp to current time for all matching unread notifications.
     * </p>
     *
     * @param userId the user ID owning the notifications, used for filtering
     * @param organizationId the organization ID to scope the bulk operation, may be null for all organizations
     * @return {@link PageModelMap} result of the Flow pipeline execution
     * @see com.openkoda.service.notification.NotificationService#markAllAsRead(Long, Long)
     */
    protected PageModelMap markAllAsRead(Long userId, Long organizationId) {
        debug("[markAllAsRead] UserId: {} OrgId: {}", userId, organizationId);
        return Flow.init()
                .then(a -> services.notification.markAllAsRead(userId, organizationId))
                .execute();
    }
}
