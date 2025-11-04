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
import com.openkoda.integration.model.dto.IntegrationTrelloDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;

/**
 * Form adapter for Trello integration configuration.
 * <p>
 * Handles form binding and validation for Trello API key and token with strict format requirements.
 * Extends {@code AbstractEntityForm<IntegrationTrelloDto, IntegrationModuleOrganizationConfiguration>}
 * and implements FrontendMappingDefinition lifecycle for seamless Spring MVC integration.
 * 
 * <p>
 * <b>Form Lifecycle:</b>
 * 
 * <ol>
 * <li><b>populateFrom(entity)</b>: Loads trelloApiKey, trelloApiToken, trelloBoardName, and trelloListName
 * from IntegrationModuleOrganizationConfiguration into DTO for form pre-population in edit views</li>
 * <li><b>validate(BindingResult)</b>: Enforces exact length and pattern requirements for Trello credentials,
 * adds field errors to BindingResult if validation fails</li>
 * <li><b>populateTo(entity)</b>: Stores validated credentials to entity using getSafeValue with
 * TRELLO_API_KEY_, TRELLO_API_TOKEN_, TRELLO_BOARD_NAME_, and TRELLO_LIST_NAME_ mapping keys</li>
 * </ol>
 * <p>
 * <b>Validation Rules (Trello-Specific Requirements):</b>
 * 
 * <ul>
 * <li><b>trelloApiKey</b>: Required (not blank), must be exactly 32 characters, hexadecimal pattern [0-9a-z]{32}
 * (lowercase alphanumeric via LOWER_CASE_ALPHANUMERIC_REGEX)</li>
 * <li><b>trelloApiToken</b>: Required (not blank), must be exactly 64 characters, hexadecimal pattern [0-9a-z]{64}
 * (lowercase alphanumeric)</li>
 * <li><b>trelloBoardName</b>: Required (not blank)</li>
 * <li><b>trelloListName</b>: Required (not blank)</li>
 * </ul>
 * <p>
 * <b>Error Codes:</b>
 * 
 * <ul>
 * <li><b>not.empty</b>: Field is required and cannot be blank</li>
 * <li><b>is.alphanumeric</b>: Value must match lowercase alphanumeric pattern [0-9a-z]*</li>
 * <li><b>wrong.size</b>: Value must be exactly 32 characters (API key) or 64 characters (token)</li>
 * </ul>
 * <p>
 * <b>Credential Source:</b> Trello uses long-lived API keys and tokens (no OAuth refresh).
 * Obtain credentials from https://trello.com/app-key. Generate token by clicking 'Token' link on that page.
 * 
 * <p>
 * <b>Credential Setup Guide:</b>
 * 
 * <ol>
 * <li>Visit https://trello.com/app-key</li>
 * <li>Copy the API key (32-character hexadecimal string)</li>
 * <li>Click 'Token' link to generate token</li>
 * <li>Authorize app access</li>
 * <li>Copy generated token (64-character hexadecimal string)</li>
 * <li>Enter both credentials in this form along with target board and list names</li>
 * </ol>
 * <p>
 * Integration with Spring MVC controllers uses @Valid annotation with BindingResult to capture validation errors.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractEntityForm
 * @see IntegrationTrelloDto
 * @see IntegrationModuleOrganizationConfiguration
 * @see IntegrationFrontendMappingDefinitions#trelloConfigurationForm
 */
