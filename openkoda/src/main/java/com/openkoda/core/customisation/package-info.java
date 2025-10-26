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
 * Provides core customization and extension infrastructure for the OpenKoda platform.
 * <p>
 * This package serves as the central customization framework enabling runtime module registration,
 * application bootstrap orchestration, server-side JavaScript execution via GraalVM, and custom
 * Hibernate SQL function integration. It defines the extension API surface and implements the
 * coordinated startup sequence for the entire application.
 * </p>
 *
 * <h2>Package Purpose</h2>
 * <p>
 * The customisation package provides four primary capabilities:
 * </p>
 * <ul>
 * <li><b>Application Bootstrap Orchestration</b>: Coordinates deterministic startup sequence including
 * repository discovery, initial data loading, event consumer registration, and extension point activation</li>
 * <li><b>Runtime Module Registration</b>: Exposes programmatic APIs for modules to register auditable classes,
 * event listeners, frontend mappings, and CRUD controllers without modifying core code</li>
 * <li><b>GraalVM JavaScript Integration</b>: Enables server-side JavaScript execution with polyglot
 * context management and bidirectional Java-JavaScript interoperability</li>
 * <li><b>Hibernate Function Contribution</b>: Extends Hibernate HQL with custom PostgreSQL functions
 * for array operations and string aggregation</li>
 * </ul>
 *
 * <h2>Key Classes and Responsibilities</h2>
 *
 * <h3>Core Bootstrap and Extension API</h3>
 * <ul>
 * <li>{@link com.openkoda.core.customisation.BasicCustomisationService} - Central Spring @Service
 * orchestration hub extending ComponentProvider. Implements deterministic {@code onApplicationStart}
 * sequence triggered by {@code ContextRefreshedEvent}: sets cron authentication, discovers searchable
 * repositories, registers dynamic repositories, loads initial data, registers built-in event consumers
 * (EmailService, BackupService, ServerJSRunner handlers, role modifications, webhooks), registers
 * configured event classes, restores persisted listeners and scheduled jobs, loads form definitions,
 * prepares search views, invokes registered startup listeners, and emits APPLICATION_STARTED event.
 * Thread-safe frontend mapping registration via synchronized methods.</li>
 * <li>{@link com.openkoda.core.customisation.CustomisationService} - Public extension interface defining
 * the customization API surface. Declares methods for auditable class registration, event listener/consumer
 * registration, module lifecycle management, settings form registration, frontend mapping registration/
 * unregistration, and CRUD controller registration with privilege-based access control. Primary contract
 * for external modules to integrate with OpenKoda runtime.</li>
 * <li>{@link com.openkoda.core.customisation.CoreSettledEvent} - Empty marker POJO event type signaling
 * that core bootstrap has finished. Emitted after {@code ContextRefreshedEvent} processing completes.
 * Used for typed event listeners requiring post-bootstrap initialization.</li>
 * </ul>
 *
 * <h3>GraalVM JavaScript Execution</h3>
 * <ul>
 * <li>{@link com.openkoda.core.customisation.ServerJSRunner} - Spring @Service managing GraalVM
 * polyglot context for server-side JavaScript execution. Configures {@code Context.Builder} with
 * {@code allowAllAccess(true)} privileges and {@code allowHostClassLookup}. Evaluates ServerJS
 * scripts with injected bindings (model, process, resources). Integrates with scheduler events
 * and CustomisationService exposure. Creates new Context per evaluation for isolation.</li>
 * <li>{@link com.openkoda.core.customisation.ServerJSProcessRunner} - Context wrapper for ServerJS
 * thread execution. Maintains thread registry with {@code LoggingEntriesStack}, exposes Services
 * to JavaScript, provides helper methods callable from scripts (getLong, getBigDecimal, emitEventAsync,
 * sleep, createFileWithContent, log variants, command execution). Handles bash/WSL command execution
 * on Windows vs Linux. Thread interruption and lifecycle management.</li>
 * </ul>
 *
 * <h3>Hibernate Custom Functions and ID Generation</h3>
 * <ul>
 * <li>{@link com.openkoda.core.customisation.CustomFunctionContributor} - Hibernate FunctionContributor
 * registered at bootstrap. Adds custom SQL functions to HQL/SQM registry: {@code string_agg} (StandardSQLFunction),
 * {@code arrays_suffix} (unnests array, concatenates suffix to elements, re-aggregates), {@code arrays_overlap}
 * (renders typed PostgreSQL array literals with overlap operator &&). Supports ArrayList&lt;String&gt; to
 * ::varchar[] and HashSet&lt;Long&gt; to ::bigint[] conversions. Assumes PostgreSQL dialect semantics.</li>
 * <li>{@link com.openkoda.core.customisation.UseIdOrGenerate} - Custom Hibernate ID generator extending
 * SequenceStyleGenerator. Implements hybrid ID generation: returns existing non-null id from entity
 * (LongIdEntity), otherwise delegates to sequence generator. Used by entities requiring pre-assigned
 * IDs (e.g., canonical Role entities with predefined identifiers).</li>
 * </ul>
 *
 * <h3>Supporting Infrastructure</h3>
 * <ul>
 * <li>{@link com.openkoda.core.customisation.FrontendMapping} - Immutable record bundling
 * {@code FrontendMappingDefinition} metadata with {@code ScopedSecureRepository}. Atomic pairing
 * of form metadata with corresponding repository for thread-safe lookups.</li>
 * <li>{@link com.openkoda.core.customisation.FrontendMappingMap} - Spring @Component singleton registry
 * bean extending {@code HashMap<String, FrontendMapping>}. Injectable map for frontend mapping name-to-definition
 * lookups. Inherits HashMap mutability requiring synchronized access via CustomisationService.</li>
 * <li>{@link com.openkoda.core.customisation.LoggingCallable} - Wrapper for concurrent task execution
 * with logging capability. Generic {@code Callable<V>} wrapping {@code Supplier} with {@code Writer}
 * for captured output. Used with ExecutorService and LoggingFutureWrapper for async logging pattern.</li>
 * <li>{@link com.openkoda.core.customisation.LoggingFutureWrapper} - Future delegation with log binding.
 * Wraps {@code Future<V>} and provides {@code getLog()} method for retrieving captured execution logs
 * from CharArrayWriter. Delegates all Future methods (cancel, isCancelled, isDone, get overloads).</li>
 * </ul>
 *
 * <h2>Framework Integration</h2>
 *
 * <h3>Spring Framework Integration</h3>
 * <p>
 * Package leverages Spring's component model and lifecycle hooks:
 * </p>
 * <ul>
 * <li><b>@Service Beans</b>: BasicCustomisationService, ServerJSRunner annotated for component scanning</li>
 * <li><b>ApplicationContext Lifecycle</b>: {@code @EventListener} on {@code ContextRefreshedEvent}
 * triggers deterministic bootstrap sequence</li>
 * <li><b>Dependency Injection</b>: Constructor injection of collaborators (AuditInterceptor, ApplicationContext,
 * repositories, services)</li>
 * <li><b>@Value Configuration</b>: Property injection for init credentials, base URL, event classes array</li>
 * <li><b>@Component Registry</b>: FrontendMappingMap as injectable singleton HashMap</li>
 * </ul>
 *
 * <h3>Hibernate Integration</h3>
 * <p>
 * Custom Hibernate extensions for PostgreSQL-specific functionality:
 * </p>
 * <ul>
 * <li><b>FunctionContributor</b>: CustomFunctionContributor registered via service loader mechanism</li>
 * <li><b>Custom Functions</b>: string_agg, arrays_suffix, arrays_overlap available in HQL queries</li>
 * <li><b>SequenceStyleGenerator Extension</b>: UseIdOrGenerate provides hybrid ID generation strategy</li>
 * <li><b>SQM API Usage</b>: SqlAppender, SqlAstTranslator for self-rendering functions</li>
 * </ul>
 *
 * <h3>GraalVM Polyglot Integration</h3>
 * <p>
 * Server-side JavaScript execution with full host access:
 * </p>
 * <ul>
 * <li><b>Context.Builder Configuration</b>: allowAllAccess(true), allowHostClassLookup(predicate)</li>
 * <li><b>Polyglot Bindings</b>: Inject model (deserialized JSON), process (ServerJSProcessRunner),
 * resources (FileSystemImpl) into JavaScript context</li>
 * <li><b>Thread Management</b>: Registry of active ServerJS threads with LoggingEntriesStack</li>
 * <li><b>Command Execution</b>: OS-aware bash/WSL integration for external process invocation</li>
 * </ul>
 *
 * <h2>Usage Patterns</h2>
 *
 * <h3>Module Registration Workflow</h3>
 * <pre><code>
 * customisationService.registerModule("my-module");
 * customisationService.registerAuditableClass(MyEntity.class, "My Entity");
 * customisationService.registerEventListener(MyEvent.class, this::handleEvent);
 * </code></pre>
 *
 * <h3>Event Consumer Setup</h3>
 * <pre><code>
 * customisationService.registerEventConsumer(new EventConsumer(
 *     MyEvent.class, params -&gt; processEvent(params)));
 * </code></pre>
 *
 * <h3>Frontend Mapping Registration</h3>
 * <pre><code>
 * customisationService.registerFrontendMapping(
 *     definition, repository);
 * </code></pre>
 *
 * <h3>Server-Side JavaScript Execution</h3>
 * <pre><code>
 * serverJSRunner.evaluateServerJsScript(
 *     "my-script", jsCode, model, args, String.class);
 * </code></pre>
 *
 * <h2>Design Patterns</h2>
 * <ul>
 * <li><b>Service Locator</b>: CustomisationService as central registry for extension points</li>
 * <li><b>Registry</b>: FrontendMappingMap for mapping lookups</li>
 * <li><b>Wrapper</b>: LoggingFutureWrapper adds logging to Future, ServerJSProcessRunner wraps thread context</li>
 * <li><b>Template Method</b>: UseIdOrGenerate extends SequenceStyleGenerator with conditional logic</li>
 * <li><b>Builder</b>: GraalVM Context.Builder for polyglot configuration</li>
 * <li><b>Event-Driven</b>: Spring ApplicationEvent publishing for bootstrap lifecycle</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <ul>
 * <li><b>Spring Framework</b>: ApplicationContext, @Service, @Component, @EventListener, @Value</li>
 * <li><b>Hibernate ORM</b>: FunctionContributor, SequenceStyleGenerator, SQM APIs</li>
 * <li><b>GraalVM Polyglot</b>: Context, Value, HostAccess</li>
 * <li><b>PostgreSQL</b>: Array operators, type casting (::varchar[], ::bigint[])</li>
 * <li><b>JDK</b>: Concurrent utilities (ExecutorService, Future, Callable), IO (Writer, CharArrayWriter)</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 * <li>{@link com.openkoda.core.audit} - Audit interceptor and property change tracking</li>
 * <li>{@link com.openkoda.core.lifecycle} - Application lifecycle management</li>
 * <li>{@link com.openkoda.core.service.event} - Event consumer infrastructure</li>
 * <li>{@link com.openkoda.service.module} - Module definition and management</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @since 1.7.1
 */
package com.openkoda.core.customisation;