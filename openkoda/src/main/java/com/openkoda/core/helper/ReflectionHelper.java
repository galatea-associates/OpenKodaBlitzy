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

package com.openkoda.core.helper;

import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.join;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

/**
 * Provides reflection-based utilities for readable method signature generation,
 * type name simplification, and field type detection.
 * <p>
 * This component offers stateless formatting helpers used for logging, metadata display,
 * and dynamic form generation. All methods are safe for concurrent use.
 * <p>
 * Example usage:
 * <pre>
 * String signature = reflectionHelper.getNameWithParamNamesAndTypes(method);
 * </pre>
 * <p>
 * <b>Performance Warning:</b> Reflection operations are slower than direct access.
 * Cache results when possible to avoid repeated reflection calls.
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see java.lang.reflect.Method
 * @see java.lang.reflect.Field
 */
@Component
public class ReflectionHelper {

    /**
     * Generates a readable method signature with parameter names for logging and debugging.
     * <p>
     * Returns a formatted string in the form "methodName(param1,param2)" which is useful
     * for displaying method information in logs and debug output.
     * 
     *
     * @param method the method to inspect
     * @return formatted signature as "methodName(param1,param2)"
     */
    public String getNameWithParamNames(Method method){
        return method.getName() + "(" + getParameterNames(method) + ")";
    }

    /**
     * Generates a detailed method signature with parameter types and names for metadata display.
     * <p>
     * Returns a formatted string in the form "methodName(Type1 param1, Type2 param2)" which
     * provides complete parameter information useful for API documentation and metadata viewers.
     * 
     *
     * @param method the method to inspect
     * @return formatted signature as "methodName(Type1 param1, Type2 param2)"
     */
    public String getNameWithParamNamesAndTypes(Method method){
        return  method.getName() + "(" + getParameterNamesAndTypes(method) + ")";
    }
    
    /**
     * Generates a complete method signature including return type, prefix, and parameters for code generation.
     * <p>
     * Returns a formatted string in the form "ReturnType prefixMethodName(Type param)" which
     * is useful for generating code stubs and documentation with full type information.
     * 
     *
     * @param method the method to inspect
     * @param methodPrefix prefix to prepend to the method name (e.g., class or module name)
     * @return formatted signature as "ReturnType prefixMethodName(Type param)"
     */
    public String getNameWithParamNamesAndTypesAndReturnType(Method method, String methodPrefix){
        return getShortName(method.getGenericReturnType()) + " " + methodPrefix + method.getName() + "(" + getParameterNamesAndTypes(method) + ")";
    }

    private String getParameterNames(Method method){
        return stream(method.getParameters())
                .map(Parameter::getName)
                .collect(joining(","));
    }

    private String getParameterTypes(Method method) {
        return stream(method.getGenericParameterTypes())
                .map(this::getShortName)
                .collect(joining(","));
    }

    private String getParameterNamesAndTypes(Method method) {
        Parameter[] names = method.getParameters();
        Type[] types = method.getGenericParameterTypes();
        String[] namesAndTypes = new String[names.length];
        for(int i = 0; i < names.length; i++){
            namesAndTypes[i] = getShortName(types[i]) + " " + names[i].getName();
        }
        return join(", ", namesAndTypes);
    }

    /**
     * Removes package names from type name to create simplified type representations.
     * <p>
     * Transforms fully qualified names like "java.util.List&lt;java.lang.String&gt;" to
     * simplified forms like "List&lt;String&gt;". This method is useful for generating
     * readable type names in logs and documentation.
     * 
     * <p>
     * <b>Limitation:</b> Supports only single-level generic types. For example,
     * "List&lt;String&gt;" works correctly, but "List&lt;List&lt;String&gt;&gt;" will not be
     * fully simplified.
     * 
     *
     * @param type the type to process (Class or ParameterizedType)
     * @return simplified type name with packages removed
     */
    public String getShortName(Type type){
        String result="";
        if(type instanceof Class<?>){
            Class<?> cType = (Class<?>) type;
            return cType.getSimpleName();
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            List<String> replaces = new ArrayList<>();
            replaces.add(((Class<?>) pType.getRawType()).getPackageName());
            for(Type t : pType.getActualTypeArguments()){
                if(t instanceof Class<?>) {
                    replaces.add(((Class<?>) t).getPackageName());
                }
            }
            result = type.getTypeName();
            for(String rep : replaces){
                result = result.replace(rep + ".", "");
            }
        }
        return result;
    }
    /**
     * Checks if a field's type is a simple type suitable for basic form fields.
     * <p>
     * Returns true for primitive types (int, boolean), their wrappers (Integer, Boolean),
     * String, and Enum types. This method is used in dynamic form generation to determine
     * which fields can use standard input controls.
     * 
     *
     * @param field the field to check
     * @return true if the field type is primitive, wrapper, String, or Enum; false otherwise
     */
    public boolean isSimpleType(Field field){
        return ClassUtils.isPrimitiveOrWrapper(field.getType()) || field.getType().isAssignableFrom(String.class) || field.getType().isEnum();
    }
    /**
     * Checks if a field's type is a boolean or Boolean wrapper.
     * <p>
     * This method is useful for form generation to determine if a checkbox
     * or toggle control should be used for the field.
     * 
     *
     * @param field the field to check
     * @return true if the field type is boolean or Boolean; false otherwise
     */
    public boolean isBoolean(Field field){
        return field.getType().isAssignableFrom(Boolean.class) || field.getType().isAssignableFrom(boolean.class);
    }

    /**
     * Retrieves all declared methods for a class identified by its fully qualified name.
     * <p>
     * Loads the class dynamically and returns all methods declared in that class,
     * including public, protected, package-private, and private methods. Does not
     * include inherited methods.
     * 
     * <p>
     * <b>Performance Warning:</b> Reflection operations have significant performance cost.
     * Avoid calling this method in hot paths or loops. Cache results when possible.
     * 
     *
     * @param className the fully qualified class name (e.g., "com.openkoda.model.User")
     * @return array of all declared methods in the class
     * @throws RuntimeException wrapping ClassNotFoundException if the class cannot be found
     */
    public Method[] getDeclaredMethods(String className){
        try {
            return Class.forName(className).getDeclaredMethods();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
