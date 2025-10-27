/*
MIT License

Copyright (c) 2014-2022, Codedose CDX Sp. z o.o. Sp. K. <stratoflow.com>

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

package com.openkoda.core.flow;
import org.graalvm.polyglot.PolyglotException;
import reactor.util.function.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Functional pipeline DSL for composing controller logic with step-by-step execution and result aggregation.
 * <p>
 * Flow provides a builder pattern for request handling in OpenKoda controllers. It enables functional composition
 * of business logic steps, where each step can access previous results and store values in a shared model map.
 * The pipeline supports transactional execution, comprehensive error handling, and automatic model population.
 * </p>
 * <p>
 * The core concept is: {@code Flow.init(services).thenSet(key, lambda).then(lambda).execute()}. Each step receives
 * a {@link ResultAndModel} containing the previous step's result, the shared {@link PageModelMap}, service beans,
 * and additional parameters. Results can be stored in the model using {@code thenSet()} variants, making them
 * available to subsequent steps and the final view.
 * </p>
 * <p>
 * Flow is immutable - each builder method returns a new Flow instance with the composed function, preserving the
 * original pipeline. This enables safe reuse and composition of flow fragments.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * Flow.init(services)
 *     .thenSet(entity, a -&gt; repository.findById(entityId))
 *     .execute();
 * </pre>
 * </p>
 * <p>
 * With transactional execution:
 * <pre>
 * Flow.init(services, () -&gt; transactionTemplate)
 *     .thenSet(entity, a -&gt; repository.save(a.result))
 *     .execute();
 * </pre>
 * </p>
 *
 * @param <I> input result type - the type of result expected from previous pipeline steps
 * @param <O> output result type - the type of result produced by this pipeline stage
 * @param <CP> context/services type - typically a services aggregator bean providing access to application services
 *
 * @see ResultAndModel
 * @see PageModelMap
 * @see TransactionalExecutor
 * @see PageAttr
 * @see BasePageAttributes
 *
 * @since 1.7.1
 * @author OpenKoda Team
 * @version 1.7.1
 */
public class Flow<I, O, CP> implements Function <ResultAndModel<I, CP>, O>, BasePageAttributes, LoggingComponent {


    /**
     * Applies this Flow's composed function to the given result and model.
     * <p>
     * This method executes the pipeline function, propagating the input result through the composed transformation.
     * </p>
     *
     * @param iResultAndModel the input result and model containing previous step's result, shared model, and services
     * @return the output result after applying this pipeline stage's transformation
     */
    @Override
    public O apply(ResultAndModel<I, CP> iResultAndModel) {
        return f.apply(iResultAndModel);
    }

    /**
     * Core function representing the composed pipeline transformation.
     * <p>
     * This function encapsulates all previous pipeline steps composed via {@code then()}, {@code thenSet()}, and
     * related builder methods. Each builder operation wraps this function with additional logic, creating a new
     * Flow instance with the extended pipeline.
     * </p>
     */
    protected final Function <ResultAndModel<I, CP>, O> f;
    
    /**
     * Transactional executor for wrapping pipeline execution in database transactions.
     * <p>
     * When set, the entire Flow execution runs within a transaction boundary. Lazily initialized from
     * {@link #transactionalExecutorProvider} if not directly set.
     * </p>
     */
    private TransactionalExecutor transactionalExecutor = null;
    
    /**
     * Supplier for lazy provisioning of transactional executor.
     * <p>
     * Provides deferred transactional executor creation, typically supplying a Spring TransactionTemplate.
     * The executor is instantiated only when {@link #execute()} is called, enabling transaction context
     * to be properly established at execution time.
     * </p>
     */
    protected Supplier<TransactionalExecutor> transactionalExecutorProvider = null;
    
    /**
     * Consumer hook for propagating configuration to chained Flow instances.
     * <p>
     * Invoked by builder methods to copy transactional executor configuration from parent flows to child flows,
     * ensuring transaction boundaries are preserved across pipeline composition.
     * </p>
     */
    protected Consumer<Function> onThen = null;

    /**
     * Global counter for assigning unique identifiers to Flow instances for debugging.
     */
    private static int flowCounter = 0;
    
    /**
     * Unique identifier for this Flow instance used in {@link #toString()} for debugging.
     */
    private final int flowNumber = flowCounter;

    /**
     * Context services providing access to application service beans.
     * <p>
     * Typically an aggregator bean (e.g., Services) exposing repositories, business services, and utilities.
     * Available to all pipeline steps via {@link ResultAndModel#services}.
     * </p>
     */
    public final CP services;

    /**
     * Additional parameters passed through the pipeline execution context.
     * <p>
     * Immutable map of parameters available to all pipeline steps. Can be used for passing request-scoped
     * data like HTTP parameters, user context, or feature flags.
     * </p>
     */
    public final Map<String, Object> params;

    /**
     * Flag controlling error verbosity in exception messages.
     * <p>
     * When true, full stack traces are included in error messages stored in the model. When false, only
     * exception class names are included. Defaults to true for comprehensive error diagnostics.
     * </p>
     */
    public static boolean FULL_STACKTRACE_IN_ERROR = true;

    /**
     * Initializes the parameters map, ensuring immutability.
     * <p>
     * Returns an empty immutable map if the input is null, otherwise wraps the provided map in an
     * unmodifiable view. This prevents accidental parameter modification during pipeline execution.
     * </p>
     *
     * @param params the parameters map to initialize, may be null
     * @return an immutable map containing the parameters, or an empty map if input is null
     */
    public static Map<String, Object> initParamsMap(Map<String, Object> params) {
        if( params == null ) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(params);
    }

