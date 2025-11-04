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

package com.openkoda.model;

import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.model.common.AuditableEntity;
import com.openkoda.model.common.TimestampedEntity;
import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Formula;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Short-lived authentication token entity for password reset, email verification, and API access with privilege-based authorization.
 * <p>
 * Persisted to 'tokens' table. Represents temporary authentication tokens generated via {@link SecureRandom} (61 bytes, URL-safe Base64 encoded).
 * Supports single-use and single-request token consumption modes for enhanced security. Tokens have configurable expiration (default 2 days from creation)
 * and can carry specific privileges for fine-grained authorization. Used for password reset flows (canRecoverPassword privilege),
 * email verification (canVerifyAccount privilege), and stateless API authentication. References {@link User} via userId foreign key.

 * <p>
 * Security characteristics: 61-byte {@link SecureRandom} generation provides approximately 488 bits of entropy for cryptographic strength.
 * Tokens include privileges string for authorization checks enabling token-scoped permissions that can limit (but not extend) user privileges.
 * SingleUse flag enforces one-time consumption for password resets and account verification workflows. SingleRequest flag enforces
 * single HTTP request usage with immediate expiration for enhanced security in sensitive operations.

 * <p>
 * Token lifecycle: Default expiration is createdOn + 2 days (configurable via constructor). Expired tokens are automatically invalidated
 * via {@link #isValid} @Formula computed field (used = false AND expires_on > current_timestamp). Tokens are removed post-consumption
 * if singleUse=true via {@link #invalidateIfSingleUse()} method.

 * <p>
 * Example usage:
 * <pre>
 * Token resetToken = new Token(user, true, Privilege.canRecoverPassword);
 * tokenRepository.save(resetToken);
 * </pre>

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see User for token owner entity
 * @see Privilege for token privilege enumeration
 * @see PrivilegeHelper for privilege serialization utilities
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "token"}))
public class Token extends TimestampedEntity implements AuditableEntity {

    /**
     * Secure random number generator for cryptographically strong token generation.
     * Shared across all Token instances for efficient entropy gathering.
     */
    private static SecureRandom random = new SecureRandom();
    
    /**
     * Default token expiration time in seconds: 2 days (172,800 seconds).
     * Used when token is created without explicit expiration time parameter.
     */
    private static final int DEFAULT_EXPIRATION_TIME_IN_SECONDS = 2 * 24 * 3600; //2 days
    
    /**
     * List of property names to exclude from audit trail logging.
     * Token value excluded for security - prevents sensitive credentials from appearing in audit logs.
     */
    final static List<String> ignoredProperties = Arrays.asList("token");
    
    /**
     * Creates token with full configuration: single-request, single-use, custom expiration, and specific privileges.
     * Combines all token security modes for maximum control over token lifecycle and permissions.
     *
     * @param u the {@link User} who owns this token, must not be null
     * @param singleRequest if true, token expires after single HTTP request
     * @param singleUse if true, token invalidates after first successful use
     * @param expirationTimeInSeconds token lifetime in seconds from creation
     * @param privileges optional array of {@link PrivilegeBase} privileges to grant to token bearer,
     *                   if empty or null, all user privileges are inherited
     */
    public Token(User u, boolean singleRequest, boolean singleUse, int expirationTimeInSeconds, PrivilegeBase ... privileges) {
        this(u, expirationTimeInSeconds, privileges);
        this.singleUse = singleUse;
        this.singleRequest = singleRequest;
    }

    /**
     * Creates single-use token with specific privileges and default 2-day expiration.
     * Used for password reset and email verification workflows requiring one-time token usage.
     *
     * @param u the {@link User} who owns this token, must not be null
     * @param singleUse if true, token invalidates after first successful use
     * @param privileges optional array of {@link PrivilegeBase} privileges to grant to token bearer
     */
    public Token(User u, boolean singleUse, PrivilegeBase ... privileges) {
        this(u, privileges);
        this.singleUse = singleUse;
    }

    /**
     * Creates token for user with default 2-day expiration, single-use enabled, and all user privileges inherited.
     * Simplest constructor for basic token generation.
     *
     * @param u the {@link User} who owns this token, must not be null
     */
    public Token(User u) {
        this();
        user = u;
        userId = user.getId();
    }

