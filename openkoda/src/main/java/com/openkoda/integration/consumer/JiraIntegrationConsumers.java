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

import com.fasterxml.jackson.databind.JsonNode;
import com.openkoda.dto.NotificationDto;
import com.openkoda.integration.controller.IntegrationComponentProvider;
import com.openkoda.integration.model.configuration.IntegrationModuleGlobalConfiguration;
import com.openkoda.integration.model.configuration.IntegrationModuleOrganizationConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Jira REST API consumer for creating issues and managing workflows in Jira projects.
 * Supports both Jira Cloud and Jira Server deployments via OAuth 2.0 authentication.
 * <p>
 * This service manages the complete OAuth 2.0 authentication flow with automatic token refresh:
 * 
 * <ul>
 *   <li>Initial authentication uses authorization code exchange to obtain access and refresh tokens</li>
 *   <li>Access tokens are automatically refreshed on 401 responses using stored refresh tokens</li>
 *   <li>Updated tokens are persisted to {@link IntegrationModuleOrganizationConfiguration}</li>
 *   <li>Jira Cloud ID discovery via accessible-resources API endpoint</li>
 * </ul>
 * <p>
 * <b>OAuth Flow:</b>
 * 
 * <ol>
 *   <li>User authorizes application via Jira OAuth consent screen</li>
 *   <li>OAuth callback receives authorization code</li>
 *   <li>{@code integrationService.getJiraToken(code)} exchanges code for access token, refresh token, and cloud ID</li>
 *   <li>Tokens are stored in {@code IntegrationModuleOrganizationConfiguration} entity</li>
 *   <li>On 401 response, refresh flow: POST to token endpoint with refresh token, obtain new access token, update entity via {@code repositories.unsecure.integration.save()}</li>
 * </ol>
 * <p>
 * <b>Issue Creation:</b> Issues are created via Jira REST API v2 using POST /rest/api/2/issue
 * with project ID, issue type ID, summary, and description fields.
 * 
 * <p>
 * Example usage:
 * 
 * <pre>
 * NotificationDto notification = ...;
 * jiraConsumers.createJiraIssueFromOrgNotification(notification);
 * </pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see IntegrationComponentProvider
 * @see IntegrationModuleOrganizationConfiguration
 * @see NotificationDto
 */
@Service
public class JiraIntegrationConsumers extends IntegrationComponentProvider {

    /**
     * JSON template for Jira issue creation request payload.
     * Format parameters: summary, description, issue type ID, project ID.
     * Source: com/openkoda/integration/consumer/JiraIntegrationConsumers.java:38
     */
    private static final String JIRA_CREATE_ISSUE_REQUEST_JSON = "{\"fields\": {" +
            "    \"summary\": \"%s\"," +
            "    \"description\": \"%s\"," +
            "    \"issuetype\": {" +
            "      \"id\": \"%s\"" +
            "    }," +
            "    \"project\": {" +
            "      \"id\": \"%s\"" +
            "    }" +
            "  }" +
            "}";
    
    /**
     * Jira OAuth token refresh endpoint URL.
     * Default: https://auth.atlassian.com/oauth/token
     * Configurable via application property: api.jira.refresh.token
     */
    @Value("${api.jira.refresh.token:https://auth.atlassian.com/oauth/token}")
    private String JIRA_REFRESH_TOKEN_API;
    
    /**
     * Jira accessible resources endpoint URL for cloud ID discovery.
     * Default: https://api.atlassian.com/oauth/token/accessible-resources
     * Configurable via application property: api.jira.get.cloudId
     */
    @Value("${api.jira.get.cloudId:https://api.atlassian.com/oauth/token/accessible-resources}")
    private String JIRA_GET_CLOUDID_API;
    
    /**
     * Jira project list endpoint URL template (requires cloud ID parameter).
     * Default: https://api.atlassian.com/ex/jira/%s/rest/api/2/project
     * Configurable via application property: api.jira.get.project.list
     */
    @Value("${api.jira.get.project.list:https://api.atlassian.com/ex/jira/%s/rest/api/2/project}")
    private String JIRA_GET_PROJECT_LIST_API;
    
