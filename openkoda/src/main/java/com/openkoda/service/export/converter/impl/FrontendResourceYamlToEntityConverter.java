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
import com.openkoda.model.component.FrontendResource;
import com.openkoda.service.export.converter.YamlToEntityConverter;
import com.openkoda.service.export.converter.YamlToEntityParentConverter;
import com.openkoda.service.export.dto.ControllerEndpointConversionDto;
import com.openkoda.service.export.dto.FrontendResourceConversionDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.openkoda.service.export.FolderPathConstants.*;

/**
 * Converter that imports FrontendResource entities from YAML configuration files with cascading controller endpoint import.
 * <p>
 * This converter implements {@link YamlToEntityConverter} to convert {@link FrontendResourceConversionDto} objects into
 * {@link FrontendResource} entities and persist them to the database. The converter is automatically discovered by the
 * export/import subsystem through the {@link YamlToEntityParentConverter} annotation.
 * </p>
 * <p>
 * <strong>Key Responsibilities:</strong>
 * </p>
 * <ul>
 * <li>Converts YAML-sourced DTOs to FrontendResource entities</li>
 * <li>Extracts accessLevel and organizationId from file path using string parsing (UI_COMPONENT_ or FRONTEND_RESOURCE_ patterns)</li>
 * <li>Implements lookup-or-create semantics: finds existing resource by name/accessLevel/organizationId, creates new if not found</li>
 * <li>Cascades import of embedded ControllerEndpoint DTOs by delegating to {@link ControllerEndpointYamlToEntityConverter}</li>
 * <li>Supports both filesystem I/O and in-memory resource loading</li>
 * </ul>
 * <p>
 * <strong>Path-Derived Parsing:</strong> The converter parses the YAML file path to extract metadata:
 * </p>
 * <ul>
 * <li>Access level: extracted between UI_COMPONENT_ or FRONTEND_RESOURCE_ prefix and first "/"</li>
 * <li>Organization ID: extracted between SUBDIR_ORGANIZATION_PREFIX and first "/"</li>
 * </ul>
 * <p>
 * <strong>Cascading Behavior:</strong> After saving the FrontendResource entity, the converter iterates through
 * {@code dto.getControllerEndpoints()} and delegates each embedded endpoint DTO to
 * {@link ControllerEndpointYamlToEntityConverter} for import. This enables complete frontend resource import
 * including all associated controller endpoints in a single operation.
 * </p>
 * <p>
 * This converter is a stateless Spring {@code @Component} and is safe for concurrent use by multiple threads.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see YamlToEntityConverter
 * @see YamlToEntityParentConverter
 * @see FrontendResource
 * @see FrontendResourceConversionDto
 * @see ControllerEndpointYamlToEntityConverter
 */
@Component
@YamlToEntityParentConverter(dtoClass = FrontendResourceConversionDto.class)
public class FrontendResourceYamlToEntityConverter extends ComponentProvider implements YamlToEntityConverter<FrontendResource, FrontendResourceConversionDto> {

    @Autowired
    ControllerEndpointYamlToEntityConverter controllerEndpointYamlToEntityConverter;

    /**
     * Converts a FrontendResourceConversionDto to a FrontendResource entity using filesystem I/O and imports embedded controller endpoints.
     * <p>
     * This method performs the following operations:
     * </p>
     * <ol>
     * <li>Parses accessLevel and organizationId from the file path using {@link #setAccessLevelAndOrgIdFromPath(FrontendResourceConversionDto, String)}</li>
     * <li>Retrieves or creates a FrontendResource entity via {@link #getFrontendResource(FrontendResourceConversionDto)}</li>
     * <li>Loads the resource content from the filesystem using {@code loadResourceAsString(dto.getContent())}</li>
     * <li>Saves the FrontendResource entity to the database</li>
     * <li>If the DTO contains controller endpoints, converts them by delegating to {@link #convertControllerEndpoints(List, Long)}</li>
     * </ol>
     *
     * @param dto the FrontendResourceConversionDto containing frontend resource metadata (must not be null)
     * @param filePath the YAML file path used to extract accessLevel and organizationId (must not be null)
     * @return the saved FrontendResource entity with generated ID
     * @throws NullPointerException if dto or filePath is null
     * @throws RuntimeException if file I/O fails during content loading or path parsing fails
     * @see #setAccessLevelAndOrgIdFromPath(FrontendResourceConversionDto, String)
     * @see #convertControllerEndpoints(List, Long)
     */
    @Override
    public FrontendResource convertAndSave(FrontendResourceConversionDto dto, String filePath) {
        debug("[convertAndSave]");
        setAccessLevelAndOrgIdFromPath(dto, filePath);
        FrontendResource frontendResource = getFrontendResource(dto);
        frontendResource.setContent(loadResourceAsString(dto.getContent()));
        repositories.secure.frontendResource.saveOne(frontendResource);
        if(dto.getControllerEndpoints() != null){
            convertControllerEndpoints(dto.getControllerEndpoints(), frontendResource.getId());
        }
        return frontendResource;
    }
    
