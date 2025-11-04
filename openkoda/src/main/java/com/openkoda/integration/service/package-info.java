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
 * Provides service layer components for managing third-party integrations within OpenKoda.
 * <p>
 * This package contains business logic services that orchestrate integration workflows with
 * external systems such as Trello, GitHub, Jira, Basecamp, and OAuth authentication providers.
 * Services handle OAuth token lifecycle management, API connectivity validation, push notification
 * delivery, and configuration persistence with organization-level scoping.
 * 
 *
 * <b>Key Services</b>
 * <ul>
 *   <li><b>IntegrationService</b>: Central facade providing unified access to all integration operations.
 *       Acts as the main entry point for controller layer to interact with external systems.</li>
 *   <li><b>IntegrationUrlHelper</b>: Constructs properly formatted URLs for OAuth authorization flows,
 *       API endpoints, and webhook callbacks with organization-specific parameters.</li>
 *   <li><b>IntegrationCheckService</b>: Validates external API connectivity, credential correctness,
 *       and network reachability before executing integration workflows.</li>
 *   <li><b>PushNotificationService</b>: Delivers push notifications to mobile devices and external
 *       notification services with error handling and retry logic.</li>
 *   <li><b>IntegrationDataLoaderService</b>: Bootstraps integration configuration data during
 *       application startup, loading default OAuth client configurations and API settings.</li>
 * </ul>
 *
 * <b>Service Responsibilities</b>
 * <p>
 * Services in this package coordinate between REST consumers (in the consumer package),
 * configuration entities (in the model package), and authorization controllers (in the controller package).
 * They implement business rules for token refresh, error recovery, and multi-tenant configuration
 * isolation.
 * 
 *
 * <b>Design Patterns</b>
 * <ul>
 *   <li><b>Service Facade</b>: IntegrationService acts as a facade with delegated responsibilities
 *       to specialized services for URL construction, validation, and notification delivery.</li>
 *   <li><b>OAuth Token Refresh</b>: Services automatically detect 401 authentication errors from
 *       external APIs and trigger OAuth token refresh flows before retrying failed requests.</li>
 *   <li><b>Per-Organization Configuration</b>: All integration configurations are scoped to specific
 *       organizations, ensuring tenant isolation and allowing different API credentials per tenant.</li>
 * </ul>
 *
 * <b>OAuth Orchestration</b>
 * <p>
 * Services manage the complete OAuth 2.0 authorization flow including authorization URL generation,
 * callback processing, token exchange with external providers, secure token storage, and automatic
 * refresh when tokens expire. Token lifecycle management ensures users maintain seamless access
 * to integrated external services without manual re-authentication.
 * 
 *
 * <b>Configuration Persistence</b>
 * <p>
 * Integration configurations are persisted using entities from the model package. Each organization
 * can maintain separate API credentials, webhook endpoints, and feature flags. Services validate
 * configuration completeness before allowing integration activation and provide clear error messages
 * for missing or invalid settings.
 * 
 *
 * <b>Notification Routing</b>
 * <p>
 * The PushNotificationService routes notifications to appropriate delivery channels based on user
 * preferences and device registrations. It supports multiple notification providers and handles
 * delivery failures with configurable retry policies.
 * 
 *
 * <b>External API Connectivity Validation</b>
 * <p>
 * Before executing integration workflows, IntegrationCheckService verifies network connectivity,
 * API endpoint availability, and credential validity. This prevents unnecessary API calls with
 * invalid configurations and provides early feedback to administrators during integration setup.
 * 
 *
 * <b>Usage Example</b>
 * <pre>
 * IntegrationService integrationService = ...;
 * String authUrl = integrationService.buildAuthorizationUrl(organizationId, "github");
 * </pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.integration.controller OAuth callback controllers
 * @see com.openkoda.integration.consumer REST API consumers for external services
 * @see com.openkoda.integration.model Integration configuration entities
 */
package com.openkoda.integration.service;