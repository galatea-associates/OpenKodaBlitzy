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

package com.openkoda.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openkoda.model.common.ComponentEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Formula;

import java.util.*;
import java.util.stream.Stream;

/**
 * JPA entity representing dynamic REST endpoints that can be created without code deployment.
 * <p>
 * This entity enables no-code API development by storing endpoint configurations including URL paths,
 * HTTP methods, and executable code that runs at request time. Supports multiple response types
 * including HTML rendering, JSON responses, file downloads, and streaming.
 * </p>
 * <p>
 * The endpoint system allows creating custom REST APIs through database configuration rather than
 * code deployment. Each endpoint is organization-scoped for multi-tenancy support and can optionally
 * reference a FrontendResource for HTML template rendering.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * ControllerEndpoint endpoint = new ControllerEndpoint(frontendResourceId, orgId);
 * endpoint.setSubPath("/api/data");
 * endpoint.setHttpMethod("GET");
 * }</pre>
 * </p>
 * <p>
 * Persisted to 'controller_endpoint' table with unique constraint ensuring no duplicate endpoints
 * with the same frontend_resource_id, sub_path, http_method, and organization_id combination.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FrontendResource
 * @see ComponentEntity
 */
@Entity
@Table(
        name = "controller_endpoint",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"frontend_resource_id", "sub_path", "http_method", "organization_id"})
        }
)
public class ControllerEndpoint extends ComponentEntity {

    /**
     * Static list of content property names used for indexing and diffing tools.
     * Contains 'code' field which holds the endpoint implementation logic.
     */
    final static List<String> contentProperties = Arrays.asList("code");

    /**
     * Defines the response type for the controller endpoint.
     * Determines how the endpoint processes and returns data to the client.
     */
    public enum ResponseType {
        /** HTML page rendering using a FrontendResource template. */
        HTML,
        /** JSON response with model attributes serialized as JSON for REST APIs. */
        MODEL_AS_JSON,
        /** File download response with appropriate content-disposition headers. */
        FILE,
        /** Streaming response for large data or real-time content delivery. */
        STREAM
    }

    /**
     * Defines the HTTP method supported by the controller endpoint.
     * Determines which HTTP verb the endpoint responds to.
     */
    public enum HttpMethod {
        /** HTTP GET method for read operations. */
        GET,
        /** HTTP POST method for create/update operations. */
        POST
    }

    /**
     * URL subpath for endpoint routing within the application.
     * <p>
     * Example: "/api/users" or "/data/export". Combined with base URL to form complete endpoint path.
     * Stored in 'sub_path' database column.
     * </p>
     */
    @Column(name = "sub_path")
    private String subPath;

    /**
     * Endpoint implementation code executed at runtime when the endpoint is called.
     * <p>
     * Contains the business logic for the endpoint, supporting HTML rendering, JSON responses,
     * file downloads, and streaming. Maximum length is 262,144 characters (65536 * 4).
     * </p>
     */
    @Column(length = 65536 * 4)
    private String code;

    /**
     * Multi-line HTTP headers to include in the response.
     * <p>
     * Format: one header per line as "Name: Value". Maximum length 1000 characters.
     * Parsed by {@link #getHttpHeadersMap()} into a Map for runtime use.
     * </p>
     */
    @Column(length = 1000)
    private String httpHeaders;

    /**
     * HTTP method for this endpoint (GET or POST).
     * Stored in 'http_method' database column.
     */
    @Column(name = "http_method")
    private HttpMethod httpMethod;

    /**
     * Comma-separated list of model attribute names exposed to the view or JSON response.
     * <p>
     * Example: "user,roles,permissions". Maximum length 1000 characters.
     * Parsed by {@link #getPageAttributesNames()} into a String array.
     * </p>
     */
    @Column(length = 1000)
    private String modelAttributes;

    /**
     * Response type enum determining how the endpoint returns data.
     * Defaults to HTML for page rendering. Not null.
     */
    @Enumerated(EnumType.STRING)
    @NotNull
    private ResponseType responseType = ResponseType.HTML;

