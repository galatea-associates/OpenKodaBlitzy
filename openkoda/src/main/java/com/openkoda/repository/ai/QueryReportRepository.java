package com.openkoda.repository.ai;


import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.report.QueryReport;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link QueryReport} entities used in AI/ML-generated query reports.
 * <p>
 * This repository interface provides standard CRUD operations, paging, and sorting capabilities for
 * AI-generated report templates and execution history. It serves as the data access layer for the
 * AI-powered report generation pipeline, storing report definitions created by ChatGPT and other AI
 * services, as well as the execution history and results of those reports.

 * <p>
 * The interface extends {@link UnsecuredFunctionalRepositoryWithLongId} to inherit generic repository
 * operations including functional query patterns (findOne, count, exists) and entity CRUD methods.
 * It also implements {@link HasSecurityRules} to participate in the application's security contract,
 * though this repository variant is unsecured and does not enforce privilege checks on operations.
 * For privilege-enforced access to QueryReport entities, use {@link SecureQueryReportRepository}.

 * <p>
 * This repository defines no custom query methods; all functionality is inherited from the base
 * repository interfaces. The actual runtime implementation is provided by Spring Data's JPA
 * repository proxy mechanism backed by Hibernate ORM. Typical usage involves dependency injection
 * into services such as ChatGPTService for AI report generation workflows.

 * <p>
 * Example usage:
 * <pre>
 * QueryReportRepository repository = ...;
 * QueryReport report = repository.findOne(reportId);
 * </pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see QueryReport
 * @see SecureQueryReportRepository
 * @see com.openkoda.service.openai.ChatGPTService
 * @see UnsecuredFunctionalRepositoryWithLongId
 * @see HasSecurityRules
 */
@Repository
public interface QueryReportRepository extends UnsecuredFunctionalRepositoryWithLongId<QueryReport>, HasSecurityRules {
}
