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

package com.openkoda.integration.model.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Global integration configuration loaded from application properties.
 * <p>
 * Contains OAuth client IDs and secrets for GitHub, Jira, and Basecamp integrations.
 * This Spring-managed bean provides application-wide integration credentials that are
 * injected from property sources such as application.properties, environment variables,
 * or external configuration files.
 * </p>
 * <p>
 * All fields are public and mutable at runtime without synchronization. The class is
 * instantiated by the Spring container as a singleton bean. Property values are injected
 * during bean initialization using {@code @Value} annotations with empty-string defaults,
 * ensuring non-null values even when credentials are not configured.
 * </p>
 * <p>
 * <b>Usage Context:</b> Autowire or inject this bean into services or controllers to
 * read global credentials for OAuth flows and API integrations.
 * </p>
 * <p>
 * <b>Security Note:</b> Credentials are stored in plain text fields. Avoid logging
 * secrets in production environments. Use external secret stores like HashiCorp Vault
 * or AWS Secrets Manager for production deployments.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * @Autowired
 * IntegrationModuleGlobalConfiguration config;
 * String clientId = config.gitHubClientId;
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see IntegrationModuleOrganizationConfiguration
 */
@Component
public class IntegrationModuleGlobalConfiguration {

    /**
     * Default constructor for Spring container instantiation.
     * <p>
     * The Spring framework invokes this constructor to create the singleton bean
     * instance during application context initialization. Property values are
     * injected into fields after construction.
     * </p>
     */
    public IntegrationModuleGlobalConfiguration() {}


    //      GitHub
    /**
     * OAuth client ID for GitHub integration.
     * <p>
     * Injected from application property {@code module.integration.github.client.id}.
     * Defaults to empty string if property is not configured. Used for GitHub OAuth
     * authorization flows to identify the application.
     * </p>
     */
    @Value("${module.integration.github.client.id:}")
    public String gitHubClientId;

    /**
     * OAuth client secret for GitHub integration.
     * <p>
     * Injected from application property {@code module.integration.github.client.secret}.
     * Defaults to empty string if property is not configured. This is a sensitive
     * credential used during GitHub OAuth token exchange.
     * </p>
     * <p>
     * <b>Security Warning:</b> This field contains sensitive data. Avoid logging or
     * exposing this value in error messages or API responses.
     * </p>
     */
    @Value("${module.integration.github.client.secret:}")
    public String gitHubClientSecret;

    //      Jira
    /**
     * OAuth client ID for Jira integration.
     * <p>
     * Injected from application property {@code module.integration.jira.client.id}.
     * Defaults to empty string if property is not configured. Used for Jira OAuth
     * authorization flows to identify the application.
     * </p>
     */
    @Value("${module.integration.jira.client.id:}")
    public String jiraClientId;

    /**
     * OAuth client secret for Jira integration.
     * <p>
     * Injected from application property {@code module.integration.jira.client.secret}.
     * Defaults to empty string if property is not configured. This is a sensitive
     * credential used during Jira OAuth token exchange.
     * </p>
     * <p>
     * <b>Security Warning:</b> This field contains sensitive data. Avoid logging or
     * exposing this value in error messages or API responses.
     * </p>
     */
    @Value("${module.integration.jira.client.secret:}")
    public String jiraClientSecret;

    //        BaseCamp
    /**
     * OAuth client ID for Basecamp integration.
     * <p>
     * Injected from application property {@code module.integration.basecamp.client.id}.
     * Defaults to empty string if property is not configured. Used for Basecamp OAuth
     * authorization flows to identify the application.
     * </p>
     */
    @Value("${module.integration.basecamp.client.id:}")
    public String basecampClientId;

    /**
     * OAuth client secret for Basecamp integration.
     * <p>
     * Injected from application property {@code module.integration.basecamp.client.secret}.
     * Defaults to empty string if property is not configured. This is a sensitive
     * credential used during Basecamp OAuth token exchange.
     * </p>
     * <p>
     * <b>Security Warning:</b> This field contains sensitive data. Avoid logging or
     * exposing this value in error messages or API responses.
     * </p>
     */
    @Value("${module.integration.basecamp.client.secret:}")
    public String basecampClientSecret;

}
