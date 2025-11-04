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

package com.openkoda.repository;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.flow.Tuple;
import com.openkoda.core.form.AbstractForm;
import com.openkoda.core.helper.JsonHelper;
import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.core.multitenancy.TenantResolver;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.core.tracker.DebugLogsDecoratorWithRequestId;
import com.openkoda.dto.file.FileDto;
import com.openkoda.form.rule.LogicalOperator;
import com.openkoda.form.rule.Operator;
import com.openkoda.model.OptionWithLabel;
import com.openkoda.model.PrivilegeGroup;
import com.openkoda.model.common.ModelConstants;
import com.openkoda.model.common.SearchableEntity;
import com.openkoda.model.common.SearchableOrganizationRelatedEntity;
import com.openkoda.model.common.SearchableRepositoryMetadata;
import com.openkoda.model.component.ControllerEndpoint;
import com.openkoda.model.component.FrontendResource;
import com.openkoda.model.file.EntityWithFiles;
import com.openkoda.model.file.File;
import com.openkoda.service.user.RoleService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.openkoda.core.form.FormFieldDefinitionBuilderStart.DATALIST_PREFIX;
import static com.openkoda.model.file.File.toFileDto;

/**
 * Repository component assembling localized JSON datalist dictionaries for UI dropdowns and autocomplete.
 * <p>
 * This Spring-managed repository queries {@link SearchableRepositories} and other services to build
 * entity dictionaries for use in frontend form controls. It produces {@link Tuple} and {@link LinkedHashMap}
 * results suitable for flexible JSON serialization, enabling dynamic population of select dropdowns,
 * autocomplete fields, and datalist elements across the OpenKoda UI.

 * <p>
 * Key responsibilities include:
 * <ul>
 *   <li>Querying JPA entities via {@link EntityManager} to generate dictionaries with custom description formulas</li>
 *   <li>Caching dictionary data atomically for performance via {@code commonDictionaries} map</li>
 *   <li>Exposing {@link #getCommonDictionaries()} for frequently-used entity lists (roles, privileges, languages, etc.)</li>
 *   <li>Applying organization-scoped filtering via {@link TenantResolver} for multi-tenant data isolation</li>
 *   <li>Supporting localization via {@link Locale}-specific formatting for countries and languages</li>
 *   <li>Using {@link PostConstruct} initialization to preload static dictionaries at application startup</li>
 * </ul>

 * <p>
 * The repository integrates with the OpenKoda security framework via {@link HasSecurityRules}, ensuring
 * dictionary queries respect user privileges and organization scope. Dictionary results are serialized
 * to JSON format using {@code JSONObject} and {@code JSONArray} for frontend consumption.

 * <p>
 * Thread-safety: Dictionary assembly methods are not synchronized; {@link #setupCommonDictionaries()}
 * performs atomic replacement of the {@code commonDictionaries} map to ensure consistency during updates.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SearchableRepositories
 * @see TenantResolver
 * @see EntityManager
 * @see HasSecurityRules
 */
@Repository("secureEntityDictionaryRepository")
public class SecureEntityDictionaryRepository extends ComponentProvider implements HasSecurityRules, ModelConstants {

    /**
     * Configured role types for dictionary population.
     * Defaults to organization, global, and global-organization role types from {@link RoleService}.
     */
    @Value("#{'${role.types:" +
            RoleService.ROLE_TYPE_ORG + "," +
            RoleService.ROLE_TYPE_GLOBAL + "," +
            RoleService.ROLE_TYPE_GLOBAL_ORG + "}'.split(',')}")
    private List<String> roleTypes;

    /**
     * Configured language options for localization support.
     * Defaults to English (en) if not specified in application properties.
     */
    @Value("#{'${language.options:en}'.split(',')}")
    private List<String> languagesList;

    /**
     * JPA EntityManager for executing custom JPQL and native SQL queries to build dictionaries.
     */
    @PersistenceContext
    private EntityManager em;

    /**
     * Singleton instance for static access to dictionary methods.
     */
    private static SecureEntityDictionaryRepository instance;

    /**
     * Cached map of common dictionaries (privileges, roles, languages, etc.) for performance.
     * Atomically replaced during {@link #setupCommonDictionaries()} to ensure consistency.
     */
    protected Map<String, Object> commonDictionaries = new HashMap<>();
    
