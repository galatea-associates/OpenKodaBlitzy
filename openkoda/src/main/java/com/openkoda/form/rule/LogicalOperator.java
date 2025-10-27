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
 * Logical operators for rule expressions in the form/rule subsystem.
 * <p>
 * Models AND and OR operators used by the form/rule subsystem for building logical expressions.
 * Implements {@link OptionWithLabel} for use in UI option lists, providing human-readable labels
 * for operator selection in form components.
 * </p>
 * <p>
 * Each operator provides a SpEL (Spring Expression Language) template fragment with surrounding
 * whitespace for direct concatenation into textual rule expressions. The SpEL fragments include
 * leading and trailing spaces to ensure proper spacing when concatenated with rule clauses.
 * </p>
 * <p>
 * <b>Mutability Warning:</b> This enum provides setters for label and spel fields, making the
 * singleton enum instances mutable at runtime. Modifications affect all consumers in the same
 * JVM process and are not thread-safe. Exercise caution when modifying these values during
 * application runtime.
 * </p>
 * <p>
 * Example usage in rule expression building:
 * <pre>{@code
 * LogicalOperator op = LogicalOperator.and;
 * String expression = "condition1" + op.getSpel() + "condition2";
 * // Results in: "condition1 and condition2"
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see OptionWithLabel
 * @see Operator
 */
public enum LogicalOperator implements OptionWithLabel {
    
    /**
     * AND logical operator with label "AND" and SpEL fragment " and ".
     * <p>
     * Represents the logical AND operation in rule expressions. The SpEL fragment
     * includes surrounding spaces for proper concatenation into textual expressions.
     * </p>
     */
    and("AND", " and "),
    
    /**
     * OR logical operator with label "OR" and SpEL fragment " or ".
     * <p>
     * Represents the logical OR operation in rule expressions. The SpEL fragment
     * includes surrounding spaces for proper concatenation into textual expressions.
     * </p>
     */
    or("OR", " or ")
    ;
    
    /**
     * Human-readable label for UI display.
     * <p>
     * This label is used in form components presenting operator choices to users.
     * Satisfies the {@link OptionWithLabel} interface contract for UI option lists.
     * </p>
     */
    private String label;
    
    /**
     * SpEL (Spring Expression Language) expression fragment with surrounding whitespace.
     * <p>
     * This fragment is designed for direct concatenation into rule clauses. The leading
     * and trailing spaces ensure proper spacing when building composite expressions.
     * </p>
     */
    private String spel;

    /**
     * Constructs a logical operator with the specified label and SpEL fragment.
     * <p>
     * Initializes the operator with a human-readable label for UI display and a
     * SpEL expression fragment for rule expression building.
     * </p>
     *
     * @param label the human-readable label for UI display (e.g., "AND", "OR")
     * @param spel the SpEL expression fragment with surrounding whitespace (e.g., " and ", " or ")
     */
    LogicalOperator(String label, String spel) {
        this.label = label;
        this.spel = spel;
    }

    /**
     * Returns the human-readable label for UI display.
     * <p>
     * This label is used in form components and option lists where users select
     * logical operators. Satisfies the {@link OptionWithLabel#getLabel()} contract.
     * </p>
     *
     * @return the label string (e.g., "AND", "OR"), never {@code null}
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the human-readable label for UI display.
     * <p>
     * <b>Warning:</b> This method mutates the enum singleton instance, affecting all
     * consumers in the same JVM process. The operation is not thread-safe and may cause
     * unpredictable behavior if invoked during concurrent access. Avoid calling this method
     * at runtime; it is primarily intended for initialization or testing scenarios.
     * </p>
     *
     * @param label the new label string (e.g., "AND", "OR")
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the SpEL expression fragment with surrounding whitespace.
     * <p>
     * This fragment is designed for direct concatenation into rule clauses. The leading
     * and trailing spaces ensure proper spacing when building composite expressions.
     * </p>
     *
     * @return the SpEL fragment (e.g., " and ", " or "), never {@code null}
     */
    public String getSpel() {
        return spel;
    }

    /**
     * Sets the SpEL expression fragment with surrounding whitespace.
     * <p>
     * <b>Warning:</b> This method mutates the enum singleton instance, affecting all
     * consumers in the same JVM process. The operation is not thread-safe and may cause
     * unpredictable behavior if invoked during concurrent access. Avoid calling this method
     * at runtime; it is primarily intended for initialization or testing scenarios.
     * </p>
     *
     * @param spel the new SpEL fragment (e.g., " and ", " or ")
     */
    public void setSpel(String spel) {
        this.spel = spel;
    }
}
