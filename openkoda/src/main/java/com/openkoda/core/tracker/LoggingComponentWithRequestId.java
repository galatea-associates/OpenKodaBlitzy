/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 and associated documentation files (the "Software"), to deal in the Software without restriction, 
including without limitation the rights to use, copy, modify, merge, publish, distribute, 
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software 
is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice 
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE 
AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS 
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES 
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.core.tracker;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openkoda.core.flow.mbean.LoggingEntriesStack;
import com.openkoda.core.helper.ReadableCode;
import com.openkoda.core.helper.UrlHelper;
import com.openkoda.core.service.AuditService;
import com.openkoda.core.service.event.ApplicationEvent;
import com.openkoda.core.service.event.ApplicationEventService;
import com.openkoda.dto.NotificationDto;
import com.openkoda.model.notification.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.util.Assert;
import org.springframework.util.ResizableByteArrayOutputStream;

import java.io.PrintStream;
import java.util.*;

/**
 * Base component providing request correlation ID tracking for distributed tracing across application layers.
 * <p>
 * Mixin interface that components extend to gain automatic request-correlated logging capabilities. 
 * Extracts or generates correlation IDs from HTTP request context (via {@link WebRequestIdHolder}) or 
 * MDC cron job context (via {@link RequestIdHolder}), automatically prepending correlation IDs to all 
 * log messages. Provides centralized logger management with per-class SLF4J logger caching, runtime 
 * debug mode toggles per logger class, and in-memory debug stack for log retention.
 * </p>
 * <p>
 * Integrates with {@link ApplicationEventService} for error event emission and {@link AuditService} for 
 * error audit trails. Thread-safe storage uses {@code ThreadLocal} MDC for correlation IDs and 
 * {@code ThreadLocal} {@link #isInLoggingLoop} for recursion prevention.
 * </p>
 * <p>
 * <b>WARNING</b>: Shared static collections ({@link #loggers}, {@link #availableLoggers}, 
 * {@link #debugLoggers}) are mutated process-wide without explicit synchronization - concurrency safety 
 * depends on concrete collection types. The {@link #loggers} HashMap may lose entries during concurrent 
 * class registration. Consider using {@code ConcurrentHashMap} for production use.
 * </p>
 * <p>
 * Usage pattern - extend this interface to automatically gain request-correlated logging:
 * <pre>{@code
 * class MyService implements LoggingComponentWithRequestId {
 *     void processUser(Long userId) {
 *         debug("Processing user {}", userId);
 *     }
 * }
 * }</pre>
 * </p>
 * <p>
 * Distributed tracing integration: Correlation IDs propagate through entire request or job execution, 
 * enabling trace correlation across controllers, services, and repositories. IDs appear in all log 
 * messages for filtering and grouping.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see RequestIdHolder
 * @see WebRequestIdHolder
 * @see LoggingEntriesStack
 * @see ApplicationEventService
 * @see AuditService
 * @see DebugLogsDecoratorWithRequestId
 */
public interface LoggingComponentWithRequestId extends ReadableCode {

    /**
     * Process-wide cache of SLF4J Logger instances keyed by implementing class.
     * <p>
     * Populated lazily on first {@link #getLogger()} call per class. Each class implementing this 
     * interface gets its own Logger instance cached here for reuse across all method calls.
     * </p>
     * <p>
     * <b>WARNING</b>: Uses unsynchronized {@code HashMap} - concurrent class registration from multiple 
     * threads may lose entries or corrupt map structure. Consider {@code ConcurrentHashMap} for 
     * production use.
     * </p>
     */
    static final Map<Class, Logger> loggers = new HashMap<Class, Logger>();
    
    /**
     * List of all classes that have registered loggers.
     * <p>
     * Updated when new logger created via {@link #getLogger()}. Used by 
     * {@code DebugLogsDecoratorWithRequestId} to enumerate available logger classes for management 
     * interfaces.
     * </p>
     * <p>
     * <b>WARNING</b>: Unsynchronized {@code ArrayList} - concurrent modifications may cause 
     * {@code ConcurrentModificationException} or inconsistent state.
     * </p>
     */
    static final List<Class> availableLoggers = new ArrayList<>();
    
