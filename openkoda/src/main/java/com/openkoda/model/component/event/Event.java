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

package com.openkoda.model.component.event;

import org.apache.commons.lang3.StringUtils;

/**
 * Lightweight POJO representing a 3-part event descriptor for mapping between EventListenerForm and EventListenerEntry.
 * <p>
 * This class encapsulates the event configuration as a triplet of (eventClassName, eventName, eventObjectType).
 * It parses comma-separated event descriptor strings and provides methods to serialize back to that format.
 * The event descriptor is used for registering and managing event listeners within the OpenKoda framework.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * Event event = new Event("com.openkoda.service.UserService,userCreated,User");
 * String className = event.getEventClassName(); // "com.openkoda.service.UserService"
 * }</pre>
 * </p>
 * <p>
 * <b>Thread-safety:</b> This is a mutable POJO and is not thread-safe. External synchronization is required
 * if instances are shared across threads.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see EventListenerEntry
 * @see com.openkoda.form.EventListenerForm
 */
public class Event {

    /**
     * Fully qualified class name of the event emitter.
     */
    private String eventClassName;
    
    /**
     * Event identifier/name used for listener registration.
     */
    private String eventName;
    
    /**
     * Type of the event object passed to listeners.
     */
    private String eventObjectType;

    /**
     * Parses a comma-separated event descriptor string into its three components.
     * <p>
     * The input string is split by comma without trimming whitespace or CSV-escaping.
     * The three tokens are assigned by index to eventClassName, eventName, and eventObjectType respectively.
     * No validation is performed on the input format.
     * </p>
     *
     * @param eventString comma-separated triple in format: {@code eventClassName,eventName,eventObjectType}
     * @throws NullPointerException if {@code eventString} is null
     * @throws ArrayIndexOutOfBoundsException if {@code eventString} contains fewer than 3 comma-separated tokens
     */
    public Event(String eventString) {
        String[] eventSplit = eventString.split(",");
        this.eventClassName = eventSplit[0];
        this.eventName = eventSplit[1];
        this.eventObjectType = eventSplit[2];
    }

    /**
     * Returns the fully qualified class name of the event emitter.
     *
     * @return the event class name
     */
    public String getEventClassName() {
        return eventClassName;
    }

    /**
     * Sets the fully qualified class name of the event emitter.
     *
     * @param eventClassName the event class name to set
     */
    public void setEventClassName(String eventClassName) {
        this.eventClassName = eventClassName;
    }

    /**
     * Returns the event identifier/name used for listener registration.
     *
     * @return the event name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Sets the event identifier/name used for listener registration.
     *
     * @param eventName the event name to set
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Returns the type of the event object passed to listeners.
     *
     * @return the event object type
     */
    public String getEventObjectType() {
        return eventObjectType;
    }

    /**
     * Sets the type of the event object passed to listeners.
     *
     * @param eventObjectType the event object type to set
     */
    public void setEventObjectType(String eventObjectType) {
        this.eventObjectType = eventObjectType;
    }

    /**
     * Returns a debug representation of this event descriptor in bracketed format.
     * <p>
     * The format is: {@code Event[eventClassName,eventName,eventObjectType]}
     * </p>
     *
     * @return debug string representation of this event
     */
    @Override
    public String toString() {
        return "Event[" + eventClassName + ',' + eventName + ',' + eventObjectType + ']';
    }

    /**
     * Serializes this event descriptor back to comma-separated format.
     * <p>
     * Uses {@link StringUtils#join(Object[], String)} to join the three components with commas.
     * The resulting format matches the input format expected by the constructor.
     * </p>
     *
     * @return comma-separated event descriptor string in format: {@code eventClassName,eventName,eventObjectType}
     * @see #Event(String)
     */
    public String getEventString(){
        return StringUtils.join(new String[] {eventClassName, eventName, eventObjectType}, ",");
    }

}
