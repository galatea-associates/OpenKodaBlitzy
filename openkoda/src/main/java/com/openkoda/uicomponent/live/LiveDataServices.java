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

package com.openkoda.uicomponent.live;

import com.openkoda.core.customisation.FrontendMapping;
import com.openkoda.core.customisation.FrontendMappingMap;
import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.core.form.CRUDControllerConfiguration;
import com.openkoda.core.form.ReflectionBasedEntityForm;
import com.openkoda.core.multitenancy.TenantResolver;
import com.openkoda.core.repository.common.ScopedSecureRepository;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.core.service.ValidationService;
import com.openkoda.form.RegisterUserForm;
import com.openkoda.model.User;
import com.openkoda.model.common.SearchableOrganizationRelatedEntity;
import com.openkoda.repository.SearchableRepositories;
import com.openkoda.service.user.UserService;
import com.openkoda.uicomponent.DataServices;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;

/**
 * Provides data repository access and form management services for UI components.
 * <p>
 * This implementation of the {@link DataServices} interface offers tenant-aware
 * data operations with privilege enforcement through secure repositories. It manages
 * form creation, validation, and persistence for organization-scoped entities.
 * </p>
 * <p>
 * Key features:
 * <ul>
 * <li>Secure repository access with configurable security scopes</li>
 * <li>Dynamic form creation from frontend mapping definitions</li>
 * <li>Entity validation using {@link ValidationService}</li>
 * <li>Tenant-scoped operations via {@link TenantResolver}</li>
 * <li>User registration with idempotent behavior</li>
 * </ul>
 * </p>
 * <p>
 * This class is stateless and thread-safe. All injected dependencies are thread-safe.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see DataServices
 * @see SearchableRepositories
 * @see ValidationService
 * @see FrontendMappingMap
 */
@Component
public class LiveDataServices implements DataServices {
    /**
     * Maps frontend mapping names to form definitions and repository configurations.
     * Used to resolve entity forms by name for dynamic form creation.
     */
    @Inject
    private FrontendMappingMap frontendMappingMap;

    /**
     * Validates entity data and populates form values to entity instances.
     * Performs Jakarta Bean Validation and binding result management.
     */
    @Inject
    private ValidationService validationService;

    /**
     * Manages user registration and retrieval operations.
     * Provides idempotent user creation from registration forms.
     */
    @Inject
    private UserService userService;

    /**
     * Retrieves a secure repository for the specified entity with default USER_IN_ORGANIZATION scope.
     * <p>
     * The repository enforces privilege checks based on the user's organization membership.
     * All operations require the user to be authenticated and associated with an organization.
     * </p>
     *
     * @param entityKey the entity type identifier (e.g., "User", "Organization")
     * @return a {@link ScopedSecureRepository} with USER_IN_ORGANIZATION security scope
     * @see #getRepository(String, HasSecurityRules.SecurityScope)
     */
    public ScopedSecureRepository<?> getRepository(String entityKey) {
        return SearchableRepositories.getSearchableRepository(entityKey, HasSecurityRules.SecurityScope.USER_IN_ORGANIZATION);
    }
    /**
     * Retrieves a secure repository for the specified entity with a custom security scope.
     * <p>
     * Allows explicit control over privilege enforcement level. Common scopes include
     * USER_IN_ORGANIZATION, GLOBAL, and ORGANIZATION_ADMIN.
     * </p>
     *
     * @param entityKey the entity type identifier (e.g., "User", "Organization")
     * @param securityScope the security scope defining privilege enforcement rules
     * @return a {@link ScopedSecureRepository} with the specified security scope
     * @see HasSecurityRules.SecurityScope
     */
    public ScopedSecureRepository<?> getRepository(String entityKey, HasSecurityRules.SecurityScope securityScope) {
        return SearchableRepositories.getSearchableRepository(entityKey, securityScope);
    }
    /**
     * Retrieves a secure repository with security scope parsed from a string value.
     * <p>
     * Convenience method for dynamic scope resolution from configuration or UI input.
     * The scope string must match a valid {@link HasSecurityRules.SecurityScope} enum name.
     * </p>
     *
     * @param entityKey the entity type identifier (e.g., "User", "Organization")
     * @param securityScope string representation of the security scope (e.g., "USER_IN_ORGANIZATION")
     * @return a {@link ScopedSecureRepository} with the parsed security scope
     * @throws IllegalArgumentException if the securityScope string is not a valid enum constant
     * @see #getRepository(String, HasSecurityRules.SecurityScope)
     */
    public ScopedSecureRepository<?> getRepository(String entityKey, String securityScope) {
        return SearchableRepositories.getSearchableRepository(entityKey, HasSecurityRules.SecurityScope.valueOf(securityScope));
    }
    /**
     * Creates and initializes a form for the specified entity using frontend mapping definition.
     * <p>
     * The form is populated from the entity if provided, or a new entity instance is created
     * for the current tenant. Binding results are initialized for validation support.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * AbstractOrganizationRelatedEntityForm form = dataServices.getForm("userForm", user);
     * }</pre>
     * </p>
     *
     * @param frontendMappingName the name of the frontend mapping definition
     * @param entity the entity to populate the form from, or null to create a new entity
     * @return an initialized form with binding results ready for validation
     * @see FrontendMappingMap
     * @see ReflectionBasedEntityForm
     */
    public AbstractOrganizationRelatedEntityForm getForm(String frontendMappingName, SearchableOrganizationRelatedEntity entity) {
        FrontendMapping frontendMapping = frontendMappingMap.get(frontendMappingName);
        CRUDControllerConfiguration conf = CRUDControllerConfiguration.getBuilder("form", frontendMapping.definition(), frontendMapping.repository(), ReflectionBasedEntityForm.class);
        Long orgId = TenantResolver.getTenantedResource().organizationId;
        if(entity == null) {
            entity = conf.createNewEntity(orgId);
        }
        ReflectionBasedEntityForm result = (ReflectionBasedEntityForm) conf.createNewForm(orgId, entity);

        if(result.getBindingResult() == null){
            result.setBindingResult(new BeanPropertyBindingResult(result, result.getFrontendMappingDefinition().name));
        }

        return result;
    }

