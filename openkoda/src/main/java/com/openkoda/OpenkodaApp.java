package com.openkoda;

import com.openkoda.core.helper.SpringProfilesHelper;
import com.openkoda.repository.NativeQueries;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.openkoda.core.helper.SpringProfilesHelper.SPRING_PROFILES_ACTIVE_ENV;
import static com.openkoda.core.helper.SpringProfilesHelper.SPRING_PROFILES_ACTIVE_PROP;
import static com.openkoda.service.dynamicentity.DynamicEntityRegistrationService.buildAndLoadDynamicClasses;

/**
 * Alternate JVM entry point that orchestrates database discovery, dynamic entity compilation, and Spring application startup.
 * <p>
 * This launcher provides a complete initialization workflow for OpenKoda applications that require:
 * <ul>
 *   <li>Profile normalization from command-line arguments or environment variables</li>
 *   <li>Database schema upgrades and dynamic entity discovery via {@link JDBCApp}</li>
 *   <li>Byte Buddy runtime class compilation before Spring context initialization</li>
 *   <li>Forced startup mode that bypasses redundant safety checks</li>
 * </ul>

 * <p>
 * The startup sequence delegates to {@link JDBCApp} for database operations, then invokes
 * {@link com.openkoda.service.dynamicentity.DynamicEntityRegistrationService#buildAndLoadDynamicClasses(ClassLoader)}
 * to compile dynamically-generated entity classes using Byte Buddy. Finally, it launches the Spring Boot application
 * context via {@link App#startApp(Class, String[], boolean)} with forced mode enabled.

 * <p>
 * Profile configuration follows a precedence hierarchy:
 * <ol>
 *   <li>Existing system property {@code spring.profiles.active} (highest priority)</li>
 *   <li>Command-line argument {@code --spring.profiles.active=value}</li>
 *   <li>Environment variable {@code SPRING_PROFILES_ACTIVE}</li>
 * </ol>

 *
 * @see App
 * @see JDBCApp
 * @see com.openkoda.service.dynamicentity.DynamicEntityRegistrationService
 * @see SpringProfilesHelper
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class OpenkodaApp {

    /**
     * Autowired utility bean for executing native SQL queries.
     * <p>
     * Currently unused in OpenkodaApp, but retained for potential future direct query execution
     * during application startup or database discovery operations.

     */
    @Autowired
    NativeQueries nativeQueries;

    /**
     * Application entry point that performs safety check and delegates to {@link #startOpenkodaApp(Class, String[])}.
     * <p>
     * Execution flow:
     * <ol>
     *   <li>Extract {@code --force} flag from command-line arguments</li>
     *   <li>Invoke {@link App#initializationSafetyCheck(boolean)} to validate initialization profile usage</li>
     *   <li>Delegate full startup orchestration to {@link #startOpenkodaApp(Class, String[])}</li>
     * </ol>

     * <p>
     * The {@code --force} flag bypasses interactive confirmation prompts when using database initialization
     * profiles such as {@code drop_and_init_database}, allowing non-interactive automated deployments.

     *
     * @param args command-line arguments, supports {@code --force} flag for initialization bypass
     * @throws ClassNotFoundException if JDBC driver or dynamic entity classes cannot be loaded
     * @throws IOException if property files or dynamic entity source files cannot be read
     * @throws URISyntaxException if dynamic entity source paths are malformed
     * @see #startOpenkodaApp(Class, String[])
     * @see App#initializationSafetyCheck(boolean)
     */
    public static void main(String[] args) throws ClassNotFoundException, IOException, URISyntaxException {
        boolean isForce = args != null && Arrays.stream(args).anyMatch(a -> "--force".equals(a));
        App.initializationSafetyCheck(isForce);
        OpenkodaApp.startOpenkodaApp(App.class, args);
    }

    /**
     * Orchestrates full application startup: profile setup, database operations, dynamic entity compilation, Spring context launch.
     * <p>
     * Complete execution workflow:
     * <ol>
     *   <li>{@link #setProfiles(String[])} normalizes {@code SPRING_PROFILES_ACTIVE} from args or environment</li>
     *   <li>{@link JDBCApp#main(String[])} performs database schema upgrades and form/dynamic entity discovery</li>
     *   <li>{@link com.openkoda.service.dynamicentity.DynamicEntityRegistrationService#buildAndLoadDynamicClasses(ClassLoader)}
     *       compiles Byte Buddy-generated entity classes (skipped for initialization profiles)</li>
     *   <li>{@link App#startApp(Class, String[], boolean)} launches Spring Boot context with {@code forced=true}</li>
     * </ol>

     * <p>
     * The {@code forced=true} parameter passed to {@link App#startApp(Class, String[], boolean)} bypasses
     * duplicate safety checks, since {@link App#initializationSafetyCheck(boolean)} was already performed
     * in {@link #main(String[])}.

     * <p>
     * Dynamic entity compilation is conditionally skipped when {@link SpringProfilesHelper#isInitializationProfile()}
     * returns true, as initialization profiles (e.g., {@code drop_and_init_database}) operate on empty databases
     * where no dynamic entities exist yet.

     *
     * @param appClass application class to run, typically {@link App}.class
     * @param args command-line arguments for profile configuration and application parameters
     * @throws IOException if dynamic entity resources cannot be read during compilation
     * @throws ClassNotFoundException if JDBC driver cannot be loaded for database operations
     * @throws URISyntaxException if resource paths for dynamic entity sources are invalid
     * @see #setProfiles(String[])
     * @see JDBCApp#main(String[])
     * @see App#startApp(Class, String[], boolean)
     * @see SpringProfilesHelper#isInitializationProfile()
     */
    public static void startOpenkodaApp(Class appClass, String[] args) throws IOException, ClassNotFoundException, URISyntaxException {
        setProfiles(args);
        JDBCApp.main(args);
        if(!SpringProfilesHelper.isInitializationProfile()) {
            buildAndLoadDynamicClasses(App.class.getClassLoader());
        }
        App.startApp(appClass, args, true);
    }

    /**
     * Normalizes Spring profile configuration from command-line arguments or environment variables.
     * <p>
     * Profile resolution follows a strict precedence hierarchy to ensure predictable configuration:
     * <ol>
     *   <li>Existing system property {@code spring.profiles.active} (highest priority, no override)</li>
     *   <li>Command-line argument {@code --spring.profiles.active=value} extracted from args array</li>
     *   <li>Environment variable {@code SPRING_PROFILES_ACTIVE} (lowest priority, typically Docker deployments)</li>
     * </ol>

     * <p>
     * Converts Maven/Gradle format {@code -Dspring-boot.run.profiles} to Spring Boot format
     * {@code --spring.profiles.active} when processing command-line arguments. The conversion is
     * handled by the JVM argument parser before args reaches this method.

     * <p>
     * Sets the {@code spring.profiles.active} system property for Spring Boot auto-detection during
     * {@link org.springframework.context.ApplicationContext} initialization.

     * <p>
     * Example: {@code --spring.profiles.active=openkoda,local} sets system property {@code spring.profiles.active}
     * to enable both the {@code openkoda} and {@code local} profiles for the application runtime.

     *
     * @param args command-line arguments potentially containing {@code --spring.profiles.active=value}
     * @see SpringProfilesHelper#SPRING_PROFILES_ACTIVE_PROP
     * @see SpringProfilesHelper#SPRING_PROFILES_ACTIVE_ENV
     */
    private static void setProfiles(String[] args){
        if(System.getProperty(SPRING_PROFILES_ACTIVE_PROP) == null && args != null && args.length > 0){
            //profiles are set as param in command line (for components app)
            //important: the argument in command line -Dspring-boot.run.profiles is switched to --spring.profiles.active in args variable
            List<String> filteredArgs = Stream.of(args).filter(a -> a.contains(SPRING_PROFILES_ACTIVE_PROP)).toList();
            if(!filteredArgs.isEmpty()) {
                System.setProperty(SPRING_PROFILES_ACTIVE_PROP, filteredArgs.get(0).split("=")[1]);
            } else if (System.getenv(SPRING_PROFILES_ACTIVE_ENV) != null) {
//                In case we start Openkoda as a cloud instance configured via environment properties in a Dockerfile
                System.setProperty(SPRING_PROFILES_ACTIVE_PROP, System.getenv(SPRING_PROFILES_ACTIVE_ENV));
            }
        }
    }
}
