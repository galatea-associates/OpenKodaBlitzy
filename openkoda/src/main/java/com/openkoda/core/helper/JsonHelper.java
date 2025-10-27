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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.openkoda.controller.common.PageAttributes;
import com.openkoda.core.flow.LoggingComponent;
import com.openkoda.core.flow.PageAttr;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;


/**
 * Provides JSON serialization and deserialization utilities using Jackson ObjectMapper and Gson.
 * <p>
 * This helper class offers static methods for converting Java objects to JSON and vice versa.
 * It includes custom serialization support for {@link PageModelMap} that encodes value class names
 * into field keys for type preservation during deserialization.
 * </p>
 * <p>
 * The class uses two JSON libraries:
 * <ul>
 *   <li>Jackson ObjectMapper - configured for indented output and Java Time support</li>
 *   <li>Gson - configured to use {@code @Expose} annotation filtering</li>
 * </ul>
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * String json = JsonHelper.toDebugJson(pageModel);
 * PageModelMap restored = JsonHelper.fromDebugJson(json);
 * </pre>
 * </p>
 * <p>
 * Thread-safety: The shared static ObjectMapper and Gson instances are thread-safe for read operations
 * and safe for concurrent use across multiple threads.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see PageModelMap
 * @see ObjectMapper
 */
@Component("json")
public class JsonHelper implements LoggingComponentWithRequestId {

    private static final ObjectMapper om = new ObjectMapper();
    private static final String keyClassSeparator = "@";


    /**
     * Custom Jackson serializer that appends class names to JSON field names for type preservation.
     * <p>
     * This serializer encodes the class name of each value directly into the JSON field key
     * by appending "{@code @ClassName}" to the field name. This allows the corresponding
     * deserializer to reconstruct the original typed values.
     * </p>
     * <p>
     * Example output: {@code {"userName@java.lang.String": "admin"}}
     * </p>
     *
     * @see KeyWithClassDeserializer
     */
    public static class KeyWithClassSerializer extends JsonSerializer<PageModelMap> {

        /**
         * Serializes a PageModelMap to JSON with class names appended to field names.
         *
         * @param value the PageModelMap to serialize
         * @param jgen the JSON generator for writing output
         * @param provider the serializer provider
         * @throws IOException if an I/O error occurs during serialization
         */
        @Override
        public void serialize(PageModelMap value, JsonGenerator jgen, SerializerProvider provider) throws IOException, org.codehaus.jackson.JsonProcessingException {
            jgen.writeStartObject();
            for (Map.Entry<String, Object> e : value.entrySet()) {
                jgen.writeFieldName(e.getKey() + (e.getValue() == null ? "" : keyClassSeparator + e.getValue().getClass().getCanonicalName()));
                jgen.writeObject(e.getValue());
            }
            jgen.writeEndObject();
        }
    }

    /**
     * Custom Jackson deserializer that reconstructs typed values from JSON with class-annotated field names.
     * <p>
     * This deserializer parses JSON field names containing "{@code @ClassName}" suffixes and uses
     * the class information to deserialize values with their correct types. If a class cannot be found,
     * a ClassNotFoundException is logged and the field is deserialized as a generic Object.
     * </p>
     * <p>
     * Example input: {@code {"userName@java.lang.String": "admin"}} becomes PageModelMap with String value.
     * </p>
     *
     * @see KeyWithClassSerializer
     */
    public static class KeyWithClassDeserializer extends JsonDeserializer<PageModelMap> {

