/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 and associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.core.service.form;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.customisation.ServerJSRunner;
import com.openkoda.core.form.AbstractForm;
import com.openkoda.core.form.CRUDControllerConfiguration;
import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.core.helper.ClusterHelper;
import com.openkoda.core.multitenancy.MultitenancyService;
import com.openkoda.core.repository.common.ScopedSecureRepository;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.core.service.event.ClusterEventSenderService;
import com.openkoda.model.common.SearchableRepositoryMetadata;
import com.openkoda.model.component.Form;
import com.openkoda.model.component.FrontendResource;
import com.openkoda.model.component.ServerJs;
import com.openkoda.repository.SecureRepositoryWrapper;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.openkoda.core.helper.NameHelper.toEntityKey;
import static com.openkoda.core.service.FrontendResourceService.frontendResourceTemplateNamePrefix;

/**
 * Central form lifecycle management service that handles runtime registration, unregistration, 
 * and reload of dynamically persisted {@link Form} entities and their UI/API exposure.
 * <p>
 * FormService is responsible for materializing persisted Form records into runtime artifacts 
 * including frontend mappings, HTML CRUD controllers, and API CRUD controllers. The service 
 * integrates with auditing and multitenancy registries and provides cluster-aware orchestration 
 * for distributed deployments.
 * </p>
 * 
 * <h2>Architecture Flow</h2>
 * <p>
 * The form binding architecture follows this flow:
 * <ol>
 *   <li>Persisted Form entity retrieved from database</li>
 *   <li>JavaScript code evaluation via {@link ServerJSRunner} generates {@link FrontendMappingDefinition}</li>
 *   <li>Controller registration makes form available at runtime</li>
 *   <li>Runtime availability for HTML and API endpoints</li>
 * </ol>
 * </p>
 * 
 * <h2>Form Registration Sequence</h2>
 * <p>
 * When a form is registered via {@link #registerForm(Form)}, the following steps occur:
 * <ul>
 *   <li>Repository resolution via services.data.getRepository with entity key</li>
 *   <li>Repository availability verification via {@link SecureRepositoryWrapper#isSet()}</li>
 *   <li>ServerJs JavaScript snippet construction calling FrontendMappingDefinitionService</li>
 *   <li>JavaScript evaluation to obtain {@link FrontendMappingDefinition}</li>
 *   <li>Frontend mapping registration via services.customisation.registerFrontendMapping</li>
 *   <li>Metadata dirty marking with {@link AbstractForm#markDirty(String)}</li>
 *   <li>Auditable class registration based on form.isRegisterAsAuditable flag</li>
 *   <li>HTML CRUD controller registration with {@link CRUDControllerConfiguration}</li>
 *   <li>API CRUD controller registration</li>
 * </ul>
 * </p>
 * 
 * <h2>Usage Examples</h2>
 * <pre>
 * // Form registration
 * formService.addForm(formId);
 * 
 * // Form reload after update
 * formService.reloadForm(formId);
 * 
 * // Cluster-aware operations
 * formService.loadClusterAware(formId);
 * 
 * // Bulk initialization at startup
 * formService.loadAllFormsFromDb(true);
 * </pre>
 * 
 * <h2>Integration Notes</h2>
 * <p>
 * This service collaborates with:
 * <ul>
 *   <li>{@link com.openkoda.core.service.FrontendMappingDefinitionService} for form definition creation</li>
 *   <li>{@link ComponentProvider} for accessing repositories, services, and debug helpers</li>
 *   <li>{@link com.openkoda.core.customisation.CustomisationService} for registering/unregistering frontend mappings and controllers</li>
 *   <li>{@link ServerJSRunner} for persisted JavaScript code evaluation (note: arbitrary runtime effects possible)</li>
 *   <li>{@link MultitenancyService} for tenant-scoped table tracking</li>
 *   <li>{@link ClusterHelper} and {@link ClusterEventSenderService} for cluster-aware broadcast</li>
 *   <li>{@link SearchableRepositoryMetadata} for repository field/configuration metadata</li>
 *   <li>{@link CRUDControllerConfiguration} for generic table fields and filter fields setup</li>
 * </ul>
 * </p>
 * 
 * <h2>Security and Privilege Handling</h2>
 * <p>
 * Implements {@link HasSecurityRules} and uses Form privilege fields (readPrivilege, writePrivilege) 
 * to register controller access rules. Generated controllers enforce privilege-based access control 
 * for all CRUD operations.
 * </p>
 * 
 * <h2>Error Handling</h2>
 * <p>
 * Methods return {@code false} when:
 * <ul>
 *   <li>Form entity not found by ID</li>
 *   <li>Repository not available ({@link SecureRepositoryWrapper#isSet()} returns false)</li>
 * </ul>
 * Note: Persisted JavaScript evaluation has arbitrary runtime effects. All dynamic code 
 * execution is centralized in this service.
 * </p>
 * 
 * <h2>Thread-Safety</h2>
 * <p>
 * Form registration and unregistration operations should be carefully synchronized in 
 * multi-threaded environments. {@link AbstractForm#markDirty(String)} marks metadata 
 * as dirty for cache invalidation across the application.
 * </p>
 * 
 * <h2>Performance Considerations</h2>
 * <p>
 * The {@link #loadAllFormsFromDb(boolean)} bulk operation may have performance impact 
 * at startup when loading many forms. Cluster-aware operations incur broadcast overhead 
 * via {@link ClusterEventSenderService}.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Form
 * @see FrontendMappingDefinition
 * @see ServerJSRunner
 * @see com.openkoda.core.service.FrontendMappingDefinitionService
 * @see com.openkoda.core.customisation.CustomisationService
 * @see MultitenancyService
 * @see ClusterEventSenderService
 */
