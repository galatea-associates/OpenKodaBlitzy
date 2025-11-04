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

package com.openkoda.integration.service;

import com.openkoda.core.customisation.CoreSettledEvent;
import com.openkoda.core.helper.SpringProfilesHelper;
import com.openkoda.integration.controller.IntegrationComponentProvider;
import com.openkoda.integration.model.IntegrationPrivilege;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;

import static com.openkoda.core.lifecycle.BaseDatabaseInitializer.ROLE_ADMIN;

/**
 * Service for loading initial integration data and bootstrapping integration module during application startup.
 * <p>
 * This service implements an event listener for {@link CoreSettledEvent} that executes after the core
 * application context is fully initialized. It loads default configurations and registers privilege
 * definitions for the integration module.
 * 
 * <p>
 * During startup, this service seeds module privileges into the administrator role by collecting
 * all values from {@code IntegrationPrivilege.values()} into a Set and calling
 * {@code services.module.addModulePrivilegesToRole(ROLE_ADMIN, privilegesSet)}.
 * This ensures administrators have access to all integration features immediately after deployment.
 * 
 * <p>
 * Execution is guarded by {@link SpringProfilesHelper#isInitializationProfile()} to run only during
 * initialization profiles. This prevents duplicate privilege seeding on subsequent application restarts.
 * Disabling this service prevents automatic privilege seeding and requires manual role updates.
 * 
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2019-10-11
 * @see CoreSettledEvent
 * @see IntegrationPrivilege
 * @see SpringProfilesHelper#isInitializationProfile()
 */
@Service
public class IntegrationDataLoaderService extends IntegrationComponentProvider {

    /**
     * Updates organization administrator and user roles with integration module privileges.
     * <p>
     * This method executes during the application startup phase when the {@link CoreSettledEvent}
     * is published. It seeds all integration privileges into the administrator role to grant
     * immediate access to integration features.
     * 
     * <p>
     * The method guards execution with {@link SpringProfilesHelper#isInitializationProfile()} to
     * ensure privilege seeding occurs only during initialization profiles (such as drop_and_init_database).
     * This prevents duplicate privilege additions on normal application restarts.
     * 
     * <p>
     * Lifecycle: This service executes after core application initialization but before the application
     * is ready to serve requests. The privilege seeding is transactional and completes before user
     * authentication begins. Triggered by CoreSettledEvent provided by Spring event mechanism.
     * 
     *
     * @see CoreSettledEvent
     * @see IntegrationPrivilege
     * @see com.openkoda.core.service.module.ModuleService#addModulePrivilegesToRole(String, Set)
     */
    @EventListener(CoreSettledEvent.class)
    public void updateOrgAdminAndUserRole() {
        if (SpringProfilesHelper.isInitializationProfile()) {
            services.module.addModulePrivilegesToRole(ROLE_ADMIN, new HashSet<>(Arrays.asList(IntegrationPrivilege.values())));
        }
    }
}
