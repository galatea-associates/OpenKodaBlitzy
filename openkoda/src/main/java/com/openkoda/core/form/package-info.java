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
 * Backend form mapping, DSL, validation, and Spring MVC binding infrastructure for OpenKoda's form-driven user interfaces.
 * <p>
 * Provides a complete framework for: declaratively defining form structures via fluent builder DSL, mapping between DTOs
 * and JPA entities using reflection, validating form data with Jakarta Bean Validation, enforcing field-level read/write
 * privileges, and automatically instantiating/binding forms in Spring MVC controllers. Supports both standard JavaBean
 * entities (via ReflectionBasedEntityForm) and dynamic map-based entities (via MapEntityForm) with a unified API.
 * </p>
 *
 * <h2>Key Components</h2>
 *
 * <h3>1. Form Definition DSL</h3>
 * <ul>
 *   <li>FormFieldDefinitionBuilderStart: Entry point providing field creation methods (text(), dropdown(), manyToOne(), files(), etc.)</li>
 *   <li>FormFieldDefinitionBuilder: Continuation providing field configuration methods (validate(), datalist(), converter(), privileges())</li>
 *   <li>FrontendMappingDefinition: Immutable container holding complete form definition with all fields and validators</li>
 *   <li>FrontendMappingFieldDefinition: Immutable descriptor for a single form field with type, validation, privileges, datalists, converters</li>
 * </ul>
 *
 * <h3>2. Form Class Hierarchy</h3>
 * <ul>
 *   <li>Form: Base abstraction defining populateFrom/validate/populateTo lifecycle</li>
 *   <li>AbstractForm: Request-scoped base with reflection-based field mapping, cached accessor Functions, and privilege-aware field processing</li>
 *   <li>AbstractEntityForm: Binds DTO to persistent LongIdEntity, manages populate/validate/persist lifecycle</li>
 *   <li>AbstractOrganizationRelatedEntityForm: Organization-scoped entity forms with tenant awareness</li>
 *   <li>ReflectionBasedEntityForm: Maps Map-based DTO to JavaBean entity via PropertyUtils reflection</li>
 *   <li>MapEntityForm: Handles MapEntity dynamic entities where both DTO and entity are Map-based</li>
 * </ul>
 *
 * <h3>3. Spring MVC Integration</h3>
 * <ul>
 *   <li>MapFormArgumentResolver: HandlerMethodArgumentResolver that auto-creates and binds form instances for controller methods</li>
 *   <li>CRUDControllerConfiguration: Builder/factory encapsulating CRUD controller config (privileges, views, repositories, forms)</li>
 *   <li>ParamNameDataBinder (deprecated): Legacy parameter name normalization for map-based binding</li>
 *   <li>RenamingProcessor (deprecated): Converts dotted parameter names to bracketed notation</li>
 * </ul>
 *
 * <h3>4. Supporting Types</h3>
 * <ul>
 *   <li>FieldType: Enum of frontend field types (text, number, email, dropdown, etc.)</li>
 *   <li>FieldDbType: Enum mapping form fields to PostgreSQL column types for dynamic entity DDL generation</li>
 *   <li>DtoAndEntity: Carrier interface for DTO and entity representations</li>
 *   <li>DtoAndEntityRecord: Immutable record implementation of DtoAndEntity</li>
 *   <li>OrganizationRelatedMap: HashMap-based DTO for organization-scoped dynamic entities</li>
 *   <li>Validator: Factory for common validation functions</li>
 * </ul>
 *
 * <h2>Design Patterns</h2>
 * <ul>
 *   <li>Builder pattern: Fluent FormFieldDefinitionBuilder API for declarative form construction</li>
 *   <li>Factory pattern: CRUDControllerConfiguration creates forms and entities via cached reflective Constructors</li>
 *   <li>Strategy pattern: Multiple form implementations (ReflectionBasedEntityForm vs MapEntityForm) with unified lifecycle</li>
 *   <li>Template method: AbstractForm defines process() template, subclasses override populateFrom/populateTo</li>
 *   <li>Caching: Static Maps cache field accessors (fieldMapping), parameter rename maps (replaceMaps), and reflective Constructors</li>
 * </ul>
 *
 * <h2>Data Flow</h2>
 * <ol>
 *   <li>Developer declares form structure via FormFieldDefinitionBuilderStart DSL → produces FrontendMappingDefinition</li>
 *   <li>MapFormArgumentResolver resolves controller method parameter → looks up CRUDControllerConfiguration by URL/key</li>
 *   <li>CRUDControllerConfiguration.createNewForm() → reflectively instantiates form (ReflectionBasedEntityForm or MapEntityForm)</li>
 *   <li>WebDataBinder binds HTTP parameters to form DTO → stores in form instance</li>
 *   <li>Controller calls form.validate() → executes field validators and form-level validators</li>
 *   <li>If valid, controller calls form.populateTo(entity) → reflection-based mapping from DTO to entity fields</li>
 *   <li>Controller persists entity via repository → form lifecycle complete</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Define form structure
 * FrontendMappingDefinition employeeForm = new FormFieldDefinitionBuilderStart()
 *     .text("firstName").validate(notNull()).writePrivilege(Privilege.canManageOrgData)
 *     .text("lastName").validate(notNull())
 *     .number("salary").validate(min(0))
 *     .dropdown("department").datalist((dto, repo) -> repo.getDepartmentOptions())
 *     .buildForm();
 *
 * // Controller method - form auto-created and bound by MapFormArgumentResolver
 * @PostMapping("/employee/new")
 * public String createEmployee(EmployeeForm form, BindingResult result) {
 *     if (form.validate(result)) {
 *         Employee employee = new Employee(organizationId);
 *         form.populateTo(employee);
 *         repository.save(employee);
 *         return "success";
 *     }
 *     return "form";
 * }
 * }</pre>
 *
 * <h2>Key Frameworks &amp; Dependencies</h2>
 * <ul>
 *   <li>Spring MVC: WebDataBinder, HandlerMethodArgumentResolver, BindingResult for request binding</li>
 *   <li>Spring Data JPA: Specification support for query filtering</li>
 *   <li>Jakarta Bean Validation: @NotNull, @Size annotations and Validator interface</li>
 *   <li>Apache Commons: BeanUtils PropertyUtils for reflection-based property access</li>
 *   <li>Reactor: Tuples for privilege evaluation pairs</li>
 *   <li>Jackson: @JsonIgnore for DTO serialization</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li>Form instances: Request-scoped, single-threaded use, NOT thread-safe</li>
 *   <li>Static caches: ConcurrentHashMap or synchronized collections for shared accessor/constructor caches</li>
 *   <li>FrontendMappingDefinition: Immutable after construction, thread-safe</li>
 * </ul>
 *
 * <h2>Common Pitfalls</h2>
 * <ul>
 *   <li>Form classes must have either no-arg constructor OR constructor accepting (FrontendMappingDefinition) OR (FrontendMappingDefinition, Long, Entity)</li>
 *   <li>Entity classes must have constructor accepting (Long organizationId) for CRUDControllerConfiguration factory methods</li>
 *   <li>Field names in form definition must match DTO property names for reflection-based mapping</li>
 *   <li>Privilege evaluation requires PrivilegeHelper to be properly initialized with SecurityService context</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>com.openkoda.core.flow: Flow pipeline for controller orchestration</li>
 *   <li>com.openkoda.core.security: PrivilegeHelper and PrivilegeBase for access control</li>
 *   <li>com.openkoda.model: Entity base classes (SearchableOrganizationRelatedEntity, MapEntity)</li>
 *   <li>com.openkoda.repository: SecureRepository and ScopedSecureRepository for data access</li>
 *   <li>com.openkoda.controller: Generic CRUD controllers using this form framework</li>
 * </ul>
 *
 * @see FrontendMappingDefinition
 * @see AbstractForm
 * @see ReflectionBasedEntityForm
 * @see MapFormArgumentResolver
 * @see CRUDControllerConfiguration
 * @since 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.core.form;