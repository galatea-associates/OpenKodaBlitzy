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

import com.openkoda.core.helper.ReadableCode;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.repository.organization.OrganizationRepository;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Central orchestrator for tenant lifecycle management and per-tenant batch operations in PostgreSQL schema-per-organization multitenancy architecture.
 * <p>
 * This service implements a schema-per-tenant multitenancy strategy where each organization receives its own PostgreSQL schema (org_[id]).
 * The service manages the complete tenant lifecycle including schema creation, initialization, and deletion, while providing high-level
 * primitives for executing operations across all active tenants in parallel.
 * <p>
 * Configuration is driven by Spring properties:
 * <ul>
 *   <li>{@code is.multitenancy} (default: false) - Master switch enabling/disabling schema-per-tenant behavior</li>
 *   <li>{@code tenant.initialization.table.names.commaseparated} - Comma-separated list of tables to clone into tenant schemas</li>
 *   <li>{@code tenant.initialization.scripts.commaseparated} - Comma-separated classpath paths to SQL initialization scripts</li>
 * </ul>
 * All schema operations are guarded as no-ops when {@code is.multitenancy=false}.
 * <p>
 * Thread Pool Execution Model: Per-tenant operations execute in parallel using a bounded thread pool (MAX_THREADS=16) to prevent
 * resource exhaustion. Each task receives isolated thread-local context via {@link TenantResolver}.
 * <p>
 * Execution Patterns:
 * <ul>
 *   <li>Async methods (*ForAllTenants): Return List of Future objects without blocking</li>
 *   <li>Sync methods (*ForAllTenantsAndWait): Block via collect() and return List of results</li>
 *   <li>Single-tenant methods: Convenience wrappers around Stream.of(orgId)</li>
 *   <li>QueryExecutor methods: Provide QueryExecutor parameter to operation</li>
 *   <li>EntityManager methods: Provide EntityManager via QueryExecutor wrapper with optional transaction support</li>
 * </ul>
 * <p>
 * Tenant Lifecycle Workflow:
 * <ol>
 *   <li>Create organization entity (service layer)</li>
 *   <li>Call {@link #createTenant(long)} - creates schema, clones tables, runs initialization scripts</li>
 *   <li>Normal operations - automatic schema routing via {@code SchemaSupportingConnectionProvider}</li>
 *   <li>Delete organization: {@link #markSchemaAsDeleted(long, int)} → {@link #dropSchemaConstraints(long, String, int)} → manual schema drop</li>
 * </ol>
 * <p>
 * State Management:
 * <ul>
 *   <li>{@code tenantedTables} - Immutable list from configuration property (configured tables)</li>
 *   <li>{@code dynamicTenantedTables} - Mutable HashSet for runtime table registration (UNSYNCHRONIZED - caller coordination required)</li>
 * </ul>
 * <p>
 * Thread Safety: Class is stateless except for configuration which is immutable after construction. The {@code dynamicTenantedTables}
 * field requires caller coordination for concurrent modifications. Thread pool creates isolated execution contexts with per-thread
 * {@link TenantResolver} propagation.
 *
 * @author Arkadiusz Drysch (adrysch)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see TenantResolver
 * @see QueryExecutor
 */
@Component
public class MultitenancyService implements LoggingComponentWithRequestId, ReadableCode {

    /**
     * Maximum thread pool size for parallel tenant operations.
     * <p>
     * Bounds thread pool creation to prevent resource exhaustion on systems with many tenants.
     * Used by {@link #executeTasks(long, boolean, List)} to create fixed thread pools.
     * 
     */
    private static final int MAX_THREADS = 16;

    /**
     * Spring-injected property value indicating whether schema-per-tenant multitenancy is enabled.
     * <p>
     * Guards all schema operations - when false, operations like {@link #createTenant(long)},
     * {@link #markSchemaAsDeleted(long, int)}, and {@link #dropSchemaConstraints(long, String, int)} return immediately as no-ops.
     * 
     * <p>
     * Configured via Spring property: {@code is.multitenancy} (default: false)
     * 
     */
    @Value("${is.multitenancy:false}")
    private boolean isMultitenancy;
    
    /**
     * Static copy of {@code isMultitenancy} populated at {@link PostConstruct} for fast static access.
     * <p>
     * Enables checking multitenancy mode without Spring context via {@link #isMultitenancy()}.
     * Thread-safe after initialization.
     * 
     */
    private static boolean isMultitenancyStatic;

    /**
     * Repository for querying active organization IDs.
     * <p>
     * Used by {@link #runForAllTenants(long, boolean, boolean, Function, BiFunction, BiFunction)}
     * to enumerate all tenants for batch operations via {@code findActiveOrganizationIdsAsList()}.
     * 
     */
    @Inject
    OrganizationRepository organizationRepository;
    
    /**
     * Immutable list of table names to clone into tenant schemas during initialization.
     * <p>
     * Populated from Spring property: {@code tenant.initialization.table.names.commaseparated}
     * 
     * <p>
     * Used by {@link #createTenant(long)} to generate CREATE TABLE statements via
     * {@code CREATE TABLE schema.table (LIKE public.table INCLUDING ALL EXCLUDING CONSTRAINTS EXCLUDING INDEXES)}.
     * 
     * <p>
     * Thread-safe: Immutable after construction.
     * 
     */
    List<String> tenantedTables = Collections.emptyList();
    
    /**
     * Mutable set of dynamically registered tenant table names.
     * <p>
     * Modified via {@link #addTenantedTables(List)} and {@link #removeTenantedTables(List)} for runtime table registration.
     * Merged with {@code tenantedTables} during tenant creation.
     * 
     * <p>
     * Thread-safety: UNSYNCHRONIZED HashSet - callers must coordinate concurrent modifications.
     * Typically modified by {@code DynamicEntityRegistrationService} for dynamic entity tables.
     * 
     */
    Set<String> dynamicTenantedTables = new HashSet<>();
    
    /**
     * Immutable list of classpath SQL script paths for tenant initialization.
     * <p>
     * Populated from Spring property: {@code tenant.initialization.scripts.commaseparated}
     * 
     * <p>
     * Used by {@link #createTenant(long)} to execute initialization scripts after schema and table creation.
     * Scripts run in order via {@link QueryExecutor#runQueriesInTransaction(String)}.
     * 
     * <p>
     * Thread-safe: Immutable after construction.
     * 
     */
    List<String> tenantInitializationScripts = Collections.emptyList();
    
    /**
     * QueryExecutor for low-level SQL execution and schema operations.
     * <p>
     * Used by all schema mutation operations including {@link #createTenant(long)},
     * {@link #markSchemaAsDeleted(long, int)}, and {@link #dropSchemaConstraints(long, String, int)}.
     * 
     * <p>
     * Provides transactional DDL execution and EntityManager operation wrappers.
     * 
     */
    QueryExecutor queryExecutor;
    
    /**
     * TenantResolver for managing thread-local tenant context.
     * <p>
     * Used to propagate organization ID context to database connection provider for automatic schema routing.
     * Each tenant operation sets {@code TenantResolver.setTenantedResource(new TenantResolver.TenantedResource(orgId))}
     * before execution.
     * 
     */
    @Inject
    TenantResolver tenantResolver;

    /**
     * Constructs MultitenancyService with injected dependencies and configuration.
     * <p>
     * Parses comma-separated Spring properties into lists of table names and script paths.
     * Whitespace is trimmed from each element after splitting.
     * 
     *
     * @param queryExecutor QueryExecutor for SQL execution (constructor-injected)
     * @param tables Comma-separated table names for tenant schema initialization (property: {@code tenant.initialization.table.names.commaseparated})
     * @param scripts Comma-separated classpath SQL script paths for tenant initialization (property: {@code tenant.initialization.scripts.commaseparated})
     */
    public MultitenancyService(
            QueryExecutor queryExecutor,
            @Value("${tenant.initialization.table.names.commaseparated:}") String tables,
            @Value("${tenant.initialization.scripts.commaseparated:}") String scripts
    ) {
        this.queryExecutor = queryExecutor;
        if (StringUtils.isNotBlank(scripts)) {
            tenantInitializationScripts = Arrays.stream(scripts.split(",")).map(a -> StringUtils.trim(a)).collect(Collectors.toList());
        }
        if (StringUtils.isNotBlank(tables)) {
            tenantedTables = Arrays.stream(tables.split(",")).map(a -> StringUtils.trim(a)).collect(Collectors.toList());
        }
    }

    /**
     * Spring PostConstruct initialization copying {@code isMultitenancy} to static field.
     * <p>
     * Enables fast static access to multitenancy mode via {@link #isMultitenancy()} without requiring Spring context.
     * Called once by Spring container after dependency injection and before bean exposure.
     * 
     * <p>
     * Thread-safety: Executed by single thread during bean initialization before any concurrent access.
     * 
     */
    @PostConstruct
    void init() {
        isMultitenancyStatic = isMultitenancy;
    }

    /**
     * Static accessor for multitenancy mode.
     * <p>
     * Returns the current multitenancy configuration without requiring Spring context access.
     * Safe to call from static contexts after Spring container initialization.
     * 
     *
     * @return true if schema-per-tenant multitenancy is enabled, false for single-schema mode
     */
    public static boolean isMultitenancy() {
        return isMultitenancyStatic;
    }

    /**
     * Synchronously collects results from Future objects via blocking {@link Future#get()} calls.
     * <p>
     * Iterates through all Futures and blocks until each result is available. Results maintain
     * the same order as the input Future list.
     * 
     *
     * @param r List of Future objects to collect results from (null-safe, returns empty list if null)
     * @param <T> Result type
     * @return List of results in same order as input Futures, or empty list if input is null
     * @throws RuntimeException Wrapping InterruptedException or ExecutionException from Future.get()
     */
    protected <T> List<T> collect(List<Future<T>> r) {
        if (r == null) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(r.size());
        try {
            for (Future<T> f : r) {
                result.add(f.get());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Adds table names to {@code dynamicTenantedTables} for runtime tenant schema management.
     * <p>
     * Tables added via this method will be included in future calls to {@link #createTenant(long)},
     * ensuring they are cloned into new tenant schemas alongside configured tables.
     * 
     * <p>
     * Use case: Dynamic entity generation via {@code DynamicEntityRegistrationService} adds generated
     * table names to ensure they are replicated in tenant schemas.
     * 
     * <p>
     * Thread-safety: UNSYNCHRONIZED - caller must coordinate concurrent calls to avoid race conditions.
     * 
     *
     * @param tableNames List of table names to add to dynamic tenant table registry
     */
    public void addTenantedTables(List<String> tableNames) {
        this.dynamicTenantedTables.addAll(tableNames);
    }
    
    /**
     * Removes table names from {@code dynamicTenantedTables}.
     * <p>
     * Removed tables will no longer be cloned into tenant schemas during subsequent {@link #createTenant(long)} calls.
     * 
     * <p>
     * Thread-safety: UNSYNCHRONIZED - caller must coordinate concurrent calls to avoid race conditions.
     * 
     *
     * @param tableNames List of table names to remove from dynamic tenant table registry
     */
    public void removeTenantedTables(List<String> tableNames) {
        this.dynamicTenantedTables.removeAll(tableNames);
    }

    /**
     * Executes function for all active tenants asynchronously without blocking.
     * <p>
     * Enumerates all active organizations via {@code OrganizationRepository.findActiveOrganizationIdsAsList()}
     * and executes the provided function for each in parallel using a bounded thread pool.
     * 
     *
     * @param timeoutInMilliseconds Maximum execution time for all tenant operations
     * @param operation Function accepting organizationId and returning result
     * @param <T> Result type
     * @return List of Future objects representing async results (one per tenant) - does not block
     */
    public <T> List<Future<T>> runForAllTenants(long timeoutInMilliseconds, Function<Long, T> operation)  {
            return runForAllTenants(timeoutInMilliseconds, false, false, operation, null, null);
    }

    /**
     * Executes function for all active tenants synchronously, blocking until completion.
     * <p>
     * Enumerates all active organizations and executes the provided function for each in parallel,
     * then blocks via {@link #collect(List)} until all operations complete.
     * 
     *
     * @param timeoutInMilliseconds Maximum execution time for all tenant operations
     * @param operation Function accepting organizationId and returning result
     * @param <T> Result type
     * @return List of results (one per tenant) in completion order
     * @throws RuntimeException If any tenant operation fails or timeout occurs
     */
    public <T> List<T> runForAllTenantsAndWait(long timeoutInMilliseconds, Function<Long, T> operation)  {
            return collect(runForAllTenants(timeoutInMilliseconds, false, true, operation, null, null));
    }

    /**
     * Executes {@link QueryExecutor} operation for all active tenants asynchronously.
     * <p>
     * Provides {@link QueryExecutor} parameter to operation for low-level SQL execution and schema operations.
     * Useful for per-tenant SQL queries or schema introspection.
     * 
     *
     * @param timeoutInMilliseconds Maximum execution time for all tenant operations
     * @param operation BiFunction accepting QueryExecutor and organizationId, returning result
     * @param <T> Result type
     * @return List of Future results (one per tenant) - does not block
     */
    public <T> List<Future<T>> runQueryExecutorForAllTenants(long timeoutInMilliseconds, BiFunction<QueryExecutor, Long, T> operation)  {
            return runForAllTenants(timeoutInMilliseconds, false, false, null, operation, null);
    }

    /**
     * Executes {@link QueryExecutor} operation for all active tenants synchronously.
     * <p>
     * Blocks until all tenant operations complete. Each operation receives QueryExecutor for SQL execution.
     * 
     *
     * @param timeoutInMilliseconds Maximum execution time for all tenant operations
     * @param operation BiFunction accepting QueryExecutor and organizationId, returning result
     * @param <T> Result type
     * @return List of results (one per tenant) in completion order
     * @throws RuntimeException If any tenant operation fails
     */
    public <T> List<T> runQueryExecutorForAllTenantsAndWait(long timeoutInMilliseconds, BiFunction<QueryExecutor, Long, T> operation)  {
        return collect(runForAllTenants(timeoutInMilliseconds, false, true, null, operation, null));
    }

    /**
     * Executes {@link EntityManager} operation for all active tenants asynchronously without transaction.
     * <p>
     * Provides EntityManager via {@link QueryExecutor#runEntityManagerOperationForOrg(Long, boolean, BiFunction)}
     * with transactional=false. Operations execute in auto-commit mode.
     * 
     *
     * @param timeoutInMilliseconds Maximum execution time for all tenant operations
     * @param operation BiFunction accepting EntityManager and organizationId, returning result
     * @param <T> Result type
     * @return List of Future results (one per tenant) - does not block
     */
    public <T> List<Future<T>> runEntityManagerForAllTenants(long timeoutInMilliseconds, BiFunction<EntityManager, Long, T> operation)  {
        return runForAllTenants(timeoutInMilliseconds, false, false, null, null, operation);
    }

    /**
     * Executes {@link EntityManager} operation for all active tenants synchronously without transaction.
     * <p>
     * Blocks until all tenant operations complete. Operations execute in auto-commit mode.
     * 
     *
     * @param timeoutInMilliseconds Maximum execution time for all tenant operations
     * @param operation BiFunction accepting EntityManager and organizationId, returning result
     * @param <T> Result type
     * @return List of results (one per tenant) in completion order
     * @throws RuntimeException If any tenant operation fails
     */
    public <T> List<T> runEntityManagerForAllTenantsAndWait(long timeoutInMilliseconds, BiFunction<EntityManager, Long, T> operation)  {
            return collect(runForAllTenants(timeoutInMilliseconds, false, true, null, null, operation));
    }

    /**
     * Executes {@link EntityManager} operation for all active tenants asynchronously in transactions.
     * <p>
     * Each tenant operation is wrapped in a separate transaction via {@link QueryExecutor#runEntityManagerOperationForOrg(Long, boolean, BiFunction)}
     * with transactional=true.
     * 
     *
     * @param timeoutInMilliseconds Maximum execution time for all tenant operations
     * @param operation BiFunction accepting EntityManager and organizationId, returning result
     * @param <T> Result type
     * @return List of Future results (one per tenant) - does not block
     */
    public <T> List<Future<T>> runEntityManagerForAllTenantsInTransaction(long timeoutInMilliseconds, BiFunction<EntityManager, Long, T> operation)  {
        return runForAllTenants(timeoutInMilliseconds, true, false, null, null, operation);
    }

    /**
     * Executes {@link EntityManager} operation for all active tenants synchronously in transactions.
     * <p>
     * Blocks until all tenant transactions complete. Each tenant operation runs in its own transaction.
     * 
     *
     * @param timeoutInMilliseconds Maximum execution time for all tenant operations
     * @param operation BiFunction accepting EntityManager and organizationId, returning result
     * @param <T> Result type
     * @return List of results (one per tenant) in completion order
     * @throws RuntimeException If any tenant transaction fails
     */
    public <T> List<T> runEntityManagerForAllTenantsAndWaitInTransaction(long timeoutInMilliseconds, BiFunction<EntityManager, Long, T> operation)  {
            return collect(runForAllTenants(timeoutInMilliseconds, true, true, null, null, operation));
    }

    /**
     * Executes function for specific tenants synchronously, blocking until completion.
     * <p>
     * Convenience method for batch operations on a subset of tenants rather than all active organizations.
     * 
     *
     * @param timeoutInMilliseconds Maximum execution time for all specified tenant operations
     * @param orgIds Collection of organization IDs to process
     * @param f Function accepting organizationId and returning result
     * @param <T> Result type
     * @return List of results (one per specified tenant) in completion order
     * @throws RuntimeException If any tenant operation fails
     */
    public <T> List<T> runForTenantsAndWait(long timeoutInMilliseconds, Collection<Long> orgIds, Function<Long, T> f) {
        return collect(runForTenants(timeoutInMilliseconds, false, true, orgIds.stream(), f, null, null));
    }

    /**
     * Executes function for single tenant synchronously, blocking until completion.
     * <p>
     * Convenience wrapper around {@link #runForTenants(long, boolean, boolean, Stream, Function, BiFunction, BiFunction)}
     * for single organization operations.
     * 
     *
     * @param timeoutInMilliseconds Maximum execution time
     * @param orgId Organization ID to process
     * @param f Function accepting organizationId and returning result
     * @param <T> Result type
     * @return List containing single result
     * @throws RuntimeException If tenant operation fails
     */
    public <T> List<T> runForTenantAndWait(long timeoutInMilliseconds, Long orgId, Function<Long, T> f) {
        return collect(runForTenants(timeoutInMilliseconds, false, true, Stream.of(orgId), f, null, null));
    }

    /**
     * Executes {@link QueryExecutor} operation for single tenant synchronously.
     * <p>
     * Convenience method for single-tenant schema operations or SQL execution with QueryExecutor access.
     * 
     *
     * @param timeoutInMilliseconds Maximum execution time
     * @param orgId Organization ID to process
     * @param operation BiFunction accepting QueryExecutor and organizationId, returning result
     * @param <T> Result type
     * @return List containing single result
     * @throws RuntimeException If tenant operation fails
     */
    public <T> List<T> runQueryExecutorForTenantAndWait(long timeoutInMilliseconds, Long orgId, BiFunction<QueryExecutor, Long, T> operation)  {
        return collect(runForTenants(timeoutInMilliseconds, false, true, Stream.of(orgId), null, operation, null));
    }


    /**
     * Creates PostgreSQL schema and initializes tenant-specific tables for new organization.
     * <p>
     * Implementation steps:
     * <ol>
     *   <li>Returns false if {@code isMultitenancy} is disabled (guard clause)</li>
     *   <li>Sets {@link TenantResolver} context to organizationId for connection routing</li>
     *   <li>Creates schema {@code org_[organizationId]} via CREATE SCHEMA DDL</li>
     *   <li>Clones tables from {@code tenantedTables} and {@code dynamicTenantedTables} using
     *       {@code CREATE TABLE schema.table (LIKE public.table INCLUDING ALL EXCLUDING CONSTRAINTS EXCLUDING INDEXES)}</li>
     *   <li>Executes tenant initialization scripts from {@code tenantInitializationScripts} in order</li>
     * </ol>
     * 
     * <p>
     * Database: PostgreSQL-specific DDL (CREATE SCHEMA, LIKE INCLUDING ALL syntax).
     * 
     * <p>
     * Thread-safety: Sets thread-local TenantResolver context before schema operations.
     * 
     * <p>
     * Error handling: Transaction rollback via {@link QueryExecutor#runQueriesInTransaction(String...)} on failure.
     * 
     *
     * @param organizationId Newly created organization ID requiring tenant setup
     * @return true if tenant created successfully, false if multitenancy disabled (no-op)
     */
    public boolean createTenant(long organizationId) {
        if (not(isMultitenancy)) {
            return false;
        }

        TenantResolver.setTenantedResource(new TenantResolver.TenantedResource(organizationId));
        debug("[createTenant] org {} set tenanted resource to {}", organizationId, TenantResolver.getTenantedResource());

        String schemaName = "org_" + organizationId;
        queryExecutor.runQueriesInTransaction(String.format("create schema %s", schemaName));
        debug("[createTenant] org {} created schema {}", organizationId, schemaName);

        String[] tenantTablesDDLs = Stream.concat(tenantedTables.stream(), dynamicTenantedTables.stream())
                .map(a -> String.format("create table %s.%s (like public.%s including all excluding constraints excluding indexes)", schemaName, a, a)).toArray(String[]::new);

        for(String tt : tenantTablesDDLs) {
            debug("[createTenant] org {} running {}", organizationId, tt);
            queryExecutor.runQueriesInTransaction(tt);
        }

        for(String s : tenantInitializationScripts) {
            String queryString = queryExecutor.readResource(s);
            debug("[createTenant] org {} running {}", organizationId, s);
            queryExecutor.runQueriesInTransaction(queryString);
        }

        debug("[createTenant] org {} tenant creation completed", organizationId);
        return true;
    }

    /**
     * Renames tenant schema from {@code org_[id]} to {@code deleted_[id]} for soft delete.
     * <p>
     * First step in organization deletion workflow. Schema is preserved but isolated from active tenants.
     * Calls database procedure {@code rename_schema(old_name, new_name)} to perform rename.
     * 
     * <p>
     * Thread-safety: Sets {@link TenantResolver} context with datasource hint before operation.
     * 
     * <p>
     * Use case: Organization deletion workflow - schema preserved for audit/recovery before final removal.
     * Follow with {@link #dropSchemaConstraints(long, String, int)} to prepare for schema drop.
     * 
     *
     * @param organizationId Organization ID being marked for deletion
     * @param assignedDatasource Datasource index where schema exists (for multi-datasource setups)
     * @return New schema name {@code deleted_[id]}, or null if multitenancy disabled
     */
    public String markSchemaAsDeleted(long organizationId, int assignedDatasource) {
        if (not(isMultitenancy)) {
            return null;
        }
        TenantResolver.setTenantedResource(new TenantResolver.TenantedResource(assignedDatasource));
        debug("[markSchemeAsDeleted] org {} set tenanted resource to {}", organizationId, TenantResolver.getTenantedResource());

        String schemaName = "org_" + organizationId;
        String newSchemaName = "deleted_" + organizationId;

        queryExecutor.runQueriesInTransaction(String.format("call rename_schema('%s', '%s');", schemaName, newSchemaName));
        debug("[markSchemeAsDeleted] org {} renamed schema {} to {}", organizationId, schemaName, newSchemaName);


        return newSchemaName;
    }

    /**
     * Drops all constraints from tenant schema to prepare for removal.
     * <p>
     * Second step in organization deletion after {@link #markSchemaAsDeleted(long, int)}.
     * Calls database procedure {@code remove_all_constraints_in_schema(schema_name)} to drop foreign key
     * and check constraints, enabling subsequent schema drop without dependency violations.
     * 
     * <p>
     * Thread-safety: Sets {@link TenantResolver} context with datasource hint before operation.
     * 
     * <p>
     * Purpose: Removes foreign key constraints that would prevent schema drop, allowing clean removal.
     * 
     *
     * @param organizationId Organization ID (for logging and context)
     * @param schemaName Schema name to drop constraints from (typically {@code deleted_[id]} after soft delete)
     * @param assignedDatasource Datasource index where schema exists
     * @return true if constraints dropped successfully, false if multitenancy disabled
     */
    public boolean dropSchemaConstraints(long organizationId, String schemaName, int assignedDatasource) {
        if (not(isMultitenancy)) {
            return false;
        }
        TenantResolver.setTenantedResource(new TenantResolver.TenantedResource(assignedDatasource));
        debug("[dropSchemaConstraints] org {} set tenanted resource to {}", organizationId, TenantResolver.getTenantedResource());

        queryExecutor.runQueriesInTransaction(String.format("call remove_all_constraints_in_schema('%s');", schemaName));
        debug("[dropSchemaConstraints] org {} dropped schema {} constraints", organizationId, schemaName);

        return true;
    }

    /**
     * Core thread pool execution method for parallel tenant operations.
     * <p>
     * Creates a fixed thread pool with {@code min(tasks.size(), MAX_THREADS)} threads and executes all tasks
     * with specified timeout. Thread pool is shut down after task submission.
     * 
     * <p>
     * Lifecycle:
     * <ol>
     *   <li>Create fixed thread pool bounded to MAX_THREADS</li>
     *   <li>Submit all tasks via {@code invokeAll} with timeout</li>
     *   <li>Immediately call {@code shutdown()} to prevent new task submissions</li>
     *   <li>If wait=true, call {@code awaitTermination} to block until completion or timeout</li>
     * </ol>
     * 
     * <p>
     * Error handling: Empty task list logged as warning and returns empty list.
     * 
     *
     * @param timeoutInMilliseconds Maximum execution time for all tasks
     * @param wait If true, blocks until all tasks complete; if false, returns Futures immediately
     * @param tasks List of Callable tasks to execute in parallel
     * @param <T> Result type
     * @return List of Future results (one per task)
     * @throws InterruptedException If thread interrupted during execution or termination await
     */
    protected  <T> List<Future<T>> executeTasks(long timeoutInMilliseconds, boolean wait, List<Callable<T>> tasks) throws InterruptedException {
        debug("[executeTasks]");
        if (tasks == null || tasks.isEmpty()) {
            warn("[executeTasks] empty tasks list");
            return Collections.emptyList();
        }
        ExecutorService tp = Executors.newFixedThreadPool(Math.min(tasks.size(), MAX_THREADS ));
        List<Future<T>> result = tp.invokeAll(tasks, timeoutInMilliseconds, TimeUnit.MILLISECONDS);
        tp.shutdown();
        if (wait) {
            tp.awaitTermination(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
        }
        return result;
    }

    /**
     * Private core method enumerating all active tenants and delegating to {@link #runForTenants}.
     * <p>
     * Queries {@link OrganizationRepository#findActiveOrganizationIdsAsList()} to enumerate all active
     * organizations, then delegates to runForTenants for parallel execution.
     * 
     *
     * @param timeoutInMilliseconds Maximum execution time for all tenant operations
     * @param transactional If true, EntityManager operations wrapped in transactions via QueryExecutor
     * @param wait If true, blocks until completion via {@link #collect(List)}
     * @param f Optional Function for simple tenant operations (orgId -> result)
     * @param qef Optional BiFunction for QueryExecutor operations (QueryExecutor, orgId -> result)
     * @param emf Optional BiFunction for EntityManager operations (EntityManager, orgId -> result)
     * @param <T> Result type
     * @return List of Future results (one per active tenant)
     */
    private <T> List<Future<T>> runForAllTenants(long timeoutInMilliseconds, boolean transactional, boolean wait, Function<Long, T> f, BiFunction<QueryExecutor, Long, T> qef, BiFunction<EntityManager, Long, T> emf) {
        Stream<Long> ids = organizationRepository.findActiveOrganizationIdsAsList().stream();
        return runForTenants(timeoutInMilliseconds, transactional, wait, ids, f, qef, emf);
    }

    /**
     * Private core orchestration method building Callable tasks and executing via thread pool.
     * <p>
     * For each organization ID, builds a Callable that:
     * <ol>
     *   <li>Sets {@link TenantResolver#setTenantedResource(TenantResolver.TenantedResource)} with orgId</li>
     *   <li>Invokes {@code f(orgId)} if f provided (simple function execution)</li>
     *   <li>Invokes {@code qef(queryExecutor, orgId)} if qef provided (QueryExecutor operation)</li>
     *   <li>Invokes {@code queryExecutor.runEntityManagerOperationForOrg(orgId, transactional, emf)} if emf provided</li>
     * </ol>
     * All tasks execute in parallel via {@link #executeTasks(long, boolean, List)}.
     * 
     * <p>
     * Error handling: Catches all exceptions, logs error, wraps in RuntimeException.
     * 
     * <p>
     * Thread-safety: Each task sets its own thread-local {@link TenantResolver} context for isolation.
     * 
     *
     * @param timeoutInMilliseconds Maximum execution time for all tenant operations
     * @param transactional If true, EntityManager operations execute in transactions
     * @param wait If true, blocks until all tasks complete
     * @param orgIds Stream of organization IDs to process
     * @param f Optional Function for simple operations (orgId -> result)
     * @param qef Optional BiFunction for QueryExecutor operations (QueryExecutor, orgId -> result)
     * @param emf Optional BiFunction for EntityManager operations (EntityManager, orgId -> result)
     * @param <T> Result type
     * @return List of Future results (one per organization)
     * @throws RuntimeException Wrapping any execution exceptions
     */
    private <T> List<Future<T>> runForTenants(long timeoutInMilliseconds, boolean transactional, boolean wait, Stream<Long> orgIds, Function<Long, T> f, BiFunction<QueryExecutor, Long, T> qef, BiFunction<EntityManager, Long, T> emf) {
        try {
            List<Callable<T>> tasks = new ArrayList<>();
            if (orgIds != null) {
                orgIds.forEach((orgId) ->
                        tasks.add(() -> {
                            TenantResolver.setTenantedResource(new TenantResolver.TenantedResource(orgId));
                            if (f != null) {
                                return f.apply(orgId);
                            }
                            if (qef != null) {
                                return qef.apply(queryExecutor, orgId);
                            }
                            if (emf != null) {
                                return queryExecutor.runEntityManagerOperationForOrg(orgId, transactional, emf);
                            }
                            return null;
                        })
                );
            }
            return executeTasks(timeoutInMilliseconds, wait, tasks);
        } catch (Exception e) {
            error("[runForAllTenants]", e);
            throw new RuntimeException(e);
        }
    }
}