        /**
         * Deserializes JSON with class-annotated field names back to a PageModelMap with correctly typed values.
         *
         * @param p the JSON parser
         * @param ctxt the deserialization context
         * @return a PageModelMap with typed values reconstructed from the JSON
         * @throws IOException if an I/O error occurs during deserialization
         * @throws JsonProcessingException if JSON processing fails
         */
        @Override
        public PageModelMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            PageModelMap result = new PageModelMap();
            for (Iterator<Map.Entry<String, JsonNode>> i = node.fields(); i.hasNext(); ) {
                Map.Entry<String, JsonNode> f = i.next();
                String[] fieldNameAndClass = StringUtils.split(f.getKey(), keyClassSeparator);
                if (fieldNameAndClass.length == 1) {
                    result.put(fieldNameAndClass[0], om.convertValue(f.getValue(), Object.class));
                } else {
                    try {
                        Class c = ctxt.findClass(fieldNameAndClass[1]);
                        Object v = om.convertValue(f.getValue(), c);
                        result.put(fieldNameAndClass[0], v);
                    } catch (ClassNotFoundException e) {
                        LoggingComponent.debugLogger.debug("[deserialize]", e);
                    }
                }
            }
            return result;
        }

    }

    /**
     * Static initialization block configuring the shared ObjectMapper instance.
     * <p>
     * Configuration includes:
     * <ul>
     *   <li>INDENT_OUTPUT - enables pretty-printed JSON output</li>
     *   <li>FAIL_ON_EMPTY_BEANS disabled - allows serialization of beans without properties</li>
     *   <li>WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS - ensures timestamp precision</li>
     *   <li>JavaTimeModule - adds support for Java 8 date/time types</li>
     *   <li>Custom module - registers KeyWithClassSerializer and KeyWithClassDeserializer for PageModelMap</li>
     * </ul>
     * </p>
     */
    static {
        om.enable(SerializationFeature.INDENT_OUTPUT);
        om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        om.enable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
        om.registerModule(new JavaTimeModule());
        om.enable(MapperFeature.USE_ANNOTATIONS);
        SimpleModule module = new SimpleModule();
        module.addSerializer(PageModelMap.class, new KeyWithClassSerializer());
        module.addDeserializer(PageModelMap.class, new KeyWithClassDeserializer());
        om.registerModule(module);
    }

    /**
     * Shared Gson instance configured to use {@code @Expose} annotation filtering.
     * Only fields annotated with {@code @Expose} are included in serialization.
     */
    private static final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    /**
     * Deserializes a JSON string to an object of the specified type using Gson.
     * <p>
     * This method uses the Gson library configured with {@code @Expose} annotation filtering.
     * Only fields annotated with {@code @Expose} in the target class are populated.
     * </p>
     *
     * @param <T> the type of the object to deserialize
     * @param jsonString the JSON string to deserialize
     * @param t the class of the target object
     * @return the deserialized object of type T
     */
    public static <T> T from(String jsonString, Class<T> t) {
        return gson.fromJson(jsonString, t);
    }

    /**
     * Serializes an object to a JSON string using Gson.
     * <p>
     * This method uses the Gson library configured with {@code @Expose} annotation filtering.
     * Only fields annotated with {@code @Expose} are included in the JSON output.
     * </p>
     *
     * @param <T> the type of the object to serialize
     * @param object the object to serialize
     * @return the JSON string representation of the object
     */
    public static <T> String to(T object) {
        return gson.toJson(object);
    }

    /**
     * Performs a basic syntax check to determine if a string looks like a JSON object.
     * <p>
     * This method checks if the trimmed input starts with '{' and ends with '}' to identify
     * potential JSON objects. It does not validate the complete JSON syntax or structure.
     * </p>
     *
     * @param jsonInput the string to check for JSON object format
     * @return true if the input looks like a JSON object (starts with '{' and ends with '}'), false otherwise
     * @throws IOException if an I/O error occurs
     */
    public static boolean looksLikeJsonObject(String jsonInput) throws IOException {
        if (StringUtils.isBlank(jsonInput)) {
            return false;
        }
        String trimmed = jsonInput.trim();
        return trimmed.charAt(0) == '{' && trimmed.charAt(trimmed.length() - 1) == '}';
    }

    /**
     * Converts a JSON string to a PageModelMap with typed values using custom deserialization.
     * <p>
     * This method uses the custom {@link KeyWithClassDeserializer} to reconstruct typed values
     * from JSON field names that contain class information. If the input is blank, returns an empty
     * PageModelMap. The deserializer handles type reconstruction by parsing class names from
     * field key suffixes.
     * </p>
     *
     * @param jsonInput the JSON string to deserialize, may be null or blank
     * @return a PageModelMap with typed values reconstructed from the JSON, or empty map if input is blank
     * @throws IOException if an I/O error occurs during JSON parsing
     * @see KeyWithClassDeserializer
     */
    public static PageModelMap fromDebugJson(String jsonInput) throws IOException {
        if (StringUtils.isBlank(jsonInput)) {
            return new PageModelMap();
        }
        TypeReference<PageModelMap> typeRef = new TypeReference<PageModelMap>() {};
        PageModelMap result;
        result = om.readValue(jsonInput, typeRef);
        return result;
    }

    /**
     * Converts a Map to a JSON string for debugging purposes with PageAttr filtering.
     * <p>
     * This method filters the input map to include only entries that correspond to known
     * {@link PageAttr} values and removes circular references such as modelAndView. The resulting
     * PageModelMap is serialized using the custom {@link KeyWithClassSerializer} to preserve type
     * information. If serialization fails, returns an error message.
     * </p>
     *
     * @param model the map to serialize, typically a Spring MVC model
     * @return a JSON string representation of the filtered model, or an error message if serialization fails
     * @see KeyWithClassSerializer
     * @see PageAttr
     */
    public static String toDebugJson(Map<String, Object> model) {

        PageModelMap pageModelMap = new PageModelMap();

        for (Map.Entry<String, Object> e : model.entrySet()) {
            if (PageAttr.getByName(e.getKey()) != null) {
                pageModelMap.put(e.getKey(), e.getValue());
            }
        }

        //model that contains model itself causes circular serialization, so removing
        pageModelMap.remove(PageAttributes.modelAndView);

        try {
            return om.writeValueAsString(pageModelMap);
        } catch (JsonProcessingException e) {
            return "Error " + e.getMessage();
        }
    }

    /**
     * Converts a map to a JSON string using manual string building with escaping.
     * <p>
     * This method manually constructs a JSON string from map entries, escaping double quotes
     * and removing "dto." prefixes from keys. It uses simple string concatenation and is suitable
     * for form data serialization.
     * </p>
     *
     * @param map the map to convert, with string keys and values
     * @return a JSON string representation of the map
     */
    public static String formMapToJson(Map<String, String> map) {
        String json = "{";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            json += "\"" + entry.getKey().replace("\"", "\\\"").replaceAll("dto\\.","") + "\"" + ":" + "\"" + entry.getValue().replace("\"", "\\\"") + "\",";
        }
        json = StringUtils.substringBeforeLast(json, ",");
        json += "}";
        return json;
    }
    /**
     * Converts a map to a form object of the specified type using Jackson ObjectMapper.
     * <p>
     * This method uses Jackson's convertValue to populate a form object from map data.
     * The map keys should match the form object's field names.
     * </p>
     *
     * @param <T> the type of the form object
     * @param map the source data map with field names as keys
     * @param formClass the class of the target form object
     * @return a populated form object of type T
     */
    public static <T> T formMapToForm(Map<String,Object> map, Class<T> formClass) {
        return om.convertValue(map, formClass);
    }

}
