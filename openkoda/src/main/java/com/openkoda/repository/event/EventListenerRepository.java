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
import com.openkoda.model.OpenkodaModule;
import com.openkoda.model.common.ModelConstants;
import com.openkoda.model.component.event.EventListenerEntry;
import com.openkoda.repository.ComponentEntityRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link EventListenerEntry} entities used in event-driven workflow management.
 * <p>
 * This repository provides data access operations for managing dynamic event listener registrations within
 * the OpenKoda platform. Event listeners are registered at runtime to handle application events and enable
 * extensible workflow automation. The repository supports discovery of existing listeners to prevent duplicates,
 * validation of listener configurations, and bulk cleanup operations during module uninstallation.
 * </p>
 * <p>
 * Event listeners are scoped to {@link OpenkodaModule} instances, allowing module-specific event handling
 * and isolation. When modules are removed or disabled, all associated event listeners are cleaned up via
 * bulk delete operations to maintain referential integrity.
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @since 2019-03-11
 * @see EventListenerEntry
 * @see OpenkodaModule
 * @see SecureEventListenerRepository
 * @see UnsecuredFunctionalRepositoryWithLongId
 */
@Repository
public interface EventListenerRepository extends UnsecuredFunctionalRepositoryWithLongId<EventListenerEntry>, ModelConstants, ComponentEntityRepository<EventListenerEntry> {

    /**
     * Finds an event listener entry by its event name and consumer method name.
     * <p>
     * This method is used for duplicate detection during event listener registration to prevent
     * multiple listeners from being registered for the same event-method combination. It also
     * supports validation workflows that need to verify listener existence before performing
     * updates or deletions.
     * </p>
     *
     * @param eventName the name of the event that triggers the listener (e.g., "UserCreated", "OrderSubmitted")
     * @param consumerMethodName the fully qualified method name that handles the event (e.g., "com.example.service.EventHandler.onUserCreated")
     * @return the matching EventListenerEntry if found, or null if no listener exists for the given event-method pair
     */
    EventListenerEntry findByEventNameAndConsumerMethodName(String eventName, String consumerMethodName);

    /**
     * Deletes all event listener entries associated with the specified module.
     * <p>
     * This bulk delete operation is typically invoked during module uninstallation or cleanup procedures
     * to remove all event listeners registered by the module. The operation executes as a single JPQL
     * DELETE statement for efficiency.
     * </p>
     * <p>
     * <b>Important:</b> This method requires an active transaction context due to the {@link Modifying}
     * annotation. The calling code must be annotated with {@code @Transactional} or executed within
     * an existing transaction. After execution, the persistence context should be cleared or synchronized
     * to avoid stale entity references, as bulk operations bypass the EntityManager cache.
     * </p>
     * <p>
     * <b>Referential Integrity:</b> Ensure that no active event processing references the listeners
     * being deleted. Deleting listeners while events are being processed may cause runtime exceptions
     * if the event handling framework attempts to invoke removed listener methods.
     * </p>
     *
     * @param module the OpenkodaModule whose event listeners should be deleted
     */
    @Modifying
    @Query("delete from EventListenerEntry where module = :module")
    void deleteByModule(OpenkodaModule module);

}