    /**
     * Map of language codes to localized display names.
     */
    private Map<String, String> languages = new LinkedHashMap<>();
    
    /**
     * Static map of ISO country codes to English country names, sorted alphabetically.
     * Initialized in static block using {@link Locale} API.
     */
    public static HashMap<String, String> countries;
    
    /**
     * Module-specific dictionaries registered at runtime via {@link #addModuleDictionary(Map, String)}.
     */
    private static Map<String, Map<Object, String>> moduleDictionaries = new HashMap<>();

    /**
     * Static initializer populating the {@link #countries} map with ISO country codes and English names.
     * Countries are sorted alphabetically by display name in English locale.
     */
    static {
        LinkedHashMap<String, String> tempMap = new LinkedHashMap<>();

        for (String iso : Locale.getISOCountries()) {
            Locale l = new Locale("", iso);
            tempMap.put(iso, l.getDisplayCountry(Locale.ENGLISH));
        }

        // sort countries by name
        countries = tempMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    /**
     * Retrieves the singleton instance of this repository.
     * Instance is initialized during Spring context startup via {@link #init()}.
     *
     * @return the singleton {@code SecureEntityDictionaryRepository} instance
     */
    public static SecureEntityDictionaryRepository getInstance() {
        return instance;
    }

    /**
     * Builds a dictionary for the specified entity key with default ID-based description.
     *
     * @param entityKey the searchable entity key registered in {@link SearchableRepositories}
     * @return map of entity IDs to string descriptions, filtered by user privileges and organization scope
     */
    public Map dictionary(String entityKey) {
        SearchableRepositoryMetadata gsa = SearchableRepositories.getSearchableRepositoryMetadata(entityKey);
        return dictionary(gsa);//, ID, null, null);
    }
    
    /**
     * Builds a dictionary for the specified entity key with a custom SQL description formula.
     *
     * @param entityKey the searchable entity key registered in {@link SearchableRepositories}
     * @param customDescriptionFormula SQL expression for generating entity descriptions (e.g., "name || ' (' || code || ')'")
     * @return map of entity IDs to formatted descriptions, filtered by security rules
     */
    public Map dictionary(String entityKey, String customDescriptionFormula) {
        SearchableRepositoryMetadata gsa = SearchableRepositories.getSearchableRepositoryMetadata(entityKey);
        return dictionary(gsa, customDescriptionFormula);//, ID, null, null);
    }
    
    /**
     * Builds a dictionary for the specified entity class with default ID-based description.
     *
     * @param entityClass the JPA entity class extending {@link SearchableEntity}
     * @return map of entity IDs to string descriptions, respecting privilege enforcement
     */
    public Map dictionary(Class entityClass) {
        SearchableRepositoryMetadata gsa = SearchableRepositories.getSearchableRepositoryMetadata(entityClass);
        return dictionary(gsa);//, ID, null, null);
    }
    
    /**
     * Builds a dictionary from searchable repository metadata with default description.
     *
     * @param <T> the entity type extending {@link SearchableEntity}
     * @param gsa the searchable repository metadata describing the entity
     * @return map of entity IDs to descriptions, or empty map if metadata is null
     */
    public <T extends SearchableEntity> Map dictionary(SearchableRepositoryMetadata gsa) {
        String customDescriptionFormula = null;
        return dictionary(gsa, customDescriptionFormula);
    }
    /**
     * Core dictionary builder applying security predicates and optional custom SQL description formula.
     * <p>
     * Executes a two-phase query: first retrieves entity IDs filtered by user privileges and organization scope
     * via {@link #toSecurePredicate}, then fetches descriptions using native SQL with the custom formula.
     * Organization-related entities are automatically filtered by {@link TenantResolver} organization context.

     *
     * @param <T> the entity type extending {@link SearchableEntity}
     * @param gsa the searchable repository metadata, must not be null
     * @param customDescriptionFormula optional SQL expression for description (null defaults to "id")
     * @return linked map preserving insertion order, mapping entity IDs to descriptions
     */
    public <T extends SearchableEntity> Map dictionary(SearchableRepositoryMetadata gsa, @Nullable String customDescriptionFormula) {
        if (gsa == null) {
            warn("SearchableRepository for entity key not found");
            return Collections.emptyMap();
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class).distinct(true);
        Root root = q.from(gsa.entityClass());
        q.select(root.get(ID));
        Long organizationId = TenantResolver.getTenantedResource().organizationId;
        q.where(toSecurePredicate((r, query, c) -> organizationId == null || !isOrganizationRelated(r.getModel().getJavaType())? c.conjunction() : c.equal(r.get("organizationId"), organizationId),null, root, q, cb, SecurityScope.USER));
        List<Long> result = em.createQuery(q).getResultList();
        String tableName = SearchableRepositories.discoverTableName(gsa.entityClass());
        String descriptionFormula = customDescriptionFormula != null ? "(''||COALESCE(" + customDescriptionFormula + ",''))" : "id";
        Query query = em.createNativeQuery("select id, " + descriptionFormula + " from " +  tableName + " where id in (:ids) order by " + descriptionFormula);
        query.setParameter("ids", result);
        List<Object[]> r = query.getResultList();

        Map<Object, String> res = new LinkedHashMap<>(r.size());
        for (Object[] t : r) {
            res.put(t[0], t[1]+"");
        }
        return res;

    }

    /**
     * Builds a dictionary using entity ID as key and specified label field for display values.
     *
     * @param <T> the entity type
     * @param entityClass the JPA entity class
     * @param labelField the entity field name for dictionary labels
     * @return map of entity IDs to label field values, respecting security scope
     */
    public <T extends SearchableEntity> Map dictionary(Class<T> entityClass, String labelField) {
        return dictionary(entityClass, ID, labelField, labelField);
    }

    /**
     * Builds a dictionary with custom key and label fields.
     *
     * @param <T> the entity type
     * @param entityClass the JPA entity class
     * @param keyField the field name to use as dictionary keys
     * @param labelField the field name to use as dictionary labels
     * @return map of key field values to label field values
     */
    public <T extends SearchableEntity> Map dictionary(Class<T> entityClass, String keyField, String labelField) {
        return dictionary(entityClass, keyField, labelField, labelField);
    }

    /**
     * Builds a dictionary for entity key with custom key and label fields.
     *
     * @param <T> the entity type
     * @param entityKey the searchable entity key
     * @param keyField the field name to use as dictionary keys
     * @param labelField the field name to use as dictionary labels
     * @return map of key field values to label field values, or empty map if entity not found
     */
    public <T extends SearchableEntity> Map dictionary(String entityKey, String keyField, String labelField) {
        Class<SearchableOrganizationRelatedEntity> entityClass = (Class<SearchableOrganizationRelatedEntity>) SearchableRepositories.getSearchableRepositoryMetadata(entityKey).entityClass();
        if (entityClass == null) {
            warn("SearchableRepository for entity key {} not found", entityKey);
            return Collections.emptyMap();
        }
        return dictionary(entityClass, keyField, labelField, labelField);
    }

    /**
     * Builds a dictionary with custom key, label, and sort fields using JPA Criteria API.
     *
     * @param <T> the entity type
     * @param entityClass the JPA entity class
     * @param keyField the field name for dictionary keys
     * @param labelField the field name for dictionary labels
     * @param sortField the field name for sorting results
     * @return linked map ordered by sort field, mapping keys to labels
     */
    public <T extends SearchableEntity> Map dictionary(Class<T> entityClass, String keyField, String labelField, String sortField) {
        CriteriaQuery<Tuple> q = getTupleCriteriaQuery(entityClass, keyField, labelField, sortField).distinct(true);
        List<Tuple> result = em.createQuery(q).getResultList();
        return toLinkedMap(result);
     }

    /**
     * Builds a dictionary with post-query tuple transformation for complex mappings.
     *
     * @param <T> the entity type
     * @param entityClass the JPA entity class
     * @param label the field name for labels
     * @param postQueryTupleMapper function transforming each tuple into a stream of tuples (null for no transformation)
     * @return map of IDs to labels after applying optional transformation
     */
    public <T extends SearchableEntity> Map dictionary(Class<T> entityClass, String label, Function<Tuple, ? extends Stream<Tuple>> postQueryTupleMapper) {
        CriteriaQuery<Tuple> q = getTupleCriteriaQuery(entityClass, ID, label, label).distinct(true);
        List<Tuple> result = em.createQuery(q).getResultList();
        if (postQueryTupleMapper != null) {
            result = result.stream().flatMap(postQueryTupleMapper).distinct().collect(Collectors.toList());
        }
        return toLinkedMap(result);
    }

    /**
     * Constructs a JPA Criteria query for tuple-based dictionary retrieval with security predicates.
     *
     * @param <T> the entity type
     * @param entityClass the JPA entity class
     * @param keyField the field name for dictionary keys
     * @param labelField the field name for dictionary labels
     * @param sortField the field name for ordering (null for no ordering)
     * @return criteria query selecting tuples of (keyField, labelField) filtered by user scope
     */
    private <T extends SearchableEntity> CriteriaQuery<Tuple> getTupleCriteriaQuery(Class<T> entityClass, String keyField, String labelField, String sortField) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> q = cb.createQuery(Tuple.class);
        Root<T> root = q.from(entityClass);
        q.select(cb.construct(Tuple.class, root.get(keyField), root.get(labelField)));
        q.where(toSecurePredicate(null, null, root, q, cb, SecurityScope.USER));
        if (sortField != null) {
            q.orderBy(cb.asc(root.get(sortField)));
        }
        return q;
    }

