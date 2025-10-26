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

package com.openkoda.service.export.dto;

/**
 * DTO for dynamic privilege definitions with category, group, and label metadata.
 * <p>
 * This is a mutable JavaBean POJO designed for YAML/JSON serialization of privilege data.
 * It extends {@link ComponentDto} to inherit module identifier and organization scope fields.
 * All fields are String type to provide flexible privilege metadata representation.
 * This DTO maps the {@code DynamicPrivilege} domain entity for component export and import operations.
 * </p>
 * <p>
 * Used by privilege export/import pipelines and RBAC configuration systems to transfer
 * privilege definitions between components. Not thread-safe.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ComponentDto for inherited module and organizationId fields
 * @see com.openkoda.model.DynamicPrivilege domain entity
 */
public class PrivilegeConversionDto extends ComponentDto {
    /**
     * Unique privilege name identifier. Nullable.
     */
    private String name;
    /**
     * Privilege category for logical grouping. Nullable.
     */
    private String category;
    /**
     * Privilege group classification for organizational hierarchy. Nullable.
     */
    private String group;
    /**
     * Human-readable privilege label for display in UI. Nullable.
     */
    private String label;
    /**
     * Computed index string for search and filtering operations. Nullable.
     */
    private String indexString;
    
    /**
     * Returns the unique privilege name identifier.
     *
     * @return the unique privilege name identifier or null if not set
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the unique privilege name identifier.
     *
     * @param name the unique privilege name identifier to set, may be null
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Returns the privilege category for logical grouping.
     *
     * @return the privilege category or null if not set
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
     * Returns the privilege group classification for organizational hierarchy.
     *
     * @return the privilege group or null if not set
     */
    public String getGroup() {
        return group;
    }
    
    /**
     * Sets the privilege group classification for organizational hierarchy.
     *
     * @param group the privilege group to set, may be null
     */
    public void setGroup(String group) {
        this.group = group;
    }
    
    /**
     * Returns the human-readable privilege label for display in UI.
     *
     * @return the privilege label or null if not set
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Sets the human-readable privilege label for display in UI.
     *
     * @param label the privilege label to set, may be null
     */
    public void setLabel(String label) {
        this.label = label;
    }
    
    /**
     * Returns the computed index string for search and filtering operations.
     *
     * @return the index string or null if not set
     */
    public String getIndexString() {
        return indexString;
    }
    
    /**
     * Sets the computed index string for search and filtering operations.
     *
     * @param indexString the index string to set, may be null
     */
    public void setIndexString(String indexString) {
        this.indexString = indexString;
    }
}
