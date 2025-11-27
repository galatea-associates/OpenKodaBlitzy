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

package com.openkoda.core.service.email;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;

import com.openkoda.controller.common.PageAttributes;
import com.openkoda.controller.common.URLConstants;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.helper.ModulesInterceptor;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.dto.CanonicalObject;
import com.openkoda.model.User;
import com.openkoda.model.file.File;
import com.openkoda.model.task.Email;

import jakarta.inject.Inject;

/**
 * Spring service for email content assembly, building {@link Email} entities from Thymeleaf templates.
 * <p>
 * This service resolves email configuration from application properties, composes subject, body, and
 * recipients from template variables, and handles {@link com.openkoda.model.file.File} attachments
 * by mapping File entity IDs to the Email's filesId list. It creates Email records suitable for
 * persistence by EmailService and asynchronous sending by EmailSenderJob.

 * <p>
 * The email preparation workflow involves:
 * <ul>
 *   <li>Resolving Thymeleaf template by name with {@link URLConstants#EMAILRESOURCE_DISCRIMINATOR} suffix</li>
 *   <li>Enriching model with application metadata (name, baseUrl, replyTo address)</li>
 *   <li>Processing template with provided variables to generate HTML content</li>
 *   <li>Optionally extracting subject from HTML {@code <title>} tag</li>
 *   <li>Scheduling email delivery with configurable delay via startAfter timestamp</li>
 * </ul>

 * <p>
 * Example usage:
 * <pre>
 * Email email = emailConstructor.prepareEmail("user@example.com", "John Doe", 
 *     "Welcome", "welcome-template", 0, modelMap, attachmentFile);
 * </pre>

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @see com.openkoda.model.task.Email
 * @see com.openkoda.core.service.email.EmailService
 * @since 1.7.1
 */
@Service
public class EmailConstructor implements LoggingComponentWithRequestId {

