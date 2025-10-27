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

package com.openkoda.model.authentication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openkoda.model.User;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

/**
 * JPA entity for Facebook OAuth authentication provider integration.
 * <p>
 * This entity stores Facebook user profile data retrieved during the OAuth 2.0 authentication flow,
 * enabling social login via Facebook and linking Facebook identities to OpenKoda User accounts.
 * Profile fields are synchronized from Facebook Graph API responses during authentication.
 * </p>
 * <p>
 * Persisted to the 'fb_users' table with a shared primary key relationship to the User entity
 * via {@code @MapsId}. The {@code @DynamicUpdate} annotation enables Hibernate optimization
 * to update only modified columns during persistence operations.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * FacebookUser fbUser = new FacebookUser(facebookId, firstName, lastName, ...);
 * fbUser.setUser(user);
 * </pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see User
 * @see LoggedUser
 * @see com.openkoda.integration.controller.FacebookController
 */
@Entity
@DynamicUpdate
@Table(name = "fb_users")
public class FacebookUser extends LoggedUser {

    /**
     * Primary key shared with the User entity via {@code @MapsId}.
     * <p>
     * This field uses the same value as the associated User's id, enforcing a one-to-one
     * relationship between FacebookUser and User entities.
     * </p>
     */
    @Id
    private Long id;

    /**
     * Facebook unique identifier from the OAuth provider (Facebook User ID).
     * <p>
     * This value is obtained from the Facebook Graph API during OAuth authentication
     * and uniquely identifies the user within Facebook's system.
     * </p>
     */
    private String facebookId;

    /**
     * User's first name from Facebook profile.
     */
    private String firstName;
    
    /**
     * User's last name from Facebook profile.
     */
    private String lastName;
    
    /**
     * User's full display name from Facebook profile.
     */
    private String name;
    
    /**
     * User's email address from Facebook profile.
     * <p>
     * Requires the 'email' permission scope in the OAuth flow.
     * </p>
     */
    private String email;
    
    /**
     * URL to the user's Facebook cover photo.
     */
    private String coverPicture;
    
    /**
     * URL to the user's Facebook profile picture.
     */
    private String picture;
    
    /**
     * Minimum age range value from Facebook.
     * <p>
     * Example: "21" for users in the 21+ age range.
     * </p>
     */
    private String ageRangeMin;

    /**
     * One-to-one relationship with the User entity.
     * <p>
     * Uses {@code @MapsId} with {@code @JoinColumn(name="user_id")} to share the primary key.
     * Annotated with {@code @JsonIgnore} to prevent JSON serialization of the user reference.
     * </p>
     */
    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    /**
     * No-argument constructor for JPA entity instantiation.
     * <p>
     * Required by JPA specification for entity creation and reflection-based operations.
     * </p>
     */
    public FacebookUser() {
    }

    /**
     * Convenience constructor for creating a FacebookUser with all profile fields.
     * <p>
     * Initializes a FacebookUser entity with data retrieved from the Facebook Graph API
     * during OAuth authentication. The User relationship must be set separately via
     * {@link #setUser(User)}.
     * </p>
     *
     * @param facebookId   Facebook unique identifier (Facebook User ID)
     * @param firstName    User's first name from Facebook profile
     * @param lastName     User's last name from Facebook profile
     * @param name         User's full display name from Facebook profile
     * @param email        User's email address from Facebook profile (requires email scope)
     * @param coverPicture URL to user's Facebook cover photo
     * @param picture      URL to user's Facebook profile picture
     * @param ageRangeMin  Minimum age range value (e.g., "21" for 21+)
     */
    public FacebookUser(String facebookId,
                        String firstName,
                        String lastName,
                        String name,
                        String email,
                        String coverPicture,
                        String picture,
                        String ageRangeMin) {
        this.facebookId = facebookId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = name;
        this.email = email;
        this.coverPicture = coverPicture;
        this.picture = picture;
        this.ageRangeMin = ageRangeMin;
    }

    /**
     * Returns the Facebook unique identifier.
     *
     * @return Facebook User ID from OAuth provider, or null if not set
     */
    public String getFacebookId() {
        return facebookId;
    }

    /**
     * Returns the user's first name from Facebook profile.
     *
     * @return first name, or null if not provided
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name from Facebook profile.
     *
     * @param firstName user's first name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the user's last name from Facebook profile.
     *
     * @return last name, or null if not provided
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name from Facebook profile.
     *
     * @param lastName user's last name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the user's full display name from Facebook profile.
     *
     * @return full display name, or null if not provided
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's full display name from Facebook profile.
     *
     * @param name user's full display name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the user's email address from Facebook profile.
     * <p>
     * This value is only available if the OAuth flow includes the 'email' permission scope.
     * </p>
     *
     * @return email address, or null if not provided or email scope not granted
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address from Facebook profile.
     *
     * @param email user's email address
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the URL to the user's Facebook cover photo.
     *
     * @return cover photo URL, or null if not available
     */
    public String getCoverPicture() {
        return coverPicture;
    }

    /**
     * Sets the URL to the user's Facebook cover photo.
     *
     * @param coverPicture cover photo URL
     */
    public void setCoverPicture(String coverPicture) {
        this.coverPicture = coverPicture;
    }

    /**
     * Returns the URL to the user's Facebook profile picture.
     *
     * @return profile picture URL, or null if not available
     */
    public String getPicture() {
        return picture;
    }

    /**
     * Sets the URL to the user's Facebook profile picture.
     *
     * @param picture profile picture URL
     */
    public void setPicture(String picture) {
        this.picture = picture;
    }

    /**
     * Returns the minimum age range value from Facebook.
     * <p>
     * Example: "21" indicates the user is in the 21+ age range.
     * </p>
     *
     * @return minimum age range value, or null if not provided
     */
    public String getAgeRangeMin() {
        return ageRangeMin;
    }

    /**
     * Sets the minimum age range value from Facebook.
     *
     * @param ageRangeMin minimum age range value (e.g., "21")
     */
    public void setAgeRangeMin(String ageRangeMin) {
        this.ageRangeMin = ageRangeMin;
    }

    /**
     * Returns a formatted string representation for audit trail purposes.
     * <p>
     * Generates a human-readable identification string containing the user's name and email,
     * suitable for audit logging and tracking.
     * </p>
     *
     * @return formatted string with name and email (e.g., "John Doe john@example.com")
     */
    @Override
    public String toAuditString() {
        return String.format("%s %s", name, email);
    }

    /**
     * Returns the associated User entity.
     * <p>
     * This represents the one-to-one relationship between FacebookUser and User,
     * with a shared primary key enforced via {@code @MapsId}.
     * </p>
     *
     * @return associated User entity
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the associated User entity.
     * <p>
     * This must be called to establish the relationship before persisting a new FacebookUser.
     * </p>
     *
     * @param user the User entity to associate with this FacebookUser
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Returns the primary key shared with the User entity.
     * <p>
     * This value is identical to the associated User's id due to the {@code @MapsId}
     * shared primary key pattern.
     * </p>
     *
     * @return shared primary key value
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * Sets the primary key value.
     * <p>
     * Typically managed automatically by JPA when the User relationship is established.
     * </p>
     *
     * @param id primary key value (must match associated User's id)
     */
    public void setId(Long id) {
        this.id = id;
    }

}
