package com.openkoda.service.export.converter.impl;

import com.openkoda.core.flow.LoggingComponent;
import com.openkoda.service.export.converter.EntityToYamlConverter;
import com.openkoda.service.export.util.ZipUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import static com.openkoda.service.export.FolderPathConstants.EXPORT_PATH;
import static java.nio.file.Files.*;
import static java.nio.file.Paths.get;

/**
 * Abstract base class that centralizes mechanics for all Entity-to-YAML converters in the export subsystem.
 * <p>
 * This class provides template methods and helper utilities for converting domain entities to exportable 
 * YAML configuration files plus optional content files. Subclasses implement entity-specific logic by 
 * overriding abstract methods for path construction, content extraction, and DTO conversion.
 * </p>
 * 
 * <h2>Core Responsibilities</h2>
 * <ol>
 *   <li><b>YAML Serialization:</b> Serializes DTOs to YAML using SnakeYAML with deterministic formatting 
 *       (DOUBLE_QUOTED scalar style, pretty flow enabled for readable output)</li>
 *   <li><b>File Persistence:</b> Persists content and YAML files to filesystem using java.nio.file APIs 
 *       (createDirectories, createFile, Files.write)</li>
 *   <li><b>Zip Packaging:</b> Packages content and YAML files into ZipOutputStream via {@link ZipUtils}</li>
 *   <li><b>Duplicate Prevention:</b> Avoids duplicate zip entries using caller-supplied {@code Set<String>} 
 *       zipEntries, checking membership before adding each entry</li>
 *   <li><b>File Cleanup:</b> Removes exported files while pruning empty parent folders during cleanup operations</li>
 * </ol>
 * 
 * <h2>SnakeYAML Configuration</h2>
 * <p>
 * Uses DumperOptions with DOUBLE_QUOTED scalar style and {@code setPrettyFlow(true)} to ensure consistent, 
 * readable YAML output. Double-quoting avoids ambiguity with special characters and ensures deterministic 
 * serialization across environments.
 * </p>
 * 
 * <h2>Exception Handling</h2>
 * <p>
 * IOExceptions from file operations are converted to unchecked RuntimeException, simplifying error propagation 
 * for callers. Subclasses should document specific exception scenarios.
 * </p>
 * 
 * <h2>Template Methods (Must Implement)</h2>
 * <ul>
 *   <li>{@link #getPathToContentFile(Object)} - Returns absolute filesystem path for entity's content file, 
 *       or null if no content file</li>
 *   <li>{@link #getContent(Object)} - Returns entity's content string, or null if no content</li>
 *   <li>{@link #getPathToYamlComponentFile(Object)} - Returns absolute filesystem path for entity's YAML 
 *       component file, or null if no YAML component</li>
 *   <li>{@link #getConversionDto(Object)} - Converts entity to DTO for YAML serialization</li>
 * </ul>
 * 
 * <h2>Helper Methods (Provided)</h2>
 * <ul>
 *   <li>{@link #addToZip(Object, ZipOutputStream, Set)} - Packages entity files into zip</li>
 *   <li>{@link #saveToFile(Object)} - Persists entity files to filesystem</li>
 *   <li>{@link #removeExportedFiles(Object)} - Deletes exported files and prunes empty folders</li>
 *   <li>{@link #getResourcePathToContentFile(Object)} - Converts absolute path to relative resource path</li>
 *   <li>{@link #dtoToYamlString(Object)} - Serializes DTO to YAML string</li>
 *   <li>{@link #getYamlDefaultFilePath(String, String, Long)} - Constructs standard YAML filename with 
 *       optional organization suffix</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * Concurrency safety depends on callers coordinating access to the shared zipEntries Set and the thread-safety 
 * of {@link ZipUtils}. This class itself maintains no mutable state beyond the autowired ZipUtils dependency.
 * </p>
 * 
 * @param <T> the domain entity type (e.g., Form, ControllerEndpoint, FrontendResource)
 * @param <D> the conversion DTO type (e.g., FormConversionDto, ControllerEndpointConversionDto)
 * 
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * 
 * @see EntityToYamlConverter
 * @see LoggingComponent
 * @see ZipUtils
 * @see Yaml
 */
public abstract class AbstractEntityToYamlConverter<T,D> implements EntityToYamlConverter<T, D>, LoggingComponent {

    @Autowired
    ZipUtils zipUtils;

