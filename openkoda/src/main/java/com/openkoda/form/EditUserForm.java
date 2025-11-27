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
import com.openkoda.dto.user.EditUserDto;
import com.openkoda.model.User;
import org.springframework.validation.BindingResult;

/**
 * Form adapter for editing existing User entities with enabled flag and language support.
 * <p>
 * This form extends {@link BasicUserForm} functionality by adding support for the enabled
 * flag and language preferences during user editing operations. It handles the lifecycle
 * of mapping User entity data to an {@link EditUserDto} for presentation, validating user
 * input, and mapping the validated data back to the User entity.

 * <p>
 * The form delegates basic field validation (email, first name, last name) to
 * {@link BasicUserForm#validate} while extending the data mapping
 * in {@link #populateFrom(User)} to include enabled status and language preference fields.

 * <p>
 * Usage example:
 * <pre>{@code
 * EditUserForm form = new EditUserForm(existingUser);
 * form.validate(bindingResult);
 * form.populateTo(existingUser);
 * }</pre>

 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2019-01-23
 * See {@code BasicUserForm}
 * See {@code EditUserDto}
 * See {@code User}
 * See {@code AbstractEntityForm}
 */
public class EditUserForm extends AbstractEntityForm<EditUserDto, User> {


    /**
     * Constructs a new EditUserForm with default settings.
     * <p>
     * Initializes the form using the predefined frontend mapping definition for user editing.
     * This constructor is typically used when creating a form without an existing User entity,
     * such as in scenarios where the entity will be loaded later.

     *
     * See {@code FrontendMappingDefinitions#editUserForm}
     */
    public EditUserForm() {
        super(FrontendMappingDefinitions.editUserForm);
    }

    /**
     * Constructs a new EditUserForm initialized with data from an existing User entity.
     * <p>
     * This constructor creates a new {@link EditUserDto}, associates it with the provided
     * User entity, and configures the form using the predefined frontend mapping definition.
     * The form is ready for immediate use after construction.

     *
     * @param entity the User entity to edit; must not be {@code null}
     * See {@code FrontendMappingDefinitions#editUserForm}
     * See {@code EditUserDto}
     */
    public EditUserForm(User entity) {
        super(new EditUserDto(), entity, FrontendMappingDefinitions.editUserForm);
    }

    /**
     * Validates the form data by delegating to BasicUserForm validation logic.
     * <p>
     * This method performs validation on the underlying {@link EditUserDto} by invoking
     * {@link BasicUserForm#validate}, which checks email format,
     * required fields (first name, last name), and other basic user field constraints.
     * Validation errors are recorded in the provided {@link BindingResult}.

     *
     * @param br the BindingResult object to record validation errors; must not be {@code null}
     * @return this EditUserForm instance for method chaining
     * @see BasicUserForm#validate
     */
    @Override
    public EditUserForm validate(BindingResult br) {
        BasicUserForm.validate(dto, br);
        return this;
    }

    /**
     * Populates the form DTO with data from the provided User entity.
     * <p>
     * This method extends {@link BasicUserForm#populateFromEntity} by additionally
     * mapping the enabled flag, language preference, and global role name from the User entity
     * to the {@link EditUserDto}. This ensures all user attributes relevant to the edit form
     * are available for display and modification.

     * <p>
     * Fields populated:
     * <ul>
     *   <li>Basic user fields (first name, last name, email) via BasicUserForm</li>
     *   <li>Enabled status flag</li>
     *   <li>Language preference</li>
     *   <li>Global role name</li>
     * </ul>

     *
     * @param entity the User entity to populate from; must not be {@code null}
     * @return this EditUserForm instance for method chaining
     * @see BasicUserForm#populateFromEntity
     * See {@code EditUserDto}
     */
    @Override
    public EditUserForm populateFrom(User entity) {
        BasicUserForm.populateFromEntity(dto, entity);
        dto.setEnabled(entity.isEnabled());
        dto.setLanguage(entity.getLanguage());
        dto.setGlobalRoleName(entity.getGlobalRoleName());
        return this;
    }

    /**
     * Populates the User entity with validated data from the form DTO.
     * <p>
     * This method transfers user data from the {@link EditUserDto} back to the User entity
     * using safe value accessors that handle null values and provide fallback defaults.
     * The method uses {@link #getSafeValue} to ensure data integrity during the mapping process.

     * <p>
     * Fields updated in the entity:
     * <ul>
     *   <li>First name (empty string if blank)</li>
     *   <li>Last name (empty string if blank)</li>
     *   <li>Email address</li>
     *   <li>Enabled status flag</li>
     *   <li>Language preference</li>
     * </ul>

     * <p>
     * Note: This method does NOT update role assignments. Role management is typically
     * handled separately through dedicated role assignment workflows.

     *
     * @param entity the User entity to populate; must not be {@code null}
     * @return the updated User entity
     * See {@code AbstractEntityForm#getSafeValue}
     */
    @Override
    protected User populateTo(User entity) {
        entity.setFirstName( getSafeValue( entity.getFirstName(), FIRST_NAME_, emptyIfBlank) );
        entity.setLastName( getSafeValue( entity.getLastName(), LAST_NAME_, emptyIfBlank) );
        entity.setEmail( getSafeValue( entity.getEmail(), EMAIL_) );
        entity.setEnabled( getSafeValue( entity.isEnabled(), ENABLED_) );
        entity.setLanguage( getSafeValue( entity.getLanguage(), LANGUAGE));
        return entity;
    }

}
