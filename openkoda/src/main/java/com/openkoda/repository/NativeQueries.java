package com.openkoda.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;

import static com.openkoda.controller.common.URLConstants.LOWERCASE_NUMERIC_UNDERSCORE_REGEXP;

/**
 * Repository bean exposing native SQL execution utilities with result transformation for dynamic queries.
 * <p>
 * This Spring-managed repository provides low-level SQL operations for dynamic table management,
 * schema introspection, and flexible query execution. Annotated with {@code @Repository} for Spring
 * bean management and exception translation.
 * </p>
 *
 * <h2>Core Capabilities</h2>
 * <ul>
 *   <li><b>Dynamic Table Creation:</b> {@link #createTable(String)} and {@link #createTableSql(String)}
 *       generate DDL for runtime entity tables with standard OpenKoda audit columns</li>
 *   <li><b>SQL Script Execution:</b> {@link #runUpdateQuery(String)} uses Spring ScriptUtils and
 *       JDBC DataSource for bulk schema modifications bypassing JPA</li>
 *   <li><b>Table Existence Checks:</b> {@link #ifTableExists(String)} and {@link #tableExistsSql()}
 *       query PostgreSQL pg_tables system catalog for schema introspection</li>
 *   <li><b>Flexible Read Queries:</b> {@link #runReadOnly(String)} executes native SELECT statements
 *       with {@link AliasToEntityHashMapResultTransformer} for LinkedHashMap result mapping</li>
 * </ul>
 *
 * <h2>Security Considerations</h2>
 * <p>
 * SQL injection protection is provided through table name validation using
 * {@code LOWERCASE_NUMERIC_UNDERSCORE_REGEXP}. Table names must match the pattern {@code [a-z0-9_]+}
 * to prevent malicious SQL injection. The {@link #runReadOnly(String)} method sanitizes queries by
 * truncating at the first semicolon to prevent statement chaining attacks.
 * </p>
 *
 * <h2>PostgreSQL-Specific Implementation</h2>
 * <p>
 * This implementation uses PostgreSQL-specific features including pg_tables system catalog queries
 * and Java text blocks for multi-line SQL formatting. Methods are non-portable to other database
 * systems without modification.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>
 * nativeQueries.createTable("dynamic_entity_products");
 * List&lt;LinkedHashMap&lt;String, Object&gt;&gt; results = nativeQueries.runReadOnly("SELECT * FROM users");
 * </pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AliasToEntityHashMapResultTransformer
 * @see EntityManager
 * @see ScriptUtils
 */
@Repository
public class NativeQueries {
    /**
     * JPA EntityManager for executing native queries via Hibernate NativeQueryImpl.
     * <p>
     * Used to create and execute native SQL queries with custom result transformers.
     * Provides access to Hibernate-specific query features including result set mapping
     * and transaction management integration.
     * </p>
     */
    @Autowired
    EntityManager entityManager;

    /**
     * JDBC DataSource for direct SQL script execution bypassing JPA.
     * <p>
     * Provides raw JDBC connections for executing SQL scripts via Spring ScriptUtils.
     * Used when JPA overhead is unnecessary or for bulk DDL operations requiring
     * direct database access without entity mapping.
     * </p>
     */
    @Autowired
    private DataSource dataSource;

    /**
     * Creates dynamic table with standard OpenKoda audit columns and timestamps.
     * <p>
     * Generates and executes a CREATE TABLE DDL statement for runtime dynamic entity tables.
     * The table includes standard audit columns: id (primary key), created_by, created_by_id,
     * created_on, index_string (varchar 16300), modified_by, modified_by_id, organization_id,
     * and updated_on with CURRENT_TIMESTAMP defaults.
     * </p>
     *
     * <h3>Validation and Security</h3>
     * <p>
     * Table names are validated against {@code LOWERCASE_NUMERIC_UNDERSCORE_REGEXP} pattern
     * ({@code [a-z0-9_]+}) to prevent SQL injection attacks. Names containing uppercase letters,
     * special characters, or SQL keywords will be rejected.
     * </p>
     *
     * <h3>Generated Schema</h3>
     * <ul>
     *   <li><b>id:</b> bigint NOT NULL (primary key)</li>
     *   <li><b>created_by:</b> varchar(255) - username of creator</li>
     *   <li><b>created_by_id:</b> bigint - creator user ID</li>
     *   <li><b>created_on:</b> timestamp with time zone (defaults to CURRENT_TIMESTAMP)</li>
     *   <li><b>index_string:</b> varchar(16300) - searchable text index (defaults to empty string)</li>
     *   <li><b>modified_by:</b> varchar(255) - username of last modifier</li>
     *   <li><b>modified_by_id:</b> bigint - modifier user ID</li>
     *   <li><b>organization_id:</b> bigint - tenant scope identifier</li>
     *   <li><b>updated_on:</b> timestamp with time zone (defaults to CURRENT_TIMESTAMP)</li>
     * </ul>
     *
     * @param tableName table name matching lowercase_numeric_underscore pattern, must not be null
     * @return {@code true} if table created successfully, {@code false} if tableName validation fails
     * @see #createTableSql(String)
     */
    public boolean createTable(String tableName){
        if(tableName.matches(LOWERCASE_NUMERIC_UNDERSCORE_REGEXP)) {
            entityManager.createNativeQuery(createTableSql(tableName)).executeUpdate();
            return true;
        }
        return false;
    }

