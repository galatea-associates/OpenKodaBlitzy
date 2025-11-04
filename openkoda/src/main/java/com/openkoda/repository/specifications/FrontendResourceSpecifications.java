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

package com.openkoda.repository.specifications;

import com.openkoda.core.helper.ReadableCode;
import com.openkoda.model.component.FrontendResource;
import com.openkoda.model.file.File;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * JPA Criteria API Specification builders for dynamic FrontendResource and File entity queries.
 * <p>
 * Provides static factory methods that return {@link Specification} instances for type-safe query 
 * construction using the JPA Criteria API. These specifications are composable via {@code and()}/{@code or()} 
 * operators to build complex filtering conditions. Used by repositories for flexible search and filtering 
 * of frontend resources and file entities.
 * 
 * <p>
 * Note: Uses string-based attribute names ("name", "resourceType", "createdOn") which are fragile 
 * to entity refactoring. Consider migrating to JPA metamodel for type safety.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.springframework.data.jpa.domain.Specification
 * @see com.openkoda.model.component.FrontendResource
 * @see com.openkoda.model.file.File
 * @see jakarta.persistence.criteria.CriteriaBuilder
 */
public class FrontendResourceSpecifications implements ReadableCode {

    /**
     * Creates a Specification that matches FrontendResource entities by exact name.
     * <p>
     * Example usage:
     * <pre>{@code
     * FrontendResourceSpecifications.searchByName("header.js")
     * }</pre>
     * 
     *
     * @param name The exact name to search for. Uses case-sensitive equality matching via {@link CriteriaBuilder#equal}
     * @return Specification for FrontendResource filtering by name attribute
     */
    public static Specification<FrontendResource> searchByName(String name) {

        return new Specification<FrontendResource>() {
            @Override
            public Predicate toPredicate(Root<FrontendResource> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                return cb.equal(root.get("name"), name);
            }
        };

    }

    /**
     * Creates a time-sensitive Specification that matches File entities created within the specified time window.
     * <p>
     * Note: This specification captures {@link LocalDateTime#now()} at construction time, making it 
     * time-dependent and unsuitable for caching across evaluation points.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * searchNotOlderThan(30).and(searchByType(type))
     * }</pre>
     * 
     *
     * @param minutes Number of minutes in the past to search from current time. Files with createdOn timestamps 
     *                greater than {@code LocalDateTime.now().minusMinutes(minutes)} will match
     * @return Specification for File filtering by createdOn timestamp
     */
    public static Specification<File> searchNotOlderThan(int minutes) {

        return (root, query, cb) -> cb.greaterThan(root.get("createdOn"), LocalDateTime.now().minusMinutes(minutes));

    }

    /**
     * Creates a Specification that matches FrontendResource entities by resource type.
     * <p>
     * Example usage:
     * <pre>{@code
     * searchByResourceType(ResourceType.JS).or(searchByResourceType(ResourceType.CSS))
     * }</pre>
     * 
     *
     * @param resourceType The {@link FrontendResource.ResourceType} enum value to match. Uses equality comparison
     * @return Specification for FrontendResource filtering by resourceType attribute
     */
    public static Specification<FrontendResource> searchByResourceType(FrontendResource.ResourceType resourceType) {

        return new Specification<FrontendResource>() {
            @Override
            public Predicate toPredicate(Root<FrontendResource> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                return cb.equal(root.get("resourceType"), resourceType);
            }
        };

    }
}
