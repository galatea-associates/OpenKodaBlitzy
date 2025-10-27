/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 and associated documentation files (the "Software"), to deal in the Software without restriction, 
including without limitation the rights to use, copy, modify, merge, publish, distribute, 
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software 
is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice 
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE 
AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS 
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES 
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.core.controller.generic;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.controller.DefaultComponentProvider;
import com.openkoda.controller.common.URLConstants;
import jakarta.inject.Inject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Lightweight base class providing common functionality for concrete controllers in OpenKoda.
 * <p>
 * This class serves as the foundation for the controller hierarchy, centralizing dependency access
 * via the component provider pattern. It extends {@link ComponentProvider} and implements
 * {@link URLConstants} to expose URL constants to all subclasses. The primary purpose is to
 * reduce boilerplate in concrete controller implementations by providing standardized access
 * to services, repositories, and utilities through the injected {@code componentProvider} field.
 * </p>
 * <p>
 * Design patterns employed:
 * <ul>
 *   <li><b>Dependency Injection</b>: Uses Jakarta {@code @Inject} annotation for provider injection</li>
 *   <li><b>Template Method</b>: Subclasses extend and add specific controller behavior</li>
 *   <li><b>Factory Pattern</b>: Provides {@code createPageable} static helper for pagination</li>
 * </ul>
 * </p>
 * <p>
 * Thread-safety: This class maintains no per-request mutable state. Thread-safety depends on
 * the injected {@code componentProvider} implementation, which is typically a singleton or
 * appropriately scoped bean managed by the Spring DI container.
 * </p>
 * <p>
 * Example usage in a concrete controller:
 * <pre>{@code
 * public class UserController extends AbstractController {
 *     public ResponseEntity<Page<User>> listUsers(int page, int size) {
 *         Pageable pageable = createPageable(page, size, Sort.Direction.ASC, "username");
 *         return ResponseEntity.ok(componentProvider.repositories.user.findAll(pageable));
 *     }
 * }
 * }</pre>
 * </p>
 * <p>
 * Important notes:
 * <ul>
 *   <li>Subclasses inherit access to the {@code componentProvider} field</li>
 *   <li>Use {@code createPageable} for consistent pagination across controllers</li>
 *   <li>Property names passed to {@code createPageable} must match entity field names exactly</li>
 * </ul>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ComponentProvider
 * @see DefaultComponentProvider
 * @see URLConstants
 * @see org.springframework.data.domain.Pageable
 */
public class AbstractController extends ComponentProvider implements URLConstants {

    /**
     * Injected component provider exposing OpenKoda service beans, repositories, and utilities.
     * <p>
     * This field is automatically injected by the Spring DI container using the Jakarta
     * {@code @Inject} annotation. Subclasses access this field to obtain services and
     * repositories without explicit dependency declarations.
     * </p>
     * <p>
     * Scope: Typically a singleton or appropriately scoped bean managed by Spring. The provider
     * implementation must be thread-safe as it is shared across concurrent requests.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * UserService userService = componentProvider.services.user;
     * }</pre>
     * </p>
     *
     * @see DefaultComponentProvider
     */
    @Inject
    protected DefaultComponentProvider componentProvider;
    
    /**
     * Creates a Spring Data {@link Pageable} object for standardized pagination.
     * <p>
     * This static helper method encapsulates {@link PageRequest#of(int, int, Sort.Direction, String...)}
     * semantics to reduce boilerplate in controller methods. It configures zero-based page indexing
     * (page 0 = first page) with single-property sorting in the specified direction.
     * </p>
     * <p>
     * Controllers typically accept page, size, and sort parameters from HTTP requests and use this
     * method to construct a {@code Pageable} for passing to repository or service layer methods.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * Pageable pageable = createPageable(0, 20, Sort.Direction.DESC, "createdDate");
     * Page<Organization> orgs = organizationRepository.findAll(pageable);
     * }</pre>
     * </p>
     * <p>
     * Important notes:
     * <ul>
     *   <li>Property name validation happens at runtime, not compile-time</li>
     *   <li>Incorrect property names result in Spring Data exceptions</li>
     *   <li>For multi-property sorting, construct {@link PageRequest} directly</li>
     * </ul>
     * </p>
     *
     * @param page Zero-based page index (0 = first page). Must be &gt;= 0
     * @param size Page size (number of records per page). Must be &gt; 0
     * @param direction Sort direction (ASC or DESC). Cannot be null
     * @param property Entity property name to sort by. Must match entity field name exactly
     *                 or Spring Data will throw exception at runtime
     * @return Pageable object configured with specified page, size, and sort parameters. Never returns null
     * @see org.springframework.data.domain.PageRequest#of(int, int, Sort.Direction, String...)
     * @see org.springframework.data.domain.Pageable
     */
    protected static Pageable createPageable(int page, int size, Sort.Direction direction, String property) {
        return PageRequest.of(page, size, direction, property);
    }

}
