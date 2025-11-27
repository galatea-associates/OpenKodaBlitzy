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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Spring Security authentication filter for API token authentication via X-API-Token HTTP header.
 * <p>
 * This filter extends {@link AbstractTokenAuthenticationFilter} and implements the
 * {@link #extractTokenFromRequest(HttpServletRequest)} method to read the token from the
 * {@code X-API-Token} header ({@link URLConstants#API_TOKEN} constant). REST API clients
 * include the X-API-Token header for stateless authentication without session cookies.
 * <p>
 * The request matcher uses {@link #checkRequestSupport(HttpServletRequest)} to verify that the
 * API_TOKEN header is present AND the user is not already authenticated. Upon successful
 * authentication, the configured {@code AuthenticationSuccessHandler} forwards the request to
 * the original URI, continuing the filter chain to the target controller endpoint.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Client sends: GET /api/users with header X-API-Token: abc123xyz
 * // Filter extracts "abc123xyz", validates Token entity
 * // Creates RequestTokenAuthenticationToken, authenticates via LoginByPasswordOrTokenAuthenticationProvider
 * }</pre>
 *
 * @see AbstractTokenAuthenticationFilter
 * @see URLConstants#API_TOKEN
 * @see com.openkoda.core.security.RequestTokenAuthenticationToken
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Service("apiTokenHeaderAuthenticationFilter")
public class ApiTokenHeaderAuthenticationFilter extends AbstractTokenAuthenticationFilter implements URLConstants {

    /**
     * Constructs the filter and configures request matching and authentication success handling.
     * <p>
     * The constructor passes {@link #checkRequestSupport(HttpServletRequest)} as a method reference
     * to the superclass, establishing the {@code RequestMatcher} that determines which requests
     * trigger this filter. It also sets an {@code AuthenticationSuccessHandler} that forwards the
     * authenticated request to the original target URI, preserving the request method, headers,
     * parameters, and body. This differs from the default success handler which redirects to a
     * default URL and would break REST API expectations.
     * 
     * <p>
     * Success handler behavior: {@code (req, resp, auth) -> req.getRequestDispatcher(req.requestURI).forward(req, resp)}
     * forwards to the original controller endpoint (e.g., /api/users → /api/users controller) with
     * an authenticated {@code SecurityContext}, continuing the Spring Security filter chain.
     * 
     */
    public ApiTokenHeaderAuthenticationFilter() {
        super( ApiTokenHeaderAuthenticationFilter::checkRequestSupport );

        //on successful authentication, forward the request to the original target
        setAuthenticationSuccessHandler(
                (req, resp, auth) -> { req.getRequestDispatcher(req.getRequestURI()).forward(req, resp); });
    }

    /**
     * Determines if this filter processes the given request based on header presence and authentication state.
     * <p>
     * This {@code RequestMatcher} predicate returns true when both conditions are met:
     * <ol>
     * <li>The {@code X-API-Token} header is present, indicating the client intends token-based authentication</li>
     * <li>The user is not already authenticated, preventing redundant Token lookups and re-authentication</li>
     * </ol>
     * If the user is already authenticated (e.g., via session-based login), this filter skips processing
     * even if the API_TOKEN header is present, providing a performance optimization.
     * 
     *
     * @param request the {@code HttpServletRequest} to check for the API_TOKEN header
     * @return true if the request contains an X-API-Token header AND the user is not authenticated, false otherwise
     */
    static boolean checkRequestSupport(HttpServletRequest request) {
        return request.getHeader(API_TOKEN) != null && !UserProvider.isAuthenticated();
    }

    /**
     * Extracts the token string from the X-API-Token HTTP header.
     * <p>
     * This method implements the abstract {@link AbstractTokenAuthenticationFilter#extractTokenFromRequest(HttpServletRequest)}
     * method by delegating to {@link HttpServletRequest#getHeader(String)} with the {@link URLConstants#API_TOKEN}
     * constant, which defines the {@code X-API-Token} header name.
     * 
     *
     * @param request the {@code HttpServletRequest} containing the X-API-Token header
     * @return the token value from the X-API-Token header, or null if the header is not present
     */
    @Override
    protected String extractTokenFromRequest(HttpServletRequest request) {
        return request.getHeader(API_TOKEN);
    }

    /**
     * Sets authentication details on the token using the authentication details source.
     * <p>
     * This method calls {@code authenticationDetailsSource.buildDetails(request)} to create
     * {@code WebAuthenticationDetails} containing the remote address and session ID from the request.
     * These authentication details are used for audit logging and authentication event tracking.
     * 
     *
     * @param request the {@code HttpServletRequest} for extracting authentication details such as IP address and session ID
     * @param authRequest the {@code UsernamePasswordAuthenticationToken} to populate with details
     */
    protected void setDetails(HttpServletRequest request, UsernamePasswordAuthenticationToken authRequest) {
        authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
    }

    /**
     * Sets the authentication manager using setter injection with lazy initialization.
     * <p>
     * The {@code @Lazy} annotation breaks circular dependencies during Spring Security filter chain
     * construction. This method delegates to the superclass {@code setAuthenticationManager()} to connect
     * the filter to the authentication provider chain, which includes
     * {@code LoginByPasswordOrTokenAuthenticationProvider}.
     * 
     *
     * @param authenticationManager the Spring Security {@code AuthenticationManager} containing the authentication provider chain
     */
    @Autowired
    @Override
    public void setAuthenticationManager(@Lazy AuthenticationManager authenticationManager) {
        super.setAuthenticationManager(authenticationManager);
    }

}