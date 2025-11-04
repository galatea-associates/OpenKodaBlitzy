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

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Formula;

import java.util.Collection;

/**
 * Represents an audit trail entry created by the system to track security-relevant and business-critical events.
 * <p>
 * This JPA entity captures detailed audit information including user actions, data access, entity modifications,
 * and system operations. Each audit record is organization-scoped (multi-tenant aware) and includes correlation
 * identifiers for distributed tracing. The audit subsystem uses this entity to maintain compliance, support
 * forensic analysis, and enable operational monitoring.

 * <p>
 * The entity extends {@link TimestampedEntity} to automatically track creation and modification timestamps,
 * implements {@link SearchableEntity} to enable full-text search via the indexString field, and implements
 * {@link OrganizationRelatedEntity} to enforce tenant isolation through organization-scoped queries.

 * <p>
 * Key features:
 * <ul>
 *   <li>Automatic timestamp management via TimestampedEntity (createdOn, updatedOn)</li>
 *   <li>Organization-scoped audit records for multi-tenancy support</li>
 *   <li>Request correlation via requestId field linked to LoggingComponentWithRequestId</li>
 *   <li>Flexible change tracking with varchar(16380) change field and TEXT content field</li>
 *   <li>Full-text search support through computed indexString column</li>
 *   <li>Computed reference string using DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA</li>
 * </ul>

 * <p>
 * Example usage:
 * <pre>{@code
 * Audit audit = new Audit();
 * audit.setOperation(AuditOperation.EDIT);
 * audit.setSeverity(Severity.INFO);
 * }</pre>

 * <p>
 * The entity is persisted to the 'audit' table with id generated using IDENTITY strategy.
 * Audit records are created by the core auditing subsystem (AuditInterceptor) during entity
 * lifecycle events and security-sensitive operations.

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see TimestampedEntity
 * @see SearchableEntity
 * @see OrganizationRelatedEntity
 * @see com.openkoda.core.audit.AuditInterceptor
 */
@Entity
public class Audit extends TimestampedEntity implements SearchableEntity, OrganizationRelatedEntity {

   /**
    * SQL formula for computing the referenceString field.
    * <p>
    * Uses the default organization-related reference field formula from ModelConstants
    * to generate a human-readable reference string combining organization and entity identifiers.

    *
    * @see com.openkoda.model.common.ModelConstants#DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA
    */
   public static final String REFERENCE_FORMULA = DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA;

   /**
    * Defines the type of operation performed that triggered the audit event.
    * <p>
    * This enum categorizes audit records by the nature of the action taken on the entity.
    * The operation type is persisted as a String value using {@link EnumType#STRING}.

    * <ul>
    *   <li>{@code ADD} - Entity creation or new data insertion</li>
    *   <li>{@code EDIT} - Entity modification or data update</li>
    *   <li>{@code DELETE} - Entity removal or data deletion</li>
    *   <li>{@code BROWSE} - Data access or view operation (read-only)</li>
    *   <li>{@code ASSIGN} - Assignment or association operation (e.g., role assignment, privilege grant)</li>
    * </ul>
    */
   public enum AuditOperation {
      ADD, EDIT, DELETE, BROWSE, ASSIGN;
   }

   /**
    * Defines the severity level of the audit event for categorization and alerting.
    * <p>
    * This enum classifies audit records by their importance or risk level, enabling
    * filtering, alerting, and compliance reporting. The severity level is persisted
    * as a String value using {@link EnumType#STRING}.

    * <ul>
    *   <li>{@code INFO} - Informational event for normal operations and routine activities</li>
    *   <li>{@code WARNING} - Potentially concerning event requiring attention but not critical</li>
    *   <li>{@code ERROR} - Critical event indicating security violation, system failure, or policy breach</li>
    * </ul>
    */
   public enum Severity {
      INFO, WARNING, ERROR
   }


   /**
    * Primary key for the audit record.
    * <p>
    * Generated using database IDENTITY strategy for optimal performance with PostgreSQL sequences.

    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   /**
    * ID of the user who triggered the audit event.
    * <p>
    * References the User entity's primary key. May be null for system-initiated operations
    * or unauthenticated requests.

    */
   @Column
   private Long userId;

   /**
    * Comma-separated list of role IDs associated with the user at the time of the audit event.
    * <p>
    * Stored as a varchar(2047) string representation of the role ID collection. This captures
    * the security context of the user when the audited action occurred, enabling retrospective
    * privilege analysis. Set via {@link #setUserRoleIds(Collection)} which converts the collection
    * to a string representation.

    */
   @Column(length=2047)
   private String userRoleIds;

   /**
    * <p>Getter for the field <code>userRoleIds</code>.</p>
    *
    * @return a {@link java.lang.String} object.
    */
   public String getUserRoleIds() {
      return userRoleIds;
   }

