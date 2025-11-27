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

package com.openkoda.integration.model;

/**
 * String constants for integration privilege tokens used in {@code @PreAuthorize} annotations and security checks.
 * <p>
 * This interface defines privilege name constants that control access to third-party integration features
 * within the OpenKoda platform. Each constant represents a permission token that is evaluated during
 * runtime security checks to determine if a user or organization has authorization to access specific
 * integration endpoints and functionality.

 * <p>
 * These constants are designed for compile-time inlining in security expressions. When used in Spring
 * Security's {@code @PreAuthorize} annotations, they ensure type-safe and consistent privilege checking
 * across integration controllers and services. The underscore prefix naming convention distinguishes
 * these static final String constants from method-level security expressions.

 * <p>
 * Example usage in security contexts:
 * <pre>{@code
 * @PreAuthorize("hasAuthority(T(com.openkoda.integration.model.IntegrationPrivilegeName)._canIntegrateWithSlack)")
 * public ResponseEntity<String> slackCallback() { ... }
 * }</pre>

 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2019-10-02
 * @see com.openkoda.model.PrivilegeNames
 */
public interface IntegrationPrivilegeName {

    /**
     * Privilege token for Slack integration access.
     * <p>
     * Controls authorization to configure Slack integrations, send messages to Slack channels,
     * and handle Slack webhook callbacks. Users or organizations must possess this privilege
     * to access Slack integration endpoints and perform Slack-related operations.

     *
     * @see com.openkoda.integration.controller.IntegrationControllerHtml
     */
    String _canIntegrateWithSlack = "canIntegrateWithSlack";

    /**
     * Privilege token for Microsoft Teams integration access.
     * <p>
     * Controls authorization to configure Microsoft Teams integrations, post messages to Teams channels,
     * and manage Teams webhook connections. This privilege is required for all Microsoft Teams
     * integration functionality within the platform.

     *
     * @see com.openkoda.integration.controller.IntegrationControllerHtml
     */
    String _canIntegrateWithMsTeams = "canIntegrateWithMsTeams";

    /**
     * Privilege token for GitHub integration access.
     * <p>
     * Controls authorization to configure GitHub OAuth integrations, access GitHub repositories,
     * and consume GitHub REST API endpoints. Users must have this privilege to authenticate with
     * GitHub and perform repository-related operations.

     *
     * @see com.openkoda.integration.consumer.GitHubIntegrationConsumers
     */
    String _canIntegrateWithGitHub = "canIntegrateWithGitHub";

    /**
     * Privilege token for Jira integration access.
     * <p>
     * Controls authorization to configure Jira integrations, create and update Jira issues,
     * and query Jira project data. This privilege enables access to Jira REST API consumer
     * functionality and OAuth callback handling.

     *
     * @see com.openkoda.integration.consumer.JiraIntegrationConsumers
     */
    String _canIntegrateWithJira = "canIntegrateWithJira";

    /**
     * Privilege token for Basecamp integration access.
     * <p>
     * Controls authorization to configure Basecamp integrations, access Basecamp projects and to-dos,
     * and manage Basecamp OAuth authentication. Users with this privilege can interact with
     * Basecamp REST API endpoints through the platform.

     *
     * @see com.openkoda.integration.consumer.BasecampIntegrationConsumers
     */
    String _canIntegrateWithBasecamp = "canIntegrateWithBasecamp";

    /**
     * Privilege token for Trello integration access.
     * <p>
     * Controls authorization to configure Trello integrations, manage Trello boards and cards,
     * and handle Trello OAuth flows. This privilege is required to access Trello consumer
     * functionality and perform board management operations.

     *
     * @see com.openkoda.integration.consumer.TrelloIntegrationConsumers
     */
    String _canIntegrateWithTrello = "canIntegrateWithTrello";

}
