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

import com.openkoda.controller.common.URLConstants;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.integration.controller.common.IntegrationURLConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Helper service for constructing OAuth callback URLs and module-specific URLs using configured base URL.
 * <p>
 * This component provides centralized URL construction for third-party integrations including
 * Trello, GitHub, Jira, Basecamp, Slack, and Microsoft Teams. Uses the base.url property to build
 * absolute URLs for OAuth redirects, ensuring callback URLs match OAuth app registrations.
 * </p>
 * <p>
 * The service generates three types of URLs for each integration:
 * <ul>
 * <li>Integration form URLs - Relative paths to integration configuration pages</li>
 * <li>Token access URLs - Absolute URLs for OAuth callback redirects</li>
 * <li>Disconnect URLs - Relative paths to disable integration endpoints</li>
 * </ul>
 * </p>
 * <p>
 * All URLs are organization-scoped to support multi-tenancy. This ensures integrations are
 * configured and managed separately for each organization in the system.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.integration.controller.common.IntegrationURLConstants
 * @see com.openkoda.controller.common.URLConstants
 */
@Component("integrationUrl")
public class IntegrationUrlHelper implements URLConstants, LoggingComponentWithRequestId {

    /**
     * Base URL for constructing absolute URLs for OAuth callbacks and external redirects.
     * <p>
     * This property is injected from application configuration (base.url) and defaults to
     * http://localhost:8080 for local development. In production environments, this should
     * be set to the publicly accessible application URL to ensure OAuth providers can
     * successfully redirect back to the application after authorization.
     * </p>
     * <p>
     * The base URL is prepended to relative paths when constructing token access URLs for
     * OAuth integrations. It must match the redirect URI registered with OAuth providers
     * (GitHub, Jira, Basecamp) for authentication to succeed.
     * </p>
     */
    @Value("${base.url:http://localhost:8080}")
    private String baseUrl;

//    INTEGRATION FORMS
    /**
     * Constructs the relative URL path to the Trello integration configuration form.
     * <p>
     * This URL points to the organization-specific settings page where users can configure
     * Trello integration parameters such as API keys, default boards, and webhook settings.
     * </p>
     *
     * @param orgId the organization identifier for organization-scoped integration settings
     * @return relative URL path to Trello integration form (e.g., /html/organization/123/integration/settings/trello)
     */
    public String trelloFormUrl(Long orgId) {
        debug("[trelloFormUrl]");
        return _HTML_ORGANIZATION + '/' + orgId + IntegrationURLConstants._INTEGRATION + _SETTINGS + IntegrationURLConstants._TRELLO;
    }

    /**
     * Constructs the relative URL path to the GitHub integration configuration form.
     * <p>
     * This URL points to the organization-specific settings page where users can configure
     * GitHub integration parameters including OAuth app credentials, repository access,
     * and webhook configurations for repository events.
     * </p>
     *
     * @param orgId the organization identifier for organization-scoped integration settings
     * @return relative URL path to GitHub integration form (e.g., /html/organization/123/integration/settings/github)
     */
    public String gitHubFormUrl(Long orgId) {
        debug("[gitHubFormUrl]");
        return _HTML_ORGANIZATION + '/' + orgId + IntegrationURLConstants._INTEGRATION + _SETTINGS + IntegrationURLConstants._GITHUB;
    }

    /**
     * Constructs the relative URL path to the Basecamp integration configuration form.
     * <p>
     * This URL points to the organization-specific settings page where users can configure
     * Basecamp integration parameters such as OAuth application credentials, default project
     * mappings, and notification preferences for Basecamp events.
     * </p>
     *
     * @param orgId the organization identifier for organization-scoped integration settings
     * @return relative URL path to Basecamp integration form (e.g., /html/organization/123/integration/settings/basecamp)
     */
    public String basecampFormUrl(Long orgId) {
        debug("[basecampFormUrl]");
        return _HTML_ORGANIZATION + '/' + orgId + IntegrationURLConstants._INTEGRATION + _SETTINGS + IntegrationURLConstants._BASECAMP;
    }

    /**
     * Constructs the relative URL path to the Jira integration configuration form.
     * <p>
     * This URL points to the organization-specific settings page where users can configure
     * Jira integration parameters including OAuth consumer credentials, Jira instance URL,
     * project mappings, and issue synchronization settings.
     * </p>
     *
     * @param orgId the organization identifier for organization-scoped integration settings
     * @return relative URL path to Jira integration form (e.g., /html/organization/123/integration/settings/jira)
     */
    public String jiraFormUrl(Long orgId) {
        debug("[gitHubFormUrl]");
        return _HTML_ORGANIZATION + '/' + orgId + IntegrationURLConstants._INTEGRATION + _SETTINGS + IntegrationURLConstants._JIRA;
    }

