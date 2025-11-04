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

package com.openkoda.controller.api.v2;

import com.openkoda.controller.api.CRUDApiController;
import com.openkoda.model.component.ServerJs;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.openkoda.controller.common.URLConstants.*;

/**
 * REST API v2 controller providing JSON CRUD endpoints for ServerJs entities.
 * <p>
 * This controller is a mirror-pattern adapter for the ServerJs domain. It extends
 * {@code CRUDApiController<ServerJs>} and exposes standard CRUD operations under
 * dual request mapping paths: an organization-scoped path composed from
 * {@code _API_V2_ORGANIZATION_ORGANIZATIONID + _SERVERJS} and a global v2 path
 * composed from {@code _API_V2 + _SERVERJS}. The controller is a stateless adapter
 * with no additional methods or fields beyond the inherited functionality.
 * 
 * <p>
 * The constructor calls {@code super(SERVERJS)} to pass the logical resource identifier
 * constant into the base {@link CRUDApiController}. All CRUD operations are inherited
 * from the parent controller, and this class exists solely to register routes and bind
 * the generic CRUD implementation to the ServerJs domain type and canonical URL constants.
 * 
 * 
 * <b>Request Mapping Base Paths</b>
 * <ul>
 *   <li>Organization-scoped: {@code /api/v2/{organizationId}/server-js}</li>
 *   <li>Global v2: {@code /api/v2/server-js}</li>
 * </ul>
 * 
 * <b>Inherited Endpoints</b>
 * <ul>
 *   <li>{@code GET /api/v2/server-js/all} - List ServerJs scripts with pagination</li>
 *   <li>{@code GET /api/v2/server-js/{id}} - Get single ServerJs script by ID</li>
 *   <li>{@code POST /api/v2/server-js/create} - Create new ServerJs from JSON</li>
 *   <li>{@code POST /api/v2/server-js/{id}/update} - Update existing script</li>
 *   <li>{@code POST /api/v2/server-js/{id}/remove} - Delete script</li>
 * </ul>
 * 
 * <b>API Response Format</b>
 * <p>
 * All endpoints return JSON responses wrapped in standard API result structure:
 * 
 * <pre>
 * // List response
 * {"success": true, "data": [{"id": 1, "name": "script.js"}]}
 * 
 * // Single response
 * {"success": true, "data": {"id": 1, "name": "script.js", "code": "..."}}
 * 
 * // Create/update response
 * {"success": true, "data": {"id": 2, "name": "new-script.js"}}
 * </pre>
 * 
 * <b>Security</b>
 * <p>
 * All endpoints require authentication and are subject to authorization checks. Multi-tenancy
 * is enforced through organization-scoped paths when the {@code organizationId} path variable
 * is present. Unauthorized requests return HTTP 401 or 403 status codes.
 * 
 * 
 * <b>Operational Considerations</b>
 * <p>
 * Removing, renaming, or changing the package of this class will break compile-time references
 * and prevent Spring from registering the endpoints. Altering {@code @RequestMapping} values
 * changes the API contract. Changing the constant passed to {@code super(...)} can break
 * base-controller binding including resource resolution, permissions, and logging. Maintain
 * compatibility when evolving URL constants or {@link CRUDApiController} APIs. Coordinate
 * changes with configuration updates and testing.
 * 
 * 
 * <b>Maintainability</b>
 * <p>
 * This class is a low-risk routing adapter but critical to the public API surface for v2.
 * Include it in CI regression suites and API contract tests. Changes to this controller
 * should be accompanied by updates to integration tests, API documentation, and release notes.
 * 
 * 
 * <b>Thread Safety</b>
 * <p>
 * Stateless singleton - thread-safe. Each request creates a new Flow pipeline for execution.
 * 
 * 
 * @see CRUDApiController
 * @see ServerJs
 * @since 1.7.1
 * @version 1.7.1
 * @author OpenKoda Team
 */
@RestController
@RequestMapping({_API_V2_ORGANIZATION_ORGANIZATIONID + _SERVERJS,_API_V2 + _SERVERJS})
public class ServerJsControllerApi extends CRUDApiController<ServerJs> {

    /**
     * Constructs controller binding to SERVERJS configuration.
     * <p>
     * No-argument constructor calling {@code super(SERVERJS)} to pass the logical
     * resource identifier for configuration resolution. The configuration is retrieved
     * from {@code ApiCRUDControllerConfigurationMap} and contains the necessary
     * components for CRUD operations including the SecureRepository, form class,
     * FrontendMappingDefinition, and privilege requirements for ServerJs entities.
     * 
     * <p>
     * Stateless - thread-safe for concurrent requests.
     * 
     */
    public ServerJsControllerApi() {
        super(SERVERJS);
    }
}