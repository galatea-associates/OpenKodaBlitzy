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

import com.openkoda.core.flow.LoggingComponent;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.openkoda.controller.common.URLConstants.API_TOKEN;

/**
 * Spring configuration class for OpenAPI 3.0 documentation generation via Springdoc.
 * <p>
 * Annotated with {@code @Configuration} to register OpenAPI beans that generate interactive API documentation.
 * Builds root OpenAPI document with application metadata (title, contact, license) and grouped API documentation
 * for organized endpoint display. Injects configuration properties via {@code @Value} for base URL, application name,
 * and admin contact email. Conditionally attaches API key security scheme for authentication documentation.
 * </p>
 * <p>
 * Generates two grouped API documentation sections:
 * <ul>
 * <li><b>auth group</b>: Authentication endpoints matching {@code /api/auth/**} pattern for login, logout, token operations</li>
 * <li><b>v1 group</b>: Versioned REST API endpoints matching {@code /api/v1/**} pattern for application resources</li>
 * </ul>
 * </p>
 * <p>
 * Interactive API documentation is accessible at {@code /swagger-ui.html} after application startup.
 * The OpenAPI JSON specification is available at {@code /v3/api-docs}.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see OpenAPI
 * @see GroupedOpenApi
 * @see io.swagger.v3.oas.annotations
 */
//TODO: Might need more Security cofigs
@Configuration
public class SwaggerConfig implements LoggingComponent {

    /**
     * Base URL for the application server, injected from {@code base.url} property.
     * Used in OpenAPI contact information and server configuration.
     * Defaults to {@code http://localhost:8080} if not configured.
     */
    @Value("${base.url:http://localhost:8080}") String baseUrl ;
    
    /**
     * Application name, injected from {@code application.name} property.
     * Used as OpenAPI document title and description.
     * Defaults to "Default Application" if not configured.
     */
    @Value("${application.name:Default Application}") String applicationName;
    
    /**
     * Admin contact email address, injected from {@code application.admin.email} property.
     * Used in OpenAPI contact information for API support inquiries.
     */
    @Value("${application.admin.email}") String contactEmail;

    /**
     * Constant name for API security requirement in OpenAPI security scheme.
     */
    private final static String API = "API";
    
    /**
     * Creates root OpenAPI documentation object with application metadata and server configuration.
     * <p>
     * Builds OpenAPI 3.0 document with API title from {@code application.name} property,
     * contact email from {@code application.admin.email}, and server URL from {@code base.url}.
     * Sets version to "1.0" and license to Apache 2.0. Security scheme for API key authentication
     * in Authorization header is conditionally attached when {@code secured} parameter is true
     * (currently disabled with {@code secured=false}).
     * </p>
     * <p>
     * This bean is the foundation for Springdoc's interactive API documentation UI. All grouped
     * API definitions ({@code authApi()}, {@code v1Api()}) reference this root document.
     * </p>
     *
     * @return configured OpenAPI root document with application info, contact details, and security schemes
     * @see Info
     * @see Components
     * @see SecurityScheme
     */
    @Bean
    public OpenAPI openkodaOpenAPI(){
        return buildOpenApi("1.0", false);
    }

    /**
     * Creates grouped API documentation for authentication endpoints.
     * <p>
     * Groups paths matching {@code /api/auth/**} pattern under "auth" group name.
     * Scans {@code com.openkoda.controller.api.auth} package for controller endpoints.
     * Provides dedicated documentation section for authentication flows including login,
     * logout, token refresh, and OAuth callback operations.
     * </p>
     * <p>
     * Displayed as separate group in Swagger UI for easy navigation to authentication endpoints.
     * </p>
     *
     * @return GroupedOpenApi for authentication API endpoints
     * @see GroupedOpenApi
     */
    @Bean
    public GroupedOpenApi publicApi(){
        return GroupedOpenApi.builder()
                .group("auth")
                .pathsToMatch("/api/auth/**")
                .packagesToScan("com.openkoda.controller.api.auth")
                .build();
    }

