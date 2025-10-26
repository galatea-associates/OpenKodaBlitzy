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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * JPA entity for local username/password authentication credentials.
 * <p>
 * This entity stores authentication credentials for users who authenticate using local
 * username and password (as opposed to OAuth, LDAP, or other external authentication methods).
 * The password is BCrypt-encoded upon creation and excluded from JSON serialization and audit logs
 * for security purposes.
 * </p>
 * <p>
 * <b>JPA Mapping:</b> Persisted to 'login_and_password' table with a shared primary key relationship
 * to the User entity via {@code @MapsId}. The {@code id} field shares the same value as the associated
 * {@code User.id}, creating a one-to-one relationship with no separate foreign key column.
 * </p>
 * <p>
 * <b>Security Features:</b>
 * <ul>
 *   <li>Password encoding: Uses BCrypt via static {@code passwordEncoder} instance</li>
 *   <li>JSON exclusion: {@code @JsonIgnore} on password and user fields prevents exposure in API responses</li>
 *   <li>Audit exclusion: Password field excluded from audit logs via {@code ignoredProperties}</li>
 * </ul>
 * </p>
 * <p>
 * <b>Hibernate Optimization:</b> {@code @DynamicUpdate} annotation ensures only modified columns
 * are included in SQL UPDATE statements, improving performance for partial entity updates.
 * </p>
 * <p>
 * <b>Thread-Safety Note:</b> The static {@code passwordEncoder} initialization via
 * {@code setPasswordEncoderOnce} is not synchronized. Proper initialization depends on
 * Spring startup ordering to avoid race conditions.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see User
 * @see LoggedUser
 * @see PasswordEncoder
 */
@Entity
@DynamicUpdate
@Table(name = "login_and_password")
public class LoginAndPassword extends LoggedUser {

    /**
     * List of field names excluded from audit logging.
     * <p>
     * Contains "password" to prevent sensitive credentials from being recorded in audit trails.
     * Used by {@link #ignorePropertiesInAudit()} to filter audit properties.
     * </p>
     */
    final static List<String> ignoredProperties = Arrays.asList("password");

    /**
     * Primary key shared with the associated User entity.
     * <p>
     * This field is mapped via {@code @MapsId} to share the same primary key value as {@code User.id},
     * implementing a one-to-one relationship with shared primary key pattern. The value is assigned
     * automatically when the {@code user} relationship is set.
     * </p>
     */
    @Id
    private Long id;

    /**
     * Unique username for authentication.
     * <p>
     * Mapped to a unique database column to enforce login uniqueness across all users.
     * This field serves as the username in Spring Security authentication.
     * </p>
     */
    @Column(unique = true)
    private String login;

    /**
     * BCrypt-encoded password hash.
     * <p>
     * Stores the hashed password for authentication. The plaintext password is encoded using
     * the static {@code passwordEncoder} during entity construction. This field is:
     * <ul>
     *   <li>{@code @JsonIgnore}: Excluded from JSON serialization to prevent exposure in API responses</li>
     *   <li>Excluded from audit logs via {@code ignoredProperties}</li>
     *   <li>Never stored in plaintext form</li>
     * </ul>
     * </p>
     * <p>
     * <b>Warning:</b> The {@link #setPassword(String)} method assigns the value directly without encoding.
     * Use the constructor {@link #LoginAndPassword(String, String, User, boolean)} to ensure proper encoding.
     * </p>
     */
    @JsonIgnore
    private String password;

    /**
     * One-to-one relationship with the User entity.
     * <p>
     * Uses {@code @MapsId} to create a shared primary key relationship, where this entity's {@code id}
     * is derived from the associated {@code User.id}. The {@code @JoinColumn(name = "user_id")} defines
     * the foreign key column, though the actual primary key is shared.
     * </p>
     * <p>
     * {@code @JsonIgnore} prevents circular references and exposure of full User object in API responses.
     * </p>
     */
    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    /**
     * Account enabled/disabled flag for authentication.
     * <p>
     * When {@code false}, the user cannot authenticate even with correct credentials.
     * This flag provides a mechanism to disable accounts without deleting them.
     * </p>
     */
    private boolean enabled;


    /**
     * Static PasswordEncoder instance for BCrypt password hashing.
     * <p>
     * Initialized once at application startup via {@link #setPasswordEncoderOnce(PasswordEncoder)}.
     * Used by the constructor to encode plaintext passwords before persistence.
     * </p>
     * <p>
     * <b>Thread-Safety Note:</b> Initialization is not synchronized. Relies on Spring's
     * single-threaded startup phase to avoid race conditions.
     * </p>
     */
    private static PasswordEncoder passwordEncoder;

    /**
     * Idempotent initialization of the static password encoder.
     * <p>
     * Sets the {@code passwordEncoder} instance used for BCrypt password hashing. This method
     * is called once during application startup to inject the Spring-managed PasswordEncoder bean.
     * Subsequent calls are ignored if the encoder is already initialized.
     * </p>
     * <p>
     * <b>Thread-Safety:</b> This method is NOT thread-safe. It relies on Spring's single-threaded
     * startup phase to ensure only one initialization occurs. Concurrent calls during startup
     * could result in race conditions.
     * </p>
     * <p>
     * <b>Startup Dependency:</b> Must be called before any {@code LoginAndPassword} entity is
     * constructed via {@link #LoginAndPassword(String, String, User, boolean)}, otherwise
     * password encoding will fail with NullPointerException.
     * </p>
     *
     * @param pe the {@link org.springframework.security.crypto.password.PasswordEncoder} instance,
     *           typically a BCryptPasswordEncoder configured by Spring Security
     */
    public static void setPasswordEncoderOnce(PasswordEncoder pe) {
        if (passwordEncoder != null) {
            //Password encoder already initialized
            return;
        }
        LoginAndPassword.passwordEncoder = pe;
    }

