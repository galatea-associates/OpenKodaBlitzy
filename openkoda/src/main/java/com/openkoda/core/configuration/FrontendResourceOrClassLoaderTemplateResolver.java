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

package com.openkoda.core.configuration;

import com.openkoda.controller.common.URLConstants;
import com.openkoda.core.configuration.TemplatePathFilteringProcessor.FilteredTemplatePath;
import com.openkoda.core.multitenancy.QueryExecutor;
import com.openkoda.core.multitenancy.TenantResolver;
import com.openkoda.core.service.FrontendResourceService;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.component.FrontendResource;
import com.openkoda.model.component.FrontendResource.Type;
import com.openkoda.service.export.ClasspathComponentImportService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

import java.util.List;
import java.util.Map;

import static com.openkoda.core.service.FrontendResourceService.frontendResourceFolderClasspath;
import static com.openkoda.core.service.FrontendResourceService.frontendResourceTemplateNamePrefix;
import static com.openkoda.service.export.FolderPathConstants.FRONTEND_RESOURCE_;
import static com.openkoda.service.export.FolderPathConstants.UI_COMPONENT_;

/**
 * Thymeleaf template resolver with tenant-aware frontend resource loading from database or classpath fallback.
 * <p>
 * Extends ClassLoaderTemplateResolver to implement a custom template resolution strategy for multi-tenant applications.
 * When a template name starts with 'frontend-resource#', this resolver attempts to load the template from a FrontendResource
 * entity in the database, enabling tenant-specific template customization. If the template is not found in the database
 * and frontendResourceCreateIfNotExist is enabled, it automatically creates the entity from the corresponding classpath resource.
 * Falls back to the packaged error.html template if all resolution attempts fail.
 * <p>
 * <b>Template Resolution Order:</b>
 * <ol>
 *   <li>Check for RESOURCE URL flag → force load from classpath</li>
 *   <li>Load from database FrontendResource entity for current organization</li>
 *   <li>If not found and auto-create enabled → import from classpath and save to database</li>
 *   <li>Fallback to classpath resource</li>
 *   <li>Ultimate fallback to error.html template</li>
 * </ol>
 * <p>
 * <b>Tenant Awareness:</b> Uses TenantResolver to determine the current organization context. Queries FrontendResource
 * entities scoped to the current organization, allowing template customization per tenant. Templates can have different
 * access levels (PUBLIC, ORGANIZATION, GLOBAL) with priority-based resolution when multiple matches exist.
 * <p>
 * <b>Configuration Flags:</b>
 * <ul>
 *   <li>frontendResourceLoadAlwaysFromResources: When true, bypasses database lookup and always loads from classpath</li>
 *   <li>frontendResourceCreateIfNotExist: When true, auto-creates database entities from classpath templates on first access</li>
 * </ul>
 * Supports URL flags RESOURCE (force classpath loading) and DRAFT (return draft content version) for testing purposes.
 * <p>
 * <b>Thread Safety:</b> Instance fields are final or injected Spring beans. Uses QueryExecutor.runEntityManagerOperationInTransaction
 * for transaction management, ensuring thread-safe database operations in multi-tenant context.
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ClassLoaderTemplateResolver
 * @see FrontendResourceService
 * @see QueryExecutor
 * @see FrontendResource
 * @see ClasspathComponentImportService
 */
public class FrontendResourceOrClassLoaderTemplateResolver extends ClassLoaderTemplateResolver implements LoggingComponentWithRequestId {

    /**
     * Configuration flag: when true, bypasses database lookup and always loads templates from classpath resources.
     * Useful for development and testing environments where database customizations should be ignored.
     */
    private final boolean frontendResourceLoadAlwaysFromResources;
    
    /**
     * Configuration flag: when true, automatically creates FrontendResource database entities from classpath templates
     * if not found during resolution. Enables dynamic template management and allows on-demand template registration.
     */
    private final boolean frontendResourceCreateIfNotExist;

    /**
     * Injected HTTP servlet request for accessing URL parameters and request context.
     * Used to check for RESOURCE and DRAFT flags in template resolution, enabling testing
     * of classpath resources and draft template versions.
     */
    @Autowired
    private HttpServletRequest request;

    /**
     * Service for FrontendResource entity operations: database queries, classpath resource loading,
     * and template content retrieval. Handles content resolution across multiple access levels and
     * organization contexts.
     */
    FrontendResourceService frontendResourceService;
    
    /**
     * Service for importing component files from classpath into database FrontendResource entities.
     * Used during auto-creation of templates to populate database from packaged resources,
     * supporting both FRONTEND_RESOURCE_ and UI_COMPONENT_ folder paths.
     */
    ClasspathComponentImportService classpathComponentImportService;

