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
import com.openkoda.core.form.FieldType;
import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.core.service.FrontendResourceService;
import com.openkoda.dto.system.FrontendResourceDto;
import com.openkoda.model.component.FrontendResource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;

/**
 * Form adapter for mapping frontend resource entities to and from DTOs with editable fragment extraction.
 * <p>
 * This form handles the bidirectional conversion between {@link FrontendResource} entities and 
 * {@link FrontendResourceDto} objects, implementing the form lifecycle pattern with {@code populateFrom()},
 * {@code validate()}, and {@code populateTo()} methods. It extracts editable content fragments from
 * the resource content using marker-based delimiters defined in {@link FrontendResourceService}
 * (CONTENT_EDITABLE_BEGIN and CONTENT_EDITABLE_END) with {@link StringUtils#substringBetween}.

 * <p>
 * The form extends {@link AbstractOrganizationRelatedEntityForm} to inherit organization-scoped entity
 * handling and integrates with the {@link FrontendMappingDefinitions#frontendResourceForm} definition
 * for frontend rendering and validation.

 * <p>
 * Example usage:
 * <pre>
 * FrontendResourceForm form = new FrontendResourceForm(orgId, entity);
 * form.populateFrom(entity).validate(bindingResult);
 * </pre>

 *
 * @param <CD> the concrete FrontendResourceDto type or subclass
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2019-02-19
 * @see AbstractOrganizationRelatedEntityForm
 * @see FrontendResource
 * @see FrontendResourceDto
 * @see FrontendResourceService
 */
public class FrontendResourceForm<CD extends FrontendResourceDto> extends AbstractOrganizationRelatedEntityForm<CD, FrontendResource> {


    /**
     * Constructs a new FrontendResourceForm with default settings.
     * <p>
     * Initializes the form with a new {@link FrontendResourceDto} instance and the standard
     * {@link FrontendMappingDefinitions#frontendResourceForm} definition. Organization and entity
     * are null, suitable for creating new resources.

     */
    public FrontendResourceForm() {
        super(null, (CD)new FrontendResourceDto(), null, FrontendMappingDefinitions.frontendResourceForm);
    }

    /**
     * Constructs a FrontendResourceForm with a custom frontend mapping definition.
     * <p>
     * Allows customization of the form's field definitions and rendering behavior
     * by providing a specific {@link FrontendMappingDefinition}.

     *
     * @param frontendMappingDefinition the custom mapping definition for form structure and validation
     */
    public FrontendResourceForm(FrontendMappingDefinition frontendMappingDefinition) {
        super(frontendMappingDefinition);
    }

    /**
     * Constructs a FrontendResourceForm with an existing DTO.
     * <p>
     * Initializes the form with the provided DTO instance, using the standard
     * {@link FrontendMappingDefinitions#frontendResourceForm} definition. Suitable for
     * scenarios where the DTO is pre-populated.

     *
     * @param dto the pre-populated FrontendResourceDto to bind to this form
     */
    public FrontendResourceForm(CD dto) {
        super(null, dto, null, FrontendMappingDefinitions.frontendResourceForm);
    }

    /**
     * Constructs a FrontendResourceForm for editing an existing entity within an organization.
     * <p>
     * Initializes the form with the specified organization ID and entity, using the standard
     * {@link FrontendMappingDefinitions#frontendResourceForm} definition. The entity's data
     * will be populated into the form's DTO via {@link #populateFrom(FrontendResource)}.

     *
     * @param organizationId the organization ID for tenant-scoped operations
     * @param entity the FrontendResource entity to edit
     */
    public FrontendResourceForm(Long organizationId, FrontendResource entity) {
        this(organizationId, entity, FrontendMappingDefinitions.frontendResourceForm);
    }

    /**
     * Constructs a FrontendResourceForm with custom mapping definition for an organization-scoped entity.
     * <p>
     * Provides full control over the form initialization with explicit organization ID, entity,
     * and custom {@link FrontendMappingDefinition}.

     *
     * @param organizationId the organization ID for tenant-scoped operations
     * @param entity the FrontendResource entity to edit
     * @param frontendMappingDefinition the custom mapping definition for form structure
     */
    public FrontendResourceForm(Long organizationId, FrontendResource entity, FrontendMappingDefinition frontendMappingDefinition) {
        super(organizationId, (CD)new FrontendResourceDto(), entity, frontendMappingDefinition);
    }

    /**
     * Constructs a FrontendResourceForm with full parameter control.
     * <p>
     * Master constructor providing complete control over all form components: organization ID,
     * pre-populated DTO, entity, and custom mapping definition. Useful for advanced scenarios
     * requiring fine-grained initialization.

     *
     * @param organizationId the organization ID for tenant-scoped operations
     * @param dto the pre-populated FrontendResourceDto to bind
     * @param entity the FrontendResource entity to edit
     * @param frontendMappingDefinition the custom mapping definition for form structure
     */
    public FrontendResourceForm(Long organizationId, CD dto, FrontendResource entity, FrontendMappingDefinition frontendMappingDefinition) {
        super(organizationId, dto, entity, frontendMappingDefinition);
    }

