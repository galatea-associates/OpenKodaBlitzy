package com.openkoda.repository.ai;

import com.openkoda.model.common.SearchableRepositoryMetadata;
import com.openkoda.model.report.QueryReport;
import com.openkoda.repository.SecureRepository;
import org.springframework.stereotype.Repository;

import static com.openkoda.controller.common.URLConstants.QUERY_REPORT;

/**
 * Secure repository interface for {@link QueryReport} entities with privilege enforcement and search capabilities.
 * <p>
 * This interface extends {@link com.openkoda.repository.SecureRepository} to ensure all access to AI-generated query report data
 * is controlled by privilege-based security checks. It is marked with {@link SearchableRepositoryMetadata}
 * to enable admin search and indexing of QueryReport entities across the application.

 * <p>
 * The {@code @SearchableRepositoryMetadata} annotation configures repository discovery and search integration:
 * <ul>
 * <li>{@code entityKey = QUERY_REPORT}: Uses constant from {@code URLConstants} for entity identification in routing and admin interfaces</li>
 * <li>{@code entityClass = QueryReport.class}: Binds this repository to the {@link QueryReport} domain entity</li>
 * <li>{@code descriptionFormula}: SQL expression for human-readable descriptions in admin views, concatenating name, query text, and organization ID with null-safety via COALESCE</li>
 * <li>{@code searchIndexFormula}: SQL expression for normalized search index text, using lowercase conversion for case-insensitive search across report metadata</li>
 * </ul>

 * <p>
 * Usage context: This repository is used by admin interfaces to search and manage AI/ChatGPT-generated query reports.
 * The search formulas enable efficient full-text search across report names, query content, and organizational context.
 * All repository operations enforce privilege checks to ensure users can only access reports they are authorized to view.

 * <p>
 * Architecture notes: This is a marker interface with no custom query methods. Runtime behavior is provided by the
 * {@link com.openkoda.repository.SecureRepository} proxy wrapper which intercepts all Spring Data JPA operations to inject privilege validation.
 * The {@code @SearchableRepositoryMetadata} annotation is processed at application startup to register this repository
 * in the global search and indexing pipeline.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see QueryReport
 * @see SecureRepository
 * @see SearchableRepositoryMetadata
 * @see com.openkoda.repository.ai.QueryReportRepository
 */
@Repository
@SearchableRepositoryMetadata(
        entityKey = QUERY_REPORT,
        entityClass = QueryReport.class,
        descriptionFormula = "'name: ' || COALESCE(name, '') || ' query:  ' || COALESCE(query, '') || ' orgId:' || COALESCE(CAST (organization_id as text), '')",
        searchIndexFormula = "'name: ' || lower(COALESCE(name, '')) || ' query:  ' || lower(COALESCE(query, '')) || ' orgId:' || COALESCE(CAST (organization_id as text), '')"
)
public interface SecureQueryReportRepository extends SecureRepository<QueryReport> {

}