    /**
     * Factory method for constructing new Flow instances with full configuration.
     * <p>
     * Used internally by builder methods to create new Flow instances with composed functions while
     * preserving transactional executor configuration and parameter context.
     * </p>
     *
     * @param <II> input result type for the new flow
     * @param <IO> output result type for the new flow
     * @param <ICP> context/services type for the new flow
     * @param params parameters map for the new flow
     * @param services services context for the new flow
     * @param f the composed function for the new flow
     * @param transactionalExecutorProvider supplier for transactional executor
     * @param onThen configuration propagation hook
     * @return a new Flow instance with the specified configuration
     */
    protected <II, IO, ICP> Flow<II, IO, ICP> constructFlow(Map<String, Object> params, ICP services, Function <ResultAndModel<II, ICP>, IO> f, Supplier<TransactionalExecutor> transactionalExecutorProvider,
                                                            Consumer<Function> onThen) {
        return new Flow<>(params, services, f, transactionalExecutorProvider, onThen);
    }

    /**
     * Factory method for constructing ResultAndModel carriers.
     * <p>
     * Creates a carrier object encapsulating the current model, step result, services, and parameters.
     * This carrier is passed to each pipeline step function.
     * </p>
     *
     * @param <IR> result type
     * @param <ICP> context/services type
     * @param model the shared page model map
     * @param result the result value from the current step
     * @param services the services context
     * @param params the parameters map
     * @return a new ResultAndModel carrier with the specified values
     */
    protected<IR, ICP> ResultAndModel<IR, ICP> constructResultAndModel(PageModelMap model, IR result, ICP services, Map<String, Object> params) {
        return new ResultAndModel<>(model, result, services, params);
    }

    /**
     * Protected constructor for creating Flow instances with full configuration including transactional support.
     *
     * @param params immutable parameters map passed through pipeline execution
     * @param services services context providing access to application beans
     * @param f the function representing this pipeline stage
     * @param transactionalExecutorProvider supplier for lazy transactional executor creation
     * @param onThen configuration propagation hook for chained flows
     */
    protected Flow(Map<String, Object> params, CP services, Function <ResultAndModel<I, CP>, O> f, Supplier<TransactionalExecutor> transactionalExecutorProvider,
                   Consumer<Function> onThen) {
        this.params = params;
        this.f = f;
        this.transactionalExecutorProvider = transactionalExecutorProvider;
        this.services = services;
        this.onThen = onThen;
        flowCounter++;
    }

    /**
     * Protected constructor for creating Flow instances without transactional support.
     *
     * @param params immutable parameters map passed through pipeline execution
     * @param services services context providing access to application beans
     * @param f the function representing this pipeline stage
     */
    protected Flow(Map<String, Object> params, CP services, Function <ResultAndModel<I, CP>, O> f) {
        this.params = params;
        this.f = f;
        this.services = services;
        flowCounter++;
    }

    /**
     * Copies transactional executor configuration from chained Flow to this Flow.
     * <p>
     * Ensures that transactional boundaries are preserved when composing flows. If the next step in the pipeline
     * is also a Flow, copies its transactional executor provider. Invokes the {@link #onThen} hook to propagate
     * additional configuration.
     * </p>
     *
     * @param <N> type parameter (unused)
     * @param after the next function in the pipeline, potentially a Flow with transactional configuration
     */
    protected <N> void copyTransactionalExecutorProvider(Function after){
        if(onThen != null) {
            onThen.accept(after);
        }
        if (after instanceof Flow) {
            this.transactionalExecutorProvider = ((Flow) after).transactionalExecutorProvider;
        }
    }

    /**
     * Composes a pipeline step that ignores the previous result.
     * <p>
     * The next step receives null as its result, effectively starting fresh while maintaining access to the
     * shared model, services, and parameters. Useful for independent operations that don't depend on prior results.
     * </p>
     *
     * @param <N> type parameter (unused)
     * @param <OI> input type for the next step (typically Object)
     * @param <OO> output type for the next step
     * @param after the function to execute, receiving null as result
     * @return a new Flow with the composed pipeline
     * @throws NullPointerException if after is null
     */
    public <N, OI, OO> Flow<I,OO,CP> thenWithoutResult(Function <ResultAndModel<OI,CP>, OO> after) {
        Objects.requireNonNull(after);
        copyTransactionalExecutorProvider(after);
        return constructFlow(params, services,
                (ResultAndModel<I, CP> t) -> after.apply( constructResultAndModel(t.model, (f.apply(t) == null ? null : null), services, params)),
                transactionalExecutorProvider, onThen);
    }

    /**
     * Composes a pipeline step using the previous result.
     * <p>
     * The next step receives the output of the current step as its input result. This is the primary composition
     * method for building sequential pipelines where each step processes the previous step's output.
     * </p>
     * <p>
     * Example:
     * <pre>
     * Flow.init(services)
     *     .then(a -&gt; repository.findById(id))
     *     .then(a -&gt; enrichEntity(a.result))
     *     .execute();
     * </pre>
     * </p>
     *
     * @param <N> output type for the next step
     * @param after the function to execute, receiving the current step's result
     * @return a new Flow with the composed pipeline
     * @throws NullPointerException if after is null
     */
    public <N> Flow<I,N,CP> then(Function <ResultAndModel<O,CP>, N> after) {
        Objects.requireNonNull(after);
        copyTransactionalExecutorProvider(after);
        return constructFlow(params, services, (ResultAndModel<I, CP> t) -> after.apply( constructResultAndModel(t.model, f.apply(t), t.services, params)), transactionalExecutorProvider, onThen);
    }

    /**
     * Composes a step that stores a default value in the model.
     * <p>
     * Uses the PageAttr's constructor to create the default value and stores it in the model under the
     * PageAttr's key. Useful for initializing model attributes with default instances.
     * </p>
     *
     * @param <N> value type
     * @param pageAttr the page attribute defining the model key and default constructor
     * @return a new Flow with the composed pipeline storing the default value
     */
    public <N> Flow<I,N,CP> thenSetDefault(PageAttr<N> pageAttr) {
        Function <ResultAndModel<O,CP>, N> after = a -> pageAttr.constructor.get();
        return thenSet(pageAttr, after);
    }

