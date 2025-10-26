package com.openkoda.core.form;

import java.util.function.Function;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Factory for common field validation functions used in form processing.
 * <p>
 * This class provides static methods that create validation functions for form fields.
 * Each validation function follows a consistent contract: it accepts a string value and
 * returns null if the value is valid, or an error key string if validation fails.
 * The error keys can be used for internationalized error messages.
 * </p>
 * <p>
 * Typical usage with FormFieldDefinitionBuilder:
 * <pre>{@code
 * FormFieldDefinition field = FormFieldDefinitionBuilder.text("username")
 *     .validate(Validator.notBlank())
 *     .validate(Validator.notCamelCase())
 *     .build();
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FormFieldDefinitionBuilder
 */
public class Validator {
    /**
     * Creates a validation function that checks if a string value is not blank.
     * <p>
     * The returned function validates that the input string is not null, empty, or
     * contains only whitespace characters. This is useful for required form fields
     * where the user must provide meaningful input.
     * </p>
     * <p>
     * Validation behavior:
     * <ul>
     *   <li>Valid input (non-blank string): Returns null</li>
     *   <li>Invalid input (null, empty, or whitespace only): Returns "not.empty" error key</li>
     * </ul>
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * Function<String, String> validator = Validator.notBlank();
     * String result = validator.apply("  "); // Returns "not.empty"
     * }</pre>
     * </p>
     *
     * @return a validation function that accepts a string and returns null if valid,
     *         or the error key "not.empty" if the string is blank
     * @see org.apache.commons.lang.StringUtils#isBlank(String)
     */
    public static Function<String,String> notBlank(){
        return v -> isBlank(v) ? "not.empty" : null;
    }
    /**
     * Creates a validation function that checks if a string is in valid camelCase format.
     * <p>
     * The returned function validates that the input string follows camelCase naming
     * conventions: starts with a lowercase letter and contains only alphanumeric characters
     * with no spaces or special characters. This is commonly used for validating identifiers,
     * variable names, or property keys.
     * </p>
     * <p>
     * Valid camelCase format:
     * <ul>
     *   <li>Starts with a lowercase letter (a-z)</li>
     *   <li>Followed by letters (a-z, A-Z) or digits (0-9)</li>
     *   <li>No spaces, underscores, or special characters</li>
     *   <li>Examples: "userName", "totalCount", "value123"</li>
     * </ul>
     * </p>
     * <p>
     * Validation behavior:
     * <ul>
     *   <li>Valid input (proper camelCase): Returns null</li>
     *   <li>Invalid input (blank, uppercase start, special chars): Returns "not.matching.camelCase" error key</li>
     * </ul>
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * Function<String, String> validator = Validator.notCamelCase();
     * String result = validator.apply("UserName"); // Returns "not.matching.camelCase"
     * }</pre>
     * </p>
     *
     * @return a validation function that accepts a string and returns null if it matches
     *         camelCase format, or the error key "not.matching.camelCase" if invalid
     */
    public static Function<String,String> notCamelCase(){
        return v -> isBlank(v) || !v.matches("([a-z]+[a-zA-Z0-9]*)+") ? "not.matching.camelCase" : null;
    }
}