    /**
     * Transaction executor for database operations in multi-tenant environment. Ensures queries
     * execute in correct tenant context with proper transaction boundaries. Used for FrontendResource
     * persistence, queries, and content updates.
     */
    QueryExecutor queryExecutor;

    /**
     * Utility component for parsing template paths: extracts access levels, email flags, and normalized
     * resource names from template URLs. Processes 'frontend-resource#' prefixes to determine
     * FrontendResource entry names and access control settings.
     */
    private final TemplatePathFilteringProcessor pathFilteringProcessor;

    /**
     * Constructs tenant-aware template resolver with database and classpath resolution capabilities.
     * <p>
     * Initializes the resolver with services for database operations, template loading, and path processing.
     * Configuration flags control whether templates are always loaded from classpath and whether database
     * entities are auto-created from classpath resources when not found.
     * 
     *
     * @param queryExecutor transaction executor for database operations in multi-tenant context,
     *                      ensuring proper tenant isolation and transaction boundaries
     * @param frontendResourceService service for FrontendResource entity operations and classpath
     *                                resource lookup, handling content resolution across access levels
     * @param classpathComponentImportService service for importing template files from classpath into
     *                                        database entities during auto-creation process
     * @param frontendResourceLoadAlwaysFromResources true to bypass database and always load from classpath
     *                                                (useful for development and testing)
     * @param frontendResourceCreateIfNotExist true to auto-create database entities from classpath templates
     *                                         on first access (enables dynamic template management)
     * @param pathFilteringProcessor utility for parsing template paths and extracting access levels
     *                               and email flags from template URLs
     */
    public FrontendResourceOrClassLoaderTemplateResolver(QueryExecutor queryExecutor,
                                                         FrontendResourceService frontendResourceService,
                                                         ClasspathComponentImportService classpathComponentImportService,
                                                         boolean frontendResourceLoadAlwaysFromResources,
                                                         boolean frontendResourceCreateIfNotExist,
                                                         TemplatePathFilteringProcessor pathFilteringProcessor) {
        this.queryExecutor = queryExecutor;
        this.frontendResourceService = frontendResourceService;
        this.classpathComponentImportService = classpathComponentImportService;
        this.frontendResourceLoadAlwaysFromResources = frontendResourceLoadAlwaysFromResources;
        this.frontendResourceCreateIfNotExist = frontendResourceCreateIfNotExist;
        this.pathFilteringProcessor = pathFilteringProcessor;
    }