@Service
public class FormService extends ComponentProvider implements HasSecurityRules {

    /**
     * Service for broadcasting form operations across cluster nodes.
     * <p>
     * Used in cluster-aware methods to ensure form registration, unregistration, 
     * and reload operations are synchronized across all nodes in the cluster. 
     * When {@link ClusterHelper#isCluster()} returns true, this service broadcasts 
     * operations to all cluster members.
     * </p>
     *
     * @see ClusterEventSenderService#loadForm(long)
     * @see ClusterEventSenderService#removeForm(long)
     * @see ClusterEventSenderService#reloadForm(long)
     */
    @Inject
    private ClusterEventSenderService clusterEventSenderService;

    /**
     * Service for managing tenant-scoped table tracking and management.
     * <p>
     * Handles the registration and removal of form table names in multitenancy 
     * environments. When multitenancy is enabled, form table names are added to 
     * the tenant-aware table registry for proper data isolation.
     * </p>
     *
     * @see MultitenancyService#addTenantedTables(List)
     * @see MultitenancyService#removeTenantedTables(List)
     * @see MultitenancyService#isMultitenancy()
     */
    @Inject
    private MultitenancyService multitenancyService;
    /**
     * Unregisters and re-registers an updated form to apply configuration changes.
     * <p>
     * This method performs a complete reload workflow by first unregistering the 
     * existing form (removing frontend mappings, controllers, and auditable class 
     * registrations), then registering it again with the current persisted configuration. 
     * This is typically used after a Form entity has been modified in the database.
     * </p>
     * <p>
     * The Form entity is fetched from the unsecure repository to ensure access 
     * regardless of security context. If the form is not found or unregistration 
     * fails, the method returns {@code false}.
     * </p>
     * 
     * <h3>Workflow</h3>
     * <ol>
     *   <li>Fetch Form entity by ID from unsecure repository</li>
     *   <li>Call {@link #unregisterForm(Form)} to clean up existing registrations</li>
     *   <li>Call {@link #registerForm(Form)} to create new registrations</li>
     * </ol>
     *
     * @param formId the ID of the Form entity to reload, must not be null
     * @return {@code true} if the form was successfully unregistered and re-registered, 
     *         {@code false} if the form was not found or unregistration failed
     * @see #addForm(Long)
     * @see #removeForm(Long)
     * @see #reloadClusterAware(long)
     */
    public boolean reloadForm(Long formId) {
        debug("[reloadForm]");
        Form form = repositories.unsecure.form.findOne(formId);
        if(form != null) {
            if(unregisterForm(form)) {
                return registerForm(form);
            }
        }
        return false;
    }

