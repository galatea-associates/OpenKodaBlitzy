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
 * Functional interface for computing results from accumulated model state in Flow pipelines.
 * <p>
 * This is a SAM (Single Abstract Method) interface designed for lambda-friendly transformation
 * of {@link PageModelMap} data into a result of type T. The interface enables concise, functional-style
 * programming when working with Flow pipelines where accumulated model state needs to be extracted
 * or transformed into a specific result type.
 * </p>
 * <p>
 * This interface is commonly used in Flow operations to map the current state of the model to a computed
 * result. As a functional interface, it supports lambda expressions, method references, and constructor
 * references, making it ideal for inline definitions in Flow pipeline chains.
 * </p>
 * <p>
 * Example usage in a Flow pipeline:
 * <pre>{@code
 * PageModelFunction<User> userExtractor = model -> model.get("currentUser");
 * User user = userExtractor.getResult(pageModelMap);
 * }</pre>
 * </p>
 *
 * @param <T> the type of result computed from the PageModelMap
 * @see PageModelMap
 * @see ResultAndModel
 * @see Flow
 * @since 1.7.1
 * @author OpenKoda Team
 */
@FunctionalInterface
public interface PageModelFunction<T> {
	
	/**
	 * Computes a result from the provided PageModelMap.
	 * <p>
	 * Extracts or transforms data from the accumulated model state to produce a result of type T.
	 * This method is invoked by Flow pipeline operations to retrieve computed values from the model.
	 * </p>
	 *
	 * @param model the PageModelMap containing accumulated state from the Flow pipeline, must not be null
	 * @return the computed result of type T extracted or derived from the model, may be null depending on implementation
	 */
	T getResult(PageModelMap model);
}
