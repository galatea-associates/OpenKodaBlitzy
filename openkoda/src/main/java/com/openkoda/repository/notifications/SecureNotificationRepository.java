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

package com.openkoda.repository.notifications;

import com.openkoda.model.common.SearchableRepositoryMetadata;
import com.openkoda.model.notification.Notification;
import com.openkoda.repository.SecureRepository;

import static com.openkoda.controller.common.URLConstants.NOTIFICATION;

/**
 * Secure marker interface enforcing privilege checks on Notification entity access operations.
 * <p>
 * This interface extends {@link com.openkoda.repository.SecureRepository} to wrap notification repository operations
 * with automatic privilege enforcement, ensuring that all database queries respect the current
 * user's role-based access control (RBAC) permissions. The security layer intercepts repository
 * methods to filter results based on organization membership and privilege grants.
 * 
 * <p>
 * The interface is annotated with {@link SearchableRepositoryMetadata} to enable full-text search
 * across notification content. The {@code searchIndexFormula} concatenates message, type, reference
 * information, organization ID, and user ID into a lowercased searchable string for efficient
 * text-based queries.
 * 
 * <p>
 * Search formula components:
 * 
 * <ul>
 *   <li><b>message</b>: Notification message content</li>
 *   <li><b>type</b>: Notification type identifier</li>
 *   <li><b>reference</b>: Generated via {@link Notification#REFERENCE_FORMULA} for consistent reference string creation</li>
 *   <li><b>orgid:</b> prefix: Organization ID for filtered searches by tenant (e.g., "orgid:123")</li>
 *   <li><b>userid:</b> prefix: User ID for filtered searches by recipient (e.g., "userid:456")</li>
 * </ul>
 * <p>
 * The search index formula is used by indexing tooling and search subsystems that read the
 * {@code @SearchableRepositoryMetadata} annotation to build full-text search capabilities.
 * Modifying the {@code searchIndexFormula} affects indexing and search behavior across the
 * application, requiring reindexing of existing notification data.
 * 
 * <p>
 * Example usage:
 * 
 * <pre>
 * SecureNotificationRepository repo = ...;
 * Optional&lt;Notification&gt; notification = repo.findOne(notificationId);
 * </pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.repository.SecureRepository
 * @see Notification
 * @see SearchableRepositoryMetadata
 * @see com.openkoda.repository.notifications.NotificationRepository
 */
// entityKey: URL constant (NOTIFICATION) identifying notification entity type in routing
// entityClass: Notification.class for type-safe repository operations and metadata resolution
// searchIndexFormula: SQL formula for full-text search index generation, concatenates notification
//                     fields (message, type, reference, organization_id, user_id) with lowercased
//                     text and prefixed identifiers for efficient filtered searching
@SearchableRepositoryMetadata(
        entityKey = NOTIFICATION,
        entityClass = Notification.class,
        searchIndexFormula =  "lower(message || ' ' || type || ' ' ||"
                + Notification.REFERENCE_FORMULA
                + " || ' orgid:' || COALESCE(CAST (organization_id as text), '')"
                + " || ' userid:' || COALESCE(CAST (user_id as text), ''))"
)
public interface SecureNotificationRepository extends SecureRepository<Notification> {


}