    /**
     * Registers a form by loading it from the database and creating runtime artifacts.
     * <p>
     * This method fetches the Form entity by ID from the unsecure repository and 
     * delegates to {@link #registerForm(Form)} to perform the complete registration 
     * sequence. Registration includes creating frontend mappings, generating CRUD 
     * controllers, and integrating with auditing and multitenancy systems.
     * </p>
     * <p>
     * The registration process evaluates the persisted JavaScript code in the Form's 
     * {@code code} field via {@link ServerJSRunner} to generate a 
     * {@link FrontendMappingDefinition}, which is then used to create HTML and API 
     * endpoints for the form.
     * </p>
     *
     * @param formId the ID of the Form entity to register, must not be null
     * @return {@code true} if the form was successfully registered, {@code false} if 
     *         the form was not found or the associated repository is not available
     * @see #registerForm(Form)
     * @see #loadClusterAware(long)
     * @see #loadAllFormsFromDb(boolean)
     */
    public boolean addForm(Long formId) {
        debug("[addForm]");
        Form form = repositories.unsecure.form.findOne(formId);
        if(form != null) {
            return registerForm(form);
        }
        return false;
    }

    /**
     * Unregisters a form by removing all associated runtime artifacts.
     * <p>
     * This method fetches the Form entity by ID from the unsecure repository and 
     * delegates to {@link #unregisterForm(Form)} to perform the complete cleanup 
     * sequence. Unregistration includes removing frontend mappings, unregistering 
     * CRUD controllers, removing multitenancy table entries, and cleaning up 
     * auditable class registrations.
     * </p>
     * <p>
     * After unregistration, the form's endpoints are no longer available and 
     * cached metadata is invalidated.
     * </p>
     *
     * @param formId the ID of the Form entity to unregister, must not be null
     * @return {@code true} if the form was successfully unregistered, {@code false} 
     *         if the form was not found
     * @see #unregisterForm(Form)
     * @see #removeClusterAware(long)
     */
    public boolean removeForm(Long formId) {
        debug("[removeForm]");
        Form form = repositories.unsecure.form.findOne(formId);
        if(form != null) {
            return unregisterForm(form);
        }
        return false;
    }

    /**
     * Loads and registers all Form entities from the database in a bulk operation.
     * <p>
     * This method is typically called during application startup or admin-initiated 
     * bulk operations to initialize all persisted forms. It fetches all Form entities 
     * from the database and registers each one via {@link #registerForm(Form)}.
     * </p>
     * <p>
     * When multitenancy is enabled ({@link MultitenancyService#isMultitenancy()} 
     * returns true), all form table names are added to the multitenancy service's 
     * tenant-aware table registry in a single batch operation for efficiency.
     * </p>
     * 
     * <h3>Usage Context</h3>
     * <ul>
     *   <li>Application startup initialization</li>
     *   <li>Admin-triggered bulk form reload operations</li>
     *   <li>System configuration changes requiring full form refresh</li>
     * </ul>
     * 
     * <h3>Performance Impact</h3>
     * <p>
     * This operation may have significant performance impact when loading many forms, 
     * as each form triggers JavaScript evaluation and controller registration. Consider 
     * using this method during maintenance windows for production systems with large 
     * numbers of forms.
     * </p>
     *
     * @param proceed if {@code true}, performs the bulk load operation; if {@code false}, 
     *                returns immediately without any action. This flag allows conditional 
     *                execution based on application configuration or environment settings.
     * @see #addForm(Long)
     * @see #registerForm(Form)
     * @see MultitenancyService#addTenantedTables(List)
     */
    public void loadAllFormsFromDb(boolean proceed) {
        debug("[loadFormsFromDb]");
        if (not(proceed)) {
            return;
        }
        List<Form> all = repositories.unsecure.form.findAll();
        all.forEach(this::registerForm);
        if(MultitenancyService.isMultitenancy()) {
            multitenancyService.addTenantedTables(all.stream().map(Form::getTableName).collect(Collectors.toList()));
        }
    }