    /**
     * Packages the entity's content and YAML component files into a ZipOutputStream.
     * <p>
     * This method implements the following algorithm:
     * <ol>
     *   <li>Retrieve pathToContentFile via {@link #getPathToContentFile(Object)}. If non-null and not already 
     *       in zipEntries Set:
     *       <ul>
     *         <li>Retrieve content via {@link #getContent(Object)}</li>
     *         <li>If content is non-null, add to zip via {@link ZipUtils#addToZipFile(String, String, ZipOutputStream)} 
     *             and track path in zipEntries</li>
     *       </ul>
     *   </li>
     *   <li>Create conversion DTO via {@link #getConversionDto(Object)}</li>
     *   <li>Retrieve pathToComponentFile via {@link #getPathToYamlComponentFile(Object)}. If non-null and not 
     *       already in zipEntries Set:
     *       <ul>
     *         <li>Serialize DTO to YAML via {@link #dtoToYamlString(Object)}</li>
     *         <li>Add YAML to zip and track path in zipEntries</li>
     *       </ul>
     *   </li>
     *   <li>Return the conversion DTO</li>
     * </ol>
     * </p>
     * 
     * <h3>Duplicate Prevention</h3>
     * <p>
     * Checks zipEntries Set before adding each file to prevent duplicate zip entries. Updates zipEntries 
     * after successful addition. This enables multiple entities to share the same zip without conflicts.
     * </p>
     * 
     * <h3>Null Handling</h3>
     * <p>
     * Null paths or null content are silently skipped (no zip entry added). This allows converters to 
     * export YAML-only (no content file) or content-only (no YAML component) as needed.
     * </p>
     * 
     * @param entity the domain entity to export (must not be null)
     * @param zipOut the ZipOutputStream to write entries to (must not be null, managed by caller)
     * @param zipEntries mutable Set tracking already-added zip entry paths to prevent duplicates (must not be null)
     * @return the conversion DTO created from entity
     * @throws NullPointerException if any parameter is null
     * @throws RuntimeException if {@link ZipUtils#addToZipFile(String, String, ZipOutputStream)} fails
     * @see ZipUtils#addToZipFile(String, String, ZipOutputStream)
     */
    public D addToZip(T entity, ZipOutputStream zipOut, Set<String> zipEntries){
        final String pathToContentFile = getPathToContentFile(entity);
        if(pathToContentFile != null && !zipEntries.contains(pathToContentFile)) {
            String content = getContent(entity);
            if(content != null) {
                zipUtils.addToZipFile(content, pathToContentFile, zipOut);
                zipEntries.add(pathToContentFile);
            }
        }
        D dto = getConversionDto(entity);
        final String pathToComponentFile = getPathToYamlComponentFile(entity);
        if(pathToComponentFile != null && !zipEntries.contains(pathToComponentFile)) {
            zipUtils.addToZipFile(dtoToYamlString(dto), pathToComponentFile, zipOut);
            zipEntries.add(pathToComponentFile);
        }
        return dto;
    }
    
    /**
     * Persists the entity's content and YAML component files to the filesystem.
     * <p>
     * If {@link #getPathToContentFile(Object)} returns a non-null path, this method saves the content 
     * returned by {@link #getContent(Object)} to that path. If {@link #getPathToYamlComponentFile(Object)} 
     * returns a non-null path, this method serializes the conversion DTO to YAML and saves it to that path.
     * </p>
     * 
     * <h3>File Creation Behavior</h3>
     * <p>
     * For each file path:
     * <ul>
     *   <li>Creates parent directories if they do not exist</li>
     *   <li>Creates the file if it does not exist</li>
     *   <li>Writes content bytes to the file</li>
     * </ul>
     * </p>
     * 
     * <p>
     * This method delegates to the private {@link #saveToFile(String, String)} helper for actual I/O operations.
     * </p>
     * 
     * @param entity the domain entity to export (must not be null)
     * @return the entity unchanged, enabling method chaining
     * @throws NullPointerException if entity is null
     * @throws RuntimeException wrapping IOException if file I/O fails (directory creation, file creation, or write)
     * @see #saveToFile(String, String)
     */
    @Override
    public T saveToFile(T entity) {
        if(getPathToContentFile(entity) != null) {
            saveToFile(getPathToContentFile(entity), getContent(entity));
        }
        if(getPathToYamlComponentFile(entity) != null) {
            saveToFile(getPathToYamlComponentFile(entity), dtoToYamlString(getConversionDto(entity)));
        }
        return entity;
    }

