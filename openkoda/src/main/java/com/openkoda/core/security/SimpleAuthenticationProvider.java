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

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Minimal pass-through AuthenticationProvider that accepts any UsernamePasswordAuthenticationToken without credential verification.
 * <p>
 * <b>WARNING:</b> This provider provides NO security - it accepts any username/password combination without validation
 * and creates an authenticated token with empty authorities. This implementation performs no password verification,
 * no UserDetailsService lookup, no database queries, and no password hashing checks.
 * </p>
 * <p>
 * This is a stateless operation that simply extracts credentials from the authentication request and returns
 * a new authenticated token. The provider is registered via Spring Security @Component auto-detection and added
 * to the AuthenticationManager provider chain.
 * </p>
 * <p>
 * <b>Potential Removal:</b> This class is flagged for review as it bypasses authentication security. It may be
 * legacy from development or testing phases. In production environments, prefer
 * {@link LoginByPasswordOrTokenAuthenticationProvider} which provides proper credential validation.
 * </p>
 * <p>
 * <b>Usage Example (NOT recommended for production):</b>
 * <pre>
 * // This provider accepts any credentials:
 * UsernamePasswordAuthenticationToken auth = 
 *     new UsernamePasswordAuthenticationToken("anyuser", "anypassword");
 * Authentication result = simpleAuthProvider.authenticate(auth);
 * // result.isAuthenticated() returns true without any validation
 * </pre>
 * </p>
 *
 * @see LoginByPasswordOrTokenAuthenticationProvider
 * @see org.springframework.security.authentication.UsernamePasswordAuthenticationToken
 * @since 1.7.1
 * @author OpenKoda Team
 * 
 * TODO: check if can be removed
 */
@Component
public class SimpleAuthenticationProvider implements AuthenticationProvider {

    /**
     * Implements AuthenticationProvider contract by creating an authenticated UsernamePasswordAuthenticationToken
     * without performing credential verification.
     * <p>
     * <b>Security Risk:</b> This method accepts any username and password combination without validation.
     * It extracts the username and password from the input authentication token and creates a new authenticated
     * token with empty authorities, bypassing all security checks.
     * </p>
     * <p>
     * Method behavior:
     * <ol>
     * <li>Extracts username via {@code authentication.getName()}</li>
     * <li>Extracts password via {@code authentication.getCredentials().toString()}</li>
     * <li>Creates new UsernamePasswordAuthenticationToken with name, password, and empty ArrayList</li>
     * <li>Returns authenticated token (authenticated flag = true)</li>
     * <li>NO password verification performed</li>
     * <li>NO UserDetailsService lookup performed</li>
     * <li>NO privilege loading performed</li>
     * </ol>
     * </p>
     * <p>
     * <b>Warning:</b> This allows authentication bypass if this provider is processed before secure providers
     * in the AuthenticationManager chain. The returned token has empty authorities, meaning the authenticated
     * user has no GrantedAuthority instances and no privileges.
     * </p>
     *
     * @param authentication Input Authentication instance, typically an unauthenticated UsernamePasswordAuthenticationToken
     *                      from a filter
     * @return UsernamePasswordAuthenticationToken with name and password credentials, empty authorities list,
     *         and authenticated flag set to true
     * @throws AuthenticationException Never thrown by this implementation (pass-through provider)
     */
    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String name = authentication.getName();
        String password = authentication.getCredentials().toString();
        return new UsernamePasswordAuthenticationToken(name, password, new ArrayList<>());
    }

    /**
     * Declares support for UsernamePasswordAuthenticationToken authentication requests.
     * <p>
     * Spring Security calls this method to determine which AuthenticationProvider should handle
     * each Authentication type. This provider declares that it can process UsernamePasswordAuthenticationToken
     * instances.
     * </p>
     * <p>
     * <b>Note:</b> This provider competes with {@link LoginByPasswordOrTokenAuthenticationProvider} for
     * UsernamePasswordAuthenticationToken processing. Provider order in the AuthenticationManager chain
     * determines which provider handles the authentication request first.
     * </p>
     *
     * @param authentication Class type of Authentication being checked
     * @return true if authentication equals UsernamePasswordAuthenticationToken.class, false otherwise
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(
                UsernamePasswordAuthenticationToken.class);
    }
}
