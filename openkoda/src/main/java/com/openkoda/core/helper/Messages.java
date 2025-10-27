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

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Helper component that simplifies access to internationalized (i18n) messages from messages_*.properties files.
 * <p>
 * This component provides convenient methods for retrieving localized messages using Spring's MessageSource,
 * with automatic fallback to human-readable label generation from field names. When a message key is not found
 * in the properties files, the component generates default labels by splitting camelCase field names into
 * capitalized words (e.g., "userName" becomes "User Name").
 * </p>
 * <p>
 * The Messages component uses Spring's {@link MessageSourceAccessor} for message resolution, which automatically
 * loads messages from src/main/resources/messages_*.properties files. The implementation uses a hard-coded
 * English locale for consistency.
 * </p>
 * <p>
 * Usage example:
 * <pre>
 * String label = messages.getFieldLabel("user.name.label", "userName");
 * String placeholder = messages.getFieldPlaceholder("user.name.placeholder", "userName");
 * </pre>
 * </p>
 * <p>
 * Thread-safety: This component is thread-safe. The underlying {@link MessageSourceAccessor} is thread-safe
 * and safe for concurrent use across multiple threads.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.springframework.context.MessageSource
 * @see org.springframework.context.support.MessageSourceAccessor
 */
@Component("messages")
public class Messages {

    /**
     * Sentinel value used internally to detect when a message key is not found in the message properties files.
     * This constant allows distinguishing between missing messages and empty string messages.
     */
    private static final String NO_MESSAGE = "|NO_MESSAGE|";
    @Inject
    private MessageSource messageSource;

    private MessageSourceAccessor accessor;

    /**
     * Configuration flag that controls whether message keys are appended to auto-generated labels for debugging.
     * When enabled, default labels will include the message key in parentheses (e.g., "User Name (user.name.label)").
     * This is useful during development to identify which fields lack proper i18n message definitions.
     * <p>
     * Configured via application property: show.message.key.for.default.field.label (default: false)
     * </p>
     */
    @Value("${show.message.key.for.default.field.label:false}")
    private boolean showMessageKeyForDefaultFieldLabel;

    /**
     * Initializes the MessageSourceAccessor wrapper for convenient message retrieval.
     * This method is called automatically by Spring after dependency injection is complete.
     * The accessor provides a simplified API for retrieving messages with default values.
     *
     * @see MessageSourceAccessor
     */
    @PostConstruct
    private void init() {
        accessor = new MessageSourceAccessor(messageSource);
    }

    /**
     * Retrieves a localized message by its message key, with optional arguments for parameterized messages.
     * If the message key is not found in the properties files, returns an empty string.
     * <p>
     * This method supports parameterized messages using standard MessageFormat syntax.
     * Arguments are substituted into placeholders like {0}, {1}, etc.
     * </p>
     *
     * @param code the message key to look up in messages_*.properties files
     * @param args optional arguments for parameterized messages (substituted into {0}, {1}, etc. placeholders)
     * @return the resolved localized message, or an empty string if the message key is not found
     */
    public String get(String code, String ... args) {
        return accessor.getMessage(code, args, "");
    }


    /**
     * Retrieves a field label message by its message key, or generates a human-readable default label if not found.
     * <p>
     * This method first attempts to resolve the message key from the properties files. If the key is not found,
     * it automatically generates a default label by splitting the camelCase field name into capitalized words.
     * For example, "userName" becomes "User Name", and "userEmailAddress" becomes "User Email Address".
     * </p>
     * <p>
     * When debug mode is enabled (showMessageKeyForDefaultFieldLabel = true), the message key is appended
     * to the generated default label in parentheses, helping developers identify fields that lack proper
     * i18n message definitions (e.g., "User Name (user.name.label)").
     * </p>
     *
     * @param code the message key to look up in messages_*.properties files
     * @param fieldName the field name in camelCase format, used for generating the default label if the message key is not found
     * @return the resolved message from properties, or a capitalized, space-separated label generated from the field name
     */
    public String getFieldLabel(String code, String fieldName) {
        String result = accessor.getMessage(code, NO_MESSAGE);
        if (NO_MESSAGE.equals(result)) {
            result = getDefaultLabel(fieldName);
            if (showMessageKeyForDefaultFieldLabel) {
                result += " (" + code + ")";
            }
        }
        return result;
    }

