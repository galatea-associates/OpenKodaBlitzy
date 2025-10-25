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

package com.openkoda.controller.api;

/**
 * Generic DTO wrapper for single-value JSON API responses.
 * <p>
 * Minimal mutable wrapper class with single parameterized field T result. This class serves as a simple
 * container for API responses returning single values. The package-private field visibility and lack of
 * explicit constructors or accessors may affect Jackson JSON binding behavior. Ensure Jackson is configured
 * for field-level access or add public getters for reliable JSON serialization.
 * </p>
 * <p>
 * No validation annotations, equals/hashCode/toString implementations, or Serializable interface are provided.
 * Consider extending this class for production use with proper encapsulation and validation.
 * </p>
 * <p>
 * Thread-safety: This class is mutable and not thread-safe if shared across threads. Create new instances
 * per request to avoid concurrent modification issues.
 * </p>
 * <p>
 * Usage examples:
 * <pre>{@code
 * ApiResult<String> result = new ApiResult<>();
 * result.result = "Operation successful";
 * }</pre>
 * <pre>{@code
 * ApiResult<Long> idResult = new ApiResult<>();
 * idResult.result = 123L;
 * }</pre>
 * </p>
 * <p>
 * Jackson configuration note: For reliable JSON binding, consider:
 * <ul>
 * <li>Adding public getter: public T getResult() { return result; }</li>
 * <li>Adding @JsonProperty annotation to the field</li>
 * <li>Configuring Jackson for field visibility</li>
 * </ul>
 * </p>
 * <p>
 * Alternative design suggestions:
 * <ul>
 * <li>Add explicit constructor: public ApiResult(T result) { this.result = result; }</li>
 * <li>Add fluent API: public ApiResult&lt;T&gt; withResult(T result)</li>
 * <li>Consider immutable design with final field</li>
 * <li>Add factory methods: success(T value), error()</li>
 * </ul>
 * </p>
 * <p>
 * Encapsulation concern: Package-private field breaks encapsulation. Callers within the same package can
 * modify the result directly. Consider making field private with public accessor methods.
 * </p>
 * <p>
 * Maintainability note: This minimal implementation can be extended with status codes, error messages,
 * or metadata fields for richer API responses. For complex responses, consider structured DTOs with
 * success/error/data/message fields.
 * </p>
 *
 * @param <T> Type of wrapped result value (e.g., String, Long, Boolean, custom DTO)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class ApiResult<T> {

    /**
     * Package-private mutable field holding the wrapped result value.
     * <p>
     * Type: Generic type T specified at instantiation (e.g., ApiResult&lt;String&gt;, ApiResult&lt;Long&gt;).
     * </p>
     * <p>
     * Visibility: Package-private, accessible within com.openkoda.controller.api package only.
     * </p>
     * <p>
     * Jackson serialization: Relies on field-level binding. Ensure Jackson is configured for field access
     * or add public getter method.
     * </p>
     * <p>
     * Null handling: No @NotNull validation. The result can be null.
     * </p>
     */
    T result;



}