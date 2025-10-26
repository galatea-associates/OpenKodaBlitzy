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

package com.openkoda.core.tracker;

import com.openkoda.core.flow.mbean.StatisticsMBean;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.Map;

/**
 * Management decorator that provides runtime operations to enable and disable per-class debug mode.
 * <p>
 * This class implements the {@link StatisticsMBean} interface to expose runtime operations for managing
 * debug logging behavior. It provides operations to enumerate available logger classes, clear debug sets,
 * and configure debugStack retention. The decorator integrates with {@link LoggingComponentWithRequestId}
 * shared state including the debugLoggers set, availableLoggers list, and debugStack.
 * </p>
 * <p>
 * Thread-safety depends on the underlying shared collections from {@link LoggingComponentWithRequestId}.
 * This class enables dynamic control of debug-level logging for specific classes at runtime without
 * requiring application restart or configuration file changes.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * decorator.turnOnDebugModeForLoggerClassname("com.openkoda.service.UserService");
 * </pre>
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @version 1.7.1
 * @since 2019-08-22
 * @see LoggingComponentWithRequestId
 * @see StatisticsMBean
 */
public class DebugLogsDecoratorWithRequestId implements StatisticsMBean, LoggingComponentWithRequestId {

    /**
     * Creates a new debug logs decorator instance.
     * <p>
     * This constructor initializes the decorator which provides management operations for debug logging.
     * The decorator relies on shared state from {@link LoggingComponentWithRequestId} for its operations.
     * </p>
     */
    public DebugLogsDecoratorWithRequestId() {
    }

    /**
     * Enables debug logging for the logger class with the specified fully qualified name.
     * <p>
     * This method validates the classname using {@code StringUtils.isBlank}, then performs a linear scan
     * over registered loggers by comparing {@code Class.getName()}. If the logger is found, it delegates
     * to {@link #turnOnDebugModeForLoggerClass(Class)} to enable debug mode.
     * </p>
     *
     * @param classname the fully qualified class name to enable debug logging for (e.g., "com.openkoda.service.UserService").
     *                  Cannot be blank or null.
     * @return true if debug mode was successfully enabled for the logger, false if classname is blank or not found
     * @see #turnOnDebugModeForLoggerClass(Class)
     */
    public boolean turnOnDebugModeForLoggerClassname(String classname) {
        this.debug("[turnOnDebugModeForLoggerClassname] {}", new Object[]{classname});
        if (StringUtils.isBlank(classname)) {
            return false;
        } else {
            Iterator var2 = loggers.entrySet().iterator();

            Map.Entry e;
            do {
                if (!var2.hasNext()) {
                    return false;
                }

                e = (Map.Entry)var2.next();
            } while(!classname.equals(((Class)e.getKey()).getName()));

            return this.turnOnDebugModeForLoggerClass((Class)e.getKey());
        }
    }

    /**
     * Enables debug logging for the specified logger class.
     * <p>
     * This method adds the class to the shared debugLoggers set, enabling debug-level output for that
     * logger class. Once added, all debug log statements from that logger will be captured and stored.
     * </p>
     *
     * @param c the Class object to enable debug logging for. Cannot be null.
     * @return true if the class was successfully added to debugLoggers set, false if the class is null
     * @see #turnOffDebugModeForLoggerClass(Class)
     */
    public boolean turnOnDebugModeForLoggerClass(Class c) {
        this.debug("[turnOnDebugModeForLoggerClass] {}", new Object[]{c});
        return c == null ? false : debugLoggers.add(c);
    }

    /**
     * Disables debug logging for the logger class with the specified fully qualified name.
     * <p>
     * This method searches registered loggers by name and delegates to {@link #turnOffDebugModeForLoggerClass(Class)}
     * if the logger is found. It performs a linear scan over registered loggers comparing each class name.
     * </p>
     *
     * @param classname the fully qualified class name to disable debug logging for. Cannot be blank or null.
     * @return true if debug mode was successfully disabled for the logger, false if the classname is not found
     * @see #turnOffDebugModeForLoggerClass(Class)
     */
    public boolean turnOffDebugModeForLoggerClassname(String classname) {
        this.debug("[turnOffDebugModeForLoggerClassname] {}", new Object[]{classname});
        Iterator var2 = loggers.entrySet().iterator();

        Map.Entry e;
        do {
            if (!var2.hasNext()) {
                return false;
            }

            e = (Map.Entry)var2.next();
        } while(!classname.equals(((Class)e.getKey()).getName()));

        return this.turnOffDebugModeForLoggerClass((Class)e.getKey());
    }

    /**
     * Disables debug logging for the specified logger class.
     * <p>
     * This method removes the class from the shared debugLoggers set, disabling debug-level output for that
     * logger class. After removal, debug log statements from that logger will no longer be captured.
     * </p>
     *
     * @param c the Class object to disable debug logging for. Cannot be null.
     * @return true if the class was successfully removed from debugLoggers set, false if the class is null
     * @see #turnOnDebugModeForLoggerClass(Class)
     */
    public boolean turnOffDebugModeForLoggerClass(Class c) {
        return c == null ? false : debugLoggers.remove(c);
    }

    /**
     * Clears all debug logger classes from the debugLoggers set.
     * <p>
     * This method disables debug mode for all loggers by removing all classes from the shared debugLoggers set.
     * After calling this method, no debug log statements will be captured from any logger until they are
     * explicitly enabled again.
     * </p>
     *
     * @see #turnOffDebugModeForLoggerClass(Class)
     */
    public void clearDebugLoggers() {
        debugLoggers.clear();
    }

    /**
     * Returns an array of fully qualified class names for all available loggers.
     * <p>
     * This method streams the availableLoggers set, maps each class to its name via {@code Class.getName()},
     * and returns the result as a String array. The returned array contains all logger classes that can be
     * enabled for debug mode.
     * </p>
     *
     * @return array of fully qualified class names for all available loggers. Never null, but may be empty if no loggers are registered.
     */
    public String[] collectLoggerNames() {
        return (String[])availableLoggers.stream().map((a) -> {
            return a.getName();
        }).toArray((x$0) -> {
            return new String[x$0];
        });
    }

    /**
     * Configures the maximum number of entries to retain in the debug stack.
     * <p>
     * This method configures debugStack retention by delegating to {@code debugStack.setMaxEntries}.
     * It controls the in-memory debug log buffer size. When the buffer is full, older entries are discarded
     * to make room for new entries.
     * </p>
     *
     * @param maxEntries the maximum number of entries to retain in debug stack. Must be a positive integer.
     * @return true on successful configuration
     */
    public boolean setMaxEntries(int maxEntries) {
        debugStack.setMaxEntries(maxEntries);
        return true;
    }
}
