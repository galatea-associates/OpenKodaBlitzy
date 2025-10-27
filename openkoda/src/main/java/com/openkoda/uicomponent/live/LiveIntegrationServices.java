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

package com.openkoda.uicomponent.live;

import com.openkoda.core.service.RestClientService;
import com.openkoda.core.service.SlackService;
import com.openkoda.uicomponent.IntegrationServices;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Live implementation of {@link IntegrationServices} providing integration capabilities for UI components.
 * <p>
 * This service acts as a facade for external integration operations including Slack messaging and REST API
 * client functionality. It delegates all operations to specialized services ({@link SlackService} and
 * {@link RestClientService}) while providing a unified interface for UI component access.
 * </p>
 * <p>
 * The service supports multiple Slack messaging patterns including simple messages, channel-specific messages,
 * and JSON-formatted messages. REST operations support both GET and POST methods with configurable headers.
 * Methods return {@code false} or empty results on HTTP errors rather than throwing exceptions.
 * </p>
 * <p>
 * This class is stateless and thread-safe as it delegates to thread-safe Spring-managed service beans.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see IntegrationServices
 * @see SlackService
 * @see RestClientService
 */
@Component
public class LiveIntegrationServices implements IntegrationServices {

    /**
     * Slack integration service delegate for sending messages to Slack channels.
     * Provides methods for simple text messages, channel-specific messages, and JSON-formatted messages.
     */
    @Inject
    SlackService slackService;

    /**
     * REST client service delegate for HTTP operations.
     * Provides GET and POST methods with support for custom headers and request bodies.
     */
    @Inject
    RestClientService restClientService;

    /**
     * Sends a simple text message to Slack using the specified webhook URL.
     * <p>
     * This is a convenience method for basic Slack messaging without specifying channel or username.
     * The message is posted to the default channel configured in the webhook.
     * </p>
     *
     * @param message the text message to send to Slack
     * @param webHook the Slack webhook URL to post the message to
     * @return {@code true} if the message was sent successfully, {@code false} on HTTP errors
     */
    public boolean sendMessageToSlack(String message, String webHook) {
        return slackService.sendMessageToSlack(message, webHook);
    }
    
    /**
     * Sends a text message to Slack with custom channel and username configuration.
     * <p>
     * This method allows posting messages to specific channels with a custom display username,
     * overriding the webhook's default settings.
     * </p>
     *
     * @param message the text message to send to Slack
     * @param webHook the Slack webhook URL to post the message to
     * @param channel the target Slack channel (e.g., "#general")
     * @param username the display name for the message sender
     * @return {@code true} if the message was sent successfully, {@code false} on HTTP errors
     */
    public boolean sendMessageToSlack(String message, String webHook, String channel, String username) {
        return slackService.sendMessageToSlack(message, webHook, channel, username);
    }

    /**
     * Sends a JSON-formatted message to Slack using the specified webhook URL.
     * <p>
     * This method accepts a pre-formatted JSON message string allowing full control over the Slack
     * message structure including attachments, blocks, and formatting options. The JSON must conform
     * to Slack's message payload format.
     * </p>
     *
     * @param JSONMessage the JSON-formatted message string conforming to Slack's payload format
     * @param webHook the Slack webhook URL to post the message to
     * @return {@code true} if the message was sent successfully, {@code false} on HTTP errors
     * @see SlackService#sendJSONMessageToSlack(String, String)
     */
    @Override
    public boolean sendJsonMessageToSlack(String JSONMessage, String webHook) {
        return slackService.sendJSONMessageToSlack(JSONMessage, webHook);
    }

    /**
     * Executes an HTTP POST request to the specified URL with custom body and headers.
     * <p>
     * This method performs a REST POST operation with configurable request body and HTTP headers.
     * Returns an empty map on HTTP errors rather than throwing exceptions.
     * </p>
     *
     * @param url the target endpoint URL for the POST request
     * @param body the request body as a map of key-value pairs
     * @param headers the HTTP headers as a map of header names to values
     * @return a map containing the response data, or empty map on HTTP errors
     * @see RestClientService#post(String, Map, Map)
     */
    @Override
    public Map restPost(String url, Map<String, String> body, Map<String, String> headers) {
        return restClientService.post(url, body, headers);
    }

    /**
     * Executes an HTTP GET request to the specified URL with custom headers.
     * <p>
     * This method performs a REST GET operation with configurable HTTP headers.
     * Returns an empty map on HTTP errors rather than throwing exceptions.
     * </p>
     *
     * @param url the target endpoint URL for the GET request
     * @param headers the HTTP headers as a map of header names to values
     * @return a map containing the response data, or empty map on HTTP errors
     * @see RestClientService#get(String, Map)
     */
    @Override
    public Map restGet(String url, Map<String, String> headers) {
        return restClientService.get(url, headers);
    }
}