    /**
     * Generates a human-readable default label from a camelCase field name by splitting and capitalizing words.
     * <p>
     * This method transforms camelCase identifiers into user-friendly labels by:
     * <ul>
     *   <li>Splitting camelCase words (e.g., "userName" splits to ["user", "Name"])</li>
     *   <li>Capitalizing each word</li>
     *   <li>Joining words with spaces</li>
     *   <li>Filtering out dot characters that may appear in nested field names</li>
     * </ul>
     * </p>
     * <p>
     * Examples:
     * <ul>
     *   <li>"userName" → "User Name"</li>
     *   <li>"userEmailAddress" → "User Email Address"</li>
     *   <li>"organizationName" → "Organization Name"</li>
     * </ul>
     * </p>
     *
     * @param fieldName the field name in camelCase format
     * @return a capitalized, space-separated label suitable for display in user interfaces
     */
    @NotNull
    public String getDefaultLabel(String fieldName) {
        String result = WordUtils.capitalize(Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(StringUtils.capitalize(fieldName))).filter(s -> !s.equals(".")).collect(Collectors.joining(" ")));
        return result;
    }


    /**
     * Retrieves a table header label message by its message key, or generates a default label with special handling for nested entity fields.
     * <p>
     * This method first attempts to resolve the message key from the properties files. If not found, it generates
     * a default label from the field name. For nested entity fields (containing dots), it applies special logic
     * to remove redundant parts. For example, if a field name is "user.userName", and the second part contains
     * the first part, it simplifies to just "userName" to avoid redundancy in the header label.
     * </p>
     * <p>
     * Examples of redundancy removal:
     * <ul>
     *   <li>"user.userName" → "userName" → "User Name"</li>
     *   <li>"organization.organizationName" → "organizationName" → "Organization Name"</li>
     *   <li>"user.email" → "user.email" → "User Email" (no redundancy, kept as-is)</li>
     * </ul>
     * </p>
     * <p>
     * When debug mode is enabled, the message key is appended to the generated label.
     * </p>
     *
     * @param code the message key to look up in messages_*.properties files
     * @param fieldName the field name, potentially with nested entity notation (e.g., "user.userName")
     * @return the resolved message or a capitalized label with redundant parts removed for nested fields
     */
    public String getTableHeaderLabel(String code, String fieldName) {
        String result = accessor.getMessage(code, NO_MESSAGE);
        if (NO_MESSAGE.equals(result)) {
            if(fieldName.contains(".")) {
//                modify header label if it refers to another entity and contains repeats
                String[] fieldNameParts = fieldName.split("\\.");
                if(fieldNameParts[1].contains(fieldNameParts[0])) {
                    fieldName = fieldNameParts[1];
                }
            }
            result = WordUtils.capitalize(Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(StringUtils.capitalize(fieldName))).filter(s -> !s.equals(".")).collect(Collectors.joining(" ")));
            if (showMessageKeyForDefaultFieldLabel) {
                result += " (" + code + ")";
            }
        }
        return result;
    }
    /**
     * Retrieves a field placeholder message by its message key, or generates a default placeholder from the field label.
     * <p>
     * This method first attempts to resolve the placeholder message key from the properties files. If not found,
     * it generates a default placeholder by retrieving the field label and appending "..." to create a typical
     * input placeholder format (e.g., "User Name...").
     * </p>
     * <p>
     * Placeholder messages are typically used in HTML input fields to provide hints about the expected input format.
     * </p>
     *
     * @param code the message key to look up in messages_*.properties files for the placeholder text
     * @param fieldName the field name in camelCase format, used for generating the default placeholder if the message key is not found
     * @return the resolved placeholder message, or the field label with "..." suffix for input placeholders
     */
    public String getFieldPlaceholder(String code, String fieldName) {
        String result = accessor.getMessage(code, NO_MESSAGE);
        if (NO_MESSAGE.equals(result)) {
            result = getFieldLabel(code, fieldName) + "...";
        }
        return result;
    }
    /**
     * Retrieves a field tooltip message by its message key, or returns an empty string if not configured.
     * <p>
     * Unlike other label methods, this method does not generate a default tooltip from the field name.
     * Tooltips are optional UI hints that provide additional context or help text for form fields.
     * If no tooltip message is configured for the given key, an empty string is returned, indicating
     * that no tooltip should be displayed.
     * </p>
     *
     * @param code the message key to look up in messages_*.properties files for the tooltip text
     * @param fieldName the field name (currently unused but kept for API consistency with other methods)
     * @return the resolved tooltip message, or an empty string if not configured
     */
    public String getFieldTooltip(String code, String fieldName) {
        String result = accessor.getMessage(code, NO_MESSAGE);
        if (NO_MESSAGE.equals(result)) {
            result = StringUtils.EMPTY;
        }
        return result;
    }

    /**
     * Retrieves a database column label message by its message key, or generates a default label from snake_case column name.
     * <p>
     * This method is specifically designed for database column names, which typically use snake_case convention.
     * It first attempts to resolve the message key from the properties files. If not found, it generates
     * a default label by replacing underscores with spaces and capitalizing the result.
     * </p>
     * <p>
     * Examples of snake_case to label conversion:
     * <ul>
     *   <li>"user_name" → "User Name"</li>
     *   <li>"email_address" → "Email Address"</li>
     *   <li>"created_at" → "Created At"</li>
     * </ul>
     * </p>
     * <p>
     * When debug mode is enabled, the column name is appended to the generated label in parentheses.
     * </p>
     *
     * @param columnName the database column name in snake_case format
     * @return the resolved message or a capitalized label generated by replacing underscores with spaces
     */
    public String getColumnLabel(String columnName) {
        String result = accessor.getMessage(columnName, NO_MESSAGE);
        if (NO_MESSAGE.equals(result)) {
            result = WordUtils.capitalize(String.join(" ", columnName.split("_")));
            if (showMessageKeyForDefaultFieldLabel) {
                result += " (" + columnName + ")";
            }
        }
        return result;
    }
}
