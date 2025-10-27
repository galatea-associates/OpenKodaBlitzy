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
 * User authentication, authorization, and RBAC (Role-Based Access Control) repository interfaces.
 * <p>
 * This package provides Spring Data JPA repository interfaces for managing User, Role, Privilege, and authentication
 * entities that form the foundation of OpenKoda's security model. It includes standard CRUD repositories, secure
 * marker interfaces with privilege enforcement, and specialized repositories for external authentication providers.
 * </p>
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li><b>User Management:</b> UserRepository, LoginAndPasswordRepository for user entities and credentials</li>
 *   <li><b>Role Management:</b> RoleRepository (single-table inheritance), GlobalRoleRepository, OrganizationRoleRepository, GlobalOrganizationRoleRepository</li>
 *   <li><b>Privilege Management:</b> DynamicPrivilegeRepository for runtime privilege definitions</li>
 *   <li><b>Association:</b> UserRoleRepository for user-role assignments</li>
 *   <li><b>Authentication:</b> TokenRepository (session/JWT tokens), ApiKeyRepository (API authentication)</li>
 *   <li><b>Secure Markers:</b> SecureUserRepository, SecureRoleRepository, SecureDynamicPrivilegeRepository, SecureUserRoleRepository with SearchableRepositoryMetadata</li>
 *   <li><b>External Providers:</b> {@link com.openkoda.repository.user.external} subpackage for OAuth/LDAP integration</li>
 * </ul>
 *
 * <h2>Key Components</h2>
 *
 * <h3>UserRepository - User Entity Management</h3>
 * <p>
 * {@link UserRepository} provides comprehensive user lookup methods:
 * <ul>
 *   <li>findByEmail, findByUsernameIgnoreCase - Username/email authentication</li>
 *   <li>findByOrganizationAndRole - Multi-tenant user-role queries</li>
 *   <li>JPQL projections aggregating roles/privileges into Tuple DTOs</li>
 *   <li>Specification+Pageable listings with @PreAuthorize CHECK_CAN_READ_USER_DATA security</li>
 * </ul>
 * Used by UserService, authentication handlers, and privilege evaluation.
 * </p>
 *
 * <h3>RoleRepository - RBAC Role Hierarchy</h3>
 * <p>
 * {@link RoleRepository} manages Role entities with single-table inheritance:
 * <ul>
 *   <li><b>GlobalRole:</b> System-wide roles (e.g., ROLE_ADMIN)</li>
 *   <li><b>OrganizationRole:</b> Tenant-scoped roles</li>
 *   <li><b>GlobalOrganizationRole:</b> Cross-tenant roles</li>
 *   <li>Privileges stored as joined string, transient privilegesSet with PrivilegeHelper serialization</li>
 *   <li>Security-aware bulk operations (@Modifying deleteRole with CHECK_CAN_MANAGE_ROLES_JPQL)</li>
 * </ul>
 * </p>
 *
 * <h3>TokenRepository - Session and API Token Management</h3>
 * <p>
 * {@link TokenRepository} manages Token entities for authentication:
 * <ul>
 *   <li>Session tokens, JWT tokens, remember-me tokens</li>
 *   <li>JPQL projection findByUserIdAndTokenWithInvalidationReasons returning Token + validation booleans</li>
 *   <li>Helper findByBase64UserIdTokenIsValidTrue decoding "userId:token" base64 strings</li>
 *   <li>Expiration tracking and invalidation status</li>
 * </ul>
 * </p>
 *
 * <h3>DynamicPrivilegeRepository - Runtime Privilege Extension</h3>
 * <p>
 * {@link DynamicPrivilegeRepository} enables plugin-based privilege definitions:
 * <ul>
 *   <li>Runtime-defined privileges beyond canonical Privilege enum</li>
 *   <li>Derived finder findByName(String)</li>
 *   <li>Guarded bulk delete deletePrivilege with removable flag and security fragment</li>
 * </ul>
 * </p>
 *
 * <h2>RBAC Model</h2>
 * <p>
 * The Role-Based Access Control model follows these patterns:
 * <ul>
 *   <li><b>User:</b> Authenticated identity with email, username, organization memberships</li>
 *   <li><b>Role:</b> Named permission set (GlobalRole, OrganizationRole, GlobalOrganizationRole)</li>
 *   <li><b>Privilege:</b> Canonical enum-based privileges + runtime DynamicPrivilege</li>
 *   <li><b>UserRole:</b> Association entity linking users to roles with optional organization scope</li>
 *   <li><b>Entity Privileges:</b> Entities define requiredReadPrivilege and requiredWritePrivilege via @Formula</li>
 *   <li><b>SecureRepository:</b> Enforces privilege checks on all repository operations</li>
 * </ul>
 * </p>
 *
 * <h2>Authentication Flows</h2>
 *
 * <h3>Username/Password Authentication</h3>
 * <pre>
 *   LoginAndPasswordRepository credentials = loginPasswordRepo.findByLogin(username);
 *   if (passwordEncoder.matches(password, credentials.getPassword())) {
 *       User user = userRepository.findByLogin(username);
 *       // Generate session token
 *       Token token = tokenRepository.save(new Token(user, UUID.randomUUID()));
 *   }
 * </pre>
 *
 * <h3>API Key Authentication</h3>
 * <pre>
 *   ApiKey apiKey = apiKeyRepository.findByKeyValue(requestApiKey);
 *   if (apiKey != null &amp;&amp; !apiKey.isExpired()) {
 *       User user = apiKey.getUser();
 *       // Authenticate request
 *   }
 * </pre>
 *
 * <h3>OAuth/External Authentication</h3>
 * <p>
 * See {@link com.openkoda.repository.user.external} for OAuth provider repositories:
 * <ul>
 *   <li>FacebookUserRepository - Facebook OAuth user mappings</li>
 *   <li>GoogleUserRepository - Google OAuth user mappings</li>
 *   <li>LDAPUserRepository - LDAP external user mappings</li>
 *   <li>LinkedinUserRepository - LinkedIn OAuth user mappings</li>
 *   <li>SalesforceUserRepository - Salesforce OAuth user mappings</li>
 * </ul>
 * Each maps external user identifiers to internal User entities.
 * </p>
 *
 * <h2>Secure Marker Interfaces</h2>
 * <p>
 * Secure*Repository interfaces extend {@link com.openkoda.repository.SecureRepository} and are annotated with
 * {@link com.openkoda.core.repository.common.SearchableRepositoryMetadata}:
 * <ul>
 *   <li>{@link SecureUserRepository} - Privilege-enforced user operations, searchable by name/email/ID</li>
 *   <li>{@link SecureRoleRepository} - Privilege-enforced role operations, searchable by role name</li>
 *   <li>{@link SecureDynamicPrivilegeRepository} - Privilege-enforced privilege operations</li>
 *   <li>{@link SecureUserRoleRepository} - Privilege-enforced user-role assignment operations</li>
 * </ul>
 * All operations throw AccessDeniedException if user lacks required privileges.
 * </p>
 *
 * <h2>Usage Patterns</h2>
 *
 * <h3>Finding Users by Role</h3>
 * <pre>
 *   List&lt;User&gt; admins = userRepository.findByOrganizationIdAndRole(orgId, "ROLE_ADMIN");
 * </pre>
 *
 * <h3>Privilege-Enforced User Lookup</h3>
 * <pre>
 *   &#64;Autowired SecureUserRepository secureUserRepo;
 *   User user = secureUserRepo.findOne(userId);  // Checks requiredReadPrivilege
 * </pre>
 *
 * <h3>Role Assignment</h3>
 * <pre>
 *   UserRole assignment = new UserRole(user, role, organization);
 *   userRoleRepository.save(assignment);
 * </pre>
 *
 * <h3>Token Validation</h3>
 * <pre>
 *   Tuple2&lt;Token, String&gt; result = tokenRepository.findByBase64UserIdTokenIsValidTrue(base64Token);
 *   Token token = result.getT1();
 *   String validationMessage = result.getT2();  // "VALID", "EXPIRED", "ALREADY_USED", or "INVALID"
 * </pre>
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>Spring Data JPA - Repository proxying and query derivation</li>
 *   <li>Hibernate/JPA - ORM and entity lifecycle management</li>
 *   <li>Spring Security - @PreAuthorize expressions and HasSecurityRules integration</li>
 *   <li>Apache Commons Codec - Base64 decoding in TokenRepository</li>
 *   <li>com.openkoda.model.authentication - User, Role, Privilege, Token, ApiKey entities</li>
 *   <li>com.openkoda.core.repository - Base repository interfaces (FunctionalRepositoryWithLongId, SecureRepository)</li>
 *   <li>com.openkoda.core.security - HasSecurityRules, PrivilegeHelper, privilege constants</li>
 *   <li>com.openkoda.core.flow - Tuple DTOs for projection queries</li>
 *   <li>Reactor - Tuple2 for multi-value returns</li>
 * </ul>
 *
 * <h2>Relationships</h2>
 * <ul>
 *   <li><b>Depends on:</b> com.openkoda.model.authentication (entities), com.openkoda.core.repository (base interfaces), com.openkoda.core.security (privilege rules)</li>
 *   <li><b>Used by:</b> com.openkoda.service.user (UserService, RoleService), authentication handlers, com.openkoda.controller (user/role management)</li>
 * </ul>
 *
 * <h2>Database Schema</h2>
 * <ul>
 *   <li><b>users table:</b> User entities with email, username, organization memberships, computed privilege formulas</li>
 *   <li><b>roles table:</b> Single-table inheritance (type discriminator), privileges joined string</li>
 *   <li><b>user_role table:</b> User-role association with optional organization_id for tenant scoping</li>
 *   <li><b>login_and_password table:</b> Credential storage with bcrypt hashed passwords</li>
 *   <li><b>token table:</b> Session/JWT token storage with expiration tracking</li>
 *   <li><b>api_key table:</b> API authentication key storage</li>
 *   <li><b>dynamic_privilege table:</b> Runtime privilege definitions</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.authentication.User
 * @see com.openkoda.model.authentication.Role
 * @see com.openkoda.model.Privilege
 * @see com.openkoda.repository.user.external
 * @see com.openkoda.core.repository.common.SearchableRepositoryMetadata
 * @see com.openkoda.repository.SecureRepository
 */
package com.openkoda.repository.user;