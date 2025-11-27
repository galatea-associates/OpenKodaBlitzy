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

import com.openkoda.model.component.FrontendResource;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A serializable DTO representing a single URL entry in an XML sitemap.
 * <p>
 * This class is used for sitemap.xml generation, containing the location URL
 * and last modification timestamp for a frontend resource. Each entry corresponds
 * to one {@code <url>} element in the sitemap following the sitemaps.org protocol.
 * 
 *
 * @see FrontendResource
 * @see SitemapIndex
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class SitemapEntry implements Serializable {

    private String loc;
    private LocalDateTime lastmod;

    /**
     * Constructs a sitemap entry from a FrontendResource.
     * <p>
     * This constructor converts a FrontendResource to a sitemap entry by constructing
     * the complete URL and initializing the last modification timestamp. URL construction
     * handles special cases: the root resource with name "/" is mapped to the base URL
     * without a trailing path segment, while other resources append their name to the base URL.
     * 
     *
     * @param entry the FrontendResource to convert to a sitemap entry
     * @param baseUrl the base URL for the application (e.g., "https://example.com")
     */
    public SitemapEntry(FrontendResource entry, String baseUrl) {
        this.loc = baseUrl + "/" + ("/".equals(entry.getName()) ? "" : entry.getName());
        this.lastmod = entry.getUpdatedOn();
    }

    /**
     * Returns the complete URL location for this sitemap entry.
     * <p>
     * This is the {@code <loc>} element in the sitemap XML, representing
     * the full URL where the resource can be accessed.
     * 
     *
     * @return the complete URL location
     */
    public String getLoc() {
        return loc;
    }

    /**
     * Sets the URL location for this sitemap entry.
     *
     * @param loc the complete URL location to set
     */
    public void setLoc(String loc) {
        this.loc = loc;
    }

    /**
     * Returns the last modification date as an ISO_LOCAL_DATE formatted string.
     * <p>
     * This is the {@code <lastmod>} element in the sitemap XML, formatted according
     * to the ISO 8601 date format (yyyy-MM-dd) as required by the sitemap protocol.
     * 
     *
     * @return the last modification date formatted as ISO_LOCAL_DATE string
     */
    public String getLastmod() {
        return lastmod.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * Sets the last modification timestamp for this sitemap entry.
     *
     * @param lastmod the last modification timestamp to set
     */
    public void setLastmod(LocalDateTime lastmod) {
        this.lastmod = lastmod;
    }
}