    /**
     * Jira issue types endpoint URL template (requires cloud ID parameter).
     * Default: https://api.atlassian.com/ex/jira/%s/rest/api/2/issuetype
     * Configurable via application property: api.jira.get.issue.type.list
     */
    @Value("${api.jira.get.issue.type.list:https://api.atlassian.com/ex/jira/%s/rest/api/2/issuetype}")
    private String JIRA_GET_ISSUE_TYPES_API;
    
    /**
     * Jira issue creation endpoint URL template (requires cloud ID parameter).
     * Default: https://api.atlassian.com/ex/jira/%s/rest/api/2/issue
     * Configurable via application property: api.jira.create.issue
     */
    @Value("${api.jira.create.issue:https://api.atlassian.com/ex/jira/%s/rest/api/2/issue}")
    private String JIRA_CREATE_ISSUE_API;
    
    /**
     * JSON template for OAuth token refresh request payload.
     * Format parameters: client ID, client secret, refresh token.
     * Source: com/openkoda/integration/consumer/JiraIntegrationConsumers.java:59
     */
    private static final String JIRA_REFRESH_TOKEN_JSON = "{" +
            "\"grant_type\":\"refresh_token\"," +
            "\"client_id\":\"%s\"," +
            "\"client_secret\":\"%s\"," +
            "\"refresh_token\":\"%s\"" +
            "}";
    
    /**
     * Spring RestTemplate for making HTTP requests to Jira REST API.
     */
    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Orchestrates Jira issue creation from organization notification with automatic token refresh.
     * <p>
     * This method performs the complete workflow for creating a Jira issue:
     * 
     * <ol>
     *   <li>Validates notification is organizational and propagation is enabled</li>
     *   <li>Retrieves organization configuration with refresh token</li>
     *   <li>Refreshes access token using OAuth refresh flow</li>
     *   <li>Discovers Jira cloud ID for organization</li>
     *   <li>Resolves project ID from project name</li>
     *   <li>Resolves issue type ID (defaults to "Task")</li>
     *   <li>Creates Jira issue via REST API</li>
     * </ol>
     * <p>
     * <b>Required Configuration:</b> Organization configuration must contain:
     * 
     * <ul>
     *   <li>{@code jiraRefreshToken} - OAuth refresh token for token renewal</li>
     *   <li>{@code jiraOrganizationName} - Jira organization/workspace name</li>
     *   <li>{@code jiraProjectName} - Target Jira project name</li>
     * </ul>
     * <p>
     * <b>Error Handling:</b>
     * 
     * <ul>
     *   <li>400 Bad Request - Invalid issue payload or missing required fields</li>
     *   <li>401 Unauthorized - Token expired (automatically refreshed)</li>
     *   <li>403 Forbidden - Insufficient permissions for project or issue type</li>
     *   <li>404 Not Found - Project or organization not found</li>
     * </ul>
     *
     * @param notification the notification DTO to convert to Jira issue (must be organizational)
     * @throws Exception if OAuth token refresh fails, Jira API returns error, or required configuration is missing
     * @see #refreshToken(IntegrationModuleOrganizationConfiguration, IntegrationModuleGlobalConfiguration)
     * @see #getCloudId(String, String)
     * @see #getProjectId(String, String, String)
     * @see #createJiraIssue(NotificationDto, String, String, String, String)
     */
    public void createJiraIssueFromOrgNotification(NotificationDto notification) throws Exception {
        debug("[createJiraIssueFromOrgNotification]");
        if (!services.notification.isOrganization(notification)) {
            info("[createJiraIssueFromOrgNotification] Notification is not organizational.");
            return;
        }
        if(!notification.getPropagate()){
            return;
        }
        IntegrationModuleOrganizationConfiguration configuration
                = integrationService.getOrganizationConfiguration(notification.getOrganizationId());
        String refreshToken = configuration.getJiraRefreshToken();
        if (StringUtils.isBlank(refreshToken)) {
            warn("[createJiraIssueFromOrgNotification] Jira token not found, try to reconnect.");
            return;
        }
        refreshToken(configuration, integrationService.getGlobalConfiguration());
        String token = configuration.getJiraToken();
        String organizationName = configuration.getJiraOrganizationName();
        String cloudId = getCloudId(token, organizationName);
        String projectName = configuration.getJiraProjectName();
        String projectId = getProjectId(token, cloudId, projectName);
        String issueTypeId = getIssueTypeId(token, cloudId, "Task");

        createJiraIssue(notification, token, cloudId, projectId, issueTypeId);
    }

