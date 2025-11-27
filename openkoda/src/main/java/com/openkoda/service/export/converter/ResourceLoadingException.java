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

/**
 * Unchecked exception signaling failures loading classpath resources during YAML import.
 * <p>
 * This exception extends {@link RuntimeException} and is thrown by YAML-to-entity converters
 * and classpath scanners to translate IO errors into a semantic exception type. It indicates
 * that a required resource file (such as a YAML configuration or entity definition) could not
 * be located or read from the classpath during the export/import process.
 * 
 * <p>
 * Typical usage occurs when {@code YamlToEntityConverter} implementations attempt to load
 * resource files and encounter file system or classpath access issues.
 * 
 *
 * @since 1.7.1
 * @author OpenKoda Team
 * @see YamlToEntityConverter
 */
public class ResourceLoadingException extends RuntimeException{
    
    /**
     * Constructs a new resource loading exception with the specified detail message.
     * <p>
     * The message should describe which resource failed to load and provide context
     * for troubleshooting the classpath configuration issue.
     * 
     *
     * @param message the detail message explaining the resource loading failure
     */
    public ResourceLoadingException(String message) {
        super(message);
    }
}
