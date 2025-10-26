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

package com.openkoda.repository;

import com.openkoda.core.helper.ApplicationContextProvider;
import com.openkoda.core.job.JobsScheduler;
import com.openkoda.core.repository.common.ScopedSecureRepository;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.common.SearchableEntity;
import com.openkoda.model.common.SearchableOrganizationRelatedEntity;
import com.openkoda.model.common.SearchableRepositoryMetadata;
import jakarta.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.openkoda.model.common.ModelConstants.INDEX_STRING_COLUMN;
import static com.openkoda.model.common.ModelConstants.UPDATED_ON;

/**
 * Startup discovery utility enumerating SecureRepository beans and caching searchable entity metadata for dynamic indexing.
 * <p>
 * This utility class discovers all {@link SecureRepository} beans from the Spring ApplicationContext at startup or lazy initialization,
 * enforcing that each repository interface is annotated with {@link SearchableRepositoryMetadata}. It resolves physical table names
 * using the {@link Table} annotation or Hibernate's {@link CamelCaseToUnderscoresNamingStrategy} when the annotation is absent.
 * </p>
 * <p>
 * The discovery process formats and caches per-entity UPDATE SQL templates for index maintenance, combining the {@code UPDATE_INDEX_QUERY}
 * pattern with the {@code INDEX_STRING_COLUMN} constant. These templates are stored in fast lookup maps and arrays for both static and
 * dynamic searchable entities, enabling efficient batch index updates via {@link JobsScheduler#searchIndexUpdaterJob()}.
 * </p>
 * <p>
 * Discovery is synchronized once for thread-safety during initialization, with the {@code discoveryCompleted} flag preventing duplicate
 * discovery. The class provides {@code registerSearchableRepository} for runtime plugin registration of dynamically generated entities.
 * </p>
 * <p>
 * Used by search indexing services and {@link SecureEntityDictionaryRepository} for global search functionality in
 * {@link com.openkoda.controller.GlobalSearchController}.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SearchableRepositoryMetadata
 * @see SecureRepository
 * @see SearchableEntity
 * @see SearchableOrganizationRelatedEntity
 */
public class SearchableRepositories {


    /**
     * SQL template for updating the search index string column of searchable entities.
     * <p>
     * Format: {@code UPDATE %s SET %s = (%s) where (CURRENT_TIMESTAMP - %s < interval '00:01:01')}
     * </p>
     * <p>
     * Parameters: table name, index column name, search index formula, updated timestamp column.
     * The WHERE clause limits updates to recently modified records (within 61 seconds) for efficiency.
     * </p>
     */
    public static final String UPDATE_INDEX_QUERY = "UPDATE %s SET %s = (%s) where (CURRENT_TIMESTAMP - %s < interval '00:01:01')";
    
    /**
     * Array of all discovered searchable repository instances.
     * Populated during {@link #discoverSearchableRepositories()} startup discovery.
     */
    private static SecureRepository<?>[] searchableRepositories = {};
    
    /**
     * Array of searchable repositories included in global search functionality.
     * Filtered subset where {@link SearchableRepositoryMetadata#includeInGlobalSearch()} returns true.
     */
    private static SecureRepository<?>[] globalSearchableRepositories = {};

    /**
     * Flag indicating whether repository discovery has completed.
     * Prevents duplicate discovery when {@link #discoverSearchableRepositories()} is called multiple times.
     */
    private static boolean discoveryCompleted = false;

    /**
     * Array of UPDATE SQL queries for updating the indexString field of static entities implementing {@link SearchableEntity}.
     * <p>
     * Used for global search in {@link com.openkoda.controller.GlobalSearchController}.
     * The queries update the indexString continuously as configured in {@link JobsScheduler#searchIndexUpdaterJob()}.
     * </p>
     */
    private static String[] searchIndexUpdates = {};
    