    /**
     * Initializes the repository at application startup, populating language dictionaries and registering singleton.
     * <p>
     * Converts configured language tags to localized display names and sets this instance as the global
     * singleton for use by {@link AbstractForm} and other components requiring dictionary access.

     */
    @PostConstruct void init() {
        for (String lang : languagesList) {
            Locale locale = Locale.forLanguageTag(lang);
            languages.put(lang, locale.getDisplayLanguage(locale));
        }
        AbstractForm.setSecureEntityDictionaryRepositoryOnce(this);
        instance = this;
    }

    /**
     * Retrieves a dictionary of available loggers for debug log configuration.
     * Maps fully-qualified class names to simple class names for all loggers registered
     * in {@link DebugLogsDecoratorWithRequestId#availableLoggers}.
     *
     * @return map of logger class names (FQN) to simple names, preserving insertion order
     */
    public Map<String, String> getLoggersDictionary() {
        Map<String, String> d = new LinkedHashMap<>(DebugLogsDecoratorWithRequestId.availableLoggers.size());
        for(Class c : DebugLogsDecoratorWithRequestId.availableLoggers) {
            d.put(c.getName(), c.getSimpleName());
        }
        return d;
    }

    /**
     * Returns a JSON string containing all common dictionaries for frontend UI consumption.
     * <p>
     * Triggers {@link #setupCommonDictionaries()} to refresh cached data and serializes
     * the {@code commonDictionaries} map to JSON format. Includes frequently-used entity lists
     * such as roles, privileges, languages, event classes, HTTP methods, and countries.

     *
     * @return JSON string representation of common dictionaries map
     * @throws JSONException if JSON serialization fails
     */
    public String getCommonDictionaries() throws JSONException {
        setupCommonDictionaries();
        return JsonHelper.to(commonDictionaries);
    }

