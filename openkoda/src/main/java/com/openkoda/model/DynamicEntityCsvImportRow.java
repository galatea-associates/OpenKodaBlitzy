package com.openkoda.model;

import com.openkoda.model.common.OpenkodaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * Per-row CSV import staging entity storing validation state and content for bulk dynamic entity data import operations.
 * <p>
 * Persisted to table {@code dynamic_entity_csv_import_row}. Represents single CSV file row during bulk import workflow.
 * Stores uploadId (batch identifier), lineNumber (row position), entityKey (unique row identifier), and content as
 * JSONB ({@code @JdbcTypeCode(SqlTypes.JSON)}, {@code columnDefinition='jsonb'}). Extends {@link OpenkodaEntity} for
 * organization scoping and audit fields. Used by CSV import pipeline to persist row-by-row validation state, error
 * tracking, and staged data before committing to target dynamic entities. Enables incremental processing, error
 * recovery, and import preview.
 * </p>
 * <p>
 * Import workflow:
 * <ol>
 *   <li>Parse CSV rows</li>
 *   <li>Create DynamicEntityCsvImportRow for each</li>
 *   <li>Validate content</li>
 *   <li>Commit valid rows to target entities</li>
 *   <li>Report errors on failed rows</li>
 * </ol>
 * </p>
 * <p>
 * JSONB storage: {@code content} column stores flexible JSON object with parsed CSV field values. PostgreSQL-specific
 * JSONB type for efficient querying and indexing of nested data structures.
 * </p>
 * <p>
 * Audit behavior: Inherits {@code toAuditString()} from {@link OpenkodaEntity}, which returns formatted string with
 * entityKey for audit identification.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see DynamicEntity
 * @see OpenkodaEntity
 */
@Entity
public class DynamicEntityCsvImportRow extends OpenkodaEntity {

    /**
     * Batch identifier grouping all rows from single CSV upload.
     * <p>
     * Used to query/delete import batch. All rows from a single CSV file share the same uploadId,
     * enabling batch operations and tracking of import progress.
     * </p>
     */
    @Column
    private Long uploadId;

    /**
     * 1-based line number from CSV file.
     * <p>
     * Enables error reporting with line references. Stored as primitive long to ensure non-null values.
     * Line 1 corresponds to the first data row (typically after header row).
     * </p>
     */
    @Column
    private long lineNumber;

    /**
     * Determines if line is valid after validation processing.
     * <p>
     * Set to {@code true} if row content passes all validation rules, {@code false} otherwise.
     * Nullable to distinguish between unprocessed (null), valid (true), and invalid (false) rows.
     * </p>
     */
    @Column
    private Boolean valid;

    /**
     * Unique identifier for this row within upload batch.
     * <p>
     * Used for deduplication and row lookup during import processing. Typically corresponds to a
     * business key or primary key value from the CSV data. Enables idempotent imports and error
     * tracking per distinct entity.
     * </p>
     */
    @Column
    private String entityKey;

    /**
     * JSON object storing parsed CSV field values.
     * <p>
     * {@code @JdbcTypeCode(SqlTypes.JSON)} with {@code columnDefinition='jsonb'} for PostgreSQL native JSON type.
     * Enables flexible schema-less storage during validation phase before committing to strongly-typed dynamic
     * entity tables. The JSONB format provides efficient querying, indexing, and storage of nested data structures.
     * </p>
     * <p>
     * Initialized to empty map ({@code Map.of()}) to prevent null pointer exceptions during validation.
     * Field names in the map correspond to CSV column headers or target entity field names.
     * </p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> content = Map.of();

    /**
     * Default constructor creating a row without organization context.
     * <p>
     * Calls {@code super(null)} to initialize OpenkodaEntity with no organization scoping.
     * Typically used by JPA entity instantiation.
     * </p>
     */
    public DynamicEntityCsvImportRow() {
        super(null);
    }

    /**
     * Constructor creating a row with organization scoping.
     * <p>
     * Enables multi-tenant data isolation by associating this import row with a specific organization.
     * Used when creating rows in tenant-aware import workflows.
     * </p>
     *
     * @param organizationId the organization identifier for tenant scoping, or null for non-scoped rows
     */
    public DynamicEntityCsvImportRow(Long organizationId) {
        super(organizationId);
    }

    /**
     * Gets the batch identifier for this import row.
     *
     * @return the upload batch ID, or null if not yet assigned
     */
    public Long getUploadId() {
        return uploadId;
    }

    /**
     * Sets the batch identifier for this import row.
     * <p>
     * All rows from the same CSV upload should share the same uploadId for batch tracking.
     * </p>
     *
     * @param uploadId the upload batch ID to assign
     */
    public void setUploadId(Long uploadId) {
        this.uploadId = uploadId;
    }

    /**
     * Gets the 1-based line number from the CSV file.
     *
     * @return the line number in the CSV file (1-based)
     */
    public long getLineNumber() {
        return lineNumber;
    }

    /**
     * Sets the 1-based line number from the CSV file.
     * <p>
     * Used for error reporting to identify which line in the CSV file contains issues.
     * </p>
     *
     * @param lineNumber the line number in the CSV file (1-based)
     */
    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Gets the validation state of this row.
     *
     * @return {@code true} if row is valid, {@code false} if invalid, or {@code null} if not yet validated
     */
    public Boolean getValid() {
        return valid;
    }

    /**
     * Sets the validation state of this row.
     * <p>
     * Should be set after validation processing completes. Use {@code true} for valid rows,
     * {@code false} for invalid rows, and {@code null} for unprocessed rows.
     * </p>
     *
     * @param valid the validation state to set
     */
    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    /**
     * Gets the unique identifier for this row within the upload batch.
     *
     * @return the entity key, or null if not set
     */
    public String getEntityKey() {
        return entityKey;
    }

    /**
     * Sets the unique identifier for this row within the upload batch.
     * <p>
     * Used for deduplication and row lookup. Should correspond to a business key or primary key
     * from the CSV data to enable idempotent imports.
     * </p>
     *
     * @param entityKey the entity key to set
     */
    public void setEntityKey(String entityKey) {
        this.entityKey = entityKey;
    }

    /**
     * Gets the JSON content storing parsed CSV field values.
     *
     * @return map of field names to values, never null (defaults to empty map)
     */
    public Map<String, Object> getContent() {
        return content;
    }

    /**
     * Sets the JSON content storing parsed CSV field values.
     * <p>
     * Field names in the map should correspond to CSV column headers or target entity field names.
     * Values can be of any type supported by JSONB serialization.
     * </p>
     *
     * @param content map of field names to values to store
     */
    public void setContent(Map<String, Object> content) {
        this.content = content;
    }
}
