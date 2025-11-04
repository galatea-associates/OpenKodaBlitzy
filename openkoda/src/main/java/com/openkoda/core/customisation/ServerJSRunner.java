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

package com.openkoda.core.customisation;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.controller.common.PageAttributes;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.dto.system.ScheduledSchedulerDto;
import com.openkoda.model.component.ServerJs;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.output.NullWriter;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for executing server-side JavaScript code using GraalVM polyglot context.
 * <p>
 * This class provides infrastructure for running JavaScript code on the server with full
 * access to Java classes and OpenKoda services. It creates a GraalVM polyglot execution
 * context with unlimited host access privileges, enabling JavaScript code to invoke Java
 * methods, instantiate classes, and interact with Spring beans.
 * <p>
 * The runner integrates with {@link ServerJs} entities stored in the database, supporting
 * model deserialization, argument passing, and result type conversion. Scripts execute with
 * access to OpenKoda services via the {@code process} binding and can manipulate model data
 * through the {@code model} binding.
 * <p>
 * Example usage:
 * <pre>{@code
 * ServerJs serverJs = repositories.unsecure.serverJs.findByName("myScript");
 * Map<String, Object> result = runner.evaluateServerJs(serverJs, null, null, Map.class);
 * }</pre>
 * <p>
 * <strong>Thread Safety:</strong> Each script evaluation creates a new GraalVM Context instance,
 * ensuring thread-safe execution. However, the contextBuilder is shared across invocations.
 * <p>
 * <strong>Security Note:</strong> This runner uses {@code allowAllAccess(true)} and
 * {@code allowHostClassLookup} with no restrictions, granting scripts unrestricted access
 * to Java classes and system resources. Only execute trusted scripts.
 *
 * @see ServerJs
 * @see ServerJSProcessRunner
 * @see com.openkoda.core.customisation.BasicCustomisationService
 * @author OpenKoda Team
 * @since 1.7.1
 */
@Service
public class ServerJSRunner extends ComponentProvider {

    /**
     * GraalVM Context.Builder configured for JavaScript execution with unlimited host access privileges.
     * <p>
     * Configuration details:
     * <ul>
     * <li>{@code allowHostAccess(HostAccess.ALL)} - Permits JavaScript to access all public Java methods and fields</li>
     * <li>{@code allowAllAccess(true)} - Grants unrestricted access to host resources and polyglot interoperability</li>
     * <li>{@code allowHostClassLoading(true)} - Enables JavaScript to load Java classes dynamically</li>
     * <li>{@code allowHostClassLookup} - Allows lookup of any Java class by name without restrictions</li>
     * </ul>
     * 
     * <p>
     * This permissive configuration enables scripts to instantiate Java objects, call static methods,
     * and interact with Spring-managed beans. Each invocation of {@link #evaluateScript} creates a
     * new Context from this builder to ensure thread isolation.
     * 
     *
     * @see Context.Builder
     */
    private Context.Builder contextBuilder = Context.newBuilder("js")
            .allowHostAccess(HostAccess.ALL)
            .allowAllAccess(true)
            .allowHostClassLoading(true)
            .allowHostClassLookup(className -> true);  //allows access to all Java classes


    /**
     * Executes a server-side JavaScript script with provided model and arguments.
     * <p>
     * This method creates a temporary {@link ServerJs} instance to deserialize the JSON model
     * into a Map, then evaluates the script within a GraalVM context. The model and arguments
     * are bound to the JavaScript execution environment, accessible via {@code model} and
     * {@code arguments} variables.
     * 
     * <p>
     * Example:
     * <pre>{@code
     * String result = (String) runner.startServerJs("return model.name;", "{\"name\":\"test\"}", null, writer);
     * }</pre>
     * 
     *
     * @param script the JavaScript code to execute, must not be null
     * @param model JSON string representing a map to be provided as the {@code model} variable
     *              in the script execution context, may be null
     * @param arguments newline-separated list of arguments provided as the {@code arguments}
     *                  variable in the script execution context, may be null
     * @param log Writer for capturing script execution logs, may be null (uses NullWriter internally)
     * @return the object returned by the script execution, or a Map containing error details if execution fails
     */
    public Object startServerJs(String script, String model, String arguments, Writer log) {
        try {
            //we create temporary serverJs instance to deserialize model into map
            //that can be improved
            ServerJs serverJs = new ServerJs(script, model, arguments);
            return evaluateScript(script, serverJs.getModelMap(), Object.class, log);
        } catch (Exception e) {
            return Collections.singletonMap(PageAttributes.error.name, e.getMessage());
        } finally {
            try {
                if(log != null) {
                    log.close();
                }
            } catch (IOException e) {
                error("[startServerJs]", e);
            }
        }
    }

