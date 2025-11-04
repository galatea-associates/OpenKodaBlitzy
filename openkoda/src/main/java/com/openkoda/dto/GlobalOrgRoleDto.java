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

import java.util.ArrayList;
import java.util.List;

/**
 * Simple Data Transfer Object (DTO) that carries global organization role names.
 * <p>
 * This DTO is used for role assignment and authorization workflows where role
 * information needs to be transferred between application layers. It provides
 * a mutable container for a list of role name strings.

 * <p>
 * Note: This class follows a mutable design pattern with direct list access.
 * The getter returns a live list (not a defensive copy), allowing callers to
 * modify the list directly. This design prioritizes simplicity and performance
 * over immutability for internal data transfer scenarios.

 * <p>
 * Example usage:
 * <pre>{@code
 * GlobalOrgRoleDto dto = new GlobalOrgRoleDto();
 * dto.getGlobalOrganizationRoles().add("ROLE_ADMIN");
 * dto.getGlobalOrganizationRoles().add("ROLE_USER");
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class GlobalOrgRoleDto {

    /**
     * List storing global organization role names as strings.
     * <p>
     * Initialized to an empty ArrayList to prevent null pointer exceptions.
     * This is a mutable collection that callers can modify directly through
     * the getter method.

     */
    public List<String> globalOrganizationRoles = new ArrayList<>();

    /**
     * Returns the list of global organization role names.
     * <p>
     * Note: This method returns the live list (not a defensive copy).
     * Modifications to the returned list will directly affect the state
     * of this DTO.

     *
     * @return the list of global organization role names; never null
     */
    public List<String> getGlobalOrganizationRoles() {
        return globalOrganizationRoles;
    }

    /**
     * Sets the list of global organization role names.
     * <p>
     * Replaces the current list with the provided list. While the field
     * is initialized to a non-null value, this method accepts null values.

     *
     * @param globalOrganizationRoles the list of role names to set; may be null
     */
    public void setGlobalOrganizationRoles(List<String> globalOrganizationRoles) {
        this.globalOrganizationRoles = globalOrganizationRoles;
    }
}
