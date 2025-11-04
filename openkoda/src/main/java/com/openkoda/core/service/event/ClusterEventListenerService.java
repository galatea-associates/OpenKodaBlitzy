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

import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;
import com.openkoda.core.service.form.FormService;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.inject.Inject;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Listener service listening to Hazelcast messages to propagate state changes over the application cluster.
 * <p>
 * Hazelcast MessageListener consuming {@link ClusterEvent} messages from cluster topic and dispatching to 
 * appropriate service handlers for cross-node configuration synchronization. Receives cluster events published 
 * by {@link ClusterEventSenderService} and routes to {@link SchedulerService}, {@link EventListenerService}, 
 * or {@link FormService} based on eventType.

 * <p>
 * Implements Hazelcast {@code MessageListener<ClusterEvent>} registered on CLUSTER_EVENT_TOPIC during Hazelcast 
 * configuration. Active only in 'hazelcast' profile; not loaded in single-instance deployments.

 * <p>
 * All service dependencies are {@code @Lazy} injected to avoid circular dependency issues during Spring context 
 * initialization. Hazelcast invokes {@link #onMessage(Message)} on message listener threads; handler service 
 * methods must be thread-safe.

 * <p>
 * Exceptions in {@link #onMessage(Message)} are logged by Hazelcast but do not stop message processing; 
 * failed nodes may have inconsistent state. Handler operations should be idempotent as duplicate messages 
 * may arrive in rare failure scenarios.

 * <p>
 * Example cluster synchronization flow:
 * <pre>{@code
 * // Node A: Publisher sends event
 * clusterEventSenderService.loadScheduler(123);
 * 
 * // Node B: This listener receives event
 * onMessage() receives event → schedulerService.loadFromDb(123)
 * }</pre>

 * <p>
 * Hazelcast guarantees message order from single publisher; concurrent publishers may interleave. 
 * All cluster nodes (including publisher) receive messages; handlers must handle self-published events gracefully.

 * <p>
 * <strong>WARNING:</strong> {@code @Lazy} injection delays circular dependency but may cause 
 * {@code NullPointerException} if service accessed before Spring fully initialized.

 *
 * @see ClusterEventSenderService for event publishing
 * @see ClusterEvent for event DTO structure and EventType enum
 * @see SchedulerService for scheduler lifecycle operations
 * @see EventListenerService for event listener lifecycle
 * @see FormService for form lifecycle operations
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
@Service
public class ClusterEventListenerService implements MessageListener<ClusterEvent>, LoggingComponentWithRequestId {

    /**
     * Scheduler lifecycle service handling SCHEDULER_* events.
     * <p>
     * {@code @Lazy} injected to break circular dependency with SchedulerService → ClusterEventSenderService → ClusterHelper.
     * Invoked for loadFromDb (SCHEDULER_ADD), remove (SCHEDULER_REMOVE), removeAndLoadFromDb (SCHEDULER_RELOAD).

     * <p>
     * Operations:
     * <ul>
     * <li>SCHEDULER_ADD: Queries Scheduler entity and registers with Spring TaskScheduler</li>
     * <li>SCHEDULER_REMOVE: Cancels ScheduledFuture and removes from currentlyScheduled Map</li>
     * <li>SCHEDULER_RELOAD: Cancels existing schedule and re-registers with updated configuration</li>
     * </ul>

     */
    @Inject @Lazy
    private SchedulerService schedulerService;

    /**
     * Event listener registry service handling EVENT_LISTENER_* events.
     * <p>
     * {@code @Lazy} injected to avoid circular dependency. Invoked for loadFromDb (EVENT_LISTENER_ADD), 
     * unregisterEventListener (EVENT_LISTENER_REMOVE), removeAndLoadFromDb (EVENT_LISTENER_RELOAD).

     * <p>
     * Operations:
     * <ul>
     * <li>EVENT_LISTENER_ADD: Queries EventListenerEntry and registers consumer with ApplicationEventService</li>
     * <li>EVENT_LISTENER_REMOVE: Removes listener tuple from ApplicationEventService registry</li>
     * <li>EVENT_LISTENER_RELOAD: Unregisters and re-registers with updated consumer configuration</li>
     * </ul>

     */
    @Inject @Lazy
    private EventListenerService eventListenerService;

    /**
     * Form lifecycle service handling FORM_* events.
     * <p>
     * {@code @Lazy} injected to avoid circular dependency. Invoked for addForm (FORM_ADD), 
     * reloadForm (FORM_RELOAD), removeForm (FORM_REMOVE).

     * <p>
     * Operations:
     * <ul>
     * <li>FORM_ADD: Loads Form entity and potentially triggers DynamicEntityRegistrationService for entity generation</li>
     * <li>FORM_RELOAD: Refreshes form metadata and recompiles validation rules</li>
     * <li>FORM_REMOVE: Unregisters form from FormService registry</li>
     * </ul>

     */
    @Inject @Lazy
    private FormService formService;

    /**
     * Hazelcast message listener callback dispatching ClusterEvent to appropriate service handler based on eventType switch.
     * <p>
     * Implements {@code MessageListener<ClusterEvent>} interface which mandates void return; cannot propagate exceptions 
     * to publisher. Routes to service methods via eventType switch:
     * <ul>
     * <li>SCHEDULER_* → schedulerService</li>
     * <li>EVENT_LISTENER_* → eventListenerService</li>
     * <li>FORM_* → formService</li>
     * </ul>

     * <p>
     * Invoked on Hazelcast message listener thread pool; not caller's request thread; MDC context may not be available. 
     * Service method exceptions are logged by Hazelcast but swallowed; failed operation may leave node in inconsistent 
     * state requiring manual intervention.

     * <p>
     * Handler methods are designed to be idempotent: loadFromDb/addForm loads only if not already loaded, remove is 
     * no-op if not present, reload always refreshes.

     * <p>
     * Event type handling details:
     * <ul>
     * <li><strong>SCHEDULER_ADD:</strong> Invokes {@code schedulerService.loadFromDb(id)} which queries Scheduler entity 
     * and registers with Spring TaskScheduler</li>
     * <li><strong>SCHEDULER_REMOVE:</strong> Invokes {@code schedulerService.remove(id)} which cancels ScheduledFuture 
     * and removes from currentlyScheduled Map</li>
     * <li><strong>SCHEDULER_RELOAD:</strong> Invokes {@code schedulerService.removeAndLoadFromDb(id)} which cancels 
     * existing schedule and re-registers with updated configuration</li>
     * <li><strong>EVENT_LISTENER_ADD:</strong> Invokes {@code eventListenerService.loadFromDb(id)} which queries 
     * EventListenerEntry and registers consumer with ApplicationEventService</li>
     * <li><strong>EVENT_LISTENER_REMOVE:</strong> Invokes {@code eventListenerService.unregisterEventListener(id)} 
     * which removes listener tuple from ApplicationEventService registry</li>
     * <li><strong>EVENT_LISTENER_RELOAD:</strong> Invokes {@code eventListenerService.removeAndLoadFromDb(id)} which 
     * unregisters and re-registers with updated consumer configuration</li>
     * <li><strong>FORM_ADD:</strong> Invokes {@code formService.addForm(id)} which loads Form entity and potentially 
     * triggers DynamicEntityRegistrationService for entity generation</li>
     * <li><strong>FORM_RELOAD:</strong> Invokes {@code formService.reloadForm(id)} which refreshes form metadata and 
     * recompiles validation rules</li>
     * <li><strong>FORM_REMOVE:</strong> Invokes {@code formService.removeForm(id)} which unregisters form from 
     * FormService registry</li>
     * </ul>

     *
     * @param message Hazelcast Message wrapper containing ClusterEvent payload with eventType and entity ID
     */
    @Override
    //TODO Rule 2.1: public method must not return void - it's implementation of an interface so can't change the signature of the method
    public void onMessage(Message<ClusterEvent> message) {
        ClusterEvent m = message.getMessageObject();
        debug("[onMessage] {} {}", m.eventType, m.id);
        switch (m.eventType) {
            case SCHEDULER_ADD: schedulerService.loadFromDb(m.id); break;
            case SCHEDULER_REMOVE: schedulerService.remove(m.id); break;
            case SCHEDULER_RELOAD: schedulerService.removeAndLoadFromDb(m.id); break;
            case EVENT_LISTENER_ADD: eventListenerService.loadFromDb(m.id); break;
            case EVENT_LISTENER_REMOVE: eventListenerService.unregisterEventListener(m.id); break;
            case EVENT_LISTENER_RELOAD: eventListenerService.removeAndLoadFromDb(m.id); break;
            case FORM_ADD: formService.addForm(m.id); break;
            case FORM_RELOAD: formService.reloadForm(m.id); break;
            case FORM_REMOVE: formService.removeForm(m.id); break;
        }

    }


}
