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

package com.openkoda.form;

import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.core.multitenancy.MultitenancyService;
import com.openkoda.dto.OrganizationDto;
import com.openkoda.model.Organization;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;

/**
 * Organization-scoped entity-to-DTO mapping form for tenant management.
 * <p>
 * This form handles bidirectional mapping between {@link Organization} entities and {@link OrganizationDto} DTOs,
 * providing safe field transfers and validation for organization-specific properties. It extends
 * {@link AbstractOrganizationRelatedEntityForm} to leverage the standard form lifecycle including
 * populateFrom (entity → DTO), validate (Jakarta Bean Validation), and populateTo (DTO → entity).
 * </p>
 * <p>
 * The form uses {@link FrontendMappingDefinitions#organizationForm} for frontend field mapping and
 * applies safe merges to entities via {@link #populateTo(Organization)} using {@code getSafeValue}
 * to prevent accidental overwrites of unchanged fields.
 * </p>
 * <p>
 * Managed organization properties include:
 * <ul>
 *   <li>name - Organization name (required field)</li>
 *   <li>assignedDatasource - Tenant-specific datasource assignment (multitenancy mode only)</li>
 *   <li>personalizeDashboard - Dashboard personalization flag</li>
 *   <li>mainBrandColor - Primary branding color</li>
 *   <li>secondBrandColor - Secondary branding color</li>
 *   <li>logoId - Reference to uploaded logo file entity</li>
 * </ul>
 * </p>
 * <p>
 * The Organization entity serves as the tenant root in OpenKoda's multi-tenancy architecture,
 * with branding properties stored in a JSONB properties map and computed privilege fields
 * enforced via JPA @Formula annotations.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Organization
 * @see OrganizationDto
 * @see AbstractOrganizationRelatedEntityForm
 * @see FrontendMappingDefinitions#organizationForm
 */
public class OrganizationForm extends AbstractOrganizationRelatedEntityForm<OrganizationDto, Organization> {

    /**
     * Constructs a form pre-populated with an existing Organization entity.
     * <p>
     * This constructor initializes the form with a new {@link OrganizationDto} and associates it with
     * the provided entity for editing scenarios. The form automatically registers the
     * {@link FrontendMappingDefinitions#organizationForm} mapping definition for field binding.
     * Call {@link #populateFrom(Organization)} after construction to transfer entity state to the DTO.
     * </p>
     *
     * @param organizationId the ID of the organization context (used for tenant scoping)
     * @param entity the {@link Organization} entity to be edited (must not be null)
     */
    public OrganizationForm(Long organizationId, Organization entity) {
        super(organizationId, new OrganizationDto(), entity, FrontendMappingDefinitions.organizationForm);
    }

    /**
     * Constructs an empty form for creating a new Organization entity.
     * <p>
     * This no-argument constructor initializes the form with default state for new organization
     * creation scenarios. The form registers {@link FrontendMappingDefinitions#organizationForm}
     * for field binding. Populate the DTO manually or bind from request parameters before validation.
     * </p>
     */
    public OrganizationForm() {
        super(FrontendMappingDefinitions.organizationForm);
    }

    /**
     * Validates required organization fields before entity persistence.
     * <p>
     * This method enforces validation rules for organization data:
     * <ul>
     *   <li>name - Must not be blank (empty or whitespace-only)</li>
     * </ul>
     * Validation errors are registered with the provided {@link BindingResult} using Spring MVC
     * field error codes. Additional custom validation logic can be added to enforce business rules.
     * </p>
     *
     * @param br the {@link BindingResult} to collect validation errors (must not be null)
     * @return this form instance for method chaining
     * @see StringUtils#isBlank(CharSequence)
     */
    @Override
    public OrganizationForm validate(BindingResult br) {
        if(StringUtils.isBlank(dto.name)) { br.rejectValue("dto.name", "not.empty", defaultErrorMessage); };
        return this;
    }

    /**
     * Transfers Organization entity state to the internal OrganizationDto for editing.
     * <p>
     * This method populates the form's DTO with current entity values, enabling the form to display
     * existing organization data for user modification. Transferred fields include:
     * <ul>
     *   <li>name - Organization name (tenant identifier)</li>
     *   <li>id - Organization primary key</li>
     *   <li>assignedDatasource - Tenant-specific database connection</li>
     *   <li>personalizeDashboard - Dashboard customization flag</li>
     *   <li>mainBrandColor - Primary branding color (hex or name)</li>
     *   <li>secondBrandColor - Secondary branding color (hex or name)</li>
     *   <li>logoId - Foreign key to File entity for organization logo</li>
     * </ul>
     * Call this method after constructing the form with an entity to prepare for editing workflows.
     * </p>
     *
     * @param entity the {@link Organization} entity whose state should be copied to the DTO (must not be null)
     * @return this form instance for method chaining
     */
    @Override
    public OrganizationForm populateFrom(Organization entity) {
        dto.name = entity.getName();
        dto.id = entity.getId();
        dto.assignedDatasource = entity.getAssignedDatasource();
        dto.personalizeDashboard = entity.getPersonalizeDashboard();
        dto.mainBrandColor = entity.getMainBrandColor();
        dto.secondBrandColor = entity.getSecondBrandColor();
        dto.logoId = entity.getLogoId();
        return this;
    }

    /**
     * Applies validated DTO state to the Organization entity using safe field merges.
     * <p>
     * This method transfers user-submitted form data from the DTO back to the entity, applying
     * changes only to fields that were present in the request using {@code getSafeValue}.
     * This pattern prevents accidental overwrites of unchanged fields when partial updates are submitted.
     * </p>
     * <p>
     * Updated fields include:
     * <ul>
     *   <li>name - Organization name (validated as non-blank)</li>
     *   <li>personalizeDashboard - Dashboard customization preference</li>
     *   <li>mainBrandColor - Primary branding color</li>
     *   <li>secondBrandColor - Secondary branding color</li>
     *   <li>logoId - Logo file reference</li>
     *   <li>assignedDatasource - Datasource assignment (only in multitenancy mode via {@link MultitenancyService#isMultitenancy()})</li>
     * </ul>
     * Call this method after successful validation to persist user changes to the entity.
     * </p>
     *
     * @param entity the {@link Organization} entity to be updated with validated DTO values (must not be null)
     * @return the updated entity instance for method chaining
     * @see #getSafeValue(Object, String)
     * @see MultitenancyService#isMultitenancy()
     */
    @Override
    protected Organization populateTo(Organization entity) {
        entity.setName(getSafeValue(entity.getName(), NAME_));
        entity.setPersonalizeDashboard(getSafeValue(entity.getPersonalizeDashboard(), PERSONALIZE_DASHBOARD));
        entity.setMainBrandColor(getSafeValue(entity.getMainBrandColor(), MAIN_BRAND_COLOR));
        entity.setSecondBrandColor(getSafeValue(entity.getSecondBrandColor(), SECOND_BRAND_COLOR));
        entity.setLogoId(getSafeValue(entity.getLogoId(), LOGO_ID));
        if(MultitenancyService.isMultitenancy()) {
            entity.setAssignedDatasource(getSafeValue(entity.getAssignedDatasource(), ASSIGNED_DATASOURCE_));
        }
        return entity;
    }

}