    /**
     * Creates a Jira issue by posting to the Jira REST API v2 issue creation endpoint.
     * <p>
     * Constructs JSON payload with issue fields (summary, description, project, issue type)
     * and sends POST request with Bearer token authentication.
     * 
     * <p>
     * <b>JSON Payload Structure:</b>
     * 
     * <pre>
     * {
     *   "fields": {
     *     "project": {"id": "projectId"},
     *     "summary": "New notification from Jira",
     *     "description": "notification message",
     *     "issuetype": {"id": "issueTypeId"}
     *   }
     * }
     * </pre>
     *
     * @param notification the notification containing message to use as issue description
     * @param token the Jira OAuth access token for authentication
     * @param cloudId the Atlassian cloud instance ID
     * @param projectId the Jira project ID where issue will be created
     * @param issueTypeId the Jira issue type ID (e.g., Task, Bug, Story)
     * @throws Exception if HTTP request fails or Jira API returns error response
     * @see #prepareJiraIssueRequest(NotificationDto, String, String)
     * @see #prepareCreateIssueUrl(String)
     */
    private void createJiraIssue(NotificationDto notification, String token, String cloudId, String projectId, String issueTypeId) throws Exception {
        debug("[createJiraIssue]");
        HttpHeaders headers = prepareAuthorizationHeader(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String issueRequestJson = prepareJiraIssueRequest(notification, projectId, issueTypeId);
        HttpEntity<String> entity = new HttpEntity<>(issueRequestJson, headers);
        String createIssueUrl = prepareCreateIssueUrl(cloudId);
        ResponseEntity<JsonNode> response = restTemplate.exchange(createIssueUrl, HttpMethod.POST, entity, JsonNode.class);
        integrationService.handleResponseError(response, "[createJiraIssueFromOrgNotification] Error when creating issue. Code: {}. Error: {}");
    }

    /**
     * Formats the Jira issue creation API URL with the specified cloud ID.
     * <p>
     * Uses the configured {@code JIRA_CREATE_ISSUE_API} template URL and replaces
     * the cloud ID placeholder with the provided value.
     * 
     *
     * @param cloudId the Atlassian cloud instance ID
     * @return formatted URL string for Jira issue creation endpoint
     * @see #JIRA_CREATE_ISSUE_API
     */
    private String prepareCreateIssueUrl(String cloudId) {
        return String.format(JIRA_CREATE_ISSUE_API, cloudId);
    }

    /**
     * Constructs JSON payload for Jira issue creation request.
     * <p>
     * Uses {@link #JIRA_CREATE_ISSUE_REQUEST_JSON} template and formats it with:
     * 
     * <ul>
     *   <li>Summary: Fixed text "New notification from Jira"</li>
     *   <li>Description: Sanitized notification message (JSON-escaped via {@code integrationService.prepareJsonString()})</li>
     *   <li>Issue type ID: Provided issue type identifier</li>
     *   <li>Project ID: Provided project identifier</li>
     * </ul>
     *
     * @param notification the notification containing message to use as issue description
     * @param projectId the Jira project ID
     * @param issueTypeId the Jira issue type ID
     * @return JSON string formatted for Jira API issue creation request
     * @see #JIRA_CREATE_ISSUE_REQUEST_JSON
     */
    private String prepareJiraIssueRequest(NotificationDto notification, String projectId, String issueTypeId) {
        String jsonMessage = integrationService.prepareJsonString(notification.getMessage());
        return String.format(JIRA_CREATE_ISSUE_REQUEST_JSON, "New notification from Jira", jsonMessage, issueTypeId, projectId);
    }

    /**
     * Refreshes Jira OAuth access token using the stored refresh token.
     * <p>
     * Implements OAuth 2.0 refresh token flow by posting to Atlassian token endpoint:
     * 
     * <ol>
     *   <li>Retrieves refresh token from organization configuration</li>
     *   <li>Constructs JSON payload with grant_type, client credentials, and refresh token</li>
     *   <li>Posts to {@code JIRA_REFRESH_TOKEN_API} endpoint</li>
     *   <li>Extracts new access token from response</li>
     *   <li>Updates organization configuration with new access token</li>
     *   <li>Persists updated configuration via {@code repositories.unsecure.integration.save()}</li>
     * </ol>
     * <p>
     * <b>Required Configuration:</b>
     * 
     * <ul>
     *   <li>{@code config.jiraRefreshToken} - Valid OAuth refresh token</li>
     *   <li>{@code globalConfig.jiraClientId} - OAuth client ID</li>
     *   <li>{@code globalConfig.jiraClientSecret} - OAuth client secret</li>
     * </ul>
     * <p>
     * <b>JSON Payload:</b> {@code {"grant_type":"refresh_token","client_id":"...","client_secret":"...","refresh_token":"..."}}
     * 
     *
     * @param config the organization configuration containing refresh token (will be updated with new access token)
     * @param globalConfig the global configuration containing OAuth client credentials
     * @throws Exception if token refresh fails, response lacks access_token, or persistence fails
     * @see #JIRA_REFRESH_TOKEN_JSON
     * @see #JIRA_REFRESH_TOKEN_API
     */
    private void refreshToken(IntegrationModuleOrganizationConfiguration config, IntegrationModuleGlobalConfiguration globalConfig) throws Exception {
        debug("[refreshToken]");
        String refreshToken = config.getJiraRefreshToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String refreshRequest = String.format(JIRA_REFRESH_TOKEN_JSON, globalConfig.jiraClientId, globalConfig.jiraClientSecret, refreshToken);
        HttpEntity<String> entity = new HttpEntity<>(refreshRequest, headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(JIRA_REFRESH_TOKEN_API, HttpMethod.POST, entity, JsonNode.class);
        integrationService.handleResponseError(response, "[refreshToken] Error during refresh of token. Code: {}. Error: {}");
        if (!response.getBody().has("access_token")) {
            error("[refreshToken] There is no access token. Body: \n{}", response.getBody().asText());
            throw new Exception("[refreshToken] No token in successful request");
        }
        String accessToken = response.getBody().get("access_token").asText();
        config.setJiraToken(accessToken);
        repositories.unsecure.integration.save(config);
    }

    /**
     * Retrieves Jira issue type ID by matching the issue type name.
     * <p>
     * Queries Jira REST API v2 issue types endpoint and iterates through returned
     * issue types to find a match by name. Commonly used to resolve "Task", "Bug",
     * "Story", or custom issue type names to their corresponding IDs.
     * 
     *
     * @param token the Jira OAuth access token for authentication
     * @param cloudId the Atlassian cloud instance ID
     * @param taskType the issue type name to search for (e.g., "Task", "Bug")
     * @return the Jira issue type ID as a string
     * @throws Exception if issue type not found in project or API request fails
     * @see #JIRA_GET_ISSUE_TYPES_API
     */
    private String getIssueTypeId(String token, String cloudId, String taskType) throws Exception {
        debug("[getIssueTypeId]");
        HttpHeaders headers = prepareAuthorizationHeader(token);
        String getIssueTypesUrl = String.format(JIRA_GET_ISSUE_TYPES_API, cloudId);
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(getIssueTypesUrl, HttpMethod.GET, entity, JsonNode.class);
        integrationService.handleResponseError(response, "[getIssueTypeId] Error when accessing issue type Id. Code: {}. Error: {}");
        for (JsonNode issueType : response.getBody()) {
            String name = issueType.get("name").asText();
            if (name.equals(taskType)) {
                return issueType.get("id").asText();
            }
        }
        error("[getIssueTypeId] Cannot find Id for the issue type");
        throw new Exception("Cannot find Id for the issue type");
    }

    /**
     * Retrieves Jira project ID by matching the project name.
     * <p>
     * Queries Jira REST API v2 projects endpoint and iterates through accessible
     * projects to find a match by exact name. The project must be accessible
     * to the authenticated user with the provided access token.
     * 
     *
     * @param token the Jira OAuth access token for authentication
     * @param cloudId the Atlassian cloud instance ID
     * @param projectName the exact project name to search for
     * @return the Jira project ID as a string
     * @throws Exception if project not found or user lacks access permissions
     * @see #JIRA_GET_PROJECT_LIST_API
     */
    private String getProjectId(String token, String cloudId, String projectName) throws Exception {
        debug("[getProjectId]");
        HttpHeaders headers = prepareAuthorizationHeader(token);
        String getProjectUrl = String.format(JIRA_GET_PROJECT_LIST_API, cloudId);
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(getProjectUrl, HttpMethod.GET, entity, JsonNode.class);
        integrationService.handleResponseError(response, "[getProjectId] Error when accessing project Id. Code: {}. Error: {}");
        for (JsonNode project : response.getBody()) {
            String name = project.get("name").asText();
            if (name.equals(projectName)) {
                return project.get("id").asText();
            }
        }
        error("[getProjectId] Cannot find Id for the project provided");
        throw new Exception("Cannot find Id for the project provided");
    }

    /**
     * Discovers Atlassian cloud ID for the specified organization name.
     * <p>
     * Queries the accessible-resources API endpoint to retrieve all Jira cloud
     * instances accessible to the authenticated user, then matches by organization
     * name to obtain the corresponding cloud ID. The cloud ID is required for
     * all subsequent Jira REST API v2 calls.
     * 
     *
     * @param token the Jira OAuth access token for authentication
     * @param organizationName the Jira organization/workspace name to search for
     * @return the Atlassian cloud ID as a string
     * @throws Exception if organization not found or user lacks access permissions
     * @see #requestCloudId(String)
     * @see #JIRA_GET_CLOUDID_API
     */
    private String getCloudId(String token, String organizationName) throws Exception {
        debug("[getCloudId]");
        ResponseEntity<JsonNode> cloudIdResponse = requestCloudId(token);
        integrationService.handleResponseError(cloudIdResponse, "[getCloudId] Error when reaching to JIRA cloudId. Code: {}. Error: {}");
        for (JsonNode resource : cloudIdResponse.getBody()) {
            String resourceOrgName = resource.get("name").asText();
            if (resourceOrgName.equals(organizationName)) {
                return resource.get("id").asText();
            }
        }
        error("[getCloudId] Cannot find Id for the organization provided");
        throw new Exception("Cannot find Id for the organization provided");
    }

    /**
     * Executes HTTP GET request to Atlassian accessible-resources API endpoint.
     * <p>
     * Returns JSON array of accessible Jira cloud resources, each containing:
     * 
     * <ul>
     *   <li>{@code id} - Cloud instance identifier</li>
     *   <li>{@code name} - Organization/workspace name</li>
     *   <li>{@code url} - Cloud instance URL</li>
     *   <li>{@code scopes} - Granted OAuth scopes</li>
     * </ul>
     *
     * @param token the Jira OAuth access token for authentication
     * @return ResponseEntity containing JsonNode array of accessible resources
     * @see #JIRA_GET_CLOUDID_API
     */
    private ResponseEntity<JsonNode> requestCloudId(String token) {
        HttpHeaders headers = prepareAuthorizationHeader(token);
        HttpEntity entity = new HttpEntity(headers);
        return restTemplate.exchange(JIRA_GET_CLOUDID_API, HttpMethod.GET, entity, JsonNode.class);
    }

    /**
     * Constructs HTTP headers with Bearer token authorization for Jira API requests.
     * <p>
     * Creates new {@link HttpHeaders} instance and sets Authorization header
     * with format: {@code Bearer <access_token>}
     * 
     *
     * @param token the Jira OAuth access token
     * @return HttpHeaders configured with Authorization: Bearer header
     */
    private HttpHeaders prepareAuthorizationHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }
}