    /**
     * Composes a step that stores two default values in the model.
     * <p>
     * Creates default values using each PageAttr's constructor and stores them in the model as a Tuple2.
     * </p>
     *
     * @param <N1> first value type
     * @param <N2> second value type
     * @param pa1 first page attribute
     * @param pa2 second page attribute
     * @return a new Flow with the composed pipeline storing both default values
     */
    public <N1, N2> Flow<I,Tuple2<N1, N2>,CP> thenSetDefault(PageAttr<N1> pa1, PageAttr<N2> pa2) {
        Function <ResultAndModel<O,CP>, Tuple2<N1, N2>> after =
                a -> Tuples.of(
                        pa1.constructor.get(),
                        pa2.constructor.get()
                );
        return thenSet(pa1, pa2, after);
    }

    /**
     * Composes a step that stores three default values in the model.
     *
     * @param <N1> first value type
     * @param <N2> second value type
     * @param <N3> third value type
     * @param pa1 first page attribute
     * @param pa2 second page attribute
     * @param pa3 third page attribute
     * @return a new Flow with the composed pipeline storing all three default values
     */
    public <N1, N2, N3> Flow<I,Tuple3<N1, N2, N3>,CP> thenSetDefault(PageAttr<N1> pa1, PageAttr<N2> pa2, PageAttr<N3> pa3) {
        Function <ResultAndModel<O,CP>, Tuple3<N1, N2, N3>> after =
                a -> Tuples.of(
                        pa1.constructor.get(),
                        pa2.constructor.get(),
                        pa3.constructor.get()
                );
        return thenSet(pa1, pa2, pa3, after);
    }

    /**
     * Composes a step that stores four default values in the model.
     *
     * @param <N1> first value type
     * @param <N2> second value type
     * @param <N3> third value type
     * @param <N4> fourth value type
     * @param pa1 first page attribute
     * @param pa2 second page attribute
     * @param pa3 third page attribute
     * @param pa4 fourth page attribute
     * @return a new Flow with the composed pipeline storing all four default values
     */
    public <N1, N2, N3, N4> Flow<I,Tuple4<N1, N2, N3, N4>,CP> thenSetDefault(PageAttr<N1> pa1, PageAttr<N2> pa2, PageAttr<N3> pa3, PageAttr<N4> pa4) {
        Function <ResultAndModel<O,CP>, Tuple4<N1, N2, N3, N4>> after =
                a -> Tuples.of(
                        pa1.constructor.get(),
                        pa2.constructor.get(),
                        pa3.constructor.get(),
                        pa4.constructor.get()
                );
        return thenSet(pa1, pa2, pa3, pa4, after);
    }

    /**
     * Composes a step that stores five default values in the model.
     *
     * @param <N1> first value type
     * @param <N2> second value type
     * @param <N3> third value type
     * @param <N4> fourth value type
     * @param <N5> fifth value type
     * @param pa1 first page attribute
     * @param pa2 second page attribute
     * @param pa3 third page attribute
     * @param pa4 fourth page attribute
     * @param pa5 fifth page attribute
     * @return a new Flow with the composed pipeline storing all five default values
     */
    public <N1, N2, N3, N4, N5> Flow<I,Tuple5<N1, N2, N3, N4, N5>,CP> thenSetDefault(PageAttr<N1> pa1, PageAttr<N2> pa2, PageAttr<N3> pa3, PageAttr<N4> pa4, PageAttr<N5> pa5) {
        Function <ResultAndModel<O,CP>, Tuple5<N1, N2, N3, N4, N5>> after =
                a -> Tuples.of(
                        pa1.constructor.get(),
                        pa2.constructor.get(),
                        pa3.constructor.get(),
                        pa4.constructor.get(),
                        pa5.constructor.get()
                );
        return thenSet(pa1, pa2, pa3, pa4, pa5, after);
    }

    /**
     * Composes a step that stores six default values in the model.
     *
     * @param <N1> first value type
     * @param <N2> second value type
     * @param <N3> third value type
     * @param <N4> fourth value type
     * @param <N5> fifth value type
     * @param <N6> sixth value type
     * @param pa1 first page attribute
     * @param pa2 second page attribute
     * @param pa3 third page attribute
     * @param pa4 fourth page attribute
     * @param pa5 fifth page attribute
     * @param pa6 sixth page attribute
     * @return a new Flow with the composed pipeline storing all six default values
     */
    public <N1, N2, N3, N4, N5, N6> Flow<I,Tuple6<N1, N2, N3, N4, N5, N6>,CP> thenSetDefault(PageAttr<N1> pa1, PageAttr<N2> pa2, PageAttr<N3> pa3, PageAttr<N4> pa4, PageAttr<N5> pa5, PageAttr<N6> pa6) {
        Function <ResultAndModel<O,CP>, Tuple6<N1, N2, N3, N4, N5, N6>> after =
                a -> Tuples.of(
                        pa1.constructor.get(),
                        pa2.constructor.get(),
                        pa3.constructor.get(),
                        pa4.constructor.get(),
                        pa5.constructor.get(),
                        pa6.constructor.get()
                );
        return thenSet(pa1, pa2, pa3, pa4, pa5, pa6, after);
    }


    /**
     * Composes a step that computes a value and stores it in the model.
     * <p>
     * Executes the function using the previous step's result, then stores the returned value in the model under
     * the specified PageAttr key. The value becomes available to subsequent steps and the final view template.
     * This is the primary method for populating model attributes.
     * </p>
     * <p>
     * Example:
     * <pre>
     * Flow.init(services)
     *     .thenSet(entity, a -&gt; repository.findById(id))
     *     .execute();
     * </pre>
     * </p>
     *
     * @param <N> value type
     * @param pageAttr the page attribute defining the model key
     * @param after the function to compute the value, receiving the previous result
     * @return a new Flow with the composed pipeline storing the computed value
     * @throws NullPointerException if pageAttr or after is null
     */
    public <N> Flow<I,N,CP> thenSet(PageAttr<N> pageAttr, Function <ResultAndModel<O,CP>, N> after) {
        Objects.requireNonNull(after);
        copyTransactionalExecutorProvider(after);
        return constructFlow(params, services, (ResultAndModel<I, CP> t) -> t.model.put(pageAttr , after.apply( constructResultAndModel(t.model, f.apply(t), t.services, params))), transactionalExecutorProvider, onThen);
    }

