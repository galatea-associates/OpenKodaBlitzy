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

import com.openkoda.core.flow.LoggingComponent;
import com.openkoda.core.flow.mbean.LoggingEntriesStack;
import com.openkoda.core.service.event.ApplicationEvent;
import com.openkoda.dto.ServerJsThreadDto;
import com.openkoda.service.Services;
import org.apache.commons.io.output.NullWriter;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Context wrapper for ServerJS thread execution providing runtime utilities and system integration.
 * <p>
 * This class serves as the execution context for server-side JavaScript threads, exposing a comprehensive
 * set of utility methods to ServerJS scripts running within the OpenKoda platform. It provides capabilities
 * for logging, data type conversion, event emission, file system operations, and command-line process execution.
 * <p>
 * An instance of ServerJSProcessRunner is created for each ServerJS execution and injected into the JavaScript
 * context, making its public methods callable from JavaScript code. The class manages thread registration,
 * log capture, and provides safe command execution across Windows (WSL) and Linux platforms.
 * <p>
 * Key responsibilities include:
 * <ul>
 *   <li>Thread registry management for tracking active ServerJS executions</li>
 *   <li>Log capture and streaming to configured writers</li>
 *   <li>Service bean exposure to JavaScript context</li>
 *   <li>OS-agnostic command execution (bash on Linux, WSL bash on Windows)</li>
 *   <li>Type conversion helpers for JavaScript-Java interoperability</li>
 *   <li>Asynchronous event emission to application event bus</li>
 * </ul>
 * <p>
 * Example usage pattern from ServerJSRunner:
 * <pre>{@code
 * ServerJSProcessRunner runner = new ServerJSProcessRunner(services, logWriter);
 * String result = runner.runCommandToString("ls -la /tmp");
 * }</pre>
 * <p>
 * <b>Thread Safety:</b> The static {@code serverJsThreads} registry uses a LinkedHashMap and is accessed
 * concurrently. Manual synchronization is required when modifying the registry beyond the automatic
 * cleanup performed in the constructor.
 *
 * @author OpenKoda Team
 * @since 1.7.1
 * @see ServerJSRunner
 * @see LoggingComponent
 * @see LoggingEntriesStack
 */
public class ServerJSProcessRunner implements LoggingComponent {

    /**
     * Operating system detection flag indicating Windows platform.
     * <p>
     * Set to {@code true} if the application runs on Windows operating system, {@code false} otherwise.
     * This flag determines command execution strategy: Windows uses WSL bash
     * ({@code c:/Windows/System32/wsl.exe bash -c command}), while Linux/Unix uses native bash.
     * 
     * <p>
     * Initialized at class loading time by inspecting the {@code os.name} system property.
     * Detection is case-insensitive and matches any OS name starting with "windows".
     * 
     *
     * @see #startProcess(String)
     */
    private static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    /**
     * Output writer for capturing ServerJS execution logs.
     * <p>
     * All log entries generated during ServerJS script execution are written to this writer,
     * enabling log streaming to files, consoles, or other destinations. If no writer is provided
     * to the constructor, a {@link NullWriter} is used to discard log output.
     * 
     * <p>
     * The writer is flushed after each log entry to ensure immediate visibility of log messages.
     * This field is {@code final} and immutable after construction, ensuring thread-safe read access.
     * 
     *
     * @see #log(String)
     * @see #log(Object)
     * @see NullWriter
     */
    public final Writer logWriter;

    /**
     * Service bean aggregator exposed to the ServerJS execution context.
     * <p>
     * Provides access to the complete OpenKoda service layer including repositories, business logic,
     * and integration services. The {@link Services} aggregator exposes 50+ service beans, enabling
     * ServerJS scripts to interact with the application's business logic, database operations,
     * and external integrations.
     * 
     * <p>
     * Example usage from ServerJS: {@code runner.services.organizationService.findById(orgId)}
     * 
     *
     * @see Services
     * @see #emitEventAsync(String, Object)
     */
    private Services services;

