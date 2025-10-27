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
import com.openkoda.model.component.ControllerEndpoint;
import com.openkoda.service.export.converter.YamlToEntityConverter;
import com.openkoda.service.export.dto.ControllerEndpointConversionDto;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.openkoda.service.export.FolderPathConstants.SUBDIR_ORGANIZATION_PREFIX;

/**
 * Converts ControllerEndpoint entities from YAML representation for import operations.
 * <p>
 * This converter implements {@link YamlToEntityConverter} to transform {@link ControllerEndpointConversionDto}
 * objects into persisted {@link ControllerEndpoint} entities. It provides lookup-or-create semantics by searching
 * for existing endpoints using the combination of frontendResourceId, subPath, httpMethod, and organizationId,
 * creating new instances when no match is found.
 * </p>
 * <p>
 * The converter supports two modes of operation: file-based code loading using {@code loadResourceAsString}
 * and in-memory code loading from a provided resources Map. This flexibility enables both filesystem-based
 * and memory-based import workflows.
 * </p>
 * <p>
 * Thread-safety: This stateless Spring component is safe for concurrent use across multiple threads.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * ControllerEndpointConversionDto dto = ...;
 * ControllerEndpoint endpoint = converter.convertAndSave(dto, "path/to/file.yaml");
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see YamlToEntityConverter
 * @see ControllerEndpoint
 * @see ControllerEndpointConversionDto
 * @see ComponentProvider
 */
@Component
public class ControllerEndpointYamlToEntityConverter extends ComponentProvider implements YamlToEntityConverter<ControllerEndpoint, ControllerEndpointConversionDto> {
    
    /**
     * Converts a ControllerEndpointConversionDto to a ControllerEndpoint entity and persists it to the database.
     * <p>
     * This method uses lookup-or-create semantics via {@link #getControllerEndpoint(ControllerEndpointConversionDto)}
     * to find existing endpoints or create new instances. The endpoint code content is loaded from the filesystem
     * using {@code loadResourceAsString} based on the path specified in the DTO.
     * </p>
     *
     * @param dto the ControllerEndpointConversionDto containing endpoint metadata (must not be null)
     * @param filePath the YAML file path (unused but required by interface contract)
     * @return the saved ControllerEndpoint entity with generated ID and persisted state
     * @throws NullPointerException if dto is null
     * @throws RuntimeException if file I/O fails during code content loading
     */
    @Override
    public ControllerEndpoint convertAndSave(ControllerEndpointConversionDto dto, String filePath) {
        debug("[convertAndSave]");

        ControllerEndpoint controllerEndpoint = getControllerEndpoint(dto);
        controllerEndpoint.setCode(loadResourceAsString(dto.getCode()));
        return repositories.unsecure.controllerEndpoint.save(controllerEndpoint);

    }
    
    /**
     * Converts a ControllerEndpointConversionDto to a ControllerEndpoint entity using in-memory resources.
     * <p>
     * This overload avoids filesystem I/O by retrieving endpoint code content from the provided resources Map
     * using the key specified by {@code dto.getCode()}. This enables efficient batch import operations where
     * all resources are preloaded into memory.
     * </p>
     *
     * @param dto the ControllerEndpointConversionDto containing endpoint metadata (must not be null)
     * @param filePath the YAML file path (unused but required by interface contract)
     * @param resources Map of resource paths to content strings for in-memory code lookup
     * @return the saved ControllerEndpoint entity with generated ID and persisted state
     * @throws NullPointerException if dto or resources is null, or if the key from dto.getCode() is not present in resources Map
     */
    @Override
    public ControllerEndpoint convertAndSave(ControllerEndpointConversionDto dto, String filePath, Map<String, String> resources) {
        debug("[convertAndSave]");

        ControllerEndpoint controllerEndpoint = getControllerEndpoint(dto);
        controllerEndpoint.setCode(resources.get(dto.getCode()));
        return repositories.unsecure.controllerEndpoint.save(controllerEndpoint);
    }

    /**
     * Extracts organization ID from a file path containing the organization subdirectory prefix.
     * <p>
     * This method uses {@code StringUtils.substringBetween} to parse the organization ID that appears
     * between {@code SUBDIR_ORGANIZATION_PREFIX} and the next forward slash in the file path. Returns
     * null if the path does not contain the organization prefix or if the extracted string is empty.
     * </p>
     *
     * @param filePath the file path potentially containing organization prefix (may be null or not contain prefix)
     * @return organization ID as Long if found, null if path does not contain organization prefix or orgIdString is empty
     * @throws NumberFormatException if the extracted organization ID string cannot be parsed as a valid Long
     */
    private Long getOrgIdFromPath(String filePath) {
        String orgIdString = StringUtils.substringBetween(filePath, SUBDIR_ORGANIZATION_PREFIX, "/");
        return StringUtils.isNotEmpty(orgIdString) ? Long.parseLong(orgIdString) : null;
    }

    /**
     * Retrieves an existing ControllerEndpoint or creates a new instance based on DTO metadata.
     * <p>
     * This helper method implements lookup-or-create semantics by querying the repository using the composite key
     * of frontendResourceId, subPath, httpMethod, and organizationId. If an existing endpoint is found, it is returned
     * and updated with the DTO values. If no match exists, a new ControllerEndpoint instance is created and populated
     * with metadata from the DTO.
     * </p>
     * <p>
     * DTO to entity field mappings:
     * <ul>
     * <li>frontendResourceId → frontendResourceId</li>
     * <li>subpath → subPath (defaults to empty string if null)</li>
     * <li>httpMethod → httpMethod (converted via enum valueOf)</li>
     * <li>organizationId → organizationId</li>
     * <li>httpHeaders → httpHeaders</li>
     * <li>modelAttributes → modelAttributes</li>
     * <li>responseType → responseType</li>
     * <li>module → moduleName</li>
     * </ul>
     * </p>
     *
     * @param dto the ControllerEndpointConversionDto containing endpoint metadata (must not be null)
     * @return existing ControllerEndpoint if found via lookup query, or new instance with fields populated from DTO (never null)
     * @throws NullPointerException if dto is null
     * @throws IllegalArgumentException if dto.getHttpMethod() or dto.getResponseType() contains an invalid enum value
     */
    @NotNull
    private ControllerEndpoint getControllerEndpoint(ControllerEndpointConversionDto dto) {
        ControllerEndpoint controllerEndpoint = repositories.unsecure.controllerEndpoint.findByFrontendResourceIdAndSubPathAndHttpMethodAndOrganizationId(
                dto.getFrontendResourceId(), dto.getSubpath(), StringUtils.isNotEmpty(dto.getHttpMethod()) ? ControllerEndpoint.HttpMethod.valueOf(dto.getHttpMethod()) : null, dto.getOrganizationId());
        if(controllerEndpoint == null) {
            controllerEndpoint = new ControllerEndpoint();
            controllerEndpoint.setFrontendResourceId(dto.getFrontendResourceId());
            controllerEndpoint.setSubPath(dto.getSubpath() != null ? dto.getSubpath() : "");
            controllerEndpoint.setHttpMethod(dto.getHttpMethod());
            controllerEndpoint.setOrganizationId(dto.getOrganizationId());
        }
        controllerEndpoint.setHttpHeaders(dto.getHttpHeaders());
        controllerEndpoint.setModelAttributes(dto.getModelAttributes());
        controllerEndpoint.setResponseType(dto.getResponseType());
        controllerEndpoint.setModuleName(dto.getModule());
        return controllerEndpoint;
    }

}
