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

import com.openkoda.model.UserRole;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Criteria API Specification builders for UserRole entity queries distinguishing organization-level and user-level roles.
 * <p>
 * Provides static factory methods that return {@link Specification} instances for type-safe UserRole query construction
 * using the JPA Criteria API. The specifications distinguish between organization-level roles (where userId is null,
 * indicating the role applies to all users in an organization) and user-level roles (where userId is not null,
 * indicating specific user assignment). These specifications are stateless, thread-safe, and composable via
 * {@code and()}/{@code or()} operators for building complex role filtering queries.
 * 
 * <p>
 * <b>Note:</b> Uses string-based attribute name ('userId') which is fragile to entity refactoring. Consider migrating
 * to JPA metamodel for type safety.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.UserRole
 * @see org.springframework.data.jpa.domain.Specification
 * @see jakarta.persistence.criteria.CriteriaBuilder
 */
public class UserRoleSpecification {

    /**
     * Creates a Specification that matches organization-level roles where userId is null.
     * <p>
     * Constructs a specification that filters UserRole entities where the userId attribute is null, indicating
     * the role applies at the organization level rather than being assigned to a specific user. These roles
     * represent default or inherited role assignments within an organization.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * getUserRolesForOrganizations().and(hasOrganizationId(orgId))
     * }</pre>
     * 
     *
     * @return Specification for UserRole filtering where userId is null (organization-level roles)
     */
    public static Specification<UserRole> getUserRolesForOrganizations() {

        return new Specification<UserRole>() {
            @Override
            public Predicate toPredicate(Root<UserRole> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                return root.get("userId").isNull();
            }
        };
    }

    /**
     * Creates a Specification that matches user-specific roles where userId is not null.
     * <p>
     * Constructs a specification that filters UserRole entities where the userId attribute is not null, indicating
     * the role is explicitly assigned to a specific user rather than applying organization-wide. These roles
     * represent direct user-to-role assignments.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * getUserRolesForUsers().and(hasRoleId(roleId))
     * }</pre>
     * 
     *
     * @return Specification for UserRole filtering where userId is not null (user-specific role assignments)
     */
    public static Specification<UserRole> getUserRolesForUsers() {

        return new Specification<UserRole>() {
            @Override
            public Predicate toPredicate(Root<UserRole> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                return root.get("userId").isNotNull();
            }
        };
    }
}
