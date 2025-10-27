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

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.openkoda.controller.common.PageAttributes.errorMessage;

/**
 * Centralized Thymeleaf template rendering service with locale support.
 * <p>
 * This service processes both named templates and ad-hoc HTML string templates, providing defensive 
 * error-to-text behavior for template processing failures. It implements 
 * {@link LoggingComponentWithRequestId} for request correlation and debugging.
 * </p>
 * <p>
 * The service supports two rendering modes:
 * <ul>
 *   <li>Named template rendering via {@link #prepareContent(String, Map)} for templates resolved by 
 *       the configured {@link TemplateEngine}</li>
 *   <li>Ad-hoc HTML string rendering via {@link #prepareContentForHtml(String, Map)} for dynamic 
 *       template evaluation</li>
 * </ul>
 * </p>
 * <p>
 * Usage example:
 * <pre>{@code
 * String html = service.prepareContent("email/welcome", Map.of("userName", "John"));
 * }</pre>
 * </p>
 * <p>
 * <b>Warning:</b> Error messages are exposed in rendered output. Ensure user-facing error messages 
 * are safe for display.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see TemplateEngine
 * @see Context
 * @see LocaleContextHolder
 * @see StringTemplateResolver
 */
@Service
public class ThymeleafService implements LoggingComponentWithRequestId {

    /**
     * Primary template engine for processing named templates.
     * <p>
     * Configured by Spring with classpath and file-based template resolvers. Used for standard 
     * template rendering operations.
     * </p>
     */
    private TemplateEngine templateEngine;
    
    /**
     * String template resolver for ad-hoc HTML string template processing.
     * <p>
     * Injected but not added to the primary templateEngine (see commented line in constructor). 
     * Used on-demand in {@link #prepareContentForHtml(String, Map)} with a dedicated 
     * TemplateEngine instance for each invocation.
     * </p>
     */
    private StringTemplateResolver stringTemplateResolver;

    /**
     * Constructs ThymeleafService with injected template processing components.
     * <p>
     * Initializes the service with a configured {@link TemplateEngine} and 
     * {@link StringTemplateResolver}. Note that the stringTemplateResolver is not added to the 
     * templateEngine (commented line preserved for reference), as it is used on-demand in separate 
     * TemplateEngine instances for ad-hoc HTML processing.
     * </p>
     *
     * @param templateEngine the primary template engine for named template processing, configured 
     *                       by Spring with appropriate template resolvers
     * @param stringTemplateResolver the resolver for processing ad-hoc HTML string templates, used 
     *                               in {@link #prepareContentForHtml(String, Map)}
     */
    public ThymeleafService(TemplateEngine templateEngine, StringTemplateResolver stringTemplateResolver) {
        this.templateEngine = templateEngine;
        this.stringTemplateResolver = stringTemplateResolver;
//        templateEngine.addTemplateResolver(stringTemplateResolver);
    }

    /**
     * Convenience method for rendering a complete named template without fragment selection.
     * <p>
     * Delegates to {@link #prepareContent(String, String, Map)} with a null fragment parameter, 
     * causing the entire template to be processed and rendered.
     * </p>
     *
     * @param templateName the name of the template to render, resolved by the configured template 
     *                     resolvers (e.g., "email/welcome" or "admin/dashboard")
     * @param model map of variable names to values to populate the template context
     * @return the rendered HTML string for the complete template
     * @see #prepareContent(String, String, Map)
     */
    public String prepareContent(String templateName, Map<String, Object> model) {
        return prepareContent(templateName, null, model);
    }

    /**
     * Renders a named template with optional fragment selection and locale-aware context.
     * <p>
     * Constructs a Thymeleaf {@link Context} with the current locale from 
     * {@link LocaleContextHolder} and populates it with the provided model variables. Processes 
     * the template using the configured {@link TemplateEngine} with optional fragment selection.
     * </p>
     * <p>
     * Fragment selection allows rendering only a specific portion of the template (e.g., a 
     * particular div or section marked with th:fragment). If fragment is null, the entire template 
     * is rendered.
     * </p>
     *
     * @param templateName the name of the template to render, resolved by the configured template 
     *                     resolvers (e.g., "email/welcome" or "admin/dashboard")
     * @param fragment optional Thymeleaf fragment selector (e.g., "content" to render only 
     *                 th:fragment="content" sections), or null to render the entire template
     * @param model map of variable names to values to populate the template context
     * @return the rendered HTML string for the specified template or fragment
     * @see TemplateEngine#process(String, Set, Context)
     * @see Context
     * @see LocaleContextHolder#getLocale()
     */
    public String prepareContent(String templateName, String fragment, Map<String, Object> model) {
        debug("[prepareContent] {}", templateName);
        final Context ctx = new Context(LocaleContextHolder.getLocale());

        for (Map.Entry<String, Object> entry : model.entrySet()) {
            ctx.setVariable(entry.getKey(), entry.getValue());
        }

        Set<String> fragments = fragment != null ? Collections.singleton(fragment) : Collections.emptySet();
        String result = templateEngine.process(templateName, fragments,  ctx);
        return result;
    }

    /**
     * Processes ad-hoc HTML string templates with defensive error handling.
     * <p>
     * This method provides special handling for dynamic template evaluation:
     * </p>
     * <ul>
     *   <li>If the model contains an "errorMessage" attribute, replaces the HTML with an error 
     *       display span</li>
     *   <li>Returns empty string for blank input HTML</li>
     *   <li>Creates a new {@link TemplateEngine} instance with {@link StringTemplateResolver} for 
     *       each invocation</li>
     *   <li>Catches {@link TemplateProcessingException} and returns the cause message instead of 
     *       propagating (defensive error handling)</li>
     * </ul>
     * <p>
     * <b>Performance Note:</b> This method creates a new TemplateEngine per call and is not 
     * optimized for high-volume use. It is designed for dynamic template evaluation in scenarios 
     * where template content is constructed at runtime.
     * </p>
     * <p>
     * <b>Security Warning:</b> Error messages are exposed in rendered output. Ensure error messages 
     * are safe for display to end users.
     * </p>
     *
     * @param html raw HTML template string containing Thymeleaf expressions (e.g., 
     *             "<div th:text=\"${userName}\"></div>")
     * @param model map of variable names to values to populate the template context
     * @return the rendered HTML string, an error message string if template processing fails, or 
     *         empty string if input is blank
     * @see TemplateEngine#process(String, Context)
     * @see StringTemplateResolver
     * @see TemplateProcessingException
     */
    public String prepareContentForHtml(String html, Map<String, Object> model) {
        debug("[prepareContentForHtml]");
        if(model.containsKey(errorMessage.name)) {
            html = "<span th:text=\"${errorMessage}\"></span>";
        } else if(StringUtils.isBlank(html)) {
            return StringUtils.EMPTY;
        }
        TemplateEngine stringTemplateEngine = new TemplateEngine();
        stringTemplateEngine.addTemplateResolver(stringTemplateResolver);
        final Context ctx = new Context(LocaleContextHolder.getLocale());

        for (Map.Entry<String, Object> entry : model.entrySet()) {
            ctx.setVariable(entry.getKey(), entry.getValue());
        }
        stringTemplateEngine.getTemplateResolvers();
        try {
            return stringTemplateEngine.process(html, ctx);
        } catch (TemplateProcessingException e) {
            return e.getCause().getMessage();
        }
    }


}
