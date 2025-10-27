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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.openkoda.dto.user.UserRoleDto;
import com.openkoda.model.common.AuditableEntityOrganizationRelated;
import com.openkoda.model.common.ModelConstants;
import com.openkoda.model.common.SearchableEntity;
import com.openkoda.model.common.TimestampedEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Formula;

import java.io.Serializable;
import java.util.Optional;

import static com.openkoda.model.common.ModelConstants.ORGANIZATION_ID;

/**
 * Join entity representing many-to-many association between User, Organization, and Role for tenant-scoped role assignments.
 * <p>
 * Persisted to 'users_roles' table. Links users to roles within specific organizational contexts, enabling multi-tenant
 * role-based access control. Each UserRole record grants a user specific role permissions within one organization.
 * Users can have multiple UserRole entries for different organizations with different roles.
 * </p>
 * <p>
 * Extends {@link TimestampedEntity} for automatic creation and update timestamp tracking.
 * Implements {@link AuditableEntityOrganizationRelated} for organization scoping and audit trail support.
 * Uses ORGANIZATION_RELATED_ID_GENERATOR sequence for tenant-scoped ID generation with allocation size of 10.
 * </p>
 * <p>
 * Multi-tenancy: The organizationId column enforces tenant isolation. Users access organization data based on
 * UserRole assignments. Global roles (organizationId = null) grant system-wide permissions.
 * </p>
 * <p>
 * Table constraints:
 * - Unique constraint on (user_id, organization_id) prevents duplicate role assignments per user per organization
 * - Index on role_id for efficient role-based queries
 * - Composite index on (user_id, role_id, organization_id) for authorization checks
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see User
 * @see Role
 * @see Organization
 * @see TimestampedEntity
 * @see AuditableEntityOrganizationRelated
 */
@Entity
@Table(name = "users_roles",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", ORGANIZATION_ID})},
        indexes = {
                @Index(name = "role_users_roles", columnList = "role_id"),
                @Index(name = "user_role_organization", columnList = "user_id, role_id, organization_id"),
        }
)
public class UserRole extends TimestampedEntity implements AuditableEntityOrganizationRelated, SearchableEntity, Serializable {

    /**
     * Formula for generating reference string used in audit trails and UI display.
     * Uses the default organization-related reference field formula inherited from parent.
     */
    public static final String REFERENCE_FORMULA = DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA;
    
