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

import com.openkoda.controller.ComponentProvider;
import com.openkoda.model.component.event.EventListenerEntry;
import com.openkoda.service.export.converter.YamlToEntityConverter;
import com.openkoda.service.export.converter.YamlToEntityParentConverter;
import com.openkoda.service.export.dto.EventListenerEntryConversionDto;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Converts EventListenerEntry entities from YAML import format.
 * <p>
 * This converter implements the {@link YamlToEntityConverter} interface to handle importing
 * {@link EventListenerEntry} entities from YAML files during the export/import process.
 * It converts {@link EventListenerEntryConversionDto} data transfer objects to
 * {@link EventListenerEntry} entities and persists them to the database.
 * </p>
 * <p>
 * The {@code @YamlToEntityParentConverter} annotation with {@code dtoClass = EventListenerEntryConversionDto.class}
 * enables auto-discovery by the export/import subsystem, allowing this converter to be automatically
 * invoked when processing EventListenerEntry definitions in YAML files.
 * </p>
 * <p>
 * This converter creates NEW entities only (no lookup-or-update logic). Each conversion always
 * instantiates a fresh {@link EventListenerEntry} and persists it via the secure repository.
 * </p>
 * <p>
 * Optional cluster-aware listener registration is supported: when the {@code resources} Map is
 * provided to the three-parameter {@code convertAndSave} method, the converter registers the
 * listener via {@code services.eventListener.registerListenerClusterAware}, enabling hot-reload
 * of event listeners without application restart.
 * </p>
 * <p>
 * This converter depends on {@link ComponentProvider} for access to
 * {@code repositories.secure.eventListener} and {@code services.eventListener}.
 * </p>
 * <p>
 * Thread-safety: This stateless Spring {@code @Component} is safe for concurrent use by multiple threads.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see YamlToEntityConverter
 * @see YamlToEntityParentConverter
 * @see EventListenerEntry
 * @see EventListenerEntryConversionDto
 * @see ComponentProvider
 */
@Component
@YamlToEntityParentConverter(dtoClass = EventListenerEntryConversionDto.class)
public class EventListenerEntryYamlToEntityConverter extends ComponentProvider implements YamlToEntityConverter<EventListenerEntry, EventListenerEntryConversionDto> {

    /**
     * Converts DTO to new EventListenerEntry entity and persists to database.
     * <p>
     * This method instantiates a new {@link EventListenerEntry}, maps all fields from the provided
     * {@link EventListenerEntryConversionDto}, and saves the entity via
     * {@code repositories.secure.eventListener.saveOne}.
     * </p>
     * <p>
     * DTO to entity field mappings:
     * <ul>
     *   <li>consumerClassName - fully qualified class name of event consumer</li>
     *   <li>consumerMethodName - method name to invoke on event</li>
     *   <li>consumerParameterClassName - parameter type for consumer method</li>
     *   <li>eventClassName - fully qualified class name of event</li>
     *   <li>eventName - human-readable event name</li>
     *   <li>eventObjectType - type classification of event object</li>
     *   <li>indexString - indexed search string</li>
     *   <li>staticData1-4 - static data fields for custom metadata</li>
     *   <li>moduleName - owning module name (from dto.getModule())</li>
     *   <li>organizationId - organization scope identifier</li>
     * </ul>
     * </p>
     * <p>
     * Note: This method does NOT register the listener with the event system. For runtime registration,
     * use {@link #convertAndSave(EventListenerEntryConversionDto, String, Map)} with a resources Map.
     * </p>
     *
     * @param dto the EventListenerEntryConversionDto containing listener metadata (must not be null)
     * @param filePath the YAML file path (unused but required by interface contract)
     * @return the saved EventListenerEntry entity with database-generated ID
     * @throws NullPointerException if dto is null
     */
    @Override
    public EventListenerEntry convertAndSave(EventListenerEntryConversionDto dto, String filePath) {
        debug("[convertAndSave]");

        EventListenerEntry eventListenerEntry = new EventListenerEntry();
        eventListenerEntry.setConsumerClassName(dto.getConsumerClassName());
        eventListenerEntry.setConsumerMethodName(dto.getConsumerMethodName());
        eventListenerEntry.setConsumerParameterClassName(dto.getConsumerParameterClassName());
        eventListenerEntry.setEventClassName(dto.getEventClassName());
        eventListenerEntry.setEventName(dto.getEventName());
        eventListenerEntry.setEventObjectType(dto.getEventObjectType());
        eventListenerEntry.setIndexString(dto.getIndexString());
        eventListenerEntry.setStaticData1(dto.getStaticData1());
        eventListenerEntry.setStaticData2(dto.getStaticData2());
        eventListenerEntry.setStaticData3(dto.getStaticData3());
        eventListenerEntry.setStaticData4(dto.getStaticData4());
        eventListenerEntry.setModuleName(dto.getModule());
        eventListenerEntry.setOrganizationId(dto.getOrganizationId());
        return repositories.secure.eventListener.saveOne(eventListenerEntry);
    }

    /**
     * Converts DTO to EventListenerEntry entity, persists, AND registers listener cluster-aware.
     * <p>
     * This method delegates to {@link #convertAndSave(EventListenerEntryConversionDto, String)}
     * for entity persistence, then calls {@code services.eventListener.registerListenerClusterAware}
     * for runtime listener registration.
     * </p>
     * <p>
     * The presence of the {@code resources} Map triggers cluster-aware listener registration,
     * which distributes the event listener across cluster nodes. This enables hot-reload of
     * event listeners without requiring application restart.
     * </p>
     * <p>
     * Use this overload when importing event listeners that should be immediately active in
     * the running application's event system.
     * </p>
     *
     * @param dto the EventListenerEntryConversionDto containing listener metadata (must not be null)
     * @param filePath the YAML file path (unused but required by interface contract)
     * @param resources Map of resource paths to content strings (unused, but presence triggers registration)
     * @return the saved EventListenerEntry entity with database-generated ID, now registered in event system
     * @throws NullPointerException if dto is null
     * @see com.openkoda.service.eventlistener.EventListenerService#registerListenerClusterAware
     */
    @Override
    public EventListenerEntry convertAndSave(EventListenerEntryConversionDto dto, String filePath, Map<String, String> resources) {
        debug("[convertAndSave]");
        EventListenerEntry eventListenerEntry = convertAndSave(dto, filePath);
        services.eventListener.registerListenerClusterAware(eventListenerEntry);
        return eventListenerEntry;
    }
}
