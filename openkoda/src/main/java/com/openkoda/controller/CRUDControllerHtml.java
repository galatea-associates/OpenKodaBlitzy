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

package com.openkoda.controller;

import com.openkoda.controller.common.PageAttributes;
import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageAttr;
import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.core.form.CRUDControllerConfiguration;
import com.openkoda.core.form.ReflectionBasedEntityForm;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.core.security.OrganizationUser;
import com.openkoda.core.security.UserProvider;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.common.SearchableOrganizationRelatedEntity;
import com.openkoda.model.file.File;
import com.openkoda.service.dynamicentity.DynamicEntityRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.openkoda.controller.common.URLConstants._HTML;
import static com.openkoda.controller.common.URLConstants._HTML_ORGANIZATION_ORGANIZATIONID;
import static com.openkoda.core.repository.common.SearchableFunctionalRepositoryWithLongId.searchSpecificationFactory;

/**
 * Generic HTML CRUD controller providing list, detail, create, edit, delete operations for any registered entity type.
 * <p>
 * Central generic adapter routing under /{obj}. Parses pagination/sorting/search/filter parameters,
 * enforces privileges from {@link CRUDControllerConfiguration}, orchestrates repository operations
 * via Flow pipelines and {@link ReflectionBasedEntityForm}. Supports CSV exports via services.csv
 * and services.file. Retrieves configuration from {@link HtmlCRUDControllerConfigurationMap} by {obj}
 * path variable.
 * </p>
 * <p>
 * Key helpers: {@link #createPageable(HttpServletRequest, String)}, {@link #createSearch(HttpServletRequest, String)},
 * {@link #convertUsingReflection(Object)}.
 * </p>
 * <p>
 * Example usage for registered entity "users":
 * <pre>
 * GET /html/users/all - Lists all users
 * GET /html/users/new-settings - Create form
 * POST /html/users/123/settings - Update user
 * </pre>
 * </p>
 * <p>
 * Thread-safety: Stateless controller, thread-safe.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CRUDControllerConfiguration
 * @see HtmlCRUDControllerConfigurationMap
 * @see ReflectionBasedEntityForm
 */
@RestController
@RequestMapping({_HTML_ORGANIZATION_ORGANIZATIONID + "/{obj}", _HTML + "/{obj}"})
public class CRUDControllerHtml extends AbstractController implements HasSecurityRules {

    /**
     * Prefix for object filter request parameters used in search and filtering operations.
     * Filter parameters follow pattern: obj_filter_{fieldName}={value}.
     */
    public static final String OBJ_FILTER_PREFIX = "obj_filter_";
    
    /**
     * Default layout name for views, configurable via application property "default.layout".
     * Defaults to "main" if not specified.
     */
    @Value("${default.layout:main}")
    String defaultLayoutName;

