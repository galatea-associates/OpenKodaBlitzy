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
 * Integration module domain model containing privilege definitions, configuration entities, and DTOs.
 * <p>
 * This package provides the core domain model for external service integrations including Slack, Microsoft Teams,
 * GitHub, Jira, Basecamp, and Trello. It defines role-based access control privileges, per-organization
 * integration settings storage, and OAuth token management.
 * </p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link com.openkoda.integration.model.IntegrationPrivilege} - Enumeration defining six integration
 *       privileges with numeric IDs (offset 1000) for role-based access control. Each privilege controls
 *       access to a specific external integration service.</li>
 *   <li>{@link com.openkoda.integration.model.IntegrationPrivilegeName} - Interface providing string constants
 *       for privilege tokens used in {@code @PreAuthorize} annotations and security checks. Constants are
 *       compile-time inlined for optimal performance.</li>
 * </ul>
 *
 * <h2>Subpackages</h2>
 * <ul>
 *   <li>{@code configuration} - Contains global and organization-scoped configuration entities.
 *       Stores OAuth client credentials, webhook URLs, and API tokens for each integration service.
 *       Configuration is managed per organization for multi-tenant support.</li>
 *   <li>{@code dto} - Data transfer objects for form binding and controller/service layer communication.
 *       Provides lightweight POJOs for integration settings (Slack, MS Teams, GitHub, Jira, Basecamp, Trello)
 *       with Jackson-friendly serialization.</li>
 * </ul>
 *
 * <h2>Usage Patterns</h2>
 * <p>
 * Privilege tokens are used in security checks throughout the integration module:
 * </p>
 * <pre>{@code
 * @PreAuthorize("hasAuthority(T(IntegrationPrivilegeName)._canIntegrateWithSlack)")
 * public void configureSlack(SlackConfig config) { ... }
 * }</pre>
 * <p>
 * Configuration entities are persisted per organization for tenant isolation:
 * </p>
 * <pre>{@code
 * IntegrationModuleOrganizationConfiguration config = repository.findByOrganizationId(orgId);
 * config.setGithubClientId("oauth-client-id");
 * }</pre>
 * <p>
 * DTOs facilitate data transfer between controller and service layers:
 * </p>
 * <pre>{@code
 * IntegrationSlackDto dto = new IntegrationSlackDto();
 * dto.setWebhookUrl("https://hooks.slack.com/...");
 * }</pre>
 *
 * <h2>Security Considerations</h2>
 * <p>
 * Integration credentials and OAuth tokens are stored as plain strings in configuration entities.
 * Ensure proper encryption at rest and secure transmission using HTTPS for production deployments.
 * Privilege checks prevent unauthorized access to integration configuration endpoints.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.integration.model.IntegrationPrivilege
 * @see com.openkoda.integration.model.IntegrationPrivilegeName
 */
package com.openkoda.integration.model;