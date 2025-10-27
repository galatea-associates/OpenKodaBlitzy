package com.openkoda.service.export;

import com.openkoda.App;
import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.core.job.SearchIndexUpdaterJob;
import com.openkoda.core.multitenancy.MultitenancyService;
import com.openkoda.model.OpenkodaModule;
import com.openkoda.model.component.Form;
import com.openkoda.model.component.event.EventListenerEntry;
import com.openkoda.service.dynamicentity.DynamicEntityRegistrationService;
import com.openkoda.service.export.dto.ComponentDto;
import com.openkoda.service.export.dto.FormConversionDto;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.openkoda.service.export.FolderPathConstants.*;
import static java.util.stream.Collectors.toMap;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * Imports OpenKoda component archives (ZIP files) containing YAML definitions, code resources, and database migration scripts into the running application.
 * <p>
 * Extends {@link YamlComponentImportService} to process ZIP uploads via {@link MultipartFile}, extracts YAML component definitions and code/resource files,
 * validates form column mappings against existing database schema, delegates entity conversion to {@link YamlToEntityConverterFactory}, executes database
 * ALTER queries for schema changes, triggers search index refresh, and conditionally restarts Spring context when dynamic entity registration is required.
 * </p>
 * <p>
 * Individual operations are wrapped in REQUIRES_NEW transactions for isolation. Validation failures prevent import and return early with diagnostic messages.
 * When updateQuery is non-empty (database schema changed), triggers {@link App#restart()} to reload dynamic entity classes, causing application downtime.
 * </p>
 * <p>
 * This service is stateless and safe for concurrent use as a Spring singleton.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * String report = service.loadResourcesFromZip(uploadedZipFile, false);
 * }</pre>
 * </p>
 *
 * @see YamlComponentImportService for base conversion infrastructure
 * @see DynamicEntityRegistrationService for dynamic entity generation
 * @see SearchIndexUpdaterJob for search index refresh
 * @see FolderPathConstants for ZIP archive path constants
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Service
public class ZipComponentImportService extends YamlComponentImportService {

    /**
     * Job service that rebuilds search indexes after component imports to ensure new content is discoverable.
     *
     * @see SearchIndexUpdaterJob
     */
    @Inject
    SearchIndexUpdaterJob searchIndexUpdaterJob;

    /**
     * Service for executing database updates across all tenant schemas in multi-tenancy mode.
     *
     * @see MultitenancyService
     */
    @Inject
    MultitenancyService multitenancyService;

    /**
     * Extracts, validates, and imports component definitions from a ZIP archive, optionally deleting existing components with matching module names.
     * <p>
     * Streams ZIP entries, parses .yaml files using SnakeYAML, collects code/resource files from {@code EXPORT_CODE_PATH_} and {@code EXPORT_RESOURCES_PATH_},
     * validates form database mappings via {@link com.openkoda.service.DatabaseValidationService}, delegates entity persistence to {@link YamlToEntityConverterFactory},
     * executes accumulated database ALTER scripts, refreshes search indexes, and restarts application context if dynamic entities were modified.
     * </p>
     * <p>
     * On validation failure, returns early with validation log and does NOT persist components. May trigger {@link App#restart()} causing application downtime.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * String result = service.loadResourcesFromZip(multipartFile, true);
     * }</pre>
     * </p>
     *
     * @param zipFile MultipartFile containing ZIP archive with YAML components and resources; must not be null
     * @param delete when true, unregisters and deletes existing components for discovered modules before import; when false, updates/merges components
     * @return String import report with IMPORT header, validation messages, component processing log, and any errors; never null
     * @see YamlToEntityConverterFactory#processYamlDto for entity conversion logic
     * @see App#restart() for context restart mechanism
     */
    public String loadResourcesFromZip(MultipartFile zipFile, boolean delete) {
        debug("[loadResourcesFromZip] {}", zipFile.getName());
        Map<String, Object> configsFromZip = new HashMap<>();
        Map <String, String> componentResourcesFromZip = new HashMap<>();
        StringBuilder importNote = new StringBuilder(String.format("IMPORT %s \r\n", zipFile.getName()));
        try {
            ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipFile.getBytes()));
            ZipEntry entry;
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            while ((entry = zis.getNextEntry()) != null) {
                if(!entry.isDirectory()) {
                    StringBuilder content = new StringBuilder();
                    int read;
                    while ((read = zis.read(buffer, 0, bufferSize)) >= 0) {
                        content.append(new String(buffer, 0, read));
                    }
                    if (entry.getName().endsWith(".yaml")) {
                        configsFromZip.put(entry.getName(), new Yaml().load(content.toString()));
                    } else if (entry.getName().contains(EXPORT_CODE_PATH_) || entry.getName().contains(EXPORT_RESOURCES_PATH_)) {
                        componentResourcesFromZip.put(entry.getName().split(EXPORT_PATH)[1], content.toString());
                    }
                }
            }
            if(delete) {
                List<String> discoveredModules = configsFromZip.values().stream().map(o -> ((ComponentDto) o).getModule()).distinct().toList();
                unregisterComponents(discoveredModules, importNote);
                deleteComponents(discoveredModules, importNote);
            }

