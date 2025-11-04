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

import com.openkoda.model.component.FrontendResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Centralized string constants defining folder path conventions for OpenKoda component export/import operations.
 * <p>
 * This class provides classpath-relative and filesystem path fragments used by import/export services to discover
 * YAML component definitions, code resources, and supplemental files. Constants include component category prefixes
 * (server-side, form, scheduler, event), export destination paths for ZIP packaging (code, templates, config,
 * privileges, migration), base paths combining category and COMPONENTS_ prefix, and derived collections
 * (BASE_FILE_PATHS, SUBDIR_FILE_PATHS) for systematic folder scanning.
 * 
 * <p>
 * <b>Usage pattern:</b> Import services scan BASE_FILE_PATHS × SUBDIR_FILE_PATHS cartesian product to discover
 * all component YAML files. Export services construct output paths using EXPORT_*_PATH_ prefixes to package
 * components into distributable ZIP archives.
 * 
 * <p>
 * <b>Thread-safety:</b> All constants are immutable and safe for concurrent access across multiple threads.
 * 
 * <p>
 * <b>Maintenance note:</b> All path fragments include trailing slashes for safe concatenation without additional
 * path separator logic.
 * 
 *
 * @see ClasspathComponentImportService for usage in classpath scanning
 * @see ComponentExportService for usage in ZIP packaging
 * @see com.openkoda.model.component.FrontendResource.AccessLevel for SUBDIR_FILE_PATHS derivation
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class FolderPathConstants {

    // Component category prefixes - Base folder names for component categories within classpath resources and exports

    /**
     * Root folder for classpath component YAML definitions.
     * <p>
     * Value: {@code "components/"}
     * 
     * <p>
     * All component YAML files are discovered relative to this base path. Import services append category-specific
     * subfolders (SERVER_SIDE_, FORM_, etc.) to create complete scan paths.
     * 
     */
    public static final String COMPONENTS_ = "components/";

    /**
     * Folder for supplemental static resources packaged in component exports.
     * <p>
     * Value: {@code "components-additional-files/"}
     * 
     * <p>
     * Contains non-YAML files such as application property templates, configuration examples, and documentation
     * that should be included in exported ZIP archives alongside YAML component definitions.
     * 
     */
    public static final String COMPONENTS_ADDITIONAL_FILES_ = "components-additional-files/";

    /**
     * Subfolder for server-side JavaScript component definitions.
     * <p>
     * Value: {@code "server-side/"}
     * 
     * <p>
     * Contains YAML files defining JavaScript functions executed on the server via GraalVM polyglot integration.
     * 
     */
    public static final String SERVER_SIDE_ = "server-side/";

    /**
     * Subfolder for dynamic form definitions.
     * <p>
     * Value: {@code "form/"}
     * 
     * <p>
     * Contains YAML files defining Form entities with field configurations, validation rules, and database mappings
     * for runtime form generation.
     * 
     */
    public static final String FORM_ = "form/";

    /**
     * Subfolder for scheduled job definitions.
     * <p>
     * Value: {@code "scheduler/"}
     * 
     * <p>
     * Contains YAML files defining Scheduler entities with cron expressions and execution logic for background tasks.
     * 
     */
    public static final String SCHEDULER_ = "scheduler/";

    /**
     * Subfolder for event listener definitions.
     * <p>
     * Value: {@code "event/"}
     * 
     * <p>
     * Contains YAML files defining EventListenerEntry entities that respond to application lifecycle and domain events.
     * 
     */
    public static final String EVENT_ = "event/";

    /**
     * Subfolder for custom event definitions.
     * <p>
     * Value: {@code "custom/"}
     * 
     * <p>
     * Contains YAML files for custom user-defined event handlers and triggers.
     * 
     */
    public static final String CUSTOM_EVENT_ = "custom/";

    /**
     * Subfolder for frontend resource and controller endpoint definitions.
     * <p>
     * Value: {@code "frontend-resource/"}
     * 
     * <p>
     * Contains YAML files defining FrontendResource entities including HTTP endpoints, Thymeleaf templates,
     * and JavaScript UI components.
     * 
     */
    public static final String FRONTEND_RESOURCE_ = "frontend-resource/";

    /**
     * Subfolder for UI component definitions.
     * <p>
     * Value: {@code "ui-component/"}
     * 
     * <p>
     * Contains YAML files defining reusable UI components and widget configurations.
     * 
     */
    public static final String UI_COMPONENT_ = "ui-component/";

    // Export destination paths - Filesystem path prefixes for exporting components to Maven src/main/resources structure

    /**
     * Base Maven resources folder for component exports.
     * <p>
     * Value: {@code "src/main/resources/"}
     * 
     * <p>
     * All exported artifacts are placed under this directory following standard Maven project structure conventions.
     * 
     */
    public static final String EXPORT_PATH="src/main/resources/";

    /**
     * Destination folder for executable code files such as JavaScript and Groovy scripts.
     * <p>
     * Value: {@code "src/main/resources/code/"}
     * 
     * <p>
     * Server-side JavaScript files and other executable code resources are exported to this location for runtime execution.
     * 
     */
    public static final String EXPORT_CODE_PATH_ = EXPORT_PATH + "code/";

    /**
     * Destination folder for Thymeleaf templates and static web resources.
     * <p>
     * Value: {@code "src/main/resources/templates/"}
     * 
     * <p>
     * HTML templates, CSS files, and frontend resources referenced by FrontendResource entities are exported here.
     * 
     */
    public static final String EXPORT_RESOURCES_PATH_ = EXPORT_PATH + "templates/";

    /**
     * Destination folder for YAML component configuration files.
     * <p>
     * Value: {@code "src/main/resources/components/"}
     * 
     * <p>
     * Component YAML definitions (forms, schedulers, events, frontend resources) are exported to this location
     * maintaining the same category subfolder structure used during import.
     * 
     */
    public static final String EXPORT_CONFIG_PATH_ = EXPORT_PATH + "components/";

    /**
     * Destination folder for privilege definition YAML files.
     * <p>
     * Value: {@code "src/main/resources/privileges/"}
     * 
     * <p>
     * DynamicPrivilege entity definitions exported as YAML are placed in this directory for access control configuration.
     * 
     */
    public static final String EXPORT_PRIVILEGE_PATH_ = EXPORT_PATH + "privileges/";

    /**
     * Destination folder for SQL database migration scripts.
     * <p>
     * Value: {@code "src/main/resources/migration/"}
     * 
     * <p>
     * Generated upgrade.sql files containing ALTER TABLE, CREATE TABLE, and other DDL statements required by
     * exported components are written to this location for database schema versioning.
     * 
     */
    public static final String EXPORT_MIGRATION_PATH_ = EXPORT_PATH + "migration/";

    /**
     * Prefix for organization-scoped subfolders within component categories.
     * <p>
     * Value: {@code "org_"}
     * 
     * <p>
     * Multi-tenant deployments use this prefix to create tenant-specific component folders following the pattern:
     * {@code COMPONENTS_ + category + access_level + SUBDIR_ORGANIZATION_PREFIX + organizationId + "/"}.
     * For example: {@code "components/form/public/org_123/user-registration.yaml"} contains a form definition
     * scoped to organization with ID 123.
     * 
     *
     * @see ClasspathComponentImportService for organization-scoped resource loading
     */
    public static final String SUBDIR_ORGANIZATION_PREFIX = "org_";

    // Composite base paths - Pre-computed composite paths combining COMPONENTS_ with category subfolders

    /**
     * Complete base path for server-side JavaScript component YAML files.
     * <p>
     * Value: {@code "components/server-side/"}
     * 
     * <p>
     * Pre-computed concatenation of {@link #COMPONENTS_} and {@link #SERVER_SIDE_} for convenient scanning.
     * 
     */
    public static final String SERVER_SIDE_BASE_FILES_PATH = COMPONENTS_ + SERVER_SIDE_;

    /**
     * Complete base path for form definition YAML files.
     * <p>
     * Value: {@code "components/form/"}
     * 
     * <p>
     * Pre-computed concatenation of {@link #COMPONENTS_} and {@link #FORM_} for convenient scanning.
     * 
     */
    public static final String FORM_BASE_FILES_PATH = COMPONENTS_ + FORM_;

    /**
     * Complete base path for scheduler definition YAML files.
     * <p>
     * Value: {@code "components/scheduler/"}
     * 
     * <p>
     * Pre-computed concatenation of {@link #COMPONENTS_} and {@link #SCHEDULER_} for convenient scanning.
     * 
     */
    public static final String SCHEDULER_BASE_FILES_PATH = COMPONENTS_ + SCHEDULER_;

    /**
     * Complete base path for event listener definition YAML files.
     * <p>
     * Value: {@code "components/event/"}
     * 
     * <p>
     * Pre-computed concatenation of {@link #COMPONENTS_} and {@link #EVENT_} for convenient scanning.
     * 
     */
    public static final String EVENT_BASE_FILES_PATH = COMPONENTS_ + EVENT_;

    /**
     * Complete base path for frontend resource definition YAML files.
     * <p>
     * Value: {@code "components/frontend-resource/"}
     * 
     * <p>
     * Pre-computed concatenation of {@link #COMPONENTS_} and {@link #FRONTEND_RESOURCE_} for convenient scanning.
     * 
     */
    public static final String FRONTEND_RESOURCE_BASE_FILES_PATH = COMPONENTS_ + FRONTEND_RESOURCE_;

    /**
     * Complete base path for UI component definition YAML files.
     * <p>
     * Value: {@code "components/ui-component/"}
     * 
     * <p>
     * Pre-computed concatenation of {@link #COMPONENTS_} and {@link #UI_COMPONENT_} for convenient scanning.
     * 
     */
    public static final String UI_COMPONENT_BASE_FILES_PATH = COMPONENTS_ + UI_COMPONENT_;

    /**
     * Ordered list of all component category base paths for systematic classpath scanning.
     * <p>
     * Contains: {@link #SERVER_SIDE_BASE_FILES_PATH}, {@link #FORM_BASE_FILES_PATH},
     * {@link #SCHEDULER_BASE_FILES_PATH}, {@link #EVENT_BASE_FILES_PATH},
     * {@link #FRONTEND_RESOURCE_BASE_FILES_PATH}, {@link #UI_COMPONENT_BASE_FILES_PATH}
     * 
     * <p>
     * <b>Usage:</b> Import services iterate this list to discover all component YAML files across different categories.
     * Each base path is combined with access-level subdirectories from {@link #SUBDIR_FILE_PATHS} to create the
     * complete scan path cartesian product.
     * 
     * <p>
     * <b>Modification:</b> When introducing new component categories, add corresponding base path entries to this list
     * to ensure automatic discovery during import operations.
     * 
     * <p>
     * <b>Type:</b> ArrayList (mutable but should be treated as effectively immutable at runtime to prevent
     * unexpected side effects in concurrent scanning operations).
     * 
     *
     * @see ClasspathComponentImportService#getAllYamlFiles() for usage in systematic scanning
     */
    public static final List<String> BASE_FILE_PATHS = new ArrayList<>(Arrays.asList(
            SERVER_SIDE_BASE_FILES_PATH,
            FORM_BASE_FILES_PATH,
            SCHEDULER_BASE_FILES_PATH,
            EVENT_BASE_FILES_PATH,
            FRONTEND_RESOURCE_BASE_FILES_PATH,
            UI_COMPONENT_BASE_FILES_PATH
    ));

    /**
     * List of access-level subdirectory paths derived from FrontendResource.AccessLevel enum for multi-tier
     * resource organization.
     * <p>
     * This list is dynamically computed by mapping {@code FrontendResource.AccessLevel.values()} through
     * the {@code getPath()} method, producing subdirectory names corresponding to each access level tier.
     * 
     * <p>
     * <b>Example values:</b> Typical access levels include {@code "public/"}, {@code "authenticated/"},
     * {@code "admin/"} (actual values depend on the AccessLevel enum definition).
     * 
     * <p>
     * <b>Usage:</b> Combined with {@link #BASE_FILE_PATHS} in a cartesian product to scan all folder combinations.
     * For each base path, import services iterate through subdirectory paths to discover YAML files organized
     * by access level. This enables access-controlled component loading where public components are separated
     * from authenticated-only or admin-only resources.
     * 
     * <p>
     * Example scan path construction:
     * <pre>{@code
     * for (String basePath : BASE_FILE_PATHS) {
     *     for (String subdirPath : SUBDIR_FILE_PATHS) {
     *         String scanPath = basePath + subdirPath; // e.g., "components/form/public/"
     *     }
     * }
     * }</pre>
     * 
     *
     * @see com.openkoda.model.component.FrontendResource.AccessLevel for enum source
     * @see ClasspathComponentImportService for cartesian product usage
     */
    public static final List<String> SUBDIR_FILE_PATHS = Arrays.stream(FrontendResource.AccessLevel.values()).map(FrontendResource.AccessLevel::getPath).toList();

}
