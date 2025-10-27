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
import jakarta.validation.constraints.Pattern;

/**
 * Request-scoped form object for new user registration.
 * <p>
 * This DTO serves as the data transfer object for user registration requests,
 * capturing login credentials and optional profile information. The form employs
 * Jakarta Bean Validation with declarative constraints to ensure data integrity
 * before processing. Email validation uses RFC-style pattern matching, while
 * password fields enforce non-empty requirements.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * RegisterUserForm form = new RegisterUserForm();
 * form.setLogin("user@example.com");
 * form.setPassword("securePassword123");
 * }</pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class RegisterUserForm {

    /**
     * User login identifier, validated as RFC-style email address.
     * <p>
     * Validation constraints:
     * <ul>
     * <li>{@code @NotBlank}: Must not be null, empty, or whitespace-only</li>
     * <li>{@code @Pattern}: Must match RFC-style email format with regex
     *     {@code ^([a-zA-Z0-9_\.\-])+\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]+)$}</li>
     * </ul>
     * Valid example: {@code user@example.com}
     * </p>
     */
    @NotBlank(message = "Email must not be blank")
    @Pattern(
            regexp = "^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]+)$",
            message = "Please provide a valid email address"
    )
    private String login;
    
    /**
     * User password, required for authentication.
     * <p>
     * Validation constraints:
     * <ul>
     * <li>{@code @NotBlank}: Must not be null, empty, or whitespace-only</li>
     * </ul>
     * Note: Additional password strength requirements may be enforced at the service layer.
     * </p>
     */
    @NotBlank(message = "Password must not be blank")
    private String password;
    
    /**
     * Optional user first name for profile display.
     * <p>
     * No validation constraints applied. May be null or empty.
     * </p>
     */
    private String firstName;
    
    /**
     * Optional user last name for profile display.
     * <p>
     * No validation constraints applied. May be null or empty.
     * </p>
     */
    private String lastName;
    
    /**
     * Optional website URL associated with the user profile.
     * <p>
     * No validation constraints applied. May be null or empty.
     * Note: Field name chosen for generic applicability across projects
     * where users register with website URLs.
     * </p>
     * 
     * @see #getNickname()
     */
    //TODO: change to 'websiteUrl' - it is more generic for projects where users register with a website url.
    private String websiteUrl;
    
    /**
     * Optional user nickname or display name.
     * <p>
     * No validation constraints applied. May be null or empty.
     * Used for alternative identification in the user interface.
     * </p>
     */
    private String nickname;
    
    /**
     * Static reCAPTCHA site key configuration for bot prevention.
     * <p>
     * This field holds the Google reCAPTCHA site key used for
     * client-side verification during user registration. The value
     * is typically configured at application startup and shared
     * across all registration form instances.
     * </p>
     */
    public static String siteKey;

    /**
     * Returns the user login email address.
     * <p>
     * This value is validated against RFC-style email format constraints
     * and must not be blank. The returned value may be null if not yet set.
     * </p>
     *
     * @return the user login email, or {@code null} if not set
     * @see #setLogin(String)
     */
    public String getLogin() {
        return login;
    }

    /**
     * Sets the user login email address.
     * <p>
     * The provided value will be validated during form submission using
     * {@code @NotBlank} and {@code @Pattern} constraints. Must match the
     * email format: {@code ^([a-zA-Z0-9_\.\-])+\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]+)$}
     * </p>
     *
     * @param login the user login email address to set
     * @see #getLogin()
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Returns the user password for authentication.
     * <p>
     * This value is validated as non-blank during form submission.
     * The returned value may be null if not yet set.
     * </p>
     *
     * @return the user password, or {@code null} if not set
     * @see #setPassword(String)
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the user password for authentication.
     * <p>
     * The provided value will be validated during form submission using
     * {@code @NotBlank} constraint. Must not be null, empty, or contain
     * only whitespace characters. Additional password strength requirements
     * may be enforced at the service layer.
     * </p>
     *
     * @param password the user password to set
     * @see #getPassword()
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the user's first name for profile display.
     * <p>
     * This is an optional field with no validation constraints.
     * May be null or empty if the user chooses not to provide it.
     * </p>
     *
     * @return the user's first name, or {@code null} if not set
     * @see #setFirstName(String)
     * @see #getLastName()
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name for profile display.
     * <p>
     * This is an optional field. The value may be null, empty, or
     * any string value without validation constraints.
     * </p>
     *
     * @param firstName the user's first name to set, may be {@code null}
     * @see #getFirstName()
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the user's last name for profile display.
     * <p>
     * This is an optional field with no validation constraints.
     * May be null or empty if the user chooses not to provide it.
     * </p>
     *
     * @return the user's last name, or {@code null} if not set
     * @see #setLastName(String)
     * @see #getFirstName()
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name for profile display.
     * <p>
     * This is an optional field. The value may be null, empty, or
     * any string value without validation constraints.
     * </p>
     *
     * @param lastName the user's last name to set, may be {@code null}
     * @see #getLastName()
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the website URL associated with the user profile.
     * <p>
     * This is an optional field with no validation constraints.
     * May be null or empty if the user does not provide a website URL.
     * The field is named generically to support various project types
     * where users register with website URLs.
     * </p>
     *
     * @return the user's website URL, or {@code null} if not set
     * @see #setWebsiteUrl(String)
     */
    public String getWebsiteUrl() {
        return websiteUrl;
    }

    /**
     * Sets the website URL associated with the user profile.
     * <p>
     * This is an optional field. The value may be null, empty, or
     * any string value without validation constraints. URL format
     * validation may be applied at the service layer if needed.
     * </p>
     *
     * @param websiteUrl the user's website URL to set, may be {@code null}
     * @see #getWebsiteUrl()
     */
    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    /**
     * Returns the user's nickname or display name.
     * <p>
     * This is an optional field with no validation constraints.
     * May be null or empty if the user does not provide a nickname.
     * Used for alternative identification in the user interface.
     * </p>
     *
     * @return the user's nickname, or {@code null} if not set
     * @see #setNickname(String)
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Sets the user's nickname or display name.
     * <p>
     * This is an optional field. The value may be null, empty, or
     * any string value without validation constraints. Provides an
     * alternative identifier for display purposes in the user interface.
     * </p>
     *
     * @param nickname the user's nickname to set, may be {@code null}
     * @see #getNickname()
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
