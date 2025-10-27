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

/**
 * Canonical contract for data transfer objects requiring human-readable string representation.
 * <p>
 * This interface defines a standard protocol for DTOs that need to provide descriptive messages
 * suitable for presentation to end-users, logging subsystems, and notification frameworks.
 * Implementing classes ensure polymorphic handling of diverse object types throughout the
 * application's notification and audit trails.
 * </p>
 * <p>
 * The notification/logging subsystems utilize this interface to obtain consistent, user-friendly
 * descriptions of domain objects without coupling to specific entity or DTO implementations.
 * This enables flexible message generation for audit logs, user notifications, system events,
 * and administrative interfaces.
 * </p>
 * <p>
 * Typical implementing classes include:
 * <ul>
 *   <li>File-related DTOs in {@code com.openkoda.dto.file} package</li>
 *   <li>Notification DTOs in {@code com.openkoda.dto} package</li>
 *   <li>System DTOs in {@code com.openkoda.dto.system} package</li>
 *   <li>User DTOs in {@code com.openkoda.dto.user} package</li>
 *   <li>Domain entities requiring notification support</li>
 * </ul>
 * </p>
 * <p>
 * Usage example in controllers and services:
 * <pre>{@code
 * CanonicalObject dto = fileService.processUpload(file);
 * notificationService.sendMessage(dto.notificationMessage());
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.dto.file
 * @see com.openkoda.service.notification
 */
public interface CanonicalObject {
    /**
     * Generates a descriptive message suitable for presentation to end-users.
     * <p>
     * Returns a human-readable string representation of this object that can be displayed
     * in user interfaces, notification messages, audit logs, or system alerts. The message
     * should be concise yet informative, providing essential context about the object without
     * exposing internal implementation details or sensitive data.
     * </p>
     * <p>
     * Expected format characteristics:
     * <ul>
     *   <li>User-friendly language appropriate for non-technical audiences</li>
     *   <li>Includes identifying information (name, ID, or key attributes)</li>
     *   <li>Omits sensitive data (passwords, tokens, internal keys)</li>
     *   <li>Concise length suitable for notification displays (typically under 200 characters)</li>
     *   <li>Localization-ready (implementations may use message bundles)</li>
     * </ul>
     * </p>
     * <p>
     * Usage context: This method is invoked by notification services, audit interceptors,
     * logging components, and UI controllers when generating user-facing messages about
     * domain operations, file uploads, user actions, or system events.
     * </p>
     *
     * @return descriptive message appropriate for end-user presentation, never {@code null}
     */
    String notificationMessage();

}
