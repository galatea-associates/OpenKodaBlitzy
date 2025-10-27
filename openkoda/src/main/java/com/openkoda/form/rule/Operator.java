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

package com.openkoda.form.rule;

import com.openkoda.model.OptionWithLabel;

/**
 * Comparison operators for rule expressions in the form/rule subsystem.
 * <p>
 * Centralizes comparison operator metadata including equality, inequality, containment,
 * membership, and relational operators. Implements {@link OptionWithLabel} for UI display
 * in option lists and selection components. Provides SpEL expression templates with
 * placeholders (%s for strings, %f for floats) for operand substitution in rule evaluation.
 * </p>
 * <p>
 * Each operator carries applicability flags ({@code stringsOperator}, {@code numbersOperator})
 * that inform UI and evaluator logic whether the operator is valid for string or numeric
 * operand types. Implementers must apply correct escaping and formatting when substituting
 * values into SpEL templates to avoid injection or formatting errors.
 * </p>
 * <p>
 * <strong>Mutability Warning:</strong> Setters exist making enum singletons mutable at runtime.
 * Changes are process-local and immediate for all consumers with no synchronization.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see OptionWithLabel
 * @see LogicalOperator
 */
public enum Operator implements OptionWithLabel {

    /**
     * Equality operator with label "==" and SpEL template " == '%s'".
     * Applicable to both string and numeric operands.
     */
    equals("==", " == '%s'", true, true),
    
    /**
     * Inequality operator with label "!=" and SpEL template " != '%s'".
     * Applicable to both string and numeric operands.
     */
    notEquals("!=", " != '%s'", true, true),
    
    /**
     * String containment operator with label "Contains" and method-like fragment ".contains('%s')".
     * Applicable to string operands only.
     */
    contains("Contains", ".contains('%s')", true, false),
    
    /**
     * Collection membership operator with label "In" and template ".contains(%s)".
     * Applicable to both string and numeric operands.
     */
    in("In", ".contains(%s)", true, true),
    
    /**
     * Greater-than relational operator with label ">" and SpEL template " > '%f'".
     * Applicable to numeric operands only.
     */
    greaterThan(">", " > '%f'", false, true),
    
    /**
     * Less-than relational operator with label "<" and SpEL template " < '%f'".
     * Applicable to numeric operands only.
     */
    lessThan("<", " < '%f'", false, true),
    ;

    /**
     * Human-readable label for UI display (satisfies OptionWithLabel interface).
     */
    private String label;
    
    /**
     * SpEL expression template with placeholders (%s, %f) or method-like fragments (.contains).
     */
    private String spel;
    
    /**
     * Boolean flag indicating applicability to string operands.
     */
    private Boolean stringsOperator;
    
    /**
     * Boolean flag indicating applicability to numeric operands.
     */
    private Boolean numbersOperator;

    /**
     * Initializes an Operator with label, SpEL template, and applicability flags.
     *
     * @param label human-readable label for UI display
     * @param spel SpEL expression template with placeholders for operand substitution
     * @param stringsOperator true if operator applies to string operands
     * @param numbersOperator true if operator applies to numeric operands
     */
    Operator(String label, String spel, Boolean stringsOperator, Boolean numbersOperator) {
        this.label = label;
        this.spel = spel;
        this.stringsOperator = stringsOperator;
        this.numbersOperator = numbersOperator;
    }

    /**
     * Returns the human-readable label for UI display.
     * Overrides {@link OptionWithLabel#getLabel()} for integration with UI option lists.
     *
     * @return the operator label (e.g., "==", "Contains", ">")
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * Sets the operator label.
     * <p>
     * <strong>Warning:</strong> Mutates enum singleton state. Changes are process-local,
     * immediate for all consumers, and unsynchronized. Avoid runtime mutations in production.
     * </p>
     *
     * @param label the new label value
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the SpEL expression template for this operator.
     * Templates contain placeholders (%s, %f) or method-like fragments requiring operand substitution.
     *
     * @return the SpEL template (e.g., " == '%s'", ".contains('%s')")
     */
    public String getSpel() {
        return spel;
    }

    /**
     * Sets the SpEL expression template.
     * <p>
     * <strong>Warning:</strong> Mutates enum singleton state. Changes are process-local,
     * immediate for all consumers, and unsynchronized. Avoid runtime mutations in production.
     * </p>
     *
     * @param spel the new SpEL template
     */
    public void setSpel(String spel) {
        this.spel = spel;
    }

    /**
     * Returns whether this operator applies to string operands.
     * UI and evaluator logic use this flag to determine valid operators for string types.
     *
     * @return true if operator is valid for strings, false otherwise
     */
    public Boolean getStringsOperator() {
        return stringsOperator;
    }

    /**
     * Sets the string applicability flag.
     * <p>
     * <strong>Warning:</strong> Mutates enum singleton state. Changes are process-local,
     * immediate for all consumers, and unsynchronized. Avoid runtime mutations in production.
     * </p>
     *
     * @param stringsOperator true if operator applies to strings
     */
    public void setStringsOperator(Boolean stringsOperator) {
        this.stringsOperator = stringsOperator;
    }

    /**
     * Returns whether this operator applies to numeric operands.
     * UI and evaluator logic use this flag to determine valid operators for numeric types.
     *
     * @return true if operator is valid for numbers, false otherwise
     */
    public Boolean getNumbersOperator() {
        return numbersOperator;
    }

    /**
     * Sets the numeric applicability flag.
     * <p>
     * <strong>Warning:</strong> Mutates enum singleton state. Changes are process-local,
     * immediate for all consumers, and unsynchronized. Avoid runtime mutations in production.
     * </p>
     *
     * @param numbersOperator true if operator applies to numbers
     */
    public void setNumbersOperator(Boolean numbersOperator) {
        this.numbersOperator = numbersOperator;
    }

    /**
     * Finds an Operator constant by label using case-insensitive matching.
     * Performs a linear scan of all enum values.
     *
     * @param text the label to search for (case-insensitive)
     * @return the matching Operator constant, or null if no match is found
     */
    public static Operator fromString(String text) {
        for(Operator operator : Operator.values()) {
            if(operator.label.equalsIgnoreCase(text)) {
                return operator;
            }
        }
        return null;
    }
}
