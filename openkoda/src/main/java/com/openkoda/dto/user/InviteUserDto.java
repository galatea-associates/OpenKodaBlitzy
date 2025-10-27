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
 * Data Transfer Object for user invitation operations.
 * <p>
 * This class extends {@link BasicUser} and adds a single public field for specifying
 * the intended role for the invited user. It is designed to carry invitation payloads
 * with identity information (inherited from BasicUser: id, firstName, lastName, email)
 * and the target role information.
 * </p>
 * <p>
 * The class follows a mutable, validator-free design for maximum compatibility with
 * controllers, services, and mapping utilities. No validation or normalization is
 * performed - callers are responsible for data integrity.
 * </p>
 * <p>
 * <b>Important:</b> Changes to field names or accessor signatures are breaking changes
 * for mappers and controllers that depend on this DTO structure.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * InviteUserDto invitation = new InviteUserDto();
 * invitation.setEmail("user@example.com");
 * invitation.setRoleName("DEVELOPER");
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see BasicUser
 */
public class InviteUserDto extends BasicUser {

    /**
     * Name of the role to be assigned to the invited user.
     * <p>
     * This field specifies the intended role for the user being invited.
     * It is nullable and no validation is enforced - callers must ensure
     * the role name is valid within the application's role system.
     * </p>
     */
    public String roleName;

    /**
     * Returns the role name for the invitation.
     * <p>
     * This method retrieves the name of the role that will be assigned
     * to the invited user during the invitation workflow.
     * </p>
     *
     * @return the role name to be assigned to the invited user, may be null
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * Sets the role name for the invitation.
     * <p>
     * This method specifies the name of the role that will be assigned
     * to the invited user. No validation or normalization is performed -
     * callers must ensure the role name is valid within the application's
     * role system.
     * </p>
     *
     * @param roleName the role name to be assigned to the invited user, may be null
     */
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}