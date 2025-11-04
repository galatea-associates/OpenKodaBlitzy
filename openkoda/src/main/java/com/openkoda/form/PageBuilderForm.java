/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 and associated documentation files (the "Software"), to deal in the Software without restriction, 
including without limitation the rights to use, copy, modify, merge, publish, distribute, 
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software 
is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice 
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE 
AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS 
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES 
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.form;

import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.dto.system.FrontendResourceDto;
import com.openkoda.model.component.FrontendResource;
import org.springframework.validation.BindingResult;

import static com.openkoda.controller.common.URLConstants.FRONTENDRESOURCEREGEX;
import static com.openkoda.core.form.FrontendMappingDefinition.createFrontendMappingDefinition;
import static com.openkoda.form.FrontendMappingDefinitions.PAGE_BUILDER_FORM;
import static com.openkoda.model.Privilege.manageFrontendResource;
import static com.openkoda.model.Privilege.readFrontendResource;

/**
 * Organization-scoped page builder form for managing dashboard page configurations.
 * <p>
 * This form performs entity-to-DTO conversion for page builder entities (FrontendResource type JSON/DASHBOARD).
 * It maps page structure and configuration fields including name and content, and extends
 * {@link AbstractOrganizationRelatedEntityForm} to provide organization-aware form lifecycle operations
 * (populateFrom, validate, populateTo).

 * <p>
 * The form validates page builder requirements such as name format (matching {@code FRONTENDRESOURCEREGEX}),
 * structure integrity, and required fields. It handles draft vs. published content retrieval and enforces
 * read and manage privilege checks via the {@code pageBuilderForm} mapping definition.

 * <p>
 * Example usage:
 * <pre>{@code
 * PageBuilderForm form = new PageBuilderForm(orgId, entity);
 * form.populateFrom(entity).validate(bindingResult);
 * }</pre>

 *
 * @param <CD> the data transfer object type extending {@link FrontendResourceDto}
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2019-02-19
 * @see AbstractOrganizationRelatedEntityForm
 * @see FrontendResourceDto
 * @see FrontendResource
 */
public class PageBuilderForm<CD extends FrontendResourceDto> extends AbstractOrganizationRelatedEntityForm<CD, FrontendResource> {

    /**
     * Frontend mapping definition for page builder forms.
     * <p>
     * Defines text fields for name and content with validation requiring name to match
     * {@code FRONTENDRESOURCEREGEX} pattern. Enforces read and manage frontend resource privileges.

     */
    public final static FrontendMappingDefinition pageBuilderForm = createFrontendMappingDefinition(PAGE_BUILDER_FORM, readFrontendResource, manageFrontendResource,
            a -> a  .text(NAME_)                    .validate(v -> v.matches(FRONTENDRESOURCEREGEX) ? null : "not.matching.name")
                    .text(CONTENT_)

    );


    /**
     * Constructs a new page builder form with default DTO and mapping definition.
     * <p>
     * Creates a form with no organization context or entity, using a new {@link FrontendResourceDto}
     * instance and the default {@link #pageBuilderForm} mapping.

     */
    public PageBuilderForm() {
        super(null, (CD)new FrontendResourceDto(), null, pageBuilderForm);
    }

    /**
     * Constructs a new page builder form with a custom mapping definition.
     * <p>
     * Creates a form using the provided mapping definition instead of the default {@link #pageBuilderForm}.
     * Useful for specialized page builder forms with custom field configurations or validation rules.

     *
     * @param frontendMappingDefinition the custom mapping definition for form fields and validation
     */
    public PageBuilderForm(FrontendMappingDefinition frontendMappingDefinition) {
        super(frontendMappingDefinition);
    }

    /**
     * Constructs a new page builder form with a pre-populated DTO.
     * <p>
     * Creates a form with the provided DTO containing form data, but no organization context or entity.
     * Uses the default {@link #pageBuilderForm} mapping definition.

     *
     * @param dto the data transfer object containing form field values
     */
    public PageBuilderForm(CD dto) {
        super(null, dto, null, pageBuilderForm);
    }

    /**
     * Constructs an organization-scoped page builder form for an existing entity.
     * <p>
     * Creates a form bound to a specific organization and frontend resource entity, using the default
     * {@link #pageBuilderForm} mapping. The form's DTO will be populated via {@link #populateFrom(FrontendResource)}.

     *
     * @param organizationId the organization ID for tenant-aware operations (nullable)
     * @param entity the frontend resource entity to populate the form from
     */
    public PageBuilderForm(Long organizationId, FrontendResource entity) {
        this(organizationId, entity, pageBuilderForm);
    }

