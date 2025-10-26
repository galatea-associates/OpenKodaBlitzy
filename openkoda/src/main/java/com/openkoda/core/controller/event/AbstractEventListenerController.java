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

package com.openkoda.core.controller.event;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.helper.JsonHelper;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.core.service.event.AbstractApplicationEvent;
import com.openkoda.dto.NotificationDto;
import com.openkoda.dto.OrganizationDto;
import com.openkoda.dto.payment.InvoiceDto;
import com.openkoda.dto.payment.PaymentDto;
import com.openkoda.dto.payment.PlanDto;
import com.openkoda.dto.payment.SubscriptionDto;
import com.openkoda.dto.system.FrontendResourceDto;
import com.openkoda.dto.system.ScheduledSchedulerDto;
import com.openkoda.dto.user.BasicUser;
import com.openkoda.dto.user.UserRoleDto;
import com.openkoda.form.EventListenerForm;
import com.openkoda.form.FrontendMappingDefinitions;
import com.openkoda.form.SendEventForm;
import com.openkoda.model.component.event.Event;
import com.openkoda.model.component.event.EventListenerEntry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.BindingResult;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract controller providing centralized EventListener entity lifecycle management and manual event emission functionality.
 * <p>
 * This controller implements business logic for event listener registration, update, and removal operations within the
 * OpenKoda platform. It leverages the Flow pipeline architecture for composable request handling and integrates with
 * {@link ComponentProvider} for repositories and services access. The controller enforces privilege-based security through
 * {@link HasSecurityRules} implementation.
 * </p>
 * <p>
 * Key responsibilities include:
 * <ul>
 * <li>Event listener CRUD operations with cluster-aware synchronization via {@link com.openkoda.core.service.event.EventListenerService}</li>
 * <li>Manual event emission from Admin panel for testing and debugging</li>
 * <li>DTO-to-event mapping for multiple event types (Invoice, Payment, Plan, Subscription, FrontendResource, Scheduler, User, UserRole, Organization, Notification)</li>
 * <li>Component export functionality for listener definitions</li>
 * <li>Form validation and entity population through validation services</li>
 * </ul>
 * </p>
 * <p>
 * Implementing classes (such as {@link EventListenerControllerHtml}) handle HTTP binding and response formatting,
 * while this abstract controller provides the core business logic implementation. All listener operations are cluster-aware,
 * ensuring consistency across distributed application instances.
 * </p>
 * <p>
 * The controller uses both secure and unsecure repository access patterns: secure repositories for read operations
 * with privilege enforcement, and unsecure repositories for mutation operations where privilege checks are performed
 * at the service layer.
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @since 2019-03-11
 * @see EventListenerControllerHtml
 * @see EventListenerEntry
 * @see com.openkoda.core.service.event.EventListenerService
 * @see com.openkoda.core.flow.Flow
 * @see ComponentProvider
 * @see HasSecurityRules
 */
public class AbstractEventListenerController extends ComponentProvider implements HasSecurityRules {

    /**
     * Retrieves a paginated list of event listeners matching the search criteria.
     * <p>
     * This method performs a secure repository search using Spring Data JPA {@link Specification} for advanced
     * query construction and {@link Pageable} for pagination and sorting. The search uses the secure repository
     * pattern to enforce privilege checks, ensuring users can only access listeners they have permission to view.
     * </p>
     * <p>
     * The method uses Flow pipeline composition with {@code Flow.init().thenSet()} to populate the model with
     * search results under the {@code eventListenerPage} key.
     * </p>
     *
     * @param eventListenerSearchTerm search string for filtering event listeners by name (supports partial matching)
     * @param eventListenerSpecification JPA Specification for constructing complex query predicates on EventListenerEntry fields
     * @param eventListenerPageable pagination and sorting parameters including page number, size, and sort criteria
     * @return PageModelMap containing the search results under model key {@code eventListenerPage} with page metadata
     * @see EventListenerEntry
     * @see org.springframework.data.jpa.domain.Specification
     * @see org.springframework.data.domain.Pageable
     */
    protected PageModelMap findListenersFlow(
            String eventListenerSearchTerm,
            Specification<EventListenerEntry> eventListenerSpecification,
            Pageable eventListenerPageable) {
        debug("[findListenersFlow]");
        return Flow.init()
                .thenSet(eventListenerPage, a -> repositories.secure.eventListener.search(eventListenerSearchTerm, null, eventListenerSpecification, eventListenerPageable))
                .execute();
    }

