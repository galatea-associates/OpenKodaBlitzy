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
 * Provides small, production-focused helper utilities, Spring components, and lightweight functional contracts
 * used across web MVC, security, persistence, and UI layers.
 * <p>
 * This package aggregates utility classes that handle common cross-cutting concerns such as URL construction,
 * JSON serialization, reflection operations, date formatting, resource loading, and Spring framework integration.
 * Most helpers are implemented as Spring components for dependency injection, while others provide static utility
 * methods for convenience.
 * </p>
 *
 * <h2>Key Helper Categories</h2>
 *
 * <h3>URL and Routing</h3>
 * <ul>
 *   <li><b>UrlHelper</b>: Centralizes canonical URL composition, servlet path parsing, and tenant resolution.
 *       Provides organization-scoped URL patterns for multi-tenancy support. Configured as {@code @Primary}
 *       Spring bean with {@code @PostConstruct} initialization for legacy static access via {@code getInstance()}.</li>
 * </ul>
 *
 * <h3>Data Serialization</h3>
 * <ul>
 *   <li><b>JsonHelper</b>: Manages JSON operations using shared ObjectMapper with custom serializers
 *       (KeyWithClassSerializer for type-preserving serialization) and Gson with {@code @Expose} filtering.
 *       Thread-safe static instances configured for indented output and Java Time support.</li>
 * </ul>
 *
 * <h3>Reflection and Introspection</h3>
 * <ul>
 *   <li><b>ReflectionHelper</b>: Generates readable method signatures, simplifies type names, detects field types.
 *       Used for logging, metadata display, and dynamic form generation. <em>Performance warning:</em> reflection
 *       operations are slower than direct access; cache results when possible.</li>
 * </ul>
 *
 * <h3>Date and Time Formatting</h3>
 * <ul>
 *   <li><b>DatesHelper</b>: Provides locale-aware date formatters, timezone handling, and date arithmetic.
 *       Allocates SimpleDateFormat per call to avoid thread-safety issues with shared mutable formatters.</li>
 * </ul>
 *
 * <h3>Resource Loading</h3>
 * <ul>
 *   <li><b>ResourcesHelper</b>: Loads classpath resources as UTF-8 strings using Apache Commons IOUtils.
 *       Accesses resources from application classpath and JAR files.</li>
 * </ul>
 *
 * <h3>Expression Evaluation</h3>
 * <ul>
 *   <li><b>RuleSpelHelper</b>: Parses and validates Spring Expression Language (SpEL) expressions, integrates
 *       with JPA Criteria API for dynamic query building. <em>Performance note:</em> SpEL parsing is expensive;
 *       cache parsed expressions where possible.</li>
 * </ul>
 *
 * <h3>Database Utilities</h3>
 * <ul>
 *   <li><b>SqlCommentStatementInspector</b>: Hibernate StatementInspector that injects SQL comments for observability.
 *       Replaces user ID placeholders and appends request IDs as comments for database query correlation.
 *       <em>Note:</em> alters SQL text, which has implications for query plan caching.</li>
 * </ul>
 *
 * <h3>Spring Integration</h3>
 * <ul>
 *   <li><b>ApplicationContextProvider</b>: Stores ApplicationContext in static field for global access outside
 *       managed components. <em>Warning:</em> introduces lifecycle risks; prefer standard dependency injection.</li>
 *   <li><b>SpringProfilesHelper</b>: Detects active Spring profiles using prioritized lookup (system property,
 *       command-line parsing, environment variable).</li>
 *   <li><b>ClusterHelper</b>: Provides Hazelcast cluster metadata, computes master/member status from injected
 *       properties, exposes static API backed by {@code @PostConstruct} initialization.</li>
 * </ul>
 *
 * <h3>User Context</h3>
 * <ul>
 *   <li><b>UserHelper</b>: Accesses current user information from UserProvider context with null-safe defaults
 *       for unauthenticated users (returns empty collections and default primitives).</li>
 *   <li><b>PrivilegeHelper</b>: Manages RBAC utilities, privilege serialization, and conversion between enum
 *       and database privileges. Implements JPA AttributeConverter for privilege persistence.</li>
 * </ul>
 *
 * <h3>Naming Conventions</h3>
 * <ul>
 *   <li><b>NameHelper</b>: Converts between camelCase, snake_case, and PascalCase using Guava CaseFormat.
 *       Used for dynamic entity generation, repository naming, and database schema mapping.</li>
 * </ul>
 *
 * <h3>Internationalization</h3>
 * <ul>
 *   <li><b>Messages</b>: Simplifies access to i18n messages from {@code messages_*.properties} files.
 *       Provides fallback label generation from field names using camelCase splitting. Uses Spring
 *       MessageSourceAccessor for message resolution.</li>
 * </ul>
 *
 * <h3>Interceptors</h3>
 * <ul>
 *   <li><b>SlashEndingUrlInterceptor</b>: HandlerInterceptor that removes trailing slashes from non-root URLs
 *       for SEO and canonical URL consistency. Does not preserve query strings during redirect.</li>
 *   <li><b>ModelEnricherInterceptor</b>: Enriches view models with common attributes before rendering (user
 *       context, notifications, date formatters, captcha configuration, build info, session metadata).</li>
 *   <li><b>ModulesInterceptor</b>: Adds module-specific menu items and resources to view models, enabling
 *       dynamic module registration and UI integration.</li>
 * </ul>
 *
 * <h3>Functional Utilities</h3>
 * <ul>
 *   <li><b>ReadableCode/ReadableCodeUtil</b>: Provides named boolean negation for improved code readability.
 *       Stateless default method and static facade, safe for concurrent use.</li>
 *   <li><b>Function3</b>: Tri-arity functional interface extending Java's functional programming support
 *       beyond BiFunction.</li>
 * </ul>
 *
 * <h2>Design Patterns</h2>
 * <p>
 * The helper package employs several design patterns:
 * </p>
 * <ul>
 *   <li><b>Singleton with {@code @PostConstruct}</b>: UrlHelper, ApplicationContextProvider, and ClusterHelper
 *       use static fields initialized via Spring lifecycle callbacks for backward compatibility.</li>
 *   <li><b>Static Utilities</b>: All *Helper classes provide static methods for convenience, though most are
 *       also available as Spring components for dependency injection.</li>
 *   <li><b>Spring Component Injection</b>: Prefer dependency injection over static access for better testability
 *       and lifecycle management.</li>
 * </ul>
 *
 * <h2>Usage Guidance</h2>
 * <p>
 * <b>When to use each helper:</b>
 * </p>
 * <ul>
 *   <li>Use <b>UrlHelper</b> for URL construction, tenant parsing, and pagination parameters.</li>
 *   <li>Use <b>JsonHelper</b> for JSON serialization with type preservation or Gson @Expose filtering.</li>
 *   <li>Use <b>ReflectionHelper</b> for method signature formatting and field type detection (cache results).</li>
 *   <li>Use <b>DatesHelper</b> for locale-aware date formatting and date arithmetic.</li>
 *   <li>Use <b>Messages</b> for i18n message retrieval with automatic fallback label generation.</li>
 *   <li>Use <b>PrivilegeHelper</b> for privilege serialization and RBAC utilities.</li>
 *   <li>Use <b>NameHelper</b> for case format conversions in dynamic entity generation.</li>
 * </ul>
 *
 * <h2>Thread-Safety Considerations</h2>
 * <p>
 * Most helpers are thread-safe for concurrent use:
 * </p>
 * <ul>
 *   <li>Static utility methods with no shared mutable state are safe.</li>
 *   <li>Spring components are typically stateless or use thread-safe collections.</li>
 *   <li>JsonHelper's shared ObjectMapper and Gson instances are thread-safe for read operations.</li>
 *   <li>DatesHelper allocates formatters per call to avoid SimpleDateFormat thread-safety issues.</li>
 *   <li>ApplicationContextProvider's static field is set once during startup; reads are safe after initialization.</li>
 * </ul>
 *
 * <h2>Performance Implications</h2>
 * <p>
 * <b>Cache expensive operations:</b>
 * </p>
 * <ul>
 *   <li><b>Reflection:</b> ReflectionHelper operations are slower than direct access; cache method signatures.</li>
 *   <li><b>SpEL Evaluation:</b> RuleSpelHelper parsing is expensive; cache parsed expressions.</li>
 *   <li><b>SQL Comments:</b> SqlCommentStatementInspector alters SQL text, affecting query fingerprinting.</li>
 * </ul>
 *
 * <h2>Important Warnings</h2>
 * <ul>
 *   <li><b>ApplicationContextProvider:</b> Static context access is an anti-pattern; use only when dependency
 *       injection is not available. Can return null before Spring initialization completes.</li>
 *   <li><b>Reflection Performance:</b> Reflection operations have significant overhead compared to direct access.</li>
 *   <li><b>SpEL Evaluation Overhead:</b> Dynamic expression evaluation is slower than compiled code.</li>
 *   <li><b>SQL Comment Caching:</b> Injected comments may affect database query plan caching and fingerprinting.</li>
 * </ul>
 *
 * <h2>Key Dependencies</h2>
 * <p>
 * This package depends on:
 * </p>
 * <ul>
 *   <li>Spring Framework (DI, MVC, SpEL, MessageSource)</li>
 *   <li>Jackson (ObjectMapper, custom serializers, JavaTimeModule)</li>
 *   <li>Gson (with @Expose annotation support)</li>
 *   <li>Apache Commons (IOUtils, Lang3, Text)</li>
 *   <li>Guava (CaseFormat for naming conventions)</li>
 *   <li>Hazelcast (cluster coordination)</li>
 *   <li>Hibernate SPI (StatementInspector for SQL interception)</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
package com.openkoda.core.helper;