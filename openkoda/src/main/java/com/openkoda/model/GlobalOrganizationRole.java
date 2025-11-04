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
 * Concrete Role subclass representing hybrid roles combining both system-wide and tenant-scoped privileges for cross-organizational support scenarios.
 * <p>
 * Persisted to 'roles' table with discriminator value 'GLOBAL_ORG'. Extends abstract Role class via single-table inheritance.
 * Hybrid role type granting both global platform access and organization-specific permissions. Enables support staff or 
 * cross-tenant administrators to access system-wide functions while also operating within organizational contexts. 
 * Examples: Support Engineer with org access, Multi-tenant Account Manager. Less common than pure GlobalRole or OrganizationRole.
 * 
 * <p>
 * <b>Inheritance:</b> Part of Role single-table inheritance hierarchy. @DiscriminatorValue('GLOBAL_ORG') identifies records 
 * as global-organization hybrid roles.
 * 
 * <p>
 * <b>Authorization:</b> Privileges evaluated in both global context (no org requirement) and organizational context (with org ID). 
 * Dual-scope access pattern.
 * 
 * <p>
 * <b>Use cases:</b> Customer support with system access, managed service providers, cross-organizational coordinators, 
 * platform support teams.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Role for base role entity
 * @see GlobalRole for pure global roles
 * @see OrganizationRole for pure tenant-scoped roles
 */
@Entity
@DiscriminatorValue("GLOBAL_ORG")
public class GlobalOrganizationRole extends Role {

    /**
     * <p>Constructor for OrganizationRole.</p>
     */
    public GlobalOrganizationRole() {
    }

    /**
     * <p>Constructor for OrganizationRole.</p>
     *
     * @param name a {@link String} object.
     */
    public GlobalOrganizationRole(String name) {
        super(name);
    }

    public GlobalOrganizationRole(Long id, String name) {
        super(id, name);
    }
}
