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

import com.openkoda.controller.HtmlCRUDControllerConfigurationMap;
import com.openkoda.core.flow.LoggingComponent;
import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.core.security.OrganizationUser;
import com.openkoda.core.security.UserProvider;
import com.openkoda.model.common.SearchableOrganizationRelatedEntity;
import com.openkoda.model.component.FrontendResource;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.BindingResult;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.openkoda.model.Privilege.readOrgData;

/**
 * Reflection-based form using PropertyUtils for automatic bidirectional mapping between Map-based DTO
 * and JavaBean entity.
 * <p>
 * This form implementation provides reflection-based field mapping for converting between
 * {@link OrganizationRelatedMap} DTOs and {@link SearchableOrganizationRelatedEntity} persistent entities.
 * Uses Apache Commons PropertyUtils.describe() and PropertyUtils.setProperty() for automatic property
 * access without requiring manual getter/setter calls. Supports common type conversions via a static
 * converters map (BigDecimal, LocalDate/Time, Long/Integer/Boolean) and resolves custom converters
 * from field definitions.
 * <p>
 * Key features:
 * <ul>
 *   <li>Automatic property extraction via PropertyUtils.describe() during populateFrom()</li>
 *   <li>Type conversion support for common types (numbers, dates, booleans)</li>
 *   <li>Custom converter resolution from FrontendMappingFieldDefinition</li>
 *   <li>Privilege-aware field access with NO_ACCESS masking for unauthorized fields</li>
 *   <li>Support for computed fields via valueSupplier functions</li>
 *   <li>Partial updates via singleFieldToUpdate mechanism</li>
 *   <li>Static utility methods for table rendering with privilege checks</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>{@code
 * // Create form for existing entity
 * ReflectionBasedEntityForm form = new ReflectionBasedEntityForm(mapping, orgId, entity);
 * form.populateFrom(entity);
 * 
 * // Validate and populate entity
 * if (form.validate(bindingResult)) {
 *     form.populateTo(entity);
 *     repository.save(entity);
 * }
 * }</pre>
 * <p>
 * Thread Safety: This class is NOT thread-safe. Instances are request-scoped and intended for
 * single-threaded use only.
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @see AbstractOrganizationRelatedEntityForm
 * @see OrganizationRelatedMap
 * @see MapEntityForm
 * @see PropertyUtils
 * @since 1.7.1
 */
public class ReflectionBasedEntityForm extends AbstractOrganizationRelatedEntityForm<OrganizationRelatedMap, SearchableOrganizationRelatedEntity> {

    /**
     * Constant marking inaccessible field values in table views when user lacks read privilege.
     * Used by static utility methods to mask field values for unauthorized access.
     */
    private static final String NO_ACCESS = "[no access]";
    
    /**
     * Static map providing type conversion functions for common Java types.
     * <p>
     * Converters handle null and blank string values appropriately for each type:
     * <ul>
     *   <li>BigDecimal: parses numeric strings, null for blank</li>
     *   <li>LocalDateTime: parses ISO datetime strings, null for blank</li>
     *   <li>LocalDate: parses ISO date strings, null for blank</li>
     *   <li>Long/Long.class: parses long values, null for blank (boxed), 0L for blank (primitive)</li>
     *   <li>Integer/int.class: parses integer values, null for blank (boxed), 0 for blank (primitive)</li>
     *   <li>Boolean/boolean.class: parses boolean values, null for blank (boxed), false for blank (primitive)</li>
     * </ul>
     * 
     * <p>
     * Used as fallback converters when no custom converter is specified on field definition.
     * 
     */
    public static final Map<Class, Function> converters = new HashMap<>();