    /**
     * Converts a FrontendResourceConversionDto to a FrontendResource entity using in-memory resources and imports embedded controller endpoints.
     * <p>
     * This method avoids file I/O by retrieving content from the provided resources Map. It performs the following operations:
     * </p>
     * <ol>
     * <li>Parses accessLevel and organizationId from the file path using {@link #setAccessLevelAndOrgIdFromPath(FrontendResourceConversionDto, String)}</li>
     * <li>Retrieves or creates a FrontendResource entity via {@link #getFrontendResource(FrontendResourceConversionDto)}</li>
     * <li>Retrieves the resource content from the resources Map using {@code dto.getContent()} as the key</li>
     * <li>Saves the FrontendResource entity to the database</li>
     * <li>If the DTO contains controller endpoints, converts them using the resources Map by delegating to {@link #convertControllerEndpoints(List, Long, Map)}</li>
     * </ol>
     *
     * @param dto the FrontendResourceConversionDto containing frontend resource metadata (must not be null)
     * @param filePath the YAML file path used to extract accessLevel and organizationId (must not be null)
     * @param resources Map of resource paths to content strings for in-memory content lookup
     * @return the saved FrontendResource entity with generated ID
     * @throws NullPointerException if dto, filePath, or resources is null
     * @see #convertControllerEndpoints(List, Long, Map)
     */
    @Override
    public FrontendResource convertAndSave(FrontendResourceConversionDto dto, String filePath, Map<String, String> resources) {
        debug("[convertAndSave]");
        setAccessLevelAndOrgIdFromPath(dto, filePath);
        FrontendResource frontendResource = getFrontendResource(dto);
        frontendResource.setContent(resources.get(dto.getContent()));
        repositories.secure.frontendResource.saveOne(frontendResource);
        if(dto.getControllerEndpoints() != null){
            convertControllerEndpoints(dto.getControllerEndpoints(), frontendResource.getId(), resources);
        }
        return frontendResource;
    }

    /**
     * Parses the YAML file path to extract and set accessLevel and organizationId in the DTO.
     * <p>
     * This method uses string parsing to extract metadata from the file path structure:
     * </p>
     * <ul>
     * <li><strong>Access Level:</strong> Extracted between UI_COMPONENT_ or FRONTEND_RESOURCE_ prefix and the first "/" character.
     * The extracted string is converted to uppercase and parsed as a {@link FrontendResource.AccessLevel} enum value.</li>
     * <li><strong>Organization ID:</strong> Extracted between SUBDIR_ORGANIZATION_PREFIX and the first "/" character.
     * The extracted string is parsed as a Long value.</li>
     * </ul>
     * <p>
     * The method modifies the DTO in-place, setting the accessLevel and organizationId fields only if the parsed
     * values are non-empty strings. If the parsing yields empty strings, the DTO fields remain unchanged.
     * </p>
     *
     * @param dto the FrontendResourceConversionDto to populate with extracted metadata (must not be null)
     * @param filePath the YAML file path to parse for metadata extraction (must not be null)
     * @throws IllegalArgumentException if the accessLevel string is not a valid FrontendResource.AccessLevel enum value
     * @throws NumberFormatException if the organizationId string is not a valid Long
     */
    private void setAccessLevelAndOrgIdFromPath(FrontendResourceConversionDto dto, String filePath) {
        //        get access level and orgId from file path
        String accessLevel = filePath.contains(UI_COMPONENT_) ? StringUtils.substringBetween(filePath, UI_COMPONENT_, "/")
                : StringUtils.substringBetween(filePath, FRONTEND_RESOURCE_, "/");
        if(StringUtils.isNotEmpty(accessLevel)) {
            dto.setAccessLevel(FrontendResource.AccessLevel.valueOf(accessLevel.toUpperCase()));
        }

        String orgIdString = StringUtils.substringBetween(filePath, SUBDIR_ORGANIZATION_PREFIX, "/");
        if(StringUtils.isNotEmpty(orgIdString)) {
            dto.setOrganizationId(Long.parseLong(orgIdString));
        }
    }

