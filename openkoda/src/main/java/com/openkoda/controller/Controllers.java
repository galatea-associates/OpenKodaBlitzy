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

import com.openkoda.controller.admin.AuditController;
import com.openkoda.controller.organization.OrganizationControllerHtml;
import com.openkoda.controller.user.UserControllerHtml;
import com.openkoda.core.controller.frontendresource.FrontendResourceControllerHtml;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

/**
 * Spring bean aggregator providing centralized access to controller instances and configuration maps.
 * <p>
 * Convenience registry collecting common controller beans and CRUD configuration maps for easy injection.
 * Provides single {@code @Autowired} field instead of multiple controller injections. Contains
 * {@link HtmlCRUDControllerConfigurationMap} and {@link ApiCRUDControllerConfigurationMap} for generic
 * controller lookup. Used by controllers needing inter-controller communication or configuration access.
 * 
 * <p>
 * Example usage:
 * <pre>
 * {@code @Autowired Controllers controllers;
 * CRUDControllerConfiguration config = controllers.htmlCrudControllerConfigurationMap.get("users");}
 * </pre>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see HtmlCRUDControllerConfigurationMap
 * @see ApiCRUDControllerConfigurationMap
 */
@Component("AllControllers")
public class Controllers {

    /**
     * Organization management controller for HTML-based user interfaces.
     * <p>
     * Handles organization CRUD operations, tenant provisioning, and organization-scoped operations
     * via Spring MVC endpoints.
     * 
     *
     * @see OrganizationControllerHtml
     */
    @Inject
    public OrganizationControllerHtml organization;

    /**
     * User management controller for HTML-based user interfaces.
     * <p>
     * Handles user CRUD operations, authentication, role assignments, and user profile management
     * via Spring MVC endpoints.
     * 
     *
     * @see UserControllerHtml
     */
    @Inject
    public UserControllerHtml user;

    /**
     * Audit trail controller for viewing and managing application audit logs.
     * <p>
     * Provides access to audit records captured by the auditing subsystem, including entity changes,
     * user actions, and system events.
     * 
     *
     * @see AuditController
     */
    @Inject
    public AuditController audit;

    /**
     * Frontend resource controller for managing UI components and assets.
     * <p>
     * Handles frontend resource operations including custom UI components, templates, and
     * frontend mapping definitions.
     * 
     *
     * @see FrontendResourceControllerHtml
     */
    @Inject
    public FrontendResourceControllerHtml frontendResource;

    /**
     * Configuration registry for HTML-based generic CRUD controllers.
     * <p>
     * Maps entity names to their CRUD controller configuration, enabling dynamic lookup of HTML
     * controller configurations for generic entity operations.
     * 
     *
     * @see HtmlCRUDControllerConfigurationMap
     * @see CRUDControllerHtml
     */
    @Inject
    public HtmlCRUDControllerConfigurationMap htmlCrudControllerConfigurationMap;

    /**
     * Configuration registry for API-based generic CRUD controllers.
     * <p>
     * Maps entity names to their CRUD controller configuration, enabling dynamic lookup of REST API
     * controller configurations for generic entity operations.
     * 
     *
     * @see ApiCRUDControllerConfigurationMap
     * @see com.openkoda.controller.api.CRUDApiController
     */
    @Inject
    public ApiCRUDControllerConfigurationMap apiCrudControllerConfigurationMap;



}
