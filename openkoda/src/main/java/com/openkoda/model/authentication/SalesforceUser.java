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
 * JPA entity representing a Salesforce OAuth 2.0 authenticated user profile.
 * <p>
 * This entity stores user profile data retrieved during Salesforce OAuth 2.0 authentication flow
 * and enables enterprise login via Salesforce Identity. It links a Salesforce identity to an
 * OpenKoda {@link User} account through a shared primary key relationship.
 * </p>
 * <p>
 * The entity is persisted to the 'salesforce_users' table with a one-to-one relationship to the
 * User entity using {@code @MapsId} annotation for shared primary key pattern. This ensures that
 * each Salesforce user profile maps directly to exactly one OpenKoda user account.
 * </p>
 * <p>
 * Profile fields are automatically synchronized from Salesforce Identity API responses during
 * OAuth authentication. The {@code @DynamicUpdate} annotation optimizes database operations by
 * updating only modified columns rather than all entity fields.
 * </p>
 * <p>
 * The organizationId field tracks the Salesforce organization (org) where the user is registered,
 * supporting multi-org Salesforce deployments.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * SalesforceUser sfUser = new SalesforceUser(salesforceId, firstName, lastName, ...);
 * sfUser.setUser(openkodaUser);
 * }</pre>
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2019-07-17
 * @see User
 * @see LoggedUser
 * @see com.openkoda.integration.controller.SalesforceController
 */
@Entity
@DynamicUpdate
@Table(name = "salesforce_users")
public class SalesforceUser extends LoggedUser {
    /**
     * Primary key shared with the associated User entity.
     * <p>
     * This field uses the {@code @MapsId} annotation to share the primary key value with the
     * {@link User} entity, implementing a one-to-one relationship with shared primary key pattern.
     * The value is automatically populated from the associated User's id field.
     * </p>
     */
    @Id
    private Long id;

    /**
     * Salesforce unique identifier for this user.
     * <p>
     * This is the Salesforce User ID retrieved from the OAuth provider during authentication.
     * It uniquely identifies the user within their Salesforce organization and is used to
     * correlate OpenKoda user accounts with Salesforce identities.
     * </p>
     */
    private String salesforceId;

    /**
     * User's first name from Salesforce profile.
     * <p>
     * Retrieved from the Salesforce Identity API response during OAuth authentication flow.
     * </p>
     */
    private String firstName;
    
    /**
     * User's last name from Salesforce profile.
     * <p>
     * Retrieved from the Salesforce Identity API response during OAuth authentication flow.
     * </p>
     */
    private String lastName;
    
    /**
     * User's full display name from Salesforce profile.
     * <p>
     * Typically formatted as "FirstName LastName" and used for display purposes throughout
     * the application. Retrieved from the Salesforce Identity API response.
     * </p>
     */
    private String name;
    
    /**
     * User's email address from Salesforce profile.
     * <p>
     * Primary contact email address retrieved from Salesforce Identity API. This email
     * may be used for application notifications and user identification.
     * </p>
     */
    private String email;
    
    /**
     * URL to user's Salesforce profile picture.
     * <p>
     * External URL pointing to the user's profile photo hosted by Salesforce. Can be used
     * to display the user's avatar in the application interface.
     * </p>
     */
    private String picture;
    
    /**
     * Salesforce organization (org) ID where the user is registered.
     * <p>
     * Identifies the specific Salesforce organization instance where this user's account
     * exists. Supports multi-org Salesforce deployments where users may belong to
     * different Salesforce organizations.
     * </p>
     */
    private String organizationId;
    
    /**
     * User's preferred username from Salesforce identity.
     * <p>
     * The username preference specified in the user's Salesforce profile, which may
     * differ from their email address. Retrieved from Salesforce Identity API.
     * </p>
     */
    private String preferredUsername;
    
    /**
     * User's nickname from Salesforce profile.
     * <p>
     * Optional informal name or alias for the user, retrieved from their Salesforce
     * profile information.
     * </p>
     */
    private String nickname;

    /**
     * One-to-one relationship with the associated OpenKoda User entity.
     * <p>
     * Uses {@code @MapsId} annotation to share the primary key with the User entity, creating
     * a one-to-one relationship where the SalesforceUser.id is identical to User.id.
     * The {@code @JoinColumn} specifies "user_id" as the foreign key column name.
     * </p>
     * <p>
     * Annotated with {@code @JsonIgnore} to prevent circular serialization when converting
     * this entity to JSON format.
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
     * Required by JPA specification for entity creation through reflection. Applications
     * should prefer using the parameterized constructor for creating new instances.
     * </p>
     */
    public SalesforceUser() {

    }

