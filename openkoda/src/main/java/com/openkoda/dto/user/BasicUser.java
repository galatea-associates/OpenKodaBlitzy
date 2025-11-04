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
import org.apache.commons.lang3.StringUtils;

/**
 * Minimal, mutable data transfer object containing four public fields for basic user information.
 * <p>
 * This DTO implements {@link CanonicalObject} to provide notification message generation
 * through the {@link #notificationMessage()} method. Public fields (id, firstName, lastName, email)
 * are exposed directly for serialization compatibility with mapping frameworks and legacy code.
 * 
 * <p>
 * This class serves as a parent class for {@code EditUserDto} and {@code InviteUserDto},
 * providing common user properties without validation or normalization logic.
 * 
 * <p>
 * <strong>Important considerations:</strong>
 * 
 * <ul>
 *   <li>TODO: Rename to BasicUserDto per Rule 5.2 (DTO class names must end with "Dto")</li>
 *   <li>Null field values can propagate to output - {@code notificationMessage()} may render literal 'null' tokens when email is null</li>
 *   <li>No validation or null-safety guarantees - callers must handle null values appropriately</li>
 *   <li>Mutable design enables direct field assignment for mapping and serialization</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CanonicalObject
 * @see com.openkoda.dto.user.EditUserDto
 * @see com.openkoda.dto.user.InviteUserDto
 */
//TODO Rule 5.2: DTO class name must end with "Dto"
public class BasicUser implements CanonicalObject {

    /**
     * User identifier, typically corresponding to the primary key in the user table.
     * <p>
     * This field is nullable and may be null for new users not yet persisted to the database.
     * 
     */
    public Long id;
    
    /**
     * User's first name.
     * <p>
     * This field is nullable and may be null or empty. No normalization or validation
     * is applied to this field.
     * 
     */
    public String firstName;
    
    /**
     * User's last name.
     * <p>
     * This field is nullable and may be null or empty. No normalization or validation
     * is applied to this field.
     * 
     */
    public String lastName;
    
    /**
     * User's email address.
     * <p>
     * This field is nullable and may be null or empty. No email format validation
     * is applied to this field. Used as fallback identifier in {@link #notificationMessage()}
     * when name fields are not available.
     * 
     */
    public String email;

    /**
     * Returns the user's first name.
     *
     * @return the first name, or null if not set
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name.
     * <p>
     * No validation or normalization is performed on the provided value.
     * 
     *
     * @param firstName the first name to set, may be null or empty
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the user's last name.
     *
     * @return the last name, or null if not set
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name.
     * <p>
     * No validation or normalization is performed on the provided value.
     * 
     *
     * @param lastName the last name to set, may be null or empty
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the user's email address.
     *
     * @return the email address, or null if not set
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     * <p>
     * No email format validation is performed on the provided value.
     * 
     *
     * @param email the email address to set, may be null or empty
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the user identifier.
     *
     * @return the user ID, or null if not set
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the user identifier.
     * <p>
     * Typically corresponds to the primary key value from the user table.
     * 
     *
     * @param id the user ID to set, may be null for new unpersisted users
     */
    public void setId(Long id) {
        this.id = id;
    }


    /**
     * Returns a formatted user summary for notification messages.
     * <p>
     * This method uses {@link StringUtils#isNotEmpty(CharSequence)} for conditional formatting
     * to produce user-friendly notification text:
     * 
     * <ul>
     *   <li>If both firstName and lastName are present: {@code "User firstName lastName, email"}</li>
     *   <li>Otherwise: {@code "User email"} (fallback format)</li>
     * </ul>
     * <p>
     * <strong>Null propagation risk:</strong> If the email field is null, the output will contain
     * the literal string "null" (e.g., {@code "User null"}). Callers should ensure email is set
     * before invoking this method for display purposes.
     * 
     *
     * @return formatted user summary string for notifications
     * @see CanonicalObject#notificationMessage()
     */
    @Override
    public String notificationMessage() {
        return StringUtils.isNotEmpty(firstName) && StringUtils.isNotEmpty(lastName) ?
                String.format("User %s %s, %s", firstName, lastName, email) : String.format("User %s", email);
    }
}