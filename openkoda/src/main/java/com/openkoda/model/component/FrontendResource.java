/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 and associated documentation files (the "Software"), to deal in the Software without restriction, 
including without limitation the rights to use, copy, modify, merge, publish, distribute, 
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software 
is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice 
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE 
AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS 
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES 
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.model.component;

import com.openkoda.model.PrivilegeNames;
import com.openkoda.model.common.ComponentEntity;
import com.openkoda.model.common.ModelConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Formula;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * JPA entity for storing frontend assets (HTML templates, JavaScript, CSS, JSON, CSV, XML)
 * used in UI rendering and dynamic page generation.
 * <p>
 * This entity enables the storage and management of frontend resources with support for
 * multi-tenancy, access control, and content versioning. Resources can be published content
 * or draft content pending review. The entity supports MD5-based change detection through
 * computed hash fields.
 * </p>
 * <p>
 * <strong>Multi-Tenancy:</strong> Resources are scoped by organization via the
 * {@code organization_id} column inherited from {@link ComponentEntity}. Access control
 * is further refined through the {@link AccessLevel} enum (PUBLIC, GLOBAL, ORGANIZATION, INTERNAL).
 * </p>
 * <p>
 * <strong>Content Storage:</strong> Supports dual content storage:
 * <ul>
 *   <li>{@code content} - Published content visible to end users</li>
 *   <li>{@code draftContent} - Unpublished draft for preview before publishing</li>
 * </ul>
 * Both content fields support up to 262,144 characters and have corresponding MD5 hash fields
 * for change detection.
 * </p>
 * <p>
 * <strong>Computed Fields:</strong> Uses Hibernate @Formula annotations for:
 * <ul>
 *   <li>{@code isPage} - True when name is not null</li>
 *   <li>{@code unsecured} - True when requiredPrivilege is null</li>
 *   <li>{@code draft} - True when draftContent exists</li>
 *   <li>{@code contentExists} - True when content is not null</li>
 *   <li>{@code contentHash} - Truncated MD5 hash of content (15 characters)</li>
 *   <li>{@code draftHash} - Truncated MD5 hash of draftContent (15 characters)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Relationships:</strong> Referenced by {@code ControllerEndpoint} entity which
 * associates dynamic REST endpoints with HTML templates stored in FrontendResource.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * FrontendResource page = new FrontendResource("home", "/home", "<html>...</html>", null, Type.HTML);
 * page.setAccessLevel(AccessLevel.PUBLIC);
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ComponentEntity
 * @see ControllerEndpoint
 */
@Entity
@Table (
    name = "frontend_resource",
    uniqueConstraints = {
            @UniqueConstraint(columnNames = {"name", "access_level", "organization_id"})
    }
)
public class FrontendResource extends ComponentEntity {

    /**
     * List of content field names used by indexing and diffing tools.
     * Contains "content" and "draftContent" fields.
     */
    final static List<String> contentProperties = Arrays.asList("content", "draftContent");
    
    /**
     * MD5 hash truncation length for performance optimization.
     * Hash values are truncated to 15 characters for storage and comparison.
     */
    public static final int HASH_TRUNCATED_LENGTH = 15;

    /**
     * Frontend resource file types with corresponding file extensions.
     * <p>
     * Each type maps to a specific file extension for content type detection
     * and URL routing. Supports JavaScript, CSS, JSON, CSV, plain text, XML,
     * and HTML resources.
     * </p>
     *
     * @see #getEntryTypeFromPath(String)
     */
    public enum Type {
        /** JavaScript resource with .js extension */
        JS(".js"),
        
        /** CSS stylesheet with .css extension */
        CSS(".css"),
        
        /** JSON data resource with .json extension */
        JSON(".json"),
        
        /** CSV data resource with .csv extension */
        CSV(".csv"),
        
        /** Plain text resource with .txt extension */
        TEXT(".txt"),
        
        /** XML data resource with .xml extension */
        XML(".xml"),
        
        /** HTML page or template with .html extension */
        HTML(".html");
        
        private String extension;

        /**
         * Constructs a Type with the specified file extension.
         *
         * @param extension the file extension including leading dot (e.g., ".js")
         */
        Type(String extension) {
            this.extension = extension;
        }