    /**
     * Primary key identifier for this UserRole assignment.
     * Generated using ORGANIZATION_RELATED_ID_GENERATOR sequence with allocation size 10 for performance.
     * Initial value starts from INITIAL_ORGANIZATION_RELATED_VALUE for tenant-scoped entities.
     */
    @Id
    @SequenceGenerator(name = ORGANIZATION_RELATED_ID_GENERATOR, sequenceName = ORGANIZATION_RELATED_ID_GENERATOR, initialValue = ModelConstants.INITIAL_ORGANIZATION_RELATED_VALUE, allocationSize = 10)
    @GeneratedValue(generator = ORGANIZATION_RELATED_ID_GENERATOR, strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Many-to-one reference to the User entity for this role assignment.
     * <p>
     * Eagerly loaded (default fetch type) for authorization checks and audit trail generation.
     * Marked as optional to handle orphaned records during cascading operations.
     * Join column is read-only (insertable=false, updatable=false) to prevent accidental modifications.
     * </p>
     * <p>
     * Note: TODO Rule 4.4 suggests this should use FetchType.LAZY for performance optimization.
     * Currently eager-loaded to ensure user data availability during privilege evaluation.
     * </p>
     *
     * @see User
     */
    //TODO Rule 4.4: should be marked with FetchType = LAZY
    @JsonIgnore
    @ManyToOne(optional = true)
    @JoinColumn(nullable = true, insertable = false, updatable = false, name = "user_id")
    private User user;
    
    /**
     * Foreign key to the User entity (user_id column).
     * Not nullable to ensure referential integrity for role assignments.
     * Updatable is false to prevent changing user associations after creation.
     */
    @Column(nullable = true, updatable = false, name = "user_id")
    private Long userId;

    /**
     * Many-to-one reference to the Role entity defining permissions for this assignment.
     * <p>
     * Eagerly loaded (default fetch type) to access privilege set during authorization checks.
     * Required field (optional=false) ensures every UserRole has an associated role definition.
     * Join column is read-only (insertable=false, updatable=false) to maintain referential integrity.
     * </p>
     * <p>
     * The role determines what privileges the user has within the organization context.
     * Role can be GlobalRole, OrganizationRole, or GlobalOrganizationRole based on inheritance.
     * </p>
     * <p>
     * Note: TODO Rule 4.4 suggests this should use FetchType.LAZY for performance optimization.
     * Currently eager-loaded to ensure privilege data availability during security evaluations.
     * </p>
     *
     * @see Role
     */
    //TODO Rule 4.4: should be marked with FetchType = LAZY
    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(nullable = false, insertable = false, updatable = false, name = "role_id")
    private Role role;
    
    /**
     * Foreign key to the Role entity (role_id column).
     * Required field (nullable=false) ensures every role assignment has a valid role.
     * Updatable is false to prevent changing role associations after creation.
     */
    @Column(nullable = false, updatable = false, name = "role_id")
    private Long roleId;

    /**
     * Many-to-one reference to the Organization entity defining tenant scope for this role assignment.
     * <p>
     * Eagerly loaded (default fetch type) for organization name display in UI and audit trails.
     * Optional field allows for global role assignments where organizationId is null.
     * Join column is read-only (insertable=false, updatable=false) to prevent accidental scope changes.
     * </p>
     * <p>
     * When organizationId is null, the role assignment is global (system-wide permissions).
     * When organizationId is set, the role assignment is scoped to that specific tenant organization.
     * </p>
     * <p>
     * Note: TODO Rule 4.4 suggests this should use FetchType.LAZY for performance optimization.
     * </p>
     *
     * @see Organization
     */
    //TODO Rule 4.4: should be marked with FetchType = LAZY
    @JsonIgnore
    @ManyToOne(optional = true)
    @JoinColumn(nullable = true, insertable = false, updatable = false, name = ORGANIZATION_ID)
    private Organization organization;
    
    /**
     * Foreign key to the Organization entity (organization_id column).
     * <p>
     * Inherited from {@link AuditableEntityOrganizationRelated} for tenant scoping.
     * Enforces tenant isolation for role assignments - users can only access data within their assigned organizations.
     * </p>
     * <p>
     * When null, indicates a global role assignment with system-wide permissions.
     * When set, restricts role permissions to the specified organization tenant.
     * Part of unique constraint (user_id, organization_id) to prevent duplicate assignments.
     * </p>
     */
    @Column(nullable = true, updatable = false, name = ORGANIZATION_ID)
    private Long organizationId;

    /**
     * Returns the name of the associated role for display and serialization purposes.
     * <p>
     * This convenience method extracts the role name from the associated Role entity.
     * Returns "N/A" if the role relationship is not loaded or is null.
     * Included in JSON serialization via @JsonInclude annotation.
     * </p>
     *
     * @return the role name string, or "N/A" if role is null
     */
    @JsonInclude()
    public String getRoleName() {
        return Optional.ofNullable(getRole()).map(a -> a.getName()).orElse("N/A");
    }

    /**
     * Returns the name of the associated organization for display and serialization purposes.
     * <p>
     * This convenience method extracts the organization name from the associated Organization entity.
     * Returns null for global role assignments where organizationId is null (system-wide permissions).
     * Included in JSON serialization via @JsonInclude annotation.
     * </p>
     *
     * @return the organization name string, or null for global roles
     */
    @JsonInclude()
    public String getOrganizationName() {
        return organizationId == null ? null : getOrganization().getName();
    }


    /**
     * Default no-argument constructor required by JPA for entity instantiation.
     * Creates an empty UserRole instance with all fields set to their default values.
     */
    public UserRole() {
    }

    /**
     * Constructs a UserRole instance with the specified primary key identifier.
     * Useful for creating entity references without fully loading the entity from database.
     *
     * @param id the primary key identifier for this UserRole
     */
    public UserRole(Long id) {
        this.id = id;
    }

    /**
     * Constructs a fully-populated UserRole instance for organization-scoped role assignment.
     * Used for creating tenant-specific role assignments where a user is granted a role within a specific organization.
     *
     * @param id the primary key identifier for this UserRole
     * @param userId the foreign key to the User entity
     * @param roleId the foreign key to the Role entity
     * @param organizationId the foreign key to the Organization entity for tenant scoping (null for global roles)
     */
    public UserRole(Long id, Long userId, Long roleId, Long organizationId) {
        this.id = id;
        this.userId = userId;
        this.roleId = roleId;
        this.organizationId = organizationId;
    }

    /**
     * Constructs a UserRole instance for global role assignment (no organization scope).
     * Used for creating system-wide role assignments where organizationId is null.
     * The resulting role assignment grants privileges across all organizations.
     *
     * @param id the primary key identifier for this UserRole
     * @param userId the foreign key to the User entity
     * @param roleId the foreign key to the Role entity
     */
    public UserRole(Long id, Long userId, Long roleId) {
        this.id = id;
        this.userId = userId;
        this.roleId = roleId;
    }

    /**
     * Returns the primary key identifier for this UserRole assignment.
     *
     * @return the unique identifier (primary key) as a Long value
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the User entity associated with this role assignment.
     * Provides access to user details such as email, login, and other profile information.
     *
     * @return the associated User entity, or null if not loaded
     * @see User
     */
    public User getUser() {
        return user;
    }

    /**
     * Returns the Organization entity defining the tenant scope for this role assignment.
     * Returns null for global role assignments where organizationId is null.
     *
     * @return the associated Organization entity, or null for global roles or if not loaded
     * @see Organization
     */
    public Organization getOrganization() {
        return organization;
    }

    /**
     * Returns the foreign key identifier for the associated User entity.
     *
     * @return the user_id foreign key value
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Returns the foreign key identifier for the associated Role entity.
     *
     * @return the role_id foreign key value
     */
    public Long getRoleId() {
        return roleId;
    }

    /**
     * Returns the foreign key identifier for the associated Organization entity.
     * Returns null for global role assignments (system-wide permissions).
     *
     * @return the organization_id foreign key value, or null for global roles
     */
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Computed reference string field for display in UI and audit trails.
     * Generated by database using @Formula with DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA.
     * This field is not stored in the database but computed at query time for consistent formatting.
     */
    @Formula(REFERENCE_FORMULA)
    private String referenceString;

    /**
     * Returns the computed reference string for this UserRole assignment.
     * Useful for displaying the role assignment in a human-readable format in UI and logs.
     *
     * @return the reference string computed by the database formula
     */
    @Override
    public String getReferenceString() {
        return referenceString;
    }


    /**
     * Returns the Role entity defining the set of privileges granted by this assignment.
     * The role contains the privilege definitions that determine what actions the user can perform.
     *
     * @return the associated Role entity (GlobalRole, OrganizationRole, or GlobalOrganizationRole), or null if not loaded
     * @see Role
     */
    public com.openkoda.model.Role getRole() {
        return role;
    }

    /**
     * Returns a formatted string representation for audit trails containing user, role, and organization information.
     * <p>
     * Format for organization-scoped roles: "user@example.com:RoleName@123" where 123 is the organization ID.
     * Format for global roles: "user@example.com:RoleName" (no organization suffix).
     * Returns "N/A" for user email if the user relationship is not loaded or is null.
     * </p>
     * <p>
     * This method is called by the audit subsystem to generate human-readable audit log entries
     * when UserRole assignments are created, modified, or deleted.
     * </p>
     *
     * @return formatted audit string in the format "email:role[@orgId]"
     */
    @Override
    public String toAuditString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Optional.ofNullable(getUser()).map(a -> a.getEmail()).orElse("N/A"));
        sb.append(":");
        sb.append(getRoleName());
        if (!isGlobal()) {
            sb.append("@");
            sb.append(organizationId);
        }
        return sb.toString();
    }


    /**
     * Returns the search index string used for full-text search capabilities.
     * This field is automatically populated by database triggers or application logic
     * to enable efficient text-based searching of UserRole assignments.
     *
     * @return the index string for search functionality
     */
    @Override
    public String getIndexString() {
        return indexString;
    }

    /**
     * Search index string column for full-text search support.
     * Populated by database triggers or application logic (insertable=false).
     * Defaults to empty string via @ColumnDefault annotation.
     * Maximum length defined by INDEX_STRING_COLUMN_LENGTH constant.
     */
    @Column(name = INDEX_STRING_COLUMN, length = INDEX_STRING_COLUMN_LENGTH, insertable = false)
    @ColumnDefault("''")
    private String indexString;


    /**
     * Converts this UserRole entity to a UserRoleDto data transfer object.
     * <p>
     * Creates a lightweight DTO containing only the identifier fields (id, userId, roleId, organizationId)
     * without the associated entity relationships. Useful for API responses and inter-layer data transfer
     * where full entity graphs are not needed.
     * </p>
     *
     * @return a UserRoleDto populated with this entity's identifier fields
     * @see UserRoleDto
     */
    public UserRoleDto getUserRoleDto() {
        UserRoleDto dto = new UserRoleDto();
        dto.setId(this.id);
        dto.setRoleId(this.roleId);
        dto.setUserId(this.userId);
        dto.setOrganizationId(this.organizationId);
        return dto;
    }
}