    /**
     * Constructs an organization-scoped page builder form with a custom mapping definition.
     * <p>
     * Creates a form bound to a specific organization and frontend resource entity, using the provided
     * custom mapping definition. Useful for specialized page builder forms with modified field configurations.

     *
     * @param organizationId the organization ID for tenant-aware operations (nullable)
     * @param entity the frontend resource entity to populate the form from
     * @param frontendMappingDefinition the custom mapping definition for form fields and validation
     */
    public PageBuilderForm(Long organizationId, FrontendResource entity, FrontendMappingDefinition frontendMappingDefinition) {
        super(organizationId, (CD)new FrontendResourceDto(), entity, frontendMappingDefinition);
    }

    /**
     * Constructs a fully-configured organization-scoped page builder form.
     * <p>
     * Creates a form with explicit organization context, pre-populated DTO, entity reference,
     * and custom mapping definition. This constructor provides maximum flexibility for
     * specialized form configurations and is the most explicit form initialization option.

     *
     * @param organizationId the organization ID for tenant-aware operations (nullable)
     * @param dto the data transfer object containing form field values
     * @param entity the frontend resource entity to populate the form from (nullable)
     * @param frontendMappingDefinition the custom mapping definition for form fields and validation
     */
    public PageBuilderForm(Long organizationId, CD dto, FrontendResource entity, FrontendMappingDefinition frontendMappingDefinition) {
        super(organizationId, dto, entity, frontendMappingDefinition);
    }

    /**
     * Transfers entity data to the form's DTO.
     * <p>
     * Populates the DTO with page configuration and content structure from the provided
     * {@link FrontendResource} entity. Retrieves the entity's name and selects either draft content
     * (if entity is in draft state) or published content for display/editing. This method is invoked
     * during form initialization to load existing entity data into the editable form representation.

     *
     * @param entity the frontend resource entity containing page configuration and content
     * @return this form instance for method chaining
     * @see #populateTo(FrontendResource)
     */
    @Override
    public PageBuilderForm populateFrom(FrontendResource entity) {
        debug("[populateFrom] {}", entity);
        dto.name = entity.getName();
        dto.content = entity.isDraft() ? entity.getDraftContent() : entity.getContent();
        return this;
    }

    /**
     * Applies validated DTO data to the entity.
     * <p>
     * Transfers validated form data from the DTO to the {@link FrontendResource} entity using
     * {@link #getSafeValue(Object, String)} to ensure safe value assignment. Sets page configuration
     * including name and content (null if blank), and enforces dashboard-specific properties:
     * type (JSON), not included in sitemap, not embeddable, no required privilege, and resource type DASHBOARD.
     * This method is invoked after successful validation to persist form changes to the entity.

     *
     * @param entity the frontend resource entity to update with validated form data
     * @return the updated entity instance
     * @see #populateFrom(FrontendResource)
     * @see #validate(BindingResult)
     */
    @Override
    protected FrontendResource populateTo(FrontendResource entity) {
        entity.setName(getSafeValue(entity.getName(), NAME_));
        entity.setContent(getSafeValue(entity.getContent(), CONTENT_, nullIfBlank));
        entity.setType(FrontendResource.Type.JSON);
        entity.setIncludeInSitemap(false);
        entity.setEmbeddable(false);
        entity.setRequiredPrivilege(null);
        entity.setResourceType(FrontendResource.ResourceType.DASHBOARD);
        return entity;
    }

    /**
     * Validates page builder requirements.
     * <p>
     * Performs validation of page builder structure, configuration, and required fields according to
     * the form's {@link FrontendMappingDefinition}. Name field validation (matching {@code FRONTENDRESOURCEREGEX})
     * is enforced by the mapping definition. Additional custom validation logic can be added here to
     * validate content structure integrity or business rules. Validation errors are recorded in the
     * provided {@link BindingResult} for display to the user.

     *
     * @param br the binding result object to record validation errors
     * @return this form instance for method chaining
     * @see #populateTo(FrontendResource)
     */
    @Override
    public PageBuilderForm validate(BindingResult br) {
        debug("[validate]");
        return this;
    }

}
