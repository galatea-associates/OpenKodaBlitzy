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

import com.openkoda.model.common.EntityWithRequiredPrivilege;
import com.openkoda.model.common.ModelConstants;
import com.openkoda.model.common.SearchableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Immutable read-only projection entity mapped to global_search_view database view for cross-entity full-text search with privilege-based filtering.
 * <p>
 * Mapped to global_search_view via {@code @Subselect} and {@code @Immutable} annotations. Provides unified search interface across all searchable 
 * entities by projecting common fields (id, name, organizationId, timestamps, indexString, requiredReadPrivilege) into single queryable view. 
 * Read-only entity - insert/update operations disabled. Used by global search functionality to query across multiple entity types with single 
 * query while respecting privilege-based access control via requiredReadPrivilege column.
 * 
 * <p>
 * <b>Immutability:</b> {@code @Immutable} annotation prevents Hibernate from tracking changes or issuing UPDATE statements. Entity is snapshot 
 * of database view at query time.
 * 
 * <p>
 * <b>Search workflow:</b> Query GlobalEntitySearch with user's search term against indexString column, filter by user's privileges via 
 * requiredReadPrivilege, return unified search results across all entity types.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
@Entity
@Immutable
@Subselect("select * from global_search_view")
public class GlobalEntitySearch implements SearchableEntity, ModelConstants, EntityWithRequiredPrivilege {

    /**
     * Entity ID from source entity. Primary key in view.
     */
    @Id
    private long id;

    /**
     * Display title/name from source entity for search result presentation.
     */
    private String name;

    /**
     * Organization ID for multi-tenant filtering. Null for global entities.
     */
    private Long organizationId;

    /**
     * Audit timestamp - entity creation time projected from source entity.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdOn;

    /**
     * Audit timestamp - entity last update time projected from source entity.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updatedOn;

    /**
     * Description text from source entity for search result preview.
     */
    private String description;

    /**
     * Privilege token required to view this search result. Used for authorization filtering before presenting results to user.
     */
    @Column(name = ModelConstants.REQUIRED_READ_PRIVILEGE_COLUMN)
    private String requiredReadPrivilege;

    /**
     * URL path to entity detail view. Used for navigation from search results.
     */
    @Column(name = "urlpath")
    private String urlPath;

    /**
     * Full-text search index containing searchable content from source entity. Queried with text search operators.
     * Non-insertable column with database default empty string.
     */
    @Column(name = INDEX_STRING_COLUMN, length = INDEX_STRING_COLUMN_LENGTH, insertable = false)
    @ColumnDefault("''")
    private String indexString;

    /**
     * Returns the full-text search index string for this entity.
     * Implements {@link SearchableEntity#getIndexString()}.
     *
     * @return the search index string, never null due to database default
     */
    @Override
    public String getIndexString() {
        return indexString;
    }

    /**
     * Returns the entity ID from the source entity.
     *
     * @return the entity primary key
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the entity ID. Note: This entity is immutable ({@code @Immutable}), 
     * so setters should not be used after entity is loaded from database view.
     *
     * @param id the entity ID
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Returns the display name/title of the entity from the source entity.
     *
     * @return the entity name for display in search results
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the entity name. Note: This entity is immutable ({@code @Immutable}).
     *
     * @param name the entity name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the organization ID for multi-tenant filtering.
     *
     * @return the organization ID, or null for global entities
     */
    public long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the organization ID. Note: This entity is immutable ({@code @Immutable}).
     *
     * @param organizationId the organization ID
     */
    public void setOrganizationId(long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Returns the description text for search result preview.
     *
     * @return the entity description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description. Note: This entity is immutable ({@code @Immutable}).
     *
     * @param description the entity description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the URL path to the entity detail view.
     *
     * @return the URL path for navigation from search results
     */
    public String getUrlPath() {
        return urlPath;
    }

    /**
     * Sets the URL path. Note: This entity is immutable ({@code @Immutable}).
     *
     * @param urlPath the URL path
     */
    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    /**
     * Sets the search index string. Note: This entity is immutable ({@code @Immutable}),
     * and this field is non-insertable from application code.
     *
     * @param indexString the search index string
     */
    public void setIndexString(String indexString) {
        this.indexString = indexString;
    }

    /**
     * Returns the privilege token required to view this search result.
     * Implements {@link EntityWithRequiredPrivilege#getRequiredReadPrivilege()}.
     * Used for authorization filtering before presenting results to user.
     *
     * @return the required read privilege token
     */
    @Override
    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    /**
     * Returns null since this is a read-only view entity.
     * Implements {@link EntityWithRequiredPrivilege#getRequiredWritePrivilege()}.
     * Write operations are not supported on this immutable entity.
     *
     * @return always null for read-only entities
     */
    @Override
    public String getRequiredWritePrivilege() {
        return null;
    }

    /**
     * Sets the required read privilege. Note: This entity is immutable ({@code @Immutable}).
     *
     * @param requiredReadPrivilege the required read privilege token
     */
    public void setRequiredReadPrivilege(String requiredReadPrivilege) {
        this.requiredReadPrivilege = requiredReadPrivilege;
    }
}
