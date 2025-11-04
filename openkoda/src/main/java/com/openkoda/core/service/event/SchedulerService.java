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

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.helper.ClusterHelper;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.core.tracker.RequestIdHolder;
import com.openkoda.dto.system.ScheduledSchedulerDto;
import com.openkoda.model.component.Scheduler;
import jakarta.inject.Inject;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * Scheduled task orchestration service bridging persisted Scheduler entities to Spring TaskScheduler with cron-based execution and cluster-aware lifecycle management.
 * <p>
 * This service registers {@link Scheduler} entities as Spring scheduled tasks using cron expressions, publishes SCHEDULER_EXECUTED events to trigger business logic,
 * and manages task lifecycle operations including schedule, reschedule, and removal. It extends {@link ComponentProvider} for services and repositories access
 * and implements {@link HasSecurityRules} for privilege checking on schedule/remove operations.

 * <p>
 * <b>Architecture:</b> Maintains an unsynchronized {@code HashMap<Long, ScheduledFuture>} tracking active scheduled tasks for cancellation.
 * Uses Spring {@link TaskScheduler} with {@link CronTrigger} for cron expression evaluation and task execution.

 * <p>
 * <b>Cluster Behavior:</b> Cluster-aware methods delegate to {@link ClusterEventSenderService} in distributed mode to propagate scheduler lifecycle
 * (add/remove/reload) across all nodes. In single-instance mode, operations are performed directly.

 * <p>
 * <b>Event Publishing:</b> {@link SchedulerTask} publishes SCHEDULER_EXECUTED event when scheduled tasks execute.
 * Event dispatch is synchronous or asynchronous based on {@link Scheduler#isAsync()} flag.

 * <p>
 * <b>Thread-Safety WARNING:</b> The {@code currentlyScheduled} HashMap is NOT synchronized. Concurrent schedule/remove operations may cause race conditions,
 * {@link java.util.ConcurrentModificationException}, or lost updates.

 * <p>
 * Usage examples:
 * <pre>{@code
 * // Schedule a scheduler
 * schedulerService.schedule(new Scheduler("0 0 * * * ?", "backupData", orgId, false, false));
 * 
 * // Cluster-aware scheduling
 * schedulerService.loadClusterAware(scheduler.getId());
 * }</pre>

 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @since 2019-03-20
 * @version 1.7.1
 * @see Scheduler
 * @see ScheduledSchedulerDto
 * @see ApplicationEvent#SCHEDULER_EXECUTED
 * @see ClusterEventSenderService
 */
@Service
public class SchedulerService extends ComponentProvider implements HasSecurityRules {


    /**
     * Unsynchronized HashMap mapping Scheduler.id to ScheduledFuture for task cancellation; enables remove() to cancel running tasks.
     * <p>
     * <b>WARNING:</b> NOT thread-safe. Concurrent modifications may cause {@link java.util.ConcurrentModificationException} or lost updates.

     */
    private Map<Long, ScheduledFuture> currentlyScheduled = new HashMap<>();

    /**
     * Spring TaskScheduler bean for scheduling tasks with cron triggers; typically ThreadPoolTaskScheduler with configurable pool size.
     */
    private final TaskScheduler taskScheduler;

    /**
     * Cluster event publisher for propagating scheduler lifecycle (add/remove/reload) across distributed nodes.
     */
    @Inject
    private ClusterEventSenderService clusterEventSenderService;

    /**
     * Constructs SchedulerService with injected Spring TaskScheduler.
     *
     * @param taskScheduler Spring-managed TaskScheduler bean injected via constructor for scheduling operations
     */
    @Autowired
    public SchedulerService(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    /**
     * Loads all Scheduler entities from database and schedules each with Spring TaskScheduler; typically invoked during application startup.
     * <p>
     * Retrieves all {@link Scheduler} entities using {@code repositories.unsecure.scheduler.findAll()} and schedules each by calling {@link #schedule(Scheduler)}.
     * Individual schedule failures are logged but do not stop iteration or affect the return value.

     *
     * @return true indicating operation completed (individual schedule failures logged but do not stop iteration)
     */
    public boolean scheduleAllFromDb() {
        debug("[scheduleAllFromDb]");
        repositories.unsecure.scheduler.findAll().forEach(this::schedule);
        return true;
    }

    /**
     * Cluster-aware scheduler registration: publishes cluster event in distributed mode, direct schedule in single-instance mode.
     * <p>
     * In cluster mode, {@link ClusterEventSenderService} publishes SCHEDULER_ADD event to all nodes.
     * In single-instance mode, loads {@link Scheduler} from database and schedules directly.

     *
     * @param schedulerId Database primary key of Scheduler entity to load and schedule
     * @return true if scheduler loaded and scheduled successfully
     */
    public boolean loadClusterAware(long schedulerId) {
        debug("[loadClusterAware] {}", schedulerId);
        if (ClusterHelper.isCluster()) {
            return clusterEventSenderService.loadScheduler(schedulerId);
        }
        Scheduler s = repositories.unsecure.scheduler.findOne(schedulerId);
        return schedule(s);
    }

    /**
     * Cluster-aware scheduler unregistration: publishes cluster event in distributed mode, direct remove in single-instance mode.
     * <p>
     * In cluster mode, {@link ClusterEventSenderService} publishes SCHEDULER_REMOVE event to all nodes.
     * In single-instance mode, calls {@link #remove(Long)} directly to cancel and remove the scheduled task.

     *
     * @param schedulerId Database primary key of Scheduler to cancel and remove
     * @return true if scheduler successfully removed
     */
    public boolean removeClusterAware(long schedulerId) {
        debug("[removeClusterAware] {}", schedulerId);
        if (ClusterHelper.isCluster()) {
            return clusterEventSenderService.removeScheduler(schedulerId);
        }
        return remove(schedulerId);
    }

    /**
     * Cluster-aware scheduler reload: publishes cluster event in distributed mode, direct remove-and-reload in single-instance mode.
     * <p>
     * In cluster mode, {@link ClusterEventSenderService} publishes SCHEDULER_RELOAD event to all nodes.
     * In single-instance mode, calls {@link #removeAndLoadFromDb(long)} to cancel existing scheduler and reload with updated configuration.

     *
     * @param schedulerId Database primary key of Scheduler to reload with updated configuration
     * @return true if scheduler successfully reloaded
     */
    public boolean reloadClusterAware(long schedulerId) {
        debug("[reloadClusterAware] {}", schedulerId);
        if (ClusterHelper.isCluster()) {
            return clusterEventSenderService.reloadScheduler(schedulerId);
        }
        return removeAndLoadFromDb(schedulerId);
    }

    /**
     * Loads Scheduler entity from database by ID and schedules with Spring TaskScheduler; used by cluster event handlers.
     * <p>
     * Retrieves {@link Scheduler} using {@code repositories.unsecure.scheduler.findOne(schedulerId)} and delegates to {@link #schedule(Scheduler)}.
     * Typically invoked by {@link ClusterEventListenerService} when handling SCHEDULER_ADD events in cluster mode.

     *
     * @param schedulerId Database primary key of Scheduler to load
     * @return true if scheduler found and scheduled; false if entity not found or scheduling failed
     * @see SchedulerService#schedule(Scheduler)
     */
    public boolean loadFromDb(long schedulerId) {
        debug("[loadFromDb] {}", schedulerId);
        Scheduler s = repositories.unsecure.scheduler.findOne(schedulerId);
        return schedule(s);
    }

    /**
     * Atomic operation canceling existing scheduler and reloading from database; used by ClusterEventListenerService for RELOAD events.
     * <p>
     * Calls {@link #remove(Long)} to cancel the current scheduled task, then {@link #loadFromDb(long)} to reload updated configuration.
     * <b>NOT truly atomic:</b> remove may succeed but reload may fail, leaving scheduler unscheduled.

     *
     * @param schedulerId Database primary key of Scheduler to reload
     * @return true if remove successful AND reload successful; false if remove failed (reload not attempted)
     * @see SchedulerService#loadFromDb(long)
     * @see SchedulerService#remove(Long)
     */
    public boolean removeAndLoadFromDb(long schedulerId) {
        debug("[removeAndLoadFromDb] {}", schedulerId);
        if (remove(schedulerId)) {
            return loadFromDb(schedulerId);
        }
        return false;
    }

    /**
     * Reschedules existing task with updated Scheduler configuration by canceling old schedule and creating new.
     * <p>
     * Calls {@link #remove(Long)} to cancel the existing scheduled task identified by {@code schedulerId},
     * then {@link #schedule(Scheduler)} to register the updated {@link Scheduler} configuration.
     * Used when cron expression or other scheduler settings change.

     *
     * @param schedulerId Database primary key of existing Scheduler to cancel
     * @param scheduler Updated Scheduler entity with new cron expression or configuration
     * @return true if remove successful AND reschedule successful; false if remove failed
     * @see SchedulerService#schedule(Scheduler)
     * @see SchedulerService#remove(Long)
     */
    public boolean reschedule(Long schedulerId, Scheduler scheduler) {
        debug("[reschedule] {} {}", schedulerId, scheduler);
        if (remove(schedulerId)) {
            return schedule(scheduler);
        }
        return false;
    }


    /**
     * Core scheduling logic: creates SchedulerTask Runnable, registers with Spring TaskScheduler using CronTrigger, stores ScheduledFuture in currentlyScheduled.
     * <p>
     * Constructs {@link ScheduledSchedulerDto} with current timestamp, wraps in {@link SchedulerTask} Runnable,
     * schedules with {@link CronTrigger} based on {@link Scheduler#getCronExpression()}, and stores {@link ScheduledFuture}
     * in {@code currentlyScheduled} map for cancellation.

     * <p>
     * <b>Thread-Safety:</b> NOT synchronized; concurrent {@code schedule()} calls for same {@code schedulerId} overwrite {@link ScheduledFuture} reference.

     * <p>
     * <b>Security:</b> Requires CHECK_CAN_MANAGE_EVENT_LISTENERS privilege.

     *
     * @param scheduler Scheduler entity with cronExpression, eventData, organizationId, onMasterOnly, isAsync configuration
     * @return false if scheduler null; true otherwise (scheduling success not guaranteed, exceptions may propagate)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_EVENT_LISTENERS)
    public boolean schedule(Scheduler scheduler) {
        if (scheduler != null) {
            debug("Scheduling task {} with data {}", scheduler.getCronExpression(), scheduler.getEventData());
            ScheduledSchedulerDto schedulerDto =
                new ScheduledSchedulerDto(
                    scheduler.getCronExpression(),
                    scheduler.getEventData(),
                    scheduler.getOrganizationId(),
                    scheduler.isOnMasterOnly(),
                            scheduler.isAsync(),
                    LocalDateTime.now());

            currentlyScheduled.put(scheduler.getId(),
                    taskScheduler.schedule(
                            new SchedulerTask(schedulerDto), new CronTrigger(scheduler.getCronExpression())
                    )
            );
            return true;
        }
        return false;
    }

    /**
     * Cancels scheduled task and removes from currentlyScheduled Map.
     * <p>
     * Retrieves {@link ScheduledFuture} from {@code currentlyScheduled} map and calls {@code cancel(false)},
     * allowing currently-executing task to complete without interruption. Removes entry from map upon successful cancellation.

     * <p>
     * <b>Cancellation Behavior:</b> Calls {@link ScheduledFuture#cancel(boolean)} with {@code false},
     * allowing running task to complete; does not interrupt running task.

     * <p>
     * <b>Security:</b> Requires CHECK_CAN_MANAGE_EVENT_LISTENERS privilege.

     *
     * @param schedulerId Database primary key identifying scheduled task to cancel
     * @return true if ScheduledFuture found, successfully canceled, and removed; false if not found or cancel failed
     */
    @PreAuthorize(CHECK_CAN_MANAGE_EVENT_LISTENERS)
    public boolean remove(Long schedulerId) {
        ScheduledFuture scheduledFuture = currentlyScheduled.get(schedulerId);
        debug("Removing scheduled task for scheduler ID {}", schedulerId);
        if (scheduledFuture != null) {
            if (scheduledFuture.cancel(false)) {
                debug("Scheduled task for scheduler ID {} removed", schedulerId);
                currentlyScheduled.remove(schedulerId);
                return true;
            }
        }
        return false;
    }

    /**
     * Runnable wrapper emitting SCHEDULER_EXECUTED event when scheduled task executes; sets MDC request ID for trace correlation, respects onMasterOnly cluster semantics.
     * <p>
     * Bridges Spring {@link TaskScheduler} execution to {@link ApplicationEvent} system; publishes {@link ScheduledSchedulerDto} payload for event consumers.
     * Created during {@link #schedule(Scheduler)} and invoked by Spring TaskScheduler at cron schedule.

     * <p>
     * <b>MDC Context:</b> Sets {@link RequestIdHolder#PARAM_CRON_JOB_ID} for request-correlated logging throughout event consumer execution.

     * <p>
     * <b>Cluster Awareness:</b> If {@code onMasterOnly=true} and not {@link ClusterHelper#isMaster()}, skips execution with debug log.

     * <p>
     * <b>Event Dispatch:</b> Publishes to {@link ApplicationEvent#SCHEDULER_EXECUTED}; consumers receive {@link ScheduledSchedulerDto};
     * {@code isAsync} determines synchronous or async dispatch.

     */
    public class SchedulerTask implements Runnable {

        /**
         * ScheduledSchedulerDto containing cron expression, event data, organization ID, cluster flags, and scheduled timestamp.
         */
        private ScheduledSchedulerDto executedScheduler;

        /**
         * Creates SchedulerTask with event payload for scheduled execution.
         *
         * @param executedScheduler Scheduler configuration and metadata to publish when task executes
         */
        SchedulerTask(ScheduledSchedulerDto executedScheduler) {
            this.executedScheduler = executedScheduler;
        }

        /**
         * TaskScheduler invokes run() at cron schedule; sets MDC request ID, checks master-only constraint, publishes SCHEDULER_EXECUTED event synchronously or asynchronously.
         * <p>
         * Sets MDC {@link RequestIdHolder#PARAM_CRON_JOB_ID} for request-correlated logging throughout event consumer execution.
         * If {@code onMasterOnly=true} and not {@link ClusterHelper#isMaster()}, skips execution with debug log.
         * Publishes {@link ApplicationEvent#SCHEDULER_EXECUTED} event; {@code isAsync} flag determines synchronous or async dispatch.

         */
        @Override
        public void run() {
            MDC.put(RequestIdHolder.PARAM_CRON_JOB_ID, RequestIdHolder.generate());
            if (executedScheduler.onMasterOnly && not(ClusterHelper.isMaster())) {
                debug("[SchedulerTask] {}, not master, skipping.", executedScheduler.notificationMessage());
                return;
            }
            debug("[SchedulerTask] {}", executedScheduler.notificationMessage());
            if (!executedScheduler.isAsync()) {
                services.applicationEvent.emitEvent(ApplicationEvent.SCHEDULER_EXECUTED, executedScheduler);
            } else {
                services.applicationEvent.emitEventAsync(ApplicationEvent.SCHEDULER_EXECUTED, executedScheduler);
            }
        }

    }

}
