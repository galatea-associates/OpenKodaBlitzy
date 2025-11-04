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

package com.openkoda.core.job;

import com.openkoda.core.audit.SystemHealthStatus;
import com.openkoda.core.service.system.SystemHealthStatusService;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.inject.Inject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Scheduled background job for monitoring system health and resource usage.
 * <p>
 * This job is invoked automatically by {@link JobsScheduler} using cron scheduling with the expression
 * {@code ${scheduled.systemHealth.check:0 0 4 * * ?}}, which defaults to daily execution at 4:00 AM.
 * The job monitors critical system resources including RAM usage, CPU usage, and disk space usage
 * against configurable threshold values. When any resource exceeds its configured threshold, the job
 * logs error-level messages to alert system administrators of potential capacity issues.
 * <p>
 * This class implements {@link LoggingComponentWithRequestId} to provide request-id-aware tracing
 * capabilities for correlation of health check executions across distributed logs.
 * <p>
 * Monitored metrics:
 * <ul>
 *   <li>RAM Usage: JVM heap memory utilization as percentage of maximum heap</li>
 *   <li>Disk Space: Aggregate disk usage across all mounted partitions</li>
 *   <li>CPU Usage: Total CPU utilization across all processes (requires sysstat)</li>
 * </ul>
 *
 * @see JobsScheduler
 * @see SystemHealthStatusService
 * @see SystemHealthStatus
 * @since 1.7.1
 * @version 1.7.1
 * @author OpenKoda Team
 */
@Component
public class SystemHealthAlertJob implements LoggingComponentWithRequestId {

    /**
     * Service providing system health metrics including RAM, CPU, and disk usage statistics.
     * Injected via Spring dependency injection to retrieve current system status snapshots.
     *
     * @see SystemHealthStatusService#statusNow()
     */
    @Inject
    SystemHealthStatusService statHelper;

    /**
     * Maximum allowed disk space usage threshold as a percentage (0-100).
     * <p>
     * Configurable via Spring property {@code max.disk.percentage} with a default value of 75%.
     * When disk usage exceeds this threshold, an error-level log message is generated during
     * the scheduled health check. This value represents aggregate usage across all mounted
     * partitions monitored by the system.
     * 
     *
     * @see #checkSystem()
     */
    @Value("${max.disk.percentage:75}")
    double maxUsedDiskSpacePercentageAllowed;

    /**
     * Maximum allowed RAM usage threshold as a percentage (0-100).
     * <p>
     * Configurable via Spring property {@code max.ram.percentage} with a default value of 75%.
     * When JVM heap memory usage exceeds this threshold, an error-level log message is generated
     * during the scheduled health check. This value is calculated as
     * {@code (totalHeapMemory / maxHeapMemory) * 100}.
     * 
     *
     * @see #checkSystem()
     */
    @Value("${max.ram.percentage:75}")
    double maxUsedRamSpacePercentageAllowed;

    /**
     * Maximum allowed CPU usage threshold as a percentage (0-100).
     * <p>
     * Configurable via Spring property {@code max.cpu.percentage} with a default value of 75%.
     * When aggregate CPU usage across all processes exceeds this threshold, an error-level log
     * message is generated during the scheduled health check. CPU monitoring requires the
     * {@code sysstat} tool to be installed and enabled on the system.
     * 
     *
     * @see #checkSystem()
     * @see #getCpuUsage(SystemHealthStatus)
     */
    @Value("${max.cpu.percentage:75}")
    double maxCpuUsagePercentageAllowed;

