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

package com.openkoda.model.component;

import com.openkoda.controller.common.PageAttributes;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.helper.JsonHelper;
import com.openkoda.model.PrivilegeNames;
import com.openkoda.model.common.ComponentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Formula;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * JPA entity for storing server-side JavaScript code executed via GraalVM JS engine.
 * <p>
 * This entity enables dynamic business logic and data transformation by storing JavaScript
 * code that runs on the JVM through GraalVM's polyglot execution context. Scripts have access
 * to Java APIs and can be executed without requiring Java code deployment. The entity is
 * organization-scoped through {@link ComponentEntity} inheritance, providing multi-tenancy
 * support.

 * <p>
 * JavaScript execution is performed by {@code JsFlowRunner} and UIComponent services at runtime.
 * Common use cases include data transformation, dynamic business logic, and API integrations
 * that need to be modified without redeploying the application.

 * <p>
 * Example usage:
 * <pre>{@code
 * ServerJs script = new ServerJs("console.log('Hello');", "{}", "arg1\narg2");
 * PageModelMap model = script.getModelMap(); // Parses JSON model
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ComponentEntity
 * @see com.openkoda.core.flow.PageModelMap
 */
@Entity
@Table (name = "server_js")
public class ServerJs extends ComponentEntity {

    /**
     * Static list of content property names used by indexing and diffing tools.
     * Contains the names of fields that represent the core content of this entity:
     * 'code', 'model', and 'arguments'.
     */
    final static List<String> contentProperties = Arrays.asList("code", "model", "arguments");

    /**
     * ServerJs script name for identification and reference.
     * This name is used to identify and locate specific scripts within the system.
     */
    @Column
    private String name;

    /**
     * JavaScript source code executed by GraalVM JS engine.
     * The code is stored as a string with a maximum length of 262,144 characters
     * (65,536 × 4). This code runs in GraalVM's polyglot context with access to
     * Java APIs and OpenKoda services.
     */
    @Column(length = 65536 * 4)
    private String code;

    /**
     * JSON model definition for PageModelMap initialization.
     * This field contains a JSON string that is parsed into a {@link PageModelMap}
     * when {@link #getModelMap()} is called. Maximum length is 262,144 characters.
     */
    @Column(length = 65536 * 4)
    private String model;

    /**
     * Newline-separated argument list for script parameterization.
     * Arguments are split by newline character when parsed by {@link #getModelMap()}.
     * Maximum length is 262,144 characters.
     */
    @Column(length = 65536 * 4)
    private String arguments;

    /**
     * Computed privilege required for reading this ServerJs script.
     * This field is computed via {@code @Formula} and always returns
     * {@link PrivilegeNames#_canReadBackend} privilege.
     */
    @Formula("( '" + PrivilegeNames._canReadBackend + "' )")
    private String requiredReadPrivilege;

    /**
     * Computed privilege required for writing/modifying this ServerJs script.
     * This field is computed via {@code @Formula} and always returns
     * {@link PrivilegeNames#_canManageBackend} privilege.
     */
    @Formula("( '" + PrivilegeNames._canManageBackend + "' )")
    private String requiredWritePrivilege;

    /**
     * No-argument constructor for JPA entity instantiation.
     * Initializes the entity with null organization ID.
     */
    public ServerJs() {
        super(null);
    }

    /**
     * Constructor for creating a ServerJs script scoped to a specific organization.
     * Enables multi-tenancy by associating the script with a tenant organization.
     *
     * @param organizationId the organization ID for tenant scoping, or null for global scope
     */
    public ServerJs(Long organizationId) {
        super(organizationId);
    }

    /**
     * Constructor for creating a ServerJs script with code, model, and arguments.
     * Initializes the entity with the provided JavaScript code, JSON model definition,
     * and newline-separated arguments. Organization ID is set to null (global scope).
     *
     * @param code the JavaScript source code to execute
     * @param model the JSON model definition for PageModelMap initialization
     * @param arguments newline-separated argument list for script parameterization
     */
    public ServerJs(String code, String model, String arguments) {
        super(null);
        this.code = code;
        this.model = model;
        this.arguments = arguments;
    }
    
    /**
     * Returns the script name for identification and reference.
     *
     * @return the ServerJs script name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the JavaScript source code to be executed by GraalVM JS engine.
     *
     * @return the JavaScript source code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Returns the JSON model definition string.
     *
     * @return the JSON model definition for PageModelMap initialization
     */
    public String getModel() {
        return this.model;
    }

    /**
     * Creates a PageModelMap from the JSON model definition and adds parsed arguments.
     * <p>
     * This method parses the JSON model string via {@link JsonHelper#fromDebugJson(String)}
     * and adds the arguments as a list to the {@link PageAttributes#arguments} key. Arguments
     * are split by newline character. If the arguments field is blank, an empty list is added.

     *
     * @return the PageModelMap initialized with model data and arguments
     * @throws IOException if JSON parsing of the model string fails due to malformed JSON
     */
    //TODO: move this logic to some helper class
    public PageModelMap getModelMap() throws IOException {
        PageModelMap result = JsonHelper.fromDebugJson(this.model);
        result.put(PageAttributes.arguments,
            StringUtils.isBlank(this.arguments) ?
                new ArrayList<>() :
                Arrays.asList(StringUtils.split(this.arguments, "\n")));
        return result;
    }

    /**
     * Sets the script name for identification and reference.
     *
     * @param name the ServerJs script name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the JavaScript source code to be executed by GraalVM JS engine.
     *
     * @param code the JavaScript source code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Sets the JSON model definition string.
     *
     * @param model the JSON model definition to set
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Returns the newline-separated argument list.
     *
     * @return the newline-separated argument list for script parameterization
     */
    public String getArguments() {
        return arguments;
    }

    /**
     * Sets the newline-separated argument list.
     *
     * @param arguments the newline-separated argument list to set
     */
    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    /**
     * Returns the audit string representation of this entity.
     * Currently not implemented and returns null.
     *
     * @return null (audit string not currently provided)
     */
    @Override
    public String toAuditString() {
        return null;
    }

    /**
     * Returns the privilege required for reading this ServerJs script.
     * Overrides {@link ComponentEntity#getRequiredReadPrivilege()} to return
     * the computed {@link PrivilegeNames#_canReadBackend} privilege.
     *
     * @return the required read privilege (canReadBackend)
     */
    @Override
    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    /**
     * Returns the privilege required for writing/modifying this ServerJs script.
     * Overrides {@link ComponentEntity#getRequiredWritePrivilege()} to return
     * the computed {@link PrivilegeNames#_canManageBackend} privilege.
     *
     * @return the required write privilege (canManageBackend)
     */
    @Override
    public String getRequiredWritePrivilege() {
        return requiredWritePrivilege;
    }

    /**
     * Returns the list of content property names for indexing and diffing tools.
     * This method returns the static list containing 'code', 'model', and 'arguments'.
     *
     * @return collection of content property names
     */
    @Override
    public Collection<String> contentProperties() {
        return contentProperties;
    }
}