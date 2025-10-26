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

import com.openkoda.controller.ComponentProvider;
import com.openkoda.controller.common.PageAttributes;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.dto.CanonicalObject;
import com.openkoda.model.task.HttpRequestTask;
import jakarta.inject.Inject;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;

import java.util.Map;

/**
 * Webhook delivery orchestration service rendering JSON payloads and headers with Thymeleaf templates.
 * <p>
 * This service creates a Thymeleaf {@link Context} with {@link ThymeleafEvaluationContext} enabling Spring bean
 * access via {@code ${@beanName}} expressions within templates. Enqueues {@link HttpRequestTask} records via
 * unsecure repository for asynchronous HTTP POST delivery by background jobs, useful for third-party webhook
 * integrations.
 * </p>
 * <p>
 * Extends {@link ComponentProvider} for repositories/services access, providing convenient access to the platform's
 * data access and business logic layers.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * service.sendToUrlWithCanonical(entity, "https://api.example.com/webhook", "webhook-content", "webhook-headers");
 * </pre>
 * </p>
 *
 * @see HttpRequestTask
 * @see CanonicalObject
 * @see TemplateEngine
 * @see ThymeleafEvaluationContext
 * @see PageModelMap
 * @see ComponentProvider
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Service
public class GenericWebhookService extends ComponentProvider {

    /**
     * Injected Thymeleaf template engine for processing webhook content and header templates.
     */
    @Inject
    private TemplateEngine templateEngine;
    
    /**
     * Application context used for ThymeleafEvaluationContext bean resolution,
     * enabling templates to access Spring beans via {@code ${@serviceName.method()}} expressions.
     */
    @Inject ApplicationContext context;

    /**
     * Sends webhook for a {@link CanonicalObject} entity with Thymeleaf-rendered content and headers.
     * <p>
     * Creates a {@link PageModelMap} with the canonicalObject attribute, renders JSON content from the specified
     * content template, renders HTTP headers from the headers template, constructs an {@link HttpRequestTask} with
     * the URL, message, and headers, then persists via unsecure repository bypassing privilege checks (assumes
     * system-level webhook delivery).
     * </p>
     * <p>
     * Returns {@code true} immediately - actual HTTP POST delivery happens asynchronously by background jobs.
     * Does not wait for HTTP response.
     * </p>
     *
     * @param object the {@link CanonicalObject} entity to include in template context
     * @param url target webhook URL for HTTP POST delivery
     * @param jsonContentTemplateName Thymeleaf template name for rendering JSON message content
     * @param jsonHeadersTemplateName Thymeleaf template name for rendering HTTP headers
     * @return always {@code true} (actual send happens asynchronously)
     * 
     * @see HttpRequestTask
     * @see CanonicalObject
     * @see PageModelMap
     */
    public boolean sendToUrlWithCanonical(CanonicalObject object, String url, String jsonContentTemplateName, String jsonHeadersTemplateName) {
        debug("[sendToUrlWithCanonical]");
        PageModelMap model = new PageModelMap();
        model.put(PageAttributes.canonicalObject, object);
        String message = prepareContent(jsonContentTemplateName, model);
        String headers = prepareContent(jsonHeadersTemplateName, model);
        HttpRequestTask httpRequestTask = new HttpRequestTask(url, message, headers);
        repositories.unsecure.httpRequest.save(httpRequestTask);
        return true;
    }

    /**
     * Renders a Thymeleaf template with model variables and Spring bean access.
     * <p>
     * Constructs a {@link Context} with the current locale from {@link LocaleContextHolder}, injects
     * {@link ThymeleafEvaluationContext} as a special variable enabling {@code ${@serviceName.method()}} 
     * Spring bean access within templates, populates model entries as context variables, and processes 
     * the template to a string.
     * </p>
     * <p>
     * The {@link ThymeleafEvaluationContext} enables templates to call Spring beans directly, for example:
     * {@code ${@emailService.formatAddress(user)}} style expressions.
     * </p>
     *
     * @param templateName the name of the Thymeleaf template to process
     * @param model map of variable names to values for template context
     * @return rendered template content as a string
     * 
     * @see TemplateEngine
     * @see Context
     * @see ThymeleafEvaluationContext
     * @see LocaleContextHolder
     */
    public String prepareContent(String templateName, Map<String, Object> model) {
        debug("[prepareContent] {}", templateName);
        final Context ctx = new Context(LocaleContextHolder.getLocale());

        //modulesInterceptor.emailModelPreHandle(model);
        ctx.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
                new ThymeleafEvaluationContext(context, null));
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            ctx.setVariable(entry.getKey(), entry.getValue());
        }

        return templateEngine.process(templateName, ctx);
    }
}
