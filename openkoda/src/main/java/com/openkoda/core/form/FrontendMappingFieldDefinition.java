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

import com.openkoda.core.security.OrganizationUser;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.common.LongIdEntity;
import com.openkoda.repository.SecureEntityDictionaryRepository;
import org.apache.commons.lang3.StringUtils;

import java.util.function.BiFunction;
import java.util.function.Function;

import static com.openkoda.core.form.FieldType.*;
import static com.openkoda.core.form.FormFieldDefinitionBuilderStart.DATALIST_PREFIX;

/**
 * Immutable form field configuration storing field name, type, validation constraints, privilege requirements, and display hints used by form templates for rendering.
 * <p>
 * This class defines a single form field's complete specification including its data type (via {@link FieldType}),
 * security requirements (read/write privileges and dynamic checks), data source (datalist suppliers or value suppliers),
 * internationalization keys (label, placeholder, tooltip, etc.), rendering properties (CSS, content type, URL),
 * and bidirectional value conversion between DTO and entity representations. All field definitions are immutable
 * after construction to ensure thread-safety and consistent rendering behavior.
 * <p>
 * Field definitions support multiple binding modes: DTO-based (dto.fieldName), map-based (dto[fieldName]), and non-DTO (fieldName).
 * The class generates standard i18n keys following the pattern formName.fieldName.suffix (label, placeholder, tooltip, warning, alert, description).
 * Datalist identifiers are automatically prefixed to maintain consistency across the application.
 * <p>
 * Example usage:
 * <pre>{@code
 * FrontendMappingFieldDefinition nameField = 
 *     FrontendMappingFieldDefinition.createFormFieldDefinition("userForm", "username", FieldType.text);
 * }</pre>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see FieldType
 * @see FormFieldDefinitionBuilderStart
 * @see FrontendMappingDefinition
 */
public class FrontendMappingFieldDefinition {
    /** Field name used for form binding and i18n key generation (e.g., "username", "email"). */
    private final String name;
    
    /** Field type defining rendering behavior (text, password, select, etc.). Null if dynamic via fieldTypeFunction. */
    public final FieldType type;
    
    /** Required privilege for reading this field value. Null if no privilege check required. */
    public final PrivilegeBase readPrivilege;
    
    /** Required privilege for writing this field value. Null if no privilege check required. */
    public final PrivilegeBase writePrivilege;
    
    /** Dynamic read access check evaluated at runtime against current user and entity. Null if privilege-based only. */
    public final BiFunction<OrganizationUser, LongIdEntity, Boolean> canReadCheck;
    
    /** Dynamic write access check evaluated at runtime against current user and entity. Null if privilege-based only. */
    public final BiFunction<OrganizationUser, LongIdEntity, Boolean> canWriteCheck;
    
    /** Supplier providing datalist options (select/autocomplete values) from form and repository context. Null if static datalist. */
    public final BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier;
    
    /** Flag indicating datalist supplier requires form context (true) vs repository-only context (false). */
    public final boolean formBasedDatalistSupplier;
    
    /** Supplier computing field value from form state rather than entity property. Null if entity-bound field. */
    public final Function<AbstractForm, Object> valueSupplier;
    
    /** Function computing field type dynamically from form state. Null if static type provided. */
    public final Function<Object, FieldType> fieldTypeFunction;
    
    /** Null-returning function placeholder for unset fieldTypeFunction. */
    private final static Function<Object, FieldType> nullFunction = a -> null;
    
    /** Null-returning supplier placeholder for unset valueSupplier. */
    private final static Function<Object, Object> nullSupplier = null;
    
    /** Primary i18n key for field (formName.fieldName pattern, e.g., "userForm.username"). */
    public final String key;
    
    /** I18n key for field label (formName.fieldName.label pattern). */
    public final String labelKey;
    
    /** I18n key for field placeholder text (formName.fieldName.placeholder pattern). */
    public final String placeholderKey;
    
    /** I18n key for field tooltip help text (formName.fieldName.tooltip pattern). */
    public final String tooltipKey;
    
    /** I18n key for field validation warning message (formName.fieldName.warning pattern). */
    public final String warningKey;
    
    /** I18n key for field alert/error message (formName.fieldName.alert pattern). */
    public final String alertKey;
    
    /** I18n key for field description text (formName.fieldName.description pattern). */
    public final String descriptionKey;
    
    /** Datalist HTML identifier for option population (auto-prefixed with "datalist-" if not already prefixed). */
    public final String datalistId;
    
    /** Default selected value for datalist fields. Null if no preselection. */
    public String preselectedValue;

    /** Additional CSS classes applied to field rendering (space-separated). Null if no custom styling. */
    public final String additionalCss;
    
    /** Flag indicating field accepts null values (true) vs requires non-null value (false). */
    public final boolean allowNull;
    
    /** URL for field action or resource (used for custom field types like html_fragment). Null if not applicable. */
    public final String url;
    
    /** Name of HTML fragment template to render for custom field types. Null if standard rendering. */
    public final String htmlFragmentName;
    
    /** Content type for field value (e.g., "application/json", "text/plain"). Null if default content type. */
    public final String contentType;
    
    /** Converter transforming DTO value to entity value during populateTo phase. Null if direct assignment. */
    public final Function dtoToEntityValueConverter;
    
    /** Converter transforming entity value to DTO value during populateFrom phase. Null if direct assignment. */
    public final Function entityToDtoValueConverter;

    /** Required privilege for field-level action button. Null if no action or no privilege required. */
    public final PrivilegeBase actionPrivilege;
    
    /** URL target for field-level action button. Null if no action configured. */
    public final String actionUrl;
    
    /** I18n key for field-level action button label. Null if no action configured. */
    public final String actionLabelKey;
    
