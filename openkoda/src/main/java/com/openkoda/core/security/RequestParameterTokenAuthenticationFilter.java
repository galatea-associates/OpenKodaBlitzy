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

import com.openkoda.core.configuration.RequestParameterTokenAuthenticationSuccessHandler;
import com.openkoda.model.Token;
import com.openkoda.repository.user.TokenRepository;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Spring Security authentication filter for token authentication via {@code ?token=...} URL query parameter.
 * <p>
 * This filter extends {@link AbstractTokenAuthenticationFilter} and implements {@code extractTokenFromRequest()}
 * to read tokens from the request parameter named "token". It is designed for GET-only requests with
 * auto-expiring single-use tokens embedded in URLs, typically used in email links to protected resources
 * (download file, view report, reset password) where users click links without prior authentication.
 * <p>
 * <b>Request Matcher:</b> The static method {@code checkRequestSupport()} verifies that the request uses
 * the GET method AND contains a non-blank "token" parameter. POST, PUT, and DELETE methods are not supported
 * for security reasons, as tokens in URLs are logged in browser history, proxy logs, and referrer headers.
 * <p>
 * <b>Success Handler:</b> Configures {@link RequestParameterTokenAuthenticationSuccessHandler} which persists
 * the SecurityContext to the HTTP session, enabling subsequent requests to use session authentication without
 * requiring the token parameter. After the first authenticated request, users can navigate the application
 * using standard session cookies.
 * <p>
 * <b>Single-Use Token Invalidation:</b> The {@code afterAuthentication()} method calls
 * {@code token.invalidateIfSingleUse()} and {@code tokenRepository.saveAndFlush()} to mark single-use tokens
 * as invalid after successful authentication. This prevents token reuse and unauthorized sharing.
 * <p>
 * <b>Usage Example:</b>
 * <pre>
 * // User clicks email link: GET /download/report?token=xyz789
 * // 1. Filter extracts "xyz789" and validates Token entity
 * // 2. Creates RequestTokenAuthenticationToken
 * // 3. LoginByPasswordOrTokenAuthenticationProvider authenticates
 * // 4. afterAuthentication() invalidates token (token.valid=false in database)
 * // 5. Success handler saves SecurityContext to session
 * // 6. Subsequent requests use session authentication without token
 * </pre>
 *
 * @see AbstractTokenAuthenticationFilter
 * @see RequestParameterTokenAuthenticationSuccessHandler
 * @see Token#invalidateIfSingleUse()
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Service("requestParameterTokenAuthenticationFilter")
public class RequestParameterTokenAuthenticationFilter extends AbstractTokenAuthenticationFilter {

    /**
     * TokenRepository for saving invalidated single-use tokens via {@code saveAndFlush()}.
     */
    @Inject
    private TokenRepository tokenRepository;

    /**
     * SecurityContextRepository injected into RequestParameterTokenAuthenticationSuccessHandler
     * for session persistence after successful authentication.
     */
    @Autowired
    SecurityContextRepository securityContextRepository;

    /**
     * Constructs the filter with GET-only request matcher and session-persisting success handler.
     * <p>
     * Configures the superclass with a RequestMatcher via method reference
     * {@code RequestParameterTokenAuthenticationFilter::checkRequestSupport}, which ensures only GET requests
     * with a "token" parameter are processed by this filter.
     * 
     * <p>
     * Sets a custom {@link RequestParameterTokenAuthenticationSuccessHandler} that saves the SecurityContext
     * to the HttpSession via {@code securityContextRepository}. This enables users to access protected resources
     * in subsequent requests without the token parameter.
     * 
     * <p>
     * <b>Typical flow:</b> First request with {@code ?token=abc} authenticates and creates a session.
     * Subsequent requests use the session cookie for authentication without requiring the token parameter.
     * This differs from {@code ApiTokenHeaderAuthenticationFilter}, which forwards without session creation
     * (stateless REST API pattern).
     * 
     */
    public RequestParameterTokenAuthenticationFilter() {
        super( RequestParameterTokenAuthenticationFilter::checkRequestSupport );
        setAuthenticationSuccessHandler(new RequestParameterTokenAuthenticationSuccessHandler(securityContextRepository));
    }