    /**
     * Executes comprehensive system health checks against configured resource thresholds.
     * <p>
     * This method retrieves the current system status via {@link SystemHealthStatusService#statusNow()}
     * and evaluates RAM usage, disk space usage, and CPU usage against their respective maximum
     * allowed thresholds. Any resource exceeding its threshold triggers an error-level log entry
     * with the current usage percentage.
     * 
     * <p>
     * Execution flow:
     * <ol>
     *   <li><b>RAM Check</b> (lines 61-73): Calculates heap memory usage as
     *       {@code (totalHeapMemory / maxHeapMemory) * 100} and compares against
     *       {@link #maxUsedRamSpacePercentageAllowed}. Logs error if threshold exceeded.</li>
     *   <li><b>Disk Check</b> (lines 62-82): Aggregates total and free space across all
     *       mounted partitions using Java streams. Calculates usage percentage as
     *       {@code (1 - freeSpace / totalSpace) * 100}. Uses sentinel value {@code -1}
     *       when partition data is unavailable. Logs error if threshold exceeded or
     *       calculation fails.</li>
     *   <li><b>CPU Check</b> (lines 83-88): Only executes if {@code sysstat} is enabled.
     *       Delegates to {@link #getCpuUsage(SystemHealthStatus)} to calculate aggregate
     *       CPU usage. Logs error if threshold exceeded.</li>
     * </ol>
     * 
     * <p>
     * All threshold violations produce error-level logs via {@code error()} method from
     * {@link LoggingComponentWithRequestId}, including current usage values for diagnostic purposes.
     * 
     *
     * @see SystemHealthStatusService#statusNow()
     * @see SystemHealthStatus
     * @see #maxUsedRamSpacePercentageAllowed
     * @see #maxUsedDiskSpacePercentageAllowed
     * @see #maxCpuUsagePercentageAllowed
     */
    public void checkSystem() {
        debug("[checkSystem]");
        SystemHealthStatus systemHealthStatus = statHelper.statusNow();
        double usedRam = ((double) systemHealthStatus.getTotalHeapMemory() / systemHealthStatus.getMaxHeapMemory()) * 100.0;
        double totalSpace = (double) systemHealthStatus.getPartitions().stream()
                .map(systemHealthStatus::getTotalSpace)
                .reduce(Long::sum)
                .orElse(-1L);
        double freeSpace = (double) systemHealthStatus.getPartitions().stream()
                .map(systemHealthStatus::getFreeSpace)
                .reduce(Long::sum)
                .orElse(-1L);

        if (usedRam > maxUsedRamSpacePercentageAllowed) {
            error("[checkSystem] RAM usage is above allowed levels. Currently at {} ", usedRam);
        }

        if (totalSpace > 0 && freeSpace > 0) {
            double usedDiskSpace = (1L - freeSpace / totalSpace) * 100.0;
            if (usedDiskSpace > maxUsedDiskSpacePercentageAllowed) {
                error("[checkSystem] Disk usage is above allowed levels. Currently at {} ", usedDiskSpace);
            }
        } else {
            error("[checkSystem] Error occurred when calculating disk usage");
        }
        if (systemHealthStatus.isSysstatEnabled()) {
            double cpuUsage = getCpuUsage(systemHealthStatus);
            if (cpuUsage > maxCpuUsagePercentageAllowed) {
                error("[checkSystem] CPU usage is above allowed levels. Currently at {} ", cpuUsage);
            }
        }
    }

    /**
     * Calculates total CPU usage percentage across all processes from pidstat data.
     * <p>
     * This private helper method parses the pidstat output contained in the provided
     * {@link SystemHealthStatus} object to extract and aggregate CPU usage percentages.
     * The implementation locates the {@code %CPU} column index within the pidstat header,
     * then sums the CPU percentage values across all process data rows to produce a
     * total system CPU utilization metric.
     * 
     * <p>
     * The method relies on the external {@code sysstat} tool (specifically the {@code pidstat}
     * command) being installed and available on the system. If pidstat data is unavailable
     * or malformed, the method returns {@code -1.0} as a sentinel value.
     * 
     *
     * @param systemHealthStatus the {@link SystemHealthStatus} object containing pidstat header
     *                           and data rows retrieved from the system
     * @return the aggregate CPU usage percentage across all processes, or {@code -1.0} if
     *         pidstat data is unavailable or cannot be parsed
     * @throws NumberFormatException if a CPU percentage value in the pidstat data is malformed
     *                               and cannot be parsed as a double
     * @throws IndexOutOfBoundsException if the calculated {@code cpuIndex} is invalid for the
     *                                   data row structure
     * @see SystemHealthStatus#getPidstatHeader()
     * @see SystemHealthStatus#getPidstatData()
     * @see SystemHealthStatus#isSysstatEnabled()
     */
    private double getCpuUsage(SystemHealthStatus systemHealthStatus) {
        int cpuIndex = Arrays.asList(systemHealthStatus.getPidstatHeader()).indexOf("%CPU");
        return systemHealthStatus.getPidstatData().stream()
                .map(row -> Double.parseDouble(row[cpuIndex]))
                .reduce(Double::sum).orElse(-1.0);
    }
}
