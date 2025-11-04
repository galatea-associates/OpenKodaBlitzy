/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 and associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.model.component;

import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.PrivilegeNames;
import com.openkoda.model.common.ComponentEntity;
import jakarta.persistence.*;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Formula;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * JPA entity for storing dynamic form/entity definitions that drive the DynamicEntity generation system,
 * enabling no-code entity creation and CRUD operations.
 * <p>
 * This entity plays a critical role in OpenKoda's dynamic entity architecture. Form definitions stored in
 * the {@code code} field are parsed by {@code DynamicEntityRegistrationService} to generate JPA entities
 * at runtime using Byte Buddy bytecode generation. The generated entities are registered with the JPA
 * EntityManager and can have REST API and HTML CRUD interfaces automatically generated.

 * <p>
 * The Form entity uses a dual privilege representation pattern:
 * <ul>
 *   <li>Transient {@link PrivilegeBase} caches ({@code readPrivilege}, {@code writePrivilege}) for in-memory access</li>
 *   <li>Persisted privilege strings ({@code readPrivilegeString}, {@code writePrivilegeString}) for database storage</li>
 *   <li>Nanosecond timestamps for lazy materialization and cache invalidation via {@link PrivilegeHelper}</li>
 * </ul>

 * <p>
 * Organization-scoped multi-tenancy is provided through inheritance from {@link ComponentEntity}, ensuring
 * forms are isolated by organization.

 * <p>
 * Example usage:
 * <pre>
 * Form form = new Form(orgId);
 * form.setName("Customer");
 * form.setTableName("customer");
 * </pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.DynamicEntity
 * @see com.openkoda.model.common.ComponentEntity
 */
@Entity
@Table(name = "form")

public class Form extends ComponentEntity {

    /**
     * Serialization version identifier for Java serialization compatibility.
     */
    private static final long serialVersionUID = 6206245838333775713L;

    /**
     * Static list of content field names used by indexing and diffing tools.
     * Contains ["code"] indicating the code field holds the primary content.
     */
    final static List<String> contentProperties = Arrays.asList("code");

    /**
     * Form name for identification and display purposes.
     */
    @Column
    private String name;

    /**
     * Form definition code in DSL format, up to 262,144 characters.
     * This code is parsed by DynamicEntityRegistrationService to generate runtime JPA entities.
     */
    @Column(length = 65536 * 4)
    private String code;

    /**
     * Transient PrivilegeBase cache for form entity read access control.
     * Lazy-materialized from {@link #readPrivilegeString} when needed, using timestamp comparison
     * to determine freshness. This represents the privilege required to read entities created from this form.
     */
    @Transient
    private PrivilegeBase readPrivilege;
    
    /**
     * Nanosecond timestamp tracking when {@link #readPrivilege} cache was last updated.
     * Used for cache invalidation by comparing with {@link #readPrivilegeStringTimestamp}.
     */
    @Transient
    private long readPrivilegeTimestamp;
    
    /**
     * Persisted privilege string stored in database column 'read_privilege'.
     * Contains the privilege name required to read entities created from this form.
     */
    @Access( AccessType.PROPERTY )
    @Column(name = "read_privilege")
    private String readPrivilegeString;
    
    /**
     * Nanosecond timestamp tracking when {@link #readPrivilegeString} was last updated.
     * Used to detect if re-materialization of {@link #readPrivilege} is needed.
     */
    @Transient
    private long readPrivilegeStringTimestamp;
    
    /**
     * Transient PrivilegeBase cache for form entity write access control.
     * Lazy-materialized from {@link #writePrivilegeString} when needed, using timestamp comparison
     * to determine freshness. This represents the privilege required to modify entities created from this form.
     */
    @Transient
    private PrivilegeBase writePrivilege;
    
    /**
     * Nanosecond timestamp tracking when {@link #writePrivilege} cache was last updated.
     * Used for cache invalidation by comparing with {@link #writePrivilegeStringTimestamp}.
     */
    @Transient
    private long writePrivilegeTimestamp;
    
    /**
     * Persisted privilege string stored in database column 'write_privilege'.
     * Contains the privilege name required to modify entities created from this form.
     */
    @Access( AccessType.PROPERTY )
    @Column(name = "write_privilege")
    private String writePrivilegeString;
    
    /**
     * Nanosecond timestamp tracking when {@link #writePrivilegeString} was last updated.
     * Used to detect if re-materialization of {@link #writePrivilege} is needed.
     */
    @Transient
    private long writePrivilegeStringTimestamp;
    
    /**
     * Computed privilege required to read this Form entity itself (not the generated entities).
     * Formula evaluates to {@link PrivilegeNames#_canReadBackend}, requiring backend read access
     * to view Form definitions in the admin interface.
     */
    @Formula("( '" + PrivilegeNames._canReadBackend + "' )")
    private String requiredReadPrivilege;

