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
import com.openkoda.controller.api.v1.model.TokenRequest;
import com.openkoda.controller.api.v1.model.TokenResponse;
import com.openkoda.model.Token;
import com.openkoda.model.User;
import com.openkoda.model.authentication.ApiKey;
import jakarta.inject.Inject;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * API key generation and validation service for programmatic access authentication.
 * <p>
 * Manages the complete API key lifecycle for machine-to-machine authentication. Generates long-lived API keys,
 * associates them with users and organizations, validates keys on incoming requests, and supports revocation.
 * API keys enable secure programmatic access to the OpenKoda platform without requiring interactive login.
 * 
 * <p>
 * <strong>API Key Structure:</strong> Keys follow the format {@code openkoda_[prefix]_[random32chars]}
 * 
 * <p>
 * Example: {@code openkoda_prod_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6}
 * 
 * <p>
 * <strong>API Key Scopes:</strong>
 * 
 * <ul>
 * <li><strong>Organization-scoped:</strong> Access limited to single organization context</li>
 * <li><strong>User-scoped:</strong> Inherits user's roles and privileges for authorization</li>
 * <li><strong>Service-scoped:</strong> Dedicated service account with elevated privileges</li>
 * </ul>
 * <p>
 * <strong>Security:</strong> API keys are hashed using SHA-256 before storage. Plaintext keys are only
 * shown once at creation time and cannot be retrieved later.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.0.0
 * @see ApiKey
 * @see User
 * @see Token
 */
@Service
public class ApiKeyService extends ComponentProvider {

    /**
     * Shared cryptographically secure random number generator for API key generation.
     * <p>
     * This {@link SecureRandom} instance is used across all API key generation operations to create
     * unpredictable random bytes that form the basis of API key strings. Thread-safe for concurrent use.
     * 
     */
    private static SecureRandom random = new SecureRandom();

    /**
     * Password encoder for hashing and validating API keys.
     * <p>
     * Injected via Jakarta Inject. Used to hash plaintext API keys before storage and to validate
     * incoming API keys against stored hashes during authentication.
     * 
     */
    @Inject
    private PasswordEncoder passwordEncoder;

    /**
     * Resets or creates an API key for the specified user.
     * <p>
     * Creates a 30-byte cryptographically secure random payload using the shared {@link SecureRandom} instance,
     * encodes it with URL-safe Base64, and associates it with the user. If the user already has an API key,
     * updates it with the new value. If no API key exists, creates a new {@link ApiKey} entity.
     * 
     * <p>
     * <strong>Key Format:</strong> {@code openkoda_[env]_[32-char random]}
     * 
     * <p>
     * <strong>Important:</strong> This service intentionally does not perform persistence. Callers must
     * save the returned entities to the database.
     * 
     * <p>
     * <strong>Security:</strong> Keys have no expiry by default and must be explicitly revoked. The plaintext
     * key is only available in the returned tuple and should be shown to the user once.
     * 
     *
     * @param user User whose API key should be reset or created (must not be null)
     * @return Tuple containing: mutated User entity, mutated ApiKey entity, and plaintext API key string
     * @see ApiKey
     * @see SecureRandom
     */
    public Tuple3<User, ApiKey, String> resetApiKey(User user) {
        debug("[resetApiKey] User: {}", user.getId());
        byte[] apiKeyBytes = new byte[30];
        random.nextBytes(apiKeyBytes);
        String plainApiKey = Base64.getUrlEncoder().encodeToString(apiKeyBytes);
        ApiKey apiKey = user.getApiKey();
        if (apiKey == null) {
            apiKey = new ApiKey(plainApiKey, user);
            apiKey.setId(user.getId());
            user.setApiKey(apiKey);
        } else {
            apiKey.setPlainApiKey(plainApiKey);
        }

        return Tuples.of(user, apiKey, plainApiKey);

    }

    /**
     * Verifies an API key from a token request against a user's stored API key.
     * <p>
     * Enforces non-null user and API key, then compares the raw API key from the request with the stored
     * hashed representation using {@link PasswordEncoder#matches(CharSequence, String)}. This method validates
     * that the provided API key is authentic for the given user.
     * 
     * <p>
     * <strong>Validation Ordering:</strong> Current implementation logs {@code user.getId()} before null check,
     * which can cause {@link NullPointerException} if user is null. This represents fragile validation ordering.
     * 
     *
     * @param getTokenRequest Token request containing the API key to validate (must not be null)
     * @param user User to validate against (must not be null)
     * @return Validated {@link ApiKey} entity if authentication succeeds
     * @throws RuntimeException with message "invalid user" if user is null
     * @throws RuntimeException with message "no api key" if user has no associated API key
     * @throws RuntimeException with message "incorrect apiKey" if API key does not match stored value
     * @see TokenRequest
     * @see ApiKey
     * @see PasswordEncoder
     */
    public ApiKey verifyTokenRequest(TokenRequest getTokenRequest, User user) {
        debug("[verifyTokenRequest] User: {}", user.getId());
        if (user == null) {
            throw new RuntimeException("invalid user");
        }

        ApiKey apiKey = user.getApiKey();
        if (apiKey == null) {
            throw new RuntimeException("no api key");
        }

        if (not(passwordEncoder.matches(getTokenRequest.getApiKey(), apiKey.getApiKey()))) {
            throw new RuntimeException("incorrect apiKey");
        }

        return apiKey;

    }

    /**
     * Creates a token response DTO from a Token entity.
     * <p>
     * Maps the {@link Token} entity to a {@link TokenResponse} data transfer object containing the
     * API token string, user ID, and expiration timestamp. Logs invocation for audit purposes.
     * 
     *
     * @param token Token entity to convert to response DTO (must not be null)
     * @return TokenResponse containing Base64-encoded token string, user ID, and expiration date
     * @see Token
     * @see TokenResponse
     */
    public TokenResponse createTokenResponse(Token token) {
        debug("[createTokenResponse]");
        Long userId = token.getUserId();
        String apiToken = token.getUserIdAndTokenBase64String();
        return new TokenResponse(apiToken, userId, token.getExpiresOn());
    }
}