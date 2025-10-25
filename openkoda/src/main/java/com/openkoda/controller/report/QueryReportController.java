package com.openkoda.controller.report;

import com.openkoda.controller.common.PageAttributes;
import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.core.form.CRUDControllerConfiguration;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.common.SearchableOrganizationRelatedEntity;
import com.openkoda.model.file.File;
import com.openkoda.model.report.QueryReport;
import com.openkoda.repository.NativeQueries;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.hibernate.exception.GenericJDBCException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.openkoda.controller.common.URLConstants.*;

/**
 * REST controller for managing saved SQL query-based reports with scheduling and export capabilities.
 * <p>
 * Provides comprehensive report library functionality including saved SQL queries with named parameters,
 * ad-hoc query execution, scheduled report runs, subscription management, and multi-format exports
 * (HTML table, CSV, Excel, PDF). Reports are persisted as QueryReport entities containing SQL,
 * parameters, and schedule configuration. Supports parameterized reports with named parameter
 * substitution (:paramName syntax), output format selection, and email delivery for scheduled reports.
 * Routes accessible under /reports/query with both global and organization-scoped endpoints.
 * </p>
 * <p>
 * Extends {@link AbstractController} for Flow-based pipeline utilities and logging helpers
 * (debug(), error()). Implements {@link HasSecurityRules} for privilege checking helpers
 * (hasGlobalOrOrgPrivilege()).
 * </p>
 * <p>
 * Methods protected with {@code @PreAuthorize} annotations requiring CHECK_CAN_CREATE_REPORTS
 * or CHECK_CAN_READ_REPORTS privileges. Organization-scoped authorization enforced via
 * hasGlobalOrOrgPrivilege() checks.
 * </p>
 * <p>
 * Injects {@link NativeQueries} for read-only SQL execution, uses Services registry for
 * validation/CSV/file operations, accesses Repositories for QueryReport persistence.
 * </p>
 * <p>
 * Operational prerequisites: Requires accessible database connection, functioning services.csv
 * and services.file subsystems, properly configured Spring Security privilege evaluation.
 * </p>
 * <p>
 * Controller is stateless with no instance fields (except injected NativeQueries). Thread-safe
 * for concurrent requests. Discovered via component scanning.
 * </p>
 * <p>
 * Comprehensive exception handling for SQL errors (InvalidDataAccessResourceUsageException,
 * JpaSystemException, GenericJDBCException) with nested cause message extraction. Missing reports
 * return 404. Authorization failures return 401. Validation errors return field-specific messages.
 * </p>
 * <p>
 * Removing this controller removes CRUD operations for saved reports, ad-hoc query execution,
 * CSV export functionality, and report re-run capabilities. Reports remain in database but
 * become inaccessible via REST API.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see QueryReport
 * @see NativeQueries
 * @see CRUDControllerConfiguration
 * @see AbstractController
 * @see HasSecurityRules
 */
@RestController
@RequestMapping({_HTML_ORGANIZATION_ORGANIZATIONID + _QUERY_REPORT, _HTML + _QUERY_REPORT})
public class QueryReportController extends AbstractController implements HasSecurityRules {

    /**
     * Injected bean for executing read-only native SQL queries against the database.
     * <p>
     * Provides runReadOnly() method with SQL injection prevention and query timeout enforcement.
     * Used by runQuery() and runQueryToCsv() methods to execute user-provided and saved report
     * SQL queries securely.
     * </p>
     */
    @Inject
    NativeQueries nativeQueries;

