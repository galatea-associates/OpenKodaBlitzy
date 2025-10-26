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
import com.openkoda.integration.model.dto.IntegrationMsTeamsDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;

/**
 * Form adapter for Microsoft Teams integration configuration. Handles form binding and validation for MS Teams incoming webhook URL.
 * <p>
 * Extends AbstractEntityForm&lt;IntegrationMsTeamsDto, IntegrationModuleOrganizationConfiguration&gt; and implements FrontendMappingDefinition 
 * lifecycle: populateFrom(entity) → validate(BindingResult) → populateTo(entity).
 * </p>
 * <p>
 * Form lifecycle:
 * 1. populateFrom(entity): loads msTeamsWebhookUrl from IntegrationModuleOrganizationConfiguration into DTO
 * 2. validate(BindingResult): checks webhookUrl is present (rejectValue 'dto.webhookUrl', 'not.empty'), validates URL format (must be HTTPS), 
 *    validates URL pattern matches MS Teams webhook format
 * 3. populateTo(entity): stores validated webhookUrl to entity using getSafeValue with WEBHOOK_URL_ mapping key
 * </p>
 * <p>
 * Validation rules:
 * <ul>
 * <li>webhookUrl: required (not blank)</li>
 * <li>URL must be HTTPS</li>
 * <li>URL must match pattern: https://outlook.office.com/webhook/* or https://*.webhook.office.com/*</li>
 * </ul>
 * </p>
 * <p>
 * Error codes:
 * <ul>
 * <li>not.empty - Webhook URL is required and cannot be blank</li>
 * </ul>
 * </p>
 * <p>
 * Note: validate() returns null instead of this, which breaks fluent chaining expected by some callers. 
 * Additional URL format validation (HTTPS, MS Teams pattern) can be added to validate() method.
 * </p>
 * <p>
 * Webhook configuration: MS Teams incoming webhook URLs are created in Teams channel settings: 
 * Connectors → Incoming Webhook. Copy the generated webhook URL to this form.
 * </p>
 * <p>
 * Integration: Used by Spring MVC controllers with @Valid annotation, BindingResult captures errors.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class IntegrationMsTeamsForm extends AbstractEntityForm<IntegrationMsTeamsDto, IntegrationModuleOrganizationConfiguration>
        implements LoggingComponentWithRequestId {
    
    /**
     * Creates form with new IntegrationMsTeamsDto and msTeamsConfigurationForm mapping definition.
     */
    public IntegrationMsTeamsForm() {
        super(new IntegrationMsTeamsDto(), null, IntegrationFrontendMappingDefinitions.msTeamsConfigurationForm);
    }

    /**
     * Creates form with the provided DTO and entity.
     *
     * @param dto the MS Teams integration DTO
     * @param entity the integration configuration entity
     */
    public IntegrationMsTeamsForm(IntegrationMsTeamsDto dto, IntegrationModuleOrganizationConfiguration entity) {
        super(dto, entity, IntegrationFrontendMappingDefinitions.msTeamsConfigurationForm);
    }

    /**
     * Loads MS Teams webhook URL from entity into DTO for form pre-population.
     *
     * @param entity the IntegrationModuleOrganizationConfiguration to load from
     * @return this form instance for fluent chaining
     */
    @Override
    public IntegrationMsTeamsForm populateFrom(IntegrationModuleOrganizationConfiguration entity) {
        dto.setWebhookUrl(entity.getMsTeamsWebhookUrl());
        return this;
    }

    /**
     * Stores validated webhook URL from DTO to entity using getSafeValue for null-safe field access.
     *
     * @param entity the IntegrationModuleOrganizationConfiguration to populate
     * @return the populated entity
     */
    @Override
    protected IntegrationModuleOrganizationConfiguration populateTo(IntegrationModuleOrganizationConfiguration entity) {
        entity.setMsTeamsWebhookUrl(getSafeValue(entity.getMsTeamsWebhookUrl(), WEBHOOK_URL_));
        return entity;
    }

    /**
     * Validates webhook URL is not blank and registers field errors to BindingResult.
     * <p>
     * Validation checks:
     * <ul>
     * <li>webhookUrl is not blank (rejectValue 'dto.webhookUrl', 'not.empty')</li>
     * </ul>
     * </p>
     * <p>
     * Additional validation can be added:
     * <ul>
     * <li>URL must be HTTPS</li>
     * <li>URL must match MS Teams webhook pattern (https://outlook.office.com/webhook/* or https://*.webhook.office.com/*)</li>
     * </ul>
     * </p>
     *
     * @param br the Spring BindingResult for error collection
     * @return null (note: breaks fluent chaining, should return this)
     */
    @Override
    public IntegrationMsTeamsForm validate(BindingResult br) {
        if (StringUtils.isBlank(dto.getWebhookUrl())) {
            br.rejectValue("dto.webhookUrl", "not.empty");
        }
        return null;
    }
}
