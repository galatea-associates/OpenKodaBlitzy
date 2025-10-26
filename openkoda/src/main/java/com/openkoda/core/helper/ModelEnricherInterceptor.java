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

package com.openkoda.core.helper;

import com.openkoda.controller.common.PageAttributes;
import com.openkoda.controller.notification.NotificationController;
import com.openkoda.core.cache.ModelCache;
import com.openkoda.core.cache.RequestSessionCacheService;
import com.openkoda.core.cache.RequestSessionContextMetadata;
import com.openkoda.core.multitenancy.TenantResolver;
import com.openkoda.core.security.OrganizationUser;
import com.openkoda.core.security.UserProvider;
import com.openkoda.core.service.SessionService;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.MutableUserInOrganization;
import com.openkoda.model.Organization;
import com.openkoda.model.Privilege;
import com.openkoda.model.notification.Notification;
import com.openkoda.repository.SecureEntityDictionaryRepository;
import com.openkoda.repository.organization.OrganizationRepository;
import com.openkoda.service.captcha.CaptchaService;
import com.openkoda.service.notification.NotificationService;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.openkoda.controller.common.PageAttributes.*;
import static com.openkoda.controller.common.URLConstants.DEBUG_MODEL;
import static com.openkoda.controller.common.URLConstants.EXTERNAL_SESSION_ID;
/* TODO: move to correct package */
/**
 * Spring HandlerInterceptor that enriches view models with common attributes before rendering.
 * <p>
 * This interceptor automatically adds standard page attributes to the ModelAndView for all controller
 * methods, providing a consistent set of data available to all views. The enrichment includes user context,
 * organization data, notifications, date formatters, captcha configuration, build information, and session
 * metadata. This ensures views have access to common attributes without requiring each controller to
 * explicitly add them.
 * </p>
 * <p>
 * The enrichment process adds the following standard attributes:
 * <ul>
 *   <li>Current user context (userId, organizationIds)</li>
 *   <li>Organization entity and ID from URL tenant resolution</li>
 *   <li>Unread notifications for dropdown display</li>
 *   <li>Date format helpers and resources version</li>
 *   <li>Privilege helpers for authorization checks</li>
 *   <li>Captcha keys for form protection</li>
 *   <li>Build version and deployment information</li>
 *   <li>Session ID and external session tracking</li>
 *   <li>Common and organization-specific dictionaries</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Important Notes:</strong>
 * <ul>
 *   <li>This interceptor is NOT for security enforcement - use Spring Security for authorization</li>
 *   <li>Redirect responses skip enrichment to avoid unnecessary processing</li>
 *   <li>Uses RequestSessionCacheService for performance optimization via memoization</li>
 *   <li>Thread-safe through Spring's request-scoped injection</li>
 * </ul>
 * </p>
 * <p>
 * Example usage (automatic - no code required):
 * <pre>{@code
 * // In any controller method returning ModelAndView
 * public ModelAndView showPage() {
 *     ModelAndView mav = new ModelAndView("page");
 *     // Interceptor automatically adds PageAttributes constants
 *     return mav; // Will contain organizationEntity, userId, notifications, etc.
 * }
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see PageAttributes
 * @see ModelAndView
 * @see HandlerInterceptor
 * @see RequestSessionCacheService
 */
@Component
public class ModelEnricherInterceptor implements ReadableCode, LoggingComponentWithRequestId, HandlerInterceptor {

    /** Repository for accessing secure entity dictionaries (common and organization-specific). */
    @Inject
    SecureEntityDictionaryRepository secureEntityDictionaryRepository;
    
    /** Repository for loading organization entities by ID from tenant resolution. */
    @Inject
    OrganizationRepository organizationRepository;
    
    /** Service for retrieving user notifications for dropdown display. */
    @Inject
    NotificationService notificationService;
    
    /** Controller for notification-related operations. */
    @Inject
    NotificationController notificationController;
    
