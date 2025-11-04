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

package com.openkoda.controller;

import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageModelMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;

import static com.openkoda.repository.specifications.GlobalSearchSpecifications.createSpecification;

/**
 * Abstract base controller providing global search functionality across multiple entity types.
 * <p>
 * This controller implements platform-wide search features using GlobalEntitySearch entities.
 * It executes full-text search queries across indexed entities, filters results by user privileges,
 * and returns paginated search results. The search functionality supports both HTML and JSON response formats.
 * 
 * <p>
 * Subclasses implement concrete search endpoints with specific request mappings. The search uses
 * Lucene/Hibernate Search integration for efficient full-text queries across entity content.
 * All search results are automatically filtered to include only entities the current user has
 * permission to view based on their assigned privileges.
 * 
 * <p>
 * Typical Flow pipeline pattern used in search:
 * <pre>{@code
 * Flow.init()
 *     .thenSet(searchResults, a -> repositories.search.findByQuery(query, pageable))
 *     .thenSet(filtered, a -> filterByPrivileges(searchResults))
 *     .execute();
 * }</pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.GlobalEntitySearch
 * @see GlobalSearchController
 */
public abstract class AbstractGlobalSearchController extends AbstractController {


    /**
     * Executes global search query and returns paginated results.
     * <p>
     * This method performs full-text search across GlobalEntitySearch entities using the provided
     * search query string. It applies pagination parameters to limit result sets, executes the search
     * using Hibernate Search specifications, and assembles a PageModelMap containing search results
     * and pagination metadata.
     * 
     * <p>
     * The search implementation uses Flow pipeline pattern to compose the search operation:
     * <ol>
     *   <li>Store the search term in the flow context</li>
     *   <li>Execute repository search with JPA Specification from GlobalSearchSpecifications</li>
     *   <li>Return PageModelMap with 'searchTerm' and 'searchPage' attributes</li>
     * </ol>
     * 
     * <p>
     * Search results include only entities visible to the current user based on their assigned
     * read privileges. The underlying repository (repositories.unsecure.search) performs the search,
     * while privilege filtering occurs at the entity level based on required permissions stored
     * in GlobalEntitySearch records.
     * 
     *
     * @param searchPageable Pagination parameters (page number, page size, sort direction) 
     *                       injected with @Qualifier("search") for custom page size configuration
     * @param search Search query string from request parameter "search_search". 
     *               Defaults to empty string if not provided. Used for full-text matching 
     *               across indexed entity fields
     * @return PageModelMap containing 'searchTerm' (the query string) and 'searchPage' 
     *         (Page of GlobalEntitySearch results with pagination metadata)
     */
    protected PageModelMap findSearchResult(@Qualifier("search") Pageable searchPageable, @RequestParam(required = false, defaultValue = "", name = "search_search") String search) {
        debug("[findSearchResult] Search: {}", search);
        return Flow.init()
                .thenSet(searchTerm, a -> search)
                .thenSet(searchPage, a -> repositories.unsecure.search.findAll(createSpecification(search), searchPageable))
                .execute();
    }
}