    /** Flag indicating field binds directly to model (true) vs via DTO wrapper (false). */
    public final boolean nonDto;
    
    /** SQL formula for computed field values (used in dynamic entity generation). Null if persisted field. */
    public final String sqlFormula;
    
    /** Entity key for foreign key relationship (used in dynamic entity generation). Null if not a reference field. */
    public final String referencedEntityKey;
    
    /** Flag enabling strict write access control (field hidden/disabled if write access denied despite global settings). */
    private boolean strictWriteAccess;
    
    /** Flag enabling strict read access control (field hidden if read access denied despite global settings). */
    private boolean strictReadAccess;
    
    /** Flag enabling full-text search indexing for this field in dynamic entities. */
    public final boolean searchEnabled;

    /**
     * Checks if field renders as plain text input.
     *
     * @param form form instance for dynamic type resolution via fieldTypeFunction
     * @return true if field type is {@link FieldType#text}, false otherwise
     */
    public boolean isText(Form form) {
        return getFieldType(form) == text;
    }

    /**
     * Checks if field renders as password input (masked characters).
     *
     * @param form form instance for dynamic type resolution via fieldTypeFunction
     * @return true if field type is {@link FieldType#password}, false otherwise
     */
    public boolean isPassword(Form form) {
        return getFieldType(form) == password;
    }

    /**
     * Checks if field renders as hidden input (not visible in UI but included in form submission).
     *
     * @param form form instance for dynamic type resolution via fieldTypeFunction
     * @return true if field type is {@link FieldType#hidden}, false otherwise
     */
    public boolean isHidden(Form form) {
        return getFieldType(form) == hidden;
    }

    /**
     * Sets strict read access mode for this field.
     * When enabled, field is hidden if user lacks read access regardless of global form settings.
     *
     * @param strictReadAccess true to enable strict read access control, false for lenient mode
     */
    public void setStrictReadAccess(boolean strictReadAccess) {
        this.strictReadAccess = strictReadAccess;
    }

    /**
     * Checks if field renders as map input (key-value pair editor).
     *
     * @param form form instance for dynamic type resolution via fieldTypeFunction
     * @return true if field type is {@link FieldType#map}, false otherwise
     */
    public boolean isMap(Form form) { return getFieldType(form) == map; }

    /**
     * Checks if field renders as file upload control (single file, multiple files, or image upload).
     *
     * @param form form instance for dynamic type resolution via fieldTypeFunction
     * @return true if field type is file_library, files_library, files, or image, false otherwise
     */
    public boolean isFileUpload(Form form) {
        FieldType t = getFieldType(form);
        return t == file_library || t == files_library || t == files || t == image;}

    /**
     * Checks if field renders as color picker input (hex/rgb color selector).
     *
     * @param form form instance for dynamic type resolution via fieldTypeFunction
     * @return true if field type is {@link FieldType#color_picker}, false otherwise
     */
    public boolean isColorPicker(Form form) { return getFieldType(form) == color_picker; }

    /**
     * Checks if field renders as time picker input (hour/minute/second selector).
     *
     * @param form form instance for dynamic type resolution via fieldTypeFunction
     * @return true if field type is {@link FieldType#time}, false otherwise
     */
    public boolean isTimePicker(Form form) { return getFieldType(form) == time; }

    /**
     * Checks if field renders as rich document editor (WYSIWYG HTML editor).
     *
     * @param form form instance for dynamic type resolution via fieldTypeFunction
     * @return true if field type is {@link FieldType#document}, false otherwise
     */
    public boolean isDocumentEditor(Form form) { return getFieldType(form) == document; }

    /**
     * Checks if field renders as code editor with syntax highlighting (JavaScript, HTML, CSS, or with autocomplete).
     *
     * @param form form instance for dynamic type resolution via fieldTypeFunction
     * @return true if field type is code_js, code_html, code_css, code_with_webendpoint_autocomplete, or code_with_form_autocomplete, false otherwise
     */
    public boolean isCodeEditor(Form form) {
        FieldType t = getFieldType(form);
        return t == code_js || t == code_html || t == code_css || t == code_with_webendpoint_autocomplete || t== code_with_form_autocomplete;
    }
    
    /**
     * Checks if field renders as code editor with web endpoint autocomplete suggestions.
     *
     * @param form form instance for dynamic type resolution via fieldTypeFunction
     * @return true if field type is {@link FieldType#code_with_webendpoint_autocomplete}, false otherwise
     */
    public boolean isCodeEditorWithWebendpointAutocomplete(Form form) {
        FieldType t = getFieldType(form);
        return t == code_with_webendpoint_autocomplete;
    }
    
    /**
     * Checks if field renders as code editor with form field autocomplete suggestions.
     *
     * @param form form instance for dynamic type resolution via fieldTypeFunction
     * @return true if field type is {@link FieldType#code_with_form_autocomplete}, false otherwise
     */
    public boolean isCodeEditorWithFormAutocomplete(Form form) {
        FieldType t = getFieldType(form);
        return t == code_with_form_autocomplete;
    }
    
    /**
     * Checks if field renders as reCAPTCHA challenge widget.
     *
     * @param form form instance for dynamic type resolution via fieldTypeFunction
     * @return true if field type is {@link FieldType#recaptcha}, false otherwise
     */
    public boolean isReCaptcha(Form form) { return getFieldType(form) == recaptcha; }

    /**
     * Resolves effective field type from static type or dynamic fieldTypeFunction.
     * Returns static type if configured, otherwise evaluates fieldTypeFunction with provided form.
     *
     * @param form form instance passed to fieldTypeFunction for dynamic type resolution
     * @return resolved FieldType for this field in the context of the given form
     */
    public FieldType getFieldType(Form form) {
        return type != null ? type : fieldTypeFunction.apply(form);
    }

