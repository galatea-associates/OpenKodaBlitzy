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

import com.openkoda.App;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.security.HasSecurityRules;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import static com.openkoda.controller.common.URLConstants._HTML;

/**
 * REST controller exposing HTTP endpoints for system health monitoring, database validation, 
 * JavaScript thread management, and application lifecycle control.
 * <p>
 * Concrete {@code @RestController} implementing admin system diagnostics interface. Maps HTTP 
 * requests to Flow-based helper methods inherited from {@link AbstractSystemHealthController}. 
 * Provides system health dashboard (systemHealth), database schema validation (validateDatabase), 
 * GraalVM JS thread monitoring (threads), thread lifecycle control (threadInterrupt, threadRemove), 
 * admin dashboard landing page (adminDashboard), component registry (components), and application 
 * restart (restart). Endpoints require varying privilege levels from read (diagnostics) to manage (restart).
 * 
 * <p>
 * Request mapping: Base path "/html" (URLConstants._HTML)
 * 
 * <p>
 * Security: Implements {@link HasSecurityRules} for privilege enforcement. System health requires 
 * CHECK_CAN_READ_SUPPORT_DATA, thread/backend operations require CHECK_CAN_READ_BACKEND or 
 * CHECK_CAN_MANAGE_BACKEND.
 * 
 * <p>
 * Response types: Returns HTML views via {@link ModelAndView} for UI display, HTMX fragments 
 * for AJAX updates, null for restart (application terminating).
 * 
 * <p>
 * Inheritance: Extends {@link AbstractSystemHealthController} for Flow-based health monitoring 
 * and thread management helpers.
 * 
 * <p>
 * System health context: Monitors JVM metrics, database schema compliance, server-side JavaScript 
 * execution threads.
 * 
 * <p>
 * Thread-safety: Stateless, thread-safe. ServerJSProcessRunner operations synchronized internally.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * See {@code AbstractSystemHealthController}
 * See {@code SystemStatusService}
 * See {@code com.openkoda.core.customisation.ServerJSProcessRunner}
 */
@RestController
@RequestMapping(_HTML)
public class SystemHealthController extends AbstractSystemHealthController implements HasSecurityRules {

    /**
     * Displays system health dashboard with JVM and database metrics.
     * <p>
     * HTTP mapping: GET /html/system-health
     * 
     * <p>
     * Security: {@code @PreAuthorize(CHECK_CAN_READ_SUPPORT_DATA)} - requires support data read privilege.
     * 
     * <p>
     * Model populated by getSystemHealth() with key "systemHealthStatus" containing Map with JVM memory 
     * (heap, non-heap MB), thread counts (active, peak), database connection pool stats, cache hit rates.
     * 
     *
     * @return Object resolving to ModelAndView with view name "system-health". Used by admin dashboard 
     *         system health monitoring tab
     */
    @PreAuthorize(CHECK_CAN_READ_SUPPORT_DATA)
    @GetMapping(_SYSTEM_HEATH)
    public Object systemHealth() {
        debug("[systemHealth]");
        return getSystemHealth()
                .mav("system-health");
    }

    /**
     * Generates database schema validation script via AJAX.
     * <p>
     * HTTP mapping: GET /html/system-health/validate
     * 
     * <p>
     * Security: {@code @PreAuthorize(CHECK_CAN_READ_SUPPORT_DATA)}
     * 
     * <p>
     * Model populated by validate() with key "databaseUpdateScript" containing SQL DDL statements to 
     * reconcile schema drift (JPA entities vs actual DB structure). Script is read-only (not executed), 
     * enables schema compliance diagnostics.
     * 
     *
     * @return Object resolving to HTMX fragment "system-health::database-validation". Used by AJAX 
     *         request from system health UI "Validate Database" button
     */
    @PreAuthorize(CHECK_CAN_READ_SUPPORT_DATA)
    @GetMapping(_SYSTEM_HEATH + _VALIDATE)
    public Object validateDatabase() {
        debug("[validateDatabase]");
        return validate()
                .mav("system-health::database-validation");
    }

