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

package com.openkoda.dto;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Transfer Object for rule engine configuration with if-then-else logic.
 * <p>
 * This DTO represents business rules and conditional workflows used by the rule editor UI.
 * It structures rule definitions using nested maps where the outer map is keyed by sequence
 * numbers (Long) and the inner map is keyed by {@link StatementKey} enum values.

 * <p>
 * The rule structure consists of three statement types:
 * <ul>
 *   <li><b>ifStatements</b> - Condition statements that are evaluated</li>
 *   <li><b>thenStatements</b> - Action statements executed when conditions are true</li>
 *   <li><b>elseStatements</b> - Action statements executed when conditions are false</li>
 * </ul>

 * <p>
 * Usage context: This DTO is used by rule services, form builders, and decision engines
 * to represent and transport rule configurations between layers of the application.

 * <p>
 * Implementation uses {@link TreeMap} to ensure ordered iteration by sequence key,
 * which is important for maintaining the logical order of rule statements.

 * <p>
 * Note: Contains TODO comments regarding Rule 5.5 design principle that DTOs should
 * not contain logic code. The {@link #allValues()} and {@link #splitValues(Map)} methods
 * may need to be refactored to a separate service class in future versions.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class RuleDto {

    /**
     * Constructs a new RuleDto with default empty inner maps.
     * <p>
     * Initializes all three statement maps (if, then, else) with a default entry
     * at sequence key 0L containing an empty inner map. This ensures that the rule
     * structure is ready for immediate use without null pointer exceptions.

     */
    public RuleDto() {
        ifStatements.computeIfAbsent(0L, k -> new TreeMap<>());
        thenStatements.computeIfAbsent(0L, k -> new TreeMap<>());
        elseStatements.computeIfAbsent(0L, k -> new TreeMap<>());
    }

    /**
     * Enum representing the component keys for rule statement structure.
     * <p>
     * Each statement in a rule (if, then, or else) is composed of multiple components
     * identified by these keys:
     * <ul>
     *   <li><b>LogicalOperator</b> - Logical operator connecting conditions (AND/OR)</li>
     *   <li><b>Field</b> - Field name or identifier for the condition</li>
     *   <li><b>Operator</b> - Comparison operator (equals, greater than, less than, etc.)</li>
     *   <li><b>Value</b> - Comparison value(s) used in the condition or action</li>
     * </ul>

     */
    public enum  StatementKey {
        LogicalOperator, Field, Operator, Value
    }

    /**
     * Ordered map of if-condition statements.
     * <p>
     * Outer map key: Long sequence number for ordering statements.
     * Inner map: Statement component map keyed by {@link StatementKey}.

     * <p>
     * Default inner map at key 0L is created by the constructor.

     */
    public Map<Long, Map<StatementKey, Object>> ifStatements = new TreeMap<>();
    
    /**
     * Ordered map of then-action statements.
     * <p>
     * These statements are executed when the if-conditions evaluate to true.
     * Outer map key: Long sequence number for ordering statements.
     * Inner map: Statement component map keyed by {@link StatementKey}.

     * <p>
     * Default inner map at key 0L is created by the constructor.

     */
    public Map<Long, Map<StatementKey, Object>> thenStatements = new TreeMap<>();
    
    /**
     * Ordered map of else-action statements.
     * <p>
     * These statements are executed when the if-conditions evaluate to false.
     * Outer map key: Long sequence number for ordering statements.
     * Inner map: Statement component map keyed by {@link StatementKey}.

     * <p>
     * Default inner map at key 0L is created by the constructor.

     */
    public Map<Long, Map<StatementKey, Object>> elseStatements = new TreeMap<>();

    /**
     * Gets the ordered map of if-condition statements.
     *
     * @return map of if-condition statements keyed by sequence number
     */
    public Map<Long, Map<StatementKey, Object>> getIfStatements() {
        return ifStatements;
    }

    /**
     * Sets the ordered map of if-condition statements.
     *
     * @param ifStatements map of if-condition statements to set
     */
    public void setIfStatements(Map<Long, Map<StatementKey, Object>> ifStatements) {
        this.ifStatements = ifStatements;
    }

    /**
     * Gets the ordered map of then-action statements.
     *
     * @return map of then-action statements keyed by sequence number
     */
    public Map<Long, Map<StatementKey, Object>> getThenStatements() {
        return thenStatements;
    }

    /**
     * Sets the ordered map of then-action statements.
     *
     * @param thenStatements map of then-action statements to set
     */
    public void setThenStatements(Map<Long, Map<StatementKey, Object>> thenStatements) {
        this.thenStatements = thenStatements;
    }

    /**
     * Gets the ordered map of else-action statements.
     *
     * @return map of else-action statements keyed by sequence number
     */
    public Map<Long, Map<StatementKey, Object>> getElseStatements() {
        return elseStatements;
    }

    /**
     * Sets the ordered map of else-action statements.
     *
     * @param elseStatements map of else-action statements to set
     */
    public void setElseStatements(Map<Long, Map<StatementKey, Object>> elseStatements) {
        this.elseStatements = elseStatements;
    }

    /**
     * Aggregates all value strings from all statement maps.
     * <p>
     * This utility method extracts and combines all values from if, then, and else
     * statement maps. Comma-separated values are split into individual strings.

     * <p>
     * Note: This method contains logic code which violates Rule 5.5 design principle
     * that DTOs should not contain business logic. Consider refactoring to a separate
     * service class in future versions.

     *
     * @return set of unique value strings extracted from all statements
     */
    //TODO Rule 5.5: DTO should not have code
    public Set<String> allValues() {
        Set<String> allValues = new HashSet<>();
        allValues.addAll(splitValues(ifStatements));
        allValues.addAll(splitValues(thenStatements));
        allValues.addAll(splitValues(elseStatements));
        return allValues;
    }
    
    /**
     * Extracts and splits comma-separated values from a statement map.
     * <p>
     * This helper method processes a statement map to extract all Value components,
     * splits comma-separated value strings, and returns them as individual strings.
     * Null values are filtered out during processing.

     * <p>
     * Note: This method contains logic code which violates Rule 5.5 design principle
     * that DTOs should not contain business logic. Consider refactoring to a separate
     * service class in future versions.

     *
     * @param statements the statement map to extract values from
     * @return set of individual value strings (comma-separated values are split)
     */
    //TODO Rule 5.5: DTO should not have code
    private Set<String> splitValues(Map<Long, Map<StatementKey, Object>> statements) {
        Set<String> values = new HashSet();
        Set<List<String>> collect = statements.values().stream().map(statementKeyObjectMap -> statementKeyObjectMap.get(StatementKey.Value))
                .filter(Objects::nonNull).map(o -> o.toString().split(",")).map(Arrays::asList).collect(Collectors.toSet());
        collect.forEach(strings -> values.addAll(strings));
        return values;
    }

}
