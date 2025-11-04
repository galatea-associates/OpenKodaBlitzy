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

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Spring HandlerInterceptor that enables dynamic module registration and UI integration.
 * <p>
 * This interceptor maintains registries of module-provided handler callbacks and invokes them
 * during the HTTP request lifecycle. Modules can register PreHandler, PostHandler, and
 * EmailModelPreHandler implementations to inject module-specific menu items, resources,
 * and data into view models. This enables modules to extend the UI navigation and enrich
 * page models without modifying core application code.
 * <p>
 * The interceptor follows Spring MVC's HandlerInterceptor contract, executing registered
 * handlers at preHandle (before controller execution) and postHandle (after controller
 * execution but before view rendering) phases. PostHandler implementations typically
 * retrieve registered OpenkodaModule instances and add module menu items to the model
 * for navigation rendering in the UI.
 * <p>
 * Thread-safety: This interceptor is thread-safe for read operations. Module discovery
 * and menu injection occur per request using registered handler instances. Handler
 * registration (add operations) should occur during application startup before handling
 * requests.
 * <p>
 * Example usage - Modules register handlers during initialization:
 * <pre>{@code
 * modulesInterceptor.registerPostHandler((req, res, modelAndView) -> {
 *     List<ModuleMenuItem> menuItems = buildModuleMenuItems();
 *     modelAndView.addObject("moduleMenuItems", menuItems);
 * });
 * }</pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.OpenkodaModule
 * @see org.springframework.web.servlet.HandlerInterceptor
 */
@Component
public class ModulesInterceptor implements ReadableCode, LoggingComponentWithRequestId, HandlerInterceptor {

    /**
     * Callback interface for module-provided pre-request handlers.
     * <p>
     * PreHandler implementations are invoked before controller execution during the preHandle
     * phase of the Spring MVC request lifecycle. Handlers can inspect or modify the request,
     * perform authentication checks, or abort request processing by returning false.
     * 
     *
     * @since 1.7.1
     */
    public static interface PreHandler {
        /**
         * Handles pre-request processing for a module.
         *
         * @param req the current HTTP request
         * @param rest the current HTTP response
         * @return true to continue request processing, false to abort
         */
        Boolean preHandle(HttpServletRequest req, HttpServletResponse rest);
    }
    /**
     * Callback interface for module-provided post-request handlers.
     * <p>
     * PostHandler implementations are invoked after controller execution but before view
     * rendering during the postHandle phase of the Spring MVC request lifecycle. Handlers
     * can enrich the ModelAndView with module-specific data such as menu items, resources,
     * or contextual information needed for UI rendering. This is the primary mechanism for
     * modules to inject navigation elements and view model data.
     * 
     *
     * @since 1.7.1
     */
    public static interface PostHandler {
        /**
         * Handles post-request processing for a module, typically enriching the view model.
         *
         * @param req the current HTTP request
         * @param rest the current HTTP response
         * @param modelAndView the view model to enrich with module data (null for direct responses)
         */
        void postHandle(HttpServletRequest req, HttpServletResponse rest, ModelAndView modelAndView);
    }

    /**
     * Callback interface for module-provided email model enrichment handlers.
     * <p>
     * EmailModelPreHandler implementations are invoked before email template rendering
     * to allow modules to inject data into the email template model. This enables modules
     * to add contextual information, branding elements, or module-specific data to
     * outgoing emails.
     * 
     *
     * @since 1.7.1
     */
    public static interface EmailModelPreHandler {
        /**
         * Enriches the email template model with module-specific data.
         *
         * @param model the email template model to enrich
         */
        void preHandle(Map<String, Object> model);
    }

    private List<PreHandler> preHandlers = new ArrayList<>();
    private List<PostHandler> postHandlers = new ArrayList<>();
    private List<EmailModelPreHandler> emailModelPreHandlers = new ArrayList<>();

    /**
     * Invokes all registered EmailModelPreHandler callbacks to enrich an email template model.
     * <p>
     * This method iterates through all registered email model handlers and invokes each one,
     * allowing modules to inject data into the provided model before email rendering.
     * 
     *
     * @param model the email template model to be enriched by modules
     * @return true always (for consistency with interceptor pattern)
     */
    public boolean emailModelPreHandle(Map<String, Object> model) {
        for (EmailModelPreHandler h : emailModelPreHandlers) {
            h.preHandle(model);
        }
        return true;
    }

    /**
     * Invokes all registered PreHandler callbacks before controller execution.
     * <p>
     * This method is called by Spring MVC before the controller handles the request.
     * It iterates through all registered module pre-handlers, invoking each in order.
     * If any handler returns false, the combined result will be false, potentially
     * aborting request processing.
     * 
     *
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @param handler the handler (controller method) that will be executed
     * @return true if all module handlers return true, false if any handler returns false
     * @throws Exception if any handler throws an exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        boolean result = true;
        for (PreHandler h : preHandlers) {
            result &= h.preHandle(request, response);
        }
        return result;
    }

    /**
     * Invokes all registered PostHandler callbacks to enrich the view model with module data.
     * <p>
     * This method is called by Spring MVC after controller execution but before view rendering.
     * It iterates through all registered module post-handlers, allowing each to enrich the
     * ModelAndView with module-specific data such as menu items, navigation elements, or
     * contextual resources. This is the primary integration point where modules inject UI
     * components into page navigation.
     * 
     * <p>
     * Module menu injection logic: PostHandler implementations typically retrieve registered
     * OpenkodaModule instances from the database or configuration, build module menu items
     * based on user permissions and context, and add these items to the model for rendering
     * in the application's navigation bar or sidebar. The model is then enriched with
     * module-specific resources needed for view rendering.
     * 
     * <p>
     * If modelAndView is null (indicating a direct response body such as REST API responses),
     * post handlers are skipped as there is no view model to enrich.
     * 
     *
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @param handler the executed handler (controller method) that processed the request
     * @param modelAndView the view model to enrich with module data (null for direct response bodies)
     * @throws Exception if any handler throws an exception during model enrichment
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        //omit post handlers if modelAndView is null - it is a direct response body
        if (modelAndView == null) {return;}
        for (PostHandler h : postHandlers) {
            h.postHandle(request, response, modelAndView);
        }
    }

    /**
     * Registers a PreHandler callback to be invoked before controller execution.
     * <p>
     * Modules call this method during initialization to register pre-request handlers.
     * Registration should occur during application startup before handling requests.
     * 
     *
     * @param modulePreHandler the pre-handler to register
     */
    public void registerPreHandler(PreHandler modulePreHandler) {
        preHandlers.add(modulePreHandler);
    }

    /**
     * Registers a PostHandler callback to be invoked after controller execution.
     * <p>
     * Modules call this method during initialization to register post-request handlers
     * that enrich view models with module-specific menu items and resources.
     * Registration should occur during application startup before handling requests.
     * 
     *
     * @param modulePostHandler the post-handler to register
     */
    public void registerPostHandler(PostHandler modulePostHandler) {
        postHandlers.add(modulePostHandler);
    }

    /**
     * Registers an EmailModelPreHandler callback to be invoked before email rendering.
     * <p>
     * Modules call this method during initialization to register email model enrichment
     * handlers. Registration should occur during application startup before sending emails.
     * 
     *
     * @param emailModelPreHandler the email model handler to register
     */
    public void registerEmailModelPreHandler(EmailModelPreHandler emailModelPreHandler) {
        emailModelPreHandlers.add(emailModelPreHandler);
    }

}
