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
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

/**
 * Spring Security authentication failure handler that redirects users to the login page with error parameters.
 * <p>
 * Implements {@link AuthenticationFailureHandler} to customize error handling after failed login attempts.
 * Analyzes the authentication exception type and redirects to appropriate error page with query parameters
 * indicating the failure reason. Distinguishes between disabled accounts (verification required) and general
 * authentication failures.
 * </p>
 * <p>
 * Redirect behavior:
 * <ul>
 * <li>Redirects to /login?verificationError for {@link DisabledException} (account not verified)</li>
 * <li>Redirects to /login?error for all other authentication failures</li>
 * </ul>
 * </p>
 * <p>
 * Thread-safety: This is a thread-safe stateless singleton with no mutable fields. Can safely handle
 * concurrent authentication failures.
 * </p>
 * <p>
 * Spring Security integration: Registered in WebSecurityConfig via
 * {@code formLogin().failureHandler(customAuthenticationFailureHandler)} to intercept failed login attempts.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AuthenticationFailureHandler
 * @see CustomAuthenticationSuccessHandler
 * @see DisabledException
 */
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    /**
     * Handles authentication failure by redirecting to login page with appropriate error parameter.
     * <p>
     * Invoked by Spring Security when authentication fails. Examines the exception type to determine
     * redirect target: /login?verificationError for disabled accounts (user not verified), /login?error
     * for all other failures (bad credentials, locked account, etc.).
     * </p>
     * <p>
     * Implementation uses {@link HttpServletResponse#sendRedirect(String)} for client-side redirect.
     * Query parameter added to URL signals error type to login page for user feedback message display.
     * </p>
     *
     * @param request the HTTP request that triggered authentication (unused but required by interface)
     * @param response the HTTP response used for redirect to error page
     * @param e the authentication exception that caused the failure ({@link DisabledException} for disabled
     *          accounts, BadCredentialsException for wrong password, etc.)
     * @throws IOException if redirect fails due to I/O error
     * @throws ServletException if servlet error occurs during redirect (not thrown in current implementation)
     * @see DisabledException
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
        response.sendRedirect("/login?" + (e instanceof DisabledException ? "verificationError" : "error"));
    }
}
