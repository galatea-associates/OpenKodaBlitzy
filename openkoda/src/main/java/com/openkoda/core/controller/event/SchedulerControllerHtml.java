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

package com.openkoda.core.controller.event;

import com.openkoda.form.SchedulerForm;
import com.openkoda.model.component.Scheduler;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import static com.openkoda.core.controller.generic.AbstractController.*;

/**
 * Spring MVC REST controller providing HTML view endpoints for Scheduler entity lifecycle management.
 * <p>
 * This controller serves as a thin HTTP adapter layer for the OpenKoda Admin panel scheduler management UI.
 * It binds HTTP requests to domain operations and delegates all business logic to {@link AbstractSchedulerController}.
 * The controller follows a clear separation of concerns: HTTP binding and view rendering are handled here,
 * while validation, persistence, component export, and cluster-aware scheduler operations are delegated to
 * the abstract parent controller.
 * </p>
 * <p>
 * <b>Request Mapping Pattern:</b> All endpoints are mapped under {@code _HTML + _SCHEDULER} path prefix,
 * providing server-side rendered HTML responses with Thymeleaf view fragments for AJAX-based partial page updates.
 * </p>
 * <p>
 * <b>Security Requirements:</b>
 * </p>
 * <ul>
 *   <li>READ operations (getAll, settings, newScheduler) require {@code CHECK_CAN_READ_BACKEND} privilege</li>
 *   <li>MANAGE operations (updateScheduler, createScheduler, remove) require {@code CHECK_CAN_MANAGE_BACKEND} privilege</li>
 * </ul>
 * <p>
 * <b>Flow Pipeline Integration:</b> All methods delegate to AbstractSchedulerController Flow-producing methods,
 * then invoke {@code .mav()} to convert the PageModelMap result into ModelAndView objects with appropriate
 * view fragment names for success and error states.
 * </p>
 * <p>
 * <b>Scheduler Management Features:</b>
 * </p>
 * <ul>
 *   <li>Paginated scheduler search with name-based filtering</li>
 *   <li>Scheduler configuration CRUD operations with cron expression validation</li>
 *   <li>Cluster-aware scheduler registration, reload, and removal</li>
 *   <li>Component export functionality for version control and deployment</li>
 *   <li>Form binding with Jakarta Bean Validation and BindingResult error handling</li>
 * </ul>
 * <p>
 * <b>Cluster-Aware Operations:</b> Scheduler lifecycle operations (create, update, remove) are synchronized
 * across all application instances in the cluster through services.scheduler cluster-aware methods
 * (loadClusterAware, reloadClusterAware, removeClusterAware), ensuring consistent scheduler state.
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @since 2019-03-20
 * @version 1.7.1
 * @see AbstractSchedulerController
 * @see com.openkoda.model.component.Scheduler
 * @see com.openkoda.form.SchedulerForm
 */
@RestController
@RequestMapping(_HTML + _SCHEDULER)
public class SchedulerControllerHtml extends AbstractSchedulerController {

    /**
     * Retrieves a paginated list of all schedulers with optional name-based search filtering.
     * <p>
     * This GET endpoint provides the main scheduler listing page for the Admin panel. It delegates to
     * {@link AbstractSchedulerController#findSchedulersFlow(String, org.springframework.data.jpa.domain.Specification, Pageable)}
     * to execute a secure paginated query, then renders the result using the "scheduler-all" Thymeleaf view fragment.
     * </p>
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_READ_BACKEND} privilege via @PreAuthorize annotation.
     * </p>
     * <p>
     * <b>Request Mapping:</b> GET {@code _HTML + _SCHEDULER + _ALL}
     * </p>
     * <p>
     * <b>Query Parameters:</b>
     * </p>
     * <ul>
     *   <li>scheduler_search - Optional search term for filtering schedulers by name (default: empty string)</li>
     *   <li>Pagination parameters handled by @Qualifier("scheduler") Pageable (page, size, sort)</li>
     * </ul>
     *
     * @param pageable Spring Data Pageable object qualified as "scheduler" for pagination and sorting configuration
     * @param search   Optional search term for filtering schedulers by name; defaults to empty string if not provided
     * @return ModelAndView containing the "scheduler-all" view fragment with model attribute "schedulerPage"
     *         populated by the Flow pipeline execution
     * @see AbstractSchedulerController#findSchedulersFlow(String, org.springframework.data.jpa.domain.Specification, Pageable)
     */
    @PreAuthorize(CHECK_CAN_READ_BACKEND)
    @GetMapping(value = _ALL)
    public Object getAll(
            @Qualifier("scheduler") Pageable pageable,
            @RequestParam(required = false, defaultValue = "", name = "scheduler_search") String search) {
        debug("[getAll] search {}", search);
        return findSchedulersFlow(search, null, pageable)
                .mav(SCHEDULER + "-" + ALL);
    }

