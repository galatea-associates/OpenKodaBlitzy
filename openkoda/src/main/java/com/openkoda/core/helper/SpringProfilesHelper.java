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

package com.openkoda.core.helper;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for detecting active Spring profiles during application startup.
 * <p>
 * This helper detects active Spring profiles using a prioritized lookup strategy:
 * first checking system properties, then parsing the sun.java.command system property,
 * and finally checking environment variables. This approach is useful for early
 * application initialization before the Spring Environment is fully available.
 * </p>
 * <p>
 * The profile detection follows this priority order:
 * 1. System property: {@code spring.profiles.active}
 * 2. Command-line argument parsed from {@code sun.java.command}
 * 3. Environment variable: {@code SPRING_PROFILES_ACTIVE}
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * if (SpringProfilesHelper.isInitializationProfile()) {
 *     // Perform database initialization
 * }
 * }</pre>
 * </p>
 * <p>
 * Thread-safety: All methods are static and read from system properties or environment
 * variables. Safe for concurrent use, but results may vary during startup if profiles
 * are modified dynamically.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.springframework.core.env.Environment for standard Spring profile access
 */
public class SpringProfilesHelper {

    /**
     * Profile name for database initialization operations.
     * <p>
     * When this profile is active, the application performs database drop and
     * initialization operations. Use {@link #isInitializationProfile()} to check
     * if this profile is active.
     * </p>
     */
    public static final String INITIALIZATION_PROFILE = "drop_and_init_database";
    
    /**
     * Profile name for test execution environments.
     * <p>
     * This profile indicates the application is running in test mode. Use
     * {@link #isTestProfile()} to check if this profile is active.
     * </p>
     */
    public static final String TEST_PROFILE = "test";
    
    /**
     * System property name for active Spring profiles.
     * <p>
     * Standard Spring property key used to specify comma-separated active profiles.
     * Example: {@code -Dspring.profiles.active=openkoda,local}
     * </p>
     */
    public static final String SPRING_PROFILES_ACTIVE_PROP = "spring.profiles.active";
    
    /**
     * Environment variable name for active Spring profiles.
     * <p>
     * Standard environment variable used to specify comma-separated active profiles.
     * Example: {@code SPRING_PROFILES_ACTIVE=openkoda,production}
     * </p>
     */
    public static final String SPRING_PROFILES_ACTIVE_ENV = "SPRING_PROFILES_ACTIVE";

    private final static Pattern PROFILE_PATTERN = Pattern.compile(".*--spring\\.profiles\\.active=([\\w,]+) .*");
    
    /**
     * Checks if a specific Spring profile is currently active.
     * <p>
     * This method uses a three-stage detection priority to determine active profiles:
     * </p>
     * <ol>
     * <li>System property lookup: {@code spring.profiles.active}</li>
     * <li>Command-line parsing: extracts profiles from {@code sun.java.command}</li>
     * <li>Environment variable lookup: {@code SPRING_PROFILES_ACTIVE}</li>
     * </ol>
     * <p>
     * The method handles comma-separated profile lists and performs exact string
     * matching against the provided profile name.
     * </p>
     *
     * @param profile the profile name to check (e.g., "openkoda", "test", "production")
     * @return {@code true} if the specified profile is active, {@code false} otherwise
     */
    public static boolean isActiveProfile(String profile) {
        String profilesCommaSeparated = System.getProperty(SPRING_PROFILES_ACTIVE_PROP);
        if (profilesCommaSeparated == null) {
            Matcher m = PROFILE_PATTERN.matcher(System.getProperty("sun.java.command"));
            if (m.matches() ) {
                profilesCommaSeparated = m.group(1);
            }
        }

        if (profilesCommaSeparated == null) {
            profilesCommaSeparated = System.getenv(SPRING_PROFILES_ACTIVE_ENV);
        }

        if (profilesCommaSeparated == null) {
            return false;
        }
        
        return ArrayUtils.contains(StringUtils.split(profilesCommaSeparated, ','), profile);
    }

    /**
     * Checks if the database initialization profile is currently active.
     * <p>
     * This is a convenience method that checks for the {@code drop_and_init_database}
     * profile. When active, the application performs database drop and initialization
     * operations, typically used for development or testing environments.
     * </p>
     *
     * @return {@code true} if the {@code drop_and_init_database} profile is active,
     *         {@code false} otherwise
     * @see #INITIALIZATION_PROFILE
     */
    public static boolean isInitializationProfile() {
        return isActiveProfile(INITIALIZATION_PROFILE);
    }
    
    /**
     * Checks if the test profile is currently active.
     * <p>
     * This is a convenience method that checks for the {@code test} profile.
     * When active, the application runs in test mode, typically used during
     * automated testing or test execution environments.
     * </p>
     *
     * @return {@code true} if the {@code test} profile is active,
     *         {@code false} otherwise
     * @see #TEST_PROFILE
     */
    public static boolean isTestProfile() {
        return isActiveProfile(TEST_PROFILE);
    }
}
