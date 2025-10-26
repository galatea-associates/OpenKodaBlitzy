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

/**
 * RuntimeException wrapper for GraalVM JavaScript execution errors in Flow pipelines.
 * <p>
 * This exception wraps GraalVM's PolyglotException to provide enhanced debugging context
 * when JavaScript code fails during Flow execution. It captures the script source code,
 * error position, and original error message to help developers diagnose issues.
 * </p>
 * <p>
 * The Flow.execute() method catches PolyglotException from GraalVM's JavaScript engine
 * and re-wraps it as JsFlowExecutionException with enriched error context including:
 * <ul>
 *   <li>Original error message from the JavaScript engine</li>
 *   <li>Position information (line and column) where the error occurred</li>
 *   <li>Complete JavaScript source code for reference</li>
 * </ul>
 * </p>
 * <p>
 * Example usage in Flow pipeline:
 * <pre>{@code
 * try {
 *     flow.executeJavaScript(jsCode);
 * } catch (PolyglotException e) {
 *     throw new JsFlowExecutionException(e.getMessage(), jsCode, e.getSourceLocation());
 * }
 * }</pre>
 * </p>
 * <p>
 * The formatted exception message includes the position and code snippet to aid debugging:
 * <pre>
 * Error executing flow: ReferenceError: variable is not defined
 * at: line 5, column 12
 * code:
 * [JavaScript source code]
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Flow
 * @see org.graalvm.polyglot.PolyglotException
 */
public class JsFlowExecutionException extends RuntimeException {

    /**
     * JavaScript source code that caused the execution error.
     * Contains the complete script text for debugging purposes.
     * Set to "N/A" when the error is not related to specific script code.
     */
    private final String code;
    
    /**
     * Position information indicating where the error occurred in the script.
     * Typically includes line and column numbers (e.g., "line 5, column 12").
     * Set to "N/A" when position information is unavailable.
     */
    private final String position;

    /**
     * Creates a new JsFlowExecutionException wrapping a GraalVM PolyglotException.
     * <p>
     * This constructor is used when Flow.execute() catches a PolyglotException during
     * JavaScript evaluation. It captures the error message, script source code, and
     * position information to provide comprehensive debugging context.
     * </p>
     * <p>
     * The exception message is formatted to include all diagnostic information:
     * error message, position, and code snippet for easy troubleshooting.
     * </p>
     *
     * @param message the error message from the JavaScript engine describing the failure
     * @param code the complete JavaScript source code that was being executed
     * @param position the position in the script where the error occurred (line and column)
     */
    public JsFlowExecutionException(String message, String code, String position) {
        super(String.format("Error executing flow: %s\nat: %s\ncode:\n%s\n", message, position, code));
        this.code = code;
        this.position = position;
    }

    /**
     * Creates a new JsFlowExecutionException for general flow execution errors.
     * <p>
     * This constructor is used when the error is not directly related to JavaScript
     * evaluation but occurs during flow execution. Since no script code or position
     * information is available, both fields are set to "N/A".
     * </p>
     * <p>
     * The original exception is preserved as the cause for complete stack trace analysis.
     * This helps developers trace errors back to their root cause in the Flow pipeline.
     * </p>
     *
     * @param message descriptive error message explaining the flow execution failure
     * @param cause the original exception that caused the flow execution to fail
     */
    public JsFlowExecutionException(String message, Throwable cause) {
        super(String.format("Error executing flow: %s\nat: %s\ncode:\n%s\n", message, "N/A", "N/A"), cause);
        this.code = "N/A";
        this.position = "N/A";
    }
}
