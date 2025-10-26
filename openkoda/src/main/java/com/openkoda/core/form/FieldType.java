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

/**
 * Enumeration of available form field types with HTML rendering mappings.
 * <p>
 * FieldType defines all supported input field types for the OpenKoda form framework.
 * Each field type maps to a specific HTML representation and optionally to a database
 * column type for dynamic entity generation. Field types are categorized into groups
 * including text inputs, date/time pickers, boolean controls, code editors, foreign key
 * references, dropdown selectors, visual dividers, file handlers, and specialized components.
 * </p>
 * <p>
 * Fields are rendered in HTML templates using the field type name. Each type specifies
 * whether it stores a value in the database and defines the corresponding database column
 * type when applicable. UI-only fields such as dividers and buttons do not store values.
 * </p>
 * <p>
 * Example usage in form field definition:
 * <pre>{@code
 * builder.text("username").label("Username")
 * builder.dropdown("status").label("Status")
 * }</pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FieldDbType
 * @see FrontendMappingFieldDefinition
 */
public enum FieldType {

    /** Text input field storing up to 255 characters. Renders as standard HTML text input. */
    text(FieldDbType.VARCHAR_255),
    
    /** Password input field with masked characters. Stores up to 255 characters. */
    password(FieldDbType.VARCHAR_255),
    
    /** Hidden input field for storing numeric identifiers. Not visible in the UI. */
    hidden(FieldDbType.BIGINT),

    /** Date picker field storing calendar dates without time component. */
    date(FieldDbType.DATE),
    
    /** Date and time picker storing timestamps with timezone information. */
    datetime(FieldDbType.TIMESTAMP_W_TZ),
    
    /** Time picker field storing time values with timezone information. */
    time(FieldDbType.TIME_W_TZ),

    /** Checkbox control for boolean values. Renders as standard checkbox input. */
    checkbox(FieldDbType.BOOLEAN),
    
    /** Checkbox with warning message displayed on selection. Stores boolean value. */
    checkbox_with_warning(FieldDbType.BOOLEAN),
    
    /** Toggle switch control for boolean values. Modern alternative to checkbox. */
    switch_values(FieldDbType.BOOLEAN),
    
    /** Toggle switch with warning message on activation. Stores boolean value. */
    switch_values_with_warning(FieldDbType.BOOLEAN),

    /** Multi-line text area storing up to 1000 characters. For larger text content. */
    textarea(FieldDbType.VARCHAR_1000),

    /** Code editor for HTML with syntax highlighting. Stores up to 262144 characters. */
    code_html(FieldDbType.VARCHAR_262144),
    
    /** Code editor for CSS with syntax highlighting. Stores up to 262144 characters. */
    code_css(FieldDbType.VARCHAR_262144),
    
    /** Code editor for JavaScript with syntax highlighting. Stores up to 262144 characters. */
    code_js(FieldDbType.VARCHAR_262144),
    
    /** JavaScript code editor with web endpoint autocomplete support. */
    code_with_webendpoint_autocomplete(FieldDbType.VARCHAR_262144),
    
    /** JavaScript code editor with form field autocomplete support. */
    code_with_form_autocomplete(FieldDbType.VARCHAR_262144),

    /** Foreign key reference field storing entity ID. Used for many-to-one relationships. */
    many_to_one(FieldDbType.BIGINT),
    
    /** Specialized selector for organization entities. Stores organization ID. */
    organization_select(FieldDbType.BIGINT),
    
    /** Selector for OpenKoda module names. Stores module identifier string. */
    module_select(FieldDbType.VARCHAR_255),

    /** HTML5 datalist field providing autocomplete suggestions. UI-only, no stored value. */
    datalist(false),
    
    /** Dropdown selector for single value from predefined options. Stores up to 255 characters. */
    dropdown(FieldDbType.VARCHAR_255),
    
    /** Dropdown with conditional disable logic. Does not store database value. */
    dropdown_with_disable,
    
    /** Radio button group for single selection. Stores selected value up to 255 characters. */
    radio_list(FieldDbType.VARCHAR_255),
    
    /** Radio button group without label rendering. Does not store database value. */
    radio_list_no_label,
    
    /** Dropdown populated with entity instances. Does not store database value directly. */
    dropdown_with_entities,

    /** Multiple checkbox selection field. Does not store database value directly. */
    checkbox_list,
    
    /** Grouped multiple checkbox selection. Does not store database value directly. */
    checkbox_list_grouped,
    
