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

package com.openkoda.core.audit;

import com.openkoda.core.service.system.SystemHealthStatusService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Data transfer object capturing system health metrics for monitoring and diagnostics.
 * <p>
 * Holds JVM heap memory statistics (current, maximum, free), PostgreSQL logging configuration
 * (log_statement, log_min_duration_statement), disk partition space (free/total), operating system
 * detection (Windows/Linux), sysstat tool availability, and pidstat process metrics. Populated by
 * SystemHealthStatusService and consumed by health monitoring endpoints and admin dashboards.
 * </p>
 * <p>
 * This mutable JavaBean provides no synchronization. Instances are not thread-safe and should be
 * accessed from a single thread or externally synchronized.
 * </p>
 *
 * @see SystemHealthStatusService
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class SystemHealthStatus {

    /**
     * Current size of heap in bytes
     */
    private long totalHeapMemory;
    /**
     * Maximum size of heap in bytes
     */
    private long maxHeapMemory;

    /**
     * Free memory within the heap in bytes
     */
    private long freeHeapMemory;

    /**
     * log_statement information from pg_settings
     * See {@link SystemHealthStatusService#getLogStatement}
     */
    private String dbLogStatement;


    /**
     * log_min_duration_statement information from pg_settings
     * See {@link SystemHealthStatusService#getLogMinDurationStatement}
     */
    private String logMinDurationStatement;

    /**
     * Should be true when application runs on Windows
     */
    private boolean isWindows;
    /**
     * Should be true when runs on linux and sysstat tool enabled
     */
    private boolean sysstatEnabled;
    /**
     * Should be true when runs on linux and sysstat tool installed
     */
    private boolean sysstatInstalled;

    /**
     * Keeps free disk space for drives
     */
    private Map<String, Long> freePartitionSpace = new HashMap<>();
    /**
     * Keeps total disk space for drives
     */
    private Map<String, Long> totalPartitionSpace = new HashMap<>();

    /**
     * Keeps processes data read from pidstat (if sysstat installed)
     */
    private List<String[]> pidstatData;
    /**
     * Keeps processes headers from pidstat (if sysstat installed)
     */
    private String[] pidstatHeader;

    /**
     * Stores disk space metrics for the specified partition path.
     *
     * @param path the partition path identifier (e.g., "/", "/home", "C:\\")
     * @param freeSpace the available space in bytes on the partition
     * @param totalSpace the total capacity in bytes of the partition
     */
    public void setDiskSpace(String path, long freeSpace, long totalSpace) {
        freePartitionSpace.put(path, freeSpace);
        totalPartitionSpace.put(path, totalSpace);
    }

    /**
     * Returns all partition paths with recorded disk space metrics.
     *
     * @return set of partition paths for which disk space has been recorded
     */
    public Set<String> getPartitions() {
        return freePartitionSpace.keySet();
    }

    /**
     * Returns the free space for the specified partition.
     *
     * @param partition the partition path to query
     * @return free space in bytes, or null if partition not found
     */
    public Long getFreeSpace(String partition) {
        return freePartitionSpace.get(partition);
    }

    /**
     * Returns the total space for the specified partition.
     *
     * @param partition the partition path to query
     * @return total space in bytes, or null if partition not found
     */
    public Long getTotalSpace(String partition) {
        return totalPartitionSpace.get(partition);
    }

    /**
     * Returns the current total heap memory size.
     *
     * @return current size of heap in bytes
     */
    public long getTotalHeapMemory() {
        return totalHeapMemory;
    }

    /**
     * Sets the current total heap memory size.
     *
     * @param totalHeapMemory the current heap size in bytes
     */
    public void setTotalHeapMemory(long totalHeapMemory) {
        this.totalHeapMemory = totalHeapMemory;
    }

    /**
     * Returns the maximum heap memory size.
     *
     * @return maximum size of heap in bytes
     */
    public long getMaxHeapMemory() {
        return maxHeapMemory;
    }

    /**
     * Sets the maximum heap memory size.
     *
     * @param maxHeapMemory the maximum heap size in bytes
     */
    public void setMaxHeapMemory(long maxHeapMemory) {
        this.maxHeapMemory = maxHeapMemory;
    }

    /**
     * Returns the free heap memory size.
     *
     * @return free memory within the heap in bytes
     */
    public long getFreeHeapMemory() {
        return freeHeapMemory;
    }

    /**
     * Sets the free heap memory size.
     *
     * @param freeHeapMemory the free heap memory in bytes
     */
    public void setFreeHeapMemory(long freeHeapMemory) {
        this.freeHeapMemory = freeHeapMemory;
    }

    /**
     * Returns the PostgreSQL log_statement configuration value.
     *
     * @return the log_statement setting from pg_settings
     */
    public String getDbLogStatement() {
        return dbLogStatement;
    }

    /**
     * Sets the PostgreSQL log_statement configuration value.
     *
     * @param dbLogStatement the log_statement setting from pg_settings
     */
    public void setDbLogStatement(String dbLogStatement) {
        this.dbLogStatement = dbLogStatement;
    }

    /**
     * Returns the PostgreSQL log_min_duration_statement configuration value.
     *
     * @return the log_min_duration_statement setting from pg_settings
     */
    public String getLogMinDurationStatement() {
        return logMinDurationStatement;
    }

    /**
     * Sets the PostgreSQL log_min_duration_statement configuration value.
     *
     * @param logMinDurationStatement the log_min_duration_statement setting from pg_settings
     */
    public void setLogMinDurationStatement(String logMinDurationStatement) {
        this.logMinDurationStatement = logMinDurationStatement;
    }

    /**
     * Sets whether the sysstat tool is enabled for process monitoring.
     *
     * @param systatEnabled true when running on Linux with sysstat tool enabled
     */
    public void setSysstatEnabled(boolean systatEnabled) {
        this.sysstatEnabled = systatEnabled;
    }

    /**
     * Returns whether the sysstat tool is enabled.
     *
     * @return true when running on Linux with sysstat tool enabled
     */
    public boolean isSysstatEnabled() {
        return sysstatEnabled;
    }

    /**
     * Sets whether the sysstat tool is installed on the system.
     *
     * @param systatEnabled true when running on Linux with sysstat tool installed
     */
    public void setSysstatInstalled(boolean systatEnabled) {
        this.sysstatInstalled = systatEnabled;
    }

    /**
     * Returns whether the sysstat tool is installed.
     *
     * @return true when running on Linux with sysstat tool installed
     */
    public boolean isSysstatInstalled() {
        return sysstatInstalled;
    }

    /**
     * Returns whether the application is running on Windows.
     *
     * @return true when application runs on Windows operating system
     */
    public boolean isWindows() {
        return isWindows;
    }

    /**
     * Sets whether the application is running on Windows.
     *
     * @param isWindows true when application runs on Windows operating system
     */
    public void setIsWindows(boolean isWindows) {
        this.isWindows = isWindows;
    }

    /**
     * Parses pidstat output rows and stores header separately from data rows.
     *
     * @param pidstatRows list of pidstat rows where first row is header and subsequent rows are process data
     */
    public void setPidstatData(List<String[]> pidstatRows) {
        this.pidstatHeader = pidstatRows.get(0);
        this.pidstatData = pidstatRows.subList(1, pidstatRows.size());
    }

    /**
     * Returns the pidstat process data rows.
     *
     * @return list of pidstat data rows excluding header, or null if not set
     */
    public List<String[]> getPidstatData() {
        return this.pidstatData;
    }

    /**
     * Returns the pidstat column headers.
     *
     * @return array of pidstat column headers, or null if not set
     */
    public String[] getPidstatHeader() {
        return pidstatHeader;
    }
}
