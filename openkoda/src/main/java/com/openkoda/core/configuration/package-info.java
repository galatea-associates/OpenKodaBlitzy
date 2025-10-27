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
 * Package com.openkoda.core.configuration centralizes Spring configuration, security wiring, template resolution,
 * scheduling, mail integration, OpenAPI documentation, WebSocket messaging, and multi-datasource binding.
 * Contains @Configuration classes that bootstrap the OpenKoda platform including auditing (AuditConfig), scheduling
 * (JobConfig), JavaMail (MailSenderConfiguration), Spring MVC (MvcConfig), Spring Security (WebSecurityConfig),
 * Thymeleaf tenant-aware resolution (FrontendResourceOrClassLoaderTemplateResolver), OpenAPI generation (SwaggerConfig),
 * and WebSocket (WebSocketConfig). Provides authentication handlers (CustomAuthenticationSuccessHandler,
 * CustomAuthenticationFailureHandler, RequestParameterTokenAuthenticationSuccessHandler) for multi-tenant aware
 * post-login routing. Includes utility components (Datasources POJO for multi-tenant datasource binding,
 * TemplatePathFilteringProcessor for template path parsing).
 *
 * <h2>Key Classes and Interfaces</h2>
 * <ul>
 *   <li><b>AuditConfig</b>: JPA auditing enablement, AuditorAware registration, Hibernate interceptor injection
 *       for entity change tracking</li>
 *   <li><b>JobConfig</b>: Async executor and scheduled task scheduler with thread pool configuration (core 5, max 10,
 *       queue 250 for async; pool size 5 for scheduled)</li>
 *   <li><b>MailSenderConfiguration</b>: JavaMail integration with SMTP property binding from spring.mail.* properties</li>
 *   <li><b>MvcConfig</b>: Central WebMvcConfigurer with interceptors, resource handlers (vendor assets with 7-day cache),
 *       template resolvers, argument resolvers (MapFormArgumentResolver)</li>
 *   <li><b>WebSecurityConfig</b>: Spring Security filter chain, authentication providers (DaoAuthenticationProvider),
 *       authorization rules, custom success/failure handlers</li>
 *   <li><b>SwaggerConfig</b>: OpenAPI 3.0 documentation generation with grouped APIs (auth, v1) accessible at
 *       /swagger-ui.html</li>
 *   <li><b>WebSocketConfig</b>: STOMP messaging broker for real-time bidirectional communication with /topic prefix
 *       for pub/sub and /app prefix for application messages</li>
 *   <li><b>ReCaptchaConfiguration</b>: Google reCAPTCHA integration for bot prevention with site key, secret key,
 *       and validation level binding</li>
 *   <li><b>FrontendResourceOrClassLoaderTemplateResolver</b>: Tenant-aware Thymeleaf template loading from database
 *       or classpath fallback with auto-creation support</li>
 *   <li><b>CustomAuthenticationSuccessHandler</b>: Post-login redirection based on user privileges and organization
 *       membership (global admin, single-org, multi-org routing)</li>
 *   <li><b>CustomAuthenticationFailureHandler</b>: Error handling with redirect to login page with error parameters
 *       (verificationError for disabled accounts, error for other failures)</li>
 *   <li><b>RequestParameterTokenAuthenticationSuccessHandler</b>: Token-based authentication with TOKEN parameter
 *       removal after login to prevent reuse</li>
 *   <li><b>Datasources</b>: ConfigurationProperties target for multi-tenant datasource binding (datasources.list[i].name
 *       and datasources.list[i].config.*)</li>
 *   <li><b>TemplatePathFilteringProcessor</b>: Template path parser extracting access levels (organization/global) and
 *       email flags from Thymeleaf template paths</li>
 * </ul>
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>Root configuration classes for core Spring infrastructure (this package)</li>
 *   <li>session/ subpackage: Hazelcast-aware distributed session scope for clustered deployments</li>
 * </ul>
 *
 * <h2>Design Patterns</h2>
 * <ul>
 *   <li><b>Configuration pattern</b>: @Configuration classes with @Bean methods for dependency injection and
 *       framework integration</li>
 *   <li><b>Template Method pattern</b>: WebMvcConfigurer callbacks (addInterceptors, addResourceHandlers, etc.)
 *       for Spring MVC customization</li>
 *   <li><b>Strategy pattern</b>: Authentication handlers with different routing strategies based on user context
 *       (saved request, global admin, organization membership)</li>
 *   <li><b>Factory pattern</b>: Bean factory methods creating configured instances (mailSender, taskExecutor,
 *       securityFilterChain)</li>
 * </ul>
 *
 * <h2>Configuration Properties</h2>
 * <ul>
 *   <li>spring.mail.host, spring.mail.port, spring.mail.username, spring.mail.password: JavaMail SMTP configuration</li>
 *   <li>recaptcha.site-key, recaptcha.secret-key, recaptcha.validation: reCAPTCHA integration properties</li>
 *   <li>datasources.list[i].name, datasources.list[i].config.*: Multi-tenant datasource configuration via HikariCP</li>
 *   <li>frontendresource.create.if.not.exist, frontendresource.load.always.from.resources: Template resolver
 *       behavior flags for database vs classpath loading</li>
 *   <li>base.url, application.name, application.admin.email: OpenAPI metadata for documentation generation</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>
 * // Accessing configured beans in controllers or services
 * {@code @Autowired} private JavaMailSender mailSender;
 * {@code @Autowired} private TaskExecutor taskExecutor;
 * </pre>
 *
 * <h2>Relationships with Other Packages</h2>
 * <ul>
 *   <li>Provides Spring infrastructure for entire application (security, MVC, scheduling, mail, WebSocket)</li>
 *   <li>Used by controller, service, model, repository modules for framework integration</li>
 *   <li>configuration.session provides distributed session management for clustered deployments</li>
 *   <li>core.security, core.form, core.audit depend on beans from this package for authentication,
 *       form binding, and entity auditing</li>
 * </ul>
 *
 * <p><b>Should I put a class into this package?</b></p>
 * <p>If there is a new area of configuration, create a @Configuration class and define beans. First check if any
 * existing configuration class covers the area you want to create a bean for.</p>
 *
 * @see AuditConfig
 * @see JobConfig
 * @see MailSenderConfiguration
 * @see MvcConfig
 * @see WebSecurityConfig
 * @see SwaggerConfig
 * @see WebSocketConfig
 * @see ReCaptchaConfiguration
 * @see FrontendResourceOrClassLoaderTemplateResolver
 * @see CustomAuthenticationSuccessHandler
 * @see CustomAuthenticationFailureHandler
 * @see Datasources
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
package com.openkoda.core.configuration;