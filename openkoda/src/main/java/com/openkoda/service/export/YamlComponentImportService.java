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

package com.openkoda.service.export;

import com.openkoda.controller.ComponentProvider;
import com.openkoda.service.export.converter.YamlToEntityConverterFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract base class for YAML component import services, providing shared YamlToEntityConverterFactory wiring for subclasses.
 * <p>
 * Extends {@link ComponentProvider} to inherit repository, service, and logging infrastructure. Exposes package-private 
 * autowired {@link YamlToEntityConverterFactory} field that subclasses use to convert deserialized YAML DTOs into 
 * persisted domain entities. Serves as a common parent for specialized import strategies including classpath scanning, 
 * ZIP upload, and filesystem monitoring.
 * </p>
 * <p>
 * This class implements the Template Method pattern where subclasses implement resource discovery and loading strategies 
 * while sharing the conversion infrastructure provided by this base class. The abstract nature ensures no direct 
 * instantiation - concrete implementations must provide the specific import mechanism.
 * </p>
 * <p>
 * Thread-safety: This is a stateless abstract class. Thread-safety depends on subclass implementations and their usage 
 * of the shared yamlToEntityConverterFactory field.
 * </p>
 * <p>
 * Subclass responsibilities include:
 * <ul>
 * <li>Discover YAML resources from specific sources (classpath, filesystem, upload streams)</li>
 * <li>Load and parse YAML content into DTOs</li>
 * <li>Delegate DTO-to-entity conversion to the inherited yamlToEntityConverterFactory</li>
 * <li>Handle resource-specific error scenarios and logging</li>
 * </ul>
 * </p>
 *
 * @see ComponentProvider for inherited repository and service infrastructure
 * @see YamlToEntityConverterFactory for DTO-to-entity conversion logic
 * @see com.openkoda.service.export.ClasspathComponentImportService for classpath-based import implementation
 * @see com.openkoda.service.export.ZipComponentImportService for ZIP upload import implementation
 * @since 1.7.1
 * @author OpenKoda Team
 */
public abstract class YamlComponentImportService extends ComponentProvider {
    
    /**
     * Factory that resolves DTO-specific converters and delegates YAML-to-entity conversion with persistence.
     * <p>
     * This field is package-private to allow access by subclasses within the same package. It is autowired by Spring 
     * during subclass instantiation, ensuring all concrete import services share the same converter factory instance.
     * </p>
     * <p>
     * Subclasses call {@code yamlToEntityConverterFactory.processYamlDto(dto, filePath)} or 
     * {@code processYamlDto(dto, filePath, resources)} to convert deserialized YAML DTOs into persisted domain entities. 
     * The factory handles converter resolution based on DTO type, entity creation, validation, and persistence.
     * </p>
     *
     * @see YamlToEntityConverterFactory#processYamlDto for converter resolution and delegation logic
     */
    @Autowired
    YamlToEntityConverterFactory yamlToEntityConverterFactory;
}
