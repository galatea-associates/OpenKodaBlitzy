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

package com.openkoda.service.user;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.model.Privilege;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.Token;
import com.openkoda.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;

import java.util.*;

/**
 * Service for generating and managing authentication tokens for users.
 * <p>
 * This service handles the creation, validation, and lifecycle management of various token types
 * used throughout the OpenKoda platform. Tokens authenticate API requests and web sessions,
 * either via GET request parameters or HTTP headers. Each token type serves specific security
 * and authentication purposes with configurable expiration and privilege constraints.
 * 
 * <p>
 * Supported token types and use cases:
 * 
 * <ul>
 * <li><b>Standard Access Tokens:</b> Session tokens for authenticated user requests with full user privileges</li>
 * <li><b>Multiple-Use Tokens:</b> Reusable tokens valid until expiration, useful for API integrations</li>
 * <li><b>Privilege-Restricted Tokens:</b> Tokens with narrowed privilege sets for least-privilege access</li>
 * <li><b>Refresher Tokens:</b> Long-lived tokens (default 30 days) used exclusively to obtain new access tokens</li>
 * <li><b>Single-Use Tokens:</b> One-time tokens invalidated after first use, suitable for password resets</li>
 * </ul>
 * <p>
 * Token lifecycle management includes creation via user entity, validation with privilege checking,
 * automatic expiration based on configured timeouts, and explicit invalidation for single-use scenarios.
 * All tokens are persisted to the database via {@code Token} entity and queried through
 * {@code TokenRepository}.
 * 
 * <p>
 * Security considerations: Refresher tokens are restricted to {@link Privilege#canRefreshTokens}
 * privilege only to prevent privilege escalation. Token validation enforces expiration checks
 * and privilege set verification before granting access.
 * 
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2018-12-14
 * @see Token
 * @see User
 * @see Privilege
 * @see com.openkoda.repository.user.TokenRepository
 */
@Service
public class TokenService extends ComponentProvider {
    
    /**
     * Privilege set for refresher tokens containing only {@link Privilege#canRefreshTokens}.
     * <p>
     * This constant defines the exact privilege set required for refresher tokens to prevent
     * privilege escalation attacks. Refresher tokens must have no other privileges beyond
     * the ability to refresh tokens.
     * 
     */
    private static final Set<Enum> REFRESHER_PRIVILEGE_SET = Collections.singleton(Privilege.canRefreshTokens);

    /**
     * Expiration time in seconds for refresher tokens.
     * <p>
     * Configurable via application property {@code tokens.refresher.expiration}.
     * Default value is 2,592,000 seconds (30 days). Refresher tokens remain valid
     * for this duration unless explicitly revoked.
     * 
     */
    @Value("${tokens.refresher.expiration:2592000}")
    int refresherTokenExpiration;

    /**
     * Creates a reusable token for a user identified by user ID with custom expiration and privileges.
     * <p>
     * This token can be used multiple times within the expiration period. Each request authenticated
     * with this token grants the user only the specified privileges, providing least-privilege access
     * for API integrations or external systems.
     * 
     *
     * @param id the user ID for whom to create the token
     * @param expirationTimeInSeconds the token validity period in seconds from creation time
     * @param allowedPrivileges the privilege set to grant when authenticating with this token,
     *                          restricts user access to only these privileges
     * @return the newly created and persisted {@link Token} entity
     */
    public Token createMultipleUseTokenForUser(Long id, int expirationTimeInSeconds, PrivilegeBase ... allowedPrivileges) {
        debug("[createMultipleUseTokenForUser] {}", id);
        return repositories.unsecure.token.saveAndFlush(new Token(repositories.unsecure.user.findOne(id), true, false, expirationTimeInSeconds, allowedPrivileges));
    }

    /**
     * Creates a reusable token for a user identified by user ID with custom privileges and default expiration.
     * <p>
     * This token can be used multiple times until the default expiration period. The token grants
     * only the specified privileges when used for authentication, suitable for long-term API access
     * with restricted permissions.
     * 
     *
     * @param id the user ID for whom to create the token
     * @param allowedPrivileges the privilege set to grant when authenticating with this token,
     *                          restricts user access to only these privileges
     * @return the newly created and persisted {@link Token} entity with default expiration
     */
    public Token createMultipleUseTokenForUser(Long id, PrivilegeBase ... allowedPrivileges) {
        debug("[createMultipleUseTokenForUser] {}", id);
        return repositories.unsecure.token.saveAndFlush(new Token(repositories.unsecure.user.findOne(id), false, allowedPrivileges));
    }

    /**
     * Creates a standard access token for the specified user with full privileges.
     * <p>
     * This token grants the user all their assigned privileges when used for authentication.
     * Suitable for standard web sessions and trusted API access where full user permissions
     * are required.
     * 
     *
     * @param user the user entity for whom to create the token, must not be null
     * @return the newly created and persisted {@link Token} entity with user's full privileges
     */
    public Token createTokenForUser(User user) {
        debug("[createTokenForUser] {}", user.getId());
        return repositories.unsecure.token.saveAndFlush(new Token(user));
    }

    /**
     * Creates a token for the specified user with restricted privileges for least-privilege access.
     * <p>
     * Once a request is authenticated with this token, the user gains only the specified privileges
     * regardless of their actual role assignments. This enables secure delegation of limited
     * capabilities to external systems or untrusted contexts.
     * 
     *
     * @param user the user entity for whom to create the token, must not be null
     * @param allowedPrivileges the privilege set to grant when authenticating with this token,
     *                          narrows user access to only these privileges
     * @return the newly created and persisted {@link Token} entity with restricted privileges
     */
    public Token createTokenForUser(User user, PrivilegeBase... allowedPrivileges) {
        debug("[createTokenForUser] {} with privileges {}", user.getId(), Arrays.toString(allowedPrivileges));
        return repositories.unsecure.token.saveAndFlush(new Token(user, allowedPrivileges));
    }

