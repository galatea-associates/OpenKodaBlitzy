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

package com.openkoda.core.form;

import com.openkoda.dto.OrganizationRelatedObject;

import java.util.HashMap;

import static java.lang.Long.parseLong;
import static java.lang.String.valueOf;

/**
 * HashMap-based DTO for organization-scoped dynamic entities.
 * <p>
 * Provides flexible key-value storage with organization awareness for handling
 * dynamic entities without predefined Java class structures. Extends HashMap
 * to support arbitrary field storage while implementing OrganizationRelatedObject
 * for multi-tenancy integration.
 * <p>
 * Used by MapEntityForm to handle MapEntity instances where field structure
 * is determined at runtime from FrontendMappingDefinition rather than static
 * JavaBean properties. The map stores field names as keys and field values
 * as objects, with special handling for the "organizationId" key to support
 * tenant-scoped operations.
 * <p>
 * Example usage:
 * <pre>{@code
 * OrganizationRelatedMap map = new OrganizationRelatedMap();
 * map.put("organizationId", 123L);
 * map.put("name", "Product Name");
 * map.put("price", 99.99);
 * Long orgId = map.getOrganizationId(); // Returns 123L
 * boolean isGlobal = map.isGlobal();    // Returns false
 * }</pre>
 *
 * @see MapEntityForm
 * @see OrganizationRelatedObject
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class OrganizationRelatedMap extends HashMap<String, Object> implements OrganizationRelatedObject {

    /**
     * Parses and returns the organization ID from the map.
     * <p>
     * Retrieves the value stored under the "organizationId" key, converts it
     * to a String, and parses it as a Long. Used by multi-tenancy infrastructure
     * to determine the tenant scope for this entity.
     * 
     * <p>
     * Exception handling: If the organizationId key is missing, the value is null,
     * or parsing fails (invalid format), returns null to indicate a global entity
     * without organization scope.
     * 
     *
     * @return the organization ID as Long, or null for global entities or parsing errors
     */
    @Override
    public Long getOrganizationId() {
        try{
            return parseLong(valueOf(get("organizationId")));
        } catch(RuntimeException e){
            return null;
        }
    }

    /**
     * Checks if this entity is global or tenant-specific.
     * <p>
     * Returns true if the entity has no organization scope (organizationId is null),
     * indicating a global entity accessible across all tenants. Returns false if
     * the entity is scoped to a specific organization, restricting access to that
     * tenant only.
     * 
     * <p>
     * Used by security infrastructure to enforce tenant isolation and determine
     * privilege evaluation scope (global privileges vs organization-scoped privileges).
     * 
     *
     * @return true if the entity is global (no organization scope), false if tenant-specific
     */
    @Override
    public boolean isGlobal() {
        return getOrganizationId() == null;
    }
}