    /**
     * List of UPDATE SQL queries for dynamically registered searchable entities.
     * Populated via {@link #registerSearchableRepository(String, SecureRepository)} for runtime-generated entities.
     */
    private static List<String> searchIndexUpdatesForDynamicEntities = new ArrayList<>();
    
    /**
     * Map of dynamic entity keys to their entity classes.
     * Stores dynamically registered searchable entities for runtime lookup.
     */
    private static Map<String, Class> dynamicEntities = new HashMap<>();
    
    /**
     * Hibernate naming strategy for converting Java class names to database table names.
     * Converts camelCase class names to underscore_separated table names when {@link Table} annotation is absent.
     */
    private static final CamelCaseToUnderscoresNamingStrategy camelCaseToUnderscoredNamingStrategy = new CamelCaseToUnderscoresNamingStrategy();

    /**
     * Discovers and caches all searchable repository beans from the Spring ApplicationContext at application startup.
     * <p>
     * This synchronized method performs a one-time discovery of all {@link SecureRepository} beans, validates that each
     * is annotated with {@link SearchableRepositoryMetadata}, and builds internal caches for fast lookup. The discovery
     * process resolves physical table names and generates UPDATE SQL templates for search index maintenance.
     * </p>
     * <p>
     * Discovery steps:
     * <ol>
     *   <li>Check {@code discoveryCompleted} flag to prevent duplicate execution</li>
     *   <li>Retrieve all SecureRepository beans from ApplicationContext</li>
     *   <li>Validate SearchableRepositoryMetadata annotation presence (throws RuntimeException if missing)</li>
     *   <li>Count total and global-searchable repositories</li>
     *   <li>Allocate arrays for searchable repositories and search index update queries</li>
     *   <li>Populate arrays with repository instances and formatted UPDATE SQL</li>
     *   <li>Build lookup maps by entity key and entity class</li>
     *   <li>Set {@code discoveryCompleted} flag to true</li>
     * </ol>
     * </p>
     * <p>
     * Thread-safety: Method is synchronized to prevent race conditions during concurrent startup initialization.
     * </p>
     *
     * @throws RuntimeException if any SecureRepository bean lacks the required SearchableRepositoryMetadata annotation
     * @see SearchableRepositoryMetadata
     * @see ApplicationContextProvider#getContext()
     */
    public synchronized static void discoverSearchableRepositories() {
        if (discoveryCompleted) {
            return;
        }
        ApplicationContext c = ApplicationContextProvider.getContext();

        Map<String, SecureRepository> searchableRepositoryBeans =
                c.getBeansOfType(SecureRepository.class);

        int searchableRepositoriesCount = 0;
        int globalSearchableRepositoriesCount = 0;

        //Count repositories
        for (Map.Entry<String, SecureRepository> e : searchableRepositoryBeans.entrySet() ) {
            //skipping GlobalSearchRepository
            if ("globalSearchRepository".equals(e.getKey())) {
                continue;
            }
            SearchableRepositoryMetadata gsa = getGlobalSearchableRepositoryAnnotation(e.getValue());
            if (gsa == null) {
                String message = String.format("Repository %s have to be annotated with SearchableRepositoryMetadata", e.getValue().getClass().getName());
                throw new RuntimeException(message);
            }
            searchableRepositoriesCount++;
            if (gsa.includeInGlobalSearch()) {
                globalSearchableRepositoriesCount++;
            }
        }
        searchableRepositories = new SecureRepository[searchableRepositoriesCount];
        searchIndexUpdates = new String[searchableRepositoriesCount];
        globalSearchableRepositories = new SecureRepository[globalSearchableRepositoriesCount];

        int sk = 0;
        int gsk = 0;
        for (Map.Entry<String, SecureRepository> e : searchableRepositoryBeans.entrySet() ) {
            //skipping GlobalSearchRepository
            if ("globalSearchRepository".equals(e.getKey())) {
                continue;
            }
            SearchableRepositoryMetadata gsa = getGlobalSearchableRepositoryAnnotation(e.getValue());
            String tableName = discoverTableName(gsa.entityClass());

            searchIndexUpdates[sk] = String.format(UPDATE_INDEX_QUERY, tableName, INDEX_STRING_COLUMN, gsa.searchIndexFormula(), UPDATED_ON);
            searchableRepositories[sk++] = e.getValue();
            if (gsa.includeInGlobalSearch()) {
                globalSearchableRepositories[gsk++] = e.getValue();
            }
            searchableRepositoryByEntityKey.put(gsa.entityKey(), e.getValue());
            searchableRepositoryMetadataByEntityKey.put(gsa.entityKey(), gsa);
            searchableRepositoryMetadataByEntityClass.put(gsa.entityClass(), gsa);
            if (SearchableOrganizationRelatedEntity.class.isAssignableFrom(gsa.entityClass())) {
                searchableOrganizationRelatedRepositoryMetadataByEntityKey.put(gsa.entityKey(), gsa);
                searchableOrganizationRelatedRepositoryMetadataByEntityClass.put(gsa.entityClass(), gsa);
            }
//            CustomPostgreSQLDialect.registerDescriptionFunction(gsa);
        }
        discoveryCompleted = true;
    }


