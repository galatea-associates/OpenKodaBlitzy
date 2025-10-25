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
 * Role-Based Access Control (RBAC) role management controllers providing web interfaces for role lifecycle operations.
 * 
 * <h2>Overview</h2>
 * <p>
 * This package contains Spring MVC controllers that manage roles within OpenKoda's RBAC security architecture.
 * Controllers provide HTTP endpoints for creating, reading, updating, and deleting roles, as well as managing
 * role-privilege assignments and role-user associations. The implementation follows OpenKoda's Flow pipeline
 * pattern for request handling and integrates with the organization-scoped multi-tenancy system.
 * </p>
 * 
 * <h2>RBAC Model</h2>
 * <p>
 * OpenKoda implements Role-Based Access Control where roles aggregate multiple privileges. Users receive roles
 * through UserRole associations. The effective privileges for a user are calculated by taking the union of all
 * privileges from all roles assigned to that user within the current organization context. This enables flexible
 * permission management while maintaining strict tenant isolation.
 * </p>
 * <p>
 * Key relationships:
 * </p>
 * <ul>
 *   <li><strong>Role</strong> - Aggregates multiple Privileges via serialized privilege string</li>
 *   <li><strong>Privilege</strong> - Canonical permission enum with PrivilegeBase interface</li>
 *   <li><strong>UserRole</strong> - Association entity linking Users to Roles with organization scope</li>
 *   <li><strong>User</strong> - Receives effective privileges from all assigned roles</li>
 * </ul>
 * 
 * <h2>Key Classes</h2>
 * <dl>
 *   <dt>{@code AbstractRoleController}</dt>
 *   <dd>Abstract base controller implementing core role CRUD operations using Flow pipelines. Provides protected
 *   methods for role creation, modification, privilege assignment, and role reconciliation. Subclasses provide
 *   concrete HTTP endpoint mappings and response rendering (HTML or REST API).</dd>
 *   
 *   <dt>{@code RoleControllerHtml}</dt>
 *   <dd>HTML UI implementation extending AbstractRoleController. Provides browser-based role management interface
 *   with server-side rendering using Thymeleaf templates. Secured with Spring Security annotations requiring
 *   appropriate role management privileges.</dd>
 * </dl>
 * 
 * <h2>Integration Points</h2>
 * <p>
 * Controllers in this package integrate with multiple layers of the OpenKoda architecture:
 * </p>
 * <ul>
 *   <li><strong>services.role</strong> - RoleService for role reconciliation and privilege assignment logic</li>
 *   <li><strong>model.Role</strong> - JPA entity for role persistence with single-table inheritance</li>
 *   <li><strong>model.Privilege</strong> - Privilege enum providing canonical privilege definitions</li>
 *   <li><strong>model.UserRole</strong> - Association entity for user-role assignments</li>
 *   <li><strong>core.security</strong> - SecurityService for privilege evaluation and enforcement</li>
 *   <li><strong>core.flow</strong> - Flow pipeline DSL for request handling and transactional execution</li>
 * </ul>
 * 
 * <h2>Usage Pattern</h2>
 * <p>
 * Concrete controllers extend {@code AbstractRoleController} and provide HTTP endpoint mappings with Spring
 * Security authorization. Controllers use the Flow pipeline pattern to compose request handling logic with
 * transactional execution and privilege checks.
 * </p>
 * <pre>
 * // Example Flow pipeline for role creation
 * Flow.init().thenSet("role", a -&gt; roleService.createRole(form))
 * </pre>
 * 
 * <h2>Package Guidelines</h2>
 * <p><b>Should I put a class into this package?</b></p>
 * <p>
 * Include classes in this package if they implement Spring MVC controller functionality specifically for role
 * management operations. Controllers must extend {@code AbstractRoleController} or implement equivalent role
 * lifecycle operations with appropriate Spring Security authorization.
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @since 2019-01-24
 */
package com.openkoda.controller.role;