   /**
    * Sets the user role IDs from a collection, converting to a comma-separated string representation.
    * <p>
    * This method accepts any Collection type and converts it to its string representation for storage.
    * Null collections are converted to an empty string. The collection typically contains Long role IDs
    * from the user's current security context.

    *
    * @param userRoleIds a {@link java.util.Collection} of role IDs (typically Long values), or null
    */
   public void setUserRoleIds(Collection<?> userRoleIds) {
      this.userRoleIds = userRoleIds == null ? "" : userRoleIds.toString();
   }

   /**
    * Fully qualified class name of the audited entity.
    * <p>
    * Stores the Java class name (e.g., "com.openkoda.model.Organization") of the entity
    * that was accessed or modified, enabling entity-type based audit queries and reports.

    */
   private String entityName;

   /**
    * Business key or natural identifier of the audited entity.
    * <p>
    * Stores a human-readable identifier for the entity (e.g., username, organization name)
    * to complement the entityId and provide meaningful audit trail readability without
    * requiring joins to the actual entity tables.

    */
   private String entityKey;

   /**
    * Type of operation performed that triggered this audit event.
    * <p>
    * Persisted as a String using {@link EnumType#STRING} for database readability
    * and forward compatibility. See {@link AuditOperation} for available operation types.

    *
    * @see AuditOperation
    */
   @Enumerated(EnumType.STRING)
   private AuditOperation operation;

   /**
    * Severity level of the audit event for risk classification.
    * <p>
    * Persisted as a String using {@link EnumType#STRING} for database readability.
    * Used for filtering audit records by importance level and triggering alerts
    * for WARNING and ERROR severity events.

    *
    * @see Severity
    */
   @Enumerated(EnumType.STRING)
   private Severity severity;

   /**
    * Primary key ID of the audited entity.
    * <p>
    * References the primary key of the entity identified by entityName. Combined with
    * entityName, provides a complete reference to the audited entity. May be null for
    * operations that don't target a specific entity (e.g., login attempts, system operations).

    */
   @Column
   private Long entityId;

   /**
    * Organization ID for multi-tenant audit record scoping.
    * <p>
    * Enforces tenant isolation by associating each audit record with a specific organization.
    * Used by SecureRepository and organization-scoped queries to ensure audit records are
    * only accessible within their tenant context. May be null for global system operations.

    */
   @Column
   private Long organizationId;

   /**
    * Detailed description of the changes made during the audit event.
    * <p>
    * Stored as varchar(16380) to accommodate detailed change tracking. Typically contains
    * before/after values, modified field names, or structured change metadata. For larger
    * change descriptions, use the content field which supports TEXT data type.

    */
   @Column(length=16380)
   private String change;

   /**
    * IP address of the client that initiated the audited operation.
    * <p>
    * Captures the source IP address for security analysis, geolocation tracking,
    * and forensic investigation. Supports both IPv4 and IPv6 address formats.

    */
   @Column
   private String ipAddress;

   /**
    * Request correlation ID for distributed tracing across the system.
    * <p>
    * Links this audit record to the request processing context managed by
    * LoggingComponentWithRequestId. Enables correlation of audit events with
    * application logs, performance traces, and error reports across service boundaries.

    *
    * @see com.openkoda.core.tracker.LoggingComponentWithRequestId
    */
   @Column
   private String requestId;

   /**
    * Large text field for storing extensive audit content.
    * <p>
    * Uses TEXT columnDefinition to support unlimited length content such as full
    * request/response payloads, serialized entity state, or detailed operation logs.
    * Use this field when the change field (varchar 16380) is insufficient.

    */
   @Column(columnDefinition = "TEXT")
   private String content;

   /**
    * Computed full-text search index string.
    * <p>
    * Non-insertable column with default empty string, populated by database triggers or
    * computed columns for full-text search functionality. Implements SearchableEntity
    * contract to enable text-based audit record queries.

    *
    * @see SearchableEntity
    */
   @Column(name = INDEX_STRING_COLUMN, length = INDEX_STRING_COLUMN_LENGTH, insertable = false)
   @ColumnDefault("''")
   private String indexString;

   /**
    * Computed human-readable reference string for the audit record.
    * <p>
    * Generated by database formula using REFERENCE_FORMULA (derived from
    * DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA in ModelConstants).
    * Combines organization and entity identifiers into a display-friendly format
    * for UI presentation and reporting.

    *
    * @see #REFERENCE_FORMULA
    * @see com.openkoda.model.common.ModelConstants#DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA
    */
   @Formula(REFERENCE_FORMULA)
   private String referenceString;

   /**
    * Returns the computed reference string for this audit record.
    * <p>
    * Implements {@link OrganizationRelatedEntity#getReferenceString()} to provide
    * a human-readable identifier combining organization and entity information.
    * The value is computed by the database using the REFERENCE_FORMULA.

    *
    * @return the computed reference string, or null if not yet persisted
    */
   @Override
   public String getReferenceString() {
      return referenceString;
   }


