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

import com.openkoda.controller.api.v1.model.RefreshTokenRequest;
import com.openkoda.controller.api.v1.model.RefresherTokenRequest;
import com.openkoda.controller.api.v1.model.TokenRequest;
import com.openkoda.controller.api.v1.model.TokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.openkoda.controller.common.URLConstants._API_AUTH;

/**
 * REST controller for API v1 authentication token endpoints.
 * <p>
 * This Spring-discovered singleton exposes three POST endpoints under the {@code _API_AUTH} base mapping
 * (typically "/api/v1/auth") for JWT token issuance and refresh operations. All business logic is delegated
 * to inherited protected methods from {@link AbstractTokenControllerApiV1}. Each endpoint converts the
 * {@link com.openkoda.core.flow.PageModelMap} result to {@link ResponseEntity} via the {@code mav()} method
 * with success and error mappers.
 * </p>
 * <p>
 * <b>Endpoints:</b>
 * <ul>
 *   <li>POST /api/v1/auth/token/user/{userId} - Issues JWT access token with API key verification</li>
 *   <li>POST /api/v1/auth/token/refresh - Exchanges refresh token for new access token</li>
 *   <li>POST /api/v1/auth/tokenrefresher/user - Authenticates with credentials and issues refresh token</li>
 * </ul>
 * </p>
 * <p>
 * <b>Security Note:</b> Currently no {@code @PreAuthorize} annotations are present (marked with TODO).
 * Endpoints are publicly accessible for authentication purposes. Credentials and tokens are sensitive -
 * enforce TLS and avoid logging raw values.
 * </p>
 * <p>
 * <b>Response Format:</b><br>
 * Success: {@code {"apiToken": "jwt...", "userId": 123, "expiresOn": "2024-01-15T10:30:00"}}<br>
 * Error: {@code {"message": "Invalid credentials", "status": 404}}
 * </p>
 *
 * @see AbstractTokenControllerApiV1
 * @see TokenResponse
 * @since 1.7.1
 * @author OpenKoda Team
 */
@RestController
@RequestMapping(_API_AUTH)
public class TokenControllerApiV1 extends AbstractTokenControllerApiV1 {

    /**
     * Issues JWT access token for authenticated user with API key verification.
     * <p>
     * HTTP Endpoint: {@code POST /api/v1/auth/token/user/{userId}}
     * </p>
     * <p>
     * Flow: Load user → Verify API key → Create token → Return TokenResponse
     * </p>
     *
     * @param userId the user ID for token issuance (path variable)
     * @param aTokenRequest the token request DTO containing apiKey field for verification
     * @return {@link ResponseEntity} with HTTP 200 and {@link TokenResponse} on success,
     *         or HTTP 404 with error message on failure (invalid user or API key)
     * @see AbstractTokenControllerApiV1#getToken(Long, TokenRequest)
     */
    //TODO Rule 1.4 All methods in non-public controllers must have @PreAuthorize
    @PostMapping(_TOKEN + _USER + _ID )
    public ResponseEntity<TokenResponse> get(
            @PathVariable(ID) Long userId, @RequestBody TokenRequest aTokenRequest) {
        debug("[getToken] UserId: {}", userId);
        return (ResponseEntity<TokenResponse>)
               getToken(userId, aTokenRequest)
                        .mav(
                                a -> ResponseEntity.ok(a.get(tokenResponse)),
                                a -> new ResponseEntity(a.get(message), HttpStatus.NOT_FOUND)
                        );
    }
    
    /**
     * Exchanges refresh token for new JWT access token.
     * <p>
     * HTTP Endpoint: {@code POST /api/v1/auth/token/refresh}
     * </p>
     * <p>
     * Flow: Verify refresh token → Create new access token → Return TokenResponse
     * </p>
     *
     * @param aTokenRequest the refresh token request DTO containing tokenRefresher field
     * @return {@link ResponseEntity} with HTTP 200 and new {@link TokenResponse} on success,
     *         or HTTP 404 with error message on failure (invalid or expired refresh token)
     * @see AbstractTokenControllerApiV1#refreshToken(RefreshTokenRequest)
     */
    //TODO Rule 1.4 All methods in non-public controllers must have @PreAuthorize
    @PostMapping(_TOKEN + _REFRESH)
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshTokenRequest aTokenRequest) {
        debug("[getToken] UserId: {}");
        return (ResponseEntity<TokenResponse>)
                refreshToken(aTokenRequest)
                .mav(
                    a -> ResponseEntity.ok(a.get(tokenResponse)),
                    a -> new ResponseEntity(a.get(message), HttpStatus.NOT_FOUND)
                );
    }
    
    /**
     * Authenticates user with credentials and issues refresh token.
     * <p>
     * HTTP Endpoint: {@code POST /api/v1/auth/tokenrefresher/user}
     * </p>
     * <p>
     * Flow: Find user by login → Verify password → Create refresh token → Return TokenResponse
     * </p>
     *
     * @param aRefresherTokenRequest the refresher token request DTO containing login and password credentials
     * @return {@link ResponseEntity} with HTTP 200 and refresh {@link TokenResponse} on success,
     *         or HTTP 404 with error message on failure (invalid credentials)
     * @see AbstractTokenControllerApiV1#getTokenRefresher(RefresherTokenRequest, String)
     */
    //TODO Rule 1.4 All methods in non-public controllers must have @PreAuthorize
    @PostMapping(_TOKENREFRESHER + _USER )
    public ResponseEntity<TokenResponse> getRefresher(@RequestBody RefresherTokenRequest aRefresherTokenRequest) {
        String username = aRefresherTokenRequest.getLogin();
        debug("[getTokenRefresher] username: {}", username);
        return (ResponseEntity<TokenResponse>)
                getTokenRefresher(aRefresherTokenRequest, username)
                        .mav(
                                a -> ResponseEntity.ok(a.get(tokenResponse)),
                                a -> new ResponseEntity(a.get(message), HttpStatus.NOT_FOUND)
                        );
    }
}