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
import com.openkoda.repository.SecureFrontendResourceRepository;
import com.openkoda.service.export.dto.ControllerEndpointConversionDto;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import static com.openkoda.service.export.FolderPathConstants.*;

/**
 * Converts ControllerEndpoint entities to YAML/JavaScript file pairs for export.
 * <p>
 * This converter extends AbstractEntityToYamlConverter and is responsible for exporting ControllerEndpoint
 * entities as JavaScript code files with YAML component metadata. The converter constructs JavaScript file
 * paths using FolderPathConstants (UI_COMPONENT_, EXPORT_CODE_PATH_) combined with frontend resource
 * metadata retrieved from SecureFrontendResourceRepository.

 * <p>
 * Key behaviors:
 * <ul>
 *   <li>JavaScript content file path: EXPORT_CODE_PATH_ + UI_COMPONENT_ + accessLevel + orgPath + filename</li>
 *   <li>Filename format: "{frontendResourceName}-{httpMethod}-{subPath}.js"</li>
 *   <li>YAML component file: null (no separate YAML component generated for controller endpoints)</li>
 *   <li>Organization handling: null organizationId = global, non-null = tenant-scoped path</li>
 * </ul>

 * <p>
 * Thread-safety: This is a stateless Spring @Component, safe for concurrent use by multiple threads.

 * <p>
 * WARNING: This converter lacks defensive null checks on frontend resource lookup. If frontendResourceId
 * is invalid or the resource does not exist, the converter may throw NullPointerException during path
 * construction.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractEntityToYamlConverter
 * @see ControllerEndpoint
 * @see ControllerEndpointConversionDto
 * @see SecureFrontendResourceRepository
 * @see com.openkoda.service.export.FolderPathConstants
 */
@Component
public class ControllerEndpointEntityToYamlConverter extends AbstractEntityToYamlConverter<ControllerEndpoint, ControllerEndpointConversionDto> {


    @Inject
    public SecureFrontendResourceRepository frontendResourceRepository;

    /**
     * Constructs the file path for the JavaScript controller endpoint code content file.
     * <p>
     * This method builds the absolute filesystem path where the JavaScript code for this controller endpoint
     * will be exported. The path pattern follows:
     * EXPORT_CODE_PATH_ + UI_COMPONENT_ + accessLevel + orgPath + "{frontendResourceName}-{httpMethod}-{subPath}.js"

     * <p>
     * Path components:
     * <ul>
     *   <li>EXPORT_CODE_PATH_: Base code export directory from FolderPathConstants</li>
     *   <li>UI_COMPONENT_: UI component subdirectory prefix</li>
     *   <li>accessLevel: Access level path from associated FrontendResource (e.g., "public/", "private/")</li>
     *   <li>orgPath: Organization-specific path (empty for global, "organization_{id}/" for tenant-scoped)</li>
     *   <li>Filename: "{frontendResourceName}-{httpMethod}-{subPath}.js"</li>
     * </ul>

     * <p>
     * Organization handling: If organizationId is null, the endpoint is global and no organization path
     * prefix is added. If organizationId is non-null, the path includes SUBDIR_ORGANIZATION_PREFIX + organizationId + "/".

     *
     * @param entity the ControllerEndpoint entity to export (must not be null)
     * @return absolute filesystem path to the .js content file
     * @throws NullPointerException if entity is null or frontendResourceId lookup fails
     * @see com.openkoda.service.export.FolderPathConstants
     */
    @Override
    public String getPathToContentFile(ControllerEndpoint entity) {
        FrontendResource frontendResource = frontendResourceRepository.findOne(entity.getFrontendResourceId());
        String orgPath = entity.getOrganizationId() == null ? "" : SUBDIR_ORGANIZATION_PREFIX + entity.getOrganizationId() + "/";
        String entityExportPath = UI_COMPONENT_ + frontendResource.getAccessLevel().getPath() + orgPath;
        return EXPORT_CODE_PATH_ + entityExportPath
                + String.format("%s-%s-%s.js", frontendResource.getName(), entity.getHttpMethod().name(), entity.getSubPath());
    }

    /**
     * Retrieves the JavaScript code content from the ControllerEndpoint entity.
     * <p>
     * This method extracts the JavaScript implementation code stored in the entity. The code typically
     * contains controller logic that handles HTTP requests for this endpoint.

     *
     * @param entity the ControllerEndpoint entity (must not be null)
     * @return JavaScript code string from entity.getCode(), may be null if no code is defined
     * @throws NullPointerException if entity is null
     */
    @Override
    public String getContent(ControllerEndpoint entity) {
        return entity.getCode();
    }

    /**
     * Returns the YAML component file path for this ControllerEndpoint (intentionally null).
     * <p>
     * ControllerEndpoint entities export only JavaScript code files and do not generate separate YAML
     * component files. The endpoint metadata is embedded within the parent FrontendResource's YAML
     * configuration instead.

     *
     * @param entity the ControllerEndpoint entity (unused)
     * @return always null - no YAML component file is generated for controller endpoints
     */
    @Override
    public String getPathToYamlComponentFile(ControllerEndpoint entity) {
        return null;
    }

    /**
     * Creates a ControllerEndpointConversionDto for YAML serialization.
     * <p>
     * This method converts the ControllerEndpoint entity into a DTO that will be serialized to YAML format
     * during the export process. The DTO contains all endpoint metadata required for reimporting the
     * controller endpoint.

     * <p>
     * DTO field mappings:
     * <ul>
     *   <li>code: Relative resource path to JavaScript file (via getResourcePathToContentFile, not absolute filesystem path)</li>
     *   <li>httpHeaders: HTTP headers configuration</li>
     *   <li>httpMethod: HTTP method as enum name string (GET, POST, PUT, DELETE, etc.)</li>
     *   <li>modelAttributes: Model attributes configuration</li>
     *   <li>subpath: Endpoint subpath (URL path component)</li>
     *   <li>responseType: Response type as enum name string</li>
     *   <li>module: Module name from entity.getModuleName()</li>
     *   <li>organizationId: Tenant organization ID (null for global endpoints)</li>
     * </ul>

     * <p>
     * Note: The code field contains a relative resource path (with EXPORT_PATH prefix removed), not the
     * actual JavaScript content. This makes the export portable across different deployment environments.

     *
     * @param entity the ControllerEndpoint entity to convert (must not be null)
     * @return populated ControllerEndpointConversionDto ready for YAML serialization
     * @throws NullPointerException if entity is null
     * @see ControllerEndpointConversionDto
     */
    @Override
    public ControllerEndpointConversionDto getConversionDto(ControllerEndpoint entity) {
        ControllerEndpointConversionDto dto = new ControllerEndpointConversionDto();
        dto.setCode(getResourcePathToContentFile(entity));
        dto.setHttpHeaders(entity.getHttpHeaders());
        dto.setHttpMethod(entity.getHttpMethod().name());
        dto.setModelAttributes(entity.getModelAttributes());
        dto.setSubpath(entity.getSubPath());
        dto.setResponseType(entity.getResponseType().name());
        dto.setModule(entity.getModuleName());
        dto.setOrganizationId(entity.getOrganizationId());
        return dto;
    }
}
