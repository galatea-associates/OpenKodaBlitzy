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
import com.openkoda.model.common.LongIdEntity;
import com.openkoda.repository.SecureEntityDictionaryRepository;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.BindingResult;
import reactor.util.function.Tuples;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Central base class for DTO-backed forms with reflection-based field mapping and validation.
 * <p>
 * AbstractForm extends {@link Form} to provide automated mapping between form fields and DTO properties.
 * It caches field accessor functions in a static map keyed by (mapping.name + '#' + dtoClass) to optimize
 * reflection-based property access. The cache can be invalidated via {@link #markDirty(String)} when
 * form definitions change dynamically.
 * </p>
 * <p>
 * The class provides privilege-aware form processing through {@link #getSafeValue} methods that enforce
 * write permissions before populating entity fields from DTO values. Field-level validation is supported
 * via {@link #validateField} overloads that inject error codes into Spring's BindingResult.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * form.populateFrom(entity);
 * if (form.validate(bindingResult)) {
 *     form.populateTo(entity);
 * }
 * }</pre>
 * </p>
 *
 * @param <D> the DTO type for form binding (JavaBean or Map-based)
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @see AbstractEntityForm
 * @see FrontendMappingDefinition
 * @see ReflectionBasedEntityForm
 * @since 1.7.1
 */
public abstract class AbstractForm<D> extends Form implements DtoAndEntity<D, LongIdEntity> {

    /**
     * Repository providing datalist lookups for form dropdowns and multiselects.
     * <p>
     * Initialized once via {@link #setSecureEntityDictionaryRepositoryOnce(SecureEntityDictionaryRepository)}
     * during application startup. Provides access to entity dictionaries for generating HTML select elements
     * with privilege-aware filtering.
     * </p>
     *
     * @see SecureEntityDictionaryRepository
     */
    protected static SecureEntityDictionaryRepository dictionaryRepository = null;

    /**
     * The DTO instance holding form data for binding and validation.
     * <p>
     * This field stores the data transfer object that backs the form. It can be a JavaBean with
     * properties accessible via getter methods, or a Map-based DTO (such as OrganizationRelatedMap)
     * for dynamic field structures.
     * </p>
     */
    public D dto;

    /**
     * Field name for partial form updates, limiting submission to a single field.
     * <p>
     * When set, only the specified field will be updated during {@link #getSafeValue} operations.
     * All other fields are ignored. This enables AJAX-style single-field updates without
     * resubmitting the entire form.
     * </p>
     */
    protected String singleFieldToUpdate;

    /**
     * Static cache mapping form definitions to field accessor functions.
     * <p>
     * The outer map key is formName + '#' + dtoClassName. The inner map associates field names
     * with Functions that extract values from DTO instances. This cache is built once per
     * form-DTO pair via {@link #detectFieldMapping(Object)} and reused across all form instances
     * of that type, optimizing reflection overhead.
     * </p>
     */
    private static Map<String, Map<String, Function>> fieldMapping = new HashMap<>();

    /**
     * Set of form names requiring field mapping cache rebuild.
     * <p>
     * Forms added to this set via {@link #markDirty(String)} will have their cached field mappings
     * regenerated on next access. This supports dynamic form definition changes at runtime.
     * Entries are removed from this set after successful cache rebuild.
     * </p>
     */
    private static Set<String> dirtyFrontendMapping = new HashSet<>();


    /**
     * Constructs a form with the specified DTO and frontend mapping definition.
     * <p>
     * Initializes the form by detecting field mappings via reflection and caching accessor functions
     * for efficient property retrieval. If the DTO is non-null, builds or reuses cached field mappings
     * for the form-DTO pair.
     * </p>
     *
     * @param dto the data transfer object backing this form (may be null)
     * @param frontendMappingDefinition the form field definitions for HTML generation and validation
     */
    public AbstractForm(D dto, FrontendMappingDefinition frontendMappingDefinition) {
        super(frontendMappingDefinition);
        detectFieldMapping(dto);
        this.dto = dto;
    }

    /**
     * Constructs a form with frontend mapping definition and null DTO.
     * <p>
     * Convenience constructor for cases where the DTO will be set later via {@link #setDto(Object)}.
     * Field mappings will be detected when the DTO is assigned.
     * </p>
     *
     * @param frontendMappingDefinition the form field definitions for HTML generation and validation
     */
    public AbstractForm(FrontendMappingDefinition frontendMappingDefinition) {
        this(null, frontendMappingDefinition);
    }

    /**
     * Validates a single form field and registers errors in the BindingResult.
     * <p>
     * Retrieves the field value from the DTO via cached accessor functions, applies optional
     * type conversion through {@link #convertDtoValue(FrontendMappingFieldDefinition, Object)},
     * then invokes the validator function. If validation fails (non-blank error code returned),
     * rejects the field value in the provided BindingResult.
     * </p>
     *
     * @param ffd the field definition identifying which field to validate
     * @param fieldValidator function accepting field value and returning error code (blank if valid)
     * @param br Spring BindingResult to receive validation errors
     */
    public void validateField(FrontendMappingFieldDefinition ffd, Function<Object, String> fieldValidator, BindingResult br) {
        Object dtoValue = getField(ffd.getPlainName());
        dtoValue = convertDtoValue(ffd, dtoValue);
        String errorCode = fieldValidator.apply(dtoValue);
        if (StringUtils.isNotBlank(errorCode)) {
            br.rejectValue(ffd.getName(isMapDto()), errorCode);
        }
    }

    /**
     * Extension point for custom type conversion before field validation.
     * <p>
     * Subclasses can override this method to transform DTO values before they are validated.
     * Common use cases include string trimming, numeric formatting, or enum conversion.
     * The default implementation returns the value unchanged.
     * </p>
     *
     * @param ffd the field definition providing metadata for conversion
     * @param dtoValue the raw value extracted from the DTO
     * @return the converted value ready for validation
     */
    protected Object convertDtoValue(FrontendMappingFieldDefinition ffd, Object dtoValue) {
        return dtoValue;
    }

    /**
     * Extracts the plain field name from a prefixed binding path.
     * <p>
     * Spring MVC binds form fields using paths like "dto.fieldName" for JavaBean properties
     * or "dto[fieldName]" for Map entries. This method strips the "dto" prefix to obtain
     * the bare field name for lookup in the field mapping cache.
     * </p>
     *
     * @param nameWithDto the binding path including dto prefix (e.g., "dto.name" or "dto[name]")
     * @return the plain field name without prefix (e.g., "name")
     */
    public String extractFieldName(String nameWithDto) {
        if (isMapDto()) {
            return nameWithDto.replace("dto[", "").replace("]", "");
        } else {
            return nameWithDto.replace("dto.", "");
        }
    }

    /**
     * Validates a single form field and returns validation status.
     * <p>
     * Similar to {@link #validateField(FrontendMappingFieldDefinition, Function, BindingResult)}
     * but returns a boolean instead of populating BindingResult. Useful for programmatic
     * validation checks where Spring binding context is not available.
     * </p>
     *
     * @param ffd the field definition identifying which field to validate
     * @param fieldValidator function accepting field value and returning error code (blank if valid)
     * @return true if validation passed (error code blank), false otherwise
     */
    public boolean validateField(FrontendMappingFieldDefinition ffd, Function<Object, String> fieldValidator) {
        Object dtoValue = getField(ffd.getPlainName());
        dtoValue = convertDtoValue(ffd, dtoValue);
        String errorCode = fieldValidator.apply(dtoValue);
        return !StringUtils.isNotBlank(errorCode);

    }

    /**
     * Initializes the static dictionary repository singleton for all forms.
     * <p>
     * This method should be called once during application startup to provide access to
     * entity dictionaries for form dropdowns and multiselects. Subsequent calls overwrite
     * the existing repository reference.
     * </p>
     *
     * @param dictionaryRepository the repository providing privilege-aware entity lookups
     */
    public static void setSecureEntityDictionaryRepositoryOnce(SecureEntityDictionaryRepository dictionaryRepository) {
        AbstractForm.dictionaryRepository = dictionaryRepository;
    }

    /**
     * Marks a form definition as dirty to force field mapping cache rebuild.
     * <p>
     * When a form's field definitions change dynamically (e.g., adding or removing fields at runtime),
     * call this method with the form name to invalidate its cached accessor functions. The cache
     * will be regenerated during the next {@link #detectFieldMapping(Object)} invocation.
     * </p>
     *
     * @param frontendMappingDefinitionName the name of the form definition requiring cache rebuild
     */
    public static void markDirty(String frontendMappingDefinitionName) {
        AbstractForm.dirtyFrontendMapping.add(frontendMappingDefinitionName);
    }

    /**
     * Returns the dictionary repository for datalist suppliers.
     * <p>
     * Provides access to the static repository instance for retrieving entity lists
     * used in form select elements. Returns null if the repository has not been initialized
     * via {@link #setSecureEntityDictionaryRepositoryOnce(SecureEntityDictionaryRepository)}.
     * </p>
     *
     * @return the dictionary repository, or null if not yet initialized
     */
    public SecureEntityDictionaryRepository getDictionaryRepository() {
        return dictionaryRepository;
    }

    /**
     * Builds reflection-based accessor cache for DTO field retrieval.
     * <p>
     * This method inspects the DTO class using Apache Commons BeanUtils PropertyDescriptors
     * and Spring ReflectionUtils to create accessor Functions for each field defined in the
     * frontend mapping. The cache is stored in {@link #fieldMapping} and keyed by
     * formName + '#' + dtoClassName to enable reuse across form instances.
     * </p>
     * <p>
     * For Map-based DTOs (OrganizationRelatedMap), accessors use Map.get(). For JavaBean DTOs,
     * accessors first try property read methods, then fall back to direct field access for
     * fields without getters. The cache is rebuilt only if the form is marked dirty via
     * {@link #markDirty(String)} or if no cache exists yet.
     * </p>
     *
     * @param dto the DTO instance whose class structure determines field accessors (null skips detection)
     */
    protected void detectFieldMapping(D dto) {
        if(dto == null) { return; }
        Class c = dto.getClass();
        Map<String, Function> m = fieldMapping.get(frontendMappingDefinition.name + "#" + c.getCanonicalName());
        if(m != null && !AbstractForm.dirtyFrontendMapping.contains(frontendMappingDefinition.name)) { return; }

        //this should be done only once per form class
        m = new HashMap<>(frontendMappingDefinition.fields.length);
        fieldMapping.put(frontendMappingDefinition.name + "#" + c.getCanonicalName(), m);
        PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(c);
        Map<String, PropertyDescriptor> pdMap = new HashMap<>(pds.length);
        for (PropertyDescriptor pd: pds) {
            pdMap.put(pd.getName(), pd);
        }
        for (FrontendMappingFieldDefinition ffd : frontendMappingDefinition.fields) {
            String ffdName = ffd.getPlainName();
            if (ffdName == null) {
                continue;
            }
            if (dto instanceof OrganizationRelatedMap) {
                m.put(ffdName, a -> ((OrganizationRelatedMap) a).get(ffdName));
            } else {
                PropertyDescriptor pd = pdMap.get(ffdName);
                if (pd != null) {
                    Method readMethod = pd.getReadMethod();
                    m.put(ffdName,
                        a -> {
                            try {
                                return readMethod.invoke(a);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    );
                    continue;
                }
                Field f = ReflectionUtils.findField(c, ffdName);
                if (f != null) {
                    m.put(ffdName,
                            a -> ReflectionUtils.getField(f, a)
                    );
                    continue;
                }
                warn("No field reader for form {} field {}", frontendMappingDefinition.name, ffdName);
            }
        }
        AbstractForm.dirtyFrontendMapping.remove(frontendMappingDefinition.name);
    }

    /**
     * Returns the DTO instance backing this form.
     * <p>
     * Part of the DtoAndEntity interface contract. Provides access to the underlying
     * data transfer object for inspection or manual manipulation.
     * </p>
     *
     * @return the form's DTO instance, or null if not yet assigned
     */
    @Override
    public D getDto() {
        return dto;
    }

    /**
     * Assigns a DTO instance to this form and detects field mappings.
     * <p>
     * Replaces the current DTO and triggers field mapping detection via
     * {@link #detectFieldMapping(Object)}. If the DTO class differs from the previous one,
     * a new accessor cache entry is created.
     * </p>
     *
     * @param dto the new DTO instance to back this form
     */
    public void setDto(D dto) {
        this.dto = dto;
        detectFieldMapping(dto);
    }

    /**
     * Evaluates read/write privileges for all form fields.
     * <p>
     * This extension point is invoked during form processing to determine which fields
     * the current user can read or modify. Subclasses typically override this method
     * to implement more sophisticated privilege checks via prepareFieldsReadWritePrivileges().
     * The default implementation checks global field privileges via PrivilegeHelper.
     * </p>
     */
    @Override
    public void process() {
        for (FrontendMappingFieldDefinition f : frontendMappingDefinition.fields) {
            readWriteForField.put(f,
                    Tuples.of(
                            PrivilegeHelper.getInstance().canReadGlobalField(f),
                            PrivilegeHelper.getInstance().canWriteGlobalField(f)));
        }
    }

    /**
     * Returns the field name configured for single-field updates.
     *
     * @return the field name limiting updates, or null for full form updates
     */
    public String getSingleFieldToUpdate() {
        return singleFieldToUpdate;
    }

    /**
     * Restricts form submission to a single field for partial updates.
     * <p>
     * When set, only the specified field passes through {@link #getSafeValue} checks.
     * All other fields retain their entity values unchanged. Useful for AJAX updates
     * where only one field is modified by the user.
     * </p>
     *
     * @param singleFieldToUpdate the field name to update, or null to update all fields
     */
    public void setSingleFieldToUpdate(String singleFieldToUpdate) {
        this.singleFieldToUpdate = singleFieldToUpdate;
    }

    /**
     * Indicates whether the DTO is Map-based or JavaBean-based.
     * <p>
     * Returns false by default, indicating JavaBean-style property access (dto.fieldName).
     * Subclasses with Map-based DTOs (OrganizationRelatedMap) should override this to return true,
     * enabling array-style binding (dto[fieldName]).
     * </p>
     *
     * @return true if DTO is a Map, false if DTO is a JavaBean
     */
    public boolean isMapDto() {
        return false;
    }

    /**
     * Returns privilege-checked field value with identity transformation.
     * <p>
     * Convenience method delegating to {@link #getSafeValue(Object, String, Function)}
     * with identity function. Use when no type conversion is needed.
     * </p>
     *
     * @param <T> the field value type
     * @param entityValue the current entity property value (returned if update not permitted)
     * @param fieldName the form field name to check privileges for
     * @return the DTO value if user has write privilege, otherwise the unchanged entity value
     */
    final protected <T> T getSafeValue(T entityValue, String fieldName) {
        return getSafeValue(entityValue, fieldName, Function.identity());
    }

    /**
     * Returns privilege-checked field value with optional transformation function.
     * <p>
     * Enforces write privilege checks before populating entity fields from DTO values.
     * The method retrieves the DTO field value, checks if the current user has write permission,
     * and applies the transformation function only if permitted. If write is denied but the DTO
     * value is non-null, throws RuntimeException to prevent privilege escalation attacks.
     * </p>
     * <p>
     * Respects {@link #singleFieldToUpdate} - only the specified field is updated when set,
     * all others return the original entity value unchanged.
     * </p>
     *
     * @param <F> the DTO field value type (input to transformation function)
     * @param <T> the entity field value type (output from transformation function)
     * @param entityValue the current entity property value (returned if update not permitted)
     * @param fieldName the form field name to check privileges for
     * @param f transformation function applied to DTO value if write is permitted
     * @return the transformed DTO value if permitted, otherwise the unchanged entity value
     * @throws RuntimeException if user attempts to modify a field without write privilege
     */
    final protected <F, T> T getSafeValue(T entityValue, String fieldName, Function<F,T> f) {
        //if singleFieldToUpdate contains field name, do not update any other field
        if (singleFieldToUpdate != null && !fieldName.equals(singleFieldToUpdate)) return entityValue;
        FrontendMappingFieldDefinition field = frontendMappingDefinition.findField(fieldName);
        Boolean canWrite = readWriteForField.get(field).getT2();
        F dtoValue = (F)getField(fieldName);
        if (canWrite) return f.apply(dtoValue);
        //here user can't update the field
        if (dtoValue == null) return entityValue;
        //here user is hacker
        throw new RuntimeException(String.format("Can't write field %s", fieldName));
    }

    /**
     * Returns privilege-checked field value with explicit DTO value and transformation.
     * <p>
     * Variant of {@link #getSafeValue(Object, String, Function)} that accepts the DTO value
     * explicitly rather than retrieving it via field mapping. Useful when the value has already
     * been extracted or requires preprocessing before privilege checking.
     * </p>
     * <p>
     * Supports partial field name matching when {@link #singleFieldToUpdate} is set, allowing
     * updates to nested fields (e.g., singleFieldToUpdate="address" permits "address.city").
     * </p>
     *
     * @param <F> the DTO field value type (input to transformation function)
     * @param <T> the entity field value type (output from transformation function)
     * @param entityValue the current entity property value (returned if update not permitted)
     * @param fieldName the form field name to check privileges for
     * @param dtoValue the DTO property value to potentially apply
     * @param f transformation function applied to DTO value if write is permitted
     * @return the transformed DTO value if permitted, otherwise the unchanged entity value
     * @throws RuntimeException if user attempts to modify a field without write privilege
     */
    final protected <F, T> T getSafeValue(T entityValue, String fieldName, F dtoValue, Function<F,T> f) {
        //if singleFieldToUpdate contains field name, do not update any other field
        if (singleFieldToUpdate != null && !(fieldName.equals(singleFieldToUpdate) || fieldName.contains(singleFieldToUpdate))) return entityValue;
        FrontendMappingFieldDefinition field = frontendMappingDefinition.findField(fieldName);
        Boolean canWrite = readWriteForField.get(field).getT2();
        if (canWrite) return f.apply(dtoValue);
        //here user can't update the field
        if (dtoValue == null) return entityValue;
        //here user is hacker
        throw new RuntimeException(String.format("Can't write field %s", fieldName));
    }

    /**
     * Converter function returning null for empty strings, preserving non-empty values.
     * <p>
     * Use with {@link #getSafeValue} to normalize empty string submissions to null:
     * {@code entity.setName(getSafeValue(entity.getName(), "name", nullOnEmpty));}
     * </p>
     */
    protected Function <String, String> nullOnEmpty = ((String s) -> !s.isEmpty() ? s : null);

    /**
     * Converter function returning null for blank strings (whitespace-only or empty).
     * <p>
     * Uses Apache Commons StringUtils.defaultIfBlank() to treat blank strings as null.
     * </p>
     */
    protected Function <String, String> nullIfBlank = ((String s) -> StringUtils.defaultIfBlank(s, null));

    /**
     * Converter function returning empty string for null values, preserving non-null strings.
     * <p>
     * Ensures non-null string values for entity properties that cannot be null.
     * </p>
     */
    protected Function <String, String> emptyOnNull = ((String s) -> StringUtils.defaultString(s));

    /**
     * Converter function returning empty string for blank values (null, empty, or whitespace-only).
     * <p>
     * Normalizes all blank inputs to empty string for consistent storage.
     * </p>
     */
    protected Function <String, String> emptyIfBlank = ((String s) -> StringUtils.defaultIfBlank(s, ""));

    /**
     * Retrieves a field value from the DTO using cached accessor functions.
     * <p>
     * Looks up the accessor function from {@link #fieldMapping} using the form name and DTO class
     * as the cache key, then invokes the function on the current DTO instance. This method is
     * called internally by validation and privilege-checking methods.
     * </p>
     *
     * @param fieldName the plain field name (without dto prefix) to retrieve
     * @return the field value extracted from the DTO
     */
    private Object getField(String fieldName) {
        //FIXME: refactor name generation
        return fieldMapping.get(frontendMappingDefinition.name + "#" + dto.getClass().getCanonicalName()).get(fieldName).apply(dto);
    }
}
