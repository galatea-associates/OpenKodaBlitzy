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

package com.openkoda.core.controller.frontendresource;

import com.openkoda.controller.common.PageAttributes;
import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.flow.BasePageAttributes;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageAttr;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.core.helper.JsonHelper;
import com.openkoda.core.multitenancy.TenantResolver;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.core.security.UserProvider;
import com.openkoda.core.service.FrontendResourceService;
import com.openkoda.dto.system.FrontendResourceDto;
import com.openkoda.form.FrontendResourceForm;
import com.openkoda.form.FrontendResourcePageForm;
import com.openkoda.form.RegisterUserForm;
import com.openkoda.model.component.ControllerEndpoint;
import com.openkoda.model.component.FrontendResource;
import com.openkoda.model.file.File;
import com.openkoda.repository.FrontendResourceRepository;
import com.openkoda.uicomponent.JsFlowRunner;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import static com.openkoda.core.service.FrontendResourceService.frontendResourceTemplateNamePrefix;


/**
 * Singleton-scoped, framework-centric orchestration controller that centralizes creation, validation, 
 * persistence, lifecycle management and runtime invocation of {@link FrontendResource} entities.
 * <p>
 * This abstract controller provides comprehensive management of FrontendResource entities including CRUD operations,
 * content validation, draft/live content management, and runtime execution of JavaScript flows via GraalVM.
 * For FrontendResource instances of {@link FrontendResource.ResourceType#UI_COMPONENT}, this controller resolves 
 * and executes {@link ControllerEndpoint} flows based on URL sub-paths and HTTP methods.
 * </p>
 * <p>
 * <b>Primary Responsibilities:</b>
 * <ul>
 *   <li>Validating and updating resource content with syntax/structure validation</li>
 *   <li>Preparing and persisting new FrontendResource records with uniqueness checks</li>
 *   <li>Publishing resources (single or batch) by copying draft content to live content</li>
 *   <li>Reloading draft content from application defaults or resources</li>
 *   <li>Copying default/live content into draft for editing</li>
 *   <li>Preparing empty forms for FrontendResource creation</li>
 *   <li>Resolving and executing ControllerEndpoint flows for UI_COMPONENT resources</li>
 * </ul>
 * </p>
 * <p>
 * <b>Controller-Endpoint Execution Feature:</b><br>
 * For FrontendResource instances of type {@link FrontendResource.ResourceType#UI_COMPONENT}, this controller
 * resolves {@link ControllerEndpoint} entities by matching frontendResourceId, URL subPath, and 
 * {@link ControllerEndpoint.HttpMethod}. The matched endpoint's JavaScript flow is executed via 
 * {@link JsFlowRunner#runPreviewFlow} or {@link JsFlowRunner#runLiveFlow} based on the preview flag.
 * </p>
 * <p>
 * <b>Script Naming Convention:</b><br>
 * JavaScript flow scripts are named using the pattern {@code controllerEndpoint-<id>.mjs} where {@code <id>}
 * is the ControllerEndpoint entity ID. This convention is implemented by {@link #deductScriptSourceFileName(ControllerEndpoint)}
 * and used for script source mapping and debugging in the GraalVM context.
 * </p>
 * <p>
 * <b>Response Type Mapping:</b><br>
 * The controller supports multiple response types for ControllerEndpoint execution:
 * <ul>
 *   <li><b>MODEL_AS_JSON:</b> Serializes PageAttr keys to JSON with Content-Type: application/json</li>
 *   <li><b>FILE:</b> Streams file content via InputStreamResource with headers: Content-Type, Accept-Ranges, 
 *       Cache-Control (max-age=604800, public), Content-Disposition (inline with filename)</li>
 *   <li><b>STREAM:</b> Wraps InputStream into InputStreamResource for binary streaming</li>
 *   <li><b>HTML:</b> Returns ModelAndView with Thymeleaf template name resolution</li>
 * </ul>
 * </p>
 * <p>
 * <b>Security:</b><br>
 * Operations are protected by {@code @PreAuthorize} annotations that enforce privilege checks.
 * Unauthorized access returns HTTP 401. The controller implements {@link HasSecurityRules} for 
 * consistent security rule application.
 * </p>
 * <p>
 * <b>Multi-Tenancy:</b><br>
 * All operations are tenant-aware via {@link TenantResolver} and organizationId parameters.
 * Resource queries are scoped by organization and access level to ensure tenant isolation.
 * </p>
 * <p>
 * <b>Flow Pipeline Patterns:</b><br>
 * The controller extensively uses the fluent {@link Flow} API for composing backend workflows:
 * <pre>{@code
 * Flow.init().then(a -> operation1()).thenSet(key, a -> operation2()).execute();
 * }</pre>
 * This pattern enables transactional execution, error handling, and model composition.
 * </p>
 * <p>
 * <b>Dependency Injection:</b><br>
 * Key dependencies injected via {@code @Inject}:
 * <ul>
 *   <li>{@link JsFlowRunner} - Executes JavaScript flows in GraalVM context</li>
 *   <li>{@link FrontendResourceService} - Validates content, publishes resources, retrieves defaults</li>
 *   <li>Repositories - Database access for FrontendResource, ControllerEndpoint entities</li>
 *   <li>{@link TenantResolver} - Resolves tenant context and access levels</li>
 *   <li>{@link UserProvider} - Provides userId for JavaScript flow execution</li>
 *   <li>{@link PageModelMap}, {@link PageAttr} - Model data carriers for Flow pipelines</li>
 * </ul>
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FrontendResourceControllerHtml
 * @see FrontendResourceService
 * @see JsFlowRunner
 * @see ControllerEndpoint
 * @see FrontendResource
 */
public class AbstractFrontendResourceController extends AbstractController implements HasSecurityRules {

    /**
     * Injected JsFlowRunner for executing JavaScript flows in GraalVM context.
     * Used to run preview and live flows for ControllerEndpoint execution with support
     * for script source mapping, debugging, and polyglot value conversion.
     */
    @Inject
    private JsFlowRunner jsFlowRunner;

