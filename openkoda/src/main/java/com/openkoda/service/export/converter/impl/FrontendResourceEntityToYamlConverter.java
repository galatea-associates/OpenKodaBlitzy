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

import com.openkoda.model.component.ControllerEndpoint;
import com.openkoda.model.component.FrontendResource;
import com.openkoda.repository.ControllerEndpointRepository;
import com.openkoda.service.export.dto.ControllerEndpointConversionDto;
import com.openkoda.service.export.dto.FrontendResourceConversionDto;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import static com.openkoda.service.export.FolderPathConstants.*;

/**
 * Converter that transforms {@link FrontendResource} entities to YAML configuration and content file pairs for export,
 * with cascading export of associated {@link ControllerEndpoint} entities.
 * <p>
 * This converter extends {@link AbstractEntityToYamlConverter} to provide specialized export logic for frontend resources
 * (HTML, JavaScript, CSS files) including both UI components and frontend resources. The converter produces two types of output:
 * </p>
 * <ul>
 *   <li>Content files: HTML/JS/CSS source code at EXPORT_RESOURCES_PATH_ + exportPath + name + type.extension</li>
 *   <li>YAML configuration: metadata at EXPORT_CONFIG_PATH_ + exportPath + name + ".yaml"</li>
 * </ul>
 * <p>
 * <b>Critical Cascading Behavior:</b> The {@link #addToZip(FrontendResource, ZipOutputStream, Set)} method queries
 * {@link ControllerEndpointRepository} for all controller endpoints associated with the frontend resource and delegates
 * their packaging to {@link ControllerEndpointEntityToYamlConverter} <i>before</i> packaging the frontend resource itself.
 * This ensures complete export of the resource and its API endpoints in a single operation.
 * </p>
 * <p>
 * <b>Export Path Construction:</b> The export path is computed based on resource type, access level, and organization:
 * </p>
 * <pre>
 * exportPath = (resourceType == UI_COMPONENT ? UI_COMPONENT_ : FRONTEND_RESOURCE_) 
 *            + accessLevel.getPath() 
 *            + (organizationId == null ? "" : SUBDIR_ORGANIZATION_PREFIX + organizationId + "/")
 * </pre>
 * <p>
 * <b>DTO Aggregation:</b> The {@link #getConversionDto(FrontendResource)} method embeds a list of
 * {@link ControllerEndpointConversionDto} objects within the {@link FrontendResourceConversionDto}, providing
 * a complete representation of the resource and its endpoints for YAML serialization.
 * </p>
 * <p>
 * <b>Dependencies:</b> This converter requires two injected components:
 * </p>
 * <ul>
 *   <li>{@link ControllerEndpointEntityToYamlConverter} - delegates controller endpoint packaging</li>
 *   <li>{@link ControllerEndpointRepository} - queries endpoints by frontend resource ID</li>
 * </ul>
 * <p>
 * <b>Thread Safety:</b> This class is a stateless Spring {@code @Component} and is safe for concurrent use
 * by multiple threads.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractEntityToYamlConverter
 * @see FrontendResource
 * @see FrontendResourceConversionDto
 * @see ControllerEndpointEntityToYamlConverter
 * @see ControllerEndpointRepository
 */
@Component
public class FrontendResourceEntityToYamlConverter extends AbstractEntityToYamlConverter<FrontendResource, FrontendResourceConversionDto>{



    @Inject
    private ControllerEndpointEntityToYamlConverter controllerEndpointEntityToYamlConverter;
    @Inject
    public ControllerEndpointRepository controllerEndpointRepository;

    /**
     * Packages the frontend resource and all associated controller endpoints into the zip output stream.
     * <p>
     * This method overrides the base implementation to provide cascading export behavior. The algorithm:
     * </p>
     * <ol>
     *   <li>Query {@link ControllerEndpointRepository#findByFrontendResourceId(Long)} to retrieve all controller endpoints
     *       linked to this frontend resource</li>
     *   <li>For each controller endpoint, delegate packaging to {@link ControllerEndpointEntityToYamlConverter#addToZip(ControllerEndpoint, ZipOutputStream, Set)}</li>
     *   <li>Call {@code super.addToZip(entity, zipOut, zipEntries)} to package the frontend resource itself</li>
     * </ol>
     * <p>
     * This ensures that all controller endpoints are exported alongside their frontend resource in a single operation,
     * maintaining referential integrity in the export package.
     * </p>
     *
     * @param entity the {@link FrontendResource} entity to export (must not be null)
     * @param zipOut the {@link ZipOutputStream} to write zip entries to (must not be null, managed by caller)
     * @param zipEntries mutable {@link Set} tracking already-added zip entry paths to prevent duplicates (must not be null)
     * @return {@link FrontendResourceConversionDto} for the exported frontend resource
     * @throws NullPointerException if any parameter is null
     * @throws RuntimeException if zip I/O fails during packaging
     * @see ControllerEndpointEntityToYamlConverter#addToZip(ControllerEndpoint, ZipOutputStream, Set)
     */
    @Override
    public FrontendResourceConversionDto addToZip(FrontendResource entity, ZipOutputStream zipOut, Set<String> zipEntries){
         List<ControllerEndpoint> controllerEndpoints = controllerEndpointRepository.findByFrontendResourceId(entity.getId());
         for(ControllerEndpoint ce : controllerEndpoints){
             controllerEndpointEntityToYamlConverter.addToZip(ce, zipOut, zipEntries);
         }
         return super.addToZip(entity, zipOut, zipEntries);

    }