    /**
     * Composes a step that computes two values and stores them in the model.
     *
     * @param <N1> first value type
     * @param <N2> second value type
     * @param pa1 first page attribute
     * @param pa2 second page attribute
     * @param after the function to compute both values as a Tuple2
     * @return a new Flow with the composed pipeline storing both values
     * @throws NullPointerException if any parameter is null
     */
    public <N1, N2> Flow<I,Tuple2<N1, N2>,CP> thenSet(PageAttr<N1> pa1, PageAttr<N2> pa2, Function <ResultAndModel<O,CP>, Tuple2<N1, N2>> after) {
        Objects.requireNonNull(after);
        copyTransactionalExecutorProvider(after);
        return constructFlow(params, services, (ResultAndModel<I, CP> t) -> t.model.put(pa1, pa2, after.apply( constructResultAndModel(t.model, f.apply(t), t.services, params))), transactionalExecutorProvider, onThen);
    }
    
    /**
     * Composes a step that computes three values and stores them in the model.
     *
     * @param <N1> first value type
     * @param <N2> second value type
     * @param <N3> third value type
     * @param pa1 first page attribute
     * @param pa2 second page attribute
     * @param pa3 third page attribute
     * @param after the function to compute all three values as a Tuple3
     * @return a new Flow with the composed pipeline storing all three values
     * @throws NullPointerException if any parameter is null
     */
    public <N1, N2, N3> Flow<I,Tuple3<N1, N2, N3>,CP> thenSet(PageAttr<N1> pa1, PageAttr<N2> pa2, PageAttr<N3> pa3, Function <ResultAndModel<O,CP>, Tuple3<N1, N2, N3>> after) {
        Objects.requireNonNull(after);
        copyTransactionalExecutorProvider(after);
        return constructFlow(params, services, (ResultAndModel<I, CP> t) -> t.model.put(pa1, pa2, pa3, after.apply( constructResultAndModel(t.model, f.apply(t), t.services, params))), transactionalExecutorProvider, onThen);
    }
    
    /**
     * Composes a step that computes four values and stores them in the model.
     *
     * @param <N1> first value type
     * @param <N2> second value type
     * @param <N3> third value type
     * @param <N4> fourth value type
     * @param pa1 first page attribute
     * @param pa2 second page attribute
     * @param pa3 third page attribute
     * @param pa4 fourth page attribute
     * @param after the function to compute all four values as a Tuple4
     * @return a new Flow with the composed pipeline storing all four values
     * @throws NullPointerException if any parameter is null
     */
    public <N1, N2, N3, N4> Flow<I,Tuple4<N1, N2, N3, N4>,CP> thenSet(PageAttr<N1> pa1, PageAttr<N2> pa2, PageAttr<N3> pa3, PageAttr<N4> pa4, Function <ResultAndModel<O,CP>, Tuple4<N1, N2, N3, N4>> after) {
        Objects.requireNonNull(after);
        copyTransactionalExecutorProvider(after);
        return constructFlow(params, services, (ResultAndModel<I, CP> t) -> t.model.put(pa1, pa2, pa3, pa4, after.apply( constructResultAndModel(t.model, f.apply(t), t.services, params))), transactionalExecutorProvider, onThen);
    }
    
    /**
     * Composes a step that computes five values and stores them in the model.
     *
     * @param <N1> first value type
     * @param <N2> second value type
     * @param <N3> third value type
     * @param <N4> fourth value type
     * @param <N5> fifth value type
     * @param pa1 first page attribute
     * @param pa2 second page attribute
     * @param pa3 third page attribute
     * @param pa4 fourth page attribute
     * @param pa5 fifth page attribute
     * @param after the function to compute all five values as a Tuple5
     * @return a new Flow with the composed pipeline storing all five values
     * @throws NullPointerException if any parameter is null
     */
    public <N1, N2, N3, N4, N5> Flow<I,Tuple5<N1, N2, N3, N4, N5>,CP> thenSet(PageAttr<N1> pa1, PageAttr<N2> pa2, PageAttr<N3> pa3, PageAttr<N4> pa4, PageAttr<N5> pa5,  Function <ResultAndModel<O,CP>, Tuple5<N1, N2, N3, N4, N5>> after) {
        Objects.requireNonNull(after);
        copyTransactionalExecutorProvider(after);
        return constructFlow(params, services, (ResultAndModel<I, CP> t) -> t.model.put(pa1, pa2, pa3, pa4, pa5, after.apply( constructResultAndModel(t.model, f.apply(t), t.services, params))), transactionalExecutorProvider, onThen);
    }

    /**
     * Composes a step that computes six values and stores them in the model.
     *
     * @param <N1> first value type
     * @param <N2> second value type
     * @param <N3> third value type
     * @param <N4> fourth value type
     * @param <N5> fifth value type
     * @param <N6> sixth value type
     * @param pa1 first page attribute
     * @param pa2 second page attribute
     * @param pa3 third page attribute
     * @param pa4 fourth page attribute
     * @param pa5 fifth page attribute
     * @param pa6 sixth page attribute
     * @param after the function to compute all six values as a Tuple6
     * @return a new Flow with the composed pipeline storing all six values
     * @throws NullPointerException if any parameter is null
     */
    public <N1, N2, N3, N4, N5, N6> Flow<I,Tuple6<N1, N2, N3, N4, N5, N6>,CP> thenSet(PageAttr<N1> pa1, PageAttr<N2> pa2, PageAttr<N3> pa3, PageAttr<N4> pa4, PageAttr<N5> pa5, PageAttr<N6> pa6,  Function <ResultAndModel<O,CP>, Tuple6<N1, N2, N3, N4, N5, N6>> after) {
        Objects.requireNonNull(after);
        copyTransactionalExecutorProvider(after);
        return constructFlow(params, services, (ResultAndModel<I, CP> t) -> t.model.put(pa1, pa2, pa3, pa4, pa5, pa6, after.apply( constructResultAndModel(t.model, f.apply(t), t.services, params))), transactionalExecutorProvider, onThen);
    }

