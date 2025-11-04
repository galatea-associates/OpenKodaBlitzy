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

package com.openkoda.core.service;

import com.openkoda.core.form.*;
import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.core.multitenancy.TenantResolver;
import com.openkoda.model.Privilege;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.common.SearchableEntity;
import com.openkoda.model.common.SearchableOrganizationRelatedEntity;
import com.openkoda.repository.SearchableRepositories;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Function;

import static com.openkoda.core.helper.PrivilegeHelper.valueOfString;

/**
 * Factory service for {@link FrontendMappingDefinition} and {@link ReflectionBasedEntityForm} instances.
 * <p>
 * This service provides multiple factory methods for creating form definitions with various privilege
 * configuration scenarios. It converts privilege strings to {@link PrivilegeBase} using {@link PrivilegeHelper},
 * builds field mappings via reflection using {@link SearchableRepositories} metadata for dynamic entity forms,
 * and seeds tenant-scoped forms with organizationId from {@link TenantResolver}.

 * <p>
 * The service maps Java types to form controls following these rules:
 * <ul>
 *   <li>Enum fields → datalist control</li>
 *   <li>SearchableEntity fields → dropdown control for entity references</li>
 *   <li>Boolean → checkbox control</li>
 *   <li>LocalDate → date control</li>
 *   <li>LocalDateTime → datetime control</li>
 *   <li>Numeric types (primitives and Number subclasses) → number control</li>
 *   <li>Default → text input control</li>
 * </ul>

 * <p>
 * Example usage:
 * <pre>
 * FrontendMappingDefinition def = service.createFrontendMappingDefinition("userForm", 
 *     a -&gt; a.text("name").email("email"));
 * </pre>

 *
 * @see FrontendMappingDefinition
 * @see ReflectionBasedEntityForm
 * @see PrivilegeHelper
 * @see SearchableRepositories
 * @see TenantResolver
 * @see FormFieldDefinitionBuilder
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Service
public class FrontendMappingDefinitionService {

    /**
     * Creates a FrontendMappingDefinition with default privileges.
     * <p>
     * Convenience overload using {@link Privilege#readOrgData} for both read and write privileges.
     * Delegates to the three-parameter variant.

     *
     * @param formName the name of the form definition
     * @param builder function that builds field definitions starting from {@link FormFieldDefinitionBuilderStart}
     * @return a configured {@link FrontendMappingDefinition} instance with default privileges
     * @see #createFrontendMappingDefinition(String, PrivilegeBase, PrivilegeBase, Function)
     */
    public FrontendMappingDefinition createFrontendMappingDefinition(
            String formName,
            Function<FormFieldDefinitionBuilderStart, FormFieldDefinitionBuilder> builder) {
        return FrontendMappingDefinition.createFrontendMappingDefinition(formName, Privilege.readOrgData, Privilege.readOrgData, builder);
    }

    /**
     * Creates a FrontendMappingDefinition with specified read and write privileges.
     * <p>
     * Core factory method creating {@link FrontendMappingDefinition} with explicit read and write privileges
     * and a field definition builder function. This method provides fine-grained privilege control for
     * form field access.

     *
     * @param formName the name of the form definition
     * @param defaultReadPrivilege the privilege required to read form fields
     * @param defaultWritePrivilege the privilege required to write form fields
     * @param builder function that builds field definitions starting from {@link FormFieldDefinitionBuilderStart}
     * @return a configured {@link FrontendMappingDefinition} instance with specified privileges
     */
    public FrontendMappingDefinition createFrontendMappingDefinition(
            String formName,
            PrivilegeBase defaultReadPrivilege,
            PrivilegeBase defaultWritePrivilege,
            Function<FormFieldDefinitionBuilderStart, FormFieldDefinitionBuilder> builder) {
        return FrontendMappingDefinition.createFrontendMappingDefinition(formName, defaultReadPrivilege, defaultWritePrivilege, builder);
    }

    /**
     * Creates a FrontendMappingDefinition with string-based privilege names.
     * <p>
     * String-based privilege variant converting privilege names to {@link PrivilegeBase} via
     * {@link PrivilegeHelper#valueOfString(String)}. This method is useful for dynamic configuration
     * scenarios where privilege names are loaded from external sources.

     *
     * @param formName the name of the form definition
     * @param defaultReadPrivilege the privilege name string required to read form fields
     * @param defaultWritePrivilege the privilege name string required to write form fields
     * @param builder function that builds field definitions starting from {@link FormFieldDefinitionBuilderStart}
     * @return a configured {@link FrontendMappingDefinition} instance with parsed privileges
     * @see PrivilegeHelper#valueOfString(String)
     */
    public FrontendMappingDefinition createFrontendMappingDefinition(
            String formName,
            String defaultReadPrivilege,
            String defaultWritePrivilege,
            Function<FormFieldDefinitionBuilderStart, FormFieldDefinitionBuilder> builder) {
        return FrontendMappingDefinition.createFrontendMappingDefinition(formName, (PrivilegeBase) PrivilegeHelper.valueOfString(defaultReadPrivilege), (PrivilegeBase) valueOfString(defaultWritePrivilege), builder);
    }

    /**
     * Creates a FrontendMappingDefinition with base form fields for inheritance scenarios.
     * <p>
     * Extended variant accepting a {@code baseFormFields} array for form inheritance scenarios.
     * This method builds upon existing field definitions, allowing forms to extend base forms
     * with additional fields while preserving the base field configuration.

     *
     * @param formName the name of the form definition
     * @param defaultReadPrivilege the privilege required to read form fields
     * @param defaultWritePrivilege the privilege required to write form fields
     * @param baseFormFields array of existing field definitions to inherit from
     * @param builder function that builds additional field definitions
     * @return a configured {@link FrontendMappingDefinition} instance with inherited and new fields
     */
    public FrontendMappingDefinition createFrontendMappingDefinition(
            String formName,
            PrivilegeBase defaultReadPrivilege,
            PrivilegeBase defaultWritePrivilege,
            FrontendMappingFieldDefinition[] baseFormFields,
            Function<FormFieldDefinitionBuilderStart, FormFieldDefinitionBuilder> builder) {
        return FrontendMappingDefinition.createFrontendMappingDefinition(formName, defaultReadPrivilege, defaultWritePrivilege, baseFormFields, builder);
    }

    /**
     * Creates a ReflectionBasedEntityForm with default privileges and no organization scope.
     * <p>
     * Convenience method that creates a {@link ReflectionBasedEntityForm} with default privileges
     * and null organizationId. Delegates to the ReflectionBasedEntityForm constructor.

     *
     * @param formName the name of the form
     * @param builder function that builds field definitions starting from {@link FormFieldDefinitionBuilderStart}
     * @return a configured {@link ReflectionBasedEntityForm} instance without tenant scope
     * @see ReflectionBasedEntityForm
     */
    public ReflectionBasedEntityForm getForm(String formName,
                                             Function<FormFieldDefinitionBuilderStart, FormFieldDefinitionBuilder> builder) {
        return new ReflectionBasedEntityForm(createFrontendMappingDefinition(formName, builder), null, null);
    }

    /**
     * Creates a tenant-scoped ReflectionBasedEntityForm with specified organizationId.
     * <p>
     * This method creates a form with specified organizationId for multi-tenancy support.
     * The organizationId is used to scope form data to a specific tenant/organization.

     *
     * @param formName the name of the form
     * @param organizationId the organization ID for tenant scoping
     * @param builder function that builds field definitions starting from {@link FormFieldDefinitionBuilderStart}
     * @return a configured {@link ReflectionBasedEntityForm} instance with tenant scope
     * @see ReflectionBasedEntityForm
     * @see TenantResolver
     */
    public ReflectionBasedEntityForm getForm(String formName, Long organizationId,
                                             Function<FormFieldDefinitionBuilderStart, FormFieldDefinitionBuilder> builder) {
        return new ReflectionBasedEntityForm(createFrontendMappingDefinition(formName, builder), organizationId, null);
    }

    /**
     * Creates a ReflectionBasedEntityForm with custom privilege name.
     * <p>
     * This method creates a form with a custom privilege name string converted to {@link PrivilegeBase}.
     * The same privilege is used for both read and write operations, providing simplified
     * privilege configuration when read and write privileges are identical.

     *
     * @param formName the name of the form
     * @param privilegeName the privilege name string for both read and write access
     * @param builder function that builds field definitions starting from {@link FormFieldDefinitionBuilderStart}
     * @return a configured {@link ReflectionBasedEntityForm} instance with specified privilege
     * @see PrivilegeHelper#valueOfString(String)
     */
    public ReflectionBasedEntityForm getForm(String formName, String privilegeName,
                                             Function<FormFieldDefinitionBuilderStart, FormFieldDefinitionBuilder> builder) {
        PrivilegeBase requiredPrivilege = (PrivilegeBase) valueOfString(privilegeName);
        return new ReflectionBasedEntityForm(createFrontendMappingDefinition(formName, requiredPrivilege, requiredPrivilege, builder), null, null);
    }

    /**
     * Creates a tenant-scoped ReflectionBasedEntityForm with custom privilege.
     * <p>
     * Combined tenant and privilege variant for fine-grained access control. This method
     * creates a form scoped to a specific organization with custom privilege requirements,
     * enabling both multi-tenancy and privilege-based field access control.

     *
     * @param formName the name of the form
     * @param organizationId the organization ID for tenant scoping
     * @param privilegeName the privilege name string for both read and write access
     * @param builder function that builds field definitions starting from {@link FormFieldDefinitionBuilderStart}
     * @return a configured {@link ReflectionBasedEntityForm} instance with tenant scope and privilege
     * @see TenantResolver
     * @see PrivilegeHelper#valueOfString(String)
     */
    public ReflectionBasedEntityForm getForm(String formName, Long organizationId, String privilegeName,
                                             Function<FormFieldDefinitionBuilderStart, FormFieldDefinitionBuilder> builder) {
        PrivilegeBase requiredPrivilege = (PrivilegeBase) valueOfString(privilegeName);
        return new ReflectionBasedEntityForm(createFrontendMappingDefinition(formName, requiredPrivilege, requiredPrivilege, builder), organizationId, null);
    }

    /**
     * Creates a dynamic entity form from SearchableRepositories metadata.
     * <p>
     * This method creates a form automatically mapped to an entity using reflection-based
     * field discovery. It retrieves the entity class from {@link SearchableRepositories} metadata
     * using the entity key, uses {@link TenantResolver} to obtain the current organization ID,
     * and applies default {@link Privilege#readOrgData} for both read and write operations.
     * Field mappings are generated by {@link #getBuilderForEntity(String)}.

     * <p>
     * <strong>Note:</strong> This method assumes current tenant context is available from TenantResolver.

     *
     * @param entityKey the entity key used to lookup metadata in SearchableRepositories
     * @return a configured {@link ReflectionBasedEntityForm} instance with reflection-based field mappings
     * @see SearchableRepositories#getSearchableRepositoryMetadata(String)
     * @see TenantResolver#getTenantedResource()
     * @see #getBuilderForEntity(String)
     */
    public ReflectionBasedEntityForm getEntityForm(String entityKey) {
        PrivilegeBase requiredPrivilege = Privilege.readOrgData;
        return new ReflectionBasedEntityForm(createFrontendMappingDefinition(entityKey + "Form",
                requiredPrivilege, requiredPrivilege, getBuilderForEntity(entityKey)), TenantResolver.getTenantedResource().organizationId, null);
    }

    /**
     * Creates a reflection-based form builder factory for the specified entity.
     * <p>
     * This private method retrieves the entity class from {@link SearchableRepositories} metadata
     * and generates a form builder function that maps entity fields to appropriate form controls
     * using reflection. The mapping logic follows these rules:

     * <ul>
     *   <li>Enum constants → skipped (not regular fields)</li>
     *   <li>{@link SearchableEntity} fields → dropdownWithDisable control for entity relationships</li>
     *   <li>Fields ending with "Id" → skipped (handled by relationship fields)</li>
     *   <li>{@link Boolean} → checkbox control</li>
     *   <li>{@link LocalDate} → date control</li>
     *   <li>{@link LocalDateTime} → datetime control</li>
     *   <li>Numeric types (int, double, long, float, or {@link Number} subclasses) → number control</li>
     *   <li>All other types → text input control (default)</li>
     * </ul>
     * <p>
     * The generated builder always starts with a hidden "id" field, then processes all declared
     * fields of the entity class in declaration order.

     *
     * @param entityKey the entity key used to lookup metadata in SearchableRepositories
     * @return a Function that transforms {@link FormFieldDefinitionBuilderStart} to complete
     *         {@link FormFieldDefinitionBuilder} with all entity fields mapped
     * @see SearchableRepositories#getSearchableRepositoryMetadata(String)
     * @see FormFieldDefinitionBuilder
     */
    private Function<FormFieldDefinitionBuilderStart, FormFieldDefinitionBuilder> getBuilderForEntity(String entityKey) {
        Class<SearchableOrganizationRelatedEntity> entityClass = (Class<SearchableOrganizationRelatedEntity>) SearchableRepositories.getSearchableRepositoryMetadata(entityKey).entityClass();
        Function<FormFieldDefinitionBuilderStart, FormFieldDefinitionBuilder> result =
            a -> {
                FormFieldDefinitionBuilder s = a.hidden("id");
                for (Field f : entityClass.getDeclaredFields()) {
                    Class type = f.getType();
                    String name = f.getName();
                    if (f.isEnumConstant()) {
                        s = s.datalist(name,
                                d -> d.enumDictionary((Enum[])type.getEnumConstants()))
                                .dropdown(name, name);
                    } else if(SearchableEntity.class.isAssignableFrom(type)) {
                        s.datalist(name, d -> d.dictionary(type))
                        .dropdownWithDisable(name + "Id", name);
                    } else if(name.endsWith("Id")) {
                        continue;
                    } else if(Boolean.class.equals(type)) {
                        s.checkbox(name);
                    } else if(LocalDate.class.equals(type)) {
                        s.date(name);
                    } else if(LocalDateTime.class.equals(type)) {
                        s.datetime(name);
                    } else if(type == int.class || type == double.class || type == long.class || type == float.class || Number.class.isAssignableFrom(type)) {
                        s.number(name);
                    } else {
                        s = s.text(f.getName());
                    }
                }
                return s;
            };
        return result;
    }


}
