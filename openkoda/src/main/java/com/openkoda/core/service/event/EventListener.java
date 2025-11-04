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

package com.openkoda.core.service.event;

import java.util.function.Consumer;

/**
 * Functional interface marking event handler methods and lambdas with domain-specific semantic meaning.
 * <p>
 * This interface extends {@link java.util.function.Consumer} to leverage JDK functional interface patterns,
 * enabling lambda expressions, method references, and functional composition. It serves as a semantic alias
 * for event consumer registration, providing type-safe event handling throughout the OpenKoda platform.

 * <p>
 * <b>Key Benefits:</b>
 * <ul>
 *   <li>Type-safe event handling with compile-time validation</li>
 *   <li>Lambda expression support for concise event handler definitions</li>
 *   <li>Method reference compatibility for existing handler methods</li>
 *   <li>Functional composition via {@code andThen()} for chaining handlers</li>
 * </ul>

 * <p>
 * <b>Integration Points:</b><br>
 * Event listeners are registered with {@link EventListenerService#registerListenerClusterAware} or
 * {@link ApplicationEventService#registerEventListener}. The ApplicationEventService dispatches
 * events asynchronously using a dedicated executor, requiring implementations to be thread-safe.

 * <p>
 * <b>Thread Safety:</b><br>
 * Implementations must be thread-safe as listeners may be invoked concurrently by ApplicationEventService's
 * async executor. Shared mutable state should be properly synchronized or avoided.

 * <p>
 * <b>Exception Handling:</b><br>
 * Uncaught exceptions in listener implementations propagate to the caller or event dispatcher.
 * Consider wrapping handler logic with appropriate error handling to prevent event processing failures.

 * <p>
 * <b>Usage Examples:</b>
 * <pre>{@code
 * // Lambda expression registration
 * EventListener<User> listener = user -> log.info("User created: {}", user.getName());
 * 
 * // Method reference registration
 * services.applicationEvent.registerEventListener(ApplicationEvent.USER_CREATED, this::handleUserCreated);
 * 
 * // Functional composition
 * EventListener<User> composed = listener.andThen(user -> sendEmail(user));
 * }</pre>

 * <p>
 * This is a SAM (Single Abstract Method) interface, inheriting {@code accept(T)} from Consumer,
 * which enables seamless lambda usage and functional programming patterns.

 *
 * @param <T> Event payload type that this listener consumes
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @since 1.7.1
 * @see EventListenerService
 * @see ApplicationEventService
 * @see EventConsumer
 * @see java.util.function.Consumer
 */
public interface EventListener<T> extends Consumer<T> {

}
