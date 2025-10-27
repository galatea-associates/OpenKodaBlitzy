/*
MIT License

Copyright (c) 2014-2022, Codedose CDX Sp. z o.o. Sp. K. <stratoflow.com>

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

package com.openkoda.core.flow;

import com.openkoda.core.flow.mbean.LoggingEntriesStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.util.Assert;
import org.springframework.util.ResizableByteArrayOutputStream;

import java.io.PrintStream;
import java.util.*;

/**
 * Logging mixin interface providing default SLF4J logging methods and in-memory debug stack for diagnostics.
 * <p>
 * This interface simplifies logging in classes by providing out-of-box logging capabilities without requiring
 * manual logger instantiation. Classes implementing this interface automatically gain access to debug, trace,
 * info, warn, and error logging methods with SLF4J placeholder support.
 * </p>
 * <p>
 * The interface maintains an in-memory debug stack that captures timestamped log entries for JMX exposure,
 * enabling real-time diagnostics and troubleshooting via JMX monitoring tools. Debug stack entries are captured
 * for classes registered in the debugLoggers set and for all warn/error level messages.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * public class MyService implements LoggingComponent {
 *     public void processUser(Long userId) {
 *         debug("Processing user: {}", userId);
 *         // Service logic
 *     }
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>Thread Safety Warning:</b> Static collections (loggers, availableLoggers, debugLoggers) are not synchronized.
 * Concurrent modification during logger registration may cause race conditions in multi-threaded environments.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @see LoggingEntriesStack
 * @see Logger
 * @since 1.7.1
 */
public interface LoggingComponent {

   /**
    * Cache of SLF4J loggers keyed by implementing class.
    * <p>
    * <b>Thread Safety:</b> This map is not synchronized. Concurrent access during logger creation
    * may result in race conditions. Consider using concurrent collections if thread-safety is required.
    * </p>
    */
   static final Map<Class, Logger> loggers = new HashMap<Class, Logger>();
   
   /**
    * List of all classes that have created loggers via this interface.
    * <p>
    * Used for runtime introspection of active loggers in the application.
    * <b>Thread Safety:</b> This list is not synchronized.
    * </p>
    */
   static final List<Class> availableLoggers = new ArrayList<>();
   
   /**
    * Set of classes configured to capture debug stack traces.
    * <p>
    * Classes in this set have their debug() and trace() calls captured to the in-memory debug stack
    * for JMX exposure. Use this for targeted diagnostic logging without full application debug mode.
    * <b>Thread Safety:</b> This set is not synchronized.
    * </p>
    */
   static final Set<Class> debugLoggers = new HashSet<>();
   
   /**
    * Dedicated logger for JMX debug stack entries.
    * <p>
    * All messages logged to the debug stack are also sent to this logger for persistence and
    * external log aggregation systems.
    * </p>
    */
   static final Logger debugLogger = LoggerFactory.getLogger( "jmxDebug" );

   /**
    * In-memory circular buffer capturing timestamped debug entries for JMX exposure.
    * <p>
    * Maintains up to 500 most recent log entries with timestamps for real-time diagnostics.
    * Entries are captured from warn/error level logs and from classes registered in debugLoggers.
    * </p>
    *
    * @see LoggingEntriesStack
    */
   static final LoggingEntriesStack<Date> debugStack = new LoggingEntriesStack<>(500);

   /**
    * Returns the SLF4J logger for the implementing class, optionally creating it if not cached.
    * <p>
    * This method provides lazy logger instantiation and caching. Loggers are keyed by the implementing
    * class type to ensure each class gets its own logger instance with appropriate naming.
    * </p>
    * <p>
    * Can be overridden to provide a pre-created logger or custom logger configuration.
    * </p>
    *
    * @param createIfNotExists if {@code true}, creates and caches a new logger if one doesn't exist;
    *                          if {@code false}, returns {@code null} when logger not found in cache
    * @return SLF4J logger instance for the implementing class, or {@code null} if not cached
    *         and createIfNotExists is {@code false}
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
    * Returns the SLF4J logger for the implementing class, creating it if not already cached.
    * <p>
    * Convenience method that calls {@link #getLogger(boolean)} with {@code true}.
    * The logger is automatically created on first access and cached for subsequent calls.
    * </p>
    *
    * @return SLF4J logger instance for the implementing class, never {@code null}
    */
   default Logger getLogger() {
      return getLogger( true );
   }

