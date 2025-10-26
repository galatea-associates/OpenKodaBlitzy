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

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.component.FrontendResource;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Provides thread-local tenant context management and Hibernate multi-tenancy integration.
 * <p>
 * TenantResolver serves as the foundational component for OpenKoda's multi-tenancy architecture by maintaining
 * tenant context per thread and integrating with Hibernate's CurrentTenantIdentifierResolver contract.
 * This enables transparent tenant isolation where each request operates within its designated tenant scope.
 * </p>
 *
 * <p><b>Core Responsibilities:</b></p>
 * <ol>
 * <li>Store tenant context information in thread-local storage for thread-safe access</li>
 * <li>Provide the current thread's TenantedResource context to application code</li>
 * <li>Supply tenant identifiers to Hibernate for schema-per-tenant routing</li>
 * </ol>
 *
 * <p><b>Thread Safety:</b></p>
 * ThreadLocal storage ensures complete thread isolation of tenant context. Each thread maintains its own
 * TenantedResource without interference from concurrent requests. Callers are responsible for setting
 * tenant context before tenant-aware operations and cleaning up context in long-running threads.
 *
 * <p><b>Hibernate Integration:</b></p>
 * Implements Hibernate's CurrentTenantIdentifierResolver to provide tenant identifiers as strings.
 * The tenant identifier is derived from TenantedResource.hashCode() to create a stable identity
 * that Hibernate uses to associate database sessions with specific tenant schemas. The SchemaSupportingConnectionProvider
 * consumes this identifier to select appropriate database connections and schemas.
 *
 * <p><b>Tenant Resolution Strategy:</b></p>
 * The TenantedResource contains rich context (organizationId, host, method, access level) that enables
 * sophisticated tenant routing strategies. While Hibernate only receives a simple hashCode-based identifier,
 * the full TenantedResource is available to connection providers for advanced allocation decisions such as
 * read-replica routing or multi-datasource selection.
 *
 * <p><b>Usage Pattern:</b></p>
 * <pre>{@code
 * TenantResolver.setTenantedResource(new TenantedResource(organizationId));
 * // All database operations now execute in tenant context
 * organizationService.performTenantOperation();
 * }</pre>
 *
 * @see com.openkoda.core.multitenancy.SchemaSupportingConnectionProvider
 * @see com.openkoda.core.multitenancy.MultitenancyService
 * @see com.openkoda.model.Organization
 * @see org.hibernate.context.spi.CurrentTenantIdentifierResolver
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Component
public class TenantResolver implements CurrentTenantIdentifierResolver, LoggingComponentWithRequestId {

    /**
     * Sentinel value representing the absence of tenant context.
     * <p>
     * This immutable singleton is returned by {@link #getTenantedResource()} when no tenant context
     * has been set for the current thread. It contains null organizationId and PUBLIC access level,
     * indicating operations should proceed without tenant-specific restrictions.
     * </p>
     * <p>
     * Using a sentinel value instead of null enables consistent API behavior and eliminates
     * null-pointer checks throughout the codebase.
     * </p>
     */
    public static final TenantedResource nonExistingTenantedResource =
            new TenantedResource(null, FrontendResource.AccessLevel.PUBLIC);

