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

package com.openkoda.dto.system;

import com.openkoda.dto.CanonicalObject;
import com.openkoda.dto.OrganizationRelatedObject;
import com.openkoda.model.component.event.Consumer;
import com.openkoda.model.component.event.Event;

/**
 * Data Transfer Object representing event listener wiring configuration for routing events to consumers in a multi-tenant environment.
 * <p>
 * This DTO captures the mapping between event types and their designated consumers, including static configuration
 * parameters and tenant-scoped authorization. It is used by event routing systems to determine which consumer should
 * handle a specific event within an organization's context.
 * </p>
 * <p>
 * The class implements {@link CanonicalObject} to provide notification messages for logging and audit trails,
 * and {@link OrganizationRelatedObject} for multi-tenant authorization and scoping.
 * </p>
 * <p>
 * <b>Design Notes:</b>
 * <ul>
 * <li>This is a mutable DTO without validation, equals/hashCode implementation, or thread-safety guarantees</li>
 * <li>Fields {@code eventObj} and {@code consumerObj} violate Rule 5.1 by embedding domain model types instead of DTOs,
 *     creating tight coupling between DTO and persistence layers</li>
 * <li>The {@code notificationMessage()} method may throw NPE if fields are null</li>
 * <li>Four static data slots (staticData1-4) provide extensibility for consumer-specific configuration</li>
 * </ul>
 * </p>
 * <p>
 * Usage by event routing systems, mappers, and persistence layers. Compatible with Jackson/Gson serialization
 * via public fields and JavaBean accessors.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CanonicalObject
 * @see OrganizationRelatedObject
 * @see Event
 * @see Consumer
 */
public class EventListenerDto implements CanonicalObject, OrganizationRelatedObject {

    /**
     * Event type identifier used for matching and routing events to appropriate consumers.
     * <p>
     * This string-based identifier should match event names registered in the system's event catalog.
     * May be null if not yet initialized.
     * </p>
     */
    public String event;
    
    /**
     * Consumer identifier specifying which consumer handler should process the event.
     * <p>
     * This identifier references a registered consumer in the system's consumer registry.
     * May be null if not yet initialized.
     * </p>
     */
    public String consumer;
    
    /**
     * Category for grouping related consumers to facilitate organization and filtering.
     * <p>
     * Used for consumer classification and administrative purposes.
     * May be null if not yet initialized.
     * </p>
     */
    public String consumerCategory;
    
    /**
     * First static configuration parameter passed to the consumer during event processing.
     * <p>
     * Provides extensibility for consumer-specific configuration without modifying the DTO structure.
     * May be null if not required by the consumer.
     * </p>
     */
    public String staticData1;
    
    /**
     * Second static configuration parameter passed to the consumer during event processing.
     * <p>
     * Provides additional extensibility for consumer-specific configuration.
     * May be null if not required by the consumer.
     * </p>
     */
    public String staticData2;
    
    /**
     * Third static configuration parameter passed to the consumer during event processing.
     * <p>
     * Provides additional extensibility for consumer-specific configuration.
     * May be null if not required by the consumer.
     * </p>
     */
    public String staticData3;
    
    /**
     * Fourth static configuration parameter passed to the consumer during event processing.
     * <p>
     * Provides additional extensibility for consumer-specific configuration.
     * Four slots ensure sufficient configuration capacity for most consumers.
     * May be null if not required by the consumer.
     * </p>
     */
    public String staticData4;
    
    /**
     * Tenant identifier for multi-tenant authorization and scoping.
     * <p>
     * Links this event listener configuration to a specific organization, ensuring tenant isolation
     * and enabling organization-scoped event routing. Required by {@link OrganizationRelatedObject}.
     * May be null for global event listeners not scoped to a specific organization.
     * </p>
     */
    public Long organizationId;
    
    /**
     * Domain model reference to the Event entity.
     * <p>
     * <b>WARNING:</b> This field violates Rule 5.1 - DTOs should not embed domain types as it creates
     * tight coupling between the DTO and persistence layers, violating separation of concerns.
     * Consider replacing with an event identifier or dedicated event DTO.
     * </p>
     * <p>
     * May be null if only the string identifier is available.
     * </p>
     * 
     * @see Event
     */
    //TODO Rule 5.1 All fields in a DTO must be either a simple field (String, numbers, boolean, enum) or other DTO or collection of these
    public Event eventObj;
    
    /**
     * Domain model reference to the Consumer entity.
     * <p>
     * <b>WARNING:</b> This field violates Rule 5.1 - DTOs should not embed domain types as it creates
     * tight coupling between the DTO and persistence layers, violating separation of concerns.
     * Consider replacing with a consumer identifier or dedicated consumer DTO.
     * </p>
     * <p>
     * May be null if only the string identifier is available.
     * </p>
     * 
     * @see Consumer
     */
    //TODO Rule 5.1 All fields in a DTO must be either a simple field (String, numbers, boolean, enum) or other DTO or collection of these
    public Consumer consumerObj;

