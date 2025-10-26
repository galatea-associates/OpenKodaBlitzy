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

package com.openkoda.core.service;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.service.backup.BackupOption;
import com.openkoda.core.service.backup.BackupWriter;
import com.openkoda.dto.system.ScheduledSchedulerDto;
import jakarta.inject.Inject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

import static com.openkoda.core.service.event.ApplicationEvent.BACKUP_CREATED;
import static com.openkoda.core.service.event.ApplicationEvent.BACKUP_FILE_COPIED;

/**
 * Service performing backup logic and providing consumer for that.
 * <p>
 * Backup is done by jenkins user performing unix commands in the directory where currently running application jar is
 * placed.
 * </p>
 * <p>
 * This service orchestrates scheduled backup signals via {@link #doFullBackup(ScheduledSchedulerDto)} which checks
 * {@code ScheduledSchedulerDto.eventData} for the "backup" keyword. When detected, it delegates archive creation to
 * {@link BackupWriter}, and emits {@link com.openkoda.core.service.event.ApplicationEvent#BACKUP_CREATED} and
 * {@link com.openkoda.core.service.event.ApplicationEvent#BACKUP_FILE_COPIED} events on success.
 * </p>
 * <p>
 * Full backup performs:
 * <ul>
 *   <li>pg_dump on the database</li>
 *   <li>tar -czf on database backup file and application.properties</li>
 *   <li>checking if gpg key is available</li>
 *   <li>if gpg key is not available then importing it from the filesystem</li>
 *   <li>encryption of tar archive with gpg</li>
 *   <li>emission of BACKUP_CREATED event</li>
 * </ul>
 * </p>
 * <p>
 * Configuration is controlled via the {@code backup.options} property which accepts comma-separated
 * {@link BackupOption} enum values (defaults to BACKUP_DATABASE,BACKUP_PROPERTIES).
 * </p>
 * <p>
 * This service extends {@link ComponentProvider} for access to services and repositories aggregators.
 * </p>
 * <p>
 * <b>Note:</b> Platform command execution requires jenkins user or equivalent with filesystem and database access.
 * </p>
 * <p>
 * Usage example: Typically invoked by SchedulerService on cron trigger with eventData="backup"
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @since 2019-03-26
 * @version 1.7.1
 * @see BackupWriter
 * @see BackupOption
 * @see ScheduledSchedulerDto
 * @see com.openkoda.core.service.event.ApplicationEvent
 * @see ComponentProvider
 */
@Service
public class BackupService extends ComponentProvider {

    /**
     * Injected delegate performing platform commands (pg_dump, tar, gpg, scp) for backup archive creation.
     */
    @Inject
    private BackupWriter backupWriter;

    /**
     * Backup scope configuration parsed from {@code backup.options} property.
     * Comma-separated {@link BackupOption} enum values configure which components to back up.
     * Defaults to BACKUP_DATABASE,BACKUP_PROPERTIES if not specified.
     */
    @Value("${backup.options:BACKUP_DATABASE,BACKUP_PROPERTIES}")
    private List<BackupOption> backupOptions;

    /**
     * String literal "backup" used for event parameter matching in {@link #isBackupEvent(ScheduledSchedulerDto)}.
     */
    public static final String BACKUP = "backup";

    /**
     * Returns the backup working directory from BackupWriter.
     *
     * @return backup working directory where backup operations are performed
     */
    public File getBackupDir() {
        return backupWriter.getBackupDir();
    }

    /**
     * Consumer for full backups.
     * <p>
     * Scheduled event consumer that checks {@code eventParameter.eventData} for the {@link #BACKUP} keyword
     * (case-insensitive). When matched, delegates to {@link #doBackup()} for backup execution.
     * </p>
     * <p>
     * Returns {@code false} if not a backup event or if backup fails.
     * Returns {@code true} on successful backup and BACKUP_CREATED event emission.
     * </p>
     *
     * @param eventParameter scheduler event DTO containing eventData field
     * @return {@code true} if backup event and backup succeeds, {@code false} otherwise
     */
    public boolean doFullBackup(ScheduledSchedulerDto eventParameter) {
        return isBackupEvent(eventParameter) && doBackup();
    }

    /**
     * Private guard checking if event parameter contains "backup" in eventData field.
     * <p>
     * Performs lowercase comparison to handle case-insensitive matching. Handles null parameter safely.
     * </p>
     *
     * @param eventParameter scheduler event DTO to check, may be null
     * @return {@code true} if eventData contains "backup" (case-insensitive), {@code false} otherwise
     */
    private boolean isBackupEvent(ScheduledSchedulerDto eventParameter) {
        return eventParameter != null && eventParameter.eventData.toLowerCase().equals(BACKUP);
    }

    /**
     * Private orchestration method for backup execution.
     * <p>
     * Invokes {@link BackupWriter#doBackup(List)} with configured {@link #backupOptions}.
     * On success, emits BACKUP_CREATED event with {@link File} payload via {@link #emitBackupCreated()}.
     * </p>
     *
     * @return {@code true} if backup succeeds, {@code false} on failure
     */
    private boolean doBackup() {
        info("[doBackup]");
        if (backupWriter.doBackup(backupOptions)) {
            emitBackupCreated();
            return true;
        }

        return false;
    }

    /**
     * Private event emitter publishing BACKUP_CREATED event.
     * <p>
     * Publishes BACKUP_CREATED event with tar archive {@link File} payload to ApplicationEventService
     * for downstream processing (e.g., remote copy, notification).
     * </p>
     */
    private void emitBackupCreated() {
        debug("[emitBackupCreated]");
        String tarBackupFile = backupWriter.getTarBackupFile();
        services.applicationEvent.emitEvent(BACKUP_CREATED, new File(tarBackupFile));
    }

    /**
     * Consumer for copying backup archive to remote host.
     * <p>
     * Event consumer for remote backup copy via SCP. Delegates to {@link BackupWriter#copyBackupFile(List, File)}
     * for secure copy execution. On success, emits BACKUP_FILE_COPIED event with target path String
     * via {@link #emitBackupFileCopied()}.
     * </p>
     *
     * @param file backup archive File to copy to remote host
     * @return {@code true} if remote copy succeeds, {@code false} on failure
     */
    public boolean copyBackupFile(File file) {
        debug("[copyBackupFile]");
        if (backupWriter.copyBackupFile(backupOptions, file)) {
            emitBackupFileCopied();
            return true;
        }

        return false;
    }

    /**
     * Private event emitter publishing BACKUP_FILE_COPIED event.
     * <p>
     * Publishes BACKUP_FILE_COPIED event with SCP target path String payload to ApplicationEventService
     * for tracking and notification of successful remote backup transfer.
     * </p>
     */
    private void emitBackupFileCopied() {
        debug("[emitBackupFileCopied]");
        String scpTargetFile = backupWriter.getScpTargetFile();
        services.applicationEvent.emitEvent(BACKUP_FILE_COPIED, scpTargetFile);
    }

}
