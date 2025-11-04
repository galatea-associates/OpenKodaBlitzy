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
 * JPA entity representing LDAP/Active Directory authentication provider integration.
 * <p>
 * This entity enables enterprise Single Sign-On (SSO) authentication by linking LDAP
 * directory identities to OpenKoda User accounts. It stores standard LDAP schema
 * attributes synchronized from directory server responses during authentication.

 * <p>
 * The entity uses a shared primary key relationship with the {@link User} entity via
 * the {@code @MapsId} annotation, ensuring each LDAP user corresponds to exactly one
 * OpenKoda user account. The {@code @DynamicUpdate} annotation optimizes database
 * operations by updating only modified columns rather than all fields.

 * <p>
 * Persisted to the {@code ldap_users} table with one-to-one relationship to the
 * {@code users} table through the {@code user_id} foreign key.

 * <p>
 * Common LDAP attributes stored include:
 * <ul>
 *   <li>{@code cn} (Common Name) - typically the full name</li>
 *   <li>{@code email} - email address from the mail attribute</li>
 *   <li>{@code givenName} - first name</li>
 *   <li>{@code sn} - surname (last name)</li>
 *   <li>{@code uid} - unique username/login identifier</li>
 * </ul>

 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2019-02-11
 * @see User
 * @see LoggedUser
 * @see org.springframework.security.ldap.authentication.LdapAuthenticationProvider
 */
@Entity
@DynamicUpdate
@Table(name = "ldap_users")
public class LDAPUser extends LoggedUser {

    /**
     * Primary key shared with the {@link User} entity via {@code @MapsId}.
     * This value is populated automatically from the associated User's ID,
     * ensuring referential integrity between LDAP authentication data and
     * the core user account.
     */
    @Id
    private Long id;

    /**
     * Common Name (cn) attribute from the LDAP directory.
     * Typically contains the user's full name as stored in the directory server.
     */
    private String cn;

    /**
     * Email address retrieved from the LDAP {@code mail} attribute.
     * Used for user communication and account identification.
     */
    private String email;

    /**
     * Given name (first name) from the LDAP {@code givenName} attribute.
     * Represents the user's first name as stored in the directory.
     */
    private String givenName;

    /**
     * Surname (last name) from the LDAP {@code sn} attribute.
     * Represents the user's family name as stored in the directory.
     */
    private String sn;

    /**
     * User identifier from the LDAP {@code uid} attribute.
     * Serves as the unique username/login identifier for authentication.
     * This is typically the value used during the LDAP login process.
     */
    private String uid;

    /**
     * One-to-one relationship with the core {@link User} entity.
     * <p>
     * The {@code @MapsId} annotation establishes a shared primary key relationship,
     * where this LDAPUser's ID is derived from the associated User's ID. The
     * {@code @JoinColumn} specifies the foreign key column {@code user_id} in the
     * {@code ldap_users} table. The {@code @JsonIgnore} annotation prevents
     * circular serialization issues during JSON conversion.

     */
    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    /**
     * Default no-argument constructor required by JPA for entity instantiation.
     * Used by the persistence provider when loading entities from the database.
     */
    public LDAPUser() {
    }

    /**
     * Convenience constructor for creating an LDAPUser with all LDAP attributes.
     * <p>
     * This constructor is typically used during LDAP authentication when creating
     * a new user record from directory server response data.

     *
     * @param cn        the Common Name (full name) from LDAP directory
     * @param email     the email address from LDAP mail attribute
     * @param givenName the given name (first name) from LDAP givenName attribute
     * @param sn        the surname (last name) from LDAP sn attribute
     * @param uid       the user identifier (username) from LDAP uid attribute
     */
    public LDAPUser(String cn, String email, String givenName, String sn, String uid) {
        this.cn = cn;
        this.email = email;
        this.givenName = givenName;
        this.sn = sn;
        this.uid = uid;
    }

    /**
     * Returns the Common Name (full name) from the LDAP directory.
     *
     * @return the cn attribute value, or {@code null} if not set
     */
    public String getCn() {
        return cn;
    }

    /**
     * Sets the Common Name (full name) from the LDAP directory.
     *
     * @param cn the cn attribute value to set
     */
    public void setCn(String cn) {
        this.cn = cn;
    }

    /**
     * Returns the email address from the LDAP mail attribute.
     *
     * @return the email address, or {@code null} if not set
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address from the LDAP mail attribute.
     *
     * @param email the email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the given name (first name) from the LDAP directory.
     *
     * @return the given name, or {@code null} if not set
     */
    public String getGivenName() {
        return givenName;
    }

    /**
     * Sets the given name (first name) from the LDAP directory.
     *
     * @param givenName the given name to set
     */
    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    /**
     * Returns the surname (last name) from the LDAP directory.
     *
     * @return the surname, or {@code null} if not set
     */
    public String getSn() {
        return sn;
    }

    /**
     * Sets the surname (last name) from the LDAP directory.
     *
     * @param sn the surname to set
     */
    public void setSn(String sn) {
        this.sn = sn;
    }

    /**
     * Returns the user identifier (username) from the LDAP directory.
     *
     * @return the uid attribute value, or {@code null} if not set
     */
    public String getUid() {
        return uid;
    }

    /**
     * Sets the user identifier (username) from the LDAP directory.
     *
     * @param uid the uid attribute value to set
     */
    public void setUid(String uid) {
        this.uid = uid;
    }

    /**
     * Returns a formatted string representation for audit trail logging.
     * <p>
     * Combines the common name (full name) and email address to provide
     * human-readable identification in audit logs and system tracking.

     *
     * @return formatted string containing cn and email (e.g., "John Doe john.doe@example.com")
     */
    @Override
    public String toAuditString() {
        return String.format("%s %s", cn, email);
    }

    /**
     * Returns the primary key identifier shared with the associated {@link User} entity.
     * <p>
     * This ID is automatically populated from the User entity via the {@code @MapsId}
     * annotation and should not be set manually.

     *
     * @return the shared primary key ID, or {@code null} if not yet persisted
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * Sets the primary key identifier.
     * <p>
     * Note: This value is typically managed automatically by JPA through the
     * {@code @MapsId} relationship and should rarely be set directly.

     *
     * @param id the primary key identifier to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the associated core {@link User} entity.
     * <p>
     * This represents the one-to-one relationship between LDAP authentication
     * data and the OpenKoda user account.

     *
     * @return the associated User entity, or {@code null} if not set
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the associated core {@link User} entity.
     * <p>
     * Establishing this relationship automatically populates the ID field via
     * the {@code @MapsId} annotation, ensuring proper shared primary key mapping.

     *
     * @param user the User entity to associate with this LDAP user
     */
    public void setUser(User user) {
        this.user = user;
    }
}
