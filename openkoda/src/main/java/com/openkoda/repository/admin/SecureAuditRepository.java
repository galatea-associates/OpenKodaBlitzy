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

package com.openkoda.repository.admin;

import com.openkoda.model.common.Audit;
import com.openkoda.model.common.SearchableRepositoryMetadata;
import com.openkoda.repository.SecureRepository;
import org.springframework.stereotype.Repository;

import static com.openkoda.controller.common.URLConstants.AUDIT;

/**
 * Secure repository marker interface for Audit entities with SearchableRepositoryMetadata.
 * <p>
 * This interface extends {@link com.openkoda.repository.SecureRepository} to provide privilege-enforced access to
 * {@link Audit} entities. All repository operations automatically enforce security checks
 * based on the current user's privileges and organization context.
 * 
 * 
 * <p>
 * The {@link SearchableRepositoryMetadata} annotation configures the search index formula
 * used for full-text searching across audit records. The formula creates a normalized,
 * lower-cased composite index from multiple audit fields including:
 * 
 * <ul>
 *   <li>Entity key and entity ID</li>
 *   <li>Change description</li>
 *   <li>Organization ID (zero-padded for sorting)</li>
 *   <li>IP address of the request</li>
 *   <li>User ID who performed the action</li>
 *   <li>Request correlation ID for tracing</li>
 * </ul>
 * 
 * <p>
 * This repository inherits standard CRUD operations from {@link com.openkoda.repository.SecureRepository} including:
 * {@code findOne()}, {@code findAll()}, {@code save()}, and {@code delete()}, all of which
 * enforce privilege checks on audit log access operations.
 * 
 * 
 * <b>Security Enforcement</b>
 * <p>
 * Access to audit records is restricted based on:
 * 
 * <ul>
 *   <li>User privileges for reading support/audit data</li>
 *   <li>Organization-scoped filtering</li>
 *   <li>User-scoped filtering for sensitive operations</li>
 * </ul>
 * 
 * <b>Search Capabilities</b>
 * <p>
 * The search index formula enables searching audit records by:
 * 
 * <pre>
 * // Search by user ID: "modifiedby:123"
 * // Search by organization: "orgid:456"
 * // Search by entity: "User:789"
 * </pre>
 * 
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see SecureRepository
 * @see Audit
 * @see SearchableRepositoryMetadata
 * @see com.openkoda.repository.admin.AuditRepository
 */
@Repository
@SearchableRepositoryMetadata(
        entityKey = AUDIT,
        entityClass = Audit.class,
        searchIndexFormula = """
            lower(COALESCE(entity_key, '') || ':' || COALESCE(entity_id, -1) || ' ' || change || ' ' || 
            (LPAD(coalesce(organization_id, 0)||'', 5, '0') || '/' || id)|| ' ' || COALESCE(ip_address, '') || ' '
            || ' modifiedby:' || COALESCE(CAST (user_id as text), '') || ' orgid:' || COALESCE(CAST (organization_id as text), '')
            || ' ' || request_id)
            """
)
public interface SecureAuditRepository extends SecureRepository<Audit> {


}
