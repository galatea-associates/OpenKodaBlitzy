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

import com.openkoda.core.security.OrganizationUser;
import com.openkoda.core.security.UserProvider;
import com.openkoda.model.GlobalEntitySearch;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

/**
 * JPA Criteria API Specification builders for privilege-aware global entity search queries.
 * <p>
 * Provides static factory methods that return {@link Specification} instances implementing
 * authorization-aware, case-insensitive text search across entities indexed in the GlobalEntitySearch
 * table. Specifications enforce privilege checks by consulting {@link UserProvider#getFromContext()}
 * to filter results based on the current user's global privileges. Unauthenticated requests return
 * empty results via {@code disjunction()} predicates. These specifications are composable via
 * {@code and()}/{@code or()} operators for complex filtering.
 * </p>
 * <p>
 * <b>Note:</b> Uses string-based attribute names ('indexString', 'requiredReadPrivilege') which are
 * fragile to entity refactoring. Consider migrating to JPA metamodel for type safety.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.GlobalEntitySearch
 * @see com.openkoda.core.security.UserProvider
 * @see com.openkoda.core.security.OrganizationUser
 * @see org.springframework.data.jpa.domain.Specification
 */
public class GlobalSearchSpecifications {

    /**
     * Creates a privilege-aware Specification that performs case-insensitive substring search on indexed entities.
     * <p>
     * Normalizes the search term using {@link StringUtils#defaultString} and {@link StringUtils#lowerCase},
     * then constructs a {@code LIKE '%term%'} predicate on the indexString attribute. Enforces authorization
     * by checking if the current user (from {@link UserProvider#getFromContext()}) has the required read
     * privilege. Returns {@code disjunction()} (always-false predicate) for unauthenticated users. For
     * authenticated users, combines text search with privilege check: matches entities where
     * requiredReadPrivilege is null OR present in user's global privileges.
     * </p>
     * <p>
     * Usage example:
     * <pre>{@code
     * Specification<GlobalEntitySearch> spec = createSpecification("invoice").and(additionalFilters);
     * }</pre>
     * </p>
     * <p>
     * <b>Security note:</b> This method enforces row-level security based on user privileges.
     * </p>
     *
     * @param searchTerm the text to search for. Null values are treated as empty strings.
     *                   Case-insensitive substring matching is applied
     * @return Specification for GlobalEntitySearch filtering with privilege enforcement and text search.
     *         Returns always-false predicate for unauthenticated requests
     */
    public static Specification<GlobalEntitySearch> createSpecification(String searchTerm) {
        return new Specification<GlobalEntitySearch>() {
            @Override
            public Predicate toPredicate(Root<GlobalEntitySearch> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
                Predicate search = cb.like(cb.lower(root.get("indexString")), "%" + StringUtils.lowerCase(StringUtils.defaultString(searchTerm, "")) + "%");
                Optional<OrganizationUser> optionalUser = UserProvider.getFromContext();

                if (!optionalUser.isPresent()) {
                    return cb.disjunction();
                }

                OrganizationUser user = optionalUser.get();

                search = cb.and(
                        cb.or(cb.isNull(root.get("requiredReadPrivilege")), root.get("requiredReadPrivilege").in(user.getGlobalPrivileges())),
                        search);

                return search;
            }
        };
    }
}
