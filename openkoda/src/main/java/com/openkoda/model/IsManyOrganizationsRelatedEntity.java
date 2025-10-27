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

/**
 * Marker interface for entities that are associated with multiple organizations in a many-to-many relationship.
 * <p>
 * Implemented by entities that can belong to or be shared across multiple tenant organizations. This enables 
 * cross-organizational data sharing and multi-tenant access control. Unlike {@link OrganizationRelatedEntity} 
 * which binds to a single organization, entities implementing this interface maintain relationships with multiple 
 * organizations simultaneously.
 * </p>
 * <p>
 * Used for entities like shared resources, cross-organizational users, or global reference data that needs 
 * organization-specific visibility. This pattern is essential in multi-tenant architectures where certain 
 * entities must be accessible across tenant boundaries while maintaining proper access control.
 * </p>
 * <p>
 * Security implication: Repository queries must filter by organizationIds to enforce proper tenant isolation. 
 * Secure repositories use the organization IDs returned by {@link #getOrganizationIds()} to restrict query 
 * results to only those entities accessible by the current user's organizations.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Organization
 * @see OrganizationRelatedEntity
 */
public interface IsManyOrganizationsRelatedEntity {

    /**
     * Returns the IDs of all organizations associated with this entity.
     * <p>
     * Used by secure repositories to filter entities based on the user's accessible organizations. When querying 
     * entities that implement this interface, the repository layer compares the returned organization IDs against 
     * the current user's organization memberships to enforce multi-tenant isolation.
     * </p>
     * <p>
     * Data handling note: The returned array should contain the primary keys of all {@link Organization} entities 
     * that this entity is associated with. An empty array indicates the entity is not associated with any specific 
     * organizations (which may represent a global or system-level entity).
     * </p>
     *
     * @return array of organization IDs, may be empty but typically non-null. Returns null only if not yet initialized.
     */
    Long[] getOrganizationIds();

}
