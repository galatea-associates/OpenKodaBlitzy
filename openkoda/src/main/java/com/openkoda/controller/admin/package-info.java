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
 * Administrative interface controllers for platform management, monitoring, and system configuration.
 * <p>
 * This package contains Spring MVC controllers implementing the administrative interface for OpenKoda.
 * Controllers provide system monitoring, log management, audit trail browsing, integration configuration,
 * and health diagnostics. All endpoints require elevated admin privileges enforced via Spring Security
 * method-level annotations.
 * </p>
 *
 * <h2>Architecture</h2>
 * <p>
 * The package follows a two-tier controller pattern:
 * </p>
 * <ul>
 *   <li><b>Abstract Base Controllers</b>: Provide reusable Flow-based helper methods for business logic composition.
 *       Methods construct {@link com.openkoda.core.flow.PageModelMap} via Flow pipelines, delegating to
 *       repositories and services. Stateless, thread-safe design.</li>
 *   <li><b>Concrete Controllers</b>: Handle HTTP request mapping ({@code @GetMapping}, {@code @PostMapping}),
 *       parameter binding, view resolution, and response formatting. Delegate business logic to abstract helpers.</li>
 * </ul>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link AbstractAdminLogsController} - Flow helpers for log viewing and logger configuration</li>
 *   <li>{@link AdminLogsController} - HTTP endpoints for log download, viewing, and configuration UI</li>
 *   <li>{@link AbstractAuditController} - Flow helpers for audit trail retrieval with pagination and search</li>
 *   <li>{@link AuditController} - HTTP endpoints for audit browsing and content download</li>
 *   <li>{@link AbstractIntegrationsController} - Flow helpers for integration configuration (email/SMTP)</li>
 *   <li>{@link IntegrationsController} - HTTP endpoints for integration setup and management</li>
 *   <li>{@link AbstractSystemHealthController} - Flow helpers for system health metrics, database validation, JS thread control</li>
 *   <li>{@link SystemHealthController} - HTTP endpoints for system health dashboard, thread monitoring, application restart</li>
 * </ul>
 *
 * <h2>Common Patterns</h2>
 * <p><b>Flow Pipeline Composition</b></p>
 * <pre>{@code
 * Flow.init()
 *     .thenSet("modelKey", a -> repositoryQuery())
 *     .then(a -> sideEffectOperation())
 *     .execute();
 * }</pre>
 *
 * <p><b>Pagination and Filtering</b></p>
 * <ul>
 *   <li>Spring Data {@link org.springframework.data.domain.Pageable} for pagination parameters</li>
 *   <li>{@code @Qualifier} annotations to namespace multiple page parameters (e.g., "audit", "obj")</li>
 *   <li>{@code @SortDefault} for default sort order (typically ID descending for newest-first)</li>
 *   <li>Full-text search parameters (e.g., "audit_search") for content filtering</li>
 * </ul>
 *
 * <p><b>Privilege Enforcement</b></p>
 * <ul>
 *   <li>{@code @PreAuthorize} annotations on all HTTP endpoint methods</li>
 *   <li>{@code CHECK_CAN_READ_SUPPORT_DATA} - Read-only access to logs, audit, system health</li>
 *   <li>{@code CHECK_CAN_MANAGE_SUPPORT_DATA} - Write access to log configuration</li>
 *   <li>{@code CHECK_CAN_READ_BACKEND} - Read access to backend systems (thread monitoring)</li>
 *   <li>{@code CHECK_CAN_MANAGE_BACKEND} - Write access to backend systems (restart, thread control, integrations)</li>
 * </ul>
 *
 * <h2>Controller Responsibilities</h2>
 * <p><b>Log Management</b> (AdminLogsController)</p>
 * <ul>
 *   <li>View in-memory circular log buffer with configurable size</li>
 *   <li>Download complete log buffer as plain text for offline analysis</li>
 *   <li>Configure debug loggers dynamically without application restart</li>
 *   <li>Set log buffer size and select logging class names</li>
 * </ul>
 *
 * <p><b>Audit Trail</b> (AuditController)</p>
 * <ul>
 *   <li>Browse system audit records with pagination (entity changes, user actions, system events)</li>
 *   <li>Full-text search across audit content and metadata fields</li>
 *   <li>Download individual audit content (JSON entity snapshots) for detailed inspection</li>
 *   <li>Privilege-enforced queries via {@code repositories.secure.audit}</li>
 * </ul>
 *
 * <p><b>Integration Configuration</b> (IntegrationsController)</p>
 * <ul>
 *   <li>Manage system-wide integration settings (non-tenant-specific)</li>
 *   <li>Configure email/SMTP: host, port, username, password, TLS/SSL</li>
 *   <li>AJAX form submission with validation feedback via HTMX fragments</li>
 *   <li>Extensible for additional integration types (OAuth, API keys)</li>
 * </ul>
 *
 * <p><b>System Health Monitoring</b> (SystemHealthController)</p>
 * <ul>
 *   <li>Display JVM metrics: heap/non-heap memory usage, thread counts (active, peak)</li>
 *   <li>Database connection pool statistics and health checks</li>
 *   <li>Cache hit rates and performance metrics</li>
 *   <li>Database schema validation: generate DDL scripts for schema drift detection (read-only)</li>
 *   <li>GraalVM JavaScript thread monitoring: list active server-side script executions</li>
 *   <li>Thread lifecycle control: interrupt runaway scripts, remove completed threads</li>
 *   <li>Application restart: graceful shutdown requiring external restart mechanism</li>
 * </ul>
 *
 * <h2>Model Keys</h2>
 * <p>Standard model keys populated by controllers for view rendering:</p>
 * <ul>
 *   <li><b>logsEntryList</b> - {@code List<String>} of in-memory log entries</li>
 *   <li><b>loggerForm</b> - {@code LoggerForm} with buffer size and logger class names</li>
 *   <li><b>logClassNamesList</b> - {@code List<String>} of available logger class names</li>
 *   <li><b>auditPage</b> - {@code Page<Audit>} paginated audit records</li>
 *   <li><b>emailConfig</b> - {@code EmailConfig} entity with SMTP settings</li>
 *   <li><b>emailConfigForm</b> - {@code EmailConfigForm} for form binding</li>
 *   <li><b>systemHealthStatus</b> - {@code Map<String, Object>} with JVM/DB metrics</li>
 *   <li><b>databaseUpdateScript</b> - {@code String} SQL DDL for schema validation</li>
 *   <li><b>serverJsThreads</b> - {@code List} of GraalVM JS thread metadata</li>
 * </ul>
 *
 * <h2>View Naming Conventions</h2>
 * <p>Thymeleaf template views resolved from controller methods:</p>
 * <ul>
 *   <li><b>logs-all</b> - Log viewing UI with entry list</li>
 *   <li><b>logs-settings</b> - Logger configuration panel</li>
 *   <li><b>audit-all</b> - Audit trail listing with search and pagination</li>
 *   <li><b>integrations</b> - Integration configuration UI (email/SMTP)</li>
 *   <li><b>system-health</b> - System health dashboard with JVM/DB metrics</li>
 *   <li><b>threads</b> - JavaScript thread monitoring UI</li>
 *   <li><b>admin-dashboard</b> - Admin landing page with navigation links</li>
 *   <li><b>components</b> - Component registry (Spring beans, modules)</li>
 * </ul>
 * <p>
 * HTMX fragments for AJAX form submission (::fragment-name notation):
 * </p>
 * <ul>
 *   <li><b>entity-forms::logger-settings-form-success</b> - Successful logger config save</li>
 *   <li><b>entity-forms::logger-settings-form-error</b> - Logger config validation errors</li>
 *   <li><b>entity-forms::email-settings-form-success</b> - Successful email config save</li>
 *   <li><b>entity-forms::email-settings-form-error</b> - Email config validation errors</li>
 *   <li><b>system-health::database-validation</b> - Database validation script partial</li>
 * </ul>
 *
 * <h2>Security Requirements</h2>
 * <p>
 * All endpoints require elevated admin privileges. Unauthorized access results in Spring Security
 * {@code AccessDeniedException}.
 * </p>
 * <ul>
 *   <li><b>Support Data Read</b>: View logs, audit, system health (non-modifying operations)</li>
 *   <li><b>Support Data Manage</b>: Modify log configuration (write operations on diagnostics)</li>
 *   <li><b>Backend Read</b>: View backend internals (thread monitoring, component registry)</li>
 *   <li><b>Backend Manage</b>: Modify backend systems (restart, thread control, integration config)</li>
 * </ul>
 *
 * <h2>Typical Usage</h2>
 * <pre>{@code
 * // View logs
 * GET /html/logs/all
 *
 * // Configure loggers
 * POST /html/logs/settings
 *
 * // Browse audit trail
 * GET /html/audit/all?audit_search=User&page=0&size=20
 *
 * // Monitor system health
 * GET /html/system-health
 *
 * // Validate database schema
 * GET /html/system-health/validate
 *
 * // Monitor JS threads
 * GET /html/threads
 * }</pre>
 *
 * <p><b>Should I put a class into this package?</b></p>
 * <p>
 * If the class is a Spring MVC controller ({@code @Controller} or {@code @RestController}) implementing
 * administrative functionality requiring elevated privileges, then yes. Administrative controllers
 * handle platform management, system monitoring, configuration, and diagnostics—not end-user
 * application features.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.core.flow.Flow
 * @see com.openkoda.core.flow.PageModelMap
 * @see com.openkoda.core.security.HasSecurityRules
 */
package com.openkoda.controller.admin;