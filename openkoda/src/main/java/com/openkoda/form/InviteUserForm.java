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
import com.openkoda.dto.user.InviteUserDto;
import com.openkoda.model.User;
import org.springframework.validation.BindingResult;

/**
 * Form for inviting users with role name validation.
 * <p>
 * This form extends {@link AbstractEntityForm} to handle user invitation workflows.
 * It enforces presence of {@code dto.roleName} in addition to standard email validation
 * checks performed by {@link BasicUserForm}. The validation flow delegates to
 * {@code BasicUserForm.validate()} for email and basic user field validation, then
 * adds role name presence validation.
 * </p>
 * <p>
 * The form binds to {@link InviteUserDto} for request data and {@link User} entity
 * for persistence. It uses {@link FrontendMappingDefinitions#inviteForm} for
 * field mapping configuration.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * InviteUserForm form = new InviteUserForm();
 * form.validate(bindingResult);
 * </pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractEntityForm
 * @see BasicUserForm
 * @see InviteUserDto
 */
public class InviteUserForm extends AbstractEntityForm<InviteUserDto, User> {

    private static final String FORM_NAME = "inviteUserForm";


    /** Constant <code>inviteUserFields</code> */

    /** Constant <code>inviteForm</code> */

    /**
     * Constructs an InviteUserForm with existing DTO and entity.
     * <p>
     * This constructor is used when both the data transfer object and the
     * target entity already exist, typically during form editing or update
     * operations. The form is initialized with the invite form field mappings
     * from {@link FrontendMappingDefinitions#inviteForm}.
     * </p>
     *
     * @param dto the {@link InviteUserDto} containing form data including email
     *            and role name
     * @param entity the {@link User} entity to be populated from the form
     */
    public InviteUserForm(InviteUserDto dto, User entity) {
        super(dto, entity, FrontendMappingDefinitions.inviteForm);
    }

    /**
     * Constructs an empty InviteUserForm with default field mappings.
     * <p>
     * This no-argument constructor creates a new form instance without pre-populated
     * data, suitable for new user invitation workflows. The DTO and entity will be
     * created or set later. Field mappings are initialized from
     * {@link FrontendMappingDefinitions#inviteForm}.
     * </p>
     */
    public InviteUserForm() {
        super(FrontendMappingDefinitions.inviteForm);
    }

    /**
     * Validates the invite user form including role name presence.
     * <p>
     * This method performs a two-stage validation process:
     * </p>
     * <ol>
     *   <li>Delegates to {@link BasicUserForm#validate(Object, BindingResult)} for
     *       standard email and basic user field validation</li>
     *   <li>Validates that {@code dto.roleName} is present and not empty, rejecting
     *       with error code 'not.empty' if blank</li>
     * </ol>
     * <p>
     * Validation errors are accumulated in the provided {@code BindingResult}.
     * The method returns {@code this} to support fluent method chaining.
     * </p>
     *
     * @param br the {@link BindingResult} to accumulate validation errors
     * @return this form instance for fluent chaining
     * @see BasicUserForm#validate(Object, BindingResult)
     */
    @Override
    public InviteUserForm validate(BindingResult br) {
        BasicUserForm.validate(dto, br);
        if (dto.roleName == null || dto.roleName.isEmpty()) {br.rejectValue("dto.roleName", "not.empty");}
        return this;
    }

    /**
     * Populates the form DTO from the given User entity.
     * <p>
     * This method delegates to {@link BasicUserForm#populateFromEntity(Object, User)}
     * to transfer standard user fields (email, first name, last name) from the entity
     * to the form's DTO. The method returns {@code this} to support fluent method chaining.
     * </p>
     *
     * @param entity the {@link User} entity to populate from
     * @return this form instance for fluent chaining
     * @see BasicUserForm#populateFromEntity(Object, User)
     */
    @Override
    public InviteUserForm populateFrom(User entity) {
        BasicUserForm.populateFromEntity(dto, entity);
        return this;
    }

    /**
     * Populates the User entity from form data.
     * <p>
     * This method transfers validated form data to the target {@link User} entity.
     * It uses {@link #getSafeValue(Object, String)} to safely update only non-null
     * form values, preserving existing entity values when form fields are empty.
     * The following fields are updated:
     * </p>
     * <ul>
     *   <li>{@code firstName} - User's first name</li>
     *   <li>{@code lastName} - User's last name</li>
     *   <li>{@code email} - User's email address</li>
     * </ul>
     *
     * @param entity the {@link User} entity to populate with form data
     * @return the populated entity instance
     */
    @Override
    protected User populateTo(User entity) {
        entity.setFirstName( getSafeValue( entity.getFirstName(), FIRST_NAME_));
        entity.setLastName( getSafeValue( entity.getLastName(), LAST_NAME_));
        entity.setEmail( getSafeValue( entity.getEmail(), EMAIL_));

        return entity;
    }

}
