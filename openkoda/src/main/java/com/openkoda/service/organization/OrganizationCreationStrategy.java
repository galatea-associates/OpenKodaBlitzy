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

package com.openkoda.service.organization;

/**
 * Enumeration defining organization creation strategies for distinguishing between creating new organizations versus assigning existing ones.
 * <p>
 * This enum serves as a compile-time, type-safe token for branching logic and API payloads throughout the OpenKoda platform.
 * It is used in DTOs, controllers, services, and persisted payloads where enum serialization is supported. The enum provides
 * a clear semantic distinction between two fundamental organization operations: creating a brand new tenant with full provisioning
 * versus associating a user or resource with an existing tenant without any new tenant creation.
 * 
 * <p>
 * Thread Safety: This enum contains only immutable constants and is inherently thread-safe. All constants are initialized
 * at class loading time and share no mutable state.
 * 
 * <p>
 * Runtime Characteristics: The enum has minimal runtime footprint with no additional fields or behavior beyond the standard
 * enum mechanics. Changing this enum's public API or removing constants will break callers and serialized consumers.
 * 
 *
 * @see com.openkoda.service.organization.OrganizationService
 * @see com.openkoda.model.Organization
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public enum OrganizationCreationStrategy {
    
    /**
     * Create a new organization with full provisioning.
     * <p>
     * This strategy indicates that a completely new {@link com.openkoda.model.Organization} entity should be constructed
     * and persisted, tenant resources should be provisioned (including dedicated schema or tables depending on the
     * multitenancy configuration), and organization creation events should be emitted for listeners to perform additional
     * initialization tasks such as creating default roles, privileges, and admin users.
     * 
     * <p>
     * Usage: Selected when registering a new tenant, creating organizations via admin interface, or provisioning new
     * customer accounts. This operation is typically more resource-intensive and may involve database schema creation
     * and infrastructure provisioning.
     * 
     */
    CREATE,
    
    /**
     * Assign to an existing organization without creating a new tenant.
     * <p>
     * This strategy indicates that a user or resource should be associated with an existing {@link com.openkoda.model.Organization}
     * entity without triggering any new tenant provisioning. No new schemas, tables, or tenant infrastructure are created.
     * This operation simply establishes relationships between entities and an already-provisioned organization.
     * 
     * <p>
     * Usage: Selected when adding users to existing organizations, assigning resources to established tenants, or creating
     * entity records that belong to a pre-existing organization context. This is a lightweight operation focused on
     * relationship creation rather than tenant provisioning.
     * 
     */
    ASSIGN
}
