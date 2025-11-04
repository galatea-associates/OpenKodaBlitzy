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

import com.openkoda.core.security.HasSecurityRules;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.openkoda.controller.common.URLConstants._HTML;

/**
 * Concrete HTML notification management controller with user interface.
 * <p>
 * Provides notification center UI with unread badge count, notification list with read/unread styling,
 * mark-as-read buttons, and delete actions. Routes under /notifications base path. Extends
 * {@link AbstractNotificationController} with HTML response handling. Includes WebSocket integration
 * for real-time notification delivery.
 * 
 * <p>
 * Handler methods are stateless and execute synchronously on the request thread. Method-level authorization
 * uses {@code @PreAuthorize(CHECK_IS_THIS_USERID)} from {@link HasSecurityRules} to verify that the
 * authenticated user matches the userId path variable.
 * 
 * <p>
 * Example notification flow with WebSocket:
 * <pre>{@code
 * // Server-sent notification
 * services.notification.send(userId, "New message", NotificationType.INFO);
 * // → Persists Notification entity
 * // → Publishes WebSocket message to /topic/notifications/{userId}
 * // → Client receives real-time update and displays toast
 * }</pre>
 *
 * <b>Endpoints:</b>
 * <ul>
 *   <li>GET /notifications/{userId}/all → List all notifications with pagination</li>
 *   <li>GET /organization/{orgId}/notifications/{userId}/all → List organization-scoped notifications</li>
 *   <li>POST /notifications/{userId}/mark-read → Mark visible notifications as read</li>
 *   <li>POST /notifications/{userId}/all/mark-read → Bulk mark all notifications as read</li>
 * </ul>
 *
 * <b>Notification Types:</b>
 * <ul>
 *   <li>INFO: General information (blue icon)</li>
 *   <li>WARNING: Warnings (yellow icon)</li>
 *   <li>ERROR: Errors (red icon)</li>
 *   <li>SUCCESS: Success messages (green icon)</li>
 * </ul>
 *
 * <b>Unread Badge Count:</b>
 * All views include unread count via {@code @ModelAttribute}, displayed in navigation bar notification icon.
 * Updates in real-time via WebSocket.
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractNotificationController
 * @see com.openkoda.model.notification.Notification
 * @see com.openkoda.service.notification.NotificationService
 */
@Controller
@RequestMapping(_HTML)
public class NotificationController extends AbstractNotificationController implements HasSecurityRules {

    /**
     * Lists all user notifications with pagination.
     * <p>
     * GET endpoint retrieving both read and unread notifications for the authenticated user across
     * all their organizations. Results are ordered by creation date descending. The Pageable parameter
     * is selected using named bean qualifier 'notification', which must exist in Spring configuration.
     * 
     *
     * @param userId the user ID from path variable, must match authenticated user per security constraint
     * @param notificationPageable pagination parameters (page, size, sort) injected via @Qualifier("notification")
     * @return ModelAndView with 'notification-all' template and model containing 'notificationPage' attribute
     * @throws java.util.NoSuchElementException if security context is empty (user not authenticated)
     * @throws org.springframework.security.access.AccessDeniedException if userId does not match authenticated user
     * @see AbstractNotificationController#getAllNotifications(Long, Pageable)
     */
    @PreAuthorize(CHECK_IS_THIS_USERID)
    @RequestMapping(_NOTIFICATION + _USERID + _ALL)
    public Object openAllNotifications(@PathVariable(USERID) Long userId, @Qualifier("notification") Pageable notificationPageable) {
        debug("[openAllNotifications] UserId: {} ", userId);
        return getAllNotifications(userId, notificationPageable).mav("notification-all");
    }

