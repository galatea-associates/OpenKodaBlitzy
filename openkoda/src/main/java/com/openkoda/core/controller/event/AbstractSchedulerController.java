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

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.form.SchedulerForm;
import com.openkoda.model.component.Scheduler;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.BindingResult;

/**
 * Abstract controller providing centralized business logic for {@link Scheduler} entity lifecycle management
 * and scheduled task configuration in the OpenKoda platform.
 * <p>
 * This controller implements the template pattern where concrete subclasses (such as {@link SchedulerControllerHtml})
 * handle HTTP request binding, security annotations, and response formatting, while this abstract base provides
 * the core business logic for scheduler CRUD operations and cluster-aware task management.
 * 
 * <p>
 * Key responsibilities include:
 * <ul>
 *   <li>Paginated scheduler search and retrieval using Spring Data Specification and Pageable</li>
 *   <li>Scheduler entity creation with cron expression validation and database persistence</li>
 *   <li>Scheduler entity updates with form validation and change propagation</li>
 *   <li>Scheduler deletion with cleanup of exported component files</li>
 *   <li>Cluster-aware scheduler operations ensuring task consistency across distributed application instances</li>
 *   <li>Component export functionality for version control and deployment of scheduler configurations</li>
 * </ul>
 * <p>
 * This controller leverages the Flow pipeline architecture for composable request handling, providing a functional
 * programming style with {@link Flow#init()}, {@code thenSet()}, {@code then()}, and {@code execute()} operations
 * that build a {@link PageModelMap} containing model attributes for view rendering.
 * 
 * <p>
 * Integration points:
 * <ul>
 *   <li>Extends {@link ComponentProvider} for access to repositories and services via dependency injection</li>
 *   <li>Implements {@link HasSecurityRules} for privilege-based access control enforcement</li>
 *   <li>Uses secure repositories ({@code repositories.secure.scheduler}) for read operations with privilege checks</li>
 *   <li>Uses unsecure repositories ({@code repositories.unsecure.scheduler}) for write operations after validation</li>
 *   <li>Delegates to {@code services.validation} for Jakarta Bean Validation and form population</li>
 *   <li>Delegates to {@code services.componentExport} for file-based export and cleanup operations</li>
 *   <li>Delegates to {@code services.scheduler} for cluster-aware scheduler lifecycle management</li>
 * </ul>
 * <p>
 * Cluster-aware operations utilize {@link com.openkoda.core.service.event.ClusterEventSenderService} to ensure
 * that scheduler registration, updates, and removals are propagated across all application instances in a
 * distributed environment, maintaining consistency for scheduled task execution.
 * 
 * <p>
 * Typical usage pattern:
 * <pre>
 * &#64;RestController
 * public class SchedulerControllerHtml extends AbstractSchedulerController {
 *     &#64;PostMapping("/create")
 *     public ModelAndView createScheduler(&#64;Valid SchedulerForm form, BindingResult br) {
 *         return create(form, br).mav("scheduler-settings");
 *     }
 * }
 * </pre>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2019-03-20
 * @see SchedulerControllerHtml
 * @see com.openkoda.core.service.event.ClusterEventSenderService
 * @see com.openkoda.model.component.Scheduler
 * @see com.openkoda.form.SchedulerForm
 */
public class AbstractSchedulerController extends ComponentProvider implements HasSecurityRules {

    /**
     * Retrieves a paginated page of {@link Scheduler} entities from the database matching the provided search criteria.
     * <p>
     * This method constructs a Flow pipeline that queries the secure scheduler repository using Spring Data JPA
     * Specification and Pageable parameters. The secure repository enforces privilege checks to ensure the current
     * user has appropriate read access to scheduler data.
     * 
     * <p>
     * The Flow pipeline pattern used here follows:
     * <pre>
     * Flow.init()
     *     .thenSet(schedulerPage, result -&gt; query operation)
     *     .execute()
     * </pre>
     * This returns a {@link PageModelMap} containing the "schedulerPage" model attribute for view rendering.
     * 
     *
     * @param schedulerSearchTerm search string for name-based filtering of schedulers; supports partial matching
     *                           and is applied to the scheduler name field; may be null or empty for unfiltered results
     * @param schedulerSpecification JPA {@link Specification} for advanced query construction enabling complex
     *                               filtering conditions beyond simple text search; may be null for no additional filtering
     * @param schedulerPageable {@link Pageable} containing pagination parameters (page number, page size) and
     *                         sorting directives (sort field, sort direction) for result set control
     * @return {@link PageModelMap} containing the Flow execution result with "schedulerPage" model attribute
     *         holding a {@link org.springframework.data.domain.Page} of {@link Scheduler} entities matching
     *         the search criteria
     */
    protected PageModelMap findSchedulersFlow(
            String schedulerSearchTerm,
            Specification<Scheduler> schedulerSpecification,
            Pageable schedulerPageable) {
        debug("[findSchedulersFlow] search {}", schedulerSearchTerm);
        return Flow.init()
                .thenSet(schedulerPage, a -> repositories.secure.scheduler.search(schedulerSearchTerm, null, schedulerSpecification, schedulerPageable))
                .execute();
    }

