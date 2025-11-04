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
 * Provides minimal controller plumbing and base classes for concrete controllers in OpenKoda.
 * <p>
 * This package forms the foundation layer of the controller hierarchy, offering common functionality
 * that reduces boilerplate in concrete controller implementations. The primary component is
 * {@link com.openkoda.core.controller.generic.AbstractController}, which centralizes dependency
 * access via the component provider pattern and standardizes pagination across the application.
 * 
 *
 * <b>Package Purpose</b>
 * <p>
 * The {@code com.openkoda.core.controller.generic} package serves as the base layer for all
 * OpenKoda web controllers. It provides:
 * 
 * <ul>
 *   <li><b>Dependency Injection Foundation:</b> Controllers extend {@code AbstractController} to
 *       gain access to the injected {@code DefaultComponentProvider}, which exposes service beans,
 *       repositories, and utilities throughout the application.</li>
 *   <li><b>Standardized Pagination:</b> The {@code createPageable()} factory method encapsulates
 *       Spring Data {@code PageRequest} creation, ensuring consistent zero-based page indexing and
 *       single-property sorting semantics across all list endpoints.</li>
 *   <li><b>URL Constant Exposure:</b> Through the {@code URLConstants} interface implementation,
 *       controllers gain access to centralized URL path definitions for consistent routing.</li>
 *   <li><b>Reduced Boilerplate:</b> By inheriting common functionality, concrete controllers avoid
 *       repetitive dependency injection code and pagination conversion logic.</li>
 * </ul>
 *
 * <b>Key Classes and Interfaces</b>
 * <dl>
 *   <dt>{@link com.openkoda.core.controller.generic.AbstractController}</dt>
 *   <dd>
 *     Lightweight base class providing common functionality for controllers. Extends
 *     {@code ComponentProvider} and implements {@code URLConstants}. Contains:
 *     <ul>
 *       <li><b>componentProvider field:</b> Protected Jakarta-injected {@code DefaultComponentProvider}
 *           field enabling access to services and repositories.</li>
 *       <li><b>createPageable() method:</b> Protected static factory method that constructs
 *           {@code Pageable} objects from page/size/sort parameters, reducing repetitive
 *           {@code PageRequest.of()} calls in request handlers.</li>
 *     </ul>
 *   </dd>
 * </dl>
 *
 * <b>Inheritance and Composition Pattern</b>
 * <p>
 * Concrete controllers follow this inheritance chain:
 * 
 * <pre>{@code
 * UserController extends AbstractController
 *                       → extends ComponentProvider
 *                       → implements URLConstants
 * }</pre>
 * <p>
 * This design allows controllers to access the component provider's exposed services while
 * maintaining URL constant visibility. The pattern promotes consistent architecture across
 * all OpenKoda web endpoints.
 * 
 *
 * <b>Pagination Factory Pattern</b>
 * <p>
 * The {@code createPageable(int page, int size, Sort.Direction direction, String property)}
 * static helper method provides standardized pagination with the following semantics:
 * 
 * <ul>
 *   <li><b>Zero-based indexing:</b> Page 0 represents the first page of results.</li>
 *   <li><b>Configurable page size:</b> Controls the number of records returned per page.</li>
 *   <li><b>Single-property sorting:</b> Sorts by one entity field name with ASC or DESC direction.</li>
 *   <li><b>Runtime property validation:</b> Property names must exactly match entity field names;
 *       mismatches result in Spring Data exceptions at runtime.</li>
 * </ul>
 * <p>
 * Example usage in a controller:
 * 
 * <pre>{@code
 * Pageable pageable = createPageable(0, 20, Sort.Direction.ASC, "username");
 * Page<User> users = userRepository.findAll(pageable);
 * }</pre>
 *
 * <b>Dependencies and Relationships</b>
 * <p>
 * This package depends on:
 * 
 * <ul>
 *   <li><b>Spring Data Domain APIs:</b> {@code org.springframework.data.domain.Pageable},
 *       {@code PageRequest}, {@code Sort.Direction} for pagination abstraction.</li>
 *   <li><b>Jakarta Injection:</b> {@code jakarta.inject.Inject} annotation for dependency
 *       injection of the component provider.</li>
 *   <li><b>OpenKoda Component Provider:</b> {@code ComponentProvider} and
 *       {@code DefaultComponentProvider} from {@code com.openkoda.controller} package for
 *       centralized bean access.</li>
 *   <li><b>URLConstants Interface:</b> {@code com.openkoda.controller.common.URLConstants}
 *       for exposing URL path constants to controller implementations.</li>
 * </ul>
 * <p>
 * Controllers throughout the application depend on this package for base functionality.
 * 
 *
 * <b>Usage Guidance</b>
 * <p>
 * When creating new controllers:
 * 
 * <ol>
 *   <li><b>Extend AbstractController:</b> All concrete controllers should extend
 *       {@code AbstractController} to inherit dependency injection and pagination support.</li>
 *   <li><b>Access services via componentProvider:</b> Use the inherited
 *       {@code componentProvider} field to obtain services and repositories:
 *       {@code componentProvider.services.user} or {@code componentProvider.repositories.organization}.</li>
 *   <li><b>Use createPageable for list endpoints:</b> When implementing paginated list
 *       operations, call {@code createPageable(page, size, direction, property)} to construct
 *       {@code Pageable} objects consistently.</li>
 *   <li><b>Validate property names:</b> Ensure property names passed to {@code createPageable}
 *       exactly match entity field names to avoid runtime sorting exceptions.</li>
 * </ol>
 *
 * <b>Thread-Safety Considerations</b>
 * <p>
 * {@code AbstractController} maintains no per-request mutable state, making it inherently
 * thread-safe at the class level. However, thread-safety of concrete controller instances
 * depends on:
 * 
 * <ul>
 *   <li><b>Injected provider scope:</b> The {@code DefaultComponentProvider} is typically
 *       configured as a singleton or appropriately scoped Spring bean.</li>
 *   <li><b>Service implementations:</b> Services obtained from the provider must be thread-safe,
 *       as multiple concurrent requests may access them simultaneously.</li>
 *   <li><b>Request-scoped data:</b> Controllers should not store request-specific data in
 *       instance fields; use method parameters and local variables instead.</li>
 * </ul>
 *
 * <b>Design Rationale</b>
 * <p>
 * This minimal plumbing layer keeps controller base classes lightweight while providing
 * essential shared functionality. The component provider pattern centralizes bean wiring,
 * reducing constructor complexity in concrete controllers. The pagination factory pattern
 * eliminates repetitive {@code PageRequest.of()} calls and enforces consistent pagination
 * semantics application-wide. Together, these patterns accelerate controller development
 * and promote architectural consistency across the OpenKoda platform.
 * 
 *
 * @since 1.7.1
 * @see com.openkoda.core.controller.generic.AbstractController
 * @see com.openkoda.controller.ComponentProvider
 * @see com.openkoda.controller.DefaultComponentProvider
 * @see com.openkoda.controller.common.URLConstants
 * @see org.springframework.data.domain.Pageable
 * @see org.springframework.data.domain.PageRequest
 */
package com.openkoda.core.controller.generic;