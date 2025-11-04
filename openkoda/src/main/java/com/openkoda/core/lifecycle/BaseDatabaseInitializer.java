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

package com.openkoda.core.lifecycle;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.core.multitenancy.QueryExecutor;
import com.openkoda.core.security.UserProvider;
import com.openkoda.model.*;
import com.openkoda.model.component.ServerJs;
import com.openkoda.service.export.ClasspathComponentImportService;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.function.Tuple2;

import java.util.*;
import java.util.stream.Collectors;

import static com.openkoda.core.helper.SpringProfilesHelper.TEST_PROFILE;

/**
 * Manages application lifecycle startup initialization by executing database operations required for a clean OpenKoda instance.
 * This class orchestrates the complete startup initialization sequence including database connection, schema verification,
 * module registration, role and privilege initialization, event listener registration, and scheduled job startup.
 * <p>
 * The initialization sequence follows this order:
 * <ol>
 * <li>Database connection establishment and schema verification</li>
 * <li>Authentication context setup via {@link UserProvider#setCronJobAuthentication}</li>
 * <li>Core module registration via {@link #createCoreModule()}</li>
 * <li>Default role and privilege initialization via {@link #createInitialRoles()}</li>
 * <li>Registration form ServerJs entity creation via {@link #createRegistrationFormServerJs()}</li>
 * <li>Custom SQL initialization scripts execution via {@link #runInitializationScripts()}</li>
 * <li>OpenKoda component loading from classpath via {@link ClasspathComponentImportService}</li>
 * <li>Authentication context cleanup via {@link UserProvider#clearAuthentication}</li>
 * </ol>
 * <p>
 * This production routine of database setup is activated when Spring profile != "test" (via {@code @Profile("!" + TEST_PROFILE)}).
 * * <p>
 * All initialization operations are executed within a single transaction (via {@code @Transactional} on {@link #loadInitialData(boolean)})
 * to ensure atomic bootstrap mutations. If any operation fails, the entire initialization is rolled back.
 * <p>
 * <b>Extension Pattern:</b> To extend or replace the scope of database initialization, implement another Spring component
 * that extends BaseDatabaseInitializer and is activated by {@link org.springframework.context.annotation.Primary} annotation
 * or specific profile. The overriding implementation should call {@code super.loadInitialData(proceed)} or implement
 * custom initialization logic while respecting the proceed parameter.
 *
 * @see SearchViewCreator
 * @see ClasspathComponentImportService
 * @see UserProvider
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 */
@Component
@Profile("!" + TEST_PROFILE)
public class BaseDatabaseInitializer extends ComponentProvider {