    /**
     * Validates the data and updates the content of {@link FrontendResource} with the specified ID.
     * <p>
     * This method executes a transactional Flow pipeline that validates the provided content string
     * for syntax and structure correctness, updates the FrontendResource entity's content field in the
     * database, and evicts the cached entity to ensure subsequent queries return the updated content.
     * </p>
     *
     * @param content JavaScript or HTML code string to validate and persist. Must conform to the 
     *                resource type's expected syntax (JavaScript for UI_COMPONENT, HTML for PAGE types).
     * @param frontendResourceId Primary key ID of the FrontendResource entity to update. Must reference
     *                           an existing entity or the operation will fail.
     * @return {@link PageModelMap} containing the validation results and updated entity. Returns error
     *         indicators if validation fails or entity not found.
     * @see FrontendResourceService#validateContent(String, BindingResult, String)
     * @see FrontendResourceRepository#updateContent(Long, String)
     * @see FrontendResourceRepository#evictOne(Long)
     */
    protected PageModelMap updateFrontendResource(String content, Long frontendResourceId){
        return Flow.init(transactional)
                .then(a -> services.frontendResource.validateContent(content, null, "dto.content"))
                .then(a -> repositories.unsecure.frontendResource.updateContent(frontendResourceId, content))
                .then(a -> repositories.unsecure.frontendResource.evictOne(frontendResourceId))
                .execute();
    }

    /**
     * Retrieves {@link FrontendResource} from the database by ID and prepares {@link FrontendResourceForm} for editing.
     * <p>
     * This method queries the FrontendResource repository for the entity matching the provided ID,
     * then constructs a FrontendResourceForm populated with the entity data and organization context
     * for use in update operations.
     * </p>
     *
     * @param organizationId Organization context for multi-tenancy. Used to scope the form to the
     *                       correct tenant and validate organization-level permissions.
     * @param frontendResourceId Primary key of the FrontendResource entity to retrieve. Must reference
     *                           an existing entity.
     * @return {@link PageModelMap} containing {@code frontendResourceEntity} (the retrieved FrontendResource)
     *         and {@code frontendResourceForm} (populated form ready for editing). Returns empty model if
     *         entity not found.
     * @see FrontendResourceRepository#findOne(Long)
     * @see FrontendResourceForm
     */
    protected PageModelMap findFrontendResource(Long organizationId, long frontendResourceId) {
        debug("[findFrontendResource] FrontendResourceId: {}", frontendResourceId);
        return Flow.init(frontendResourceId)
                .thenSet(frontendResourceEntity, a -> repositories.unsecure.frontendResource.findOne(frontendResourceId))
                .thenSet(frontendResourceForm, a -> new FrontendResourceForm(organizationId, a.model.get(frontendResourceEntity)))
                .execute();
    }

    /**
     * Publishes the {@link FrontendResource} with the specified ID by copying draft content to live content.
     * <p>
     * This method executes a publish workflow that retrieves the draft FrontendResource entity,
     * calls {@link FrontendResourceService#publish(FrontendResource)} to copy the draftContent field
     * to the content field (making it live), and persists the updated entity. This operation makes
     * draft changes visible to end users.
     * </p>
     *
     * @param frontendResourceId ID of the FrontendResource to publish. Must reference an existing
     *                           entity with draft content.
     * @return {@link PageModelMap} containing {@code frontendResourceEntity} with the published entity
     *         (draft content now copied to live content field).
     * @see FrontendResourceService#publish(FrontendResource)
     * @see FrontendResourceRepository#findOne(Long)
     * @see FrontendResourceRepository#save(FrontendResource)
     */
    protected PageModelMap publishFrontendResource(long frontendResourceId) {
        debug("[publishFrontendResource] FrontendResourceId: {}", frontendResourceId);
        return Flow.init()
                .then(a -> repositories.unsecure.frontendResource.findOne(frontendResourceId))
                .then(a -> services.frontendResource.publish(a.result))
                .thenSet(frontendResourceEntity, a -> repositories.unsecure.frontendResource.save(a.result))
                .execute();
    }


    /**
     * Publishes all draft {@link FrontendResource} entities by copying draft content to live content in batch.
     * <p>
     * This method executes a transactional batch publish workflow that finds all FrontendResource entities
     * where {@code isDraft == true} via {@link FrontendResourceRepository#findAllAsStreamByIsDraftTrue()},
     * then calls {@link FrontendResourceService#publishAll(Stream)} to copy draftContent to content field
     * for all entities in the stream. This operation makes all pending draft changes live simultaneously.
     * </p>
     *
     * @return {@link PageModelMap} containing a Stream of published FrontendResource entities. Each entity
     *         in the stream has draft content copied to live content.
     * @see FrontendResourceService#publishAll(java.util.stream.Stream)
     * @see FrontendResourceRepository#findAllAsStreamByIsDraftTrue()
     */
    protected PageModelMap publishAllFrontendResource() {
        debug("[publishAllFrontendResource]");
        return Flow.init(transactional)
                .then(a -> repositories.unsecure.frontendResource.findAllAsStreamByIsDraftTrue())
                .then(a -> services.frontendResource.publishAll(a.result))
                .execute();
    }


    /**
     * Clears the content of the {@link FrontendResource} and reloads it from application defaults or resources.
     * <p>
     * This method clears the current content of the FrontendResource entity via 
     * {@link FrontendResourceService#clear(FrontendResource)}, then loads fresh content directly from 
     * packaged application resources or retrieves the default content based on the resource's type, name, 
     * access level, and organizationId. The resource remains in draft state after reload.
     * </p>
     *
     * @param frontendResourceId ID of the FrontendResource entity to reload. Must reference an existing entity.
     * @return {@link PageModelMap} containing the reloaded FrontendResource entity with content reset to
     *         application default or packaged resource content.
     * @see FrontendResourceService#clear(FrontendResource)
     * @see FrontendResourceService#getContentOrDefault(FrontendResource.Type, String, FrontendResource.AccessLevel, Long)
     * @see FrontendResourceRepository#findOne(Long)
     * @see FrontendResourceRepository#save(FrontendResource)
     */
    protected PageModelMap reloadFrontendResource(long frontendResourceId) {
        debug("[reloadFrontendResource] FrontendResourceId: {}", frontendResourceId);
        return Flow.init()
                .then(a -> repositories.unsecure.frontendResource.findOne(frontendResourceId))
                .then(a -> services.frontendResource.clear(a.result))
                .then(a -> {
                    a.result.setContent(services.frontendResource.getContentOrDefault(a.result.getType(), a.result.getName(), a.result.getAccessLevel(), a.result.getOrganizationId()));
                    return a.result;
                })
                .then(a -> repositories.unsecure.frontendResource.save(a.result))
                .execute();
    }

