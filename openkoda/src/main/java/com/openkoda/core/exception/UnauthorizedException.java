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

package com.openkoda.core.exception;

import com.openkoda.core.flow.HttpStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user attempts to access a resource without proper authentication.
 * <p>
 * This exception represents HTTP 401 Unauthorized status, which indicates that authentication
 * is required but has failed or has not been provided. Common scenarios include missing
 * credentials, invalid tokens, or expired authentication sessions.
 * <p>
 * This differs from HTTP 403 Forbidden, which indicates the user is authenticated but lacks
 * authorization to access the resource. Use this exception for authentication failures, not
 * authorization denials.
 * <p>
 * The {@code @ResponseStatus} annotation automatically maps this exception to HTTP 401
 * when thrown from Spring MVC controllers. Spring handles serialization and error response
 * generation. The exception extends {@link HttpStatusException} to allow programmatic
 * inspection of the HTTP status code.
 * <p>
 * The {@code serialVersionUID} field ensures serialization stability across versions when
 * this exception is transmitted across network boundaries or persisted.
 * <p>
 * Example usage in authentication filter:
 * <pre>{@code
 * if (jwtToken == null || !jwtValidator.isValid(jwtToken)) {
 *     throw new UnauthorizedException("Invalid JWT token");
 * }
 * }</pre>
 * <p>
 * Example usage in service layer:
 * <pre>{@code
 * if (!authenticationService.isAuthenticated(request)) {
 *     throw new UnauthorizedException();
 * }
 * }</pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * See {@code HttpStatusException}
 * See {@code org.springframework.security.access.AccessDeniedException}
 * See {@code com.openkoda.core.security.ErrorLoggingExceptionResolver}
 */
@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "Unauthorized")
public class UnauthorizedException extends HttpStatusException {

    /**
     * Serial version UID for serialization stability across versions.
     */
    private static final long serialVersionUID = 9105354632528391981L;

    /**
     * Creates a new UnauthorizedException with a detailed authentication failure message.
     * <p>
     * Use this constructor to provide specific information about why authentication failed,
     * such as "Invalid JWT token", "Session expired", or "Missing authentication credentials".
     * The message helps with debugging and can be logged for security auditing.
     * 
     *
     * @param message the authentication failure reason, providing details about what went wrong
     */
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * Creates a new UnauthorizedException with a default authentication error message.
     * <p>
     * Use this no-argument constructor when a generic authentication failure message is
     * sufficient. Spring's {@code @ResponseStatus} annotation provides the "Unauthorized"
     * reason phrase in the HTTP response.
     * 
     */
    public UnauthorizedException() {
        super(HttpStatus.UNAUTHORIZED);
    }
}
