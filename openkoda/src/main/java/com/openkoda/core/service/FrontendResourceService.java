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

package com.openkoda.core.service;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.exception.FrontendResourceValidationException;
import com.openkoda.model.component.FrontendResource;
import jakarta.annotation.PostConstruct;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.stream.Stream;

import static com.openkoda.service.export.FolderPathConstants.SUBDIR_ORGANIZATION_PREFIX;

/**
 * Template lifecycle and validation service for FrontendResource entities.
 * <p>
 * This service manages the complete lifecycle of frontend resources including HTML validation,
 * draft/publish workflow, template loading, and URL generation. It validates HTML content using
 * Jsoup with a relaxed whitelist to ensure safe content while allowing common HTML tags.

 * <p>
 * Key responsibilities include:
 * <ul>
 *   <li>Validating HTML content using Jsoup.clean with relaxed whitelist</li>
 *   <li>Managing draft/publish workflow by transitioning draftContent to content field</li>
 *   <li>Reading classpath templates for defaults from /templates/frontend-resource/</li>
 *   <li>Preparing editable pages by splicing content between CONTENT_EDITABLE markers</li>
 *   <li>Computing resource hashes via MD5 for cache-busting URLs</li>
 *   <li>Resolving resource URLs with baseUrl prefix</li>
 * </ul>

 * <p>
 * HTML validation uses custom OutputSettings with prettyPrint disabled, HTML syntax mode,
 * and base entity escaping. The relaxed whitelist allows common HTML tags but blocks scripts
 * and other potentially dangerous content.

 * <p>
 * Example usage:
 * <pre>
 * service.validateContent(htmlContent, bindingResult, "content");
 * </pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FrontendResource
 * @see org.jsoup.Jsoup
 * @see org.jsoup.safety.Whitelist
 * @see ComponentProvider
 */
@Service
@Component("frontendResource")
public class FrontendResourceService extends ComponentProvider {

    /**
     * Static singleton instance reference initialized by {@link #init()} during {@code @PostConstruct} phase.
     * Provides access to this service instance from static contexts.
     */
    private FrontendResourceService instance;

    /**
     * HTML comment marker delimiting the start of editable content regions in templates.
     * Used by {@link #prepareFrontendResourcePage(Long, String)} to identify where to splice custom content.
     */
    public static final String CONTENT_EDITABLE_BEGIN = "<!--CONTENT EDITABLE START-->";
    
    /**
     * HTML comment marker delimiting the end of editable content regions in templates.
     * Used by {@link #prepareFrontendResourcePage(Long, String)} to identify where to splice custom content.
     */
    public static final String CONTENT_EDITABLE_END = "<!--CONTENT EDITABLE END-->";
    
    /**
     * Thymeleaf template name prefix for frontend resource template resolution.
     * Prepended to template names when resolving Thymeleaf templates.
     */
    public static final String frontendResourceTemplateNamePrefix = "frontend-resource/";
    
    /**
     * XML template path prefix for XML-based frontend resources.
     * Used when loading XML templates from the classpath.
     */
    public static final String frontendResourceXmlNamePrefix = "templates/xml/";
    
    /**
     * Classpath folder path for frontend resource template lookup.
     * All default templates are loaded from this base path: {@code /templates/frontend-resource/}
     */
    public static final String frontendResourceFolderClasspath = "/templates/frontend-resource/";

    /**
     * Default template name for frontend resource pages.
     * Configurable via {@code default.frontendResourcePage.template.name} property.
     * Defaults to "frontend-resource-template" if not specified.
     */
    @Value("${default.frontendResourcePage.template.name:frontend-resource-template}")
    String defaultFrontendResourcePageTemplate;

    /**
     * Base URL for the application, used in Jsoup HTML validation and resource URL construction.
     * Configurable via {@code base.url} property. Defaults to http://localhost:8080.
     */
    @Value("${base.url:http://localhost:8080}")
    String baseUrl;


    /**
     * Initializes the static instance reference after bean construction.
     * This {@code @PostConstruct} callback enables access to the service from static contexts.
     */
    @PostConstruct
    void init() {
        instance = this;
    }

    /**
     * Jsoup output configuration for HTML validation and cleaning operations.
     * <p>
     * Configuration:
     * <ul>
     *   <li>prettyPrint = false: No formatting whitespace added</li>
     *   <li>syntax = HTML: Uses HTML syntax mode</li>
     *   <li>escapeMode = base: Base entity escape mode for minimal escaping</li>
     * </ul>

     * Used by {@link #validateContent(String, BindingResult, String)} for consistent HTML processing.
     *
     * @see Document.OutputSettings
     */
    private Document.OutputSettings outputSettings =
            new Document.OutputSettings()
                    .prettyPrint(false)
                    .syntax(Document.OutputSettings.Syntax.html)
                    .escapeMode(Entities.EscapeMode.base);

