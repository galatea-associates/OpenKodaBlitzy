package com.openkoda.model;

import com.openkoda.model.common.*;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Formula;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

import static com.openkoda.model.common.ModelConstants.*;

/**
 * Runtime-configurable privilege entity for creating custom permissions without code deployment, complementing static Privilege enum.
 * <p>
 * Persisted to 'dynamic_privilege' table. Enables application administrators to define new privileges at runtime through UI 
 * without code changes. Implements PrivilegeBase interface for uniform privilege handling alongside static Privilege enum. 
 * Each dynamic privilege has unique name (primary identity), computed read/write privilege requirements via @Formula, 
 * and database-generated indexString and removable flag. Used in advanced scenarios requiring customer-specific permissions 
 * beyond predefined Privilege enum.
 * </p>
 * <p>
 * Implements same PrivilegeBase contract as Privilege enum, enabling polymorphic privilege handling in Role.privilegesSet 
 * and authorization logic. Supports categorization via category and group fields, with database index on name for 
 * efficient lookups.
 * </p>
 * <p>
 * Computed fields via @Formula:
 * <ul>
 *   <li>indexString: Search index (default empty string), non-insertable</li>
 *   <li>removable: Flag indicating deletion capability (default true)</li>
 *   <li>requiredReadPrivilege: Constant '_canReadBackend' for read access control</li>
 *   <li>requiredWritePrivilege: Constant '_canManageBackend' for write access control</li>
 * </ul>
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * DynamicPrivilege customPriv = new DynamicPrivilege();
 * customPriv.setName("custom_action");
 * customPriv.setLabel("Custom Action");
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Privilege for static privilege enumeration
 * @see PrivilegeBase for privilege interface contract
 * @see Role for privilege assignments and authorization
 */
@Entity
@Table(name = "dynamic_privilege",
    indexes = {
            @Index(columnList = "name")
    }
)
public class DynamicPrivilege implements PrivilegeBase, SearchableEntity, LongIdEntity, AuditableEntity, Serializable, EntityWithRequiredPrivilege  {

    private static final long serialVersionUID = -4712574897029645493L;

    @Id
    @SequenceGenerator(name = GLOBAL_ID_GENERATOR, sequenceName = GLOBAL_ID_GENERATOR, initialValue = ModelConstants.INITIAL_GLOBAL_VALUE, allocationSize = 10)
    @GeneratedValue(generator = ModelConstants.GLOBAL_ID_GENERATOR, strategy = GenerationType.SEQUENCE)
    private Long id;
    
    @Column(unique = false)
    private String category;
    
    @Column(name = "privilege_group")
    @Enumerated(EnumType.STRING)
    private PrivilegeGroup group;
    
    /**
     * Unique privilege name serving as canonical identifier.
     * <p>
     * Must be unique across dynamic_privilege table. @NotNull constraint enforced at database level.
     * Used as primary identity for privilege lookups and authorization checks. Implements PrivilegeBase.name() contract.
     * </p>
     */
    @Column(unique = true, nullable = false)
    private String name;
    
    @Column(unique = true, nullable = false)
    private String label;
    
    @LastModifiedDate
    @Column(name = UPDATED_ON, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP", insertable=false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updatedOn;
    
    /**
     * Database-generated search index string.
     * <p>
     * Computed via @Formula (default empty string). Non-insertable field managed by database.
     * Used for full-text search indexing of privilege metadata. Implements SearchableEntity contract.
     * </p>
     */
    @Column(name = INDEX_STRING_COLUMN, length = INDEX_STRING_COLUMN_LENGTH, insertable = false)
    @ColumnDefault("''")
    private String indexString;

    /**
     * Database-generated flag indicating if privilege can be deleted.
     * <p>
     * Database default value set via columnDefinition (default true). System privileges created during initialization 
     * may have removable set to false to prevent accidental deletion.
     * </p>
     */
    @Column(columnDefinition = "boolean default true")
    private Boolean removable;
    
    /**
     * Formula constant for privilege management read access control.
     * <p>
     * Computed via @Formula expression evaluating to PrivilegeNames._canReadBackend constant.
     * Implements EntityWithRequiredPrivilege.getRequiredReadPrivilege() contract for privilege-based access control.
     * </p>
     */
    @Formula("( '" + PrivilegeNames._canReadBackend + "' )")
    private String requiredReadPrivilege;

    /**
     * Formula constant for privilege management write access control.
     * <p>
     * Computed via @Formula expression evaluating to PrivilegeNames._canManageBackend constant.
     * Implements EntityWithRequiredPrivilege.getRequiredWritePrivilege() contract for privilege-based access control.
     * </p>
     */
    @Formula("( '" + PrivilegeNames._canManageBackend + "' )")
    private String requiredWritePrivilege;
    
    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public PrivilegeGroup getGroup() {
        return group;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String name() {
        return name;
    }
    
    public String getName() {
        return name();
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setGroup(PrivilegeGroup group) {
        this.group = group;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }

    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(LocalDateTime updatedOn) {
        this.updatedOn = updatedOn;
    }

    public String getIndexString() {
        return indexString;
    }

    public void setIndexString(String indexString) {
        this.indexString = indexString;
    }

    public Boolean getRemovable() {
        return removable;
    }

    public void setRemovable(Boolean removable) {
        this.removable = removable;
    }

    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    public void setRequiredReadPrivilege(String requiredReadPrivilege) {
        this.requiredReadPrivilege = requiredReadPrivilege;
    }

    public String getRequiredWritePrivilege() {
        return requiredWritePrivilege;
    }

    public void setRequiredWritePrivilege(String requiredWritePrivilege) {
        this.requiredWritePrivilege = requiredWritePrivilege;
    }
    
    @Override
    public String toAuditString() {
        return this.name;
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof DynamicPrivilege)) {
            return false;
        }
        
        return this.name.equals(((DynamicPrivilege)obj).name);
    }
    
    @Override
    public String toString() {
        return this.name;
    }
}
