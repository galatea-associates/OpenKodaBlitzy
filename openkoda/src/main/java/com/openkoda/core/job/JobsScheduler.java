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

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.inject.Inject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Central orchestrator for all scheduled background jobs in the OpenKoda platform.
 * <p>
 * This Spring component wires together four job implementations and provides scheduling
 * orchestration using Spring's {@code @Scheduled} annotation. The scheduler uses two
 * timing strategies: fixed-delay intervals for continuous jobs (email, webhook, search)
 * and cron expressions for periodic jobs (system health checks).
 * 
 * <p>
 * All scheduled methods are stateless delegates that pass control to injected job beans.
 * Actual concurrency control, transaction management, and error handling are implemented
 * within the individual job classes. This separation ensures the scheduler remains simple
 * and focused solely on timing orchestration.
 * 
 * <p>
 * This class implements {@link LoggingComponentWithRequestId} to enable request-id-aware
 * tracing for all scheduled job executions, facilitating debugging and audit trails.
 * 
 *
 * @since 1.7.1
 * @version 1.7.1
 * @author OpenKoda Team
 * @see EmailSenderJob
 * @see PostMessagesToWebhookJob
 * @see SearchIndexUpdaterJob
 * @see SystemHealthAlertJob
 */
@Component
public class JobsScheduler implements LoggingComponentWithRequestId {

    /**
     * Injected bean responsible for processing and delivering queued email messages.
     * Invoked by the {@link #emailSenderJob()} scheduled method.
     */
    @Inject EmailSenderJob emailSenderJob;
    
    /**
     * Injected bean responsible for posting queued messages to configured webhook endpoints.
     * Invoked by the {@link #postMessagesToWebhookJob()} scheduled method.
     */
    @Inject PostMessagesToWebhookJob postMessagesToWebhookJob;
    
    /**
     * Injected bean responsible for updating search indexes with latest database changes.
     * Invoked by the {@link #searchIndexUpdaterJob()} scheduled method.
     */
    @Inject SearchIndexUpdaterJob searchIndexUpdaterJob;
    
    /**
     * Injected bean responsible for monitoring system health metrics (RAM, CPU, disk usage).
     * Invoked by the {@link #systemHealthAlertJob()} scheduled method.
     */
    @Inject SystemHealthAlertJob systemHealthAlertJob;

    /**
     * Scheduled trigger for email delivery processing.
     * <p>
     * Runs every 5 seconds after the previous execution completes, with an initial 10-second
     * delay after application startup. The fixed-delay scheduling ensures no overlapping
     * executions occur, preventing concurrent email delivery attempts.
     * 
     * <p>
     * This method delegates all business logic to {@link EmailSenderJob#send()}, which handles
     * transactional email queue processing, SMTP delivery, and error recovery. Any exceptions
     * thrown by the delegate propagate to Spring's scheduler, which logs the error and continues
     * scheduling subsequent executions.
     * 
     */
    @Scheduled(initialDelay = 10000, fixedDelay = 5000)
    public void emailSenderJob() {
        emailSenderJob.send();
    }

    /**
     * Scheduled trigger for webhook message delivery processing.
     * <p>
     * Runs every 5 seconds after the previous execution completes, with an initial 10-second
     * delay after application startup. The timing parameters match {@link #emailSenderJob()}
     * to provide consistent message delivery frequency across channels.
     * 
     * <p>
     * This method delegates to {@link PostMessagesToWebhookJob#send()}, which handles
     * transactional HTTP POST operations to configured webhook endpoints. The fixed-delay
     * scheduling prevents concurrent webhook deliveries and ensures ordered processing.
     * 
     */
    @Scheduled(initialDelay = 10000, fixedDelay = 5000)
    public void postMessagesToWebhookJob() {
        postMessagesToWebhookJob.send();
    }

    /**
     * Scheduled trigger for search index synchronization.
     * <p>
     * Runs every 10 seconds after the previous execution completes, with an initial 10-second
     * delay after application startup. The slower interval (compared to email/webhook jobs)
     * reflects the less time-sensitive nature of search index updates.
     * 
     * <p>
     * This method delegates to {@link SearchIndexUpdaterJob#updateSearchIndexes()}, which
     * executes native SQL updates to synchronize search indexes with database changes. The
     * entire index update runs within a single transaction, ensuring consistency.
     * 
     */
    @Scheduled(initialDelay = 10000, fixedDelay = 10000)
    public void searchIndexUpdaterJob() {
        searchIndexUpdaterJob.updateSearchIndexes();
    }

    /**
     * Scheduled trigger for system health monitoring and alerting.
     * <p>
     * Runs daily at 4:00 AM by default, using the cron expression "0 0 4 * * ?" (second=0,
     * minute=0, hour=4, any day/month/weekday). The schedule can be overridden via the
     * {@code scheduled.systemHealth.check} property in application configuration.
     * 
     * <p>
     * This method uses cron-based scheduling instead of fixed-delay because system health
     * checks are resource-intensive and only need periodic execution rather than continuous
     * monitoring. Delegates to {@link SystemHealthAlertJob#checkSystem()}, which monitors
     * RAM usage, CPU load, and disk space, generating alerts when thresholds are exceeded.
     * 
     */
    @Scheduled(cron = "${scheduled.systemHealth.check:0 0 4 * * ?}")
    public void systemHealthAlertJob() {
        systemHealthAlertJob.checkSystem();
    }

}
