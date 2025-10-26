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

package com.openkoda.repository.specifications;

import com.openkoda.model.notification.Notification;
import com.openkoda.model.notification.ReadNotification;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

/**
 * JPA Criteria API Specification builders for Notification entity queries with read-status filtering.
 * <p>
 * Provides static factory methods that return {@link Specification} instances for constructing type-safe
 * notification queries using the JPA Criteria API. Specifications implement complex visibility logic
 * including user-specific, organization-specific, and global notification filtering, combined with
 * read/unread status tracking via subqueries. Uses {@link Subquery} to filter out notifications that
 * appear in the ReadNotification join table. These specifications are composable via {@code and()}/{@code or()}
 * operators for building complex notification retrieval queries.
 * </p>
 * <p>
 * Note: The filename contains a typo ('Sepcifications' instead of 'Specifications'). Uses string-based
 * attribute names which are fragile to entity refactoring.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.notification.Notification
 * @see com.openkoda.model.notification.ReadNotification
 * @see jakarta.persistence.criteria.Subquery
 * @see org.springframework.data.jpa.domain.Specification
 */
public class NotificationSepcifications {

    /**
     * Creates a Specification that retrieves all unread notifications visible to the specified user.
     * <p>
     * Constructs a complex predicate that: (1) filters notifications based on visibility rules
     * (user-specific, organization-specific, or global), (2) excludes notifications hidden from
     * their author, and (3) uses a {@code NOT IN} subquery to filter out notifications present
     * in the ReadNotification table. The visibility logic considers three notification types:
     * notifications targeted to the specific user (userId matches, organizationId null),
     * notifications scoped to user's organizations (userId null, organizationId in set), and
     * global notifications (both userId and organizationId null).
     * </p>
     * <p>
     * Usage example: {@code allUnreadForUser(userId, orgIds).and(additionalFilters)}
     * </p>
     *
     * @param id the user ID to retrieve unread notifications for. Must not be null
     * @param organizationIds set of organization IDs the user belongs to. If empty, only user-specific
     *                        and global notifications are included. Must not be null
     * @return Specification for Notification filtering that returns only unread notifications visible
     *         to the specified user
     */
    public static Specification<Notification> allUnreadForUser(Long id, Set<Long> organizationIds) {
        return new Specification<Notification>() {
            @Override
            public Predicate toPredicate(Root<Notification> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

                Predicate allUserNotificationsPredicate = getAllUserNotificationsPredicate(root, cb, id, organizationIds);

                Subquery<Long> sq = query.subquery(Long.class);
                Root<ReadNotification> readNotificationRoot = sq.from(ReadNotification.class);
                sq.select(readNotificationRoot.get("notificationId"));

                Predicate readPredicate = cb.in(root.get("id")).value(sq).not();

                return cb.and(allUserNotificationsPredicate, readPredicate);
            }
        };
    }

    /**
     * Helper method that constructs the visibility predicate for user-accessible notifications.
     * <p>
     * Builds a composite predicate that implements the notification visibility model:
     * (1) Excludes notifications where hiddenFromAuthor is true AND the user is the creator,
     * (2) Includes user-specific notifications (userId equals parameter, organizationId null),
     * (3) Includes global notifications (both userId and organizationId null),
     * (4) Optionally includes organization-scoped notifications (userId null, organizationId in
     * provided set) when organizationIds is non-empty. All predicates are combined with OR logic
     * for inclusion, wrapped with the hiddenFromAuthor exclusion using AND.
     * </p>
     *
     * @param root the Criteria API Root for Notification entity
     * @param cb the CriteriaBuilder for constructing predicates
     * @param id the user ID to filter notifications for
     * @param organizationIds set of organization IDs for organization-scoped notifications.
     *                        Empty set excludes organization notifications
     * @return composite Predicate representing the visibility rules for user-accessible notifications
     */
    static Predicate getAllUserNotificationsPredicate(Root<Notification> root, CriteriaBuilder cb, Long id, Set<Long> organizationIds) {

        Predicate hiddenFromAuthorPredicate = cb.not(cb.and(cb.isTrue(root.get("hiddenFromAuthor")), cb.equal(root.get("createdBy").get("createdById"), id)));
        Predicate nullOrganizationIdPredicate = root.get("organizationId").isNull();
        Predicate userIdSpecificPredicate = cb.equal(root.get("userId"), id);
        Predicate userSpecificaPredicate = cb.and(nullOrganizationIdPredicate, userIdSpecificPredicate);

        Predicate nullUserIdPredicate = root.get("userId").isNull();
        Predicate globalPredicate = cb.and(nullOrganizationIdPredicate, nullUserIdPredicate);
        if(!organizationIds.isEmpty()) {
            Predicate organizationIdSpecificPredicate = root.get("organizationId").in(organizationIds);
            Predicate organizationSpecificaPredicate = cb.and(nullUserIdPredicate, organizationIdSpecificPredicate);
            return cb.and(hiddenFromAuthorPredicate, cb.or(userSpecificaPredicate, organizationSpecificaPredicate, globalPredicate));
        }
        return cb.and(hiddenFromAuthorPredicate, cb.or(userSpecificaPredicate, globalPredicate));
    }
}
