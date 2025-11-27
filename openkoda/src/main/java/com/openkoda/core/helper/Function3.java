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

package com.openkoda.core.helper;

/**
 * Functional interface for three-argument functions.
 * <p>
 * This interface extends Java's functional programming support beyond the standard
 * {@link java.util.function.BiFunction} by accepting three input parameters instead of two.
 * It represents a function that takes three arguments and produces a result.
 * <p>
 * This is a functional interface whose functional method is {@link #apply(Object, Object, Object)}.
 * As such, it can be used as the assignment target for lambda expressions or method references.
 * <p>
 * Example usage with lambda:
 * <pre>{@code
 * Function3<String, Integer, Boolean, String> formatter =
 *     (s, i, b) -> s + i + (b ? " active" : " inactive");
 * String result = formatter.apply("User", 42, true);
 * }</pre>
 * <p>
 * <strong>Thread Safety:</strong> This interface is stateless and inherently thread-safe.
 * However, thread-safety of specific implementations depends on the lambda or method reference used.
 *
 * @param <T1> the type of the first argument to the function
 * @param <T2> the type of the second argument to the function
 * @param <T3> the type of the third argument to the function
 * @param <R> the type of the result of the function
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see java.util.function.BiFunction
 */
@FunctionalInterface
public interface Function3<T1, T2, T3, R> {

    /**
     * Applies this function to the given arguments.
     * <p>
     * This is the functional method that processes three input arguments and produces a single result.
     * The behavior of this method is defined by the lambda expression or method reference
     * that implements this interface.
     * 
     *
     * @param t1 the first function argument
     * @param t2 the second function argument
     * @param t3 the third function argument
     * @return the function result computed from the three input arguments
     */
    R apply(T1 t1, T2 t2, T3 t3);

}
