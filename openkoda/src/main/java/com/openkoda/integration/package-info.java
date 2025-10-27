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

/**
 * Third-party service integration module providing organization-scoped integrations with Trello, GitHub, Jira, Basecamp, MS Teams, and Slack.
 * <p>
 * This module enables multi-tenant applications to connect with external collaboration and project management platforms.
 * Each organization can configure its own integration settings, OAuth credentials, and notification forwarding rules.
 * The module handles the complete integration lifecycle from OAuth authorization to API interaction and token management.
 * </p>
 *
 * <h2>Core Capabilities</h2>
 * <ul>
 *   <li><b>OAuth 2.0 Authentication</b>: Implements complete OAuth 2.0 authorization code flow for supported providers.
 *       Controllers handle callback endpoints, exchange authorization codes for access tokens, and persist tokens securely.</li>
 *   <li><b>Per-Organization Configuration</b>: Each organization maintains isolated configuration via {@code IntegrationModuleOrganizationConfiguration} entity.
 *       Settings include API keys, OAuth tokens, project identifiers, and provider-specific parameters stored in the database.</li>
 *   <li><b>REST API Consumers</b>: Stateless service components use Spring {@code RestTemplate} to interact with external APIs.
 *       Consumers handle JSON payload construction, HTTP header management, and response parsing for each provider.</li>
 *   <li><b>Admin UI Controllers</b>: Spring MVC controllers provide organization-scoped integration setup interfaces.
 *       Administrators configure API credentials, authorize OAuth applications, and test connectivity through web forms.</li>
 *   <li><b>Notification Forwarding</b>: Integration consumers listen for internal notification events and forward them to external services.
 *       Notifications are filtered by organization scope and propagation rules before delivery.</li>
 * </ul>
 *
 * <h2>Module Structure</h2>
 * <ul>
 *   <li><b>consumer/</b>: API client implementations for each third-party provider. Classes convert internal notification
 *       DTOs to provider-specific API calls (Trello cards, GitHub issues, Jira tickets, Basecamp todos, Teams messages, Slack posts).</li>
 *   <li><b>controller/</b>: OAuth callback handlers and admin UI controllers. Manages authorization flows, token exchanges,
 *       configuration form submission, and integration disconnection. Uses Flow pipeline pattern for request handling.</li>
 *   <li><b>form/</b>: Form adapters and frontend mapping definitions. Validates user input, maps form fields to entity properties,
 *       and enforces provider-specific constraints (API key formats, URL patterns, required fields).</li>
 *   <li><b>model/</b>: Entity classes and DTOs for integration configuration. Includes global configuration (OAuth client IDs/secrets),
 *       per-organization settings, privilege definitions, and data transfer objects for form binding.</li>
 *   <li><b>service/</b>: Business logic orchestration for integration operations. Central facade registers notification consumers,
 *       performs OAuth token exchanges, manages token refresh, constructs callback URLs, and handles API errors.</li>
 * </ul>
 *
 * <h2>Architecture Pattern</h2>
 * <p>
 * The module follows a <b>per-organization configuration with OAuth token management</b> pattern:
 * </p>
 * <ol>
 *   <li>Global configuration defines OAuth client credentials via Spring properties ({@code module.integration.github.client.id}, etc.)</li>
 *   <li>Organization administrators authorize the application through OAuth provider consent screens</li>
 *   <li>OAuth callbacks exchange authorization codes for access tokens, which are encrypted and stored per organization</li>
 *   <li>Notification consumers load organization-specific configuration, validate tokens, and make authenticated API calls</li>
 *   <li>Token refresh logic handles expiration automatically for providers supporting refresh tokens (GitHub, Jira)</li>
 *   <li>Error handling delegates to central service methods for consistent retry and logging behavior</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <p>
 * To enable GitHub integration for an organization:
 * </p>
 * <pre>
 * // 1. Register OAuth app with GitHub, obtain client ID and secret
 * // 2. Configure in application.properties:
 * module.integration.github.client.id=your_client_id
 * module.integration.github.client.secret=your_client_secret
 *
 * // 3. Administrator connects via admin UI at /html/organization/{orgId}/integrations
 * // 4. User authorizes, callback processes token exchange
 * // 5. Notifications automatically forward to GitHub as issues:
 * services.notification.createOrganizationNotification(orgId, "Bug found", NotificationDto);
 * // GitHub issue created via GitHubConsumer if organization has valid config
 * </pre>
 *
 * <h2>Common Pitfalls</h2>
 * <ul>
 *   <li><b>Callback URL Mismatch</b>: OAuth callback URLs must match the OAuth app registration exactly, including protocol,
 *       domain, port, and path. Mismatches cause authorization failures. Verify {@code base.url} property matches your deployment.</li>
 *   <li><b>Token Expiration</b>: API tokens expire and require refresh logic. Consumers must implement retry-on-401 patterns
 *       and call appropriate refresh methods (Basecamp: {@code integrationService.refreshBasecampToken}, Jira: OAuth refresh flow).
 *       Failure to refresh results in silent integration failures.</li>
 *   <li><b>Configuration Validation</b>: Always validate configuration values before API calls. Missing or malformed settings
 *       (repository names, project IDs, webhook URLs) cause runtime exceptions. Use form validators and null checks.</li>
 *   <li><b>Organization Scope</b>: Integration consumers must respect organization boundaries. Always verify notification scope
 *       with {@code services.notification.isOrganization(notification)} before loading configuration and making external calls.</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.integration.model.configuration.IntegrationModuleOrganizationConfiguration
 * @see com.openkoda.integration.service.IntegrationService
 */
package com.openkoda.integration;