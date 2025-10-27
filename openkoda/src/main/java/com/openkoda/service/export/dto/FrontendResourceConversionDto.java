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

package com.openkoda.service.export.dto;

import com.openkoda.model.component.FrontendResource;

import java.util.List;

/**
 * DTO for frontend resources such as HTML templates and JavaScript modules with nested controller endpoints and access control metadata.
 * <p>
 * This is a mutable JavaBean POJO designed for YAML/JSON serialization in component export and import operations.
 * It extends {@link ComponentDto} to inherit module identifier and organization scope fields. This DTO maps the
 * {@link com.openkoda.model.component.FrontendResource} domain entity and is used throughout frontend resource
 * export/import pipelines and converters.
 * </p>
 * <p>
 * The {@code controllerEndpoints} field contains a nested list of {@link ControllerEndpointConversionDto} objects
 * and is not defensively copied - callers receive and set direct references to the underlying list. The {@code embeddable}
 * field is exposed as a public field and provides both {@code isEmbeddable()} and {@code getEmbeddable()} accessors
 * for JavaBean framework compatibility.
 * </p>
 * <p>
 * This class provides no validation or framework annotations and is not thread-safe. It is intended for use by
 * mapping frameworks (Jackson, MapStruct), export pipelines, and admin controllers.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ComponentDto for inherited module and organizationId fields
 * @see com.openkoda.model.component.FrontendResource domain entity
 * @see FrontendResource.AccessLevel for access control enumeration
 * @see ControllerEndpointConversionDto for nested endpoint DTOs
 */
public class FrontendResourceConversionDto extends ComponentDto{

    /**
     * Flag indicating whether this resource should be included in the sitemap.
     * Defaults to false.
     */
    private boolean includeInSitemap;
    
    /**
     * Access control level for this resource.
     * Nullable.
     */
    private FrontendResource.AccessLevel accessLevel;
    
    /**
     * Resource name identifier.
     * Nullable.
     */
    private String name;
    
    /**
     * Required privilege name for accessing this resource.
     * Nullable.
     */
    private String requiredPrivilege;
    
    /**
     * Resource type classification.
     * Nullable.
     */
    private String type;
    
    /**
     * Technical resource type (HTML, JavaScript, CSS, etc.).
     * Nullable.
     */
    private String resourceType;
    
    /**
     * Raw content of the frontend resource (template code, script, etc.).
     * Nullable.
     */
    private String content;
    
    /**
     * List of associated controller endpoint definitions.
     * Not defensively copied.
     * Nullable.
     */
    private List<ControllerEndpointConversionDto> controllerEndpoints;
    
    /**
     * Flag indicating whether this resource can be embedded in other resources.
     * Public field exposes both isEmbeddable() and getEmbeddable() accessors.
     * Defaults to false.
     */
    public boolean embeddable;

    /**
     * @return true if this resource should be included in the sitemap, false otherwise
     */
    public boolean getIncludeInSitemap() {
        return includeInSitemap;
    }

    /**
     * @param includeInSitemap true to include this resource in the sitemap, false to exclude it
     */
    public void setIncludeInSitemap(boolean includeInSitemap) {
        this.includeInSitemap = includeInSitemap;
    }

    /**
     * @return the access control level or null if not set
     */
    public FrontendResource.AccessLevel getAccessLevel() {
        return accessLevel;
    }

    /**
     * @param accessLevel the access control level to set, may be null
     */
    public void setAccessLevel(FrontendResource.AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    /**
     * @return the resource name identifier or null if not set
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the resource name identifier to set, may be null
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the required privilege name for accessing this resource or null if not set
     */
    public String getRequiredPrivilege() {
        return requiredPrivilege;
    }

    /**
     * @param requiredPrivilege the required privilege name for accessing this resource to set, may be null
     */
    public void setRequiredPrivilege(String requiredPrivilege) {
        this.requiredPrivilege = requiredPrivilege;
    }

    /**
     * @return the resource type classification or null if not set
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the resource type classification to set, may be null
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the technical resource type (HTML, JavaScript, CSS, etc.) or null if not set
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * @param resourceType the technical resource type (HTML, JavaScript, CSS, etc.) to set, may be null
     */
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * @return the raw content of the frontend resource (template code, script, etc.) or null if not set
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content the raw content of the frontend resource (template code, script, etc.) to set, may be null
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @return the list of controller endpoints or null if not set. Returns direct reference, not a defensive copy
     */
    public List<ControllerEndpointConversionDto> getControllerEndpoints() {
        return controllerEndpoints;
    }

    /**
     * @param controllerEndpoints the list of controller endpoints to set, may be null. Stores direct reference without defensive copying
     */
    public void setControllerEndpoints(List<ControllerEndpointConversionDto> controllerEndpoints) {
        this.controllerEndpoints = controllerEndpoints;
    }

    /**
     * @return true if this resource can be embedded in other resources, false otherwise
     */
    public boolean isEmbeddable() {
        return embeddable;
    }

    /**
     * @return true if this resource can be embedded in other resources, false otherwise
     */
    public boolean getEmbeddable() {
        return embeddable;
    }

    /**
     * @param embeddable true to allow this resource to be embedded in other resources, false to disallow embedding
     */
    public void setEmbeddable(boolean embeddable) {
        this.embeddable = embeddable;
    }
}
