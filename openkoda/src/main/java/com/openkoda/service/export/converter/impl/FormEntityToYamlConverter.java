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
import com.openkoda.model.component.Form;
import com.openkoda.service.export.dto.FormConversionDto;
import org.springframework.stereotype.Component;

import static com.openkoda.service.export.FolderPathConstants.*;

/**
 * Converts Form entities to YAML configuration and JavaScript code file pairs for export.
 * <p>
 * This converter extends {@link AbstractEntityToYamlConverter} to handle the export of Form entities
 * into two separate files: a YAML configuration file containing form metadata and a JavaScript file
 * containing the form's code implementation. The converter manages organization-specific paths
 * for tenant-scoped forms, ensuring proper isolation of exported form definitions.
 * 
 * <p>
 * The JavaScript content file path follows the pattern: {@code EXPORT_CODE_PATH_ + FORM_ + orgPath + "{name}.js"},
 * where orgPath includes the organization ID for tenant-scoped forms or remains empty for global forms.
 * The YAML configuration path is constructed via {@code getYamlDefaultFilePath} using the pattern:
 * {@code EXPORT_CONFIG_PATH_ + FORM_ + filename}.
 * 
 * <p>
 * Organization-specific behavior: When organizationId is present in the Form entity, it is included
 * in both the JavaScript file path (as a directory prefix) and the YAML filename (as a suffix),
 * enabling proper tenant isolation during export and import operations.
 * 
 * <p>
 * This component implements {@link LoggingComponent} for debug logging capabilities during the
 * conversion process.
 * 
 * <p>
 * Thread-safety: This is a stateless Spring {@code @Component} that is safe for concurrent use
 * by multiple threads.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractEntityToYamlConverter
 * @see Form
 * @see FormConversionDto
 * @see com.openkoda.service.export.FolderPathConstants
 * @see LoggingComponent
 */
@Component
public class FormEntityToYamlConverter extends AbstractEntityToYamlConverter<Form, FormConversionDto> implements LoggingComponent   {

    /**
     * Constructs the file path for the JavaScript form definition code file.
     * <p>
     * The path follows the pattern: {@code EXPORT_CODE_PATH_ + FORM_ + orgPath + "{name}.js"},
     * where the filename is formatted as {@code "{formName}.js"}.
     * 
     * <p>
     * Organization path logic: If the organizationId is null, the form is treated as global
     * (no organization prefix). Otherwise, the path includes {@code SUBDIR_ORGANIZATION_PREFIX + organizationId + "/"}
     * to ensure tenant-scoped isolation of exported form code.
     * 
     *
     * @param entity the Form entity to export (must not be null)
     * @return absolute path to .js content file containing form definition code
     * @throws NullPointerException if entity is null or entity.getName() is null
     * @see com.openkoda.service.export.FolderPathConstants#EXPORT_CODE_PATH_
     * @see com.openkoda.service.export.FolderPathConstants#FORM_
     * @see com.openkoda.service.export.FolderPathConstants#SUBDIR_ORGANIZATION_PREFIX
     */
    public String getPathToContentFile(Form entity){
        String orgPath = entity.getOrganizationId() == null ? "" : SUBDIR_ORGANIZATION_PREFIX + entity.getOrganizationId() + "/";

        String entityExportPath = FORM_ + orgPath;
        return EXPORT_CODE_PATH_ + entityExportPath + String.format("%s.js", entity.getName());
    }

    /**
     * Retrieves the JavaScript form definition code from the Form entity.
     * <p>
     * This method returns {@code entity.getCode()}, which contains the form's JavaScript
     * implementation that will be written to the .js content file during export.
     * 
     *
     * @param entity the Form entity (must not be null)
     * @return JavaScript form definition code string, may be null if no code defined
     * @throws NullPointerException if entity is null
     */
    @Override
    public String getContent(Form entity) {
        return entity.getCode();
    }

    /**
     * Creates a FormConversionDto for YAML serialization from the Form entity.
     * <p>
     * This method maps Form entity fields to the DTO used for YAML export. The DTO includes
     * form metadata such as name, privileges, table configuration, and controller registration flags.
     * 
     * <p>
     * DTO field mappings:
     * <ul>
     *   <li>name - form identifier</li>
     *   <li>readPrivilege - converted to string representation via getReadPrivilegeAsString()</li>
     *   <li>writePrivilege - converted to string representation via getWritePrivilegeAsString()</li>
     *   <li>tableColumns - column definitions for table view</li>
     *   <li>filterColumns - column definitions for filtering</li>
     *   <li>tableName - database table name</li>
     *   <li>tableView - view configuration</li>
     *   <li>registerApiCrudController - API controller registration flag</li>
     *   <li>registerHtmlCrudController - HTML controller registration flag</li>
     *   <li>showOnOrganizationDashboard - dashboard visibility flag</li>
     *   <li>code - relative resource path to .js file (not the actual JavaScript content)</li>
     *   <li>module - module name for form organization</li>
     *   <li>organizationId - tenant scope identifier</li>
     * </ul>
     * 
     * <p>
     * Note: The {@code code} field contains the relative resource path to the .js file
     * (via {@code getResourcePathToContentFile}), not the actual JavaScript content.
     * Privilege fields are converted to string representation for YAML serialization.
     * 
     *
     * @param entity the Form entity to convert (must not be null)
     * @return populated FormConversionDto ready for YAML serialization
     * @throws NullPointerException if entity is null
     * @see FormConversionDto
     */
    public FormConversionDto getConversionDto(Form entity){
        FormConversionDto dto = new FormConversionDto();
        dto.setName(entity.getName());
        dto.setReadPrivilege(entity.getReadPrivilegeAsString());
        dto.setTableColumns(entity.getTableColumns());
        dto.setFilterColumns(entity.getFilterColumns());
        dto.setTableName(entity.getTableName());
        dto.setTableView(entity.getTableView());
        dto.setWritePrivilege(entity.getWritePrivilegeAsString());
        dto.setRegisterApiCrudController(entity.isRegisterApiCrudController());
        dto.setRegisterHtmlCrudController(entity.isRegisterHtmlCrudController());
        dto.setShowOnOrganizationDashboard(entity.isShowOnOrganizationDashboard());
        dto.setCode(getResourcePathToContentFile(entity));
        dto.setModule(entity.getModuleName());
        dto.setOrganizationId(entity.getOrganizationId());
        return dto;
    }

    /**
     * Constructs the YAML component file path for form configuration.
     * <p>
     * The path follows the pattern: {@code EXPORT_CONFIG_PATH_ + FORM_ + "{name}-{orgId}.yaml"}
     * via {@code getYamlDefaultFilePath}. The organizationId is included in the filename
     * for tenant-scoped forms, ensuring proper isolation of form configurations.
     * 
     *
     * @param entity the Form entity to export (must not be null)
     * @return absolute path to YAML configuration file
     * @throws NullPointerException if entity is null or entity.getName() is null
     * @see com.openkoda.service.export.FolderPathConstants#EXPORT_CONFIG_PATH_
     * @see com.openkoda.service.export.FolderPathConstants#FORM_
     */
    public String getPathToYamlComponentFile(Form entity){
        return getYamlDefaultFilePath(EXPORT_CONFIG_PATH_ + FORM_, entity.getName(), entity.getOrganizationId());
    }
}