    /**
     * Constructs the relative URL path to the Slack integration configuration form.
     * <p>
     * This URL points to the organization-specific settings page where users can configure
     * Slack integration parameters such as OAuth app credentials, default channels for
     * notifications, bot token settings, and webhook configurations.
     * </p>
     *
     * @param orgId the organization identifier for organization-scoped integration settings
     * @return relative URL path to Slack integration form (e.g., /html/organization/123/integration/settings/slack)
     */
    public String slackFormUrl(Long orgId) {
        debug("[slackFormUrl]");
        return _HTML_ORGANIZATION + '/' + orgId + IntegrationURLConstants._INTEGRATION + _SETTINGS + IntegrationURLConstants._SLACK;
    }

    /**
     * Constructs the relative URL path to the Microsoft Teams integration configuration form.
     * <p>
     * This URL points to the organization-specific settings page where users can configure
     * Microsoft Teams integration parameters including app registration credentials, team
     * and channel mappings, and notification delivery preferences.
     * </p>
     *
     * @param orgId the organization identifier for organization-scoped integration settings
     * @return relative URL path to Microsoft Teams integration form (e.g., /html/organization/123/integration/settings/msteams)
     */
    public String msTeamsFormUrl(Long orgId) {
        debug("[msTeamsFormUrl]");
        return _HTML_ORGANIZATION + '/' + orgId + IntegrationURLConstants._INTEGRATION + _SETTINGS + IntegrationURLConstants._MSTEAMS;
    }

//    TOKEN ACCESS
    /**
     * Constructs the absolute URL for GitHub OAuth callback redirect.
     * <p>
     * This URL is used as the redirect_uri parameter during GitHub OAuth authorization flow.
     * After the user authorizes the application on GitHub, GitHub redirects back to this URL
     * with an authorization code that can be exchanged for an access token. This absolute URL
     * must match the callback URL registered in the GitHub OAuth app configuration.
     * </p>
     *
     * @param orgId the organization identifier for organization-scoped token storage
     * @return complete absolute URL for GitHub OAuth callback (e.g., http://example.com/html/organization/123/integration/github)
     */
    public String gitHubTokenAccessUrl(Long orgId) {
        debug("[gitHubTokenAccessUrl]");
        return baseUrl + _HTML_ORGANIZATION + '/' + orgId + IntegrationURLConstants._INTEGRATION + IntegrationURLConstants._GITHUB;
    }

    /**
     * Constructs the absolute URL for Jira OAuth callback redirect.
     * <p>
     * This URL is used as the callback URL during Jira OAuth 1.0a authorization flow. After
     * the user authorizes the application on Jira, Jira redirects back to this URL with an
     * OAuth verifier that can be used to obtain access tokens. This absolute URL must match
     * the callback URL registered in the Jira application link configuration.
     * </p>
     *
     * @param orgId the organization identifier for organization-scoped token storage
     * @return complete absolute URL for Jira OAuth callback (e.g., http://example.com/html/organization/123/integration/jira)
     */
    public String jiraTokenAccessUrl(Long orgId) {
        return baseUrl + _HTML_ORGANIZATION + '/' + orgId + IntegrationURLConstants._INTEGRATION + IntegrationURLConstants._JIRA;
    }

    /**
     * Constructs the absolute URL for Basecamp OAuth callback redirect.
     * <p>
     * This URL is used as the redirect_uri parameter during Basecamp OAuth 2.0 authorization
     * flow. After the user authorizes the application on Basecamp, Basecamp redirects back to
     * this URL with an authorization code that can be exchanged for an access token. This
     * absolute URL must match the redirect URI registered in the Basecamp OAuth app settings.
     * </p>
     *
     * @param orgId the organization identifier for organization-scoped token storage
     * @return complete absolute URL for Basecamp OAuth callback (e.g., http://example.com/html/organization/123/integration/basecamp)
     */
    public String basecampRedirectUrl(Long orgId) {
        debug("[basecampRedirectUrl]");
        return baseUrl + _HTML_ORGANIZATION + '/' + orgId + IntegrationURLConstants._INTEGRATION + IntegrationURLConstants._BASECAMP;
    }

//    DISABLE INTEGRATION
    /**
     * Constructs the relative URL path to disable Jira integration for an organization.
     * <p>
     * This URL points to the endpoint that handles disconnecting the Jira integration,
     * which typically involves revoking access tokens, clearing stored credentials, and
     * disabling webhook subscriptions for the specified organization.
     * </p>
     *
     * @param orgId the organization identifier for which to disable Jira integration
     * @return relative URL path to Jira disconnect endpoint (e.g., /html/organization/123/integration/settings/jira/disconnect)
     */
    public String jiraDisableUrl(Long orgId) {
        return _HTML_ORGANIZATION + "/" + orgId + IntegrationURLConstants._INTEGRATION + _SETTINGS + IntegrationURLConstants._JIRA + IntegrationURLConstants._DISCONNECT;
    }

