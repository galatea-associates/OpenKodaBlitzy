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

import com.openkoda.core.flow.Flow;
import com.openkoda.integration.controller.common.IntegrationURLConstants;
import com.openkoda.integration.form.*;
import com.openkoda.integration.model.configuration.IntegrationModuleOrganizationConfiguration;
import com.openkoda.integration.model.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import static com.openkoda.controller.common.URLConstants._HTML_ORGANIZATION_ORGANIZATIONID;
import static com.openkoda.controller.common.URLConstants._MODULE;
import static com.openkoda.core.security.HasSecurityRules.CHECK_CAN_MANAGE_ORG_DATA;
import static com.openkoda.integration.controller.common.IntegrationPageAttributes.*;
import static com.openkoda.integration.controller.common.IntegrationURLConstants._INTEGRATION;

/**
 * Spring MVC controller for integration module admin UI and OAuth callback endpoints.
 * Handles organization integration settings, OAuth authorization flows, and disconnect operations.
 * <p>
 * <b>Architecture:</b>
 * <ul>
 *   <li>Extends {@link IntegrationComponentProvider} to access integration services (integrationService, integrationUrlHelper)</li>
 *   <li>Uses Flow-based request handling with {@link com.openkoda.integration.controller.common.IntegrationPageAttributes} for typed model binding</li>
 *   <li>Implements GET/POST routes for integration configuration UI at /organization/{organizationId}/integration</li>
 *   <li>Handles OAuth callbacks with CODE and STATE parameters for GitHub, Jira, Basecamp</li>
 *   <li>Enforces security with @PreAuthorize checks (CHECK_CAN_MANAGE_ORG_DATA)</li>
 *   <li>Returns Thymeleaf fragments via Flow.mav() for partial page updates</li>
 * </ul>

 * <p>
 * <b>OAuth Callback Flow:</b>
 * <ol>
 *   <li>User clicks 'Connect GitHub' button in admin UI</li>
 *   <li>Redirect to GitHub authorization URL with client_id and redirect_uri (from IntegrationUrlHelper)</li>
 *   <li>User grants permissions on GitHub</li>
 *   <li>GitHub redirects to callback: /organization/{orgId}/integration/github?code={code}</li>
 *   <li>Controller receives code, validates implicit state</li>
 *   <li>Calls integrationService.getGitHubToken(code) to exchange for access token</li>
 *   <li>Token stored in IntegrationModuleOrganizationConfiguration entity</li>
 *   <li>User redirected to integration settings page with success navigation fragment</li>
 * </ol>

 * <p>
 * <b>Flow Pipeline Pattern:</b>
 * Flow.init() → thenSet(form) → then(validate) → then(persist) → execute() → mav(successView, errorView)

 *
 * @see IntegrationComponentProvider
 * @see com.openkoda.integration.service.IntegrationService
 * @see com.openkoda.integration.controller.common.IntegrationURLConstants
 * @see com.openkoda.integration.controller.common.IntegrationPageAttributes
 * @see Flow
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
@Controller
@RequestMapping({_HTML_ORGANIZATION_ORGANIZATIONID + _INTEGRATION, _HTML_ORGANIZATION_ORGANIZATIONID + _MODULE + _INTEGRATION} )
public class IntegrationControllerHtml extends IntegrationComponentProvider implements IntegrationURLConstants {

    /**
     * Displays integration settings page for an organization with current configuration.
     *
     * @param organizationId the organization identifier
     * @param request HTTP request for Flow initialization
     * @return Flow result rendering "module/integration/organization-settings" Thymeleaf template
     *         with organizationEntityId and organizationIntegrationModuleConfiguration model attributes
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_MANAGE_ORG_DATA privilege
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ORG_DATA)
    @GetMapping(_SETTINGS)
    public Object organizationSettings(@PathVariable(ORGANIZATIONID) Long organizationId, HttpServletRequest request) {
        debug("[organizationSettings] ModuleName: {}, OrgId: {}", INTEGRATION, organizationId);
        return integrationService.initFlowForOrganizationConfiguration(organizationId, request)
                .thenSet(organizationEntityId, a -> organizationId)
                .thenSet(organizationIntegrationModuleConfiguration,
                        a -> repositories.unsecure.integration.findByOrganizationId(organizationId))
                .execute()
                .mav(String.format("module/%s/organization-settings", INTEGRATION));
    }

    /**
     * Saves Trello integration configuration for an organization.
     * Validates form, populates entity, persists to database.
     *
     * @param orgId the organization identifier
     * @param form the Trello configuration form with API keys and settings
     * @param br Spring validation binding result
     * @return Flow result rendering "integration-fragments::trello-form-success" on success,
     *         "integration-fragments::trello-form-error" on validation failure
     */
    @PostMapping(_SETTINGS + _TRELLO)
    public Object saveTrelloConfig(@PathVariable(ORGANIZATIONID) Long orgId, @Valid IntegrationTrelloForm form, BindingResult br) {
        debug("[saveTrelloConfig] OrgId: {}", orgId);
        return Flow.init(integrationTrelloForm, form)
                .thenSet(organizationEntityId, a -> orgId)
                .then(a -> integrationService.getOrganizationConfiguration(orgId))
                .then(a -> a.model.get(integrationTrelloForm).recoverEntity(a.result))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br,a.result))
                .then(a -> repositories.unsecure.integration.save(a.result))
                .thenSet(integrationTrelloForm, a -> new IntegrationTrelloForm(new IntegrationTrelloDto(), (IntegrationModuleOrganizationConfiguration) a.result))
                .execute()
                .mav("module/integration/integration-fragments::trello-form-success",
                        "module/integration/integration-fragments::trello-form-error");
    }

    /**
     * Saves GitHub integration configuration for an organization.
     * Validates form, populates entity, persists to database.
     *
     * @param orgId the organization identifier
     * @param form the GitHub configuration form with OAuth client credentials
     * @param br Spring validation binding result
     * @return Flow result rendering "integration-fragments::gitHub-form-success" on success,
     *         "integration-fragments::gitHub-form-error" on validation failure
     */
    @PostMapping(_SETTINGS + _GITHUB)
    public Object saveGitHubConfig(@PathVariable(ORGANIZATIONID) Long orgId, @Valid IntegrationGitHubForm form, BindingResult br) {
        debug("[saveGitHubConfig] OrgId: {}", orgId);
        return Flow.init(integrationGitHubForm, form)
                .thenSet(organizationEntityId, a -> orgId)
                .then(a -> integrationService.getOrganizationConfiguration(orgId))
                .then(a -> a.model.get(integrationGitHubForm).recoverEntity(a.result))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br,a.result))
                .then(a -> repositories.unsecure.integration.save(a.result))
                .thenSet(integrationGitHubForm, a -> new IntegrationGitHubForm(new IntegrationGitHubDto(), (IntegrationModuleOrganizationConfiguration) a.result))
                .execute()
                .mav("module/integration/integration-fragments::gitHub-form-success",
                        "module/integration/integration-fragments::gitHub-form-error");
    }

    /**
     * Saves Slack integration configuration for an organization.
     * Validates form, populates entity, persists to database.
     *
     * @param orgId the organization identifier
     * @param form the Slack configuration form with webhook URL and settings
     * @param br Spring validation binding result
     * @return Flow result rendering "integration-fragments::slack-form-success" on success,
     *         "integration-fragments::slack-form-error" on validation failure
     */
    @PostMapping(_SETTINGS + _SLACK)
    public Object saveSlackConfig(@PathVariable(ORGANIZATIONID) Long orgId, @Valid IntegrationSlackForm form, BindingResult br) {
        debug("[saveSlackConfig] OrgId: {}", orgId);
        return Flow.init(integrationSlackForm, form)
                .thenSet(organizationEntityId, a -> orgId)
                .then(a -> integrationService.getOrganizationConfiguration(orgId))
                .then(a -> a.model.get(integrationSlackForm).recoverEntity(a.result))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br,a.result))
                .then(a -> repositories.unsecure.integration.save(a.result))
                .thenSet(integrationSlackForm, a -> new IntegrationSlackForm(new IntegrationSlackDto(), (IntegrationModuleOrganizationConfiguration) a.result))
                .execute()
                .mav("module/integration/integration-fragments::slack-form-success",
                        "module/integration/integration-fragments::slack-form-error");
    }

    /**
     * Saves Microsoft Teams integration configuration for an organization.
     * Validates form, populates entity, persists to database.
     *
     * @param orgId the organization identifier
     * @param form the MS Teams configuration form with webhook URL and settings
     * @param br Spring validation binding result
     * @return Flow result rendering "integration-fragments::msteams-form-success" on success,
     *         "integration-fragments::msteams-form-error" on validation failure
     */
    @PostMapping(_SETTINGS + _MSTEAMS)
    public Object saveMsTeamsConfig(@PathVariable(ORGANIZATIONID) Long orgId, @Valid IntegrationMsTeamsForm form, BindingResult br) {
        debug("[saveMsTeamsConfig] OrgId: {}", orgId);
        return Flow.init(integrationMsTeamsForm, form)
                .thenSet(organizationEntityId, a -> orgId)
                .then(a -> integrationService.getOrganizationConfiguration(orgId))
                .then(a -> a.model.get(integrationMsTeamsForm).recoverEntity(a.result))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br,a.result))
                .then(a -> repositories.unsecure.integration.save(a.result))
                .thenSet(integrationMsTeamsForm, a -> new IntegrationMsTeamsForm(new IntegrationMsTeamsDto(), (IntegrationModuleOrganizationConfiguration) a.result))
                .execute()
                .mav("module/integration/integration-fragments::msteams-form-success",
                        "module/integration/integration-fragments::msteams-form-error");
    }

    /**
     * Saves Basecamp integration configuration for an organization.
     * Validates form, populates entity, persists to database.
     *
     * @param orgId the organization identifier
     * @param form the Basecamp configuration form with OAuth client credentials
     * @param br Spring validation binding result
     * @return Flow result rendering "integration-fragments::basecamp-form-success" on success,
     *         "integration-fragments::basecamp-form-error" on validation failure
     */
    @PostMapping(_SETTINGS + _BASECAMP)
    public Object saveBasecampConfig(@PathVariable(ORGANIZATIONID) Long orgId, @Valid IntegrationBasecampForm form, BindingResult br) {
        debug("[saveBasecampConfig] OrgId: {}", orgId);
        return Flow.init(integrationBasecampForm, form)
                .thenSet(organizationEntityId, a -> orgId)
                .then(a -> integrationService.getOrganizationConfiguration(orgId))
                .then(a -> a.model.get(integrationBasecampForm).recoverEntity(a.result))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br,a.result))
                .then(a -> repositories.unsecure.integration.save(a.result))
                .thenSet(integrationBasecampForm, a -> new IntegrationBasecampForm(new IntegrationBasecampDto(), (IntegrationModuleOrganizationConfiguration) a.result))
                .execute()
                .mav("module/integration/integration-fragments::basecamp-form-success",
                        "module/integration/integration-fragments::basecamp-form-error");
    }

    /**
     * Saves Jira integration configuration for an organization.
     * Validates form, populates entity, persists to database.
     *
     * @param orgId the organization identifier
     * @param form the Jira configuration form with OAuth client credentials
     * @param br Spring validation binding result
     * @return Flow result rendering "integration-fragments::jira-form-success" on success,
     *         "integration-fragments::jira-form-error" on validation failure
     */
    @PostMapping(_SETTINGS + _JIRA)
    public Object saveJiraconfig(@PathVariable(ORGANIZATIONID) Long orgId, @Valid IntegrationJiraForm form, BindingResult br) {
        debug("[saveJiraconfig] OrgId: {}", orgId);
        return Flow.init(integrationJiraForm, form)
                .thenSet(organizationEntityId, a -> orgId)
                .then(a -> integrationService.getOrganizationConfiguration(orgId))
                .then(a -> a.model.get(integrationJiraForm).recoverEntity(a.result))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br,a.result))
                .then(a -> repositories.unsecure.integration.save(a.result))
                .thenSet(integrationJiraForm, a -> new IntegrationJiraForm(new IntegrationJiraDto(), (IntegrationModuleOrganizationConfiguration) a.result))
                .execute()
                .mav("module/integration/integration-fragments::jira-form-success",
                        "module/integration/integration-fragments::jira-form-error");
    }

    /**
     * OAuth callback endpoint for GitHub authorization.
     * Exchanges authorization code for access token and stores in organization configuration.
     *
     * @param orgId the organization identifier from path variable
     * @param code OAuth authorization code from GitHub
     * @return Thymeleaf navigation fragment redirecting to integration settings page
     * @throws Exception if token exchange fails or API call errors
     */
    @GetMapping(_GITHUB)
    public Object acceptGitHubToken(@PathVariable(ORGANIZATIONID) Long orgId, @RequestParam(value = CODE, required = true) String code) throws Exception {
        debug("[acceptGitHubToken] OrgId: {}", orgId);
        integrationService.getGitHubToken(orgId, code);
        String configUrl = services.url.moduleSettings(INTEGRATION, null, orgId);
        return "generic-forms::go-to(url='" + configUrl + "')";
    }

    /**
     * OAuth callback endpoint for Jira authorization.
     * Exchanges authorization code for access token and stores in organization configuration.
     *
     * @param code OAuth authorization code from Jira
     * @param orgId organization identifier passed as OAuth state parameter (CSRF protection)
     * @return Thymeleaf navigation fragment redirecting to integration settings page
     * @throws Exception if token exchange fails or API call errors
     */
    @GetMapping(_JIRA)
    public Object acceptJiraToken(@RequestParam(CODE) String code, @RequestParam(STATE) Long orgId) throws Exception {
        debug("[acceptJiraToken]");
        integrationService.getJiraToken(orgId, code);
        String configUrl = services.url.moduleSettings(INTEGRATION, null, orgId);
        return "generic-forms::go-to(url='" + configUrl + "')";
    }

    /**
     * OAuth callback endpoint for Basecamp authorization.
     * Exchanges authorization code for access token and stores in organization configuration.
     *
     * @param code OAuth authorization code from Basecamp
     * @param orgId organization identifier passed as OAuth state parameter (CSRF protection)
     * @return Thymeleaf navigation fragment redirecting to integration settings page
     * @throws Exception if token exchange fails or API call errors
     */
    @GetMapping(_BASECAMP)
    public Object acceptBasecampToken(@RequestParam(CODE) String code, @RequestParam(STATE) Long orgId) throws Exception {
        debug("[acceptBasecampToken] OrgId: {}", orgId);
        integrationService.getBasecampToken(orgId, code);
        String configUrl = services.url.moduleSettings(INTEGRATION, null, orgId);
        return "generic-forms::go-to(url='" + configUrl + "')";
    }

    /**
     * Disconnects Jira integration for an organization.
     * Clears OAuth tokens and configuration from database.
     *
     * @param orgId the organization identifier
     * @return Thymeleaf navigation fragment redirecting to integration settings page
     */
    @GetMapping(_SETTINGS + _JIRA + _DISCONNECT)
    public Object deleteJiraConfig(@PathVariable(ORGANIZATIONID) Long orgId) {
        debug("[deleteJiraConfig] OrgId: {}", orgId);
        integrationService.cleanOrgConfig(_JIRA, orgId);
        String configUrl = services.url.moduleSettings(INTEGRATION, null, orgId);
        return "generic-forms::go-to(url='" + configUrl + "')";
    }

    /**
     * Disconnects Basecamp integration for an organization.
     * Clears OAuth tokens and configuration from database.
     *
     * @param orgId the organization identifier
     * @return Thymeleaf navigation fragment redirecting to integration settings page
     */
    @GetMapping(_SETTINGS + _BASECAMP + _DISCONNECT)
    public Object deleteBasecampConfig(@PathVariable(ORGANIZATIONID) Long orgId) {
        debug("[deleteBasecampConfig] OrgId: {}", orgId);
        integrationService.cleanOrgConfig(_BASECAMP, orgId);
        String configUrl = services.url.moduleSettings(INTEGRATION, null, orgId);
        return "generic-forms::go-to(url='" + configUrl + "')";
    }

    /**
     * Disconnects Trello integration for an organization.
     * Clears API keys and configuration from database.
     *
     * @param orgId the organization identifier
     * @return Thymeleaf navigation fragment redirecting to integration settings page
     */
    @GetMapping(_SETTINGS + _TRELLO + _DISCONNECT)
    public Object deleteTrelloConfig(@PathVariable(ORGANIZATIONID) Long orgId) {
        debug("[deleteTrelloConfig] OrgId: {}", orgId);
        integrationService.cleanOrgConfig(_TRELLO, orgId);
        String configUrl = services.url.moduleSettings(INTEGRATION, null, orgId);
        return "generic-forms::go-to(url='" + configUrl + "')";
    }

    /**
     * Disconnects GitHub integration for an organization.
     * Clears OAuth tokens and configuration from database.
     *
     * @param orgId the organization identifier
     * @return Thymeleaf navigation fragment redirecting to integration settings page
     */
    @GetMapping(_SETTINGS + _GITHUB + _DISCONNECT)
    public Object deleteGitHubConfig(@PathVariable(ORGANIZATIONID) Long orgId) {
        debug("[deleteGitHubConfig] OrgId: {}", orgId);
        integrationService.cleanOrgConfig(_GITHUB, orgId);
        String configUrl = services.url.moduleSettings(INTEGRATION, null, orgId);
        return "generic-forms::go-to(url='" + configUrl + "')";
    }

    /**
     * Disconnects Microsoft Teams integration for an organization.
     * Clears webhook URLs and configuration from database.
     *
     * @param orgId the organization identifier
     * @return Thymeleaf navigation fragment redirecting to integration settings page
     */
    @GetMapping(_SETTINGS + _MSTEAMS + _DISCONNECT)
    public Object deleteMsTeamsConfig(@PathVariable(ORGANIZATIONID) Long orgId) {
        debug("[deleteMsTeamsConfig] OrgId: {}", orgId);
        integrationService.cleanOrgConfig(_MSTEAMS, orgId);
        String configUrl = services.url.moduleSettings(INTEGRATION, null, orgId);
        return "generic-forms::go-to(url='" + configUrl + "')";
    }

    /**
     * Disconnects Slack integration for an organization.
     * Clears webhook URLs and configuration from database.
     *
     * @param orgId the organization identifier
     * @return Thymeleaf navigation fragment redirecting to integration settings page
     */
    @GetMapping(_SETTINGS + _SLACK + _DISCONNECT)
    public Object deleteSlackConfig(@PathVariable(ORGANIZATIONID) Long orgId) {
        debug("[deleteSlackConfig] OrgId: {}", orgId);
        integrationService.cleanOrgConfig(_SLACK, orgId);
        String configUrl = services.url.moduleSettings(INTEGRATION, null, orgId);
        return "generic-forms::go-to(url='" + configUrl + "')";
    }

}
