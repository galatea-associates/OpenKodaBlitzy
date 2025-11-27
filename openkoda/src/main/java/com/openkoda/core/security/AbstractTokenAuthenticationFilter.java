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
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.Token;
import com.openkoda.repository.user.TokenRepository;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import reactor.util.function.Tuple2;

/**
 * Abstract base class for Spring Security authentication filters implementing token-based authentication 
 * via Token entity lookup from database.
 * <p>
 * This class uses the template method pattern to define the authentication lifecycle: extract token from 
 * request, lookup Token entity in database, validate token, create RequestTokenAuthenticationToken, and 
 * delegate to AuthenticationManager. Concrete subclasses implement {@code extractTokenFromRequest()} for 
 * transport-specific token extraction.
 * <p>
 * Three concrete implementations exist:
 * <ul>
 * <li>{@code RequestParameterTokenAuthenticationFilter} - extracts token from {@code ?token=...} URL parameter</li>
 * <li>{@code ApiTokenHeaderAuthenticationFilter} - extracts token from {@code X-API-Token} HTTP header</li>
 * <li>{@code TokenPathPrefixAuthenticationFilter} - extracts token from {@code /__t_/{token}/...} URL path prefix</li>
 * </ul>
 * <p>
 * Authentication flow:
 * <ol>
 * <li>{@link #extractTokenFromRequest(HttpServletRequest)} - abstract method retrieves token string from HTTP request</li>
 * <li>{@code tokenRepository.findByBase64UserIdTokenIsValidTrue(token)} - validates token, returns Tuple2 with Token entity and error message</li>
 * <li>{@link #prepareAuthentication(Token)} - creates RequestTokenAuthenticationToken with userId, email, privileges, singleRequest flag</li>
 * <li>{@link #beforeAuthentication(HttpServletRequest, HttpServletResponse, Token)} - extension hook for subclass pre-authentication logic</li>
 * <li>{@code authenticationManager.authenticate(requestToken)} - delegates to LoginByPasswordOrTokenAuthenticationProvider</li>
 * <li>{@link #afterAuthentication(HttpServletRequest, HttpServletResponse, Token)} - extension hook for subclass post-authentication logic</li>
 * </ol>
 * <p>
 * This class extends Spring Security {@link AbstractAuthenticationProcessingFilter}, integrating with the 
 * filter chain and SecurityContext management.
 * <p>
 * Example usage: API client sends {@code X-API-Token: abc123}, ApiTokenHeaderAuthenticationFilter extracts 
 * "abc123", base class validates Token entity, creates RequestTokenAuthenticationToken, 
 * LoginByPasswordOrTokenAuthenticationProvider applies privilege narrowing.
 *
 * @see RequestTokenAuthenticationToken
 * @see com.openkoda.repository.user.TokenRepository#findByBase64UserIdTokenIsValidTrue(String)
 * @see com.openkoda.model.Token
 * @since 1.7.1
 * @author OpenKoda Team
 */
