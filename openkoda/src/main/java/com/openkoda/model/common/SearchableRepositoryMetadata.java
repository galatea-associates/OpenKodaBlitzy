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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runtime repository-level annotation providing comprehensive search and navigation metadata.
 * <p>
 * This annotation is applied to Spring Data JPA repository interfaces to provide richer metadata
 * for search indexing, UI navigation, and global search integration. It extends the simpler
 * {@code @SearchableRepository} annotation with additional configuration options for entity
 * discovery and display.

 * <p>
 * Classpath scanners and background indexing jobs use reflection to discover repositories
 * annotated with {@code @SearchableRepositoryMetadata} at runtime. The metadata enables
 * automatic search result rendering, autocomplete suggestions, and dynamic navigation path
 * generation for both global and organization-scoped entity access.

 * <p>
 * Example usage:
 * <pre>{@code
 * @SearchableRepositoryMetadata(
 *     entityKey = "user",
 *     entityClass = User.class,
 *     includeInGlobalSearch = true,
 *     descriptionFormula = "(first_name||' '||last_name||' <'||email||'>')",
 *     searchIndexFormula = "(first_name||' '||last_name||' '||email||' '||COALESCE(phone,''))",
 *     globalPathFormula = "('/admin/users/'||id)"
 * )
 * public interface UserRepository extends JpaRepository<User, Long> {
 * }
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SearchableEntity
 * @see SearchableRepository
 * @see IndexStringColumn
 * @see com.openkoda.model.common.ModelConstants#INDEX_STRING_COLUMN
 */
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
//@SearchableRepository
public @interface SearchableRepositoryMetadata {

    /**
     * Returns the unique string identifier for the entity type.
     * <p>
     * The entity key is used in search results, routing, and UI display to uniquely identify
     * the entity type across the application. It must be unique across all searchable entities.

     * <p>
     * Example values: "user", "organization", "form", "dynamic_entity_person"

     *
     * @return entity key string, must be unique across all searchable entities
     */
    String entityKey();

    /**
     * Returns the Class reference to the SearchableEntity implementation.
     * <p>
     * This enables type-safe entity access and repository lookup during search operations
     * and navigation path generation.

     * <p>
     * Example values: User.class, Organization.class, DynamicEntity.class

     *
     * @return Class object for entity implementing SearchableEntity
     */
    Class<? extends SearchableEntity> entityClass();

    /**
     * Returns a flag controlling whether this entity appears in global search results.
     * <p>
     * When set to {@code true}, the entity will be included in global search operations.
     * Set to {@code false} to exclude internal or configuration entities from user-facing
     * global search results.

     * <p>
     * Default value: {@code false} (entity excluded from global search)

     * <p>
     * Use case: Set {@code true} for primary user-facing entities like users, organizations,
     * and forms. Set {@code false} for internal entities like tokens, configuration records,
     * and system metadata.

     *
     * @return {@code true} to include in global search, {@code false} to exclude
     */
    boolean includeInGlobalSearch() default false;

    /**
     * Returns the SQL expression to generate human-readable description for search results.
     * <p>
     * This SQL fragment is used to compute a display string for each entity instance in
     * search results and autocomplete suggestions. The expression is evaluated in the database
     * context and should produce a readable string representation.

     * <p>
     * Default value: {@code "(''||id)"} - displays the entity ID

     * <p>
     * Example expressions:
     * <ul>
     *   <li>{@code "(name||' - '||email)"} - for User entities</li>
     *   <li>{@code "(title||' ('||status||')')"} - for Form entities</li>
     *   <li>{@code "(code||': '||description)"} - for categorized entities</li>
     * </ul>

     *
     * @return SQL fragment producing entity description string
     */
    String descriptionFormula() default "(''||id)";

    /**
     * Returns the SQL expression to populate the indexString column for full-text search.
     * <p>
     * This SQL fragment is used by indexing jobs to compute the searchable text for each
     * entity instance. The expression should concatenate all fields that should be searchable,
     * typically including names, descriptions, tags, and other textual fields.

     * <p>
     * Default value: {@code "(''||id)"} - indexes by ID only

     * <p>
     * Example expressions:
     * <ul>
     *   <li>{@code "(name||' '||description||' '||COALESCE(tags,''))"}</li>
     *   <li>{@code "(first_name||' '||last_name||' '||email||' '||COALESCE(phone,''))"}</li>
     *   <li>{@code "(title||' '||content||' '||COALESCE(keywords,''))"}</li>
     * </ul>

     * <p>
     * The computed value is stored in the {@code indexString} column and used for efficient
     * full-text search queries.

     *
     * @return SQL fragment for index string computation
     */
    String searchIndexFormula() default "(''||id)";

    /**
     * Returns the SQL expression generating URL path for global (non-organization-scoped) entity access.
     * <p>
     * This SQL fragment produces the navigation URL for accessing the entity in a global context,
     * outside of any specific organization scope. When empty, the entity has no global access path.

     * <p>
     * Default value: empty string (no global path)

     * <p>
     * Example expressions:
     * <ul>
     *   <li>{@code "('/admin/users/'||id)"} - for global user management</li>
     *   <li>{@code "('/settings/modules/'||id)"} - for system-wide module configuration</li>
     *   <li>{@code "('/reports/'||id||'/view')"} - for global report access</li>
     * </ul>

     * <p>
     * The generated path is used in search result links and navigation menus.

     *
     * @return SQL fragment producing URL path, or empty string for no global access
     */
    String globalPathFormula() default "";

    /**
     * Returns the SQL expression generating URL path for organization-scoped entity access.
     * <p>
     * This SQL fragment produces the navigation URL for accessing the entity within a specific
     * organization context (multi-tenant scope). The expression typically includes the organization ID
     * to ensure proper tenant isolation.

     * <p>
     * Default value: empty string (no organization-scoped path)

     * <p>
     * Example expressions:
     * <ul>
     *   <li>{@code "('/organization/'||organization_id||'/forms/'||id)"}</li>
     *   <li>{@code "('/org/'||organization_id||'/reports/'||id||'/edit')"}</li>
     *   <li>{@code "('/tenant/'||organization_id||'/entities/'||table_name||'/'||id)"}</li>
     * </ul>

     * <p>
     * The generated path is used in tenant-scoped navigation and search results within the
     * organization context.

     *
     * @return SQL fragment producing org-scoped URL path, or empty string for no organization access
     */
    String organizationRelatedPathFormula() default "";

}