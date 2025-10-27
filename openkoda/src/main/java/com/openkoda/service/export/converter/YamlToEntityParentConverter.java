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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runtime-retained type annotation marking YamlToEntityConverter implementations for auto-registration in YamlToEntityConverterFactory.
 * <p>
 * This annotation identifies converter classes that transform YAML data transfer objects (DTOs) into
 * JPA entity instances during the import/export process. Annotated converters are automatically
 * discovered via classpath scanning and registered in the converter factory at application startup.
 * </p>
 * <p>
 * The annotation is retained at runtime ({@link RetentionPolicy#RUNTIME}) to enable reflection-based
 * discovery during factory initialization. It targets type-level declarations ({@link ElementType#TYPE})
 * and should be applied to classes implementing the YamlToEntityConverter interface.
 * </p>
 * <p>
 * Usage example:
 * <pre>{@code
 * @YamlToEntityParentConverter(dtoClass = FormConversionDto.class)
 * public class FormYamlConverter implements YamlToEntityConverter<FormConversionDto, Form> { }
 * }</pre>
 * </p>
 *
 * @see YamlToEntityConverterFactory
 * @see YamlToEntityConverter
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface YamlToEntityParentConverter {
    
    /**
     * Specifies the DTO class that this converter handles during YAML deserialization.
     * <p>
     * The converter factory uses this class reference as a lookup key to map incoming DTO types
     * to their corresponding converter implementations. When importing YAML data, the factory
     * selects the converter whose dtoClass matches the deserialized DTO type.
     * </p>
     *
     * @return the DTO class this converter processes (e.g., FormConversionDto.class)
     */
    Class<?> dtoClass();
}
