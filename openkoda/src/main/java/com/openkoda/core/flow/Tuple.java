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

import reactor.util.function.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * Lightweight heterogeneous tuple container for holding multiple values.
 * <p>
 * This class provides a simple way to store and access multiple values without creating custom data structures.
 * Values are stored in an Object array and accessed through positional accessor methods (v0-v7 or getV0-getV7).
 * The class also provides interoperability with Reactor's strongly-typed Tuple classes (Tuple2 through Tuple8).
 * <p>
 * Example usage:
 * <pre>{@code
 * Tuple tuple = new Tuple("user", 123, true);
 * String name = tuple.v(String.class, 0);  // "user"
 * }</pre>
 * <p>
 * This class is not generic and does not use type parameters. Accessor methods return Object or use unchecked casts.
 * This class is not synchronized. If multiple threads access a Tuple instance concurrently, external synchronization is required.
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @since 2016-09-25
 * @see reactor.util.function.Tuples
 */
public class Tuple {
    /**
     * Internal storage array holding the tuple values.
     * <p>
     * This array is publicly accessible but declared final to prevent reassignment.
     * Elements can be accessed directly or through accessor methods.
     * 
     */
    public final Object[] list;

    /**
     * Creates a tuple from an array of objects.
     * <p>
     * This constructor accepts a pre-existing array and uses it directly as internal storage.
     * The array is not copied, so modifications to the original array affect the tuple.
     * 
     *
     * @param args the array of objects to store in this tuple
     */
    public Tuple(Object [] args) {
        list = args;
    }

    /**
     * Creates a tuple with one element.
     *
     * @param a1 the first element
     */
    public Tuple(Object a1) {        list = new Object[]{a1};    }
    
    /**
     * Creates a tuple with two elements.
     *
     * @param a1 the first element
     * @param a2 the second element
     */
    public Tuple(Object a1, Object a2) {        list = new Object[]{a1,a2};    }
    
    /**
     * Creates a tuple with three elements.
     *
     * @param a1 the first element
     * @param a2 the second element
     * @param a3 the third element
     */
    public Tuple(Object a1, Object a2, Object a3) {        list = new Object[]{a1,a2,a3};    }
    
    /**
     * Creates a tuple with four elements.
     *
     * @param a1 the first element
     * @param a2 the second element
     * @param a3 the third element
     * @param a4 the fourth element
     */
    public Tuple(Object a1, Object a2, Object a3, Object a4) {        list = new Object[]{a1,a2,a3,a4};    }
    
    /**
     * Creates a tuple with five elements.
     *
     * @param a1 the first element
     * @param a2 the second element
     * @param a3 the third element
     * @param a4 the fourth element
     * @param a5 the fifth element
     */
    public Tuple(Object a1, Object a2, Object a3, Object a4, Object a5) {        list = new Object[]{a1,a2,a3,a4,a5};    }
    
    /**
     * Creates a tuple with six elements.
     *
     * @param a1 the first element
     * @param a2 the second element
     * @param a3 the third element
     * @param a4 the fourth element
     * @param a5 the fifth element
     * @param a6 the sixth element
     */
    public Tuple(Object a1, Object a2, Object a3, Object a4, Object a5, Object a6) {        list = new Object[]{a1,a2,a3,a4,a5,a6};    }
    
    /**
     * Creates a tuple with seven elements.
     *
     * @param a1 the first element
     * @param a2 the second element
     * @param a3 the third element
     * @param a4 the fourth element
     * @param a5 the fifth element
     * @param a6 the sixth element
     * @param a7 the seventh element
     */
    public Tuple(Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7) {        list = new Object[]{a1,a2,a3,a4,a5,a6,a7};    }
    
    /**
     * Creates a tuple with eight elements.
     *
     * @param a1 the first element
     * @param a2 the second element
     * @param a3 the third element
     * @param a4 the fourth element
     * @param a5 the fifth element
     * @param a6 the sixth element
     * @param a7 the seventh element
     * @param a8 the eighth element
     */
    public Tuple(Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8) {        list = new Object[]{a1,a2,a3,a4,a5,a6,a7,a8};    }

    /**
     * Returns the element at the specified position, cast to the specified type.
     * <p>
     * This method performs an unchecked cast to the requested type. No runtime type checking is performed.
     * If the element at the specified index is not compatible with the requested type, a ClassCastException
     * will be thrown when the returned value is used.
     * 
     *
     * @param <T> the type to cast the element to
     * @param c the class object representing the desired type (used for type inference)
     * @param index the zero-based position of the element to retrieve
     * @return the element at the specified position, cast to type T
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     * @throws ClassCastException if the element cannot be cast to the specified type (thrown on usage)
     */
    public <T> T v(Class<T> c, int index) {
        return (T) list[index];
    }

    /**
     * Returns the first element (position 0), typically used as a key in key-value pair scenarios.
     *
     * @return the first element in the tuple
     */
    public Object getKey() { return v0();}
    
