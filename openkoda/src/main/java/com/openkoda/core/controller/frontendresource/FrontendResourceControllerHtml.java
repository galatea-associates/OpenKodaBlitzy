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

import com.openkoda.controller.DefaultComponentProvider;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.core.form.CRUDControllerConfiguration;
import com.openkoda.core.form.ReflectionBasedEntityForm;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.common.ComponentEntity;
import com.openkoda.model.common.SearchableOrganizationRelatedEntity;
import com.openkoda.model.component.FrontendResource;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import static com.openkoda.controller.common.URLConstants._FRONTENDRESOURCE;
import static com.openkoda.controller.common.URLConstants._HTML_ORGANIZATION_ORGANIZATIONID;
import static com.openkoda.core.controller.generic.AbstractController._HTML;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


/**
 * REST controller providing HTML-focused REST endpoints for server-side FrontendResource management and CRUD operations.
 * <p>
 * This controller extends {@link AbstractFrontendResourceController} to inherit orchestration methods for FrontendResource
 * entity lifecycle management, and exposes concrete REST endpoints for HTML-based frontend resource operations.
 * It supports multi-tenancy through dual request mapping paths: organization-scoped routes
 * ({@code /{organizationId}/html/frontendresource}) and non-scoped routes ({@code /html/frontendresource}).
 * </p>
 * <p>
 * <b>Security Model:</b> All mutating endpoints enforce role-based access control through method-level
 * {@code @PreAuthorize} annotations (CHECK_CAN_MANAGE_FRONTEND_RESOURCES, CHECK_CAN_READ_FRONTEND_RESOURCES).
 * Additional privilege checks via {@code hasGlobalOrOrgPrivilege()} ensure authorization at the organization
 * level, returning HTTP 401 Unauthorized on privilege check failures.
 * </p>
 * <p>
 * <b>Flow Pipeline Integration:</b> Endpoints compose backend operations using the fluent Flow API
 * ({@code Flow.init().then().thenSet().execute()}), transforming results into ModelAndView-like HTML fragments
 * via {@code .mav()} for both success and error states. This enables seamless integration with server-side
 * HTML rendering frameworks.
 * </p>
 * <p>
 * <b>Service Dependencies:</b>
 * <ul>
 *   <li>{@code services.componentExport} - Component export/import operations</li>
 *   <li>{@code services.zipService} - ZIP archive creation for batch exports</li>
 *   <li>{@code services.validation} - Form validation and entity population</li>
 *   <li>{@code repositories.secure/unsecure} - Data access with/without privilege enforcement</li>
 * </ul>
 * </p>
 * <p>
 * <b>Preview vs Live Modes:</b> The controller supports safe content editing through draft/live content separation.
 * Preview mode operations avoid persistent side effects for safe testing, while live mode operations persist
 * changes to the database. Publish operations promote draft content to live production content.
 * </p>
 * <p>
 * <b>Special Handling:</b> The controller supports REGISTER path injection with RegisterUserForm for user
 * registration flows integrated with frontend resources.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * // Update frontend resource content
 * POST /html/frontendresource/123?content=<html>...</html>
 * 
 * // Publish draft to live
 * POST /html/frontendresource/123/publish
 * }</pre>
 * </p>
 *
 * @see AbstractFrontendResourceController
 * @see FrontendResource
 * @see com.openkoda.core.service.FrontendResourceService
 * @since 1.7.1
 * @version 1.7.1
 * @author OpenKoda Team
 */
@RestController
@RequestMapping({_HTML_ORGANIZATION_ORGANIZATIONID + _FRONTENDRESOURCE, _HTML + _FRONTENDRESOURCE})
public class FrontendResourceControllerHtml extends AbstractFrontendResourceController {