public abstract class AbstractTokenAuthenticationFilter extends AbstractAuthenticationProcessingFilter
        implements URLConstants, LoggingComponentWithRequestId {

    /**
     * TokenRepository for querying Token entity via findByBase64UserIdTokenIsValidTrue(tokenString).
     * Validates token expiration and user association.
     */
    @Inject
    private TokenRepository tokenRepository;

    /**
     * Protected constructor for subclass invocation. Configures RequestMatcher determining which 
     * HTTP requests trigger this filter.
     * <p>
     * Delegates to {@link AbstractAuthenticationProcessingFilter} superclass constructor, setting 
     * the matcher that defines URL patterns requiring token authentication.
     * 
     * <p>
     * Concrete subclasses provide RequestMatcher via Spring configuration, typically using 
     * {@code @Configuration} class with FilterRegistrationBean.
     * 
     *
     * @param requiresAuthenticationRequestMatcher Spring Security RequestMatcher defining URL patterns 
     *        requiring token authentication (e.g., {@code AntPathRequestMatcher("/api/**")})
     */
    public AbstractTokenAuthenticationFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
        super(requiresAuthenticationRequestMatcher);
    }

    /**
     * Extracts token string from request. Subclass must implement transport-specific token 
     * extraction logic.
     * <p>
     * Examples:
     * <ul>
     * <li>{@code ApiTokenHeaderAuthenticationFilter}: return request.getHeader("X-API-Token")</li>
     * <li>{@code RequestParameterTokenAuthenticationFilter}: return request.getParameter("token")</li>
     * <li>{@code TokenPathPrefixAuthenticationFilter}: extract from request.getRequestURI() path segment</li>
     * </ul>
     * 
     *
     * @param request HttpServletRequest containing token in header, parameter, or path
     * @return String token value extracted from request, null if token not present
     */
    protected abstract String extractTokenFromRequest(HttpServletRequest request);

    /**
     * Extension hook for subclass to perform pre-authentication checks or setup. Called after 
     * Token entity validated but before AuthenticationManager.authenticate().
     * <p>
     * Default implementation is no-op (empty method body). Subclasses can override to perform 
     * additional validation or prepare request context.
     * 
     *
     * @param request HttpServletRequest for accessing request details
     * @param response HttpServletResponse for writing errors if needed
     * @param token Token entity loaded from database, contains user, privileges, singleRequest flag, expiration
     */
    protected void beforeAuthentication(HttpServletRequest request, HttpServletResponse response, Token token){};

    /**
     * Extension hook for subclass to perform post-authentication cleanup. Called after successful 
     * AuthenticationManager.authenticate() but before returning Authentication.
     * <p>
     * RequestParameterTokenAuthenticationFilter overrides this to call token.invalidateIfSingleUse() 
     * and tokenRepository.saveAndFlush(), invalidating one-time tokens.
     * 
     *
     * @param request HttpServletRequest for accessing request details
     * @param response HttpServletResponse for writing response if needed
     * @param token Token entity that was used for authentication
     */
    protected void afterAuthentication(HttpServletRequest request, HttpServletResponse response, Token token){};

    /**
     * Creates RequestTokenAuthenticationToken from Token entity for AuthenticationManager processing.
     * <p>
     * Extracts userId from token.getUser().getId(), email from token.getUser().getEmail(), 
     * privileges from token.getPrivilegesSet() (Set&lt;PrivilegeBase&gt;), and singleRequest flag 
     * from token.isSingleRequest().
     * 
     * <p>
     * LoginByPasswordOrTokenAuthenticationProvider will apply privilege narrowing via 
     * OrganizationUser.retainPrivileges() using token.privileges.
     * 
     *
     * @param token Token entity loaded from database with user, privileges, singleRequest flag
     * @return RequestTokenAuthenticationToken - Unauthenticated authentication request with userId, 
     *         email, token string, privileges set, singleRequest flag
     */
    protected Authentication prepareAuthentication(Token token) {
        return new RequestTokenAuthenticationToken(
                token.getUser().getId(),
                token.getUser().getEmail(),
                token.getToken(),
                token.getPrivilegesSet(),
                token.isSingleRequest());
    }

    /**
     * Implements Spring Security AbstractAuthenticationProcessingFilter contract, orchestrates 
     * token-based authentication lifecycle.
     * <p>
     * Authentication lifecycle:
     * <ol>
     * <li>Calls requiresAuthentication(request, response) verifying RequestMatcher matches this request</li>
     * <li>Throws AuthenticationServiceException if matcher does not match (defensive check)</li>
     * <li>Calls extractTokenFromRequest(request) delegating to subclass transport-specific extraction</li>
     * <li>Queries tokenRepository.findByBase64UserIdTokenIsValidTrue(requestToken) returning Tuple2&lt;Token, String&gt;</li>
     * <li>If token null, logs error message ("Token not found", "Token expired", etc.) and throws AuthenticationServiceException</li>
     * <li>Calls prepareAuthentication(token) creating RequestTokenAuthenticationToken</li>
     * <li>Calls beforeAuthentication(request, response, token) extension hook</li>
     * <li>Delegates to authenticationManager.authenticate(requestToken) - LoginByPasswordOrTokenAuthenticationProvider handles RequestTokenAuthenticationToken</li>
     * <li>Calls afterAuthentication(request, response, token) extension hook</li>
     * <li>Returns authenticated Authentication with OrganizationUser principal</li>
     * </ol>
     * 
     * <p>
     * Token validation: findByBase64UserIdTokenIsValidTrue() checks Token.valid=true, 
     * Token.expirationDate &gt; now, User.enabled=true.
     * 
     * <p>
     * Logging: debug logs request.servletPath, warn logs failed authentication attempts with error messages.
     * 
     *
     * @param request HttpServletRequest to extract token from
     * @param response HttpServletResponse for error handling
     * @return Authentication - Authenticated RequestTokenAuthenticationToken with OrganizationUser principal 
     *         after successful authentication
     * @throws AuthenticationServiceException if token extraction fails, token validation fails, or 
     *         authentication not supported for this request
     */
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        debug("[attemptAuthentication] token auth for {}", request.getServletPath());
        if (!requiresAuthentication(request, response)) {
            warn("[attemptAuthentication] passed check but attempted auth {}", request.getRequestURI());
            throw new AuthenticationServiceException("Authentication not supported");
        }
        String requestToken = extractTokenFromRequest(request);
        Tuple2<Token, String> t = tokenRepository.findByBase64UserIdTokenIsValidTrue(requestToken);
        if (t.getT1() == null) {
            warn("[attemptAuthentication] {}", t.getT2());
            throw new AuthenticationServiceException(t.getT2());
        }
        Token token = t.getT1();
        Authentication apiHeaderToken = prepareAuthentication(token);
        beforeAuthentication(request, response, token);
        Authentication result = this.getAuthenticationManager().authenticate(apiHeaderToken);
        afterAuthentication(request, response, token);
        return result;
    }

    /**
     * Setter injection for Spring Security AuthenticationManager with @Lazy annotation to avoid 
     * circular dependency during bean initialization.
     * <p>
     * Delegates to superclass setAuthenticationManager() connecting filter to authentication 
     * provider chain.
     * <p>
     * {@code @Lazy} breaks circular dependency: filter needs AuthenticationManager, security config needs 
     * filter for chain construction.
     *
     * @param authenticationManager AuthenticationManager containing LoginByPasswordOrTokenAuthenticationProvider 
     *        for processing RequestTokenAuthenticationToken
     */
    @Autowired
    @Override
    public void setAuthenticationManager(@Lazy AuthenticationManager authenticationManager) {
        super.setAuthenticationManager(authenticationManager);
    }
}