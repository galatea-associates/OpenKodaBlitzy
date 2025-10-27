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

package com.openkoda.integration.controller;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.integration.service.IntegrationService;
import com.openkoda.integration.service.IntegrationUrlHelper;
import jakarta.inject.Inject;

/**
 * Abstract base controller providing common integration components and dependencies to integration controllers.
 * <p>
 * Extends {@link ComponentProvider} to inherit base controller infrastructure including repositories,
 * services, and security context. Injects integration-specific services: {@link IntegrationService}
 * for OAuth token management and organization configuration, and {@link IntegrationUrlHelper} for
 * provider authorization URL generation.
 * </p>
 * <p>
 * Extended by {@link IntegrationControllerHtml} to access integration module beans without manual
 * service lookup. Uses Jakarta CDI field injection (@Inject) for stateless dependency wiring.
 * </p>
 * <p>
 * <b>Design Pattern:</b> Implements component provider pattern - centralizes dependency injection
 * so subclasses inherit all necessary services without boilerplate constructor or setter injection.
 * </p>
 * <p>
 * <b>Thread Safety:</b> Stateless and thread-safe. Contains no mutable state, only injected service
 * references managed by the Spring/Jakarta CDI container.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * public class IntegrationControllerHtml extends IntegrationComponentProvider {
 *     // Inherits integrationService and integrationUrlHelper
 *     public Object callback(String code) {
 *         integrationService.getGitHubToken(orgId, code);
 *         return "redirect:/settings";
 *     }
 * }
 * }</pre>
 * </p>
 *
 * @see IntegrationControllerHtml
 * @see IntegrationService
 * @see IntegrationUrlHelper
 * @see ComponentProvider
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class IntegrationComponentProvider extends ComponentProvider {

    /**
     * Integration service handling OAuth token exchange, organization configuration persistence,
     * and provider-specific API interactions (GitHub, Jira, Basecamp, Trello, Slack, MS Teams).
     * Injected by Spring/Jakarta CDI container.
     */
    @Inject
    public IntegrationService integrationService;

    /**
     * URL helper service generating provider-specific OAuth authorization URLs with client IDs,
     * redirect URIs, and CSRF state parameters. Injected by Spring/Jakarta CDI container.
     */
    @Inject
    public IntegrationUrlHelper integrationUrlHelper;

}
