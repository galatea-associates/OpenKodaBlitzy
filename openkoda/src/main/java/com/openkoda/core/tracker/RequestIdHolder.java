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

package com.openkoda.core.tracker;

import com.openkoda.core.helper.ApplicationContextProvider;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.openkoda.controller.common.URLConstants.EXTERNAL_SESSION_ID;

/**
 * Central utility for generating and resolving context-appropriate correlation IDs for distributed tracing.
 * <p>
 * Provides ID generation and context-aware resolution for both HTTP requests and scheduled jobs. Automatically 
 * determines execution context (web request vs cron job) and retrieves the appropriate correlation ID. For web 
 * requests, delegates to request-scoped {@link WebRequestIdHolder} bean and appends external session ID from 
 * request attributes. For cron jobs, retrieves job ID from SLF4J MDC (placed by {@link TrackJobAspect}). 
 * Thread-safe through use of ThreadLocal-based MDC and request-scoped beans.

 * <p>
 * <b>ID Resolution Algorithm:</b>
 * <ol>
 * <li>Check if {@link org.springframework.web.context.request.RequestContextHolder} has request attributes</li>
 * <li>If yes: retrieve {@link WebRequestIdHolder} bean from ApplicationContext + append EXTERNAL_SESSION_ID attribute</li>
 * <li>If no: fall back to {@link #cronJobId()} which reads from MDC</li>
 * <li>Return empty string if no context available</li>
 * </ol>

 * <p>
 * <b>ID Format:</b> Generated IDs follow format: {@code yyyyMMddHHmmss-[8-char-alphanumeric-random]} 
 * (e.g., {@code 20240115143022-aB3dEf7H})

 * <p>
 * Example - automatic context-aware resolution:
 * <pre>{@code
 * String id = RequestIdHolder.getId();
 * // Returns web request ID if in HTTP context, job ID if in scheduled job, empty string otherwise
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see WebRequestIdHolder
 * @see TrackJobAspect
 * @see LoggingComponentWithRequestId
 * @see org.slf4j.MDC
 */
@Component
public class RequestIdHolder {

    /**
     * MDC key for storing cron job correlation IDs.
     * <p>
     * Value: {@code "jobId"}. Used by {@link TrackJobAspect} to inject job IDs into MDC before scheduled 
     * method execution. Public mutable field - can be changed but not recommended as it breaks integration 
     * with {@link TrackJobAspect}.

     *
     * @see TrackJobAspect#setJobIdForThread()
     */
    public static String PARAM_CRON_JOB_ID = "jobId";
    
    /**
     * Thread-safe date-time formatter for ID generation timestamp prefix.
     * <p>
     * Pattern: {@code yyyyMMddHHmmss} (e.g., {@code 20240115143022}). Immutable and thread-safe per 
     * {@link DateTimeFormatter} contract.

     */
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Generates a new unique correlation ID with timestamp and random suffix.
     * <p>
     * Creates correlation ID by combining current timestamp ({@code yyyyMMddHHmmss} format via 
     * {@link LocalDateTime#now()}) with 8-character alphanumeric random string from 
     * {@link RandomStringUtils}. Thread-safe as {@link LocalDateTime} is immutable and 
     * {@link RandomStringUtils} uses secure random generation. IDs are unique with high probability 
     * due to timestamp precision and random suffix.

     * <p>
     * Used by {@link WebRequestIdHolder} during bean initialization and {@link TrackJobAspect} 
     * before scheduled job execution.

     *
     * @return unique correlation ID in format {@code yyyyMMddHHmmss-[8-char-random]} 
     *         (e.g., {@code 20240115143022-aB3dEf7H})
     * @see RandomStringUtils#randomAlphanumeric(int)
     */
    public static String generate() {
        return formatter.format(LocalDateTime.now()) + "-" + RandomStringUtils.randomAlphanumeric(8);
    }

    /**
     * Resolves context-appropriate correlation ID for current execution (web request or cron job).
     * <p>
     * Context-aware ID resolution:
     * <ol>
     * <li>Checks {@link RequestContextHolder} for request attributes to detect web request context</li>
     * <li>If web context exists: retrieves {@link WebRequestIdHolder} bean from ApplicationContext and 
     *     appends EXTERNAL_SESSION_ID attribute if present</li>
     * <li>If no web context: delegates to {@link #cronJobId()} to retrieve job ID from MDC</li>
     * <li>Returns empty string if neither context available</li>
     * </ol>
     * Thread-safe through ThreadLocal-based mechanisms.

     * <p>
     * Primary method for retrieving correlation ID in application code. Automatically adapts to 
     * execution context.

     *
     * @return web request ID (with optional external session ID appended) if in HTTP context, 
     *         cron job ID if in scheduled job context, empty string if no context available
     * @throws org.springframework.beans.BeansException if ApplicationContext lookup fails 
     *         (rare, indicates Spring context not initialized)
     * @see WebRequestIdHolder#getWebRequestId()
     * @see #cronJobId()
     * @see org.springframework.web.context.request.RequestContextHolder
     */
    public static String getId() {
        return RequestContextHolder.getRequestAttributes() != null ?
                    ApplicationContextProvider.getContext().getBean(WebRequestIdHolder.class).getWebRequestId()
                    + StringUtils.defaultString((String)RequestContextHolder.getRequestAttributes().getAttribute(EXTERNAL_SESSION_ID, 0))
                : cronJobId();
    }

    /**
     * Retrieves cron job correlation ID from SLF4J MDC.
     * <p>
     * Reads job ID from SLF4J MDC using key {@link #PARAM_CRON_JOB_ID} ({@code "jobId"}). Returns 
     * empty string if MDC entry is null or empty. Job IDs are placed into MDC by {@link TrackJobAspect} 
     * before scheduled method execution. Thread-safe as MDC is ThreadLocal-based.

     * <p>
     * Called by {@link #getId()} when no web request context exists. Public for direct access in 
     * specialized scenarios.

     *
     * @return job correlation ID if present in MDC under key {@link #PARAM_CRON_JOB_ID}, 
     *         empty string otherwise
     * @see TrackJobAspect
     * @see org.slf4j.MDC#get(String)
     */
    public static String cronJobId() {
        return StringUtils.isNotEmpty(MDC.get(RequestIdHolder.PARAM_CRON_JOB_ID)) ? MDC.get(PARAM_CRON_JOB_ID) : "";
    }
}
