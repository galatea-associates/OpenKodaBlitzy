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

import java.time.LocalDateTime;

/**
 * Data transfer object that extends SchedulerDto to add scheduled execution time tracking for recurring jobs.
 * <p>
 * This DTO inherits CanonicalObject and OrganizationRelatedObject capabilities from its parent SchedulerDto,
 * providing multi-tenant job scheduling with execution timestamp tracking. The scheduledAt field captures when
 * a scheduled job was or will be executed, enabling execution tracking and audit trails.

 * <p>
 * This class follows a mutable design pattern without validation, equals/hashCode implementations, or thread-safety
 * guarantees. It is used by job schedulers, execution tracking systems, and audit systems to record and monitor
 * recurring job execution times. In distributed systems, careful handling of the timezone-naive LocalDateTime is required.

 * <p>
 * Example usage:
 * <pre>
 * ScheduledSchedulerDto dto = new ScheduledSchedulerDto("0 0 * * *", "{}", orgId, true, LocalDateTime.now());
 * </pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SchedulerDto
 */
public class ScheduledSchedulerDto extends SchedulerDto {

    /**
     * Default constructor for framework instantiation (Jackson, JPA, etc.).
     * <p>
     * Creates an empty ScheduledSchedulerDto with all fields set to their default values (null for objects).
     * Used by serialization frameworks and ORM systems for object instantiation.

     */
    public ScheduledSchedulerDto() {}

    /**
     * Constructor with all parameters including async flag.
     * <p>
     * Creates a fully initialized ScheduledSchedulerDto with scheduled execution time tracking.
     * Delegates to parent SchedulerDto constructor for base field initialization.

     *
     * @param cronExpression Cron expression defining recurring schedule (e.g., "0 0 * * *" for daily at midnight)
     * @param eventData JSON or string payload for scheduled event, passed to job handler during execution
     * @param organizationId Tenant identifier for multi-tenant job isolation, ensures job runs in correct tenant context
     * @param onMasterOnly Whether job should only run on master node in clustered deployment to prevent duplicate execution
     * @param async Whether job execution is asynchronous, affecting execution threading and blocking behavior
     * @param scheduledAt Timestamp for scheduled execution (LocalDateTime, timezone-naive)
     */
    public ScheduledSchedulerDto(String cronExpression, String eventData, Long organizationId, boolean onMasterOnly, boolean async, LocalDateTime scheduledAt) {
        super(cronExpression, eventData, organizationId, onMasterOnly, async);
        this.scheduledAt = scheduledAt;
    }
    
    /**
     * Convenience constructor defaulting async to false.
     * <p>
     * Creates a ScheduledSchedulerDto with synchronous execution behavior by default.
     * Delegates to full constructor with async=false.

     *
     * @param cronExpression Cron expression defining recurring schedule (e.g., "0 0 * * *" for daily at midnight)
     * @param eventData JSON or string payload for scheduled event, passed to job handler during execution
     * @param organizationId Tenant identifier for multi-tenant job isolation, ensures job runs in correct tenant context
     * @param onMasterOnly Whether job should only run on master node in clustered deployment to prevent duplicate execution
     * @param scheduledAt Timestamp for scheduled execution (LocalDateTime, timezone-naive)
     */
    public ScheduledSchedulerDto(String cronExpression, String eventData, Long organizationId, boolean onMasterOnly,
            LocalDateTime scheduledAt) {
        this(cronExpression, eventData, organizationId, onMasterOnly, false, scheduledAt);
    }

    /**
     * Timestamp when the scheduled job was or will be executed.
     * <p>
     * Uses LocalDateTime semantics which is timezone-naive, requiring careful handling in distributed systems
     * across multiple timezones. Consider using Instant or ZonedDateTime for timezone-aware scheduling in
     * future versions.

     */
    public LocalDateTime scheduledAt;

    /**
     * Gets the scheduled execution timestamp.
     *
     * @return LocalDateTime timestamp of scheduled execution, may be null if not yet set
     */
    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    /**
     * Sets the scheduled execution timestamp.
     *
     * @param scheduledAt Timestamp for scheduled execution (LocalDateTime, timezone-naive)
     */
    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    /**
     * Overrides parent's format to include scheduledAt timestamp for audit trails.
     * <p>
     * Generates a formatted notification message including inherited cronExpression and eventData from
     * parent SchedulerDto, plus the scheduledAt timestamp. Used by audit systems and notification handlers
     * to track scheduled job execution.

     *
     * @return Formatted "Scheduler: %s. With payload: %s. Executed at: %s" message for audit trails
     * @throws NullPointerException if cronExpression, eventData, or scheduledAt is null
     */
    @Override
    public String notificationMessage() {
        return String.format("Scheduler: %s. With payload: %s. Executed at: %s", cronExpression, eventData, scheduledAt);
    }
}