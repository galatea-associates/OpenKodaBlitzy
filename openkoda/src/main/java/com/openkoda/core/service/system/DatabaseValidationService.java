package com.openkoda.core.service.system;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.customisation.FrontendMapping;
import com.openkoda.core.customisation.FrontendMappingMap;
import com.openkoda.core.form.FieldType;
import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.core.form.FrontendMappingFieldDefinition;
import com.openkoda.core.helper.NameHelper;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.openkoda.core.helper.NameHelper.toColumnName;
import static java.util.stream.Collectors.toSet;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Schema-reconciliation service that compares application {@link FrontendMappingDefinition} metadata
 * against live JDBC database schema and generates safe SQL update scripts without execution.
 * <p>
 * This service validates the database state against registered {@link FrontendMappingFieldDefinition}
 * entries, builds ALTER TABLE statements for missing columns, and handles foreign key constraints
 * for organization_select fields. The generated DDL scripts are safe to review and execute manually.

 * <p>
 * Core responsibilities include:
 * <ul>
 *   <li>Comparing FrontendMapping field definitions against actual database columns</li>
 *   <li>Generating ALTER TABLE ADD COLUMN IF NOT EXISTS statements for missing columns</li>
 *   <li>Creating foreign key constraints for organization_select field types</li>
 *   <li>Validating column type consistency between application metadata and database schema</li>
 *   <li>Producing validation logs identifying schema inconsistencies</li>
 * </ul>

 * <p>
 * SQL generation patterns:
 * <ul>
 *   <li>Uses ALTER TABLE IF NOT EXISTS for safe idempotent DDL</li>
 *   <li>Generates CRLF (\\r\\n) line terminators for cross-platform compatibility</li>
 *   <li>Creates foreign key constraints with fk_ prefix for organization_select fields</li>
 *   <li>References organization(id) for organizational scoping</li>
 * </ul>

 * <p>
 * Exception handling: {@link SQLException} instances are caught and rethrown as {@link RuntimeException}.
 * Database connections are always released via {@link DataSourceUtils#releaseConnection} in finally blocks
 * to prevent connection leaks.

 * <p>
 * Thread-safety: This service is effectively stateless (namingStrategy is immutable after initialization)
 * and safe for concurrent calls. Multiple threads can safely invoke validation and script generation methods.

 * <p>
 * Integration points:
 * <ul>
 *   <li>Extends {@link ComponentProvider} for access to repositories and services</li>
 *   <li>Uses {@code repositories.unsecure.form} to retrieve form entity table names</li>
 *   <li>Applies {@link NameHelper#toColumnName} for field name to column name conversion</li>
 *   <li>Processes {@link FrontendMappingFieldDefinition} metadata for type information</li>
 *   <li>Accesses {@link DataSource} via Spring JDBC utilities for connection management</li>
 * </ul>

 * <p>
 * Example usage showing typical validation workflow with form import:
 * <pre>{@code
 * String updateScript = databaseValidationService.getUpdateScript(true);
 * // Review generated SQL script for ALTER TABLE statements
 * // Execute script manually or via database migration tool
 * }</pre>

 *
 * @author OpenKoda Team
 * @since 1.7.1
 * @see FrontendMappingDefinition
 * @see FrontendMappingFieldDefinition
 * @see ComponentProvider
 */
@Service
public class DatabaseValidationService extends ComponentProvider {

    /**
     * JDBC data source for accessing database metadata.
     * Used to retrieve {@link DatabaseMetaData} for schema introspection via
     * {@link DataSourceUtils#getConnection}.
     */
    @Autowired
    private DataSource dataSource;

    /**
     * Registry of all registered {@link FrontendMapping} definitions.
     * Maps entity names to their corresponding frontend mapping metadata,
     * injected during application startup from all discovered form definitions.
     */
    @Inject
    FrontendMappingMap frontendMappingMap;

    /**
     * Hibernate naming strategy for converting Java field names to database column names.
     * Applies camel case to underscore conversion (e.g., organizationName becomes organization_name).
     * Initialized in {@link #init()} method via {@link PostConstruct}.
     */
    CamelCaseToUnderscoresNamingStrategy namingStrategy;

    /**
     * Initializes the naming strategy for field name to column name conversion.
     * Called automatically after dependency injection via {@link PostConstruct}.
     */
    @PostConstruct
    void init() {
        namingStrategy = new CamelCaseToUnderscoresNamingStrategy();
    }

    /**
     * Aggregates DDL update scripts for all registered {@link FrontendMapping} entries.
     * Validates current database state against all registered {@link FrontendMappingFieldDefinition}
     * and builds the complete update SQL script content by iterating through all entries
     * in the {@link FrontendMappingMap}.
     * <p>
     * This method retrieves form entity table names from {@code repositories.unsecure.form}
     * and delegates to {@link #getUpdateScript(FrontendMappingDefinition, String, boolean)}
     * for each registered mapping. Generated scripts use ALTER TABLE IF NOT EXISTS statements
     * for safe idempotent execution.

     *
     * @param includeOnlyMissingColumns if true, generates DDL only for columns not present in database;
     *                                  if false, generates DDL for all columns regardless of existence
     * @return concatenated SQL script content with ALTER TABLE statements for all registered entities,
     *         using CRLF line terminators; empty string if no mappings are registered
     * @see #getUpdateScript(FrontendMappingDefinition, String, boolean)
     */
    public String getUpdateScript(boolean includeOnlyMissingColumns) {
        debug("[validateDatabaseAndGetUpdateScript]");
        StringBuilder updateDatabaseScript = new StringBuilder();
        Map<String, String> tableNamesMap = repositories.unsecure.form.getNameAndTableNameAsMap();
        for(Map.Entry<String, FrontendMapping> frontendMappingEntry : frontendMappingMap.entrySet()) {
            String entityName = tableNamesMap.containsKey(frontendMappingEntry.getKey()) ? tableNamesMap.get(frontendMappingEntry.getKey()) : frontendMappingEntry.getKey();
            updateDatabaseScript.append(getUpdateScript(frontendMappingEntry.getValue().definition(), entityName, includeOnlyMissingColumns));
        }
        return updateDatabaseScript.toString();
    }

    /**
     * Generates per-entity DDL script for a specific {@link FrontendMappingDefinition}.
     * Compares the field definitions from the frontend mapping against the actual database
     * table schema and builds ALTER TABLE statements for missing or new columns.
     * <p>
     * For each field in the mapping (excluding files type), this method:
     * <ul>
     *   <li>Converts field name to column name using {@link NameHelper#toColumnName}</li>
     *   <li>Checks if column exists in the database table</li>
     *   <li>Generates ALTER TABLE ADD COLUMN IF NOT EXISTS statement if needed</li>
     *   <li>Creates foreign key constraint for organization_select field types</li>
     * </ul>

     * <p>
     * Foreign key generation for organization_select fields follows the pattern:
     * <pre>{@code
     * alter table <table_name> add constraint fk_<column_name>
     *   foreign key (<column_name>) references organization(id);
     * }</pre>

     *
     * @param frontendMappingDefinition the frontend mapping definition containing field metadata
     *                                  to validate against the database schema
     * @param tableName the database table name to validate; must match actual table name in database
     * @param includeOnlyMissingColumns if true, generates DDL only for columns not present in database;
     *                                  if false, generates DDL for all columns in the mapping
     * @return SQL script content with ALTER TABLE statements for the specified entity,
     *         using CRLF line terminators; empty string if no changes are needed
     * @throws RuntimeException if {@link SQLException} occurs during database metadata access;
     *                          original exception is wrapped and rethrown
     * @see FrontendMappingDefinition#getDbTypeFields()
     * @see FieldType#getDbType()
     */
    public String getUpdateScript(FrontendMappingDefinition frontendMappingDefinition, String tableName, boolean includeOnlyMissingColumns){
        StringBuilder updateDatabaseScript = new StringBuilder();
        Set<FrontendMappingFieldDefinition> fields = Arrays.stream(frontendMappingDefinition.getDbTypeFields())
                .collect(toSet());
        try {
            Map<String, String> tableColumns = getTableColumns(tableName);
            for(FrontendMappingFieldDefinition field : fields.stream().filter(f -> f.getType() != FieldType.files).toList()) {
                String columnName = toColumnName(field.getName());
                boolean addColumn = !includeOnlyMissingColumns || !tableColumns.containsKey(columnName);
                if(addColumn) {
                    String dbType = field.getType().getDbType().getValue();
                    updateDatabaseScript.append(String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS %s %s;\r\n", tableName, columnName, dbType));
                    if(field.getType().equals(FieldType.organization_select)) {
                        updateDatabaseScript.append(String.format("alter table %s add constraint %s foreign key (%s) references %s(%s);\r\n",
                                tableName,
                                "fk_" + columnName,
                                columnName,
                                "organization",
                                "id"
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return updateDatabaseScript.toString();
    }

    /**
     * Validates provided column names and types against current database state.
     * Compares the expected column definitions from {@link FrontendMappingFieldDefinition}
     * against actual database table metadata retrieved via JDBC.
     * <p>
     * This method performs three types of validation:
     * <ul>
     *   <li>Table existence check - logs if table does not exist</li>
     *   <li>Column presence check - generates ALTER TABLE DDL for missing columns</li>
     *   <li>Column type consistency check - validates database type matches expected type</li>
     * </ul>

     * <p>
     * For missing columns, this method appends ALTER TABLE ADD COLUMN IF NOT EXISTS statements
     * to the updateDatabaseScript parameter. For organization_select fields, it additionally
     * generates foreign key constraint statements referencing organization(id).

     * <p>
     * All validation findings are appended to the validationLog parameter with descriptive
     * messages. Type mismatches result in false return value while missing columns do not
     * affect validation success (they are handled via generated DDL).

     *
     * @param tableName the database table name to validate; if blank, validation is skipped
     * @param columns map of field names to {@link FrontendMappingFieldDefinition} containing
     *                expected field metadata; files type fields are automatically filtered out
     * @param validationLog mutable StringBuilder to append validation messages and findings;
     *                      includes table existence, missing columns, and type mismatches
     * @param updateDatabaseScript mutable StringBuilder to append generated ALTER TABLE statements
     *                             for missing columns and required foreign key constraints
     * @return true when all present columns have matching types (missing columns do not affect result);
     *         false if any existing column has type mismatch with expected definition
     * @throws RuntimeException if {@link SQLException} occurs during database metadata access;
     *                          connection is always released via finally block before rethrowing
     * @see #getTableColumns(String)
     * @see FieldType#getDbType()
     */
    public Boolean validateColumnTypes(String tableName, Map<String, FrontendMappingFieldDefinition> columns, StringBuilder validationLog, StringBuilder updateDatabaseScript) {
        debug("[validateColumnTypes]");
        boolean validationSuccess = true;
        if(isNotBlank(tableName)) {
        try {
            Map<String, String> tableColumns = getTableColumns(tableName);
            if(tableColumns.isEmpty()) {
                validationLog.append(String.format("Table %s does not exist. Will be created on form import.\r\n", tableName));
            }
            for(Map.Entry<String, FrontendMappingFieldDefinition> column : columns.entrySet().stream().filter(e -> e.getValue().getType() != FieldType.files).collect(toSet())) {
                String columnName = toColumnName(column.getKey());
                if (!tableColumns.containsKey(columnName)) {
                    validationLog.append(String.format("Column %s not present in table %s\r\n", columnName, tableName));
//                  table does not contain column, add alter query
                        updateDatabaseScript.append(String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS %s %s;\r\n",
                                tableName, columnName, column.getValue().getType().getDbType().getValue()));
                        FieldType columnFieldType = column.getValue().getType();
                        if (columnFieldType.equals(FieldType.organization_select)) {
                            updateDatabaseScript.append(
                                    String.format("alter table %s add constraint %s foreign key (%s) references %s(%s);\r\n",
                                            tableName,
                                            "fk_" + columnName,
                                            columnName,
                                            "organization",
                                            "id"
                                    ));
                        }
                    } else if (!column.getValue().getType().getDbType().getColumnType().equals(tableColumns.get(columnName))) {
                        validationSuccess = false;
                        validationLog.append(String.format("Table %s column %s type %s does not match db state (%s)\r\n",
                                tableName, columnName, column.getValue().getType().getDbType().getColumnType(), tableColumns.get(columnName)));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return validationSuccess;
    }

    /**
     * Retrieves column metadata for the specified database table via JDBC.
     * Queries {@link DatabaseMetaData#getColumns} to build a map of column names
     * to their database type names (e.g., "varchar", "bigint", "text").
     * <p>
     * Connection management:
     * <ul>
     *   <li>Obtains connection via {@link DataSourceUtils#getConnection} for proper transaction integration</li>
     *   <li>Releases connection in finally block via {@link DataSourceUtils#releaseConnection}</li>
     *   <li>Ensures connection is returned to pool even if SQLException occurs</li>
     * </ul>

     *
     * @param tableName the database table name to query for column metadata;
     *                  case-sensitivity depends on database configuration
     * @return map of column names (keys) to database type names (values);
     *         empty map if table does not exist or has no columns
     * @throws SQLException if database access error occurs during metadata retrieval;
     *                      connection is released before exception propagates
     */
    private Map<String, String> getTableColumns(String tableName) throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        DatabaseMetaData metaData = connection.getMetaData();
        Map<String, String> tableColumns = new HashMap<>();
        try(ResultSet columns = metaData.getColumns(null,null, tableName, null)){
            while(columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String typeName = columns.getString("TYPE_NAME");
                tableColumns.put(columnName, typeName);
//                String columnSize = columns.getString("COLUMN_SIZE");
//                String datatype = columns.getString("DATA_TYPE");
//                String isNullable = columns.getString("IS_NULLABLE");
//                String isAutoIncrement = columns.getString("IS_AUTOINCREMENT");
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
        return tableColumns;
    }
}
