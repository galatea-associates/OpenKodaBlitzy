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

package com.openkoda.controller;

import com.openkoda.core.form.*;
import com.openkoda.core.repository.common.ScopedSecureRepository;
import com.openkoda.model.Privilege;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.repository.SecureMapEntityRepository;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.function.Function;


/**
 * Abstract base class for storing and managing generic CRUD controller configurations in a registry pattern.
 * <p>
 * Extends {@code HashMap<String, CRUDControllerConfiguration>} to provide a centralized registry of controller
 * configurations. Each configuration binds a controller key to entity repository, form class, frontend mapping
 * definition, and privilege requirements. Registration occurs at application startup via {@link CRUDControllers}.
 * Supports runtime registration and unregistration for dynamic controller addition.
 * </p>
 * <p>
 * Used by {@link CRUDControllerHtml} and CRUDApiController to retrieve configurations by entity name. Controllers
 * query this map using the entity key (e.g., "users", "organizations") to obtain repository references, form
 * classes, and privilege settings for generic CRUD operations.
 * </p>
 * <p>
 * Thread-safety note: Uses plain {@code HashMap} without synchronization. Assumes registration occurs during
 * single-threaded application startup. Runtime modifications should be externally synchronized.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CRUDControllers
 * @see CRUDControllerConfiguration
 * @see CRUDControllerHtml
 */
public abstract class AbstractCRUDControllerConfigurationMap extends HashMap<String, CRUDControllerConfiguration> {

    /**
     * Autowired secure repository for MapEntity CRUD operations used by deprecated registration methods.
     *
     * @see com.openkoda.model.MapEntity
     */
    @Inject
    SecureMapEntityRepository secureMapEntityRepository;

    /**
     * Registers generic CRUD controller configuration with field builder pattern.
     * <p>
     * Creates {@link FrontendMappingDefinition} from builder function, constructs
     * {@link CRUDControllerConfiguration}, and registers in map under provided key.
     * Enables generic CRUD endpoints at paths like {@code /{key}/all}, {@code /{key}/{id}},
     * {@code /{key}/new}, etc.
     * </p>
     *
     * @param key Controller registry key, typically entity name in lowercase (e.g., "users", "organizations")
     * @param defaultReadPrivilege Minimum privilege required for read operations (list, detail views)
     * @param defaultWritePrivilege Minimum privilege required for write operations (create, edit, delete)
     * @param builder Form field definition builder function creating {@code FrontendMappingDefinition}
     * @param secureRepository Privilege-enforcing repository for entity data access
     * @param formClass Form class for request binding, typically {@code ReflectionBasedEntityForm} or custom form
     * @return Registered {@code CRUDControllerConfiguration} instance for fluent API chaining
     */
    public CRUDControllerConfiguration registerCRUDControllerBuilder(
            String key,
            PrivilegeBase defaultReadPrivilege,
            PrivilegeBase defaultWritePrivilege,
            Function<FormFieldDefinitionBuilderStart,
            FormFieldDefinitionBuilder> builder,
            ScopedSecureRepository secureRepository,
            Class formClass
            ) {

        CRUDControllerConfiguration controllerConfiguration = CRUDControllerConfiguration.getBuilder(key,
                FrontendMappingDefinition.createFrontendMappingDefinition(key, defaultReadPrivilege, defaultWritePrivilege, builder),
                secureRepository, formClass);
        this.put(key, controllerConfiguration);
        return controllerConfiguration;
    }

