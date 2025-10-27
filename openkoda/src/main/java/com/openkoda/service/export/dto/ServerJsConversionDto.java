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

package com.openkoda.service.export.dto;

/**
 * DTO for server-side JavaScript code modules with arguments and model metadata.
 * <p>
 * This is a mutable JavaBean POJO for YAML/JSON serialization that extends ComponentDto
 * for module and organization scope. It maps the ServerJs domain entity for server-side
 * JavaScript code export/import operations.
 * </p>
 * <p>
 * The code field contains executable JavaScript payload and must be treated as opaque
 * by serialization frameworks. This DTO is used by JavaScript module pipelines and
 * GraalVM integration. This class is not thread-safe.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ComponentDto for inherited module and organizationId fields
 * @see com.openkoda.model.ServerJs domain entity
 */
public class ServerJsConversionDto extends ComponentDto {

    /**
     * JavaScript module name identifier. Nullable.
     */
    private String name;
    
    /**
     * Function arguments specification or parameter metadata in serialized format. Nullable.
     */
    private String arguments;
    
    /**
     * Executable JavaScript code payload. Treat as opaque - do not parse or validate content during serialization. Nullable.
     */
    private String code;
    
    /**
     * Data model or context metadata associated with this JavaScript module. Nullable.
     */
    private String model;

    /**
     * Gets the JavaScript module name identifier.
     *
     * @return the JavaScript module name identifier or null if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the JavaScript module name identifier.
     *
     * @param name the JavaScript module name identifier to set, may be null
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the function arguments specification or parameter metadata in serialized format.
     *
     * @return the function arguments specification or parameter metadata or null if not set
     */
    public String getArguments() {
        return arguments;
    }

    /**
     * Sets the function arguments specification or parameter metadata in serialized format.
     *
     * @param arguments the function arguments specification or parameter metadata to set, may be null
     */
    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    /**
     * Gets the executable JavaScript code payload.
     * <p>
     * The returned code should be treated as opaque executable content.
     * </p>
     *
     * @return the executable JavaScript code payload or null if not set
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the executable JavaScript code payload.
     * <p>
     * The code is stored without validation or parsing.
     * </p>
     *
     * @param code the executable JavaScript code payload to set, may be null
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets the data model or context metadata associated with this JavaScript module.
     *
     * @return the data model or context metadata or null if not set
     */
    public String getModel() {
        return model;
    }

    /**
     * Sets the data model or context metadata associated with this JavaScript module.
     *
     * @param model the data model or context metadata to set, may be null
     */
    public void setModel(String model) {
        this.model = model;
    }

}
