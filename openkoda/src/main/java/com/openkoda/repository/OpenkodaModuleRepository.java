package com.openkoda.repository;

import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.OpenkodaModule;
import org.springframework.stereotype.Repository;

/**
 * Repository managing OpenkodaModule entities representing installable application modules and plugins.
 * <p>
 * Manages OpenkodaModule entities defining modular application extensions with versioning, dependencies, 
 * and lifecycle hooks. Provides lookups for module activation status, version constraints, and dependency 
 * resolution. Used by CustomisationService for module registration and bootstrap. Supports module-scoped 
 * component cleanup operations via related ComponentEntityRepository interfaces.

 * <p>
 * This repository extends {@code UnsecuredFunctionalRepositoryWithLongId} to provide unsecured access to 
 * module metadata, as module registration occurs during application startup before security context 
 * initialization. Modules define reusable functionality including controllers, services, entities, and 
 * frontend resources that can be dynamically loaded and configured per organization.

 * <p>
 * Example usage:
 * <pre>{@code
 * OpenkodaModule module = repository.findByName("custom-integration");
 * boolean exists = repository.existsByName("custom-integration");
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see OpenkodaModule
 * @see com.openkoda.core.customisation.CustomisationService
 * // ComponentEntityRepository
 */
@Repository
public interface OpenkodaModuleRepository extends UnsecuredFunctionalRepositoryWithLongId<OpenkodaModule>, HasSecurityRules {

    /**
     * Finds an OpenkodaModule by its unique name.
     * <p>
     * Module names uniquely identify installable extensions and plugins within the OpenKoda platform.
     * This method is used during module registration to check for existing modules and avoid duplicate
     * registrations. Returns null if no module with the specified name exists.

     *
     * @param name the unique module name to search for (must not be null)
     * @return the OpenkodaModule with the specified name, or null if not found
     * @see OpenkodaModule#getName()
     */
    OpenkodaModule findByName(String name);

    /**
     * Checks whether an OpenkodaModule with the specified name exists.
     * <p>
     * Provides efficient existence checking without loading the full entity. Used during module
     * bootstrap to verify module registration status before attempting registration or initialization.

     *
     * @param name the unique module name to check (must not be null)
     * @return true if a module with the specified name exists, false otherwise
     * @see #findByName(String)
     */
    boolean existsByName(String name);
}
