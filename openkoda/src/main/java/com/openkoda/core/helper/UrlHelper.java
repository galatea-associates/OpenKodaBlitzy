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

package com.openkoda.core.helper;

import com.openkoda.controller.common.URLConstants;
import com.openkoda.core.multitenancy.TenantResolver;
import com.openkoda.model.Organization;
import com.openkoda.model.common.LongIdEntity;
import com.openkoda.model.component.FrontendResource;
import com.openkoda.model.component.event.EventListenerEntry;
import com.openkoda.model.file.File;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centralizes canonical URL composition, servlet path parsing, tenant resolution from URLs,
 * preview link generation, pagination helpers, and file URL encoding.
 * <p>
 * This {@code @Primary} Spring bean provides a comprehensive URL management facility for the
 * OpenKoda platform. It serves as the authoritative source for constructing tenant-aware URLs,
 * parsing organization identifiers from request paths, and generating properly encoded links
 * for entities, files, and frontend resources.
 * </p>
 * <p>
 * The class uses a static instance pattern via {@link #getInstance()} to support legacy code
 * that cannot use dependency injection. The {@link #init()} method, invoked by Spring's
 * {@code @PostConstruct}, populates this static reference after bean initialization.
 * </p>
 * <p>
 * <b>Multi-Tenancy Support:</b> Organization-scoped URL patterns enable tenant isolation.
 * URLs follow the format {@code /html/organization/{orgId}/{entityKey}} for tenant-specific
 * resources and {@code /html/{entityKey}} for global resources. Compiled regex patterns
 * extract organization IDs from incoming request paths.
 * </p>
 * <p>
 * <b>Thread Safety:</b> Static Pattern instances are compiled once during class loading and
 * are thread-safe. Instance methods use injected {@code @Value} properties that remain constant
 * after initialization.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * // Injected usage
 * String url = urlHelper.entityBase(orgId, "users");
 * 
 * // Static instance access
 * Long orgId = UrlHelper.getOrganizationIdFromUrlOrNull(request.getServletPath());
 * }</pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see TenantResolver
 * @see URLConstants
 */
@Component("url")
@Primary
public class UrlHelper implements URLConstants, ReadableCode {

    /**
     * Static instance for non-dependency injection contexts.
     * Populated by {@link #init()} via {@code @PostConstruct}.
     * Enables legacy code access through {@link #getInstance()}.
     */
    private static UrlHelper instance;

    /**
     * Cryptographically secure random number generator for generating random values.
     * Used by {@link #randomInt(int, int)} for unpredictable integer generation.
     */
    private final static Random random = new SecureRandom();

    /**
     * Regex pattern for extracting organization ID from organization-scoped URL paths.
     * Matches paths like {@code /html/organization/{orgId}/...} and captures the organization ID in group 1.
     * Compiled once during class loading for thread-safe reuse.
     */
    private static final Pattern htmlOrganizationPath = Pattern.compile(URLConstants._HTML_ORGANIZATION + "/([0-9]+).*$");
    
    /**
     * Regex pattern for extracting mapping key from URL paths.
     * Matches paths like {@code /html/organization/{orgId}/{mappingKey}/...} or {@code /html/{mappingKey}/...}
     * and captures the mapping key in group 2. Supports alphanumeric keys with hyphens and underscores.
     */
    private static final Pattern mappingKeyPath = Pattern.compile(URLConstants._HTML + "(" + _ORGANIZATION + "/[0-9]+)?/([0-9A-Za-z-_]+)/.*$");
    
    /**
     * Regex pattern for extracting both organization ID and entity key from URL paths.
     * Matches paths like {@code /html/organization/{orgId}/{entityKey}/...} or {@code /html/{entityKey}/...}.
     * Captures organization ID in group 2 and entity key in group 3.
     * Used by {@link #getTenantedResource(HttpServletRequest)} for tenant resolution.
     */
    private static final Pattern organizationIdAndEntityKeyPath = Pattern.compile(URLConstants._HTML + "(" + _ORGANIZATION + "/([0-9]+))?/([0-9A-Za-z-_]+)?(/.*)?$");

    /**
     * Base URL for the OpenKoda application instance.
     * Injected from {@code base.url} property with default {@code http://localhost:8080}.
     * Used by {@link #getAbsoluteFileURL(File)} and affiliation link generation.
     */
    @Value("${base.url:http://localhost:8080}")
    private String baseUrl;

    /**
     * URL template for organization-specific logo image links.
     * Injected from {@code logo.image.href} property with default {@code /html/organization/%s/dashboard}.
     * The {@code %s} placeholder is replaced with organization ID via {@link String#format(String, Object...)}.
     */
    @Value("${logo.image.href:/html/organization/%s/dashboard}")
    private String logoImageHref;
    
    /**
     * URL for global (non-organization-specific) logo image links.
     * Injected from {@code logo.image.href.global} property with default {@code /html/organization/all}.
     * Used when organization ID is null in {@link #logoImageUrl(Long)}.
     */
    @Value("${logo.image.href.global:/html/organization/all}")
    private String logoImageHrefGlobal;

    /**
     * Initializes the static instance for legacy non-DI access.
     * Invoked by Spring after dependency injection completes.
     * Enables {@link #getInstance()} to return this bean instance.
     */
    @PostConstruct void init() {
        instance = this;
    }

    /**
     * Returns the static UrlHelper instance for non-dependency injection contexts.
     * <p>
     * Provides access to URL helper functionality in legacy code that cannot use
     * Spring's dependency injection. The instance is populated by {@link #init()}
     * after Spring initializes the bean.
     * </p>
     *
     * @return the singleton UrlHelper instance
     */
    public static UrlHelper getInstance() {
        return instance;
    }

    /**
     * Returns the configured base URL for this OpenKoda instance.
     *
     * @return the base URL (e.g., {@code http://localhost:8080})
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Returns the base URL or empty string if instance is not yet initialized.
     * Safe to call before Spring context initialization completes.
     *
     * @return the base URL, or empty string if {@link #instance} is null
     */
    public static String getBaseUrlOrEmpty() {
        return instance == null ? "" : instance.baseUrl;
    }

    /**
     * Returns the base URL path for entity CRUD operations.
     * <p>
     * Generates tenant-aware URLs following the pattern:
     * <ul>
     *   <li>Organization-scoped: {@code /html/organization/{orgId}/{entityKey}}</li>
     *   <li>Global (orgId is null): {@code /html/{entityKey}}</li>
     *   <li>Organization entity: {@code /html/organization} (special case)</li>
     * </ul>
     * </p>
     *
     * @param organizationId the tenant organization ID, or null for global resources
     * @param entityKey the entity type identifier (e.g., "users", "roles", "organization")
     * @return the base URL path for the entity
     */
    public String entityBase(Long organizationId, String entityKey) {
        return organizationId == null || entityKey.equals(ORGANIZATION) ? _HTML + "/" + entityKey : _HTML + _ORGANIZATION + "/" + organizationId + "/" + entityKey;
    }

    /**
     * Returns the base URL path for a global (non-organization-scoped) entity.
     *
     * @param entityKey the entity type identifier
     * @return the base URL path {@code /html/{entityKey}}
     */
    public String entityBase(String entityKey) {
        return entityBase(null, entityKey);
    }

    /**
     * Returns the base URL path for a specific global entity instance.
     *
     * @param entityKey the entity type identifier
     * @param id the entity instance ID
     * @return the base URL path {@code /html/{entityKey}/{id}}
     */
    public String entityBase(String entityKey, Long id) {
        return entityBase(null, entityKey) + "/" + id;
    }

    /**
     * Returns the base URL path for a specific organization-scoped entity instance.
     *
     * @param organizationId the tenant organization ID
     * @param entityKey the entity type identifier
     * @param id the entity instance ID
     * @return the base URL path for the entity instance
     */
    public String entityBase(Long organizationId, String entityKey, Long id) {
        return entityBase(organizationId, entityKey) + "/" + id;
    }

    /**
     * Returns the URL path for an entity operation (e.g., settings, view, remove).
     * <p>
     * Appends the operation suffix to the entity base URL. For new entity operations
     * (when {@code entityId} is null), includes the {@code /new} path segment.
     * </p>
     *
     * @param organizationId the tenant organization ID, or null for global
     * @param entityKey the entity type identifier
     * @param entityId the entity instance ID, or null for new entity operations
     * @param operation the operation suffix (e.g., "/settings", "/view")
     * @return the operation URL path
     */
    public String operation(Long organizationId, String entityKey, Long entityId, String operation) {
        String base = entityBase(organizationId, entityKey);
        return entityId == null ? base + _NEW + operation : base + "/" + entityId + operation;
    }

    /**
     * Returns the URL path for an entity operation without a specific entity ID.
     *
     * @param organizationId the tenant organization ID, or null for global
     * @param entityKey the entity type identifier
     * @param operation the operation suffix
     * @return the operation URL path for new entity operations
     */
    public String operation(Long organizationId, String entityKey, String operation) {
        return operation(organizationId, entityKey, null, operation);
    }

    /**
     * Returns the URL path for a global entity operation.
     *
     * @param entityKey the entity type identifier
     * @param entityId the entity instance ID
     * @param operation the operation suffix
     * @return the operation URL path
     */
    public String operation(String entityKey, Long entityId, String operation) {
        return operation(null, entityKey, entityId, operation);
    }

    /**
     * Returns the URL path for a global entity operation without an entity ID.
     *
     * @param entityKey the entity type identifier
     * @param operation the operation suffix
     * @return the operation URL path
     */
    public String operation(String entityKey, String operation) {
        return entityBase(entityKey) + operation;
    }

    /**
     * Returns the URL path for an entity's settings form.
     *
     * @param organizationId the tenant organization ID, or null for global
     * @param entityKey the entity type identifier
     * @param entityId the entity instance ID
     * @return the settings form URL path
     */
    public String form(Long organizationId, String entityKey, Long entityId) {
        return operation(organizationId, entityKey, entityId, _SETTINGS);
    }

    /**
     * Returns the URL path for exporting entity data as CSV report.
     *
     * @param organizationId the tenant organization ID, or null for global
     * @param entityKey the entity type identifier
     * @return the CSV report export URL path
     */
    public String reportCsv(Long organizationId, String entityKey) {
        String base = entityBase(organizationId, entityKey);
        return base + _REPORT + _CSV;
    }

    /**
     * Returns the URL path for viewing an entity instance.
     *
     * @param organizationId the tenant organization ID, or null for global
     * @param entityKey the entity type identifier
     * @param entityId the entity instance ID
     * @return the view URL path
     */
    public String view(Long organizationId, String entityKey, Long entityId) {
        return operation(organizationId, entityKey, entityId, _VIEW);
    }

    /**
     * Returns the URL path for removing an entity instance.
     *
     * @param organizationId the tenant organization ID, or null for global
     * @param entityKey the entity type identifier
     * @param entityId the entity instance ID
     * @return the remove operation URL path
     */
    public String remove(Long organizationId, String entityKey, Long entityId) {
        return operation(organizationId, entityKey, entityId, _REMOVE);
    }

    /**
     * Returns the settings form URL path for an organization-scoped entity.
     *
     * @param organization the tenant organization, or null for global
     * @param entityKey the entity type identifier
     * @param entity the entity instance, or null for new entity
     * @return the settings form URL path
     */
    public String form(Organization organization, String entityKey, LongIdEntity entity) {
        return form(organization == null ? null : organization.getId(), entityKey, entity == null ? null : entity.getId());
    }

    /**
     * Returns the settings form URL path for a new global entity.
     *
     * @param entityKey the entity type identifier
     * @return the new entity form URL path
     */
    public String form(String entityKey) {
        return form((Long)null, entityKey, (Long)null);
    }

    /**
     * Returns the settings form URL path for a global entity instance.
     *
     * @param entityKey the entity type identifier
     * @param entityId the entity instance ID
     * @return the settings form URL path
     */
    public String form(String entityKey, Long entityId) {
        return form(null, entityKey, entityId);
    }

    /**
     * Returns the settings form URL path for a global entity.
     *
     * @param entityKey the entity type identifier
     * @param entity the entity instance
     * @return the settings form URL path
     */
    public String form(String entityKey, LongIdEntity entity) {
        return form(null, entityKey, entity);
    }

    /**
     * Returns the settings form URL path for a new organization-scoped entity.
     *
     * @param organizationId the tenant organization ID
     * @param entityKey the entity type identifier
     * @return the new entity form URL path
     */
    public String form(Long organizationId, String entityKey) {
        return form(organizationId, entityKey, null);
    }

    /**
     * Returns the settings form URL path for a new entity within an organization.
     *
     * @param organization the tenant organization
     * @param entityKey the entity type identifier
     * @return the new entity form URL path
     */
    public String form(Organization organization, String entityKey) {
        return form(organization, entityKey, null);
    }

    /**
     * Returns the URL path for listing all entities of a given type.
     *
     * @param organizationId the tenant organization ID, or null for global
     * @param entityKey the entity type identifier
     * @return the list all entities URL path
     */
    public String all(Long organizationId, String entityKey) {
        return entityBase(organizationId, entityKey) + _ALL;
    }

    /**
     * Returns the URL path for listing all entities within an organization.
     *
     * @param organization the tenant organization, or null for global
     * @param entityKey the entity type identifier
     * @return the list all entities URL path
     */
    public String all(Organization organization, String entityKey) {
        return all(organization == null ? null : organization.getId(), entityKey);
    }

    /**
     * Returns the URL path for listing all global entities.
     *
     * @param entityKey the entity type identifier
     * @return the list all entities URL path
     */
    public String all(String entityKey) {
        return all((Long)null, entityKey);
    }

    //    ORGANIZATION URLS
    /**
     * Returns the base URL path for an organization.
     *
     * @param id the organization ID
     * @return the organization base URL path {@code /html/organization/{id}}
     */
    public String organizationBase(long id) {
        return _HTML + _ORGANIZATION + "/" + id;
    }

    /**
     * Returns the base HTML path.
     *
     * @return the base path {@code /html}
     */
    public String base() {
        return _HTML;
    }

    /**
     * Returns the URL path for listing all organizations.
     *
     * @return the all organizations URL path
     */
    public String allOrganizations() {
        return all(ORGANIZATION);
    }
    
    /**
     * Returns the URL path for creating a new organization.
     *
     * @return the new organization form URL path
     */
    public String newOrganization() {
        return form(ORGANIZATION);
    }
    
    /**
     * Returns the URL path for removing an organization.
     *
     * @param organizationId the organization ID to remove
     * @return the remove organization URL path
     */
    public String removeOrganization(Long organizationId) {
        return organizationBase(organizationId) + _ENTITY + _REMOVE;
    }
    
    /**
     * Returns the URL path for an organization's settings page.
     *
     * @param id the organization ID
     * @return the organization settings URL path
     */
    public String organizationSettings(long id) { return form(id, ORGANIZATION, id); }
    
    /**
     * Returns the URL path for an organization's dashboard.
     *
     * @param id the organization ID
     * @return the organization dashboard URL path
     */
    public String organizationDashboard(long id) { return operation(id, ORGANIZATION, id, _DASHBOARD); }
    
    /**
     * Returns the URL path for modifying a member's role within an organization.
     *
     * @param id the organization ID
     * @param userId the user ID whose role is being modified
     * @return the modify member role URL path with userId query parameter
     */
    public String organizationModifyMemberRole(long id, long userId) {
        return operation(id, ORGANIZATION, id, _MEMBER) + "?userId=" + userId;
    }
    
    /**
     * Returns the logo image URL for an organization or global logo.
     * Uses the configured logo template from {@link #logoImageHref} for organization-specific logos,
     * or {@link #logoImageHrefGlobal} when organization ID is null.
     *
     * @param organizationId the organization ID, or null for global logo
     * @return the logo image URL
     */
    public String logoImageUrl(Long organizationId) { return organizationId == null ? logoImageHrefGlobal : String.format(logoImageHref, organizationId);}

    /**
     * Returns the URL path for resetting a user's API key.
     *
     * @param id the user ID
     * @return the reset API key URL path
     */
    public String resetApiKey(long id) {
        return operation(USER, id, _SETTINGS + _APIKEY);
    }

//    USER URLS

    /**
     * Returns the base URL path for a user.
     *
     * @param id the user ID
     * @return the user base URL path
     */
    public String userBase(long id) {
        return entityBase(USER, id);
    }

    /**
     * Returns the URL path for listing all users.
     *
     * @return the all users URL path
     */
    public String users() {
        return all(USER);
    }

    /**
     * Returns the URL path for a user's profile page.
     *
     * @param id the user ID
     * @return the user profile URL path
     */
    public String userProfile(long id) {
        return operation(USER, id, _PROFILE);
    }

    /**
     * Returns the URL path for accessing file content.
     *
     * @param orgId the organization ID
     * @param fileId the file ID
     * @return the file content URL path
     */
    public String fileContent(long orgId, long fileId) {
        return operation(orgId, FILE, fileId, _CONTENT);
    }

    /**
     * Returns the URL path for a user's settings page.
     *
     * @param id the user ID
     * @return the user settings URL path
     */
    public String userSettings(long id) {
        return form(USER, id);
    }

    /**
     * Returns the URL path for the password reset page.
     *
     * @return the password recovery URL path
     */
    public String resetPassword() {
        return _PASSWORD + _RECOVERY;
    }

    /**
     * Returns the URL path for spoofing (impersonating) a user.
     *
     * @param id the user ID to spoof
     * @return the spoof user URL path
     */
    public String spoof(long id) {
        return operation(USER, id, _SPOOF);
    }

    /**
     * Returns the URL path for exiting spoof mode.
     *
     * @return the exit spoof URL path
     */
    public String exitSpoof() {
        return operation(USER, _SPOOF + _EXIT);
    }

//    ROLE URLS

    /**
     * Returns the base URL path for a role.
     *
     * @param id the role ID
     * @return the role base URL path
     */
    public String roleBase(long id) {
        return entityBase(ROLE, id);
    }

    /**
     * Returns the URL path for listing all roles.
     *
     * @return the all roles URL path
     */
    public String allRoles() {
        return all(ROLE);
    }

    /**
     * Returns the URL path for listing all privileges.
     *
     * @return the all privileges URL path
     */
    public String allPrivileges() {
        return all(PRIVILEGE);
    }
    
    /**
     * Returns the URL path for creating a new role.
     *
     * @return the new role form URL path
     */
    public String newRole() {
        return form(ROLE);
    }

    /**
     * Returns the URL path for creating a new privilege.
     *
     * @return the new privilege form URL path
     */
    public String newPrivilege() {
        return form(PRIVILEGE);
    }
    
    /**
     * Returns the URL path for a role's settings page.
     *
     * @param id the role ID
     * @return the role settings URL path
     */
    public String roleSettings(long id) {
        return operation(ROLE, id, _SETTINGS);
    }

    /**
     * Returns the URL path for a privilege's settings page.
     *
     * @param id the privilege ID
     * @return the privilege settings URL path
     */
    public String privilegeSettings(long id) {
        return operation(PRIVILEGE, id, _SETTINGS);
    }
    
    /**
     * Returns the URL path for managing a role's privileges.
     *
     * @param id the role ID
     * @return the role privileges management URL path
     */
    public String rolePrivileges(long id) {
        return operation(ROLE, id, _PRIVILEGES);
    }

//    HISTORY URLS

    /**
     * Returns the URL path for listing all audit entries.
     *
     * @return the all audit entries URL path
     */
    public String allAudit() {
        return all(AUDIT);
    }
    
    /**
     * Returns the URL path for listing audit entries with search filter.
     *
     * @param search the search term (URL-encoded)
     * @return the filtered audit entries URL path with search query parameter
     */
    public String allAudit(String search) {
        return all(AUDIT) + "?audit_search=" + encode(search);
    }

    /**
     * Returns the URL path for an organization's history page.
     *
     * @param id the organization ID
     * @return the organization history URL path
     */
    public String organizationHistory(long id) {
        return operation(id, ORGANIZATION, id, _HISTORY);
    }

    /**
     * Returns the URL path for downloading audit entry content.
     *
     * @param id the audit entry ID
     * @return the download audit content URL path
     */
    public String downloadAuditContent(long id) {
        return operation(AUDIT, id, _CONTENT);
    }

//    MODULE URLS

    /**
     * Returns the URL path for listing all modules.
     *
     * @return the all modules URL path
     */
    public String allModules() {
        return all(MODULE);
    }

    /**
     * Returns the URL path for listing all modules accessible to a user within an organization.
     *
     * @param orgId the organization ID
     * @param userId the user ID
     * @return the user organization modules URL path
     */
    public String allUserOrgModules(long orgId, long userId) {
        return operation(orgId, USER, userId, _MODULE + _ALL);
    }

    /**
     * Returns the URL path for module settings based on context.
     * Routes to the appropriate settings URL depending on whether user ID and/or organization ID are provided.
     *
     * @param moduleName the module name
     * @param userId the user ID, or null for non-user-specific settings
     * @param orgId the organization ID, or null for global settings
     * @return the module settings URL path for the appropriate context
     */
    public String moduleSettings(String moduleName, Long userId, Long orgId) {
        if (userId != null && orgId != null) {
            return userOrganizationModuleSettings(moduleName, userId, orgId);
        } else if (userId != null) {
            return userGlobalModuleSettings(moduleName, userId);
        } else if (orgId != null) {
            return organizationModuleSettings(moduleName, orgId);
        } else {
            return globalModuleSettings(moduleName);
        }
    }

    /**
     * Returns the URL path for global module settings.
     *
     * @param moduleName the module name
     * @return the global module settings URL path
     */
    public String globalModuleSettings(String moduleName) {
        return operation(MODULE, "/" + moduleName + _SETTINGS);
    }

    /**
     * Returns the URL path for user-specific global module settings.
     *
     * @param moduleName the module name
     * @param userId the user ID
     * @return the user global module settings URL path
     */
    public String userGlobalModuleSettings(String moduleName, long userId) {
        return _HTML + _MODULE + "/" + moduleName + _USER + "/" + userId + _SETTINGS;
    }

    /**
     * Returns the base URL path for an organization's module.
     *
     * @param moduleName the module name
     * @param orgId the organization ID
     * @return the organization module base URL path
     */
    public String organizationModuleBase(String moduleName, long orgId) {
        return entityBase(orgId, MODULE) + "/" + moduleName;
    }

    /**
     * Returns the URL path for organization module settings.
     *
     * @param moduleName the module name
     * @param orgId the organization ID
     * @return the organization module settings URL path
     */
    public String organizationModuleSettings(String moduleName, long orgId) {
        return organizationModuleBase(moduleName, orgId) + _SETTINGS;
    }

    /**
     * Returns the URL path for user-specific organization module settings.
     *
     * @param moduleName the module name
     * @param userId the user ID
     * @param orgId the organization ID
     * @return the user organization module settings URL path
     */
    public String userOrganizationModuleSettings(String moduleName, long userId, long orgId) {
        return organizationModuleBase(moduleName, orgId) + _USER + "/" + userId + _SETTINGS;
    }

    /**
     * Returns the URL path for accessing an organization's module.
     *
     * @param organizationId the organization ID
     * @param moduleName the module name
     * @return the organization module URL path with "module-" prefix
     */
    public String module(long organizationId, String moduleName) {
        return organizationBase(organizationId) + "/" + "module-" + moduleName;
    }

    /**
     * Returns the URL path for accessing an organization's module (alias).
     *
     * @param id the organization ID
     * @param module the module name
     * @return the organization module URL path
     */
    public String organizationModule(long id, String module) {
        return module(id, module);
    }

//    FRONTEND RESOURCE URLS

    /**
     * Returns the URL path for accessing a frontend resource entry by name.
     *
     * @param name the frontend resource name
     * @return the frontend resource entry URL path
     */
    public String frontendResourceEntry(String name) {
        return operation(FRONTENDRESOURCE, "/" + name);
    }

    /**
     * Returns the URL path for listing all frontend resources.
     *
     * @return the all frontend resources URL path
     */
    public String allFrontendResource() {
        return all(FRONTENDRESOURCE);
    }
    
    /**
     * Returns the URL path for listing all files.
     *
     * @return the all files URL path
     */
    public String allFiles() {
        return all(FILE);
    }

    /**
     * Returns the URL path for deleting a frontend resource.
     *
     * @param frontendResourceId the frontend resource ID
     * @return the delete frontend resource URL path
     */
    public String deleteFrontendResource(long frontendResourceId) {
        return operation(FRONTENDRESOURCE,  frontendResourceId, _REMOVE);
    }

    /**
     * Returns the URL path for deleting a frontend resource draft.
     *
     * @param frontendResourceId the frontend resource ID
     * @return the delete draft URL path
     */
    public String deleteFrontendResourceDraft(long frontendResourceId) {
        return operation(FRONTENDRESOURCE,  frontendResourceId, _REMOVE + _DRAFT);
    }
    
    /**
     * Returns the URL path for frontend resource settings.
     *
     * @param frontendResourceId the frontend resource ID
     * @return the frontend resource settings URL path
     */
    public String frontendResourceSettings(long frontendResourceId) {
        return form(FRONTENDRESOURCE, frontendResourceId);
    }
    
    /**
     * Returns the URL path for copying live version to draft.
     *
     * @param frontendResourceId the frontend resource ID
     * @return the copy live to draft URL path
     */
    public String frontendResourceCopyLiveToDraft(long frontendResourceId) {
        return operation(FRONTENDRESOURCE,  frontendResourceId, _COPY + _LIVE);
    }
    
    /**
     * Returns the URL path for copying resource to draft.
     *
     * @param frontendResourceId the frontend resource ID
     * @return the copy resource to draft URL path
     */
    public String frontendResourceCopyResourceToDraft(long frontendResourceId) {
        return operation(FRONTENDRESOURCE,  frontendResourceId, _COPY + _RESOURCE);
    }

    /**
     * Returns the URL path for publishing a frontend resource.
     *
     * @param frontendResourceId the frontend resource ID
     * @return the publish frontend resource URL path
     */
    public String publishFrontendResource(long frontendResourceId) {
        return operation(FRONTENDRESOURCE,  frontendResourceId, _PUBLISH);
    }

    /**
     * Returns the URL path for clearing a frontend resource cache.
     *
     * @param frontendResourceId the frontend resource ID
     * @return the clear frontend resource URL path
     */
    public String clearFrontendResource(long frontendResourceId) {return operation(FRONTENDRESOURCE,  frontendResourceId, _CLEAR); }
    
    /**
     * Returns the URL path for reloading a frontend resource.
     *
     * @param frontendResourceId the frontend resource ID
     * @return the reload frontend resource URL path
     */
    public String reloadFrontendResource(long frontendResourceId) {return operation(FRONTENDRESOURCE,  frontendResourceId, _RELOAD); }
    
    /**
     * Returns the URL path for publishing all frontend resources.
     *
     * @return the publish all frontend resources URL path
     */
    public String publishAllFrontendResource() {return all(FRONTENDRESOURCE) + _PUBLISH; }
    
    /**
     * Returns the URL path for clearing all frontend resources cache.
     *
     * @return the clear all frontend resources URL path
     */
    public String clearAllFrontendResource() {return all(FRONTENDRESOURCE) + _CLEAR; }
    
    /**
     * Returns the URL path for reloading frontend resource to draft.
     *
     * @param frontendResourceId the frontend resource ID
     * @return the reload to draft URL path
     */
    public String reloadFrontendResourceToDraft(long frontendResourceId) {return _HTML + _FRONTENDRESOURCE + "/" + frontendResourceId + _RELOAD_TO_DRAFT; }

    /**
     * Returns the URL path for creating a new frontend resource entry.
     *
     * @return the new frontend resource form URL path
     */
    public String newFrontendResourceEntry() {
        return form(FRONTENDRESOURCE);
    }

    /**
     * Returns the URL path for exporting frontend resources as ZIP.
     *
     * @return the ZIP export URL path
     */
    public String zipFrontendResource() {
        return operation(FRONTENDRESOURCE, _ZIP);
    }
//    FRONTEND ELEMENTS

    /**
     * Returns the URL path for listing all UI components.
     *
     * @return the all UI components URL path
     */
    public String allUIComponents() {
        return all(WEBENDPOINT);
    }

    /**
     * Returns the URL path for creating a new UI component.
     *
     * @return the new UI component form URL path
     */
    public String newUIComponent() {
        return form(WEBENDPOINT);
    }

    /**
     * Returns the URL path for UI component settings.
     *
     * @param UIComponentId the UI component ID
     * @return the UI component settings URL path
     */
    public String UIComponentSettings(long UIComponentId) {
        return form(WEBENDPOINT, UIComponentId);
    }

//    FORM URLS
    /**
     * Returns the URL path for listing all forms.
     *
     * @return the all forms URL path
     */
    public String allForm() {
        return all(FORM);
    }


    //    SERVER JS URLS
    /**
     * Returns the URL path for listing all server-side JavaScript resources.
     *
     * @return the all server JS URL path
     */
    public String allServerJs() {
        return all(SERVERJS);
    }
    
    /**
     * Returns the URL path for listing all page builder resources.
     *
     * @return the all page builder URL path
     */
    public String allPageBuilder() {
        return all(PAGEBUILDER);
    }

    /**
     * Returns the URL path for listing all active threads.
     *
     * @return the all threads URL path
     */
    public String allThreads() {
        return entityBase(THREAD);
    }

    /**
     * Returns the URL path for interrupting a thread.
     *
     * @param id the thread ID
     * @return the interrupt thread URL path
     */
    public String interruptThread(long id) {
        return operation(THREAD, id, _INTERRUPT);
    }

    /**
     * Returns the URL path for removing a thread.
     *
     * @param id the thread ID
     * @return the remove thread URL path
     */
    public String removeThread(long id) {
        return operation(THREAD, id, _REMOVE);
    }

//    EVENT LISTENER URLS
    /**
     * Returns the URL path for listing all event listeners.
     *
     * @return the all event listeners URL path
     */
    public String allEventListeners(){
        return all(EVENTLISTENER);
    }

    /**
     * Returns the URL path for listing all custom events.
     *
     * @return the all custom events URL path
     */
    public String allCustomEvents(){
        return all(CUSTOM_EVENT);
    }
    
    /**
     * Returns the URL path for creating a new event listener.
     *
     * @return the new event listener form URL path
     */
    public String newEventListener(){
        return form(EVENTLISTENER);
    }

    /**
     * Returns the URL path for event listener settings.
     *
     * @param eventListenerId the event listener ID
     * @return the event listener settings URL path
     */
    public String eventListenerSettings(long eventListenerId){
        return form(EVENTLISTENER, eventListenerId);
    }

    /**
     * Returns the URL path for sending an event.
     *
     * @return the send event URL path
     */
    public String sendEvent(){
        return operation(EVENTLISTENER, _SEND);
    }
    
    /**
     * Returns the URL path for creating a new custom event.
     *
     * @return the new custom event form URL path
     */
    public String newCustomEvent() {
        return form(CUSTOM_EVENT);
    }

//    Admin Dashboard

    /**
     * Returns the URL path for the admin dashboard.
     *
     * @return the admin dashboard URL path
     */
    public String adminDashboard() {
        return _HTML + _DASHBOARD;
    }

//    COMPONENTS

    /**
     * Returns the URL path for the components page.
     *
     * @return the components URL path
     */
    public String components() {
        return _HTML + _COMPONENTS;
    }

//    SCHEDULER URLS

    /**
     * Returns the URL path for listing all schedulers.
     *
     * @return the all schedulers URL path
     */
    public String allSchedulers(){
        return all(SCHEDULER);
    }
    
    /**
     * Returns the URL path for creating a new scheduler.
     *
     * @return the new scheduler form URL path
     */
    public String newScheduler(){
        return form(SCHEDULER);
    }

    /**
     * Returns the URL path for scheduler settings.
     *
     * @param schedulerId the scheduler ID
     * @return the scheduler settings URL path
     */
    public String schedulerSettings(long schedulerId){
        return form(SCHEDULER, schedulerId);
    }

//    LOGS URLS

    /**
     * Returns the URL path for listing all logs.
     *
     * @return the all logs URL path
     */
    public String allLogs() {
        return all(LOGS);
    }

    /**
     * Returns the URL path for downloading logs.
     *
     * @return the download logs URL path
     */
    public String downloadLogs() {
        return operation(LOGS, _DOWNLOAD);
    }

    /**
     * Returns the URL path for logs settings.
     *
     * @return the logs settings URL path
     */
    public String logsSettings(){
        return operation(LOGS, _SETTINGS);
    }


//   NOTIFICATIONS

    /**
     * Returns the URL path for listing all notifications for a user.
     *
     * @param userId the user ID
     * @param organizationId the organization ID, or null for global
     * @return the all notifications URL path
     */
    public String notificationsAll(long userId, Long organizationId){
        return operation(organizationId, NOTIFICATION, userId, _ALL);
    }

    /**
     * Returns the URL path for marking specific notifications as read.
     *
     * @param userId the user ID
     * @param organizationId the organization ID, or null for global
     * @param unreadNotificationsListString comma-separated list of notification IDs
     * @return the mark notifications as read URL path with query parameter
     */
    public String markNotificationsAsRead(long userId, Long organizationId, String unreadNotificationsListString) {
        return operation(organizationId, NOTIFICATION, userId, _MARK_READ + "?unreadNotifications=" + unreadNotificationsListString);
    }

    /**
     * Returns the URL path for marking all notifications as read for a user.
     *
     * @param userId the user ID
     * @param organizationId the organization ID, or null for global
     * @return the mark all notifications as read URL path
     */
    public String markAllNotificationsAsRead(long userId, Long organizationId) {
        return operation(organizationId, NOTIFICATION, userId, _ALL + _MARK_READ);
    }

//    SYSTEM HEALTH
    /**
     * Returns the URL path for the system health page.
     *
     * @return the system health URL path
     */
    public String systemHealth() {
        return entityBase(SYSTEM_HEATH);
    }

    /**
     * Returns the URL path for system database validation.
     *
     * @return the database validation URL path
     */
    public String systemDatabaseValidation() {
        return entityBase(SYSTEM_HEATH) + _VALIDATE;
    }

//   AFFILIATION

    /**
     * Returns the URL path for listing all affiliation codes for an organization.
     *
     * @param orgId the organization ID
     * @return the all affiliation codes URL path
     */
    public String affiliationCodeAll(long orgId) {
        return all(orgId, AFFILIATION_CODE);
    }

    /**
     * Returns the URL path for listing affiliation events with search filter.
     *
     * @param orgId the organization ID
     * @param searchParam the search parameter
     * @return the affiliation events URL path with search query parameter
     */
    public String affiliationEventAll(long orgId, String searchParam) {
        return all(orgId, AFFILIATION_EVENT) + "/?obj_search=" + searchParam;
    }

    /**
     * Returns the absolute affiliation link URL.
     * Constructs a complete URL using {@link #baseUrl} and the affiliation code.
     *
     * @param affiliationCode the affiliation code
     * @return the complete affiliation link URL with aff_code query parameter
     */
    public String getAffiliationLink(String affiliationCode) {
        return baseUrl + "?aff_code=" + affiliationCode;
    }

//  YAML EXPORT

    /**
     * Returns the URL path for exporting all YAML resources globally.
     *
     * @return the export all YAML resources URL path
     */
    public String exportAllYamlResources(){
        return operation(ORGANIZATION, _EXPORT_YAML + _ALL);
    }

    /**
     * Returns the URL path for importing components from ZIP.
     *
     * @return the import components ZIP URL path
     */
    public String importComponentsZip(){
        return operation(COMPONENT, _IMPORT + _ZIP);
    }

    /**
     * Returns the URL path for exporting all YAML resources for a specific organization.
     *
     * @param id the organization ID
     * @return the export YAML resources URL path for the organization
     */
    public String exportAllYamlResourcesForOrg(long id) {
        return operation(ORGANIZATION, id, _EXPORT_YAML + _ALL);
    }
    
    /**
     * Returns the URL path for exporting all frontend resources as YAML.
     *
     * @return the export frontend resources YAML URL path
     */
    public String yamlAllFrontendResources(){
        return operation(FRONTENDRESOURCE, _EXPORT_YAML);
    }

    /**
     * Returns the URL path for exporting all UI components as YAML.
     *
     * @return the export UI components YAML URL path
     */
    public String yamlAllUiComponents(){
        return operation(UI_COMPONENT,  _EXPORT_YAML);
    }

    /**
     * Returns the URL path for exporting all form resources as YAML.
     *
     * @return the export form resources YAML URL path
     */
    public String yamlAllFormResources(){
        return operation(FORM, _EXPORT_YAML);
    }
    
    /**
     * Returns the URL path for exporting all server JS resources as YAML.
     *
     * @return the export server JS resources YAML URL path
     */
    public String yamlAllServerJsResources(){
        return operation(SERVERJS, _EXPORT_YAML);
    }

    /**
     * Returns the URL path for exporting all event listener resources as YAML.
     *
     * @return the export event listeners YAML URL path
     */
    public String yamlAllEventResources(){
        return operation(EVENTLISTENER, _EXPORT_YAML);
    }
    
    /**
     * Returns the URL path for exporting all custom event resources as YAML.
     *
     * @return the export custom events YAML URL path
     */
    public String yamlAllCustomEventResources(){
        return operation(CUSTOM_EVENT, _EXPORT_YAML);
    }

    /**
     * Returns the URL path for exporting all scheduler resources as YAML.
     *
     * @return the export schedulers YAML URL path
     */
    public String yamlAllSchedulerResources(){
        return operation(SCHEDULER, _EXPORT_YAML);
    }

    /**
     * Returns the URL path for exporting a specific frontend resource as YAML.
     *
     * @param id the frontend resource ID
     * @return the export frontend resource YAML URL path
     */
    public String yamlFrontendResource(long id){
        return operation(FRONTENDRESOURCE, id, _EXPORT_YAML);
    }
    
    /**
     * Returns the URL path for exporting a specific server JS resource as YAML.
     *
     * @param id the server JS ID
     * @return the export server JS YAML URL path
     */
    public String yamlServerJs(long id){
        return operation(SERVERJS, id, _EXPORT_YAML);
    }
    
    /**
     * Returns the URL path for exporting a specific form as YAML.
     *
     * @param id the form ID
     * @return the export form YAML URL path
     */
    public String yamlForm(long id){
        return operation(FORM, id, _EXPORT_YAML);
    }
    
    /**
     * Returns the URL path for exporting a specific event listener as YAML.
     *
     * @param id the event listener ID
     * @return the export event listener YAML URL path
     */
    public String yamlEventListener(long id){
        return operation(EVENTLISTENER, id, _EXPORT_YAML);
    }
    
    /**
     * Returns the URL path for exporting a specific scheduler as YAML.
     *
     * @param id the scheduler ID
     * @return the export scheduler YAML URL path
     */
    public String yamlScheduler(long id){
        return operation(SCHEDULER, id, _EXPORT_YAML);
    }

    /**
     * Returns the URL path for the integrations page.
     *
     * @return the integrations URL path
     */
    public String integrations() {
        return entityBase(INTEGRATIONS);
    }

//    AI

    /**
     * Returns the URL path for AI-powered reporting.
     * Generates organization-scoped or global AI reporting URLs.
     *
     * @param orgId the organization ID, or null for global
     * @return the AI reporting URL path
     */
    public String aiReporting(Long orgId) {
        return (orgId != null ? organizationBase(orgId) : _HTML) + _CN + "/reporting-report";
    }

    /**
     * Returns the URL path for generating a report prompt using AI.
     *
     * @param orgId the organization ID, or null for global
     * @return the report prompt URL path
     */
    public String reportPrompt(Long orgId) {
        return entityBase(orgId, QUERY_REPORT) + _PROMPT;
    }

    /**
     * Returns the URL path for executing a report query.
     *
     * @param orgId the organization ID, or null for global
     * @return the report query URL path
     */
    public String reportQuery(Long orgId) {
        return entityBase(orgId, QUERY_REPORT) + _QUERY;
    }

    /**
     * Returns the URL path for exporting report query results as CSV.
     *
     * @param orgId the organization ID, or null for global
     * @return the report query CSV export URL path
     */
    public String reportQueryCsv(Long orgId) {
        return reportQuery(orgId) + _CSV;
    }

    /**
     * Returns the URL path for listing all query reports.
     *
     * @param orgId the organization ID, or null for global
     * @return the all query reports URL path
     */
    public String allQueryReports(Long orgId){
        return all(orgId, QUERY_REPORT);
    }

    /**
     * Returns the URL path for a specific query report or new report form.
     *
     * @param orgId the organization ID, or null for global
     * @param reportId the report ID, or null for new report
     * @return the query report URL path
     */
    public String queryReport(Long orgId, Long reportId){
        return entityBase(orgId, QUERY_REPORT) + "/" + (reportId != null ? reportId : "");
    }

//    OTHER

    /**
     * Joins multiple strings with comma separator, filtering out null values.
     * Removes null string literals and empty comma separators from the result.
     *
     * @param strings the strings to join
     * @return the joined string with nulls filtered out
     */
    public String joinStrings(String ... strings){
        return String.join(", ", strings).replaceAll("(, |)null", "");
    }

    /**
     * Returns the signature of an event listener for display purposes.
     * Formats as {@code ClassName::methodName(EventType)}.
     *
     * @param eventListenerEntry the event listener entry
     * @return the formatted event listener signature
     */
    public String getEventListenerSignature(EventListenerEntry eventListenerEntry){

        return eventListenerEntry.getConsumerClassName(true) + "::" + eventListenerEntry.getConsumerMethodName() + "(" + eventListenerEntry.getEventObjectType(true) + ")";
    }

    /**
     * Checks if a page property is sorted in ascending order.
     *
     * @param page the Spring Data page
     * @param property the property name
     * @return true if sorted ascending, false otherwise
     */
    public boolean isAsc(PageImpl page, String property) {
        Sort.Direction direction = getDirection(page, property);
        return Sort.Direction.ASC.equals(direction);
    }

    /**
     * Returns the opposite sort direction.
     *
     * @param direction the current direction
     * @return "ASC" if direction is DESC, "DESC" otherwise
     */
    public String otherDirection(Sort.Direction direction) {
        return (Sort.Direction.DESC.equals(direction)) ? Sort.Direction.ASC.name() : Sort.Direction.DESC.name();
    }

    /**
     * Returns the sort parameter string for pagination links.
     * Toggles between ASC and DESC for the given property, or defaults to "id,ASC".
     *
     * @param page the current page
     * @param property the property to sort by, or null for default
     * @return the sort parameter string in format "property,DIRECTION"
     */
    public String pageSort(PageImpl page, String property) {
        if (property == null) {
            return "id,ASC";
        }

        boolean orderIsNull = isOrder(page, property);

        if (isOrder(page, property)) {
            return property + "," + otherDirection(page.getSort().getOrderFor(property).getDirection());
        }

        return property + ",ASC";
    }

    /**
     * Checks if a property has an active sort order on the page.
     *
     * @param page the Spring Data page
     * @param property the property name
     * @return true if the property is being sorted
     */
    public boolean isOrder(PageImpl page, String property) {
        return getOrder(page, property) != null;
    }

    /**
     * Returns the sort direction for a property on the page.
     *
     * @param page the Spring Data page
     * @param property the property name
     * @return the sort direction, or null if not sorted
     */
    public Sort.Direction getDirection(PageImpl page, String property) {
        Sort.Order order = getOrder(page, property);
        return order == null ? null : order.getDirection();
    }

    /**
     * Returns the sort order for a property on the page.
     *
     * @param page the Spring Data page
     * @param property the property name
     * @return the sort order, or null if not sorted
     */
    public Sort.Order getOrder(PageImpl page, String property) {
        boolean pageIsNull = ( page == null );
        boolean sortIsNull = pageIsNull || ( page.getSort() == null );
        Sort.Order order = sortIsNull ? null : ( page.getSort().getOrderFor(property) );
        return order;
    }

    /**
     * Returns URL query parameters for pagination.
     * Creates parameters for page number, size, sort, and search with a qualifier prefix.
     *
     * @param property the sort property
     * @param qualifier the parameter name prefix (e.g., "obj" produces "obj_page", "obj_size")
     * @param page the current page
     * @param search the search term, or null
     * @return the query string starting with "?", or empty string if page is null
     */
    public String pageableParams(String property, String qualifier, PageImpl page, String search) {
        if (page == null) {
            return "";
        }
        return String.format("?%s_page=%d&%s_size=%d&%s_sort=%s&%s_search=%s", qualifier, page.getNumber(), qualifier, page.getSize(), qualifier, pageSort(page, property), qualifier, (search == null ? "" : search));
    }

    /**
     * Returns combined URL query parameters for pagination, filtering, and additional params.
     *
     * @param property the sort property
     * @param qualifier the parameter name prefix
     * @param page the current page
     * @param search the search term
     * @param filters the filter map (key-value pairs)
     * @param remainingParameters additional query parameters to append
     * @return the complete query string
     */
    public String sortParams(String property, String qualifier, PageImpl page, String search, Map<String, String> filters, String remainingParameters) {
        return pageableParams(property, qualifier, page, search) + filterParams(qualifier, filters) + (remainingParameters == null ? "" : remainingParameters);
    }

    /**
     * Returns URL query parameters for filters.
     * Constructs filter parameters in format "&{qualifier}_filter_{key}={value}".
     *
     * @param qualifier the parameter name prefix
     * @param filters the filter map with key-value pairs
     * @return the filter query parameters string, or empty if no filters
     */
    public String filterParams(String qualifier, Map<String, String> filters) {
        StringBuilder sb = new StringBuilder();
        if(filters != null && !filters.isEmpty()) {
            for(Map.Entry<String, String> entry : filters.entrySet()) {
                if(StringUtils.isNotBlank(entry.getValue())) {
                    sb.append(String.format("&%s_filter_%s=%s", qualifier, entry.getKey(), entry.getValue()));
                }
            }
        }
        String result = sb.toString();
        return result;
    }

    /**
     * Creates a Thymeleaf expression for pagination links.
     * Generates Thymeleaf syntax for constructing paginated URLs.
     *
     * @param qualifer the parameter name prefix (typo preserved for compatibility)
     * @param pageParam the page parameter expression
     * @return the Thymeleaf expression string
     */
    public String createThymeleafExpressionForPagination(String qualifer ,String pageParam){
        return "@{${currentUri}(${qualifier} + '_page' =" + pageParam +" , ${qualifier} + '_size' = ${page.size}, ${qualifier} + '_sort' =" + "${param. " + qualifer + "_sort}, ${qualifier} + '_search' =" + "${param." + qualifer + "_search})}";
    }

    /**
     * Extracts the search parameter from an HTTP request with parameter prefix.
     *
     * @param request the HTTP servlet request
     * @param paramPrefix the parameter name prefix
     * @return the search parameter value, or empty string if not present
     */
    public static String getSearchForParamPrefix(HttpServletRequest request, String paramPrefix) {
        return StringUtils.defaultString(request.getParameter(paramPrefix + "search"));
    }

    /**
     * Creates a Spring Data Pageable from HTTP request parameters with prefix.
     * Extracts page number, size, and sort parameters to construct a Pageable.
     * Defaults: page=0, size=10, sort=id,DESC.
     *
     * @param request the HTTP servlet request
     * @param paramPrefix the parameter name prefix
     * @return the Pageable for Spring Data repositories
     */
    public static Pageable getPageableForParamPrefix(HttpServletRequest request, String paramPrefix) {
        int page = Integer.parseInt(StringUtils.defaultIfBlank(request.getParameter(paramPrefix + "page"), "0"));
        int size = Integer.parseInt(StringUtils.defaultIfBlank(request.getParameter(paramPrefix + "size"), "10"));
        String sortString = StringUtils.defaultIfBlank(request.getParameter(paramPrefix + "sort"), "id,DESC");
        String[] sortValues = sortString.split(",");

        String sortProperty = sortValues[0];
        Sort.Direction sortDirection = Sort.Direction.valueOf(sortValues[1]);

        Pageable workLogPageable = PageRequest.of(page, size, sortDirection, sortProperty);
        return workLogPageable;
    }

    /**
     * Returns the URL path for the features page.
     *
     * @return the features URL path
     */
    public String features() {
        return "/features";
    }

    /**
     * Returns the URL path for accessing a file asset.
     * Constructs file URLs with proper encoding of the filename.
     * Public files omit the {@code /html} prefix for direct access.
     * <p>
     * <b>Note on URL Encoding:</b> Uses platform default charset via {@link Charset#defaultCharset()},
     * which may vary across systems.
     * </p>
     *
     * @param f the file entity
     * @return the file access URL path with encoded filename
     */
    public static String getFileURL(File f) {
        return String.format((f.isPublicFile() ? "" : _HTML) + FILE_ASSET + "%d/%s", f.getId(), encode(f.getFilename()));
    }

    /**
     * Returns the absolute URL for accessing a file asset.
     * Combines {@link #baseUrl} with the file URL path.
     *
     * @param f the file entity
     * @return the complete absolute file URL
     */
    public String getAbsoluteFileURL(File f) {
        return getBaseUrl() + getFileURL(f);
    }

    /**
     * URL-encodes a string using the platform default charset.
     * <p>
     * <b>Platform Dependency:</b> Encoding behavior depends on {@link Charset#defaultCharset()},
     * which varies by system configuration and may affect URL compatibility.
     * </p>
     *
     * @param string the string to encode
     * @return the URL-encoded string
     */
    public static String encode(String string) {
        return URLEncoder.encode(string, Charset.defaultCharset());
    }

    /**
     * Extracts the organization ID from a request URL path, or returns null.
     * Parses organization-scoped URL patterns like {@code /html/organization/{orgId}/...}
     * using the {@link #htmlOrganizationPath} regex pattern.
     * <p>
     * Example usage:
     * <pre>{@code
     * Long orgId = UrlHelper.getOrganizationIdFromUrlOrNull(request);
     * if (orgId != null) { /* tenant-specific logic */ }
     * }</pre>
     * </p>
     *
     * @param request the HTTP servlet request
     * @return the extracted organization ID, or null if not found
     */
    public Long getOrganizationIdFromUrlOrNull(HttpServletRequest request) {
        Matcher m = htmlOrganizationPath.matcher(request.getServletPath());

        if (not(m.matches())) {
            return null;
        }

        return Long.parseLong(m.group(1));
    }

    /**
     * Extracts the mapping key from a request URL path, or returns null.
     * Parses URL patterns to identify the entity or resource key.
     *
     * @param request the HTTP servlet request
     * @return the extracted mapping key, or null if not found
     */
    public String getMappingKeyOrNull(HttpServletRequest request) {
        Matcher m = mappingKeyPath.matcher(request.getServletPath());

        if (not(m.matches())) {
            return null;
        }
        return m.group(2);
    }

    /**
     * Resolves tenant-aware resource information from an HTTP request.
     * <p>
     * Parses the request URL and query parameters to extract organization ID, entity key,
     * and access level. Validates that path-based and parameter-based organization IDs match,
     * throwing {@code RuntimeException} on mismatch for security.
     * </p>
     * <p>
     * <b>Multi-Tenancy:</b> Enables tenant isolation by identifying which organization context
     * the request operates within. Returns {@code nonExistingTenantedResource} if URL doesn't
     * match expected patterns.
     * </p>
     *
     * @param request the HTTP servlet request
     * @return the resolved tenanted resource with organization ID, entity key, and access level
     * @throws RuntimeException if organization ID from path and query parameter don't match
     * @see TenantResolver
     */
    public TenantResolver.TenantedResource getTenantedResource(HttpServletRequest request) {
        Matcher m = organizationIdAndEntityKeyPath.matcher(request.getServletPath());
        String orgIdParam = request.getParameter(ORGANIZATIONID);
        String orgIdString;

        if (not(m.matches())) {
            return TenantResolver.nonExistingTenantedResource;
        } else {
            orgIdString = m.group(2);
        }

        if(StringUtils.isEmpty(orgIdString)) {
            orgIdString = orgIdParam;
        }

        if(!StringUtils.isEmpty(orgIdParam)) {
            if(!orgIdString.equals(orgIdParam)) {
                throw new RuntimeException("Access denied");
            }
        }

        Long orgId = orgIdString == null ? null : Long.parseLong(orgIdString);
        String entityKey = m.group(3);

        if(StringUtils.isNotEmpty(entityKey) && (entityKey.equals(CN) || entityKey.equals(CI))) {
            entityKey = null;
        }

        FrontendResource.AccessLevel accessLevel = null;

        if (!request.getRequestURI().contains(_HTML)) {
            accessLevel = FrontendResource.AccessLevel.PUBLIC;
        } else if (request.getRequestURI().contains(_HTML) && !request.getRequestURI().contains(_ORGANIZATION)) {
            accessLevel = FrontendResource.AccessLevel.GLOBAL;
        } else if (request.getRequestURI().contains(_HTML_ORGANIZATION)) {
            accessLevel = FrontendResource.AccessLevel.ORGANIZATION;
        }

        return new TenantResolver.TenantedResource(orgId, request.getLocalAddr(), entityKey, request.getMethod(), accessLevel);
    }

    /**
     * Generates a random integer within the specified range.
     * Uses {@link #random} (SecureRandom) for cryptographically strong randomness.
     *
     * @param fromInclusive the lower bound (inclusive)
     * @param toExclusive the upper bound (exclusive)
     * @return a random integer in the range [fromInclusive, toExclusive)
     */
    public int randomInt(int fromInclusive, int toExclusive) {
        return fromInclusive + random.nextInt(toExclusive - fromInclusive);
    }

    /**
     * Returns the preview URL for a UI component with draft mode enabled.
     * Constructs tenant-aware preview URLs based on access level (GLOBAL or ORGANIZATION).
     * Includes {@code draft=true} query parameter and organization ID when applicable.
     *
     * @param organizationId the organization ID, or null for global access
     * @param frontendResourceUrl the frontend resource URL path
     * @param accessLevel the access level (GLOBAL or ORGANIZATION)
     * @return the UI component preview URL with draft mode
     */
    public String getUiComponentPreviewUrl(Long organizationId, String frontendResourceUrl, FrontendResource.AccessLevel accessLevel) {
        String accessLevelPath = "";
        String orgIdParam = (organizationId != null ? "&organizationId=" + organizationId : "");

        if (accessLevel.equals(FrontendResource.AccessLevel.GLOBAL)) {
            accessLevelPath = _HTML;
        } else if (accessLevel.equals(FrontendResource.AccessLevel.ORGANIZATION)) {
            accessLevelPath = _HTML_ORGANIZATION + (organizationId != null ? "/" + organizationId : "");
            orgIdParam = "";
        }

        return accessLevelPath + _CN + "/" + frontendResourceUrl + "?draft=true" + orgIdParam;
    }

    /**
     * Returns the absolute settings URL for a UI component.
     * Combines {@link #baseUrl} with tenant-aware settings path.
     *
     * @param organizationId the organization ID, or null for global
     * @param frontendResourceId the frontend resource ID
     * @return the complete UI component settings URL
     */
    public String getUiComponentSettingsUrl(Long organizationId, Long frontendResourceId) {
        return baseUrl +
                (organizationId == null ? _HTML + _WEBENDPOINT : _HTML_ORGANIZATION + "/" + organizationId + _WEBENDPOINT)
                + "/" + frontendResourceId + _SETTINGS;
    }

    /**
     * Returns the preview URL for a frontend resource.
     * Constructs tenant-aware URLs based on access level and resource type.
     * Optionally includes {@code ?draft} or {@code ?resource} query parameters for non-UI components.
     *
     * @param frontendResource the frontend resource entity
     * @param draft true to include draft query parameter
     * @param resource true to include resource query parameter
     * @return the frontend resource preview URL
     */
    public String getFrontendResourcePreviewUrl(FrontendResource frontendResource, boolean draft, boolean resource) {
        String accessLevelPath = "";
        String uiComponentPath = frontendResource.getResourceType().equals(FrontendResource.ResourceType.UI_COMPONENT) ? _CN : "";

        if (frontendResource.getAccessLevel().equals(FrontendResource.AccessLevel.GLOBAL)) {
            accessLevelPath = _HTML;
        } else if (frontendResource.getAccessLevel().equals(FrontendResource.AccessLevel.ORGANIZATION)) {
            accessLevelPath = _HTML_ORGANIZATION + (frontendResource.getOrganizationId() != null ? "/" + frontendResource.getOrganizationId() : "");
        }

        return accessLevelPath + uiComponentPath + "/" + frontendResource.getName()
                + (StringUtils.isEmpty(uiComponentPath) ? (draft ? "?draft" : "") + (resource ? "?resource" : "") : "");
    }

}