    /**
     * Creates a token for the specified user with custom expiration and restricted privileges.
     * <p>
     * Once a request is authenticated with this token, the user gains only the specified privileges
     * for the duration of the expiration period. Useful for time-limited API access with controlled
     * permissions, such as temporary integrations or trial access.
     * 
     *
     * @param user the user entity for whom to create the token, must not be null
     * @param expirationTimeInSeconds the token validity period in seconds from creation time
     * @param allowedPrivileges the privilege set to grant when authenticating with this token,
     *                          narrows user access to only these privileges
     * @return the newly created and persisted {@link Token} entity with expiration and restricted privileges
     */
    public Token createTokenForUser(User user, int expirationTimeInSeconds, PrivilegeBase ... allowedPrivileges) {
        debug("[createTokenForUser] {} with privileges {}", user.getId(), Arrays.toString(allowedPrivileges));
        return repositories.unsecure.token.saveAndFlush(new Token(user, expirationTimeInSeconds, allowedPrivileges));
    }

    /**
     * Verifies a token and immediately invalidates it for single-use scenarios.
     * <p>
     * This method is essential for one-time token workflows such as password reset flows,
     * email verification, or secure action confirmations. The token is queried from the
     * repository, validated for expiration and authenticity, then marked as invalid and
     * persisted immediately via {@code saveAndFlush} to prevent reuse.
     * 
     * <p>
     * Token validation checks include expiration time and validity flag. If the token
     * is found and valid, it is invalidated in memory and immediately flushed to the
     * database before returning.
     * 
     *
     * @param base64UserIdToken the base64-encoded token string to verify and invalidate
     * @return the {@link Token} entity if found and valid before invalidation, or null if
     *         the token was not found, already expired, or previously invalidated
     */
    public Token verifyAndInvalidateToken(String base64UserIdToken) {
        debug("[verifyAndInvalidateToken]");
        Tuple2<Token, String> token = repositories.unsecure.token.findByBase64UserIdTokenIsValidTrue(base64UserIdToken);
        if(token.getT1() != null) {
            debug("[verifyAndInvalidateToken] invalidating token {}", token.getT1().getId());
            repositories.unsecure.token.saveAndFlush(token.getT1().invalidate());
            return token.getT1();
        }
        debug("[verifyAndInvalidateToken] {}", token.getT2());
        return null;
    }

    /**
     * Creates a long-lived refresher token used to obtain short-lived access tokens.
     * <p>
     * Refresher tokens provide a secure mechanism for obtaining new access tokens without
     * requiring the user to re-authenticate. The refresher token has only the
     * {@link Privilege#canRefreshTokens} privilege and a long expiration period (default 30 days,
     * configurable via {@code tokens.refresher.expiration} property).
     * 
     * <p>
     * Refresher tokens are restricted to a single privilege ({@code canRefreshTokens}) to prevent
     * privilege escalation attacks. They cannot be used for regular API calls or data access,
     * only for obtaining new access tokens via {@link #createTokenForRefresher(String)}.
     * 
     *
     * @param user the user entity for whom to create the refresher token, must not be null
     * @return the newly created and persisted refresher {@link Token} entity with long expiration
     * @throws NullPointerException if the supplied user is null
     */
    public Token createRefresherTokenForUser(User user) {
        user = Objects.requireNonNull(user);
        debug("[createRefresherTokenForUser] for user {}", user.getId());
        var token = new Token(user, refresherTokenExpiration, Privilege.canRefreshTokens);
        return repositories.unsecure.token.saveAndFlush(token);
    }

    /**
     * Creates a new access token from a valid refresher token after strict validation.
     * <p>
     * This method validates that the refresher token's privilege set exactly equals
     * {@link #REFRESHER_PRIVILEGE_SET} (containing only {@link Privilege#canRefreshTokens})
     * to prevent privilege escalation attacks. If the refresher token has any additional
     * privileges, it is rejected as invalid.
     * 
     * <p>
     * Security enforcement: The method rejects refresher tokens with privileges beyond
     * {@code canRefreshTokens} to ensure that compromised refresher tokens cannot be used
     * for unauthorized access. Only tokens with the exact expected privilege set are accepted.
     * 
     * <p>
     * Upon successful validation, a new standard access token is created for the user
     * with their full privileges and default expiration.
     * 
     *
     * @param refresherTokenBase64 the base64-encoded refresher token string, must not be null
     * @return a newly generated standard access {@link Token} with user's full privileges,
     *         or null if the refresher token is invalid, expired, or has incorrect privileges
     * @throws NullPointerException if refresherTokenBase64 is null
     */
    public Token createTokenForRefresher(String refresherTokenBase64) {
        debug("[createTokenForRefresher]");
        refresherTokenBase64 = Objects.requireNonNull(refresherTokenBase64);
        Tuple2<Token, String> tokenTuple = repositories.unsecure.token.findByBase64UserIdTokenIsValidTrue(refresherTokenBase64);
        Optional<Token> tokenRefresher = Optional.ofNullable(tokenTuple.getT1());
        return tokenRefresher.filter(token -> token.getPrivilegesSet().equals(REFRESHER_PRIVILEGE_SET)).map(token -> {
            var user = tokenRefresher.get().getUser();
            var tokenToReturn = new Token(user);
            repositories.unsecure.token.saveAndFlush(tokenToReturn);
            return tokenToReturn;
        }).orElseGet(() -> {
            debug("[createTokenForRefresher] refresher token invalid: {}", tokenTuple.getT2());
            return null;
        });
    }
}
