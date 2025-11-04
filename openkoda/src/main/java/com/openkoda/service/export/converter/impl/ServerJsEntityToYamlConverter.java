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
import com.openkoda.model.component.ServerJs;
import com.openkoda.service.export.dto.ServerJsConversionDto;
import org.springframework.stereotype.Component;

import static com.openkoda.service.export.FolderPathConstants.*;

/**
 * Converter that extends {@link AbstractEntityToYamlConverter} for {@link ServerJs} entities.
 * <p>
 * This converter converts ServerJs entities to YAML configuration and JavaScript code file pairs for export.
 * The JavaScript code file is stored separately from the YAML metadata, with the YAML containing a relative
 * resource path reference to the code file.
 * 
 * <p>
 * <b>JavaScript Content File Path Construction:</b> EXPORT_CODE_PATH_ + SERVER_SIDE_ + orgPath + "{name}.js"
 * 
 * <p>
 * <b>YAML Path Construction:</b> EXPORT_CONFIG_PATH_ + SERVER_SIDE_ + "{name}-{orgId}.yaml" via getYamlDefaultFilePath
 * 
 * <p>
 * <b>CRITICAL:</b> The SERVER_SIDE_ path prefix distinguishes server-side JavaScript (executed by GraalVM JS engine)
 * from client-side frontend resources. This separation ensures proper organization and prevents confusion between
 * server and client code.
 * 
 * <p>
 * <b>Organization-Specific Behavior:</b> The organizationId is included in both the JavaScript file path and
 * YAML filename for tenant-scoped server scripts. Global scripts (organizationId null) are stored without
 * the organization prefix.
 * 
 * <p>
 * This converter implements {@link LoggingComponent} for debug logging capabilities. It is a stateless
 * Spring {@code @Component} and is safe for concurrent use.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractEntityToYamlConverter
 * @see ServerJs
 * @see ServerJsConversionDto
 * @see LoggingComponent
 * @see com.openkoda.service.export.FolderPathConstants
 */
@Component
public class ServerJsEntityToYamlConverter extends AbstractEntityToYamlConverter<ServerJs, ServerJsConversionDto> implements LoggingComponent {

    /**
     * Constructs the absolute file path for the server-side JavaScript code content file.
     * <p>
     * <b>Path Pattern:</b> EXPORT_CODE_PATH_ + SERVER_SIDE_ + orgPath + "{name}.js"
     * 
     * <p>
     * <b>Filename Format:</b> "{name}.js" where name is from entity.getName()
     * 
     * <p>
     * <b>Organization Path Logic:</b>
     * <ul>
     *   <li>If organizationId is null: orgPath = "" (global script, no organization prefix)</li>
     *   <li>If organizationId is not null: orgPath = SUBDIR_ORGANIZATION_PREFIX + organizationId + "/" (tenant-scoped)</li>
     * </ul>
     * 
     * <p>
     * <b>CRITICAL:</b> The SERVER_SIDE_ prefix ensures that server-side JavaScript is organized separately from
     * frontend resources. This prevents confusion between server-executed code and client-side JavaScript.
     * 
     *
     * @param entity the ServerJs entity to export (must not be null)
     * @return absolute path to .js content file containing server-side JavaScript code
     * @throws NullPointerException if entity is null or entity.getName() is null
     * @see com.openkoda.service.export.FolderPathConstants#EXPORT_CODE_PATH_
     * @see com.openkoda.service.export.FolderPathConstants#SERVER_SIDE_
     * @see com.openkoda.service.export.FolderPathConstants#SUBDIR_ORGANIZATION_PREFIX
     */
    @Override
    public String getPathToContentFile(ServerJs entity) {
        String orgPath = entity.getOrganizationId() == null ? "" : SUBDIR_ORGANIZATION_PREFIX + entity.getOrganizationId() + "/";
        String entityExportPath = SERVER_SIDE_ + orgPath;
        return EXPORT_CODE_PATH_ + entityExportPath + String.format("%s.js", entity.getName());
    }

    /**
     * Retrieves the JavaScript code content from the ServerJs entity.
     * <p>
     * Returns entity.getCode() which contains the server-side JavaScript implementation that will be
     * executed by the GraalVM JS engine.
     * 
     *
     * @param entity the ServerJs entity (must not be null)
     * @return JavaScript code string, may be null if no code is defined
     * @throws NullPointerException if entity is null
     */
    @Override
    public String getContent(ServerJs entity) {
        return entity.getCode();
    }

    /**
     * Constructs the absolute YAML component file path for server-side JavaScript metadata.
     * <p>
     * <b>Path Pattern:</b> EXPORT_CONFIG_PATH_ + SERVER_SIDE_ + "{name}-{orgId}.yaml" via getYamlDefaultFilePath
     * 
     * <p>
     * <b>Organization-Specific Behavior:</b> The organizationId is included in the YAML filename for tenant-scoped
     * server scripts. This allows multiple organizations to have server scripts with the same name without conflicts.
     * 
     *
     * @param entity the ServerJs entity to export (must not be null)
     * @return absolute path to YAML configuration file
     * @throws NullPointerException if entity is null or entity.getName() is null
     * @see com.openkoda.service.export.FolderPathConstants#EXPORT_CONFIG_PATH_
     * @see com.openkoda.service.export.FolderPathConstants#SERVER_SIDE_
     */
    @Override
    public String getPathToYamlComponentFile(ServerJs entity) {
        return getYamlDefaultFilePath(EXPORT_CONFIG_PATH_ + SERVER_SIDE_, entity.getName(), entity.getOrganizationId());
    }

    /**
     * Creates a {@link ServerJsConversionDto} for YAML serialization from the ServerJs entity.
     * <p>
     * <b>DTO Field Mappings:</b>
     * <ul>
     *   <li>arguments - script parameter names/types from entity.getArguments()</li>
     *   <li>model - data model identifier from entity.getModel()</li>
     *   <li>name - script name from entity.getName()</li>
     *   <li>code - relative resource path to .js file via getResourcePathToContentFile (NOT the actual JavaScript content)</li>
     *   <li>module - module name from entity.getModuleName()</li>
     *   <li>organizationId - tenant organization ID from entity.getOrganizationId()</li>
     * </ul>
     * 
     * <p>
     * <b>Note:</b> The code field contains a relative resource path to the .js file, not the actual JavaScript content.
     * This allows the import process to locate and load the code file separately.
     * 
     *
     * @param entity the ServerJs entity to convert (must not be null)
     * @return populated ServerJsConversionDto ready for YAML serialization
     * @throws NullPointerException if entity is null
     * @see ServerJsConversionDto
     */
    @Override
    public ServerJsConversionDto getConversionDto(ServerJs entity) {
        ServerJsConversionDto dto = new ServerJsConversionDto();
        dto.setArguments(entity.getArguments());
        dto.setModel(entity.getModel());
        dto.setName(entity.getName());
        dto.setCode(getResourcePathToContentFile(entity));
        dto.setModule(entity.getModuleName());
        dto.setOrganizationId(entity.getOrganizationId());
        return dto;
    }
}