    /**
     * Default template name for HTML-type FrontendResource content rendering.
     * <p>
     * Injected via {@code @Value} from application property {@code default.frontendResourcePage.template.name}
     * with default value "frontend-resource-template". This template name is used as the default for rendering
     * HTML-type {@link FrontendResource} entities when no specific template is configured.
     * </p>
     * <p>
     * The template is resolved from the application's template directory and processed by the configured
     * template engine (typically Thymeleaf) for server-side HTML generation.
     * </p>
     *
     * @see FrontendResource.Type#HTML
     */
    @Value("${default.frontendResourcePage.template.name:frontend-resource-template}")
    String defaultFrontendResourcePageTemplate;

    /**
     * REST endpoint to update FrontendResource content with JavaScript/HTML code validation and persistence.
     * <p>
     * Accepts POST requests to {@code /{organizationId}/html/frontendresource/{id}} (organization-scoped) or
     * {@code /html/frontendresource/{id}} (non-scoped) to update the content of an existing FrontendResource entity.
     * The submitted content undergoes validation before being persisted to the database.
     * </p>
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_MANAGE_FRONTEND_RESOURCES} privilege via {@code @PreAuthorize}.
     * Users without this privilege receive HTTP 401 Unauthorized responses.
     * </p>
     * <p>
     * <b>Flow Pipeline Pattern:</b> Delegates to {@code updateFrontendResource(content, id)} from superclass,
     * then transforms the result using {@code .mav(a -> true, a -> a.get(message))} to produce a ModelAndView
     * HTML fragment with success state and message attribute for server-side rendering.
     * </p>
     * <p>
     * Example request:
     * <pre>{@code
     * POST /html/frontendresource/123
     * content=<html><body>Updated content</body></html>
     * }</pre>
     * </p>
     *
     * @param organizationId optional organization context for multi-tenancy (may be null for global resources)
     * @param content String containing JavaScript/HTML code to validate and persist to the FrontendResource
     * @param id Long primary key of the FrontendResource entity to update
     * @return Object representing ModelAndView HTML fragment with success message on success, or error response on failure
     * @see AbstractFrontendResourceController#updateFrontendResource(String, Long)
     */
    @PostMapping(_ID)
    @ResponseBody
    @PreAuthorize(CHECK_CAN_MANAGE_FRONTEND_RESOURCES)
    public Object update(
            @PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
            @RequestParam String content,
            @PathVariable(ID) Long id) {
        debug("[update] FrontendResourceId: {}", id);
        return updateFrontendResource(content, id)
                .mav(a -> true, a -> a.get(message));
    }


