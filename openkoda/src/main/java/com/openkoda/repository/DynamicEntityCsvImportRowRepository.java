package com.openkoda.repository;

import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.model.DynamicEntityCsvImportRow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository managing DynamicEntityCsvImportRow entities for CSV import batch tracking.
 * <p>
 * This repository extends UnsecuredFunctionalRepositoryWithLongId to provide Spring Data JPA
 * persistence operations for CSV import row entities. It tracks individual CSV import rows 
 * with upload batch identifiers, enabling import progress monitoring and batch management.

 * <p>
 * The repository exposes findLastUploadId() which returns a Long scalar representing the 
 * most recent batch ID. Note that the query implementation uses LIMIT syntax which is 
 * dialect-sensitive and optimized for PostgreSQL. The Long return type is nullable to 
 * handle empty result sets when no imports have been performed.

 * <p>
 * Example usage:
 * <pre>
 * Long lastId = repository.findLastUploadId();
 * </pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see DynamicEntityCsvImportRow
 * @see UnsecuredFunctionalRepositoryWithLongId
 */
@Repository
public interface DynamicEntityCsvImportRowRepository extends UnsecuredFunctionalRepositoryWithLongId<DynamicEntityCsvImportRow>{

   /**
    * Retrieves the most recent CSV import batch upload identifier.
    * <p>
    * This method executes a native JPQL query to fetch the maximum uploadId from all 
    * DynamicEntityCsvImportRow entities. The query orders results by uploadId in descending 
    * order and limits the result to a single row.

    * <p>
    * Note: This query uses LIMIT clause syntax which is dialect-sensitive. The current 
    * implementation uses PostgreSQL LIMIT syntax. For other database dialects, consider 
    * using Spring Data's Top or First keywords in method naming conventions.

    *
    * @return Latest upload ID as Long, or null if no CSV imports exist in the database
    */
   @Query("select uploadId from DynamicEntityCsvImportRow order by uploadId desc limit 1")
   Long findLastUploadId();
}