    /**
     * Retrieves an event listener by ID and initializes its form for editing within an organization context.
     * <p>
     * This method loads the {@link EventListenerEntry} entity from the database using the unsecure repository
     * (privilege checks occur at service layer), then creates an {@link EventListenerForm} initialized with the
     * entity data and organization context for multi-tenant operations.
     * </p>
     * <p>
     * The Flow pipeline populates two model keys: {@code eventListenerEntity} containing the loaded entity,
     * and {@code eventListenerForm} containing the initialized form ready for rendering or further processing.
     * </p>
     *
     * @param organizationId organization context identifier for multi-tenant listener operations (may be null for global listeners)
     * @param eListenerId EventListenerEntry database primary key for entity retrieval
     * @return PageModelMap containing model keys {@code eventListenerEntity} and {@code eventListenerForm}
     * @see EventListenerEntry
     * @see EventListenerForm
     */
    protected PageModelMap find(Long organizationId, long eListenerId) {
        debug("[find] ListenerId: {}", eListenerId);
        return Flow.init()
                .thenSet(eventListenerEntity, a -> repositories.unsecure.eventListener.findOne(eListenerId))
                .thenSet(eventListenerForm, a -> new EventListenerForm(organizationId, a.result))
                .execute();
    }


    /**
     * Creates a new event listener by validating form data, persisting the entity, and registering it cluster-wide.
     * <p>
     * This method executes a complete creation workflow:
     * <ol>
     * <li>Validates the form using Jakarta Bean Validation via {@code services.validation.validateAndPopulateToEntity}</li>
     * <li>Populates form data into a new {@link EventListenerEntry} entity</li>
     * <li>Persists the entity using {@code repositories.unsecure.eventListener.save}</li>
     * <li>Exports the listener configuration as a component for versioning and deployment</li>
     * <li>Registers the listener across all cluster nodes via {@code services.eventListener.registerListenerClusterAware}</li>
     * <li>Resets the form to a fresh instance for subsequent operations</li>
     * </ol>
     * </p>
     * <p>
     * The cluster-aware registration ensures the listener becomes active on all application instances, maintaining
     * consistency in distributed environments. Component export enables versioned deployment of listener configurations.
     * </p>
     *
     * @param eListenerForm validated form containing listener configuration (event type, code, enabled state)
     * @param br BindingResult for Jakarta Bean Validation error tracking and form error display
     * @return PageModelMap containing model keys {@code eventListenerForm} (reset to new instance) and {@code eventListenerEntity} (persisted entity)
     * @see EventListenerEntry
     * @see EventListenerForm
     * @see com.openkoda.core.service.event.EventListenerService#registerListenerClusterAware(EventListenerEntry)
     * @see com.openkoda.service.export.ComponentExportService
     */
    protected PageModelMap create(EventListenerForm eListenerForm, BindingResult br) {
        debug("[create]");
        return Flow.init(eventListenerForm, eListenerForm)
                .then(a -> services.validation.validateAndPopulateToEntity(eListenerForm, br,new EventListenerEntry()))
                .thenSet(eventListenerEntity, a -> repositories.unsecure.eventListener.save(a.result))
                .then(a -> services.componentExport.exportToFileIfRequired(a.result))
                .then(a -> services.eventListener.registerListenerClusterAware((EventListenerEntry) a.result))
                .thenSet(eventListenerForm, a -> new EventListenerForm())
                .execute();
    }

