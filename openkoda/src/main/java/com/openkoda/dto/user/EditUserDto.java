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

/**
 * Data transfer object for user edit and update operations.
 * <p>
 * This class extends {@link BasicUser} and adds three public fields specific to user edit operations:
 * enabled status, language preference, and global role assignment. It inherits id, firstName, lastName,
 * and email fields from BasicUser.
 * </p>
 * <p>
 * EditUserDto is a transient mutable payload object used for mapping user data in edit/update operations
 * within controllers and services. It contains no business logic or validation - the class serves purely
 * as a data container for transferring user edit information between application layers.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * EditUserDto dto = new EditUserDto();
 * dto.setEnabled(true);
 * dto.setLanguage("en");
 * dto.setGlobalRoleName("ROLE_USER");
 * }</pre>
 * </p>
 * <p>
 * <b>Important for maintainers:</b> Field names and accessor method names must remain stable for mapping
 * compatibility with controllers, services, and form bindings. Changes to field names or accessor signatures
 * are breaking changes for mappers and controllers that depend on this DTO structure.
 * </p>
 *
 * @see BasicUser
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class EditUserDto extends BasicUser {

    /**
     * User account enabled status.
     * <p>
     * Indicates whether the user account is active and can authenticate. A value of {@code true} means
     * the account is enabled and the user can log in. A value of {@code false} means the account is
     * disabled and authentication attempts will be rejected. Null values may be treated as disabled
     * depending on the context.
     * </p>
     */
    public Boolean enabled;

    /**
     * Returns the user account enabled status.
     * <p>
     * This method provides access to the enabled flag that controls whether the user account is active.
     * </p>
     *
     * @return the user account enabled status; {@code true} if enabled, {@code false} if disabled,
     *         or {@code null} if not specified
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets the user account enabled status.
     * <p>
     * Use this method to enable or disable a user account during edit operations.
     * </p>
     *
     * @param enabled the enabled status to set; {@code true} to enable the account, {@code false} to
     *                disable it, or {@code null} to leave unspecified
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * User's preferred language or locale setting.
     * <p>
     * Stores the language preference for the user, typically as a locale code such as "en" for English
     * or "pl" for Polish. This setting controls the language used for the user interface and
     * communications. Null values indicate no explicit language preference has been set.
     * </p>
     */
    public String language;

    /**
     * Returns the user's preferred language setting.
     * <p>
     * Retrieves the language/locale code representing the user's interface language preference.
     * </p>
     *
     * @return the language code (e.g., "en", "pl"), or {@code null} if no preference is set
     */
    public String getLanguage() { return language; }

    /**
     * Sets the user's preferred language setting.
     * <p>
     * Assigns a language/locale code for the user's interface preference during edit operations.
     * </p>
     *
     * @param language the language code to set (e.g., "en", "pl"), or {@code null} to clear the preference
     */
    public void setLanguage(String language) { this.language = language; }

    /**
     * Name of the global role assigned to the user.
     * <p>
     * Stores the name of the global (non-organization-specific) role that defines the user's system-wide
     * permissions. Examples include "ROLE_USER", "ROLE_ADMIN", or "ROLE_GLOBAL_ADMIN". This field is used
     * during user edit operations to assign or modify the user's global role membership. Null values
     * indicate no global role has been assigned.
     * </p>
     */
    public String globalRoleName;

    /**
     * Returns the global role name assigned to the user.
     * <p>
     * Provides access to the global role name that determines the user's system-wide permissions.
     * </p>
     *
     * @return the global role name (e.g., "ROLE_USER", "ROLE_ADMIN"), or {@code null} if no global
     *         role is assigned
     */
    public String getGlobalRoleName() {
        return globalRoleName;
    }

    /**
     * Sets the global role name for the user.
     * <p>
     * Assigns a global role to the user during edit operations, controlling system-wide access permissions.
     * </p>
     *
     * @param globalRoleName the global role name to set (e.g., "ROLE_USER", "ROLE_ADMIN"), or {@code null}
     *                       to remove the global role assignment
     */
    public void setGlobalRoleName(String globalRoleName) {
        this.globalRoleName = globalRoleName;
    }
}