    /**
     * Returns the event type identifier.
     *
     * @return event type identifier for matching and routing, may be null
     */
    public String getEvent() {
        return event;
    }

    /**
     * Sets the event type identifier.
     *
     * @param event event type identifier for matching and routing, may be null
     */
    public void setEvent(String event) {
        this.event = event;
    }

    /**
     * Returns the consumer identifier.
     *
     * @return consumer identifier specifying the handler for the event, may be null
     */
    public String getConsumer() {
        return consumer;
    }

    /**
     * Sets the consumer identifier.
     *
     * @param consumer consumer identifier specifying the handler for the event, may be null
     */
    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    /**
     * Returns the consumer category.
     *
     * @return category for grouping related consumers, may be null
     */
    public String getConsumerCategory() {
        return consumerCategory;
    }

    /**
     * Sets the consumer category.
     *
     * @param consumerCategory category for grouping related consumers, may be null
     */
    public void setConsumerCategory(String consumerCategory) {
        this.consumerCategory = consumerCategory;
    }

    /**
     * Returns the first static configuration parameter.
     *
     * @return first static configuration parameter passed to consumer, may be null
     */
    public String getStaticData1() {
        return staticData1;
    }

    /**
     * Sets the first static configuration parameter.
     *
     * @param staticData1 first static configuration parameter passed to consumer, may be null
     */
    public void setStaticData1(String staticData1) {
        this.staticData1 = staticData1;
    }

    /**
     * Returns the second static configuration parameter.
     *
     * @return second static configuration parameter passed to consumer, may be null
     */
    public String getStaticData2() {
        return staticData2;
    }

    /**
     * Sets the second static configuration parameter.
     *
     * @param staticData2 second static configuration parameter passed to consumer, may be null
     */
    public void setStaticData2(String staticData2) {
        this.staticData2 = staticData2;
    }

    /**
     * Returns the third static configuration parameter.
     *
     * @return third static configuration parameter passed to consumer, may be null
     */
    public String getStaticData3() {
        return staticData3;
    }

    /**
     * Sets the third static configuration parameter.
     *
     * @param staticData3 third static configuration parameter passed to consumer, may be null
     */
    public void setStaticData3(String staticData3) {
        this.staticData3 = staticData3;
    }

    /**
     * Returns the fourth static configuration parameter.
     *
     * @return fourth static configuration parameter passed to consumer, may be null
     */
    public String getStaticData4() {
        return staticData4;
    }

    /**
     * Sets the fourth static configuration parameter.
     *
     * @param staticData4 fourth static configuration parameter passed to consumer, may be null
     */
    public void setStaticData4(String staticData4) {
        this.staticData4 = staticData4;
    }

    /**
     * Returns the tenant identifier for multi-tenant authorization.
     *
     * @return organization identifier for tenant-scoped event routing, may be null for global listeners
     */
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the tenant identifier for multi-tenant authorization.
     *
     * @param organizationId organization identifier for tenant-scoped event routing, may be null for global listeners
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Returns the Event domain model entity reference.
     *
     * @return Event entity reference, may be null if only string identifier is available
     */
    public Event getEventObj() {
        return eventObj;
    }

    /**
     * Sets the Event domain model entity reference.
     *
     * @param eventObj Event entity reference, may be null if only string identifier is available
     */
    public void setEventObj(Event eventObj) {
        this.eventObj = eventObj;
    }

    /**
     * Returns the Consumer domain model entity reference.
     *
     * @return Consumer entity reference, may be null if only string identifier is available
     */
    public Consumer getConsumerObj() {
        return consumerObj;
    }

    /**
     * Sets the Consumer domain model entity reference.
     *
     * @param consumerObj Consumer entity reference, may be null if only string identifier is available
     */
    public void setConsumerObj(Consumer consumerObj) {
        this.consumerObj = consumerObj;
    }

    /**
     * Returns a formatted notification message describing this event listener configuration.
     * <p>
     * Generates a human-readable template string suitable for logging and audit trails,
     * showing the event-to-consumer routing and associated static configuration parameters.
     * </p>
     * <p>
     * <b>Warning:</b> This method may throw {@link NullPointerException} if {@code event}, {@code consumer},
     * or any {@code staticData} fields are null when {@link String#format(String, Object...)} is invoked.
     * Callers should ensure fields are initialized before invoking this method.
     * </p>
     * <p>
     * Output format: "Event listener on %s forwarded to %s. Static data: %s, %s, %s, %s."
     * </p>
     *
     * @return formatted notification message for logging and audit trails
     * @throws NullPointerException if any referenced field is null during string formatting
     */
    @Override
    public String notificationMessage() {
        return String.format("Event listener on %s forwarded to %s. Static data: %s, %s, %s, %s.", event, consumer, staticData1, staticData2, staticData3, staticData4);
    }

}