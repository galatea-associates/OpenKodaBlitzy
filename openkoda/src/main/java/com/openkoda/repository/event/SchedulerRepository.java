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

package com.openkoda.repository.event;

import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.model.common.ModelConstants;
import com.openkoda.model.component.Scheduler;
import com.openkoda.repository.ComponentEntityRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for managing {@link Scheduler} entities that define cron-based job scheduling configurations.
 * <p>
 * This repository provides data access operations for scheduler records that configure background task execution
 * and scheduled event emissions. The {@link Scheduler} entity stores cron expressions, execution timing, and
 * activation status for jobs managed by the {@code JobsScheduler} framework.
 * </p>
 * <p>
 * This interface inherits comprehensive data access capabilities from {@link UnsecuredFunctionalRepositoryWithLongId},
 * including standard CRUD operations, pagination, sorting, and advanced search functionality. The repository operates
 * without automatic privilege enforcement, allowing direct access to scheduler configurations for administrative purposes.
 * For privilege-checked operations, use {@code SecureSchedulerRepository} instead.
 * </p>
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Manage scheduler lifecycle (create, update, delete scheduler configurations)</li>
 *   <li>Query schedulers by cron expression, next execution time, and active status</li>
 *   <li>Support pagination and sorting for scheduler listings</li>
 *   <li>Enable search functionality across scheduler attributes</li>
 *   <li>Provide component entity operations for scheduler import/export</li>
 * </ul>
 *
 * <h3>Inherited Capabilities</h3>
 * From {@link UnsecuredFunctionalRepositoryWithLongId}:
 * <ul>
 *   <li>Standard CRUD operations: {@code save()}, {@code findById()}, {@code findAll()}, {@code delete()}</li>
 *   <li>Pagination support: {@code findAll(Pageable)}</li>
 *   <li>Sorting support: {@code findAll(Sort)}</li>
 *   <li>Search operations: {@code search(String, Pageable)}</li>
 *   <li>Batch operations: {@code saveAll()}, {@code deleteAll()}</li>
 * </ul>
 *
 * <h3>Typical Usage Patterns</h3>
 * <pre>
 * // Create new scheduler
 * Scheduler scheduler = new Scheduler();
 * scheduler.setCronExpression("0 0 * * *");
 * schedulerRepository.save(scheduler);
 *
 * // Query active schedulers
 * List&lt;Scheduler&gt; active = schedulerRepository.findAll();
 * </pre>
 *
 * <p>
 * <strong>Integration:</strong> This repository is used by {@code JobsScheduler} to load scheduler configurations
 * for background task execution. Schedulers define when and how often jobs should run based on cron expressions.
 * </p>
 *
 * <p>
 * <strong>Thread Safety:</strong> As a Spring Data JPA repository, this interface is thread-safe and can be
 * safely injected and used across multiple threads within the Spring application context.
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @since 2019-03-20
 * @see Scheduler
 * @see UnsecuredFunctionalRepositoryWithLongId
 * @see ComponentEntityRepository
 * @see com.openkoda.core.job.JobsScheduler
 */
@Repository
public interface SchedulerRepository extends UnsecuredFunctionalRepositoryWithLongId<Scheduler>, ModelConstants, ComponentEntityRepository<Scheduler> {

}
