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

import com.openkoda.core.form.CRUDControllerConfiguration;
import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.core.repository.common.ScopedSecureRepository;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.component.Form;
import com.openkoda.repository.FormRepository;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toCollection;

/**
 * Concrete implementation of CRUD controller configuration registry for HTML endpoints with organization-scoped visibility filtering.
 * <p>
 * Spring {@code @Component} extending {@link AbstractCRUDControllerConfigurationMap}. Stores configurations for HTML-based 
 * generic CRUD controllers ({@link CRUDControllerHtml}). Provides {@link #getExposed()} and {@link #getExposedSorted()} 
 * methods that filter configurations by {@link PrivilegeHelper} privilege checks and {@link Form#isShowOnOrganizationDashboard()} 
 * visibility. Injects {@link FormRepository} for organization scoping.
 * 
 * <p>
 * Singleton instance exposed in static field {@link #instance} for {@code CustomisationService} access. Uses plain 
 * {@link HashMap} with potential concurrency considerations for the exposed map.
 * 
 * <p>
 * Example usage:
 * <pre>{@code
 * HtmlCRUDControllerConfigurationMap map = HtmlCRUDControllerConfigurationMap.getControllers();
 * Set<Entry<String, CRUDControllerConfiguration>> exposed = map.getExposedSorted(organizationId);
 * }</pre>
 * <p>
 * Thread-safety note: Uses plain HashMap without synchronization for the exposed map. {@link #getExposed(Long)} 
 * computes filter on each call - thread-safe but not optimized for high concurrency.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractCRUDControllerConfigurationMap
 * @see CRUDControllerHtml
 * @see CRUDControllerConfiguration
 * @see FormRepository
 */
@Component
public class HtmlCRUDControllerConfigurationMap extends AbstractCRUDControllerConfigurationMap{

    private static final long serialVersionUID = 2132602257370923654L;
    
    /**
     * Static singleton reference to this configuration map for {@code CustomisationService} access.
     * <p>
     * Thread-safety note: Set once during {@link #init()} lifecycle callback, read-only afterward.
     * 
     *
     * @see #getControllers()
     * @see #init()
     */
    private static HtmlCRUDControllerConfigurationMap instance;

    /**
     * Spring lifecycle callback that assigns singleton reference.
     * <p>
     * Sets {@link #instance} static field to this instance during Spring bean initialization. This enables 
     * static access via {@link #getControllers()} for {@code CustomisationService} and other components 
     * requiring global access to the configuration registry.
     * 
     *
     * @see #getControllers()
     */
    @PostConstruct
    public void init() {
        instance = this;
    }

    /**
     * Autowired {@link FormRepository} for organization-scoped form configuration retrieval.
     * <p>
     * Enables form entity lookup by name to determine organization ID and visibility settings for controller exposure.
     * Used in {@link #setOrgIdAndExpose(String, CRUDControllerConfiguration)} and 
     * {@link #getExposed(Long, Set)} to filter configurations based on {@link Form#isShowOnOrganizationDashboard()}.
     * 
     *
     * @see Form
     * @see #setOrgIdAndExpose(String, CRUDControllerConfiguration)
     * @see #getExposed(Long, Set)
     */
    @Inject
    FormRepository formRepository;
    
    /**
     * Internal HashMap storing controller configurations marked for exposure in UI.
     * <p>
     * Populated by {@link #registerAndExposeCRUDController} methods and queried by {@link #getExposed()} 
     * methods with privilege and visibility filtering. Uses plain HashMap without explicit synchronization.
     * 
     *
     * @see #getExposed()
     * @see #registerAndExposeCRUDController(FrontendMappingDefinition, ScopedSecureRepository, Class)
     */
    private final HashMap<String, CRUDControllerConfiguration> exposed = new HashMap<>();

    /**
     * Registers a CRUD controller configuration with default privileges and marks it for UI exposure.
     * <p>
     * Creates a {@link CRUDControllerConfiguration} by delegating to 
     * {@link AbstractCRUDControllerConfigurationMap#registerCRUDController(FrontendMappingDefinition, ScopedSecureRepository, Class)}, 
     * then calls {@link #setOrgIdAndExpose(String, CRUDControllerConfiguration)} to associate organization ID 
     * from the corresponding {@link Form} and add to {@link #exposed} map.
     * 
     * <p>
     * Note: Marked with TODO comment indicating this approach may be refactored in future versions.
     * 
     *
     * @param frontendMappingDefinition the form definition containing name and field mappings
     * @param secureRepository the repository for data access with privilege enforcement
     * @param formClass the form class used for binding and validation
     * @return the registered and exposed {@link CRUDControllerConfiguration}
     * @see #registerAndExposeCRUDController(FrontendMappingDefinition, ScopedSecureRepository, Class, PrivilegeBase, PrivilegeBase)
     * @see #setOrgIdAndExpose(String, CRUDControllerConfiguration)
     */
    //TODO: this is dirty solution, to be trashed
    public CRUDControllerConfiguration registerAndExposeCRUDController(
            FrontendMappingDefinition frontendMappingDefinition,
            ScopedSecureRepository secureRepository,
            Class formClass
    ) {
        CRUDControllerConfiguration c = super.registerCRUDController(frontendMappingDefinition, secureRepository, formClass);
        setOrgIdAndExpose(frontendMappingDefinition.name, c);
        return c;
    }

