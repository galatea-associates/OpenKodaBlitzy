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
 * Concrete Role subclass representing system-wide roles with privileges applicable across all organizations without tenant scope restrictions.
 * <p>
 * Persisted to 'roles' table with discriminator value 'GLOBAL'. Extends abstract Role class via single-table inheritance.
 * Global roles grant system-wide permissions that apply regardless of organizational context. Used for platform administrators,
 * support staff, and cross-tenant operations.
 * </p>
 * <p>
 * Part of Role single-table inheritance hierarchy. {@code @DiscriminatorValue('GLOBAL')} identifies records as global roles.
 * Examples of global roles include Super Admin, System Auditor, and Global Support. Privileges assigned to global roles
 * are checked without requiring organization ID in authorization logic.
 * </p>
 * <p>
 * Use cases:
 * <ul>
 *   <li>Platform administration - managing system-wide configuration and settings</li>
 *   <li>System-wide reporting - accessing data across all organizations</li>
 *   <li>Cross-organizational user management - managing users across multiple tenants</li>
 *   <li>Global configuration - system-level feature toggles and parameters</li>
 * </ul>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Role
 * @see OrganizationRole
 * @see GlobalOrganizationRole
 */
@Entity
@DiscriminatorValue("GLOBAL")
public class GlobalRole extends Role {

    /**
     * Default constructor for GlobalRole.
     * <p>
     * Creates a new global role instance without initializing name or privileges.
     * Used by JPA for entity instantiation during database queries. Client code should
     * typically use the parameterized constructor to create named global roles.
     * </p>
     */
    public GlobalRole() {
    }

    /**
     * Parameterized constructor for GlobalRole with role name.
     * <p>
     * Creates a new global role with the specified name. The role will have system-wide scope
     * without tenant restrictions. Privileges must be assigned separately after creation.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * GlobalRole adminRole = new GlobalRole("Super Admin");
     * }</pre>
     * </p>
     *
     * @param name the name of the global role, typically descriptive of system-wide permissions (e.g., "Super Admin", "System Auditor")
     */
    public GlobalRole(String name) {
        super(name);
    }
}
