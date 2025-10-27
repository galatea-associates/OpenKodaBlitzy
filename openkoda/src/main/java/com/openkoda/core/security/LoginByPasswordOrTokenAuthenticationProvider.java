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
import com.openkoda.model.authentication.LoggedUser;
import com.openkoda.repository.user.TokenRepository;
import jakarta.inject.Inject;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Primary secure authentication provider handling both form-based password authentication and token-based authentication.
 * <p>
 * This provider extends Spring Security's {@link DaoAuthenticationProvider} to inherit BCrypt password verification,
 * UserDetailsService integration, and account status checks. It supports dual authentication mechanisms:
 * password login via {@link UsernamePasswordAuthenticationToken} and token-based authentication via 
 * {@link RequestTokenAuthenticationToken}.
 * </p>
 * <p>
 * Password authentication delegates to superclass {@code additionalAuthenticationChecks()} for BCrypt verification
 * against the user's stored password. Token authentication applies privilege narrowing and sets single-request flags
 * based on token metadata.
 * </p>
 * <p>
 * The privilege narrowing mechanism allows API tokens to restrict (but not extend) a user's full privileges.
 * When a token specifies limited privileges via {@code Token.privileges}, the {@link OrganizationUser#retainPrivileges(Set)}
 * method removes any privileges not in the token's allowed set, ensuring token-based authentication operates with 
 * minimal required permissions.
 * </p>
 * <p>
 * Registered as {@code @Service("loginByPasswordOrTokenAuthenticationProvider")}, this provider integrates with 
 * Spring Security's AuthenticationManager provider chain to handle authentication requests.
 * </p>
 * <p>
 * Usage examples:
 * </p>
 * <pre>
 * // Form login: LoginAndPasswordAuthenticationFilter creates token
 * UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
 * // This provider validates password via superclass
 * 
 * // API token: AbstractTokenAuthenticationFilter creates token
 * RequestTokenAuthenticationToken tokenAuth = new RequestTokenAuthenticationToken(token);
 * // This provider applies privilege narrowing
 * </pre>
 *
 * @see RequestTokenAuthenticationToken
 * @see OrganizationUser#retainPrivileges(Set)
 * @see DaoAuthenticationProvider
 * @see AbstractTokenAuthenticationFilter
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Service("loginByPasswordOrTokenAuthenticationProvider")
public class LoginByPasswordOrTokenAuthenticationProvider extends DaoAuthenticationProvider
        implements URLConstants, HasSecurityRules, LoggingComponentWithRequestId {

    /**
     * Configures the UserDetailsService for loading user details during authentication.
     * <p>
     * This setter injection method receives an {@link OrganizationUserDetailsService} instance that loads
     * {@code User} entities and builds {@link OrganizationUser} principals. The service is delegated to
     * the superclass to enable {@link DaoAuthenticationProvider}'s password verification mechanism.
     * </p>
     * <p>
     * Spring calls this setter during bean wiring to connect the authentication provider to the user loading
     * service, establishing the link between authentication requests and user data retrieval.
     * </p>
     *
     * @param userDetailsService the {@link OrganizationUserDetailsService} instance for loading user details
     */
    @Inject
    @Override
    public void setUserDetailsService(UserDetailsService userDetailsService) {
        super.setUserDetailsService(userDetailsService);
    }

    /**
     * Repository for querying Token entity metadata.
     * <p>
     * Note: This field is injected but not currently used in this class. It may be a legacy field
     * from previous implementations.
     * </p>
     */
    @Inject
    private TokenRepository tokenRepository;

    /**
     * Declares support for both password and token authentication types.
     * <p>
     * This provider supports {@link UsernamePasswordAuthenticationToken} for password-based login
     * (inherited from superclass) and {@link RequestTokenAuthenticationToken} for token-based
     * authentication. Spring Security calls this method to route Authentication instances to the
     * appropriate provider.
     * </p>
     * <p>
     * Dual support enables a single provider to handle both password and token authentication flows,
     * simplifying the authentication configuration.
     * </p>
     *
     * @param authentication the Class type of Authentication being checked
     * @return {@code true} if authentication is {@link UsernamePasswordAuthenticationToken} or 
     *         {@link RequestTokenAuthenticationToken} subclass, {@code false} otherwise
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return (super.supports(authentication) || RequestTokenAuthenticationToken.class.isAssignableFrom(authentication));
    }

    /**
     * Creates a new authentication provider instance.
     * <p>
     * The constructor retrieves {@code preAuthenticationChecks} from the superclass, which performs
     * account status validation (enabled, non-expired, non-locked) before authentication. Currently,
     * the checks are retrieved but not modified, which may indicate legacy code.
     * </p>
     */
    public LoginByPasswordOrTokenAuthenticationProvider() {
        UserDetailsChecker checks = getPreAuthenticationChecks();
    }

    /**
     * Creates an authenticated token after successful authentication.
     * <p>
     * Delegates to the superclass to create an authenticated {@link UsernamePasswordAuthenticationToken}
     * with the user's principal and authorities. No custom logic is needed as the superclass handles
     * standard authentication token creation.
     * </p>
     *
     * @param principal typically an {@link OrganizationUser} from UserDetailsService
     * @param authentication the original unauthenticated Authentication request
     * @param user the UserDetails loaded from {@link OrganizationUserDetailsService}
     * @return authenticated {@link UsernamePasswordAuthenticationToken} with OrganizationUser principal and authorities
     */
    @Override
    protected Authentication createSuccessAuthentication(Object principal, Authentication authentication, UserDetails user) {
        return super.createSuccessAuthentication(principal, authentication, user);
    }


    /**
     * Performs authentication-type-specific validation.
     * <p>
     * This method handles three authentication types:
     * </p>
     * <ol>
     * <li><b>PreauthenticatedReloadUserToken</b>: Logs warning "not supported" and skips checks.
     *     The reload flow is handled elsewhere in the system.</li>
     * <li><b>RequestTokenAuthenticationToken</b>: Applies privilege narrowing and configures token-specific settings:
     *     <ul>
     *       <li>If the token has privileges, calls {@link OrganizationUser#retainPrivileges(Set)} to remove
     *           any privileges not in the token's allowed set</li>
     *       <li>Sets single-request auth flag if the token is one-time-use, triggering invalidation after use</li>
     *       <li>Sets authentication method to TOKEN for audit logging</li>
     *     </ul>
     * </li>
     * <li><b>UsernamePasswordAuthenticationToken</b>: Delegates to superclass for BCrypt password verification
     *     against {@code UserDetails.password}</li>
     * </ol>
     * <p>
     * The privilege narrowing mechanism ensures token-based authentication operates with minimal required permissions.
     * Tokens can restrict but not extend a user's full privilege set.
     * </p>
     *
     * @param userDetails the {@link OrganizationUser} principal loaded from {@link OrganizationUserDetailsService}
     * @param authentication the Authentication request ({@link UsernamePasswordAuthenticationToken}, 
     *                       {@link RequestTokenAuthenticationToken}, or {@link PreauthenticatedReloadUserToken})
     * @throws org.springframework.security.authentication.BadCredentialsException if password verification fails
     *         (superclass behavior for UsernamePasswordAuthenticationToken)
     */
    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) {
        debug("[additionalAuthenticationChecks]");
        if (authentication instanceof PreauthenticatedReloadUserToken) {
            //not supported
            warn("[additionalAuthenticationChecks] PreauthenticatedReloadUserToken not supported");
        } else if (authentication instanceof RequestTokenAuthenticationToken) {
            debug("[additionalAuthenticationChecks]");
            RequestTokenAuthenticationToken requestToken = (RequestTokenAuthenticationToken) authentication;
            //narrow down privileges given to the user if the token requires it
            OrganizationUser ou = (OrganizationUser) userDetails;
            if (requestToken.hasPrivileges() ) {
                ou.retainPrivileges(requestToken.getPrivileges());
            }
            ou.setSingleRequestAuth(requestToken.isSingleRequest());
            ou.setAuthMethod(LoggedUser.AuthenticationMethods.TOKEN);
        } else {
            super.additionalAuthenticationChecks(userDetails, authentication);
        }

    }

}