    /**
     * Copies default resource content from application resources to the draft content field of {@link FrontendResource}.
     * <p>
     * This method retrieves the FrontendResource entity, loads default content from packaged application
     * resources via {@link FrontendResourceService#getContentOrDefault} based on the resource's type, name,
     * access level and organizationId, then sets this content as draftContent. The live content field remains
     * unchanged, allowing the default content to be edited before publishing.
     * </p>
     *
     * @param frontendResourceId ID of the FrontendResource entity to update with default draft content.
     * @return {@link PageModelMap} containing the updated FrontendResource entity with draftContent populated
     *         from application defaults.
     * @see FrontendResourceService#getContentOrDefault(FrontendResource.Type, String, FrontendResource.AccessLevel, Long)
     * @see FrontendResourceRepository#findOne(Long)
     * @see FrontendResourceRepository#save(FrontendResource)
     */
    protected PageModelMap copyResourceContentToDraft(long frontendResourceId) {
        debug("[loadFrontendResourceToDraft] FrontendResourceId: {}", frontendResourceId);
        return Flow.init()
                .then(a -> repositories.unsecure.frontendResource.findOne(frontendResourceId))
                .then(a -> {
                    a.result.setDraftContent(services.frontendResource.getContentOrDefault(a.result.getType(), a.result.getName(), a.result.getAccessLevel(), a.result.getOrganizationId()));
                    return a.result;
                })
                .then(a -> repositories.unsecure.frontendResource.save(a.result))
                .execute();
    }

    /**
     * Copies the live content field to the draft content field of {@link FrontendResource} for editing.
     * <p>
     * This method retrieves the FrontendResource entity and copies the current live content (content field)
     * to the draftContent field, enabling modifications to the live version without affecting the currently
     * published content. The updated entity is persisted to the database.
     * </p>
     *
     * @param frontendResourceId ID of the FrontendResource entity to copy live content from.
     * @return {@link PageModelMap} containing the updated FrontendResource entity with draftContent set to
     *         a copy of the live content.
     * @see FrontendResourceRepository#findOne(Long)
     * @see FrontendResourceRepository#save(FrontendResource)
     */
    protected PageModelMap copyLiveContentToDraft(long frontendResourceId) {
        debug("[copyLiveContentToDraft] FrontendResourceId: {}", frontendResourceId);
        return Flow.init()
                .then(a -> repositories.unsecure.frontendResource.findOne(frontendResourceId))
                .then(a -> {
                    a.result.setDraftContent(a.result.getContent());
                    return a.result;
                })
                .then(a -> repositories.unsecure.frontendResource.save(a.result))
                .execute();
    }

    /**
     * Validates data for a new {@link FrontendResource} and saves it to the database if validation succeeds.
     * <p>
     * This method executes a multi-stage validation sequence:
     * <ol>
     *   <li>Checks name uniqueness via {@link FrontendResourceService#checkNameExists} to prevent duplicates</li>
     *   <li>Validates content syntax/structure via {@link FrontendResourceService#validateContent}</li>
     *   <li>Performs Jakarta Bean Validation and populates a new FrontendResource entity</li>
     *   <li>Saves the validated entity to the database</li>
     *   <li>Returns a new empty form for subsequent create operations</li>
     * </ol>
     * </p>
     *
     * @param frontendResourceFormData Form containing {@link FrontendResourceDto} with name, content, type,
     *                                 and accessLevel fields. All fields undergo validation.
     * @param br BindingResult for collecting validation errors. Errors are added if name exists, content
     *           is invalid, or bean validation fails.
     * @return {@link PageModelMap} containing the saved {@code frontendResourceEntity} and a new empty
     *         {@code frontendResourceForm} ready for creating another resource. Returns validation errors
     *         in model if any validation step fails.
     * @see FrontendResourceService#checkNameExists(String, BindingResult)
     * @see FrontendResourceService#validateContent(String, BindingResult, String)
     * @see com.openkoda.core.service.ValidationService#validateAndPopulateToEntity
     * @see FrontendResourceRepository#save(FrontendResource)
     */
    protected PageModelMap createFrontendResource(FrontendResourceForm frontendResourceFormData, BindingResult br) {
        debug("[createFrontendResource]");
        return Flow.init(frontendResourceForm, frontendResourceFormData)
                .then(a -> services.frontendResource.checkNameExists(((FrontendResourceDto) frontendResourceFormData.dto).name, br))
                .then(a -> services.frontendResource.validateContent(((FrontendResourceDto) frontendResourceFormData.dto).content, br, "dto.content"))
                .then(a -> services.validation.validateAndPopulateToEntity(frontendResourceFormData, br, new FrontendResource()))
                .then(a -> repositories.unsecure.frontendResource.save(((FrontendResource)a.result)))
                .thenSet(frontendResourceForm, a -> new FrontendResourceForm())
                .execute();
    }

    /**
     * Prepares an empty {@link FrontendResourceForm} for creating a new FrontendResource.
     * <p>
     * This method constructs and returns a new, unpopulated FrontendResourceForm scoped to the
     * specified organization context. The form is ready to accept user input for creating a new
     * FrontendResource entity.
     * </p>
     *
     * @param organizationId Organization context for multi-tenancy. The created resource will be
     *                       scoped to this organization.
     * @param type The {@link FrontendResource.Type} enum value indicating the resource type
     *             (e.g., HTML, JAVASCRIPT, CSS) for the new resource.
     * @return {@link PageModelMap} containing {@code organizationRelatedForm} with an empty
     *         FrontendResourceForm ready for data entry.
     */
    protected PageModelMap getFrontendResourceForm(Long organizationId, FrontendResource.Type type) {
        debug("[createFrontendResource]");
        return Flow.init()
                .thenSet(organizationRelatedForm, a ->new FrontendResourceForm())
                .execute();
    }

