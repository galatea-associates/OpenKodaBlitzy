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

package com.openkoda.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openkoda.model.Organization;
import com.openkoda.model.PrivilegeNames;
import com.openkoda.model.common.ComponentEntity;
import com.openkoda.model.common.ModelConstants;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Formula;

/**
 * JPA entity for persisting scheduled job definitions that are loaded on application startup
 * and registered with the job scheduler.
 * <p>
 * This entity represents a scheduled task configuration that defines when and how periodic events
 * should be emitted within the OpenKoda event-driven architecture. Each scheduler instance specifies
 * a cron expression for timing, event data for emission, and execution parameters such as master-only
 * or asynchronous processing.

 * <p>
 * The scheduler entity inherits organization-scoped multi-tenancy support from {@link ComponentEntity},
 * ensuring that scheduled jobs are properly isolated within their organizational context. Persistence
 * occurs via standard JPA operations, followed by automatic loading during application startup and
 * registration with the job scheduler for periodic event emission.

 * <p>
 * Example usage:
 * <pre>{@code
 * Scheduler job = new Scheduler("0 0 * * * ?", "eventClassName,eventName,eventType", true);
 * schedulerRepository.save(job);
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ComponentEntity
 * @see Organization
 */
@Entity
@Table(name = "scheduler", uniqueConstraints = @UniqueConstraint(columnNames = {"cron_expression", "event_data"}))
public class Scheduler extends ComponentEntity {

    /**
     * Formula used to compute the reference string for this scheduler entity.
     * Uses the default organization-related reference field formula inherited from {@link ComponentEntity}.
     */
    public static final String REFERENCE_FORMULA = DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA;

