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

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import org.springframework.stereotype.Service;

import static com.openkoda.core.helper.ClusterHelper.*;
import static com.openkoda.core.service.event.ClusterEvent.EventType.*;

/**
 * Stateless publisher propagating configuration lifecycle events to Hazelcast cluster topic for cross-node
 * synchronization in distributed deployments.
 * <p>
 * This service publishes ClusterEvent messages to all cluster nodes when Scheduler, EventListenerEntry,
 * or Form entities are created, modified, or deleted. It provides the broadcast mechanism for keeping
 * runtime configuration synchronized across a multi-node OpenKoda cluster deployment.

 * <p>
 * <b>Architecture:</b> Uses {@link com.openkoda.core.helper.ClusterHelper#isCluster()} check before
 * publishing. In single-instance mode (non-cluster), all methods perform no operation and return false.
 * In cluster mode, events are published to Hazelcast ITopic&lt;ClusterEvent&gt; named CLUSTER_EVENT_TOPIC
 * retrieved via {@code getHazelcastInstance().getTopic()}.

 * <p>
 * <b>Statefulness:</b> Completely stateless service with no caching or internal state. Safe for concurrent
 * invocation from multiple threads without synchronization.

 * <p>
 * <b>Transaction Context:</b> Typically invoked AFTER database transaction commit to ensure entity exists
 * when other nodes reload from database. Publish operations should occur after successful persistence.

 * <p>
 * <b>Error Handling:</b> Hazelcast publish() exceptions propagate to caller. No retry or error recovery
 * logic is implemented. Calling services should handle failures appropriately.

 * <p>
 * <b>Usage:</b> Called by SchedulerService, EventListenerService, and FormService after entity persistence
 * operations to notify other cluster nodes to synchronize their runtime state.

 * <p>
 * <b>Idempotency:</b> Publish operations are idempotent. Duplicate events are handled gracefully by
 * receiving nodes through the ClusterEventListenerService event handlers.

 * <p>
 * <b>Cluster Detection:</b> Uses ClusterHelper.isCluster() which checks for 'hazelcast' Spring profile
 * activation to determine if cluster mode is enabled.

 * <p>
 * Example usage:
 * <pre>{@code
 * // After scheduler persistence
 * Scheduler scheduler = schedulerRepository.save(newScheduler);
 * clusterEventSenderService.loadScheduler(scheduler.getId());
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ClusterEventListenerService for event consumption and handler routing
 * @see ClusterEvent for event DTO structure
 * @see com.openkoda.core.helper.ClusterHelper for cluster mode detection and Hazelcast access
 */
@Service
public class ClusterEventSenderService implements LoggingComponentWithRequestId {


    /**
     * Publishes SCHEDULER_ADD event signaling all cluster nodes to load scheduler from database and
     * register scheduled tasks.
     * <p>
     * Called by SchedulerService.schedule() after successful database insert. Receiving nodes invoke
     * SchedulerService.loadScheduler() to query the scheduler entity by ID and register it in their
     * local job scheduler instance.

     * <p>
     * <b>Timing:</b> Publish AFTER database commit to ensure entity exists when other nodes query by ID.

     *
     * @param schedulerId Database primary key of Scheduler entity to load
     * @return true if cluster mode enabled and event published; false if single-instance mode (no-op)
     */
    public boolean loadScheduler(long schedulerId) {
        debug("[loadScheduler] {}", schedulerId);
        if(isCluster()) {
            getHazelcastInstance().getTopic(CLUSTER_EVENT_TOPIC).publish(new ClusterEvent(SCHEDULER_ADD, schedulerId));
            return true;
        }
        return false;
    }

    /**
     * Publishes SCHEDULER_RELOAD event signaling all cluster nodes to reload scheduler configuration and
     * reschedule tasks.
     * <p>
     * Called by SchedulerService.reschedule() after database update. Receiving nodes invoke
     * SchedulerService.reloadScheduler() to fetch updated configuration and reschedule the task
     * with new timing or parameters.

     * <p>
     * <b>Timing:</b> Publish AFTER database commit to ensure updated entity is visible to other nodes.

     *
     * @param schedulerId Database primary key of Scheduler entity to reload
     * @return true if cluster mode enabled and event published; false if single-instance mode (no-op)
     */
    public boolean reloadScheduler(long schedulerId) {
        debug("[reloadScheduler] {}", schedulerId);
        if(isCluster()) {
            getHazelcastInstance().getTopic(CLUSTER_EVENT_TOPIC).publish(new ClusterEvent(SCHEDULER_RELOAD, schedulerId));
            return true;
        }
        return false;
    }

    /**
     * Publishes SCHEDULER_REMOVE event signaling all cluster nodes to unregister scheduler and cancel
     * scheduled tasks.
     * <p>
     * Called by SchedulerService.remove() after database deletion. Receiving nodes invoke
     * SchedulerService.removeScheduler() to cancel the scheduled task and remove it from the
     * local job scheduler registry.

     * <p>
     * <b>Timing:</b> Publish AFTER database commit to ensure deletion is persisted before other nodes
     * remove their local registrations.

     *
     * @param schedulerId Database primary key of Scheduler entity to remove
     * @return true if cluster mode enabled and event published; false if single-instance mode (no-op)
     */
    public boolean removeScheduler(long schedulerId) {
        debug("[removeScheduler] {}", schedulerId);
        if(isCluster()) {
            getHazelcastInstance().getTopic(CLUSTER_EVENT_TOPIC).publish(new ClusterEvent(SCHEDULER_REMOVE, schedulerId));
            return true;
        }
        return false;
    }

