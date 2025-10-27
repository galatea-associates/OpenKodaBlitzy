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

/**
 * External authentication provider integration repositories for OAuth and LDAP user management.
 * <p>
 * This package contains Spring Data JPA repositories for managing external authentication provider
 * integrations. These repositories handle user mappings between OpenKoda User entities and external
 * identity providers including Facebook, Google, LinkedIn, LDAP/Active Directory, and Salesforce.
 * </p>
 *
 * <h2>Supported Authentication Providers</h2>
 * <ul>
 *   <li><b>Facebook</b> - OAuth 2.0 authentication via Facebook Login</li>
 *   <li><b>Google</b> - OAuth 2.0 authentication via Google Sign-In</li>
 *   <li><b>LinkedIn</b> - OAuth 2.0 authentication via LinkedIn Sign-In</li>
 *   <li><b>LDAP/Active Directory</b> - Directory server authentication via LDAP protocol</li>
 *   <li><b>Salesforce</b> - OAuth 2.0 authentication via Salesforce Identity</li>
 * </ul>
 *
 * <h2>OAuth Authentication Flow</h2>
 * <p>
 * The OAuth 2.0 authorization flow for external providers follows these steps:
 * </p>
 * <ol>
 *   <li><b>External Provider Authentication</b> - User logs in with provider credentials (e.g., Facebook login screen)</li>
 *   <li><b>Authorization Callback</b> - Provider redirects to OpenKoda callback URL with authorization code</li>
 *   <li><b>Token Exchange</b> - OpenKoda exchanges authorization code for access token</li>
 *   <li><b>User Identifier Lookup</b> - Repository findBy[Provider]Id methods locate existing user mappings</li>
 *   <li><b>Account Linking</b> - System either links to existing User entity or creates new User</li>
 * </ol>
 *
 * <h2>LDAP Authentication Flow</h2>
 * <p>
 * The LDAP authentication flow uses directory server credentials:
 * </p>
 * <ol>
 *   <li><b>Directory Server Authentication</b> - User provides LDAP credentials (username/password)</li>
 *   <li><b>UID Lookup</b> - System retrieves user identifier (uid) from LDAP directory</li>
 *   <li><b>Repository Query</b> - LDAPUserRepository.findByUid locates existing mapping</li>
 *   <li><b>User Creation/Linking</b> - System creates User entity or links to existing account</li>
 * </ol>
 *
 * <h2>Security Model</h2>
 * <p>
 * All repositories enforce privilege-based access control via Spring Security method-level security.
 * Each repository query requires appropriate CHECK_CAN_READ_* privileges defined via @PreAuthorize
 * annotations. This ensures external authentication operations respect OpenKoda's role-based
 * access control (RBAC) model.
 * </p>
 *
 * <h2>Spring Data Integration</h2>
 * <p>
 * All repositories extend JpaRepository&lt;Entity, Long&gt; and HasSecurityRules interfaces, providing:
 * </p>
 * <ul>
 *   <li>Standard CRUD operations (save, findById, delete)</li>
 *   <li>Pagination and sorting support</li>
 *   <li>Spring Data query derivation from method names</li>
 *   <li>Transaction management via @Transactional</li>
 *   <li>Privilege enforcement via HasSecurityRules contract</li>
 * </ul>
 *
 * <h2>Usage Patterns</h2>
 * <p>
 * These repositories are used by OAuth callback controllers in the
 * com.openkoda.integration.controller package. Controllers handle provider-specific
 * authentication callbacks and delegate user lookup/creation to these repositories.
 * </p>
 *
 * <h2>Persistence</h2>
 * <p>
 * Each repository maps to a corresponding entity table:
 * </p>
 * <ul>
 *   <li>FacebookUserRepository → facebook_user table</li>
 *   <li>GoogleUserRepository → google_user table</li>
 *   <li>LDAPUserRepository → ldap_user table</li>
 *   <li>LinkedinUserRepository → linkedin_user table</li>
 *   <li>SalesforceUserRepository → salesforce_user table</li>
 * </ul>
 *
 * <h2>Key Repository Interfaces</h2>
 * <ul>
 *   <li><b>FacebookUserRepository</b> - Facebook OAuth user mappings with findByFacebookId</li>
 *   <li><b>GoogleUserRepository</b> - Google Sign-In user mappings with findByGoogleId</li>
 *   <li><b>LDAPUserRepository</b> - LDAP/Active Directory user mappings with findByUid</li>
 *   <li><b>LinkedinUserRepository</b> - LinkedIn Sign-In user mappings with findByLinkedinId</li>
 *   <li><b>SalesforceUserRepository</b> - Salesforce OAuth user mappings with findBySalesforceId</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <p>
 * This package requires:
 * </p>
 * <ul>
 *   <li>Spring Data JPA for repository infrastructure</li>
 *   <li>Spring Security for method-level security annotations</li>
 *   <li>JpaRepository base interface for CRUD operations</li>
 *   <li>EntityManager for JPA persistence context</li>
 *   <li>Transaction manager for transactional operations</li>
 *   <li>Datasource configuration for database connectivity</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All repositories are thread-safe Spring-managed singleton beans. Repository methods can be
 * safely invoked from multiple threads concurrently. The underlying EntityManager is thread-safe
 * within transactional boundaries managed by Spring's transaction infrastructure.
 * </p>
 *
 * @see com.openkoda.integration.controller OAuth callback controllers
 * @see com.openkoda.model.authentication External user entity classes
 * @see com.openkoda.repository.user.UserRepository Core user repository
 * @see org.springframework.data.jpa.repository.JpaRepository Spring Data JPA base interface
 * @since 1.7.1
 * @version 1.7.1
 */
package com.openkoda.repository.user.external;