    /**
     * Retrieves a single {@link Scheduler} entity from the database by primary key and prepares a bound
     * {@link SchedulerForm} for editing in the administrative UI.
     * <p>
     * This method constructs a Flow pipeline that first fetches the scheduler entity using the secure repository
     * (enforcing privilege checks), then initializes a SchedulerForm pre-populated with the entity's current data.
     * The form is ready for display in update/edit views.
     * 
     * <p>
     * Flow pipeline composition:
     * <pre>
     * Flow.init()
     *     .thenSet(schedulerEntity, -&gt; find operation)
     *     .thenSet(schedulerForm, -&gt; new SchedulerForm with entity)
     *     .execute()
     * </pre>
     * 
     *
     * @param schedulerId database primary key identifier ({@code id} column) of the {@link Scheduler} entity to retrieve;
     *                   must correspond to an existing scheduler record or the secure repository will return empty result
     * @return {@link PageModelMap} containing the Flow execution result with two model attributes: "schedulerEntity"
     *         holding the retrieved {@link Scheduler} entity, and "schedulerForm" holding the initialized
     *         {@link SchedulerForm} bound to the entity for editing
     */
    protected PageModelMap find(long schedulerId) {
        debug("[find] schedulerId {}", schedulerId);
        return Flow.init()
                .thenSet(schedulerEntity, a -> repositories.secure.scheduler.findOne(schedulerId))
                .thenSet(schedulerForm, a -> new SchedulerForm(null, a.result))
                .execute();
    }


    /**
     * Validates the provided scheduler configuration data and creates a new {@link Scheduler} entity with
     * cluster-aware registration in the running application.
     * <p>
     * This method orchestrates the complete scheduler creation workflow through a Flow pipeline:
     * <ol>
     *   <li>Form validation: Validates {@link SchedulerForm} data using Jakarta Bean Validation constraints,
     *       checking cron expression syntax, required fields, and business rules</li>
     *   <li>Entity population: Populates a new {@link Scheduler} entity from validated form data via
     *       {@code services.validation.validateAndPopulateToEntity()}</li>
     *   <li>Database persistence: Saves the new scheduler entity using {@code repositories.unsecure.scheduler.saveAndFlush()}
     *       to ensure immediate database commit and ID generation</li>
     *   <li>Component export: Optionally exports the scheduler configuration to file via
     *       {@code services.componentExport.exportToFileIfRequired()} for version control and deployment</li>
     *   <li>Cluster-aware registration: Loads the scheduler into the runtime task scheduling system across all
     *       application instances via {@code services.scheduler.loadClusterAware()}, utilizing
     *       {@link com.openkoda.core.service.event.ClusterEventSenderService} for distributed coordination</li>
     *   <li>Form reset: Initializes a fresh empty {@link SchedulerForm} for potential next creation operation</li>
     * </ol>
     * 
     * <p>
     * Validation errors are recorded in the {@link BindingResult} and will prevent entity creation. The calling
     * controller should check {@code br.hasErrors()} to determine success or failure for appropriate view rendering.
     * 
     *
     * @param schedulerFormData validated {@link SchedulerForm} containing scheduler configuration including name,
     *                         cron expression (validated against cron syntax rules), event code to execute, and
     *                         enabled/disabled state; form must pass Jakarta Bean Validation constraints
     * @param br {@link BindingResult} for capturing Jakarta Bean Validation errors during form processing;
     *          populated with field errors if validation fails, preventing entity creation
     * @return {@link PageModelMap} containing the Flow execution result with model attributes: "schedulerForm"
     *         initially holding the input form data, then replaced with a fresh empty form upon successful creation,
     *         and "schedulerEntity" holding the persisted {@link Scheduler} entity with generated ID
     * @see com.openkoda.core.service.event.ClusterEventSenderService
     */
    protected PageModelMap create(SchedulerForm schedulerFormData, BindingResult br) {
        debug("[createScheduler]");
        return Flow.init(schedulerForm, schedulerFormData)
                .then(a -> services.validation.validateAndPopulateToEntity(schedulerFormData, br,new Scheduler()))
                .thenSet(schedulerEntity, a -> repositories.unsecure.scheduler.saveAndFlush(a.result))
                .then(a -> services.componentExport.exportToFileIfRequired(a.result))
                .then(a -> services.scheduler.loadClusterAware(a.result.getId()))
                .thenSet(schedulerForm, a -> new SchedulerForm())
                .execute();
    }

