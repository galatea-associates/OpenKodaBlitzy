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

package com.openkoda.controller.api.auth;

import com.openkoda.controller.api.ApiAttributes;
import com.openkoda.controller.api.v1.model.RefreshTokenRequest;
import com.openkoda.controller.api.v1.model.RefresherTokenRequest;
import com.openkoda.controller.api.v1.model.TokenRequest;
import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageModelMap;

/**
 * Abstract helper centralizing token-related Flow orchestration for API v1 authentication endpoints.
 * <p>
 * This class provides protected Flow-returning methods for token operations used by concrete REST controllers
 * such as {@link TokenControllerApiV1}. It is NOT request-mapped itself - subclasses expose HTTP endpoints
 * and delegate business logic to these protected methods. Extends {@link AbstractController} for injected
 * services and repositories access, and implements {@link ApiAttributes} for standard flow key constants
 * like {@code tokenResponse} and {@code userEntity}.
 * </p>
 * <p>
 * <b>Design Characteristics:</b>
 * <ul>
 *   <li>Stateless: Contains no instance fields; per-request state carried in Flow/PageModelMap objects</li>
 *   <li>Protected visibility: All methods return {@link PageModelMap} for controller consumption</li>
 *   <li>Delegation pattern: HTTP handling delegated to subclasses; token business logic centralized here</li>
 * </ul>
 * </p>
 * <p>
 * <b>Core Methods:</b> {@link #getToken(Long, TokenRequest)}, {@link #refreshToken(RefreshTokenRequest)},
 * {@link #getTokenRefresher(RefresherTokenRequest, String)}. Each method orchestrates Flow pipelines that
 * load users, validate requests, issue tokens via {@code services.token}, and map results to
 * {@link com.openkoda.controller.api.v1.model.TokenResponse} stored in the {@code tokenResponse}
 * PageModelMap attribute.
 * </p>
 * <p>
 * <b>Service Dependencies:</b>
 * <ul>
 *   <li>{@code services.apiKey} - API key verification and token response formatting</li>
 *   <li>{@code services.token} - JWT token creation and refresh token management</li>
 *   <li>{@code services.user} - User authentication and password verification</li>
 *   <li>{@code repositories.unsecure.user} - Unsecured user repository for authentication flows</li>
 * </ul>
 * </p>
 *
 * @see TokenControllerApiV1
 * @see AbstractController
 * @see ApiAttributes
 * @see com.openkoda.core.flow.PageModelMap
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class AbstractTokenControllerApiV1 extends AbstractController implements ApiAttributes {

    /**
     * Issues JWT access token for user with API key verification.
     * <p>
     * This method orchestrates a Flow pipeline that loads the user entity, verifies the provided API key
     * against the user's registered keys, creates a JWT access token, and maps it to a TokenResponse DTO.
     * The result is stored in the {@code tokenResponse} PageModelMap attribute for consumption by controllers.
     * </p>
     * <p>
     * <b>Flow Pipeline:</b>
     * <ol>
     *   <li>Load user entity via {@code repositories.unsecure.user.findOne(userId)}</li>
     *   <li>Verify API key via {@code services.apiKey.verifyTokenRequest(aTokenRequest, user)}</li>
     *   <li>Create JWT token via {@code services.token.createTokenForUser(user)}</li>
     *   <li>Map token to TokenResponse via {@code services.apiKey.createTokenResponse(token)}</li>
     * </ol>
     * </p>
     * <p>
     * <b>Security Note:</b> API key must be valid and associated with the specified user. Tokens are
     * sensitive credentials - enforce TLS in production and avoid logging raw token values.
     * </p>
     *
     * @param userId the user ID for token issuance (must exist in user repository)
     * @param aTokenRequest the token request DTO containing {@code apiKey} field for verification
     * @return {@link PageModelMap} with {@code tokenResponse} attribute containing JWT token and metadata
     *         on success, or error details in {@code message} attribute on failure (invalid user or API key)
     * @see com.openkoda.controller.api.v1.model.TokenRequest
     * @see com.openkoda.controller.api.v1.model.TokenResponse
     */
    protected PageModelMap getToken(Long userId, TokenRequest aTokenRequest){
        return Flow.init(userEntity, repositories.unsecure.user.findOne(userId))
                .then(a -> services.apiKey.verifyTokenRequest(aTokenRequest, a.result))
                .then(a -> services.token.createTokenForUser(a.result.getUser()))
                .thenSet(tokenResponse, a -> services.apiKey.createTokenResponse(a.result))
                .execute();
    }

    /**
     * Exchanges refresh token for new JWT access token.
     * <p>
     * This method orchestrates a Flow pipeline that validates the provided refresh token (tokenRefresher),
     * creates a new JWT access token for the authenticated user, and maps it to a TokenResponse DTO.
     * The result is stored in the {@code tokenResponse} PageModelMap attribute.
     * </p>
     * <p>
     * <b>Flow Pipeline:</b>
     * <ol>
     *   <li>Verify refresh token via {@code services.token.createTokenForRefresher(tokenRefresher)}</li>
     *   <li>Map new token to TokenResponse via {@code services.apiKey.createTokenResponse(token)}</li>
     * </ol>
     * </p>
     * <p>
     * <b>Usage:</b> Clients use this method to obtain new access tokens without re-authenticating with
     * username/password. The refresh token must be valid and not expired.
     * </p>
     *
     * @param aTokenRequest the refresh token request DTO containing {@code tokenRefresher} field
     * @return {@link PageModelMap} with {@code tokenResponse} attribute containing new JWT access token
     *         on success, or error details in {@code message} attribute on failure (invalid or expired refresh token)
     * @see com.openkoda.controller.api.v1.model.RefreshTokenRequest
     * @see com.openkoda.controller.api.v1.model.TokenResponse
     */
    protected PageModelMap refreshToken(RefreshTokenRequest aTokenRequest){
        return Flow.init(null, services.token.createTokenForRefresher(aTokenRequest.getTokenRefresher()))
                .thenSet(tokenResponse, a -> services.apiKey.createTokenResponse(a.result))
                .execute();
    }

    /**
     * Authenticates user with credentials and issues refresh token.
     * <p>
     * This method orchestrates a Flow pipeline that finds the user by login, verifies the provided password,
     * creates a refresh token (tokenRefresher), and maps it to a TokenResponse DTO. The refresh token can
     * later be exchanged for access tokens via {@link #refreshToken(RefreshTokenRequest)} without requiring
     * password re-entry.
     * </p>
     * <p>
     * <b>Flow Pipeline:</b>
     * <ol>
     *   <li>Find user by login via {@code repositories.unsecure.user.findByLogin(username)}</li>
     *   <li>Verify password via {@code services.user.verifyPassword(user, password)}</li>
     *   <li>Create refresh token via {@code services.token.createRefresherTokenForUser(user)}</li>
     *   <li>Map token to TokenResponse via {@code services.apiKey.createTokenResponse(token)}</li>
     * </ol>
     * </p>
     * <p>
     * <b>Security Note:</b> Credentials are sensitive - enforce TLS and avoid logging passwords. Failed
     * authentication attempts should be rate-limited to prevent brute-force attacks.
     * </p>
     *
     * @param aRefresherTokenRequest the refresher token request DTO containing {@code login} and {@code password} fields
     * @param username the user login name extracted from {@code aRefresherTokenRequest.getLogin()}
     * @return {@link PageModelMap} with {@code tokenResponse} attribute containing refresh token on success,
     *         or error details in {@code message} attribute on failure (invalid credentials)
     * @see com.openkoda.controller.api.v1.model.RefresherTokenRequest
     * @see com.openkoda.controller.api.v1.model.TokenResponse
     */
    protected PageModelMap getTokenRefresher(RefresherTokenRequest aRefresherTokenRequest, String username){
        return Flow.init(userEntity, repositories.unsecure.user.findByLogin(username))
                .then(a -> services.user.verifyPassword(a.result, aRefresherTokenRequest.getPassword()))
                .then(a -> services.token.createRefresherTokenForUser(a.result))
                .thenSet(tokenResponse, a -> services.apiKey.createTokenResponse(a.result))
                .execute();
    }
}
