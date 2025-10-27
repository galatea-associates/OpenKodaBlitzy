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

package com.openkoda.core.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Marker authentication token for preauthenticated user reload flows.
 * <p>
 * This class extends {@code UsernamePasswordAuthenticationToken} and serves as a type discriminator
 * for {@code LoginByPasswordOrTokenAuthenticationProvider}. It is used by {@code UserProvider} when
 * reloading a principal from the database after privilege changes. When a {@code wasModifiedSince}
 * check triggers a reload, {@code UserProvider} creates this token to rebuild the {@code OrganizationUser}
 * with updated roles and privileges.
 * </p>
 * <p>
 * The {@code LoginByPasswordOrTokenAuthenticationProvider} recognizes this token type and handles it
 * without password verification, since the user has already been authenticated. This is a thin wrapper
 * with no additional fields beyond the superclass properties (principal, credentials, authorities).
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * OrganizationUser user = userRepository.findById(userId);
 * Collection<GrantedAuthority> authorities = loadAuthorities(user);
 * return new PreauthenticatedReloadUserToken(user, null, authorities);
 * }</pre>
 * </p>
 *
 * @see UserProvider
 * @see LoginByPasswordOrTokenAuthenticationProvider
 * @see OrganizationUser
 * @since 1.7.1
 * @author OpenKoda Team
 */
// TODO: Examine if this can be removed
public class PreauthenticatedReloadUserToken extends UsernamePasswordAuthenticationToken {

    /**
     * Creates an authenticated token for preauthenticated user reload scenario.
     * <p>
     * This constructor builds an authentication token for reloading a user's principal
     * and privileges from the database without requiring password verification. The token
     * is created in an authenticated state (isAuthenticated() returns true) with the
     * updated user information.
     * </p>
     *
     * @param principal the OrganizationUser principal being reloaded from database with updated privileges
     * @param credentials the user credentials (typically null for preauthenticated flows where password already verified)
     * @param authorities the collection of GrantedAuthority instances representing user's current privileges after reload
     */
    public PreauthenticatedReloadUserToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }

}
