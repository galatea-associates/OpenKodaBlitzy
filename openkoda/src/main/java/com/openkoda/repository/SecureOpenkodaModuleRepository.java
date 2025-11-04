package com.openkoda.repository;

import com.openkoda.model.OpenkodaModule;
import com.openkoda.model.common.SearchableRepositoryMetadata;
import org.springframework.stereotype.Repository;

import static com.openkoda.controller.common.URLConstants.MODULE;


/**
 * Secure repository marker interface for OpenkodaModule entities with SearchableRepositoryMetadata.
 * <p>
 * Extends {@link com.openkoda.repository.SecureRepository}&lt;OpenkodaModule&gt; to provide privilege-enforced operations
 * for managing application modules and plugins. This interface serves as a typed repository contract
 * enabling module discovery, versioning queries, and access-controlled CRUD operations for installable
 * OpenKoda modules.
 * 
 * <p>
 * All operations inherited from SecureRepository enforce privilege validation based on the current
 * user's access rights. The {@link SearchableRepositoryMetadata} annotation configures this repository
 * for dynamic discovery by {@code SearchableRepositories}, enabling module search indexing and
 * metadata-driven UI generation for module management interfaces.
 * 
 * <p>
 * This repository is primarily used by {@code CustomisationService} for module registration,
 * bootstrap operations, and lifecycle management. Module management UIs leverage the searchable
 * metadata for displaying and filtering installed modules with proper privilege enforcement.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SecureRepository
 * @see OpenkodaModule
 * @see SearchableRepositoryMetadata
 * @see com.openkoda.core.customisation.CustomisationService
 */
@Repository
@SearchableRepositoryMetadata(
        entityKey = MODULE,
        descriptionFormula = "(''||name)",
        entityClass = OpenkodaModule.class
)
public interface SecureOpenkodaModuleRepository extends SecureRepository<OpenkodaModule> {

}