    /**
     * Publishes EVENT_LISTENER_ADD event signaling all cluster nodes to register event consumer from database.
     * <p>
     * Called by EventListenerService.registerListener() after database insert. Receiving nodes invoke
     * EventListenerService.loadListener() to query the EventListenerEntry by ID and register the
     * event consumer in their local application event system.

     * <p>
     * <b>Timing:</b> Publish AFTER database commit to ensure entity exists when other nodes query by ID.

     *
     * @param eventListenerId Database primary key of EventListenerEntry to load
     * @return true if cluster mode enabled and event published; false if single-instance mode (no-op)
     */
    public boolean loadEventListener(long eventListenerId) {
        debug("[loadEventListener] {}", eventListenerId);
        if(isCluster()) {
            getHazelcastInstance().getTopic(CLUSTER_EVENT_TOPIC).publish(new ClusterEvent(EVENT_LISTENER_ADD, eventListenerId));
            return true;
        }
        return false;
    }

    /**
     * Publishes EVENT_LISTENER_RELOAD event signaling all cluster nodes to reload event consumer configuration.
     * <p>
     * Called after EventListenerEntry update. Receiving nodes re-register the event consumer with
     * updated configuration, including new event types, filters, or handler logic.

     * <p>
     * <b>Timing:</b> Publish AFTER database commit to ensure updated entity is visible to other nodes.

     *
     * @param eventListenerId Database primary key of EventListenerEntry to reload
     * @return true if cluster mode enabled and event published; false if single-instance mode (no-op)
     */
    public boolean reloadEventListener(long eventListenerId) {
        debug("[reloadEventListener] {}", eventListenerId);
        if(isCluster()) {
            getHazelcastInstance().getTopic(CLUSTER_EVENT_TOPIC).publish(new ClusterEvent(EVENT_LISTENER_RELOAD, eventListenerId));
            return true;
        }
        return false;
    }

    /**
     * Publishes EVENT_LISTENER_REMOVE event signaling all cluster nodes to unregister event consumer.
     * <p>
     * Called after EventListenerEntry deletion. Receiving nodes invoke
     * ApplicationEventService.unregisterEventListener() to remove the event consumer from their
     * local application event system.

     * <p>
     * <b>Timing:</b> Publish AFTER database commit to ensure deletion is persisted before other nodes
     * remove their local registrations.

     *
     * @param eventListenerId Database primary key of EventListenerEntry to remove
     * @return true if cluster mode enabled and event published; false if single-instance mode (no-op)
     */
    public boolean removeEventListener(long eventListenerId) {
        debug("[removeEventListener] {}", eventListenerId);
        if(isCluster()) {
            getHazelcastInstance().getTopic(CLUSTER_EVENT_TOPIC).publish(new ClusterEvent(EVENT_LISTENER_REMOVE, eventListenerId));
            return true;
        }
        return false;
    }

    /**
     * Publishes FORM_ADD event signaling all cluster nodes to register form and trigger dynamic entity
     * generation if applicable.
     * <p>
     * Called by FormService after Form insert. Receiving nodes invoke FormService.loadForm() and
     * potentially DynamicEntityRegistrationService if the form requires runtime entity generation.
     * This ensures form metadata and associated dynamic entities are synchronized across all nodes.

     * <p>
     * <b>Timing:</b> Publish AFTER database commit to ensure entity exists when other nodes query by ID.

     *
     * @param formId Database primary key of Form entity to load
     * @return true if cluster mode enabled and event published; false if single-instance mode (no-op)
     */
    public boolean loadForm(long formId) {
        debug("[loadEventListener] {}", formId);
        if(isCluster()) {
            getHazelcastInstance().getTopic(CLUSTER_EVENT_TOPIC).publish(new ClusterEvent(FORM_ADD, formId));
            return true;
        }
        return false;
    }

    /**
     * Publishes FORM_RELOAD event signaling all cluster nodes to reload form definition and recompile
     * validation rules.
     * <p>
     * Called after Form update. Receiving nodes refresh form metadata and regenerate validation
     * logic to reflect the updated configuration. This ensures consistent form behavior across
     * all cluster nodes.

     * <p>
     * <b>Timing:</b> Publish AFTER database commit to ensure updated entity is visible to other nodes.

     *
     * @param formId Database primary key of Form entity to reload
     * @return true if cluster mode enabled and event published; false if single-instance mode (no-op)
     */
    public boolean reloadForm(long formId) {
        debug("[reloadEventListener] {}", formId);
        if(isCluster()) {
            getHazelcastInstance().getTopic(CLUSTER_EVENT_TOPIC).publish(new ClusterEvent(FORM_RELOAD, formId));
            return true;
        }
        return false;
    }

    /**
     * Publishes FORM_REMOVE event signaling all cluster nodes to unregister form from FormService.
     * <p>
     * Called after Form deletion. Receiving nodes invoke FormService.removeForm() to remove the
     * form metadata from their local registry, ensuring the deleted form is no longer available
     * for use across the cluster.

     * <p>
     * <b>Timing:</b> Publish AFTER database commit to ensure deletion is persisted before other nodes
     * remove their local registrations.

     *
     * @param formId Database primary key of Form entity to remove
     * @return true if cluster mode enabled and event published; false if single-instance mode (no-op)
     */
    public boolean removeForm(long formId) {
        debug("[removeEventListener] {}", formId);
        if(isCluster()) {
            getHazelcastInstance().getTopic(CLUSTER_EVENT_TOPIC).publish(new ClusterEvent(FORM_REMOVE, formId));
            return true;
        }
        return false;
    }

}
