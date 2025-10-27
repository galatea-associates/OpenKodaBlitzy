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
import com.openkoda.integration.model.dto.IntegrationGitHubDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;

/**
 * Form adapter for GitHub integration configuration.
 * <p>
 * Handles form binding and validation for GitHub repository owner, name, and access token.
 * This form extends AbstractEntityForm&lt;IntegrationGitHubDto, IntegrationModuleOrganizationConfiguration&gt;
 * and implements the FrontendMappingDefinition lifecycle pattern for processing GitHub integration settings.
 * </p>
 * 
 * <h3>Form Lifecycle</h3>
 * <p>
 * This form follows a three-stage lifecycle:
 * </p>
 * <ol>
 * <li><b>populateFrom(entity)</b>: Loads gitHubRepoOwner and gitHubRepoName from 
 * IntegrationModuleOrganizationConfiguration into the DTO for form pre-population in edit views</li>
 * <li><b>validate(BindingResult)</b>: Validates that repository owner and name are not empty, 
 * checks format constraints (alphanumeric with hyphens), and adds field errors to BindingResult</li>
 * <li><b>populateTo(entity)</b>: Stores validated values back to entity using getSafeValue 
 * with GITHUB_REPO_NAME_ and GITHUB_REPO_OWNER_ mapping keys for null-safe field access</li>
 * </ol>
 * 
 * <h3>Validation Rules</h3>
 * <ul>
 * <li><b>repoOwner</b>: Required, alphanumeric with hyphens, max 39 chars (GitHub username constraints)</li>
 * <li><b>repoName</b>: Required, alphanumeric with hyphens/underscores, max 100 chars</li>
 * <li><b>accessToken</b>: Optional (can be set via OAuth), if provided validate format</li>
 * </ul>
 * 
 * <h3>Error Codes</h3>
 * <ul>
 * <li><b>not.empty</b>: Field is required and cannot be blank</li>
 * </ul>
 * 
 * <h3>Integration</h3>
 * <p>
 * This form is used by Spring MVC controllers with the @Valid annotation to trigger validation.
 * BindingResult captures errors for form rendering in Thymeleaf templates.
 * </p>
 * 
 * <h3>Example Usage</h3>
 * <pre>
 * IntegrationGitHubForm form = new IntegrationGitHubForm(dto, entity);
 * form.populateFrom(entity);
 * // Process form submission
 * form.validate(bindingResult);
 * if (!bindingResult.hasErrors()) {
 *     form.populateTo(entity);
 * }
 * </pre>
 * 
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see IntegrationGitHubDto
 * @see IntegrationModuleOrganizationConfiguration
 * @see IntegrationFrontendMappingDefinitions#gitHubConfigurationForm
 */