    /**
     * Returns the internal map of common dictionaries without JSON serialization.
     *
     * @return map of dictionary keys (with "datalist:" prefix) to JSON array strings or objects
     */
    public Map<String, Object> getCommonDictionariesMap() {
        return commonDictionaries;
    }

    /**
     * Assembles and caches common dictionaries by querying services and repositories.
     * <p>
     * Populates {@code commonDictionaries} with JSON-serialized arrays for boolean values, privileges,
     * privilege groups, frontend resource types, consumers, roles (organization/global/global-organization),
     * languages, events, event classes, role types, HTTP methods, response types, countries, operators,
     * and logical operators. Module-specific dictionaries registered via {@link #addModuleDictionary}
     * are also included. Dictionary map is atomically replaced to ensure consistency during concurrent access.

     *
     * @throws JSONException if JSON serialization of any dictionary fails
     */
    public void setupCommonDictionaries() throws JSONException {
        Map<String, Object> newCommonDictionaries = new HashMap<>();
        newCommonDictionaries.clear();
        newCommonDictionaries.put(DATALIST_PREFIX + "booleanValues", toJsonString(Map.of("true", "YES", "false", "NO")));
        newCommonDictionaries.put(DATALIST_PREFIX + "privileges", PrivilegeHelper.allEnumsAsPrivilegeBaseJsonString(true));
        newCommonDictionaries.put(DATALIST_PREFIX + "privilegeGroups", enumsLabelToJsonString(PrivilegeGroup.values()));
        newCommonDictionaries.put(DATALIST_PREFIX + "privilegesGrouped", PrivilegeHelper.allEnumsAsPrivilegeBaseJsonString(false));
        newCommonDictionaries.put(DATALIST_PREFIX + "frontendResourceType", enumsToJsonString(FrontendResource.Type.values()));
        newCommonDictionaries.put(DATALIST_PREFIX + "consumers", mapToJsonString(services.eventListener.getConsumersArray()));
        newCommonDictionaries.put(DATALIST_PREFIX + "organizationRoles", listTupleToJsonString(repositories.unsecure.organizationRole.findAllAsTupleWithLabelName()));
        newCommonDictionaries.put(DATALIST_PREFIX + "globalRoles", listTupleToJsonString(repositories.unsecure.globalRole.findAllAsTupleWithLabelName()));
        newCommonDictionaries.put(DATALIST_PREFIX + "languages", toJsonString(languages));
        newCommonDictionaries.put(DATALIST_PREFIX + "events", mapObjectToJsonString(services.eventListener.getEvents()));
        //newCommonDictionaries.put(DATALIST_PREFIX + "eventClasses", mapObjectToJsonString(services.eventListener.getEvents()));
        newCommonDictionaries.put(DATALIST_PREFIX + "eventClasses", mapObjectToJsonString(services.dynamicEntity.getAll()));
        newCommonDictionaries.put(DATALIST_PREFIX + "roleTypes", listStringToJsonString(roleTypes));
        newCommonDictionaries.put(DATALIST_PREFIX + "httpMethod", enumsToJsonString(ControllerEndpoint.HttpMethod.values()));
        newCommonDictionaries.put(DATALIST_PREFIX + "responseType", enumsToJsonString(ControllerEndpoint.ResponseType.values()));
        newCommonDictionaries.put(DATALIST_PREFIX + "countries", toJsonString(countries));
        newCommonDictionaries.put(DATALIST_PREFIX + "operators", enumsLabelToJsonString(Operator.values()));
        newCommonDictionaries.put(DATALIST_PREFIX + "logicalOperators", enumsLabelToJsonString(LogicalOperator.values()));
        newCommonDictionaries.put(DATALIST_PREFIX + "globalOrganizationRoles", listTupleToJsonString(repositories.unsecure.globalOrganizationRole.findAllAsTuple()));
        newCommonDictionaries.putAll(moduleDictionaries);
        commonDictionaries = newCommonDictionaries;
    }