    /**
     * Displays server-side JavaScript thread monitoring UI.
     * <p>
     * HTTP mapping: GET /html/threads
     * 
     * <p>
     * Security: {@code @PreAuthorize(CHECK_CAN_READ_BACKEND)} - requires backend read privilege.
     * 
     * <p>
     * Model populated by getThreads() with key "serverJsThreads" containing List of GraalVM JS thread 
     * metadata (thread ID, script name, start timestamp, execution status).
     * 
     *
     * @return Object resolving to ModelAndView with view name "threads". Used by admin dashboard JS 
     *         thread monitoring tab for debugging long-running server-side scripts
     */
    @PreAuthorize(CHECK_CAN_READ_BACKEND)
    @GetMapping(_THREAD)
    public Object threads() {
        debug("[threads]");
        return getThreads()
                .mav("threads");
    }

    /**
     * Interrupts running JavaScript execution thread.
     * <p>
     * HTTP mapping: POST /html/threads/{id}/interrupt
     * 
     * <p>
     * Path variable: {id} mapped to threadId Long parameter.
     * 
     * <p>
     * Security: {@code @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)} - requires backend management privilege.
     * 
     * <p>
     * Executes interruptThread(threadId) which sets interrupt flag via ServerJSProcessRunner.interruptThread(threadId). 
     * Thread terminates at next interruptible operation.
     * 
     *
     * @param threadId unique identifier of thread to interrupt (from serverJsThreads list)
     * @return Object resolving to ModelAndView with view name "threads", model refreshed after interrupt. 
     *         Used by admin UI "Interrupt" button for terminating runaway JS scripts
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @PostMapping(_THREAD_ID_INTERRUPT)
    public Object threadInterrupt(@PathVariable(ID) Long threadId) {
        debug("[threadInterrupt]");
        return interruptThread(threadId)
                .mav("threads");
    }

    /**
     * Removes JavaScript thread from monitoring registry.
     * <p>
     * HTTP mapping: POST /html/threads/{id}/remove
     * 
     * <p>
     * Path variable: {id} mapped to threadId Long parameter.
     * 
     * <p>
     * Security: {@code @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)}
     * 
     * <p>
     * Executes removeThread(threadId) which calls ServerJSProcessRunner.removeJsThread(threadId) to 
     * remove thread metadata. Only removes completed/interrupted threads (not active).
     * 
     *
     * @param threadId unique identifier of thread to remove (from serverJsThreads list)
     * @return Object resolving to ModelAndView with view name "threads", model refreshed after removal. 
     *         Used by admin UI "Remove" button for cleanup
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @PostMapping(_THREAD_ID_REMOVE)
    public Object threadRemove(@PathVariable(ID) Long threadId) {
        debug("[threadInterrupt]");
        return removeThread(threadId)
                .mav("threads");
    }

    /**
     * Displays admin dashboard landing page.
     * <p>
     * HTTP mapping: GET /html/dashboard
     * 
     * <p>
     * Security: {@code @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)}
     * 
     * <p>
     * Landing page contains links to logs, audit, system health, threads, integrations.
     * 
     *
     * @param pageable pagination parameters qualified as "obj" (currently unused)
     * @return ModelAndView with view name "admin-dashboard" (empty model). Used as main admin UI entry point
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @GetMapping(_DASHBOARD)
    public Object adminDashboard(@Qualifier("obj") Pageable pageable) {
        return new ModelAndView("admin-dashboard");

    }

    /**
     * Displays component registry UI (module/bean listing).
     * <p>
     * HTTP mapping: GET /html/components
     * 
     * <p>
     * Security: {@code @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)}
     * 
     *
     * @return Object resolving to ModelAndView with view name "components" (empty model via Flow.init().execute()). 
     *         Used by admin dashboard components tab for viewing registered Spring beans and modules
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @GetMapping(_COMPONENTS)
    public Object components() {
        return Flow.init()
                .execute()
                .mav("components");

    }

    /**
     * Initiates graceful application shutdown.
     * <p>
     * HTTP mapping: GET /html/restart
     * 
     * <p>
     * Security: {@code @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)}
     * 
     * <p>
     * Executes App.shutdown() which triggers Spring context shutdown. Application must be managed by 
     * external process manager (systemd, Docker) for automatic restart.
     * 
     *
     * @return null (application terminating, no response sent). Used by admin UI "Restart Application" 
     *         button for applying configuration changes requiring restart
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @GetMapping("/restart")
    public Object restart() {
        App.shutdown();
        return null;

    }

}
