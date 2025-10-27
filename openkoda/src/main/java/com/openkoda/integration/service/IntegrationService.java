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

package com.openkoda.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.core.service.event.EventConsumerCategory;
import com.openkoda.dto.NotificationDto;
import com.openkoda.integration.consumer.BasecampIntegrationConsumers;
import com.openkoda.integration.consumer.GitHubIntegrationConsumers;
import com.openkoda.integration.consumer.JiraIntegrationConsumers;
import com.openkoda.integration.consumer.TrelloIntegrationConsumers;
import com.openkoda.integration.controller.IntegrationComponentProvider;
import com.openkoda.integration.controller.common.IntegrationPageAttributes;
import com.openkoda.integration.controller.common.IntegrationURLConstants;
import com.openkoda.integration.form.*;
import com.openkoda.integration.model.IntegrationPrivilege;
import com.openkoda.integration.model.configuration.IntegrationModuleGlobalConfiguration;
import com.openkoda.integration.model.configuration.IntegrationModuleOrganizationConfiguration;
import com.openkoda.integration.model.dto.*;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Central integration service facade orchestrating OAuth flows, token management, configuration persistence, 
 * and notification consumer registration.
 * <p>
 * This service acts as the primary entry point for all integration-related operations in OpenKoda. It manages
 * the complete lifecycle of third-party integrations including GitHub, Jira, Basecamp, Trello, Slack, and MS Teams.
 * </p>
 * 
 * <h3>Key Responsibilities:</h3>
 * <ul>
 *   <li>Registers notification consumers (Trello, GitHub, Jira, Basecamp) for event-driven integration workflows</li>
 *   <li>Performs OAuth token exchanges with external providers using authorization codes</li>
 *   <li>Persists and retrieves per-organization integration configuration</li>
 *   <li>Implements token refresh logic for maintaining active OAuth sessions</li>
 *   <li>Provides error handling helpers for HTTP responses and JSON string preparation</li>
 * </ul>
 * 
 * <h3>OAuth Flow:</h3>
 * <p>
 * The standard OAuth flow managed by this service follows these steps:
 * </p>
 * <ol>
 *   <li>User initiates OAuth by navigating to provider-specific authorization URL (e.g., {@code gitHubOAuthUrl})</li>
 *   <li>Provider redirects back with authorization code</li>
 *   <li>Service exchanges code for access token (e.g., {@code getGitHubToken})</li>
 *   <li>Token is stored in {@link IntegrationModuleOrganizationConfiguration} via repository</li>
 * </ol>
 * 
 * <h3>Token Refresh:</h3>
 * <p>
 * When API calls return 401 Unauthorized, the service can refresh expired tokens using refresh tokens.
 * For example, {@link #refreshBasecampToken(Long)} obtains a new access token using the stored refresh token.
 * </p>
 * 
 * <h3>Configuration Management:</h3>
 * <p>
 * Configuration is scoped per organization. {@link #getOrganizationConfiguration(Long)} retrieves the
 * configuration entity containing tokens, API keys, and provider-specific settings for a given organization.
 * The {@link #cleanOrgConfig(String, Long)} method clears configuration when disconnecting an integration.
 * </p>
 * 
 * <h3>Example Usage:</h3>
 * <pre>
 * // Exchange GitHub authorization code for token
 * integrationService.getGitHubToken(organizationId, authorizationCode);
 * 
 * // Retrieve organization configuration
 * IntegrationModuleOrganizationConfiguration config = 
 *     integrationService.getOrganizationConfiguration(organizationId);
 * </pre>
 * 
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see IntegrationModuleOrganizationConfiguration
 * @see IntegrationModuleGlobalConfiguration
 * @see IntegrationComponentProvider
 */
@Service("integrationService")
public class IntegrationService extends IntegrationComponentProvider {

    @Inject
    private IntegrationModuleGlobalConfiguration integrationGlobalConfiguration;

    public static final String SHA3_256 = "SHA3-256";
    public static final String GITHUB_TOKEN_REQUEST_JSON = "{" +
            "\"client_id\":\"%s\"," +
            "\"client_secret\":\"%s\"," +
            "\"code\":\"%s\"" +
            "}";

    @Value("${api.basecamp.oauth.authorize:https://launchpad.37signals.com/authorization/new?type=web_server&client_id=%s&redirect_uri=%s&state=%s}")
    private String BASECAMP_OAUTH_AUTHORIZE_API;
    @Value("${api.basecamp.authorize.token:https://launchpad.37signals.com/authorization/token?type=web_server&client_id=%s&redirect_uri=%s&client_secret=%s&code=%s}")
    private String BASECAMP_AUTHORIZE_TOKEN_API;
    @Value("${api.basecamp.refresh.token:https://launchpad.37signals.com/authorization/token?type=refresh&refresh_token=%s&client_id=%s&redirect_uri=%s&client_secret=%s}")
    private String BASECAMP_REFRESH_TOKEN_API;
    @Value("${api.github.oauth.authorize:https://github.com/login/oauth/authorize?scope=%s&client_id=%s&redirect_uri=%s}")
    private String GITHUB_OAUTH_AUTHORIZE_API;
    @Value("${api.github.authorize.token:https://github.com/login/oauth/access_token}")
    private String GITHUB_AUTHORIZE_TOKEN_API;
    @Value("${api.github.access.scope:repo}")
    private String GITHUB_SCOPE;
    @Value("${api.jira.oauth.authorize:https://auth.atlassian.com/authorize?audience=api.atlassian.com&client_id=%s&scope=%s&redirect_uri=%s&state=%s&response_type=code&prompt=consent}")
    private String JIRA_CLOUD_OAUTH_AUTHORIZE_API;
    @Value("${api.jira.authorize.token:https://auth.atlassian.com/oauth/token}")
    private String JIRA_AUTHORIZE_TOKEN_API;
    @Value("${api.jira.access.scope:write:jira-work read:jira-work offline_access}")
    private String JIRA_SCOPE;

    /**
     * Initializes the integration service by registering privileges and notification consumers.
     * <p>
     * This method is called automatically after dependency injection is complete. It performs two
     * critical setup tasks: registering integration privileges with the privilege system and
     * registering notification consumers for event-driven integration workflows.
     * </p>
     * 
     * @see IntegrationPrivilege
     * @see #registerConsumers()
     */
    @PostConstruct
    void init() {
        PrivilegeHelper.registerEnumClasses(new Class[]{IntegrationPrivilege.class});
        registerConsumers();
    }

    /**
     * Validates HTTP response and handles errors by logging and throwing exceptions.
     * <p>
     * This helper method checks if the response status code indicates success (2xx range).
     * If the response is not successful, it logs the error with the provided message and
     * throws an exception to halt processing.
     * </p>
     * 
     * @param response the HTTP response entity to validate
     * @param errorMessage the error message template to log if response indicates failure
     * @throws Exception if the response status code is not in the 2xx success range
     */
    public void handleResponseError(ResponseEntity response, String errorMessage) throws Exception {
        debug("[handleResponseError]");
        if (!response.getStatusCode().is2xxSuccessful()) {
            error(errorMessage, response.getStatusCodeValue(), response.getBody());
            throw new Exception("Response is not successful");
        }
    }

    /**
     * Prepares a GitHub integration form for a specific organization.
     * <p>
     * This method retrieves the organization's configuration and creates a form
     * with appropriate field mappings based on whether a GitHub token exists.
     * If no token is present, the form is created in a disabled state requiring
     * OAuth authorization first.
     * </p>
     * 
     * @param organizationId the ID of the organization for which to prepare the form
     * @return a configured GitHub integration form, either enabled or disabled based on token presence
     */
    public IntegrationGitHubForm prepareGitHubFormForOrg(Long organizationId) {
        debug("[prepareGitHubFormForOrg]");
        IntegrationModuleOrganizationConfiguration organizationConfiguration = getOrganizationConfiguration(organizationId);
        String gitHubToken = organizationConfiguration.getGitHubToken();
        if (StringUtils.isBlank(gitHubToken)) {
            return new IntegrationGitHubForm(new IntegrationGitHubDto(), organizationConfiguration, IntegrationFrontendMappingDefinitions.gitHubConfigurationFormDisabled);
        }
        return new IntegrationGitHubForm(new IntegrationGitHubDto(), organizationConfiguration, IntegrationFrontendMappingDefinitions.gitHubConfigurationForm);
    }

    /**
     * Prepares a Trello integration form for a specific organization.
     * <p>
     * This method retrieves the organization's configuration and creates a form
     * populated with existing Trello settings such as API key, API token, board name, and list name.
     * </p>
     * 
     * @param organizationId the ID of the organization for which to prepare the form
     * @return a configured Trello integration form with current organization settings
     */
    public IntegrationTrelloForm prepareTrelloFormForOrg(Long organizationId) {
        return new IntegrationTrelloForm(new IntegrationTrelloDto(), integrationService.getOrganizationConfiguration(organizationId));
    }

    /**
     * Prepares a Basecamp integration form for a specific organization.
     * <p>
     * This method retrieves the organization's configuration and creates a form
     * with appropriate field mappings based on whether a Basecamp refresh token exists.
     * If no refresh token is present, the form is created in a disabled state requiring
     * OAuth authorization first.
     * </p>
     * 
     * @param organizationId the ID of the organization for which to prepare the form
     * @return a configured Basecamp integration form, either enabled or disabled based on token presence
     */
    public IntegrationBasecampForm prepareBasecampFormForOrg(Long organizationId) {
        debug("[prepareBasecampFormForOrg]");
        IntegrationModuleOrganizationConfiguration organizationConfiguration = getOrganizationConfiguration(organizationId);
        String basecampRefreshToken = organizationConfiguration.getBasecampRefreshToken();
        if (StringUtils.isBlank(basecampRefreshToken)) {
            return new IntegrationBasecampForm(new IntegrationBasecampDto(), organizationConfiguration, IntegrationFrontendMappingDefinitions.basecampConfigurationFormDisabled);
        }
        return new IntegrationBasecampForm(new IntegrationBasecampDto(), organizationConfiguration, IntegrationFrontendMappingDefinitions.basecampConfigurationForm);
    }

    /**
     * Prepares a Slack integration form for a specific organization.
     * <p>
     * This method retrieves the organization's configuration and creates a form
     * populated with existing Slack settings such as webhook URL for push notifications.
     * </p>
     * 
     * @param organizationId the ID of the organization for which to prepare the form
     * @return a configured Slack integration form with current organization settings
     */
    public IntegrationSlackForm prepareSlackFormForOrg(Long organizationId) {
        return new IntegrationSlackForm(new IntegrationSlackDto(), integrationService.getOrganizationConfiguration(organizationId));
    }

    /**
     * Prepares a Microsoft Teams integration form for a specific organization.
     * <p>
     * This method retrieves the organization's configuration and creates a form
     * populated with existing MS Teams settings such as webhook URL for push notifications.
     * </p>
     * 
     * @param organizationId the ID of the organization for which to prepare the form
     * @return a configured MS Teams integration form with current organization settings
     */
    public IntegrationMsTeamsForm prepareMsTeamsFormForOrg(Long organizationId) {
        return new IntegrationMsTeamsForm(new IntegrationMsTeamsDto(), integrationService.getOrganizationConfiguration(organizationId));
    }

    /**
     * Prepares a Jira integration form for a specific organization.
     * <p>
     * This method retrieves the organization's configuration and creates a form
     * with appropriate field mappings based on whether a Jira access token exists.
     * If no token is present, the form is created in a disabled state requiring
     * OAuth authorization first.
     * </p>
     * 
     * @param organizationId the ID of the organization for which to prepare the form
     * @return a configured Jira integration form, either enabled or disabled based on token presence
     */
    public IntegrationJiraForm prepareJiraFormForOrg(Long organizationId) {
        IntegrationModuleOrganizationConfiguration organizationConfiguration = getOrganizationConfiguration(organizationId);
        String jiraToken = organizationConfiguration.getJiraToken();
        if (StringUtils.isBlank(jiraToken)) {
            return new IntegrationJiraForm(new IntegrationJiraDto(), organizationConfiguration, IntegrationFrontendMappingDefinitions.jiraConfigurationFormDisabled);
        }
        return new IntegrationJiraForm(new IntegrationJiraDto(), integrationService.getOrganizationConfiguration(organizationId));
    }

    /**
     * Exchanges a GitHub OAuth authorization code for an access token.
     * <p>
     * This method is called after the OAuth callback receives an authorization code from GitHub.
     * It makes a POST request to GitHub's token endpoint with the client credentials and code,
     * then extracts the access token from the response and persists it in the organization's
     * configuration for future API calls.
     * </p>
     * <p>
     * <strong>OAuth Flow Step:</strong> This is step 3 of the OAuth flow - exchanging the
     * authorization code for an access token.
     * </p>
     * 
     * @param orgId the organization ID for which to store the token
     * @param temporaryCode the authorization code received from GitHub's OAuth callback
     * @throws Exception if the token exchange fails or the response is invalid
     * @see #gitHubOAuthUrl(Long)
     */
    public void getGitHubToken(Long orgId, String temporaryCode) throws Exception {
        debug("[getGitHubToken]");
        String tokenRequest = prepareGithubTokenRequest(temporaryCode);
        HttpHeaders headers = getHttpHeadersOfApplicationJson();
        HttpEntity<String> request = new HttpEntity<>(tokenRequest, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(GITHUB_AUTHORIZE_TOKEN_API, request, JsonNode.class);
        handleResponseError(response, "[getGitHubToken] Error during authorization of new token. Code: {}. Error: {}");
        JsonNode body = response.getBody();
        if (body.has("error") || !body.has("access_token")) {
            error("[getGitHubToken] Error: {}", body.has("error") ? body.get("error").asText() : "There is no access token");
            return;
        }
        String gitHubToken = body.get("access_token").asText();
        IntegrationModuleOrganizationConfiguration configuration = integrationService.getOrganizationConfiguration(orgId);
        configuration.setGitHubToken(gitHubToken);
        repositories.unsecure.integration.save(configuration);
    }

    /**
     * Exchanges a Jira OAuth authorization code for access and refresh tokens.
     * <p>
     * This method is called after the OAuth callback receives an authorization code from Atlassian/Jira.
     * It makes a POST request to Jira's token endpoint with the client credentials and code,
     * then extracts both the access token and refresh token from the response and persists them
     * in the organization's configuration. The refresh token enables automatic token renewal.
     * </p>
     * <p>
     * <strong>OAuth Flow Step:</strong> This is step 3 of the OAuth flow - exchanging the
     * authorization code for access and refresh tokens.
     * </p>
     * 
     * @param orgId the organization ID for which to store the tokens
     * @param temporaryCode the authorization code received from Jira's OAuth callback
     * @throws Exception if the token exchange fails or the response is invalid
     * @see #jiraOAuthUrl(Long)
     */
    public void getJiraToken(Long orgId, String temporaryCode) throws Exception {
        debug("[getJiraToken]");
        RestTemplate restTemplate = new RestTemplate();
        String tokenRequest = prepareJiraTokenRequest(temporaryCode, orgId);
        HttpHeaders headers = getHttpHeadersOfApplicationJson();
        HttpEntity<String> request = new HttpEntity<>(tokenRequest, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(JIRA_AUTHORIZE_TOKEN_API, request, JsonNode.class);
        handleResponseError(response, "[getGitHubToken] Error during authorization of new token. Code: {}. Error: {}");
        JsonNode body = response.getBody();
        if (body.has("error") || !body.has("access_token")) {
            error("[getGitHubToken] Error: {}", body.has("error") ? body.get("error").asText() : "There is no access token");
        }
        IntegrationModuleOrganizationConfiguration configuration = integrationService.getOrganizationConfiguration(orgId);
        String jiraToken = body.get("access_token").asText();
        configuration.setJiraToken(jiraToken);
        String jiraRefreshToken = body.get("refresh_token").asText();
        configuration.setJiraRefreshToken(jiraRefreshToken);
        repositories.unsecure.integration.save(configuration);
    }

    /**
     * Prepares the JSON request body for GitHub token exchange.
     * <p>
     * This helper method constructs a JSON payload containing the client ID, client secret,
     * and authorization code required by GitHub's token endpoint.
     * </p>
     * 
     * @param temporaryCode the authorization code received from GitHub
     * @return a JSON string formatted for GitHub's token exchange API
     */
    private String prepareGithubTokenRequest(String temporaryCode) {
        debug("[prepareGithubTokenRequest]");
        IntegrationModuleGlobalConfiguration globalConfig = integrationService.getGlobalConfiguration();
        String request = String.format(GITHUB_TOKEN_REQUEST_JSON, globalConfig.gitHubClientId, globalConfig.gitHubClientSecret, temporaryCode);
        return request;
    }

    /**
     * Prepares the JSON request body for Jira token exchange.
     * <p>
     * This helper method constructs a JSON payload containing the grant type, client ID,
     * client secret, authorization code, and redirect URI required by Jira's token endpoint.
     * </p>
     * 
     * @param temporaryCode the authorization code received from Jira
     * @param orgId the organization ID used to construct the redirect URI
     * @return a JSON string formatted for Jira's token exchange API
     */
    private String prepareJiraTokenRequest(String temporaryCode, Long orgId) {
        debug("[prepareJiraTokenRequest]");
        IntegrationModuleGlobalConfiguration globalConfig = integrationService.getGlobalConfiguration();
        String request = String.format("{" +
                "    \"grant_type\": \"authorization_code\"," +
                "    \"client_id\": \"%s\"," +
                "    \"client_secret\": \"%s\"," +
                "    \"code\": \"%s\",\n" +
                "    \"redirect_uri\": \"%s\"" +
                "}", globalConfig.jiraClientId, globalConfig.jiraClientSecret, temporaryCode, integrationUrlHelper.jiraTokenAccessUrl(orgId));
        return request;
    }

    /**
     * Exchanges a Basecamp OAuth authorization code for access and refresh tokens.
     * <p>
     * This method is called after the OAuth callback receives an authorization code from Basecamp.
     * It makes a POST request to Basecamp's token endpoint with the client credentials and code,
     * then extracts both the access token and refresh token from the response and persists them
     * in the organization's configuration. The refresh token enables automatic token renewal.
     * </p>
     * <p>
     * <strong>OAuth Flow Step:</strong> This is step 3 of the OAuth flow - exchanging the
     * authorization code for access and refresh tokens.
     * </p>
     * 
     * @param orgId the organization ID for which to store the tokens
     * @param code the authorization code received from Basecamp's OAuth callback
     * @throws Exception if the token exchange fails or the response is invalid
     * @see #basecampOAuthUrl(Long)
     * @see #refreshBasecampToken(Long)
     */
    public void getBasecampToken(Long orgId, String code) throws Exception {
        debug("[getBasecampToken]");
        String tokenRequestUrl = prepareBasecampAccessTokenRequestUrl(code, orgId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(tokenRequestUrl, request, JsonNode.class);
        handleResponseError(response, "[getBasecampToken] Error during authorization of new token. Code: {}. Error: {}");
        JsonNode body = response.getBody();
        if (body.has("error") || !body.has("access_token")) {
            error("[getBasecampToken] Error: {}", body.has("error") ? body.get("error").asText() : "There is no access token");
        } else {
            String basecampAccessToken = body.get("access_token").textValue();
            IntegrationModuleOrganizationConfiguration configuration = integrationService.getOrganizationConfiguration(orgId);
            configuration.setBasecampAccessToken(basecampAccessToken);
            String basecampRefreshToken = body.get("refresh_token").textValue();
            configuration.setBasecampRefreshToken(basecampRefreshToken);
            repositories.unsecure.integration.save(configuration);
        }
    }

    /**
     * Refreshes an expired Basecamp access token using the stored refresh token.
     * <p>
     * This method is called when a Basecamp API request returns a 401 Unauthorized error,
     * indicating the access token has expired. It uses the stored refresh token to obtain
     * a new access token from Basecamp and updates the organization's configuration with
     * the new token.
     * </p>
     * <p>
     * <strong>Token Lifecycle:</strong> Access tokens expire after a certain period. This method
     * enables seamless token renewal without requiring user re-authentication.
     * </p>
     * 
     * @param orgId the organization ID for which to refresh the token
     * @return {@code true} if the token was successfully refreshed, {@code false} if the refresh failed
     * @throws Exception if the refresh request fails or the response is invalid
     * @see #getBasecampToken(Long, String)
     */
    public boolean refreshBasecampToken(Long orgId) throws Exception {
        debug("[refreshBasecampToken]");
        IntegrationModuleOrganizationConfiguration configuration = integrationService.getOrganizationConfiguration(orgId);
        String tokenRequestUrl = prepareBasecampRefreshTokenRequestUrl(configuration.getBasecampRefreshToken(), orgId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(tokenRequestUrl, request, JsonNode.class);
        handleResponseError(response, "[refreshBasecampToken] Error during authorization of new token. Code: {}. Error: {}");
        JsonNode body = response.getBody();
        if (body.has("error") || !body.has("access_token")) {
            error("[getBasecampToken] Error: {}", body.has("error") ? body.get("error").asText() : "There is no access token");
            return false;
        }
        String basecampAccessToken = body.get("access_token").asText();
        configuration.setBasecampAccessToken(basecampAccessToken);
        repositories.unsecure.integration.save(configuration);
        return true;
    }

    /**
     * Retrieves the integration configuration for a specific organization.
     * <p>
     * This method fetches the per-organization configuration entity containing OAuth tokens,
     * API keys, and provider-specific settings. Each organization has its own configuration
     * allowing independent integration setups across tenants.
     * </p>
     * 
     * @param organizationId the ID of the organization whose configuration to retrieve
     * @return the organization's integration configuration, or {@code null} if not found
     */
    public IntegrationModuleOrganizationConfiguration getOrganizationConfiguration(Long organizationId) {
        debug("[getOrganizationConfiguration]");
        return repositories.unsecure.integration.findByOrganizationId(organizationId);
    }

    /**
     * Retrieves the integration configuration for a specific organization (internal method).
     * <p>
     * This is an internal alias for {@link #getOrganizationConfiguration(Long)} used within
     * the integration module for consistency with naming conventions.
     * </p>
     * 
     * @param organizationId the ID of the organization whose configuration to retrieve
     * @return the organization's integration configuration, or {@code null} if not found
     * @see #getOrganizationConfiguration(Long)
     */
    public IntegrationModuleOrganizationConfiguration getInnerOrganizationConfig(Long organizationId) {
        debug("[getInnerOrganizationConfig]");
        return getOrganizationConfiguration(organizationId);
    }

    /**
     * Retrieves the global integration configuration shared across all organizations.
     * <p>
     * The global configuration contains OAuth client IDs and secrets for external providers
     * that are configured at the application level rather than per-organization. These settings
     * are typically managed by administrators and used across all tenant organizations.
     * </p>
     * 
     * @return the global integration configuration injected at service initialization
     */
    public IntegrationModuleGlobalConfiguration getGlobalConfiguration() {
        debug("[getGlobalConfiguration]");
        return integrationGlobalConfiguration;
    }

    /**
     * Constructs the GitHub OAuth authorization URL for initiating the OAuth flow.
     * <p>
     * This URL redirects the user to GitHub's authorization page where they grant permissions
     * to the OpenKoda application. After authorization, GitHub redirects back to the callback URL
     * with an authorization code that can be exchanged for an access token.
     * </p>
     * <p>
     * <strong>OAuth Flow Step:</strong> This is step 1 of the OAuth flow - redirecting the user
     * to the provider's authorization page.
     * </p>
     * 
     * @param orgId the organization ID to include in the callback URL for token storage
     * @return the complete GitHub OAuth authorization URL
     * @see #getGitHubToken(Long, String)
     */
    public String gitHubOAuthUrl(Long orgId) {
        debug("[gitHubOAuthUrl]");
        String clientId = getGlobalConfiguration().gitHubClientId;
        return String.format(GITHUB_OAUTH_AUTHORIZE_API, GITHUB_SCOPE, clientId, integrationUrlHelper.gitHubTokenAccessUrl(orgId));
    }

    /**
     * Constructs the Jira OAuth authorization URL for initiating the OAuth flow.
     * <p>
     * This URL redirects the user to Atlassian's authorization page where they grant permissions
     * to the OpenKoda application. After authorization, Jira redirects back to the callback URL
     * with an authorization code that can be exchanged for access and refresh tokens.
     * </p>
     * <p>
     * <strong>OAuth Flow Step:</strong> This is step 1 of the OAuth flow - redirecting the user
     * to the provider's authorization page.
     * </p>
     * 
     * @param orgId the organization ID to include in the callback URL and state parameter
     * @return the complete Jira OAuth authorization URL
     * @see #getJiraToken(Long, String)
     */
    public String jiraOAuthUrl(Long orgId) {
        debug("[jiraOAuthUrl]");
        String clientId = getGlobalConfiguration().jiraClientId;
        String callbackUrl = integrationUrlHelper.jiraTokenAccessUrl(orgId);
        String oauthUrl = String.format(JIRA_CLOUD_OAUTH_AUTHORIZE_API, clientId, JIRA_SCOPE, callbackUrl, orgId);
        debug("[jiraOAuthUrl] returning url {}", oauthUrl);
        return oauthUrl;
    }

    /**
     * Constructs the Basecamp OAuth authorization URL for initiating the OAuth flow.
     * <p>
     * This URL redirects the user to Basecamp's (37signals) authorization page where they grant
     * permissions to the OpenKoda application. After authorization, Basecamp redirects back to
     * the callback URL with an authorization code that can be exchanged for access and refresh tokens.
     * </p>
     * <p>
     * <strong>OAuth Flow Step:</strong> This is step 1 of the OAuth flow - redirecting the user
     * to the provider's authorization page.
     * </p>
     * 
     * @param orgId the organization ID to include in the callback URL and state parameter
     * @return the complete Basecamp OAuth authorization URL
     * @see #getBasecampToken(Long, String)
     */
    public String basecampOAuthUrl(Long orgId) {
        debug("[basecampOAuthUrl]");
        String clientId = getGlobalConfiguration().basecampClientId;
        return String.format(BASECAMP_OAUTH_AUTHORIZE_API, clientId, integrationUrlHelper.basecampRedirectUrl(orgId), orgId);
    }

    /**
     * Creates HTTP headers for JSON content type requests.
     * <p>
     * This helper method constructs standard HTTP headers that accept and send JSON content.
     * Used for OAuth token exchange requests that require JSON payloads.
     * </p>
     * 
     * @return HTTP headers configured for application/json content type
     */
    private HttpHeaders getHttpHeadersOfApplicationJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Prepares the URL for Basecamp access token exchange.
     * <p>
     * This helper method constructs a complete URL with query parameters containing
     * the client ID, redirect URI, client secret, and authorization code required
     * by Basecamp's token endpoint.
     * </p>
     * 
     * @param code the authorization code received from Basecamp
     * @param orgId the organization ID used to construct the redirect URI
     * @return the complete URL for Basecamp token exchange
     */
    private String prepareBasecampAccessTokenRequestUrl(String code, Long orgId) {
        debug("[prepareBasecampRokenRequestUrl]");
        IntegrationModuleGlobalConfiguration globalConfig = integrationService.getGlobalConfiguration();
        return String.format(BASECAMP_AUTHORIZE_TOKEN_API, globalConfig.basecampClientId, integrationUrlHelper.basecampRedirectUrl(orgId), globalConfig.basecampClientSecret, code);
    }

    /**
     * Prepares the URL for Basecamp token refresh.
     * <p>
     * This helper method constructs a complete URL with query parameters containing
     * the refresh token, client ID, redirect URI, and client secret required
     * by Basecamp's token refresh endpoint.
     * </p>
     * 
     * @param refreshToken the refresh token stored in the organization's configuration
     * @param orgId the organization ID used to construct the redirect URI
     * @return the complete URL for Basecamp token refresh
     */
    private String prepareBasecampRefreshTokenRequestUrl(String refreshToken, Long orgId) {
        debug("[prepareBasecampRefreshTokenRequestUrl]");
        IntegrationModuleGlobalConfiguration globalConfig = integrationService.getGlobalConfiguration();
        return String.format(BASECAMP_REFRESH_TOKEN_API, refreshToken, globalConfig.basecampClientId, integrationUrlHelper.basecampRedirectUrl(orgId), globalConfig.basecampClientSecret);
    }

    /**
     * Clears the integration configuration for a specific provider and organization.
     * <p>
     * This method is called when disconnecting an integration. It removes all provider-specific
     * settings including OAuth tokens, API keys, and configuration parameters, then persists
     * the cleaned configuration to the database.
     * </p>
     * <p>
     * Supported applications: Trello, GitHub, Slack, MS Teams, Jira, and Basecamp.
     * </p>
     * 
     * @param application the provider identifier (e.g., "_trello", "_github", "_jira")
     * @param orgId the organization ID whose configuration to clear
     * @see IntegrationURLConstants
     */
    public void cleanOrgConfig(String application, Long orgId) {
        IntegrationModuleOrganizationConfiguration configuration = getOrganizationConfiguration(orgId);
        switch (application) {
            case IntegrationURLConstants._TRELLO:
                configuration.setTrelloApiToken(null);
                configuration.setTrelloApiKey(null);
                configuration.setTrelloBoardName(null);
                configuration.setTrelloListName(null);
                break;
            case IntegrationURLConstants._GITHUB:
                configuration.setGitHubToken(null);
                configuration.setGitHubRepoOwner(null);
                configuration.setGitHubRepoName(null);
                break;
            case IntegrationURLConstants._SLACK:
                configuration.setSlackWebhookUrl(null);
                break;
            case IntegrationURLConstants._MSTEAMS:
                configuration.setMsTeamsWebhookUrl(null);
                break;
            case IntegrationURLConstants._JIRA:
                configuration.setJiraOrganizationName(null);
                configuration.setJiraProjectName(null);
                configuration.setJiraToken(null);
                configuration.setJiraRefreshToken(null);
                break;
            case IntegrationURLConstants._BASECAMP:
                configuration.setBasecampAccessToken(null);
                configuration.setBasecampRefreshToken(null);
                configuration.setBasecampToDoListUrl(null);
                configuration.setBasecampProjectId(null);
                configuration.setBasecampAccountId(null);
                configuration.setBasecampToDoListId(null);
                break;
            default:
        }
        repositories.unsecure.integration.save(configuration);
    }

    /**
     * Escapes double quotes in a string for safe JSON formatting.
     * <p>
     * This utility method prepares strings for inclusion in JSON payloads by escaping
     * double quote characters. This prevents JSON parsing errors when strings contain
     * quotes that would otherwise break the JSON structure.
     * </p>
     * 
     * @param message the string to escape
     * @return the string with all double quotes escaped with backslashes
     */
    public String prepareJsonString(String message){
        return StringUtils.replace(message, "\"", "\\\"");
    }

    /**
     * Initializes a Flow pipeline populated with integration forms for an organization.
     * <p>
     * This method creates a Flow that prepares forms for all supported integrations
     * (Trello, GitHub, Slack, MS Teams, Jira, and Basecamp) and stores them in the
     * Flow's result map. Controllers use this Flow to render the integration configuration page.
     * </p>
     * 
     * @param organizationId the organization ID for which to prepare integration forms
     * @param request the HTTP servlet request (not currently used but available for future enhancements)
     * @return a Flow pipeline containing all integration forms in the result map
     * @see Flow
     * @see IntegrationPageAttributes
     */
    public Flow<Long, ?, ?> initFlowForOrganizationConfiguration(Long organizationId, HttpServletRequest request) {
        return Flow.init(organizationEntityId, organizationId)
                .thenSet(IntegrationPageAttributes.integrationTrelloForm, a -> integrationService.prepareTrelloFormForOrg(organizationId))
                .thenSet(IntegrationPageAttributes.integrationGitHubForm, a -> integrationService.prepareGitHubFormForOrg(organizationId))
                .thenSet(IntegrationPageAttributes.integrationSlackForm, a -> integrationService.prepareSlackFormForOrg(organizationId))
                .thenSet(IntegrationPageAttributes.integrationMsTeamsForm, a -> integrationService.prepareMsTeamsFormForOrg(organizationId))
                .thenSet(IntegrationPageAttributes.integrationJiraForm, a -> integrationService.prepareJiraFormForOrg(organizationId))
                .thenSet(IntegrationPageAttributes.integrationBasecampForm, a -> integrationService.prepareBasecampFormForOrg(organizationId));
    }

    /**
     * Registers notification consumers for all supported integrations.
     * <p>
     * This method registers event consumers that listen for {@link NotificationDto} events
     * and create corresponding items in external systems (Trello cards, GitHub issues,
     * Jira issues, Basecamp to-dos). Each consumer is registered with the application
     * event system and categorized as an INTEGRATION consumer.
     * </p>
     * <p>
     * Registered consumers:
     * </p>
     * <ul>
     *   <li>Trello - Creates cards on specified board and list</li>
     *   <li>GitHub - Creates issues in specified repository</li>
     *   <li>Jira - Creates issues in specified project</li>
     *   <li>Basecamp - Posts to-dos to specified to-do list</li>
     * </ul>
     * 
     * @see TrelloIntegrationConsumers
     * @see GitHubIntegrationConsumers
     * @see JiraIntegrationConsumers
     * @see BasecampIntegrationConsumers
     */
    private void registerConsumers() {
        services.applicationEvent.registerEventConsumerWithMethod(
                NotificationDto.class,
                TrelloIntegrationConsumers.class,
                "createTrelloCardFromOrgNotification",
                "This creates Cards on the specified List and Board on Trello based on the Notification.",
                EventConsumerCategory.INTEGRATION
        );
        services.applicationEvent.registerEventConsumerWithMethod(
                NotificationDto.class,
                GitHubIntegrationConsumers.class,
                "createGitHubIssueFromOrgNotification",
                "This creates Issue on the specified repository on GitHub, based on the Notification.",
                EventConsumerCategory.INTEGRATION
        );
        services.applicationEvent.registerEventConsumerWithMethod(
                NotificationDto.class,
                JiraIntegrationConsumers.class,
                "createJiraIssueFromOrgNotification",
                "This creates Issue on the specified project on JIRA, based on the Notification.",
                EventConsumerCategory.INTEGRATION
        );
        services.applicationEvent.registerEventConsumerWithMethod(
                NotificationDto.class,
                BasecampIntegrationConsumers.class,
                "postBasecampToDo",
                "This posts a To-Do to Basecamp To-Do List based on the Notification.",
                EventConsumerCategory.INTEGRATION
        );
    }
}