    /**
     * Executes a server-side JavaScript script in response to a scheduler event.
     * <p>
     * This method is triggered by {@link com.openkoda.core.service.event.ApplicationEvent#SCHEDULER_EXECUTED}
     * events. It validates that the event's data matches the expected {@code schedulerData} parameter
     * before retrieving and executing the named ServerJs script. The scheduler event data and additional
     * arguments are passed to the script execution context.
     * 
     * <p>
     * Event consumers in {@link com.openkoda.core.customisation.BasicCustomisationService} register
     * this method to handle scheduled JavaScript execution.
     * 
     *
     * @param dto the {@link ScheduledSchedulerDto} containing event metadata including {@code eventData}
     * @param schedulerData the expected event data value to match against {@code dto.eventData}, exits
     *                      early if values do not match
     * @param serverJsName the name of the ServerJs entity to retrieve and execute, must exist in database
     * @param argument1 first custom argument passed to the script execution context, may be null
     * @param argument2 second custom argument passed to the script execution context, may be null
     * @return the object returned by the script execution, or null if event data does not match
     * @see com.openkoda.core.customisation.BasicCustomisationService
     */
    public Object startScheduledServerJs(ScheduledSchedulerDto dto, String schedulerData, String serverJsName, String argument1, String argument2) {
        if (not(StringUtils.equals(dto.eventData, schedulerData))) {
            debug("[startScheduledServerJs] not this event. Exiting");
            return null;
        }
        ServerJs serverJs = repositories.unsecure.serverJs.findByName(serverJsName);
        return evaluateServerJsScript(serverJs, null, Arrays.asList(schedulerData, serverJsName, argument1, argument2), Object.class);
    }

    /**
     * Executes a server-side JavaScript script with {@link CustomisationService} integration.
     * <p>
     * This method retrieves a named ServerJs entity and executes it with custom arguments,
     * exposing the full OpenKoda service layer to the script through bindings. The
     * {@code startDateTime} parameter can be used by calling code to track execution timing,
     * though it is not directly passed to the script.
     * 
     * <p>
     * Event consumers registered in {@link com.openkoda.core.customisation.BasicCustomisationService}
     * use this method to execute customisation scripts with access to services.
     * 
     *
     * @param startDateTime the timestamp when execution was initiated, typically used for logging or tracking
     * @param serverJsName the name of the ServerJs entity to retrieve and execute, must exist in database
     * @param argument1 first custom argument passed to the script execution context, may be null
     * @param argument2 second custom argument passed to the script execution context, may be null
     * @param argument3 third custom argument passed to the script execution context, may be null
     * @return the object returned by the script execution
     * @see CustomisationService
     * @see com.openkoda.core.customisation.BasicCustomisationService
     */
    public Object startCustomisationServerJs(LocalDateTime startDateTime, String serverJsName, String argument1, String argument2, String argument3) {
        ServerJs serverJs = repositories.unsecure.serverJs.findByName(serverJsName);
        return evaluateServerJsScript(serverJs, null, Arrays.asList(serverJsName, argument1, argument2, argument3), Object.class);
    }

    /**
     * Evaluates a server-side JavaScript script by name with external model data and arguments.
     * <p>
     * This convenience method retrieves the ServerJs entity by name and delegates to
     * {@link #evaluateServerJsScript(ServerJs, Map, List, Class)} for execution.
     * 
     *
     * @param <T> the expected return type of the script execution
     * @param serverJsName the name of the ServerJs entity to retrieve and execute, must exist in database
     * @param externalModel additional model data merged into the ServerJs default model, may be null;
     *                      external entries override default model values with matching keys
     * @param externalArguments additional arguments merged with ServerJs default arguments, may be null;
     *                          external arguments override default values
     * @param resultType the Java class representing the expected script return type, used for type conversion
     * @return the script execution result converted to the specified type
     * @throws RuntimeException if the ServerJs entity is not found or script execution fails
     */
    public <T> T evaluateServerJsScript(String serverJsName, Map<String, Object> externalModel, List<String> externalArguments, Class<T> resultType) {
        return evaluateServerJsScript(repositories.unsecure.serverJs.findByName(serverJsName), externalModel, externalArguments, resultType);
    }

    /**
     * Evaluates a server-side JavaScript script with external model data and arguments.
     * <p>
     * This method is an alias for {@link #evaluateServerJsScript(ServerJs, Map, List, Class)},
     * providing a shorter method name for convenience.
     * 
     *
     * @param <T> the expected return type of the script execution
     * @param serverJs the ServerJs entity containing script code and default model/arguments, must not be null
     * @param externalModel additional model data merged into the ServerJs default model, may be null
     * @param externalArguments additional arguments merged with ServerJs default arguments, may be null
     * @param resultType the Java class representing the expected script return type, used for type conversion
     * @return the script execution result converted to the specified type
     * @throws RuntimeException if script execution fails
     * @see #evaluateServerJsScript(ServerJs, Map, List, Class)
     */
    public <T> T evaluateServerJs(ServerJs serverJs, Map<String, Object> externalModel, List<String> externalArguments, Class<T> resultType) {
        return evaluateServerJsScript(serverJs, externalModel, externalArguments, resultType);
    }

