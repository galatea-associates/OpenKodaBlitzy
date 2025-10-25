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

package com.openkoda.service.export.converter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Generic interface for converting deserialized YAML DTOs into persisted domain entities.
 * <p>
 * This interface defines the contract for converters that transform YAML-based data transfer objects
 * into JPA entities and persist them to the database. Implementations handle the mapping logic
 * between the DTO structure and the entity model, along with any necessary resource loading
 * (such as code snippets or templates) required during the conversion process.
 * </p>
 * <p>
 * Typical usage involves deserializing YAML configuration files into DTOs, then using a converter
 * implementation to create and save the corresponding entities:
 * <pre>{@code
 * YamlToEntityConverter<Form, FormDto> converter = ...;
 * Form form = converter.convertAndSave(dto, "forms/contact-form.yaml");
 * }</pre>
 * </p>
 *
 * @param <T> the entity type to be persisted (e.g., Form, FrontendResource)
 * @param <D> the DTO type representing the deserialized YAML structure
 * @since 1.7.1
 * @author OpenKoda Team
 * @see ResourceLoadingException
 * @see YamlToEntityConverterFactory
 */
public interface YamlToEntityConverter<T, D>{
    /**
     * Converts the provided DTO into a domain entity and persists it to the database.
     * <p>
     * This method performs the mapping from the deserialized YAML DTO structure to the target
     * entity type, populating all required fields and relationships. After conversion, the entity
     * is saved via the appropriate repository. Any embedded resources (code, templates) are loaded
     * from the classpath using the provided file path as a reference.
     * </p>
     *
     * @param dto the data transfer object containing the deserialized YAML data
     * @param filePath the original file path of the YAML source, used for resolving relative resource paths
     * @return the persisted entity with database-generated ID and any computed fields populated
     */
    T convertAndSave(D dto, String filePath);
    
    /**
     * Converts the provided DTO into a domain entity and persists it to the database, using an in-memory resource map.
     * <p>
     * This overload accepts a pre-loaded map of resources (typically code snippets or template files) to avoid
     * classpath I/O operations during conversion. The resource map keys represent resource paths, and values
     * contain the resource content as strings. This is useful for bulk import operations or when resources
     * are already loaded into memory.
     * </p>
     *
     * @param dto the data transfer object containing the deserialized YAML data
     * @param filePath the original file path of the YAML source, used for logging and error reporting
     * @param resources an in-memory map of resource paths to their string content (e.g., code files, templates)
     * @return the persisted entity with database-generated ID and any computed fields populated
     */
    T convertAndSave(D dto, String filePath, Map<String, String> resources);
    
    /**
     * Loads a classpath resource as a UTF-8 encoded string.
     * <p>
     * This helper method uses {@link ClassLoader#getResourceAsStream(String)} to locate and read
     * the resource from the classpath. The entire resource content is read into memory as a byte array,
     * then decoded as UTF-8. This method is typically used to load code snippets, templates, or
     * configuration files referenced by YAML import definitions.
     * </p>
     *
     * @param path the classpath-relative path to the resource (e.g., "forms/templates/default.html")
     * @return the complete resource content as a UTF-8 string
     * @throws ResourceLoadingException if the resource is not found at the specified path
     * @throws ResourceLoadingException if an I/O error occurs while reading the resource
     */
    default String loadResourceAsString(String path) {
        try {
            if (this.getClass().getClassLoader().getResourceAsStream(path) == null){
                throw new ResourceLoadingException("File not found under the path: " + path);
            }
            return new String(this.getClass().getClassLoader().getResourceAsStream(path).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResourceLoadingException("Couldn't read the file under the path: " + path);
        }
    }
}