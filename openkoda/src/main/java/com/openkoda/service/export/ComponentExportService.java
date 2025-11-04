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

package com.openkoda.service.export;

import com.openkoda.core.flow.LoggingComponent;
import com.openkoda.core.service.system.DatabaseValidationService;
import com.openkoda.model.DynamicPrivilege;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.common.ComponentEntity;
import com.openkoda.model.component.Form;
import com.openkoda.service.export.converter.EntityToYamlConverterFactory;
import com.openkoda.service.export.util.ZipUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

import static com.openkoda.service.export.FolderPathConstants.*;

/**
 * Serializes OpenKoda component entities to YAML format and packages them into distributable ZIP archives for export and migration purposes.
 * <p>
 * This service orchestrates the complete export workflow by delegating entity-to-YAML conversion to {@link EntityToYamlConverterFactory},
 * aggregating multiple entities and their dependencies, generating database upgrade scripts via {@link DatabaseValidationService},
 * and packaging YAML files and supplemental resources using {@link ZipUtils}. When the configuration property
 * {@code components.export.syncWithFilesystem=true} is set, the service also syncs artifacts to the filesystem.

 * <p>
 * Operations performed by this service are NOT transactional. Callers are responsible for managing transaction boundaries
 * when invoking export operations. The service is stateless and thread-safe when used as a Spring singleton.

 * <p>
 * Example usage:
 * <pre>{@code
 * ComponentExportService service = ...;
 * ByteArrayOutputStream zip = service.exportToZip(List.of(formEntity, privilegeEntity));
 * }</pre>

 *
 * @see EntityToYamlConverterFactory for per-entity conversion logic
 * @see ZipUtils for ZIP entry construction
 * @see DatabaseValidationService for SQL upgrade script generation
 * @see FolderPathConstants for export path constants
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Service
public class ComponentExportService implements LoggingComponent {

    /**
     * Factory that delegates entity-specific YAML serialization to registered converters.
     * <p>
     * This factory maintains a registry of converters for different ComponentEntity types and selects
     * the appropriate converter based on entity class at runtime.

     *
     * @see EntityToYamlConverterFactory
     */
    @Autowired
    private EntityToYamlConverterFactory entityToYamlConverterFactory;

    /**
     * Utility service for creating ZIP entries from content strings, files, and URLs.
     * <p>
     * Provides methods to add various types of content to ZIP archives, including string content,
     * file system files, and resources loaded from URLs.

     *
     * @see ZipUtils
     */
    @Autowired
    ZipUtils zipUtils;

    /**
     * Generates SQL upgrade scripts for database schema changes required by exported components.
     * <p>
     * This service analyzes the current database schema and generates migration scripts that can be
     * packaged with exported components to ensure target environments have the required schema updates.

     *
     * @see DatabaseValidationService
     */
    @Autowired
    DatabaseValidationService databaseValidationService;

    /**
     * Configuration flag controlling whether YAML artifacts are persisted to disk in addition to ZIP packaging.
     * <p>
     * When {@code true}, YAML files are written to filesystem locations specified by {@link FolderPathConstants}.
     * When {@code false}, artifacts exist only in generated ZIP archives. This setting allows for development
     * workflows where component YAML files are maintained in the file system for version control and editing.

     * <p>
     * Configured via application property: {@code components.export.syncWithFilesystem}

     * Default: {@code false}
     */
    @Value("${components.export.syncWithFilesystem:false}")
    private boolean syncWithFilesystem;

    /**
     * Exports a list of component entities into a ZIP archive containing YAML files, supplemental resources, and database upgrade scripts.
     * <p>
     * This method iterates through the provided entities, calling {@link EntityToYamlConverterFactory#exportToZip} for each entity.
     * For {@link Form} entities, it automatically collects {@link DynamicPrivilege} dependencies from the read privilege field.
     * Dependencies are then exported via {@link #addEntityDependencies}, and database migration scripts are appended via
     * {@link #additionalExportFiles}. The method returns a {@link ByteArrayOutputStream} containing the complete ZIP archive.

     * <p>
     * The ZIP archive structure includes:
     * <ul>
     *   <li>Component YAML definitions in appropriate subdirectories</li>
     *   <li>Supplemental files under {@code EXPORT_PATH}</li>
     *   <li>Database upgrade script {@code upgrade.sql} under {@code EXPORT_MIGRATION_PATH_}</li>
     * </ul>

     * <p>
     * Example usage:
     * <pre>{@code
     * ByteArrayOutputStream zip = service.exportToZip(formList);
     * }</pre>

     *
     * @param entities list of {@link ComponentEntity} objects to export (Forms, Privileges, FrontendResources, etc.);
     *                 null-safe but empty list returns empty ZIP
     * @return {@link ByteArrayOutputStream} containing ZIP archive with YAML component definitions and supplemental files; never null
     */
    public ByteArrayOutputStream exportToZip(List<?> entities){
        debug("[exportEntityList]");

        ByteArrayOutputStream zipByteArrayOutputStream = new ByteArrayOutputStream();
        List dependencies = new ArrayList<>();
        Set<String> zipEntries = new HashSet<>();
        try (ZipOutputStream zipOut = new ZipOutputStream(zipByteArrayOutputStream)) {
            for (Object entity : entities) {
                debug("[exportToZip] Adding {}", entity.toString());
                entityToYamlConverterFactory.exportToZip(entity, zipOut, zipEntries);
                if(entity instanceof Form) {
                    Form entityForm = (Form)entity;
                    PrivilegeBase priv = entityForm.getReadPrivilege();
                    if(priv instanceof DynamicPrivilege) {
                        dependencies.add(priv);
                    }
                }
            }
            
            List<String> dbScriptLines = null;
            if (dependencies != null) {
                addEntityDependencies(zipOut, dependencies, dbScriptLines = new ArrayList<>(), zipEntries);
            }
            
            additionalExportFiles(zipOut, dbScriptLines);
            debug("All YAML files added to ZIP successfully.");
        } catch (IOException e) {
            error("[exportEntityList]", e);
        } catch (Exception ee) {
            error("[exportEntityList]", ee);
            throw ee;
        }

        return zipByteArrayOutputStream;
    }
    
    /**
     * Exports entity dependencies (privileges, related components) and appends their SQL upgrade scripts.
     * <p>
     * This method processes dependent entities, typically {@link DynamicPrivilege} instances collected from
     * Form read privileges. It filters duplicates using {@code distinct()} and delegates to
     * {@link EntityToYamlConverterFactory#exportToZip} for each dependency. SQL script lines generated by
     * converters are collected in the {@code dbUpgradeEntries} list for aggregation into the final upgrade script.

     *
     * @param zipOut open {@link ZipOutputStream} to write dependency YAML entries; not closed by this method
     * @param dependencies list of dependent entities (typically {@link DynamicPrivilege} instances); duplicates are filtered
     * @param dbUpgradeEntries mutable list collecting SQL script lines from converters; may be null
     * @param zipEntries set tracking already-added ZIP entry names to prevent duplicates
     */
    private void addEntityDependencies(ZipOutputStream zipOut, List dependencies, List<String> dbUpgradeEntries, Set<String> zipEntries) {
        // TODO Auto-generated method stub
        debug("[addEntityDependencies] Adding entity dependencies {}", dependencies);
        dependencies.stream().distinct().forEach( d -> {
            entityToYamlConverterFactory.exportToZip(d, zipOut, dbUpgradeEntries, zipEntries);
        }); 
    }
    
    /**
     * Conditionally persists component entities to filesystem YAML files based on {@code syncWithFilesystem} configuration.
     * <p>
     * When {@code syncWithFilesystem=true}, this method delegates to {@link EntityToYamlConverterFactory#exportToFile}
     * for each entity, writing YAML representations to the filesystem locations specified by {@link FolderPathConstants}.
     * When the feature is disabled or the entities list is null, the method returns null.

     *
     * @param entities list of {@link ComponentEntity} objects to persist; null-safe
     * @return list of persisted entities with updated file paths if {@code syncWithFilesystem=true}; null if feature disabled or entities null
     */
    public List<ComponentEntity> exportToFileIfRequired(List<ComponentEntity> entities){
        if(syncWithFilesystem && entities != null){
            return entities.stream().map(entityToYamlConverterFactory::exportToFile).toList();
        }
        return null;
    }
    
    /**
     * Conditionally persists a single component entity to filesystem YAML file based on {@code syncWithFilesystem} configuration.
     * <p>
     * When {@code syncWithFilesystem=true}, this method delegates to {@link EntityToYamlConverterFactory#exportToFile}
     * to write the entity's YAML representation to the filesystem. The entity's file path may be updated by the converter.

     *
     * @param entity {@link ComponentEntity} to persist; must not be null if {@code syncWithFilesystem=true}
     * @return the entity unchanged (file path may be updated by converter)
     */
    public ComponentEntity exportToFileIfRequired(ComponentEntity entity){
        if(syncWithFilesystem){
            entityToYamlConverterFactory.exportToFile(entity);
        }
        return entity;
    }
    
    /**
     * Conditionally removes filesystem YAML files for component entities based on {@code syncWithFilesystem} configuration.
     * <p>
     * When {@code syncWithFilesystem=true}, this method delegates to {@link EntityToYamlConverterFactory#removeExportedFiles}
     * for each entity to delete corresponding YAML files from the filesystem. This is typically used during component deletion
     * or when rolling back exports.

     *
     * @param entities list of {@link ComponentEntity} objects whose files should be deleted; null-safe
     * @return list of entities after file removal if {@code syncWithFilesystem=true}; null if feature disabled or entities null
     */
    public List<ComponentEntity> removeExportedFilesIfRequired(List<ComponentEntity> entities){
        if(syncWithFilesystem && entities != null){
            return entities.stream().map(entityToYamlConverterFactory::removeExportedFiles).toList();
        }
        return null;
    }
    
    /**
     * Conditionally removes filesystem YAML file for a single component entity based on {@code syncWithFilesystem} configuration.
     * <p>
     * When {@code syncWithFilesystem=true}, this method delegates to {@link EntityToYamlConverterFactory#removeExportedFiles}
     * to delete the entity's corresponding YAML file from the filesystem.

     *
     * @param entity {@link ComponentEntity} whose file should be deleted
     * @return the entity unchanged
     */
    public ComponentEntity removeExportedFilesIfRequired(ComponentEntity entity){
        if(syncWithFilesystem){
            entityToYamlConverterFactory.removeExportedFiles(entity);
        }
        return entity;
    }
    
    /**
     * Packages supplemental static resources and database upgrade scripts into the export ZIP archive.
     * <p>
     * This method scans the {@code COMPONENTS_ADDITIONAL_FILES_} classpath folder for static resources to include
     * in the export. It handles both filesystem resources (during development) and JAR-packaged resources (in production).
     * Application property files are added with the {@code EXPORT_PATH} prefix, while other files are added at the root level.

     * <p>
     * The method also generates an aggregate {@code upgrade.sql} script by:
     * <ul>
     *   <li>Retrieving base schema update script from {@link DatabaseValidationService#getUpdateScript(boolean)}</li>
     *   <li>Appending converter-provided SQL lines from {@code dbScriptLines} parameter</li>
     *   <li>Writing the complete script to {@code EXPORT_MIGRATION_PATH_/upgrade.sql}</li>
     * </ul>

     * <p>
     * Implementation note: Handles both {@code file://} filesystem and {@code jar:file:} packaged resources via
     * {@link #jarResources(URL)} helper method.

     *
     * @param zos open {@link ZipOutputStream} to write supplemental files; not closed by this method
     * @param dbScriptLines optional list of SQL lines from entity converters to append to upgrade script; may be null
     */
    private void additionalExportFiles(ZipOutputStream zos, List<String> dbScriptLines) {
        debug("[additionalExportFiles]");
        try {
            URL additionalFilesFolder = getClass().getClassLoader().getResource(COMPONENTS_ADDITIONAL_FILES_);
            if (!additionalFilesFolder.toString().startsWith("jar:file:")) {
                try (DirectoryStream<Path> stream = Files
                        .newDirectoryStream(Paths.get(additionalFilesFolder.toURI()))) {
                    for (Path path : stream) {
                        if (!Files.isDirectory(path)) {
                            String fileName = path.getFileName().toString();
                            zipUtils.addURLFileToZip(path.toUri().toURL(),
                                    (fileName.contains("application") ? EXPORT_PATH : "") + fileName, zos);
                        }
                    }
                }
            } else {
                List<String> nestedResources = jarResources(additionalFilesFolder);
                for (String string : nestedResources) {
                    URL nestedUrl = getClass().getClassLoader().getResource(string);
                    String fileName = new File(string).getName();
                    zipUtils.addURLFileToZip(nestedUrl,
                            (fileName.contains("application") ? EXPORT_PATH : "") + fileName, zos);
                }
            }
//          add migration script if exists
            StringBuilder dbUpdateScriptContent = new StringBuilder(databaseValidationService.getUpdateScript(false));
            if(StringUtils.isNotEmpty(dbUpdateScriptContent)) {
//                TODO: update sql script name generation so it contains version info
                if(dbScriptLines != null) {
                    dbScriptLines.forEach( l -> dbUpdateScriptContent.append(System.getProperty("line.separator")).append(l));
                }
                zipUtils.addToZipFile(dbUpdateScriptContent.toString(), EXPORT_MIGRATION_PATH_ + "upgrade.sql", zos);
            }
        } catch (IOException | URISyntaxException e) {
            error("[additionalExportFiles]", e);
        }
    }

    /**
     * Enumerates file entries within a folder inside a JAR archive.
     * <p>
     * This method parses a {@code jar:file:} URL to extract the JAR file path and the target folder within the JAR.
     * It then iterates through all JAR entries, filtering those that start with the specified folder path prefix.
     * The method is used to discover supplemental export resources when the application is deployed as a packaged JAR.

     * <p>
     * Implementation detail: The method splits the URL on the {@code '!'} separator to extract the JAR file path
     * and entry folder path. It then opens the JAR file and iterates entries using {@link JarFile#entries()}.

     * <p>
     * Current limitation: The method is non-recursive and does not descend into subdirectories within the target folder.

     *
     * @param jarFile URL with {@code jar:file:} scheme pointing to folder within JAR
     *                (e.g., {@code jar:file:/path/to.jar!/folder/subfolder/})
     * @return list of entry paths (relative to JAR root) within the specified folder; empty list if folder not found or on IOException
     */
    // TODO : list it recursively
    private List<String> jarResources(URL jarFile) {
        // at this point argument URL is useful but compatible when trying to list
        // actual nested folder within a JAR
        String[] jarPathParts = jarFile.getPath().replace("file:", "").split("!", 2);

        final String jarFilePath = jarPathParts[0];
        // Jar entries do not start with / so we need to drop it from the original URL
        final String subfolder = jarPathParts[1].replaceFirst("/", "").replace("!/", "/");
        List<String> folderContent = new ArrayList<>();
        try (JarFile jar = new JarFile(new File(jarFilePath))) {
            Enumeration<JarEntry> e = jar.entries();
            while (e.hasMoreElements()) {
                JarEntry jarEntry = e.nextElement();
                // match only file inside the folder but not the folder itself
                if (!jarEntry.getName().equals(subfolder) && jarEntry.getName().startsWith(subfolder)) {
                    // folderContent.add(jarEntry)
                    folderContent.add(jarEntry.getName());
                }
            }
        } catch (IOException e) {
            error("[jarResources] error while listing content of jar [{}]", jarFile.toString());
        }

        return folderContent;
    }

}
