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

package com.openkoda.core.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.form.TemplateFormFieldNames;
import com.openkoda.model.common.LongIdEntity;
import org.springframework.validation.BindingResult;
import reactor.util.function.Tuples;

/**
 * Extension of {@link AbstractForm} that binds a persistent entity with privilege-aware population lifecycle.
 * <p>
 * This form class manages the complete lifecycle of mapping between a DTO and a persistent JPA entity 
 * (extending {@link LongIdEntity}). It evaluates per-field read/write privileges using {@link PrivilegeHelper}
 * and provides a safe population method that enforces privilege checks before copying DTO values to entity fields.
 * </p>
 * <p>
 * The form lifecycle consists of:
 * <ol>
 *   <li>Construction - Initializes form with optional entity, automatically populating DTO from entity if present</li>
 *   <li>Privilege evaluation - {@link #process()} calls {@link #prepareFieldsReadWritePrivileges(LongIdEntity)} 
 *       to evaluate global read/write privileges for each field</li>
 *   <li>Validation - Inherited from {@link AbstractForm}, validates DTO values against field constraints</li>
 *   <li>Population - {@link #populateToEntity(LongIdEntity)} combines privilege evaluation with 
 *       {@link #populateTo(LongIdEntity)} to safely copy DTO values to entity</li>
 * </ol>
 * </p>
 * <p>
 * Subclasses must implement {@link #populateFrom(LongIdEntity)} to copy entity fields to DTO, and 
 * {@link #populateTo(LongIdEntity)} to copy DTO fields back to entity using {@link #getSafeValue(Object, FrontendMappingFieldDefinition)}
 * for privilege enforcement.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * // Load entity and create form
 * User user = userRepository.findById(userId);
 * UserForm form = new UserForm(dto, user, mapping);
 * 
 * // Evaluate privileges and validate
 * form.process();
 * if (form.validate(bindingResult)) {
 *     // Safely populate entity with privilege checks
 *     form.populateToEntity(user);
 *     userRepository.save(user);
 * }
 * }</pre>
 * </p>
 *
 * @param <D> the DTO type for form binding
 * @param <E> the entity type extending {@link LongIdEntity} for persistence
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @see AbstractForm
 * @see AbstractOrganizationRelatedEntityForm
 * @see ReflectionBasedEntityForm
 * @see PrivilegeHelper
 * @since 1.7.1
 */
public abstract class AbstractEntityForm<D, E extends LongIdEntity> extends AbstractForm<D> implements TemplateFormFieldNames {

    /**
     * The persistent entity representation for database operations.
     * <p>
     * Holds the JPA entity instance that this form manipulates. When constructing a form for a new entity,
     * this field is null. When editing an existing entity, this field holds the loaded entity instance.
     * Subclasses use this field in {@link #populateFrom(LongIdEntity)} and {@link #populateTo(LongIdEntity)}
     * to perform bidirectional mapping between entity and DTO.
     * </p>
     */
    final public E entity;

    /**
     * Cached primary key of the entity, null for new entities.
     * <p>
     * Stores the entity's ID at form construction time. This field is null when creating a new entity
     * (before persistence), and populated with {@link LongIdEntity#getId()} when editing an existing entity.
     * Used for UI rendering and determining whether the form represents a new or existing entity.
     * </p>
     */
    public final Long id;

    /**
     * Spring MVC validation errors from form binding.
     * <p>
     * Holds validation errors detected during HTTP parameter binding and field validation. This field is
     * set by Spring MVC's {@link org.springframework.web.bind.WebDataBinder} after binding request parameters
     * to the form's DTO. Controllers access this via {@link #getBindingResult()} to check validation status
     * and render error messages in the UI.
     * </p>
     */
    @JsonIgnore
    private BindingResult bindingResult;

    /**
     * Constructs a form for creating a new entity without a DTO.
     * <p>
     * This constructor is used when creating a form for a new entity before HTTP parameter binding.
     * The entity and DTO fields are initialized to null, and {@link #id} is null indicating a new entity.
     * After construction, Spring MVC's data binder will populate the DTO from request parameters.
     * </p>
     *
     * @param frontendMappingDefinition the form field definitions and validation rules
     */
    public AbstractEntityForm(FrontendMappingDefinition frontendMappingDefinition) {
        this( null, null, frontendMappingDefinition);
    }

