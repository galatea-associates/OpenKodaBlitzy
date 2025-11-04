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
import com.openkoda.model.component.Scheduler;
import com.openkoda.service.export.dto.SchedulerConversionDto;
import org.springframework.stereotype.Component;

import static com.openkoda.service.export.FolderPathConstants.EXPORT_CONFIG_PATH_;
import static com.openkoda.service.export.FolderPathConstants.SCHEDULER_;

/**
 * Converter that transforms Scheduler entities to YAML-only export format.
 * <p>
 * This converter extends {@link AbstractEntityToYamlConverter} to handle the export of
 * {@link Scheduler} entities. Unlike converters that export both YAML configuration and
 * separate content files, this converter produces YAML-only output since schedulers contain
 * all necessary configuration within their entity properties.

 * <p>
 * <b>YAML-Only Output:</b> Both {@link #getPathToContentFile(Scheduler)} and 
 * {@link #getContent(Scheduler)} return null, indicating no separate content files are generated.

 * <p>
 * <b>YAML Path Construction:</b> The YAML file path follows the pattern 
 * {@code EXPORT_CONFIG_PATH_ + SCHEDULER_ + "{eventData}-{orgId}.yaml"}, constructed via
 * {@link #getYamlDefaultFilePath(String, String, Long)} using the entity's eventData as the
 * base filename.

 * <p>
 * <b>DTO Mapping:</b> The converter maps the following Scheduler fields to SchedulerConversionDto:
 * <ul>
 *   <li>cronExpression - cron schedule syntax for job execution timing</li>
 *   <li>eventData - event identifier or payload data</li>
 *   <li>onMasterOnly - flag indicating whether job runs only on cluster master</li>
 *   <li>module - module name (from entity.getModuleName())</li>
 *   <li>organizationId - tenant scope identifier</li>
 * </ul>

 * <p>
 * <b>Thread Safety:</b> This is a stateless Spring {@link Component} that is safe for
 * concurrent use across multiple threads.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractEntityToYamlConverter
 * @see Scheduler
 * @see SchedulerConversionDto
 * @see LoggingComponent
 * @see com.openkoda.service.export.FolderPathConstants
 */
@Component
public class SchedulerEntityToYamlConverter extends AbstractEntityToYamlConverter<Scheduler, SchedulerConversionDto> implements LoggingComponent {

    /**
     * Returns the content file path for the scheduler entity.
     * <p>
     * This method intentionally returns null because Scheduler entities export only YAML
     * configuration without separate content files. All scheduler configuration is contained
     * within the YAML metadata, making additional content files unnecessary.

     *
     * @param entity the Scheduler entity (unused)
     * @return always null - no content file for scheduler entities
     */
    @Override
    public String getPathToContentFile(Scheduler entity) {
        return null;
    }

    /**
     * Returns the content string for the scheduler entity.
     * <p>
     * This method intentionally returns null because Scheduler entities have no content to
     * export beyond YAML metadata. The scheduler configuration is fully represented in the
     * YAML file produced by {@link #getConversionDto(Scheduler)}, eliminating the need for
     * separate content data.

     *
     * @param entity the Scheduler entity (unused)
     * @return always null - no content for scheduler entities
     */
    @Override
    public String getContent(Scheduler entity) {
        return null;
    }

    /**
     * Constructs the YAML component file path for scheduler configuration export.
     * <p>
     * The path follows the pattern: {@code EXPORT_CONFIG_PATH_ + SCHEDULER_ + "{eventData}-{orgId}.yaml"},
     * constructed via {@link #getYamlDefaultFilePath(String, String, Long)}. The entity's
     * eventData serves as the base filename, which describes the scheduled event.

     * <p>
     * <b>Organization-Specific Behavior:</b> The organizationId is included in the filename
     * for tenant-scoped schedulers, ensuring that schedulers for different tenants are
     * exported to separate files and can be properly restored.

     *
     * @param entity the Scheduler entity to export (must not be null)
     * @return absolute path to YAML configuration file
     * @throws NullPointerException if entity is null or entity.getEventData() is null
     * @see com.openkoda.service.export.FolderPathConstants#EXPORT_CONFIG_PATH_
     * @see com.openkoda.service.export.FolderPathConstants#SCHEDULER_
     */
    @Override
    public String getPathToYamlComponentFile(Scheduler entity) {
        return getYamlDefaultFilePath(EXPORT_CONFIG_PATH_ + SCHEDULER_, entity.getEventData(), entity.getOrganizationId());
    }

    /**
     * Creates a SchedulerConversionDto for YAML serialization from the Scheduler entity.
     * <p>
     * This method maps all relevant scheduler configuration fields from the entity to the DTO,
     * which is then serialized to YAML format. The following field mappings are performed:
     * <ul>
     *   <li><b>cronExpression</b> - cron schedule syntax defining job execution timing</li>
     *   <li><b>eventData</b> - event identifier or payload data that describes the scheduled event</li>
     *   <li><b>onMasterOnly</b> - boolean flag indicating whether job executes only on cluster master node</li>
     *   <li><b>module</b> - module name (obtained via entity.getModuleName())</li>
     *   <li><b>organizationId</b> - tenant scope identifier for organization-specific schedulers</li>
     * </ul>

     * <p>
     * All fields are copied directly from the entity to the DTO, preserving null values where
     * present in the source entity.

     *
     * @param entity the Scheduler entity to convert (must not be null)
     * @return populated SchedulerConversionDto ready for YAML serialization
     * @throws NullPointerException if entity is null
     * @see SchedulerConversionDto
     */
    @Override
    public SchedulerConversionDto getConversionDto(Scheduler entity) {
        SchedulerConversionDto dto = new SchedulerConversionDto();
        dto.setCronExpression(entity.getCronExpression());
        dto.setEventData(entity.getEventData());
        dto.setOnMasterOnly(entity.isOnMasterOnly());
        dto.setModule(entity.getModuleName());
        dto.setOrganizationId(entity.getOrganizationId());
        return dto;
    }
}