    /**
     * Retrieves organization-specific dictionaries as JSON (currently returns empty result).
     *
     * @param organizationId the organization identifier for scoped dictionary data
     * @return JSON string of organization-scoped dictionaries (currently empty map)
     */
    public String getOrganizationDictionaries(Long organizationId) {
        Map<String, Map<Object, String>> result = new HashMap<>(3);
        return JsonHelper.to(result);
    }
    
    /**
     * Returns the set of all common dictionary keys available in the cache.
     *
     * @return set of dictionary names (typically with "datalist:" prefix)
     */
    public Set<String> getCommonDictionariesNames() {
        return commonDictionaries.keySet();
    }
    
    /**
     * Converts a list of tuples to a linked map preserving insertion order.
     * Extracts first tuple element as key (Object) and second as value (String).
     *
     * @param allByOrganizationId list of tuples containing (key, value) pairs
     * @return linked map of keys to string values
     */
    public static Map<Object, String> toLinkedMap(List<Tuple> allByOrganizationId) {
        Map<Object, String> result = new LinkedHashMap<>(allByOrganizationId.size());
        for (Tuple t : allByOrganizationId) {
            result.put(t.v(Object.class, 0), t.v(String.class, 1));
        }
        return result;
    }

    /**
     * Converts an array of objects to a linked map using toString for both keys and values.
     *
     * @param values array of objects to convert
     * @return linked map of objects to their string representations
     */
    public Map<Object, String> toLinkedMap(Object[] values) {
        Map<Object, String> result = new LinkedHashMap<>(values.length);
        for (Object t : values) {
            result.put(t, t.toString());
        }
        return result;
    }

    /**
     * Creates a dictionary from enum values using enum instances as keys and names as values.
     *
     * @param <E> the enum type
     * @param enumValues array of enum constants
     * @return map of enum instances to enum names
     */
    public <E extends Enum<E>> Map<Object, String> enumDictionary(E[] enumValues) {
        return enumsToMap(enumValues);
    }

