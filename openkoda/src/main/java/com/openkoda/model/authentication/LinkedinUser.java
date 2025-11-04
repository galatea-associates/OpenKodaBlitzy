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
 * JPA entity for LinkedIn OAuth 2.0 authentication provider integration.
 * <p>
 * This entity stores LinkedIn user profile data retrieved during the OAuth 2.0 authentication flow,
 * enabling professional network social login via LinkedIn. It links a LinkedIn identity to an OpenKoda
 * User account through a one-to-one shared primary key relationship.

 * <p>
 * The entity is persisted to the 'linkedin_users' table and uses the {@code @MapsId} pattern to share
 * the primary key with the {@link User} entity, ensuring a strict one-to-one association. Profile fields
 * are synchronized from LinkedIn API v2 responses during authentication, including first name, last name,
 * email (requires r_emailaddress scope), and profile picture URL.

 * <p>
 * The {@code @DynamicUpdate} annotation optimizes database operations by updating only modified columns
 * rather than all columns, improving performance for partial entity updates.

 * <p>
 * Example usage:
 * <pre>
 * LinkedinUser linkedinUser = new LinkedinUser(linkedinId, firstName, lastName, email, pictureUrl);
 * </pre>

 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2019-08-21
 * @see User
 * @see LoggedUser
 * // LinkedinController
 */
@Entity
@DynamicUpdate
@Table(name = "linkedin_users")
public class LinkedinUser extends LoggedUser {

    /**
     * Primary key identifier shared with the {@link User} entity.
     * <p>
     * This field uses the {@code @MapsId} pattern to share the same primary key value as the associated
     * User entity, enforcing a one-to-one relationship at the database level.

     */
    @Id
    private Long id;

    /**
     * LinkedIn unique identifier from the OAuth provider.
     * <p>
     * This is the LinkedIn member ID that uniquely identifies the user within LinkedIn's system.
     * It is retrieved from the LinkedIn API during OAuth authentication and remains constant for the user.

     */
    private String linkedinId;

    /**
     * User's first name retrieved from LinkedIn profile.
     * <p>
     * Corresponds to the {@code localizedFirstName} field from the LinkedIn Profile API v2.
     * This field is updated during authentication if the user's profile information changes.

     */
    private String firstName;
    
    /**
     * User's last name retrieved from LinkedIn profile.
     * <p>
     * Corresponds to the {@code localizedLastName} field from the LinkedIn Profile API v2.
     * This field is updated during authentication if the user's profile information changes.

     */
    private String lastName;
    
    /**
     * User's email address from LinkedIn profile.
     * <p>
     * This field requires the {@code r_emailaddress} permission scope during OAuth authorization.
     * If the scope is not granted, this field may be null. The email is retrieved from the
     * LinkedIn Email Address API endpoint during authentication.

     */
    private String email;
    
    /**
     * URL to the user's LinkedIn profile picture.
     * <p>
     * This URL points to the user's LinkedIn profile photo and can be used to display their picture
     * within the application. The URL may change if the user updates their profile picture.

     */
    private String profilePicture;

    /**
     * One-to-one relationship with the {@link User} entity.
     * <p>
     * This bidirectional association uses {@code @MapsId} to share the primary key with the User entity.
     * The relationship is mapped through the {@code user_id} foreign key column. The {@code @JsonIgnore}
     * annotation prevents circular reference issues during JSON serialization.

     */
    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    /**
     * No-argument constructor required by JPA for entity instantiation.
     * <p>
     * This constructor is used by the JPA provider (Hibernate) when loading entities from the database
     * or creating new instances via reflection. It should not be called directly in application code.

     */
    public LinkedinUser() {
    }

    /**
     * Convenience constructor for creating a LinkedinUser with all LinkedIn profile fields.
     * <p>
     * This constructor initializes a new LinkedinUser entity with data retrieved from the LinkedIn API
     * during OAuth authentication. All profile fields are populated from the LinkedIn API v2 response.

     *
     * @param linkedinId the LinkedIn member ID (unique identifier from LinkedIn OAuth provider)
     * @param firstName the user's first name from LinkedIn profile (localizedFirstName)
     * @param lastName the user's last name from LinkedIn profile (localizedLastName)
     * @param email the user's email address from LinkedIn profile (requires r_emailaddress scope, may be null)
     * @param profilePicture the URL to the user's LinkedIn profile picture (may be null)
     */
    public LinkedinUser(String linkedinId,
                        String firstName,
                        String lastName,
                        String email,
                        String profilePicture) {
        this.linkedinId = linkedinId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.profilePicture = profilePicture;
    }