    /**
     * Immutable tenant context carrier containing attributes for datasource and schema selection.
     * <p>
     * TenantedResource encapsulates all information required by database allocation strategies to route
     * tenant requests to appropriate datasources and schemas. This includes tenant identification,
     * request context, and routing hints for sophisticated multi-datasource configurations.
     * </p>
     *
     * <p><b>Field Descriptions:</b></p>
     * <ul>
     * <li><b>organizationId:</b> Primary tenant identifier mapping to {@link com.openkoda.model.Organization} entity.
     *     Null indicates no tenant context (public access).</li>
     * <li><b>host:</b> Request host header for multi-domain tenant resolution strategies. Enables host-based
     *     tenant identification when organizationId is not directly available.</li>
     * <li><b>entityKey:</b> Entity identifier for resource-specific context and fine-grained access control.</li>
     * <li><b>method:</b> HTTP method (GET, POST, PUT, DELETE) enabling read/write routing strategies.
     *     Example: route GET requests to read replicas, writes to primary database.</li>
     * <li><b>accessLevel:</b> FrontendResource access level (PUBLIC, PROTECTED, PRIVATE) for privilege-based
     *     connection selection. Not included in equals/hashCode.</li>
     * <li><b>preselectedDatasourceIndex:</b> Optional hint (-1 if unset) for multi-datasource allocation strategies
     *     to prefer specific datasource. Not included in equals/hashCode.</li>
     * </ul>
     *
     * <p><b>Equality Contract:</b></p>
     * equals() and hashCode() are based on organizationId, host, entityKey, and method only.
     * The accessLevel and preselectedDatasourceIndex fields are intentionally excluded from equality
     * to ensure consistent tenant identifier generation for Hibernate session association.
     *
     * <p><b>Thread Safety:</b></p>
     * All fields are final, making instances immutable after construction. Safe for concurrent read
     * access from multiple threads without synchronization.
     *
     * <p><b>Constructor Variants:</b></p>
     * Multiple constructors provide convenient initialization patterns:
     * <ul>
     * <li>Full constructor: All context attributes specified</li>
     * <li>organizationId-only: Simple tenant context without request details</li>
     * <li>organizationId + accessLevel: Tenant context with privilege hints</li>
     * <li>preselectedDatasourceIndex: Direct datasource selection override</li>
     * </ul>
     *
     * <p><b>Example Usage:</b></p>
     * <pre>{@code
     * // Simple tenant context
     * TenantedResource tr = new TenantedResource(organizationId);
     *
     * // Read/write routing context
     * TenantedResource tr = new TenantedResource(orgId, host, entityKey, "POST", AccessLevel.PROTECTED);
     * }</pre>
     */
    public static class TenantedResource {

        /**
         * Primary tenant identifier corresponding to {@link com.openkoda.model.Organization#id}.
         * Null indicates no tenant context (public/non-tenant operations).
         */
        public final Long organizationId;

        /**
         * Request host header for multi-domain tenant resolution.
         * Used by allocation strategies that identify tenants by domain name.
         */
        public final String host;

        /**
         * Entity identifier for resource-specific context.
         * Enables fine-grained tenant routing based on the accessed resource.
         */
        public final String entityKey;

        /**
         * HTTP method (GET, POST, PUT, DELETE) for read/write routing strategies.
         * Example: route read operations to replicas, writes to primary.
         */
        public final String method;

        /**
         * Frontend resource access level for privilege-based connection selection.
         * Not included in equals/hashCode contract for consistent tenant identifier generation.
         */
        public final FrontendResource.AccessLevel accessLevel;

        /**
         * Datasource index hint for multi-datasource allocation strategies.
         * Value of -1 indicates no preference; non-negative values suggest preferred datasource.
         * Not included in equals/hashCode contract.
         */
        public final int preselectedDatasourceIndex;

        /**
         * Creates a fully-specified tenant context with all routing attributes.
         *
         * @param organizationId Tenant identifier (null for public access)
         * @param host Request host for multi-domain routing
         * @param entityKey Resource identifier for entity-specific routing
         * @param method HTTP method for read/write routing strategies
         * @param accessLevel Privilege level for connection selection
         */
        public TenantedResource(Long organizationId, String host, String entityKey, String method, FrontendResource.AccessLevel accessLevel) {
            this.organizationId = organizationId;
            this.host = host;
            this.entityKey = entityKey;
            this.method = method;
            this.preselectedDatasourceIndex = -1;
            this.accessLevel = accessLevel;
        }

        /**
         * Creates a simple tenant context with only organization identifier.
         * Host, entityKey, and method are null; accessLevel defaults to PUBLIC.
         *
         * @param organizationId Tenant identifier (null for public access)
         */
        public TenantedResource(Long organizationId) {
            this.organizationId = organizationId;
            this.host = null;
            this.entityKey = null;
            this.method = null;
            this.preselectedDatasourceIndex = -1;
            this.accessLevel = FrontendResource.AccessLevel.PUBLIC;
        }

        /**
         * Creates a tenant context with organization identifier and access level.
         * Host, entityKey, and method are null.
         *
         * @param organizationId Tenant identifier (null for public access)
         * @param accessLevel Privilege level for connection selection
         */
        public TenantedResource(Long organizationId, FrontendResource.AccessLevel accessLevel) {
            this.organizationId = organizationId;
            this.host = null;
            this.entityKey = null;
            this.method = null;
            this.preselectedDatasourceIndex = -1;
            this.accessLevel = accessLevel;
        }