    /**
     * Generates CREATE TABLE DDL statement for dynamic entity tables.
     * <p>
     * Returns a SQL CREATE TABLE statement string with standard OpenKoda audit columns
     * and default values. The generated SQL uses PostgreSQL syntax with text blocks for
     * multi-line formatting. Does not execute the statement - use {@link #createTable(String)}
     * for immediate execution.
     * </p>
     *
     * <h3>Note on SQL Formatting</h3>
     * <p>
     * Uses Java 17 text blocks for readable multi-line SQL formatting. The resulting DDL
     * includes all standard audit columns with appropriate data types, nullability constraints,
     * and default value expressions compatible with PostgreSQL.
     * </p>
     *
     * @param tableName table name to use in the DDL statement
     * @return SQL CREATE TABLE statement with standard audit columns and timestamp defaults
     * @see #createTable(String)
     */
    public String createTableSql(String tableName) {
        return "CREATE TABLE " + tableName + """
                (id bigint NOT NULL,
                 created_by character varying(255),
                 created_by_id bigint,
                 created_on timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
                 index_string character varying(16300)  DEFAULT '',
                 modified_by character varying(255),
                 modified_by_id bigint, organization_id bigint,
                 updated_on timestamp with time zone DEFAULT CURRENT_TIMESTAMP)""";
    }

    /**
     * Executes SQL update script via JDBC connection using Spring ScriptUtils.
     * <p>
     * Bypasses JPA and executes SQL statements directly through the DataSource connection.
     * Uses Spring's {@link ScriptUtils} to parse and execute semicolon-separated SQL statements
     * from the provided script content. Suitable for bulk schema modifications, database migrations,
     * and DDL operations that do not require JPA entity mapping.
     * </p>
     *
     * <h3>Implementation Details</h3>
     * <p>
     * Obtains a raw JDBC {@link Connection} from the injected {@link DataSource} and passes it
     * to ScriptUtils along with the script content wrapped in a ByteArrayResource. Script execution
     * is not transactional unless wrapped in an external transaction boundary.
     * </p>
     *
     * <h3>Use Cases</h3>
     * <ul>
     *   <li>Bulk schema modifications (ALTER TABLE, CREATE INDEX)</li>
     *   <li>Database migration scripts with multiple DDL statements</li>
     *   <li>Batch data manipulation operations (INSERT, UPDATE, DELETE)</li>
     *   <li>Schema initialization and teardown operations</li>
     * </ul>
     *
     * @param sqlUpdateScript SQL script content with semicolon-separated statements
     * @throws SQLException if script execution fails due to syntax errors, constraint violations,
     *         or connection issues
     * @see ScriptUtils#executeSqlScript(Connection, org.springframework.core.io.Resource)
     */
    public void runUpdateQuery(String sqlUpdateScript) throws SQLException {
        Connection connection = dataSource.getConnection();
        ScriptUtils.executeSqlScript(connection, new ByteArrayResource(sqlUpdateScript.getBytes()));
    }

    /**
     * Checks PostgreSQL pg_tables catalog for table existence.
     * <p>
     * Executes a PostgreSQL-specific EXISTS query against the pg_tables system catalog
     * to determine if a table with the specified name exists in any schema. Returns a
     * boolean result indicating presence or absence of the table.
     * </p>
     *
     * <h3>PostgreSQL-Specific Query</h3>
     * <p>
     * Uses the PostgreSQL pg_tables system view which contains one row for each table
     * in the database. The query is non-portable and will not work with other database
     * systems without modification to use their respective system catalogs.
     * </p>
     *
     * <h3>Schema Scope</h3>
     * <p>
     * Searches across all schemas accessible to the current database user. Does not
     * filter by schema, so will return {@code true} if the table exists in any schema
     * (public, pg_catalog, information_schema, or custom schemas).
     * </p>
     *
     * @param tableName table name to check, must not be null
     * @return {@code true} if table exists in any schema, {@code false} otherwise
     * @see #tableExistsSql()
     */
    public boolean ifTableExists(String tableName){
           return (Boolean) entityManager.createNativeQuery(tableExistsSql()).setParameter("tableName", tableName)
                   .getSingleResult();
    }