public class IntegrationGitHubForm extends AbstractEntityForm<IntegrationGitHubDto, IntegrationModuleOrganizationConfiguration>
        implements LoggingComponentWithRequestId {
    
    /**
     * Creates a new GitHub integration form with default settings.
     * <p>
     * Initializes the form with a new IntegrationGitHubDto and the default
     * gitHubConfigurationForm mapping definition. The entity is set to null
     * and should be provided later via populateFrom or constructor injection.
     * </p>
     */
    public IntegrationGitHubForm() {
        super(new IntegrationGitHubDto(), null, IntegrationFrontendMappingDefinitions.gitHubConfigurationForm);
    }

    /**
     * Creates a new GitHub integration form with the specified DTO and entity.
     * <p>
     * This constructor is typically used when editing an existing GitHub integration
     * configuration. The form is initialized with the default gitHubConfigurationForm
     * mapping definition.
     * </p>
     * 
     * @param dto the GitHub integration DTO containing form field values
     * @param entity the integration configuration entity to bind to
     */
    public IntegrationGitHubForm(IntegrationGitHubDto dto, IntegrationModuleOrganizationConfiguration entity) {
        super(dto, entity, IntegrationFrontendMappingDefinitions.gitHubConfigurationForm);
    }

    /**
     * Creates a new GitHub integration form with a custom mapping definition.
     * <p>
     * This constructor allows overriding the default gitHubConfigurationForm mapping
     * with a custom FrontendMappingDefinition. This is useful for creating read-only
     * or disabled form variants.
     * </p>
     * 
     * @param formDef the custom frontend mapping definition to use instead of the default
     */
    public IntegrationGitHubForm(FrontendMappingDefinition formDef) {
        super(new IntegrationGitHubDto(), null, formDef);
    }

    /**
     * Creates a new GitHub integration form with all parameters specified.
     * <p>
     * This is the most flexible constructor, allowing complete control over the DTO,
     * entity, and mapping definition. Useful for advanced scenarios requiring custom
     * configuration.
     * </p>
     * 
     * @param dto the GitHub integration DTO containing form field values
     * @param entity the integration configuration entity to bind to
     * @param formDef the custom frontend mapping definition to use
     */
    public IntegrationGitHubForm(IntegrationGitHubDto dto, IntegrationModuleOrganizationConfiguration entity, FrontendMappingDefinition formDef) {
        super(dto, entity, formDef);
    }

    /**
     * Populates the form DTO from the integration configuration entity.
     * <p>
     * Loads gitHubRepoName and gitHubRepoOwner from the IntegrationModuleOrganizationConfiguration
     * entity into the DTO for form pre-population in edit views. This method is called when
     * rendering an edit form to display existing configuration values.
     * </p>
     * 
     * @param entity the IntegrationModuleOrganizationConfiguration to load values from
     * @return this form instance for fluent method chaining
     */
    @Override
    public IntegrationGitHubForm populateFrom(IntegrationModuleOrganizationConfiguration entity) {
        dto.setGitHubRepoName(entity.getGitHubRepoName());
        dto.setGitHubRepoOwner(entity.getGitHubRepoOwner());
        return this;
    }

    /**
     * Populates the integration configuration entity from the form DTO.
     * <p>
     * Stores validated repository name and owner from the DTO to the entity using getSafeValue
     * for null-safe field access. The getSafeValue method uses GITHUB_REPO_NAME_ and 
     * GITHUB_REPO_OWNER_ frontend mapping keys to retrieve values from the form.
     * This method is called after successful validation to persist changes.
     * </p>
     * 
     * @param entity the IntegrationModuleOrganizationConfiguration to populate with form values
     * @return the populated entity for further processing or persistence
     */
    @Override
    protected IntegrationModuleOrganizationConfiguration populateTo(IntegrationModuleOrganizationConfiguration entity) {
        entity.setGitHubRepoName(getSafeValue(entity.getGitHubRepoName(), GITHUB_REPO_NAME_));
        entity.setGitHubRepoOwner(getSafeValue(entity.getGitHubRepoOwner(), GITHUB_REPO_OWNER_));
        return entity;
    }

    /**
     * Validates the GitHub integration form fields.
     * <p>
     * Performs validation on the repository name and owner fields to ensure they are not blank.
     * Validation errors are registered on the BindingResult with the error code "not.empty"
     * for display in the form UI.
     * </p>
     * 
     * <h4>Validation Rules:</h4>
     * <ul>
     * <li>gitHubRepoName: Must not be blank (rejectValue on 'dto.gitHubRepoName' with error code 'not.empty')</li>
     * <li>gitHubRepoOwner: Must not be blank (rejectValue on 'dto.gitHubRepoOwner' with error code 'not.empty')</li>
     * </ul>
     * 
     * <p>
     * <b>Note:</b> Additional format validation can be added if needed, such as:
     * alphanumeric pattern matching, length constraints (max 39 chars for owner per GitHub username rules,
     * max 100 chars for repository name), and special character validation (hyphens/underscores allowed).
     * </p>
     * 
     * @param br the Spring BindingResult for collecting validation errors
     * @return this form instance for fluent method chaining
     */
    @Override
    public IntegrationGitHubForm validate(BindingResult br) {
        if (StringUtils.isBlank(dto.getGitHubRepoName())) {
            br.rejectValue("dto.gitHubRepoName", "not.empty");
        }
        if (StringUtils.isBlank(dto.getGitHubRepoOwner())) {
            br.rejectValue("dto.gitHubRepoOwner", "not.empty");
        }
        return this;
    }
}
