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

package com.openkoda.repository.user.external;

import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.authentication.LinkedinUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Repository managing LinkedinUser entities for LinkedIn Sign-In integration.
 * <p>
 * Provides CRUD operations and privilege-protected queries for LinkedIn OAuth user mappings.
 * This repository handles the persistence layer for LinkedIn authentication, enabling the OAuth flow
 * where external LinkedIn authentication leads to token exchange, user lookup, and User entity linking.
 * 
 * <p>
 * Persists to 'linkedin_user' table with method-level {@code @PreAuthorize} privilege enforcement
 * via Spring Security. The CHECK_CAN_READ_LINKEDIN_USER privilege is required for query operations.
 * 
 * <p>
 * Usage context: Used by LinkedIn OAuth callback controllers for account linking, authentication,
 * and admin flows. Extends Spring Data JPA repository for automatic query generation.
 * 
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @version 1.7.1
 * @since 2019-07-18
 * @see LinkedinUser
 * @see HasSecurityRules
 */
public interface LinkedinUserRepository extends JpaRepository<LinkedinUser, Long>, HasSecurityRules {

    /**
     * Finds a LinkedinUser by LinkedIn identifier with privilege protection.
     * <p>
     * This method performs a {@code @PreAuthorize} protected lookup by LinkedIn user identifier
     * from OAuth response. Spring Data JPA automatically generates the query based on the method
     * name pattern {@code findBy[PropertyName]}.
     * 
     * <p>
     * Thread-safety: Repository is a thread-safe Spring-managed singleton.
     * 
     *
     * @param linkedinId LinkedIn user identifier from OAuth response, must not be null
     * @return LinkedinUser entity with matching LinkedIn ID, or null if not found
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_READ_LINKEDIN_USER privilege
     */
    @PreAuthorize(CHECK_CAN_READ_LINKEDIN_USER)
    LinkedinUser findByLinkedinId(String linkedinId);
}
