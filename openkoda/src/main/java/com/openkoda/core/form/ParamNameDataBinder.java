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

package com.openkoda.core.form;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;

/**
 * Deprecated Spring MVC data binder converting dotted parameter names to bracketed notation for Map-based form binding.
 * <p>
 * This legacy data binder converts HTTP parameter names from dotted notation (dto.field) to bracketed notation
 * (dto[field]) to enable proper binding to Map-based DTOs in Spring MVC. When HTML forms use input names like
 * "dto.fieldName", Spring's default binding treats these as nested JavaBean properties. For Map-based forms
 * (MapEntityForm, OrganizationRelatedMap), bracketed notation is required to correctly populate Map entries.
 * </p>
 * <p>
 * This implementation extends ExtendedServletRequestDataBinder and performs parameter name conversion during
 * construction. The constructor iterates through all property values, identifies names starting with "dto.",
 * and adds renamed entries using replaceFirstLevel() conversion logic.
 * </p>
 * <p>
 * Conversion examples:
 * <ul>
 *   <li>"dto.field" → "dto[field]"</li>
 *   <li>"dto.field.nested" → "dto[field].nested" (first level only)</li>
 *   <li>"dto.field.nested" → "dto[field][nested]" (all levels via replaceAllLevels)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Deprecation Notice:</b> This class is deprecated and replaced by {@link RenamingProcessor} which provides
 * better performance through parameter rename map caching per form class. New code should use RenamingProcessor
 * configured as a custom HandlerMethodArgumentResolver in Spring MVC configuration.
 * </p>
 * <p>
 * Example usage (deprecated approach):
 * <pre>{@code
 * // Old approach - creates new binder instance per request
 * MutablePropertyValues propertyValues = new ServletRequestParameterPropertyValues(request);
 * ParamNameDataBinder binder = new ParamNameDataBinder(form, "form", propertyValues);
 * 
 * // New approach - use RenamingProcessor configured in Spring MVC
 * // RenamingProcessor automatically handles parameter name conversion
 * // for MapEntityForm parameters in controller methods
 * }</pre>
 * </p>
 *
 * @see RenamingProcessor
 * @see MapFormArgumentResolver
 * @see MapEntityForm
 * @see OrganizationRelatedMap
 * @deprecated Replaced by {@link RenamingProcessor} for better performance with cached rename maps
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Deprecated
public class ParamNameDataBinder extends ExtendedServletRequestDataBinder {

    /**
     * Constructs a ParamNameDataBinder and converts dotted parameter names to bracketed notation.
     * <p>
     * Iterates through all property values in the provided MutablePropertyValues collection, identifies
     * parameters starting with "dto.", and adds renamed versions using bracketed notation. The conversion
     * applies {@link #replaceFirstLevel(String)} to transform "dto.field" into "dto[field]" for proper
     * Map-based binding in Spring MVC.
     * </p>
     * <p>
     * This conversion is necessary because HTML forms typically use dotted notation (name="dto.fieldName")
     * which Spring's default binder interprets as nested JavaBean properties. For Map-based DTOs like
     * OrganizationRelatedMap, bracketed notation is required to correctly populate Map entries rather than
     * attempting JavaBean property access.
     * </p>
     * <p>
     * Example transformation:
     * <pre>
     * Input: PropertyValue("dto.firstName", "John")
     * Output: Adds PropertyValue("dto[firstName]", "John")
     * </pre>
     * </p>
     *
     * @param target the target object for data binding (typically a Form instance)
     * @param objectName the name of the target object for binding errors
     * @param pv the MutablePropertyValues containing HTTP request parameters to convert
     * @see #replaceFirstLevel(String)
     * @see ExtendedServletRequestDataBinder
     */
    public ParamNameDataBinder(Object target, String objectName, MutablePropertyValues pv) {
        super(target, objectName);
        for (PropertyValue a : pv.getPropertyValues()) {
            if (a.getName().startsWith("dto.")) {
                String newName = replaceFirstLevel(a.getName());
                pv.addPropertyValue(newName, pv.get(a.getName()));
            }
        }
    }

    /**
     * Converts first-level dotted parameter name to bracketed notation for Map-based binding.
     * <p>
     * Transforms parameter names from dotted notation (dto.field) to bracketed notation (dto[field])
     * for the first level only. Nested levels remain in their original format. This conversion enables
     * proper binding to Map-based DTOs where keys are accessed via bracket notation rather than
     * JavaBean property paths.
     * </p>
     * <p>
     * Conversion algorithm:
     * <ol>
     *   <li>Strips "dto." prefix from the parameter name</li>
     *   <li>Finds the first delimiter (. or [ or () using StringUtils.indexOfAny</li>
     *   <li>If no delimiter found, wraps entire name in brackets: "dto[fieldName]"</li>
     *   <li>If delimiter found, wraps first segment in brackets and preserves rest: "dto[field].nested"</li>
     * </ol>
     * </p>
     * <p>
     * Conversion examples:
     * <pre>
     * "dto.field" → "dto[field]"
     * "dto.field.nested" → "dto[field].nested"
     * "dto.field[0]" → "dto[field][0]"
     * "dto.method()" → "dto[method]()"
     * </pre>
     * </p>
     *
     * @param name the parameter name to convert, must start with "dto."
     * @return the converted parameter name with first-level bracketed notation
     * @see StringUtils#indexOfAny(CharSequence, String)
     * @see #replaceAllLevels(String)
     */
    public static String replaceFirstLevel(String name) {
        String nameWithoutDtoPrefix = name.substring(4);
        int closingPos = StringUtils.indexOfAny(nameWithoutDtoPrefix, ".[(");
        String result;
        if (closingPos < 0) {
            result = "dto[" + nameWithoutDtoPrefix + "]";
        } else {
            result = "dto[" + StringUtils.substring(nameWithoutDtoPrefix, 0, closingPos)
                    + "]" + StringUtils.substring(nameWithoutDtoPrefix, closingPos);
        }
        return result;
    }
    
    /**
     * Converts all levels of dotted parameter name to bracketed notation for nested Map binding.
     * <p>
     * Transforms parameter names from dotted notation (dto.field.nested) to fully bracketed notation
     * (dto[field][nested]) for all levels. This conversion enables proper binding to nested Map structures
     * where each level is accessed via bracket notation.
     * </p>
     * <p>
     * Conversion algorithm:
     * <ol>
     *   <li>Strips "dto." prefix from the parameter name</li>
     *   <li>Replaces all dots (.) with "][" using String.replaceAll</li>
     *   <li>Wraps result in "dto[" prefix and "]" suffix</li>
     * </ol>
     * </p>
     * <p>
     * Conversion examples:
     * <pre>
     * "dto.field" → "dto[field]"
     * "dto.field.nested" → "dto[field][nested]"
     * "dto.field.nested.deep" → "dto[field][nested][deep]"
     * </pre>
     * </p>
     * <p>
     * <b>Note:</b> This method converts all dot delimiters indiscriminately. It does not preserve
     * existing brackets or handle method call syntax. Use {@link #replaceFirstLevel(String)} if you
     * need to preserve nested notation beyond the first level.
     * </p>
     *
     * @param name the parameter name to convert, must start with "dto."
     * @return the converted parameter name with all levels in bracketed notation
     * @see #replaceFirstLevel(String)
     */
    public static String replaceAllLevels(String name) {
        String nameWithoutDtoPrefix = name.substring(4);
        String result = nameWithoutDtoPrefix.replaceAll("\\.", "][");
        result = "dto[" + result + "]";
        return result;
    }

}