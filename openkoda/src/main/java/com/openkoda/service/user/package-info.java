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
 * User account management, authentication, and authorization services.
 * <p>
 * This package provides comprehensive user lifecycle services for OpenKoda applications. It handles
 * user account creation, authentication token management, role-based access control (RBAC), API key
 * generation for programmatic access, and privilege evaluation. Services in this package coordinate
 * with Spring Security to provide secure authentication and fine-grained authorization.
 * 
 *
 * <b>Key Services</b>
 * <ul>
 *   <li><b>UserService</b>: Core user management service providing CRUD operations, password management,
 *       and account status control. Handles user creation, updates, deletion, and password reset workflows.</li>
 *   <li><b>RoleService</b>: Manages role definitions, role assignments to users, and privilege synchronization.
 *       Supports global roles, organization-scoped roles, and role hierarchy.</li>
 *   <li><b>BasicPrivilegeService</b>: Provides cached privilege evaluation for performance. Determines if
 *       a user has specific privileges within organizational contexts.</li>
 *   <li><b>TokenService</b>: Generates and validates JWT and opaque authentication tokens. Manages token
 *       lifecycle including expiration and revocation.</li>
 *   <li><b>ApiKeyService</b>: Manages API key lifecycle for programmatic access to OpenKoda APIs.
 *       Handles key generation, validation, and revocation.</li>
 *   <li><b>UserRoleService</b>: Manages the junction between users and roles. Handles user-role assignments
 *       and removals at both global and organization levels.</li>
 * </ul>
 *
 * <b>Authentication Flows</b>
 * <p>
 * The package supports multiple authentication mechanisms:
 * 
 * <ul>
 *   <li><b>Password Login</b>: UserService validates credentials against stored password hashes.
 *       Upon successful validation, TokenService generates a JWT for subsequent requests.</li>
 *   <li><b>API Key Authentication</b>: ApiKeyService validates API key headers against stored keys.
 *       API keys provide long-lived programmatic access without password exposure.</li>
 *   <li><b>OAuth Integration</b>: External OAuth providers authenticate users. Users are linked
 *       to OpenKoda accounts via Token entities for session management.</li>
 * </ul>
 *
 * <b>Authorization Model</b>
 * <p>
 * Authorization follows a role-based access control (RBAC) pattern with these components:
 * 
 * <ul>
 *   <li><b>User to Role Mapping</b>: Users are assigned roles through UserRole junction entities.
 *       Roles can be global or organization-scoped.</li>
 *   <li><b>Role to Privilege Mapping</b>: Each Role entity contains a set of Privilege enum values
 *       that define allowed operations.</li>
 *   <li><b>Privilege Evaluation</b>: BasicPrivilegeService checks if a user has required privileges
 *       by traversing User → UserRole → Role → Privileges.</li>
 *   <li><b>Secure Repositories</b>: Data access layer enforces organization-scoped queries automatically,
 *       ensuring users only access data within their organizational context.</li>
 * </ul>
 *
 * <b>Usage Example</b>
 * <pre>
 * // Create user with role
 * User user = userService.createUser("john@example.com", "password123");
 * roleService.assignRole(user, organizationRole);
 * 
 * // Check privilege
 * boolean canEdit = privilegeService.hasPrivilege(userId, Privilege.EDIT_USER);
 * </pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.0.0
 * @see com.openkoda.model.User
 * @see com.openkoda.model.Role
 * @see com.openkoda.model.Privilege
 * @see com.openkoda.model.UserRole
 * @see com.openkoda.model.Token
 * @see com.openkoda.model.authentication.ApiKey
 */
package com.openkoda.service.user;