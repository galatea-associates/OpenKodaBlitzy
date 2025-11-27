package com.openkoda.service.upgrade;

import com.openkoda.model.DbVersion;
import com.openkoda.repository.DbVersionRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Database schema migration orchestration service with version tracking and transactional execution.
 * <p>
 * This service executes database schema upgrades during OpenKoda bootstrap by parsing versioned SQL migration scripts,
 * tracking applied migrations in the {@code db_version} table, and ensuring idempotent execution with transactional safety.
 * Each migration is executed exactly once and persisted to prevent duplicate application across application restarts.
 * 
 * 
 * <b>Bootstrap DB Migration Workflow (7-step process)</b>
 * <ol>
 *   <li>Read application version from META-INF/MANIFEST.MF (Implementation-Version attribute)</li>
 *   <li>Query current database version from db_version table (highest successfully applied version)</li>
 *   <li>Load migration script from classpath resource (default: /migration/core_upgrade.sql)</li>
 *   <li>Parse version markers (-- @version: major.minor.build.revision) to identify migration blocks</li>
 *   <li>Filter migrations: exclude already-applied versions, include versions ≤ app version</li>
 *   <li>Execute each migration in sorted order within REQUIRES_NEW transaction boundary</li>
 *   <li>Persist migration result to db_version table (done=true on success, done=false with error note on failure)</li>
 * </ol>
 * 
 * <b>Version Marker Format</b>
 * <p>
 * Migration scripts use special comment markers to delimit version blocks:
 * 
 * <pre>
 * -- @version: 1.7.1.0
 * ALTER TABLE organization ADD COLUMN new_field VARCHAR(255);
 * 
 * -- @version: 1.7.2.0
 * CREATE INDEX idx_user_email ON user_account(email);
 * </pre>
 * <p>
 * Optional {@code -- @init} marker indicates migration should run during fresh database initialization.
 * 
 * 
 * <b>Transaction Boundaries</b>
 * <p>
 * Each migration version executes within its own transaction (REQUIRES_NEW isolation level).
 * On SQLException during migration execution:
 * 
 * <ul>
 *   <li>Transaction is rolled back automatically</li>
 *   <li>Migration marked as done=false with error message stored in note column</li>
 *   <li>Operator prompted to proceed or halt via {@link #proceedOnError(DbVersion)}</li>
 * </ul>
 * 
 * <b>proceedOnError Operator Behavior</b>
 * <p>
 * When migration fails, operator interaction determines next action:
 * 
 * <ul>
 *   <li><b>Interactive mode</b>: Prompts "Proceed with startup? (y/n)" via System.in</li>
 *   <li><b>Force mode</b>: Automatically proceeds (configured via isForce flag)</li>
 *   <li><b>Operator declines</b>: Calls System.exit(0) to halt application startup</li>
 * </ul>
 * 
 * <b>Migration Script Structure</b>
 * <p>
 * Default script location: {@code /migration/core_upgrade.sql} (configurable via upgrade.db.file property).
 * Script format requirements:
 * 
 * <ul>
 *   <li>Version markers must match pattern: {@code ^--\s*@version\s*:\s*(\d+)\.(\d+)\.(\d+)\.(\d+).*}</li>
 *   <li>SQL statements between version markers belong to that version's migration</li>
 *   <li>Comment lines (starting with --) are ignored except for version markers</li>
 *   <li>Blank lines are ignored</li>
 * </ul>
 * 
 * <b>db_version Table Schema (5 key columns)</b>
 * <p>
 * Migration execution history persisted to {@code public.db_version} table:
 * 
 * <table border="1">
 *   <caption>Database Version Table Schema</caption>
 *   <tr><th>Column</th><th>Type</th><th>Description</th></tr>
 *   <tr><td>major</td><td>int4</td><td>Major version number (e.g., 1 in 1.7.1.0)</td></tr>
 *   <tr><td>minor</td><td>int4</td><td>Minor version number (e.g., 7 in 1.7.1.0)</td></tr>
 *   <tr><td>build</td><td>int4</td><td>Build version number (e.g., 1 in 1.7.1.0)</td></tr>
 *   <tr><td>revision</td><td>int4</td><td>Revision number (e.g., 0 in 1.7.1.0)</td></tr>
 *   <tr><td>done</td><td>bool</td><td>Success indicator: true if migration applied successfully, false if failed</td></tr>
 *   <tr><td>note</td><td>varchar(255)</td><td>Error message if done=false, null otherwise</td></tr>
 * </table>
 * <p>
 * Table created automatically via {@link #DV_VERSION_DDL} if not exists during first migration.
 * 
 * 
 * <b>Version Comparison Logic (Semantic Versioning)</b>
 * <p>
 * Versions compared using numeric hash: {@code major * 10000000 + minor * 100000 + build * 100 + revision}.
 * This ensures correct ordering: 1.7.1.0 &lt; 1.7.2.0 &lt; 1.8.0.0 &lt; 2.0.0.0.
 * Implemented via {@link DbVersion#hashCode()} and {@link DbVersion#compareTo(DbVersion)}.
 * 
 * 
 * <b>Usage Example - Bootstrap Migration</b>
 * <pre>
 * DbVersionService service = new DbVersionService();
 * Connection con = dataSource.getConnection();
 * service.tryUpgade(con); // Executes all pending migrations
 * </pre>
 * 
 * <b>Usage Example - SQL Migration Script</b>
 * <pre>
 * -- @version: 1.7.1.0
 * -- @init
 * CREATE TABLE example (id BIGINT PRIMARY KEY);
 * 
 * -- @version: 1.7.2.0
 * ALTER TABLE example ADD COLUMN name VARCHAR(255);
 * </pre>
 * 
 * <b>Dependencies and Thread-Safety</b>
 * <p>
 * Dependencies: {@link DbVersionRepository} for JPA-based version queries, Spring @Value for configuration injection.
 * Thread-safety: Service is NOT thread-safe. Migrations executed serially during single-threaded application bootstrap.
 * Concurrent execution would cause duplicate migration attempts and transaction conflicts.
 * 
 * 
 * <b>Comparison with Flyway</b>
 * <p>
 * Unlike Flyway, this service provides interactive failure handling via proceedOnError operator prompts.
 * Flyway enforces strict migration immutability; this service allows operator discretion to proceed after failures.
 * Both track applied migrations in dedicated version table (Flyway: flyway_schema_history, OpenKoda: db_version).
 * 
 * 
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.0.0
 * @see DbVersion
 * @see DbVersionRepository
 * @see com.openkoda.JDBCApp
 */
@Service
public class DbVersionService {

    /**
     * Default classpath location for core database migration script.
     * Contains versioned SQL blocks with {@code -- @version} markers.
     * Configurable via {@code upgrade.db.file} property.
     */
    static final String MIGRATION_UPGRADE_SQL = "/migration/core_upgrade.sql";
    /**
     * SQL query template to retrieve the latest successfully applied database version.
     * Returns single row with highest version_numeric where done=true.
     * Used by {@link #getCurrentVersion(Connection)} to determine migration starting point.
     * Configurable via {@code upgrade.db.current} property.
     */
    static final String FIND_CURRENT = """
            SELECT v.major, v.minor, v.build, v.revision, v.done, v.note, (v.major * 10000000 + v.minor * 100000 + v.build * 100 + v.revision) AS "version_numeric"
            FROM db_version v 
            WHERE v.done = true 
            ORDER BY version_numeric DESC
            LIMIT 1
            """;
    /**
     * SQL query template to retrieve all successfully applied versions older than specified version.
     * Placeholder $1 replaced with target version's hashCode via {@link #getFindInstalledQuery(DbVersion)}.
     * Returns rows in ascending version order (oldest first).
     * Used by {@link #findAllInstalled(Connection, DbVersion)} to prevent duplicate execution.
     * Configurable via {@code upgrade.db.installed} property.
     */
    static final String FIND_OLDER_INSTALLED = """
            SELECT v.major, v.minor, v.build, v.revision, v.done, v.note, (v.major * 10000000 + v.minor * 100000 + v.build * 100 + v.revision) AS "version_numeric"
            FROM db_version v 
            WHERE v.done = true AND (v.major * 10000000 + v.minor * 100000 + v.build * 100 + v.revision) < $1
            ORDER BY version_numeric ASC
            """;
    
    /**
     * Prepared statement template for inserting migration execution results into db_version table.
     * Uses seq_global_id sequence for primary key generation.
     * Binds 6 parameters: major, minor, build, revision (integers), done (boolean), note (string).
     * Called by {@link #prepareInsert(DbVersion, Connection)} after each migration attempt.
     * Configurable via {@code upgrade.db.insert} property.
     */
    static final String VERSION_INSERT = """
            INSERT INTO public.db_version
                (id, created_by, created_by_id, created_on, modified_by, modified_by_id, updated_on,  major, minor, build, revision, done,note)
            VALUES(nextval('seq_global_id'), '', 0, CURRENT_TIMESTAMP, '', 0, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?);
            """;
    
    /**
     * DDL statement to create db_version table if not exists.
     * Executed during first migration attempt when {@link #getCurrentVersion(Connection)} returns null.
     * Table structure includes audit columns (created_by, created_on, updated_on) and version tracking columns
     * (major, minor, build, revision, done, note).
     * Idempotent operation safe for repeated execution via CREATE TABLE IF NOT EXISTS.
     */
    static final String DV_VERSION_DDL = """
                CREATE TABLE IF NOT EXISTS public.db_version (
                    id int8 NOT NULL,
                    created_by varchar(255) NULL,
                    created_by_id int8 NULL,
                    created_on timestamptz NULL DEFAULT CURRENT_TIMESTAMP,
                    modified_by varchar(255) NULL,
                    modified_by_id int8 NULL,
                    updated_on timestamptz NULL DEFAULT CURRENT_TIMESTAMP,
                    build int4 NOT NULL,
                    done bool NULL,
                    major int4 NOT NULL,
                    minor int4 NOT NULL,
                    note varchar(255) NULL,
                    revision int4 NOT NULL,
                    CONSTRAINT db_version_pkey PRIMARY KEY (id)
                );
            """;

    /**
     * Regular expression for parsing application version from MANIFEST.MF Implementation-Version.
     * Captures major.minor.build with optional additional characters (e.g., "1.7.1-SNAPSHOT").
     * Used by {@link #getAppVersion()} to extract numeric version components.
     */
    private static final String APP_VERSION_REGEXP = "(\\d+)\\.(\\d+)\\.(\\d+).*";
    
    /**
     * Regular expression for parsing version markers in migration script comments.
     * Matches pattern: {@code -- @version: 1.7.1.0} with optional whitespace and trailing content.
     * Captures four numeric groups: major, minor, build, revision.
     * Used by {@link #loadUpgradeSteps()} to delimit migration blocks.
     */
    private static final String VERSION_REGEXP = "^--\\s*@version\\s*:\\s*(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+).*";
    
    /**
     * Regular expression for detecting initialization marker in migration scripts.
     * Matches pattern: {@code -- @init} with optional trailing content.
     * Indicates migration should execute during fresh database initialization (when getCurrentVersion returns null).
     * Used by {@link #loadUpgradeSteps()} to set DbVersion.runOnInit flag.
     */
    private static final String RUN_ON_INIT_REGEXP = "^--\\s*@init.*";
    
    /** Compiled pattern for {@link #APP_VERSION_REGEXP} used by {@link #getAppVersion()}. */
    private static final Pattern APP_VERSION_PATTERN = Pattern.compile(APP_VERSION_REGEXP);
    
    /** Compiled pattern for {@link #VERSION_REGEXP} used by {@link #loadUpgradeSteps()}. */
    private static final Pattern VERSION_PATTERN = Pattern.compile(VERSION_REGEXP);
    
    /** Compiled pattern for {@link #RUN_ON_INIT_REGEXP} used by {@link #loadUpgradeSteps()}. */
    private static final Pattern RUN_ON_INIT_PATTERN = Pattern.compile(RUN_ON_INIT_REGEXP);
    
    /**
     * JPA repository for querying db_version table via Spring Data.
     * Used by {@link #getCurrentVersion()} for repository-based version retrieval.
     * Injected via constructor or field injection.
     */
    private DbVersionRepository repository;

    /**
     * Classpath location of migration script file.
     * Defaults to {@link #MIGRATION_UPGRADE_SQL}.
     * Configurable via {@code upgrade.db.file} application property.
     * Script must contain version markers matching {@link #VERSION_REGEXP}.
     */
    @Value("${upgrade.db.file:" + MIGRATION_UPGRADE_SQL +"}")
    private String upgradeScript = MIGRATION_UPGRADE_SQL;
    
    /**
     * SQL query for retrieving current database version.
     * Defaults to {@link #FIND_CURRENT}.
     * Configurable via {@code upgrade.db.current} application property.
     * Must return columns: major, minor, build, revision, done, note, version_numeric.
     */
    @Value("${upgrade.db.current:" + FIND_CURRENT + "}")
    private String currentVersionQuery = FIND_CURRENT;

    /**
     * SQL query template for retrieving all installed versions older than specified version.
     * Defaults to {@link #FIND_OLDER_INSTALLED}.
     * Configurable via {@code upgrade.db.installed} application property.
     * Contains $1 placeholder replaced by version hashCode.
     */
    @Value("${upgrade.db.installed:" + FIND_OLDER_INSTALLED + "}")
    private String allInstalledQuery = FIND_OLDER_INSTALLED;
    
    /**
     * Prepared statement template for inserting migration results.
     * Defaults to {@link #VERSION_INSERT}.
     * Configurable via {@code upgrade.db.insert} application property.
     * Must bind parameters: major, minor, build, revision, done, note.
     */
    @Value("${upgrade.db.insert:" + VERSION_INSERT + "}")
    private String dbVersionInsertQuery = VERSION_INSERT;
    
    /**
     * Force mode flag for automated migration failure handling.
     * When true, {@link #proceedOnError(DbVersion)} automatically proceeds without operator prompt.
     * When false, prompts operator via System.in for proceed decision.
     * Set via constructor for JDBCApp CLI usage.
     */
    private boolean isForce = false;
    
    /**
     * Constructor for CLI and test usage with custom configuration.
     * Allows override of migration script location, SQL queries, and force mode.
     * Used by {@link com.openkoda.JDBCApp} for bootstrap migration execution.
     * 
     * @param upgradeScript Classpath location of migration script, defaults to {@link #MIGRATION_UPGRADE_SQL} if blank
     * @param currentVersionQuery SQL query for current version, defaults to {@link #FIND_CURRENT} if blank
     * @param dbVersionInsertQuery SQL insert statement, defaults to {@link #VERSION_INSERT} if blank
     * @param isForce Force mode flag (true to skip operator prompts on failure)
     */
    public DbVersionService(String upgradeScript, String currentVersionQuery,
            String dbVersionInsertQuery, boolean isForce) {
        this.upgradeScript = StringUtils.defaultIfBlank(upgradeScript, MIGRATION_UPGRADE_SQL);
        this.currentVersionQuery = StringUtils.defaultIfBlank(currentVersionQuery, FIND_CURRENT);
        this.dbVersionInsertQuery = StringUtils.defaultIfBlank(dbVersionInsertQuery, VERSION_INSERT);
        this.isForce = isForce;
    }

    /**
     * Default constructor for Spring dependency injection.
     * Configuration values injected via @Value annotations from application properties.
     * Repository injected via @Autowired field injection.
     * Force mode defaults to false (interactive operator prompts enabled).
     */
    @Autowired
    public DbVersionService() {
    }

    /**
     * Retrieves current database version via JPA repository.
     * Delegates to {@link DbVersionRepository#findCurrentDbVersion()} for repository-based query.
     * 
     * @return Latest successfully applied DbVersion from db_version table, or null if no migrations applied
     */
    public DbVersion getCurrentVersion() {
        return repository.findCurrentDbVersion();
    }
    
    /**
     * Retrieves latest successfully applied database version via JDBC connection.
     * Queries db_version table ordered by version_numeric DESC with LIMIT 1.
     * Returns null if db_version table does not exist or is empty (fresh database).
     * 
     * @param con JDBC connection for querying db_version table (must be open and valid)
     * @return Latest successfully applied DbVersion where done=true, or null if no successful migrations exist
     * @throws SQLException if query execution fails or connection is invalid
     */
    public DbVersion getCurrentVersion(Connection con) throws SQLException {
        try(Statement stmt = con.createStatement()) {
            try (ResultSet resultSet = stmt.executeQuery(currentVersionQuery)) {
                // use resultSet here
                if(resultSet.next()) {
                    DbVersion current = new DbVersion();
                    current.setMajor(resultSet.getInt("major"));
                    current.setMinor(resultSet.getInt("minor"));
                    current.setBuild(resultSet.getInt("build"));
                    current.setRevision(resultSet.getInt("revision"));
                    current.setDone(resultSet.getBoolean("done"));
                    current.setNote(resultSet.getString("note"));
                    return current;
                }
                
                return null;
            } catch (SQLException sqle) {
                System.out.println(sqle.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Retrieves all successfully applied database versions older than specified application version.
     * Used by {@link #runUpgrade} to filter out already-executed migrations and prevent duplicate application.
     * Query returns rows in ascending version order (oldest first).
     * 
     * @param con Database connection for querying version history (must be open and valid)
     * @param appVersion Application version acting as upper bound filter (versions &gt;= appVersion excluded from results)
     * @return List of all successfully applied DbVersion objects where done=true and version &lt; appVersion, ordered by version number ascending, empty list if none found
     * @throws SQLException if query execution fails or connection is invalid
     */
    public List<DbVersion> findAllInstalled(Connection con, DbVersion appVersion) throws SQLException {
        List<DbVersion> installedVersion = new ArrayList<>();
        try(Statement stmt = con.createStatement()) {
            try (ResultSet resultSet = stmt.executeQuery(getFindInstalledQuery(appVersion))) {
                // use resultSet here
                while(resultSet.next()) {
                    DbVersion current = new DbVersion();
                    current.setMajor(resultSet.getInt("major"));
                    current.setMinor(resultSet.getInt("minor"));
                    current.setBuild(resultSet.getInt("build"));
                    current.setRevision(resultSet.getInt("revision"));
                    current.setDone(resultSet.getBoolean("done"));
                    current.setNote(resultSet.getString("note"));
                    installedVersion.add(current);
                }
            } catch (SQLException sqle) {
                System.out.println(sqle.getMessage());
            }
        }
        
        return installedVersion;
    }

    /**
     * Generates SQL query for finding installed versions by replacing placeholder with version hash.
     * Substitutes $1 placeholder in {@link #allInstalledQuery} template with appVersion.hashCode().
     * Version hashCode computed as: major * 10000000 + minor * 100000 + build * 100 + revision.
     * 
     * @param appVersion Version used to generate numeric hash for query substitution (upper bound filter)
     * @return SQL query string with $1 placeholder replaced by appVersion.hashCode() integer value
     */
    public String getFindInstalledQuery(DbVersion appVersion) {
        return allInstalledQuery.replace("$1", Integer.toString(appVersion.hashCode()));
    }
    
    /**
     * Executes filtered and sorted database migrations within transactional boundaries.
     * Filters migrations to exclude already-applied versions (via allInstalled comparison) and versions exceeding appVersion.
     * Sorts remaining migrations in ascending version order before sequential execution.
     * Each migration executes within auto-commit disabled transaction; manual commit via {@link #prepareInsert} after execution.
     * 
     * <p>Migration execution logic:</p>
     * <ul>
     *   <li>If currentVersion is null (fresh DB) and migration has runOnInit=true: execute migration</li>
     *   <li>If currentVersion is null and runOnInit=false: skip migration, mark done=true</li>
     *   <li>If currentVersion exists: execute all non-installed migrations</li>
     *   <li>On SQLException: rollback transaction, mark done=false with error note, invoke {@link #proceedOnError}</li>
     * </ul>
     * 
     * @param appVersion Application version from MANIFEST.MF acting as upper bound filter (migrations > appVersion skipped)
     * @param currentVersion Latest successfully applied version from db_version table, null if fresh database initialization
     * @param allInstalled List of all previously applied versions to prevent duplicate execution
     * @param upgradeScriptsMap Map of DbVersion to SQL script content for each migration block
     * @param con JDBC connection with auto-commit disabled for transactional execution
     * @return true if all migrations succeeded, false if any migration failed (operator chose to proceed after failure)
     * @throws SQLException if connection operations fail outside migration execution (filtering/sorting errors)
     */
    public boolean runUpgrade(DbVersion appVersion, DbVersion currentVersion, List<DbVersion> allInstalled,
            Map<DbVersion, String> upgradeScriptsMap, Connection con) throws SQLException {
        con.setAutoCommit(false);
        List<Map.Entry<DbVersion, String>> versionsToRun = upgradeScriptsMap.entrySet().stream()
                .filter( ev -> ev.getKey() != null && (currentVersion == null 
                                    || currentVersion != null 
                                        && allInstalled.stream().noneMatch( ai -> ai.hashCode() == ev.getKey().hashCode())))
                .filter( ev -> ev.getKey() != null && appVersion != null && ev.getKey().compareTo(appVersion) <= 0)
                .sorted( (v1, v2) -> v1.getKey().compareTo(v2.getKey())) .toList();
        for (Entry<DbVersion, String> ev : versionsToRun) {
            if(currentVersion != null || currentVersion == null && ev.getKey().isRunOnInit()) {
                if(currentVersion != null && currentVersion.compareTo(ev.getKey()) > 0) {
                    System.out.printf("%s Upgrading to %s as it was probably not yet executed %n", Character.toString(0x1F5C0), ev.getKey());
                } else {
                    System.out.printf("%s Upgrading to %s%n", Character.toString(0x1F5C0), ev.getKey());
                }
                try(Statement stmt = con.createStatement()) {
                    ev.getKey().setDone(true);
                    stmt.execute(ev.getValue().trim());
                } catch (SQLException e) {
                    System.out.printf("%s Upgrade failed due to : %s%n", Character.toString(0x1F5C0), e.getMessage());
                    ev.getKey().setDone(false);
                    ev.getKey().setNote(e.getMessage());
                    try {
                        con.rollback();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
            } else {
                System.out.printf("%s Skipping due to initialization to %s%n", Character.toString(0x1F5C0), ev.getKey());
                ev.getKey().setDone(true);
            }
            
            prepareInsert(ev.getKey(), con);
            if(Boolean.FALSE.equals(ev.getKey().getDone())) {
                proceedOnError(ev.getKey());
                return false;
            }
        }

        return true;
    }
    
    /**
     * Handles migration failure by prompting operator to proceed or halt application startup.
     * Interactive mode prompts "Proceed with startup? (y/n)" via System.in; non-'y' response calls System.exit(0).
     * Force mode (isForce=true) automatically proceeds without prompt for automated deployments.
     * 
     * <p>Operator decision outcomes:</p>
     * <ul>
     *   <li>Force mode: Automatically proceeds, prints "Force mode, assuming yes"</li>
     *   <li>Interactive 'y' response: Proceeds with startup despite failed migration</li>
     *   <li>Interactive non-'y' response: Prints "Unfinished upgrade, stopping", calls System.exit(0)</li>
     * </ul>
     * 
     * @param dbVersion Failed migration version requiring operator decision (displays version in prompt)
     */
    private void proceedOnError(DbVersion dbVersion) {
        System.out.println("*********************************************************************");
        System.out.printf("%s Could not upgrade to %s and further versions %n", Character.toString(0x1F5C0), dbVersion);
        System.out.printf("%s Proceed with startup? (y/n) ", Character.toString(0x1F5C0));
        if(isForce) {
            System.out.println(" Force mode, assuming yes");
        } else {                    
            int c;
            try {
                c = System.in.read();
                if (c != 'y') {
                    System.out.println(Character.toString(0x1F5C0) + " Unfinished upgrade, stopping");
                    System.exit(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("*********************************************************************");
    }
    
    /**
     * Persists migration execution result to db_version table and commits transaction.
     * Binds DbVersion properties (major, minor, build, revision, done, note) to {@link #dbVersionInsertQuery} prepared statement.
     * Uses seq_global_id sequence for primary key generation.
     * Commits transaction immediately after insert to persist migration status.
     * 
     * @param ver DbVersion to persist in db_version table (done=true on success, done=false with error note on failure)
     * @param con Database connection for insert and commit operations
     * @throws RuntimeException wrapping SQLException if insert execution or commit fails
     */
    private void prepareInsert(DbVersion ver, Connection con) {
        try(PreparedStatement stmt = con.prepareStatement(dbVersionInsertQuery)) {
            stmt.setInt(1, ver.getMajor());
            stmt.setInt(2, ver.getMinor());
            stmt.setInt(3, ver.getBuild());
            stmt.setInt(4, ver.getRevision());
            stmt.setBoolean(5, ver.getDone());
            stmt.setString(6, ver.getNote());
            stmt.execute();
            con.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads and parses migration script from classpath, extracting version blocks delimited by version markers.
     * Reads {@link #upgradeScript} file line-by-line, detecting version markers matching {@link #VERSION_REGEXP}.
     * SQL statements between version markers accumulated as migration content for that version.
     * Comment lines (starting with --) ignored except for version markers and @init markers.
     * Blank lines ignored.
     * 
     * <p>Version marker format: {@code -- @version: major.minor.build.revision}</p>
     * <p>Optional init marker: {@code -- @init} (must appear after version marker, sets DbVersion.runOnInit=true)</p>
     * 
     * <p>Example migration script structure:</p>
     * <pre>
     * -- @version: 1.7.1.0
     * -- @init
     * CREATE TABLE example (id BIGINT);
     * 
     * -- @version: 1.7.2.0
     * ALTER TABLE example ADD COLUMN name VARCHAR(255);
     * </pre>
     * 
     * @return Map of DbVersion to SQL script content for each migration block, empty map if script file not found
     * @throws IOException if upgrade script file cannot be read from classpath resource
     */
    public Map<DbVersion, String> loadUpgradeSteps() throws IOException {
        Resource upgradeSql = new ClassPathResource(upgradeScript);
        Map<DbVersion, String> versionScripts = new HashMap<>();
        if(upgradeSql.exists()) {
            InputStreamReader streamReader = new InputStreamReader(upgradeSql.getInputStream(), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader);
            Matcher m = null;
            DbVersion version = null;
            StringBuilder currentVersionScript = new StringBuilder();
            for (String line; (line = reader.readLine()) != null;) {
                if(line.startsWith("--") && (m = VERSION_PATTERN.matcher(line)).matches()) {
                    // finish current version and prepre lines for a next one
                    if (version != null) {
                        versionScripts.put(version, currentVersionScript.toString());
                        currentVersionScript = new StringBuilder();
                    }
                    
                    Integer major = Integer.valueOf(m.group(1));
                    Integer minor = Integer.valueOf(m.group(2));
                    Integer build = Integer.valueOf(m.group(3));
                    Integer revision = Integer.valueOf(m.group(4));
                    version = new DbVersion(major, minor, build, revision);
                } else if(line.startsWith("--") && (m = RUN_ON_INIT_PATTERN.matcher(line)).matches()) {
                    version.setRunOnInit(true);
                } else if(!line.startsWith("--") && StringUtils.isNotBlank(line)){
                    currentVersionScript.append(line).append("\n");
                }
            }
            
            if (version != null) {
                versionScripts.put(version, currentVersionScript.toString());
            }
        }
        
        return versionScripts;
    }
    
    /**
     * Extracts application version from META-INF/MANIFEST.MF Implementation-Version attribute.
     * Searches all MANIFEST.MF resources for entry with Implementation-Title="openkoda".
     * Parses Implementation-Version using {@link #APP_VERSION_PATTERN} to extract major.minor.build components.
     * Revision component hardcoded to 99 (ignored in app version comparisons).
     * 
     * <p>Fallback behavior: Returns version 0.0.0.99 if manifest not found or parsing fails.</p>
     * 
     * @return Application version from META-INF/MANIFEST.MF Implementation-Version, or DbVersion(0, 0, 0, 99) if not found
     * @throws IOException if manifest resource cannot be read (error printed to System.err, returns fallback version)
     */
    DbVersion getAppVersion() throws IOException {
        String appVersionString = "0.0.0";
        DbVersion appVersion =  new DbVersion(0, 0, 0, 99);
        Enumeration<URL> resources = getClass().getClassLoader()
                .getResources("META-INF/MANIFEST.MF");
              while (resources.hasMoreElements()) {
                  try {
                    Manifest manifest = new Manifest(resources.nextElement().openStream());
                    if("openkoda".equals(manifest.getMainAttributes().getValue("Implementation-Title"))){
                        appVersionString = manifest.getMainAttributes().getValue("Implementation-Version");
                        Matcher m = APP_VERSION_PATTERN.matcher(appVersionString);
                        if(m.matches()) {
                            appVersion.setMajor(Integer.parseInt(m.group(1)));
                            appVersion.setMinor(Integer.parseInt(m.group(2)));
                            appVersion.setBuild(Integer.parseInt(m.group(3)));
                        }
                        
                        break;
                    }
                  } catch (IOException E) {
                    // handle
                      E.printStackTrace();
                  }
              }
          return appVersion;
    }

    /**
     * Orchestrates complete database migration workflow during application bootstrap.
     * Executes 7-step migration process: read app version, query current DB version, find all installed versions,
     * load migration scripts, filter/sort migrations, execute pending migrations, persist results.
     * Creates db_version table if not exists (fresh database initialization).
     * Prints migration progress to System.out with Unicode icon prefix (0x1F5C0).
     * 
     * <p>Execution flow:</p>
     * <ol>
     *   <li>Extract application version from MANIFEST.MF via {@link #getAppVersion()}</li>
     *   <li>Query installed versions via {@link #findAllInstalled(Connection, DbVersion)}</li>
     *   <li>Query current version via {@link #getCurrentVersion(Connection)}</li>
     *   <li>Print current app version and DB model version to System.out</li>
     *   <li>Load migration scripts via {@link #loadUpgradeSteps()}</li>
     *   <li>If currentVersion is null: execute {@link #DV_VERSION_DDL} to create db_version table</li>
     *   <li>Execute migrations via {@link #runUpgrade}</li>
     * </ol>
     * 
     * @param con JDBC connection for DDL execution and migration queries (must be open and valid, auto-commit managed by runUpgrade)
     * @throws SQLException if database operations fail (connection errors, query execution failures, DDL failures)
     * @throws IOException if migration script file cannot be read from classpath or MANIFEST.MF is inaccessible
     */
    public void tryUpgade(Connection con) throws SQLException, IOException {
        // revision is ignored when comparing app version
        DbVersion appVersion = getAppVersion();
        
        List<DbVersion> allInstalled = findAllInstalled(con, appVersion);
        DbVersion currentVersion = getCurrentVersion(con);
        System.out.println(Character.toString(0x1F5C0) + " Current app version      : " + appVersion);
        System.out.println(Character.toString(0x1F5C0) + " Current DB model version : " + currentVersion);        
        Map<DbVersion, String> upgradeScriptsMap = loadUpgradeSteps();
        if(currentVersion == null) {
            System.out.println(Character.toString(0x1F5C0) + " Initializing db model versions");
            try(Statement stmt = con.createStatement()) {
                stmt.execute(DV_VERSION_DDL);
            }
        }

        runUpgrade(appVersion, currentVersion, allInstalled, upgradeScriptsMap, con);
    }
}
