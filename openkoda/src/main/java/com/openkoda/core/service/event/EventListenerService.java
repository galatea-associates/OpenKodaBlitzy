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

package com.openkoda.core.service.event;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.helper.ClusterHelper;
import com.openkoda.core.helper.NameHelper;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.component.event.Consumer;
import com.openkoda.model.component.event.EventListenerEntry;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import static com.openkoda.core.service.event.ApplicationEvent.*;

/**
 * Event listener discovery, conversion, and lifecycle management service for persisted EventListenerEntry records
 * with reflection-based consumer registration.
 * <p>
 * This service provides comprehensive event listener management capabilities including:
 * <ul>
 *   <li>Discovery of event types and consumers via reflection</li>
 *   <li>Maintenance of UI dropdowns for event and consumer selection</li>
 *   <li>Registration of persisted listeners from database</li>
 *   <li>Cluster-aware registration and unregistration</li>
 * </ul>

 * <p>
 * <b>Architecture:</b> Extends {@link ComponentProvider} for services and repositories access; implements
 * {@link HasSecurityRules} for privilege checking. The {@code @DependsOn} annotation ensures
 * {@link ApplicationEventService} is initialized before this service.

 * <p>
 * <b>State Management:</b> Maintains ordered LinkedHashMaps (events, consumers, consumersArray) and eventClasses list.
 * <b>WARNING:</b> Internal collections are NOT synchronized and unsafe for concurrent modifications.

 * <p>
 * <b>Reflection:</b> Uses reflection to discover {@link AbstractApplicationEvent} fields, resolve consumer methods,
 * and build metadata for UI dropdowns.

 * <p>
 * <b>Cluster Integration:</b> Cluster-aware methods delegate to {@link ClusterEventSenderService} in clustered mode;
 * direct registration in single-instance mode.

 * <p>
 * Example usage:
 * <pre>{@code
 * // Discovery
 * eventListenerService.registerEventClasses(new Class[]{ApplicationEvent.class});
 * 
 * // Registration
 * eventListenerService.registerListenerClusterAware(listenerEntry);
 * }</pre>

 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @since 1.7.1
 * @see ApplicationEventService
 * @see EventConsumer
 * @see EventListenerEntry
 * @see ClusterEventSenderService
 */
@DependsOn({"allServices", "applicationEventService"})
@Service
public class EventListenerService extends ComponentProvider implements HasSecurityRules {

    /**
     * LinkedHashMap maintaining insertion order of discovered event descriptors for UI dropdown rendering.
     * <p>
     * Key format: 'EventClassName,EventName,EventObjectType' (comma-separated)
     * <br>Value format: 'EventName (ObjectType)' for display in UI

     * <p>
     * <b>WARNING:</b> NOT synchronized; unsafe for concurrent modifications.

     */
    private Map<Object, String> events = new LinkedHashMap<>();
    
    /**
     * LinkedHashMap of discovered consumer methods for UI dropdown rendering.
     * <p>
     * Key: canonical method name
     * <br>Value: human-friendly description

     * <p>
     * <b>WARNING:</b> NOT synchronized; unsafe for concurrent modifications.

     */
    private Map<Object, String> consumers = new LinkedHashMap<>();
    
    /**
     * LinkedHashMap of consumer metadata Maps for detailed UI presentation.
     * <p>
     * Key: canonical method name
     * <br>Value: Map with keys 'c' (category), 'm' (method), 'p' (parameters), 'd' (description), 'v' (verbose)

     * <p>
     * <b>WARNING:</b> NOT synchronized; unsafe for concurrent modifications.

     */
    private Map<Object, Map<String, String>> consumersArray = new LinkedHashMap<>();

    /**
     * List of {@link AbstractApplicationEvent} subclasses for reflective event discovery.
     * Populated via {@link #registerEventClasses(Class[])}.
     */
    private List<Class> eventClasses = new ArrayList<>();

    /**
     * Cluster event publisher for propagating listener lifecycle across nodes in distributed deployments.
     */
    @Inject
    private ClusterEventSenderService clusterEventSenderService;



    /**
     * Returns map of events available to assign to listeners for UI dropdown rendering.
     * <p>
     * Key format: serialized event metadata (EventClassName, EventName, EventObjectType)
     * <br>Value format: display label for UI dropdown

     * <p>
     * <b>Note:</b> Immutable view recommended to prevent external modifications.

     *
     * @return Map of event descriptors for UI dropdown rendering
     */
    public Map<Object, String> getEvents() {
        return events;
    }