    /**
     * Computed privilege required to modify this Form entity itself (not the generated entities).
     * Formula evaluates to {@link PrivilegeNames#_canManageBackend}, requiring backend management access
     * to edit Form definitions in the admin interface.
     */
    @Formula("( '" + PrivilegeNames._canManageBackend + "' )")
    private String requiredWritePrivilege;

    /**
     * If true, generates REST API CRUD endpoints for entities created from this form.
     * The generated endpoints provide standard create, read, update, and delete operations
     * accessible via HTTP REST API.
     */
    private boolean registerApiCrudController;

    /**
     * If true, generates HTML CRUD UI for entities created from this form.
     * The generated interface provides web-based forms for create, read, update, and delete operations.
     */
    private boolean registerHtmlCrudController;

    /**
     * If true, displays form-based entities on the organization dashboard.
     * This makes the entities easily accessible from the main dashboard view.
     */
    private boolean showOnOrganizationDashboard;

    /**
     * If true (default), enables audit logging for entities created from this form.
     * Audit logging tracks all create, update, and delete operations on generated entities.
     * Database default is true.
     */
    @Column(columnDefinition = "boolean default true")
    private boolean registerAsAuditable;
    
    /**
     * Comma-separated column names for table display.
     * Whitespace is automatically stripped when set via {@link #setTableColumns(String)}.
     * Used to configure which fields are displayed in the table view.
     */
    private String tableColumns;
    
    /**
     * Comma-separated column names for filtering capabilities.
     * Whitespace is automatically stripped when set via {@link #setFilterColumns(String)}.
     * Used to configure which fields can be used as filter criteria.
     */
    private String filterColumns;

    /**
     * Database table name for the generated dynamic entity.
     * This becomes the actual SQL table name when the entity is created via Byte Buddy generation.
     */
    private String tableName;

    /**
     * Custom view name for table display.
     * If specified, uses this view instead of the default table name for queries.
     */
    private String tableView;

    /**
     * No-argument constructor for JPA entity instantiation.
     * Initializes controller registration flags and dashboard visibility to false by default.
     */
    public Form() {
        super(null);
        registerApiCrudController = false;
        registerHtmlCrudController = false;
        showOnOrganizationDashboard = false;
    }

    /**
     * Constructor for creating a Form scoped to a specific organization.
     * Enables multi-tenant isolation by associating the form with an organization.
     * Initializes controller registration flags and dashboard visibility to false by default.
     *
     * @param organizationId the organization ID for tenant scoping, may be null for global forms
     */
    public Form(Long organizationId) {
        super(organizationId);
        registerApiCrudController = false;
        registerHtmlCrudController = false;
        showOnOrganizationDashboard = false;
    }

    /**
     * Returns the form name for identification and display.
     *
     * @return the form name, may be null
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the form name for identification and display.
     *
     * @param name the form name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the form definition code in DSL format.
     * This code is parsed to generate runtime JPA entities via Byte Buddy.
     *
     * @return the form definition code, may be null, maximum length 262,144 characters
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the form definition code in DSL format.
     * This code will be parsed by DynamicEntityRegistrationService during entity generation.
     *
     * @param code the form definition code to set, maximum length 262,144 characters
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Returns the read privilege for entities created from this form, using lazy materialization.
     * <p>
     * If the persisted privilege string has been updated more recently than the cached PrivilegeBase
     * (determined by comparing nanosecond timestamps), this method re-materializes the PrivilegeBase
     * from the string via {@link PrivilegeHelper#valueOfString(String)}.

     * <p>
     * Thread-safety note: This method uses {@link System#nanoTime()} timestamps but is not synchronized,
     * so concurrent access may cause redundant PrivilegeHelper calls.

     *
     * @return the read privilege for form-based entities, may be null
     */
    public PrivilegeBase getReadPrivilege() {
        if(this.readPrivilegeStringTimestamp > this.readPrivilegeTimestamp || readPrivilegeTimestamp == 0) {
            this.readPrivilege = PrivilegeHelper.valueOfString(this.readPrivilegeString);
        }
        
        return readPrivilege;
    }

    /**
     * Returns the persisted read privilege string.
     *
     * @return the read privilege string stored in database column 'read_privilege', may be null
     */
    public String getReadPrivilegeString() {
        return this.readPrivilegeString;
    }
    
    /**
     * Sets the read privilege for entities created from this form.
     * Updates both the transient cache and persisted string, recording the update timestamp.
     *
     * @param readPrivilege the read privilege to set, if non-null its name() is persisted
     */
    public void setReadPrivilege(PrivilegeBase readPrivilege) {
        this.readPrivilege = readPrivilege;
        this.readPrivilegeStringTimestamp = System.nanoTime();
        if(readPrivilege != null) {
            this.readPrivilegeString = readPrivilege.name();
        }
    }
    
