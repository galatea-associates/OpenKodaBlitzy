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

import com.openkoda.core.flow.Flow;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.component.FrontendResource;
import com.openkoda.repository.specifications.FrontendResourceSpecifications;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.openkoda.controller.common.URLConstants.*;

/**
 * REST controller providing component export and import functionality for YAML archives.
 * <p>
 * Enables bulk export of entities (FrontendResource, ServerJs, EventListener, SchedulerEvent, Form, Privilege)
 * as zipped YAML archives and transactional import from uploaded ZIP files. Export methods serialize entity
 * collections via {@code services.componentExport.exportToZip}. Import methods deserialize and load resources
 * via {@code services.zipComponentImport.loadResourcesFromZip}. All endpoints are guarded with
 * {@code @PreAuthorize} annotations requiring either {@code canManageOrgData} or {@code canManageBackend} privileges.
 * <p>
 * Typical usage for exporting all frontend resources:
 * <pre>
 * GET /html/frontendResource/export-yaml
 * </pre>
 * Returns a ZIP file containing YAML representations of all frontend resources.
 * <p>
 * Security notes: All endpoints require administrative privileges. Imported YAML is deserialized,
 * so ensure trusted sources only. Export operations use secure repositories to enforce privilege checks.
 * <p>
 * Thread-safety: Stateless controller, thread-safe. Import uses transactional service methods.
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.service.export.ComponentExportService
 * @see com.openkoda.service.export.ZipComponentImportService
 * @see ComponentProvider
 */
@RestController
@RequestMapping({_HTML})
public class ComponentsController extends ComponentProvider implements HasSecurityRules {


    /**
     * Exports all organization-scoped resources as a ZIP containing YAML files.
     * <p>
     * Collects all ServerJs, FrontendResource, EventListener, SchedulerEvent, Form, and Privilege entities
     * from secure repositories, serializes them to YAML format, and packages them into a downloadable ZIP archive.
     * 
     *
     * @return byte array containing ZIP archive with YAML representations of all resources
     * @throws org.springframework.security.access.AccessDeniedException if user lacks canManageOrgData privilege
     * @see com.openkoda.service.export.ComponentExportService#exportToZip(List)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ORG_DATA)
    @ResponseBody
    @GetMapping(value = _ORGANIZATION + _EXPORT_YAML + _ALL, produces = "application/zip")
    public byte[] exportAllResourceToZippedYamls(){
        debug("[exportAllResourceToZippedYamls]");
        List<Object> allResources = new ArrayList<>();
        allResources.addAll(repositories.secure.serverJs.findAll());
        allResources.addAll(repositories.secure.frontendResource.findAll());
        allResources.addAll(repositories.secure.eventListener.findAll());
        allResources.addAll(repositories.secure.scheduler.findAll());
        allResources.addAll(repositories.secure.form.findAll());
        allResources.addAll(repositories.secure.privilege.findAll());

        return services.componentExport.exportToZip(allResources).toByteArray();
    }

    /**
     * Exports all resources for a specific organization as a ZIP containing YAML files.
     * <p>
     * Retrieves ServerJs, FrontendResource, EventListener, SchedulerEvent, and Form entities
     * scoped to the specified organization, serializes them to YAML, and packages into a ZIP archive.
     * 
     *
     * @param organizationId the unique identifier of the organization whose resources to export
     * @return byte array containing ZIP archive with YAML representations of organization-specific resources
     * @throws org.springframework.security.access.AccessDeniedException if user lacks canManageOrgData privilege
     * @see com.openkoda.service.export.ComponentExportService#exportToZip(List)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ORG_DATA)
    @ResponseBody
    @GetMapping(value = _ORGANIZATION + _ORGANIZATIONID + _EXPORT_YAML + _ALL, produces = "application/zip")
    public byte[] exportAllForOrg(@PathVariable long organizationId){
        debug("[exportAllResourceToZippedYamls]");
        List<Object> allResources = new ArrayList<>();
        allResources.addAll(repositories.secure.serverJs.search(organizationId));
        allResources.addAll(repositories.secure.frontendResource.search(organizationId));
        allResources.addAll(repositories.secure.eventListener.search(organizationId));
        allResources.addAll(repositories.secure.scheduler.search(organizationId));
        allResources.addAll(repositories.secure.form.findAll());

        return services.componentExport.exportToZip(allResources).toByteArray();
    }

    /**
     * Exports all frontend resources as a ZIP containing YAML files.
     * <p>
     * Retrieves all FrontendResource entities, serializes them to YAML format, and packages
     * into a downloadable ZIP archive. Useful for backing up or migrating frontend configurations.
     * 
     *
     * @return byte array containing ZIP archive with YAML representations of all frontend resources
     * @throws org.springframework.security.access.AccessDeniedException if user lacks canManageBackend privilege
     * @see com.openkoda.service.export.ComponentExportService#exportToZip(List)
     */
    @GetMapping(value = _FRONTENDRESOURCE + _EXPORT_YAML, produces = "application/zip")
    @ResponseBody
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    public byte[] exportAllFrontendResourcesToZippedYamls(){
        debug("[exportAllFrontendResourcesToZippedYamls]");
        return services.componentExport.exportToZip(repositories.secure.frontendResource.findAll()).toByteArray();
    }

