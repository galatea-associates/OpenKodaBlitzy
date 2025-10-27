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

package com.openkoda.core.service.backup;

/**
 * Feature flags for backup scope configuration used by backup orchestration services.
 * <p>
 * This enum defines the set of backup operations that can be selectively enabled or disabled
 * when creating system backups. The backup orchestration layer (typically {@link BackupWriter})
 * consumes a {@code Collection<BackupOption>} or {@code EnumSet<BackupOption>} to determine
 * which backup tasks to execute during a backup operation.
 * </p>
 * <p>
 * Common usage patterns include:
 * <ul>
 *     <li>Database-only backup: {@code EnumSet.of(BackupOption.BACKUP_DATABASE)}</li>
 *     <li>Full local backup: {@code EnumSet.of(BACKUP_DATABASE, BACKUP_PROPERTIES)}</li>
 *     <li>Full backup with remote copy: {@code EnumSet.allOf(BackupOption.class)}</li>
 * </ul>
 * </p>
 * <p>
 * The enum serves as a contract boundary between backup callers and the backup implementation.
 * Renaming or removing constants is a breaking API change that affects:
 * <ul>
 *     <li>Direct callers using explicit enum references</li>
 *     <li>Persisted configuration files or database records storing option names</li>
 *     <li>Log files and monitoring systems parsing option values</li>
 *     <li>Integration tests validating backup behavior</li>
 * </ul>
 * </p>
 * <p>
 * Integration with {@link BackupWriter}:
 * <ul>
 *     <li>{@link BackupWriter#doBackup(java.util.Collection)} accepts a collection of these options
 *         to orchestrate the complete backup workflow</li>
 *     <li>{@link BackupWriter#copyBackupFile(java.util.Collection, java.io.File)} checks for
 *         {@link #SCP_ENABLED} to determine whether to transfer the backup to a remote host</li>
 * </ul>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see BackupWriter
 */
public enum BackupOption {
    
    /**
     * Enables database backup via PostgreSQL pg_dump utility.
     * <p>
     * When this option is present, the backup orchestration executes {@code pg_dump} to create
     * a complete SQL dump of the configured PostgreSQL database. The dump is written to a
     * timestamped .sql file in the backup directory.
     * </p>
     * <p>
     * The database connection parameters (host, database name, username, password) are derived
     * from the Spring datasource configuration. The PGPASSWORD environment variable is injected
     * into the pg_dump subprocess for authentication.
     * </p>
     * <p>
     * This operation requires:
     * <ul>
     *     <li>PostgreSQL pg_dump utility available in system PATH or configured via properties</li>
     *     <li>Valid database credentials in application configuration</li>
     *     <li>Network connectivity to the database server</li>
     *     <li>Sufficient disk space in the backup directory</li>
     * </ul>
     * </p>
     */
    BACKUP_DATABASE,
    
    /**
     * Enables application properties file backup.
     * <p>
     * When this option is present, the backup orchestration includes the application's
     * configuration properties file (typically application.properties or application.yml)
     * in the backup archive. This ensures that configuration settings can be restored
     * along with database data.
     * </p>
     * <p>
     * The properties file path is determined by the {@code backup.application.properties.file.path}
     * configuration property. If the file is not found or not readable, the backup operation
     * logs a warning but continues with other enabled tasks.
     * </p>
     */
    BACKUP_PROPERTIES,
    
    /**
     * Enables secure copy (SCP) transfer of backup archive to a remote host.
     * <p>
     * When this option is present, the backup orchestration uses the {@code scp} command-line
     * utility to transfer the generated backup archive (.tar.gz or .tar.gz.gpg) to a configured
     * remote server. This provides off-site backup storage for disaster recovery scenarios.
     * </p>
     * <p>
     * Remote transfer configuration is provided via application properties:
     * <ul>
     *     <li>{@code backup.scp.host}: Target hostname or IP address</li>
     *     <li>{@code backup.scp.target.directory}: Destination directory path on remote host</li>
     *     <li>SSH key authentication must be configured externally (via ~/.ssh/config or ssh-agent)</li>
     * </ul>
     * </p>
     * <p>
     * This operation requires:
     * <ul>
     *     <li>SCP utility available in system PATH or configured via properties</li>
     *     <li>SSH key-based authentication configured for passwordless access</li>
     *     <li>Network connectivity to the remote host</li>
     *     <li>Write permissions to the target directory on remote host</li>
     * </ul>
     * </p>
     * <p>
     * If SCP transfer fails, the backup operation returns false and logs diagnostic information
     * to backup_error.log, but the local backup archive remains intact.
     * </p>
     */
    SCP_ENABLED
}