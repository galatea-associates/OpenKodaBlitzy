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

package com.openkoda.controller.frontendresource;

import com.openkoda.core.controller.frontendresource.AbstractFrontendResourceController;
import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.model.component.ControllerEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Controller for rendering public frontend resources accessible without authentication.
 * <p>
 * Renders FrontendResource pages with public visibility. No login required. Used for landing pages,
 * marketing content, and public documentation. Retrieves FrontendResource by URL path, executes
 * associated ServerJs code, and renders via Thymeleaf template. Routes under public URL paths (/).
 * </p>
 * <p>
 * All URLs under root path made from letters, numbers, and dashes are loaded from database as
 * FrontendResource entities. This enables dynamic content management where pages are stored in the
 * database and can be updated without code deployment. Supports draft preview mode for content review
 * before publication.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * // Accessing root: GET /
 * // Accessing page: GET /about-us
 * // Accessing nested: GET /products/overview
 * }</pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.core.controller.frontendresource.AbstractFrontendResourceController
 * @see com.openkoda.model.FrontendResource
 */
@Controller
@RequestMapping({"/"})
public class PublicFrontendResourceController extends AbstractFrontendResourceController {

    /**
     * Default home view name used when root path (/) is accessed without a specific FrontendResource path.
     * <p>
     * Configured via application property {@code default.pages.homeview} with default value "home".
     * This view is rendered when {@code frontendResourcePath} is null in the root URL request.
     * </p>
     *
     * @see #openFrontendResourcePage(String, String, Boolean, Map, AbstractOrganizationRelatedEntityForm, HttpServletRequest, HttpServletResponse)
     */
    @Value("${default.pages.homeview:home}")
    String homeViewName;

    /**
     * Renders public frontend resource page for both GET and POST requests without authentication requirements.
     * <p>
     * Handles root path (/) and slug-based URL patterns. Retrieves FrontendResource from database by path,
     * executes ServerJs for dynamic content generation, and renders via Thymeleaf template. Falls back to
     * configured {@code homeview} when no path provided. Supports draft preview mode and subpath navigation.
     * </p>
     * <p>
     * URL patterns supported:
     * <ul>
     *   <li>"/" - Root path, uses {@link #homeViewName}</li>
     *   <li>"/{path}" - Slug format (letters, numbers, dash matched by FRONTENDRESOURCEREGEX)</li>
     *   <li>"/{path}/{subPath}" - Nested resource with hierarchical navigation</li>
     * </ul>
     * </p>
     * <p>
     * Processing flow:
     * <ol>
     *   <li>Path normalization: null frontendResourcePath defaults to homeViewName</li>
     *   <li>Session initialization: {@code request.getSession(true)} ensures session exists</li>
     *   <li>Delegation: Calls {@code invokeFrontendResourceEntry} from AbstractFrontendResourceController</li>
     * </ol>
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * GET /about-us -> Renders "about-us" FrontendResource
     * POST /contact?draft=true -> Renders draft "contact" page with form data
     * }</pre>
     * </p>
     *
     * @param frontendResourcePath URL path to FrontendResource in slug format (letters, numbers, dash).
     *                            If null, uses {@code homeViewName} from configuration (default: "home").
     *                            Matched by FRONTENDRESOURCEREGEX pattern.
     * @param subPath Optional subpath for hierarchical resource navigation. Normalized to empty string if null.
     *               Passed to {@code invokeFrontendResourceEntry} for nested resource routing.
     * @param draft If true, renders draft version of FrontendResource for preview. If false (default),
     *             renders published version. Used for content review before publication.
     * @param requestParams Map of all HTTP request parameters. Passed to {@code invokeFrontendResourceEntry}
     *                     for ServerJs context and form population.
     * @param form Validated form object for POST requests. Must be AbstractOrganizationRelatedEntityForm subclass.
     *            Used for form submission handling and validation.
     * @param request HttpServletRequest used for session management (ensures session exists via {@code getSession(true)})
     *               and HTTP method detection (GET/POST).
     * @param response HttpServletResponse for potential error handling and response customization.
     * @return ModelAndView with resource content and template name, or String view name, or ResponseEntity
     *        depending on {@code invokeFrontendResourceEntry} result. Return type varies based on
     *        FrontendResource configuration and execution flow.
     * @see AbstractFrontendResourceController#invokeFrontendResourceEntry
     * @see com.openkoda.model.FrontendResource
     * @see com.openkoda.model.component.ControllerEndpoint.HttpMethod
     */
    @RequestMapping(
            value = {
                    "/",
                    "/{frontendResourcePath:" + FRONTENDRESOURCEREGEX + "$}",
                    "/{frontendResourcePath:" + EXCLUDE_SWAGGER_UI_REGEX + URL_WITH_DASH_REGEX + "$}/{subPath:" + FRONTENDRESOURCEREGEX + "$}"
            },
            method = {GET, POST})
    @Transactional
    public Object openFrontendResourcePage(
            @PathVariable(value = "frontendResourcePath", required = false) String frontendResourcePath,
            @PathVariable(value = "subPath", required = false) String subPath,
            @RequestParam(value = "draft", required = false, defaultValue = "false") Boolean draft,
            @RequestParam Map<String,String> requestParams,
            @Valid AbstractOrganizationRelatedEntityForm form,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        debug("[openFrontendResourcePage] path {}/{}", frontendResourcePath, subPath);
        request.getSession(true);
        String finalPath = frontendResourcePath == null ? homeViewName : frontendResourcePath;
        if(subPath == null) {
            subPath = "";
        }
        return invokeFrontendResourceEntry(null, finalPath, null, subPath, ControllerEndpoint.HttpMethod.valueOf(request.getMethod()), draft, requestParams, form);
    }
}