    /**
     * Bounded circular buffer for storing recent ServerJS log entries in memory.
     * <p>
     * Maintains the last 50 log entries for the current thread's execution, enabling real-time
     * monitoring and debugging of ServerJS scripts. Each entry is timestamped with
     * {@link LocalDateTime} and stored as a string message.
     * 
     * <p>
     * This stack operates as a circular buffer: when capacity (50 entries) is exceeded, the oldest
     * entries are automatically evicted. The log stack is registered in {@link #serverJsThreads}
     * during construction, making it accessible for monitoring via {@link #getServerJsThreads()}.
     * 
     * <p>
     * Log entries are added via {@link #log(String)} and {@link #log(Object)} methods, which
     * write to both the {@link #logWriter} and this in-memory stack.
     * 
     *
     * @see LoggingEntriesStack
     * @see #log(String)
     * @see #serverJsThreads
     */
    private LoggingEntriesStack<String> logStack = new LoggingEntriesStack<>(50);

    /**
     * Global registry of all running ServerJS threads with their associated log stacks.
     * <p>
     * Maps each active ServerJS execution thread to its corresponding {@link LoggingEntriesStack},
     * enabling monitoring, log capture, and thread management across the application. The registry
     * is populated automatically in the constructor when a new ServerJSProcessRunner is instantiated.
     * 
     * <p>
     * LinkedHashMap preserves insertion order, allowing iteration in chronological order of thread creation.
     * The registry implements automatic cleanup: when size exceeds 30 entries, the first terminated thread
     * is removed to prevent unbounded growth.
     * 
     * <p>
     * <b>Thread Safety:</b> This LinkedHashMap is not synchronized. Concurrent access from multiple threads
     * requires external synchronization. The constructor modifies this map, as do {@link #interruptThread(long)}
     * and {@link #removeJsThread(long)} methods.
     * 
     * <p>
     * <b>Access Pattern:</b> Use {@link #getServerJsThreads()} for safe retrieval as DTOs.
     * 
     *
     * @see LoggingEntriesStack
     * @see ServerJsThreadDto
     * @see #getServerJsThreads()
     */
    //TODO: make private or package
    public static final Map<Thread, LoggingEntriesStack<String>> serverJsThreads = new LinkedHashMap<>();

    /**
     * Retrieves all active ServerJS threads as data transfer objects with their log stacks.
     * <p>
     * Converts the internal {@link #serverJsThreads} registry into a map of DTOs suitable for
     * presentation or monitoring interfaces. Each thread is wrapped in a {@link ServerJsThreadDto}
     * containing thread metadata (ID, name, state) while preserving the associated log stack.
     * 
     * <p>
     * This method provides a safe way to access thread information without exposing the raw
     * Thread objects, preventing unintended thread manipulation.
     * 
     *
     * @return new HashMap mapping ServerJsThreadDto to LoggingEntriesStack for each active thread;
     *         empty map if no ServerJS threads are currently running
     * @see ServerJsThreadDto
     * @see LoggingEntriesStack
     * @see #serverJsThreads
     */
    public static Map<ServerJsThreadDto,LoggingEntriesStack<String>> getServerJsThreads(){
        Map<ServerJsThreadDto,LoggingEntriesStack<String>> map = new HashMap<>();
        for(Map.Entry<Thread, LoggingEntriesStack<String>> item:serverJsThreads.entrySet()){
            map.put(new ServerJsThreadDto(item.getKey()),item.getValue());
        }
        return map;
    }

