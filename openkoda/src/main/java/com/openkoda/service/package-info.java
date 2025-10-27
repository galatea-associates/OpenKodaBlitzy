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
 * Business logic orchestration and service layer for OpenKoda platform.
 * <p>
 * Provides Spring-managed service beans implementing business logic, orchestration workflows, and integration with external systems.
 * This package serves as the application's service layer, coordinating between controllers (presentation), repositories (data access),
 * and core framework components.
 * </p>
 * <p>
 * The service layer is organized into functional subpackages:
 * </p>
 * <h3>Core Service Capabilities</h3>
 * <ul>
 * <li><b>Services.java</b>: Central aggregator exposing 50+ service beans as injectable fields for legacy code compatibility</li>
 * <li><b>export/</b>: Component export/import orchestration using YAML serialization and ZIP packaging</li>
 * <li><b>dynamicentity/</b>: Runtime JPA entity generation using Byte Buddy, enabling dynamic data model creation</li>
 * <li><b>openai/</b>: ChatGPT integration for AI-powered features with conversation management and prompt templating</li>
 * <li><b>organization/</b>: Multi-tenant organization provisioning, lifecycle management, and tenant-scoped operations</li>
 * <li><b>user/</b>: User account management, authentication tokens, API keys, roles, and privileges</li>
 * <li><b>upgrade/</b>: Database schema migration orchestration with version tracking and transactional execution</li>
 * </ul>
 * <h3>Supporting Services</h3>
 * <ul>
 * <li><b>map/</b>: Geospatial WKT POINT parsing via JTS topology library</li>
 * <li><b>notification/</b>: Application notification lifecycle and delivery management</li>
 * <li><b>role/</b>: Role assignment reconciliation using server-side JavaScript evaluation</li>
 * <li><b>csv/</b>: CSV file assembly utilities for data export</li>
 * <li><b>autocomplete/</b>: Reflection-based autocomplete for forms and web endpoints</li>
 * <li><b>captcha/</b>: reCAPTCHA verification for bot protection</li>
 * </ul>
 * <h3>Architectural Patterns</h3>
 * <p>
 * All services follow Spring dependency injection patterns with {@code @Service} or {@code @Component} stereotypes.
 * Services use {@code @Transactional} boundaries for data consistency, inject secure/unsecure repositories for data access,
 * and leverage core framework components (Flow pipelines, MultitenancyService, LoggingComponent) for cross-cutting concerns.
 * </p>
 * <h3>Thread Safety</h3>
 * <p>
 * Most services are stateless and thread-safe. Notable exceptions: DynamicEntityDescriptorFactory uses unsynchronized
 * static registries (external synchronization required), ChatGPTService uses synchronized disk cache writes,
 * RoleModificationsConsumers temporarily switches authentication context (not thread-safe during execution).
 * </p>
 * <h3>Dependencies</h3>
 * <p>
 * This package depends on:
 * </p>
 * <ul>
 * <li>com.openkoda.core: Foundation framework (Flow, security, caching, auditing)</li>
 * <li>com.openkoda.model: JPA entities and domain model</li>
 * <li>com.openkoda.repository: Data access via Spring Data JPA</li>
 * <li>Spring Framework: {@code @Service}, {@code @Transactional}, dependency injection</li>
 * <li>External libraries: Byte Buddy (dynamic types), JTS (geospatial), OpenAI client, SnakeYAML</li>
 * </ul>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * @Autowired
 * private OrganizationService organizationService;
 *
 * Organization org = organizationService.createOrganization("TenantCo");
 * }
 * </pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.core
 * @see com.openkoda.model
 * @see com.openkoda.repository
 */
package com.openkoda.service;