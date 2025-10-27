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
 * JPA entity for Google OAuth 2.0 authentication provider integration.
 * <p>
 * This entity stores Google user profile data retrieved during the OAuth 2.0 authentication flow,
 * enabling social login via Google Sign-In. The entity links a Google identity to an OpenKoda
 * User account through a one-to-one relationship using a shared primary key pattern.
 * </p>
 * <p>
 * <b>JPA Mapping:</b> Persisted to the {@code google_users} table with a shared primary key
 * relationship to the {@link User} entity via {@code @MapsId}.
 * </p>
 * <p>
 * <b>OAuth Integration:</b> Profile fields are populated from Google People API responses
 * during authentication and synchronized on each login to keep user data current.
 * </p>
 * <p>
 * <b>Hibernate Optimization:</b> The {@code @DynamicUpdate} annotation ensures only modified
 * columns are included in SQL UPDATE statements, improving performance for partial updates.
 * </p>
 * <p>
 * <b>Relationship:</b> Maintains a bidirectional one-to-one relationship with {@link User}
 * using {@code @MapsId} shared primary key pattern, where {@code GoogleUser.id} equals {@code User.id}.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see User
 * @see LoggedUser
 * @see com.openkoda.integration.controller.GoogleController
 */
@Entity
@DynamicUpdate
@Table(name = "google_users")
public class GoogleUser extends LoggedUser {

    /**
     * Primary key for the GoogleUser entity, shared with the associated User entity.
     * <p>
     * This ID is managed via {@code @MapsId} annotation, establishing a shared primary key
     * relationship where {@code GoogleUser.id} equals {@code User.id}.
     * </p>
     */
    @Id
    private Long id;

    /**
     * Google's unique identifier for the user (Google User ID / sub claim from OAuth token).
     * <p>
     * This value corresponds to the {@code sub} (subject) claim in the Google ID token
     * and uniquely identifies the user within Google's authentication system.
     * </p>
     */
    @Column
    private String googleId;

    /**
     * User's first name (given name) retrieved from Google profile.
     * <p>
     * Populated from the {@code given_name} field in the Google People API response.
     * </p>
     */
    private String firstName;

    /**
     * User's last name (family name) retrieved from Google profile.
     * <p>
     * Populated from the {@code family_name} field in the Google People API response.
     * </p>
     */
    private String lastName;

    /**
     * User's full display name retrieved from Google profile.
     * <p>
     * Populated from the {@code name} field in the Google People API response.
     * Typically combines first name and last name.
     * </p>
     */
    private String name;

    /**
     * User's email address retrieved from Google profile.
     * <p>
     * Populated from the {@code email} claim in the Google ID token.
     * Requires the {@code email} scope during OAuth authentication.
     * </p>
     */
    private String email;

    /**
     * URL to the user's Google profile picture.
     * <p>
     * Populated from the {@code picture} field in the Google People API response.
     * Provides a link to the user's profile image hosted by Google.
     * </p>
     */
    private String picture;

    /**
     * One-to-one relationship with the User entity using shared primary key pattern.
     * <p>
     * The {@code @MapsId} annotation establishes that this relationship shares the primary key,
     * where {@code GoogleUser.id} is derived from {@code User.id}. The {@code @JoinColumn}
     * specifies the foreign key column {@code user_id} in the {@code google_users} table.
     * </p>
     * <p>
     * The {@code @JsonIgnore} annotation prevents serialization of the User entity to avoid
     * circular references and unnecessary data exposure in JSON responses.
     * </p>
     */
    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    /**
     * No-argument constructor required by JPA for entity instantiation.
     * <p>
     * This constructor is used by the JPA provider (Hibernate) when loading entities
     * from the database or creating new entity instances via reflection.
     * </p>
     */
    public GoogleUser() {
    }

    /**
     * Convenience constructor for creating a GoogleUser with all Google profile fields.
     * <p>
     * This constructor is typically used during OAuth authentication flow to populate
     * the GoogleUser entity with data retrieved from the Google People API response.
     * </p>
     *
     * @param googleId  Google's unique identifier for the user (sub claim from ID token)
     * @param firstName user's first name from Google profile (given name)
     * @param lastName  user's last name from Google profile (family name)
     * @param name      user's full display name from Google profile
     * @param email     user's email address from Google profile (requires email scope)
     * @param picture   URL to user's Google profile picture
     */
    public GoogleUser(String googleId, String firstName, String lastName, String name, String email, String picture) {
        this.googleId = googleId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = name;
        this.email = email;
        this.picture = picture;
    }

    /**
     * Returns Google's unique identifier for the user.
     * <p>
     * This value corresponds to the {@code sub} (subject) claim from the Google ID token
     * and uniquely identifies the user within Google's authentication system.
     * </p>
     *
     * @return the Google user ID (sub claim), or null if not set
     */
    public String getGoogleId() {
        return googleId;
    }

    /**
     * Returns the user's first name from Google profile.
     *
     * @return the first name (given name), or null if not available
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name from Google profile.
     *
     * @param firstName the first name (given name) to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the user's last name from Google profile.
     *
     * @return the last name (family name), or null if not available
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name from Google profile.
     *
     * @param lastName the last name (family name) to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the user's full display name from Google profile.
     *
     * @return the full display name, or null if not available
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's full display name from Google profile.
     *
     * @param name the full display name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the user's email address from Google profile.
     * <p>
     * This email corresponds to the {@code email} claim from the Google ID token
     * and requires the {@code email} scope during OAuth authentication.
     * </p>
     *
     * @return the email address, or null if not available
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address from Google profile.
     *
     * @param email the email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the URL to the user's Google profile picture.
     *
     * @return the profile picture URL, or null if not available
     */
    public String getPicture() {
        return picture;
    }

    /**
     * Sets the URL to the user's Google profile picture.
     *
     * @param picture the profile picture URL to set
     */
    public void setPicture(String picture) {
        this.picture = picture;
    }

    /**
     * Returns a formatted string representation for audit trail logging.
     * <p>
     * Combines the user's full display name and email address to create a human-readable
     * identifier for audit logs. This format helps track authentication events and user
     * actions in the audit trail.
     * </p>
     *
     * @return formatted string in the format "name email" for audit logging
     */
    @Override
    public String toAuditString() {
        return String.format("%s %s", name, email);
    }

    /**
     * Returns the associated User entity.
     * <p>
     * Provides access to the OpenKoda User account linked to this Google authentication.
     * The relationship uses a shared primary key pattern via {@code @MapsId}.
     * </p>
     *
     * @return the associated User entity, or null if not set
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the associated User entity.
     * <p>
     * Establishes the relationship between this GoogleUser and the corresponding
     * OpenKoda User account. This should be set during the OAuth authentication flow
     * when linking or creating user accounts.
     * </p>
     *
     * @param user the User entity to associate with this GoogleUser
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Returns the primary key shared with the associated User entity.
     * <p>
     * This ID is managed via {@code @MapsId} annotation, where {@code GoogleUser.id}
     * equals {@code User.id} in the shared primary key relationship pattern.
     * </p>
     *
     * @return the primary key (shared with User.id), or null if not set
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * Sets the primary key shared with the associated User entity.
     * <p>
     * Typically managed automatically by JPA through the {@code @MapsId} relationship.
     * Manual setting should be done with caution to maintain referential integrity.
     * </p>
     *
     * @param id the primary key to set (should match the associated User.id)
     */
    public void setId(Long id) {
        this.id = id;
    }

}
