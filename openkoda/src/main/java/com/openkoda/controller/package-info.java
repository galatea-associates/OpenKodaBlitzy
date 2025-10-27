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
 * Web layer package containing Spring MVC controllers, REST API endpoints, and request handling infrastructure
 * for the OpenKoda platform.
 * <p>
 * Controllers implement the presentation layer using Flow pipelines for composition, PageModelMap for view data
 * assembly, and Result carriers for operation outcomes. This package provides both HTML-based user interfaces
 * and JSON REST APIs for programmatic access.
 * </p>
 *
 * <h2>Key Classes and Interfaces</h2>
 * <ul>
 *   <li>{@code CRUDControllerHtml} - Generic HTML CRUD controller for any entity type with list/create/edit/delete operations</li>
 *   <li>{@code CRUDApiController} - Generic REST API controller providing JSON CRUD endpoints</li>
 *   <li>{@code PageBuilderController} - Dynamic page rendering from FrontendMappingDefinition configurations</li>
 *   <li>{@code PublicController} - Unauthenticated public endpoints (registration, password reset)</li>
 *   <li>{@code ErrorControllerImpl} - Global error handling mapping exceptions to user-friendly pages</li>
 *   <li>{@code ComponentsController} - Component export/import endpoints for YAML archives</li>
 *   <li>{@code SitemapController} - XML sitemap generation for SEO</li>
 * </ul>
 *
 * <h2>Design Patterns</h2>
 * <ul>
 *   <li><b>Flow Pipeline Pattern</b> - Controllers use {@code Flow.init().thenSet(...).execute()} for composable request handling</li>
 *   <li><b>Generic CRUD Pattern</b> - Parameterized controllers reduce boilerplate for entity management</li>
 *   <li><b>Configuration Registry Pattern</b> - CRUDControllerConfigurationMap centralizes controller setup</li>
 *   <li><b>Security-by-Default</b> - @PreAuthorize annotations enforce privilege checks on endpoints</li>
 * </ul>
 *
 * <h2>Subpackages Overview</h2>
 * <ul>
 *   <li>{@code admin} - Admin interface controllers (logs, audit, integrations, system health)</li>
 *   <li>{@code api} - REST API controllers (v1, v2) with JSON responses</li>
 *   <li>{@code common} - Common constants (PageAttributes, SessionData, URLConstants)</li>
 *   <li>{@code file} - File upload/download controllers</li>
 *   <li>{@code frontendresource} - Frontend resource serving (assets, public, restricted)</li>
 *   <li>{@code notification} - Notification management controllers</li>
 *   <li>{@code organization} - Multi-tenant organization management</li>
 *   <li>{@code report} - Report generation controllers (AI-powered, query-based)</li>
 *   <li>{@code role} - Role and permission management</li>
 *   <li>{@code user} - User management and password recovery</li>
 * </ul>
 *
 * <h2>Request Flow</h2>
 * <p>
 * Typical request flow: HTTP Request → Spring MVC Controller → Flow Pipeline → Service Layer → Repository Layer
 * → Database → Result → View/JSON Response. Controllers remain thin adapters delegating business logic to services.
 * </p>
 *
 * <h2>Authentication and Authorization</h2>
 * <p>
 * Controllers use Spring Security @PreAuthorize with HasSecurityRules constants. Public endpoints in PublicController
 * require no authentication. Admin endpoints require {@code canReadLogs}, {@code canAccessGlobalSettings} privileges.
 * </p>
 *
 * <h2>Relationships</h2>
 * <p>
 * Controllers depend on: {@code core.flow} (Flow, PageModelMap), {@code service.*} (business logic),
 * {@code repository.*} (data access), {@code model.*} (domain entities), {@code form.*} (request binding).
 * Used by: Spring MVC dispatcher servlet.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Flow-based controller pattern
 * @GetMapping("/users")
 * public Object listUsers(@PathVariable Long orgId, Pageable pageable) {
 *     return Flow.init(services)
 *         .thenSet("users", a -> services.user.findAll(orgId, pageable))
 *         .execute();
 * }
 * }</pre>
 *
 * <h2>Common Pitfalls</h2>
 * <ul>
 *   <li>Avoid business logic in controllers - delegate to services</li>
 *   <li>Always use Flow pipelines for request handling consistency</li>
 *   <li>Secure all endpoints with @PreAuthorize unless explicitly public</li>
 *   <li>Use PageModelMap for view data, not raw ModelMap</li>
 * </ul>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Controllers should be stateless and thread-safe</li>
 *   <li>Use @PathVariable for resource IDs, @RequestParam for filters</li>
 *   <li>Return view names for HTML, ResponseEntity for APIs</li>
 *   <li>Log request correlation IDs using LoggingComponentWithRequestId</li>
 * </ul>
 *
 * <p><b>Should I put a class into this package?</b></p>
 * <p>
 * If the class is a controller or closely related class providing specific functionality, then yes.
 * For foundation-level or abstract controller code, consider {@code com.openkoda.core.controller} package.
 * </p>
 *
 * @since 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.controller;