    /**
     * Default global admin role created during application initialization.
     * This role has full system privileges and is assigned to the initial admin user.
     * Users with this role have unrestricted access to all application features across all organizations.
     * Privilege set: {@link PrivilegeHelper#getAdminPrivilegeSet}
     */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * Default global user role created during application initialization.
     * This role has standard user privileges applicable across all organizations.
     * Users with this role have basic access rights without administrative capabilities.
     * Privilege set: {@link PrivilegeHelper#getUserPrivilegeSet}
     */
    public static final String ROLE_USER = "ROLE_USER";

    /**
     * Default organization-level admin role created during application initialization.
     * This role has administrative privileges scoped to a specific organization (tenant).
     * Users with this role can manage organization-specific resources and users.
     * Privilege set: {@link PrivilegeHelper#getOrgAdminPrivilegeSet}
     */
    public static final String ROLE_ORG_ADMIN = "ROLE_ORG_ADMIN";

    /**
     * Default organization-level user role created during application initialization.
     * This role has standard user privileges scoped to a specific organization (tenant).
     * Users with this role can access organization-specific resources without administrative rights.
     * Privilege set: {@link PrivilegeHelper#getOrgUserPrivilegeSet}
     */
    public static final String ROLE_ORG_USER = "ROLE_ORG_USER";

    /**
     * Core module identifier used to register the OpenKoda core module entity in the database.
     * This constant is persisted as an {@link OpenkodaModule} entity during initialization
     * to represent the foundational OpenKoda module in the system.
     */
    public static final String CORE_MODULE = "core";

    /**
     * List of SQL initialization scripts executed on application initialization.
     * Scripts are loaded from classpath resources specified in the comma-separated property
     * {@code global.initialization.scripts.commaseparated}.
     * Scripts are executed in the order they appear in the list.
     */
    List<String> globalInitializationScripts = Collections.emptyList();

    /**
     * Content of an external SQL script to be executed during database initialization.
     * Used primarily in cloud instance setup where post-init scripts cannot be added as
     * classpath resource files (like {@code init.sql}). Double quotes in the script content
     * are automatically unescaped to support bash script insertion.
     * Property key: {@code global.initialization.externalScript}
     */
    private String initializationExternalScript;

    /**
     * Spring Security password encoder for hashing user passwords.
     * Injected but not directly used in this class; available for subclass implementations
     * that may need custom password encoding logic during initialization.
     */
    @Inject
    private PasswordEncoder passwordEncoder;

    /**
     * Email address for the initial application administrator account.
     * This property is mandatory; if blank, the application exits with status code 1.
     * Property key: {@code application.admin.email}
     */
    @Value("${application.admin.email}")
    private String applicationAdminEmail;

    /**
     * Username for the initial application administrator account.
     * Property key: {@code init.admin.username}
     */
    @Value("${init.admin.username}")
    private String initAdminUsername;

    /**
     * Password for the initial application administrator account.
     * Property key: {@code init.admin.password}
     */
    @Value("${init.admin.password}")
    private String initAdminPassword;

    /**
     * First name for the initial application administrator account.
     * Property key: {@code init.admin.firstName}
     * Default value: "Mark"
     */
    @Value("${init.admin.firstName:Mark}") private String initAdminFirstName;

    /**
     * Last name for the initial application administrator account.
     * Property key: {@code init.admin.lastName}
     * Default value: "Administrator"
     */
    @Value("${init.admin.lastName:Administrator}") private String initAdminLastName;

    /**
     * Base URL for the OpenKoda application instance.
     * Used for generating absolute URLs in email notifications and external integrations.
     * Property key: {@code base.url}
     * Default value: "http://localhost:8080"
     */
    @Value("${base.url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Flag indicating that the initialization setup has already been executed.
     * Set to {@code true} after successful completion of {@link #loadInitialData(boolean)}
     * to guard against redundant initialization operations.
     */
    boolean alreadySetup = false;

    /**
     * Executes database queries transactionally during initialization.
     * Provides methods for running SQL scripts from classpath resources or raw SQL strings
     * within transaction boundaries to ensure atomic database operations.
     */
    private QueryExecutor queryExecutor;

    /**
     * Imports OpenKoda components from classpath during initialization.
     * Loads component definitions (forms, server-side JavaScript, frontend resources, etc.)
     * packaged within the application or provided by extension modules.
     */
    private ClasspathComponentImportService classpathComponentImportService;

    /**
     * Constructs a BaseDatabaseInitializer with required dependencies for database initialization.
     * Parses initialization script configuration from properties and prepares the component
     * for executing startup database operations.
     * <p>
     * Initialization scripts are parsed by splitting the comma-separated list, trimming whitespace,
     * and collecting into an ordered list. External script content has double quotes unescaped to
     * support bash script insertion patterns commonly used in cloud deployments.
     * 
     *
     * @param queryExecutor executes SQL queries transactionally for database initialization operations
     *                      such as schema setup, data seeding, and migration scripts
     * @param initializationScripts comma-separated list of classpath SQL script resource paths to execute
     *                              during startup. Scripts are executed in order. Property key:
     *                              {@code global.initialization.scripts.commaseparated}. Example:
     *                              "classpath:init.sql,classpath:seed-data.sql"
     * @param initializationExternalScript external SQL script content to execute after classpath scripts.
     *                                     Double quotes are automatically unescaped. Property key:
     *                                     {@code global.initialization.externalScript}. Useful for
     *                                     cloud environments where scripts are passed as environment variables.
     * @param classpathComponentImportService loads OpenKoda components (forms, ServerJs, frontend resources)
     *                                        from classpath during initialization, enabling modular component
     *                                        packaging and extension
     */
    public BaseDatabaseInitializer(
            @Autowired QueryExecutor queryExecutor,
            @Value("${global.initialization.scripts.commaseparated:}") String initializationScripts,
            @Value("${global.initialization.externalScript:}") String initializationExternalScript,
            @Autowired ClasspathComponentImportService classpathComponentImportService) {
        this.queryExecutor = queryExecutor;
        if (StringUtils.isNotBlank(initializationScripts)) {
            globalInitializationScripts = Arrays.stream(initializationScripts.split(",")).map(a -> StringUtils.trim(a)).collect(Collectors.toList());
        }

        if (StringUtils.isNotBlank(initializationExternalScript)) {
            // need to unescape double quotes most likely inserted by a bash scripts
            this.initializationExternalScript = initializationExternalScript.replace("\"", "");
        }

        this.classpathComponentImportService = classpathComponentImportService;
    }

    /**
     * Main procedure for database initialization orchestrating the complete startup sequence.
     * Executes the following operations in order within a single transaction:
     * <ol>
     * <li>Establishes authentication context via {@link UserProvider#setCronJobAuthentication}</li>
     * <li>Creates core module entity via {@link #createCoreModule()}</li>
     * <li>Creates default roles and admin user via {@link #createInitialRoles()}</li>
     * <li>Creates registration form ServerJs via {@link #createRegistrationFormServerJs()}</li>
     * <li>Executes custom SQL initialization scripts via {@link #runInitializationScripts()}</li>
     * <li>Loads OpenKoda components from classpath via {@link ClasspathComponentImportService#loadAllComponents}</li>
     * <li>Sets {@link #alreadySetup} flag to true</li>
     * </ol>
     * <p>
     * The authentication context is guaranteed to be cleared in a finally block regardless of
     * success or failure, ensuring proper cleanup via {@link UserProvider#clearAuthentication}.
     * 
     * <p>
     * This method can be invoked by customized inherited implementations of database initializers.
     * Any overriding implementation should respect the proceed parameter and run initialization
     * only when {@code proceed == true}. When {@code proceed == false}, the method returns immediately
     * without performing any operations.
     * 
     * <p>
     * The {@code @Transactional} annotation ensures all database operations are executed atomically.
     * If any operation fails, the entire initialization is rolled back.
     * 
     *
     * @param proceed flag indicating whether initialization should proceed. When {@code false},
     *                the method returns immediately without performing any operations. This parameter
     *                guards against redundant initialization when {@link #alreadySetup} is {@code true}
     *                or when custom initialization logic determines setup is unnecessary.
     */
    @Transactional
    public void loadInitialData(boolean proceed) {
        debug("[onApplicationContextStarting] proceed {}", proceed);
        if (not(proceed)) {
            return;
        }

        try {
            UserProvider.setCronJobAuthentication();
            createCoreModule();
            createInitialRoles();
            createRegistrationFormServerJs();
            runInitializationScripts();
            classpathComponentImportService.loadAllComponents();
            alreadySetup = true;
        } finally {
            UserProvider.clearAuthentication();
        }

    }

    /**
     * Executes SQL initialization scripts from classpath resources and external script content.
     * Scripts are executed in two phases:
     * <ol>
     * <li>Classpath scripts from {@link #globalInitializationScripts} list are executed sequentially
     *     in the order they appear. Each script is loaded from classpath and executed within a transaction
     *     via {@link QueryExecutor#runQueryFromResourceInTransaction}.</li>
     * <li>External script content from {@link #initializationExternalScript} (if present) is executed
     *     after all classpath scripts via {@link QueryExecutor#runQueriesInTransaction}.</li>
     * </ol>
     * <p>
     * All scripts are executed transactionally. If any script fails, an exception propagates to the
     * caller ({@link #loadInitialData(boolean)}), causing the entire initialization transaction to roll back.
     * 
     * <p>
     * Script execution is logged at INFO level showing the script path being executed.
     * 
     */
    private void runInitializationScripts() {
        for(String s : globalInitializationScripts) {
            info("[runInitializationScripts] executing script {}", s);
            queryExecutor.runQueryFromResourceInTransaction(s);
        }

        if (this.initializationExternalScript != null) {
            queryExecutor.runQueriesInTransaction(this.initializationExternalScript);
        }
    }

    /**
     * Creates the initial registration form ServerJs entity in the database.
     * This ServerJs entity defines the model structure for the user registration form,
     * mapping the {@code registerForm} identifier to {@link com.openkoda.form.RegisterUserForm}.
     * <p>
     * The ServerJs entity is persisted with:
     * <ul>
     * <li>code: "model"</li>
     * <li>model: JSON mapping {@code "registerForm@com.openkoda.form.RegisterUserForm" : {}}</li>
     * <li>name: "initRegisterForm"</li>
     * </ul>
     * 
     * <p>
     * The entity is saved via {@link com.openkoda.repository.unsecure.UnsecureServerJsRepository}
     * to bypass privilege checks during initialization when no authenticated user context exists.
     * 
     * <p>
     * <b>Note:</b> This method is marked for potential removal (TODO comment). The necessity of
     * creating this ServerJs entity during initialization should be reviewed as the registration
     * form model may be better defined through component imports or runtime configuration.
     * 
     */
    //TODO - check if can be removed
    private void createRegistrationFormServerJs() {
        ServerJs registerFormServerJs = new ServerJs();
        registerFormServerJs.setCode("model");
        registerFormServerJs.setModel("{\"registerForm@com.openkoda.form.RegisterUserForm\" : {}}");
        registerFormServerJs.setName("initRegisterForm");
        repositories.unsecure.serverJs.save(registerFormServerJs);
    }

    /**
     * Creates initial default roles and the administrative user for the OpenKoda application.
     * This method establishes the foundational role-based access control (RBAC) structure by creating:
     * <ul>
     * <li><b>ROLE_UNAUTHENTICATED:</b> Role for unauthenticated users with no privileges</li>
     * <li><b>ROLE_ADMIN:</b> Global administrator role with full system privileges from
     *     {@link PrivilegeHelper#getAdminPrivilegeSet}</li>
     * <li><b>ROLE_USER:</b> Global user role with standard privileges from
     *     {@link PrivilegeHelper#getUserPrivilegeSet}</li>
     * <li><b>ROLE_ORG_ADMIN:</b> Organization-level administrator role with tenant-scoped
     *     administrative privileges from {@link PrivilegeHelper#getOrgAdminPrivilegeSet}</li>
     * <li><b>ROLE_ORG_USER:</b> Organization-level user role with tenant-scoped standard
     *     privileges from {@link PrivilegeHelper#getOrgUserPrivilegeSet}</li>
     * </ul>
     * <p>
     * Roles are created or updated via {@link com.openkoda.service.role.RoleService#createOrUpdateGlobalRole}
     * and {@link com.openkoda.service.role.RoleService#createOrUpdateOrgRole} with the removable flag
     * set to {@code false}, preventing accidental deletion of these foundational roles.
     * 
     * <p>
     * <b>Admin User Creation:</b> If the {@link #applicationAdminEmail} property is blank, the application
     * prints an error message and exits with status code 1, as the admin email is mandatory for initialization.
     * If a user with {@link #initAdminUsername} does not already exist, a new admin user is created with:
     * <ul>
     * <li>First name: {@link #initAdminFirstName}</li>
     * <li>Last name: {@link #initAdminLastName}</li>
     * <li>Email: {@link #applicationAdminEmail}</li>
     * <li>Login credentials: {@link #initAdminUsername} and {@link #initAdminPassword}</li>
     * <li>Global role: ROLE_ADMIN</li>
     * </ul>
     * The admin user and credentials are persisted via unsecure repositories to bypass privilege checks.
     * 
     */
    private void createInitialRoles() {

        // in order to create admin the application.admin.email property must be set
        if (StringUtils.isBlank(applicationAdminEmail)) {
            System.out.println("*********************************************************************");
            System.out.println(" Initialization Error: set application.admin.email property.");
            System.out.println("*********************************************************************");
            System.exit(1);
        }

        Set<PrivilegeBase> adminPrivileges = PrivilegeHelper.getAdminPrivilegeSet();
        Set<PrivilegeBase> userPrivileges = new HashSet<>(PrivilegeHelper.getUserPrivilegeSet());
        Set<PrivilegeBase> unauthenticatedPrivileges = new HashSet<>(Arrays.asList());
        Set<PrivilegeBase> orgAdminPrivileges = new HashSet<>(PrivilegeHelper.getOrgAdminPrivilegeSet());
        Set<PrivilegeBase> orgUserPrivileges = new HashSet<>(PrivilegeHelper.getOrgUserPrivilegeSet());

        services.role.createOrUpdateGlobalRole("ROLE_UNAUTHENTICATED", unauthenticatedPrivileges, false);

        GlobalRole adminRole = services.role.createOrUpdateGlobalRole(ROLE_ADMIN, adminPrivileges, false);
        GlobalRole userRole = services.role.createOrUpdateGlobalRole(ROLE_USER, userPrivileges, false);
        OrganizationRole orgAdmin = services.role.createOrUpdateOrgRole(ROLE_ORG_ADMIN, orgAdminPrivileges, false);
        OrganizationRole orgUser = services.role.createOrUpdateOrgRole(ROLE_ORG_USER, orgUserPrivileges, false);

        if (repositories.unsecure.user.findByLogin(initAdminUsername) == null) {
            User u = services.user.createUser(initAdminFirstName, initAdminLastName, applicationAdminEmail, true,
                    new String[]{ROLE_ADMIN}, new Tuple2[]{});
            u.setLoginAndPassword(initAdminUsername, initAdminPassword, true);
            repositories.unsecure.loginAndPassword.save(u.getLoginAndPassword());
            repositories.unsecure.user.save(u);
        }
    }

    /**
     * Creates and persists the core OpenKoda module entity in the database.
     * This method registers the foundational "core" module using the {@link #CORE_MODULE}
     * constant as the module identifier.
     * <p>
     * The {@link OpenkodaModule} entity represents an installed module in the OpenKoda system
     * and is used for tracking module registration, dependencies, and component associations.
     * The core module is the foundational module that must exist before other modules can be registered.
     * 
     * <p>
     * The module is persisted via {@link com.openkoda.repository.unsecure.UnsecureOpenkodaModuleRepository}
     * to bypass privilege checks during initialization when no authenticated user context exists.
     * 
     */
    private void createCoreModule() {
        OpenkodaModule openkodaModule = new OpenkodaModule(CORE_MODULE);
        repositories.unsecure.openkodaModule.save(openkodaModule);
    }
}