    /**
     * Returns the array of all discovered searchable repository instances.
     * <p>
     * This array is populated during {@link #discoverSearchableRepositories()} and contains all SecureRepository beans
     * that are annotated with {@link SearchableRepositoryMetadata}.
     * </p>
     *
     * @return array of searchable repository instances, empty array before discovery completes
     */
    public static ScopedSecureRepository<?>[] getSearchableRepositories() {
        return searchableRepositories;
    }

    /**
     * Returns a map of searchable repositories indexed by their entity keys.
     * <p>
     * The entity key is obtained from {@link SearchableRepositoryMetadata#entityKey()} and provides fast lookup
     * of repositories by their associated entity identifier.
     * </p>
     *
     * @return map of entity keys to searchable repository instances
     */
    public static Map<String, SecureRepository> getSearchableRepositoriesWithEntityKeys() {
        return searchableRepositoryByEntityKey;
    }

    /**
     * Retrieves the SearchableRepositoryMetadata annotation from a scoped secure repository.
     * <p>
     * Delegates to {@link ScopedSecureRepository#getSearchableRepositoryMetadata()} to obtain the metadata
     * that defines search indexing behavior and entity characteristics.
     * </p>
     *
     * @param r the scoped secure repository instance
     * @return the SearchableRepositoryMetadata annotation associated with the repository
     */
    public static SearchableRepositoryMetadata getGlobalSearchableRepositoryAnnotation(ScopedSecureRepository r) {
        return r.getSearchableRepositoryMetadata();
    }

    /**
     * Map of entity keys to searchable repository instances.
     * Populated during {@link #discoverSearchableRepositories()} for fast repository lookup by entity identifier.
     */
    private static final Map<String, SecureRepository> searchableRepositoryByEntityKey = new HashMap<>();
    
    /**
     * Map of entity keys to their SearchableRepositoryMetadata annotations.
     * Enables quick metadata retrieval without repository instance lookup.
     */
    private static final Map<String, SearchableRepositoryMetadata> searchableRepositoryMetadataByEntityKey = new HashMap<>();
    
    /**
     * Map of entity classes to their SearchableRepositoryMetadata annotations.
     * Enables metadata lookup by entity class type for type-safe operations.
     */
    private static final Map<Class, SearchableRepositoryMetadata> searchableRepositoryMetadataByEntityClass = new HashMap<>();
    
    /**
     * Map of organization-related entity keys to their SearchableRepositoryMetadata annotations.
     * Filtered subset containing only entities implementing {@link SearchableOrganizationRelatedEntity}.
     */
    private static final Map<String, SearchableRepositoryMetadata> searchableOrganizationRelatedRepositoryMetadataByEntityKey = new HashMap<>();
    