    /** Helper for URL parsing and tenant resource resolution. */
    @Inject
    UrlHelper urlHelper;
    
    /** Resolver for determining tenant (organization) context from request. */
    @Inject
    TenantResolver tenantResolver;
    
    /** Service for handling captcha validation in requests. */
    @Inject
    CaptchaService captchaService;
    
    /** Cache service for request-session scoped memoization to optimize repeated lookups. */
    @Inject RequestSessionCacheService cacheService;
    
    /** Default layout template name (configured via default.layout property, defaults to "main"). */
    @Value("${default.layout:main}")
    String defaultLayoutName;
    
    /** Plain layout template name for minimal UI (configured via default.layout.plain property). */
    @Value("${default.layout.plain:plain}")
    String plainLayoutName;
    
    /** Embedded layout template name for iframe contexts (configured via default.layout.embedded property). */
    @Value("${default.layout.embedded:embedded}")
    String embeddedLayoutName;

    /** Service for session management and external session ID tracking. */
    @Inject
    SessionService sessionService;

    /** Build properties from Maven build (optional - may be null in development). */
    @Autowired(required = false)
    private BuildProperties buildProperties;
    
    /** Static resources version for cache busting (initialized at construction time). */
    private static String resourcesVersion;
    
    /** Cached build information map to avoid repeated BuildProperties parsing. */
    private Map<String, Object> buildInfo;

