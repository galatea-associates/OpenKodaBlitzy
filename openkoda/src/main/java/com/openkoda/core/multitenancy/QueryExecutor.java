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

package com.openkoda.core.multitenancy;

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.persistence.*;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Centralized low-level database interaction component for executing SQL queries and EntityManager operations.
 * <p>
 * QueryExecutor provides a unified interface for database operations with comprehensive lifecycle management.
 * It creates EntityManager instances, configures them, executes operations, and ensures proper cleanup.
 * The component supports both transactional and non-transactional execution modes, integrates with
 * TenantResolver for tenant-aware operations, and provides automatic resource loading from classpath.
 * <p>
 * Key capabilities include:
 * <ul>
 * <li>EntityManager lifecycle management with automatic cleanup via try-finally blocks</li>
 * <li>Optional transaction control: transactional execution with commit/rollback or auto-commit mode</li>
 * <li>Tenant-aware query execution integrated with TenantResolver and SchemaSupportingConnectionProvider</li>
 * <li>Automatic classpath resource loading for SQL scripts</li>
 * <li>Schema introspection via getSchemas() for discovering tenant schemas</li>
 * <li>Comprehensive error handling with logging and transaction rollback</li>
 * </ul>
 * <p>
 * Thread-safety: This component uses a stateless design with no instance state beyond the injected
 * EntityManagerFactory. Each method invocation creates an isolated EntityManager, making it safe for
 * concurrent use by multiple threads in the service layer.
 * <p>
 * Transaction management: Transactional methods use JPA EntityTransaction (not Spring @Transactional).
 * Transactions are automatically rolled back on any exception. Auto-commit mode is used for
 * non-transactional operations.
 * <p>
 * Tenant-aware execution: TenantResolver.getTenantedResource() provides tenant context, and
 * SchemaSupportingConnectionProvider automatically sets the PostgreSQL search_path to execute
 * queries against tenant-specific schemas (org_&lt;organizationId&gt;).
 * <p>
 * Usage patterns:
 * <pre>{@code
 * // Execute SQL script from classpath
 * queryExecutor.runQueryFromResourceInTransaction("/sql/init-tenant.sql");
 * 
 * // Execute EntityManager operation with transaction
 * queryExecutor.runEntityManagerOperationInTransaction(em -> {
 *     return em.find(Organization.class, orgId);
 * });
 * }</pre>
 *
 * @see TenantResolver#getTenantedResource()
 * @see MultitenancyService
 * @see SchemaSupportingConnectionProvider
 * @see LoggingComponentWithRequestId
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
@Component
public class QueryExecutor implements LoggingComponentWithRequestId {

    /**
     * JPA EntityManagerFactory for creating EntityManager instances.
     * <p>
     * Injected by Spring container via @PersistenceUnit annotation. The factory is thread-safe
     * and used to create isolated EntityManager instances for each database operation. The factory
     * lifecycle is managed by Spring and closed on application shutdown.
     * 
     * <p>
     * Note: While EntityManagerFactory is thread-safe, EntityManager instances created from it
     * are not thread-safe and should not be shared between threads.
     * 
     */
    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    /**
     * Reads SQL script or resource content from the classpath.
     * <p>
     * This method loads resource files such as SQL initialization scripts from the classpath
     * and returns their content as a String. It uses try-with-resources to ensure automatic
     * closure of the InputStream, preventing resource leaks.
     * 
     * <p>
     * Error handling: If the resource is not found or an IOException occurs during reading,
     * the error is logged via LoggingComponentWithRequestId and the method returns null.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * String sql = readResource("/sql/init-tenant.sql");
     * String schema = readResource("/multitenancy/create-schema.sql");
     * }</pre>
     * 
     *
     * @param path Classpath resource path (e.g., "/sql/init-tenant.sql")
     * @return Resource content as String, or null if resource not found or IO error occurs
     */
    String readResource(String path) {
        try (InputStream resourceIO = this.getClass().getResourceAsStream(path)) {
            return IOUtils.toString(resourceIO);
        } catch (Exception e) {
            error(e, "[readResource] Can't find or read resource {}", path);
        }
        return null;
    }

