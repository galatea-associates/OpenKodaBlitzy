package com.openkoda.model.report;

import com.openkoda.model.PrivilegeNames;
import com.openkoda.model.common.OpenkodaEntity;
import jakarta.persistence.Entity;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Formula;

/**
 * JPA entity representing query-based report definitions for ad-hoc SQL reporting.
 * <p>
 * QueryReport stores report metadata including a human-readable name and the SQL query string
 * to execute for report generation. This entity supports parameterized queries with runtime
 * parameter binding, enabling flexible reporting capabilities across the OpenKoda platform.
 * Reports are organization-scoped for multi-tenant isolation and include computed privilege
 * fields for fine-grained authorization control.
 * </p>
 * <p>
 * <b>Persistence Details:</b><br>
 * Persisted to 'query_reports' table (inferred from JPA naming convention). Extends
 * {@link OpenkodaEntity} to inherit auditing fields (createdOn, updatedOn) and organization
 * scoping (organizationId) for tenant isolation.
 * </p>
 * <p>
 * <b>Report Workflow:</b><br>
 * 1. Create QueryReport instance with organization context and SQL query<br>
 * 2. Reporting service executes query with runtime parameter binding<br>
 * 3. Export handlers generate output files using {@link #getFileName()} for naming<br>
 * 4. UI components display reports using {@link #getName()} for labels
 * </p>
 * <p>
 * <b>Multi-Tenancy:</b><br>
 * Organization-scoped via inherited organizationId field ensures tenant isolation. Each report
 * definition is visible only within its owning organization context, preventing cross-tenant
 * data access in multi-tenant deployments.
 * </p>
 * <p>
 * <b>Security:</b><br>
 * Computed privilege fields ({@link #requiredReadPrivilege}, {@link #requiredWritePrivilege})
 * use Hibernate @Formula annotations to return privilege constants for authorization checks.
 * Read operations require {@link PrivilegeNames#_readOrgData}, write operations require
 * {@link PrivilegeNames#_manageOrgData}. Service layer must sanitize SQL queries to prevent
 * SQL injection attacks.
 * </p>
 * <p>
 * <b>Integration:</b><br>
 * - Reporting services: Query execution and parameter binding<br>
 * - Export handlers: File generation with CSV, Excel, PDF formats<br>
 * - UI components: Report management interfaces and display widgets<br>
 * - AI integration: Supports AI-assisted report generation via canUseReportingAI privilege
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * QueryReport report = new QueryReport(organizationId);
 * report.setName("Monthly Sales Report");
 * report.setQuery("SELECT * FROM sales WHERE month = :month");
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see OpenkodaEntity
 * @see PrivilegeNames
 */
@Entity
public class QueryReport extends OpenkodaEntity {

    /**
     * Human-readable report name for display and export filename generation.
     * <p>
     * This field stores the report's display label shown in UI components and used
     * by {@link #getFileName()} to generate filesystem-friendly export filenames.
     * Persisted as VARCHAR column in database.
     * </p>
     */
    private String name;
    
    /**
     * SQL query string to execute for report generation.
     * <p>
     * Stores the raw SQL query without validation or sanitization. The service layer
     * must validate and sanitize this query before execution to prevent SQL injection
     * attacks. Supports parameterized queries with runtime parameter binding using
     * named parameters (e.g., :paramName syntax).
     * </p>
     */
    private String query;

    /**
     * Computed privilege field for read authorization checks.
     * <p>
     * Returns {@link PrivilegeNames#_readOrgData} constant via Hibernate @Formula annotation.
     * This field is not persisted to the database but computed at query time using the
     * SQL expression: {@code ( 'ORGANIZATION_DATA_READ' )}. Used by authorization services
     * to enforce read access control for report viewing and execution.
     * </p>
     *
     * @see PrivilegeNames#_readOrgData
     */
    @Formula("( '" + PrivilegeNames._readOrgData + "' )")
    private String requiredReadPrivilege;

