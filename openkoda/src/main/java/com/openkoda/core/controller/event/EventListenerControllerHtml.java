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

import com.openkoda.form.EventListenerForm;
import com.openkoda.form.SendEventForm;
import com.openkoda.model.component.event.Event;
import com.openkoda.model.component.event.EventListenerEntry;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

import static com.openkoda.core.controller.generic.AbstractController.*;

/**
 * Spring MVC REST controller providing HTML view adapters for event listener management.
 * <p>
 * This controller serves as the HTTP entry point for server-side rendered HTML pages managing
 * {@link EventListenerEntry} entities. It follows a delegation pattern where HTTP request
 * bindings are resolved, business logic is delegated to {@link AbstractEventListenerController},
 * and ModelAndView responses are generated via the {@code .mav()} method for Thymeleaf rendering.
 * </p>
 * <p>
 * All endpoints are mapped with the prefix {@code _HTML + _EVENTLISTENER} and integrate with
 * the Flow pipeline architecture for transactional execution. Security is enforced via
 * {@code @PreAuthorize} annotations requiring either {@code CHECK_CAN_READ_BACKEND} for
 * read operations or {@code CHECK_CAN_MANAGE_BACKEND} for write operations.
 * </p>
 * <p>
 * Form binding leverages Jakarta Bean Validation with {@code @Valid} annotations, and
 * validation errors are captured in {@code BindingResult} instances for conditional view
 * fragment selection (success vs. error fragments).
 * </p>
 * 
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @since 2019-03-11
 * @see AbstractEventListenerController
 * @see EventListenerEntry
 * @see EventListenerForm
 */
@RestController
@RequestMapping(_HTML + _EVENTLISTENER)
public class EventListenerControllerHtml extends AbstractEventListenerController {

    /**
     * Displays paginated list of all event listeners with optional search filtering.
     * <p>
     * Maps to {@code GET _HTML/_EVENTLISTENER/_ALL} and requires {@code CHECK_CAN_READ_BACKEND}
     * privilege. Delegates to {@link #findListenersFlow(String, Object, Pageable)} which executes
     * a Flow pipeline performing the search query, then renders the result as ModelAndView using
     * the "eventlistener-all" Thymeleaf template fragment.
     * </p>
     *
     * @param pageable pagination parameters qualified with "event" bean name for page size and sorting
     * @param search optional search string for filtering event listeners by name or properties (default: empty string)
     * @return ModelAndView containing paginated list of {@link EventListenerEntry} entities for "eventlistener-all" view
     */
    @PreAuthorize(CHECK_CAN_READ_BACKEND)
    @GetMapping(value = _ALL)
    public Object getAll(
            @Qualifier("event") Pageable pageable,
            @RequestParam(required = false, defaultValue = "", name = "event_search") String search) {
        debug("[getAll]");
        return findListenersFlow(search, null, pageable)
                .mav("eventlistener-" + ALL);
    }

    /**
     * Displays the settings configuration page for an existing event listener.
     * <p>
     * Maps to {@code GET _HTML/_EVENTLISTENER/{id}/_SETTINGS} and requires
     * {@code CHECK_CAN_READ_BACKEND} privilege. Delegates to {@link #find(Object, Long)} which
     * retrieves the specified {@link EventListenerEntry} via Flow pipeline, then renders the
     * "eventlistener-settings" view for editing.
     * </p>
     *
     * @param eListenerId the ID of the event listener entity to display, bound from path variable "id"
     * @return ModelAndView containing the {@link EventListenerEntry} entity and associated form for "eventlistener-settings" view
     */
    @PreAuthorize(CHECK_CAN_READ_BACKEND)
    @GetMapping(value = _ID + _SETTINGS)
    public Object settings(@PathVariable(ID) Long eListenerId)
    {debug("[settings] ListenerId: {}", eListenerId);
        return find(null, eListenerId)
                .mav("eventlistener-" + SETTINGS);
    }

