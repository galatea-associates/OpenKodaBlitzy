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

import com.openkoda.controller.frontendresource.RestrictedFrontendResourceController;
import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.Tuple;
import com.openkoda.core.form.CRUDControllerConfiguration;
import com.openkoda.core.helper.JsonHelper;
import com.openkoda.core.helper.ModelEnricherInterceptor;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.form.PageBuilderForm;
import com.openkoda.model.component.FrontendResource;
import com.openkoda.model.file.File;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.server.RequestPath;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.view.RedirectView;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebExpressionContext;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.security.Principal;
import java.util.*;

import static com.openkoda.controller.common.URLConstants.*;
import static org.springframework.web.util.ServletRequestPathUtils.PATH_ATTRIBUTE;

/**
 * MVC controller for dynamic dashboard and page-builder feature with embedded widget rendering.
 * <p>
 * Handles create/edit/save flows for FrontendResource dashboards. Composes embedded widget HTML 
 * by dispatching to other controllers (RestrictedFrontendResourceController, CRUDControllerHtml) 
 * using request wrapper pattern. Wraps requests with EmbeddedHttpServletRequest/FakeHttpServletRequest, 
 * deserializes dashboard JSON via JsonHelper, uses Thymeleaf TemplateEngine.process to render 
 * delegated ModelAndView fragments. Assembles complete dashboard from individual widget responses.
 * </p>
 * <p>
 * Request mappings support both global and organization-scoped page builders with base paths 
 * "/page-builder" or "/dashboard". Dashboard configuration is stored as JSON in FrontendResource 
 * content field, defining widget layout and settings.
 * </p>
 * <p>
 * Thread-safety: Stateless controller, thread-safe. Request wrapping is per-request.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FrontendResource
 * @see RestrictedFrontendResourceController
 * @see CRUDControllerHtml
 */
@Controller
@RequestMapping({_HTML_ORGANIZATION_ORGANIZATIONID + _PAGEBUILDER, _HTML + _PAGEBUILDER})
public class PageBuilderController extends AbstractController implements HasSecurityRules {

    @Inject
    HtmlCRUDControllerConfigurationMap crudControllerConfigurationMap;

    /**
     * Data transfer record for available embeddable widget components in page builder.
     * <p>
     * Aggregates all component types that can be embedded in dashboard: FrontendResource 
     * widgets, UI components, CRUD table configurations, and image files for visual content.
     * </p>
     *
     * @param frontendResources List of embeddable FrontendResource definitions
     * @param uiComponents List of embeddable UI component definitions
     * @param tables Set of exposed CRUD controller configurations for table widgets
     * @param images List of image files available for dashboard backgrounds and content
     */
    public record EmbeddableComponents(List<Tuple> frontendResources, List<Tuple> uiComponents, Set<Map.Entry<String, CRUDControllerConfiguration>> tables, List<File> images){};

    /**
     * Displays form for creating new dashboard.
     * <p>
     * Loads available embeddable components (frontend resources, UI components, CRUD tables, 
     * images) and initializes empty PageBuilderForm. Renders page builder editor interface 
     * where users can drag-drop widgets and configure dashboard layout.
     * </p>
     * <p>
     * HTTP mapping: GET /page-builder/new
     * </p>
     *
     * @param organizationId Optional organization ID for organization-scoped dashboards, null for global
     * @return ModelAndView with empty dashboard form and available widget types
     */
    @GetMapping({_NEW_SETTINGS})
    @ResponseBody
    @PreAuthorize(CHECK_CAN_ACCESS_GLOBAL_SETTINGS)
    public Object newPage(@PathVariable(value = ORGANIZATIONID, required = false) Long organizationId) {
        return Flow.init()
            .thenSet(embeddableComponents, a -> new EmbeddableComponents(
                    repositories.unsecure.frontendResource.findAllEmbeddableResources(),
                    repositories.unsecure.frontendResource.findAllEmbeddableUiComponents(),
                    crudControllerConfigurationMap.getExposed(),
                    repositories.unsecure.file.findByContentTypeStartsWith("image/")
            ))
            .thenSet(pageBuilderForm, a -> new PageBuilderForm(organizationId, null))
            .execute().mav("page/builder");
    }