    /**
     * Persists new or updates existing QueryReport entity with SQL query, parameters, and schedule configuration.
     * <p>
     * Validates and saves QueryReport settings using CRUD configuration pattern. For existing reports
     * (existingReportId provided), loads entity from secure repository and updates fields. For new reports,
     * creates entity via conf.createNewEntity(organizationId). Performs form validation via
     * services.validation.validateAndPopulateToEntity() and persists via
     * conf.getSecureRepository().saveOne(). Returns report ID and updated form on success, or field
     * error details on validation failure. Uses Flow pipeline for transactional entity lifecycle management.
     * </p>
     *
     * @param existingReportId Optional ID of existing QueryReport to update. If null, creates new report.
     *                        Used to distinguish between create and update operations in POST /new/settings
     *                        vs POST /reportId/settings
     * @param organizationId Optional organization ID for tenant-scoped report. If null, creates global report.
     *                      Used for multi-tenancy and authorization checks via hasGlobalOrOrgPrivilege()
     * @param form Form containing report settings: name, description, SQL query, named parameters,
     *            schedule (cron expression), subscribers. Must pass @Valid Bean Validation constraints.
     *            Populated to QueryReport entity via validateAndPopulateToEntity()
     * @param br BindingResult capturing validation errors from @Valid annotation. Used to return
     *          field-specific error messages to client on validation failure
     * @return On success: ResponseEntity with saved report ID and updated form. On validation failure:
     *        field error details from BindingResult. On authorization failure: HTTP 401 Unauthorized.
     *        Return type is Object to support multiple response types via Flow.mav()
     */
    @PostMapping({_NEW_SETTINGS, _ID_SETTINGS})
    @PreAuthorize(CHECK_CAN_CREATE_REPORTS)
    @ResponseBody
    public Object saveReport(@PathVariable(name=ID, required = false) Long existingReportId,
                             @PathVariable(name=ORGANIZATIONID, required = false) Long organizationId,
                             @Valid AbstractOrganizationRelatedEntityForm form, BindingResult br) {
        debug("[saveReport]");

        CRUDControllerConfiguration conf = controllers.htmlCrudControllerConfigurationMap.get(QUERY_REPORT);
        if (!hasGlobalOrOrgPrivilege(conf.getPostNewPrivilege(), organizationId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return Flow.init()
                .then(a -> (SearchableOrganizationRelatedEntity)conf.getSecureRepository().findOne(existingReportId))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br, a.result != null ? a.result : conf.createNewEntity(organizationId)))
                .thenSet(organizationRelatedEntity, a -> (SearchableOrganizationRelatedEntity)conf.getSecureRepository().saveOne(a.result))
                .thenSet(reportId, a -> a.model.get(organizationRelatedEntity).getId())
                .thenSet(conf.getFormAttribute(), a -> conf.createNewForm(organizationId, a.model.get(organizationRelatedEntity)))
                .execute()
                .mav(a -> a.get(reportId), a -> br.getFieldError().getField());
    }

    /**
     * Executes arbitrary read-only SQL query for privileged users and returns formatted results.
     * <p>
     * Executes user-provided SQL query via nativeQueries.runReadOnly() with comprehensive error handling.
     * Validates user has CHECK_CAN_CREATE_REPORTS privilege. Catches database exceptions
     * (InvalidDataAccessResourceUsageException, JpaSystemException, GenericJDBCException) and surfaces
     * nested error messages rather than failing silently. Returns results as LinkedHashMap list for
     * flexible rendering via configurable resultView template (default 'report-data-table').
     * Optionally associates execution with saved QueryReport entity via reportId parameter. Prepares
     * form for saving query if user has save privilege (CHECK_CAN_CREATE_REPORTS). Uses Flow pipeline
     * to populate model with query, results, reportId, form, and error log.
     * </p>
     * <p>
     * Flow execution: 1) Execute query via nativeQueries.runReadOnly() with exception handling,
     * 2) Check user save privilege, 3) Load QueryReport entity if reportId provided, 4) Create form
     * if save permitted, 5) Populate model with results/errors/form, 6) Render via resultView template.
     * </p>
     * <p>
     * Enforces CHECK_CAN_CREATE_REPORTS privilege via @PreAuthorize. Uses nativeQueries.runReadOnly()
     * which restricts to SELECT statements and applies query timeout (default 30 seconds). SQL injection
     * prevention via JDBC PreparedStatement under the hood.
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * // Ad-hoc query execution
     * runQuery(null, "SELECT * FROM users LIMIT 10", null, "report-data-table");
     *
     * // Re-run saved report
     * runQuery(orgId, "SELECT revenue FROM sales", reportId, "report-chart-view");
     * </pre>
     * </p>
     *
     * @param organizationId Optional organization ID for tenant-scoped query execution. Null indicates
     *                      global context. Used for authorization checks and report saving
     * @param query SQL query string to execute. Must be SELECT statement (enforced by
     *             nativeQueries.runReadOnly()). Supports standard SQL syntax.
     *             Example: 'SELECT name, email FROM users WHERE created &gt; CURRENT_DATE - INTERVAL 30 DAY'
     * @param reportId Optional ID of saved QueryReport to associate with execution. Used to load report
     *                metadata and enable re-run functionality. If null, treats as ad-hoc query execution
     * @param resultView Thymeleaf template name for rendering results (default 'report-data-table').
     *                  Allows customization of result presentation (table, chart, pivot). Template
     *                  receives genericReportViewLinkedHashMap model attribute
     * @return ModelAndView with query results, error log (if any), report form (if save privilege exists),
     *        and reportId. View determined by resultView parameter. Model keys: genericReportViewLinkedHashMap
     *        (result rows), error (exception message), PageAttributes.query (executed SQL),
     *        PageAttributes.reportId, form attribute (for saving)
     */
    @PostMapping(_QUERY)
    @PreAuthorize(CHECK_CAN_CREATE_REPORTS)
    public Object runQuery(@PathVariable(name=ORGANIZATIONID, required = false) Long organizationId,
                           @RequestParam("query") String query,
                           @RequestParam(value = "reportId", required = false) Long reportId,
                           @RequestParam(value = "resultView", defaultValue = "report-data-table") String resultView) {
        debug("[runQuery]");

        List<LinkedHashMap<String, Object>> queryResult = new ArrayList<>();
        String errorLog = null;
        try {
            queryResult = nativeQueries.runReadOnly(query);
        } catch (InvalidDataAccessResourceUsageException | JpaSystemException | GenericJDBCException e) {
            error("[runQuery]", e);
            errorLog = String.format("%s\n%s", e.getCause().getMessage(), e.getCause().getCause().getMessage());
        }

        CRUDControllerConfiguration conf = controllers.htmlCrudControllerConfigurationMap.get(QUERY_REPORT);
        boolean canSaveReport = hasGlobalOrOrgPrivilege(conf.getGetSettingsPrivilege(), organizationId);

        List<LinkedHashMap<String, Object>> finalQueryResult = queryResult;
        String finalErrorLog = errorLog;
        return Flow.init(PageAttributes.query, query)
                .thenSet(PageAttributes.reportId, a -> reportId)
                .thenSet(genericReportViewLinkedHashMap, a -> finalQueryResult)
                .thenSet(error, a -> finalErrorLog)
                .then(a -> (QueryReport) conf.getSecureRepository().findOne(reportId))
                .then(a -> a.result != null ? a.result : new QueryReport(query))
                .thenSet(conf.getFormAttribute(), a -> canSaveReport ? conf.createNewForm(organizationId, a.result) : null)
                .execute()
                .mav(resultView);

    }

