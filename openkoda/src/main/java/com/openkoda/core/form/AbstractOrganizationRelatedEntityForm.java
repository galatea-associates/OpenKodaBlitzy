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


import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.dto.OrganizationRelatedObject;
import com.openkoda.model.common.OrganizationRelatedEntity;
import reactor.util.function.Tuples;

/**
 * Organization-scoped extension of {@link AbstractEntityForm} for tenant-aware entity binding.
 * <p>
 * This abstract form class provides multi-tenancy support by associating form instances with a specific
 * organization context. It extends the base entity form functionality with organization-scoped privilege
 * evaluation, ensuring that field read/write permissions are checked against both global and organization-specific
 * role assignments.
 * <p>
 * The form validates tenant consistency during construction by invoking {@code assertFormConsistency()},
 * which ensures the organization ID stored in the form matches the organization ID of the bound entity.
 * This validation prevents cross-tenant data leakage in multi-tenant deployments.
 * <p>
 * Example usage:
 * <pre>{@code
 * MyOrgForm form = new MyOrgForm(orgId, dto, entity, mapping);
 * form.populateFrom(entity);
 * if (form.validate()) {
 *     form.populateTo(entity);
 * }
 * }</pre>
 *
 * @param <C> the DTO type extending {@link OrganizationRelatedObject}, representing the data transfer object
 *            that carries organization context
 * @param <E> the entity type extending {@link OrganizationRelatedEntity}, representing the JPA entity
 *            that is scoped to an organization
 * @see AbstractEntityForm
 * @see OrganizationRelatedEntity
 * @see MapEntityForm
 * @see OrganizationRelatedObject
 * @see HasSecurityRules
 * @since 1.7.1
 * @author OpenKoda Team
 */
public abstract class AbstractOrganizationRelatedEntityForm<C extends OrganizationRelatedObject, E extends OrganizationRelatedEntity>
        extends AbstractEntityForm<C, E> implements OrganizationRelatedObject, HasSecurityRules {

    /**
     * The organization ID storing the tenant context for multi-tenancy support.
     * <p>
     * This field identifies which organization (tenant) this form instance belongs to, enabling
     * organization-scoped privilege evaluation and ensuring data isolation in multi-tenant deployments.
     * The organization ID is validated against the bound entity's organization during form construction.
     * 
     *
     * @see OrganizationRelatedEntity#getOrganizationId()
     */
    private Long organizationId;

    /**
     * Constructs a new organization-related entity form with no initial configuration.
     * <p>
     * This no-argument constructor creates an empty form instance with no frontend mapping definition.
     * Typically used for scenarios where the form configuration will be set later through setters.
     * 
     */
    public AbstractOrganizationRelatedEntityForm() {
        super(null);
    }

    /**
     * Constructs a new organization-related entity form with the specified frontend mapping definition.
     * <p>
     * This constructor initializes the form with field definitions from the provided mapping,
     * but without binding to a specific organization, DTO, or entity. Use this constructor when
     * you need to establish the form structure before setting the organizational context.
     * 
     *
     * @param frontendMappingDefinition the frontend mapping definition containing field definitions
     *                                   and validation rules for the form
     */
    public AbstractOrganizationRelatedEntityForm(FrontendMappingDefinition frontendMappingDefinition) {
        super(frontendMappingDefinition);
    }

    /**
     * Constructs a fully-configured organization-related entity form with tenant context validation.
     * <p>
     * This constructor creates a form instance bound to a specific organization, DTO, and entity,
     * with the provided frontend mapping definition. After initialization, it invokes
     * {@code assertFormConsistency()} to validate that the organization ID provided matches
     * the organization ID of the bound entity, preventing cross-tenant data access violations.
     * 
     *
     * @param organizationId the organization ID representing the tenant context for this form
     * @param dto the data transfer object containing form data, must extend {@link OrganizationRelatedObject}
     * @param entity the JPA entity to bind to this form, must extend {@link OrganizationRelatedEntity}
     * @param frontendMappingDefinition the frontend mapping definition containing field definitions
     *                                   and validation rules
     * @throws AssertionError if the organization ID does not match the entity's organization ID
     *                         (when assertions are enabled)
     */
    public AbstractOrganizationRelatedEntityForm(Long organizationId, C dto, E entity, FrontendMappingDefinition frontendMappingDefinition) {
        super(dto, entity, frontendMappingDefinition);
        this.organizationId = organizationId;
        assertFormConsistency(this);
    }

    /**
     * Returns the organization ID representing the tenant context for this form.
     * <p>
     * This method implements the {@link OrganizationRelatedObject} interface, providing
     * tenant identification for multi-tenancy support. The returned organization ID is used
     * throughout the application to scope data access and privilege evaluation to the appropriate tenant.
     * 
     *
     * @return the organization ID for this form instance, or {@code null} if no organization context is set
     * @see OrganizationRelatedObject#getOrganizationId()
     */
    @Override
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Prepares field-level read and write privileges using organization-scoped privilege evaluation.
     * <p>
     * This method overrides the base {@link com.openkoda.core.form.AbstractEntityForm#prepareFieldsReadWritePrivileges}
     * to apply organization-scoped security checks. For each field defined in the frontend mapping,
     * it evaluates whether the current user has permission to read and write that field within the
     * context of the specified organization.
     * 
     * <p>
     * Organization-scoped privileges check both global role assignments (which apply across all organizations)
     * and organization-specific role assignments (which apply only within the specified organization).
     * This dual-level evaluation enables fine-grained access control in multi-tenant environments where
     * users may have different permissions in different organizations.
     * 
     * <p>
     * The privilege evaluation uses {@link com.openkoda.core.helper.PrivilegeHelper}
     * to determine field-level access permissions. Results are stored in the {@code readWriteForField} map
     * for later use during form rendering and validation.
     * 
     *
     * @param entity the entity instance for which to prepare field privileges
     * @see com.openkoda.core.helper.PrivilegeHelper
     */
    @Override
    final public void prepareFieldsReadWritePrivileges(E entity) {
        for (FrontendMappingFieldDefinition f : frontendMappingDefinition.fields) {
            readWriteForField.put(f,
                    Tuples.of(
                            PrivilegeHelper.getInstance().canReadFieldInOrganization(f, entity, organizationId),
                            PrivilegeHelper.getInstance().canWriteFieldInOrganization(f, entity, organizationId)));
        }
    }
}