    /**
     * Imports a list of ControllerEndpoint DTOs by delegating to ControllerEndpointYamlToEntityConverter (filesystem I/O version).
     * <p>
     * This method iterates through the provided list of {@link ControllerEndpointConversionDto} objects,
     * sets the frontendResourceId on each DTO to associate the endpoint with the parent FrontendResource,
     * and delegates the conversion and persistence to {@link ControllerEndpointYamlToEntityConverter#convertAndSave(ControllerEndpointConversionDto, String)}.
     * </p>
     *
     * @param controllerEndpointConversionDtos list of controller endpoint DTOs to import (must not be null)
     * @param frontendResourceId the ID of the parent FrontendResource to associate endpoints with (must not be null)
     * @throws NullPointerException if any parameter is null
     * @throws RuntimeException if endpoint conversion fails during delegation
     */
    private void convertControllerEndpoints(List<ControllerEndpointConversionDto> controllerEndpointConversionDtos, Long frontendResourceId) {
        for (ControllerEndpointConversionDto controllerEndpointConversionDto : controllerEndpointConversionDtos) {
            controllerEndpointConversionDto.setFrontendResourceId(frontendResourceId);
            controllerEndpointYamlToEntityConverter.convertAndSave(controllerEndpointConversionDto, null);
        }
    }

    /**
     * Imports a list of ControllerEndpoint DTOs using in-memory resources by delegating to ControllerEndpointYamlToEntityConverter.
     * <p>
     * This method iterates through the provided list of {@link ControllerEndpointConversionDto} objects,
     * sets the frontendResourceId on each DTO to associate the endpoint with the parent FrontendResource,
     * and delegates the conversion and persistence to
     * {@link ControllerEndpointYamlToEntityConverter#convertAndSave(ControllerEndpointConversionDto, String, Map)}
     * with the resources Map for in-memory content lookup.
     * </p>
     *
     * @param controllerEndpointConversionDtos list of controller endpoint DTOs to import (must not be null)
     * @param frontendResourceId the ID of the parent FrontendResource to associate endpoints with (must not be null)
     * @param resources Map of resource paths to content strings for in-memory content lookup (must not be null)
     * @throws NullPointerException if any parameter is null
     */
    private void convertControllerEndpoints(List<ControllerEndpointConversionDto> controllerEndpointConversionDtos, Long frontendResourceId, Map<String, String> resources) {
        for (ControllerEndpointConversionDto controllerEndpointConversionDto : controllerEndpointConversionDtos) {
            controllerEndpointConversionDto.setFrontendResourceId(frontendResourceId);
            controllerEndpointYamlToEntityConverter.convertAndSave(controllerEndpointConversionDto, null, resources);
        }
    }

    /**
     * Lookup-or-create helper that retrieves an existing FrontendResource or creates a new instance.
     * <p>
     * This method implements lookup-or-create semantics by querying the database for an existing
     * FrontendResource using a composite key of name, accessLevel, and organizationId. If no matching
     * entity is found, a new FrontendResource instance is created.
     * </p>
     * <p>
     * The method maps the following DTO fields to the entity:
     * </p>
     * <ul>
     * <li><strong>name</strong> - Entity name (used as lookup key)</li>
     * <li><strong>accessLevel</strong> - Access level enum (used as lookup key)</li>
     * <li><strong>organizationId</strong> - Tenant organization ID (used as lookup key)</li>
     * <li><strong>includeInSitemap</strong> - Whether to include in sitemap</li>
     * <li><strong>requiredPrivilege</strong> - Required privilege for access</li>
     * <li><strong>type</strong> - Resource type (HTML, JS, CSS, etc.)</li>
     * <li><strong>resourceType</strong> - Resource category</li>
     * <li><strong>moduleName</strong> - Module identifier</li>
     * <li><strong>embeddable</strong> - Whether resource can be embedded</li>
     * </ul>
     * <p>
     * This method always returns a non-null entity, either an existing one from the database or a
     * new instance with fields populated from the DTO.
     * </p>
     *
     * @param dto the FrontendResourceConversionDto containing resource metadata (must not be null)
     * @return existing FrontendResource if found by composite key, or new instance with fields populated from DTO
     * @throws NullPointerException if dto is null
     */
    private FrontendResource getFrontendResource(FrontendResourceConversionDto dto){
        FrontendResource frontendResource = repositories.unsecure.frontendResource.findByNameAndAccessLevelAndOrganizationId(dto.getName(), dto.getAccessLevel(), dto.getOrganizationId());
        if(frontendResource == null) {
            frontendResource = new FrontendResource();
            frontendResource.setName(dto.getName());
            frontendResource.setAccessLevel(dto.getAccessLevel());
            frontendResource.setOrganizationId(dto.getOrganizationId());
        }
        frontendResource.setIncludeInSitemap(dto.getIncludeInSitemap());
        frontendResource.setRequiredPrivilege(dto.getRequiredPrivilege());
        frontendResource.setType(dto.getType());
        frontendResource.setResourceType(dto.getResourceType());
        frontendResource.setModuleName(dto.getModule());
        frontendResource.setEmbeddable(dto.isEmbeddable());
        return frontendResource;
    }
}
