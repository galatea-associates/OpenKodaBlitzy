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

package com.openkoda.controller;

import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.service.SitemapIndex;
import com.openkoda.model.component.FrontendResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Collection;

/**
 * XML sitemap generation controller for SEO optimization.
 * <p>
 * Provides XML sitemap endpoints (index, general, pages sitemap) for search engine crawlers.
 * Returns ModelAndView for XML views or serializes SitemapIndex as application/xml via @ResponseBody.
 * Uses repositories.unsecure.frontendResource.getEntriesToSitemap() for public page discovery and
 * services.url.getBaseUrl() for absolute URL construction. Conforms to sitemaps.org protocol.
 * 
 * <p>
 * Request mappings:
 * <ul>
 *   <li>GET /sitemap.xml - Sitemap index listing all sub-sitemaps</li>
 *   <li>GET /sitemap-general.xml - Static pages sitemap</li>
 *   <li>GET /sitemap-pages.xml - Dynamic FrontendResource pages sitemap</li>
 * </ul>
 * <p>
 * SEO notes: Sitemap helps search engines discover pages efficiently. Update frequencies guide
 * crawler priorities. Static pages have higher priority than dynamic content.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.core.service.SitemapIndex
 * @see com.openkoda.model.component.FrontendResource
 */
@Controller
public class SitemapController extends AbstractController {

    /**
     * Generates sitemap with static and general pages.
     * <p>
     * HTTP mapping: GET /sitemap-general.xml
     * 
     * <p>
     * Creates XML urlset with static pages including home, about, contact, etc. Includes fixed
     * priorities and weekly change frequencies for general site pages.
     * 
     * <p>
     * Response format:
     * <pre>{@code
     * <?xml version="1.0" encoding="UTF-8"?>
     * <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
     *   <url>
     *     <loc>https://example.com/</loc>
     *     <changefreq>weekly</changefreq>
     *     <priority>1.0</priority>
     *   </url>
     * </urlset>
     * }</pre>
     * 
     *
     * @return ModelAndView rendering XML view with static pages, includes base URL for absolute links
     */
    @RequestMapping(value = _GENERAL_SITEMAP + _XML_EXTENSION, headers = _XML_HEADER, produces = MediaType.APPLICATION_XML_VALUE)
    public Object getGeneralSitemap() {
        debug("[getGeneralSitemap]");
        return new ModelAndView(XML  + _GENERAL_SITEMAP + _XML_EXTENSION, baseUrl.name, services.url.getBaseUrl());
    }

    /**
     * Generates sitemap index listing all sub-sitemaps.
     * <p>
     * HTTP mapping: GET /sitemap.xml
     * 
     * <p>
     * Creates sitemap index referencing /sitemap-general.xml, /sitemap-pages.xml, etc.
     * Conforms to sitemaps.org index format. Content-type: application/xml.
     * 
     * <p>
     * Response format:
     * <pre>{@code
     * <?xml version="1.0" encoding="UTF-8"?>
     * <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
     *   <sitemap>
     *     <loc>https://example.com/sitemap-general.xml</loc>
     *   </sitemap>
     *   <sitemap>
     *     <loc>https://example.com/sitemap-pages.xml</loc>
     *   </sitemap>
     * </sitemapindex>
     * }</pre>
     * 
     *
     * @return ModelAndView rendering XML view with sitemapindex root element listing sub-sitemap locations
     */
    @RequestMapping(value = _SITEMAP_INDEX + _XML_EXTENSION, headers = _XML_HEADER, produces = MediaType.APPLICATION_XML_VALUE)
    public Object getSitemapIndex() {
        debug("[getSiteMapIndex]");
        return new ModelAndView(XML  + _SITEMAP_INDEX + _XML_EXTENSION, baseUrl.name, services.url.getBaseUrl());
    }

    /**
     * Redirects sitemap requests to the sitemap index.
     * <p>
     * HTTP mapping: GET /sitemap.xml (alternative entry point)
     * 
     * <p>
     * Provides a redirect from /sitemap.xml to /sitemap.xml for consistent access.
     * Returns a RedirectView that exposes no model attributes for clean redirection.
     * 
     *
     * @return RedirectView redirecting to sitemap index endpoint
     */
    @RequestMapping(value = _SITEMAP + _XML_EXTENSION, headers = _XML_HEADER, produces = MediaType.APPLICATION_XML_VALUE)
    public Object getSitemap() {
        debug("[getSitemap]");

        RedirectView redirectView = new RedirectView();
        redirectView.setExposeModelAttributes(false);
        redirectView.setUrl(_SITEMAP_INDEX + _XML_EXTENSION);
        return redirectView;
    }

    /**
     * Generates sitemap with dynamic FrontendResource pages.
     * <p>
     * HTTP mapping: GET /sitemap-pages.xml
     * 
     * <p>
     * Queries repositories.unsecure.frontendResource.getEntriesToSitemap() for public pages,
     * builds absolute URLs via services.url.getBaseUrl() + resource.urlPath, includes lastmod
     * dates, sets priority based on page importance, returns urlset XML.
     * 
     * <p>
     * Response format:
     * <pre>{@code
     * <?xml version="1.0" encoding="UTF-8"?>
     * <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
     *   <url>
     *     <loc>https://example.com/page-1</loc>
     *     <lastmod>2023-12-15</lastmod>
     *     <changefreq>weekly</changefreq>
     *     <priority>0.8</priority>
     *   </url>
     * </urlset>
     * }</pre>
     * 
     *
     * @return SitemapIndex serialized as XML with public FrontendResource URLs, includes modification dates
     */
    @RequestMapping(value = _PAGES_SITEMAP + _XML_EXTENSION, headers = _XML_HEADER, produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public SitemapIndex getPagesSitemap() {
        debug("[getPagesSitemap]");
        Collection<FrontendResource> entries = repositories.unsecure.frontendResource.getEntriesToSitemap();
        return new SitemapIndex(entries, services.url.getBaseUrl());
    }
}