    /**
     * Registers a form with cluster-aware synchronization across all nodes.
     * <p>
     * This method ensures that form registration is synchronized across all nodes 
     * in a clustered deployment. When {@link ClusterHelper#isCluster()} returns true, 
     * the operation is broadcast to all cluster members via 
     * {@link ClusterEventSenderService#loadForm(long)}. In single-node mode, it 
     * delegates to the local {@link #addForm(Long)} method.
     * </p>
     * <p>
     * Use this method instead of {@link #addForm(Long)} in clustered environments 
     * to ensure consistent form availability across all application instances.
     * </p>
     * 
     * <h3>Cluster Behavior</h3>
     * <ul>
     *   <li><b>Cluster mode:</b> Broadcasts operation via ClusterEventSenderService</li>
     *   <li><b>Single-node mode:</b> Calls local addForm method</li>
     * </ul>
     *
     * @param formId the ID of the Form entity to register across the cluster
     * @return {@code true} if the form was successfully registered (locally or cluster-wide), 
     *         {@code false} if registration failed
     * @see #addForm(Long)
     * @see ClusterEventSenderService#loadForm(long)
     * @see ClusterHelper#isCluster()
     */
    public boolean loadClusterAware(long formId) {
        debug("[loadClusterAware] {}", formId);
        if (ClusterHelper.isCluster()) {
            return clusterEventSenderService.loadForm(formId);
        }
        return addForm(formId);
    }

    /**
     * Unregisters a form with cluster-aware synchronization across all nodes.
     * <p>
     * This method ensures that form unregistration is synchronized across all nodes 
     * in a clustered deployment. When {@link ClusterHelper#isCluster()} returns true, 
     * the operation is broadcast to all cluster members via 
     * {@link ClusterEventSenderService#removeForm(long)}. In single-node mode, it 
     * delegates to the local {@link #removeForm(Long)} method.
     * </p>
     * <p>
     * Use this method instead of {@link #removeForm(Long)} in clustered environments 
     * to ensure forms are removed consistently across all application instances.
     * </p>
     * 
     * <h3>Cluster Behavior</h3>
     * <ul>
     *   <li><b>Cluster mode:</b> Broadcasts operation via ClusterEventSenderService</li>
     *   <li><b>Single-node mode:</b> Calls local removeForm method</li>
     * </ul>
     *
     * @param formId the ID of the Form entity to unregister across the cluster
     * @return {@code true} if the form was successfully unregistered (locally or cluster-wide), 
     *         {@code false} if unregistration failed
     * @see #removeForm(Long)
     * @see ClusterEventSenderService#removeForm(long)
     * @see ClusterHelper#isCluster()
     */
    public boolean removeClusterAware(long formId) {
        debug("[removeClusterAware] {}", formId);
        if (ClusterHelper.isCluster()) {
            return clusterEventSenderService.removeForm(formId);
        }
        return removeForm(formId);
    }

    /**
     * Reloads a form with cluster-aware synchronization across all nodes.
     * <p>
     * This method ensures that form reload (unregister + re-register) is synchronized 
     * across all nodes in a clustered deployment. When {@link ClusterHelper#isCluster()} 
     * returns true, the operation is broadcast to all cluster members via 
     * {@link ClusterEventSenderService#reloadForm(long)}. In single-node mode, it 
     * delegates to the local {@link #reloadForm(Long)} method.
     * </p>
     * <p>
     * Use this method instead of {@link #reloadForm(Long)} in clustered environments 
     * to ensure form configuration changes are applied consistently across all 
     * application instances.
     * </p>
     * 
     * <h3>Cluster Behavior</h3>
     * <ul>
     *   <li><b>Cluster mode:</b> Broadcasts operation via ClusterEventSenderService</li>
     *   <li><b>Single-node mode:</b> Calls local reloadForm method</li>
     * </ul>
     *
     * @param formId the ID of the Form entity to reload across the cluster
     * @return {@code true} if the form was successfully reloaded (locally or cluster-wide), 
     *         {@code false} if reload failed
     * @see #reloadForm(Long)
     * @see ClusterEventSenderService#reloadForm(long)
     * @see ClusterHelper#isCluster()
     */
    public boolean reloadClusterAware(long formId) {
        debug("[reloadClusterAware] {}", formId);
        if (ClusterHelper.isCluster()) {
            return clusterEventSenderService.reloadForm(formId);
        }
        return reloadForm(formId);
    }

