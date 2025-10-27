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

import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.model.common.*;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

import static com.openkoda.model.common.ModelConstants.*;

@Entity
/**
 * Abstract base entity for role-based access control (RBAC) implementing single-table inheritance
 * for global, organizational, and hybrid role types.
 * <p>
 * Persisted to 'roles' table with single-table inheritance strategy using type discriminator column.
 * Abstract base class for three concrete role types: GlobalRole (system-wide permissions),
 * OrganizationRole (tenant-scoped permissions), and GlobalOrganizationRole (hybrid with both
 * global and tenant access). Roles contain a collection of privileges serialized as a joined
 * string in the privileges column and deserialized on demand to transient privilegesSet via
 * PrivilegeHelper.
 * </p>
 * <p>
 * Supports dynamic privilege assignment, role name management, and computed access control tokens.
 * Uses GLOBAL_ID_GENERATOR sequence with initial value 10000 and allocationSize 10 for batch ID
 * allocation. Implements custom UseIdOrGenerate strategy to allow explicit ID assignment for
 * predefined roles.
 * </p>
 * <p>
 * Inheritance strategy: {@code @Inheritance(SINGLE_TABLE)} with {@code @DiscriminatorColumn(name='type')}.
 * Subtypes: GlobalRole ('GLOBAL'), OrganizationRole ('ORG'), GlobalOrganizationRole ('GLOBAL_ORG').
 * </p>
 * <p>
 * Privilege serialization: privileges column stores joined string format '(privilege1)(privilege2)'.
 * Lazy deserialization to privilegesSet Set&lt;PrivilegeBase&gt; on first access via getPrivilegesSet().
 * </p>
 * <p>
 * Audit fields: {@code @LastModifiedDate} on updatedOn column with database-level default CURRENT_TIMESTAMP.
 * {@code @PostUpdate} lifecycle callback ensures updatedOn refresh on entity modifications.
 * </p>
 * <p>
 * Computed fields: {@code @Formula}-derived requiredReadPrivilege (_canReadBackend),
 * requiredWritePrivilege (_canManageBackend), indexString (database-generated search index with
 * default empty string).
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see GlobalRole
 * @see OrganizationRole
 * @see GlobalOrganizationRole
 * @see Privilege
 * @see PrivilegeHelper
 * @see UserRole
 */
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Table(name = "roles")
public abstract class Role implements SearchableEntity, LongIdEntity, AuditableEntity, Serializable, EntityWithRequiredPrivilege {

