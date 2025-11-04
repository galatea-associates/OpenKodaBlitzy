/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 and associated documentation files (the "Software"), to deal in the Software without restriction, 
including without limitation the rights to use, copy, modify, merge, publish, distribute, 
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software 
is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice 
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE 
AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS 
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES 
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.controller.frontendresource;

import com.openkoda.core.controller.frontendresource.AbstractFrontendResourceController;
import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.model.component.ControllerEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

import static com.openkoda.controller.common.URLConstants._HTML;
import static com.openkoda.controller.common.URLConstants._HTML_ORGANIZATION_ORGANIZATIONID;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Controller for rendering authenticated frontend resources with privilege checks and organization context.
 * <p>
 * Renders FrontendResource pages requiring authentication and specific privileges. Enforces 
 * {@code resource.requiredPrivilege} checks before rendering. Used for dashboards, admin pages, 
 * and protected content. Retrieves FrontendResource by URL path or ID, verifies user privileges, 
 * executes ServerJs code, and renders template. Routes under authenticated URL paths 
 * ({@code _HTML}, {@code _HTML_ORGANIZATION_ORGANIZATIONID}).
 * <p>
 * The controller provides two routing mechanisms:
 * <ul>
 *   <li><b>Slug-based routing</b> ({@code _CN}): Uses human-readable URL paths to locate FrontendResource</li>
 *   <li><b>ID-based routing</b> ({@code _CI}): Uses numeric database IDs for stable programmatic access</li>
 * </ul>
 * Both mechanisms support organization-scoped multi-tenant resource access and optional subpath navigation.
 * <p>
 * <b>Authentication &amp; Authorization:</b> All routes require authentication. Each FrontendResource 
 * defines a {@code requiredPrivilege} that is checked before rendering. Unauthorized access results 
 * in error responses.
 * <p>
 * <b>Organization Context:</b> Supports multi-tenant deployments through optional {@code organizationId} 
 * parameter. When absent from path variables, attempts to parse from request parameters 
 * ({@code ORGANIZATIONID} key) using {@link NumberUtils#isCreatable(String)} validation.
 * <p>
 * Example routing patterns:
 * <pre>
 * _HTML/_CN/dashboard              (slug-based, no organization)
 * _HTML_ORGANIZATION_123/_CN/admin (slug-based, organization 123)
 * _HTML/_CI/456                    (ID-based, resource ID 456)
 * </pre>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.component.FrontendResource
 * @see AbstractFrontendResourceController
 * @see com.openkoda.model.Privilege
 */
@Controller
@RequestMapping({_HTML, _HTML_ORGANIZATION_ORGANIZATIONID})
public class RestrictedFrontendResourceController extends AbstractFrontendResourceController {

    @Value("${default.pages.homeview:home}")
    String homeViewName;

    /**
     * Renders authenticated frontend resource by URL path with organization context and privilege enforcement.
     * <p>
     * Handles slug-based URL patterns under {@code _CN} routes. Retrieves FrontendResource from database 
     * by path, verifies user has required privilege, executes ServerJs for dynamic content, and renders 
     * via Thymeleaf. Supports organization-scoped resources via {@code organizationId} parameter. 
     * Falls back to homeview when no path provided. Includes organizationId fallback parsing from 
     * request parameters.
     * 
     * <p>
     * <b>Request Flow:</b>
     * <ol>
     *   <li>Initialize HTTP session via {@code request.getSession(true)}</li>
     *   <li>Normalize frontendResourcePath (use homeViewName if null)</li>
     *   <li>Normalize subPath (empty string if null)</li>
     *   <li>Parse organizationId from requestParams if path variable is null</li>
     *   <li>Delegate to {@code invokeFrontendResourceEntry} for privilege check and rendering</li>
     * </ol>
     * 
     * <p>
     * <b>URL Pattern Examples:</b>
     * <pre>
     * _HTML/_CN/                    (root, uses homeViewName)
     * _HTML/_CN/dashboard           (slug-based path)
     * _HTML/_CN/admin/users         (slug with subpath)
     * _HTML_ORGANIZATION_123/_CN/   (organization-scoped root)
     * </pre>
     * 
     * <p>
     * <b>Privilege Enforcement:</b> The {@code invokeFrontendResourceEntry} method (inherited from 
     * {@link AbstractFrontendResourceController}) checks {@code resource.requiredPrivilege} before 
     * rendering. Access denied response returned if user lacks privilege.
     * 
     *
     * @param organizationId ID of the organization context for multi-tenant resource access. 
     *                       If null, attempts to parse from {@code requestParams} using 
     *                       {@code ORGANIZATIONID} key. Used for organization-scoped FrontendResource 
     *                       lookup and privilege evaluation
     * @param frontendResourcePath URL path to FrontendResource (slug format: letters, numbers, dash). 
     *                             If null, uses {@code homeViewName} from configuration (default: "home"). 
     *                             Matched by {@code FRONTENDRESOURCEREGEX} and {@code URL_WITH_DASH_REGEX} patterns
     * @param subPath Optional subpath for hierarchical resource navigation. Normalized to empty string 
     *                if null. Passed to {@code invokeFrontendResourceEntry} for nested resource routing
     * @param draft If true, renders draft version of FrontendResource for preview. If false (default), 
     *              renders published version. Used for content review before publication
     * @param requestParams Map of all HTTP request parameters. Used for organizationId fallback parsing 
     *                      (via {@link NumberUtils#isCreatable(String)} and {@link Long#parseLong(String)}) 
     *                      and passed to {@code invokeFrontendResourceEntry} for ServerJs context
     * @param form Validated form object for POST requests. Must be {@link AbstractOrganizationRelatedEntityForm} 
     *             subclass. Used for form submission handling with organization context
     * @param request {@link HttpServletRequest} used for session management (ensures session exists via 
     *                {@code getSession(true)}) and HTTP method detection (GET/POST via {@code getMethod()})
     * @param response {@link HttpServletResponse} for potential error handling and response customization
     * @return {@link org.springframework.web.servlet.ModelAndView} with resource content and template name, 
     *         or String view name, or {@link org.springframework.http.ResponseEntity}. Returns error response 
     *         if user lacks required privilege or resource not found
     */
    @RequestMapping(
            value = {
                    _CN + "/",
                    _CN + "/{frontendResourcePath:" + FRONTENDRESOURCEREGEX + "$}",
                    _CN + "/{frontendResourcePath:" + EXCLUDE_SWAGGER_UI_REGEX + URL_WITH_DASH_REGEX + "$}/{subPath:" + FRONTENDRESOURCEREGEX + "$}"
            },
            method = {GET, POST})
    @Transactional
    public Object openFrontendResourcePage(
            @PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
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
        if(organizationId == null && NumberUtils.isCreatable(requestParams.get(ORGANIZATIONID))) {
            organizationId = Long.parseLong(requestParams.get(ORGANIZATIONID));
        }
        return invokeFrontendResourceEntry(organizationId, finalPath, null, subPath, ControllerEndpoint.HttpMethod.valueOf(request.getMethod()), draft, requestParams, form);
    }

    /**
     * Renders authenticated frontend resource by numeric ID with organization context and privilege enforcement.
     * <p>
     * Handles ID-based URL patterns under {@code _CI} routes. Retrieves FrontendResource from database 
     * by numeric ID (instead of slug), verifies user privilege, executes ServerJs, and renders template. 
     * Preferred for programmatic resource access and stable URLs that don't change when resource names 
     * change. Supports organization-scoped resources and subpath navigation. Includes organizationId 
     * fallback parsing from request parameters.
     * 
     * <p>
     * <b>Advantages of ID-based routing:</b>
     * <ul>
     *   <li>Stable URLs - resource ID never changes even when name/path is updated</li>
     *   <li>Direct database lookup - faster than slug-based path matching</li>
     *   <li>Programmatic access - easy to generate URLs from entity IDs</li>
     *   <li>No URL conflicts - numeric IDs guarantee uniqueness</li>
     * </ul>
     * 
     * <p>
     * <b>Request Flow:</b>
     * <ol>
     *   <li>Initialize HTTP session via {@code request.getSession(true)}</li>
     *   <li>Normalize subPath (empty string if null)</li>
     *   <li>Parse organizationId from requestParams if path variable is null</li>
     *   <li>Delegate to {@code invokeFrontendResourceEntry} with frontendResourceId for direct lookup</li>
     * </ol>
     * 
     * <p>
     * <b>URL Pattern Examples:</b>
     * <pre>
     * _HTML/_CI/                    (root with ID)
     * _HTML/_CI/123                 (resource ID 123)
     * _HTML/_CI/456/settings        (resource 456 with subpath)
     * _HTML_ORGANIZATION_789/_CI/123 (organization-scoped ID lookup)
     * </pre>
     * 
     * <p>
     * <b>Privilege Enforcement:</b> The {@code invokeFrontendResourceEntry} method (inherited from 
     * {@link AbstractFrontendResourceController}) checks {@code resource.requiredPrivilege} before 
     * rendering. Returns access denied response if user lacks required privilege.
     * 
     *
     * @param organizationId ID of the organization context for multi-tenant resource access. 
     *                       If null, attempts to parse from {@code requestParams} using 
     *                       {@code ORGANIZATIONID} key with {@link NumberUtils#isCreatable(String)} 
     *                       validation and {@link Long#parseLong(String)} conversion. Used for 
     *                       organization-scoped privilege evaluation
     * @param frontendResourceId Numeric ID of the FrontendResource to render. Direct database ID lookup 
     *                           instead of slug-based path lookup. Provides stable URL even when resource 
     *                           name/path changes. If null, may trigger error handling in 
     *                           {@code invokeFrontendResourceEntry}
     * @param subPath Optional subpath for hierarchical resource navigation. Normalized to empty string 
     *                if null. Passed to {@code invokeFrontendResourceEntry} for nested content routing
     * @param draft If true, renders draft version of FrontendResource for preview. If false (default), 
     *              renders published version. Used for content review before publication
     * @param requestParams Map of all HTTP request parameters. Used for organizationId fallback parsing 
     *                      (via {@link NumberUtils#isCreatable(String)} check) and passed to 
     *                      {@code invokeFrontendResourceEntry} for ServerJs execution context
     * @param form Validated form object for POST requests. Must be {@link AbstractOrganizationRelatedEntityForm} 
     *             subclass. Used for form submission with organization context and validation
     * @param request {@link HttpServletRequest} used for session management (ensures session exists via 
     *                {@code getSession(true)}) and HTTP method detection via {@code getMethod()}
     * @param response {@link HttpServletResponse} for potential error handling and custom response headers
     * @return {@link org.springframework.web.servlet.ModelAndView} with resource content and template name, 
     *         or String view name, or {@link org.springframework.http.ResponseEntity}. Returns access denied 
     *         response if user lacks required privilege or resource not found
     */
    @RequestMapping(
            value = {
                    _CI + "/",
                    _CI + "/{frontendResourceId}",
                    _CI + "/{frontendResourceId}/{subPath:" + FRONTENDRESOURCEREGEX + "$}"
            },
            method = {GET, POST})
    @Transactional
    public Object openFrontendResourcePage(
            @PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(value = "frontendResourceId", required = false) Long frontendResourceId,
            @PathVariable(value = "subPath", required = false) String subPath,
            @RequestParam(value = "draft", required = false, defaultValue = "false") Boolean draft,
            @RequestParam Map<String,String> requestParams,
            @Valid AbstractOrganizationRelatedEntityForm form,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        debug("[openFrontendResourcePage] id {} subpath {}", frontendResourceId, subPath);
        request.getSession(true);
        if(subPath == null) {
            subPath = "";
        }
        if(organizationId == null && NumberUtils.isCreatable(requestParams.get(ORGANIZATIONID))) {
            organizationId = Long.parseLong(requestParams.get(ORGANIZATIONID));
        }
        return invokeFrontendResourceEntry(organizationId, null, frontendResourceId, subPath, ControllerEndpoint.HttpMethod.valueOf(request.getMethod()), draft, requestParams, form);
    }
}
