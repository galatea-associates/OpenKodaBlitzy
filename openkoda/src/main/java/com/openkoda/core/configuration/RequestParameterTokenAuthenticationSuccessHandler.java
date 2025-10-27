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

package com.openkoda.core.configuration;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;

import java.io.IOException;

import static com.openkoda.controller.common.URLConstants.TOKEN;

/**
 * Spring Security authentication success handler for token-based authentication that removes TOKEN parameter after successful login.
 * <p>
 * Extends SimpleUrlAuthenticationSuccessHandler to handle token-based authentication flows. After successful
 * authentication via URL parameter token (e.g., /resource?TOKEN=abc123), persists SecurityContext and redirects
 * to same URL with TOKEN parameter stripped to prevent token reuse or exposure in browser history. Used for
 * one-time authentication links and API key authentication flows.
 * </p>
 * <p>
 * Removing TOKEN parameter after authentication prevents token leakage via referer headers, browser history, or
 * log files. Token should only be valid for single authentication attempt.
 * </p>
 * <p>
 * <b>WARNING:</b> getUrlWithoutTokenParameter() calls request.getQueryString() which may return null if no query
 * string exists, causing NullPointerException. Also does not normalize leftover ampersands (e.g., '?&param=value'
 * or '?param1=value&&param2=value').
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SimpleUrlAuthenticationSuccessHandler
 * @see SecurityContextRepository
 * @see com.openkoda.controller.common.URLConstants#TOKEN
 */
public class RequestParameterTokenAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private SecurityContextRepository securityContextRepository;

    /**
     * Constructs token authentication success handler with security context repository.
     *
     * @param securityContextRepository Spring Security repository for persisting authentication context between requests
     */
    public RequestParameterTokenAuthenticationSuccessHandler(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Handles successful token authentication by persisting security context and redirecting to URL without token parameter.
     * <p>
     * Invoked by Spring Security after successful token-based authentication. First saves SecurityContext to
     * repository for session persistence. Then constructs redirect URL by stripping TOKEN parameter from original
     * request URL using regex replacement. Redirects to cleaned URL to prevent token reuse.
     * </p>
     *
     * @param request the HTTP request containing TOKEN parameter that triggered authentication
     * @param response the HTTP response used for redirect to cleaned URL
     * @param authentication the Authentication object containing authenticated principal
     * @throws IOException if redirect fails due to I/O error
     * @throws ServletException if servlet error occurs during authentication handling
     * @see #getUrlWithoutTokenParameter(HttpServletRequest)
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        this.securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
        String redirectUrl = getUrlWithoutTokenParameter(request);
        response.sendRedirect(redirectUrl);
    }

    /**
     * Constructs redirect URL with TOKEN parameter removed from query string.
     * <p>
     * Uses regex to strip TOKEN parameter and its value from query string. Pattern matches 'TOKEN=[alphanumeric_-]+'
     * with optional trailing equals signs. Reconstructs URL with servlet path plus cleaned query string. Empty query
     * string results in URL without '?' separator.
     * </p>
     * <p>
     * <b>WARNING:</b> Does not check for null query string which causes NullPointerException if request has no query
     * parameters. Does not normalize resulting ampersands (may produce '?&param=value').
     * </p>
     *
     * @param request the HTTP request containing query string to process
     * @return servlet path with TOKEN parameter removed from query string (e.g., '/resource?other=value' from '/resource?TOKEN=abc123&other=value')
     */
    private String getUrlWithoutTokenParameter(HttpServletRequest request) {
        String queryReplacement = request.getQueryString().replaceAll(TOKEN + "=[0-9a-zA-Z_\\-]+[=]*", "");
        return request.getServletPath()
                + (StringUtils.isEmpty(queryReplacement) ? "" : "?" + queryReplacement);
    }
}