    /**
     * Updates an existing {@link Scheduler} entity with validated form data and triggers cluster-aware reload
     * of the modified scheduler configuration across all application instances.
     * <p>
     * This method orchestrates the complete scheduler update workflow through a Flow pipeline:
     * <ol>
     *   <li>Form initialization: Sets the {@link SchedulerForm} in the Flow context for model attribute access</li>
     *   <li>Entity retrieval: Fetches the existing {@link Scheduler} entity by ID using the unsecure repository
     *       (privilege checks performed at controller layer)</li>
     *   <li>Form validation and population: Validates the updated form data using Jakarta Bean Validation and
     *       populates changes to the retrieved entity via {@code services.validation.validateAndPopulateToEntity()}</li>
     *   <li>Database update: Persists the modified entity using {@code repositories.unsecure.scheduler.saveAndFlush()}
     *       to ensure immediate database commit</li>
     *   <li>Component export: Exports the updated scheduler configuration to file via
     *       {@code services.componentExport.exportToFileIfRequired()} for version control tracking</li>
     *   <li>Cluster-aware reload: Reloads the updated scheduler in the runtime scheduling system across all
     *       application instances via {@code services.scheduler.reloadClusterAware()}, utilizing
     *       {@link com.openkoda.core.service.event.ClusterEventSenderService} to propagate changes</li>
     * </ol>
     * 
     * <p>
     * Validation errors are recorded in the {@link BindingResult}. If validation fails, the entity is not updated
     * and the calling controller should render an error view based on {@code br.hasErrors()}.
     * 
     *
     * @param schedulerId database primary key identifier of the {@link Scheduler} entity to update; must correspond
     *                   to an existing scheduler record or the Flow will fail
     * @param schedulerFormData validated {@link SchedulerForm} containing updated scheduler configuration including
     *                         modified cron expression (validated for syntax correctness), event code, name, and
     *                         enabled state; form must pass Jakarta Bean Validation constraints
     * @param br {@link BindingResult} for capturing Jakarta Bean Validation errors during form processing;
     *          populated with field errors if validation fails, preventing entity update
     * @return {@link PageModelMap} containing the Flow execution result with "schedulerForm" model attribute
     *         holding the input form data for potential re-display on validation errors
     * @see com.openkoda.core.service.event.ClusterEventSenderService
     */
    protected PageModelMap update(long schedulerId, SchedulerForm schedulerFormData, BindingResult br) {
        debug("[updateScheduler] schedulerId: {}", schedulerId);
        return Flow.init().init(schedulerForm, schedulerFormData)
                .then(a -> repositories.unsecure.scheduler.findOne(schedulerId))
                .then(a -> services.validation.validateAndPopulateToEntity(schedulerFormData, br,a.result))
                .then(a -> repositories.unsecure.scheduler.saveAndFlush(a.result))
                .then(a -> services.componentExport.exportToFileIfRequired(a.result))
                .then(a -> services.scheduler.reloadClusterAware(a.result.getId()))
                .execute();
    }

    /**
     * Deletes a {@link Scheduler} entity from the database and removes it from the runtime scheduling system
     * across all application instances in a cluster-aware manner.
     * <p>
     * This method orchestrates the complete scheduler deletion workflow through a Flow pipeline:
     * <ol>
     *   <li>Entity retrieval: Fetches the {@link Scheduler} entity by ID using the secure repository to verify
     *       existence and enforce privilege checks</li>
     *   <li>Export cleanup: Removes any exported component files associated with the scheduler via
     *       {@code services.componentExport.removeExportedFilesIfRequired()}, ensuring cleanup of version
     *       control artifacts</li>
     *   <li>Database deletion: Permanently deletes the scheduler record using
     *       {@code repositories.unsecure.scheduler.deleteOne()}</li>
     *   <li>Cluster-aware removal: Unregisters the scheduler from the runtime task scheduling system across all
     *       application instances via {@code services.scheduler.removeClusterAware()}, utilizing
     *       {@link com.openkoda.core.service.event.ClusterEventSenderService} to propagate the removal operation</li>
     * </ol>
     * 
     * <p>
     * This operation is irreversible and immediately stops the scheduler's execution across the entire distributed
     * application. Any running tasks triggered by this scheduler will complete their current execution but will not
     * be scheduled again.
     * 
     *
     * @param schedulerId database primary key identifier of the {@link Scheduler} entity to delete; must correspond
     *                   to an existing scheduler record or the secure repository retrieval will fail
     * @return {@link PageModelMap} containing the Flow execution result; typically used by the calling controller
     *         to determine success/failure for appropriate view rendering
     * @see com.openkoda.core.service.event.ClusterEventSenderService
     */
    protected PageModelMap removeScheduler(long schedulerId) {
        debug("[removeScheduler] schedulerId: {}", schedulerId);
        return Flow.init(schedulerId)
                .then(a -> repositories.secure.scheduler.findOne(schedulerId))
                .then(a -> services.componentExport.removeExportedFilesIfRequired(a.result))
                .then(a -> repositories.unsecure.scheduler.deleteOne(schedulerId))
                .then(a -> services.scheduler.removeClusterAware(schedulerId))
                .execute();
    }
}