    /**
     * Set of classes with debug mode enabled.
     * <p>
     * Classes in this set have debug and trace messages logged to {@link #debugStack}. Modified by 
     * {@code DebugLogsDecoratorWithRequestId} toggle methods to enable or disable debug mode at runtime.
     * </p>
     * <p>
     * <b>WARNING</b>: Unsynchronized {@code HashSet} - concurrent adds or removes may cause inconsistent 
     * debug mode state across threads.
     * </p>
     */
    static final Set<Class> debugLoggers = new HashSet<>();
    
    /**
     * Dedicated SLF4J logger for debug stack messages.
     * <p>
     * Logger name: {@code "jmxDebug"}. Used by {@link #logToDebugStack(Throwable, String, boolean, Object...)} 
     * to write formatted messages. Configure log level independently in logging configuration (logback.xml 
     * or log4j2.xml).
     * </p>
     * <p>
     * Immutable and thread-safe after initialization.
     * </p>
     */
    static final Logger debugLogger = LoggerFactory.getLogger( "jmxDebug" );
    
    /**
     * In-memory bounded debug log buffer with 500-entry capacity.
     * <p>
     * Stores request-keyed debug messages for runtime inspection via JMX or management interfaces. 
     * Entries formatted as {@code "ClassName - message"}. Capacity configurable via 
     * {@code DebugLogsDecoratorWithRequestId.setMaxEntries()}.
     * </p>
     * <p>
     * Thread-safety depends on {@link LoggingEntriesStack} implementation. Used by {@link #getDebugEntries()} 
     * to retrieve captured debug logs.
     * </p>
     */
    static final LoggingEntriesStack<String> debugStack = new LoggingEntriesStack<>(500);
    
    /**
     * ThreadLocal flag to prevent infinite recursion when error logging triggers exceptions.
     * <p>
     * Set to {@code true} during {@link #emitErrorLogNotificationEvent(Throwable, String, Object...)} 
     * execution. If error occurs during event emission or audit creation, recursion is detected and 
     * prevented. Thread-safe via {@code ThreadLocal} isolation. Initialized to {@code false} for each 
     * thread.
     * </p>
     */
    ThreadLocal<Boolean> isInLoggingLoop =  ThreadLocal.withInitial( () -> false);

    /**
     * Retrieves cached SLF4J Logger for implementing class, optionally creating if absent.
     * <p>
     * Checks {@link #loggers} cache for implementing class's Logger. If not found and 
     * {@code createIfNotExists} is {@code true}, creates new Logger via 
     * {@link LoggerFactory#getLogger(Class)}, caches it in {@link #loggers}, and adds class to 
     * {@link #availableLoggers} list.
     * </p>
     * <p>
     * <b>WARNING</b>: Uses unsynchronized {@code HashMap} - concurrent creates from multiple threads 
     * may lose cache entries or corrupt map structure.
     * </p>
     *
     * @param createIfNotExists {@code true} to create and cache logger if not exists, {@code false} 
     *                          to return {@code null} if not cached
     * @return SLF4J Logger for implementing class, or {@code null} if not cached and 
     *         {@code createIfNotExists} is {@code false}
     * @see LoggerFactory#getLogger(Class)
     */
    default Logger getLogger(boolean createIfNotExists) {
        Logger l = loggers.get( getClass() );
        if ( l == null && createIfNotExists ) {
            l = LoggerFactory.getLogger( getClass() );
            loggers.put( getClass() , l );
            availableLoggers.add( getClass() );
        }
        return l;
    }

    /**
     * Retrieves cached SLF4J Logger for implementing class, creating if necessary.
     * <p>
     * Convenience method that delegates to {@link #getLogger(boolean)} with {@code true}. Always returns 
     * a logger, creating and caching if needed.
     * </p>
     * <p>
     * Excluded from JSON serialization via {@link JsonIgnore} to avoid exposing internal logger instances.
     * </p>
     *
     * @return SLF4J Logger for implementing class, never {@code null}
     */
    @JsonIgnore
    default Logger getLogger() {
        return getLogger( true );
    }


    /**
     * Appends correlation ID to log message.
     * <p>
     * Delegates to {@link #appendRequestId(String, boolean)} with {@code false}. Retrieves correlation 
     * ID via {@link RequestIdHolder#getId()} and prepends it to message.
     * </p>
     *
     * @param message original log message
     * @return message prefixed with correlation ID in format {@code "correlationId: message"}
     */
    default String appendRequestId(String message) {
        return appendRequestId(message, false);
    }

