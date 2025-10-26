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
 * Provides application lifecycle management components for OpenKoda.
 * <p>
 * This package manages critical startup operations including database initialization,
 * view creation, and system component registration. Components execute during Spring
 * application context startup, ensuring the database contains required roles, privileges,
 * and administrative users before the application services requests. The package separates
 * production initialization ({@code BaseDatabaseInitializer}) from test initialization
 * to support different runtime environments.
 * </p>
 *
 * <h2>Key Classes and Interfaces</h2>
 * <ul>
 *   <li><b>BaseDatabaseInitializer</b>: Orchestrates production database initialization
 *       including role/privilege setup, admin user creation, and SQL script execution.
 *       Executes during Spring context startup via {@code loadInitialData()} method.</li>
 *   <li><b>SearchViewCreator</b>: Maintains the {@code global_search_view} database view
 *       by discovering searchable repositories and generating SQL UNION queries for
 *       unified search functionality across entities.</li>
 * </ul>
 *
 * <h2>Application Startup Sequence</h2>
 * <pre>
 * Spring Application Startup
 *      ↓
 * [Database Connection Initialization]
 *      ↓
 * [Schema Verification/Migration]
 *      ↓
 * [BaseDatabaseInitializer.loadInitialData()]
 *      ├── Set Cron Job Authentication Context
 *      ├── createCoreModule() → OpenkodaModule persistence
 *      ├── createInitialRoles()
 *      │   ├── PrivilegeHelper.getAdminPrivilegeSet()
 *      │   ├── Role creation (ROLE_ADMIN, ROLE_USER, etc.)
 *      │   └── Admin user creation if not exists
 *      ├── createRegistrationFormServerJs() → ServerJs persistence
 *      ├── runInitializationScripts()
 *      │   ├── Execute classpath SQL scripts
 *      │   └── Execute external SQL script
 *      ├── classpathComponentImportService.loadAllComponents()
 *      └── Clear Authentication Context
 *      ↓
 * [SearchViewCreator.prepareSearchableRepositories()] (if triggered)
 *      ├── DROP VIEW IF EXISTS global_search_view
 *      ├── Discover searchable repositories
 *      ├── Generate UNION subqueries
 *      └── CREATE OR REPLACE VIEW global_search_view
 *      ↓
 * [Application Ready - ApplicationReadyEvent]
 *      ↓
 * [Scheduled Jobs Startup]
 *      ↓
 * [Application Running]
 * </pre>
 *
 * <h2>Design Patterns</h2>
 * <ul>
 *   <li><b>Extension Pattern</b>: {@code BaseDatabaseInitializer} can be extended and
 *       replaced via Spring's {@code @Primary} annotation or profile-based activation,
 *       allowing customized initialization logic.</li>
 *   <li><b>Component Pattern</b>: Uses Spring {@code @Component} stereotype for automatic
 *       discovery and dependency injection during context initialization.</li>
 *   <li><b>Transactional Pattern</b>: Leverages {@code @Transactional} to ensure atomic
 *       database operations with automatic rollback on failures.</li>
 *   <li><b>Authentication Context Pattern</b>: Establishes temporary system authentication
 *       for privileged operations during initialization, cleared in finally blocks.</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <p>Custom initialization by extending the base initializer:</p>
 * <pre>{@code
 * @Primary
 * @Component
 * public class CustomInitializer extends BaseDatabaseInitializer {
 *     // Override initialization logic
 * }}</pre>
 *
 * <h2>Relationships with Other Packages</h2>
 * <ul>
 *   <li><b>core.security</b>: Depends on {@code UserProvider} for authentication context management</li>
 *   <li><b>core.multitenancy</b>: Uses {@code QueryExecutor} for database operations</li>
 *   <li><b>service.export</b>: Calls {@code ClasspathComponentImportService} for component loading</li>
 *   <li><b>core.helper</b>: Retrieves privilege sets via {@code PrivilegeHelper}</li>
 *   <li><b>repository</b>: Performs entity persistence operations through repository beans</li>
 *   <li><b>core.configuration</b>: Related for Spring bean wiring and profile activation</li>
 * </ul>
 *
 * <h2>Common Pitfalls and Best Practices</h2>
 * <ul>
 *   <li><b>Respect the proceed parameter</b>: Custom initializers should honor the configuration
 *       flag to skip initialization when appropriate.</li>
 *   <li><b>Transactional boundaries</b>: Ensure {@code @Transactional} annotations are present
 *       for atomic operations with proper rollback behavior.</li>
 *   <li><b>Authentication cleanup</b>: Always clear the authentication context in finally blocks
 *       to prevent context leakage.</li>
 *   <li><b>Profile separation</b>: Use {@code @Profile} annotations to separate test and production
 *       initialization to avoid unintended side effects.</li>
 *   <li><b>Configuration validation</b>: Validate required properties (e.g., {@code applicationAdminEmail})
 *       before use to fail fast with clear error messages.</li>
 * </ul>
 *
 * <h2>Application Lifecycle Events</h2>
 * <p>
 * This package responds to Spring application lifecycle events:
 * </p>
 * <ul>
 *   <li><b>ContextRefreshedEvent</b>: Triggered when {@code ApplicationContext} is initialized
 *       or refreshed, signaling that all beans are loaded.</li>
 *   <li><b>ApplicationReadyEvent</b>: Triggered when the application is fully started and
 *       ready to service requests.</li>
 *   <li><b>ContextClosedEvent</b>: Triggered during graceful shutdown of the application context.</li>
 * </ul>
 *
 * <h2>Shutdown Procedures</h2>
 * <p>
 * This package focuses primarily on application startup. Shutdown operations are minimal,
 * as database connections and transactions are managed by Spring's lifecycle. Components may
 * implement {@code @PreDestroy} methods for cleanup operations if needed. Graceful shutdown
 * ensures that in-progress transactions complete before context closure.
 * </p>
 *
 * @version 1.7.1
 * @since 1.7.1
 */
package com.openkoda.core.lifecycle;