package com.openkoda.service.export;

import com.openkoda.core.job.SearchIndexUpdaterJob;
import com.openkoda.core.multitenancy.QueryExecutor;
import com.openkoda.model.component.FrontendResource;
import com.openkoda.service.export.converter.ResourceLoadingException;
import com.openkoda.service.upgrade.DbVersionService;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.openkoda.service.export.FolderPathConstants.*;

/**
 * Discovers and imports OpenKoda component YAML definitions from classpath resources, supporting both filesystem and JAR-packaged deployments.
 * <p>
 * Extends {@link YamlComponentImportService} to scan configured base paths ({@link FolderPathConstants#BASE_FILE_PATHS}) and subdirectories
 * ({@link FolderPathConstants#SUBDIR_FILE_PATHS}) for .yaml files. Handles both file:// URLs (development) and jar:// URLs (packaged WAR/JAR),
 * parses YAML using SnakeYAML, delegates entity conversion to YamlToEntityConverterFactory, optionally executes /migration/upgrade.sql
 * database scripts, and triggers search index refresh after import completion.

 * <p>
 * Resource resolution supports organization-specific YAML files in org_{id} subfolders with fallback to common files. For example,
 * when loading a component for organization 123, the service first searches for {@code components/basePath/accessLevel/org_123/name.yaml},
 * then falls back to {@code components/basePath/accessLevel/name.yaml} if organization-specific file is not found.

 * <p>
 * Thread-safety: Stateless service safe for concurrent use. However, component import operations should be synchronized at the application
 * level to prevent duplicate entity creation.

 * <p>
 * Example usage:
 * <pre>{@code
 * classpathComponentImportService.loadAllComponents();
 * }</pre>

 *
 * @see YamlComponentImportService
 * @see FolderPathConstants
 * @see QueryExecutor
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Service
public class ClasspathComponentImportService extends YamlComponentImportService {

    /**
     * Query executor for running database migration scripts discovered at /migration/upgrade.sql.
     * Used to execute SQL commands in a transaction after component YAML files are loaded.
     */
    @Inject
    QueryExecutor queryExecutor;
    
    /**
     * Search index updater job for refreshing Hibernate Search indexes after component import.
     * Ensures newly imported entities are immediately searchable.
     */
    @Inject
    SearchIndexUpdaterJob searchIndexUpdaterJob;
    
    /**
     * Database version service for managing schema versioning and migrations.
     * Used internally for database upgrade operations coordinated with component imports.
     */
    @Inject
    private DbVersionService dbVersionService;
    
    /**
     * Scans classpath folders for component YAML definitions, loads and imports all discovered components, executes database migrations, and refreshes search indexes.
     * <p>
     * This method orchestrates the complete component import workflow:
     * <ol>
     *   <li>Discovers all .yaml files across configured base paths and subdirectories</li>
     *   <li>Parses each YAML file and converts to OpenKoda component entities</li>
     *   <li>Executes optional /migration/upgrade.sql script if present on classpath</li>
     *   <li>Triggers search index refresh to make imported components searchable</li>
     * </ol>

     * <p>
     * Typically invoked during application startup to initialize components from packaged resources.

     *
     * @see #getAllYamlFiles()
     * @see #loadYamlFile(String)
     * @see QueryExecutor#runQueryFromResourceInTransaction(String)
     * @see SearchIndexUpdaterJob#updateSearchIndexes()
     */
    public void loadAllComponents() {
        debug("[loadResourcesFromFiles]");

        Set<String> yamlFiles = getAllYamlFiles();

        for (String yamlFile : yamlFiles) {
            loadYamlFile(yamlFile);
        }
        if(new ClassPathResource("/migration/upgrade.sql").exists()) {
            queryExecutor.runQueryFromResourceInTransaction("/migration/upgrade.sql");
        }
        searchIndexUpdaterJob.updateSearchIndexes();
    }
    
    /**
     * Loads an organization-scoped or common YAML component definition by name and access level with automatic fallback logic.
     * <p>
     * Resolution strategy:
     * <ol>
     *   <li>First attempts to load organization-specific file: {@code components/basePath/accessLevel/org_{organizationId}/name.yaml}</li>
     *   <li>If not found, falls back to common file: {@code components/basePath/accessLevel/name.yaml}</li>
     * </ol>
     * This allows per-tenant component customization while maintaining shared defaults.

     * <p>
     * Example: Loading a form component for organization 42 with GLOBAL access level from "forms" base path
     * will search for {@code components/forms/global/org_42/myform.yaml}, then {@code components/forms/global/myform.yaml}.

     *
     * @param basePath the base folder path within components directory (e.g., "forms", "controllers"), must not be null
     * @param accessLevel the access level subdirectory (GLOBAL, ORGANIZATION, etc.), may be null for root level
     * @param organizationId the organization identifier for scoped resource lookup, must not be null
     * @param name the component name without .yaml extension, must not be null
     * @return the parsed YAML content as Object (typically a Map), or null if resource not found
     * @see FrontendResource.AccessLevel
     * @see #loadYamlFile(String)
     */
    public Object loadResourceFromFile(String basePath, FrontendResource.AccessLevel accessLevel, Long organizationId, String name) {
        debug("[loadResourceFromFile] {} {} {}", name, accessLevel, organizationId);

        String aLevel = accessLevel != null ? accessLevel.getPath() : "";
        String yamlFile = COMPONENTS_ + basePath + aLevel + SUBDIR_ORGANIZATION_PREFIX + organizationId + "/" + name + ".yaml";
        Resource resource = new ClassPathResource(yamlFile);
        if (resource.exists()) {
//            resource exists in /components/basePath/accessLevel/org_orgId/ directory
//            let's load yaml configuration
            return loadYamlFile(yamlFile);
        } else {
//            no yaml for given organization ID try to load common organization file
            yamlFile = COMPONENTS_ + basePath + aLevel + name + ".yaml";
            resource = new ClassPathResource(yamlFile);
            if (resource.exists()) {
                return loadYamlFile(yamlFile);
            }
        }
        return null;
    }
    
    /**
     * Discovers all .yaml files across configured base paths and subdirectories by scanning classpath resources.
     * <p>
     * Iterates through all combinations of {@link FolderPathConstants#BASE_FILE_PATHS} and {@link FolderPathConstants#SUBDIR_FILE_PATHS},
     * delegating to {@link #getYamlFilesFromDir(String, String, Set)} for protocol-specific scanning (file:// or jar://).
     * Collects organization-specific YAML files in org_{id} subfolders as well as common files at each path level.

     *
     * @return a Set of relative classpath paths to discovered .yaml files, never null (may be empty)
     * @see FolderPathConstants#BASE_FILE_PATHS
     * @see FolderPathConstants#SUBDIR_FILE_PATHS
     * @see #getYamlFilesFromDir(String, String, Set)
     */
    private Set<String> getAllYamlFiles() {
        Set<String> yamlFiles = new HashSet<>();
        List<String> allFilePaths = new ArrayList<>(BASE_FILE_PATHS);
        List<String> subdirFilePaths = new ArrayList<>(SUBDIR_FILE_PATHS);
        for (String folderPath : allFilePaths) {
            for (String subdirPath : subdirFilePaths) {
                getYamlFilesFromDir(folderPath, subdirPath, yamlFiles);
            }
            getYamlFilesFromDir(folderPath, "", yamlFiles);
        }
        return yamlFiles;
    }

    /**
     * Enumerates .yaml files from a specific classpath folder by delegating to protocol-specific handlers.
     * <p>
     * Resolves all classpath resources matching the folderPath + subdirPath combination, then dispatches to either
     * {@link #getFromFile(Set, URL, String, String)} for file:// URLs (filesystem directories in development) or
     * {@link #getFromJar(Set, String, URL)} for jar:// URLs (packaged WAR/JAR deployments). Accumulates discovered
     * file paths into the provided yamlFiles set.

     *
     * @param folderPath the base folder path to scan (e.g., "components/forms/"), must not be null
     * @param subdirPath the subdirectory path relative to folderPath (e.g., "global/"), may be empty string
     * @param yamlFiles the accumulator set for discovered .yaml file paths, modified in place
     * @throws ResourceLoadingException if IOException occurs while accessing classpath resources
     * @see #getFromFile(Set, URL, String, String)
     * @see #getFromJar(Set, String, URL)
     */
    private void getYamlFilesFromDir(String folderPath, String subdirPath, Set<String> yamlFiles) {
        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources(folderPath + subdirPath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if (url.getProtocol().equals("jar")) {
                    getFromJar(yamlFiles, folderPath, url);
                } else if (url.getProtocol().equals("file")) {
                    getFromFile(yamlFiles, url, folderPath, subdirPath);
                }
            }
        } catch (IOException e) {
            throw new ResourceLoadingException("Error accessing resource under path: " + folderPath);
        }
    }

    /**
     * Scans a filesystem directory for .yaml files including organization-specific subfolders (org_{id}).
     * <p>
     * Converts the file:// URL to a filesystem Path and enumerates directory contents. For each subdirectory,
     * recursively scans for .yaml files (supporting org_{id} folder pattern). For direct .yaml files in the
     * base directory, constructs relative classpath paths by combining basePath, subdirPath, and filename.
     * Handles Windows path separators by normalizing to forward slashes.

     *
     * @param yamlFiles the accumulator set for discovered .yaml file paths, modified in place
     * @param url the file:// URL pointing to the directory to scan, must not be null
     * @param basePath the base classpath path (e.g., "components/forms/"), used to construct relative paths
     * @param subdirPath the subdirectory path (e.g., "global/"), may be empty string
     * @throws ResourceLoadingException if URISyntaxException occurs converting URL to Path, or IOException during directory traversal
     * @see DirectoryStream
     */
    private void getFromFile(Set<String> yamlFiles, URL url, String basePath, String subdirPath) {
        try {
            Path resourceFolderPath = Paths.get(url.toURI());
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(resourceFolderPath)) {
                for (Path path : stream) {
                    if (Files.isDirectory(path)) {
                        try (DirectoryStream<Path> orgAssignedStream = Files.newDirectoryStream(path)) {
                            for (Path orgAssignedPath : orgAssignedStream) {
                                if(orgAssignedPath.toString().endsWith(".yaml")) {
                                    String p = orgAssignedPath.toString().replace("\\", "/");
                                    yamlFiles.add(p.substring(StringUtils.indexOf(p, basePath)));
                                }
                            }
                        }
                    } else if(path.toString().endsWith(".yaml")) {
                        yamlFiles.add(basePath + subdirPath + path.getFileName().toString());
                    }
                }
            } catch (IOException e) {
                throw new ResourceLoadingException("YAML files not found under the path: " + resourceFolderPath);
            }
        } catch (URISyntaxException e) {
            throw new ResourceLoadingException("Incorrect folder path syntax: " + e.getMessage());
        }
    }

    /**
     * Scans a JAR file for .yaml entries matching the specified folder path prefix.
     * <p>
     * Opens a JarURLConnection to the packaged archive, enumerates all JAR entries, and delegates to
     * {@link #getMatchingJarEntries(JarURLConnection, String)} to filter entries starting with folderPath
     * and ending with .yaml extension. Accumulates matching entry names as classpath-relative paths.

     * <p>
     * This method enables component discovery in production deployments where resources are packaged
     * in WAR or JAR files rather than exploded on the filesystem.

     *
     * @param yamlFiles the accumulator set for discovered .yaml file paths, modified in place
     * @param folderPath the folder path prefix to match (e.g., "components/forms/"), must not be null
     * @param url the jar:// URL pointing to the JAR resource, must not be null
     * @throws ResourceLoadingException if IOException occurs opening JAR connection or reading entries
     * @see JarURLConnection
     * @see #getMatchingJarEntries(JarURLConnection, String)
     */
    private void getFromJar(Set<String> yamlFiles, String folderPath, URL url) {
        try {
            URLConnection urlConnection = url.openConnection();
            if (urlConnection instanceof JarURLConnection) {
                List<String> fileNames = getMatchingJarEntries((JarURLConnection) urlConnection, folderPath);
                yamlFiles.addAll(fileNames);
            }
        } catch (IOException e) {
            throw new ResourceLoadingException("Error loading resources from path " + folderPath);
        }
    }

    /**
     * Enumerates JAR entries with names starting with the specified folder path and ending with .yaml extension.
     * <p>
     * Opens the JAR file from the connection, iterates through all entries, and filters by prefix (folderPath)
     * and suffix (.yaml). Returns a list of matching entry names as classpath-relative paths suitable for
     * resource loading. Handles nested organization-specific folders (org_{id}) transparently.

     *
     * @param jarConnection the JarURLConnection to the packaged archive, must not be null
     * @param folderPath the folder path prefix for filtering entries (e.g., "components/forms/"), must not be null
     * @return a List of matching entry names (e.g., "components/forms/global/myform.yaml"), never null (may be empty)
     * @throws IOException if error occurs reading JAR file or enumerating entries
     * @see JarFile
     * @see JarEntry
     */
    private List<String> getMatchingJarEntries(JarURLConnection jarConnection, String folderPath) throws IOException {
        List<String> matchingEntries = new ArrayList<>();
        try (JarFile jarFile = jarConnection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.startsWith(folderPath) && entryName.endsWith(".yaml")) {
                    matchingEntries.add(entryName);
                }
            }
        }
        return matchingEntries;
    }

    /**
     * Loads and parses a YAML file from classpath, delegating DTO-to-entity conversion to YamlToEntityConverterFactory.
     * <p>
     * Opens an InputStream for the specified classpath path using {@link #loadResource(String)}, parses YAML content
     * with SnakeYAML, and delegates the resulting DTO object to {@code yamlToEntityConverterFactory.processYamlDto()}
     * for conversion to OpenKoda component entities. Returns the parsed YAML content as an Object (typically a Map).

     * <p>
     * Note: The method attempts resource loading twice as a workaround for potential classloader caching issues.

     *
     * @param yamlFile the classpath-relative path to the .yaml file (e.g., "components/forms/global/myform.yaml"), must not be null
     * @return the parsed YAML content as Object, or null if resource not found or parsing fails
     * @see #loadResource(String)
     * @see Yaml#load(InputStream)
     */
    private Object loadYamlFile(String yamlFile) {
        InputStream inputStream = loadResource(yamlFile);
        if(inputStream == null){
            inputStream = loadResource(yamlFile);
        }
        if (inputStream != null) {
            debug("[YamlLoaderService] Processing file: " + yamlFile);
            return yamlToEntityConverterFactory.processYamlDto(new Yaml().load(inputStream), yamlFile);
        }
        return null;
    }

    /**
     * Opens an InputStream for a classpath resource at the specified path using the thread context classloader.
     * <p>
     * Wraps {@code ClassLoader.getResourceAsStream()} to load resources from both filesystem (development) and
     * packaged archives (JAR/WAR). Returns null if the resource does not exist rather than throwing an exception,
     * allowing caller to handle missing resources gracefully.

     *
     * @param path the classpath-relative path to the resource (e.g., "components/forms/myform.yaml"), must not be null
     * @return an InputStream for reading the resource content, or null if resource not found
     * @see ClassLoader#getResourceAsStream(String)
     */
    private InputStream loadResource(String path) {
        return this.getClass().getClassLoader().getResourceAsStream(path);
    }
}
