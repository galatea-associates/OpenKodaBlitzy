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

package com.openkoda.dto.system;

import com.openkoda.dto.CanonicalObject;
import com.openkoda.dto.OrganizationRelatedObject;

/**
 * Data transfer object representing recurring job metadata with cron scheduling configuration.
 * <p>
 * This DTO encapsulates the essential parameters for scheduled job execution including
 * cron expression, event data payload, and multi-tenant configuration. It implements
 * {@link CanonicalObject} for notification message formatting and
 * {@link OrganizationRelatedObject} for multi-tenant job isolation.
 * </p>
 * <p>
 * This class follows a mutable design pattern without validation, equals/hashCode overrides,
 * or thread-safety guarantees. Validation of cron expression syntax and event data payload
 * format is performed by scheduler frameworks (Spring Scheduling, Quartz) and event handlers,
 * not by this DTO.
 * </p>
 * <p>
 * Usage example:
 * <pre>{@code
 * SchedulerDto job = new SchedulerDto("0 0 * * * *", "{\"action\":\"sync\"}", tenantId, true);
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CanonicalObject
 * @see OrganizationRelatedObject
 */
public class SchedulerDto implements CanonicalObject, OrganizationRelatedObject {

    /**
     * Default no-argument constructor for framework instantiation.
     * <p>
     * Required by Jackson for JSON deserialization, JPA for entity mapping,
     * and other serialization/persistence frameworks. Initializes all fields
     * to their default values (null for objects, false for booleans).
     * </p>
     */
    public SchedulerDto() {}

    /**
     * Constructs a SchedulerDto with all scheduling parameters.
     * <p>
     * Initializes a complete scheduler configuration including cron scheduling,
     * event payload, multi-tenant isolation, cluster deployment settings, and
     * execution mode. No validation is performed on the parameters.
     * </p>
     *
     * @param cronExpression cron expression defining job schedule (e.g., "0 0 * * * *" for hourly execution)
     * @param eventData JSON or string payload passed to scheduled event handler
     * @param organizationId tenant identifier for multi-tenant job isolation (null for system-wide jobs)
     * @param onMasterOnly whether job should only execute on master node in clustered deployments
     * @param async whether job execution is asynchronous (true for non-blocking execution)
     */
    public SchedulerDto(String cronExpression, String eventData, Long organizationId, boolean onMasterOnly, boolean async) {
        this.cronExpression = cronExpression;
        this.eventData = eventData;
        this.organizationId = organizationId;
        this.onMasterOnly = onMasterOnly;
        this.async = async;
    }

    /**
     * Constructs a SchedulerDto with default synchronous execution.
     * <p>
     * Convenience constructor that delegates to the full constructor with
     * async parameter defaulting to false (synchronous, blocking execution).
     * </p>
     *
     * @param cronExpression cron expression defining job schedule
     * @param eventData JSON or string payload for scheduled event
     * @param organizationId tenant identifier (null for system-wide jobs)
     * @param onMasterOnly whether to run only on master node
     */
    public SchedulerDto(String cronExpression, String eventData, Long organizationId, boolean onMasterOnly) {
        this(cronExpression, eventData, organizationId, onMasterOnly, false);
    }

    /**
     * Cron expression defining the recurring schedule for job execution.
     * <p>
     * Standard cron format (e.g., "0 0 * * * *" for hourly, "0 0 0 * * *" for daily).
     * No validation is performed by this DTO; validation is delegated to scheduler
     * frameworks such as Spring Scheduling or Quartz.
     * </p>
     */
    public String cronExpression;

    /**
     * JSON or string payload passed to the scheduled event handler.
     * <p>
     * Typically contains JSON configuration for the job execution. No validation
     * is performed by this DTO; validation is delegated to event handlers.
     * </p>
     */
    public String eventData;

    /**
     * Tenant identifier for multi-tenant job isolation and authorization.
     * <p>
     * When null, indicates a system-wide job not scoped to any specific tenant.
     * When non-null, restricts job execution and data access to the specified organization.
     * </p>
     */
    public Long organizationId;

    /**
     * Whether job should only execute on the master node in clustered deployments.
     * <p>
     * When true, prevents duplicate job execution across multiple application instances
     * in a cluster. When false, job may execute on any node.
     * </p>
     */
    public boolean onMasterOnly;

    /**
     * Whether job execution is asynchronous (non-blocking).
     * <p>
     * When true, job runs asynchronously without blocking the scheduler thread.
     * When false, job runs synchronously and may block subsequent job execution.
     * </p>
     */
    private boolean async;

    /**
     * Returns the cron expression defining the job schedule.
     *
     * @return cron expression string, may be null
     */
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * Sets the cron expression defining the job schedule.
     * <p>
     * No validation is performed; cron syntax validation is delegated
     * to scheduler frameworks.
     * </p>
     *
     * @param cronExpression cron expression (no validation performed)
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * Returns the event payload passed to the scheduled event handler.
     *
     * @return JSON or string event payload, may be null
     */
    public String getEventData() {
        return eventData;
    }

    /**
     * Sets the event payload for the scheduled job.
     * <p>
     * Typically a JSON string containing job configuration. No validation
     * is performed by this DTO.
     * </p>
     *
     * @param eventData event payload string (typically JSON)
     */
    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    /**
     * Returns the tenant identifier for multi-tenant job isolation.
     *
     * @return organization ID for multi-tenant isolation, null for system-wide jobs
     */
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the tenant identifier for multi-tenant job scoping.
     * <p>
     * Use null to indicate a system-wide job not scoped to any specific tenant.
     * </p>
     *
     * @param organizationId tenant ID (null for system-wide)
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Returns whether the job should only execute on the master node in clustered deployments.
     *
     * @return true if job should only run on master node in clustered deployment
     */
    public boolean isOnMasterOnly() {
        return onMasterOnly;
    }

    /**
     * Sets whether the job should only execute on the master node.
     * <p>
     * In clustered deployments, setting this to true prevents duplicate job
     * execution across multiple application instances.
     * </p>
     *
     * @param onMasterOnly whether to restrict execution to master node
     */
    public void setOnMasterOnly(boolean onMasterOnly) {
        this.onMasterOnly = onMasterOnly;
    }

    /**
     * Returns whether job execution is asynchronous (non-blocking).
     *
     * @return true if job execution is asynchronous (non-blocking)
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * Sets whether job execution should be asynchronous.
     * <p>
     * When true, jobs run asynchronously without blocking the scheduler thread.
     * When false, jobs run synchronously and may block subsequent job execution.
     * </p>
     *
     * @param async whether execution should be asynchronous
     */
    public void setAsync(boolean async) {
        this.async = async;
    }

    /**
     * Returns a formatted notification message for audit trails and logging.
     * <p>
     * Generates a message in the format: "Scheduler: {cronExpression}. With payload: {eventData}."
     * This message is used for logging, notifications, and audit trails to provide
     * human-readable job identification.
     * </p>
     *
     * @return formatted notification message with cronExpression and eventData
     * @throws NullPointerException if cronExpression or eventData is null during String.format()
     */
    @Override
    public String notificationMessage() {
        return String.format("Scheduler: %s. With payload: %s.", cronExpression, eventData);
    }

}