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

import com.openkoda.core.repository.common.FunctionalRepositoryWithLongId;
import com.openkoda.model.task.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * Generic base repository managing Task entities for background processing queue.
 * <p>
 * Provides queries by task type, status, priority, and scheduled execution. Used by JobsScheduler
 * for asynchronous task execution. Concrete implementations include EmailRepository and
 * HttpRequestTaskRepository.
 * 
 * <p>
 * The {@code @NoRepositoryBean} annotation prevents Spring Data from creating a repository bean
 * for this abstract interface, allowing only concrete subtype repositories to be instantiated.
 * 
 *
 * @param <T> Task subtype (Email, HttpRequestTask, or other Task derivatives)
 * @author OpenKoda Team
 * @since 1.7.1
 * @see FunctionalRepositoryWithLongId
 * @see Task
 * @see com.openkoda.model.task.Email
 * @see com.openkoda.model.task.HttpRequestTask
 * @see com.openkoda.repository.task.EmailRepository
 * @see com.openkoda.repository.task.HttpRequestTaskRepository
 */
@NoRepositoryBean
public interface TaskRepository<T extends Task> extends FunctionalRepositoryWithLongId<T> {

    /**
     * Pageable preset for selecting the single oldest task sorted by startAfter timestamp ascending.
     * <p>
     * Used for sequential task processing.
     * 
     */
    Pageable OLDEST_1 = PageRequest.of(0, 1, Sort.Direction.ASC, "startAfter");

    /**
     * Pageable preset for selecting 10 oldest tasks sorted by startAfter timestamp ascending.
     * <p>
     * Commonly used batch size for background job processing.
     * 
     */
    Pageable OLDEST_10 = PageRequest.of(0, 10, Sort.Direction.ASC, "startAfter");

    /**
     * Pageable preset for selecting 100 oldest tasks sorted by startAfter timestamp ascending.
     * <p>
     * Used for bulk task processing operations.
     * 
     */
    Pageable OLDEST_100 = PageRequest.of(0, 100, Sort.Direction.ASC, "startAfter");

    /**
     * Bulk updates task state for the provided list of tasks using JPQL.
     * <p>
     * This JPQL bulk update bypasses entity lifecycle callbacks and may desynchronize managed
     * entity state across persistence contexts. The {@code updatedOn} field is automatically
     * set to {@code CURRENT_TIMESTAMP} for audit trail purposes.
     * 
     * <p>
     * The {@code @Modifying} annotation indicates this query modifies database state and requires
     * transaction context. Ensure this method is called within an active transaction.
     * 
     *
     * @param tasks List of Task entities to update with new state
     * @param taskState Target state to apply (typically {@code Task.TaskState.DOING})
     * @return Number of rows affected by the bulk update operation
     * @see Task.TaskState
     * @see org.springframework.data.jpa.repository.Modifying
     */
    @Modifying
    @Query(value = "UPDATE Task t SET t.state = :state, t.updatedOn = CURRENT_TIMESTAMP where t in :tasks")
    int setDoingState(@Param("tasks") List<Task> tasks, @Param("state") Task.TaskState taskState);

    /**
     * Finds tasks and atomically sets their status to {@code DOING} to prevent concurrent execution.
     * <p>
     * This method executes in a new transaction ({@code REQUIRES_NEW}) to ensure atomic claim across
     * clustered nodes. The supplied query MUST acquire a {@code PESSIMISTIC_WRITE} lock to prevent
     * two or more nodes from processing the same tasks simultaneously.
     * 
     * <p>
     * Usage example:
     * <pre>{@code
     * Page<Email> tasks = emailRepository.findTasksAndSetStateDoing(
     *     () -> emailRepository.findByCanBeStartedTrue(OLDEST_10)
     * );
     * }</pre>
     * 
     * <p>
     * This pattern provides concurrency control for distributed background task processing,
     * ensuring each task is executed exactly once across multiple application instances.
     * 
     *
     * @param query Supplier providing a Page of tasks with {@code PESSIMISTIC_WRITE} lock
     *              (e.g., {@code () -> findByCanBeStartedTrue(OLDEST_10)})
     * @return Page of tasks with state atomically set to {@code DOING} within a new transaction
     * @see Task.TaskState#DOING
     * @see org.springframework.transaction.annotation.Propagation#REQUIRES_NEW
     */
    @Transactional(propagation = REQUIRES_NEW)
    default Page<T> findTasksAndSetStateDoing(Supplier<Page<T>> query) {
        Page<T> result = query.get();
        if (result.getNumberOfElements() > 0) {
            setDoingState((List<Task>) result.getContent(), Task.TaskState.DOING);
        }
        return result;
    }

}
