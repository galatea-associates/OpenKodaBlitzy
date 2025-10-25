package com.openkoda.service.dynamicentity;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.multitenancy.MultitenancyService;
import com.openkoda.model.DynamicEntity;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * DDL provisioning service for creating and updating database tables for runtime dynamic entities.
 * <p>
 * This service handles tenant-aware table creation with multi-tenancy support by adding organization_id
 * foreign keys when multi-tenancy is enabled. Executes DDL operations via native queries and persists
 * metadata through the DynamicEntity model. All DDL operations use REQUIRES_NEW transaction propagation
 * to ensure DDL commits independently of the caller's transaction context.
 * </p>
 * <p>
 * For multi-tenant deployments, this service iterates all tenant EntityManagers via the
 * MultitenancyService callback pattern, checking table existence per tenant and creating missing tables
 * as needed. Tables are automatically registered with MultitenancyService for tenant-aware query routing.
 * </p>
 * <p>
 * Thread-safety: Operations execute within transactional boundaries managed by Spring's transaction
 * infrastructure. The REQUIRES_NEW propagation ensures DDL isolation from surrounding transactions.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * dynamicEntityService.createDynamicTableIfNotExists("dynamic_entity_form_123");
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see DynamicEntity
 * @see MultitenancyService
 * @see com.openkoda.repository.NativeQueries
 */
@Service
public class DynamicEntityService extends ComponentProvider {

    @Inject
    MultitenancyService multitenancyService;

    /**
     * Creates database table if not exists in default schema and all tenant schemas when multi-tenancy is enabled.
     * <p>
     * Executes CREATE TABLE DDL via native queries and persists DynamicEntity metadata record. For multi-tenant
     * deployments, iterates all tenant EntityManagers via MultitenancyService callback pattern, checking table
     * existence per tenant and creating missing tables. Registers table with MultitenancyService for tenant-aware
     * query routing.
     * </p>
     * <p>
     * Transaction behavior: This method uses {@code @Transactional(propagation=REQUIRES_NEW)} to ensure DDL
     * commits independently of the caller's transaction. This isolation is critical for DDL operations that
     * cannot be rolled back in most database systems.
     * </p>
     *
     * @param tableName database table name for the dynamic entity (e.g., 'dynamic_entity_form_123'). Must be a
     *                  valid SQL identifier following database naming conventions.
     * @return always returns true; side-effects (table creation, metadata persistence) indicate actual outcome.
     *         Check database state or DynamicEntity repository to verify successful creation.
     * @throws org.springframework.dao.DataAccessException if DDL execution fails due to SQL syntax errors,
     *         permission issues, or database connectivity problems. Caller should handle by logging error
     *         and notifying user of table creation failure.
     */
    @Transactional(propagation = REQUIRES_NEW)
    public boolean createDynamicTableIfNotExists(String tableName) {
        if (!repositories.unsecure.nativeQueries.ifTableExists(tableName)) {
            repositories.unsecure.nativeQueries.createTable(tableName);
            repositories.unsecure.dynamicEntity.save(create(tableName));
        }
        if (MultitenancyService.isMultitenancy()) {
            String tableExistsSql = repositories.unsecure.nativeQueries.tableExistsSql();
            String tableSql = repositories.unsecure.nativeQueries.createTableSql(tableName);

            multitenancyService.runEntityManagerForAllTenantsInTransaction(1000, (em, orgId) -> {
                Boolean exists = (Boolean) em.createNativeQuery(tableExistsSql, Boolean.class).setParameter("tableName", tableName).getSingleResult();
                if (exists == null || !exists) {
                    em.createNativeQuery(tableSql).executeUpdate();
                    return true;
                }
                return false;
            });
//            notice table added
            multitenancyService.addTenantedTables(Collections.singletonList(tableName));
        }
        return true;
    }
    
    /**
     * Returns insertion-ordered map of all available entity types for UI selection.
     * <p>
     * The first entry is plain String.class for backward compatibility. Subsequent entries are populated from
     * the dynamicEntityClasses registry with format: 'entityKey (fully.qualified.ClassName)'. The map maintains
     * insertion order via LinkedHashMap for consistent UI presentation.
     * </p>
     * <p>
     * Used by UI components for dynamic entity type dropdown population, allowing users to select entity types
     * when creating forms or configuring dynamic entities.
     * </p>
     *
     * @return LinkedHashMap keyed by class name (String) or entity key (String) with display label values.
     *         Map is never null but may contain only the default String.class entry if no dynamic entities
     *         are registered.
     */
    public Map<Object, String> getAll() {
        Map<Object, String> eventsClasses = new LinkedHashMap<>();
        eventsClasses.put(String.class.getName(), String.format("Plain String (%s)", String.class.getName()));
        services.dynamicEntityRegistration.dynamicEntityClasses.entrySet() .stream().forEach( de -> {
            eventsClasses.put(de.getValue().getName(), String.format(" %s (%s)", de.getKey(), de.getValue().getName()));
        });
        
        return eventsClasses;
    }

    /**
     * Factory method creating DynamicEntity metadata object.
     * <p>
     * Sets tableName property on the new instance. Does not persist to database - persistence is the
     * caller's responsibility (typically via {@link #createDynamicTableIfNotExists(String)}).
     * </p>
     *
     * @param tableName database table name to set on the DynamicEntity metadata object
     * @return new DynamicEntity instance (not persisted) with tableName property initialized
     */
    private DynamicEntity create(String tableName) {
        DynamicEntity dynamicEntity = new DynamicEntity();
        dynamicEntity.setTableName(tableName);
        return dynamicEntity;
    }
}
