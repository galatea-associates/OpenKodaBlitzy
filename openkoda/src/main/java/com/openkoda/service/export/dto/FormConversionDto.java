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
 * DTO for dynamic form definitions with CRUD controller generation flags and database table mappings.
 * <p>
 * This is a mutable JavaBean POJO designed for YAML/JSON serialization. It extends ComponentDto
 * to inherit module identifier and organization scope fields. This DTO maps the Form domain entity
 * for export/import operations.

 * <p>
 * Boolean flags control automatic CRUD controller scaffolding:
 * <ul>
 *   <li>registerApiCrudController - generates REST API CRUD endpoints</li>
 *   <li>registerHtmlCrudController - generates HTML/UI CRUD pages</li>
 *   <li>showOnOrganizationDashboard - controls dashboard visibility</li>
 * </ul>

 * <p>
 * This DTO is used by form export pipelines and dynamic entity generation systems. It is not thread-safe
 * and provides no validation or defensive copying.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ComponentDto for inherited module and organizationId fields
 * @see com.openkoda.model.component.Form
 */
public class FormConversionDto extends ComponentDto {
    /**
     * Form name identifier. Nullable.
     */
    private String name;
    
    /**
     * Form definition code or template. Nullable.
     */
    private String code;
    
    /**
     * Required privilege name for reading form data. Nullable.
     */
    private String readPrivilege;
    
    /**
     * Required privilege name for writing/submitting form data. Nullable.
     */
    private String writePrivilege;
    
    /**
     * Flag indicating whether to automatically generate REST API CRUD controller for this form.
     * Defaults to false.
     */
    private boolean registerApiCrudController;
    
    /**
     * Flag indicating whether to automatically generate HTML/UI CRUD controller for this form.
     * Defaults to false.
     */
    private boolean registerHtmlCrudController;
    
    /**
     * Flag indicating whether to display this form on the organization dashboard.
     * Defaults to false.
     */
    private boolean showOnOrganizationDashboard;
    
    /**
     * Database table column definitions in serialized format. Nullable.
     */
    private String tableColumns;
    
    /**
     * Filterable column names for search and query operations. Nullable.
     */
    private String filterColumns;
    
    /**
     * Database table name for form persistence. Nullable.
     */
    private String tableName;
    
    /**
     * Database view name for form data retrieval. Nullable.
     */
    private String tableView;

    /**
     * Returns the form name identifier.
     *
     * @return the form name identifier or null if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the form name identifier.
     *
     * @param name the form name identifier to set, may be null
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the form definition code or template.
     *
     * @return the form definition code or template or null if not set
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the form definition code or template.
     *
     * @param code the form definition code or template to set, may be null
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Returns the required privilege name for reading form data.
     *
     * @return the required privilege name for reading form data or null if not set
     */
    public String getReadPrivilege() {
        return readPrivilege;
    }

    /**
     * Sets the required privilege name for reading form data.
     *
     * @param readPrivilege the required privilege name for reading form data to set, may be null
     */
    public void setReadPrivilege(String readPrivilege) {
        this.readPrivilege = readPrivilege;
    }

    /**
     * Returns the required privilege name for writing/submitting form data.
     *
     * @return the required privilege name for writing/submitting form data or null if not set
     */
    public String getWritePrivilege() {
        return writePrivilege;
    }

    /**
     * Sets the required privilege name for writing/submitting form data.
     *
     * @param writePrivilege the required privilege name for writing/submitting form data to set, may be null
     */
    public void setWritePrivilege(String writePrivilege) {
        this.writePrivilege = writePrivilege;
    }

    /**
     * Indicates whether to automatically generate REST API CRUD controller for this form.
     *
     * @return true if REST API CRUD controller should be generated, false otherwise
     */
    public boolean isRegisterApiCrudController() {
        return registerApiCrudController;
    }

    /**
     * Sets whether to automatically generate REST API CRUD controller for this form.
     *
     * @param registerApiCrudController true to enable REST API CRUD controller generation, false to disable
     */
    public void setRegisterApiCrudController(boolean registerApiCrudController) {
        this.registerApiCrudController = registerApiCrudController;
    }

    /**
     * Indicates whether to automatically generate HTML/UI CRUD controller for this form.
     *
     * @return true if HTML/UI CRUD controller should be generated, false otherwise
     */
    public boolean isRegisterHtmlCrudController() {
        return registerHtmlCrudController;
    }

    /**
     * Sets whether to automatically generate HTML/UI CRUD controller for this form.
     *
     * @param registerHtmlCrudController true to enable HTML/UI CRUD controller generation, false to disable
     */
    public void setRegisterHtmlCrudController(boolean registerHtmlCrudController) {
        this.registerHtmlCrudController = registerHtmlCrudController;
    }

    /**
     * Indicates whether to display this form on the organization dashboard.
     *
     * @return true if form should be displayed on organization dashboard, false otherwise
     */
    public boolean isShowOnOrganizationDashboard() {
        return showOnOrganizationDashboard;
    }

    /**
     * Sets whether to display this form on the organization dashboard.
     *
     * @param showOnOrganizationDashboard true to display form on organization dashboard, false to hide
     */
    public void setShowOnOrganizationDashboard(boolean showOnOrganizationDashboard) {
        this.showOnOrganizationDashboard = showOnOrganizationDashboard;
    }

    /**
     * Returns the database table column definitions in serialized format.
     *
     * @return the database table column definitions in serialized format or null if not set
     */
    public String getTableColumns() {
        return tableColumns;
    }

    /**
     * Sets the database table column definitions in serialized format.
     *
     * @param tableColumns the database table column definitions in serialized format to set, may be null
     */
    public void setTableColumns(String tableColumns) {
        this.tableColumns = tableColumns;
    }

    /**
     * Returns the filterable column names for search and query operations.
     *
     * @return the filterable column names for search and query operations or null if not set
     */
    public String getFilterColumns() {
        return filterColumns;
    }

    /**
     * Sets the filterable column names for search and query operations.
     *
     * @param filterColumns the filterable column names for search and query operations to set, may be null
     */
    public void setFilterColumns(String filterColumns) {
        this.filterColumns = filterColumns;
    }

    /**
     * Returns the database table name for form persistence.
     *
     * @return the database table name for form persistence or null if not set
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets the database table name for form persistence.
     *
     * @param tableName the database table name for form persistence to set, may be null
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns the database view name for form data retrieval.
     *
     * @return the database view name for form data retrieval or null if not set
     */
    public String getTableView() {
        return tableView;
    }

    /**
     * Sets the database view name for form data retrieval.
     *
     * @param tableView the database view name for form data retrieval to set, may be null
     */
    public void setTableView(String tableView) {
        this.tableView = tableView;
    }
}
