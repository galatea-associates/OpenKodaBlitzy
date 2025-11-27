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

package com.openkoda.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Multi-tenancy marker interface for organization-scoped data transfer objects.
 * <p>
 * This interface identifies DTOs that belong to a specific organization (tenant) in the
 * multi-tenant OpenKoda system. The organization ID serves as the primary mechanism for
 * tenant isolation and data routing, ensuring that operations are performed within the
 * correct organizational boundary.

 * <p>
 * Objects implementing this interface can be either organization-scoped (non-null organization ID)
 * or global (null organization ID). Global objects are accessible across all tenants and
 * represent system-wide data such as global roles, system configurations, or shared resources.
 * Organization-scoped objects are isolated to their respective tenant and implement proper
 * data segregation.

 * <p>
 * Typical implementing classes include organization-scoped DTOs for notifications, files,
 * system settings, and user-related data transfer objects. Services, repositories, and
 * authorization code use this interface to determine the tenant context for operations,
 * apply privilege checks, and enforce multi-tenancy boundaries.

 * <p>
 * Example usage:
 * <pre>{@code
 * OrganizationRelatedObject dto = ...;
 * if (!dto.isGlobal()) {
 *     Long orgId = dto.getOrganizationId();
 *     // Apply organization-scoped operations
 * }
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.Organization
 * @see com.openkoda.core.multitenancy
 */
public interface OrganizationRelatedObject {

    /**
     * Returns the organization identifier for this tenant-scoped object.
     * <p>
     * The organization ID is used throughout OpenKoda for tenant isolation,
     * data routing, and privilege enforcement. Services and repositories use
     * this identifier to scope queries and operations to the appropriate tenant.

     *
     * @return the organization ID for tenant-scoped objects, or {@code null} for
     *         global objects accessible across all tenants
     */
    Long getOrganizationId();

    /**
     * Convenience method for testing whether this object is global (not tenant-scoped).
     * <p>
     * A global object has no associated organization and is accessible across all tenants
     * in the system. This typically applies to system-wide configurations, global roles,
     * or shared resources that are not restricted to a single organization.

     * <p>
     * This derived property is excluded from JSON serialization via {@code @JsonIgnore}
     * to avoid redundant data in API responses, as the global status can be determined
     * from the organization ID.

     *
     * @return {@code true} if the object is global (organization ID is {@code null}),
     *         {@code false} if the object is tenant-scoped (organization ID is non-null)
     */
    @JsonIgnore
    default boolean isGlobal() {
        return getOrganizationId() == null;
    }


}