    /**
     * Core runtime execution method for FrontendResource pages and UI components.
     * <p>
     * This method finds the requested {@link FrontendResource} by URL path and organization context,
     * prepares the model for display, and for {@link FrontendResource.ResourceType#UI_COMPONENT} types,
     * resolves and executes matching {@link ControllerEndpoint} JavaScript flows.
     * </p>
     * <p>
     * <b>Resolution Strategy:</b><br>
     * Uses {@link TenantResolver#getTenantedResource()} to determine the current tenant's access level,
     * then queries via {@link FrontendResourceRepository#findByUrlPathAndAccessLevelAndOrganizationId}
     * with priority-based sorting to find the most specific matching resource.
     * </p>
     * <p>
     * <b>Special Path Handling:</b><br>
     * For the special "REGISTER" path, injects a {@link RegisterUserForm} into the ModelAndView for
     * user registration functionality.
     * </p>
     * <p>
     * <b>View Name Resolution:</b><br>
     * Constructs Thymeleaf view names using the pattern: {@code frontendResourceTemplateNamePrefix + resource.getName()}.
     * If no resource is found in the database, falls back to a template named after the frontendResourcePath.
     * </p>
     * <p>
     * <b>UI_COMPONENT Handling:</b><br>
     * For resources of type {@link FrontendResource.ResourceType#UI_COMPONENT}, resolves a 
     * {@link ControllerEndpoint} by matching frontendResourceId, subPath, and httpMethod. If found,
     * delegates to {@link #evaluateControllerEndpoint} to execute the endpoint's JavaScript flow and
     * return the appropriate response (HTML ModelAndView or ResponseEntity for JSON/FILE/STREAM types).
     * </p>
     * <p>
     * Example query pattern:
     * <pre>{@code
     * Page<Object[]> resources = repository.findByUrlPathAndAccessLevelAndOrganizationId(
     *     "/dashboard", resourceId, PUBLIC, orgId, PageRequest.of(0,1, ASC, "priority")
     * );
     * }</pre>
     * </p>
     *
     * @param organizationId Organization context for multi-tenancy. Used to scope resource queries and
     *                       JavaScript flow execution to the correct tenant.
     * @param frontendResourcePath URL path of the {@link FrontendResource} to invoke (e.g., "/dashboard", "/api/data").
     * @param frontendResourceId Optional ID for direct FrontendResource lookup. Pass null to query by path only.
     * @param subPath Sub-path for ControllerEndpoint matching (e.g., "/submit" for endpoint resolution).
     *                Used only for UI_COMPONENT resources.
     * @param httpMethod HTTP method enum ({@link ControllerEndpoint.HttpMethod#GET}, POST, PUT, DELETE, PATCH)
     *                   for ControllerEndpoint matching. Used only for UI_COMPONENT resources.
     * @param preview Boolean flag indicating preview mode vs. live mode. When true, executes JavaScript flows
     *                in preview mode with different validation and error handling.
     * @param requestParams Map of request parameters from {@link HttpServletRequest}. Passed to JavaScript
     *                      flow execution context for access in ControllerEndpoint code.
     * @param form Optional form for REGISTER path or ControllerEndpoint form binding. Injected into ModelAndView
     *             or passed to JavaScript flow execution context.
     * @return {@link ModelAndView} for HTML responses with Thymeleaf template name and model data, or
     *         {@link ResponseEntity} for non-HTML response types (MODEL_AS_JSON, FILE, STREAM) from
     *         ControllerEndpoint execution.
     * @see TenantResolver#getTenantedResource()
     * @see FrontendResourceRepository#findByUrlPathAndAccessLevelAndOrganizationId
     * @see #evaluateControllerEndpoint
     * @see com.openkoda.repository.ControllerEndpointRepository#findByFrontendResourceIdAndSubPathAndHttpMethod
     */
    protected Object invokeFrontendResourceEntry(Long organizationId,
                                                 String frontendResourcePath,
                                                 Long frontendResourceId,
                                                 String subPath,
                                                 ControllerEndpoint.HttpMethod httpMethod,
                                                 boolean preview,
                                                 Map<String,String> requestParams,
                                                 AbstractOrganizationRelatedEntityForm form) {
        debug("[invokeFrontendResourceEntry] FrontendResourcePath: {}", frontendResourcePath);
        FrontendResourceRepository fr = repositories.unsecure.frontendResource;
        FrontendResource frontendResource = null;
        Pageable pageable = PageRequest.of(0,1, Sort.Direction.ASC, "priority");
        TenantResolver.TenantedResource tenantedResource = TenantResolver.getTenantedResource();


        Page<Object[]> frontendResources = fr.findByUrlPathAndAccessLevelAndOrganizationId(
                frontendResourcePath,
                frontendResourceId,
                tenantedResource.accessLevel,
                organizationId,
                pageable);

        if(!frontendResources.isEmpty()) {
            frontendResource = (FrontendResource) frontendResources.getContent().get(0)[0];
        }

        ModelAndView mav = new ModelAndView();
        if(REGISTER.equals(frontendResourcePath)) {
            mav.addObject("registerForm", new RegisterUserForm());
        }
        if (frontendResource != null) {
            mav.setViewName(FrontendResourceService.frontendResourceTemplateNamePrefix + frontendResource.getName());
            if(frontendResource.getResourceType().equals(FrontendResource.ResourceType.UI_COMPONENT)) {
                ControllerEndpoint controllerEndpoint = repositories.unsecure.controllerEndpoint.findByFrontendResourceIdAndSubPathAndHttpMethod(
                    frontendResource.getId(),
                    subPath,
                    httpMethod);
                if(controllerEndpoint != null) {
                    return evaluateControllerEndpoint(organizationId, frontendResourcePath, subPath,
                            mav, frontendResource, httpMethod, controllerEndpoint, preview, requestParams, form);
                }
            }
        } else {
            debug("[invokeFrontendResourceEntry] FrontendResourceEntry not found in db: {}", frontendResourcePath);
            mav.setViewName(frontendResourceTemplateNamePrefix + frontendResourcePath);
        }
        return mav;
    }

