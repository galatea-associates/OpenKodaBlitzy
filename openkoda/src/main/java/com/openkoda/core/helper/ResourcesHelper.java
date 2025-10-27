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

package com.openkoda.core.helper;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Provides utilities for loading classpath resources as strings using Apache Commons IOUtils.
 * <p>
 * This component loads resources from the application classpath and JAR files. It uses
 * {@link Class#getResourceAsStream(String)} internally for resource loading and
 * {@link IOUtils#toString(java.io.InputStream, String)} for conversion to strings.
 * </p>
 * <p>
 * The helper returns empty strings on errors rather than throwing exceptions, which simplifies
 * resource loading in optional configuration scenarios.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * String sqlContent = ResourcesHelper.getResourceAsStringOrEmpty("/sql/init.sql");
 * String template = ResourcesHelper.getResourceAsStringOrEmpty("/templates/email.html");
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.apache.commons.io.IOUtils
 */
@Component("resources")
public class ResourcesHelper {

    /**
     * Loads a classpath resource as a UTF-8 encoded string.
     * <p>
     * This method uses {@link Class#getResourceAsStream(String)} to load the resource from the
     * classpath and converts it to a string using {@link IOUtils#toString(java.io.InputStream, String)}.
     * The stream is closed automatically by IOUtils.
     * </p>
     * <p>
     * <b>Important warnings:</b>
     * </p>
     * <ul>
     * <li>May throw {@link NullPointerException} if the resource is not found, as
     * {@code getResourceAsStream()} returns null for missing resources</li>
     * <li>Returns an empty string for IO errors without distinguishing the cause</li>
     * <li>Does not distinguish between a missing resource, an empty resource, and an IO error</li>
     * </ul>
     * <p>
     * <b>Thread-safety:</b> This static method has no shared state and is safe for concurrent use.
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * String content = ResourcesHelper.getResourceAsStringOrEmpty("/sql/init.sql");
     * </pre>
     * </p>
     *
     * @param path the classpath resource path, such as "/templates/example.txt" or "/sql/schema.sql"
     * @return the resource content as a UTF-8 string, or an empty string if an IOException occurs
     * @see org.apache.commons.io.IOUtils#toString(java.io.InputStream, String)
     */
    public static String getResourceAsStringOrEmpty(String path){
        try {
            return IOUtils.toString(ResourcesHelper.class.getResourceAsStream(path), StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            return "";
        }
    }
}
