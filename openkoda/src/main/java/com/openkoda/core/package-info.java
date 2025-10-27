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
 * Foundation layer providing cross-cutting concerns, framework infrastructure, and domain-agnostic utilities
 * for the entire OpenKoda application.
 * 
 * <p>
 * This package contains all foundation classes that are abstract, generic, or framework-like rather than
 * implementations of specific business functionality. The core module serves as the foundational layer used
 * by all higher-level modules including controller, service, model, and repository packages. It provides
 * essential cross-cutting concerns such as request tracing, entity auditing, caching, security enforcement,
 * and multi-tenancy support that span across the entire application architecture.
 * </p>
 * 
 * <h2>Key Classes and Interfaces</h2>
 * <ul>
 *   <li>{@code LoggingComponentWithRequestId} (tracker): Base component providing request correlation ID tracking
 *       and distributed tracing capabilities across all service layers</li>
 *   <li>{@code AuditInterceptor} (audit): Hibernate interceptor that automatically captures entity changes for
 *       audit trail generation with property-level change detection</li>
 *   <li>{@code RequestSessionCacheService} (cache): Request-scoped memoization service enabling efficient
 *       caching of expensive operations within a single HTTP request lifecycle</li>
 *   <li>{@code Flow} (flow): Functional pipeline DSL for composing controller logic with transactional execution,
 *       error handling, and result aggregation</li>
 *   <li>{@code FrontendMappingDefinition} (form): Builder interface for declarative form field definitions with
 *       reflection-based entity mapping and validation rules</li>
 *   <li>{@code CustomisationService} (customisation): Runtime extension API enabling module registration and
 *       bootstrap customization without code modification</li>
 *   <li>{@code SecureRepository} (repository): Privilege-enforcing repository wrapper interface that adds
 *       role-based access control checks to all data access operations</li>
 *   <li>{@code UrlHelper} (helper): Low-level utility for URL manipulation, parameter extraction, and
 *       path construction across the application</li>
 * </ul>
 * 
 * <h2>Package Structure</h2>
 * <p>The core package is organized into 16 specialized subpackages, each focused on specific framework concerns:</p>
 * <ul>
 *   <li><b>tracker/</b> - Request and job correlation ID tracking for distributed tracing and log aggregation</li>
 *   <li><b>audit/</b> - Hibernate-based entity auditing with property change detection and session-scoped
 *       audit capture</li>
 *   <li><b>cache/</b> - In-process memoization and request-scoped caching strategies</li>
 *   <li><b>configuration/</b> - Spring @Configuration classes for bean wiring and application context setup</li>
 *   <li><b>controller/</b> - Controller utilities including health probes and common response handlers</li>
 *   <li><b>customisation/</b> - Runtime extension APIs and bootstrap services for dynamic module registration</li>
 *   <li><b>exception/</b> - HTTP-aware exception hierarchy with status code mapping and error resolution</li>
 *   <li><b>flow/</b> - Functional pipeline DSL primitives for request handling and business logic composition</li>
 *   <li><b>form/</b> - Form DSL with field definition builders and reflection-based entity mappers</li>
 *   <li><b>helper/</b> - Low-level utilities including URL manipulation, JSON processing, reflection helpers,
 *       and date utilities</li>
 *   <li><b>job/</b> - Scheduled background job infrastructure with cron-based execution</li>
 *   <li><b>lifecycle/</b> - Application startup and shutdown lifecycle management hooks</li>
 *   <li><b>multitenancy/</b> - Tenant isolation mechanisms and organization-scoped operation support</li>
 *   <li><b>repository/</b> - Repository base contracts, secure wrappers, and data access utilities</li>
 *   <li><b>security/</b> - Security primitives including authentication handlers and privilege enforcement</li>
 *   <li><b>service/</b> - Core domain-agnostic services used across business logic layers</li>
 * </ul>
 * 
 * <h2>Design Patterns</h2>
 * <p>The core module implements several key design patterns:</p>
 * <ul>
 *   <li><b>Interceptor Pattern</b> - AuditInterceptor captures entity changes transparently during
 *       Hibernate session operations</li>
 *   <li><b>Aspect-Oriented Programming</b> - TrackJobAspect weaves request correlation IDs into method
 *       execution contexts</li>
 *   <li><b>Builder Pattern</b> - FrontendMappingDefinition and Flow provide fluent APIs for complex
 *       object construction</li>
 *   <li><b>Service Locator/Aggregator</b> - Repositories and Services classes aggregate related beans
 *       for convenient access</li>
 *   <li><b>Strategy Pattern</b> - Security and customisation modules allow pluggable behavior
 *       selection at runtime</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <p><b>Flow Pipeline Composition:</b></p>
 * <pre>
 * Flow.init(userRepository, securityService)
 *     .thenSet("user", a -&gt; userRepository.findById(userId))
 *     .execute();
 * </pre>
 * 
 * <p><b>Request-Scoped Caching:</b></p>
 * <pre>
 * Organization org = cache.tryGet(cacheKey, 
 *     () -&gt; organizationRepository.findOne(orgId));
 * </pre>
 * 
 * <p><b>Audit Trail Generation:</b></p>
 * <pre>
 * // AuditInterceptor automatically captures changes
 * organization.setName("UpdatedName");
 * repository.save(organization); // Audit entry created
 * </pre>
 * 
 * <h2>Relationships with Other Packages</h2>
 * <ul>
 *   <li><b>core → controller, service, model, repository</b> - Provides foundation classes and utilities
 *       used throughout all application layers</li>
 *   <li><b>core.flow → controller</b> - Flow pipeline DSL is the primary mechanism for implementing
 *       request handling logic in all controllers</li>
 *   <li><b>core.security → all layers</b> - Security primitives enforce privilege checks across
 *       controller, service, and repository operations</li>
 *   <li><b>core.multitenancy → service, repository</b> - Tenant context propagation enables
 *       organization-scoped data isolation in service and repository layers</li>
 *   <li><b>core.audit → model</b> - Auditing infrastructure tracks changes to all JPA entities
 *       defined in the model package</li>
 * </ul>
 * 
 * <h2>Guidelines for Adding Classes</h2>
 * <p>
 * <b>Should I put a class into this package?</b> If the class is NOT an implementation of concrete business
 * functionality, then it probably belongs in core. Consider these questions:
 * </p>
 * <ul>
 *   <li>Is it domain-agnostic and reusable across multiple business contexts?</li>
 *   <li>Does it provide framework-level infrastructure or cross-cutting concerns?</li>
 *   <li>Is it an abstract base class or generic utility used by multiple modules?</li>
 * </ul>
 * <p>
 * If you answered yes to any of these questions, place the class in the appropriate core subpackage based
 * on its specific concern area (tracker, audit, cache, security, etc.).
 * </p>
 * 
 * @see com.openkoda.core.flow.Flow
 * @see com.openkoda.core.audit.AuditInterceptor
 * @see com.openkoda.core.cache.RequestSessionCacheService
 * @see com.openkoda.core.tracker.LoggingComponentWithRequestId
 * @see com.openkoda.core.customisation.CustomisationService
 * @see com.openkoda.core.security
 * @see com.openkoda.core.multitenancy
 * 
 * @since 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.core;