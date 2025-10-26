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

import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Descriptor and global registry for strongly-typed page model keys.
 * <p>
 * PageAttr provides type-safe access to page model attributes, avoiding
 * string-based attribute lookup. Each instance registers itself in a global
 * registry upon construction, enabling retrieval by name.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * PageAttr&lt;Organization&gt; ORG = new PageAttr&lt;&gt;("organization");
 * PageAttr attr = PageAttr.getByName("organization");
 * </pre>
 * </p>
 * <p>
 * <b>Warning:</b> The internal registry is not thread-safe. Registry population
 * occurs during class initialization and should not be modified at runtime.
 * The registry uses type erasure, so runtime type information is not preserved.
 * </p>
 *
 * @param <T> the type of the page attribute value
 * @see com.openkoda.core.flow.BasePageAttributes for canonical page attribute instances
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class PageAttr<T> {

	/**
	 * Global registry mapping attribute names to PageAttr instances.
	 * <p>
	 * This registry is not synchronized and uses type erasure, so type safety
	 * is only enforced at compile time through the generic parameter.
	 * </p>
	 */
	private final static Map<String, PageAttr> allByName = new HashMap<>();

	/**
	 * The unique name identifier for this page attribute.
	 */
	public final String name;
	
	/**
	 * Optional constructor supplier for creating default instances of this attribute.
	 */
	public final Supplier<T> constructor;

	/**
	 * Creates a page attribute with the specified name and no default constructor.
	 * <p>
	 * This constructor registers the attribute in the global registry and asserts
	 * that the name is unique.
	 * </p>
	 *
	 * @param name the unique name for this page attribute, must not already exist in the registry
	 * @throws IllegalArgumentException if the name already exists in the registry
	 */
	public PageAttr(String name) {
		this(name, () -> null);
	}

	/**
	 * Creates a page attribute with the specified name and optional default constructor.
	 * <p>
	 * This constructor registers the attribute in the global registry and asserts
	 * that the name is unique. The constructor supplier can be used to create
	 * default instances when the attribute value is not present.
	 * </p>
	 *
	 * @param name the unique name for this page attribute, must not already exist in the registry
	 * @param constructor supplier for creating default instances, may return null
	 * @throws IllegalArgumentException if the name already exists in the registry
	 */
	public PageAttr(String name, Supplier<T> constructor) {
		Assert.isTrue(!allByName.containsKey(name), "Page Attribute Collision: " + name);
		this.constructor = constructor;
        this.name = name;
		allByName.put(name, this);
	}

	/**
	 * Retrieves a page attribute by its registered name.
	 *
	 * @param name the name of the page attribute to retrieve
	 * @return the PageAttr instance associated with the name, or null if not found
	 */
	public static PageAttr getByName(String name) {
	    return allByName.get(name);
    }
}
