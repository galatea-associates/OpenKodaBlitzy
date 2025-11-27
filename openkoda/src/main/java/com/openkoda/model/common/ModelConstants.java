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

import java.text.SimpleDateFormat;

import static com.openkoda.controller.common.URLConstants.*;

/**
 * Central repository of JPA and database constants used throughout the OpenKoda domain model.
 * <p>
 * This interface provides a comprehensive set of constants for:
 * <ul>
 *   <li>Sequence generator names for entity ID generation</li>
 *   <li>Standard database column names (timestamps, foreign keys, search indexes)</li>
 *   <li>SQL formula expressions for computed fields</li>
 *   <li>Initial sequence values for different entity types</li>
 *   <li>Privilege system placeholders and column names</li>
 *   <li>Path formula bases for UI navigation URLs</li>
 * </ul>

 * <p>
 * This interface is typically implemented by base entity classes such as {@code TimestampedEntity} 
 * and {@code OpenkodaEntity} to provide convenient access to these constants throughout the domain model.
 * All constants defined here follow OpenKoda naming conventions and are used consistently across
 * JPA entity mappings, repository queries, and SQL formulas.

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.common.OpenkodaEntity
 * @see com.openkoda.model.common.TimestampedEntity
 * @see com.openkoda.model.common.SearchableEntity
 */
public interface ModelConstants {

   // ========================================
   // Sequence Generator Names
   // ========================================

   /**
    * Standard primary key column name used across all entity tables.
    * <p>
    * This constant defines the conventional column name "id" for primary key fields
    * in JPA entity mappings.

    */
   String ID = "id";

   /**
    * Sequence generator name for global (non-organization-scoped) entity IDs.
    * <p>
    * This sequence is used by entities that are not scoped to a specific organization,
    * such as system-wide configuration entities. The sequence uses {@code allocationSize=10}
    * for performance optimization, allowing Hibernate to pre-allocate ID blocks and reduce
    * database round-trips during entity persistence.

    *
    * @see #INITIAL_GLOBAL_VALUE
    */
   String GLOBAL_ID_GENERATOR = "seqGlobalId";

   /**
    * Sequence generator name specifically for Organization entity IDs.
    * <p>
    * This dedicated sequence generates primary keys for the Organization entity,
    * which represents tenants in the multi-tenancy architecture. Uses {@code allocationSize=10}
    * for batch ID allocation.

    *
    * @see #INITIAL_ORGANIZATION_VALUE
    * @see com.openkoda.model.Organization
    */
   String ORGANIZATION_ID_GENERATOR = "seqOrganizationId";

   /**
    * Sequence generator name for organization-scoped (tenant-aware) entity IDs.
    * <p>
    * This sequence is used by entities that extend {@code OpenkodaEntity} and are scoped
    * to specific organizations in the multi-tenancy system. These entities include an
    * {@code organization_id} foreign key column. The sequence employs {@code allocationSize=10}
    * to optimize batch inserts.

    *
    * @see #INITIAL_ORGANIZATION_RELATED_VALUE
    * @see #ORGANIZATION_ID
    * @see com.openkoda.model.common.OpenkodaEntity
    */
   String ORGANIZATION_RELATED_ID_GENERATOR = "seqOrganizationRelatedId";

   /**
    * Sequence generator name for bulk operation entity IDs.
    * <p>
    * This sequence is designated for mass data import or bulk entity creation scenarios
    * where high-volume ID generation is required. Uses the same performance optimization
    * strategy with pre-allocated ID blocks.

    */
   String MASS_ID_GENERATOR = "massGlobalId";

   // ========================================
   // Date Format
   // ========================================

