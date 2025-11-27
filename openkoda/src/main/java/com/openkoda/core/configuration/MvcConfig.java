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

import com.openkoda.controller.HtmlCRUDControllerConfigurationMap;
import com.openkoda.controller.common.SessionData;
import com.openkoda.controller.common.URLConstants;
import com.openkoda.core.customisation.FrontendMappingMap;
import com.openkoda.core.exception.ErrorLoggingExceptionResolver;
import com.openkoda.core.form.MapFormArgumentResolver;
import com.openkoda.core.helper.ModulesInterceptor;
import com.openkoda.core.helper.SlashEndingUrlInterceptor;
import com.openkoda.core.helper.UrlHelper;
import com.openkoda.core.multitenancy.QueryExecutor;
import com.openkoda.core.service.FrontendResourceService;
import com.openkoda.model.MutableUserInOrganization;
import com.openkoda.service.export.ClasspathComponentImportService;
import jakarta.inject.Inject;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.CacheControl;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.openkoda.core.service.FrontendResourceService.frontendResourceTemplateNamePrefix;

/**
 * Central Spring MVC configuration class implementing WebMvcConfigurer.
 * <p>
 * Annotated with {@code @Configuration} to register Spring MVC customizations for the OpenKoda platform.
 * This class configures critical MVC infrastructure including view mappings, resource handlers, interceptors,
 * exception resolution, argument resolvers, and template resolvers for multi-tenant Thymeleaf rendering.
 * 
 * <p>
 * Key configurations provided:
 * <ul>
 *   <li>Root view mapping to homepage</li>
 *   <li>Resource handlers for vendor assets (/vendor/**) with 7-day cache control</li>
 *   <li>Three custom interceptors: modelEnricherInterceptor (common model attributes),
 *       modulesInterceptor (module-specific processing), slashEndingUrlInterceptor (URL normalization)</li>
 *   <li>ErrorLoggingExceptionResolver for uncaught exception handling with requestId tracking</li>
 *   <li>MapFormArgumentResolver for automatic MapEntity form binding in controller methods</li>
 *   <li>FrontendResourceOrClassLoaderTemplateResolver for tenant-aware template loading from database or classpath</li>
 *   <li>StringTemplateResolver for inline template string resolution</li>
 *   <li>Request-scoped getUserInOrganization bean (MutableUserInOrganization) for authenticated user context</li>
 *   <li>Multi-datasource configuration via datasources() bean bound to application properties</li>
 * </ul>
 * <p>
 * This configuration integrates Thymeleaf template resolution, custom argument resolvers for form handling,
 * comprehensive exception handling, and multi-datasource support for tenant isolation.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer
 * @see FrontendResourceOrClassLoaderTemplateResolver
 * @see ErrorLoggingExceptionResolver
 * @see MapFormArgumentResolver
 * @see Datasources
 * @see MutableUserInOrganization
 */
@Configuration
@EnableSpringDataWebSupport
@Import(SwaggerConfig.class)
public class MvcConfig implements URLConstants, WebMvcConfigurer  {

    public static final String USER_IN_ORG = "userInOrganization";


    @Value("${user.agent.excluded.from.error.log:}")
    String userAgentExcludedFromErrorLog;

    @Value("${frontendresource.load.always.from.resources:false}")
    boolean frontendResourceLoadAlwaysFromResources;

    @Value("${frontendresource.create.if.not.exist:false}")
    boolean frontendResourceCreateIfNotExist;

    @Value("${default.pages.homeview:home}")
    String homeViewName;

    @Inject
    private HandlerInterceptor modelEnricherInterceptor;

    @Inject
    private ModulesInterceptor modulesInterceptor;

    @Inject
    private SlashEndingUrlInterceptor slashEndingUrlInterceptor;

    @Inject
    public ClasspathComponentImportService classpathComponentImportService;