    /**
     * Validates HTML content using Jsoup with a relaxed whitelist to ensure safe content.
     * <p>
     * This method cleans the provided HTML content using Jsoup.clean with the baseUrl and
     * a relaxed whitelist that allows common HTML tags but blocks scripts and potentially
     * dangerous content. If the cleaned content differs from the original, validation fails.

     * <p>
     * On validation failure:
     * <ul>
     *   <li>Rejects the specified BindingResult field with error code "not.valid"</li>
     *   <li>Throws FrontendResourceValidationException with cleaned content suggestion</li>
     * </ul>

     * <p>
     * Note: Uses relaxed Whitelist allowing common HTML tags (&lt;b&gt;, &lt;i&gt;, &lt;p&gt;, &lt;div&gt;, etc.)
     * but not scripts or dangerous attributes.

     *
     * @param content the HTML content to validate
     * @param br the BindingResult to add validation errors to (may be null)
     * @param dtoFieldName the DTO field name to reject on validation failure
     * @return true if validation passes (content equals cleaned content)
     * @throws FrontendResourceValidationException if validation fails, containing cleaned content suggestion
     * @see Jsoup#clean(String, String, Whitelist, Document.OutputSettings)
     * @see Whitelist#relaxed()
     */
    public boolean validateContent(String content, BindingResult br, String dtoFieldName) {
        debug("[validateContent] content: {}", content);
        String cleaned = Jsoup.clean(content, baseUrl, Whitelist.relaxed(), outputSettings);
        if (!content.equals(cleaned)) {
            if(br != null) {
                br.rejectValue(dtoFieldName, "not.valid");
            }
            throw new FrontendResourceValidationException(false, cleaned);
        }
        return true;
    }

    /**
     * Checks if a FrontendResource with the given name already exists in the database.
     * <p>
     * This method queries the unsecure repository (bypassing privilege checks) to find
     * an existing FrontendResource by name. If found, it rejects the BindingResult field
     * "dto.name" with error code "name.exists".

     * <p>
     * Warning: Uses unsecure repository, bypassing privilege checks for name uniqueness validation.

     *
     * @param name the FrontendResource name to check for existence
     * @param br the BindingResult to add validation errors to (may be null)
     * @return true if a FrontendResource with this name exists, false otherwise
     */
    public boolean checkNameExists(String name, BindingResult br) {
        debug("[checkNameExists] {}", name);
        boolean exists = repositories.unsecure.frontendResource.findByName(name) != null;
        if (exists && br != null) {
            br.rejectValue("dto.name", "name.exists");
        }
        return exists;
    }

    /**
     * Promotes draft content to published state by transitioning draftContent to content field.
     * <p>
     * This method implements the publish workflow:
     * <ul>
     *   <li>If resource is already published (not isDraft()), returns unchanged</li>
     *   <li>Copies draftContent to content field</li>
     *   <li>Clears draftContent by setting to null</li>
     * </ul>

     * <p>
     * Note: This method modifies the entity but does not persist it. Caller must save the returned entity.

     *
     * @param frontendResource the FrontendResource to publish
     * @return the modified FrontendResource with published content (not persisted)
     * @see #clear(FrontendResource)
     * @see #publishAll(Stream)
     */
    public FrontendResource publish(FrontendResource frontendResource) {
        debug("[publish] {}", frontendResource);
        if (not(frontendResource.isDraft())) {
            return frontendResource;
        }
        frontendResource.setContent( frontendResource.getDraftContent() );
        frontendResource.setDraftContent( null );
        return frontendResource;
    }

    /**
     * Reverts published content to draft state, opposite of {@link #publish(FrontendResource)}.
     * <p>
     * This method implements the clear/unpublish workflow:
     * <ul>
     *   <li>If resource has no published content (not isContentExists()), returns unchanged</li>
     *   <li>Copies content to draftContent field</li>
     *   <li>Clears content by setting to null</li>
     * </ul>

     * <p>
     * Note: This method modifies the entity but does not persist it. Caller must save the returned entity.

     *
     * @param frontendResource the FrontendResource to clear/unpublish
     * @return the modified FrontendResource with content moved to draft (not persisted)
     * @see #publish(FrontendResource)
     * @see #clearAll(Stream)
     */
    public FrontendResource clear(FrontendResource frontendResource) {
        debug("[clear] {}", frontendResource);
        if (not(frontendResource.isContentExists())) {
            return frontendResource;
        }
        frontendResource.setDraftContent( frontendResource.getContent() );
        frontendResource.setContent( null );
        return frontendResource;
    }