    /**
     * Returns all exposed controller configuration entries without filtering.
     * <p>
     * Provides direct access to {@link #exposed} map entries. For privilege-filtered results, 
     * use {@link #getExposed(Long)} or {@link #getExposedSorted(Long)} instead.
     * 
     *
     * @return unfiltered set of all controller configuration map entries
     * @see #getExposed(Long)
     * @see #getExposedSorted()
     */
    public Set<Entry<String, CRUDControllerConfiguration>> getExposed() {
        return exposed.entrySet();
    }
    
    /**
     * Returns privilege-filtered map of visible controller configurations for the specified organization.
     * <p>
     * Delegates to {@link #getExposed(Long, Set)} with {@link #getExposed()} as initial set. 
     * Applies organization-scoped filtering based on configuration's organization ID, user privileges 
     * via {@link PrivilegeHelper#hasGlobalOrOrgPrivilege(PrivilegeBase, Long)}, and form visibility 
     * via {@link Form#isShowOnOrganizationDashboard()}.
     * 
     * <p>
     * Note: Computes filter on each call - consider caching for frequent access patterns.
     * 
     *
     * @param organizationId the organization ID for scoping, or null for no organization filtering
     * @return filtered set of controller configuration entries visible to the specified organization
     * @see #getExposed(Long, Set)
     * @see PrivilegeHelper#hasGlobalOrOrgPrivilege(PrivilegeBase, Long)
     * @see Form#isShowOnOrganizationDashboard()
     */
    public Set<Entry<String, CRUDControllerConfiguration>> getExposed(Long organizationId){
        return getExposed(organizationId, getExposed());
    }
    
    /**
     * Returns sorted privilege-filtered list of visible configurations without organization scoping.
     * <p>
     * Delegates to {@link #getExposedSorted(Long)} with null organization ID, returning all configurations 
     * that pass privilege checks, sorted alphabetically by configuration key.
     * 
     *
     * @return sorted {@link LinkedHashSet} of visible controller configuration entries
     * @see #getExposedSorted(Long)
     */
    public LinkedHashSet<Entry<String, CRUDControllerConfiguration>> getExposedSorted() {
        return getExposedSorted(null);
    }
    
    /**
     * Returns sorted privilege-filtered list of visible configurations for the specified organization.
     * <p>
     * Calls {@link #getExposed(Long, Set)} to apply organization-scoped filtering and privilege checks, 
     * then converts to stream, sorts entries alphabetically by key using {@link Map.Entry#comparingByKey()}, 
     * and collects to {@link LinkedHashSet} preserving sort order.
     * 
     * <p>
     * Filtering criteria:
     * <ul>
     *   <li>Configuration organization ID matches or is null</li>
     *   <li>User has required privilege via {@link PrivilegeHelper#hasGlobalOrOrgPrivilege(PrivilegeBase, Long)}</li>
     *   <li>Associated {@link Form} has {@link Form#isShowOnOrganizationDashboard()} enabled</li>
     * </ul>
     * 
     *
     * @param organizationId the organization ID for scoping, or null for no organization filtering
     * @return sorted {@link LinkedHashSet} of controller configuration entries
     * @see #getExposed(Long, Set)
     * @see Map.Entry#comparingByKey()
     */
    public LinkedHashSet<Entry<String, CRUDControllerConfiguration>> getExposedSorted(Long organizationId) {
        return getExposed(organizationId, getExposed()).stream()
                .sorted(Map.Entry.comparingByKey()).collect(toCollection(LinkedHashSet::new));
    }
    
