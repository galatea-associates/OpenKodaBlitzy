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
import com.openkoda.model.component.Form;
import com.openkoda.service.export.converter.YamlToEntityConverter;
import com.openkoda.service.export.converter.YamlToEntityParentConverter;
import com.openkoda.service.export.dto.FormConversionDto;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Converts FormConversionDto instances to Form entities for import operations.
 * <p>
 * This converter implements the YamlToEntityConverter interface to enable import of Form entities
 * from YAML configuration files. Forms represent dynamic data model definitions with associated
 * JavaScript code that define database tables, CRUD operations, and user interface components.
 * </p>
 * <p>
 * The converter performs the following critical operations:
 * <ul>
 *   <li>Lookup-or-create semantics: finds existing form by name, creates new if not found</li>
 *   <li>Dynamic database table creation via services.dynamicEntity.createDynamicTableIfNotExists BEFORE saving form</li>
 *   <li>Privilege string parsing: converts string privilege names to enum values via PrivilegeHelper</li>
 *   <li>JavaScript code loading from either filesystem or in-memory resources Map</li>
 * </ul>
 * </p>
 * <p>
 * The annotation @YamlToEntityParentConverter(dtoClass = FormConversionDto.class) enables
 * auto-discovery by the export/import subsystem, allowing this converter to be automatically
 * registered and invoked when importing Form entities.
 * </p>
 * <p>
 * <strong>Dynamic Table Creation:</strong> This converter enables runtime schema evolution
 * by ensuring the dynamic database table exists before persisting the form entity. This
 * allows the application to create new entity types at runtime without requiring database
 * migrations or application restarts.
 * </p>
 * <p>
 * Dependencies: Requires ComponentProvider for access to repositories.secure.form,
 * repositories.unsecure.form, and services.dynamicEntity.
 * </p>
 * <p>
 * Thread-safety: This converter is a stateless Spring @Component and is safe for concurrent use.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see YamlToEntityConverter
 * @see YamlToEntityParentConverter
 * @see Form
 * @see FormConversionDto
 * @see ComponentProvider
 * @see PrivilegeHelper
 */
@Component
@YamlToEntityParentConverter(dtoClass = FormConversionDto.class)
public class FormYamlToEntityConverter extends ComponentProvider implements YamlToEntityConverter<Form, FormConversionDto> {

    /**
     * Converts a FormConversionDto to a Form entity using filesystem I/O, creates the dynamic database table, and persists the entity.
     * <p>
     * This method performs the following operations in sequence:
     * <ol>
     *   <li>Calls getForm with the DTO and JavaScript code loaded from filesystem via loadResourceAsString</li>
     *   <li>Ensures the dynamic database table exists by calling services.dynamicEntity.createDynamicTableIfNotExists</li>
     *   <li>Persists the form entity via repositories.secure.form.saveOne</li>
     * </ol>
     * </p>
     * <p>
     * <strong>CRITICAL:</strong> Dynamic table creation happens BEFORE form persistence to ensure
     * the database schema is ready to support the form's defined entity structure. This enables
     * runtime schema evolution without database migrations.
     * </p>
     * <p>
     * The JavaScript code is loaded from the filesystem using the path specified in dto.getCode().
     * If the code file cannot be read, a RuntimeException will be thrown.
     * </p>
     *
     * @param dto the FormConversionDto containing form metadata (must not be null, dto.getCode() must be a valid path)
     * @param filePath the YAML file path (unused but required by interface)
     * @return the saved Form entity with generated ID
     * @throws NullPointerException if dto is null
     * @throws RuntimeException if file I/O fails during loadResourceAsString or dynamic table creation fails
     * @see #getForm(FormConversionDto, String)
     */
    @Override
    public Form convertAndSave(FormConversionDto dto, String filePath) {
        debug("[convertAndSave]");
        Form form = getForm(dto, loadResourceAsString(dto.getCode()));
        services.dynamicEntity.createDynamicTableIfNotExists(form.getTableName());
        return repositories.secure.form.saveOne(form);
    }

