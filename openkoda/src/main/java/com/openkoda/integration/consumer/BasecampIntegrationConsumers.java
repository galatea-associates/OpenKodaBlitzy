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

package com.openkoda.integration.consumer;

import com.openkoda.dto.NotificationDto;
import com.openkoda.integration.controller.IntegrationComponentProvider;
import com.openkoda.integration.model.configuration.IntegrationModuleOrganizationConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Basecamp REST API consumer for creating todos in Basecamp projects.
 * <p>
 * Implements notification-to-external-artifact conversion for Basecamp integration. This service
 * authenticates using OAuth access tokens from per-organization configuration and constructs JSON
 * payloads for Basecamp API endpoints via synchronous RestTemplate HTTP POST requests.
 * </p>
 * <p>
 * Key features include:
 * <ul>
 *   <li>OAuth access token authentication from per-organization configuration</li>
 *   <li>JSON payload construction for Basecamp API endpoints</li>
 *   <li>RestTemplate synchronous HTTP POST requests</li>
 *   <li>Automatic token refresh on 401 Unauthorized responses via integrationService.refreshBasecampToken()</li>
 *   <li>API endpoint template: https://3.basecampapi.com/{accountId}/buckets/{projectId}/todolists/{todolistId}/todos.json</li>
 * </ul>
 * </p>
 * <p>
 * Architecture notes: Stateless Spring {@code @Service} component that verifies notification scope
 * (services.notification.isOrganization), respects propagate flag, and loads per-organization configuration
 * via integrationService.getOrganizationConfiguration().
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * NotificationDto notification = ...;
 * basecampConsumer.postBasecampToDo(notification);
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see IntegrationComponentProvider
 * @see NotificationDto
 */
@Service
public class BasecampIntegrationConsumers extends IntegrationComponentProvider {