    /**
     * Lazy-loaded optional association to FrontendResource for HTML template rendering.
     * <p>
     * Used when responseType is HTML to determine which template renders the response.
     * Fetch type LAZY to avoid loading template unless needed. Read-only association
     * (insertable=false, updatable=false) managed via frontendResourceId.
     * </p>
     */
    @JsonIgnore
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(nullable = true, insertable = false, updatable = false, name = FRONTEND_RESOURCE_ID)
    private FrontendResource frontendResource;

    /**
     * Foreign key to FrontendResource entity for HTML template association.
     * Nullable to support endpoints without templates (e.g., JSON APIs).
     */
    @Column(nullable = true, name = FRONTEND_RESOURCE_ID)
    private Long frontendResourceId;

    /**
     * Default no-arg constructor required by JPA.
     * Initializes entity with null organization scope.
     */
    public ControllerEndpoint() {
        super(null);
    }

    /**
     * Creates a new controller endpoint scoped to an organization.
     *
     * @param organizationId the organization ID for multi-tenant scoping (nullable)
     */
    public ControllerEndpoint(Long organizationId) {
        super(null);
    }

    /**
     * Creates a new controller endpoint with frontend resource and organization scope.
     *
     * @param frontendResourceId the frontend resource ID for HTML template association (nullable)
     * @param organizationId the organization ID for multi-tenant scoping (nullable)
     */
    public ControllerEndpoint(Long frontendResourceId, Long organizationId) {
        super(organizationId);
        this.frontendResourceId = frontendResourceId;
    }

    /**
     * Returns the URL subpath for this endpoint.
     *
     * @return the subpath string (e.g., "/api/users"), may be null
     */
    public String getSubPath() {
        return subPath;
    }

    /**
     * Sets the URL subpath for endpoint routing.
     *
     * @param subPath the subpath string to set (e.g., "/api/users")
     */
    public void setSubPath(String subPath) {
        this.subPath = subPath;
    }

    /**
     * Returns the endpoint implementation code executed at runtime.
     *
     * @return the code string, may be null
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the endpoint implementation code.
     *
     * @param code the code string to execute at runtime, max 262,144 characters
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Returns the multi-line HTTP headers string.
     *
     * @return the headers string in "Name: Value" format, one per line, may be null
     */
    public String getHttpHeaders() {
        return httpHeaders;
    }

    /**
     * Sets the HTTP headers for the response.
     *
     * @param httpHeaders the headers string with one header per line as "Name: Value"
     */
    public void setHttpHeaders(String httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    /**
     * Returns the HTTP method for this endpoint.
     *
     * @return the HTTP method enum value (GET or POST), may be null
     */
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    /**
     * Sets the HTTP method for this endpoint.
     *
     * @param httpMethod the HTTP method enum value to set
     */
    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    /**
     * Sets the HTTP method for this endpoint from a string value.
     *
     * @param httpMethod the HTTP method name as string ("GET" or "POST")
     * @throws IllegalArgumentException if the method name is invalid
     */
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = HttpMethod.valueOf(httpMethod);
    }

    /**
     * Returns the comma-separated model attributes string.
     *
     * @return the model attributes string, may be null
     */
    public String getModelAttributes() {
        return modelAttributes;
    }

    /**
     * Sets the model attributes exposed to the view or JSON response.
     *
     * @param modelAttributes comma-separated attribute names (e.g., "user,roles,permissions")
     */
    public void setModelAttributes(String modelAttributes) {
        this.modelAttributes = modelAttributes;
    }

    /**
     * Returns the response type for this endpoint.
     *
     * @return the response type enum value, never null (defaults to HTML)
     */
    public ResponseType getResponseType() {
        return responseType;
    }

    /**
     * Sets the response type for this endpoint.
     *
     * @param responseType the response type enum value to set
     */
    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    /**
     * Sets the response type for this endpoint from a string value.
     *
     * @param responseType the response type name as string (e.g., "HTML", "MODEL_AS_JSON")
     * @throws IllegalArgumentException if the response type name is invalid
     */
    public void setResponseType(String responseType) {
        this.responseType = ResponseType.valueOf(responseType);
    }

