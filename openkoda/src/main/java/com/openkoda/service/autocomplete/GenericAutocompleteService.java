package com.openkoda.service.autocomplete;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.helper.ReflectionHelper;
import com.openkoda.uicomponent.annotation.Autocomplete;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 * Generic reflection-based autocomplete service that provides method suggestion and documentation
 * extraction for code editors and development tools.
 * <p>
 * This service centralizes reflection logic for discovering and formatting autocomplete suggestions
 * from annotated Java methods. It uses the {@link Autocomplete} annotation to identify methods that
 * should be exposed in autocomplete contexts, and formats them with parameter names and documentation
 * for presentation to users.
 * 
 * <p>
 * Reflection capabilities include:
 * <ul>
 * <li>Extracting public methods annotated with {@code @Autocomplete}</li>
 * <li>Generating formatted method signatures with parameter names</li>
 * <li>Retrieving documentation strings from annotation attributes</li>
 * <li>Supporting optional variable name prefixes for context-aware suggestions</li>
 * </ul>
 * 
 * <p>
 * The autocomplete format consists of suggestion keys (method signatures) mapped to documentation
 * values, suitable for JSON serialization and consumption by web-based code editors. When duplicate
 * method signatures occur, the merge strategy prefers the first non-empty documentation string.
 * 
 * <p>
 * Dependencies: Requires {@link ReflectionHelper} for reflection operations and relies on the
 * {@link Autocomplete} annotation contract for method discovery and documentation extraction.
 * 
 * <p>
 * Thread-safety: This class is designed to be used as a Spring-managed singleton. The injected
 * {@link ReflectionHelper} must be thread-safe for safe concurrent use. Methods are stateless
 * and side-effect-free.
 * 
 * <p>
 * Performance notes: Reflection operations are performed on-demand without caching. For performance-critical
 * scenarios, consider caching the results of {@link #getExposedMethods(String)} and
 * {@link #getSuggestionsAndDocumentation(Method[], String)}.
 * 
 *
 * @see ReflectionHelper
 * @see Autocomplete
 * @see java.lang.reflect.Method
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
class GenericAutocompleteService extends ComponentProvider {
    /**
     * Reflection utility for introspecting classes and methods.
     * <p>
     * Provides method discovery, signature formatting with parameter names, and other
     * reflection-based operations required for autocomplete suggestion generation.
     * 
     */
    @Autowired
    private ReflectionHelper helper;

    /**
     * Extracts autocomplete suggestions and documentation from an array of methods.
     * <p>
     * This method streams the provided methods and collects them into a map where keys are
     * formatted suggestion strings (method signatures) and values are documentation strings
     * from the {@link Autocomplete} annotation. An optional variable name prefix can be
     * prepended to each suggestion for context-aware autocomplete.
     * 
     * <p>
     * When duplicate suggestion keys occur (multiple methods with identical signatures), the
     * merge function preserves the first non-empty documentation string, discarding empty
     * documentation in favor of meaningful content.
     * 
     *
     * @param methods array of methods to process for autocomplete suggestions
     * @param variableName optional prefix to prepend to each suggestion (e.g., "user" results
     *                     in "user.methodName(...)"), or null for no prefix
     * @return map of suggestion strings to documentation strings, suitable for JSON serialization
     * @see #getSuggestion(String, Method)
     * @see #getDocumentation(Method)
     */
    Map<String,String> getSuggestionsAndDocumentation(Method[] methods, String variableName){
        return stream(methods)
                .collect(toMap(m -> getSuggestion(variableName, m), this::getDocumentation, (m1, m2) -> !m1.equals("") ? m1 : m2 ));
    }
    
    /**
     * Formats a method as an autocomplete suggestion string with optional variable prefix.
     * <p>
     * Generates a formatted method signature including parameter names using {@link ReflectionHelper}.
     * If a variable name is provided, it is prepended with a dot separator to create context-aware
     * suggestions like "user.getName()" or "organization.getProperties()".
     * 
     * <p>
     * Null-handling: When variableName is null, only the method signature is returned without
     * any prefix.
     * 
     *
     * @param variableName optional prefix to prepend (e.g., "user"), or null for no prefix
     * @param method the method to format as a suggestion
     * @return formatted suggestion string in the form "variableName.methodName(param1, param2)"
     *         or "methodName(param1, param2)" if variableName is null
     * @see ReflectionHelper#getNameWithParamNames(Method)
     */
    String getSuggestion(String variableName, Method method){
        return (variableName != null ? variableName + "." : "") + helper.getNameWithParamNames(method);
    }
    
    /**
     * Retrieves documentation for a method from its {@link Autocomplete} annotation.
     * <p>
     * Extracts the {@code doc} attribute value from the method's {@code @Autocomplete} annotation.
     * This documentation string typically describes the method's purpose, behavior, or usage
     * examples for display in autocomplete tooltips or documentation panels.
     * 
     * <p>
     * Warning: This method assumes the {@link Autocomplete} annotation is present. A
     * {@link NullPointerException} will occur if called on a method without this annotation.
     * Callers should ensure methods are filtered by {@link #getExposedMethods(String)} before
     * invoking this method.
     * 
     *
     * @param method the method annotated with {@code @Autocomplete}
     * @return the documentation string from the annotation's {@code doc} attribute
     * @throws NullPointerException if the method is not annotated with {@link Autocomplete}
     * @see Autocomplete#doc()
     */
    String getDocumentation(Method method){
        return method.getAnnotation(Autocomplete.class).doc();
    }
    
    /**
     * Discovers methods annotated with {@link Autocomplete} in a specified class.
     * <p>
     * Uses reflection to retrieve all declared methods from the class identified by the fully-qualified
     * class name, then filters to include only those methods annotated with {@code @Autocomplete}.
     * This filtering logic ensures only methods explicitly marked for autocomplete exposure are
     * included in suggestion lists.
     * 
     * <p>
     * The returned array contains methods suitable for passing to
     * {@link #getSuggestionsAndDocumentation(Method[], String)} for autocomplete generation.
     * 
     *
     * @param className fully-qualified class name (e.g., "com.openkoda.service.UserService")
     * @return array of methods from the class that are annotated with {@link Autocomplete},
     *         or an empty array if no annotated methods are found
     * @see ReflectionHelper#getDeclaredMethods(String)
     * @see Autocomplete
     */
    Method[] getExposedMethods(String className){
        return stream(helper.getDeclaredMethods(className))
                .filter(f -> f.isAnnotationPresent(Autocomplete.class))
                .toArray(Method[]::new);
    }
}