    /**
     * Sets the persisted read privilege string directly.
     * Updates the string timestamp to trigger re-materialization on next {@link #getReadPrivilege()} call.
     *
     * @param privilegeString the privilege string to persist in database column 'read_privilege'
     */
    public void setReadPrivilegeString(String privilegeString) {
        this.readPrivilegeString = privilegeString;
        this.readPrivilegeStringTimestamp = System.nanoTime();
    }

    /**
     * Returns the write privilege for entities created from this form, using lazy materialization.
     * <p>
     * If the persisted privilege string has been updated more recently than the cached PrivilegeBase
     * (determined by comparing nanosecond timestamps), this method re-materializes the PrivilegeBase
     * from the string via {@link PrivilegeHelper#valueOfString(String)}.

     * <p>
     * Thread-safety note: This method uses {@link System#nanoTime()} timestamps but is not synchronized,
     * so concurrent access may cause redundant PrivilegeHelper calls.

     *
     * @return the write privilege for form-based entities, may be null
     */
    public PrivilegeBase getWritePrivilege() {
        if(this.writePrivilegeStringTimestamp > this.writePrivilegeTimestamp || writePrivilegeTimestamp == 0) {
            this.writePrivilege = PrivilegeHelper.valueOfString(this.writePrivilegeString);
        }
        
        return writePrivilege;
    }
    
    /**
     * Returns the persisted write privilege string.
     *
     * @return the write privilege string stored in database column 'write_privilege', may be null
     */
    public String getWritePrivilegeString() {
        return this.writePrivilegeString;
    }
    
    /**
     * Sets the persisted write privilege string directly.
     * Updates the string timestamp to trigger re-materialization on next {@link #getWritePrivilege()} call.
     *
     * @param privilegeString the privilege string to persist in database column 'write_privilege'
     */
    public void setWritePrivilegeString(String privilegeString) {
        this.writePrivilegeString = privilegeString;
        this.writePrivilegeStringTimestamp = System.nanoTime();
    }

    /**
     * Sets the write privilege for entities created from this form.
     * Updates both the transient cache and persisted string, recording the update timestamp.
     *
     * @param writePrivilege the write privilege to set, if non-null its name() is persisted
     */
    public void setWritePrivilege(PrivilegeBase writePrivilege) {
        this.writePrivilege = writePrivilege;
        this.writePrivilegeStringTimestamp = System.nanoTime();
        if(writePrivilege != null) {
            this.writePrivilegeString = writePrivilege.name();
        }
    }

    /**
     * Returns the privilege required to read this Form entity definition in the admin interface.
     * Overrides {@link ComponentEntity#getRequiredReadPrivilege()} with backend read privilege.
     * <p>
     * Note: This is the privilege for the Form entity itself, not for entities created from this form.
     * For form-based entity privileges, see {@link #getReadPrivilege()}.

     *
     * @return the required read privilege string, evaluates to {@link PrivilegeNames#_canReadBackend}
     */
    @Override
    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    /**
     * Returns the privilege required to modify this Form entity definition in the admin interface.
     * Overrides {@link ComponentEntity#getRequiredWritePrivilege()} with backend management privilege.
     * <p>
     * Note: This is the privilege for the Form entity itself, not for entities created from this form.
     * For form-based entity privileges, see {@link #getWritePrivilege()}.

     *
     * @return the required write privilege string, evaluates to {@link PrivilegeNames#_canManageBackend}
     */
    @Override
    public String getRequiredWritePrivilege() {
        return requiredWritePrivilege;
    }

    /**
     * Returns whether REST API CRUD endpoints should be generated for this form's entities.
     *
     * @return true if REST API CRUD endpoints should be generated, false otherwise
     */
    public boolean isRegisterApiCrudController() {
        return registerApiCrudController;
    }

    /**
     * Sets whether REST API CRUD endpoints should be generated for this form's entities.
     * When true, standard create, read, update, and delete REST endpoints are automatically generated.
     *
     * @param registerApiCrudController true to generate REST API endpoints, false to skip
     */
    public void setRegisterApiCrudController(boolean registerApiCrudController) {
        this.registerApiCrudController = registerApiCrudController;
    }

    /**
     * Returns whether HTML CRUD UI should be generated for this form's entities.
     *
     * @return true if HTML CRUD UI should be generated, false otherwise
     */
    public boolean isRegisterHtmlCrudController() {
        return registerHtmlCrudController;
    }

    /**
     * Sets whether HTML CRUD UI should be generated for this form's entities.
     * When true, web-based forms for create, read, update, and delete operations are automatically generated.
     *
     * @param registerHtmlCrudController true to generate HTML CRUD UI, false to skip
     */
    public void setRegisterHtmlCrudController(boolean registerHtmlCrudController) {
        this.registerHtmlCrudController = registerHtmlCrudController;
    }

