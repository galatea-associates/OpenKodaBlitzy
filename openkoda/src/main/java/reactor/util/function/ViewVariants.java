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

package reactor.util.function;


import com.openkoda.core.flow.PageModelFunction;

import java.util.Optional;

/**
 * Typed Tuple4 specialization for view resolution with primary and fallback rendering paths.
 * <p>
 * ViewVariants is a specialized tuple that bundles four Optional components for view template resolution
 * and page model construction. It extends {@code Tuple4<Optional<String>, Optional<String>, Optional<PageModelFunction>, Optional<PageModelFunction>>}
 * with explicit semantic meaning for each tuple slot:

 * <ul>
 *   <li><b>Slot 1 (T1)</b>: Primary view template name wrapped in {@code Optional<String>} (e.g., {@code Optional.of("user-profile")})</li>
 *   <li><b>Slot 2 (T2)</b>: Fallback view template name wrapped in {@code Optional<String>} (e.g., {@code Optional.of("default-profile")})</li>
 *   <li><b>Slot 3 (T3)</b>: Primary {@link PageModelFunction} callback for model construction wrapped in Optional</li>
 *   <li><b>Slot 4 (T4)</b>: Fallback {@link PageModelFunction} callback as alternative wrapped in Optional</li>
 * </ul>
 * <p>
 * This class is used by view resolution code, controller adapters, and reactive pipelines to provide
 * flexible rendering strategies with automatic fallback when primary views or model functions are unavailable.

 * <p>
 * <b>Immutability:</b> ViewVariants instances are immutable if the underlying Tuple4 implementation
 * and Optional instances are immutable (which they are by default).

 * <p>
 * <b>Thread-Safety:</b> Instances are thread-safe assuming PageModelFunction callbacks do not capture
 * mutable state or perform non-thread-safe operations.

 * <p>
 * <b>Package Naming Note:</b> This class resides in the {@code reactor.util.function} namespace,
 * which mirrors Project Reactor packages. Despite the package name, this is an OpenKoda-specific
 * implementation for view variant management.

 * <p>
 * Example usage:
 * <pre>{@code
 * ViewVariants variants = ViewVariants.of(
 *     Optional.of("user-profile"), Optional.of("default"),
 *     Optional.of(model -> model.put("user", currentUser)), Optional.empty());
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see PageModelFunction
 * @see Tuple4
 */
public class ViewVariants extends Tuple4<Optional<String>, Optional<String>, Optional<PageModelFunction>, Optional<PageModelFunction>> {
    /**
     * Package-private constructor that delegates all four Optional components to the Tuple4 superclass.
     * <p>
     * This constructor follows a package-private visibility design pattern to enforce controlled instantiation.
     * No additional fields or state are declared beyond what Tuple4 provides. All tuple data is managed
     * by the parent class.

     * <p>
     * <b>Usage Note:</b> Clients should use the public static factory method {@link #of(Optional, Optional, Optional, Optional)}
     * for instantiation instead of direct construction. This design enforces consistent instantiation patterns
     * and provides a cleaner API surface.

     *
     * @param s Primary view template name wrapped in Optional
     * @param s2 Fallback view template name wrapped in Optional
     * @param pageModelFunction Primary PageModelFunction callback wrapped in Optional
     * @param pageModelFunction2 Fallback PageModelFunction callback wrapped in Optional
     */
    ViewVariants(Optional<String> s, Optional<String> s2, Optional<PageModelFunction> pageModelFunction, Optional<PageModelFunction> pageModelFunction2) {
        super(s, s2, pageModelFunction, pageModelFunction2);
    }

    /**
     * Creates an immutable ViewVariants bundle containing primary and fallback view components.
     * <p>
     * This factory method is the preferred way to instantiate ViewVariants. It bundles all four
     * components (primary view, fallback view, primary model function, fallback model function)
     * into a single immutable tuple for use in view resolution code, controller adapters, and
     * reactive pipelines.

     * <p>
     * Example usage:
     * <pre>{@code
     * ViewVariants variants = ViewVariants.of(
     *     Optional.of("user-profile"), Optional.of("default-profile"),
     *     Optional.of(model -> model.put("data", userData)), Optional.empty());
     * }</pre>

     *
     * @param t1 Primary view template name (e.g., {@code Optional.of("user-profile")}) used for primary rendering path
     * @param t2 Fallback view template name (e.g., {@code Optional.of("default-profile")}) used when primary view is unavailable
     * @param t3 Primary {@link PageModelFunction} callback for constructing the page model in the primary rendering path
     * @param t4 Fallback {@link PageModelFunction} callback for constructing the page model in the fallback rendering path
     * @return An immutable ViewVariants bundle containing all four components for view resolution
     * @see PageModelFunction
     */
    public static ViewVariants of(Optional<String> t1, Optional<String> t2, Optional<PageModelFunction> t3, Optional<PageModelFunction> t4) {
        return new ViewVariants(t1, t2, t3, t4);
    }

}