    /**
     * Evaluates the {@link ControllerEndpoint} JavaScript flow and prepares the response based on response type.
     * <p>
     * This private method executes the JavaScript code associated with a ControllerEndpoint entity via
     * {@link JsFlowRunner} in GraalVM context, merges the resulting {@link PageModelMap} into the provided
     * ModelAndView, and determines the appropriate view name or response entity based on the execution result
     * and configured {@link ControllerEndpoint.ResponseType}.
     * </p>
     * <p>
     * <b>Script Source Filename Deduction:</b><br>
     * Generates script filename via {@link #deductScriptSourceFileName(ControllerEndpoint)} using pattern
     * {@code controllerEndpoint-<id>.mjs} for GraalVM debugging and stack traces.
     * </p>
     * <p>
     * <b>Preview vs. Live Execution:</b><br>
     * Executes {@link JsFlowRunner#runPreviewFlow} when preview=true for draft content with relaxed validation,
     * or {@link JsFlowRunner#runLiveFlow} when preview=false for published content with strict validation.
     * Both executions inject organizationId and userId from {@link UserProvider#getUserIdOrNotExistingId()}
     * into the JavaScript context.
     * </p>
     * <p>
     * <b>PageModelMap Merge:</b><br>
     * The returned PageModelMap from JavaScript execution is merged into the ModelAndView's ModelMap, making
     * all JavaScript-set page attributes available in the Thymeleaf template context.
     * </p>
     * <p>
     * <b>View Name Selection Logic (HTML Response Type):</b><br>
     * Determines view name based on PageModelMap attributes and form presence:
     * <ul>
     *   <li>If {@link BasePageAttributes#reload} key present and no error: {@code "generic-forms::reload"}</li>
     *   <li>If {@code redirectUrl} key present and no error: {@code "generic-forms::go-to(url='<redirectUrl>')"}</li>
     *   <li>If error and form is null: {@code "webendpoint-settings::preview-error"}</li>
     *   <li>If error and form present: {@code "generic-settings-entity-form::generic-settings-form-error"}</li>
     *   <li>If no error and form present: {@code "generic-settings-entity-form::generic-settings-form-reload"}</li>
     * </ul>
     * Error state is determined by {@link BasePageAttributes#isError} attribute.
     * </p>
     * <p>
     * <b>ResponseType.HTML Switch Case:</b><br>
     * Returns the populated ModelAndView for rendering via Thymeleaf with the determined view name.
     * </p>
     * <p>
     * <b>Default Case (Non-HTML Response Types):</b><br>
     * Delegates to {@link #getControllerEndpointResult} for MODEL_AS_JSON, FILE, and STREAM response types,
     * which returns a ResponseEntity with appropriate Content-Type headers and response body.
     * </p>
     * <p>
     * Example JavaScript flow execution:
     * <pre>{@code
     * PageModelMap result = jsFlowRunner.runPreviewFlow(
     *     jsCode, requestParams, organizationId, userId, form, "controllerEndpoint-123.mjs"
     * );
     * }</pre>
     * </p>
     *
     * @param organizationId Organization context for JavaScript flow execution and multi-tenancy.
     * @param frontendResourcePath URL path of the {@link FrontendResource} for logging and context.
     * @param subPath Sub-path for the resource invocation (used for logging).
     * @param mav {@link ModelAndView} to populate with JavaScript execution results and view name.
     * @param frontendResource {@link FrontendResource} entity being invoked (for context).
     * @param httpMethod {@link ControllerEndpoint.HttpMethod} of the request (GET, POST, etc.).
     * @param controllerEndpoint {@link ControllerEndpoint} entity containing the JavaScript code to execute
     *                           and response type configuration.
     * @param preview Boolean flag for preview mode (true) vs. live mode (false) execution.
     * @param requestParams Map of request parameters from {@link HttpServletRequest} passed to JavaScript context.
     * @param form Optional {@link AbstractOrganizationRelatedEntityForm} for form binding. Injected into
     *             JavaScript context and used for view name determination.
     * @return {@link ModelAndView} for HTML response type with populated model and view name, or
     *         {@link ResponseEntity} for MODEL_AS_JSON, FILE, or STREAM response types with appropriate
     *         Content-Type headers and response body.
     * @see JsFlowRunner#runPreviewFlow
     * @see JsFlowRunner#runLiveFlow
     * @see #getControllerEndpointResult
     * @see #deductScriptSourceFileName(ControllerEndpoint)
     */
    private Object evaluateControllerEndpoint(Long organizationId,
                                              String frontendResourcePath,
                                              String subPath,
                                              ModelAndView mav,
                                              FrontendResource frontendResource,
                                              ControllerEndpoint.HttpMethod httpMethod,
                                              ControllerEndpoint controllerEndpoint,
                                              boolean preview,
                                              Map<String,String> requestParams,
                                              AbstractOrganizationRelatedEntityForm form) {
        switch (controllerEndpoint.getResponseType()) {
            case HTML -> {
                String scriptSourceFileName = deductScriptSourceFileName(controllerEndpoint);
//                        display frontend resource normally and run if available
                debug("[evaluateControllerEndpoint] Run ControllerEndpoint Flow Id {} for HTTP Method {}",
                        controllerEndpoint.getId(), httpMethod);
                long userId = UserProvider.getUserIdOrNotExistingId();
                PageModelMap pageModelMap = preview
                        ? jsFlowRunner.runPreviewFlow(controllerEndpoint.getCode(), requestParams, organizationId, userId, form, scriptSourceFileName)
                        : jsFlowRunner.runLiveFlow(controllerEndpoint.getCode(), requestParams, organizationId, userId, form, scriptSourceFileName);
                mav.getModelMap().putAll(pageModelMap);
                boolean isError = Boolean.TRUE.equals(pageModelMap.get(BasePageAttributes.isError));
                if (pageModelMap.has(reload) && !isError) {
                    mav.setViewName("generic-forms::reload");
                } else if (pageModelMap.has(redirectUrl) && !isError) {
                    mav.setViewName("generic-forms::go-to(url='" + pageModelMap.get(redirectUrl) + "')");
                } else {
                    if (form == null) {
                        if (isError) {
                            mav.setViewName("webendpoint-" + SETTINGS + "::" + PREVIEW + "-error");
                        }
                    } else {
                        mav.getModelMap().put("organizationRelatedForm", form);
                        if (isError) {
                            mav.setViewName("generic-settings-entity-form::generic-settings-form-error");
                        } else {
                            mav.setViewName("generic-settings-entity-form::generic-settings-form-reload");
                        }
                    }
                }
                return mav;
            }
            default -> {
                // get controller endpoint result
                PageModelMap pageModelMap = getControllerEndpointResult(organizationId, frontendResourcePath, subPath, frontendResource, controllerEndpoint, httpMethod, preview, requestParams, form);
                Object body = pageModelMap.get(PageAttributes.controllerEndpointResult);
                return new ResponseEntity(body, pageModelMap.get(httpHeaders), HttpStatus.OK);
            }
        }
    }


