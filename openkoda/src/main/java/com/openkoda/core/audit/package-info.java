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
 * Core application auditing subsystem capturing entity lifecycle changes via Hibernate interceptor SPI for persistent audit trail.
 *
 * <b>AUDIT ARCHITECTURE OVERVIEW</b>
 * <p>
 * The audit system tracks entity changes through a six-stage pipeline:
 * 
 * <ol>
 *   <li>Hibernate detects entity changes during session flush</li>
 *   <li>PropertyChangeInterceptor (session-scoped) hooks lifecycle events</li>
 *   <li>AuditInterceptor (singleton) computes diffs via PersistanceInterceptor</li>
 *   <li>AuditedObjectState snapshots accumulated in session auditMap</li>
 *   <li>beforeTransactionCompletion converts to Audit entities via PropertyChangeListener</li>
 *   <li>AuditRepository persists batch within active transaction</li>
 * </ol>
 * <p>
 * Key components and their responsibilities:
 * 
 * <ul>
 *   <li><b>PropertyChangeInterceptor</b>: Session-scoped Hibernate hook managing per-session audit state</li>
 *   <li><b>AuditInterceptor</b>: Singleton change detector with entity registry</li>
 *   <li><b>PersistanceInterceptor</b>: Abstract base with computeChanges algorithm</li>
 *   <li><b>AuditedObjectState</b>: Immutable snapshot DTO capturing entity state</li>
 *   <li><b>PropertyChangeListener</b>: Converts state snapshots to Audit entities</li>
 *   <li><b>AuditChangeFactory</b>: Formats HTML change descriptions</li>
 *   <li><b>IpService</b>: Extracts client IP with proxy support</li>
 * </ul>
 *
 * <b>CONFIGURATION REQUIREMENTS</b>
 * <p>
 * <b>CRITICAL:</b> Must configure in application.properties:
 * 
 * <pre>
 * spring.jpa.properties.hibernate.ejb.interceptor.session_scoped=com.openkoda.core.audit.PropertyChangeInterceptor
 * </pre>
 * <p>
 * The session_scoped semantics ensure Hibernate creates one PropertyChangeInterceptor per session for thread isolation.
 * Each interceptor maintains its own auditMap ConcurrentHashMap to track changes within that session.
 * 
 *
 * <b>REGISTERED ENTITIES</b>
 * <p>
 * The following 18 entities are audited by default (registered in AuditInterceptor constructor):
 * 
 * <ul>
 *   <li>Task, Organization, UserRole, User, Role, Form, ServerJs</li>
 *   <li>OrganizationRole, GlobalRole, FrontendResource, ControllerEndpoint</li>
 *   <li>Email, HttpRequestTask, EventListenerEntry</li>
 *   <li>FacebookUser, GoogleUser, LDAPUser, LoginAndPassword</li>
 *   <li>DynamicEntity (runtime-generated entities)</li>
 * </ul>
 * <p>
 * To make additional entity classes auditable, implement {@link com.openkoda.model.common.AuditableEntityOrganizationRelated}
 * or {@link com.openkoda.model.common.AuditableEntity} and register via {@link com.openkoda.core.customisation.BasicCustomisationService}.
 * 
 *
 * <b>AUDIT TRAIL DATA MODEL</b>
 * <p>
 * <b>AuditedObjectState</b> captures entity snapshots with:
 * 
 * <ul>
 *   <li><b>properties</b>: Map of HTML change descriptions</li>
 *   <li><b>changes</b>: Map of before/after value pairs</li>
 *   <li><b>operation</b>: Enum (ADD/EDIT/DELETE)</li>
 *   <li><b>content</b>: Optional field for large payloads</li>
 * </ul>
 * <p>
 * <b>Audit</b> entity persists with:
 * 
 * <ul>
 *   <li><b>entityName</b>, <b>entityId</b>: Identifies changed entity</li>
 *   <li><b>organizationId</b>: Multi-tenant isolation</li>
 *   <li><b>userId</b>, <b>roleIds</b>: Actor identification</li>
 *   <li><b>ipAddress</b>: Client IP (via IpService)</li>
 *   <li><b>requestId</b>: Correlation token for request tracing</li>
 *   <li><b>operation</b>: ADD, EDIT, or DELETE</li>
 *   <li><b>change</b>: HTML-formatted change description</li>
 *   <li><b>content</b>: Large payload storage (e.g., full document text)</li>
 *   <li><b>timestamps</b>: createdOn, updatedOn</li>
 * </ul>
 *
 * <b>CHANGE DETECTION ALGORITHM</b>
 * <p>
 * PersistanceInterceptor.computeChanges compares currentState vs previousState arrays:
 * 
 * <ol>
 *   <li>Iterates through property arrays in parallel</li>
 *   <li>Reports changes when values differ via equals()</li>
 *   <li>Handles special types: arrays via Arrays.toString, LocalDateTime via DatesHelper, Date via FastDateFormat</li>
 *   <li>Respects AuditableEntity.ignorePropertiesInAudit() exclusions</li>
 *   <li>Separates AuditableEntity.contentProperties() into content field</li>
 * </ol>
 * <p>
 * HTML formatting uses pattern: 'from <b>oldValue</b> to <b>newValue</b>' with human-readable labels via
 * StringUtils.splitByCharacterTypeCamelCase. Null values render as '[no value]'.
 * 
 *
 * <b>TRANSACTION SAFETY</b>
 * <p>
 * <b>CRITICAL WARNING:</b> beforeTransactionCompletion executes inside the active Hibernate transaction.
 * Audit save errors can rollback the entire transaction including business changes. Ensure AuditRepository
 * operations succeed to prevent data loss.
 * 
 * <p>
 * Flush timing: Audit entities are persisted before commit. Failures in audit persistence affect the
 * surrounding transaction, potentially rolling back both audit and business data changes.
 * 
 *
 * <b>THREAD SAFETY</b>
 * <ul>
 *   <li><b>PropertyChangeInterceptor</b>: Session-scoped, one instance per Hibernate session, naturally thread-isolated</li>
 *   <li><b>AuditInterceptor</b>: Singleton Spring @Service, mutable auditListeners registry modified only in constructor</li>
 *   <li><b>AuditedObjectState</b>: Immutable references but mutable maps, session-scoped lifecycle prevents sharing</li>
 * </ul>
 *
 * <b>KEY CLASSES</b>
 * <ul>
 *   <li><b>PropertyChangeInterceptor</b>: Session-scoped Hibernate Interceptor hook</li>
 *   <li><b>AuditInterceptor</b>: Singleton service with entity registry and lifecycle implementations</li>
 *   <li><b>PersistanceInterceptor</b>: Abstract base with computeChanges algorithm</li>
 *   <li><b>AuditedObjectState</b>: Immutable snapshot DTO</li>
 *   <li><b>PropertyChangeListener</b>: Converts state to Audit entities</li>
 *   <li><b>AuditChangeFactory</b>: Formats HTML change descriptions</li>
 *   <li><b>IpService</b>: Extracts client IP with proxy support</li>
 *   <li><b>SuccessAuthenticationListener</b>: Updates User.lastLogin on authentication</li>
 *   <li><b>SystemHealthStatus</b>: Health metrics DTO (unrelated to main audit flow)</li>
 *   <li><b>package-info.java</b>: Package documentation</li>
 * </ul>
 *
 * <b>USAGE</b>
 * <p><b>Should I put a class into this package?</b></p>
 * <p>
 * This package provides a closed audit trail implementation. Add classes here only if extending core audit
 * functionality (e.g., custom PropertyChangeListener subclasses, additional audit formatters, specialized
 * interceptor logic). For custom entity auditing, implement AuditableEntity and register via
 * BasicCustomisationService rather than adding code to this package.
 * 
 *
 * <b>RELATED PACKAGES</b>
 * @see com.openkoda.model.common.Audit
 * @see com.openkoda.model.common.AuditableEntity
 * @see com.openkoda.repository.admin.AuditRepository
 * @see com.openkoda.core.customisation.BasicCustomisationService
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
package com.openkoda.core.audit;