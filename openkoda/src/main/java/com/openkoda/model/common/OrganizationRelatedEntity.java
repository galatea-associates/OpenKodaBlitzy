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

package com.openkoda.model.common;

import com.openkoda.dto.OrganizationRelatedObject;

/**
 * Defines the contract for domain entities that are scoped to a specific organization (tenant) in the multi-tenant architecture.
 * 
 * <p>
 * This interface establishes the organization-scoped entity pattern used throughout OpenKoda for tenant data isolation.
 * Entities implementing this interface belong to a specific tenant (organization) and are automatically filtered by
 * organization context in queries. This pattern enables secure multi-tenancy where each organization's data is
 * logically separated from other tenants' data.
 * 
 * 
 * <p>
 * <b>Primary Use Case:</b> All domain entities that represent tenant-specific data should implement this interface.
 * The organizationId property serves as the tenant discriminator, ensuring that data access operations are
 * automatically scoped to the current user's organization context.
 * 
 * 
 * <p>
 * <b>Inheritance Hierarchy:</b>
 * 
 * <ul>
 *   <li>Extends {@link OrganizationRelatedObject} - Provides DTO contract for organization association</li>
 *   <li>Extends {@link LongIdEntity} - Provides Long-based primary key contract</li>
 * </ul>
 * 
 * <p>
 * <b>Typical Implementation:</b> Most domain entities extend {@link OpenkodaEntity} which provides the base
 * implementation of this interface, including the organizationId field mapped as a foreign key to the
 * Organization table. The organizationId should be immutable once set (using {@code updatable=false}).
 * 
 * 
 * <p>
 * <b>Tenant Isolation Pattern:</b> The {@link com.openkoda.repository.SecureRepository} pattern
 * automatically enforces organization-scoped queries, filtering results based on the current user's organization
 * context. This ensures that users can only access data belonging to their organization.
 * 
 * 
 * <p>
 * <b>Non-Organization-Related Entities:</b> The following entities are global (cross-tenant) and do NOT implement
 * this interface: User, Token, Role, ReadNotification, FacebookUser, GoogleUser, LDAPUser, LoginAndPassword.
 * These entities are shared across organizations or manage cross-tenant concerns.
 * 
 * 
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see OrganizationRelatedObject
 * @see LongIdEntity
 * @see OpenkodaEntity
 * @see com.openkoda.model.Organization
 * @see com.openkoda.repository.SecureRepository
 */
public interface OrganizationRelatedEntity extends OrganizationRelatedObject, LongIdEntity {
    /**
     * Returns the organization ID that identifies which tenant (organization) this entity belongs to.
     * 
     * <p>
     * This property serves as the tenant discriminator in OpenKoda's multi-tenant architecture, enabling
     * data isolation between organizations. All queries on organization-related entities should filter
     * by this organizationId to ensure tenant data separation.
     * 
     * 
     * <p>
     * The organizationId is typically implemented as a foreign key to the Organization entity and should
     * be immutable once set (using {@code @Column(updatable=false)}). The {@link com.openkoda.repository.SecureRepository}
     * pattern automatically enforces organization-scoped queries using this field.
     * 
     *
     * @return the organization ID (foreign key to Organization entity), may be null for global entities
     *         that are not yet associated with a specific organization
     */
    Long getOrganizationId();

    /**
     * Returns a computed reference string used for UI display and cross-entity references.
     * 
     * <p>
     * <b>Implementation Requirement:</b> This method must be implemented using {@link org.hibernate.annotations.Formula}
     * annotation to compute the value at the database level. The reference string is typically non-insertable and
     * non-updatable, as it's derived from other entity properties.
     * 
     * 
     * <p>
     * The standard implementation uses {@link com.openkoda.model.common.ModelConstants#DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA}
     * which generates a string combining the entity class name and ID. This provides a human-readable reference
     * that can be used in logs, audit trails, and UI displays.
     * 
     * 
     * <p>
     * Example implementation:
     * <pre>
     * {@code @Formula(ModelConstants.DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA)}
     * private String referenceString;
     * </pre>
     * 
     *
     * @return computed reference string combining entity class name and ID, typically in format "EntityName[id]"
     * @see org.hibernate.annotations.Formula
     * @see com.openkoda.model.common.ModelConstants#DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA
     */
    String getReferenceString();

}