    /**
     * Bulk publish operation applying {@link #publish(FrontendResource)} to all resources in stream.
     * <p>
     * This method iterates over the stream, publishes each FrontendResource, and persists
     * the changes using the unsecure repository. Each resource's draft content is promoted
     * to published state.

     *
     * @param result the stream of FrontendResource entities to publish
     * @return the transformed stream of published resources
     * @see #publish(FrontendResource)
     */
    public Stream<FrontendResource> publishAll(Stream<FrontendResource> result) {
        debug("[publishAll]");
        result.forEach( a -> repositories.unsecure.frontendResource.save(publish(a)));
        return result;
    }

    /**
     * Bulk clear operation applying {@link #clear(FrontendResource)} to all resources in stream.
     * <p>
     * This method iterates over the stream, clears each FrontendResource, and persists
     * the changes using the unsecure repository. Each resource's published content is moved
     * back to draft state.

     *
     * @param result the stream of FrontendResource entities to clear
     * @return the transformed stream of cleared resources
     * @see #clear(FrontendResource)
     */
    public Stream<FrontendResource> clearAll(Stream<FrontendResource> result) {
        debug("[clearAll]");
        result.forEach( a -> repositories.unsecure.frontendResource.save(clear(a)));
        return result;
    }

    /**
     * Retrieves FrontendResource content by type, name, access level, and organization ID.
     * <p>
     * This method attempts to load content from the classpath using the constructed resource path.
     * If not found in the database, it falls back to the default classpath template from
     * {@code /templates/frontend-resource/}. The resource path is constructed based on:
     * <ul>
     *   <li>Access level path (GLOBAL, ORGANIZATION)</li>
     *   <li>Organization-specific subdirectory if organizationId provided</li>
     *   <li>Resource name with type extension (.html, .js, etc.)</li>
     * </ul>

     * <p>
     * Warning: Uses unsecure repository bypassing privilege checks. IOException silently returns empty string.

     *
     * @param type the FrontendResource type (HTML, JS, CSS, etc.)
     * @param frontendResourceName the resource name without extension
     * @param accessLevel the access level (GLOBAL or ORGANIZATION)
     * @param organizationId the organization ID (null for global resources)
     * @return the resource content as String, or empty string if not found or on IOException
     */
    public String getContentOrDefault(FrontendResource.Type type, String frontendResourceName, FrontendResource.AccessLevel accessLevel, Long organizationId) {
        debug("[getContent] {}", frontendResourceName);
        String contentBasePath = frontendResourceFolderClasspath;
        contentBasePath += accessLevel.getPath();

        String resourceName = (organizationId == null || accessLevel == FrontendResource.AccessLevel.GLOBAL)
                ?
                contentBasePath + frontendResourceName + type.getExtension() :
                contentBasePath + SUBDIR_ORGANIZATION_PREFIX + organizationId + "/" + frontendResourceName + type.getExtension();
        try {
            InputStream resourceIO = this.getClass().getResourceAsStream(resourceName);
            if (resourceIO != null) {
                return IOUtils.toString(resourceIO);
            }
        } catch (IOException e) {
            error("Could not load FrontendResource from templates", e);
        }
        return "";
    }

    /**
     * Constructs URL for classpath resource corresponding to the given FrontendResource.
     * <p>
     * The URL is constructed from:
     * <ul>
     *   <li>Base classpath folder ({@link #frontendResourceFolderClasspath})</li>
     *   <li>Access level path</li>
     *   <li>Organization-specific subdirectory if applicable</li>
     *   <li>Resource name with extension (.html for HTML type, empty otherwise)</li>
     * </ul>

     *
     * @param frontendResource the FrontendResource entity
     * @return URL object for the classpath resource, or null if not found
     * @see #resourceExists(FrontendResource)
     */
    public URL resourceURL(FrontendResource frontendResource) {
        String extension = frontendResource.getType() == FrontendResource.Type.HTML ? ".html" : "";
        return  frontendResource.getOrganizationId() == null ?
                this.getClass().getResource(frontendResourceFolderClasspath
                        + frontendResource.getAccessLevel().getPath()
                        + frontendResource.getName()
                        + extension)
                : this.getClass().getResource(frontendResourceFolderClasspath
                        + frontendResource.getAccessLevel().getPath()
                        + SUBDIR_ORGANIZATION_PREFIX
                        + frontendResource.getOrganizationId()
                        + "/"
                        + frontendResource.getName()
                        + extension);
    }

