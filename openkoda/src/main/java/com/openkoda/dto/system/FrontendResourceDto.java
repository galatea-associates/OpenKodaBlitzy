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

package com.openkoda.dto.system;

import com.openkoda.dto.CanonicalObject;
import com.openkoda.dto.OrganizationRelatedObject;
import com.openkoda.model.component.FrontendResource;


/**
 * Data Transfer Object for frontend resources and CMS content management.
 * <p>
 * This DTO models frontend/CMS resources for content management and dynamic page generation
 * within OpenKoda's multi-tenant CMS system. It implements {@link CanonicalObject} and
 * {@link OrganizationRelatedObject} to support multi-tenant CMS operations with proper
 * organizational scoping and audit trail generation.
 * </p>
 * <p>
 * The DTO is used by CMS controllers, Thymeleaf template resolution, and frontend resource
 * management services to transfer content data between layers without exposing domain entities.
 * It supports various resource types including pages, templates, fragments, and embeddable
 * components with granular access control.
 * </p>
 * <p>
 * <b>Design Notes:</b>
 * This is a mutable DTO with public fields following JavaBean conventions. It does not provide
 * validation, equals/hashCode implementation, or thread-safety guarantees. The DTO is tightly
 * coupled to domain enums ({@link FrontendResource.AccessLevel} and {@link FrontendResource.Type}),
 * requiring careful serialization and integration testing when enum values change.
 * </p>
 * <p>
 * <b>Enum Coupling Warning:</b>
 * Changes to {@link FrontendResource.AccessLevel} or {@link FrontendResource.Type} enums
 * constitute breaking changes requiring version migration and thorough integration testing
 * of serialization/deserialization paths.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * FrontendResourceDto dto = new FrontendResourceDto();
 * dto.setName("home-page");
 * dto.setType(FrontendResource.Type.PAGE);
 * }</pre>
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @since 2019-02-19
 * @version 1.7.1
 * @see CanonicalObject
 * @see OrganizationRelatedObject
 * @see FrontendResource
 */
public class FrontendResourceDto implements CanonicalObject, OrganizationRelatedObject {

    /**
     * Unique resource identifier used for lookups and URL routing.
     * <p>
     * This name serves as the primary identifier for the frontend resource
     * within its organization scope. It is used for URL mapping, template
     * resolution, and resource lookups in the CMS system.
     * </p>
     */
    public String name;
    
    /**
     * Tenant identifier for multi-tenant CMS content isolation.
     * <p>
     * Specifies which organization owns this frontend resource, enabling
     * proper data isolation in multi-tenant deployments. Null values indicate
     * global resources accessible across all organizations.
     * </p>
     */
    public Long organizationId;
    
    /**
     * Raw HTML/template content for rendering.
     * <p>
     * Contains the actual content to be rendered, which may include HTML,
     * Thymeleaf template expressions, or other markup. This is the production
     * content served to end users.
     * </p>
     */
    public String content;
    
    /**
     * Editable version of content for WYSIWYG editors.
     * <p>
     * Stores a simplified or modified version of the content optimized for
     * visual editing in WYSIWYG editors. This may differ from the production
     * content to facilitate easier editing workflows.
     * </p>
     */
    public String contentEditable;
    
    /**
     * Sample data for preview and testing purposes.
     * <p>
     * Contains test data used to preview the resource with mock content during
     * development and testing. This data is not used in production rendering.
     * </p>
     */
    public String testData;
    
    /**
     * Privilege name required for accessing this resource.
     * <p>
     * Specifies the privilege identifier that users must possess to access
     * this resource. If null or empty, no specific privilege check is enforced
     * beyond the access level settings.
     * </p>
     */
    public String requiredPrivilege;
    
    /**
     * Whether to include this resource in sitemap.xml generation.
     * <p>
     * When true, this resource will be included in automatically generated
     * sitemap files for search engine indexing. Typically set to true for
     * public pages and false for templates, fragments, or restricted content.
     * </p>
     */
    public boolean includeInSitemap;
    
    /**
     * Whether resource can be embedded in iframe or other contexts.
     * <p>
     * Controls whether this resource can be embedded within iframes or other
     * embedding contexts. When false, the resource should only be accessed
     * directly. Note that this field has dual accessors: {@link #isEmbeddable()}
     * and {@link #getEmbeddable()} to support both JavaBean and boolean "is"
     * naming conventions.
     * </p>
     */
    public boolean embeddable;
    
    /**
     * Enum controlling access permissions (PUBLIC, AUTHENTICATED, RESTRICTED).
     * <p>
     * Defines the access level for this resource. This field is coupled to the
     * {@link FrontendResource.AccessLevel} domain enum, and changes to that enum
     * constitute breaking changes requiring version migration.
     * </p>
     *
     * @see FrontendResource.AccessLevel
     */
    public FrontendResource.AccessLevel accessLevel;
    
    /**
     * Enum defining resource type (PAGE, TEMPLATE, FRAGMENT, etc.).
     * <p>
     * Specifies the type of frontend resource, which determines how it is
     * processed and rendered. This field is coupled to the
     * {@link FrontendResource.Type} domain enum. Changes to that enum values
     * constitute breaking changes with potential serialization risks requiring
     * thorough integration testing.
     * </p>
     *
     * @see FrontendResource.Type
     */
    public FrontendResource.Type type;