    /**
     * Constructs a form with DTO, entity, and field definitions.
     * <p>
     * This is the main constructor used for both new and existing entities. When editing an existing entity,
     * the entity parameter is non-null and this constructor:
     * <ol>
     *   <li>Stores the entity reference</li>
     *   <li>Caches the entity's ID in the {@link #id} field</li>
     *   <li>Calls {@link #populateFrom(LongIdEntity)} to copy entity values to the DTO</li>
     * </ol>
     * When creating a new entity, pass null for both dto and entity parameters.
     * </p>
     *
     * @param dto the DTO instance for form binding (may be null)
     * @param entity the persistent entity instance (null for new entities)
     * @param frontendMappingDefinition the form field definitions and validation rules
     */
    public AbstractEntityForm(D dto, E entity, FrontendMappingDefinition frontendMappingDefinition) {
        super(dto, frontendMappingDefinition);
        this.entity = entity;
        if (entity == null) {
            id = null;
        } else {
            id = entity.getId();
            populateFrom(entity);
        }

    }

    /**
     * Safely populates entity properties from DTO with privilege evaluation and enforcement.
     * <p>
     * This is the main public method for copying DTO values to the entity. It performs a two-phase operation:
     * <ol>
     *   <li>Calls {@link #prepareFieldsReadWritePrivileges(LongIdEntity)} to evaluate global read/write privileges
     *       for every field using {@link PrivilegeHelper}</li>
     *   <li>Delegates to {@link #populateTo(LongIdEntity)} which uses {@link #getSafeValue(Object, FrontendMappingFieldDefinition)}
     *       to enforce write privilege checks before setting each field value</li>
     * </ol>
     * This ensures that users cannot modify fields they lack write privileges for, even if they tamper with
     * HTTP request parameters.
     * </p>
     *
     * @param entity the persistent entity to populate with DTO values
     * @return the populated entity (same instance passed as parameter)
     */
    final public E populateToEntity(E entity) {
        prepareFieldsReadWritePrivileges(entity);
        return populateTo(entity);
    }

    /**
     * Evaluates field-level privileges for global (non-organization-scoped) access control.
     * <p>
     * Overrides {@link AbstractForm#process()} to perform privilege evaluation using global privilege checks
     * via {@link PrivilegeHelper#canReadField(FrontendMappingFieldDefinition, Object)} and
     * {@link PrivilegeHelper#canWriteField(FrontendMappingFieldDefinition, Object)}. The evaluated privileges
     * are stored in {@link #readWriteForField} map for use during UI rendering and field population.
     * </p>
     * <p>
     * For organization-scoped privilege evaluation, see {@link AbstractOrganizationRelatedEntityForm}
     * which overrides {@link #prepareFieldsReadWritePrivileges(LongIdEntity)} to use
     * organization-aware privilege checks.
     * </p>
     */
    @Override
    public final void process() {
        prepareFieldsReadWritePrivileges(entity);
    }

    /**
     * Returns the cached entity primary key, null for new entities.
     * <p>
     * Provides access to the entity's ID that was cached at form construction time. This value is null
     * when the form represents a new entity (not yet persisted), and contains the entity's database ID
     * when editing an existing entity. Controllers use this to determine whether to perform INSERT or
     * UPDATE operations.
     * </p>
     *
     * @return the entity ID, or null if this is a new entity form
     */
    public final Long getId() {
        return id;
    }

    /**
     * Deprecated placeholder method that returns the entity without modification.
     * <p>
     * This method was originally intended to reassign the entity reference after detachment or session
     * eviction, but is no longer used. The commented-out code shows the original intent to reassign
     * {@code this.entity}, but this would violate the final field constraint. Modern code should manage
     * entity lifecycle externally rather than relying on this method.
     * </p>
     *
     * @param entity the entity to "recover"
     * @return the same entity instance passed as parameter (no-op)
     * @deprecated No longer used; manage entity lifecycle externally
     */
    @Deprecated
    public E recoverEntity(E entity) {
//        this.entity = entity;
        return entity;
    }

    /**
     * Evaluates and caches per-field read/write privileges using global privilege checking.
     * <p>
     * Iterates through all field definitions in {@link #frontendMappingDefinition} and evaluates whether
     * the current user has read and write privileges for each field. Uses {@link PrivilegeHelper} singleton
     * to perform global privilege checks (not organization-scoped). Results are stored in the
     * {@link #readWriteForField} map as {@link reactor.util.function.Tuple2} of (canRead, canWrite) booleans.
     * </p>
     * <p>
     * The privilege checks consider:
     * <ul>
     *   <li>Field-level privilege requirements: {@link FrontendMappingFieldDefinition#readPrivilege} and
     *       {@link FrontendMappingFieldDefinition#writePrivilege}</li>
     *   <li>Runtime predicate functions: {@link FrontendMappingFieldDefinition#canReadCheck} and
     *       {@link FrontendMappingFieldDefinition#canWriteCheck}</li>
     *   <li>Current user's global role assignments and privileges</li>
     * </ul>
     * </p>
     * <p>
     * Subclasses like {@link AbstractOrganizationRelatedEntityForm} override this method to use
     * organization-scoped privilege evaluation instead of global checks.
     * </p>
     *
     * @param entity the entity context for privilege evaluation (may be null for new entities)
     */
    public void prepareFieldsReadWritePrivileges(E entity) {
        for (FrontendMappingFieldDefinition f : frontendMappingDefinition.fields) {
            readWriteForField.put(f,
                Tuples.of(
                    PrivilegeHelper.getInstance().canReadField(f, entity),
                    PrivilegeHelper.getInstance().canWriteField(f, entity)));
        }
    }


