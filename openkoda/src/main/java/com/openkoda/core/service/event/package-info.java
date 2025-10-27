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
 * <p>Consists of all event-related service classes.</p>
 * <p>Schedulers are also considered as event related as they schedule events emissions.</p>
 * <br/>
 * <p><b>Should I put a class into this package?</b></p>
 * <p>If the class is a service performing event-related logic or is a helper object user in other event-related
 * services then YES.</p>
 * <br/>
 * <h2>Package Purpose</h2>
 * <p>Application event publishing and handling infrastructure providing synchronous in-process events 
 * (ApplicationEvent) and asynchronous cluster-wide events (ClusterEvent) with scheduled event triggers 
 * (SchedulerService) and dynamic listener registration (EventListenerService).</p>
 *
 * <h2>Key Architecture Patterns</h2>
 *
 * <h3>1. IN-PROCESS EVENT BUS (ApplicationEvent)</h3>
 * <ul>
 * <li>Synchronous and asynchronous event dispatch within single JVM instance via ApplicationEventService</li>
 * <li>Type-safe event descriptors (AbstractApplicationEvent, ApplicationEvent) pairing canonical names with payload Classes</li>
 * <li>Functional and reflective consumer registration via EventConsumer wrapper abstraction</li>
 * <li>Event listeners registered programmatically or persisted in EventListenerEntry entities</li>
 * </ul>
 *
 * <h3>2. CLUSTER EVENT PROPAGATION (ClusterEvent)</h3>
 * <ul>
 * <li>Cross-node configuration synchronization in distributed Hazelcast deployments</li>
 * <li>ClusterEventSenderService publishes lifecycle events (SCHEDULER_*, EVENT_LISTENER_*, FORM_*) to cluster topic</li>
 * <li>ClusterEventListenerService consumes messages and dispatches to SchedulerService, EventListenerService, FormService</li>
 * <li>Active only in 'hazelcast' profile; no-op in single-instance mode</li>
 * </ul>
 *
 * <h3>3. SCHEDULED EVENT TRIGGERS (SchedulerService)</h3>
 * <ul>
 * <li>Cron-based task scheduling via Spring TaskScheduler publishing SCHEDULER_EXECUTED events</li>
 * <li>Persisted Scheduler entities with cronExpression, eventData, organizationId, onMasterOnly, isAsync configuration</li>
 * <li>SchedulerTask Runnable sets MDC request ID, respects cluster master-only semantics, publishes to ApplicationEventService</li>
 * <li>Cluster-aware lifecycle methods propagate scheduler registration/removal across nodes</li>
 * </ul>
 *
 * <h3>4. EVENT LISTENER REGISTRY (EventListenerService)</h3>
 * <ul>
 * <li>Discovery, conversion, and lifecycle management of persisted EventListenerEntry records</li>
 * <li>Reflection-based event type and consumer method discovery for UI dropdowns</li>
 * <li>Registers consumers with ApplicationEventService using EventConsumer wrapper</li>
 * <li>Cluster-aware registration/unregistration propagates listener configuration across nodes</li>
 * </ul>
 *
 * <h2>Key Classes</h2>
 *
 * <h3>Event Descriptors</h3>
 * <ul>
 * <li><b>AbstractApplicationEvent</b>: Generic base class for typed event descriptors with static registry</li>
 * <li><b>ApplicationEvent</b>: Concrete subclass declaring canonical application-wide events 
 * (USER_*, ORGANIZATION_*, NOTIFICATION_*, BACKUP_*, SCHEDULER_EXECUTED, etc.)</li>
 * </ul>
 *
 * <h3>Event Services</h3>
 * <ul>
 * <li><b>ApplicationEventService</b>: Central in-process event bus managing listener registration and 
 * synchronous/asynchronous event dispatch</li>
 * <li><b>EventListenerService</b>: Event listener discovery, conversion, and lifecycle management for 
 * persisted EventListenerEntry</li>
 * <li><b>SchedulerService</b>: Scheduled task orchestration bridging Scheduler entities to Spring TaskScheduler 
 * with event publishing</li>
 * </ul>
 *
 * <h3>Cluster Synchronization</h3>
 * <ul>
 * <li><b>ClusterEvent</b>: Serializable DTO propagating configuration lifecycle events 
 * (SCHEDULER_*, EVENT_LISTENER_*, FORM_*) via Hazelcast</li>
 * <li><b>ClusterEventSenderService</b>: Stateless publisher propagating events to Hazelcast cluster topic</li>
 * <li><b>ClusterEventListenerService</b>: Hazelcast MessageListener routing cluster events to service handlers</li>
 * </ul>
 *
 * <h3>Consumer Infrastructure</h3>
 * <ul>
 * <li><b>EventConsumer</b>: Immutable wrapper normalizing functional (Consumer&lt;T&gt;, BiConsumer&lt;T,String[]&gt;) 
 * and reflective (Method) consumer modalities</li>
 * <li><b>EventListener</b>: Functional interface extending Consumer&lt;T&gt; for semantic event handler marking</li>
 * <li><b>EventConsumerCategory</b>: Enum categorizing consumers by domain 
 * (INTEGRATION, BACKUP, MESSAGE, PUSH_NOTIFICATION, ROLE_MODIFICATION, SERVER_SIDE_CODE)</li>
 * </ul>
 *
 * <h2>Usage Patterns</h2>
 *
 * <h3>Event Publishing</h3>
 * <pre>{@code
 * // Synchronous event dispatch
 * services.applicationEvent.emitEvent(ApplicationEvent.USER_CREATED, basicUser);
 *
 * // Asynchronous event dispatch
 * services.applicationEvent.emitEventAsync(ApplicationEvent.BACKUP_CREATED, backupFile);
 * }</pre>
 *
 * <h3>Listener Registration (Programmatic)</h3>
 * <pre>{@code
 * // Lambda consumer
 * services.applicationEvent.registerEventListener(
 *     ApplicationEvent.USER_LOGGED_IN, 
 *     user -> auditLog(user)
 * );
 *
 * // Method reference
 * services.applicationEvent.registerEventListener(
 *     ApplicationEvent.ORGANIZATION_CREATED,
 *     this::handleOrgCreated
 * );
 * }</pre>
 *
 * <h3>Listener Registration (Persisted)</h3>
 * <pre>{@code
 * // Cluster-aware registration from EventListenerEntry
 * EventListenerEntry entry = new EventListenerEntry(
 *     "ApplicationEvent", "USER_CREATED", "BasicUser",
 *     "AuditService", "logUserCreation", null, null, null, null
 * );
 * eventListenerService.registerListenerClusterAware(entry);
 * }</pre>
 *
 * <h3>Scheduled Event Configuration</h3>
 * <pre>{@code
 * // Create scheduler publishing event on cron schedule
 * Scheduler scheduler = new Scheduler();
 * scheduler.setCronExpression("0 0 2 * * ?"); // Daily at 2 AM
 * scheduler.setEventData("dailyBackup");
 * scheduler.setOrganizationId(orgId);
 * scheduler.setOnMasterOnly(true); // Run only on cluster master
 * scheduler.setAsync(false); // Synchronous event dispatch
 * schedulerService.schedule(scheduler);
 * }</pre>
 *
 * <h2>Cluster Propagation Workflow</h2>
 *
 * <h3>Scheduler Lifecycle</h3>
 * <ol>
 * <li>Node A: schedulerService.schedule(scheduler) persists Scheduler entity</li>
 * <li>Node A: clusterEventSenderService.loadScheduler(id) publishes SCHEDULER_ADD to cluster topic</li>
 * <li>All nodes (including A): ClusterEventListenerService.onMessage() receives event</li>
 * <li>All nodes: schedulerService.loadFromDb(id) loads entity and registers with Spring TaskScheduler</li>
 * </ol>
 *
 * <h3>Event Listener Lifecycle</h3>
 * <ol>
 * <li>Node B: eventListenerService.registerListenerClusterAware(entry) persists EventListenerEntry</li>
 * <li>Node B: clusterEventSenderService.loadEventListener(id) publishes EVENT_LISTENER_ADD</li>
 * <li>All nodes: ClusterEventListenerService.onMessage() receives event</li>
 * <li>All nodes: eventListenerService.loadFromDb(id) registers consumer with ApplicationEventService</li>
 * </ol>
 *
 * <h2>Error Handling and Retry</h2>
 *
 * <h3>ApplicationEvent Dispatch</h3>
 * <ul>
 * <li>Synchronous emitEvent(): First listener exception propagates to caller, halting remaining listeners</li>
 * <li>Asynchronous emitEventAsync(): Listener exceptions swallowed by fixed thread pool (4 threads); no error feedback</li>
 * <li>No automatic retry mechanism; consumers must implement own error recovery</li>
 * </ul>
 *
 * <h3>ClusterEvent Propagation</h3>
 * <ul>
 * <li>Hazelcast publish() exceptions propagate to caller; no built-in retry</li>
 * <li>Message ordering guaranteed from single publisher; concurrent publishers may interleave</li>
 * <li>Handler operations designed to be idempotent for rare duplicate message scenarios</li>
 * </ul>
 *
 * <h2>Thread-Safety Considerations</h2>
 *
 * <h3>ApplicationEventService</h3>
 * <ul>
 * <li>Registration methods synchronized; event dispatch NOT synchronized</li>
 * <li>Public listeners Map is mutable; external modifications bypass synchronization</li>
 * <li>Async executor (4 fixed threads) may invoke listeners concurrently; consumers must be thread-safe</li>
 * </ul>
 *
 * <h3>EventListenerService</h3>
 * <ul>
 * <li>Internal collections (events, consumers, consumersArray) NOT synchronized</li>
 * <li>Reflection-based discovery operates on immutable ApplicationEvent static fields (thread-safe)</li>
 * </ul>
 *
 * <h3>SchedulerService</h3>
 * <ul>
 * <li>currentlyScheduled HashMap NOT synchronized; concurrent modifications may cause race conditions</li>
 * <li>Spring TaskScheduler handles concurrent task execution safely</li>
 * <li>SchedulerTask.run() sets MDC context before event publishing for request correlation</li>
 * </ul>
 *
 * <h2>Integration Notes</h2>
 *
 * <h3>Spring Framework</h3>
 * <ul>
 * <li>ApplicationEventService can optionally integrate with Spring ApplicationEventPublisher for unified event system</li>
 * <li>SchedulerService uses Spring TaskScheduler (typically ThreadPoolTaskScheduler) with CronTrigger for cron evaluation</li>
 * <li>EventListenerService uses @DependsOn to ensure ApplicationEventService initialized before listener registration</li>
 * </ul>
 *
 * <h3>Hazelcast</h3>
 * <ul>
 * <li>Cluster events published to ITopic&lt;ClusterEvent&gt; named CLUSTER_EVENT_TOPIC</li>
 * <li>ClusterEventListenerService registered as MessageListener&lt;ClusterEvent&gt; during Hazelcast configuration</li>
 * <li>Active only when ClusterHelper.isCluster() returns true ('hazelcast' profile)</li>
 * </ul>
 *
 * <h3>Database Persistence</h3>
 * <ul>
 * <li>Scheduler entities persisted for durable scheduled task configuration across restarts</li>
 * <li>EventListenerEntry entities enable dynamic listener registration without code deployment</li>
 * <li>Cluster event handlers reload entities by ID after receiving propagation messages</li>
 * </ul>
 *
 * <h2>Common Pitfalls</h2>
 * <ol>
 * <li>Forgetting to publish cluster events after database changes in distributed deployments</li>
 * <li>Not handling null event payloads in consumer methods</li>
 * <li>Assuming synchronous emitEvent() completes all listeners (first exception halts dispatch)</li>
 * <li>Modifying ApplicationEventService.listeners Map externally without synchronization</li>
 * <li>Registering non-thread-safe consumers for async event dispatch</li>
 * <li>Not implementing idempotent cluster event handlers (duplicate messages may arrive)</li>
 * </ol>
 *
 * <h2>Related Packages</h2>
 * <ul>
 * <li>com.openkoda.model.component.event: EventListenerEntry and Consumer entity definitions</li>
 * <li>com.openkoda.model.component: Scheduler entity for cron-based task configuration</li>
 * <li>com.openkoda.dto.system: ScheduledSchedulerDto event payload for scheduled task execution</li>
 * <li>com.openkoda.core.helper: ClusterHelper for cluster mode detection and Hazelcast access</li>
 * </ul>
 *
 * @since 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.core.service.event;