    /**
     * Exports all UI components as a ZIP containing YAML files.
     * <p>
     * Filters frontend resources by ResourceType.UI_COMPONENT, serializes matching entities to YAML,
     * and packages into a ZIP archive. UI components represent reusable UI building blocks.
     * 
     *
     * @return byte array containing ZIP archive with YAML representations of UI component resources
     * @throws org.springframework.security.access.AccessDeniedException if user lacks canManageBackend privilege
     * @see FrontendResource.ResourceType#UI_COMPONENT
     * @see com.openkoda.service.export.ComponentExportService#exportToZip(List)
     */
    @GetMapping(value =  _UI_COMPONENT + _EXPORT_YAML, produces = "application/zip")
    @ResponseBody
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    public byte[] exportAllUiComponentsToZippedYamls(){
        debug("[exportAllFrontendResourcesToZippedYamls]");
        return services.componentExport.exportToZip(repositories.secure.frontendResource.search(FrontendResourceSpecifications.searchByResourceType(FrontendResource.ResourceType.UI_COMPONENT))).toByteArray();
    }

    /**
     * Exports a single frontend resource as a ZIP containing one YAML file.
     * <p>
     * Retrieves the specified FrontendResource entity by ID, serializes it to YAML, and packages
     * into a ZIP archive. Useful for backing up individual configurations before modification.
     * 
     *
     * @param frontendResourceId the unique identifier of the frontend resource to export
     * @return byte array containing ZIP archive with YAML representation of the single frontend resource
     * @throws org.springframework.security.access.AccessDeniedException if user lacks canManageBackend privilege
     * @see com.openkoda.service.export.ComponentExportService#exportToZip(List)
     */
    @GetMapping(value = _FRONTENDRESOURCE + _ID + _EXPORT_YAML, produces = "application/zip")
    @ResponseBody
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    public byte[] exportFrontendResourceToZippedYaml(@PathVariable(ID) long frontendResourceId){
        debug("[exportFrontendResourceToZippedYaml]");
        return services.componentExport.exportToZip(Arrays.asList(repositories.secure.frontendResource.findOne(frontendResourceId))).toByteArray();
    }


