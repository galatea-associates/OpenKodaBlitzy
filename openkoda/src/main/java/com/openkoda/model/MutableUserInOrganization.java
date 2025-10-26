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

package com.openkoda.model;

/**
 * Simple mutable DTO holding userId and organizationId pair for representing user-organization association state with presence check helper.
 * <p>
 * Lightweight data transfer object (not a JPA entity) for passing user-organization relationship information between layers.
 * Contains userId and organizationId as mutable Long fields with public accessors. Provides isPresent() helper method to check
 * if both IDs are non-null, indicating valid association. Used in service layer, controllers, and forms for user-organization
 * context passing without loading full User or UserRole entities.
 * </p>
 * <p>
 * Design: Plain Java Bean (POJO) with no JPA annotations. Mutable for form binding and data transfer.
 * Simpler alternative to full UserRole entity when only IDs needed.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see User
 * @see Organization
 * @see UserRole
 */
public class MutableUserInOrganization {

    /**
     * User entity ID. Nullable. Null indicates no user associated.
     */
    private Long userId;
    
    /**
     * Organization entity ID. Nullable. Null indicates no organization associated.
     */
    private Long organizationId;

    /**
     * Returns the user entity ID.
     * <p>
     * Gets the ID of the User entity associated with this user-organization relationship.
     * May be null if no user is currently associated.
     * </p>
     *
     * @return the user ID, or null if no user associated
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Sets the user entity ID.
     * <p>
     * Assigns the ID of the User entity for this user-organization relationship.
     * Set to null to indicate no user association.
     * </p>
     *
     * @param userId the user ID to set, or null to clear user association
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Returns the organization entity ID.
     * <p>
     * Gets the ID of the Organization entity associated with this user-organization relationship.
     * May be null if no organization is currently associated.
     * </p>
     *
     * @return the organization ID, or null if no organization associated
     */
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the organization entity ID.
     * <p>
     * Assigns the ID of the Organization entity for this user-organization relationship.
     * Set to null to indicate no organization association.
     * </p>
     *
     * @param organizationId the organization ID to set, or null to clear organization association
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "MutableUserInOrganization{" +
                "userId=" + userId +
                ", organizationId=" + organizationId +
                '}';
    }

    /**
     * Returns true if both userId and organizationId are non-null, indicating valid user-organization association.
     * <p>
     * Helper method to check if this DTO represents a complete user-organization relationship.
     * Returns true when both IDs are present (non-null), false if either ID is null.
     * Useful for validation and conditional logic when processing user-organization contexts.
     * </p>
     *
     * @return true if both userId and organizationId are non-null, false otherwise
     */
    public boolean isPresent() {
        return userId != null && organizationId != null;
    }
}