    /**
     * Converts enum array to map with enum instances as keys and names as values.
     *
     * @param <E> the enum type
     * @param enumValues array of enum constants
     * @return map of enum instances to enum names
     */
    public static <E extends Enum<E>> Map<Object, String> enumsToMap(E[] enumValues) {
        return Arrays.stream(enumValues).collect(Collectors.toMap(Function.identity(), E::name));
    }
    
    /**
     * Converts enum array to map with enum instances as keys and labels as values.
     * Requires enum to implement {@link OptionWithLabel} interface.
     *
     * @param <E> the enum type extending {@link OptionWithLabel}
     * @param enumValues array of enum constants
     * @return map of enum instances to their display labels
     */
    public static <E extends Enum<E>> Map<Object, String> enumsToMapWithLabels(E[] enumValues) {
        return Arrays.stream(enumValues).collect(Collectors.toMap(Function.identity(), e -> ((OptionWithLabel) e).getLabel()));
    }


    /**
     * Serializes an enum array to JSON array string using enum names for both keys and values.
     *
     * @param <E> the enum type
     * @param enumClass array of enum constants
     * @return JSON array string with objects containing "k" and "v" properties
     * @throws JSONException if JSON serialization fails
     */
    private <E extends Enum<E>> String enumsToJsonString(E[] enumClass) throws JSONException {
        JSONArray results = new JSONArray();
        JSONObject result;
        for (E e : enumClass) {
            result = new JSONObject();
            result.put("k", e.name());
            result.put("v", e.name());
            results.put(result);
        }
        return results.toString();
    }

    /**
     * Serializes an enum array to JSON using names as keys and labels as values.
     * Requires enum to implement {@link OptionWithLabel}.
     *
     * @param <E> the enum type
     * @param enumClass array of enum constants implementing {@link OptionWithLabel}
     * @return JSON array string with "k" (name) and "v" (label) properties
     * @throws JSONException if JSON serialization fails
     */
    private <E extends Enum<E>> String enumsLabelToJsonString(E[] enumClass) throws JSONException {
        JSONArray results = new JSONArray();
        JSONObject result;
        for (E e : enumClass) {
            result = new JSONObject();
            result.put("k", e.name());
            result.put("v", ((OptionWithLabel) e).getLabel());
            results.put(result);
        }
        return results.toString();
    }

    /**
     * Serializes a map of object arrays to JSON format.
     *
     * @param objArray map with keys and array values
     * @return JSON array string with "k" and "v" (JSON array) properties
     * @throws JSONException if JSON serialization fails
     */
    private Object mapObjectArraysToJsonString(Map<Object, Object[]> objArray) throws JSONException {
        JSONArray results = new JSONArray();
        JSONObject result;
        for (var entry : objArray.entrySet()) {
            result = new JSONObject();
            result.put("k", entry.getKey());
            JSONArray values = new JSONArray();
            for (Object o : entry.getValue()) {
                values.put(o);
            }
            result.put("v", values);
            results.put(result);
        }
        return results.toString();
    }

    /**
     * Serializes a map of objects to strings as JSON array.
     *
     * @param map the map to serialize
     * @return JSON array string with "k" and "v" properties for each entry
     * @throws JSONException if JSON serialization fails
     */
    private String mapObjectToJsonString(Map<Object, String> map) throws JSONException {
        JSONArray results = new JSONArray();
        JSONObject result;
        for (var entry : map.entrySet()) {
            result = new JSONObject();
            result.put("k", entry.getKey());
            result.put("v", entry.getValue());
            results.put(result);
        }
        return results.toString();
    }

    /**
     * Serializes a string-to-string map as JSON array.
     *
     * @param map the map to serialize
     * @return JSON array string with "k" and "v" properties
     * @throws JSONException if JSON serialization fails
     */
    private String toJsonString(Map<String, String> map) throws JSONException {
        JSONArray results = new JSONArray();
        JSONObject result;
        for (var entry : map.entrySet()) {
            result = new JSONObject();
            result.put("k", entry.getKey());
            result.put("v", entry.getValue());
            results.put(result);
        }
        return results.toString();
    }

