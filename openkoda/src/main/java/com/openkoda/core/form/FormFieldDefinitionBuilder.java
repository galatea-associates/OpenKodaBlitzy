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

import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.core.security.OrganizationUser;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.common.LongIdEntity;
import com.openkoda.uicomponent.annotation.Autocomplete;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.openkoda.core.form.FrontendMappingFieldDefinition.*;
import static com.openkoda.core.helper.PrivilegeHelper.valueOfString;

/**
 * Fluent builder for configuring field definitions created by FormFieldDefinitionBuilderStart.
 * <p>
 * Provides methods to add CSS classes, configure privileges, add validators, set value converters,
 * and attach actions to fields. Extends FormFieldDefinitionBuilderStart to inherit field creation
 * methods, enabling a complete fluent API for declarative form construction.
 * </p>
 * <p>
 * This builder allows chaining configuration methods after creating a field. Once a field is created
 * via FormFieldDefinitionBuilderStart methods (like text(), dropdown(), etc.), this class provides
 * continuation methods to configure that field's behavior, appearance, and validation rules.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * FormFieldDefinitionBuilder builder = new FormFieldDefinitionBuilder("employeeForm", readPriv, writePriv);
 * builder.text("firstName")
 *     .additionalCss("form-control")
 *     .validate(Validator.notBlank())
 *     .additionalPrivileges(Privilege.canEditUser, Privilege.canEditUser);
 * 
 * builder.dropdown("department", "departments")
 *     .searchable()
 *     .withPreselectedValue("IT");
 * }</pre>
 * </p>
 * <p>
 * Thread-safety: Not thread-safe. Instances should be used in a single-threaded context during
 * form definition construction.
 * </p>
 *
 * @param <V> the field value type for type-safe validation functions. Can be changed via
 *           {@link #valueType(Class)} or {@link #valueConverters(Function, Function)} methods
 *
 * @see FormFieldDefinitionBuilderStart
 * @see FrontendMappingFieldDefinition
 * @see FrontendMappingDefinition
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class FormFieldDefinitionBuilder<V> extends FormFieldDefinitionBuilderStart {


    /**
     * Returns all accumulated field definitions as an array for FrontendMappingDefinition construction.
     * <p>
     * This method is typically called internally by {@link FrontendMappingDefinition} to retrieve
     * the complete set of field definitions after the builder chain completes. The returned array
     * contains all fields added via text(), dropdown(), manyToOne(), and other field creation methods.
     * </p>
     *
     * @return array of FrontendMappingFieldDefinition instances representing all defined form fields
     * @see FrontendMappingDefinition#createFrontendMappingDefinition(String, PrivilegeBase, PrivilegeBase, Function)
     */
    @Autocomplete
    public FrontendMappingFieldDefinition[] getFieldsAsArray() {
        return fields.toArray(new FrontendMappingFieldDefinition[fields.size()]);
    }
    /**
     * Returns all field-level validators as a Tuple2 array for validation execution.
     * <p>
     * Each tuple contains the field definition and its associated validation function. Validators
     * are added via the {@link #validate(Function)} method. During form validation, each validator
     * is executed against the corresponding field value, returning null for valid values or an
     * error code string for invalid values.
     * </p>
     *
     * @return array of Tuple2 pairs containing field definitions and their validator functions
     * @see #validate(Function)
     * @see AbstractForm#validateField(FrontendMappingFieldDefinition, Function, Object, org.springframework.validation.BindingResult)
     */
    @Autocomplete
    public Tuple2<FrontendMappingFieldDefinition, Function<?, String>>[] getFieldValidatorsAsArray() {
        return fieldValidators.toArray(new Tuple2[fieldValidators.size()]);
    }
    /**
     * Returns all form-level validators as a function array for cross-field validation.
     * <p>
     * Form-level validators validate relationships between multiple fields or perform validation
     * that requires access to the entire form state. Validators are added via the
     * {@link #validateForm(Function)} method. Each validator function receives the form instance
     * and returns a map of field names to error code strings, allowing validation errors to be
     * associated with specific fields.
     * </p>
     *
     * @return array of form-level validator functions returning field-to-error-code maps
     * @see #validateForm(Function)
     */
    @Autocomplete
    public Function<? extends Form, Map<String, String>>[]  getFormValidatorsAsArray() {
        return formValidators.toArray(new Function[formValidators.size()]);
    }
    /**
     * Constructs a new FormFieldDefinitionBuilder with default privileges for all fields.
     * <p>
     * The formName is used as a prefix for i18n key generation (e.g., formName.fieldName.label).
     * Default privileges are applied to all fields unless overridden via
     * {@link #additionalPrivileges(PrivilegeBase, PrivilegeBase)} for specific fields.
     * </p>
     *
     * @param formName the form identifier used for i18n key prefixing and form lookup
     * @param defaultReadPrivilege the privilege required to view fields by default
     * @param defaultWritePrivilege the privilege required to edit fields by default
     */
    @Autocomplete
    public FormFieldDefinitionBuilder(String formName, PrivilegeBase defaultReadPrivilege, PrivilegeBase defaultWritePrivilege) {
        super(formName, defaultReadPrivilege, defaultWritePrivilege);
    }
    /**
     * Adds CSS classes to the last created field for styling.
     * <p>
     * CSS classes are applied to the field's HTML input element for styling purposes. Multiple
     * classes can be specified separated by spaces (e.g., "form-control input-lg"). This method
     * modifies the most recently created field definition and returns this builder for method chaining.
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * builder.text("email").additionalCss("form-control required");
     * }</pre>
     * </p>
     *
     * @param additionalCss CSS class names to add to the field's HTML element
     * @return this builder for method chaining
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> additionalCss(String additionalCss) {
        fields.set(fields.size() - 1, lastField = createFormFieldDefinition(formName, lastField, additionalCss));
        return this;
    }

    /**
     * Marks the last created field as searchable in table filters.
     * <p>
     * When a field is marked searchable, it becomes available as a filter option in table views.
     * This is typically used for fields that should support user-driven filtering in data tables
     * or listing pages. The field will appear in the filter UI, allowing users to narrow down
     * displayed records based on this field's value.
     * </p>
     *
     * @return this builder for method chaining
     * @see FieldType
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> searchable() {
        fields.set(fields.size() - 1, lastField = createFormFieldDefinitionWithSearchEnabled(formName, lastField));
        return this;
    }

    /**
     * Sets SQL formula for computed database columns in dynamic entities.
     * <p>
     * SQL formulas define computed columns that derive their values from other columns or
     * expressions. This is used when generating dynamic entity table schemas via
     * DynamicEntityService. The formula is stored in field metadata and applied during DDL
     * generation. For example, a formula might compute a full name from first and last name fields.
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * builder.text("fullName").sqlFormula("first_name || ' ' || last_name");
     * }</pre>
     * </p>
     *
     * @param sqlFormula the SQL expression for the computed column
     * @return this builder for method chaining
     * @see com.openkoda.service.dynamicentity.DynamicEntityService
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> sqlFormula(String sqlFormula) {
        fields.set(fields.size() - 1, lastField = createFormFieldDefinitionWithSqlFormula(formName, lastField, sqlFormula));
        return this;
    }

    /**
     * Sets read and write privileges on the last created field, overriding defaults.
     * <p>
     * Privilege-based access control determines which users can view (read) and edit (write)
     * specific fields. These field-level privileges override the default privileges set in the
     * constructor. During form rendering, fields are hidden or disabled based on the current
     * user's privileges evaluated via PrivilegeHelper.
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * builder.text("salary")
     *     .additionalPrivileges(Privilege.canViewSalary, Privilege.canEditSalary);
     * }</pre>
     * </p>
     *
     * @param readPrivilege the privilege required to view this field
     * @param writePrivilege the privilege required to edit this field
     * @return this builder for method chaining
     * @see PrivilegeHelper
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> additionalPrivileges(PrivilegeBase readPrivilege, PrivilegeBase writePrivilege) {
        fields.set(fields.size() - 1, lastField = createFormFieldDefinition(formName, lastField, readPrivilege, writePrivilege));
        return this;
    }
    /**
     * Sets read and write privileges on the last created field using privilege name strings.
     * <p>
     * This is a convenience overload that accepts privilege names as strings and resolves them
     * via PrivilegeHelper.valueOfString(). Privilege names should match enum constant names from
     * the Privilege enum (e.g., "canReadBackend", "canManageOrgData").
     * </p>
     *
     * @param readPrivilege the privilege name required to view this field
     * @param writePrivilege the privilege name required to edit this field
     * @return this builder for method chaining
     * @see #additionalPrivileges(PrivilegeBase, PrivilegeBase)
     * @see PrivilegeHelper#valueOfString(String)
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> additionalPrivileges(String readPrivilege, String writePrivilege) {
        fields.set(fields.size() - 1, lastField = createFormFieldDefinition(formName, lastField, (PrivilegeBase) valueOfString(readPrivilege), (PrivilegeBase) valueOfString(writePrivilege)));
        return this;
    }
    /**
     * Sets runtime read and write privilege checks using custom predicate functions.
     * <p>
     * This overload allows dynamic privilege evaluation based on the current user and entity
     * context. The predicate functions receive the current OrganizationUser and the entity being
     * edited, returning true if the operation is permitted. This enables context-sensitive access
     * control that goes beyond static privilege-based checks.
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * builder.text("status")
     *     .additionalPrivileges(
     *         (user, entity) -> user.hasRole("ADMIN"),
     *         (user, entity) -> entity.getStatus() == Status.DRAFT
     *     );
     * }</pre>
     * </p>
     *
     * @param canReadCheck predicate function determining if the user can view the field
     * @param canWriteCheck predicate function determining if the user can edit the field
     * @return this builder for method chaining
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> additionalPrivileges(BiFunction<OrganizationUser, LongIdEntity, Boolean> canReadCheck, BiFunction<OrganizationUser, LongIdEntity, Boolean> canWriteCheck) {
        fields.set(fields.size() - 1, lastField = createFormFieldDefinition(formName, lastField, canReadCheck, canWriteCheck));
        return this;
    }

    /**
     * Sets runtime visibility check with strict read access enforcement.
     * <p>
     * Controls whether the field is displayed in the form based on a custom predicate function.
     * When the predicate returns false, the field is completely hidden from the UI. Sets strict
     * read access mode, meaning the field will not be rendered at all if access is denied (rather
     * than being rendered but disabled).
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * builder.text("internalNotes")
     *     .visible((user, entity) -> user.hasPrivilege(Privilege.canReadInternalData));
     * }</pre>
     * </p>
     *
     * @param canReadCheck predicate function determining if the field should be visible
     * @return this builder for method chaining
     * @see #enabled(BiFunction)
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> visible(BiFunction<OrganizationUser, LongIdEntity, Boolean> canReadCheck) {
        boolean strictWrite = lastField.isStrictWriteAccess();
        fields.set(fields.size() - 1, lastField = createFormFieldDefinition(formName, lastField, canReadCheck, lastField.canWriteCheck));
        lastField.setStrictReadAccess(true);
        lastField.setStrictWriteAccess(strictWrite);
        return this;
    }
    
    /**
     * Sets runtime editability check with strict write access enforcement.
     * <p>
     * Controls whether the field is editable based on a custom predicate function. When the
     * predicate returns false, the field is rendered but disabled (read-only). Sets strict write
     * access mode, meaning write access denial is enforced more strictly in the backend (attempts
     * to update the field will be rejected even if the form is submitted).
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * builder.text("approvalStatus")
     *     .enabled((user, entity) -> entity.getStatus() == Status.DRAFT 
     *                              && user.hasPrivilege(Privilege.canApprove));
     * }</pre>
     * </p>
     *
     * @param canWriteCheck predicate function determining if the field should be editable
     * @return this builder for method chaining
     * @see #visible(BiFunction)
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> enabled(BiFunction<OrganizationUser, LongIdEntity, Boolean> canWriteCheck) {
        boolean strictRead = lastField.isStrictReadAccess();
        fields.set(fields.size() - 1, lastField = createFormFieldDefinition(formName, lastField, lastField.canReadCheck, canWriteCheck));
        lastField.setStrictWriteAccess(true);
        lastField.setStrictReadAccess(strictRead);
        return this;
    }
    
    /**
     * Attaches a privileged action button next to the field.
     * <p>
     * Adds an action button (typically rendered next to or below the field) that triggers a specific
     * URL when clicked. The action is only visible to users with the specified privilege. This is
     * useful for field-related operations like "Generate Password", "Clear Value", or "Preview".
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * builder.text("apiKey")
     *     .additionalAction("generate.key.label", "/api/generate-key", Privilege.canManageApiKeys);
     * }</pre>
     * </p>
     *
     * @param actionLabelKey i18n key for the action button label
     * @param actionUrl the URL to invoke when the action button is clicked
     * @param additionalActionPrivilege privilege required to see and use this action
     * @return this builder for method chaining
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> additionalAction(String actionLabelKey, String actionUrl, PrivilegeBase additionalActionPrivilege) {
        fields.set(fields.size() - 1, lastField = createFormFieldDefinition(formName, lastField, actionLabelKey, actionUrl, additionalActionPrivilege));
        return this;
    }
    /**
     * Attaches a privileged action button next to the field using a privilege name string.
     * <p>
     * Convenience overload that accepts the privilege name as a string and resolves it via
     * PrivilegeHelper.valueOfString(). The privilege name should match an enum constant name
     * from the Privilege enum.
     * </p>
     *
     * @param actionLabelKey i18n key for the action button label
     * @param actionUrl the URL to invoke when the action button is clicked
     * @param privilegeNameAsString privilege name required to see and use this action
     * @return this builder for method chaining
     * @see #additionalAction(String, String, PrivilegeBase)
     * @see PrivilegeHelper#valueOfString(String)
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> additionalAction(String actionLabelKey, String actionUrl, String privilegeNameAsString) {
        fields.set(fields.size() - 1, lastField = createFormFieldDefinition(formName, lastField, actionLabelKey, actionUrl, (PrivilegeBase) PrivilegeHelper.valueOfString(privilegeNameAsString)));
        return this;
    }
    /**
     * Provides computed values for display-only fields.
     * <p>
     * The value supplier function is called during form rendering to generate field values that
     * are not stored directly in the DTO or entity. This is useful for computed or derived fields
     * that display information but don't correspond to a specific database column. The function
     * receives the form instance and returns the value to display.
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * builder.text("fullName")
     *     .valueSupplier(form -> form.getDto().getFirstName() + " " + form.getDto().getLastName());
     * }</pre>
     * </p>
     *
     * @param valueSupplier function computing the field value from the form instance
     * @return this builder for method chaining
     * @see ReflectionBasedEntityForm#populateSuppliedValuesFrom()
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> valueSupplier(Function<AbstractForm, Object> valueSupplier) {
        fields.set(fields.size() - 1, lastField = createFormFieldDefinition(formName, lastField, valueSupplier));
        return this;
    }
    /**
     * Sets bidirectional conversion functions between DTO and entity representations.
     * <p>
     * Value converters enable mapping between different data types or formats used in the DTO
     * (form representation) versus the entity (database representation). The toEntityValue
     * converter is applied when populating the entity from the DTO (form submission). The
     * toDtoValue converter is applied when populating the DTO from the entity (form display).
     * </p>
     * <p>
     * This method also changes the builder's type parameter to V2, enabling type-safe validation
     * function chaining after conversion.
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * builder.text("amount")
     *     .valueConverters(
     *         str -> new BigDecimal(str),      // DTO (String) to entity (BigDecimal)
     *         bd -> bd.toPlainString()         // entity (BigDecimal) to DTO (String)
     *     );
     * }</pre>
     * </p>
     *
     * @param <V2> the new value type after conversion
     * @param toEntityValue converter from DTO value (type V) to entity value (type V2)
     * @param toDtoValue converter from entity value to DTO value for display
     * @return builder with updated type parameter V2 for method chaining
     * @see ReflectionBasedEntityForm#getConverter(FrontendMappingFieldDefinition)
     */
    @Autocomplete
    public <V2> FormFieldDefinitionBuilder<V2> valueConverters(Function<V, V2> toEntityValue, Function<Object, Object> toDtoValue) {
        fields.set(fields.size() - 1, lastField = createFormFieldDefinition(formName, lastField, toEntityValue, toDtoValue));
        return (FormFieldDefinitionBuilder<V2>) this;
    }
    /**
     * Configures the description field for foreign key dropdowns.
     * <p>
     * When using dropdown or many_to_one fields with datalists, this method specifies which field
     * from the referenced entity should be used as the display label. The source parameter
     * typically refers to a field name like "name" or "description" from the referenced entity.
     * This modifies the datalist field (created immediately before the current field) to use the
     * specified source for display labels.
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * builder.datalist("departments", (dto, repo) -> repo.findAllDepartments())
     *     .dropdown("departmentId", "departments")
     *     .referenceDescriptionSource("departmentName");
     * }</pre>
     * </p>
     *
     * @param source the field name from the referenced entity to use as display label
     * @return this builder for method chaining
     * @see #dropdown(String, String)
     * @see #many_to_one(String, String)
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> referenceDescriptionSource(String source) {
        FrontendMappingFieldDefinition datalistField = fields.get(fields.size() - 2);
        FrontendMappingFieldDefinition dropdownField = fields.get(fields.size() - 1);
        fields.set(fields.size() - 2, createFormFieldDefinition(formName, datalistField, (f, d) -> d.dictionary(dropdownField.referencedEntityKey, source)));
        return this;
    }
    /**
     * Adds a field-level validator returning an error code string.
     * <p>
     * Validators are executed during form validation to check field values against business rules.
     * The validator function receives the field value and returns null if the value is valid, or
     * an error code string if validation fails. The error code is used to look up the localized
     * error message for display to the user.
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * builder.text("email")
     *     .validate(Validator.notBlank())
     *     .validate(val -> val.contains("@") ? null : "invalid.email.format");
     * }</pre>
     * </p>
     *
     * @param validatorReturningErrorCode validation function returning null for valid values
     *                                    or an error code string for invalid values
     * @return this builder for method chaining
     * @see Validator
     * @see AbstractForm#validateField(FrontendMappingFieldDefinition, Function, Object, org.springframework.validation.BindingResult)
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> validate(Function<V, String> validatorReturningErrorCode) {
        fieldValidators.add(Tuples.of(lastField, validatorReturningErrorCode));
        return this;
    }
    /**
     * Sets the default selected value for dropdowns and select lists.
     * <p>
     * The preselected value is used when rendering new forms (not editing existing entities) to
     * set a default selection. This is useful for dropdown fields where a sensible default should
     * be pre-selected, reducing user effort. The value should match one of the keys from the
     * datalist options.
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * builder.dropdown("country", "countries")
     *     .withPreselectedValue("US");
     * }</pre>
     * </p>
     *
     * @param preselectedValue the default value to pre-select in the dropdown
     * @return this builder for method chaining
     * @see #dropdown(String, String)
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> withPreselectedValue(String preselectedValue) {
        lastField.preselectedValue = preselectedValue;
        return this;
    }
    /**
     * Adds a form-level validator returning a field-to-error-code map.
     * <p>
     * Form-level validators perform cross-field validation where multiple field values must be
     * checked together. The validator function receives the entire form instance and returns a
     * map of field names to error code strings. If the map is empty or null, validation passes.
     * Otherwise, the error codes are associated with the corresponding fields for display.
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * builder.validateForm(form -> {
     *     Map<String, String> errors = new HashMap<>();
     *     if (form.getDto().getStartDate().isAfter(form.getDto().getEndDate())) {
     *         errors.put("endDate", "end.date.before.start.date");
     *     }
     *     return errors;
     * });
     * }</pre>
     * </p>
     *
     * @param validatorReturningRejectedFieldToErrorCodeMap validator function returning a map
     *                                                       of field names to error codes, or
     *                                                       empty/null for valid forms
     * @return this builder for method chaining
     */
    @Autocomplete
    public FormFieldDefinitionBuilder<V> validateForm(Function<? extends Form, Map<String, String>> validatorReturningRejectedFieldToErrorCodeMap) {
        formValidators.add(validatorReturningRejectedFieldToErrorCodeMap);
        return this;
    }
    /**
     * Changes the builder's generic type parameter for type-safe builder chaining.
     * <p>
     * This is a type inference helper method that allows changing the value type parameter V
     * without actually modifying any field configuration. It's useful when the field's actual
     * type differs from the initial type parameter, enabling subsequent validate() calls to
     * receive correctly-typed arguments.
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * builder.text("age")
     *     .valueType(Integer.class)
     *     .validate(age -> age >= 0 && age <= 150 ? null : "invalid.age.range");
     * }</pre>
     * </p>
     *
     * @param <VT> the new value type parameter
     * @param c the class representing the new value type (used for type inference)
     * @return builder cast to FormFieldDefinitionBuilder&lt;VT&gt; for type-safe chaining
     */
    @Autocomplete
    public <VT> FormFieldDefinitionBuilder<VT> valueType(Class<VT> c) {
        return (FormFieldDefinitionBuilder<VT>)this;
    }



}