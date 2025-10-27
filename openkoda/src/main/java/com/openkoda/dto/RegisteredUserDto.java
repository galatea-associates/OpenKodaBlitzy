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

import com.openkoda.form.RegisterUserForm;
import jakarta.servlet.http.Cookie;
import org.apache.commons.lang3.StringUtils;

/**
 * Data Transfer Object for user registration data and tracking information.
 * <p>
 * This DTO captures registration data submitted by new users and includes tracking
 * information such as HTTP cookies for analytics and registration source tracking.
 * It is used throughout registration workflows, user onboarding processes, and
 * tracking systems to carry user information between layers.
 * </p>
 * <p>
 * Implements {@link CanonicalObject} to provide standardized notification message
 * formatting for registration events and user onboarding notifications.
 * </p>
 * <p>
 * Note: This class contains TODO comments regarding field naming conventions
 * (websiteUrl preferred over project-specific names) and design rule compliance
 * for complex types in DTOs (Cookie[] array for tracking).
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see RegisterUserForm
 * @see com.openkoda.model.User
 */
public class RegisteredUserDto implements CanonicalObject {

    /**
     * User login/username for authentication.
     * This is the primary identifier used for user authentication.
     */
    public String login;
    
    /**
     * User first name (optional for registration).
     * May be null or empty if not provided during registration.
     */
    public String firstName;
    
    /**
     * User last name (optional for registration).
     * May be null or empty if not provided during registration.
     */
    public String lastName;
    
    /**
     * Website URL provided during registration (optional).
     * <p>
     * TODO: Field naming - 'websiteUrl' is preferred as more generic for projects
     * where users register with a website URL rather than project-specific names.
     * </p>
     */
    //TODO: change to 'websiteUrl' - it is more generic for projects where users register with a website url.
    public String websiteUrl;
    
    /**
     * Display nickname (optional).
     * May be null or empty if not provided during registration.
     */
    public String nickname;
    
    /**
     * Organization/tenant ID as primitive long.
     * This field cannot be null and identifies the tenant scope for multi-tenant environments.
     */
    public long organizationId;
    
    /**
     * Newly created user ID as primitive long.
     * This is the database identifier assigned to the user after successful registration.
     */
    public long userId;

    /**
     * HTTP cookies array for registration source tracking and analytics.
     * <p>
     * TODO Rule 5.1: All fields in a DTO must be either a simple field (String, numbers,
     * boolean, enum) or other DTO or collection of these. As cookies allow us to track
     * registration sources, they are an optional part of the DTO despite being a complex type.
     * </p>
     */
    //TODO Rule 5.1 All fields in a DTO must be either a simple field (String, numbers, boolean, enum) or other DTO or collection of these
    //as cookies allows us to track registartion sources, they are an optional part of the DTO
    public Cookie[] cookies;

    /**
     * Constructs a RegisteredUserDto from a RegisterUserForm with user and organization identifiers.
     * <p>
     * This mapping constructor extracts registration data from the form and combines it with
     * the newly assigned user ID, organization ID, and tracking cookies.
     * </p>
     *
     * @param registerUserForm the registration form containing user-submitted data
     * @param userId the newly created user identifier
     * @param organizationId the organization/tenant identifier
     * @param cookies HTTP cookies for registration source tracking (may be null)
     */
    public RegisteredUserDto(RegisterUserForm registerUserForm, long userId, long organizationId, Cookie[] cookies) {
        this.login = registerUserForm.getLogin();
        this.firstName = registerUserForm.getFirstName();
        this.lastName = registerUserForm.getLastName();
        this.websiteUrl = registerUserForm.getWebsiteUrl();
        this.nickname = registerUserForm.getNickname();
        this.organizationId = organizationId;
        this.userId = userId;
        this.cookies = cookies;
    }

    /**
     * Constructs a RegisteredUserDto with all fields explicitly provided.
     * <p>
     * This full constructor allows direct instantiation with all registration data,
     * user identifiers, and tracking information.
     * </p>
     *
     * @param login user login/username for authentication
     * @param firstName user first name (may be null)
     * @param lastName user last name (may be null)
     * @param websiteUrl website URL provided during registration (may be null)
     * @param nickname display nickname (may be null)
     * @param userId newly created user identifier
     * @param organizationId organization/tenant identifier
     * @param cookies HTTP cookies for registration source tracking (may be null)
     */
    public RegisteredUserDto(String login, String firstName, String lastName, String websiteUrl, String nickname, long userId, long organizationId, Cookie[] cookies) {
        this.login = login;
        this.firstName = firstName;
        this.lastName = lastName;
        this.websiteUrl = websiteUrl;
        this.nickname = nickname;
        this.organizationId = organizationId;
        this.userId = userId;
        this.cookies = cookies;
    }

    /**
     * Gets the user login/username.
     *
     * @return the user login identifier
     */
    public String getLogin() {
        return login;
    }

    /**
     * Sets the user login/username.
     *
     * @param login the user login identifier to set
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Gets the user first name.
     *
     * @return the user first name, may be null or empty
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user first name.
     *
     * @param firstName the user first name to set (may be null)
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the user last name.
     *
     * @return the user last name, may be null or empty
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user last name.
     *
     * @param lastName the user last name to set (may be null)
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the website URL provided during registration.
     *
     * @return the website URL, may be null or empty
     */
    public String getWebsiteUrl() {
        return websiteUrl;
    }

    /**
     * Sets the website URL provided during registration.
     *
     * @param websiteUrl the website URL to set (may be null)
     */
    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    /**
     * Gets the display nickname.
     *
     * @return the display nickname, may be null or empty
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Sets the display nickname.
     *
     * @param nickname the display nickname to set (may be null)
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Gets the newly created user identifier.
     *
     * @return the user ID as primitive long
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Sets the user identifier.
     *
     * @param userId the user ID to set
     */
    public void setUserId(long userId) {
        this.userId = userId;
    }

    /**
     * Gets the organization/tenant identifier.
     *
     * @return the organization ID as primitive long
     */
    public long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the organization/tenant identifier.
     *
     * @param organizationId the organization ID to set
     */
    public void setOrganizationId(long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Gets the HTTP cookies array for registration source tracking.
     *
     * @return the cookies array, may be null
     */
    public Cookie[] getCookies() {
        return cookies;
    }

    /**
     * Sets the HTTP cookies array for registration source tracking.
     *
     * @param cookies the cookies array to set (may be null)
     */
    public void setCookies(Cookie[] cookies) {
        this.cookies = cookies;
    }

    /**
     * Generates a standardized notification message for user registration events.
     * <p>
     * The message format is: "Registered User [firstName lastName,] login [URL: websiteUrl] [Nickname: nickname]"
     * where firstName/lastName, websiteUrl, and nickname are conditionally included only if they are not empty.
     * </p>
     *
     * @return formatted notification message describing the registered user
     */
    @Override
    public String notificationMessage() {
        StringBuilder sb = new StringBuilder("Registered User ");
        if(StringUtils.isNotEmpty(firstName) && StringUtils.isNotEmpty(lastName)) {
            sb.append(String.format("%s %s,", firstName, lastName));
        }
        sb.append(String.format("%s ", login));
        if(StringUtils.isNotEmpty(websiteUrl)) {
            sb.append(String.format("URL: %s ", websiteUrl));
        }
        if(StringUtils.isNotEmpty(nickname)) {
            sb.append(String.format("Nickname: %s ", nickname));
        }
        return sb.toString();
    }
}
