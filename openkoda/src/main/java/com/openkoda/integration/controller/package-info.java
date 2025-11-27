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
 * Spring MVC controllers implementing integration module admin UI and OAuth callback endpoints.
 * Handles organization-scoped integration configuration, OAuth authorization flows (GitHub, Jira, 
 * Basecamp, Trello, Slack, Microsoft Teams), and disconnect operations.
 * <p>
 * <b>Architecture Overview:</b>
 * Controllers extend {@link IntegrationComponentProvider} for service access, use Flow-based 
 * request handling, enforce @PreAuthorize security checks, bind @Valid DTOs with BindingResult,
 * and persist via repositories.unsecure.integration.

 * <p>
 * <b>OAuth Authorization Flow:</b>
 * <ol>
 *   <li>User initiates connection from organization settings page</li>
 *   <li>Redirect to external provider (GitHub/Jira/Basecamp) authorization URL with client_id, redirect_uri</li>
 *   <li>User grants permissions on provider's consent screen</li>
 *   <li>Provider redirects to callback endpoint with authorization code and state parameter</li>
 *   <li>Controller receives code, validates state for CSRF protection</li>
 *   <li>IntegrationService exchanges code for access token via provider's token endpoint</li>
 *   <li>Access token stored in IntegrationModuleOrganizationConfiguration entity</li>
 *   <li>User redirected to integration settings page with success message</li>
 * </ol>

 * <p>
 * <b>Key Endpoints:</b>
 * <ul>
 *   <li>GET /organization/{organizationId}/integration/settings - Display integration configuration UI</li>
 *   <li>POST /organization/{organizationId}/integration/settings/{provider} - Save provider configuration</li>
 *   <li>GET /organization/{organizationId}/integration/{provider} - OAuth callback endpoints (GitHub, Jira, Basecamp)</li>
 *   <li>GET /organization/{organizationId}/integration/settings/{provider}/disconnect - Remove provider configuration</li>
 * </ul>

 * <p>
 * <b>Subpackage:</b>
 * {@link com.openkoda.integration.controller.common} - URL constants and page attributes for typed model binding

 * <p>
 * <b>Security:</b>
 * All configuration endpoints require CHECK_CAN_MANAGE_ORG_DATA privilege via @PreAuthorize.
 * OAuth state parameter provides CSRF protection for callback flows.

 * <p>
 * <b>Key Classes:</b>
 * <ul>
 *   <li>{@link IntegrationComponentProvider} - Abstract base providing injected integration services</li>
 *   <li>{@link IntegrationControllerHtml} - Main controller implementing all integration endpoints</li>
 * </ul>

 *
 * @see com.openkoda.integration.service.IntegrationService
 * @see com.openkoda.integration.controller.common.IntegrationURLConstants
 * @see com.openkoda.integration.controller.common.IntegrationPageAttributes
 * @see com.openkoda.core.flow.Flow
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
package com.openkoda.integration.controller;