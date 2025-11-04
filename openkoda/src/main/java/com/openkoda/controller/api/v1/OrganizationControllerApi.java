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

package com.openkoda.controller.api.v1;

import com.openkoda.controller.api.CRUDApiController;
import com.openkoda.model.Organization;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.openkoda.controller.common.URLConstants.*;

/**
 * RestController providing REST API v1 endpoints for Organization entity CRUD operations.
 * <p>
 * This controller is a minimal declarative adapter extending {@code CRUDApiController<Organization>}.
 * It is marked as {@code @Deprecated}, indicating a planned migration to a newer API version.
 * Coordinate with all API clients before removing this controller to prevent service disruptions.
 * 
 * <p>
 * The controller exposes organization CRUD operations under two URL paths for flexibility:
 * <ul>
 *   <li>{@code /api/v1/{organizationId}/organization} - Organization-scoped routes for operations within a specific organization context</li>
 *   <li>{@code /api/v1/organization} - Collection-scoped routes for operations across organizations</li>
 * </ul>
 * <p>
 * The controller is stateless with no additional methods beyond the inherited CRUD functionality.
 * All CRUD operations are provided by the parent {@link CRUDApiController} class.
 * The constructor calls {@code super(ORGANIZATION)} to bind the controller to the ORGANIZATION resource configuration.
 * 
 * <p>
 * Inherited endpoints include:
 * <ul>
 *   <li>GET /all - List all organizations</li>
 *   <li>GET /{id} - Retrieve organization details by ID</li>
 *   <li>POST /create - Create a new organization</li>
 *   <li>POST /{id}/update - Update an existing organization</li>
 *   <li>POST /{id}/remove - Delete an organization</li>
 * </ul>
 *
 * @see com.openkoda.controller.api.CRUDApiController
 * @see com.openkoda.model.Organization
 * @see com.openkoda.controller.common.URLConstants
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @deprecated Planned for migration to newer API version - coordinate with API clients before removal
 */
@Deprecated
@RestController
@RequestMapping({_API_V1_ORGANIZATION + _ORGANIZATIONID + _ORGANIZATION,_API_V1_ORGANIZATION})
public class OrganizationControllerApi extends CRUDApiController<Organization> {
    
    /**
     * Constructs the controller by binding it to the ORGANIZATION resource configuration.
     * <p>
     * Invokes the parent constructor with the ORGANIZATION constant to configure this controller
     * instance for Organization entity CRUD operations. The ORGANIZATION constant is defined in
     * {@link com.openkoda.controller.common.URLConstants} and determines routing behavior
     * and resource identification for all inherited CRUD endpoints.
     * 
     * <p>
     * This controller is a stateless singleton managed by Spring and is instantiated once
     * at application startup during component scanning.
     * 
     */
    public OrganizationControllerApi(){
        super(ORGANIZATION);
    }
}
