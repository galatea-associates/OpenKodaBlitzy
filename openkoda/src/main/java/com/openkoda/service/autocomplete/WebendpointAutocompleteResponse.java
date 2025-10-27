package com.openkoda.service.autocomplete;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for web endpoint code editor autocomplete functionality.
 * <p>
 * This class provides autocomplete suggestions for the web endpoint code editor, including
 * service method suggestions, model keys, import options, and ServerJs function lists.
 * The response is designed to help developers write endpoint code with context-aware
 * suggestions for available services, repositories, Flow operations, and code templates.
 * </p>
 * <p>
 * The response structure includes four main sections:
 * <ul>
 *   <li>Service suggestions - Available service methods from LiveComponentProvider</li>
 *   <li>Model keys - PageModelMap keys for data binding</li>
 *   <li>Import suggestions - Available ServerJs modules</li>
 *   <li>ServerJs suggestions - Function lists for each ServerJs module</li>
 * </ul>
 * </p>
 * <p>
 * This DTO is typically serialized to JSON for REST API responses and consumed by
 * the web endpoint code editor to provide intelligent autocomplete and code assistance.
 * All fields use direct references (mutable) and should be treated as request-scoped objects.
 * </p>
 * <p>
 * Example usage in endpoint code editor:
 * <pre>
 * WebendpointAutocompleteResponse response = autocompleteService.getResponse();
 * Map&lt;String,String&gt; services = response.getServicesSuggestions();
 * </pre>
 * </p>
 *
 * @see com.openkoda.service.autocomplete.WebendpointAutocompleteService
 * @see com.openkoda.service.Services
 * @see com.openkoda.uicomponent.live.LiveComponentProvider
 * @see com.openkoda.model.file.ServerJs
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class WebendpointAutocompleteResponse {

    /**
     * Map of service method suggestions to their documentation.
     * <p>
     * Contains suggestions extracted from LiveComponentProvider fields, providing
     * autocomplete options for service methods available in the endpoint context.
     * Keys are formatted method signatures with parameter names, and values are
     * documentation strings from the @Autocomplete annotation.
     * </p>
     * <p>
     * Example entries: {@code "userService.findById(userId)" -> "Finds user by ID"}
     * </p>
     */
    Map<String,String> servicesSuggestions;
    
    /**
     * Array of available model keys for PageModelMap data binding.
     * <p>
     * Contains predefined keys that can be used in the PageModelMap for storing
     * and retrieving data in web endpoint flows. Common keys include entity IDs
     * (organizationEntityId, userEntityId), result objects, and flow parameters.
     * </p>
     * <p>
     * Example values: {@code ["organizationEntityId", "userEntityId", "currentUser"]}
     * </p>
     */
    String[] modelKeys;
    
    /**
     * Array of ServerJs module names available for import.
     * <p>
     * Lists all ServerJs modules that can be imported in the web endpoint code editor.
     * These modules are retrieved from the ServerJs repository and represent reusable
     * JavaScript code libraries that extend endpoint functionality.
     * </p>
     * <p>
     * Example values: {@code ["CommonUtils", "DataValidator", "EmailHelper"]}
     * </p>
     */
    String[] importSuggestions;
    
    /**
     * Map of ServerJs module names to their available function lists.
     * <p>
     * Provides method-level autocomplete suggestions for each ServerJs module. Keys
     * are module names, and values are lists of function names extracted from the
     * module's JavaScript code using the JsParser service. This enables intelligent
     * autocomplete for imported ServerJs modules.
     * </p>
     * <p>
     * Example structure: {@code {"CommonUtils" -> ["formatDate", "validateEmail"]}}
     * </p>
     */
    Map<String, List<String>> serverJsSuggestions;

    /**
     * Gets the service method suggestions map.
     * <p>
     * Returns a map containing service method signatures as keys and their
     * documentation as values. The map provides direct reference (mutable).
     * </p>
     *
     * @return map of service method suggestions to documentation, may be null
     */
    public Map<String, String> getServicesSuggestions() {
        return servicesSuggestions;
    }

    /**
     * Sets the service method suggestions map.
     * <p>
     * Stores the map containing service method signatures and their documentation.
     * The map is stored as a direct reference without defensive copying.
     * </p>
     *
     * @param servicesSuggestions map of service method suggestions to documentation, may be null
     */
    public void setServicesSuggestions(Map<String, String> servicesSuggestions) {
        this.servicesSuggestions = servicesSuggestions;
    }

    /**
     * Gets the array of available PageModelMap keys.
     * <p>
     * Returns an array containing predefined keys for data binding in web endpoint flows.
     * The array is returned as a direct reference (mutable).
     * </p>
     *
     * @return array of model keys for PageModelMap, may be null
     */
    public String[] getModelKeys() {
        return modelKeys;
    }

    /**
     * Sets the array of available PageModelMap keys.
     * <p>
     * Stores the array containing predefined keys for data binding in web endpoint flows.
     * The array is stored as a direct reference without defensive copying.
     * </p>
     *
     * @param modelKeys array of model keys for PageModelMap, may be null
     */
    public void setModelKeys(String[] modelKeys) {
        this.modelKeys = modelKeys;
    }

    /**
     * Gets the array of available ServerJs module names for import.
     * <p>
     * Returns an array containing names of ServerJs modules that can be imported
     * in the web endpoint code editor. The array is returned as a direct reference (mutable).
     * </p>
     *
     * @return array of ServerJs module names available for import, may be null
     */
    public String[] getImportSuggestions() {
        return importSuggestions;
    }

    /**
     * Sets the array of available ServerJs module names for import.
     * <p>
     * Stores the array containing names of ServerJs modules that can be imported
     * in the web endpoint code editor. The array is stored as a direct reference
     * without defensive copying.
     * </p>
     *
     * @param importSuggestions array of ServerJs module names available for import, may be null
     */
    public void setImportSuggestions(String[] importSuggestions) {
        this.importSuggestions = importSuggestions;
    }

    /**
     * Gets the map of ServerJs module names to their function lists.
     * <p>
     * Returns a map where keys are ServerJs module names and values are lists
     * of function names available in each module. This enables method-level
     * autocomplete for imported ServerJs modules. The map is returned as a
     * direct reference (mutable).
     * </p>
     *
     * @return map of ServerJs module names to function lists, may be null
     */
    public Map<String, List<String>> getServerJsSuggestions() {
        return serverJsSuggestions;
    }

    /**
     * Sets the map of ServerJs module names to their function lists.
     * <p>
     * Stores a map where keys are ServerJs module names and values are lists
     * of function names available in each module. The map is stored as a direct
     * reference without defensive copying.
     * </p>
     *
     * @param serverJsSuggestions map of ServerJs module names to function lists, may be null
     */
    public void setServerJsSuggestions(Map<String, List<String>> serverJsSuggestions) {
        this.serverJsSuggestions = serverJsSuggestions;
    }
}
