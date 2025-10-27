package com.openkoda;

import com.openkoda.core.helper.PrivilegeHelper;
import com.openkoda.core.helper.SpringProfilesHelper;
import com.openkoda.model.component.Form;
import com.openkoda.service.dynamicentity.DynamicEntityRegistrationService;
import com.openkoda.service.upgrade.DbVersionService;
import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import static com.openkoda.core.helper.SpringProfilesHelper.SPRING_PROFILES_ACTIVE_PROP;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Command-line utility for database discovery, dynamic entity initialization, and schema upgrades without starting full Spring context.
 * <p>
 * JDBCApp provides a lightweight alternative to full application startup for database maintenance operations. It performs:
 * <ul>
 * <li>Property loading with multi-level precedence: packaged properties &lt; profile-specific properties &lt; system properties &lt; environment variables</li>
 * <li>Direct JDBC connection establishment using resolved datasource credentials</li>
 * <li>Database schema upgrades via {@link DbVersionService} when not in initialization profiles</li>
 * <li>Dynamic entity form discovery by querying form and dynamic_entity tables</li>
 * <li>Runtime entity descriptor generation for Byte Buddy class compilation</li>
 * <li>Initialization profile support with CASCADE DROP of all public schema tables</li>
 * </ul>
 * </p>
 * <p>
 * Property resolution follows strict precedence hierarchy for datasource credentials:
 * <ol>
 * <li>Base defaults from application-openkoda.properties</li>
 * <li>Profile-specific overrides from application-{profile}.properties</li>
 * <li>System property overrides via -Dspring.datasource.url=...</li>
 * <li>Environment variable overrides via SPRING_DATASOURCE_URL (highest precedence)</li>
 * </ol>
 * This enables Docker/Kubernetes-style configuration through environment variables.
 * </p>
 * <p>
 * When SPRING_PROFILES_ACTIVE contains initialization profiles (e.g., drop_and_init_database), 
 * JDBCApp executes destructive DROP TABLE CASCADE operations on all public schema tables.
 * Use {@code --force} flag to bypass safety confirmation prompts in {@link DbVersionService}.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see DbVersionService
 * @see DynamicEntityRegistrationService
 * @see SpringProfilesHelper
 */
public class JDBCApp {
    
    /**
     * Maps Spring Boot property keys to their corresponding environment variable names for datasource configuration.
     * <p>
     * This mapping enables environment variable overrides of datasource properties following Docker/Kubernetes conventions:
     * <ul>
     * <li>{@code spring.datasource.url} → {@code SPRING_DATASOURCE_URL}</li>
     * <li>{@code spring.datasource.username} → {@code SPRING_DATASOURCE_USERNAME}</li>
     * <li>{@code spring.datasource.password} → {@code SPRING_DATASOURCE_PASSWORD}</li>
     * </ul>
     * </p>
     * <p>
     * Used by {@link #getProperty(Properties, String)} to resolve property values with environment variable precedence.
     * Allows containerized deployments to inject credentials via environment without modifying property files.
     * </p>
     *
     * @see #getProperty(Properties, String)
     */
    private static final Map<String, String> propertyAlternativesMap = Map.of(
            "spring.datasource.url","SPRING_DATASOURCE_URL",
            "spring.datasource.username", "SPRING_DATASOURCE_USERNAME",
            "spring.datasource.password", "SPRING_DATASOURCE_PASSWORD");

