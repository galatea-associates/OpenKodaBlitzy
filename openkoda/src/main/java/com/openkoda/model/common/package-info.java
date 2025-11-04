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
 * Provides foundational base classes, interfaces, constants, and annotations for OpenKoda's domain model.
 * <p>
 * This package contains the shared JPA contracts, {@code @MappedSuperclass} base entities, multi-tenancy support,
 * audit trail infrastructure, and search indexing capabilities that underpin the entire application data model.
 * 
 *
 * <b>Package Contents Overview</b>
 *
 * <b>Key Base Classes</b>
 * <ul>
 *   <li>{@link com.openkoda.model.common.TimestampedEntity}: {@code @MappedSuperclass} providing automatic audit
 *       timestamps (createdOn, updatedOn, createdBy, modifiedBy) via Spring Data JPA auditing</li>
 *   <li>{@link com.openkoda.model.common.OpenkodaEntity}: Core {@code @MappedSuperclass} adding organization scope,
 *       timestamps, properties Map for flexible key-value storage, and search indexString</li>
 *   <li>{@link com.openkoda.model.common.ComponentEntity}: Module-scoped entity base class extending OpenkodaEntity
 *       with association to {@code OpenkodaModule}</li>
 *   <li>{@link com.openkoda.model.common.Audit}: JPA entity storing persistent audit trail records with operation
 *       type, severity, change details, and request correlation</li>
 * </ul>
 *
 * <b>Foundational Interfaces</b>
 * <ul>
 *   <li>{@link com.openkoda.model.common.LongIdEntity}: Root interface providing {@code getId()} contract and
 *       Serializable support for all entities</li>
 *   <li>{@link com.openkoda.model.common.AuditableEntity}: Contract for {@code toAuditString()} audit trail
 *       generation with optional property exclusion via {@code ignorePropertiesInAudit()}</li>
 *   <li>{@link com.openkoda.model.common.OrganizationRelatedEntity}: Multi-tenancy contract exposing
 *       {@code getOrganizationId()} and {@code getReferenceString()} for organization-scoped entities</li>
 *   <li>{@link com.openkoda.model.common.SearchableEntity}: Search index contract providing {@code getIndexString()}
 *       for full-text search capabilities</li>
 *   <li>{@link com.openkoda.model.common.EntityWithRequiredPrivilege}: Per-instance privilege enforcement contract
 *       with {@code getRequiredReadPrivilege()} and {@code getRequiredWritePrivilege()} accessors</li>
 * </ul>
 *
 * <b>Composite Marker Interfaces</b>
 * <ul>
 *   <li>{@link com.openkoda.model.common.SearchableOrganizationRelatedEntity}: Combines organization scope and
 *       search capabilities (extends {@code OrganizationRelatedEntity} and {@code SearchableEntity})</li>
 *   <li>{@link com.openkoda.model.common.AuditableEntityOrganizationRelated}: Combines audit trail support and
 *       organization scope (extends {@code AuditableEntity} and {@code OrganizationRelatedEntity})</li>
 * </ul>
 *
 * <b>Annotations</b>
 * <ul>
 *   <li>{@link com.openkoda.model.common.SearchableRepository}: Repository-level annotation configuring search
 *       index update SQL via {@code indexUpdateSql()} element</li>
 *   <li>{@link com.openkoda.model.common.SearchableRepositoryMetadata}: Comprehensive annotation providing entity
 *       key, entity class, global search inclusion flag, description formula, search index formula, and path formulas
 *       for UI navigation</li>
 *   <li>{@link com.openkoda.model.common.IndexStringColumn}: Field-level annotation specifying SQL expression for
 *       populating search index columns</li>
 *   <li>{@link com.openkoda.model.common.TenantedTable}: Type-level annotation marking entity tables as
 *       tenant-scoped for multi-tenancy discovery and partitioning</li>
 * </ul>
 *
 * <b>Constants Interface</b>
 * <ul>
 *   <li>{@link com.openkoda.model.common.ModelConstants}: Central repository defining sequence generator names,
 *       column names and lengths, timestamp column names, SQL formula fragments, initial sequence values, privilege
 *       placeholders (USER_ID_PLACEHOLDER), and path formula bases</li>
 * </ul>
 *
 * <b>Base Class Hierarchy</b>
 * <p>
 * The entity inheritance pattern follows a layered approach:
 * 
 * <pre>
 * LongIdEntity (root interface)
 *   ↓
 * TimestampedEntity (@MappedSuperclass with audit timestamps)
 *   ↓
 * OpenkodaEntity (@MappedSuperclass with organization scope, properties Map, search index)
 *   ↓
 * ComponentEntity (@MappedSuperclass with module association)
 *   ↓
 * [Domain entity implementations]
 * </pre>
 * <p>
 * Interface composition for OpenkodaEntity:
 * 
 * <ul>
 *   <li>Implements {@code SearchableOrganizationRelatedEntity} (= OrganizationRelatedEntity + SearchableEntity)</li>
 *   <li>Implements {@code AuditableEntityOrganizationRelated} (= AuditableEntity + OrganizationRelatedEntity)</li>
 *   <li>Implements {@code EntityWithRequiredPrivilege}</li>
 *   <li>Implements {@code ModelConstants}</li>
 * </ul>
 *
 * <b>JPA Conventions</b>
 * <ul>
 *   <li><b>Inheritance Strategy:</b> {@code @MappedSuperclass} with TABLE_PER_CLASS approach where each concrete
 *       entity class gets its own database table</li>
 *   <li><b>Sequence Generators:</b> ORGANIZATION_RELATED_ID_GENERATOR with {@code allocationSize=10} for performance
 *       optimization (Hibernate pre-allocates IDs in batches)</li>
 *   <li><b>Audit Fields:</b> Spring Data JPA {@code @EntityListeners(AuditingEntityListener.class)} enables
 *       automatic population of {@code @CreatedBy}, {@code @CreatedDate}, {@code @LastModifiedBy}, and
 *       {@code @LastModifiedDate} annotations</li>
 *   <li><b>Computed Fields:</b> {@code @Formula} annotations generate SQL expressions for referenceString and
 *       privilege fields evaluated at query time</li>
 *   <li><b>Column Defaults:</b> {@code @ColumnDefault} annotations specify database defaults for indexString and
 *       timestamp columns</li>
 *   <li><b>Non-Insertable Columns:</b> indexString and referenceString columns are typically non-insertable,
 *       populated by database triggers or scheduled jobs</li>
 * </ul>
 *
 * <b>Multi-Tenancy Pattern</b>
 * <p>
 * OpenKoda implements organization-scoped data isolation through the following mechanisms:
 * 
 * <ul>
 *   <li><b>Organization Foreign Key:</b> Entities implement {@code OrganizationRelatedEntity} with organizationId
 *       column referencing the Organization entity</li>
 *   <li><b>Automatic Query Filtering:</b> {@code SecureRepository} wrappers automatically filter queries by the
 *       current user's accessible organizations</li>
 *   <li><b>Tenant Discovery:</b> {@code @TenantedTable} annotation marks entities for tenant-scoping discovery and
 *       database partitioning logic</li>
 *   <li><b>Cascade Deletion:</b> Organization removal triggers cascade deletion of tenant-scoped data via
 *       {@code remove_organizations_by_id.sql} procedure</li>
 * </ul>
 *
 * <b>Audit Trail Pattern</b>
 * <p>
 * Comprehensive audit trail functionality includes:
 * 
 * <ul>
 *   <li><b>Automatic Timestamps:</b> Spring Data auditing populates createdOn, updatedOn, createdBy, and modifiedBy
 *       fields on entity changes</li>
 *   <li><b>Audit String Generation:</b> {@code AuditableEntity.toAuditString()} is invoked by
 *       {@code AuditInterceptor} during Hibernate session flush to capture entity state</li>
 *   <li><b>Persistent Audit Records:</b> {@code Audit} entity stores change records with entityName, entityId,
 *       operation type, severity level, change details, and request correlation ID</li>
 *   <li><b>Selective Auditing:</b> {@code ignorePropertiesInAudit()} method allows entities to exclude sensitive
 *       fields (passwords, tokens) from audit trail</li>
 * </ul>
 *
 * <b>Search Index Pattern</b>
 * <p>
 * Full-text search capabilities are enabled through:
 * 
 * <ul>
 *   <li><b>Index String Column:</b> {@code SearchableEntity.getIndexString()} provides access to the indexString
 *       column containing concatenated searchable entity data</li>
 *   <li><b>Search Annotations:</b> {@code @SearchableRepository} and {@code @SearchableRepositoryMetadata} configure
 *       index update SQL expressions for scheduled jobs</li>
 *   <li><b>Index Population:</b> Background jobs execute UPDATE statements using {@code searchIndexFormula} to
 *       populate indexString columns with current entity data</li>
 *   <li><b>Global Search Control:</b> {@code includeInGlobalSearch} flag in {@code @SearchableRepositoryMetadata}
 *       controls whether entities appear in application-wide search results</li>
 * </ul>
 *
 * <b>Privilege Enforcement Pattern</b>
 * <p>
 * Per-instance privilege requirements are implemented through:
 * 
 * <ul>
 *   <li><b>Privilege Contract:</b> {@code EntityWithRequiredPrivilege} interface exposes
 *       {@code getRequiredReadPrivilege()} and {@code getRequiredWritePrivilege()} returning privilege name strings
 *       or null for public access</li>
 *   <li><b>Dynamic Formulas:</b> {@code @Formula} annotations compute privilege names using SQL expressions that may
 *       include {@code USER_ID_PLACEHOLDER} token replaced with current user ID in queries</li>
 *   <li><b>Query-Time Enforcement:</b> {@code SecureRepository} evaluates required privileges when executing queries,
 *       filtering results based on current user's granted privileges</li>
 *   <li><b>Null Semantics:</b> Returning null from privilege accessors indicates no privilege required, allowing
 *       public or owner-based access</li>
 * </ul>
 *
 * <b>Key Classes and Interfaces</b>
 * <p>
 * This package contains 18 source artifacts:
 * 
 * <ol>
 *   <li><b>LongIdEntity:</b> Root interface providing Long ID contract and Serializable support</li>
 *   <li><b>TimestampedEntity:</b> @MappedSuperclass with automatic audit timestamp fields</li>
 *   <li><b>OpenkodaEntity:</b> Core @MappedSuperclass with organization scope and properties storage</li>
 *   <li><b>ComponentEntity:</b> @MappedSuperclass for module-scoped entities</li>
 *   <li><b>AuditableEntity:</b> Interface contract for audit trail string generation</li>
 *   <li><b>OrganizationRelatedEntity:</b> Multi-tenancy interface for organization-scoped entities</li>
 *   <li><b>SearchableEntity:</b> Interface contract for search index string access</li>
 *   <li><b>EntityWithRequiredPrivilege:</b> Interface contract for per-instance privilege enforcement</li>
 *   <li><b>SearchableOrganizationRelatedEntity:</b> Composite marker combining search and organization scope</li>
 *   <li><b>AuditableEntityOrganizationRelated:</b> Composite marker combining audit and organization scope</li>
 *   <li><b>Audit:</b> JPA entity for persistent audit trail storage</li>
 *   <li><b>ModelConstants:</b> Central constants repository for sequences, columns, formulas, and placeholders</li>
 *   <li><b>@SearchableRepository:</b> Repository-level search configuration annotation</li>
 *   <li><b>@SearchableRepositoryMetadata:</b> Comprehensive search and navigation metadata annotation</li>
 *   <li><b>@IndexStringColumn:</b> Field-level index SQL configuration annotation</li>
 *   <li><b>@TenantedTable:</b> Type-level multi-tenancy marking annotation</li>
 *   <li><b>TimestampedEntity.UID:</b> Embeddable user identifier composite (name + ID) for audit fields</li>
 *   <li><b>package-info:</b> This package documentation file</li>
 * </ol>
 *
 * <b>Design Patterns</b>
 * <ul>
 *   <li><b>Template Method:</b> Base classes (TimestampedEntity, OpenkodaEntity) provide common infrastructure with
 *       hooks that subclasses override for specific behavior</li>
 *   <li><b>Strategy:</b> Privilege formulas encapsulate access control logic as pluggable SQL expressions evaluated
 *       at query time</li>
 *   <li><b>Marker Interface:</b> Composite interfaces (SearchableOrganizationRelatedEntity) type-bind multiple
 *       contracts without adding methods</li>
 * </ul>
 *
 * <b>Usage Examples</b>
 *
 * <b>Example 1: Creating Organization-Scoped Entity</b>
 * <pre>{@code
 * @Entity
 * public class CustomEntity extends OpenkodaEntity {
 *     // Inherits: id, organizationId, timestamps, properties, indexString, referenceString
 * }
 * }</pre>
 *
 * <b>Example 2: Implementing Audit Trail</b>
 * <pre>{@code
 * @Override
 * public String toAuditString() {
 *     return "CustomEntity: " + getName() + " (ID: " + getId() + ")";
 * }
 * }</pre>
 *
 * <b>Example 3: Per-Instance Privilege with User ID Placeholder</b>
 * <pre>{@code
 * @Formula("CASE WHEN user_id = ##userId## THEN NULL ELSE 'canReadData' END")
 * private String requiredReadPrivilege;
 * }</pre>
 *
 * <b>Thread Safety</b>
 * <ul>
 *   <li><b>ModelConstants.simpleDateFormat:</b> NOT thread-safe. Synchronize access or use ThreadLocal wrapper when
 *       formatting dates</li>
 *   <li><b>Entity Instances:</b> Not thread-safe per JPA specification. Avoid sharing entity instances across
 *       threads</li>
 *   <li><b>TimestampedEntity.UID:</b> Embeddable is effectively immutable after construction, safe for concurrent
 *       reads</li>
 * </ul>
 *
 * @see com.openkoda.model Main domain entities package using these base classes
 * @see com.openkoda.repository Data access layer with SecureRepository implementations
 * @see com.openkoda.core.audit Audit infrastructure including AuditInterceptor
 * @see com.openkoda.core.multitenancy Multi-tenancy infrastructure and organization-scoped operations
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
package com.openkoda.model.common;