    /**
     * Returns the second element (position 1), typically used as a value in key-value pair scenarios.
     *
     * @return the second element in the tuple
     */
    public Object getValue() { return v1();}

    /**
     * Returns the element at position 0.
     *
     * @return the first element in the tuple
     */
    public Object v0(){ return list[0]; }
    
    /**
     * Returns the element at position 1.
     *
     * @return the second element in the tuple
     */
    public Object v1(){ return list[1]; }
    
    /**
     * Returns the element at position 2.
     *
     * @return the third element in the tuple
     */
    public Object v2(){ return list[2]; }
    
    /**
     * Returns the element at position 3.
     *
     * @return the fourth element in the tuple
     */
    public Object v3(){ return list[3]; }
    
    /**
     * Returns the element at position 4.
     *
     * @return the fifth element in the tuple
     */
    public Object v4(){ return list[4]; }
    
    /**
     * Returns the element at position 5.
     *
     * @return the sixth element in the tuple
     */
    public Object v5(){ return list[5]; }
    
    /**
     * Returns the element at position 6.
     *
     * @return the seventh element in the tuple
     */
    public Object v6(){ return list[6]; }
    
    /**
     * Returns the element at position 7.
     *
     * @return the eighth element in the tuple
     */
    public Object v7(){ return list[7]; }

    /**
     * Returns the element at position 0 (JavaBean-style accessor).
     *
     * @return the first element in the tuple
     */
    public Object getV0(){ return list[0]; }
    
    /**
     * Returns the element at position 1 (JavaBean-style accessor).
     *
     * @return the second element in the tuple
     */
    public Object getV1(){ return list[1]; }
    
    /**
     * Returns the element at position 2 (JavaBean-style accessor).
     *
     * @return the third element in the tuple
     */
    public Object getV2(){ return list[2]; }
    
    /**
     * Returns the element at position 3 (JavaBean-style accessor).
     *
     * @return the fourth element in the tuple
     */
    public Object getV3(){ return list[3]; }
    
    /**
     * Returns the element at position 4 (JavaBean-style accessor).
     *
     * @return the fifth element in the tuple
     */
    public Object getV4(){ return list[4]; }
    
    /**
     * Returns the element at position 5 (JavaBean-style accessor).
     *
     * @return the sixth element in the tuple
     */
    public Object getV5(){ return list[5]; }
    
    /**
     * Returns the element at position 6 (JavaBean-style accessor).
     *
     * @return the seventh element in the tuple
     */
    public Object getV6(){ return list[6]; }
    
    /**
     * Returns the element at position 7 (JavaBean-style accessor).
     *
     * @return the eighth element in the tuple
     */
    public Object getV7(){ return list[7]; }

    /**
     * Returns the first element for Reactor interoperability.
     *
     * @return the first element in the tuple
     */
    public Object t1() {
        return list[0];
    }
    
    /**
     * Converts this tuple to a Reactor Tuple2 containing the first two elements.
     *
     * @return a strongly-typed Reactor Tuple2 with elements at positions 0 and 1
     * @see reactor.util.function.Tuple2
     */
    public Tuple2 t2() {
        return Tuples.of(list[0], list[1]);
    }
    
    /**
     * Converts this tuple to a Reactor Tuple3 containing the first three elements.
     *
     * @return a strongly-typed Reactor Tuple3 with elements at positions 0, 1, and 2
     * @see reactor.util.function.Tuple3
     */
    public Tuple3 t3() {
        return Tuples.of(list[0], list[1], list[2]);
    }
    
    /**
     * Converts this tuple to a Reactor Tuple4 containing the first four elements.
     *
     * @return a strongly-typed Reactor Tuple4 with elements at positions 0-3
     * @see reactor.util.function.Tuple4
     */
    public Tuple4 t4() {
        return Tuples.of(list[0], list[1], list[2], list[3]);
    }
    
    /**
     * Converts this tuple to a Reactor Tuple5 containing the first five elements.
     *
     * @return a strongly-typed Reactor Tuple5 with elements at positions 0-4
     * @see reactor.util.function.Tuple5
     */
    public Tuple5 t5() {
        return Tuples.of(list[0], list[1], list[2], list[3], list[4]);
    }
    
    /**
     * Converts this tuple to a Reactor Tuple6 containing the first six elements.
     *
     * @return a strongly-typed Reactor Tuple6 with elements at positions 0-5
     * @see reactor.util.function.Tuple6
     */
    public Tuple6 t6() {
        return Tuples.of(list[0], list[1], list[2], list[3], list[4], list[5]);
    }
    
    /**
     * Converts this tuple to a Reactor Tuple7 containing the first seven elements.
     *
     * @return a strongly-typed Reactor Tuple7 with elements at positions 0-6
     * @see reactor.util.function.Tuple7
     */
    public Tuple7 t7() {
        return Tuples.of(list[0], list[1], list[2], list[3], list[4], list[5], list[6]);
    }
    
