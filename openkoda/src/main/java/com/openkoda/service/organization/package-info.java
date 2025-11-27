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

/**
 * Provides multi-tenant organization provisioning and lifecycle management services.
 * <p>
 * This package contains services responsible for creating, managing, and deleting tenant organizations
 * within OpenKoda's multi-tenancy architecture. Organizations serve as the root entity in the tenant
 * hierarchy, with all tenant-scoped data linked via organization_id foreign keys.
 * 
 *
 * <b>Key Services</b>
 * <ul>
 *   <li>{@code OrganizationService} - Core service for organization CRUD operations, tenant provisioning,
 *       and cascade deletion of organization-scoped resources</li>
 *   <li>{@code OrganizationCreationStrategy} - Enumeration defining organization creation strategies
 *       (with or without initial data setup)</li>
 * </ul>
 *
 * <b>Multi-Tenancy Architecture</b>
 * <p>
 * Organizations form the foundation of OpenKoda's multi-tenancy isolation:
 * 
 * <ul>
 *   <li><b>Tenant Hierarchy Root</b>: Organization entity serves as the top-level tenant container,
 *       with all tenant-specific entities linked via organization_id foreign key relationships</li>
 *   <li><b>FK Pattern</b>: Tenant-scoped entities use organization_id columns to enforce data isolation
 *       at the database level</li>
 *   <li><b>Secure Repository Enforcement</b>: All repository queries automatically filter by current
 *       organization context through SecureRepository wrappers</li>
 *   <li><b>Dynamic Entities</b>: Runtime-generated entities are automatically provisioned as
 *       tenant-aware with organization_id columns</li>
 *   <li><b>Tenant Resolution</b>: MultitenancyService resolves the current organization context
 *       from request attributes or authentication tokens</li>
 * </ul>
 *
 * <b>Organization Lifecycle</b>
 * <p>
 * Organization provisioning involves schema setup, initial role creation, privilege assignment,
 * and optional seed data population. Organization deletion cascades to all dependent tenant resources
 * including users, roles, dynamic entities, and file storage.
 * 
 *
 * <b>Usage Example</b>
 * <pre>{@code
 * OrganizationService orgService = ...;
 * Organization tenant = orgService.createOrganization("TenantCo");
 * }</pre>
 *
 * @see com.openkoda.model.Organization
 * @see com.openkoda.core.multitenancy.MultitenancyService
 * @see com.openkoda.core.multitenancy
 * @since 1.7.1
 * @version 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.service.organization;