package com.openkoda.service.autocomplete;


import com.openkoda.model.component.ServerJs;
import com.openkoda.uicomponent.live.LiveComponentProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 * Provides autocomplete suggestions for the web endpoint code editor.
 * <p>
 * This service enhances the code editing experience by offering context-aware suggestions
 * for web endpoint controllers. It aggregates multiple sources of autocomplete data including
 * service access via the Services aggregator, repository queries, Flow pipeline operations,
 * model properties, PageModelMap keys, and HTTP response helpers.

 * <p>
 * The service supports code snippet insertion patterns for common operations such as:
 * <ul>
 * <li>Accessing services through the Services aggregator (services.organizationService, services.userService)</li>
 * <li>Repository query methods (repositories.secure.organization.findAll())</li>
 * <li>Flow pipeline composition (Flow.init().thenSet())</li>
 * <li>Model properties and PageModelMap keys (organizationEntityId, userEntityId)</li>
 * <li>HTTP response helpers and controller utilities</li>
 * <li>ServerJs module imports and function suggestions</li>
 * </ul>

 * <p>
 * This service is a stateless singleton managed by Spring's service component scanning.
 * It extends GenericAutocompleteService to inherit base reflection and method extraction
 * capabilities, and depends on repositories and services via ComponentProvider.

 *
 * @see WebendpointAutocompleteResponse
 * @see GenericAutocompleteService
 * @see com.openkoda.service.Services
 * // LiveComponentProvider
 * @see ServerJs
 * // JsParser
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
@Service
public class WebendpointAutocompleteService extends GenericAutocompleteService {

    /**
     * Generates a comprehensive autocomplete response for web endpoint code editors.
     * <p>
     * This method aggregates autocomplete data from multiple sources to provide a rich
     * editing experience. The response includes service suggestions from LiveComponentProvider,
     * model property keys for common entities, ServerJs module import suggestions, and
     * function signatures from ServerJs modules.

     * <p>
     * The response is constructed fresh on each invocation without caching, ensuring
     * up-to-date suggestions that reflect the current state of ServerJs modules and
     * available services.

     *
     * @return populated WebendpointAutocompleteResponse containing service suggestions,
     *         model keys (organizationEntityId, userEntityId), ServerJs import suggestions,
     *         and ServerJs function mappings
     * @see WebendpointAutocompleteResponse
     * @see #getSuggestionsAndDocumentation()
     * @see #getImportSuggestions()
     * @see #getServerJsSuggestions()
     */
    public WebendpointAutocompleteResponse getResponse() {
        WebendpointAutocompleteResponse response = new WebendpointAutocompleteResponse();
        response.setServicesSuggestions(getSuggestionsAndDocumentation());
        response.setModelKeys(new String[]{"organizationEntityId","userEntityId"});
        response.setImportSuggestions(getImportSuggestions());
        response.setServerJsSuggestions(getServerJsSuggestions());
        return response;
    }
    
    /**
     * Extracts service suggestions and documentation from LiveComponentProvider fields.
     * <p>
     * This method uses reflection to discover all service interfaces exposed through
     * LiveComponentProvider, then processes each field to extract method suggestions
     * with their documentation. The reflection is performed on
     * {@code LiveComponentProvider.class.getDeclaredFields()}, and each field's type
     * is analyzed to generate autocomplete entries.

     * <p>
     * The method employs stream processing to flatten the field-to-suggestions mapping
     * into a single map where keys are fully-qualified method names and values are
     * their documentation strings.

     *
     * @return Map of service method suggestions where keys are method names prefixed
     *         with their service field name (e.g., "dataServices.getData()") and values
     *         are method documentation strings extracted via reflection
     * // LiveComponentProvider
     * @see #getSuggestionsAndDocumentation(Map, String)
     * @see #getExposedMethods(String)
     */
    private Map<String, String> getSuggestionsAndDocumentation(){
        return stream(LiveComponentProvider.class.getDeclaredFields())
                .map(f -> getSuggestionsAndDocumentation(getExposedMethods(f.getType().getName()), f.getName()))
                .flatMap(map -> map.entrySet().stream())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Retrieves ServerJs module names for import statement autocomplete suggestions.
     * <p>
     * This method queries the repository to fetch all available ServerJs modules and
     * extracts their names for use as import suggestions in the code editor. The query
     * is executed via {@code repositories.secure.serverJs.findAll()} and results are
     * streamed and mapped to module names.

     * <p>
     * Example usage in code editor: User types "import " and receives suggestions like
     * "utils", "validators", "transformers" based on available ServerJs modules.

     *
     * @return Array of ServerJs module names available for import, extracted from
     *         all ServerJs entities via repository query
     * @see ServerJs
     * @see com.openkoda.repository.ServerJsRepository
     */
    private String[] getImportSuggestions() {
        return repositories.secure.serverJs.findAll().stream().map(ServerJs::getName).toArray(String[]::new);
    }
    
    /**
     * Generates function signature suggestions for ServerJs modules.
     * <p>
     * This method retrieves all ServerJs modules from the repository and analyzes each
     * module's code to extract function signatures. It integrates with
     * {@code services.jsParser.getFunctions()} to parse JavaScript code and identify
     * available functions within each module.

     * <p>
     * The returned map enables the code editor to provide function-level autocomplete
     * after a module is imported. For example, after importing "utils", the editor can
     * suggest functions like "formatDate()", "validateEmail()", etc.

     *
     * @return Map where keys are ServerJs module names and values are lists of function
     *         names extracted from the module's code via JsParser integration
     * @see ServerJs
     * // JsParser
     * @see com.openkoda.service.Services#jsParser
     */
    private Map<String, List<String>> getServerJsSuggestions(){
        return repositories.secure.serverJs.findAll().stream().collect(Collectors.toMap(ServerJs::getName, s -> services.jsParser.getFunctions(s.getCode())));
    }
}
