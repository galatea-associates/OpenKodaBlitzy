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

package com.openkoda.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import static com.openkoda.core.controller.generic.AbstractController._AUDIT;
import static com.openkoda.core.controller.generic.AbstractController._HTML;
import static com.openkoda.core.security.HasSecurityRules.CHECK_CAN_READ_SUPPORT_DATA;

/**
 * MVC controller exposing HTTP endpoints for audit trail browsing, search, and audit content retrieval.
 * <p>
 * This concrete {@code @Controller} implements the admin audit interface, mapping HTTP requests to 
 * Flow-based helper methods inherited from {@link AbstractAuditController}. Provides paginated audit 
 * listing with full-text search ({@link #getAll}) and individual audit content download ({@link #downloadContent}).
 * All endpoints require audit read privileges enforced via {@code @PreAuthorize}.
 * 
 * <p>
 * Audit records track entity changes, user actions, and system events for compliance and debugging. The controller
 * displays audit records with entity type, operation (CREATE/UPDATE/DELETE), user, timestamp, and JSON content.
 * All queries use {@code repositories.secure.audit} for privilege-enforced data access, ensuring only users with
 * {@code CHECK_CAN_READ_SUPPORT_DATA} privilege can view audit trails.
 * 
 * <p>
 * <b>Request Mapping:</b> Base path {@code /html/audit} (constructed from {@code AbstractController._HTML + _AUDIT})
 * 
 * <p>
 * <b>Security:</b> Requires {@code CHECK_CAN_READ_SUPPORT_DATA} privilege for all endpoints
 * 
 * <p>
 * <b>Response Types:</b> Returns HTML views via {@code ModelAndView} for listing UI, plain text {@code @ResponseBody} 
 * for content download
 * 
 * <p>
 * <b>Thread-safety:</b> This controller is stateless and thread-safe.
 * 
 * <p>
 * Example usage:
 * <pre>{@code
 * GET /html/audit/all?audit_search=User&page=0&size=20
 * GET /html/audit/123/content
 * }</pre>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractAuditController for Flow-based audit retrieval implementation
 * @see com.openkoda.model.common.Audit for audit entity structure
 * @see com.openkoda.repository.admin.AuditRepository for secure audit repository
 */

@Controller
@RequestMapping(_HTML + _AUDIT)
public class AuditController extends AbstractAuditController {

    /**
     * Retrieves paginated audit records with optional full-text search.
     * <p>
     * This endpoint provides the main audit trail browsing interface for administrators. Results are paginated
     * with default descending sort by ID (newest records first). The search parameter filters audit records 
     * across content and entity fields. Executes {@code findAll(auditPageable, search)} helper method inherited
     * from {@link AbstractAuditController} and renders results in the "audit-all" Thymeleaf view.
     * 
     * <p>
     * <b>HTTP Mapping:</b> {@code GET /html/audit/all}
     * 
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_READ_SUPPORT_DATA} privilege via {@code @PreAuthorize}
     * 
     * <p>
     * Example requests:
     * <pre>{@code
     * GET /html/audit/all?audit_search=User&page=0&size=20
     * GET /html/audit/all?page=1&sort=id,asc
     * }</pre>
     * 
     *
     * @param auditPageable pagination and sorting parameters, qualified as "audit" to distinguish from other
     *                      page parameters. Default sort by ID descending via {@code @SortDefault}. Supports
     *                      standard Spring Data pagination (page, size, sort parameters)
     * @param search full-text search query string that searches across audit content and entity fields. 
     *               Defaults to empty string if not provided (unfiltered results). Parameter name is "audit_search"
     * @param request {@link HttpServletRequest} context (unused in current implementation, available for 
     *                request attribute access if needed)
     * @return {@link Object} resolving to {@code ModelAndView} with view name "audit-all". Model is populated 
     *         by {@code findAll(auditPageable, search)} with key "auditPage" containing {@code Page<Audit>} result.
     *         Used by admin audit trail UI with filtering, pagination, and sorting controls
     */
    @PreAuthorize(CHECK_CAN_READ_SUPPORT_DATA)
    @GetMapping(_ALL)
    public Object getAll(
            @SortDefault(sort = ID, direction = Sort.Direction.DESC)
            @Qualifier("audit") Pageable auditPageable,
            @RequestParam(required = false, defaultValue = "", name = "audit_search") String search,
            HttpServletRequest request) {
        debug("[getAll] search: {}", search);
        return  findAll(auditPageable, search)
            .mav("audit-all");
    }


    /**
     * Downloads individual audit record content as plain text.
     * <p>
     * This endpoint retrieves the detailed content field of a specific audit record, typically containing
     * a JSON representation of the entity state at the time of the audited operation. The content field
     * captures the complete entity snapshot for CREATE/UPDATE operations or the entity state before DELETE.
     * Executes {@code repositories.secure.audit.findOne(auditId).getContent()} with privilege enforcement.
     * 
     * <p>
     * <b>HTTP Mapping:</b> {@code GET /html/audit/{id}/content} where {@code {id}} is the audit record identifier
     * 
     * <p>
     * <b>Security:</b> Requires {@code CHECK_CAN_READ_SUPPORT_DATA} privilege via {@code @PreAuthorize}.
     * Privilege is enforced by the secure repository ensuring only authorized users can access audit content
     * 
     * <p>
     * <b>Response Type:</b> {@code @ResponseBody} returns raw String content (typically JSON)
     * 
     * <p>
     * Example request:
     * <pre>{@code
     * GET /html/audit/123/content
     * }</pre>
     * 
     *
     * @param auditId unique identifier of the audit record to retrieve. Path variable mapped from {@code {id}}
     *                in the URL pattern. Must be a valid Long value representing an existing audit record ID
     * @return {@link String} containing the audit content field, typically a JSON representation of the entity
     *         state. Used by admin UI "View Content" button for detailed audit inspection and debugging
     * @throws org.springframework.security.access.AccessDeniedException if user lacks {@code CHECK_CAN_READ_SUPPORT_DATA} privilege
     * @throws jakarta.persistence.EntityNotFoundException if audit record with specified ID does not exist
     */
    @GetMapping(_ID + _CONTENT)
    @PreAuthorize(CHECK_CAN_READ_SUPPORT_DATA)
    @ResponseBody
    public String downloadContent(@PathVariable(ID) Long auditId) {
        debug("[downloadContent] auditId: {}", auditId);
        return repositories.secure.audit.findOne(auditId).getContent();
    }

}