    /**
     * Computed privilege field for write authorization checks.
     * <p>
     * Returns {@link PrivilegeNames#_manageOrgData} constant via Hibernate @Formula annotation.
     * This field is not persisted to the database but computed at query time using the
     * SQL expression: {@code ( 'ORGANIZATION_DATA_MANAGE' )}. Used by authorization services
     * to enforce write access control for report creation, modification, and deletion.
     * </p>
     *
     * @see PrivilegeNames#_manageOrgData
     */
    @Formula("( '" + PrivilegeNames._manageOrgData + "' )")
    private String requiredWritePrivilege;

    /**
     * Constructs an organization-scoped QueryReport for multi-tenant report creation.
     * <p>
     * Creates a new report instance bound to the specified organization context, ensuring
     * tenant isolation in multi-tenant deployments. The organizationId is passed to the
     * parent {@link OpenkodaEntity} constructor to establish tenant scope.
     * </p>
     *
     * @param organizationId the organization identifier for tenant scoping, or null for
     *                       global reports (not recommended for multi-tenant environments)
     */
    public QueryReport(Long organizationId) {
        super(organizationId);
    }

    /**
     * Constructs a QueryReport with no organization context (JPA no-arg constructor).
     * <p>
     * Required by JPA specification for entity instantiation during query result
     * materialization. Passes null organization to parent constructor. For application
     * code, prefer {@link #QueryReport(Long)} with explicit organization context to
     * ensure proper multi-tenant isolation.
     * </p>
     */
    public QueryReport() {
        super(null);
    }

    /**
     * Constructs a QueryReport with initialized query field for quick report creation.
     * <p>
     * Convenience constructor that creates a report instance with the SQL query already
     * set. The organization context is null, so caller should set organizationId separately
     * for multi-tenant deployments. Name field remains null and should be set via
     * {@link #setName(String)} for proper display and export filename generation.
     * </p>
     *
     * @param query the SQL query string to execute for report generation (not validated
     *              or sanitized; service layer must validate before execution)
     */
    public QueryReport(String query) {
        super(null);
        this.query = query;
    }

    /**
     * Returns the report name for display and filename generation.
     * <p>
     * Retrieves the human-readable report name used in UI components for display labels
     * and by {@link #getFileName()} for generating export filenames. May be null if not
     * yet set via {@link #setName(String)} or during entity initialization.
     * </p>
     *
     * @return the report name, or null if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the report name for display and filename generation.
     * <p>
     * Updates the human-readable report name. This value is used by UI forms for display
     * and by import handlers during report definition imports. The name is also used by
     * {@link #getFileName()} to generate filesystem-friendly export filenames.
     * </p>
     *
     * @param name the report name to set (may be null)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the SQL query string for execution by the reporting service.
     * <p>
     * Retrieves the stored SQL query string that will be executed to generate report data.
     * This query is stored without validation or sanitization, so the reporting service
     * must validate and sanitize before execution to prevent SQL injection attacks.
     * Supports parameterized queries with named parameters.
     * </p>
     *
     * @return the SQL query string, or null if not set
     */
    public String getQuery() {
        return query;
    }

    /**
     * Sets the SQL query string for report generation.
     * <p>
     * Updates the SQL query that will be executed by the reporting service. No validation
     * or sanitization is performed at this level. The caller must ensure the query is safe
     * and valid before persisting. The reporting service must validate and sanitize this
     * query before execution to prevent SQL injection vulnerabilities.
     * </p>
     *
     * @param query the SQL query string to set (not validated; may be null)
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Generates a filesystem-friendly export filename from the report name.
     * <p>
     * Creates a sanitized filename suitable for export file generation by replacing all
     * whitespace characters with underscores and converting to lowercase. If the name
     * field is null or empty, returns the default filename "report". This method is used
     * by export handlers to generate filenames for CSV, Excel, and PDF exports.
     * </p>
     * <p>
     * Example transformations:<br>
     * "Monthly Sales Report" → "monthly_sales_report"<br>
     * null → "report"<br>
     * "" → "report"
     * </p>
     *
     * @return filesystem-friendly filename derived from report name, or "report" if name
     *         is null or empty
     */
    public String getFileName() {
        return StringUtils.isNotEmpty(name) ? name.replaceAll("\\s+", "_").toLowerCase() : "report";
    }
}