    /**
     * Resolves Thymeleaf template resource from database or classpath based on template name and flags.
     * <p>
     * Core resolution method invoked by the Thymeleaf engine during template processing. Analyzes the template
     * name prefix and URL flags to determine the appropriate resolution strategy. For templates starting with
     * 'frontend-resource#', this method extracts the access level, checks URL flags (RESOURCE/DRAFT), queries
     * the database for the FrontendResource entity, auto-creates if missing (when enabled), and returns a
     * StringTemplateResource with the template content. For standard templates, delegates to the superclass
     * ClassLoaderTemplateResolver for traditional classpath loading.
     * 
     * <p>
     * <b>Resolution Logic for 'frontend-resource#' Templates:</b>
     * <ol>
     *   <li>Parse template path to extract FrontendResource entry name and access level</li>
     *   <li>If RESOURCE URL parameter present, return content directly from classpath</li>
     *   <li>Query database for FrontendResource entity matching name, access level, and organization</li>
     *   <li>If not found, attempt to load from classpath component folders (FRONTEND_RESOURCE_, UI_COMPONENT_)</li>
     *   <li>If still not found and auto-create enabled, create new database entity from classpath</li>
     *   <li>If entity exists but lacks content, populate from classpath resource</li>
     *   <li>Return draft content if DRAFT URL parameter present, otherwise return regular content</li>
     * </ol>
     * Uses priority-based query to handle multiple access level matches, preferring organization-specific over global templates.
     * 
     * <p>
     * <b>Transaction Management:</b> Uses QueryExecutor.runEntityManagerOperationInTransaction for database queries
     * to ensure proper transaction boundaries and tenant context isolation in multi-tenant deployments.
     * 
     *
     * @param configuration Thymeleaf engine configuration (unused but required by interface contract)
     * @param ownerTemplate parent template name for fragment resolution (unused in current implementation)
     * @param template template name to resolve (e.g., 'frontend-resource#mytemplate.html' or standard classpath reference)
     * @param resourceName original resource name before Thymeleaf preprocessing and normalization
     * @param characterEncoding template character encoding (typically UTF-8 for web templates)
     * @param templateResolutionAttributes additional resolution attributes from Thymeleaf context (unused in current implementation)
     * @return ITemplateResource containing template content - StringTemplateResource for database templates,
     *         ClassLoaderTemplateResource for packaged classpath templates, or error template if resolution fails
     * @see ITemplateResource
     * @see StringTemplateResource
     * @see FilteredTemplatePath
     * @see FrontendResource
     */
    @Override
    protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration, String ownerTemplate, String template, String resourceName, String characterEncoding, Map<String, Object> templateResolutionAttributes) {
        trace("[computeTemplateResource] ownerTemplate {} template {} resource {}", ownerTemplate, template, resourceName);

        //if the template name starts with the frontendResourceTemplateNamePrefix, use the custom routine
        if (StringUtils.startsWith(template, frontendResourceTemplateNamePrefix)) {
            TenantResolver.TenantedResource tenantedResource = TenantResolver.getTenantedResource();

            //resolve the actual template name
            FilteredTemplatePath filteredTemplatePath = pathFilteringProcessor.processTemplatePath(template,
                    resourceName, tenantedResource.accessLevel);

            //if request has URLConstants#RESOURCE parameter, then return template content from filesystem, not from the DB
            boolean isResourceTesting = (isHttpRequest() && request.getParameter(URLConstants.RESOURCE) != null);
            if(isResourceTesting) {
                return new StringTemplateResource(
                        getResourceContent(filteredTemplatePath.getFrontendResourceEntryName(),
                                filteredTemplatePath.getAccessLevel(), tenantedResource.organizationId));
            }


            //...else, try to find template in database
            FrontendResource.AccessLevel finalAccessLevel = filteredTemplatePath.getAccessLevel();
            List<Object[]> entries = queryExecutor.runEntityManagerOperationInTransaction(em ->
                    em.createQuery("select c, case  " +
                                            "when c.accessLevel = 'PUBLIC' and c.organizationId is not null then 1 " +
                                            "when c.accessLevel = 'PUBLIC' and c.organizationId is null then 2 " +
                                            "when c.accessLevel = 'GLOBAL' and c.organizationId is not null then 1 " +
                                            "when c.accessLevel = 'GLOBAL' and c.organizationId is null then 2 " +
                                            "when c.accessLevel = 'ORGANIZATION' and c.organizationId is not null then 1 " +
                                            "when c.accessLevel = 'ORGANIZATION' and c.organizationId is null then 2 " +
                                            "when c.accessLevel = 'GLOBAL' and 'ORGANIZATION' = :p3 and c.organizationId is not null then 3 " +
                                            "when c.accessLevel = 'GLOBAL' and 'ORGANIZATION' = :p3 and c.organizationId is null then 4 " +
                                            "else 10 end as priority " +
                                            "from FrontendResource c " +
                                            "where " +
                                            "c.name = :p1 and " +
                                            "(cast (c.accessLevel as text) = :p3 or (cast (c.accessLevel as text) = 'GLOBAL' and 'ORGANIZATION' = :p3)) and " +
                                            "(c.organizationId = :p2 OR c.organizationId is NULL) order by priority limit 1"
                                    , Object[].class)
                            .setParameter("p1", filteredTemplatePath.getFrontendResourceEntryName())
                            .setParameter("p2", tenantedResource.organizationId)
                            .setParameter("p3", finalAccessLevel.toString())
                            .getResultList());
            FrontendResource entry = entries == null || entries.isEmpty() ? null : (FrontendResource) entries.get(0)[0];

            //if entry not found in the database...
            if (entry == null) {

                entry = (FrontendResource) classpathComponentImportService.loadResourceFromFile(FRONTEND_RESOURCE_,
                        filteredTemplatePath.getAccessLevel(), tenantedResource.organizationId,
                        filteredTemplatePath.getFrontendResourceEntryName());

                if (entry == null) {
//                    try ui component
                    entry = (FrontendResource) classpathComponentImportService.loadResourceFromFile(UI_COMPONENT_,
                            filteredTemplatePath.getAccessLevel(), tenantedResource.organizationId,
                            filteredTemplatePath.getFrontendResourceEntryName());
                    if(entry == null && filteredTemplatePath.getAccessLevel().equals(FrontendResource.AccessLevel.ORGANIZATION)) {
                        // try to find global level ui component
                        entry = (FrontendResource) classpathComponentImportService.loadResourceFromFile(UI_COMPONENT_,
                                FrontendResource.AccessLevel.GLOBAL, tenantedResource.organizationId,
                                filteredTemplatePath.getFrontendResourceEntryName());
                    }
                }

                if (entry == null) {
                    //...try to create if from filesystem
                    entry = createEntry(filteredTemplatePath.getFrontendResourceEntryName(),
                            filteredTemplatePath.getAccessLevel(), tenantedResource.organizationId);
                    if(entry == null && filteredTemplatePath.getAccessLevel().equals(FrontendResource.AccessLevel.ORGANIZATION)) {
                        // try to find global level ui component
                        entry =createEntry(filteredTemplatePath.getFrontendResourceEntryName(),
                                FrontendResource.AccessLevel.GLOBAL, tenantedResource.organizationId);
                    }
                }

                //if the entry was not created (eg. not found in filesystem), return error template
                if (entry == null) {
                    return getErrorTemplate(configuration, ownerTemplate, template, characterEncoding, templateResolutionAttributes);
                }
            } else if (frontendResourceLoadAlwaysFromResources) {
                // else set content from filesystem if the resource should always be read from filesystem
                entry.setContent(
                        getContentOrNull(entry.getType(), filteredTemplatePath.getFrontendResourceEntryName(),
                                filteredTemplatePath.getAccessLevel(), tenantedResource.organizationId));
            } else if (not(entry.isContentExists())) {
                // else if the database entry does not have content, read it from filesystem
                entry = fillFrontendResourceEntryContentFromResource(entry,
                        filteredTemplatePath.getFrontendResourceEntryName(), filteredTemplatePath.getAccessLevel(),
                        tenantedResource.organizationId);
            }

            // if request parameter indicates it's a test of draft version of the Frontend resource,
            // then return draft content, otherwise return regular content
            boolean isDraftTesting = (isHttpRequest() && request.getParameter(URLConstants.DRAFT) != null);
            String content = entry.isDraft() && isDraftTesting ? entry.getDraftContent() : entry.getContent();
            return new StringTemplateResource(content);
        }
        //...else delegate template resolution to the super class implementation.
        return super.computeTemplateResource(configuration, ownerTemplate,
                template, resourceName, characterEncoding, templateResolutionAttributes);
    }



    private ITemplateResource getErrorTemplate(IEngineConfiguration configuration, String ownerTemplate, String template, String characterEncoding, Map<String, Object> templateResolutionAttributes) {
        warn("[computeTemplateResource] template '{}' not found neither in db nor in resources", template);
        return super.computeTemplateResource(configuration, ownerTemplate,
                frontendResourceTemplateNamePrefix + "error", frontendResourceFolderClasspath + "error.html", characterEncoding, templateResolutionAttributes);
    }


    private FrontendResource createEntry(String entryName, FrontendResource.AccessLevel accessLevel, Long organizationId) {
        debug("[createEntry] {}", entryName);

        FrontendResource.Type type = Type.getEntryTypeFromPath(entryName);
        String content = getContentOrNull(type, entryName, accessLevel, organizationId);

        //create frontendResource entry when content in resources exists
        //or stub content when not in resources
        if (content != null || frontendResourceCreateIfNotExist) {
            FrontendResource result = new FrontendResource();
            result.setName(entryName);
            result.setType(type);
            result.setContent(content);
            result.setAccessLevel(accessLevel);
            result.setOrganizationId(organizationId);
            result.setEmbeddable(false);
            result.setIncludeInSitemap(false);
            queryExecutor.runEntityManagerOperationInTransaction(em -> {em.persist(result); em.flush(); return null;});
            return result;
        }

        return null;
    }

    private FrontendResource fillFrontendResourceEntryContentFromResource(final FrontendResource entry, String entryName, FrontendResource.AccessLevel accessLevel, Long organizationId) {
        return queryExecutor.runEntityManagerOperationInTransaction(em -> {
            entry.setContent(getContentOrDefault(entry.getType(), entryName, accessLevel, organizationId));
            return em.merge(entry);
        });
    }

    private String getContentOrNull(Type type, String frontendResourceEntryName, FrontendResource.AccessLevel accessLevel, Long organizationId) {
        debug("[getContentOrNull] {}", frontendResourceEntryName);

        String content = frontendResourceService.getContentOrDefault(type, frontendResourceEntryName, accessLevel, organizationId);
        if(StringUtils.isNotBlank(content)) {
            return content;
        }

        return null;
    }

    private String getContentOrDefault(Type result, String frontendResourceEntryName, FrontendResource.AccessLevel accessLevel, Long organizationId) {
        debug("[getContentOrDefault] {}", frontendResourceEntryName);
        String content = getContentOrNull(result, frontendResourceEntryName, accessLevel, organizationId);
        return content != null ? content : String.format("Add content here [%s]", frontendResourceTemplateNamePrefix + frontendResourceEntryName);
    }

    private String getResourceContent(String frontendResourceEntryName, FrontendResource.AccessLevel accessLevel, Long organizationId) {
        FrontendResource.Type type = Type.getEntryTypeFromPath(frontendResourceEntryName);
        return frontendResourceService.getContentOrDefault(type, frontendResourceEntryName, accessLevel, organizationId);
    }

    private boolean isHttpRequest() {
        return request != null && RequestContextHolder.getRequestAttributes() != null;
    }
}
