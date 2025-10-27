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

import com.openkoda.core.form.AbstractEntityForm;
import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.dto.user.PrivilegeDto;
import com.openkoda.model.DynamicPrivilege;
import com.openkoda.model.PrivilegeBase;
import org.springframework.validation.BindingResult;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Entity-to-DTO conversion form for Privilege entities.
 * <p>
 * Performs {@link PrivilegeBase} to {@link PrivilegeDto} mapping for dynamic privilege management.
 * Expects {@code PrivilegeBase} instances that are {@link DynamicPrivilege} at runtime. The form
 * performs safe field merging via {@code getSafeValue} in {@link #populateTo(PrivilegeBase)} to
 * conditionally update entity fields based on DTO presence.
 * </p>
 * <p>
 * Extends {@link AbstractEntityForm} with generic types {@code PrivilegeDto} and {@code PrivilegeBase},
 * implementing {@link TemplateFormFieldNames} for field name constants. Follows the standard form
 * lifecycle: {@code populateFrom} (entity → DTO), {@code validate} (custom validation logic),
 * {@code populateTo} (DTO → entity).
 * </p>
 * <p>
 * <b>Runtime Type Requirement:</b> The {@code populateTo} method casts {@code PrivilegeBase} to
 * {@code DynamicPrivilege}, so the entity parameter must be a {@code DynamicPrivilege} instance.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2024-05-27
 * @see AbstractEntityForm
 * @see PrivilegeDto
 * @see PrivilegeBase
 * @see DynamicPrivilege
 * @see TemplateFormFieldNames
 * @see FrontendMappingDefinitions#privilegeForm
 */
public class PrivilegeForm extends AbstractEntityForm<PrivilegeDto, PrivilegeBase> implements TemplateFormFieldNames {

    /**
     * Constructs a PrivilegeForm with explicit DTO, entity, and frontend mapping definition.
     * <p>
     * Initializes the form with the provided {@link PrivilegeDto}, {@link PrivilegeBase} entity,
     * and custom {@link FrontendMappingDefinition}. Use this constructor when you need full control
     * over all three form components.
     * </p>
     *
     * @param dto the {@link PrivilegeDto} instance for form data binding
     * @param entity the {@link PrivilegeBase} entity to populate from or populate to
     * @param frontendMappingDefinition the {@link FrontendMappingDefinition} defining field mappings and validation rules
     */
    public PrivilegeForm(PrivilegeDto dto, PrivilegeBase entity, FrontendMappingDefinition frontendMappingDefinition) {
        super(dto, entity, frontendMappingDefinition);
    }

    /**
     * Constructs a PrivilegeForm for an existing entity with a new DTO.
     * <p>
     * Creates a new {@link PrivilegeDto} instance and uses the provided {@link PrivilegeBase} entity
     * along with the standard {@link FrontendMappingDefinitions#privilegeForm} mapping. Use this
     * constructor when populating from an existing privilege entity.
     * </p>
     *
     * @param entity the {@link PrivilegeBase} entity to populate from (must be a {@link DynamicPrivilege} instance for populateTo)
     */
    public PrivilegeForm(PrivilegeBase entity) {
        super(new PrivilegeDto(), entity, FrontendMappingDefinitions.privilegeForm);
    }

    /**
     * Constructs an empty PrivilegeForm with no DTO or entity.
     * <p>
     * Initializes the form with {@code null} for both DTO and entity, using the standard
     * {@link FrontendMappingDefinitions#privilegeForm} mapping. Use this constructor when
     * creating a new privilege from scratch; you'll need to set the DTO and entity before
     * using the form lifecycle methods.
     * </p>
     */
    public PrivilegeForm() {
        super(null, null, FrontendMappingDefinitions.privilegeForm);
    }

    /**
     * Populates the DTO from the given PrivilegeBase entity.
     * <p>
     * Transfers entity state to the {@link PrivilegeDto}: copies {@code id}, {@code name},
     * {@code label}, {@code category}, and {@code privilegeGroup} from the {@link PrivilegeBase}
     * entity to the corresponding DTO fields. This method is typically called when editing an
     * existing privilege.
     * </p>
     *
     * @param entity the {@link PrivilegeBase} entity to populate from (must not be {@code null})
     * @return this form instance for fluent chaining
     * @see AbstractEntityForm#populateFrom(Object)
     */
    @Override
    public PrivilegeForm populateFrom(PrivilegeBase entity) {
        dto.setId(entity.getId());
        dto.setName(entity.name());
        dto.setLabel(entity.getLabel());
        dto.setCategory(entity.getCategory());
        dto.setPrivilegeGroup(entity.getGroup());
        return this;
    }

    /**
     * Transfers validated form data to the DynamicPrivilege entity.
     * <p>
     * Applies validated DTO field values to the {@link DynamicPrivilege} entity using
     * {@code getSafeValue} for safe merging. This method conditionally updates entity fields
     * only when corresponding DTO values are present, preserving existing entity values for
     * absent DTO fields.
     * </p>
     * <p>
     * <b>Runtime Requirement:</b> The entity parameter must be a {@link DynamicPrivilege} instance.
     * This method performs an unchecked cast from {@code PrivilegeBase} to {@code DynamicPrivilege}.
     * </p>
     * <p>
     * Transfers: {@code id}, {@code name}, {@code label}, {@code category}, and {@code group}
     * using field name constants ({@link #ID_}, {@link #NAME_}, {@link #LABEL_}, "category", "group").
     * </p>
     *
     * @param entity the {@link PrivilegeBase} entity to populate (must be a {@link DynamicPrivilege} instance)
     * @return the updated {@link DynamicPrivilege} entity
     * @throws ClassCastException if entity is not a {@link DynamicPrivilege} instance
     * @see AbstractEntityForm#populateTo(Object)
     * @see #getSafeValue(Object, String)
     */
    @Override
    protected DynamicPrivilege populateTo(PrivilegeBase entity) {
        DynamicPrivilege dynamicEntiry = (DynamicPrivilege)entity;
        dynamicEntiry.setId(getSafeValue(entity.getId(), ID_));
        dynamicEntiry.setName(getSafeValue(entity.name(), NAME_));
        dynamicEntiry.setLabel(getSafeValue(entity.getLabel(), LABEL_));
        dynamicEntiry.setCategory(getSafeValue(entity.getCategory(), "category"));
        dynamicEntiry.setGroup(getSafeValue(entity.getGroup(), "group"));
        return dynamicEntiry;
    }

    /**
     * Validates form data using custom logic for privilege creation and editing.
     * <p>
     * Performs validation checks on the {@link PrivilegeDto} fields:
     * </p>
     * <ul>
     *   <li>Validates {@code name} is not blank - rejects with error code "not.empty" if blank</li>
     *   <li>Validates {@code label} is not blank - rejects with error code "not.empty" if blank</li>
     *   <li>Validates {@code category} is not blank - rejects with error code "not.empty" if blank</li>
     *   <li>Validates {@code privilegeGroup} is not null - rejects with error code "not.empty" if null</li>
     * </ul>
     * <p>
     * Uses Apache Commons {@code StringUtils.isBlank} for string validation and direct null checks
     * for the privilege group enum. All validation errors are recorded in the provided
     * {@link BindingResult}.
     * </p>
     *
     * @param br the {@link BindingResult} for collecting validation errors
     * @return this form instance for fluent chaining
     * @see AbstractEntityForm#validate(BindingResult)
     * @see org.apache.commons.lang3.StringUtils#isBlank(CharSequence)
     */
    @Override
    public PrivilegeForm validate(BindingResult br) {
        if (isBlank(dto.getName())) {
            br.rejectValue("dto.name", "not.empty");
        }
        
        if (isBlank(dto.getLabel())) {
            br.rejectValue("dto.label", "not.empty");
        }
        
        if (isBlank(dto.getCategory())) {
            br.rejectValue("dto.category", "not.empty");
        }
        
        if (dto.getPrivilegeGroup() == null) {
            br.rejectValue("dto.group", "not.empty");
        }
        return this;
    }

}
