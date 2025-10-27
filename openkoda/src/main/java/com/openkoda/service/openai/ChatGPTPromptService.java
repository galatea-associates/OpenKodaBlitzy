package com.openkoda.service.openai;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.helper.ReflectionHelper;
import com.openkoda.model.common.OpenkodaEntity;
import com.openkoda.model.common.SearchableEntity;
import com.openkoda.repository.SearchableRepositories;
import com.openkoda.service.autocomplete.WebendpointAutocompleteService;
import com.openkoda.uicomponent.annotation.AiHint;
import com.openkoda.uicomponent.annotation.Autocomplete;
import com.openkoda.uicomponent.live.LiveComponentProvider;
import jakarta.inject.Inject;
import jakarta.persistence.Table;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Formula;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.openkoda.core.helper.NameHelper.toColumnName;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.capitalize;

/**
 * Template-driven prompt composition service with reflection-based context generation for ChatGPT.
 * <p>
 * This service generates comprehensive prompts for ChatGPT by loading templates from the classpath
 * {@code /gpt/} directory and substituting placeholders with dynamically generated context information.
 * Template placeholders {@code ${CONTEXT_SPECIFICATION}} and {@code ${DATA_MODEL}} are replaced with
 * reflection-derived metadata from the OpenKoda domain model and service layer.
 * </p>
 * <p>
 * Context generation uses reflection to inspect {@link LiveComponentProvider} fields annotated with
 * {@link Autocomplete}, extracting method signatures to provide ChatGPT with available service APIs.
 * Data model introspection examines {@link SearchableEntity} classes to generate method signatures,
 * field hints from {@link AiHint} annotations, and JPA relationship descriptions.
 * </p>
 * <p>
 * Template syntax supports variable substitution with {@code ${VARIABLE_NAME}} placeholders.
 * The service performs deterministic placeholder replacement with null-safe string concatenation.
 * </p>
 * <p>
 * Configuration: Excluded fields can be configured via the {@code chat.gpt.promptFileName.excludedFields}
 * property to filter sensitive or irrelevant entity fields from AI context generation.
 * </p>
 * <p>
 * Architecture: Uses {@link ReflectionHelper} for type introspection, {@link SearchableRepositories}
 * for entity metadata, and Apache Commons IOUtils for template file loading. Generates deterministic
 * output suitable for AI prompt engineering with comprehensive domain context.
 * </p>
 * <p>
 * Thread-safety: Stateless service, thread-safe. All methods operate on method parameters without
 * shared mutable state.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * String prompt = chatGPTPromptService.getPromptFromFileForEntities("generate_code.txt", "user", "organization");
 * }</pre>
 * </p>
 *
 * @see ChatGPTService for prompt execution and conversation management
 * @see com.openkoda.model.Conversation entity for ChatGPT conversation persistence
 * @see Autocomplete annotation for service method exposure
 * @see AiHint annotation for entity field hints
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
@Service
public class ChatGPTPromptService extends ComponentProvider {

    @Value("${chat.gpt.promptFileName.excludedFields:}")
    String[] excludedFields;
    private static final String templatePath = "/gpt/";

    private static final String defaultPrompt = "promptFileName.txt";

    @Inject
    ReflectionHelper reflectionHelper;
    @Inject
    WebendpointAutocompleteService webendpointAutocompleteService;

    /**
     * Loads a prompt template from classpath and substitutes placeholders with generated context.
     * <p>
     * This is the primary API method for generating ChatGPT prompts. Loads the specified template
     * file from the {@code /gpt/} directory in the classpath, replacing {@code ${CONTEXT_SPECIFICATION}}
     * with service method suggestions and {@code ${DATA_MODEL}} with entity metadata for the specified
     * repository keys. Falls back to {@code defaultPrompt} if the template file is not found.
     * </p>
     * <p>
     * Template loading uses classpath resource resolution via {@link Class#getResourceAsStream(String)}.
     * Placeholder replacement uses simple string substitution with context and data model generators.
     * Null or empty entityKeys array results in empty data model content.
     * </p>
     * <p>
     * Example with entity keys:
     * <pre>{@code
     * String prompt = getPromptFromFileForEntities("generate_code.txt", "user", "organization");
     * }</pre>
     * </p>
     *
     * @param promptFileName template filename under {@code /gpt/} directory (e.g., "generate_code.txt"),
     *                       defaults to "promptFileName.txt" if null
     * @param entityKeys varargs array of entity repository keys for data model generation (e.g., "user",
     *                   "organization"), may be null or empty for context-only prompts
     * @return assembled prompt string with substituted placeholders, empty string if template not found
     * @see #getContextSpecification() for context generation
     * @see #getDataModels(String...) for data model generation
     */
    public String getPromptFromFileForEntities(String promptFileName, String... entityKeys) {
        debug("[getWebEndpointPrompt] {}", entityKeys);
        if(entityKeys == null) {
            entityKeys = new String[0];
        }
        String promptTemplate="";
        String promptPath = templatePath + StringUtils.defaultString(promptFileName, defaultPrompt);
        try (InputStream resourceIO = this.getClass().getResourceAsStream(promptPath)) {
            debug("[getWebEndpointPrompt] read promptFileName {}", promptPath);
            if (resourceIO != null) {
                promptTemplate = IOUtils.toString(resourceIO);
            }
        } catch (IOException e) {
            error("Could not load ChatGPT promptFileName template", e);
        }

        return promptTemplate.replace("${CONTEXT_SPECIFICATION}", getContextSpecification())
                .replace("${DATA_MODEL}", getDataModels(entityKeys));

    }

    /**
     * Generates context specification string containing available service method suggestions.
     * <p>
     * Uses reflection to inspect all fields declared in {@link LiveComponentProvider}, filtering
     * for methods annotated with {@link Autocomplete} to generate a sorted list of service method
     * suggestions in the format {@code context.services.fieldName.methodName(paramTypes)}.
     * This provides ChatGPT with awareness of available OpenKoda service APIs.
     * </p>
     *
     * @return context specification string with service method suggestions, one per line, sorted alphabetically
     * @see LiveComponentProvider for injected service fields
     * @see Autocomplete annotation for method exposure filtering
     */
    private String getContextSpecification(){
        debug("[getContextSpecification]");
        return stream(LiveComponentProvider.class.getDeclaredFields())
                .map(f -> getSuggestions(getExposedMethods(f.getType().getName()), f.getName()))
                .flatMap(Collection::stream)
                .sorted()
                .collect(joining("\n"));
    }

    /**
     * Retrieves methods annotated with {@link Autocomplete} from the specified class.
     * <p>
     * Uses {@link ReflectionHelper} to get declared methods and filters for those marked with
     * {@link Autocomplete} annotation, indicating they should be exposed in AI context.
     * </p>
     *
     * @param className fully qualified class name to inspect for exposed methods
     * @return array of methods annotated with {@link Autocomplete}, empty array if none found
     */
    private Method[] getExposedMethods(String className){
        return stream(reflectionHelper.getDeclaredMethods(className))
                .filter(f -> f.isAnnotationPresent(Autocomplete.class))
                .toArray(Method[]::new);
    }

    /**
     * Formats method array into suggestion strings with context.services prefix.
     * <p>
     * Transforms methods into AI-readable suggestions formatted as
     * {@code context.services.variableName.methodName(paramTypes)}.
     * </p>
     *
     * @param methods array of methods to format into suggestions
     * @param variableName variable name prefix for the service field (e.g., "dataServices")
     * @return list of formatted suggestion strings, empty list if methods array is empty
     */
    private List<String> getSuggestions(Method[] methods, String variableName){
        debug("[getSuggestions-1]");
        return stream(methods)
                .map(m->"context.services." + getSuggestion(variableName,m))
                .collect(Collectors.toList());
    }

    /**
     * Formats a single method into a suggestion string with variable name prefix.
     * <p>
     * Uses {@link ReflectionHelper#getNameWithParamNamesAndTypes(Method)} to generate
     * method signature including parameter names and types.
     * </p>
     *
     * @param variableName variable name to prepend to method signature, null for no prefix
     * @param method method to format into suggestion string
     * @return formatted suggestion string (e.g., "dataServices.getData(String key)")
     */
    private String getSuggestion(String variableName, Method method){
        debug("[getSuggestions-2]");
        return (variableName != null ? variableName + "." : "") + reflectionHelper.getNameWithParamNamesAndTypes(method);
    }

    /**
     * Generates concatenated data models for multiple entity repository keys.
     * <p>
     * Processes each entity key through {@link #getDataModel(String)} and concatenates results
     * with newline separators. Each data model includes method signatures, manual entries for
     * {@link OpenkodaEntity} methods, AI hints, and relationship descriptions.
     * </p>
     *
     * @param entityKeys varargs array of entity repository keys (e.g., "user", "organization")
     * @return concatenated data models for all entities, one per line, empty string if no keys provided
     * @see #getDataModel(String) for single entity data model generation
     */
    public String getDataModels(String ... entityKeys){
        debug("[getDataModels] {}", entityKeys);
        return Arrays.stream(entityKeys).map(this::getDataModel).collect(joining("\n"));
    }

    /**
     * Generates concatenated database schemas for multiple entity repository keys.
     * <p>
     * Processes each entity key through {@link #getDataSchema(String)} to extract JPA table names
     * and column names from entity field mappings. Provides database-level schema information for AI context.
     * </p>
     *
     * @param entityKeys varargs array of entity repository keys for schema generation
     * @return concatenated database schemas with table and column names, empty string if no keys provided
     * @see #getDataSchema(String) for single entity schema generation
     */
    public String getDataSchemas(String ... entityKeys){
        debug("[getDataSchemas] {}", entityKeys);
        return Arrays.stream(entityKeys).map(this::getDataSchema).collect(joining("\n"));
    }

    /**
     * Generates complete data model description for a single entity repository key.
     * <p>
     * Retrieves the {@link SearchableEntity} class from {@link SearchableRepositories} and generates
     * a comprehensive data model including: entity prefix, detected getter/setter method signatures,
     * manual {@link OpenkodaEntity} method entries (getProperty, setProperty, timestamps, organizationId),
     * all {@link AiHint} annotations, and JPA relationship descriptions. Provides complete entity API
     * surface for AI-assisted code generation.
     * </p>
     *
     * @param entityKey single entity repository key to generate data model for (e.g., "user")
     * @return complete data model description with methods, hints, and relationships
     * @see SearchableRepositories#getSearchableRepositoryEntityClass(String) for entity class resolution
     * @see OpenkodaEntity for base entity methods
     */
    private String getDataModel(String entityKey){
        debug("[getDataModel] {}", entityKey);
        Class<SearchableEntity> entityClass = SearchableRepositories.getSearchableRepositoryEntityClass(entityKey);
        return getPrefix(entityClass) + ":\n" +
                getDetectedMethodEntries(entityClass) + "\n" +
                getManualMethodEntries(entityClass) + "\n" +
                getAllHints(entityClass) + "\n" +
                getRelated(entityClass) + "\n";
    }

    /**
     * Generates database schema description for a single entity repository key.
     * <p>
     * Extracts JPA {@link Table} annotation to determine table name (falls back to entity key if
     * annotation not present), then maps eligible entity fields to database column names using
     * {@link com.openkoda.core.helper.NameHelper#toColumnName(String)}. Includes AI hints for
     * additional context. Provides database-level schema information for SQL generation.
     * </p>
     *
     * @param entityKey single entity repository key for schema extraction
     * @return database schema string with table name and column names in parentheses (e.g., "users (id,name,email)")
     * @see Table annotation for table name resolution
     */
    private String getDataSchema(String entityKey){
        debug("[getDataSchema] {}", entityKey);
        Class<SearchableEntity> entityClass = SearchableRepositories.getSearchableRepositoryEntityClass(entityKey);
        Table[] annotationsByType = entityClass.getAnnotationsByType(Table.class);
        String tableName = annotationsByType.length > 0 ? annotationsByType[0].name() : entityKey;
        return tableName + " (" +
                getEligibleFields(entityClass).stream().map(field -> toColumnName(field.getName())).collect(joining(",")) + ")\n" +
                getAllHints(entityClass) + "\n";
    }

    /**
     * Generates formatted method signature strings for eligible entity methods.
     * <p>
     * Detects eligible getter and setter methods via {@link #detectEligibleMethods(Class)} and formats
     * each with parameter types and return type using {@link ReflectionHelper}. Prefixes method signatures
     * with lowercased entity name for context.
     * </p>
     *
     * @param entityClass entity class to extract method signatures from
     * @return formatted method signatures with entity prefix, one per line (e.g., "user.getName() : String")
     * @see #detectEligibleMethods(Class) for method detection logic
     */
    private String getDetectedMethodEntries(Class<SearchableEntity> entityClass){
        debug("[getDetectedMethodEntries]");
        return detectEligibleMethods(entityClass)
                .stream()
                .map(m -> reflectionHelper.getNameWithParamNamesAndTypesAndReturnType(m, getPrefix(entityClass) + "."))
                .collect(joining("\n"));
    }

    /**
     * Generates manual method entries for {@link OpenkodaEntity} base methods.
     * <p>
     * If the entity class is assignable to {@link OpenkodaEntity}, adds method signatures for
     * property bag accessors (getProperty, setProperty), auditing timestamps (getCreatedOn, getUpdatedOn),
     * and multi-tenancy identifier (getOrganizationId). These methods are not detected via reflection
     * on the entity class itself but are inherited from the base entity.
     * </p>
     *
     * @param entityClass entity class to check for {@link OpenkodaEntity} inheritance
     * @return newline-separated manual method entries if entity extends {@link OpenkodaEntity}, empty string otherwise
     * @see OpenkodaEntity for base entity methods
     */
    private String getManualMethodEntries(Class<SearchableEntity> entityClass){
        debug("[getManualMethodEntries]");
        List<String> result = new ArrayList<>();
        if(OpenkodaEntity.class.isAssignableFrom(entityClass)){
            result.add("String " + getPrefix(entityClass) + ".getProperty(String key)");
            result.add("void " + getPrefix(entityClass) + ".setProperty(String key, String value)");
            result.add("LocalDateTime " + getPrefix(entityClass) + ".getCreatedOn()");
            result.add("LocalDateTime " + getPrefix(entityClass) + ".getUpdatedOn()");
            result.add("Long " + getPrefix(entityClass) + ".getOrganizationId()");
        }
        return String.join("\n", result);
    }

    /**
     * Detects eligible getter and setter methods from entity class fields.
     * <p>
     * Iterates through eligible fields and attempts to find corresponding getter methods. For simple
     * types (determined via {@link ReflectionHelper#isSimpleType(Field)}), also attempts to find setter
     * methods unless the field is annotated with {@link Formula} (indicating a computed field).
     * Complex types only include getters. Swallows {@link NoSuchMethodException} for missing accessors.
     * </p>
     *
     * @param entityClass entity class to analyze for getter/setter methods
     * @return list of detected getter and setter methods for eligible fields
     * @see #getEligibleFields(Class) for field filtering logic
     * @see Formula annotation for computed field detection
     */
    private List<Method> detectEligibleMethods(Class<SearchableEntity> entityClass){
        debug("[detectEligibleMethods]");
        List<Method> methods = new ArrayList<>();
        for(Field f : getEligibleFields(entityClass)){
            if(reflectionHelper.isSimpleType(f)){
                addGetterIfExists(f,entityClass, methods);
                if(f.getAnnotation(Formula.class) == null){
                    addSetterIfExists(f, entityClass, methods);
                }
            } else {
                addGetterIfExists(f, entityClass, methods);
            }
        }
        return methods;
    }

    /**
     * Filters entity class fields to eligible non-static, non-excluded fields.
     * <p>
     * Applies {@link #isEligible(Field)} predicate to filter declared fields, excluding static
     * fields and fields listed in the {@code excludedFields} configuration property.
     * </p>
     *
     * @param entityClass entity class to extract fields from
     * @return filtered list of eligible instance fields for data model generation
     * @see #isEligible(Field) for eligibility criteria
     */
    private List<Field> getEligibleFields(Class<SearchableEntity> entityClass){
        debug("[getEligibleFields]");
        return stream(entityClass.getDeclaredFields())
                .filter(this::isEligible)
                .collect(Collectors.toList());
    }

    /**
     * Determines if a field is eligible for data model inclusion.
     * <p>
     * A field is eligible if it is not listed in the {@code excludedFields} configuration array
     * and is not a static field. Filters sensitive or framework-internal fields from AI context.
     * </p>
     *
     * @param field field to check eligibility for
     * @return {@code true} if field is eligible for inclusion, {@code false} otherwise
     */
    private boolean isEligible(Field field){
        return !Arrays.asList(excludedFields).contains(field.getName())
                && !Modifier.isStatic(field.getModifiers());
    }

    /**
     * Attempts to find and add setter method for the specified field to the method list.
     * <p>
     * Constructs setter name as "set" + capitalized field name and attempts to resolve via
     * {@link Class#getMethod(String, Class[])}. Swallows {@link NoSuchMethodException} if setter
     * does not exist (e.g., for read-only fields or non-standard naming conventions).
     * </p>
     *
     * @param field field to find setter for
     * @param entityClass entity class to search for setter method
     * @param methods list to add found setter method to
     */
    private void addSetterIfExists(Field field, Class<SearchableEntity> entityClass, List<Method> methods){
        try {
            Method m = entityClass.getMethod("set" + capitalize(field.getName()), field.getType());
            methods.add(m);
        } catch (NoSuchMethodException e) {
            //do nothing
        }
    }

    /**
     * Attempts to find and add getter method for the specified field to the method list.
     * <p>
     * Constructs getter name using "get" prefix for non-boolean fields or "is" prefix for boolean
     * fields (determined via {@link ReflectionHelper#isBoolean(Field)}), then attempts to resolve
     * via {@link Class#getMethod(String, Class[])}. Swallows {@link NoSuchMethodException} if getter
     * does not exist.
     * </p>
     *
     * @param field field to find getter for
     * @param entityClass entity class to search for getter method
     * @param methods list to add found getter method to
     */
    private void addGetterIfExists(Field field, Class<SearchableEntity> entityClass, List<Method> methods){
        try {
            String prefix = reflectionHelper.isBoolean(field) ? "is" : "get";
            Method m = entityClass.getMethod(prefix + capitalize(field.getName()), null);
            methods.add(m);
        } catch (NoSuchMethodException e) {
            //do nothing
        }
    }

    /**
     * Generates lowercased entity name prefix for method and hint formatting.
     * <p>
     * Extracts short name via {@link ReflectionHelper#getShortName(Class)}, converts to lowercase,
     * and removes timestamp suffix by splitting on underscore and taking the first token. This
     * produces consistent entity prefixes for data model generation (e.g., "User" → "user").
     * </p>
     *
     * @param entityClass entity class to generate prefix from
     * @return lowercased short name without timestamp suffix (e.g., "user", "organization")
     */
    private String getPrefix(Class<SearchableEntity> entityClass){
        return reflectionHelper.getShortName(entityClass).toLowerCase()
                //remove timestamp from entity name
                .split("_")[0];
    }

    /**
     * Extracts and concatenates all {@link AiHint} annotations from entity class fields.
     * <p>
     * Iterates through declared fields and collects {@link AiHint} annotation values, prefixing
     * each with "hint for [entityPrefix]: " to provide additional context guidance for AI prompt
     * generation. Hints can describe field semantics, validation rules, or business constraints.
     * </p>
     *
     * @param entityClass entity class to extract AI hints from
     * @return concatenated hint strings with entity prefix, one per line, empty string if no hints present
     * @see AiHint annotation for field-level AI context hints
     */
    private String getAllHints(Class<SearchableEntity> entityClass) {
        debug("[getAllHints]");
        List<String> hints = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            AiHint hint = field.getDeclaredAnnotation(AiHint.class);
            if(hint != null) {
                hints.add("hint for " + getPrefix(entityClass) + ": " + hint.value());
            }

        }
        return String.join("\n", hints);
    }

    /**
     * Generates relationship descriptions for JPA association fields.
     * <p>
     * Identifies fields annotated with JPA relationship annotations (ManyToOne, ManyToMany, OneToOne)
     * via {@link #getRelatedFields(Class)} and generates natural language descriptions formatted as
     * "Know that [entityPrefix] is assigned to object with repository name [fieldName]". Provides
     * relational context for AI understanding of entity associations.
     * </p>
     *
     * @param entityClass entity class to extract relationship information from
     * @return newline-separated relationship descriptions, empty string if no relationships found
     * @see #getRelatedFields(Class) for relationship field detection
     */
    private String getRelated(Class<SearchableEntity> entityClass) {
        debug("[getRelated]");
        List<String> result = new ArrayList<>();
        for (Field field : getRelatedFields(entityClass)) {
            result.add("Know that " + getPrefix(entityClass) + " is assigned to object with repository name " + field.getName());
        }
        return String.join("\n", result);
    }

    /**
     * Identifies fields annotated with JPA relationship annotations.
     * <p>
     * Filters declared fields for those annotated with ManyToOne, ManyToMany, or OneToOne annotations
     * by checking if any declared annotation type name contains these strings. Uses string matching
     * rather than direct annotation type comparison for flexibility with JPA provider variations.
     * </p>
     *
     * @param entityClass entity class to scan for relationship fields
     * @return list of fields representing JPA associations, empty list if no relationships found
     */
    private List<Field> getRelatedFields(Class<SearchableEntity> entityClass) {
        debug("[getRelatedFields]");
        return stream(entityClass.getDeclaredFields())
                .filter(field -> stream(field.getDeclaredAnnotations())
                        .anyMatch(annotation -> StringUtils.indexOfAny(annotation.annotationType().getName(), new String[]{"ManyToOne", "ManyToMany", "OneToOne"}) > -1))
                .collect(Collectors.toList());
    }
}
