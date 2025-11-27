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

package com.openkoda.form;

import jakarta.validation.constraints.NotBlank;

/**
 * Request-scoped form for password change operations.
 * <p>
 * This form provides a lightweight DTO for password change requests, using Jakarta Bean Validation
 * for declarative constraint enforcement. The form binds the target user identifier and new password
 * for validation before applying password changes to the user entity.

 * <p>
 * The form uses the {@code @NotBlank} constraint on the password field to ensure that password
 * changes always provide a non-empty value. The userId field identifies the target user whose
 * password is being changed.

 * <p>
 * Usage example:
 * <pre>
 * PasswordChangeForm form = new PasswordChangeForm(userId);
 * form.setPassword("newPassword123");
 * // Validate using Spring's BindingResult and apply changes
 * </pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2018-12-14
 * @see jakarta.validation.constraints.NotBlank
 */
public class PasswordChangeForm {

    /**
     * The identifier of the target user whose password is being changed.
     * <p>
     * This field holds the primary key of the User entity. It is populated either
     * through the parameterized constructor or via setter method before validation.

     */
    private long userId;
    
    /**
     * The new password to set for the target user.
     * <p>
     * This field is validated using Jakarta Bean Validation's {@code @NotBlank} constraint,
     * which ensures the password is not null, empty, or whitespace-only. The validation
     * message "Password must not be blank" will be recorded in the BindingResult if
     * validation fails.

     *
     * @see jakarta.validation.constraints.NotBlank
     */
    @NotBlank(message = "Password must not be blank")
    private String password;

    /**
     * No-argument constructor for PasswordChangeForm.
     * <p>
     * Creates a new instance with default field values (userId=0, password=null).
     * The userId and password must be set via setter methods before validation.

     */
    public PasswordChangeForm() {
    }

    /**
     * Constructs a PasswordChangeForm with the specified target user identifier.
     * <p>
     * Creates a new instance pre-populated with the userId of the target user
     * whose password will be changed. The password field must still be set via
     * setter method before validation.

     *
     * @param userId the identifier of the target user whose password is being changed
     */
    public PasswordChangeForm(long userId) {
        this.userId = userId;
    }

    /**
     * Returns the identifier of the target user whose password is being changed.
     *
     * @return the target user's identifier (primary key)
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Sets the identifier of the target user whose password is being changed.
     *
     * @param userId the target user's identifier (primary key)
     */
    public void setUserId(long userId) {
        this.userId = userId;
    }

    /**
     * Returns the new password to be set for the target user.
     * <p>
     * This value is validated by the {@code @NotBlank} constraint to ensure
     * it is not null, empty, or whitespace-only before applying the password change.

     *
     * @return the new password, or null if not yet set
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the new password for the target user.
     * <p>
     * The provided password will be validated using Jakarta Bean Validation's
     * {@code @NotBlank} constraint when the form is validated. The password
     * must be non-null, non-empty, and contain at least one non-whitespace character.

     *
     * @param password the new password to set for the target user
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