   /**
    * Returns the ID of the user who triggered the audit event.
    *
    * @return the user ID, or null if the operation was system-initiated
    */
   public Long getUserId() {
      return userId;
   }

   /**
    * Sets the ID of the user who triggered the audit event.
    *
    * @param userId the user ID, or null for system-initiated operations
    */
   public void setUserId(Long userId) {
      this.userId = userId;
   }

   /**
    * Returns the primary key of this audit record.
    *
    * @return the audit record ID, or null if not yet persisted
    */
   public Long getId() {
      return id;
   }

   /**
    * Returns the primary key ID of the audited entity.
    *
    * @return the entity ID, or null if not applicable
    */
   public Long getEntityId() {
      return entityId;
   }

   /**
    * Sets the primary key ID of the audited entity.
    *
    * @param entityId the entity ID, or null if not applicable to this audit event
    */
   public void setEntityId(Long entityId) {
      this.entityId = entityId;
   }

   /**
    * Returns the severity level of this audit event.
    *
    * @return the severity level (INFO, WARNING, or ERROR)
    */
   public Severity getSeverity() {
      return severity;
   }

   /**
    * Sets the severity level for risk classification.
    *
    * @param severity the severity level (INFO, WARNING, or ERROR)
    */
   public void setSeverity(Severity severity) {
      this.severity = severity;
   }

   /**
    * Returns the detailed description of changes made during the audit event.
    *
    * @return the change description, or null if not applicable
    */
   public String getChange() {
      return change;
   }

   /**
    * Sets the detailed description of changes.
    *
    * @param change the change description (maximum 16380 characters)
    */
   public void setChange(String change) {
      this.change = change;
   }

   /**
    * Returns the IP address of the client that initiated the audited operation.
    *
    * @return the client IP address (IPv4 or IPv6 format), or null if not available
    */
   public String getIpAddress() {
      return ipAddress;
   }

   /**
    * Sets the IP address of the client.
    *
    * @param ipAddress the client IP address (supports both IPv4 and IPv6 formats)
    */
   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   /**
    * Returns the request correlation ID for distributed tracing.
    *
    * @return the request correlation ID linked to LoggingComponentWithRequestId, or null
    */
   public String getRequestId() {
      return requestId;
   }

   /**
    * Sets the request correlation ID for linking audit events to application logs.
    *
    * @param requestId the request correlation ID from LoggingComponentWithRequestId
    */
   public void setRequestId(String requestId) {
      this.requestId = requestId;
   }

   /**
    * Returns the fully qualified class name of the audited entity.
    *
    * @return the entity class name (e.g., "com.openkoda.model.Organization"), or null
    */
   public String getEntityName() {
      return entityName;
   }

   /**
    * Sets the fully qualified class name of the audited entity.
    *
    * @param entityName the entity class name
    */
   public void setEntityName(String entityName) {
      this.entityName = entityName;
   }

   /**
    * Returns the business key or natural identifier of the audited entity.
    *
    * @return the human-readable entity identifier (e.g., username, organization name), or null
    */
   public String getEntityKey() {
      return entityKey;
   }

   /**
    * Sets the business key or natural identifier of the audited entity.
    *
    * @param entityKey the human-readable entity identifier for audit trail readability
    */
   public void setEntityKey(String entityKey) {
      this.entityKey = entityKey;
   }

   /**
    * Returns the type of operation that triggered this audit event.
    *
    * @return the audit operation type (ADD, EDIT, DELETE, BROWSE, or ASSIGN)
    */
   public AuditOperation getOperation() {
      return operation;
   }

   /**
    * Sets the type of operation for this audit event.
    *
    * @param operation the audit operation type
    */
   public void setOperation(AuditOperation operation) {
      this.operation = operation;
   }

   /**
    * Returns the organization ID for multi-tenant audit record scoping.
    *
    * @return the organization ID, or null for global system operations
    */
   public Long getOrganizationId() {
      return organizationId;
   }

   /**
    * Sets the organization ID for tenant isolation.
    *
    * @param organizationId the organization ID for tenant-scoped audit records
    */
   public void setOrganizationId(Long organizationId) {
      this.organizationId = organizationId;
   }

   /**
    * Returns the computed full-text search index string.
    * <p>
    * Implements {@link SearchableEntity#getIndexString()} to provide text-based
    * search functionality for audit records.

    *
    * @return the index string for full-text search, or empty string
    */
   @Override
   public String getIndexString() {
      return indexString;
   }

    /**
     * Returns the large text content field for extensive audit data.
     *
     * @return the audit content (unlimited length via TEXT column), or null
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the large text content field for storing extensive audit information.
     *
     * @param content the audit content (supports unlimited length via TEXT column type)
     */
    public void setContent(String content) {
        this.content = content;
    }
}
