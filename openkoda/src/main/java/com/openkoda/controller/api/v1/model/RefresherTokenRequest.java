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
 * Mutable DTO carrying user credentials for refresh token issuance.
 * <p>
 * This class is a simple two-field JavaBean containing login and password strings
 * used by the TokenControllerApiV1 getRefresher endpoint for authentication.
 * It follows standard JavaBean conventions with private fields and public getters/setters.
 * The class preserves the MIT license header and contains no validation annotations.
 * 
 * <p>
 * <b>Security Warning:</b> This DTO contains highly sensitive data including plaintext passwords.
 * Always enforce HTTPS for transmission, clear password from memory after use, and never log raw passwords.
 * Handle this object with extreme caution in production environments.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class RefresherTokenRequest
{
    /**
     * User login name or email address for authentication.
     */
    private String login;
    
    /**
     * User plaintext password for authentication.
     * <p>
     * <b>Security Note:</b> This field contains highly sensitive data.
     * Transmit over TLS only and clear from memory after validation.
     * 
     */
    private String password;

    /**
     * Returns the user login name or email address.
     *
     * @return user login identifier, may be null if not set
     */
    public String getLogin() {
        return login;
    }

    /**
     * Sets the user login name from the JSON request.
     *
     * @param login user login name or email address to authenticate
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Returns the plaintext password for authentication.
     *
     * @return user password, may be null if not set
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Sets the plaintext password from the JSON request.
     *
     * @param password user plaintext password (handle securely, clear after use)
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
