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
import com.openkoda.model.component.ServerJs;
import com.openkoda.service.export.converter.YamlToEntityConverter;
import com.openkoda.service.export.converter.YamlToEntityParentConverter;
import com.openkoda.service.export.dto.ServerJsConversionDto;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Converter that implements YamlToEntityConverter for importing ServerJs entities from YAML configuration files.
 * <p>
 * This converter transforms {@link ServerJsConversionDto} objects into {@link ServerJs} entities and persists them
 * to the database. The converter is auto-discovered via the {@code @YamlToEntityParentConverter} annotation which
 * binds it to the ServerJsConversionDto class, enabling seamless YAML import workflows.
 * 
 * <p>
 * The conversion process follows lookup-or-create semantics: it searches for an existing ServerJs entity by name,
 * and creates a new instance if not found. ServerJs represents server-side JavaScript code that is executed by the
 * GraalVM JS engine, enabling dynamic scripting capabilities within the OpenKoda platform.
 * 
 * <p>
 * This converter depends on {@link ComponentProvider} to access both secure and unsecure repository beans
 * (repositories.secure.serverJs and repositories.unsecure.serverJs). The secure repository enforces privilege
 * checks during save operations, while the unsecure repository is used for read-only lookups during import.
 * 
 * <p>
 * Thread-Safety: This is a stateless Spring {@code @Component} that is safe for concurrent use by multiple threads.
 * All state is passed through method parameters, and repository operations are transactional.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see YamlToEntityConverter
 * @see YamlToEntityParentConverter
 * @see ServerJs
 * @see ServerJsConversionDto
 * @see ComponentProvider
 */
@Component
@YamlToEntityParentConverter(dtoClass = ServerJsConversionDto.class)
public class ServerJsYamlToEntityConverter extends ComponentProvider implements YamlToEntityConverter<ServerJs, ServerJsConversionDto> {

    /**
     * Converts a ServerJsConversionDto to a ServerJs entity using filesystem I/O and persists it to the database.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Calls {@link #getServerJs(ServerJsConversionDto)} to lookup or create the ServerJs entity</li>
     *   <li>Loads the JavaScript code from the filesystem using {@code loadResourceAsString(dto.getCode())}</li>
     *   <li>Sets the loaded code on the ServerJs entity</li>
     *   <li>Saves the entity using {@code repositories.secure.serverJs.saveOne()}</li>
     * </ol>
     * 
     *
     * @param dto the ServerJsConversionDto containing server-side script metadata including name, arguments,
     *            model identifier, module name, organization ID, and code file path (must not be null)
     * @param filePath the YAML file path (unused but required by the YamlToEntityConverter interface)
     * @return the saved ServerJs entity with generated ID and all fields populated
     * @throws NullPointerException if dto is null
     * @throws RuntimeException if file I/O fails during loadResourceAsString operation
     * @see #getServerJs(ServerJsConversionDto)
     * @see #convertAndSave(ServerJsConversionDto, String, Map)
     */
    @Override
    public ServerJs convertAndSave(ServerJsConversionDto dto, String filePath) {
        debug("[convertAndSave]");
        ServerJs serverJs = getServerJs(dto);
        serverJs.setCode(loadResourceAsString(dto.getCode()));
        return repositories.secure.serverJs.saveOne(serverJs);
    }

    /**
     * Converts a ServerJsConversionDto to a ServerJs entity using in-memory resources and persists it to the database.
     * <p>
     * This method is an optimization that avoids filesystem I/O by retrieving JavaScript code from an in-memory
     * resources Map. It performs the following steps:
     * <ol>
     *   <li>Calls {@link #getServerJs(ServerJsConversionDto)} to lookup or create the ServerJs entity</li>
     *   <li>Retrieves the JavaScript code from the resources Map using {@code dto.getCode()} as the key</li>
     *   <li>Sets the retrieved code on the ServerJs entity</li>
     *   <li>Saves the entity using {@code repositories.secure.serverJs.saveOne()}</li>
     * </ol>
     * This method is typically used during batch imports where all resources are preloaded into memory.
     * 
     *
     * @param dto the ServerJsConversionDto containing server-side script metadata including name, arguments,
     *            model identifier, module name, organization ID, and code resource key (must not be null)
     * @param filePath the YAML file path (unused but required by the YamlToEntityConverter interface)
     * @param resources Map of resource paths to content strings for in-memory code lookup (must not be null)
     * @return the saved ServerJs entity with generated ID and all fields populated
     * @throws NullPointerException if dto or resources is null, or if the key {@code dto.getCode()} is not
     *                              present in the resources Map
     * @see #getServerJs(ServerJsConversionDto)
     * @see #convertAndSave(ServerJsConversionDto, String)
     */
    @Override
    public ServerJs convertAndSave(ServerJsConversionDto dto, String filePath, Map<String, String> resources) {
        debug("[convertAndSave]");
        ServerJs serverJs = getServerJs(dto);
        serverJs.setCode(resources.get(dto.getCode()));
        return repositories.secure.serverJs.saveOne(serverJs);
    }

    /**
     * Lookup-or-create helper that finds an existing ServerJs entity by name or instantiates a new one.
     * <p>
     * This method implements idempotent import behavior by performing a lookup query using
     * {@code repositories.unsecure.serverJs.findByName(dto.getName())}. If an existing entity is found,
     * it is reused and its fields are updated. If not found, a new ServerJs instance is created.
     * 
     * <p>
     * The following field mappings are applied from DTO to entity:
     * <ul>
     *   <li><b>name</b> - Used as the lookup key and entity identifier</li>
     *   <li><b>arguments</b> - Script parameter names and types (e.g., "userId:Long,status:String")</li>
     *   <li><b>model</b> - Data model identifier that the script operates on</li>
     *   <li><b>moduleName</b> - Module name for organizational grouping (from dto.getModule())</li>
     *   <li><b>organizationId</b> - Tenant scope for multi-tenancy isolation</li>
     * </ul>
     * 
     * <p>
     * Note: The {@code code} field is NOT set by this method. The caller is responsible for setting the JavaScript
     * code content, either from filesystem I/O or from an in-memory resources Map.
     * 
     *
     * @param dto the ServerJsConversionDto containing script metadata (must not be null)
     * @return existing ServerJs entity if found by name, or a new instance with fields populated from DTO
     *         (excluding code field). Always returns non-null.
     * @throws NullPointerException if dto is null
     */
    private ServerJs getServerJs(ServerJsConversionDto dto){
        ServerJs serverJs = repositories.unsecure.serverJs.findByName(dto.getName());
        if(serverJs == null) {
            serverJs = new ServerJs();
            serverJs.setName(dto.getName());
        }
        serverJs.setArguments(dto.getArguments());
        serverJs.setModel(dto.getModel());
        serverJs.setModuleName(dto.getModule());
        serverJs.setOrganizationId(dto.getOrganizationId());
        return serverJs;
    }
}
