package com.openkoda.model;

import com.openkoda.model.common.AuditableEntity;
import com.openkoda.model.common.ModelConstants;
import com.openkoda.model.common.TimestampedEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;

/**
 * Runtime entity definition for Byte Buddy-generated JPA entities representing dynamically created database tables.
 * <p>
 * Persisted to 'dynamic_entity' table. Stores metadata for runtime-generated JPA entity classes created via
 * DynamicEntityRegistrationService using Byte Buddy bytecode generation. Each DynamicEntity record corresponds
 * to a database table created dynamically without code deployment. Unique tableName constraint ensures one
 * entity definition per table. Used by DynamicEntityDescriptor factory for entity class synthesis with JPA
 * annotations. Extends TimestampedEntity for audit timestamps. Part of OpenKoda's no-code entity creation
 * capability.
 * 
 * <p>
 * Created from Form entities via DynamicEntityRegistrationService, which generates JPA entity classes,
 * repositories, and DDL at runtime. Enables application administrators to define new entity types through
 * UI without code changes.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.service.dynamicentity.DynamicEntityDescriptor
 * @see com.openkoda.service.dynamicentity.DynamicEntityRegistrationService
 * @see com.openkoda.model.component.Form
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"table_name"}))
public class DynamicEntity extends TimestampedEntity implements AuditableEntity {
    /**
     * Primary key from seqGlobalId sequence.
     */
    @Id
    @SequenceGenerator(name = GLOBAL_ID_GENERATOR, sequenceName = GLOBAL_ID_GENERATOR, initialValue = ModelConstants.INITIAL_GLOBAL_VALUE, allocationSize = 10)
    @GeneratedValue(generator = ModelConstants.GLOBAL_ID_GENERATOR, strategy = GenerationType.SEQUENCE)
    private Long id;
    
    /**
     * Database table name for generated entity. Must be unique. @NotNull constraint enforced.
     * Used as entity class name and repository naming.
     */
    @NotNull
    @Column(name = "table_name")
    private String tableName;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
     * Returns tableName for audit trail identification.
     *
     * @return the table name used to identify this dynamic entity in audit logs
     */
    @Override
    public String toAuditString() {
        return tableName;
    }

    @Override
    public Collection<String> ignorePropertiesInAudit() {
        return AuditableEntity.super.ignorePropertiesInAudit();
    }

    @Override
    public Collection<String> contentProperties() {
        return AuditableEntity.super.contentProperties();
    }

}
