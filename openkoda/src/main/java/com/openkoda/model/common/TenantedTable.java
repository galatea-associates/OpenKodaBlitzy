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

package com.openkoda.model.common;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Runtime type-level annotation marking JPA entity classes as tenant-scoped for multi-tenancy metadata discovery.
 * <p>
 * This annotation identifies entity tables that participate in OpenKoda's organization-based multi-tenancy model,
 * where data is isolated by organization (tenant). The multi-tenancy infrastructure uses reflection to discover
 * annotated entities and apply tenant-scoping logic, including query filtering, partitioning, and cascade operations.

 * <p>
 * Entities marked with {@code @TenantedTable} should implement {@link OrganizationRelatedEntity} and include an
 * {@code organizationId} foreign key column referencing the {@link com.openkoda.model.Organization} entity. The {@link com.openkoda.repository.SecureRepository}
 * layer enforces tenant-scoped queries automatically, filtering data by the current user's organization memberships.

 * <p>
 * Example usage:
 * <pre>{@code
 * @Entity
 * @TenantedTable(value = {"form"})
 * public class Form extends OpenkodaEntity implements OrganizationRelatedEntity {
 *     // Entity with tenant isolation via organizationId
 * }
 * }</pre>

 * <p>
 * The multi-tenancy infrastructure processes {@code @TenantedTable} annotations for:
 * <ul>
 *   <li>Schema validation: verify {@code organization_id} column exists</li>
 *   <li>Database partitioning: configure table partitioning by organization for performance and isolation</li>
 *   <li>Data export/import: identify tenant-scoped entities requiring organization context</li>
 *   <li>Organization deletion: cascade delete tenant-scoped data via procedures like {@code remove_organizations_by_id.sql}</li>
 * </ul>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * // OrganizationRelatedEntity
 * @see com.openkoda.model.Organization
 * @see OpenkodaEntity
 * @see com.openkoda.repository.SecureRepository
 * @see ModelConstants#ORGANIZATION_ID
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface TenantedTable {
    
    /**
     * Specifies tenant-scoping metadata for the annotated entity table.
     * <p>
     * This array defines table names, partition keys, or tenant identifiers used by the multi-tenancy infrastructure
     * to discover and configure organization-scoped entities. Typical values include:

     * <ul>
     *   <li>Primary table name: {@code @TenantedTable(value = {"user_data"})} - single table tenant scope</li>
     *   <li>Multiple related tables: {@code @TenantedTable(value = {"orders", "order_items"})} - composite tenant scope</li>
     *   <li>Partition configuration: {@code @TenantedTable(value = {"organization_id"})} - partition column identifier</li>
     * </ul>
     * <p>
     * The tenancy discovery mechanism scans the classpath for {@code @TenantedTable} annotations and uses the returned
     * values to identify tables requiring an {@code organization_id} foreign key. This metadata drives partitioning logic
     * for multi-tenant database deployments and ensures tenant data isolation through query filtering.

     *
     * @return array of strings for tenant-scoping configuration (table names, partition keys, or tenant identifiers)
     */
    String[] value();
}