    /**
     * Creates a FrontendMappingDefinition from a Form entity using static factory method.
     * <p>
     * This convenience method extracts the form's name, privilege strings, and JavaScript 
     * code, then delegates to the overloaded method for evaluation.
     * </p>
     *
     * @param form the Form entity containing name, privileges, and code, must not be null
     * @return a FrontendMappingDefinition instance created by evaluating the form's JavaScript code
     * @see #getFrontendMappingDefinition(String, String, String, String)
     * @see Form#getName()
     * @see Form#getReadPrivilegeAsString()
     * @see Form#getWritePrivilegeAsString()
     * @see Form#getCode()
     */
    public static FrontendMappingDefinition getFrontendMappingDefinition(Form form) {
        return getFrontendMappingDefinition(form.getName(), form.getReadPrivilegeAsString(), form.getWritePrivilegeAsString(), form.getCode());
    }

    /**
     * Creates a FrontendMappingDefinition by evaluating persisted JavaScript code.
     * <p>
     * This method constructs a ServerJs snippet that calls 
     * {@link com.openkoda.core.service.FrontendMappingDefinitionService#createFrontendMappingDefinition(String, String, String, Object)}
     * with the provided parameters and the form's JavaScript code. The ServerJs is then 
     * evaluated via {@link ServerJSRunner#evaluateServerJs(ServerJs, Map, Object, Class)} 
     * to produce a FrontendMappingDefinition instance.
     * </p>
     * <p>
     * <b>Warning:</b> The JavaScript code evaluation has arbitrary runtime effects. 
     * Ensure that persisted code is trusted and validated before registration.
     * </p>
     * 
     * <h3>JavaScript Evaluation</h3>
     * <p>
     * The constructed JavaScript snippet follows this pattern:
     * <pre>
     * let form = new com.openkoda.core.service.FrontendMappingDefinitionService()
     *     .createFrontendMappingDefinition("formName", "readPriv", "writePriv", codeObject);
     * form
     * </pre>
     * </p>
     *
     * @param name the form name identifier, must not be null
     * @param readPrivilege the privilege required for read operations, may be null or empty
     * @param writePrivilege the privilege required for write operations, may be null or empty
     * @param code the JavaScript code defining the form structure and behavior, must not be null
     * @return a FrontendMappingDefinition instance created by evaluating the JavaScript code
     * @see ServerJs
     * @see ServerJSRunner#evaluateServerJs(ServerJs, Map, Object, Class)
     * @see com.openkoda.core.service.FrontendMappingDefinitionService
     */
    public static FrontendMappingDefinition getFrontendMappingDefinition(String name, String readPrivilege, String writePrivilege, String code) {
        String finalScript = "let form = new com.openkoda.core.service.FrontendMappingDefinitionService().createFrontendMappingDefinition(\""
                + name + "\",\"" + readPrivilege + "\",\"" + writePrivilege + "\","
                + code + ");\nform";
        ServerJs serverJs = new ServerJs(finalScript, StringUtils.EMPTY, null);
        Map<String, Object> model = new HashMap();
        return new ServerJSRunner().evaluateServerJs(serverJs, model, null, FrontendMappingDefinition.class);
    }

