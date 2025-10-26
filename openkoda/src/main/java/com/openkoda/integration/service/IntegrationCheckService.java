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

import com.openkoda.integration.controller.IntegrationComponentProvider;
import com.openkoda.integration.model.configuration.IntegrationModuleGlobalConfiguration;
import com.openkoda.integration.model.configuration.IntegrationModuleOrganizationConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Service for validating integration configurations and checking connectivity to external services.
 * <p>
 * Verifies OAuth tokens are valid, checks API connectivity, validates webhook URLs, and performs 
 * health checks. Uses Apache Commons Lang3 StringUtils (isNotBlank/isNotEmpty) for all validations.
 * This {@code @Component} is stateless, safe as a Spring singleton, and relied upon to avoid 
 * runtime integration operations when credentials or tokens are missing.
 * </p>
 * <p>
 * Provides synchronous, read-only boolean predicates used by controllers, UIs, and services to 
 * gate features. Methods fall into three categories:
 * </p>
 * <ul>
 *   <li>Global configuration checks: Verify OAuth client IDs and secrets are configured at the 
 *       application level (e.g., {@link #isJiraConfiguredGlobally()})</li>
 *   <li>Connection checks: Verify OAuth access tokens are present for an organization 
 *       (e.g., {@link #isGitHubConnected(Long)})</li>
 *   <li>Full integration checks: Verify complete configuration including tokens and required 
 *       fields for operation (e.g., {@link #isJiraIntegrated(Long)})</li>
 * </ul>
 * <p>
 * All methods query configuration entities via {@code integrationService.getGlobalConfiguration()} 
 * and {@code integrationService.getOrganizationConfiguration(orgId)}. No external API calls are 
 * made; validations are purely based on stored configuration data.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see IntegrationModuleGlobalConfiguration
 * @see IntegrationModuleOrganizationConfiguration
 */
@Component("integrationCheck")
public class IntegrationCheckService extends IntegrationComponentProvider {

    /**
     * Checks if Jira integration is configured globally with valid OAuth client credentials.
     * <p>
     * Fetches {@link IntegrationModuleGlobalConfiguration} and validates that both Jira client ID
     * and client secret are present and not blank. This check is required before initiating any
     * Jira OAuth flow.
     * </p>
     *
     * @return {@code true} if Jira client ID and secret are configured, {@code false} otherwise
     * @see #isJiraConnected(Long)
     * @see #isJiraIntegrated(Long)
     */
    public boolean isJiraConfiguredGlobally(){
        IntegrationModuleGlobalConfiguration globalConfig = integrationService.getGlobalConfiguration();
        String jiraClientId = globalConfig.jiraClientId;
        String jiraClientSecret = globalConfig.jiraClientSecret;
        return StringUtils.isNotBlank(jiraClientId) && StringUtils.isNotBlank(jiraClientSecret);
    }

    /**
     * Checks if Basecamp integration is configured globally with valid OAuth client credentials.
     * <p>
     * Fetches {@link IntegrationModuleGlobalConfiguration} and validates that both Basecamp client 
     * ID and client secret are present and not blank. This check is required before initiating any
     * Basecamp OAuth flow.
     * </p>
     *
     * @return {@code true} if Basecamp client ID and secret are configured, {@code false} otherwise
     * @see #isBasecampConnected(Long)
     * @see #isBasecampIntegrated(Long)
     */
    public boolean isBasecampConfiguredGlobally(){
        IntegrationModuleGlobalConfiguration globalConfig = integrationService.getGlobalConfiguration();
        String basecampClientId = globalConfig.basecampClientId;
        String basecampClientSecret = globalConfig.basecampClientSecret;
        return StringUtils.isNotBlank(basecampClientId) && StringUtils.isNotBlank(basecampClientSecret);
    }

    /**
     * Checks if GitHub integration is configured globally with valid OAuth client credentials.
     * <p>
     * Fetches {@link IntegrationModuleGlobalConfiguration} and validates that both GitHub client ID
     * and client secret are present and not blank. This check is required before initiating any
     * GitHub OAuth flow.
     * </p>
     *
     * @return {@code true} if GitHub client ID and secret are configured, {@code false} otherwise
     * @see #isGitHubConnected(Long)
     * @see #isGitHubIntegrated(Long)
     */
    public boolean isGitHubConfiguredGlobally(){
        IntegrationModuleGlobalConfiguration globalConfig = integrationService.getGlobalConfiguration();
        String gitHubClientId = globalConfig.gitHubClientId;
        String gitHubClientSecret = globalConfig.gitHubClientSecret;
        return StringUtils.isNotBlank(gitHubClientId) && StringUtils.isNotBlank(gitHubClientSecret);
    }

    /**
     * Checks if GitHub OAuth access token is present for the specified organization.
     * <p>
     * Verifies that a GitHub access token has been obtained and stored in the organization's
     * configuration. Does not validate token freshness or permissions. This indicates that the
     * OAuth flow has completed successfully at least once.
     * </p>
     *
     * @param orgId the organization ID to check for GitHub token presence
     * @return {@code true} if GitHub token exists and is not empty, {@code false} otherwise
     * @see #isGitHubConfiguredGlobally()
     * @see #isGitHubIntegrated(Long)
     */
    public boolean isGitHubConnected(Long orgId) {
        return StringUtils.isNotEmpty(integrationService.getOrganizationConfiguration(orgId).getGitHubToken());
    }

    /**
     * Checks if Jira OAuth access token is present for the specified organization.
     * <p>
     * Verifies that a Jira access token has been obtained and stored in the organization's
     * configuration. Does not validate token freshness or permissions. This indicates that the
     * OAuth flow has completed successfully at least once.
     * </p>
     *
     * @param orgId the organization ID to check for Jira token presence
     * @return {@code true} if Jira token exists and is not empty, {@code false} otherwise
     * @see #isJiraConfiguredGlobally()
     * @see #isJiraIntegrated(Long)
     */
    public boolean isJiraConnected(Long orgId) {
        return StringUtils.isNotEmpty(integrationService.getOrganizationConfiguration(orgId).getJiraToken());
    }

    /**
     * Checks if Basecamp OAuth access token is present for the specified organization.
     * <p>
     * Verifies that a Basecamp access token has been obtained and stored in the organization's
     * configuration. Does not validate token freshness or permissions. This indicates that the
     * OAuth flow has completed successfully at least once.
     * </p>
     *
     * @param orgId the organization ID to check for Basecamp token presence
     * @return {@code true} if Basecamp access token exists and is not empty, {@code false} otherwise
     * @see #isBasecampConfiguredGlobally()
     * @see #isBasecampIntegrated(Long)
     */
    public boolean isBasecampConnected(Long orgId) {
        return StringUtils.isNotEmpty(integrationService.getOrganizationConfiguration(orgId).getBasecampAccessToken());
    }

    /**
     * Checks if Trello integration is fully configured and operational for the specified organization.
     * <p>
     * Validates complete configuration including API key, API token, board name, and list name. All
     * four fields must be present and not blank for Trello operations to function. This is the most
     * comprehensive check for Trello readiness.
     * </p>
     *
     * @param orgId the organization ID to check for complete Trello configuration
     * @return {@code true} if all required Trello fields are configured, {@code false} otherwise
     * @see IntegrationModuleOrganizationConfiguration#getTrelloApiKey()
     * @see IntegrationModuleOrganizationConfiguration#getTrelloApiToken()
     */
    public boolean isTrelloIntegrated(Long orgId) {
        IntegrationModuleOrganizationConfiguration config = integrationService.getOrganizationConfiguration(orgId);
        return StringUtils.isNotBlank(config.getTrelloApiKey()) && StringUtils.isNotBlank(config.getTrelloApiToken())
                && StringUtils.isNotBlank(config.getTrelloBoardName()) && StringUtils.isNotBlank(config.getTrelloListName());
    }

    /**
     * Checks if GitHub integration is fully configured and operational for the specified organization.
     * <p>
     * Validates complete configuration including OAuth access token, repository name, and repository
     * owner. All three fields must be present and not blank for GitHub operations to function. This
     * is the most comprehensive check for GitHub readiness.
     * </p>
     *
     * @param orgId the organization ID to check for complete GitHub configuration
     * @return {@code true} if all required GitHub fields are configured, {@code false} otherwise
     * @see #isGitHubConnected(Long)
     * @see IntegrationModuleOrganizationConfiguration#getGitHubToken()
     */
    public boolean isGitHubIntegrated(Long orgId) {
        IntegrationModuleOrganizationConfiguration config = integrationService.getOrganizationConfiguration(orgId);
        return StringUtils.isNotBlank(config.getGitHubToken()) && StringUtils.isNotBlank(config.getGitHubRepoName())
                && StringUtils.isNotBlank(config.getGitHubRepoOwner());
    }

    /**
     * Checks if Slack integration is fully configured and operational for the specified organization.
     * <p>
     * Validates that a Slack webhook URL is present and not blank. The webhook URL is required for
     * sending notifications to Slack channels. This is the only required field for Slack integration.
     * </p>
     *
     * @param orgId the organization ID to check for Slack webhook configuration
     * @return {@code true} if Slack webhook URL is configured, {@code false} otherwise
     * @see IntegrationModuleOrganizationConfiguration#getSlackWebhookUrl()
     */
    public boolean isSlackIntegrated(Long orgId) {
        IntegrationModuleOrganizationConfiguration config = integrationService.getOrganizationConfiguration(orgId);
        return StringUtils.isNotBlank(config.getSlackWebhookUrl());
    }

    /**
     * Checks if Microsoft Teams integration is fully configured and operational for the specified organization.
     * <p>
     * Validates that a Microsoft Teams webhook URL is present and not blank. The webhook URL is
     * required for sending notifications to Teams channels. This is the only required field for
     * Microsoft Teams integration.
     * </p>
     *
     * @param orgId the organization ID to check for Microsoft Teams webhook configuration
     * @return {@code true} if Teams webhook URL is configured, {@code false} otherwise
     * @see IntegrationModuleOrganizationConfiguration#getMsTeamsWebhookUrl()
     */
    public boolean isMsTeamsIntegrated(Long orgId) {
        IntegrationModuleOrganizationConfiguration config = integrationService.getOrganizationConfiguration(orgId);
        return StringUtils.isNotBlank(config.getMsTeamsWebhookUrl());
    }

    /**
     * Checks if Jira integration is fully configured and operational for the specified organization.
     * <p>
     * Validates complete configuration including OAuth access token, organization name, and project
     * name. All three fields must be present and not blank for Jira operations to function. This is
     * the most comprehensive check for Jira readiness.
     * </p>
     *
     * @param orgId the organization ID to check for complete Jira configuration
     * @return {@code true} if all required Jira fields are configured, {@code false} otherwise
     * @see #isJiraConnected(Long)
     * @see IntegrationModuleOrganizationConfiguration#getJiraToken()
     */
    public boolean isJiraIntegrated(Long orgId) {
        IntegrationModuleOrganizationConfiguration config = integrationService.getOrganizationConfiguration(orgId);
        return StringUtils.isNotBlank(config.getJiraToken()) && StringUtils.isNotBlank(config.getJiraOrganizationName())
                && StringUtils.isNotBlank(config.getJiraProjectName());
    }

    /**
     * Checks if Basecamp integration is fully configured and operational for the specified organization.
     * <p>
     * Validates complete configuration including OAuth access token and to-do list URL. Both fields
     * must be present and not blank for Basecamp operations to function. This is the most
     * comprehensive check for Basecamp readiness.
     * </p>
     *
     * @param orgId the organization ID to check for complete Basecamp configuration
     * @return {@code true} if all required Basecamp fields are configured, {@code false} otherwise
     * @see #isBasecampConnected(Long)
     * @see IntegrationModuleOrganizationConfiguration#getBasecampAccessToken()
     */
    public boolean isBasecampIntegrated(Long orgId) {
        IntegrationModuleOrganizationConfiguration config = integrationService.getOrganizationConfiguration(orgId);
        return StringUtils.isNotBlank(config.getBasecampAccessToken()) && StringUtils.isNotBlank(config.getBasecampToDoListUrl());
    }
}