    /**
     * Updates an existing event listener by retrieving it, validating changes, persisting updates, and updating the cluster.
     * <p>
     * This method executes a complete update workflow:
     * <ol>
     * <li>Retrieves the existing {@link EventListenerEntry} from the database</li>
     * <li>Validates the form using Jakarta Bean Validation and populates changes into the entity</li>
     * <li>Persists the updated entity using {@code saveAndFlush} for immediate database synchronization</li>
     * <li>Exports the updated listener configuration for component versioning</li>
     * <li>Updates the listener across all cluster nodes via {@code services.eventListener.updateEventListenerClusterAware}</li>
     * </ol>
     * </p>
     * <p>
     * The cluster-aware update ensures all application instances reload the listener configuration, maintaining
     * consistency for changes to event handling logic, enabled state, or event type subscriptions.
     * </p>
     *
     * @param eventListenerId EventListenerEntry database primary key for entity retrieval and update
     * @param eListenerForm validated form containing updated listener configuration (event type, code, enabled state)
     * @param br BindingResult for Jakarta Bean Validation error tracking and form error display
     * @return PageModelMap containing model key {@code eventListenerForm} with submitted data
     * @see EventListenerEntry
     * @see EventListenerForm
     * @see com.openkoda.core.service.event.EventListenerService#updateEventListenerClusterAware(EventListenerEntry)
     */
    protected PageModelMap update(long eventListenerId, EventListenerForm eListenerForm, BindingResult br) {
        debug("[update] ListenerId: {}", eventListenerId);
        return Flow.init(eventListenerForm, eListenerForm)
                .then(a -> repositories.unsecure.eventListener.findOne(eventListenerId))
                .then(a -> services.validation.validateAndPopulateToEntity(eListenerForm, br, a.result))
                .then(a -> repositories.unsecure.eventListener.saveAndFlush(a.result))
                .then(a -> services.componentExport.exportToFileIfRequired(a.result))
                .then(a -> services.eventListener.updateEventListenerClusterAware((EventListenerEntry) a.result))
                .execute();
    }

    /**
     * Removes an event listener by deleting it from the database and unregistering it from all cluster nodes.
     * <p>
     * This method executes a complete deletion workflow:
     * <ol>
     * <li>Retrieves the {@link EventListenerEntry} and stores it in {@code eventListenerEntityToUnregister} model key</li>
     * <li>Removes any exported component files associated with the listener</li>
     * <li>Deletes the entity from the database using {@code deleteOne}</li>
     * <li>Unregisters the listener from all cluster nodes via {@code services.eventListener.unregisterEventListenerClusterAware}</li>
     * </ol>
     * </p>
     * <p>
     * The cluster-aware unregistration ensures the listener stops processing events on all application instances,
     * maintaining consistency across the distributed environment. Export cleanup removes versioned configuration files.
     * </p>
     *
     * @param eventListenerId EventListenerEntry database primary key for entity retrieval and deletion
     * @return PageModelMap containing model key {@code eventListenerEntityToUnregister} with the entity before deletion
     * @see EventListenerEntry
     * @see com.openkoda.core.service.event.EventListenerService#unregisterEventListenerClusterAware(EventListenerEntry)
     * @see com.openkoda.service.export.ComponentExportService
     */
    protected PageModelMap remove(long eventListenerId) {
        debug("[remove] ListenerId: {}", eventListenerId);
        return Flow.init(eventListenerId)
                .thenSet(eventListenerEntityToUnregister, a -> repositories.unsecure.eventListener.findOne(eventListenerId))
                .then(a -> services.componentExport.removeExportedFilesIfRequired(a.result))
                .then(a -> repositories.unsecure.eventListener.deleteOne( a.result.getId() ) )
                .then(a -> services.eventListener.unregisterEventListenerClusterAware(a.model.get(eventListenerEntityToUnregister)))
                .execute();
    }


    /**
     * Initializes the manual event emission UI by preparing an empty SendEventForm for event type selection.
     * <p>
     * This method creates a new {@link SendEventForm} instance and adds it to the model under the {@code sendEventForm} key,
     * enabling administrators to select an event type for manual emission in the Admin panel. Manual event emission is used
     * for testing event listeners and debugging event-driven workflows.
     * </p>
     * <p>
     * After event type selection, the {@link #prepareEvent(SendEventForm, BindingResult)} method is called to load
     * event-specific fields.
     * </p>
     *
     * @return PageModelMap containing model key {@code sendEventForm} with an empty form for event type selection
     * @see SendEventForm
     * @see #prepareEvent(SendEventForm, BindingResult)
     */
    protected PageModelMap chooseEvent() {
        debug("[chooseEvent]");
        return Flow.init()
                .thenSet(sendEventForm, a -> new SendEventForm())
                .execute();
    }