    /**
     * Returns map of consumers available to assign to listeners for UI dropdown rendering.
     * <p>
     * Key format: canonical method name (ConsumerClassName, ConsumerMethodName, ConsumerObjectType, parameter count)
     * <br>Value format: human-friendly description for UI dropdown

     * <p>
     * <b>Note:</b> Immutable view recommended to prevent external modifications.

     *
     * @return Map of consumer methods for UI dropdown rendering
     */
    public Map<Object, String> getConsumers() {
        return consumers;
    }


    /**
     * Registers array of {@link AbstractApplicationEvent} subclasses for reflective field discovery by
     * {@link #setAllAvailableAppEvents()}.
     * <p>
     * Typically used to register event descriptor classes (e.g., ApplicationEvent.class) containing
     * public static event field definitions.

     *
     * @param events array of AbstractApplicationEvent subclasses (typically ApplicationEvent.class)
     * @param <T> event type extending AbstractApplicationEvent
     * @return true if eventClasses list modified (new classes added), false otherwise
     */
    public <T> boolean registerEventClasses(Class<T>[] events) {
        debug("[registerEventClasses]");
        return eventClasses.addAll(java.util.Arrays.asList(events));
    }

    /**
     * Registers single {@link AbstractApplicationEvent} subclass for reflective event discovery.
     * <p>
     * Used to register individual event descriptor classes containing public static event field definitions.

     *
     * @param eventClass event descriptor class to register
     * @param <T> event type extending AbstractApplicationEvent
     * @return true if class added to eventClasses list, false if already present
     */
    public <T> boolean registerEventClass(Class<T> eventClass) {
        debug("[registerEventClass]");
        return eventClasses.add(eventClass);
    }

    /**
     * Loads all persisted {@link EventListenerEntry} entities from database and registers with
     * {@link ApplicationEventService}.
     * <p>
     * Typically invoked during application startup to restore event listener configurations.
     * Individual registration failures are logged but do not stop iteration.

     *
     * @return true indicating operation completed (individual failures logged but non-blocking)
     * @see #registerListener(EventListenerEntry)
     */
    public boolean registerAllEventListenersFromDb() {
        debug("[registerAllEventListenersFromDb]");
        repositories.unsecure.eventListener.findAll().forEach(this::registerListener);
        return true;
    }

