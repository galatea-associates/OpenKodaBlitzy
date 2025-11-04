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
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.*;
import com.openkoda.service.user.BasicPrivilegeService.PrivilegeChangeEvent;
import jakarta.inject.Inject;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Role management service for creating, assigning, and querying roles with privilege synchronization.
 * <p>
 * Manages role definitions and assignments across the OpenKoda platform. Creates new roles with privilege sets,
 * updates existing roles, assigns roles to users, and synchronizes role-privilege relationships. Supports three
 * role types with different scoping rules for multi-tenant environments.

 * <p>
 * Role types and hierarchy:

 * <ul>
 * <li>{@link GlobalRole} - Applies across all organizations (e.g., SYSTEM_ADMIN, GLOBAL_USER)</li>
 * <li>{@link OrganizationRole} - Scoped to a single organization (e.g., ORG_ADMIN, ORG_USER)</li>
 * <li>{@link GlobalOrganizationRole} - Hybrid type for special cases requiring cross-tenant access</li>
 * </ul>
 * <p>
 * Role hierarchy: SYSTEM_ADMIN (global) &gt; ORG_ADMIN (per org) &gt; ORG_USER (per org)

 * <p>
 * Privilege aggregation: A user's effective privileges are the union of all assigned role privileges.
 * Privileges are stored as a serialized string in the database and deserialized using {@code PrivilegeHelper}.

 * <p>
 * Example usage:
 * <pre>
 * Set&lt;PrivilegeBase&gt; privileges = Set.of(Privilege.READ_USERS, Privilege.WRITE_USERS);
 * GlobalRole adminRole = roleService.createOrUpdateGlobalRole("ADMIN", privileges, true);
 * </pre>

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.0.0
 * @see Role
 * @see GlobalRole
 * @see OrganizationRole
 * @see GlobalOrganizationRole
 * @see Privilege
 * @see PrivilegeBase
 * @see UserRole
 */

@Service
public class RoleService extends ComponentProvider implements HasSecurityRules {

    /**
     * Role type constant for organization-scoped roles.
     * <p>
     * Organization roles apply only within a single tenant context and are the most common role type.
     * Used when creating or validating {@link OrganizationRole} instances.

     */
    public static final String ROLE_TYPE_ORG = "ORG";
    
    /**
     * Role type constant for global roles.
     * <p>
     * Global roles apply across all organizations and typically grant system-wide administrative privileges.
     * Used when creating or validating {@link GlobalRole} instances.

     */
    public static final String ROLE_TYPE_GLOBAL = "GLOBAL";
    
    /**
     * Role type constant for global-organization hybrid roles.
     * <p>
     * Global-organization roles provide cross-tenant access for special cases requiring organization context
     * with global reach. Used when creating or validating {@link GlobalOrganizationRole} instances.

     */
    public static final String ROLE_TYPE_GLOBAL_ORG = "GLOBAL_ORG";
    
    /**
     * Spring event publisher for broadcasting role and privilege changes.
     * <p>
     * Used to publish {@link PrivilegeChangeEvent} after role modifications, triggering privilege cache
     * invalidation and security context updates across the application.

     */
    @Inject private ApplicationEventPublisher applicationEventPublisher;
    
    /**
     * Creates a new global role or updates an existing one with the specified privileges.
     * <p>
     * Global roles apply across all organizations and are typically used for system-wide administrative
     * functions. If a role with the given name already exists, its privileges and removability flag are updated.
     * Otherwise, a new {@link GlobalRole} is created. Publishes a {@link PrivilegeChangeEvent} to trigger
     * privilege cache synchronization.

     *
     * @param name the unique name for the global role (e.g., "SYSTEM_ADMIN")
     * @param privileges the set of {@link PrivilegeBase} enums to assign to the role
     * @param removable whether the role can be deleted by users (false for system-required roles)
     * @return the created or updated {@link GlobalRole} entity persisted to the database
     */
    public GlobalRole createOrUpdateGlobalRole(String name, Set<PrivilegeBase> privileges, boolean removable) {
        debug("[createOrUpdateGlobalRole] name: {}; privilages: {}; removable: {}", name, privileges, removable);
        GlobalRole role = repositories.unsecure.globalRole.findByName(name);
        if (role == null) {
            role = new GlobalRole(name);
        }
        role.setPrivilegesSet(privileges);
        role.setRemovable(removable);
        repositories.unsecure.globalRole.save(role);
        role = repositories.unsecure.globalRole.findByName(name);
        applicationEventPublisher.publishEvent(new PrivilegeChangeEvent(this));
        return role;
    }

    /**
     * Creates or updates an organization role with the specified privileges.
     * <p>
     * This is a convenience method that delegates to {@link #createOrUpdateOrgRole(Long, String, Set, boolean)}
     * with a null organization ID, allowing the role to be discovered by name across organizations.

     *
     * @param name the unique name for the organization role (e.g., "ORG_ADMIN")
     * @param privileges the set of {@link PrivilegeBase} enums to assign to the role
     * @param removable whether the role can be deleted by users
     * @return the created or updated {@link OrganizationRole} entity
     * @see #createOrUpdateOrgRole(Long, String, Set, boolean)
     */
    public OrganizationRole createOrUpdateOrgRole(String name, Set<PrivilegeBase> privileges, boolean removable) {
        return createOrUpdateOrgRole(null, name, privileges, removable);
    }