    /**
     * Performs the complete form registration sequence to materialize runtime artifacts.
     * <p>
     * This private method orchestrates the complex registration workflow that transforms 
     * a persisted Form entity into runtime-available UI and API endpoints. The registration 
     * process involves multiple integration points and side effects across the application.
     * </p>
     * 
     * <h3>Registration Sequence</h3>
     * <ol>
     *   <li><b>Repository Resolution:</b> Calls services.data.getRepository with entity key 
     *       derived from form name to obtain the {@link ScopedSecureRepository}</li>
     *   <li><b>Repository Availability Check:</b> Casts repository to 
     *       {@link SecureRepositoryWrapper} and calls isSet() to verify availability. 
     *       Returns false if repository is not available.</li>
     *   <li><b>ServerJs Construction:</b> Builds JavaScript snippet calling 
     *       FrontendMappingDefinitionService.createFrontendMappingDefinition with form 
     *       name, privilege strings, and persisted code</li>
     *   <li><b>JavaScript Evaluation:</b> Evaluates ServerJs via 
     *       {@link ServerJSRunner#evaluateServerJs(ServerJs, Map, Object, Class)} to 
     *       obtain {@link FrontendMappingDefinition}. <b>Note:</b> This step has arbitrary 
     *       runtime effects based on persisted code.</li>
     *   <li><b>Frontend Mapping Registration:</b> Passes FrontendMappingDefinition to 
     *       services.customisation.registerFrontendMapping</li>
     *   <li><b>Metadata Dirty Marking:</b> Calls {@link AbstractForm#markDirty(String)} 
     *       to invalidate cached metadata</li>
     *   <li><b>Auditable Class Registration:</b> Based on form.isRegisterAsAuditable() flag, 
     *       registers or unregisters the entity class with the auditable registry using 
     *       {@link SearchableRepositoryMetadata}</li>
     *   <li><b>HTML CRUD Controller:</b> If form.isRegisterHtmlCrudController() is true, 
     *       registers HTML CRUD controller with {@link CRUDControllerConfiguration}. 
     *       Configures generic table fields via setGenericTableFields, filter fields via 
     *       setFilterFields, and optionally creates table view endpoint if form.getTableView() 
     *       is not empty.</li>
     *   <li><b>API CRUD Controller:</b> If form.isRegisterApiCrudController() is true, 
     *       registers API CRUD controller for programmatic access</li>
     * </ol>
     * 
     * <h3>Integration Points</h3>
     * <ul>
     *   <li>services.data.getRepository: Dynamic repository lookup</li>
     *   <li>services.customisation.registerFrontendMapping: Frontend mapping registry</li>
     *   <li>services.customisation.registerAuditableClass/unregisterAuditableClass: Auditing integration</li>
     *   <li>services.customisation.registerHtmlCrudController: HTML controller registry</li>
     *   <li>services.customisation.registerApiCrudController: API controller registry</li>
     * </ul>
     * 
     * <h3>Side Effects</h3>
     * <ul>
     *   <li>Creates runtime HTTP endpoints for form access</li>
     *   <li>Modifies global frontend mapping registry</li>
     *   <li>Alters auditing configuration</li>
     *   <li>Invalidates cached form metadata</li>
     *   <li>Executes arbitrary JavaScript code with full system access</li>
     * </ul>
     *
     * @param form the Form entity to register, must not be null
     * @return {@code true} if registration completed successfully, {@code false} if 
     *         the associated repository is not available (SecureRepositoryWrapper.isSet() returns false)
     * @see #getFrontendMappingDefinition(Form)
     * @see SecureRepositoryWrapper#isSet()
     * @see SearchableRepositoryMetadata
     * @see CRUDControllerConfiguration
     * @see AbstractForm#markDirty(String)
     */
    private boolean registerForm(Form form) {
        debug("[registerForm]");

        ScopedSecureRepository<?> repository = services.data.getRepository(toEntityKey(form.getName()), SecurityScope.USER);

        if(((SecureRepositoryWrapper) repository).isSet()) {
            FrontendMappingDefinition formFieldDefinitionBuilder = getFrontendMappingDefinition(
                    form.getName(),
                    form.getReadPrivilegeAsString(),
                    form.getWritePrivilegeAsString(),
                    form.getCode());

            services.customisation.registerFrontendMapping(formFieldDefinitionBuilder, repository);
            AbstractForm.markDirty(form.getName());
            SearchableRepositoryMetadata repositoryMetadata = repository.getSearchableRepositoryMetadata();
            if(form.isRegisterAsAuditable()) {
                services.customisation.registerAuditableClass((Class) repositoryMetadata.entityClass(), repositoryMetadata.entityKey());
            } else {
                services.customisation.unregisterAuditableClass(repositoryMetadata.entityClass());
            }

            if (form.isRegisterHtmlCrudController()) {
                CRUDControllerConfiguration crudControllerConfiguration = services.customisation.registerHtmlCrudController(formFieldDefinitionBuilder, repository, form.getReadPrivilege(), form.getWritePrivilege())
                        .setGenericTableFields(form.getTableColumnsList())
                        .setFilterFields(form.getFilterColumnsList());
                if(StringUtils.isNotEmpty(form.getTableView())) {
                    crudControllerConfiguration.setTableViewWebEndpoint(frontendResourceTemplateNamePrefix + FrontendResource.AccessLevel.GLOBAL.getPath() + form.getTableView());
                }
            }
            if (form.isRegisterApiCrudController()) {
                services.customisation.registerApiCrudController(formFieldDefinitionBuilder, repository, form.getReadPrivilege(), form.getWritePrivilege());
            }
            return true;
        }
        return false;
    }

