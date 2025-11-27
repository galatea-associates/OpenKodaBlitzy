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
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Slack webhook integration service for rendering and delivering notification messages.
 * <p>
 * Provides message delivery to Slack via webhook URLs with two operational modes:
 * asynchronous delivery via HttpRequestTask queue (default) and synchronous posting 
 * via RestTemplate. Renders message payloads using Thymeleaf templates with support 
 * for CanonicalObject entities.

 * <p>
 * Constructs JSON payloads conforming to Slack webhook API specification with text, 
 * channel, and username fields. Escapes double quotes in message content for JSON 
 * safety. Asynchronous messages are enqueued to unsecure repository for background 
 * job delivery without privilege checks.

 * <p>
 * Example usage:
 * <pre>
 * slackService.sendToSlackWithCanonical(entity, "slack/notification-template", 
 *     webhookUrl, "#general", "BotName");
 * </pre>

 *
 * @see HttpRequestTask
 * @see CanonicalObject
 * @see RestTemplate
 * @see TemplateEngine
 * @see PageModelMap
 * @see ComponentProvider
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Service
public class SlackService extends ComponentProvider {

    /**
     * Thymeleaf template engine for rendering message content from templates.
     */
    @Inject
    private TemplateEngine templateEngine;

    /**
     * RestTemplate instance for synchronous HTTP POST operations.
     * Initialized in {@link #init()} lifecycle callback.
     */
    private RestTemplate restTemplate;

    /**
     * Initializes RestTemplate instance for synchronous HTTP operations.
     * <p>
     * Lifecycle callback invoked after dependency injection completes.
     * Creates new RestTemplate without custom configuration or interceptors.

     */
    @PostConstruct
    private void init() {
        debug("[init] Preparing RestTemplate object with headers");
        restTemplate = new RestTemplate();
    }

    /**
     * Sends Slack message for CanonicalObject entity using default channel and username.
     * <p>
     * Convenience overload delegating to five-parameter variant with null channel 
     * and username. Renders message from Thymeleaf template with canonicalObject 
     * as model attribute.

     *
     * @param object CanonicalObject entity to include in template model
     * @param templateName Thymeleaf template name for message rendering
     * @param webHook Slack webhook URL for message delivery
     * @return true always (actual delivery is asynchronous)
     */
    public boolean sendToSlackWithCanonical(CanonicalObject object, String templateName, String webHook) {
        return sendToSlackWithCanonical(object, templateName, webHook, null, null);
    }

    /**
     * Sends Slack message for CanonicalObject entity with optional channel and username.
     * <p>
     * Renders message from Thymeleaf template with canonicalObject attribute in model.
     * Template receives PageModelMap containing the entity for property access.
     * Delegates to sendMessageToSlack for asynchronous delivery via HttpRequestTask queue.
     * Returns immediately without waiting for HTTP response.

     *
     * @param object CanonicalObject entity to include in template model
     * @param templateName Thymeleaf template name for message rendering
     * @param webHook Slack webhook URL for message delivery
     * @param channel optional Slack channel override (e.g., "#general"), null for default
     * @param username optional bot username override, null for default
     * @return true always (actual delivery is asynchronous)
     */
    public boolean sendToSlackWithCanonical(CanonicalObject object, String templateName, String webHook, String channel, String username) {
        debug("[sendToSlackWithCanonical]");
        PageModelMap model = new PageModelMap();
        model.put(PageAttributes.canonicalObject, object);
        String message = prepareContent(templateName, model);
        sendMessageToSlack(message, webHook, channel, username);
        return true;
    }
    
    /**
     * Sends text message to Slack webhook using default channel and username.
     * <p>
     * Convenience overload delegating to four-parameter variant with null channel 
     * and username. Message text is escaped and formatted as JSON payload.

     *
     * @param message text content for Slack message
     * @param webHook Slack webhook URL for message delivery
     * @return true always (actual delivery is asynchronous)
     */
    public boolean sendMessageToSlack(String message, String webHook) {
        return sendMessageToSlack(message, webHook, null, null);
    }
    