    /**
     * Registers a new user or returns an existing user with the specified email.
     * <p>
     * This method provides idempotent user registration behavior. If a user with the
     * given email already exists, that user is returned without modification. Otherwise,
     * a new user is created with the provided details.
     * </p>
     *
     * @param email the user's email address, used as login identifier
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @return the registered or existing {@link User} instance
     * @see UserService#registerUserOrReturnExisting
     */
    @Override
    public User registerUserOrReturnExisting(String email, String firstName, String lastName) {
        RegisterUserForm form = new RegisterUserForm();
        form.setLogin(email);
        form.setFirstName(firstName);
        form.setLastName(lastName);
        return userService.registerUserOrReturnExisting(form, false);
    }

    /**
     * Creates a form for a new entity using the specified frontend mapping definition.
     * <p>
     * Convenience method that creates a form with a new entity instance for the current tenant.
     * Equivalent to calling {@link #getForm(String, SearchableOrganizationRelatedEntity)} with null entity.
     * </p>
     *
     * @param frontendMappingName the name of the frontend mapping definition
     * @return an initialized form with a new entity instance
     * @see #getForm(String, SearchableOrganizationRelatedEntity)
     */
    public AbstractOrganizationRelatedEntityForm getForm(String frontendMappingName) {
        return getForm(frontendMappingName, null);
    }

    /**
     * Validates form data and persists the entity with tenant-aware security checks.
     * <p>
     * The form is validated using Jakarta Bean Validation, then populated to the entity.
     * If validation fails, binding errors are recorded in the form's binding result.
     * The entity is persisted using a secure repository that enforces privilege checks.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * SearchableOrganizationRelatedEntity saved = dataServices.saveForm(form, entity);
     * }</pre>
     * </p>
     *
     * @param form the validated form containing user input
     * @param entity the entity to populate and save, or null to create a new entity
     * @return the persisted entity with generated ID and updated fields
     * @see ValidationService#validateAndPopulateToEntity
     * @see ScopedSecureRepository#saveOne
     */
    public SearchableOrganizationRelatedEntity saveForm(AbstractOrganizationRelatedEntityForm form, SearchableOrganizationRelatedEntity entity) {
        FrontendMapping frontendMapping = frontendMappingMap.get(form.frontendMappingDefinition.name);

        CRUDControllerConfiguration conf = CRUDControllerConfiguration.getBuilder("form", frontendMapping.definition(), frontendMapping.repository(), ReflectionBasedEntityForm.class);
        Long orgId = TenantResolver.getTenantedResource().organizationId;
        if (entity == null) {
            entity = conf.createNewEntity(orgId);
        }

        SearchableOrganizationRelatedEntity e = validationService.validateAndPopulateToEntity(form, form.getBindingResult(), entity);
        e = (SearchableOrganizationRelatedEntity) conf.getSecureRepository().saveOne(e);
        return e;

    }

    /**
     * Validates and persists a new entity from form data.
     * <p>
     * Convenience method that creates a new entity for the current tenant before saving.
     * Equivalent to calling {@link #saveForm(AbstractOrganizationRelatedEntityForm, SearchableOrganizationRelatedEntity)}
     * with null entity.
     * </p>
     *
     * @param form the validated form containing user input for the new entity
     * @return the persisted entity with generated ID
     * @see #saveForm(AbstractOrganizationRelatedEntityForm, SearchableOrganizationRelatedEntity)
     */
    @Override
    public SearchableOrganizationRelatedEntity saveForm(AbstractOrganizationRelatedEntityForm form) {
        return saveForm(form, null);
    }


}
