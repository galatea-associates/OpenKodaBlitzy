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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract base class for typed application event descriptors providing event registry and metadata management.
 * <p>
 * This class maintains a static registry (HashMap) of all event descriptors keyed by eventName for event lookup
 * and discovery across the application. Subclasses (such as ApplicationEvent) declare canonical application-wide
 * event types (USER_CREATED, ORGANIZATION_UPDATED, etc.) as static final instances.

 * <p>
 * Event descriptors are effectively immutable after construction, with all fields being final. The static registry
 * is populated during class initialization and should not be modified at runtime.

 * <p>
 * <b>Thread-Safety Warning:</b> The static eventList HashMap is NOT synchronized. Registration occurs during
 * class initialization and should not be modified concurrently at runtime.

 * <p>
 * Example usage:
 * <pre>{@code
 * // Event lookup
 * AbstractApplicationEvent event = AbstractApplicationEvent.getEvent("user.created");
 * 
 * // Type-safe usage in ApplicationEvent subclass
 * public static final ApplicationEvent<User> USER_CREATED = 
 *     new ApplicationEvent<>(User.class, "user.created");
 * }</pre>

 *
 * @param <T> Payload type that event consumers will receive when this event is published (e.g., User, Organization, SchedulerDto)
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @since 1.7.1
 * @see ApplicationEvent for concrete event type definitions
 * @see ApplicationEventService for event publishing and listener registration
 */
public class AbstractApplicationEvent<T> {

    /**
     * Runtime Class object representing the payload type T for reflection-based consumer verification and type safety.
     */
    private final Class<T> eventClass;
    
    /**
     * Unique canonical event identifier used for registry lookup and configuration persistence.
     */
    private final String eventName;
    
    /**
     * Static global registry mapping event names to event descriptor instances.
     * <p>
     * Populated during static initialization. NOT thread-safe for concurrent modifications.

     */
    private final static Map<String, AbstractApplicationEvent> eventList = new HashMap<>();


    /**
     * Protected constructor invoked only by ApplicationEvent static initializers to create event descriptors.
     * <p>
     * Registers this event descriptor in the static eventList HashMap as a side effect. The constructor is NOT
     * thread-safe and should only be called during class initialization to avoid concurrent modification issues.

     *
     * @param eventClass Runtime Class representing payload type for type-safe event handling
     * @param eventName Unique canonical event name used as registry key and configuration identifier; must not collide with existing events
     */
    protected AbstractApplicationEvent(Class<T> eventClass, String eventName) {
        this.eventClass = eventClass;
        this.eventName = eventName;
        eventList.put(eventName, this);
    }

    /**
     * Compares this event descriptor with another object for equality based on eventClass and eventName.
     * <p>
     * Two objects of this class are considered equal if they have the same eventClass and eventName fields.

     *
     * @param o Object to compare for equality based on eventClass and eventName
     * @return true if both objects have equal eventClass and eventName fields, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractApplicationEvent<?> that = (AbstractApplicationEvent<?>) o;
        return Objects.equals(eventClass, that.eventClass) &&
                Objects.equals(eventName, that.eventName);
    }

    /**
     * Generates the hash code based on the eventClass and eventName fields.
     * <p>
     * By implementing the hashCode() method in this way, the hash code for an object of this class
     * will be based on the values of its eventClass and eventName fields.
     * This ensures that two objects that are equal according to their equals() method will also have the same hash code.
     * This is important for correctness when using hash-based data structures such as HashMap and HashSet.

     *
     * @return Hash code computed from eventClass and eventName for use in hash-based collections
     */
    @Override
    public int hashCode() {
        return Objects.hash(eventClass, eventName);
    }

    /**
     * Retrieves an event descriptor from the static registry by its unique event name.
     * <p>
     * Returns null for unregistered event names; callers should check for null before using the returned value.

     *
     * @param eventName Unique event identifier to look up in registry
     * @return AbstractApplicationEvent descriptor matching eventName, or null if no matching event registered
     */
    public static AbstractApplicationEvent getEvent(String eventName){
        return eventList.get(eventName);
    }
}