    /**
     * Sends text message to Slack webhook with optional channel and username overrides.
     * <p>
     * Constructs Slack JSON payload using String.format, escaping double quotes in 
     * message text with backslash. Optional channel and username fields are included 
     * only if non-null. Delegates to sendJSONMessageToSlack for asynchronous enqueue.

     * <p>
     * <b>Warning:</b> Message text is escaped for JSON but not validated. 
     * Ensure user input is sanitized to prevent injection attacks.

     *
     * @param message text content for Slack message
     * @param webHook Slack webhook URL for message delivery
     * @param channel optional Slack channel override (e.g., "#general"), null to omit
     * @param username optional bot username override, null to omit
     * @return true always (actual delivery is asynchronous)
     */
    public boolean sendMessageToSlack(String message, String webHook, String channel, String username) {
        debug("[sendMessageToSlack] Message to {}", webHook);
        String requestJson = String.format("{\"text\":\"%s\"%s%s}",
                StringUtils.replace(message, "\"", "\\\""),
                StringUtils.defaultIfBlank(channel != null ? String.format(", \"channel\": \"%s\"", channel) : null, ""),
                StringUtils.defaultIfBlank(username != null ? String.format(", \"username\": \"%s\"", username) : null, ""));
        return sendJSONMessageToSlack(requestJson, webHook);

    }

    /**
     * Enqueues pre-formatted JSON payload for asynchronous Slack delivery.
     * <p>
     * Creates HttpRequestTask with webhook URL and JSON message body, persisting 
     * via unsecure repository for background job delivery. Returns immediately 
     * without waiting for HTTP response. Actual HTTP POST occurs asynchronously 
     * via scheduled job processing HttpRequestTask queue.

     * <p>
     * <b>Note:</b> Uses unsecure repository - delivery not subject to privilege checks.
     * Suitable for system-level notifications.

     *
     * @param requestJson formatted JSON payload conforming to Slack webhook API
     * @param webHook Slack webhook URL for message delivery
     * @return true always (enqueue successful, not HTTP response)
     */
    public boolean sendJSONMessageToSlack(String requestJson, String webHook) {
        debug("[sendMessageToSlack] Message to {}", webHook);
        HttpRequestTask httpRequestTask = new HttpRequestTask(webHook, requestJson);
        repositories.unsecure.httpRequest.save(httpRequestTask);
        return true;

    }

    /**
     * Posts message to Slack webhook synchronously using RestTemplate.
     * <p>
     * Constructs simple JSON payload with text field, escaping double quotes. 
     * Performs immediate HTTP POST using RestTemplate.postForEntity, blocking 
     * thread until Slack responds or timeout occurs. Validates webhook URL 
     * not blank before posting.

     * <p>
     * <b>Note:</b> Synchronous method blocks calling thread. Prefer asynchronous 
     * sendMessageToSlack methods to avoid blocking request threads.

     *
     * @param message text content for Slack message
     * @param webHook Slack webhook URL for message delivery
     * @return true always (even if webhook blank or HTTP fails)
     */
    public boolean postMessageToSlackWebhook(String message, String webHook) {
        debug("[postMessageToSlackWebhook]");
        String requestJson = String.format("{\"text\":\"%s\"}",
                StringUtils.replace(message, "\"", "\\\""));
        if(StringUtils.isNotBlank(webHook)) {
            HttpHeaders httpHeaders = new HttpHeaders();
            HttpEntity<String> httpEntity = new HttpEntity<>(requestJson, httpHeaders);
            restTemplate.postForEntity(webHook, httpEntity, String.class);
        }
        return true;
    }

    /**
     * Renders Thymeleaf template with model variables for message content generation.
     * <p>
     * Creates Context with current locale from LocaleContextHolder, populates model 
     * entries as template variables, processes template to string. Enables dynamic 
     * message content with entity property access and conditional logic in templates.

     *
     * @param templateName Thymeleaf template name for rendering
     * @param model template variables map for property access
     * @return rendered message content as string
     */
    public String prepareContent(String templateName, Map<String, Object> model) {
        debug("[prepareContent] {}", templateName);
        final Context ctx = new Context(LocaleContextHolder.getLocale());

        for (Map.Entry<String, Object> entry : model.entrySet()) {
            ctx.setVariable(entry.getKey(), entry.getValue());
        }

        return templateEngine.process(templateName, ctx);
    }
}
