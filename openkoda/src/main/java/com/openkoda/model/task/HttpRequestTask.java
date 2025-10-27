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

package com.openkoda.model.task;

import com.openkoda.core.helper.JsonHelper;
import com.openkoda.model.common.AuditableEntityOrganizationRelated;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.*;

/**
 * Concrete {@link Task} entity for asynchronous HTTP request execution via webhooks and REST API calls.
 * <p>
 * This entity represents an HTTP request task in the single-table inheritance hierarchy (discriminator value "httprequest").
 * It supports asynchronous HTTP request workflows: creation → scheduling → execution by background worker → response capture.
 * The task can execute multiple HTTP methods (GET, POST, PUT, DELETE) with JSON payloads and custom headers.
 * </p>
 * <p>
 * HTTP request tasks are organization-scoped for multi-tenant webhook isolation, ensuring each tenant's webhooks
 * are executed within their security context. Request/response handling includes JSON payload processing,
 * custom header configuration, timeout management, and error handling capabilities.
 * </p>
 * <p>
 * Persistence: Entity stored in 'tasks' table with {@code @DiscriminatorValue("httprequest")} for single-table inheritance.
 * The requestUrl field is required ({@code @Column(nullable=false)}). Large text fields (json, headersJson) use
 * {@code @Column(length=65535)} for TEXT storage in database.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Task
 */
@Entity
@DiscriminatorValue("httprequest")
public class HttpRequestTask extends Task implements AuditableEntityOrganizationRelated {

    /**
     * Static list defining searchable content properties for auditing.
     * Contains "json" field to enable content-based audit trail searches.
     */
    final static List<String> contentProperties = Arrays.asList("json");

    /**
     * Target URL for HTTP request execution.
     * Required field as indicated by {@code @Column(nullable=false)}.
     * Represents the webhook or REST API endpoint that will receive the HTTP request.
     */
    @Column(nullable = false)
    private String requestUrl;

    /**
     * JSON request payload/body as string.
     * Stored as TEXT in database with {@code @Column(length=65535)} to accommodate large payloads.
     * Contains the data to be sent in the HTTP request body.
     */
    @Column(length = 65535)
    private String json;

    /**
     * HTTP headers as JSON string.
     * Stored as TEXT in database with {@code @Column(length=65535)} to support multiple headers.
     * Serialized header key-value pairs that will be included in the HTTP request.
     */
    @Column(length = 65535)
    private String headersJson;

    /**
     * Timestamp when the HTTP request was successfully sent.
     * Formatted with Spring {@code @DateTimeFormat(ISO.DATE)} for consistent date handling.
     * Null until the request is actually executed by the background worker.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Date dateSent;

    /**
     * Transient runtime cache of parsed headers from headersJson.
     * Map of header names to header values parsed from JSON representation.
     * Not persisted to database ({@code @Transient}).
     * <p>
     * Note: Cache is not invalidated when {@link #setHeadersJson(String)} is called,
     * which may lead to stale cached headers if JSON is modified after first access.
     * </p>
     */
    @Transient
    private Map<String, String> headersMap;

    /**
     * Default no-argument constructor for JPA entity instantiation.
     * Required by JPA specification for entity lifecycle management.
     */
    public HttpRequestTask() {
    }

    /**
     * Creates HTTP request task with target URL and JSON payload.
     *
     * @param requestUrl target endpoint URL for the HTTP request
     * @param json request body as JSON string
     */
    public HttpRequestTask(String requestUrl, String json) {
        this.requestUrl = requestUrl;
        this.json = json;
    }

    /**
     * Creates HTTP request task with URL, payload, and custom headers.
     *
     * @param requestUrl target endpoint URL for the HTTP request
     * @param json request body as JSON string
     * @param headersJson serialized HTTP headers as JSON string
     */
    public HttpRequestTask(String requestUrl, String json, String headersJson) {
        this.requestUrl = requestUrl;
        this.json = json;
        this.headersJson = headersJson;
    }

    /**
     * Creates organization-scoped HTTP request task for multi-tenant isolation.
     *
     * @param requestUrl target endpoint URL for the HTTP request
     * @param json request body as JSON string
     * @param organizationId tenant scope identifier for webhook isolation
     */
    public HttpRequestTask(String requestUrl, String json, Long organizationId) {
        this.requestUrl = requestUrl;
        this.json = json;
        this.setOrganizationId(organizationId);
    }

    /**
     * Gets the target endpoint URL for the HTTP request.
     *
     * @return target URL string for the HTTP request
     */
    public String getRequestUrl() {
        return requestUrl;
    }

    /**
     * Sets the target endpoint URL for the HTTP request.
     *
     * @param requestUrl target URL for the HTTP request
     */
    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    /**
     * Gets the JSON request payload/body.
     *
     * @return JSON string containing the request body
     */
    public String getJson() {
        return json;
    }

    /**
     * Sets the JSON request payload/body.
     *
     * @param json request body as JSON string
     */
    public void setJson(String json) {
        this.json = json;
    }

    /**
     * Gets the timestamp when the HTTP request was successfully sent.
     *
     * @return date when request was sent, or null if not yet executed
     */
    public Date getDateSent() {
        return dateSent;
    }

    /**
     * Sets the timestamp when the HTTP request was sent.
     *
     * @param dateSent timestamp of request execution
     */
    public void setDateSent(Date dateSent) {
        this.dateSent = dateSent;
    }

    /**
     * Returns audit trail identifier for this HTTP request task.
     * Provides a simple string representation for audit logging purposes.
     *
     * @return audit string in format "ID: {id}"
     */
    @Override
    public String toAuditString() {
        return "ID: " + this.getId();
    }

    /**
     * Returns list of searchable content properties for auditing.
     * Contains "json" field to enable content-based audit trail searches.
     *
     * @return collection containing "json" property name
     */
    @Override
    public Collection<String> contentProperties() {
        return contentProperties;
    }

    /**
     * Gets the raw HTTP headers as JSON string.
     *
     * @return serialized headers as JSON string
     */
    public String getHeadersJson() {
        return headersJson;
    }

    /**
     * Sets the HTTP headers as JSON string.
     * <p>
     * Note: Does not invalidate the cached {@link #headersMap}, which may lead to
     * stale cached headers if called after {@link #getHeadersMap()} has been invoked.
     * </p>
     *
     * @param headersJson serialized headers as JSON string
     */
    public void setHeadersJson(String headersJson) {
        this.headersJson = headersJson;
    }

    /**
     * Gets parsed HTTP headers as a map with lazy loading and caching.
     * <p>
     * Lazily parses headersJson into Map&lt;String, String&gt; using {@link JsonHelper}.
     * The result is cached transiently for subsequent calls. If headersJson is null or empty,
     * defaults to empty JSON object "{}".
     * </p>
     * <p>
     * Warning: Cache is not invalidated when {@link #setHeadersJson(String)} is called,
     * which could lead to stale cached headers if JSON is modified after first access.
     * </p>
     *
     * @return parsed headers as map of header names to header values
     */
    public Map<String, String> getHeadersMap() {
        if ( headersMap == null ) {
            headersMap = JsonHelper.from(StringUtils.defaultString(headersJson, "{}"), Map.class);
        }
        return headersMap;
    }
}
