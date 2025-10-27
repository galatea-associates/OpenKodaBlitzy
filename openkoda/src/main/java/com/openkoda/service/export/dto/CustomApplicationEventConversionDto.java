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
 * DTO for custom application event metadata including event class, category, and privilege requirements.
 * <p>
 * This is a mutable JavaBean POJO for YAML/JSON serialization of custom application event definitions.
 * Extends ComponentDto for module and organization scope. All fields are String type for flexible
 * event metadata representation. Used by export/import pipelines for transferring custom application
 * event configurations between environments. Not thread-safe.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ComponentDto for inherited module and organizationId fields
 */
public class CustomApplicationEventConversionDto extends ComponentDto {

    /**
     * Event type identifier. Nullable.
     */
    private String type;
    
    /**
     * Human-readable event name. Nullable.
     */
    private String name;
    
    /**
     * Technical event name for registration. Nullable.
     */
    private String eventName;
    
    /**
     * Fully-qualified class name of the event implementation. Nullable.
     */
    private String eventClass;
    
    /**
     * Event category for logical grouping. Nullable.
     */
    private String eventCategory;

    /**
     * Required privilege name for reading/viewing this event. Nullable.
     */
    private String readPrivilege;
    
    /**
     * Required privilege name for triggering/writing this event. Nullable.
     */
    private String writePrivilege;
    
    /**
     * Computed index string for search and filtering. Nullable.
     */
    private String indexString;
    
    /**
     * Class name metadata for reflection-based operations. Nullable.
     */
    private String className;
    
    
    /**
     * Gets the event type identifier.
     *
     * @return the event type identifier or null if not set
     */
    public String getType() {
        return type;
    }
    
    /**
     * Sets the event type identifier.
     *
     * @param type the event type identifier to set, may be null
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Gets the event category for logical grouping.
     *
     * @return the event category or null if not set
     */
    public String getEventCategory() {
        return eventCategory;
    }
    
    /**
     * Sets the event category for logical grouping.
     *
     * @param category the event category to set, may be null
     */
    public void setEventCategory(String category) {
        this.eventCategory = category;
    }
    
    /**
     * Gets the human-readable event name.
     *
     * @return the human-readable event name or null if not set
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the human-readable event name.
     *
     * @param name the human-readable event name to set, may be null
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Gets the technical event name for registration.
     *
     * @return the technical event name or null if not set
     */
    public String getEventName() {
        return eventName;
    }
    
    /**
     * Sets the technical event name for registration.
     *
     * @param eventName the technical event name to set, may be null
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
    
    /**
     * Gets the fully-qualified class name of the event implementation.
     *
     * @return the fully-qualified class name or null if not set
     */
    public String getEventClass() {
        return eventClass;
    }
    
    /**
     * Sets the fully-qualified class name of the event implementation.
     *
     * @param eventClass the fully-qualified class name to set, may be null
     */
    public void setEventClass(String eventClass) {
        this.eventClass = eventClass;
    }
    
    /**
     * Gets the required privilege name for reading/viewing this event.
     *
     * @return the required privilege name for reading or null if not set
     */
    public String getReadPrivilege() {
        return readPrivilege;
    }
    
    /**
     * Sets the required privilege name for reading/viewing this event.
     *
     * @param readPrivilege the required privilege name for reading to set, may be null
     */
    public void setReadPrivilege(String readPrivilege) {
        this.readPrivilege = readPrivilege;
    }
    
    /**
     * Gets the required privilege name for triggering/writing this event.
     *
     * @return the required privilege name for triggering or null if not set
     */
    public String getWritePrivilege() {
        return writePrivilege;
    }
    
    /**
     * Sets the required privilege name for triggering/writing this event.
     *
     * @param writePrivilege the required privilege name for triggering to set, may be null
     */
    public void setWritePrivilege(String writePrivilege) {
        this.writePrivilege = writePrivilege;
    }
    
    /**
     * Gets the computed index string for search and filtering.
     *
     * @return the computed index string or null if not set
     */
    public String getIndexString() {
        return indexString;
    }
    
    /**
     * Sets the computed index string for search and filtering.
     *
     * @param indexString the computed index string to set, may be null
     */
    public void setIndexString(String indexString) {
        this.indexString = indexString;
    }
    
    /**
     * Gets the class name metadata for reflection-based operations.
     *
     * @return the class name metadata or null if not set
     */
    public String getClassName() {
        return className;
    }
    
    /**
     * Sets the class name metadata for reflection-based operations.
     *
     * @param className the class name metadata to set, may be null
     */
    public void setClassName(String className) {
        this.className = className;
    }

    
}
