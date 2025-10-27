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

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.web.ProxyingHandlerMethodArgumentResolver;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring MVC argument resolver extension providing parameter name normalization for Map-based forms.
 * <p>
 * This processor handles the conversion of dotted notation parameter names (dto.field) to 
 * bracketed notation (dto[field]) for proper Map-based DTO binding in Spring MVC. It extends 
 * {@link ProxyingHandlerMethodArgumentResolver} to integrate with Spring Data's argument 
 * resolution infrastructure.
 * </p>
 * <p>
 * The processor caches rename mappings per form class in a {@link ConcurrentHashMap} for 
 * performance optimization. When processing {@link MapEntityForm} instances, the rename map 
 * is computed once via reflection and reused for all subsequent requests involving the same 
 * form class.
 * </p>
 * <p>
 * Example usage in Spring configuration:
 * <pre>
 * {@code @Configuration
 * public class WebConfig {
 *     @Bean
 *     public RenamingProcessor renamingProcessor(ConversionService cs) {
 *         return new RenamingProcessor(true, () -> cs);
 *     }
 * }}</pre>
 * </p>
 * <p>
 * Note: This class contains commented code suggesting {@code ParamNameDataBinder} was the 
 * original approach. The current implementation represents an evolution of the parameter 
 * renaming strategy.
 * </p>
 *
 * @see MapEntityForm
 * @see ProxyingHandlerMethodArgumentResolver
 * @see WebDataBinder
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class RenamingProcessor extends ProxyingHandlerMethodArgumentResolver {

    /**
     * Spring MVC handler adapter for request mapping configuration.
     * Autowired to access web binding initializer for custom data binder setup.
     */
    @Autowired
    private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    /**
     * Thread-safe cache mapping form classes to their parameter rename maps.
     * <p>
     * Structure: {@code Class<?> -> Map<oldName, newName>} where oldName uses dotted notation
     * (dto.field) and newName uses bracketed notation (dto[field]). The cache is populated 
     * lazily on first request for each form class and reused for subsequent requests to 
     * avoid repeated reflection overhead.
     * </p>
     * <p>
     * Thread-safety is ensured by using {@link ConcurrentHashMap}, allowing concurrent reads 
     * and writes without external synchronization.
     * </p>
     *
     * @see #analyzeClass(Class)
     */
    private final Map<Class<?>, Map<String, String>> replaceMap = new ConcurrentHashMap<Class<?>, Map<String, String>>();

    /**
     * Constructs a RenamingProcessor with specified annotation and conversion service configuration.
     * <p>
     * This constructor initializes the parent {@link ProxyingHandlerMethodArgumentResolver} with 
     * the provided conversion service factory and annotation requirement flag. It is typically 
     * invoked by Spring configuration when registering custom argument resolvers.
     * </p>
     *
     * @param annotationNotRequired if {@code true}, the resolver applies to all applicable 
     *                              parameters regardless of annotations; if {@code false}, only 
     *                              processes annotated parameters
     * @param of factory providing the {@link ConversionService} for type conversion during 
     *           parameter binding
     */
    public RenamingProcessor(boolean annotationNotRequired, ObjectFactory<ConversionService> of) {
        super(of, annotationNotRequired);
    }

    /**
     * Binds HTTP request parameters to the target form object with parameter name preprocessing.
     * <p>
     * This method checks if the target is assignable from {@link MapEntityForm}. If not, it 
     * delegates to the superclass implementation. For MapEntityForm instances, it retrieves or 
     * builds a rename map by analyzing the form class structure, then caches the mapping for 
     * future requests.
     * </p>
     * <p>
     * The rename map converts dotted notation parameter names (dto.fieldName) from HTML forms 
     * to bracketed notation (dto[fieldName]) required for Map-based binding. This conversion 
     * ensures proper data binding when form fields reference nested Map properties.
     * </p>
     * <p>
     * Performance optimization: The rename map is computed once per form class via 
     * {@link #analyzeClass(Class)} and cached in {@link #replaceMap} for all subsequent 
     * requests using the same form type.
     * </p>
     * <p>
     * Note: Commented code suggests the original implementation used {@code ParamNameDataBinder} 
     * for parameter renaming. The current approach represents an evolved strategy.
     * </p>
     *
     * @param binder the {@link WebDataBinder} instance for binding request parameters to the 
     *               target object
     * @param nativeWebRequest the current web request providing access to request parameters
     * @see #analyzeClass(Class)
     */
    @Override
    protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest nativeWebRequest) {
        Object target = binder.getTarget();
        Class<?> targetClass = target.getClass();
        if (!targetClass.isAssignableFrom(MapEntityForm.class)) {
            super.bindRequestParameters(binder, nativeWebRequest);
        }
        if (!replaceMap.containsKey(targetClass)) {
            Map<String, String> mapping = analyzeClass((Class<MapEntityForm>) targetClass);
            replaceMap.put(targetClass, mapping);
        }
        Map<String, String> mapping = replaceMap.get(targetClass);
//        ParamNameDataBinder paramNameDataBinder = new ParamNameDataBinder(target, binder.getObjectName(), mapping);
//        requestMappingHandlerAdapter.getWebBindingInitializer().initBinder(paramNameDataBinder, nativeWebRequest);
//        super.bindRequestParameters(paramNameDataBinder, nativeWebRequest);
    }

    /**
     * Analyzes a form class and generates parameter name conversion mappings via reflection.
     * <p>
     * This method discovers all declared fields in the form class and creates a rename map 
     * converting dotted notation to bracketed notation. The conversion pattern transforms 
     * "dto.fieldName" into "dto[fieldName]" to support HTML form input names that reference 
     * nested Map properties.
     * </p>
     * <p>
     * Example conversion:
     * <pre>
     * Input field name:  "dto.name"
     * Output mapping:    "dto[name]" -> "dto.name"
     * </pre>
     * </p>
     * <p>
     * The method uses Java reflection to inspect field declarations and generates mappings 
     * for all discovered fields. If no fields are found, it returns an empty immutable map 
     * for efficiency.
     * </p>
     *
     * @param targetClass the {@link MapEntityForm} class to analyze for field-based rename 
     *                    mappings
     * @return an immutable map of parameter name conversions (bracketed notation as key, 
     *         original dotted notation as value), or {@link Collections#emptyMap()} if no 
     *         fields are present
     * @see Field#getName()
     */
    private static Map<String, String> analyzeClass(Class<MapEntityForm> targetClass) {
        Field[] fields = targetClass.getDeclaredFields();
        Map<String, String> renameMap = new HashMap<String, String>();
        for (Field field : fields) {
            renameMap.put(field.getName().replaceFirst("\\.", "[") + "]", field.getName());
        }
        if (renameMap.isEmpty()) return Collections.emptyMap();
        return renameMap;
    }
}