    /**
     * Initializes a Flow with no services or initial values.
     * <p>
     * Creates a minimal Flow that returns its input result unchanged. Useful as a starting point for
     * pipelines that don't require service access.
     * </p>
     *
     * @param <A> result type
     * @param <CP> context/services type
     * @return a new Flow ready for composition
     */
    public static <A, CP> Flow<A, A, CP> init() {
        return new Flow<>(initParamsMap(null), null, a -> a.result);
    }

    /**
     * Initializes a Flow with services context.
     * <p>
     * Creates a Flow with access to service beans, typically a Services aggregator providing repositories
     * and business services. This is the most common initialization pattern for controller flows.
     * </p>
     *
     * @param <A> result type
     * @param <CP> context/services type
     * @param services the services context, typically a Services aggregator bean
     * @return a new Flow ready for composition with service access
     */
    public static <A, CP> Flow<A, A, CP> init(CP services) {
        return new Flow<>(initParamsMap(null), services, a -> a.result);
    }

    /**
     * Initializes a Flow with services and parameters.
     *
     * @param <A> result type
     * @param <CP> context/services type
     * @param services the services context
     * @param params additional parameters to pass through the pipeline
     * @return a new Flow with service and parameter access
     */
    public static <A, CP> Flow<A, A, CP> init(CP services, Map params) {
        return new Flow<>(initParamsMap(params), services, a -> a.result);
    }

    /**
     * Initializes a Flow with transactional execution support.
     * <p>
     * Creates a Flow that executes within a transaction boundary. The transactional executor is lazily
     * created from the provider when {@link #execute()} is called.
     * </p>
     *
     * @param <A> result type
     * @param <CP> context/services type
     * @param transactionalExecutorProvider supplier for transactional executor, typically providing a Spring TransactionTemplate
     * @return a new Flow with transactional execution support
     */
    public static <A,CP> Flow<A, A,CP> init(Supplier<TransactionalExecutor> transactionalExecutorProvider) {
        return new Flow<>(initParamsMap(null), null, a -> a.result, transactionalExecutorProvider, null);
    }

    /**
     * Initializes a Flow with services and transactional execution support.
     *
     * @param <A> result type
     * @param <CP> context/services type
     * @param services the services context
     * @param transactionalExecutorProvider supplier for transactional executor
     * @return a new Flow with service access and transactional execution
     */
    public static <A,CP> Flow<A, A,CP> init(CP services, Supplier<TransactionalExecutor> transactionalExecutorProvider) {
        return new Flow<>(initParamsMap(null), services, a -> a.result, transactionalExecutorProvider, null);
    }

    /**
     * Initializes a Flow with services and a custom initial function.
     *
     * @param <I> input type
     * @param <O> output type
     * @param <CP> context/services type
     * @param services the services context
     * @param f the initial transformation function
     * @return a new Flow with the specified function
     */
    public static <I, O,CP> Flow<I, O,CP> init(CP services, Function <ResultAndModel<I, CP>, O> f) {
        return new Flow(initParamsMap(null), services, f);
    }

    /**
     * Initializes a Flow with services and an initial value.
     * <p>
     * The flow immediately returns the specified initial value, ignoring any input result.
     * </p>
     *
     * @param <A> result type
     * @param <CP> context/services type
     * @param services the services context
     * @param initValue the initial value to return
     * @return a new Flow returning the initial value
     */
    public static <A,CP> Flow<A, A,CP> init(CP services, A initValue) {
        return new Flow<>(initParamsMap(null), services, a -> initValue);
    }

    /**
     * Initializes a Flow with services and two initial values as a Tuple2.
     *
     * @param <A1> first value type
     * @param <A2> second value type
     * @param <CP> context/services type
     * @param services the services context
     * @param a1 first initial value
     * @param a2 second initial value
     * @return a new Flow returning both values as a Tuple2
     */
    public static <A1, A2,CP> Flow<Tuple2<A1, A2>, Tuple2<A1, A2>,CP> init(CP services, A1 a1, A2 a2) {
        return new Flow<>(initParamsMap(null), services, a -> Tuples.of(a1, a2));
    }

    /**
     * Initializes a Flow with services and three initial values as a Tuple3.
     *
     * @param <A1> first value type
     * @param <A2> second value type
     * @param <A3> third value type
     * @param <CP> context/services type
     * @param services the services context
     * @param a1 first initial value
     * @param a2 second initial value
     * @param a3 third initial value
     * @return a new Flow returning all three values as a Tuple3
     */
    public static <A1, A2, A3,CP> Flow<Tuple3<A1, A2, A3>, Tuple3<A1, A2, A3>,CP> init(CP services, A1 a1, A2 a2, A3 a3) {
        return new Flow<>(initParamsMap(null), services, a -> Tuples.of(a1, a2, a3));
    }

    /**
     * Initializes a Flow with services and five initial values as a Tuple5.
     *
     * @param <A1> first value type
     * @param <A2> second value type
     * @param <A3> third value type
     * @param <A4> fourth value type
     * @param <A5> fifth value type
     * @param <CP> context/services type
     * @param services the services context
     * @param a1 first initial value
     * @param a2 second initial value
     * @param a3 third initial value
     * @param a4 fourth initial value
     * @param a5 fifth initial value
     * @return a new Flow returning all five values as a Tuple5
     */
    public static <A1, A2, A3, A4, A5,CP> Flow<Tuple5<A1, A2, A3, A4, A5>, Tuple5<A1, A2, A3, A4, A5>,CP> init(CP services, A1 a1, A2 a2, A3 a3, A4 a4, A5 a5) {
        return new Flow<>(initParamsMap(null), services, a -> Tuples.of(a1, a2, a3, a4, a5));
    }

