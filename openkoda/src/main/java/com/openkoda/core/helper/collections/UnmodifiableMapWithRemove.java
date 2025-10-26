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

package com.openkoda.core.helper.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An unmodifiable map implementation using the decorator pattern that wraps an underlying HashMap.
 * <p>
 * This class provides selective mutability by allowing {@code remove()} operations while blocking
 * all add and update operations. Any attempt to add or update entries via methods such as
 * {@code put()}, {@code putAll()}, {@code replace()}, or compute methods will result in an
 * {@link UnsupportedOperationException}.
 * </p>
 * <p>
 * This implementation is useful when you need to provide a map that can be pruned but not
 * expanded or modified, such as for managing cached data that can be invalidated but not
 * added to by downstream consumers.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * Map<String, Integer> source = new HashMap<>();
 * source.put("key1", 100);
 * Map<String, Integer> restrictedMap = new UnmodifiableMapWithRemove<>(source);
 * restrictedMap.remove("key1"); // Allowed
 * restrictedMap.put("key2", 200); // Throws UnsupportedOperationException
 * }</pre>
 * </p>
 * <p>
 * Thread-safety: This class is not thread-safe. External synchronization is required if
 * instances are accessed by multiple threads concurrently.
 * </p>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see UnmodifiableSetWithRemove
 * @see java.util.HashMap
 */
public class UnmodifiableMapWithRemove<K, V> extends HashMap<K, V> {

    /**
     * Throws UnsupportedOperationException to prevent adding or updating entries.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return never returns normally
     * @throws UnsupportedOperationException always, as this map does not support add/update operations
     */
    @Override
    public V put(K key, V value) { throw new UnsupportedOperationException(); }

    /**
     * Throws UnsupportedOperationException to prevent bulk additions or updates.
     *
     * @param m mappings to be stored in this map
     * @throws UnsupportedOperationException always, as this map does not support add/update operations
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) { throw new UnsupportedOperationException(); }

    /**
     * Throws UnsupportedOperationException to prevent conditional additions.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return never returns normally
     * @throws UnsupportedOperationException always, as this map does not support add/update operations
     */
    @Override
    public V putIfAbsent(K key, V value) { throw new UnsupportedOperationException(); }

    /**
     * Throws UnsupportedOperationException to prevent conditional replacement of values.
     *
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return never returns normally
     * @throws UnsupportedOperationException always, as this map does not support add/update operations
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) { throw new UnsupportedOperationException(); }

    /**
     * Throws UnsupportedOperationException to prevent replacement of values.
     *
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return never returns normally
     * @throws UnsupportedOperationException always, as this map does not support add/update operations
     */
    @Override
    public V replace(K key, V value) { throw new UnsupportedOperationException(); }

    /**
     * Throws UnsupportedOperationException to prevent merging values.
     *
     * @param key key with which the resulting value is to be associated
     * @param value the non-null value to be merged with the existing value
     * @param remappingFunction the function to recompute a value if present
     * @return never returns normally
     * @throws UnsupportedOperationException always, as this map does not support add/update operations
     */
    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) { throw new UnsupportedOperationException(); }

    /**
     * Throws UnsupportedOperationException to prevent bulk replacement of values.
     *
     * @param function the function to apply to each entry
     * @throws UnsupportedOperationException always, as this map does not support add/update operations
     */
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) { throw new UnsupportedOperationException(); }

    /**
     * Throws UnsupportedOperationException to prevent computed additions.
     *
     * @param key key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return never returns normally
     * @throws UnsupportedOperationException always, as this map does not support add/update operations
     */
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) { throw new UnsupportedOperationException(); }

    /**
     * Throws UnsupportedOperationException to prevent computed updates of existing entries.
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return never returns normally
     * @throws UnsupportedOperationException always, as this map does not support add/update operations
     */
    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) { throw new UnsupportedOperationException(); }

    /**
     * Throws UnsupportedOperationException to prevent computed mapping operations.
     *
     * @param key key with which the computed value is to be associated
     * @param remappingFunction the function to compute a value
     * @return never returns normally
     * @throws UnsupportedOperationException always, as this map does not support add/update operations
     */
    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) { throw new UnsupportedOperationException(); }

    /**
     * Returns a wrapped view of the entries contained in this map.
     * <p>
     * The returned set wraps the underlying entry set using {@link UnmodifiableSetWithRemove},
     * which blocks direct additions via {@code add()} but permits iterator-based removal.
     * </p>
     *
     * @return a set view of the mappings contained in this map, wrapped to prevent additions
     */
    public Set entrySet() { return new UnmodifiableSetWithRemove(super.entrySet()); }

    /**
     * Returns a wrapped view of the keys contained in this map.
     * <p>
     * The returned set wraps the underlying key set using {@link UnmodifiableSetWithRemove},
     * which blocks direct additions via {@code add()} but permits iterator-based removal.
     * </p>
     *
     * @return a set view of the keys contained in this map, wrapped to prevent additions
     */
    public Set keySet() { return new UnmodifiableSetWithRemove(super.keySet()); }

    /**
     * Returns a wrapped view of the values contained in this map.
     * <p>
     * The returned collection wraps the underlying value collection using
     * {@link UnmodifiableSetWithRemove}. Note that while values are typically returned as a
     * Collection, this implementation wraps them in a Set-backed wrapper for consistency with
     * other view methods, blocking direct additions but permitting iterator-based removal.
     * </p>
     *
     * @return a collection view of the values contained in this map, wrapped to prevent additions
     */
    public Collection values() { return new UnmodifiableSetWithRemove(super.values()); }

    /**
     * Constructs an UnmodifiableMapWithRemove by copying entries from the source map.
     * <p>
     * This constructor creates a new HashMap-backed instance with preallocated capacity based
     * on the source map size. It uses {@code super.put()} to bypass the overridden {@code put()}
     * method during initialization, allowing the map to be populated before enforcing the
     * add/update restrictions.
     * </p>
     *
     * @param m the source map whose entries are to be copied (must not be null)
     */
    public UnmodifiableMapWithRemove(Map<K, V> m) {
        super(m.size());
        for (Entry<K, V> e : m.entrySet()) super.put(e.getKey(), e.getValue());
    }
}
