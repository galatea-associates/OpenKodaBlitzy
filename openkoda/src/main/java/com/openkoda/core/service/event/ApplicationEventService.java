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

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple6;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Central in-process event bus managing synchronous and asynchronous event publishing with type-safe listener registration.
 * <p>
 * This service maintains a registry of event descriptors mapped to listener tuples and provides mechanisms for both
 * synchronous and asynchronous event dispatch. It supports functional (Consumer, BiConsumer) and reflective (Method-based)
 * event consumers with up to 4 static String parameters for parameterized event handling.
 * </p>
 * 
 * <p><b>Architecture:</b></p>
 * <ul>
 *   <li>Maintains public {@code Map<AbstractApplicationEvent, ListenerTupleList>} registry of event-to-listeners mappings</li>
 *   <li>Maintains {@code LinkedHashMap<Class, List<EventConsumer>>} for type-based consumer lookup</li>
 *   <li>Fixed thread pool (4 threads) for asynchronous event dispatch created eagerly at class load</li>
 *   <li>Works with EventListenerService for persisted listener registration and EventConsumer wrapper abstraction</li>
 * </ul>
 * 
 * <p><b>Execution Models:</b></p>
 * <ol>
 *   <li><b>Synchronous:</b> {@link #emitEvent(AbstractApplicationEvent, Object)} invokes listeners on caller thread; exceptions propagate to caller</li>
 *   <li><b>Asynchronous:</b> {@link #emitEventAsync(AbstractApplicationEvent, Object)} offloads to fixed thread pool; exceptions swallowed by executor</li>
 * </ol>
 * 
 * <p><b>Thread-Safety Considerations:</b></p>
 * <ul>
 *   <li>Registration methods ({@code registerEventListener}) are synchronized to prevent concurrent modification</li>
 *   <li>Event dispatch ({@code emitEvent}) is NOT synchronized; listeners may receive concurrent events from async executor</li>
 *   <li>Public {@code listeners} Map is mutable; external modifications bypass synchronization and may cause race conditions</li>
 *   <li>Listener implementations must be thread-safe for async event dispatch</li>
 * </ul>
 * 
 * <p><b>Error Propagation:</b></p>
 * <ul>
 *   <li>Synchronous: First listener exception propagates to caller, halting remaining listeners</li>
 *   <li>Asynchronous: Listener exceptions swallowed by executor; no error feedback to caller</li>
 *   <li>No automatic retry mechanism; consumers must implement own error recovery</li>
 * </ul>
 * 
 * <p><b>WARNING:</b> The {@code asyncEventsExecutor} thread pool is never shutdown during lifecycle, 
 * which may cause resource leaks. The public {@code listeners} Map should not be modified externally 
 * as it bypasses synchronization.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Synchronous event publish
 * applicationEventService.emitEvent(ApplicationEvent.USER_CREATED, basicUser);
 * 
 * // Asynchronous event publish
 * applicationEventService.emitEventAsync(ApplicationEvent.BACKUP_CREATED, backupFile);
 * 
 * // Lambda listener registration
 * applicationEventService.registerEventListener(
 *     ApplicationEvent.USER_LOGGED_IN, 
 *     user -> auditLog(user)
 * );
 * }</pre>
 * 
 * @author OpenKoda Team
 * @since 1.7.1
 * @see AbstractApplicationEvent
 * @see ApplicationEvent
 * @see EventConsumer
 * @see EventListenerService
 */
@Service("applicationEventService")
    public class ApplicationEventService implements LoggingComponentWithRequestId {

    /**
     * Type alias for ArrayList storing Tuple6 listener registrations (EventConsumer, staticData1-4, eventListenerId) 
     * for type-safe collection operations.
     * <p>
     * Each tuple contains:
     * <ul>
     *   <li>T1: EventConsumer wrapper abstracting functional or reflective consumer</li>
     *   <li>T2-T5: Up to 4 static String parameters for parameterized event handling</li>
     *   <li>T6: Database EventListenerEntry.id for persisted listeners (null for programmatic listeners)</li>
     * </ul>
     * </p>
     */
    public class ListenerTupleList extends ArrayList<Tuple6<EventConsumer, String, String, String, String, Long>>{}

    /**
     * Singleton empty ListenerTupleList returned by {@link #emitEvent(AbstractApplicationEvent, Object)} when 
     * no listeners registered for event; avoids null checks in event dispatch logic.
     */
    private final ListenerTupleList empty = new ListenerTupleList();

    /**
     * Static self-reference set by {@link #setThisService()} @PostConstruct for static accessor 
     * {@link #getApplicationEventService()}; enables legacy code access without dependency injection.
     */
    private static ApplicationEventService thisService;

    /**
     * Fixed thread pool (4 threads) for asynchronous event dispatch via {@link #emitEventAsync(AbstractApplicationEvent, Object)}.
     * <p>
     * <b>WARNING:</b> Never shutdown during lifecycle, may leak resources; shared across all async event publications.
     * Created eagerly at class load time.
     * </p>
     */
    private final static ExecutorService asyncEventsExecutor = Executors.newFixedThreadPool(4);

    /**
     * PUBLIC mutable registry mapping event descriptors to listener tuples; synchronized access required for modifications.
     * <p>
     * Used by {@link #emitEvent(AbstractApplicationEvent, Object)} for listener dispatch. External modifications 
     * bypass synchronization and may cause race conditions.
     * </p>
     * <p>
     * Key: AbstractApplicationEvent descriptor<br>
     * Value: ListenerTupleList containing registered listeners with static parameters and entry IDs
     * </p>
     */
    public Map<AbstractApplicationEvent,
            ListenerTupleList> listeners = new HashMap<>();

    /**
     * LinkedHashMap maintaining insertion order of Class-to-EventConsumer mappings for type-based consumer lookup.
     * <p>
     * Used by {@link EventListenerService#findConsumersByEventType(Class)} to discover consumers registered for 
     * event payload types. Supports inheritance-based consumer matching.
     * </p>
     */
    private LinkedHashMap<Class, List<EventConsumer>> consumers = new LinkedHashMap<>();

    /**
     * Registers event consumer with up to 4 static String parameters for parameterized event handling.
     * <p>
     * This method is synchronized to prevent concurrent modification of the listeners Map during registration.
     * Static parameters are passed to the consumer during event dispatch and can be used for configuration
     * or context-specific event handling.
     * </p>
     * 
     * @param <T> Event payload type
     * @param event Event descriptor to associate consumer with
     * @param eventConsumer EventConsumer wrapper abstracting functional or reflective consumer
     * @param staticData1 First static configuration parameter passed to consumer (null if not used)
     * @param staticData2 Second static configuration parameter (null if not used)
     * @param staticData3 Third static configuration parameter (null if not used)
     * @param staticData4 Fourth static configuration parameter (null if not used)
     * @param eventListenerId Database EventListenerEntry.id for persisted listeners; enables unregistration; null for programmatic listeners
     * @return true if listener successfully added to registry
     */
    synchronized public <T> boolean registerEventListener(AbstractApplicationEvent<T> event, EventConsumer<T> eventConsumer, String staticData1, String staticData2, String staticData3, String staticData4, Long eventListenerId) {
        debug("[registerEventListener] event: {} eventConsumer: {} eventListenerId: {}", event, eventConsumer, eventListenerId);
        ListenerTupleList eventListeners = getEventListener(event);
        return eventListeners.add(Tuples.of(eventConsumer, staticData1, staticData2, staticData3, staticData4, eventListenerId));
    }

    /**
     * Registers functional Consumer&lt;T&gt; for lambda/method reference event handling without static parameters.
     * <p>
     * This method is synchronized to prevent concurrent modification of the listeners Map. Suitable for simple 
     * event handlers that don't require configuration parameters.
     * </p>
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * registerEventListener(USER_CREATED, user -> log.info("User: {}", user));
     * }</pre>
     * 
     * @param <T> Event payload type
     * @param event Event descriptor to associate consumer with
     * @param eventListener Functional Consumer&lt;T&gt; lambda or method reference
     * @return true if listener successfully added to registry
     */
    synchronized public <T> boolean registerEventListener(AbstractApplicationEvent<T> event, Consumer<T>
            eventListener) {
        debug("[registerEventListener] event: {} eventListener: {}", event, eventListener);
        ListenerTupleList eventListeners = getEventListener(event);
        return eventListeners.add(Tuples.of(new EventConsumer(eventListener), null, null, null, null, null));
    }

    /**
     * Registers functional BiConsumer&lt;T,String[]&gt; for parameterized event handling with up to 4 static parameters.
     * <p>
     * This method is synchronized to prevent concurrent modification of the listeners Map. The BiConsumer 
     * receives both the event payload and an array of static configuration parameters during event dispatch.
     * </p>
     * 
     * @param <T> Event payload type
     * @param event Event descriptor to associate consumer with
     * @param eventListener Functional BiConsumer accepting event and static configuration
     * @param staticData1 First static configuration parameter passed to consumer (null if not used)
     * @param staticData2 Second static configuration parameter (null if not used)
     * @param staticData3 Third static configuration parameter (null if not used)
     * @param staticData4 Fourth static configuration parameter (null if not used)
     * @return true if listener successfully added to registry
     */
    synchronized public <T> boolean registerEventListener(AbstractApplicationEvent<T> event, BiConsumer<T, String>
            eventListener, String staticData1, String staticData2, String staticData3, String staticData4) {
        debug("[registerEventListener] event: {}", event);
        ListenerTupleList eventListeners = getEventListener(event);
        return eventListeners.add(Tuples.of(new EventConsumer(eventListener), staticData1, staticData2, staticData3, staticData4, null));
    }


    /**
     * Retrieves or creates ListenerTupleList for the specified event descriptor.
     * <p>
     * Ensures every event has an associated list even before first listener registration, simplifying 
     * event dispatch logic. This method is NOT thread-safe for concurrent modifications; callers should 
     * use synchronized registration methods.
     * </p>
     * 
     * @param <T> Event payload type
     * @param event Event descriptor to retrieve listeners for
     * @return ListenerTupleList containing all registered listeners; creates new empty list if none registered
     */
    private <T> ListenerTupleList getEventListener(AbstractApplicationEvent<T> event) {
        debug("[getEventListener] event: {}", event);
        ListenerTupleList eventListeners = listeners.get(event);
        if (eventListeners == null) {
            eventListeners = new ListenerTupleList();
            listeners.put(event, eventListeners);
        }
        return eventListeners;
    }

    /**
     * Registers EventConsumer for type-based lookup by EventListenerService; supports inheritance 
     * (parent class consumers handle subclass events).
     * <p>
     * This method is synchronized to prevent concurrent modification of the consumers Map. Used by 
     * EventListenerService to register consumers discovered via reflection or persisted EventListenerEntry.
     * </p>
     * 
     * @param <T> Event payload type
     * @param eventClass Event payload Class for type-based consumer matching
     * @param eventConsumer EventConsumer wrapper for functional or reflective consumer
     * @return true if consumer successfully added to class-to-consumer map
     */
    synchronized public <T> boolean registerEventConsumer(Class<T> eventClass, EventConsumer<T> eventConsumer) {
        debug("[registerEventListener] eventConsumer: {}", eventConsumer);
        List<EventConsumer> eventConsumers = consumers.get(eventClass);
        if (eventConsumers == null) {
            eventConsumers = new ArrayList<>();
            consumers.put(eventClass, eventConsumers);
        }
        return eventConsumers.add(eventConsumer);
    }

    /**
     * Registers reflective method consumer by resolving method signature and creating EventConsumer wrapper.
     * <p>
     * This method is synchronized to prevent concurrent modification of the consumers Map. It resolves 
     * the consumer method via reflection using the event class plus static parameter classes, then 
     * creates an EventConsumer wrapper for the reflective method invocation.
     * </p>
     * <p>
     * Logs info on success, error on failure (NoSuchMethodException). Returns false if method signature 
     * cannot be resolved, allowing graceful degradation during consumer discovery.
     * </p>
     * 
     * @param <T> Event payload type
     * @param eventClass Event payload Class expected as first method parameter
     * @param eventConsumerClass Spring bean class declaring consumer method
     * @param eventConsumerMethodName Method name to invoke reflectively
     * @param description Human-friendly consumer description for UI/logging
     * @param category EventConsumerCategory for domain-based filtering
     * @param methodStaticParamsClass Variable-length Class array for static parameter types (0-4 String.class entries)
     * @return true if method resolved and consumer registered; false if NoSuchMethodException
     * @throws NoSuchMethodException If method signature not found (caught internally and logged)
     */
    synchronized public <T> boolean registerEventConsumerWithMethod(Class<T> eventClass,
                                                                    Class eventConsumerClass,
                                                                    String eventConsumerMethodName,
                                                                    String description,
                                                                    EventConsumerCategory category,
                                                                    Class... methodStaticParamsClass) {
        debug("[registerEventConsumerWithMethod] methodName: {} description: {}", eventConsumerMethodName, description);
        try {
            EventConsumer<T> eventConsumer;

            if (methodStaticParamsClass.length == 0) {
                eventConsumer = new EventConsumer<>(eventConsumerClass.getMethod(eventConsumerMethodName, eventClass), eventClass, 0, description, category);
            } else {
                Class[] eventClassInArray = {eventClass};
                Class[] allMethodParamsClass = ArrayUtils.addAll(eventClassInArray, methodStaticParamsClass);
                eventConsumer = new EventConsumer<>(eventConsumerClass.getMethod(eventConsumerMethodName, allMethodParamsClass), eventClass, methodStaticParamsClass.length, description, category);
            }

            info("Registering Event Consumer {} with method {} for event type {}. {}", eventConsumerClass, eventConsumerMethodName, eventClass, description);
            return registerEventConsumer(eventClass, eventConsumer);
        } catch (NoSuchMethodException e) {
            error(e, "Could not find event consumer method [{}, {}]. Consumer not registered.", eventConsumerClass.getName(), eventConsumerMethodName);
        }
        return false;
    }

    /**
     * Removes persisted event listener by database EventListenerEntry.id; scans all event listener lists 
     * to find matching tuple.
     * <p>
     * This method is synchronized to prevent concurrent modification of the listeners Map during unregistration.
     * Performs O(n*m) scan across all events and listeners; suitable for infrequent unregistration operations.
     * </p>
     * 
     * @param <T> Event payload type
     * @param eventListenerEntryId Database primary key of EventListenerEntry to unregister
     * @return true if listener found and removed; false if no matching listener
     */
    synchronized public <T> boolean unregisterEventListener(Long eventListenerEntryId) {
        debug("[unregisterEventListener] eventListenerEntryId: {}", eventListenerEntryId);
        Optional<Tuple6<EventConsumer, String, String, String, String, Long>> listenerTuple = null;
        for (Map.Entry<AbstractApplicationEvent, ListenerTupleList> l : listeners.entrySet()) {
            ListenerTupleList t = l.getValue();
            listenerTuple = t.stream()
                    .filter(tuple -> tuple.getT6() != null && tuple.getT6().equals(eventListenerEntryId))
                    .findFirst();
            if (listenerTuple.isPresent()) {
                return t.remove(listenerTuple.get());
            }
        }
        debug("[unregisterEventListener] no eventListener with entryId {} found", eventListenerEntryId);
        return false;
    }

    /**
     * Submits event for asynchronous dispatch on fixed thread pool; returns immediately without waiting for listeners.
     * <p>
     * Listener exceptions are swallowed by the executor; no error feedback to caller. Suitable for 
     * fire-and-forget event publication where caller doesn't need to know listener execution results.
     * </p>
     * 
     * @param <T> Event payload type
     * @param event Event descriptor to publish
     * @param object Event payload to pass to listeners
     * @return true indicating submission success (not listener execution success)
     */
    public <T> boolean emitEventAsync(AbstractApplicationEvent<T> event, T object) {
        asyncEventsExecutor.submit(() -> emitEvent(event, object));
        return true;
    }

    /**
     * Dispatches event synchronously to all registered listeners on caller thread; propagates listener exceptions.
     * <p>
     * Iterates listeners and invokes accept() with event plus 0-4 static parameters based on tuple configuration.
     * First listener exception halts dispatch and propagates to caller; remaining listeners are not invoked.
     * </p>
     * 
     * @param <T> Event payload type
     * @param event Event descriptor to publish
     * @param object Event payload to pass to listeners
     * @return true indicating successful dispatch (not individual listener success)
     */
    public <T> boolean emitEvent(AbstractApplicationEvent<T> event, T object) {
        debug("[emitEvent] event: {}", event);
        listeners.getOrDefault(event, empty).forEach(
                a -> {
                    if (a.getT2() == null) {
                        a.getT1().accept(object, null);
                    } else if (a.getT3() == null) {
                        a.getT1().accept(object, a.getT2());
                    } else if (a.getT4() == null) {
                        a.getT1().accept(object, a.getT2(), a.getT3());
                    } else if (a.getT5() == null) {
                        a.getT1().accept(object, a.getT2(), a.getT3(), a.getT4());
                    } else {
                        a.getT1().accept(object, a.getT2(), a.getT3(), a.getT4(), a.getT5());
                    }
                });
        return true;
    }

    /**
     * Returns entry set of consumers Map for iteration over Class-to-EventConsumer mappings.
     * <p>
     * Exposes internal consumers Map for EventListenerService integration; modifications affect live registry.
     * Method name contains typo (Consumer<b>t</b>) but preserved for backward compatibility.
     * </p>
     * 
     * @return Entry set of consumers Map for iteration over Class-to-EventConsumer mappings
     */
    Set<Map.Entry<Class, List<EventConsumer>>> getConsumertEntrySet() {
        return consumers.entrySet();
    }

    /**
     * Finds all EventConsumers registered for event class or parent classes using assignability check.
     * <p>
     * Supports inheritance: consumers registered for User.class will match BasicUser.class events.
     * This enables polymorphic event handling where parent class consumers receive subclass events.
     * </p>
     * 
     * @param c Event payload Class to match against registered consumer classes
     * @return List of matching EventConsumers supporting polymorphic event handling
     */
    List<EventConsumer> findConsumersByEventType(Class c) {
        debug("[findConsumersByEventType] type: {}", c);
        List<EventConsumer> result = new ArrayList<>();
        for (Map.Entry<Class, List<EventConsumer>> e : consumers.entrySet()) {
            if (e.getKey().isAssignableFrom(c)) {
                result.addAll(e.getValue());
            }
        }
        return result;
    }

    /**
     * @PostConstruct lifecycle method setting static thisService reference for legacy static accessor.
     * <p>
     * Method executed automatically after the bean has been constructed by the Spring framework.
     * Idempotent: only sets thisService once even if called multiple times; enables 
     * {@link #getApplicationEventService()} static access for components without dependency injection.
     * </p>
     */
    @PostConstruct
    void setThisService() {
        if (thisService == null) {
            thisService = this;
        }
    }

    /**
     * Returns static singleton reference to Spring-managed ApplicationEventService instance.
     * <p>
     * Legacy static accessor for components without dependency injection; prefer @Autowired injection in new code.
     * </p>
     * 
     * @return Static singleton reference to Spring-managed ApplicationEventService instance
     */
    public static ApplicationEventService getApplicationEventService() {
        return thisService;
    }

}