    /**
     * Prepares an event-specific form with fields matching the selected event type's DTO structure.
     * <p>
     * This method validates the event selection, creates an {@link Event} object from the selected event type,
     * and generates a {@link SendEventForm} populated with {@link FrontendMappingDefinitions} for the corresponding
     * DTO class. The form contains fields appropriate for the event's data structure, enabling administrators to
     * populate event data for manual emission.
     * </p>
     * <p>
     * The method uses {@link #getEventFormForClass(Event)} to map event types to their corresponding DTO forms
     * (e.g., InvoiceDto, PaymentDto, UserDto). The populated form is returned under the {@code sendEventForm} model key.
     * </p>
     *
     * @param eventForm SendEventForm containing the selected event type from {@link #chooseEvent()}
     * @param br BindingResult for Jakarta Bean Validation error tracking during event form validation
     * @return PageModelMap containing model key {@code sendEventForm} with event-specific fields based on the DTO structure
     * @see SendEventForm
     * @see Event
     * @see FrontendMappingDefinitions
     * @see #getEventFormForClass(Event)
     */
    protected PageModelMap prepareEvent(SendEventForm eventForm, BindingResult br) {
        debug("[prepareEvent]");
        return Flow.init(sendEventForm, eventForm)
                .then(a -> services.validation.validate(eventForm, br))
                .then(a -> new Event(a.result.getEvent()))
                .thenSet(sendEventForm, a -> getEventFormForClass(a.result))
                .execute();
    }

