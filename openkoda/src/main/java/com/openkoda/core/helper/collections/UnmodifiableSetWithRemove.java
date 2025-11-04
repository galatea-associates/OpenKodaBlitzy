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
import java.util.HashSet;

/**
 * An unmodifiable set implementation that permits removal operations but blocks all additions.
 * <p>
 * This class uses the decorator pattern by extending {@link HashSet} to provide selective
 * mutability. While typical unmodifiable collections block all modifications, this implementation
 * allows removal operations ({@code remove}, {@code clear}, {@code iterator().remove()},
 * {@code removeAll}, {@code retainAll}) to remain functional while preventing any additions
 * via {@code add} or {@code addAll} methods.
 * <p>
 * The {@code add} and {@code addAll} operations throw {@link UnsupportedOperationException}
 * to enforce the no-addition policy. All removal-related APIs inherited from {@link HashSet}
 * remain operative and will successfully delete elements from the set.
 * <p>
 * Example usage:
 * <pre>{@code
 * Set<String> original = Set.of("a", "b", "c");
 * UnmodifiableSetWithRemove<String> set = new UnmodifiableSetWithRemove<>(original);
 * set.remove("a"); // succeeds
 * }</pre>
 * <p>
 * Thread-safety: This class is not thread-safe. It inherits the non-synchronized behavior
 * from {@link HashSet}. External synchronization is required for concurrent access.
 *
 * @param <E> the type of elements maintained by this set
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see UnmodifiableMapWithRemove
 * @see java.util.HashSet
 */
public class UnmodifiableSetWithRemove<E> extends HashSet<E> {

    /**
     * Blocks element insertion by throwing an exception.
     * <p>
     * This method prevents any additions to the set by immediately throwing
     * {@link UnsupportedOperationException}. The element parameter is ignored.
     * 
     *
     * @param e the element to add (ignored, operation always fails)
     * @return {@code false} (never reached due to exception)
     * @throws UnsupportedOperationException always thrown with message "This set does not support add operations"
     */
    @Override
    public boolean add(E e) { throw new UnsupportedOperationException(); }

    /**
     * Blocks bulk insertion by throwing an exception.
     * <p>
     * This method prevents any bulk additions to the set by immediately throwing
     * {@link UnsupportedOperationException}. The collection parameter is ignored.
     * 
     *
     * @param c the collection of elements to add (ignored, operation always fails)
     * @return {@code false} (never reached due to exception)
     * @throws UnsupportedOperationException always thrown with message "This set does not support add operations"
     */
    @Override
    public boolean addAll(Collection<? extends E> c) { throw new UnsupportedOperationException(); }

    /**
     * Creates a new set by copying elements from the source collection.
     * <p>
     * This constructor initializes the underlying {@link HashSet} with a preallocated
     * capacity based on the source collection size for efficiency. It populates the set
     * by iterating through the source collection and calling {@code super.add()} to bypass
     * the overridden {@code add} method that throws exceptions.
     * 
     *
     * @param c the source collection to copy elements from (must not be null)
     * @throws NullPointerException if the specified collection is null
     */
    public UnmodifiableSetWithRemove(Collection<? extends E> c) {
        super(c.size());
        for (E e : c) super.add(e);
    }
}