    /**
     * Initializes a Flow with services and six initial values as a Tuple6.
     *
     * @param <A1> first value type
     * @param <A2> second value type
     * @param <A3> third value type
     * @param <A4> fourth value type
     * @param <A5> fifth value type
     * @param <A6> sixth value type
     * @param <CP> context/services type
     * @param services the services context
     * @param a1 first initial value
     * @param a2 second initial value
     * @param a3 third initial value
     * @param a4 fourth initial value
     * @param a5 fifth initial value
     * @param a6 sixth initial value
     * @return a new Flow returning all six values as a Tuple6
     */
    public static <A1, A2, A3, A4, A5, A6,CP> Flow<Tuple6<A1, A2, A3, A4, A5, A6>, Tuple6<A1, A2, A3, A4, A5, A6>,CP> init(CP services, A1 a1, A2 a2, A3 a3, A4 a4, A5 a5, A6 a6) {
        return new Flow<>(initParamsMap(null), services, a -> Tuples.of(a1, a2, a3, a4, a5, a6));
    }

    /**
     * Initializes a Flow with a PageAttr and value, automatically storing it in the model.
     *
     * @param <A1> value type
     * @param <CP> context/services type
     * @param a1 page attribute defining the model key
     * @param t the value to store
     * @return a new Flow with the value stored in the model
     */
    public static <A1,CP> Flow<A1, A1, CP> init(PageAttr<A1> a1, A1 t) {
        return Flow.init(null, a1, t);
    }

    /**
     * Initializes a Flow with services, a PageAttr, and value, automatically storing it in the model.
     *
     * @param <A1> value type
     * @param <CP> context/services type
     * @param services the services context
     * @param a1 page attribute defining the model key
     * @param t the value to store
     * @return a new Flow with service access and the value stored in the model
     */
    public static <A1,CP> Flow<A1, A1, CP> init(CP services, PageAttr<A1> a1, A1 t) {
        return Flow.init(services, t).thenSet(a1, a -> a.result);
    }

    /**
     * Initializes a Flow with services, two PageAttrs, and a Tuple2 value, storing both in the model.
     *
     * @param <A1> first value type
     * @param <A2> second value type
     * @param <CP> context/services type
     * @param services the services context
     * @param a1 first page attribute
     * @param a2 second page attribute
     * @param t the tuple containing both values
     * @return a new Flow with both values stored in the model
     */
    public static <A1, A2,CP> Flow<Tuple2<A1, A2>, Tuple2<A1, A2>,CP> init(CP services, PageAttr<A1> a1, PageAttr<A2> a2, Tuple2<A1, A2> t) {
        return Flow.init(services, t).thenSet(a1, a2, a -> a.result);
    }

    /**
     * Initializes a Flow with services, three PageAttrs, and a Tuple3 value, storing all in the model.
     *
     * @param <A1> first value type
     * @param <A2> second value type
     * @param <A3> third value type
     * @param <CP> context/services type
     * @param services the services context
     * @param a1 first page attribute
     * @param a2 second page attribute
     * @param a3 third page attribute
     * @param t the tuple containing all three values
     * @return a new Flow with all three values stored in the model
     */
    public static <A1, A2, A3,CP> Flow<Tuple3<A1, A2, A3>, Tuple3<A1, A2, A3>,CP> init(CP services, PageAttr<A1> a1, PageAttr<A2> a2, PageAttr<A3> a3, Tuple3<A1, A2, A3> t) {
        return Flow.init(services, t).thenSet(a1, a2, a3, a -> a.result);
    }

    /**
     * Initializes a Flow with services, four PageAttrs, and a Tuple4 value, storing all in the model.
     *
     * @param <A1> first value type
     * @param <A2> second value type
     * @param <A3> third value type
     * @param <A4> fourth value type
     * @param <CP> context/services type
     * @param services the services context
     * @param a1 first page attribute
     * @param a2 second page attribute
     * @param a3 third page attribute
     * @param a4 fourth page attribute
     * @param t the tuple containing all four values
     * @return a new Flow with all four values stored in the model
     */
    public static <A1, A2, A3, A4,CP> Flow<Tuple4<A1, A2, A3, A4>, Tuple4<A1, A2, A3, A4>,CP> init(CP services, PageAttr<A1> a1, PageAttr<A2> a2, PageAttr<A3> a3, PageAttr<A4> a4, Tuple4<A1, A2, A3, A4> t) {
        return Flow.init(services, t).thenSet(a1, a2, a3, a4, a -> a.result);
    }

    /**
     * Initializes a Flow with services, five PageAttrs, and a Tuple5 value, storing all in the model.
     *
     * @param <A1> first value type
     * @param <A2> second value type
     * @param <A3> third value type
     * @param <A4> fourth value type
     * @param <A5> fifth value type
     * @param <CP> context/services type
     * @param services the services context
     * @param a1 first page attribute
     * @param a2 second page attribute
     * @param a3 third page attribute
     * @param a4 fourth page attribute
     * @param a5 fifth page attribute
     * @param t the tuple containing all five values
     * @return a new Flow with all five values stored in the model
     */
    public static <A1, A2, A3, A4, A5,CP> Flow<Tuple5<A1, A2, A3, A4, A5>, Tuple5<A1, A2, A3, A4, A5>,CP> init(CP services, PageAttr<A1> a1, PageAttr<A2> a2, PageAttr<A3> a3, PageAttr<A4> a4, PageAttr<A5> a5, Tuple5<A1, A2, A3, A4, A5> t) {
        return Flow.init(services, t).thenSet(a1, a2, a3, a4, a5, a -> a.result);
    }

