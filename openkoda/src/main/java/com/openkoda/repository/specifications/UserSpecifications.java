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

import com.openkoda.core.flow.Tuple;
import com.openkoda.core.helper.ReadableCode;
import com.openkoda.core.security.OrganizationUser;
import com.openkoda.core.security.UserProvider;
import com.openkoda.model.Privilege;
import com.openkoda.model.User;
import com.openkoda.model.UserRole;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Optional;

/**
 * JPA Criteria API Specification builders for authorization-aware User and UserRole entity queries.
 * <p>
 * Provides static factory methods that return {@link Specification} instances implementing privilege-based
 * access control for User queries. Specifications enforce authorization by consulting
 * {@link UserProvider#getFromContext()} to filter results based on the current user's privileges and
 * organization memberships. Unauthenticated requests return empty results via {@code disjunction()} predicates.
 * Supports both global {@link Privilege#readUserData} privilege (allowing unrestricted access) and
 * organization-scoped privilege checks. Uses {@link CollectionJoin} on the roles collection to filter users
 * by organization membership. Calls {@code query.distinct(true)} to eliminate duplicate rows from the join.
 * </p>
 * <p>
 * <b>Note:</b> Uses string-based attribute names ('organizationId', 'roles') which are fragile to entity
 * refactoring. The dict() method currently returns null as a placeholder and should be implemented or removed.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.User
 * @see com.openkoda.model.UserRole
 * @see com.openkoda.core.security.UserProvider
 * @see com.openkoda.core.security.OrganizationUser
 * @see com.openkoda.model.Privilege
 * @see org.springframework.data.jpa.domain.Specification
 */
public class UserSpecifications implements ReadableCode {

    /**
     * Placeholder specification for Tuple queries - currently returns null.
     * <p>
     * This method is a placeholder that returns a Specification with a null predicate. This is potentially
     * dangerous as it may cause NullPointerExceptions when evaluated by the JPA provider. This method should
     * either be properly implemented with a valid predicate or removed from the API.
     * </p>
     *
     * @return Specification for Tuple that returns null predicate (placeholder implementation)
     * @deprecated This method returns null and should be implemented or removed
     */
    public static Specification<Tuple> dict() {
        return new Specification<Tuple>() {
            @Override
            public Predicate toPredicate(Root<Tuple> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                return null;
            }
        };
    }
    /**
     * Creates a Specification that filters UserRole entities by organization ID.
     * <p>
     * Constructs a specification that matches UserRole entities belonging to the specified organization.
     * When specificOrganizationId is null, returns {@code conjunction()} (always-true predicate) allowing
     * all roles. When non-null, applies exact equality matching on the organizationId attribute.
     * </p>
     * <p>
     * Usage example: {@code searchUserRoleSpecification(123L).and(additionalFilters)}
     * </p>
     *
     * @param specificOrganizationId The organization ID to filter by, or null to allow all organizations (returns conjunction)
     * @return Specification for UserRole filtering by organizationId. Returns always-true predicate when parameter is null
     */
    public static Specification<UserRole> searchUserRoleSpecification(Long specificOrganizationId) {

        return new Specification<UserRole>() {
            @Override
            public Predicate toPredicate(Root<UserRole> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

                if (specificOrganizationId != null) {
                    return cb.equal(root.get("organizationId"), specificOrganizationId);
                }

                return cb.conjunction();

            }

        };

    }


    /**
     * Creates an authorization-aware Specification that filters User entities based on current user's privileges and organization access.
     * <p>
     * Constructs a complex privilege-enforcing predicate with the following authorization model:
     * <ol>
     * <li>Returns {@code disjunction()} (always-false) for unauthenticated requests.</li>
     * <li>If user has global {@link Privilege#readUserData} privilege AND specificOrganizationId is null,
     *     returns {@code conjunction()} (allows all users).</li>
     * <li>If specificOrganizationId is provided, checks if user has readUserData privilege for that organization
     *     (global or org-specific), then joins the 'roles' collection and filters by organizationId,
     *     calling {@code query.distinct(true)} to avoid duplicate rows.</li>
     * <li>If specificOrganizationId is null but user lacks global privilege, filters users by user's accessible
     *     organizationIds via roles join.</li>
     * <li>Returns {@code disjunction()} if user has no organization access.</li>
     * </ol>
     * </p>
     * <p>
     * Usage example: {@code searchSpecification(null).and(nameFilter)}
     * </p>
     * <p>
     * <b>Security note:</b> This method enforces row-level security based on user privileges and organization memberships.
     * </p>
     *
     * @param specificOrganizationId The specific organization ID to scope the query to, or null to use user's accessible organizations
     * @return Specification for User filtering with privilege enforcement and organization scoping. Returns always-false predicate for unauthorized access
     */
    public static Specification<User> searchSpecification(Long specificOrganizationId) {

        return new Specification<User>() {
            @Override
            public Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                Optional<OrganizationUser> optionalUser = UserProvider.getFromContext();
                Predicate noResult = cb.disjunction();

                if(!optionalUser.isPresent()) {
                    return noResult;
                }
                OrganizationUser organizationUser = optionalUser.get();
                if (organizationUser.hasGlobalPrivilege(Privilege.readUserData) && specificOrganizationId == null) {
                    return cb.conjunction();
                }
                CollectionJoin<User, UserRole> roles = root.joinCollection("roles");
                if (specificOrganizationId != null) {
                    if ( organizationUser.hasGlobalOrOrgPrivilege(Privilege.readUserData, specificOrganizationId) ) {
                        query.distinct(true);
                        return roles.get("organizationId").in(Arrays.asList(specificOrganizationId));
                    }
                    return noResult;
                }

                if (organizationUser.getOrganizationIds().isEmpty()) {
                    return noResult;
                }

                return roles.get("organizationId").in(organizationUser.getOrganizationIds());

            }

        };
    }
}