    /**
     * Retrieves {@link FrontendResource} of type {@link FrontendResource.Type#HTML} and prepares {@link FrontendResourcePageForm}.
     * <p>
     * This method is specifically for HTML page type resources. It sets the {@code isPageEditor} flag to true,
     * calls {@link FrontendResourceService#prepareFrontendResourcePageEntity} to retrieve and prepare the
     * FrontendResource entity with page-specific processing, then constructs a FrontendResourcePageForm
     * populated with the entity data for editing HTML content.
     * </p>
     *
     * @param organizationId Organization context for multi-tenancy and form scoping.
     * @param frontendResourceId Primary key of the FrontendResource entity to retrieve. Must be of type HTML.
     * @return {@link PageModelMap} containing {@code isPageEditor=true}, {@code frontendResourceEntity}
     *         (prepared HTML page entity), and {@code frontendResourcePageForm} (populated form for editing).
     * @see FrontendResourceService#prepareFrontendResourcePageEntity(Long)
     * @see FrontendResourcePageForm
     */
    // Frontend Resource Pages
    protected PageModelMap findFrontendResourcePage(Long organizationId, Long frontendResourceId) {
        debug("[findFrontendResourcePage] FrontendResourceId: {}", frontendResourceId);
        Pageable page = PageRequest.of(0, 100);
        return Flow.init(isPageEditor, true)
                .thenSet(frontendResourceEntity, a -> services.frontendResource.prepareFrontendResourcePageEntity(frontendResourceId))
                .thenSet(frontendResourcePageForm, a -> new FrontendResourcePageForm(organizationId, a.result))
                .execute();
    }

    /**
     * Validates and updates a {@link FrontendResource} of type {@link FrontendResource.Type#HTML} in the database.
     * <p>
     * This method executes a transactional Flow pipeline for HTML page resources that:
     * <ol>
     *   <li>Stores the form in PageModelMap for reference</li>
     *   <li>Validates name uniqueness via {@link FrontendResourceService#checkNameExists}</li>
     *   <li>Prepares the page entity via {@link FrontendResourceService#prepareFrontendResourcePage}
     *       with contentEditable flag from form DTO</li>
     *   <li>Performs Jakarta Bean Validation and populates the prepared entity from form data</li>
     *   <li>Saves the validated entity to the database</li>
     * </ol>
     * </p>
     *
     * @param frontendResourceForm {@link FrontendResourcePageForm} containing {@link FrontendResourceDto}
     *                             with name, content, and contentEditable fields for HTML page updates.
     * @param frontendResourceId Primary key of the FrontendResource entity to update. Must be of type HTML.
     * @param br BindingResult for collecting validation errors. Errors added if name exists or bean validation fails.
     * @return {@link PageModelMap} containing {@code frontendResourcePageForm} and {@code frontendResourceEntity}
     *         with the saved entity. Returns validation errors if any validation step fails.
     * @see FrontendResourceService#checkNameExists(String, BindingResult)
     * @see FrontendResourceService#prepareFrontendResourcePage(Long, Boolean)
     * @see com.openkoda.core.service.ValidationService#validateAndPopulateToEntity
     * @see FrontendResourceRepository#save(FrontendResource)
     */
    protected PageModelMap updateFrontendResourcePage(FrontendResourcePageForm frontendResourceForm, Long frontendResourceId, BindingResult br) {
        debug("[createFrontendResource]");
        return Flow.init(transactional)
                .thenSet(frontendResourcePageForm, a -> frontendResourceForm)
                .then(a -> services.frontendResource.checkNameExists(frontendResourceForm.dto.name, br))
                .then(a -> services.frontendResource.prepareFrontendResourcePage(frontendResourceId, frontendResourceForm.dto.contentEditable))
                .then(a -> services.validation.validateAndPopulateToEntity(frontendResourceForm, br, a.result))
                .thenSet(frontendResourceEntity, a -> repositories.unsecure.frontendResource.save(a.result))
                .execute();
    }

