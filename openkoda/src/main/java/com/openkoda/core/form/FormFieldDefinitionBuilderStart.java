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
import com.openkoda.model.OpenkodaModule;
import com.openkoda.model.Organization;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.repository.SecureEntityDictionaryRepository;
import com.openkoda.uicomponent.annotation.Autocomplete;
import reactor.util.function.Tuple2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.openkoda.core.form.FieldType.*;
import static com.openkoda.core.form.FrontendMappingFieldDefinition.createFormFieldDefinition;
import static com.openkoda.core.form.FrontendMappingFieldDefinition.createNonDtoFormFieldDefinition;

/**
 * Fluent builder DSL entry point for declarative form field definitions.
 * <p>
 * Provides methods to create text inputs, dropdowns, checkboxes, file uploads, code editors,
 * and other form field types. Each method returns a FormFieldDefinitionBuilder for further
 * configuration chaining, enabling a fluent API for form construction.
 * </p>
 * <p>
 * This builder accumulates field definitions in a list, along with field-level and form-level
 * validators. Field definitions specify both database schema (column type, constraints) and
 * presentation layer rendering (input type, validation rules).
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * FormFieldDefinitionBuilderStart builder = new FormFieldDefinitionBuilderStart("userForm", readPriv, writePriv);
 * builder.text("firstName")
 *        .dropdown("role", "rolesList")
 *        .checkbox("active");
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FormFieldDefinitionBuilder
 * @see FrontendMappingDefinition
 */
public class FormFieldDefinitionBuilderStart {
    /**
     * Prefix used for datalist field names to distinguish them from regular form fields.
     * Datalist fields store reusable value lists for populating dropdowns and select inputs.
     */
    public static final String DATALIST_PREFIX = "__datalist_";
    
    /**
     * Accumulated list of field definitions for this form.
     * Each entry specifies database column type, presentation layer rendering, and access privileges.
     */
    protected final List<FrontendMappingFieldDefinition> fields = new ArrayList<>();
    
    /**
     * Field-level validation functions paired with their target field definitions.
     * Applied during form validation to check individual field values.
     */
    protected final List<Tuple2<FrontendMappingFieldDefinition, Function<?, String>>> fieldValidators = new ArrayList<>();
    
    /**
     * Form-level validation functions that operate on the entire form instance.
     * Applied after field validators to check cross-field constraints.
     */
    protected final List<Function<? extends Form, Map<String, String>>>  formValidators = new ArrayList<>();
    
    /**
     * Form identifier used for internationalization lookups and field name prefixing.
     */
    protected final String formName;
    
    /**
     * Default read privilege applied to all fields unless overridden at the field level.
     * Controls visibility of field values in the presentation layer.
     */
    protected final PrivilegeBase defaultReadPrivilege;
    
    /**
     * Default write privilege applied to all fields unless overridden at the field level.
     * Controls whether users can modify field values in the presentation layer.
     */
    protected final PrivilegeBase defaultWritePrivilege;
    
    /**
     * Constant field name for ReCaptcha challenge fields.
     * Used when adding CAPTCHA validation to forms.
     */
    protected static String RECAPTCHA = "ReCaptcha";

    /**
     * Tracks the most recently added field definition for builder chaining.
     * Allows subsequent builder method calls to configure the last field added.
     * Ignored during JSON serialization.
     */
    @JsonIgnore
    protected FrontendMappingFieldDefinition lastField;