    /** Multi-select dropdown storing comma-separated values. Stores up to 1000 characters. */
    multiselect(FieldDbType.VARCHAR_1000),

    /** Visual separator for form sections. UI-only element, does not store value. */
    divider,
    
    /** Section header with embedded hyperlink. UI-only element. */
    section_with_link,
    
    /** Section header with checkbox control. Does not store database value. */
    section_with_checkbox,
    
    /** Section header with checkbox and warning message. Does not store database value. */
    section_with_checkbox_with_warning,
    
    /** Section header with toggle switch control. Does not store database value. */
    section_with_switch,
    
    /** Section with toggle switch controlling content visibility. UI-only element. */
    section_with_switch_content(false),

    /** Action button element. UI-only, does not store value. */
    button,
    
    /** Submit button opening results in new browser tab. UI-only element. */
    submit_to_new_tab(false),

    /** Numeric input field with validation. Stores decimal values. */
    number(FieldDbType.NUMERIC),
    
    /** Geographic map coordinate field. Stores location data up to 255 characters. */
    map(FieldDbType.VARCHAR_255),

    /** Document attachment field. Does not store database value directly. */
    document,
    
    /** Conditional rule with single action branch. Does not store database value. */
    rule_then,
    
    /** Conditional rule with action and alternative branches. Does not store database value. */
    rule_then_else,

    /** Image URL input field. Stores image reference up to 255 characters. */
    image_url(FieldDbType.VARCHAR_255),

    /** Multiple files selector from library. Does not store database value directly. */
    files_library,
    
    /** Single file selector from library. Does not store database value directly. */
    file_library,
    
    /** Multiple images selector from library. Does not store database value directly. */
    images_library,
    
    /** Single image selector from library. Does not store database value directly. */
    image_library,
    
    /** Image upload field. Does not store database value directly. */
    image,
    
    /** Multiple file upload field. Stores file references up to 255 characters. */
    files(FieldDbType.VARCHAR_255),

    /** One-to-many relationship component. Does not store database value directly. */
    one_to_many,
    
    /** Color picker input field. Stores color value up to 255 characters. */
    color_picker(FieldDbType.VARCHAR_255),

    /** Section with embedded dropdown selector. Stores selected value up to 255 characters. */
    section_with_dropdown(FieldDbType.VARCHAR_255),

    /** Google reCAPTCHA verification field. UI-only element. */
    recaptcha,
    
    /** Generic div container element. UI-only element. */
    div
    ;

    /**
     * Indicates whether this field type stores a value in the database.
     * <p>
     * Fields that store values map to database columns during dynamic entity generation.
     * UI-only fields such as dividers, buttons, and sections return false and do not
     * create corresponding database columns.
     * </p>
     *
     * @return true if field stores a database value, false for UI-only elements
     */
    public boolean hasValue() {
        return hasValue;
    }
    /**
     * Returns the name of this field type for template rendering.
     * <p>
     * The name corresponds to the enum constant identifier and is used in HTML templates
     * to determine the appropriate rendering logic for each field type.
     * </p>
     *
     * @return the field type name (e.g., "text", "dropdown", "checkbox")
     */
    public String getName() {
        return name();
    }

    /**
     * Returns the associated database column type for dynamic entity generation.
     * <p>
     * The database type determines the SQL column type created when this field is used
     * in a dynamic entity definition. Returns null for UI-only fields that do not store
     * database values.
     * </p>
     *
     * @return the database column type, or null if field does not store a value
     * @see FieldDbType
     */
    public FieldDbType getDbType() {
        return dbType;
    }


    /**
     * Constructs a field type with specified value storage and database type.
     *
     * @param hasValue whether this field stores a database value
     * @param dbType the database column type for dynamic entity generation
     */
    FieldType(boolean hasValue, FieldDbType dbType) {
        this.hasValue = hasValue;
        this.dbType = dbType;
    }

    /**
     * Constructs a field type with database storage enabled.
     *
     * @param dbType the database column type for dynamic entity generation
     */
    FieldType(FieldDbType dbType) {
        this.hasValue=true;
        this.dbType = dbType;
    }

    /**
     * Constructs a field type without database storage.
     *
     * @param hasValue whether this field stores a database value
     */
    FieldType(boolean hasValue) {
        this.hasValue = hasValue;
        this.dbType = null;
    }

    /**
     * Constructs a field type with database storage enabled and no explicit database type.
     */
    FieldType() {
        this.hasValue = true;
        this.dbType = null;
    }

    private boolean hasValue;
    private FieldDbType dbType;

}
