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

/**
 * Functional interface for lambda-friendly mapping from {@link ResultAndModel} to result type.
 * <p>
 * This interface is used in Flow pipelines to transform intermediate results and model state
 * into new results. It enables concise lambda expressions that extract or compute values from
 * the combined result and model context.
 * <p>
 * Example usage in Flow pipeline:
 * <pre>{@code
 * ResultAndModelFunction<User, Organization> mapper = 
 *     (resultAndModel) -> resultAndModel.result;
 * }</pre>
 * 
 * @param <R> the result type returned by this function
 * @param <I> the input result type from ResultAndModel
 * @see ResultAndModel
 * @see PageModelFunction
 * @since 1.7.1
 * @author OpenKoda Team
 */
public interface ResultAndModelFunction<R, I> {
	
	/**
	 * Gets the result value by transforming the provided ResultAndModel.
	 * <p>
	 * This method extracts or computes a value of type R from the ResultAndModel
	 * carrier, which bundles the intermediate result, model data, services, and parameters.
	 * 
	 * 
	 * @param model the ResultAndModel containing result, PageModelMap, services and parameters
	 * @return the computed or extracted result of type R
	 */
	R getResult(ResultAndModel<I, ?> model);
}