        /**
         * Creates a tenant context with explicit datasource selection hint.
         * Overrides allocation strategy to prefer the specified datasource index.
         * OrganizationId is null; accessLevel defaults to PUBLIC.
         *
         * @param preselectedDatasourceIndex Preferred datasource index (0-based)
         */
        public TenantedResource(int preselectedDatasourceIndex) {
            this.organizationId = null;
            this.host = null;
            this.entityKey = null;
            this.method = null;
            this.preselectedDatasourceIndex = preselectedDatasourceIndex;
            this.accessLevel = FrontendResource.AccessLevel.PUBLIC;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TenantedResource that = (TenantedResource) o;

            if (organizationId != null ? !organizationId.equals(that.organizationId) : that.organizationId != null)
                return false;
            if (host != null ? !host.equals(that.host) : that.host != null) return false;
            if (entityKey != null ? !entityKey.equals(that.entityKey) : that.entityKey != null) return false;
            return method != null ? method.equals(that.method) : that.method == null;
        }

        @Override
        public int hashCode() {
            int result = organizationId != null ? organizationId.hashCode() : 0;
            result = 31 * result + (host != null ? host.hashCode() : 0);
            result = 31 * result + (entityKey != null ? entityKey.hashCode() : 0);
            result = 31 * result + (method != null ? method.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return
                    "org=" + organizationId +
                    ", host='" + host + '\'' +
                    ", entityKey='" + entityKey + '\'' +
                    ", method='" + method + '\'' +
                    ", pre=" + preselectedDatasourceIndex +
                    '}';
        }
    }

    /**
     * Thread-local storage for tenant context, ensuring thread-safe tenant isolation.
     * <p>
     * Each thread maintains its own TenantedResource without interference from concurrent requests.
     * The ThreadLocal pattern guarantees that tenant context set in one thread does not affect
     * other threads, enabling safe parallel request processing in multi-tenant environments.
     * </p>
     * <p>
     * Callers are responsible for cleaning up thread-local state in long-running threads to prevent
     * memory leaks. In typical web request processing, thread pools handle cleanup automatically.
     * </p>
     */
    private static ThreadLocal<TenantedResource> tenantedResource = new ThreadLocal<>();

    /**
     * Sets the tenant context for the current thread.
     * <p>
     * This method should be called before any tenant-aware database operations to establish
     * the tenant scope. The context remains active for the current thread until explicitly
     * changed or the thread terminates.
     * </p>
     *
     * @param tr The tenant resource context to associate with current thread (null accepted)
     * @return Always returns true for backward compatibility with boolean expressions
     */
    public static boolean setTenantedResource(TenantedResource tr) {
        tenantedResource.set(tr);
        return true;
    }

    /**
     * Retrieves the tenant context for the current thread.
     * <p>
     * Returns the TenantedResource previously set via {@link #setTenantedResource(TenantedResource)}.
     * If no context has been set for this thread, returns the {@link #nonExistingTenantedResource}
     * sentinel value instead of null, eliminating the need for null checks.
     * </p>
     *
     * @return Current thread's TenantedResource, or nonExistingTenantedResource sentinel if none set (never null)
     */
    public static TenantedResource getTenantedResource() {
        return tenantedResource.get() == null ? nonExistingTenantedResource : tenantedResource.get();
    }

    /**
     * Resolves the current tenant identifier for Hibernate multi-tenancy.
     * <p>
     * Implements Hibernate's {@link CurrentTenantIdentifierResolver} contract to provide
     * tenant identifiers as strings. The identifier is derived from the current thread's
     * TenantedResource hashCode, creating a stable identity that Hibernate uses to associate
     * database sessions with tenant schemas.
     * </p>
     *
     * @return String representation of current tenant identifier (hashCode of TenantedResource)
     */
    @Override
    public String resolveCurrentTenantIdentifier() {
        return String.valueOf(getTenantedResource().hashCode());
    }

    /**
     * Validates existing Hibernate sessions (disabled in this implementation).
     * <p>
     * Implements Hibernate's {@link CurrentTenantIdentifierResolver} contract.
     * Returning false instructs Hibernate to skip validation of existing sessions,
     * allowing flexible tenant context switching within the same thread.
     * </p>
     *
     * @return Always false - session validation is not enforced
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

}