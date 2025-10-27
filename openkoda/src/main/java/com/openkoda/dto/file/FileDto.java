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

package com.openkoda.dto.file;

import com.google.gson.annotations.Expose;
import com.openkoda.dto.CanonicalObject;
import com.openkoda.dto.OrganizationRelatedObject;

import static com.openkoda.controller.common.URLConstants.*;

/**
 * Mutable Data Transfer Object representing stored file metadata with precomputed URLs.
 * <p>
 * This DTO encapsulates file metadata including unique identifiers, content information,
 * and precomputed download and delete URLs for API responses. It implements both
 * {@link CanonicalObject} and {@link OrganizationRelatedObject} interfaces to support
 * multi-tenant file scoping where files can be either globally accessible or scoped to
 * a specific organization.
 * </p>
 * <p>
 * The class uses Gson {@code @Expose} annotations to control JSON serialization.
 * Note that {@code organizationId} is intentionally NOT exposed in JSON output to avoid
 * leaking internal tenant identifiers in API responses.
 * </p>
 * <p>
 * <b>Important Design Note (TODO Rule 5.5):</b> This DTO contains URL-building logic
 * via {@link #getUrlBase(Long, Long)}, which is a design smell. DTOs should ideally
 * be pure data containers without business logic. URL construction should be delegated
 * to a separate service or URL builder component.
 * </p>
 * <p>
 * <b>URL Recomputation Behavior:</b> The {@code downloadUrl} and {@code deleteUrl} fields
 * are computed during construction. If {@code id} or {@code organizationId} change after
 * construction via setters, the URLs are NOT automatically recomputed. Callers must either
 * preserve consistency or recreate the DTO if identifiers change.
 * </p>
 * <p>
 * DTOs are intentionally mutable and do not perform validation or synchronization.
 * This class is used across controllers, services, and mappers to represent file metadata
 * in API responses.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CanonicalObject
 * @see OrganizationRelatedObject
 */
public class FileDto implements CanonicalObject, OrganizationRelatedObject{

    /**
     * Optional organization identifier for multi-tenant file isolation.
     * <p>
     * When present, the file is scoped to a specific organization. When {@code null},
     * the file is globally accessible. This field is intentionally NOT annotated with
     * {@code @Expose}, so it will not appear in JSON serialization to avoid exposing
     * internal tenant identifiers in API responses.
     * </p>
     */
    public Long organizationId;

    /**
     * Unique file identifier.
     * <p>
     * This field is exposed in JSON serialization via {@code @Expose} annotation.
     * </p>
     */
    @Expose
    public Long id;
    
    /**
     * Human-friendly file name.
     * <p>
     * This field is exposed in JSON serialization via {@code @Expose} annotation.
     * Typically includes the file extension (e.g., "document.pdf").
     * </p>
     */
    @Expose
    public String filename;
    
    /**
     * MIME type of the file content.
     * <p>
     * This field is exposed in JSON serialization via {@code @Expose} annotation.
     * Examples: "application/pdf", "image/jpeg", "text/plain".
     * </p>
     */
    @Expose
    public String contentType;
    
    /**
     * Precomputed download endpoint URL.
     * <p>
     * This field is exposed in JSON serialization via {@code @Expose} annotation.
     * The URL is computed during construction by calling {@link #getUrlBase(Long, Long)}
     * and appending "/content". For global files, the format is "/html/file/{id}/content".
     * For organization-scoped files, the format is "/html/organization/{orgId}/file/{id}/content".
     * </p>
     */
    @Expose
    public String downloadUrl;
    
    /**
     * Precomputed delete endpoint URL.
     * <p>
     * This field is exposed in JSON serialization via {@code @Expose} annotation.
     * The URL is computed during construction by calling {@link #getUrlBase(Long, Long)}
     * and appending "/remove". For global files, the format is "/html/file/{id}/remove".
     * For organization-scoped files, the format is "/html/organization/{orgId}/file/{id}/remove".
     * </p>
     */
    @Expose
    public String deleteUrl;
    
    /**
     * Visibility flag indicating if the file is publicly accessible.
     * <p>
     * This field is exposed in JSON serialization via {@code @Expose} annotation.
     * When {@code true}, the file can be accessed without authentication. When {@code false}
     * or {@code null}, the file requires appropriate privileges for access.
     * </p>
     */
    @Expose
    public Boolean publicFile;

    //TODO Rule 5.5: DTO should not have code
    /**
     * Computes the base URL path for file operations.
     * <p>
     * This private static helper method selects the appropriate base URL path based on
     * whether the file is globally accessible or organization-scoped. The logic is:
     * <ul>
     *   <li>If {@code organizationId} is {@code null}: Returns "/html/file/{id}" (global file path)</li>
     *   <li>If {@code organizationId} is present: Returns "/html/organization/{orgId}/file/{id}" (organization-scoped path)</li>
     * </ul>
     * </p>
     * <p>
     * The method uses static imports from {@code URLConstants}: {@code _HTML}, {@code _FILE},
     * and {@code _HTML_ORGANIZATION}.
     * </p>
     *
     * @param id the unique file identifier
     * @param organizationId the optional organization identifier, or {@code null} for global files
     * @return the computed base URL path without trailing slash
     */
    private static String getUrlBase(Long id, Long organizationId) {
        return organizationId == null ? (_HTML + _FILE + "/" + id) : (_HTML_ORGANIZATION + "/" + organizationId + _FILE + "/" + id);
    }

