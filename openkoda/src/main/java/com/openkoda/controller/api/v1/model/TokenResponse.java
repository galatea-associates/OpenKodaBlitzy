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

package com.openkoda.controller.api.v1.model;

import java.time.LocalDateTime;

/**
 * Immutable response DTO containing JWT access token and metadata.
 * <p>
 * This class encapsulates the token response returned by authentication and token generation endpoints.
 * It contains three final fields (apiToken, userId, expiresOn) initialized via constructor.
 * The class provides getters only with no setters, making it immutable and thread-safe for concurrent read access.
 * </p>
 * <p>
 * <b>Note:</b> LocalDateTime serialization requires Jackson JavaTimeModule for ISO-8601 formatting.
 * </p>
 * <p>
 * Example JSON response:
 * <pre>{@code
 * {
 *   "apiToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "userId": 123,
 *   "expiresOn": "2024-01-15T10:30:00"
 * }
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class TokenResponse {
    /**
     * JWT access token string in header.payload.signature format.
     * <p>
     * This token is used for authenticating subsequent API requests.
     * </p>
     */
    private final String apiToken;
    
    /**
     * User ID for which the token was issued.
     */
    private final Long userId;
    
    /**
     * Token expiration timestamp in UTC.
     * <p>
     * Requires Jackson JavaTimeModule for proper ISO-8601 serialization.
     * </p>
     */
    private final LocalDateTime expiresOn;

    /**
     * Constructs an immutable token response with the specified token, user ID, and expiration timestamp.
     * <p>
     * This constructor initializes all final fields, making the instance immutable and thread-safe.
     * Note that the constructor parameter 'token' is assigned to the apiToken field.
     * </p>
     *
     * @param token JWT access token string to return to the client
     * @param userId User ID for which this token is issued
     * @param expiresOn Token expiration timestamp in UTC
     */
    public TokenResponse(String token, Long userId, LocalDateTime expiresOn) {
        this.apiToken = token;
        this.userId = userId;
        this.expiresOn = expiresOn;
    }

    /**
     * Returns the JWT access token.
     *
     * @return JWT access token string in header.payload.signature format
     */
    public String getApiToken() {
        return apiToken;
    }

    /**
     * Returns the user ID for which the token was issued.
     *
     * @return User ID as Long
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Returns the token expiration timestamp.
     *
     * @return Expiration timestamp in UTC as LocalDateTime
     */
    public LocalDateTime getExpiresOn() {
        return expiresOn;
    }
}