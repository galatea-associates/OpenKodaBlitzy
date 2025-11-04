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

import java.util.Map;

/**
 * Result carrier combining HTTP response/result with view model data, enabling functional
 * composition in Flow pipelines.
 * <p>
 * This class bundles all context needed for result transformation during Flow pipeline execution.
 * It packages the result value, the accumulated PageModelMap containing view data, service context,
 * and additional parameters into a single immutable carrier. Flow steps receive ResultAndModel
 * instances as input and produce new results through functional transformations.
 * <p>
 * The {@link #mav(ResultAndModelFunction, ResultAndModelFunction)} method dispatches to success
 * or error ResultAndModelFunction providers based on {@link BasePageAttributes#isError} flag,
 * enabling conditional result generation for successful vs failed executions.
 * <p>
 * Example usage in Flow pipeline:
 * <pre>{@code
 * Flow.init(services)
 *     .thenSet(entity, a -> repository.findOne(id))
 *     .then(a -> new Result(a.result))
 *     .execute();
 * }</pre>
 *
 * @param <R> the result type carried by this instance
 * @param <CP> the context/services type (typically a services aggregator bean)
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @since 2016-09-14
 * @see PageModelMap
 * @see ResultAndModelFunction
 * @see BasePageAttributes
 * @see Flow
 */
public class ResultAndModel<R, CP> {
    /**
     * Object creation counter for diagnostic monitoring and memory leak detection.
     */
    private static int objCount = 0;
    
    /**
     * The PageModelMap containing accumulated view data from Flow pipeline execution.
     * This model is passed to Thymeleaf templates for rendering.
     */
    public final PageModelMap model;
    
    /**
     * The result value of type R produced by the current Flow step.
     */
    public final R result;
    
    /**
     * The service context providing access to application services and repositories.
     */
    public final CP services;
    
    /**
     * Additional parameters map for passing extra context through the Flow pipeline.
     */
    public final Map<String, Object> params;
    
    /**
     * Constructs a new ResultAndModel carrier bundling result, model, services and parameters.
     *
     * @param model the PageModelMap containing view data
     * @param result the result value of type R
     * @param services the service context of type CP
     * @param params additional parameters map
     */
    protected ResultAndModel(PageModelMap model, R result, CP services, Map<String, Object> params) {
        this.result = result;
        this.model = model;
        this.services = services;
        this.params = params;
        objCount++;
    }


    /**
     * Dispatches to success or error result provider based on BasePageAttributes.isError flag.
     * <p>
     * This method checks the {@link BasePageAttributes#isError} flag in the model to determine
     * which ResultAndModelFunction to invoke. If isError is true (validation or execution failed),
     * forValidationError is called. Otherwise, forSuccess is called. This enables conditional
     * result generation based on Flow execution outcome.
     * 
     *
     * @param <O> the output type to be produced
     * @param forSuccess result provider invoked when isError is false (execution succeeded)
     * @param forValidationError result provider invoked when isError is true (execution failed)
     * @return the object produced by the selected provider
     * @see BasePageAttributes#isError
     * @see ResultAndModelFunction
     */
    public <O> O mav(ResultAndModelFunction<O, R> forSuccess, ResultAndModelFunction<O, R> forValidationError) {
        return model.get(BasePageAttributes.isError) ? forValidationError.getResult(this) : forSuccess.getResult(this);
    }

    /**
     * Returns result using the same provider regardless of validation outcome.
     * <p>
     * This convenience method invokes the provided ResultAndModelFunction whether isError is
     * true or false. Use this when the same result transformation applies to both successful
     * and failed executions.
     * 
     *
     * @param <O> the output type to be produced
     * @param forSuccessAndError result provider invoked regardless of isError flag
     * @return the object produced by the provider
     * @see #mav(ResultAndModelFunction, ResultAndModelFunction)
     */
    public <O> O mav(ResultAndModelFunction<O, R> forSuccessAndError) {
        return mav(forSuccessAndError, forSuccessAndError);
    }

    /**
     * Returns the total number of ResultAndModel instances created since JVM startup.
     * <p>
     * This diagnostic counter helps monitor ResultAndModel allocation for memory leak detection
     * and performance analysis. The counter increments on every constructor invocation and is
     * never reset during JVM lifetime.
     * 
     *
     * @return the total count of ResultAndModel objects created
     */
    public static int getObjectCount() {
        return objCount;
    }
}