    /**
     * Discovers all public static {@link AbstractApplicationEvent} fields via reflection and populates
     * events Map for UI dropdown rendering.
     * <p>
     * <b>Reflection mechanism:</b> Uses {@link ParameterizedType} to extract generic type argument
     * (event payload type) for display labeling.

     * <p>
     * Iterates registered eventClasses, extracts public fields, and builds composite keys containing
     * event class name, field name, and payload type.

     *
     * @return events Map containing discovered event descriptors with display labels
     */
    public Map<Object, String> setAllAvailableAppEvents() {
        debug("[setAllAvailableAppEvents]");
        for (Class<AbstractApplicationEvent> ec : eventClasses) {
            for (Field field : ec.getFields()) {
                String eventType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0].getTypeName();
                events.put(
                        StringUtils.join(new Object[] {
                                field.getType().getName(),
                                field.getName(),
                                eventType},
                                ","),
                        field.getName() + " (" + NameHelper.getClassName(eventType) + ")");
            }
        }
        return events;
    }

    /**
     * Iterates {@link ApplicationEventService} consumer registry and builds UI dropdown Maps with
     * canonical method names and descriptions.
     * <p>
     * Filters to consumers with non-null consumerMethod (reflective consumers); omits functional lambda consumers.

     * <p>
     * Builds two internal maps:
     * <ul>
     *   <li><b>consumers:</b> canonical name → description</li>
     *   <li><b>consumersArray:</b> canonical name → detailed metadata Map</li>
     * </ul>

     *
     * @return consumers Map containing discovered consumer methods
     */
    public Map<Object, String> setAllAvailableAppConsumers() {
        debug("[setAllAvailableAppConsumers]");
        Set<Map.Entry<Class, List<EventConsumer>>> consumersMap = services.applicationEvent.getConsumertEntrySet();
        if (consumersMap != null) {
            for (Map.Entry<Class, List<EventConsumer>> c : consumersMap) {
                for (EventConsumer ec : c.getValue()) {
                    if (ec.getConsumerMethod() != null) {
                        String canonicalName = Consumer.canonicalMethodName(
                                ec.getConsumerMethod().getDeclaringClass().getName(),
                                ec.getConsumerMethod().getName(),
                                ec.getConsumerMethod().getParameterTypes()[0].getName(),
                                ec.getConsumerMethod().getParameterTypes().length - 1);
                        consumers.put(canonicalName, ec.getDescription());
                        consumersArray.put(canonicalName, ec.propertiesToMap());
                    }
                }
            }
        }
        return consumers;
    }

    /**
     * Resolves {@link AbstractApplicationEvent} instance via reflection by class name and static field name.
     * <p>
     * Uses {@code Class.forName()} to load event descriptor class, then retrieves public static field
     * containing the event instance.

     *
     * @param className fully-qualified class name (e.g., 'com.openkoda.core.service.event.ApplicationEvent')
     * @param fieldName static field name (e.g., 'USER_CREATED')
     * @return AbstractApplicationEvent instance retrieved from static field
     * @throws ClassNotFoundException if className cannot be loaded
     * @throws NoSuchFieldException if fieldName not found in class
     * @throws IllegalAccessException if field access denied (unlikely for public static fields)
     */
    private AbstractApplicationEvent getEventByClassAndName(String className, String fieldName) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        debug("Getting event by class: {} and name: {}", className, fieldName);
        Field field = Class.forName(className).getField(fieldName);
        return (AbstractApplicationEvent) field.get(this);
    }


    /**
     * Searches {@link ApplicationEventService} consumer registry for {@link EventConsumer} matching
     * method signature and parameter count.
     * <p>
     * Iterates consumers registered for eventObjectClass and uses {@code verifyMethod()} to match
     * consumer class name, method name, event type, and static parameter count.

     *
     * @param eventObjectClass event payload Class to match
     * @param consumerClassName fully-qualified consumer class name
     * @param consumerMethodName consumer method name
     * @param numberOfParameters count of static String parameters (0-4)
     * @return matching EventConsumer, or null if no match found
     */
    private EventConsumer getConsumer(Class eventObjectClass, String consumerClassName, String consumerMethodName, int numberOfParameters) {
        debug("[getConsumer] event object class: {}, consumer class name: {}, consumer method name: {}, number of static method parameters: {}",
                eventObjectClass.getName(), consumerClassName, consumerMethodName, numberOfParameters);
        List<EventConsumer> eventConsumers = services.applicationEvent.findConsumersByEventType(eventObjectClass);
        for (EventConsumer ec : eventConsumers) {
            if (ec.verifyMethod(consumerClassName, consumerMethodName, eventObjectClass, numberOfParameters)) {
                return ec;
            }
        }
        return null;
    }

    /**
     * Registers event listener with cluster propagation in distributed mode; publishes EVENT_LISTENER_CREATED event.
     * <p>
     * <b>Cluster behavior:</b> In cluster mode, delegates to {@link ClusterEventSenderService} to propagate
     * registration across nodes. In single-instance mode, registers directly via {@link #registerListener(EventListenerEntry)}.

     * <p>
     * Publishes {@code EVENT_LISTENER_CREATED} event to notify application components of listener creation.

     *
     * @param eventListenerEntry persisted listener configuration to register
     * @return true if registration successful
     * @see ApplicationEvent#EVENT_LISTENER_CREATED
     * @see #registerListener(EventListenerEntry)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_EVENT_LISTENERS)
    public boolean registerListenerClusterAware(EventListenerEntry eventListenerEntry) {
        debug("[registerListenerClusterAware] {}", eventListenerEntry);
        boolean result;
        if (ClusterHelper.isCluster()) {
            result = clusterEventSenderService.loadEventListener(eventListenerEntry.getId());
        } else {
            result = registerListener(eventListenerEntry);
        }
        services.applicationEvent.emitEvent(EVENT_LISTENER_CREATED, eventListenerEntry);
        return result;
    }

    /**
     * Unregisters event listener with cluster propagation; publishes EVENT_LISTENER_DELETED event.
     * <p>
     * <b>Cluster behavior:</b> In cluster mode, delegates to {@link ClusterEventSenderService} to propagate
     * unregistration across nodes. In single-instance mode, unregisters directly via {@link #unregisterEventListener(Long)}.

     * <p>
     * Publishes {@code EVENT_LISTENER_DELETED} event to notify application components of listener deletion.

     *
     * @param eventListenerEntry persisted listener configuration to unregister
     * @return true if unregistration successful
     * @see ApplicationEvent#EVENT_LISTENER_DELETED
     * @see #unregisterEventListener(Long)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_EVENT_LISTENERS)
    public boolean unregisterEventListenerClusterAware(EventListenerEntry eventListenerEntry) {
        debug("[updateEventListenerClusterAware] {}", eventListenerEntry);
        boolean result;
        if (ClusterHelper.isCluster()) {
            result = clusterEventSenderService.removeEventListener(eventListenerEntry.getId());
        } else {
            result = unregisterEventListener(eventListenerEntry.getId());
        }
        services.applicationEvent.emitEvent(EVENT_LISTENER_DELETED, eventListenerEntry);
        return result;
    }


    /**
     * Updates event listener configuration with cluster propagation by reloading from database.
     * <p>
     * <b>Cluster behavior:</b> In cluster mode, publishes RELOAD event via {@link ClusterEventSenderService}.
     * In single-instance mode, calls {@link #updateEventListener(EventListenerEntry)} directly.

     *
     * @param eventListenerEntry updated listener configuration
     * @return true if update successful
     */
    @PreAuthorize(CHECK_CAN_MANAGE_EVENT_LISTENERS)
    public boolean updateEventListenerClusterAware(EventListenerEntry eventListenerEntry) {
        debug("[updateEventListenerClusterAware] {}", eventListenerEntry);
        boolean result;
        if (ClusterHelper.isCluster()) {
            result = clusterEventSenderService.reloadEventListener(eventListenerEntry.getId());
        } else {
            result = updateEventListener(eventListenerEntry);
        }
        return result;
    }

    /**
     * Updates listener by unregistering old configuration and registering new; publishes EVENT_LISTENER_MODIFIED event.
     * <p>
     * Atomic update operation: unregisters existing listener via {@link #unregisterEventListener(Long)},
     * then registers updated configuration via {@link #registerListener(EventListenerEntry)}.

     * <p>
     * Publishes {@code EVENT_LISTENER_MODIFIED} event to notify application components of configuration change.

     *
     * @param eventListenerEntry updated listener configuration
     * @return true if re-registration successful
     * @see #registerListener(EventListenerEntry)
     * @see #unregisterEventListener(Long)
     * @see ApplicationEvent#EVENT_LISTENER_MODIFIED
     */
    private boolean updateEventListener(EventListenerEntry eventListenerEntry) {
        debug("[updateEventListener] {}", eventListenerEntry);
        unregisterEventListener(eventListenerEntry.getId());
        boolean r = registerListener(eventListenerEntry);
        services.applicationEvent.emitEvent(EVENT_LISTENER_MODIFIED, eventListenerEntry);
        return r;
    }

    /**
     * Atomic operation unregistering listener and reloading from database; used by ClusterEventListenerService
     * for RELOAD events.
     * <p>
     * Removes existing listener registration via {@link #unregisterEventListener(Long)}, then reloads
     * fresh configuration from database via {@link #loadFromDb(Long)}.

     *
     * @param eventListenerEntryId database primary key of listener to reload
     * @return true if reload successful
     * @see #unregisterEventListener(Long)
     * @see #loadFromDb(Long)
     */
    public boolean removeAndLoadFromDb(Long eventListenerEntryId) {
        debug("[removeAndLoadFromDb] eventListenerEntryId: {}", eventListenerEntryId);
        unregisterEventListener(eventListenerEntryId);
        return loadFromDb(eventListenerEntryId);
    }

    /**
     * Loads {@link EventListenerEntry} from database by ID and registers with {@link ApplicationEventService};
     * used by cluster event handlers.
     * <p>
     * Fetches listener entity from database, then registers via {@link #registerListener(EventListenerEntry)}.

     *
     * @param eventListenerEntryId database primary key of listener to load
     * @return true if registration successful
     * @see #registerListener(EventListenerEntry)
     */
    public boolean loadFromDb(Long eventListenerEntryId) {
        debug("[loadFromDb] eventListenerEntryId: {}", eventListenerEntryId);
        EventListenerEntry eventListenerEntry = repositories.unsecure.eventListener.findOne(eventListenerEntryId);
        return registerListener(eventListenerEntry);
    }

    /**
     * Core registration logic: resolves event descriptor and consumer via reflection, registers with
     * {@link ApplicationEventService}.
     * <p>
     * <b>Reflection workflow:</b>
     * <ol>
     *   <li>Uses {@code Class.forName()} to load event object type</li>
     *   <li>Counts static data parameters via {@link #getNumberOfConsumerMethodParameters(EventListenerEntry)}</li>
     *   <li>Resolves consumer via {@link #getConsumer(Class, String, String, int)}</li>
     *   <li>Retrieves event descriptor via {@link #getEventByClassAndName(String, String)}</li>
     *   <li>Registers with ApplicationEventService including up to 4 static String parameters</li>
     * </ol>

     *
     * @param eventListenerEntry listener configuration with event/consumer metadata and static parameters
     * @return true if successfully registered
     * @throws RuntimeException if event/consumer resolution fails or registration throws exception
     */
    private boolean registerListener(EventListenerEntry eventListenerEntry) {
        debug("[registerListener] {}", eventListenerEntry);
        try {
            Class<?> eventObjectClass = Class.forName(eventListenerEntry.getEventObjectType());

            int numberOfConsumerMethodParameters = getNumberOfConsumerMethodParameters(eventListenerEntry);

            EventConsumer consumer = getConsumer(
                    eventObjectClass,
                    eventListenerEntry.getConsumerClassName(),
                    eventListenerEntry.getConsumerMethodName(),
                    numberOfConsumerMethodParameters
            );
            if (consumer != null) {
                info("Registering event listener {}", eventListenerEntry);
                services.applicationEvent.registerEventListener(
                        getEventByClassAndName(eventListenerEntry.getEventClassName(), eventListenerEntry.getEventName()),
                        consumer,
                        eventListenerEntry.getStaticData1(),
                        eventListenerEntry.getStaticData2(),
                        eventListenerEntry.getStaticData3(),
                        eventListenerEntry.getStaticData4(),
                        eventListenerEntry.getId());
                return true;
            } else {
                warn("Event Listener not registered {}", eventListenerEntry);
                throw new RuntimeException(formatMessage("Event Listener not registered {}", eventListenerEntry));
            }
        } catch (Exception e) {
            error(e, "Could not register event listener {}", eventListenerEntry);
            throw new RuntimeException(e);
        }
    }

    /**
     * Counts consecutive non-null static data fields for consumer method parameter matching.
     * <p>
     * Examines staticData1 through staticData4 fields and returns count of non-null values (0-4).

     *
     * @param listenerEntry listener configuration with staticData1-4 fields
     * @return count of non-null static data fields (0-4)
     */
    private int getNumberOfConsumerMethodParameters(EventListenerEntry listenerEntry) {
        int numberOfParameters = 0;

        if (listenerEntry.getStaticData1() != null) { numberOfParameters++; }
        if (listenerEntry.getStaticData2() != null) { numberOfParameters++; }
        if (listenerEntry.getStaticData3() != null) { numberOfParameters++; }
        if (listenerEntry.getStaticData4() != null) { numberOfParameters++; }
        return numberOfParameters;
    }


    /**
     * Delegates to {@link ApplicationEventService#unregisterEventListener(Long)} to remove listener from registry.
     * <p>
     * Exceptions during unregistration are caught, logged, and return false.

     *
     * @param eventListenerEntryId database primary key of listener to unregister
     * @return true if listener found and removed; false if not found or exception occurred
     */
    public boolean unregisterEventListener(Long eventListenerEntryId) {
        debug("[unregisterEventListener] eventListenerEntryId: {}", eventListenerEntryId);
        try {
            return services.applicationEvent.unregisterEventListener(eventListenerEntryId);
        } catch (Exception e) {
            error(e, "Could not register event listener {}", eventListenerEntryId);
        }
        return false;
    }

    /**
     * Returns consumer metadata Maps for detailed UI rendering.
     * <p>
     * Map structure: canonical method name → Map(category, method, parameters, description, verbose)

     * <p>
     * <b>Note:</b> Immutable view recommended to prevent external modifications.

     *
     * @return Map of consumer metadata Maps for detailed UI rendering
     */
    public Map<Object, Map<String, String>> getConsumersArray() {
        return consumersArray;
    }
}
