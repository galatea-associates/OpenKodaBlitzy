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
import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.dto.user.RoleDto;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.Role;
import org.springframework.validation.BindingResult;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Entity-to-DTO conversion form for Role entity with privilege set transformation.
 * <p>
 * This form performs Role-to-RoleDto mapping with bidirectional privilege set conversion.
 * The {@link #populateFrom(Role)} method transfers entity state to DTO, converting the
 * privilege set to a string list using {@link PrivilegeBase#name()} stream mapping.
 * The {@link #populateTo(Role)} method applies validated form data back to the entity,
 * using {@link PrivilegeHelper#valueOfString(String)} to transform List&lt;String&gt; to
 * Set&lt;PrivilegeBase&gt;, defaulting to an empty HashSet if null.
 * </p>
 * <p>
 * This form extends {@link AbstractEntityForm} with generic types RoleDto and Role,
 * implementing {@link TemplateFormFieldNames} for consistent field name constants.
 * Request-scoped lifecycle follows the standard populateFrom → validate → populateTo pattern.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2019-01-25
 * @see AbstractEntityForm
 * @see RoleDto
 * @see Role
 * @see PrivilegeHelper
 * @see PrivilegeBase
 */
public class RoleForm extends AbstractEntityForm<RoleDto, Role> implements TemplateFormFieldNames {

    /**
     * Constructs a RoleForm with fully specified DTO, entity, and frontend mapping definition.
     * <p>
     * This constructor initializes the form with all required components for role entity
     * management, allowing custom frontend mapping definitions beyond the default
     * {@link FrontendMappingDefinitions#roleForm}.
     * </p>
     *
     * @param dto the RoleDto instance containing form data
     * @param entity the Role entity instance to be populated
     * @param frontendMappingDefinition the frontend mapping definition for field rendering and validation
     */
    public RoleForm(RoleDto dto, Role entity, FrontendMappingDefinition frontendMappingDefinition) {
        super(dto, entity, frontendMappingDefinition);
    }

    /**
     * Constructs a RoleForm for an existing Role entity.
     * <p>
     * This constructor creates a new RoleDto instance and uses the default
     * {@link FrontendMappingDefinitions#roleForm} mapping definition. Typically used
     * when editing an existing role where form data will be populated via
     * {@link #populateFrom(Role)}.
     * </p>
     *
     * @param entity the Role entity to be managed by this form
     */
    public RoleForm(Role entity) {
        super(new RoleDto(), entity, FrontendMappingDefinitions.roleForm);
    }

    /**
     * Constructs an empty RoleForm for new role creation.
     * <p>
     * This no-argument constructor initializes the form with null DTO and entity,
     * using the default {@link FrontendMappingDefinitions#roleForm} mapping definition.
     * Typically used for new role creation forms where both DTO and entity will be
     * instantiated during form processing.
     * </p>
     */
    public RoleForm() {
        super(null, null, FrontendMappingDefinitions.roleForm);
    }

    /**
     * Transfers Role entity state to RoleDto with privilege set transformation.
     * <p>
     * This method populates the form DTO from the Role entity, copying name and type
     * directly. The privilege set is converted from Set&lt;PrivilegeBase&gt; to
     * List&lt;String&gt; using stream mapping with {@link PrivilegeBase#name()}.
     * This transformation enables frontend rendering of privilege selections as
     * simple string identifiers.
     * </p>
     *
     * @param entity the Role entity containing source data
     * @return this RoleForm instance for fluent method chaining
     * @see Role#getPrivilegesSet()
     * @see PrivilegeBase#name()
     */
    @Override
    public RoleForm populateFrom(Role entity) {
        dto.name = entity.getName();
        dto.type = entity.getType();
        dto.privileges =  entity.getPrivilegesSet().stream().map(PrivilegeBase::name).collect(Collectors.toList());
        return this;
    }

    /**
     * Applies validated form data to Role entity with privilege set transformation.
     * <p>
     * This method transfers validated DTO data back to the Role entity using
     * {@link #getSafeValue(Object, String)} for conditional updates. The role name
     * is updated from the NAME_ field. The privilege set undergoes transformation
     * from List&lt;String&gt; to Set&lt;PrivilegeBase&gt; using
     * {@link PrivilegeHelper#valueOfString(String)} for each privilege name,
     * defaulting to an empty HashSet if the privilege list is null.
     * </p>
     * <p>
     * The getSafeValue method ensures that only non-null DTO values trigger entity
     * updates, preserving existing entity state when form fields are not provided.
     * </p>
     *
     * @param entity the Role entity to be updated with validated form data
     * @return the updated Role entity
     * @see PrivilegeHelper#valueOfString(String)
     * @see AbstractEntityForm#getSafeValue(Object, String)
     */
    @Override
    protected Role populateTo(Role entity) {
        
        entity.setName(getSafeValue(entity.getName(), NAME_));
        entity.setPrivilegesSet(getSafeValue(entity.getPrivilegesSet(), PRIVILEGES_,
                ((List<String> p) -> (p != null ?
                        p.stream().map(PrivilegeHelper::valueOfString).collect(Collectors.toSet()) : new HashSet<>()))));

        return entity;
    }

    /**
     * Validates form data ensuring required fields are present.
     * <p>
     * This method performs validation on the role name and type fields, rejecting
     * the form with error code 'not.empty' if either field is blank. Validation uses
     * {@link org.apache.commons.lang3.StringUtils#isBlank(CharSequence)} to check for
     * null, empty, or whitespace-only values.
     * </p>
     * <p>
     * Validation errors are recorded in the provided {@link BindingResult}, which can
     * be inspected by controllers to determine form processing success or failure.
     * </p>
     *
     * @param br the BindingResult for collecting validation errors
     * @return this RoleForm instance for fluent method chaining
     */
    @Override
    public RoleForm validate(BindingResult br) {
        if (isBlank(dto.name)) {
            br.rejectValue("dto.name", "not.empty");
        }
        if (isBlank(dto.type)) {
            br.rejectValue("dto.type", "not.empty");
        }
        return this;
    }

}
