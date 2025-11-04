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

/**
 * DTO for exporting and importing controller endpoint definitions with HTTP routing metadata and executable code.
 * <p>
 * This is a mutable JavaBean POJO used for YAML/JSON serialization during export/import operations.
 * It extends {@link ComponentDto} to inherit module and organization scope fields, enabling tenant-aware
 * and module-specific endpoint definitions. This DTO maps to the {@code ControllerEndpoint} domain entity
 * and is processed by export/import pipelines and converters.
 * 
 * <p>
 * Instances of this class are not thread-safe and should not be shared across threads without external synchronization.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ComponentDto for inherited module and organizationId fields
 * @see com.openkoda.model.component.ControllerEndpoint domain entity
 */
public class ControllerEndpointConversionDto extends ComponentDto {

    /**
     * URL subpath segment for the controller endpoint routing. Nullable.
     */
    private String subpath;
    
    /**
     * HTTP request/response headers configuration in serialized format. Nullable.
     */
    private String httpHeaders;
    
    /**
     * HTTP method (GET, POST, PUT, DELETE, etc.) for this endpoint. Nullable.
     */
    private String httpMethod;
    
    /**
     * Model attributes metadata for view rendering. Nullable.
     */
    private String modelAttributes;
    
    /**
     * Response content type or view resolution strategy. Nullable.
     */
    private String responseType;
    
    /**
     * Executable controller code or handler logic. Nullable.
     */
    private String code;
    
    /**
     * Foreign key reference to associated FrontendResource entity. Nullable.
     */
    private Long frontendResourceId;

    /**
     * Gets the URL subpath segment for the controller endpoint routing.
     *
     * @return the URL subpath segment or null if not set
     */
    public String getSubpath() {
        return subpath;
    }

    /**
     * Sets the URL subpath segment for the controller endpoint routing.
     *
     * @param subpath the URL subpath segment to set, may be null
     */
    public void setSubpath(String subpath) {
        this.subpath = subpath;
    }

    /**
     * Gets the HTTP request/response headers configuration in serialized format.
     *
     * @return the HTTP headers configuration or null if not set
     */
    public String getHttpHeaders() {
        return httpHeaders;
    }

    /**
     * Sets the HTTP request/response headers configuration in serialized format.
     *
     * @param httpHeaders the HTTP headers configuration to set, may be null
     */
    public void setHttpHeaders(String httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    /**
     * Gets the HTTP method (GET, POST, PUT, DELETE, etc.) for this endpoint.
     *
     * @return the HTTP method or null if not set
     */
    public String getHttpMethod() {
        return httpMethod;
    }

    /**
     * Sets the HTTP method (GET, POST, PUT, DELETE, etc.) for this endpoint.
     *
     * @param httpMethod the HTTP method to set, may be null
     */
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    /**
     * Gets the model attributes metadata for view rendering.
     *
     * @return the model attributes metadata or null if not set
     */
    public String getModelAttributes() {
        return modelAttributes;
    }

    /**
     * Sets the model attributes metadata for view rendering.
     *
     * @param modelAttributes the model attributes metadata to set, may be null
     */
    public void setModelAttributes(String modelAttributes) {
        this.modelAttributes = modelAttributes;
    }

    /**
     * Gets the response content type or view resolution strategy.
     *
     * @return the response content type or null if not set
     */
    public String getResponseType() {
        return responseType;
    }

    /**
     * Sets the response content type or view resolution strategy.
     *
     * @param responseType the response content type to set, may be null
     */
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    /**
     * Gets the executable controller code or handler logic.
     *
     * @return the executable controller code or null if not set
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the executable controller code or handler logic.
     *
     * @param code the executable controller code to set, may be null
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets the foreign key reference to associated FrontendResource entity.
     *
     * @return the FrontendResource entity ID or null if not set
     */
    public Long getFrontendResourceId() {
        return frontendResourceId;
    }

    /**
     * Sets the foreign key reference to associated FrontendResource entity.
     *
     * @param frontendResourceId the FrontendResource entity ID to set, may be null
     */
    public void setFrontendResourceId(Long frontendResourceId) {
        this.frontendResourceId = frontendResourceId;
    }

}
