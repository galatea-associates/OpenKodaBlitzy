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

package com.openkoda.repository;

import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.integration.model.configuration.IntegrationModuleOrganizationConfiguration;
import org.springframework.stereotype.Repository;

/**
 * Repository managing Integration configuration entities for third-party service connections.
 * <p>
 * Manages {@link IntegrationModuleOrganizationConfiguration} entities storing per-organization configuration
 * for external APIs including Trello, GitHub, Jira, and OAuth providers. Provides organization-scoped
 * lookups for integration settings including API credentials, webhook URLs, and consumer keys.
 * Used by integration services and OAuth callback controllers for third-party authentication and
 * data synchronization.
 * </p>
 * <p>
 * This repository extends {@link UnsecuredFunctionalRepositoryWithLongId} to provide standard CRUD
 * operations without automatic privilege enforcement, as integration configurations are typically
 * accessed in system-level contexts during OAuth flows and scheduled synchronization tasks.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see IntegrationModuleOrganizationConfiguration
 * @see com.openkoda.integration.service.IntegrationService
 */
@Repository
public interface IntegrationRepository extends UnsecuredFunctionalRepositoryWithLongId<IntegrationModuleOrganizationConfiguration>, HasSecurityRules {

    /**
     * Finds the integration configuration for a specific organization.
     * <p>
     * Retrieves the per-organization integration settings including API credentials, OAuth tokens,
     * webhook endpoints, and consumer keys for third-party services. Returns null if no integration
     * configuration exists for the specified organization.
     * </p>
     *
     * @param organizationId the unique identifier of the organization, must not be null
     * @return the integration configuration for the organization, or null if not found
     */
    IntegrationModuleOrganizationConfiguration findByOrganizationId(Long organizationId);
}
