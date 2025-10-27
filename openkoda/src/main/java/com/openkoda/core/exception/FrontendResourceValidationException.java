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

package com.openkoda.core.exception;


import com.openkoda.core.flow.HttpStatusException;
import com.openkoda.model.component.FrontendResource;
import org.springframework.http.HttpStatus;

//TODO: check why we can not use ValidationException instead
/**
 * Specialized exception for FrontendResource validation failures, providing detailed feedback for developers.
 * <p>
 * This exception extends {@link HttpStatusException} with HTTP 500 (INTERNAL_SERVER_ERROR) status and carries
 * both a validation result indicator and a human-readable suggestion message. The unique dual-purpose design
 * enables the exception to communicate validation outcomes (pass with warnings vs. critical failure) alongside
 * actionable feedback for correcting FrontendResource issues.
 * </p>
 * <p>
 * Common validation scenarios include JavaScript code syntax validation, template syntax errors, security checks
 * for malicious code patterns, and resource configuration validation. The suggestion field provides specific
 * guidance for resolving detected issues, such as syntax error locations or security violation descriptions.
 * </p>
 * <p>
 * Both the {@code result} and {@code suggestion} fields are immutable (final), ensuring consistent exception
 * state throughout exception handling workflows. This immutability supports thread-safe exception propagation
 * in concurrent validation scenarios.
 * </p>
 * <p>
 * Example usage in FrontendResource validation:
 * <pre>
 * if (!isValidJavaScript(code)) {
 *     throw new FrontendResourceValidationException(false, "JavaScript syntax error at line 42");
 * }
 * </pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see FrontendResource
 * @see HttpStatusException
 */
public class FrontendResourceValidationException extends HttpStatusException {
    /**
     * Validation result indicator. True if validation passed with warnings, false if validation failed critically.
     * <p>
     * Use this field to distinguish between non-blocking warnings (true) and blocking validation errors (false).
     * When true, the FrontendResource may be saved with warnings displayed to the developer. When false,
     * the resource should be rejected until validation issues are resolved.
     * </p>
     */
    public final boolean result;
    
    /**
     * Human-readable suggestion or error message describing the validation issue and recommended corrective action.
     * <p>
     * This message provides actionable feedback for frontend developers, including specific error locations,
     * violation descriptions, and guidance for resolution. The suggestion is displayed in validation error
     * responses and development tools to facilitate rapid issue correction.
     * </p>
     */
    public final String suggestion;

    /**
     * Creates a new FrontendResourceValidationException with validation outcome and suggestion message.
     * <p>
     * This constructor sets the HTTP status to 500 (INTERNAL_SERVER_ERROR) and stores the validation state
     * for later retrieval. The exception can be caught and processed to display validation feedback to
     * frontend developers through the UI or API responses.
     * </p>
     *
     * @param result validation outcome indicator; true if validation passed with non-blocking warnings,
     *               false if validation failed with critical errors that prevent resource acceptance
     * @param suggestion descriptive message providing actionable feedback for display to frontend developers,
     *                   including error details, locations, and recommended corrective actions
     */
    public FrontendResourceValidationException(boolean result, String suggestion) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, suggestion);
        this.result = result;
        this.suggestion = suggestion;
    }

    /**
     * Returns the validation result indicator.
     * <p>
     * Use this method to determine if validation passed with warnings (true) or failed with critical
     * errors (false). This distinction enables conditional handling where warnings allow resource
     * acceptance with developer notification, while errors block resource persistence.
     * </p>
     *
     * @return true if validation passed with non-blocking warnings, false if validation failed critically
     */
    public boolean isResult() {
        return result;
    }

    /**
     * Returns the validation suggestion message.
     * <p>
     * This message provides actionable feedback for correcting FrontendResource validation issues,
     * including specific error locations, violation descriptions, and resolution guidance. The suggestion
     * is typically displayed in validation error responses to help developers quickly identify and fix
     * problems with JavaScript code, templates, or resource configuration.
     * </p>
     *
     * @return human-readable suggestion describing the validation issue and recommended corrective action
     */
    public String getSuggestion() {
        return suggestion;
    }
}