    /**
     * Returns the unique resource identifier.
     *
     * @return the resource name, or null if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the unique resource identifier.
     *
     * @param name the resource name for lookups and URL routing
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the tenant identifier for this resource.
     *
     * @return the organization ID, or null for global resources
     */
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the tenant identifier for multi-tenant content isolation.
     *
     * @param organizationId the organization ID, or null for global resources
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Returns the raw HTML/template content for rendering.
     *
     * @return the production content, or null if not set
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the raw HTML/template content for rendering.
     *
     * @param content the production content with HTML or Thymeleaf expressions
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Returns the privilege name required for accessing this resource.
     *
     * @return the required privilege identifier, or null if no privilege check
     */
    public String getRequiredPrivilege() {
        return requiredPrivilege;
    }

    /**
     * Sets the privilege name required for accessing this resource.
     *
     * @param requiredPrivilege the privilege identifier, or null for no privilege check
     */
    public void setRequiredPrivilege(String requiredPrivilege) {
        this.requiredPrivilege = requiredPrivilege;
    }

    /**
     * Returns the resource type enum.
     *
     * @return the resource type (PAGE, TEMPLATE, FRAGMENT, etc.), or null if not set
     * @see FrontendResource.Type
     */
    public FrontendResource.Type getType() {
        return type;
    }

    /**
     * Sets the resource type enum.
     * <p>
     * This enum value determines how the resource is processed and rendered.
     * Ensure the provided enum value is compatible with serialization requirements.
     * </p>
     *
     * @param type the resource type enum value
     * @see FrontendResource.Type
     */
    public void setType(FrontendResource.Type type) {
        this.type = type;
    }

    /**
     * Returns the sample data for preview and testing.
     *
     * @return the test data, or null if not set
     */
    public String getTestData() {
        return testData;
    }

    /**
     * Sets the sample data for preview and testing purposes.
     *
     * @param testData the mock content for development and testing
     */
    public void setTestData(String testData) {
        this.testData = testData;
    }

    /**
     * Returns whether this resource should be included in sitemap.xml.
     *
     * @return true if the resource should be included in sitemap generation
     */
    public boolean getIncludeInSitemap() {
        return includeInSitemap;
    }

    /**
     * Sets whether this resource should be included in sitemap.xml generation.
     *
     * @param includeInSitemap true to include in sitemap, false to exclude
     */
    public void setIncludeInSitemap(boolean includeInSitemap) {
        this.includeInSitemap = includeInSitemap;
    }

    /**
     * Returns whether the resource can be embedded (boolean "is" accessor).
     * <p>
     * This method follows the boolean "is" naming convention. A dual accessor
     * {@link #getEmbeddable()} is also provided for JavaBean compatibility.
     * Both methods return the same value.
     * </p>
     *
     * @return true if the resource can be embedded in iframe or other contexts
     * @see #getEmbeddable()
     */
    public boolean isEmbeddable() {
        return embeddable;
    }

    /**
     * Returns whether the resource can be embedded (JavaBean accessor).
     * <p>
     * This method follows the JavaBean "get" naming convention. A dual accessor
     * {@link #isEmbeddable()} is also provided for boolean "is" convention.
     * Both methods return the same value.
     * </p>
     *
     * @return true if the resource can be embedded in iframe or other contexts
     * @see #isEmbeddable()
     */
    public boolean getEmbeddable() {
        return embeddable;
    }

    /**
     * Sets whether the resource can be embedded in iframe or other contexts.
     *
     * @param embeddable true to allow embedding, false to restrict direct access only
     */
    public void setEmbeddable(boolean embeddable) {
        this.embeddable = embeddable;
    }

    /**
     * Returns the editable version of content for WYSIWYG editors.
     *
     * @return the editor-optimized content, or null if not set
     */
    public String getContentEditable() {
        return contentEditable;
    }

    /**
     * Sets the editable version of content optimized for visual editing.
     *
     * @param contentEditable the simplified content for WYSIWYG editors
     */
    public void setContentEditable(String contentEditable) {
        this.contentEditable = contentEditable;
    }

    /**
     * Returns the access level enum controlling resource permissions.
     *
     * @return the access level (PUBLIC, AUTHENTICATED, RESTRICTED), or null if not set
     * @see FrontendResource.AccessLevel
     */
    public FrontendResource.AccessLevel getAccessLevel() {
        return accessLevel;
    }

    /**
     * Sets the access level enum controlling resource permissions.
     * <p>
     * This enum value determines who can access this resource. Ensure the provided
     * enum value is compatible with serialization and authorization requirements.
     * </p>
     *
     * @param accessLevel the access level enum value
     * @see FrontendResource.AccessLevel
     */
    public void setAccessLevel(FrontendResource.AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    /**
     * Returns a formatted notification message for audit trails.
     * <p>
     * Generates a human-readable message describing this CMS entry using the format
     * "CmsEntry %s of type: %s." with the resource name and type. This message is
     * used for audit logging and notification systems.
     * </p>
     * <p>
     * <b>Warning:</b> This method does not perform null checking. If {@link #name}
     * or {@link #type} is null, {@link String#format(String, Object...)} will produce
     * "null" in the output string rather than throwing a NullPointerException.
     * Callers should ensure name and type are set before invoking this method for
     * meaningful audit messages.
     * </p>
     *
     * @return the formatted notification message in the form "CmsEntry %s of type: %s."
     */
    @Override
    public String notificationMessage() {
        return String.format("CmsEntry %s of type: %s.", name, type);
    }

}
