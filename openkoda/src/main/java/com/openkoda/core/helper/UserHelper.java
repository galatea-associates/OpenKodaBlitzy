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

package com.openkoda.core.helper;

import com.openkoda.core.security.OrganizationUser;
import com.openkoda.core.security.UserProvider;
import com.openkoda.model.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Provides convenient access to current user information from the UserProvider context.
 * <p>
 * This helper component offers null-safe methods for accessing user details with appropriate
 * default values for unauthenticated users. All methods return non-null values, providing
 * empty collections or default primitives when no authenticated user is present in the context.
 * </p>
 * <p>
 * The helper retrieves user information from the thread-local UserProvider context, making it
 * safe for concurrent use across multiple requests. Each method handles the case where no user
 * is authenticated by returning sensible defaults rather than null values or throwing exceptions.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * String name = userHelper.getFullName(); // Returns "Unlogged" if not authenticated
 * Long userId = userHelper.getUserId();   // Returns -1L if not authenticated
 * </pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see UserProvider
 * @see OrganizationUser
 */
@Component("user")
public class UserHelper {

    /**
     * Returns map entries of organization ID to name for the current authenticated user.
     * <p>
     * Each entry contains the organization ID as the key and organization name as the value,
     * representing all organizations to which the current user belongs. If no user is
     * authenticated or the user belongs to no organizations, returns an empty set.
     * </p>
     *
     * @return a {@link java.util.Set} of map entries containing organization IDs and names,
     *         or an empty set for unlogged users
     */
    public Set<Map.Entry<Long, String>> organizations() {
        return UserProvider.getFromContext().map( OrganizationUser::getOrganizationNames ).map(Map::entrySet).orElse(Collections.emptySet());
    }

    /**
     * Checks if the current authenticated user belongs to any organizations.
     * <p>
     * This method provides a convenient way to determine whether the user has organization
     * memberships without needing to retrieve and check the organization collection size.
     * Returns false for unauthenticated users or users with no organization memberships.
     * </p>
     *
     * @return {@code true} if the current user belongs to at least one organization,
     *         {@code false} for unlogged users or users with no organizations
     */
    public boolean hasOrganizations() {
        return UserProvider.getFromContext().map( OrganizationUser::getOrganizationNames ).map(a -> a.size() > 0).orElse(false);
    }

    /**
     * Returns the unique identifier for the current authenticated user.
     * <p>
     * This method provides null-safe access to the user ID, returning a default sentinel
     * value of -1L for unauthenticated users instead of null. This ensures callers can
     * safely use the returned value without null checks while still distinguishing
     * unauthenticated sessions.
     * </p>
     *
     * @return the user ID as a {@link java.lang.Long}, or -1L as default for unlogged users
     */
    public Long getUserId() {
        return UserProvider.getFromContext().map( OrganizationUser::getUser).map(User::getId ).orElse( -1L );
    }

    /**
     * Returns the full name or identifier for the current authenticated user using priority logic.
     * <p>
     * The method applies the following priority for constructing the user's display name:
     * </p>
     * <ol>
     *   <li>If both first name and last name are present and non-blank: returns "FirstName LastName"</li>
     *   <li>If either name is missing or blank: returns the user's email address</li>
     *   <li>If no authenticated user exists: returns "Unlogged"</li>
     * </ol>
     * <p>
     * This provides a consistent, non-null display value suitable for UI rendering and logging
     * without requiring explicit null handling by callers.
     * </p>
     *
     * @return the user's full name, email, or "Unlogged" as a {@link java.lang.String}
     */
    public String getFullName() {
        return UserProvider.getFromContext().map( OrganizationUser::getUser)
                .map(user -> StringUtils.isNotBlank(user.getFirstName()) && StringUtils.isNotBlank(user.getLastName())
                        ? user.getFirstName() + " " + user.getLastName() : user.getEmail()).orElse( "Unlogged");
    }

    /**
     * Returns the email address for the current authenticated user.
     * <p>
     * This method provides null-safe access to the user's email address, returning an
     * empty string for unauthenticated users. The returned value is always non-null,
     * allowing safe use in string operations without null checks.
     * </p>
     *
     * @return the user's email address as a {@link java.lang.String},
     *         or an empty string for unlogged users
     */
    public String getEmail() {
        return UserProvider.getFromContext().map(OrganizationUser::getUser).map(User::getEmail).orElse("");
    }
}
