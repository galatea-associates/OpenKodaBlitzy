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

package com.openkoda.core.service.event;

import java.io.Serializable;

/**
 * Immutable serializable DTO propagating configuration lifecycle events across distributed application instances via Hazelcast cluster messaging.
 * <p>
 * This class enables cross-node synchronization of stateful configuration (Schedulers, EventListeners, Forms) in multi-instance deployments.
 * Events are published by ClusterEventSenderService to a Hazelcast topic and consumed by ClusterEventListenerService on all cluster nodes.

 * <p>
 * Active only in 'hazelcast' profile for clustered deployments; no-op in single-instance mode. Implements {@link Serializable} for Hazelcast network transport.

 * <p>
 * <strong>WARNING:</strong> Lacks explicit serialVersionUID which may cause deserialization issues across versions. Final fields ensure thread-safe message passing;
 * instances are created once and published to the topic.

 * <p>
 * Currently supports three stateful configuration areas:

 * <ul>
 * <li><strong>Schedulers</strong> - Synchronizes scheduled task configuration across nodes</li>
 * <li><strong>EventListeners</strong> - Synchronizes event consumer registration across nodes</li>
 * <li><strong>Forms</strong> - Synchronizes form definitions and triggers dynamic entity generation</li>
 * </ul>
 * <p>
 * The third potential area (Logs) is not critical for cluster propagation.

 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * clusterEventSenderService.sendSchedulerCreated(schedulerId);
 * }</pre>

 * <p>
 * <strong>Design Notes:</strong>

 * <ul>
 * <li>Use ClusterEvent for cross-node configuration sync; ApplicationEvent for in-process business events</li>
 * <li>Events are published to Hazelcast ITopic&lt;ClusterEvent&gt; named cluster topic</li>
 * <li>Published after database commit to ensure entity exists when other nodes reload</li>
 * <li>Handler operations should be idempotent as events may be received multiple times in rare failure scenarios</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ClusterEventSenderService for event publishing API
 * @see ClusterEventListenerService for event consumption and handler routing
 * @see SchedulerService for scheduler lifecycle operations
 * @see EventListenerService for event listener lifecycle operations
 */
public class ClusterEvent implements Serializable {

    /**
     * Categorizes cluster event types for routing to appropriate handlers in ClusterEventListenerService.
     */
    public enum EventType {
        /** Published when new Scheduler created; signals all nodes to load scheduler from database and register with SchedulerService. */
        SCHEDULER_ADD,
        /** Published when Scheduler deleted; signals all nodes to unregister scheduler from SchedulerService and cancel scheduled tasks. */
        SCHEDULER_REMOVE,
        /** Published when Scheduler configuration modified; signals all nodes to reload scheduler from database and reschedule tasks. */
        SCHEDULER_RELOAD,
        /** Published when new EventListenerEntry created; signals all nodes to register event consumer with EventListenerService. */
        EVENT_LISTENER_ADD,
        /** Published when EventListenerEntry deleted; signals all nodes to unregister event consumer from EventListenerService. */
        EVENT_LISTENER_REMOVE,
        /** Published when EventListenerEntry configuration modified; signals all nodes to reload listener registration from database. */
        EVENT_LISTENER_RELOAD,
        /** Published when new Form entity created; signals all nodes to register form with FormService and trigger dynamic entity generation if applicable. */
        FORM_ADD,
        /** Published when Form entity deleted; signals all nodes to unregister form from FormService. */
        FORM_REMOVE,
        /** Published when Form configuration modified; signals all nodes to reload form definition from database and recompile validation rules. */
        FORM_RELOAD
    }

    /**
     * Cluster event category determining which handler processes this event in ClusterEventListenerService.
     */
    public final EventType eventType;
    
    /**
     * Database primary key of modified configuration entity (Scheduler.id, EventListenerEntry.id, or Form.id) for node-local reload.
     */
    public final long id;

    /**
     * Creates immutable cluster event for Hazelcast topic publishing.
     * <p>
     * Typical usage: {@code new ClusterEvent(EventType.SCHEDULER_ADD, scheduler.getId())}

     *
     * @param eventType Event category determining handler routing
     * @param id Database entity ID for node-local reload from persistent storage
     */
    public ClusterEvent(EventType eventType, long id) {
        this.eventType = eventType;
        this.id = id;
    }

}