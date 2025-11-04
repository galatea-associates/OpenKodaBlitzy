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

package com.openkoda.integration.form;

import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.form.TemplateFormFieldNames;

import static com.openkoda.core.form.FrontendMappingDefinition.createFrontendMappingDefinition;
import static com.openkoda.model.Privilege.manageOrgData;
import static com.openkoda.model.Privilege.readOrgData;

/**
 * Frontend field mapping definitions and identifiers for integration configuration forms.
 * <p>
 * Declares stable frontend field names used by Thymeleaf templates and form binding.
 * Provides {@link FrontendMappingDefinition} factory instances and canonical field identifier constants
 * for all supported third-party integrations (Trello, GitHub, Jira, Basecamp, Slack, MS Teams).
 * Used to maintain consistency between backend form processing (AbstractEntityForm implementations)
 * and frontend form rendering (Thymeleaf templates).

 * <p>
 * Each FrontendMappingDefinition instance declares form name, required privileges
 * (readOrgData for view, manageOrgData for edit), and field definitions using builder DSL
 * ({@code .text(...)} for text input fields).

 * <p>
 * <b>Architecture Role:</b> Single source of truth for frontend field identifiers.
 * Controllers, form adapters, and templates depend on this interface for stable mapping keys.
 * Changes to field names here propagate to all integration forms.

 * <p>
 * <b>Privilege Semantics:</b> Forms created with readOrgData privilege are editable by users
 * with organization data management rights. Forms created with readOrgData privilege and null
 * manageOrgData privilege (e.g., gitHubConfigurationFormDisabled) render as read-only/disabled variants.

 * <p>
 * <b>Builder DSL:</b> FrontendMappingDefinition uses createFrontendMappingDefinition factory
 * with functional builder: {@code .text(fieldName)} adds text input field, additional methods
 * available for other field types.

 * <p>
 * <b>Inherited Field Identifiers:</b> Extends TemplateFormFieldNames which declares canonical
 * String constants for field identifiers:
 * <ul>
 * <li>TRELLO_API_KEY_ = 'trelloApiKey': Frontend field name for Trello API key (32 hexadecimal chars)</li>
 * <li>TRELLO_API_TOKEN_ = 'trelloApiToken': Frontend field name for Trello API token (64 hexadecimal chars)</li>
 * <li>TRELLO_BOARD_NAME_ = 'trelloBoardName': Frontend field name for Trello board name</li>
 * <li>TRELLO_LIST_NAME_ = 'trelloListName': Frontend field name for Trello list name</li>
 * <li>GITHUB_REPO_OWNER_ = 'gitHubRepoOwner': Frontend field name for GitHub repository owner</li>
 * <li>GITHUB_REPO_NAME_ = 'gitHubRepoName': Frontend field name for GitHub repository name</li>
 * <li>GITHUB_ACCESS_TOKEN_ = 'gitHubAccessToken': Frontend field name for GitHub access token (OAuth)</li>
 * <li>PROJECT_NAME_ = 'projectName': Frontend field name for Jira project name</li>
 * <li>ORGANIZATION_NAME_ = 'organizationName': Frontend field name for Jira organization name</li>
 * <li>JIRA_ACCESS_TOKEN_ = 'jiraAccessToken': Frontend field name for Jira access token (OAuth)</li>
 * <li>JIRA_REFRESH_TOKEN_ = 'jiraRefreshToken': Frontend field name for Jira refresh token (OAuth)</li>
 * <li>JIRA_CLOUD_ID_ = 'jiraCloudId': Frontend field name for Jira cloud ID (discovered via API)</li>
 * <li>TODO_LIST_URL_ = 'toDoListUrl': Frontend field name for Basecamp todo list URL</li>
 * <li>WEBHOOK_URL_ = 'webhookUrl': Frontend field name for Slack/MS Teams webhook URL</li>
 * <li>EMAIL_ = 'email': Frontend field name for email address</li>
 * </ul>

 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * // Controllers construct form adapters with FrontendMappingDefinition
 * new IntegrationTrelloForm(dto, entity, IntegrationFrontendMappingDefinitions.trelloConfigurationForm);
 * }</pre>
 * Form adapter uses mapping to bind DTO fields to entity properties via
 * {@code getSafeValue(entity.getProperty(), FIELD_IDENTIFIER_)}.
 * Thymeleaf templates reference same field identifiers for consistent form rendering.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FrontendMappingDefinition
 * @see TemplateFormFieldNames
 * @see com.openkoda.core.form.AbstractEntityForm
 */
