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
 * Marker interface for Data Transfer Objects (DTOs) that are associated with a specific user.
 * <p>
 * This interface provides a standard contract for DTOs that carry user context information.
 * The user ID is used for user-scoped operations, authorization checks, and audit trail generation
 * throughout the OpenKoda platform. By implementing this interface, DTOs signal that they
 * represent data owned by or related to a specific user in the system.
 * </p>
 * <p>
 * Typical implementing classes include user-related DTOs such as {@code RegisteredUserDto},
 * {@code NotificationDto}, and other DTOs that track user-specific data or actions.
 * </p>
 * <p>
 * This interface is commonly used by:
 * <ul>
 *   <li>Service layer components for user-scoped data retrieval and filtering</li>
 *   <li>Notification subsystems to determine notification recipients</li>
 *   <li>Authorization code to verify user access rights</li>
 *   <li>Audit logging to track user-initiated operations</li>
 * </ul>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public interface UserRelatedObject {

    /**
     * Returns the unique identifier of the user associated with this DTO.
     * <p>
     * The user ID is used to scope operations to a specific user context, enabling
     * user-specific data filtering, authorization checks, and audit trail generation.
     * When the user context is not available or not applicable, this method may return null.
     * </p>
     *
     * @return the user ID, or null when user context is not available
     */
    Long getUserId();

}
