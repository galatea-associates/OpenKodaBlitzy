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
 * JPA entity for programmatic API key authentication credentials.
 * <p>
 * This entity enables machine-to-machine authentication for REST API access without
 * requiring username/password credentials. API keys are BCrypt-encoded for security
 * and are excluded from JSON serialization and audit logs to prevent exposure.

 * <p>
 * Persisted to 'api_key' table with a shared primary key relationship to the User entity,
 * establishing a one-to-one association via the {@code @MapsId} pattern. This ensures
 * each API key is uniquely associated with exactly one User account.

 * <p>
 * Security considerations:
 * <ul>
 *   <li>API keys are BCrypt-encoded via static PasswordEncoder</li>
 *   <li>{@code @JsonIgnore} annotations prevent JSON exposure of sensitive fields</li>
 *   <li>API key field is excluded from audit logs via {@link #ignorePropertiesInAudit()}</li>
 * </ul>

 * <p>
 * Thread-safety note: The static {@code passwordEncoder} initialization is not synchronized,
 * creating a potential startup ordering dependency. Ensure {@link #setPasswordEncoderOnce(PasswordEncoder)}
 * is called during application initialization before any ApiKey instances are created.

 * <p>
 * {@code @DynamicUpdate} annotation enables Hibernate optimization to update only modified
 * columns in UPDATE statements, reducing database overhead.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see User
 * @see LoggedUser
 * @see PasswordEncoder
 */
@Entity

@DynamicUpdate
@Table(name = "api_key")
public class ApiKey extends LoggedUser {

    /**
     * List of field names excluded from audit logging.
     * <p>
     * Contains "apiKey" to prevent the BCrypt-encoded API key hash from appearing
     * in audit trails for security purposes.

     */
    final static List<String> ignoredProperties = Arrays.asList("apiKey");

    /**
     * Primary key shared with User entity via {@code @MapsId}.
     * <p>
     * This ID is identical to the associated User's ID, establishing the shared
     * primary key relationship pattern for one-to-one associations.

     */
    @Id
    private Long id;

    /**
     * BCrypt-encoded API key hash.
     * <p>
     * This field stores the encoded hash of the API key, not the plaintext value.
     * The {@code @JsonIgnore} annotation prevents this sensitive field from being
     * included in JSON serialization, and it is excluded from audit logs via
     * {@link #ignorePropertiesInAudit()}.

     * <p>
     * Warning: Use {@link #setPlainApiKey(String)} to encode and set a new API key.
     * The {@link #setApiKey(String)} method assigns the value directly without encoding.

     */
    @JsonIgnore
    private String apiKey;

    /**
     * One-to-one relationship with User entity via shared primary key.
     * <p>
     * The {@code @MapsId} annotation indicates this relationship uses the User's ID
     * as this entity's primary key. The {@code @JoinColumn(name="user_id")} specifies
     * the foreign key column name in the api_key table.

     * <p>
     * {@code @JsonIgnore} prevents circular reference issues during JSON serialization.

     */
    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    /**
     * Static PasswordEncoder instance for BCrypt encoding of API keys.
     * <p>
     * This encoder is initialized once during application startup via
     * {@link #setPasswordEncoderOnce(PasswordEncoder)}. It is used by the constructor
     * and {@link #setPlainApiKey(String)} to encode plaintext API keys before storage.

     * <p>
     * Thread-safety note: Initialization is not synchronized. Ensure this is set
     * during single-threaded application startup before any ApiKey instances are created.

     */
    private static PasswordEncoder passwordEncoder;

    /**
     * Idempotent initialization of the static PasswordEncoder instance.
     * <p>
     * This method sets the static {@code passwordEncoder} field if it has not been
     * initialized. Subsequent calls are ignored, making this method safe to call
     * multiple times during application startup.

     * <p>
     * Thread-safety warning: This method is NOT thread-safe. It should only be called
     * during single-threaded application initialization before any ApiKey instances
     * are created or any concurrent access occurs.

     *
     * @param pe the PasswordEncoder to use for BCrypt encoding of API keys
     */
    public static void setPasswordEncoderOnce(PasswordEncoder pe) {
        if (passwordEncoder != null) {
            //Password encoder already initialized
            return;
        }
        ApiKey.passwordEncoder = pe;
    }

    /**
     * No-argument constructor for JPA entity instantiation.
     * <p>
     * Required by JPA specification for entity lifecycle management. This constructor
     * is used by Hibernate when loading entities from the database.

     */
    public ApiKey() {
    }

    /**
     * Creates an ApiKey entity with plaintext API key encoding.
     * <p>
     * The plaintext API key is immediately encoded using the static {@code passwordEncoder}
     * via BCrypt hashing before storage. The encoded hash is stored in the {@link #apiKey}
     * field, not the plaintext value.

     * <p>
     * Precondition: The static {@code passwordEncoder} must be initialized via
     * {@link #setPasswordEncoderOnce(PasswordEncoder)} before calling this constructor,
     * otherwise a NullPointerException will be thrown.

     *
     * @param plainApiKey the plaintext API key to encode and store
     * @param user the User entity to associate with this API key via shared primary key
     * @throws NullPointerException if passwordEncoder has not been initialized
     */
    public ApiKey(String plainApiKey, User user) {
        this.apiKey = passwordEncoder.encode(plainApiKey);
        this.user = user;
    }

    /**
     * Returns the User entity associated with this API key.
     * <p>
     * This relationship is established via the {@code @MapsId} shared primary key pattern,
     * ensuring a one-to-one association between ApiKey and User.

     *
     * @return the associated User entity
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the User entity associated with this API key.
     * <p>
     * This establishes the one-to-one relationship between ApiKey and User via
     * shared primary key.

     *
     * @param user the User entity to associate with this API key
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Returns a string representation of this entity for audit trail purposes.
     * <p>
     * Provides the entity ID as a string for audit logging. This method is part
     * of the {@link com.openkoda.model.common.AuditableEntity} contract.

     *
     * @return the string representation of the ID for audit trails
     */
    @Override
    public String toAuditString() {
        return id + "";
    }

    /**
     * Returns the BCrypt-encoded API key hash.
     * <p>
     * This method returns the encoded hash value, not the plaintext API key.
     * The value is suitable for comparison with user-provided API keys using
     * PasswordEncoder's matches() method.

     *
     * @return the BCrypt-encoded API key hash
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets the API key value directly without encoding.
     * <p>
     * <strong>Warning:</strong> This method assigns the provided value directly to the
     * {@link #apiKey} field without BCrypt encoding. Use {@link #setPlainApiKey(String)}
     * instead if you need to encode a plaintext API key before storage.

     * <p>
     * This method is typically used only when setting an already-encoded hash value,
     * such as during entity deserialization or data migration.

     *
     * @param apiKey the API key value to set (should be pre-encoded)
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Encodes and stores a plaintext API key using BCrypt hashing.
     * <p>
     * This method encodes the provided plaintext API key via the static
     * {@code passwordEncoder} and stores the resulting hash in the {@link #apiKey}
     * field. This is the recommended method for updating an API key with a new
     * plaintext value.

     * <p>
     * Precondition: The static {@code passwordEncoder} must be initialized via
     * {@link #setPasswordEncoderOnce(PasswordEncoder)} before calling this method,
     * otherwise a NullPointerException will be thrown.

     *
     * @param plainApiKey the plaintext API key to encode and store
     * @throws NullPointerException if passwordEncoder has not been initialized
     */
    public void setPlainApiKey(String plainApiKey) {
        this.apiKey = passwordEncoder.encode(plainApiKey);
    }

    /**
     * Returns the primary key shared with the User entity.
     * <p>
     * This ID is identical to the associated User's ID due to the {@code @MapsId}
     * shared primary key relationship pattern.

     *
     * @return the primary key value
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * Sets the primary key shared with the User entity.
     * <p>
     * This ID should match the associated User's ID to maintain the shared
     * primary key relationship established by {@code @MapsId}.

     *
     * @param id the primary key value
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the collection of field names to exclude from audit logging.
     * <p>
     * This method is part of the {@link com.openkoda.model.common.AuditableEntity}
     * contract. It returns a list containing "apiKey" to prevent the BCrypt-encoded
     * API key hash from appearing in audit trails for security purposes.

     *
     * @return a collection containing "apiKey" to exclude from audit logs
     */
    @Override
    public Collection<String> ignorePropertiesInAudit() {
        return ignoredProperties;
    }
}