    /**
     * Default no-argument constructor for JPA.
     * <p>
     * Required by JPA specification for entity instantiation during database result set mapping.
     * Should not be used directly in application code - use the parameterized constructor instead.
     * </p>
     */
    public LoginAndPassword() {
    }

    /**
     * Creates a LoginAndPassword entity with plaintext password encoding.
     * <p>
     * This constructor encodes the plaintext password using the static {@code passwordEncoder}
     * before persisting it. This is the preferred way to create LoginAndPassword instances
     * as it ensures passwords are properly hashed.
     * </p>
     * <p>
     * <b>Example usage:</b>
     * <pre>{@code
     * User user = new User();
     * LoginAndPassword lap = new LoginAndPassword("john.doe", "securePassword123", user, true);
     * }</pre>
     * </p>
     * <p>
     * <b>Prerequisite:</b> The static {@code passwordEncoder} must be initialized via
     * {@link #setPasswordEncoderOnce(PasswordEncoder)} before calling this constructor,
     * otherwise a NullPointerException will be thrown.
     * </p>
     *
     * @param login         the unique username for authentication, must not be null
     * @param plainPassword the plaintext password to be BCrypt-encoded before storage, must not be null
     * @param user          the associated {@link com.openkoda.model.User} entity, must not be null
     * @param enabled       {@code true} to enable authentication, {@code false} to disable the account
     * @throws NullPointerException if passwordEncoder has not been initialized via setPasswordEncoderOnce
     */
    public LoginAndPassword(String login, String plainPassword, User user, boolean enabled) {
        this.login = login;
        this.password = passwordEncoder.encode(plainPassword);
        this.user = user;
        this.enabled = enabled;
    }

    /**
     * Returns the unique username for authentication.
     *
     * @return the login username, or {@code null} if not yet set
     */
    public String getLogin() {
        return login;
    }

    /**
     * Sets the unique username for authentication.
     * <p>
     * The login must be unique across all LoginAndPassword entities due to the
     * {@code @Column(unique = true)} constraint on the database column.
     * </p>
     *
     * @param login the username to set, should be unique and not null
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Returns the BCrypt-encoded password hash.
     * <p>
     * <b>Security Note:</b> This method is excluded from JSON serialization via {@code @JsonIgnore}.
     * The returned value is a BCrypt hash, not the original plaintext password.
     * </p>
     *
     * @return the encoded password hash, or {@code null} if not yet set
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password field directly without encoding.
     * <p>
     * <b>Warning:</b> This method assigns the value directly without BCrypt encoding.
     * It should only be used when the password is already encoded (e.g., during entity
     * deserialization or updates with pre-hashed passwords).
     * </p>
     * <p>
     * <b>Recommended:</b> For creating new credentials with plaintext passwords, use the
     * constructor {@link #LoginAndPassword(String, String, User, boolean)} which automatically
     * encodes the password via the static {@code passwordEncoder}.
     * </p>
     *
     * @param password the password value to set (should already be BCrypt-encoded for security)
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the associated User entity.
     * <p>
     * This method is excluded from JSON serialization via {@code @JsonIgnore} to prevent
     * circular references and excessive data exposure in API responses.
     * </p>
     *
     * @return the {@link com.openkoda.model.User} entity, or {@code null} if not yet set
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the associated User entity.
     * <p>
     * Due to the {@code @MapsId} annotation, setting this relationship also establishes
     * the shared primary key value. The {@code id} field will be assigned the value of
     * {@code user.getId()} when the entity is persisted.
     * </p>
     *
     * @param user the {@link com.openkoda.model.User} entity to associate, should not be null
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Returns a string representation for audit trail identification.
     * <p>
     * This implementation returns the {@code login} username to identify the authentication
     * record in audit logs without exposing sensitive information like the password.
     * </p>
     *
     * @return the login username for audit identification
     */
    @Override
    public String toAuditString() {
        return login;
    }

    /**
     * Returns the shared primary key with the associated User entity.
     * <p>
     * This ID value is derived from the {@code User.id} via the {@code @MapsId} annotation,
     * implementing a one-to-one relationship with shared primary key pattern. The value is
     * assigned automatically when the entity is persisted with an associated User.
     * </p>
     *
     * @return the primary key, or {@code null} if not yet persisted
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * Returns the list of property names to exclude from audit logging.
     * <p>
     * This implementation returns {@code ["password"]} to ensure sensitive password hashes
     * are never recorded in audit trails, even in encoded form.
     * </p>
     *
     * @return collection containing "password" field name
     */
    @Override
    public Collection<String> ignorePropertiesInAudit() {
        return ignoredProperties;
    }

    /**
     * Returns the account enabled/disabled flag.
     * <p>
     * When {@code false}, Spring Security will reject authentication attempts even if
     * the username and password are correct. This provides a mechanism to temporarily
     * or permanently disable accounts without deletion.
     * </p>
     *
     * @return {@code true} if authentication is enabled, {@code false} if disabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the account enabled/disabled flag for authentication control.
     * <p>
     * Use this method to enable or disable user authentication without removing the
     * LoginAndPassword entity. Disabled accounts retain their credentials but cannot log in.
     * </p>
     *
     * @param enabled {@code true} to enable authentication, {@code false} to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