    /**
     * Converts a FormConversionDto to a Form entity using an in-memory resources Map, creates the dynamic database table, and persists the entity.
     * <p>
     * This method performs the same operations as convertAndSave(dto, filePath) but avoids filesystem I/O
     * by retrieving the JavaScript code from the provided resources Map. This is useful for batch imports
     * or testing scenarios where content is already loaded in memory.
     * </p>
     * <p>
     * Operation sequence:
     * <ol>
     *   <li>Calls getForm with the DTO and JavaScript code retrieved from resources Map using dto.getCode() as key</li>
     *   <li>Ensures the dynamic database table exists by calling services.dynamicEntity.createDynamicTableIfNotExists</li>
     *   <li>Persists the form entity via repositories.secure.form.saveOne</li>
     * </ol>
     * </p>
     * <p>
     * <strong>CRITICAL:</strong> Dynamic table creation happens BEFORE form persistence to ensure
     * the database schema is ready to support the form's defined entity structure.
     * </p>
     *
     * @param dto the FormConversionDto containing form metadata (must not be null)
     * @param filePath the YAML file path (unused but required by interface)
     * @param resources Map of resource paths to content strings for in-memory code lookup (must not be null)
     * @return the saved Form entity with generated ID
     * @throws NullPointerException if dto or resources is null, or if dto.getCode() key is not present in resources Map
     * @throws RuntimeException if dynamic table creation fails
     * @see #getForm(FormConversionDto, String)
     */
    @Override
    public Form convertAndSave(FormConversionDto dto, String filePath, Map<String, String> resources) {
        debug("[convertAndSave]");
        Form form = getForm(dto, resources.get(dto.getCode()));
        services.dynamicEntity.createDynamicTableIfNotExists(form.getTableName());
        form = repositories.secure.form.saveOne(form);
        return form;
    }

    /**
     * Lookup-or-create helper that finds an existing Form by name or instantiates a new one, then populates all fields from the DTO.
     * <p>
     * This method implements lookup-or-create semantics:
     * <ul>
     *   <li>Queries repositories.unsecure.form.findByName using dto.getName() as the lookup key</li>
     *   <li>If a form is found, it is updated with values from the DTO</li>
     *   <li>If no form is found, a new Form instance is created and populated</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Field Mappings:</strong>
     * <ul>
     *   <li>name: Lookup key for finding existing forms</li>
     *   <li>readPrivilege: Parsed from string using PrivilegeHelper.valueOfString, null if blank</li>
     *   <li>writePrivilege: Parsed from string using PrivilegeHelper.valueOfString, null if blank</li>
     *   <li>registerApiCrudController: Controls API endpoint generation</li>
     *   <li>registerHtmlCrudController: Controls HTML UI generation</li>
     *   <li>showOnOrganizationDashboard: Controls dashboard visibility</li>
     *   <li>tableColumns: Comma-separated list of table columns</li>
     *   <li>filterColumns: Comma-separated list of filter columns</li>
     *   <li>moduleName: Module owning this form</li>
     *   <li>tableName: Database table name for dynamic entity</li>
     *   <li>organizationId: Tenant scope (null for global forms)</li>
     *   <li>tableView: Database view name if applicable</li>
     *   <li>code: JavaScript form definition from the code parameter</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Privilege Parsing:</strong> Privilege strings are parsed using the PrivilegeHelper singleton.
     * If the privilege string is blank (null, empty, or whitespace), the privilege field is set to null.
     * If the privilege string contains an invalid privilege name, an IllegalArgumentException is thrown.
     * </p>
     * <p>
     * This method always returns a non-null entity, either an existing instance or a newly created one.
     * </p>
     *
     * @param dto the FormConversionDto containing form metadata (must not be null)
     * @param code the JavaScript form definition code content (may be null)
     * @return existing Form if found by name, or new instance with fields populated from DTO and code parameter
     * @throws NullPointerException if dto is null
     * @throws IllegalArgumentException if privilege strings contain invalid privilege names
     * @see PrivilegeHelper#valueOfString(String)
     */
    @NotNull
    private Form getForm(FormConversionDto dto, String code) {
        Form form = repositories.unsecure.form.findByName(dto.getName());
        if(form == null) {
            form = new Form();
            form.setName(dto.getName());
        }
        form.setReadPrivilege(isNotBlank(dto.getReadPrivilege()) ? PrivilegeHelper.getInstance().valueOfString(dto.getReadPrivilege()) : null);
        form.setWritePrivilege(isNotBlank(dto.getWritePrivilege()) ? PrivilegeHelper.getInstance().valueOfString(dto.getWritePrivilege()) : null);
        form.setRegisterApiCrudController(dto.isRegisterApiCrudController());
        form.setRegisterHtmlCrudController(dto.isRegisterHtmlCrudController());
        form.setShowOnOrganizationDashboard(dto.isShowOnOrganizationDashboard());
        form.setTableColumns(dto.getTableColumns());
        form.setFilterColumns(dto.getFilterColumns());
        form.setModuleName(dto.getModule());
        form.setTableName(dto.getTableName());
        form.setOrganizationId(dto.getOrganizationId());
        form.setTableView(dto.getTableView());
        form.setCode(code);
        return form;
    }
}
