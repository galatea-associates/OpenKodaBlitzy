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

package com.openkoda.integration.controller.common;

import com.openkoda.controller.common.PageAttributes;
import com.openkoda.core.flow.PageAttr;
import com.openkoda.integration.form.*;

/**
 * Page model attributes container for integration views.
 * Provides typed access to integration form models in Flow pipelines and Thymeleaf templates.
 * <p>
 * This interface follows the interface-as-constants pattern where each constant is a typed
 * descriptor initialized at class load time with a stable attribute key matching the descriptor name.
 * All descriptors are implicitly public static final as defined by Java interface semantics.
 * </p>
 * <p>
 * The PageAttr descriptors provide compile-time type safety for model binding and extraction,
 * reducing reliance on brittle string-key access patterns. Controllers use these descriptors
 * to add form models to page contexts, and views reference them for rendering.
 * </p>
 * <p>
 * Example usage in Flow pipeline:
 * <pre>
 * Flow.init(integrationGitHubForm, form).thenSet(...)
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see PageAttr
 * @see PageAttributes
 * @see IntegrationTrelloForm
 * @see IntegrationGitHubForm
 * @see IntegrationSlackForm
 * @see IntegrationMsTeamsForm
 * @see IntegrationJiraForm
 * @see IntegrationBasecampForm
 */
public interface IntegrationPageAttributes extends PageAttributes {

    /**
     * Typed descriptor for Trello configuration form model.
     * Used to bind and retrieve IntegrationTrelloForm instances in page contexts.
     */
    PageAttr<IntegrationTrelloForm> integrationTrelloForm = new PageAttr<>("integrationTrelloForm");

    /**
     * Typed descriptor for GitHub configuration form model.
     * Used to bind and retrieve IntegrationGitHubForm instances in page contexts.
     */
    PageAttr<IntegrationGitHubForm> integrationGitHubForm = new PageAttr<>("integrationGitHubForm");

    /**
     * Typed descriptor for Slack configuration form model.
     * Used to bind and retrieve IntegrationSlackForm instances in page contexts.
     */
    PageAttr<IntegrationSlackForm> integrationSlackForm = new PageAttr<>("integrationSlackForm");

    /**
     * Typed descriptor for Microsoft Teams configuration form model.
     * Used to bind and retrieve IntegrationMsTeamsForm instances in page contexts.
     */
    PageAttr<IntegrationMsTeamsForm> integrationMsTeamsForm = new PageAttr<>("integrationMsTeamsForm");

    /**
     * Typed descriptor for Jira configuration form model.
     * Used to bind and retrieve IntegrationJiraForm instances in page contexts.
     */
    PageAttr<IntegrationJiraForm> integrationJiraForm = new PageAttr<>("integrationJiraForm");

    /**
     * Typed descriptor for Basecamp configuration form model.
     * Used to bind and retrieve IntegrationBasecampForm instances in page contexts.
     */
    PageAttr<IntegrationBasecampForm> integrationBasecampForm = new PageAttr<>("integrationBasecampForm");
}
