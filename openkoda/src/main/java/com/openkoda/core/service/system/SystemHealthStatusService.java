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

package com.openkoda.core.service.system;

import com.openkoda.core.audit.SystemHealthStatus;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.persistence.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * System health monitoring service aggregating JVM, filesystem, and PostgreSQL metrics into SystemHealthStatus DTO.
 * <p>
 * This service provides comprehensive health snapshots for diagnostic endpoints and monitoring systems. It captures
 * Runtime heap metrics (maximum, free, and total memory), collects filesystem disk space from root directories, and
 * queries PostgreSQL pg_settings for log configuration. On Linux systems, it probes sysstat availability and runs
 * pidstat for process-level metrics including CPU, memory, and disk I/O statistics.
 * </p>
 * <p>
 * Environment-specific behavior: The {@code isWindows} flag short-circuits Linux-specific diagnostics. Sysstat detection
 * uses the {@code apt list} command to check package installation, and enablement is verified by reading
 * {@code /etc/default/sysstat} for {@code ENABLED="true"}.
 * </p>
 * <p>
 * Core responsibilities:
 * <ul>
 * <li>Captures Runtime heap metrics: {@code maxMemory()}, {@code freeMemory()}, {@code totalMemory()}</li>
 * <li>Collects filesystem disk space from root directories via {@code FileSystems.getDefault().getRootDirectories()}</li>
 * <li>Queries PostgreSQL {@code pg_settings} for {@code log_statement} and {@code log_min_duration_statement}</li>
 * <li>On Linux: Probes sysstat availability and enablement, executes pidstat for process metrics</li>
 * </ul>
 * </p>
 * <p>
 * Exception handling: All external process and database interactions are wrapped in try/catch blocks with error logging.
 * Failures return conservative defaults. EntityManager instances are always closed in finally blocks to prevent resource leaks.
 * </p>
 * <p>
 * Thread-safety: This is a stateless service suitable for concurrent health checks from multiple threads.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * SystemHealthStatus status = systemHealthStatusService.statusNow();
 * }</pre>
 * </p>
 * <p>
 * Integration points: Uses {@link LoggingComponentWithRequestId} for tracing, produces {@link SystemHealthStatus} DTO
 * for metrics transport, consumed by ApplicationStatusController {@code /ping} endpoint.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SystemHealthStatus
 * @see LoggingComponentWithRequestId
 */
@Service
public class SystemHealthStatusService implements LoggingComponentWithRequestId {

    /**
     * Path to the cat executable for reading system configuration files.
     * <p>
     * Configurable via {@code system.cat.executable} property. Defaults to {@code cat}.
     * Used to read {@code /etc/default/sysstat} for sysstat enablement status.
     * </p>
     */
    @Value("${system.cat.executable:cat}")
    private String catExecutable;

    /**
     * Path to the apt executable for checking package installation status.
     * <p>
     * Configurable via {@code system.apt.executable} property. Defaults to {@code apt}.
     * Used to verify sysstat package installation via {@code apt list --installed sysstat}.
     * </p>
     */
    @Value("${system.apt.executable:apt}")
    private String aptExecutable;

    /**
     * Path to the pidstat executable for process-level statistics collection.
     * <p>
     * Configurable via {@code system.pidstat.executable} property. Defaults to {@code pidstat}.
     * Executed with {@code -urd -h} flags to capture CPU, memory, and disk I/O metrics in human-readable format.
     * </p>
     */
    @Value("${system.pidstat.executable:pidstat}")
    private String pidstatExecutable;

    /**
     * EntityManagerFactory for database queries against PostgreSQL pg_settings.
     * <p>
     * Used to retrieve logging configuration: {@code log_statement} and {@code log_min_duration_statement}.
     * EntityManager instances are created per query and closed in finally blocks.
     * </p>
     */
    @PersistenceUnit
    EntityManagerFactory entityManagerFactory;