public class IntegrationTrelloForm extends AbstractEntityForm<IntegrationTrelloDto, IntegrationModuleOrganizationConfiguration>
        implements LoggingComponentWithRequestId {

    /**
     * Regex pattern for lowercase alphanumeric validation: [0-9a-z]*.
     * <p>
     * Used to validate Trello API key (32 chars) and token (64 chars) are hexadecimal strings
     * containing only lowercase letters (a-z) and digits (0-9).
     * 
     */
    public static final String LOWER_CASE_ALPHANUMERIC_REGEX = "[0-9a-z]*";

    /**
     * Creates form with new IntegrationTrelloDto and trelloConfigurationForm mapping definition.
     * <p>
     * Initializes the form with an empty DTO and the Trello frontend mapping definition from
     * IntegrationFrontendMappingDefinitions. Used for creating new Trello integration configurations.
     * 
     */
    public IntegrationTrelloForm() {
        super(new IntegrationTrelloDto(), null, IntegrationFrontendMappingDefinitions.trelloConfigurationForm);
    }

    /**
     * Creates form with provided Trello integration DTO and configuration entity.
     * <p>
     * Used for editing existing Trello integration configurations. The DTO contains form field values
     * and the entity represents the persistent configuration.
     * 
     *
     * @param dto the Trello integration DTO containing form field values
     * @param entity the integration configuration entity to populate or update
     */
    public IntegrationTrelloForm(IntegrationTrelloDto dto, IntegrationModuleOrganizationConfiguration entity) {
        super(dto, entity, IntegrationFrontendMappingDefinitions.trelloConfigurationForm);
    }

    /**
     * Loads Trello configuration from entity into DTO for form pre-population.
     * <p>
     * Copies trelloApiKey, trelloApiToken, trelloBoardName, and trelloListName from the
     * IntegrationModuleOrganizationConfiguration entity into the form's DTO. This method is called
     * when editing existing Trello integration configurations to display current values in the form.
     * 
     *
     * @param entity the IntegrationModuleOrganizationConfiguration to load from
     * @return this form instance for fluent chaining
     */
    @Override
    public IntegrationTrelloForm populateFrom(IntegrationModuleOrganizationConfiguration entity) {
        dto.setTrelloApiKey(entity.getTrelloApiKey());
        dto.setTrelloApiToken(entity.getTrelloApiToken());
        dto.setTrelloBoardName(entity.getTrelloBoardName());
        dto.setTrelloListName(entity.getTrelloListName());
        return this;
    }

    /**
     * Stores validated Trello configuration from DTO to entity.
     * <p>
     * Transfers API key, token, board name, and list name from the form's DTO to the entity using
     * getSafeValue for null-safe field access. Uses frontend mapping keys TRELLO_API_KEY_,
     * TRELLO_API_TOKEN_, TRELLO_BOARD_NAME_, and TRELLO_LIST_NAME_ from IntegrationFrontendMappingDefinitions.
     * This method is called after successful validation to persist the configuration.
     * 
     *
     * @param entity the IntegrationModuleOrganizationConfiguration to populate
     * @return the populated entity for fluent chaining
     */
    @Override
    protected IntegrationModuleOrganizationConfiguration populateTo(IntegrationModuleOrganizationConfiguration entity) {
        entity.setTrelloApiKey(getSafeValue(entity.getTrelloApiKey(), TRELLO_API_KEY_));
        entity.setTrelloApiToken(getSafeValue(entity.getTrelloApiToken(), TRELLO_API_TOKEN_));
        entity.setTrelloBoardName(getSafeValue(entity.getTrelloBoardName(), TRELLO_BOARD_NAME_));
        entity.setTrelloListName(getSafeValue(entity.getTrelloListName(), TRELLO_LIST_NAME_));
        return entity;
    }

    /**
     * Performs strict validation of Trello credentials and configuration.
     * <p>
     * Validates all Trello configuration fields with specific constraints enforced by Trello's API.
     * Validation errors are added to the BindingResult for display in the form UI.
     * 
     * <p>
     * <b>Validation Checks Performed:</b>
     * 
     * <ol>
     * <li>trelloApiKey not blank (rejectValue 'dto.trelloApiKey', 'not.empty')</li>
     * <li>trelloApiKey matches LOWER_CASE_ALPHANUMERIC_REGEX (rejectValue 'dto.trelloApiKey', 'is.alphanumeric')</li>
     * <li>trelloApiKey length exactly 32 characters (rejectValue 'dto.trelloApiKey', 'wrong.size')</li>
     * <li>trelloApiToken not blank (rejectValue 'dto.trelloApiToken', 'not.empty')</li>
     * <li>trelloApiToken matches LOWER_CASE_ALPHANUMERIC_REGEX (rejectValue 'dto.trelloApiToken', 'is.alphanumeric')</li>
     * <li>trelloApiToken length exactly 64 characters (rejectValue 'dto.trelloApiToken', 'wrong.size')</li>
     * <li>trelloBoardName not blank (rejectValue 'dto.trelloBoardName', 'not.empty')</li>
     * <li>trelloListName not blank (rejectValue 'dto.trelloListName', 'not.empty')</li>
     * </ol>
     * <p>
     * The strict 32-character and 64-character length requirements match Trello's API key and token formats.
     * Both must be lowercase hexadecimal strings (0-9, a-z).
     * 
     *
     * @param br the Spring BindingResult for error collection
     * @return this form instance for fluent chaining
     */
    @Override
    public IntegrationTrelloForm validate(BindingResult br) {
        if (StringUtils.isBlank(dto.getTrelloApiKey())) {
            br.rejectValue("dto.trelloApiKey", "not.empty");
        }
        if (!dto.getTrelloApiKey().matches(LOWER_CASE_ALPHANUMERIC_REGEX)) {
            br.rejectValue("dto.trelloApiKey", "is.alphanumeric");
        }
        if (dto.getTrelloApiKey().length() != 32) {
            br.rejectValue("dto.trelloApiKey", "wrong.size");
        }
        if (StringUtils.isBlank(dto.getTrelloApiToken())) {
            br.rejectValue("dto.trelloApiToken", "not.empty");
        }
        if (!dto.getTrelloApiToken().matches(LOWER_CASE_ALPHANUMERIC_REGEX)) {
            br.rejectValue("dto.trelloApiToken", "is.alphanumeric");
        }
        if (dto.getTrelloApiToken().length() != 64) {
            br.rejectValue("dto.trelloApiToken", "wrong.size");
        }
        if (StringUtils.isBlank(dto.getTrelloBoardName())) {
            br.rejectValue("dto.trelloBoardName", "not.empty");
        }
        if (StringUtils.isBlank(dto.getTrelloListName())) {
            br.rejectValue("dto.trelloListName", "not.empty");
        }
        return this;
    }
}
