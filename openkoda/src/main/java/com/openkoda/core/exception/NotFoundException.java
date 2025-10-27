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
 * Exception thrown when a requested resource cannot be found in the system.
 * <p>
 * This exception should be used when an entity lookup fails, such as when querying
 * for an organization, user, or other resource by ID that does not exist in the database.
 * It automatically maps to HTTP 404 (Not Found) status when thrown from Spring MVC controllers.
 * </p>
 * <p>
 * The {@code @ResponseStatus} annotation instructs Spring MVC to return an HTTP 404 response
 * with the reason "Resource Not Found" when this exception is thrown and not caught.
 * The exception extends {@link HttpStatusException}, allowing programmatic access to the
 * HTTP status code via {@code getHttpStatus()}.
 * </p>
 * <p>
 * Typical usage in repository or service layers:
 * <pre>{@code
 * Organization org = organizationRepository.findById(id)
 *     .orElseThrow(() -> new NotFoundException("Organization not found: " + id));
 * }</pre>
 * </p>
 * <p>
 * When uncaught, this exception is handled by {@code ErrorLoggingExceptionResolver},
 * which logs the error and returns a formatted error response to the client.
 * The {@code serialVersionUID} ensures serialization stability across application versions.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see HttpStatusException
 * @see org.springframework.web.bind.annotation.ResponseStatus
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Resource Not Found")
public class NotFoundException extends HttpStatusException {

    /**
     * Serial version UID for serialization compatibility across application versions.
     */
    private static final long serialVersionUID = 1886015530047410403L;

    /**
     * Creates a new NotFoundException with a descriptive error message.
     * <p>
     * Use this constructor to provide specific details about the missing resource,
     * such as the entity type and identifier that could not be found.
     * </p>
     *
     * @param message the descriptive error message explaining which resource was not found,
     *                typically includes the entity type and ID (e.g., "User not found: 123")
     */
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Creates a new NotFoundException with a default error message.
     * <p>
     * Use this no-argument constructor when a generic "not found" error is sufficient
     * and specific resource details are not needed in the error message.
     * </p>
     */
    public NotFoundException() {
        super(HttpStatus.NOT_FOUND);

    }
}
