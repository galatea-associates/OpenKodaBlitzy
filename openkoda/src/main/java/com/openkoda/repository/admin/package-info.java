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

/**
 * Audit logging repositories for tracking entity changes and user actions.
 * <p>
 * This package provides Spring Data JPA repository interfaces for managing audit trail data
 * in the OpenKoda application. The repositories support secure access to audit records with
 * organization-scoped filtering and comprehensive search capabilities.
 * </p>
 * 
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.openkoda.repository.admin.AuditRepository} - Main repository for audit record CRUD operations and queries</li>
 *   <li>{@link com.openkoda.repository.admin.SecureAuditRepository} - Secure repository wrapper with privilege enforcement</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>
 * The audit repositories integrate with:
 * </p>
 * <ul>
 *   <li>{@link com.openkoda.core.audit.AuditInterceptor} - Hibernate interceptor for capturing entity changes</li>
 *   <li>{@link com.openkoda.core.audit.PropertyChangeInterceptor} - Session-scoped property change tracking</li>
 *   <li>{@link com.openkoda.model.common.Audit} - JPA entity representing audit records</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>
 * // Query audit records for an organization
 * Page&lt;Audit&gt; audits = auditRepository.findAllByOrganizationId(orgId, "user", pageable);
 * </pre>
 * 
 * <h2>Security and Compliance</h2>
 * <p>
 * All repository operations enforce privilege checks through Spring Security's {@code @PreAuthorize}
 * annotations. Audit records are immutable after creation and support compliance reporting requirements.
 * </p>
 * 
 * @since 1.7.1
 * @see com.openkoda.model.common.Audit
 * @see com.openkoda.core.audit.AuditInterceptor
 */
package com.openkoda.repository.admin;