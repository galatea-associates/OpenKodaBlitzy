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

package com.openkoda.core.customisation;

/**
 * Marker event indicating that OpenKoda core framework initialization is complete.
 * <p>
 * This event is published by the Spring application context after all core data setup,
 * service registration, and customization bootstrap procedures have finished. It serves
 * as a synchronization point for components that depend on a fully initialized OpenKoda
 * core environment before performing their own initialization logic.
 * </p>
 * <p>
 * The event is emitted by {@link BasicCustomisationService} following the processing of
 * Spring's {@link org.springframework.context.event.ContextRefreshedEvent}. This ensures
 * that all Spring beans are instantiated, dependency injection is complete, and OpenKoda's
 * customization subsystem has finished registering custom modules, privileges, and
 * dynamic entities before downstream components begin their initialization.
 * </p>
 * <p>
 * <b>Lifecycle Context:</b> The typical Spring boot sequence is:
 * <ol>
 * <li>Spring context initialization</li>
 * <li>{@link org.springframework.context.event.ContextRefreshedEvent} published</li>
 * <li>{@link BasicCustomisationService} processes customizations</li>
 * <li>{@code CoreSettledEvent} published (this event)</li>
 * <li>Application ready for normal operation</li>
 * </ol>
 * </p>
 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * @EventListener
 * public void onCoreSettled(CoreSettledEvent event) {
 *     // Perform initialization that requires fully settled core
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>Thread Safety:</b> This class is immutable and thread-safe. Event instances can be
 * safely published and consumed across multiple threads.
 * </p>
 * <p>
 * <b>Immutability:</b> This is a stateless marker class with no fields or methods. Each
 * instance serves purely as a type-safe signal in Spring's event publishing mechanism.
 * </p>
 *
 * @see BasicCustomisationService
 * @see org.springframework.context.event.ContextRefreshedEvent
 * @see org.springframework.context.event.EventListener
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class CoreSettledEvent {
}
