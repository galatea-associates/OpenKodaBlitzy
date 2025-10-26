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

package com.openkoda.core.job;

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.repository.SearchableRepositories;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled background job that maintains full-text search indexes for all searchable entities.
 * <p>
 * This job is invoked by {@link com.openkoda.core.job.JobsScheduler} with fixed-delay scheduling
 * (initialDelay=10000ms, fixedDelay=10000ms), running every 10 seconds to ensure search indexes
 * remain current. It updates the search_index column for all searchable tables to enable
 * full-text search functionality across the application.
 * </p>
 * <p>
 * The job executes in two passes:
 * <ol>
 * <li>Updates search indexes for static entities (predefined JPA entities)</li>
 * <li>Updates search indexes for dynamic entities (runtime-generated entities)</li>
 * </ol>
 * Each pass executes native SQL UPDATE statements provided by {@link SearchableRepositories}.
 * </p>
 * <p>
 * This class implements {@link LoggingComponentWithRequestId} to enable request-id-aware tracing
 * for debugging and audit purposes.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see SearchableRepositories
 * @see EntityManager
 * @see com.openkoda.core.job.JobsScheduler
 */
@Component
public class SearchIndexUpdaterJob implements LoggingComponentWithRequestId {

    /**
     * JPA persistence context for executing native SQL UPDATE statements.
     * <p>
     * This EntityManager is injected by Spring via the {@link PersistenceContext} annotation
     * and provides the database operations needed to update search indexes using
     * database-specific full-text search features.
     * </p>
     */
    @PersistenceContext
    EntityManager entityManager;
    
    /**
     * Executes scheduled search index updates for all searchable entities in the database.
     * <p>
     * This method runs within a single transaction (via {@link Transactional} annotation),
     * ensuring that all SQL updates either succeed together or roll back together.
     * </p>
     * <p>
     * The update process follows two phases:
     * </p>
     * <ol>
     * <li><b>Static Entity Updates</b>: Iterates through SQL statements from
     * {@link SearchableRepositories#getSearchIndexUpdates()} to update search indexes
     * for predefined JPA entities (e.g., Organization, User, Role)</li>
     * <li><b>Dynamic Entity Updates</b>: Iterates through SQL statements from
     * {@link SearchableRepositories#getSearchIndexUpdatesForDynamicEntities()} to update
     * search indexes for runtime-generated dynamic entities</li>
     * </ol>
     * <p>
     * Native SQL is used because search index updates rely on database-specific full-text
     * search features (e.g., {@code to_tsvector} in PostgreSQL). Each SQL statement is
     * executed via {@code entityManager.createNativeQuery(sql).executeUpdate()}.
     * </p>
     * <p>
     * <b>Transaction Behavior</b>: If any update fails, the entire transaction rolls back
     * and no indexes are updated. This ensures consistency across all searchable tables.
     * </p>
     * <p>
     * This method is stateless and relies on {@link SearchableRepositories} to provide
     * the appropriate SQL statements for the current database schema.
     * </p>
     *
     * @see SearchableRepositories#getSearchIndexUpdates()
     * @see SearchableRepositories#getSearchIndexUpdatesForDynamicEntities()
     * @see EntityManager#createNativeQuery(String)
     */
    @Transactional
    public void updateSearchIndexes() {
        for (String s: SearchableRepositories.getSearchIndexUpdates()) {
            entityManager.createNativeQuery(s).executeUpdate();
        }
//        dynamic entities
        for (String s: SearchableRepositories.getSearchIndexUpdatesForDynamicEntities()) {
            entityManager.createNativeQuery(s).executeUpdate();
        }
    }

}