    static {
        converters.put(BigDecimal.class, a -> (a == null || StringUtils.isBlank(a+"")) ? null : new BigDecimal(a + ""));
        converters.put(LocalDateTime.class, a -> (a == null || StringUtils.isBlank(a+"")) ? null : LocalDateTime.parse(a + ""));
        converters.put(LocalDate.class, a -> (a == null || StringUtils.isBlank(a+"")) ? null : LocalDate.parse(a + ""));
        converters.put(Long.class, a -> (a == null || StringUtils.isBlank(a+"")) ? null : Long.valueOf(a+""));
        converters.put(long.class, a -> (a == null || StringUtils.isBlank(a+"")) ? 0L : Long.parseLong(a+""));
        converters.put(Integer.class, a -> (a == null || StringUtils.isBlank(a+"")) ? null : Integer.valueOf(a+""));
        converters.put(int.class, a -> (a == null || StringUtils.isBlank(a+"")) ? 0 : Integer.parseInt(a+""));
        converters.put(Boolean.class, a -> (a == null || StringUtils.isBlank(a+"")) ? null : Boolean.valueOf(a+""));
        converters.put(boolean.class, a -> (a == null || StringUtils.isBlank(a+"")) ? false : Boolean.parseBoolean(a+""));
    }

    /**
     * Creates a new form instance for a new entity without pre-populating from existing entity.
     * <p>
     * This constructor is used when creating forms for new entities that haven't been persisted yet.
     * The DTO will be empty and must be populated from HTTP request parameters via Spring MVC binding.
     * 
     *
     * @param frontendMappingDefinition the form field definitions and validation rules
     */
    public ReflectionBasedEntityForm(FrontendMappingDefinition frontendMappingDefinition) {
        super(frontendMappingDefinition);
    }

    /**
     * Creates a form instance pre-populated from an existing entity for editing operations.
     * <p>
     * This constructor initializes the form with data from the provided entity and evaluates
     * field privileges for the specified organization. Automatically calls populateFrom() to
     * extract entity properties into the DTO map, and initializes computed fields via
     * populateSuppliedValuesFrom().
     * 
     *
     * @param frontendMappingDefinition the form field definitions and validation rules
     * @param organizationId the organization context for privilege evaluation (null for global entities)
     * @param entity the existing entity to populate the form from
     */
    public ReflectionBasedEntityForm(FrontendMappingDefinition frontendMappingDefinition, Long organizationId, SearchableOrganizationRelatedEntity entity) {
        super(organizationId, new OrganizationRelatedMap(), entity, frontendMappingDefinition);
        populateSuppliedValuesFrom();
    }

    /**
     * Initializes DTO fields that have value suppliers (computed or display-only fields).
     * <p>
     * Iterates through all field definitions and populates DTO values for fields with
     * configured valueSupplier functions. These are typically computed fields that derive
     * their values from other form fields or entity state rather than being directly editable.
     * Called automatically by the constructor after entity population.
     * 
     */
    private void populateSuppliedValuesFrom() {
        for (FrontendMappingFieldDefinition f : frontendMappingDefinition.getFields()) {
            if (f.valueSupplier != null) {
                dto.put(f.getPlainName(), f.valueSupplier.apply(this));
            }
        }
    }

    /**
     * Validation stub method - returns null as validation is handled by parent class.
     * <p>
     * This form relies on AbstractForm's validation infrastructure via validateField() and
     * field validators configured in FrontendMappingDefinition. This method exists to satisfy
     * the Form interface contract but delegates all actual validation to the parent class.
     * 
     *
     * @param br the Spring MVC BindingResult for collecting validation errors (unused)
     * @return null (validation handled by parent class)
     * @param <F> the form type parameter
     */
    @Override
    public <F extends Form> F validate(BindingResult br) {
        return null;
    }

    /**
     * Populates the form DTO from entity properties using reflection.
     * <p>
     * Uses PropertyUtils.describe() to extract all readable properties from the entity as a Map,
     * then copies these values into the DTO. For each field in the form definition:
     * <ul>
     *   <li>If the field has a valueSupplier, the supplied value is computed and used</li>
     *   <li>If the field has an entityToDtoValueConverter, the converter is applied to transform the entity value</li>
     *   <li>Otherwise, the raw entity property value is copied directly to the DTO</li>
     * </ul>
     * 
     * <p>
     * This method enables automatic form population without manual getter calls for each field.
     * 
     *
     * @param entity the entity to extract properties from
     * @return this form instance for method chaining
     * @throws RuntimeException wrapping IllegalAccessException, InvocationTargetException, or NoSuchMethodException
     *         if entity introspection fails
     */
    @Override
    protected ReflectionBasedEntityForm populateFrom(SearchableOrganizationRelatedEntity entity) {
        try {
            Map<String, Object> propertiesMap = PropertyUtils.describe(entity);
            dto.putAll(propertiesMap);
            for (FrontendMappingFieldDefinition f : frontendMappingDefinition.getFields()) {
                if (f.valueSupplier != null) {
                    dto.put(f.getPlainName(), f.valueSupplier.apply(this));
                } else {
                    Object entityValue = propertiesMap.get(f.getPlainName());
                    if (f.entityToDtoValueConverter != null) {
                        dto.put(f.getPlainName(), f.entityToDtoValueConverter.apply(entityValue));
                    } else {
                        dto.put(f.getPlainName(), entityValue);
                    }
                }
            }
            return this;
        } catch (IllegalAccessException |InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(String.format("Can't describe entity %s", entity), e);
        }
    }