    /**
     * Creates grouped API documentation for v1 API endpoints.
     * <p>
     * Groups paths matching {@code /api/v1/**} pattern under "v1" group name.
     * Scans {@code com.openkoda.controller.api.v1} package for REST controller endpoints.
     * Provides versioned API documentation for application REST endpoints including
     * entity operations, business logic APIs, and integration endpoints.
     * </p>
     * <p>
     * Displayed as separate group in Swagger UI for version-specific API navigation.
     * </p>
     *
     * @return GroupedOpenApi for v1 REST API endpoints
     * @see GroupedOpenApi
     */
    @Bean
    public GroupedOpenApi v1Api(){
        return GroupedOpenApi.builder()
                .group("v1")
                .pathsToMatch("/api/v1/**")
                .packagesToScan("com.openkoda.controller.api.v1")
                .build();
    }

    /**
     * Builds OpenAPI root document with metadata and optional security scheme.
     * <p>
     * Creates OpenAPI 3.0 document with application info (title, description, version, contact, license).
     * When {@code secured} is true, adds API key security scheme to Components for authorization documentation.
     * Security scheme uses API_TOKEN constant as scheme name and configures APIKEY type in HEADER location.
     * </p>
     *
     * @param version API version string displayed in documentation (e.g., "1.0")
     * @param secured true to attach API key security scheme for authentication documentation, false otherwise
     * @return configured OpenAPI document with info and optional security components
     * @see OpenAPI
     * @see Components
     * @see #buildOpenApiInfo(String)
     * @see #securityScheme()
     */
    protected OpenAPI buildOpenApi(String version, boolean secured){
        debug("[buildApiInfo]");

        OpenAPI openAPI =  new OpenAPI().info(buildOpenApiInfo(version));
        if(secured){
            openAPI.components(new Components().addSecuritySchemes(API_TOKEN, securityScheme()));
        }
        return openAPI;
    }

    /**
     * Builds OpenAPI Info metadata object with application details.
     * <p>
     * Constructs Info object with:
     * <ul>
     * <li>Title and description from {@code application.name} property</li>
     * <li>Version from provided parameter</li>
     * <li>Apache 2.0 license with Springdoc URL</li>
     * <li>Contact with support team name, base URL from {@code base.url}, and email from {@code application.admin.email}</li>
     * </ul>
     * </p>
     * <p>
     * This metadata is displayed at the top of Swagger UI documentation page.
     * </p>
     *
     * @param version API version string to display in documentation
     * @return Info object with application metadata for OpenAPI document
     * @see Info
     * @see License
     * @see Contact
     */
    protected Info buildOpenApiInfo(String version){
        return new Info()
                .title(applicationName)
                .description(applicationName)
                .version(version)
                .license(new License().name("Apache 2.0").url("http://springdoc.org"))
                .contact(new Contact()
                        .name(applicationName + " - Support Team")
                        .url(baseUrl)
                        .email(contactEmail));
    }

    /**
     * Creates API key security scheme for authentication documentation.
     * <p>
     * Configures SecurityScheme with:
     * <ul>
     * <li>Scheme: API_TOKEN constant from URLConstants</li>
     * <li>Name: "API" as security requirement name</li>
     * <li>Type: APIKEY for API key authentication</li>
     * <li>Location: HEADER indicating token passed in HTTP header</li>
     * </ul>
     * </p>
     * <p>
     * When attached to OpenAPI document, displays "Authorize" button in Swagger UI
     * for users to input their API key for authenticated endpoint testing.
     * </p>
     *
     * @return SecurityScheme configured for API key authentication in header
     * @see SecurityScheme
     * @see com.openkoda.controller.common.URLConstants#API_TOKEN
     */
    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .scheme(API_TOKEN)
                .name(API)
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER);
    }
}