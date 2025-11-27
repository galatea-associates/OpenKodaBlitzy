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
 * Provides the event-driven architecture subsystem for asynchronous component communication
 * through event-listener registration and dispatch mechanisms.
 * <p>
 * This package implements domain models and persistence primitives for OpenKoda's event system,
 * enabling decoupled communication between application components through registered listeners.
 * Events are persisted in the database and reloaded at application startup, supporting
 * multi-tenant isolation and runtime event dispatching.
 * 
 *
 * <b>Key Classes</b>
 * <ul>
 * <li>{@link com.openkoda.model.component.event.Event} - Lightweight POJO representing a 3-part
 *     event descriptor (eventClassName, eventName, eventObjectType). Parses comma-separated
 *     event configuration strings for listener registration.</li>
 * <li>{@link com.openkoda.model.component.event.Consumer} - Consumer signature descriptor POJO
 *     containing className, methodName, parameterType, and numberOfStaticParams. Used for
 *     mapping event handlers to listener registrations.</li>
 * <li>{@link com.openkoda.model.component.event.EventListenerEntry} - JPA entity for persistent
 *     listener registrations with multi-tenancy support. Extends ComponentEntity to inherit
 *     organization-scoped operations. Mapped to 'event_listener' table with unique constraints
 *     to prevent duplicate registrations.</li>
 * <li>Scheduler Integration - Schedulers that emit events are considered event-related and
 *     should be placed in this package.</li>
 * </ul>
 *
 * <b>Event Lifecycle</b>
 * <ol>
 * <li><b>Registration</b> - Event listeners are registered in the EventListenerEntry table,
 *     either programmatically or through administrative interfaces.</li>
 * <li><b>Persistence</b> - Listener configurations are stored in the 'event_listener' table
 *     with unique constraints on event_name, consumer_method_name, and static_data_1-4.</li>
 * <li><b>Application Startup</b> - Registered listeners are reloaded from the database during
 *     application initialization.</li>
 * <li><b>Runtime Dispatch</b> - When events occur, the system matches them to registered
 *     listeners and dispatches accordingly.</li>
 * <li><b>Async/Sync Execution</b> - Listeners are executed in the appropriate execution context
 *     based on their configuration.</li>
 * </ol>
 *
 * <b>Serialization Format</b>
 * <p>
 * Event and Consumer objects use brittle comma-separated string formats for persistence:
 * 
 * <ul>
 * <li><b>Event Format</b> - Comma-separated triple: {@code eventClassName,eventName,eventObjectType}</li>
 * <li><b>Consumer Format</b> - Comma-separated 4-tuple:
 *     {@code className,methodName,parameterType,numberOfStaticParams}</li>
 * <li><b>Parsing Contract</b> - Uses {@code String.split(',')} with no CSV-escaping or trimming.
 *     Callers must sanitize input.</li>
 * <li><b>Brittle Nature</b> - Malformed strings cause NPE, ArrayIndexOutOfBoundsException,
 *     or NumberFormatException. Silent failures occur with incorrect token counts.</li>
 * </ul>
 *
 * <b>Multi-Tenancy Support</b>
 * <p>
 * EventListenerEntry extends ComponentEntity to provide organization-scoped operations:
 * 
 * <ul>
 * <li>Each listener registration is scoped to a specific organization via organization_id.</li>
 * <li>Listeners are isolated per tenant, preventing cross-tenant event dispatch.</li>
 * <li>Administrative interfaces enforce tenant-aware access control.</li>
 * </ul>
 *
 * <b>Thread Safety</b>
 * <ul>
 * <li><b>Event and Consumer POJOs</b> - Mutable and not thread-safe. External synchronization
 *     required for concurrent access.</li>
 * <li><b>EventListenerEntry</b> - JPA-managed entity requiring transaction context. Follow
 *     standard JPA/Hibernate concurrency patterns.</li>
 * </ul>
 *
 * <b>Common Pitfalls</b>
 * <ul>
 * <li><b>Malformed Strings</b> - Parsing failures from comma-separated input cause runtime
 *     exceptions (NPE, ArrayIndexOutOfBoundsException).</li>
 * <li><b>NumberFormatException</b> - Consumer constructor throws if numberOfStaticParams
 *     token is not a valid integer.</li>
 * <li><b>Silent Failures</b> - Consumer with non-4-token input leaves fields null without
 *     throwing exceptions.</li>
 * <li><b>No Input Validation</b> - Callers must validate and trim input before parsing.
 *     No CSV-escaping is performed.</li>
 * <li><b>Brittle Contracts</b> - Changing comma-separated formats breaks configuration
 *     import/export and runtime discovery.</li>
 * </ul>
 *
 * <b>Package Structure Guidelines</b>
 * <p><b>Should I put a class into this package?</b></p>
 * <ul>
 * <li>Event-related JPA entities belong here.</li>
 * <li>Helper objects for event logic should be placed here.</li>
 * <li>Schedulers that emit events are considered event-related.</li>
 * <li>Form-to-model mapping integration (e.g., {@code com.openkoda.form.EventListenerForm})
 *     uses classes in this package.</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
package com.openkoda.model.component.event;