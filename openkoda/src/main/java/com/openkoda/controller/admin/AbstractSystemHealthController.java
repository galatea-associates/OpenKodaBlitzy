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

package com.openkoda.controller.admin;

import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.customisation.ServerJSProcessRunner;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageModelMap;

/**
 * Abstract base controller providing Flow-based helper methods for system health monitoring,
 * database validation, and server-side JavaScript thread management.
 * <p>
 * Stateless abstract controller implementing system diagnostics and monitoring functionality.
 * Provides system health status collection ({@link #getSystemHealth()}), database schema validation
 * ({@link #validate()}), GraalVM JS thread listing ({@link #getThreads()}), and thread lifecycle
 * management ({@link #interruptThread(Long)}, {@link #removeThread(Long)}). Designed for reuse by
 * concrete controllers that handle HTTP bindings and view resolution for admin monitoring dashboards.
 * 
 * <p>
 * System health context: Monitors JVM metrics (memory, threads, CPU), database connection status,
 * and cache hit rates via {@code services.systemStatus}.
 * 
 * <p>
 * Database validation: Generates DDL update scripts for schema drift detection via
 * {@code services.databaseValidationService}.
 * 
 * <p>
 * JavaScript thread management: Controls server-side GraalVM JS execution threads via
 * {@link ServerJSProcessRunner} singleton.
 * 
 * <p>
 * Thread-safety: Stateless, thread-safe. {@link ServerJSProcessRunner} operations are
 * synchronized internally.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * See {@code SystemHealthController} for concrete HTTP endpoint implementation
 * See {@code SystemStatusService} for health metrics collection
 * See {@code ServerJSProcessRunner} for GraalVM JS thread registry
 */
public class AbstractSystemHealthController extends AbstractController {

    /**
     * Retrieves current system health metrics including JVM, database, and cache statistics.
     * <p>
     * Collects comprehensive system health information via {@code services.systemStatus.statusNow()}
     * and populates the model for admin dashboard display. Metrics include JVM memory usage (heap
     * and non-heap in MB), active and peak thread counts, database connection pool statistics,
     * and cache hit rate metrics.
     * 
     * <p>
     * Example usage:
     * <pre>
     * PageModelMap model = getSystemHealth();
     * Map healthStatus = model.get("systemHealthStatus");
     * </pre>
     * 
     *
     * @return {@link PageModelMap} with model key "systemHealthStatus" containing {@code Map}
     *         with JVM memory usage (heap, non-heap), thread counts, database connection pool
     *         stats, and cache metrics
     */
    protected PageModelMap getSystemHealth(){
        return Flow.init()
                .thenSet(systemHealthStatus, a -> services.systemStatus.statusNow())
                .execute();
    }
    /**
     * Generates database schema validation script for detecting schema drift.
     * <p>
     * Executes {@code services.databaseValidationService.getUpdateScript(true)} with detailed
     * diff enabled to produce SQL DDL statements that reconcile differences between JPA entity
     * definitions and the actual database structure. The generated script is read-only and not
     * automatically executed, enabling safe schema compliance diagnostics.
     * 
     * <p>
     * Used by admin endpoints to display schema drift without modifying the database.
     * 
     *
     * @return {@link PageModelMap} with model key "databaseUpdateScript" containing SQL DDL
     *         statements to reconcile schema drift between JPA entities and actual database structure
     */
    protected PageModelMap validate(){
        return Flow.init()
                .thenSet(databaseUpdateScript, a -> services.databaseValidationService.getUpdateScript(true))
                .execute();
    }

    /**
     * Retrieves list of active server-side JavaScript execution threads.
     * <p>
     * Queries {@link ServerJSProcessRunner#getServerJsThreads()} to obtain metadata for all
     * tracked GraalVM JavaScript execution threads. Thread metadata includes thread ID, script
     * name, start timestamp, and execution status. Used by admin monitoring UI to track and
     * debug long-running server-side JavaScript scripts.
     * 
     *
     * @return {@link PageModelMap} with model key "serverJsThreads" containing {@code List}
     *         of thread metadata (thread ID, script name, start time, status)
     */
    protected PageModelMap getThreads(){
        return Flow.init()
               .thenSet(serverJsThreads,  a ->ServerJSProcessRunner.getServerJsThreads())
                .execute();
    }

    /**
     * Interrupts a running JavaScript execution thread.
     * <p>
     * Executes {@link ServerJSProcessRunner#interruptThread(Long)} which sets the interrupt
     * flag on the specified thread. The thread will terminate at the next interruptible
     * operation, allowing for graceful termination of long-running server-side JavaScript
     * scripts. Used by admin UI to stop runaway or hung JavaScript executions.
     * 
     *
     * @param threadId unique identifier of the thread to interrupt (obtained from serverJsThreads list)
     * @return {@link PageModelMap} (empty model, operation performs side-effect only)
     */
    protected PageModelMap interruptThread(Long threadId){
        return Flow.init()
                .then(a -> ServerJSProcessRunner.interruptThread(threadId))
                .execute();
    }

    /**
     * Removes a JavaScript thread from the monitoring registry.
     * <p>
     * Executes {@link ServerJSProcessRunner#removeJsThread(Long)} which removes thread metadata
     * from the tracking registry. This operation should only be performed on completed or
     * interrupted threads, not on actively executing threads. Used by admin UI for cleanup of
     * finished thread entries from the monitoring list.
     * 
     *
     * @param threadId unique identifier of the thread to remove (obtained from serverJsThreads list)
     * @return {@link PageModelMap} (empty model, operation performs side-effect only)
     */
    protected PageModelMap removeThread(Long threadId){
        return Flow.init()
                .then(a -> ServerJSProcessRunner.removeJsThread(threadId))
                .execute();
    }
}
