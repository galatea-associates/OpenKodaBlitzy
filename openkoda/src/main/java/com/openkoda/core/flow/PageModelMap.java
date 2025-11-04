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
import org.springframework.web.servlet.ModelAndView;
import reactor.util.function.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.openkoda.core.flow.BasePageAttributes.bindingResult;
import static com.openkoda.core.flow.BasePageAttributes.isError;
import static java.util.Optional.ofNullable;

/**
 * A typed model container for view data passed from controllers to templates.
 * <p>
 * This class extends {@link HashMap} to store key-value pairs for Thymeleaf template rendering.
 * It provides type-safe access to model attributes using {@link PageAttr} keys, eliminating the need
 * for manual casting and reducing runtime errors. The container is created and populated by Flow pipelines
 * during request processing, then passed to views via {@link ModelAndView}.
 * <p>
 * Key features include:
 * <ul>
 *   <li>Type-safe get/put operations using PageAttr typed keys</li>
 *   <li>Multi-value operations returning Tuple2 through Tuple6 for batch retrieval</li>
 *   <li>Change tracking via internal 'added' and 'removed' maps</li>
 *   <li>Automatic view selection based on {@link BasePageAttributes#isError} flag</li>
 *   <li>Filtering of internal metadata (bindingResult) when converting to plain Map</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * PageModelMap model = new PageModelMap();
 * model.put(userAttr, currentUser);
 * User user = model.get(userAttr); // Type-safe retrieval
 * }</pre>
 * <p>
 * <b>Thread Safety:</b> This class is not synchronized. External synchronization is required
 * when accessed concurrently from multiple threads.
 * <p>
 * <b>Implementation Note:</b> Many methods use unchecked casts from Object to the target type T.
 * Type safety relies on correct usage of PageAttr keys with matching value types.
 *
 * @see Flow
 * @see PageAttr
 * @see BasePageAttributes
 * @see ViewAndObjectProvider
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class PageModelMap extends HashMap<String, Object> {

    private static final long serialVersionUID = -6492787151065187523L;

    /**
     * Tracks attributes added to this model during its lifecycle.
     * <p>
     * Records each key-value pair added via {@link #put(PageAttr, Object)} or {@link #put(String, Object)}.
     * Used for change tracking and debugging purposes. Cleared by {@link #clearTrace()}.
     * 
     */
    final Map<String, Object> added = new HashMap<>();
    
    /**
     * Tracks attributes removed from this model during its lifecycle.
     * <p>
     * Records each key-value pair removed via {@link #remove(PageAttr)} or when null values are set.
     * Used for change tracking and debugging purposes. Cleared by {@link #clearTrace()}.
     * 
     */
    final Map<String, Object> removed = new HashMap<>();

    /**
     * Retrieves a typed value from this model using a PageAttr key.
     * <p>
     * Provides type-safe access to model attributes without manual casting. Returns the value
     * associated with the given key's name, or null if no mapping exists.
     * 
     *
     * @param <T> the expected type of the value
     * @param key the PageAttr key identifying the attribute
     * @return the value associated with the key, or null if not present
     * @see #put(PageAttr, Object)
     * @see #getOrDefault(PageAttr, Object)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(PageAttr<T> key) {
        return (T) super.get(key.name);
    }

    /**
     * Converts and remaps a value from one PageAttr key to another using a transformation function.
     * <p>
     * Retrieves the value at {@code fromKey}, applies the transformation function, removes the original
     * key, and stores the result at {@code targetKey}. If the source value is null, no transformation occurs.
     * 
     *
     * @param <F> the source value type
     * @param <T> the target value type
     * @param fromKey the PageAttr key of the source value
     * @param targetKey the PageAttr key where the transformed value will be stored
     * @param remappingFunction the function to transform the source value
     * @return this PageModelMap for method chaining
     */
    public <F, T> PageModelMap convert(PageAttr<F> fromKey, PageAttr<T> targetKey, Function<F, T> remappingFunction) {
        F from = this.get(fromKey);
        if (from == null) {
            return this;
        } else {
            T result = remappingFunction.apply(from);
            this.remove(fromKey);
            this.put(targetKey, result);
            return this;
        }
    }

    /**
     * Converts this model to a plain Map containing specified attributes.
     * <p>
     * Creates a new HashMap with the {@link BasePageAttributes#isError} flag and all specified keys.
     * Automatically filters out the {@link BasePageAttributes#bindingResult} to prevent exposing
     * internal validation metadata to templates.
     * 
     *
     * @param keys the PageAttr keys to include in the result map
     * @return a new Map containing the specified attributes without bindingResult
     */
    public Map<String, Object> getAsMap(PageAttr... keys) {
        Map<String, Object> result = new HashMap<>(keys.length);
        result.put(isError.name, get(isError));
        for (PageAttr key : keys) {
            result.put(key.name, get(key));
        }
        result.remove(bindingResult.name);
        return result;
    }


    /**
     * Retrieves two typed values as a Tuple2.
     * <p>
     * Batch retrieval operation that returns two values in a single call. Uses unchecked casts
     * from Object to the expected types.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @return a Tuple2 containing both values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2> Tuple2<T1, T2> get(PageAttr<T1> key1, PageAttr<T2> key2) {
        return Tuples.of((T1) super.get(key1.name), (T2) super.get(key2.name));
    }

    /**
     * Retrieves three typed values as a Tuple3.
     * <p>
     * Batch retrieval operation that returns three values in a single call. Uses unchecked casts
     * from Object to the expected types.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @return a Tuple3 containing all three values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3> Tuple3<T1, T2, T3> get(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3) {
        return Tuples.of((T1) super.get(key1.name), (T2) super.get(key2.name), (T3) super.get(key3.name));
    }

    /**
     * Retrieves four typed values as a Tuple4.
     * <p>
     * Batch retrieval operation that returns four values in a single call. Uses unchecked casts
     * from Object to the expected types.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param <T4> the type of the fourth value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param key4 the PageAttr key for the fourth value
     * @return a Tuple4 containing all four values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> get(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, PageAttr<T4> key4) {
        return Tuples.of((T1) super.get(key1.name), (T2) super.get(key2.name), (T3) super.get(key3.name), (T4) super.get(key4.name));
    }

    /**
     * Retrieves five typed values as a Tuple5.
     * <p>
     * Batch retrieval operation that returns five values in a single call. Uses unchecked casts
     * from Object to the expected types.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param <T4> the type of the fourth value
     * @param <T5> the type of the fifth value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param key4 the PageAttr key for the fourth value
     * @param key5 the PageAttr key for the fifth value
     * @return a Tuple5 containing all five values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> get(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, PageAttr<T4> key4, PageAttr<T5> key5) {
        return Tuples.of((T1) super.get(key1.name), (T2) super.get(key2.name), (T3) super.get(key3.name), (T4) super.get(key4.name), (T5) super.get(key5.name));
    }

    /**
     * Retrieves six typed values as a Tuple6.
     * <p>
     * Batch retrieval operation that returns six values in a single call. Uses unchecked casts
     * from Object to the expected types.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param <T4> the type of the fourth value
     * @param <T5> the type of the fifth value
     * @param <T6> the type of the sixth value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param key4 the PageAttr key for the fourth value
     * @param key5 the PageAttr key for the fifth value
     * @param key6 the PageAttr key for the sixth value
     * @return a Tuple6 containing all six values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> get(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, PageAttr<T4> key4, PageAttr<T5> key5, PageAttr<T6> key6) {
        return Tuples.of((T1) super.get(key1.name), (T2) super.get(key2.name), (T3) super.get(key3.name), (T4) super.get(key4.name), (T5) super.get(key5.name), (T6) super.get(key6.name));
    }

    /**
     * Stores a typed value using a PageAttr key.
     * <p>
     * Type-safe put operation that delegates to {@link #put(String, Object)}.
     * Records the operation in the {@link #added} map for change tracking.
     * 
     *
     * @param <T> the type of the value
     * @param key the PageAttr key identifying the attribute
     * @param value the value to store, or null to remove the mapping
     * @return the stored value
     * @see #get(PageAttr)
     */
    @SuppressWarnings("unchecked")
    public <T> T put(PageAttr<T> key, T value) {
        return (T) this.put(key.name, value);
    }

    /**
     * Stores a value with change tracking.
     * <p>
     * Overrides HashMap's put to track changes. When value is null, removes the mapping and records
     * it in {@link #removed}. Otherwise, stores the value and records it in {@link #added}.
     * 
     *
     * @param key the attribute name
     * @param value the value to store, or null to remove the mapping
     * @return the stored value, or the removed value if value was null
     */
    public Object put(String key, Object value) {
        if (value == null) {
            Object t = super.remove(key);
            if (t != null) {
                removed.put(key, t);
            }
            return t;
        }

        added.put(key, value);
        super.put(key, value);
        return value;
    }

    /**
     * Stores two typed values from a Tuple2.
     * <p>
     * Batch put operation that stores the tuple components at their respective keys.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param value the Tuple2 containing both values
     * @return a Tuple2 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2> Tuple2<T1, T2> put(PageAttr<T1> key1, PageAttr<T2> key2, Tuple2<T1, T2> value) {
        return Tuples.of(this.put(key1, value.getT1()), this.put(key2, value.getT2()));
    }

    /**
     * Stores two typed values from a generic Tuple with unchecked casts.
     * <p>
     * Batch put operation accepting a generic Tuple. Uses unchecked casts to extract values.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param value the generic Tuple containing both values
     * @return a Tuple2 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2> Tuple2<T1, T2> put(PageAttr<T1> key1, PageAttr<T2> key2, Tuple value) {
        return Tuples.of(this.put(key1, (T1) value.getT1()), this.put(key2, (T2) value.getT2()));
    }

    /**
     * Stores two typed values at specified keys.
     * <p>
     * Batch put operation that stores individual values at their respective keys.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param v1 the first value
     * @param v2 the second value
     * @return a Tuple2 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2> Tuple2<T1, T2> put(PageAttr<T1> key1, PageAttr<T2> key2, T1 v1, T2 v2) {
        return Tuples.of(this.put(key1, v1), this.put(key2, v2));
    }

    /**
     * Stores three typed values at specified keys.
     * <p>
     * Batch put operation that stores three individual values at their respective keys.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param v1 the first value
     * @param v2 the second value
     * @param v3 the third value
     * @return a Tuple3 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3> Tuple3<T1, T2, T3> put(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, T1 v1, T2 v2, T3 v3) {
        return Tuples.of(this.put(key1, v1), this.put(key2, v2), this.put(key3, v3));
    }

    /**
     * Stores three typed values from a Tuple3.
     * <p>
     * Batch put operation that stores the tuple components at their respective keys.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param value the Tuple3 containing all three values
     * @return a Tuple3 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3> Tuple3<T1, T2, T3> put(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, Tuple3<T1, T2, T3> value) {
        return Tuples.of(this.put(key1, value.getT1()), this.put(key2, value.getT2()), this.put(key3, value.getT3()));
    }

    /**
     * Stores three typed values from a generic Tuple with unchecked casts.
     * <p>
     * Batch put operation accepting a generic Tuple. Uses unchecked casts to extract values.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param value the generic Tuple containing all three values
     * @return a Tuple3 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3> Tuple3<T1, T2, T3> put(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, Tuple value) {
        return Tuples.of(this.put(key1, (T1) value.getT1()), this.put(key2, (T2) value.getT2()), this.put(key3, (T3) value.getT3()));
    }

    /**
     * Stores four typed values from a Tuple4.
     * <p>
     * Batch put operation that stores the tuple components at their respective keys.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param <T4> the type of the fourth value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param key4 the PageAttr key for the fourth value
     * @param value the Tuple4 containing all four values
     * @return a Tuple4 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> put(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, PageAttr<T4> key4, Tuple4<T1, T2, T3, T4> value) {
        return Tuples.of(this.put(key1, value.getT1()), this.put(key2, value.getT2()), this.put(key3, value.getT3()), this.put(key4, value.getT4()));
    }

    /**
     * Stores four typed values from a generic Tuple with unchecked casts.
     * <p>
     * Batch put operation accepting a generic Tuple. Uses unchecked casts to extract values.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param <T4> the type of the fourth value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param key4 the PageAttr key for the fourth value
     * @param value the generic Tuple containing all four values
     * @return a Tuple4 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> put(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, PageAttr<T4> key4, Tuple value) {
        return Tuples.of(this.put(key1, (T1) value.getT1()), this.put(key2, (T2) value.getT2()), this.put(key3, (T3) value.getT3()), this.put(key4, (T4) value.getT4()));
    }

    /**
     * Stores four typed values at specified keys.
     * <p>
     * Batch put operation that stores four individual values at their respective keys.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param <T4> the type of the fourth value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param key4 the PageAttr key for the fourth value
     * @param v1 the first value
     * @param v2 the second value
     * @param v3 the third value
     * @param v4 the fourth value
     * @return a Tuple4 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> put(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, PageAttr<T4> key4, T1 v1, T2 v2, T3 v3, T4 v4) {
        return Tuples.of(this.put(key1, v1), this.put(key2, v2), this.put(key3, v3), this.put(key4, v4));
    }

    /**
     * Stores five typed values from a Tuple5.
     * <p>
     * Batch put operation that stores the tuple components at their respective keys.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param <T4> the type of the fourth value
     * @param <T5> the type of the fifth value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param key4 the PageAttr key for the fourth value
     * @param key5 the PageAttr key for the fifth value
     * @param value the Tuple5 containing all five values
     * @return a Tuple5 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> put(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, PageAttr<T4> key4, PageAttr<T5> key5, Tuple5<T1, T2, T3, T4, T5> value) {
        return Tuples.of(this.put(key1, value.getT1()), this.put(key2, value.getT2()), this.put(key3, value.getT3()), this.put(key4, value.getT4()), this.put(key5, value.getT5()));
    }

    /**
     * Stores five typed values from a generic Tuple with unchecked casts.
     * <p>
     * Batch put operation accepting a generic Tuple. Uses unchecked casts to extract values.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param <T4> the type of the fourth value
     * @param <T5> the type of the fifth value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param key4 the PageAttr key for the fourth value
     * @param key5 the PageAttr key for the fifth value
     * @param value the generic Tuple containing all five values
     * @return a Tuple5 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> put(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, PageAttr<T4> key4, PageAttr<T5> key5, Tuple value) {
        return Tuples.of(this.put(key1, (T1) value.getT1()), this.put(key2, (T2) value.getT2()), this.put(key3, (T3) value.getT3()), this.put(key4, (T4) value.getT4()), this.put(key5, (T5) value.getT5()));
    }

    /**
     * Stores five typed values at specified keys.
     * <p>
     * Batch put operation that stores five individual values at their respective keys.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param <T4> the type of the fourth value
     * @param <T5> the type of the fifth value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param key4 the PageAttr key for the fourth value
     * @param key5 the PageAttr key for the fifth value
     * @param v1 the first value
     * @param v2 the second value
     * @param v3 the third value
     * @param v4 the fourth value
     * @param v5 the fifth value
     * @return a Tuple5 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> put(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, PageAttr<T4> key4, PageAttr<T5> key5, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5) {
        return Tuples.of(this.put(key1, v1), this.put(key2, v2), this.put(key3, v3), this.put(key4, v4), this.put(key5, v5));
    }


    /**
     * Stores six typed values from a Tuple6.
     * <p>
     * Batch put operation that stores the tuple components at their respective keys.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param <T4> the type of the fourth value
     * @param <T5> the type of the fifth value
     * @param <T6> the type of the sixth value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param key4 the PageAttr key for the fourth value
     * @param key5 the PageAttr key for the fifth value
     * @param key6 the PageAttr key for the sixth value
     * @param value the Tuple6 containing all six values
     * @return a Tuple6 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> put(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, PageAttr<T4> key4, PageAttr<T5> key5, PageAttr<T6> key6, Tuple6<T1, T2, T3, T4, T5, T6> value) {
        return Tuples.of(this.put(key1, value.getT1()), this.put(key2, value.getT2()), this.put(key3, value.getT3()), this.put(key4, value.getT4()), this.put(key5, value.getT5()), this.put(key6, value.getT6()));
    }

    /**
     * Stores six typed values from a generic Tuple with unchecked casts.
     * <p>
     * Batch put operation accepting a generic Tuple. Uses unchecked casts to extract values.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param <T4> the type of the fourth value
     * @param <T5> the type of the fifth value
     * @param <T6> the type of the sixth value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param key4 the PageAttr key for the fourth value
     * @param key5 the PageAttr key for the fifth value
     * @param key6 the PageAttr key for the sixth value
     * @param value the generic Tuple containing all six values
     * @return a Tuple6 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> put(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, PageAttr<T4> key4, PageAttr<T5> key5, PageAttr<T6> key6, Tuple value) {
        return Tuples.of(this.put(key1, (T1) value.getT1()), this.put(key2, (T2) value.getT2()), this.put(key3, (T3) value.getT3()), this.put(key4, (T4) value.getT4()), this.put(key5, (T5) value.getT5()), this.put(key6, (T6) value.getT6()));
    }

    /**
     * Stores six typed values at specified keys.
     * <p>
     * Batch put operation that stores six individual values at their respective keys.
     * 
     *
     * @param <T1> the type of the first value
     * @param <T2> the type of the second value
     * @param <T3> the type of the third value
     * @param <T4> the type of the fourth value
     * @param <T5> the type of the fifth value
     * @param <T6> the type of the sixth value
     * @param key1 the PageAttr key for the first value
     * @param key2 the PageAttr key for the second value
     * @param key3 the PageAttr key for the third value
     * @param key4 the PageAttr key for the fourth value
     * @param key5 the PageAttr key for the fifth value
     * @param key6 the PageAttr key for the sixth value
     * @param v1 the first value
     * @param v2 the second value
     * @param v3 the third value
     * @param v4 the fourth value
     * @param v5 the fifth value
     * @param v6 the sixth value
     * @return a Tuple6 of the stored values
     */
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> put(PageAttr<T1> key1, PageAttr<T2> key2, PageAttr<T3> key3, PageAttr<T4> key4, PageAttr<T5> key5, PageAttr<T6> key6, T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6) {
        return Tuples.of(this.put(key1, v1), this.put(key2, v2), this.put(key3, v3), this.put(key4, v4), this.put(key5, v5), this.put(key6, v6));
    }


    /**
     * Removes a value using a PageAttr key and tracks the removal.
     * <p>
     * Type-safe removal that records the removed value in the {@link #removed} map for change tracking.
     * 
     *
     * @param <T> the expected type of the removed value
     * @param key the PageAttr key identifying the attribute to remove
     * @return the removed value, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T remove(PageAttr<T> key) {
        T t = (T) super.remove(key.name);
        removed.put(key.name, t);
        return t;
    }

    /**
     * Retrieves a typed value or returns a default if not present.
     * <p>
     * Type-safe access with fallback value. Uses unchecked cast to the expected type.
     * 
     *
     * @param <T> the expected type of the value
     * @param key the PageAttr key identifying the attribute
     * @param defaultValue the value to return if no mapping exists
     * @return the value associated with the key, or the default value
     * @see #get(PageAttr)
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(PageAttr<T> key, Object defaultValue) {
        return (T) super.getOrDefault(key.name, defaultValue);
    }

    /**
     * Checks whether this model contains a mapping for the given key.
     *
     * @param <T> the type associated with the key
     * @param key the PageAttr key to check
     * @return true if a mapping exists, false otherwise
     */
    public <T> boolean has(PageAttr<T> key) {
        return super.containsKey(key.name);
    }

    /**
     * Clears the change tracking maps.
     * <p>
     * Removes all entries from {@link #added} and {@link #removed}, resetting change tracking
     * without affecting the model's actual contents.
     * 
     */
    void clearTrace() {
        added.clear();
        removed.clear();
    }

    /**
     * Checks whether validation passed for this model.
     * <p>
     * Always returns false. This method exists for subclass overrides that track validation state.
     * 
     *
     * @return false in this base implementation
     */
    public boolean isValidationOk() {
        return false;
    }

    /**
     * Creates a model and view result using the request context.
     * <p>
     * Returns this model directly without view selection. Delegates to {@link #mav(HttpServletRequest, ViewAndObjectProvider)}
     * with a null provider.
     * 
     *
     * @param request the HTTP servlet request
     * @return this PageModelMap instance
     */
    public Object mav(HttpServletRequest request) {
        return mav(request, null);
    }

    /**
     * Creates a ModelAndView or result object based on validation state with full customization.
     * <p>
     * Consults {@link BasePageAttributes#isError} to select between success and error paths.
     * Allows specifying both view names and custom result providers for each outcome.
     * 
     *
     * @param request the HTTP servlet request
     * @param viewNameForSuccess view name used when validation passes
     * @param viewNameForValidationError view name used when validation fails
     * @param forSuccess function to generate result when validation passes
     * @param forValidationError function to generate result when validation fails
     * @return ModelAndView or custom result based on isError flag
     */
    public Object mav(
            HttpServletRequest request,
            String viewNameForSuccess, String viewNameForValidationError,
            PageModelFunction<Object> forSuccess, PageModelFunction<Object> forValidationError
            ) {
        return mav(request, (r) -> Tuples.of(
                ofNullable(viewNameForSuccess),
                ofNullable(viewNameForValidationError),
                ofNullable(forSuccess),
                ofNullable(forValidationError)));
    }

    /**
     * Creates a result using the same view and function for both success and error cases.
     * <p>
     * Convenience method when success and error handling are identical. Typically used for
     * JSON responses where the same function generates output regardless of validation state.
     * 
     *
     * @param request the HTTP servlet request
     * @param viewName view name used for both success and error
     * @param forJson function to generate result for both outcomes
     * @return ModelAndView or custom result
     */
    public Object mav(
            HttpServletRequest request,
            String viewName,
            PageModelFunction<Object> forJson
            ) {
        return mav(request, (r) -> Tuples.of(
                ofNullable(viewName),
                ofNullable(viewName),
                ofNullable(forJson),
                ofNullable(forJson)));
    }

    /**
     * Core view selection logic based on error state and provider configuration.
     * <p>
     * Implements the decision tree for view selection:
     * <ol>
     *   <li>If viewProvider is null, returns this model directly</li>
     *   <li>If {@link BasePageAttributes#isError} is true and error view name exists, returns ModelAndView with error view</li>
     *   <li>If isError is true and error function exists, invokes error function</li>
     *   <li>If success view name exists, returns ModelAndView with success view</li>
     *   <li>If success function exists, invokes success function</li>
     *   <li>Otherwise, returns this model</li>
     * </ol>
     * 
     *
     * @param request the HTTP servlet request
     * @param viewProvider function providing view names and result functions
     * @return ModelAndView, custom result object, or this model
     */
    private Object mav(
            HttpServletRequest request,
            ViewAndObjectProvider viewProvider) {
        PageModelMap model = this;
        if (viewProvider == null) {
            return model;
        }

        boolean error = model.has(isError) ? model.get(isError) : false;

        Tuple4<
                Optional<String>,
                Optional<String>,
                Optional<PageModelFunction>,
                Optional<PageModelFunction>> p
                = viewProvider.apply(request);

        if (error) {
            if (p.getT2().isPresent()) {
                //there is error and we know view name
                return new ModelAndView(p.getT2().get(), model);
            }

            if (p.getT4().isPresent()) {
                //there is error and we know what model we want to return
                return p.getT4().get().getResult(model);
            }
        }

        if (p.getT1().isPresent()) {
            //no error or no error handler and we know view name
            return new ModelAndView(p.getT1().get(), model);
        }

        if (p.getT3().isPresent()) {
            //no error or no error handler and we know what model we want to return
            return p.getT3().get().getResult(model);
        }

        return model;
    }


    /**
     * Creates a ModelAndView based on the {@link BasePageAttributes#isError} flag.
     * <p>
     * Checks the {@code isError} attribute to select between success and error views.
     * Returns a ModelAndView with this model as the view model data.
     * 
     *
     * @param viewNameForSuccess view name used when isError is false
     * @param viewNameForValidationError view name used when isError is true
     * @return ModelAndView with the selected view and this model
     */
    public ModelAndView mav(String viewNameForSuccess, String viewNameForValidationError) {
        PageModelMap model = this;
        return model.get(isError) ? new ModelAndView(viewNameForValidationError, model) : new ModelAndView(viewNameForSuccess, model);
    }

    /**
     * Returns a result object based on the {@link BasePageAttributes#isError} flag.
     * <p>
     * Invokes the appropriate function provider depending on the isError state.
     * Useful for generating JSON responses or custom result objects.
     * 
     *
     * @param forSuccess function invoked when isError is false
     * @param forValidationError function invoked when isError is true
     * @return the result object from the selected function
     */
    public Object mav(PageModelFunction<Object> forSuccess, PageModelFunction<Object> forValidationError) {
        PageModelMap model = this;
        return model.get(isError) ? forValidationError.getResult(model) : forSuccess.getResult(model);
    }

    /**
     * Returns a custom result or ModelAndView based on the {@link BasePageAttributes#isError} flag.
     * <p>
     * Invokes the success function when isError is false, or creates a ModelAndView with the error view
     * when isError is true. Useful for JSON success responses with fallback to error views.
     * 
     *
     * @param forSuccess function invoked when isError is false
     * @param viewNameForValidationError view name used when isError is true
     * @return custom result object or ModelAndView
     */
    public Object mav(PageModelFunction<Object> forSuccess, String viewNameForValidationError) {
        PageModelMap model = this;
        return model.get(isError) ? new ModelAndView(viewNameForValidationError, model) : forSuccess.getResult(model);
    }

    /**
     * Returns a ModelAndView or custom result based on the {@link BasePageAttributes#isError} flag.
     * <p>
     * Creates a ModelAndView with the success view when isError is false, or invokes the error function
     * when isError is true. Useful for standard success views with custom error handling.
     * 
     *
     * @param viewNameForSuccess view name used when isError is false
     * @param forValidationError function invoked when isError is true
     * @return ModelAndView or custom result object
     */
    public Object mav(String viewNameForSuccess, PageModelFunction<Object> forValidationError) {
        PageModelMap model = this;
        return model.get(isError) ? forValidationError.getResult(model) : new ModelAndView(viewNameForSuccess, model);
    }

    /**
     * Returns a result object using the same function regardless of validation state.
     * <p>
     * Convenience method when the same result provider applies to both success and error cases.
     * Delegates to {@link #mav(PageModelFunction, PageModelFunction)} with the same function for both.
     * 
     *
     * @param resultProvider function to generate the result
     * @return the result object from the function
     */
    public Object mav(PageModelFunction<Object> resultProvider) {
        return mav(resultProvider, resultProvider);
    }

    /**
     * Creates a ModelAndView using the same view name regardless of validation state.
     * <p>
     * Convenience method when the same view applies to both success and error cases.
     * Delegates to {@link #mav(String, String)} with the same view name for both.
     * 
     *
     * @param viewName name of the view
     * @return ModelAndView with the specified view and this model
     */
    public ModelAndView mav(String viewName) {
        return mav(viewName, viewName);
    }


}