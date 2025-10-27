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

package com.openkoda.controller.api;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.controller.DefaultComponentProvider;
import com.openkoda.controller.common.URLConstants;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageAttr;
import com.openkoda.core.form.CRUDControllerConfiguration;
import com.openkoda.core.form.ReflectionBasedEntityForm;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.common.SearchableOrganizationRelatedEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.HashMap;

/**
 * Abstract generic REST API controller providing JSON CRUD endpoints for organization-scoped entities.
 * <p>
 * This controller serves as a centralized adapter that eliminates boilerplate code for REST API CRUD operations.
 * It extends {@link ComponentProvider} for service and repository access, implements {@link URLConstants} for
 * routing constants, and implements {@link HasSecurityRules} for privilege enforcement. The controller is
 * parameterized with entity type {@code E} extending {@link SearchableOrganizationRelatedEntity}.
 * </p>
 * <p>
 * At construction, a final {@code String key} parameter selects the appropriate {@link CRUDControllerConfiguration}
 * from {@code controllers.apiCrudControllerConfigurationMap}. The injected {@link DefaultComponentProvider} is
 * forwarded to {@link Flow#init} for pipeline initialization. The controller is stateless except for the immutable
 * key field, making it thread-safe.
 * </p>
 * <p>
 * All endpoints return JSON via {@code produces=MediaType.APPLICATION_JSON_VALUE}. Security is enforced through
 * privilege checks using {@code hasGlobalOrOrgPrivilege} before each operation. Unauthorized requests return
 * HTTP 401 UNAUTHORIZED status. The {@link com.openkoda.core.repository.common.SecureRepository} enforces
 * row-level security during all data access operations.
 * </p>
 * <p>
 * Flow Pipeline Pattern: All operations use {@code Flow.init(componentProvider)} for composable request handling.
 * The {@link ReflectionBasedEntityForm} handles entity-to-JSON mapping with privilege-based field filtering.
 * </p>
 * <p>
 * Thread-safety: Stateless controller except for immutable key field - thread-safe. Each request creates a new
 * Flow pipeline instance.
 * </p>
 *
 * @param <E> Entity type extending {@link SearchableOrganizationRelatedEntity} for CRUD operations
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CRUDControllerConfiguration
 * @see com.openkoda.controller.common.ApiCRUDControllerConfigurationMap
 * @see ComponentProvider
 * @see HasSecurityRules
 */
abstract public class CRUDApiController<E extends SearchableOrganizationRelatedEntity> extends ComponentProvider implements URLConstants, HasSecurityRules {

    /**
     * Injected DefaultComponentProvider for Flow pipeline initialization.
     * <p>
     * This component provider is passed to {@code Flow.init(componentProvider)} to enable
     * access to services and repositories within Flow pipelines. It provides the foundation
     * for all CRUD operations in this controller.
     * </p>
     *
     * @see Flow#init(ComponentProvider)
     */
    @Inject
    protected DefaultComponentProvider componentProvider;

    /**
     * Immutable configuration key resolving entity-specific CRUD settings from ApiCRUDControllerConfigurationMap.
     * <p>
     * This key is set in the constructor and never changes. It determines the repository, form class,
     * privileges, and other settings for all CRUD operations performed by this controller instance.
     * The key corresponds to an entry in {@code controllers.apiCrudControllerConfigurationMap}, such as
     * "FRONTENDRESOURCE" or "SERVERJS".
     * </p>
     */
    private final String key;

    /**
     * Constructs controller with configuration key for entity-specific CRUD settings.
     * <p>
     * The key parameter stores an immutable reference that resolves to a {@link CRUDControllerConfiguration}
     * at runtime. This configuration contains the {@link com.openkoda.core.repository.common.SecureRepository},
     * form class, {@link com.openkoda.core.form.FrontendMappingDefinition}, and privilege requirements for
     * all CRUD operations.
     * </p>
     *
     * @param key Configuration key in ApiCRUDControllerConfigurationMap (e.g., "FRONTENDRESOURCE", "SERVERJS")
     */
    public CRUDApiController(String key) {
        this.key = key;
    }

