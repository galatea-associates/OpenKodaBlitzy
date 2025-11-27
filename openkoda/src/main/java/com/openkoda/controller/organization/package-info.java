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
 * Provides Spring MVC controllers for multi-tenant organization management operations.
 * <p>
 * This package implements the web layer for organization lifecycle management including
 * creation, configuration, member management, and deletion. Organizations represent tenant
 * boundaries in the OpenKoda multi-tenant architecture, providing data isolation and
 * user access control at the tenant level.
 * 
 *
 * <b>Multi-Tenancy Architecture</b>
 * <p>
 * Organizations serve as the primary tenant entity in OpenKoda's multi-tenant model.
 * Each organization maintains:
 * 
 * <ul>
 *   <li>Isolated data scope with organization-scoped privileges</li>
 *   <li>Custom property bag (JSONB storage) for tenant-specific configuration</li>
 *   <li>Member roster with role-based access control</li>
 *   <li>Optional dedicated database schema for complete data isolation</li>
 * </ul>
 *
 * <b>Key Classes</b>
 * <dl>
 *   <dt>{@link com.openkoda.controller.organization.AbstractOrganizationController}</dt>
 *   <dd>Abstract base controller providing Flow-based business logic for organization
 *       operations including search with pagination, CRUD operations, member management
 *       (invite/remove users), and organization deletion with cascade cleanup. Enforces
 *       organization-scoped privileges via secure repositories.</dd>
 *
 *   <dt>{@link com.openkoda.controller.organization.OrganizationControllerHtml}</dt>
 *   <dd>Concrete Spring MVC controller exposing HTML endpoints for organization management
 *       UI. Extends AbstractOrganizationController and maps HTTP requests to Flow pipelines.
 *       Handles form binding, validation, and ModelAndView rendering. Routes under
 *       /organizations with privilege-based access control.</dd>
 * </dl>
 *
 * <b>Service Dependencies</b>
 * <p>
 * Controllers delegate to service layer for business logic execution:
 * 
 * <ul>
 *   <li>{@code OrganizationService} - Tenant provisioning, schema management, organization removal</li>
 *   <li>{@code UserService} - Member invitation, user-organization association</li>
 *   <li>{@code UserRoleService} - Role assignment and privilege reconciliation</li>
 *   <li>{@code ValidationService} - Form validation and business rule enforcement</li>
 * </ul>
 *
 * <b>Repository Access</b>
 * <p>
 * Data access uses both secure and unsecure repository patterns:
 * 
 * <ul>
 *   <li>{@code repositories.secure.organization} - Privilege-enforcing organization queries</li>
 *   <li>{@code repositories.unsecure.organization} - Administrative operations bypassing privilege checks</li>
 *   <li>{@code repositories.unsecure.user} - User lookup for member management</li>
 *   <li>{@code repositories.unsecure.role} - Global role retrieval for assignment</li>
 * </ul>
 *
 * <b>Typical Usage Patterns</b>
 * <p>
 * Organization CRUD workflow:
 * 
 * <pre>{@code
 * // Create organization with initial admin user
 * services.organization.createOrganization(form.getName());
 *
 * // Update organization properties
 * services.organization.saveOrganization(organization);
 * }</pre>
 *
 * <p>
 * Member management workflow:
 * 
 * <pre>{@code
 * // Invite user to organization
 * services.user.inviteNewOrExistingUser(email, organizationId, roleName);
 *
 * // Remove user from organization
 * services.user.removeUserRole(userRoleId);
 * }</pre>
 *
 * <b>Package Guidelines</b>
 * <p>
 * <b>Should I put a class into this package?</b>
 * 
 * <p>
 * Yes, if the class is a Spring MVC controller implementing HTTP endpoints for organization
 * management operations. Controllers should extend AbstractOrganizationController and focus
 * on request/response handling while delegating business logic to the service layer.
 * 
 *
 * @see com.openkoda.model.Organization
 * @see com.openkoda.service.organization.OrganizationService
 * @see com.openkoda.core.flow.Flow
 * @since 1.7.1
 * @version 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.controller.organization;