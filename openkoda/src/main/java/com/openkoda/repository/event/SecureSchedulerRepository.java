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

import com.openkoda.model.common.SearchableRepositoryMetadata;
import com.openkoda.model.component.Scheduler;
import com.openkoda.repository.SecureRepository;
import org.springframework.stereotype.Repository;

import static com.openkoda.controller.common.URLConstants.SCHEDULER;

/**
 * Secure repository for managing {@link Scheduler} entities with privilege-based access control and full-text search capabilities.
 * <p>
 * This interface extends {@link SecureRepository} to provide privilege-enforced access to scheduler records that manage cron-based
 * job scheduling and scheduled event emissions. All repository operations automatically verify user privileges before granting access
 * to scheduler entities, ensuring secure multi-tenant data isolation.
 * </p>
 * <p>
 * The repository integrates with the OpenKoda job scheduling framework ({@code JobsScheduler}) to support background task execution,
 * recurring event emissions, and time-based workflow automation. Schedulers define cron expressions that determine when events
 * should be emitted into the event-driven processing pipeline.
 * </p>
 * <p>
 * <b>Search and Indexing</b>
 * <br>
 * The {@link SearchableRepositoryMetadata} annotation configures advanced search capabilities:
 * </p>
 * <ul>
 * <li><b>Description Formula:</b> Generates user-friendly display text combining the cron expression and event data:
 * {@code (''|| cron_expression || ', Event data = "' || event_data || '"')}. This formula creates readable scheduler
 * descriptions in search results and administrative interfaces.</li>
 * <li><b>Search Index Formula:</b> Builds full-text search tokens by concatenating lowercase cron expression, event data,
 * organization identifier (for tenant scoping), and the scheduler's reference formula:
 * {@code lower(cron_expression || ' ' || event_data || ' orgid:' || COALESCE(CAST(organization_id as text), '') || ' ' || Scheduler.REFERENCE_FORMULA)}.
 * This enables searching schedulers by cron pattern, event payload, and organization scope.</li>
 * </ul>
 * <p>
 * <b>Usage Patterns:</b>
 * </p>
 * <pre>
 * SecureSchedulerRepository repo = ...;
 * Optional&lt;Scheduler&gt; scheduler = repo.findOne(schedulerId); // privilege-checked
 * </pre>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @since 2019-03-20
 * @see Scheduler
 * @see SecureRepository
 * @see SchedulerRepository
 * @see SearchableRepositoryMetadata
 */
@Repository
@SearchableRepositoryMetadata(
        entityKey = SCHEDULER,
        descriptionFormula = "(''|| cron_expression || ', Event data = \"' || event_data || '\"')",
        entityClass = Scheduler.class,
        searchIndexFormula = "lower(cron_expression || ' ' || event_data || ' ' ||"
                + "' orgid:' || COALESCE(CAST (organization_id as text), '') || ' ' ||"
                + Scheduler.REFERENCE_FORMULA + ")"
)
public interface SecureSchedulerRepository extends SecureRepository<Scheduler> {


}
