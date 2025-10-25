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

import com.openkoda.core.flow.LoggingComponent;
import com.openkoda.model.common.ComponentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

/**
 * Spring component centralizing discovery and lookup of EntityToYamlConverter implementations by entity class.
 * <p>
 * This factory maintains an internal registry that maps entity classes to their corresponding
 * {@link EntityToYamlConverter} implementations. During construction, the factory accepts an injected
 * list of all available converters and builds an immutable map keyed by entity Class. Entity types
 * are determined via reflective inspection using {@code getGenericSuperclass()} and parameterized type
 * resolution to extract the first generic type argument from each converter's superclass.
 * </p>
 * <p>
 * The factory provides three main operations:
 * <ul>
 *   <li>Exporting entities to ZIP archives with optional database upgrade scripts</li>
 *   <li>Exporting entities to individual YAML files on the filesystem</li>
 *   <li>Removing previously exported files</li>
 * </ul>
 * All operations delegate to the appropriate converter instance based on the entity's runtime class.
 * </p>
 * <p>
 * <b>Thread Safety:</b> The converter map is final and immutable after construction. All lookups
 * are thread-safe. However, thread safety of export operations depends on the underlying converter
 * implementations.
 * </p>
 * <p>
 * <b>Error Handling:</b> If no converter is registered for a given entity class, methods throw
 * {@link IllegalArgumentException} with a descriptive message identifying the unregistered entity type.
 * </p>
 * <p>
 * <b>Type Safety:</b> Due to Java type erasure, unchecked casts are required when retrieving converters
 * from the map. These casts are safe because the map is constructed to guarantee type correspondence
 * between entity classes and their converters.
 * </p>
 *
 * @since 1.7.1
 * @author OpenKoda Team
 * @see EntityToYamlConverter
 */
@Component
public class EntityToYamlConverterFactory implements LoggingComponent {

    private final Map<Class<?>, EntityToYamlConverter<?, ?>> converterMap;

    /**
     * Constructs the factory by building an immutable registry of converters keyed by entity class.
     * <p>
     * Spring automatically injects all available {@link EntityToYamlConverter} bean instances.
     * For each converter, the constructor uses reflection via {@link #getEntityType(EntityToYamlConverter)}
     * to determine the entity class it handles by inspecting the generic superclass and extracting
     * the first parameterized type argument. The resulting map provides O(1) lookup of converters
     * by entity class during export operations.
     * </p>
     *
     * @param converters the list of all EntityToYamlConverter beans discovered by Spring, must not be null
     * @throws IllegalStateException if multiple converters are registered for the same entity class (map key collision)
     */
    @Autowired
    public EntityToYamlConverterFactory(List<EntityToYamlConverter<?, ?>> converters) {
        this.converterMap = converters.stream()
                .collect(Collectors.toMap(converter -> getEntityType(converter), Function.identity()));
    }

    /**
     * Exports an entity to a ZIP archive by delegating to the registered converter for the entity's class.
     * <p>
     * This method looks up the appropriate {@link EntityToYamlConverter} based on the entity's runtime class,
     * then delegates to the converter's {@code addToZip()} method to serialize the entity as YAML and add it
     * to the provided ZIP output stream. If {@code dbUpgradeEntries} is non-null, the method also invokes
     * the converter's {@code getUpgradeScript()} to generate database migration SQL statements.
     * </p>
     * <p>
     * The {@code zipEntries} set tracks which entries have already been written to the ZIP archive to prevent
     * duplicate entries across multiple export operations on the same stream.
     * </p>
     *
     * @param entity the entity to export, must not be null
     * @param zipOut the ZIP output stream to write to, must not be null
     * @param dbUpgradeEntries optional list to accumulate database upgrade SQL statements; pass null to skip upgrade script generation
     * @param zipEntries set tracking ZIP entry names already written to prevent duplicates, must not be null
     * @return the same ZIP output stream for method chaining
     * @throws IllegalArgumentException if no converter is registered for the entity's class
     * @throws IllegalArgumentException if entity is null
     */
    @SuppressWarnings("unchecked")
    public ZipOutputStream exportToZip(Object entity, ZipOutputStream zipOut, List<String> dbUpgradeEntries, Set<String> zipEntries) {
        debug("[exportToZip]");

        EntityToYamlConverter<Object, Object> converter = (EntityToYamlConverter<Object, Object>) converterMap.get(entity.getClass());

        if(converter == null){
            throw new IllegalArgumentException("No parent converter found for entity " + entity.getClass().getName());
        }
        
        converter.addToZip(entity, zipOut, zipEntries);
        if(dbUpgradeEntries != null) {
            converter.getUpgradeScript(entity, dbUpgradeEntries);
        }
        
        return zipOut;
    }
    
