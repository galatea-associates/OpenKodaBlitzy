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
 * JPA domain models for authentication credentials and external authentication provider identity metadata.
 * <p>
 * This package contains entity classes representing various authentication methods supported by OpenKoda,
 * including local password-based authentication, API key authentication, OAuth provider integrations
 * (Facebook, Google, LinkedIn, Salesforce), and enterprise LDAP authentication. All authentication entities
 * maintain a one-to-one relationship with the core {@link com.openkoda.model.User} entity using JPA's
 * {@code @MapsId} annotation to share the same primary key.
 * 
 *
 * <b>Authentication Architecture</b>
 * <p>
 * OpenKoda supports multiple authentication methods that can be used independently or in combination:
 * 
 * <ul>
 *   <li><b>Local Password Authentication</b>: Traditional username/password stored in {@code LoginAndPassword} entity</li>
 *   <li><b>API Key Authentication</b>: Programmatic access via tokens stored in {@code ApiKey} entity</li>
 *   <li><b>OAuth Providers</b>: Social login via Facebook, Google, LinkedIn, Salesforce with provider-specific entities</li>
 *   <li><b>LDAP Authentication</b>: Enterprise directory integration via {@code LDAPUser} entity</li>
 * </ul>
 *
 * <b>Provider Integration Patterns</b>
 * <p>
 * All authentication entities follow a consistent integration pattern with the User entity:
 * 
 * <pre>
 * // One-to-one shared primary key relationship
 * {@code @Entity}
 * {@code @Table(name = "kk_login_and_password")}
 * public class LoginAndPassword {
 *     {@code @Id}
 *     private Long userId;
 *     
 *     {@code @OneToOne}
 *     {@code @MapsId}
 *     {@code @JoinColumn(name = "user_id")}
 *     private User user;
 *     
 *     {@code @JsonIgnore}
 *     private String password; // BCrypt encoded
 * }
 * </pre>
 *
 * <b>Key Entities</b>
 * <ul>
 *   <li><b>LoginAndPassword</b>: Local username/password authentication with BCrypt encoding</li>
 *   <li><b>ApiKey</b>: Programmatic API access tokens with optional expiration</li>
 *   <li><b>FacebookUser, GoogleUser, LinkedInUser, SalesforceUser</b>: OAuth provider-specific identity metadata</li>
 *   <li><b>LDAPUser</b>: Enterprise LDAP directory authentication integration</li>
 *   <li><b>LoggedUser</b>: Abstract base class for authentication entities requiring common audit behavior</li>
 * </ul>
 *
 * <b>JPA Mapping Patterns</b>
 * <p>
 * All authentication entities use consistent JPA mapping annotations:
 * 
 * <ul>
 *   <li>{@code @OneToOne @MapsId}: Shares primary key with User entity for efficient joins</li>
 *   <li>{@code @JsonIgnore}: Excludes sensitive fields (passwords, tokens) from JSON serialization</li>
 *   <li>{@code @DynamicUpdate}: Optimizes SQL UPDATE statements to include only changed columns</li>
 *   <li>{@code @Table(name = "kk_*")}: Consistent table naming convention with "kk_" prefix</li>
 * </ul>
 *
 * <b>Security Patterns</b>
 * <p>
 * The package implements security best practices for credential management:
 * 
 * <ul>
 *   <li><b>BCrypt Password Encoding</b>: LoginAndPassword uses static {@code PasswordEncoder} for one-way hashing</li>
 *   <li><b>Audit Exclusion</b>: Sensitive fields listed in {@code ignoredProperties} to prevent audit logging</li>
 *   <li><b>JSON Serialization Protection</b>: {@code @JsonIgnore} prevents accidental credential exposure</li>
 *   <li><b>Token Security</b>: API keys and OAuth tokens stored securely with optional expiration</li>
 * </ul>
 *
 * <b>Usage Examples</b>
 * <p>
 * Creating a local password authentication for a user:
 * 
 * <pre>
 * User user = new User();
 * user.setEmail("user@example.com");
 * 
 * LoginAndPassword login = new LoginAndPassword(user);
 * login.setPassword("plainPassword"); // Automatically BCrypt encoded
 * login.setPasswordEncoded(LoginAndPassword.passwordEncoder.encode("plainPassword"));
 * </pre>
 * <p>
 * Linking an OAuth provider to an existing user:
 * 
 * <pre>
 * GoogleUser googleUser = new GoogleUser();
 * googleUser.setUser(existingUser);
 * googleUser.setProviderId("google-oauth-id-123");
 * googleUser.setEmail("user@gmail.com");
 * </pre>
 *
 * <b>Relationships</b>
 * <p>
 * All authentication entities maintain a one-to-one relationship with {@link com.openkoda.model.User}.
 * A single user can have multiple authentication methods enabled simultaneously (e.g., both local password
 * and Google OAuth), allowing flexible authentication options.
 * 
 *
 * <b>Common Patterns</b>
 * <ul>
 *   <li><b>Static PasswordEncoder</b>: {@code LoginAndPassword.passwordEncoder} initialized once for BCrypt encoding</li>
 *   <li><b>toAuditString()</b>: All entities implement custom audit string generation excluding sensitive data</li>
 *   <li><b>ignoredProperties Lists</b>: Static lists defining fields excluded from audit trails</li>
 *   <li><b>Constructor Patterns</b>: Entities provide constructors accepting User to establish relationship</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.User
 * @see org.springframework.security.crypto.password.PasswordEncoder
 */
package com.openkoda.model.authentication;