            StringBuilder validationLog = new StringBuilder("VALIDATION\r\n");
            StringBuilder updateQuery = new StringBuilder();
            Map<String, FrontendMappingDefinition> frontendMappingDefinitions = getFrontendMappingDefinitions(configsFromZip, componentResourcesFromZip);
            Map<String,String> formTableNames = getFormTableNames(configsFromZip);
            if (!frontendMappingDefinitions.entrySet().stream()
                    .allMatch(mappingDefinition -> services.databaseValidation.validateColumnTypes(formTableNames.get(mappingDefinition.getKey()), mappingDefinition.getValue().getFieldNameDbTypeMap(), validationLog, updateQuery))) {
                error("[loadResourcesFromZip] Validation error");
                return importNote.append(validationLog).toString();
            }

            List<Object> processedComponents = processComponentsFromZip(configsFromZip, componentResourcesFromZip, importNote);
            try {
                searchIndexUpdaterJob.updateSearchIndexes();
            } catch (Exception e) {
                warn("Error updating search engine during the import");
            }

//            assumption: that if no update query generated then we don't need to reload entities and restart the app
            if(!updateQuery.isEmpty()) {
                executeDatabaseUpdate(updateQuery.toString());
//            load entity classes and restart spring context
                debug("[loadResourcesFromZip] Context restart required");
                List<Form> createdForms = processedComponents.stream().filter(o -> o instanceof Form).map(o -> (Form) o).toList();

                DynamicEntityRegistrationService.generateDynamicEntityDescriptors(createdForms, frontendMappingDefinitions, System.currentTimeMillis());
                importNote.append("RESTARTING ...");
                App.restart();
            } else {
//                no conflicts requiring db changes, safe to register forms
                services.form.loadAllFormsFromDb(true);
            }

        } catch (IOException e) {
            error("[loadResourcesFromZip]", e);
            importNote.append("ERROR ");
            importNote.append(e.getMessage());
            return importNote.toString();
        }
        return importNote.toString();
    }

    /**
     * Extracts form names and their database table names from parsed YAML DTOs for validation.
     * <p>
     * Filters {@link FormConversionDto} entries from the configuration map and constructs a mapping of form names to table names.
     * This mapping is passed to {@code validateColumnTypes} for schema validation before persistence.
     * </p>
     *
     * @param configsFromZip Map of YAML file paths to deserialized DTO objects
     * @return Map of form name to table name for FormConversionDto entries; empty map if no forms
     * @see FormConversionDto
     */
    private Map<String, String> getFormTableNames(Map<String, Object> configsFromZip) {
        return configsFromZip.values().stream()
                .filter(o -> o instanceof FormConversionDto)
                .map(o -> (FormConversionDto) o)
                .collect(toMap(FormConversionDto::getName, FormConversionDto::getTableName));
    }

    /**
     * Constructs {@link FrontendMappingDefinition} objects for forms by parsing code resources and privilege metadata.
     * <p>
     * Delegates to {@code services.form.getFrontendMappingDefinition} to parse field definitions from form code resources.
     * The resulting mapping is used for database validation to ensure form fields match database column types.
     * </p>
     *
     * @param configsFromZip Map of YAML file paths to FormConversionDto objects
     * @param componentResourcesFromZip Map of resource paths to file contents (JavaScript code for forms)
     * @return Map of form name to FrontendMappingDefinition for database validation
     * @see FrontendMappingDefinition
     * @see FormConversionDto
     */
    private Map<String, FrontendMappingDefinition> getFrontendMappingDefinitions(Map<String, Object> configsFromZip, Map<String, String> componentResourcesFromZip) {
        return configsFromZip.values().stream()
                .filter(o -> o instanceof FormConversionDto)
                .map(o -> (FormConversionDto) o)
                .collect(toMap(FormConversionDto::getName,
                        dto -> services.form.getFrontendMappingDefinition(dto.getName(), dto.getReadPrivilege(), dto.getWritePrivilege(), componentResourcesFromZip.get(dto.getCode()))));
    }

    /**
     * Removes all existing component entities for specified modules from database, or creates module placeholder if not found.
     * <p>
     * Deletes {@link com.openkoda.model.component.ControllerEndpoint}, {@link com.openkoda.model.component.FrontendResource},
     * {@link Form}, {@link EventListenerEntry}, {@link com.openkoda.model.component.Scheduler}, and
     * {@link com.openkoda.model.component.ServerJs} entities associated with each module. Saves {@link OpenkodaModule} entities
     * to ensure module registration persists.
     * </p>
     * <p>
     * Called when {@code delete=true} to clear existing components before reimport.
     * </p>
     *
     * @param discoveredModules List of module names extracted from imported YAML files
     * @param importNote Mutable StringBuilder accumulating import operation log messages
     * @see OpenkodaModule
     */
    private void deleteComponents(List<String> discoveredModules, StringBuilder importNote) {
        debug("[deleteComponents]");
        for (String module : discoveredModules) {
            OpenkodaModule openkodaModule = repositories.unsecure.openkodaModule.findByName(module);
            if (openkodaModule != null) {
                importNote.append(String.format("DELETE existing components for module %s \r\n", module));
                repositories.unsecure.controllerEndpoint.deleteByModule(openkodaModule);
                repositories.unsecure.frontendResource.deleteByModule(openkodaModule);
                repositories.unsecure.form.deleteByModule(openkodaModule);
                repositories.unsecure.eventListener.deleteByModule(openkodaModule);
                repositories.unsecure.scheduler.deleteByModule(openkodaModule);
                repositories.unsecure.serverJs.deleteByModule(openkodaModule);
            } else {
                openkodaModule = new OpenkodaModule(module);
                importNote.append(String.format("CREATE module %s \r\n", module));
            }
            repositories.unsecure.openkodaModule.save(openkodaModule);
        }
    }

    /**
     * Unregisters runtime-registered components (forms, event listeners, schedulers) from cluster-aware in-memory registries before deletion.
     * <p>
     * Calls {@code removeClusterAware} and {@code unregisterEventListenerClusterAware} to cleanup in-memory state for forms, event listeners,
     * and schedulers associated with the specified modules. Must be called before {@link #deleteComponents} to prevent orphaned registrations.
     * </p>
     *
     * @param discoveredModules List of module names to unregister
     * @param importNote Mutable StringBuilder accumulating operation log
     * @see EventListenerEntry
     * @see Form
     */
    private void unregisterComponents(List<String> discoveredModules, StringBuilder importNote) {
        debug("[unregisterComponents]");
        for (String module : discoveredModules) {
            OpenkodaModule openkodaModule = repositories.unsecure.openkodaModule.findByName(module);
            if (openkodaModule != null) {
                importNote.append(String.format("UNREGISTER components for module %s \r\n", module));
                repositories.unsecure.form.findByModule(openkodaModule).forEach(componentEntity -> services.form.removeClusterAware(componentEntity.getId()));
                repositories.unsecure.eventListener.findByModule(openkodaModule).forEach(componentEntity -> services.eventListener.unregisterEventListenerClusterAware((EventListenerEntry) componentEntity));
                repositories.unsecure.scheduler.findByModule(openkodaModule).forEach(componentEntity -> services.scheduler.removeClusterAware(componentEntity.getId()));
            }
        }
    }

    /**
     * Executes database schema ALTER/UPDATE queries in a new transaction, propagating changes to all tenant schemas in multi-tenancy mode.
     * <p>
     * Uses {@code @Transactional(propagation = REQUIRES_NEW)} to ensure rollback isolation from outer transaction. If multi-tenancy is enabled,
     * delegates to {@link MultitenancyService#runEntityManagerForAllTenantsInTransaction} to apply changes to each tenant database.
     * SQLExceptions are logged to error level but not rethrown; caller should check database state.
     * </p>
     *
     * @param updateQuery SQL script (multiple statements separated by semicolons) to execute; must not be null or empty
     * @see MultitenancyService
     */
    @Transactional(propagation = REQUIRES_NEW)
    public void executeDatabaseUpdate(String updateQuery) {
        try {
            repositories.unsecure.nativeQueries.runUpdateQuery(updateQuery);
            if(MultitenancyService.isMultitenancy()) {
                multitenancyService.runEntityManagerForAllTenantsInTransaction(1000, (em, orgId) -> {
                    em.createNativeQuery(updateQuery).executeUpdate();
                    return true;
                });
            }
        } catch (SQLException e) {
            error("[executeDatabaseUpdate]", e);
        }
    }

    /**
     * Converts and persists YAML DTOs to domain entities by delegating to {@link YamlToEntityConverterFactory} for each entry.
     * <p>
     * Streams {@code configsFromZip} entries, logs component class and filename, delegates to
     * {@link YamlToEntityConverterFactory#processYamlDto} for conversion and persistence.
     * Returns a list of persisted domain entities with mixed types (Form, Privilege, FrontendResource, etc.).
     * </p>
     *
     * @param configsFromZip Map of YAML file paths to deserialized DTO objects
     * @param componentResourcesFromZip Map of resource paths to code/resource file contents
     * @param importNote Mutable StringBuilder accumulating processing log
     * @return List of persisted domain entities (mixed types: Form, Privilege, FrontendResource, etc.); never null
     * @see YamlToEntityConverterFactory#processYamlDto
     */
    private List<Object> processComponentsFromZip(Map<String, Object> configsFromZip, Map<String, String> componentResourcesFromZip, StringBuilder importNote) {
        return configsFromZip.entrySet().stream().map(entry -> {
            importNote.append(String.format("PROCESS component %s from file %s\r\n", entry.getValue().getClass(), entry.getKey()));
            return yamlToEntityConverterFactory.processYamlDto( entry.getValue(), entry.getKey(), componentResourcesFromZip);
        }).collect(Collectors.toList());
    }

}
