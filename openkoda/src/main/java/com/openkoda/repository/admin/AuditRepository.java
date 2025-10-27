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

import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.common.Audit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JPA repository managing Audit entities for application audit trail logging.
 * <p>
 * This repository provides queries for audit record retrieval by entity, action, timestamp,
 * and user. Used by {@link com.openkoda.core.audit.AuditInterceptor} for Hibernate
 * session-scoped audit capture. The repository extends {@link UnsecuredFunctionalRepositoryWithLongId}
 * to inherit base CRUD operations and implements {@link HasSecurityRules} to provide security
 * rule constants for method-level authorization.
 * </p>
 * 
 * <h2>Audit Trail Capabilities</h2>
 * <p>
 * This repository supports:
 * </p>
 * <ul>
 *   <li>Dynamic filtering with JPA Specifications</li>
 *   <li>Organization-scoped and user-scoped audit queries</li>
 *   <li>Full-text search across audit record index strings</li>
 *   <li>Independent transaction handling for audit persistence</li>
 * </ul>
 * 
 * <h2>Security Integration</h2>
 * <p>
 * Repository methods are protected by Spring Security {@code @PreAuthorize} annotations
 * using constants from {@link HasSecurityRules}:
 * </p>
 * <ul>
 *   <li>{@code CHECK_CAN_READ_SUPPORT_DATA} - For specification-based queries</li>
 *   <li>{@code CHECK_CAN_READ_ORG_AUDIT_JPQL} - For organization-scoped JPQL queries</li>
 * </ul>
 * 
 * <h2>Transaction Management</h2>
 * <p>
 * The {@link #saveAudit(Audit)} method uses {@code REQUIRES_NEW} propagation to ensure
 * audit records persist even when the calling transaction rolls back, maintaining audit
 * trail integrity.
 * </p>
 * 
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see Audit
 * @see com.openkoda.core.audit.AuditInterceptor
 * @see UnsecuredFunctionalRepositoryWithLongId
 * @see HasSecurityRules
 */
@Repository
public interface AuditRepository extends UnsecuredFunctionalRepositoryWithLongId<Audit>, HasSecurityRules {

    /**
     * Finds audit records matching the given JPA Specification with pagination support.
     * <p>
     * This method enables dynamic filtering of audit records based on arbitrary criteria
     * using Spring Data JPA Specifications. Access is restricted to users with support
     * data read privileges.
     * </p>
     * 
     * @param specification the JPA Specification defining filter criteria for audit records,
     *                     can combine multiple predicates for entity type, timestamp ranges,
     *                     user IDs, and organization IDs
     * @param pageable the pagination parameters including page number, size, and sort order
     * @return a Page containing audit records matching the specification, with pagination
     *         metadata including total elements and total pages
     * @throws org.springframework.security.access.AccessDeniedException if the current user
     *         lacks {@code CHECK_CAN_READ_SUPPORT_DATA} privilege
     * @see Specification
     * @see Page
     */
    //TODO: is it used anywhere?
    @PreAuthorize(CHECK_CAN_READ_SUPPORT_DATA)
    Page<Audit> findAll(Specification<Audit> specification, Pageable pageable);

    /**
     * Finds audit records for a specific organization with user-scoped filtering and text search.
     * <p>
     * This method retrieves audit records filtered by organization ID, current user ID (via SpEL),
     * and a case-insensitive search term. The query automatically enforces user-level security
     * by binding {@code principal.user.id} and applies organization-level audit read privileges.
     * </p>
     * 
     * <p>
     * The search term is matched against the audit record's {@code indexString} field using
     * case-insensitive LIKE with wildcard wrapping. Records with {@code organizationId = null}
     * are included in results (global audit entries).
     * </p>
     * 
     * <h3>Search Behavior</h3>
     * <ul>
     *   <li>Search is case-insensitive via {@code LOWER()} function</li>
     *   <li>Wildcards automatically prepended and appended to search term</li>
     *   <li>Empty search string returns all records for the organization and user</li>
     * </ul>
     * 
     * @param organizationId the ID of the organization to filter audit records, or null to
     *                      include only global audit entries (those without organization scope)
     * @param search the search term to match against audit record index strings, will be
     *              lower-cased and wrapped with wildcards for partial matching
     * @param pageable the pagination parameters including page number, size, and sort order
     * @return a Page of audit records matching the organization, user, and search criteria,
     *         with pagination metadata
     * @throws org.springframework.security.access.AccessDeniedException if the current user
     *         lacks organization audit read privileges per {@code CHECK_CAN_READ_ORG_AUDIT_JPQL}
     * @see Audit#getIndexString()
     */
    //fixme: extract security rule
    @Query("select o from Audit o where (o.organizationId = null OR o.organizationId = :organizationId) AND o.userId = ?#{principal.user.id} AND o.indexString like CONCAT('%' , LOWER(:search) , '%') AND " + CHECK_CAN_READ_ORG_AUDIT_JPQL)
    Page<Audit> findAllByOrganizationId(@Param("organizationId") Long organizationId, @Param("search") String search, Pageable pageable);

    /**
     * Saves an audit record in an independent transaction.
     * <p>
     * This method persists the provided audit entity using {@code REQUIRES_NEW} transaction
     * propagation, ensuring the audit record is committed independently of the calling
     * transaction. This guarantees audit trail integrity even when the caller's transaction
     * rolls back due to errors.
     * </p>
     * 
     * <h3>Transaction Isolation Rationale</h3>
     * <p>
     * The independent transaction behavior is critical for audit compliance:
     * </p>
     * <ul>
     *   <li>Audit records survive application transaction rollbacks</li>
     *   <li>Failed operations are still logged in the audit trail</li>
     *   <li>Maintains complete history of attempted changes</li>
     * </ul>
     * 
     * <p>
     * This method is typically invoked by {@link com.openkoda.core.audit.AuditInterceptor}
     * during Hibernate flush operations to capture entity changes.
     * </p>
     * 
     * @param a the Audit entity to persist, must have all required fields populated including
     *         entityKey, entityId, change description, and timestamp
     * @return the saved Audit entity with generated ID and any database-computed fields
     * @throws org.springframework.dao.DataIntegrityViolationException if required audit fields
     *         are missing or constraint violations occur
     * @see Propagation#REQUIRES_NEW
     * @see com.openkoda.core.audit.AuditInterceptor
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    default Audit saveAudit(Audit a) {
        return save(a);
    }
}
