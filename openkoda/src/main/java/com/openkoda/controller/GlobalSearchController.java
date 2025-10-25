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

import com.openkoda.core.job.JobsScheduler;
import com.openkoda.model.common.SearchableEntity;
import com.openkoda.model.common.SearchableRepositoryMetadata;
import com.openkoda.repository.SearchableRepositories;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.openkoda.controller.common.URLConstants._HTML;

/**
 * REST controller implementing global search endpoint under HTML namespace.
 * <p>
 * Concrete implementation of {@link AbstractGlobalSearchController}. Provides search functionality 
 * accessible from main navigation. Injects {@code @Qualifier("search")} Pageable and 
 * {@code @RequestParam("search_search")} query parameter. Delegates to 
 * {@link AbstractGlobalSearchController#findSearchResult(Pageable, String)}, returns ModelAndView 
 * with 'search' view name. Thin adapter with lightweight logging.
 * </p>
 * <p>
 * The search is performed among entities implementing {@link SearchableEntity} interface which 
 * requires implementation of {@link SearchableEntity#getIndexString()}. The implementation is 
 * done at repository level via {@link SearchableRepositoryMetadata#searchIndexFormula()} annotation. 
 * The {@link SearchableEntity#getIndexString()} field for every searchable entity is updated 
 * continuously as configured in {@link JobsScheduler#searchIndexUpdaterJob()}. Initial setup of 
 * {@link SearchableRepositories#getSearchIndexUpdates()} required for this update occurs at 
 * application startup in {@link SearchableRepositories#discoverSearchableRepositories()}.
 * </p>
 * <p>
 * Request mapping: Typically "/search" under HTML namespace. Returns HTML view for browser rendering.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractGlobalSearchController
 * @see SearchableEntity
 * @see SearchableRepositories
 */
@RestController
@RequestMapping(_HTML)
public class GlobalSearchController extends AbstractGlobalSearchController {

    /**
     * Executes platform-wide search and renders results page.
     * <p>
     * HTTP mapping: {@code GET /search}
     * </p>
     * <p>
     * Delegates to {@link AbstractGlobalSearchController#findSearchResult(Pageable, String)} which 
     * performs full-text search across {@link com.openkoda.model.GlobalEntitySearch} entities, 
     * filters by user privileges, and paginates results. The Flow-based implementation from parent 
     * class handles search execution and result assembly.
     * </p>
     * <p>
     * Response format: View 'search' with attributes:
     * </p>
     * <ul>
     *   <li>'searchResults' (List): Filtered and paginated search result entities</li>
     *   <li>'query' (String): Original search query for display</li>
     *   <li>'pagination' (PageMetadata): Pagination metadata for navigation</li>
     * </ul>
     *
     * @param searchPageable Pagination parameters with {@code @Qualifier("search")} for custom page size.
     *                       Specifies page number, page size, and sort order for results.
     * @param search Search query string from {@code @RequestParam("search_search")}. 
     *               Defaults to empty string if not provided. Used for full-text search across 
     *               indexed entity fields.
     * @return ModelAndView with 'search' view name and results populated in PageModelMap. 
     *         Contains search results, query echo, and pagination metadata for view rendering.
     */
    @GetMapping(value = _SEARCH)
    //TODO Rule 1.4 All methods in non-public controllers must have @PreAuthorize
    public Object getSearchResult(@Qualifier("search") Pageable searchPageable,
                                  @RequestParam(required = false, defaultValue = "", name = "search_search") String search) {
        debug("[getSearchResult] {}", search);
        return findSearchResult(searchPageable, search)
                .mav("search");
    }

}
