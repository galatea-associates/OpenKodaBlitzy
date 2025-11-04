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

package com.openkoda.uicomponent;

import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.core.repository.common.ScopedSecureRepository;
import com.openkoda.model.User;
import com.openkoda.model.common.SearchableOrganizationRelatedEntity;
import com.openkoda.uicomponent.annotation.Autocomplete;

/**
 * UI-facing service contract for data repository access, form-to-entity mapping, and user registration.
 * <p>
 * This interface defines methods for retrieving security-scoped repositories by entity name or key,
 * supporting form-to-entity persistence with validation and mapping, providing form retrieval by
 * frontend mapping name (new or entity-populated), and offering user registration with duplicate detection.

 * <p>
 * All methods are annotated with {@link Autocomplete} to provide UI tooling metadata for code completion
 * and documentation hints. Implementations handle tenant scoping, security enforcement, and transactional
 * semantics to ensure data integrity and access control.

 * <p>
 * Key capabilities:
 * <ul>
 * <li>Repository retrieval with configurable security scopes (USER_IN_ORGANIZATION, GLOBAL, ORGANIZATION)</li>
 * <li>Form validation and entity persistence with privilege enforcement</li>
 * <li>Form creation and population for new entity and edit workflows</li>
 * <li>Idempotent user registration with email-based duplicate detection</li>
 * </ul>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ScopedSecureRepository
 * @see AbstractOrganizationRelatedEntityForm
 * @see SearchableOrganizationRelatedEntity
 * @see User
 */
public interface DataServices {
    /**
     * Retrieves security-scoped repository for entity using default USER_IN_ORGANIZATION scope.
     * <p>
     * Resolves repository from SearchableRepositories.getSearchableRepository(entityName) with default
     * security scope. Repository enforces read and write privileges per logged-in user and organization context.
     * Thread-safe and stateless.

     * <p>
     * Example usage:
     * <pre>{@code
     * ScopedSecureRepository<?> repo = getRepository("user");
     * }</pre>

     *
     * @param entityName Entity name or key for repository lookup (e.g., "organization", "user", "dynamicEntity")
     * @return ScopedSecureRepository instance with privilege enforcement for the entity type
     * @throws RuntimeException if entity name not found in SearchableRepositories registry
     */
    @Autocomplete(doc="Get data repository for an entity using its key value")
    ScopedSecureRepository<?> getRepository(String entityName);
    
    /**
     * Retrieves security-scoped repository for entity with specified security scope.
     * <p>
     * Resolves repository from SearchableRepositories.getSearchableRepository(entityKey) and applies
     * securityScope via PrivilegeScopeEnum.valueOf(securityScope). Allows explicit control of privilege
     * checking level. Security scope affects which entity records are visible and modifiable.

     * <p>
     * Example usage:
     * <pre>{@code
     * ScopedSecureRepository<?> repo = getRepository("role", "GLOBAL");
     * }</pre>

     *
     * @param entityKey Entity name or key for repository lookup
     * @param securityScope Security scope name (e.g., "USER_IN_ORGANIZATION", "GLOBAL", "ORGANIZATION")
     * @return ScopedSecureRepository instance with specified privilege enforcement scope
     * @throws RuntimeException if entity key or security scope not found
     */
    @Autocomplete(doc="Get data repository for an entity using its key value")
    ScopedSecureRepository<?> getRepository(String entityKey, String securityScope);
    
    /**
     * Validates form, creates new entity, populates from form, persists with security checks.
     * <p>
     * Validates form via validationService.validateAndPopulateToEntity, creates new entity instance,
     * populates fields from form, resolves organization ID via TenantResolver, persists via
     * SecureRepository.saveOne with privilege enforcement. Returns persisted entity with generated ID
     * and timestamps. Form must pass validation and organization context must be available.

     * <p>
     * Example usage:
     * <pre>{@code
     * SearchableOrganizationRelatedEntity entity = saveForm(organizationForm);
     * }</pre>

     *
     * @param form Form containing user input and validation rules
     * @return Persisted SearchableOrganizationRelatedEntity with generated ID and timestamps
     * @throws RuntimeException if form validation fails (hasErrors=true)
     */
    @Autocomplete(doc="Save form data as a new entity record")
    SearchableOrganizationRelatedEntity saveForm(AbstractOrganizationRelatedEntityForm form);
    