    /**
     * Converts this tuple to a Reactor Tuple8 containing all eight elements.
     *
     * @return a strongly-typed Reactor Tuple8 with elements at positions 0-7
     * @see reactor.util.function.Tuple8
     */
    public Tuple8 t8() {
        return Tuples.of(list[0], list[1], list[2], list[3], list[4], list[5], list[6], list[7]);
    }
    /**
     * Returns the first element for Reactor interoperability (JavaBean-style accessor).
     *
     * @return the first element in the tuple
     */
    public Object getT1() {
        return list[0];
    }
    
    /**
     * Converts this tuple to a Reactor Tuple2 (JavaBean-style accessor).
     *
     * @return a strongly-typed Reactor Tuple2 with elements at positions 0 and 1
     * @see reactor.util.function.Tuple2
     */
    public Tuple2 getT2() {
        return Tuples.of(list[0], list[1]);
    }
    
    /**
     * Converts this tuple to a Reactor Tuple3 (JavaBean-style accessor).
     *
     * @return a strongly-typed Reactor Tuple3 with elements at positions 0, 1, and 2
     * @see reactor.util.function.Tuple3
     */
    public Tuple3 getT3() {
        return Tuples.of(list[0], list[1], list[2]);
    }
    
    /**
     * Converts this tuple to a Reactor Tuple4 (JavaBean-style accessor).
     *
     * @return a strongly-typed Reactor Tuple4 with elements at positions 0-3
     * @see reactor.util.function.Tuple4
     */
    public Tuple4 getT4() {
        return Tuples.of(list[0], list[1], list[2], list[3]);
    }
    
    /**
     * Converts this tuple to a Reactor Tuple5 (JavaBean-style accessor).
     *
     * @return a strongly-typed Reactor Tuple5 with elements at positions 0-4
     * @see reactor.util.function.Tuple5
     */
    public Tuple5 getT5() {
        return Tuples.of(list[0], list[1], list[2], list[3], list[4]);
    }
    
    /**
     * Converts this tuple to a Reactor Tuple6 (JavaBean-style accessor).
     *
     * @return a strongly-typed Reactor Tuple6 with elements at positions 0-5
     * @see reactor.util.function.Tuple6
     */
    public Tuple6 getT6() {
        return Tuples.of(list[0], list[1], list[2], list[3], list[4], list[5]);
    }
    
    /**
     * Converts this tuple to a Reactor Tuple7 (JavaBean-style accessor).
     *
     * @return a strongly-typed Reactor Tuple7 with elements at positions 0-6
     * @see reactor.util.function.Tuple7
     */
    public Tuple7 getT7() {
        return Tuples.of(list[0], list[1], list[2], list[3], list[4], list[5], list[6]);
    }
    
    /**
     * Converts this tuple to a Reactor Tuple8 (JavaBean-style accessor).
     *
     * @return a strongly-typed Reactor Tuple8 with elements at positions 0-7
     * @see reactor.util.function.Tuple8
     */
    public Tuple8 getT8() {
        return Tuples.of(list[0], list[1], list[2], list[3], list[4], list[5], list[6], list[7]);
    }


    /**
     * Aggregates a collection of tuples by collecting elements at each position into separate collections.
     * <p>
     * This method performs per-position collection aggregation. Each position in the input tuples is
     * collected into a separate collection (List or Set) based on the corresponding Collector specification.
     * The result is a new Tuple where each element is a collection containing all values from that position
     * across all input tuples.
     * 
     * <p>
     * Example: Given tuples [(a,1), (b,2), (c,3)] with collectors [SET, LIST], the result is
     * a Tuple containing [{a,b,c}, [1,2,3]].
     * 
     *
     * @param tuples the collection of tuples to aggregate
     * @param collectors varargs array of Collector enums specifying the collection type for each position
     * @return a new Tuple where each element is a collection of values from that position across all input tuples
     */
    public static Tuple collect(Collection<Tuple> tuples, Collector ... collectors) {
        Collection[]result = new Collection[collectors.length];
        int size = tuples.size();
        for (int k = 0; k < collectors.length ; k++) {
            Collector c = collectors[k];
            switch (c) {
                case LIST:
                    result[k] = new ArrayList<>(size); break;
                case SET:
                    result[k] = new HashSet<>(); break;
            }
        }
        for(Tuple t : tuples) {
            for (int k = 0; k < collectors.length ; k++) {
                result[k].add(t);
            }
        }
        return new Tuple(result);
    }

    /**
     * Enumeration defining collection types for the {@link #collect(Collection, Collector...)} method.
     * <p>
     * Specifies whether elements at a given position should be collected into a List (preserving order
     * and allowing duplicates) or a Set (removing duplicates).
     * 
     */
    public static enum Collector {
        /**
         * Collect elements into an ArrayList, preserving insertion order and allowing duplicates.
         */
        LIST,
        
        /**
         * Collect elements into a HashSet, automatically removing duplicates.
         */
        SET
    }

    @Override
    public String toString() {
        return "Tuple: " + Arrays.toString(list);
    }
}
