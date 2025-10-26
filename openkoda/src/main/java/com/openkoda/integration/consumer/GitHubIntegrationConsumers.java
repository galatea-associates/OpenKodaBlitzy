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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * GitHub REST API consumer for creating issues in GitHub repositories.
 * <p>
 * This service converts OpenKoda notifications to GitHub issues using the GitHub Issues API.
 * It provides OAuth access token authentication and supports per-organization repository
 * configuration. The consumer creates issues via POST requests to the GitHub API endpoint
 * {@code https://api.github.com/repos/{owner}/{repo}/issues}.
 * </p>
 * <p>
 * Repository owner and name are configured per organization through
 * {@link IntegrationModuleOrganizationConfiguration}. The service verifies notification
 * scope (organization-level only), respects the propagate flag, and retrieves per-organization
 * configuration via {@code integrationService.getInnerOrganizationConfig()}.
 * </p>
 * <p>
 * Architecture: Stateless Spring service bean that delegates to RestTemplate for HTTP
 * communication. Handles GitHub API responses including error codes for invalid tokens (401),
 * insufficient permissions (403), repository not found (404), and rate limiting (429).
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * NotificationDto notification = new NotificationDto();
 * gitHubConsumer.createGitHubIssueFromOrgNotification(notification);
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see IntegrationModuleOrganizationConfiguration
 * @see NotificationDto
 */
@Service
public class GitHubIntegrationConsumers extends IntegrationComponentProvider {

    /**
     * GitHub API endpoint template for creating issues.
     * <p>
     * Template URL with placeholders for repository owner and repository name.
     * Format: {@code https://api.github.com/repos/%s/%s/issues} where first placeholder
     * is owner and second is repository name. Configurable via Spring property
     * {@code api.github.create.issue}.
     * </p>
     */
    @Value("${api.github.create.issue:https://api.github.com/repos/%s/%s/issues}")
    private String GITHUB_CREATE_ISSUE_API;
    
    /**
     * Internationalization message key for GitHub issue title.
     * <p>
     * Message key {@code notification.github.title} used to retrieve localized
     * issue title from message bundles.
     * </p>
     */
    private final String GITHUB_ISSUE_TITLE = "notification.github.title";
    
    /**
     * RestTemplate instance for making HTTP requests to GitHub API.
     */
    private RestTemplate restTemplate = new RestTemplate();
    
    /**
     * JSON template for GitHub issue creation request payload.
     * <p>
     * Template structure: {@code {"title":"%s","body":"%s"}} where first placeholder
     * is issue title and second is issue body content. GitHub API accepts additional
     * optional fields like labels, assignees, and milestone.
     * </p>
     */
    private static final String GITHUB_ISSUE_JSON = "{" +
            "\"title\":\"%s\"," +
            "\"body\":\"%s\"" +
            "}";

    /**
     * Creates a GitHub issue from an organization notification.
     * <p>
     * This method converts an OpenKoda organization-level notification into a GitHub issue
     * by posting to the configured repository's Issues API. The workflow includes scope
     * verification, configuration validation, URL formatting, header preparation, message
     * sanitization, HTTP POST execution, and error handling.
     * </p>
     * <p>
     * Required configuration fields (retrieved from {@link IntegrationModuleOrganizationConfiguration}):
     * </p>
     * <ul>
     *   <li>{@code gitHubRepoOwner} - GitHub repository owner username or organization</li>
     *   <li>{@code gitHubRepoName} - GitHub repository name</li>
     *   <li>{@code gitHubAccessToken} - OAuth access token for authentication</li>
     * </ul>
     * <p>
     * JSON payload structure sent to GitHub API:
     * <pre>{@code
     * {
     *   "title": "Localized notification title",
     *   "body": "Sanitized notification message"
     * }
     * }</pre>
     * </p>
     * <p>
     * The method respects the notification's propagate flag and only processes
     * organization-scoped notifications. If the notification is not organizational
     * or propagate is false, the method returns early without creating an issue.
     * </p>
     * <p>
     * Error handling via {@code handleResponseError()} covers:
     * </p>
     * <ul>
     *   <li>401 Unauthorized - Invalid or expired GitHub access token</li>
     *   <li>403 Forbidden - Insufficient permissions to create issues</li>
     *   <li>404 Not Found - Repository does not exist or is inaccessible</li>
     *   <li>429 Too Many Requests - GitHub API rate limit exceeded (5000 requests/hour for authenticated users)</li>
     * </ul>
     * <p>
     * Rate limiting note: GitHub API has rate limits of 5000 requests per hour for
     * authenticated requests. This consumer handles 429 responses through the error handler.
     * </p>
     *
     * @param notification the notification DTO containing title and body for the GitHub issue.
     *                     Must be organization-scoped with propagate flag set to true.
     * @throws Exception if GitHub API request fails, including {@code RestClientException}
     *                   for network errors, authentication failures, or rate limiting
     * @see IntegrationModuleOrganizationConfiguration
     * @see NotificationDto
     */
    public void createGitHubIssueFromOrgNotification(NotificationDto notification) throws Exception {
        debug("[createGitHubIssueFromOrgNotification]");
        if (!services.notification.isOrganization(notification)) {
            info("[createGitHubIssueFromOrgNotification] Notification is not organizational.");
            return;
        }
        if(!notification.getPropagate()){
            return;
        }
        IntegrationModuleOrganizationConfiguration integrationConfiguration
                = integrationService.getInnerOrganizationConfig(notification.getOrganizationId());
        String repoName = integrationConfiguration.getGitHubRepoName();
        String repoOwner = integrationConfiguration.getGitHubRepoOwner();
        if (StringUtils.isBlank(repoName) || StringUtils.isBlank(repoOwner)) {
            warn("[createGitHubIssueFromOrgNotification] GitHub owner or repo not introduced");
            return;
        }
        String url = String.format(GITHUB_CREATE_ISSUE_API, repoOwner, repoName);
        HttpHeaders headers = prepareAuthorizationHeader(integrationConfiguration.getGitHubToken());
        String message = integrationService.prepareJsonString(notification.getMessage());
        String issueRequest = String.format(GITHUB_ISSUE_JSON, messages.get(GITHUB_ISSUE_TITLE), message);
        HttpEntity<String> entity = new HttpEntity<>(issueRequest, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        integrationService.handleResponseError(response, "[createGitHubIssueFromOrgNotification] Error while creating new Issue. Code: {}. Error: {}");
    }

    /**
     * Prepares HTTP Authorization header for GitHub API authentication.
     * <p>
     * Constructs an {@link HttpHeaders} object with the Authorization header set to
     * GitHub's required format: {@code "token <access_token>"}. GitHub uses the
     * "token" prefix (not "Bearer") for OAuth token authentication.
     * </p>
     * <p>
     * Example header value: {@code "token ghp_abc123xyz456"}
     * </p>
     *
     * @param token the GitHub OAuth access token (personal access token or OAuth app token)
     * @return HttpHeaders instance with Authorization header configured for GitHub API
     * @see HttpHeaders#AUTHORIZATION
     */
    private HttpHeaders prepareAuthorizationHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "token " + token);
        return headers;
    }
}
