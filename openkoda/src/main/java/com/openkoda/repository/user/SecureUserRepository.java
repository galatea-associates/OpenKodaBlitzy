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

package com.openkoda.repository.user;

import com.openkoda.model.User;
import com.openkoda.model.common.SearchableRepositoryMetadata;
import com.openkoda.repository.SecureRepository;
import org.springframework.stereotype.Repository;

import static com.openkoda.controller.common.URLConstants.*;

/**
 * Secure marker repository interface for privilege-enforced User operations with rich searchable metadata.
 * <p>
 * This interface extends {@link SecureRepository} to provide privilege-checked repository operations for
 * User entities. All inherited methods (findOne, findAll, save, delete) enforce privilege checks based
 * on entity requiredReadPrivilege and requiredWritePrivilege formulas. Operations throw
 * {@link org.springframework.security.access.AccessDeniedException} if user lacks required privileges.
 * </p>
 * <p>
 * Searchable metadata configuration via {@link SearchableRepositoryMetadata} annotation:
 * <ul>
 *   <li><b>entityKey:</b> USER - Entity type identifier for search system</li>
 *   <li><b>descriptionFormula:</b> "(concat(first_name,' ', last_name))" - SQL formula returning full name as description</li>
 *   <li><b>globalPathFormula:</b> "'/html/user/' || id || '/profile'" - SQL formula generating deep link to user profile page</li>
 *   <li><b>searchIndexFormula:</b> "lower(coalesce(first_name, '') || coalesce(last_name, '') || ' userid:' || COALESCE(CAST (id as text), '') || ' ' || email)" 
 *       - Lowercase searchable text concatenating first name, last name, 'userid:' prefix with ID, and email</li>
 *   <li><b>entityClass:</b> User.class - Entity type for repository operations</li>
 * </ul>
 * Used by search/autocomplete features to index and retrieve users by name, ID, and email with direct profile links.
 * </p>
 * <p>
 * Note: This is a marker interface with no custom methods - all operations inherited from SecureRepository.
 * Instantiated by Spring Data as runtime proxy with security advice. Global path formula enables search
 * results to link directly to user profile pages.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see User
 * @see SecureRepository
 * @see SearchableRepositoryMetadata
 * @see UserRepository
 */
@Repository
@SearchableRepositoryMetadata(
        entityKey = USER,
        descriptionFormula = "(concat(first_name,' ', last_name))",
        globalPathFormula = "'" + _HTML + "/user/' || id || '" + _PROFILE + "'",
        entityClass = User.class,
        searchIndexFormula =  "lower(coalesce(first_name, '') || ' ' || coalesce(last_name, '') || ' userid:' || COALESCE(CAST (id as text), '') || ' ' || email)"
)
public interface SecureUserRepository extends SecureRepository<User> {


}