    /**
     * Primary key generated from seqGlobalId sequence (initial 10000, allocationSize 10).
     * Uses custom UseIdOrGenerate strategy to support explicit ID assignment for system roles
     * while auto-generating for user-created roles.
     */
    @Id
    @SequenceGenerator(name = GLOBAL_ID_GENERATOR, sequenceName = GLOBAL_ID_GENERATOR, initialValue = ModelConstants.INITIAL_GLOBAL_VALUE, allocationSize = 10)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="IdOrGenerated")
    @GenericGenerator(name="IdOrGenerated", strategy="com.openkoda.core.customisation.UseIdOrGenerate")
    private Long id;

    /**
     * Timestamp of last role modification.
     * {@code @LastModifiedDate} for Spring Data auditing integration.
     * Database default CURRENT_TIMESTAMP, non-insertable.
     * Also updated via {@code @PostUpdate} lifecycle callback.
     */
    @LastModifiedDate
    @Column(name = UPDATED_ON, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP", insertable=false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updatedOn;

    /**
     * Role display name used in UI and audit trails.
     * Not enforced as unique at database level.
     */
    private String name;

    /**
     * Serialized privilege collection as joined string format '(privilegeName1)(privilegeName2)'.
     * Maximum length 65535 characters.
     * Synchronized with privilegesSet via PrivilegeHelper.toJoinedStringInParenthesis().
     */
    @Column(length = 65535)
    private String privileges;

    /**
     * Transient in-memory Set of PrivilegeBase privileges.
     * Lazy-initialized from privileges string on first getPrivilegesSet() call via
     * PrivilegeHelper.fromJoinedStringInParenthesisToPrivilegeEnumSet().
     * Not persisted.
     */
    @Transient
    private Set<PrivilegeBase> privilegesSet;

    /**
     * Full-text search index column, max length 16300.
     * Database-generated with default empty string, non-insertable from application.
     */
    @Column(name = INDEX_STRING_COLUMN, length = INDEX_STRING_COLUMN_LENGTH, insertable = false)
    @ColumnDefault("''")
    private String indexString;

    /**
     * Flag indicating if role can be deleted.
     * System roles have removable=false. Default true for user-created roles.
     */
    @Column(columnDefinition = "boolean default true")
    private Boolean removable;

    /**
     * {@code @Formula} constant '_canReadBackend'.
     * Privilege token required to read role configuration.
     */
    @Formula("( '" + PrivilegeNames._canReadBackend + "' )")
    private String requiredReadPrivilege;

    /**
     * {@code @Formula} constant '_canManageBackend'.
     * Privilege token required to modify role configuration.
     */
    @Formula("( '" + PrivilegeNames._canManageBackend + "' )")
    private String requiredWritePrivilege;

    /**
     * Returns discriminator value from {@code @DiscriminatorValue} annotation.
     * Returns 'GLOBAL', 'ORG', or 'GLOBAL_ORG' depending on concrete subclass.
     * Returns null if no discriminator annotation found.
     *
     * @return discriminator value as String, or null if annotation not present
     */
    public String getType(){
        DiscriminatorValue val = this.getClass().getAnnotation( DiscriminatorValue.class );
        return val == null ? null : val.value();
    }

    /**
     * Default constructor for JPA.
     */
    public Role() {}

    /**
     * Constructor with role name.
     *
     * @param name role display name
     */
    public Role(String name) {
        this.name = name;
    }

    /**
     * Constructor with explicit ID and name.
     * Used for system roles with predefined IDs.
     *
     * @param id explicit role ID (for system roles)
     * @param name role display name
     */
    public Role(Long id, String name) {
        this.name = name;
        this.id = id;
    }

    /**
     * Returns lazy-initialized Set of privileges.
     * On first call, deserializes privileges string via PrivilegeHelper.
     * Subsequent calls return cached privilegesSet.
     * Never returns null - returns empty set if privileges is null/empty.
     *
     * @return a {@link java.util.Set} of {@link PrivilegeBase} objects, never null
     */
    public Set<PrivilegeBase> getPrivilegesSet() {
        if ( privilegesSet == null ) {
            privilegesSet = PrivilegeHelper.fromJoinedStringInParenthesisToPrivilegeEnumSet( privileges );
        }
        return privilegesSet;
    }

    /**
     * Checks if role contains specified privilege.
     * Returns true if privilege found in privilegesSet, false otherwise.
     *
     * @param privilege a {@link com.openkoda.model.Privilege} object to check
     * @return true if role has the privilege, false otherwise
     */
    public boolean hasPrivilege(Privilege privilege) {
        return getPrivilegesSet().contains(privilege);
    }

    /**
     * Checks if role contains privilege with specified name.
     * Returns true if privilege found in privilegesSet, false otherwise.
     *
     * @param privilege privilege name as String to check
     * @return true if role has privilege with matching name, false otherwise
     */
    public boolean hasPrivilege(String privilege) {
        return getPrivilegesSet().stream().anyMatch(a -> a.name().equals(privilege));
    }

    /**
     * Adds privilege to role's privilege collection.
     * Updates both privilegesSet and serialized privileges string.
     * Does not persist - requires explicit save.
     *
     * @param privilege a {@link PrivilegeBase} object to add
     */
    public void addPrivilege(PrivilegeBase privilege) {
        Set<PrivilegeBase> privileges = getPrivilegesSet();
        privileges.add(privilege);
        setPrivilegesSet(privileges);
    }

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return name;
    }

    /**
     * <p>Setter for the field <code>name</code>.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets privilege collection and synchronizes to serialized privileges string
     * via PrivilegeHelper.toJoinedStringInParenthesis().
     * Replaces entire privilege set.
     *
     * @param privilegesSet a {@link java.util.Set} of {@link PrivilegeBase} objects
     */
    public void setPrivilegesSet(Set<PrivilegeBase> privilegesSet) {
        this.privilegesSet = privilegesSet;
        this.privileges = PrivilegeHelper.toJoinedStringInParenthesis( privilegesSet );
    }

    /**
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return a {@link java.lang.Long} object.
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>Getter for the field <code>privileges</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPrivileges() {
        return privileges;
    }

    /**
     * Returns database-generated search index string.
     *
     * @return search index string, may be empty but not null
     */
    @Override
    public String getIndexString() {
        return indexString;
    }

    /**
     * Returns timestamp of last role modification.
     *
     * @return last update timestamp
     */
    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }

    /**
     * {@code @PostUpdate} JPA lifecycle callback.
     * Updates updatedOn timestamp to current time after entity update operations.
     */
    @PostUpdate
    protected void postUpdate() {
        updatedOn = LocalDateTime.now();
    }

    /**
     * Returns whether role can be deleted.
     * System roles return false, user-created roles return true.
     *
     * @return true if role can be deleted, false for system roles
     */
    public Boolean getRemovable() {
        return removable;
    }

    /**
     * Sets whether role can be deleted.
     *
     * @param removable true if role can be deleted, false otherwise
     */
    public void setRemovable(Boolean removable) {
        this.removable = removable;
    }

    /**
     * Returns role name for audit trail entries.
     * Implements AuditableEntity interface.
     *
     * @return role name for audit logs
     */
    @Override
    public String toAuditString() {
        return name;
    }

    /**
     * Returns privilege token required to read role configuration.
     * {@code @Formula}-derived constant '_canReadBackend'.
     *
     * @return required read privilege token
     */
    @Override
    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    /**
     * Returns privilege token required to modify role configuration.
     * {@code @Formula}-derived constant '_canManageBackend'.
     *
     * @return required write privilege token
     */
    @Override
    public String getRequiredWritePrivilege() {
        return requiredWritePrivilege;
    }
}