    /**
     * Constructs the relative URL path to disable Basecamp integration for an organization.
     * <p>
     * This URL points to the endpoint that handles disconnecting the Basecamp integration,
     * which typically involves revoking OAuth access tokens, removing stored credentials,
     * and clearing project mappings for the specified organization.
     * </p>
     *
     * @param orgId the organization identifier for which to disable Basecamp integration
     * @return relative URL path to Basecamp disconnect endpoint (e.g., /html/organization/123/integration/settings/basecamp/disconnect)
     */
    public String basecampDisableUrl(Long orgId) {
        return _HTML_ORGANIZATION + "/" + orgId + IntegrationURLConstants._INTEGRATION + _SETTINGS + IntegrationURLConstants._BASECAMP + IntegrationURLConstants._DISCONNECT;
    }

    /**
     * Constructs the relative URL path to disable Trello integration for an organization.
     * <p>
     * This URL points to the endpoint that handles disconnecting the Trello integration,
     * which typically involves revoking API tokens, clearing stored credentials, removing
     * webhook subscriptions, and clearing board mappings for the specified organization.
     * </p>
     *
     * @param orgId the organization identifier for which to disable Trello integration
     * @return relative URL path to Trello disconnect endpoint (e.g., /html/organization/123/integration/settings/trello/disconnect)
     */
    public String trelloDisableUrl(Long orgId) {
        return _HTML_ORGANIZATION + "/" + orgId + IntegrationURLConstants._INTEGRATION + _SETTINGS + IntegrationURLConstants._TRELLO + IntegrationURLConstants._DISCONNECT;
    }

    /**
     * Constructs the relative URL path to disable GitHub integration for an organization.
     * <p>
     * This URL points to the endpoint that handles disconnecting the GitHub integration,
     * which typically involves revoking OAuth access tokens, removing stored credentials,
     * disabling repository webhooks, and clearing repository access for the specified organization.
     * </p>
     *
     * @param orgId the organization identifier for which to disable GitHub integration
     * @return relative URL path to GitHub disconnect endpoint (e.g., /html/organization/123/integration/settings/github/disconnect)
     */
    public String gitHubDisableUrl(Long orgId) {
        return _HTML_ORGANIZATION + "/" + orgId + IntegrationURLConstants._INTEGRATION + _SETTINGS + IntegrationURLConstants._GITHUB + IntegrationURLConstants._DISCONNECT;
    }

    /**
     * Constructs the relative URL path to disable Microsoft Teams integration for an organization.
     * <p>
     * This URL points to the endpoint that handles disconnecting the Microsoft Teams integration,
     * which typically involves revoking app registration tokens, clearing stored credentials,
     * removing team and channel mappings for the specified organization.
     * </p>
     *
     * @param orgId the organization identifier for which to disable Microsoft Teams integration
     * @return relative URL path to Microsoft Teams disconnect endpoint (e.g., /html/organization/123/integration/settings/msteams/disconnect)
     */
    public String msTeamsDisableUrl(Long orgId) {
        return _HTML_ORGANIZATION + "/" + orgId + IntegrationURLConstants._INTEGRATION + _SETTINGS + IntegrationURLConstants._MSTEAMS + IntegrationURLConstants._DISCONNECT;
    }

    /**
     * Constructs the relative URL path to disable Slack integration for an organization.
     * <p>
     * This URL points to the endpoint that handles disconnecting the Slack integration,
     * which typically involves revoking OAuth bot tokens, removing stored credentials,
     * clearing channel mappings, and disabling webhook configurations for the specified organization.
     * </p>
     *
     * @param orgId the organization identifier for which to disable Slack integration
     * @return relative URL path to Slack disconnect endpoint (e.g., /html/organization/123/integration/settings/slack/disconnect)
     */
    public String slackDisableUrl(Long orgId) {
        return _HTML_ORGANIZATION + "/" + orgId + IntegrationURLConstants._INTEGRATION + _SETTINGS + IntegrationURLConstants._SLACK + IntegrationURLConstants._DISCONNECT;
    }

}