    /**
     * Processes manual event emission by parsing form data, constructing the event object, and emitting the event.
     * <p>
     * This method handles the final step of manual event emission from the Admin panel:
     * <ol>
     * <li>Extracts the event type from the {@code eventData} map and creates an {@link Event} object</li>
     * <li>Filters form fields with {@code dto.*} prefix using {@link StringUtils#isNotBlank} to include only populated fields</li>
     * <li>Converts the filtered map to JSON using {@link JsonHelper#formMapToJson}</li>
     * <li>Adds polymorphic type information with {@code object@} prefix for typed deserialization</li>
     * <li>Deserializes the JSON into the appropriate DTO object using {@link JsonHelper#fromDebugJson}</li>
     * <li>Resolves the event class via {@link AbstractApplicationEvent#getEvent}</li>
     * <li>Emits the event using {@code services.applicationEvent.emitEvent} for listener processing</li>
     * </ol>
     * </p>
     * <p>
     * The generic approach using Map allows handling diverse event types without type-specific code paths.
     * Supported event objects include InvoiceDto, PaymentDto, PlanDto, SubscriptionDto, FrontendResourceDto,
     * ScheduledSchedulerDto, BasicUser, UserRoleDto, OrganizationDto, and NotificationDto.
     * </p>
     *
     * @param eventData Map of event DTO field values with {@code dto.*} prefixed keys (e.g., "dto.name", "dto.amount")
     * @return PageModelMap containing model key {@code sendEventForm} with the event type that was emitted
     * @throws IOException if JSON parsing fails due to invalid event data format or incompatible DTO structure
     * @see com.openkoda.core.service.event.ApplicationEventService#emitEvent
     * @see AbstractApplicationEvent
     * @see JsonHelper
     */
    protected PageModelMap emitEvent(Map<String, String> eventData) throws IOException {
        debug("[emitEvent]");
        Event event = new Event(eventData.remove("event"));
        Map<String, String> objectData = eventData.entrySet().stream()
                .filter(e -> e.getKey().startsWith("dto.") && StringUtils.isNotBlank(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        String json = JsonHelper.formMapToJson(objectData);
        json = "{" + "\"object@" + event.getEventObjectType() + "\":" + json + "}";
        Object object = JsonHelper.fromDebugJson(json).get("object");
        AbstractApplicationEvent appEvent = AbstractApplicationEvent.getEvent(event.getEventName());
        return Flow.init(sendEventForm, new SendEventForm(event.getEventString()))
                .then(a -> services.applicationEvent.emitEvent(appEvent, object))
                .execute();
    }

    /**
     * Maps an event type to its corresponding SendEventForm with DTO-specific field definitions.
     * <p>
     * This private helper method uses a switch statement on {@code event.getEventObjectType()} to create
     * {@link SendEventForm} instances with appropriate {@link FrontendMappingDefinitions} for each supported
     * DTO type. The form fields are generated based on the DTO structure, enabling dynamic form rendering
     * in the Admin panel for manual event emission.
     * </p>
     * <p>
     * Supported event object types and their mappings:
     * <ul>
     * <li>{@code com.openkoda.dto.payment.InvoiceDto} → {@link FrontendMappingDefinitions#sendEventInvoiceDto}</li>
     * <li>{@code com.openkoda.dto.payment.PaymentDto} → {@link FrontendMappingDefinitions#sendEventPaymentDto}</li>
     * <li>{@code com.openkoda.dto.payment.PlanDto} → {@link FrontendMappingDefinitions#sendEventPlanDto}</li>
     * <li>{@code com.openkoda.dto.payment.SubscriptionDto} → {@link FrontendMappingDefinitions#sendEventSubscriptionDto}</li>
     * <li>{@code com.openkoda.dto.system.CmsDto} / {@link FrontendResourceDto} → {@link FrontendMappingDefinitions#sendEventFrontendResourceDto}</li>
     * <li>{@code com.openkoda.dto.system.ScheduledSchedulerDto} → {@link FrontendMappingDefinitions#sendEventScheduledSchedulerDto}</li>
     * <li>{@code com.openkoda.dto.user.BasicUser} → {@link FrontendMappingDefinitions#sendEventBasicUser}</li>
     * <li>{@code com.openkoda.dto.user.UserRoleDto} → {@link FrontendMappingDefinitions#sendEventUserRoleDto}</li>
     * <li>{@code com.openkoda.dto.OrganizationDto} → {@link FrontendMappingDefinitions#sendEventOrganizationDto}</li>
     * <li>{@code com.openkoda.dto.NotificationDto} → {@link FrontendMappingDefinitions#sendEventNotificationDto}</li>
     * <li>Unknown types → Empty {@link SendEventForm}</li>
     * </ul>
     * </p>
     *
     * @param event Event object containing the event type and associated DTO class name
     * @return SendEventForm initialized with a new DTO instance, appropriate FrontendMappingDefinitions, and event type string;
     *         returns empty form for unknown event types
     * @see SendEventForm
     * @see FrontendMappingDefinitions
     * @see Event#getEventObjectType()
     */
    private SendEventForm getEventFormForClass(Event event) {
        switch (event.getEventObjectType()){
            case "com.openkoda.dto.payment.InvoiceDto":
                return new SendEventForm<>(new InvoiceDto(), FrontendMappingDefinitions.sendEventInvoiceDto, event.getEventString());
            case "com.openkoda.dto.payment.PaymentDto":
                return new SendEventForm<>(new PaymentDto(), FrontendMappingDefinitions.sendEventPaymentDto, event.getEventString());
            case "com.openkoda.dto.payment.PlanDto":
                return new SendEventForm<>(new PlanDto(), FrontendMappingDefinitions.sendEventPlanDto, event.getEventString());
            case "com.openkoda.dto.payment.SubscriptionDto":
                return new SendEventForm<>(new SubscriptionDto(), FrontendMappingDefinitions.sendEventSubscriptionDto, event.getEventString());
            case "com.openkoda.dto.system.CmsDto":
                return new SendEventForm<>(new FrontendResourceDto(), FrontendMappingDefinitions.sendEventFrontendResourceDto, event.getEventString());
            case "com.openkoda.dto.system.ScheduledSchedulerDto":
                return new SendEventForm<>(new ScheduledSchedulerDto(), FrontendMappingDefinitions.sendEventScheduledSchedulerDto, event.getEventString());
            case "com.openkoda.dto.user.BasicUser":
                return new SendEventForm<>(new BasicUser(), FrontendMappingDefinitions.sendEventBasicUser, event.getEventString());
            case "com.openkoda.dto.user.UserRoleDto":
                return new SendEventForm<>(new UserRoleDto(), FrontendMappingDefinitions.sendEventUserRoleDto, event.getEventString());
            case "com.openkoda.dto.OrganizationDto":
                return new SendEventForm<>(new OrganizationDto(), FrontendMappingDefinitions.sendEventOrganizationDto, event.getEventString());
            case "com.openkoda.dto.NotificationDto":
                return new SendEventForm<>(new NotificationDto(), FrontendMappingDefinitions.sendEventNotificationDto, event.getEventString());
            default:
                return new SendEventForm();
        }
    }
}