    /**
     * Performs the complete form unregistration sequence to remove runtime artifacts.
     * <p>
     * This private method orchestrates the cleanup workflow that removes all runtime 
     * artifacts created during form registration. The unregistration process ensures 
     * that the form's endpoints are no longer accessible and all related integrations 
     * are cleaned up.
     * </p>
     * 
     * <h3>Unregistration Sequence</h3>
     * <ol>
     *   <li><b>Multitenancy Table Removal:</b> Calls 
     *       {@link MultitenancyService#removeTenantedTables(List)} with the form's table 
     *       name to remove it from the tenant-aware table registry</li>
     *   <li><b>Frontend Mapping Unregistration:</b> Calls 
     *       services.customisation.unregisterFrontendMapping with form name to remove 
     *       frontend mapping from the registry</li>
     *   <li><b>HTML CRUD Controller Unregistration:</b> Calls 
     *       services.customisation.unregisterHtmlCrudController with lowercase form name 
     *       to remove HTML endpoints</li>
     *   <li><b>API CRUD Controller Unregistration:</b> Calls 
     *       services.customisation.unregisterApiCrudController with lowercase form name 
     *       to remove API endpoints</li>
     * </ol>
     * 
     * <h3>Cleanup Integration Points</h3>
     * <ul>
     *   <li>MultitenancyService.removeTenantedTables: Tenant table registry cleanup</li>
     *   <li>services.customisation.unregisterFrontendMapping: Frontend mapping cleanup</li>
     *   <li>services.customisation.unregisterHtmlCrudController: HTML controller cleanup</li>
     *   <li>services.customisation.unregisterApiCrudController: API controller cleanup</li>
     * </ul>
     * 
     * <h3>Side Effects</h3>
     * <ul>
     *   <li>Removes runtime HTTP endpoints for form access</li>
     *   <li>Modifies global frontend mapping registry</li>
     *   <li>Cleans up multitenancy table tracking</li>
     *   <li>Removes both HTML and API controllers regardless of registration flags</li>
     * </ul>
     * 
     * <h3>Post-Unregistration State</h3>
     * <p>
     * After unregistration, attempts to access the form's endpoints will result in 
     * 404 errors. Cached metadata may still reference the form until next cache 
     * invalidation cycle.
     * </p>
     *
     * @param form the Form entity to unregister, must not be null
     * @return {@code true} always (unregistration sequence completes successfully 
     *         even if some components were not previously registered)
     * @see MultitenancyService#removeTenantedTables(List)
     * @see #registerForm(Form)
     */
    private boolean unregisterForm(Form form) {
        debug("[unregisterForm]");
        multitenancyService.removeTenantedTables(Collections.singletonList(form.getTableName()));
        services.customisation.unregisterFrontendMapping(form.getName());
        services.customisation.unregisterHtmlCrudController(form.getName().toLowerCase());
        services.customisation.unregisterApiCrudController(form.getName().toLowerCase());
        return true;
    }
}