    /**
     * Creates a new organization role or updates an existing one with the specified privileges.
     * <p>
     * Organization roles are scoped to a single tenant and provide privileges only within that organization's
     * context. If a role with the given name exists, its privileges and removability flag are updated.
     * The organization ID is stored for reference but role lookup is name-based. Publishes a
     * {@link PrivilegeChangeEvent} to trigger privilege cache synchronization.

     *
     * @param id the organization ID for reference (nullable, not used for role lookup)
     * @param name the unique name for the organization role
     * @param privileges the set of {@link PrivilegeBase} enums to assign to the role
     * @param removable whether the role can be deleted by users (false for required tenant roles)
     * @return the created or updated {@link OrganizationRole} entity persisted to the database
     */
    public OrganizationRole createOrUpdateOrgRole(Long id, String name, Set<PrivilegeBase> privileges, boolean removable) {
        debug("[createOrUpdateOrgRole] orgName: {}; privilages: {}; removable: {}", name, privileges, removable);
        OrganizationRole role = repositories.unsecure.organizationRole.findByName(name);
        if (role == null) {
            role = new OrganizationRole(id, name);
        }
        role.setPrivilegesSet(privileges);
        role.setRemovable(removable);
        repositories.unsecure.organizationRole.save(role);
        role = repositories.unsecure.organizationRole.findByName(name);
        applicationEventPublisher.publishEvent(new PrivilegeChangeEvent(this));
        return role;
    }

    /**
     * Creates or updates a global-organization role with the specified privileges.
     * <p>
     * This is a convenience method that delegates to {@link #createOrUpdateGlobalOrgRole(Long, String, Set, boolean)}
     * with a null organization ID.

     *
     * @param name the unique name for the global-organization role
     * @param privileges the set of {@link PrivilegeBase} enums to assign to the role
     * @param removable whether the role can be deleted by users
     * @return the created or updated {@link GlobalOrganizationRole} entity
     * @see #createOrUpdateGlobalOrgRole(Long, String, Set, boolean)
     */
    public GlobalOrganizationRole createOrUpdateGlobalOrgRole(String name, Set<PrivilegeBase> privileges, boolean removable) {
        return createOrUpdateGlobalOrgRole(null, name, privileges, removable);
    }

    /**
     * Creates a new global-organization role or updates an existing one with the specified privileges.
     * <p>
     * Global-organization roles are hybrid roles providing cross-tenant access while maintaining organization
     * context. This role type is rarely used and typically reserved for special administrative scenarios.
     * If a role with the given name exists, its privileges and removability flag are updated. Publishes a
     * {@link PrivilegeChangeEvent} to trigger privilege cache synchronization.

     *
     * @param id the organization ID for reference (nullable, not used for role lookup)
     * @param name the unique name for the global-organization role
     * @param privileges the set of {@link PrivilegeBase} enums to assign to the role
     * @param removable whether the role can be deleted by users
     * @return the created or updated {@link GlobalOrganizationRole} entity persisted to the database
     */
    public GlobalOrganizationRole createOrUpdateGlobalOrgRole(Long id, String name, Set<PrivilegeBase> privileges, boolean removable) {
        debug("[createOrUpdateGlobalOrgRole] orgName: {}; privilages: {}; removable: {}", name, privileges, removable);
        GlobalOrganizationRole role = repositories.unsecure.globalOrganizationRole.findByName(name);
        if (role == null) {
            role = new GlobalOrganizationRole(id, name);
        }
        role.setPrivilegesSet(privileges);
        role.setRemovable(removable);
        repositories.unsecure.globalOrganizationRole.save(role);
        role = repositories.unsecure.globalOrganizationRole.findByName(name);
        applicationEventPublisher.publishEvent(new PrivilegeChangeEvent(this));
        return role;
    }