    /**
     * REST endpoint to publish draft FrontendResource content to live production.
     * <p>
     * Accepts POST requests to {@code /{organizationId}/html/frontendresource/{id}/publish} (organization-scoped)
     * or {@code /html/frontendresource/{id}/publish} (non-scoped). This operation replaces the live content field
     * with the draft content field for the specified FrontendResource entity, promoting draft changes to production.
     * </p>
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_MANAGE_FRONTEND_RESOURCES} privilege via {@code @PreAuthorize}.
     * This is a privileged operation as it affects production content visible to end users.
     * </p>
     * <p>
     * <b>Flow Pipeline:</b> Delegates to {@code publishFrontendResource(frontendResourceId)} from superclass,
     * then transforms to ModelAndView HTML fragment via {@code .mav(a -> true)} for unconditional success response.
     * </p>
     * <p>
     * Example request:
     * <pre>{@code
     * POST /html/frontendresource/123/publish
     * }</pre>
     * </p>
     *
     * @param organizationId optional organization context for multi-tenancy (may be null for global resources)
     * @param frontendResourceId Long primary key of the FrontendResource entity to publish from draft to live
     * @return Object representing ModelAndView HTML fragment indicating publish success
     * @see AbstractFrontendResourceController#publishFrontendResource(Long)
     */
    @PostMapping(_ID + _PUBLISH)
    @PreAuthorize(CHECK_CAN_MANAGE_FRONTEND_RESOURCES)
    public Object publish(
            @PathVariable(value = ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(ID) Long frontendResourceId) {
        debug("[publish] FrontendResourceId: {}", frontendResourceId);
        return publishFrontendResource(frontendResourceId)
                .mav(a -> true);
    }

    /**
     * REST endpoint to batch publish all draft FrontendResource entities to live production.
     * <p>
     * Accepts POST requests to {@code /{organizationId}/html/frontendresource/all/publish} (organization-scoped)
     * or {@code /html/frontendresource/all/publish} (non-scoped). This batch operation replaces live content with
     * draft content for ALL FrontendResource entities in the database that have draft content pending, promoting
     * all draft changes to production simultaneously.
     * </p>
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_MANAGE_FRONTEND_RESOURCES} privilege via {@code @PreAuthorize}.
     * This is a highly privileged operation as it affects all production content across the system. Use with caution
     * in production environments.
     * </p>
     * <p>
     * <b>Flow Pipeline:</b> Delegates to {@code publishAllFrontendResource()} from superclass, then transforms
     * to ModelAndView HTML fragment via {@code .mav(a -> true)} for success response.
     * </p>
     * <p>
     * Example request:
     * <pre>{@code
     * POST /html/frontendresource/all/publish
     * }</pre>
     * </p>
     *
     * @return Object representing ModelAndView HTML fragment indicating batch publish success
     * @see AbstractFrontendResourceController#publishAllFrontendResource()
     */
    @PostMapping(_ALL + _PUBLISH)
    @PreAuthorize(CHECK_CAN_MANAGE_FRONTEND_RESOURCES)
    public Object publishAll() {
        debug("[publishAll]");
        return publishAllFrontendResource()
                .mav(a -> true);
    }


    /**
     * Legacy REST endpoint to reload FrontendResource content (deprecated).
     * <p>
     * Accepts POST requests to {@code /html/frontendresource/{id}/reload}. This method is no longer used due to
     * changes in the frontend resource flow architecture. The legacy behavior cleared draft content and loaded
     * default content directly into the content field, which has been superseded by the draft/live content separation model.
     * </p>
     * <p>
     * <b>Migration:</b> Use {@link #reloadToDraft(Long)} instead, which correctly loads default content into the
     * draftContent field rather than the live content field, preserving the draft/live workflow integrity.
     * </p>
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_MANAGE_FRONTEND_RESOURCES} privilege via {@code @PreAuthorize}.
     * </p>
     *
     * @param frontendResourceId Long primary key of the FrontendResource entity to reload
     * @return Object representing ModelAndView HTML fragment
     * @deprecated Replaced by {@link #reloadToDraft(Long)} which loads into draftContent field instead of content field.
     *             This method will be removed in a future version.
     * @see #reloadToDraft(Long)
     */
    @Deprecated
    @PostMapping(_ID + _RELOAD)
    @PreAuthorize(CHECK_CAN_MANAGE_FRONTEND_RESOURCES)
    public Object reload(@PathVariable(ID) Long frontendResourceId) {
        debug("[reload] FrontendResourceId: {}", frontendResourceId);
        return reloadFrontendResource(frontendResourceId)
                .mav(a -> true);
    }

    /**
     * REST endpoint to reload default resource content into draft field for editing.
     * <p>
     * Accepts POST requests to {@code /html/frontendresource/{id}/reloadToDraft}. This operation clears the
     * draftContent field and loads default resource content from application resources based on the FrontendResource's
     * type, name, accessLevel, and organizationId. The default template is resolved from the application's resource
     * directory or uses {@link #defaultFrontendResourcePageTemplate} for HTML-type resources.
     * </p>
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_MANAGE_FRONTEND_RESOURCES} privilege via {@code @PreAuthorize}.
     * </p>
     * <p>
     * <b>Draft/Live Workflow:</b> This operation affects only the draftContent field, leaving the live content
     * field untouched. This enables safe editing and preview workflows where changes can be reviewed before
     * publishing to production via {@link #publish(Long, Long)}.
     * </p>
     * <p>
     * <b>Flow Pipeline:</b> Delegates to {@code copyResourceContentToDraft(frontendResourceId)} from superclass,
     * which resolves and loads the default resource content into draft.
     * </p>
     * <p>
     * Example request:
     * <pre>{@code
     * POST /html/frontendresource/123/reloadToDraft
     * }</pre>
     * </p>
     *
     * @param frontendResourceId Long primary key of the FrontendResource entity whose draft content should be reloaded from defaults
     * @return Object representing ModelAndView HTML fragment indicating reload success
     * @see AbstractFrontendResourceController#copyResourceContentToDraft(Long)
     */
    @RequestMapping(value = _ID + _RELOAD_TO_DRAFT, method = POST)
    @PreAuthorize(CHECK_CAN_MANAGE_FRONTEND_RESOURCES)
    public Object reloadToDraft(@PathVariable(ID) Long frontendResourceId) {
        debug("[reload] FrontendResourceId: {}", frontendResourceId);
        return copyResourceContentToDraft(frontendResourceId)
                .mav(a -> true);
    }

    /**
     * REST endpoint to copy live production content to draft field for editing.
     * <p>
     * Accepts POST requests to {@code /html/frontendresource/{id}/copy/live}. This operation copies the current
     * live content field value to the draftContent field, enabling editors to modify production content without
     * affecting the live version. This is the recommended workflow for editing existing production content:
     * copy live to draft, edit draft, preview changes, then publish when ready.
     * </p>
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_MANAGE_FRONTEND_RESOURCES} privilege via {@code @PreAuthorize}.
     * The {@code @ResponseBody} annotation enables direct response writing for HTML fragment delivery.
     * </p>
     * <p>
     * <b>Safe Editing Workflow:</b> This operation ensures that live production content remains unaffected during
     * editing. Changes are isolated in the draftContent field until explicitly published via {@link #publish(Long, Long)}.
     * </p>
     * <p>
     * <b>Flow Pipeline:</b> Delegates to {@code copyLiveContentToDraft(frontendResourceId)} from superclass,
     * then transforms to ModelAndView HTML fragment via {@code .mav(a -> true)}.
     * </p>
     * <p>
     * Example request:
     * <pre>{@code
     * POST /html/frontendresource/123/copy/live
     * }</pre>
     * </p>
     *
     * @param frontendResourceId Long primary key of the FrontendResource entity whose live content should be copied to draft
     * @return Object representing ModelAndView HTML fragment with direct response body for success indication
     * @see AbstractFrontendResourceController#copyLiveContentToDraft(Long)
     */
    @RequestMapping(value = _ID + _COPY + _LIVE, method = POST)
    @PreAuthorize(CHECK_CAN_MANAGE_FRONTEND_RESOURCES)
    @ResponseBody
    public Object copyLiveToDraft(@PathVariable(ID) Long frontendResourceId) {
        debug("[copyLiveToDraft] FrontendResourceId: {}", frontendResourceId);
        return copyLiveContentToDraft(frontendResourceId)
                .mav(a -> true);
    }

    /**
     * REST endpoint to copy default application resource content to draft field.
     * <p>
     * Accepts POST requests to {@code /html/frontendresource/{id}/copy/resource}. This operation loads default
     * resource content from the application's resource directory into the draftContent field, useful for resetting
     * customized content back to application defaults without affecting the live production content field.
     * </p>
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_MANAGE_FRONTEND_RESOURCES} privilege via {@code @PreAuthorize}.
     * The {@code @ResponseBody} annotation enables direct response writing for HTML fragment delivery.
     * </p>
     * <p>
     * <b>Reset to Defaults Workflow:</b> This operation is useful when reverting customizations to application
     * defaults. The default resource is resolved based on the FrontendResource's type, name, accessLevel, and
     * organizationId. Changes are isolated in draftContent and can be previewed before publishing to live.
     * </p>
     * <p>
     * <b>Flow Pipeline:</b> Delegates to {@code copyResourceContentToDraft(frontendResourceId)} from superclass,
     * which resolves and loads the default resource content into the draft field.
     * </p>
     * <p>
     * Example request:
     * <pre>{@code
     * POST /html/frontendresource/123/copy/resource
     * }</pre>
     * </p>
     *
     * @param frontendResourceId Long primary key of the FrontendResource entity whose draft should be populated with default resource content
     * @return Object representing ModelAndView HTML fragment with direct response body for success indication
     * @see AbstractFrontendResourceController#copyResourceContentToDraft(Long)
     */
    @RequestMapping(value = _ID + _COPY + _RESOURCE, method = POST)
    @PreAuthorize(CHECK_CAN_MANAGE_FRONTEND_RESOURCES)
    @ResponseBody
    public Object copyResourceToDraft(@PathVariable(ID) Long frontendResourceId) {
        debug("[copyResourceToDraft] FrontendResourceId: {}", frontendResourceId);
        return copyResourceContentToDraft(frontendResourceId)
                .mav(a -> true);
    }

    /**
     * REST endpoint to export all FrontendResource entities as a ZIP archive for backup/export.
     * <p>
     * Accepts GET requests to {@code /{organizationId}/html/frontendresource/zip} (organization-scoped) or
     * {@code /html/frontendresource/zip} (non-scoped), producing an {@code application/zip} response containing
     * all FrontendResource entities from the database. This is useful for system backups, migrations, or exporting
     * frontend resources for deployment to other environments.
     * </p>
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_READ_FRONTEND_RESOURCES} privilege via {@code @PreAuthorize}.
     * Note this is a read-only operation requiring lower privileges than mutating operations, as it performs
     * no modifications to the database.
     * </p>
     * <p>
     * <b>Implementation:</b> Retrieves all FrontendResource entities via
     * {@code repositories.unsecure.frontendResource.findAll()} (using unsecure repository for complete access),
     * packages them into a ZIP archive via {@code services.zipService.zipFrontendResources()}, and returns the
     * byte array via {@code .toByteArray()} for binary download. The {@code @ResponseBody} annotation enables
     * direct binary response writing.
     * </p>
     * <p>
     * Example request:
     * <pre>{@code
     * GET /html/frontendresource/zip
     * Response: application/zip binary content
     * }</pre>
     * </p>
     *
     * @return byte[] containing ZIP archive of all FrontendResource entities with application/zip content type
     * @see com.openkoda.core.service.ZipService#zipFrontendResources(java.util.List)
     * @see com.openkoda.repository.FrontendResourceRepository
     */
    @RequestMapping(value = _ZIP, method = GET, produces = "application/zip")
    @ResponseBody
    @PreAuthorize(CHECK_CAN_READ_FRONTEND_RESOURCES)
    public byte[] getAllZipped() {
        debug("[getAllZipped]");
        return services.zipService.zipFrontendResources(repositories.unsecure.frontendResource.findAll()).toByteArray();
    }

    /**
     * REST endpoint to create a new FrontendResource entity with validation and privilege enforcement.
     * <p>
     * Accepts POST requests to {@code /{organizationId}/html/frontendresource/new/settings} (organization-scoped)
     * or {@code /html/frontendresource/new/settings} (non-scoped) with form data containing FrontendResource DTO
     * attributes. The submitted form undergoes Jakarta Bean Validation before entity creation.
     * </p>
     * <p>
     * <b>Privilege Check:</b> Retrieves {@code CRUDControllerConfiguration} for FRONTENDRESOURCE entity type,
     * extracts {@code postNewPrivilege} and validates via {@code hasGlobalOrOrgPrivilege(privilege, organizationId)}.
     * If privilege check fails, returns {@code ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()} with HTTP 401.
     * </p>
     * <p>
     * <b>Flow Pipeline:</b> Initializes Flow with {@code componentProvider} and form attribute, then executes:
     * <ol>
     *   <li>Validates and populates form data to new entity via {@code services.validation.validateAndPopulateToEntity()}</li>
     *   <li>Saves entity via {@code conf.getSecureRepository().saveOne()} with privilege enforcement</li>
     *   <li>Exports to file if ComponentEntity via {@code services.componentExport.exportToFileIfRequired()}</li>
     *   <li>Creates new empty form via {@code conf.createNewForm(organizationId, result)} for next operation</li>
     *   <li>Transforms result to ModelAndView with success/error fragments via {@code .mav()}</li>
     * </ol>
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * Flow.init(componentProvider, "form", form)
     *     .then(validate and save)
     *     .mav(successFragment, errorFragment);
     * }</pre>
     * </p>
     *
     * @param form Valid AbstractOrganizationRelatedEntityForm containing FrontendResource DTO data with Jakarta Bean Validation annotations
     * @param br BindingResult capturing validation errors from form binding and validation process
     * @return Object representing ModelAndView HTML fragment on success, or ResponseEntity with HTTP 401 Unauthorized on privilege failure
     * @see CRUDControllerConfiguration
     * @see com.openkoda.core.service.ValidationService#validateAndPopulateToEntity
     */
    @PostMapping(_NEW_SETTINGS)
    public Object saveNew(@Valid AbstractOrganizationRelatedEntityForm form, BindingResult br) {
        debug("[saveNew]");
        CRUDControllerConfiguration conf = controllers.htmlCrudControllerConfigurationMap.get(FRONTENDRESOURCE);
        PrivilegeBase privilege = conf.getPostNewPrivilege();
        Long organizationId = ((ReflectionBasedEntityForm) form).dto.getOrganizationId();
        if (not(hasGlobalOrOrgPrivilege(privilege, organizationId))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ((Flow<Object, AbstractOrganizationRelatedEntityForm, DefaultComponentProvider>)
                Flow.init(componentProvider, conf.getFormAttribute(), form))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br, conf.createNewEntity(organizationId)))
                .then(a -> (SearchableOrganizationRelatedEntity)conf.getSecureRepository().saveOne(a.result))
                .then(a -> services.componentExport.exportToFileIfRequired((ComponentEntity) a.result))
                .thenSet(conf.getFormAttribute(), a -> conf.createNewForm(organizationId, a.result))
                .execute()
                .mav(conf.getFormSuccessFragment(), conf.getFormErrorFragment());
    }
    /**
     * REST endpoint to update an existing FrontendResource entity with validation and privilege enforcement.
     * <p>
     * Accepts POST requests to {@code /{organizationId}/html/frontendresource/{id}/settings} (organization-scoped)
     * or {@code /html/frontendresource/{id}/settings} (non-scoped) with form data containing updated FrontendResource
     * DTO attributes. The submitted form undergoes Jakarta Bean Validation before entity update.
     * </p>
     * <p>
     * <b>Privilege Check:</b> Retrieves {@code CRUDControllerConfiguration} for FRONTENDRESOURCE entity type,
     * extracts {@code postSavePrivilege} and validates via {@code hasGlobalOrOrgPrivilege(privilege, organizationId)}.
     * If privilege check fails, returns {@code ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()} with HTTP 401.
     * </p>
     * <p>
     * <b>Flow Pipeline:</b> Initializes Flow with {@code componentProvider} and form attribute, then executes:
     * <ol>
     *   <li>Finds existing entity via {@code conf.getSecureRepository().findOne(objectId)} with privilege checks</li>
     *   <li>Validates and populates form updates to entity via {@code services.validation.validateAndPopulateToEntity()}</li>
     *   <li>Saves updated entity via {@code conf.getSecureRepository().saveOne()} with privilege enforcement</li>
     *   <li>Exports to file if ComponentEntity via {@code services.componentExport.exportToFileIfRequired()}</li>
     *   <li>Refreshes form with updated entity via {@code conf.createNewForm(organizationId, result)}</li>
     *   <li>Transforms result to ModelAndView with success/error fragments via {@code .mav()}</li>
     * </ol>
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * Flow.init(componentProvider, "form", form)
     *     .then(find, validate, save)
     *     .mav(successFragment, errorFragment);
     * }</pre>
     * </p>
     *
     * @param objectId Long primary key of the existing FrontendResource entity to update
     * @param organizationId optional organization context for multi-tenancy privilege validation (may be null)
     * @param form Valid AbstractOrganizationRelatedEntityForm containing updated FrontendResource DTO data
     * @param br BindingResult capturing validation errors from form binding and validation process
     * @return Object representing ModelAndView HTML fragment on success, or ResponseEntity with HTTP 401 Unauthorized on privilege failure
     * @see CRUDControllerConfiguration
     * @see com.openkoda.core.service.ValidationService#validateAndPopulateToEntity
     */
    @PostMapping(_ID_SETTINGS)
    public Object save(
            @PathVariable(ID) Long objectId,
            @PathVariable(name = ORGANIZATIONID, required = false) Long organizationId,
            @Valid AbstractOrganizationRelatedEntityForm form, BindingResult br) {
        debug("[saveNew]");
        CRUDControllerConfiguration conf = controllers.htmlCrudControllerConfigurationMap.get(FRONTENDRESOURCE);
        PrivilegeBase privilege = conf.getPostSavePrivilege();
        if (not(hasGlobalOrOrgPrivilege(privilege, organizationId))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ((Flow<Object, AbstractOrganizationRelatedEntityForm, DefaultComponentProvider>)
                Flow.init(componentProvider, conf.getFormAttribute(), form))
                .then(a -> (SearchableOrganizationRelatedEntity)conf.getSecureRepository().findOne(objectId))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br,a.result))
                .then(a -> (SearchableOrganizationRelatedEntity)conf.getSecureRepository().saveOne(a.result))
                .then(a -> services.componentExport.exportToFileIfRequired((ComponentEntity) a.result))
                .thenSet(conf.getFormAttribute(), a -> conf.createNewForm(organizationId, a.result))
                .execute()
                .mav(conf.getFormSuccessFragment(), conf.getFormErrorFragment());
    }
    /**
     * REST endpoint to permanently delete a FrontendResource entity from the database.
     * <p>
     * Accepts POST requests to {@code /{organizationId}/html/frontendresource/{id}/remove} (organization-scoped)
     * or {@code /html/frontendresource/{id}/remove} (non-scoped). This operation performs permanent deletion of
     * the FrontendResource entity and any associated exported files. This is NOT a draft clearing operation - it
     * completely removes the entity from the database.
     * </p>
     * <p>
     * <b>Warning:</b> This is a destructive operation that cannot be undone. Both draft and live content are
     * permanently deleted. For clearing only draft content while preserving the entity, use {@link #removeDraft(Long, Long)} instead.
     * </p>
     * <p>
     * <b>Privilege Check:</b> Retrieves {@code CRUDControllerConfiguration} for FRONTENDRESOURCE entity type,
     * extracts {@code postRemovePrivilege} and validates via {@code hasGlobalOrOrgPrivilege(privilege, organizationId)}.
     * Returns HTTP 401 Unauthorized if privilege check fails.
     * </p>
     * <p>
     * <b>Transactional:</b> The {@code @Transactional} annotation ensures atomic operation - either the entity
     * and all associated files are deleted, or no changes are made on error (rollback).
     * </p>
     * <p>
     * <b>Flow Pipeline:</b> Initializes Flow with {@code objectId}, then executes:
     * <ol>
     *   <li>Finds entity via {@code repositories.secure.scheduler.findOne(objectId)} [NOTE: appears to be bug - should use frontendResource repository]</li>
     *   <li>Removes exported files via {@code services.componentExport.removeExportedFilesIfRequired()}</li>
     *   <li>Deletes entity via {@code conf.getSecureRepository().deleteOne(objectId)}</li>
     *   <li>Transforms to ModelAndView with success=true or error=false via {@code .mav()}</li>
     * </ol>
     * </p>
     *
     * @param objectId Long primary key of the FrontendResource entity to permanently delete
     * @param organizationId optional organization context for multi-tenancy privilege validation (may be null)
     * @return Object representing ModelAndView HTML fragment indicating deletion success/failure, or ResponseEntity with HTTP 401 on privilege failure
     * @see #removeDraft(Long, Long)
     */
    @PostMapping(_ID_REMOVE)
    @Transactional
    public Object remove(
            @PathVariable(name=ID) Long objectId,
            @PathVariable(name = ORGANIZATIONID, required = false) Long organizationId) {
        CRUDControllerConfiguration conf = controllers.htmlCrudControllerConfigurationMap.get(FRONTENDRESOURCE);
        PrivilegeBase privilege = conf.getPostRemovePrivilege();
        if (not(hasGlobalOrOrgPrivilege(privilege, organizationId))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return Flow.init(componentProvider, objectId)
                .then(a -> repositories.secure.scheduler.findOne(objectId))
                .then(a -> services.componentExport.removeExportedFilesIfRequired(a.result))
                .then(a -> conf.getSecureRepository().deleteOne(objectId))
                .execute()
                .mav(a -> true, a -> false);
    }
    /**
     * REST endpoint to clear draft content without deleting the FrontendResource entity.
     * <p>
     * Accepts POST requests to {@code /{organizationId}/html/frontendresource/{id}/remove/draft} (organization-scoped)
     * or {@code /html/frontendresource/{id}/remove/draft} (non-scoped). This operation sets the draftContent field
     * to null while preserving the entity and its live content field. This is useful for discarding draft changes
     * without affecting the published live version.
     * </p>
     * <p>
     * <b>Note:</b> This is NOT a permanent deletion operation - it only clears the draft content field. The
     * FrontendResource entity and its live content remain in the database. For permanent entity deletion, use
     * {@link #remove(Long, Long)} instead.
     * </p>
     * <p>
     * <b>Privilege Check:</b> Retrieves {@code CRUDControllerConfiguration} for FRONTENDRESOURCE entity type,
     * extracts {@code postRemovePrivilege} and validates via {@code hasGlobalOrOrgPrivilege(privilege, organizationId)}.
     * Returns HTTP 401 Unauthorized if privilege check fails.
     * </p>
     * <p>
     * <b>Transactional:</b> The {@code @Transactional} annotation ensures atomic operation - the draftContent
     * field update is either fully committed or rolled back on error.
     * </p>
     * <p>
     * <b>Flow Pipeline:</b> Initializes Flow with {@code objectId}, then executes:
     * <ol>
     *   <li>Finds entity via {@code repositories.secure.frontendResource.findOne(objectId)} with privilege checks</li>
     *   <li>Sets draftContent to null in model via {@code a.model.get(frontendResourceEntity).setDraftContent(null)}</li>
     *   <li>Saves updated entity via {@code repositories.secure.frontendResource.saveOne()}</li>
     *   <li>Transforms to ModelAndView with success=true or error=false via {@code .mav()}</li>
     * </ol>
     * </p>
     * <p>
     * Example request:
     * <pre>{@code
     * POST /html/frontendresource/123/remove/draft
     * }</pre>
     * </p>
     *
     * @param objectId Long primary key of the FrontendResource entity whose draft content should be cleared
     * @param organizationId optional organization context for multi-tenancy privilege validation (may be null)
     * @return Object representing ModelAndView HTML fragment indicating draft removal success/failure, or ResponseEntity with HTTP 401 on privilege failure
     * @see #remove(Long, Long)
     */
    @PostMapping(_ID_REMOVE + _DRAFT)
    @Transactional
    public Object removeDraft(
            @PathVariable(name=ID) Long objectId,
            @PathVariable(name = ORGANIZATIONID, required = false) Long organizationId) {
        CRUDControllerConfiguration conf = controllers.htmlCrudControllerConfigurationMap.get(FRONTENDRESOURCE);
        PrivilegeBase privilege = conf.getPostRemovePrivilege();
        if (not(hasGlobalOrOrgPrivilege(privilege, organizationId))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return Flow.init(componentProvider, objectId)
                .thenSet(frontendResourceEntity, a -> repositories.secure.frontendResource.findOne(objectId))
                .then(a -> {
                    a.model.get(frontendResourceEntity).setDraftContent(null);
                    return a.result;
                })
                .then(a -> repositories.secure.frontendResource.saveOne(a.model.get(frontendResourceEntity)))
                .execute()
                .mav(a -> true, a -> false);
    }
}