    /**
     * Creates token for user with custom expiration time and all user privileges inherited.
     *
     * @param u the {@link User} who owns this token, must not be null
     * @param expirationTimeInSeconds token lifetime in seconds from creation
     */
    public Token(User u, int expirationTimeInSeconds) {
        this(expirationTimeInSeconds);
        user = u;
        userId = user.getId();
    }

    /**
     * Creates token for user with custom expiration time and specific privileges.
     * Enables token-scoped permission restrictions beyond user's granted privileges.
     *
     * @param u the {@link User} who owns this token, must not be null
     * @param expirationTimeInSeconds token lifetime in seconds from creation
     * @param privileges optional array of {@link PrivilegeBase} privileges to grant to token bearer,
     *                   any privilege user doesn't have is ignored
     */
    public Token(User u, int expirationTimeInSeconds, PrivilegeBase ... privileges) {
        this(u, expirationTimeInSeconds);
        this.privileges = PrivilegeHelper.toJoinedStringInParenthesis(privileges);
    }

    /**
     * Creates token for user with default 2-day expiration and specific privileges.
     * Commonly used for API access tokens with limited permission scope.
     *
     * @param u the {@link User} who owns this token, must not be null
     * @param privileges optional array of {@link PrivilegeBase} privileges to grant to token bearer
     */
    public Token(User u, PrivilegeBase ... privileges) {
        this(u);
        this.privileges = PrivilegeHelper.toJoinedStringInParenthesis(privileges);
    }

    /**
     * Creates token with custom expiration time and cryptographically generated token value.
     * Generates 61-byte SecureRandom value encoded as URL-safe Base64 string (~81 characters).
     * Initializes token as unused with expiration time = now + expirationTimeInSeconds.
     *
     * @param expirationTimeInSeconds token lifetime in seconds from creation
     */
    public Token(int expirationTimeInSeconds) {
        used = false;
        byte[] tokenBytes = new byte[61];
        random.nextBytes(tokenBytes);
        token = Base64.getUrlEncoder().encodeToString(tokenBytes);
        expiresOn = LocalDateTime.now().plusSeconds(expirationTimeInSeconds);
    }

    /**
     * Creates token with default 2-day expiration and cryptographically generated token value.
     * Default constructor delegates to {@link #Token(int)} with DEFAULT_EXPIRATION_TIME_IN_SECONDS (2 days).
     */
    public Token() {
        this(DEFAULT_EXPIRATION_TIME_IN_SECONDS);
    }