    /**
     * Constructs a new form field definition builder with specified default privileges.
     * <p>
     * The default privileges are applied to all fields created by this builder unless
     * explicitly overridden at the field level. This allows centralized privilege
     * management for forms where most fields share the same access control requirements.
     * </p>
     *
     * @param formName the form identifier used for i18n lookups and field name prefixing
     * @param defaultReadPrivilege the default privilege required to view field values (can be null)
     * @param defaultWritePrivilege the default privilege required to modify field values (can be null)
     */
    public FormFieldDefinitionBuilderStart (
            String formName,
            PrivilegeBase defaultReadPrivilege,
            PrivilegeBase defaultWritePrivilege) {
        this.formName = formName;
        this.defaultReadPrivilege = defaultReadPrivilege;
        this.defaultWritePrivilege = defaultWritePrivilege;
    }
    /**
     * Creates a reusable value list for populating dropdowns and select inputs (presentation layer only).
     * <p>
     * Datalists store key-value pairs that can be referenced by multiple dropdown fields. The supplier
     * function is invoked at form rendering time with access to the current entity and repository.
     * </p>
     *
     * @param datalistId unique identifier for this datalist, referenced by dropdown fields
     * @param datalistSupplier function that returns a map of values, accepting current DTO/entity and repository
     * @return builder for further configuration chaining
     */
    @Autocomplete(doc = """
            Create list of values which can be later used to populate e.g. dropdowns. (Presentation layer impact only). Examples:
            <br/>Simple data list with fixed values:<br/>
            <code>
            .datalist("weekendDays", d => d.toLinkedMap(["Saturday","Sunday"]))
            .dropdown("nonWorking", "weekendDays")
            </code>
            <br/>Simple data list with fixed values:<br/>
            <code>
            .datalist("workingDays", a.services.data.getRepository("weekDays","ALL"))
            .dropdown("working", "workingDays")
            </code>
            """)
    public FormFieldDefinitionBuilder<Object> datalist(String datalistId, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier) {
        fields.add(lastField = createFormFieldDefinition(formName, DATALIST_PREFIX + datalistId, datalistId, datalist, datalistSupplier, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    /**
     * Creates a reusable value list for populating dropdowns (presentation layer only).
     * <p>
     * Simplified variant that accepts a supplier function with access only to the repository,
     * without the current DTO/entity context.
     * </p>
     *
     * @param datalistId unique identifier for this datalist, referenced by dropdown fields
     * @param datalistSupplier function that returns a map of values from repository queries
     * @return builder for further configuration chaining
     */
    @Autocomplete(doc = """
            Create list of values which can be later used to populate e.g. dropdowns. (Presentation layer impact only). Examples:
            <br/>Simple data list with fixed values:<br/>
            <code>
            .datalist("weekendDays", d => d.toLinkedMap(["Saturday","Sunday"]))
            .dropdown("nonWorking", "weekendDays")
            </code>
            <br/>Simple data list with fixed values:<br/>
            <code>
            .datalist("workingDays", a.services.data.getRepository("weekDays","ALL"))
            .dropdown("working", "workingDays")
            <code>
            """)
    public FormFieldDefinitionBuilder<Object> datalist(String datalistId, Function<SecureEntityDictionaryRepository, Object> datalistSupplier) {
        fields.add(lastField = createFormFieldDefinition(formName, DATALIST_PREFIX + datalistId, datalistId, datalist, datalistSupplier, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    /**
     * Creates a string column in the database and text input field on the form.
     * <p>
     * Generates a VARCHAR database column and renders as an HTML text input element.
     * The field value can be optionally computed via SQL formula instead of stored directly.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration (validation, display options, SQL formulas)
     */
    @Autocomplete(doc = """
            Create string column in the database and add simple text input to the form. Examples:
            <br/>Simple text input both in the form and in the table:<br/>
            <code>
            .text("firstName")
            </code>
            <br/>Simple text calculated from sql formula:<br/>
            <code>
            .checkbox("username").sqlFormula("select first_name ||' '|| last_name from customer")
            </code>
            """)
    public FormFieldDefinitionBuilder<String> text(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, text, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    /**
     * Creates a string column in the database and textarea input field on the form.
     * <p>
     * Generates a TEXT or VARCHAR database column and renders as an HTML textarea element
     * for multi-line text input.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration (rows, max length, validation)
     */
    @Autocomplete(doc = """
            Create string column in the database and add textarea input to the form. Examples:
            <br/>Simple textarea input both in the form and in the table:<br/>
            <code>
            .textarea("notes")
            </code>
            """)
    public FormFieldDefinitionBuilder<String> textarea(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, textarea, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    /**
     * Creates a boolean column in the database and checkbox input field on the form.
     * <p>
     * Generates a BOOLEAN database column and renders as an HTML checkbox element.
     * The field value can be optionally computed via SQL formula returning boolean.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration (default value, SQL formula)
     */
    @Autocomplete(doc = """
            Create boolean column in the database and add checkbox input to the form. Examples:
            <br/>Simple checkbox both in the form and in the table:<br/>
            <code>
            .checkbox("flag")
            </code>
            <br/>Simple checkbox calculated from sql formula:<br/>
            <code>
            .checkbox("flag").sqlFormula("true")
            </code>
            """)
    public FormFieldDefinitionBuilder<Boolean> checkbox(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, checkbox, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Boolean>)this;
    }
    /**
     * Creates a timestamp column in the database and datetime picker field on the form.
     * <p>
     * Generates a TIMESTAMP database column and renders as a datetime picker widget
     * for selecting both date and time values.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration (date format, time zone, validation)
     */
    @Autocomplete(doc = """
            Create date with time column in the database and add date time picker input to the form. Examples:
            <br/>Simple datetime both in the form and in the table:<br/>
            <code>
            .datetime("busArrivalDateTime")
            </code>
            <br/>Simple datetime calculated from sql formula:<br/>
            <code>
            .datetime("currentTime").sqlFormula("select CURRENT_TIME")
            </code>
            """)
    public FormFieldDefinitionBuilder<LocalDateTime> datetime(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, datetime, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<LocalDateTime>)this;
    }
    /**
     * Creates a date column in the database and date picker field on the form.
     * <p>
     * Generates a DATE database column and renders as a date picker widget
     * for selecting date values without time component.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration (date format, min/max dates, validation)
     */
    @Autocomplete(doc = """
            Create date column in the database and add date picker input to the form. Examples:
            <br/>Simple date both in the form and in the table:<br/>
            <code>
            .date("busArrivalDate")
            </code>
            <br/>Simple date calculated from sql formula:<br/>
            <code>
            .date("currentDate").sqlFormula("select CURRENT_DATE")
            </code>
            """)
    public FormFieldDefinitionBuilder<LocalDate> date(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, date, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<LocalDate>)this;
    }
    /**
     * Creates a numeric column in the database and numeric input field on the form.
     * <p>
     * Generates a NUMERIC or INTEGER database column and renders as an HTML number input
     * element with validation for numeric values only.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration (min/max values, decimal places, validation)
     */
    @Autocomplete(doc = """
            Create numeric column in the database and add numeric input to the form. Examples:
            <br/>Simple numeric both in the form and in the table:<br/>
            <code>
            .number(455)
            </code>
            <br/>Simple numeric calculated from sql formula:<br/>
            <code>
            .number("quantity").sqlFormula("select count(*) from products")
            </code>
            """)
    public FormFieldDefinitionBuilder<Number> number(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, number, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Number>)this;
    }
    /**
     * Creates a non-null string column in the database and dropdown select field on the form.
     * <p>
     * Generates a VARCHAR database column with NOT NULL constraint and renders as an HTML
     * select element populated from the specified datalist. Value selection is required.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @param datalistId identifier of the datalist containing available options
     * @return builder for further configuration (default value, validation)
     * @see #datalist(String, BiFunction)
     */
    @Autocomplete(doc = "Create non null string column in the database and select input with required value on presentation layer." +
            "This action may be preceded by appropriate data list creation. See also 'datalist'")
    public FormFieldDefinitionBuilder<String> dropdown(String fieldName, String datalistId) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, datalistId, dropdown, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }


//    public FormFieldDefinitionBuilder<Object> oneToMany(String fieldName, String urlToAddObjectToList, String datalistId, Function<AbstractForm, Object> datalistSupplier, String componentFragmentName) {
//        fields.add( lastField = createFormFieldDefinition(formName, fieldName, one_to_many, defaultReadPrivilege, defaultWritePrivilege, urlToAddObjectToList, datalistId, datalistSupplier, componentFragmentName));
//        return (FormFieldDefinitionBuilder<Object>)this;
//    }

//    public FormFieldDefinitionBuilder<Object> oneToMany(String fieldName, String urlToAddObjectToList, Function<Object, Object> valueSupplier) {
//        fields.add(lastField = createFormFieldDefinition(formName, fieldName, one_to_many, defaultReadPrivilege, defaultWritePrivilege, urlToAddObjectToList, valueSupplier, "forms::default-entity-tile"));
//        return (FormFieldDefinitionBuilder<Object>)this;
//    }
    @Autocomplete(doc = "Create nullable string column in the database and select input with optional value on presentation layer." +
            "This action may be preceded by appropriate data list creation. See also 'datalist'")
    public FormFieldDefinitionBuilder<String> dropdown(String fieldName, String datalistId, boolean allowNull) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, datalistId, allowNull, dropdown, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    @Autocomplete(doc = "Create non null string column in the database and select input with required value on presentation layer.")
    public FormFieldDefinitionBuilder<String> dropdown(String fieldName, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, null, dropdown, datalistSupplier, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    @Autocomplete(doc = "Create nullable string column in the database and select input with optional value on presentation layer." +
            "This action may be preceded by appropriate data list creation. See also 'datalist'")
    public FormFieldDefinitionBuilder<String> dropdown(String fieldName, String datalistId, Boolean allowNull) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, datalistId, allowNull, dropdown, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    @Autocomplete(doc = "Create non null string column in the database and select input with required value by default disabled on presentation layer." +
            "This action may be preceded by appropriate data list creation. See also 'datalist'")
    public FormFieldDefinitionBuilder<String> dropdownWithDisable(String fieldName, String datalistId) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, datalistId, dropdown_with_disable, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    @Autocomplete(doc = "Create non null string column in the database and select input with required value by default disabled on presentation layer.")
    public FormFieldDefinitionBuilder<String> dropdownWithDisable(String fieldName, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, null, dropdown_with_disable, datalistSupplier, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
//    Dropdown element for section. Section fields to show/hide are selected by the matching css class, same as set for section_with_dropdown.
    @Autocomplete(doc = "Create non null string column in the database and select input with optional value on presentation layer." +
            "This action may be preceded by appropriate data list creation. See also 'datalist'" +
            "Section fields to show or hide on the basis of dropdown value are selected by their matching additional css class")
    public FormFieldDefinitionBuilder<String> sectionWithDropdown(String fieldName, String datalistId) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, datalistId, true, section_with_dropdown, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    @Autocomplete(doc = "Create non null string column in the database and select input with optional value on presentation layer." +
            "Section fields to show or hide on the basis of dropdown value are selected by their matching additional css class")
    public FormFieldDefinitionBuilder<String> sectionWithDropdown(String fieldName, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, null, true, section_with_dropdown, null, datalistSupplier, defaultReadPrivilege, defaultWritePrivilege, null));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    @Autocomplete(doc = "Create non null string column in the database and select input on presentation layer. Use when there is no dto available for this form object." +
            "This action may be preceded by appropriate data list creation. See also 'datalist'")
    public FormFieldDefinitionBuilder<String> dropdownNonDto(String fieldName, String datalistId) {
        fields.add(lastField = createNonDtoFormFieldDefinition(formName, fieldName, datalistId, dropdown, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    /**
     * Creates a checkbox list field on the form (presentation layer only).
     * <p>
     * Renders as a list of checkboxes populated from the datalist supplier function.
     * Multiple options can be selected. Selected values are stored as comma-separated string.
     * </p>
     *
     * @param fieldName the form field identifier
     * @param datalistSupplier function providing checkbox options with labels and values
     * @return builder for further configuration (layout, validation)
     */
    @Autocomplete(doc = "Create checkbox list element. (only on presentation layer) " +
            "Provide list values as a second argument (BiFunction datalistSupplier). ")
    public FormFieldDefinitionBuilder<String> checkboxList(String fieldName, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, fieldName, checkbox_list, datalistSupplier, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    @Autocomplete(doc = "Create checkbox list element. (only on presentation layer) " +
            "This action may be preceded by appropriate data list creation. See also 'datalist'")
    public FormFieldDefinitionBuilder<String> checkboxList(String fieldName, String datalistId) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, datalistId, checkbox_list, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }

    @Autocomplete(doc = "Create checkbox list element. Checkboxes are grouped into columns by their 'category' property. (only on presentation layer) " +
            "This action may be preceded by appropriate data list creation. See also 'datalist'")
    public FormFieldDefinitionBuilder<String> checkboxListGrouped(String fieldName, String datalistId) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, datalistId, checkbox_list_grouped, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }

    /**
     * Creates a string column in the database and multiselect dropdown field on the form.
     * <p>
     * Generates a VARCHAR database column storing comma-separated selected values and renders
     * as a multiselect dropdown allowing multiple option selection from the datalist.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @param datalistId identifier of the datalist containing available options
     * @return builder for further configuration (max selections, validation)
     * @see #datalist(String, BiFunction)
     */
    @Autocomplete(doc = "Create non null string column in the database and multiselect dropdown on presentation layer. " +
            "Selected values are stored in the database as a comma-separated string." +
            "This action may be preceded by appropriate data list creation. See also 'datalist'")
    public FormFieldDefinitionBuilder<String> multiselect(String fieldName, String datalistId) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, datalistId, multiselect, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }

    /**
     * Creates a nullable foreign key column in the database and dropdown field on the form.
     * <p>
     * Generates a BIGINT database column for storing entity IDs and renders as a dropdown
     * populated with records from the referenced entity table. Establishes a many-to-one
     * relationship between the current entity and the referenced entity.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @param referencedEntityKey the entity key identifying the referenced entity type
     * @return builder for further configuration (cascade options, fetch strategy)
     */
    @Autocomplete(doc = "Create nullable numeric column in the database and a dropdown element on the presentation layer." + 
            "Values available in the dropdown are loaded from the database as the referenced entity key table records. ")
    public FormFieldDefinitionBuilder<Long> manyToOne(String fieldName, String referencedEntityKey) {
        String datalistId = fieldName + "_" + referencedEntityKey;
        datalist(datalistId, d -> d.dictionary(referencedEntityKey));
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, datalistId, referencedEntityKey,true, many_to_one, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Long>)this;
    }

    /**
     * Creates a nullable foreign key column in the database and organization selector on the form.
     * <p>
     * Generates a BIGINT database column and renders as a dropdown populated with all
     * available organizations. Establishes a reference to the Organization entity.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration
     * @see Organization
     */
    @Autocomplete(doc = "Create nullable numeric reference column in the database and select input populated with organization IDs on presentation layer.")
    public FormFieldDefinitionBuilder<Long> organizationSelect(String fieldName) {
        datalist("organizations", d -> d.dictionary(Organization.class));
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, "organizations", true, organization_select, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Long>)this;
    }
    /**
     * Creates a nullable foreign key column in the database and module selector on the form.
     * <p>
     * Generates a BIGINT database column and renders as a dropdown populated with all
     * available OpenKoda modules. Establishes a reference to the OpenkodaModule entity.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration
     * @see OpenkodaModule
     */
    @Autocomplete(doc = "Create nullable numeric reference column in the database and select input populated with Openkoda Modules IDs on presentation layer.")
    public FormFieldDefinitionBuilder<Long> moduleSelect(String fieldName) {
        datalist("modules", d -> d.dictionary(OpenkodaModule.class));
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, "modules", true, module_select, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Long>)this;
    }
    /**
     * Creates a radio button list field on the form (presentation layer only).
     * <p>
     * Renders as a list of radio buttons populated from the specified datalist.
     * Only one option can be selected at a time.
     * </p>
     *
     * @param fieldName the form field identifier
     * @param datalistId identifier of the datalist containing radio button options
     * @return builder for further configuration (layout, default selection)
     * @see #datalist(String, BiFunction)
     */
    @Autocomplete(doc = "Create radio elements list (only on presentation layer)." +
            "This action may be preceded by appropriate data list creation. See also 'datalist'")
    public FormFieldDefinitionBuilder<Object> radioList(String fieldName, String datalistId) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, datalistId, radio_list, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    @Autocomplete(doc = "Create radio elements list without the preceding default label element (only on presentation layer)." +
            "Provide list values as a second argument (BiFunction datalistSupplier). ")
    public FormFieldDefinitionBuilder<Object> radioListNoLabel(String fieldName, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, null, radio_list_no_label, null, datalistSupplier, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    @Autocomplete(doc = "Create radio elements list without the preceding default label element (only on presentation layer)." +
            "This action may be preceded by appropriate data list creation. See also 'datalist'")
    public FormFieldDefinitionBuilder<Object> radioListNoLabel(String fieldName, String dataListId) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, dataListId, radio_list_no_label, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    @Autocomplete(doc = "Create custom field, providing its type with Function<Object,FieldType> as a second argument (only on presentation layer).")
    public FormFieldDefinitionBuilder<Object> customFieldType(String fieldName, Function<Object, FieldType> fieldTypeFunction) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, fieldTypeFunction, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    @Autocomplete(doc = "Create divider element only on presentation layer.")
    public FormFieldDefinitionBuilder<Object> divider(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, divider, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    /**
     * Creates a text column in the database and CSS code editor field on the form.
     * <p>
     * Generates a TEXT database column and renders as a code editor with CSS syntax
     * highlighting and validation.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration (editor theme, validation)
     */
    @Autocomplete(doc = "Create non null long string column in the database and a CSS code editor element on presentation layer.")
    public FormFieldDefinitionBuilder<String> codeCss(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, code_css, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    /**
     * Creates a text column in the database and HTML code editor field on the form.
     * <p>
     * Generates a TEXT database column and renders as a code editor with HTML syntax
     * highlighting and validation.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration (editor theme, validation)
     */
    @Autocomplete(doc = "Create non null long string column in the database and a HTML code editor element on presentation layer.")
    public FormFieldDefinitionBuilder<String> codeHtml(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, code_html, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    /**
     * Creates a text column in the database and JavaScript code editor field on the form.
     * <p>
     * Generates a TEXT database column and renders as a code editor with JavaScript syntax
     * highlighting and validation.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration (editor theme, validation)
     */
    @Autocomplete(doc = "Create non null long string column in the database and a JS code editor element on presentation layer.")
    public FormFieldDefinitionBuilder<String> codeJs(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, code_js, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    /**
     * Creates a text column in the database and code editor with WebEndpoint autocomplete on the form.
     * <p>
     * Generates a TEXT database column and renders as a code editor with intelligent
     * autocomplete suggestions for WebEndpoint API references.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration (editor theme, validation)
     */
    @Autocomplete(doc = "Create non null long string column in the database and a code editor element with WebEndpoint specific autocomplete functionality on presentation layer.")
    public FormFieldDefinitionBuilder<String> codeWithWebendpointAutocomplete(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, code_with_webendpoint_autocomplete, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    /**
     * Creates a text column in the database and code editor with form field autocomplete on the form.
     * <p>
     * Generates a TEXT database column and renders as a code editor with intelligent
     * autocomplete suggestions for form field references.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration (editor theme, validation)
     */
    @Autocomplete(doc = "Create non null long string column in the database and a code editor element with autocomplete functionality on presentation layer.")
    public FormFieldDefinitionBuilder<String> codeWithFormAutocomplete(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, code_with_form_autocomplete, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    /**
     * Creates a string column in the database and hidden input field on the form.
     * <p>
     * Generates a VARCHAR database column and renders as an HTML hidden input element
     * that stores values without displaying them to the user.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration
     */
    @Autocomplete(doc = "Create non null string column in the database and an input of type hidden on presentation layer.")
    public FormFieldDefinitionBuilder<String> hidden(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, hidden, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    @Autocomplete(doc = "Create non null boolean column in the database and switch element on presentation layer.")
    public FormFieldDefinitionBuilder<Object> switchValues(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, switch_values, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    @Autocomplete(doc = "Create switch element which switches visibility of section elements marked by this fieldName as a css class (only on presentation layer)." +
            "Switching on will trigger the warning in a form of a JS alert.")
    public FormFieldDefinitionBuilder<Object> switchValuesWithWarning(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, switch_values_with_warning, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    @Autocomplete(doc = "Create checkbox element which switches visibility of section elements marked by this fieldName as a css class (only on presentation layer)." +
            "Selecting the checkbox will trigger the warning in a form of a JS alert.")
    public FormFieldDefinitionBuilder<Object> sectionWithCheckboxWithWarning(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, section_with_checkbox_with_warning, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    @Autocomplete(doc = "Create link element which switches visibility of section elements marked by this fieldName as a css class (only on presentation layer).")
    public FormFieldDefinitionBuilder<Object> sectionWithLink(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, section_with_link, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    @Autocomplete(doc = "Create checkbox element which switches visibility of section elements marked by this fieldName as a css class (only on presentation layer).")
    public FormFieldDefinitionBuilder<Object> sectionWithCheckbox(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, section_with_checkbox, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    @Autocomplete(doc = "Create switch element which switches visibility of section elements marked by this fieldName as a css class (only on presentation layer).")
    public FormFieldDefinitionBuilder<Object> sectionWithSwitch(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, section_with_switch, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    @Autocomplete(doc = "Create switch element which shows/hides other elements marked by additional css class (only on presentation layer).")
    public FormFieldDefinitionBuilder<Object> sectionWithSwitchContent(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, section_with_switch_content, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    /**
     * Creates a string column in the database and password input field on the form.
     * <p>
     * Generates a VARCHAR database column and renders as an HTML password input element
     * with masked character display for secure entry.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration (min length, strength validation)
     */
    @Autocomplete(doc = "Create non null string column in the database and password input element on presentation layer.")
    public FormFieldDefinitionBuilder<String> password(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, password, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    @Autocomplete(doc = "Create submit to new tab button only on presentation layer.")
    public FormFieldDefinitionBuilder<Object> submitToNewTab(String fieldName, String url) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, submit_to_new_tab, defaultReadPrivilege, defaultWritePrivilege, url));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    @Autocomplete(doc = "Create non null string column in the database and map element on presentation layer.")
    public FormFieldDefinitionBuilder<Object> map(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, map, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>) this;
    }

    @Autocomplete(doc = "Create images library element only on presentation layer.")
    public FormFieldDefinitionBuilder<Object> imagesLibrary(String fieldName, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, fieldName, files_library, datalistSupplier, defaultReadPrivilege, defaultWritePrivilege, "image/png,image/jpeg", filesConverter));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    @Autocomplete(doc = "Create image library element only on presentation layer.")
    public FormFieldDefinitionBuilder<Object> imageLibrary(String fieldName, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, fieldName, file_library, datalistSupplier, defaultReadPrivilege, defaultWritePrivilege, "image/png,image/jpeg", filesConverter));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    /**
     * Creates a string column in the database and file upload gallery field on the form.
     * <p>
     * Generates a VARCHAR database column storing comma-separated file IDs and renders
     * as a file gallery with upload functionality filtered by MIME type.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @param datalistSupplier function providing existing files for display
     * @param mimeType comma-separated list of allowed MIME types (e.g., "image/png,image/jpeg")
     * @return builder for further configuration (max files, file size limits)
     */
    @Autocomplete(doc = "Create non null string column in the database to store comma-separated list of file IDs and a file gallery with upload section on presentation layer.")
    public FormFieldDefinitionBuilder<Object> files(String fieldName, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier, String mimeType) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, fieldName, files, datalistSupplier, defaultReadPrivilege, defaultWritePrivilege, mimeType, filesConverter));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    /**
     * Creates an image selector field on the form (presentation layer only).
     * <p>
     * Renders as a single image upload/selection widget accepting PNG and JPEG formats.
     * The selected image ID is stored but no database column is created automatically.
     * </p>
     *
     * @param fieldName the form field identifier
     * @return builder for further configuration (image dimensions, cropping)
     */
    @Autocomplete(doc = "Create single image selector element (only on presentation layer).")
    public FormFieldDefinitionBuilder<Object> image(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, image, "", defaultReadPrivilege, defaultWritePrivilege, "image/png,image/jpeg", filesConverter));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    @Autocomplete(doc = "Create non null string column in the database and simple text input element on presentation layer.")
    public FormFieldDefinitionBuilder<String> imageUrl(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, image_url, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    /**
     * Creates a string column in the database and color picker field on the form.
     * <p>
     * Generates a VARCHAR database column storing color hex values and renders as a
     * color picker widget for visual color selection.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration (default color, color format)
     */
    @Autocomplete(doc = "Create non null string column in the database and color picker element on presentation layer.")
    public FormFieldDefinitionBuilder<String> colorPicker(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, color_picker, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<String>)this;
    }
    /**
     * Creates a timestamp with timezone column in the database and time picker field on the form.
     * <p>
     * Generates a TIMESTAMP WITH TIME ZONE database column and renders as a time picker
     * widget for selecting time values with timezone awareness.
     * </p>
     *
     * @param fieldName the database column name and form field identifier
     * @return builder for further configuration (time format, timezone, validation)
     */
    @Autocomplete(doc = "Create non null timestamp with timezone column in the database and time picker element on presentation layer.")
    public FormFieldDefinitionBuilder<Object> timePicker(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, time, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    @Autocomplete(doc = "Create 'then' rule configuration element (presentation layer only).")
    public FormFieldDefinitionBuilder<Object> ruleThen(String fieldName, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier, String url) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName,null, url, rule_then, datalistSupplier, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    /**
     * Creates a ReCaptcha challenge field on the form (presentation layer only).
     * <p>
     * Renders as a Google ReCaptcha widget for bot detection and spam prevention.
     * No database column is created; validation occurs server-side during form submission.
     * </p>
     *
     * @return builder for further configuration (ReCaptcha version, theme)
     */
    @Autocomplete(doc = "Create recaptcha element only on presentation layer.")
    public FormFieldDefinitionBuilder<Object> recaptcha() {
        fields.add(lastField = createFormFieldDefinition(formName, RECAPTCHA, recaptcha, defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    /**
     * Creates a div container element on the form (presentation layer only).
     * <p>
     * Renders as an HTML div element for layout structuring and visual organization.
     * No database column is created; used purely for presentation purposes.
     * </p>
     *
     * @param fieldName the element identifier for CSS styling and JavaScript access
     * @return builder for further configuration (CSS classes, inline styles)
     */
    @Autocomplete(doc = "Create div only on presentation layer.")
    public FormFieldDefinitionBuilder<Object> div(String fieldName) {
        fields.add(lastField = createFormFieldDefinition(formName, fieldName, div,  defaultReadPrivilege, defaultWritePrivilege));
        return (FormFieldDefinitionBuilder<Object>)this;
    }
    /**
     * Utility function for converting file ID inputs to a list of Long values.
     * <p>
     * Handles multiple input formats from file upload fields:
     * <ul>
     * <li>null → empty list</li>
     * <li>Single string ID → single-element list</li>
     * <li>String array of IDs → list of parsed Long values</li>
     * </ul>
     * Used internally by file and image field types to normalize uploaded file references.
     * </p>
     */
    //TODO: move to some better place
    Function filesConverter = new Function() {
        @Override
        public Object apply(Object o) {
            List<Long> result = new ArrayList<>();
            if (o == null) {return result;}
            if (o instanceof String) {
                result.add(Long.valueOf((String) o));
                return result;
            }
            if (o.getClass().isArray()) {
                String[] oa = (String[])o;
                for (String s : oa) {
                    result.add(Long.valueOf(s));
                }
                return result;
            }
            return result;
        }
    };
}