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

package com.openkoda.repository.task;

import com.openkoda.model.task.HttpRequestTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

/**
 * Repository managing HttpRequestTask entities for asynchronous HTTP request execution.
 * <p>
 * This repository provides specialized query methods for managing HTTP-based tasks including
 * webhook delivery, external API integrations, and scheduled HTTP operations. It extends
 * TaskRepository to inherit standard task lifecycle operations while adding HTTP-specific
 * lookups by URL patterns, request methods, execution status, and retry counts.
 * </p>
 * <p>
 * Key features include pessimistic locking support for distributed task execution and
 * URL pattern matching for administrative monitoring of webhook delivery pipelines.
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @since 2019-07-02
 * @see TaskRepository
 * @see HttpRequestTask
 * @see com.openkoda.model.task.Task
 */
public interface HttpRequestTaskRepository extends TaskRepository<HttpRequestTask> {

    /**
     * Finds all HTTP request tasks that are ready to start and match the specified URL pattern.
     * <p>
     * This method is used for administrative inspection and webhook monitoring to locate
     * pending HTTP requests targeting specific endpoints. The URL pattern matching uses
     * database-specific LIKE semantics, so the pattern should include wildcards (%) as needed.
     * </p>
     * <p>
     * Typical use cases include debugging webhook deliveries, monitoring integration status,
     * and filtering tasks by destination service or API endpoint.
     * </p>
     *
     * @param requestUrlPart URL pattern fragment for LIKE search to filter HTTP requests by
     *                       target endpoint (use % wildcards for pattern matching)
     * @return List of HttpRequestTask entities that can be started and match the URL pattern,
     *         or empty list if no matching tasks are found
     */
    List<HttpRequestTask> findByCanBeStartedTrueAndRequestUrlLike(String requestUrlPart);

    /**
     * Finds a page of HTTP request tasks that are ready to start, with pessimistic locking.
     * <p>
     * This method applies a PESSIMISTIC_WRITE lock to prevent concurrent task execution
     * across multiple application nodes or threads. The lock ensures that selected tasks
     * cannot be claimed by other processes until the current transaction completes.
     * </p>
     * <p>
     * <b>Transactional Requirements:</b> This method must be called within an active transaction
     * to maintain the pessimistic lock. The lock is released when the transaction commits or rolls back.
     * </p>
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * httpRequestTaskRepository.findTasksAndSetStateDoing(
     *     () -> httpRequestTaskRepository.findByCanBeStartedTrue(pageable)
     * );
     * </pre>
     * </p>
     * <p>
     * Tasks eligible for execution include newly created tasks and previously failed tasks
     * that have not exceeded their maximum retry count. The canBeStarted flag automatically
     * accounts for retry delays and failure backoff periods.
     * </p>
     *
     * @param pageable Pagination parameters specifying page size and sort order for task
     *                 selection (typically sorted by creation date or priority)
     * @return Page of HttpRequestTask entities ready for execution with PESSIMISTIC_WRITE
     *         lock applied, or empty page if no tasks are available
     */
    @Lock(PESSIMISTIC_WRITE)
    Page<HttpRequestTask> findByCanBeStartedTrue(Pageable pageable);
}
