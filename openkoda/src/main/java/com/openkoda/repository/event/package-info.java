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
 * Provides repository infrastructure for event-driven processing and scheduled job management in OpenKoda.
 * This package contains Spring Data JPA repository interfaces for accessing event listener and scheduler entities,
 * supporting both secure and unsecured data access patterns.
 * 
 * <h2>Package Purpose</h2>
 * <p>
 * This package implements the data access layer for OpenKoda's event-driven architecture and job scheduling system.
 * Event listeners enable dynamic registration of business logic that responds to application events, while schedulers
 * support cron-based job execution that emits events at specified intervals. Together, these repositories provide
 * the persistence foundation for runtime-configurable event processing workflows.
 * </p>
 * 
 * <h2>Key Repository Interfaces</h2>
 * <ul>
 *   <li><b>EventListenerRepository</b> - Base repository for EventListener entities supporting dynamic event handler registration</li>
 *   <li><b>SchedulerRepository</b> - Base repository for Scheduler entities enabling cron-based job scheduling</li>
 *   <li><b>SecureEventListenerRepository</b> - Privilege-enforcing wrapper for EventListener access with SearchableRepositoryMetadata</li>
 *   <li><b>SecureSchedulerRepository</b> - Privilege-enforcing wrapper for Scheduler access with role-based security</li>
 * </ul>
 * 
 * <h2>Package Organization</h2>
 * <p>
 * Repositories in this package follow OpenKoda's dual-repository pattern:
 * </p>
 * <ul>
 *   <li><b>Base Repositories</b> - Extend {@code UnsecuredFunctionalRepositoryWithLongId} providing standard CRUD operations
 *       and custom query methods without privilege enforcement. Used by service layer components that implement
 *       authorization logic explicitly.</li>
 *   <li><b>Secure Repositories</b> - Extend {@code SecureRepository} interface with {@code SearchableRepositoryMetadata}
 *       enabling automatic privilege checks on all data access operations. Integrated with OpenKoda's RBAC system
 *       to enforce read, write, and delete permissions based on user roles and organization context.</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>
 * This package integrates with several key OpenKoda subsystems:
 * </p>
 * <ul>
 *   <li><b>Event Processing Framework</b> ({@code com.openkoda.core.flow}) - Repositories supply event listener
 *       configurations that determine which Flow pipelines execute in response to application events. Event listeners
 *       are resolved at runtime based on event type and organization context.</li>
 *   <li><b>JobsScheduler</b> ({@code com.openkoda.core.job}) - Scheduler repository provides cron expressions and
 *       job configurations to the background job executor. Jobs retrieve their execution schedule from the database,
 *       enabling runtime modification without application restart.</li>
 *   <li><b>Event Entities</b> ({@code com.openkoda.model.component.event}) - Repositories persist EventListener and
 *       Scheduler domain entities that define event handling behavior and scheduling rules.</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Dynamic Event Listener Registration</h3>
 * <pre>
 * EventListener listener = eventListenerRepository.findByEventName("USER_CREATED");
 * listener.executeFlow(eventData);
 * </pre>
 * 
 * <h3>Cron-Based Job Scheduling</h3>
 * <pre>
 * Scheduler job = schedulerRepository.findByName("DailyReportJob");
 * jobsScheduler.schedule(job.getCronExpression(), job::execute);
 * </pre>
 * 
 * <h2>Relationships with Other Packages</h2>
 * <ul>
 *   <li>{@code com.openkoda.model.component.event} - Defines EventListener and Scheduler entity classes persisted by these repositories</li>
 *   <li>{@code com.openkoda.core.job} - Consumes Scheduler configurations to execute background jobs</li>
 *   <li>{@code com.openkoda.core.flow} - Invokes event listeners to trigger Flow pipeline execution</li>
 *   <li>{@code com.openkoda.repository} - Inherits base repository contracts and secure repository infrastructure</li>
 * </ul>
 * 
 * <h2>Why Schedulers Are in the Event Package</h2>
 * <p>
 * Schedulers are included in this event-focused package because scheduled jobs fundamentally emit events at their
 * configured execution times. A scheduler executing a cron job publishes an event that triggers registered event
 * listeners, making schedulers event producers within OpenKoda's event-driven architecture. This co-location
 * reflects the tight coupling between scheduling and event emission.
 * </p>
 * 
 * <h2>Adding Classes to This Package</h2>
 * <p>
 * <b>Should I add a class to this package?</b> Consider these guidelines:
 * </p>
 * <ul>
 *   <li><b>YES</b> if implementing a Spring Data JPA repository for EventListener or Scheduler entities</li>
 *   <li><b>YES</b> if creating custom query methods for event listener discovery or scheduler lookup</li>
 *   <li><b>YES</b> if implementing JPA Specification builders for complex event listener queries</li>
 *   <li><b>NO</b> if implementing event processing logic (belongs in {@code com.openkoda.core.flow})</li>
 *   <li><b>NO</b> if implementing job execution logic (belongs in {@code com.openkoda.core.job})</li>
 *   <li><b>NO</b> if defining entity classes (belongs in {@code com.openkoda.model.component.event})</li>
 * </ul>
 * 
 * @since 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.repository.event;