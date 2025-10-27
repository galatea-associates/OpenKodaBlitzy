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

package com.openkoda.dto.user;

import com.openkoda.dto.CanonicalObject;
import com.openkoda.model.PrivilegeGroup;

/**
 * Mutable Data Transfer Object for privilege information transport across application layers.
 * <p>
 * This DTO implements {@link CanonicalObject} to support notification message generation.
 * It serves as a lightweight, serializable representation of privilege data used by controllers,
 * services, mappers, and serializers. The class is designed for compatibility with Jackson
 * serialization and reflection-based mapping frameworks.
 * </p>
 * <p>
 * <b>Important Design Notes:</b>
 * <ul>
 *   <li>Primitive {@code long} id field defaults to 0 when uninitialized</li>
 *   <li>No validation or concurrency controls are enforced - callers are responsible for data integrity</li>
 *   <li>Mutable design optimized for mapping and serialization compatibility</li>
 * </ul>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CanonicalObject
 * @see PrivilegeGroup
 */
public class PrivilegeDto implements CanonicalObject {

    /**
     * Privilege identifier.
     * <p>
     * Primitive {@code long} field defaults to 0 when uninitialized.
     * </p>
     */
    private long id;
    
    /**
     * Privilege name identifier.
     * <p>
     * Used as the primary identifier for privilege lookup and reference.
     * </p>
     */
    private String name;
    
    /**
     * Human-readable privilege label.
     * <p>
     * Descriptive text displayed in user interfaces and reports.
     * </p>
     */
    private String label;
    
    /**
     * Privilege category for logical grouping.
     * <p>
     * Used to organize privileges into functional or domain-specific categories.
     * </p>
     */
    private String category;
    
    /**
     * Domain reference to privilege group for semantic grouping.
     * <p>
     * Associates this privilege with a {@link PrivilegeGroup} enum for hierarchical
     * organization and access control classification.
     * </p>
     */
    private PrivilegeGroup privilegeGroup;

    /**
     * Returns the privilege name identifier.
     *
     * @return the privilege name, may be null if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the privilege name identifier.
     *
     * @param name the privilege name to set, may be null
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Returns the human-readable privilege label.
     *
     * @return the privilege label for display purposes, may be null if not set
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Sets the human-readable privilege label.
     *
     * @param label the descriptive privilege label to set, may be null
     */
    public void setLabel(String label) {
        this.label = label;
    }
    
    /**
     * Returns the privilege category for logical grouping.
     *
     * @return the privilege category, may be null if not set
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the privilege category for logical grouping.
     *
     * @param category the privilege category to set, may be null
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Returns the privilege identifier.
     *
     * @return the privilege id, defaults to 0 if uninitialized
     */
    public long getId() {
        return id;
    }
    
    /**
     * Sets the privilege identifier.
     *
     * @param id the privilege identifier to set
     */
    public void setId(long id) {
        this.id = id;
    }
        
    /**
     * Sets the domain reference to the privilege group for semantic grouping.
     *
     * @param privilegeGroup the {@link PrivilegeGroup} enum value to associate, may be null
     */
    public void setPrivilegeGroup(PrivilegeGroup privilegeGroup) {
        this.privilegeGroup = privilegeGroup;
    }

    /**
     * Returns the domain reference to the privilege group for semantic grouping.
     *
     * @return the associated {@link PrivilegeGroup}, may be null if not set
     */
    public PrivilegeGroup getPrivilegeGroup() {
        return privilegeGroup;
    }
    
    /**
     * Generates a notification message for this privilege.
     * <p>
     * Implements {@link CanonicalObject#notificationMessage()} to provide a formatted
     * string representation suitable for notification systems.
     * </p>
     * <p>
     * Format: "Role: {name}.Scope: {category}."
     * </p>
     *
     * @return formatted notification message containing privilege name and category
     */
    @Override
    public String notificationMessage() {
        return String.format("Role: %s.Scope: %s.", name, category);
    }
}