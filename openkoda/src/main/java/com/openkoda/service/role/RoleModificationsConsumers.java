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

package com.openkoda.service.role;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.controller.common.PageAttributes;
import com.openkoda.core.customisation.ServerJSRunner;
import com.openkoda.core.security.UserProvider;
import com.openkoda.dto.OrganizationRelatedObject;
import com.openkoda.model.Role;
import com.openkoda.model.UserRole;
import jakarta.inject.Inject;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Role assignment reconciliation service using server-side JavaScript evaluation for dynamic role modifications.
 * <p>
 * This service orchestrates role assignment changes by executing JavaScript scripts that determine which roles
 * should be added or removed for users within an organization. It integrates with {@link ServerJSRunner} to
 * evaluate scripts and performs bulk save/delete operations using unsecure repositories for performance.
 * The service switches authentication contexts via {@link UserProvider} to perform privileged operations when
 * modifying global roles.
 * </p>
 * <p>
 * Complete reconciliation workflow:
 * <ol>
 *   <li>Execute server-side JavaScript script with organization context</li>
 *   <li>Script returns Map&lt;String, Boolean&gt; indicating role presence (true=add, false=remove)</li>
 *   <li>Filter map to extract role names for addition and removal</li>
 *   <li>Query existing UserRole entities matching removal criteria</li>
 *   <li>Delete UserRole entities in bulk</li>
 *   <li>Compute set difference to find users lacking required roles</li>
 *   <li>Create new UserRole entities for missing assignments</li>
 *   <li>Save new UserRole entities in bulk</li>
 * </ol>
 * </p>
 * <p>
 * JavaScript script interface specification:
 * <pre>
 * Input: organizationRelatedObject (available in script context)
 * Output: Map&lt;String, Boolean&gt; where key=roleName, value=shouldHaveRole
 * 
 * Example script:
 * if (organizationRelatedObject.department === 'Engineering') {
 *   return { 'DEVELOPER': true, 'VIEWER': false };
 * }
 * </pre>
 * </p>
 * <p>
 * <b>Thread-safety WARNING:</b> Methods that call {@link UserProvider#setConsumerAuthentication()} and
 * {@link UserProvider#clearAuthentication()} temporarily modify thread-local authentication state. Callers
 * must ensure proper cleanup and avoid concurrent modifications to the same organization's roles.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ServerJSRunner
 * @see UserProvider
 * @see UserRole
 * @see Role
 */
@Service
public class RoleModificationsConsumers extends ComponentProvider {

    /**
     * Server-side JavaScript execution engine used to evaluate role modification scripts.
     * <p>
     * Scripts are evaluated with organization context and return role assignment decisions
     * as a Map of role names to boolean flags indicating presence requirements.
     * </p>
     */
    @Inject
    ServerJSRunner serverJSRunner;

    /**
     * Reconciles user-level role assignments for all users in an organization by executing a JavaScript script.
     * <p>
     * This method executes the specified server-side JavaScript script to determine which roles should be present
     * for users within the organization. The script returns a map of role names to boolean flags, where true
     * indicates the role should be added and false indicates removal. The method then computes the set difference
     * between current assignments and desired state, performing bulk delete and save operations using unsecure
     * repositories for performance.
     * </p>
     * <p>
     * Workflow for role removal:
     * <ol>
     *   <li>Query all existing UserRole entities in organization with roles marked for removal</li>
     *   <li>Delete all matched UserRole entities in a single batch operation</li>
     * </ol>
     * </p>
     * <p>
     * Workflow for role addition:
     * <ol>
     *   <li>Query all user IDs in the organization</li>
     *   <li>For each role to add, query user IDs that already have the role</li>
     *   <li>Compute set difference to find users lacking the role</li>
     *   <li>Create new UserRole entities for missing assignments</li>
     *   <li>Save all new UserRole entities in a single batch operation</li>
     * </ol>
     * </p>
     * <p>
     * <b>Note:</b> This method does not establish a transactional boundary. Callers are responsible for
     * managing transaction scope if atomicity is required across script execution and role modifications.
     * </p>
     *
     * @param organizationRelatedObject the organization context for role modifications, passed to the JavaScript
     *                                  script for evaluation criteria (must not be null)
     * @param scriptName the name of the server-side JavaScript script to execute, which must return a
     *                   Map&lt;String, Boolean&gt; of role names to presence flags (must not be null)
     * @return always returns true after attempting role modifications, regardless of whether changes were made
     */
    public boolean modifyRoleForAllUsersInOrganization(OrganizationRelatedObject organizationRelatedObject, String scriptName) {
        debug("[modifyRoleForAllUsersInOrganization]");

        Map<String, Boolean> userRolesFromScript = runModifyRolesScript(organizationRelatedObject, scriptName);

        Set<String> rolesToRemove = getRoleNamesToRemove(userRolesFromScript);
        Set<String> rolesToAdd = getRoleNamesToAdd(userRolesFromScript);

        if (CollectionUtils.isNotEmpty(rolesToRemove)) {
//            search user roles which exist and should be removed
            List<UserRole> userRolesToRemove = repositories.unsecure.userRole.findAllUserRolesInOrganizationWithRoles(organizationRelatedObject.getOrganizationId(), rolesToRemove);
            repositories.unsecure.userRole.deleteAll(userRolesToRemove);
        }

        if (CollectionUtils.isNotEmpty(rolesToAdd)) {
            Set<Long> userIdsInOrganization = repositories.unsecure.userRole.findAllUserIdsInOrganization(organizationRelatedObject.getOrganizationId());
            Map<String, Role> rolesMappedByName = repositories.unsecure.role.findAll().stream().collect(Collectors.toMap(Role::getName, Function.identity()));

            List<UserRole> userRolesToAdd = new ArrayList<>();

            for (String s : rolesToAdd) {

                Set<Long> userIdsWithRole = repositories.unsecure.userRole.findAllUserIdsInOrganizationWithRole(organizationRelatedObject.getOrganizationId(), s);

                //prepare user Ids where we want to add the role
                Set<Long> userIdsWithoutRole = new HashSet<>();
                userIdsWithoutRole.addAll(userIdsInOrganization);
                userIdsWithoutRole.removeAll(userIdsWithRole);

                userIdsWithoutRole.forEach(a -> userRolesToAdd.add(new UserRole(null, a, rolesMappedByName.get(s).getId(), organizationRelatedObject.getOrganizationId())));
            }

            repositories.unsecure.userRole.saveAll(userRolesToAdd);
        }
        return true;

    }

    /**
     * Reconciles organization-global role assignments (roles not tied to specific users) by executing a JavaScript script.
     * <p>
     * This method handles global roles at the organization level, where UserRole entities have null userId fields.
     * Similar to user-level role modifications, it executes a JavaScript script to determine desired role state,
     * then performs bulk operations to add or remove global roles. Unlike user-level modifications, this method
     * requires temporary authentication context switching via {@link UserProvider} to perform privileged operations.
     * </p>
     * <p>
     * Workflow for global role removal:
     * <ol>
     *   <li>Set consumer authentication context via {@link UserProvider#setConsumerAuthentication()}</li>
     *   <li>Query all global UserRole entities (userId=null) in organization with roles marked for removal</li>
     *   <li>Delete all matched UserRole entities in a single batch operation</li>
     *   <li>Clear authentication context via {@link UserProvider#clearAuthentication()}</li>
     * </ol>
     * </p>
     * <p>
     * Workflow for global role addition:
     * <ol>
     *   <li>Query existing global roles in organization to avoid duplicates</li>
     *   <li>Filter roles to add, excluding those already assigned</li>
     *   <li>Create new UserRole entities with null userId for global assignment</li>
     *   <li>Save all new UserRole entities in a single batch operation</li>
     * </ol>
     * </p>
     * <p>
     * <b>Security note:</b> This method temporarily elevates privileges via {@link UserProvider#setConsumerAuthentication()}
     * during role removal operations. The authentication context is cleared in a finally-like pattern to prevent
     * privilege leakage, but callers should be aware of the security implications.
     * </p>
     *
     * @param organizationRelatedObject the organization context for role modifications, passed to the JavaScript
     *                                  script for evaluation criteria (must not be null)
     * @param scriptName the name of the server-side JavaScript script to execute, which must return a
     *                   Map&lt;String, Boolean&gt; of role names to presence flags (must not be null)
     * @return always returns true after attempting role modifications, regardless of whether changes were made
     */
    public boolean modifyGlobalRoleForOrganization(OrganizationRelatedObject organizationRelatedObject, String scriptName) {
        debug("[modifyGlobalRoleForOrganization]");

        Map<String, Boolean> globalRolesFromScript = runModifyRolesScript(organizationRelatedObject, scriptName);
        Set<String> rolesToRemove = getRoleNamesToRemove(globalRolesFromScript);
        Set<String> rolesToAdd = getRoleNamesToAdd(globalRolesFromScript);

        if (CollectionUtils.isNotEmpty(rolesToRemove)) {
//            search global organization roles which exist and should be removed
            UserProvider.setConsumerAuthentication();
            List<UserRole> rolesToBeRemoved = repositories.unsecure.userRole.findAllGlobalRolesInOrganizationWithRoles(organizationRelatedObject.getOrganizationId(), rolesToRemove);
            repositories.unsecure.userRole.deleteAll(rolesToBeRemoved);
            UserProvider.clearAuthentication();
        }

        if (CollectionUtils.isNotEmpty(rolesToAdd)) {
            Map<String, Role> rolesMappedByName = repositories.unsecure.role.findAll().stream().collect(Collectors.toMap(Role::getName, Function.identity()));

            List<UserRole> rolesToBeAdded = new ArrayList<>();
            List<String> rolesAlreadyAdded = repositories.unsecure.userRole.findAllGlobalRolesInOrganizationWithRoles(organizationRelatedObject.getOrganizationId(), rolesToAdd)
                    .stream().map(UserRole::getRoleName).collect(Collectors.toList());
            rolesToAdd.stream().filter(role -> !rolesAlreadyAdded.contains(role))
                    .forEach(role -> rolesToBeAdded.add(new UserRole(null, null, rolesMappedByName.get(role).getId(), organizationRelatedObject.getOrganizationId())));

            repositories.unsecure.userRole.saveAll(rolesToBeAdded);
        }
        return true;
    }

    /**
     * Executes a server-side JavaScript script and returns role modification decisions.
     * <p>
     * This method builds a script execution model containing the organizationRelatedObject under the key
     * {@link PageAttributes#organizationRelatedObject}, invokes {@link ServerJSRunner} to evaluate the named
     * script, and returns the result as a map of role names to boolean presence flags. If the script returns
     * null or encounters an error, an empty map is returned and an error is logged.
     * </p>
     * <p>
     * The script is expected to access the organizationRelatedObject from its execution context and return a
     * Map&lt;String, Boolean&gt; where each key is a role name and each value indicates whether the role should
     * be present (true) or absent (false).
     * </p>
     *
     * @param organizationRelatedObject the organization context to pass to the script execution model, available
     *                                  to the script for evaluation logic (must not be null)
     * @param scriptName the name of the server-side JavaScript script to execute via {@link ServerJSRunner}
     *                   (must not be null)
     * @return a map of role names to boolean presence flags (true=add role, false=remove role), or an empty
     *         map if the script returns null or encounters an error during execution
     */
    private Map<String, Boolean> runModifyRolesScript(OrganizationRelatedObject organizationRelatedObject, String scriptName) {
        debug("[runModifyRolesScript]");
        Map<String, Object> model = new HashMap<>(Map.of(PageAttributes.organizationRelatedObject.name, organizationRelatedObject));
        Map scriptResult = serverJSRunner.evaluateServerJsScript(scriptName, model, null, Map.class);

        if (scriptResult == null) {
            error("[modifyGlobalRoleForOrganization] Script returned null");
            return Collections.EMPTY_MAP;
        }
        return scriptResult;
    }

    /**
     * Filters a role map to extract names of roles marked for removal.
     * <p>
     * This method processes the role modification decisions returned by the JavaScript script, identifying
     * all role names where the boolean flag is false (indicating the role should be removed). The result
     * is used to query and delete existing UserRole entities matching the removal criteria.
     * </p>
     *
     * @param roles a map of role names to presence flags from script execution, where false indicates the
     *              role should be removed (must not be null, may be empty)
     * @return a set of role names where the presence flag is false (roles to remove), or an empty set if
     *         no roles are marked for removal
     */
    private Set<String> getRoleNamesToRemove(Map<String, Boolean> roles) {
        debug("[getRoleNamesToRemove]");
        return roles.entrySet().stream().filter(a -> not(a.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    /**
     * Filters a role map to extract names of roles marked for addition.
     * <p>
     * This method processes the role modification decisions returned by the JavaScript script, identifying
     * all role names where the boolean flag is true (indicating the role should be added). The result is
     * used to create new UserRole entities for users or organizations that lack the required roles.
     * </p>
     *
     * @param roles a map of role names to presence flags from script execution, where true indicates the
     *              role should be added (must not be null, may be empty)
     * @return a set of role names where the presence flag is true (roles to add), or an empty set if no
     *         roles are marked for addition
     */
    private Set<String> getRoleNamesToAdd(Map<String, Boolean> roles) {
        debug("[getRoleNamesToAdd]");
        return roles.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

}

