package com.openkoda.service.autocomplete;

import java.util.Map;

/**
 * Data Transfer Object for form field autocomplete suggestions with context metadata.
 * <p>
 * This class provides autocomplete suggestions for form field definitions in the OpenKoda
 * form designer and validation editor. It contains method suggestions with corresponding
 * documentation for both FormFieldDefinitionBuilderStart and FormFieldDefinitionBuilder APIs.
 * </p>
 * <p>
 * The response structure includes:
 * </p>
 * <ul>
 * <li>Suggestions list mapping method names to their documentation</li>
 * <li>Available form field configuration methods</li>
 * <li>Entity fields for data binding</li>
 * <li>Validation options for form field rules</li>
 * </ul>
 * <p>
 * This class extends {@link GenericAutocompleteService} to inherit common autocomplete
 * functionality for package-level operations. The response is typically serialized to JSON
 * for use in frontend autocomplete components within the form designer interface.
 * </p>
 * <p>
 * Typical usage occurs when a developer configures form fields using the visual form designer
 * or writes validation rules in the validation editor, where autocomplete assists with
 * available FormFieldDefinitionBuilder methods.
 * </p>
 *
 * @see FormAutocompleteService
 * @see com.openkoda.core.form.FormFieldDefinitionBuilderStart
 * @see com.openkoda.core.form.FormFieldDefinitionBuilder
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class FormAutocompleteResponse extends GenericAutocompleteService {

    /**
     * Map of FormFieldDefinitionBuilderStart method suggestions to their documentation.
     * <p>
     * Contains initial builder methods available at form field definition start,
     * such as text(), number(), select(), etc. Keys are method names, values are
     * descriptive documentation strings for autocomplete display.
     * </p>
     */
    private Map<String,String> builderStart;
    
    /**
     * Map of FormFieldDefinitionBuilder method suggestions to their documentation.
     * <p>
     * Contains fluent builder methods for configuring form field properties,
     * such as required(), label(), placeholder(), validate(), etc. Keys are
     * method names, values are descriptive documentation strings.
     * </p>
     */
    private Map<String,String> builder;

    /**
     * Returns the map of FormFieldDefinitionBuilderStart method suggestions.
     * <p>
     * Retrieves autocomplete suggestions for initial form field builder methods
     * that start field definitions, such as text(), checkbox(), or date().
     * </p>
     *
     * @return map containing method names as keys and documentation strings as values,
     *         or null if not yet populated
     */
    public Map<String, String> getBuilderStart() {
        return builderStart;
    }

    /**
     * Sets the map of FormFieldDefinitionBuilderStart method suggestions.
     * <p>
     * Populates autocomplete suggestions for form field definition starter methods.
     * Used when building the autocomplete response for form designer initialization.
     * </p>
     *
     * @param builderStart map containing method names as keys and documentation strings
     *                     as values, may be null to clear suggestions
     */
    public void setBuilderStart(Map<String, String> builderStart) {
        this.builderStart = builderStart;
    }

    /**
     * Returns the map of FormFieldDefinitionBuilder method suggestions.
     * <p>
     * Retrieves autocomplete suggestions for fluent builder methods that configure
     * form field properties, validation rules, and display options.
     * </p>
     *
     * @return map containing method names as keys and documentation strings as values,
     *         or null if not yet populated
     */
    public Map<String, String> getBuilder() {
        return builder;
    }

    /**
     * Sets the map of FormFieldDefinitionBuilder method suggestions.
     * <p>
     * Populates autocomplete suggestions for form field configuration methods.
     * Used when building the autocomplete response for ongoing field definition.
     * </p>
     *
     * @param builder map containing method names as keys and documentation strings
     *                as values, may be null to clear suggestions
     */
    public void setBuilder(Map<String, String> builder) {
        this.builder = builder;
    }
}