    /**
     * Displays the configuration page for creating a new event listener.
     * <p>
     * Maps to {@code GET _HTML/_EVENTLISTENER/_NEW_SETTINGS} and requires
     * {@code CHECK_CAN_MANAGE_BACKEND} privilege for write access. Delegates to
     * {@link #find(Object, Long)} with ID=-1 to initialize an empty form, then renders
     * the "eventlistener-settings" view with a blank {@link EventListenerForm} for creation.
     * </p>
     *
     * @return ModelAndView containing an initialized empty {@link EventListenerForm} for "eventlistener-settings" view
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @GetMapping(_NEW_SETTINGS)
    public Object newListener() {
        debug("[newListener]");
        return find(null, -1L)
                .mav("eventlistener-" + SETTINGS);
    }

    /**
     * Processes form submission to update an existing event listener configuration.
     * <p>
     * Maps to {@code POST _HTML/_EVENTLISTENER/{id}/_SETTINGS} and requires
     * {@code CHECK_CAN_MANAGE_BACKEND} privilege. Validates the submitted {@link EventListenerForm}
     * via Jakarta Bean Validation, then delegates to {@link #update(Long, EventListenerForm, BindingResult)}
     * which executes the Flow pipeline for persistence. Returns conditional view fragments based on
     * validation success: "::eventlistener-settings-form-success" or "::eventlistener-settings-form-error".
     * </p>
     *
     * @param listenerId the ID of the event listener entity to update, bound from path variable "id"
     * @param eventListenerForm the validated form containing updated listener configuration fields
     * @param br binding result capturing validation errors from {@code @Valid} annotation
     * @return ModelAndView with success fragment on valid update or error fragment on validation failure
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @PostMapping(value = _ID + _SETTINGS)
    public Object updateEventListener(@PathVariable(ID) Long listenerId, @Valid EventListenerForm eventListenerForm, BindingResult br) {
        debug("[updateEventListener] ListenerId: {}", listenerId);
        return update(listenerId, eventListenerForm, br)
                .mav(ENTITY + '-' + FORMS + "::eventlistener-settings-form-success",
                        ENTITY + '-' + FORMS + "::eventlistener-settings-form-error");
    }

    /**
     * Processes form submission to create a new event listener entity.
     * <p>
     * Maps to {@code POST _HTML/_EVENTLISTENER/_NEW_SETTINGS} and requires
     * {@code CHECK_CAN_MANAGE_BACKEND} privilege. Validates the submitted {@link EventListenerForm}
     * via Jakarta Bean Validation, then delegates to {@link #create(EventListenerForm, BindingResult)}
     * which executes the Flow pipeline for database persistence. Returns conditional view fragments:
     * "::eventlistener-settings-form-success" on successful creation or
     * "::eventlistener-settings-form-error" on validation failure.
     * </p>
     *
     * @param eventListenerForm the validated form containing new listener configuration including event type, consumer name, and code
     * @param br binding result capturing validation errors such as required field violations
     * @return ModelAndView with success fragment containing created entity or error fragment with validation messages
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @PostMapping(_NEW_SETTINGS)
    public Object createEventListener(@Valid EventListenerForm eventListenerForm, BindingResult br) {
        debug("[createEventListener]");
        return create(eventListenerForm, br)
                .mav(ENTITY + '-' + FORMS + "::eventlistener-settings-form-success",
                        ENTITY + '-' + FORMS + "::eventlistener-settings-form-error");
    }

    /**
     * Deletes an existing event listener entity from the database.
     * <p>
     * Maps to {@code POST _HTML/_EVENTLISTENER/{id}/_REMOVE} and requires
     * {@code CHECK_CAN_MANAGE_BACKEND} privilege. Delegates to {@link #remove(Long)} which
     * executes the Flow pipeline for deletion, then generates a boolean-based ModelAndView
     * response using lambda functions {@code (a -> true)} for success and {@code (a -> false)}
     * for failure, enabling conditional view fragment selection.
     * </p>
     *
     * @param listenerId the ID of the event listener entity to delete, bound from path variable "id"
     * @return ModelAndView with boolean result indicating successful deletion (true) or failure (false)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @PostMapping(value = _ID_REMOVE)
    public Object removeEventListener(@PathVariable(ID) Long listenerId) {
        debug("[removeEventListener] ListenerId: {}", listenerId);
        return remove(listenerId)
                .mav(a -> true, a -> false);
    }

    /**
     * Displays the initial event type selection page for manual event emission from Admin panel.
     * <p>
     * Maps to {@code GET _HTML/_EVENTLISTENER/_SEND} and requires {@code CHECK_CAN_MANAGE_BACKEND}
     * privilege. Delegates to {@link #chooseEvent()} which prepares the available {@link Event}
     * types for selection, then renders the "eventlistener-send" view containing an event type
     * picker form for administrative manual event triggering.
     * </p>
     *
     * @return ModelAndView containing available event types for selection in "eventlistener-send" view
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @GetMapping(value = _SEND)
    public Object sendEventListener(){
        debug("[sendEventListener]");
        return chooseEvent()
                .mav("eventlistener-" + SEND);
    }

    /**
     * Processes event type selection and displays the DTO data entry form for manual event emission.
     * <p>
     * Maps to {@code POST _HTML/_EVENTLISTENER/_SEND} and requires {@code CHECK_CAN_MANAGE_BACKEND}
     * privilege. Validates the {@link SendEventForm} containing the selected event type, then
     * delegates to {@link #prepareEvent(SendEventForm, BindingResult)} which prepares the appropriate
     * DTO input form. Returns conditional fragments: "::eventlistener-emit-event" for successful
     * selection or "::eventlistener-choose-event-error" on validation failure.
     * </p>
     *
     * @param eventType the form containing the selected {@link Event} type identifier to emit
     * @param br binding result capturing validation errors from event type selection
     * @return ModelAndView with DTO form fragment on success or error fragment on invalid selection
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @PostMapping(value = _SEND)
    public Object chosenEvent(SendEventForm eventType, BindingResult br){
        debug("[chosenEvent]");
            return prepareEvent(eventType, br)
                .mav(ENTITY + '-' + FORMS + "::eventlistener-emit-event",
                        ENTITY + '-' + FORMS + "::eventlistener-choose-event-error");
    }

    /**
     * Processes the manual event emission with provided DTO data from Admin panel form submission.
     * <p>
     * Maps to {@code POST _HTML/_EVENTLISTENER/_EMIT} with content type
     * {@code application/x-www-form-urlencoded} and requires {@code CHECK_CAN_MANAGE_BACKEND}
     * privilege. Accepts form data as key-value pairs, delegates to {@link #emitEvent(Map)} which
     * parses the form parameters into the appropriate DTO structure and publishes the event to
     * registered listeners. Returns conditional fragments: "::eventlistener-emit-event-success"
     * on successful emission or "::eventlistener-emit-event-error" on processing failure.
     * </p>
     *
     * @param formData map of form field names to values containing the event DTO payload from x-www-form-urlencoded submission
     * @return ModelAndView with success fragment on event publication or error fragment on failure
     * @throws IOException if JSON parsing of form data fails during DTO construction or event serialization
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @PostMapping(value = _EMIT, headers = "Accept=application/x-www-form-urlencoded")
    public Object emitFormEvent(@RequestParam  Map<String,String> formData) throws IOException {
        debug("[emitEvent]");
        return emitEvent(formData)
                .mav(ENTITY + '-' + FORMS + "::eventlistener-emit-event-success",
                        ENTITY + '-' + FORMS + "::eventlistener-emit-event-error");
    }

}