    /**
     * Prepares the response body and headers for {@link ControllerEndpoint} JavaScript flow evaluation results.
     * <p>
     * This method constructs a comprehensive {@link PageModelMap} containing the ControllerEndpoint entity,
     * FrontendResource entity, JavaScript execution results, configured HTTP headers, and the final response
     * body. It handles response types MODEL_AS_JSON, FILE, and STREAM by executing the JavaScript flow and
     * transforming results into appropriate response formats.
     * </p>
     * <p>
     * <b>Flow Pipeline Execution:</b><br>
     * Executes a Flow pipeline that:
     * <ol>
     *   <li>Sets {@code controllerEndpoint} in model with the provided ControllerEndpoint entity</li>
     *   <li>Sets {@code frontendResourceEntity} in model with the FrontendResource entity</li>
     *   <li>Sets {@code uiComponentModel} by executing JavaScript flow (preview or live mode) via
     *       {@link JsFlowRunner#runPreviewFlow} or {@link JsFlowRunner#runLiveFlow} with organizationId,
     *       userId from {@link UserProvider#getUserIdOrNotExistingId()}, and request parameters</li>
     *   <li>Sets {@code httpHeaders} by calling {@link #setupHttpHeaders(ControllerEndpoint)} to populate
     *       headers from ControllerEndpoint.httpHeadersMap configuration</li>
     *   <li>Sets {@code controllerEndpointResult} by switching on {@link ControllerEndpoint.ResponseType}:
     *     <ul>
     *       <li><b>MODEL_AS_JSON:</b> Calls {@link #getModelAsJsonResult} to serialize PageModelMap to JSON</li>
     *       <li><b>FILE:</b> Calls {@link #getFileResult} to create InputStreamResource from File object,
     *           returns null on IOException/SQLException</li>
     *       <li><b>STREAM:</b> Extracts InputStream from first page attribute name and wraps in InputStreamResource</li>
     *     </ul>
     *   </li>
     * </ol>
     * </p>
     * <p>
     * Script source filename is deduced via {@link #deductScriptSourceFileName(ControllerEndpoint)}.
     * </p>
     *
     * @param organizationId Organization context for JavaScript flow execution and multi-tenancy.
     * @param frontendResourceUrl URL path of the {@link FrontendResource} for logging and context.
     * @param subPath Sub-path for the resource invocation (used for logging).
     * @param frontendResource {@link FrontendResource} entity being invoked. Stored in model as frontendResourceEntity.
     * @param cEndpoint {@link ControllerEndpoint} entity containing JavaScript code, response type, HTTP headers,
     *                  and page attribute configuration.
     * @param httpMethod {@link ControllerEndpoint.HttpMethod} of the request (GET, POST, etc.).
     * @param preview Boolean flag for preview mode (true) vs. live mode (false) JavaScript execution.
     * @param requestParams Map of request parameters from {@link HttpServletRequest} passed to JavaScript context.
     * @param form Optional {@link AbstractOrganizationRelatedEntityForm} for form binding in JavaScript context.
     * @return {@link PageModelMap} containing keys: {@code controllerEndpoint}, {@code frontendResourceEntity},
     *         {@code uiComponentModel} (PageModelMap from JS execution), {@code httpHeaders} (populated HttpHeaders),
     *         and {@code controllerEndpointResult} (String JSON, InputStreamResource, or null based on response type).
     * @see JsFlowRunner#runPreviewFlow
     * @see JsFlowRunner#runLiveFlow
     * @see #setupHttpHeaders(ControllerEndpoint)
     * @see #getModelAsJsonResult
     * @see #getFileResult
     * @see #deductScriptSourceFileName(ControllerEndpoint)
     */
    protected PageModelMap getControllerEndpointResult(Long organizationId,
                                                       String frontendResourceUrl,
                                                       String subPath,
                                                       FrontendResource frontendResource,
                                                       ControllerEndpoint cEndpoint,
                                                       ControllerEndpoint.HttpMethod httpMethod,
                                                       boolean preview,
                                                       Map<String,String> requestParams,
                                                       AbstractOrganizationRelatedEntityForm form) {
        debug("[getControllerEndpointResult] organizationId: {} frontendResourceUrl: {} subPath: {}", organizationId, frontendResourceUrl, subPath);
        long userId = UserProvider.getUserIdOrNotExistingId();
        String scriptSourceFileName = deductScriptSourceFileName(cEndpoint);
        return Flow.init()
                .thenSet(controllerEndpoint, a -> cEndpoint)
                .thenSet(frontendResourceEntity, a -> frontendResource)
                .thenSet(uiComponentModel, a -> a.result != null ? (preview ? jsFlowRunner.runPreviewFlow(cEndpoint.getCode(), requestParams, organizationId, userId, form, scriptSourceFileName) : jsFlowRunner.runLiveFlow(cEndpoint.getCode(), requestParams, organizationId, userId, form, scriptSourceFileName)) : new PageModelMap())
                .thenSet(httpHeaders, a -> setupHttpHeaders(cEndpoint))
                .thenSet(controllerEndpointResult, a -> {
                    switch (cEndpoint.getResponseType()) {
                        case MODEL_AS_JSON -> {
                            return getModelAsJsonResult(a.model.get(uiComponentModel), a.result, cEndpoint);
                        }
                        case FILE -> {
                            InputStreamResource file = getFileResult(a.model.get(uiComponentModel), a.result, cEndpoint);
                            if (file != null) return file;
                        }
                        case STREAM -> {
                            InputStream stream = (InputStream) a.model.get(uiComponentModel).get(cEndpoint.getPageAttributesNames()[0]);
                            return new InputStreamResource(stream);
                        }
                    }
                    return null;
                })
                .execute();
    }

    /**
     * Sets up HTTP response headers from {@link ControllerEndpoint} configuration.
     * <p>
     * This private method creates a new {@link HttpHeaders} object and populates it by iterating over
     * the ControllerEndpoint's configured HTTP headers map. Each key-value pair from
     * {@link ControllerEndpoint#getHttpHeadersMap()} is added to the response headers.
     * </p>
     *
     * @param controllerEndpoint {@link ControllerEndpoint} entity containing httpHeadersMap configuration
     *                           with custom HTTP headers to include in the response.
     * @return {@link HttpHeaders} populated with all entries from controllerEndpoint.getHttpHeadersMap().
     *         Returns empty HttpHeaders if the map is empty.
     */
    private HttpHeaders setupHttpHeaders(ControllerEndpoint controllerEndpoint) {
        debug("[setupHttpHeaders]");
        HttpHeaders responseHeaders = new HttpHeaders();
        for (Map.Entry<String, String> httpHeader : controllerEndpoint.getHttpHeadersMap().entrySet()) {
            responseHeaders.add(httpHeader.getKey(), httpHeader.getValue());
        }
        return responseHeaders;
    }

    /**
     * Converts {@link PageModelMap} result model from JavaScript execution to JSON string response.
     * <p>
     * This private method serializes the PageModelMap returned from JavaScript flow execution into a
     * JSON string. It sets the Content-Type header to {@code application/json;charset=UTF-8} and performs
     * conditional serialization based on ControllerEndpoint configuration.
     * </p>
     * <p>
     * <b>Conditional Serialization Logic:</b><br>
     * If {@link ControllerEndpoint#getModelAttributes()} is not blank, serializes only the selected PageAttr
     * keys specified in {@link ControllerEndpoint#getPageAttributesNames()}. Each attribute name is converted
     * to a {@link PageAttr} enum (or creates a new PageAttr if not found), and the resulting array is passed to
     * {@link PageModelMap#getAsMap(PageAttr...)} to extract only those attributes.
     * </p>
     * <p>
     * If modelAttributes is blank, serializes only the {@code isError} attribute from the model.
     * </p>
     * <p>
     * Serialization is performed via {@link JsonHelper#to(Object)}.
     * </p>
     *
     * @param resultModel {@link PageModelMap} from JavaScript flow execution containing page attributes
     *                    to serialize (e.g., data, isError, message).
     * @param httpHeaders {@link HttpHeaders} to configure. Content-Type is set to application/json;charset=UTF-8.
     * @param controllerEndpoint {@link ControllerEndpoint} with modelAttributes configuration. If modelAttributes
     *                           is not blank, only the specified page attributes are serialized.
     * @return String JSON representation of the selected page attributes or isError attribute. Never null.
     * @see JsonHelper#to(Object)
     * @see PageModelMap#getAsMap(PageAttr...)
     */
    private String getModelAsJsonResult(PageModelMap resultModel, HttpHeaders httpHeaders, ControllerEndpoint controllerEndpoint) {
        debug("[getModelAsJsonResult]");
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
        return JsonHelper.to(StringUtils.isNotBlank(controllerEndpoint.getModelAttributes()) ?
                resultModel.getAsMap(Arrays.stream(controllerEndpoint.getPageAttributesNames())
                        .map(attr -> PageAttr.getByName(attr) != null ?
                                PageAttr.getByName(attr) : new PageAttr(attr)).toArray(PageAttr[]::new))
                : resultModel.getAsMap(isError));
    }