    /**
     * Map of organization-related entity classes to their SearchableRepositoryMetadata annotations.
     * Filtered subset for type-safe lookup of organization-scoped searchable entities.
     */
    private static final Map<Class, SearchableRepositoryMetadata> searchableOrganizationRelatedRepositoryMetadataByEntityClass = new HashMap<>();
    
    /**
     * Retrieves a scoped searchable repository for the specified entity key and security scope.
     * <p>
     * Returns a {@link SecureRepositoryWrapper} that enforces the provided security scope on all repository operations.
     * Used to obtain privilege-checked repository access within a specific organizational or global context.
     * </p>
     *
     * @param entityKey the entity identifier as defined in {@link SearchableRepositoryMetadata#entityKey()}
     * @param scope the security scope to enforce on repository operations
     * @return scoped secure repository wrapped with security scope enforcement, or null if entity key not found
     * @see SecureRepositoryWrapper
     * @see HasSecurityRules.SecurityScope
     */
    public static ScopedSecureRepository getSearchableRepository(String entityKey, HasSecurityRules.SecurityScope scope) {
        return new SecureRepositoryWrapper(searchableRepositoryByEntityKey.get(entityKey), scope);
    }

    /**
     * Registers a dynamically generated searchable repository at runtime.
     * <p>
     * This method enables plugin-style registration of repositories for entities generated at runtime (e.g., via Byte Buddy).
     * It extracts the {@link SearchableRepositoryMetadata} from the repository, adds it to all lookup maps, generates an
     * UPDATE SQL query for index maintenance, and registers the entity class in the dynamic entities map.
     * </p>
     * <p>
     * Used by the dynamic entity generation subsystem to integrate runtime-generated entities into the global search infrastructure.
     * </p>
     *
     * @param tableName the physical database table name for the entity
     * @param repository the SecureRepository instance for the dynamic entity, must have SearchableRepositoryMetadata annotation
     * @see SearchableRepositoryMetadata
     * @see com.openkoda.service.dynamicentity.DynamicEntityRegistrationService
     */
    public static void registerSearchableRepository(String tableName, SecureRepository repository) {
        SearchableRepositoryMetadata gsa = getGlobalSearchableRepositoryAnnotation(repository);
        searchableRepositoryByEntityKey.put(gsa.entityKey(), repository);
        searchableRepositoryMetadataByEntityKey.put(gsa.entityKey(), gsa);
        searchableRepositoryMetadataByEntityClass.put(gsa.entityClass(), gsa);
        searchIndexUpdatesForDynamicEntities.add(String.format(UPDATE_INDEX_QUERY, tableName, INDEX_STRING_COLUMN, gsa.searchIndexFormula(), UPDATED_ON));
        dynamicEntities.put(gsa.entityKey(), gsa.entityClass());
    }

    /**
     * Retrieves the entity class for a searchable repository identified by entity key.
     * <p>
     * Looks up the {@link SearchableRepositoryMetadata} by entity key and returns the associated entity class.
     * Used for dynamic entity instantiation and type resolution in generic repository operations.
     * </p>
     *
     * @param entityKey the entity identifier as defined in {@link SearchableRepositoryMetadata#entityKey()}
     * @return the entity class implementing {@link SearchableEntity}, or null if entity key not found
     */
    public static Class<SearchableEntity> getSearchableRepositoryEntityClass(String entityKey) {
        SearchableRepositoryMetadata gsa = searchableRepositoryMetadataByEntityKey.get(entityKey);

        if (gsa == null) { return null; }

        return (Class<SearchableEntity>) gsa.entityClass();
    }
    
    /**
     * Retrieves the SearchableRepositoryMetadata annotation for the specified entity key.
     * <p>
     * Provides fast lookup of metadata defining search indexing behavior, entity characteristics, and global search inclusion.
     * </p>
     *
     * @param entityKey the entity identifier as defined in {@link SearchableRepositoryMetadata#entityKey()}
     * @return the SearchableRepositoryMetadata annotation, or null if entity key not found
     */
    public static SearchableRepositoryMetadata getSearchableRepositoryMetadata(String entityKey) {
        return searchableRepositoryMetadataByEntityKey.get(entityKey);
    }
    
