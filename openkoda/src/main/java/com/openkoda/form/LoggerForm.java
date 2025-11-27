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

import com.openkoda.core.form.AbstractForm;
import com.openkoda.dto.system.LoggerDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Form for binding logger configuration data from HTTP requests.
 * <p>
 * This request-scoped form manages debug logging configuration by binding to {@link LoggerDto}.
 * When constructed with the parameterized constructor, it pre-seeds the buffer size field
 * (converting maxEntries to String) and extracts class names from the provided debug logger classes.
 * The {@link #validate(BindingResult)} method ensures the buffer size field is present and
 * contains valid numeric content, rejecting invalid values with standard validation error codes.

 * <p>
 * Example usage:
 * <pre>{@code
 * LoggerForm form = new LoggerForm(debugClasses, 100);
 * form.validate(bindingResult);
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractForm
 * @see LoggerDto
 * @see FrontendMappingDefinitions#loggerForm
 */
public class LoggerForm extends AbstractForm<LoggerDto> {

    /**
     * Initializes a new logger form with an empty DTO and default frontend mapping.
     * <p>
     * Creates a new instance with a fresh {@link LoggerDto} and binds it to
     * {@link FrontendMappingDefinitions#loggerForm} for request binding.

     */
    public LoggerForm() {
        super(new LoggerDto(), FrontendMappingDefinitions.loggerForm);
    }

    /**
     * Initializes a logger form pre-seeded with debug logger configuration.
     * <p>
     * Creates a new instance with a fresh {@link LoggerDto} and binds it to
     * {@link FrontendMappingDefinitions#loggerForm}. The DTO is pre-populated with
     * the maximum buffer size (converted to String) and a set of fully-qualified class names
     * extracted from the provided debug logger classes.

     *
     * @param debugLoggers Set of Class instances for which debug logging should be enabled
     * @param maxEntries Maximum buffer size for log entries (converted to String for bufferSizeField)
     */
    public LoggerForm(Set<Class> debugLoggers, int maxEntries) {
        super(new LoggerDto(), FrontendMappingDefinitions.loggerForm);
        dto.bufferSizeField = String.valueOf(maxEntries);
        dto.loggingClasses = debugLoggers.stream().map(Class::getName).collect(Collectors.toSet());
    }

    /**
     * Validates form data using custom logic for buffer size field.
     * <p>
     * Performs two validation checks on the buffer size field:

     * <ol>
     *   <li>Presence check: Rejects with error code 'not.empty' if the field is blank</li>
     *   <li>Numeric check: Rejects with error code 'is.number' if the field contains
     *       non-numeric characters (validated using regex pattern {@code \d+})</li>
     * </ol>
     * <p>
     * Returns this form instance to support fluent method chaining in controller workflows.

     *
     * @param br BindingResult for collecting validation errors
     * @return This form instance for fluent chaining
     */
    @Override
    public LoggerForm validate(BindingResult br) {
        if (StringUtils.isBlank(dto.bufferSizeField)) {
            br.rejectValue("dto.bufferSizeField", "not.empty");
        }
        if (!StringUtils.isBlank(dto.bufferSizeField) && !dto.bufferSizeField.matches("\\d+")) {
            br.rejectValue("dto.bufferSizeField", "is.number");
        }
        return this;
    }

}
