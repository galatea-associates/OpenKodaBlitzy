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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Carries optional documentation text to enhance autocompletion suggestions and inline help.
 * <p>
 * This annotation provides human-readable documentation that can be used by IDEs for autocomplete tooltips,
 * inline help, and generated documentation. It is available at runtime via reflection, allowing tools and
 * frameworks to extract documentation metadata programmatically.
 * </p>
 * <p>
 * The annotation can be applied to any program element (classes, fields, methods, parameters) as it does not
 * specify an {@code @Target} restriction. It contains a single optional {@link #doc()} element that defaults
 * to an empty string when not specified. Consumers should treat an empty string as "no documentation provided".
 * </p>
 * <p>
 * <b>Usage Example:</b>
 * <pre>
 * {@code
 * @Autocomplete(doc = "User's primary email address")
 * private String emailAddress;
 * }
 * </pre>
 * </p>
 * <p>
 * <b>Reflection Access Pattern:</b>
 * <pre>
 * {@code
 * Field field = ...;
 * Autocomplete autocomplete = field.getAnnotation(Autocomplete.class);
 * if (autocomplete != null && !autocomplete.doc().isEmpty()) {
 *     String documentation = autocomplete.doc();
 * }
 * }
 * </pre>
 * </p>
 * <p>
 * This annotation can be applied to multiple program elements within the same class or method to provide
 * documentation at different levels of granularity. Annotation instances are immutable JVM proxies and are
 * thread-safe by design.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AiHint
 * @see java.lang.annotation.Retention
 * @see java.lang.annotation.RetentionPolicy
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Autocomplete {

    /**
     * Documentation text for the annotated element.
     * <p>
     * This element provides concise, descriptive text suitable for IDE tooltips, autocomplete suggestions,
     * and generated documentation. When not specified, it defaults to an empty string; consumers should
     * treat an empty string as indicating that no documentation has been provided for the annotated element.
     * </p>
     * <p>
     * The documentation text should be brief and focused, describing the purpose, expected values, or
     * usage guidelines for the annotated element in a way that assists developers during code completion
     * and navigation.
     * </p>
     *
     * @return documentation text, or empty string if not provided
     */
    String doc() default "";
}