    /**
     * Populates entity fields from the form DTO with privilege enforcement.
     * <p>
     * Supports both full-form updates and partial single-field updates:
     * <ul>
     *   <li>If singleFieldToUpdate is set, only that field is updated</li>
     *   <li>Otherwise, all fields in the form definition are processed</li>
     * </ul>
     * For each field, setEntityValue() is called to apply type conversion and privilege checks.
     * 
     *
     * @param entity the entity to update with DTO values
     * @return the updated entity instance
     */
    @Override
    protected SearchableOrganizationRelatedEntity populateTo(SearchableOrganizationRelatedEntity entity) {
       if (singleFieldToUpdate != null) {
           FrontendMappingFieldDefinition f = frontendMappingDefinition.findField(singleFieldToUpdate);
           setEntityValue(entity, f);
       } else {
           for (FrontendMappingFieldDefinition f : frontendMappingDefinition.getFields()) {
               setEntityValue(entity, f);
           }
       }
       return entity;
    }

    /**
     * Sets a single entity field value from the DTO with type conversion and privilege enforcement.
     * <p>
     * Performs the following steps:
     * <ol>
     *   <li>Checks if the field type has a database value (skips UI-only fields)</li>
     *   <li>Resolves the appropriate converter for the field type</li>
     *   <li>Reads the current entity value via BeanUtils.getProperty()</li>
     *   <li>Applies getSafeValue() to enforce write privileges and convert the DTO value</li>
     *   <li>Sets the converted value on the entity via PropertyUtils.setProperty()</li>
     * </ol>
     * 
     *
     * @param entity the entity to update
     * @param f the field definition with type, name, and privilege information
     * @throws RuntimeException wrapping IllegalAccessException, InvocationTargetException, or NoSuchMethodException
     *         if property access fails
     */
    private void setEntityValue(SearchableOrganizationRelatedEntity entity, FrontendMappingFieldDefinition f)  {
        try {
            if (not(f.getFieldType(this).hasValue())) {
                return;
            }
            Function converter = getConverter(entity, f);
            PropertyUtils.setProperty(entity, f.getPlainName(), getSafeValue(BeanUtils.getProperty(entity, f.getPlainName()), f.getPlainName(), converter));
//        BeanUtils.setProperty(entity, fieldName, getSafeValue(BeanUtils.getProperty(entity, fieldName), fieldName, converter));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(String.format("Can't write field %s", f.getPlainName()), e);
        }
    }

    /**
     * Converts a DTO value to entity type before validation using the field's converter.
     * <p>
     * This override ensures DTO values are properly converted before field validators are applied.
     * Returns the raw DTO value if entity is null (new entity case), otherwise applies the
     * resolved converter function.
     * 
     *
     * @param ffd the field definition containing converter information
     * @param dtoValue the raw value from the DTO map
     * @return the converted value ready for validation
     */
    @Override
    protected Object convertDtoValue(FrontendMappingFieldDefinition ffd, Object dtoValue) {
        return entity == null ? dtoValue : getConverter(entity, ffd).apply(dtoValue);
    }

