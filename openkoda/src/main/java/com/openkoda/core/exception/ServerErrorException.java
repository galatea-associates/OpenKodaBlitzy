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
 * Represents an internal server error (HTTP 500) thrown when the application encounters unexpected failures.
 * <p>
 * This exception is used to wrap and signal various internal server error scenarios, including:
 * </p>
 * <ul>
 *   <li>Database connection failures or query execution errors</li>
 *   <li>External service timeouts or integration failures</li>
 *   <li>Configuration errors or missing required resources</li>
 *   <li>Unexpected runtime exceptions during request processing</li>
 * </ul>
 * <p>
 * The exception extends {@link HttpStatusException} to enable programmatic HTTP status inspection
 * and integrates with Spring MVC via the {@code @ResponseStatus} annotation, which automatically
 * maps uncaught instances to HTTP 500 responses.
 * </p>
 * <p>
 * When thrown from controller methods or service layers, {@link ErrorLoggingExceptionResolver}
 * catches the exception, logs relevant diagnostic information (request ID, URI, status), and
 * redirects to the error page with appropriate query parameters.
 * </p>
 * <p>
 * Usage in service layer:
 * </p>
 * <pre>{@code
 * try {
 *     executeExternalApiCall();
 * } catch (IOException e) {
 *     throw new ServerErrorException("External API connection failed");
 * }
 * }</pre>
 * <p>
 * Usage in integration points:
 * </p>
 * <pre>{@code
 * if (configurationMissing) {
 *     throw new ServerErrorException("Required configuration property not found");
 * }
 * }</pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see HttpStatusException
 * @see ErrorLoggingExceptionResolver
 */
@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Server Error")
public class ServerErrorException extends HttpStatusException {

    /**
     * Serial version UID for serialization stability across versions.
     */
    private static final long serialVersionUID = -7961799985336841330L;

    /**
     * Creates a new server error exception with a descriptive message.
     * <p>
     * The message is used for logging and debugging purposes to help operators
     * diagnose the root cause of internal failures. Include relevant context
     * such as operation being performed or resource identifier.
     * </p>
     *
     * @param message detailed error description for logging and debugging
     */
    public ServerErrorException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    /**
     * Creates a new server error exception with a default message.
     * <p>
     * This constructor provides a generic server error indication when
     * a detailed message is not available or necessary.
     * </p>
     */
    public ServerErrorException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
