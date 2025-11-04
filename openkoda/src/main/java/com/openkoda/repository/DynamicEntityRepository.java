package com.openkoda.repository;

import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.model.DynamicEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Repository managing DynamicEntity definitions for runtime-generated JPA entities.
 * <p>
 * This repository exposes {@code getTableNames()} JPQL query and PostgreSQL-native
 * {@code getDynamicTablesColumnNamesQuery()} for schema introspection. It provides a default
 * helper method {@code getDynamicTablesColumnNames()} that converts List&lt;Object[]&gt; results
 * to a Map&lt;String,String&gt; via stream collectors. The native query uses PostgreSQL system
 * catalogs (pg_tables and information_schema.columns) and is not portable across database dialects.
 * 
 * <p>
 * This repository is used by DynamicEntityRegistrationService during Byte Buddy entity generation
 * to discover existing dynamic table schemas and synchronize runtime JPA entity definitions with
 * database structures.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see DynamicEntity
 * @see com.openkoda.service.dynamicentity.DynamicEntityRegistrationService
 * @see org.springframework.data.jpa.repository.JpaRepository
 */
@Repository
public interface DynamicEntityRepository extends UnsecuredFunctionalRepositoryWithLongId<DynamicEntity>{

    /**
     * Retrieves all dynamic entity table names from DynamicEntity definitions.
     * <p>
     * Executes a JPQL projection query that extracts the {@code tableName} field values
     * from all DynamicEntity records. This method is used to identify which database tables
     * correspond to runtime-generated entities.
     * 
     *
     * @return list of table name strings from DynamicEntity tableName field
     */
    @Query(value="select de.tableName from DynamicEntity de")
    List<Object> getTableNames();
    /**
     * Executes PostgreSQL-native query to introspect column names for dynamic tables.
     * <p>
     * This native query joins {@code pg_tables} and {@code information_schema.columns} system
     * catalogs to retrieve table and column name pairs for all tables referenced in DynamicEntity
     * records. Results are aggregated using {@code string_agg()} to produce comma-separated
     * column lists per table.
     * 
     * <p>
     * <b>Warning:</b> This query uses PostgreSQL-specific system catalog tables (pg_tables and
     * information_schema.columns) and is not portable to other database dialects. Migration to
     * Oracle, MySQL, or SQL Server requires dialect-specific rewrites.
     * 
     *
     * @return list of Object[] arrays where [0] is table name (String) and [1] is comma-separated
     *         column names (String) from information_schema
     */
    @Query(nativeQuery = true, value = """
            select table_name, string_agg(' ' || column_name, ',') from
               (SELECT table_name, column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name IN
               (select distinct tablename from pg_tables
                inner join dynamic_entity as de on de.table_name=pg_tables.tablename and pg_tables.schemaname='public')) x
            group by table_name
            """)
    List<Object[]> getDynamicTablesColumnNamesQuery();

    /**
     * Helper method converting column query results to table-to-column-list map.
     * <p>
     * Streams the Object[] results from {@code getDynamicTablesColumnNamesQuery()} and collects
     * them into a Map where keys are table names and values are comma-separated column lists.
     * This default method implementation uses {@code Collectors.toMap()} to group column names
     * by table name for convenient lookup during dynamic entity registration.
     * 
     *
     * @return map of table names (String) to comma-separated column names (String)
     */
    default Map<String,String> getDynamicTablesColumnNames(){
        return getDynamicTablesColumnNamesQuery().stream().collect(Collectors.toMap(l -> (String) ((Object[])l)[0], l ->  (String)((Object[])l)[1]));
    }
}
