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

package com.openkoda.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Concrete Role subclass representing tenant-scoped roles with privileges limited to specific organization context.
 * <p>
 * Persisted to 'roles' table with discriminator value 'ORG'. Extends abstract {@link Role} class via single-table
 * inheritance. Organization roles grant permissions within single tenant boundaries. Privileges are evaluated only
 * within the organization context where user has role assignment via {@link UserRole}.
 * 
 * <p>
 * Examples: Organization Admin, Org Member, Org Viewer. Most common role type for multi-tenant applications enforcing
 * data isolation between customers.
 * 
 * <p>
 * <b>Inheritance:</b> Part of Role single-table inheritance hierarchy. {@code @DiscriminatorValue('ORG')} identifies
 * records as organization roles.
 * 
 * <p>
 * <b>Multi-tenancy:</b> Enforces tenant isolation. Users with organization roles can only access data within their
 * assigned organizations. Authorization checks require organizationId matching.
 * 
 * <p>
 * <b>Use cases:</b> Tenant administrators, department managers, organizational viewers, per-customer role customization.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Role
 * @see GlobalRole
 * @see UserRole
 */
@Entity
@DiscriminatorValue("ORG")
public class OrganizationRole extends Role {

    /**
     * Default no-argument constructor for OrganizationRole.
     * <p>
     * Required by JPA for entity instantiation during query result mapping. Creates an empty organization role
     * instance that must be populated with name, privileges, and organization association before use.
     * 
     */
    public OrganizationRole() {
    }

    /**
     * Creates an organization role with the specified name.
     * <p>
     * Initializes a new organization-scoped role instance with the given name. The role is not yet associated with
     * a specific organization until persisted with an organization context. Privileges must be assigned separately
     * via {@code setPrivileges()} or role configuration.
     * 
     *
     * @param name the unique name for this organization role within its organization scope (e.g., "Organization Admin")
     */
    public OrganizationRole(String name) {
        super(name);
    }

    /**
     * Creates an organization role with specified ID and name.
     * <p>
     * Used for constructing organization role instances with known database identity, typically during data migration,
     * testing scenarios, or manual entity construction. The ID should match an existing database record or be null
     * for new entities.
     * 
     *
     * @param id the unique identifier for this role (nullable for new entities)
     * @param name the unique name for this organization role within its organization scope
     */
    public OrganizationRole(Long id, String name) {
        super(id, name);
    }

}
