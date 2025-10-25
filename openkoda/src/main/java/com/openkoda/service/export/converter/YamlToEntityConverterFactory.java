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

import com.openkoda.controller.ComponentProvider;
import com.openkoda.model.OpenkodaModule;
import com.openkoda.model.Organization;
import com.openkoda.service.export.dto.ComponentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring component extending ComponentProvider that auto-registers converters annotated with {@code @YamlToEntityParentConverter}.
 * <p>
 * This factory discovers and manages {@link YamlToEntityConverter} implementations at application startup,
 * building a registry keyed by DTO class. It provides a centralized entry point for processing deserialized
 * YAML DTOs and converting them into persisted domain entities during import operations.
 * </p>
 * <p>
 * At construction time, the factory inspects each autowired converter for the {@link YamlToEntityParentConverter}
 * annotation. Converters bearing this annotation are registered in a private static {@code Map<Class<?>, YamlToEntityConverter<?,?>}
 * keyed by the {@code dtoClass} attribute declared in the annotation. This registration occurs once during Spring
 * bean initialization and relies on Spring's initialization ordering guarantees.
 * </p>
 * <p>
 * The {@code processYamlDto} methods resolve the appropriate converter by inspecting the runtime class of the
 * provided DTO. If no converter is registered for the DTO class, an {@code IllegalArgumentException} is thrown.
 * The no-resources overload performs additional precondition checks to ensure referenced {@link Organization}
 * and {@link OpenkodaModule} entities exist in the database, creating placeholder entities when necessary.
 * This prevents foreign key constraint violations during entity persistence.
 * </p>
 * <p>
 * Thread-safety: The static registry map is populated during construction and remains immutable thereafter.
 * Read operations are thread-safe without synchronization. The factory is not designed to support dynamic
 * registration of converters after initialization.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * FormConversionDto dto = yamlParser.parse(yamlContent);
 * Form savedForm = factory.processYamlDto(dto, "forms/contact-form.yaml");
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see YamlToEntityConverter
 * @see YamlToEntityParentConverter
 * @see ComponentProvider
 */
@Component
public class YamlToEntityConverterFactory extends ComponentProvider {
    /**
     * Static registry mapping DTO classes to their corresponding converter implementations.
     * <p>
     * Populated during construction by inspecting {@link YamlToEntityParentConverter} annotations
     * on autowired converter beans. The map remains immutable after initialization and provides
     * thread-safe read access for converter lookups during runtime import operations.
     * </p>
     */
    private static Map<Class<?>, YamlToEntityConverter<?, ?>> parentConverters = new HashMap<>();

    /**
     * Constructs the factory and auto-registers all converters annotated with {@code @YamlToEntityParentConverter}.
     * <p>
     * Spring autowires the complete list of {@link YamlToEntityConverter} beans available in the application
     * context. This constructor iterates through each converter, examines its {@link YamlToEntityParentConverter}
     * annotation (if present), and registers the converter in the static {@code parentConverters} map using
     * the annotation's {@code dtoClass} attribute as the key.
     * </p>
     * <p>
     * Converters lacking the annotation are silently ignored. Duplicate registrations for the same DTO class
     * result in the last-processed converter overwriting earlier entries, with behavior dependent on Spring's
     * bean instantiation order.
     * </p>
     *
     * @param converterList the complete list of {@link YamlToEntityConverter} beans autowired by Spring;
     *                      must not be {@code null} but may be empty if no converters are defined
     */
    @Autowired
    public YamlToEntityConverterFactory(List<YamlToEntityConverter<?, ?>> converterList) {
        for (YamlToEntityConverter<?, ?> converter : converterList) {
            YamlToEntityParentConverter annotation = converter.getClass().getAnnotation(YamlToEntityParentConverter.class);
            if (annotation != null) {
                parentConverters.put(annotation.dtoClass(), converter);
            }
        }
    }

