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

package com.openkoda.controller;

import com.openkoda.controller.common.URLConstants;
import com.openkoda.core.exception.ErrorLoggingExceptionResolver;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.helper.ReadableCode;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.core.tracker.RequestIdHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.*;

import static com.openkoda.controller.common.PageAttributes.*;

/**
 * Global error handling controller mapping exceptions to user-friendly error pages or API responses.
 * <p>
 * Spring Boot {@code /error} handler implementing {@link org.springframework.boot.web.servlet.error.ErrorController}.
 * Catches exceptions from other controllers, maps to HTTP status codes, and renders appropriate error views
 * (404, 500, 403) or JSON for API requests. Builds {@link PageModelMap} with requestId and status.
 * Deduplicates noisy NOT_FOUND errors using in-memory collections (lastErrors, unavailablePages).
 * Returns API JSON error responses for {@code Accept: application/json} requests.
 * 
 * <p>
 * Request mapping: {@code /error} (Spring Boot standard error endpoint)
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.springframework.boot.web.servlet.error.ErrorController
 * @see PageModelMap
 */
@Controller
public class ErrorControllerImpl implements ErrorController, LoggingComponentWithRequestId, URLConstants, ReadableCode {

    /**
     * Maximum size for the error log buffer before warning is logged.
     * <p>
     * When {@link #lastErrors} reaches this threshold, accumulated errors are logged
     * at WARN level and the collection is cleared to prevent memory growth.
     * 
     */
    private static final int lastErrorsSize = 30;

    /**
     * In-memory collection tracking recent error timestamps for deduplication.
     * <p>
     * Concurrent collection for multi-threaded error logging. Prevents log spam
     * from repeated identical errors (e.g., missing static assets). Cleared when
     * size reaches {@link #lastErrorsSize}.
     * 
     * <p>
     * Format: "{status} {requestUri} {timestamp}"
     * 
     */
    private static final List lastErrors = new ArrayList(lastErrorsSize + 5);

    /**
     * Set of URL paths that have generated 404 errors.
     * <p>
     * Avoids repeated logging of known missing resources. Once a URL path generates
     * a NOT_FOUND error, subsequent requests to the same path are not logged again,
     * reducing noise in error logs.
     * 
     */
    private static final Set<String> unavailablePages = new HashSet<>();

    /**
     * HTTP status code representing page not found errors.
     * <p>
     * Used for comparing response status to determine if special NOT_FOUND
     * deduplication logic should be applied.
     * 
     */
    private static final HttpStatus pageNotFoundStatus = HttpStatus.NOT_FOUND;

    /**
     * User agent patterns excluded from error logging (configured via properties).
     * <p>
     * Property key: {@code user.agent.excluded.from.error.log}. Allows filtering
     * out known bots or monitoring tools from error logs to reduce noise.
     * 
     */
    @Value("${user.agent.excluded.from.error.log:}")
    String userAgentExcludedFromErrorLog;

    /**
     * Handles all uncaught exceptions and HTTP errors for the application.
     * <p>
     * HTTP mapping: {@code GET/POST /error}. Extracts status code from
     * {@code jakarta.servlet.error.status_code} attribute, retrieves exception from
     * {@code jakarta.servlet.error.exception}, logs error with request correlation ID,
     * builds error response with status/message/requestId, returns HTML view for browsers
     * or JSON for API clients.
     * 
     * <p>
     * Status code mapping:
     * <ul>
     *   <li>404 → not-found view</li>
     *   <li>403 → access-denied view</li>
     *   <li>500 → server-error view</li>
     *   <li>others → generic-error view</li>
     * </ul>
     * 
     * <p>
     * Implements deduplication for NOT_FOUND errors to prevent log spam from
     * repeated requests to missing resources. Tracks unavailable pages and logs
     * only first occurrence.
     * 
     *
     * @param reqId optional request correlation ID for tracking errors across logs
     * @param responseStatus HTTP status code for the error (default: NOT_FOUND)
     * @param request HttpServletRequest containing error attributes (status_code, exception, message, request_uri)
     * @return {@link ModelAndView} with error template for HTML requests, or {@link ResponseEntity} with JSON for API requests (URLs starting with /api)
     */
    @RequestMapping("/error")
    @ResponseBody
    public Object handleError(
            @RequestParam(name = "requestId", required = false) String reqId,
            @RequestParam(name = "status", required = false, defaultValue = "NOT_FOUND") HttpStatus responseStatus,
            HttpServletRequest request) {
        debug("[handleError] ReqId: {}", reqId);
        PageModelMap model = new PageModelMap();
        Optional<String> requestUri = getErrorRequestUri(request);
        Optional<String> requestErrorMessage = getErrorMessage(request);
        model.put(errorMessage, requestErrorMessage.orElse(null));
        model.put(errorHttpStatus, responseStatus);

        if (reqId == null) {
            reqId = RequestIdHolder.getId();
            String userAgent = request.getHeader("User-Agent");
            boolean isExcludedUserAgent = ErrorLoggingExceptionResolver.isExcludedUserAgent(userAgent);
            if (not(isExcludedUserAgent)) {
                if(pageNotFoundStatus.equals(responseStatus) && requestUri.isPresent()) {
                    if(unavailablePages.contains(requestUri.get())) {
                        model.put(requestId, reqId);
                        return new ModelAndView("frontend-resource/error", model, responseStatus);
                    }
                    unavailablePages.add(requestUri.get());
                }
                lastErrors.add(String.format("%s %s %s", responseStatus, requestUri, LocalDateTime.now()));
                if (lastErrors.size() >= lastErrorsSize) {
                    warn("Last ca. {} errors outside the controller flow: {}", lastErrorsSize, lastErrors.toString());
                    lastErrors.clear();
                }
            }
        }
        if (getErrorRequestUri(request).map( a -> a.startsWith(_API) ).orElse(false) ) {
            return ResponseEntity.status(responseStatus).body(model);
        }
        model.put(requestId, reqId);
        return new ModelAndView("frontend-resource/error", model, responseStatus);
    }

    /**
     * Extracts the original request URI from error attributes.
     * <p>
     * Retrieves the {@code jakarta.servlet.error.request_uri} attribute which contains
     * the URI of the request that caused the error. Used for error logging and
     * deduplication tracking.
     * 
     *
     * @param request HttpServletRequest containing servlet error attributes
     * @return Optional containing request URI string, or empty if attribute not present
     */
    private Optional<String> getErrorRequestUri(HttpServletRequest request) {
        return Optional.ofNullable((String) request.getAttribute("jakarta.servlet.error.request_uri"));
    }

    /**
     * Extracts the error message from error attributes.
     * <p>
     * Retrieves the {@code jakarta.servlet.error.message} attribute which contains
     * the exception message or HTTP status reason phrase. Displayed to users in
     * error views.
     * 
     *
     * @param request HttpServletRequest containing servlet error attributes
     * @return Optional containing error message string, or empty if attribute not present
     */
    private Optional<String> getErrorMessage(HttpServletRequest request) {
        return Optional.ofNullable((String) request.getAttribute("jakarta.servlet.error.message"));
    }

    /**
     * Extracts the HTTP status code from error attributes.
     * <p>
     * Retrieves the {@code jakarta.servlet.error.status_code} attribute which contains
     * the numeric HTTP status code (e.g., 404, 500). Used for determining which error
     * view to render and for error classification.
     * 
     *
     * @param request HttpServletRequest containing servlet error attributes
     * @return Integer status code, or null if attribute not present
     */
    private Integer getErrorStatusCode(HttpServletRequest request) {
        return (Integer) request.getAttribute("jakarta.servlet.error.status_code");
    }
}
