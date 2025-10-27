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
import com.openkoda.model.authentication.FacebookUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;

/**
 * Repository managing FacebookUser entities for Facebook OAuth integration.
 * <p>
 * Provides CRUD operations and privilege-protected queries for Facebook OAuth user mappings.
 * This repository enables linking external Facebook authentication with internal User entities
 * through the Facebook OAuth flow.
 * </p>
 * <p>
 * OAuth Flow:
 * External Facebook authentication → token exchange → findByFacebookId lookup → link/create User entity
 * </p>
 * <p>
 * Persistence: Persists to 'facebook_user' table
 * </p>
 * <p>
 * Security: Method-level {@code @PreAuthorize} CHECK_CAN_READ_FACEBOOK_USER privilege enforcement
 * via Spring Security. All query methods require appropriate privileges.
 * </p>
 * <p>
 * Usage Context: Used by Facebook OAuth callback controllers for account linking and authentication
 * during the user login process.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see FacebookUser
 * @see HasSecurityRules
 */
@Repository
public interface FacebookUserRepository extends JpaRepository<FacebookUser, Long>, HasSecurityRules {


    /**
     * Finds FacebookUser entity by Facebook user identifier from OAuth response.
     * <p>
     * This method is protected by {@code @PreAuthorize} annotation requiring
     * CHECK_CAN_READ_FACEBOOK_USER privilege. It performs a lookup by the unique
     * Facebook ID received during OAuth authentication flow.
     * </p>
     * <p>
     * Spring Data automatically generates the query based on method name pattern.
     * The repository is a thread-safe Spring-managed singleton.
     * </p>
     *
     * @param facebookId Facebook user identifier from OAuth response, must not be null
     * @return FacebookUser entity with matching Facebook ID, or null if not found
     * @throws org.springframework.security.access.AccessDeniedException if user lacks CHECK_CAN_READ_FACEBOOK_USER privilege
     */
    @PreAuthorize(CHECK_CAN_READ_FACEBOOK_USER)
    FacebookUser findByFacebookId(String facebookId);

}