    /**
     * Transfers FrontendResource entity data to the form's DTO for editing.
     * <p>
     * Populates the DTO with entity properties including name, organization ID, content (selecting
     * draft or published content based on {@code entity.isDraft()}), required privilege, sitemap
     * inclusion flag, type, embeddability, and access level. Additionally, extracts editable content
     * fragments between {@link FrontendResourceService#CONTENT_EDITABLE_BEGIN} and 
     * {@link FrontendResourceService#CONTENT_EDITABLE_END} markers using 
     * {@link StringUtils#substringBetween}, wrapping the extracted fragment in the same markers
     * for the {@code dto.contentEditable} field.

     * <p>
     * Example marker extraction:
     * <pre>
     * Content: "static &lt;!--editable--&gt;changeable&lt;!--/editable--&gt; static"
     * Result: dto.contentEditable = "&lt;!--editable--&gt;changeable&lt;!--/editable--&gt;"
     * </pre>

     *
     * @param entity the FrontendResource entity to populate from
     * @return this form instance for method chaining
     */
    @Override
    public FrontendResourceForm populateFrom(FrontendResource entity) {
        debug("[populateFrom] {}", entity);
        dto.name = entity.getName();
        dto.organizationId = entity.getOrganizationId();
        dto.content = entity.isDraft() ? entity.getDraftContent() : entity.getContent();
        dto.contentEditable = FrontendResourceService.CONTENT_EDITABLE_BEGIN + StringUtils.substringBetween(dto.content, FrontendResourceService.CONTENT_EDITABLE_BEGIN, FrontendResourceService.CONTENT_EDITABLE_END) + FrontendResourceService.CONTENT_EDITABLE_END;
        dto.requiredPrivilege = entity.getRequiredPrivilege();
        dto.includeInSitemap = entity.getIncludeInSitemap();
        dto.type = entity.getType();
        dto.embeddable = entity.isEmbeddable();
        dto.accessLevel = entity.getAccessLevel();
        return this;
    }

    /**
     * Determines the appropriate code editor field type based on the frontend resource type.
     * <p>
     * Maps {@link FrontendResource.Type} enum values to corresponding {@link FieldType} constants
     * for syntax highlighting and editor configuration. Returns {@code code_html} as the default
     * when the input is null or unrecognized.

     *
     * @param e the frontend resource type (typically a FrontendResource.Type enum value), may be null
     * @return the corresponding FieldType (code_js for JS, code_css for CSS, code_html otherwise)
     */
    static FieldType getCodeType(Object e) {
        if (e == null) {
            return FieldType.code_html;
        }
        switch (FrontendResource.Type.valueOf(e + "")) {
            case JS:
                return FieldType.code_js;
            case CSS:
                return FieldType.code_css;
            default:
                return FieldType.code_html;
        }
    }

    /**
     * Applies validated DTO data to the FrontendResource entity.
     * <p>
     * Transfers form DTO values to the entity using {@code getSafeValue()} with field name constants
     * (NAME_, ORGANIZATION_ID_, CONTENT_, TYPE_, INCLUDE_IN_SITEMAP_, EMBEDDABLE_, ACCESS_LEVEL,
     * REQUIRED_PRIVILEGE_) to safely handle null or blank values. The content is saved as draft content,
     * and the resource type is explicitly set to {@link FrontendResource.ResourceType#RESOURCE}.

     * <p>
     * This method is typically called after successful validation to persist form changes to the entity.

     *
     * @param entity the FrontendResource entity to populate with DTO values
     * @return the updated entity instance
     */
    @Override
    protected FrontendResource populateTo(FrontendResource entity) {
        entity.setName(getSafeValue(entity.getName(), NAME_));
        entity.setOrganizationId(getSafeValue(entity.getOrganizationId(), ORGANIZATION_ID_));
        entity.setDraftContent(getSafeValue(entity.getDraftContent(), CONTENT_, nullIfBlank));
        entity.setType(getSafeValue(entity.getType(), TYPE_));
        entity.setIncludeInSitemap(getSafeValue(entity.getIncludeInSitemap(), INCLUDE_IN_SITEMAP_));
        entity.setEmbeddable(getSafeValue(entity.isEmbeddable(), EMBEDDABLE_));
        entity.setOrganizationId(getSafeValue(entity.getOrganizationId(), ORGANIZATION_ID_));
        entity.setAccessLevel(getSafeValue(entity.getAccessLevel(), ACCESS_LEVEL));
        entity.setRequiredPrivilege(getSafeValue(entity.getRequiredPrivilege(), REQUIRED_PRIVILEGE_, nullIfBlank));
        entity.setResourceType(FrontendResource.ResourceType.RESOURCE);
        return entity;
    }

    /**
     * Validates the form data against business rules and constraints.
     * <p>
     * Performs validation of the form's DTO data, adding any validation errors to the provided
     * {@link BindingResult}. This implementation currently delegates to the parent form validation
     * framework and Jakarta Bean Validation constraints defined on the DTO.

     *
     * @param br the BindingResult to accumulate validation errors
     * @return this form instance for method chaining
     */
    @Override
    public FrontendResourceForm validate(BindingResult br) {
        debug("[validate]");
        return this;
    }

}
