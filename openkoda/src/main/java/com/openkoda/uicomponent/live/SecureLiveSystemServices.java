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

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.openkoda.core.customisation.ServerJSRunner;
import com.openkoda.uicomponent.SystemServices;

import jakarta.inject.Inject;

/**
 * Production-safe implementation of {@link SystemServices} interface for cloud/production environments.
 * <p>
 * This implementation is active when the "cloud" Spring profile is enabled and provides security-hardened
 * implementations of system service methods. Unlike {@link LiveSystemServices}, this class restricts or
 * disables all command execution and server-side code execution capabilities to prevent arbitrary code
 * execution in production environments.
 * 
 * <p>
 * All command execution methods ({@link #runCommandToStream(String)}, {@link #runCommandToString(String)},
 * {@link #runCommandToByteArray(String)}) return safe default values (null, empty string, or nullInputStream)
 * rather than executing system commands. The {@link #runServerSideCode(String, Map, List)} method also
 * returns null to prevent dynamic code execution.
 * 
 * <p>
 * <strong>SECURITY NOTE:</strong> This implementation is designed for cloud and production deployments where
 * arbitrary command and code execution poses significant security risks. For development and local environments
 * with full command execution capabilities, use {@link LiveSystemServices} with appropriate profiles.
 * 
 * <p>
 * This class is stateless and thread-safe. All operations are no-ops for production safety.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SystemServices
 * @see LiveSystemServices
 */
@Component
@Profile("cloud")
public class SecureLiveSystemServices implements SystemServices {

    /**
     * ServerJS runner instance injected for potential future use in whitelisted operations.
     * <p>
     * Currently unused in this secure implementation. Reserved for future enhancements where specific
     * whitelisted server-side scripts may be permitted in production environments with strict privilege checks.
     * 
     */
    @Inject ServerJSRunner serverJSRunner;

    /**
     * Runs Server side code (AKA ServerJS) by its name.
     * <p>
     * As the method is used in JavaScript flows and these are dynamic by nature,
     * there's no need to care about specific types and the method returns Object type.
     * 
     * <p>
     * <strong>SECURITY NOTE:</strong> In this secure cloud profile implementation, this method always returns
     * {@code null} and does not execute any server-side code. This prevents arbitrary code execution in
     * production environments. For actual server-side code execution, use the development profile with
     * {@link LiveSystemServices}.
     * 
     *
     * @param serverJsName the name of the server side code entity to execute
     * @param model the model map containing context and variables for code execution
     * @param arguments the list of string arguments to pass to the server-side code
     * @return always returns {@code null} in cloud profile for security; no code is executed
     */
    @Override
    public Object runServerSideCode(String serverJsName, Map<String, Object> model, List<String> arguments) {
        return null;
    }

    /**
     * Executes system command and returns standard output as stream.
     * <p>
     * <strong>SECURITY NOTE:</strong> In this secure cloud profile implementation, this method does not
     * execute any system commands. It always returns {@link InputStream#nullInputStream()} to prevent
     * arbitrary command execution in production environments. For actual command execution, use the
     * development profile with {@link LiveSystemServices}.
     * 
     *
     * @param command the Linux/system command that would be executed (ignored in secure implementation)
     * @return always returns {@link InputStream#nullInputStream()} for security; no command is executed
     */
    @Override
    public InputStream runCommandToStream(String command) {
        return InputStream.nullInputStream();
    }

    /**
     * Executes system command and returns standard output as String.
     * <p>
     * <strong>SECURITY NOTE:</strong> In this secure cloud profile implementation, this method does not
     * execute any system commands. It always returns an empty string to prevent arbitrary command execution
     * in production environments. For actual command execution, use the development profile with
     * {@link LiveSystemServices}.
     * 
     *
     * @param command the Linux/system command that would be executed (ignored in secure implementation)
     * @return always returns an empty string for security; no command is executed
     */
    @Override
    public String runCommandToString(String command) {
        return "";
    }
    /**
     * Executes system command and returns standard output as byte array.
     * <p>
     * <strong>SECURITY NOTE:</strong> In this secure cloud profile implementation, this method does not
     * execute any system commands. It always returns {@code null} to prevent arbitrary command execution
     * in production environments. For actual command execution, use the development profile with
     * {@link LiveSystemServices}.
     * 
     *
     * @param command the Linux/system command that would be executed (ignored in secure implementation)
     * @return always returns {@code null} for security; no command is executed
     */
    @Override
    public byte[] runCommandToByteArray(String command) {
        return null;
    }
}