    /**
     * Internal method applying privilege and visibility filtering to controller configurations.
     * <p>
     * If organization ID is null, returns copy of initial set without filtering. Otherwise, applies three-stage 
     * filtering pipeline:
     * 
     * <ol>
     *   <li>Organization scope: Includes configurations with null organization ID or matching the specified organization</li>
     *   <li>Privilege check: Validates user has required privilege via {@link PrivilegeHelper#hasGlobalOrOrgPrivilege(PrivilegeBase, Long)}</li>
     *   <li>Visibility check: Verifies {@link Form#isShowOnOrganizationDashboard()} returns true for associated form</li>
     * </ol>
     * <p>
     * Uses {@link FormRepository#findByName(String)} to retrieve form entity for visibility determination.
     * 
     *
     * @param organizationId the organization ID for scoping, or null to skip filtering
     * @param initialExposedSet the initial set of controller configuration entries to filter
     * @return filtered {@link LinkedHashSet} containing only visible configurations
     * @see PrivilegeHelper#hasGlobalOrOrgPrivilege(PrivilegeBase, Long)
     * @see FormRepository#findByName(String)
     * @see Form#isShowOnOrganizationDashboard()
     */
    private LinkedHashSet<Entry<String, CRUDControllerConfiguration>> getExposed(Long organizationId, Set<Entry<String, CRUDControllerConfiguration>> initialExposedSet) {
        if(organizationId == null){
            return new LinkedHashSet<>(initialExposedSet);
        }
        return initialExposedSet.stream()
                .filter(e -> e.getValue().getOrganizationId() == null || organizationId == null || organizationId.equals(e.getValue().getOrganizationId()))
                .filter( e -> PrivilegeHelper.getInstance().hasGlobalOrOrgPrivilege(e.getValue().getGetAllPrivilege(), organizationId))
                .filter(controller -> {
                    String formName = controller.getValue().getFrontendMappingDefinition().name;
                    Form form = formRepository.findByName(formName);
                    return form.isShowOnOrganizationDashboard();
                })
                .collect(toCollection(LinkedHashSet::new));
    }

    /**
     * Registers a CRUD controller configuration with explicit read and write privileges and marks it for UI exposure.
     * <p>
     * Creates a {@link CRUDControllerConfiguration} by delegating to 
     * {@link AbstractCRUDControllerConfigurationMap#registerCRUDController(FrontendMappingDefinition, ScopedSecureRepository, Class, PrivilegeBase, PrivilegeBase)} 
     * with specified privileges, then calls {@link #setOrgIdAndExpose(String, CRUDControllerConfiguration)} to 
     * associate organization ID from the corresponding {@link Form} and add to {@link #exposed} map.
     * 
     *
     * @param frontendMappingDefinition the form definition containing name and field mappings
     * @param secureRepository the repository for data access with privilege enforcement
     * @param formClass the form class used for binding and validation
     * @param readPrivilege the privilege required for read operations (GET operations)
     * @param writePrivilege the privilege required for write operations (POST, PUT, DELETE operations)
     * @return the registered and exposed {@link CRUDControllerConfiguration}
     * @see #registerAndExposeCRUDController(FrontendMappingDefinition, ScopedSecureRepository, Class)
     * @see #setOrgIdAndExpose(String, CRUDControllerConfiguration)
     */
    public CRUDControllerConfiguration registerAndExposeCRUDController(
            FrontendMappingDefinition frontendMappingDefinition,
            ScopedSecureRepository secureRepository,
            Class formClass,
            PrivilegeBase readPrivilege, PrivilegeBase writePrivilege) {
        CRUDControllerConfiguration c = super.registerCRUDController(frontendMappingDefinition, secureRepository, formClass, readPrivilege, writePrivilege);
        setOrgIdAndExpose(frontendMappingDefinition.name, c);
        return c;
    }

    /**
     * Unregisters a CRUD controller configuration by key, removing it from both exposed and base maps.
     * <p>
     * Removes the configuration from {@link #exposed} map first, then delegates to 
     * {@link AbstractCRUDControllerConfigurationMap#unregisterCRUDController(String)} to remove from base registry.
     * 
     *
     * @param key the unique configuration key to unregister
     * @see AbstractCRUDControllerConfigurationMap#unregisterCRUDController(String)
     */
    @Override
    public void unregisterCRUDController(String key) {
        exposed.remove(key);
        super.unregisterCRUDController(key);
    }

    /**
     * Returns the singleton instance of this configuration map for global access.
     * <p>
     * Provides static access to the Spring-managed singleton instance initialized in {@link #init()}. 
     * Used by {@code CustomisationService} and other components requiring configuration registry access.
     * 
     *
     * @return the singleton {@link HtmlCRUDControllerConfigurationMap} instance
     * @see #instance
     * @see #init()
     */
    public static HtmlCRUDControllerConfigurationMap getControllers() {
        return instance;
    }

    /**
     * Associates organization ID with configuration and adds it to the exposed map.
     * <p>
     * Retrieves {@link Form} entity by name using {@link FormRepository#findByName(String)}, extracts 
     * organization ID (null if form not found), sets it on the configuration via 
     * {@link CRUDControllerConfiguration#setOrganizationId(Long)}, and adds configuration to {@link #exposed} 
     * map using the configuration's key.
     * 
     *
     * @param formName the form name used to look up the form entity
     * @param conf the controller configuration to associate with organization and expose
     * @see FormRepository#findByName(String)
     * @see CRUDControllerConfiguration#setOrganizationId(Long)
     * @see Form#getOrganizationId()
     */
    private void setOrgIdAndExpose(String formName, CRUDControllerConfiguration conf){
        Form form = formRepository.findByName(formName);
        conf.setOrganizationId(form != null ? form.getOrganizationId() : null);
        exposed.put(conf.getKey(), conf);
    }
}
