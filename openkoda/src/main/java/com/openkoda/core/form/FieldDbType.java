package com.openkoda.core.form;

/**
 * Enumeration of PostgreSQL database column types for dynamic entity generation.
 * <p>
 * This enum defines the mapping between form field types and PostgreSQL database column types.
 * Each constant specifies both the SQL DDL type string used in CREATE TABLE statements
 * and the PostgreSQL catalog type name from pg_catalog. These mappings are used by
 * DynamicEntityService to generate database tables at runtime for dynamically created entities.
 * </p>
 * <p>
 * The enum provides two representations for each type:
 * <ul>
 * <li><strong>value</strong>: The SQL DDL type string used in CREATE TABLE statements (e.g., "varchar(255)", "bigint")</li>
 * <li><strong>columnType</strong>: The PostgreSQL catalog type name from pg_catalog (e.g., "varchar", "int8")</li>
 * </ul>
 * When columnType is null, the value is used for both representations.
 * </p>
 * <p>
 * Example DDL generation:
 * <pre>{@code
 * // Using VARCHAR_255 enum constant
 * FieldDbType type = FieldDbType.VARCHAR_255;
 * String ddl = "CREATE TABLE entity (field " + type.getValue() + ")";
 * // Result: "CREATE TABLE entity (field varchar(255))"
 * }</pre>
 * </p>
 *
 * @see FieldType
 * @see com.openkoda.service.dynamicentity.DynamicEntityService
 * @since 1.7.1
 * @author OpenKoda Team
 */
public enum FieldDbType {

    /**
     * Variable character string with maximum 255 characters.
     * <p>
     * SQL DDL: {@code varchar(255)}<br>
     * PostgreSQL catalog type: {@code varchar}
     * </p>
     * Used for short text fields like names, titles, and identifiers.
     */
    VARCHAR_255("varchar(255)", "varchar"),
    
    /**
     * Variable character string with maximum 1000 characters.
     * <p>
     * SQL DDL: {@code varchar(1000)}<br>
     * PostgreSQL catalog type: {@code varchar}
     * </p>
     * Used for medium text fields like descriptions and comments.
     */
    VARCHAR_1000("varchar(1000)", "varchar"),
    
    /**
     * Variable character string with maximum 262144 characters (256KB).
     * <p>
     * SQL DDL: {@code varchar(262144)}<br>
     * PostgreSQL catalog type: {@code varchar}
     * </p>
     * Used for large text fields like articles, code snippets, and document content.
     */
    VARCHAR_262144("varchar(262144)", "varchar"),
    
    /**
     * 64-bit signed integer.
     * <p>
     * SQL DDL: {@code bigint}<br>
     * PostgreSQL catalog type: {@code int8}
     * </p>
     * Used for numeric identifiers, foreign keys, and large integer values.
     * Range: -9,223,372,036,854,775,808 to 9,223,372,036,854,775,807.
     */
    BIGINT("bigint", "int8"),
    
    /**
     * Arbitrary precision numeric value.
     * <p>
     * SQL DDL: {@code numeric}<br>
     * PostgreSQL catalog type: {@code numeric}
     * </p>
     * Used for decimal values like prices, percentages, and measurements
     * where exact precision is required.
     */
    NUMERIC("numeric", null),
    
    /**
     * Boolean value (true or false).
     * <p>
     * SQL DDL: {@code boolean}<br>
     * PostgreSQL catalog type: {@code bool}
     * </p>
     * Used for flags, switches, and binary state fields.
     */
    BOOLEAN("boolean", "bool"),
    
    /**
     * Calendar date without time component.
     * <p>
     * SQL DDL: {@code date}<br>
     * PostgreSQL catalog type: {@code date}
     * </p>
     * Used for birth dates, due dates, and other date-only values.
     */
    DATE("date", null),
    
    /**
     * Timestamp with time zone information.
     * <p>
     * SQL DDL: {@code timestamp with time zone}<br>
     * PostgreSQL catalog type: {@code timestamptz}
     * </p>
     * Used for event timestamps, created/modified dates, and scheduled times
     * where time zone awareness is required.
     */
    TIMESTAMP_W_TZ("timestamp with time zone", "timestamptz"),
    
    /**
     * Time of day with time zone information.
     * <p>
     * SQL DDL: {@code time with time zone}<br>
     * PostgreSQL catalog type: {@code timetz}
     * </p>
     * Used for recurring daily events and time-of-day schedules
     * where time zone awareness is required.
     */
    TIME_W_TZ("time with time zone", "timetz"),
    ;

    /**
     * The SQL DDL type string used in CREATE TABLE statements.
     * <p>
     * Examples: "varchar(255)", "bigint", "timestamp with time zone"
     * </p>
     */
    private String value;
    
    /**
     * The PostgreSQL catalog type name from pg_catalog.
     * <p>
     * May be null, in which case {@link #getValue()} is used for catalog lookups.
     * Examples: "varchar", "int8", "timestamptz"
     * </p>
     */
    private String columnType;

    /**
     * Constructs a FieldDbType enum constant with DDL and catalog type.
     *
     * @param value the SQL DDL type string used in CREATE TABLE statements
     *              (e.g., "varchar(255)", "bigint", "timestamp with time zone")
     * @param columnType the PostgreSQL catalog type name from pg_catalog
     *                   (e.g., "varchar", "int8", "timestamptz"), or null to use value
     */
    FieldDbType(String value, String columnType) {
        this.value = value;
        this.columnType = columnType;
    }

    /**
     * Returns the PostgreSQL catalog type name for this field type.
     * <p>
     * This method returns the pg_catalog type name used for type lookups in PostgreSQL's
     * system catalogs. If columnType was specified in the constructor, it is returned;
     * otherwise, the DDL value is returned as a fallback.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * FieldDbType type = FieldDbType.BIGINT;
     * String catalogType = type.getColumnType(); // Returns "int8"
     * }</pre>
     * </p>
     *
     * @return the PostgreSQL catalog type name, or the DDL value if columnType is null
     */
    public String getColumnType() {
        return columnType != null ? columnType : value;
    }

    /**
     * Returns the SQL DDL type string for CREATE TABLE statements.
     * <p>
     * This method returns the type specification used when generating CREATE TABLE
     * DDL statements for dynamic entities. The value includes size constraints
     * where applicable (e.g., "varchar(255)") and PostgreSQL-specific type names
     * (e.g., "timestamp with time zone").
     * </p>
     * <p>
     * Example usage in dynamic entity table creation:
     * <pre>{@code
     * FieldDbType type = FieldDbType.VARCHAR_255;
     * String ddl = "CREATE TABLE dynamic_entity (" +
     *              "id bigint PRIMARY KEY, " +
     *              "field_name " + type.getValue() + ")";
     * // Result: CREATE TABLE dynamic_entity (id bigint PRIMARY KEY, field_name varchar(255))
     * }</pre>
     * </p>
     *
     * @return the SQL DDL type string used in CREATE TABLE statements
     * @see com.openkoda.service.dynamicentity.DynamicEntityService
     */
    public String getValue() {
        return value;
    }
}
