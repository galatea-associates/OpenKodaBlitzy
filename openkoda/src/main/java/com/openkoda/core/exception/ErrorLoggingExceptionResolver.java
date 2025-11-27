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

package com.openkoda.core.exception;

import com.openkoda.core.flow.HttpStatusException;
import com.openkoda.core.helper.ReadableCode;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.core.tracker.RequestIdHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import java.io.IOException;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;


/**
 * Spring MVC exception resolver that centralizes uncaught controller exception handling.
 * <p>
 * This resolver extends {@link SimpleMappingExceptionResolver} to integrate with Spring's exception
 * resolution chain and implements {@link LoggingComponentWithRequestId} for request-correlated logging
 * and {@link ReadableCode} for readable conditional logic.
 * 
 * <p>
 * Exception resolution workflow:
 * 
 * <ol>
 *   <li>Catches uncaught exceptions from controllers</li>
 *   <li>Maps to appropriate HTTP status (HttpStatusException → custom status, AccessDeniedException → 401, others → 500)</li>
 *   <li>Performs conditional logging (suppresses noise from excluded user agents and client aborts)</li>
 *   <li>Redirects to /error endpoint with status and requestId query parameters</li>
 * </ol>
 * <p>
 * Integration with {@link RequestIdHolder} enables request correlation for distributed tracing.
 * Noise suppression mechanisms detect client abort conditions and filter user agents to reduce
 * log pollution from known problematic clients.
 * 
 * <p>
 * Example configuration in MvcConfig:
 * 
 * <pre>
 * ErrorLoggingExceptionResolver resolver = new ErrorLoggingExceptionResolver(excludedAgent);
 * resolvers.add(resolver);
 * </pre>
 * <p>
 * Thread safety: This class is effectively thread-safe. The static field is only mutated during construction,
 * and all instance methods operate on immutable state or thread-safe Spring components.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.core.configuration.MvcConfig#configureHandlerExceptionResolvers
 * @see HttpStatusException
 * @see SimpleMappingExceptionResolver
 * @see LoggingComponentWithRequestId
 * @see RequestIdHolder
 */
