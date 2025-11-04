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

import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.dto.system.EventListenerDto;
import com.openkoda.model.component.event.Consumer;
import com.openkoda.model.component.event.Event;
import com.openkoda.model.component.event.EventListenerEntry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;

/**
 * Parses and validates comma-delimited consumer and event descriptors for event listener configuration.
 * <p>
 * This form validates static parameter counts and type compatibility using reflection with {@code Class.forName}
 * and the {@link #isPerfectMatch(String, String)} helper method. It extends {@link AbstractOrganizationRelatedEntityForm}
 * to provide organization-scoped form handling for {@link EventListenerEntry} entities.

 * <p>
 * The form parses comma-delimited strings containing consumer and event information:
 * <ul>
 *   <li>Consumer format: className,methodName,parameterClassName</li>
 *   <li>Event format: className,eventName,objectType</li>
 * </ul>
 * Reflection-based validation ensures that the event object type is compatible with the consumer parameter type.
 * Static data parameters (staticData1-4) are validated against the consumer's expected parameter count.

 * <p>
 * Reflection failures are logged using {@link LoggingComponentWithRequestId} and rethrown as {@link RuntimeException}.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractOrganizationRelatedEntityForm
 * @see EventListenerEntry
 * @see EventListenerDto
 */
public class EventListenerForm extends AbstractOrganizationRelatedEntityForm<EventListenerDto, EventListenerEntry> implements LoggingComponentWithRequestId {

    /**
     * Creates a new EventListenerForm with default initialization.
     * <p>
     * This constructor initializes the form with the predefined frontend mapping definition
     * from {@code FrontendMappingDefinitions.eventListenerForm}.

     */
    public EventListenerForm() {
        super(FrontendMappingDefinitions.eventListenerForm);
    }

    /**
     * Creates a new EventListenerForm initialized with an existing entity.
     * <p>
     * This constructor prepares the form for editing an existing {@link EventListenerEntry}
     * by associating it with an organization and initializing the DTO.

     *
     * @param organizationId the ID of the organization this event listener belongs to
     * @param entity the existing {@link EventListenerEntry} entity to populate the form from
     */
    public EventListenerForm(Long organizationId, EventListenerEntry entity) {
        super(organizationId, new EventListenerDto(), entity, FrontendMappingDefinitions.eventListenerForm);
    }

    /**
     * Transfers event listener entity data to the EventListenerDto.
     * <p>
     * This method populates the form's DTO with data from the {@link EventListenerEntry} entity,
     * including comma-delimited consumer and event descriptor strings, static data parameters
     * (staticData1-4), and the organization ID. The consumer and event strings contain class names,
     * method/event names, and parameter types in comma-separated format.

     *
     * @param entity the {@link EventListenerEntry} entity to populate from
     * @return this form instance for method chaining
     */
    @Override
    public EventListenerForm populateFrom(EventListenerEntry entity) {
        dto.consumer = entity.getConsumerString();
        dto.event = entity.getEventString();
        dto.staticData1 = entity.getStaticData1();
        dto.staticData2 = entity.getStaticData2();
        dto.staticData3 = entity.getStaticData3();
        dto.staticData4 = entity.getStaticData4();
        dto.organizationId = entity.getOrganizationId();
        return this;
    }

    /**
     * Extracts a specific parameter from a comma-delimited parameter string.
     * <p>
     * This helper method splits the parameter string by commas and returns the value
     * at the specified index position. It asserts that the index is within bounds.

     *
     * @param allParams the comma-delimited string containing all parameters
     * @param n the zero-based index of the parameter to extract
     * @return the parameter value at position n
     */
    private String getConsumerOrEventParam(String allParams, int n)
    {
        String[] parameters = allParams.split(",");
        assert(parameters.length > n);
        return parameters[n];
    }

    /**
     * Applies validated DTO data to the EventListenerEntry entity.
     * <p>
     * This method performs reverse comma-delimited concatenation, extracting individual components
     * from the consumer and event descriptor strings in the DTO and setting them as separate fields
     * on the {@link EventListenerEntry} entity. The consumer descriptor is split into className,
     * methodName, and parameterClassName. The event descriptor is split into className, eventName,
     * and objectType. Static data parameters and organization ID are also transferred.

     *
     * @param entity the {@link EventListenerEntry} entity to populate with DTO values
     * @return the populated entity instance
     * @Override
     */
    @Override
    protected EventListenerEntry populateTo(EventListenerEntry entity) {
        entity.setConsumerClassName(getSafeValue(entity.getConsumerClassName(), CONSUMER_, (s -> getConsumerOrEventParam((String) s,0))));
        entity.setConsumerMethodName(getSafeValue(entity.getConsumerMethodName(), CONSUMER_, (s -> getConsumerOrEventParam((String) s,1))));
        entity.setConsumerParameterClassName(getSafeValue(entity.getConsumerParameterClassName(), CONSUMER_, (s -> getConsumerOrEventParam((String) s,2))));
        entity.setStaticData1(getSafeValue(entity.getStaticData1(), STATIC_DATA_1_, nullOnEmpty));
        entity.setStaticData2(getSafeValue(entity.getStaticData2(), STATIC_DATA_2_, nullOnEmpty));
        entity.setStaticData3(getSafeValue(entity.getStaticData3(), STATIC_DATA_3_, nullOnEmpty));
        entity.setStaticData4(getSafeValue(entity.getStaticData4(), STATIC_DATA_4_, nullOnEmpty));
        entity.setEventClassName(getSafeValue(entity.getEventClassName(), EVENT_, (s -> getConsumerOrEventParam((String) s,0))));
        entity.setEventName(getSafeValue(entity.getEventName(), EVENT_, (s -> getConsumerOrEventParam((String) s,1))));
        entity.setEventObjectType(getSafeValue(entity.getEventObjectType(), EVENT_, (s -> getConsumerOrEventParam((String) s,2))));
        entity.setOrganizationId(getSafeValue(entity.getOrganizationId(), ORGANIZATION_ID_));
        return entity;
    }

