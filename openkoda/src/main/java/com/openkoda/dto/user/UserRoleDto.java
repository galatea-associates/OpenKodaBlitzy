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

package com.openkoda.dto.user;

import com.openkoda.dto.CanonicalObject;
import com.openkoda.dto.OrganizationRelatedObject;

/**
 * Data Transfer Object representing a user-role-organization association.
 * <p>
 * This DTO captures the three-way relationship between a user, a role, and an organization,
 * implementing {@link CanonicalObject} for identity tracking and {@link OrganizationRelatedObject}
 * for organization-scoped operations. All fields are public Long wrapper types to support nullable
 * identifiers during serialization and deserialization.
 * </p>
 * <p>
 * The mutable design allows for flexible mapping between persistence entities and API representations.
 * Callers must ensure non-null field values before invoking {@link #notificationMessage()} to avoid
 * NullPointerException during string formatting.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CanonicalObject
 * @see OrganizationRelatedObject
 */
public class UserRoleDto implements CanonicalObject, OrganizationRelatedObject {
    /**
     * Primary identifier for this user-role-organization association.
     * Nullable to support newly created associations before persistence.
     */
    public Long id;
    
    /**
     * User identifier in the association.
     * Nullable to support partial data scenarios during deserialization.
     */
    public Long userId;
    
    /**
     * Role identifier in the association.
     * Nullable to support partial data scenarios during deserialization.
     */
    public Long roleId;
    
    /**
     * Organization identifier defining the scope of this user-role association.
     * Nullable to support global role assignments without organization context.
     */
    public Long organizationId;

    /**
     * Returns the association identifier.
     *
     * @return the primary identifier for this user-role-organization association, may be null
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the association identifier.
     *
     * @param id the primary identifier to set, may be null
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the user identifier.
     *
     * @return the user identifier in this association, may be null
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Sets the user identifier.
     *
     * @param userId the user identifier to set, may be null
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Returns the role identifier.
     *
     * @return the role identifier in this association, may be null
     */
    public Long getRoleId() {
        return roleId;
    }

    /**
     * Sets the role identifier.
     *
     * @param roleId the role identifier to set, may be null
     */
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    /**
     * Returns the organization identifier defining the scope of this association.
     *
     * @return the organization identifier, may be null for global role assignments
     */
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the organization identifier.
     *
     * @param organizationId the organization identifier to set, may be null
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Returns a formatted notification message describing this user-role-organization association.
     * <p>
     * Generates a human-readable message in the format:
     * "User: %d was granted with role: %d, within organization %d."
     * </p>
     * <p>
     * <b>CRITICAL:</b> This method uses {@code %d} integer format specifiers with nullable Long wrapper
     * fields. If any of {@link #userId}, {@link #roleId}, or {@link #organizationId} are null, calling
     * this method will cause a NullPointerException or unexpected formatting behavior. Callers must ensure
     * all three identifiers are non-null before invoking this method.
     * </p>
     *
     * @return formatted notification message describing the user-role grant operation
     * @throws NullPointerException if userId, roleId, or organizationId are null during string formatting
     */
    @Override
    public String notificationMessage() {
        return String.format("User: %d was granted with role: %d, within organization %d.", userId, roleId, organizationId);
    }
}
