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

package com.openkoda.core.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation that aggregates OpenAPI {@link Parameter} documentation for pageable REST endpoints.
 * <p>
 * This annotation provides consistent parameter descriptions for Spring Data pagination parameters
 * in OpenAPI/Swagger documentation. It automatically documents three standard query parameters
 * used for pagination and sorting in REST API endpoints.
 * </p>
 * <p>
 * The annotation defines the following query parameters:
 * <ul>
 * <li><b>page</b>: Zero-based page index (0..N). Default value is 0.</li>
 * <li><b>size</b>: Number of records per page. Default value is 20.</li>
 * <li><b>sort</b>: Sorting criteria in the format {@code property,asc|desc}. Multiple sort
 * criteria are supported. Default sort order is ascending.</li>
 * </ul>
 * </p>
 * <p>
 * Example usage on a controller method:
 * <pre>{@code
 * @GetMapping("/api/organizations")
 * @ApiPageable
 * public ResponseEntity<Page<Organization>> getOrganizations(Pageable pageable) {
 *     return ResponseEntity.ok(organizationService.findAll(pageable));
 * }
 * }</pre>
 * </p>
 * <p>
 * This annotation requires {@code io.swagger.v3.oas.annotations} OpenAPI annotations on the classpath
 * and is used by OpenAPI documentation generators (such as Springdoc or Swagger) to produce
 * comprehensive API documentation with pagination parameter details.
 * </p>
 * <p>
 * This is a compile-time and runtime annotation ({@link RetentionPolicy#RUNTIME} retention) with
 * no runtime behavior. It is purely for documentation generation purposes and does not affect
 * application execution.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see io.swagger.v3.oas.annotations.Parameter
 * @see org.springframework.data.domain.Pageable
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Parameters({
        @Parameter(name = "page", schema = @Schema(defaultValue = "0", type="int"), in = ParameterIn.QUERY, description = "Results page you want to retrieve (0..N)"),
        @Parameter(name = "size", schema = @Schema(defaultValue = "20", type="int"), in = ParameterIn.QUERY, description = "Number of records per page."),
        @Parameter(name = "sort", array = @ArraySchema(schema = @Schema(type = "string")), in = ParameterIn.QUERY, description = "Sorting criteria in the format: property(,asc|desc). "
                + "Default sort order is ascending. " + "Multiple sort criteria are supported.")})
public @interface ApiPageable { }