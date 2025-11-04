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

/**
 * Common constants, type-safe attribute descriptors, and URL patterns shared across all controller modules 
 * in the OpenKoda web layer.
 * 
 * <p>
 * This package centralizes cross-controller infrastructure to ensure consistency and enable compile-time 
 * validation. It provides strongly-typed PageModelMap attribute keys ({@link PageAttributes}), HTTP session 
 * attribute keys ({@link SessionData}), and application URL path constants ({@link URLConstants}). These 
 * shared contracts prevent magic strings, reduce typos, and simplify refactoring across the entire 
 * controller layer.

 * 
 * <b>Key Classes and Their Roles</b>
 * 
 * <b>PageAttributes</b>
 * <p>
 * Authoritative registry of MVC model attribute names with compile-time type checking via PageAttr&lt;T&gt; 
 * descriptors. Used by controllers to populate PageModelMap and by Thymeleaf templates to access view data. 
 * Includes {@code pageConverter} utility for entity to DTO page transformations.

 * <p>
 * Example usage:
 * <pre>{@code
 * pageModel.put(PageAttributes.organizationDto, dto);
 * }</pre>

 * 
 * <b>SessionData</b>
 * <p>
 * HTTP session attribute key constants for user authentication state (SPOOFING_USER), locale preferences 
 * (LOCALE), and multi-tenant organization selection (CURRENT_ORGANIZATION_ID). Accessed via 
 * {@code @SessionAttribute} or HttpSession by controllers, filters, and services.

 * <p>
 * Example usage:
 * <pre>{@code
 * @SessionAttribute(SessionData.CURRENT_ORGANIZATION_ID) Long orgId
 * }</pre>

 * 
 * <b>URLConstants</b>
 * <p>
 * Centralized URL path fragments, composed templates, API version prefixes (_API_V1, _API_V2), Ant-style 
 * security patterns, parameter templates with regex constraints (_ID, _ORGANIZATIONID), and validation 
 * regexes. Referenced in {@code @RequestMapping} annotations and Spring Security configuration.

 * <p>
 * Example usage:
 * <pre>{@code
 * @RequestMapping(URLConstants._ADMIN + URLConstants._SETTINGS)
 * }</pre>

 * 
 * <b>Usage Guidance</b>
 * <p>
 * Import constants to avoid string literals in controller code. This approach enables IDE autocomplete, 
 * provides compile-time validation, and prevents runtime string typos.

 * 
 * <b>Relationships with Other Packages</b>
 * <ul>
 * <li><b>Used by:</b> All controller subpackages (admin, api, organization, user, etc.), security 
 *     configuration, view builders, filters</li>
 * <li><b>Depends on:</b> core.flow (PageAttr, BasePageAttributes), core.helper (ReadableCode)</li>
 * <li><b>Consumers:</b> Controllers reference these constants in @RequestMapping, security uses Ant 
 *     expressions, templates access attribute keys</li>
 * </ul>
 * 
 * <b>Design Rationale</b>
 * <p>
 * Centralizing constants enforces consistency, enables IDE autocomplete, provides compile-time validation, 
 * and prevents runtime string typos. Changes to URLs or attribute names require only updates here and 
 * coordinated recompilation, detected at build time rather than runtime.

 * 
 * <b>Common Pitfalls</b>
 * <p>
 * Changing constant values impacts many consumers. Run full regression test suite when modifying. Constants 
 * are inlined by Java compiler, requiring clean rebuild after changes.

 * 
 * <p><b>Should I put a class into this package?</b></p>
 * <p>If something is shared across all controllers, then the package is for you.</p>
 * 
 * @version 1.7.1
 * @since 1.7.1
 */
package com.openkoda.controller.common;