    /**
     * Appends correlation ID to log message, optionally building audit UI link.
     * <p>
     * If {@code appendAuditUrl} is {@code true}, formats message as 
     * {@code "{baseUrl}/html/audit/all?audit_search={correlationId}: {message}"} for clickable audit 
     * trail access. If {@code false}, formats as {@code "{correlationId}: {message}"}.
     * </p>
     * <p>
     * Uses {@link UrlHelper#getBaseUrlOrEmpty()} for base URL and {@link RequestIdHolder#getId()} for 
     * correlation ID.
     * </p>
     *
     * @param message original log message
     * @param appendAuditurl {@code true} to build clickable audit UI link with correlation ID, 
     *                       {@code false} for simple prefix
     * @return message with correlation ID prepended, either as audit UI URL or simple prefix
     */
    default String appendRequestId(String message, boolean appendAuditurl) {
        return appendAuditurl ? String.format("%s/html/audit/all?audit_search=%s: %s",
                UrlHelper.getBaseUrlOrEmpty(), RequestIdHolder.getId(), message)
                : RequestIdHolder.getId() + ": " + message;
    }

    /**
     * Logs debug-level message with correlation ID and optional debug stack capture.
     * <p>
     * Logs to {@link #debugStack} if debug mode enabled for class (via {@link #isDebugLogger()}), then 
     * logs to SLF4J logger at debug level with correlation ID prepended. Debug stack entry includes 
     * class name and formatted message.
     * </p>
     *
     * @param format SLF4J format string with {@code {}} placeholders (e.g., {@code "User id: {}"})
     * @param arguments values to fill placeholders
     */
    default void debug(String format, Object... arguments) {
        logToDebugStack(null, format , true, arguments );
        Logger l = getLogger();
        format = appendRequestId(format);
        l.debug( format , arguments );
    }

    /**
     * Formats message using SLF4J MessageFormatter.
     * <p>
     * Uses {@link MessageFormatter#arrayFormat(String, Object[])} to substitute arguments into format 
     * string. Handles throwables in argument array per SLF4J conventions.
     * </p>
     *
     * @param format format string with {@code {}} placeholders
     * @param arguments values to fill placeholders
     * @return formatted message with placeholders replaced
     */
    default String formatMessage(String format, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat( format , arguments );
        return ft.getMessage();
    }

    /**
     * Writes formatted message to debug stack with optional exception stack trace.
     * <p>
     * Formats message via {@link #formatMessage(String, Object...)}. If throwable provided, captures 
     * full stack trace via {@link Throwable#printStackTrace(PrintStream)} into 
     * {@link ResizableByteArrayOutputStream}, appends to message. Logs to {@link #debugLogger} at debug 
     * level and stores in {@link #debugStack} with correlation ID key and {@code "ClassName - message"} 
     * format.
     * </p>
     * <p>
     * Used by {@link #debug(String, Object...)}, {@link #trace(String, Object...)}, 
     * {@link #warn(String, Object...)}, and {@link #error(String, Object...)} methods.
     * </p>
     *
     * @param t throwable to capture stack trace from, or {@code null}
     * @param message message format string
     * @param checkIfDebugLoggerEnabled {@code true} to skip logging if debug mode not enabled for class, 
     *                                  {@code false} to always log
     * @param arguments format string arguments
     */
    default void logToDebugStack(Throwable t, String message, boolean checkIfDebugLoggerEnabled, Object... arguments) {
        if (checkIfDebugLoggerEnabled && !isDebugLogger()) {
            return;
        }
        PrintStream ps = null;
        message = formatMessage(message, arguments);
        if ( t != null ) {
            try {
                ResizableByteArrayOutputStream buffer = new ResizableByteArrayOutputStream(8 * 128);
                ps = new PrintStream(buffer);
                ps.append(message);
                ps.append("\n");
                t.printStackTrace(ps);
                message = buffer.toString();
            } finally {
                if (ps != null) {
                    ps.close();
                }
            }
        }
        debugLogger.debug( message );
        debugStack.log(RequestIdHolder.getId(), getClass().getSimpleName() + " - " + message );
    }