   /**
    * Logs a debug-level message with SLF4J placeholder support.
    * <p>
    * If the implementing class is registered in debugLoggers, the message is also captured
    * to the in-memory debug stack for JMX exposure and diagnostics.
    * </p>
    *
    * @param format format string with SLF4J placeholders ({}), example: "User id: {}"
    * @param arguments values to substitute into placeholders in the format string
    * @see #isDebugLogger()
    * @see #logToDebugStack(Throwable, String, Object...)
    */
   default void debug(String format, Object... arguments) {
      Logger l = getLogger();
      l.debug( format , arguments );
      if ( isDebugLogger() ) {
         logToDebugStack(null, format , arguments );
      }
   }

   /**
    * Formats a message string by substituting SLF4J placeholders with provided arguments.
    * <p>
    * Uses SLF4J's MessageFormatter to safely substitute {} placeholders with argument values.
    * This method handles null arguments and prevents placeholder mismatch exceptions.
    * </p>
    *
    * @param format format string with SLF4J placeholders ({})
    * @param arguments values to substitute into placeholders
    * @return formatted message string with placeholders replaced by argument values
    */
   default String formatMessage(String format, Object... arguments) {
      FormattingTuple ft = MessageFormatter.arrayFormat( format , arguments );
      return ft.getMessage();
   }

   /**
    * Logs a message to the in-memory debug stack with timestamp and optional exception stack trace.
    * <p>
    * This method captures log entries for JMX exposure, enabling real-time diagnostics without
    * accessing log files. The message is formatted with SLF4J placeholders, and if a throwable
    * is provided, its stack trace is appended to the debug entry.
    * </p>
    * <p>
    * Entries are stored in a circular buffer (debugStack) limited to 500 most recent entries.
    * All entries are also logged to the "jmxDebug" logger for persistence.
    * </p>
    *
    * @param t optional throwable whose stack trace will be appended to the message, or {@code null}
    * @param message format string with SLF4J placeholders ({})
    * @param arguments values to substitute into message placeholders
    * @see #debugStack
    * @see #debugLogger
    */
   default void logToDebugStack(Throwable t, String message, Object... arguments) {
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
      debugStack.log(new Date(), getClass().getSimpleName() + " - " + message );
   }

   /**
    * Logs a trace-level message with SLF4J placeholder support.
    * <p>
    * If the implementing class is registered in debugLoggers, the message is also captured
    * to the in-memory debug stack for JMX exposure and diagnostics.
    * </p>
    *
    * @param format format string with SLF4J placeholders ({}), example: "Entering method with param: {}"
    * @param arguments values to substitute into placeholders in the format string
    * @see #isDebugLogger()
    * @see #logToDebugStack(Throwable, String, Object...)
    */
   default void trace(String format, Object... arguments) {
      Logger l = getLogger();
      l.trace( format , arguments );
      if ( isDebugLogger() ) {
         logToDebugStack( null, format , arguments );
      }
   }

   /**
    * Checks whether the implementing class is registered for debug stack capture.
    * <p>
    * Classes in the debugLoggers set have their debug() and trace() calls captured
    * to the in-memory debug stack for JMX diagnostics.
    * </p>
    *
    * @return {@code true} if this class is registered in debugLoggers, {@code false} otherwise
    * @see #debugLoggers
    */
   default boolean isDebugLogger() {
      return debugLoggers.contains( getClass() );
   }

   /**
    * Logs an info-level message with SLF4J placeholder support.
    * <p>
    * Info-level messages are not captured to the debug stack.
    * </p>
    *
    * @param format format string with SLF4J placeholders ({}), example: "User id: {}"
    * @param arguments values to substitute into placeholders in the format string
    */
   default void info(String format, Object... arguments) {
      getLogger().info( format , arguments );
   }

   /**
    * Logs a warning-level message with SLF4J placeholder support.
    * <p>
    * Warning messages are automatically captured to the in-memory debug stack for JMX exposure,
    * regardless of whether the implementing class is in debugLoggers.
    * </p>
    *
    * @param format format string with SLF4J placeholders ({}), example: "Invalid configuration: {}"
    * @param arguments values to substitute into placeholders in the format string
    * @see #logToDebugStack(Throwable, String, Object...)
    */
   default void warn(String format, Object... arguments) {
      getLogger().warn( format , arguments );
      logToDebugStack( null, format , arguments );
   }