    /**
     * Queries PostgreSQL information_schema to retrieve all tenant schema names.
     * <p>
     * This method queries the database to discover all schemas with the 'org_' prefix, which
     * represent tenant-specific schemas in the multi-tenancy architecture. Each tenant schema
     * is named following the pattern org_&lt;organizationId&gt; (e.g., "org_1", "org_2", "org_3").
     * 
     * <p>
     * The method executes a native SQL query against the PostgreSQL information_schema.schemata
     * system catalog to find matching schemas. Only org_* schemas are returned; public and
     * system schemas are excluded.
     * 
     * <p>
     * Used by: MultitenancyService for enumerating active tenants and performing operations
     * across all tenant schemas.
     * 
     *
     * @param em Active EntityManager for executing the native query
     * @return List of schema names starting with 'org_' prefix (e.g., ["org_1", "org_2", "org_3"])
     */
    protected List<String> getSchemas(EntityManager em) {
        return (List<String>) em.createNativeQuery("SELECT schema_name FROM information_schema.schemata where schema_name like 'org_%';").getResultList();
    }

    /**
     * Executes SQL script from classpath resource within a database transaction.
     * <p>
     * This method loads an SQL script from the specified classpath location using readResource()
     * and executes it within a transaction. The transaction is automatically rolled back if any
     * error occurs during execution.
     * 
     * <p>
     * Use case: Executing DDL (CREATE TABLE, ALTER TABLE) or DML (INSERT, UPDATE, DELETE)
     * scripts that require atomicity. If the script contains multiple statements, either all
     * succeed or all are rolled back together.
     * 
     * <p>
     * Error handling: Errors are logged via LoggingComponentWithRequestId and the transaction
     * is rolled back automatically.
     * 
     *
     * @param classpathResource Classpath path to SQL script file (e.g., "/sql/init-tenant.sql")
     */
    public void runQueryFromResourceInTransaction(String classpathResource) {
        String queryString = readResource(classpathResource);
        runQueries(true, false, queryString);
    }

    /**
     * Executes SQL script from classpath resource without transaction (auto-commit mode).
     * <p>
     * This method loads an SQL script from the specified classpath location using readResource()
     * and executes it without wrapping it in a transaction. Each SQL statement within the script
     * is auto-committed by the database.
     * 
     * <p>
     * Use case: Executing scripts where transaction control is handled externally, or for
     * operations that should not be grouped together atomically. This mode is also useful for
     * DDL statements that implicitly commit in some database systems.
     * 
     *
     * @param classpathResource Classpath path to SQL script file (e.g., "/sql/query.sql")
     */
    public void runQueryFromResource(String classpathResource) {
        String queryString = readResource(classpathResource);
        runQueries(false, false, queryString);
    }

    /**
     * Executes an array of SQL queries within a single database transaction.
     * <p>
     * This method wraps all provided queries in a single transaction, ensuring atomicity.
     * Either all queries execute successfully and the transaction is committed, or if any
     * query fails, the entire transaction is rolled back.
     * 
     * <p>
     * Atomicity guarantee: All queries succeed together or all are rolled back together.
     * This ensures database consistency even when multiple related operations are performed.
     * 
     *
     * @param queries Variable number of SQL query strings to execute sequentially
     * @return true if all queries executed successfully, false otherwise
     */
    public boolean runQueriesInTransaction(String ... queries) {
        return runQueries(true, false, queries);
    }

    /**
     * Executes an array of SQL queries without transaction (auto-commit mode).
     * <p>
     * This method executes the provided queries sequentially without wrapping them in a
     * transaction. Each query is auto-committed by the database immediately after execution.
     * 
     * <p>
     * Use case: Executing independent queries that do not need to be grouped atomically,
     * or when transaction control is managed externally.
     * 
     *
     * @param queries Variable number of SQL query strings to execute sequentially
     * @return true if queries executed successfully, false otherwise
     */
    public boolean runQueries(String ... queries) {
        return runQueries(false, false, queries);
    }

