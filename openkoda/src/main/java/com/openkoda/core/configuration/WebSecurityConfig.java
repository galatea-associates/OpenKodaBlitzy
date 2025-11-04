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

import com.openkoda.controller.common.URLConstants;
import com.openkoda.core.security.*;
import jakarta.annotation.Resource;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.CompositeFilter;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;


/**
 * Spring Security configuration class defining authentication, authorization, and security filter chain.
 * <p>
 * Annotated with {@code @Configuration} to register security beans. This class configures:
 * <ul>
 * <li>Authentication providers (DaoAuthenticationProvider with UserDetailsService)</li>
 * <li>Authentication manager for credential validation</li>
 * <li>Multiple security filter chains with path-based access rules:
 *   <ul>
 *   <li>Web filter chain for HTML endpoints with form login</li>
 *   <li>API authentication filter chains (stateless)</li>
 *   <li>Token-based authentication filter chains</li>
 *   <li>Public web filter chain with CSRF protection</li>
 *   </ul>
 * </li>
 * <li>Form login with custom success/failure handlers</li>
 * <li>Logout configuration with session cleanup</li>
 * <li>Remember-me authentication using token-based algorithm</li>
 * <li>CSRF protection with configurable disabled pages</li>
 * <li>Security context repository for HTTP session</li>
 * <li>Session management (stateful for web, stateless for APIs)</li>
 * </ul>
 * <p>
 * Integrates custom authentication handlers:
 * <ul>
 * <li>{@link CustomAuthenticationSuccessHandler} - Multi-tenant aware post-authentication routing</li>
 * <li>{@link CustomAuthenticationFailureHandler} - Failed login handling</li>
 * <li>{@link RequestParameterTokenAuthenticationSuccessHandler} - Token-based authentication success</li>
 * </ul>
 * <p>
 * Security model supports multiple authentication mechanisms:
 * <ul>
 * <li>Form-based username/password authentication</li>
 * <li>Request parameter token authentication</li>
 * <li>API token header authentication</li>
 * <li>Token path prefix authentication</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.springframework.security.web.SecurityFilterChain
 * @see org.springframework.security.authentication.AuthenticationManager
 * @see org.springframework.security.authentication.dao.DaoAuthenticationProvider
 * @see CustomAuthenticationSuccessHandler
 * @see CustomAuthenticationFailureHandler
 * @see OrganizationUserDetailsService
 */
@Configuration
@Order(SecurityProperties.BASIC_AUTH_ORDER)
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
@ComponentScan(basePackages = "com.openkoda")
@EnableWebSecurity
public class WebSecurityConfig implements URLConstants {

    public static final String CHANGE_PASSWORD_PRIVILEGE = "CHANGE_PASSWORD_PRIVILEGE";

    @Value("${local.network:127.0.0.1/32}")
    private String localNetwork;

    @Value("${page.after.logout:/home}")
    private String pageAfterLogin;

    @Value("${rememberme.key:uniqueRememberKey}")
    private String rememberMeKey;

    @Value("${rememberme.parameter:remember-me}")
    private String rememberMeParameter;

    @Value("${rememberme.parameter:remember-me}")
    private String rememberMeCookieName;

    @Value("${application.pages.public:}")
    public String[] publicPages;

    @Value("${application.pages.csrf-disabled}")
    public String[] csrfDisabledPages;

    @Value("${page.after.auth:/html/organization/all}")
    private String pageAfterAuth;


    @Resource(name = "customUserDetailsService")
    protected OrganizationUserDetailsService customUserDetailsService;

    @Resource(name = "requestParameterTokenAuthenticationFilter")
    protected RequestParameterTokenAuthenticationFilter requestParameterTokenAuthenticationFilter;

    @Resource(name = "loginAndPasswordAuthenticationFilter")
    protected LoginAndPasswordAuthenticationFilter loginAndPasswordAuthenticationFilter;

    @Resource(name = "apiTokenHeaderAuthenticationFilter")
    protected ApiTokenHeaderAuthenticationFilter apiTokenHeaderAuthenticationFilter;

