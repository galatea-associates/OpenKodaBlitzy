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

import com.openkoda.model.task.Email;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

/**
 * Spring Data JPA repository managing Email entities for outbound email queue.
 * <p>
 * Provides queries for pending emails by status, priority, and scheduled send time.
 * Integrates with EmailConfig for SMTP delivery. Used by notification services
 * for async email dispatch.
 * 
 * <p>
 * This repository extends TaskRepository to inherit task lifecycle operations including
 * status management, pessimistic locking for concurrent task execution prevention, and
 * organization-scoped queries for multi-tenant email operations.
 * 
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @since 1.7.1
 * @see com.openkoda.model.task.Email
 * @see TaskRepository
 * @see com.openkoda.model.task.Task
 */
@Repository
public interface EmailRepository extends TaskRepository<Email> {

    /**
     * Retrieves paginated Email tasks ready for execution with pessimistic write lock.
     * <p>
     * This method requires pessimistic lock to avoid concurrent task execution on two or more nodes.
     * The PESSIMISTIC_WRITE lock ensures database-level locking preventing multiple application instances
     * from processing the same email simultaneously.
     * 
     * <p>
     * In order to query and upgrade Task status in one shot use:
     * {@code emailRepository.findTasksAndSetStateDoing(() -> emailRepository.findByCanBeStartedTrue(pageable))}
     * 
     *
     * @param pageable pagination parameters specifying page size and sort order for task selection
     * @return page of Email tasks ready for execution with PESSIMISTIC_WRITE lock applied
     * @see TaskRepository#findTasksAndSetStateDoing
     */
    @Lock(PESSIMISTIC_WRITE)
    Page<Email> findByCanBeStartedTrue(Pageable pageable);

    /**
     * Finds Email entities by organization ID with subject text matching.
     * <p>
     * Performs organization-scoped search with partial subject matching using SQL LIKE semantics.
     * Case sensitivity depends on database collation settings. Used for administrative lookups
     * and UI-facing email search functionality.
     * 
     *
     * @param orgId organization ID to filter emails by tenant
     * @param title subject text fragment for LIKE search (case-sensitive based on DB collation)
     * @return list of Email entities matching organization and subject criteria
     */
    List<Email> findByOrganizationIdAndSubjectContaining(Long orgId, String title);
}
