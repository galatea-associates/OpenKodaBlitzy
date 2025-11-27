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

import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.util.List;

/**
 * Declares canonical typed page attribute constants for OpenKoda controllers and views.
 * <p>
 * This interface provides standardized {@link PageAttr} constants that avoid ad-hoc string keys
 * when passing data between controllers, Flow pipelines, and view templates. Each constant
 * represents a commonly-used page attribute with strong typing for type-safe data access.
 * <p>
 * These attributes are used in Flow pipelines to set error states, messages, and validation
 * results that are rendered in Thymeleaf templates. Using these typed constants prevents
 * typos and enables IDE auto-completion.
 * <p>
 * Example usage in a Flow pipeline:
 * <pre>{@code
 * Flow.init()
 *     .thenSet(isError, a -> true)
 *     .thenSet(message, a -> "Operation completed successfully");
 * }</pre>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 28.01.17
 * @see PageAttr
 * @see Flow
 * @see org.springframework.validation.BindingResult
 */
public interface BasePageAttributes {
    
    /**
     * Indicates whether an error occurred during request processing.
     * <p>
     * Set to {@code true} when an error condition is detected in a Flow pipeline or controller.
     * View templates can check this attribute to display error banners or styling.
     * 
     */
    PageAttr<Boolean> isError = new PageAttr<>("isError");
    
    /**
     * Stores a user-facing message for display in the view.
     * <p>
     * Used for success messages, informational text, or general feedback to users.
     * This attribute is rendered in view templates to communicate operation results.
     * 
     */
    PageAttr<String> message = new PageAttr<>("message");
    
    /**
     * Stores an error message describing what went wrong.
     * <p>
     * Used when an error occurs to provide specific details to the user. View templates
     * display this message in error notification areas. Typically set alongside {@link #isError}.
     * 
     */
    PageAttr<String> error = new PageAttr<>("error");
    
    /**
     * Stores an exception object for error handling and diagnostics.
     * <p>
     * Captures the full exception when errors occur during request processing. Useful for
     * logging, debugging, and displaying technical error details in development environments.
     * In production, expose exception details carefully to avoid security risks.
     * 
     */
    PageAttr<Exception> exception = new PageAttr<>("exception");
    
    /**
     * Stores Spring validation results from form binding.
     * <p>
     * Contains validation errors detected when binding request parameters to form objects.
     * View templates access this attribute to display field-specific error messages next
     * to form inputs. Set automatically by Spring MVC during form validation.
     * 
     *
     * @see org.springframework.validation.BindingResult
     */
    PageAttr<BindingResult> bindingResult = new PageAttr<>("bindingResult");
    
    /**
     * Stores a flattened list of all validation errors.
     * <p>
     * Provides a simplified view of all validation errors from {@link BindingResult} for
     * display in summary error sections. Use this attribute when showing all errors together
     * rather than per-field error messages.
     * 
     *
     * @see org.springframework.validation.ObjectError
     * @see #bindingResult
     */
    PageAttr<List<ObjectError>> bindingResultAllErrors = new PageAttr<>("bindingResultAllErrors");
}
