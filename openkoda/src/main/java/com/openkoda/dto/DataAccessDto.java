package com.openkoda.dto;

/**
 * Data Transfer Object for dynamic entity configuration and database table management.
 * <p>
 * This DTO serves as the primary configuration container for the OpenKoda dynamic entity generation system,
 * which uses Byte Buddy for runtime JPA entity class creation. It captures all metadata required to either
 * create a new database table with corresponding JPA entity or map an existing table to a dynamically
 * generated entity class.
 * </p>
 * <p>
 * The DTO implements {@link OrganizationRelatedObject} to support multi-tenant entity scoping, ensuring
 * that dynamically created entities are properly isolated within their respective organization contexts.
 * This is critical for maintaining data separation in OpenKoda's multi-tenancy architecture.
 * </p>
 * <p>
 * Usage context: This DTO is primarily consumed by {@code DynamicEntityRegistrationService} during the
 * form-driven entity creation workflow. When users define new entity types through the OpenKoda admin
 * interface, this DTO captures the configuration which is then transformed into:
 * <ul>
 *   <li>Runtime-generated JPA entity classes (via Byte Buddy)</li>
 *   <li>Database table DDL (CREATE TABLE statements)</li>
 *   <li>Optional REST API CRUD endpoints</li>
 *   <li>Optional HTML CRUD user interface</li>
 *   <li>Audit trail configuration</li>
 * </ul>
 * </p>
 * <p>
 * Notable implementation detail: The {@link #setId(Long)} method has an unusual return type, returning
 * the ID value itself rather than {@code void}. This design facilitates method chaining in certain
 * configuration scenarios.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.DynamicEntity
 * @see com.openkoda.service.dynamicentity.DynamicEntityRegistrationService
 * @see com.openkoda.service.dynamicentity.DynamicEntityService
 */
public class DataAccessDto implements OrganizationRelatedObject{

    /**
     * Unique identifier for the dynamic entity definition.
     * <p>
     * This ID corresponds to the primary key of the form or entity configuration record
     * that defines this dynamic entity type.
     * </p>
     */
    public Long id;
    
    /**
     * Human-readable name for the dynamic entity.
     * <p>
     * This name is displayed in the user interface and serves as the entity's label
     * in dashboards, menus, and reports.
     * </p>
     */
    public String name;
    
    /**
     * Unique code identifier for the entity type.
     * <p>
     * This code is used internally for programmatic references to the entity type
     * and must be unique across all dynamic entities within an organization.
     * </p>
     */
    public String code;
    
    /**
     * Flag indicating whether advanced configuration options are enabled.
     * <p>
     * When true, additional configuration fields become available in the admin interface,
     * allowing power users to customize entity behavior, table schemas, and integration settings.
     * </p>
     */
    public boolean advanced;
    
    /**
     * Organization identifier for multi-tenant entity scoping.
     * <p>
     * Specifies which organization owns this dynamic entity definition. All instances
     * of the dynamically created entity will be scoped to this organization, ensuring
     * proper tenant isolation in multi-tenant deployments.
     * </p>
     */
    public Long organizationId;
    
    /**
     * Required privilege string for read access to entity instances.
     * <p>
     * Defines the OpenKoda privilege name that users must possess to view or query
     * instances of this dynamic entity. Integrated with the SecureRepository pattern
     * for automatic privilege enforcement.
     * </p>
     */
    public String readPrivilege;
    
    /**
     * Required privilege string for write access to entity instances.
     * <p>
     * Defines the OpenKoda privilege name required to create, update, or delete
     * instances of this dynamic entity. Enforced at the service and repository layers
     * through the privilege system.
     * </p>
     */
    public String writePrivilege;
    
    /**
     * Flag to enable audit trail for the dynamic entity.
     * <p>
     * When true, the generated entity class will be registered with the audit subsystem,
     * enabling automatic tracking of all create, update, and delete operations via
     * Hibernate's AuditInterceptor. Defaults to false for performance optimization.
     * </p>
     */
    public boolean registerAsAuditable;
    
