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

package com.openkoda.dto.payment;

import com.openkoda.dto.CanonicalObject;
import com.openkoda.dto.OrganizationRelatedObject;

/**
 * Data Transfer Object for script execution outcomes in the payment domain processing.
 * <p>
 * This mutable POJO conveys script execution results and implements both {@link CanonicalObject}
 * and {@link OrganizationRelatedObject} contracts to support notification generation and
 * multi-tenant organization scoping.
 * </p>
 * <p>
 * Used in background workers, script orchestration workflows, and notification flows to
 * transport script execution results to designated result handlers for further processing.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CanonicalObject
 * @see OrganizationRelatedObject
 */
public class ScriptTaskDto implements CanonicalObject, OrganizationRelatedObject {

    /**
     * Script execution result or output.
     * <p>
     * Contains the outcome of the script execution, typically a string representation
     * of the script's return value or processing result.
     * </p>
     */
    public String result;

    /**
     * Name of the handler to process the script execution result.
     * <p>
     * Identifies which result handler should be invoked to handle the script outcome,
     * enabling dynamic routing of results to appropriate processing components.
     * </p>
     */
    public String resultHandlerName;

    /**
     * Multi-tenant organization identifier.
     * <p>
     * Associates this script task with a specific organization for tenant isolation
     * and organization-scoped operations.
     * </p>
     */
    public Long organizationId;

    /**
     * Returns a notification message for this script task.
     * <p>
     * Implements {@link CanonicalObject#notificationMessage()} to provide a fixed
     * literal notification message describing the script execution result.
     * </p>
     *
     * @return the fixed notification message "Script execution result"
     */
    @Override
    public String notificationMessage() {
        return String.format("Script execution result");
    }

    /**
     * Gets the script execution result.
     *
     * @return the script execution result or output, may be null
     */
    public String getResult() {
        return result;
    }

    /**
     * Sets the script execution result.
     *
     * @param result the script execution result or output to set
     */
    public void setResult(String result) {
        this.result = result;
    }

    /**
     * Gets the result handler name.
     *
     * @return the name of the handler to process the script execution result, may be null
     */
    public String getResultHandlerName() {
        return resultHandlerName;
    }

    /**
     * Sets the result handler name.
     *
     * @param resultHandlerName the name of the handler to process the script execution result
     */
    public void setResultHandlerName(String resultHandlerName) {
        this.resultHandlerName = resultHandlerName;
    }

    /**
     * Gets the organization identifier.
     * <p>
     * Implements {@link OrganizationRelatedObject#getOrganizationId()} to provide
     * the multi-tenant organization identifier for this script task.
     * </p>
     *
     * @return the organization identifier, may be null
     */
    @Override
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the organization identifier.
     *
     * @param organizationId the multi-tenant organization identifier to associate with this script task
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }
}