    /**
     * Constructs the absolute file path for the frontend resource content file (HTML, JavaScript, or CSS).
     * <p>
     * The path pattern is: {@code EXPORT_RESOURCES_PATH_ + exportPath + name + type.extension}
     * </p>
     * <p>
     * The {@code exportPath} component is computed based on:
     * </p>
     * <ul>
     *   <li>Resource type: {@code UI_COMPONENT_} or {@code FRONTEND_RESOURCE_}</li>
     *   <li>Access level path: from {@code entity.getAccessLevel().getPath()}</li>
     *   <li>Organization path: {@code SUBDIR_ORGANIZATION_PREFIX + organizationId + "/"} if tenant-scoped, empty string if global</li>
     * </ul>
     * <p>
     * The file extension is determined by {@code entity.getType().getExtension()}, which returns values
     * like {@code .html}, {@code .js}, or {@code .css} based on the resource type.
     * </p>
     *
     * @param entity the {@link FrontendResource} entity to export (must not be null)
     * @return absolute filesystem path to the content file with appropriate extension
     * @throws NullPointerException if entity is null
     * @see #getExportPath(FrontendResource)
     */
    @Override
    public String getPathToContentFile(FrontendResource entity) {
        return EXPORT_RESOURCES_PATH_ + getExportPath(entity) + entity.getName() + entity.getType().getExtension();
    }

    /**
     * Retrieves the content from the frontend resource entity (HTML, JavaScript, or CSS source code).
     *
     * @param entity the {@link FrontendResource} entity (must not be null)
     * @return content string from {@code entity.getContent()}, may be null if no content is defined
     * @throws NullPointerException if entity is null
     */
    @Override
    public String getContent(FrontendResource entity) {
        return entity.getContent();
    }

    /**
     * Constructs the absolute file path for the frontend resource YAML configuration file.
     * <p>
     * The path pattern is: {@code EXPORT_CONFIG_PATH_ + exportPath + name + ".yaml"}
     * </p>
     * <p>
     * The YAML file contains metadata about the frontend resource including access level, required privileges,
     * resource type, and references to associated controller endpoints.
     * </p>
     *
     * @param entity the {@link FrontendResource} entity to export (must not be null)
     * @return absolute filesystem path to the YAML configuration file
     * @throws NullPointerException if entity is null
     * @see #getExportPath(FrontendResource)
     */
    @Override
    public String getPathToYamlComponentFile(FrontendResource entity) {
        return EXPORT_CONFIG_PATH_ + getExportPath(entity) + entity.getName() + ".yaml";
    }

    /**
     * Creates a {@link FrontendResourceConversionDto} with embedded controller endpoint DTOs for YAML serialization.
     * <p>
     * <b>Critical Aggregation Logic:</b> This method performs the following steps:
     * </p>
     * <ol>
     *   <li>Calls {@link #populateDto(FrontendResource)} to create a DTO with basic frontend resource fields</li>
     *   <li>Queries {@link ControllerEndpointRepository#findByFrontendResourceId(Long)} to retrieve all associated endpoints</li>
     *   <li>Maps each {@link ControllerEndpoint} to {@link ControllerEndpointConversionDto} via
     *       {@link ControllerEndpointEntityToYamlConverter#getConversionDto(ControllerEndpoint)}</li>
     *   <li>Sets the list of controller endpoint DTOs on the frontend resource DTO</li>
     * </ol>
     * <p>
     * The populated DTO includes these fields from {@link #populateDto(FrontendResource)}:
     * </p>
     * <ul>
     *   <li>{@code content} - relative resource path to content file (via {@link #getResourcePathToContentFile(FrontendResource)})</li>
     *   <li>{@code includeInSitemap} - sitemap inclusion flag</li>
     *   <li>{@code name} - resource name</li>
     *   <li>{@code accessLevel} - access level enum</li>
     *   <li>{@code requiredPrivilege} - required privilege string</li>
     *   <li>{@code type} - resource type enum name (HTML, JS, CSS)</li>
     *   <li>{@code resourceType} - UI_COMPONENT or FRONTEND_RESOURCE enum name</li>
     *   <li>{@code module} - module name</li>
     *   <li>{@code organizationId} - tenant organization ID</li>
     *   <li>{@code embeddable} - embeddable flag</li>
     * </ul>
     *
     * @param entity the {@link FrontendResource} entity to convert (must not be null)
     * @return populated {@link FrontendResourceConversionDto} with embedded controller endpoint list
     * @throws NullPointerException if entity is null
     * @see #populateDto(FrontendResource)
     */
    @Override
    public FrontendResourceConversionDto getConversionDto(FrontendResource entity) {
        FrontendResourceConversionDto dto = populateDto(entity);
        List<ControllerEndpointConversionDto> controllerEndpointDtos = controllerEndpointRepository.findByFrontendResourceId(entity.getId()).stream()
                .map(controllerEndpoint -> controllerEndpointEntityToYamlConverter.getConversionDto(controllerEndpoint))
                .collect(Collectors.toList());
        dto.setControllerEndpoints(controllerEndpointDtos);
        return dto;
    }

