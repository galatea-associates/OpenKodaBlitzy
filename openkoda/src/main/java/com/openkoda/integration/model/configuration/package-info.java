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
 * Integration configuration models for third-party service integrations.
 * <p>
 * This package provides configuration management at two levels: global settings from
 * application.properties and per-organization settings from database storage. These models
 * enable both system-wide and tenant-specific integration credentials and settings.
 * 
 *
 * <b>Key Classes</b>
 * <ul>
 *   <li><b>IntegrationModuleGlobalConfiguration</b> - Spring-managed configuration bean that
 *       loads global integration settings from application.properties. This provides default
 *       values and system-wide credentials for integrations like OAuth providers, API keys,
 *       and service endpoints.</li>
 *   <li><b>IntegrationModuleOrganizationConfiguration</b> - JPA entity that stores
 *       tenant-scoped integration credentials in the database. Each organization can maintain
 *       its own API keys, OAuth client IDs, and custom integration parameters, enabling
 *       multi-tenancy support for third-party services.</li>
 * </ul>
 *
 * <b>Usage Context</b>
 * <p>
 * These configuration models are used throughout the integration subsystem to manage
 * credentials and settings for:
 * 
 * <ul>
 *   <li>OAuth authentication flows with providers like GitHub, Google, and Microsoft</li>
 *   <li>Third-party service integrations including Trello, Jira, and Basecamp</li>
 *   <li>REST API consumers requiring authentication tokens or API keys</li>
 *   <li>Per-organization customization of integration behavior and credentials</li>
 * </ul>
 * <p>
 * Global configurations provide fallback defaults, while organization-specific configurations
 * override these values when tenant-scoped credentials are required. This two-tier approach
 * supports both single-tenant and multi-tenant deployment scenarios.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.integration
 */
package com.openkoda.integration.model.configuration;