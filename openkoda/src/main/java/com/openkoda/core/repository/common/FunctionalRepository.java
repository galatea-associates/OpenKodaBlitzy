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

package com.openkoda.core.repository.common;

/**
 * Marker interface for compile-time grouping of functional repository types.
 * <p>
 * This interface serves as a named type for repository classification and type-safe grouping.
 * It declares no methods or metadata and exists purely for compile-time semantics.
 * Classes that implement this interface signal that they provide functional repository capabilities.
 * </p>
 * <p>
 * The marker pattern allows the type system to enforce contracts at compile time without requiring
 * runtime behavior. This interface enables developers to write type-safe code that operates on
 * repositories with functional characteristics.
 * </p>
 * <p>
 * <b>Warning:</b> Removing or renaming this interface will cause compile-time failures in any code
 * that references {@code FunctionalRepository} as a type constraint or marker.
 * </p>
 *
 * @since 1.7.1
 * @author OpenKoda Team
 */
public interface FunctionalRepository {


}
