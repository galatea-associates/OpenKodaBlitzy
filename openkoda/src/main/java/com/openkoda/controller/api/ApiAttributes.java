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

import com.openkoda.controller.api.v1.model.TokenResponse;
import com.openkoda.core.flow.PageAttr;
import com.openkoda.core.helper.ReadableCode;

/**
 * Interface centralizing strongly-typed PageAttr constants for API controller Flow pipelines.
 * <p>
 * Marker interface extending ReadableCode that defines compile-time-checked Flow model attribute keys
 * for API responses. Placing PageAttr constants in an interface provides implicit public static final
 * semantics, avoiding scattered string literals across controllers. Interface fields are initialized at
 * class load time.
 * </p>
 * <p>
 * Controllers implementing this interface gain direct access to typed attribute constants for PageModelMap
 * operations. This enforces compile-time type safety: the tokenResponse constant is typed as
 * {@code PageAttr<TokenResponse>}, preventing incorrect type assignments in Flow.thenSet calls.
 * </p>
 * <p>
 * <b>Usage Example:</b>
 * </p>
 * <pre>{@code
 * public class TokenControllerApiV1 extends AbstractController implements ApiAttributes {
 *     public Object getToken(TokenRequest request) {
 *         return Flow.init(services)
 *             .thenSet(tokenResponse, a -> createToken(request))
 *             .execute().mav();
 *     }
 * }
 * }</pre>
 * <p>
 * <b>Design Rationale:</b>
 * </p>
 * <ul>
 * <li>Interface placement provides implicit public static final without explicit modifiers</li>
 * <li>Extends ReadableCode for utility methods for readable error handling</li>
 * <li>PageAttr wrapping enables generic type parameter for compile-time type checking</li>
 * <li>Centralized constants provide single source of truth for API attribute keys</li>
 * </ul>
 * <p>
 * <b>Benefits Over String Literals:</b>
 * </p>
 * <ul>
 * <li>Compile-time type safety: PageAttr&lt;TokenResponse&gt; prevents type mismatches</li>
 * <li>Refactoring support: IDE renames propagate to all usages</li>
 * <li>Autocomplete: IDE suggests available attributes</li>
 * <li>Typo prevention: Compiler catches key name errors</li>
 * <li>Documentation: Self-documenting attribute types and purposes</li>
 * </ul>
 * <p>
 * <b>Extension Pattern:</b> To add new API attributes, add PageAttr constants to this interface.
 * For example: {@code PageAttr<UserProfile> userProfile = new PageAttr<>("userProfile");}
 * </p>
 * <p>
 * <b>Implementation Note:</b> Controllers implementing this interface automatically inherit the
 * tokenResponse constant without import or qualification. Alternative: Static import from class
 * implementing interface.
 * </p>
 * <p>
 * <b>Class Loading:</b> Interface fields are initialized when the interface is first loaded by
 * ClassLoader. Initialization occurs once per JVM, before any implementing class instantiation.
 * </p>
 * <p>
 * <b>Maintainability Note:</b> Changing tokenResponse constant name or type is a breaking change
 * affecting all API controllers. Coordinate updates with AbstractTokenControllerApiV1 and
 * TokenControllerApiV1.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see PageAttr
 * @see TokenResponse
 * @see ReadableCode
 */
public interface ApiAttributes extends ReadableCode {

    /**
     * Strongly-typed PageAttr constant for TokenResponse attribute in Flow pipelines and PageModelMap.
     * <p>
     * Canonical Flow model key for token responses in authentication endpoints. This constant is used in
     * AbstractTokenControllerApiV1 and TokenControllerApiV1 to store TokenResponse DTOs containing
     * apiToken, userId, and expiresOn fields. The typed nature prevents typos in string keys and enables
     * IDE autocomplete.
     * </p>
     * <p>
     * <b>Type Safety:</b> {@code PageAttr<TokenResponse>} provides compile-time type checking that prevents
     * assigning incompatible types. Flow.thenSet(tokenResponse, ...) requires lambda returning TokenResponse.
     * </p>
     * <p>
     * <b>Key Details:</b>
     * </p>
     * <ul>
     * <li>Key name: "tokenResponse" - string key used in PageModelMap for storage/retrieval</li>
     * <li>Type: PageAttr&lt;TokenResponse&gt; - enforces type checking at compile time</li>
     * <li>Visibility: public static final (implicit from interface field)</li>
     * <li>Thread-safety: Immutable constant initialized at class load - safe for concurrent access</li>
     * </ul>
     * <p>
     * <b>Usage Example:</b>
     * </p>
     * <pre>{@code
     * Flow.init(services)
     *     .thenSet(tokenResponse, a -> new TokenResponse(...))
     *     .execute();
     * }</pre>
     * <p>
     * <b>Type Safety in Action:</b>
     * </p>
     * <pre>{@code
     * // Compile error: incompatible types
     * Flow.thenSet(tokenResponse, a -> "string");
     * 
     * // OK: correct type
     * Flow.thenSet(tokenResponse, a -> new TokenResponse(...));
     * }</pre>
     *
     * @see TokenResponse
     * @see PageAttr
     */
    PageAttr<TokenResponse> tokenResponse = new PageAttr<>("tokenResponse");

}