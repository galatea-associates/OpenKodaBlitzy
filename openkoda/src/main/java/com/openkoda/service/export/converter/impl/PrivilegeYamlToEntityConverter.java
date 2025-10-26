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
import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.model.DynamicPrivilege;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.PrivilegeGroup;
import com.openkoda.service.export.converter.YamlToEntityConverter;
import com.openkoda.service.export.converter.YamlToEntityParentConverter;
import com.openkoda.service.export.dto.PrivilegeConversionDto;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Converter that imports DynamicPrivilege entities from YAML configuration files.
 * <p>
 * This converter implements the {@link YamlToEntityConverter} interface to convert
 * {@link PrivilegeConversionDto} objects into {@link DynamicPrivilege} entities and
 * persists them through the privilege service. The converter is automatically discovered
 * by the export/import subsystem via the {@link YamlToEntityParentConverter} annotation.
 * </p>
 * <p>
 * <b>Key Responsibilities:</b>
 * </p>
 * <ul>
 * <li>Converts PrivilegeConversionDto to DynamicPrivilege entities with lookup-or-create semantics</li>
 * <li>Uses {@link PrivilegeHelper#valueOfString(String)} to find existing privileges by name</li>
 * <li>Delegates persistence to {@code services.privilege.createOrUpdateDynamicPrivilege} for proper privilege registration</li>
 * <li>Sets removable field to true for all dynamic privileges (per application design)</li>
 * </ul>
 * <p>
 * <b>Delegation Pattern:</b> This converter does NOT directly save entities via repository.
 * Instead, it delegates to {@link com.openkoda.service.privilege.PrivilegeService#createOrUpdateDynamicPrivilege}
 * to ensure proper privilege enumeration registration in the runtime privilege system.
 * </p>
 * <p>
 * <b>Return Type:</b> Methods return {@link PrivilegeBase} (interface), but the actual
 * returned instance is always a {@link DynamicPrivilege}.
 * </p>
 * <p>
 * <b>Thread Safety:</b> Stateless Spring {@code @Component} safe for concurrent use.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see YamlToEntityConverter
 * @see YamlToEntityParentConverter
 * @see PrivilegeBase
 * @see DynamicPrivilege
 * @see PrivilegeConversionDto
 * @see ComponentProvider
 * @see PrivilegeHelper
 */
@Component
@YamlToEntityParentConverter(dtoClass = PrivilegeConversionDto.class)
public class PrivilegeYamlToEntityConverter extends ComponentProvider implements YamlToEntityConverter<PrivilegeBase, PrivilegeConversionDto> {

    /**
     * Converts a PrivilegeConversionDto to a DynamicPrivilege entity and persists it via PrivilegeService.
     * <p>
     * This method is the filesystem I/O version of the converter (though privileges have no content files).
     * It delegates to {@link #getPrivilege(PrivilegeConversionDto)} to create or update the entity,
     * then registers the privilege through {@code services.privilege.createOrUpdateDynamicPrivilege}
     * to ensure proper privilege enumeration registration in the runtime system.
     * </p>
     * <p>
     * <b>Note:</b> The {@code filePath} parameter is unused because privileges have no associated
     * content files, but it is required by the {@link YamlToEntityConverter} interface.
     * </p>
     *
     * @param dto the PrivilegeConversionDto containing privilege metadata (must not be null)
     * @param filePath the YAML file path (unused but required by interface)
     * @return PrivilegeBase (DynamicPrivilege instance) after registration
     * @throws NullPointerException if dto is null
     * @throws IllegalArgumentException if dto.getGroup() is not a valid PrivilegeGroup enum value
     * @see #getPrivilege(PrivilegeConversionDto)
     * @see com.openkoda.service.privilege.PrivilegeService#createOrUpdateDynamicPrivilege
     */
    @Override
    public PrivilegeBase convertAndSave(PrivilegeConversionDto dto, String filePath) {
        debug("[convertAndSave]");
        DynamicPrivilege form = getPrivilege(dto);
        return services.privilege.createOrUpdateDynamicPrivilege(form);
    }

    /**
     * Converts a PrivilegeConversionDto to a DynamicPrivilege entity and persists it via PrivilegeService (in-memory version).
     * <p>
     * This method has identical behavior to {@link #convertAndSave(PrivilegeConversionDto, String)}
     * but accepts an in-memory resources Map. The resources parameter is unused because privileges
     * have no associated content files, but the method signature is required by the
     * {@link YamlToEntityConverter} interface.
     * </p>
     * <p>
     * The method delegates to {@link #getPrivilege(PrivilegeConversionDto)} to create or update
     * the entity, then registers the privilege through {@code services.privilege.createOrUpdateDynamicPrivilege}.
     * </p>
     *
     * @param dto the PrivilegeConversionDto containing privilege metadata (must not be null)
     * @param filePath the YAML file path (unused)
     * @param resources Map of resource paths to content (unused)
     * @return PrivilegeBase (DynamicPrivilege instance) after registration
     * @throws NullPointerException if dto is null
     * @throws IllegalArgumentException if dto.getGroup() is not a valid PrivilegeGroup enum value
     */
    @Override
    public PrivilegeBase convertAndSave(PrivilegeConversionDto dto, String filePath, Map<String, String> resources) {
        debug("[convertAndSave]");
        DynamicPrivilege form = getPrivilege(dto);
        return services.privilege.createOrUpdateDynamicPrivilege(form);
    }

    /**
     * Lookup-or-create helper that finds an existing privilege by name or instantiates a new DynamicPrivilege.
     * <p>
     * This method performs a lookup using {@link PrivilegeHelper#getInstance()#valueOfString(String)}
     * to find an existing privilege by name. If found, it casts the {@link PrivilegeBase} to
     * {@link DynamicPrivilege} (assumes only dynamic privileges are managed by this converter).
     * If not found, it creates a new DynamicPrivilege instance.
     * </p>
     * <p>
     * <b>DTO to Entity Mappings:</b>
     * </p>
     * <ul>
     * <li>{@code name} - Lookup key and identifier</li>
     * <li>{@code group} - Converted to {@link PrivilegeGroup} enum via valueOf</li>
     * <li>{@code category} - Privilege category string</li>
     * <li>{@code label} - Human-readable label</li>
     * <li>{@code indexString} - Search index string for privilege lookup</li>
     * <li>{@code removable} - Always set to true (dynamic privileges are always removable per design)</li>
     * </ul>
     *
     * @param dto the PrivilegeConversionDto containing privilege metadata (must not be null)
     * @return DynamicPrivilege with fields populated from DTO (either existing or new instance)
     * @throws NullPointerException if dto is null
     * @throws IllegalArgumentException if dto.getGroup() is not a valid PrivilegeGroup enum value
     * @throws ClassCastException if existing privilege is not DynamicPrivilege (should not occur)
     * @see PrivilegeHelper#valueOfString(String)
     * @see PrivilegeGroup
     */
    @NotNull
    private DynamicPrivilege getPrivilege(PrivilegeConversionDto dto) {
        PrivilegeBase form = PrivilegeHelper.getInstance().valueOfString(dto.getName());
        DynamicPrivilege dynamicPrivilege;
        if(form == null) {
            dynamicPrivilege = new DynamicPrivilege();
            dynamicPrivilege.setName(dto.getName());
        } else {
            dynamicPrivilege = (DynamicPrivilege)form;
        }
        
        dynamicPrivilege.setGroup(PrivilegeGroup.valueOf(dto.getGroup()));
        dynamicPrivilege.setCategory(dto.getCategory());
        dynamicPrivilege.setLabel(dto.getLabel());
        dynamicPrivilege.setIndexString(dto.getIndexString());
        // Dynamic priovilages are always removable
        dynamicPrivilege.setRemovable(true);
        return dynamicPrivilege;
    }
}