    /**
     * Populates the DTO with values from the entity (entity → DTO direction).
     * <p>
     * Subclasses must implement this method to copy entity field values to the DTO representation.
     * This method is called automatically during form construction when an existing entity is provided,
     * enabling the form to display current entity values in the UI.
     * </p>
     * <p>
     * Implementation approaches:
     * <ul>
     *   <li>{@link ReflectionBasedEntityForm} - Uses reflection with {@link org.apache.commons.beanutils.PropertyUtils}
     *       to automatically copy properties by name</li>
     *   <li>{@link MapEntityForm} - Directly copies the entity's value map to the DTO map</li>
     *   <li>Custom forms - Manually copy each field, applying custom converters or computed values</li>
     * </ul>
     * Implementations should handle null entity fields gracefully and apply any necessary type conversions
     * using field-specific {@link FrontendMappingFieldDefinition#entityToDtoValueConverter} if configured.
     * </p>
     *
     * @param entity the persistent entity to read values from
     * @param <F> the concrete form type for fluent chaining
     * @return this form instance for method chaining
     */
    abstract protected <F extends AbstractEntityForm<D, E>> F populateFrom(E entity);

    /**
     * Populates the entity with values from the DTO (DTO → entity direction) with privilege enforcement.
     * <p>
     * Subclasses must implement this method to copy DTO field values to the entity. This method is called
     * by {@link #populateToEntity(LongIdEntity)} after privilege evaluation, ensuring that only fields
     * the user has write privileges for are modified. Implementations must use
     * {@link #getSafeValue(Object, FrontendMappingFieldDefinition)} to enforce write privilege checks
     * before setting each entity field.
     * </p>
     * <p>
     * Implementation pattern:
     * <pre>{@code
     * protected User populateTo(User entity) {
     *     entity.setFirstName(getSafeValue(dto.getFirstName(), findField("firstName")));
     *     entity.setLastName(getSafeValue(dto.getLastName(), findField("lastName")));
     *     entity.setSalary(getSafeValue(dto.getSalary(), findField("salary")));
     *     return entity;
     * }
     * }</pre>
     * The {@link #getSafeValue(Object, FrontendMappingFieldDefinition)} method returns the new value if the user
     * has write privilege, or the entity's existing value if the user lacks privilege, preventing unauthorized
     * field modifications.
     * </p>
     * <p>
     * Implementations should apply custom type conversions using field-specific
     * {@link FrontendMappingFieldDefinition#dtoToEntityValueConverter} if configured, and handle null DTO
     * values according to field constraints.
     * </p>
     *
     * @param entity the persistent entity to update with DTO values
     * @return the updated entity (same instance passed as parameter)
     */
    abstract protected E populateTo(E entity);

    /**
     * Returns the Spring MVC binding result containing validation errors.
     * <p>
     * Provides access to the {@link BindingResult} object populated by Spring MVC's
     * {@link org.springframework.web.bind.WebDataBinder} during HTTP parameter binding and validation.
     * Controllers check this result to determine whether form submission was valid before persisting
     * the entity. The binding result contains field errors, global errors, and rejected values.
     * </p>
     *
     * @return the validation errors from form binding, or null if not yet bound
     */
    public BindingResult getBindingResult() {
        return bindingResult;
//        return null;
    }

    /**
     * Sets the Spring MVC binding result after form validation.
     * <p>
     * Called by Spring MVC's {@link org.springframework.web.bind.WebDataBinder} or by
     * {@link MapFormArgumentResolver} to attach validation errors to the form instance. This allows
     * the form to carry its validation state through the controller method and into the view layer
     * for error message rendering.
     * </p>
     *
     * @param bindingResult the validation errors from form binding
     */
    public void setBindingResult(BindingResult bindingResult) {
        this.bindingResult = bindingResult;
    }

    /**
     * Returns the persistent entity associated with this form.
     * <p>
     * Implements {@link DtoAndEntity#getEntity()} to provide access to the JPA entity for use in
     * datalist suppliers and field converters that need both DTO and entity representations. This method
     * returns null when the form represents a new entity (not yet constructed).
     * </p>
     *
     * @return the persistent entity instance, or null for new entity forms
     */
    @Override
    public E getEntity() {
        return entity;
    }
}