    /**
     * Validates the event listener configuration by parsing consumer and event descriptor strings.
     * <p>
     * This method performs comprehensive validation including:

     * <ul>
     *   <li>Parsing consumer and event strings into {@link Consumer} and {@link Event} objects</li>
     *   <li>Performing reflection-based type checking using {@link #isPerfectMatch(String, String)}</li>
     *   <li>Validating parameter compatibility between event object type and consumer parameter type</li>
     *   <li>Validating static data parameter counts against the consumer's expected parameter count</li>
     *   <li>Rejecting with appropriate error codes for incompatibilities and missing required data</li>
     * </ul>
     * <p>
     * Type compatibility validation uses {@code Class.forName} to load classes and
     * {@code Class.isAssignableFrom} to verify that the event object type can be passed
     * to the consumer method. Validation failures are recorded in the {@link BindingResult}
     * with error codes: "not.empty", "not.valid", or "incompatible.consumer".

     *
     * @param br the {@link BindingResult} to record validation errors
     * @return this form instance for method chaining
     */
    @Override
    public EventListenerForm validate(BindingResult br) {

        if (dto.event == "") {
            br.rejectValue("dto.event", "not.empty", defaultErrorMessage);
        } else {
            dto.eventObj = new Event(dto.event);
        }
        if (dto.consumer == null) {
            br.rejectValue("dto.consumer", "not.empty", defaultErrorMessage);
        } else {
            dto.consumerObj = new Consumer(dto.consumer);
        }
        if (dto.consumer != null && dto.event != null) {
            String eventObjectClassName = StringUtils.substringAfterLast(dto.event, ",");
            String consumerObjectClassName = dto.consumer.split(",")[2];
            if (!isPerfectMatch(eventObjectClassName, consumerObjectClassName)
                    && !isPerfectMatch(dto.eventObj.getEventClassName(), consumerObjectClassName)) {
                br.rejectValue("dto.consumer", "incompatible.consumer", defaultErrorMessage);
                br.rejectValue("dto.event", "incompatible.consumer", defaultErrorMessage);
            }
        }
        if (dto.consumer != null && !(dto.consumerObj.getNumberOfStaticParams() >= 1) && !StringUtils.isBlank
                (dto.staticData1)) {
            br.rejectValue("dto.staticData1", "not.valid", defaultErrorMessage);
        }
        if (dto.consumer != null && !(dto.consumerObj.getNumberOfStaticParams() >= 2) && !StringUtils.isBlank
                (dto.staticData2)) {
            br.rejectValue("dto.staticData2", "not.valid", defaultErrorMessage);
        }
        if (dto.consumer != null && !(dto.consumerObj.getNumberOfStaticParams() >= 3) && !StringUtils.isBlank
                (dto.staticData3)) {
            br.rejectValue("dto.staticData3", "not.valid", defaultErrorMessage);
        }
        if (dto.consumer != null && !(dto.consumerObj.getNumberOfStaticParams() == 4) && !StringUtils.isBlank
                (dto.staticData4)) {
            br.rejectValue("dto.staticData4", "not.valid", defaultErrorMessage);
        }

        if (dto.consumer != null && dto.consumerObj.getNumberOfStaticParams() >= 1 && StringUtils.isBlank(dto.staticData1)) {
            br.rejectValue("dto.staticData1", "not.empty", defaultErrorMessage);
        }
        if (dto.consumer != null && dto.consumerObj.getNumberOfStaticParams() >= 2 && StringUtils.isBlank(dto.staticData2)) {
            br.rejectValue("dto.staticData2", "not.empty", defaultErrorMessage);
        }
        if (dto.consumer != null && dto.consumerObj.getNumberOfStaticParams() >= 3 && StringUtils.isBlank(dto.staticData3)) {
            br.rejectValue("dto.staticData3", "not.empty", defaultErrorMessage);
        }
        if (dto.consumer != null && dto.consumerObj.getNumberOfStaticParams() == 4 && StringUtils.isBlank(dto.staticData4)) {
            br.rejectValue("dto.staticData4", "not.empty", defaultErrorMessage);
        }
        return this;
    }

    /**
     * Validates type compatibility between event and consumer classes using reflection.
     * <p>
     * This method uses {@code Class.forName} to load both the event class and consumer parameter class,
     * then checks whether the consumer parameter type can accept instances of the event type using
     * {@code Class.isAssignableFrom}. This ensures that the event object can be passed to the consumer
     * method without type errors at runtime.

     * <p>
     * If reflection fails (e.g., class not found or security restrictions), the error is logged
     * using {@link LoggingComponentWithRequestId} and rethrown as a {@link RuntimeException}.

     *
     * @param eventClassName the fully qualified class name of the event object type
     * @param consumerClassName the fully qualified class name of the consumer parameter type
     * @return {@code true} if the consumer class is assignable from the event class, {@code false} otherwise
     * @throws RuntimeException if reflection fails (class loading, security exceptions)
     */
    public boolean isPerfectMatch(String eventClassName, String consumerClassName) {
        try {
            Class<?> consumerObjectClass = Class.forName(consumerClassName);
            Class<?> eventObjectClass = Class.forName(eventClassName);
            return consumerObjectClass.isAssignableFrom(eventObjectClass);
        } catch (Exception e) {
            error(e, "Error when trying to find classes for EventListener: {} {}", eventClassName, consumerClassName);
            throw new RuntimeException(e);
        }
    }
}