    /**
     * Displays dashboard editor with existing configuration.
     * <p>
     * Loads FrontendResource entity by ID and populates PageBuilderForm with saved dashboard 
     * JSON configuration. Provides same embeddable component catalog as new dashboard flow. 
     * Users can modify widget layout, add/remove widgets, update configurations.
     * </p>
     * <p>
     * HTTP mapping: GET /page-builder/{id}
     * </p>
     *
     * @param organizationId Optional organization ID for organization-scoped dashboards, null for global
     * @param id FrontendResource ID of dashboard to edit
     * @return ModelAndView with dashboard JSON configuration loaded into editor
     */
    @GetMapping({_ID_SETTINGS})
    @ResponseBody
    @PreAuthorize(CHECK_CAN_ACCESS_GLOBAL_SETTINGS)
    public Object edit(@PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
                       @PathVariable("id") Long id) {
        return Flow.init()
            .thenSet(embeddableComponents, a -> new EmbeddableComponents(
                    repositories.unsecure.frontendResource.findAllEmbeddableResources(),
                    repositories.unsecure.frontendResource.findAllEmbeddableUiComponents(),
                    crudControllerConfigurationMap.getExposed(),
                    repositories.unsecure.file.findByContentTypeStartsWith("image/")
            ))
            .thenSet(frontendResourceEntity, a -> repositories.secure.frontendResource.findOne(id))
            .thenSet(pageBuilderForm, a -> new PageBuilderForm(organizationId, a.result))
            .execute().mav("page/builder");
    }

