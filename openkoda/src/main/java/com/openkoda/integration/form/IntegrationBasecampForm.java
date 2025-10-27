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
import com.openkoda.integration.model.dto.IntegrationBasecampDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Form adapter for Basecamp integration configuration.
 * <p>
 * Handles form binding, validation, and entity mapping for Basecamp todo list URL configuration.
 * Extends {@code AbstractEntityForm<IntegrationBasecampDto, IntegrationModuleOrganizationConfiguration>}
 * and implements the FrontendMappingDefinition lifecycle: populateFrom(entity) → validate(BindingResult) → populateTo(entity).
 * </p>
 * <p>
 * This form parses Basecamp URLs to extract accountId, projectId, and todolistId components using regex
 * pattern matching and StringUtils substring operations. The expected URL format is:
 * {@code https://3.basecamp.com/{accountId}/buckets/{projectId}/todolists/{todolistId}}.
 * </p>
 * <p>
 * Form lifecycle:
 * <ol>
 * <li>populateFrom(entity): Loads toDoListUrl from IntegrationModuleOrganizationConfiguration into DTO</li>
 * <li>validate(BindingResult): Validates URL format, checks that account/project/todolist IDs are present</li>
 * <li>populateTo(entity): Extracts IDs from URL using private Function lambdas, stores parsed components</li>
 * </ol>
 * </p>
 * <p>
 * Validation error codes:
 * <ul>
 * <li>not.empty - URL is required and cannot be blank</li>
 * <li>not.valid - URL does not match expected Basecamp format</li>
 * </ul>
 * </p>
 * <p>
 * Note: The validate() method returns null instead of this, which breaks fluent chaining
 * expected by some callers.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class IntegrationBasecampForm extends AbstractEntityForm<IntegrationBasecampDto, IntegrationModuleOrganizationConfiguration>
        implements LoggingComponentWithRequestId {
    
    /**
     * Creates form with new IntegrationBasecampDto and basecampConfigurationForm mapping definition.
     */
    public IntegrationBasecampForm() {
        super(new IntegrationBasecampDto(), null, IntegrationFrontendMappingDefinitions.basecampConfigurationForm);
    }

    /**
     * Creates form with provided DTO and entity.
     *
     * @param dto the Basecamp integration DTO
     * @param entity the integration configuration entity
     */
    public IntegrationBasecampForm(IntegrationBasecampDto dto, IntegrationModuleOrganizationConfiguration entity) {
        super(dto, entity, IntegrationFrontendMappingDefinitions.basecampConfigurationForm);
    }

    /**
     * Creates form with custom frontend mapping definition.
     *
     * @param formDef custom frontend mapping definition to override default
     */
    public IntegrationBasecampForm(FrontendMappingDefinition formDef) {
        super(new IntegrationBasecampDto(), null, formDef);
    }

    /**
     * Creates form with provided DTO, entity, and custom mapping definition.
     *
     * @param dto the Basecamp integration DTO
     * @param entity the integration configuration entity
     * @param formDef the frontend mapping definition
     */
    public IntegrationBasecampForm(IntegrationBasecampDto dto, IntegrationModuleOrganizationConfiguration entity, FrontendMappingDefinition formDef) {
        super(dto, entity, formDef);
    }

    /**
     * Populates form from entity for edit views.
     * <p>
     * Loads basecampToDoListUrl from IntegrationModuleOrganizationConfiguration into DTO
     * for form pre-population.
     * </p>
     *
     * @param entity the IntegrationModuleOrganizationConfiguration to load from
     * @return this form instance for fluent chaining
     */
    @Override
    public IntegrationBasecampForm populateFrom(IntegrationModuleOrganizationConfiguration entity) {
        dto.setToDoListUrl(entity.getBasecampToDoListUrl());
        return this;
    }

    /**
     * Populates entity from form after validation.
     * <p>
     * Extracts accountId, projectId, and toDoListId from URL using private Function lambdas
     * (accountIdFromUrl, projectIdFromUrl, toDoListIdFromUrl). Stores parsed components and
     * raw URL to entity.
     * </p>
     *
     * @param entity the IntegrationModuleOrganizationConfiguration to populate
     * @return the populated entity
     */
    @Override
    protected IntegrationModuleOrganizationConfiguration populateTo(IntegrationModuleOrganizationConfiguration entity) {
        entity.setBasecampAccountId(getSafeValue(entity.getBasecampToDoListUrl(), TODO_LIST_URL_, accountIdFromUrl));
        entity.setBasecampProjectId(getSafeValue(entity.getBasecampToDoListUrl(), TODO_LIST_URL_, projectIdFromUrl));
        entity.setBasecampToDoListId(getSafeValue(entity.getBasecampToDoListUrl(), TODO_LIST_URL_, toDoListIdFromUrl));
        entity.setBasecampToDoListUrl(dto.getToDoListUrl());
        return entity;
    }

    /**
     * Validates form data and registers errors.
     * <p>
     * Validates toDoListUrl is not blank (rejectValue 'not.empty'). Validates URL matches
     * pattern: {@code ^(https?|ftp|file)://3.basecamp.com/[0-9]*/buckets/[0-9]*/todolists/[0-9]*}
     * (rejectValue 'not.valid').
     * </p>
     * <p>
     * Note: This method returns null instead of this, which breaks fluent chaining expected
     * by some callers.
     * </p>
     *
     * @param br the Spring BindingResult for error collection
     * @return null (note: should return this for fluent chaining)
     */
    @Override
    public IntegrationBasecampForm validate(BindingResult br) {
        if (StringUtils.isBlank(dto.getToDoListUrl())) {
            br.rejectValue("dto.toDoListUrl", "not.empty");
        }
        Pattern pattern = Pattern.compile("^(https?|ftp|file):\\/\\/3.basecamp.com\\/[0-9]*\\/buckets\\/[0-9]*\\/todolists\\/[0-9]*");
        if (!pattern.matcher(dto.getToDoListUrl()).matches()) {
            br.rejectValue("dto.toDoListUrl", "not.valid");
        }
        return null;
    }

    /**
     * Lambda function to extract Basecamp account ID from URL.
     * <p>
     * Uses StringUtils.substringBetween(url, ".com/", "/") to extract the account ID portion.
     * </p>
     */
    private Function <String, String> accountIdFromUrl = ((String s) -> StringUtils.substringBetween(dto.getToDoListUrl(), ".com/", "/"));
    
    /**
     * Lambda function to extract project ID from URL.
     * <p>
     * Uses StringUtils.substringBetween(url, "buckets/", "/") to extract the project ID portion.
     * </p>
     */
    private Function <String, String> projectIdFromUrl = ((String s) -> StringUtils.substringBetween(dto.getToDoListUrl(), "buckets/", "/"));
    
    /**
     * Lambda function to extract todolist ID from URL.
     * <p>
     * Uses StringUtils.substringAfterLast(url, "todolists/") to extract the todolist ID portion.
     * </p>
     */
    private Function <String, String> toDoListIdFromUrl = ((String s) -> StringUtils.substringAfterLast(dto.getToDoListUrl(), "todolists/"));
}
