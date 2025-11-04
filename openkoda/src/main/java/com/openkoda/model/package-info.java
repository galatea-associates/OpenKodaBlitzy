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
 * Core domain model package containing JPA entities, enums, interfaces, and DTOs for OpenKoda's multi-tenant application platform.
 * <p>
 * This package centralizes all persistence layer domain model types including tenant management (Organization),
 * user authentication (User, Token), role-based access control (Role, Privilege, UserRole), dynamic entity system
 * (DynamicEntity, DynamicPrivilege), and supporting entities for configuration, audit, and search.
 * Uses JPA 3.x (Jakarta Persistence) with Hibernate 6.x extensions for {@code @Formula} computed fields,
 * {@code @DynamicUpdate} selective updates, {@code @JdbcTypeCode} for JSON storage, and Spring Data auditing
 * ({@code @LastModifiedDate}, {@code @CreatedDate}).
 * 
 *
 * <b>Architecture Overview</b>
 * <p>
 * The model package follows a multi-tenancy pattern with {@link com.openkoda.model.Organization} as the primary
 * tenant entity. Entities extend base classes from the {@code common/} subpackage for shared behavior:
 * 
 * <ul>
 *   <li>{@code TimestampedEntity} - Provides audit timestamps (createdOn, updatedOn)</li>
 *   <li>{@code OpenkodaEntity} - Adds organization scope plus timestamps for tenant-scoped entities</li>
 *   <li>{@code SearchableEntity} - Adds full-text indexing capabilities via indexString column</li>
 *   <li>{@code AuditableEntity} - Provides audit string generation for audit trails</li>
 *   <li>{@code EntityWithRequiredPrivilege} - Adds {@code @Formula} privilege requirements for access control</li>
 * </ul>
 * <p>
 * ID generation uses ModelConstants sequence generators:
 * 
 * <ul>
 *   <li>{@code GLOBAL_ID_GENERATOR} - For cross-tenant entities (User, Role, Token)</li>
 *   <li>{@code ORGANIZATION_ID_GENERATOR} - For Organization IDs</li>
 *   <li>{@code ORGANIZATION_RELATED_ID_GENERATOR} - For tenant-scoped entities (UserRole, EmailConfig)</li>
 * </ul>
 *
 * <b>Subpackage Organization</b>
 * <ul>
 *   <li>{@code authentication/} - External authentication provider entities (FacebookUser, GoogleUser, LinkedinUser, LDAPUser, SalesforceUser, LoginAndPassword)</li>
 *   <li>{@code common/} - Base entity classes, constants, and interfaces shared across all domain types</li>
 *   <li>{@code component/} - UI component definitions and event tracking entities</li>
 *   <li>{@code file/} - File storage and management entities</li>
 *   <li>{@code module/} - Modular feature entities for plugin-style extensibility</li>
 *   <li>{@code notification/} - Notification lifecycle and delivery entities</li>
 *   <li>{@code report/} - Reporting and analytics entities</li>
 *   <li>{@code task/} - Background task and scheduled job entities</li>
 * </ul>
 *
 * <b>Key Entity Listings</b>
 *
 * <b>Core Tenant Entities</b>
 * <ul>
 *   <li>{@link com.openkoda.model.Organization} - Tenant entity with property bag and branding configuration</li>
 *   <li>{@link com.openkoda.model.User} - Authentication entity with multi-organization membership support</li>
 *   <li>{@link com.openkoda.model.Role} - RBAC role entity (abstract base for GlobalRole, OrganizationRole, GlobalOrganizationRole)</li>
 *   <li>{@link com.openkoda.model.Privilege} - Permissions enum defining canonical system privileges</li>
 *   <li>{@link com.openkoda.model.UserRole} - Join entity linking users to roles within organizational contexts</li>
 * </ul>
 *
 * <b>Dynamic Entity System</b>
 * <ul>
 *   <li>{@link com.openkoda.model.DynamicEntity} - Runtime entity definitions for Byte Buddy-generated JPA entities</li>
 *   <li>{@link com.openkoda.model.DynamicPrivilege} - Custom runtime-configurable privileges</li>
 *   <li>{@code Form} - Entity definitions for no-code entity creation</li>
 * </ul>
 *
 * <b>Configuration Entities</b>
 * <ul>
 *   <li>{@link com.openkoda.model.EmailConfig} - Per-organization SMTP and mail provider configuration</li>
 *   <li>{@link com.openkoda.model.MapEntity} - Key-value store for flexible organization-scoped configuration</li>
 *   <li>{@link com.openkoda.model.OpenkodaModule} - Feature module definitions for modular architecture</li>
 * </ul>
 *
 * <b>Security Entities</b>
 * <ul>
 *   <li>{@link com.openkoda.model.Token} - Short-lived authentication tokens for password reset, email verification, and API access</li>
 *   <li>{@link com.openkoda.model.PrivilegeBase} - Interface for uniform privilege handling across static enums and dynamic entities</li>
 *   <li>{@link com.openkoda.model.PrivilegeGroup} - Privilege categorization enum for UI organization</li>
 *   <li>{@link com.openkoda.model.PrivilegeNames} - String constants for privilege names used in {@code @Formula} annotations</li>
 *   <li>{@link com.openkoda.model.PrivilegeType} - Coarse-grained privilege classification (ORGANIZATION, GLOBAL, USER)</li>
 * </ul>
 *
 * <b>Search and Audit Entities</b>
 * <ul>
 *   <li>{@link com.openkoda.model.GlobalEntitySearch} - Read-only projection for cross-entity full-text search with privilege filtering</li>
 *   <li>{@link com.openkoda.model.DbVersion} - Schema version tracking for database migration orchestration</li>
 * </ul>
 *
 * <b>JPA Patterns and Conventions</b>
 *
 * <b>Sequence Generators</b>
 * <p>
 * All ID generation uses ModelConstants sequence generator names with {@code allocationSize=10} for batch allocation:
 * 
 * <pre>
 * {@code @GeneratedValue(generator = ModelConstants.GLOBAL_ID_GENERATOR)
 * @GenericGenerator(name = ModelConstants.GLOBAL_ID_GENERATOR, ...)}
 * </pre>
 *
 * <b>Audit Fields</b>
 * <p>
 * Timestamps managed via Spring Data JPA auditing with {@code @CreatedDate} and {@code @LastModifiedDate}
 * annotations. {@code AuditingEntityListener} must be registered via {@code @EnableJpaAuditing}.
 * 
 *
 * <b>Computed Fields</b>
 * <p>
 * {@code @Formula} annotations define database-computed fields for derived values. Common patterns:
 * 
 * <ul>
 *   <li>{@code indexString} - Full-text search index, database-generated with default empty string</li>
 *   <li>{@code requiredReadPrivilege} - Privilege token required to read entity (e.g., "_readOrgData")</li>
 *   <li>{@code requiredWritePrivilege} - Privilege token required to modify entity (e.g., "_manageOrgData")</li>
 *   <li>{@code referenceString} - Display string for UI reference (e.g., organization ID as string)</li>
 * </ul>
 *
 * <b>Multi-Tenancy</b>
 * <p>
 * Tenant isolation enforced via {@code organizationId} column on all tenant-scoped entities.
 * {@code OpenkodaEntity} base class provides organization scope. Repository queries must filter
 * by {@code organizationId} to enforce proper tenant isolation in multi-tenant deployments.
 * 
 *
 * <b>Search Indexing</b>
 * <p>
 * Full-text search enabled via {@code INDEX_STRING_COLUMN} (length 16300) on searchable entities.
 * Database-generated with default empty string. Non-insertable from application code. Populated
 * via database triggers or computed via {@code @Formula} annotations.
 * 
 *
 * <b>JSON Storage</b>
 * <p>
 * PostgreSQL JSONB storage for flexible schema-less data using {@code @JdbcTypeCode(SqlTypes.JSON)}
 * with {@code columnDefinition='jsonb'}. Common use cases:
 * 
 * <ul>
 *   <li>{@code Organization.properties} - Key-value property bag via {@code @ElementCollection} join table</li>
 *   <li>{@code MapEntity.value} - JSON string with transient typed Map view</li>
 *   <li>{@code DynamicEntityCsvImportRow.content} - Flexible CSV row storage during import validation</li>
 *   <li>{@code User.attributes} - Custom user metadata without schema changes</li>
 * </ul>
 *
 * <b>Common Pitfalls</b>
 * <ul>
 *   <li><strong>DbVersion NPE Risk:</strong> {@code DbVersion.value()} and {@code toString()} perform Integer
 *       unboxing without null checks. Ensure major/minor/build/revision are initialized to avoid NPE.</li>
 *   <li><strong>MapEntity Serialization:</strong> {@code setValueAsMap()} caches in transient field only.
 *       Use {@code updateValueFromMap()} before persist/merge to serialize to value column.</li>
 *   <li><strong>Role Privilege Set:</strong> {@code Role.privilegesSet} is transient. Always use
 *       {@code getPrivilegesSet()} for lazy initialization. {@code setPrivilegesSet()} synchronizes
 *       to privileges string via PrivilegeHelper serialization.</li>
 *   <li><strong>Organization Sequence:</strong> Organization uses dedicated sequence with initial value 122.
 *       Do not use GLOBAL_ID_GENERATOR for organization entities.</li>
 *   <li><strong>Computed Fields Non-Insertable:</strong> {@code @Formula} fields and database-generated
 *       columns (indexString, referenceString) are read-only. Attempting to set values has no effect.</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.Organization
 * @see com.openkoda.model.User
 * @see com.openkoda.model.Role
 * @see com.openkoda.model.Privilege
 * @see com.openkoda.model.UserRole
 */
package com.openkoda.model;