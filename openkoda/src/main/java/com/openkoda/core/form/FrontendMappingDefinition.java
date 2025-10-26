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

import com.openkoda.model.PrivilegeBase;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.util.function.Tuple2;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builder interface for declarative form field definitions and frontend rendering configuration.
 * <p>
 * Provides a fluent API for defining form fields, validation rules, and display properties.
 * This class generates {@link FrontendMappingFieldDefinition} instances used by form classes
 * to declare their structure for rendering, validation, and data binding.
 * </p>
 * <p>
 * The FrontendMappingDefinition represents an immutable configuration produced by the
 * {@link FormFieldDefinitionBuilder} DSL. It encapsulates field definitions, validators,
 * and metadata needed for form lifecycle operations (populateFrom, validate, populateTo).
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * FrontendMappingDefinition userForm = FrontendMappingDefinition.createFrontendMappingDefinition(
 *     "userForm", readPrivilege, writePrivilege,
 *     b -> b.text("name").dropdown("role", datalist)
 * );
 * }</pre>
 * </p>
 *
 * @see FormFieldDefinitionBuilder
 * @see FormFieldDefinitionBuilderStart
 * @see FrontendMappingFieldDefinition
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class FrontendMappingDefinition {

    /**
     * The complete collection of field definitions for form rendering.
     * <p>
     * Contains all fields configured through the builder, including base form fields
     * when using form extension/composition patterns. Fields are used for frontend
     * rendering, validation execution, and data binding operations.
     * </p>
     */
    public final FrontendMappingFieldDefinition[] fields;
    
    /**
     * The form identifier used for internationalization (i18n) key generation.
     * <p>
     * This name serves as the base key for form-related i18n lookups and is used
     * to generate the {@link #formLabel} key. It also provides the mapping key
     * for form lookups via {@link #getMappingKey()}.
     * </p>
     */
    public final String name;
    
    /**
     * The internationalization (i18n) key for the form title.
     * <p>
     * Generated as {@code name + ".label"} during construction. This key is used
     * by the frontend rendering engine to display the localized form title.
     * </p>
     */
    public final String formLabel;

    /**
     * Field-level validation functions storing field and validator pairs.
     * <p>
     * Each tuple contains a {@link FrontendMappingFieldDefinition} and a validation
     * function that accepts the field value and returns an error message string
     * (or null if validation passes). These validators are executed during the
     * form's validate() lifecycle phase.
     * </p>
     */
    public final Tuple2<FrontendMappingFieldDefinition, Function<Object, String>>[] fieldValidators;
    
    /**
     * Form-level validation functions returning error maps.
     * <p>
     * Each function accepts the entire form instance and returns a map of field names
     * to error messages. Form-level validators can perform cross-field validation
     * logic that depends on multiple field values. Executed after field-level
     * validators during the validate() lifecycle phase.
     * </p>
     */
    public final Function<? extends Form, Map<String, String>>[]  formValidators;

    /**
     * Static ReCaptcha site key for integration with Google ReCaptcha verification.
     * <p>
     * When configured, this key enables ReCaptcha protection on forms. The value
     * is typically set during application initialization from configuration properties.
     * </p>
     */
    public static String siteKey;

    /**
     * Constructs an immutable form definition with fields, validators, and metadata.
     * <p>
     * Creates a complete form configuration that includes field definitions for rendering,
     * field-level validation functions, and form-level validation functions. The form label
     * is automatically generated as {@code name + ".label"} for i18n lookups.
     * </p>
     *
     * @param name the form identifier used for i18n key generation and mapping lookups (must not be null)
     * @param fields the complete array of field definitions for this form (must not be null)
     * @param fieldValidators array of field-validator tuples for field-level validation (may be empty but not null)
     * @param formValidators array of form-level validation functions for cross-field validation (may be empty but not null)
     */
    public FrontendMappingDefinition(String name, FrontendMappingFieldDefinition[] fields, Tuple2<FrontendMappingFieldDefinition, Function<Object, String>>[] fieldValidators, Function<? extends AbstractForm, Map<String, String>>[] formValidators) {
        this.name = name;
        this.fields = fields;
        this.fieldValidators = fieldValidators;
        this.formValidators = formValidators;
        this.formLabel = name + ".label";
    }

    /**
     * Creates a form definition using a builder function for declarative field configuration.
     * <p>
     * This factory method provides a fluent API for defining form structures. The builder
     * function receives a {@link FormFieldDefinitionBuilderStart} and returns a configured
     * {@link FormFieldDefinitionBuilder}. Default read and write privileges are applied
     * to all fields unless overridden at the field level.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * FrontendMappingDefinition def = createFrontendMappingDefinition(
     *     "userForm", READ_PRIVILEGE, WRITE_PRIVILEGE,
     *     b -> b.text("username").email("email").dropdown("role", roleList)
     * );
     * }</pre>
     * </p>
     *
     * @param formName the form identifier for i18n and mapping lookups
     * @param defaultReadPrivilege the default privilege required to read form fields (may be null for no restriction)
     * @param defaultWritePrivilege the default privilege required to modify form fields (may be null for no restriction)
     * @param builder the builder function that defines form fields and validators
     * @return a new immutable FrontendMappingDefinition instance
     * @see FormFieldDefinitionBuilder
     */
    public static FrontendMappingDefinition createFrontendMappingDefinition(
            String formName,
            PrivilegeBase defaultReadPrivilege,
            PrivilegeBase defaultWritePrivilege,
            Function<FormFieldDefinitionBuilderStart, FormFieldDefinitionBuilder> builder) {
        FormFieldDefinitionBuilder ffdb = builder.apply(new FormFieldDefinitionBuilder(formName, defaultReadPrivilege, defaultWritePrivilege));
        return new FrontendMappingDefinition(formName, ffdb.getFieldsAsArray(), ffdb.getFieldValidatorsAsArray(), ffdb.getFormValidatorsAsArray());
    }

    /**
     * Creates a form definition by extending a base form with additional fields.
     * <p>
     * This factory method supports form extension and composition patterns. The base form
     * fields are combined with fields defined in the builder function, enabling reuse of
     * common field configurations across multiple forms. Base fields are prepended to the
     * fields defined in the builder.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * FrontendMappingDefinition extendedForm = createFrontendMappingDefinition(
     *     "extendedUserForm", READ_PRIVILEGE, WRITE_PRIVILEGE,
     *     baseUserFormFields,
     *     b -> b.text("additionalField").checkbox("isActive")
     * );
     * }</pre>
     * </p>
     *
     * @param formName the form identifier for i18n and mapping lookups
     * @param defaultReadPrivilege the default privilege required to read form fields (may be null for no restriction)
     * @param defaultWritePrivilege the default privilege required to modify form fields (may be null for no restriction)
     * @param baseFormFields array of field definitions to prepend to the builder-defined fields (must not be null)
     * @param builder the builder function that defines additional form fields and validators
     * @return a new immutable FrontendMappingDefinition instance with combined fields
     * @see FormFieldDefinitionBuilder
     */
    public static FrontendMappingDefinition createFrontendMappingDefinition(
            String formName,
            PrivilegeBase defaultReadPrivilege,
            PrivilegeBase defaultWritePrivilege,
            FrontendMappingFieldDefinition[] baseFormFields,
            Function<FormFieldDefinitionBuilderStart, FormFieldDefinitionBuilder> builder) {
        FormFieldDefinitionBuilder ffdb = builder.apply(new FormFieldDefinitionBuilder(formName, defaultReadPrivilege, defaultWritePrivilege));
        return new FrontendMappingDefinition(formName, ArrayUtils.addAll(baseFormFields, ffdb.getFieldsAsArray()), ffdb.getFieldValidatorsAsArray(), ffdb.getFormValidatorsAsArray());
    }

    /**
     * Returns the complete array of field definitions for this form.
     * <p>
     * This method provides access to all field configurations including those from
     * base forms in extension scenarios. The returned array is used for form rendering,
     * validation execution, and data binding operations.
     * </p>
     *
     * @return an array of {@link FrontendMappingFieldDefinition} objects (never null, may be empty)
     */
    public FrontendMappingFieldDefinition[] getFields() {
        return fields;
    }

    /**
     * Filters and returns field definitions matching specified field types.
     * <p>
     * This method is useful for selectively processing fields based on their type,
     * such as retrieving only text fields or dropdown fields for specialized rendering
     * or validation logic.
     * </p>
     *
     * @param types list of {@link FieldType} values to filter by (must not be null)
     * @return list of matching field definitions (never null, may be empty)
     */
    public List<FrontendMappingFieldDefinition> getFieldsByType(List<FieldType> types) {
        return Arrays.stream(fields).filter(field -> types.contains(field.getType())).collect(Collectors.toList());
    }

    /**
     * Returns plain field names for fields matching specified types.
     * <p>
     * Extracts the plain names (without path qualifiers) from fields matching the
     * given types. This is useful for generating field name lists for validation
     * or data binding operations.
     * </p>
     *
     * @param types list of {@link FieldType} values to filter by (must not be null)
     * @return list of plain field names as objects (never null, may be empty)
     */
    public List<Object> getFieldsNamesByType(List<FieldType> types) {
        return Arrays.stream(fields).filter(field -> types.contains(field.getType())).map(FrontendMappingFieldDefinition::getPlainName).collect(Collectors.toList());
    }

    /**
     * Returns fields with database column types for dynamic entity generation.
     * <p>
     * Filters fields to include only those with defined database types (where
     * {@code field.getType().getDbType() != null}). This method is essential for
     * DDL generation in the dynamic entity system, as it identifies fields that
     * should be mapped to database columns.
     * </p>
     *
     * @return array of field definitions with database types (never null, may be empty)
     */
    public FrontendMappingFieldDefinition[] getDbTypeFields(){
        return Arrays.stream(getFields())
                .filter(s -> s.getType() != null && s.getType().getDbType() != null)
                .toArray(FrontendMappingFieldDefinition[]::new);
    }

    /**
     * Creates a name-to-field mapping for fields with database types.
     * <p>
     * Generates a map where keys are field names and values are field definitions
     * for all fields with defined database types. This mapping is used during DDL
     * generation for dynamic entities, enabling efficient field lookup by name during
     * schema creation and modification.
     * </p>
     *
     * @return map of field names to field definitions with database types (never null, may be empty)
     */
    public Map<String, FrontendMappingFieldDefinition> getFieldNameDbTypeMap(){
        return Arrays.stream(getFields())
                .filter(s -> s.getType() != null && s.getType().getDbType() != null)
                .collect(Collectors.toMap(FrontendMappingFieldDefinition::getName, field -> field));
    }

    /**
     * Returns names of fields that store database values.
     * <p>
     * Filters fields to include only those that have a value type (where
     * {@code field.getType().hasValue()} returns true). These are fields that
     * actually store data values in the database, as opposed to structural or
     * display-only fields.
     * </p>
     *
     * @return array of field names for valued fields (never null, may be empty)
     */
    public String[] getNamesOfValuedTypeFields(){
        return Arrays.stream(getFields())
                .filter(s -> s.getType() != null && s.getType().hasValue())
                .map(FrontendMappingFieldDefinition::getName)
                .toArray(String[]::new);
    }

    /**
     * Finds a field definition by name with special handling for many-to-one relationships.
     * <p>
     * Searches for a field matching the exact plain name. If not found and the field name
     * contains a dot (indicating a nested property path like "relation.property"), this method
     * attempts to find a many_to_one field whose ID field would match the path. For example,
     * searching for "user.name" will match a many_to_one field named "userId" and create
     * a derived text field definition.
     * </p>
     * <p>
     * This special logic enables form binding to work with nested entity properties while
     * the underlying field stores only the foreign key ID.
     * </p>
     *
     * @param fieldName the field name to search for (plain name or dotted path)
     * @return the matching field definition, a derived field for many_to_one paths, or null if not found
     */
    public FrontendMappingFieldDefinition findField(String fieldName) {
        for (FrontendMappingFieldDefinition f : fields) {
            if (f.getPlainName().equals(fieldName)) {
                return f;
            } else if (f.getType().equals(FieldType.many_to_one) && fieldName.contains(".") && (StringUtils.substringBefore(fieldName, ".") + "Id").equals(f.getPlainName())) {
                return FrontendMappingFieldDefinition.createFormFieldDefinition(name, fieldName, FieldType.text, f.readPrivilege, f.writePrivilege);
            }
        }
        return null;
    }

    /**
     * Returns the lowercase form name for mapping lookups.
     * <p>
     * Converts the form name to lowercase for case-insensitive form registry lookups.
     * This key is used by the form mapping system to locate form definitions at runtime.
     * </p>
     *
     * @return the lowercase form name (never null)
     */
    public String getMappingKey() {
        return name.toLowerCase();
    }
}
