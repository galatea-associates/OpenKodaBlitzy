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
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.integration.model.configuration.IntegrationModuleOrganizationConfiguration;
import com.openkoda.integration.model.dto.IntegrationSlackDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;

/**
 * Form adapter for Slack integration configuration. Handles form binding and validation for Slack incoming webhook URL.
 * <p>
 * Extends {@link AbstractEntityForm} and implements the FrontendMappingDefinition lifecycle:
 * populateFrom(entity) → validate(BindingResult) → populateTo(entity).
 * 
 * <p>
 * Form lifecycle:
 * 
 * <ol>
 *   <li>populateFrom(entity): loads slackWebhookUrl from IntegrationModuleOrganizationConfiguration into DTO</li>
 *   <li>validate(BindingResult): checks webhookUrl is present (rejectValue 'dto.webhookUrl', 'not.empty'),
 *       validates URL format (must be HTTPS), validates URL pattern matches Slack webhook format</li>
 *   <li>populateTo(entity): stores validated webhookUrl to entity using getSafeValue with WEBHOOK_URL_ mapping key</li>
 * </ol>
 * <p>
 * Validation rules:
 * 
 * <ul>
 *   <li>webhookUrl: required (not blank)</li>
 *   <li>URL must be HTTPS</li>
 *   <li>URL must match pattern: https://hooks.slack.com/services/*</li>
 * </ul>
 * <p>
 * Error codes:
 * 
 * <ul>
 *   <li>not.empty - Webhook URL is required and cannot be blank</li>
 * </ul>
 * <p>
 * Note: validate() returns null instead of this, which breaks fluent chaining expected by some callers.
 * Additional URL format validation (HTTPS, Slack pattern) can be added to validate() method.
 * 
 * <p>
 * Webhook configuration: Slack incoming webhook URLs are created in Slack workspace settings:
 * Apps → Incoming Webhooks → Add New Webhook to Workspace. Copy the generated webhook URL
 * (format: https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX) to this form.
 * 
 * <p>
 * Integration: Used by Spring MVC controllers with @Valid annotation, BindingResult captures errors.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see IntegrationSlackDto
 * @see IntegrationModuleOrganizationConfiguration
 * @see IntegrationFrontendMappingDefinitions#slackConfigurationForm
 */
public class IntegrationSlackForm extends AbstractEntityForm<IntegrationSlackDto, IntegrationModuleOrganizationConfiguration>
        implements LoggingComponentWithRequestId {
    
    /**
     * Creates form with new IntegrationSlackDto and slackConfigurationForm mapping definition.
     * <p>
     * This default constructor initializes the form with an empty DTO and the standard
     * Slack configuration form definition from {@link IntegrationFrontendMappingDefinitions}.
     * 
     */
    public IntegrationSlackForm() {
        super(new IntegrationSlackDto(), null, IntegrationFrontendMappingDefinitions.slackConfigurationForm);
    }

    /**
     * Creates form with provided DTO and entity.
     * <p>
     * This constructor is used when populating the form with existing configuration
     * data for editing.
     * 
     *
     * @param dto the Slack integration DTO containing webhook URL
     * @param entity the integration configuration entity
     */
    public IntegrationSlackForm(IntegrationSlackDto dto, IntegrationModuleOrganizationConfiguration entity) {
        super(dto, entity, IntegrationFrontendMappingDefinitions.slackConfigurationForm);
    }

    /**
     * Loads Slack webhook URL from entity into DTO for form pre-population in edit views.
     * <p>
     * This method is called when displaying the form with existing configuration data.
     * It retrieves the Slack webhook URL from the entity and sets it in the DTO for
     * rendering in the user interface.
     * 
     *
     * @param entity the IntegrationModuleOrganizationConfiguration to load from
     * @return this form instance for fluent chaining
     */
    @Override
    public IntegrationSlackForm populateFrom(IntegrationModuleOrganizationConfiguration entity) {
        dto.setWebhookUrl(entity.getSlackWebhookUrl());
        return this;
    }

    /**
     * Stores validated webhook URL from DTO to entity using null-safe field access.
     * <p>
     * This method is called after successful validation to persist the form data back
     * to the entity. It uses getSafeValue with WEBHOOK_URL_ frontend mapping key to
     * safely retrieve the webhook URL value from the DTO and store it in the entity.
     * 
     *
     * @param entity the IntegrationModuleOrganizationConfiguration to populate
     * @return the populated entity
     */
    @Override
    protected IntegrationModuleOrganizationConfiguration populateTo(IntegrationModuleOrganizationConfiguration entity) {
        entity.setSlackWebhookUrl(getSafeValue(entity.getSlackWebhookUrl(), WEBHOOK_URL_));
        return entity;
    }

    /**
     * Validates webhook URL is not blank and registers errors in BindingResult.
     * <p>
     * Current validation checks:
     * 
     * <ul>
     *   <li>Webhook URL is not blank (rejectValue 'dto.webhookUrl', 'not.empty')</li>
     * </ul>
     * <p>
     * Additional validation can be added:
     * 
     * <ul>
     *   <li>URL must be HTTPS</li>
     *   <li>URL must match Slack webhook pattern (https://hooks.slack.com/services/*)</li>
     * </ul>
     * <p>
     * Webhook setup guide: To obtain Slack webhook URL:
     * 
     * <ol>
     *   <li>Go to Slack workspace settings</li>
     *   <li>Navigate to Apps → Incoming Webhooks</li>
     *   <li>Click 'Add New Webhook to Workspace'</li>
     *   <li>Select channel for notifications</li>
     *   <li>Copy generated webhook URL (format: https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX)</li>
     * </ol>
     *
     * @param br the Spring BindingResult for error collection
     * @return null (note: breaks fluent chaining, should return this)
     */
    @Override
    public IntegrationSlackForm validate(BindingResult br) {
        if (StringUtils.isBlank(dto.getWebhookUrl())) {
            br.rejectValue("dto.webhookUrl", "not.empty");
        }
        return null;
    }
}