    /**
     * Executes SQL query and exports results as CSV file download.
     * <p>
     * Executes read-only SQL query via nativeQueries.runReadOnly() and generates CSV export using
     * services.csv.createCSV(). Retrieves saved QueryReport by reportId to use report filename,
     * otherwise generates timestamped filename (report_yyyy-MM-dd-HH-mm.csv). Transforms LinkedHashMap
     * query results into table format (header row + data rows) for CSV serialization. Uses
     * services.file.getFileContentAndPrepareResponse() to stream file content with appropriate
     * Content-Disposition header for browser download. Handles database and I/O exceptions gracefully
     * with error logging. Uses Flow pipeline to orchestrate: load report → build filename → transform
     * results → create CSV file → return File entity.
     * </p>
     * <p>
     * Timestamp format: yyyy-MM-dd-HH-mm (DateTimeFormatter). CSV generation: 1) Extract headers from
     * first result row keys, 2) Extract data rows from result values, 3) Call services.csv.createCSV()
     * to create File entity with CSV content, 4) Stream file via services.file.getFileContentAndPrepareResponse().
     * Exception handling: catches InvalidDataAccessResourceUsageException, JpaSystemException,
     * GenericJDBCException during query execution and logs errors but continues to return empty CSV
     * rather than failing.
     * </p>
     * <p>
     * Protected by @PreAuthorize(CHECK_CAN_READ_REPORTS) privilege. Executes queries as read-only
     * via nativeQueries.runReadOnly() which prevents DML/DDL operations.
     * </p>
     * <p>
     * Result transformation example:
     * <pre>
     * // Query result: List&lt;LinkedHashMap&lt;String, Object&gt;&gt;
     * // [{"name":"John","email":"john@example.com"},{"name":"Jane","email":"jane@example.com"}]
     * // Transformed to:
     * // Headers: ["name", "email"]
     * // Rows: [["John","john@example.com"],["Jane","jane@example.com"]]
     * // CSV output:
     * // name,email
     * // John,john@example.com
     * // Jane,jane@example.com
     * </pre>
     * </p>
     *
     * @param organizationId Optional organization ID for tenant-scoped query execution. Used for
     *                      authorization and multi-tenancy. Null indicates global context
     * @param reportId Optional ID of saved QueryReport. Used to retrieve report filename for CSV export.
     *                If null, generates generic timestamped filename (report_yyyy-MM-dd-HH-mm.csv).
     *                Example: Sales_Report_2025-01-15-14-30.csv
     * @param query SQL SELECT statement to execute. Results will be exported to CSV. Must be valid
     *             SQL syntax. Executed via nativeQueries.runReadOnly() for security
     * @param response HttpServletResponse for streaming CSV file content to client.
     *                Services.file.getFileContentAndPrepareResponse() writes content and sets headers
     *                (Content-Type: text/csv, Content-Disposition: attachment)
     * @throws SQLException If CSV creation encounters database access errors during file persistence
     *                     or query execution fails with unrecoverable SQL error
     * @throws IOException If CSV file writing fails or HTTP response streaming encounters I/O error
     */
    @PostMapping(_QUERY + _CSV)
    @PreAuthorize(CHECK_CAN_READ_REPORTS)
    public void runQueryToCsv(@PathVariable(name=ORGANIZATIONID, required = false) Long organizationId,
                              @RequestParam(value = "reportId", required = false) Long reportId,
                              @RequestParam("query") String query,
                              HttpServletResponse response) throws SQLException, IOException {
        debug("[runQueryToCsv]");

        List<LinkedHashMap<String, Object>> queryResult = new ArrayList<>();
        try {
            queryResult = nativeQueries.runReadOnly(query);
        } catch (InvalidDataAccessResourceUsageException | JpaSystemException | GenericJDBCException e) {
            error("[runQueryToCsv]", e);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
        List<LinkedHashMap<String, Object>> result = queryResult;

        File report = Flow.init()
                .then(a -> repositories.secure.queryReport.findOne(reportId))
                .thenSet(fileName, a -> a.result != null ? String.format("%s_%s.csv", a.result.getFileName(), dtf.format(LocalDateTime.now())) : String.format("report_%s.csv", dtf.format(LocalDateTime.now())))
                .thenSet(genericReportViewLinkedHashMap, a -> result)
                .thenSet(genericTableViewList, a -> !a.model.get(genericReportViewLinkedHashMap).isEmpty() ?
                        a.model.get(genericReportViewLinkedHashMap).stream().map(stringObjectMap -> stringObjectMap.values().stream().toList()).collect(Collectors.toList())
                        : new ArrayList<>())
                .thenSet(genericTableHeaders, a -> !a.model.get(genericReportViewLinkedHashMap).isEmpty() ? a.model.get(genericReportViewLinkedHashMap).get(0).keySet().toArray(String[]::new) : new String[]{})
                .thenSet(file, a -> {
                    try {
                        return services.csv.createCSV(a.model.get(fileName), a.model.get(genericTableViewList), a.model.get(genericTableHeaders));
                    } catch (IOException | SQLException e) {
                        error("[runQueryToCsv]", e);
                        return null;
                    }
                })
                .execute()
                .get(file);
        services.file.getFileContentAndPrepareResponse(report, true, false, response);
    }

    /**
     * Retrieves saved QueryReport by ID and re-executes its SQL query with current data.
     * <p>
     * Loads QueryReport entity from secure repository using repositories.secure.queryReport.findOne(reportId).
     * If report exists, delegates to runQuery() method to execute the report's stored SQL query and
     * render results. If report not found (null), returns HTTP 404 Not Found response. Useful for
     * report library functionality where users can save, list, and re-run reports. Respects
     * organization-scoped authorization via secure repository access.
     * </p>
     * <p>
     * Flow: 1) Load QueryReport by reportId via repositories.secure.queryReport.findOne(),
     * 2) If found: delegate to runQuery(organizationId, report.getQuery(), reportId, resultView),
     * 3) If null: return ResponseEntity.notFound().
     * </p>
     * <p>
     * Protected by @PreAuthorize(CHECK_CAN_READ_REPORTS) privilege. Uses
     * repositories.secure.queryReport.findOne() which enforces privilege-based access control.
     * Only returns reports user has permission to read based on global/organization privileges.
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * // Re-run saved report with custom view
     * getReport(orgId, 42L, "report-chart-view");
     * // Loads QueryReport ID 42, executes its query, renders as chart
     * </pre>
     * </p>
     *
     * @param organizationId Optional organization ID for tenant-scoped report access. Used for
     *                      authorization checks. Passed through to runQuery() for execution context.
     *                      Null indicates global report
     * @param reportId Required ID of saved QueryReport entity to retrieve and execute. Must exist
     *                in database or returns 404. Used to load report metadata (SQL query, parameters,
     *                name) from repositories.secure.queryReport
     * @param resultView Thymeleaf template name for rendering report results (default 'report-data-table').
     *                  Passed through to runQuery() method. Allows customization of result presentation format
     * @return On success: ModelAndView from runQuery() with report results, form, and metadata.
     *        On report not found: ResponseEntity.notFound() with HTTP 404 status. Return type Object
     *        supports both response types
     */
    @GetMapping(_ID)
    @PreAuthorize(CHECK_CAN_READ_REPORTS)
    public Object getReport(@PathVariable(name=ORGANIZATIONID, required = false) Long organizationId,
                            @PathVariable(name=ID) Long reportId,
                            @RequestParam(value = "resultView", defaultValue = "report-data-table") String resultView) {
        debug("[getReport]");
        QueryReport queryReportById = repositories.secure.queryReport.findOne(reportId);
        if(queryReportById != null) {
            return runQuery(organizationId, queryReportById.getQuery(), reportId, resultView);
        } else {
            return ResponseEntity.notFound();
        }
    }
}
