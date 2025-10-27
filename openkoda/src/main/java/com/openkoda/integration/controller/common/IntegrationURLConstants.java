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

package com.openkoda.integration.controller.common;

import com.openkoda.controller.common.URLConstants;

/**
 * Constants for integration module URL paths, path variables, and request parameters.
 * <p>
 * This interface centralizes routing keys and OAuth callback parameter names used by
 * integration controllers and views. All constants are implicitly {@code public static final}
 * per Java interface contract, enabling compile-time constant inlining by the JVM.
 * </p>
 * <p>
 * The interface-as-constants pattern provides type-safe, discoverable URL fragments that
 * ensure consistent route registration across Trello, GitHub, Slack, Microsoft Teams,
 * Jira, and Basecamp integrations. Controllers concatenate these constants in
 * {@code @GetMapping} and {@code @PostMapping} annotations to build complete endpoint paths.
 * </p>
 * <p>
 * Example controller usage:
 * <pre>{@code
 * @GetMapping(IntegrationURLConstants._INTEGRATION + IntegrationURLConstants._GITHUB)
 * public ModelAndView githubCallback(@RequestParam(IntegrationURLConstants.CODE) String code) {
 *     // OAuth callback handler
 * }
 * }</pre>
 * </p>
 * <p>
 * <strong>Important:</strong> Changing these constants requires coordinated updates to
 * controller endpoints, integration tests, OAuth provider configurations, and view templates.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.controller.common.URLConstants
 */
public interface IntegrationURLConstants extends URLConstants {

    /** Base module path identifier for integration subsystem. */
    String INTEGRATION = "integration";
    
    /** Leading slash prefix for integration module routes, computed as {@code '/' + INTEGRATION}. */
    String _INTEGRATION = "/" + INTEGRATION;
    
    /** URL path segment for Trello integration endpoints. */
    String _TRELLO = "/trello";
    
    /** URL path segment for GitHub integration endpoints. */
    String _GITHUB = "/github";
    
    /** URL path segment for Slack integration endpoints. */
    String _SLACK = "/slack";
    
    /** URL path segment for Microsoft Teams integration endpoints. */
    String _MSTEAMS = "/msteams";
    
    /** URL path segment for Jira integration endpoints. */
    String _JIRA = "/jira";
    
    /** URL path segment for Basecamp integration endpoints. */
    String _BASECAMP = "/basecamp";
    
    /** URL path segment for OAuth disconnection action. */
    String _DISCONNECT = "/disconnect";

    // Request parameters
    
    /** OAuth 2.0 standard authorization code parameter name for callback handling. */
    String CODE = "code";
    
    /** OAuth 2.0 standard CSRF protection state parameter name for callback validation. */
    String STATE = "state";

}