    /**
     * Displays the settings page for an existing scheduler with populated configuration form.
     * <p>
     * This GET endpoint retrieves a scheduler entity by ID and prepares a SchedulerForm populated with
     * its current configuration including name, cron expression, event code, and enabled state. The form
     * is rendered in the "scheduler-settings" view fragment for editing.
     * </p>
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_READ_BACKEND} privilege via @PreAuthorize annotation.
     * </p>
     * <p>
     * <b>Request Mapping:</b> GET {@code _HTML + _SCHEDULER + _ID + _SETTINGS}
     * </p>
     * <p>
     * <b>Flow Delegation:</b> Delegates to {@link AbstractSchedulerController#find(Long)} which retrieves
     * the Scheduler entity via repositories.unsecure.scheduler.findOne and initializes SchedulerForm with
     * the entity's current values.
     * </p>
     *
     * @param schedulerId Database primary key identifier of the Scheduler entity to retrieve
     * @return ModelAndView containing the "scheduler-settings" view fragment with model attributes
     *         "schedulerEntity" (Scheduler) and "schedulerForm" (SchedulerForm) populated by the Flow pipeline
     * @see AbstractSchedulerController#find(Long)
     * @see com.openkoda.model.component.Scheduler
     * @see com.openkoda.form.SchedulerForm
     */
    @PreAuthorize(CHECK_CAN_READ_BACKEND)
    @GetMapping(value = _ID + _SETTINGS)
    public Object settings(@PathVariable(ID) Long schedulerId) {
        debug("[settings] schedulerId {}", schedulerId);
        return find(schedulerId)
                .mav(SCHEDULER + "-settings");
    }

