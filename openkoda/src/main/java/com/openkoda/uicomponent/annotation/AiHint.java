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

package com.openkoda.uicomponent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field with a concise hint for AI prompt construction and metadata-driven tooling.
 * <p>
 * This annotation embeds developer-authored hints on field declarations to provide semantic
 * metadata for AI systems, UI tooltips, documentation generators, and other runtime consumers.
 * The hint text clarifies the field's meaning, format, units, or usage constraints in a way
 * that enhances automated processing and human comprehension.

 * <p>
 * <b>Retention and Target:</b> Available at runtime via reflection ({@code RetentionPolicy.RUNTIME})
 * and restricted to field declarations only ({@code ElementType.FIELD}).

 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * @AiHint("duration is in seconds")
 * private int timeout;
 * 
 * @AiHint("format: ISO-8601")
 * private String createdDate;
 * }</pre>

 * <p>
 * <b>Accessing at Runtime:</b> Consumers read annotation values via reflection:
 * <pre>{@code
 * Field field = MyClass.class.getDeclaredField("timeout");
 * if (field.isAnnotationPresent(AiHint.class)) {
 *     AiHint hint = field.getAnnotation(AiHint.class);
 *     String hintText = hint.value();
 * }
 * }</pre>

 * <p>
 * <b>Mandatory Value:</b> The {@link #value()} element has no default, making the hint
 * text mandatory at every usage site. Consumers should cache extracted hints for
 * performance and handle missing annotations defensively.

 * <p>
 * <b>Thread Safety:</b> Annotation instances are immutable JVM proxies and inherently thread-safe.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Autocomplete
 * @see java.lang.annotation.Retention
 * @see java.lang.annotation.Target
 * @see java.lang.reflect.Field#getAnnotation(Class)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AiHint {
    /**
     * The hint text providing context or clarification for AI processing.
     * <p>
     * This element must be a concise, descriptive string that explains the field's
     * purpose, format, units, constraints, or other semantic information. The hint
     * should be written to improve AI prompt construction, UI tooltip generation,
     * and automated documentation.

     * <p>
     * <b>Examples:</b>
     * <ul>
     * <li>{@code "duration is in seconds"} - clarifies time units</li>
     * <li>{@code "format: ISO-8601"} - specifies expected date format</li>
     * <li>{@code "nullable; defaults to system locale"} - documents nullability and default behavior</li>
     * <li>{@code "must be positive integer"} - expresses validation constraint</li>
     * </ul>

     * <p>
     * <b>No Default:</b> This element has no default value, making the hint mandatory
     * at every usage site. Consumers can rely on the presence of hint text whenever
     * the annotation is present.

     *
     * @return hint text for the annotated field, never {@code null} or empty when annotation is present
     */
    String value();
}
