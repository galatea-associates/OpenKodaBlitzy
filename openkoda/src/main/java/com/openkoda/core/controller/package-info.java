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
 * Provides cross-cutting controller-level utilities and infrastructure for the OpenKoda platform.
 * <p>
 * This package contains foundational components that support Spring MVC controller operations
 * across the entire application. It delivers essential services including OpenAPI documentation
 * parameter helpers, application health monitoring endpoints, and abstract base controller
 * classes for administrative user interface operations.
 * </p>
 *
 * <h2>Package Purpose</h2>
 * <p>
 * The core.controller package serves as the foundation layer for web request handling utilities
 * that are used throughout the OpenKoda controller hierarchy. Unlike domain-specific controller
 * packages (such as com.openkoda.controller.organization or com.openkoda.controller.user), this
 * package provides horizontal functionality applicable to all controller implementations.
 * </p>
 *
 * <h2>Key Components</h2>
 *
 * <h3>ApiPageable Annotation</h3>
 * <p>
 * Provides OpenAPI documentation support for Spring Data pageable parameters. This annotation
 * enables automatic generation of Swagger/OpenAPI documentation for REST API endpoints that
 * accept pagination and sorting parameters. It eliminates repetitive parameter documentation
 * by declaratively specifying pagination capabilities.
 * </p>
 * <pre>
 * {@code @GetMapping("/api/organizations")
 * @ApiPageable
 * public Page<Organization> list(Pageable pageable) { ... }}
 * </pre>
 *
 * <h3>ApplicationStatusController</h3>
 * <p>
 * Exposes health check and liveness probe endpoints for operational monitoring. This controller
 * provides standardized HTTP endpoints (typically GET /ping or GET /health) that monitoring
 * systems, load balancers, and orchestration platforms use to verify application availability
 * and readiness. Essential for production deployments with Kubernetes, Docker Swarm, or cloud
 * platform health checks.
 * </p>
 * <pre>
 * curl http://localhost:8080/ping
 * </pre>
 *
 * <h2>Subpackages</h2>
 * <dl>
 *     <dt>event/</dt>
 *     <dd>Event-driven controller utilities and event handling infrastructure for asynchronous
 *     request processing and application event publication within controller contexts.</dd>
 *
 *     <dt>frontendresource/</dt>
 *     <dd>Frontend resource management controllers for serving static assets, JavaScript modules,
 *     CSS stylesheets, and dynamic frontend resources. Handles resource versioning and cache
 *     control headers.</dd>
 *
 *     <dt>generic/</dt>
 *     <dd>Generic and abstract controller base classes providing reusable CRUD operations,
 *     common request handling patterns, and administrative UI controller scaffolding. Controllers
 *     in application-specific packages extend these base classes to inherit standard operations.</dd>
 * </dl>
 *
 * <h2>Design Patterns</h2>
 * <p>
 * This package implements several architectural patterns:
 * </p>
 * <ul>
 *     <li><strong>Cross-Cutting Concerns:</strong> Health probes and API documentation helpers
 *     provide horizontal functionality used across all controller implementations.</li>
 *     <li><strong>Template Method:</strong> Abstract controller base classes in the generic/
 *     subpackage define common operation workflows while allowing specialization in subclasses.</li>
 *     <li><strong>Decorator Pattern:</strong> ApiPageable annotation decorates controller methods
 *     with additional metadata for documentation generation without modifying method signatures.</li>
 * </ul>
 *
 * <h2>Relationships with Other Modules</h2>
 * <p>
 * The core.controller package integrates with several other OpenKoda modules:
 * </p>
 * <ul>
 *     <li><strong>com.openkoda.core.flow:</strong> Controllers use Flow pipelines for request
 *     handling orchestration and response construction via PageModelMap.</li>
 *     <li><strong>com.openkoda.core.security:</strong> Health endpoints may bypass authentication
 *     while admin UI base controllers enforce privilege checks.</li>
 *     <li><strong>com.openkoda.controller:</strong> Application-specific controllers extend base
 *     classes and use utilities provided by this foundation package.</li>
 *     <li><strong>com.openkoda.service:</strong> Controllers delegate business logic to service
 *     layer components while handling request/response translation.</li>
 * </ul>
 *
 * <h2>Usage Guidance</h2>
 *
 * <h3>OpenAPI Documentation Generation</h3>
 * <p>
 * Use the ApiPageable annotation on any REST API controller method that accepts Spring Data
 * Pageable parameters. This automatically documents pagination query parameters (page, size,
 * sort) in generated OpenAPI/Swagger specifications without manual parameter declaration.
 * </p>
 *
 * <h3>Operational Health Monitoring</h3>
 * <p>
 * Configure your monitoring system, load balancer, or orchestration platform to poll the
 * ApplicationStatusController health endpoint. For Kubernetes deployments, configure liveness
 * and readiness probes to use the /ping endpoint. For Docker Compose or Swarm deployments,
 * configure healthcheck directives to verify application availability.
 * </p>
 *
 * <h3>Extending Abstract Controllers</h3>
 * <p>
 * When creating new administrative UI controllers, extend the abstract base classes from the
 * generic/ subpackage. These base classes provide standard CRUD operations, privilege enforcement,
 * and common request handling patterns. Override template methods to customize behavior for
 * specific entity types while inheriting standard operational patterns.
 * </p>
 *
 * @since 1.7.1
 * @author OpenKoda Team
 * @see com.openkoda.core.flow.Flow
 * @see com.openkoda.core.security
 * @see com.openkoda.controller
 */
package com.openkoda.core.controller;