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

import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.CaseFormat.*;

/**
 * Provides naming convention utilities for converting between different case formats.
 * <p>
 * This helper class converts between camelCase, snake_case, and PascalCase using Guava CaseFormat.
 * It is primarily used for dynamic entity generation, repository naming, and database schema mapping.
 * The class facilitates the translation of Java naming conventions to database naming conventions and vice versa.
 * <p>
 * Example usage:
 * <pre>{@code
 * String tableName = NameHelper.toTableName("userRole"); // Returns "user_role"
 * String fieldName = NameHelper.toFieldName("user_name"); // Returns "userName"
 * }</pre>
 * <p>
 * Thread-safety: All methods are static with no shared mutable state, making them safe for concurrent use.
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.google.common.base.CaseFormat
 */
@Component("namehelper")
public class NameHelper {

    /**
     * Delimiter used for separating multiple values in concatenated strings.
     * This constant is used when joining or splitting composite identifiers or value lists.
     */
    public final static String DELIMITER = "#;#";

    /**
     * Creates a formatted method signature description for documentation purposes.
     * <p>
     * The signature format is "ClassName :: methodName(Type1, Type2)". This format provides
     * a clear and readable representation of method signatures, useful for logging, debugging,
     * and generating dynamic documentation.
     * 
     *
     * @param method the Method object to describe (null-safe, returns empty string if null)
     * @return formatted method signature string, or empty string if method is null
     */
    public static String createMethodDescription(Method method) {
        if (method == null) { return ""; }

        String signature = method.getDeclaringClass().getSimpleName()
                + " :: "
                + method.getName()
                + "("
                + Arrays.stream(method.getGenericParameterTypes()).map(pt -> getClassName(pt.getTypeName())).collect
                (Collectors.joining(", "))
                + ")";
        return signature;
    }

    /**
     * Extracts the simple class name from a fully qualified type name.
     * <p>
     * This method returns the substring after the last dot character, effectively
     * converting a fully qualified class name to its simple name.
     * 
     *
     * @param eventObjectType the fully qualified type name (e.g., "com.openkoda.model.User")
     * @return the simple class name (e.g., "User"), or the original string if no dot is present
     */
    public static String getClassName(String eventObjectType) {
        return StringUtils.substringAfterLast(eventObjectType, ".");
    }

    /**
     * Converts an array of fully qualified class name strings to an array of Class objects.
     * <p>
     * This method uses reflection to load each class by name. If a class cannot be found,
     * the ClassNotFoundException is caught, the stack trace is printed, and an empty array
     * is returned instead of propagating the exception.
     * 
     *
     * @param classNames array of fully qualified class names (e.g., "java.lang.String")
     * @return array of Class objects corresponding to the class names, or empty array if
     *         classNames is null or if any ClassNotFoundException occurs
     */
    public static Class<?>[] getClasses(String[] classNames){
        try {
            if(classNames != null){
                List<Class<?>> classes = new ArrayList<>();
                for(String cs : classNames){
                    classes.add(Class.forName(cs));
                }
                return classes.toArray(new Class[0]);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return (Class<?>[]) new Class[0];
    }

    /**
     * Converts an entity key from lowerCamel format to database table name in lower_underscore format.
     * <p>
     * This conversion is used when mapping Java entity names to database table names,
     * following standard database naming conventions.
     * 
     * <p>
     * Example: "userRole" converts to "user_role"
     * 
     *
     * @param entityKey the entity key in lowerCamel format (e.g., "userRole")
     * @return the table name in lower_underscore format (e.g., "user_role")
     */
    public static String toTableName(String entityKey){
         return CaseFormat.LOWER_CAMEL.to(LOWER_UNDERSCORE, entityKey);
    }

    /**
     * Converts a form name from lowerCamel format to entity class name in UpperCamel (PascalCase) format.
     * <p>
     * This conversion is used when generating entity class names from form definitions,
     * following Java class naming conventions.
     * 
     * <p>
     * Example: "userRole" converts to "UserRole"
     * 
     *
     * @param formName the form name in lowerCamel format (e.g., "userRole")
     * @return the entity class name in UpperCamel format (e.g., "UserRole")
     */
    public static String toEntityClassName(String formName){
        return LOWER_CAMEL.to(UPPER_CAMEL, formName);
    }

    /**
     * Converts a form name to an entity key.
     * <p>
     * This method currently serves as an identity function, returning the form name unchanged.
     * It provides a consistent API for entity key generation and allows for future modifications
     * to the entity key format without changing client code.
     * 
     *
     * @param formName the form name to convert
     * @return the entity key (currently identical to the input formName)
     */
    public static String toEntityKey(String formName){
        return formName;
    }

    /**
     * Converts a database column name from lower_underscore format to Java field name in lowerCamel format.
     * <p>
     * This conversion is used when mapping database column names to Java field names,
     * following standard Java naming conventions.
     * 
     * <p>
     * Example: "user_name" converts to "userName"
     * 
     *
     * @param columnName the database column name in lower_underscore format (e.g., "user_name")
     * @return the Java field name in lowerCamel format (e.g., "userName")
     */
    public static String toFieldName(String columnName){
        return LOWER_UNDERSCORE.to(LOWER_CAMEL, columnName);
    }

    /**
     * Converts a Java field name from lowerCamel format to database column name in lower_underscore format.
     * <p>
     * This conversion is used when mapping Java field names to database column names,
     * following standard database naming conventions.
     * 
     * <p>
     * Example: "userName" converts to "user_name"
     * 
     *
     * @param fieldName the Java field name in lowerCamel format (e.g., "userName")
     * @return the database column name in lower_underscore format (e.g., "user_name")
     */
    public static String toColumnName(String fieldName){
        return LOWER_CAMEL.to(LOWER_UNDERSCORE, fieldName);
    }

    /**
     * Generates a secure repository class name from a form name.
     * <p>
     * The generated repository name has the format "SecureGenerated[EntityName]Repository",
     * where [EntityName] is the PascalCase version of the form name. This naming convention
     * is used for dynamically generated secure repository classes.
     * 
     * <p>
     * Example: "user" converts to "SecureGeneratedUserRepository"
     * 
     *
     * @param formName the entity form name (e.g., "user")
     * @return the repository class name with "SecureGenerated" prefix (e.g., "SecureGeneratedUserRepository")
     */
    public static String toRepositoryName(String formName){
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, "SecureGenerated" + toEntityClassName(formName) + "Repository");
    }

}
