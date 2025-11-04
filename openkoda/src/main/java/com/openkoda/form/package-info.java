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
 * Contains server-side form adapters, mapping constants and lightweight DTOs mediating between 
 * HTTP-bound presentation DTOs and persistent domain entities. The package centralizes 
 * populateFrom/populateTo mapping logic, request-time validation hooks, and frontend mapping 
 * metadata via FrontendMappingDefinition instances. Most classes extend core abstractions 
 * (AbstractForm, AbstractEntityForm, AbstractOrganizationRelatedEntityForm) and rely on shared 
 * helpers such as getSafeValue, nullIfBlank and defaultErrorMessage. Forms are request-scoped, 
 * not thread-safe, and follow a populateFrom -&gt; validate(BindingResult) -&gt; populateTo lifecycle.
 * 
 * <b>Key Classes</b>
 * <ul>
 *   <li>{@link com.openkoda.form.FrontendMappingDefinitions} - Authoritative DSL-style collection 
 *       of pre-built form definitions used application-wide</li>
 *   <li>{@link com.openkoda.form.BasicUserForm}, {@link com.openkoda.form.EditUserForm}, 
 *       {@link com.openkoda.form.InviteUserForm} - User entity form hierarchy with email validation</li>
 *   <li>{@link com.openkoda.form.RoleForm}, {@link com.openkoda.form.PrivilegeForm} - RBAC model 
 *       forms with privilege set conversion</li>
 *   <li>{@link com.openkoda.form.OrganizationForm} - Tenant entity form with branding configuration</li>
 *   <li>{@link com.openkoda.form.EmailConfigForm} - Conditional validation form requiring SMTP or 
 *       Mailgun configuration</li>
 *   <li>{@link com.openkoda.form.SchedulerForm} - Cron expression validation using CronSequenceGenerator</li>
 *   <li>{@link com.openkoda.form.EventListenerForm} - Reflection-based consumer/event descriptor validation</li>
 *   <li>{@link com.openkoda.form.SendEventForm} - Generic one-field event carrier</li>
 *   <li>{@link com.openkoda.form.TemplateFormFieldNames}, {@link com.openkoda.form.FileFrontendMappingDefinitions} 
 *       - Centralized field name constants</li>
 * </ul>
 * 
 * <b>Design Patterns</b>
 * <p><b>Form Lifecycle:</b></p>
 * <ul>
 *   <li>populateFrom (entity → form) - Transfers entity state to DTO</li>
 *   <li>validate (Bean Validation) - Validates DTO using Jakarta Bean Validation and custom logic</li>
 *   <li>populateTo (form → entity) - Applies validated DTO data back to entity</li>
 * </ul>
 * 
 * <p><b>Validation Strategy:</b> Jakarta Bean Validation with declarative constraints 
 * ({@code @NotBlank}, {@code @Pattern}) combined with custom validate() logic for complex rules.</p>
 * 
 * <p><b>Safe Merging:</b> getSafeValue helper enables conditional field updates based on DTO presence, 
 * avoiding overwrites of unchanged fields.</p>
 * 
 * <p><b>Frontend Mapping DSL:</b> Field builders with validators, value suppliers, and privilege 
 * predicates define form structure and behavior declaratively.</p>
 * 
 * <b>Usage Examples</b>
 * <pre>
 * BasicUserForm form = new BasicUserForm(user);
 * form.populateFrom(user);
 * if(form.validate(br).getResult()) form.populateTo(user);
 * </pre>
 * 
 * <b>Relationships</b>
 * <p>Depends on:</p>
 * <ul>
 *   <li>core.form DSL (AbstractEntityForm, FrontendMappingDefinition)</li>
 *   <li>model entities (User, Role, Organization)</li>
 *   <li>dto package (BasicUserDto, RoleDto)</li>
 * </ul>
 * 
 * <b>Common Pitfalls</b>
 * <ul>
 *   <li>Forgetting to call validate before populateTo leads to invalid entity state</li>
 *   <li>Some forms have unimplemented validate stubs (FileForm, GlobalOrgRoleForm) requiring completion</li>
 *   <li>emailIsValid in BasicUserForm compiles Pattern on each call (performance concern)</li>
 *   <li>Forms are request-scoped - do not share instances across threads</li>
 * </ul>
 * 
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
package com.openkoda.form;