    /**
     * Lists all entities with pagination, sorting, and search filtering.
     * <p>
     * This endpoint retrieves entities using the configured {@link com.openkoda.core.repository.common.SecureRepository}.
     * It first checks the GetAllPrivilege from the controller configuration via {@code hasGlobalOrOrgPrivilege}.
     * If the privilege check passes, it executes a Flow pipeline that calls
     * {@code conf.getSecureRepository().search(search, organizationId, additionalSpecification, pageable)},
     * maps results using {@link ReflectionBasedEntityForm#calculateFieldsValuesWithReadPrivilegesAsMap}, and
     * returns a JSON map from {@code genericTableViewMap}.
     * </p>
     * <p>
     * HTTP Mapping: {@code GET /{base-path}/all}
     * </p>
     * <p>
     * Response format: {@code {"data": [{...entity fields...}], "totalElements": 100, "totalPages": 10}}
     * </p>
     *
     * @param organizationId Optional organization ID for tenant-scoped filtering (may be null for global scope)
     * @param aPageable Pagination parameters (page, size, sort) with qualifier "obj"
     * @param search Optional search query for full-text filtering (defaults to empty string)
     * @return JSON map with paginated entity list, or {@link ResponseEntity} with HTTP 401 UNAUTHORIZED if privilege check fails
     * @throws org.springframework.security.access.AccessDeniedException if user lacks GetAllPrivilege from configuration
     */
    @GetMapping(value=_ALL, produces=MediaType.APPLICATION_JSON_VALUE)
    public Object getAll(
            @PathVariable(name=ORGANIZATIONID, required = false) Long organizationId,
            @Qualifier("obj") Pageable aPageable,
            @RequestParam(required = false, defaultValue = "", name = "obj_search") String search) {
        debug("[getAll]");
        CRUDControllerConfiguration conf = controllers.apiCrudControllerConfigurationMap.get(key);
        PrivilegeBase privilege = conf.getGetAllPrivilege();
        if (not(hasGlobalOrOrgPrivilege(privilege, organizationId))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return Flow.init(componentProvider)
                .then( a -> (Page<SearchableOrganizationRelatedEntity>) conf.getSecureRepository().search(search, organizationId, conf.getAdditionalSpecification(), aPageable))
                .thenSet(genericTableViewMap, a -> ReflectionBasedEntityForm.calculateFieldsValuesWithReadPrivilegesAsMap(conf.getFrontendMappingDefinition(), a.result, conf.getTableFormFieldNames()))
                .execute()
                .getAsMap(genericTableViewMap);
    }

    /**
     * Retrieves single entity by ID with read privilege filtering.
     * <p>
     * This endpoint checks GetSettingsPrivilege, calls {@code conf.getSecureRepository().findOne(objectId)},
     * and maps the entity to JSON using {@link ReflectionBasedEntityForm#calculateFieldValuesWithReadPrivilegesAsMap}.
     * Field visibility is filtered based on read privileges defined in the {@link com.openkoda.core.form.FrontendMappingDefinition}
     * field definitions.
     * </p>
     * <p>
     * HTTP Mapping: {@code GET /{base-path}/{id}}
     * </p>
     * <p>
     * Response format: {@code {"id": 123, "name": "...", "updatedOn": "2023-12-15T10:30:00"}}
     * </p>
     *
     * @param objectId Entity ID to retrieve
     * @param organizationId Optional organization ID for tenant context (may be null for global scope)
     * @return JSON map with entity fields filtered by read privileges, or {@link ResponseEntity} with HTTP 401 UNAUTHORIZED
     * @throws org.springframework.security.access.AccessDeniedException if user lacks GetSettingsPrivilege
     * @throws com.openkoda.core.exception.ResourceNotFoundException if entity with given ID is not found
     */
    @GetMapping(value = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object settings(
            @PathVariable(name = ID) Long objectId,
            @PathVariable(name = ORGANIZATIONID, required = false) Long organizationId
            ) {

        CRUDControllerConfiguration conf = controllers.apiCrudControllerConfigurationMap.get(key);
        PrivilegeBase privilege = conf.getGetSettingsPrivilege();
        if (not(hasGlobalOrOrgPrivilege(privilege, organizationId))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return Flow.init(componentProvider, objectId)
                .then(a -> conf.getSecureRepository().findOne(objectId))
                .thenSet(conf.getEntityPageAttribute(), a -> ReflectionBasedEntityForm.calculateFieldValuesWithReadPrivilegesAsMap(conf.getFrontendMappingDefinition(), (SearchableOrganizationRelatedEntity) a.result, conf.getFrontendMappingDefinition().getNamesOfValuedTypeFields()))
                .execute().getAsMap(conf.getEntityPageAttribute());
    }

    /**
     * Updates existing entity from JSON request body.
     * <p>
     * This endpoint checks PostSavePrivilege, loads the entity via {@code findOne(objectId)}, creates a
     * {@link ReflectionBasedEntityForm}, calls {@code prepareDto(params, entity)} to bind request data,
     * validates via {@code services.validation.validateAndPopulateToEntity}, and saves the entity if valid
     * via {@code saveOne(entity)}. Returns validation status and entity ID on success.
     * </p>
     * <p>
     * HTTP Mapping: {@code POST /{base-path}/{id}/update}
     * </p>
     * <p>
     * Request body example: {@code {"name": "Updated Name", "description": "..."}}
     * </p>
     * <p>
     * Response on success: {@code {"isValid": true, "longEntityId": 123}}
     * <br>
     * Response on validation error: {@code {"isValid": false, "longEntityId": null}}
     * </p>
     *
     * @param objectId Entity ID to update
     * @param organizationId Optional organization ID for tenant context (may be null for global scope)
     * @param params JSON request body as HashMap containing field names and values to update
     * @return JSON map with validation result and entity ID: {@code {"isValid": true/false, "longEntityId": <id>/null}}
     * @throws org.springframework.security.access.AccessDeniedException if user lacks PostSavePrivilege
     * @throws javax.validation.ValidationException if entity validation fails
     */
    @PostMapping(value="{id}/update", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object save(
            @PathVariable(ID) Long objectId,
            @PathVariable(name = ORGANIZATIONID, required = false) Long organizationId,
            @RequestBody HashMap<String,String> params) {
        debug("[saveNew]");
        CRUDControllerConfiguration conf = controllers.apiCrudControllerConfigurationMap.get(key);
        PrivilegeBase privilege = conf.getPostSavePrivilege();
        if (not(hasGlobalOrOrgPrivilege(privilege, organizationId))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return
                Flow.init(componentProvider)
                .thenSet((PageAttr<E>) conf.getEntityPageAttribute(), a -> (E) conf.getSecureRepository().findOne(objectId))
                .thenSet((PageAttr<ReflectionBasedEntityForm>) conf.getFormAttribute(), a -> (ReflectionBasedEntityForm) conf.createNewForm(organizationId, a.result))
                 .then(a -> a.result.prepareDto(params, (E) a.model.get(conf.getEntityPageAttribute())))
                 .thenSet(isValid,a -> services.validation.validateAndPopulateToEntity((ReflectionBasedEntityForm) a.model.get(conf.getFormAttribute()), (E) a.model.get(conf.getEntityPageAttribute())))
                 .thenSet(longEntityId, a -> {
                     if(a.result) {
                         return ((E) conf.getSecureRepository().saveOne(a.model.get(conf.getEntityPageAttribute()))).getId();
                     }
                     return null;
                 })
                .execute()
                .getAsMap(isValid, longEntityId);
    }

    /**
     * Creates new entity from JSON request body.
     * <p>
     * This endpoint checks PostNewPrivilege, creates a new entity via {@code conf.createNewEntity(organizationId)},
     * creates a form, binds the request parameters via {@code prepareDto}, validates the entity, and saves it
     * if validation succeeds. Returns validation status and the new entity ID on success, or {@code isValid: false}
     * on validation failure.
     * </p>
     * <p>
     * HTTP Mapping: {@code POST /{base-path}/create}
     * </p>
     * <p>
     * Request body example: {@code {"name": "New Entity", "type": "dashboard"}}
     * </p>
     * <p>
     * Response on success: {@code {"isValid": true, "longEntityId": 456}}
     * <br>
     * Response on validation error: {@code {"isValid": false, "longEntityId": null}}
     * </p>
     *
     * @param organizationId Optional organization ID for new entity tenant assignment (may be null for global scope)
     * @param params JSON request body as HashMap containing field names and values for the new entity
     * @return JSON map with validation result and new entity ID: {@code {"isValid": true/false, "longEntityId": <id>/null}}
     * @throws org.springframework.security.access.AccessDeniedException if user lacks PostNewPrivilege
     * @throws javax.validation.ValidationException if entity validation fails
     */
    @PostMapping(value="create", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object saveNew(
            @PathVariable(name = ORGANIZATIONID, required = false) Long organizationId,
            @RequestBody HashMap<String,String> params) {
        debug("[saveNew]");
        CRUDControllerConfiguration conf = controllers.apiCrudControllerConfigurationMap.get(key);
        PrivilegeBase privilege = conf.getPostNewPrivilege();
        if (not(hasGlobalOrOrgPrivilege(privilege, organizationId))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return Flow.init(componentProvider)
                .thenSet((PageAttr<E>) conf.getEntityPageAttribute(), a -> (E) conf.createNewEntity(organizationId))
                .thenSet((PageAttr<ReflectionBasedEntityForm>) conf.getFormAttribute(), a -> (ReflectionBasedEntityForm) conf.createNewForm(organizationId, a.result))
                .then(a -> a.result.prepareDto(params, (SearchableOrganizationRelatedEntity) a.model.get(conf.getEntityPageAttribute())))
                .thenSet(isValid,a -> services.validation.validateAndPopulateToEntity((ReflectionBasedEntityForm) a.model.get(conf.getFormAttribute()), (E) a.model.get(conf.getEntityPageAttribute())))
                .thenSet(longEntityId, a -> {
                    if(a.result) {
                        return ((E) conf.getSecureRepository().saveOne(a.model.get(conf.getEntityPageAttribute()))).getId();
                    }
                    return null;
                })
                .execute()
                .getAsMap(isValid, longEntityId);
    }

    /**
     * Deletes entity by ID.
     * <p>
     * This endpoint checks PostRemovePrivilege and calls {@code conf.getSecureRepository().deleteOne(objectId)}
     * via a Flow pipeline. Returns an empty Flow result on success, or HTTP 401 UNAUTHORIZED if the user lacks
     * the required privilege.
     * </p>
     * <p>
     * HTTP Mapping: {@code POST /{base-path}/{id}/remove}
     * </p>
     *
     * @param objectId Entity ID to delete
     * @param organizationId Optional organization ID for tenant context (may be null for global scope)
     * @return Empty Flow result on success, or {@link ResponseEntity} with HTTP 401 UNAUTHORIZED
     * @throws org.springframework.security.access.AccessDeniedException if user lacks PostRemovePrivilege
     * @throws com.openkoda.core.exception.ResourceNotFoundException if entity with given ID is not found
     */
    @PostMapping(value="{id}/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object remove(
            @PathVariable(name=ID) Long objectId,
            @PathVariable(name = ORGANIZATIONID, required = false) Long organizationId) {
        CRUDControllerConfiguration conf = controllers.apiCrudControllerConfigurationMap.get(key);
        PrivilegeBase privilege = conf.getPostRemovePrivilege();
        if (not(hasGlobalOrOrgPrivilege(privilege, organizationId))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return Flow.init(componentProvider, objectId)
                .then(a -> conf.getSecureRepository().deleteOne(objectId))
                .execute();
    }
}