    /**
     * Saves new dashboard configuration to FrontendResource.
     * <p>
     * Validates PageBuilderForm, creates new FrontendResource entity with dashboard JSON 
     * content, persists to database via secure repository. Redirects to edit page for 
     * newly created dashboard.
     * </p>
     * <p>
     * HTTP mapping: POST /page-builder/new
     * </p>
     *
     * @param organizationId Optional organization ID for organization-scoped dashboard, null for global
     * @param form PageBuilderForm with dashboard JSON configuration, name, and metadata
     * @param br BindingResult for validation errors
     * @return RedirectView to dashboard edit page on success
     */
    @PostMapping({_NEW_SETTINGS})
    @ResponseBody
    @PreAuthorize(CHECK_CAN_ACCESS_GLOBAL_SETTINGS)
    public Object saveNew(@PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
                          @Valid PageBuilderForm form,
                          BindingResult br) {
        FrontendResource fr = Flow.init()
                .then(a -> new FrontendResource(organizationId))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br, a.result))
                .thenSet(frontendResourceEntity, a -> repositories.secure.frontendResource.save(a.result))
                .thenSet(pageBuilderForm, a -> new PageBuilderForm(organizationId, a.result))
                .execute().get(frontendResourceEntity);
        return new RedirectView(_HTML + _PAGEBUILDER + "/" + fr.getId() + _SETTINGS);
    }

    /**
     * Saves updated dashboard configuration to existing FrontendResource.
     * <p>
     * Loads existing FrontendResource by ID, validates PageBuilderForm, populates updated 
     * dashboard JSON content to entity, persists changes via secure repository. Redirects 
     * back to edit page to show saved changes.
     * </p>
     * <p>
     * HTTP mapping: POST /page-builder/{id}
     * </p>
     *
     * @param organizationId Optional organization ID for organization-scoped dashboard, null for global
     * @param form PageBuilderForm with updated dashboard JSON configuration
     * @param frontendResourceId FrontendResource ID of dashboard to update
     * @param br BindingResult for validation errors
     * @return RedirectView to dashboard edit page on success
     */
    @PostMapping({_ID_SETTINGS})
    @ResponseBody
    @PreAuthorize(CHECK_CAN_ACCESS_GLOBAL_SETTINGS)
    public Object save(@PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
                       @Valid PageBuilderForm form,
                       @PathVariable(value = ID, required = false) Long frontendResourceId,
                       BindingResult br) {
        Flow.init()
                .thenSet(frontendResourceEntity, a -> repositories.unsecure.frontendResource.findDashboardDefinition(frontendResourceId))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br, a.result))
                .thenSet(frontendResourceEntity, a -> repositories.secure.frontendResource.save(a.result))
                .thenSet(pageBuilderForm, a -> new PageBuilderForm(organizationId, a.result))
            .execute();
        return new RedirectView(_HTML + _PAGEBUILDER + "/" + frontendResourceId + _SETTINGS);

    }

    /**
     * HttpServletRequest wrapper for embedded widget rendering with view parameter override.
     * <p>
     * Wraps original dashboard request to inject "__view=embedded" parameter when delegating 
     * to widget controllers. This signals widget controllers to render in embedded mode 
     * without full page chrome. Preserves all other request parameters and attributes.
     * </p>
     * <p>
     * Usage: EmbeddedHttpServletRequest wrapped = new EmbeddedHttpServletRequest(originalRequest);
     * </p>
     */
    public static class EmbeddedHttpServletRequest extends HttpServletRequestWrapper {

        /**
         * Constructs a request object wrapping the given request.
         *
         * @param request The request to wrap
         * @throws IllegalArgumentException if the request is null
         */
        public EmbeddedHttpServletRequest(HttpServletRequest request) {
            super(request);
        }

        /**
         * Overrides parameter retrieval to inject embedded view parameter.
         * <p>
         * Returns "embedded" for "__view" parameter name to signal embedded rendering mode.
         * All other parameters delegate to wrapped request.
         * </p>
         *
         * @param name Parameter name to retrieve
         * @return "embedded" if name is "__view", otherwise delegates to wrapped request
         */
        @Override
        public String getParameter(String name) {
            if ("__view".equals(name)) {
                return "embedded";
            }
            return super.getParameter(name);
        }
    }

    /**
     * HttpServletRequest wrapper for widget dispatching with customizable URI and query parameters.
     * <p>
     * Wraps original request to override requestURI, queryString, and requestURL for delegating 
     * to widget controllers. Allows dashboard controller to dispatch to specific widget endpoints 
     * (e.g., table CRUD, frontend resource page) while preserving authentication and session context.
     * Essential for request handler mapping to route to correct widget controller.
     * </p>
     * <p>
     * All other request attributes (cookies, headers, authentication, session) delegate to wrapped 
     * request for security and context preservation.
     * </p>
     * <p>
     * Usage: FakeHttpServletRequest fake = new FakeHttpServletRequest(original, "/widget-path", "param=value", null);
     * </p>
     *
     * @see RequestPath
     */
    public static class FakeHttpServletRequest extends HttpServletRequestWrapper {

        private final String requestURI;
        private final String queryString;
        private final String requestURL;

        /**
         * Constructs request wrapper with custom URI and query string for widget dispatch.
         *
         * @param request Original HttpServletRequest to wrap
         * @param newRequestURI Custom request URI for widget endpoint routing
         * @param newQueryString Custom query string with widget-specific parameters
         * @param newRequestURL Custom request URL (typically http://localhost:8080 + newRequestURI)
         */
        public FakeHttpServletRequest(HttpServletRequest request,  String newRequestURI, String newQueryString, String newRequestURL) {
            super(request);
            this.requestURI = newRequestURI;
            this.queryString = newQueryString;
            this.requestURL = "http://localhost:8080" + newRequestURI;
        }

        /**
         * Returns custom request URI for widget endpoint routing.
         *
         * @return Custom requestURI if set during construction, otherwise delegates to wrapped request
         */
        @Override
        public String getRequestURI() {
            return requestURI != null ? requestURI : super.getRequestURI();
        }

        /**
         * Returns custom query string with widget-specific parameters.
         *
         * @return Custom queryString if set during construction, otherwise delegates to wrapped request
         */
        @Override
        public String getQueryString() {
            return queryString != null ? queryString : super.getQueryString();
        }

        /**
         * Returns custom request URL for widget dispatch.
         *
         * @return Custom requestURL as StringBuffer if set, otherwise delegates to wrapped request
         */
        @Override
        public StringBuffer getRequestURL() {
            return new StringBuffer(requestURL != null ? requestURL : super.getRequestURL().toString());
        }

        @Override
        public String getAuthType() {
            return super.getAuthType();
        }

        @Override
        public Cookie[] getCookies() {
            return super.getCookies();
        }

        @Override
        public long getDateHeader(String name) {
            return super.getDateHeader(name);
        }

        @Override
        public String getHeader(String name) {
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return super.getHeaderNames();
        }

        @Override
        public int getIntHeader(String name) {
            return super.getIntHeader(name);
        }

        @Override
        public HttpServletMapping getHttpServletMapping() {
            return super.getHttpServletMapping();
        }

        @Override
        public String getMethod() {
            return super.getMethod();
        }

        @Override
        public String getPathInfo() {
            return super.getPathInfo();
        }

        @Override
        public String getPathTranslated() {
            return super.getPathTranslated();
        }

        @Override
        public String getContextPath() {
            return super.getContextPath();
        }

        @Override
        public String getRemoteUser() {
            return super.getRemoteUser();
        }

        @Override
        public boolean isUserInRole(String role) {
            return super.isUserInRole(role);
        }

        @Override
        public Principal getUserPrincipal() {
            return super.getUserPrincipal();
        }

        @Override
        public String getRequestedSessionId() {
            return super.getRequestedSessionId();
        }

        @Override
        public String getServletPath() {
            return super.getServletPath();
        }

        @Override
        public HttpSession getSession(boolean create) {
            return super.getSession(create);
        }

        @Override
        public HttpSession getSession() {
            return super.getSession();
        }

        @Override
        public Collection<Part> getParts() throws IOException, ServletException {
            return super.getParts();
        }

        @Override
        public Part getPart(String name) throws IOException, ServletException {
            return super.getPart(name);
        }

        @Override
        public Map<String, String> getTrailerFields() {
            return super.getTrailerFields();
        }

        /**
         * Returns request attribute with special handling for RequestPath.
         * <p>
         * Parses custom requestURI into RequestPath for PATH_ATTRIBUTE to enable Spring 
         * request handler mapping with overridden URI. Essential for routing widget dispatch 
         * to correct controller endpoints.
         * </p>
         *
         * @param name Attribute name to retrieve
         * @return RequestPath parsed from custom URI if PATH_ATTRIBUTE, otherwise delegates to wrapped request
         */
        @Override
        public Object getAttribute(String name) {
            if (PATH_ATTRIBUTE.equals(name)) {
                return RequestPath.parse(requestURI, getContextPath());
            }
            return super.getAttribute(name);
        }
    }

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    TemplateEngine templateEngine;

    @Autowired
    ViewResolver viewResolver;

    @Autowired
    ModelEnricherInterceptor modelEnricherInterceptor;

    @Autowired
    RestrictedFrontendResourceController restrictedFrontendResourceController;

    @Autowired
    CRUDControllerHtml crudControllerHtml;

    @Autowired
    ApplicationContext context;

    /**
     * Renders complete dashboard by composing widget fragments.
     * <p>
     * Loads FrontendResource dashboard definition by ID or name, deserializes dashboard JSON 
     * configuration containing widget array. Iterates each widget definition, creates 
     * EmbeddedHttpServletRequest wrapping original request with widget-specific parameters, 
     * dispatches to appropriate widget controller endpoint based on widget type (webEndpoint, 
     * frontendResource, table). Captures ModelAndView response from widget controller, processes 
     * through Thymeleaf TemplateEngine to render HTML fragment, assembles all widget HTML 
     * fragments into complete dashboard grid layout.
     * </p>
     * <p>
     * Widget dispatch mechanism uses FakeHttpServletRequest/Response to capture sub-controller 
     * responses without HTTP round-trip. Preserves authentication and session context from 
     * original request while routing to widget-specific endpoints.
     * </p>
     * <p>
     * HTTP mapping: GET /page-builder/{id}/view
     * </p>
     * <p>
     * Supported widget types:
     * <ul>
     *   <li>webEndpoint: Dispatches to RestrictedFrontendResourceController.openFrontendResourcePage by name</li>
     *   <li>frontendResource: Dispatches to RestrictedFrontendResourceController.openFrontendResourcePage by ID</li>
     *   <li>table: Dispatches to CRUDControllerHtml.getAll for CRUD table widget</li>
     * </ul>
     * </p>
     *
     * @param organizationId Optional organization ID for organization-scoped dashboard, null for global
     * @param id Dashboard FrontendResource ID
     * @param commonSearch Common search parameter passed to table widgets
     * @param requestParams All request parameters passed through to widget controllers
     * @param request Original HttpServletRequest for wrapping and context preservation
     * @param response HttpServletResponse for widget controller invocation
     * @return ModelAndView with assembled HTML containing all widget fragments rendered in dashboard layout
     * @throws Exception If widget controller throws exception during dispatch or rendering fails
     */
    @GetMapping(_ID + "/view")
    @ResponseBody
    public Object invokeUrls(@PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
                             @PathVariable("id") Long id,
                             @RequestParam(required = false, defaultValue = "", name = "obj_search") String commonSearch,
                             @RequestParam Map<String,String> requestParams,
                             HttpServletRequest request, HttpServletResponse response) {
        String dashboardName = requestParams.get("dn");
        return Flow.init()
                .thenSet(frontendResourceEntity, a ->
                        StringUtils.isBlank(dashboardName) ?
                                repositories.unsecure.frontendResource.findDashboardDefinition(id) :
                                repositories.unsecure.frontendResource.findDashboardDefinitionByName(dashboardName))
                .then(a -> {

                    List<Object> responses = JsonHelper.from(a.result.getContent(), List.class);
                    for (Object wo : responses) {
                        Map<String, Object> w = (Map<String, Object>)wo;
                        try {
                            boolean generate = Boolean.parseBoolean(w.get("generate") + "");
                            if (!generate) {
                                continue;
                            }

                            EmbeddedHttpServletRequest r = new EmbeddedHttpServletRequest(request);
                            String widgetId = w.get("id") + "";
                            String widgetName = w.get("name") + "";
                            String widgetType = w.get("type") + "";
                            Object resp = null;
                            switch (widgetType) {
                                case "webEndpoint":
                                    resp = restrictedFrontendResourceController.openFrontendResourcePage(organizationId, widgetName, null, false, requestParams, null, r, response);
                                    break;
                                case "frontendResource":
                                    Long webEndpointId = Long.parseLong(widgetId);
                                    resp = restrictedFrontendResourceController.openFrontendResourcePage(organizationId, webEndpointId, null, false, requestParams, null, r, response);
                                    break;
                                case "table":
                                    resp = crudControllerHtml.getAll(organizationId, widgetId, commonSearch, r);
                                    break;

                            }

                            if (resp instanceof ModelAndView) {
                                ModelAndView mav = (ModelAndView) resp;
                                modelEnricherInterceptor.postHandle(r, response, null, mav);
                                IServletWebExchange exchange = JakartaServletWebApplication.buildApplication(r.getServletContext()).buildExchange(r, response);
                                final IEngineConfiguration configuration = templateEngine.getConfiguration();
                                WebExpressionContext ctx = new WebExpressionContext(configuration, exchange);
                                ctx.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
                                        new ThymeleafEvaluationContext(context, null));

                                ctx.setVariables(mav.getModel());
                                String html = templateEngine.process(mav.getViewName(), ctx);
                                w.put("content", html);
                            } else {
                                w.put("content", "Widget is not a HTML");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    a.model.get(frontendResourceEntity).setContent(JsonHelper.to(responses));
                    return responses;
                })

        .execute().mav("page/view");

    }

}