    /**
     * Exports all server-side JavaScript code as a ZIP containing YAML files.
     * <p>
     * Retrieves all ServerJs entities, serializes them to YAML format, and packages into a ZIP archive.
     * ServerJs entities contain server-side JavaScript code executed in the GraalVM context.
     * 
     *
     * @return byte array containing ZIP archive with YAML representations of all ServerJs entities
     * @throws org.springframework.security.access.AccessDeniedException if user lacks canManageBackend privilege
     * @see com.openkoda.service.export.ComponentExportService#exportToZip(List)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @ResponseBody
    @GetMapping(value = _SERVERJS + _EXPORT_YAML, produces = "application/zip")
    public byte[] exportAllServerJsToZippedYamls(){
        debug("[exportAllServerJsToZippedYamls]");
        return services.componentExport.exportToZip(repositories.secure.serverJs.findAll()).toByteArray();
    }

    /**
     * Exports a single server-side JavaScript entity as a ZIP containing one YAML file.
     * <p>
     * Retrieves the specified ServerJs entity by ID, serializes it to YAML, and packages into a ZIP archive.
     * Useful for backing up individual server-side scripts before modification.
     * 
     *
     * @param serverJsId the unique identifier of the ServerJs entity to export
     * @return byte array containing ZIP archive with YAML representation of the single ServerJs entity
     * @throws org.springframework.security.access.AccessDeniedException if user lacks canManageBackend privilege
     * @see com.openkoda.service.export.ComponentExportService#exportToZip(List)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @ResponseBody
    @GetMapping(value = _SERVERJS + _ID + _EXPORT_YAML, produces = "application/zip")
    public byte[] exportServerJsToZippedYaml(@PathVariable(ID) long serverJsId){
        debug("[exportServerJsToZippedYaml]");
        return services.componentExport.exportToZip(Arrays.asList(repositories.secure.serverJs.findOne(serverJsId))).toByteArray();
    }

    /**
     * Exports all form definitions as a ZIP containing YAML files.
     * <p>
     * Retrieves all Form entities, serializes them to YAML format, and packages into a ZIP archive.
     * Form entities define dynamic form structures and validation rules.
     * 
     *
     * @return byte array containing ZIP archive with YAML representations of all Form entities
     * @throws org.springframework.security.access.AccessDeniedException if user lacks canManageBackend privilege
     * @see com.openkoda.service.export.ComponentExportService#exportToZip(List)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @GetMapping(value = _FORM + _EXPORT_YAML, produces = "application/zip")
    @ResponseBody
    public byte[] exportAllFormsToZippedYamls(){
        debug("[exportAllFormsToZippedYamls]");
        return services.componentExport.exportToZip(repositories.secure.form.findAll()).toByteArray();
    }

    /**
     * Exports a single form definition as a ZIP containing one YAML file.
     * <p>
     * Retrieves the specified Form entity by ID, serializes it to YAML, and packages into a ZIP archive.
     * Useful for backing up individual form configurations before modification.
     * 
     *
     * @param formId the unique identifier of the Form entity to export
     * @return byte array containing ZIP archive with YAML representation of the single Form entity
     * @throws org.springframework.security.access.AccessDeniedException if user lacks canManageBackend privilege
     * @see com.openkoda.service.export.ComponentExportService#exportToZip(List)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @GetMapping(value = _FORM + _ID + _EXPORT_YAML, produces = "application/zip")
    @ResponseBody
    public byte[] exportFormToZippedYaml(@PathVariable(ID) long formId){
        debug("[exportFormToZippedYaml]");
        return services.componentExport.exportToZip(Arrays.asList(repositories.secure.form.findOne(formId))).toByteArray();
    }

    /**
     * Exports all scheduler configurations as a ZIP containing YAML files.
     * <p>
     * Retrieves all SchedulerEvent entities, serializes them to YAML format, and packages into a ZIP archive.
     * SchedulerEvent entities define scheduled tasks and their execution parameters.
     * 
     *
     * @return byte array containing ZIP archive with YAML representations of all SchedulerEvent entities
     * @throws org.springframework.security.access.AccessDeniedException if user lacks canManageBackend privilege
     * @see com.openkoda.service.export.ComponentExportService#exportToZip(List)
     */
    @GetMapping(value = _SCHEDULER + _EXPORT_YAML, produces = "application/zip")
    @ResponseBody
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    public byte[] exportAllSchedulersToZippedYamls(){
        debug("[exportAllSchedulersToZippedYamls]");
        return services.componentExport.exportToZip(repositories.secure.scheduler.findAll()).toByteArray();
    }

