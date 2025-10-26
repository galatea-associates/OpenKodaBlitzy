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
import com.openkoda.core.form.OrganizationRelatedMap;
import com.openkoda.core.helper.JsonHelper;
import com.openkoda.model.common.*;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Formula;

/**
 * Organization-scoped key-value store entity persisting flexible JSON string with transient typed Map view for configuration and metadata storage.
 * <p>
 * Persisted to map_entity table. Provides flexible key-value storage per organization without schema constraints.
 * Stores data as JSON string in value column (length ~65536*4 = 262144 characters). Exposes transient valueAsMap
 * property (OrganizationRelatedMap) deserialized via JsonHelper.from(value, OrganizationRelatedMap.class).
 * </p>
 * <p>
 * <strong>WARNING:</strong> setValueAsMap and updateValueFromMap have different serialization behaviors - setValueAsMap
 * caches in transient field, updateValueFromMap immediately serializes to value column. Extends TimestampedEntity for
 * audit timestamps.
 * </p>
 * <p>
 * Usage: Store organization-specific configuration, preferences, or metadata that doesn't fit fixed schema. Alternative
 * to properties Map in Organization entity for bulk key-value storage.
 * </p>
 * <p>
 * JSON handling: Uses JsonHelper for serialization/deserialization. transient valueAsMap field holds parsed Map, value
 * column holds serialized JSON string.
 * </p>
 * <p>
 * <strong>WARNING:</strong> setValueAsMap vs updateValueFromMap semantics differ. setValueAsMap only updates transient
 * field and caches for later serialization. updateValueFromMap immediately serializes to value column for persistence.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Organization
 * @see OrganizationRelatedMap
 * @see JsonHelper
 */
@Entity
@Table (name = "map_entity")
public class MapEntity extends TimestampedEntity implements SearchableOrganizationRelatedEntity, AuditableEntityOrganizationRelated, EntityWithRequiredPrivilege {

    public static final String REFERENCE_FORMULA = DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA;

    @Id
    @SequenceGenerator(name = ORGANIZATION_RELATED_ID_GENERATOR, sequenceName = ORGANIZATION_RELATED_ID_GENERATOR, initialValue = ModelConstants.INITIAL_ORGANIZATION_RELATED_VALUE, allocationSize = 10)
    @GeneratedValue(generator = ORGANIZATION_RELATED_ID_GENERATOR, strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = INDEX_STRING_COLUMN, length = INDEX_STRING_COLUMN_LENGTH, insertable = false)
    @ColumnDefault("''")
    private String indexString;

    //TODO Rule 4.4: should be marked with FetchType = LAZY
    @JsonIgnore
    @ManyToOne(optional = true)
    @JoinColumn(nullable = true, insertable = false, updatable = false, name = ORGANIZATION_ID)
    private Organization organization;

    @Column(nullable = true, name = ORGANIZATION_ID)
    private Long organizationId;

    @Formula(REFERENCE_FORMULA)
    private String referenceString;

    @Column
    private String key;

    /**
     * JSON string storing serialized key-value Map. Maximum length ~262144 characters (65536*4).
     * Contains serialized OrganizationRelatedMap.
     */
    @Column(length = 65536 * 4)
    private String value;

    /**
     * Transient typed Map view of value JSON string. Lazy-initialized on first getValueAsMap() call
     * via JsonHelper.from(value, OrganizationRelatedMap.class). Not persisted directly.
     */
    @Transient
    public OrganizationRelatedMap valueAsMap;

    @Formula("( '" + PrivilegeNames._readOrgData + "' )")
    private String requiredReadPrivilege;

    @Formula("( '" + PrivilegeNames._manageOrgData + "' )")
    private String requiredWritePrivilege;

    public MapEntity() { }

    public MapEntity(Long organizationId) {
        this.organizationId = organizationId;
    }


    @Override
    public String getReferenceString() {
        return referenceString;
    }

    /**
     * Returns deserialized Map from value JSON string. Lazy-initializes on first call.
     * Returns null if value is null.
     *
     * @return deserialized OrganizationRelatedMap from value JSON string, or null if value is null
     */
    public OrganizationRelatedMap getValueAsMap() {
        if (valueAsMap == null) {
            valueAsMap = JsonHelper.from(this.value, OrganizationRelatedMap.class);
        }
        return valueAsMap;
    }

    /**
     * Sets transient valueAsMap field. Caches Map for later serialization.
     * Does NOT immediately update value column.
     *
     * @param valueMap the OrganizationRelatedMap to cache in transient field
     */
    public void setValueAsMap(OrganizationRelatedMap valueMap) {
        value = JsonHelper.to(valueMap);
    }

    /**
     * Serializes valueAsMap to value column immediately via JsonHelper.toJson().
     * Call before persist/merge to ensure JSON string updated.
     */
    public void updateValueFromMap() {
        value = JsonHelper.to(valueAsMap);
    }

    @Override
    public String toAuditString() {
        return null;
    }

    @Override
    public String getIndexString() {
        return this.indexString;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public Long getOrganizationId() {
        return this.organizationId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    @Override
    public String getRequiredWritePrivilege() {
        return requiredWritePrivilege;
    }
}