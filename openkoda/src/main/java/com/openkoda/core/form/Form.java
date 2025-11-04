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

import com.openkoda.core.flow.PostExecuteProcessablePageAttr;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import org.springframework.validation.BindingResult;
import reactor.util.function.Tuple2;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base abstraction for all form definitions in OpenKoda, providing field-level privilege checking
 * and validation capabilities. This class serves as the foundation for request-side form adapters
 * that handle data transfer between entities and web forms with security enforcement.
 * <p>
 * Forms in OpenKoda follow a three-phase lifecycle:
 * <ol>
 *   <li><b>Construction:</b> Form is created with a {@link FrontendMappingDefinition} that defines field metadata</li>
 *   <li><b>Privilege Evaluation:</b> Each field's read/write permissions are evaluated using {@link #canReadField} and {@link #canWriteField}</li>
 *   <li><b>Validation:</b> Form data is validated using {@link #validate} with Spring's BindingResult</li>
 *   <li><b>Rendering:</b> Form fields are rendered based on privilege checks stored in {@link #readWriteForField}</li>
 * </ol>
 * <p>
 * The {@link #readWriteForField} map maintains privilege state for each field, enabling the rendering
 * layer to show/hide fields based on user permissions. The map stores tuples where the first element
 * indicates read permission and the second indicates write permission for each field.
 * <p>
 * Example usage:
 * <pre>
 * FrontendMappingDefinition mapping = new FrontendMappingDefinition(fields);
 * MyForm form = new MyForm(mapping);
 * </pre>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FrontendMappingDefinition
 * @see FrontendMappingFieldDefinition
 */
public abstract class Form implements PostExecuteProcessablePageAttr, LoggingComponentWithRequestId {

    /**
     * Map storing privilege evaluation results for each field in this form.
     * The tuple contains (canRead, canWrite) permissions for field-level access control.
     * This map is populated during form construction and used during rendering to determine
     * which fields should be visible or editable based on the current user's privileges.
     */
    protected final Map<FrontendMappingFieldDefinition, Tuple2<Boolean, Boolean>> readWriteForField;
    
    /**
     * The frontend mapping definition containing field metadata and configuration for this form.
     * Defines the structure, validation rules, and rendering behavior for all form fields.
     */
    public final FrontendMappingDefinition frontendMappingDefinition;
    
    /**
     * Default error message displayed when field validation fails without a specific error message.
     */
    public static final String defaultErrorMessage = "Invalid value";
    
    /**
     * Flag indicating whether this form contains at least one writeable field for the current user.
     * Set to true when {@link #canWriteField} returns true for any field.
     */
    public boolean anyWriteableField = false;

    /**
     * Constructs a new form with the specified frontend mapping definition.
     * Initializes the privilege map with capacity based on the number of fields defined.
     *
     * @param frontendMappingDefinition the field definitions and metadata for this form
     */
    public Form(FrontendMappingDefinition frontendMappingDefinition) {
        this.readWriteForField = new LinkedHashMap<>(frontendMappingDefinition.fields.length);
        this.frontendMappingDefinition = frontendMappingDefinition;
    }

    /**
     * Validates this form's field values using Spring's validation framework.
     * Implementations should perform business logic validation and add errors to the BindingResult.
     * This method is called after data binding but before entity population.
     *
     * @param br the Spring BindingResult for collecting validation errors
     * @param <F> the specific form type being validated
     * @return this form instance for method chaining
     */
    public abstract <F extends Form> F validate(BindingResult br);

    /**
     * Checks if the current user has read permission for the specified field.
     * Returns true only if the field exists in the privilege map and read permission is granted.
     * This method is used by the rendering layer to determine field visibility.
     *
     * @param field the field definition to check read permission for
     * @return true if the user can read this field, false otherwise
     */
    public final boolean canReadField(FrontendMappingFieldDefinition field) {
        Tuple2<Boolean, Boolean> value = readWriteForField.get(field);
        return value != null && value.getT1() != null && value.getT1();
    }

    /**
     * Checks if the current user has write permission for the specified field.
     * Returns true only if the field exists in the privilege map and write permission is granted.
     * This method also updates the {@link #anyWriteableField} flag when any field is writeable.
     *
     * @param field the field definition to check write permission for
     * @return true if the user can modify this field, false otherwise
     */
    public final boolean canWriteField(FrontendMappingFieldDefinition field) {
        Tuple2<Boolean, Boolean> value = readWriteForField.get(field);
        boolean canWrite = value != null && value.getT2() != null && value.getT2();
        if(!anyWriteableField && canWrite) {
            anyWriteableField = true;
        }
        return canWrite;
    }

    /**
     * Returns the frontend mapping definition containing field metadata for this form.
     *
     * @return the frontend mapping definition
     */
    public FrontendMappingDefinition getFrontendMappingDefinition() {
        return frontendMappingDefinition;
    }

    /**
     * Checks if this form contains any fields that require a code editor component.
     * Used to determine if code editor JavaScript libraries should be loaded on the page.
     *
     * @return true if any field requires a code editor, false otherwise
     */
    public boolean requiresCodeEditor() {
        return Arrays.stream(frontendMappingDefinition.fields).anyMatch(a -> a.isCodeEditor(this));
    }
    /**
     * Checks if this form contains any code editor fields that require web endpoint autocomplete functionality.
     * Used to determine if autocomplete libraries for API endpoints should be loaded.
     *
     * @return true if any field requires web endpoint autocomplete, false otherwise
     */
    public boolean requiresCodeEditorWithWebendpointAutocomplete(){
        return Arrays.stream(frontendMappingDefinition.fields).anyMatch(a -> a.isCodeEditorWithWebendpointAutocomplete(this));
    }
    /**
     * Checks if this form contains any code editor fields that require form field autocomplete functionality.
     * Used to determine if autocomplete libraries for form fields should be loaded.
     *
     * @return true if any field requires form field autocomplete, false otherwise
     */
    public boolean requiresCodeEditorWithFormAutocomplete(){
        return Arrays.stream(frontendMappingDefinition.fields).anyMatch(a -> a.isCodeEditorWithFormAutocomplete(this));
    }
    /**
     * Checks if this form contains any fields that require a map component for geospatial data.
     * Used to determine if mapping JavaScript libraries should be loaded on the page.
     *
     * @return true if any field requires a map component, false otherwise
     */
    public boolean requiresMap() {
        return Arrays.stream(frontendMappingDefinition.fields).anyMatch(a -> a.isMap(this));
    }

    /**
     * Checks if this form contains any fields that require a rich text document editor component.
     * Used to determine if document editor JavaScript libraries should be loaded on the page.
     *
     * @return true if any field requires a document editor, false otherwise
     */
    public boolean requiresDocumentEditor() {
        return Arrays.stream(frontendMappingDefinition.fields).anyMatch(a -> a.isDocumentEditor(this));
    }

    /**
     * Checks if this form contains any fields that require file upload functionality.
     * Used to determine if file upload JavaScript libraries and handlers should be loaded.
     *
     * @return true if any field requires file upload, false otherwise
     */
    public boolean requiresFileUpload() {
        return Arrays.stream(frontendMappingDefinition.fields).anyMatch(a -> a.isFileUpload(this));
    }

    /**
     * Checks if this form contains any fields that require a color picker component.
     * Used to determine if color picker JavaScript libraries should be loaded on the page.
     *
     * @return true if any field requires a color picker, false otherwise
     */
    public boolean requiresColorPicker() {
        return Arrays.stream(frontendMappingDefinition.fields).anyMatch(a -> a.isColorPicker(this));
    }

    /**
     * Checks if this form contains any fields that require a time picker component.
     * Used to determine if time picker JavaScript libraries should be loaded on the page.
     *
     * @return true if any field requires a time picker, false otherwise
     */
    public boolean requiresTimePicker() {
        return Arrays.stream(frontendMappingDefinition.fields).anyMatch(a -> a.isTimePicker(this));
    }

    /**
     * Checks if this form contains any fields that require Google reCAPTCHA verification.
     * Used to determine if reCAPTCHA JavaScript libraries should be loaded on the page.
     *
     * @return true if any field requires reCAPTCHA, false otherwise
     */
    public boolean requiresReCaptcha() {
        return Arrays.stream(frontendMappingDefinition.fields).anyMatch(a -> a.isReCaptcha(this));
    }

}
