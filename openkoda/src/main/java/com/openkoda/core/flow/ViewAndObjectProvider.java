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

import jakarta.servlet.http.HttpServletRequest;
import reactor.util.function.Tuple4;

import java.util.Optional;
import java.util.function.Function;

/**
 * Functional interface for request-aware view selection providing separate success and error view paths.
 * <p>
 * This interface enables conditional view selection based on HTTP request context by returning a 4-tuple
 * containing separate view names and result generators for both success and error execution paths.
 * The {@link PageModelMap#mav()} method consults this provider and examines {@link BasePageAttributes#isError}
 * to select the appropriate view and result generator.
 * </p>
 * <p>
 * The returned Tuple4 contains the following components:
 * </p>
 * <ul>
 *   <li><b>T1: Optional&lt;String&gt;</b> - View name for successful execution</li>
 *   <li><b>T2: Optional&lt;String&gt;</b> - View name for error execution</li>
 *   <li><b>T3: Optional&lt;PageModelFunction&gt;</b> - Result generator for success path</li>
 *   <li><b>T4: Optional&lt;PageModelFunction&gt;</b> - Result generator for error path</li>
 * </ul>
 * <p>
 * Example usage as a lambda implementation:
 * <pre>{@code
 * ViewAndObjectProvider provider = request ->
 *     Tuples.of(Optional.of("successView"), Optional.of("errorView"),
 *               Optional.of(model -> result), Optional.of(model -> error));
 * }</pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
* @since 08.08.17
 * @version 1.7.1
 * @see PageModelMap
 * @see PageModelFunction
 * @see BasePageAttributes
 */
public interface ViewAndObjectProvider extends Function<
        HttpServletRequest,
        Tuple4<
            Optional<String>, //viewname generator for success
            Optional<String>, //viewname generator for error
            Optional<PageModelFunction>, //result object generator for success
            Optional<PageModelFunction>  //result object generator for error
        >> { }