    /**
     * Primary key generated from seqGlobalId sequence (initial value 10000, allocationSize 10).
     * Uses batch allocation for performance optimization.
     */
    @Id
    @SequenceGenerator(name = GLOBAL_ID_GENERATOR, sequenceName = GLOBAL_ID_GENERATOR, initialValue = INITIAL_GLOBAL_VALUE, allocationSize = 10)
    @GeneratedValue(generator = GLOBAL_ID_GENERATOR, strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Many-to-one reference to the {@link User} who owns this token.
     * Eager-loaded for authentication and authorization checks.
     * TODO Rule 4.4: should be marked with @JsonIgnore and FetchType = LAZY
     */
    //TODO Rule 4.4: should be marked with @JsonIgnore and FetchType = LAZY
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Foreign key to users table. Owner of this token.
     * Non-insertable and non-updatable - automatically synchronized with user association.
     * Used for queries without loading full User entity.
     */
    @Column(insertable = false, updatable = false, name = "user_id")
    private Long userId;

    /**
     * 61-byte {@link SecureRandom} value, URL-safe Base64 encoded (~81 characters).
     * Serves as unique token identifier and bearer credential providing approximately 488 bits of entropy.
     * Unique constraint enforced via table-level @UniqueConstraint on (user_id, token) columns.
     */
    private String token;

    /**
     * Flag indicating whether token has been consumed/invalidated.
     * Set to true via {@link #invalidate()} or {@link #invalidateIfSingleUse()} methods.
     * Used in {@link #isValid} @Formula computation.
     */
    @Column(name = "used")
    private boolean used;

    /**
     * If true, token is invalidated immediately after first successful use.
     * Used for password reset and email verification workflows where single-use tokens prevent replay attacks.
     * Default value: true (single-use enabled by default).
     */
    @Column(name = "single_use")
    private boolean singleUse = true;

    /**
     * If true, token expires after single HTTP request regardless of result.
     * Enhanced security mode for sensitive operations requiring immediate token expiration.
     * Default value: false (single-request mode disabled by default).
     */
    @Column(name = "single_request")
    private boolean singleRequest = false;

    /**
     * Token expiration timestamp. Tokens are invalid after this time.
     * Default: createdOn + 2 days (DEFAULT_EXPIRATION_TIME_IN_SECONDS).
     * Configurable via constructor parameter expirationTimeInSeconds.
     * Expired tokens automatically rejected by authentication via {@link #isValid} @Formula.
     */
    @Column(name = "expires_on")
    private LocalDateTime expiresOn;

    /**
     * Computed boolean field indicating token validity.
     * Database formula expression: (used = false AND expires_on > current_timestamp)
     * Returns true if token is unused and not yet expired, false otherwise.
     * Database-computed field - not insertable/updatable from application code.
     */
    @Formula("(used = false AND expires_on > current_timestamp)")
    private boolean isValid;

    /**
     * Transient in-memory set of {@link PrivilegeBase} privileges carried by this token.
     * Lazy-initialized from {@link #privileges} string on first {@link #getPrivilegesSet()} call
     * via {@link PrivilegeHelper#fromJoinedStringInParenthesisToPrivilegeEnumSet(String)}.
     * Not persisted directly - derived from privileges column.
     * Enables token-scoped authorization by restricting user privileges during token authentication.
     */
    @Transient
    private Set<PrivilegeBase> privilegesSet;

    /**
     * Serialized privilege string granting specific permissions to token bearer.
     * Format: '(privilege1)(privilege2)' per {@link PrivilegeHelper} serialization convention.
     * Maximum length: 65535 characters for large privilege sets.
     * <p>
     * Token authentication can limit (but not extend) the privileges given to the user.
     * If privileges == null, then all user's privileges are granted during token authentication.
     * Any privilege that user does not have is ignored - tokens cannot escalate user privileges.

     * Null value: Token inherits all user privileges without restrictions.
     */
    @Column(length = 65535)
    private String privileges;

    /**
     * Returns lazy-initialized set of privileges carried by this token.
     * On first call, deserializes {@link #privileges} string via {@link PrivilegeHelper#fromJoinedStringInParenthesisToPrivilegeEnumSet(String)}.
     * Subsequent calls return cached privilegesSet without re-parsing.
     * <p>
     * If privileges string is null, returns empty set (token inherits all user privileges).
     * Privilege set enables token-scoped authorization checks during authentication.

     *
     * @return set of {@link PrivilegeBase} privileges, never null (returns empty set if no privileges defined)
     * @see PrivilegeHelper for privilege serialization/deserialization utilities
     */
    public Set<PrivilegeBase> getPrivilegesSet() {
        if ( privilegesSet == null ) {
            privilegesSet = PrivilegeHelper.fromJoinedStringInParenthesisToPrivilegeEnumSet( privileges );
        }
        return privilegesSet;
    }

    /**
     * Checks if token has explicit privilege restrictions.
     * Returns false if token inherits all user privileges (privileges == null).
     * Returns true if token carries specific privilege set limiting user permissions.
     *
     * @return true if token has explicit privileges string, false if privileges is null
     */
    public boolean hasPrivileges() {
        return privileges != null;
    }

    /**
     * Marks token as used/invalidated by setting used flag to true.
     * Once invalidated, token will fail {@link #isValid} @Formula check (used = false AND expires_on > current_timestamp).
     * Invalidated tokens are rejected by authentication regardless of expiration time.
     * <p>
     * Fluent API design returns this token instance for method chaining.

     *
     * @return this {@link Token} instance for method chaining
     */
    public Token invalidate() {
        used = true;
        return this;
    }

    /**
     * Conditionally invalidates token if {@link #singleUse} flag is true.
     * Used after successful token authentication to enforce single-use semantics for password reset and verification flows.
     * No-op if singleUse is false (multi-use tokens remain valid until expiration).
     * <p>
     * Fluent API design returns this token instance for method chaining.

     *
     * @return this {@link Token} instance for method chaining
     * @see #invalidate() for unconditional invalidation
     */
    public Token invalidateIfSingleUse() {
        if (singleUse) {
            invalidate();
        }
        return this;
    }

    /**
     * Returns the primary key identifier for this token.
     *
     * @return token ID generated from seqGlobalId sequence, may be null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the user who owns this token.
     * Many-to-one association eager-loaded for authentication and authorization.
     *
     * @return {@link User} entity owning this token, may be null for detached token instances
     */
    public User getUser() {
        return user;
    }

    /**
     * Returns the URL-safe Base64-encoded token value serving as bearer credential.
     * 61-byte SecureRandom value encoded to approximately 81 character string.
     * This value is sent to users via email/URL and presented back during authentication.
     *
     * @return token string value used for authentication, never null after construction
     */
    public String getToken() {
        return token;
    }

    /**
     * Checks if token has been consumed/invalidated.
     * Tokens marked as used will fail {@link #isValid} validation regardless of expiration time.
     *
     * @return true if token has been invalidated via {@link #invalidate()}, false if still available
     */
    public boolean isUsed() {
        return used;
    }

    /**
     * Returns the timestamp when this token expires.
     * After this time, token will fail {@link #isValid} @Formula check and be rejected by authentication.
     *
     * @return expiration timestamp (createdOn + expiration seconds), never null after construction
     */
    public LocalDateTime getExpiresOn() {
        return expiresOn;
    }

    /**
     * Checks if token is currently valid for authentication.
     * Computed field via @Formula annotation: (used = false AND expires_on &gt; current_timestamp)
     * Token is valid only if unused and not yet expired.
     *
     * @return true if token can be used for authentication, false if invalidated or expired
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Returns the serialized privilege string in '(privilege1)(privilege2)' format.
     * Null if token inherits all user privileges without restrictions.
     *
     * @return privilege string or null if no privilege restrictions
     * @see #getPrivilegesSet() for deserialized privilege set
     */
    public String getPrivileges() {
        return privileges;
    }

    /**
     * Returns the foreign key ID of the user who owns this token.
     * Non-insertable/non-updatable column automatically synchronized with user association.
     * Used for efficient queries without loading full User entity.
     *
     * @return user ID foreign key, may be null if user not set
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Returns Base64-encoded string combining userId and token value separated by colon.
     * Format after encoding: Base64(userId:token)
     * Used for compact token transmission in URLs or headers where both user and token must be carried together.
     *
     * @return Base64-encoded "userId:token" string for combined authentication credential
     */
    public String getUserIdAndTokenBase64String() {
        return Base64.getEncoder().encodeToString(StringUtils.join(userId, ":", token).getBytes());
    }

    /**
     * Checks if token expires after single HTTP request.
     * Single-request tokens are invalidated immediately after first request regardless of result,
     * providing enhanced security for sensitive operations.
     *
     * @return true if single-request mode enabled, false for standard multi-request tokens
     */
    public boolean isSingleRequest() {
        return singleRequest;
    }

    /**
     * Returns list of property names to exclude from audit trail logging.
     * Implements {@link AuditableEntity#ignorePropertiesInAudit()}.
     * Token value excluded for security - prevents sensitive credentials from appearing in audit logs.
     *
     * @return collection containing "token" property name for audit exclusion
     */
    @Override
    public Collection<String> ignorePropertiesInAudit() {
        return ignoredProperties;
    }

    /**
     * Returns string representation of this token for audit trail entries.
     * Implements {@link AuditableEntity#toAuditString()}.
     * Returns token ID only - token value excluded for security.
     *
     * @return audit string containing token ID in format " ID: {id}"
     */
    @Override
    public String toAuditString() {
        return " ID: " + id;
    }
}
