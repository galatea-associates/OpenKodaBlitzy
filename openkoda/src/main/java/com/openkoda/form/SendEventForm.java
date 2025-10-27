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
import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.dto.CanonicalObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;

/**
 * Generic one-field event carrier form for triggering server-side events.
 * <p>
 * This request-scoped form extends {@link AbstractForm} with a generic {@link CanonicalObject} DTO type parameter.
 * The form is designed for event dispatching scenarios where a simple event identifier needs to be validated
 * and transmitted from the client to the server. Event presence is validated via the {@link #validate(BindingResult)}
 * method using {@link StringUtils#isBlank(CharSequence)} check.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * SendEventForm<UserDTO> form = new SendEventForm<>("user.created");
 * form.validate(bindingResult);
 * }</pre>
 * </p>
 *
 * @param <T> the DTO type extending {@link CanonicalObject} associated with this form
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractForm
 * @see FrontendMappingDefinition
 * @see FrontendMappingDefinitions
 */
public class SendEventForm<T extends CanonicalObject> extends AbstractForm<T> {
    
    /**
     * Event identifier string that will be validated for presence.
     * <p>
     * This field holds the event name or identifier that triggers server-side processing.
     * Validation is performed via {@link #validate(BindingResult)} using {@link StringUtils#isBlank(CharSequence)}
     * to ensure the event identifier is not null, empty, or whitespace-only.
     * </p>
     */
    String event;

    /**
     * Default constructor initializing the form with the standard sendEventForm frontend mapping.
     * <p>
     * This constructor delegates to the parent {@link AbstractForm} constructor with
     * {@link FrontendMappingDefinitions#sendEventForm} as the frontend mapping definition.
     * The event field remains null until explicitly set via {@link #setEvent(String)} or
     * another constructor.
     * </p>
     */
    public SendEventForm() {
        super(FrontendMappingDefinitions.sendEventForm);
    }

    /**
     * Constructor allowing custom frontend mapping definition.
     * <p>
     * This constructor initializes the form with a custom {@link FrontendMappingDefinition},
     * providing flexibility for specialized form configurations beyond the standard sendEventForm mapping.
     * The event field remains null until explicitly set.
     * </p>
     *
     * @param frontendMappingDefinition the custom frontend mapping definition for this form
     */
    public SendEventForm(FrontendMappingDefinition frontendMappingDefinition) {
        super(frontendMappingDefinition);
    }

    /**
     * Constructor initializing the form with a DTO and custom frontend mapping definition.
     * <p>
     * This constructor enables initialization with both a data transfer object and a custom
     * frontend mapping, allowing the form to be pre-populated with entity data while using
     * a specialized form configuration. The event field remains null until explicitly set.
     * </p>
     *
     * @param dto the data transfer object extending {@link CanonicalObject}
     * @param frontendMappingDefinition the frontend mapping definition for this form
     */
    public SendEventForm(T dto, FrontendMappingDefinition frontendMappingDefinition) {
        super(dto, frontendMappingDefinition);
    }

    /**
     * Full constructor initializing the form with DTO, frontend mapping, and event identifier.
     * <p>
     * This constructor provides complete initialization of all form components: the data transfer object,
     * custom frontend mapping definition, and the event identifier. This is useful when the form needs
     * to be fully configured in a single step.
     * </p>
     *
     * @param dto the data transfer object extending {@link CanonicalObject}
     * @param frontendMappingDefinition the frontend mapping definition for this form
     * @param event the event identifier string to be set
     */
    public SendEventForm(T dto, FrontendMappingDefinition frontendMappingDefinition, String event) {
        super(dto, frontendMappingDefinition);
        this.event = event;
    }

    /**
     * Convenience constructor for initializing the form with only an event identifier.
     * <p>
     * This constructor delegates to the default constructor to set up the standard
     * {@link FrontendMappingDefinitions#sendEventForm} mapping, then immediately sets the event
     * identifier. This is the most common constructor for simple event dispatching scenarios.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * SendEventForm form = new SendEventForm("notification.sent");
     * }</pre>
     * </p>
     *
     * @param eventString the event identifier string to be set
     */
    public SendEventForm(String eventString) {
        this();
        this.event = eventString;
    }

    /**
     * Validates the event field for presence and adds error to binding result if blank.
     * <p>
     * This method overrides {@link AbstractForm#validate(BindingResult)} to perform event-specific
     * validation. It uses {@link StringUtils#isBlank(CharSequence)} to check if the event identifier
     * is null, empty, or contains only whitespace. If validation fails, a field error with code
     * "not.empty" is added to the provided {@link BindingResult}.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * form.validate(bindingResult);
     * if (bindingResult.hasErrors()) { ... }
     * }</pre>
     * </p>
     *
     * @param br the Spring {@link BindingResult} to which validation errors are added
     * @return this form instance for fluent method chaining
     */
    @Override
    public SendEventForm validate(BindingResult br) {
        if(StringUtils.isBlank(event)){
            br.rejectValue("event", "not.empty");
        }
        return this;
    }

    /**
     * Returns the event identifier string.
     * <p>
     * Retrieves the current event identifier that will be used for server-side event dispatching.
     * The returned value may be null if the event has not been set via constructor or
     * {@link #setEvent(String)}.
     * </p>
     *
     * @return the event identifier string, or null if not set
     */
    public String getEvent() {
        return event;
    }

    /**
     * Sets the event identifier string.
     * <p>
     * Updates the event identifier that will be validated and used for server-side event processing.
     * The provided value should not be blank if the form will be validated via {@link #validate(BindingResult)}.
     * </p>
     *
     * @param event the event identifier string to set
     */
    public void setEvent(String event) {
        this.event = event;
    }
}