    /**
     * Computes the export path prefix based on resource type, access level, and organization.
     * <p>
     * The path is constructed from three components:
     * </p>
     * <ol>
     *   <li><b>Resource type prefix:</b> {@code UI_COMPONENT_} if {@code resourceType == UI_COMPONENT}, 
     *       otherwise {@code FRONTEND_RESOURCE_}</li>
     *   <li><b>Access level path:</b> from {@code accessLevel.getPath()}</li>
     *   <li><b>Organization path:</b> empty string if {@code organizationId} is null (global resource),
     *       otherwise {@code SUBDIR_ORGANIZATION_PREFIX + organizationId + "/"} for tenant-scoped resources</li>
     * </ol>
     *
     * @param entity the {@link FrontendResource} entity (must not be null)
     * @return export path prefix string used in content and YAML file path construction
     * @throws NullPointerException if entity is null
     */
    private String getExportPath(FrontendResource entity){
        String orgPath = entity.getOrganizationId() == null ? "" : SUBDIR_ORGANIZATION_PREFIX + entity.getOrganizationId() + "/";
        return (entity.getResourceType().equals(FrontendResource.ResourceType.UI_COMPONENT) ? UI_COMPONENT_ : FRONTEND_RESOURCE_) + entity.getAccessLevel().getPath() + orgPath;
    }

    /**
     * Maps {@link FrontendResource} entity fields to {@link FrontendResourceConversionDto} (without controller endpoints).
     * <p>
     * This method populates the basic fields of the DTO. The controller endpoints list is not set here;
     * it is populated separately by {@link #getConversionDto(FrontendResource)}.
     * </p>
     * <p>
     * Field mappings:
     * </p>
     * <ul>
     *   <li>{@code content} - relative resource path via {@link #getResourcePathToContentFile(FrontendResource)}</li>
     *   <li>{@code includeInSitemap} - from {@code entity.getIncludeInSitemap()}</li>
     *   <li>{@code name} - from {@code entity.getName()}</li>
     *   <li>{@code accessLevel} - from {@code entity.getAccessLevel()}</li>
     *   <li>{@code requiredPrivilege} - from {@code entity.getRequiredPrivilege()}</li>
     *   <li>{@code type} - enum name from {@code entity.getType().name()}</li>
     *   <li>{@code resourceType} - enum name from {@code entity.getResourceType().name()}</li>
     *   <li>{@code module} - from {@code entity.getModuleName()}</li>
     *   <li>{@code organizationId} - from {@code entity.getOrganizationId()}</li>
     *   <li>{@code embeddable} - from {@code entity.isEmbeddable()}</li>
     * </ul>
     *
     * @param entity the {@link FrontendResource} entity to convert (must not be null)
     * @return {@link FrontendResourceConversionDto} with basic fields populated (controllerEndpoints list not set)
     * @throws NullPointerException if entity is null
     */
    private FrontendResourceConversionDto populateDto(FrontendResource entity) {
        FrontendResourceConversionDto dto = new FrontendResourceConversionDto();
        dto.setContent(getResourcePathToContentFile(entity));
        dto.setIncludeInSitemap(entity.getIncludeInSitemap());
        dto.setName(entity.getName());
        dto.setAccessLevel(entity.getAccessLevel());
        dto.setRequiredPrivilege(entity.getRequiredPrivilege());
        dto.setType(entity.getType().name());
        dto.setResourceType(entity.getResourceType().name());
        dto.setModule(entity.getModuleName());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setEmbeddable(entity.isEmbeddable());
        return dto;
    }
}
