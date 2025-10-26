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

import com.openkoda.core.form.AbstractEntityForm;
import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.integration.model.configuration.IntegrationModuleOrganizationConfiguration;
import com.openkoda.integration.model.dto.IntegrationJiraDto;
import org.springframework.validation.BindingResult;

/**
 * Form adapter for Jira integration configuration. Handles form binding for Jira project name,
 * access token, refresh token, and cloud ID (typically set via OAuth, not manual entry).
 * <p>
 * Extends {@code AbstractEntityForm<IntegrationJiraDto, IntegrationModuleOrganizationConfiguration>},
 * implements {@link FrontendMappingDefinition} lifecycle: {@code populateFrom(entity)} →
 * {@code validate(BindingResult)} → {@code populateTo(entity)}.
 * </p>
 * <p>
 * Form lifecycle:
 * <ol>
 * <li>{@code populateFrom(entity)}: loads projectName and organizationName from
 * IntegrationModuleOrganizationConfiguration into DTO</li>
 * <li>{@code validate(BindingResult)}: currently a no-op placeholder that returns this without
 * adding validation errors (business rules can be added later)</li>
 * <li>{@code populateTo(entity)}: stores validated values back to entity using getSafeValue
 * with PROJECT_NAME_ and ORGANIZATION_NAME_ mapping keys</li>
 * </ol>
 * </p>
 * <p>
 * OAuth integration note: Access token and refresh token are typically obtained via OAuth callback
 * controller (not entered manually by users). Cloud ID is discovered via Jira accessible-resources
 * API after OAuth authentication. This form primarily handles project name and organization name
 * manual input.
 * </p>
 * <p>
 * Validation note: {@code validate()} is a no-op placeholder - checks projectName is not empty
 * can be added if needed.
 * </p>
 * <p>
 * Integration: Used by Spring MVC controllers with @Valid annotation, BindingResult captures errors.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractEntityForm
 * @see IntegrationJiraDto
 * @see IntegrationModuleOrganizationConfiguration
 * @see IntegrationFrontendMappingDefinitions#jiraConfigurationForm
 */
public class IntegrationJiraForm extends AbstractEntityForm<IntegrationJiraDto, IntegrationModuleOrganizationConfiguration> implements LoggingComponentWithRequestId {
    
    /**
     * Creates form with new IntegrationJiraDto and jiraConfigurationForm mapping definition.
     */
    public IntegrationJiraForm() {
        super(new IntegrationJiraDto(), null, IntegrationFrontendMappingDefinitions.jiraConfigurationForm);
    }

    /**
     * Creates form with provided DTO and entity using jiraConfigurationForm mapping definition.
     *
     * @param integrationJiraDto the Jira integration DTO
     * @param organizationConfiguration the integration configuration entity
     */
    public IntegrationJiraForm(IntegrationJiraDto integrationJiraDto, IntegrationModuleOrganizationConfiguration organizationConfiguration) {
        super(integrationJiraDto, organizationConfiguration, IntegrationFrontendMappingDefinitions.jiraConfigurationForm);
    }

    /**
     * Creates form with provided DTO, entity, and custom frontend mapping definition.
     *
     * @param integrationJiraDto the Jira integration DTO
     * @param organizationConfiguration the integration configuration entity
     * @param formDef custom frontend mapping definition to override default
     */
    public IntegrationJiraForm(IntegrationJiraDto integrationJiraDto, IntegrationModuleOrganizationConfiguration organizationConfiguration, FrontendMappingDefinition formDef) {
        super(integrationJiraDto, organizationConfiguration, formDef);
    }

    /**
     * Loads Jira configuration from entity into DTO for form pre-population.
     * <p>
     * Copies jiraProjectName and jiraOrganizationName from the integration configuration entity
     * into the DTO for rendering in edit views.
     * </p>
     *
     * @param entity the IntegrationModuleOrganizationConfiguration to load from
     * @return this form instance for fluent chaining
     */
    @Override
    public IntegrationJiraForm populateFrom(IntegrationModuleOrganizationConfiguration entity) {
        dto.setProjectName(entity.getJiraProjectName());
        dto.setOrganizationName(entity.getJiraOrganizationName());
        return this;
    }

    /**
     * Stores validated Jira configuration from DTO back to entity for persistence.
     * <p>
     * Uses {@code getSafeValue} for null-safe field access with PROJECT_NAME_ and ORGANIZATION_NAME_
     * frontend mapping keys. Writes projectName and organizationName to entity properties.
     * </p>
     *
     * @param entity the IntegrationModuleOrganizationConfiguration to populate
     * @return the populated entity
     */
    @Override
    protected IntegrationModuleOrganizationConfiguration populateTo(IntegrationModuleOrganizationConfiguration entity) {
        entity.setJiraProjectName(getSafeValue(entity.getJiraProjectName(), PROJECT_NAME_));
        entity.setJiraOrganizationName(getSafeValue(entity.getJiraOrganizationName(), ORGANIZATION_NAME_));
        return entity;
    }

    /**
     * Validates the Jira configuration form data.
     * <p>
     * Currently a no-op placeholder that performs no validation and registers no errors.
     * Business validation rules (e.g., projectName is not empty, organizationName format)
     * can be added here if needed.
     * </p>
     * <p>
     * Note: Jira OAuth tokens (jiraAccessToken, jiraRefreshToken, jiraCloudId) are set via
     * JiraIntegrationController OAuth callback, not via this form. This form handles only
     * project/organization name configuration.
     * </p>
     *
     * @param br the Spring BindingResult for error collection
     * @return this form instance for fluent chaining
     */
    @Override
    public IntegrationJiraForm validate(BindingResult br) {
        return this;
    }
}