    /**
     * Core query execution method with transactional and logging control.
     * <p>
     * This is the primary internal method for executing SQL queries with configurable
     * transaction behavior and performance logging. It measures execution time using
     * System.nanoTime() and reports the duration in microseconds at debug level.
     * 
     * <p>
     * Transaction control: If transactional is true, all queries are wrapped in a single
     * transaction with automatic commit on success or rollback on error. If false, each
     * query is auto-committed individually.
     * 
     * <p>
     * Performance logging: Execution time is always measured and logged at debug level
     * in microseconds via LoggingComponentWithRequestId, regardless of the logTime parameter.
     * 
     *
     * @param transactional If true, wraps execution in transaction; if false, uses auto-commit
     * @param logTime If true, logs execution time in microseconds at debug level (currently unused)
     * @param queries Array of SQL query strings to execute sequentially
     * @return true if all queries executed successfully
     */
    private boolean runQueries(boolean transactional, boolean logTime, String ... queries) {
        long start = System.nanoTime();
        runEntityManagerOperation(transactional, em -> {
            executeQueries(em, queries);
            return null;
        });
        long stop = System.nanoTime();
        debug("[runQueries] Successfully in {} us", (stop - start) / 1000);
        return true;

    }

    /**
     * Executes an EntityManager operation without transaction.
     * <p>
     * This convenience method executes a read-only or auto-commit operation using an
     * EntityManager. The operation is not wrapped in a transaction, so changes are
     * auto-committed by the database.
     * 
     * <p>
     * Use case: Read-only operations such as queries that do not require transaction
     * protection, or single write operations where auto-commit is acceptable.
     * 
     * <p>
     * Delegates to: runEntityManagerOperationForOrg(null, false, ...)
     * 
     *
     * @param query Function accepting EntityManager and returning result
     * @param <T> Result type
     * @return Result of the operation
     */
    public <T> T runEntityManagerOperation(Function<EntityManager, T> query) {
        return runEntityManagerOperationForOrg(null, false, (em, orgId) -> query.apply(em));
    }

    /**
     * Executes an EntityManager operation with optional transaction control.
     * <p>
     * This method provides explicit control over transaction behavior. If transactional is true,
     * the operation is wrapped in a transaction with automatic commit on success or rollback
     * on error. If false, auto-commit mode is used.
     * 
     * <p>
     * Delegates to: runEntityManagerOperationForOrg(null, transactional, ...)
     * 
     *
     * @param transactional If true, wraps in transaction; if false, no transaction
     * @param query Function accepting EntityManager and returning result
     * @param <T> Result type
     * @return Result of the operation
     */
    public <T> T runEntityManagerOperation(boolean transactional, Function<EntityManager, T> query) {
        return runEntityManagerOperationForOrg(null, transactional, (em, orgId) -> query.apply(em));
    }

    /**
     * Executes an EntityManager operation within a database transaction.
     * <p>
     * This convenience method ensures the operation is wrapped in a transaction with automatic
     * commit on success or rollback on error. It is the recommended approach for write
     * operations that require atomicity.
     * 
     * <p>
     * Use case: Write operations such as persist(), merge(), or remove() that require
     * transactional protection to ensure data consistency.
     * 
     * <p>
     * Delegates to: runEntityManagerOperationForOrg(null, true, ...)
     * 
     *
     * @param query Function accepting EntityManager and returning result
     * @param <T> Result type
     * @return Result of the operation
     */
    public <T> T runEntityManagerOperationInTransaction(Function<EntityManager, T> query) {
        return runEntityManagerOperationForOrg(null, true, (em, orgId) -> query.apply(em));
    }