    /**
     * Executes a server-side JavaScript script with merged model and arguments.
     * <p>
     * This method performs the complete ServerJs execution workflow:
     * 
     * <ol>
     * <li>Extracts script code from the ServerJs entity</li>
     * <li>Deserializes the ServerJs model into a {@link PageModelMap}</li>
     * <li>Merges external model data, overriding default model values</li>
     * <li>Merges external arguments, replacing default arguments if provided</li>
     * <li>Delegates to {@link #evaluateScript} for GraalVM context execution</li>
     * </ol>
     * <p>
     * Example complete flow:
     * <pre>{@code
     * Map<String, Object> customData = Map.of("userId", 123);
     * Integer result = runner.evaluateServerJsScript(serverJs, customData, null, Integer.class);
     * }</pre>
     * 
     *
     * @param <T> the expected return type of the script execution
     * @param serverJs the ServerJs entity containing script code, model JSON, and arguments, must not be null
     * @param externalModel additional model data merged into the ServerJs default model, may be null;
     *                      external entries override default model values with matching keys
     * @param externalArguments additional arguments replacing ServerJs default arguments if non-empty, may be null
     * @param resultType the Java class representing the expected script return type, used for GraalVM type conversion
     * @return the script execution result converted to the specified type
     * @throws RuntimeException if serverJs is null or script execution fails
     */
    private <T> T evaluateServerJsScript(
            ServerJs serverJs,
            Map<String, Object> externalModel,
            List<String> externalArguments,
            Class<T> resultType) {

        if (serverJs == null) {
            error("[evaluateServerJsScript] ServerJs is null");
            throw new RuntimeException("No entry");
        }

        String script = serverJs.getCode();
        try {
            PageModelMap map = serverJs.getModelMap();
            if (externalModel != null) {
                map.putAll(externalModel);
            }
            if (CollectionUtils.isNotEmpty(externalArguments)) {
                map.put(arguments, externalArguments);
            }
            return evaluateScript(script, map, resultType, null);
        } catch (Exception e) {
            error(e, "[evaluateServerJsScript] When evaluating {} : {}", serverJs.getName(),
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            throw new RuntimeException("Error when evaluating js", e);
        }
    }


    /**
     * Evaluates JavaScript code within a GraalVM polyglot context with injected bindings.
     * <p>
     * This method creates a new GraalVM Context for each invocation, ensuring thread-safe
     * script execution. It injects three categories of bindings into the JavaScript environment:
     * 
     * <ul>
     * <li><strong>Script bindings</strong> - Custom model data passed as {@code bindings} parameter,
     *     accessible by key name in JavaScript (e.g., {@code bindings.put("userId", 123)} becomes {@code userId} variable)</li>
     * <li><strong>Resource bindings</strong> - Static resources from {@link ComponentProvider#resources},
     *     providing access to shared OpenKoda components</li>
     * <li><strong>Model binding</strong> - The complete bindings map exposed as {@code model} variable</li>
     * <li><strong>Process binding</strong> - A {@link ServerJSProcessRunner} instance exposed as {@code process}
     *     variable, providing access to services and logging capabilities</li>
     * </ul>
     * <p>
     * The Context is created from {@link #contextBuilder} with unrestricted host access, allowing
     * JavaScript code to invoke Java methods and instantiate classes.
     * 
     * <p>
     * <strong>Thread Safety:</strong> A new Context is created for each invocation, providing
     * thread isolation for concurrent script executions.
     * 
     *
     * @param <T> the expected return type of the script execution
     * @param script the JavaScript source code to execute, must not be null
     * @param bindings a map of key-value pairs to inject as JavaScript variables, must not be null
     * @param resultType the Java class for converting the script return value, must not be null
     * @param log Writer for capturing process logs, may be null (uses NullWriter if null)
     * @return the script execution result converted to the specified type
     * @see ServerJSProcessRunner
     * @see ComponentProvider#resources
     */
    //TODO: this method is to be redesigned
    private <T> T evaluateScript(String script, Map<String, Object> bindings, Class<T> resultType, Writer log) {
        Context c = contextBuilder.build();
        Value b = c.getBindings("js");
        for (Map.Entry<String, Object> o : bindings.entrySet()) {
            b.putMember(o.getKey(), o.getValue());
        }
        for (Map.Entry<String, Object> o : ComponentProvider.resources.entrySet()) {
            b.putMember(o.getKey(), o.getValue());
        }
        b.putMember("model", bindings);
        b.putMember("process", new ServerJSProcessRunner(services, log == null ? new NullWriter() : log));
        T result = c.eval("js", script).as(resultType);
        return result;
    }

}