    /**
     * Constructs a new ModelEnricherInterceptor and initializes the static resources version.
     * <p>
     * The resources version is generated from the current timestamp (format: yyMMddHHmm) and
     * is used for cache busting static resources (CSS, JavaScript) in the view templates.
     * This ensures browsers fetch updated resources after deployments.
     * </p>
     */
    public ModelEnricherInterceptor() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmm");
        resourcesVersion = sdf.format(new Date());
    }

    /**
     * Pre-processes the request before handler execution to resolve tenant context and handle captcha.
     * <p>
     * This method executes before the controller method and performs several setup tasks:
     * handles captcha token validation, resolves the current user from security context,
     * determines the tenant (organization) from the URL, sets up external session tracking,
     * and validates user privileges for organization access. If the user has readOrgData privilege,
     * the organization entity is loaded and added to request attributes for use in postHandle.
     * </p>
     *
     * @param request the current HTTP request to pre-process
     * @param response the current HTTP response (not modified)
     * @param handler the handler (controller method) that will be executed
     * @return true to continue processing the request, false to abort
     * @throws Exception if organization lookup fails or other processing errors occur
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        debug("[preHandle]");

        //if there is a captcha token in the request, handle it
        captchaService.handleCaptcha(request);
        OrganizationUser user = cacheService.tryGet(OrganizationUser.class, () -> UserProvider.getFromContext().orElse(null));
        RequestSessionContextMetadata<OrganizationUser> requestSessionMeta = cacheService.getRequestSessionMetadata(request);
        //if there is external session provided, add it to request attributes
        //and it will be added to request Id
        if (requestSessionMeta.getExternalSessionId() != null) {
            RequestContextHolder.getRequestAttributes().setAttribute(EXTERNAL_SESSION_ID, requestSessionMeta.getExternalSessionId(), 0);
        }
        
        TenantResolver.TenantedResource tr = urlHelper.getTenantedResource(request);
        tenantResolver.setTenantedResource(tr);

        if (not(user != null)) {
            debug("[preHandle] no user present");
            return true;
        }

        Long orgId = tr.organizationId;

        if (orgId == null) {
            return true;
        }

        OrganizationUser loggedUser = user;
        trace("[preHandle] got user");
        
        if (loggedUser.hasGlobalOrOrgPrivilege(Privilege.readOrgData, orgId)) {
            Organization org = organizationRepository.findOne(orgId);
            if (org == null) {
                throw new RuntimeException(
                        String.format("Request [%s] for orgId [%d] that does not exist.", request.getRequestURI(), orgId));
            }
            request.setAttribute(ORGANIZATION_ENTITY_ID, orgId);
            request.setAttribute(ORGANIZATION_ENTITY, org);
        }

        return true;
    }

    /**
     * Enriches the ModelAndView with standard page attributes after handler execution.
     * <p>
     * This method is invoked after the controller method completes but before the view is rendered.
     * It adds common attributes to the model including user context, organization data, notifications,
     * dictionaries, date formatters, build information, and session metadata. The enrichment uses
     * RequestSessionCacheService for performance optimization through memoization.
     * </p>
     * <p>
     * Enrichment is skipped for redirect responses (RedirectView or view names starting with "redirect:")
     * to avoid unnecessary processing when the view will not be rendered.
     * </p>
     * <p>
     * Added attributes include: organizationEntity, organizationEntityId, userEntityId,
     * unreadNotificationsList, unreadNotificationsNumber, commonDictionaries, organizationDictionariesJson,
     * defaultLayout, resourcesVersion, buildInfo, and modelAndView reference.
     * </p>
     *
     * @param request the current HTTP servlet request containing organization and user context
     * @param response the current HTTP servlet response (not modified by this method)
     * @param handler the executed handler (controller method) that generated the ModelAndView
     * @param modelAndView the ModelAndView to enrich with standard attributes, or null if handler returned void
     * @throws Exception if enrichment encounters errors (e.g., database access failures)
     * @see PageAttributes
     * @see RequestSessionCacheService
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        debug("[postHandle]");
        if (modelAndView == null || StringUtils.startsWith(modelAndView.getViewName(), "redirect:") || modelAndView.getView() instanceof RedirectView) {
            return;
        }

        
        Map<String, Object> existingModel = modelAndView.getModel();
        //resolve org

        boolean modelHasOrganization = existingModel != null && existingModel.containsKey(organizationEntity.name);
        boolean modelHasOrganizationId = existingModel != null && existingModel.containsKey(organizationEntityId.name);
        


        RequestSessionContextMetadata<ModelCache> requestSessionMeta = cacheService.getRequestSessionMetadata(request);
        ModelCache requestSessionModel = cacheService.tryGet(ModelCache.class, () -> 
            enrichModel(request, modelAndView, existingModel, modelHasOrganization, modelHasOrganizationId,
                    requestSessionMeta)
        );
        
        existingModel.putAll(requestSessionModel.getModel());
    }

    /**
     * Performs the actual model enrichment by adding common attributes and dictionary values.
     * <p>
     * This method adds standard page attributes including organization context, user information,
     * notifications, dictionaries, layout configuration, and build information. The enrichment
     * validates consistency between URL-based organization context and controller-provided model
     * to prevent mismatches. Results are cached in RequestSessionCacheService for performance.
     * </p>
     * <p>
     * For widget scopes, notification enrichment is skipped to optimize performance for embedded
     * components. The method also handles special debug mode when user has global settings privilege
     * and DEBUG_MODEL parameter is present.
     * </p>
     *
     * @param request the current HTTP request containing organization and user attributes
     * @param modelAndView the ModelAndView being enriched (may be modified for debug mode)
     * @param existingModel the existing model map from the controller
     * @param modelHasOrganization true if controller already added organization entity to model
     * @param modelHasOrganizationId true if controller already added organization ID to model
     * @param requestSessionMeta metadata for request-session caching and widget scope detection
     * @return ModelCache containing the enriched model map to merge with existing model
     */
    protected ModelCache enrichModel(HttpServletRequest request, ModelAndView modelAndView,
                                     Map<String, Object> existingModel, boolean modelHasOrganization, boolean modelHasOrganizationId,
                                     RequestSessionContextMetadata<ModelCache> requestSessionMeta) {
        debug("[enrichModel] >>>>  Enriching model");
        Map<String, Object> model = new HashMap<>();
        Long orgId = (Long) request.getAttribute(organizationEntityId.name);
        Organization org = (Organization) request.getAttribute(organizationEntity.name);
        //check consistency between url with organization id and model returned from controller
        //org in the controller should be the same as in the url for consistency reasons
        if (modelHasOrganizationId && orgId != null) {
            Object modelOrgId = existingModel.get(organizationEntityId.name);
            if (not(orgId.equals(modelOrgId))) {
                error("organizationEntityId [{}] page attribute should match the org id [{}] in the url [{}]. Check the Flow model or ModelAndView.",
                        modelOrgId, orgId, request.getRequestURI());
            }
        }
        if (modelHasOrganization && org != null) {
            Organization modelOrg = (Organization) existingModel.get(organizationEntity.name);
            if (not(org.getOrganizationId().equals(modelOrg.getId()))) {
                error("organizationEntity [{}] page attribute should match the org id [{}] in the url [{}]. Check the Flow model or ModelAndView.",
                        modelOrg.getId(), org.getOrganizationId(), request.getRequestURI());
            }
        }

        if (org != null || orgId != null) {
            orgId = orgId != null ? orgId : org.getOrganizationId();
            model.put(organizationDictionariesJson.name, secureEntityDictionaryRepository.getOrganizationDictionaries(orgId));
            model.put(organizationEntity.name, org);
            model.put(organizationEntityId.name, orgId);
        }

        //resolve user
        Optional<OrganizationUser> user = UserProvider.getFromContext();
        boolean isUser = user.map(a -> a.getUser()).map(a -> a.getId()).isPresent();
        boolean userIsInOrg = orgId != null && isUser && user.get().getOrganizationIds().contains(orgId);
        String pageLayout = detectPageLayout(request);
        MutableUserInOrganization userInOrg = ApplicationContextProvider.getContext().getBean(MutableUserInOrganization.class);

        if (isUser) {
            debug("[enrichModel] isUser");
            Long userId = user.get().getUser().getId();
            userInOrg.setUserId(userId);
        }

        if (userIsInOrg) {
            debug("[enrichModel] userIsInOrg");
            userInOrg.setOrganizationId(orgId);
        }

        try {
            model.put(commonDictionaries.name, secureEntityDictionaryRepository.getCommonDictionaries());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        model.put(commonDictionariesNames.name, secureEntityDictionaryRepository.getCommonDictionariesNames());
        model.put(defaultLayout.name, pageLayout);
        model.put(PageAttributes.resourcesVersion.name, resourcesVersion);
        if(buildInfo == null) {
            buildInfo = buildAppInfo();
        }
        
        model.put(PageAttributes.buildInfo.name, buildInfo);
        existingModel.put(PageAttributes.modelAndView.name, modelAndView);

        //Add Notifications to model for dropdown display
        if (isUser) {
            Long userId = user.get().getUser().getId();

            Set<Long> organizationIds;
            if(orgId != null && user.get().getOrganizationIds().contains(orgId)) {
                organizationIds = Collections.singleton(orgId);
            } else {
                organizationIds = user.get().getOrganizationIds();
            }
            
            // perform following model addons only if it's not a 'widget' session/scope
            if(!requestSessionMeta.isWidget()) {
                List<Notification> usersUnreadNotificationsList = notificationService.getUsersUnreadNotifications(userId, organizationIds, PageRequest.of(0, 5));
      
                String unreadNotificationsIdListString = notificationService.getIdListAsString(usersUnreadNotificationsList);
                int unreadNotificationsNumber = notificationService.getUsersUnreadNotificationsNumber(userId, organizationIds);
      
                model.put(readNotificationsList.name, null);
                model.put(unreadNotificationsList.name, usersUnreadNotificationsList);
                model.put(PageAttributes.unreadNotificationsIdListString.name, unreadNotificationsIdListString);
                model.put(PageAttributes.unreadNotificationsNumber.name, unreadNotificationsNumber);
            }

            model.put(userEntityId.name, userId);
        }
        
        
        debug("[enrichModel] <<< Enriched model");
        if (isUser && user.get().hasGlobalPrivilege(Privilege.canAccessGlobalSettings) && request.getParameterMap().containsKey(DEBUG_MODEL)) {
            modelAndView.setViewName("model");
            String s = JsonHelper.toDebugJson(existingModel);
            modelAndView.getModel().clear();
            modelAndView.getModel().put("modelJson", s);
        }
        
        ModelCache dashboardModel = new ModelCache();
        dashboardModel.setModel(model);
        return dashboardModel;
    }

    /**
     * Detects the page layout template to use based on request parameters or referer.
     * <p>
     * Checks the "__view" parameter for explicit layout selection ("plain" or "embedded").
     * If not present, examines the Referer header to maintain layout across page navigation.
     * Defaults to the configured default layout if no indicators are found.
     * </p>
     *
     * @param request the HTTP request to examine for layout indicators
     * @return the layout template name (defaultLayoutName, plainLayoutName, or embeddedLayoutName)
     */
    private String detectPageLayout(HttpServletRequest request) {
        String pageLayoutParameter = request.getParameter("__view");

        if (pageLayoutParameter != null) {
            switch (pageLayoutParameter) {
                case "plain":
                    return plainLayoutName;
                case "embedded":
                    return embeddedLayoutName;
            }
        } else {
            String referer = request.getHeader("Referer");
            if (referer != null) {
                if (referer.contains("__view=plain")) {
                    return plainLayoutName;
                } else if (referer.contains("__view=embedded")) {
                    return embeddedLayoutName;
                }
            }
        }
        return defaultLayoutName;
    }

    /**
     * Builds application build information map from Maven BuildProperties or defaults.
     * <p>
     * Extracts artifact, group, version, timestamp, git branch, commit ID, and hostname
     * from BuildProperties when available (production builds). For development environments
     * without BuildProperties, returns sensible defaults (version "HEAD", branch "local").
     * The result is cached in buildInfo field to avoid repeated parsing.
     * </p>
     *
     * @return map containing build metadata (Artifact, Version, Timestamp, Branch, CommitId, Hostname)
     */
    private Map<String, Object> buildAppInfo(){
        Map<String, Object> map = new HashMap<>();
        DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        if(buildProperties != null) {
            map.put("Artifact", buildProperties.getArtifact());
            map.put("Group", buildProperties.getGroup());
            map.put("Version", buildProperties.getVersion());
            if(buildProperties.getTime() != null) {
                map.put("Timestamp", date.format(LocalDateTime.ofInstant(buildProperties.getTime(), ZoneId.systemDefault())));
            }
            
            map.put("Branch", buildProperties.get("git.branch"));
            map.put("CommitId", buildProperties.get("git.commit.id.abbrev"));
            map.put("Hostname", buildProperties.get("hostname"));
        } else {
            map.put("Version", "HEAD");
            LocalDateTime now = LocalDateTime.now();
            
            map.put("Timestamp", date.format(now));
            map.put("Branch", "local");
            map.put("Hostname", "localhost");
            
        }
        
        return map;
    }
    
    /**
     * Cleans up after request completion, including single-request authentication cleanup.
     * <p>
     * This method executes after the view has been rendered and the response is complete.
     * It checks if the current user was authenticated for a single request only (e.g., API token)
     * and clears the authentication context to prevent session leakage.
     * </p>
     *
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @param handler the handler (controller method) that was executed
     * @param ex any exception thrown during handler execution, or null if successful
     * @throws Exception if cleanup encounters errors
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // After completing the request, if the authentication was for a single request, logout the user
        Optional<OrganizationUser> user = UserProvider.getFromContext();
        boolean isUser = user.map(a -> a.getUser()).map(a -> a.getId()).isPresent();

        if (isUser && user.get().isSingleRequestAuth()) {
            if (user.get().isSingleRequestAuth()) {
                UserProvider.clearAuthentication();
            }
        }
    }

}
