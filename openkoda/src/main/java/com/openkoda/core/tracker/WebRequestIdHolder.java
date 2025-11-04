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

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Request-scoped bean that holds unique correlation ID for HTTP request tracing.
 * <p>
 * Stores a unique web request ID generated at bean instantiation via {@link RequestIdHolder#generate()}.
 * The ID persists for the entire HTTP request lifecycle, enabling distributed tracing across controllers,
 * services, and repositories. Spring creates a new instance per HTTP request (via {@code @RequestScope})
 * and destroys it after response completion. Thread-safe as each request thread gets its own instance.

 * <p>
 * <b>Bean Lifecycle:</b> Instantiated when first accessed during HTTP request processing, field
 * {@code webRequestId} initialized via {@link RequestIdHolder#generate()} in field initializer,
 * bean destroyed after HTTP response sent.

 * <p>
 * <b>Integration:</b> Retrieved by {@link RequestIdHolder#getId()} when web request context exists.
 * Used by {@code LoggingComponentWithRequestId} for request correlation in log messages.

 * <p>
 * Example - Spring automatically injects per-request instance:
 * <pre>
 * {@code
 * @Autowired
 * WebRequestIdHolder holder;
 * // Each HTTP request gets unique ID
 * String id = holder.getWebRequestId();
 * }
 * </pre>

 * <p>
 * <b>Note:</b> Not suitable for scheduled jobs or async tasks - use {@link RequestIdHolder#generate()}
 * directly for non-web contexts.

 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @version 1.7.1
 * @since 2019-07-26
 * @see RequestIdHolder
 * @see org.springframework.web.context.annotation.RequestScope
 */
@Component
@RequestScope
public class WebRequestIdHolder {

    /**
     * Unique correlation ID for this HTTP request, generated at bean instantiation.
     * <p>
     * Format: yyyyMMddHHmmss-[8-char-random] (e.g., 20240115143022-aB3dEf7H).
     * Immutable after initialization during bean construction.

     */
    private String webRequestId = RequestIdHolder.generate();

    /**
     * Returns the unique correlation ID for this HTTP request.
     * <p>
     * Retrieves the immutable correlation ID assigned to this HTTP request. The ID was generated
     * once during bean initialization and does not change during the request lifecycle. Used by
     * logging components, audit services, and trace correlation systems.

     * <p>
     * <b>Thread-safety:</b> This bean is request-scoped (one instance per request thread),
     * making this method inherently thread-safe.

     *
     * @return web request correlation ID in format yyyyMMddHHmmss-[8-char-random],
     *         generated at bean instantiation
     * @see RequestIdHolder#generate()
     */
    public String getWebRequestId() {
        return webRequestId;
    }
}
