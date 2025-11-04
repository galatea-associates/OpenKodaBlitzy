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

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Data SpEL EvaluationContextExtension providing custom "security" root object for repository method
 * security expressions.
 * <p>
 * This extension solves a critical problem: Spring Security sets the principal object as a String for anonymous
 * authentication, causing ClassCastException in @PreAuthorize rules expecting OrganizationUser. This class
 * intercepts the SpEL evaluation context and provides OrganizationUser.empty singleton for anonymous principals
 * (null authentication or String principal), ensuring security expressions evaluate safely.
 * <p>
 * Integrates with HasSecurityRules {@code @Query} JPQL expressions (CHECK_CAN_READ_ORG_DATA_JPQL patterns) and
 * {@code @PreAuthorize} method security. Registered as Spring Data SpEL extension via EvaluationContextExtension
 * interface, automatically detected by Spring Data repositories.
 * <p>
 * Example usage:
 * <pre>
 * // Repository method with @Query accessing principal
 * &#64;Query("SELECT o FROM Organization o WHERE o.id IN ?#{principal.organizationIds}")
 * List&lt;Organization&gt; findUserOrganizations();
 * 
 * // For anonymous users, principal.organizationIds safely resolves to empty set
 * // avoiding NullPointerException or ClassCastException
 * </pre>
 *
 * @see OrganizationUser#empty
 * @see HasSecurityRules
 * @see org.springframework.data.spel.spi.EvaluationContextExtension
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class SecurityEvaluationContextExtension implements EvaluationContextExtension, LoggingComponentWithRequestId {

    /**
     * Returns the "security" identifier registering this extension with Spring Data SpEL evaluation context.
     * <p>
     * This overrides the default Spring Security extension, replacing String principal with OrganizationUser
     * for anonymous contexts in repository security expressions.
     * 
     *
     * @return "security" - Extension identifier used by Spring Data to register root object in SpEL expressions
     *         (accessible as ?#{principal})
     */
    @Override
    public String getExtensionId() {
        return "security";
    }

    /**
     * Creates SecurityExpressionRoot with OrganizationUser principal for SpEL evaluation in repository security
     * expressions.
     * <p>
     * Handles four scenarios:
     * <ol>
     * <li>Authentication is null → creates AnonymousAuthenticationToken with OrganizationUser.empty</li>
     * <li>Principal is null → creates AnonymousAuthenticationToken with OrganizationUser.empty</li>
     * <li>Principal is String (Spring Security default for anonymous) → creates AnonymousAuthenticationToken
     * with OrganizationUser.empty</li>
     * <li>Otherwise → uses existing Authentication with OrganizationUser principal</li>
     * </ol>
     * 
     * <p>
     * OrganizationUser.empty has nonExistingPrivilege sentinel allowing privilege checks to fail gracefully
     * without NullPointerException. Logging calls (trace/debug) track anonymous authentication detection.
     * 
     *
     * @return SecurityExpressionRoot wrapping Authentication with OrganizationUser principal (empty singleton
     *         for anonymous, actual user for authenticated)
     */
    @Override
    public SecurityExpressionRoot getRootObject() {
        trace("[getRootObject]");
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getPrincipal() == null
                || String.class.equals(a.getPrincipal().getClass())) {
            debug("[getRootObject] AnonymousAuthenticationToken ");
            a = new AnonymousAuthenticationToken("key", OrganizationUser.empty,  AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        }
        return new SecurityExpressionRoot(a) {};
    }

}
