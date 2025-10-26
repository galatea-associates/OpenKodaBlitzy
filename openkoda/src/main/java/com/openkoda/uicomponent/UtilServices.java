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

package com.openkoda.uicomponent;

import com.openkoda.core.flow.LoggingComponent;
import com.openkoda.model.file.File;
import com.openkoda.service.csv.CsvService;
import com.openkoda.uicomponent.annotation.Autocomplete;
import jakarta.inject.Inject;
import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * Utility service providing common operations for date/time, parsing, JSON, URI encoding, CSV export, and hashing.
 * <p>
 * This class provides stateless utility methods exposed to UI flows and JavaScript execution contexts.
 * Methods are bound as JavaScript functions in JsFlowRunner for GraalVM access.
 * All methods are annotated with {@code @Autocomplete} for UI tooling hints.
 * Implements LoggingComponent for error logging capabilities.
 * </p>
 * <p>
 * Thread-safety: All methods are stateless or delegate to thread-safe libraries.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see JsFlowRunner
 * @see LiveComponentProvider
 * @see CsvService
 */
@Component
public class UtilServices implements LoggingComponent {

    /**
     * CsvService for CSV file generation and export.
     * <p>
     * Injected via {@code @Inject}, used by toCSV method.
     * </p>
     */
    @Inject
    CsvService csvService;

    /**
     * Returns current date in system default timezone.
     * <p>
     * Example: {@code dateNow()} returns {@code LocalDate.of(2025, 10, 25)} on October 25, 2025.
     * </p>
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Get the current date' hint for UI tooling.
     * </p>
     *
     * @return LocalDate representing today's date
     */
    @Autocomplete(doc="Get the current date")
    public LocalDate dateNow() {
        return LocalDate.now();
    }

    /**
     * Returns current date and time in system default timezone.
     * <p>
     * Example: {@code dateTimeNow()} returns {@code LocalDateTime.of(2025, 10, 25, 14, 30, 0)} at 2:30 PM.
     * </p>
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Get the current date and time' hint for UI tooling.
     * </p>
     *
     * @return LocalDateTime representing current moment
     */
    @Autocomplete(doc="Get the current date and time")
    public LocalDateTime dateTimeNow() {
        return LocalDateTime.now();
    }

    /**
     * Parses string to integer.
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Parse string to integer' hint for UI tooling.
     * </p>
     *
     * @param s String representation of integer (e.g., '123', '-456')
     * @return Parsed integer value
     * @throws NumberFormatException If string cannot be parsed as integer
     */
    @Autocomplete(doc="Parse string to integer")
    public int parseInt(String s) {
        return Integer.parseInt(s);
    }

    /**
     * Parses string to long integer.
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Parse string to long' hint for UI tooling.
     * </p>
     *
     * @param s String representation of long (e.g., '9223372036854775807')
     * @return Parsed long value
     * @throws NumberFormatException If string cannot be parsed as long
     */
    @Autocomplete(doc="Parse string to long")
    public long parseLong(String s) {
        return Long.parseLong(s);
    }

    /**
     * Parses ISO date string to LocalDate.
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Parse date string to date object' hint for UI tooling.
     * </p>
     *
     * @param s ISO date string (e.g., '2025-10-25', yyyy-MM-dd format)
     * @return Parsed LocalDate
     * @throws java.time.format.DateTimeParseException If string format invalid
     */
    @Autocomplete(doc="Parse date string to date object")
    public LocalDate parseDate(String s) {
        return LocalDate.parse(s);
    }

    /**
     * Parses ISO time string to LocalTime.
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Parse time string (eg. 23:12) to time object' hint for UI tooling.
     * </p>
     *
     * @param s ISO time string (e.g., '14:30', '14:30:45', HH:mm or HH:mm:ss format)
     * @return Parsed LocalTime
     * @throws java.time.format.DateTimeParseException If string format invalid
     */
    @Autocomplete(doc="Parse time string (eg. 23:12) to time object")
    public LocalTime parseTime(String s) {
        return LocalTime.parse(s);
    }

    /**
     * Converts object to string representation via toString() method.
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Get string value of the object' hint for UI tooling.
     * </p>
     *
     * @param o Object to convert (may be null)
     * @return String representation, or 'null' if o is null
     * @throws NullPointerException If o is null and implementation calls o.toString()
     */
    @Autocomplete(doc="Get string value of the object")
    public String toString(Object o) {
        return o.toString();
    }

    /**
     * Checks if double value is NaN (Not-a-Number).
     * <p>
     * Use case: Validate arithmetic results: {@code isNaN(0.0/0.0)} returns true.
     * </p>
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Check for NaN (Not a Number)' hint for UI tooling.
     * </p>
     *
     * @param d Double value to check
     * @return true if d is NaN, false otherwise
     */
    @Autocomplete(doc="Check for NaN (Not a Number)")
    public boolean isNaN(double d) {
        return Double.isNaN(d);
    }

    /**
     * Parses string to floating-point number.
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Parse string to floating-point number' hint for UI tooling.
     * </p>
     *
     * @param s String representation of float (e.g., '3.14', '1.5e10')
     * @return Parsed float value
     * @throws NumberFormatException If string cannot be parsed as float
     */
    @Autocomplete(doc="Parse string to floating-point number")
    public float parseFloat(String s) {
        return Float.parseFloat(s);
    }

