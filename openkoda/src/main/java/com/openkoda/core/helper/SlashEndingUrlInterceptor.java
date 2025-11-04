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

package com.openkoda.core.helper;

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring HandlerInterceptor that redirects non-root requests ending with trailing slash to slashless path.
 * <p>
 * This interceptor normalizes URLs for SEO and canonical URL consistency by removing trailing slashes
 * from all paths except the root path "/". When a request URI ends with a trailing slash (and is not
 * the root), the interceptor performs an HTTP redirect to the same path without the trailing slash.
 * <p>
 * Important: This interceptor does not preserve query strings during redirect. For example, a request
 * to "/users/?page=1" will redirect to "/users" without the page parameter. This limitation should be
 * considered when using this interceptor with parameterized requests.
 * <p>
 * Example usage:
 * <pre>
 * Request to "/users/" redirects to "/users"
 * Request to "/" is preserved (root path not redirected)
 * </pre>
 * <p>
 * Thread-safety: This interceptor is stateless and safe for concurrent use across multiple requests.
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.springframework.web.servlet.HandlerInterceptor
 */
@Component
public class SlashEndingUrlInterceptor implements LoggingComponentWithRequestId, HandlerInterceptor {

    /**
     * Intercepts incoming requests and redirects non-root paths ending with trailing slash.
     * <p>
     * This method examines the request URI and performs the following logic:
     * <ul>
     * <li>Root path "/" is preserved without redirect</li>
     * <li>Non-root paths ending with "/" are redirected to the same path without trailing slash</li>
     * <li>All other paths continue processing normally</li>
     * </ul>
     * 
     * <p>
     * Redirect logic example:
     * <pre>
     * "/users/" -> redirects to "/users" (returns false)
     * "/" -> no redirect (returns true)
     * "/users" -> no redirect (returns true)
     * </pre>
     * 
     * <p>
     * Note: Query strings are not preserved during redirect. A request to "/users/?page=1"
     * will redirect to "/users" without the page parameter.
     * 
     *
     * @param request current HTTP servlet request
     * @param response current HTTP servlet response used for sending redirect
     * @param handler chosen handler for request execution
     * @return {@code false} if redirect was performed (stops request processing),
     *         {@code true} to continue with normal request processing
     * @throws Exception if the redirect operation fails
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        debug("[preHandle]");
        String uri = request.getRequestURI();

        if (!uri.equals("/") && uri.endsWith("/")) {
            response.sendRedirect(uri.substring(0, uri.length() - 1));
            return false;
        }
        return true;
    }
}