    /**
     * Processes a deserialized YAML DTO and converts it into a persisted domain entity with precondition checks.
     * <p>
     * This method resolves the appropriate {@link YamlToEntityConverter} by inspecting the runtime class of the
     * provided DTO. Before delegation, it performs precondition checks to ensure referenced {@link Organization}
     * and {@link OpenkodaModule} entities exist in the database. If the DTO declares an {@code organizationId}
     * that is not found, a placeholder {@code Organization} is created. Similarly, if the DTO's module name
     * is not registered, a placeholder {@code OpenkodaModule} is created. These preconditions prevent foreign
     * key constraint violations during subsequent entity persistence operations.
     * </p>
     * <p>
     * After precondition setup, the method delegates to the converter's {@code convertAndSave(dto, filePath)}
     * method, which handles DTO-to-entity mapping, validation, and persistence. The returned entity reflects
     * the saved state from the database, including generated IDs and computed fields.
     * </p>
     *
     * @param <T> the type of the domain entity produced by the conversion
     * @param <D> the type of the YAML DTO consumed by the conversion; must extend {@link ComponentDto}
     * @param dto the deserialized DTO to convert; if {@code null}, the method returns {@code null} immediately
     * @param filePath the file path of the original YAML file, used for logging and error reporting;
     *                 must not be {@code null}
     * @return the persisted domain entity resulting from the conversion, or {@code null} if {@code dto} was {@code null}
     * @throws IllegalArgumentException if no converter is registered for the DTO's runtime class
     * @see #processYamlDto(Object, String, Map)
     */
    @SuppressWarnings("unchecked")
    public <T, D> T processYamlDto(D dto, String filePath) {
        debug("[processYamlDto] {}", filePath);

        if (dto == null) {
            return null;
        }
        Long organizationId = ((ComponentDto) dto).getOrganizationId();
        if(organizationId != null && !repositories.unsecure.organization.existsById(organizationId)){
            repositories.unsecure.organization.save(new Organization(organizationId));
        }
        if(!repositories.unsecure.openkodaModule.existsByName(((ComponentDto) dto).getModule())) {
            repositories.unsecure.openkodaModule.save(new OpenkodaModule(((ComponentDto) dto).getModule()));
        }

        YamlToEntityConverter<T, D> converter = (YamlToEntityConverter<T, D>) parentConverters.get(dto.getClass());

        if (converter == null) {
            throw new IllegalArgumentException("No parent converter found for DTO class: " + dto.getClass().getName());
        }
        debug("[processYamlDto] Converting dto: " + dto.getClass().getName());
        return converter.convertAndSave(dto, filePath);
    }

    /**
     * Processes a deserialized YAML DTO with in-memory resources and converts it into a persisted domain entity.
     * <p>
     * This overload resolves the appropriate {@link YamlToEntityConverter} by inspecting the runtime class of
     * the provided DTO and delegates to the converter's {@code convertAndSave(dto, filePath, resources)} method.
     * Unlike {@link #processYamlDto(Object, String)}, this variant does NOT perform precondition checks for
     * {@link Organization} or {@link OpenkodaModule} existence, assuming the caller has already ensured these
     * entities are present or that the converter will handle their creation.
     * </p>
     * <p>
     * The {@code resources} map supplies in-memory content (such as code templates, frontend resources, or
     * configuration files) that the converter may need during entity construction. This allows converters to
     * avoid classpath I/O by accessing resources directly from the map, which is particularly useful during
     * bulk imports or when resources are sourced from ZIP archives.
     * </p>
     *
     * @param <T> the type of the domain entity produced by the conversion
     * @param <D> the type of the YAML DTO consumed by the conversion
     * @param dto the deserialized DTO to convert; if {@code null}, the method returns {@code null} immediately
     * @param filePath the file path of the original YAML file, used for logging and error reporting;
     *                 must not be {@code null}
     * @param resources an in-memory map of resource paths to their string content, used by converters to
     *                  access auxiliary files (e.g., code templates, frontend resources) without classpath lookups;
     *                  must not be {@code null} but may be empty
     * @return the persisted domain entity resulting from the conversion, or {@code null} if {@code dto} was {@code null}
     * @throws IllegalArgumentException if no converter is registered for the DTO's runtime class
     * @see #processYamlDto(Object, String)
     */
    public <T, D> T processYamlDto(D dto, String filePath, Map<String, String> resources) {
        debug("[processYamlDto] {}", filePath);

        if (dto == null) {
            return null;
        }

        YamlToEntityConverter<T, D> converter = (YamlToEntityConverter<T, D>) parentConverters.get(dto.getClass());

        if (converter == null) {
            throw new IllegalArgumentException("No parent converter found for DTO class: " + dto.getClass().getName());
        }
        debug("[processYamlDto] Converting dto: " + dto.getClass().getName());
        return converter.convertAndSave(dto, filePath, resources);
    }
}