    /**
     * Initializes a Flow with services, six PageAttrs, and a Tuple6 value, storing all in the model.
     *
     * @param <A1> first value type
     * @param <A2> second value type
     * @param <A3> third value type
     * @param <A4> fourth value type
     * @param <A5> fifth value type
     * @param <A6> sixth value type
     * @param <CP> context/services type
     * @param services the services context
     * @param a1 first page attribute
     * @param a2 second page attribute
     * @param a3 third page attribute
     * @param a4 fourth page attribute
     * @param a5 fifth page attribute
     * @param a6 sixth page attribute
     * @param t the tuple containing all six values
     * @return a new Flow with all six values stored in the model
     */
    public static <A1, A2, A3, A4, A5, A6,CP> Flow<Tuple6<A1, A2, A3, A4, A5, A6>, Tuple6<A1, A2, A3, A4, A5, A6>,CP> init(CP services, PageAttr<A1> a1, PageAttr<A2> a2, PageAttr<A3> a3, PageAttr<A4> a4, PageAttr<A5> a5, PageAttr<A6> a6, Tuple6<A1, A2, A3, A4, A5, A6> t) {
        return Flow.init(services, t).thenSet(a1, a2, a3, a4, a5, a6, a -> a.result);
    }

    /**
     * Executes the composed pipeline with a new model.
     * <p>
     * This is the terminal operation that triggers pipeline execution. Creates a new empty {@link PageModelMap},
     * executes all composed steps in sequence, and returns the populated model containing all values stored via
     * {@code thenSet()} operations. The model is suitable for rendering in view templates.
     * </p>
     * <p>
     * If a {@link TransactionalExecutor} is configured, the entire pipeline executes within a transaction
     * boundary. Exceptions are caught and handled according to type, with error information stored in the model.
     * </p>
     *
     * @return the populated page model map containing all stored values and error state
     */
    public PageModelMap execute() {
        PageModelMap model = new PageModelMap();
        ResultAndModel<O,CP> result = executeFlow(model);
        return model;
    }

    /**
     * Executes the composed pipeline with an existing model.
     * <p>
     * Executes the pipeline using a pre-populated model, allowing values to be passed in and accumulated across
     * multiple flow executions. Useful for chaining flows or providing initial model state.
     * </p>
     *
     * @param model the existing page model map to populate
     * @return the same page model map with additional values from pipeline execution
     */
    public PageModelMap execute(PageModelMap model) {
        ResultAndModel<O,CP> result = executeFlow(model);
        return model;
    }

    /**
     * Executes the composed pipeline and returns both result and model.
     * <p>
     * Deprecated in favor of {@link #execute()} which returns only the model. The result value is typically
     * not needed as relevant values are stored in the model via {@code thenSet()} operations.
     * </p>
     *
     * @return a carrier containing both the final result and populated model
     * @deprecated Use {@link #execute()} instead. This method will be removed in a future version.
     */
    @Deprecated(forRemoval = true)
    public ResultAndModel<O,CP> executeWithResult() {
        PageModelMap model = new PageModelMap();
        ResultAndModel<O,CP> result = executeFlow(model);
        return result;
    }

    /**
     * Executes the pipeline with comprehensive exception handling and post-processing.
     * <p>
     * This method orchestrates the complete execution flow:
     * </p>
     * <ol>
     * <li>Provisions the {@link TransactionalExecutor} from the provider if configured</li>
     * <li>Executes the pipeline function, optionally within a transaction boundary</li>
     * <li>On success: sets {@code isError=false}, invokes post-execute processors</li>
     * <li>On failure: catches exceptions in this hierarchy and stores error details in model</li>
     * </ol>
     * <p>
     * Exception handling hierarchy:
     * </p>
     * <ul>
     * <li>{@link HttpStatusException} - HTTP-aware exceptions: logged, model populated with error, rethrown</li>
     * <li>{@link ValidationException} - Validation failures: logged, model populated with error, NOT rethrown</li>
     * <li>{@link PolyglotException} - GraalVM JS errors: wrapped as {@link JsFlowExecutionException} with source location</li>
     * <li>{@link Exception} - Generic exceptions: logged and rethrown</li>
     * </ul>
     * <p>
     * Error information stored in model includes: {@code isError=true}, error message, full stack trace (if
     * {@link #FULL_STACKTRACE_IN_ERROR} is true), and exception object.
     * </p>
     * <p>
     * Post-execute processors: After successful execution, iterates through model values and invokes
     * {@link PostExecuteProcessablePageAttr#process()} for any attributes implementing that interface.
     * </p>
     *
     * @param model the page model map to populate
     * @return a carrier containing the final result, populated model, services, and parameters
     * @throws HttpStatusException if an HTTP-aware exception occurs (rethrown after logging)
     * @throws JsFlowExecutionException if a GraalVM PolyglotException occurs (wrapped and rethrown)
     * @throws Exception if any other exception occurs (rethrown after logging)
     */
    private ResultAndModel<O,CP> executeFlow(PageModelMap model) {
        try {
            O result = null;
            if(transactionalExecutor == null && transactionalExecutorProvider != null) {
                transactionalExecutor = transactionalExecutorProvider.get();
            }
            if(transactionalExecutor == null) {
                result = apply(constructResultAndModel(model, null, services, params));
            } else {
                transactionalExecutor.executeInTransaction(() -> apply(constructResultAndModel(model, null, services, params)));
            }
            model.put(isError, message, false, "");
            applyPostExecuteProcessors(model);
            return constructResultAndModel(model, result, services, params);
        } catch (HttpStatusException e) {
            logError(e);
            model.put(isError, message, error, exception, true, getSimpleErrorMessage(e), getMessageString(e), e);
            throw e;
        } catch (ValidationException e) {
            logError(e);
            model.put(isError, message, error, exception, true, getSimpleErrorMessage(e), getMessageString(e), e);
        } catch (PolyglotException e) {
            String simpleMessage = getSimpleErrorMessage(e);
            JsFlowExecutionException jsException;
            if (e.getSourceLocation() != null) {
                String code = e.getSourceLocation().getCharacters() != null ? e.getSourceLocation().getCharacters().toString() : null;
                String location = e.getSourceLocation().toString();
                jsException = new JsFlowExecutionException(simpleMessage, code, location);
            } else {
                jsException = new JsFlowExecutionException(simpleMessage, e);
            }
            model.put(isError, message, error, exception, true, getSimpleErrorMessage(jsException), getMessageString(jsException), jsException);
            throw jsException;
        } catch (Exception e) {
            logError(e);
            throw e;
        }
        applyPostExecuteProcessors(model);
        return constructResultAndModel(model, null, services, params);
    }