   /**
    * Simple date formatter with pattern "dd/MM/yy" for date display.
    * <p>
    * <strong>THREAD-SAFETY WARNING:</strong> {@link SimpleDateFormat} is NOT thread-safe.
    * Concurrent access to this shared instance must be synchronized, or use {@link ThreadLocal}
    * wrappers for thread-local instances.

    * <p>
    * Example of safe usage:
    * <pre>
    * synchronized(ModelConstants.simpleDateFormat) {
    *     String formatted = ModelConstants.simpleDateFormat.format(date);
    * }
    * </pre>

    * <p>
    * For modern applications, consider using {@link java.time.format.DateTimeFormatter} which
    * is immutable and thread-safe.

    */
   SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "dd/MM/yy" );

   // ========================================
   // Search Index Constants
   // ========================================

   /**
    * Standard column name for full-text search index string.
    * <p>
    * This column stores a concatenated, searchable string representation of entity data
    * used for full-text search functionality. Entities implementing {@code SearchableEntity}
    * typically define this as a non-insertable, non-updatable column with {@code @ColumnDefault('')}.

    *
    * @see #INDEX_STRING_COLUMN_LENGTH
    * @see com.openkoda.model.common.SearchableEntity
    */
   String INDEX_STRING_COLUMN = "index_string";

   /**
    * Maximum length for the index string column in characters.
    * <p>
    * This defines the varchar length constraint for the {@code index_string} column
    * used in full-text search indexes. The value of 16300 characters provides sufficient
    * capacity for comprehensive entity data concatenation while remaining within database
    * varchar limits.

    *
    * @see #INDEX_STRING_COLUMN
    */
   int INDEX_STRING_COLUMN_LENGTH = 16300;

   // ========================================
   // Timestamp Column Names
   // ========================================

   /**
    * Standard column name for the last modification timestamp.
    * <p>
    * This column stores the date and time when an entity was last updated.
    * Used by {@code TimestampedEntity} in conjunction with the {@code @LastModifiedDate}
    * annotation from Spring Data JPA auditing.

    *
    * @see #CREATED_ON
    * @see com.openkoda.model.common.TimestampedEntity
    */
   String UPDATED_ON = "updated_on";

   /**
    * Standard column name for the creation timestamp.
    * <p>
    * This column stores the date and time when an entity was first persisted.
    * Used by {@code TimestampedEntity} in conjunction with the {@code @CreatedDate}
    * annotation from Spring Data JPA auditing.

    *
    * @see #UPDATED_ON
    * @see com.openkoda.model.common.TimestampedEntity
    */
   String CREATED_ON = "created_on";

   // ========================================
   // Foreign Key Column Names
   // ========================================

   /**
    * Standard foreign key column name for organization references.
    * <p>
    * This column links entities to their owning organization in the multi-tenancy system.
    * All entities extending {@code OpenkodaEntity} include this foreign key to provide
    * tenant-aware data isolation.

    *
    * @see #ORGANIZATION_RELATED_ID_GENERATOR
    * @see com.openkoda.model.Organization
    * @see com.openkoda.model.common.OpenkodaEntity
    */
   String ORGANIZATION_ID = "organization_id";

   /**
    * Standard foreign key column name for ServerJs references.
    * <p>
    * This column links entities to ServerJs script definitions used in the
    * GraalVM JavaScript integration subsystem.

    *
    * @see com.openkoda.model.component.ServerJs
    */
   String SERVER_JS_ID = "server_js_id";

   /**
    * Standard foreign key column name for FrontendResource references.
    * <p>
    * This column links entities to frontend resource definitions such as
    * JavaScript, CSS, or HTML template resources.

    *
    * @see com.openkoda.model.component.FrontendResource
    */
   String FRONTEND_RESOURCE_ID = "frontend_resource_id";

   /**
    * Standard column name for email addresses.
    * <p>
    * This provides a conventional column name for email fields across various
    * entity types in the domain model.

    */
   String EMAIL = "email";

   // ========================================
   // SQL Formula Constants
   // ========================================

   /**
    * SQL formula expression for computing organization-scoped entity reference strings.
    * <p>
    * This formula generates a standardized reference string in the format "OOOOO/IIIII"
    * where OOOOO is the zero-padded organization ID (5 digits) and IIIII is the entity ID.

    * <p>
    * Formula breakdown:
    * <ul>
    *   <li>{@code LPAD(..., 5, '0')} - Pads the organization ID to 5 digits with leading zeros</li>
    *   <li>{@code coalesce(organization_id, 0)} - Uses 0 if organization_id is NULL</li>
    *   <li>{@code || '/' || id} - Concatenates with '/' separator and entity ID</li>
    * </ul>

    * <p>
    * Example results:
    * <ul>
    *   <li>organization_id=123, id=45678 → "00123/45678"</li>
    *   <li>organization_id=NULL, id=789 → "00000/789"</li>
    * </ul>

    * <p>
    * This formula is used by {@code OpenkodaEntity} in the {@code referenceString} field
    * with the {@code @Formula} annotation to provide human-readable, sortable entity identifiers.

    *
    * @see com.openkoda.model.common.OpenkodaEntity
    * @see #ORGANIZATION_ID
    */
   String DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA =
           "(LPAD(coalesce(" + ORGANIZATION_ID + ", 0)||'', 5, '0') || '/' || id)";

   // ========================================
   // Initial Sequence Values
   // ========================================

   /**
    * Starting sequence value for global (non-organization-scoped) entity IDs.
    * <p>
    * This high initial value of 10,000 helps avoid conflicts with seed data and
    * enables range-based detection of entity types during data operations.
    * Global entities start their ID sequence from this value.

    *
    * @see #GLOBAL_ID_GENERATOR
    */
   int INITIAL_GLOBAL_VALUE = 10000;

   /**
    * Starting sequence value for privilege entity IDs.
    * <p>
    * Privilege entities begin ID allocation from 100,000 to clearly segregate
    * privilege identifiers from other entity types and reserve lower ranges
    * for system-defined privileges.

    *
    * @see com.openkoda.model.Privilege
    * @see com.openkoda.model.PrivilegeBase
    */
   int INITIAL_PRIVILEGE_VALUE = 100000;

   /**
    * Starting sequence value for Organization entity IDs.
    * <p>
    * Organization IDs begin at 122, which reserves IDs below this value for
    * seed organizations and system tenants created during initial database setup.

    *
    * @see #ORGANIZATION_ID_GENERATOR
    * @see com.openkoda.model.Organization
    */
   int INITIAL_ORGANIZATION_VALUE = 122;

   /**
    * Starting sequence value for organization-scoped (tenant-aware) entity IDs.
    * <p>
    * This high initial value of 120,150 for organization-related entities ensures
    * clear separation from global entities and provides ample room for seed data.
    * All entities extending {@code OpenkodaEntity} use this sequence starting point.

    *
    * @see #ORGANIZATION_RELATED_ID_GENERATOR
    * @see com.openkoda.model.common.OpenkodaEntity
    */
   int INITIAL_ORGANIZATION_RELATED_VALUE = 120150;

   // ========================================
   // Privilege System Placeholders
   // ========================================

   /**
    * Placeholder token for current user ID in privilege SQL formulas.
    * <p>
    * This token "##userId##" is replaced at runtime with the actual current user ID
    * when evaluating {@code @Formula} expressions in {@code EntityWithRequiredPrivilege} implementations.
    * This enables dynamic, user-specific privilege checks directly in SQL.

    * <p>
    * Example usage in a {@code @Formula} annotation:
    * <pre>
    * {@code @Formula("CASE WHEN user_id = ##userId## THEN NULL ELSE 'canReadUserData' END")}
    * private String requiredReadPrivilege;
    * </pre>

    * <p>
    * The formula returns NULL (no privilege required) if the entity belongs to the current user,
    * otherwise it requires the 'canReadUserData' privilege.

    *
    * @see com.openkoda.model.common.EntityWithRequiredPrivilege
    */
   String USER_ID_PLACEHOLDER = "##userId##";

   // ========================================
   // Privilege Column Names
   // ========================================

   /**
    * Legacy database column name for required privilege.
    * <p>
    * This column name "required_privilege" represents the legacy approach to
    * storing privilege requirements. Modern implementations use separate
    * read and write privilege columns.

    *
    * @see #REQUIRED_PRIVILEGE
    */
   String REQUIRED_PRIVILEGE_COLUMN = "required_privilege";

   /**
    * Java field name for the required privilege property (legacy).
    * <p>
    * This camelCase field name corresponds to the {@code required_privilege}
    * database column for legacy entities with unified privilege checking.

    *
    * @see #REQUIRED_PRIVILEGE_COLUMN
    */
   String REQUIRED_PRIVILEGE = "requiredPrivilege";

   /**
    * Java field name for the required read privilege property.
    * <p>
    * This field stores the privilege name required to read (view) an entity.
    * Used by {@code EntityWithRequiredPrivilege} implementations to enforce
    * fine-grained, row-level read access control.

    *
    * @see #REQUIRED_READ_PRIVILEGE_COLUMN
    * @see #REQUIRED_WRITE_PRIVILEGE
    * @see com.openkoda.model.common.EntityWithRequiredPrivilege
    */
   String REQUIRED_READ_PRIVILEGE = "requiredReadPrivilege";

   /**
    * Java field name for the required write privilege property.
    * <p>
    * This field stores the privilege name required to modify (update/delete) an entity.
    * Used by {@code EntityWithRequiredPrivilege} implementations to enforce
    * fine-grained, row-level write access control.

    *
    * @see #REQUIRED_READ_PRIVILEGE
    * @see com.openkoda.model.common.EntityWithRequiredPrivilege
    */
   String REQUIRED_WRITE_PRIVILEGE = "requiredWritePrivilege";

   /**
    * Database column name for the required read privilege.
    * <p>
    * This column "required_read_privilege" stores the privilege name string
    * that must be possessed by a user to read the entity. Computed via
    * {@code @Formula} annotations using conditional logic and {@link #USER_ID_PLACEHOLDER}.

    *
    * @see #REQUIRED_READ_PRIVILEGE
    * @see #USER_ID_PLACEHOLDER
    */
   String REQUIRED_READ_PRIVILEGE_COLUMN = "required_read_privilege";

   // ========================================
   // Path Formula Bases for UI Navigation
   // ========================================

   /**
    * Base path formula for global (non-organization-scoped) entity URLs.
    * <p>
    * This constant provides the starting path "/html/" for constructing navigation
    * URLs to global entities that are not scoped to specific organizations.

    * <p>
    * Example usage in {@code @SearchableRepositoryMetadata} path formulas for
    * generating entity detail and settings page URLs.

    *
    * @see #ID_PATH_FORMULA
    * @see #ID_VIEW_PATH_FORMULA
    */
   String GLOBAL_PATH_FORMULA_BASE = "'" + _HTML + "/";

   /**
    * Base path formula for organization-scoped entity URLs.
    * <p>
    * This SQL formula conditionally includes the organization context in the URL path.
    * For organization-scoped entities, it generates paths like:
    * {@code /html/organization/{organizationId}/...}

    * <p>
    * Formula logic:
    * <ul>
    *   <li>If {@code organization_id} is NULL: returns "/html/"</li>
    *   <li>If {@code organization_id} has a value: returns "/html/organization/{organizationId}/"</li>
    * </ul>

    * <p>
    * This enables tenant-aware URL generation for multi-tenancy navigation where
    * organization context is preserved in the URL structure.

    *
    * @see #ORGANIZATION_ID
    * @see #ID_PATH_FORMULA
    * @see com.openkoda.model.common.OpenkodaEntity
    */
   String ORG_RELATED_PATH_FORMULA_BASE = "'" + _HTML + "' || case when " + ORGANIZATION_ID + " is null then '' else '" + _ORGANIZATION + "/'||" + ORGANIZATION_ID + " end || '/";

   /**
    * Path formula suffix for entity settings pages.
    * <p>
    * This formula fragment appends "/{id}/settings" to a base path, constructing
    * URLs that navigate to entity settings/configuration pages.

    * <p>
    * Combined with {@link #GLOBAL_PATH_FORMULA_BASE} or {@link #ORG_RELATED_PATH_FORMULA_BASE},
    * produces paths like: {@code /html/user/12345/settings}

    *
    * @see #ID_VIEW_PATH_FORMULA
    */
   String ID_PATH_FORMULA = "/' || id || '" + _SETTINGS + "'";

   /**
    * Path formula suffix for entity view/detail pages.
    * <p>
    * This formula fragment appends "/{id}/view" to a base path, constructing
    * URLs that navigate to entity detail/view pages.

    * <p>
    * Combined with {@link #GLOBAL_PATH_FORMULA_BASE} or {@link #ORG_RELATED_PATH_FORMULA_BASE},
    * produces paths like: {@code /html/organization/123/user/45678/view}

    *
    * @see #ID_PATH_FORMULA
    */
   String ID_VIEW_PATH_FORMULA = "/' || id || '" + _VIEW + "'";
}
