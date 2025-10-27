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

/**
 * Composite marker interface combining audit trail support and organization-scoped entity contracts.
 * <p>
 * This interface type-binds two foundational concerns for OpenKoda domain entities:
 * <ul>
 *   <li><b>Audit Trail Support</b>: Entities can generate audit trail strings via {@link AuditableEntity#toAuditString()} 
 *       for persistence in {@link Audit} records by the auditing subsystem</li>
 *   <li><b>Organization Scope</b>: Entities are scoped to organizations (tenants) via {@link OrganizationRelatedEntity#getOrganizationId()} 
 *       for multi-tenant data isolation</li>
 * </ul>
 * </p>
 * <p>
 * <b>Usage Pattern:</b><br>
 * Entities implementing this interface support both audit logging and tenant isolation. The primary implementer 
 * is {@link OpenkodaEntity}, which provides base implementations for organization-scoped entities with automatic 
 * audit trail generation. All concrete domain entities extending {@code OpenkodaEntity} inherit this contract.
 * </p>
 * <p>
 * <b>Inherited Methods from {@link AuditableEntity}:</b>
 * <ul>
 *   <li>{@code toAuditString()}: Returns human-readable audit trail representation stored in {@link Audit#change} or {@link Audit#content}</li>
 *   <li>{@code ignorePropertiesInAudit()}: Returns collection of property names to exclude from audit trail (default: empty)</li>
 *   <li>{@code contentProperties()}: Returns collection of property names for {@link Audit#content} storage (default: empty)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Inherited Methods from {@link OrganizationRelatedEntity}:</b>
 * <ul>
 *   <li>{@code getOrganizationId()}: Returns organization ID for tenant scope and data isolation</li>
 *   <li>{@code getReferenceString()}: Returns computed reference string combining organization ID and entity ID</li>
 * </ul>
 * </p>
 * <p>
 * <b>Implementation Guidance:</b>
 * <ul>
 *   <li>Implementers must override {@code toAuditString()} to provide meaningful audit trail representation including entity type and key identifiers</li>
 *   <li>Implementers optionally override {@code ignorePropertiesInAudit()} to exclude sensitive properties (e.g., passwords, tokens) from audit records</li>
 *   <li>Implementers must ensure organization-scoped data isolation via proper {@code organizationId} field population</li>
 *   <li>This is a marker interface with no additional methods beyond those inherited from parent interfaces</li>
 * </ul>
 * </p>
 * <p>
 * <b>Audit Trail Integration:</b><br>
 * The auditing subsystem's {@link com.openkoda.core.audit.AuditInterceptor} invokes {@code toAuditString()} 
 * during Hibernate session flush operations to capture entity changes. Results are persisted to {@link Audit} 
 * entity records with operation type, severity, user information, and correlation IDs for traceability.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see AuditableEntity
 * @see OrganizationRelatedEntity
 * @see OpenkodaEntity
 * @see Audit
 * @see com.openkoda.core.audit.AuditInterceptor
 */
public interface AuditableEntityOrganizationRelated extends AuditableEntity, OrganizationRelatedEntity {
}
