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

package com.openkoda.dto;

/**
 * Data Transfer Object capturing a snapshot of thread execution state and identifier.
 * <p>
 * This DTO is used for monitoring and debugging server-side JavaScript execution threads,
 * particularly for tracking thread pool activity in GraalVM JS integration. It captures
 * point-in-time state from a live Thread instance, providing an immutable snapshot suitable
 * for logging, metrics collection, and thread pool management.
 * </p>
 * <p>
 * Example usage for thread monitoring:
 * <pre>
 * Thread currentThread = Thread.currentThread();
 * ServerJsThreadDto snapshot = new ServerJsThreadDto(currentThread);
 * // snapshot.state contains current execution state
 * // snapshot.id contains unique thread identifier
 * </pre>
 * </p>
 * <p>
 * <strong>Note:</strong> This object represents an immutable snapshot of thread state at
 * the moment of construction. It does not maintain a live reference to the source Thread,
 * and the captured state may become outdated as the thread continues execution.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Thread
 * @see Thread.State
 */
public class ServerJsThreadDto {

    /**
     * Thread execution state at the time of snapshot creation.
     * <p>
     * Represents the current state of the thread using the standard Java {@link Thread.State} enum.
     * Possible values include:
     * </p>
     * <ul>
     * <li>{@code NEW} - Thread has not yet started</li>
     * <li>{@code RUNNABLE} - Thread is executing or ready to execute</li>
     * <li>{@code BLOCKED} - Thread is blocked waiting for a monitor lock</li>
     * <li>{@code WAITING} - Thread is waiting indefinitely for another thread</li>
     * <li>{@code TIMED_WAITING} - Thread is waiting for a specified period</li>
     * <li>{@code TERMINATED} - Thread has completed execution</li>
     * </ul>
     */
    public Thread.State state;
    
    /**
     * Unique thread identifier within the JVM instance.
     * <p>
     * This is the system-assigned thread ID obtained from {@link Thread#getId()}.
     * Thread IDs are unique within a single JVM execution but may be reused
     * across JVM restarts.
     * </p>
     */
    public long id;

    /**
     * Creates a snapshot of the specified thread's execution state and identifier.
     * <p>
     * This constructor captures the current state and ID from a live Thread instance,
     * creating an immutable snapshot suitable for monitoring, debugging, and thread
     * pool management. The snapshot reflects the thread's state at the moment of
     * construction and does not track subsequent state changes.
     * </p>
     *
     * @param thread the source Thread from which to capture state and ID; must not be null
     * @throws NullPointerException if thread parameter is null
     */
    public ServerJsThreadDto(Thread thread) {
       this.state=thread.getState();
       this.id=thread.getId();
    }
}