    /**
     * Checks if a classpath resource exists for the given FrontendResource.
     * <p>
     * This method delegates to {@link #resourceURL(FrontendResource)} and checks for null.

     *
     * @param frontendResource the FrontendResource entity to check
     * @return true if the classpath resource exists, false otherwise
     * @see #resourceURL(FrontendResource)
     */
    public boolean resourceExists(FrontendResource frontendResource) {
        return resourceURL(frontendResource) != null;
    }

    /**
     * Prepares an editable FrontendResource page by splicing content between template markers.
     * <p>
     * This method loads a FrontendResource by ID (or creates a new one if null), then constructs
     * an editable page by:
     * <ul>
     *   <li>Extracting content before {@link #CONTENT_EDITABLE_BEGIN} marker</li>
     *   <li>Inserting the provided contentEditable</li>
     *   <li>Appending content after {@link #CONTENT_EDITABLE_END} marker</li>
     * </ul>

     * <p>
     * If frontendResourceId is null, uses the default template specified by
     * {@link #defaultFrontendResourcePageTemplate}.

     *
     * @param frontendResourceId the ID of the FrontendResource to edit (null for new resource)
     * @param contentEditable the editable content to splice between markers
     * @return the prepared FrontendResource with spliced content
     */
    public FrontendResource prepareFrontendResourcePage(Long frontendResourceId, String contentEditable) {
        FrontendResource frontendResource = frontendResourceId == null ? new FrontendResource() : repositories.unsecure.frontendResource.findOne(frontendResourceId);
        FrontendResource baseFrontendResource = frontendResourceId == null ? repositories.unsecure.frontendResource.findByName(defaultFrontendResourcePageTemplate) : frontendResource;
        String before = StringUtils.substringBefore(baseFrontendResource.getContent(), CONTENT_EDITABLE_BEGIN);
        String after = StringUtils.substringAfter(baseFrontendResource.getContent(), CONTENT_EDITABLE_END);
        frontendResource.setContent(before + contentEditable + after);
        return frontendResource;
    }

    /**
     * Prepares a FrontendResource page entity without contentEditable splicing.
     * <p>
     * This is a variant of {@link #prepareFrontendResourcePage(Long, String)} that returns
     * the raw entity without splicing custom content between markers. If frontendResourceId
     * is null, creates a new FrontendResource and copies content from the default template.

     *
     * @param frontendResourceId the ID of the FrontendResource (null for new resource with default template)
     * @return the FrontendResource entity with content
     * @see #prepareFrontendResourcePage(Long, String)
     */
    public FrontendResource prepareFrontendResourcePageEntity(Long frontendResourceId) {
        FrontendResource result = null;
        if (frontendResourceId == null) {
            result = new FrontendResource();
            FrontendResource base = repositories.unsecure.frontendResource.findByName(defaultFrontendResourcePageTemplate);
            result.setContent(base.getContent());
        } else {
            result = repositories.unsecure.frontendResource.findOne(frontendResourceId);
        }
        return result;
    }

    /**
     * Computes MD5 hash of FrontendResource content for cache-busting URLs.
     * <p>
     * This method:
     * <ul>
     *   <li>Retrieves content using {@link #getContentOrDefault}</li>
     *   <li>Computes MD5 hash using {@link DigestUtils#md5Hex(String)}</li>
     *   <li>Truncates to {@link FrontendResource#HASH_TRUNCATED_LENGTH} characters</li>
     * </ul>

     * <p>
     * The truncated hash is appended to resource URLs as a query parameter for cache invalidation.

     *
     * @param frontendResource the FrontendResource to compute hash for
     * @return truncated MD5 hash string for cache-busting
     * @see DigestUtils#md5Hex(String)
     * @see #getResourceURL(FrontendResource)
     */
    public String resourceHash(FrontendResource frontendResource) {
        String content = getContentOrDefault(frontendResource.getType(), frontendResource.getName(), frontendResource.getAccessLevel(), frontendResource.getOrganizationId());
        String md5 = DigestUtils.md5Hex(content);
        return StringUtils.substring(md5, 0, FrontendResource.HASH_TRUNCATED_LENGTH);
    }

    /**
     * Constructs the public URL for a FrontendResource with baseUrl prefix.
     * <p>
     * This method calls {@link #resourceURL(FrontendResource)} to get the classpath URL,
     * then converts it to a string. If the resource doesn't exist, returns empty string.
     * The URL typically includes a hash query parameter for cache invalidation.

     *
     * @param frontendResource the FrontendResource entity
     * @return the public URL string, or empty string if resource not found
     * @see #resourceURL(FrontendResource)
     * @see #resourceHash(FrontendResource)
     */
    public String getResourceURL(FrontendResource frontendResource) {
        URL url = resourceURL(frontendResource);
        return url == null ? "" : url.toString();
    }

}
