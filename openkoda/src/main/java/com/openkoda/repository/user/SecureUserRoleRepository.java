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

import com.openkoda.model.UserRole;
import com.openkoda.model.common.SearchableRepositoryMetadata;
import com.openkoda.repository.SecureRepository;
import org.springframework.stereotype.Repository;

/**
 * Secure marker repository interface for privilege-enforced UserRole operations with SQL subquery-based searchable metadata.
 * <p>
 * This interface extends {@link SecureRepository} to provide privilege-checked repository operations for
 * UserRole association entities. All inherited methods (findOne, findAll, save, delete) enforce privilege
 * checks based on entity requiredReadPrivilege and requiredWritePrivilege formulas. Operations throw
 * {@link org.springframework.security.access.AccessDeniedException} if user lacks required privileges.
 * </p>
 * <p>
 * Searchable metadata configuration via {@link SearchableRepositoryMetadata} annotation:
 * <ul>
 *   <li><b>entityKey:</b> "userRole" - Entity type identifier for search system</li>
 *   <li><b>entityClass:</b> UserRole.class - Entity type for repository operations</li>
 *   <li><b>searchIndexFormula:</b> Complex SQL subquery joining to users table:
 *       <pre>
 *       SELECT u.first_name || ' ' || u.last_name || ' userid:' || COALESCE(CAST(user_id as text), '')
 *              || ' orgid:' || COALESCE(CAST(organization_id as text), '')
 *       FROM users u WHERE u.id = user_id
 *       </pre>
 *       Constructs searchable text from related User name with 'userid:' and 'orgid:' prefixes.
 *       Handles null user_id (default organization roles) with COALESCE.
 *   </li>
 * </ul>
 * Used by search/autocomplete features to index UserRole assignments by user name, user ID, and organization ID.
 * </p>
 * <p>
 * Note: This is a marker interface with no custom methods - all operations inherited from SecureRepository.
 * Instantiated by Spring Data as runtime proxy with security advice. Search index formula uses SQL subquery
 * to denormalize user information for efficient text search without additional joins.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see UserRole
 * @see com.openkoda.model.User
 * @see SecureRepository
 * @see SearchableRepositoryMetadata
 * @see UserRoleRepository
 */
@Repository
@SearchableRepositoryMetadata(
        entityClass = UserRole.class,
        entityKey = "userRole",
        searchIndexFormula = """
            (select u.first_name || ' ' || u.last_name || ' userid:' || COALESCE(CAST (user_id as text), '')
             || ' orgid:' || COALESCE(CAST (organization_id as text), '')
             from users u where u.id = user_id)
            """
)
public interface SecureUserRoleRepository extends SecureRepository<UserRole> {


}