    /**
     * Returns a formatted string representation of this LinkedinUser for audit trail purposes.
     * <p>
     * The audit string includes the user's first name, last name, and email address, providing
     * a human-readable identifier for audit logs and tracking purposes.

     *
     * @return a formatted string in the format "firstName lastName email" for audit logging
     */
    @Override
    public String toAuditString() {
        return String.format("%s %s %s", firstName, lastName, email);
    }

    /**
     * Returns the primary key identifier shared with the associated {@link User} entity.
     * <p>
     * This method overrides {@link LoggedUser#getId} to provide access to the shared primary key.
     * The ID value is synchronized with the User entity through the {@code @MapsId} relationship.

     *
     * @return the primary key identifier, or null if not yet persisted
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * Sets the primary key identifier for this LinkedinUser entity.
     * <p>
     * This ID is shared with the associated {@link User} entity via the {@code @MapsId} pattern.
     * Typically, this value is managed by JPA and should not be set manually in application code.

     *
     * @param id the primary key identifier to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the LinkedIn unique identifier (member ID) from the OAuth provider.
     * <p>
     * This is the immutable LinkedIn member ID that uniquely identifies the user within LinkedIn's system.
     * It is retrieved during the OAuth authentication flow and remains constant for the user.

     *
     * @return the LinkedIn member ID, or null if not yet set
     */
    public String getLinkedinId() {
        return linkedinId;
    }

    /**
     * Sets the LinkedIn unique identifier (member ID) for this user.
     * <p>
     * This value is typically set once during initial OAuth authentication and should not change
     * for the lifetime of the user's LinkedIn account association.

     *
     * @param linkedinId the LinkedIn member ID to set
     */
    public void setLinkedinId(String linkedinId) {
        this.linkedinId = linkedinId;
    }

    /**
     * Returns the user's first name from their LinkedIn profile.
     * <p>
     * This value corresponds to the {@code localizedFirstName} field from the LinkedIn Profile API v2
     * and is updated during authentication if the user's profile changes.

     *
     * @return the user's first name, or null if not available
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name from their LinkedIn profile.
     * <p>
     * This value is synchronized from the LinkedIn API during authentication and represents
     * the user's localized first name.

     *
     * @param firstName the user's first name to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the user's last name from their LinkedIn profile.
     * <p>
     * This value corresponds to the {@code localizedLastName} field from the LinkedIn Profile API v2
     * and is updated during authentication if the user's profile changes.

     *
     * @return the user's last name, or null if not available
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name from their LinkedIn profile.
     * <p>
     * This value is synchronized from the LinkedIn API during authentication and represents
     * the user's localized last name.

     *
     * @param lastName the user's last name to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the user's email address from their LinkedIn profile.
     * <p>
     * This field requires the {@code r_emailaddress} permission scope during OAuth authorization.
     * If the application does not request or the user does not grant this scope, this field will be null.
     * The email is retrieved from the LinkedIn Email Address API endpoint during authentication.

     *
     * @return the user's email address, or null if not available or permission not granted
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address from their LinkedIn profile.
     * <p>
     * This value is synchronized from the LinkedIn API during authentication when the {@code r_emailaddress}
     * permission scope is granted. The email may be null if the permission was not requested or granted.

     *
     * @param email the user's email address to set, may be null
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the URL to the user's LinkedIn profile picture.
     * <p>
     * This URL points to the user's current profile photo on LinkedIn and can be used to display
     * their picture within the application. The URL may change if the user updates their profile picture.

     *
     * @return the profile picture URL, or null if not available
     */
    public String getProfilePicture() {
        return profilePicture;
    }

    /**
     * Sets the URL to the user's LinkedIn profile picture.
     * <p>
     * This value is synchronized from the LinkedIn API during authentication and provides
     * a direct link to the user's profile photo.

     *
     * @param profilePicture the profile picture URL to set, may be null
     */
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    /**
     * Returns the associated {@link User} entity for this LinkedIn user.
     * <p>
     * This method provides access to the OpenKoda User entity that is linked to this LinkedIn identity
     * through the one-to-one shared primary key relationship. The User entity contains the core user
     * information and relationships within the OpenKoda system.

     *
     * @return the associated User entity, or null if not yet set
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the associated {@link User} entity for this LinkedIn user.
     * <p>
     * This method establishes the one-to-one relationship between this LinkedIn identity and an OpenKoda
     * User account. The relationship uses the {@code @MapsId} pattern, so the User's ID will be shared
     * as this entity's primary key.

     *
     * @param user the User entity to associate with this LinkedIn user
     */
    public void setUser(User user) {
        this.user = user;
    }
}