    /**
     * Constructs a new SalesforceUser with all Salesforce profile fields.
     * <p>
     * Convenience constructor for creating a SalesforceUser entity from OAuth authentication
     * response data. Initializes all profile fields retrieved from Salesforce Identity API.
     * The User relationship must be set separately using {@link #setUser(User)}.
     * </p>
     *
     * @param salesforceId Salesforce unique identifier (Salesforce User ID)
     * @param firstName user's first name from Salesforce profile
     * @param lastName user's last name from Salesforce profile
     * @param name user's full display name from Salesforce profile
     * @param email user's email address from Salesforce profile
     * @param picture URL to user's Salesforce profile picture
     * @param organizationId Salesforce organization ID where user is registered
     * @param preferredUsername user's preferred username from Salesforce identity
     * @param nickname user's nickname from Salesforce profile
     */
    public SalesforceUser(String salesforceId,
                          String firstName,
                          String lastName,
                          String name,
                          String email,
                          String picture,
                          String organizationId,
                          String preferredUsername,
                          String nickname) {
        this.salesforceId = salesforceId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.organizationId = organizationId;
        this.preferredUsername = preferredUsername;
        this.nickname = nickname;
    }

    /**
     * Returns the primary key shared with the associated User entity.
     * <p>
     * This ID value is identical to the associated {@link User#getId()} due to the
     * {@code @MapsId} shared primary key pattern.
     * </p>
     *
     * @return the shared primary key, or null if not yet persisted
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * Sets the primary key for this entity.
     * <p>
     * Typically populated automatically through the {@code @MapsId} relationship when
     * the User entity is associated. Manual setting should be avoided in favor of
     * letting JPA manage the shared primary key.
     * </p>
     *
     * @param id the primary key value to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the Salesforce unique identifier for this user.
     *
     * @return the Salesforce User ID, or null if not set
     */
    public String getSalesforceId() {
        return salesforceId;
    }

    /**
     * Sets the Salesforce unique identifier for this user.
     *
     * @param salesforceId the Salesforce User ID to set
     */
    public void setSalesforceId(String salesforceId) {
        this.salesforceId = salesforceId;
    }

    /**
     * Returns the user's first name from Salesforce profile.
     *
     * @return the first name, or null if not set
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name from Salesforce profile.
     *
     * @param firstName the first name to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the user's last name from Salesforce profile.
     *
     * @return the last name, or null if not set
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name from Salesforce profile.
     *
     * @param lastName the last name to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the user's full display name from Salesforce profile.
     *
     * @return the full display name, or null if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's full display name from Salesforce profile.
     *
     * @param name the full display name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the user's email address from Salesforce profile.
     *
     * @return the email address, or null if not set
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address from Salesforce profile.
     *
     * @param email the email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the URL to user's Salesforce profile picture.
     *
     * @return the profile picture URL, or null if not set
     */
    public String getPicture() {
        return picture;
    }

    /**
     * Sets the URL to user's Salesforce profile picture.
     *
     * @param picture the profile picture URL to set
     */
    public void setPicture(String picture) {
        this.picture = picture;
    }

    /**
     * Returns the Salesforce organization (org) ID where the user is registered.
     *
     * @return the Salesforce organization ID, or null if not set
     */
    public String getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the Salesforce organization (org) ID where the user is registered.
     *
     * @param organizationId the Salesforce organization ID to set
     */
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Returns the user's preferred username from Salesforce identity.
     *
     * @return the preferred username, or null if not set
     */
    public String getPreferredUsername() {
        return preferredUsername;
    }

    /**
     * Sets the user's preferred username from Salesforce identity.
     *
     * @param preferredUsername the preferred username to set
     */
    public void setPreferredUsername(String preferredUsername) {
        this.preferredUsername = preferredUsername;
    }

    /**
     * Returns the user's nickname from Salesforce profile.
     *
     * @return the nickname, or null if not set
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Sets the user's nickname from Salesforce profile.
     *
     * @param nickname the nickname to set
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Returns the associated OpenKoda User entity.
     * <p>
     * This one-to-one relationship links the Salesforce authentication profile to the
     * corresponding OpenKoda user account.
     * </p>
     *
     * @return the associated User entity, or null if not set
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the associated OpenKoda User entity.
     * <p>
     * Establishes the one-to-one relationship between this Salesforce profile and an
     * OpenKoda user account. The primary key will be shared via {@code @MapsId} annotation.
     * </p>
     *
     * @param user the User entity to associate with this Salesforce profile
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Returns a formatted string representation for audit trail logging.
     * <p>
     * Combines the user's full name and email address in a human-readable format
     * for audit and logging purposes. Format: "Name email@example.com"
     * </p>
     *
     * @return formatted string containing name and email, suitable for audit logs
     */
    @Override
    public String toAuditString() {
        return String.format("%s %s", name, email);
    }

}
