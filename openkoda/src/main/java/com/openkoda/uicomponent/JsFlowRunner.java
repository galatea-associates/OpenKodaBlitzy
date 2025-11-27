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

package com.openkoda.uicomponent;

import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.flow.form.JsFlow;
import com.openkoda.core.flow.form.JsResultAndModel;
import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.repository.SecureServerJsRepository;
import com.openkoda.service.map.MapService;
import com.openkoda.uicomponent.live.LiveComponentProvider;
import com.vividsolutions.jts.geom.Point;
import jakarta.inject.Inject;
import org.graalvm.polyglot.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.openkoda.controller.common.PageAttributes.*;

/**
 * Executes JavaScript flow definitions in GraalVM polyglot Context, enabling server-side JavaScript flows with access to Java service beans.
 * <p>
 * This component creates isolated GraalVM Context instances for evaluating JavaScript code that defines Flow pipelines.
 * Each evaluation occurs in a fresh Context with the following capabilities:

 * <ul>
 *   <li>Binds Java service objects (LiveComponentProvider) to JavaScript namespace</li>
 *   <li>Binds utility functions (date/time parsing, JSON, URI encoding, geometry parsing) as JavaScript functions</li>
 *   <li>Evaluates JavaScript code with access to Flow and JsResultAndModel</li>
 *   <li>Converts JavaScript return values to Java Flow instances</li>
 *   <li>Executes Flow to produce PageModelMap result</li>
 *   <li>Supports preview mode (exception-safe) and live mode (exception-propagating)</li>
 * </ul>
 * <p>
 * <strong>Security Note:</strong> Context is configured with allowAllAccess=true and allowHostClassLookup=true.
 * JavaScript code has broad host access, so ensure script sources are trusted.

 * <p>
 * <strong>Thread-safety Note:</strong> Context instances are not thread-safe. This component creates a fresh
 * Context per evaluation call to ensure thread safety.

 * <p>
 * Example JavaScript flow:
 * <pre>{@code
 * flow.thenSet("x", a -> 5)
 *     .thenSet("result", a -> a.result("x") * 2)
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.graalvm.polyglot.Context
 * @see Flow
 * @see JsFlow
 * @see JsResultAndModel
 * @see LiveComponentProvider
 * @see FileSystemImpl
 */
@Component
public class JsFlowRunner {

    /**
     * Default Context.Builder pre-configured with JavaScript engine and permissive access policies.
     * <p>
     * Configuration includes:

     * <ul>
     *   <li>allowHostAccess=ALL - JavaScript can access Java objects</li>
     *   <li>allowAllAccess=true - Enables all polyglot capabilities</li>
     *   <li>allowHostClassLoading=true - JavaScript can load Java classes</li>
     *   <li>allowHostClassLookup=true - JavaScript can lookup Java class by name</li>
     * </ul>
     * <p>
     * <strong>Note:</strong> Currently unused, as evaluateJsFlow creates a fresh Context.Builder per call.

     */
    private Context.Builder contextBuilder = Context.newBuilder("js")
            .allowHostAccess(HostAccess.ALL)
            .allowAllAccess(true)
            .allowHostClassLoading(true)
            .allowHostClassLookup(className -> true);

    /**
     * LiveComponentProvider aggregating service beans for JavaScript flow access.
     * <p>
     * Injected and bound to JavaScript context as utility function sources, providing:

     * <ul>
     *   <li>util.dateNow() - Current LocalDate</li>
     *   <li>util.dateTimeNow() - Current LocalDateTime</li>
     *   <li>util.parseInt/parseLong/parseFloat - String to numeric conversions</li>
     *   <li>util.parseDate/parseTime - String to temporal conversions</li>
     *   <li>util.parseJSON/toJSON - JSON parsing and serialization</li>
     *   <li>util.encodeURI/decodeURI - URI encoding utilities</li>
     * </ul>
     * <p>
     * Used in both preview and live mode flows.

     */
    @Inject
    private LiveComponentProvider componentProvider;

    /**
     * Optional preview mode component provider for restricted service access during development.
     * <p>
     * Provides a limited set of services for safe preview execution in form designers,
     * preventing unintended side effects during flow testing.

     * <p>
     * <strong>Note:</strong> Required=false - may be null if preview mode is not configured.
     * Used exclusively by {@link #runPreviewFlow}.

     */
    @Autowired(required = false)
    private PreviewComponentProviderInterface previewComponentProviderInterface;

    /**
     * Repository for querying ServerJs entities containing JavaScript code.
     * <p>
     * <strong>Note:</strong> Currently unused in this class but available for potential
     * future direct script loading from database.

     */
    @Autowired
    private SecureServerJsRepository serverJsRepository;
    
