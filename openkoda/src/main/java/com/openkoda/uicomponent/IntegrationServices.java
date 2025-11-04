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

package com.openkoda.uicomponent;

import com.openkoda.uicomponent.annotation.Autocomplete;

import java.util.Map;

/**
 * Service contract for third-party system integrations including Slack messaging and generic REST client operations.
 * <p>
 * This interface provides a unified API for integrating with external services from UI flows and automation scripts.
 * It offers two primary integration capabilities:
 * 
 * <ul>
 *   <li>Slack webhook integration with text and JSON message support for notifications and alerts</li>
 *   <li>Generic REST client methods (POST/GET) with flexible Map-based bodies and headers for custom API integrations</li>
 * </ul>
 * <p>
 * All methods are annotated with {@code @Autocomplete} to provide UI tooling metadata for visual development environments.
 * Implementations handle HTTP communication, error handling, retries, and timeouts according to service-specific
 * configuration. This interface is suitable for UI flows requiring external service communication without direct
 * HTTP client management.
 * 
 * <p>
 * Typical use cases include:
 * 
 * <ul>
 *   <li>Sending deployment notifications to Slack channels</li>
 *   <li>Triggering webhooks for CI/CD pipeline integration</li>
 *   <li>Querying external APIs for data enrichment</li>
 *   <li>Posting events to third-party monitoring systems</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.uicomponent.live.LiveIntegrationServices
 */
public interface IntegrationServices {
    
    /**
     * Sends plain text message to Slack via webhook URL.
     * <p>
     * Delegates to SlackService.sendMessageToSlack(message, webHook). Constructs Slack-formatted JSON payload
     * and sends HTTP POST to the webhook URL. Uses default channel and username from webhook configuration.
     * This is the simplest method for sending text notifications to Slack.
     * 
     * <p>
     * Example usage:
     * <pre>
     * sendMessageToSlack("Deployment complete", "https://hooks.slack.com/services/XXX/YYY/ZZZ");
     * </pre>
     * 
     * <p>
     * Note: The {@code @Autocomplete} annotation provides 'Send a message to Slack webhook' hint for UI tooling.
     * 
     *
     * @param message plain text message content to send to Slack
     * @param webHook Slack webhook URL (e.g., "https://hooks.slack.com/services/...")
     * @return true if message sent successfully, false on HTTP error or network failure
     */
    @Autocomplete(doc="Send a message to Slack webhook")
    boolean sendMessageToSlack(String message, String webHook);
    
    /**
     * Sends plain text message to Slack with explicit channel and username override.
     * <p>
     * Constructs Slack JSON payload with channel and username fields, sends HTTP POST to webhook URL.
     * Allows overriding default webhook channel and username configuration for targeted message routing.
     * Channel must start with '#' for public channels or '@' for direct messages.
     * 
     * <p>
     * Example usage:
     * <pre>
     * sendMessageToSlack("Alert!", webHook, "#alerts", "MonitorBot");
     * </pre>
     * 
     *
     * @param message plain text message content to send to Slack
     * @param webHook Slack webhook URL
     * @param channel target channel name (e.g., "#deployments", "@john")
     * @param username display username for message sender
     * @return true if message sent successfully, false on HTTP error or network failure
     */
    @Autocomplete(doc="Send a message to Slack webhook to specific channel using specific username")
    boolean sendMessageToSlack(String message, String webHook, String channel, String username);
    
    /**
     * Sends pre-formatted JSON message to Slack webhook for advanced formatting.
     * <p>
     * Sends JSONMessage as-is to webhook without modification. Allows full control of Slack message format
     * including blocks, attachments, markdown, colors, and other advanced Slack message features.
     * Caller is responsible for ensuring valid Slack JSON structure conforming to the Slack message payload schema.
     * 
     * <p>
     * Example usage:
     * <pre>
     * sendJsonMessageToSlack("{\"text\":\"*Bold* message\",\"channel\":\"#general\"}", webHook);
     * </pre>
     * 
     * <p>
     * Note: JSON must conform to Slack message payload schema - invalid JSON returns false.
     * 
     *
     * @param JSONMessage Slack-formatted JSON string with blocks, attachments, or text fields
     * @param webHook Slack webhook URL
     * @return true if message sent successfully, false on HTTP error, network failure, or invalid JSON
     */
    @Autocomplete(doc="Send a JSON-formatted message to Slack webhook")
    boolean sendJsonMessageToSlack(String JSONMessage, String webHook);

    /**
     * Executes HTTP POST request with Map body and headers, returns response as Map.
     * <p>
     * Delegates to RestClientService.restPost. Serializes body Map to JSON, adds headers, sends HTTP POST,
     * and deserializes JSON response to Map. Suitable for RESTful API calls from UI flows without direct
     * HTTP client management. Response Map contains implementation-specific structure typically including
     * status code, response headers, and response body.
     * 
     * <p>
     * Example usage:
     * <pre>
     * Map body = Map.of("name", "my-repo");
     * Map headers = Map.of("Authorization", "token XXX");
     * Map response = restPost("https://api.github.com/repos", body, headers);
     * </pre>
     * 
     * <p>
     * Note: The {@code @Autocomplete} annotation provides 'Rest client POST' hint for UI tooling.
     * Error handling: Returns error Map on HTTP errors - check response status field.
     * 
     *
     * @param url target URL for POST request (e.g., "https://api.example.com/endpoint")
     * @param body request body as String-to-String Map (serialized to JSON)
     * @param headers HTTP headers as String-to-String Map (e.g., {"Content-Type": "application/json"})
     * @return response Map containing status, headers, body (implementation-specific structure)
     */
    @Autocomplete(doc="Rest client POST")
    Map restPost(String url, Map<String, String> body, Map<String, String> headers);
    
    /**
     * Executes HTTP GET request with headers, returns response as Map.
     * <p>
     * Delegates to RestClientService.restGet. Adds headers, sends HTTP GET, and deserializes JSON response
     * to Map. Suitable for querying external APIs from UI flows without direct HTTP client management.
     * Response Map contains implementation-specific structure typically including status code, response headers,
     * and response body. No request body parameter - use restPost for requests with body payload.
     * 
     * <p>
     * Example usage:
     * <pre>
     * Map headers = Map.of("Accept", "application/json");
     * Map response = restGet("https://api.example.com/data", headers);
     * </pre>
     * 
     * <p>
     * Note: Error handling returns error Map on HTTP errors - check response status field.
     * 
     *
     * @param url target URL for GET request
     * @param headers HTTP headers as String-to-String Map
     * @return response Map containing status, headers, body (implementation-specific structure)
     */
    @Autocomplete(doc="Rest client GET")
    Map restGet(String url, Map<String, String> headers);
}
