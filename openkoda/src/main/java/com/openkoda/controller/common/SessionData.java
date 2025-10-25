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

package com.openkoda.controller.common;

/**
 * HTTP session attribute key constants for user context and state management.
 * <p>
 * Defines session attribute keys used for storing authenticated user state, selected organization
 * in multi-tenant context, and locale preferences. Controllers and filters access these constants
 * via {@code @SessionAttribute} annotation or {@code HttpSession} directly. Each attribute is
 * session-scoped per user and survives across requests until logout or session timeout.
 * </p>
 * <p>
 * Session attributes are populated during login by authentication filters and updated when users
 * change organization context or locale preferences. The session is cleared on logout or expiration.
 * </p>
 * <p>
 * Usage example:
 * <pre>{@code
 * // In controller method
 * @SessionAttribute(SessionData.CURRENT_ORGANIZATION_ID) Long orgId
 * }</pre>
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @version 1.7.1
 * @since 2019-03-08
 * @see javax.servlet.http.HttpSession
 */
public interface SessionData {

    /**
     * Session attribute key for storing the original User entity during admin impersonation.
     * <p>
     * When an administrator activates user impersonation (spoofing), this attribute stores the
     * original authenticated User entity to allow reverting back to the admin's session. The
     * value type is {@code com.openkoda.model.User}. Authentication filters check this attribute
     * to determine if the current session is spoofed, and controllers display an "Exit Spoof"
     * option when present.
     * </p>
     * <p>
     * This attribute is populated when an admin activates user impersonation and cleared when
     * the spoofing session ends via the exit spoof action or logout.
     * </p>
     *
     * @see com.openkoda.model.User
     */
    String SPOOFING_USER = "spoofingUser";

    /**
     * Session attribute key for storing the selected user locale for internationalization.
     * <p>
     * Stores the user's selected locale as a language code string (e.g., "en", "pl") for
     * internationalization and message bundle selection. The value is used by Spring's
     * {@code LocaleResolver} to determine which resource bundle to use for UI text rendering.
     * </p>
     * <p>
     * This attribute is populated on login from user preferences or the browser's Accept-Language
     * header. Users can change the locale through the UI language switcher, which updates this
     * session attribute and affects subsequent page renders.
     * </p>
     *
     * @see org.springframework.web.servlet.LocaleResolver
     */
    String LOCALE = "locale";

    /**
     * Session attribute key for storing the selected Organization ID in multi-tenant context.
     * <p>
     * Stores the primary key ({@code Long}) of the currently selected Organization for multi-tenant
     * context scoping. Controllers use this value to filter organization-scoped data queries and
     * enforce tenant isolation. The stored ID is validated against the authenticated user's
     * organization memberships to prevent unauthorized access.
     * </p>
     * <p>
     * This attribute is populated during login with the user's default organization or when the
     * user explicitly switches organizations through the UI organization selector. All subsequent
     * requests use this context for data access and privilege evaluation.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * Long orgId = (Long) session.getAttribute(SessionData.CURRENT_ORGANIZATION_ID);
     * }</pre>
     * </p>
     *
     * @see com.openkoda.model.Organization
     */
    String CURRENT_ORGANIZATION_ID = "currentOrganizationId";

}