    @Resource(name = "tokenPathPrefixAuthenticationFilter")
    protected TokenPathPrefixAuthenticationFilter tokenPathPrefixAuthenticationFilter;

    @Resource(name = "loginByPasswordOrTokenAuthenticationProvider")
    protected LoginByPasswordOrTokenAuthenticationProvider loginByPasswordOrTokenAuthenticationProvider;

    private ApplicationAwarePasswordEncoder passwordEncoder;

    public WebSecurityConfig(@Autowired ApplicationAwarePasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }


    /*
     * Global rules:
     * - UserDetailsService is responsible for reading user (with privileges) by username
     *   and setting the password from LoginAndPassword if exists.
     *   Username is email from User entity.
     * - AuthenticationProvider is responsible for checking if the request with authentication data is correct, ie.
     *   - for form authentication (default, username and password) it checks password from LoginAndPassword
     *   - for requestToken it checks if request parameter "token" exists in Token entity for given user
     *   - for apiToken it checks it "api-token" Header exists in Token entity for given user
     * See LoginByPasswordOrTokenAuthenticationProvider.additionalAuthenticationChecks()
     */

    /**
     * Creates the authentication manager with configured authentication providers.
     * <p>
     * This bean is the entry point for all authentication attempts in the application.
     * It delegates to two configured providers:
     * <ol>
     * <li>{@link DaoAuthenticationProvider} - Validates username/password credentials using
     * {@link OrganizationUserDetailsService} for user lookup and {@link ApplicationAwarePasswordEncoder}
     * for password verification</li>
     * <li>{@link LoginByPasswordOrTokenAuthenticationProvider} - Validates token-based authentication
     * including request parameter tokens and API token headers</li>
     * </ol>
     * 
     * <p>
     * The ProviderManager iterates through providers until one successfully authenticates
     * or all providers have been attempted.
     * 
     *
     * @return configured {@link ProviderManager} with DaoAuthenticationProvider and token authentication provider
     * @see DaoAuthenticationProvider
     * @see ProviderManager
     * @see LoginByPasswordOrTokenAuthenticationProvider
     * @see OrganizationUserDetailsService
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        List<AuthenticationProvider> authenticationProviders = new ArrayList<>();
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        daoAuthenticationProvider.setUserDetailsService(customUserDetailsService);
        authenticationProviders.add(daoAuthenticationProvider);
        authenticationProviders.add(loginByPasswordOrTokenAuthenticationProvider);
        return new ProviderManager(authenticationProviders);
    }


    @Value("${page.after.auth.for.one.organization:/html/organization/%s/settings}")
    private String pageAfterAuthForOneOrganization;

    @Value("${page.after.auth.for.multiple.organizations:/html/organization/all}")
    private String pageAfterAuthForMultipleOrganizations;

    /**
     * Configures the primary web security filter chain for HTML endpoints.
     * <p>
     * This filter chain handles authentication and authorization for web UI endpoints
     * matching the pattern {@code /html/**}. Configuration includes:
     * <ul>
     * <li>SSO filter before username/password authentication</li>
     * <li>Login and password authentication filter after standard authentication</li>
     * <li>Frame options set to same origin for X-Frame-Options header</li>
     * <li>Form-based login with custom processing URL</li>
     * <li>Remember-me functionality with token-based services</li>
     * </ul>
     * 
     * <p>
     * Authorization rules delegate to {@link #webHttpSecurity(HttpSecurity)}.
     * 
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain} for web endpoints
     * @throws Exception if configuration fails
     * @see #webHttpSecurity(HttpSecurity)
     */
    @Bean
    @Order(10)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        return webHttpSecurity(http).build();
    }
    /**
     * Configures the API authentication security filter chain.
     * <p>
     * This filter chain handles stateless API authentication endpoints. Configuration includes:
     * <ul>
     * <li>Anonymous access disabled</li>
     * <li>CSRF protection disabled (stateless API)</li>
     * <li>Request cache disabled</li>
     * <li>Stateless session management (no HTTP session created)</li>
     * </ul>
     * 
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain} for API authentication endpoints
     * @throws Exception if configuration fails
     * @see #apiAuthHttpSecurity(HttpSecurity)
     */
    @Bean
    @Order(20)
    public SecurityFilterChain apiAuthSecurityFilterChain(HttpSecurity http) throws Exception {
        return apiAuthHttpSecurity(http).build();
    }

    /**
     * Configures the API token security filter chain for token path prefix authentication.
     * <p>
     * This filter chain handles requests with token embedded in URL path. Configuration includes:
     * <ul>
     * <li>Token path prefix authentication filter before standard authentication</li>
     * <li>Anonymous access disabled</li>
     * <li>CSRF protection disabled (stateless API)</li>
     * <li>Request cache disabled</li>
     * <li>All requests require authentication</li>
     * <li>Stateless session management</li>
     * </ul>
     * 
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain} for token path prefix endpoints
     * @throws Exception if configuration fails
     * @see TokenPathPrefixAuthenticationFilter
     * @see #apiTokenHttpSecurity(HttpSecurity)
     */
    @Bean
    @Order(21)
    public SecurityFilterChain apiTokenSecurityFilterChain(HttpSecurity http) throws Exception {
        return apiTokenHttpSecurity(http).build();
    }

    /**
     * Configures the API V1 security filter chain for header-based token authentication.
     * <p>
     * This filter chain handles API V1 endpoints with token in request header. Configuration includes:
     * <ul>
     * <li>API token header authentication filter for extracting token from headers</li>
     * <li>Anonymous access disabled</li>
     * <li>CSRF protection disabled (stateless API)</li>
     * <li>Request cache disabled</li>
     * <li>All requests require authentication</li>
     * <li>Stateless session management</li>
     * <li>Custom authentication entry point returning HTTP 401 Unauthorized</li>
     * </ul>
     * 
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain} for API V1 endpoints
     * @throws Exception if configuration fails
     * @see ApiTokenHeaderAuthenticationFilter
     * @see #apiV1HttpSecurity(HttpSecurity)
     */
    @Bean
    @Order(22)
    public SecurityFilterChain apiV1SecurityFilterChain(HttpSecurity http) throws Exception {
        return apiV1HttpSecurity(http).build();
    }

    /**
     * Configures the API V2 security filter chain for header-based token authentication.
     * <p>
     * This filter chain handles API V2 endpoints with token in request header. Configuration includes:
     * <ul>
     * <li>API token header authentication filter for extracting token from headers</li>
     * <li>Anonymous access disabled</li>
     * <li>CSRF protection disabled (stateless API)</li>
     * <li>Request cache disabled</li>
     * <li>All requests require authentication</li>
     * <li>Stateless session management</li>
     * <li>Custom authentication entry point returning HTTP 401 Unauthorized</li>
     * </ul>
     * 
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain} for API V2 endpoints
     * @throws Exception if configuration fails
     * @see ApiTokenHeaderAuthenticationFilter
     * @see #apiV2HttpSecurity(HttpSecurity)
     */
    @Bean
    @Order(23)
    public SecurityFilterChain apiV2SecurityFilterChain(HttpSecurity http) throws Exception {
        return apiV2HttpSecurity(http).build();
    }

    /**
     * Configures the public web security filter chain as fallback for all remaining endpoints.
     * <p>
     * This filter chain handles all requests not matched by higher-order filter chains.
     * Configuration includes:
     * <ul>
     * <li>SSO filter and login/password authentication filters</li>
     * <li>Frame options set to same origin</li>
     * <li>CSRF protection enabled with configurable disabled pages</li>
     * <li>Public pages accessible without authentication</li>
     * <li>Frontend resources with organization ID parameter permit all</li>
     * <li>Frontend resources with auth parameters require authentication</li>
     * <li>Password management endpoints require authentication</li>
     * <li>Logout configuration with session cleanup and user unsubscription</li>
     * </ul>
     * 
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain} for public web endpoints
     * @throws Exception if configuration fails
     * @see #publicWebHttpSecurity(HttpSecurity)
     */
    @Bean
    @Order(50)
    public SecurityFilterChain publicWebSecurityFilterChain(HttpSecurity http) throws Exception {
        return publicWebHttpSecurity(http).build();
    }

    /**
     * Configures HTTP security for web endpoints matching {@code /html/**}.
     * <p>
     * Authorization rules:
     * <ul>
     * <li>Public pages (configured via {@code application.pages.public}) - permit all</li>
     * <li>{@code /html/admin/**} - requires ROLE_ADMIN</li>
     * <li>{@code /html/local/**} - restricted to local network IP addresses</li>
     * <li>{@code /html/**} - requires authentication</li>
     * </ul>
     * 
     * <p>
     * Form login configuration:
     * <ul>
     * <li>Login page: configured via URLConstants._LOGIN</li>
     * <li>Processing URL: /perform_login</li>
     * </ul>
     * 
     * <p>
     * Remember-me configuration uses token-based services with configurable key,
     * parameter name, and cookie name from application properties.
     * 
     *
     * @param http the {@link HttpSecurity} to configure
     * @return configured {@link HttpSecurity} for web endpoints
     * @throws Exception if configuration fails
     */
    public HttpSecurity webHttpSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher(_HTML + "/**")
                .addFilterBefore(ssoFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(loginAndPasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers().frameOptions().sameOrigin().and()
                .authorizeHttpRequests(auth -> auth
                        // Public pages - no authentication required
                        .requestMatchers(publicPages).permitAll()
                        // Admin pages - requires ROLE_ADMIN
                        .requestMatchers(_HTML + _ADMIN + _ANY).hasRole("_ADMIN")
                        // Local network pages - IP address restriction
                        .requestMatchers(_HTML + _LOCAL + "/**").access(new WebExpressionAuthorizationManager(
                                    "hasIpAddress('" + localNetwork + "')"
                            ))
                        // All other /html/** pages require authentication
                        .requestMatchers(_HTML + "/**").authenticated()
                )
                .formLogin(form -> form
                        .loginPage(_LOGIN)
                        .loginProcessingUrl("/perform_login")
                )
                .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices(customUserDetailsService))
                        .rememberMeCookieName(rememberMeCookieName)
                        .rememberMeParameter(rememberMeParameter)
                        .key(rememberMeKey)
                );
    }
    /**
     * Configures HTTP security for API authentication endpoints.
     * <p>
     * Disables stateful features for API endpoints:
     * <ul>
     * <li>Anonymous authentication disabled</li>
     * <li>CSRF protection disabled (not needed for stateless APIs)</li>
     * <li>Request cache disabled</li>
     * <li>Stateless session creation policy (no HTTP session)</li>
     * </ul>
     * 
     *
     * @param http the {@link HttpSecurity} to configure
     * @return configured {@link HttpSecurity} for API auth endpoints
     * @throws Exception if configuration fails
     */
    public HttpSecurity apiAuthHttpSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher(_API_AUTH_ANT_EXPRESSION)
                .anonymous(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
    /**
     * Configures HTTP security for token path prefix authentication endpoints.
     * <p>
     * Adds {@link TokenPathPrefixAuthenticationFilter} to extract and validate token from URL path.
     * All requests require authentication. Configuration:
     * <ul>
     * <li>Token path prefix filter before standard authentication</li>
     * <li>Anonymous authentication disabled</li>
     * <li>CSRF protection disabled</li>
     * <li>Request cache disabled</li>
     * <li>All requests authenticated</li>
     * <li>Stateless session management</li>
     * </ul>
     * 
     *
     * @param http the {@link HttpSecurity} to configure
     * @return configured {@link HttpSecurity} for token path endpoints
     * @throws Exception if configuration fails
     * @see TokenPathPrefixAuthenticationFilter
     */
    public HttpSecurity apiTokenHttpSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher(_TOKEN_PREFIX_ANT_EXPRESSION)
                .addFilterBefore(tokenPathPrefixAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .anonymous(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
    /**
     * Configures HTTP security for API V1 endpoints with header-based token authentication.
     * <p>
     * Adds {@link ApiTokenHeaderAuthenticationFilter} to extract and validate API token from request headers.
     * All requests require authentication. Configuration:
     * <ul>
     * <li>API token header filter before standard authentication</li>
     * <li>Anonymous authentication disabled</li>
     * <li>CSRF protection disabled</li>
     * <li>Request cache disabled</li>
     * <li>All requests authenticated</li>
     * <li>Stateless session management</li>
     * <li>Custom authentication entry point returning 401 Unauthorized</li>
     * </ul>
     * 
     *
     * @param http the {@link HttpSecurity} to configure
     * @return configured {@link HttpSecurity} for API V1 endpoints
     * @throws Exception if configuration fails
     * @see ApiTokenHeaderAuthenticationFilter
     */
    public HttpSecurity apiV1HttpSecurity(HttpSecurity http) throws Exception {
       return http.securityMatcher(_API_V1_ANT_EXPRESSION)
                .addFilterBefore(apiTokenHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .anonymous(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)));
    }
    /**
     * Configures HTTP security for API V2 endpoints with header-based token authentication.
     * <p>
     * Adds {@link ApiTokenHeaderAuthenticationFilter} to extract and validate API token from request headers.
     * All requests require authentication. Configuration:
     * <ul>
     * <li>API token header filter before standard authentication</li>
     * <li>Anonymous authentication disabled</li>
     * <li>CSRF protection disabled</li>
     * <li>Request cache disabled</li>
     * <li>All requests authenticated</li>
     * <li>Stateless session management</li>
     * <li>Custom authentication entry point returning 401 Unauthorized</li>
     * </ul>
     * 
     *
     * @param http the {@link HttpSecurity} to configure
     * @return configured {@link HttpSecurity} for API V2 endpoints
     * @throws Exception if configuration fails
     * @see ApiTokenHeaderAuthenticationFilter
     */
    public HttpSecurity apiV2HttpSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher(_API_V2_ANT_EXPRESSION)
                .addFilterBefore(apiTokenHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .anonymous(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)));
    }
    /**
     * Configures HTTP security for public web endpoints as fallback for all remaining requests.
     * <p>
     * Authorization rules:
     * <ul>
     * <li>Public pages (configured via {@code application.pages.public}) - permit all</li>
     * <li>Frontend resources with organization ID parameter - permit all</li>
     * <li>Frontend resources with authentication parameters - require authentication</li>
     * <li>Password management endpoints ({@code /password/**}) - require authentication</li>
     * </ul>
     * 
     * <p>
     * CSRF protection enabled with configurable disabled pages via {@code application.pages.csrf-disabled}.
     * 
     * <p>
     * Logout configuration:
     * <ul>
     * <li>Logout URL: configured via URLConstants._LOGOUT</li>
     * <li>Deletes JSESSIONID cookie</li>
     * <li>Unsubscribes user from custom user details service</li>
     * <li>Redirects to home page after logout</li>
     * </ul>
     * 
     *
     * @param http the {@link HttpSecurity} to configure
     * @return configured {@link HttpSecurity} for public web endpoints
     * @throws Exception if configuration fails
     */
    public HttpSecurity publicWebHttpSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher("/**")
                .addFilterBefore(ssoFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(loginAndPasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers().frameOptions().sameOrigin().and()
                .csrf(csrf -> csrf
                        // Disable CSRF for configured pages
                        .ignoringRequestMatchers(csrfDisabledPages)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public pages - no authentication required
                        .requestMatchers(publicPages).permitAll()
                        // Frontend resources with organization ID - public access
                        .requestMatchers(regexMatcher("/" + FRONTENDRESOURCEREGEX + FRONTENDRESOURCE_ORGID_PARAM_REGEX)).permitAll()
                        // Frontend resources with auth parameters - require authentication
                        .requestMatchers(regexMatcher("/" + FRONTENDRESOURCEREGEX + FRONTENDRESOURCE_AUTH_PARAMS_REGEX)).authenticated()
                        // Password management - require authentication
                        .requestMatchers(_PASSWORD + "/**").authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl(_LOGOUT)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler( (request, response, authentication) -> {
                            String email = authentication.getName();
                            customUserDetailsService.unsubscribeUser(email);
                            response.sendRedirect("/");
                            })
                );
    }
    /**
     * Creates a composite SSO filter for single sign-on authentication.
     * <p>
     * Configures {@link RequestParameterTokenAuthenticationFilter} with custom success handler
     * that uses the security context repository for session management.
     * 
     *
     * @return composite {@link Filter} containing SSO authentication filters
     * @see RequestParameterTokenAuthenticationFilter
     * @see RequestParameterTokenAuthenticationSuccessHandler
     */
    private Filter ssoFilter() {
        CompositeFilter filter = new CompositeFilter();
        List<Filter> filters = new ArrayList<>();
        requestParameterTokenAuthenticationFilter.setAuthenticationSuccessHandler(new RequestParameterTokenAuthenticationSuccessHandler(securityContextRepository()));
        filters.add(requestParameterTokenAuthenticationFilter);
        filter.setFilters(filters);
        return filter;
    }

    /**
     * Creates a Spring Security evaluation context extension for SpEL expressions.
     * <p>
     * Enables use of Spring Security expressions in Spring Data JPA queries and other
     * SpEL contexts (e.g., {@code ?#{ principal.username }}).
     * 
     *
     * @return {@link SecurityEvaluationContextExtension} for SpEL integration
     */
    @Bean
    public SecurityEvaluationContextExtension securityEvaluationContextExtension() {
        return new SecurityEvaluationContextExtension();
    }

    /**
     * Creates a servlet context initializer to configure session cookie security.
     * <p>
     * Configures the session cookie secure flag based on application property
     * {@code secure.cookie} (defaults to true). When secure flag is enabled,
     * cookies are only transmitted over HTTPS connections.
     * 
     *
     * @param secure whether to enable secure flag on session cookies (from {@code secure.cookie} property)
     * @return {@link ServletContextInitializer} that configures session cookie security
     */
    @Bean
    public ServletContextInitializer servletContextInitializer(@Value("${secure.cookie:true}") boolean secure) {
        return new ServletContextInitializer() {

            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
                servletContext.getSessionCookieConfig().setSecure(secure);
            }
        };
    }

    /**
     * Creates the security context repository for persisting authentication between requests.
     * <p>
     * Uses {@link HttpSessionSecurityContextRepository} to store {@link org.springframework.security.core.context.SecurityContext}
     * in the HTTP session, enabling stateful session management for web endpoints.
     * 
     *
     * @return {@link HttpSessionSecurityContextRepository} for HTTP session-based security context storage
     */
    @Bean()
    public SecurityContextRepository securityContextRepository(){
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Creates remember-me authentication services using token-based algorithm.
     * <p>
     * Configuration:
     * <ul>
     * <li>Remember-me key: from {@code rememberme.key} property (default: "uniqueRememberKey")</li>
     * <li>Encoding algorithm: SHA256 for token generation</li>
     * <li>Matching algorithm: MD5 for token validation (for backward compatibility)</li>
     * <li>User details service: injected {@link UserDetailsService} for user lookup</li>
     * </ul>
     * 
     * <p>
     * Token is stored in cookie with name configured via {@code rememberme.parameter} property.
     * 
     *
     * @param userDetailsService the {@link UserDetailsService} for loading user details during remember-me authentication
     * @return configured {@link TokenBasedRememberMeServices} for remember-me functionality
     * @see TokenBasedRememberMeServices
     */
    @Bean
    RememberMeServices rememberMeServices(UserDetailsService userDetailsService) {
        TokenBasedRememberMeServices.RememberMeTokenAlgorithm encodingAlgorithm = TokenBasedRememberMeServices.RememberMeTokenAlgorithm.SHA256;
        TokenBasedRememberMeServices rememberMe = new TokenBasedRememberMeServices(rememberMeKey, userDetailsService, encodingAlgorithm);
        rememberMe.setMatchingAlgorithm(TokenBasedRememberMeServices.RememberMeTokenAlgorithm.MD5);
        return rememberMe;
    }


}
