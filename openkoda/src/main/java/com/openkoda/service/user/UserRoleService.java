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

package com.openkoda.service.user;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.model.Privilege;
import com.openkoda.model.UserRole;
import com.openkoda.repository.specifications.UserRoleSpecification;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Junction entity management service for user-role assignments with organization scoping.
 * <p>
 * Manages {@link UserRole} entities that represent the many-to-many relationship between
 * users and roles. This service provides queries for retrieving user-role assignments
 * scoped by organization, supporting the multi-tenant role-based access control system.

 * <p>
 * The UserRole junction entity contains:
 * <ul>
 *   <li>id - Primary key</li>
 *   <li>userId - Foreign key to User entity</li>
 *   <li>roleId - Foreign key to Role entity</li>
 *   <li>organizationId - Optional foreign key for organization-scoped roles</li>
 * </ul>
 * The entity enforces a unique constraint on (userId, roleId, organizationId) to prevent
 * duplicate role assignments. The createdAt timestamp provides audit trail for assignments.

 * <p>
 * This stateless service acts as a façade, isolating callers from direct repository and
 * specification wiring. All operations use secure repositories with privilege enforcement.

 * <p>
 * Thread-safety: This service is stateless and thread-safe.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.0.0
 * @see UserRole
 * @see com.openkoda.model.Role
 * @see com.openkoda.model.User
 * @see UserRoleSpecification
 */
@Service
public class UserRoleService extends ComponentProvider {

    /**
     * Retrieves all user-role assignments for a specific organization.
     * <p>
     * Queries UserRole entities filtered by organization ID using a join query with the Role
     * entity. This method supports organization-scoped role queries for multi-tenant scenarios
     * where users have different roles in different organizations.

     * <p>
     * The query uses {@link UserRoleSpecification#getUserRolesForOrganizations()} to build
     * the JPA Specification and executes through the secure repository to enforce privilege checks.

     *
     * @param organizationId the organization ID to filter user-role assignments; must not be null
     * @return list of {@link UserRole} entities for the specified organization; empty list if none found
     * @see UserRoleSpecification#getUserRolesForOrganizations()
     */
    public List<UserRole> getUserRolesForOrganization(Long organizationId){
        return repositories.secure.userRole.search(organizationId, UserRoleSpecification.getUserRolesForOrganizations());
    }
}