    /**
     * Creates a new role based on its type discriminator value.
     * <p>
     * This method routes role creation to the appropriate type-specific method based on the provided type string.
     * Supports three role types: {@code ROLE_TYPE_GLOBAL}, {@code ROLE_TYPE_ORG}, and {@code ROLE_TYPE_GLOBAL_ORG}.
     * All roles created through this method are marked as removable by default. Requires {@code CHECK_CAN_MANAGE_ROLES}
     * privilege to execute.

     *
     * @param name the unique name for the role
     * @param type the role type discriminator ({@link #ROLE_TYPE_GLOBAL}, {@link #ROLE_TYPE_ORG}, or {@link #ROLE_TYPE_GLOBAL_ORG})
     * @param privileges the set of {@link PrivilegeBase} enums to assign to the role
     * @return the created {@link Role} entity, or null if the type is not recognized
     * @see #createOrUpdateGlobalRole(String, Set, boolean)
     * @see #createOrUpdateOrgRole(String, Set, boolean)
     * @see #createOrUpdateGlobalOrgRole(String, Set, boolean)
     */
    @PreAuthorize(CHECK_CAN_MANAGE_ROLES)
    public Role createRole(String name, String type, Set<PrivilegeBase> privileges) {
        debug("[createRole] Creating role {} of type {} and privileges {}", name, type, privileges);
        if (type.equals(ROLE_TYPE_GLOBAL)) {
            return createOrUpdateGlobalRole(name, privileges, true);
        } else if (type.equals(ROLE_TYPE_ORG)) {
            return createOrUpdateOrgRole(name, privileges, true);
        } else if (type.equals(ROLE_TYPE_GLOBAL_ORG)) {
            return createOrUpdateGlobalOrgRole(name, privileges, true);
        }
        return null;
    }

    /**
     * Validates whether a role with the given name and type already exists in the database.
     * <p>
     * Checks the appropriate repository based on the role type discriminator. If a duplicate role is found,
     * adds a validation error to the {@link BindingResult} with the error code "name.exists". This method
     * is typically used during form validation before role creation.

     *
     * @param name the role name to check for uniqueness
     * @param type the role type discriminator ({@link #ROLE_TYPE_GLOBAL}, {@link #ROLE_TYPE_ORG}, or {@link #ROLE_TYPE_GLOBAL_ORG})
     * @param br the Spring {@link BindingResult} to populate with validation errors if the role exists
     * @return true if a role with the given name and type exists, false otherwise
     */
    public boolean checkIfRoleNameAlreadyExists(String name, String type, BindingResult br) {
        debug("[checkIfRoleNameAlreadyExists]");
        boolean roleExists = false;
        if (type.equals(ROLE_TYPE_GLOBAL)) {
            roleExists = repositories.unsecure.globalRole.findByName(name) != null;
        } else if (type.equals(ROLE_TYPE_ORG)) {
            roleExists = repositories.unsecure.organizationRole.findByName(name) != null;
        } else if (type.equals(ROLE_TYPE_GLOBAL_ORG)) {
            roleExists = repositories.unsecure.globalOrganizationRole.findByName(name) != null;
        }
        if (roleExists) {
            debug("[checkIfRoleNameAlreadyExists] role with name {} and type {} already exists", name, type);
            br.rejectValue("name", "name.exists");
        }
        return roleExists;
    }

    /**
     * Adds additional privileges to an existing role, merging them with current privileges.
     * <p>
     * Retrieves the role by name, merges the provided privileges with the role's existing privilege set,
     * and persists the updated role. Privileges are serialized as a comma-separated string in the database
     * using {@code PrivilegeHelper}. Publishes a {@link PrivilegeChangeEvent} to trigger cache invalidation.
     * If the role is not found, returns null.

     *
     * @param roleName the unique name of the role to update
     * @param privileges the set of {@link PrivilegeBase} enums to add (duplicates are ignored)
     * @return the updated {@link Role} entity, or null if the role with the given name does not exist
     */
    public Role addPrivilegesToRole(String roleName, Set<PrivilegeBase> privileges) {
        debug("[addPrivilagesToRole] role name: {}; privilages: {}", roleName, privileges);
        Role role = repositories.unsecure.role.findByName(roleName);
        if (role == null) {
            debug("[addPrivilegesToRole] role {} not found", roleName);
            return null;
        }
        privileges.addAll(role.getPrivilegesSet());
        role.setPrivilegesSet(privileges);
        role = repositories.unsecure.role.save(role);
        applicationEventPublisher.publishEvent(new PrivilegeChangeEvent(this));
        return role;
    }
    
    /**
     * Removes the specified privileges from all roles in the system.
     * <p>
     * Iterates through all roles in the database and removes any occurrences of the provided privileges.
     * Creates defensive copies of privilege sets to safely modify role privileges without side effects.
     * Only roles that have been modified are persisted. Publishes a {@link PrivilegeChangeEvent} to trigger
     * privilege cache synchronization across the application. This method is typically used when deprecating
     * or removing privilege definitions from the system.

     *
     * @param privileges the set of {@link PrivilegeBase} enums to remove from all roles
     */
    public void removePrivilegesFromRoles(Set<PrivilegeBase> privileges) {
        debug("[addPrivilagesToRole] privilages: {}", privileges);
        List<Role> roles = repositories.unsecure.role.findAll();
        List<Role> modifiedRoles = new ArrayList<>();
        for (Role role : roles) {
            Set<PrivilegeBase> currentPrivs = new HashSet<>(role.getPrivilegesSet());
            if(currentPrivs.removeAll(privileges)) {
                modifiedRoles.add(role);
            }
            
            role.setPrivilegesSet(currentPrivs); 
        }
        
        applicationEventPublisher.publishEvent(new PrivilegeChangeEvent(this));
        repositories.unsecure.role.saveAll(modifiedRoles);
    }

}