    /**
     * Registers root URL '/' to redirect to homepage view.
     * <p>
     * Maps the empty path to the default landing page configured via
     * {@code default.pages.homeview} property. The view name is prefixed
     * with {@code frontendResourceTemplateNamePrefix} to enable tenant-aware
     * template resolution through the FrontendResourceOrClassLoaderTemplateResolver.
     * 
     *
     * @param registry ViewControllerRegistry for view controller mappings
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName( frontendResourceTemplateNamePrefix + homeViewName);
    }

    /**
     * Configures static resource handlers for vendor assets served from classpath.
     * <p>
     * Maps requests to {@code /vendor/**} paths to {@code classpath:/public/vendor/} location
     * with 7-day cache control headers. This enables efficient serving of JavaScript libraries,
     * CSS frameworks, and other static assets packaged in JARs (e.g., WebJars).
     * The public cache control allows both browser and CDN caching for optimal performance.
     * 
     *
     * @param registry ResourceHandlerRegistry for static resource mapping configuration
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/vendor/**").addResourceLocations("classpath:/public/vendor/").setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());
    }

    /**
     * Registers three custom interceptors executed for all HTTP requests.
     * <p>
     * Interceptors are executed in registration order:
     * <ul>
     *   <li>{@code modelEnricherInterceptor} - Adds common model attributes to all views</li>
     *   <li>{@code modulesInterceptor} - Performs module-specific request processing</li>
     *   <li>{@code slashEndingUrlInterceptor} - Normalizes URLs by handling trailing slashes</li>
     * </ul>
     * These interceptors provide cross-cutting request handling functionality including
     * model enrichment, module lifecycle hooks, and URL consistency enforcement.
     * 
     *
     * @param registry InterceptorRegistry for interceptor registration and ordering
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(modelEnricherInterceptor);
        registry.addInterceptor(modulesInterceptor);
        registry.addInterceptor(slashEndingUrlInterceptor);
    }

    /**
     * Configures exception resolvers for handling uncaught exceptions in controller methods.
     * <p>
     * Creates and registers {@link ErrorLoggingExceptionResolver} for comprehensive exception handling.
     * This resolver maps exceptions to the /error view with requestId tracking, provides conditional
     * logging based on exception type and severity, and implements noise suppression for client-abort
     * scenarios. User agents matching the {@code user.agent.excluded.from.error.log} pattern are
     * excluded from error logging to reduce log noise from bots and scanners.
     * 
     *
     * @param exceptionResolvers List of HandlerExceptionResolver instances to be configured
     * @see ErrorLoggingExceptionResolver
     */
    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
        exceptionResolvers.add(new ErrorLoggingExceptionResolver(userAgentExcludedFromErrorLog));
    }

    /**
     * Creates request-scoped MutableUserInOrganization bean holding current user context.
     * <p>
     * Scope {@code REQUEST} ensures a new instance is created for each HTTP request.
     * This bean is populated by the security infrastructure with the authenticated user's
     * information and organization context. Controllers and services can inject this bean
     * to access the current user's identity and tenant association without passing
     * context explicitly through method parameters.
     * 
     *
     * @return request-scoped MutableUserInOrganization for current authenticated user context
     * @see MutableUserInOrganization
     */
    @Bean
    @Scope("request")
    public MutableUserInOrganization getUserInOrganization() {
        return new MutableUserInOrganization();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) mappings.
     * <p>
     * Currently disabled. When enabled, this method can configure CORS policies
     * for specific URL patterns to allow cross-origin requests from designated
     * origins. This is useful for enabling API access from frontend applications
     * hosted on different domains (e.g., separate Angular or React applications).
     * 
     *
     * @param registry CorsRegistry for CORS configuration
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/data/**").allowedOrigins("http://localhost:4200");
    }

    /**
     * Creates Datasources configuration bean bound to application properties.
     * <p>
     * Binds multi-tenant datasource configurations from application.properties using
     * the {@code datasources} property prefix via {@code @ConfigurationProperties}.
     * This enables external configuration of multiple datasources with properties like:
     * <ul>
     *   <li>datasources.list[i].name - datasource identifier</li>
     *   <li>datasources.list[i].config.* - datasource-specific configuration (URL, credentials, pool settings)</li>
     * </ul>
     * The returned POJO provides structured access to multi-datasource configuration
     * for tenant isolation and data partitioning strategies.
     * 
     *
     * @return Datasources POJO bound to application properties for multi-datasource configuration
     * @see Datasources
     */
    @Bean
    @ConfigurationProperties(prefix = "datasources")
    public Datasources datasources() {
        return new Datasources();
    }


    /**
     * Creates FrontendResourceOrClassLoaderTemplateResolver for tenant-aware template loading.
     * <p>
     * This resolver enables templates to be loaded from either the database (per-organization customization)
     * or classpath resources (default templates). It supports dynamic template resolution with
     * the prefix {@code frontend-resource#} and suffix {@code .html}, rendering in HTML5 mode.
     * Template caching is controlled by the {@code frontendresource.load.always.from.resources} flag.
     * Order 1 ensures this resolver is evaluated before the StringTemplateResolver (order 2).
     * 
     * <p>
     * This enables per-organization template customization where tenants can override default
     * templates with custom versions stored in the database while falling back to classpath
     * resources when customizations don't exist.
     * 
     *
     * @param queryExecutor QueryExecutor for database template queries
     * @param frontendResourceService FrontendResourceService for template loading logic
     * @param filteringProcessor TemplatePathFilteringProcessor for template path validation
     * @return configured ITemplateResolver for database and classpath template resolution
     * @see FrontendResourceOrClassLoaderTemplateResolver
     */
    @Bean
    @Description("Thymeleaf template resolver serving HTML 5")
    public ClassLoaderTemplateResolver templateResolver(QueryExecutor queryExecutor, FrontendResourceService frontendResourceService, TemplatePathFilteringProcessor filteringProcessor) {

        FrontendResourceOrClassLoaderTemplateResolver templateResolver = new FrontendResourceOrClassLoaderTemplateResolver(
                queryExecutor,
                frontendResourceService,
                classpathComponentImportService,
                frontendResourceLoadAlwaysFromResources,
                frontendResourceCreateIfNotExist,
                filteringProcessor);
        templateResolver.setPrefix("templates/");
        templateResolver.setCacheable(false);
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML5");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setOrder(1);
        return templateResolver;
    }


    /**
     * Creates StringTemplateResolver for inline template string resolution.
     * <p>
     * This resolver enables dynamic template generation from string sources rather than
     * file-based templates. Template mode is set to HTML, and order is 1 (evaluated
     * alongside the database/classpath resolver). This resolver acts as a fallback
     * mechanism enabling runtime template generation from strings, useful for
     * dynamic content rendering scenarios where templates are constructed programmatically.
     * 
     *
     * @return configured ITemplateResolver for string-based template resolution
     */
    @Bean
    @Description("Thymeleaf string resolver serving HTML 5")
    public StringTemplateResolver stringTemplateResolver() {

        StringTemplateResolver stringTemplateResolver = new StringTemplateResolver();
        stringTemplateResolver.setTemplateMode(TemplateMode.HTML);
        stringTemplateResolver.setOrder(1);
        return stringTemplateResolver;

    }

    @Autowired
    @Qualifier("mvcConversionService")
    public ObjectFactory<ConversionService> conversionService;

    @Inject
    public HtmlCRUDControllerConfigurationMap htmlCrudControllerConfigurationMap;

    @Inject
    FrontendMappingMap frontendMappingMap;

    @Inject
    public UrlHelper urlHelper;

    /**
     * Registers custom argument resolvers including MapFormArgumentResolver.
     * <p>
     * Creates and registers {@link MapFormArgumentResolver} for automatic MapEntity form binding.
     * This enables controller method parameters annotated with {@code @ModelAttribute} of type
     * MapEntity to be automatically resolved and populated from HTTP request data.
     * The resolver uses reflection-based mapping to convert form data to MapEntity instances,
     * leveraging htmlCrudControllerConfigurationMap for CRUD configuration, frontendMappingMap
     * for field mappings, and urlHelper for URL resolution.
     * 
     * <p>
     * This enables Spring MVC to resolve custom parameter types in controller methods,
     * simplifying form handling for dynamic entity types.
     * 
     *
     * @param argumentResolvers List of HandlerMethodArgumentResolver instances to be registered
     * @see MapFormArgumentResolver
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new MapFormArgumentResolver(htmlCrudControllerConfigurationMap, frontendMappingMap, urlHelper));
    }

    /**
     * Creates SessionLocaleResolver for locale management in user sessions.
     * <p>
     * Configures a session-based locale resolver with English as the default locale.
     * The locale is stored in the session under the attribute name defined by
     * {@code SessionData.LOCALE}. This enables per-user locale preferences that
     * persist across requests within the same session, supporting internationalization
     * of the application UI.
     * 
     *
     * @return configured LocaleResolver for session-based locale management
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(Locale.ENGLISH);
        slr.setLocaleAttributeName(SessionData.LOCALE);
        return slr;
    }

}
