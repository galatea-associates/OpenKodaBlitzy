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

package com.openkoda.core.controller;

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.repository.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Lightweight Spring REST controller exposing a public health and liveness endpoint for monitoring systems.
 * <p>
 * This controller provides a {@code GET /ping} endpoint designed for load balancers, orchestration platforms
 * (such as Kubernetes), and monitoring systems to verify application health. The endpoint performs diagnostic
 * checks on critical system components and returns their status with execution timing metrics.
 * <p>
 * <b>Response Behavior:</b>
 * <ul>
 * <li>HTTP 200 (OK) - All monitored components are healthy and operational</li>
 * <li>HTTP 503 (SERVICE_UNAVAILABLE) - One or more components have failed health checks</li>
 * </ul>
 * <p>
 * <b>Security Considerations:</b><br>
 * This endpoint is intentionally public and does NOT require authentication. It returns only non-sensitive
 * diagnostic information suitable for exposure to monitoring infrastructure. Do not add checks that reveal
 * sensitive system details or credentials.
 * <p>
 * <b>Response Format:</b><br>
 * The endpoint returns a {@link LinkedHashMap} preserving insertion order with per-component status:
 * <pre>{@code
 * {
 *   "db": "OK",           // Database connectivity status
 *   "dbTime": "45"        // Database check execution time in milliseconds
 * }
 * }</pre>
 * <p>
 * <b>Example Load Balancer Configuration (HAProxy):</b>
 * <pre>{@code
 * option httpchk GET /ping
 * http-check expect status 200
 * }</pre>
 * <p>
 * <b>Example Kubernetes Liveness Probe:</b>
 * <pre>{@code
 * livenessProbe:
 *   httpGet:
 *     path: /ping
 *     port: 8080
 *   initialDelaySeconds: 30
 *   periodSeconds: 10
 * }</pre>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see LoggingComponentWithRequestId
 * @see UserRepository
 */
@RestController
public class ApplicationStatusController implements LoggingComponentWithRequestId {

    @Autowired
    UserRepository userRepository;

    /**
     * Wraps a health check operation with execution time measurement and exception handling.
     * <p>
     * This utility method executes the provided health check supplier, measures its execution time
     * using {@link System#currentTimeMillis()}, and records both the status and timing in the
     * diagnostic log map. Any exceptions thrown during execution are caught and converted to an
     * "ERROR" status, ensuring the health check endpoint remains stable even when components fail.
     * 
     * <p>
     * <b>Behavior:</b>
     * <ul>
     * <li>Executes the provided {@link Supplier} health check function</li>
     * <li>On success: Records status as "OK" in the log map</li>
     * <li>On exception: Records status as "ERROR" in the log map</li>
     * <li>Always records execution time as "{@code <entryName>Time}" in milliseconds</li>
     * </ul>
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * Map<String, String> diagnostics = new LinkedHashMap<>();
     * boolean dbHealthy = measureTime("db", diagnostics, userRepository::count);
     * // diagnostics now contains: {"db": "OK", "dbTime": "42"}
     * }</pre>
     * 
     * <p>
     * This method is designed for use within the {@link #ping(HttpServletResponse)} endpoint to
     * uniformly measure and report health check execution across all monitored components.
     * 
     *
     * @param entryName the component name used as the key in the diagnostic map; status is stored
     *                  under this key and timing under "{@code <entryName>Time}"
     * @param log the diagnostic map where status ("OK"/"ERROR") and timing (milliseconds as string)
     *            are recorded; must be mutable and non-null
     * @param f the health check supplier to execute; exceptions are caught and treated as failures;
     *          return value is ignored (only successful execution matters)
     * @return {@code true} if the health check executed successfully without throwing an exception,
     *         {@code false} if an exception was caught (indicating component failure)
     * @since 1.7.1
     * @see #ping(HttpServletResponse)
     */
    boolean measureTime(String entryName, Map<String, String> log, Supplier f) {
        debug("[measureTime]");
        long start = System.currentTimeMillis();
        boolean result = true;
        try {
            f.get();
            log.put(entryName, "OK");
        } catch (Exception e) {
            log.put(entryName, "ERROR");
            result = false;
        }
        long end = System.currentTimeMillis();
        log.put(entryName + "Time", (end - start) + "");
        return result;
    }


    /**
     * Public health check endpoint that performs diagnostic checks on critical system components.
     * <p>
     * This method executes health checks for each monitored component (currently database connectivity)
     * and returns a diagnostic map with status and execution timing. The HTTP response status is set
     * based on the overall health state:
     * <ul>
     * <li>HTTP 200 - All components passed health checks</li>
     * <li>HTTP 503 (SERVICE_UNAVAILABLE) - One or more components failed</li>
     * </ul>
     * 
     * <p>
     * <b>Current Health Checks:</b>
     * <ul>
     * <li><b>db</b> - Database connectivity verified via {@link UserRepository#count()}</li>
     * </ul>
     * 
     * <p>
     * <b>Response Map Structure:</b><br>
     * The returned {@link LinkedHashMap} preserves insertion order and contains:
     * <ul>
     * <li>{@code <component>} - Status string: "OK" (success) or "ERROR" (failure)</li>
     * <li>{@code <component>Time} - Execution time in milliseconds as string</li>
     * </ul>
     * 
     * <p>
     * Example response body:
     * <pre>{@code
     * {"db": "OK", "dbTime": "23"}
     * }</pre>
     * 
     * <p>
     * This endpoint is exposed at {@code GET /ping} via {@link GetMapping} and requires no authentication.
     * It is suitable for use with monitoring systems, load balancers, and orchestration health probes.
     * 
     *
     * @param response the HTTP servlet response used to set status code (200 for healthy, 503 for unhealthy);
     *                 status is modified as a side-effect when health checks fail
     * @return a {@link LinkedHashMap} containing per-component status ("OK"/"ERROR") and execution time
     *         (in milliseconds as string); map preserves insertion order for consistent response structure
     * @since 1.7.1
     * @see #measureTime(String, Map, Supplier)
     */
    @GetMapping("/ping")
    public Map<String, String> ping(HttpServletResponse response) {
        debug("[ping]");
        Map<String, String> log = new LinkedHashMap<>();

        boolean dbWorks = measureTime("db", log, userRepository::count);

        boolean isOk = dbWorks;

        if (!isOk) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        }

        return log;

    }

}