    /**
     * Parses JSON string to JSONObject.
     * <p>
     * Attempts to parse JSON string using org.json.JSONObject. On JSONException, logs error
     * via LoggingComponent.error() and returns null instead of throwing exception.
     * </p>
     * <p>
     * Example: {@code parseJSON("{\"name\":\"test\"}")} returns JSONObject with name='test'.
     * </p>
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Parse JSON string to object' hint for UI tooling.
     * </p>
     *
     * @param s JSON string (e.g., '{"key":"value"}')
     * @return JSONObject instance, or null if parsing fails
     */
    @Autocomplete(doc="Parse JSON string to object")
    public JSONObject parseJSON(String s) {
        try {
            return new JSONObject(s);
        } catch (JSONException e) {
            error("JSON parsing failed because of: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts object to pretty-printed JSON string via Jackson ObjectMapper.
     * <p>
     * Uses Jackson ObjectMapper with default pretty printer. On IOException, logs error and returns null.
     * Suitable for POJOs, Maps, Lists. Ignores transient fields unless configured otherwise.
     * </p>
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Convert objects to JSON string' hint for UI tooling.
     * </p>
     *
     * @param o Object to serialize (must be Jackson-serializable)
     * @return JSON string with pretty printing, or null if serialization fails
     * @throws IOException Caught internally, logged via error(), returns null
     */
    @Autocomplete(doc="Convert objects to JSON string")
    public String toJSON(Object o) {
        try {
            return new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(o);
        } catch (IOException e) {
            error("JSON serialization failed because of: " + e.getMessage());
            return null;
        }
    }

    /**
     * Decodes percent-encoded URI string to plain text using UTF-8.
     * <p>
     * Uses URLDecoder.decode with UTF-8 charset. On UnsupportedEncodingException (unlikely with UTF-8),
     * returns error message string instead of throwing exception.
     * </p>
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Decode Uniform Resource Identifier (URI) string' hint for UI tooling.
     * </p>
     *
     * @param uri URI string with percent-encoding (e.g., 'Hello%20World')
     * @return Decoded string (e.g., 'Hello World'), or error message if decoding fails
     */
    @Autocomplete(doc="Decode Uniform Resource Identifier (URI) string")
    public String decodeURI(String uri) {
        try {
            return URLDecoder.decode(uri, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return "Decoding failed because of: " + e.getMessage();
        }
    }

    /**
     * Encodes string to percent-encoded URI format using UTF-8.
     * <p>
     * Uses URLEncoder.encode with UTF-8 charset. On UnsupportedEncodingException (unlikely),
     * returns error message string. Suitable for encoding query parameters, path segments.
     * </p>
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Encode Uniform Resource Identifier (URI) string' hint for UI tooling.
     * </p>
     *
     * @param s Plain text string to encode
     * @return Percent-encoded URI string, or error message if encoding fails
     */
    @Autocomplete(doc="Encode Uniform Resource Identifier (URI) string")
    public String encodeURI(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return "Encoding failed because of: " + e.getMessage();
        }
    }

    /**
     * Exports data to CSV file entity with optional headers.
     * <p>
     * Delegates to csvService.createCSV. Converts List&lt;Object[]&gt; to List&lt;List&lt;Object&gt;&gt;,
     * includes headers as first row if provided, generates CSV content, creates File entity with
     * contentType='text/csv', persists to storage.
     * </p>
     * <p>
     * Example: {@code toCSV("users.csv", userData, "ID", "Name", "Email")} creates CSV with headers.
     * </p>
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Export data to CSV file' hint for UI tooling.
     * </p>
     *
     * @param filename Target filename with .csv extension
     * @param data List of Object arrays, each array is one CSV row
     * @param headers Varargs header strings for first row (optional)
     * @return Persisted File entity containing CSV data
     * @throws IOException If CSV writing fails
     * @throws SQLException If file persistence fails
     */
    @Autocomplete(doc="Export data to CSV file")
    public File toCSV(String filename, List<Object[]> data, String... headers) throws IOException, SQLException {
        return csvService.createCSV(filename, data.stream().map(Arrays::asList)
                .toList(), headers);
    }

    /**
     * Computes MD5 hash of string and returns hexadecimal representation.
     * <p>
     * Uses Apache Commons DigestUtils.md5Hex. String converted to bytes using default charset,
     * MD5 digest computed, result encoded as hex string. MD5 is cryptographically broken - use
     * only for checksums, not security.
     * </p>
     * <p>
     * Security warning: MD5 vulnerable to collisions - do not use for passwords or security tokens.
     * </p>
     * <p>
     * Note: {@code @Autocomplete} annotation provides 'Compute MD5 hash of string' hint for UI tooling.
     * </p>
     *
     * @param value String to hash
     * @return 32-character hexadecimal MD5 hash (lowercase)
     */
    @Autocomplete(doc="Compute MD5 hash of string")
    public String md5(String value) {
        return DigestUtils.md5Hex(value);
    }
}
