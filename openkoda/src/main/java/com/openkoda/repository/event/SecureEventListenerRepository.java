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
import com.openkoda.model.component.event.EventListenerEntry;
import com.openkoda.repository.SecureRepository;
import org.springframework.stereotype.Repository;

import static com.openkoda.controller.common.URLConstants.EVENTLISTENER;

/**
 * Secure repository interface for managing {@link EventListenerEntry} entities with privilege-based access control.
 * <p>
 * This repository extends the {@link com.openkoda.repository.SecureRepository} pattern to enforce privilege checks on all operations
 * involving event listener registrations. It provides a secure facade for accessing event listener entries
 * used in the dynamic event-driven workflow management system.
 * 
 * <p>
 * The repository is annotated with {@link SearchableRepositoryMetadata} to enable full-text search capabilities
 * and custom display formatting for event listener entries. The metadata configuration includes:
 * 
 * <ul>
 *   <li><b>descriptionFormula</b>: SQL formula for displaying event listener entries in the UI. The formula
 *       concatenates event details (name, object type), consumer information (class name, method name, parameter type),
 *       and static data parameters (static_data_1 through static_data_4). It uses PostgreSQL string functions like
 *       {@code substring} with regex patterns to extract simple class names and {@code COALESCE} to handle nullable fields.</li>
 *   <li><b>searchIndexFormula</b>: SQL formula for building the full-text search index. The formula creates a searchable
 *       text by concatenating event class name, event name, object type, consumer identifiers, organization scope
 *       (via 'orgid:' token), and the reference formula from {@link EventListenerEntry#REFERENCE_FORMULA}. All text
 *       is lowercased for case-insensitive search.</li>
 * </ul>
 * <p>
 * This repository is used in the event-driven workflow system to manage dynamic listener registrations.
 * Event listeners are registered at runtime to respond to application events such as entity lifecycle events,
 * custom business events, or integration events. The secure repository pattern ensures that only users with
 * appropriate privileges can view, modify, or delete event listener configurations.
 * 
 * <p>
 * For unsecured operations (e.g., internal system use without privilege checks), use {@link EventListenerRepository} instead.
 * 
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @since 2019-03-11
 * @see EventListenerEntry
 * @see SecureRepository
 * @see EventListenerRepository
 * @see SearchableRepositoryMetadata
 */
@Repository
@SearchableRepositoryMetadata(
        entityKey = EVENTLISTENER,
        descriptionFormula = "('Event: ' || event_name || '(' || substring(event_object_type from '[^.]+$') || " +
                "'), Consumer: ' || substring(consumer_class_name from '[^.]+$') ||'::' || consumer_method_name || '(' || substring(consumer_parameter_class_name from '[^.]+$')" +
                "|| COALESCE( ', \"' || static_data_1 || '\"', '') || COALESCE( ', \"' || static_data_2 || '\"', '') || COALESCE( ', \"' || static_data_3 || '\"', '')  || COALESCE( ', \"' || static_data_4 || '\"', '')  ||  ')')",
        entityClass = EventListenerEntry.class,
        searchIndexFormula = "lower(event_class_name || ' ' || event_name || ' ' || event_object_type || ' ' || consumer_class_name || ' ' || consumer_method_name ||"
                + " ' orgid:' || COALESCE(CAST (organization_id as text), '') || ' ' ||"
                + EventListenerEntry.REFERENCE_FORMULA + ")"
)
public interface SecureEventListenerRepository extends SecureRepository<EventListenerEntry> {


}