    /**
     * Primary key identifier for this scheduler entity.
     * Generated using the ORGANIZATION_RELATED_ID_GENERATOR sequence with an allocation size of 10
     * for optimized batch processing.
     */
    @Id
    @SequenceGenerator(name = ORGANIZATION_RELATED_ID_GENERATOR, sequenceName = ORGANIZATION_RELATED_ID_GENERATOR, initialValue = ModelConstants.INITIAL_ORGANIZATION_RELATED_VALUE, allocationSize = 10)
    @GeneratedValue(generator = ModelConstants.ORGANIZATION_RELATED_ID_GENERATOR, strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Computed searchable index field for full-text search capabilities.
     * This field has a database default value of empty string and is maintained by database triggers.
     */
    @Column(name = INDEX_STRING_COLUMN, length = INDEX_STRING_COLUMN_LENGTH, insertable = false)
    @ColumnDefault("''")
    private String indexString;

    /**
     * Cron expression defining the schedule for this job.
     * Uses standard cron syntax (e.g., "0 0 * * * ?" for hourly execution).
     * Stored in the 'cron_expression' database column and is required (non-null).
     */
    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    /**
     * Event descriptor for scheduled event emission.
     * Contains the event information that will be emitted when the scheduled job executes.
     * Stored in the 'event_data' database column and is required (non-null).
     */
    @Column(name = "event_data", nullable = false)
    private String eventData;

    /**
     * Computed reference string for display and search purposes.
     * Generated via database formula using {@link #REFERENCE_FORMULA} for consistent
     * identification across the application.
     */
    @Formula(REFERENCE_FORMULA)
    private String referenceString;

    /**
     * Indicates whether this scheduler should run only on the master node in clustered deployments.
     * When true, the job executes exclusively on the master node to prevent duplicate executions.
     * When false, the job may run on any node in the cluster.
     */
    @Column(name = "on_master_only", nullable = false)
    private boolean onMasterOnly;

    /**
     * Indicates whether event processing should occur asynchronously.
     * When true, the emitted event is processed asynchronously in a separate thread.
     * When false, the event is processed synchronously within the scheduler thread.
     */
    @Column(name = "async", nullable = false)
    private boolean async;
    
    /**
     * Computed privilege required to read this scheduler entity.
     * Generated via database formula requiring {@link PrivilegeNames#_canReadBackend} privilege
     * for accessing scheduler information.
     */
    @Formula("( '" + PrivilegeNames._canReadBackend + "' )")
    private String requiredReadPrivilege;

    /**
     * Computed privilege required to modify this scheduler entity.
     * Generated via database formula requiring {@link PrivilegeNames#_canManageBackend} privilege
     * for creating, updating, or deleting scheduler configurations.
     */
    @Formula("( '" + PrivilegeNames._canManageBackend + "' )")
    private String requiredWritePrivilege;

    /**
     * Returns the computed reference string for this scheduler entity.
     * Overrides {@link ComponentEntity#getReferenceString()} to provide the formula-computed value.
     *
     * @return the reference string computed via database formula
     */
    @Override
    public String getReferenceString() {
        return referenceString;
    }

    /**
     * Default no-argument constructor required by JPA.
     * Creates an empty scheduler instance that should be populated before persistence.
     */
    public Scheduler() {
    }

    /**
     * Creates a new scheduler with the specified configuration.
     *
     * @param cronExpression the cron expression defining the schedule (e.g., "0 0 * * * ?")
     * @param eventData the event descriptor for scheduled emission
     * @param onMasterOnly true if the job should run only on the master node, false otherwise
     */
    public Scheduler(String cronExpression, String eventData, boolean onMasterOnly) {
        this.cronExpression = cronExpression;
        this.eventData = eventData;
        this.onMasterOnly = onMasterOnly;
    }

    /**
     * Returns the unique identifier for this scheduler entity.
     *
     * @return the scheduler ID, or null if not yet persisted
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this scheduler entity.
     *
     * @param id the scheduler ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Sets the searchable index string for this scheduler entity.
     *
     * @param indexString the index string to set for search capabilities
     */
    public void setIndexString(String indexString) {
        this.indexString = indexString;
    }

    /**
     * Returns the cron expression defining this scheduler's execution schedule.
     *
     * @return the cron expression string (e.g., "0 0 * * * ?")
     */
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * Sets the cron expression defining this scheduler's execution schedule.
     *
     * @param cronExpression the cron expression to set (e.g., "0 0 * * * ?")
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * Returns the event descriptor for scheduled emission.
     *
     * @return the event data containing the event information to emit
     */
    public String getEventData() {
        return eventData;
    }

    /**
     * Sets the event descriptor for scheduled emission.
     *
     * @param eventData the event data containing the event information to emit
     */
    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    /**
     * Returns the audit trail representation of this scheduler entity.
     * Used by the auditing subsystem to record changes to scheduler configurations.
     *
     * @return a string containing the scheduler ID for audit logging
     */
    @Override
    public String toAuditString() {
        return "ID: " + id;
    }

    /**
     * Returns the searchable index string for this scheduler entity.
     * Used for full-text search capabilities within the application.
     *
     * @return the index string for search functionality
     */
    @Override
    public String getIndexString() {
        return indexString;
    }

    /**
     * Checks whether this scheduler should run only on the master node.
     *
     * @return true if the job executes only on master node, false if it can run on any node
     */
    public boolean isOnMasterOnly() {
        return onMasterOnly;
    }

    /**
     * Sets whether this scheduler should run only on the master node.
     *
     * @param onMasterOnly true to restrict execution to master node, false to allow on any node
     */
    public void setOnMasterOnly(boolean onMasterOnly) {
        this.onMasterOnly = onMasterOnly;
    }

    /**
     * Returns the privilege required to read this scheduler entity.
     * Overrides {@link ComponentEntity#getRequiredReadPrivilege()} to provide the computed privilege value.
     *
     * @return the privilege name required for read access (canReadBackend)
     */
    @Override
    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    /**
     * Returns the privilege required to modify this scheduler entity.
     * Overrides {@link ComponentEntity#getRequiredWritePrivilege()} to provide the computed privilege value.
     *
     * @return the privilege name required for write access (canManageBackend)
     */
    @Override
    public String getRequiredWritePrivilege() {
        return requiredWritePrivilege;
    }

    /**
     * Checks whether event processing should occur asynchronously.
     *
     * @return true if events are processed asynchronously, false for synchronous processing
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * Sets whether event processing should occur asynchronously.
     *
     * @param async true for asynchronous event processing, false for synchronous processing
     */
    public void setAsync(boolean async) {
        this.async = async;
    }
}