    /**
     * Application base URL for generating absolute links in email templates.
     * Configured via {@code base.url} property, defaults to http://localhost:8080.
     */
    @Value("${base.url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Application name displayed in email sender field (nameFrom) and available to templates.
     * Configured via {@code application.name} property, defaults to "Default Application".
     */
    @Value("${application.name:Default Application}")
    private String applicationName;

    /**
     * Application description available to email templates for branding purposes.
     * Configured via {@code application.description} property.
     */
    @Value("${application.description:A low-code framework for SaaS products}")
    private String applicationDescription;

    /**
     * Reply-to email address for outgoing emails.
     * Configured via {@code mail.replyTo} property, empty by default.
     */
    @Value("${mail.replyTo:}")
    private String mailReplyTo;

    /**
     * Thymeleaf template engine for processing email templates into HTML content.
     * Injected by Spring framework.
     */
    @Inject
    private TemplateEngine templateEngine;

    /**
     * Modules interceptor for enriching email model with custom module-specific variables.
     * Called via {@link ModulesInterceptor#emailModelPreHandle(Map)} during content preparation.
     */
    @Inject
    ModulesInterceptor modulesInterceptor;

    /**
     * Spring application context for exposing beans to Thymeleaf via ThymeleafEvaluationContext.
     * Enables template access to Spring-managed components.
     */
    @Inject ApplicationContext context;
    
    /**
     * Creates a new Thymeleaf Context initialized with the current thread's locale.
     * <p>
     * This method retrieves the locale from {@link LocaleContextHolder} which is set by
     * Spring's LocaleChangeInterceptor based on the current HTTP request or default locale.
     * The Context is used as a variable container for template processing.

     *
     * @return Thymeleaf Context with current locale from LocaleContextHolder
     */
    private Context getContext() {
        debug("[getContext]");
        return new Context(LocaleContextHolder.getLocale());
    }

    /**
     * Prepares an Email entity by rendering a Thymeleaf template with provided variables and metadata.
     * <p>
     * This method orchestrates email content assembly by adding application name to the model,
     * calling {@link #prepareContent(String, Map)} to render HTML from the specified template,
     * populating Email fields with recipient details and content, computing the startAfter timestamp
     * as {@code LocalDateTime.now().plusSeconds(delayInSeconds)}, and mapping File attachment entities
     * to the Email's filesId list via {@link File#getId()}.

     * <p>
     * The generated Email entity is ready for persistence by EmailService and subsequent asynchronous
     * sending by EmailSenderJob. The nameFrom field is automatically set to the application name.

     *
     * @param emailTo recipient email address (e.g., "user@example.com")
     * @param nameTo recipient display name (e.g., "John Doe"), may be null
     * @param subject email subject line, displayed in recipient's inbox
     * @param templateName Thymeleaf template name without {@link URLConstants#EMAILRESOURCE_DISCRIMINATOR} suffix
     * @param delayInSeconds send delay in seconds, 0 for immediate sending
     * @param model template variables map, enriched with applicationName during processing
     * @param attachments optional File entities to attach, their IDs are mapped to Email.filesId list
     * @return Email entity populated with rendered HTML content, metadata, and attachment references
     * @see #prepareContent(String, Map)
     * @see com.openkoda.model.task.Email#setFilesId(java.util.List)
     */
    public Email prepareEmail(String emailTo, String nameTo, String subject, String templateName, int delayInSeconds,
                              Map<String, Object> model, File... attachments) {
        debug("[prepareEmail] {} {} {} {} {}", emailTo, nameTo, subject, templateName, delayInSeconds);
        model.put("applicationName", applicationName);
        String content = prepareContent( templateName, model );
        Email email = new Email();
        email.setEmailTo(emailTo);
        email.setContent(content);
        email.setSubject(subject);
        email.setNameTo(nameTo);
        email.setNameFrom(applicationName);
        email.setStartAfter(LocalDateTime.now().plusSeconds(delayInSeconds));
        if (attachments != null) {
            ArrayList<Long> fileIds = new ArrayList<>();
            for (File f: attachments) {
                fileIds.add(f.getId());
            }
            email.setFilesId(fileIds);
        }
        return email;
    }

    /**
     * Prepares an email with subject extracted from template's HTML {@code <title>} tag.
     * <p>
     * This variant creates a {@link PageModelMap} containing the provided {@link CanonicalObject}
     * under the {@link PageAttributes#canonicalObject} key, then delegates to the master variant
     * to render content and extract the subject from the HTML title element.

     *
     * @param emailTo recipient email address
     * @param templateName Thymeleaf template name
     * @param object canonical object to include in template model
     * @return Email entity with subject derived from template's {@code <title>} tag
     * @see #prepareEmailWithTitleFromTemplate(String, String, String, String, PageModelMap, File...)
     */
    public Email prepareEmailWithTitleFromTemplate(String emailTo, String templateName, CanonicalObject object) {
        debug("[prepareEmailTitleFromTemplate] {}", templateName);
        PageModelMap model = new PageModelMap();
        model.put(PageAttributes.canonicalObject, object);
        return prepareEmailWithTitleFromTemplate(emailTo, null, emailTo, templateName, model);
    }

    /**
     * Prepares an email for a User recipient with subject extracted from template's HTML {@code <title>} tag.
     * <p>
     * This variant creates a {@link PageModelMap} containing the User entity under the
     * {@link PageAttributes#userEntity} key, automatically using the user's email and name
     * from {@link User#getEmail()} and {@link User#getName()}.

     *
     * @param recipient User entity providing email address and display name
     * @param templateName Thymeleaf template name
     * @return Email entity addressed to the user with subject derived from template's {@code <title>} tag
     * @see #prepareEmailWithTitleFromTemplate(String, String, String, String, PageModelMap, File...)
     */
    public Email prepareEmailWithTitleFromTemplate(User recipient, String templateName) {
        debug("[prepareEmailTitleFromTemplate] {}", templateName);
        PageModelMap model = new PageModelMap();
        model.put(PageAttributes.userEntity, recipient);
        return prepareEmailWithTitleFromTemplate(recipient.getEmail(), null, recipient.getName(), templateName, model);
    }

    /**
     * Prepares an email for a User recipient with existing model enriched with user entity.
     * <p>
     * This variant adds the User entity to the provided {@link PageModelMap} under the
     * {@link PageAttributes#userEntity} key, allowing templates to access both custom variables
     * from the model and user-specific data. The subject is extracted from the template's {@code <title>} tag.

     *
     * @param recipient User entity providing email address and display name
     * @param templateName Thymeleaf template name
     * @param model existing PageModelMap with custom variables, will be enriched with user entity
     * @return Email entity addressed to the user with subject derived from template's {@code <title>} tag
     * @see #prepareEmailWithTitleFromTemplate(String, String, String, String, PageModelMap, File...)
     */
    public Email prepareEmailWithTitleFromTemplate(User recipient, String templateName, PageModelMap model) {
        debug("[prepareEmailTitleFromTemplate] {}", templateName);
        model.put(PageAttributes.userEntity, recipient);
        return prepareEmailWithTitleFromTemplate(recipient.getEmail(), null, recipient.getName(), templateName, model);
    }

    /**
     * Prepares an email for a User recipient with custom subject and model enriched with user entity.
     * <p>
     * This variant adds the User entity to the provided {@link PageModelMap} under the
     * {@link PageAttributes#userEntity} key and allows specifying a custom subject line.
     * If the subject is blank, it falls back to extracting from the template's {@code <title>} tag
     * or defaults to "System message".

     *
     * @param recipient User entity providing email address and display name
     * @param subject custom email subject, or null to extract from template
     * @param templateName Thymeleaf template name
     * @param model existing PageModelMap with custom variables, will be enriched with user entity
     * @return Email entity addressed to the user with specified or derived subject
     * @see #prepareEmailWithTitleFromTemplate(String, String, String, String, PageModelMap, File...)
     */
    public Email prepareEmailWithTitleFromTemplate(User recipient, String subject, String templateName, PageModelMap model) {
        debug("[prepareEmailTitleFromTemplate] {}", templateName);
        model.put(PageAttributes.userEntity, recipient);
        return prepareEmailWithTitleFromTemplate(recipient.getEmail(), subject, recipient.getName(), templateName, model);
    }

    /**
     * Master variant that prepares an email with subject extracted from template or provided explicitly.
     * <p>
     * This method calls {@link #prepareEmail(String, String, String, String, int, Map, File...)} with
     * zero delay and an empty initial subject, then derives the final subject via
     * {@link #getTitleFromHTML(String)} to extract content from the rendered HTML's {@code <title>} tag.
     * Falls back to the provided subject parameter if extraction yields blank, or defaults to
     * "System message" if both are blank.

     * <p>
     * This is the master implementation that all other {@code prepareEmailWithTitleFromTemplate} variants
     * delegate to after preparing their respective models.

     *
     * @param emailTo recipient email address
     * @param subject custom email subject, or null to extract from template's {@code <title>} tag
     * @param nameTo recipient display name
     * @param templateName Thymeleaf template name
     * @param model PageModelMap with template variables
     * @param attachments optional File entities to attach
     * @return Email entity with subject derived from template, provided parameter, or "System message" default
     * @see #prepareEmail(String, String, String, String, int, Map, File...)
     * @see #getTitleFromHTML(String)
     */
    public Email prepareEmailWithTitleFromTemplate(String emailTo, String subject, String nameTo, String templateName, PageModelMap model, File... attachments) {
        debug("[prepareEmailTitleFromTemplate] {}", templateName);
        Email email = this.prepareEmail(emailTo, nameTo, "", templateName, 0, model, attachments);        
        String title = StringUtils.defaultIfBlank(subject, StringUtils.defaultIfBlank(getTitleFromHTML(email.getContent()), "System message"));
        email.setSubject(title);
        return email;
    }

    /**
     * Extracts the content between {@code <title>} and {@code </title>} tags from HTML content.
     * <p>
     * Uses {@link StringUtils#substringBetween(String, String, String)} to parse the title element.
     * Returns null if the HTML does not contain a title tag or if the content is malformed.

     *
     * @param content HTML string potentially containing a {@code <title>} element
     * @return extracted title text, or null if title tag not found
     */
    private String getTitleFromHTML(String content) {
        debug("[getTitleFromHTML]");
        return StringUtils.substringBetween(content, "<title>", "</title>");
    }

    /**
     * Renders a Thymeleaf email template into HTML content with provided variables and application context.
     * <p>
     * This method creates a Thymeleaf {@link Context} initialized with {@link LocaleContextHolder#getLocale()},
     * then forces the locale to {@link Locale#ENGLISH} for deterministic email rendering regardless of
     * user locale. It sets baseUrl and mailReplyTo variables, calls
     * {@link ModulesInterceptor#emailModelPreHandle(Map)} for module-specific model enrichment,
     * registers {@link ThymeleafEvaluationContext} to expose Spring beans to templates, adds all
     * model entries to the context, and processes the template with
     * {@link URLConstants#EMAILRESOURCE_DISCRIMINATOR} suffix appended to the template name.

     * <p>
     * The rendered HTML is suitable for email body content and may contain HTML title, styling, and
     * dynamic content generated from the model variables.

     *
     * @param templateName Thymeleaf template name without {@link URLConstants#EMAILRESOURCE_DISCRIMINATOR} suffix
     * @param model map of variables available to the template during rendering
     * @return rendered HTML string ready for inclusion in Email entity content field
     * @see TemplateEngine#process(String, org.thymeleaf.context.IContext)
     * @see ModulesInterceptor#emailModelPreHandle(Map)
     */
    public String prepareContent(String templateName, Map<String, Object> model) {
        debug("[prepareContent] {}", templateName);
        final Context ctx = getContext();

        ctx.setLocale(Locale.ENGLISH);
        ctx.setVariable("baseUrl", baseUrl);
        ctx.setVariable("mailReplyTo", mailReplyTo);

        modulesInterceptor.emailModelPreHandle(model);
        ctx.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
                new ThymeleafEvaluationContext(context, null));

        for (Map.Entry<String, Object> entry : model.entrySet()) {
            ctx.setVariable(entry.getKey(), entry.getValue());
        }

        return templateEngine.process(templateName + URLConstants.EMAILRESOURCE_DISCRIMINATOR, ctx);
    }

}
