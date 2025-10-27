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
 * Provides HTTP-aware exception types and Spring MVC exception resolution for the OpenKoda platform.
 * <p>
 * This package centralizes exception handling by combining typed exception classes with declarative
 * HTTP status mapping and a global exception resolver. All exceptions extend
 * {@link com.openkoda.core.flow.HttpStatusException} from the core.flow package, enabling
 * programmatic status inspection. The architecture integrates with Spring MVC to provide
 * consistent error responses and request-correlated logging.
 * </p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li><b>NotFoundException</b>: HTTP 404 exception for missing resources, invalid entity IDs,
 *       or non-existent records. Use when repository lookups fail or requested items don't exist.</li>
 *   <li><b>UnauthorizedException</b>: HTTP 401 exception for authentication failures such as
 *       invalid credentials, expired tokens, or missing authentication headers.</li>
 *   <li><b>ServerErrorException</b>: HTTP 500 exception for internal errors including database
 *       failures, external service timeouts, configuration problems, or unexpected runtime exceptions.</li>
 *   <li><b>FrontendResourceValidationException</b>: HTTP 500 exception with validation context
 *       for FrontendResource issues. Carries both validation result (boolean) and suggestion
 *       (String) for developer feedback.</li>
 *   <li><b>ErrorLoggingExceptionResolver</b>: Spring MVC exception resolver that handles uncaught
 *       controller exceptions. Performs conditional logging, maps exceptions to HTTP status codes,
 *       and redirects to the /error endpoint with status and requestId parameters.</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>
 * The exception hierarchy follows this structure:
 * </p>
 * <pre>
 * HttpStatusException (from core.flow)
 *   ├── NotFoundException (@ResponseStatus HTTP 404)
 *   ├── UnauthorizedException (@ResponseStatus HTTP 401)
 *   ├── ServerErrorException (@ResponseStatus HTTP 500)
 *   └── FrontendResourceValidationException (HTTP 500 with validation state)
 * </pre>
 * <p>
 * Each exception uses Spring's {@code @ResponseStatus} annotation for declarative HTTP status
 * mapping. This allows Spring MVC to automatically convert exceptions into appropriate HTTP
 * responses without additional configuration. The ErrorLoggingExceptionResolver serves as
 * a catch-all for uncaught exceptions, ensuring consistent error handling across the application.
 * </p>
 * <p>
 * The resolver integrates with RequestIdHolder from the core.tracker package to correlate
 * exceptions with specific requests. All error redirects include both HTTP status and request ID,
 * enabling efficient troubleshooting and log correlation.
 * </p>
 *
 * <h2>Usage Patterns</h2>
 * <p>
 * <b>When to throw each exception type:</b>
 * </p>
 * <ul>
 *   <li><b>NotFoundException</b>: Repository methods that fail to find entities by ID, service
 *       layer lookups for non-existent resources, or API endpoints accessing missing data.</li>
 *   <li><b>UnauthorizedException</b>: Authentication filters detecting invalid tokens, privilege
 *       checks failing due to missing authentication, or expired session validation.</li>
 *   <li><b>ServerErrorException</b>: Database connection failures, external API timeouts,
 *       configuration initialization errors, or unexpected runtime conditions.</li>
 *   <li><b>FrontendResourceValidationException</b>: JavaScript syntax errors in dynamic code,
 *       template validation failures, or security checks on frontend resources.</li>
 * </ul>
 * <p>
 * <b>Exception resolution flow:</b>
 * </p>
 * <ol>
 *   <li>Controller or Flow pipeline throws typed exception</li>
 *   <li>Spring MVC invokes ErrorLoggingExceptionResolver</li>
 *   <li>Resolver determines if logging is warranted (excludes client aborts and filtered user agents)</li>
 *   <li>Resolver extracts HTTP status from exception type or @ResponseStatus annotation</li>
 *   <li>Resolver logs exception with message, status, URI, and cause</li>
 *   <li>Resolver returns ModelAndView redirect to /error?status=STATUS&amp;requestId=ID</li>
 *   <li>ErrorController at /error handles final response rendering</li>
 * </ol>
 * <p>
 * <b>Logging strategy:</b>
 * </p>
 * <p>
 * ErrorLoggingExceptionResolver applies conditional logging to reduce noise. It suppresses
 * logging for configured user agent substrings (e.g., health check bots) and Tomcat
 * ClientAbortException cases (client disconnects). All suppressed exceptions emit DEBUG-level
 * diagnostics for troubleshooting. This balances operational visibility with log cleanliness.
 * </p>
 *
 * <h2>Integration Notes</h2>
 * <p>
 * ErrorLoggingExceptionResolver must be registered in the Spring MVC exception resolver chain,
 * typically via {@code MvcConfig#configureHandlerExceptionResolvers}. The resolver extends
 * {@code SimpleMappingExceptionResolver} and implements {@code LoggingComponentWithRequestId}
 * for request-correlated logging.
 * </p>
 * <p>
 * All typed exceptions coordinate with {@link com.openkoda.core.flow.HttpStatusException} to
 * enable status inspection via {@code getStatus()} method. Spring Security's
 * {@code AccessDeniedException} is automatically mapped to HTTP 401 (UNAUTHORIZED) by the
 * resolver, providing seamless integration with privilege enforcement.
 * </p>
 * <p>
 * The resolver constructor accepts a {@code userAgentExcludedFromErrorLog} parameter for
 * configuring user agent filtering. Pass a substring to suppress ERROR-level logging for
 * matching User-Agent headers (e.g., "ELB-HealthChecker" for AWS load balancers). This
 * prevents log pollution from automated health checks and monitoring probes.
 * </p>
 * <p>
 * Client abort detection recognizes Tomcat's ClientAbortException wrapping IOException,
 * automatically suppressing ERROR logs when clients disconnect during response streaming.
 * This pattern reduces noise from mobile clients with intermittent connectivity.
 * </p>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li><b>Use typed exceptions for HTTP semantics</b>: Throw NotFoundException for missing
 *       resources (404), UnauthorizedException for authentication failures (401), and
 *       ServerErrorException for internal errors (500). This provides clear HTTP status mapping.</li>
 *   <li><b>Provide descriptive messages</b>: Include entity IDs, requested resources, or error
 *       context in exception messages for debugging. Example: "Organization not found: id=123".</li>
 *   <li><b>Let ErrorLoggingExceptionResolver handle uncaught exceptions</b>: Don't catch and
 *       rethrow generic exceptions. The resolver provides consistent logging and error responses.</li>
 *   <li><b>Use FrontendResourceValidationException for validation errors</b>: Include actionable
 *       suggestions in the message to guide developers fixing validation issues.</li>
 *   <li><b>Consider request correlation</b>: Exception logs include request IDs for correlation
 *       with access logs and distributed traces. Use this for troubleshooting production issues.</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <p>
 * <b>Repository layer throwing NotFoundException:</b>
 * </p>
 * <pre>
 * public Organization findById(Long id) {
 *     return repository.findById(id)
 *         .orElseThrow(() -&gt; new NotFoundException("Organization not found: " + id));
 * }
 * </pre>
 * <p>
 * <b>Service layer handling internal errors:</b>
 * </p>
 * <pre>
 * try {
 *     externalApiClient.fetchData();
 * } catch (IOException e) {
 *     throw new ServerErrorException("External API unavailable");
 * }
 * </pre>
 * <p>
 * <b>ErrorLoggingExceptionResolver processing exceptions:</b>
 * </p>
 * <pre>
 * // Configured in MvcConfig
 * &#64;Override
 * public void configureHandlerExceptionResolvers(List&lt;HandlerExceptionResolver&gt; resolvers) {
 *     resolvers.add(new ErrorLoggingExceptionResolver("ELB-HealthChecker"));
 * }
 * 
 * // Exception thrown in controller is caught by resolver
 * // Resolver logs: "Exception [Organization not found: 123], status [NOT_FOUND], URI [/api/org/123]"
 * // Resolver redirects to: /error?status=NOT_FOUND&amp;requestId=abc-123-def
 * </pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.core.flow.HttpStatusException
 * @see ErrorLoggingExceptionResolver
 * @see org.springframework.web.servlet.handler.SimpleMappingExceptionResolver
 */
package com.openkoda.core.exception;