public class ErrorLoggingExceptionResolver extends
		SimpleMappingExceptionResolver implements LoggingComponentWithRequestId, ReadableCode {

	/**
	 * Static configuration for user agent substring filtering.
	 * <p>
	 * Exceptions from requests with matching User-Agent headers are not logged at ERROR level
	 * to reduce log noise from known problematic clients. A null value means no filtering is applied.
	 * 
	 */
	private static String userAgentExcludedFromErrorLog = null;

	/**
	 * Constructs an exception resolver with user agent filtering configuration.
	 * <p>
	 * Initialization steps:
	 * 
	 * <ol>
	 *   <li>Sets warn log category to this class name for controlled logging levels</li>
	 *   <li>Sets default error view to "frontend-resource/error" for error page rendering</li>
	 *   <li>Normalizes userAgentExcludedFromErrorLog parameter (blank values become null)</li>
	 * </ol>
	 *
	 * @param userAgentExcludedFromErrorLog User agent substring to exclude from ERROR logging,
	 *                                      or null/blank to disable filtering
	 */
	public ErrorLoggingExceptionResolver(String userAgentExcludedFromErrorLog) {
		setWarnLogCategory(getClass().getName());
		setDefaultErrorView("frontend-resource/error");
		this.userAgentExcludedFromErrorLog = StringUtils.defaultIfBlank(userAgentExcludedFromErrorLog, null);
	}


	/**
	 * Resolves exceptions thrown during controller request processing.
	 * <p>
	 * This method uses the older Spring MVC API and receives the handler that generated the exception
	 * (typically an <tt>@Controller</tt> annotated class).
	 * 
	 * <p>
	 * Exception resolution logic:
	 * 
	 * <ol>
	 *   <li>Determines if exception should be logged via {@link #shouldErrorLogException}</li>
	 *   <li>Extracts HTTP status from exception type:
	 *     <ul>
	 *       <li>{@link HttpStatusException} → uses custom status from exception</li>
	 *       <li>{@link AccessDeniedException} → returns 401 UNAUTHORIZED</li>
	 *       <li>All other exceptions → returns 500 INTERNAL_SERVER_ERROR</li>
	 *     </ul>
	 *   </li>
	 *   <li>Logs exception with message, status, URI, and cause if logging is warranted</li>
	 *   <li>Returns ModelAndView redirect to /error endpoint with status and requestId query parameters</li>
	 * </ol>
	 *
	 * @param request   Current HTTP request being processed
	 * @param response  HTTP response (status may be pre-set by servlet container)
	 * @param handler   Controller handler that generated the exception (typically @Controller)
	 * @param exception Uncaught exception to be resolved
	 * @return ModelAndView redirect to /error endpoint with status and requestId query parameters
	 */
	@Override
	protected ModelAndView doResolveException(HttpServletRequest request,
											  HttpServletResponse response, Object handler, Exception exception) {
		if (shouldErrorLogException(exception, request)) {
			error(exception, "Error message: {}, Status: {}, URI: {}, \ncause: {}",
					exception == null ? "" : exception.getLocalizedMessage(), response.getStatus(), request.getRequestURI(), defaultIfNull(exception.getCause(),"not attached"));
		}
		HttpStatus errorStatus = (exception instanceof HttpStatusException) ?
				((HttpStatusException) exception).status :
					((exception instanceof AccessDeniedException) ? UNAUTHORIZED : INTERNAL_SERVER_ERROR);
		String redirect = String.format("redirect:/error?status=%s&requestId=%s", errorStatus.name(), RequestIdHolder.getId());
		return new ModelAndView(redirect);
	}

	/**
	 * Determines if a given exception should be logged at ERROR level.
	 * <p>
	 * Logging decision logic:
	 * 
	 * <ul>
	 *   <li>Returns false for null exceptions</li>
	 *   <li>Returns false for excluded user agents (configured via constructor parameter)</li>
	 *   <li>Returns false for Tomcat {@link ClientAbortException} with {@link IOException} cause
	 *       (indicates client disconnects, not server errors)</li>
	 *   <li>Emits DEBUG diagnostic context for troubleshooting filter decisions</li>
	 *   <li>Returns true otherwise (exception should be logged at ERROR level)</li>
	 * </ul>
	 * <p>
	 * This method reduces log noise from known benign conditions while preserving visibility
	 * into genuine server-side errors.
	 * 
	 *
	 * @param exception Exception to evaluate for logging eligibility
	 * @param request   HTTP request containing User-Agent header for filtering
	 * @return True if exception should be logged at ERROR level, false to suppress logging
	 */
	private boolean shouldErrorLogException(Exception exception, HttpServletRequest request) {
		boolean exceptionIsNotNull = exception != null;
		String userAgent = request.getHeader("User-Agent");
		boolean isExcludedUserAgent = isExcludedUserAgent(userAgent);
		boolean hasCause = exceptionIsNotNull && exception.getCause() != null;
		boolean isClientAbortException = exceptionIsNotNull && hasCause && (exception instanceof ClientAbortException)
				&& (exception.getCause() instanceof IOException);
		debug("[exceptionToLog] is not null {} class {} cause {} agent [{}]", exceptionIsNotNull,
                exceptionIsNotNull ? exception.getClass().getSimpleName() : "",
                hasCause ? exception.getCause().getClass().getSimpleName() : "", userAgent);
		return not(isExcludedUserAgent || isClientAbortException);
	}

	/**
	 * Determines if a given User-Agent should be excluded from ERROR logging when exceptions occur.
	 * <p>
	 * This static method performs substring matching against the configured exclusion filter
	 * to reduce log pollution from known troublesome agents (such as aggressive crawlers,
	 * monitoring tools, or misbehaving clients).
	 * 
	 * <p>
	 * The filter is case-sensitive and matches any occurrence of the configured substring
	 * within the User-Agent header. If no filter is configured (null value), all user agents
	 * are allowed and this method returns false.
	 * 
	 *
	 * @param userAgent User-Agent header value from HTTP request, may be null
	 * @return True if user agent contains the configured exclusion substring,
	 *         false otherwise or if no filter is configured
	 */
	public static boolean isExcludedUserAgent(String userAgent) {
        return StringUtils.contains(userAgent, userAgentExcludedFromErrorLog);
    }

}