    /**
     * Exports a single scheduler configuration as a ZIP containing one YAML file.
     * <p>
     * Retrieves the specified SchedulerEvent entity by ID, serializes it to YAML, and packages into a ZIP archive.
     * Useful for backing up individual scheduler configurations before modification.
     * 
     *
     * @param schedulerId the unique identifier of the SchedulerEvent entity to export
     * @return byte array containing ZIP archive with YAML representation of the single SchedulerEvent entity
     * @throws org.springframework.security.access.AccessDeniedException if user lacks canManageBackend privilege
     * @see com.openkoda.service.export.ComponentExportService#exportToZip(List)
     */
    @GetMapping(value = _SCHEDULER + _ID + _EXPORT_YAML, produces = "application/zip")
    @ResponseBody
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    public byte[] exportSchedulerToZippedYaml(@PathVariable(ID) long schedulerId){
        debug("[exportSchedulerToZippedYaml]");
        return services.componentExport.exportToZip(Arrays.asList(repositories.secure.scheduler.findOne(schedulerId))).toByteArray();
    }

    /**
     * Exports all event listener configurations as a ZIP containing YAML files.
     * <p>
     * Retrieves all EventListener entities, serializes them to YAML format, and packages into a ZIP archive.
     * EventListener entities define event-driven automation rules and handlers.
     * 
     *
     * @return byte array containing ZIP archive with YAML representations of all EventListener entities
     * @throws org.springframework.security.access.AccessDeniedException if user lacks canManageBackend privilege
     * @see com.openkoda.service.export.ComponentExportService#exportToZip(List)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @ResponseBody
    @GetMapping(value = _EVENTLISTENER + _EXPORT_YAML, produces = "application/zip")
    public byte[] exportAllEventListenersToZippedYamls(){
        debug("[exportAllEventListenersToZippedYamls]");
        return services.componentExport.exportToZip(repositories.secure.eventListener.findAll()).toByteArray();
    }

    /**
     * Exports a single event listener configuration as a ZIP containing one YAML file.
     * <p>
     * Retrieves the specified EventListener entity by ID, serializes it to YAML, and packages into a ZIP archive.
     * Useful for backing up individual event listener configurations before modification.
     * 
     *
     * @param eventListenerId the unique identifier of the EventListener entity to export
     * @return byte array containing ZIP archive with YAML representation of the single EventListener entity
     * @throws org.springframework.security.access.AccessDeniedException if user lacks canManageBackend privilege
     * @see com.openkoda.service.export.ComponentExportService#exportToZip(List)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @ResponseBody
    @GetMapping(value = _EVENTLISTENER + _ID + _EXPORT_YAML, produces = "application/zip")
    public byte[] exportEventListenerToZippedYaml(@PathVariable(ID) long eventListenerId){
        debug("[exportEventListenerToZippedYaml]");
        return services.componentExport.exportToZip(Arrays.asList(repositories.secure.eventListener.findOne(eventListenerId))).toByteArray();
    }

    /**
     * Imports components from an uploaded ZIP archive containing YAML files transactionally.
     * <p>
     * Accepts a multipart file upload, deserializes YAML entity definitions using
     * {@code services.zipComponentImport.loadResourcesFromZip}, validates entities, and persists them
     * within a single transaction. The delete parameter controls whether existing entities are removed
     * before import. On error, the entire transaction rolls back.
     * 
     * <p>
     * Example usage:
     * <pre>
     * POST /html/component/import/zip
     * Content-Type: multipart/form-data
     * Parameters: file=[ZIP archive], delete=false
     * </pre>
     * Returns a view model containing import results and validation messages.
     * 
     *
     * @param file the uploaded ZIP file containing YAML entity definitions
     * @param delete if true, removes existing components before import (default: false)
     * @return Flow execution result with "components" view and import log
     * @see com.openkoda.service.export.ZipComponentImportService#loadResourcesFromZip(MultipartFile, boolean)
     * @see Flow
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @Transactional
    @RequestMapping(value = _COMPONENT + _IMPORT + _ZIP, method = RequestMethod.POST)
    public Object importComponentsZip(@RequestParam("file") MultipartFile file, @RequestParam(value = "delete", defaultValue = "false") Boolean delete) {
        debug("[importComponentsZip]");
        return Flow.init()
                .thenSet(importLog ,a -> services.zipComponentImport.loadResourcesFromZip(file, delete))
                .execute()
                .mav("components");
    }
}
