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
 * Mutable DTO carrying API key for token issuance.
 * <p>
 * This class represents a simple JavaBean for transferring API key credentials
 * in token generation requests. It contains a single field for the API key and
 * standard getter/setter methods. The class is used by TokenControllerApiV1 to
 * process token requests submitted via the REST API endpoint.
 * </p>
 * <p>
 * The class does not include validation annotations. Validation logic for the
 * API key is handled by the service layer during token generation.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * POST /api/v1/auth/token/user/{userId}
 * {"apiKey": "api_key_value"}
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class TokenRequest {
    /**
     * API key for token request authentication.
     * <p>
     * This field contains the secret credential used to authenticate the token
     * generation request. The API key should be treated as sensitive data and
     * protected like a password in transit and storage.
     * </p>
     */
    private String apiKey;

    /**
     * Returns the API key value.
     *
     * @return API key string (may be null if not set)
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets the API key value from JSON request.
     *
     * @param apiKey API key to validate for token issuance
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}