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

package com.openkoda.core.service;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.component.FrontendResource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * XML-serializable sitemap container representing the root urlset element containing multiple URL entries.
 * <p>
 * This class serves as the root container for generating XML sitemaps that comply with the sitemaps.org protocol.
 * It is annotated with Jackson XML bindings to facilitate automatic serialization to XML format. Each instance
 * contains a collection of {@link SitemapEntry} objects that represent individual URLs to be included in the sitemap.
 * </p>
 * <p>
 * The class implements {@link LoggingComponentWithRequestId} to provide request-scoped logging capabilities,
 * allowing trace correlation across distributed operations. Debug logging is used to track sitemap initialization
 * and entry retrieval operations.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * SitemapIndex sitemap = new SitemapIndex(frontendResources, "https://example.com");
 * }</pre>
 * </p>
 *
 * @see SitemapEntry
 * @see FrontendResource
 * @see JacksonXmlRootElement
 * @since 1.7.1
 * @author OpenKoda Team
 */
@JacksonXmlRootElement(localName = "urlset")
public class SitemapIndex implements Serializable, LoggingComponentWithRequestId {

    /**
     * Constructs a new SitemapIndex by converting a collection of FrontendResource objects to SitemapEntry objects.
     * <p>
     * This constructor performs the following operations:
     * </p>
     * <ul>
     *   <li>Initializes debug logging for sitemap creation tracking</li>
     *   <li>Creates a new ArrayList to store sitemap entries</li>
     *   <li>Iterates through each FrontendResource in the provided collection</li>
     *   <li>Converts each FrontendResource to a SitemapEntry, propagating the baseUrl to construct absolute URLs</li>
     *   <li>Adds each SitemapEntry to the internal entries list</li>
     * </ul>
     * <p>
     * The baseUrl parameter is propagated to each {@link SitemapEntry} constructor to ensure all URLs in the sitemap
     * are absolute and properly formatted according to the sitemaps.org specification.
     * </p>
     *
     * @param entries the collection of FrontendResource objects to include in the sitemap, must not be null
     * @param baseUrl the base URL to prepend to all resource paths, must not be null or empty
     */
    public SitemapIndex(Collection<FrontendResource> entries, String baseUrl) {
        debug("[SitemapIndex]");
        this.entries = new ArrayList<>();
        for (FrontendResource entry : entries) {
            SitemapEntry sitemapEntry = new SitemapEntry(entry, baseUrl);
            this.entries.add(sitemapEntry);
        }
    }

    /**
     * List of sitemap entries representing individual URLs to be included in the sitemap.
     * <p>
     * This field is annotated with {@code @JacksonXmlProperty(localName = "url")} to serialize each entry
     * as a {@code <url>} element in the generated XML. The {@code @JacksonXmlElementWrapper(useWrapping = false)}
     * annotation ensures that entries are rendered as unwrapped {@code <url>} elements directly under the
     * {@code <urlset>} root element, without an intermediate wrapper element.
     * </p>
     * <p>
     * Each {@link SitemapEntry} in this list contains location, last modification date, change frequency,
     * and priority information for a specific URL in the sitemap.
     * </p>
     */
    @JacksonXmlProperty(localName = "url")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<SitemapEntry> entries;

    /**
     * XML namespace attribute for the sitemap schema.
     * <p>
     * This field is serialized as the {@code xmlns} attribute on the root {@code <urlset>} element,
     * declaring conformance to the sitemaps.org schema version 0.9. The namespace is set to
     * {@code "http://www.sitemaps.org/schemas/sitemap/0.9"} which is the official XML namespace
     * for the Sitemap protocol as defined by sitemaps.org.
     * </p>
     */
    @JacksonXmlProperty(isAttribute = true)
    private String xmlns = "http://www.sitemaps.org/schemas/sitemap/0.9";

    /**
     * Returns the list of sitemap entries contained in this sitemap index.
     * <p>
     * This method provides access to the collection of {@link SitemapEntry} objects that represent
     * individual URLs included in the sitemap. Each entry contains location, last modification date,
     * change frequency, and priority information.
     * </p>
     * <p>
     * Debug logging is performed when this method is invoked to track sitemap entry access for
     * troubleshooting and monitoring purposes.
     * </p>
     * <p>
     * Note: This class is not thread-safe. It is intended for single-threaded XML serialization
     * operations. Concurrent access to the returned list may result in undefined behavior.
     * </p>
     *
     * @return the list of sitemap entries, never null
     */
    public List<SitemapEntry> getEntries() {
        debug("[getEntries]");
        return entries;
    }
}
