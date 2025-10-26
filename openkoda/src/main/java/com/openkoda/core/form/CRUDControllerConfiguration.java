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

package com.openkoda.core.form;

import com.openkoda.controller.common.PageAttributes;
import com.openkoda.core.flow.PageAttr;
import com.openkoda.core.repository.common.ScopedSecureRepository;
import com.openkoda.dto.OrganizationRelatedObject;
import com.openkoda.model.MapEntity;
import com.openkoda.model.Privilege;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.common.SearchableOrganizationRelatedEntity;
import com.openkoda.repository.SearchableRepositories;
import org.apache.groovy.util.Arrays;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Builder and factory for configuring generic CRUD controller operations with privilege management,
 * view rendering, and reflective form/entity instantiation.
 * <p>
 * Encapsulates all configuration needed for a generic CRUD controller: entity/form classes and their
 * cached reflective Constructors, secure repository, frontend mapping definition, privilege requirements
 * for each CRUD operation (getAllPrivilege, postNewPrivilege, etc.), Thymeleaf view/fragment paths
 * (tableView, settingsView, formNewFragment), generic table field names, filter field names, and JPA
 * Specification predicates. Supports both standard JavaBean entities and MapEntity dynamic entities
 * (detected via entityClass.equals(MapEntity.class)).
 * </p>
 * <p>
 * Created via static getBuilder() factory methods. Used by MapFormArgumentResolver to instantiate
 * forms for controller methods, and by generic CRUD controllers to create entities, apply specifications,
 * and render views.
 * </p>
 * <p>
 * Thread-safety: Mutable after construction via fluent setters; intended for single-threaded
 * configuration then read-only use in request handling.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * CRUDControllerConfiguration<UserDTO, User, UserForm> config =
 *     CRUDControllerConfiguration.getBuilder("user", mapping, repository, UserForm.class)
 *         .setGetAllPrivilege(Privilege.canReadBackend)
 *         .setTableView("custom-table")
 *         .setGenericTableFields("name", "email", "active");
 * }</pre>
 * </p>
 *
 * @param <D> DTO type extending OrganizationRelatedObject for form binding
 * @param <E> entity type extending SearchableOrganizationRelatedEntity for persistence
 * @param <F> form type extending AbstractOrganizationRelatedEntityForm for request handling
 * @see MapFormArgumentResolver
 * @see AbstractOrganizationRelatedEntityForm
 * @see FrontendMappingDefinition
 * @see ScopedSecureRepository
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class CRUDControllerConfiguration<D extends OrganizationRelatedObject, E extends SearchableOrganizationRelatedEntity, F extends AbstractOrganizationRelatedEntityForm<D, E>> {
    
    /**
     * Field name constant for entity ID used in getReportFormFieldNames().
     */
    public static final String ID = "id";
    
    /**
     * Field name constant for organization ID used in getReportFormFieldNames().
     */
    public static final String ORGANIZATION_ID = "organizationId";
    
        private final String key;
        private final FrontendMappingDefinition frontendMappingDefinition;
        private final Class<E> entityClass;
        private final boolean isMapEntity;
        private final ScopedSecureRepository<E> secureRepository;
        private final Constructor<E> entityConstructor;
        private Class<F> formClass;

        private boolean frontendMappingDefinitionInConstructor = false;
        private Constructor<F> formConstructor;
        private Constructor<F> formEntityConstructor;
        @SuppressWarnings("unchecked")
        private Class<D> dtoClass = (Class<D>) OrganizationRelatedMap.class;
        private PrivilegeBase defaultControllerPrivilege = Privilege.isUser;
        private PrivilegeBase getAllPrivilege;
        private PrivilegeBase getNewPrivilege;
        private PrivilegeBase getSettingsPrivilege;
        private PrivilegeBase postNewPrivilege;
        private PrivilegeBase postSavePrivilege;
        private PrivilegeBase postRemovePrivilege;
        private PageAttr<?> entityPageAttribute = PageAttributes.organizationRelatedEntityPage;
        private PageAttr<?> entityAttribute = PageAttributes.organizationRelatedEntity;
        private PageAttr<?> formAttribute = PageAttributes.organizationRelatedForm;
        private String tableView = "generic-all";
        private String tableViewWebEndpoint = null;
        private String settingsView = "generic-settings";
        private String readView = "generic-view";
        private String formNewFragment = "generic-settings-entity-form::generic-settings-form-new";
        private String formSuccessFragment = "generic-settings-entity-form::generic-settings-form-success";
        private String formErrorFragment = "generic-settings-entity-form::generic-settings-form-error";
        private String navigationFragment;
        private String menuItem;
        private Specification<E> additionalPredicate;

        private String[] genericTableFields;
        private String[] filterFields;
        private Long organizationId;

        /**
         * Private constructor for creating CRUDControllerConfiguration without initial privileges.
         * <p>
         * Initializes the configuration by extracting the entity class from repository annotation,
         * detecting if it's a MapEntity, caching the entity constructor for reflective instantiation,
         * and detecting appropriate form constructors via {@link #detectFormConstructor()}.
         * </p>
         *
         * @param key unique identifier for this configuration (typically entity name)
         * @param frontendMappingDefinition form field definitions for rendering and validation
         * @param secureRepository privilege-enforcing repository for entity data access
         * @param formClass form class to instantiate for request handling
         * @throws RuntimeException wrapping NoSuchMethodException if entity lacks (Long organizationId) constructor
         */
        private CRUDControllerConfiguration(String key, FrontendMappingDefinition frontendMappingDefinition,
                                            ScopedSecureRepository<E> secureRepository,
                                            Class<F> formClass) {
                try {
                        this.key = key;
                        this.frontendMappingDefinition = frontendMappingDefinition;
                        this.secureRepository = secureRepository;
                        this.entityClass = (Class<E>) SearchableRepositories.getGlobalSearchableRepositoryAnnotation(secureRepository).entityClass();
                        this.isMapEntity = this.entityClass.equals(MapEntity.class);
                        this.entityConstructor = this.entityClass.getConstructor(Long.class);
                        this.formClass = formClass;
                        detectFormConstructor();
                } catch (NoSuchMethodException | SecurityException e) {
                        throw new RuntimeException(e);
                }
        }
        
        /**
         * Private constructor for creating CRUDControllerConfiguration with initial read/write privileges.
         * <p>
         * Initializes the configuration by extracting the entity class from repository annotation,
         * detecting if it's a MapEntity, caching the entity constructor for reflective instantiation,
         * detecting appropriate form constructors, and setting default privileges for all CRUD operations.
         * The readPrivilege is applied to getAllPrivilege and getSettingsPrivilege, while writePrivilege
         * is applied to getNewPrivilege, postNewPrivilege, postSavePrivilege, and postRemovePrivilege.
         * </p>
         *
         * @param key unique identifier for this configuration (typically entity name)
         * @param frontendMappingDefinition form field definitions for rendering and validation
         * @param secureRepository privilege-enforcing repository for entity data access
         * @param formClass form class to instantiate for request handling
         * @param readPrivilege privilege required for read operations (getAll, getSettings)
         * @param writePrivilege privilege required for write operations (new, save, remove)
         * @throws RuntimeException wrapping NoSuchMethodException if entity lacks (Long organizationId) constructor
         */
        private CRUDControllerConfiguration(String key, FrontendMappingDefinition frontendMappingDefinition,
                                            ScopedSecureRepository<E> secureRepository,
                                            Class<F> formClass, PrivilegeBase readPrivilege, PrivilegeBase writePrivilege) {
                try {
                        this.key = key;
                        this.frontendMappingDefinition = frontendMappingDefinition;
                        this.secureRepository = secureRepository;
                        this.entityClass = (Class<E>) SearchableRepositories.getGlobalSearchableRepositoryAnnotation(secureRepository).entityClass();
                        this.isMapEntity = this.entityClass.equals(MapEntity.class);
                        this.entityConstructor = this.entityClass.getConstructor(Long.class);
                        this.formClass = formClass;
                        detectFormConstructor();
                        this.getAllPrivilege = readPrivilege;
                        this.getNewPrivilege = writePrivilege;
                        this.getSettingsPrivilege = readPrivilege;
                        this.postNewPrivilege = writePrivilege;
                        this.postSavePrivilege = writePrivilege;
                        this.postRemovePrivilege = writePrivilege;
                } catch (NoSuchMethodException | SecurityException e) {
                        throw new RuntimeException(e);
                }
        }
        
        /**
         * Factory method for creating a new CRUDControllerConfiguration instance without initial privileges.
         * <p>
         * Creates a builder-style configuration object that can be further customized via fluent setter
         * methods. Privileges default to {@code Privilege.isUser} for all CRUD operations unless
         * explicitly overridden.
         * </p>
         *
         * @param key unique identifier for this configuration (typically entity name)
         * @param frontendMappingDefinition form field definitions for rendering and validation
         * @param secureRepository privilege-enforcing repository for entity data access
         * @param formClass form class to instantiate for request handling
         * @return new CRUDControllerConfiguration instance ready for fluent configuration
         */
        public static CRUDControllerConfiguration getBuilder(
                String key,
                FrontendMappingDefinition frontendMappingDefinition,
                ScopedSecureRepository secureRepository,
                Class formClass) {
                return new CRUDControllerConfiguration(key, frontendMappingDefinition,
                        secureRepository, formClass);
        }

        /**
         * Factory method for creating a new CRUDControllerConfiguration instance with initial privileges.
         * <p>
         * Creates a builder-style configuration object with default read/write privileges applied to
         * CRUD operations. The readPrivilege applies to list and settings views, while writePrivilege
         * applies to create, update, and delete operations. Can be further customized via fluent setter
         * methods to override specific operation privileges.
         * </p>
         *
         * @param key unique identifier for this configuration (typically entity name)
         * @param frontendMappingDefinition form field definitions for rendering and validation
         * @param secureRepository privilege-enforcing repository for entity data access
         * @param formClass form class to instantiate for request handling
         * @param readPrivilege privilege required for read operations (getAll, getSettings)
         * @param writePrivilege privilege required for write operations (new, save, remove)
         * @return new CRUDControllerConfiguration instance ready for fluent configuration
         */
        public static CRUDControllerConfiguration getBuilder(
                String key,
                FrontendMappingDefinition frontendMappingDefinition,
                ScopedSecureRepository secureRepository,
                Class formClass,
                PrivilegeBase readPrivilege,
                PrivilegeBase writePrivilege){
                return new CRUDControllerConfiguration(key, frontendMappingDefinition,
                        secureRepository, formClass, readPrivilege, writePrivilege);
        }

        /**
         * Sets the form class and re-detects form constructors.
         * <p>
         * Updates the form class used for instantiation and re-scans for appropriate constructors
         * (with/without FrontendMappingDefinition parameter). Use this method when changing the
         * form implementation after initial configuration.
         * </p>
         *
         * @param formClass new form class to use for request handling
         * @return this configuration for fluent method chaining
         * @throws RuntimeException wrapping NoSuchMethodException if form lacks required constructor
         */
        public CRUDControllerConfiguration<D, E, F> setFormClass(Class<F> formClass) {
                this.formClass = formClass;
                try {
                        detectFormConstructor();
                } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                }
                return this;
        }

        /**
         * Sets the DTO class used for form binding.
         * <p>
         * Overrides the default OrganizationRelatedMap DTO class. Use this method when your form
         * uses a custom DTO class instead of the default Map-based DTO.
         * </p>
         *
         * @param dtoClass DTO class for form data binding
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setDtoClass(Class<D> dtoClass) {
                this.dtoClass = dtoClass;
                return this;
        }

        /**
         * Sets the default privilege for all CRUD operations.
         * <p>
         * This privilege serves as a fallback when specific operation privileges (getAllPrivilege,
         * postNewPrivilege, etc.) are not explicitly set. Individual operation privileges can
         * still override this default.
         * </p>
         *
         * @param defaultControllerPrivilege fallback privilege for all operations
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setDefaultControllerPrivilege(PrivilegeBase defaultControllerPrivilege) {
                this.defaultControllerPrivilege = defaultControllerPrivilege;
                return this;
        }

        /**
         * Sets the privilege required for GET /all (list) operations.
         *
         * @param getAllPrivilege privilege required to list entities
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setGetAllPrivilege(PrivilegeBase getAllPrivilege) {
                this.getAllPrivilege = getAllPrivilege;
                return this;
        }

        /**
         * Sets the privilege required for GET /new (create form) operations.
         *
         * @param getNewPrivilege privilege required to display entity creation form
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setGetNewPrivilege(PrivilegeBase getNewPrivilege) {
                this.getNewPrivilege = getNewPrivilege;
                return this;
        }

        /**
         * Sets the privilege required for GET /settings (entity settings) operations.
         *
         * @param getSettingsPrivilege privilege required to view entity settings
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setGetSettingsPrivilege(PrivilegeBase getSettingsPrivilege) {
                this.getSettingsPrivilege = getSettingsPrivilege;
                return this;
        }

        /**
         * Sets the privilege required for POST /new (entity creation) operations.
         *
         * @param postNewPrivilege privilege required to create new entities
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setPostNewPrivilege(PrivilegeBase postNewPrivilege) {
                this.postNewPrivilege = postNewPrivilege;
                return this;
        }

        /**
         * Sets the privilege required for POST /save (entity update) operations.
         *
         * @param postSavePrivilege privilege required to save/update entities
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setPostSavePrivilege(PrivilegeBase postSavePrivilege) {
                this.postSavePrivilege = postSavePrivilege;
                return this;
        }

        /**
         * Sets the privilege required for POST /remove (entity deletion) operations.
         *
         * @param postRemovePrivilege privilege required to remove/delete entities
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setPostRemovePrivilege(PrivilegeBase postRemovePrivilege) {
                this.postRemovePrivilege = postRemovePrivilege;
                return this;
        }

        /**
         * Sets the Thymeleaf template name for the table/list view.
         *
         * @param tableView Thymeleaf template name (default: "generic-all")
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setTableView(String tableView) {
                this.tableView = tableView;
                return this;
        }

        /**
         * Sets the web endpoint path for dynamically rendered table views.
         *
         * @param tableViewWebEndpoint endpoint URL for dynamic table rendering
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setTableViewWebEndpoint(String tableViewWebEndpoint) {
                this.tableViewWebEndpoint = tableViewWebEndpoint;
                return this;
        }

        /**
         * Returns the field names to display in table views.
         *
         * @return array of field names for table columns
         */
        public String[] getTableFormFieldNames() {
                return genericTableFields;
        }

        /**
         * Returns the field names for report generation, including ID and ORGANIZATION_ID.
         * <p>
         * Prepends the standard ID and ORGANIZATION_ID fields to the generic table fields
         * for comprehensive report output.
         * </p>
         *
         * @return array of field names with ID and ORGANIZATION_ID prepended
         */
        public String[] getReportFormFieldNames() {
                return Arrays.concat(new String[]{ID, ORGANIZATION_ID}, genericTableFields);
        }

        /**
         * Returns the field names available for filtering in list views.
         *
         * @return array of filterable field names
         */
        public String[] getFilterFieldNames() {
                return filterFields;
        }

        /**
         * Sets the fields to display as columns in table/list views.
         *
         * @param genericTableFields field names for table columns
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setGenericTableFields(String... genericTableFields) {
                this.genericTableFields = genericTableFields;
                return this;
        }

        /**
         * Sets the fields available for filtering in list views.
         *
         * @param filterFields field names that can be used as filters
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setFilterFields(String... filterFields) {
                this.filterFields = filterFields;
                return this;
        }

        /**
         * Sets the Thymeleaf template name for the settings/edit view.
         *
         * @param settingsView Thymeleaf template name (default: "generic-settings")
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setSettingsView(String settingsView) {
                this.settingsView = settingsView;
                return this;
        }

        /**
         * Sets the Thymeleaf template name for the read-only entity detail view.
         *
         * @param readView Thymeleaf template name (default: "generic-view")
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setReadView(String readView) {
                this.readView = readView;
                return this;
        }

        /**
         * Returns the Thymeleaf fragment path for the new entity form.
         *
         * @return Thymeleaf fragment specification (template::fragment)
         */
        public String getFormNewFragment() {
                return formNewFragment;
        }

        /**
         * Sets the Thymeleaf fragment path for rendering the new entity form.
         *
         * @param formNewFragment Thymeleaf fragment specification (default: "generic-settings-entity-form::generic-settings-form-new")
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setFormNewFragment(String formNewFragment) {
                this.formNewFragment = formNewFragment;
                return this;
        }

        /**
         * Returns the Thymeleaf fragment path for the form success message.
         *
         * @return Thymeleaf fragment specification (template::fragment)
         */
        public String getFormSuccessFragment() {
                return formSuccessFragment;
        }

        /**
         * Sets the Thymeleaf fragment path for rendering form success messages.
         *
         * @param formSuccessFragment Thymeleaf fragment specification (default: "generic-settings-entity-form::generic-settings-form-success")
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setFormSuccessFragment(String formSuccessFragment) {
                this.formSuccessFragment = formSuccessFragment;
                return this;
        }

        /**
         * Returns the Thymeleaf fragment path for the form error message.
         *
         * @return Thymeleaf fragment specification (template::fragment)
         */
        public String getFormErrorFragment() {
                return formErrorFragment;
        }

        /**
         * Sets the Thymeleaf fragment path for rendering form error messages.
         *
         * @param formErrorFragment Thymeleaf fragment specification (default: "generic-settings-entity-form::generic-settings-form-error")
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setFormErrorFragment(String formErrorFragment) {
                this.formErrorFragment = formErrorFragment;
                return this;
        }

        /**
         * Returns the Thymeleaf fragment path for the navigation menu.
         *
         * @return Thymeleaf fragment specification (template::fragment)
         */
        public String getNavigationFragment() {
                return navigationFragment;
        }

        /**
         * Sets the Thymeleaf fragment path for rendering navigation menus.
         *
         * @param navigationFragment Thymeleaf fragment specification
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setNavigationFragment(String navigationFragment) {
                this.navigationFragment = navigationFragment;
                return this;
        }

        /**
         * Returns the menu item identifier for navigation highlighting.
         *
         * @return menu item identifier
         */
        public String getMenuItem() {
                return menuItem;
        }

        /**
         * Sets the menu item identifier for active menu highlighting in navigation.
         *
         * @param menuItem menu item identifier matching navigation structure
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setMenuItem(String menuItem) {
                this.menuItem = menuItem;
                return this;
        }

        /**
         * Returns the unique identifier for this configuration.
         *
         * @return configuration key (typically entity name)
         */
        public String getKey() {
                return key;
        }

        /**
         * Returns the form field definitions used for rendering and validation.
         *
         * @return frontend mapping definition containing field specifications
         */
        public FrontendMappingDefinition getFrontendMappingDefinition() {
                return frontendMappingDefinition;
        }

        /**
         * Returns the form class used for request handling.
         *
         * @return form class implementing AbstractOrganizationRelatedEntityForm
         */
        public Class<F> getFormClass() {
                return formClass;
        }

        /**
         * Returns the DTO class used for form data binding.
         *
         * @return DTO class (defaults to OrganizationRelatedMap)
         */
        public Class<D> getDtoClass() {
                return dtoClass;
        }

        /**
         * Returns the entity class used for persistence.
         *
         * @return entity class implementing SearchableOrganizationRelatedEntity
         */
        public Class<E> getEntityClass() {
                return entityClass;
        }

        /**
         * Returns the default privilege used as fallback for all CRUD operations.
         *
         * @return default controller privilege
         */
        public PrivilegeBase getDefaultControllerPrivilege() {
                return defaultControllerPrivilege;
        }

        /**
         * Returns the privilege required for GET /all operations.
         * Falls back to defaultControllerPrivilege if not explicitly set.
         *
         * @return privilege for list operations
         */
        public PrivilegeBase getGetAllPrivilege() {
                return getAllPrivilege != null ? getAllPrivilege : defaultControllerPrivilege;
        }

        /**
         * Returns the privilege required for GET /new operations.
         * Falls back to defaultControllerPrivilege if not explicitly set.
         *
         * @return privilege for displaying create form
         */
        public PrivilegeBase getGetNewPrivilege() {
                return getNewPrivilege != null ? getNewPrivilege : defaultControllerPrivilege;
        }

        /**
         * Returns the privilege required for GET /settings operations.
         * Falls back to defaultControllerPrivilege if not explicitly set.
         *
         * @return privilege for viewing entity settings
         */
        public PrivilegeBase getGetSettingsPrivilege() {
                return getSettingsPrivilege != null ? getSettingsPrivilege : defaultControllerPrivilege;
        }

        /**
         * Returns the privilege required for POST /new operations.
         * Falls back to defaultControllerPrivilege if not explicitly set.
         *
         * @return privilege for creating new entities
         */
        public PrivilegeBase getPostNewPrivilege() {
                return postNewPrivilege != null ? postNewPrivilege : defaultControllerPrivilege;
        }

        /**
         * Returns the privilege required for POST /save operations.
         * Falls back to defaultControllerPrivilege if not explicitly set.
         *
         * @return privilege for saving/updating entities
         */
        public PrivilegeBase getPostSavePrivilege() {
                return postSavePrivilege != null ? postSavePrivilege : defaultControllerPrivilege;
        }

        /**
         * Returns the privilege required for POST /remove operations.
         * Falls back to defaultControllerPrivilege if not explicitly set.
         *
         * @return privilege for removing/deleting entities
         */
        public PrivilegeBase getPostRemovePrivilege() {
                return postRemovePrivilege != null ? postRemovePrivilege : defaultControllerPrivilege;
        }

        /**
         * Returns the read privilege for a specific form field.
         * Delegates to the field definition in frontendMappingDefinition.
         *
         * @param fieldName name of the field to check
         * @return read privilege for the specified field
         */
        public PrivilegeBase getFieldReadPrivilege(String fieldName) {
                return frontendMappingDefinition.findField(fieldName).readPrivilege;
        }

        /**
         * Returns the write privilege for a specific form field.
         * Delegates to the field definition in frontendMappingDefinition.
         *
         * @param fieldName name of the field to check
         * @return write privilege for the specified field
         */
        public PrivilegeBase getFieldWritePrivilege(String fieldName) {
                return frontendMappingDefinition.findField(fieldName).writePrivilege;
        }

        /**
         * Returns the Thymeleaf template name for table/list views.
         *
         * @return table view template name
         */
        public String getTableView() {
                return tableView;
        }

        /**
         * Returns the web endpoint path for dynamically rendered table views.
         *
         * @return table view endpoint URL
         */
        public String getTableViewWebEndpoint() {
                return tableViewWebEndpoint;
        }

        /**
         * Returns the Thymeleaf template name for settings/edit views.
         *
         * @return settings view template name
         */
        public String getSettingsView() {
                return settingsView;
        }

        /**
         * Returns the Thymeleaf template name for read-only detail views.
         *
         * @return read view template name
         */
        public String getReadView() {
                return readView;
        }

        /**
         * Returns the privilege-enforcing repository for entity data access.
         *
         * @return scoped secure repository
         */
        public ScopedSecureRepository<E> getSecureRepository() {
                return secureRepository;
        }

        /**
         * Creates a new entity instance using the cached reflective constructor.
         * <p>
         * Instantiates the entity via the constructor accepting (Long organizationId).
         * For MapEntity instances, additionally sets the entity key to match this configuration's key.
         * </p>
         *
         * @param organizationId organization ID for tenant-scoped entity creation
         * @return new entity instance initialized with organization ID
         * @throws RuntimeException wrapping InstantiationException, IllegalAccessException, or InvocationTargetException
         */
        public E createNewEntity(Long organizationId) {
                try {
                        E entity = entityConstructor.newInstance(organizationId);
                        if (isMapEntity) {
                                ((MapEntity) entity).setKey(key);
                        }
                        return entity;
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                }
        }

        /**
         * Creates a new empty form instance using the cached reflective constructor.
         * <p>
         * Uses the appropriate constructor based on whether the form class accepts
         * FrontendMappingDefinition as a parameter (detected in {@link #detectFormConstructor()}).
         * For forms with FrontendMappingDefinition constructor, passes the mapping definition.
         * For forms with no-arg constructor, invokes the default constructor.
         * </p>
         *
         * @return new form instance ready for request binding
         * @throws RuntimeException wrapping InstantiationException, IllegalAccessException, or InvocationTargetException
         */
        public F createNewForm() {
                try {
                        if (frontendMappingDefinitionInConstructor) {
                                return formConstructor.newInstance(frontendMappingDefinition);
                        } else {
                                return formConstructor.newInstance();
                        }

                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                }
        }

        /**
         * Creates a new form instance pre-populated with an existing entity.
         * <p>
         * Uses the appropriate constructor based on whether the form class accepts
         * FrontendMappingDefinition as a parameter. The form is initialized with the
         * provided organization ID and entity, enabling edit/update scenarios where
         * the form should be populated from existing entity data.
         * </p>
         *
         * @param organizationId organization ID for tenant context
         * @param entity existing entity to populate the form from
         * @return new form instance populated with entity data
         * @throws RuntimeException wrapping InstantiationException, IllegalAccessException, or InvocationTargetException
         */
        public F createNewForm(Long organizationId, E entity) {
                try {
                        if (frontendMappingDefinitionInConstructor) {
                                return formEntityConstructor.newInstance(frontendMappingDefinition, organizationId, entity);
                        } else {
                                return formEntityConstructor.newInstance(organizationId, entity);
                        }
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                }
        }

        /**
         * Detects and caches form constructors via reflection.
         * <p>
         * Attempts to find a constructor accepting FrontendMappingDefinition as the first parameter.
         * For ReflectionBasedEntityForm, uses SearchableOrganizationRelatedEntity as the entity type
         * parameter. For other form classes, uses the specific entity class.
         * </p>
         * <p>
         * If no FrontendMappingDefinition constructor is found, falls back to no-arg constructor
         * for empty forms and (Long, entityClass) constructor for entity-populated forms.
         * Sets frontendMappingDefinitionInConstructor flag to guide form instantiation.
         * </p>
         *
         * @throws NoSuchMethodException if no suitable constructor is found
         */
        private void detectFormConstructor() throws NoSuchMethodException {
                try {
                        this.formConstructor = this.formClass.getDeclaredConstructor(FrontendMappingDefinition.class);
                        if (ReflectionBasedEntityForm.class.equals(formClass)) {
                                this.formEntityConstructor = this.formClass.getDeclaredConstructor(FrontendMappingDefinition.class, Long.class, SearchableOrganizationRelatedEntity.class);
                        } else {
                                this.formEntityConstructor = this.formClass.getDeclaredConstructor(FrontendMappingDefinition.class, Long.class, entityClass);
                        }
                        frontendMappingDefinitionInConstructor = true;
                } catch (NoSuchMethodException e) {
                        this.formConstructor = this.formClass.getDeclaredConstructor();
                        this.formEntityConstructor = this.formClass.getDeclaredConstructor(Long.class, entityClass);
                        frontendMappingDefinitionInConstructor = false;
                }
        }

        /**
         * Returns a JPA Specification for filtering entities in repository queries.
         * <p>
         * For MapEntity configurations, adds a predicate filtering by the entity key
         * (cb.equal(root.get("key"), key)). If additionalPredicate is set, combines
         * both predicates using cb.and().
         * </p>
         * <p>
         * For regular entities, returns the additionalPredicate if set, otherwise returns
         * cb.conjunction() (always-true predicate).
         * </p>
         *
         * @return JPA Specification for use in repository findAll operations
         */
        public Specification<E> getAdditionalSpecification() {
                return isMapEntity ? (additionalPredicate != null ? (root, query, cb) -> cb.and(cb.equal(root.get("key"), key), additionalPredicate.toPredicate(root, query, cb))
                        : (root, query, cb) -> cb.equal(root.get("key"), key)) : (additionalPredicate != null ? ((root, query, cb) -> additionalPredicate.toPredicate(root, query, cb)) : (root, query, cb) -> cb.conjunction());
        }

        /**
         * Returns whether the configured entity is a MapEntity (dynamic entity).
         *
         * @return true if entity class equals MapEntity.class, false otherwise
         */
        public boolean isMapEntity() {
                return isMapEntity;
        }

        /**
         * Returns the page attribute for storing entity pages in Flow PageModelMap.
         *
         * @return page attribute for entity page results
         */
        public PageAttr<Page<E>> getEntityPageAttribute() {
                return (PageAttr<Page<E>>) entityPageAttribute;
        }

        /**
         * Sets the page attribute for storing entity pages in Flow PageModelMap.
         *
         * @param entityPageAttribute page attribute for paginated entity results
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setEntityPageAttribute(PageAttr<Page<E>> entityPageAttribute) {
                this.entityPageAttribute = entityPageAttribute;
                return this;
        }

        /**
         * Returns the page attribute for storing single entities in Flow PageModelMap.
         *
         * @return page attribute for single entity instances
         */
        public PageAttr<E> getEntityAttribute() {
                return (PageAttr<E>) entityAttribute;
        }

        /**
         * Sets the page attribute for storing single entities in Flow PageModelMap.
         *
         * @param entityAttribute page attribute for entity instances
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setEntityAttribute(PageAttr<E> entityAttribute) {
                this.entityAttribute = entityAttribute;
                return this;
        }

        /**
         * Returns the page attribute for storing forms in Flow PageModelMap.
         *
         * @return page attribute for form instances
         */
        public PageAttr<F> getFormAttribute() {
                return (PageAttr<F>) formAttribute;
        }

        /**
         * Sets the page attribute for storing forms in Flow PageModelMap.
         *
         * @param formAttribute page attribute for form instances
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setFormAttribute(PageAttr<F> formAttribute) {
                this.formAttribute = formAttribute;
                return this;
        }

        /**
         * Sets a custom JPA Specification predicate for filtering repository queries.
         * <p>
         * The predicate is combined with MapEntity key filtering (if applicable) in
         * {@link #getAdditionalSpecification()} to produce the final query specification.
         * Use this to add custom filtering logic beyond the standard MapEntity key filtering.
         * </p>
         *
         * @param additionalPredicate custom JPA Specification for additional query conditions
         * @return this configuration for fluent method chaining
         */
        public CRUDControllerConfiguration<D, E, F> setAdditionalPredicate(Specification<E> additionalPredicate) {
                this.additionalPredicate = additionalPredicate;
                return this;
        }

        /**
         * Returns the organization ID for tenant-scoped operations.
         *
         * @return organization ID or null for global operations
         */
        public Long getOrganizationId() {
                return organizationId;
        }

        /**
         * Sets the organization ID for tenant-scoped operations.
         *
         * @param organizationId organization ID for tenant context
         */
        public void setOrganizationId(Long organizationId) {
                this.organizationId = organizationId;
        }
}


