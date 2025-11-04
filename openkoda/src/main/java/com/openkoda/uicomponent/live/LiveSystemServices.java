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

package com.openkoda.uicomponent.live;

import com.openkoda.core.customisation.ServerJSProcessRunner;
import com.openkoda.core.customisation.ServerJSRunner;
import com.openkoda.uicomponent.SystemServices;
import jakarta.inject.Inject;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;


/**
 * Non-cloud implementation of {@link SystemServices} providing unrestricted server-side JavaScript
 * execution and system command execution capabilities for local and development environments.
 * <p>
 * This implementation is active when the "cloud" profile is NOT active (configured via
 * {@code @Profile("!cloud")}), making it suitable for local development and testing environments
 * where unrestricted access to system resources is acceptable.
 * 
 * <p>
 * LiveSystemServices delegates JavaScript execution to {@link ServerJSRunner} for evaluating
 * ServerJS scripts and to {@link ServerJSProcessRunner} for executing operating system commands.
 * All command execution methods provide direct access to system shell commands without restrictions.
 * 
 * <p>
 * <b>SECURITY WARNING:</b> This implementation allows unrestricted command execution and is intended
 * ONLY for local development and non-production environments. For production and cloud deployments,
 * use {@code SecureLiveSystemServices} which provides appropriate security restrictions.
 * 
 * <p>
 * Thread-Safety: This class is stateless and thread-safe, delegating to thread-safe service components.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SystemServices
 * @see ServerJSRunner
 * @see ServerJSProcessRunner
 */
@Component
@Profile("!cloud")
public class LiveSystemServices implements SystemServices {

    /**
     * Server-side JavaScript evaluation engine used to execute ServerJS scripts.
     * Injected by Spring dependency injection to evaluate dynamic JavaScript code
     * with provided model context and arguments.
     */
    @Inject ServerJSRunner serverJSRunner;

    /**
     * Runs server-side code (ServerJS) by its name, executing the script within the provided context.
     * <p>
     * As this method is used in JavaScript flows which are dynamic by nature, the return type is
     * Object to support JavaScript's dynamic typing and allow flexible flow composition without
     * compile-time type constraints.
     * 
     *
     * @param serverJsName the name of the ServerJS entity to execute, must correspond to a registered
     *                     server-side code definition (not null)
     * @param model the execution context model containing variables and beans accessible to the script,
     *              may be null or empty if script requires no context
     * @param arguments the list of string arguments passed to the script execution, may be null or
     *                  empty if script requires no arguments
     * @return the result of the ServerJS execution as an Object, type depends on script implementation
     *         and may be null if script produces no result
     */
    @Override
    public Object runServerSideCode(String serverJsName, Map<String, Object> model, List<String> arguments) {
        return serverJSRunner.evaluateServerJsScript(serverJsName, model, arguments, Object.class);
    }

    /**
     * Executes a system command and returns the standard output as an InputStream for streaming access.
     * <p>
     * <b>SECURITY NOTE:</b> This method executes commands without restrictions and should only be
     * used in local/development environments. Command input is not sanitized or validated.
     * 
     *
     * @param command the shell command to execute (e.g., "ls -la", "cat /etc/hosts"), executed
     *                directly in the system shell without escaping or validation
     * @return an InputStream containing the command's standard output, never null but may be empty
     *         if command produces no output
     */
    @Override
    public InputStream runCommandToStream(String command) {
        return ServerJSProcessRunner.commandToInputStream(command);
    }

    /**
     * Executes a system command and returns the standard output as a String for convenient text processing.
     * <p>
     * <b>SECURITY NOTE:</b> This method executes commands without restrictions and should only be
     * used in local/development environments. Command input is not sanitized or validated.
     * 
     *
     * @param command the shell command to execute (e.g., "echo 'Hello'", "pwd"), executed directly
     *                in the system shell without escaping or validation
     * @return a String containing the command's standard output, never null but may be empty if
     *         command produces no output
     */
    @Override
    public String runCommandToString(String command) {
        return ServerJSProcessRunner.commandToString(command);
    }
    /**
     * Executes a system command and returns the standard output as a byte array for binary data processing.
     * <p>
     * <b>SECURITY NOTE:</b> This method executes commands without restrictions and should only be
     * used in local/development environments. Command input is not sanitized or validated.
     * 
     *
     * @param command the shell command to execute (e.g., "cat image.png"), executed directly in
     *                the system shell without escaping or validation
     * @return a byte array containing the command's standard output, never null but may be empty
     *         if command produces no output
     */
    @Override
    public byte[] runCommandToByteArray(String command) {
        return ServerJSProcessRunner.commandToByteArray(command);
    }
}
