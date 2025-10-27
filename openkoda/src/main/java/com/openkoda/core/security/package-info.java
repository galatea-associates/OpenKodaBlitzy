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

/**
 * OpenKoda's core security infrastructure implementing authentication, authorization, and privilege enforcement.
 * <p>
 * This package provides the foundation for securing the OpenKoda platform through multiple layers of protection:
 * authentication mechanisms, privilege-based authorization, custom Spring Security principals, user management
 * integration, and security rule definitions. Built on Spring Framework, Spring Security, Jakarta Servlet APIs,
 * and OpenKoda's domain model (User, Token, ApiKey, Role, Privilege), this package enables enterprise-grade
 * security for multi-tenant applications with fine-grained access control.
 * </p>
 *
 * <h2>Architecture</h2>
 * <p>
 * The security architecture integrates with Spring Security's filter chain, authentication providers, and
 * authorization infrastructure. Key architectural components include:
 * </p>
 * <ul>
 *   <li><b>Authentication Layer:</b> Multiple authentication mechanisms (token-based, form-based, impersonation)
 *       implemented as Spring Security filters and authentication providers</li>
 *   <li><b>Authorization Layer:</b> Privilege-based access control with global and organization-scoped privileges,
 *       integrated with {@literal @}PreAuthorize method-level security and repository filtering</li>
 *   <li><b>Security Context:</b> Custom principal (OrganizationUser) extending Spring Security UserDetails with
 *       privilege sets and organization memberships</li>
 *   <li><b>User Management:</b> UserDetailsService integration loading privileges from the database and managing
 *       privilege reload for active users</li>
 *   <li><b>Security Rules:</b> HasSecurityRules interface providing {@literal @}PreAuthorize expressions, JPQL
 *       predicates, and programmatic checks for consistent security enforcement</li>
 * </ul>
 *
 * <h2>Authentication</h2>
 * <p>
 * This package implements multiple authentication mechanisms to support different client types and use cases:
 * </p>
 *
 * <h3>Token-Based Authentication</h3>
 * <p>
 * Three filter implementations provide token-based authentication for API clients and embedded resources:
 * </p>
 * <ul>
 *   <li><b>ApiTokenHeaderAuthenticationFilter:</b> Authenticates requests with API_TOKEN header for REST API clients.
 *       Validates token against Token entity in database and establishes SecurityContext.</li>
 *   <li><b>RequestParameterTokenAuthenticationFilter:</b> Authenticates requests with TOKEN query parameter for
 *       single-use links. Supports time-limited tokens and automatic expiration.</li>
 *   <li><b>TokenPathPrefixAuthenticationFilter:</b> Authenticates requests with /__t_/{token}/path URL prefix for
 *       embedding authenticated resources in external applications. Extracts token from path and forwards to
 *       target resource.</li>
 * </ul>
 *
 * <h3>Form-Based Authentication</h3>
 * <p>
 * LoginAndPasswordAuthenticationFilter extends Spring Security's UsernamePasswordAuthenticationFilter to provide:
 * </p>
 * <ul>
 *   <li>Traditional username/password login form authentication</li>
 *   <li>Legacy login identifier mapping to canonical email usernames</li>
 *   <li>Role-based post-login redirects configured per user type</li>
 *   <li>Integration with CustomAuthenticationSuccessHandler and CustomAuthenticationFailureHandler</li>
 * </ul>
 *
 * <h3>Impersonation</h3>
 * <p>
 * RunAsService allows privileged users to impersonate other users for support and testing scenarios:
 * </p>
 * <ul>
 *   <li><b>startRunAsUser(userId):</b> Creates spoofed OrganizationUser with restricted privileges and replaces
 *       current SecurityContext. Original context preserved for restoration.</li>
 *   <li><b>exitRunAsUser():</b> Restores original SecurityContext and terminates impersonation session.</li>
 *   <li>Impersonation flag tracked in OrganizationUser for audit logging and UI indicators.</li>
 * </ul>
 *
 * <h3>Authentication Providers</h3>
 * <ul>
 *   <li><b>LoginByPasswordOrTokenAuthenticationProvider:</b> Validates passwords via BCrypt and processes token
 *       authentication. Extends DaoAuthenticationProvider with dual authentication mode support.</li>
 *   <li><b>SimpleAuthenticationProvider:</b> Pass-through provider with minimal validation. Used for specific
 *       authentication scenarios with pre-validated credentials.</li>
 * </ul>
 *
 * <h3>Authentication Tokens</h3>
 * <ul>
 *   <li><b>RequestTokenAuthenticationToken:</b> Carries raw token string, userId, privilege set, and single-use flag
 *       through authentication pipeline.</li>
 *   <li><b>PreauthenticatedReloadUserToken:</b> Marker token triggering privilege reload for authenticated users
 *       when roles or privileges change.</li>
 * </ul>
 *
 * <h3>Filter Base Class</h3>
 * <p>
 * AbstractTokenAuthenticationFilter provides centralized token authentication lifecycle:
 * </p>
 * <ul>
 *   <li>Token extraction from various sources (header, parameter, path)</li>
 *   <li>TokenRepository lookup and validation</li>
 *   <li>Request correlation logging with LoggingComponentWithRequestId</li>
 *   <li>Extension hooks for custom token processing</li>
 * </ul>
 *
 * <h2>Authorization</h2>
 * <p>
 * The authorization model enforces privilege-based access control at multiple layers using OpenKoda's
 * flexible privilege system with global and organization-scoped privileges.
 * </p>
 *
 * <h3>Privilege-Based Authorization Model</h3>
 * <ul>
 *   <li><b>OrganizationUser principal:</b> Custom UserDetails implementation storing global privileges
 *       (Set&lt;PrivilegeBase&gt;) and per-organization privileges (Map&lt;Long, Set&lt;PrivilegeBase&gt;&gt;).
 *       Implements HasSecurityRules interface for security rule evaluation.</li>
 *   <li><b>{@literal @}PreAuthorize integration:</b> Method-level security annotations using HasSecurityRules
 *       constants (CHECK_CAN_READ_ORG_DATA, CHECK_CAN_MANAGE_BASIC_DATA) evaluated by Spring Security AOP.</li>
 *   <li><b>JPQL security predicates:</b> Repository {@literal @}Query methods include HasSecurityRules JPQL
 *       predicates (JPQL_IS_ORG_RELATED) for entity filtering at query time.</li>
 *   <li><b>Programmatic checks:</b> BiFunction checks for dynamic authorization decisions such as form field
 *       visibility and UI element rendering.</li>
 * </ul>
 *
 * <h3>Privilege Enforcement</h3>
 * <ul>
 *   <li><b>HasSecurityRules interface:</b> Central security rule surface defining CHECK_* constants for
 *       {@literal @}PreAuthorize expressions, JPQL predicates for repository queries, and programmatic BiFunction
 *       checks for application logic.</li>
 *   <li><b>SecurityEvaluationContextExtension:</b> Spring Security SpEL evaluation context extension exposing
 *       user(), hasGlobalPrivilege(), and hasOrgPrivilege() functions for use in security expressions.</li>
 *   <li><b>SecureRepository integration:</b> Repository layer applies JPA Criteria API predicates via
 *       HasSecurityRules.securityScope() methods to filter entities by privilege and organization membership.</li>
 *   <li><b>Method-level guards:</b> Service methods annotated with {@literal @}PreAuthorize using HasSecurityRules
 *       constants to prevent unauthorized method invocation.</li>
 * </ul>
 *
 * <h3>Authorization Scopes</h3>
 * <ul>
 *   <li><b>GLOBAL:</b> User has privilege globally across all organizations. Used for platform administration.</li>
 *   <li><b>ORGANIZATION:</b> User has privilege within specific organization context. Most common scope for
 *       multi-tenant operations.</li>
 *   <li><b>USER:</b> Entity belongs to current user (created_by field matches authenticated user ID).</li>
 *   <li><b>USER_IN_ORGANIZATION:</b> Entity belongs to user within organization scope. Combines ownership and
 *       organization membership checks.</li>
 *   <li><b>ALL:</b> No privilege restrictions. Used for public access endpoints and unauthenticated resources.</li>
 * </ul>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li><b>OrganizationUser:</b> Custom Spring Security principal implementing UserDetails and HasSecurityRules.
 *       Stores global and organization-scoped privileges, role membership, organization names, and impersonation
 *       flags. Immutable after construction for thread safety.</li>
 *
 *   <li><b>OrganizationUserDetailsService:</b> Spring Security UserDetailsService implementation. Loads User entity
 *       with joined roles and privileges, builds OrganizationUser, and registers in subscribedUsers map for
 *       privilege-change reload notifications.</li>
 *
 *   <li><b>UserProvider:</b> Centralized accessor for current OrganizationUser from SecurityContext. Provides
 *       per-request caching, modification detection, privilege reloading on role/privilege changes, and synthetic
 *       principal creation for unauthenticated contexts.</li>
 *
 *   <li><b>HasSecurityRules:</b> Interface defining security rule constants and methods for multi-layered security
 *       enforcement. Provides {@literal @}PreAuthorize expressions (CHECK_*), JPQL predicates (JPQL_*), and
 *       programmatic BiFunction checks (securityScope*) for consistent authorization across application layers.</li>
 *
 *   <li><b>ApplicationAwarePasswordEncoder:</b> BCryptPasswordEncoder bootstrap with DelegatingPasswordEncoder
 *       registration. Supports legacy password migration by delegating to multiple password encoding strategies
 *       based on password prefix.</li>
 *
 *   <li><b>LoginByPasswordOrTokenAuthenticationProvider:</b> DaoAuthenticationProvider extension supporting dual
 *       authentication modes. Validates passwords via UserDetailsService and PasswordEncoder, or processes token
 *       authentication via TokenRepository lookup.</li>
 *
 *   <li><b>AbstractTokenAuthenticationFilter:</b> Base filter for token authentication implementations. Centralizes
 *       token extraction lifecycle, TokenRepository lookup, request correlation logging, and extensible hooks for
 *       custom token processing in subclasses.</li>
 *
 *   <li><b>RunAsService:</b> Impersonation API allowing privileged users to assume identity of other users.
 *       Creates spoofed OrganizationUser with restricted privileges, preserves original SecurityContext for
 *       restoration, and tracks impersonation state for audit logging.</li>
 *
 *   <li><b>SecurityEvaluationContextExtension:</b> Spring Security SpEL context extension implementing
 *       EvaluationContextExtension. Exposes user(), hasGlobalPrivilege(), and hasOrgPrivilege() functions for
 *       use in {@literal @}PreAuthorize expressions and repository {@literal @}Query predicates.</li>
 * </ul>
 *
 * <h2>Security Architecture</h2>
 *
 * <h3>Spring Security Integration</h3>
 * <ul>
 *   <li>Filters registered in SecurityConfiguration filter chain with specific order and URL patterns</li>
 *   <li>AuthenticationManager delegates to provider chain (LoginByPasswordOrTokenAuthenticationProvider,
 *       SimpleAuthenticationProvider)</li>
 *   <li>SecurityContext holds authenticated OrganizationUser and is stored in ThreadLocal via SecurityContextHolder</li>
 *   <li>SecurityContextRepository persists SecurityContext to HTTP session for subsequent requests</li>
 * </ul>
 *
 * <h3>Authentication Flow</h3>
 * <ol>
 *   <li>Filter extracts credentials: token from header/parameter/path OR username/password from form</li>
 *   <li>Filter creates Authentication token: RequestTokenAuthenticationToken OR UsernamePasswordAuthenticationToken</li>
 *   <li>Filter invokes AuthenticationManager.authenticate() with created token</li>
 *   <li>AuthenticationManager delegates to LoginByPasswordOrTokenAuthenticationProvider in provider chain</li>
 *   <li>Provider validates credentials: token lookup in TokenRepository OR password via UserDetailsService and
 *       PasswordEncoder</li>
 *   <li>Provider loads OrganizationUser with privileges via OrganizationUserDetailsService</li>
 *   <li>Provider returns authenticated Authentication with OrganizationUser principal and granted authorities</li>
 *   <li>Filter stores Authentication in SecurityContext via SecurityContextHolder.setContext()</li>
 *   <li>SecurityContextRepository persists SecurityContext to HTTP session for subsequent requests</li>
 *   <li>Success/failure handler performs redirect or error rendering based on authentication result</li>
 * </ol>
 *
 * <h3>Authorization Flow</h3>
 * <ol>
 *   <li>{@literal @}PreAuthorize annotation on method specifies security expression
 *       (e.g., {@literal @}PreAuthorize(CHECK_CAN_READ_ORG_DATA))</li>
 *   <li>Spring Security AOP interceptor evaluates expression via SecurityEvaluationContextExtension</li>
 *   <li>Extension resolves user() function to current OrganizationUser from SecurityContext</li>
 *   <li>Extension evaluates hasGlobalPrivilege() or hasOrgPrivilege() against OrganizationUser privilege sets</li>
 *   <li>If expression returns true, method execution proceeds; if false, AccessDeniedException thrown</li>
 *   <li>Repository layer applies additional JPA Criteria predicates via HasSecurityRules.securityScope() methods
 *       for entity filtering based on privilege and organization membership</li>
 * </ol>
 *
 * <h2>Usage Patterns</h2>
 *
 * <h3>Common Security Patterns</h3>
 * <ul>
 *   <li><b>REST API authentication:</b> Client sends API_TOKEN header, ApiTokenHeaderAuthenticationFilter
 *       authenticates request, SecurityContext available for entire request lifecycle</li>
 *
 *   <li><b>Form login:</b> User submits username/password, LoginAndPasswordAuthenticationFilter maps login identifier
 *       to email, authenticates via provider chain, redirects based on user role</li>
 *
 *   <li><b>Embedded resource authentication:</b> URL contains /__t_/{token}/path prefix, TokenPathPrefixAuthenticationFilter
 *       extracts token, authenticates user, forwards to target path with established SecurityContext</li>
 *
 *   <li><b>Method-level security:</b> Service method annotated with {@literal @}PreAuthorize(CHECK_CAN_MANAGE_BASIC_DATA),
 *       SecurityEvaluationContextExtension evaluates privilege against current user, throws AccessDeniedException
 *       if unauthorized</li>
 *
 *   <li><b>Repository filtering:</b> SecureRepository queries apply JPA predicates via HasSecurityRules.securityScope()
 *       to filter entities by privilege and organization, ensuring users only access authorized data</li>
 *
 *   <li><b>User impersonation:</b> Administrator calls runAsService.startRunAsUser(targetUserId), SecurityContext
 *       replaced with spoofed OrganizationUser having restricted privileges, runAsService.exitRunAsUser() restores
 *       original context</li>
 *
 *   <li><b>Privilege reload:</b> User role or privilege modified in database, OrganizationUserDetailsService notifies
 *       subscribedUsers, UserProvider detects modification on next request and rebuilds OrganizationUser with
 *       current privileges</li>
 * </ul>
 *
 * <h3>Example Usage</h3>
 * <pre>
 * // Example: Service method with privilege check
 * {@literal @}PreAuthorize(HasSecurityRules.CHECK_CAN_MANAGE_BASIC_DATA)
 * public void updateOrganization(Long orgId, OrganizationForm form) {
 *     Organization org = repositories.secure.organization.findOne(orgId);
 *     form.populateTo(org);
 *     repositories.unsecure.organization.save(org);
 * }
 * 
 * // Example: Repository query with JPQL predicate
 * {@literal @}Query("SELECT o FROM Organization o WHERE " + HasSecurityRules.JPQL_IS_ORG_RELATED)
 * List&lt;Organization&gt; findAccessibleOrganizations();
 * 
 * // Example: Programmatic privilege check
 * if (userProvider.getFromContext().hasGlobalPrivilege(PrivilegeNames.canManageBackend)) {
 *     // Admin-only logic
 * }
 * </pre>
 *
 * <h2>Package Relationships</h2>
 *
 * <h3>Dependencies</h3>
 * <p>This package depends on:</p>
 * <ul>
 *   <li><b>com.openkoda.model:</b> User, Token, ApiKey, Role, Privilege, Organization entities for authentication
 *       and authorization data model</li>
 *   <li><b>com.openkoda.repository.user:</b> UserRepository, TokenRepository, ApiKeyRepository for credential
 *       lookup and validation</li>
 *   <li><b>com.openkoda.core.repository:</b> SecureRepository interface for privilege-enforced data access layer</li>
 *   <li><b>com.openkoda.core.configuration:</b> CustomAuthenticationSuccessHandler, CustomAuthenticationFailureHandler
 *       for login flow customization and redirect logic</li>
 *   <li><b>com.openkoda.controller.common:</b> URLConstants defining authentication path patterns and endpoint URLs</li>
 * </ul>
 *
 * <h3>Dependents</h3>
 * <p>Packages depending on this package:</p>
 * <ul>
 *   <li><b>com.openkoda.service:</b> Service layer uses {@literal @}PreAuthorize annotations with HasSecurityRules
 *       constants for method-level authorization</li>
 *   <li><b>com.openkoda.repository:</b> SecureRepository implementations use HasSecurityRules predicates for
 *       query-time entity filtering</li>
 *   <li><b>com.openkoda.controller:</b> Controllers access UserProvider for current user context and authorization
 *       decisions</li>
 *   <li><b>com.openkoda.core.flow:</b> Flow pipelines access SecurityContext for authorization and user context
 *       in request processing</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li><b>SecurityContext storage:</b> SecurityContext stored in ThreadLocal via SecurityContextHolder provides
 *       thread-safe per-request isolation. Each thread has independent SecurityContext.</li>
 *
 *   <li><b>OrganizationUser immutability:</b> OrganizationUser immutable after construction. Safe to share across
 *       threads and cache without synchronization concerns.</li>
 *
 *   <li><b>UserProvider caching:</b> Uses RequestSessionCacheService for per-request caching. Thread-safe as cache
 *       is request-scoped via ThreadLocal.</li>
 *
 *   <li><b>Token validation:</b> Token validation stateless with no shared mutable state. Thread-safe for
 *       concurrent authentication requests.</li>
 *
 *   <li><b>Impersonation:</b> RunAsService modifies ThreadLocal SecurityContext. Safe within single thread but
 *       context not shared across threads. Impersonation state per-thread only.</li>
 * </ul>
 *
 * <h2>Common Pitfalls</h2>
 * <ul>
 *   <li><b>Circular dependency during SecurityConfiguration:</b> Use {@literal @}Lazy annotation on AuthenticationManager
 *       injection in filter bean definitions to break circular dependency during Spring context initialization.</li>
 *
 *   <li><b>Missing {@literal @}PreAuthorize annotation:</b> Methods without {@literal @}PreAuthorize annotations
 *       bypass privilege checks entirely. Always annotate service methods with appropriate HasSecurityRules constants.</li>
 *
 *   <li><b>Incorrect SecurityScope:</b> Using ORGANIZATION scope without organizationId parameter causes authorization
 *       failures. Ensure organizationId present in security expressions requiring organization context.</li>
 *
 *   <li><b>Token not found in repository:</b> AbstractTokenAuthenticationFilter logs "token not found" and throws
 *       AuthenticationServiceException. Verify token exists in database and is not expired or revoked.</li>
 *
 *   <li><b>Privilege reload not triggered:</b> OrganizationUserDetailsService.subscribedUsers only tracks currently
 *       logged-in users. Users not actively authenticated require logout/login to receive privilege updates.</li>
 *
 *   <li><b>Impersonation stack overflow:</b> Nested startRunAsUser() calls without matching exitRunAsUser() cause
 *       context corruption. Always use try-finally pattern to ensure exitRunAsUser() execution.</li>
 *
 *   <li><b>SecurityContext propagation to async threads:</b> {@literal @}Async methods do not inherit SecurityContext
 *       by default. Use SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL) for automatic propagation
 *       or manually propagate SecurityContext to async threads.</li>
 * </ul>
 *
 * @see org.springframework.security.core.Authentication
 * @see org.springframework.security.core.context.SecurityContextHolder
 * @see org.springframework.security.access.prepost.PreAuthorize
 * @see com.openkoda.model.User
 * @see com.openkoda.model.Privilege
 * @see com.openkoda.core.repository.SecureRepository
 * @since 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.core.security;