    /**
     * Displays the scheduler creation page with an empty configuration form for defining a new scheduled task.
     * <p>
     * This GET endpoint initializes a new SchedulerForm by calling {@link AbstractSchedulerController#find(Long)}
     * with ID=-1 as a convention for creating new entities. The empty form is rendered in the "scheduler-settings"
     * view fragment, allowing administrators to configure scheduler name, cron expression, event code to execute,
     * and enabled state.
     * </p>
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_MANAGE_BACKEND} privilege via @PreAuthorize annotation,
     * as this endpoint initiates the creation workflow.
     * </p>
     * <p>
     * <b>Request Mapping:</b> GET {@code _HTML + _SCHEDULER + _NEW_SETTINGS}
     * </p>
     * <p>
     * <b>Form Initialization:</b> The SchedulerForm is initialized with default values suitable for a new
     * scheduler. After form submission, the scheduler will be registered cluster-wide via
     * services.scheduler.loadClusterAware.
     * </p>
     *
     * @return ModelAndView containing the "scheduler-settings" view fragment with model attribute
     *         "schedulerForm" (empty SchedulerForm) ready for user input
     * @see AbstractSchedulerController#find(Long)
     * @see com.openkoda.form.SchedulerForm
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @GetMapping(_NEW_SETTINGS)
    public Object newScheduler() {
        debug("[newScheduler]");
        return find(-1L)
                .mav(SCHEDULER + "-settings");
    }

    /**
     * Updates an existing scheduler configuration with validated form data and reloads it cluster-wide.
     * <p>
     * This POST endpoint processes scheduler configuration updates submitted from the settings page. It performs
     * Jakarta Bean Validation on the SchedulerForm, updates the Scheduler entity in the database, exports the
     * component definition for version control, and triggers cluster-aware scheduler reload via
     * services.scheduler.reloadClusterAware to ensure all application instances reflect the updated configuration.
     * </p>
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_MANAGE_BACKEND} privilege via @PreAuthorize annotation.
     * </p>
     * <p>
     * <b>Request Mapping:</b> POST {@code _HTML + _SCHEDULER + _ID + _SETTINGS}
     * </p>
     * <p>
     * <b>Validation and Processing:</b>
     * </p>
     * <ul>
     *   <li>Form is validated using Jakarta Bean Validation (@Valid annotation)</li>
     *   <li>Cron expression syntax is validated by Spring's CronExpression parser</li>
     *   <li>On success: Entity is persisted via repositories.unsecure.scheduler.saveAndFlush, component exported,
     *       scheduler reloaded cluster-wide, and "scheduler-settings-form-success" fragment is returned</li>
     *   <li>On validation failure: BindingResult errors are populated and "scheduler-settings-form-error"
     *       fragment is returned with error messages</li>
     * </ul>
     * <p>
     * <b>Cluster Synchronization:</b> The services.scheduler.reloadClusterAware operation ensures that the updated
     * scheduler configuration is reloaded on all nodes in the application cluster, maintaining consistency.
     * </p>
     *
     * @param schedulerId   Database primary key identifier of the Scheduler entity to update
     * @param schedulerForm SchedulerForm object populated from form submission, validated with @Valid annotation
     * @param br            BindingResult containing validation errors if any; checked in AbstractSchedulerController.update
     * @return ModelAndView with conditional view fragments: "entity-forms::scheduler-settings-form-success" on
     *         successful update, or "entity-forms::scheduler-settings-form-error" when validation fails
     * @see AbstractSchedulerController#update(Long, SchedulerForm, BindingResult)
     * @see com.openkoda.form.SchedulerForm
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @PostMapping(value = _ID + _SETTINGS)
    public Object updateScheduler(@PathVariable(ID) Long schedulerId, @Valid SchedulerForm schedulerForm, BindingResult br) {
        debug("[updateScheduler] schedulerId {}", schedulerId);
        return update(schedulerId, schedulerForm, br)
                .mav(ENTITY + '-' + FORMS + "::scheduler-settings-form-success",
                        ENTITY + '-' + FORMS + "::scheduler-settings-form-error");
    }

    /**
     * Creates a new scheduler from validated form data and registers it cluster-wide for execution.
     * <p>
     * This POST endpoint processes new scheduler creation submitted from the creation form. It performs
     * Jakarta Bean Validation on the SchedulerForm, creates a new Scheduler entity in the database,
     * exports the component definition for version control, and registers the scheduler cluster-wide via
     * services.scheduler.loadClusterAware to ensure all application instances begin executing the scheduled task.
     * </p>
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_MANAGE_BACKEND} privilege via @PreAuthorize annotation.
     * </p>
     * <p>
     * <b>Request Mapping:</b> POST {@code _HTML + _SCHEDULER + _NEW_SETTINGS}
     * </p>
     * <p>
     * <b>Validation and Processing:</b>
     * </p>
     * <ul>
     *   <li>Form is validated using Jakarta Bean Validation (@Valid annotation)</li>
     *   <li>Cron expression syntax is validated by Spring's CronExpression parser</li>
     *   <li>Scheduler name must be unique (validated in form)</li>
     *   <li>On success: Entity is persisted via repositories.unsecure.scheduler.saveAndFlush, component exported,
     *       scheduler registered cluster-wide, form is reset, and "scheduler-settings-form-success" fragment returned</li>
     *   <li>On validation failure: BindingResult errors are populated and "scheduler-settings-form-error"
     *       fragment is returned with error messages</li>
     * </ul>
     * <p>
     * <b>Cluster Registration:</b> The services.scheduler.loadClusterAware operation registers the new scheduler
     * on all nodes in the application cluster, ensuring distributed execution according to the cron schedule.
     * </p>
     *
     * @param schedulerForm SchedulerForm object populated from form submission with name, cron expression, event code,
     *                      and enabled state; validated with @Valid annotation
     * @param br            BindingResult containing validation errors if any; checked in AbstractSchedulerController.create
     * @return ModelAndView with conditional view fragments: "entity-forms::scheduler-settings-form-success" on
     *         successful creation, or "entity-forms::scheduler-settings-form-error" when validation fails
     * @see AbstractSchedulerController#create(SchedulerForm, BindingResult)
     * @see com.openkoda.form.SchedulerForm
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @PostMapping(_NEW_SETTINGS)
    public Object createScheduler(@Valid SchedulerForm schedulerForm, BindingResult br) {
        debug("[createScheduler]");
        return create(schedulerForm, br)
                .mav(ENTITY + '-' + FORMS + "::scheduler-settings-form-success",
                        ENTITY + '-' + FORMS + "::scheduler-settings-form-error");
    }

    /**
     * Deletes a scheduler from the database and unregisters it from cluster-wide execution.
     * <p>
     * This POST endpoint removes a scheduler entity, cleaning up its exported component files, deleting the
     * database record, and unregistering the scheduler from all application instances via
     * services.scheduler.removeClusterAware to stop scheduled task execution across the cluster.
     * </p>
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_MANAGE_BACKEND} privilege via @PreAuthorize annotation.
     * </p>
     * <p>
     * <b>Request Mapping:</b> POST {@code _HTML + _SCHEDULER + _ID_REMOVE}
     * </p>
     * <p>
     * <b>Deletion Process:</b>
     * </p>
     * <ul>
     *   <li>Scheduler entity is retrieved via repositories.unsecure.scheduler.findOne</li>
     *   <li>Exported component files are removed via services.componentExport.removeExportedFilesIfRequired</li>
     *   <li>Database record is deleted via repositories.unsecure.scheduler.deleteOne</li>
     *   <li>Scheduler is unregistered cluster-wide via services.scheduler.removeClusterAware</li>
     * </ul>
     * <p>
     * <b>Response Handling:</b> Uses boolean lambda-based view selection via {@code .mav(a -> true, a -> false)}
     * to return success (true) or failure (false) indicators for AJAX response handling in the frontend.
     * </p>
     * <p>
     * <b>Cluster Synchronization:</b> The services.scheduler.removeClusterAware operation ensures that the
     * scheduler is unregistered and stopped on all nodes in the application cluster.
     * </p>
     *
     * @param schedulerId Database primary key identifier of the Scheduler entity to delete
     * @return ModelAndView with boolean result: success view on successful deletion, failure view otherwise
     * @see AbstractSchedulerController#removeScheduler(Long)
     * @see com.openkoda.model.component.Scheduler
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @PostMapping(value = _ID_REMOVE)
    public Object remove(@PathVariable(ID) Long schedulerId) {
        debug("[remove] schedulerId {}", schedulerId);
        return removeScheduler(schedulerId)
                .mav(a -> true, a -> false);
    }

}
