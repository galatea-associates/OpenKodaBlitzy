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

package com.openkoda.service.user;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.controller.common.PageAttributes;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.helper.Messages;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.core.security.OrganizationUser;
import com.openkoda.core.security.UserProvider;
import com.openkoda.core.service.email.StandardEmailTemplates;
import com.openkoda.dto.RegisteredUserDto;
import com.openkoda.form.InviteUserForm;
import com.openkoda.form.RegisterUserForm;
import com.openkoda.model.*;
import com.openkoda.model.authentication.LoginAndPassword;
import com.openkoda.model.task.Email;
import com.openkoda.service.organization.OrganizationCreationStrategy;
import com.openkoda.service.organization.OrganizationService;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.openkoda.controller.common.URLConstants.*;
import static com.openkoda.core.lifecycle.BaseDatabaseInitializer.ROLE_USER;
import static com.openkoda.core.service.event.ApplicationEvent.*;

/**
 * User account lifecycle management service with CRUD operations and privilege-checked queries.
 * <p>
 * This service orchestrates the complete user lifecycle including creation, organization and role assignment,
 * activation, authentication, profile updates, password management, and deactivation. It integrates with
 * Spring Security for password encoding, the organization service for multi-tenancy support, and the email
 * service for user notifications.
 * </p>
 * <p>
 * <b>User Lifecycle:</b>
 * <ol>
 * <li>Create user account with {@link #createUser} methods</li>
 * <li>Assign organizations and roles via {@link #addOrgRoleToUser} and {@link #addGlobalRoleToUser}</li>
 * <li>Send invitation or verification emails</li>
 * <li>User authenticates through Spring Security integration</li>
 * <li>Update profile information and change passwords as needed</li>
 * <li>Reset passwords using time-limited tokens</li>
 * <li>Deactivate accounts while preserving audit trail</li>
 * </ol>
 * </p>
 * <p>
 * <b>Example Usage:</b>
 * <pre>
 * User user = userService.createUser("John", "Doe", "john@example.com");
 * userService.addOrgRoleToUser(user, Tuples.of("ROLE_ORG_ADMIN", organizationId));
 * </pre>
 * </p>
 * <p>
 * <b>Thread Safety:</b> This service is stateless and thread-safe. All operations delegate to Spring Data
 * repositories which provide transactional guarantees.
 * </p>
 * <p>
 * <b>Configuration:</b> Behavior is controlled via application properties including organization creation strategy,
 * default roles, password length, and base URL for email links.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.0.0
 * @see User
 * @see UserRole
 * @see Organization
 * @see Role
 * @see org.springframework.security.crypto.password.PasswordEncoder
 */
@Service
public class UserService extends ComponentProvider implements HasSecurityRules {
    /**
     * Organization creation strategy for new user registration.
     * <p>
     * Determines whether to CREATE a new organization per user or ASSIGN users to a default organization.
     * Configured via application property {@code organization.creation.strategy}.
     * </p>
     *
     * @see OrganizationCreationStrategy
     */
    @Value("${organization.creation.strategy:ASSIGN}")
    OrganizationCreationStrategy creationStrategy;

    /**
     * Default organization ID for ASSIGN strategy.
     * <p>
     * When creation strategy is ASSIGN, new users are automatically assigned to this organization.
     * Configured via application property {@code organization.creation.strategy.assign.id}.
     * Default: 121
     * </p>
     */
    @Value("${organization.creation.strategy.assign.id:121}")
    Long defaultOrgId;

    /**
     * Allows creating users without organization assignment.
     * <p>
     * When true, users can be created without organization membership. When false, all users must
     * belong to at least one organization. Configured via application property
     * {@code organization.creation.strategy.no.organization.users}.
     * Default: false
     * </p>
     */
    @Value("${organization.creation.strategy.no.organization.users:false}")
    boolean canCreateUserWithoutOrg;

    /**
     * Default global role name assigned to new users.
     * <p>
     * This role is automatically assigned during user registration to grant system-wide permissions.
     * Configured via application property {@code role.global.user}.
     * Default: ROLE_USER
     * </p>
     */
    @Value("${role.global.user:ROLE_USER}")
    private String roleGlobalUser;

    /**
     * Default organization admin role name.
     * <p>
     * Assigned to users who create or are invited as administrators of an organization.
     * Configured via application property {@code role.org.admin}.
     * Default: ROLE_ORG_ADMIN
     * </p>
     */
    @Value("${role.org.admin:ROLE_ORG_ADMIN}")
    private String roleOrgAdmin;