    /**
     * Resolves the appropriate type converter function for a form field.
     * <p>
     * Converter resolution follows this priority order:
     * <ol>
     *   <li>Custom dtoToEntityValueConverter from field definition (if configured)</li>
     *   <li>Enum valueOf converter for enum fields</li>
     *   <li>List&lt;Long&gt; parser for collections (handles String and String[] inputs)</li>
     *   <li>String.join(",") for multiselect String fields (converts String[] to comma-separated)</li>
     *   <li>Static converters map lookup by field type (BigDecimal, dates, numbers, booleans)</li>
     *   <li>Function.identity() as final fallback (no conversion)</li>
     * </ol>
     * 
     * <p>
     * Uses reflection to inspect the target entity field type via ReflectionUtils.findField().
     * 
     *
     * @param entity the target entity for field type inspection
     * @param f the field definition with converter configuration
     * @return a Function that converts Object (DTO value) to the appropriate entity field type
     */
    private Function getConverter(SearchableOrganizationRelatedEntity entity, FrontendMappingFieldDefinition f) {
        Function converter = f.dtoToEntityValueConverter;
        if (converter == null) {
            Field field = ReflectionUtils.findField(entity.getClass(), f.getPlainName());
            if (field.getType().isEnum()) {
                converter = a -> (a == null || StringUtils.isBlank(a + "")) ? null : Enum.valueOf(((Class<? extends Enum>) field.getType()), (String) a);
            } else if ("java.util.List<java.lang.Long>".equals(field.getGenericType().getTypeName())) {
                converter = a -> {
                    if (a == null) { return null; }
                    if (String.class.equals(a.getClass())) {
                        return new ArrayList<Long>(List.of(Long.parseLong((String)a)));
                    }
                    if (String[].class.equals(a.getClass())) {
                        List<Long> collect = Arrays.stream((String[]) a)
                                .map(Long::parseLong)
                                .collect(Collectors.toList());
                        return collect;
                    }
                    return a;
                };
            } else if ("java.lang.String".equals(field.getGenericType().getTypeName())) {
                converter = a -> {
//                for multiselect which sends data as String[]
                    if (a instanceof String []) { return String.join(",", (String[]) a); }
                    return a;
                };
            } else {
                converter = converters.getOrDefault(field.getType(), Function.identity());
            }
        }
        return converter;
    }

    /**
     * Indicates this form uses Map-based DTO binding style (dto[field] instead of dto.field).
     * <p>
     * Returns true because ReflectionBasedEntityForm uses OrganizationRelatedMap (extends HashMap)
     * as its DTO. This affects HTML form input naming conventions - fields should be rendered as
     * {@code <input name="dto[fieldName]">} for proper Spring MVC binding via RenamingProcessor.
     * 
     *
     * @return true to indicate Map-based DTO binding
     */
    @Override
    public boolean isMapDto() {
        return true;
    }

    /**
     * Prepares table column headers with privilege-based visibility filtering.
     * <p>
     * Filters the provided field names based on:
     * <ul>
     *   <li>Field-level read privileges (canReadGlobalOrOrgField check)</li>
     *   <li>Explicit field column visibility settings</li>
     *   <li>Field existence in the form definition</li>
     * </ul>
     * Creates placeholder text field definitions for field names not found in the form definition.
     * Used by table rendering utilities to build column headers that respect user privileges.
     * 
     *
     * @param fd the form definition containing field metadata
     * @param fieldNames array of field names to include as columns
     * @param fieldColumnVisibility map tracking which fields are visible (updated by this method)
     * @param organizationId the organization context for privilege evaluation (null for global)
     * @return list of field definitions for visible columns
     */
    public static List<FrontendMappingFieldDefinition> getFieldsHeaders(FrontendMappingDefinition fd, String[] fieldNames, Map<String, Boolean> fieldColumnVisibility, Long organizationId) {
        if (fieldNames == null) {return Collections.emptyList();}
        List<FrontendMappingFieldDefinition> result = new ArrayList<>(fieldNames.length);
        for (int k = 0; k < fieldNames.length; k++) {
            if(organizationId != null && Boolean.FALSE.equals(fieldColumnVisibility.get(fieldNames[k]))) {
                continue;
            }

            FrontendMappingFieldDefinition field = fd.findField(fieldNames[k]);
            if(field == null) {
                field = FrontendMappingFieldDefinition.createFormFieldDefinition(fd.name, fieldNames[k], FieldType.text);
            }
            
            boolean canRead = organizationId == null || field.readPrivilege == null || PrivilegeHelper.getInstance().canReadGlobalOrOrgField(field, organizationId);
            if(canRead || fieldColumnVisibility.get(fieldNames[k])) {
                result.add(field);
            }
        }
        return result;
    }

