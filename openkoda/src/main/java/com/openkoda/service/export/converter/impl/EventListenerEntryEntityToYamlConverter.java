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

package com.openkoda.service.export.converter.impl;

import com.openkoda.core.flow.LoggingComponent;
import com.openkoda.model.component.event.EventListenerEntry;
import com.openkoda.service.export.dto.EventListenerEntryConversionDto;
import org.springframework.stereotype.Component;

import static com.openkoda.service.export.FolderPathConstants.EVENT_;
import static com.openkoda.service.export.FolderPathConstants.EXPORT_CONFIG_PATH_;

/**
 * Converter for exporting EventListenerEntry entities to YAML-only configuration files.
 * <p>
 * This converter extends {@link AbstractEntityToYamlConverter} to handle EventListenerEntry entities
 * and produces ONLY YAML component files without separate content files. Event listener entries
 * contain metadata for runtime event subscription and handling, including consumer class/method
 * names, event types, and static configuration data.
 * </p>
 * <p>
 * YAML path construction follows the pattern: EXPORT_CONFIG_PATH_ + EVENT_ + "{eventName}-{orgId}.yaml"
 * using the inherited {@link #getYamlDefaultFilePath(String, String, Long)} method. Organization IDs
 * are included in filenames for tenant-scoped event listeners, enabling multi-tenancy support.
 * </p>
 * <p>
 * This converter produces ONLY YAML component files:
 * <ul>
 *   <li>{@link #getPathToContentFile(EventListenerEntry)} returns null (no content files)</li>
 *   <li>{@link #getContent(EventListenerEntry)} returns null (no content to export)</li>
 *   <li>{@link #getPathToYamlComponentFile(EventListenerEntry)} returns YAML configuration path</li>
 *   <li>{@link #getConversionDto(EventListenerEntry)} creates DTO for YAML serialization</li>
 * </ul>
 * </p>
 * <p>
 * Thread-safety: This class is a stateless Spring {@code @Component} and is safe for concurrent use
 * by multiple threads. All state is passed via method parameters.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractEntityToYamlConverter
 * @see EventListenerEntry
 * @see EventListenerEntryConversionDto
 * @see com.openkoda.service.export.FolderPathConstants
 * @see LoggingComponent
 */
@Component
public class EventListenerEntryEntityToYamlConverter extends AbstractEntityToYamlConverter<EventListenerEntry, EventListenerEntryConversionDto> implements  LoggingComponent {

    /**
     * Returns the content file path for the EventListenerEntry entity.
     * <p>
     * This method intentionally returns null because EventListenerEntry entities export only
     * YAML configuration without separate content files. All event listener metadata
     * (consumer class, event name, parameters) is contained within the YAML component file.
     * </p>
     *
     * @param entity the EventListenerEntry entity (unused)
     * @return always null - no content file for event listener entries
     */
    @Override
    public String getPathToContentFile(EventListenerEntry entity) {
        return null;
    }

    /**
     * Returns the content string for the EventListenerEntry entity.
     * <p>
     * This method intentionally returns null because EventListenerEntry entities have no
     * content to export beyond YAML metadata. Unlike Form or ServerJs entities that include
     * JavaScript code files, event listeners contain only configuration metadata.
     * </p>
     *
     * @param entity the EventListenerEntry entity (unused)
     * @return always null - no content for event listener entries
     */
    @Override
    public String getContent(EventListenerEntry entity) {
        return null;
    }

    /**
     * Constructs the YAML component file path for event listener configuration.
     * <p>
     * This method generates the absolute filesystem path where the EventListenerEntry's YAML
     * configuration will be exported. The path pattern follows:
     * <pre>
     * EXPORT_CONFIG_PATH_ + EVENT_ + "{eventName}-{orgId}.yaml"
     * </pre>
     * </p>
     * <p>
     * Organization-specific behavior: The organizationId is included in the filename for
     * tenant-scoped event listeners, enabling multi-tenancy support. If organizationId is null,
     * the listener is treated as global (no organization suffix).
     * </p>
     *
     * @param entity the EventListenerEntry entity to export (must not be null)
     * @return absolute path to YAML configuration file
     * @throws NullPointerException if entity is null or entity.getEventName() is null
     * @see com.openkoda.service.export.FolderPathConstants#EXPORT_CONFIG_PATH_
     * @see com.openkoda.service.export.FolderPathConstants#EVENT_
     */
    @Override
    public String getPathToYamlComponentFile(EventListenerEntry entity) {
        return getYamlDefaultFilePath(EXPORT_CONFIG_PATH_ + EVENT_, entity.getEventName(), entity.getOrganizationId());
    }

    /**
     * Creates an EventListenerEntryConversionDto for YAML serialization.
     * <p>
     * This method converts the EventListenerEntry entity to a Data Transfer Object (DTO)
     * suitable for YAML serialization. The DTO contains all metadata necessary to recreate
     * the event listener during import, including consumer/event class names, method
     * signatures, and static configuration data.
     * </p>
     * <p>
     * DTO field mappings from entity:
     * <ul>
     *   <li>consumerClassName - fully qualified class name of the event consumer</li>
     *   <li>eventName - unique identifier for the event type</li>
     *   <li>consumerMethodName - method name to invoke when event fires</li>
     *   <li>consumerParameterClassName - parameter type for consumer method</li>
     *   <li>eventClassName - fully qualified class name of the event</li>
     *   <li>eventObjectType - type of the event object</li>
     *   <li>indexString - indexed search string for the listener</li>
     *   <li>staticData1-4 - four static configuration fields for custom data</li>
     *   <li>module - module name from entity.getModuleName()</li>
     *   <li>organizationId - tenant scope identifier</li>
     * </ul>
     * All fields are copied directly from entity to DTO, preserving null values.
     * </p>
     *
     * @param entity the EventListenerEntry entity to convert (must not be null)
     * @return populated EventListenerEntryConversionDto ready for YAML serialization
     * @throws NullPointerException if entity is null
     * @see EventListenerEntryConversionDto
     */
    @Override
    public EventListenerEntryConversionDto getConversionDto(EventListenerEntry entity) {
        EventListenerEntryConversionDto dto = new EventListenerEntryConversionDto();
        dto.setConsumerClassName(entity.getConsumerClassName());
        dto.setEventName(entity.getEventName());
        dto.setConsumerMethodName(entity.getConsumerMethodName());
        dto.setConsumerParameterClassName(entity.getConsumerParameterClassName());
        dto.setEventClassName(entity.getEventClassName());
        dto.setEventObjectType(entity.getEventObjectType());
        dto.setIndexString(entity.getIndexString());
        dto.setStaticData1(entity.getStaticData1());
        dto.setStaticData2(entity.getStaticData2());
        dto.setStaticData3(entity.getStaticData3());
        dto.setStaticData4(entity.getStaticData4());
        dto.setModule(entity.getModuleName());
        dto.setOrganizationId(entity.getOrganizationId());
        return dto;
    }
}