    /**
     * Validates form, updates existing entity, populates from form, persists with security checks.
     * <p>
     * Validates form, populates entity fields from form via populateTo, persists via
     * SecureRepository.saveOne. Preserves entity ID, updates timestamp. Enforces update privileges.
     * Entity must exist and be accessible to current user per security scope.

     * <p>
     * Example usage:
     * <pre>{@code
     * SearchableOrganizationRelatedEntity updated = saveForm(userForm, existingUser);
     * }</pre>

     *
     * @param form Form containing user input and validation rules
     * @param entity Existing entity to update (must have ID)
     * @return Updated SearchableOrganizationRelatedEntity with modified fields and updated timestamp
     * @throws RuntimeException if form validation fails
     */
    @Autocomplete(doc="Update an entity with form data")
    SearchableOrganizationRelatedEntity saveForm(AbstractOrganizationRelatedEntityForm form, SearchableOrganizationRelatedEntity entity);
    
    /**
     * Retrieves empty form instance by frontend mapping name, ready for new entity creation.
     * <p>
     * Resolves form builder from CRUDControllerConfiguration.getBuilder(frontendMappingName), creates
     * form with default constructor. Form is ready for user input, validation, and entity creation.
     * Used for new entity creation workflows.

     * <p>
     * Example usage:
     * <pre>{@code
     * AbstractOrganizationRelatedEntityForm form = getForm("organizationForm");
     * }</pre>

     *
     * @param frontendMappingName Form identifier (e.g., "organizationForm", "userForm")
     * @return AbstractOrganizationRelatedEntityForm instance initialized with default values
     * @throws RuntimeException if frontendMappingName not found in FrontendMappingMap
     */
    @Autocomplete(doc="Retrieve a form by its identifier (key)")
    AbstractOrganizationRelatedEntityForm getForm(String frontendMappingName);
    
    /**
     * Retrieves form instance populated from existing entity for editing.
     * <p>
     * Creates form via CRUDControllerConfiguration.getBuilder, calls form.populateFrom(entity) to
     * transfer entity fields to form fields. Form is ready for display, modification, validation,
     * and update. Used for entity edit workflows where form reflects current entity state.

     * <p>
     * Example usage:
     * <pre>{@code
     * AbstractOrganizationRelatedEntityForm form = getForm("userForm", user);
     * }</pre>

     *
     * @param frontendMappingName Form identifier matching entity type
     * @param entity Entity to populate form from
     * @return AbstractOrganizationRelatedEntityForm with fields populated from entity
     * @throws RuntimeException if frontendMappingName not found or entity type mismatch
     */
    @Autocomplete(doc="Retrieve a form associated with a provided entity object")
    AbstractOrganizationRelatedEntityForm getForm(String frontendMappingName, SearchableOrganizationRelatedEntity entity);
    
    /**
     * Registers new user with email and name or returns existing user if email already registered.
     * <p>
     * Delegates to userService.registerUserOrReturnExisting(email, firstName, lastName). If user with
     * email exists, returns existing User. If not, creates User with email, first name, last name,
     * generates default login, persists and returns new User. Idempotent operation with email uniqueness
     * constraint preventing duplicates. Transaction ensures consistency.

     * <p>
     * Example usage:
     * <pre>{@code
     * User user = registerUserOrReturnExisting("test@example.com", "John", "Doe");
     * }</pre>

     *
     * @param email User email address (unique identifier, case-insensitive)
     * @param firstName User's first name
     * @param lastName User's last name
     * @return User entity (newly created or existing)
     */
    @Autocomplete(doc="Register a new user or return an existing user's data")
    User registerUserOrReturnExisting(String email, String firstName, String lastName);
}
