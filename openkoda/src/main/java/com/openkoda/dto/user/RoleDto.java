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

import java.util.List;

/**
 * Mutable Data Transfer Object for role information.
 * <p>
 * This DTO encapsulates role data for transfer across application layers including
 * controllers, services, and serializers. It implements {@link CanonicalObject} to
 * provide formatted notification message generation.
 * </p>
 * <p>
 * The class uses public fields for direct field access, which facilitates mapping
 * and serialization by frameworks such as Jackson. No validation or concurrency
 * controls are enforced by this DTO - callers are responsible for data integrity.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * RoleDto dto = new RoleDto();
 * dto.name = "Administrator";
 * dto.type = "GlobalRole";
 * dto.privileges = Arrays.asList("canAccessSystem", "canManageUsers");
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CanonicalObject
 */
public class RoleDto implements CanonicalObject {

    /**
     * Role name identifier.
     * <p>
     * This field stores the unique name of the role such as "Administrator",
     * "User", or custom role names. The value is used for display purposes
     * and role identification throughout the application.
     * </p>
     */
    public String name;

    /**
     * Role type classification.
     * <p>
     * Indicates the scope and type of the role, such as "GlobalRole",
     * "OrganizationRole", or "GlobalOrganizationRole". This classification
     * determines the role's applicability and privilege scope within the system.
     * </p>
     */
    public String type;

    /**
     * Collection of privilege names associated with this role.
     * <p>
     * Contains string identifiers of privileges granted to this role, such as
     * "canAccessSystem", "canManageUsers", etc. The collection is assigned by
     * reference and its lifecycle is managed by callers. Modifications to the
     * returned list affect the DTO's internal state.
     * </p>
     */
    public List<String> privileges;

    /**
     * Returns the role name identifier.
     * <p>
     * Retrieves the name of this role, such as "Administrator" or custom role names.
     * The returned value may be {@code null} if not yet initialized.
     * </p>
     *
     * @return the role name identifier, or {@code null} if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the role name identifier.
     * <p>
     * Assigns the name of this role. No validation is performed on the provided
     * value - callers are responsible for ensuring data integrity.
     * </p>
     *
     * @param name the role name identifier to set, may be {@code null}
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the role type classification.
     * <p>
     * Retrieves the type classification indicating the scope of this role,
     * such as "GlobalRole", "OrganizationRole", or "GlobalOrganizationRole".
     * The returned value may be {@code null} if not yet initialized.
     * </p>
     *
     * @return the role type classification, or {@code null} if not set
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the role type classification.
     * <p>
     * Assigns the type classification for this role. Valid values typically
     * include "GlobalRole", "OrganizationRole", or "GlobalOrganizationRole",
     * but no validation is enforced at this level.
     * </p>
     *
     * @param type the role type classification to set, may be {@code null}
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the list of privilege names associated with this role.
     * <p>
     * Retrieves the collection of privilege identifiers granted to this role.
     * The returned list is the actual internal collection, not a copy - modifications
     * to the list affect this DTO's state. The collection lifecycle is managed by
     * callers. May return {@code null} if privileges have not been initialized.
     * </p>
     *
     * @return the list of privilege names, or {@code null} if not set
     */
    public List<String> getPrivileges() {
        return privileges;
    }

    /**
     * Sets the privileges collection by reference.
     * <p>
     * Assigns the privilege names collection to this role. The provided list is
     * stored by reference, not copied - subsequent modifications to the list will
     * be reflected in this DTO. Callers manage the collection's lifecycle and
     * are responsible for ensuring data integrity.
     * </p>
     *
     * @param privileges the list of privilege names to set, may be {@code null}
     */
    public void setPrivileges(List<String> privileges) {
        this.privileges = privileges;
    }

    /**
     * Returns a formatted summary message for notifications.
     * <p>
     * Generates a human-readable notification message containing the role name
     * and scope type. The format is: "Role: [name].Scope: [type]." This method
     * is defined by the {@link CanonicalObject} interface for standardized
     * notification message generation.
     * </p>
     * <p>
     * Example output: "Role: Administrator.Scope: GlobalRole."
     * </p>
     *
     * @return formatted notification message using role name and type
     */
    @Override
    public String notificationMessage() {
        return String.format("Role: %s.Scope: %s.", name, type);
    }
}