    /**
     * Logs trace-level message with correlation ID and optional debug stack capture.
     * <p>
     * Logs to {@link #debugStack} if debug mode enabled, then logs to SLF4J logger at trace level with 
     * correlation ID prepended.
     * </p>
     *
     * @param format SLF4J format string with {@code {}} placeholders
     * @param arguments values to fill placeholders
     */
    default void trace(String format, Object... arguments) {
        logToDebugStack( null, format , true, arguments );
        Logger l = getLogger();
        format = appendRequestId(format);
        l.trace( format , arguments );
    }

    /**
     * Checks if debug mode is enabled for implementing class.
     * <p>
     * Returns {@code true} if class is in {@link #debugLoggers} set (debug messages logged to 
     * {@link #debugStack}), {@code false} otherwise. Debug mode can be enabled or disabled via 
     * {@code DebugLogsDecoratorWithRequestId} toggle methods.
     * </p>
     * <p>
     * Excluded from JSON serialization via {@link JsonIgnore}.
     * </p>
     *
     * @return {@code true} if class is in debugLoggers set, {@code false} otherwise
     */
    @JsonIgnore
    default boolean isDebugLogger() {
        return debugLoggers.contains( getClass() );
    }

    /**
     * Logs info-level message with correlation ID.
     * <p>
     * Logs to SLF4J logger at info level with correlation ID prepended. Does not log to debug stack.
     * </p>
     *
     * @param format SLF4J format string with {@code {}} placeholders (e.g., {@code "User id: {}"})
     * @param arguments values to fill placeholders
     */
    default void info(String format, Object... arguments) {
        format = appendRequestId(format);
        getLogger().info( format , arguments );
    }

    /**
     * Logs warn-level message with correlation ID and debug stack capture.
     * <p>
     * Always logs to {@link #debugStack} (regardless of debug mode), then logs to SLF4J logger at warn 
     * level with correlation ID prepended.
     * </p>
     *
     * @param format SLF4J format string with {@code {}} placeholders (e.g., {@code "User id: {}"})
     * @param arguments values to fill placeholders
     */
    default void warn(String format, Object... arguments) {
        logToDebugStack( null, format , false, arguments );
        format = appendRequestId(format);
        getLogger().warn( format , arguments );
    }

    /**
     * Logs warn-level message with exception stack trace.
     * <p>
     * Logs message and exception to {@link #debugStack} with full stack trace captured, then logs to 
     * SLF4J logger at warn level with correlation ID prepended.
     * </p>
     *
     * @param message warning message
     * @param throwable exception to log with stack trace
     */
    default void warn(String message, Throwable throwable) {
        logToDebugStack(throwable, message, false);
        message = appendRequestId(message);
        getLogger().warn( message , throwable );
    }

    /**
     * Logs error-level message with audit UI link and emits error event.
     * <p>
     * Logs to {@link #debugStack}, logs to SLF4J logger at error level with audit UI link appended, 
     * emits {@code APPLICATION_ERROR} event to {@link ApplicationEventService} for notification system 
     * integration. Prevents recursion via {@link #isInLoggingLoop} ThreadLocal.
     * </p>
     *
     * @param format SLF4J format string with {@code {}} placeholders (e.g., {@code "User id: {}"})
     * @param arguments values to fill placeholders
     * @see #emitErrorLogNotificationEvent(Throwable, String, Object...)
     */
    default void error(String format, Object... arguments) {
        logToDebugStack(null, format , false, arguments );
        format = appendRequestId(format, true);
        getLogger().error( format , arguments );
        emitErrorLogNotificationEvent(null, format, arguments);
    }


    /**
     * Logs error with exception, audit UI link, and error event emission.
     * <p>
     * Logs message and exception to {@link #debugStack} with full stack trace, logs to SLF4J logger at 
     * error level with audit UI link, emits error event, creates error audit via 
     * {@link AuditService#createErrorAuditForException(Throwable, String)}.
     * </p>
     *
     * @param throwable exception to log with stack trace
     * @param format SLF4J format string with {@code {}} placeholders
     * @param arguments values to fill placeholders
     */
    default void error(Throwable throwable, String format, Object... arguments) {
        logToDebugStack(throwable, format , false, arguments );
        format = appendRequestId(format, true);
        getLogger().error( format , arguments );
        emitErrorLogNotificationEvent(throwable, format, arguments);
    }

