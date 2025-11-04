/*
MIT License

Copyright (c) 2014-2022, Codedose CDX Sp. z o.o. Sp. K. <stratoflow.com>

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

package com.openkoda.core.flow;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Abstract exception that couples an HTTP status code with error handling in Flow pipelines.
 * <p>
 * This exception enables REST API error handling with proper HTTP status codes. When thrown
 * during Flow pipeline execution, {@link Flow#execute()} catches this exception and uses the
 * {@link #status} field to determine the HTTP response status code for the client.
 * <p>
 * Flow treats HttpStatusException specially compared to generic exceptions. While generic
 * exceptions result in HTTP 500 (Internal Server Error), HttpStatusException allows
 * controllers to return specific status codes like 404 (Not Found), 403 (Forbidden),
 * or 401 (Unauthorized) by throwing the appropriate subclass.
 * <p>
 * Example usage with concrete subclass:
 * <pre>
 * if (!hasPermission) { throw new ForbiddenException(); }
 * </pre>
 *
 * @see Flow
 * @see ResponseEntity
 * @since 1.7.1
 * @author OpenKoda Team
 */
public abstract class HttpStatusException extends RuntimeException {

    /**
     * The HTTP status code associated with this exception.
     * <p>
     * Flow.execute() reads this field to determine the HTTP response status when
     * this exception is caught during pipeline execution.
     * 
     */
    public final HttpStatus status;

    /**
     * Creates an exception with the specified HTTP status code.
     *
     * @param status the HTTP status code to return when this exception is caught by Flow
     */
    public HttpStatusException(HttpStatus status) {
        this.status = status;
    }

    /**
     * Creates an exception with the specified HTTP status code and underlying cause.
     *
     * @param status the HTTP status code to return when this exception is caught by Flow
     * @param cause the underlying exception that caused this error
     */
    public HttpStatusException(HttpStatus status, Throwable cause) {
        super(cause);
        this.status = status;
    }

    /**
     * Creates an exception with the specified HTTP status code and error message.
     *
     * @param status the HTTP status code to return when this exception is caught by Flow
     * @param message the error message describing what went wrong
     */
    public HttpStatusException(HttpStatus status, String message) {
        super(message);
        this.status = status;

    }

    /**
     * Builds a ResponseEntity with the HTTP status configured in this exception.
     * <p>
     * Creates an empty ResponseEntity with the status code from the {@link #status} field.
     * This method allows REST controllers to easily convert the exception into an HTTP response.
     * 
     *
     * @return a ResponseEntity with the configured HTTP status code and no body
     */
    public ResponseEntity build() {
        return ResponseEntity.status(status).build();
    }


}