    /**
     * Returns the associated frontend resource for HTML template rendering.
     *
     * @return the frontend resource entity, may be null (lazy-loaded)
     */
    public FrontendResource getFrontendResource() {
        return frontendResource;
    }

    /**
     * Sets the frontend resource for HTML template rendering.
     *
     * @param frontendResource the frontend resource entity to associate
     */
    public void setFrontendResource(FrontendResource frontendResource) {
        this.frontendResource = frontendResource;
    }

    /**
     * Returns the frontend resource ID.
     *
     * @return the frontend resource ID, may be null
     */
    public Long getFrontendResourceId() {
        return frontendResourceId;
    }

    /**
     * Sets the frontend resource ID for template association.
     *
     * @param frontendResourceId the frontend resource ID to set
     */
    public void setFrontendResourceId(Long frontendResourceId) {
        this.frontendResourceId = frontendResourceId;
    }

    /**
     * Parses the multi-line httpHeaders string into a Map for runtime use.
     * <p>
     * Splits httpHeaders on newlines and parses each line as "Name: Value" format.
     * Trims whitespace from header names and values. Returns empty map if httpHeaders is blank.
     * </p>
     * <p>
     * Note: No validation of header format. ArrayIndexOutOfBoundsException may occur if
     * a line does not contain ":" separator. Parsing is brittle and expects exact format.
     * </p>
     *
     * @return map of header names to values, never null (empty map if no headers)
     */
    public Map<String, String> getHttpHeadersMap() {
        Map<String, String> httpHeadersMap = new HashMap();
        if(StringUtils.isNotBlank(this.httpHeaders)) {
            for (String httpHeader : Arrays.asList(this.httpHeaders.split("\n"))) {
                String[] headerParts = httpHeader.split(":");
                httpHeadersMap.put(headerParts[0].trim(), headerParts[1].trim());
            }
        }
        return httpHeadersMap;
    }

    /**
     * Parses the comma-separated modelAttributes into a trimmed String array.
     * <p>
     * Splits modelAttributes on commas and trims whitespace from each attribute name.
     * Returns null (not empty array) if modelAttributes is blank.
     * </p>
     *
     * @return array of attribute names, or null if modelAttributes is blank
     */
    public String[] getPageAttributesNames() {
        return StringUtils.isNotBlank(this.modelAttributes) ? Stream.of(this.modelAttributes.split(","))
                .map(s -> s.trim()).toArray(String[]::new) : null;
    }

    /**
     * Computed field always returning NULL for read privilege requirements.
     * Controller endpoints do not enforce read privileges via the privilege system.
     * Evaluated via database @Formula as "(NULL)".
     */
    @Formula("(NULL)")
    protected String requiredReadPrivilege;

    /**
     * Computed field always returning NULL for write privilege requirements.
     * Controller endpoints do not enforce write privileges via the privilege system.
     * Evaluated via database @Formula as "(NULL)".
     */
    @Formula("(NULL)")
    protected String requiredWritePrivilege;

    /**
     * Returns the required read privilege for this endpoint.
     * <p>
     * Overrides ComponentEntity to return NULL, indicating no privilege enforcement
     * for read operations on controller endpoints.
     * </p>
     *
     * @return null (no read privilege required)
     */
    @Override
    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    /**
     * Returns the required write privilege for this endpoint.
     * <p>
     * Overrides ComponentEntity to return NULL, indicating no privilege enforcement
     * for write operations on controller endpoints.
     * </p>
     *
     * @return null (no write privilege required)
     */
    @Override
    public String getRequiredWritePrivilege() {
        return requiredWritePrivilege;
    }

    /**
     * Returns the collection of content property names for indexing and diffing tools.
     * <p>
     * Overrides ComponentEntity to expose the 'code' field as the primary content
     * that should be indexed or compared when analyzing endpoint changes.
     * </p>
     *
     * @return collection containing "code" property name
     */
    @Override
    public Collection<String> contentProperties() {
        return contentProperties;
    }

}
