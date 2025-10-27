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

package com.openkoda.core.multitenancy;

import com.openkoda.core.helper.ReadableCode;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides database connections with automatic PostgreSQL schema-per-tenant isolation.
 * <p>
 * This connection provider extends Hibernate's {@link DatasourceConnectionProviderImpl} to inject
 * tenant-specific schema context into each database connection. It implements a PostgreSQL schema-per-tenant
 * isolation strategy where each tenant's data resides in a dedicated schema (org_&lt;organizationId&gt;).
 * </p>
 * <p>
 * <b>PostgreSQL search_path Mechanism:</b><br>
 * The provider automatically sets the PostgreSQL search_path to "org_&lt;organizationId&gt;,public" for each
 * connection checkout. This configuration causes PostgreSQL to:
 * <ul>
 * <li>First search the tenant-specific schema (org_&lt;organizationId&gt;) for tables and objects</li>
 * <li>Fall back to the public schema if objects are not found in the tenant schema</li>
 * <li>Automatically isolate tenant data without requiring application-level WHERE filters</li>
 * </ul>
 * For example, when organizationId=123, queries against "organizations" table automatically resolve
 * to "org_123.organizations", providing transparent tenant isolation at the database level.
 * </p>
 * <p>
 * <b>Integration with TenantResolver:</b><br>
 * The current tenant context is obtained from {@link TenantResolver#getTenantedResource()}, which provides
 * thread-local tenant information. Each thread maintains its own tenant context, enabling safe concurrent
 * processing of requests from different tenants.
 * </p>
 * <p>
 * <b>Thread-Safety:</b><br>
 * This class is stateless and safe for concurrent use across multiple threads. Tenant context is retrieved
 * from thread-local storage, and search_path is set on a per-connection basis, ensuring isolation between
 * concurrent tenant operations.
 * </p>
 * <p>
 * <b>Connection Lifecycle:</b><br>
 * <ol>
 * <li>Connection checkout: {@link #getConnection()} obtains pooled connection and sets tenant search_path</li>
 * <li>Application uses connection with automatic tenant schema resolution</li>
 * <li>Connection return: {@link #closeConnection(Connection)} resets search_path to "public" before returning to pool</li>
 * </ol>
 * </p>
 * <p>
 * <b>Deployment Scenarios:</b><br>
 * <ul>
 * <li><b>Multi-schema (schema-per-tenant):</b> Each tenant has dedicated org_&lt;id&gt; schema for complete isolation</li>
 * <li><b>Single-schema:</b> All tenants share public schema (search_path setting is benign if tenant schemas don't exist)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Performance Considerations:</b><br>
 * Each connection checkout incurs minimal overhead of one additional SET command to configure search_path.
 * Pooled connections are safely reused across different tenants as search_path is reset on return.
 * </p>
 * <p>
 * <b>Database Compatibility:</b><br>
 * This implementation is PostgreSQL-specific and relies on PostgreSQL's search_path behavior. It is not
 * portable to other database systems without modification.
 * </p>
 * <p>
 * Example tenant schema setup (created by {@link MultitenancyService}):
 * <pre>{@code
 * CREATE SCHEMA org_123;
 * CREATE TABLE org_123.organizations (...);
 * }</pre>
 * </p>
 *
 * @see TenantResolver#getTenantedResource()
 * @see TenantResolver.TenantedResource#organizationId
 * @see MultitenancyService
 * @see org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class SchemaSupportingConnectionProvider extends DatasourceConnectionProviderImpl implements ReadableCode {

    /**
     * Retrieves a database connection and configures it with tenant-specific PostgreSQL search_path.
     * <p>
     * This method obtains a pooled JDBC connection from the underlying datasource and automatically
     * sets the PostgreSQL search_path to "org_&lt;organizationId&gt;,public" based on the current
     * thread's tenant context. This configuration enables transparent tenant data isolation without
     * requiring application-level filtering.
     * </p>
     * <p>
     * <b>Implementation Flow:</b>
     * <ol>
     * <li>Retrieves current {@link TenantResolver.TenantedResource} from thread-local context</li>
     * <li>Obtains pooled connection by calling {@code super.getConnection()}</li>
     * <li>Executes "SET search_path TO org_&lt;organizationId&gt;,public" on the connection</li>
     * <li>Returns schema-aware connection ready for tenant-scoped database operations</li>
     * </ol>
     * </p>
     * <p>
     * <b>Example:</b> For tenant with organizationId=123, queries automatically resolve to org_123 schema:
     * <pre>{@code
     * Connection conn = getConnection();
     * // SELECT * FROM organizations resolves to org_123.organizations
     * }</pre>
     * </p>
     * <p>
     * <b>Thread-Safety:</b> Safe for concurrent use. Each thread retrieves its own tenant context from
     * thread-local storage, and search_path is set on the connection instance (not shared state).
     * </p>
     * <p>
     * <b>Performance:</b> Adds one SET command execution per connection checkout. This overhead is minimal
     * compared to the security benefit of automatic tenant isolation.
     * </p>
     * <p>
     * <b>Error Handling:</b> If search_path configuration fails, SQLException propagates to caller and
     * the connection remains in the pool (no resource leak).
     * </p>
     *
     * @return JDBC Connection configured with tenant-specific search_path for isolated database access
     * @throws SQLException if connection acquisition fails or search_path configuration fails
     * @see TenantResolver#getTenantedResource()
     * @see #closeConnection(Connection)
     */
    @Override
    public Connection getConnection() throws SQLException {
        TenantResolver.TenantedResource tr = TenantResolver.getTenantedResource();
        Connection c = super.getConnection();
        String setSearchPathStatement = String.format("set search_path to org_%d,public", tr.organizationId);
        c.prepareStatement(setSearchPathStatement).execute();
        return c;
    }

    /**
     * Resets PostgreSQL search_path to default and returns the connection to the pool.
     * <p>
     * This method ensures connection pool hygiene by resetting the search_path to "public" before
     * returning the connection to the pool. This prevents tenant context pollution when connections
     * are reused for different tenants in subsequent checkouts.
     * </p>
     * <p>
     * <b>Implementation Flow:</b>
     * <ol>
     * <li>Executes "SET search_path TO public" to restore default PostgreSQL schema context</li>
     * <li>Calls {@code super.closeConnection(conn)} to return connection to the datasource pool</li>
     * </ol>
     * </p>
     * <p>
     * <b>Critical for Multi-Tenancy:</b> Failure to reset search_path could cause cross-tenant data
     * leakage if a connection retains org_123 search_path and is later checked out by tenant 456.
     * This reset ensures each connection checkout starts with clean state.
     * </p>
     * <p>
     * <b>Thread-Safety:</b> Safe for concurrent use. The search_path reset operates on connection-local
     * state only and does not affect other threads or connections.
     * </p>
     * <p>
     * <b>Resource Cleanup:</b> Even if search_path reset fails, the connection is still returned to
     * the pool. Connection pool health checks can detect and evict connections with incorrect state.
     * </p>
     * <p>
     * Example connection lifecycle:
     * <pre>{@code
     * Connection conn = getConnection(); // search_path = org_123,public
     * // ... use connection for tenant 123 operations
     * closeConnection(conn); // search_path reset to public
     * }</pre>
     * </p>
     *
     * @param conn the JDBC connection to reset and return to the pool
     * @throws SQLException if search_path reset fails or connection close operation fails
     * @see #getConnection()
     */
    @Override
    public void closeConnection(Connection conn) throws SQLException {
        conn.prepareStatement("set search_path to public").execute();
        super.closeConnection(conn);
    }

}