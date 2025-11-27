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
import com.openkoda.model.DynamicPrivilege;
import com.openkoda.service.export.dto.PrivilegeConversionDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import static com.openkoda.service.export.FolderPathConstants.*;

/**
 * Converter for exporting DynamicPrivilege entities to YAML format with SQL upgrade script generation.
 * <p>
 * This converter extends {@link AbstractEntityToYamlConverter} to handle the export of dynamic privilege
 * configurations as YAML files. Unlike other entity converters, this implementation produces YAML-only
 * output without separate content files, as privileges are metadata-only entities.

 * <p>
 * Key behaviors:
 * <ul>
 *   <li>YAML Export: Generates YAML component files at EXPORT_PRIVILEGE_PATH_ with privilege metadata</li>
 *   <li>No Content Files: {@link #getContent(DynamicPrivilege)} returns null - privileges have no separate content</li>
 *   <li>SQL Generation: {@link #getUpgradeScript(DynamicPrivilege, List)} appends INSERT statements to database
 *       upgrade scripts for recreating privileges in target environments</li>
 *   <li>Global Scope: Privileges are not tenant-scoped; organizationId is always null in export paths</li>
 * </ul>

 * <p>
 * SQL INSERT Format:
 * <pre>
 * INSERT INTO public.dynamic_privilege (id, category, privilege_group, index_string, label, name, removable, updated_on)
 * VALUES (nextval('seq_global_id'), category, group, indexString, label, name, true, now());
 * </pre>
 * The generated SQL uses sequence generation for IDs and hardcodes removable=true with current timestamp.

 * <p>
 * This component implements {@link LoggingComponent} for debug logging during export operations.
 * The class is stateless and safe for concurrent use as a Spring singleton.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractEntityToYamlConverter
 * @see DynamicPrivilege
 * @see PrivilegeConversionDto
 * @see LoggingComponent
 */
@Component
public class PrivilegeEntityToYamlConverter extends AbstractEntityToYamlConverter<DynamicPrivilege, PrivilegeConversionDto> implements LoggingComponent   {

    /**
     * Returns the path to the content file for the privilege entity.
     * <p>
     * Note: This method returns a non-null path but the actual content is null (see {@link #getContent(DynamicPrivilege)}).
     * This appears to be a vestigial implementation as privilege exports only produce YAML component files without
     * separate content files. The path pattern used here may not match the actual export behavior.

     *
     * @param entity the DynamicPrivilege entity to get the content path for (must not be null)
     * @return file path string in format EXPORT_CODE_PATH_ + FORM_ + "{name}.yaml" (unused due to null content)
     * @throws NullPointerException if entity is null or entity.getName() is null
     */
    public String getPathToContentFile(DynamicPrivilege entity){
        return EXPORT_CODE_PATH_ + FORM_ + String.format("%s.yaml", entity.getName());
    }

    /**
     * Returns the content for the privilege entity (always null).
     * <p>
     * DynamicPrivilege entities export only YAML metadata configuration files and do not have separate
     * content files. This design reflects that privileges are metadata-only entities consisting of
     * category, group, label, and name - they have no executable content or form definitions like
     * other exportable entities.

     *
     * @param entity the DynamicPrivilege entity (unused parameter)
     * @return always null - no content files are generated for privilege entities
     */
    @Override
    public String getContent(DynamicPrivilege entity) {
        return null;
    }

    /**
     * Generates SQL INSERT statement for recreating the privilege in a target database.
     * <p>
     * This method produces a database migration script entry that can be included in upgrade SQL files.
     * The generated INSERT statement creates a new dynamic_privilege row with all necessary fields populated
     * from the source entity.

     * <p>
     * SQL Template:
     * <pre>
     * INSERT INTO public.dynamic_privilege (id, category, privilege_group, index_string, label, name, removable, updated_on)
     * VALUES (nextval('seq_global_id'), '{category}', '{group}', '{indexString}', '{label}', '{name}', true, now());
     * </pre>

     * <p>
     * Key characteristics:
     * <ul>
     *   <li>ID Generation: Uses nextval('seq_global_id') for new sequence-based ID</li>
     *   <li>Removable: Hardcoded to true (all dynamically created privileges are removable)</li>
     *   <li>Timestamp: Uses now() for updated_on field to reflect import time</li>
     *   <li>Defensive: If dbUpgradeEntries is null, no action is taken (silent no-op)</li>
     * </ul>

     *
     * @param entity the DynamicPrivilege entity to generate SQL for (must not be null when dbUpgradeEntries is non-null)
     * @param dbUpgradeEntries mutable List to append the SQL INSERT statement to; may be null (no-op if null)
     * @throws NullPointerException if entity is null and dbUpgradeEntries is non-null (during entity field access)
     */
    @Override
    public void getUpgradeScript(DynamicPrivilege entity, List<String> dbUpgradeEntries) {
        if(dbUpgradeEntries != null) {
            dbUpgradeEntries.add("INSERT INTO public.dynamic_privilege (id,category,privilege_group,index_string,\"label\",\"name\",removable,updated_on) VALUES\n"
            + String.format("(nextval('seq_global_id'),'%s', '%s' ,'%s','%s','%s',true,now());"
            , entity.getCategory(), entity.getGroup().name(), entity.getIndexString(), entity.getLabel(), entity.getName()));
        }
    }
    