    /**
     * Returns static field type without dynamic resolution.
     *
     * @return static FieldType configured at construction, or null if dynamic fieldTypeFunction used
     */
    public FieldType getType() {
        return type;
    }

    /**
     * Creates field definition with dynamic type resolved via function.
     * Use for fields whose type depends on form state or entity data.
     *
     * @param formName form name for i18n key generation (e.g., "userForm")
     * @param name field name for binding and key generation (e.g., "status")
     * @param fieldTypeFunction function computing FieldType from form or entity context
     * @return new field definition with dynamic type resolution
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, Function<Object, FieldType> fieldTypeFunction) {
        return new FrontendMappingFieldDefinition(formName, name, null, null, null, null, null, null, null, false,
                fieldTypeFunction, null,null, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with dynamic type and privilege-based access control.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param fieldTypeFunction function computing FieldType from form or entity context
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param requiredWritePrivilege privilege required to modify field value, null if no restriction
     * @return new field definition with dynamic type and privilege checks
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, Function<Object, FieldType> fieldTypeFunction, PrivilegeBase requiredReadPrivilege, PrivilegeBase requiredWritePrivilege) {
        return new FrontendMappingFieldDefinition(formName, name, null, null, null, requiredReadPrivilege, requiredWritePrivilege, null, null, false, fieldTypeFunction, null, null, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with computed value from form state rather than entity property.
     * Use for calculated fields, aggregations, or virtual fields not backed by entity persistence.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param valueSupplier function computing field value from AbstractForm instance
     * @param type static field type for rendering
     * @return new field definition with value supplier
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, Function<AbstractForm, Object> valueSupplier, FieldType type) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, null, null, valueSupplier, null, false, null, null, null, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates basic field definition with static type and no access restrictions.
     * Use for simple text inputs, numbers, dates without special configuration.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param type static field type for rendering
     * @return new field definition with minimal configuration
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, FieldType type) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, null, null, null, null, false, null, null, null, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with static datalist for select/autocomplete options.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param type static field type for rendering (typically select or autocomplete)
     * @return new field definition with static datalist reference
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, String datalistId, FieldType type) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, null, null, null, null, false, null, datalistId, null, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with static datalist and privilege-based access control.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param type static field type for rendering
     * @param readPrivilege privilege required to read field value, null if no restriction
     * @param writePrivilege privilege required to modify field value, null if no restriction
     * @return new field definition with datalist and privilege checks
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, String datalistId, FieldType type, PrivilegeBase readPrivilege, PrivilegeBase writePrivilege) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, readPrivilege, writePrivilege, null, null, false, null, datalistId, null,  false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with custom CSS styling, datalist, and privilege checks.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param type static field type for rendering
     * @param additionalCss space-separated CSS classes applied to field
     * @param readPrivilege privilege required to read field value, null if no restriction
     * @param writePrivilege privilege required to modify field value, null if no restriction
     * @return new field definition with styling and access control
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, String datalistId, FieldType type, String additionalCss, PrivilegeBase readPrivilege, PrivilegeBase writePrivilege) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, readPrivilege, writePrivilege, null, null, false, null, datalistId, additionalCss, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates non-DTO field definition binding directly to model instead of DTO wrapper.
     * Use for fields that bypass standard DTO transformation and access model properties directly.
     *
     * @param formName form name for i18n key generation
     * @param name field name for direct model binding (not wrapped in dto.fieldName)
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param type static field type for rendering
     * @param readPrivilege privilege required to read field value, null if no restriction
     * @param writePrivilege privilege required to modify field value, null if no restriction
     * @return new field definition with direct model binding
     */
    public static FrontendMappingFieldDefinition createNonDtoFormFieldDefinition(String formName, String name, String datalistId, FieldType type, PrivilegeBase readPrivilege, PrivilegeBase writePrivilege) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, readPrivilege, writePrivilege, null, null, false, null, datalistId, null, false, null, null, null, null, null, null, null, null, null, null,true, false);
    }

    /**
     * Creates field definition with dynamic access checks evaluated at runtime.
     * Use when access control depends on entity state or user attributes beyond simple privilege checks.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param type static field type for rendering
     * @param canReadCheck function evaluating read access from user and entity, null if no check
     * @param canWriteCheck function evaluating write access from user and entity, null if no check
     * @return new field definition with dynamic access control
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, String datalistId, FieldType type, BiFunction<OrganizationUser, LongIdEntity, Boolean> canReadCheck, BiFunction<OrganizationUser, LongIdEntity, Boolean> canWriteCheck) {
        return new FrontendMappingFieldDefinition(formName, name, type, canReadCheck, canWriteCheck, null, null, null, null, false, null, datalistId, null, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }
    
    /**
     * Creates field definition with dynamic access checks and null value support.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param allowNull true if field accepts null values, false if required
     * @param type static field type for rendering
     * @param canReadCheck function evaluating read access from user and entity, null if no check
     * @param canWriteCheck function evaluating write access from user and entity, null if no check
     * @return new field definition with nullable support and dynamic access control
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, String datalistId, boolean allowNull, FieldType type, BiFunction<OrganizationUser, LongIdEntity, Boolean> canReadCheck, BiFunction<OrganizationUser, LongIdEntity, Boolean> canWriteCheck) {
        return new FrontendMappingFieldDefinition(formName, name, type, canReadCheck, canWriteCheck, null, null, null, null, false, null, datalistId, null, allowNull, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with nullable support and privilege-based access control.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param allowNull true if field accepts null values, false if required
     * @param type static field type for rendering
     * @param readPrivilege privilege required to read field value, null if no restriction
     * @param writePrivilege privilege required to modify field value, null if no restriction
     * @return new field definition with nullable support and privilege checks
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, String datalistId, boolean allowNull, FieldType type, PrivilegeBase readPrivilege, PrivilegeBase writePrivilege) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, readPrivilege, writePrivilege, null, null, false, null, datalistId, null, allowNull, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition for foreign key reference with nullable support.
     * Use for fields representing relationships to other entities in dynamic entity generation.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param referencedEntityKey entity key for foreign key target (used in DDL generation)
     * @param allowNull true if field accepts null values (optional reference), false if required
     * @param type static field type for rendering
     * @param readPrivilege privilege required to read field value, null if no restriction
     * @param writePrivilege privilege required to modify field value, null if no restriction
     * @return new field definition for foreign key with reference metadata
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, String datalistId, String referencedEntityKey, boolean allowNull, FieldType type, PrivilegeBase readPrivilege, PrivilegeBase writePrivilege) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, readPrivilege, writePrivilege, null, null, false, null, datalistId, null, allowNull, null, null, null, null, null, null, null, null, null, referencedEntityKey,false, false);
    }

    /**
     * Creates field definition with dynamic datalist options computed from form and repository context.
     * Use for select/autocomplete fields whose options depend on current entity state or available data.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param type static field type for rendering (typically select or autocomplete)
     * @param datalistSupplier function computing datalist options from form and repository
     * @return new field definition with dynamic datalist
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, String datalistId, FieldType type, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, null, null, null, datalistSupplier, true, null, datalistId, null, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with dynamic datalist and privilege-based access control.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param type static field type for rendering
     * @param datalistSupplier function computing datalist options from form and repository
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param requiredWritePrivilege privilege required to modify field value, null if no restriction
     * @return new field definition with dynamic datalist and privilege checks
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, String datalistId,
                                                                           FieldType type, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier, PrivilegeBase requiredReadPrivilege, PrivilegeBase requiredWritePrivilege) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, requiredReadPrivilege,
                requiredWritePrivilege, null, datalistSupplier, true, null, datalistId, null,  false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with repository-only datalist supplier (no form context required).
     * Use for datalist options computed from database queries without current form/entity state.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param type static field type for rendering
     * @param datalistSupplier function computing datalist options from repository only
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param requiredWritePrivilege privilege required to modify field value, null if no restriction
     * @return new field definition with repository-based datalist
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, String datalistId,
                                                                           FieldType type, Function<SecureEntityDictionaryRepository, Object> datalistSupplier, PrivilegeBase requiredReadPrivilege, PrivilegeBase requiredWritePrivilege) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, requiredReadPrivilege,
                requiredWritePrivilege, null, (f, d) -> datalistSupplier.apply(d), false, null, datalistId, null,  false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with dynamic datalist, privilege checks, content type, and value converter.
     * Use for complex fields requiring type-specific serialization and DTO-entity transformation.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param type static field type for rendering
     * @param datalistSupplier function computing datalist options from form and repository
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param requiredWritePrivilege privilege required to modify field value, null if no restriction
     * @param contentType MIME type for field value (e.g., "application/json")
     * @param dtoToEntityValueConverter function transforming DTO value to entity value
     * @return new field definition with datalist, privileges, and conversion
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, String datalistId,
                                                                           FieldType type, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier, PrivilegeBase requiredReadPrivilege, PrivilegeBase requiredWritePrivilege, String contentType, Function dtoToEntityValueConverter) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, requiredReadPrivilege,
                requiredWritePrivilege, null, datalistSupplier, true, null, datalistId, null, false, null, null, contentType, dtoToEntityValueConverter, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with dynamic datalist, privilege checks, and URL for custom actions.
     * Use for fields with associated resource URLs (e.g., API endpoints for autocomplete).
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param url URL for field action or resource loading
     * @param type static field type for rendering
     * @param datalistSupplier function computing datalist options from form and repository
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param requiredWritePrivilege privilege required to modify field value, null if no restriction
     * @return new field definition with datalist, privileges, and URL
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, String datalistId, String url,
                                                                           FieldType type, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier, PrivilegeBase requiredReadPrivilege, PrivilegeBase requiredWritePrivilege) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, requiredReadPrivilege,
                requiredWritePrivilege, null, datalistSupplier, true, null, datalistId, null, false, url, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with dynamic datalist, privilege checks, and custom CSS styling.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param type static field type for rendering
     * @param additionalCss space-separated CSS classes applied to field
     * @param datalistSupplier function computing datalist options from form and repository
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param requiredWritePrivilege privilege required to modify field value, null if no restriction
     * @return new field definition with datalist, privileges, and styling
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, String datalistId, FieldType type, String additionalCss,
                                                                           BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier, PrivilegeBase requiredReadPrivilege, PrivilegeBase requiredWritePrivilege) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, requiredReadPrivilege,
                requiredWritePrivilege, null, datalistSupplier, true, null, datalistId, additionalCss, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with full configuration: dynamic datalist, nullable support, styling, privilege checks, and value converter.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param datalistId identifier for datalist (auto-prefixed with "datalist-")
     * @param allowNull true if field accepts null values, false if required
     * @param type static field type for rendering
     * @param additionalCss space-separated CSS classes applied to field
     * @param datalistSupplier function computing datalist options from form and repository
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param requiredWritePrivilege privilege required to modify field value, null if no restriction
     * @param dtoToEntityValueConverter function transforming DTO value to entity value
     * @return new field definition with complete configuration
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, String datalistId, Boolean allowNull, FieldType type, String additionalCss,
                                                                           BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier, PrivilegeBase requiredReadPrivilege, PrivilegeBase requiredWritePrivilege, Function dtoToEntityValueConverter) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, requiredReadPrivilege,
                requiredWritePrivilege, null, datalistSupplier, true, null, datalistId, additionalCss, allowNull, null, null, null, dtoToEntityValueConverter, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with privilege checks and computed value from form state.
     * Use for calculated or virtual fields requiring access control.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param type static field type for rendering
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param requiredWritePrivilege privilege required to modify field value, null if no restriction
     * @param valueSupplier function computing field value from AbstractForm instance
     * @return new field definition with privilege checks and value computation
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, FieldType type,
                                                                           PrivilegeBase requiredReadPrivilege, PrivilegeBase requiredWritePrivilege, Function<AbstractForm, Object> valueSupplier) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, requiredReadPrivilege,
                requiredWritePrivilege, valueSupplier, null, false, null, null, null, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with privilege checks, value supplier, and DTO mode specification.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param type static field type for rendering
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param requiredWritePrivilege privilege required to modify field value, null if no restriction
     * @param valueSupplier function computing field value from AbstractForm instance
     * @param hasDto flag indicating field uses DTO wrapper (parameter present for signature compatibility but not used)
     * @return new field definition with privilege checks and value computation
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, FieldType type,
                                                                           PrivilegeBase requiredReadPrivilege, PrivilegeBase requiredWritePrivilege, Function<AbstractForm, Object> valueSupplier, boolean hasDto) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, requiredReadPrivilege,
                requiredWritePrivilege, valueSupplier, null, false, null, null, null, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with privilege-based access control only.
     * Use for standard entity-bound fields requiring role-based security.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param type static field type for rendering
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param requiredWritePrivilege privilege required to modify field value, null if no restriction
     * @return new field definition with privilege checks
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, FieldType type, PrivilegeBase requiredReadPrivilege, PrivilegeBase requiredWritePrivilege) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, requiredReadPrivilege, requiredWritePrivilege, null, null, false, null, null, null, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with privilege checks and custom CSS styling.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param type static field type for rendering
     * @param additionalCss space-separated CSS classes applied to field
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param requiredWritePrivilege privilege required to modify field value, null if no restriction
     * @return new field definition with privilege checks and styling
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, FieldType type, String additionalCss, PrivilegeBase requiredReadPrivilege, PrivilegeBase requiredWritePrivilege) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, requiredReadPrivilege, requiredWritePrivilege, null, null, false, null, null, additionalCss, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with privilege checks, styling, content type, and value converter.
     * Use for fields with specialized content types requiring transformation (e.g., JSON fields).
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param type static field type for rendering
     * @param additionalCss space-separated CSS classes applied to field
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param requiredWritePrivilege privilege required to modify field value, null if no restriction
     * @param contentType MIME type for field value (e.g., "application/json")
     * @param dtoToEntityValueConverter function transforming DTO value to entity value
     * @return new field definition with privileges, styling, and conversion
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, FieldType type, String additionalCss, PrivilegeBase requiredReadPrivilege, PrivilegeBase requiredWritePrivilege, String contentType, Function dtoToEntityValueConverter) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, requiredReadPrivilege, requiredWritePrivilege, null, null, false, null, null, additionalCss, false, null, null, contentType, dtoToEntityValueConverter, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with read privilege and dynamic write access check.
     * Use for fields with static read security but entity-specific write restrictions.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param type static field type for rendering
     * @param additionalCss space-separated CSS classes applied to field
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param canWriteCheck function evaluating write access from user and entity
     * @return new field definition with mixed access control and styling
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, FieldType type, String additionalCss, PrivilegeBase requiredReadPrivilege, BiFunction<OrganizationUser, LongIdEntity, Boolean> canWriteCheck) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, canWriteCheck, requiredReadPrivilege, null, null, null, false, null, null, additionalCss, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with dynamic access checks only (no privilege requirements).
     * Use for fields with entity-specific security logic independent of role-based privileges.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param type static field type for rendering
     * @param canReadCheck function evaluating read access from user and entity, null if no check
     * @param canWriteCheck function evaluating write access from user and entity, null if no check
     * @return new field definition with dynamic access control
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, FieldType type, BiFunction<OrganizationUser, LongIdEntity, Boolean> canReadCheck, BiFunction<OrganizationUser, LongIdEntity, Boolean> canWriteCheck) {
        return new FrontendMappingFieldDefinition(formName, name, type, canReadCheck, canWriteCheck, null, null, null, null, false, null, null, null, false, null, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with privilege checks and associated URL.
     * Use for fields with external resources or API endpoints (e.g., autocomplete, file upload targets).
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param type static field type for rendering
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param requiredWritePrivilege privilege required to modify field value, null if no restriction
     * @param url URL for field action or resource loading
     * @return new field definition with privilege checks and URL
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, FieldType type, PrivilegeBase requiredReadPrivilege, PrivilegeBase requiredWritePrivilege, String url) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, requiredReadPrivilege, requiredWritePrivilege, null, null, false, null, null, null, false, url, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition for custom HTML fragment rendering with privilege checks and value computation.
     * Use for fields requiring custom rendering templates loaded from htmlFragmentName.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param type static field type for rendering (typically html_fragment)
     * @param requiredReadPrivilege privilege required to read field value, null if no restriction
     * @param requiredWritePrivilege privilege required to modify field value, null if no restriction
     * @param url URL for custom fragment loading or submission
     * @param valueSupplier function computing field value from AbstractForm instance
     * @param htmlFragmentName name of HTML fragment template for custom rendering
     * @return new field definition with custom rendering and privilege checks
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, FieldType type, PrivilegeBase requiredReadPrivilege, PrivilegeBase requiredWritePrivilege, String url, Function<AbstractForm, Object> valueSupplier, String htmlFragmentName) {
        return new FrontendMappingFieldDefinition(formName, name, type, null, null, requiredReadPrivilege, requiredWritePrivilege, valueSupplier, null, false, null, null, null, false, url, htmlFragmentName, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates field definition with dynamic access checks and URL.
     *
     * @param formName form name for i18n key generation
     * @param name field name for binding and key generation
     * @param type static field type for rendering
     * @param canReadCheck function evaluating read access from user and entity, null if no check
     * @param canWriteCheck function evaluating write access from user and entity, null if no check
     * @param url URL for field action or resource loading
     * @return new field definition with dynamic access control and URL
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, String name, FieldType type, BiFunction<OrganizationUser, LongIdEntity, Boolean> canReadCheck, BiFunction<OrganizationUser, LongIdEntity, Boolean> canWriteCheck, String url) {
        return new FrontendMappingFieldDefinition(formName, name, type, canReadCheck, canWriteCheck, null, null, null, null, false, null, null, null, false, url, null, null, null, null, null, null, null, null, null,false, false);
    }

    /**
     * Creates new field definition by copying existing definition and overriding CSS styling.
     * Use to reuse field configuration across forms with different visual presentation.
     *
     * @param formName form name for i18n key generation
     * @param f existing field definition to copy
     * @param additionalCss space-separated CSS classes replacing original styling
     * @return new field definition with updated styling
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, FrontendMappingFieldDefinition f, String additionalCss) {
        return new FrontendMappingFieldDefinition(formName, f.name, f.type, f.canReadCheck, f.canWriteCheck, f.readPrivilege, f.writePrivilege, f.valueSupplier, f.datalistSupplier, true, f.fieldTypeFunction, f.datalistId, additionalCss, f.allowNull, f.url, f.htmlFragmentName, f.contentType, f.dtoToEntityValueConverter, f.entityToDtoValueConverter, f.actionPrivilege, f.actionUrl, f.actionLabelKey, f.sqlFormula, f.referencedEntityKey, f.nonDto, f.searchEnabled);
    }

    /**
     * Creates new field definition for computed database field by copying existing definition and adding SQL formula.
     * Use for dynamic entity generation with calculated columns.
     *
     * @param formName form name for i18n key generation
     * @param f existing field definition to copy
     * @param sqlFormula SQL expression for computed column (e.g., "CASE WHEN active THEN 'Y' ELSE 'N' END")
     * @return new field definition with SQL formula for dynamic entity DDL generation
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinitionWithSqlFormula(String formName, FrontendMappingFieldDefinition f, String sqlFormula) {
        return new FrontendMappingFieldDefinition(formName, f.name, f.type, f.canReadCheck, f.canWriteCheck, f.readPrivilege, f.writePrivilege, f.valueSupplier, f.datalistSupplier, true, f.fieldTypeFunction, f.datalistId, null, f.allowNull, f.url, f.htmlFragmentName, f.contentType, f.dtoToEntityValueConverter, f.entityToDtoValueConverter, f.actionPrivilege, f.actionUrl, f.actionLabelKey, sqlFormula, f.referencedEntityKey, f.nonDto, f.searchEnabled);
    }

    /**
     * Creates new field definition by copying existing definition and enabling full-text search indexing.
     * Use for dynamic entity fields requiring search capability.
     *
     * @param formName form name for i18n key generation
     * @param f existing field definition to copy
     * @return new field definition with search indexing enabled
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinitionWithSearchEnabled(String formName, FrontendMappingFieldDefinition f) {
        return new FrontendMappingFieldDefinition(formName, f.name, f.type, f.canReadCheck, f.canWriteCheck, f.readPrivilege, f.writePrivilege, f.valueSupplier, f.datalistSupplier, true, f.fieldTypeFunction, f.datalistId, null, f.allowNull, f.url, f.htmlFragmentName, f.contentType, f.dtoToEntityValueConverter, f.entityToDtoValueConverter, f.actionPrivilege, f.actionUrl, f.actionLabelKey, f.sqlFormula, f.referencedEntityKey, f.nonDto, true);
    }

    /**
     * Creates new field definition by copying existing definition and replacing datalist supplier.
     * Use to reuse field configuration with different option sources.
     *
     * @param formName form name for i18n key generation
     * @param f existing field definition to copy
     * @param datalistSupplier function computing datalist options from form and repository
     * @return new field definition with updated datalist supplier
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, FrontendMappingFieldDefinition f, BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier) {
        return new FrontendMappingFieldDefinition(formName, f.name, f.type, f.canReadCheck, f.canWriteCheck, f.readPrivilege, f.writePrivilege, f.valueSupplier, datalistSupplier, true, f.fieldTypeFunction, f.datalistId, f.additionalCss, f.allowNull, f.url, f.htmlFragmentName, f.contentType, f.dtoToEntityValueConverter, f.entityToDtoValueConverter, f.actionPrivilege, f.actionUrl, f.actionLabelKey, f.sqlFormula, f.referencedEntityKey, f.nonDto, f.searchEnabled);
    }

    /**
     * Creates new field definition by copying existing definition and replacing privilege requirements.
     * Use to adapt field configuration to different security contexts.
     *
     * @param formName form name for i18n key generation
     * @param f existing field definition to copy
     * @param readPrivilege new read privilege requirement, null if no restriction
     * @param writePrivilege new write privilege requirement, null if no restriction
     * @return new field definition with updated privileges
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, FrontendMappingFieldDefinition f, PrivilegeBase readPrivilege, PrivilegeBase writePrivilege) {
        return new FrontendMappingFieldDefinition(formName, f.name, f.type, f.canReadCheck, f.canWriteCheck, readPrivilege, writePrivilege, f.valueSupplier, f.datalistSupplier, true, f.fieldTypeFunction, f.datalistId, f.additionalCss, f.allowNull, f.url, f.htmlFragmentName, f.contentType, f.dtoToEntityValueConverter, f.entityToDtoValueConverter, f.actionPrivilege, f.actionUrl, f.actionLabelKey, f.sqlFormula, f.referencedEntityKey, f.nonDto, f.searchEnabled);
    }

    /**
     * Creates new field definition by copying existing definition and replacing dynamic access checks.
     * Use to adapt field configuration to different entity-specific security logic.
     *
     * @param formName form name for i18n key generation
     * @param f existing field definition to copy
     * @param canReadCheck new read access check function, null if no check
     * @param canWriteCheck new write access check function, null if no check
     * @return new field definition with updated dynamic access checks
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, FrontendMappingFieldDefinition f, BiFunction<OrganizationUser, LongIdEntity, Boolean> canReadCheck, BiFunction<OrganizationUser, LongIdEntity, Boolean> canWriteCheck) {
        return new FrontendMappingFieldDefinition(formName, f.name, f.type, canReadCheck, canWriteCheck, f.readPrivilege, f.writePrivilege, f.valueSupplier, f.datalistSupplier, true, f.fieldTypeFunction, f.datalistId,  f.additionalCss, f.allowNull, f.url, f.htmlFragmentName, f.contentType, f.dtoToEntityValueConverter, f.entityToDtoValueConverter, f.actionPrivilege, f.actionUrl, f.actionLabelKey, f.sqlFormula, f.referencedEntityKey, f.nonDto, f.searchEnabled);
    }

    /**
     * Creates new field definition by copying existing definition and replacing value supplier.
     * Use to compute field value differently while preserving other configuration.
     *
     * @param formName form name for i18n key generation
     * @param f existing field definition to copy
     * @param valueSupplier new function computing field value from AbstractForm instance
     * @return new field definition with updated value supplier
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, FrontendMappingFieldDefinition f, Function<AbstractForm, Object> valueSupplier) {
        return new FrontendMappingFieldDefinition(formName, f.name, f.type, f.canReadCheck, f.canWriteCheck, f.readPrivilege, f.writePrivilege, valueSupplier, f.datalistSupplier, true, f.fieldTypeFunction, f.datalistId, f.additionalCss, f.allowNull, f.url, f.htmlFragmentName, f.contentType, f.dtoToEntityValueConverter, f.entityToDtoValueConverter, f.actionPrivilege, f.actionUrl, f.actionLabelKey, f.sqlFormula, f.referencedEntityKey, f.nonDto, f.searchEnabled);
    }

    /**
     * Creates new field definition by copying existing definition and replacing bidirectional value converters.
     * Use to customize DTO-entity transformation logic while preserving other configuration.
     *
     * @param formName form name for i18n key generation
     * @param f existing field definition to copy
     * @param dtoToEntityValueConverter new function transforming DTO value to entity value
     * @param entityToDtoValueConverter new function transforming entity value to DTO value
     * @return new field definition with updated value converters
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, FrontendMappingFieldDefinition f, Function dtoToEntityValueConverter, Function entityToDtoValueConverter) {
        return new FrontendMappingFieldDefinition(formName, f.name, f.type, f.canReadCheck, f.canWriteCheck, f.readPrivilege, f.writePrivilege, f.valueSupplier, f.datalistSupplier, true, f.fieldTypeFunction, f.datalistId, f.additionalCss, f.allowNull, f.url, f.htmlFragmentName, f.contentType, dtoToEntityValueConverter, entityToDtoValueConverter, f.actionPrivilege, f.actionUrl, f.actionLabelKey, f.sqlFormula, f.referencedEntityKey, f.nonDto, f.searchEnabled);
    }

    /**
     * Creates new field definition by copying existing definition and adding field-level action button.
     * Use to add contextual actions next to field (e.g., "Generate" button for auto-populated fields).
     *
     * @param formName form name for i18n key generation
     * @param f existing field definition to copy
     * @param actionLabelKey i18n key for action button label
     * @param actionUrl URL target for action button
     * @param actionPrivilege privilege required to display action button, null if no restriction
     * @return new field definition with field-level action
     */
    public static FrontendMappingFieldDefinition createFormFieldDefinition(String formName, FrontendMappingFieldDefinition f, String  actionLabelKey, String actionUrl, PrivilegeBase actionPrivilege ) {
        return new FrontendMappingFieldDefinition(formName, f.name, f.type, f.canReadCheck, f.canWriteCheck, f.readPrivilege, f.writePrivilege, f.valueSupplier, f.datalistSupplier, true, f.fieldTypeFunction, f.datalistId, f.additionalCss, f.allowNull, f.url, f.htmlFragmentName, f.contentType, f.dtoToEntityValueConverter, f.entityToDtoValueConverter, actionPrivilege, actionUrl, actionLabelKey, f.sqlFormula, f.referencedEntityKey, f.nonDto, f.searchEnabled);
    }

    /**
     * Returns field name for standard DTO-based form binding (dto.fieldName).
     * Equivalent to getName(false) for DTO-wrapped forms.
     *
     * @return field name in DTO binding format (e.g., "dto.username")
     */
    public String getName() {
        return name;
    }

    /**
     * Returns raw field name without DTO wrapper or map notation.
     * Use to access field name for key generation, logging, or metadata operations.
     *
     * @return plain field name (e.g., "username")
     */
    public String getPlainName() {
        return name;
    }

    /**
     * Returns field name formatted for form binding based on DTO structure.
     * Generates appropriate binding expression for map-based DTOs (dto[fieldName]),
     * object DTOs (dto.fieldName), or non-DTO direct binding (fieldName).
     *
     * @param dtoIsMap true if DTO is Map-based requiring bracket notation, false for object property notation
     * @return field name formatted for form binding (e.g., "dto[username]" or "dto.username" or "username")
     */
    public String getName(boolean dtoIsMap) {
        return nonDto ? name : (dtoIsMap ? "dto[" + name + "]" : "dto." + name );
    }

    /**
     * Checks if strict write access mode is enabled for this field.
     * When true, field is disabled or hidden if user lacks write access regardless of global form settings.
     *
     * @return true if strict write access control enabled, false for lenient mode
     */
    public boolean isStrictWriteAccess() {
        return strictWriteAccess;
    }

    /**
     * Sets strict write access mode for this field.
     * When enabled, field is disabled or hidden if user lacks write access regardless of global form settings.
     *
     * @param strictWriteAccess true to enable strict write access control, false for lenient mode
     */
    public void setStrictWriteAccess(boolean strictWriteAccess) {
        this.strictWriteAccess = strictWriteAccess;
    }

    /**
     * Checks if strict read access mode is enabled for this field.
     * When true, field is hidden if user lacks read access regardless of global form settings.
     *
     * @return true if strict read access control enabled, false for lenient mode
     */
    public boolean isStrictReadAccess() {
        return strictReadAccess;
    }
    
    /**
     * Constructs immutable field definition with complete configuration.
     * Protected constructor used by static factory methods. Generates i18n keys (label, placeholder, tooltip, etc.)
     * following formName.fieldName.suffix pattern and auto-prefixes datalistId if needed.
     *
     * @param formName form name for i18n key generation (e.g., "userForm")
     * @param name field name for binding and key generation (e.g., "username")
     * @param type static field type for rendering, null if dynamic via fieldTypeFunction
     * @param canReadCheck dynamic read access check evaluated at runtime, null if privilege-based only
     * @param canWriteCheck dynamic write access check evaluated at runtime, null if privilege-based only
     * @param readPrivilege required privilege for reading field value, null if no restriction
     * @param writePrivilege required privilege for modifying field value, null if no restriction
     * @param valueSupplier function computing field value from form state, null if entity-bound
     * @param datalistSupplier function computing datalist options from form and repository, null if static datalist
     * @param formBasedDatalistSupplier true if datalist supplier requires form context, false for repository-only
     * @param fieldTypeFunction function computing type dynamically from form state, null if static type
     * @param datalistId identifier for datalist options (auto-prefixed with "datalist-"), null if no datalist
     * @param additionalCss space-separated CSS classes for field styling, null for default styling
     * @param allowNull true if field accepts null values, false if required
     * @param url URL for field action or resource loading, null if not applicable
     * @param htmlFragmentName name of HTML fragment template for custom rendering, null for standard rendering
     * @param contentType MIME type for field value (e.g., "application/json"), null for default type
     * @param dtoToEntityValueConverter function transforming DTO value to entity value during populateTo, null for direct assignment
     * @param entityToDtoValueConverter function transforming entity value to DTO value during populateFrom, null for direct assignment
     * @param actionPrivilege required privilege for field-level action button, null if no action or unrestricted
     * @param actionUrl URL target for field-level action button, null if no action
     * @param actionLabelKey i18n key for field-level action button label, null if no action
     * @param sqlFormula SQL expression for computed field in dynamic entities, null if persisted field
     * @param referencedEntityKey entity key for foreign key relationships in dynamic entities, null if not a reference
     * @param nonDto true if field binds directly to model bypassing DTO, false for standard DTO binding
     * @param searchEnabled true to enable full-text search indexing for this field in dynamic entities
     */
    protected FrontendMappingFieldDefinition(
            String formName,
            String name,
            FieldType type,
            BiFunction<OrganizationUser, LongIdEntity, Boolean> canReadCheck,
            BiFunction<OrganizationUser, LongIdEntity, Boolean> canWriteCheck,
            PrivilegeBase readPrivilege,
            PrivilegeBase writePrivilege,
            Function<AbstractForm, Object> valueSupplier,
            BiFunction<DtoAndEntity, SecureEntityDictionaryRepository, Object> datalistSupplier,
            boolean formBasedDatalistSupplier,
            Function<Object, FieldType> fieldTypeFunction,
            String datalistId,
            String additionalCss,
            boolean allowNull,
            String url,
            String htmlFragmentName,
            String contentType,
            Function<?, ?> dtoToEntityValueConverter,
            Function<?, ?> entityToDtoValueConverter,
            PrivilegeBase actionPrivilege,
            String actionUrl,
            String actionLabelKey,
            String sqlFormula,
            String referencedEntityKey,
            boolean nonDto,
            boolean searchEnabled) {
        this.name = name;
        this.type = type;
        this.readPrivilege = readPrivilege;
        this.writePrivilege = writePrivilege;
        this.canReadCheck = canReadCheck;
        this.canWriteCheck = canWriteCheck;
        this.valueSupplier = valueSupplier;
        this.datalistSupplier = datalistSupplier;
        this.formBasedDatalistSupplier = formBasedDatalistSupplier;
        this.fieldTypeFunction = fieldTypeFunction;
        this.nonDto = nonDto;
        this.key = formName + "." + name;
        this.labelKey = formName + "." + name + ".label";
        this.placeholderKey = formName + "." + name + ".placeholder";
        this.tooltipKey = formName + "." + name + ".tooltip";
        this.warningKey = formName + "." + name + ".warning";
        this.alertKey = formName + "." + name + ".alert";
        this.descriptionKey = formName + "." + name + ".description";
        this.datalistId = StringUtils.isNotEmpty(datalistId) && datalistId.contains(DATALIST_PREFIX) ? datalistId : DATALIST_PREFIX + datalistId;
        this.additionalCss = additionalCss;
        this.allowNull = allowNull;
        this.url = url;
        this.htmlFragmentName = htmlFragmentName;
        this.contentType = contentType;
        this.dtoToEntityValueConverter = dtoToEntityValueConverter;
        this.entityToDtoValueConverter = entityToDtoValueConverter;
        this.actionPrivilege = actionPrivilege;
        this.actionUrl = actionUrl;
        this.actionLabelKey = actionLabelKey;
        this.sqlFormula = sqlFormula;
        this.referencedEntityKey = referencedEntityKey;
        this.searchEnabled = searchEnabled;

    }

}
