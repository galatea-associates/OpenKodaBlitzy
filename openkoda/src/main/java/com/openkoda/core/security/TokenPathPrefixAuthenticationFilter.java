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

package com.openkoda.core.security;

import com.openkoda.controller.common.URLConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Spring Security authentication filter for token authentication via URL path prefix pattern {@code /__t_/{token}/...}.
 * <p>
 * This filter extends {@link AbstractTokenAuthenticationFilter} and implements the {@code extractTokenFromRequest()}
 * method to parse the authentication token from the request URI path prefix. It enables embedding authentication tokens
 * directly in URL paths for resources accessed without session cookies, such as public links to protected downloads
 * or embedded images in emails requiring authentication.
 * <p>
 * The request matcher {@link #checkRequestSupport(HttpServletRequest)} verifies that the HTTP method is GET and
 * the URI starts with the {@code "/__t_/"} prefix defined in {@link URLConstants#__T_}. This GET-only restriction
 * prevents CSRF attacks via token-in-URL for state-changing operations, consistent with the
 * {@link RequestParameterTokenAuthenticationFilter} security model.
 * <p>
 * Token extraction parses the substring between the {@code "/__t_/"} prefix and the next {@code "/"} delimiter
 * from the request URI. For example, the URI {@code /__t_/abc123/download/report.pdf} extracts token {@code "abc123"}.
 * <p>
 * The authentication success handler forwards the request to the target URI with the token prefix removed.
 * For example, {@code /__t_/abc123/download/report.pdf} forwards to {@code /download/report.pdf} after successful
 * authentication. This preserves the request method, headers, parameters, and body while continuing the Spring Security
 * filter chain with the authenticated {@link org.springframework.security.core.context.SecurityContext}.
 * <p>
 * Usage examples:
 * <pre>
 * // Example 1: Download protected file
 * GET /__t_/xyz789/api/files/report.pdf
 * → extracts token "xyz789"
 * → forwards to /api/files/report.pdf after authentication
 * 
 * // Example 2: Embedded image in email
 * GET /__t_/abc123/images/logo.png
 * → extracts token "abc123"
 * → forwards to /images/logo.png
 * </pre>
 *
 * @see AbstractTokenAuthenticationFilter
 * @see URLConstants#__T_
 * @see RequestTokenAuthenticationToken
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Service("tokenPathPrefixAuthenticationFilter")
public class TokenPathPrefixAuthenticationFilter extends AbstractTokenAuthenticationFilter implements URLConstants {

    /**
     * Constructs the filter with a GET-only path-prefix {@link org.springframework.security.web.util.matcher.RequestMatcher}
     * and an {@link org.springframework.security.web.authentication.AuthenticationSuccessHandler} that forwards to the
     * target URI with the token prefix stripped.
     * <p>
     * The constructor passes the static method {@link #checkRequestSupport(HttpServletRequest)} as a RequestMatcher
     * to the superclass via {@code super(TokenPathPrefixAuthenticationFilter::checkRequestSupport)}.
     * 
     * <p>
     * The authentication success handler extracts the target URI by finding the second "/" delimiter (after the
     * {@code "/__t_/token"} prefix) and forwards the authenticated request to that URI:
     * <pre>
     * // Example URI processing:
     * req.requestURI = "/__t_/abc123/download/report.pdf"
     * req.requestURI.indexOf("/", 1) = 11 (index of second "/", skips first "/" at index 0)
     * req.requestURI.substring(11) = "/download/report.pdf"
     * </pre>
     * The forward preserves the request method, headers, parameters, and body while continuing the Spring Security
     * filter chain with the authenticated SecurityContext.
     * 
     */
    public TokenPathPrefixAuthenticationFilter() {
        super( TokenPathPrefixAuthenticationFilter::checkRequestSupport );

        //on successful authentication, forward the request to the original target
        setAuthenticationSuccessHandler(
                (req, resp, auth) -> { req.getRequestDispatcher(req.getRequestURI().substring(req.getRequestURI().indexOf("/", 1))).forward(req, resp); });
    }

    /**
     * {@link org.springframework.security.web.util.matcher.RequestMatcher} predicate that determines if this filter
     * processes the given HTTP request.
     * <p>
     * Returns {@code true} if the request method equals "GET" AND the request URI starts with the {@code "/__t_/"}
     * prefix defined in {@link URLConstants#__T_}.
     * 
     * <p>
     * The dual condition enforces:
     * <ol>
     *   <li>GET method only - Prevents CSRF attacks via token-in-URL for state-changing POST/PUT/DELETE operations.
     *       This is consistent with the {@link RequestParameterTokenAuthenticationFilter} security model.</li>
     *   <li>URI starts with {@code "/__t_/"} - Verified using {@link StringUtils#startsWith(CharSequence, CharSequence)}
     *       to check the {@link URLConstants#__T_} constant.</li>
     * </ol>
     * 
     *
     * @param request the HttpServletRequest to check for GET method and {@code "/__t_/"} URI prefix
     * @return {@code true} if request is GET and URI starts with {@code "/__t_/"}, {@code false} otherwise
     */
    static boolean checkRequestSupport(HttpServletRequest request) {
        return request.getMethod().equals("GET") && StringUtils.startsWith(request.getRequestURI(), __T_);
    }

    /**
     * Implements {@link AbstractTokenAuthenticationFilter#extractTokenFromRequest(HttpServletRequest)} to extract
     * the token string from the URI path segment between the {@code "/__t_/"} prefix and the next "/" delimiter.
     * <p>
     * Extraction algorithm:
     * <pre>
     * request.requestURI = "/__t_/abc123/download/report.pdf"
     * __T_.length() = 6 (length of "/__t_/" prefix)
     * request.requestURI.indexOf("/", 1) = 13 (index of "/" after token, search starts at index 1 to skip leading "/")
     * request.requestURI.substring(6, 13) = "abc123" (extracted token)
     * </pre>
     * 
     * <p>
     * Edge case: If no "/" appears after the token (e.g., {@code "/__t_/abc123"}), the {@code substring} method
     * throws {@link IndexOutOfBoundsException}. The filter should reject such malformed URIs.
     * 
     *
     * @param request the HttpServletRequest with URI format {@code /__t_/{token}/targetPath}
     * @return the token value parsed from the URI path, substring between {@code __T_.length()} and {@code indexOf("/", 1)}
     */
    @Override
    protected String extractTokenFromRequest(HttpServletRequest request) {
        return request.getRequestURI().substring(__T_.length(), request.getRequestURI().indexOf("/", 1));
    }

    /**
     * Sets authentication details on the authentication token using the {@code authenticationDetailsSource}.
     * <p>
     * Delegates to {@code authenticationDetailsSource.buildDetails(request)} to create
     * {@link org.springframework.security.web.authentication.WebAuthenticationDetails} containing the remote IP address
     * and session ID. These details are used for audit logging and authentication event tracking.
     * 
     *
     * @param request the HttpServletRequest for extracting authentication details (IP address, session ID)
     * @param authRequest the UsernamePasswordAuthenticationToken to populate with WebAuthenticationDetails
     */
    protected void setDetails(HttpServletRequest request, UsernamePasswordAuthenticationToken authRequest) {
        authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
    }

    /**
     * Setter injection for the Spring Security {@link AuthenticationManager} with {@code @Lazy} annotation to avoid
     * circular dependency during Spring Security configuration initialization.
     * <p>
     * Delegates to {@code super.setAuthenticationManager(authenticationManager)} to connect this filter to the
     * authentication provider chain. The AuthenticationManager contains {@link LoginByPasswordOrTokenAuthenticationProvider}
     * for processing {@link RequestTokenAuthenticationToken} instances created by this filter.
     * 
     * <p>
     * The {@code @Lazy} annotation breaks the circular dependency that occurs when the SecurityConfiguration creates
     * both this filter bean and the AuthenticationManager bean that references the filter.
     * 
     *
     * @param authenticationManager the AuthenticationManager containing LoginByPasswordOrTokenAuthenticationProvider
     */
    @Autowired
    @Override
    public void setAuthenticationManager(@Lazy AuthenticationManager authenticationManager) {
        super.setAuthenticationManager(authenticationManager);
    }

}