    /**
     * Flag to generate entity lifecycle events.
     * <p>
     * When true (default), the system emits events for entity lifecycle operations
     * (creation, modification, deletion), enabling event-driven workflows and integrations.
     * Defaults to true.
     * </p>
     */
    public boolean registerEntityEvent = true;
    
    /**
     * Flag to auto-generate REST API CRUD endpoints for the entity.
     * <p>
     * When true, the system automatically creates a REST controller exposing standard
     * CRUD operations (GET, POST, PUT, DELETE) for this entity type, following OpenKoda's
     * API conventions and security model.
     * </p>
     */
    public boolean registerApiCrudController;
    
    /**
     * Flag to auto-generate HTML CRUD user interface for the entity.
     * <p>
     * When true, the system generates Thymeleaf-based HTML views and controllers
     * for listing, creating, editing, and deleting instances of this entity type,
     * integrated with the OpenKoda admin interface.
     * </p>
     */
    public boolean registerHtmlCrudController;
    
    /**
     * Flag to display the entity on the organization dashboard.
     * <p>
     * When true (default), a widget or link for this entity type appears on the
     * organization's main dashboard, providing quick access to entity management.
     * Defaults to true.
     * </p>
     */
    public boolean showOnOrganizationDashboard = true;
    
    /**
     * Column configuration string for UI table display.
     * <p>
     * Defines which entity fields should be displayed as columns in list views,
     * along with their display order and formatting. Format is implementation-specific
     * and interpreted by the UI table rendering engine.
     * </p>
     */
    public String tableColumns;
    
    /**
     * Comma-separated list of filterable column names.
     * <p>
     * Specifies which entity fields should have filter controls in the UI list view,
     * enabling users to search and filter entity instances by these fields.
     * </p>
     */
    public String filterColumns;
    
    /**
     * Name of existing database table to map to the dynamic entity.
     * <p>
     * When mapping a dynamic entity to an existing database table (createNewTable=false),
     * this field specifies the table name. The entity generation process will introspect
     * the table schema and create corresponding JPA entity fields.
     * </p>
     */
    public String existingTableName;
    
    /**
     * Name for the new database table to be created.
     * <p>
     * When creating a new table for the dynamic entity (createNewTable=true), this field
     * specifies the table name. The system will execute CREATE TABLE DDL based on the
     * entity field definitions.
     * </p>
     */
    public String newTableName;
    
    /**
     * Flag indicating whether to create a new database table.
     * <p>
     * When true, the system creates a new table using newTableName and the defined columns.
     * When false, the system maps to an existing table specified in existingTableName.
     * </p>
     */
    public boolean createNewTable;
    
    /**
     * Comma-separated list of column names for the entity.
     * <p>
     * Defines the database column names that correspond to entity fields. Used in
     * conjunction with field type definitions to generate the complete table schema
     * or validate mapping to existing tables.
     * </p>
     */
    public String columnNames;
    
    /**
     * SQL view definition for complex entity queries.
     * <p>
     * Optional SQL SELECT statement that defines a database view for this entity.
     * When specified, enables advanced query scenarios such as joins across multiple
     * tables, computed columns, and aggregations.
     * </p>
     */
    public String tableView;

    /**
     * Gets the unique identifier for this dynamic entity definition.
     *
     * @return the entity definition ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this dynamic entity definition.
     * <p>
     * Note: This method has an unusual return type. Unlike standard JavaBean setters
     * that return void, this method returns the ID value to facilitate method chaining
     * in configuration scenarios.
     * </p>
     *
     * @param id the entity definition ID to set
     * @return the ID value that was set (not the DTO instance)
     */
    public Long setId(Long id) {
        this.id = id;
        return id;
    }