    /**
     * Lists organization-scoped notifications with pagination.
     * <p>
     * GET endpoint retrieving both read and unread notifications for the authenticated user within
     * a specific organization context. Filters results to the provided organization ID. Results are
     * ordered by creation date descending.
     * 
     *
     * @param organizationId the organization ID from path variable to scope notification results
     * @param userId the user ID from path variable, must match authenticated user per security constraint
     * @param notificationPageable pagination parameters (page, size, sort) injected via @Qualifier("notification")
     * @return ModelAndView with 'notification-all' template and model containing 'notificationPage' attribute
     * @throws org.springframework.security.access.AccessDeniedException if userId does not match authenticated user
     * @see AbstractNotificationController#getAllNotifications(Long, Long, Pageable)
     */
    @PreAuthorize(CHECK_IS_THIS_USERID)
    @RequestMapping(_ORGANIZATION_ORGANIZATIONID + _NOTIFICATION + _USERID + _ALL)
    public Object openAllNotifications(@PathVariable(ORGANIZATIONID) Long organizationId, @PathVariable(USERID) Long userId, @Qualifier("notification") Pageable notificationPageable) {
        debug("[openAllNotifications] UserId: {} OrgId: {}", userId, organizationId);
        return getAllNotifications(userId, organizationId, notificationPageable).mav("notification-all");
    }

    /**
     * Marks visible dropdown notifications as read.
     * <p>
     * POST endpoint that accepts a comma-separated string of notification IDs currently visible
     * in the user's notification dropdown. Delegates to {@link AbstractNotificationController#markAsRead}
     * which persists read state via NotificationService. Both organization-scoped and non-organization
     * paths are supported.
     * 
     *
     * @param organizationId optional organization ID from path variable, may be null if using non-organization path
     * @param userId the user ID from path variable, must match authenticated user per security constraint
     * @param unreadNotifications comma-separated string of notification IDs to mark as read (e.g., "123,456,789")
     * @return ResponseEntity with HTTP 200 OK status and success message body
     * @throws org.springframework.security.access.AccessDeniedException if userId does not match authenticated user
     * @see AbstractNotificationController#markAsRead(String, Long)
     * @see com.openkoda.service.notification.NotificationService#markAsRead(String, Long)
     */
    @PreAuthorize(CHECK_IS_THIS_USERID)
    @PostMapping(value = {_ORGANIZATION_ORGANIZATIONID + _NOTIFICATION + _USERID + _MARK_READ, _NOTIFICATION + _USERID + _MARK_READ})
    public Object markNotificationAsRead(@PathVariable(name = ORGANIZATIONID, required = false) Long organizationId, @PathVariable(USERID) Long userId, @RequestParam("unreadNotifications") String unreadNotifications) {
        debug("[markNotificationAsRead] UserId: {} OrgId: {}", userId, organizationId);
        markAsRead(unreadNotifications, userId);
        return ResponseEntity.status(HttpStatus.OK).body("Successfully marked notifications as read!");
    }

    /**
     * Bulk marks all user notifications as read.
     * <p>
     * POST endpoint that marks all unread notifications for the authenticated user as read. When
     * organizationId is provided, only marks notifications within that organization context.
     * Updates readAt timestamp to current time via NotificationService. Supports both organization-scoped
     * and non-organization paths.
     * 
     *
     * @param organizationId optional organization ID from path variable, may be null if using non-organization path
     * @param userId the user ID from path variable, must match authenticated user per security constraint
     * @return ResponseEntity with HTTP 200 OK status and success message body
     * @throws org.springframework.security.access.AccessDeniedException if userId does not match authenticated user
     * @see AbstractNotificationController#markAllAsRead(Long, Long)
     * @see com.openkoda.service.notification.NotificationService#markAllAsRead(Long, Long)
     */
    @PreAuthorize(CHECK_IS_THIS_USERID)
    @PostMapping(value = {_ORGANIZATION_ORGANIZATIONID + _NOTIFICATION + _USERID + _ALL + _MARK_READ, _NOTIFICATION + _USERID + _ALL + _MARK_READ})
    public Object markReadAllNotifications(@PathVariable(name = ORGANIZATIONID, required = false) Long organizationId, @PathVariable(USERID) Long userId) {
        debug("[markReadAllNotifications] UserId: {} OrgId: {}", userId, organizationId);
        markAllAsRead(userId, organizationId);
        return ResponseEntity.status(HttpStatus.OK).body("Successfully marked all user's notifications as read!");
    }
}
