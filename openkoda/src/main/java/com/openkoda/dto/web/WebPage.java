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

package com.openkoda.dto.web;

import com.openkoda.dto.CanonicalObject;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Web page data transfer object with URL parsing utility.
 * <p>
 * Implements {@link CanonicalObject} for human-readable notification formatting. This class is
 * intentionally mutable with a public field design to support reflection-based mappers and
 * serialization frameworks (such as Jackson). It contains no validation, synchronization, or
 * defensive copying by design.
 * </p>
 * <p>
 * The class is used by controllers, service layers, mapping frameworks, and serializers to
 * transport web page URL information across application boundaries. The static {@link #getDomain(String)}
 * utility method provides basic URL parsing for extracting domain names.
 * </p>
 * <p>
 * <strong>Thread Safety:</strong> This class is not thread-safe. No synchronization is provided
 * for the public mutable field.
 * </p>
 * <p>
 * <strong>Binary Compatibility:</strong> Changes to the public field name or type are breaking
 * changes for serialization and reflection-based mapping frameworks.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CanonicalObject
 */
//TODO Rule 5.2: DTO class name must end with "Dto"
public class WebPage implements CanonicalObject {

    /**
     * The URL of the web page.
     * <p>
     * This field is intentionally public and mutable to enable direct field access by
     * reflection-based mapping frameworks and serializers. No validation or normalization
     * is performed on assignment.
     * </p>
     * <p>
     * <strong>Warning:</strong> Changing this field's name or type is a breaking change for
     * binary and serialization compatibility with existing clients and mappers.
     * </p>
     */
    public String url;

    /**
     * Constructs a WebPage with the specified URL.
     * <p>
     * Assigns the URL directly to the public field without validation or normalization.
     * </p>
     *
     * @param url the URL string for the web page, may be null
     */
    public WebPage(String url) {
        this.url = url;
    }

    /**
     * Constructs a WebPage with a null URL.
     * <p>
     * This no-argument constructor is required by frameworks that use reflection to
     * instantiate objects, such as Jackson and other serialization libraries.
     * </p>
     */
    public WebPage() {
    }

    /**
     * Extracts the host portion from a URL string.
     * <p>
     * This static helper method normalizes URLs without an explicit {@code http://} or {@code https://}
     * scheme by prefixing {@code http://}. It then uses {@link URI} for parsing and returns the
     * host portion via {@link URI#getHost()}.
     * </p>
     * <p>
     * The method is side-effect-free and performs no validation beyond the normalization. If the
     * URL cannot be parsed, the method catches {@link URISyntaxException} and returns {@code null}.
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * String domain = WebPage.getDomain("example.com");  // Returns "example.com"
     * </pre>
     * </p>
     *
     * @param url the URL string to parse, may lack a scheme prefix
     * @return the host portion from the URI, or {@code null} if parsing fails
     */
    //TODO Rule 5.5: DTO should not have code
    public static String getDomain(String url){

        if(!url.contains("http") && !url.contains("https")){
            url = "http://" + url;
        }

        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            return domain;

        } catch (URISyntaxException e) {
            return null;
        }

    }

    /**
     * Returns a canonical human-readable notification message for this web page.
     * <p>
     * Implements the {@link CanonicalObject} contract by formatting the URL in the
     * standard form {@code "Page: <url>"}.
     * </p>
     *
     * @return a formatted notification message string with the URL, or {@code "Page: null"} if URL is null
     */
    @Override
    public String notificationMessage() {
        return String.format("Page: %s", url);
    }
}