    /**
     * JavaScript parser for extracting function signatures and analyzing code structure.
     * <p>
     * <strong>Note:</strong> Currently unused in this class but available for potential
     * future script introspection and validation capabilities.

     */
    @Autowired
    private JsParser jsParser;
    
    /**
     * FileSystemImpl bound to Context for JavaScript import statement support.
     * <p>
     * Enables JavaScript imports by resolving import paths to ServerJs entities.
     * Example: {@code import {fn} from 'path'} resolves via FileSystemImpl to
     * stored ServerJs code.

     * <p>
     * Used in evaluateJsFlow with Context.Builder.fileSystem(fileSystemImp)
     * and allowIO=true configuration.

     */
    @Inject
    private FileSystemImpl fileSystemImp;

    /**
     * Creates GraalVM Context, binds service objects and utility functions, evaluates JavaScript code, returns Flow instance.
     * <p>
     * This method performs the following steps:

     * <ol>
     *   <li>Creates Context with fileSystemImp (for JavaScript imports)</li>
     *   <li>Enables allowIO=true, allowHostAccess=ALL, allowAllAccess=true, allowHostClassLoading=true</li>
     *   <li>Binds 'flow' variable to initializedFlow</li>
     *   <li>Binds 'context' variable to initialResultAndModel</li>
     *   <li>Binds utility functions: dateNow, dateTimeNow, parseInt, parseLong, parseDate, parseTime, toString, isNaN, parseFloat, parseJSON, parsePoint, toJSON, decodeURI, encodeURI</li>
     *   <li>Rewrites script: prepends 'let result = ' to capture flow, appends ';\\nresult' to return flow</li>
     *   <li>Evaluates via c.eval(Source) and converts result to Flow using .as(Flow.class)</li>
     * </ol>
     * <p>
     * Example transformation:

     * <pre>{@code
     * Input:  "flow.thenSet(\"x\", a -> 5)"
     * Output: "let result = flow.thenSet(\"x\", a -> 5);\nresult"
     * }</pre>
     * <p>
     * <strong>Note:</strong> Context lifecycle - created per call, used once, eligible for GC after return.
     * This ensures thread safety and prevents context pollution between evaluations.

     *
     * @param jsFlow JavaScript source code to evaluate (Flow definition)
     * @param initializedFlow Initial Flow instance bound to 'flow' variable in JavaScript
     * @param initialResultAndModel JsResultAndModel bound to 'context' variable
     * @param scriptSourceFileName Source filename for error reporting and debugging
     * @return Flow instance extracted from JavaScript evaluation result
     * @throws RuntimeException If script evaluation fails with IOException
     */
    private Flow evaluateJsFlow(String jsFlow, Flow initializedFlow, JsResultAndModel initialResultAndModel, String scriptSourceFileName) {
        Context c = Context.newBuilder("js")
                .fileSystem(fileSystemImp)
                .allowIO(true)
                .allowHostAccess(HostAccess.ALL)
                .allowAllAccess(true)
                .allowHostClassLoading(true)
                .allowHostClassLookup(className -> true)
                .build();
        Value b = c.getBindings("js");
        b.putMember("flow", initializedFlow);
        b.putMember("context", initialResultAndModel);
        b.putMember("dateNow", (Supplier<LocalDate>) () -> componentProvider.util.dateNow());
        b.putMember("dateTimeNow", (Supplier<LocalDateTime>) () -> componentProvider.util.dateTimeNow());
        b.putMember("parseInt", (Function<String, Integer>) s -> componentProvider.util.parseInt(s));
        b.putMember("parseLong", (Function<String, Long>) s -> componentProvider.util.parseLong(s));
        b.putMember("parseDate", (Function<String, LocalDate>) s -> componentProvider.util.parseDate(s));
        b.putMember("parseTime", (Function<String, LocalTime>) s -> componentProvider.util.parseTime(s));
        b.putMember("toString", (Function<Object, String>) s -> componentProvider.util.toString(s));
        b.putMember("isNaN", (Function<Double, Boolean>) s -> componentProvider.util.isNaN(s));
        b.putMember("parseFloat", (Function<String, Float>) s -> componentProvider.util.parseFloat(s));
        b.putMember("parseJSON", (Function<String, JSONObject>) s -> componentProvider.util.parseJSON(s));
        b.putMember("parsePoint", (Function<String, Point>) s -> MapService.parsePoint(s));
        b.putMember("toJSON", (Function<Object, String>) s -> componentProvider.util.toJSON(s));
        b.putMember("decodeURI", (Function<String, String>) s -> componentProvider.util.decodeURI(s));
        b.putMember("encodeURI", (Function<String, String>) s -> componentProvider.util.encodeURI(s));

        String finalScript = jsFlow.replaceFirst("flow", "let result = flow");
        finalScript +=   ";\nresult";
        try {
            return c.eval(Source.newBuilder("js", finalScript, scriptSourceFileName).build()).as(initializedFlow.getClass());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Executes Flow with initial PageModelMap to produce result PageModelMap.
     * <p>
     * Simple delegation to Flow.execute(PageModelMap), included for clarity
     * and potential interception point for logging or monitoring.

     *
     * @param f Flow to execute
     * @param initialModel Initial PageModelMap with existing attributes
     * @return PageModelMap containing execution results and model attributes
     */
    private PageModelMap executeFlow(Flow f, PageModelMap initialModel) {
        return f.execute(initialModel);
    }

    /**
     * Executes JavaScript flow in preview mode with exception handling - returns error message on PolyglotException.
     * <p>
     * This method creates a JsFlow initialized with previewComponentProviderInterface (preview mode services),
     * sets organizationEntityId and userEntityId, creates JsResultAndModel, calls evaluateJsFlow to compile
     * JavaScript, and executes Flow via executeFlow. Catches PolyglotException and returns PageModelMap
     * with 'errorMessage' attribute instead of propagating the exception.

     * <p>
     * <strong>Use case:</strong> Preview mode for form designers - show errors instead of failing controller execution.
     * This enables developers to test flows safely without impacting production systems.

     *
     * @param jsFlow JavaScript source code defining flow logic
     * @param params Request parameters Map (String to String)
     * @param organizationId Organization ID for tenant context (may be null)
     * @param userId User ID for security context
     * @param form Optional AbstractOrganizationRelatedEntityForm (may be null)
     * @param scriptSourceFileName Source filename for error reporting
     * @return PageModelMap with execution results, or PageModelMap with 'errorMessage' attribute on PolyglotException
     * @see JsFlow
     * @see JsResultAndModel
     * @see PreviewComponentProviderInterface
     */
    public PageModelMap runPreviewFlow(String jsFlow, Map<String, String> params, Long organizationId, long userId, AbstractOrganizationRelatedEntityForm form, String scriptSourceFileName) {
        Flow f = JsFlow.init(previewComponentProviderInterface, params, form)
                .thenSet(organizationEntityId, userEntityId, a -> Tuples.of(organizationId, userId));

        try {
            JsResultAndModel initialResultAndModel = JsResultAndModel.constructNew(componentProvider, params, form);
            return executeFlow(evaluateJsFlow(jsFlow, f, initialResultAndModel, scriptSourceFileName), initialResultAndModel.model);
        } catch (PolyglotException e) {
            PageModelMap pageModelMap = new PageModelMap();
            pageModelMap.put(errorMessage, e.getMessage());
            return pageModelMap;
        }
    }

    /**
     * Executes JavaScript flow in live mode - propagates exceptions to caller.
     * <p>
     * This method creates a JsFlow initialized with componentProvider (live mode services with full access),
     * sets organizationEntityId and userEntityId, creates JsResultAndModel, calls evaluateJsFlow to compile
     * JavaScript, and executes Flow via executeFlow. Does NOT catch PolyglotException - allows exception
     * to propagate to controller error handling.

     * <p>
     * <strong>Use case:</strong> Production mode - failures are logged and handled by Spring error handling.
     * Provides full access to services and allows proper error propagation for monitoring and alerting.

     *
     * @param jsFlow JavaScript source code defining flow logic
     * @param params Request parameters Map (String to String)
     * @param organizationId Organization ID for tenant context (may be null)
     * @param userId User ID for security context
     * @param form Optional AbstractOrganizationRelatedEntityForm (may be null)
     * @param scriptSourceFileName Source filename for error reporting
     * @return PageModelMap with execution results
     * @throws PolyglotException If JavaScript evaluation or execution fails
     * @see JsFlow
     * @see JsResultAndModel
     * @see LiveComponentProvider
     */
    public PageModelMap runLiveFlow(String jsFlow, Map<String, String> params, Long organizationId, long userId, AbstractOrganizationRelatedEntityForm form, String scriptSourceFileName) {
        Flow f = JsFlow.init(componentProvider, params, form)
                .thenSet(organizationEntityId, userEntityId, a -> Tuples.of(organizationId, userId));

        JsResultAndModel initialResultAndModel = JsResultAndModel.constructNew(componentProvider, params, form);
        return executeFlow(evaluateJsFlow(jsFlow, f, initialResultAndModel, scriptSourceFileName), initialResultAndModel.model);
    }
}
