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
 * Background task system entities for asynchronous job execution in OpenKoda.
 * <p>
 * This package provides JPA entity models for scheduling, tracking, and executing
 * background tasks asynchronously. Tasks are persisted to the database and processed
 * by background workers, enabling reliable execution of long-running operations
 * without blocking HTTP request threads.
 * 
 *
 * <b>Task Architecture</b>
 * <p>
 * The task system uses JPA single-table inheritance with {@link com.openkoda.model.task.Task}
 * as the abstract base entity. All task types share the 'tasks' table and are differentiated
 * by a 'type' discriminator column. This design provides:
 * 
 * <ul>
 *   <li>Unified task scheduling and lifecycle management</li>
 *   <li>Consistent retry logic across all task types</li>
 *   <li>Efficient querying via single-table storage</li>
 *   <li>Extensibility through polymorphic task subclasses</li>
 * </ul>
 *
 * <b>Key Task Entities</b>
 * <ul>
 *   <li>{@link com.openkoda.model.task.Task} - Abstract base entity defining common task lifecycle,
 *       retry logic, organization scoping, and state management (NEW/DOING/DONE/FAILED/FAILED_PERMANENTLY)</li>
 *   <li>{@link com.openkoda.model.task.Email} - Email sending task with recipient metadata, HTML content,
 *       file attachments, and SMTP/Mailgun integration. Executed by {@link com.openkoda.core.job.EmailSenderJob}</li>
 *   <li>{@link com.openkoda.model.task.HttpRequestTask} - HTTP request execution task for webhooks and
 *       external API calls with JSON payloads, custom headers, and response capture</li>
 * </ul>
 *
 * <b>Task Lifecycle</b>
 * <p>
 * Tasks progress through the following states:
 * 
 * <ol>
 *   <li><strong>NEW</strong> - Task created and ready for execution when startAfter time is reached</li>
 *   <li><strong>DOING</strong> - Task currently executing by background worker</li>
 *   <li><strong>DONE</strong> - Task completed successfully, no further action needed</li>
 *   <li><strong>FAILED</strong> - Task execution failed but can retry (attempts &lt; maxAttempts)</li>
 *   <li><strong>FAILED_PERMANENTLY</strong> - Task exhausted retry attempts, manual intervention required</li>
 * </ol>
 *
 * <b>Task Scheduling</b>
 * <p>
 * Tasks are scheduled using the {@code startAfter} field (LocalDateTime). Background workers
 * query for startable tasks using the computed {@code canBeStarted} field, which evaluates:
 * {@code current_timestamp > start_after AND (state = 'NEW' OR state = 'FAILED')}
 * 
 * <p>
 * This DB-side computation enables efficient task selection without loading all tasks into memory.
 * 
 *
 * <b>Retry Strategy</b>
 * <p>
 * Failed tasks are automatically retried up to {@code MAX_ATTEMPTS_DEFAULT} (5 attempts).
 * The retry workflow:
 * 
 * <ol>
 *   <li>Task execution fails, {@link com.openkoda.model.task.Task#fail()} called</li>
 *   <li>If attempts &lt; maxAttempts: state → FAILED, task remains eligible for retry</li>
 *   <li>If attempts &gt;= maxAttempts: state → FAILED_PERMANENTLY, no further retries</li>
 *   <li>Background worker re-selects FAILED tasks via canBeStarted formula</li>
 * </ol>
 * <p>
 * Exponential backoff can be implemented by updating {@code startAfter} in {@code fail()} method.
 * 
 *
 * <b>Multi-Tenancy Support</b>
 * <p>
 * All task entities implement {@link com.openkoda.model.common.AuditableEntityOrganizationRelated},
 * providing organization-scoped task isolation. Each task has:
 * 
 * <ul>
 *   <li>{@code organizationId} - Scalar column for write operations and indexing</li>
 *   <li>{@code organization} - ManyToOne association for lazy-loading Organization entity</li>
 * </ul>
 * <p>
 * Background workers should filter tasks by organizationId to maintain tenant isolation.
 * 
 *
 * <b>Integration with Background Workers</b>
 * <p>
 * Task execution is coordinated by background job schedulers in {@link com.openkoda.core.job}.
 * Workers query for eligible tasks, execute them, and update state:
 * 
 * <pre>
 * Task task = taskRepository.findStartableTask();
 * task.start();  // State: NEW → DOING, attempts++
 * try {
 *     executeTask(task);
 *     task.complete();  // State: DOING → DONE
 * } catch (Exception e) {
 *     task.fail();  // State: DOING → FAILED or FAILED_PERMANENTLY
 * }
 * </pre>
 *
 * <b>Task Monitoring</b>
 * <p>
 * Task execution can be monitored through:
 * 
 * <ul>
 *   <li>Task state: Query tasks by state to identify pending/failed tasks</li>
 *   <li>Attempt count: Identify tasks requiring attention (high attempts)</li>
 *   <li>Timestamps: Inherited {@code createdOn}/{@code modifiedOn} from {@link com.openkoda.model.common.TimestampedEntity}</li>
 *   <li>Audit trail: {@code toAuditString()} and {@code contentProperties()} for searchable content</li>
 * </ul>
 *
 * <b>Integration with Scheduler</b>
 * <p>
 * For recurring tasks, integrate with {@link com.openkoda.model.component.Scheduler} entity which
 * defines cron-based schedules for periodic task creation. The scheduler creates new Task
 * instances at configured intervals.
 * 
 *
 * <b>Database Schema</b>
 * <p>
 * Tasks table structure:
 * 
 * <ul>
 *   <li><strong>id</strong> - Primary key (sequence-generated, organization-related ID range)</li>
 *   <li><strong>type</strong> - Discriminator column ('email', 'httprequest', etc.)</li>
 *   <li><strong>state</strong> - Task lifecycle state (VARCHAR, enum values)</li>
 *   <li><strong>attempts</strong> - Retry attempt counter (INT)</li>
 *   <li><strong>start_after</strong> - Scheduled execution time (TIMESTAMP)</li>
 *   <li><strong>organization_id</strong> - Tenant scope (BIGINT, nullable)</li>
 *   <li><strong>created_on</strong> - Creation timestamp (inherited from TimestampedEntity)</li>
 *   <li><strong>modified_on</strong> - Last modification timestamp (inherited)</li>
 *   <li>Type-specific columns for Email and HttpRequestTask subclasses</li>
 * </ul>
 *
 * <b>Thread Safety</b>
 * <p>
 * Task state mutations ({@code start()}, {@code fail()}, {@code complete()}) are not
 * internally synchronized. Thread safety relies on:
 * 
 * <ul>
 *   <li>JPA transaction isolation preventing concurrent modifications</li>
 *   <li>Background worker coordination to prevent duplicate task execution</li>
 *   <li>Database-level locking when selecting tasks for execution</li>
 * </ul>
 *
 * @see com.openkoda.model.task.Task
 * @see com.openkoda.model.task.Email
 * @see com.openkoda.model.task.HttpRequestTask
 * @see com.openkoda.core.job.EmailSenderJob
 * @see com.openkoda.model.component.Scheduler
 * @since 1.7.1
 * @author OpenKoda Team
 * @version 1.7.1
 */
package com.openkoda.model.task;