        /**
         * Returns the file extension for this type.
         *
         * @return the file extension including leading dot
         */
        public String getExtension() {
            return extension;
        }

        /**
         * Determines the resource Type from a file path by matching file extension.
         * <p>
         * Iterates through all Type values and returns the first match based on
         * file extension. If no match is found, defaults to HTML.
         * </p>
         *
         * @param path the file path to analyze
         * @return the matching Type, or HTML if no match found
         */
        public static Type getEntryTypeFromPath(String path) {
            for(Type t : values()) {
                if (path.endsWith(t.extension)) {
                    return t;
                }
            }
            return Type.HTML;
        }
    }

    /**
     * Resource category classification for organizational and functional grouping.
     * <p>
     * Defines the high-level category of the frontend resource which affects how
     * the resource is processed and displayed. Each category has an associated
     * field name used in data binding and UI rendering.
     * </p>
     */
    public enum ResourceType{
        /** Generic resource category with field name "resource" */
        RESOURCE("resource"),
        
        /** UI component category with field name "uiComponent" */
        UI_COMPONENT("uiComponent"),
        
        /** Dashboard widget category with field name "dashboard" */
        DASHBOARD("dashboard");

        private String fieldName;

        /**
         * Constructs a ResourceType with the specified field name.
         *
         * @param fieldName the field name for data binding
         */
        ResourceType(String fieldName) {this.fieldName = fieldName;}
    }

    /**
     * Resource visibility scope defining access control level and URL path prefix.
     * <p>
     * Controls which users can access the resource and determines the URL path
     * prefix for routing. Supports multi-tenancy by providing organization-level
     * isolation along with public and global visibility options.
     * </p>
     * <p>
     * Example URL construction: {@code /{path}{resourceName}} where path is the
     * AccessLevel path prefix (e.g., "public/", "organization/").
     * </p>
     */
    public enum AccessLevel {
        /** Publicly accessible resources with path prefix "public/" */
        PUBLIC("public/"),
        
        /** Globally accessible resources across all organizations with path prefix "global/" */
        GLOBAL("global/"),
        
        /** Organization-scoped resources with path prefix "organization/" */
        ORGANIZATION("organization/"),
        
        /** Internal system resources with path prefix "internal/" */
        INTERNAL("internal/");

        private String path;

        /**
         * Constructs an AccessLevel with the specified URL path prefix.
         *
         * @param path the URL path prefix including trailing slash
         */
        AccessLevel(String path) {
            this.path = path;
        }

        /**
         * Returns the URL path prefix for this access level.
         *
         * @return the path prefix with trailing slash
         */
        public String getPath() {
            return path;
        }

        /**
         * Sets the URL path prefix for this access level.
         *
         * @param path the path prefix to set
         */
        public void setPath(String path) {
            this.path = path;
        }
    }

    /**
     * Resource name, not null, part of unique constraint with access_level and organization_id.
     * Used as the primary identifier for the frontend resource.
     */
    @NotNull
    private String name;

    /**
     * Published content visible to end users, supports up to 262,144 characters.
     * Stores HTML templates, JavaScript code, CSS styles, JSON data, CSV data, XML, or text
     * depending on the resource type.
     */
    @Column(length = 65536 * 4)
    private String content;

    /**
     * Draft content for preview before publishing, supports up to 262,144 characters.
     * When present, indicates an unpublished version exists for review. Separate from
     * published content to allow safe editing without affecting live resources.
     */
    @Column(name = "draft_content", length = 65536 * 4)
    private String draftContent;

    /**
     * Optional privilege string for access control.
     * When set, only users with this privilege can access the resource.
     * Null value indicates unrestricted access (unsecured resource).
     */
    @Column(name = ModelConstants.REQUIRED_PRIVILEGE_COLUMN)
    private String requiredPrivilege;

    /**
     * Whether to include this resource in sitemap generation.
     * When true, the resource URL is added to the site's sitemap for search engine indexing.
     */
    @Column(name = "include_in_sitemap")
    private boolean includeInSitemap;

    /**
     * Computed field indicating if this resource is a page.
     * True when name is not null. Evaluated via @Formula annotation.
     */
    @Formula(" (name is not null) ")
    private boolean isPage;

