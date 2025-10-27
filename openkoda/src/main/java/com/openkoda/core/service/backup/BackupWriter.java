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

import com.google.common.collect.ImmutableList;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.annotation.PostConstruct;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Runtime orchestration service for creating, packaging, optionally encrypting, and transferring backups.
 * <p>
 * This service provides comprehensive backup functionality for OpenKoda applications, including database dumps,
 * properties file backup, tar.gz archive creation, GPG encryption, and SCP remote transfer capabilities.
 * It orchestrates external system utilities (pg_dump, tar, gpg, scp) via ProcessBuilder to execute backup workflows.
 * </p>
 * <p>
 * <b>Key Responsibilities:</b>
 * <ul>
 *   <li>Database backup: Creates PostgreSQL dumps via pg_dump with authenticated connections</li>
 *   <li>Properties file backup: Captures application configuration for disaster recovery</li>
 *   <li>Archive creation: Packages backup files into compressed tar.gz archives</li>
 *   <li>GPG encryption: Optionally encrypts backup archives using configured GPG keys</li>
 *   <li>Remote transfer: Copies backup files to remote hosts via SCP when configured</li>
 * </ul>
 * </p>
 * <p>
 * <b>Configuration:</b> All operational parameters are injected via {@code @Value} properties:
 * <ul>
 *   <li>{@code spring.datasource.url}: Database connection URL for pg_dump target extraction</li>
 *   <li>{@code backup.file.directory}: Local filesystem directory for backup file storage</li>
 *   <li>{@code backup.gpg.key.name}: GPG key identifier for encryption (optional)</li>
 *   <li>{@code backup.scp.host}: Remote SCP destination host (optional)</li>
 *   <li>{@code backup.pg_dump.executable}: Path to pg_dump binary (default: "pg_dump")</li>
 * </ul>
 * See constructor parameters for complete configuration reference.
 * </p>
 * <p>
 * <b>Platform Dependencies:</b> Requires external system utilities in PATH or configured via properties:
 * <ul>
 *   <li>{@code pg_dump}: PostgreSQL backup utility (version must match database server)</li>
 *   <li>{@code tar}: Archive creation utility (standard on Unix/Linux systems)</li>
 *   <li>{@code gpg}: GNU Privacy Guard for encryption (optional, if encryption enabled)</li>
 *   <li>{@code scp}: Secure copy for remote transfer (optional, if remote backup enabled)</li>
 * </ul>
 * Missing binaries cause methods to return {@code false} without throwing exceptions.
 * </p>
 * <p>
 * <b>Integration:</b> Implements {@link LoggingComponentWithRequestId} for structured logging with request correlation IDs.
 * Used by higher-level BackupService orchestration components. Backup operations are controlled via
 * {@link BackupOption} enum flags passed to {@link #doBackup(Collection)} and {@link #copyBackupFile(Collection, File)}.
 * </p>
 * <p>
 * <b>Thread Safety:</b> NOT designed for concurrent invocations without external synchronization.
 * Holds transient mutable state (backupDir, backupDateInfo, databaseBackupFile, tarBackupFile) during operations.
 * Multiple simultaneous backup executions may result in file conflicts or corrupted state.
 * </p>
 * <p>
 * <b>Error Handling:</b> Returns boolean success/failure indicators. Logs errors via {@link LoggingComponentWithRequestId}
 * methods and redirects subprocess stderr output to {@code backup_error.log} file for diagnostics.
 * Does not throw exceptions for operational failures to prevent disruption of calling services.
 * </p>
 * <p>
 * <b>Example Usage:</b>
 * <pre>{@code
 * // Database-only backup
 * boolean success = backupWriter.doBackup(EnumSet.of(BackupOption.BACKUP_DATABASE));
 * 
 * // Full backup with encryption and remote copy
 * boolean success = backupWriter.doBackup(EnumSet.of(
 *     BackupOption.BACKUP_DATABASE,
 *     BackupOption.BACKUP_PROPERTIES
 * ));
 * File backupFile = new File(backupWriter.getTarBackupFile() + ".gpg");
 * backupWriter.copyBackupFile(EnumSet.of(BackupOption.SCP_ENABLED), backupFile);
 * }</pre>
 * </p>
 * <p>
 * <b>Operational Risks:</b>
 * <ul>
 *   <li>Missing external binaries (pg_dump, tar, gpg, scp) cause silent failures (returns false)</li>
 *   <li>Incorrect filesystem permissions prevent backup file creation</li>
 *   <li>Misconfigured properties (wrong database credentials, invalid GPG keys) lead to failures logged in backup_error.log</li>
 *   <li>Windows platform: tar and SCP operations are disabled (returns false)</li>
 * </ul>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see BackupOption
 * @see LoggingComponentWithRequestId
 */
@Component
public class BackupWriter implements LoggingComponentWithRequestId {

    /**
     * Filename for subprocess stderr redirection. All external command errors are appended to this file for diagnostics.
     */
    private static final String ERROR_LOGS_FILE_NAME = "backup_error.log";

    /** Database connection URL injected from {@code spring.datasource.url} property. Used to extract database name and host for pg_dump. */
    private final String datasourceUrl;
    
    /** Database username injected from {@code spring.datasource.username} property. Passed to pg_dump -U parameter. */
    private final String datasourceUsername;
    
    /** Database password injected from {@code spring.datasource.password} property. Set as PGPASSWORD environment variable for pg_dump. */
    private final String datasourcePassword;
    
    /** Date format pattern injected from {@code backup.date.pattern} property (default: "yyyyMMdd-HHmm"). Used to timestamp backup filenames. */
    private final String datePattern;
    
    /** Local backup directory path injected from {@code backup.file.directory} property. Created if not exists. */
    private final String fileDirectory;
    
    /** GPG key identifier injected from {@code backup.gpg.key.name} property. Empty string disables encryption. */
    private final String gpgKeyName;
    
    /** GPG key file path injected from {@code backup.gpg.key.file} property. Used to import key if not already present. */
    private final String gpgKeyFile;
    
    /** Application properties file path injected from {@code backup.application.properties} property. Backed up when BACKUP_PROPERTIES option enabled. */
    private final String applicationPropertiesFilePath;
    
    /** Application name injected from {@code application.name} property (default: "Default Application"). Used in backup archive filename. */
    private final String applicationName;
    
    /** SCP remote host injected from {@code backup.scp.host} property. Empty string disables remote copy. */
    private final String scpHost;
    
    /** SCP target directory path injected from {@code backup.scp.target} property. Remote destination for backup files. */
    private final String scpTargetDirectory;

    /** SimpleDateFormat instance initialized from {@link #datePattern} during {@link #init()}. Thread-unsafe, shared across operations. */
    private SimpleDateFormat dateFormat;
    
    /** OS detection flag set during {@link #init()}. True if running on Windows (disables tar and SCP operations). */
    private boolean isWindows;
    
    /** Backup directory File instance created during {@link #doBackup(Collection)}. Used to construct backup file paths. */
    private File backupDir;
    
    /** Formatted timestamp string (e.g., "_20231215-1430") appended to backup filenames. Set during pg_dump execution. */
    private String backupDateInfo;
    
    /** Full path to database dump SQL file (e.g., "/backups/openkoda_20231215-1430.sql"). Set during pg_dump execution. */
    private String databaseBackupFile;
    
    /** Full path to tar.gz archive file (e.g., "/backups/OpenKoda_20231215-1430.tar.gz"). Set during tar command execution. */
    private String tarBackupFile;
    
    /** SCP target path including host and remote directory. Set during {@link #copyBackupFile(Collection, File)} execution. */
    private String scpTargetFile;
    
    /** Path to pg_dump executable injected from {@code backup.pg_dump.executable} property (default: "pg_dump"). */
    private String pgDumpExecutable;
    
    /** Path to gpg executable injected from {@code backup.gpg.executable} property (default: "gpg"). */
    private String gpgExecutable;
    
    /** Path to scp executable injected from {@code backup.scp.executable} property (default: "scp"). */
    private String scpExecutable;

    /**
     * Constructs BackupWriter with injected configuration from Spring properties.
     * <p>
     * All parameters are injected via {@code @Value} annotations from application.properties or environment variables.
     * Empty string defaults for optional parameters (GPG, SCP) disable corresponding features.
     * </p>
     *
     * @param datasourceUrl Database connection URL from {@code spring.datasource.url}. 
     *                      Format: "jdbc:postgresql://host:port/database". Used to extract database name and host for pg_dump.
     * @param datasourceUsername Database username from {@code spring.datasource.username}. Passed to pg_dump -U parameter.
     * @param datasourcePassword Database password from {@code spring.datasource.password}. Set as PGPASSWORD environment variable.
     * @param datePattern Date format pattern from {@code backup.date.pattern} (default: "yyyyMMdd-HHmm"). 
     *                    Must be valid SimpleDateFormat pattern. Used to create unique backup filenames.
     * @param fileDirectory Local backup directory from {@code backup.file.directory} (default: empty string). 
     *                      If empty or nonexistent, directory is created during backup execution.
     * @param gpgKeyName GPG key identifier from {@code backup.gpg.key.name} (default: empty string). 
     *                   When empty, encryption is disabled. Should match existing GPG key name or key to be imported.
     * @param gpgKeyFile GPG key file path from {@code backup.gpg.key.file} (default: empty string). 
     *                   Used to import GPG key if {@code gpgKeyName} is specified but key not found in keyring.
     * @param applicationPropertiesFilePath Application properties file path from {@code backup.application.properties} (default: empty string).
     *                                       Backed up when BackupOption.BACKUP_PROPERTIES is enabled.
     * @param applicationName Application name from {@code application.name} (default: "Default Application"). 
     *                        Used in backup archive filename (spaces removed). Example: "OpenKoda" → "OpenKoda_20231215-1430.tar.gz".
     * @param scpHost Remote SCP host from {@code backup.scp.host} (default: empty string). 
     *                When empty, remote copy is disabled. Format: "user@hostname" or "hostname" if SSH config exists.
     * @param scpTargetDirectory Remote directory from {@code backup.scp.target} (default: empty string). 
     *                           Destination path on remote host. Example: "/backups/openkoda".
     * @param scpExecutable Path to scp binary from {@code backup.scp.executable} (default: "scp"). 
     *                      Allows custom scp path if not in system PATH.
     * @param pgDumpExecutable Path to pg_dump binary from {@code backup.pg_dump.executable} (default: "pg_dump"). 
     *                         Must match PostgreSQL server version. Allows custom path if not in system PATH.
     * @param gpgExecutable Path to gpg binary from {@code backup.gpg.executable} (default: "gpg"). 
     *                      Allows custom gpg path if not in system PATH.
     */
    public BackupWriter(
            @Value("${spring.datasource.url}") String datasourceUrl,
            @Value("${spring.datasource.username}") String datasourceUsername,
            @Value("${spring.datasource.password}") String datasourcePassword,
            @Value("${backup.date.pattern:yyyyMMdd-HHmm}") String datePattern,
            @Value("${backup.file.directory:}") String fileDirectory,
            @Value("${backup.gpg.key.name:}") String gpgKeyName,
            @Value("${backup.gpg.key.file:}") String gpgKeyFile,
            @Value("${backup.application.properties:}") String applicationPropertiesFilePath,
            @Value("${application.name:Default Application}") String applicationName,
            @Value("${backup.scp.host:}") String scpHost,
            @Value("${backup.scp.target:}") String scpTargetDirectory,
            @Value("${backup.scp.executable:scp}") String scpExecutable,
            @Value("${backup.pg_dump.executable:pg_dump}") String pgDumpExecutable,
            @Value("${backup.gpg.executable:gpg}") String gpgExecutable
            ) {

        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
        this.datePattern = datePattern;
        this.fileDirectory = fileDirectory;
        this.gpgKeyName = gpgKeyName;
        this.gpgKeyFile = gpgKeyFile;
        this.applicationPropertiesFilePath = applicationPropertiesFilePath;
        this.applicationName = applicationName;
        this.scpHost = scpHost;
        this.scpTargetDirectory = scpTargetDirectory;
        this.gpgExecutable = gpgExecutable;
        this.scpExecutable = scpExecutable;
        this.pgDumpExecutable = pgDumpExecutable;

    }

    /**
     * Initializes transient state after dependency injection.
     * <p>
     * Called automatically by Spring after constructor execution via {@code @PostConstruct} annotation.
     * Creates SimpleDateFormat instance from injected date pattern and detects operating system type.
     * </p>
     * <p>
     * OS detection uses {@code os.name} system property. Windows platforms disable tar and SCP operations
     * since these utilities are not natively available. Backup operations on Windows are limited to database dumps only.
     * </p>
     */
    @PostConstruct
    void init() {
        dateFormat = new SimpleDateFormat(datePattern);
        isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    /**
     * Returns the backup directory File instance created during backup execution.
     * <p>
     * Used for test access and runtime state retrieval. Value is set during {@link #doBackup(Collection)} execution.
     * Returns {@code null} if no backup has been initiated yet.
     * </p>
     *
     * @return backup directory File instance, or {@code null} if not yet initialized
     */
    public File getBackupDir() {
        return backupDir;
    }

    /**
     * Returns the full path to the tar.gz backup archive file.
     * <p>
     * Used for test access and to retrieve archive location after successful backup.
     * Format: "[fileDirectory]/[applicationName]_[timestamp].tar.gz"
     * Example: "/backups/OpenKoda_20231215-1430.tar.gz"
     * Returns {@code null} if tar archive has not been created yet.
     * </p>
     *
     * @return tar backup file path, or {@code null} if not yet created
     */
    public String getTarBackupFile() {
        return tarBackupFile;
    }

    /**
     * Returns the full path to the database dump SQL file.
     * <p>
     * Used for test access and to retrieve database backup location after successful pg_dump execution.
     * Format: "[fileDirectory]/[databaseName]_[timestamp].sql"
     * Example: "/backups/openkoda_20231215-1430.sql"
     * Returns {@code null} if database backup has not been performed yet.
     * </p>
     *
     * @return database backup file path, or {@code null} if not yet created
     */
    public String getDatabaseBackupFile() {
        return databaseBackupFile;
    }

    /**
     * Returns the application properties file path configured for backup.
     * <p>
     * This is the injected value from {@code backup.application.properties} property.
     * The file is included in backup archives when BackupOption.BACKUP_PROPERTIES is enabled.
     * </p>
     *
     * @return application properties file path from configuration
     */
    public String getApplicationPropertiesFilePath() {
        return applicationPropertiesFilePath;
    }

    /**
     * Returns the SCP target file path including remote host and directory.
     * <p>
     * Format: "[scpHost]:[scpTargetDirectory]/[filename]" or "[scpTargetDirectory]/[filename]" if no host.
     * Example: "user@backupserver:/backups/openkoda/OpenKoda_20231215-1430.tar.gz.gpg"
     * Returns {@code null} if SCP copy has not been executed yet.
     * </p>
     *
     * @return SCP target path, or {@code null} if not yet set
     */
    public String getScpTargetFile() {
        return scpTargetFile;
    }

    /**
     * Returns the configured SCP target directory on remote host.
     * <p>
     * Package-private accessor for internal use. This is the injected value from {@code backup.scp.target} property.
     * </p>
     *
     * @return SCP target directory from configuration
     */
    String getScpTargetDirectory() {
        return scpTargetDirectory;
    }

    /**
     * Returns the configured local backup directory path.
     * <p>
     * This is the injected value from {@code backup.file.directory} property.
     * Directory is created during backup execution if it does not exist.
     * </p>
     *
     * @return local backup directory path from configuration
     */
    public String getFileDirectory() {
        return fileDirectory;
    }

    /**
     * Executes complete backup workflow including database dump, archive creation, and encryption.
     * <p>
     * Orchestrates the entire backup process based on provided {@link BackupOption} flags:
     * <ol>
     *   <li>Creates backup directory if it does not exist (via {@link #fileDirectory} configuration)</li>
     *   <li>Performs PostgreSQL database dump using pg_dump if BACKUP_DATABASE option enabled</li>
     *   <li>Creates tar.gz archive containing database dump and/or properties file based on options</li>
     *   <li>Encrypts archive with GPG if {@link #gpgKeyName} is configured (imports key if necessary)</li>
     * </ol>
     * </p>
     * <p>
     * <b>BackupOption Usage:</b>
     * <ul>
     *   <li>{@code BackupOption.BACKUP_DATABASE}: Includes database dump in backup (creates .sql file via pg_dump)</li>
     *   <li>{@code BackupOption.BACKUP_PROPERTIES}: Includes application.properties in backup archive</li>
     * </ul>
     * Both options can be combined. If no paths are available for archiving, method returns {@code false}.
     * </p>
     * <p>
     * <b>Platform Restrictions:</b> Tar and GPG operations are disabled on Windows (returns {@code false}).
     * Database dump may succeed on Windows if pg_dump.exe is available and configured.
     * </p>
     * <p>
     * <b>Error Handling:</b> All subprocess errors are logged to {@link #ERROR_LOGS_FILE_NAME} file.
     * Method returns {@code false} on any IOException or InterruptedException without propagating exceptions.
     * Check error log file for detailed diagnostics on failures.
     * </p>
     * <p>
     * <b>State Side Effects:</b> Sets transient fields during execution:
     * {@link #backupDir}, {@link #backupDateInfo}, {@link #databaseBackupFile}, {@link #tarBackupFile}.
     * </p>
     *
     * @param backupOptions Collection of {@link BackupOption} flags controlling which backup tasks to execute.
     *                       Must not be {@code null}. Empty collection results in no-op (returns {@code false}).
     * @return {@code true} if all enabled backup operations completed successfully, {@code false} on any failure
     *         or if running on Windows platform (tar/GPG not supported)
     * @see BackupOption
     * @see #copyBackupFile(Collection, File)
     */
    public boolean doBackup(Collection<BackupOption> backupOptions) {
        try {
            debug("[doBackup] execution");
            createBackupDirectoryCommand();
            doDatabaseBackup(backupOptions);
            return doTarEncrypt(backupOptions);
        } catch (IOException | InterruptedException e) {
            error(e, "Backup creation error due to {}. See {} for more info.", e.getMessage(), ERROR_LOGS_FILE_NAME);
        }

        return false;
    }

    private void createBackupDirectoryCommand() {
        debug("[createBackupDirectoryCommand]");
        backupDir = new File(fileDirectory);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    }

    private void doDatabaseBackup(Collection<BackupOption> backupOptions) throws InterruptedException, IOException {
        if (backupOptions.contains(BackupOption.BACKUP_DATABASE)) {
            pgDumpCommand().start().waitFor();
        }
    }

    private ProcessBuilder pgDumpCommand() {
        debug("[pgDumpCommand]");
        String databaseName = datasourceUrl.substring(datasourceUrl.lastIndexOf("/") + 1);
        String databaseHost = StringUtils.substringBetween(datasourceUrl, "://", ":");
        backupDateInfo = "_" + dateFormat.format(new Date());
        databaseBackupFile = backupDir.getPath() + File.separator + databaseName + backupDateInfo + ".sql";
        ProcessBuilder builder = new ProcessBuilder(
                pgDumpExecutable,
                "-U", datasourceUsername,
                "-h", databaseHost,
                "-d", databaseName,
                "-f", databaseBackupFile);

        builder.environment().put("PGPASSWORD", datasourcePassword);
        builder.redirectError(ProcessBuilder.Redirect.appendTo(new File(ERROR_LOGS_FILE_NAME)));

        return builder;
    }

    private boolean doTarEncrypt(Collection<BackupOption> backupOptions) throws InterruptedException, IOException {
        debug("[doTarEncrypt]");
        List<String> backupPaths = getPathsToBackup(backupOptions);
        if (isWindows || backupPaths.isEmpty()) {
            return false;
        }

        tarCommand(backupPaths).start().waitFor();
        encryptWithGpg();

        return true;
    }

    private List<String> getPathsToBackup(Collection<BackupOption> backupOptions) {
        List<String> paths = new ArrayList<>();
        if (backupOptions.contains(BackupOption.BACKUP_DATABASE)) {
            paths.add(databaseBackupFile);
        }

        if (backupOptions.contains(BackupOption.BACKUP_PROPERTIES)) {
            paths.add(applicationPropertiesFilePath);
        }

        return paths;
    }

    private ProcessBuilder tarCommand(List<String> backupPaths) {
        debug("[tarCommand]");
        tarBackupFile = backupDir.getPath()
                + File.separator + applicationName.replaceAll("\\s", "")
                + backupDateInfo + ".tar.gz";

        List<String> command = ImmutableList.<String>builder()
                .add("tar", "-czf", tarBackupFile)
                .addAll(backupPaths)
                .build();

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectError(ProcessBuilder.Redirect.appendTo(new File(ERROR_LOGS_FILE_NAME)));

        return builder;
    }

    private void encryptWithGpg() throws InterruptedException, IOException {
        debug("[encryptWithGpg]");
        if (!gpgKeyName.isEmpty()) {
            boolean gpgKeyAvailable = gpgExists();
//            if the gpg key does not exist the system should load one
            if (!gpgKeyAvailable) {
                loadGpgKey().start().waitFor();
//                making sure the gpg key got loaded correctly and it exists
                gpgKeyAvailable = gpgExists();
            }
            if (gpgKeyAvailable) {
                encryptGpgCommand().start().waitFor();
            }
        }
    }

    private boolean gpgExists() {
        debug("[gpgExists]");
        try {
            String output = IOUtils.toString(checkGpgKeyCommand().start().getInputStream());
            return output.contains(gpgKeyName);
        } catch (IOException e) {
            error("Cannot check if GPG key exists", e);
        }

        return false;
    }

    private ProcessBuilder checkGpgKeyCommand() {
        debug("[checkGpgKeyCommand]");
        ProcessBuilder builder = new ProcessBuilder(
                gpgExecutable,
                "-k", gpgKeyName);
        builder.redirectError(ProcessBuilder.Redirect.appendTo(new File(ERROR_LOGS_FILE_NAME)));

        return builder;
    }

    private ProcessBuilder loadGpgKey() {
        debug("[loadGpgKey]");
        ProcessBuilder builder = new ProcessBuilder(
                gpgExecutable,
                "--import", gpgKeyFile);
        builder.redirectError(ProcessBuilder.Redirect.appendTo(new File(ERROR_LOGS_FILE_NAME)));

        return builder;
    }

    private ProcessBuilder encryptGpgCommand() {
        debug("[encryptGpgCommand]");
        ProcessBuilder builder = new ProcessBuilder(
                gpgExecutable, "-e", "--always-trust",
                "-r", gpgKeyName,
                tarBackupFile);
        builder.redirectError(ProcessBuilder.Redirect.appendTo(new File(ERROR_LOGS_FILE_NAME)));

        return builder;
    }

    /**
     * Transfers backup file to remote host via SCP (Secure Copy Protocol).
     * <p>
     * Executes SCP command to copy the specified backup file to the configured remote destination.
     * Operation is conditional on BackupOption.SCP_ENABLED flag and platform compatibility (non-Windows).
     * Requires valid SSH authentication setup (SSH keys or ssh-agent) for passwordless copy.
     * </p>
     * <p>
     * <b>Prerequisites:</b>
     * <ul>
     *   <li>{@link #scpHost} must be configured (non-empty) with format "user@hostname" or "hostname"</li>
     *   <li>{@link #scpTargetDirectory} must be configured with valid remote directory path</li>
     *   <li>SSH authentication must be pre-configured (public key in authorized_keys or ssh-agent)</li>
     *   <li>Remote directory must exist and be writable by authenticated user</li>
     *   <li>scp executable must be available in PATH or configured via {@link #scpExecutable}</li>
     * </ul>
     * </p>
     * <p>
     * <b>BackupOption Usage:</b> Method only executes if {@code backupOptions} contains BackupOption.SCP_ENABLED.
     * If flag is absent, method returns {@code false} without attempting transfer.
     * </p>
     * <p>
     * <b>File Parameter Validation:</b> The {@code fileToCopy} parameter is validated for:
     * <ul>
     *   <li>Non-null reference</li>
     *   <li>File existence on local filesystem</li>
     *   <li>Read permissions for current process</li>
     * </ul>
     * Validation failure returns {@code false} without attempting SCP.
     * </p>
     * <p>
     * <b>Platform Restrictions:</b> SCP operations are disabled on Windows (returns {@code false}).
     * Use alternative transfer methods (SFTP, rsync) on Windows platforms.
     * </p>
     * <p>
     * <b>Error Handling:</b> SCP process stderr is redirected to {@link #ERROR_LOGS_FILE_NAME}.
     * Method returns {@code false} on IOException, InterruptedException, or non-zero SCP exit code.
     * Check error log file for detailed SCP diagnostics (authentication failures, network errors, permission issues).
     * </p>
     * <p>
     * <b>State Side Effects:</b> Sets {@link #scpTargetFile} field with complete remote path during execution.
     * </p>
     *
     * @param backupOptions Collection of {@link BackupOption} flags. Must contain BackupOption.SCP_ENABLED to execute transfer.
     *                       Must not be {@code null}.
     * @param fileToCopy Local backup file to transfer. Typically the encrypted archive file (e.g., "backup.tar.gz.gpg").
     *                    Must not be {@code null}, must exist, and must be readable.
     * @return {@code true} if SCP process completed with exit code 0 (successful transfer),
     *         {@code false} if SCP disabled, file invalid, platform incompatible, or transfer failed
     * @see BackupOption#SCP_ENABLED
     * @see #doBackup(Collection)
     */

    public boolean copyBackupFile(Collection<BackupOption> backupOptions, File fileToCopy) {
        try {
            if (isScpEnabled(backupOptions) && isAccessible(fileToCopy)) {
                debug("[copyBackupFile] execution");
                int result = scpCommand(fileToCopy).start().waitFor();
                return 0 == result;
            }
        } catch (InterruptedException | IOException e) {
            error(e, "SCP execution error due to {}. See {} for more info.", e.getMessage(), ERROR_LOGS_FILE_NAME);
        }

        return false;
    }

    private boolean isScpEnabled(Collection<BackupOption> backupOptions) {
        return !isWindows && backupOptions.contains(BackupOption.SCP_ENABLED);
    }

    private boolean isAccessible(File file) {
        return null != file && file.exists() && file.canRead();
    }


    /**
     * Copy file
     *
     * @param path to file
     */
    private ProcessBuilder scpCommand(File path) {
        debug("[scpCommand]");
        String scpSourceFile = path.getAbsolutePath();

        // copy te remote if host is specified
        scpTargetFile = scpHost.isEmpty() ? "" : scpHost + ":";
        scpTargetFile += scpTargetDirectory + File.separator + path.getName();

        ProcessBuilder builder = new ProcessBuilder(
                scpExecutable,
                scpSourceFile,
                scpTargetFile);

        builder.redirectError(ProcessBuilder.Redirect.appendTo(new File(ERROR_LOGS_FILE_NAME)));

        return builder;
    }

}