    /**
     * Serializes a nested map structure to JSON array with object values.
     *
     * @param map map with object keys and nested string maps as values
     * @return JSON array string with "k" and "v" (JSON object) properties
     * @throws JSONException if JSON serialization fails
     */
    private String mapToJsonString(Map<Object, Map<String, String>> map) throws JSONException {
        JSONArray results = new JSONArray();
        JSONObject result;
        for (var entry : map.entrySet()) {
            result = new JSONObject();
            result.put("k", entry.getKey());
            JSONObject value = new JSONObject();
            for (var valueEntry : entry.getValue().entrySet()) {
                value.put(valueEntry.getKey(), valueEntry.getValue());
            }
            result.put("v", value);
            results.put(result);
        }
        return results.toString();
    }

    /**
     * Serializes a list of tuples to JSON array, applying i18n message lookup for labels.
     *
     * @param allByOrganizationId list of tuples with (key, label) pairs
     * @return JSON array string with localized labels where available
     * @throws JSONException if JSON serialization fails
     */
    private String listTupleToJsonString(List<Tuple> allByOrganizationId) throws JSONException {
        JSONArray results = new JSONArray();
        JSONObject result;
        for (Tuple t : allByOrganizationId) {
            result = new JSONObject();
            String label = messages.get(t.v(String.class, 1));
            result.put("k", t.v(String.class, 0));
            result.put("v", StringUtils.isNotEmpty(label) ? label : t.v(String.class, 0));
            results.put(result);
        }
        return results.toString();
    }

    /**
     * Serializes a list of strings to JSON array using same value for keys and labels.
     *
     * @param allByOrganizationId list of string values
     * @return JSON array string with matching "k" and "v" properties
     * @throws JSONException if JSON serialization fails
     */
    private String listStringToJsonString(List<String> allByOrganizationId) throws JSONException {
        JSONArray results = new JSONArray();
        JSONObject result;
        for (String s : allByOrganizationId) {
            result = new JSONObject();
            result.put("k", s);
            result.put("v", s);
            results.put(result);
        }
        return results.toString();
    }

    /**
     * Converts tuple list to linked map with i18n message lookup for labels.
     *
     * @param allByOrganizationId list of tuples containing (key, message_key) pairs
     * @return map with keys to localized labels (or keys if message not found)
     */
    private Map<Object, String> toLinkedMapWithCustomLabels(List<Tuple> allByOrganizationId) {
        Map<Object, String> result = new LinkedHashMap<>(allByOrganizationId.size());
        for (Tuple t : allByOrganizationId) {
            String label = messages.get(t.v(String.class, 1));
            result.put(t.v(String.class, 0), StringUtils.isNotEmpty(label) ? label : t.v(String.class, 0));
        }
        return result;
    }
    
    /**
     * Registers a module-specific dictionary for inclusion in common dictionaries.
     * Module dictionaries are merged into {@link #commonDictionaries} during {@link #setupCommonDictionaries()}.
     *
     * @param dictionary the map of dictionary entries
     * @param dictName the dictionary name (typically with "datalist:" prefix)
     */
    public static void addModuleDictionary(Map<Object,String> dictionary, String dictName){
        moduleDictionaries.put(dictName, dictionary);
    }
    
    /**
     * Converts a collection of objects to linked map using toString for keys and values.
     *
     * @param values collection of objects to convert
     * @return linked map of objects to their string representations
     */
    public static Map<Object, String> collectionToLinkedMap(Collection<Object> values) {
        Map<Object, String> result = new LinkedHashMap<>(values.size());
        for (Object t : values) {
            result.put(t, t.toString());
        }
        return result;
    }

    /**
     * Retrieves file DTOs for an entity with associated files.
     * Converts entity's {@link File} collection to indexed {@link FileDto} map for frontend use.
     *
     * @param entity the entity implementing {@link EntityWithFiles}
     * @return map of sequential indices to file DTOs, or empty map if entity or files are null
     */
    public final Map<Long, FileDto> getFileDtos(EntityWithFiles entity)  {
        if (entity == null || entity.getFiles() == null) {
            return Collections.emptyMap();
        }
        Map<Long, FileDto> result = new LinkedHashMap<>();
        Long i = 0l;
        for (File a : entity.getFiles()) {
            if (a != null) {
                result.put(i, toFileDto(a));
                i++;
            }
        }
        return result;
    }

}