public interface IntegrationFrontendMappingDefinitions extends TemplateFormFieldNames {
    /**
     * Form identifier for Trello integration configuration form.
     * Value: 'trelloConfigurationForm'
     */
    String TRELLO_CONFIGURATION_FORM = "trelloConfigurationForm";
    
    /**
     * Form identifier for GitHub integration configuration form.
     * Value: 'gitHubConfigurationForm'
     */
    String GITHUB_CONFIGURATION_FORM = "gitHubConfigurationForm";
    
    /**
     * Form identifier for Jira integration configuration form.
     * Value: 'jiraConfigurationForm'
     */
    String JIRA_CONFIGURATION_FORM = "jiraConfigurationForm";
    
    /**
     * Form identifier for Basecamp integration configuration form.
     * Value: 'basecampConfigurationForm'
     */
    String BASECAMP_CONFIGURATION_FORM = "basecampConfigurationForm";
    
    /**
     * Form identifier for Slack integration configuration form.
     * Value: 'slackConfigurationForm'
     */
    String SLACK_CONFIGURATION_FORM = "slackConfigurationForm";
    
    /**
     * Form identifier for Microsoft Teams integration configuration form.
     * Value: 'msTeamsConfigurationForm'
     */
    String MSTEAMS_CONFIGURATION_FORM = "msTeamsConfigurationForm";
    
    /**
     * Form identifier for email integration configuration form.
     * Value: 'emailConfigurationForm'
     */
    String EMAIL_CONFIGURATION_FORM = "emailConfigurationForm";

    /**
     * Trello integration form mapping definition with editable text fields.
     * <p>
     * Privileges: readOrgData (view), manageOrgData (edit)

     * <p>
     * Fields:
     * <ul>
     * <li>TRELLO_API_KEY_ (32-char hexadecimal API key)</li>
     * <li>TRELLO_API_TOKEN_ (64-char hexadecimal token)</li>
     * <li>TRELLO_BOARD_NAME_ (target board name)</li>
     * <li>TRELLO_LIST_NAME_ (target list name)</li>
     * </ul>

     * Used by IntegrationTrelloForm
     */
    FrontendMappingDefinition trelloConfigurationForm = createFrontendMappingDefinition(TRELLO_CONFIGURATION_FORM, readOrgData, manageOrgData,
            a -> a  .text(TRELLO_API_KEY_)
                    .text(TRELLO_API_TOKEN_)
                    .text(TRELLO_BOARD_NAME_)
                    .text(TRELLO_LIST_NAME_)
    );

    /**
     * GitHub integration form mapping definition with editable text fields.
     * <p>
     * Privileges: readOrgData (view), manageOrgData (edit)

     * <p>
     * Fields:
     * <ul>
     * <li>GITHUB_REPO_OWNER_ (repository owner username)</li>
     * <li>GITHUB_REPO_NAME_ (repository name)</li>
     * </ul>

     * Used by IntegrationGitHubForm
     */
    FrontendMappingDefinition gitHubConfigurationForm = createFrontendMappingDefinition(GITHUB_CONFIGURATION_FORM, readOrgData, manageOrgData,
            a -> a  .text(GITHUB_REPO_OWNER_)
                    .text(GITHUB_REPO_NAME_)
    );

    /**
     * Read-only variant of GitHub integration form.
     * <p>
     * Privileges: readOrgData (view), null (no edit)

     * <p>
     * Fields:
     * <ul>
     * <li>GITHUB_REPO_NAME_ (rendered as disabled input)</li>
     * <li>GITHUB_REPO_OWNER_ (rendered as disabled input)</li>
     * </ul>

     * Used for display-only scenarios where users should not modify GitHub configuration
     */
    FrontendMappingDefinition gitHubConfigurationFormDisabled = createFrontendMappingDefinition(GITHUB_CONFIGURATION_FORM, readOrgData, null,
            a -> a  .text(GITHUB_REPO_NAME_)
                    .text(GITHUB_REPO_OWNER_)
    );

