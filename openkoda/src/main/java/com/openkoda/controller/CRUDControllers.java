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

import com.openkoda.core.customisation.CustomisationService;
import com.openkoda.core.form.ReflectionBasedEntityForm;
import com.openkoda.form.PageBuilderForm;
import com.openkoda.model.Privilege;
import com.openkoda.model.component.FrontendResource;
import com.openkoda.repository.SecureFormRepository;
import com.openkoda.repository.SecureFrontendResourceRepository;
import com.openkoda.repository.SecureServerJsRepository;
import com.openkoda.repository.ai.SecureQueryReportRepository;
import com.openkoda.repository.organization.SecureOrganizationRepository;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static com.openkoda.form.FrontendMappingDefinitions.*;
import static com.openkoda.model.Privilege.canCreateReports;
import static com.openkoda.model.Privilege.canReadReports;

/**
 * Bootstrap class that registers generic CRUD controller configurations at application startup.
 * <p>
 * Spring {@code @Component} implementing {@code @PostConstruct} initialization via
 * {@link CustomisationService} listener. Registers standard entity CRUD controllers in
 * {@link HtmlCRUDControllerConfigurationMap} including: organizations, page-builder,
 * frontend-resources, and query-reports. Each registration binds entity key to repository,
 * form class, frontend mapping definition, and privilege requirements.
 * </p>
 * <p>
 * Enables automatic CRUD endpoints for registered entities:
 * <ul>
 *   <li>{@code GET /{entity}/all} - List view with pagination</li>
 *   <li>{@code GET /{entity}/{id}} - Detail view</li>
 *   <li>{@code GET /{entity}/new} - Create form</li>
 *   <li>{@code POST /{entity}/save} - Submit create/edit form</li>
 *   <li>{@code GET /{entity}/{id}/edit} - Edit form</li>
 *   <li>{@code POST /{entity}/{id}/delete} - Delete entity</li>
 * </ul>
 * </p>
 * <p>
 * Custom modules can register additional CRUD controllers by listening to
 * {@link CustomisationService#registerOnApplicationStartListener(Consumer)} and calling
 * {@link HtmlCRUDControllerConfigurationMap#registerCRUDControllerBuilder}.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see HtmlCRUDControllerConfigurationMap
 * @see CRUDControllerHtml
 * @see CustomisationService
 */
@Component
public class CRUDControllers {

    @Inject
    CustomisationService customisationService;
    @Inject
    HtmlCRUDControllerConfigurationMap htmlCrudControllerConfigurationMap;
    @Inject
    SecureServerJsRepository serverJsRepository;
    @Inject
    SecureFrontendResourceRepository frontendResourceRepository;
    @Inject
    SecureOrganizationRepository secureOrganizationRepository;
    @Inject
    SecureFormRepository formRepository;
    @Inject
    SecureQueryReportRepository queryReportRepository;

    /**
     * Registers standard CRUD controllers at application startup.
     * <p>
     * Invoked by {@link CustomisationService#registerOnApplicationStartListener(Consumer)}
     * during {@code @PostConstruct} phase. Calls
     * {@link HtmlCRUDControllerConfigurationMap#registerCRUDController} for each standard
     * entity with appropriate privileges and configuration.
     * </p>
     * <p>
     * Registered entities and their configurations:
     * <ul>
     *   <li><b>organizations</b>: Organization management with {@code readOrgData} and
     *       {@code manageOrgData} privileges. Table fields: id, name</li>
     *   <li><b>page-builder</b>: Dashboard builder interface with {@code canAccessGlobalSettings}
     *       privilege. Filters DASHBOARD resource types. Navigation: configuration tab</li>
     *   <li><b>frontend-resources</b>: Frontend resource management. Table fields: name,
     *       includeInSitemap, type. Filters RESOURCE types. Navigation: resources tab</li>
     *   <li><b>query-reports</b>: AI-powered report generation with {@code canReadReports} and
     *       {@code canCreateReports} privileges. Table fields: name, organizationId. Navigation:
     *       reporting tab</li>
     * </ul>
     * </p>
     * <p>
     * <b>Note:</b> Registration order matters for dependencies. Foundational entities like
     * organizations are registered first to ensure availability for dependent entities.
     * </p>
     */
    @PostConstruct
    void init() {

        customisationService.registerOnApplicationStartListener(
                a -> htmlCrudControllerConfigurationMap.registerCRUDController(
                                organizationsApi, secureOrganizationRepository, ReflectionBasedEntityForm.class, Privilege.readOrgData,Privilege.manageOrgData)
                        .setGenericTableFields("id","name"));
        customisationService.registerOnApplicationStartListener(
                a -> htmlCrudControllerConfigurationMap.registerCRUDController(PAGE_BUILDER_FORM,
                                PageBuilderForm.pageBuilderForm, frontendResourceRepository, PageBuilderForm.class, Privilege.canAccessGlobalSettings,Privilege.canAccessGlobalSettings)
                        .setGenericTableFields("name")
                        .setNavigationFragment("navigation-fragments::configuration-nav-tabs('builder')")
                        .setMenuItem("configuration")
                        .setAdditionalPredicate((r, q, cb) -> cb.equal(r.get("resourceType"), FrontendResource.ResourceType.DASHBOARD)));
        customisationService.registerOnApplicationStartListener(
                a -> htmlCrudControllerConfigurationMap.registerCRUDController(
                                frontendResourceForm, frontendResourceRepository, ReflectionBasedEntityForm.class)
                        .setGenericTableFields("name","includeInSitemap","type")
                        .setNavigationFragment("navigation-fragments::configuration-nav-tabs('resources')")
                        .setMenuItem("resources")
                        .setTableView("frontend-resource-all")
                        .setAdditionalPredicate((root, query, cb) -> cb.and(cb.equal(root.get("resourceType"), FrontendResource.ResourceType.RESOURCE))));
        customisationService.registerOnApplicationStartListener(
                a -> htmlCrudControllerConfigurationMap.registerCRUDController(
                                queryReportForm, queryReportRepository, ReflectionBasedEntityForm.class,  canReadReports, canCreateReports)
                        .setGenericTableFields("name","organizationId")
                        .setTableView("frontend-resource/global/report-all")
                        .setSettingsView("report-data-table")
                        .setNavigationFragment("navigation-fragments::reporting-nav-tabs('reports')")
                        .setMenuItem("reports"));

    }

}
