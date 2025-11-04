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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openkoda.dto.CanonicalObject;
import com.openkoda.model.common.*;
import com.openkoda.model.file.File;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Formula;

import java.util.HashMap;
import java.util.Map;

/**
 * Tenant entity representing an isolated organizational context with custom property storage and branding configuration.
 * <p>
 * Persisted to 'organizations' table. Serves as the primary tenant entity in OpenKoda's multi-tenancy architecture,
 * providing complete data isolation between different customer organizations. Each organization maintains its own
 * property bag stored as JSONB for flexible configuration, branding settings (colors, logo), and computed access
 * control tokens. Organizations use a dedicated sequence generator (seqOrganizationId) with initial value 122.
 * 
 * <p>
 * JPA mapping details: {@code @Entity} with {@code @DynamicUpdate} for selective column updates. Extends
 * {@link TimestampedEntity} for automatic createdOn/updatedOn audit fields via AuditingEntityListener.
 * 
 * <p>
 * Multi-tenancy note: organizationId field duplicates id to satisfy {@link OrganizationRelatedEntity} interface
 * for polymorphic entity handling.
 * 
 * <p>
 * Computed fields: {@code @Formula}-derived fields include referenceString (id as string), requiredReadPrivilege
 * (_readOrgData token), requiredWritePrivilege (_manageOrgData token), and indexString (search index,
 * database-generated default empty string).
 * 
 * <p>
 * Property storage: properties Map persisted to organization_property join table, enabling flexible key-value
 * configuration without schema changes.
 * 
 * <p>
 * Branding: Supports UI customization via mainBrandColor, secondBrandColor, logoId (lazy-loaded File reference),
 * and personalizeDashboard flag.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see TimestampedEntity for audit field inheritance
 * @see AuditableEntity for audit string generation
 * @see SearchableOrganizationRelatedEntity for search capabilities
 * @see EntityWithRequiredPrivilege for privilege requirements
 * @see File for logo association
 */
@Entity
@DynamicUpdate
public class Organization extends TimestampedEntity implements AuditableEntity, SearchableOrganizationRelatedEntity, CanonicalObject, EntityWithRequiredPrivilege {

    /**
     * Formula constant for computing referenceString field.
     * Returns the organization ID as a string representation.
     */
    public static final String REFERENCE_FORMULA = "(id)";

