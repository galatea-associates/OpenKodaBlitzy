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
 * Provides web controllers for user account management and password recovery functionality.
 * 
 * <p>This package contains Spring MVC controllers responsible for managing the complete user lifecycle,
 * including account creation, email verification, profile management, password recovery, and user
 * administration operations. Controllers in this package handle both HTML-based user interfaces and
 * RESTful API endpoints for user-related operations.</p>
 * 
 * <h2>Key Controller Classes</h2>
 * <ul>
 *   <li>{@code AbstractUserController} - Base controller providing common user management functionality
 *       and shared Flow pipeline operations for user-related requests</li>
 *   <li>{@code UserControllerHtml} - Handles HTML-based user interface operations including user listing,
 *       creation, profile editing, and account management</li>
 *   <li>{@code PasswordRecoveryController} - Manages password reset workflows including recovery token
 *       generation, email delivery, and password change operations</li>
 * </ul>
 * 
 * <h2>User Lifecycle Operations</h2>
 * <p>Controllers in this package support the following user lifecycle stages:</p>
 * <ol>
 *   <li><b>Creation</b> - New user account registration with role assignment and organization association</li>
 *   <li><b>Email Verification</b> - Email confirmation workflows to activate user accounts</li>
 *   <li><b>Profile Management</b> - User profile updates including personal information and preferences</li>
 *   <li><b>Password Management</b> - Password changes, recovery workflows, and security operations</li>
 *   <li><b>Deactivation/Deletion</b> - Account suspension and permanent removal operations</li>
 * </ol>
 * 
 * <h2>Relationships with Other Modules</h2>
 * <p>User controllers integrate with multiple system components:</p>
 * <ul>
 *   <li><b>Services</b> - Delegates business logic to {@code UserService} for user operations and
 *       {@code EmailService} for password recovery notifications</li>
 *   <li><b>Model</b> - Operates on {@code User} entities and {@code UserRole} associations for
 *       role-based access control</li>
 *   <li><b>Repositories</b> - Accesses user data through {@code UserRepository} and related secure
 *       repository wrappers with privilege enforcement</li>
 *   <li><b>Core Security</b> - Integrates with authentication and authorization mechanisms for
 *       user impersonation and privilege validation</li>
 * </ul>
 * 
 * <h2>Typical User Operations</h2>
 * <p>Common user management workflows include:</p>
 * <ul>
 *   <li><b>Listing Users</b> - Display paginated user lists with filtering and search capabilities</li>
 *   <li><b>Creating Accounts</b> - Register new users with email validation and initial role assignment</li>
 *   <li><b>Changing Passwords</b> - Self-service password changes and administrator-initiated resets</li>
 *   <li><b>User Impersonation</b> - Administrator capability to assume user identity for support purposes</li>
 *   <li><b>Profile Updates</b> - Modify user details, contact information, and preferences</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Flow pipeline pattern for user creation
 * return Flow.init()
 *     .thenSet(user, a -> userService.createUser(userForm))
 *     .then(a -> services.email.sendVerificationEmail(a.result(user)));
 * }</pre>
 * 
 * <h2>Package Guidelines</h2>
 * <p><b>Should I put a class into this package?</b></p>
 * <p>Place a controller in this package if it:</p>
 * <ul>
 *   <li>Implements user account management functionality (creation, updates, deletion)</li>
 *   <li>Handles user authentication workflows (password recovery, email verification)</li>
 *   <li>Manages user-related administrative operations (impersonation, role assignment)</li>
 *   <li>Provides user profile or preference management features</li>
 * </ul>
 * 
 * @since 1.7.1
 * @see com.openkoda.service.user.UserService
 * @see com.openkoda.model.User
 * @see com.openkoda.model.UserRole
 */
package com.openkoda.controller.user;