    /**
     * Exports an entity to a ZIP archive without generating database upgrade scripts.
     * <p>
     * This is a convenience overload that delegates to
     * {@link #exportToZip(Object, ZipOutputStream, List, Set)} with {@code dbUpgradeEntries} set to null.
     * Use this method when only entity serialization is needed without corresponding SQL migration statements.
     * </p>
     *
     * @param entity the entity to export, must not be null
     * @param zipOut the ZIP output stream to write to, must not be null
     * @param zipEntries set tracking ZIP entry names already written to prevent duplicates, must not be null
     * @return the same ZIP output stream for method chaining
     * @throws IllegalArgumentException if no converter is registered for the entity's class
     * @see #exportToZip(Object, ZipOutputStream, List, Set)
     */
    public ZipOutputStream exportToZip(Object entity, ZipOutputStream zipOut, Set<String> zipEntries) {
        return exportToZip(entity, zipOut, null, zipEntries);
    }
    
    /**
     * Exports a component entity to an individual YAML file on the filesystem.
     * <p>
     * This method looks up the appropriate {@link EntityToYamlConverter} for the entity's class and
     * invokes {@code saveToFile()} to serialize the entity as YAML and persist it to disk. The converter
     * determines the target file path and naming convention. This operation is typically used for exporting
     * individual components during development or for version control integration.
     * </p>
     *
     * @param entity the component entity to export, must not be null
     * @return the same entity instance for method chaining
     * @throws IllegalArgumentException if no converter is registered for the entity's class
     */
    public ComponentEntity exportToFile(ComponentEntity entity) {
        debug("[processEntityToYaml]");

        EntityToYamlConverter<Object, Object> converter = (EntityToYamlConverter<Object, Object>) converterMap.get(entity.getClass());

        if(converter == null){
            throw new IllegalArgumentException("No parent converter found for entity " + entity.getClass().getName());
        }
        converter.saveToFile(entity);
        return entity;

    }
    /**
     * Removes previously exported YAML files for a component entity from the filesystem.
     * <p>
     * This method looks up the appropriate {@link EntityToYamlConverter} for the entity's class and
     * delegates to {@code removeExportedFiles()} to delete the corresponding YAML file(s). This is typically
     * used during component deletion or when cleaning up exported artifacts. The converter determines
     * which files to remove based on the entity's identity and configured export paths.
     * </p>
     *
     * @param entity the component entity whose exported files should be removed, must not be null
     * @return the same entity instance for method chaining
     * @throws IllegalArgumentException if no converter is registered for the entity's class
     */
    public ComponentEntity removeExportedFiles(ComponentEntity entity){
        EntityToYamlConverter<Object, Object> converter = (EntityToYamlConverter<Object, Object>) converterMap.get(entity.getClass());

        if(converter == null){
            throw new IllegalArgumentException("No parent converter found for entity " + entity.getClass().getName());
        }
        converter.removeExportedFiles(entity);
        return entity;
    }
    /**
     * Extracts the entity class handled by a converter through reflection on its generic superclass.
     * <p>
     * This method uses Java reflection to determine the entity type (first generic type parameter) from
     * the converter's superclass. It handles two cases:
     * <ul>
     *   <li>If the generic superclass is a raw Class, it returns that class directly</li>
     *   <li>If the generic superclass is a ParameterizedType, it extracts the first type argument</li>
     * </ul>
     * This reflection-based approach enables automatic discovery of entity-to-converter mappings without
     * requiring explicit registration.
     * </p>
     *
     * @param converter the converter instance to inspect, must not be null
     * @return the Class object representing the entity type handled by this converter
     */
    private static Class<?> getEntityType(EntityToYamlConverter<?, ?> converter) {
        if (converter.getClass().getGenericSuperclass().getClass().getName().equals(Class.class.getName())) {
            return (Class<?>) converter.getClass().getGenericSuperclass();
        }

        ParameterizedType type = (ParameterizedType) converter.getClass().getGenericSuperclass();
        return (Class<?>) type.getActualTypeArguments()[0];
    }
}
