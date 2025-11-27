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

package com.openkoda.service.export.dto;

/**
 * DTO for event listener registration metadata specifying consumer class, method, and static data parameters.
 * <p>
 * This is a mutable JavaBean POJO for YAML/JSON serialization that extends ComponentDto to inherit
 * module and organization scope fields. It maps the EventListenerEntry domain entity for component
 * exports. All fields are String type, providing flexible metadata representation for event listener
 * registration. This DTO is used by export/import pipelines and listener registration systems to
 * configure event-driven behavior. It is not thread-safe and should not be shared across threads
 * without external synchronization.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ComponentDto for inherited module and organizationId fields
 * @see com.openkoda.model.component.event.EventListenerEntry
 */
public class EventListenerEntryConversionDto extends ComponentDto {

    /**
     * Fully-qualified class name of the event consumer/handler. Nullable.
     */
    private String consumerClassName;
    
    /**
     * Method name in the consumer class to invoke when event is triggered. Nullable.
     */
    private String consumerMethodName;
    
    /**
     * Fully-qualified class name of the method parameter type. Nullable.
     */
    private String consumerParameterClassName;
    
    /**
     * Fully-qualified class name of the event being listened to. Nullable.
     */
    private String eventClassName;
    
    /**
     * Technical event name for listener registration. Nullable.
     */
    private String eventName;
    
    /**
     * Event object type identifier for filtering and routing. Nullable.
     */
    private String eventObjectType;
    
    /**
     * Computed index string for search and filtering operations. Nullable.
     */
    private String indexString;
    
    /**
     * First static data parameter passed to listener on invocation. Nullable.
     */
    private String staticData1;
    
    /**
     * Second static data parameter passed to listener on invocation. Nullable.
     */
    private String staticData2;
    
    /**
     * Third static data parameter passed to listener on invocation. Nullable.
     */
    private String staticData3;
    
    /**
     * Fourth static data parameter passed to listener on invocation. Nullable.
     */
    private String staticData4;

    /**
     * Gets the fully-qualified class name of the event consumer/handler.
     *
     * @return the consumer class name or null if not set
     */
    public String getConsumerClassName() {
        return consumerClassName;
    }

    /**
     * Sets the fully-qualified class name of the event consumer/handler.
     *
     * @param consumerClassName the consumer class name to set, may be null
     */
    public void setConsumerClassName(String consumerClassName) {
        this.consumerClassName = consumerClassName;
    }

    /**
     * Gets the method name in the consumer class to invoke when event is triggered.
     *
     * @return the consumer method name or null if not set
     */
    public String getConsumerMethodName() {
        return consumerMethodName;
    }

    /**
     * Sets the method name in the consumer class to invoke when event is triggered.
     *
     * @param consumerMethodName the consumer method name to set, may be null
     */
    public void setConsumerMethodName(String consumerMethodName) {
        this.consumerMethodName = consumerMethodName;
    }

    /**
     * Gets the fully-qualified class name of the method parameter type.
     *
     * @return the consumer parameter class name or null if not set
     */
    public String getConsumerParameterClassName() {
        return consumerParameterClassName;
    }

    /**
     * Sets the fully-qualified class name of the method parameter type.
     *
     * @param consumerParameterClassName the consumer parameter class name to set, may be null
     */
    public void setConsumerParameterClassName(String consumerParameterClassName) {
        this.consumerParameterClassName = consumerParameterClassName;
    }

    /**
     * Gets the fully-qualified class name of the event being listened to.
     *
     * @return the event class name or null if not set
     */
    public String getEventClassName() {
        return eventClassName;
    }

    /**
     * Sets the fully-qualified class name of the event being listened to.
     *
     * @param eventClassName the event class name to set, may be null
     */
    public void setEventClassName(String eventClassName) {
        this.eventClassName = eventClassName;
    }

    /**
     * Gets the technical event name for listener registration.
     *
     * @return the event name or null if not set
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Sets the technical event name for listener registration.
     *
     * @param eventName the event name to set, may be null
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Gets the event object type identifier for filtering and routing.
     *
     * @return the event object type or null if not set
     */
    public String getEventObjectType() {
        return eventObjectType;
    }

    /**
     * Sets the event object type identifier for filtering and routing.
     *
     * @param eventObjectType the event object type to set, may be null
     */
    public void setEventObjectType(String eventObjectType) {
        this.eventObjectType = eventObjectType;
    }

    /**
     * Gets the computed index string for search and filtering operations.
     *
     * @return the index string or null if not set
     */
    public String getIndexString() {
        return indexString;
    }

    /**
     * Sets the computed index string for search and filtering operations.
     *
     * @param indexString the index string to set, may be null
     */
    public void setIndexString(String indexString) {
        this.indexString = indexString;
    }

    /**
     * Gets the first static data parameter passed to listener on invocation.
     *
     * @return the first static data parameter or null if not set
     */
    public String getStaticData1() {
        return staticData1;
    }

    /**
     * Sets the first static data parameter passed to listener on invocation.
     *
     * @param staticData1 the first static data parameter to set, may be null
     */
    public void setStaticData1(String staticData1) {
        this.staticData1 = staticData1;
    }

    /**
     * Gets the second static data parameter passed to listener on invocation.
     *
     * @return the second static data parameter or null if not set
     */
    public String getStaticData2() {
        return staticData2;
    }

    /**
     * Sets the second static data parameter passed to listener on invocation.
     *
     * @param staticData2 the second static data parameter to set, may be null
     */
    public void setStaticData2(String staticData2) {
        this.staticData2 = staticData2;
    }

    /**
     * Gets the third static data parameter passed to listener on invocation.
     *
     * @return the third static data parameter or null if not set
     */
    public String getStaticData3() {
        return staticData3;
    }

    /**
     * Sets the third static data parameter passed to listener on invocation.
     *
     * @param staticData3 the third static data parameter to set, may be null
     */
    public void setStaticData3(String staticData3) {
        this.staticData3 = staticData3;
    }

    /**
     * Gets the fourth static data parameter passed to listener on invocation.
     *
     * @return the fourth static data parameter or null if not set
     */
    public String getStaticData4() {
        return staticData4;
    }

    /**
     * Sets the fourth static data parameter passed to listener on invocation.
     *
     * @param staticData4 the fourth static data parameter to set, may be null
     */
    public void setStaticData4(String staticData4) {
        this.staticData4 = staticData4;
    }

}
