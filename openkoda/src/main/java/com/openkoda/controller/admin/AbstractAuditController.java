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

import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.model.Privilege;
import org.springframework.data.domain.Pageable;

/**
 * Abstract base controller providing Flow-based helper methods for audit trail browsing and search functionality.
 * <p>
 * This stateless abstract controller implements audit entity retrieval with pagination and full-text search capabilities.
 * All queries execute via {@code repositories.secure.audit} ensuring privilege enforcement. Designed for reuse by concrete
 * controllers that handle HTTP bindings (e.g., {@code @GetMapping}), view resolution, and audit content download operations.
 * </p>
 * <p>
 * Audit trail context: Used for browsing system audit records including entity changes, user actions, and system events.
 * Supports full-text search across audit content fields for filtering by entity type, operation, and user.
 * Integrates with Spring Data {@link Pageable} for efficient large dataset browsing with sorting.
 * </p>
 * <p>
 * Architectural patterns:
 * <ul>
 *   <li>Flow pipeline composition: Uses {@code Flow.init().thenSet(auditPage, ...).execute()} pattern</li>
 *   <li>Privilege enforcement: All queries execute via {@code repositories.secure.audit} ensuring user has audit read privileges</li>
 *   <li>Pagination: Integrates with Spring Data Pageable for efficient large dataset browsing</li>
 *   <li>Model key convention: {@code auditPage} contains {@code Page<Audit>} result for view rendering</li>
 *   <li>Search flexibility: Accepts empty/null search parameter for unfiltered results</li>
 * </ul>
 * </p>
 * <p>
 * Thread-safety: Stateless, thread-safe. Contains no instance fields.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AuditController for concrete HTTP endpoint implementation
 * @see com.openkoda.model.Audit for audit entity structure
 * @see com.openkoda.repository.admin.AuditRepository for secure audit repository
 */
public class AbstractAuditController extends AbstractController {

    /**
     * Retrieves paginated audit records with optional search filtering.
     * <p>
     * Executes a privilege-enforced query via {@code repositories.secure.audit.search()} to retrieve audit records
     * matching the provided search criteria. The search parameter enables full-text filtering across audit content
     * fields including entity type, operation, and associated user information.
     * </p>
     * <p>
     * Flow execution pattern:
     * <pre>
     * Flow.init()
     *     .thenSet(auditPage, a -&gt; repositories.secure.audit.search(...))
     *     .execute()
     * </pre>
     * </p>
     * <p>
     * Model keys populated:
     * <ul>
     *   <li>{@code auditPage}: Contains {@code Page<Audit>} result with audit records matching search criteria</li>
     * </ul>
     * </p>
     * <p>
     * Typical usage by concrete controllers:
     * <pre>
     * PageModelMap model = findAll(auditPageable, "User");
     * return mav("audit-all").addObject(model);
     * </pre>
     * </p>
     *
     * @param auditPageable pagination and sorting parameters, typically sorted by ID descending for newest-first display.
     *                      Qualifies page/size/sort request parameters to avoid conflicts with other paginated data.
     *                      Must not be {@code null}.
     * @param search full-text search query string that searches across audit content fields for entity type, operation,
     *               and user filtering. May be {@code null} or empty string for unfiltered results (returns all audit
     *               records subject to privilege enforcement).
     * @return {@link PageModelMap} with model key {@code "auditPage"} containing {@code Page<Audit>} result.
     *         The Page includes total elements count, current page number, and list of audit entities.
     *         Used by concrete controllers to display audit listing UI with filtering and pagination controls.
     * @see AuditController#getAll(Pageable, String, javax.servlet.http.HttpServletRequest) for HTTP endpoint usage
     * @see com.openkoda.repository.admin.AuditRepository#search(String, Long, Long, Pageable) for underlying query
     */
    protected PageModelMap findAll(Pageable auditPageable, String search){
        return Flow.init()
                .thenSet( auditPage, a -> repositories.secure.audit.search(search, null, null, auditPageable))
                .execute();
    }
}
