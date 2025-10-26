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

package com.openkoda.uicomponent;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Service contract for system-level operations including server-side code execution and OS command running.
 * <p>
 * Provides methods for executing ServerJs code with external model and arguments, and offers OS command
 * execution with multiple output formats (stream, string, byte array). Implementations are selected by
 * Spring profile using {@code @Profile("!cloud")} versus {@code @Profile("cloud")} annotations.
 * </p>
 * <p>
 * Security critical: implementations must enforce privilege checks and command sanitization.
 * {@code LiveSystemServices} (non-cloud) allows unrestricted execution, while
 * {@code SecureLiveSystemServices} (cloud) may restrict commands for security.
 * </p>
 * <p>
 * <strong>Security Warning:</strong> Unrestricted command execution exposes OS-level access - use only
 * in trusted environments.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see LiveSystemServices
 * @see SecureLiveSystemServices
 * @see ServerJSRunner
 * @see ServerJSProcessRunner
 */
public interface SystemServices {
    /**
     * Executes ServerJs JavaScript code with external model variables and arguments.
     * <p>
     * Loads ServerJs entity by name, creates JavaScript execution context via {@code ServerJSRunner}
     * or {@code ServerJSProcessRunner}, binds externalModel as global variables, passes externalArguments
     * as function parameters, executes code, and converts JavaScript result to Java Object. May execute
     * in-process or in a separate process depending on implementation.
     * </p>
     * <p>
     * JavaScript code has access to server-side services and database - ensure code is trusted.
     * </p>
     * <p>
     * Example usage:
     * {@code runServerSideCode("dataProcessor", Map.of("input", data), List.of("arg1", "arg2"))}
     * </p>
     * <p>
     * Security: Implementation must validate serverJsName exists and user has execute privileges.
     * </p>
     *
     * @param serverJsName ServerJs entity name to load and execute
     * @param externalModel Map of external variables passed to JavaScript execution context
     * @param externalArguments List of string arguments passed to JavaScript code as function parameters
     * @return Execution result as Object (JavaScript return value converted to Java type)
     * @throws RuntimeException If ServerJs entity not found or execution fails
     */
    Object runServerSideCode(String serverJsName, Map<String, Object> externalModel, List<String> externalArguments);

    /**
     * Executes OS command and returns output as InputStream for streaming scenarios.
     * <p>
     * Executes command via {@code ProcessBuilder} or {@code Runtime.exec}, captures stdout as
     * {@code InputStream}. Suitable for large command outputs or streaming scenarios. Caller is
     * responsible for closing the stream. Stderr may be redirected to stdout or discarded depending
     * on implementation.
     * </p>
     * <p>
     * Example usage: {@code runCommandToStream("cat /var/log/app.log")} returns log file as stream.
     * </p>
     * <p>
     * <strong>Security Warning:</strong> CRITICAL - Command injection risk if command contains
     * unsanitized user input. Implementation should whitelist allowed commands or sanitize parameters.
     * </p>
     * <p>
     * Note: Cloud/production implementations ({@code SecureLiveSystemServices}) may restrict or
     * disable this method.
     * </p>
     *
     * @param command OS command to execute (e.g., 'ls -la', 'ps aux')
     * @return InputStream containing command stdout output
     * @throws RuntimeException If command execution fails or returns non-zero exit code
     */
    InputStream runCommandToStream(String command);

    /**
     * Executes OS command and returns output as String.
     * <p>
     * Executes command, reads stdout to completion, and converts bytes to String using platform
     * default charset. Suitable for small text outputs. Entire output is held in memory.
     * </p>
     * <p>
     * Example usage: {@code runCommandToString("hostname")} returns server hostname as string.
     * </p>
     * <p>
     * <strong>Security Warning:</strong> CRITICAL - Command injection risk if command contains
     * user input.
     * </p>
     * <p>
     * Note: For large outputs, use {@code runCommandToStream} to avoid memory issues.
     * </p>
     *
     * @param command OS command to execute
     * @return Command stdout output as String (platform default charset)
     * @throws RuntimeException If command execution fails or returns non-zero exit code
     */
    String runCommandToString(String command);

    /**
     * Executes OS command and returns output as byte array.
     * <p>
     * Executes command, reads stdout to completion as bytes. Suitable for binary outputs or when
     * charset is unknown. Entire output is held in memory.
     * </p>
     * <p>
     * Example usage: {@code runCommandToByteArray("cat image.png")} returns image bytes.
     * </p>
     * <p>
     * <strong>Security Warning:</strong> CRITICAL - Command injection risk if command contains
     * user input.
     * </p>
     * <p>
     * Note: For large binary outputs, use {@code runCommandToStream} to avoid {@code OutOfMemoryError}.
     * </p>
     *
     * @param command OS command to execute
     * @return Command stdout output as byte array
     * @throws RuntimeException If command execution fails or returns non-zero exit code
     */
    byte[] runCommandToByteArray(String command);
}