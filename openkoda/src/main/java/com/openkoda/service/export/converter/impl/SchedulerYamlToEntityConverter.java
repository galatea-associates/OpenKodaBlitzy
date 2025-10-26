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
import com.openkoda.model.component.Scheduler;
import com.openkoda.service.export.converter.YamlToEntityConverter;
import com.openkoda.service.export.converter.YamlToEntityParentConverter;
import com.openkoda.service.export.dto.SchedulerConversionDto;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Converter for importing Scheduler entities from YAML configuration files.
 * <p>
 * This converter implements {@link YamlToEntityConverter} and is responsible for converting
 * {@link SchedulerConversionDto} objects to {@link Scheduler} entities and persisting them to the database.
 * The converter is annotated with {@code @YamlToEntityParentConverter(dtoClass = SchedulerConversionDto.class)}
 * to enable auto-discovery by the export/import subsystem.
 * </p>
 * <p>
 * Important behavioral characteristics:
 * <ul>
 *   <li>Creates NEW entities only - no lookup-or-update logic, always instantiates fresh Scheduler</li>
 *   <li>Supports optional scheduler registration: when resources Map is provided, registers the scheduler job
 *       via {@code services.scheduler.schedule} for runtime execution</li>
 *   <li>Enables hot-reload of scheduled jobs without application restart when using the resources Map overload</li>
 * </ul>
 * </p>
 * <p>
 * This class is a stateless Spring {@code @Component} and is safe for concurrent use.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see YamlToEntityConverter
 * @see YamlToEntityParentConverter
 * @see Scheduler
 * @see SchedulerConversionDto
 * @see ComponentProvider
 */
@Component
@YamlToEntityParentConverter(dtoClass = SchedulerConversionDto.class)
public class SchedulerYamlToEntityConverter extends ComponentProvider implements YamlToEntityConverter<Scheduler, SchedulerConversionDto> {

    /**
     * Converts a SchedulerConversionDto to a new Scheduler entity and persists it to the database.
     * <p>
     * This method instantiates a new Scheduler entity, maps all DTO fields to the entity, and saves
     * it via {@code repositories.secure.scheduler.saveOne}. It does NOT register the scheduler with
     * the runtime scheduler service. To enable runtime execution of the scheduled job, use
     * {@link #convertAndSave(SchedulerConversionDto, String, Map)} instead.
     * </p>
     * <p>
     * DTO to entity field mappings:
     * <ul>
     *   <li>cronExpression - Cron schedule expression for job execution</li>
     *   <li>eventData - Event identifier/payload for the scheduled event</li>
     *   <li>onMasterOnly - Flag indicating if job should run only on cluster master</li>
     *   <li>moduleName - Module that owns this scheduler</li>
     *   <li>organizationId - Tenant organization ID for scoped schedulers</li>
     * </ul>
     * </p>
     *
     * @param dto the SchedulerConversionDto containing scheduler metadata (must not be null)
     * @param filePath the YAML file path (unused but required by interface)
     * @return the saved Scheduler entity with generated ID
     * @throws NullPointerException if dto is null
     */
    @Override
    public Scheduler convertAndSave(SchedulerConversionDto dto, String filePath) {
        debug("[convertAndSave]");

        Scheduler scheduler = new Scheduler();
        scheduler.setCronExpression(dto.getCronExpression());
        scheduler.setEventData(dto.getEventData());
        scheduler.setOnMasterOnly(dto.isOnMasterOnly());
        scheduler.setModuleName(dto.getModule());
        scheduler.setOrganizationId(dto.getOrganizationId());

        return repositories.secure.scheduler.saveOne(scheduler);
    }

    /**
     * Converts a SchedulerConversionDto to a Scheduler entity, persists it, AND registers the scheduler
     * job for runtime execution.
     * <p>
     * This method delegates to {@link #convertAndSave(SchedulerConversionDto, String)} for entity
     * persistence, then calls {@code services.scheduler.schedule} to register the scheduler job with
     * the runtime scheduler service. The presence of the resources Map parameter triggers scheduler
     * registration, enabling scheduled job execution.
     * </p>
     * <p>
     * This overload enables hot-reload of scheduled jobs without application restart, allowing dynamic
     * import and activation of scheduler configurations.
     * </p>
     *
     * @param dto the SchedulerConversionDto containing scheduler metadata (must not be null)
     * @param filePath the YAML file path (unused but required by interface)
     * @param resources Map of resource paths to content strings (unused, but presence triggers registration)
     * @return the saved Scheduler entity with generated ID, now registered in scheduler service
     * @throws NullPointerException if dto is null
     * @see com.openkoda.service.scheduler.SchedulerService#schedule(Scheduler)
     */
    @Override
    public Scheduler convertAndSave(SchedulerConversionDto dto, String filePath, Map<String, String> resources) {
        Scheduler scheduler = convertAndSave(dto, filePath);
        services.scheduler.schedule(scheduler);
        return scheduler;
    }
}