    /**
     * Constructs a ServerJSProcessRunner context for the current thread's ServerJS execution.
     * <p>
     * Initializes the execution context by exposing service layer beans and configuring log capture.
     * The constructor automatically registers the current thread in the {@link #serverJsThreads}
     * registry with a new {@link LoggingEntriesStack} for log capture.
     * 
     * <p>
     * <b>Automatic Registry Cleanup:</b> If the registry size exceeds 30 entries, the first
     * terminated thread is automatically removed to prevent unbounded memory growth. This cleanup
     * occurs synchronously during construction and does not guarantee removal (if all threads
     * are still active, no cleanup occurs).
     * 
     * <p>
     * <b>Null Safety:</b> If {@code logWriter} is {@code null}, a {@link NullWriter} is substituted
     * to safely discard log output without throwing exceptions.
     * 
     *
     * @param services service bean aggregator to expose to ServerJS context; must not be {@code null}
     *                 as it provides access to application business logic and integrations
     * @param logWriter output writer for log messages; {@code null} is acceptable and results in
     *                  log output being discarded via NullWriter
     * @see Services
     * @see LoggingEntriesStack
     * @see #serverJsThreads
     * @see NullWriter
     */
    public ServerJSProcessRunner(Services services, Writer logWriter) {
        this.logWriter = logWriter == null ? new NullWriter() : logWriter;
        this.services = services;
        serverJsThreads.put(Thread.currentThread(), logStack);
        if (serverJsThreads.size() > 30) {
            Optional<Thread> t = serverJsThreads.keySet().stream().filter(a -> a.getState() == Thread.State.TERMINATED).findFirst();
            if (t.isPresent()) {
                serverJsThreads.remove(t.get());
            }
        }
    }

    /**
     * Converts string representation to Long value for use in ServerJS scripts.
     * <p>
     * Provides JavaScript-to-Java type conversion for numeric values requiring 64-bit precision.
     * This method is callable from ServerJS contexts and enables handling of large integer values
     * that exceed JavaScript's safe integer range (2^53 - 1).
     * 
     * <p>
     * <b>Note:</b> This method appears unused in Java code but is dynamically invoked from
     * ServerJS scripts. Do not remove.
     * 
     * <p>
     * Example from ServerJS: {@code let id = runner.getLong("9223372036854775807");}
     * 
     *
     * @param val string containing decimal representation of a long value
     * @return Long value parsed from the string
     * @throws NumberFormatException if the string cannot be parsed as a valid long value
     * @see Long#valueOf(String)
     */
    public Long getLong(String val) {
        return Long.valueOf(val);
    }

    /**
     * Converts string representation to BigDecimal value for precise decimal arithmetic in ServerJS.
     * <p>
     * Enables JavaScript-to-Java conversion for financial calculations and other scenarios requiring
     * arbitrary-precision decimal arithmetic. JavaScript's native number type (IEEE 754 double) suffers
     * from floating-point precision issues; this method provides lossless decimal representation.
     * 
     * <p>
     * <b>Note:</b> This method appears unused in Java code but is dynamically invoked from
     * ServerJS scripts. Do not remove.
     * 
     * <p>
     * Example from ServerJS: {@code let price = runner.getBigDecimal("19.99");}
     * 
     *
     * @param val string containing decimal representation (e.g., "123.45", "0.001", "1E+10")
     * @return BigDecimal value parsed from the string with exact precision
     * @throws NumberFormatException if the string is not a valid decimal representation
     * @see BigDecimal#BigDecimal(String)
     */
    public BigDecimal getBigDecimal(String val) {
        return new BigDecimal(val);
    }


    /**
     * Emits an application event asynchronously from ServerJS context with associated payload.
     * <p>
     * Enables ServerJS scripts to participate in OpenKoda's event-driven architecture by publishing
     * events to the application event bus. The event is processed asynchronously by registered listeners,
     * allowing non-blocking integration with business workflows, audit trails, and external systems.
     * 
     * <p>
     * The event name must correspond to a registered event enum constant in the {@link ApplicationEvent}
     * registry. The payload object is delivered to all event listeners subscribed to the specified event.
     * 
     * <p>
     * <b>Note:</b> This method appears unused in Java code but is dynamically invoked from
     * ServerJS scripts. Do not remove.
     * 
     * <p>
     * Example from ServerJS: {@code runner.emitEventAsync("USER_CREATED", userObj);}
     * 
     *
     * @param eventName name of the event to emit; must match a registered ApplicationEvent enum constant
     * @param object payload object to deliver with the event; can be {@code null}
     * @return {@code true} if event emission succeeded, {@code false} if event name is unrecognized
     *         or emission failed
     * @see ApplicationEvent#getEvent(String)
     * @see com.openkoda.core.service.event.ApplicationEventService#emitEventAsync(com.openkoda.core.service.event.AbstractApplicationEvent, Object)
     */
    public boolean emitEventAsync(String eventName, Object object) {
        return services.applicationEvent.emitEventAsync(ApplicationEvent.getEvent(eventName), object);
    }