    /**
     * Platform detection flag indicating Windows operating system.
     * <p>
     * Set to {@code true} if the {@code os.name} system property starts with "windows" (case-insensitive).
     * When {@code true}, Linux-specific diagnostics (sysstat, pidstat) are short-circuited.
     * </p>
     */
    public static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    /**
     * Captures a comprehensive health snapshot aggregating JVM, filesystem, and database metrics.
     * <p>
     * This is the main entry point for system health diagnostics. The method collects:
     * <ul>
     * <li>JVM heap memory: maximum, free, and total heap memory from {@code Runtime}</li>
     * <li>Filesystem disk space: free and total space for all root directories</li>
     * <li>PostgreSQL logging configuration: {@code log_statement} and {@code log_min_duration_statement} settings</li>
     * <li>On Linux (non-Windows): sysstat installation and enablement status, pidstat process metrics if available</li>
     * </ul>
     * </p>
     * <p>
     * Behavior on Windows: Sets {@code isWindows} flag and returns immediately after basic metrics, skipping sysstat checks.
     * </p>
     * <p>
     * Behavior on Linux: Performs progressive health checks:
     * <ol>
     * <li>If sysstat not installed: Sets {@code sysstatInstalled=false} and returns</li>
     * <li>If sysstat not enabled: Sets {@code sysstatEnabled=false} and returns</li>
     * <li>If fully available: Collects pidstat data and includes in status</li>
     * </ol>
     * </p>
     * <p>
     * Exception handling: Database and process execution failures are logged but do not throw exceptions.
     * Conservative defaults are returned for failed metric collections.
     * </p>
     *
     * @return SystemHealthStatus DTO containing comprehensive system health metrics
     * @see SystemHealthStatus
     * @see #getLogStatement()
     * @see #getLogMinDurationStatement()
     * @see #isSysstatInstalled()
     * @see #isSysstatEnabled()
     * @see #getPidstatRows()
     */
    public SystemHealthStatus statusNow() {
        debug("[statusNow]");
        SystemHealthStatus status = new SystemHealthStatus();
        status.setMaxHeapMemory(Runtime.getRuntime().maxMemory());
        status.setFreeHeapMemory(Runtime.getRuntime().freeMemory());
        status.setTotalHeapMemory(Runtime.getRuntime().totalMemory());
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            File rootFile = root.toFile();
            status.setDiskSpace(rootFile.getPath(), rootFile.getFreeSpace(), rootFile.getTotalSpace());

        }
        status.setDbLogStatement(getLogStatement());
        status.setLogMinDurationStatement(getLogMinDurationStatement());
        if (isWindows) {
            status.setIsWindows(true);
            return status;
        }
        if (!isSysstatInstalled()) {
            status.setSysstatInstalled(false);
            return status;
        }
        status.setSysstatInstalled(true);
        if (!isSysstatEnabled()) {
            status.setSysstatEnabled(false);
            return status;
        }
        status.setSysstatEnabled(true);
        status.setPidstatData(getPidstatRows());
        return status;
    }

    /**
     * Executes pidstat command and parses output into rows of process-level metrics.
     * <p>
     * Runs {@code pidstat -urd -h} which captures:
     * <ul>
     * <li>{@code -u}: CPU utilization statistics</li>
     * <li>{@code -r}: Memory utilization statistics</li>
     * <li>{@code -d}: Disk I/O statistics</li>
     * <li>{@code -h}: Human-readable output format</li>
     * </ul>
     * </p>
     * <p>
     * Parsing behavior: Skips the first two header lines, then tokenizes each line on whitespace.
     * Joins the first two tokens (timestamp and identifier) into a single first element.
     * Example transformation: {@code ["10:30:15", "AM", "123", ...]} becomes {@code ["10:30:15 AM", "123", ...]}.
     * </p>
     * <p>
     * Process execution: Uses {@link ProcessBuilder} with configured {@code pidstatExecutable} path.
     * Reads stdout via {@code IOUtils.toString()}. Logs stderr if non-blank.
     * </p>
     * <p>
     * Exception handling: IOException and InterruptedException are caught and logged.
     * Returns empty list on failure, ensuring the health check does not fail catastrophically.
     * </p>
     *
     * @return List of String arrays, each representing a pidstat output row with parsed fields.
     *         Returns empty list if pidstat execution fails or parsing encounters errors.
     * @see ProcessBuilder
     */
    private List<String[]> getPidstatRows() {
        debug("[getPidstatRows]");
        List<String[]> cells = new ArrayList<>();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(pidstatExecutable, "-urd", "-h");
            Process p = processBuilder.start();
            p.waitFor();
            String pidstat = IOUtils.toString(p.getInputStream());
            String errors = IOUtils.toString(p.getErrorStream());
            if (StringUtils.isNotBlank(errors)) {
                error("Error during running pidstat: \n{}", errors);
            }
            List<String> lines = Arrays.asList(pidstat.split("\\n"));
            lines = lines.subList(2, lines.size());
            for (String s : lines) {
                String[] tmp = s.split("\\s+");
                String firstElem = tmp[0] + " " + tmp[1];
                tmp = Arrays.copyOfRange(tmp, 1, tmp.length);
                tmp[0] = firstElem;
                cells.add(tmp);
            }
        } catch (IOException | InterruptedException e) {
            error("Error during getting pidstat data", e);
        }
        return cells;
    }

    /**
     * Queries PostgreSQL pg_settings for the log_min_duration_statement configuration setting.
     * <p>
     * This setting controls the minimum execution time (in milliseconds) above which statements are logged.
     * A value of {@code -1} disables duration-based logging. A value of {@code 0} logs all statements.
     * </p>
     * <p>
     * Database interaction: Creates an {@link EntityManager} from the injected {@code EntityManagerFactory},
     * sets {@code FlushModeType.AUTO}, begins a transaction, executes native SQL query against {@code pg_settings},
     * commits the transaction, and closes the EntityManager in a finally block.
     * </p>
     * <p>
     * Query executed: {@code SELECT setting FROM pg_settings WHERE name = 'log_min_duration_statement'}
     * </p>
     * <p>
     * Resource management: The EntityManager is guaranteed to be closed in the finally block,
     * preventing connection leaks even if exceptions occur during query execution.
     * </p>
     *
     * @return String value of the {@code log_min_duration_statement} setting from PostgreSQL configuration.
     *         Typical values include {@code -1} (disabled), {@code 0} (log all), or millisecond thresholds like {@code 1000}.
     * @throws RuntimeException if database query fails or setting does not exist in pg_settings
     * @see EntityManager
     * @see EntityManagerFactory
     */
    public String getLogMinDurationStatement() {
        debug("[getLogMinDurationStatement]");
        String result;
        EntityManager em = null;
        try {
            em = entityManagerFactory.createEntityManager();
            em.setFlushMode(FlushModeType.AUTO);
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            result = (String) em.createNativeQuery("SELECT setting FROM pg_settings WHERE name = 'log_min_duration_statement'").getSingleResult();
            transaction.commit();
            em.close();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
                em = null;
            }
        }
        return result;
    }

    /**
     * Queries PostgreSQL pg_settings for the log_statement configuration setting.
     * <p>
     * This setting controls which SQL statements are logged. Valid values include:
     * <ul>
     * <li>{@code none}: No statements logged (default)</li>
     * <li>{@code ddl}: Log all DDL statements (CREATE, ALTER, DROP)</li>
     * <li>{@code mod}: Log all DDL plus data-modifying statements (INSERT, UPDATE, DELETE)</li>
     * <li>{@code all}: Log all statements including SELECT</li>
     * </ul>
     * </p>
     * <p>
     * Database interaction: Creates an {@link EntityManager} from the injected {@code EntityManagerFactory},
     * sets {@code FlushModeType.AUTO}, begins a transaction, executes native SQL query against {@code pg_settings},
     * commits the transaction, and closes the EntityManager in a finally block.
     * </p>
     * <p>
     * Query executed: {@code SELECT setting FROM pg_settings WHERE name = 'log_statement'}
     * </p>
     * <p>
     * Resource management: The EntityManager is guaranteed to be closed in the finally block,
     * preventing connection leaks even if exceptions occur during query execution.
     * </p>
     *
     * @return String value of the {@code log_statement} setting from PostgreSQL configuration.
     *         One of: {@code none}, {@code ddl}, {@code mod}, or {@code all}.
     * @throws RuntimeException if database query fails or setting does not exist in pg_settings
     * @see EntityManager
     * @see EntityManagerFactory
     */
    public String getLogStatement() {
        debug("[getLogStatement]");
        String result;
        EntityManager em = null;
        try {
            em = entityManagerFactory.createEntityManager();
            em.setFlushMode(FlushModeType.AUTO);
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            result = (String) em.createNativeQuery("SELECT setting FROM pg_settings WHERE name = 'log_statement'").getSingleResult();
            transaction.commit();
            em.close();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
                em = null;
            }
        }
        return result;
    }

    /**
     * Checks sysstat package installation status using apt package manager.
     * <p>
     * Sysstat is a Linux utility collection providing system performance monitoring tools including pidstat.
     * This method verifies installation by executing {@code apt list --installed sysstat} and checking
     * whether the output contains the string "sysstat".
     * </p>
     * <p>
     * Platform behavior: Immediately returns {@code false} on Windows systems without attempting apt execution.
     * </p>
     * <p>
     * Process execution: Uses {@link ProcessBuilder} with configured {@code aptExecutable} path.
     * Executes {@code apt list --installed sysstat}, waits for completion, and reads stdout.
     * </p>
     * <p>
     * Exception handling: All exceptions during process execution are caught and logged.
     * Returns {@code false} on any error, ensuring health checks degrade gracefully.
     * </p>
     *
     * @return {@code true} if sysstat package is installed and detected via apt, {@code false} otherwise.
     *         Always returns {@code false} on Windows systems.
     * @see ProcessBuilder
     */
    public boolean isSysstatInstalled() {
        debug("[isSysstatInstalled]");
        if (isWindows) {
            return false;
        }
        try {
            ProcessBuilder pBuilder = new ProcessBuilder(aptExecutable, "list", "--installed", "sysstat");
            Process p = pBuilder.start();
            p.waitFor();
            String aptSysstat = IOUtils.toString(p.getInputStream());
            debug("[isSysstatInstalled] output: {}", aptSysstat);
            if (aptSysstat.contains("sysstat")) {
                return true;
            }
        } catch (Exception e) {
            error(e,"Error occurred when trying to determine availability of sysstat");
        }
        return false;
    }

    /**
     * Checks whether sysstat data collection is enabled by reading /etc/default/sysstat configuration.
     * <p>
     * On Debian-based systems, sysstat requires manual enablement via the {@code /etc/default/sysstat} file.
     * This method reads the file and checks for the line {@code ENABLED="true"}.
     * </p>
     * <p>
     * Process execution: Uses {@link ProcessBuilder} with configured {@code catExecutable} to read
     * {@code /etc/default/sysstat}. Searches file content for the exact string {@code ENABLED="true"}.
     * </p>
     * <p>
     * Exception handling: IOException and InterruptedException are caught and logged.
     * Returns {@code false} on any error, including file not found or read permission denied.
     * </p>
     * <p>
     * Prerequisite: This method should only be called after verifying sysstat installation via {@link #isSysstatInstalled()}.
     * </p>
     *
     * @return {@code true} if {@code /etc/default/sysstat} contains {@code ENABLED="true"}, {@code false} otherwise.
     *         Returns {@code false} if file cannot be read or does not exist.
     * @see #isSysstatInstalled()
     * @see ProcessBuilder
     */
    private boolean isSysstatEnabled() {
        debug("[isSysstatEnabled]");
        try {
            ProcessBuilder pBuilder = new ProcessBuilder(catExecutable, "/etc/default/sysstat");
            Process p = pBuilder.start();
            p.waitFor();
            String sysstat = IOUtils.toString(p.getInputStream());
            if (sysstat.contains("ENABLED=\"true\"")) {
                return true;
            }
        } catch (IOException | InterruptedException e) {
            error("Error occurred when trying to check if sysstat enabled", e);
        }
        return false;
    }
}
