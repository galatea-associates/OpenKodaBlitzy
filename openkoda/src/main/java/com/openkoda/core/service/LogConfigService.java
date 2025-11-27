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

package com.openkoda.core.service;

import com.openkoda.core.tracker.DebugLogsDecoratorWithRequestId;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Runtime logging configuration service that enables dynamic per-class debug mode toggles.
 * <p>
 * This service provides a facade for {@link DebugLogsDecoratorWithRequestId} functionality,
 * enabling runtime control of debug logging on a per-class basis. It supports individual
 * and bulk enable/disable operations for debug loggers, manages debug entry buffer configuration,
 * and provides configuration persistence through the {@link #saveConfig(int, Collection)} method.

 * <p>
 * Debug logging operates by maintaining a set of active logger classes whose debug output is
 * captured in a request-scoped buffer. When debug mode is enabled for a class, its debug log
 * entries are recorded and can be retrieved via {@link #getDebugEntriesAsList()} for runtime
 * inspection and troubleshooting.

 * <p>
 * Example usage:
 * <pre>{@code
 * service.turnOnDebugForClassNames(Arrays.asList("com.openkoda.controller.UserController"));
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see DebugLogsDecoratorWithRequestId
 * @see LoggingComponentWithRequestId
 */
@Service
public class LogConfigService implements LoggingComponentWithRequestId {

    /**
     * Injected decorator managing per-class logger states and debug entry buffer.
     * Provides the underlying implementation for all debug logging configuration operations.
     */
    @Inject
    private DebugLogsDecoratorWithRequestId debugLogsDecorator;

    /**
     * Returns the current debug buffer size limit from the debug stack.
     * <p>
     * The buffer size determines the maximum number of debug log entries
     * that can be retained in memory. When this limit is exceeded, older
     * entries are evicted to make room for new entries.

     *
     * @return the current maximum number of debug entries that can be stored
     * @see #setMaxEntries(int)
     */
    public int getMaxEntries() {
        debug("[getMaxEntries]");
        return debugLogsDecorator.debugStack.getMaxEntries();
    }

    /**
     * Sets the maximum debug buffer capacity.
     * <p>
     * Configures the maximum number of debug log entries that can be stored
     * in the debug stack. When the buffer reaches this capacity, older entries
     * are automatically evicted when new entries are added, following a
     * first-in-first-out (FIFO) eviction policy.

     *
     * @param maxBuffer the maximum number of debug entries to retain; must be positive
     * @return {@code true} if the buffer size was successfully updated, {@code false} otherwise
     * @see #getMaxEntries()
     */
    public boolean setMaxEntries(int maxBuffer) {
        debug("[setMaxEntries] {}", maxBuffer);
        return debugLogsDecorator.setMaxEntries(maxBuffer);
    }

    /**
     * Enables debug logging for the specified class by name string.
     * <p>
     * Activates debug mode for the logger associated with the given class name,
     * adding it to the set of active debug loggers. Once enabled, debug log
     * entries from this class will be captured in the debug buffer for inspection.

     *
     * @param classname the fully qualified class name to enable debug logging for; must not be null
     * @return {@code true} if debug mode was successfully enabled, {@code false} otherwise
     * @see #turnOnDebugModeForLoggerClass(Class)
     * @see #turnOffDebugModeForLoggerClassname(String)
     */
    public boolean turnOnDebugModeForLoggerClassname(String classname) {
        debug("[turnOnDebugModeForLoggerClassname] {}", classname);
        return debugLogsDecorator.turnOnDebugModeForLoggerClassname(classname);
    }

    /**
     * Enables debug logging for the specified class using a type-safe Class reference.
     * <p>
     * This is a type-safe variant of {@link #turnOnDebugModeForLoggerClassname(String)}
     * that accepts a Class object rather than a string. It provides compile-time type
     * checking and avoids potential typos in class names.

     *
     * @param c the Class object to enable debug logging for; must not be null
     * @return {@code true} if debug mode was successfully enabled, {@code false} otherwise
     * @see #turnOnDebugModeForLoggerClassname(String)
     * @see #turnOffDebugModeForLoggerClass(Class)
     */
    public boolean turnOnDebugModeForLoggerClass(Class c) {
        debug("[turnOnDebugModeForLoggerClass] {}", c);
        return debugLogsDecorator.turnOnDebugModeForLoggerClass(c);
    }

    /**
     * Disables debug logging for the specified class by name string.
     * <p>
     * Deactivates debug mode for the logger associated with the given class name,
     * removing it from the set of active debug loggers. Debug log entries from
     * this class will no longer be captured after this operation.

     *
     * @param classname the fully qualified class name to disable debug logging for; must not be null
     * @return {@code true} if debug mode was successfully disabled, {@code false} otherwise
     * @see #turnOffDebugModeForLoggerClass(Class)
     * @see #turnOnDebugModeForLoggerClassname(String)
     */
    public boolean turnOffDebugModeForLoggerClassname(String classname) {
        debug("[turnOffDebugModeForLoggerClassname] {}", classname);
        return debugLogsDecorator.turnOffDebugModeForLoggerClassname(classname);
    }

    /**
     * Disables debug logging for the specified class using a type-safe Class reference.
     * <p>
     * This is a type-safe variant of {@link #turnOffDebugModeForLoggerClassname(String)}
     * that accepts a Class object rather than a string. It provides compile-time type
     * checking and avoids potential typos in class names.

     *
     * @param c the Class object to disable debug logging for; must not be null
     * @return {@code true} if debug mode was successfully disabled, {@code false} otherwise
     * @see #turnOffDebugModeForLoggerClassname(String)
     * @see #turnOnDebugModeForLoggerClass(Class)
     */
    public boolean turnOffDebugModeForLoggerClass(Class c) {
        debug("[turnOffDebugModeForLoggerClass] {}", c);
        return debugLogsDecorator.turnOffDebugModeForLoggerClass(c);
    }
    /**
     * Removes all active debug loggers and resets to default state.
     * <p>
     * Clears the entire set of classes that have debug logging enabled,
     * effectively disabling debug mode for all loggers. This operation
     * does not affect the debug buffer itself or previously captured entries.

     *
     * @return {@code true} indicating the operation completed successfully
     * @see #getDebugLoggers()
     */
    public boolean clearDebugLoggers() {
        debug("[clearDebugLoggers]");
        debugLogsDecorator.clearDebugLoggers();
        return true;
    }

    /**
     * Returns an array of all logger names available in the system.
     * <p>
     * Collects the names of all loggers that are available for debug mode
     * configuration. This includes loggers for all classes in the application
     * context that participate in the logging framework.

     *
     * @return an array containing all available logger names as strings
     * @see #getAvailableLoggers()
     */
    public String[] collectLoggerNames() {
        debug("[collectLoggerNames] {}");
        return debugLogsDecorator.collectLoggerNames();
    }

    /**
     * Enables debug logging for a collection of classes in bulk.
     * <p>
     * Iterates through the provided collection and enables debug mode for each
     * class. This method returns {@code true} only if all operations succeed.
     * Note that partial failures are possible - some classes may have debug
     * mode enabled while others fail.

     *
     * @param classCollection the collection of Class objects to enable debug logging for; must not be null
     * @return {@code true} if debug mode was successfully enabled for all classes, {@code false} if any operation failed
     * @see #turnOnDebugModeForLoggerClass(Class)
     * @see #turnOnDebugForClassNames(Collection)
     */
    public boolean turnOnDebugForClasses(Collection<Class> classCollection) {
        debug("[turnOnDebugForClasses]");
        boolean r = true;
        for (Class c : classCollection) {
            r = turnOnDebugModeForLoggerClass(c) && r;
        }
        return r;
    }

    /**
     * Enables debug logging for a collection of classes by name in bulk.
     * <p>
     * Iterates through the provided collection of class names and enables debug
     * mode for each. This method returns {@code true} only if all operations succeed.
     * Note that partial failures are possible - some classes may have debug mode
     * enabled while others fail.

     *
     * @param classCollection the collection of fully qualified class names to enable debug logging for; must not be null
     * @return {@code true} if debug mode was successfully enabled for all classes, {@code false} if any operation failed
     * @see #turnOnDebugModeForLoggerClassname(String)
     * @see #turnOnDebugForClasses(Collection)
     */
    public boolean turnOnDebugForClassNames(Collection<String> classCollection) {
        debug("[turnOnDebugForClassNames]");
        boolean r = true;
        for (String name : classCollection) {
            r = turnOnDebugModeForLoggerClassname(name) && r;
        }
        return r;
    }

    /**
     * Disables debug logging for a collection of classes in bulk.
     * <p>
     * Iterates through the provided collection and disables debug mode for each
     * class. This method returns {@code true} only if all operations succeed.
     * Note that partial failures are possible - some classes may have debug
     * mode disabled while others fail.

     *
     * @param classCollection the collection of Class objects to disable debug logging for; must not be null
     * @return {@code true} if debug mode was successfully disabled for all classes, {@code false} if any operation failed
     * @see #turnOffDebugModeForLoggerClass(Class)
     * @see #turnOffDebugForClassNames(Collection)
     */
    public boolean turnOffDebugForClasses(Collection<Class> classCollection) {
        debug("[turnOffDebugForClasses]");
        boolean r = true;
        for (Class c : classCollection) {
            r = turnOffDebugModeForLoggerClass(c) && r;
        }
        return r;
    }

    /**
     * Disables debug logging for a collection of classes by name in bulk.
     * <p>
     * Iterates through the provided collection of class names and disables debug
     * mode for each. This method returns {@code true} only if all operations succeed.
     * Note that partial failures are possible - some classes may have debug mode
     * disabled while others fail.

     *
     * @param classCollection the collection of fully qualified class names to disable debug logging for; must not be null
     * @return {@code true} if debug mode was successfully disabled for all classes, {@code false} if any operation failed
     * @see #turnOffDebugModeForLoggerClassname(String)
     * @see #turnOffDebugForClasses(Collection)
     */
    public boolean turnOffDebugForClassNames(Collection<String> classCollection) {
        debug("[turnOffDebugForClassNames]");
        boolean r = true;
        for (String name : classCollection) {
            r = turnOffDebugModeForLoggerClassname(name) && r;
        }
        return r;
    }

    /**
     * Reconciliation operation that sets active debug loggers to exactly match the provided collection.
     * <p>
     * This method performs a diff-based reconciliation: it disables debug mode for any loggers
     * that are currently active but not in the provided collection, and enables debug mode for
     * any loggers in the collection that are not currently active. The end result is that the
     * set of active debug loggers exactly matches the provided collection.

     *
     * @param classes the collection of Class objects representing the desired set of active debug loggers; must not be null
     * @return {@code true} if all operations succeeded, {@code false} if any enable or disable operation failed
     * @see #setDebugForClassNames(Collection)
     */
    public boolean setDebugForClasses(Collection<Class> classes) {
        debug("[setDebugForClasses]");
        boolean result = true;
        Set<String> classSet = classes.stream().map(Class::getName).collect(Collectors.toSet());
        return setDebugForClassNames(classSet);
    }

    /**
     * String-based reconciliation variant using diff algorithm to set active debug loggers.
     * <p>
     * This method implements a set reconciliation algorithm that compares the currently
     * active debug loggers with the desired set provided in the {@code classes} parameter.
     * It performs the minimum set of operations needed to achieve the desired state:
     * <ul>
     *   <li>Disables debug mode for loggers not in the provided collection</li>
     *   <li>Enables debug mode for loggers in the collection that are not currently active</li>
     *   <li>Leaves unchanged any loggers that are both active and in the provided collection</li>
     * </ul>

     *
     * @param classes the collection of fully qualified class names representing the desired set of active debug loggers; must not be null
     * @return {@code true} if all operations succeeded, {@code false} if any enable or disable operation failed
     * @see #setDebugForClasses(Collection)
     */
    public boolean setDebugForClassNames(Collection<String> classes) {
        debug("[setDebugForClassNames]");
        boolean result = true;
        Set<String> classSet = new HashSet<>(classes);
        Set<String> workingClasses = this.debugLogsDecorator.getDebugLoggers().stream()
                .map(Class::getName)
                .collect(Collectors.toSet());
        for (String name : workingClasses) {
            if (!classSet.contains(name)) {
                result = turnOffDebugModeForLoggerClassname(name) && result;
            } else {
                classSet.remove(name);
            }
        }
        for (String name : classSet) {
            result = turnOnDebugModeForLoggerClassname(name) && result;
        }
        return result;
    }

    /**
     * Atomic configuration save combining buffer size and logger set updates.
     * <p>
     * This method provides a convenient way to persist the complete debug logging
     * configuration in a single operation. It combines setting the debug buffer size
     * and reconciling the active debug loggers to match the provided collection.
     * Both operations are performed, and the method returns {@code true} only if
     * both succeed.

     *
     * @param buffer the desired debug buffer size; must be positive
     * @param classes the collection of fully qualified class names for active debug loggers; must not be null
     * @return {@code true} if both buffer size update and logger reconciliation succeeded, {@code false} if either failed
     * @see #setMaxEntries(int)
     * @see #setDebugForClassNames(Collection)
     */
    public boolean saveConfig(int buffer, Collection<String> classes) {
        debug("[saveConfig]");
        boolean result = true;
        result = setMaxEntries(buffer) && result;
        result = setDebugForClassNames(classes) && result;
        return result;
    }

    /**
     * Returns chronologically reversed list of debug entries as key-value pairs.
     * <p>
     * Retrieves the captured debug log entries from the buffer and returns them
     * as a list of Map.Entry pairs where the key is typically a timestamp or
     * sequence identifier and the value is the debug message. The list is reversed
     * so that the most recent entries appear first, facilitating recent-first
     * inspection during troubleshooting.

     *
     * @return a list of debug entries as Map.Entry&lt;String, String&gt; pairs, with most recent entries first
     * @see #getMaxEntries()
     */
    public List<Map.Entry<String, String>> getDebugEntriesAsList() {
        debug("[getDebugEntriesAsList]");
        ArrayList<Map.Entry<String, String>> a = new ArrayList<>(debugLogsDecorator.getDebugEntries().entrySet());
        Collections.reverse(a);
        return a;
    }

    /**
     * Returns the set of currently active debug logger Class objects.
     * <p>
     * Retrieves the collection of Class objects for which debug logging is
     * currently enabled. This provides a type-safe representation of the
     * active debug configuration.

     *
     * @return a set of Class objects representing loggers with debug mode enabled
     * @see #getDebugLoggersNames()
     */
    public Set<Class> getDebugLoggers() {
        debug("[getDebugLoggers]");
        return debugLogsDecorator.getDebugLoggers();
    }

    /**
     * Returns the set of active logger class names as strings.
     * <p>
     * Retrieves the collection of fully qualified class names for which debug
     * logging is currently enabled. This provides a string-based representation
     * of the active debug configuration, useful for persistence or display purposes.

     *
     * @return a set of fully qualified class names representing loggers with debug mode enabled
     * @see #getDebugLoggers()
     */
    public Set<String> getDebugLoggersNames() {
        debug("[getDebugLoggers]");
        return debugLogsDecorator.getDebugLoggers().stream().map(Class::getName).collect(Collectors.toSet());
    }

    /**
     * Returns a list of all logger classes available for debugging.
     * <p>
     * Retrieves the complete collection of Class objects that are available
     * for debug mode configuration. This includes all classes in the application
     * context that participate in the logging framework, regardless of whether
     * debug mode is currently enabled for them.

     *
     * @return a list of all Class objects available for debug logging configuration
     * @see #collectLoggerNames()
     */
    public List<Class> getAvailableLoggers() {
        debug("[getAvailableLoggers]");
        return debugLogsDecorator.getAvailableLoggers();
    }
}
