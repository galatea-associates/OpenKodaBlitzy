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

package com.openkoda.dto.system;

import com.openkoda.dto.CanonicalObject;

import java.util.Set;

/**
 * Data Transfer Object for configuring logging system parameters including buffer size and class-specific logging.
 * <p>
 * This DTO implements {@link CanonicalObject} to provide notification messages for audit trails and system monitoring.
 * It is designed as a mutable JavaBean without validation, equals/hashCode implementations, or thread-safety guarantees.

 * <p>
 * The DTO is used by logging configuration services, admin controllers, and system setup components to manage
 * runtime logging behavior. It supports configuring a logging buffer size (parsed from string to integer) and
 * specifying a set of fully-qualified class names for which detailed logging should be enabled.

 * <p>
 * <strong>Important Notes:</strong>

 * <ul>
 * <li>This class has a mutable design without validation - external validation is required before use</li>
 * <li>{@link #getBufferSize()} will throw {@link NumberFormatException} if {@code bufferSizeField} is not a valid integer or is null</li>
 * <li>{@link #notificationMessage()} will throw {@link NullPointerException} if {@code loggingClasses} is null</li>
 * <li>The fluent setter pattern in {@link #setLoggingClasses(Set)} violates Rule 5.4 (DTOs should not have state-changing methods)</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * LoggerDto dto = new LoggerDto();
 * dto.setBufferSize(1024);
 * dto.setLoggingClasses(Set.of("com.openkoda.service.UserService"));
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CanonicalObject
 */
public class LoggerDto implements CanonicalObject {

    /**
     * Text representation of logging buffer size in bytes or entries.
     * <p>
     * This field stores the buffer size as a string and is parsed to an integer via {@link #getBufferSize()}.
     * If the value is not a valid integer format or is null, {@link NumberFormatException} will be thrown
     * during parsing.

     *
     * @see #getBufferSize()
     * @see #setBufferSize(int)
     */
    public String bufferSizeField;
    
    /**
     * Set of fully-qualified class names to enable detailed logging for.
     * <p>
     * This field contains class names (e.g., "com.openkoda.service.UserService") for which the logging
     * system should capture detailed log output. If this field is null when {@link #notificationMessage()}
     * is called, a {@link NullPointerException} will be thrown.

     *
     * @see #getLoggingClasses()
     * @see #setLoggingClasses(Set)
     */
    public Set<String> loggingClasses;

    /**
     * Parses and returns the buffer size as an integer.
     * <p>
     * This method converts the string representation in {@link #bufferSizeField} to an integer
     * using {@link Integer#parseInt(String)}. No null-safety checks are performed, so this method
     * will throw an exception if the field is null or contains an invalid format.

     *
     * @return the parsed buffer size as an integer
     * @throws NumberFormatException if {@code bufferSizeField} is not a valid integer or is null
     * @see #setBufferSize(int)
     */
    public int getBufferSize() {
        return Integer.parseInt(bufferSizeField);
    }

    /**
     * Sets the buffer size by converting the integer value to a string.
     * <p>
     * This method converts the provided integer buffer size to its string representation
     * using {@link String#valueOf(int)} and stores it in {@link #bufferSizeField}.
     * No validation is performed on the input value.

     *
     * @param bufferSize the buffer size to set (should be non-negative, but no validation is performed)
     * @see #getBufferSize()
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSizeField = String.valueOf(bufferSize);
    }

    /**
     * Returns the raw string representation of the buffer size field.
     * <p>
     * This getter provides direct access to the string field without parsing.
     * The returned value may be null if not yet initialized.

     *
     * @return the raw string representation of buffer size, may be null
     * @see #setBufferSizeField(String)
     */
    public String getBufferSizeField() {
        return bufferSizeField;
    }

    /**
     * Sets the raw string representation of the buffer size field.
     * <p>
     * This setter accepts a string that should be parseable as an integer for use with {@link #getBufferSize()}.
     * No validation is performed on the input value.

     *
     * @param bufferSizeField the string representation of buffer size, should be parseable as an integer
     * @see #getBufferSizeField()
     */
    public void setBufferSizeField(String bufferSizeField) {
        this.bufferSizeField = bufferSizeField;
    }

    /**
     * Returns the set of fully-qualified class names for which detailed logging is enabled.
     * <p>
     * This getter provides access to the collection of class names that should have detailed logging.
     * The returned set may be null if not yet initialized, or empty if no classes are configured.

     *
     * @return the set of fully-qualified class names, may be null or empty
     * @see #setLoggingClasses(Set)
     */
    public Set<String> getLoggingClasses() {
        return loggingClasses;
    }

    /**
     * Sets the collection of class names for which detailed logging should be enabled.
     * <p>
     * This is a fluent setter that returns the current {@code LoggerDto} instance to enable method chaining.
     * The fluent pattern allows for expressive configuration but violates Rule 5.4, which states that DTOs
     * should not have state-changing methods (only constructors are allowed).

     * <p>
     * Example usage with method chaining:
     * <pre>{@code
     * LoggerDto dto = new LoggerDto()
     *     .setLoggingClasses(Set.of("com.openkoda.service.UserService"));
     * }</pre>

     *
     * @param loggingClasses the set of fully-qualified class names to enable logging for
     * @return this {@code LoggerDto} instance for fluent API method chaining
     * @see #getLoggingClasses()
     */
    //TODO Rule 5.4: DTO must not have methods that change its state (constructors are allowed)
    public LoggerDto setLoggingClasses(Set<String> loggingClasses) {
        this.loggingClasses = loggingClasses;
        return this;
    }

    /**
     * Returns a formatted notification message for audit trails and system monitoring.
     * <p>
     * This method provides a human-readable description of the logging configuration, including
     * the buffer size and the number of classes configured for detailed logging. The message
     * format is: "Logging config. Buffer: %s. No of classes: %d."

     * <p>
     * <strong>Warning:</strong> This method does not perform null-safety checks. If {@link #loggingClasses}
     * is null when this method is called, a {@link NullPointerException} will be thrown when attempting
     * to call {@code size()} on the null set.

     *
     * @return a formatted notification message describing the logging configuration
     * @throws NullPointerException if {@code loggingClasses} is null
     */
    @Override
    public String notificationMessage() {
        return String.format("Logging config. Buffer: %s. No of classes: %d.", bufferSizeField, loggingClasses.size());
    }
}