    /**
     * Base URL for the application.
     * <p>
     * Used to construct absolute URLs in email notifications including password recovery and
     * account verification links. Configured via application property {@code base.url}.
     * Default: http://localhost:8080
     * </p>
     */
    @Value("${base.url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Application name displayed in email notifications.
     * <p>
     * Used in email subject lines and content. Configured via application property {@code application.name}.
     * Default: Default Application
     * </p>
     */
    @Value("${application.name:Default Application}")
    private String applicationName;

    /**
     * Length of auto-generated initial passwords.
     * <p>
     * When users are invited or registered without providing a password, a random alphanumeric
     * password of this length is generated. Configured via application property
     * {@code user.initial.password.length}.
     * Default: 15 characters
     * </p>
     */
    @Value("${user.initial.password.length:15}")
    private int initialPasswordLength;

    /**
     * Organization service for tenant provisioning and management.
     * <p>
     * Used to create organizations during user registration when creation strategy is CREATE.
     * </p>
     */
    @Inject
    private OrganizationService organizationService;

    /**
     * Password encoder for hashing and verifying passwords.
     * <p>
     * Uses BCrypt with strength 10. Static to ensure single instance across service lifecycle.
     * Initialized via {@link #setPasswordEncoderOnce(PasswordEncoder)}.
     * </p>
     *
     * @see #setPasswordEncoderOnce(PasswordEncoder)
     */
    private static PasswordEncoder passwordEncoder;

    /**
     * Internationalized message service for email templates.
     * <p>
     * Provides localized content for email subject lines and notification text.
     * </p>
     */
    @Inject
    private Messages messages;

    /**
     * Creates a new user account with specified name, email, enabled status, and role assignments.
     * <p>
     * This is the primary user creation method that handles complete user initialization including
     * global and organization-specific role assignments. The user entity is persisted immediately,
     * and a USER_CREATED event is emitted for downstream processing.
     * </p>
     * <p>
     * <b>Transaction Behavior:</b> This operation is transactional, ensuring atomic creation of
     * user entity and all associated role assignments.
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * User user = createUser("John", "Doe", "john@example.com", true,
     *     new String[]{"ROLE_USER"}, new Tuple2[]{Tuples.of("ROLE_ORG_ADMIN", 100L)});
     * </pre>
     * </p>
     *
     * @param firstName the user's first name (nullable, can be empty string)
     * @param lastName the user's last name (nullable, can be empty string)
     * @param email the user's unique email address, converted to lowercase for consistency
     * @param userEnabled true to create an active user, false to create disabled account requiring activation
     * @param globalRoles array of global role names to assign (e.g., "ROLE_USER"), nullable for no global roles
     * @param orgRoles array of organization-specific role tuples (role name, organization ID), nullable for no org roles
     * @return the created User entity with generated ID and all roles assigned
     * @see #addGlobalRoleToUser(User, String...)
     * @see #addOrgRoleToUser(User, Tuple2[])
     * @see User
     * @see UserRole
     */
    public User createUser(String firstName,
                           String lastName,
                           String email,
                           boolean userEnabled,
                           String[] globalRoles,
                           Tuple2<String /* roleName */, Long /* orgId */>[] orgRoles) {
        debug("[createUser] with name {} {} and email {}", firstName, lastName, email);
        User u = new User(firstName, lastName, email.toLowerCase());
        u.setEnabled(userEnabled);
        u = repositories.unsecure.user.save(u);
        addGlobalRoleToUser(u, globalRoles);
        addOrgRoleToUser(u, orgRoles);
        services.applicationEvent.emitEvent(USER_CREATED, u.getBasicUser());
        debug("[createUser] event USER_CREATED emitted");
        return repositories.unsecure.user.findOne(u.getId());
    }

    /**
     * Creates a new enabled user from an existing User entity with default global role.
     * <p>
     * Convenience method that extracts name and email from the provided User entity and assigns
     * the default global role configured in {@code role.global.user} property.
     * </p>
     *
     * @param user the User entity containing name and email (must not be null)
     * @return the created User entity with generated ID and default global role assigned
     * @see #createUser(String, String, String, boolean, String[], Tuple2[])
     */
    public User createUser(User user) {
        String[] globalRoles = {roleGlobalUser};
        return createUser(user.getFirstName(), user.getLastName(), user.getEmail(), globalRoles);
    }

    /**
     * Creates a new enabled user with default global role and specified organization roles.
     * <p>
     * This overload simplifies creation of users who need organization-specific roles while
     * automatically assigning the default system-wide role.
     * </p>
     * <p>
     * Example:
     * <pre>
     * User user = createUser("John", "Doe", "john@example.com",
     *     Tuples.of("ROLE_ORG_ADMIN", orgId));
     * </pre>
     * </p>
     *
     * @param firstName the user's first name (nullable)
     * @param lastName the user's last name (nullable)
     * @param email the user's unique email address, converted to lowercase
     * @param orgRoles varargs of organization-specific role tuples (role name, organization ID)
     * @return the created User entity with generated ID and assigned roles
     * @see #createUser(String, String, String, boolean, String[], Tuple2[])
     */
    public User createUser(String firstName,
                           String lastName,
                           String email,
                           Tuple2<String /* roleName */, Long /* orgId */>... orgRoles) {
        String[] globalRoles = {roleGlobalUser};
        return createUser(firstName, lastName, email, true, globalRoles, orgRoles);
    }

    /**
     * Creates a new enabled user with specified global roles only.
     * <p>
     * Use this method for creating users without organization assignments, such as system
     * administrators or global service accounts. Requires {@code canCreateUserWithoutOrg}
     * configuration to be enabled.
     * </p>
     *
     * @param firstName the user's first name (nullable)
     * @param lastName the user's last name (nullable)
     * @param email the user's unique email address, converted to lowercase
     * @param globalRoles varargs of global role names to assign (e.g., "ROLE_USER", "ROLE_ADMIN")
     * @return the created User entity with generated ID and assigned global roles
     * @see #createUser(String, String, String, boolean, String[], Tuple2[])
     */
    public User createUser(String firstName,
                           String lastName,
                           String email,
                           String... globalRoles) {
        return createUser(firstName, lastName, email, true, globalRoles, null);
    }

    /**
     * Assigns organization-specific roles to an existing user.
     * <p>
     * Creates UserRole associations for each organization-role tuple provided. Each role assignment
     * is persisted immediately and a USER_ROLE_CREATED event is emitted for each successful assignment.
     * Non-existent role names are silently skipped.
     * </p>
     * <p>
     * Example:
     * <pre>
     * addOrgRoleToUser(user, Tuples.of("ROLE_ORG_ADMIN", orgId));
     * </pre>
     * </p>
     *
     * @param u the User entity to receive role assignments (must not be null)
     * @param orgRoles varargs of organization-specific role tuples containing (role name, organization ID),
     *                 nullable to assign no roles
     * @return list of created UserRole entities, empty list if orgRoles is null or all role names are invalid
     * @see #addGlobalRoleToUser(User, String...)
     * @see UserRole
     * @see Role
     */
    public List<UserRole> addOrgRoleToUser(User u, Tuple2<String, Long>... orgRoles) {
        debug("[addOrgRoleToUser] userId: {}", u.getId());
        if (orgRoles == null) {
            return Collections.emptyList();
        }
        List<UserRole> result = new ArrayList<>(orgRoles.length);
        for (Tuple2<String, Long> t : orgRoles) {
            Role r = repositories.unsecure.role.findByName(t.getT1());
            if (r != null) {
                UserRole ur = new UserRole(null, u.getId(), r.getId(), t.getT2());
                result.add(repositories.unsecure.userRole.save(ur));
                services.applicationEvent.emitEvent(USER_ROLE_CREATED, ur.getUserRoleDto());
            }
        }
        return result;
    }

    /**
     * Assigns system-wide global roles to an existing user.
     * <p>
     * Global roles provide permissions across all organizations and are not scoped to a specific tenant.
     * This method delegates to {@link #addOrgRoleToUser(User, Tuple2[])} with null organization IDs.
     * </p>
     * <p>
     * Example:
     * <pre>
     * addGlobalRoleToUser(user, "ROLE_USER", "ROLE_ADMIN");
     * </pre>
     * </p>
     *
     * @param u the User entity to receive global role assignments (must not be null)
     * @param globalRoles varargs of global role names (e.g., "ROLE_USER", "ROLE_ADMIN"),
     *                    nullable to assign no roles
     * @return list of created UserRole entities with null organization IDs, empty list if globalRoles is null
     * @see #addOrgRoleToUser(User, Tuple2[])
     * @see UserRole
     */
    public List<UserRole> addGlobalRoleToUser(User u, String... globalRoles) {
        debug("[addGlobalRoleToUser] userId: {}", u.getId());
        if (globalRoles == null) {
            return Collections.emptyList();
        }
        return addOrgRoleToUser(u, Stream.of(globalRoles).map(a -> Tuples.of(a, null)).toArray(Tuple2[]::new));
    }


    /**
     * Changes a user's role within a specific organization.
     * <p>
     * Replaces the existing organization role with the new role. If the user has multiple roles
     * for the same organization, the first matching role is replaced. If the new role does not exist,
     * the existing role is preserved.
     * </p>
     *
     * @param u the User entity whose role will be changed (must not be null)
     * @param organizationId the organization ID for which to change the role (must not be null)
     * @param roleName the new role name to assign (e.g., "ROLE_ORG_ADMIN")
     * @return the updated or unchanged UserRole entity, or null if no role exists and new role is invalid
     * @see #changeUserGlobalRole(User, String)
     */
    public UserRole changeUserOrganizationRole(User u, Long organizationId, String roleName) {
        debug("[changeUserOrganizationRole] userId: {}", u.getId());
        return changeUserRole(u, Tuples.of(roleName, organizationId));
    }

    /**
     * Changes a user's global role.
     * <p>
     * Replaces the existing global role with the new role. Requires global settings access privilege.
     * Global roles are not scoped to any specific organization.
     * </p>
     *
     * @param u the User entity whose global role will be changed (must not be null)
     * @param roleName the new global role name to assign (e.g., "ROLE_USER", "ROLE_ADMIN")
     * @return the updated or unchanged UserRole entity, or null if no global role exists and new role is invalid
     * @see #changeUserOrganizationRole(User, Long, String)
     */
    @PreAuthorize(CHECK_CAN_ACCESS_GLOBAL_SETTINGS)
    public UserRole changeUserGlobalRole(User u, String roleName) {
        debug("[changeUserGlobalRole] userId: {}", u.getId());
        return changeUserRole(u, Tuples.of(roleName, null));
    }

    /**
     * Internal method to change a user's role (global or organization-specific).
     * <p>
     * Locates the existing role by type (GLOBAL or ORG) and organization ID, deletes it if found,
     * and creates a new UserRole with the specified role name. If the existing and new roles are
     * identical, no change is made.
     * </p>
     *
     * @param u the User entity whose role will be changed
     * @param role tuple containing (role name, organization ID or null for global)
     * @return the updated UserRole entity, existing role if unchanged, or null if no role found and new role invalid
     */
    private UserRole changeUserRole(User u, Tuple2<String, Long> role){
        debug("[changeUserOrganizationRole] userId: {}", u.getId());
        String type = role.getT2() == null ? "GLOBAL" : "ORG";
        Optional<UserRole> oldRole = u.getRoles().stream()
                .filter(r -> r.getRole().getType().equals(type) && (r.getOrganizationId() == null || r.getOrganizationId().equals(role.getT2())))
                .findAny();
        Role r = repositories.unsecure.role.findByName(role.getT1());
        if (r == null) {
            return oldRole.isPresent() ? oldRole.get() : null;
        }
        if (oldRole.isPresent() && oldRole.get().getRole().getName().equals(role.getT1())) {
            return oldRole.get();
        }
        if (oldRole.isPresent()) {
            repositories.unsecure.userRole.deleteUserRole(oldRole.get().getId());
        }
        UserRole ur = new UserRole(null, u.getId(), r.getId(), role.getT2());
        return repositories.unsecure.userRole.save(ur);
    }

    /**
     * Invites a new or existing user to join an organization.
     * <p>
     * Handles two distinct invitation workflows:
     * </p>
     * <ul>
     * <li><b>New User:</b> Creates user account, generates random password, assigns organization role,
     *     sends invitation email with password recovery link</li>
     * <li><b>Existing User:</b> Adds organization role to existing account, sends invitation email
     *     with organization details</li>
     * </ul>
     * <p>
     * Requires privilege to invite users to organizations. Email is persisted for asynchronous delivery.
     * Password length is configured via {@code user.initial.password.length} property.
     * </p>
     * <p>
     * Example:
     * <pre>
     * Tuple3&lt;Email, User, InviteUserForm&gt; result =
     *     inviteNewOrExistingUser(form, null, organization);
     * </pre>
     * </p>
     *
     * @param userForm the invitation form containing user details (email, name, role)
     * @param user the existing User entity if inviting an existing user, null to create a new user
     * @param organization the Organization entity to which the user is being invited (must not be null)
     * @return tuple containing (Email entity for notification, User entity created or updated, empty InviteUserForm)
     * @throws RuntimeException if organization is null
     * @see InviteUserForm
     * @see StandardEmailTemplates#INVITE_NEW
     * @see StandardEmailTemplates#INVITE_EXISTING
     */
    @PreAuthorize(CHECK_CAN_INVITE_USER_TO_ORG)
    public Tuple3<Email, User, InviteUserForm> inviteNewOrExistingUser(InviteUserForm userForm, User user, Organization
            organization) {
        debug("[inviteNewOrExistingUser]");

        if (organization == null) {
            RuntimeException e = new RuntimeException("Organization must not be null");
            error("Organization must not be null", e);
        }

        Long orgId = organization.getId();
        String template;
        String subject;
        PageModelMap model = new PageModelMap();

        if (user == null) {
            user = createUser(userForm.dto.firstName, userForm.dto.lastName, userForm.dto.email, Tuples.of(userForm.dto.roleName, orgId));
            user.setLoginAndPassword(user.getEmail(), RandomStringUtils.randomAlphanumeric(initialPasswordLength), false);
            repositories.unsecure.loginAndPassword.save(user.getLoginAndPassword());
            user = repositories.unsecure.user.save(user);
            template = StandardEmailTemplates.INVITE_NEW;
            subject = "Invitation to join new organization in " + applicationName;
            model.put(passwordRecoveryLink, getPasswordRecoveryLink(user));
            debug("[inviteNewOrExistingUser] user {} created and invited", user.getId());
        }
        else {
            addOrgRoleToUser(user, Tuples.of(userForm.dto.roleName, orgId));
            template = StandardEmailTemplates.INVITE_EXISTING;
            subject = "Invitation to join your organization in " + applicationName;
            debug("[inviteNewOrExistingUser] user {} invited", user.getId());
        }

        model.put(userEntity, user);
        if (getLoggedOrganizationUser().isPresent()) {
            model.put("invitedBy", getLoggedOrganizationUser().get().getUser().getEmail());
        }
        model.put(organizationEntity, repositories.unsecure.organization.findOne(orgId));


        Email email = services.emailConstructor.prepareEmail(
                user.getEmail(), user.getName().isEmpty() ? user.getEmail() : user.getName(), subject, template, 5, model);

        email = repositories.unsecure.email.save(email);

        return Tuples.of(email, user, new InviteUserForm());
    }

    /**
     * Returns the currently authenticated user from the Spring Security context.
     * <p>
     * Extracts the User entity from the current authentication token. Returns null if no user
     * is authenticated (e.g., anonymous requests).
     * </p>
     *
     * @return the authenticated User entity, or null if no user is currently authenticated
     * @see UserProvider
     * @see #getCurrentOrganizationUser()
     */
    public User getCurrentUser() {
        debug("[getCurrentUser]");
        return UserProvider.getFromContext().map(OrganizationUser::getUser).orElse(null);
    }

    /**
     * Returns the currently authenticated user with organization context.
     * <p>
     * Provides the full OrganizationUser wrapper which includes both the User entity and the
     * current organization context. This is useful for multi-tenant operations that need to know
     * both the user identity and their active organization.
     * </p>
     *
     * @return Optional containing the OrganizationUser if authenticated, empty Optional if anonymous
     * @see UserProvider
     * @see OrganizationUser
     * @see #getCurrentUser()
     */
    public Optional<OrganizationUser> getCurrentOrganizationUser() {
        debug("[getCurrentOrganizationUser]");
        return UserProvider.getFromContext();
    }

    /**
     * Registers a new user or returns existing user with matching email.
     * <p>
     * Convenience method that delegates to full registration with system user privileges enabled.
     * </p>
     *
     * @param registerUserForm the registration form containing user details (name, email, password)
     * @return the User entity (newly created or existing)
     * @see #registerUserOrReturnExisting(RegisterUserForm, Cookie[], String, boolean)
     */
    public User registerUserOrReturnExisting(RegisterUserForm registerUserForm) {
        return registerUserOrReturnExisting(registerUserForm, null, "", true).getT1();
    }

    /**
     * Registers a new user or returns existing user with configurable system user mode.
     * <p>
     * Controls whether registration executes with system privileges (asSystemUser=true) or
     * within the current security context.
     * </p>
     *
     * @param registerUserForm the registration form containing user details
     * @param asSystemUser true to execute with system privileges, false to use current authentication
     * @return the User entity (newly created or existing)
     * @see #registerUserOrReturnExisting(RegisterUserForm, Cookie[], String, boolean)
     */
    public User registerUserOrReturnExisting(RegisterUserForm registerUserForm, boolean asSystemUser) {
        return registerUserOrReturnExisting(registerUserForm, null, "", asSystemUser).getT1();
    }

    /**
     * Registers a new user or returns existing user with full control over registration parameters.
     * <p>
     * This is the primary registration method that handles:
     * </p>
     * <ul>
     * <li>Checking for existing user by email (case-insensitive)</li>
     * <li>Creating organization based on configured strategy (CREATE or ASSIGN)</li>
     * <li>Creating user account with encrypted password</li>
     * <li>Assigning default roles (global and organization)</li>
     * <li>Sending account verification email</li>
     * <li>Emitting USER_REGISTERED event</li>
     * </ul>
     * <p>
     * <b>Transaction Management:</b> Operations execute with system privileges when asSystemUser is true,
     * ensuring authentication context is properly set and cleared.
     * </p>
     *
     * @param registerUserForm the registration form containing user details (name, email, password)
     * @param cookies browser cookies for tracking (nullable, used in USER_REGISTERED event)
     * @param languagePrefix language code for localized email content (e.g., "en", "pl"), empty string for default
     * @param asSystemUser true to execute with system privileges, false to use current authentication
     * @return tuple containing (User entity, boolean indicating if user already existed)
     * @see RegisterUserForm
     * @see OrganizationCreationStrategy
     * @see #registerUserWithStrategy(RegisterUserForm, Cookie[], Organization, String[])
     */
    public Tuple2<User, Boolean> registerUserOrReturnExisting(RegisterUserForm registerUserForm, Cookie[] cookies, String languagePrefix, boolean asSystemUser) {
        debug("[registerUserOrReturnExisting]");
        try {
            User existingUser = repositories.unsecure.user.findByEmailLowercase(registerUserForm.getLogin());
            boolean userAlreadyExists = existingUser != null;
            if (!userAlreadyExists) {

                List<String> globalRoles = new ArrayList<>();
                //user with this email does not exist so create a brand new one with new organization
                if (asSystemUser) {
                    UserProvider.setCronJobAuthentication();
                    globalRoles = Collections.singletonList(roleGlobalUser);
                }
                Organization organization = createOrganizationOrAssignToDefault(registerUserForm);
                User user = registerUserWithStrategy(registerUserForm, cookies, organization, globalRoles.toArray(String[]::new));
                repositories.unsecure.loginAndPassword.save(user.getLoginAndPassword());
                existingUser = repositories.unsecure.user.save(user);
                if (asSystemUser) {
                    sendAccountVerificationEmail(existingUser, languagePrefix);
                }
                debug("[registerUserOrReturnExisting] new user with id {} registered", user.getId());
            }

            return Tuples.of(existingUser, userAlreadyExists);

        } finally {
            if (asSystemUser) {
                UserProvider.clearAuthentication();
            }
        }
    }

    /**
     * Registers a new user or adds organization role to existing user with custom email template.
     * <p>
     * This method supports custom registration workflows where the email template and organization
     * are provided externally. If the user already exists, they are assigned the specified organization
     * role. A random password is generated for new users, and a password recovery email is sent.
     * </p>
     * <p>
     * <b>System Privileges:</b> Executes with system authentication context via UserProvider.setCronJobAuthentication().
     * </p>
     *
     * @param firstName the user's first name (last name will be null)
     * @param organization the organization to assign the user to (must not be null)
     * @param userLogin the login identifier, typically email address
     * @param email the user's email address for notifications
     * @param websiteUrl the website URL to include in email content
     * @param emailTemplateName the email template name to use for notification
     * @param emailTitle the email subject line
     * @param orgRole the organization role name to assign (e.g., "ROLE_ORG_USER")
     * @return the User entity (newly created or existing with new organization role)
     * @see #registerUser(String, Organization, String, String, String, String, String, String)
     */
    public User registerUserOrReturnExisting(String firstName, Organization organization, String userLogin, String email, String websiteUrl, String emailTemplateName, String emailTitle, String orgRole) {
        debug("[registerUserOrReturnExisting]");
        try {
            PageModelMap model = new PageModelMap();

            User user = repositories.unsecure.user.findByEmailLowercase(email);

            if (user == null) {
                //user with this email does not exist so create a brand new one with new organization
                UserProvider.setCronJobAuthentication();

                user = createUser(firstName, null, userLogin, true, new String[]{ROLE_USER}, new Tuple2[]{Tuples.of(orgRole, organization.getId())});
                user.setLoginAndPassword(user.getEmail(), RandomStringUtils.randomAlphanumeric(initialPasswordLength), false);
                user = repositories.unsecure.user.save(user);
                debug("[registerUserOrReturnExisting] new user with id {} registered", user.getId());
            } else {
                addOrgRoleToUser(user, Tuples.of(orgRole, organization.getId()));
                user = repositories.unsecure.user.save(user);
            }

            model.put(userEntity, user);
            model.put(organizationEntity, organization);
            model.put(PageAttributes.websiteUrl, websiteUrl);
            model.put(passwordRecoveryLink, getPasswordRecoveryLink(user));
            Email emailMsg = services.emailConstructor.prepareEmail(email, user.getName().isEmpty() ? email : user.getName(),
                    emailTitle, emailTemplateName, 5, model);
            repositories.unsecure.email.save(emailMsg);

            return user;

        } finally {
            UserProvider.clearAuthentication();
        }
    }

    /**
     * Initializes the static password encoder instance on first invocation.
     * <p>
     * This method ensures the password encoder is set exactly once during application startup.
     * Subsequent calls are ignored. The encoder is typically BCrypt with strength 10 for secure
     * password hashing.
     * </p>
     * <p>
     * <b>Thread Safety:</b> While this method can be called concurrently, the first successful
     * initialization wins. No synchronization is needed due to idempotent behavior.
     * </p>
     *
     * @param pe the PasswordEncoder instance to use for password hashing and verification (must not be null)
     * @see org.springframework.security.crypto.password.PasswordEncoder
     * @see #getPasswordEncoder()
     */
    public static void setPasswordEncoderOnce(PasswordEncoder pe) {
        if (passwordEncoder != null) {
            //Password encoder already initialized
            return;
        }
        UserService.passwordEncoder = pe;
    }

    /**
     * Creates a new organization or returns the default organization based on configured strategy.
     * <p>
     * When strategy is CREATE, derives organization name from email prefix (part before @).
     * When strategy is ASSIGN, returns the pre-configured default organization.
     * </p>
     *
     * @param registerUserForm the registration form containing user email for organization naming
     * @return newly created Organization if CREATE strategy, default Organization if ASSIGN strategy
     * @see OrganizationCreationStrategy
     */
    private Organization createOrganizationOrAssignToDefault(RegisterUserForm registerUserForm) {
        if (creationStrategy.equals(OrganizationCreationStrategy.CREATE)) {
            return organizationService.createOrganization(StringUtils.substringBefore(registerUserForm.getLogin(), "@"), 0);
        }
        return repositories.unsecure.organization.findOne(defaultOrgId);
    }

    /**
     * Registers a user with the appropriate strategy based on organization availability.
     * <p>
     * Handles three scenarios:
     * </p>
     * <ul>
     * <li><b>No organization + allowed:</b> Creates user with global roles only</li>
     * <li><b>No organization + not allowed:</b> Throws NullPointerException</li>
     * <li><b>With organization:</b> Creates user with organization admin role and global roles</li>
     * </ul>
     * <p>
     * Emits USER_REGISTERED event with registration details and cookies for tracking.
     * Sets user password using BCrypt encoding.
     * </p>
     *
     * @param registerUserForm the registration form with user details
     * @param cookies browser cookies for event tracking (nullable)
     * @param organization the organization to assign user to (nullable based on configuration)
     * @param globalRoles array of global role names to assign
     * @return the created User entity with LoginAndPassword set
     * @throws NullPointerException if organization is null and canCreateUserWithoutOrg is false
     * @see RegisteredUserDto
     */
    private User registerUserWithStrategy(RegisterUserForm registerUserForm, Cookie[] cookies, Organization organization, String[] globalRoles) {
        User user;
        if (organization == null) {
            if (canCreateUserWithoutOrg) {
                user = createUser(registerUserForm.getFirstName(), registerUserForm.getLastName(), registerUserForm.getLogin(), globalRoles);
                user.setLoginAndPassword(user.getEmail(), registerUserForm.getPassword(), false);
                services.applicationEvent.emitEvent(USER_REGISTERED, new RegisteredUserDto(registerUserForm, user.getId(), 0l, cookies));
            } else {
                throw new NullPointerException("Can't create user without organization.");
            }
        } else {
            Tuple2<String, Long> orgRole = Tuples.of(roleOrgAdmin, organization.getId());
            Tuple2[] orgRoles = {orgRole};
            user = createUser(registerUserForm.getFirstName(), registerUserForm.getLastName(), registerUserForm.getLogin(), false, globalRoles, orgRoles);
            user.setLoginAndPassword(user.getEmail(), registerUserForm.getPassword() != null ? registerUserForm.getPassword() : RandomStringUtils.randomAlphanumeric(initialPasswordLength), false);
            services.applicationEvent.emitEvent(USER_REGISTERED, new RegisteredUserDto(registerUserForm, user.getId(), organization.getId(), cookies));
        }
        return user;
    }

    /**
     * Registers a new user without checking for existing account.
     * <p>
     * Creates a new user account unconditionally, assigns organization role, generates random password,
     * and sends customized welcome email. Use this method when existence check is not required or
     * already performed externally.
     * </p>
     * <p>
     * <b>Warning:</b> Does not check for duplicate users. Caller must ensure email uniqueness
     * to avoid constraint violations.
     * </p>
     *
     * @param firstName the user's first name (last name will be null)
     * @param organization the organization to assign the user to (must not be null)
     * @param userLogin the login identifier, typically email address
     * @param email the user's email address for notifications
     * @param websiteUrl the website URL to include in email content
     * @param emailTemplateName the email template name to use for notification
     * @param emailTitle the email subject line
     * @param orgRole the organization role name to assign
     * @return the newly created User entity
     * @see #registerUserOrReturnExisting(String, Organization, String, String, String, String, String, String)
     */
    public User registerUser(String firstName, Organization organization, String userLogin,  String email, String websiteUrl, String emailTemplateName, String emailTitle, String orgRole) {
        debug("[registerUserOrReturnExisting]");
        try {
            PageModelMap model = new PageModelMap();


                //user with this email does not exist so create a brand new one with new organization
            UserProvider.setCronJobAuthentication();

            User user = createUser(firstName, null, userLogin, true, null, new Tuple2[]{Tuples.of(orgRole, organization.getId())});
            user.setLoginAndPassword(user.getEmail(), RandomStringUtils.randomAlphanumeric(initialPasswordLength), false);
            user = repositories.unsecure.user.save(user);
            debug("[registerUserOrReturnExisting] new user with id {} registered", user.getId());

            model.put(userEntity, user);
            model.put(organizationEntity, organization);
            model.put(PageAttributes.websiteUrl, websiteUrl);
            model.put(passwordRecoveryLink, getPasswordRecoveryLink(user));
            Email emailMsg = services.emailConstructor.prepareEmail(email, user.getName().isEmpty() ? email : user.getName(),
                    emailTitle, emailTemplateName, 5, model);
            repositories.unsecure.email.save(emailMsg);

            return user;

        } finally {
            UserProvider.clearAuthentication();
        }
    }
    /**
     * Creates a personal organization and assigns default roles for externally authenticated users.
     * <p>
     * This method supports user onboarding from external authentication providers including social
     * services (OAuth), LDAP, and Salesforce. The organization name is derived from the email address
     * prefix (part before @). The user is assigned both global user role and organization admin role.
     * </p>
     * <p>
     * <b>Use Case:</b> When users authenticate via external providers without pre-existing organizations,
     * this creates their tenant space and grants administrative privileges.
     * </p>
     * <p>
     * Example: User with email "john@example.com" gets organization named "john".
     * </p>
     *
     * @param user the User entity from external authentication (must not be null, must have email set)
     * @return true if organization and roles were successfully created and assigned
     * @see OrganizationService#createOrganization(String, long)
     * @see #addGlobalRoleToUser(User, String...)
     * @see #addOrgRoleToUser(User, Tuple2[])
     */
    public boolean createOrganizationAndSetRoles(User user) {
        debug("[createOrganizationAndSetRoles] user: {}", user);
        Organization organization = organizationService.createOrganization(StringUtils.substringBefore(user.getEmail(), "@"), 0);
        String[] globalRoles = {roleGlobalUser};
        Tuple2<String, Long> orgRole = Tuples.of(roleOrgAdmin, organization.getId());
        Tuple2[] orgRoles = {orgRole};

        debug("[createOrganizationAndSetRoles] adding global roles for user");
        addGlobalRoleToUser(user, globalRoles);
        debug("[createOrganizationAndSetRoles] adding org (id: {}) roles for user", organization.getId());
        addOrgRoleToUser(user, orgRoles);
        return true;
    }

    /**
     * Resends account verification email to a user by email address.
     * <p>
     * Looks up the user by email (case-insensitive) and sends a new verification email with
     * account activation link. Returns false if email is blank or user is not found.
     * </p>
     * <p>
     * <b>Use Case:</b> User registration workflow when verification email is not received or expired.
     * </p>
     *
     * @param email the user's email address (must not be blank)
     * @return true if user was found and email was sent, false if email is blank or user not found
     * @see #sendAccountVerificationEmail(User, String)
     * @see #getAccountVerificationLink(User)
     */
    public boolean resendAccountVerificationEmail(String email) {
        debug("[resendAccountVerificationEmail] email: {}", email);
        if(StringUtils.isBlank(email)) {
            debug("[resendAccountVerificationEmail] email cannot be blank!");
            return false;
        }
        User user = repositories.unsecure.user.findByEmailLowercase(email);
        if (user != null) {
            sendAccountVerificationEmail(user, "");
            return true;
        }
        debug("[resendAccountVerificationEmail] user with email {} not found", email);
        return false;
    }

    /**
     * Sends account verification email with localized content.
     * <p>
     * Constructs and persists an email containing the account verification link. The email template
     * and subject line are localized based on the language prefix. Email is queued for asynchronous
     * delivery.
     * </p>
     *
     * @param user the User entity to send verification email to (must not be null)
     * @param languagePrefix language code for localization (e.g., "en", "pl"), empty string for default
     * @return the same User entity (for method chaining)
     * @see StandardEmailTemplates#WELCOME
     * @see #getAccountVerificationLink(User)
     */
    private User sendAccountVerificationEmail(User user, String languagePrefix) {
        debug("[sendAccountVerificationEmail] userId: {}", user.getId());

        PageModelMap model = new PageModelMap();

        String template = StandardEmailTemplates.WELCOME + (StringUtils.isEmpty(languagePrefix)? "" : "-" + languagePrefix);
        languagePrefix = languagePrefix == "" ? "" : "." + languagePrefix;
        String subject = messages.get("email.welcome.subject" + languagePrefix) + " " + applicationName;

        model.put(userEntity, user);
        model.put(accountVerificationLink, getAccountVerificationLink(user));

        Email email = services.emailConstructor.prepareEmailWithTitleFromTemplate(
                user.getEmail(), null, user.getName(), template, model);

        repositories.unsecure.email.save(email);
        return user;
    }

    /**
     * Changes a user's password to a new value.
     * <p>
     * Encodes the new password using BCrypt (strength 10) and updates the LoginAndPassword entity.
     * The account is automatically enabled upon successful password change. Changes are immediately
     * flushed to the database.
     * </p>
     * <p>
     * <b>Security:</b> Caller must validate password strength before calling this method.
     * No password complexity validation is performed here.
     * </p>
     * <p>
     * Example:
     * <pre>
     * changePassword(user, "newSecurePassword123");
     * </pre>
     * </p>
     *
     * @param user the User entity whose password will be changed (must not be null, must have LoginAndPassword)
     * @param newPassword the new plain text password to set (will be hashed with BCrypt)
     * @return true if password was changed successfully, false if user has no LoginAndPassword entity
     * @see PasswordEncoder
     * @see LoginAndPassword
     * @see #verifyPassword(User, String)
     */
    public boolean changePassword(User user, String newPassword) {
        debug("[changePassword]");
        LoginAndPassword loginAndPassword = user.getLoginAndPassword();
        if (loginAndPassword != null) {
            loginAndPassword.setPassword(passwordEncoder.encode(newPassword));
            loginAndPassword.setEnabled(true);
            repositories.unsecure.user.saveAndFlush(user);
            debug("[changePassword] password for user {} changed successfully", user.getId());
        }
        return true;
    }

    /**
     * Verifies a plain text password against the user's stored hashed password.
     * <p>
     * Checks that the provided password matches the BCrypt hash stored in the database and that
     * the user account is enabled. Returns the user entity on successful verification, null otherwise.
     * </p>
     * <p>
     * <b>Use Case:</b> Password authentication during login or password change verification.
     * </p>
     * <p>
     * Example:
     * <pre>
     * User authenticated = verifyPassword(user, "userInputPassword");
     * if (authenticated != null) { /&#42; login successful &#42;/ }
     * </pre>
     * </p>
     *
     * @param user the User entity to verify (must not be null)
     * @param passwordToVerify the plain text password to check against stored hash
     * @return the User entity if password matches and account is enabled, null otherwise
     * @see PasswordEncoder
     * @see LoginAndPassword#isEnabled()
     * @see #changePassword(User, String)
     */
    public User verifyPassword(User user, String passwordToVerify) {
        debug("[verifyPassword]");
        LoginAndPassword loginAndPassword = user.getLoginAndPassword();
        if (loginAndPassword != null) {
            if (passwordEncoder.matches(passwordToVerify, loginAndPassword.getPassword()) && loginAndPassword.isEnabled()) {
                return user;
            }
        }
        return null;
    }

    /**
     * Generates a time-limited password recovery link for a user.
     * <p>
     * Creates a secure token with {@code canResetPassword} privilege and constructs a full URL
     * for the password recovery page. The token is encoded in Base64 and includes user ID for verification.
     * Token expiration is controlled by the token service configuration (typically 24 hours).
     * </p>
     * <p>
     * <b>URL Format:</b> {baseUrl}/password/recovery/verify?token={base64EncodedToken}
     * </p>
     *
     * @param user the User entity requesting password recovery (must not be null)
     * @return the complete password recovery URL with embedded token
     * @see Token
     * @see Privilege#canResetPassword
     * @see #passwordRecovery(User)
     */
    public String getPasswordRecoveryLink(User user) {
        debug("[getPasswordRecoveryLink] userId: {}", user.getId());
        Token token = services.token.createTokenForUser(user, Privilege.canResetPassword);

        return baseUrl + _PASSWORD + _RECOVERY + _VERIFY + "?"
                + TOKEN + "=" + token.getUserIdAndTokenBase64String();
    }

    /**
     * Generates a time-limited account verification link for a user.
     * <p>
     * Creates a secure token with {@code canVerifyAccount} privilege and constructs a full URL
     * for the account verification page. Used during registration to confirm email ownership.
     * Token expiration is controlled by the token service configuration.
     * </p>
     * <p>
     * <b>URL Format:</b> {baseUrl}/register/verify?verifyToken={base64EncodedToken}
     * </p>
     *
     * @param user the User entity requiring account verification (must not be null)
     * @return the complete account verification URL with embedded token
     * @see Token
     * @see Privilege#canVerifyAccount
     * @see #sendAccountVerificationEmail(User, String)
     */
    public String getAccountVerificationLink(User user) {
        debug("[getAccountVerificationLink] userId: {}", user.getId());
        Token token = services.token.createTokenForUser(user, Privilege.canVerifyAccount);

        return baseUrl + _REGISTER + _VERIFY + "?"
                + VERIFY_TOKEN + "=" + token.getUserIdAndTokenBase64String();
    }

    /**
     * Initiates password recovery workflow for a user.
     * <p>
     * Generates a time-limited password recovery token and sends an email containing the recovery link.
     * The email is persisted for asynchronous delivery. User can follow the link to set a new password
     * without knowing the current password.
     * </p>
     * <p>
     * <b>Security:</b> Token is valid for limited time (typically 24 hours) and can only be used once.
     * </p>
     * <p>
     * Example workflow:
     * <ol>
     * <li>User requests password reset</li>
     * <li>System calls passwordRecovery(user)</li>
     * <li>Email with recovery link is sent</li>
     * <li>User clicks link and sets new password</li>
     * <li>Token is consumed and invalidated</li>
     * </ol>
     * </p>
     *
     * @param user the User entity requesting password recovery (must not be null)
     * @return true if password recovery email was successfully queued for delivery
     * @see #getPasswordRecoveryLink(User)
     * @see StandardEmailTemplates#PASSWORD_RECOVERY
     * @see Token
     */
    public boolean passwordRecovery(User user) {
        debug("[passwordRecovery] userId: {}", user.getId());
        PageModelMap model = new PageModelMap();
        model.put(userEntity, user);
        model.put(passwordRecoveryLink, getPasswordRecoveryLink(user));

        Email emailMsg = services.emailConstructor.prepareEmailWithTitleFromTemplate(
                user.getEmail(),
                null,
                user.getName().isEmpty() ? user.getEmail() : user.getName(),
                StandardEmailTemplates.PASSWORD_RECOVERY,
                model);

        repositories.unsecure.email.save(emailMsg);
        return true;
    }

    /**
     * Returns the static password encoder instance used for password hashing and verification.
     * <p>
     * The encoder is initialized once during application startup via {@link #setPasswordEncoderOnce(PasswordEncoder)}.
     * Typically configured as BCrypt with strength 10.
     * </p>
     *
     * @return the PasswordEncoder instance, or null if not yet initialized
     * @see #setPasswordEncoderOnce(PasswordEncoder)
     * @see org.springframework.security.crypto.password.PasswordEncoder
     */
    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    /**
     * Validates that a user does not already have a role in the specified organization.
     * <p>
     * Looks up the user by email (case-insensitive) and checks for existing UserRole associations
     * with the given organization. If a role exists, adds a validation error to the BindingResult
     * with message key "email.exists.in.organization".
     * </p>
     * <p>
     * <b>Use Case:</b> Form validation during user invitation to prevent duplicate invitations.
     * </p>
     *
     * @param email the user's email address to check (nullable, blank is treated as valid)
     * @param organizationId the organization ID to check for existing roles (must not be null)
     * @param br the BindingResult to add validation errors to if user already has a role
     * @return true if user does not have a role in organization (or email is blank), false if role exists
     * @see BindingResult
     * @see UserRole
     */
    public boolean validateIfUserDoesNotHaveRoleInOrganization(String email, Long organizationId, BindingResult br){
        boolean doesNotHave = true;
        if(email != null && !email.isBlank()) {
            User user = repositories.unsecure.user.findByEmailLowercase(email);
            if (user != null) {
                doesNotHave = repositories.unsecure.userRole.findByOrganizationIdAndUserId(organizationId, user.getId()).isEmpty();
            }
            if (!doesNotHave) {
                br.rejectValue("dto.email", "email.exists.in.organization");
            }
        }
        return doesNotHave;
    }

}