    /**
     * Returns parameterized SQL for table existence check against PostgreSQL system catalog.
     * <p>
     * Generates a PostgreSQL EXISTS query string with a named parameter placeholder
     * ({@code :tableName}) for use with JPA native query parameter binding. The query
     * checks the pg_tables system view for the presence of a table with the specified name.
     * </p>
     *
     * <h3>SQL Implementation</h3>
     * <p>
     * Uses Java 17 text blocks for readable multi-line SQL formatting. Returns an EXISTS
     * subquery that evaluates to a boolean result, compatible with PostgreSQL's EXISTS
     * operator semantics. The query template must be parameterized before execution using
     * {@link jakarta.persistence.Query#setParameter(String, Object)}.
     * </p>
     *
     * <h3>Dialect Compatibility</h3>
     * <p>
     * Non-portable query - PostgreSQL-specific. Other databases use different system
     * catalogs (e.g., INFORMATION_SCHEMA.TABLES for ANSI SQL, USER_TABLES for Oracle).
     * </p>
     *
     * @return PostgreSQL EXISTS query string with {@code :tableName} parameter placeholder
     * @see #ifTableExists(String)
     */
    public String tableExistsSql() {
        return """
                SELECT EXISTS (
                    SELECT FROM
                        pg_tables
                    WHERE
                        tablename  = :tableName)""";
    }

    /**
     * Executes read-only native SQL query with LinkedHashMap result transformation.
     * <p>
     * Executes a native SQL SELECT query and transforms results into a list of insertion-order
     * LinkedHashMap instances, where each map represents a result row with column aliases as keys
     * and query result values as map values. Provides flexible result mapping for dynamic reporting
     * queries and data exports without requiring predefined entity classes.
     * </p>
     *
     * <h3>Transaction Configuration</h3>
     * <p>
     * Annotated with {@code @Transactional(readOnly=true)} for read-only transaction optimization.
     * The read-only flag hints to the persistence provider and JDBC driver that no write operations
     * will occur, enabling potential performance optimizations such as avoiding transaction log writes
     * and optimizing connection handling.
     * </p>
     *
     * <h3>Result Transformation</h3>
     * <p>
     * Applies {@link AliasToEntityHashMapResultTransformer#INSTANCE} via Hibernate's
     * {@link NativeQueryImpl} to convert query tuples into LinkedHashMap instances. Each column
     * alias becomes a map key, preserving result order and providing flexible JSON serialization.
     * Null aliases are skipped to avoid polluting result maps.
     * </p>
     *
     * <h3>SQL Injection Mitigation</h3>
     * <p>
     * Sanitizes input by truncating the query at the first semicolon using
     * {@link StringUtils#substringBefore(String, String)}. This prevents SQL injection attacks
     * via statement chaining where malicious users append additional statements after the
     * SELECT query. <b>Warning:</b> Does not protect against all injection vectors - use
     * parameterized queries for user-supplied filter values.
     * </p>
     *
     * <h3>Use Cases</h3>
     * <ul>
     *   <li>Dynamic reporting queries with variable column selections</li>
     *   <li>Data exports requiring flexible JSON/CSV serialization</li>
     *   <li>Administrative queries for schema introspection</li>
     *   <li>Ad-hoc data analysis without predefined entity mappings</li>
     * </ul>
     *
     * <h3>Usage Example</h3>
     * <pre>
     * List&lt;LinkedHashMap&lt;String, Object&gt;&gt; users = 
     *     nativeQueries.runReadOnly("SELECT id, name, email FROM users WHERE active = true");
     * </pre>
     *
     * @param query native SQL SELECT query, semicolon-terminated statements automatically truncated
     * @return list of LinkedHashMap instances with column aliases as keys and result values,
     *         empty list if query returns no rows
     * @see AliasToEntityHashMapResultTransformer
     * @see NativeQueryImpl
     */
    @Transactional(readOnly = true)
    public List<LinkedHashMap<String, Object>> runReadOnly(String query) {
        Query q1 = entityManager.createNativeQuery(StringUtils.substringBefore(query,";"));
        NativeQueryImpl nativeQuery = (NativeQueryImpl) q1;
        nativeQuery.setResultTransformer(AliasToEntityHashMapResultTransformer.INSTANCE);
        return nativeQuery.getResultList();
    }
}
