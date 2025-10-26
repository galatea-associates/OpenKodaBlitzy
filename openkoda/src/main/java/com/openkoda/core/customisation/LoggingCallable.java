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

import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Wrapper for concurrent task execution with logging capability.
 * <p>
 * This class implements {@link Callable} to enable asynchronous task execution in thread pools
 * while maintaining a connection to a logging {@link Writer}. It wraps a {@link Supplier} containing
 * the actual task logic and provides the necessary infrastructure for logging output capture
 * during concurrent execution.
 * </p>
 * <p>
 * The primary use case is integrating with {@link LoggingFutureWrapper} to capture logs from
 * tasks submitted to an {@link java.util.concurrent.ExecutorService}:
 * <pre>{@code
 * ExecutorService executor = Executors.newSingleThreadExecutor();
 * Future<String> future = executor.submit(new LoggingCallable<>(writer, () -> "result"));
 * }</pre>
 * </p>
 * <p>
 * <b>Thread-Safety:</b> This class is thread-safe as long as the provided {@link Writer} is
 * safely published or synchronized externally when accessed from multiple threads.
 * </p>
 *
 * @param <V> the return value type of the wrapped task
 * @author OpenKoda Team
 * @since 1.7.1
 * @see LoggingFutureWrapper
 * @see java.util.concurrent.Callable
 * @see java.io.Writer
 */
public class LoggingCallable<V> implements Callable<V> {

    /**
     * Writer for capturing output during task execution.
     * <p>
     * This writer instance is shared with the calling context to enable log retrieval
     * after task completion. Typically a {@link java.io.CharArrayWriter} is used to
     * collect output in memory.
     * </p>
     */
    private final Writer log;
    
    /**
     * Supplier containing the actual task logic to execute.
     * <p>
     * The supplier's {@code get()} method is invoked when {@link #call()} is executed,
     * allowing the task to return a value of type {@code V}.
     * </p>
     */
    private Supplier<V> callable;

    /**
     * Constructs a logging-enabled callable wrapper.
     * <p>
     * Creates a {@link Callable} that wraps the provided {@link Supplier} and binds it to
     * a {@link Writer} for log capture. The writer should be shared with the calling context
     * to enable log retrieval after execution completes.
     * </p>
     *
     * @param log the {@link Writer} for capturing execution logs, typically a {@link java.io.CharArrayWriter}
     * @param s the {@link Supplier} containing the task logic to execute
     */
    public LoggingCallable(Writer log, Supplier<V> s) {
        this.log = log;
        callable = s;
    }

    /**
     * Executes the wrapped task and returns the result.
     * <p>
     * Invokes the wrapped {@link Supplier}'s {@code get()} method and returns the value.
     * Any exceptions thrown by the supplier are propagated to the caller.
     * </p>
     *
     * @return the execution result produced by the wrapped supplier
     * @throws Exception if the wrapped supplier throws any exception during execution
     */
    @Override
    public V call() throws Exception {
        return callable.get();
    }

}