    /**
     * Logs exception details with truncated stack trace for diagnostics.
     * <p>
     * Logs the exception message and the first two stack trace elements to provide context without excessive
     * verbosity. Uses the {@link LoggingComponent#error} method for consistent logging.
     * </p>
     *
     * @param e the exception to log
     */
    private void logError(Exception e) {
        String messageString = getSimpleErrorMessage(e);
        StackTraceElement[] st = e.getStackTrace();
        String stLine1 = (st != null && st.length > 0) ? "" + st[0] : "N/A";
        String stLine2 = (st != null && st.length > 1) ? "" + st[1] : "N/A";
        error(e, "[execute] {} {} {}", messageString, stLine1, stLine2);
    }

    /**
     * Extracts a simple error message from an exception.
     * <p>
     * Returns the exception's message string, or "N/A" if the message is null.
     * </p>
     *
     * @param e the exception to extract message from
     * @return the exception message or "N/A" if null
     */
    private String getSimpleErrorMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "N/A";
    }

    /**
     * Invokes post-execute processing on model attributes that support it.
     * <p>
     * Iterates through all model values and invokes {@link PostExecuteProcessablePageAttr#process()} on any
     * attributes implementing that interface. This allows attributes to perform cleanup, validation, or
     * transformation after the pipeline completes successfully.
     * </p>
     *
     * @param model the page model map containing attributes to process
     */
    private void applyPostExecuteProcessors(PageModelMap model) {
        for (Object o : model.values()) {
            if (PostExecuteProcessablePageAttr.class.isAssignableFrom(o.getClass())) {
                ((PostExecuteProcessablePageAttr)o).process();
            }
        }
    }

    /**
     * Constructs an error message string with optional full stack trace.
     * <p>
     * If {@link #FULL_STACKTRACE_IN_ERROR} is true, returns the complete stack trace as a string. Otherwise,
     * returns only the exception class simple name. Used for populating error information in the model.
     * </p>
     *
     * @param e the exception to format
     * @return formatted error message with or without full stack trace
     */
    private String getMessageString(Exception e) {
        String messageString = e.getClass().getSimpleName();
        if(FULL_STACKTRACE_IN_ERROR) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            messageString = sw.toString();
        }
        return messageString;
    }

    /**
     * Sets the transactional executor for this Flow.
     * <p>
     * Configures the executor that will wrap pipeline execution in a transaction. Returns this Flow instance
     * for method chaining.
     * </p>
     *
     * @param transactionalExecutor the transactional executor to use
     * @return this Flow instance for chaining
     */
    public Flow<I, O,CP> setTransactionalExecutor(TransactionalExecutor transactionalExecutor) {
        this.transactionalExecutor = transactionalExecutor;
        return this;
    }

    /**
     * Sets the transactional executor provider for this Flow.
     * <p>
     * Configures a supplier that will lazily create the transactional executor when {@link #execute()} is called.
     * Returns this Flow instance for method chaining.
     * </p>
     *
     * @param transactionalExecutorProvider the supplier for transactional executor
     * @return this Flow instance for chaining
     */
    public Flow<I, O,CP> setTransactionalExecutorProvider(Supplier<TransactionalExecutor> transactionalExecutorProvider) {
        this.transactionalExecutorProvider = transactionalExecutorProvider;
        return this;
    }

    /**
     * Sets the configuration propagation hook for this Flow.
     * <p>
     * Configures a consumer that will be invoked when builder methods compose new flows, allowing custom
     * configuration to be propagated through the pipeline chain. Returns this Flow instance for chaining.
     * </p>
     *
     * @param onThen the configuration propagation consumer
     * @return this Flow instance for chaining
     */
    public Flow<I, O, CP> onThen(Consumer<Function> onThen) {
        this.onThen = onThen;
        return this;
    }

    /**
     * Returns a string representation of this Flow for debugging.
     * <p>
     * Format: "Flow-N" where N is the unique flow number assigned during construction.
     * </p>
     *
     * @return debug string identifying this Flow instance
     */
    @Override
    public String toString() {
        return "Flow-" + flowNumber;
    }

    /**
     * Composes a step that computes a value and stores it in the model under a string key.
     * <p>
     * Similar to {@link #thenSet(PageAttr, Function)} but uses a plain string key instead of a PageAttr.
     * Useful for dynamic model keys not defined as PageAttr constants.
     * </p>
     *
     * @param modelKey the string key for storing the value in the model
     * @param after the function to compute the value
     * @return a new Flow with the composed pipeline storing the value under the specified key
     * @throws NullPointerException if modelKey or after is null
     */
    public Flow<I,Object,CP> thenSet(String modelKey, Function<ResultAndModel<O,CP>, Object> after) {
        Objects.requireNonNull(after);
        copyTransactionalExecutorProvider(after);
        return constructFlow(params, services, (ResultAndModel<I, CP> t) -> t.model.put(modelKey , after.apply( constructResultAndModel(t.model, f.apply(t), t.services, params))), transactionalExecutorProvider, onThen);
    }
}