    /**
     * Creates a PrivilegeConversionDto for YAML serialization of the privilege entity.
     * <p>
     * This method transforms the DynamicPrivilege entity into a simplified DTO containing only the fields
     * necessary for YAML export. The DTO excludes internal database fields like id, indexString, removable,
     * and updated_on, focusing on the logical privilege definition.

     * <p>
     * DTO Field Mappings:
     * <ul>
     *   <li>name: Direct copy from entity.getName()</li>
     *   <li>category: Direct copy from entity.getCategory()</li>
     *   <li>group: Enum name string from entity.getGroup().name()</li>
     *   <li>label: Direct copy from entity.getLabel()</li>
     * </ul>

     *
     * @param entity the DynamicPrivilege entity to convert (must not be null)
     * @return populated PrivilegeConversionDto ready for YAML serialization with name, category, group, and label
     * @throws NullPointerException if entity is null or any required entity field is null
     * @see PrivilegeConversionDto
     */
    public PrivilegeConversionDto getConversionDto(DynamicPrivilege entity){
        PrivilegeConversionDto dto = new PrivilegeConversionDto();
        dto.setName(entity.getName());
        dto.setCategory(entity.getCategory());
        dto.setGroup(entity.getGroup().name());
        dto.setLabel(entity.getLabel());
        return dto;
    }

    /**
     * Constructs the YAML component file path for exporting the privilege configuration.
     * <p>
     * This method generates the file path where the privilege's YAML metadata will be written during export.
     * The path uses the standard privilege export directory with a filename based on the privilege name.

     * <p>
     * Path Pattern: EXPORT_PRIVILEGE_PATH_ + "{name}.yaml"

     * <p>
     * Note: The organizationId parameter passed to getYamlDefaultFilePath is always null because privileges
     * are global entities, not tenant-scoped. All privileges apply across all organizations in the system.

     *
     * @param entity the DynamicPrivilege entity to export (must not be null)
     * @return absolute path to the YAML configuration file for this privilege
     * @throws NullPointerException if entity is null or entity.getName() is null
     * @see com.openkoda.service.export.FolderPathConstants#EXPORT_PRIVILEGE_PATH_
     */
    public String getPathToYamlComponentFile(DynamicPrivilege entity){
        return getYamlDefaultFilePath(EXPORT_PRIVILEGE_PATH_, entity.getName(), null);
    }
    
    /**
     * Adds the privilege entity to a ZIP archive for export.
     * <p>
     * This method delegates to the parent class implementation without additional behavior. The override
     * exists for extensibility, allowing future customizations to the privilege export ZIP process if needed.
     * The parent implementation handles:
     * <ul>
     *   <li>Converting the entity to PrivilegeConversionDto via {@link #getConversionDto(DynamicPrivilege)}</li>
     *   <li>Serializing the DTO to YAML format</li>
     *   <li>Writing the YAML to the ZIP archive at the path from {@link #getPathToYamlComponentFile(DynamicPrivilege)}</li>
     *   <li>Tracking added entries in zipEntries to prevent duplicates</li>
     * </ul>

     *
     * @param entity the DynamicPrivilege entity to export (must not be null)
     * @param zipOut the ZipOutputStream to write the YAML entry to (must not be null)
     * @param zipEntries Set of already-added zip entry paths for duplicate prevention (must not be null)
     * @return PrivilegeConversionDto for the exported privilege
     * @throws NullPointerException if any parameter is null
     * @throws RuntimeException if ZIP I/O operations fail during export
     */
    @Override
    public PrivilegeConversionDto addToZip(DynamicPrivilege entity, ZipOutputStream zipOut, Set<String> zipEntries) {
        return super.addToZip(entity, zipOut, zipEntries);
    }
}