    /**
     * Slack integration form mapping definition with webhook URL field.
     * <p>
     * Privileges: readOrgData (view), manageOrgData (edit)

     * <p>
     * Fields:
     * <ul>
     * <li>WEBHOOK_URL_ (Slack incoming webhook URL format: https://hooks.slack.com/services/*)</li>
     * </ul>

     * Used by IntegrationSlackForm
     */
    FrontendMappingDefinition slackConfigurationForm = createFrontendMappingDefinition(SLACK_CONFIGURATION_FORM, readOrgData, manageOrgData,
            a -> a  .text(WEBHOOK_URL_)
    );

    /**
     * Microsoft Teams integration form mapping definition with webhook URL field.
     * <p>
     * Privileges: readOrgData (view), manageOrgData (edit)

     * <p>
     * Fields:
     * <ul>
     * <li>WEBHOOK_URL_ (MS Teams incoming webhook URL format: https://outlook.office.com/webhook/* or https://*.webhook.office.com/*)</li>
     * </ul>

     * Used by IntegrationMsTeamsForm
     */
    FrontendMappingDefinition msTeamsConfigurationForm = createFrontendMappingDefinition(MSTEAMS_CONFIGURATION_FORM, readOrgData, manageOrgData,
            a -> a  .text(WEBHOOK_URL_)
    );

    /**
     * Email integration form mapping definition.
     * <p>
     * Privileges: readOrgData (view), manageOrgData (edit)

     * <p>
     * Fields:
     * <ul>
     * <li>EMAIL_ (email address for notifications)</li>
     * </ul>

     */
    FrontendMappingDefinition emailConfigurationForm = createFrontendMappingDefinition(EMAIL_CONFIGURATION_FORM, readOrgData, manageOrgData,
            a -> a  .text(EMAIL_)
    );

    /**
     * Jira integration form mapping definition with editable text fields.
     * <p>
     * Privileges: readOrgData (view), manageOrgData (edit)

     * <p>
     * Fields:
     * <ul>
     * <li>ORGANIZATION_NAME_ (Jira organization/site name)</li>
     * <li>PROJECT_NAME_ (Jira project key or name)</li>
     * </ul>

     * <p>
     * Used by IntegrationJiraForm. Note: OAuth tokens (jiraAccessToken, jiraRefreshToken, jiraCloudId)
     * are set via OAuth callback controller, not via this form.

     */
    FrontendMappingDefinition jiraConfigurationForm = createFrontendMappingDefinition(JIRA_CONFIGURATION_FORM, readOrgData, manageOrgData,
            a -> a  .text(ORGANIZATION_NAME_)
                    .text(PROJECT_NAME_)
    );

    /**
     * Read-only variant of Jira integration form.
     * <p>
     * Privileges: readOrgData (view), null (no edit)

     * <p>
     * Fields:
     * <ul>
     * <li>ORGANIZATION_NAME_ (rendered as disabled input)</li>
     * <li>PROJECT_NAME_ (rendered as disabled input)</li>
     * </ul>

     * Used for display-only scenarios
     */
    FrontendMappingDefinition jiraConfigurationFormDisabled = createFrontendMappingDefinition(JIRA_CONFIGURATION_FORM, readOrgData, null,
            a -> a  .text(ORGANIZATION_NAME_)
                    .text(PROJECT_NAME_)
    );

    /**
     * Basecamp integration form mapping definition with todo list URL field.
     * <p>
     * Privileges: readOrgData (view), manageOrgData (edit)

     * <p>
     * Fields:
     * <ul>
     * <li>TODO_LIST_URL_ (Basecamp todo list URL format: https://3.basecamp.com/{accountId}/buckets/{projectId}/todolists/{todolistId})</li>
     * </ul>

     * <p>
     * Used by IntegrationBasecampForm. Form extracts accountId, projectId, todolistId from URL
     * during validation.

     */
    FrontendMappingDefinition basecampConfigurationForm = createFrontendMappingDefinition(BASECAMP_CONFIGURATION_FORM, readOrgData, manageOrgData,
            a -> a  .text(TODO_LIST_URL_)
    );

    /**
     * Read-only variant of Basecamp integration form.
     * <p>
     * Privileges: readOrgData (view), null (no edit)

     * <p>
     * Fields:
     * <ul>
     * <li>TODO_LIST_URL_ (rendered as disabled input)</li>
     * </ul>

     * Used for display-only scenarios
     */
    FrontendMappingDefinition basecampConfigurationFormDisabled = createFrontendMappingDefinition(BASECAMP_CONFIGURATION_FORM, readOrgData, null,
            a -> a  .text(TODO_LIST_URL_)
    );

}
