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

package com.openkoda.dto;

/**
 * Data transfer object carrying server-side JavaScript code metadata and execution context.
 * <p>
 * This DTO encapsulates JavaScript source code along with execution parameters for dynamic
 * server-side scripting using GraalVM JS integration. It supports tenant-scoped script execution
 * through the {@link OrganizationRelatedObject} contract, allowing scripts to be isolated per organization.
 * </p>
 * <p>
 * The DTO is used by JsFlowRunner and script execution services to provide:
 * <ul>
 *   <li>JavaScript source code to be executed by the GraalVM engine</li>
 *   <li>Script identification and logging via name field</li>
 *   <li>Model/context object bindings for script access</li>
 *   <li>Script arguments for parameterized execution</li>
 *   <li>Multi-tenant isolation through organization identifier</li>
 * </ul>
 * </p>
 * <p>
 * Implements {@link CanonicalObject} for notification integration and {@link OrganizationRelatedObject}
 * for multi-tenant JavaScript execution within organization-scoped contexts.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.uicomponent.JsFlowRunner
 * @see OrganizationRelatedObject
 * @see CanonicalObject
 */
public class ServerJsDto implements CanonicalObject, OrganizationRelatedObject{

    /**
     * Organization identifier for tenant-scoped script execution.
     * <p>
     * When set, the script executes within the context of the specified organization,
     * enabling multi-tenant isolation. Null value indicates a global script not scoped
     * to any particular organization.
     * </p>
     */
    public Long organizationId;

    /**
     * Human-readable script name for identification and logging purposes.
     * <p>
     * Provides a descriptive identifier for the JavaScript code, used in logs,
     * error messages, and administrative interfaces to distinguish between different scripts.
     * </p>
     */
    public String name;
    
    /**
     * JavaScript source code to be executed by the GraalVM engine.
     * <p>
     * Contains the complete JavaScript source that will be evaluated in the GraalVM Context.
     * The code has access to model objects and arguments provided through this DTO's other fields.
     * </p>
     */
    public String code;
    
    /**
     * Model or context object name available to script execution.
     * <p>
     * Specifies the name of the model object that will be bound and accessible within
     * the JavaScript execution context, allowing scripts to interact with application data.
     * </p>
     */
    public String model;
    
    /**
     * Script arguments as a string, parsed by the execution engine.
     * <p>
     * Contains arguments that will be passed to the JavaScript code during execution.
     * The format and parsing are handled by the script execution engine.
     * </p>
     */
    public String arguments;

    
    /**
     * Returns the human-readable script name.
     *
     * @return the script name used for identification and logging, may be null
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the JavaScript source code to be executed.
     *
     * @return the JavaScript source code string, may be null
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Returns the model or context object name for script execution.
     *
     * @return the model object name accessible to the script, may be null
     */
    public String getModel() {
        return this.model;
    }

    /**
     * Returns the script arguments string.
     *
     * @return the arguments to be passed to the script during execution, may be null
     */
    public String getArguments() {
        return arguments;
    }

    /**
     * Sets the human-readable script name.
     *
     * @param name the script name for identification and logging
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the JavaScript source code to be executed.
     *
     * @param code the JavaScript source code string
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Sets the model or context object name for script execution.
     *
     * @param model the model object name to be accessible to the script
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Sets the script arguments string.
     *
     * @param arguments the arguments to be passed to the script during execution
     */
    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    /**
     * Returns the notification message for this server JavaScript DTO.
     * <p>
     * This implementation returns an empty string as server-side JavaScript execution
     * does not generate user-facing notification messages.
     * </p>
     *
     * @return an empty string
     */
    @Override
    public String notificationMessage() {
        return "";
    }

    /**
     * Returns the organization identifier for tenant-scoped script execution.
     * <p>
     * Implements {@link OrganizationRelatedObject} contract to support multi-tenant
     * JavaScript execution with organization-level isolation.
     * </p>
     *
     * @return the organization ID for tenant-scoped execution, or null for global scripts
     */
    @Override
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the organization identifier for tenant-scoped script execution.
     * <p>
     * When set, the script will execute within the security context of the specified organization.
     * Set to null for global scripts not scoped to any particular tenant.
     * </p>
     *
     * @param organizationId the organization ID for tenant isolation, or null for global scope
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }
}