    /**
     * Deletes the entity's exported content and YAML files, pruning empty parent folders.
     * <p>
     * If {@link #getPathToContentFile(Object)} returns a non-null path, this method removes that file. 
     * If {@link #getPathToYamlComponentFile(Object)} returns a non-null path, this method removes that file.
     * </p>
     * 
     * <h3>Folder Pruning Behavior</h3>
     * <p>
     * After deleting each file, this method checks if the parent folder is empty (no remaining files). 
     * If the parent folder is empty, it deletes the parent folder as well. This prevents accumulation 
     * of empty directory structures during cleanup operations.
     * </p>
     * 
     * <p>
     * This method delegates to the private {@link #removeFileIfExists(String)} helper for actual deletion logic.
     * </p>
     * 
     * @param entity the domain entity whose exported files to remove (must not be null)
     * @return the entity unchanged, enabling method chaining
     * @throws NullPointerException if entity is null
     * @throws RuntimeException wrapping IOException if file deletion or directory listing fails
     * @see #removeFileIfExists(String)
     */
    @Override
    public T removeExportedFiles(T entity) {
        if(getPathToContentFile(entity) != null) {
            removeFileIfExists(getPathToContentFile(entity));
        }
        if(getPathToYamlComponentFile(entity) != null) {
           removeFileIfExists(getPathToYamlComponentFile(entity));
        }
        return entity;
    }

