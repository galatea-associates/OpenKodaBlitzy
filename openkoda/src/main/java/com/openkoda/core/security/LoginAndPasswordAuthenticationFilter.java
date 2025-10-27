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

import com.openkoda.core.configuration.CustomAuthenticationFailureHandler;
import com.openkoda.core.configuration.CustomAuthenticationSuccessHandler;
import com.openkoda.repository.user.UserRepository;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Spring Security form-based authentication filter for username/password login that maps legacy
 * login identifiers to canonical email addresses.
 * <p>
 * This filter extends {@link UsernamePasswordAuthenticationFilter} to provide traditional HTML form
 * POST authentication via the /login endpoint. Its key responsibility is mapping legacy login
 * identifiers (stored in LoginAndPassword.login field) to canonical email usernames (stored in
 * User.email field) via {@link UserRepository#findUsernameLowercaseByLogin(String)}.
 * </p>
 * <p>
 * The filter integrates with {@link CustomAuthenticationSuccessHandler} for role-based post-login
 * redirects:
 * </p>
 * <ul>
 *   <li>Global admin users (superuser privilege) → dashboard</li>
 *   <li>Single organization users → organization settings page</li>
 *   <li>Multiple organization users → organization selection page</li>
 * </ul>
 * <p>
 * It also integrates with {@link CustomAuthenticationFailureHandler} for login error handling
 * and user feedback.
 * </p>
 * <p>
 * <b>Use Case:</b> Supports organizations that allow users to authenticate with custom login
 * identifiers (such as employee ID or username) distinct from the email address stored in the
 * User entity.
 * </p>
 * <p>
 * <b>Login/Email Distinction:</b>
 * </p>
 * <ul>
 *   <li>LoginAndPassword.login: custom identifier chosen by user (may be username, employee ID, or email)</li>
 *   <li>User.email: canonical lowercase email address used as Spring Security username</li>
 *   <li>This filter bridges the gap by looking up email from login identifier</li>
 * </ul>
 * <p>
 * <b>Authentication Flow Example:</b>
 * </p>
 * <pre>
 * 1. User submits form: username="john.doe", password="secret123"
 * 2. obtainUsername() queries: findUsernameLowercaseByLogin("john.doe") → "john.doe@example.com"
 * 3. Filter creates UsernamePasswordAuthenticationToken with email
 * 4. LoginByPasswordOrTokenAuthenticationProvider validates credentials
 * 5. CustomAuthenticationSuccessHandler redirects based on user's organization membership
 * </pre>
 *
 * @see CustomAuthenticationSuccessHandler
 * @see CustomAuthenticationFailureHandler
 * @see UserRepository#findUsernameLowercaseByLogin(String)
 * @see LoginByPasswordOrTokenAuthenticationProvider
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Service("loginAndPasswordAuthenticationFilter")
public class LoginAndPasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    @Inject
    private UserRepository userRepository;

    /**
     * Constructs the authentication filter with role-based redirect URLs and authentication handlers.
     * <p>
     * This constructor initializes the filter with customizable redirect URLs based on the user's
     * organization membership and privilege level. It instantiates {@link CustomAuthenticationSuccessHandler}
     * to perform intelligent post-login redirects and {@link CustomAuthenticationFailureHandler} to
     * render login error messages.
     * </p>
     * <p>
     * <b>Redirect Logic:</b> The success handler evaluates the OrganizationUser principal after
     * authentication and redirects according to the following rules:
     * </p>
     * <ul>
     *   <li>If user.isSuperUser() (canManageBackend privilege) → pageAfterAuthForGlobalAdmin</li>
     *   <li>If user.organizationNames.size() == 1 → pageAfterAuthForOneOrganization (formatted with organizationId)</li>
     *   <li>If user.organizationNames.size() &gt; 1 → pageAfterAuthForMultipleOrganizations</li>
     * </ul>
     * <p>
     * <b>Typical Property Configuration:</b>
     * </p>
     * <pre>
     * page.after.auth.for.multiple.organizations=/html/organization/all
     * page.after.auth.for.one.organization=/html/organization/%s/settings
     * page.after.auth.for.global.admin=/html/dashboard
     * </pre>
     *
     * @param pageAfterAuthForMultipleOrganizations redirect URL for users belonging to multiple
     *        organizations, defaults to "/html/organization/all" (organization selection page)
     * @param pageAfterAuthForOneOrganization redirect URL template for users belonging to a single
     *        organization, defaults to "/html/organization/%s/settings" (organization settings page
     *        with organizationId substitution)
     * @param pageAfterAuthForGlobalAdmin redirect URL for global admin users with superuser privilege,
     *        defaults to "/html/dashboard" (admin dashboard)
     * @param securityContextRepository SecurityContextRepository for persisting SecurityContext to
     *        the session after successful authentication
     */
    public LoginAndPasswordAuthenticationFilter(
            @Value("${page.after.auth.for.multiple.organizations:/html/organization/all}")
                    String pageAfterAuthForMultipleOrganizations,
            @Value("${page.after.auth.for.one.organization:/html/organization/%s/settings}")
                    String pageAfterAuthForOneOrganization,
            @Value("${page.after.auth.for.global.admin:/html/dashboard}")
                    String pageAfterAuthForGlobalAdmin,
            @Autowired SecurityContextRepository securityContextRepository
    ) {
        setAuthenticationSuccessHandler(
                new CustomAuthenticationSuccessHandler(
                        pageAfterAuthForMultipleOrganizations,
                        pageAfterAuthForOneOrganization,
                        pageAfterAuthForGlobalAdmin,
                        securityContextRepository));
        setAuthenticationFailureHandler(new CustomAuthenticationFailureHandler());
    }

    /**
     * Overrides UsernamePasswordAuthenticationFilter to map legacy login identifiers to canonical
     * email usernames.
     * <p>
     * This method extracts the "username" parameter from the form POST (which is actually a login
     * identifier, not necessarily an email), then queries the database to find the corresponding
     * User.email address. If a match is found, it returns the canonical email; otherwise, it
     * returns the original login identifier unchanged.
     * </p>
     * <p>
     * <b>Lookup Algorithm:</b>
     * </p>
     * <ol>
     *   <li>super.obtainUsername(request) extracts "username" parameter from form POST</li>
     *   <li>userRepository.findUsernameLowercaseByLogin(login) queries database:
     *       SELECT LOWER(u.email) FROM User u JOIN LoginAndPassword lap ON u.id = lap.userId WHERE lap.login = :login</li>
     *   <li>If match found, returns User.email (canonical username for Spring Security)</li>
     *   <li>If no match (null), returns original login identifier (user might be authenticating with email directly)</li>
     * </ol>
     * <p>
     * <b>Email Normalization:</b> This method always returns a lowercase email address per User.email
     * storage conventions and Spring Security username requirements.
     * </p>
     * <p>
     * <b>Fallback Behavior:</b> If the login is not found in the LoginAndPassword table, authentication
     * proceeds with the login as the username. This handles users who authenticate directly with their
     * email address.
     * </p>
     *
     * @param request HttpServletRequest containing form POST data with "username" parameter (actually
     *        a login identifier, not necessarily an email address)
     * @return canonical lowercase email address from User.email if login found in LoginAndPassword
     *         table, otherwise returns the original login identifier unchanged
     * @see UserRepository#findUsernameLowercaseByLogin(String)
     */
    @Override
    protected String obtainUsername(HttpServletRequest request) {
        String login = super.obtainUsername(request);
        String username = userRepository.findUsernameLowercaseByLogin(login);
        return username != null ? username : login;
    }

    /**
     * Sets the AuthenticationManager for this filter via setter injection with lazy initialization.
     * <p>
     * This method configures the authentication flow by connecting the filter to the authentication
     * provider chain. The {@link Lazy @Lazy} annotation breaks a circular dependency during Spring
     * Security configuration initialization.
     * </p>
     * <p>
     * <b>Circular Dependency Resolution:</b>
     * </p>
     * <ul>
     *   <li>SecurityConfiguration creates LoginAndPasswordAuthenticationFilter bean</li>
     *   <li>SecurityConfiguration creates AuthenticationManager bean referencing the filter</li>
     *   <li>@Lazy annotation defers AuthenticationManager injection until after both beans are initialized</li>
     * </ul>
     * <p>
     * <b>Authentication Flow After Manager Set:</b>
     * </p>
     * <ol>
     *   <li>Filter calls attemptAuthentication() with UsernamePasswordAuthenticationToken</li>
     *   <li>AuthenticationManager delegates to LoginByPasswordOrTokenAuthenticationProvider</li>
     *   <li>Provider validates credentials via UserDetailsService and PasswordEncoder</li>
     *   <li>On success, SecurityContext is updated with authenticated OrganizationUser</li>
     *   <li>CustomAuthenticationSuccessHandler performs role-based redirect</li>
     * </ol>
     *
     * @param authenticationManager AuthenticationManager containing ProviderManager with
     *        LoginByPasswordOrTokenAuthenticationProvider for validating username/password credentials
     */
    @Autowired
    @Override
    public void setAuthenticationManager(@Lazy AuthenticationManager authenticationManager) {
        super.setAuthenticationManager(authenticationManager);
    }
}