    /**
     * Gets the human-readable name for this dynamic entity.
     *
     * @return the entity name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the human-readable name for this dynamic entity.
     *
     * @param name the entity name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the unique code identifier for this entity type.
     *
     * @return the entity code
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the unique code identifier for this entity type.
     *
     * @param code the entity code to set (must be unique within the organization)
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets the organization identifier for multi-tenant scoping.
     * <p>
     * This method implements the {@link OrganizationRelatedObject} contract,
     * enabling automatic organization-scoped operations throughout the OpenKoda framework.
     * </p>
     *
     * @return the organization ID
     */
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the organization identifier for multi-tenant scoping.
     *
     * @param organizationId the organization ID to set
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Checks if audit trail is enabled for this dynamic entity.
     *
     * @return true if entity instances should be tracked by the audit subsystem, false otherwise
     */
    public boolean isRegisterAsAuditable() {
        return registerAsAuditable;
    }

    /**
     * Sets whether audit trail should be enabled for this dynamic entity.
     *
     * @param registerAsAuditable true to enable audit tracking, false to disable
     */
    public void setRegisterAsAuditable(boolean registerAsAuditable) {
        this.registerAsAuditable = registerAsAuditable;
    }

    /**
     * Checks if REST API CRUD endpoints should be auto-generated for this entity.
     *
     * @return true if API endpoints should be created, false otherwise
     */
    public boolean isRegisterApiCrudController() {
        return registerApiCrudController;
    }

    /**
     * Sets whether REST API CRUD endpoints should be auto-generated for this entity.
     *
     * @param registerApiCrudController true to generate API endpoints, false to skip
     */
    public void setRegisterApiCrudController(boolean registerApiCrudController) {
        this.registerApiCrudController = registerApiCrudController;
    }

    /**
     * Checks if HTML CRUD user interface should be auto-generated for this entity.
     *
     * @return true if HTML UI should be created, false otherwise
     */
    public boolean isRegisterHtmlCrudController() {
        return registerHtmlCrudController;
    }

    /**
     * Sets whether HTML CRUD user interface should be auto-generated for this entity.
     *
     * @param registerHtmlCrudController true to generate HTML UI, false to skip
     */
    public void setRegisterHtmlCrudController(boolean registerHtmlCrudController) {
        this.registerHtmlCrudController = registerHtmlCrudController;
    }

    /**
     * Checks if this entity should be displayed on the organization dashboard.
     *
     * @return true if entity should appear on dashboard (default), false to hide
     */
    public boolean isShowOnOrganizationDashboard() {
        return showOnOrganizationDashboard;
    }

    /**
     * Sets whether this entity should be displayed on the organization dashboard.
     *
     * @param showOnOrganizationDashboard true to show on dashboard, false to hide
     */
    public void setShowOnOrganizationDashboard(boolean showOnOrganizationDashboard) {
        this.showOnOrganizationDashboard = showOnOrganizationDashboard;
    }

    /**
     * Gets the column configuration string for UI table display.
     *
     * @return the table columns configuration
     */
    public String getTableColumns() {
        return tableColumns;
    }

    /**
     * Sets the column configuration string for UI table display.
     *
     * @param tableColumns the table columns configuration to set
     */
    public void setTableColumns(String tableColumns) {
        this.tableColumns = tableColumns;
    }

    /**
     * Gets the comma-separated list of filterable column names.
     *
     * @return the filter columns list
     */
    public String getFilterColumns() {
        return filterColumns;
    }

    /**
     * Sets the comma-separated list of filterable column names.
     *
     * @param filterColumns the filter columns list to set
     */
    public void setFilterColumns(String filterColumns) {
        this.filterColumns = filterColumns;
    }

    /**
     * Gets the name of the existing database table to map to the entity.
     *
     * @return the existing table name, or null if creating a new table
     */
    public String getExistingTableName() {
        return existingTableName;
    }

    /**
     * Sets the name of the existing database table to map to the entity.
     *
     * @param existingTableName the existing table name to set
     */
    public void setExistingTableName(String existingTableName) {
        this.existingTableName = existingTableName;
    }