    /**
     * Private helper that persists a content string to the specified filesystem path.
     * <p>
     * This method implements the following file creation logic:
     * <ol>
     *   <li>Resolve Path from pathToFile string</li>
     *   <li>If file does not exist:
     *       <ul>
     *         <li>Create parent directories if needed via {@link Files#createDirectories(Path, java.nio.file.attribute.FileAttribute...)}</li>
     *         <li>Create the file via {@link Files#createFile(Path, java.nio.file.attribute.FileAttribute...)}</li>
     *       </ul>
     *   </li>
     *   <li>If content is non-null, write content bytes to file via {@link Files#write(Path, byte[], java.nio.file.OpenOption...)}</li>
     * </ol>
     * </p>
     * 
     * <h3>Exception Handling</h3>
     * <p>
     * Any IOException encountered during directory creation, file creation, or write operations is wrapped 
     * in an unchecked RuntimeException and re-thrown.
     * </p>
     * 
     * @param pathToFile absolute file path string (must not be null)
     * @param content file content string (may be null - creates an empty file if null)
     * @throws RuntimeException wrapping IOException if directory creation, file creation, or write fails
     */
    private void saveToFile(String pathToFile, String content){
        try {
            Path fullPath = get(pathToFile);
            if(!exists(fullPath)) {
                Path folderPath = fullPath.getParent();
                if(!exists(folderPath)){
                    Files.createDirectories(folderPath);
                }
                createFile(fullPath);
            }
            if(content != null) {
                Files.write(fullPath, content.getBytes());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Private helper that deletes a file and prunes its parent folder if empty.
     * <p>
     * This method implements the following deletion logic:
     * <ol>
     *   <li>Resolve Path from pathToFile string</li>
     *   <li>If file exists, delete the file via {@link Files#delete(Path)}</li>
     *   <li>Check if parent folder is empty by listing directory contents via {@link Files#list(Path)}</li>
     *   <li>If parent folder is empty (list returns no entries), delete the parent folder</li>
     * </ol>
     * </p>
     * 
     * <h3>Exception Handling</h3>
     * <p>
     * Any IOException encountered during file deletion or directory listing is wrapped in an unchecked 
     * RuntimeException and re-thrown.
     * </p>
     * 
     * @param pathToFile absolute file path string (must not be null)
     * @throws RuntimeException wrapping IOException if file deletion or directory listing fails
     */
    private void removeFileIfExists(String pathToFile){
        Path fullPath = get(pathToFile);
        try {
            if(exists(fullPath)) {
                delete(fullPath);
                if(Files.list(fullPath.getParent()).toList().isEmpty()){
                    delete(fullPath.getParent());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Converts an absolute filesystem path to a relative resource path by removing the EXPORT_PATH prefix.
     * <p>
     * This method performs the transformation: {@link #getPathToContentFile(Object)}.replace(EXPORT_PATH, "")
     * </p>
     * 
     * <h3>Use Case</h3>
     * <p>
     * Resource paths are stored in DTOs for import operations. Relative paths are portable across 
     * environments with different filesystem layouts, enabling export from one system and import to another.
     * </p>
     * 
     * @param entity the domain entity (must not be null)
     * @return relative resource path string with EXPORT_PATH prefix removed
     * @throws NullPointerException if entity is null or {@link #getPathToContentFile(Object)} returns null
     * @see com.openkoda.service.export.FolderPathConstants#EXPORT_PATH
     */
    public String getResourcePathToContentFile(T entity){
        return getPathToContentFile(entity).replace(EXPORT_PATH, "");
    }
    
    /**
     * Template method - subclasses implement to return the absolute filesystem path for the entity's content file.
     * <p>
     * Returns null if the entity has no content file (YAML-only export).
     * </p>
     * 
     * @param entity the domain entity (must not be null)
     * @return absolute filesystem path to content file, or null if no content file
     * @throws NullPointerException implementation-dependent
     */
    public abstract String getPathToContentFile(T entity);
    
    /**
     * Template method - subclasses implement to return the entity's content string.
     * <p>
     * Returns null if the entity has no content (YAML-only export).
     * </p>
     * 
     * @param entity the domain entity (must not be null)
     * @return content string (JavaScript, HTML, CSS, etc.), or null if no content
     * @throws NullPointerException implementation-dependent
     */
    public abstract String getContent(T entity);
    
    /**
     * Template method - subclasses implement to return the absolute filesystem path for the entity's YAML component file.
     * <p>
     * Returns null if the entity has no YAML component (content-only export, which is rare).
     * </p>
     * 
     * @param entity the domain entity (must not be null)
     * @return absolute filesystem path to YAML file, or null if no YAML component
     * @throws NullPointerException implementation-dependent
     */
    public abstract String getPathToYamlComponentFile(T entity);
    
    /**
     * Template method - subclasses implement to convert the entity to a DTO for YAML serialization.
     * 
     * @param entity the domain entity to convert (must not be null)
     * @return conversion DTO with fields mapped from entity
     * @throws NullPointerException implementation-dependent
     */
    public abstract D getConversionDto(T entity);

    /**
     * Serializes a DTO to YAML string using SnakeYAML with deterministic formatting.
     * <p>
     * This method configures SnakeYAML with:
     * <ul>
     *   <li><b>DumperOptions.ScalarStyle.DOUBLE_QUOTED:</b> All scalar values are enclosed in double quotes</li>
     *   <li><b>setPrettyFlow(true):</b> Enables readable flow style formatting</li>
     * </ul>
     * </p>
     * 
     * <h3>Rationale</h3>
     * <p>
     * Double-quoting ensures consistent YAML output across all platforms and avoids ambiguity with special 
     * characters. This produces deterministic serialization that is safe for version control and 
     * environment portability.
     * </p>
     * 
     * @param object the DTO object to serialize (typically a conversion DTO instance)
     * @return YAML string representation of the object
     * @throws org.yaml.snakeyaml.error.YAMLException if YAML serialization fails
     */
    String dtoToYamlString(Object object) {
        DumperOptions options = new DumperOptions();
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);

        return yaml.dump(object);
    }
    
    /**
     * Constructs a standard YAML filename pattern with optional organization suffix.
     * <p>
     * This method generates filenames according to the following pattern:
     * <ul>
     *   <li>If organizationId is null: {@code "{filePath}{entityName}.yaml"}</li>
     *   <li>If organizationId is non-null: {@code "{filePath}{entityName}_{organizationId}.yaml"}</li>
     * </ul>
     * </p>
     * 
     * <h3>Use Case</h3>
     * <p>
     * Generates consistent filenames for both tenant-scoped and global entities. The organization 
     * suffix enables multiple organizations to export entities with the same name without filename 
     * collisions.
     * </p>
     * 
     * @param filePath directory path prefix (typically EXPORT_CONFIG_PATH_ + entity type constant)
     * @param entityName entity name for filename (must not be null)
     * @param organizationId tenant organization ID for multi-tenancy, or null for global entities
     * @return complete YAML file path with organization suffix if applicable
     * @throws NullPointerException if filePath or entityName is null
     */
    String getYamlDefaultFilePath(String filePath, String entityName, Long organizationId){
        debug("[getYamlDefaultFilePath]");
        return organizationId == null ? String.format("%s%s.yaml",filePath, entityName) : String.format("%s%s_%s.yaml", filePath, entityName, organizationId);
    }
}