    /**
     * Returns whether entities from this form should be displayed on the organization dashboard.
     *
     * @return true if entities should appear on the dashboard, false otherwise
     */
    public boolean isShowOnOrganizationDashboard() {
        return showOnOrganizationDashboard;
    }

    /**
     * Sets whether entities from this form should be displayed on the organization dashboard.
     * When true, the entities are easily accessible from the main dashboard view.
     *
     * @param showOnOrganizationDashboard true to show on dashboard, false to hide
     */
    public void setShowOnOrganizationDashboard(boolean showOnOrganizationDashboard) {
        this.showOnOrganizationDashboard = showOnOrganizationDashboard;
    }

    /**
     * Returns whether audit logging is enabled for entities created from this form.
     *
     * @return true if audit logging is enabled (default), false otherwise
     */
    public boolean isRegisterAsAuditable() {
        return registerAsAuditable;
    }

    /**
     * Sets whether audit logging is enabled for entities created from this form.
     * When true (default), all create, update, and delete operations on generated entities are logged.
     *
     * @param registerAsAuditable true to enable audit logging, false to disable
     */
    public void setRegisterAsAuditable(boolean registerAsAuditable) {
        this.registerAsAuditable = registerAsAuditable;
    }

    /**
     * Returns the comma-separated column names for table display.
     *
     * @return the table columns string with whitespace removed, may be null
     */
    public String getTableColumns() {
        return tableColumns;
    }

    /**
     * Sets the comma-separated column names for table display.
     * Automatically strips ALL whitespace characters from the input via {@code replaceAll("\\s", "")}.
     * If the input is empty or blank, sets the field to null.
     *
     * @param tableColumns comma-separated column names, whitespace will be removed
     */
    public void setTableColumns(String tableColumns) {
        this.tableColumns = StringUtils.isNotEmpty(tableColumns) ? tableColumns.replaceAll("\\s", "") : null;
    }

    /**
     * Returns the table columns as a String array by splitting on comma delimiter.
     *
     * @return array of column names, or empty array if tableColumns is null
     */
    public String[] getTableColumnsList() {
        return tableColumns != null ? tableColumns.split(",") : ArrayUtils.EMPTY_STRING_ARRAY;
    }

    /**
     * Returns the comma-separated column names for filtering capabilities.
     *
     * @return the filter columns string with whitespace removed, may be null
     */
    public String getFilterColumns() {
        return filterColumns;
    }

    /**
     * Sets the comma-separated column names for filtering capabilities.
     * Automatically strips ALL whitespace characters from the input via {@code replaceAll("\\s", "")}.
     * If the input is empty or blank, sets the field to null.
     *
     * @param filterColumns comma-separated column names, whitespace will be removed
     */
    public void setFilterColumns(String filterColumns) {
        this.filterColumns = StringUtils.isNotEmpty(filterColumns) ? filterColumns.replaceAll("\\s", "") : null;
    }

    /**
     * Returns the filter columns as a String array by splitting on comma delimiter.
     *
     * @return array of filter column names, or empty array if filterColumns is null
     */
    public String[] getFilterColumnsList() {
        return filterColumns != null ? filterColumns.split(",") : ArrayUtils.EMPTY_STRING_ARRAY;
    }

    /**
     * Returns the database table name for the generated dynamic entity.
     *
     * @return the SQL table name, may be null
     */
    public String getTableName() {
        return tableName;
    }
    
    /**
     * Sets the database table name for the generated dynamic entity.
     * This becomes the actual SQL table name when the entity is created via Byte Buddy generation.
     *
     * @param tableName the SQL table name to set
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns the custom view name for table display.
     *
     * @return the view name, may be null
     */
    public String getTableView() {
        return tableView;
    }

    /**
     * Sets the custom view name for table display.
     * If specified, uses this view instead of the default table name for queries.
     *
     * @param tableView the view name to set
     */
    public void setTableView(String tableView) {
        this.tableView = tableView;
    }

    /**
     * Convenience method returning the read privilege as a string.
     * Equivalent to calling {@link #getReadPrivilegeString()}.
     *
     * @return the read privilege string, may be null
     */
    public String getReadPrivilegeAsString(){
        return this.readPrivilegeString;
    }

    /**
     * Convenience method returning the write privilege as a string.
     * Equivalent to calling {@link #getWritePrivilegeString()}.
     *
     * @return the write privilege string, may be null
     */
    public String getWritePrivilegeAsString(){
        return this.writePrivilegeString;
    }

    /**
     * Returns the list of content field names used by indexing and diffing tools.
     * Overrides {@link ComponentEntity#contentProperties()}.
     *
     * @return collection containing ["code"], indicating the code field holds the primary content
     */
    @Override
    public Collection<String> contentProperties() {
        return contentProperties;
    }
}