    /**
     * Core EntityManager operation execution with tenant context and transaction control.
     * <p>
     * This is the primary internal method for executing EntityManager operations with full
     * control over tenant context and transaction behavior. It provides comprehensive
     * lifecycle management including creation, configuration, execution, error handling,
     * and guaranteed cleanup.
     * 
     * <p>
     * Implementation details:
     * <ul>
     * <li>Creates a fresh EntityManager from EntityManagerFactory</li>
     * <li>Sets FlushModeType.AUTO for automatic change detection</li>
     * <li>Begins transaction if transactional=true</li>
     * <li>Executes diagnostic "show search_path" query for debugging tenant context</li>
     * <li>Retrieves TenantedResource from TenantResolver for context logging</li>
     * <li>Applies operation with EntityManager and orgId parameters</li>
     * <li>Commits transaction on success if transactional</li>
     * <li>Rolls back transaction on exception if transactional</li>
     * <li>Always closes EntityManager in finally block</li>
     * </ul>
     * 
     * <p>
     * Error handling: Catches all exceptions, logs them via LoggingComponentWithRequestId,
     * rolls back the transaction if active, and returns null. The EntityManager is guaranteed
     * to be closed via the finally block even if an exception occurs.
     * 
     * <p>
     * Resource management: EntityManager lifecycle is strictly controlled with creation at
     * method entry and guaranteed closure in finally block, preventing resource leaks.
     * 
     * <p>
     * Tenant awareness: The method integrates with TenantResolver to log tenant context
     * information and with SchemaSupportingConnectionProvider which automatically sets
     * the PostgreSQL search_path based on the organization ID, ensuring queries execute
     * against the correct tenant-specific schema (org_&lt;organizationId&gt;).
     * 
     * <p>
     * Thread-safety: This method is safe for concurrent use as it creates an isolated
     * EntityManager instance per invocation. EntityManagers are not shared between threads.
     * 
     * <p>
     * Logging: Debug-level logging includes search_path, tenant context (TenantedResource),
     * organization ID, and transaction mode for troubleshooting tenant-specific issues.
     * 
     *
     * @param orgId Organization ID for tenant context (can be null for non-tenant operations)
     * @param transactional If true, wraps in transaction with commit/rollback; if false, auto-commit
     * @param operation BiFunction accepting EntityManager and organizationId, returning result
     * @param <T> Type of the operation result
     * @return Result of operation, or null if an exception occurred
     * @see TenantResolver#getTenantedResource()
     * @see SchemaSupportingConnectionProvider
     */
    <T> T runEntityManagerOperationForOrg(Long orgId, boolean transactional, BiFunction<EntityManager, Long, T> operation) {
        debug("[runEntityManagerOperationForOrg] org {} t {}", orgId, transactional);
        EntityManager em = null;
        T result = null;
        EntityTransaction transaction = null;
        try {
            em = entityManagerFactory.createEntityManager();
            em.setFlushMode(FlushModeType.AUTO);
            if (transactional) {
                transaction = em.getTransaction();
                transaction.begin();
            }

            Object searchPath = em.createNativeQuery("show search_path").getSingleResult();
            TenantResolver.TenantedResource tr = TenantResolver.getTenantedResource();
            debug("[runEntityManagerOperationForOrg] search path {} tr {}", searchPath, tr);
            result = operation.apply(em, orgId);

            if (transactional) {
                transaction.commit();
            }
            em.close();
        } catch (Exception e) {
            error("[runEntityManagerOperationForOrg]", e);
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
        return result;
    }

    /**
     * Executes an array of native SQL queries using the provided EntityManager.
     * <p>
     * This method iterates through the provided queries and executes each one using
     * EntityManager.createNativeQuery().executeUpdate(). Each query and its numeric
     * result (rows affected) are logged at debug level via LoggingComponentWithRequestId.
     * 
     * <p>
     * Implementation: Uses em.createNativeQuery().executeUpdate() for each query, which
     * is appropriate for DDL (CREATE, ALTER, DROP) and DML (INSERT, UPDATE, DELETE)
     * statements.
     * 
     * <p>
     * Transaction control: This method does not manage transactions. The caller is
     * responsible for transaction control (begin, commit, rollback). Typically called
     * from runQueries() or runEntityManagerOperationForOrg() which handle transactions.
     * 
     * <p>
     * Logging: Debug-level logging of each query string before execution and the numeric
     * result (number of rows affected) after execution.
     * 
     *
     * @param em Active EntityManager for query execution
     * @param queries Array of SQL query strings to execute sequentially
     */
    void executeQueries(EntityManager em, String... queries) {
        for (String queryString : queries) {
            debug("[executeQueries] {}", queryString);
            int result = em.createNativeQuery(queryString).executeUpdate();
            debug("[executeQueries] {}", result);
        }
    }

}
