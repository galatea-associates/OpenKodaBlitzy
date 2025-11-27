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

/**
 * RuntimeException marker for validation failures in Flow pipeline execution.
 * <p>
 * This exception signals business-level validation errors during Flow processing.
 * The Flow pipeline catches this exception and sets {@code BasePageAttributes.isError} to true,
 * then stores the error message in the model for display to users.
 * <p>
 * Unlike {@link HttpStatusException}, which maps errors to specific HTTP status codes,
 * ValidationException represents validation logic failures that do not require
 * transaction rollback annotations. Flow handles this exception specially by capturing
 * the message and preserving the execution context for error presentation.
 * <p>
 * Usage example:
 * <pre>{@code
 * if (user.getEmail() == null) {
 *     throw new ValidationException("Email address is required");
 * }
 * }</pre>
 *
 * @see Flow
 * @see BasePageAttributes
 * @see HttpStatusException
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class ValidationException extends RuntimeException {

    /**
     * Creates a ValidationException with the specified error message.
     * <p>
     * The message describes the validation failure and is displayed to users
     * when Flow catches this exception and populates the error model attributes.
     * 
     *
     * @param message the validation error message describing why validation failed
     */
    public ValidationException(String message) {
        super(message);
    }
}