    /**
     * Gets the name for the new database table to be created.
     *
     * @return the new table name, or null if mapping to an existing table
     */
    public String getNewTableName() {
        return newTableName;
    }

    /**
     * Sets the name for the new database table to be created.
     *
     * @param newTableName the new table name to set
     */
    public void setNewTableName(String newTableName) {
        this.newTableName = newTableName;
    }

    /**
     * Checks if a new database table should be created.
     *
     * @return true if creating a new table, false if mapping to existing table
     */
    public boolean isCreateNewTable() {
        return createNewTable;
    }

    /**
     * Sets whether a new database table should be created.
     *
     * @param createNewTable true to create new table, false to map to existing
     */
    public void setCreateNewTable(boolean createNewTable) {
        this.createNewTable = createNewTable;
    }

    /**
     * Gets the comma-separated list of column names for the entity.
     *
     * @return the column names list
     */
    public String getColumnNames() {
        return columnNames;
    }

    /**
     * Sets the comma-separated list of column names for the entity.
     *
     * @param columnNames the column names list to set
     */
    public void setColumnNames(String columnNames) {
        this.columnNames = columnNames;
    }

    /**
     * Gets the effective table name for this dynamic entity.
     * <p>
     * This method centralizes the logic for selecting between {@link #newTableName} and
     * {@link #existingTableName} based on the {@link #createNewTable} flag. This ensures
     * consistent table name resolution throughout the dynamic entity generation workflow.
     * </p>
     *
     * @return the new table name if createNewTable is true, otherwise the existing table name
     */
    public String getTableName(){
        if(isCreateNewTable()){
            return getNewTableName();
        }
        return getExistingTableName();
    }

    /**
     * Gets the SQL view definition for this entity.
     *
     * @return the SQL SELECT statement defining the view, or null if not using a view
     */
    public String getTableView() {
        return tableView;
    }

    /**
     * Sets the SQL view definition for this entity.
     *
     * @param tableView the SQL SELECT statement to set
     */
    public void setTableView(String tableView) {
        this.tableView = tableView;
    }

    /**
     * Gets the required privilege string for read access to entity instances.
     *
     * @return the read privilege name
     */
    public String getReadPrivilege() {
        return readPrivilege;
    }

    /**
     * Sets the required privilege string for read access to entity instances.
     *
     * @param readPrivilege the read privilege name to set
     */
    public void setReadPrivilege(String readPrivilege) {
        this.readPrivilege = readPrivilege;
    }

    /**
     * Gets the required privilege string for write access to entity instances.
     *
     * @return the write privilege name
     */
    public String getWritePrivilege() {
        return writePrivilege;
    }

    /**
     * Sets the required privilege string for write access to entity instances.
     *
     * @param writePrivilege the write privilege name to set
     */
    public void setWritePrivilege(String writePrivilege) {
        this.writePrivilege = writePrivilege;
    }

    /**
     * Checks if advanced configuration options are enabled.
     *
     * @return true if advanced options should be displayed, false otherwise
     */
    public boolean isAdvanced() {
        return advanced;
    }

    /**
     * Sets whether advanced configuration options are enabled.
     *
     * @param advanced true to enable advanced options, false to hide them
     */
    public void setAdvanced(boolean advanced) {
        this.advanced = advanced;
    }
    
    /**
     * Sets whether entity lifecycle events should be generated.
     *
     * @param registerEntityEvent true to emit lifecycle events (default), false to disable
     */
    public void setRegisterEntityEvent(boolean registerEntityEvent) {
        this.registerEntityEvent = registerEntityEvent;
    }
    
    /**
     * Checks if entity lifecycle events should be generated.
     *
     * @return true if lifecycle events should be emitted (default), false otherwise
     */
    public boolean isRegisterEntityEvent() {
        return registerEntityEvent;
    }
}