    /**
     * Computed field indicating if this resource has no access restrictions.
     * True when requiredPrivilege is null. Evaluated via @Formula annotation.
     */
    @Formula(" (required_privilege is null) ")
    private boolean unsecured;

    /**
     * Computed field indicating if draft content exists.
     * True when draftContent is not null. Evaluated via @Formula annotation.
     */
    @Formula(" (draft_content is not null) ")
    private boolean draft;

    /**
     * Computed field indicating if published content exists.
     * True when content is not null. Evaluated via @Formula annotation.
     */
    @Formula(" (content is not null) ")
    private boolean contentExists;

    /**
     * Whether resource can be embedded in iframes.
     * Database default is false. When true, allows the resource to be embedded
     * in iframe elements on other pages.
     */
    @Column(name = "embeddable", columnDefinition = "boolean default false")
    private boolean embeddable;

    /**
     * Truncated MD5 hash of content for change detection.
     * Computed via @Formula using LEFT(MD5(content), 15).
     * Returns first 15 characters of MD5 hash for performance optimization.
     * Used to detect content changes without comparing full content strings.
     */
    @Formula(" (LEFT(MD5(content), " + HASH_TRUNCATED_LENGTH + " )) ")
    private String contentHash;

    /**
     * Truncated MD5 hash of draftContent for change detection.
     * Computed via @Formula using LEFT(MD5(draft_content), 15).
     * Returns first 15 characters of MD5 hash for performance optimization.
     * Used to detect draft changes without comparing full draft content strings.
     */
    @Formula(" (LEFT(MD5(draft_content), " + HASH_TRUNCATED_LENGTH + " )) ")
    private String draftHash;

    /**
     * Resource type enum defining file type and extension.
     * Defaults to HTML. Stored as STRING in database.
     * Determines content type handling and URL routing.
     */
    @Enumerated(EnumType.STRING)
    @NotNull
    private Type type = Type.HTML;

    /**
     * Resource category enum for organizational grouping.
     * Defaults to RESOURCE. Stored as STRING in resource_type column.
     * Classifies resource as generic RESOURCE, UI_COMPONENT, or DASHBOARD widget.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    @NotNull
    private ResourceType resourceType = ResourceType.RESOURCE;

    /**
     * Access level enum defining visibility scope and URL path prefix.
     * Defaults to PUBLIC. Stored as STRING in access_level column.
     * Controls multi-tenancy isolation: PUBLIC, GLOBAL, ORGANIZATION, or INTERNAL.
     */
    @Column(name = "access_level")
    @Enumerated(EnumType.STRING)
    private AccessLevel accessLevel = AccessLevel.PUBLIC;

    /**
     * Computed privilege required to read this FrontendResource entity.
     * Always evaluates to readFrontendResource privilege via @Formula annotation.
     * Overrides ComponentEntity.getRequiredReadPrivilege().
     */
    @Formula("( '" + PrivilegeNames._readFrontendResource + "' )")
    private String requiredReadPrivilege;

    /**
     * Computed privilege required to write/modify this FrontendResource entity.
     * Always evaluates to manageFrontendResource privilege via @Formula annotation.
     * Overrides ComponentEntity.getRequiredWritePrivilege().
     */
    @Formula("( '" + PrivilegeNames._manageFrontendResource + "' )")
    private String requiredWritePrivilege;

    /**
     * Constructs a FrontendResource with specified properties.
     * <p>
     * Creates a new frontend resource with the given name, content, type, and
     * optional privilege restriction. The resource is not associated with any
     * organization (organizationId is null).
     * </p>
     *
     * @param name the resource name, must not be null
     * @param urlPath the URL path (parameter not used in constructor body)
     * @param content the published content, may be null
     * @param requiredPrivilege the privilege enum required for access, null for unrestricted
     * @param type the resource type (JS, CSS, HTML, etc.), must not be null
     */
    public FrontendResource(String name, String urlPath, String content, Enum requiredPrivilege, Type type) {
        super(null);
        this.name = name;
        this.content = content;
        this.type = type;
        this.requiredPrivilege = requiredPrivilege == null ? null : requiredPrivilege.name();
    }