    /**
     * Primary key generated from seqOrganizationId sequence with initial value 122, allocationSize 1
     * for single-value allocation.
     * <p>
     * Serves as unique organization identifier across the system. The sequence starts at 122 to allow
     * reserved IDs for system organizations.
     * 
     */
    @Id
    @SequenceGenerator(name = ORGANIZATION_ID_GENERATOR, sequenceName = ORGANIZATION_ID_GENERATOR,
            initialValue = ModelConstants.INITIAL_ORGANIZATION_VALUE, allocationSize = 1)
    @GeneratedValue(generator = ModelConstants.ORGANIZATION_ID_GENERATOR, strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Organization display name, used in UI and audit trails.
     * <p>
     * No uniqueness constraint enforced at database level, allowing multiple organizations
     * with the same display name if needed.
     * 
     */
    @Column
    private String name;

    /**
     * Duplicate of id column, non-insertable/non-updatable.
     * <p>
     * Required to satisfy {@link OrganizationRelatedEntity} interface contract for polymorphic
     * entity queries. Value automatically synchronized with id by database mapping.
     * 
     */
    @Column(name = "id", insertable = false, updatable = false)
    private Long organizationId;

    /**
     * Full-text search index column, maximum length 16300 characters.
     * <p>
     * Database-generated with default empty string. Non-insertable from application code.
     * Used for efficient full-text search queries on organization data.
     * 
     */
    @Column(name = INDEX_STRING_COLUMN, length = INDEX_STRING_COLUMN_LENGTH, insertable = false)
    @ColumnDefault("''")
    private String indexString;

    /**
     * Computed field returning organization ID as string for reference display in UI.
     * <p>
     * {@code @Formula} computed field: (id). Returns organization ID as string representation
     * for use in templates and API responses.
     * 
     */
    @Formula(REFERENCE_FORMULA)
    private String referenceString;

    /**
     * Datasource assignment identifier for database sharding/routing.
     * <p>
     * Default value 0. Used in multi-database deployments to route queries to specific
     * database instances based on organization assignment.
     * 
     */
    @Column(name = "assigned_datasource", columnDefinition = "INTEGER DEFAULT 0")
    private int assignedDatasource;

    @Override
    public String getReferenceString() {
        return referenceString;
    }

    /**
     * Computed constant privilege token required for read access to organization data.
     * <p>
     * {@code @Formula} computed constant: '_readOrgData'. This privilege token must be granted
     * to users or roles to enable read access to this organization's data.
     * 
     *
     * @see PrivilegeNames#_readOrgData
     */
    @Formula("( '" + PrivilegeNames._readOrgData + "' )")
    private String requiredReadPrivilege;

    /**
     * Computed constant privilege token required for write access to organization data.
     * <p>
     * {@code @Formula} computed constant: '_manageOrgData'. This privilege token must be granted
     * to users or roles to enable write access to this organization's data.
     * 
     *
     * @see PrivilegeNames#_manageOrgData
     */
    @Formula("( '" + PrivilegeNames._manageOrgData + "' )")
    private String requiredWritePrivilege;

    /**
     * Flexible key-value storage for organization-specific configuration.
     * <p>
     * {@code @ElementCollection} Map persisted to organization_property join table. Enables flexible
     * key-value storage for organization-specific configuration (SMTP settings, API keys, feature flags)
     * without schema modifications. Properties are stored as name-value pairs in the join table.
     * 
     */
    @ElementCollection
    @CollectionTable(name = "organization_property",
            joinColumns = {
                    @JoinColumn(name = "organization_id", referencedColumnName = "id")
            })
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    private Map<String, String> properties = new HashMap<>();

    /**
     * Flag enabling per-organization dashboard customization.
     * <p>
     * Default false. When enabled, allows organizations to customize their dashboard layout
     * and widgets according to their specific needs.
     * 
     */
    @Column(columnDefinition = "boolean default false")
    private Boolean personalizeDashboard = false;

    /**
     * Primary brand color in hex format for organization branding.
     * <p>
     * Used for UI theme customization to match organization's visual identity.
     * 
     */
    @Column
    private String mainBrandColor;

    /**
     * Secondary brand color in hex format for organization branding.
     * <p>
     * Used for UI theme customization to complement the main brand color.
     * 
     */
    @Column
    private String secondBrandColor;

    /**
     * Lazy-loaded reference to organization logo File entity.
     * <p>
     * Read-only association for fetching logo file when needed. Logo is loaded lazily to avoid
     * unnecessary database queries. Non-insertable and non-updatable; use logoId to modify the association.
     * 
     *
     * @see File
     */
    @JsonIgnore
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(nullable = true, insertable = false, updatable = false, name = "logo_id")
    protected File logo;

    /**
     * Updatable foreign key to organization logo File entity.
     * <p>
     * Nullable. Set this field to associate a logo with the organization. The logo itself
     * is fetched via the {@link #logo} association when needed.
     * 
     */
    @Column(nullable = true, name = "logo_id", updatable = true)
    protected Long logoId;

    /**
     * Default no-argument constructor required by JPA.
     * <p>
     * Creates an empty Organization instance. Fields should be populated using setters
     * before persisting to database.
     * 
     */
    public Organization() {
    }

    /**
     * Constructor for creating an Organization with a specific organization ID.
     * <p>
     * Used primarily for testing or when reconstructing entities with known IDs.
     * Note that organizationId is normally non-updatable and synchronized with id.
     * 
     *
     * @param organizationId the organization identifier to set
     */
    public Organization(Long organizationId){
        this.organizationId=organizationId;
    }

    /**
     * Constructor for creating an Organization with a name and default datasource.
     * <p>
     * Creates a new organization with the specified name and assigns it to the default
     * datasource (0).
     * 
     *
     * @param name the organization display name
     */
    public Organization(String name) {
        this.name = name;
        this.assignedDatasource = 0;
    }

    /**
     * Constructor for creating an Organization with a name and specific datasource assignment.
     * <p>
     * Creates a new organization with the specified name and assigns it to the given datasource.
     * If assignedDatasource is null, defaults to 0.
     * 
     *
     * @param name the organization display name
     * @param assignedDatasource the datasource assignment identifier (null defaults to 0)
     */
    public Organization(String name, Integer assignedDatasource) {
        this.name = name;
        this.assignedDatasource = assignedDatasource == null ? 0 : assignedDatasource;
    }

    /**
     * Gets the primary key identifier.
     *
     * @return the organization ID (primary key)
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the primary key identifier.
     * <p>
     * Typically not called directly as the ID is generated by the database sequence.
     * May be used when reconstructing entities or in testing scenarios.
     * 
     *
     * @param id the organization ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the organization display name.
     *
     * @return the organization name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the organization display name.
     *
     * @param name the organization name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns organization name for audit trail entries.
     * <p>
     * Provides a human-readable representation of the organization for audit logs and
     * tracking changes.
     * 
     *
     * @return the organization name for audit purposes
     */
    @Override
    public String toAuditString() {
        return name;
    }

    /**
     * Gets the organization identifier.
     * <p>
     * Returns the organizationId field which is synchronized with the id field.
     * 
     *
     * @return the organization ID
     */
    @Override
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Gets the full-text search index string.
     * <p>
     * Returns the database-generated search index used for efficient full-text queries.
     * 
     *
     * @return the search index string (may be empty string if not yet generated)
     */
    @Override
    public String getIndexString() {
        return indexString;
    }

    /**
     * Gets the datasource assignment identifier.
     *
     * @return the assigned datasource identifier (default 0)
     */
    public int getAssignedDatasource() {
        return assignedDatasource;
    }

    /**
     * Sets the datasource assignment identifier.
     *
     * @param assignedDatasource the datasource identifier to assign
     */
    public void setAssignedDatasource(int assignedDatasource) {
        this.assignedDatasource = assignedDatasource;
    }

    /**
     * Formats notification message including organization name and ID.
     * <p>
     * Provides a formatted string suitable for user notifications and system messages.
     * 
     *
     * @return formatted notification message with organization name and ID
     */
    @Override
    public String notificationMessage() {
        return String.format("org: %s, org id: %s", name, id);
    }

    /**
     * Gets the privilege token required for read access to organization data.
     * <p>
     * Returns the computed privilege constant '_readOrgData' required for reading
     * this organization's data.
     * 
     *
     * @return the required read privilege token
     */
    @Override
    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    /**
     * Gets the privilege token required for write access to organization data.
     * <p>
     * Returns the computed privilege constant '_manageOrgData' required for modifying
     * this organization's data.
     * 
     *
     * @return the required write privilege token
     */
    @Override
    public String getRequiredWritePrivilege() {
        return requiredWritePrivilege;
    }

    /**
     * Retrieves a property value by key from the properties map.
     * <p>
     * Returns null if the key is not found in the properties map.
     * 
     *
     * @param name the property key to look up
     * @return the property value, or null if not found
     */
    public String getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Sets a property value in the properties map.
     * <p>
     * Adds or updates the property with the specified key and value.
     * 
     *
     * @param name the property key
     * @param value the property value to set
     * @return the previous value associated with the key, or null if there was no mapping
     */
    public String setProperty(String name, String value) {
        return properties.put(name, value);
    }

    /**
     * Gets the personalize dashboard flag.
     *
     * @return true if dashboard personalization is enabled, false otherwise
     */
    public Boolean getPersonalizeDashboard() {
        return personalizeDashboard;
    }

    /**
     * Sets the personalize dashboard flag.
     *
     * @param personalizeDashboard true to enable dashboard personalization, false to disable
     */
    public void setPersonalizeDashboard(Boolean personalizeDashboard) {
        this.personalizeDashboard = personalizeDashboard;
    }

    /**
     * Gets the main brand color in hex format.
     *
     * @return the main brand color hex code (e.g., "#FF0000"), or null if not set
     */
    public String getMainBrandColor() {
        return mainBrandColor;
    }

    /**
     * Sets the main brand color in hex format.
     *
     * @param mainBrandColor the main brand color hex code to set
     */
    public void setMainBrandColor(String mainBrandColor) {
        this.mainBrandColor = mainBrandColor;
    }

    /**
     * Gets the secondary brand color in hex format.
     *
     * @return the secondary brand color hex code (e.g., "#00FF00"), or null if not set
     */
    public String getSecondBrandColor() {
        return secondBrandColor;
    }

    /**
     * Sets the secondary brand color in hex format.
     *
     * @param secondBrandColor the secondary brand color hex code to set
     */
    public void setSecondBrandColor(String secondBrandColor) {
        this.secondBrandColor = secondBrandColor;
    }

    /**
     * Gets the logo file identifier.
     *
     * @return the logo file ID, or null if no logo is assigned
     */
    public Long getLogoId() {
        return logoId;
    }

    /**
     * Sets the logo file identifier.
     * <p>
     * Set this field to associate a logo file with the organization. The actual file
     * entity can be fetched via {@link #getLogo()}.
     * 
     *
     * @param logoId the logo file ID to set, or null to remove the logo association
     */
    public void setLogoId(Long logoId) {
        this.logoId = logoId;
    }

    /**
     * Gets the lazy-loaded logo File entity.
     * <p>
     * Fetches the logo file associated with this organization. The file is loaded lazily,
     * so accessing this method may trigger a database query if the logo hasn't been loaded yet.
     * 
     *
     * @return the logo File entity, or null if no logo is assigned
     * @see File
     */
    public File getLogo() {
        return logo;
    }
}