    /**
     * Basecamp API endpoint URL template for creating todos.
     * <p>
     * Template format: https://3.basecampapi.com/{accountId}/buckets/{projectId}/todolists/{todolistId}/todos.json
     * </p>
     * <p>
     * Placeholders:
     * <ul>
     *   <li>%s (first) - Basecamp account ID</li>
     *   <li>%s (second) - Project ID (bucket)</li>
     *   <li>%s (third) - Todo list ID</li>
     * </ul>
     * </p>
     * Configured via application property: {@code api.basecamp.post.message}
     */
    @Value("${api.basecamp.post.message:https://3.basecampapi.com/%s/buckets/%s/todolists/%s/todos.json}")
    public String BASECAMP_POST_TODO_URL;

    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Creates a Basecamp todo from an organization notification with automatic token refresh on 401 Unauthorized.
     * <p>
     * This method converts an OpenKoda notification into a Basecamp todo item by performing the following workflow:
     * <ol>
     *   <li>Scope verification - ensures notification is organization-level via services.notification.isOrganization()</li>
     *   <li>Propagate flag check - exits early if notification.getPropagate() is false</li>
     *   <li>Configuration loading - retrieves per-organization Basecamp configuration (access token, account ID, project ID, todo list ID)</li>
     *   <li>URL preparation - constructs Basecamp API endpoint from configuration via prepareBasecampToDoUrl()</li>
     *   <li>JSON payload construction - creates todo JSON with content and description via prepareToDoData()</li>
     *   <li>HTTP POST - executes RestTemplate POST request with Bearer token authentication</li>
     *   <li>Error handling - delegates to handleBasecampError() with automatic token refresh and single retry on 401 Unauthorized</li>
     * </ol>
     * </p>
     * <p>
     * Authentication: Uses OAuth access token from IntegrationModuleOrganizationConfiguration.getBasecampAccessToken().
     * If the token is missing or empty, the method logs a warning and returns without posting.
     * </p>
     * <p>
     * Token refresh: On 401 Unauthorized response, automatically calls integrationService.refreshBasecampToken()
     * and retries the request once via recursive call to postBasecampToDo().
     * </p>
     *
     * @param notification the NotificationDto containing the message to convert to a Basecamp todo. Must be organization-level
     *                     with propagate flag set to true. The notification message is used for the todo description.
     * @throws Exception on API errors, RestClientException, or if token refresh fails. Specific errors are logged
     *                   via integrationService.handleResponseError()
     * @see #prepareBasecampToDoUrl(IntegrationModuleOrganizationConfiguration)
     * @see #prepareToDoData(String)
     * @see #prepareHeaders(String)
     * @see #handleBasecampError(ResponseEntity, NotificationDto)
     */
    public void postBasecampToDo(NotificationDto notification) throws Exception {
        debug("[postBasecampToDo]");
        if (!services.notification.isOrganization(notification)) {
            info("[postBasecampToDo] Notification is not organization level.");
            return;
        }
        if(!notification.getPropagate()){
            return;
        }
        IntegrationModuleOrganizationConfiguration organizationConfig
                = integrationService.getOrganizationConfiguration(notification.getOrganizationId());
        if (StringUtils.isEmpty(organizationConfig.getBasecampAccessToken())) {
            warn("[postBasecampToDo] Missing Access Token.");
            return;
        }
        String requestUrl = prepareBasecampToDoUrl(organizationConfig);
        String toDoRequest = prepareToDoData(notification.getMessage());
        HttpEntity<String> entity = new HttpEntity<>(toDoRequest, prepareHeaders(organizationConfig.getBasecampAccessToken()));
        ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.POST, entity, String.class);
        handleBasecampError(response, notification);
    }

    /**
     * Prepares the Basecamp API endpoint URL for creating a todo from organization configuration.
     * <p>
     * Constructs the full Basecamp API URL by formatting the BASECAMP_POST_TODO_URL template with
     * the account ID, project ID (bucket), and todo list ID from the organization's Basecamp configuration.
     * </p>
     * <p>
     * Required configuration parameters from organizationConfig:
     * <ul>
     *   <li>accountId - Basecamp account identifier (from getBasecampAccountId())</li>
     *   <li>projectId - Basecamp project/bucket identifier (from getBasecampProjectId())</li>
     *   <li>toDoListId - Basecamp todo list identifier (from getBasecampToDoListId())</li>
     * </ul>
     * All three parameters must be non-blank, otherwise the method logs an error and returns null.
     * </p>
     *
     * @param organizationConfig the IntegrationModuleOrganizationConfiguration containing Basecamp account settings.
     *                           Must have non-blank accountId, projectId, and toDoListId.
     * @return the formatted Basecamp API URL string in the format
     *         https://3.basecampapi.com/{accountId}/buckets/{projectId}/todolists/{todolistId}/todos.json,
     *         or null if any required configuration parameter is blank
     */
    private String prepareBasecampToDoUrl(IntegrationModuleOrganizationConfiguration organizationConfig) {
        debug("[prepareBasecampToDoUrl]");
        String accountId = organizationConfig.getBasecampAccountId();
        String projectId = organizationConfig.getBasecampProjectId();
        String toDoListId = organizationConfig.getBasecampToDoListId();
        if (StringUtils.isBlank(accountId) || StringUtils.isBlank(projectId) || StringUtils.isBlank(toDoListId)) {
            error("[prepareBasecampToDoUrl] Basecamp accountId, projectId or toDoListId not set");
            return null;
        }
        return String.format(BASECAMP_POST_TODO_URL, accountId, projectId, toDoListId);
    }

    /**
     * Constructs the JSON payload for creating a Basecamp todo with content and description fields.
     * <p>
     * JSON structure:
     * <pre>{@code
     * {
     *   "content": "notification.basecamp.subject",
     *   "description": "message with escaped quotes"
     * }
     * }</pre>
     * </p>
     * <p>
     * The content field is populated from the internationalized message key "notification.basecamp.subject"
     * via the messages service. The description field contains the provided message with double quotes
     * escaped to \" for JSON compatibility using StringUtils.replace().
     * </p>
     *
     * @param message the notification message to include in the todo description. Double quotes in the message
     *                are automatically escaped to prevent JSON parsing errors.
     * @return a JSON string representing the todo data in Basecamp API format with content and description fields
     */
    private String prepareToDoData(String message) {
        debug("[prepareToDoData]");
        String messageJson = "{" +
                "\"content\":\"%s\"," +
                "\"description\":\"%s\"" +
                "}";
        return String.format(messageJson, messages.get(
                "notification.basecamp.subject"),
                StringUtils.replace(message, "\"", "\\\"")
        );
    }

    /**
     * Prepares HTTP headers for Basecamp API requests with OAuth Bearer token authentication.
     * <p>
     * Configured headers:
     * <ul>
     *   <li>Authorization: Bearer {accessToken} - OAuth 2.0 access token for authentication</li>
     *   <li>Content-Type: application/json - indicates JSON payload format</li>
     * </ul>
     * </p>
     * <p>
     * The Authorization header uses the Bearer token scheme as required by the Basecamp API OAuth implementation.
     * </p>
     *
     * @param accessToken the OAuth access token from organization configuration (IntegrationModuleOrganizationConfiguration.getBasecampAccessToken()).
     *                    Must be non-empty to authenticate successfully.
     * @return configured HttpHeaders with Authorization Bearer token and Content-Type application/json
     */
    private HttpHeaders prepareHeaders(String accessToken) {
        debug("[prepareHeaders]");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    /**
     * Handles Basecamp API error responses with automatic token refresh and retry on 401 Unauthorized.
     * <p>
     * Error handling workflow:
     * <ol>
     *   <li>401 Unauthorized: Calls integrationService.refreshBasecampToken() to obtain a new access token.
     *       If token refresh succeeds, recursively calls postBasecampToDo() for a single retry attempt.</li>
     *   <li>Other errors: Delegates to integrationService.handleResponseError() for logging and error propagation
     *       with the message template "[postBasecampToDo] Error while posting message. Code: {}. Error: {}"</li>
     * </ol>
     * </p>
     * <p>
     * Note: Token refresh and retry occurs only once. If the retry also fails, the error is propagated
     * via handleResponseError() without further retry attempts.
     * </p>
     *
     * @param response the ResponseEntity from the Basecamp API request containing HTTP status code and response body
     * @param notification the original NotificationDto used for the retry attempt if token refresh succeeds
     * @throws Exception on API errors after token refresh fails, or for non-401 HTTP error responses.
     *                   Errors are logged via integrationService.handleResponseError()
     * @see com.openkoda.integration.service.IntegrationService#refreshBasecampToken(Long)
     * @see com.openkoda.integration.service.IntegrationService#handleResponseError(ResponseEntity, String)
     */
    private void handleBasecampError(ResponseEntity response, NotificationDto notification) throws Exception {
        debug("[handleBasecampError]");
        if (response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            if (integrationService.refreshBasecampToken(notification.getOrganizationId())) {
                postBasecampToDo(notification);
            }
        } else {
            integrationService.handleResponseError(response, "[postBasecampToDo] Error while posting message. Code: {}. Error: {}");
        }
    }
}