    /**
     * RequestMatcher predicate that determines if this filter processes the given request.
     * <p>
     * Returns {@code true} if the request method equals "GET" AND the request contains a non-blank
     * "token" parameter. This dual condition ensures:
     * 
     * <ol>
     *   <li><b>GET method only</b> - Prevents CSRF attacks via token-in-URL for POST/PUT/DELETE operations.
     *       Tokens in URLs are logged in browser history, proxy logs, and referrer headers, making them
     *       unsuitable for state-changing operations.</li>
     *   <li><b>Token parameter present</b> - {@code StringUtils.isNotBlank()} checks for non-null,
     *       non-empty, and non-whitespace token values.</li>
     * </ol>
     *
     * @param request HttpServletRequest to check for GET method and token parameter
     * @return true if request method is GET and token parameter is present and non-blank, false otherwise
     */
    static boolean checkRequestSupport(HttpServletRequest request) {
        return request.getMethod().equals("GET") && StringUtils.isNotBlank(request.getParameter(TOKEN));
    }

    /**
     * Implements {@link AbstractTokenAuthenticationFilter} abstract method to extract the token string
     * from the {@code ?token=...} URL query parameter.
     * <p>
     * Delegates to {@code HttpServletRequest.getParameter()} for query parameter extraction.
     * The constant {@code TOKEN} defines the parameter name "token".
     * 
     *
     * @param request HttpServletRequest containing the token query parameter
     * @return String token value from {@code request.getParameter(TOKEN)}, null if parameter not present
     */
    @Override
    protected String extractTokenFromRequest(HttpServletRequest request) {
        return request.getParameter(TOKEN);
    }

    /**
     * Overrides {@link AbstractTokenAuthenticationFilter} extension hook to invalidate single-use tokens
     * after successful authentication.
     * <p>
     * Calls {@code token.invalidateIfSingleUse()} which checks the {@code token.singleRequest} flag.
     * If true, sets {@code token.valid=false} to mark the token as invalid and returns the token instance
     * (fluent API). The invalidated token is then persisted immediately via
     * {@code tokenRepository.saveAndFlush()}, which flushes the Hibernate session to the database.
     * 
     * <p>
     * <b>Security benefit:</b> Prevents token reuse. A second GET request with the same token will fail
     * authentication because {@code token.valid=false}. This makes email link tokens one-time-use,
     * preventing unauthorized sharing or replay attacks.
     * 
     *
     * @param request HttpServletRequest (unused in this implementation)
     * @param response HttpServletResponse (unused in this implementation)
     * @param token Token entity to potentially invalidate based on singleRequest flag
     */
    @Override
    protected void afterAuthentication(HttpServletRequest request, HttpServletResponse response, Token token) {
        tokenRepository.saveAndFlush(token.invalidateIfSingleUse());
    }

    /**
     * Sets authentication details on the token using {@code authenticationDetailsSource}.
     * <p>
     * Delegates to {@code authenticationDetailsSource.buildDetails(request)} to extract authentication
     * details such as IP address and session ID for audit logging purposes.
     * 
     *
     * @param request HttpServletRequest for extracting details (IP address, session ID)
     * @param authRequest UsernamePasswordAuthenticationToken to populate with WebAuthenticationDetails
     */
    protected void setDetails(HttpServletRequest request, UsernamePasswordAuthenticationToken authRequest) {
        authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
    }

    /**
     * Setter injection for Spring Security {@link AuthenticationManager} with {@code @Lazy} annotation
     * to avoid circular dependency during bean initialization.
     * <p>
     * Delegates to superclass {@code setAuthenticationManager()} to connect this filter to the
     * authentication provider chain. The {@code @Lazy} annotation breaks circular dependencies that
     * occur during Spring Security configuration, where the filter needs the AuthenticationManager
     * and the security configuration needs the filter for chain construction.
     * 
     *
     * @param authenticationManager AuthenticationManager containing LoginByPasswordOrTokenAuthenticationProvider
     *                              for processing RequestTokenAuthenticationToken
     */
    @Autowired
    @Override
    public void setAuthenticationManager(@Lazy AuthenticationManager authenticationManager) {
        super.setAuthenticationManager(authenticationManager);
    }

}