    /**
     * Pauses ServerJS script execution for the specified duration.
     * <p>
     * Causes the current thread to sleep for the given number of milliseconds, enabling timing control
     * in ServerJS scripts for polling operations, rate limiting, or coordinating with external systems.
     * The sleep can be interrupted via {@link #interruptThread(long)}, which will log the interruption
     * and return {@code false}.
     * 
     * <p>
     * <b>Note:</b> This method appears unused in Java code but is dynamically invoked from
     * ServerJS scripts. Do not remove.
     * 
     * <p>
     * Example from ServerJS: {@code runner.sleep(5000); // pause for 5 seconds}
     * 
     *
     * @param milliseconds duration to sleep in milliseconds; must be non-negative
     * @return {@code true} if sleep completed normally, {@code false} if interrupted
     * @see Thread#sleep(long)
     * @see #interruptThread(long)
     */
    public boolean sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            error("[sleep]", e);
            return false;
        }
        return true;
    }

    /**
     * Creates a new file on the filesystem with the specified content from ServerJS context.
     * <p>
     * Writes the provided string content to a file at the given path, creating the file if it
     * doesn't exist or overwriting it if it does. This method enables ServerJS scripts to generate
     * reports, export data, or create configuration files during execution.
     * 
     * <p>
     * <b>Security Note:</b> The file is created with default permissions. ServerJS scripts have
     * filesystem access according to the application's security context. Validate file paths
     * to prevent directory traversal attacks.
     * 
     * <p>
     * <b>Note:</b> This method appears unused in Java code but is dynamically invoked from
     * ServerJS scripts. Do not remove.
     * 
     * <p>
     * Example from ServerJS: {@code runner.createFileWithContent("/tmp/report.txt", data);}
     * 
     *
     * @param filePath absolute or relative path where the file should be created
     * @param content text content to write to the file; written using platform's default charset
     * @return {@code true} if file was successfully created and written, {@code false} on IOException
     * @see File#File(String)
     * @see FileWriter
     */
    public boolean createFileWithContent(String filePath, String content) {
        try {
            File file = new File(filePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
            return true;
        } catch (IOException e) {
            error(e, "[createFileWithContent] could not create file {}", filePath);
            return false;
        }
    }


    /**
     * Logs an object's string representation from ServerJS context with automatic type conversion.
     * <p>
     * Convenience method that converts any object to its string representation via {@code toString()}
     * before delegating to {@link #log(String)}. This enables ServerJS scripts to log complex objects,
     * arrays, or primitives without explicit string conversion.
     * 
     * <p>
     * <b>Note:</b> This method appears unused in Java code but is dynamically invoked from
     * ServerJS scripts. Do not remove.
     * 
     *
     * @param logEntry object to log; converted to string via {@code toString()}; {@code null} results in "null"
     * @return {@code true} indicating successful log write
     * @throws IOException if writing to {@link #logWriter} fails
     * @throws InterruptedException if the current thread is interrupted during logging
     * @see #log(String)
     */
    public boolean log(Object logEntry) throws IOException, InterruptedException {
        log(logEntry + "");
        return true;
    }

    /**
     * Logs a message from ServerJS context to both the configured writer and in-memory log stack.
     * <p>
     * Writes the log entry to {@link #logWriter} with newline appending and immediate flush,
     * and records the timestamped entry in {@link #logStack} for monitoring. This dual-write
     * approach enables both persistent log streaming and real-time in-memory access.
     * 
     * <p>
     * After logging, checks for thread interruption and throws {@link InterruptedException} if
     * the thread has been interrupted via {@link #interruptThread(long)}, enabling graceful
     * script termination.
     * 
     * <p>
     * <b>Note:</b> This method appears unused in Java code but is dynamically invoked from
     * ServerJS scripts. Do not remove.
     * 
     * <p>
     * Example from ServerJS: {@code runner.log("Processing record " + recordId);}
     * 
     *
     * @param logEntry message to log; appended with newline before writing
     * @return {@code true} indicating successful log write
     * @throws IOException if writing to {@link #logWriter} fails
     * @throws InterruptedException if the current thread has been interrupted
     * @see #logWriter
     * @see #logStack
     * @see LoggingEntriesStack#log(Object, String)
     */
    public boolean log(String logEntry) throws IOException, InterruptedException {
        logWriter.write(logEntry);
        logWriter.write("\n");
        logWriter.flush();
        logStack.log(LocalDateTime.now().toString(), logEntry);
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        return true;
    }

    /**
     * Executes a shell command and applies a callback function to each output line from ServerJS.
     * <p>
     * Streams the command's standard output line-by-line, invoking the provided function for each line.
     * This enables efficient processing of large outputs without loading the entire result into memory.
     * Common use cases include processing CSV files, parsing logs, or filtering command output.
     * 
     * <p>
     * The command executes under bash (Linux) or WSL bash (Windows) as determined by {@link #isWindows}.
     * The callback function receives each line as a string parameter; the function's return value is ignored.
     * 
     * <p>
     * <b>Interruption Support:</b> Checks for thread interruption between lines, enabling graceful
     * cancellation via {@link #interruptThread(long)}.
     * 
     * <p>
     * <b>Note:</b> This method appears unused in Java code but is dynamically invoked from
     * ServerJS scripts. Do not remove.
     * 
     * <p>
     * Example from ServerJS: {@code runner.runCommandCallbackPerLine("cat data.csv", processLine);}
     * 
     *
     * @param command bash command to execute; syntax depends on OS (see {@link #startProcess(String)})
     * @param f callback function applied to each output line; receives line as String parameter
     * @return {@code true} if command executed successfully, {@code false} on IOException
     * @throws InterruptedException if thread is interrupted during execution
     * @see #startProcess(String)
     * @see #runCommandCallbackWhole(String, Function)
     */
    public boolean runCommandCallbackPerLine(String command, Function<String, Object> f) throws InterruptedException {

        try {
            Process process = startProcess(command);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    f.apply(line);
                }
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Executes a shell command and applies a callback function to the entire output from ServerJS.
     * <p>
     * Buffers the command's complete standard output in memory before invoking the callback function
     * with the entire result as a single string. Suitable for processing structured documents like XML,
     * JSON, or small text files where the entire content must be parsed atomically.
     * 
     * <p>
     * <b>Memory Warning:</b> The entire output is loaded into memory. For large outputs (>10MB),
     * use {@link #runCommandCallbackPerLine(String, Function)} to stream line-by-line instead.
     * 
     * <p>
     * The command executes under bash (Linux) or WSL bash (Windows) as determined by {@link #isWindows}.
     * Line breaks in the output are not preserved; lines are concatenated without delimiters.
     * 
     * <p>
     * <b>Note:</b> This method appears unused in Java code but is dynamically invoked from
     * ServerJS scripts. Do not remove.
     * 
     * <p>
     * Example from ServerJS: {@code runner.runCommandCallbackWhole("cat config.xml", parseXml);}
     * 
     *
     * @param command bash command to execute; syntax depends on OS (see {@link #startProcess(String)})
     * @param f callback function invoked with complete output as String parameter
     * @return {@code true} if command executed successfully, {@code false} on IOException
     * @throws InterruptedException if thread is interrupted during execution
     * @see #startProcess(String)
     * @see #runCommandCallbackPerLine(String, Function)
     */
    public boolean runCommandCallbackWhole(String command, Function<String, Object> f) throws InterruptedException {

        try {
            Process process = startProcess(command);
            CharArrayWriter result = new CharArrayWriter();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    result.append(line);
                }
            }
            f.apply(result.toString());
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Executes a shell command and returns the output as an InputStream from ServerJS context.
     * <p>
     * Instance method wrapper for {@link #commandToInputStream(String)} enabling invocation from
     * ServerJS scripts. Returns a stream to the process's standard output, allowing streaming
     * consumption of command results.
     * 
     *
     * @param command bash command to execute
     * @return InputStream connected to command's standard output, or {@code null} on IOException
     * @see #commandToInputStream(String)
     */
    public InputStream runCommandToInputStream(String command) {
        return commandToInputStream(command);
    }

    /**
     * Executes a shell command and returns the output as a String from ServerJS context.
     * <p>
     * Instance method wrapper for {@link #commandToString(String)} enabling invocation from
     * ServerJS scripts. Waits for command completion and returns the entire standard output
     * as a string.
     * 
     *
     * @param command bash command to execute
     * @return command output as String, or {@code null} on IOException or interruption
     * @see #commandToString(String)
     */
    public String runCommandToString(String command) {
        return commandToString(command);
    }

    /**
     * Executes a shell command and returns the output as a byte array from ServerJS context.
     * <p>
     * Instance method wrapper for {@link #commandToByteArray(String)} enabling invocation from
     * ServerJS scripts. Waits for command completion and returns the standard output as raw bytes.
     * 
     *
     * @param command bash command to execute
     * @return command output as byte array, or {@code null} on IOException or interruption
     * @see #commandToByteArray(String)
     */
    public byte[] runCommandToByteArray(String command) {
        return commandToByteArray(command);
    }

    /**
     * Executes a shell command and returns the standard output as an InputStream.
     * <p>
     * Starts the process and immediately returns a stream to its standard output without waiting
     * for completion. This enables streaming consumption of command output for large results
     * or long-running processes. The process continues running while the stream is consumed.
     * 
     * <p>
     * <b>Process Lifecycle:</b> The caller is responsible for closing the returned InputStream
     * and managing the process lifecycle. The process is destroyed on IOException but remains
     * running for successful invocations.
     * 
     * <p>
     * The command executes under bash (Linux) or WSL bash (Windows) as determined by {@link #isWindows}.
     * 
     *
     * @param command bash command to execute; must be valid bash syntax
     * @return InputStream connected to process standard output, or {@code null} on IOException;
     *         closing the stream does not terminate the process
     * @see #startProcess(String)
     * @see #commandToString(String)
     * @see #commandToByteArray(String)
     */
    public static InputStream commandToInputStream(String command) {
        Process process = null;
        try {
            process = startProcess(command);
            return process.getInputStream();
        } catch (IOException e) {
            if (process != null) {
                process.destroy();
            }
            return null;
        }
    }

    /**
     * Executes a shell command, waits for completion, and returns standard output as a byte array.
     * <p>
     * Buffers the entire command output in memory as raw bytes, making it suitable for binary
     * data (images, compressed files) or when charset encoding must be handled explicitly.
     * The method blocks until the command completes or the thread is interrupted.
     * 
     * <p>
     * <b>Memory Warning:</b> Entire output is loaded into memory. Avoid using this method for
     * commands producing large outputs (>10MB). For text output, prefer {@link #commandToString(String)}.
     * For large results, use {@link #commandToInputStream(String)} for streaming.
     * 
     * <p>
     * <b>Interruption Support:</b> Returns {@code null} if the thread is interrupted during output
     * consumption, enabling graceful cancellation.
     * 
     * <p>
     * The command executes under bash (Linux) or WSL bash (Windows) as determined by {@link #isWindows}.
     * 
     *
     * @param command bash command to execute; must be valid bash syntax
     * @return command standard output as byte array, or {@code null} on IOException or interruption
     * @see #startProcess(String)
     * @see #commandToString(String)
     * @see #commandToInputStream(String)
     */
    public static byte[] commandToByteArray(String command) {

        try {
            Process process = startProcess(command);
            try (InputStream is = process.getInputStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) {
                        return null;
                    }
                    baos.write(buffer, 0, len);
                }
                return baos.toByteArray();
            }
        } catch (IOException e) {
            return null;
        }

    }

    /**
     * Executes a shell command, waits for completion, and returns standard output as a String.
     * <p>
     * Buffers the entire command output in memory as text using the platform's default charset,
     * making it ideal for text-based commands (ls, grep, cat). The method blocks until the command
     * completes or the thread is interrupted.
     * 
     * <p>
     * <b>Memory Warning:</b> Entire output is loaded into memory. Avoid using this method for
     * commands producing large outputs (>10MB). For streaming consumption, use
     * {@link #commandToInputStream(String)} instead.
     * 
     * <p>
     * <b>Interruption Support:</b> Returns {@code null} if the thread is interrupted during output
     * consumption, enabling graceful cancellation.
     * 
     * <p>
     * The command executes under bash (Linux) or WSL bash (Windows) as determined by {@link #isWindows}.
     * Output encoding uses the platform's default charset from {@link InputStreamReader}.
     * 
     *
     * @param command bash command to execute; must be valid bash syntax
     * @return command standard output as String with platform default charset encoding,
     *         or {@code null} on IOException or interruption
     * @see #startProcess(String)
     * @see #commandToByteArray(String)
     * @see #commandToInputStream(String)
     */
    public static String commandToString(String command) {

        try {
            Process process = startProcess(command);
            try (InputStreamReader sr = new InputStreamReader(process.getInputStream())) {

                StringWriter sw = new StringWriter();
                char[] buffer = new char[1024];
                int len;
                while ((len = sr.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) {
                        return null;
                    }
                    sw.write(buffer, 0, len);
                }
                return sw.toString();
            }
        } catch (IOException e) {
            return null;
        }

    }

    /**
     * Executes a shell command and monitors output until expected text appears or timeout expires.
     * <p>
     * Streams the command's standard output line-by-line, searching for a line containing the
     * expected text (case-insensitive substring match). Returns immediately when a match is found
     * or when the timeout expires. Useful for monitoring log files or waiting for application
     * startup indicators.
     * 
     * <p>
     * <b>Timeout Behavior:</b> Timeout is checked between lines, not mid-line. If the process
     * blocks on a long line, the actual wait time may exceed the timeout slightly. Timeout is
     * based on wall-clock time, not CPU time.
     * 
     * <p>
     * <b>Process Lifecycle:</b> The process is NOT terminated when a match is found or timeout
     * expires. The caller is responsible for managing the process lifecycle using the returned PID.
     * 
     * <p>
     * <b>Note:</b> This method appears unused in Java code but is dynamically invoked from
     * ServerJS scripts. Do not remove.
     * 
     * <p>
     * Example from ServerJS: {@code let pid = runner.runCommandWaitForOutputWithTimeout("tail -f /var/log/app.log", "STARTED", 60);}
     * 
     *
     * @param command bash command to execute; typically a long-running or streaming command like tail -f
     * @param expectedOutput text to search for in command output; matching is case-insensitive substring
     * @param timeout maximum wait time in seconds before giving up
     * @return process PID (as Long) if expectedOutput found before timeout, -1L if command completed
     *         or timeout expired without finding expectedOutput, {@code null} on IOException
     * @throws InterruptedException if thread is interrupted during execution
     * @see #startProcess(String)
     * @see Process#pid()
     */
    public Long runCommandWaitForOutputWithTimeout(String command, String expectedOutput, int timeout) throws InterruptedException {
        try {
            Process process = startProcess(command);
            LocalDateTime timeoutEnd = LocalDateTime.now().plusSeconds(timeout);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null && timeoutEnd.isAfter(LocalDateTime.now())) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    if(line.toLowerCase().contains(expectedOutput.toLowerCase())) {
                        return process.pid();
                    }
                }
                return -1L;
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Starts a shell process to execute the given command with OS-specific bash invocation.
     * <p>
     * This internal method provides cross-platform command execution by automatically selecting
     * the appropriate bash invocation strategy based on the operating system:
     * 
     * <ul>
     *   <li><b>Linux/Unix:</b> {@code bash -c "command"} - Direct bash invocation</li>
     *   <li><b>Windows:</b> {@code c:/Windows/System32/wsl.exe bash -c "command"} - Bash via WSL</li>
     * </ul>
     * <p>
     * The {@code -c} flag instructs bash to execute the command string directly without reading
     * from a script file. All commands should use bash syntax (pipes, redirections, environment
     * variables, etc.) regardless of the host OS.
     * 
     * <p>
     * <b>Windows Requirements:</b> Windows Subsystem for Linux (WSL) must be installed and
     * configured. The wsl.exe executable must exist at {@code c:/Windows/System32/wsl.exe}.
     * WSL provides a Linux environment on Windows, enabling bash command execution.
     * 
     * <p>
     * <b>Process Management:</b> The returned Process object represents the bash process, not
     * the command itself. Standard output/error streams and exit code refer to bash's execution.
     * The process starts immediately and runs asynchronously.
     * 
     *
     * @param commandString bash command to execute; must use bash syntax (e.g., "ls -la | grep txt")
     * @return started Process object representing the bash process executing the command
     * @throws IOException if bash executable is not found, WSL is not available (Windows),
     *         or process creation fails
     * @see ProcessBuilder
     * @see Process
     * @see #isWindows
     */
    private static Process startProcess(String commandString) throws IOException {
        Process process;
        String [] command = isWindows ?
                new String[] {"c:/Windows/System32/wsl.exe", "bash", "-c", commandString} :
                new String[] {"bash", "-c", commandString} ;
        ProcessBuilder p = new ProcessBuilder(command);
        process = p.start();
        return process;
    }

    /**
     * Interrupts a running ServerJS thread by its thread ID for graceful cancellation.
     * <p>
     * Searches the {@link #serverJsThreads} registry for a thread matching the given ID and
     * invokes {@link Thread#interrupt()} to request cooperative cancellation. The interrupted
     * thread will detect interruption in methods like {@link #log(String)},
     * {@link #sleep(int)}, and command execution methods, typically throwing
     * {@link InterruptedException} to terminate the script gracefully.
     * 
     * <p>
     * <b>Cooperative Cancellation:</b> Interruption is a request, not forceful termination.
     * The target thread must check {@link Thread#isInterrupted()} or handle
     * {@link InterruptedException} to actually stop. Well-behaved ServerJS scripts should
     * check interruption status periodically.
     * 
     * <p>
     * <b>Thread Safety:</b> This method reads from {@link #serverJsThreads} without synchronization.
     * Concurrent modifications may result in ConcurrentModificationException if iteration occurs
     * during registry changes.
     * 
     *
     * @param threadId unique identifier of the ServerJS thread to interrupt (from {@link Thread#getId()})
     * @return {@code true} always, regardless of whether a matching thread was found;
     *         method is idempotent and safe to call with non-existent thread IDs
     * @see Thread#interrupt()
     * @see #removeJsThread(long)
     * @see #serverJsThreads
     */
    public static boolean interruptThread(long threadId) {
        Optional<Thread> t = serverJsThreads.keySet().stream().filter(a -> a.getId() == threadId).findFirst();
        if (t.isPresent()) {
            t.get().interrupt();
        }
        return true;
    }

    /**
     * Removes a terminated ServerJS thread from the registry for manual cleanup.
     * <p>
     * Searches {@link #serverJsThreads} for a thread matching the given ID and removes it only
     * if its state is {@link Thread.State#TERMINATED}. This provides manual cleanup of completed
     * threads to free memory used by the log stack. Automatic cleanup occurs in the constructor
     * when the registry exceeds 30 entries.
     * 
     * <p>
     * <b>Safety:</b> Only terminated threads can be removed. This prevents accidental removal
     * of active threads and ensures log stacks remain accessible for running executions.
     * Attempting to remove a non-terminated thread returns {@code false} without modification.
     * 
     * <p>
     * <b>Thread Safety:</b> This method modifies {@link #serverJsThreads} without synchronization.
     * Concurrent registry access may result in ConcurrentModificationException. Synchronize
     * externally if calling from multiple threads.
     * 
     *
     * @param threadId unique identifier of the ServerJS thread to remove (from {@link Thread#getId()})
     * @return {@code true} if a terminated thread with the given ID was found and removed,
     *         {@code false} if no matching thread exists or the thread is not terminated
     * @see Thread.State#TERMINATED
     * @see #interruptThread(long)
     * @see #serverJsThreads
     */
    public static boolean removeJsThread(long threadId) {
        return serverJsThreads.keySet().removeIf( a -> a.getState().equals(Thread.State.TERMINATED) && a.getId() == threadId);
    }

}