    /**
     * Registers CRUD controller configuration with pre-built FrontendMappingDefinition.
     * <p>
     * Extracts mapping key from {@code FrontendMappingDefinition}, creates
     * {@link CRUDControllerConfiguration}, and stores in registry. Use when
     * {@code FrontendMappingDefinition} already exists.
     * </p>
     *
     * @param frontendMappingDefinition Complete form field mapping with privilege configuration
     * @param secureRepository Secure repository for entity persistence
     * @param formClass Form class for request and response binding
     * @return Registered configuration instance
     */
    public CRUDControllerConfiguration registerCRUDController(
            FrontendMappingDefinition frontendMappingDefinition,
            ScopedSecureRepository secureRepository,
            Class formClass
            ) {
        String key = frontendMappingDefinition.getMappingKey();
        CRUDControllerConfiguration controllerConfiguration = CRUDControllerConfiguration.getBuilder(key,
                frontendMappingDefinition, secureRepository, formClass);
        this.put(key, controllerConfiguration);
        return controllerConfiguration;
    }

    /**
     * Removes CRUD controller configuration from registry.
     * <p>
     * Removes configuration entry, disabling CRUD endpoints for this entity.
     * Useful for dynamic controller lifecycle management.
     * </p>
     *
     * @param key Controller key to unregister
     */
    public void unregisterCRUDController(String key) {
        this.remove(key);
    }

    /**
     * Registers CRUD controller with default ReflectionBasedEntityForm.
     * <p>
     * Convenience method defaulting to {@link ReflectionBasedEntityForm} for automatic
     * entity-to-form binding using reflection.
     * </p>
     *
     * @param frontendMappingDefinition Complete form field mapping with privilege configuration
     * @param secureRepository Secure repository for entity persistence
     * @return Registered configuration
     */
    public CRUDControllerConfiguration registerCRUDController(
            FrontendMappingDefinition frontendMappingDefinition,
            ScopedSecureRepository secureRepository) {
        return registerCRUDController(frontendMappingDefinition, secureRepository, ReflectionBasedEntityForm.class);
    }

    /**
     * Registers CRUD controller with field builder and default ReflectionBasedEntityForm.
     * <p>
     * Convenience method for most common registration pattern. Defaults to
     * {@link ReflectionBasedEntityForm} for automatic entity-to-form binding.
     * </p>
     *
     * @param key Controller registry key, typically entity name in lowercase (e.g., "users", "organizations")
     * @param defaultReadPrivilege Minimum privilege required for read operations (list, detail views)
     * @param defaultWritePrivilege Minimum privilege required for write operations (create, edit, delete)
     * @param builder Form field definition builder function creating {@code FrontendMappingDefinition}
     * @param secureRepository Privilege-enforcing repository for entity data access
     * @return Registered configuration
     */
    public CRUDControllerConfiguration registerCRUDControllerBuilder(
            String key,
            PrivilegeBase defaultReadPrivilege,
            PrivilegeBase defaultWritePrivilege,
            Function<FormFieldDefinitionBuilderStart, FormFieldDefinitionBuilder> builder,
            ScopedSecureRepository secureRepository) {

        CRUDControllerConfiguration controllerConfiguration = CRUDControllerConfiguration.getBuilder(key,
                FrontendMappingDefinition.createFrontendMappingDefinition(key, defaultReadPrivilege, defaultWritePrivilege, builder),
                secureRepository, ReflectionBasedEntityForm.class);
        this.put(key, controllerConfiguration);
        return controllerConfiguration;
    }


    /**
     * Registers CRUD controller with privilege override.
     * <p>
     * Overrides default privileges from {@code FrontendMappingDefinition} with provided values.
     * </p>
     *
     * @param frontendMappingDefinition Complete form field mapping with privilege configuration
     * @param secureRepository Secure repository for entity persistence
     * @param formClass Form class for request and response binding
     * @param defaultReadPrivilege Override privilege required for read operations
     * @param defaultWritePrivilege Override privilege required for write operations
     * @return Registered configuration
     */
    public CRUDControllerConfiguration registerCRUDController(
            FrontendMappingDefinition frontendMappingDefinition,
            ScopedSecureRepository secureRepository,
            Class formClass,
            PrivilegeBase defaultReadPrivilege,
            PrivilegeBase defaultWritePrivilege) {

        String key = frontendMappingDefinition.getMappingKey();
        CRUDControllerConfiguration controllerConfiguration = CRUDControllerConfiguration.getBuilder(key,
                frontendMappingDefinition, secureRepository, formClass, defaultReadPrivilege, defaultWritePrivilege);
        this.put(key, controllerConfiguration);
        return controllerConfiguration;
    }

