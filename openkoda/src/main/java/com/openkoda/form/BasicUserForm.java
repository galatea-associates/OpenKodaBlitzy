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
import com.openkoda.dto.user.BasicUser;
import com.openkoda.model.User;
import org.springframework.validation.BindingResult;

import java.util.regex.Pattern;

import static com.openkoda.form.FrontendMappingDefinitions.userForm;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Maps BasicUser DTO to User entity with email validation.
 * <p>
 * This form adapter transfers data between {@link BasicUser} DTOs and {@link User} entities,
 * enforcing email validation using a compiled Pattern on each emailIsValid call. Extends
 * {@link AbstractEntityForm} and exposes the frontend mapping {@link FrontendMappingDefinitions#userForm}.
 * 
 * <p>
 * <b>Known Issues:</b>
 * The {@link #populateTo(User)} method contains a bug where {@code entity.setFirstName()} is incorrectly
 * called three times instead of calling setFirstName, setLastName, and setEmail respectively.
 * 
 * <p>
 * <b>Note:</b> The {@code dtoField} exists but is currently unused in the implementation.
 * 
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractEntityForm
 * @see BasicUser
 * @see User
 * @see FrontendMappingDefinitions#userForm
 */
public class BasicUserForm extends AbstractEntityForm<BasicUser, User> {

    private Object dtoField;

    /**
     * Constructs a BasicUserForm with specified DTO and entity using the default user form mapping.
     * <p>
     * Initializes the form with the provided {@link BasicUser} DTO and {@link User} entity,
     * using the predefined {@link FrontendMappingDefinitions#userForm} mapping definition.
     * 
     *
     * @param dto the BasicUser DTO containing form data
     * @param entity the User entity to be populated or populated from
     */
    public BasicUserForm(BasicUser dto, User entity) {
        super(dto, entity, userForm);
    }

    /**
     * Constructs a BasicUserForm with specified DTO, entity, and custom form mapping.
     * <p>
     * Initializes the form with the provided {@link BasicUser} DTO, {@link User} entity,
     * and a custom {@link FrontendMappingDefinition} allowing override of the default mapping.
     * 
     *
     * @param dto the BasicUser DTO containing form data
     * @param entity the User entity to be populated or populated from
     * @param form the custom FrontendMappingDefinition to use for field mappings
     */
    public BasicUserForm(BasicUser dto, User entity, FrontendMappingDefinition form) {
        super(dto, entity, form);
    }

    /**
     * Constructs an empty BasicUserForm with null DTO and entity using the default user form mapping.
     * <p>
     * Creates a form instance with null DTO and entity, using the predefined
     * {@link FrontendMappingDefinitions#userForm} mapping definition. Typically used for
     * creating new user forms where both DTO and entity will be set later.
     * 
     */
    public BasicUserForm() {
        super(null, null, userForm);
    }

    /**
     * Validates the BasicUser DTO email field using an RFC-style regex pattern.
     * <p>
     * Performs validation of the email field using the {@link #emailIsValid(String)} helper
     * method with a compiled regex Pattern. Rejects with 'not.valid' error code if the email
     * format is invalid, and 'not.empty' if the email is blank but not null.
     * 
     *
     * @param dto the BasicUser DTO to validate
     * @param br the BindingResult to record validation errors
     * @return the validated BasicUser DTO
     */
    protected static BasicUser validate(BasicUser dto, BindingResult br) {

        if (isNotBlank(dto.email) && !emailIsValid(dto.email)) {
            br.rejectValue("dto.email", "not.valid");
        }
        if (isBlank(dto.email) && dto.email!=null) {
            br.rejectValue("dto.email", "not.empty");
        }
        return dto;
    }

    /**
     * Validates email format using a compiled regex pattern.
     * <p>
     * Helper method that compiles a regex pattern on each invocation to validate email format.
     * The pattern follows RFC-style email validation:
     * {@code ^([a-zA-Z0-9_\-\.+]+)@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.)|(([a-zA-Z0-9\-]+\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\]?)$}
     * 
     * <p>
     * <b>Performance Note:</b> The pattern is compiled on each call rather than being cached as a
     * static field, which may impact performance in high-frequency validation scenarios.
     * 
     *
     * @param email the email address to validate
     * @return true if the email matches the validation pattern, false otherwise
     */
    protected static boolean emailIsValid(String email){
        final Pattern pattern = Pattern.compile("^([a-zA-Z0-9_\\-\\.\\+]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$");
        if(pattern.matcher(email).matches()){
            return true;
        }
        return false;
    }

    /**
     * Transfers User entity data to BasicUser DTO.
     * <p>
     * Static helper method that populates the BasicUser DTO with firstName, lastName, and email
     * values from the User entity. This method directly assigns to DTO fields rather than using setters.
     * 
     *
     * @param dto the BasicUser DTO to populate
     * @param entity the User entity containing source data
     * @return the populated BasicUser DTO
     */
    protected static BasicUser populateFromEntity(BasicUser dto, User entity) {
        dto.firstName = entity.getFirstName();
        dto.lastName = entity.getLastName();
        dto.email = entity.getEmail();
        return dto;
    }

    /**
     * Populates this form's DTO from the User entity.
     * <p>
     * Transfers User entity data (email, firstName, lastName) to the internal BasicUser DTO
     * by delegating to {@link #populateFromEntity(BasicUser, User)}. This method implements
     * the form lifecycle pattern for entity-to-DTO mapping.
     * 
     *
     * @param entity the User entity to populate from
     * @return this BasicUserForm instance for method chaining
     */
    @Override
    public BasicUserForm populateFrom(User entity) {
        populateFromEntity(dto, entity);
        return this;
    }

    /**
     * Applies validated DTO data to the User entity.
     * <p>
     * Transfers data from the internal BasicUser DTO to the User entity using safe value getters.
     * This method implements the form lifecycle pattern for DTO-to-entity mapping.
     * 
     * <p>
     * <b>IMPLEMENTATION BUG:</b> This method incorrectly calls {@code entity.setFirstName()} three times
     * on lines 107-109. The correct implementation should call:
     * <ul>
     *   <li>Line 107: {@code entity.setFirstName(getSafeValue(entity.getFirstName(), FIRST_NAME_))}</li>
     *   <li>Line 108: {@code entity.setLastName(getSafeValue(entity.getLastName(), LAST_NAME_))} (not setFirstName)</li>
     *   <li>Line 109: {@code entity.setEmail(getSafeValue(entity.getEmail(), EMAIL_))} (not setFirstName)</li>
     * </ul>
     * As a result, lastName and email values are not properly saved to the entity.
     * 
     *
     * @param entity the User entity to populate with DTO data
     * @return the populated User entity
     */
    @Override
    protected User populateTo(User entity) {
        entity.setFirstName( getSafeValue( entity.getFirstName(), FIRST_NAME_) );
        entity.setFirstName( getSafeValue( entity.getLastName(), LAST_NAME_) );
        entity.setFirstName( getSafeValue( entity.getEmail(), EMAIL_) );
        return entity;
    }

    /**
     * Validates this form's DTO and records errors in the BindingResult.
     * <p>
     * Instance method that delegates to the static {@link #validate(BasicUser, BindingResult)}
     * method to perform email validation on the internal DTO. This method implements the
     * form lifecycle pattern for validation.
     * 
     *
     * @param br the BindingResult to record validation errors
     * @return this BasicUserForm instance for method chaining
     */
    public BasicUserForm validate(BindingResult br) {
        validate(dto, br);
        return this;
    }
}