    /**
     * Extracts entity data as table rows with privilege-based field access control.
     * <p>
     * For each entity in the provided list, extracts field values according to the specified field names,
     * applying per-field read privilege checks. Fields the user cannot read are masked with NO_ACCESS.
     * Also determines column-level visibility: columns where all values are NO_ACCESS are flagged for
     * hiding in fieldColumnVisibility map and removed from result rows.
     * 
     * <p>
     * Resolves datalist values for dropdown/multiselect fields by looking up values in preloaded
     * dictionaries (from field datalistSupplier).
     * 
     *
     * @param fd the form definition containing field metadata and datalist suppliers
     * @param entities the list of entities to extract data from
     * @param fieldNames array of field names to include as columns
     * @param fieldColumnVisibility map tracking which fields have at least one readable value (updated)
     * @param organizationId the organization context for privilege evaluation (null for global/superuser)
     * @return list of lists where each inner list represents a table row with field values
     */
    public static List<List<Object>> calculateFieldsValuesWithReadPrivileges(FrontendMappingDefinition fd, List<? extends SearchableOrganizationRelatedEntity> entities, String[] fieldNames
            , Map<String, Boolean> fieldColumnVisibility, Long organizationId) {
        List<List<Object>> result = new ArrayList<>(entities.size());
        Map<String, Map> dictionaries = new HashMap<>();
        for (FrontendMappingFieldDefinition field : fd.fields) {
            if (field.datalistSupplier != null && !field.formBasedDatalistSupplier) {
                dictionaries.put(field.datalistId, (Map) field.datalistSupplier.apply(null, dictionaryRepository));
            }
        }

        OrganizationUser user = UserProvider.getFromContext().get();
        for(String fieldName : fieldNames) {
            fieldColumnVisibility.put(fieldName, ((organizationId == null && user.isSuperUser()) || entities.size() == 0) ? Boolean.TRUE : Boolean.FALSE);
        }

        for (SearchableOrganizationRelatedEntity se: entities) {
            List<Object> accessibleFields = calculateFieldValuesWithReadPrivileges(fd, se, fieldNames, dictionaries, fieldColumnVisibility, (organizationId == null && user.isSuperUser()));
            if(accessibleFields != null && accessibleFields.size() > 0) {
                result.add(accessibleFields);
            }
        }

        // hide columns with all denied/hidden fields
        if(organizationId != null) {
            for (int k = fieldNames.length - 1; k >= 0; k--) {
                if(!fieldColumnVisibility.get(fieldNames[k])) {
                    for (List<Object> row : result) {
                        if(row.size() >= k + 1) {
                            row.remove(k);
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Extracts field values from a single entity with privilege-based access control.
     * <p>
     * For each requested field name:
     * <ol>
     *   <li>Resolves the field definition from the form definition</li>
     *   <li>Checks read privilege using PrivilegeHelper.canReadField() or superuser override</li>
     *   <li>Extracts the property value via PropertyUtils.getProperty() if readable</li>
     *   <li>Replaces value with NO_ACCESS constant if privilege check fails</li>
     *   <li>Resolves datalist display values for dropdown/foreign key fields</li>
     *   <li>Updates fieldColumnVisibility map to track if any row has readable value</li>
     * </ol>
     * 
     * <p>
     * Supports nested property access (e.g., "role.name") for many-to-one relationships.
     * Creates default field definitions with readOrgData privilege for fields not in form definition
     * (typically audit fields like createdOn, updatedOn).
     * 
     *
     * @param fd the form definition containing field metadata
     * @param entity the entity to extract field values from
     * @param fieldNames array of field names to extract
     * @param dictionaries preloaded datalist maps for dropdown value resolution
     * @param fieldColumnVisibility map tracking which fields are readable (updated)
     * @param canReadAll true if user is superuser and can bypass privilege checks
     * @return list of field values (or NO_ACCESS for unauthorized fields)
     */
    public static List<Object> calculateFieldValuesWithReadPrivileges(FrontendMappingDefinition fd, SearchableOrganizationRelatedEntity entity, String[] fieldNames, Map<String, Map> dictionaries,
            Map<String, Boolean> fieldColumnVisibility, boolean canReadAll) {
        if (fieldNames == null) {
            return Collections.emptyList();
        }

        List<Object> result = new ArrayList<>(fieldNames.length);
        try {
            int i = 0;
            for (int k = 0; k < fieldNames.length; k++) {
                try {
                    boolean isReferenceFieldProperty = fieldNames[k].contains(".");
                    String referencedEntityKey = isReferenceFieldProperty ? StringUtils.substringBefore(fieldNames[k], ".") : fieldNames[k];
                    FrontendMappingFieldDefinition f = isReferenceFieldProperty ?
                            Arrays.stream(fd.fields)
                                    .filter(def -> def.referencedEntityKey != null && def.referencedEntityKey.equals(referencedEntityKey)).findFirst().orElse(null)
                            : fd.findField(fieldNames[k]);
                    if(f == null) {
    //                    allow display of data which have no column representation for users with readOrgData privilege,
//                    most likely these are columns like createdOn, updatedOn, organizationId, etc.
//                    all entity specific columns should have their field representation with access limitation
                        f = FrontendMappingFieldDefinition.createFormFieldDefinition(fd.name, fieldNames[k], FieldType.text, readOrgData, readOrgData);
                    }

                    boolean canRead = f.readPrivilege == null || canReadAll || PrivilegeHelper.getInstance().canReadField(f, entity);
                    fieldColumnVisibility.put(fieldNames[k], fieldColumnVisibility.get(fieldNames[k]) || canRead);

                    if(isReferenceFieldProperty) {
                        result.add(canRead && PropertyUtils.getProperty(entity, referencedEntityKey) != null ? PropertyUtils.getProperty(entity, fieldNames[k]) : NO_ACCESS);
                    } else {
                        result.add(canRead ? PropertyUtils.getProperty(entity, fieldNames[k]) : NO_ACCESS);
                    }
                    if (!isReferenceFieldProperty && f.datalistId != null && dictionaries.containsKey(f.datalistId)) {
                        result.set(i, canRead ? dictionaries.get(f.datalistId).get(result.get(i)) : NO_ACCESS);
                    }

                } catch (NoSuchMethodException exc) {
                    LoggingComponent.debugLogger.warn("Could not read entity property", exc);
                    result.add("");
                }

                i++;
            }
        } catch (Exception e) {
            LoggingComponent.debugLogger.warn("Could not read entity property", e);
        }
        return result;
    }

    /**
     * Extracts field values from a page of entities as a list of maps with privilege checks.
     * <p>
     * Convenience method that iterates over a Spring Data Page of entities and extracts each
     * entity's field values into a Map using calculateFieldValuesWithReadPrivilegesAsMap().
     * Used for JSON API responses where map structure is preferred over list structure.
     * 
     *
     * @param fd the form definition containing field metadata
     * @param entities the page of entities to extract data from
     * @param fieldNames array of field names to include
     * @return list of maps where each map represents one entity with field name → value mappings
     */
    public static List<Map<String, Object>> calculateFieldsValuesWithReadPrivilegesAsMap(FrontendMappingDefinition fd, Page<SearchableOrganizationRelatedEntity> entities, String[] fieldNames) {
        List<Map<String, Object>> result = new ArrayList<>(entities.getNumberOfElements());
        for (SearchableOrganizationRelatedEntity se: entities) {
            result.add(calculateFieldValuesWithReadPrivilegesAsMap(fd, se, fieldNames));
        }
        return result;
    }

    /**
     * Extracts field values from a single entity into a map with privilege-based access control.
     * <p>
     * For each requested field name, checks read privilege and extracts the property value via
     * PropertyUtils.getProperty(). Fields the user cannot read are set to empty string instead of
     * NO_ACCESS (suitable for API responses where empty is preferred over error markers).
     * 
     *
     * @param fd the form definition containing field metadata
     * @param entity the entity to extract field values from
     * @param fieldNames array of field names to include
     * @return map of field name → field value (or empty string for unauthorized fields)
     */
    public static Map<String, Object> calculateFieldValuesWithReadPrivilegesAsMap(FrontendMappingDefinition fd, SearchableOrganizationRelatedEntity entity, String[] fieldNames) {
        HashMap<String, Object> result = new HashMap<>();
        if (fieldNames == null) {return result;}
        try {
            for (String fn :  fieldNames) {
                FrontendMappingFieldDefinition f = fd.findField(fn);
                boolean canRead = PrivilegeHelper.getInstance().canReadField(f, entity);
                result.put(fn, canRead ? PropertyUtils.getProperty(entity, fn) : "");
            }
        } catch (Exception e) {
            LoggingComponent.debugLogger.warn("Could not read entity property", e);
        }
        return result;
    }

    /**
     * Prepares the DTO for partial form updates by merging request parameters with entity state.
     * <p>
     * Performs three steps:
     * <ol>
     *   <li>Nulls non-writeable DTO fields based on user privileges (prevents unauthorized updates)</li>
     *   <li>Casts all DTO values to String for validator compatibility</li>
     *   <li>Merges request parameters into DTO (only for fields in form definition)</li>
     * </ol>
     * Used for AJAX partial form updates where only specific fields are being modified.
     * 
     *
     * @param params map of request parameters from HTTP request
     * @param entity the existing entity for privilege evaluation
     * @return true (always succeeds)
     */
    public boolean prepareDto(Map<String,String> params, SearchableOrganizationRelatedEntity entity) {
        nullNonWriteableDtoFields(entity);
        castToString();
        setParamsToDto(params);
        return true;
    }

    /**
     * Resolves filter field definitions for table filtering UI.
     * <p>
     * For each field name, retrieves the field definition. For dropdown and many_to_one fields,
     * also includes the referenced datalist field definition (the dictionary field that provides
     * the dropdown options). This enables proper filter rendering with both the filter field and
     * its associated datalist.
     * 
     *
     * @param fd the form definition containing field metadata
     * @param fieldNames array of field names to use as filters
     * @return list of field definitions for filter UI rendering
     */
    public static List<FrontendMappingFieldDefinition> getFilterFields(FrontendMappingDefinition fd, String[] fieldNames) {
        if (fieldNames == null) {
            return Collections.emptyList();
        }
        List<FrontendMappingFieldDefinition> result = new ArrayList<>(fieldNames.length);
        for (int k = 0; k < fieldNames.length; k++) {
            FrontendMappingFieldDefinition field = fd.findField(fieldNames[k]);
            if(field.getType().equals(FieldType.dropdown) || field.getType().equals(FieldType.many_to_one)) {
                FrontendMappingFieldDefinition dictionaryField = fd.findField(field.datalistId);
                if(dictionaryField != null) {
                    result.add(dictionaryField);
                }
            }
            result.add(field);
        }
        return result;
    }

    /**
     * Returns field names suitable for use as table filters.
     * <p>
     * Retrieves field names for field types that support filtering: text, checkbox, dropdown,
     * number, date, datetime, and many_to_one. These types have meaningful filter operations
     * (equals, contains, range, etc.) unlike file uploads or code editors.
     * 
     *
     * @param fd the form definition to extract filterable fields from
     * @return collection of field names suitable for filtering
     */
    public static Collection<Object> getFilterFieldsNames(FrontendMappingDefinition fd) {
        return fd.getFieldsNamesByType(List.of(FieldType.text, FieldType.checkbox, FieldType.dropdown, FieldType.number, FieldType.date, FieldType.datetime, FieldType.many_to_one));
    }

    /**
     * Returns field names for table column display including many-to-one relationship properties.
     * <p>
     * Builds column names by:
     * <ul>
     *   <li>Finding all many_to_one fields in the form definition</li>
     *   <li>For each many_to_one field, looking up its referenced entity's form definition via
     *       HtmlCRUDControllerConfigurationMap</li>
     *   <li>Adding nested property names (e.g., "role.name") for each valued field in the
     *       referenced entity</li>
     *   <li>Including all direct valued fields from the main form definition</li>
     * </ul>
     * This enables table columns to show related entity properties (foreign key attributes)
     * alongside the main entity's properties.
     * 
     *
     * @param fd the form definition to extract table columns from
     * @return collection of field names (including nested properties) for table columns
     */
    public static Collection<Object> getTableColumnsNames(FrontendMappingDefinition fd) {
        List<Object> result = new ArrayList<>();
        List<FrontendMappingFieldDefinition> manyToOneFields = fd.getFieldsByType(List.of(FieldType.many_to_one));
        for(FrontendMappingFieldDefinition field : manyToOneFields) {
            CRUDControllerConfiguration controllerConfig = HtmlCRUDControllerConfigurationMap.getControllers().get(field.referencedEntityKey);
            if(controllerConfig != null) {
                result.addAll(Arrays.stream(controllerConfig.getFrontendMappingDefinition().getNamesOfValuedTypeFields())
                        .map(atr -> field.getName().replace("Id", "") + "." + atr).toList());
            }
        }
        result.addAll(Arrays.asList(fd.getNamesOfValuedTypeFields()));
        return result;
    }

    /**
     * Parses filter request parameters into typed tuples for JPA specification building.
     * <p>
     * Converts filter parameter entries (from HTTP request) into Tuple3 objects containing:
     * <ul>
     *   <li>T1: Full parameter key (e.g., "name_eq", "age_gt")</li>
     *   <li>T2: Field definition resolved from field name (before underscore)</li>
     *   <li>T3: Filter value (the search term)</li>
     * </ul>
     * Used by specification builders to construct type-aware JPA criteria queries.
     * 
     *
     * @param fd the form definition for field lookup
     * @param filters map of filter parameter name → filter value
     * @return list of tuples with parameter key, field definition, and filter value
     */
    public static List<Tuple3<String, FrontendMappingFieldDefinition, String>> getFilterTypesAndValues(FrontendMappingDefinition fd, Map<String,String> filters) {
        return filters.entrySet().stream()
                .map(entry -> Tuples.of(entry.getKey(), fd.findField(StringUtils.substringBefore(entry.getKey(), "_")), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Nulls DTO fields for which the user has no write permission.
     * <p>
     * Evaluates write privileges for each DTO field and sets non-writeable fields to null.
     * This prevents unauthorized field updates - when getSafeValue() is called during
     * setEntityValue(), it will throw an exception if attempting to update a field the user
     * cannot write. By nulling these fields preemptively, we avoid the exception and ensure
     * only authorized fields are updated.
     * 
     *
     * @param entity the entity for privilege evaluation
     */
    private void nullNonWriteableDtoFields(SearchableOrganizationRelatedEntity entity){
        prepareFieldsReadWritePrivileges(entity);
        dto.forEach((fieldName, fieldValue) -> {
            FrontendMappingFieldDefinition field = frontendMappingDefinition.findField(fieldName);
            if(field != null && !readWriteForField.get(field).getT2()){
                dto.put(fieldName, null);
            }
        });
    }

    /**
     * Merges request parameters into the DTO map.
     * <p>
     * For each request parameter, checks if it corresponds to a field in the form definition.
     * Only parameters with matching field definitions are copied to the DTO. Fields with
     * valueSupplier are skipped (these are computed fields that shouldn't be updated directly).
     * 
     * <p>
     * Note: This method does NOT enforce write privileges - that is done by nullNonWriteableDtoFields()
     * before this method is called, and by getSafeValue() during populateTo().
     * 
     *
     * @param params map of request parameters from HTTP request
     */
    private void setParamsToDto(Map<String,String> params){
        if (params != null && !params.isEmpty()) {
            params.forEach((fieldName, fieldValue) -> {
                FrontendMappingFieldDefinition field = frontendMappingDefinition.findField(fieldName);
                //if valueSupplier is not null it means a field's value is calculated form others fields so shouldn't be updated directly
                if (field != null && field.valueSupplier == null) {
                    dto.put(fieldName, fieldValue);
                }
            });
        }
    }

    /**
     * Converts all DTO values to String representation for validator compatibility.
     * <p>
     * Field validators in FormFieldDefinitionBuilder expect String inputs and may fail with
     * ClassCastException if DTO values are non-String objects (e.g., enums, numbers). This method
     * explicitly calls toString() on all DTO values to ensure validators receive String inputs.
     * Null values are preserved as null.
     * 
     * <p>
     * Example: FrontendResource.Type enum values are converted to their string names before
     * validation.
     * 
     *
     * @see FormFieldDefinitionBuilder#validate(Function)
     * @see FrontendResource.Type
     */
    private void castToString(){
        dto.forEach((fieldName, fieldValue) -> {
            dto.put(fieldName, fieldValue != null ? fieldValue.toString() : null);
        });
    }
}