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
import com.openkoda.model.component.FrontendResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.openkoda.controller.common.URLConstants.*;

/**
 * REST API v2 controller providing JSON CRUD endpoints for FrontendResource entities.
 * <p>
 * This class is a thin adapter extending {@link CRUDApiController} parameterized with
 * {@link FrontendResource}. It exposes standard CRUD operations under dual URL paths:
 * organization-scoped ({@code /api/v2/{organizationId}/frontend-resource}) and global v2
 * ({@code /api/v2/frontend-resource}). All request handling is delegated to the parent
 * {@code CRUDApiController}, which implements pagination, search, create, update, and delete
 * operations. This controller is discovered by Spring component scanning at startup and
 * instantiated as a stateless singleton bean.
 * 
 * 
 * <b>Request Mapping Base Paths</b>
 * <ul>
 *   <li>{@code /api/v2/{organizationId}/frontend-resource} - Organization-scoped endpoints</li>
 *   <li>{@code /api/v2/frontend-resource} - Global v2 endpoints</li>
 * </ul>
 * 
 * <b>Inherited CRUD Endpoints</b>
 * <p>
 * All endpoints are inherited from {@link CRUDApiController} and return JSON responses
 * wrapped in {@code ApiResult}:
 * 
 * <ul>
 *   <li><b>GET /api/v2/frontend-resource/all</b> - List FrontendResources with pagination and search</li>
 *   <li><b>GET /api/v2/frontend-resource/{id}</b> - Get single FrontendResource by ID</li>
 *   <li><b>POST /api/v2/frontend-resource/create</b> - Create new FrontendResource from JSON body</li>
 *   <li><b>POST /api/v2/frontend-resource/{id}/update</b> - Update existing FrontendResource</li>
 *   <li><b>POST /api/v2/frontend-resource/{id}/remove</b> - Delete FrontendResource by ID</li>
 * </ul>
 * <p>
 * Organization-scoped variants replace {@code /api/v2} with {@code /api/v2/{organizationId}}.
 * 
 * 
 * <b>API Response Format</b>
 * <p><b>Success response (list):</b></p>
 * <pre>{@code
 * { "data": [...], "success": true }
 * }</pre>
 * 
 * <p><b>Success response (single):</b></p>
 * <pre>{@code
 * { "data": {...}, "success": true }
 * }</pre>
 * 
 * <p><b>Error response:</b></p>
 * <pre>{@code
 * { "error": "...", "success": false }
 * }</pre>
 * 
 * <b>Usage Example</b>
 * <pre>{@code
 * curl -X GET http://localhost:8080/api/v2/frontend-resource/all
 * curl -X GET http://localhost:8080/api/v2/frontend-resource/123
 * curl -X POST http://localhost:8080/api/v2/frontend-resource/create -d '{...}'
 * }</pre>
 * 
 * <b>Security</b>
 * <p>
 * Authentication and authorization are enforced by the parent controller. Multi-tenancy
 * isolation is applied through organization-scoped paths. Requests without valid
 * authentication return HTTP 401, and requests without required privileges return HTTP 403.
 * 
 * 
 * <b>Operational Considerations</b>
 * <p>
 * This controller is a low-risk routing adapter. Removing or renaming it breaks API contracts.
 * Changing the {@code @RequestMapping} paths or the constant passed to {@code super(...)}
 * affects endpoint registration and configuration resolution. Coordinate changes with API
 * documentation, client applications, and integration tests.
 * 
 * 
 * <b>Maintainability</b>
 * <p>
 * This class contains no business logic and serves only to bind the generic CRUD controller
 * to the FrontendResource domain type. Treat it as part of the public API surface. Include
 * in CI regression suites and API contract tests. The stateless design ensures thread safety
 * for concurrent requests.
 * 
 *
 * @see CRUDApiController
 * @see FrontendResource
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
@RestController
@RequestMapping({_API_V2_ORGANIZATION_ORGANIZATIONID + _FRONTENDRESOURCE, _API_V2 + _FRONTENDRESOURCE})
public class FrontendResourceControllerApi extends CRUDApiController<FrontendResource> {

    /**
     * Constructs the controller and binds it to the FRONTENDRESOURCE configuration key.
     * <p>
     * This no-argument constructor calls {@code super(FRONTENDRESOURCE)}, passing the logical
     * resource identifier to the base {@link CRUDApiController}. The base controller resolves
     * configuration from {@code ApiCRUDControllerConfigurationMap}, which contains the
     * {@code SecureRepository<FrontendResource>}, form class, {@code FrontendMappingDefinition},
     * and privilege requirements for this resource type.
     * 
     * <p>
     * <b>Note:</b> This controller is stateless with no instance fields, ensuring thread safety
     * for concurrent HTTP requests.
     * 
     */
    public FrontendResourceControllerApi() {
        super(FRONTENDRESOURCE);
    }
}

