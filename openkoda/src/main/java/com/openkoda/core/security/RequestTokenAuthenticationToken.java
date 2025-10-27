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

package com.openkoda.core.security;

import com.openkoda.model.PrivilegeBase;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Collections;
import java.util.Set;

/**
 * Immutable Authentication carrier for token-based authentication flows in OpenKoda.
 * <p>
 * This token extends {@link UsernamePasswordAuthenticationToken} to support API tokens,
 * single-use tokens, and path-prefix tokens. It holds the raw token string, user ID,
 * privilege restrictions, and single-request flag.
 * </p>
 * <p>
 * Created unauthenticated ({@code setAuthenticated(false)}) by authentication filters
 * and consumed by {@code LoginByPasswordOrTokenAuthenticationProvider}. The Token entity
 * can limit but not extend user privileges, enabling privilege narrowing for token-based access.
 * </p>
 * <p>
 * Thread-safe through immutability: the privileges Set is wrapped with
 * {@link Collections#unmodifiableSet(Set)} to prevent modification.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * RequestTokenAuthenticationToken token = new RequestTokenAuthenticationToken(
 *     userId, email, rawToken, privilegeSet, false);
 * </pre>
 * </p>
 *
 * @see AbstractTokenAuthenticationFilter
 * @see com.openkoda.model.Token
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class RequestTokenAuthenticationToken extends UsernamePasswordAuthenticationToken {


    private final String token;

    private final Long userId;

    private final Set<PrivilegeBase> privileges;

    private final boolean singleRequest;

    /**
     * Creates an unauthenticated token for token-based authentication.
     * <p>
     * Constructs a token with user ID, email principal, raw token string, privilege set,
     * and single-request flag. The privileges Set is defensively copied to an unmodifiable
     * Set for immutability and thread-safety.
     * </p>
     *
     * @param userId Database ID of user associated with this token (nullable for invalid tokens)
     * @param email User email used as principal name in Spring Security context
     * @param token Raw token string extracted from request (header, parameter, or path prefix)
     * @param privileges Set of privileges granted by this token (may be subset of user's full privileges for privilege narrowing)
     * @param singleRequest Flag indicating token should be invalidated after successful authentication (single-use tokens)
     */
    public RequestTokenAuthenticationToken(Long userId, String email, String token, Set<PrivilegeBase> privileges, boolean singleRequest) {
        super(email, null);
        this.token = token;
        this.userId = userId;
        this.privileges = Collections.unmodifiableSet(privileges);
        this.singleRequest = singleRequest;
        setAuthenticated(false);
    }

    /**
     * Returns the raw token string extracted from HTTP request.
     *
     * @return Raw token string from header, parameter, or path prefix
     */
    public String getToken() {
        return token;
    }

    /**
     * Returns the database ID of user associated with this token.
     *
     * @return Database user ID, or null for invalid tokens
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Returns unmodifiable Set of privilege restrictions for token-based authentication.
     * <p>
     * This Set represents narrowed privileges granted by the token, which may be
     * a subset of the user's full privileges for restricted token-based access.
     * </p>
     *
     * @return Unmodifiable Set of PrivilegeBase restrictions
     */
    public Set<PrivilegeBase> getPrivileges() {
        return privileges;
    }

    /**
     * Returns true if token should be invalidated after use.
     * <p>
     * Single-use tokens are invalidated after successful authentication,
     * implementing the single-request token pattern for enhanced security.
     * </p>
     *
     * @return True for single-use tokens, false for reusable tokens
     */
    public boolean isSingleRequest() {
        return singleRequest;
    }

    /**
     * Returns true if token grants any privileges.
     * <p>
     * Convenience method to check if the privilege set is empty,
     * useful for validating token authorization scope.
     * </p>
     *
     * @return True if privileges are granted, false if privilege set is empty
     */
    public boolean hasPrivileges() {
        return !privileges.isEmpty();
    }
}