    /**
     * Constructs a FileDto with explicit downloadUrl override.
     * <p>
     * This constructor accepts five parameters including an explicit {@code downloadUrl}.
     * It delegates to the four-parameter constructor {@link #FileDto(Long, Long, String, String)}
     * to initialize fields and compute default URLs, then overrides the {@code downloadUrl}
     * with the provided value.
     * </p>
     *
     * @param id the unique file identifier
     * @param organizationId the optional organization identifier, or {@code null} for global files
     * @param filename the human-friendly file name
     * @param contentType the MIME type of the file content
     * @param downloadUrl the explicit download URL to use instead of the computed default
     */
    public FileDto(Long id, Long organizationId, String filename, String contentType, String downloadUrl) {
        this(id, organizationId, filename, contentType);
        this.downloadUrl = downloadUrl;
    }

    /**
     * Constructs a FileDto with computed download and delete URLs.
     * <p>
     * This four-parameter constructor initializes all fields and computes {@code downloadUrl}
     * and {@code deleteUrl} by calling {@link #getUrlBase(Long, Long)} and appending the
     * appropriate suffixes ("/content" and "/remove" respectively).
     * </p>
     * <p>
     * <b>Note:</b> If {@code id} or {@code organizationId} are changed after construction via
     * setters, the URLs will NOT be automatically recomputed. Callers must recreate the DTO
     * if identifiers change.
     * </p>
     *
     * @param id the unique file identifier
     * @param organizationId the optional organization identifier, or {@code null} for global files
     * @param filename the human-friendly file name
     * @param contentType the MIME type of the file content
     */
    public FileDto(Long id, Long organizationId, String filename, String contentType) {
        this.id = id;
        this.organizationId = organizationId;
        this.filename = filename;
        this.contentType = contentType;
        this.downloadUrl = getUrlBase(id, organizationId) + "/content";
        this.deleteUrl = getUrlBase(id, organizationId) + "/remove";
    }

    /**
     * Default no-argument constructor.
     * <p>
     * Provided for framework compatibility and JSON deserializers (e.g., Gson, Jackson).
     * All fields are initialized to their default values ({@code null} for reference types).
     * </p>
     */
    public FileDto() {
    }

    /**
     * Returns the human-friendly file name.
     *
     * @return the file name, or {@code null} if not set
     */
    public String getFilename() {
        return this.filename;
    }

    /**
     * Returns the MIME type of the file content.
     *
     * @return the content type, or {@code null} if not set
     */
    public String getContentType() {
        return this.contentType;
    }


    /**
     * Sets the human-friendly file name.
     *
     * @param filename the file name to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Sets the MIME type of the file content.
     *
     * @param contentType the content type to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }


    /**
     * Returns an empty string as the notification message.
     * <p>
     * This method implements the {@link CanonicalObject} contract. FileDto does not
     * provide a custom notification message, so it returns an empty string.
     * </p>
     *
     * @return an empty string
     */
    @Override
    public String notificationMessage() {
        return "";
    }

    /**
     * Returns the organization identifier for multi-tenant file scoping.
     * <p>
     * This method implements the {@link OrganizationRelatedObject} contract, allowing
     * the file to be associated with a specific organization for tenant isolation.
     * </p>
     *
     * @return the organization identifier, or {@code null} for global files
     */
    @Override
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the organization identifier for multi-tenant file scoping.
     * <p>
     * <b>Warning:</b> Changing the organization identifier after construction does NOT
     * automatically recompute the {@code downloadUrl} and {@code deleteUrl} fields.
     * Callers must recreate the DTO if this value changes.
     * </p>
     *
     * @param organizationId the organization identifier to set, or {@code null} for global files
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Returns the precomputed download endpoint URL.
     *
     * @return the download URL, or {@code null} if not set
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Sets the download endpoint URL.
     *
     * @param downloadUrl the download URL to set
     */
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /**
     * Returns the precomputed delete endpoint URL.
     *
     * @return the delete URL, or {@code null} if not set
     */
    public String getDeleteUrl() {
        return deleteUrl;
    }

    /**
     * Sets the delete endpoint URL.
     *
     * @param deleteUrl the delete URL to set
     */
    public void setDeleteUrl(String deleteUrl) {
        this.deleteUrl = deleteUrl;
    }

    /**
     * Returns the visibility flag indicating if the file is publicly accessible.
     *
     * @return {@code true} if the file is public, {@code false} if restricted, or {@code null} if not set
     */
    public Boolean getPublicFile() {
        return publicFile;
    }

    /**
     * Sets the visibility flag indicating if the file is publicly accessible.
     *
     * @param publicFile {@code true} for public access, {@code false} for restricted access
     */
    public void setPublicFile(Boolean publicFile) {
        this.publicFile = publicFile;
    }

    /**
     * Returns the unique file identifier.
     *
     * @return the file identifier, or {@code null} if not set
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique file identifier.
     * <p>
     * <b>Warning:</b> Changing the file identifier after construction does NOT
     * automatically recompute the {@code downloadUrl} and {@code deleteUrl} fields.
     * Callers must recreate the DTO if this value changes.
     * </p>
     *
     * @param id the file identifier to set
     */
    public void setId(Long id) {
        this.id = id;
    }
}