   /**
    * Logs a warning-level message with exception stack trace.
    * <p>
    * The message and exception stack trace are captured to the in-memory debug stack for JMX exposure.
    * </p>
    *
    * @param message warning message to log
    * @param throwable exception providing stack trace context for the warning
    * @see #logToDebugStack(Throwable, String, Object...)
    */
   default void warn(String message, Throwable throwable) {
      getLogger().warn( message , throwable );
      logToDebugStack(throwable, message);
   }

   /**
    * Logs an error-level message with SLF4J placeholder support.
    * <p>
    * Error messages are automatically captured to the in-memory debug stack for JMX exposure,
    * regardless of whether the implementing class is in debugLoggers.
    * </p>
    *
    * @param format format string with SLF4J placeholders ({}), example: "Failed to process: {}"
    * @param arguments values to substitute into placeholders in the format string
    * @see #logToDebugStack(Throwable, String, Object...)
    */
   default void error(String format, Object... arguments) {
      getLogger().error( format , arguments );
      logToDebugStack(null, format , arguments );
   }


   /**
    * Logs an error-level message with exception, using SLF4J placeholder support.
    * <p>
    * Logs both the formatted message and the exception cause separately, then captures
    * the message and full exception stack trace to the in-memory debug stack for JMX exposure.
    * </p>
    *
    * @param throwable exception providing stack trace and error context
    * @param format format string with SLF4J placeholders ({}), example: "Database error for user: {}"
    * @param arguments values to substitute into placeholders in the format string
    * @see #logToDebugStack(Throwable, String, Object...)
    */
   default void error(Throwable throwable, String format, Object... arguments) {
      getLogger().error( format , arguments );
      getLogger().error("[error] cause:",  throwable );
      logToDebugStack(throwable, format , arguments );
   }

   /**
    * Logs an error-level message with exception stack trace.
    * <p>
    * The message and exception stack trace are captured to the in-memory debug stack for JMX exposure.
    * </p>
    *
    * @param message error message to log
    * @param throwable exception providing stack trace and error context
    * @see #logToDebugStack(Throwable, String, Object...)
    */
   default void error(String message, Throwable throwable) {
      getLogger().error( message , throwable );
      logToDebugStack( throwable, message );
   }

   /**
    * Returns the in-memory debug stack containing timestamped log entries for JMX exposure.
    * <p>
    * The debug stack captures up to 500 most recent entries from warn/error level logs and
    * from classes registered in debugLoggers. Entries are keyed by timestamp with formatted
    * messages including class names.
    * </p>
    *
    * @return map of timestamps to log message strings from the debug stack
    * @see LoggingEntriesStack
    * @see #debugStack
    */
   default Map<Date, String> getDebugEntries() {
      return debugStack;
   }

   /**
    * Asserts that the provided object is not null using Spring's Assert utility.
    * <p>
    * Convenience method for inline null-checking in method implementations.
    * </p>
    *
    * @param o object to check for null
    * @throws IllegalArgumentException if the object is null
    */
   default void notNull(Object o) {
      Assert.notNull( o );
   }

   /**
    * Asserts that the provided boolean condition is true using Spring's Assert utility.
    * <p>
    * Convenience method for inline condition validation in method implementations.
    * </p>
    *
    * @param b boolean condition to validate
    * @throws IllegalArgumentException if the condition is false
    */
   default void isTrue(Boolean b) {
      Assert.isTrue( b );
   }

   /**
    * Returns the list of all classes that have created loggers via this interface.
    * <p>
    * Used for runtime introspection of active loggers in the application. Classes are added
    * to this list when their logger is first created via {@link #getLogger(boolean)}.
    * </p>
    * <p>
    * <b>Thread Safety:</b> This list is not synchronized.
    * </p>
    *
    * @return list of classes with instantiated loggers
    * @see #availableLoggers
    */
   default List<Class> getAvailableLoggers() {
      return availableLoggers;
   }

   /**
    * Returns the set of classes configured to capture debug stack traces.
    * <p>
    * Classes in this set have their debug() and trace() calls captured to the in-memory
    * debug stack for JMX exposure. Add classes to this set to enable targeted diagnostic
    * logging without full application debug mode.
    * </p>
    * <p>
    * <b>Thread Safety:</b> This set is not synchronized.
    * </p>
    *
    * @return set of classes with debug stack capture enabled
    * @see #debugLoggers
    * @see #isDebugLogger()
    */
   default Set<Class> getDebugLoggers() {
      return debugLoggers;
   }

}