    /**
     * Registers CRUD controller with explicit key and privilege override.
     * <p>
     * Use when registry key should differ from mapping key. Overrides privileges from
     * {@code FrontendMappingDefinition} with provided values.
     * </p>
     *
     * @param key Explicit registry key (overrides {@code FrontendMappingDefinition.getMappingKey()})
     * @param frontendMappingDefinition Complete form field mapping with privilege configuration
     * @param secureRepository Secure repository for entity persistence
     * @param formClass Form class for request and response binding
     * @param defaultReadPrivilege Override privilege required for read operations
     * @param defaultWritePrivilege Override privilege required for write operations
     * @return Registered configuration
     */
    public CRUDControllerConfiguration registerCRUDController(
            String key,
            FrontendMappingDefinition frontendMappingDefinition,
            ScopedSecureRepository secureRepository,
            Class formClass,
            PrivilegeBase defaultReadPrivilege,
            PrivilegeBase defaultWritePrivilege) {

        CRUDControllerConfiguration controllerConfiguration = CRUDControllerConfiguration.getBuilder(key,
                frontendMappingDefinition, secureRepository, formClass, defaultReadPrivilege, defaultWritePrivilege);
        this.put(key, controllerConfiguration);
        return controllerConfiguration;
    }

    /**
     * Registers MapEntity CRUD controller with global settings privilege.
     * <p>
     * Legacy method hardcoded to {@code SecureMapEntityRepository}, {@code canAccessGlobalSettings}
     * privilege, and {@code MapEntityForm}. Retained for backward compatibility only.
     * </p>
     *
     * @param key Controller registry key
     * @param builder Form field definition builder function
     * @return Registered configuration for MapEntity
     * @deprecated Use {@link #registerCRUDControllerBuilder(String, PrivilegeBase, PrivilegeBase, Function, ScopedSecureRepository, Class)}
     *             with explicit repository and privileges
     */
    @Deprecated
    public CRUDControllerConfiguration registerCRUDControllerBuilder(
            String key,
            Function<FormFieldDefinitionBuilderStart,
            FormFieldDefinitionBuilder> builder
        ) {

        CRUDControllerConfiguration controllerConfiguration = CRUDControllerConfiguration.getBuilder(key,
                FrontendMappingDefinition.createFrontendMappingDefinition(key, Privilege.canAccessGlobalSettings, Privilege.canAccessGlobalSettings, builder),
                secureMapEntityRepository, MapEntityForm.class);
        this.put(key, controllerConfiguration);
        return controllerConfiguration;
    }

    /**
     * Registers pre-constructed CRUDControllerConfiguration.
     * <p>
     * Direct registration method when configuration is constructed externally.
     * </p>
     *
     * @param key Registry key
     * @param controllerConfiguration Complete configuration instance
     * @return Registered configuration
     */
    public CRUDControllerConfiguration registerCRUDControllerBuilder(
            String key,
            CRUDControllerConfiguration controllerConfiguration
            ) {
        this.put(key, controllerConfiguration);
        return controllerConfiguration;
    }

    /**
     * Case-insensitive configuration retrieval.
     * <p>
     * Tries lowercase key first, falls back to exact match. Enables flexible controller
     * key resolution.
     * </p>
     *
     * @param key Controller key in any case
     * @return Configuration for key (lowercase or exact match), or null if not found
     */
    public CRUDControllerConfiguration getIgnoreCase(String key) {
        return super.getOrDefault(StringUtils.lowerCase(key), super.get(key));
    }
}
