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

import java.io.CharArrayWriter;
import java.io.Writer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * A {@link Future} implementation that wraps an existing Future and binds it with a log {@link Writer}
 * to capture execution output from concurrent tasks.
 * <p>
 * This wrapper delegates all Future operations to the wrapped instance while maintaining a reference
 * to a Writer that captures log output during task execution. Typically used with {@link LoggingCallable}
 * to enable log retrieval from asynchronous operations executed via {@code ExecutorService}.
 * </p>
 * <p>
 * The log Writer is shared between the task execution context and this wrapper, allowing callers to
 * retrieve accumulated log output via {@link #getLog()} after task completion. When used with
 * {@link CharArrayWriter}, the complete execution log can be extracted as a String.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * LoggingCallable<Result> task = new LoggingCallable<>(log, () -> doWork());
 * Future<Result> future = executor.submit(task);
 * LoggingFutureWrapper<Result> wrapper = new LoggingFutureWrapper<>(future, log);
 * Result result = wrapper.get();
 * String executionLog = wrapper.getLog();
 * }</pre>
 * </p>
 * <p>
 * Thread-safety: This wrapper is thread-safe for Future operations. However, concurrent access to the
 * underlying Writer should be synchronized by the task execution framework to avoid log corruption.
 * </p>
 *
 * @param <V> the type of the result returned by the wrapped Future's computation
 * @author OpenKoda Team
 * @since 1.7.1
 * @see LoggingCallable
 * @see CharArrayWriter
 * @see Future
 */
public class LoggingFutureWrapper<V> implements Future<V> {

    private final Future<V> wrapped;
    private Writer log;

    /**
     * Creates a new LoggingFutureWrapper that delegates Future operations to the wrapped instance
     * and binds a Writer for log capture.
     *
     * @param wrapped the Future to wrap and delegate operations to
     * @param log the Writer that captures log output from the task execution, typically a {@link CharArrayWriter}
     */
    public LoggingFutureWrapper(Future<V> wrapped, Writer log) {
        this.wrapped = wrapped;
        this.log = log;
    }

    /**
     * Attempts to cancel execution of the wrapped task. Delegates to the wrapped Future's cancel method.
     *
     * @param mayInterruptIfRunning true if the thread executing the task should be interrupted;
     *                               otherwise, in-progress tasks are allowed to complete
     * @return false if the task could not be cancelled, typically because it has already completed;
     *         true otherwise
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return wrapped.cancel(mayInterruptIfRunning);
    }

    /**
     * Returns true if the wrapped task was cancelled before it completed normally.
     *
     * @return true if the task was cancelled before completion
     */
    @Override
    public boolean isCancelled() {
        return wrapped.isCancelled();
    }

    /**
     * Returns true if the wrapped task completed. Completion may be due to normal termination,
     * an exception, or cancellation.
     *
     * @return true if the task completed
     */
    @Override
    public boolean isDone() {
        return wrapped.isDone();
    }

    /**
     * Waits if necessary for the computation to complete, then retrieves the result.
     * Delegates to the wrapped Future's get method.
     *
     * @return the computed result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     */
    @Override
    public V get() throws InterruptedException, ExecutionException {
        return wrapped.get();
    }

    /**
     * Waits if necessary for at most the given time for the computation to complete,
     * then retrieves the result if available. Delegates to the wrapped Future's get method.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the computed result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws TimeoutException if the wait timed out
     */
    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return wrapped.get(timeout, unit);
    }

    /**
     * Retrieves the accumulated log output from the task execution.
     * <p>
     * When the log Writer is a {@link CharArrayWriter}, this method returns the complete log contents
     * as a String. This is the typical usage pattern with {@link LoggingCallable}, which creates a
     * CharArrayWriter for capturing task output.
     * </p>
     * <p>
     * If the log Writer is not a CharArrayWriter instance, an empty string is returned. This occurs
     * when a different Writer implementation is provided or when no logging was configured.
     * </p>
     * <p>
     * This method should be called after the task completes to retrieve the full execution log.
     * Calling before completion may return a partial log depending on the Writer's buffering behavior.
     * </p>
     *
     * @return the log contents as a String if the Writer is a CharArrayWriter; empty string otherwise
     */
    public String getLog() {
        if (log instanceof CharArrayWriter) {
            CharArrayWriter caw = (CharArrayWriter)log;
            return caw.toString();
        }
        return "";
    }

}
