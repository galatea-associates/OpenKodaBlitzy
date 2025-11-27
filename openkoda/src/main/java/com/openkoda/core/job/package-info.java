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

/**
 * Provides scheduled background job execution for OpenKoda system maintenance and asynchronous task processing.
 * <p>
 * This package manages scheduled background jobs for email delivery, webhook delivery, search index maintenance,
 * and system health monitoring. Jobs run automatically on configurable schedules using Spring's @Scheduled mechanism.
 * 
 *
 * <b>Package Architecture</b>
 * <p>
 * The JobsScheduler orchestrates four job implementations using Spring @Scheduled annotations. Each job processes
 * tasks from the database in a stateless manner with atomic task claiming and transactional execution.
 * 
 *
 * <b>Key Components</b>
 * <ul>
 * <li><b>JobsScheduler</b>: Central scheduling orchestrator that uses @Scheduled annotations to trigger job execution
 *     at configured intervals</li>
 * <li><b>EmailSenderJob</b>: Processes Email tasks from the database and sends messages via the EmailSender service.
 *     Runs every 5 seconds using fixedDelay scheduling</li>
 * <li><b>PostMessagesToWebhookJob</b>: Processes HttpRequestTask entities and posts messages to webhook URLs via
 *     RestTemplate. Runs every 5 seconds using fixedDelay scheduling</li>
 * <li><b>SearchIndexUpdaterJob</b>: Updates search_index columns for searchable entities using native SQL queries.
 *     Runs every 10 seconds using fixedDelay scheduling</li>
 * <li><b>SystemHealthAlertJob</b>: Monitors RAM, CPU, and disk usage against configurable thresholds and sends alerts.
 *     Runs daily at 4:00 AM by default using cron scheduling</li>
 * </ul>
 *
 * <b>Scheduling Patterns</b>
 * <p>
 * Jobs use two scheduling patterns:
 * 
 * <ul>
 * <li><b>Fixed Delay</b>: Jobs wait for a specified delay after the previous execution completes before starting again.
 *     Used for EmailSenderJob and PostMessagesToWebhookJob (5 second delay) and SearchIndexUpdaterJob (10 second delay)</li>
 * <li><b>Cron Expression</b>: Jobs run at specific times defined by cron syntax. Used for SystemHealthAlertJob
 *     (default: 0 0 4 * * ? for daily 4:00 AM execution)</li>
 * </ul>
 *
 * <b>Design Patterns</b>
 * <p>
 * <b>Atomic Task Claiming</b>: Jobs use repository-based findTasksAndSetStateDoing() methods to atomically claim tasks
 * and set their state to DOING. This prevents duplicate processing when multiple application instances run concurrently.
 * 
 * <p>
 * <b>Transactional Execution</b>: All job methods are marked @Transactional, ensuring that database changes either
 * commit together or roll back together. If a job throws an exception, the transaction rolls back and tasks remain
 * ready for retry.
 * 
 * <p>
 * <b>Request Tracing</b>: All jobs implement LoggingComponentWithRequestId for request-id-aware logging, enabling
 * trace correlation across distributed operations.
 * 
 *
 * <b>Configuration Properties</b>
 * <p>
 * Spring configuration requirements:
 * 
 * <ul>
 * <li><code>@EnableScheduling</code> must be enabled in application configuration</li>
 * <li><code>scheduled.systemHealth.check</code>: Cron expression for system health checks (default: 0 0 4 * * ?)</li>
 * <li><code>max.disk.percentage</code>: Disk usage threshold percentage (default: 75)</li>
 * <li><code>max.ram.percentage</code>: RAM usage threshold percentage (default: 75)</li>
 * <li><code>max.cpu.percentage</code>: CPU usage threshold percentage (default: 75)</li>
 * </ul>
 *
 * <b>Usage Example</b>
 * <pre>{@code
 * // Jobs execute automatically via Spring scheduler
 * // Manual execution example (for testing):
 * List<Email> tasks = emailRepository.findTasksAndSetStateDoing(10);
 * tasks.forEach(email -> emailSender.sendEmail(email));
 * }</pre>
 *
 * <b>Thread Safety</b>
 * <p>
 * Jobs are stateless Spring beans with no instance variables. The Spring scheduler uses a default single-threaded
 * executor, preventing concurrent execution of the same job. Database-level locking via findTasksAndSetStateDoing()
 * prevents task duplication across multiple application instances.
 * 
 *
 * <b>Error Handling</b>
 * <p>
 * When a job throws an exception, Spring's scheduler propagates the exception causing the transaction to roll back.
 * Failed tasks remain in a ready state and are retried on the next scheduled execution. This ensures no task is lost
 * due to transient failures.
 * 
 *
 * <b>Related Packages</b>
 * <ul>
 * <li>{@link com.openkoda.core.service.email} - EmailSender service for email delivery</li>
 * <li>{@link com.openkoda.repository.task} - Task repository interfaces for Email and HttpRequestTask entities</li>
 * <li>{@link com.openkoda.core.tracker} - Request tracing and logging infrastructure</li>
 * <li>{@link com.openkoda.model.task} - Task entity definitions including Email and HttpRequestTask</li>
 * </ul>
 *
 * @since 1.7.1
 * @version 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.core.job;