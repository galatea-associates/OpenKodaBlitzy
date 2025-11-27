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

 *
 * <b>Package Structure</b>
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
 * <b>Key Components</b>
 *
 * <b>UserRepository - User Entity Management</b>
 * <p>
 * {@link UserRepository} provides comprehensive user lookup methods:
 * <ul>
 *   <li>findByEmail, findByUsernameIgnoreCase - Username/email authentication</li>
 *   <li>findByOrganizationAndRole - Multi-tenant user-role queries</li>
 *   <li>JPQL projections aggregating roles/privileges into Tuple DTOs</li>
 *   <li>Specification+Pageable listings with @PreAuthorize CHECK_CAN_READ_USER_DATA security</li>
 * </ul>
 * Used by UserService, authentication handlers, and privilege evaluation.

 *
 * <b>RoleRepository - RBAC Role Hierarchy</b>
 * <p>
 * {@link RoleRepository} manages Role entities with single-table inheritance:
 * <ul>
 *   <li><b>GlobalRole:</b> System-wide roles (e.g., ROLE_ADMIN)</li>
 *   <li><b>OrganizationRole:</b> Tenant-scoped roles</li>
 *   <li><b>GlobalOrganizationRole:</b> Cross-tenant roles</li>
 *   <li>Privileges stored as joined string, transient privilegesSet with PrivilegeHelper serialization</li>
 *   <li>Security-aware bulk operations (@Modifying deleteRole with CHECK_CAN_MANAGE_ROLES_JPQL)</li>
 * </ul>

 *
 * <b>TokenRepository - Session and API Token Management</b>
 * <p>
 * {@link TokenRepository} manages Token entities for authentication:
 * <ul>
 *   <li>Session tokens, JWT tokens, remember-me tokens</li>
 *   <li>JPQL projection findByUserIdAndTokenWithInvalidationReasons returning Token + validation booleans</li>
 *   <li>Helper findByBase64UserIdTokenIsValidTrue decoding "userId:token" base64 strings</li>
 *   <li>Expiration tracking and invalidation status</li>
 * </ul>

 *
 * <b>DynamicPrivilegeRepository - Runtime Privilege Extension</b>
 * <p>
 * {@link DynamicPrivilegeRepository} enables plugin-based privilege definitions:
 * <ul>
 *   <li>Runtime-defined privileges beyond canonical Privilege enum</li>
 *   <li>Derived finder findByName(String)</li>
 *   <li>Guarded bulk delete deletePrivilege with removable flag and security fragment</li>
 * </ul>

 *
 * <b>RBAC Model</b>
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

 *
 * <b>Authentication Flows</b>
 *
 * <b>Username/Password Authentication</b>
 * <pre>
 *   LoginAndPasswordRepository credentials = loginPasswordRepo.findByLogin(username);
 *   if (passwordEncoder.matches(password, credentials.getPassword())) {
 *       User user = userRepository.findByLogin(username);
 *       // Generate session token
 *       Token token = tokenRepository.save(new Token(user, UUID.randomUUID()));
 *   }
 * </pre>
 *
 * <b>API Key Authentication</b>
 * <pre>
 *   ApiKey apiKey = apiKeyRepository.findByKeyValue(requestApiKey);
 *   if (apiKey != null &amp;&amp; !apiKey.isExpired()) {
 *       User user = apiKey.getUser();
 *       // Authenticate request
 *   }
 * </pre>
 *
 * <b>OAuth/External Authentication</b>
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

 *
 * <b>Secure Marker Interfaces</b>
 * <p>
 * Secure*Repository interfaces extend {@link com.openkoda.repository.SecureRepository} and are annotated with
 * {@link com.openkoda.model.common.SearchableRepositoryMetadata}:
 * <ul>
 *   <li>{@link SecureUserRepository} - Privilege-enforced user operations, searchable by name/email/ID</li>
 *   <li>{@link SecureRoleRepository} - Privilege-enforced role operations, searchable by role name</li>
 *   <li>{@link SecureDynamicPrivilegeRepository} - Privilege-enforced privilege operations</li>
 *   <li>{@link SecureUserRoleRepository} - Privilege-enforced user-role assignment operations</li>
 * </ul>
 * All operations throw AccessDeniedException if user lacks required privileges.

 *
 * <b>Usage Patterns</b>
 *
 * <b>Finding Users by Role</b>
 * <pre>
 *   List&lt;User&gt; admins = userRepository.findByOrganizationIdAndRole(orgId, "ROLE_ADMIN");
 * </pre>
 *
 * <b>Privilege-Enforced User Lookup</b>
 * <pre>
 *   &#64;Autowired SecureUserRepository secureUserRepo;
 *   User user = secureUserRepo.findOne(userId);  // Checks requiredReadPrivilege
 * </pre>
 *
 * <b>Role Assignment</b>
 * <pre>
 *   UserRole assignment = new UserRole(user, role, organization);
 *   userRoleRepository.save(assignment);
 * </pre>
 *
 * <b>Token Validation</b>
 * <pre>
 *   Tuple2&lt;Token, String&gt; result = tokenRepository.findByBase64UserIdTokenIsValidTrue(base64Token);
 *   Token token = result.getT1();
 *   String validationMessage = result.getT2();  // "VALID", "EXPIRED", "ALREADY_USED", or "INVALID"
 * </pre>
 *
 * <b>Dependencies</b>
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
 * <b>Relationships</b>
 * <ul>
 *   <li><b>Depends on:</b> com.openkoda.model.authentication (entities), com.openkoda.core.repository (base interfaces), com.openkoda.core.security (privilege rules)</li>
 *   <li><b>Used by:</b> com.openkoda.service.user (UserService, RoleService), authentication handlers, com.openkoda.controller (user/role management)</li>
 * </ul>
 *
 * <b>Database Schema</b>
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
 * @see com.openkoda.model.User
 * @see com.openkoda.model.Role
 * @see com.openkoda.model.Privilege
 * @see com.openkoda.repository.user.external
 * @see com.openkoda.model.common.SearchableRepositoryMetadata
 * @see com.openkoda.repository.SecureRepository
 */
package com.openkoda.repository.user;