    /**
     * Lists all entities with pagination, sorting, and filtering.
     * <p>
     * GET endpoint that displays paginated list of entity instances registered under the given objKey.
     * Applies search filtering, column sorting, custom filters, and privilege-based access control.
     * Returns entities visible to current user based on organization membership and privileges.
     * </p>
     * <p>
     * HTTP mapping: GET /{obj}/all where {obj} is entity type key (e.g., "users", "organizations")
     * </p>
     * <p>
     * Query parameters:
     * - {obj}_page: Page number (0-based, default: 0)
     * - {obj}_size: Page size (default: 10)
     * - {obj}_sort: Sort column with optional direction (e.g., "name,asc")
     * - {obj}_search: Full-text search term
     * - obj_search: Common search term across all columns
     * - obj_filter_{field}: Filter by specific field value
     * </p>
     * <p>
     * Flow usage:
     * <pre>
     * Flow.init()
     *   .thenSet(entities, a -> repository.findAll(pageable))
     *   .execute()
     * </pre>
     * </p>
     *
     * @param organizationId Organization ID for organization-scoped entities (optional, extracted from path)
     * @param objKey Entity type key from configuration map (e.g., "users", "organizations")
     * @param commonSearch Optional global search query parameter
     * @param request HTTP request containing pagination, sorting, and filter parameters
     * @return ModelAndView with view name and PageModelMap containing:
     *         'entities' - paginated list,
     *         'genericTableViewList' - formatted entity rows,
     *         'genericTableViewHeaders' - table column headers,
     *         'pagination' - page metadata
     * @throws org.springframework.security.access.AccessDeniedException if user lacks required read privilege
     */
    @SuppressWarnings("unchecked")
    @GetMapping(_ALL)
    //TODO Rule 1.2: All business logic delegation should be in Abstract Controller
    //TODO Rule 1.4 All methods in non-public controllers must have @PreAuthorize
    public Object getAll(
            @PathVariable(name=ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(name="obj", required=true) String objKey,
            @RequestParam(required = false, defaultValue = "", name = "obj_search") String commonSearch,
            HttpServletRequest request
            ) {
        debug("[getAll]");
        Pageable aPageable = createPageable(request, objKey);
        String search = createSearch(request, objKey);
        Tuple2<String, Map<String, String>> remainingParams = createRemainingParams(request, objKey);
        Map<String, String> objFilters = createFilters(request, objKey);


        @SuppressWarnings("rawtypes")
        CRUDControllerConfiguration conf = controllers.htmlCrudControllerConfigurationMap.getIgnoreCase(objKey);
        Set<Long> organizationIdsWithPrivilege;
        OrganizationUser user = UserProvider.getFromContext().get();
        if(user.isSuperUser()) {
            organizationIdsWithPrivilege = null;
        } else {
            organizationIdsWithPrivilege = UserProvider.getFromContext().get().getOrganizationRoles().keySet().stream()
                .filter( orgId -> hasGlobalOrOrgPrivilege(conf.getGetAllPrivilege(), orgId))
                .collect(Collectors.toSet());
            if (notValidAccess(conf.getGetAllPrivilege(), conf.getOrganizationId(), organizationId) && organizationIdsWithPrivilege.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        // if there is no sorting set in the request, by default sort using the first column available (ususally ID or NAME)
        if(aPageable.getSort().isUnsorted() &&  conf.getTableFormFieldNames().length > 0) {
            String sortColumn = conf.getTableFormFieldNames()[0];
            aPageable = PageRequest.of(aPageable.getPageNumber(), aPageable.getPageSize(), Sort.Direction.ASC, sortColumn);
        }

        final Pageable finalPageable = aPageable;
        Map<String, Boolean> fieldColumnVisibility = new HashMap<>();
        return Flow.init(componentProvider)
                .thenSet(searchTerm, a -> search)
                .thenSet(PageAttributes.objFilters, a -> objFilters)
                .thenSet((PageAttr<Page<SearchableOrganizationRelatedEntity>>)conf.getEntityPageAttribute(), a ->  {
                    final Long effectiveOrganizationId = (organizationId == null && organizationIdsWithPrivilege != null && organizationIdsWithPrivilege.size() == 1) ? organizationIdsWithPrivilege.iterator().next() : organizationId;
                    if(effectiveOrganizationId != null) {
                            return (Page<SearchableOrganizationRelatedEntity>) conf.getSecureRepository()
                        .search(search, effectiveOrganizationId, searchSpecificationFactory(commonSearch).and(conf.getAdditionalSpecification()), finalPageable, ReflectionBasedEntityForm.getFilterTypesAndValues(conf.getFrontendMappingDefinition(), objFilters));
                    } else {
                        return (Page<SearchableOrganizationRelatedEntity>) conf.getSecureRepository()
                            .search(search, organizationIdsWithPrivilege, searchSpecificationFactory(commonSearch).and(conf.getAdditionalSpecification()), finalPageable, ReflectionBasedEntityForm.getFilterTypesAndValues(conf.getFrontendMappingDefinition(), objFilters));
                    }
                })
                .thenSet(genericTableViewList, a -> ReflectionBasedEntityForm.calculateFieldsValuesWithReadPrivileges(conf.getFrontendMappingDefinition(), a.result.toList(), conf.getTableFormFieldNames(), fieldColumnVisibility, organizationId))
                .thenSet(genericTableViewHeaders, a -> ReflectionBasedEntityForm.getFieldsHeaders(conf.getFrontendMappingDefinition(), conf.getTableFormFieldNames(), fieldColumnVisibility, organizationId))
                .thenSet(genericTableFilters, a -> ReflectionBasedEntityForm.getFilterFields(conf.getFrontendMappingDefinition(), conf.getFilterFieldNames()))
                .thenSet(genericViewNavigationFragment, a -> conf.getNavigationFragment())
                .thenSet(isMapEntity, a -> conf.isMapEntity())
                .thenSet(frontendMappingDefinition, a -> conf.getFrontendMappingDefinition())
                .thenSet(menuItem, a -> conf.getMenuItem() != null ? conf.getMenuItem() : objKey)
                .thenSet(organizationRelatedObjectKey, a -> objKey)
                .thenSet(organizationRelatedForm, a -> conf.createNewForm())
                .thenSet(isAuditable, a -> services.customisation.isAuditableClass(conf.getEntityClass()))
                .thenSet(remainingParameters, remainingParametersMap, a -> remainingParams)
                .execute()
                .mav(conf.getTableViewWebEndpoint() != null ? conf.getTableViewWebEndpoint() : conf.getTableView());
    }

    /**
     * Displays empty entity creation form.
     * <p>
     * GET endpoint that renders form for creating new entity instance. Form fields are defined
     * in {@link CRUDControllerConfiguration#getFrontendMappingDefinition()}. Returns empty form
     * with default values and validation rules applied.
     * </p>
     * <p>
     * HTTP mapping: GET /{obj}/new-settings
     * </p>
     *
     * @param organizationId Organization ID for organization-scoped entities (optional)
     * @param objKey Entity type key from configuration map (e.g., "users", "organizations")
     * @return ModelAndView with empty form and settings view template
     * @throws org.springframework.security.access.AccessDeniedException if user lacks required create privilege
     */
    @GetMapping(_NEW_SETTINGS)
    //TODO Rule 1.2: All business logic delegation should be in Abstract Controller
    //TODO Rule 1.4 All methods in non-public controllers must have @PreAuthorize
    public Object create(
            @PathVariable(name = ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(name="obj", required=true) String objKey) {
        CRUDControllerConfiguration conf = controllers.htmlCrudControllerConfigurationMap.getIgnoreCase(objKey);
        if (notValidAccess(conf.getGetNewPrivilege(), conf.getOrganizationId(), organizationId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return Flow.init(transactional)
                .thenSet(conf.getFormAttribute(), a -> conf.createNewForm(organizationId, null))
                .thenSet(genericViewNavigationFragment, a -> conf.getNavigationFragment())
                .thenSet(menuItem, a -> conf.getMenuItem() != null ? conf.getMenuItem() : objKey)
                .execute()
                .mav(conf.getSettingsView());
    }

    /**
     * Displays entity edit form with pre-populated data.
     * <p>
     * GET endpoint that retrieves existing entity by ID and renders edit form with current values.
     * Form fields are populated from entity using {@link ReflectionBasedEntityForm}. Returns form
     * ready for modification and submission.
     * </p>
     * <p>
     * HTTP mapping: GET /{obj}/{id}/settings
     * </p>
     *
     * @param objectId Entity ID to edit
     * @param organizationId Organization ID for organization-scoped entities (optional)
     * @param objKey Entity type key from configuration map (e.g., "users", "organizations")
     * @return ModelAndView with pre-populated form and settings view template
     * @throws com.openkoda.core.exception.ResourceNotFoundException if entity with given ID not found
     * @throws org.springframework.security.access.AccessDeniedException if user lacks required read privilege
     */
    @GetMapping(_ID_SETTINGS)
    //TODO Rule 1.2: All business logic delegation should be in Abstract Controller
    //TODO Rule 1.4 All methods in non-public controllers must have @PreAuthorize
    public Object setting(
            @PathVariable(name = ID) Long objectId,
            @PathVariable(name = ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(name="obj", required=true) String objKey) {

        CRUDControllerConfiguration conf = controllers.htmlCrudControllerConfigurationMap.getIgnoreCase(objKey);
        if (notValidAccess(conf.getGetSettingsPrivilege(), conf.getOrganizationId(), organizationId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return Flow.init(componentProvider, objectId)
                .then(a -> conf.getSecureRepository().findOne(objectId))
                .thenSet(conf.getFormAttribute(), ac -> conf.createNewForm(organizationId, (SearchableOrganizationRelatedEntity) ac.result))
                .thenSet(genericViewNavigationFragment, a -> conf.getNavigationFragment())
                .thenSet(menuItem, a -> conf.getMenuItem() != null ? conf.getMenuItem() : objKey)
                .execute()
                .mav(conf.getSettingsView());
    }

    /**
     * Processes entity creation form submission.
     * <p>
     * POST endpoint that validates form data, creates new entity instance, and persists to database.
     * Performs Jakarta Bean Validation on form, populates entity from form using reflection,
     * saves via secure repository. Returns success or error fragment based on validation result.
     * </p>
     * <p>
     * HTTP mapping: POST /{obj}/new-settings
     * </p>
     * <p>
     * Flow pipeline:
     * <pre>
     * validateAndPopulateToEntity(form) -> repository.save() -> createNewForm()
     * </pre>
     * </p>
     *
     * @param organizationId Organization ID for organization-scoped entities (optional)
     * @param objKey Entity type key from configuration map (e.g., "users", "organizations")
     * @param form Bound form data with @Valid validation applied
     * @param br BindingResult containing validation errors if any
     * @return ModelAndView with success fragment (entity saved) or error fragment (validation failed)
     * @throws jakarta.validation.ValidationException if form validation fails
     * @throws org.springframework.security.access.AccessDeniedException if user lacks required create privilege
     */
    @PostMapping(_NEW_SETTINGS)
    @Transactional
    //TODO Rule 1.2: All business logic delegation should be in Abstract Controller
    //TODO Rule 1.4 All methods in non-public controllers must have @PreAuthorize
    public Object saveNew(
            @PathVariable(name = ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(name="obj", required=true) String objKey,
            @Valid AbstractOrganizationRelatedEntityForm form, BindingResult br) {
        debug("[saveNew]");
        CRUDControllerConfiguration conf = controllers.htmlCrudControllerConfigurationMap.getIgnoreCase(objKey);
        if (notValidAccess(conf.getPostNewPrivilege(), conf.getOrganizationId(), organizationId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ///
        return ((Flow<Object, AbstractOrganizationRelatedEntityForm, DefaultComponentProvider>)
                Flow.init(componentProvider, conf.getFormAttribute(), form))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br, conf.createNewEntity(organizationId)))
                .then(a -> (SearchableOrganizationRelatedEntity)conf.getSecureRepository().saveOne(a.result))
                .thenSet(conf.getFormAttribute(), a -> conf.createNewForm(organizationId, a.result))
                .thenSet(organizationRelatedObjectKey, a -> objKey)
                .execute()
                .mav(conf.getFormSuccessFragment(), conf.getFormErrorFragment());
    }

    /**
     * Processes entity update form submission.
     * <p>
     * POST endpoint that validates form data, retrieves existing entity, updates fields, and persists changes.
     * Performs Jakarta Bean Validation on form, loads entity by ID, populates updated fields from form,
     * saves via secure repository. Returns success or error fragment based on validation result.
     * </p>
     * <p>
     * HTTP mapping: POST /{obj}/{id}/settings
     * </p>
     * <p>
     * Flow pipeline:
     * <pre>
     * repository.findOne(id) -> validateAndPopulateToEntity(form) -> repository.save()
     * </pre>
     * </p>
     *
     * @param objectId Entity ID to update
     * @param organizationId Organization ID for organization-scoped entities (optional)
     * @param objKey Entity type key from configuration map (e.g., "users", "organizations")
     * @param form Bound form data with @Valid validation applied
     * @param br BindingResult containing validation errors if any
     * @return ModelAndView with success fragment (entity updated) or error fragment (validation failed)
     * @throws jakarta.validation.ValidationException if form validation fails
     * @throws com.openkoda.core.exception.ResourceNotFoundException if entity with given ID not found
     * @throws org.springframework.security.access.AccessDeniedException if user lacks required update privilege
     */
    @PostMapping(_ID_SETTINGS)
    @Transactional
    //TODO Rule 1.2: All business logic delegation should be in Abstract Controller
    //TODO Rule 1.4 All methods in non-public controllers must have @PreAuthorize
    public Object save(
            @PathVariable(ID) Long objectId,
            @PathVariable(name = ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(name="obj", required=true) String objKey,
            @Valid AbstractOrganizationRelatedEntityForm form, BindingResult br) {
        debug("[saveNew]");
        CRUDControllerConfiguration conf = controllers.htmlCrudControllerConfigurationMap.getIgnoreCase(objKey);
        if (notValidAccess(conf.getPostSavePrivilege(), conf.getOrganizationId(), organizationId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ((Flow<Object, AbstractOrganizationRelatedEntityForm, DefaultComponentProvider>)
                Flow.init(componentProvider, conf.getFormAttribute(), form))
                .then(a -> (SearchableOrganizationRelatedEntity)conf.getSecureRepository().findOne(objectId))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br,a.result))
                .then(a -> (SearchableOrganizationRelatedEntity)conf.getSecureRepository().saveOne(a.result))
                .thenSet(conf.getFormAttribute(), a -> conf.createNewForm(organizationId, a.result))
                .execute()
                .mav(conf.getFormSuccessFragment(), conf.getFormErrorFragment());
    }

    /**
     * Deletes entity by ID.
     * <p>
     * POST endpoint that removes entity instance from database. Performs secure delete via repository
     * with privilege enforcement. Returns boolean success indicator.
     * </p>
     * <p>
     * HTTP mapping: POST /{obj}/{id}/remove
     * </p>
     *
     * @param objectId Entity ID to delete
     * @param organizationId Organization ID for organization-scoped entities (optional)
     * @param objKey Entity type key from configuration map (e.g., "users", "organizations")
     * @return ModelAndView with true on success, false on failure
     * @throws com.openkoda.core.exception.ResourceNotFoundException if entity with given ID not found
     * @throws org.springframework.security.access.AccessDeniedException if user lacks required delete privilege
     */
    @PostMapping(_ID_REMOVE)
    @Transactional
    //TODO Rule 1.2: All business logic delegation should be in Abstract Controller
    //TODO Rule 1.4 All methods in non-public controllers must have @PreAuthorize
    public Object remove(
            @PathVariable(name=ID) Long objectId,
            @PathVariable(name = ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(name="obj", required=true) String objKey) {
        CRUDControllerConfiguration conf = controllers.htmlCrudControllerConfigurationMap.getIgnoreCase(objKey);
        if (notValidAccess(conf.getPostRemovePrivilege(), conf.getOrganizationId(), organizationId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return Flow.init(componentProvider, objectId)
                .then(a -> conf.getSecureRepository().deleteOne(objectId))
                .execute()
                .mav(a -> true, a -> false);
    }

    /**
     * Shows entity detail view.
     * <p>
     * GET endpoint that retrieves entity by ID and displays read-only detail view. Converts entity
     * to map representation using reflection for flexible rendering. Useful for viewing entity
     * details without edit capability.
     * </p>
     * <p>
     * HTTP mapping: GET /{obj}/{id}/view
     * </p>
     *
     * @param objectId Entity ID to view
     * @param organizationId Organization ID for organization-scoped entities (optional)
     * @param objKey Entity type key from configuration map (e.g., "users", "organizations")
     * @return ModelAndView with 'organizationRelatedEntity' and 'organizationRelatedObjectMap' attributes
     * @throws com.openkoda.core.exception.ResourceNotFoundException if entity with given ID not found
     * @throws org.springframework.security.access.AccessDeniedException if user lacks required read privilege
     */
    @GetMapping(_ID + _VIEW)
    //TODO Rule 1.2: All business logic delegation should be in Abstract Controller
    //TODO Rule 1.4 All methods in non-public controllers must have @PreAuthorize
    public Object view(
            @PathVariable(name = ID) Long objectId,
            @PathVariable(name = ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(name="obj", required=true) String objKey) {

        CRUDControllerConfiguration conf = controllers.htmlCrudControllerConfigurationMap.getIgnoreCase(objKey);
        if (notValidAccess(conf.getGetSettingsPrivilege(), conf.getOrganizationId(), organizationId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return Flow.init(componentProvider, objectId)
                .thenSet(organizationRelatedObjectKey, a -> objKey)
                .thenSet(organizationRelatedEntity, a -> (SearchableOrganizationRelatedEntity) conf.getSecureRepository().findOne(objectId))
                .thenSet(organizationRelatedObjectMap, a -> convertUsingReflection(a.result))
                .thenSet(genericViewNavigationFragment, a -> conf.getNavigationFragment())
                .thenSet(menuItem, a -> conf.getMenuItem() != null ? conf.getMenuItem() : objKey)
                .execute()
                .mav(conf.getReadView());
    }

    /**
     * Exports entity list as CSV file.
     * <p>
     * GET endpoint that generates CSV report of filtered entity list. Applies same search and filter
     * parameters as list view, renders all matching entities (no pagination) to CSV format. Uses
     * services.csv.createCSV for generation and services.file for attachment download. CSV filename
     * includes entity key and timestamp.
     * </p>
     * <p>
     * HTTP mapping: GET /{obj}/report/csv
     * </p>
     * <p>
     * Query parameters: Same as getAll (search, filters) but without pagination
     * </p>
     * <p>
     * CSV format: First row contains column headers, subsequent rows contain entity data
     * </p>
     *
     * @param organizationId Organization ID for organization-scoped entities (optional)
     * @param objKey Entity type key from configuration map (e.g., "users", "organizations")
     * @param search Optional search query parameter
     * @param filters Map of filter parameters (prefix: obj_filter_)
     * @param response HTTP response to write CSV content with attachment disposition
     * @throws SQLException if CSV generation fails due to database error
     * @throws IOException if file writing fails
     * @throws org.springframework.security.access.AccessDeniedException if user lacks required read privilege
     */
    @Transactional(readOnly = true)
    @GetMapping(_REPORT + _CSV)
    public void getCsvReport(
            @PathVariable(name=ORGANIZATIONID, required = false) Long organizationId,
            @PathVariable(name="obj", required=true) String objKey,
            @RequestParam(required = false, defaultValue = "", name = "obj_search") String search,
            @RequestParam(required = false) Map<String, String> filters,
            HttpServletResponse response
    ) throws SQLException, IOException {
        debug("[getCsvReport]");
        Map<String, String> objFilters = filters.entrySet().stream().filter(entry -> entry.getKey().startsWith(OBJ_FILTER_PREFIX) && StringUtils.isNotEmpty(entry.getValue()))
                .collect(Collectors.toMap(entry -> StringUtils.substringAfter(entry.getKey(), OBJ_FILTER_PREFIX), Map.Entry::getValue));

        CRUDControllerConfiguration conf = controllers.htmlCrudControllerConfigurationMap.getIgnoreCase(objKey);
        if (notValidAccess(conf.getGetAllPrivilege(), conf.getOrganizationId(), organizationId)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
        Map<String, Boolean> fieldColumnVisibility = new HashMap<>();
        File report = Flow.init(componentProvider)
                .thenSet(genericTableHeaders, a -> conf.getReportFormFieldNames())
                .then(a -> (List<SearchableOrganizationRelatedEntity>) conf.getSecureRepository()
                        .search(search, organizationId, conf.getAdditionalSpecification(), ReflectionBasedEntityForm.getFilterTypesAndValues(conf.getFrontendMappingDefinition(), objFilters)))
                .thenSet(genericTableViewList, a -> ReflectionBasedEntityForm.calculateFieldsValuesWithReadPrivileges(conf.getFrontendMappingDefinition(), a.result, a.model.get(genericTableHeaders), fieldColumnVisibility, organizationId))
                .thenSet(genericTableViewHeaders, a -> ReflectionBasedEntityForm.getFieldsHeaders(conf.getFrontendMappingDefinition(), a.model.get(genericTableHeaders), fieldColumnVisibility, organizationId))
                .thenSet(file, a -> {
                    try {
                        return services.csv.createCSV(String.format("%s_%s.csv", objKey, dtf.format(LocalDateTime.now())), a.model.get(genericTableViewList), a.model.get(genericTableHeaders));
                    } catch (IOException | SQLException e) {
                        error("[getCsvReport]", e);
                        return null;
                    }
                })
                .execute()
                .get(file);
        services.file.getFileContentAndPrepareResponse(report, true, false, response);
    }

    /**
     * Validates user has required privilege for operation.
     * <p>
     * Checks if current user has global or organization-specific privilege for accessing entity.
     * Used internally by all CRUD endpoints for privilege enforcement.
     * </p>
     *
     * @param privilege Required privilege for operation (read, create, update, delete)
     * @param confOrganizationId Organization ID from controller configuration (unused in current implementation)
     * @param organizationId Organization ID from request path (used for privilege check)
     * @return true if access denied (user lacks privilege), false if access granted
     */
    private boolean notValidAccess(PrivilegeBase privilege, Long confOrganizationId, Long organizationId){
        return !hasGlobalOrOrgPrivilege(privilege, organizationId);
    }

    /**
     * Converts entity to Map using reflection for flexible view rendering.
     * <p>
     * Extracts all declared fields from entity object and populates map with field name as key
     * and field value as value. Skips fields from dynamic entity package to avoid circular references.
     * Used by view endpoint for generic entity display.
     * </p>
     *
     * @param object Entity instance to convert (typically SearchableOrganizationRelatedEntity)
     * @return Map with field names as keys and field values as values
     * @throws RuntimeException wrapping IllegalAccessException if field access fails
     */
    private Map<String, Object> convertUsingReflection(Object object) {
        Map<String, Object> map = new HashMap<>();
        Field[] fields = object.getClass().getDeclaredFields();

        try {
            for (Field field: fields) {
                if(!field.getType().getName().startsWith(DynamicEntityRegistrationService.PACKAGE)) {
                    field.setAccessible(true);
                    map.put(field.getName(), field.get(object));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return map;
    }

    /**
     * Extracts search query parameter from HTTP request.
     * <p>
     * Constructs search parameter name from qualifier (entity key) and "_search" suffix.
     * Supports case-insensitive parameter matching. Returns last value if parameter specified
     * multiple times. Returns empty string if parameter not present.
     * </p>
     * <p>
     * Expected parameter format: {qualifier}_search={searchTerm}
     * </p>
     *
     * @param request HTTP request containing search parameters
     * @param qualifier Entity key qualifier (e.g., "users" results in parameter "users_search")
     * @return Search query string, or empty string if not specified
     */
    public static String createSearch(HttpServletRequest request, String qualifier) {
        String result = request.getParameter(qualifier + "_search");
        if(result == null) {
            Optional<Map.Entry<String, String[]>> searches = request.getParameterMap().entrySet().stream().filter( e -> StringUtils.equalsAnyIgnoreCase(qualifier  + "_search", e.getKey())).findFirst();
            if(searches != null && searches.isPresent() && searches.get().getValue().length > 0) {
                return searches.get().getValue()[searches.get().getValue().length - 1];
            }
        } else {
            return result;
        }
        
        return "";
    }

    /**
     * Extracts non-entity-specific request parameters.
     * <p>
     * Collects all request parameters that don't start with qualifier prefix (entity-specific params).
     * Returns both query string format and map representation for URL reconstruction and parameter access.
     * Useful for preserving external parameters when redirecting or paginating.
     * </p>
     *
     * @param request HTTP request with all parameters
     * @param qualifier Entity key qualifier to exclude (parameters starting with "{qualifier}_" are filtered out)
     * @return Tuple2 with query string (e.g., "&param1=value1&param2=value2") and parameter map
     */
    public static Tuple2<String, Map<String, String>> createRemainingParams(HttpServletRequest request, String qualifier) {
        StringBuilder result = new StringBuilder();
        Map<String, String> resultMap = new HashMap<>();
        String prefix = qualifier + "_";
        Enumeration<String> n = request.getParameterNames();
        while (n.hasMoreElements()) {
            String k = n.nextElement();
            if (StringUtils.startsWithIgnoreCase(k, prefix)) { continue; }
            String[] vals = request.getParameterValues(k);
            String v = vals != null ? vals[vals.length - 1] : null;
            resultMap.put(k, v);
            result.append('&');
            result.append(k);
            result.append('=');
            result.append(v);
        }
        return Tuples.of(result.toString(), resultMap);
    }

    /**
     * Extracts field-specific filter parameters from HTTP request.
     * <p>
     * Collects filter parameters following pattern {qualifier}_filter_{fieldName}={value}.
     * Filters out empty values. Returns map with field names as keys and filter values as values.
     * Used by getAll and getCsvReport for applying column-specific filters.
     * </p>
     * <p>
     * Example: users_filter_status=active, users_filter_role=admin results in
     * Map: {"status": "active", "role": "admin"}
     * </p>
     *
     * @param request HTTP request containing filter parameters
     * @param qualifier Entity key qualifier (e.g., "users" results in parameters like "users_filter_status")
     * @return Map of field names to filter values (empty map if no filters specified)
     */
    public static Map<String, String> createFilters(HttpServletRequest request, String qualifier) {
        Map<String, String> result = new HashMap<>();
        String prefix = qualifier + "_filter_";
        Enumeration<String> n = request.getParameterNames();
        while (n.hasMoreElements()) {
            String s = n.nextElement();
            if (s.toLowerCase().startsWith(prefix)) {
                String v = request.getParameter(s);
                if (StringUtils.isNotBlank(v)) {
                    result.put(s.substring(prefix.length()), request.getParameter(s));
                }
            }
        }

        return result;
    }

    /**
     * Constructs Spring Data Pageable from HTTP request parameters.
     * <p>
     * Extracts pagination and sorting parameters from request with entity-specific qualifier prefix.
     * Parses page number, page size, and optional sort column with direction. Returns Pageable
     * instance for use with Spring Data JPA repositories.
     * </p>
     * <p>
     * Expected parameters:
     * - {qualifier}_page: Page number (0-based, default: 0)
     * - {qualifier}_size: Page size (default: 10)
     * - {qualifier}_sort: Sort specification in format "column" or "column,direction" (e.g., "name,asc")
     * </p>
     *
     * @param request HTTP request containing pagination parameters
     * @param qualifier Entity key qualifier (e.g., "users" results in parameters like "users_page", "users_size")
     * @return Pageable instance with page, size, and sort configuration
     */
    public static Pageable createPageable(HttpServletRequest request, String qualifier) {
        int page = NumberUtils.isParsable(request.getParameter(qualifier + "_page")) ? Integer.parseInt(request.getParameter(qualifier + "_page")) : 0;
        int size = NumberUtils.isParsable(request.getParameter(qualifier + "_size")) ? Integer.parseInt(request.getParameter(qualifier + "_size")) : 10;
        String sortParam = request.getParameter(qualifier + "_sort");

        if (sortParam != null && !sortParam.isEmpty()) {
            String[] sortParams = sortParam.split(",");
            Sort sort;
            if (sortParams.length > 1) {
                Sort.Direction direction = Sort.Direction.fromString(sortParams[1]);
                sort = Sort.by(direction, sortParams[0]);
            } else {
                sort = Sort.by(sortParams[0]);
            }
            return PageRequest.of(page, size, sort);
        } else {
            return PageRequest.of(page, size);
        }
    }
}