    /**
     * Constructs an empty FrontendResource for JPA.
     * <p>
     * No-argument constructor required by JPA specification. Sets organizationId to null
     * and initializes type to HTML (default), resourceType to RESOURCE (default),
     * and accessLevel to PUBLIC (default).
     * </p>
     */
    public FrontendResource() {
        super(null);
    }

    /**
     * Constructs a FrontendResource associated with an organization.
     * <p>
     * Creates a new frontend resource scoped to the specified organization for
     * multi-tenancy support. Type defaults to HTML, resourceType to RESOURCE,
     * and accessLevel to PUBLIC.
     * </p>
     *
     * @param organizationId the organization ID for tenant scoping, may be null
     */
    public FrontendResource(Long organizationId) {
        super(organizationId);
    }

    /**
     * Returns whether this resource has no access restrictions.
     * <p>
     * Computed field evaluating to true when requiredPrivilege is null.
     * </p>
     *
     * @return true if resource is unsecured (no privilege required), false otherwise
     */
    public boolean isUnsecured() {
        return unsecured;
    }

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return name;
    }

    /**
     * <p>Setter for the field <code>name</code>.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the published content of this resource.
     * <p>
     * Content can be HTML templates, JavaScript code, CSS styles, JSON data,
     * CSV data, XML, or text depending on the resource type.
     * </p>
     *
     * @return the published content, may be null if not set
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns whether this resource is a page.
     * <p>
     * Computed field evaluating to true when name is not null.
     * </p>
     *
     * @return true if resource is a page (has a name), false otherwise
     */
    public boolean isPage() {
        return isPage;
    }

    /**
     * Returns whether this resource should be included in sitemap generation.
     *
     * @return true if resource should be in sitemap, false otherwise
     */
    public boolean getIncludeInSitemap() {
        return includeInSitemap;
    }

    /**
     * Sets whether this resource should be included in sitemap generation.
     *
     * @param includeInSitemap true to include in sitemap, false to exclude
     */
    public void setIncludeInSitemap(boolean includeInSitemap) {
        this.includeInSitemap = includeInSitemap;
    }

    /**
     * Returns the privilege string required for accessing this resource.
     * <p>
     * When null, the resource is publicly accessible without privilege checks.
     * </p>
     *
     * @return the required privilege string, or null for unrestricted access
     */
    public String getRequiredPrivilege() {
        return requiredPrivilege;
    }

    /**
     * Sets the privilege string required for accessing this resource.
     *
     * @param requiredPrivilege the privilege string, or null for unrestricted access
     */
    public void setRequiredPrivilege(String requiredPrivilege) {
        this.requiredPrivilege = requiredPrivilege;
    }

    /**
     * Sets the published content of this resource.
     * <p>
     * Content can be HTML templates, JavaScript code, CSS styles, JSON data,
     * CSV data, XML, or text depending on the resource type. Maximum length
     * is 262,144 characters.
     * </p>
     *
     * @param content the published content to set, may be null
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Returns the resource type enum.
     *
     * @return the Type enum value (JS, CSS, JSON, etc.)
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the resource type enum.
     *
     * @param type the Type enum value to set
     */
    public void setType(Type type) {
        this.type = type;
    }
    
    /**
     * Sets the resource type from a string name.
     * <p>
     * Converts the string to Type enum using Enum.valueOf().
     * </p>
     *
     * @param type the type name as string (e.g., "HTML", "JS")
     * @throws IllegalArgumentException if type name is invalid
     */
    public void setType(String type) {
        this.type = Type.valueOf(type);
    }

    /**
     * Returns the resource category enum.
     *
     * @return the ResourceType enum value (RESOURCE, UI_COMPONENT, or DASHBOARD)
     */
    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * Sets the resource category enum.
     *
     * @param resourceType the ResourceType enum value to set
     */
    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }
    
    /**
     * Sets the resource category from a string name.
     * <p>
     * Converts the string to ResourceType enum using Enum.valueOf().
     * </p>
     *
     * @param resourceType the resource type name as string
     * @throws IllegalArgumentException if resource type name is invalid
     */
    public void setResourceType(String resourceType){ this.resourceType = ResourceType.valueOf(resourceType);}

    /**
     * Returns the list of content field names for indexing and diffing tools.
     * <p>
     * Overrides ComponentEntity to provide the content properties list
     * containing "content" and "draftContent".
     * </p>
     *
     * @return collection of content property names
     */
    @Override
    public Collection<String> contentProperties() {
        return contentProperties;
    }

    /**
     * Returns the draft content pending publication.
     * <p>
     * Draft content allows editing without affecting the published content visible
     * to end users. Maximum length is 262,144 characters.
     * </p>
     *
     * @return the draft content, or null if no draft exists
     */
    public String getDraftContent() {
        return draftContent;
    }

    /**
     * Sets the draft content for preview before publishing.
     *
     * @param draftContent the draft content to set, may be null
     */
    public void setDraftContent(String draftContent) {
        this.draftContent = draftContent;
    }

    /**
     * Returns whether draft content exists for this resource.
     * <p>
     * Computed field evaluating to true when draftContent is not null.
     * </p>
     *
     * @return true if draft content exists, false otherwise
     */
    public boolean isDraft() {
        return draft;
    }

    /**
     * Returns whether published content exists for this resource.
     * <p>
     * Computed field evaluating to true when content is not null.
     * </p>
     *
     * @return true if published content exists, false otherwise
     */
    public boolean isContentExists() {
        return contentExists;
    }

    /**
     * Returns the truncated MD5 hash of published content.
     * <p>
     * Computed via database @Formula using LEFT(MD5(content), 15).
     * Returns empty string if hash is null. Used for change detection.
     * </p>
     *
     * @return the 15-character content hash, or empty string if null
     */
    public String getContentHash() { return contentHash == null ? "" : contentHash; }

    /**
     * Sets the content hash value.
     * <p>
     * Note: This field is computed by database formula and should not normally be set manually.
     * </p>
     *
     * @param contentHash the content hash to set
     */
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    /**
     * Returns the truncated MD5 hash of draft content.
     * <p>
     * Computed via database @Formula using LEFT(MD5(draft_content), 15).
     * Returns empty string if hash is null. Used for change detection.
     * </p>
     *
     * @return the 15-character draft hash, or empty string if null
     */
    public String getDraftHash() { return draftHash == null ? "" : draftHash; }

    /**
     * Sets the draft hash value.
     * <p>
     * Note: This field is computed by database formula and should not normally be set manually.
     * </p>
     *
     * @param draftHash the draft hash to set
     */
    public void setDraftHash(String draftHash) { this.draftHash = draftHash; }

    /**
     * Returns the privilege required to read this FrontendResource entity.
     * <p>
     * Overrides ComponentEntity to provide computed privilege via @Formula.
     * Always returns readFrontendResource privilege constant.
     * </p>
     *
     * @return the privilege string constant for reading frontend resources
     */
    @Override
    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    /**
     * Returns the privilege required to write/modify this FrontendResource entity.
     * <p>
     * Overrides ComponentEntity to provide computed privilege via @Formula.
     * Always returns manageFrontendResource privilege constant.
     * </p>
     *
     * @return the privilege string constant for managing frontend resources
     */
    @Override
    public String getRequiredWritePrivilege() {
        return requiredWritePrivilege;
    }

    /**
     * Returns whether this resource can be embedded in iframes.
     *
     * @return true if resource is embeddable, false otherwise
     */
    public boolean isEmbeddable() {
        return embeddable;
    }

    /**
     * Sets whether this resource can be embedded in iframes.
     * <p>
     * When false (default), prevents embedding in iframe elements for security.
     * When true, allows the resource to be embedded on other pages.
     * </p>
     *
     * @param embeddable true to allow embedding, false to prevent
     */
    public void setEmbeddable(boolean embeddable) {
        this.embeddable = embeddable;
    }

    /**
     * Returns the access level defining visibility scope.
     *
     * @return the AccessLevel enum value (PUBLIC, GLOBAL, ORGANIZATION, or INTERNAL)
     */
    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    /**
     * Sets the access level defining visibility scope and URL path prefix.
     * <p>
     * Controls multi-tenancy isolation and determines which users can access
     * the resource. Also affects URL routing via path prefix.
     * </p>
     *
     * @param accessLevel the AccessLevel enum value to set
     */
    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }
}