    /**
     * Retrieves the SearchableRepositoryMetadata annotation for the specified entity class.
     * <p>
     * Enables type-safe metadata lookup by entity class for compile-time type checking in generic repository operations.
     * </p>
     *
     * @param entityClass the entity class implementing {@link SearchableEntity}
     * @return the SearchableRepositoryMetadata annotation, or null if entity class not registered
     */
    public static SearchableRepositoryMetadata getSearchableRepositoryMetadata(Class entityClass) {
        return searchableRepositoryMetadataByEntityClass.get(entityClass);
    }

    /**
     * Returns the array of UPDATE SQL queries for maintaining search indexes on static entities.
     * <p>
     * Each query updates the {@code indexString} column for recently modified records using the formula defined in
     * {@link SearchableRepositoryMetadata#searchIndexFormula()}. These queries are executed periodically by
     * {@link JobsScheduler#searchIndexUpdaterJob()} to keep search indexes current.
     * </p>
     *
     * @return array of formatted UPDATE SQL queries for static searchable entities
     * @see #UPDATE_INDEX_QUERY
     */
    public static String[] getSearchIndexUpdates() {
        return searchIndexUpdates;
    }

    /**
     * Returns the list of UPDATE SQL queries for maintaining search indexes on dynamically registered entities.
     * <p>
     * Similar to {@link #getSearchIndexUpdates()}, but contains queries for entities registered at runtime via
     * {@link #registerSearchableRepository(String, SecureRepository)}. Used for dynamic entity search indexing.
     * </p>
     *
     * @return mutable list of formatted UPDATE SQL queries for dynamic searchable entities
     */
    public static List<String> getSearchIndexUpdatesForDynamicEntities() {
        return searchIndexUpdatesForDynamicEntities;
    }
    
    /**
     * Discovers the physical database table name for the specified entity class.
     * <p>
     * Resolution strategy:
     * <ol>
     *   <li>If entity class has {@link Table} annotation with non-blank name, use that name</li>
     *   <li>Otherwise, convert the class simple name from camelCase to underscore_separated format using
     *       {@link CamelCaseToUnderscoresNamingStrategy}</li>
     * </ol>
     * </p>
     * <p>
     * Example: {@code Organization.class} resolves to {@code "organization"} table, or uses {@link Table#name()} if present.
     * </p>
     *
     * @param c the entity class to resolve the table name for
     * @return the physical database table name
     */
    public static String discoverTableName(Class c) {
        Table table = (Table) c.getAnnotation(Table.class);
        if ( table != null && StringUtils.isNotBlank(table.name())) {
            return table.name();
        }
        Identifier i = camelCaseToUnderscoredNamingStrategy.toPhysicalTableName(Identifier.toIdentifier(c.getSimpleName()), null);
        return i.getText();
    }

    /**
     * Returns an array of entity keys for all dynamically registered searchable repositories.
     * <p>
     * Contains entity keys added via {@link #registerSearchableRepository(String, SecureRepository)} for runtime-generated
     * entities such as those created by the Byte Buddy dynamic entity generation subsystem.
     * </p>
     *
     * @return array of entity keys for dynamic searchable entities
     */
    public static String[] getDynamicSearchableRepositoriesEntityKeys() {
        return dynamicEntities.keySet().toArray(new String[0]);
    }

    /**
     * Returns the map of dynamic entity keys to their entity classes.
     * <p>
     * Provides access to the complete registry of dynamically registered searchable entities, enabling runtime
     * type resolution and entity class lookup for plugin-style entity generation.
     * </p>
     *
     * @return map of entity keys to entity classes for dynamic searchable entities
     */
    public static Map<String, Class> getDynamicSearchableRepositoriesMap() {
        return dynamicEntities;
    }

}
