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
 * Provides web controllers for event listener and scheduler management in the OpenKoda Admin panel.
 * 
 * <b>Package Purpose</b>
 * <p>
 * This package contains controllers that expose HTTP endpoints for managing event listeners and schedulers
 * in the OpenKoda platform. Event listeners respond to application events, while schedulers trigger events
 * at specified times using cron expressions. Both components support multi-tenancy and cluster-aware operations.
 * 
 * 
 * <b>Architectural Role</b>
 * <p>
 * This package sits in the web layer of the OpenKoda architecture, handling HTTP requests for event system
 * configuration. Controllers delegate business logic to service classes and use the Flow pipeline pattern
 * for composable request handling. The abstract-concrete controller pattern separates business logic
 * (abstract controllers) from HTTP binding (concrete HTML controllers).
 * 
 * 
 * <b>Controller Pattern</b>
 * <p>
 * The package follows a two-tier controller architecture:
 * 
 * <ul>
 * <li><b>Abstract Controllers</b>: Contain reusable business logic for CRUD operations, validation,
 *     and service orchestration. These controllers are independent of HTTP binding details.</li>
 * <li><b>HTML Controllers</b>: Extend abstract controllers and provide Spring MVC REST endpoints
 *     that return ModelAndView objects for Thymeleaf rendering. They handle form binding,
 *     validation errors, and view selection.</li>
 * </ul>
 * 
 * <b>Key Classes</b>
 * 
 * <b>AbstractEventListenerController</b>
 * <p>
 * Provides business logic for EventListenerEntry entity management. Key responsibilities include:
 * 
 * <ul>
 * <li>CRUD operations for event listener configuration</li>
 * <li>Manual event emission for testing listeners</li>
 * <li>Cluster-aware listener registration and unregistration</li>
 * <li>DTO-to-event mapping for runtime event construction</li>
 * <li>Component export functionality for version control</li>
 * </ul>
 * 
 * <b>EventListenerControllerHtml</b>
 * <p>
 * Spring MVC REST controller providing HTML view endpoints at {@code _HTML + _EVENTLISTENER} paths.
 * Handles form binding, validation, and ModelAndView generation for the Admin UI. Uses Flow pipelines
 * to compose request handling logic with transaction management and error handling.
 * 
 * 
 * <b>AbstractSchedulerController</b>
 * <p>
 * Provides business logic for Scheduler entity lifecycle management. Key responsibilities include:
 * 
 * <ul>
 * <li>CRUD operations for scheduler configuration</li>
 * <li>Cron expression parsing and validation</li>
 * <li>Cluster-aware scheduler registration and unregistration</li>
 * <li>Dynamic scheduler enable/disable operations</li>
 * <li>Component export functionality for deployment</li>
 * </ul>
 * 
 * <b>SchedulerControllerHtml</b>
 * <p>
 * Spring MVC REST controller providing HTML view endpoints at {@code _HTML + _SCHEDULER} paths.
 * Exposes scheduler configuration UI with form validation and Thymeleaf view rendering.
 * 
 * 
 * <b>Admin UI Integration</b>
 * <p>
 * Controllers integrate with the OpenKoda Admin panel using these patterns:
 * 
 * <ul>
 * <li><b>Security</b>: Spring Security {@code @PreAuthorize} annotations enforce privilege checks
 *     using {@code CHECK_CAN_READ_BACKEND} and {@code CHECK_CAN_MANAGE_BACKEND} privileges</li>
 * <li><b>Flow Pipelines</b>: Functional composition via {@code Flow.init().thenSet().then().execute()}
 *     for transaction management and error handling</li>
 * <li><b>PageModelMap</b>: Model attribute management for view rendering with type-safe accessors</li>
 * <li><b>ModelAndView</b>: Fragment selection via {@code .mav()} method with success/error view variants
 *     for AJAX-based partial page updates</li>
 * <li><b>Jakarta Bean Validation</b>: {@code @Valid} annotation with {@code BindingResult} for form validation
 *     and error message display</li>
 * </ul>
 * 
 * <b>Security Requirements</b>
 * <p>
 * Access to event listener and scheduler endpoints requires backend privileges:
 * 
 * <ul>
 * <li><b>READ Privilege</b> ({@code CHECK_CAN_READ_BACKEND}): Required for viewing event listeners
 *     and schedulers via {@code getAll} and {@code settings} endpoints</li>
 * <li><b>MANAGE Privilege</b> ({@code CHECK_CAN_MANAGE_BACKEND}): Required for creating, updating,
 *     and removing listeners and schedulers, plus manual event emission</li>
 * </ul>
 * <p>
 * Controllers use secure repositories for queries requiring privilege checks and unsecure repositories
 * for mutations after validation. This ensures consistent authorization enforcement across operations.
 * 
 * 
 * <b>Multi-Tenancy Support</b>
 * <p>
 * Event listeners support organization-scoped operations. When creating or updating listeners,
 * the system associates them with the current organization context. This enables tenant-specific
 * event handling and listener configuration isolation.
 * 
 * 
 * <b>Cluster-Aware Operations</b>
 * <p>
 * Both event listeners and schedulers operate in clustered environments. When registering or
 * unregistering listeners/schedulers, the system uses {@code ClusterEventSenderService} to broadcast
 * changes across all application instances. This ensures consistent event handling and scheduling
 * across the cluster without manual coordination.
 * 
 * 
 * <b>Technical Dependencies</b>
 * <p>
 * Controllers depend on these key frameworks and components:
 * 
 * <ul>
 * <li><b>Spring Framework</b>: {@code @RestController}, {@code @RequestMapping}, {@code @PreAuthorize},
 *     {@code @PathVariable}, {@code @RequestParam}, {@code @Valid} for MVC and security</li>
 * <li><b>Spring Data JPA</b>: {@code Pageable}, {@code Specification} for pagination and filtering,
 *     secure/unsecure repository split for authorization</li>
 * <li><b>Jakarta Bean Validation</b>: {@code @Valid} annotation with {@code BindingResult} for
 *     declarative validation and error handling</li>
 * <li><b>Flow Pipeline</b>: {@code Flow}, {@code PageModelMap}, {@code ResultAndModel} for
 *     functional composition and transaction management</li>
 * <li><b>Service Layer</b>: {@code ValidationService}, {@code ComponentExportService},
 *     {@code EventListenerService}, {@code SchedulerService}, {@code ApplicationEventService}
 *     for business logic orchestration</li>
 * </ul>
 * 
 * <b>Operational Characteristics</b>
 * <p>
 * Controllers in this package exhibit these operational properties:
 * 
 * <ul>
 * <li><b>Stateless</b>: Controllers are stateless Spring beans with no instance state</li>
 * <li><b>Business Logic Delegation</b>: Side effects like database I/O, file exports, and runtime
 *     registration are delegated to abstract controllers and service classes</li>
 * <li><b>Cluster Consistency</b>: Cluster-aware operations via {@code ClusterEventSenderService}
 *     ensure configuration consistency across application instances</li>
 * <li><b>Manual Event Emission</b>: Provides functionality for testing event listeners in
 *     production environments without triggering actual business events</li>
 * </ul>
 * 
 * <b>Usage Guidance</b>
 * <p>
 * When extending this package:
 * 
 * <ul>
 * <li>Extend abstract controllers for new access types (e.g., API controllers returning JSON)</li>
 * <li>Implement HTTP binding and result formatting in concrete controllers</li>
 * <li>Delegate business logic to abstract controller methods to avoid duplication</li>
 * <li>Use Flow pipelines for transaction management and error handling</li>
 * <li>Follow the secure/unsecure repository pattern for consistent authorization</li>
 * </ul>
 * 
 * <b>Related Packages</b>
 * <ul>
 * <li>{@link com.openkoda.core.service.event} - Event listener and scheduler service layer</li>
 * <li>{@code com.openkoda.model.component.event} - Event listener and scheduler entities</li>
 * <li>{@code com.openkoda.form} - Form definitions for event listener and scheduler configuration</li>
 * </ul>
 * 
 * <p><b>Should I put a class into this package?</b></p>
 * <p>
 * Place a class in this package if it is a controller that handles HTTP requests related to event
 * listeners or schedulers. Abstract controllers containing shared business logic belong here,
 * as do concrete controllers providing specific HTTP bindings (HTML, JSON, etc.).
 * 
 * 
 * @since 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.core.controller.event;