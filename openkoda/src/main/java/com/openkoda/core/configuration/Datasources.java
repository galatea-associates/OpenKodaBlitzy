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

package com.openkoda.core.configuration;

import com.zaxxer.hikari.HikariConfig;

import java.util.List;

/**
 * Spring ConfigurationProperties target class that binds multi-tenant datasource configurations from application.properties.
 * <p>
 * This POJO is automatically populated by Spring Boot via property binding using the "datasources" prefix.
 * Properties with prefix {@code datasources.list[i]} are mapped to the list field, enabling configuration
 * of multiple database connections for database-per-tenant or database-per-organization multi-tenancy scenarios.
 * </p>
 * <p>
 * Datasources are created at application startup and pooled via HikariCP for high-performance connection management.
 * Each tenant database configuration includes a unique name identifier and a HikariConfig object containing
 * JDBC connection parameters, pool sizing, and advanced Hikari settings.
 * </p>
 * <p>
 * Configuration example in application.properties:
 * <pre>
 * datasources.list[0].name=primary
 * datasources.list[0].config.jdbcUrl=jdbc:postgresql://localhost:5432/database
 * datasources.list[0].config.username=postgres
 * datasources.list[0].config.password=********
 * datasources.list[0].config.maximumPoolSize=10
 * ...
 * datasources.list[1].name=secondary_1
 * ...
 * datasources.list[2].name=secondary_2
 * </pre>
 * More config settings can be applied - see
 * <a href="https://github.com/brettwooldridge/HikariCP#gear-configuration-knobs-baby">HikariCP Configuration</a>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.zaxxer.hikari.HikariConfig
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 */
public class Datasources {

    /**
     * Represents a single tenant database configuration with name and HikariCP connection pool settings.
     * <p>
     * This nested class holds configuration for one datasource in a multi-tenant environment.
     * The {@code name} field provides a unique identifier for the datasource (e.g., "primary", "secondary_1"),
     * while the {@code config} field contains HikariCP connection pool parameters including JDBC URL,
     * credentials, pool sizing, connection timeout, and other performance tuning settings.
     * </p>
     * <p>
     * Binding pattern: Maps to {@code datasources.list[i].name} and {@code datasources.list[i].config.*}
     * properties in application.properties, where Spring Boot automatically instantiates TenantDB objects
     * and populates fields from the configuration hierarchy.
     * </p>
     *
     * @author OpenKoda Team
     * @since 1.7.1
     */
    public static class TenantDB {
        public String name;
        public HikariConfig config;

        /**
         * Gets the unique name identifier for this tenant database.
         *
         * @return the datasource name (e.g., 'primary', 'secondary_1')
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the unique name identifier for this tenant database.
         *
         * @param name the datasource name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets the HikariCP connection pool configuration for this datasource.
         *
         * @return the HikariConfig instance containing JDBC URL, credentials, pool size and other connection parameters
         * @see com.zaxxer.hikari.HikariConfig
         */
        public HikariConfig getConfig() {
            return config;
        }

        /**
         * Sets the HikariCP connection pool configuration for this datasource.
         *
         * @param config the HikariConfig instance to set
         */
        public void setConfig(HikariConfig config) {
            this.config = config;
        }
    }

    public List<TenantDB> list;

    /**
     * Gets the list of all configured tenant databases.
     *
     * @return list of TenantDB configurations, one per datasource definition in application.properties
     */
    public List<TenantDB> getList() {
        return list;
    }

    /**
     * Sets the list of tenant database configurations.
     *
     * @param list the list of TenantDB configurations to set
     */
    public void setList(List<TenantDB> list) {
        this.list = list;
    }
}