    /**
     * Creates {@link InputStreamResource} from {@link File} entity for streaming file content in response.
     * <p>
     * This private method extracts a File object from the JavaScript execution result model using the first
     * page attribute name from {@link ControllerEndpoint#getPageAttributesNames()}, configures HTTP headers
     * via {@link #setupFileResultHeaders(HttpHeaders, File)}, and creates an InputStreamResource wrapping
     * the file's content stream for streaming to the client.
     * </p>
     * <p>
     * <b>Exception Handling:</b><br>
     * Catches {@link IOException} and {@link SQLException} from {@link File#getContentStream()} and prints
     * stack trace. Returns null on exception, which should be handled by the caller to provide appropriate
     * error response to the client.
     * </p>
     *
     * @param resultModel {@link PageModelMap} from JavaScript flow execution. Must contain a {@link File}
     *                    object at the key specified by controllerEndpoint.getPageAttributesNames()[0].
     * @param httpHeaders {@link HttpHeaders} to configure with file-specific headers (Content-Type,
     *                    Accept-Ranges, Cache-Control, Content-Disposition).
     * @param controllerEndpoint {@link ControllerEndpoint} with pageAttributesNames configuration. The first
     *                           name is used as the key to extract the File object from the result model.
     * @return {@link InputStreamResource} wrapping the file content stream for response body, or null if
     *         IOException or SQLException occurs when opening the file content stream.
     * @see #setupFileResultHeaders(HttpHeaders, File)
     * @see File#getContentStream()
     */
    private InputStreamResource getFileResult(PageModelMap resultModel, HttpHeaders httpHeaders, ControllerEndpoint controllerEndpoint) {
        debug("[getFileResult]");
        File file = (File) (resultModel.get(controllerEndpoint.getPageAttributesNames()[0]));
        setupFileResultHeaders(httpHeaders, file);
        try {
            return new InputStreamResource(file.getContentStream());
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Configures {@link HttpHeaders} for File response type {@link ControllerEndpoint} with appropriate content headers.
     * <p>
     * This private method populates HTTP headers required for file streaming responses. Each header is only
     * set if not already present (checked via {@code !httpHeaders.containsKey()}) to preserve any custom
     * headers configured in the ControllerEndpoint.
     * </p>
     * <p>
     * <b>Configured Headers:</b>
     * <ul>
     *   <li><b>Content-Type:</b> Set to {@link File#getContentType()} or defaults to {@link MediaType#IMAGE_PNG_VALUE}
     *       if file content type is null. Indicates MIME type of the file content.</li>
     *   <li><b>Accept-Ranges:</b> Set to {@code "bytes"} to enable partial content support (HTTP 206 responses)
     *       for large file streaming and resume capabilities.</li>
     *   <li><b>Cache-Control:</b> Set to {@code "max-age=604800, public"} for 7-day browser caching
     *       (604800 seconds = 7 days) with public caching allowed.</li>
     *   <li><b>Content-Disposition:</b> Set to {@code "inline; filename=\"<filename>\""} to display file
     *       in browser with filename hint. Uses {@link File#getFilename()} for the filename value.</li>
     * </ul>
     * </p>
     *
     * @param httpHeaders {@link HttpHeaders} to configure. Only headers not already present are added.
     * @param file {@link File} entity with content metadata (contentType, filename) used for header values.
     */
    private void setupFileResultHeaders(HttpHeaders httpHeaders, File file) {
        debug("[setupFileResultHeaders]");
        if(!httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)){
            httpHeaders.add(HttpHeaders.CONTENT_TYPE,
                    file.getContentType() != null ? file.getContentType() : MediaType.IMAGE_PNG_VALUE);
        }
        if(!httpHeaders.containsKey(HttpHeaders.ACCEPT_RANGES)){
            httpHeaders.add(HttpHeaders.ACCEPT_RANGES, "bytes");
        }
        if(!httpHeaders.containsKey(HttpHeaders.CACHE_CONTROL)){
            httpHeaders.add(HttpHeaders.CACHE_CONTROL, "max-age=604800, public");
        }
        if(!httpHeaders.containsKey(HttpHeaders.CONTENT_DISPOSITION)){
            httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"");
        }
    }

    /**
     * Deduces the script source filename for {@link ControllerEndpoint} JavaScript flow execution.
     * <p>
     * This private method generates a standardized filename using the naming convention
     * {@code controllerEndpoint-<id>.mjs} where {@code <id>} is the ControllerEndpoint entity's primary key.
     * </p>
     * <p>
     * <b>Purpose:</b><br>
     * The generated filename is used by {@link JsFlowRunner} for script source mapping and debugging in the
     * GraalVM context. This naming convention enables clear identification of script sources in stack traces
     * and debugging output, mapping each executed script back to its ControllerEndpoint entity.
     * </p>
     * <p>
     * <b>File Extension:</b><br>
     * The {@code .mjs} extension indicates ES6 module JavaScript, signaling to the GraalVM engine that the
     * script uses ECMAScript module syntax with import/export statements.
     * </p>
     * <p>
     * Example: For ControllerEndpoint with id=123, returns {@code "controllerEndpoint-123.mjs"}.
     * </p>
     *
     * @param ce {@link ControllerEndpoint} entity. Must not be null and must have a non-null ID.
     * @return String filename in the format {@code "controllerEndpoint-<id>.mjs"}. Never null.
     */
    @NotNull
    private String deductScriptSourceFileName(ControllerEndpoint ce) {
        return "controllerEndpoint-" + ce.getId() + ".mjs";
    }

}