    /**
     * CLI entry point that discovers dynamic entities and performs database operations without full Spring context initialization.
     * <p>
     * Execution workflow follows this sequence:
     * <ol>
     * <li>Loads application-openkoda.properties as default property baseline</li>
     * <li>Merges active profile properties from SPRING_PROFILES_ACTIVE (e.g., application-openkoda.properties, application-local.properties)</li>
     * <li>Resolves datasource credentials (url/username/password) with system property and environment variable overrides</li>
     * <li>Establishes direct JDBC connection via {@link DriverManager}</li>
     * <li>For non-initialization profiles: Executes {@link DbVersionService} schema upgrades if applicable</li>
     * <li>Queries form and dynamic_entity tables via SQL JOIN to reconstruct {@link Form} model instances</li>
     * <li>Converts privilege strings to enum values using {@link PrivilegeHelper#valueOfString(String)}</li>
     * <li>Generates {@link com.openkoda.service.dynamicentity.DynamicEntityDescriptor} instances for runtime Byte Buddy class generation</li>
     * <li>For initialization profiles (drop_and_init_database): Executes DO block with DROP TABLE CASCADE on all public schema tables</li>
     * </ol>
     * </p>
     * <p>
     * Property loading precedence (highest to lowest):
     * <ul>
     * <li>Environment variables: SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD</li>
     * <li>System properties: -Dspring.datasource.url=..., -Dspring.datasource.username=..., -Dspring.datasource.password=...</li>
     * <li>Profile-specific properties: application-{profile}.properties files on classpath or filesystem</li>
     * <li>Default properties: application-openkoda.properties packaged in JAR</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Warning:</strong> Initialization profiles (e.g., drop_and_init_database) irreversibly delete all database tables.
     * The {@code --force} flag bypasses interactive confirmations in {@link DbVersionService} - use with caution in production.
     * </p>
     *
     * @param args Command-line arguments, supports {@code --force} flag to bypass user prompts in database operations
     * @throws ClassNotFoundException If JDBC driver class (PostgreSQL) cannot be loaded from classpath
     * @throws IOException If property files (application-openkoda.properties or profile-specific properties) cannot be read
     * @see DbVersionService#tryUpgade(Connection)
     * @see DynamicEntityRegistrationService#generateDynamicEntityDescriptors(java.util.List, long)
     * @see SpringProfilesHelper#isInitializationProfile()
     */
    public static void main(String[] args) throws ClassNotFoundException, IOException {
        boolean isForce = args != null && Arrays.stream(args).anyMatch(a -> "--force".equals(a));
        System.out.println("*********************************************************************");
        System.out.println(" " + Character.toString(0x1F50D) + " Look for dynamic entities");
        Properties defaultProps = new Properties();
        try(InputStream defaultAsStream = JDBCApp.class.getClassLoader().getResourceAsStream("application-openkoda.properties")) {
            defaultProps.load(defaultAsStream);
        }
        Properties appProps = new Properties(defaultProps);
        if(System.getProperty(SPRING_PROFILES_ACTIVE_PROP) != null) {
            String [] activeProfiles = System.getProperty(SPRING_PROFILES_ACTIVE_PROP).split(",");
            for(String activeProfile : activeProfiles) {
                try(InputStream configFromJarAsStream = JDBCApp.class.getClassLoader().getResourceAsStream(String.format("application-%s.properties", activeProfile))) {
                    if(configFromJarAsStream != null) {
                        System.out.println(" Load database properties for active profile " + activeProfile);
                        appProps.load(configFromJarAsStream);
                    } else {
                        System.out.println(" No config in jar for profile " + activeProfile);
                        try(InputStream configFromOutsideJarAsStream = new FileInputStream(String.format("./application-%s.properties", activeProfile))) {
                            System.out.println(" Load properties from outside jar for active profile " + activeProfile);
                            appProps.load(configFromOutsideJarAsStream);
                        } catch (FileNotFoundException e) {
                            System.out.println(" No properties outside jar for active profile " + activeProfile);
                        }
                    }
                }
            }
        } else {
            System.out.println(" Load database default properties");
        }

        String url = getProperty(appProps, "spring.datasource.url");
        String username = getProperty(appProps, "spring.datasource.username");
        String password = getProperty(appProps, "spring.datasource.password");
        
        String upgradeScript = getProperty(appProps, "upgrade.db.file");
        String currentDbVersionQuery = getProperty(appProps, "upgrade.db.current");
        String dbVersionInsertQuery = getProperty(appProps, "upgrade.db.insert");
        DbVersionService versionService = new DbVersionService(upgradeScript, currentDbVersionQuery, dbVersionInsertQuery, isForce);        
        
        try (Connection con = DriverManager
                .getConnection(
                        url,
                        username,
                        password)) {

            // use con here
            try (Statement stmt = con.createStatement()) {
              if(!SpringProfilesHelper.isInitializationProfile()) {
                  // try to perform db upgrades - if applicable    
                  versionService.tryUpgade(con);
                  
                  // use stmt here
                  String sql = """
                          select * from form as f
                          inner join dynamic_entity as de on de.table_name=f.table_name
                          """;
                  try (ResultSet resultSet = stmt.executeQuery(sql)) {
                      // use resultSet here
                      ArrayList<Form> forms = new ArrayList<Form>();
                      while (resultSet.next()) {
                          Form form = new Form();
                          form.setTableName(resultSet.getString("table_name"));
                          form.setName(resultSet.getString("name"));
                          if(isNotBlank(resultSet.getString("write_privilege"))) {
                              form.setWritePrivilege(PrivilegeHelper.valueOfString(resultSet.getString("write_privilege")));
                          }
                          if(isNotBlank(resultSet.getString("read_privilege"))) {
                              form.setReadPrivilege(PrivilegeHelper.valueOfString(resultSet.getString("read_privilege")));
                          }
                          form.setCode(resultSet.getString("code"));
                          forms.add(form);
                      }
                          System.out.println(" " + Character.toString(0x1F50D) + " Found " + forms.size() + " forms for dynamic entities");
                          int generatedEntities = DynamicEntityRegistrationService.generateDynamicEntityDescriptors(forms, System.currentTimeMillis());
                          System.out.println(" " + Character.toString(0x2705) + " Generated " + generatedEntities + " dynamic entity descriptions");
                      System.out.println("*********************************************************************");
                      }
              }
              else {
                  final String DROP_ALL_TABLES = """
                          DO $$ DECLARE
                              r RECORD;
                          BEGIN
                              FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
                                  EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE';
                              END LOOP;
                          END $$;
                          """;
                  stmt.execute(DROP_ALL_TABLES);
              }
          }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
    
    /**
     * Resolves property value with precedence hierarchy: properties file &lt; system property &lt; environment variable.
     * <p>
     * Property resolution follows this order (highest precedence wins):
     * <ol>
     * <li>Environment variable (via {@link System#getenv(String)}) if property key exists in {@link #propertyAlternativesMap}</li>
     * <li>System property (via {@link System#getProperty(String)}) if property key exists in {@link #propertyAlternativesMap}</li>
     * <li>Properties file value from merged application properties</li>
     * </ol>
     * </p>
     * <p>
     * For datasource properties (spring.datasource.url, spring.datasource.username, spring.datasource.password),
     * this method checks {@link #propertyAlternativesMap} for corresponding environment variable names
     * (SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD) and applies overrides if present.
     * This enables Docker/Kubernetes-style configuration where credentials are injected via environment.
     * </p>
     * <p>
     * Example resolution for {@code spring.datasource.url}:
     * <pre>
     * // Baseline: application-openkoda.properties contains spring.datasource.url=jdbc:postgresql://localhost:5432/openkoda
     * // Override 1: System.setProperty("spring.datasource.url", "jdbc:postgresql://db:5432/prod") takes precedence
     * // Override 2: System.getenv("SPRING_DATASOURCE_URL") = "jdbc:postgresql://prod-db:5432/openkoda" wins (highest)
     * </pre>
     * </p>
     * <p>
     * For properties not in {@link #propertyAlternativesMap}, returns the value from {@code appProps} without override checks.
     * </p>
     *
     * @param appProps Merged application properties from packaged and profile-specific property files
     * @param property Property key to resolve (e.g., {@code spring.datasource.url}, {@code upgrade.db.file})
     * @return Resolved property value with highest precedence applied, or {@code null} if property not found in any source
     * @see #propertyAlternativesMap
     */
    public static String getProperty(Properties appProps, String property) {
        String value = appProps.getProperty(property);
        if(propertyAlternativesMap.containsKey(property)) {
            if(StringUtils.isNoneBlank(System.getProperty(property))) {
                value = System.getProperty(property);
            }
            
            String envValue = System.getenv(propertyAlternativesMap.get(property));
            if(StringUtils.isNoneBlank(envValue)) {
                value = envValue;
            }
        }
        
        return value;
    }
}
