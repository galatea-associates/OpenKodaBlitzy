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

/**
 * Mutable DTO accepting refresh token for access token renewal.
 * <p>
 * This single-field JavaBean carries a refresh token string for token renewal operations.
 * Used by TokenControllerApiV1 refresh endpoint to receive refresh token requests from clients.
 * Jackson JSON binding deserializes incoming requests without validation annotations,
 * so consumers must validate, trim and verify the token string before use.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * POST /api/v1/auth/token/refresh
 * Content-Type: application/json
 * {"tokenRefresher": "refresh_token_value"}
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class RefreshTokenRequest {
    
    /**
     * Refresh token string for token renewal.
     * <p>
     * Contains sensitive token credential. Enforce TLS for transmission
     * and avoid logging this value in plain text.
     * </p>
     */
    private String tokenRefresher;

    /**
     * Returns the refresh token value.
     *
     * @return refresh token string (may be null if not set)
     */
    public String getTokenRefresher() {
        return tokenRefresher;
    }

    /**
     * Sets the refresh token value from JSON request.
     *
     * @param tokenRefresher refresh token to validate
     */
    public void setTokenRefresher(String tokenRefresher) {
        this.tokenRefresher = tokenRefresher;
    }
}