    /**
     * Logs error message with exception and emits error event.
     * <p>
     * Logs message and exception to {@link #debugStack}, logs to SLF4J logger at error level with audit 
     * UI link, emits error event to {@link ApplicationEventService}.
     * </p>
     *
     * @param message error message
     * @param throwable exception to log with stack trace
     */
    default void error(String message, Throwable throwable) {
        logToDebugStack( throwable, message, false );
        message = appendRequestId(message, true);
        getLogger().error( message , throwable );
        emitErrorLogNotificationEvent(throwable, message);
    }

    /**
     * Retrieves in-memory debug log entries.
     * <p>
     * Returns {@link #debugStack} for runtime inspection of captured debug messages. Used by management 
     * interfaces (JMX or MBean) to view recent debug logs.
     * </p>
     * <p>
     * Excluded from JSON serialization via {@link JsonIgnore} to avoid exposing large debug history.
     * </p>
     *
     * @return map of correlation ID to debug messages from debugStack
     */
    @JsonIgnore
    default Map<String, String> getDebugEntries() {
        return debugStack;
    }

    /**
     * Validates object is not null.
     * <p>
     * Delegates to {@link Assert#notNull(Object)}. Convenience assertion method.
     * </p>
     *
     * @param o object to validate
     * @throws IllegalArgumentException if object is {@code null}
     */
    default void notNull(Object o) {
        Assert.notNull( o, "Object must not be null" );
    }

    /**
     * Validates boolean is true.
     * <p>
     * Delegates to {@link Assert#isTrue(boolean)}. Convenience assertion method.
     * </p>
     *
     * @param b Boolean to validate
     * @throws IllegalArgumentException if boolean is {@code false} or {@code null}
     */
    default void isTrue(Boolean b) {
        Assert.isTrue( b, "Condition must be true" );
    }

    /**
     * Lists all classes with registered loggers.
     * <p>
     * Returns {@link #availableLoggers} list. Used by {@code DebugLogsDecoratorWithRequestId.collectLoggerNames()} 
     * to enumerate logger classes for management interfaces.
     * </p>
     * <p>
     * Excluded from JSON serialization via {@link JsonIgnore}.
     * </p>
     *
     * @return list of Class objects that have called {@link #getLogger()}
     */
    @JsonIgnore
    default List<Class> getAvailableLoggers() {
        return availableLoggers;
    }

    /**
     * Retrieves set of classes with debug mode enabled.
     * <p>
     * Returns {@link #debugLoggers} set. Used by management interfaces to inspect which classes have 
     * debug mode enabled.
     * </p>
     * <p>
     * Excluded from JSON serialization via {@link JsonIgnore}.
     * </p>
     *
     * @return set of Class objects in debug mode (logging to {@link #debugStack})
     */
    @JsonIgnore
    default Set<Class> getDebugLoggers() {
        return debugLoggers;
    }

    /**
     * Emits error event to ApplicationEventService and creates audit trail.
     * <p>
     * Formats message, checks {@link #isInLoggingLoop} ThreadLocal to prevent recursion. If not in loop, 
     * sets flag, emits {@code APPLICATION_ERROR} event with {@link NotificationDto}, creates error audit 
     * via {@link AuditService#createErrorAuditForException(Throwable, String)}, then clears flag. If in 
     * loop, logs error about recursion and returns.
     * </p>
     * <p>
     * Integrates logging with notification and audit subsystems. Recursion prevention critical for errors 
     * during event emission or audit creation.
     * </p>
     *
     * @param throwable exception to include in audit, or {@code null}
     * @param format error message format string
     * @param arguments format string arguments
     * @see ApplicationEventService#emitEvent(String, Object)
     * @see AuditService#createErrorAuditForException(Throwable, String)
     */
    default void emitErrorLogNotificationEvent(Throwable throwable, String format, Object... arguments){
        ApplicationEventService applicationEventService = ApplicationEventService.getApplicationEventService();
        if(applicationEventService != null){
            String message = formatMessage(format, arguments);
            if (isInLoggingLoop.get()) {
                String errorMessage = "Logger fell in loop, due to some exceptions. Nested error: \n" + format;
                getLogger().error(errorMessage, arguments );
                logToDebugStack(null, errorMessage , false, arguments );
            } else {
                isInLoggingLoop.set(true);
                applicationEventService.emitEvent(ApplicationEvent.APPLICATION_ERROR, new NotificationDto(message, Notification.NotificationType.ERROR));
                AuditService.createErrorAuditForException(throwable, message);
                isInLoggingLoop.set(false);
            }
        }
    }

}
