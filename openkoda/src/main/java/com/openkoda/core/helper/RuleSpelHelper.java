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

import com.openkoda.dto.RuleDto;
import com.openkoda.form.rule.LogicalOperator;
import com.openkoda.form.rule.Operator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.InlineList;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides Spring Expression Language (SpEL) utilities for parsing and serializing ternary rule expressions.
 * <p>
 * This helper class enables conversion between string-based SpEL expressions and structured rule data transfer objects.
 * It validates literal tokens with a whitelist regex pattern to prevent code injection, traverses the SpEL Abstract
 * Syntax Tree (AST) to produce structural maps, and builds JPA Criteria API Predicate objects from expressions.
 * </p>
 * <p>
 * Key capabilities:
 * <ul>
 *   <li>Parse SpEL expressions into structured RuleDto objects</li>
 *   <li>Serialize RuleDto objects back to SpEL expression strings</li>
 *   <li>Validate SpEL rule syntax and literal value safety</li>
 *   <li>Generate JPA Criteria Predicates from SpEL rules</li>
 *   <li>Convert rules to SQL CASE WHEN statements</li>
 * </ul>
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * boolean valid = RuleSpelHelper.isRuleValid("field == 'value'");
 * RuleDto dto = RuleSpelHelper.parseToRuleDto("status == 'active' ? 1 : 0");
 * }</pre>
 * </p>
 * <p>
 * <b>Important warnings:</b>
 * <ul>
 *   <li>Runtime exceptions propagate for invalid AST structures</li>
 *   <li>SpEL evaluation can be slow - cache parsed results when possible</li>
 *   <li>Whitelist regex (VALUE_REGEX) prevents code injection in literal values</li>
 *   <li>SpEL parsing is expensive - avoid in performance-critical loops</li>
 * </ul>
 * </p>
 * <p>
 * Thread-safety: All methods are static and stateless, safe for concurrent use.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.springframework.expression.spel.standard.SpelExpressionParser
 * @see jakarta.persistence.criteria.Predicate
 * @see com.openkoda.dto.RuleDto
 */
@Component
public class RuleSpelHelper {

    /** Ternary operator question mark separator in SpEL expressions. */
    public static final String QUESTION_MARK = " ? ";
    
    /** Ternary operator colon separator in SpEL expressions. */
    public static final String COLON = " : ";
    
    /** Single quote character for string literal values. */
    public static final String VALUE_APOSTROPHE = "'";
    
    /** Whitelist regex pattern allowing alphanumeric characters, spaces, commas, periods, underscores, and hyphens. */
    public static final String VALUE_REGEX = "^([a-zA-Z0-9,. _-]+)$";
    
    /** Default value for empty else clause in ternary expressions. */
    public static final String EMPTY_ELSE = "null";

    /**
     * Converts a RuleDto object to its SpEL expression string representation.
     * <p>
     * Traverses the structured rule data transfer object and produces a ternary SpEL expression
     * in the format: {@code condition ? thenValue : elseValue}. Validates that both if-condition
     * and then-statement operators are present before generating the expression.
     * </p>
     *
     * @param ruleDto the rule data transfer object containing if/then/else statement components
     * @return the SpEL expression string, or empty string if required operators are missing
     * @see #parseToRuleDto(String)
     */
    public static String parseToString(RuleDto ruleDto) {
        if(ruleDto.getIfStatements().get(0L).get(RuleDto.StatementKey.Operator) != null
                && ruleDto.getThenStatements().get(0L).get(RuleDto.StatementKey.Operator) != null) {
            String ruleIf = concatRulePart(ruleDto.getIfStatements());
            String ruleThen = concatRulePart(ruleDto.getThenStatements());
            String ruleElse = EMPTY_ELSE;
            if(ruleDto.getElseStatements().get(0L).get(RuleDto.StatementKey.Operator) != null
                && StringUtils.isNotEmpty(ruleDto.getElseStatements().get(0L).get(RuleDto.StatementKey.Operator).toString())) {
                ruleElse = concatRulePart(ruleDto.getElseStatements());
            }
            return ruleIf + QUESTION_MARK + ruleThen + COLON + ruleElse;
        }
        return "";
    }

    /**
     * Parses a SpEL expression string into a structured RuleDto data transfer object.
     * <p>
     * Converts the string-based SpEL rule into separate if/then/else statement components by traversing
     * the Abstract Syntax Tree (AST). Each statement is decomposed into fields, operators, and values
     * stored in a map structure for programmatic access and manipulation.
     * </p>
     * <p>
     * The method expects ternary expressions with the pattern: {@code condition ? thenValue : elseValue}.
     * The else clause is optional.
     * </p>
     *
     * @param rule the SpEL expression string to parse (e.g., "status == 'active' ? 1 : 0")
     * @return structured RuleDto with separated if/then/else statement maps, or empty RuleDto if rule is null/empty
     * @throws org.springframework.expression.ParseException if the SpEL syntax is invalid
     * @see #parseToString(RuleDto)
     * @see com.openkoda.dto.RuleDto
     */
    public static RuleDto parseToRuleDto(String rule) {
        RuleDto ruleDto = new RuleDto();
        if(StringUtils.isEmpty(rule)) {
            return ruleDto;
        }

        SpelExpressionParser parser = new SpelExpressionParser();
        SpelExpression exp = parser.parseRaw(rule);

        ruleDto.setIfStatements(parse(0, new TreeMap<>(), null, "", Collections.emptyList(), null, exp.getAST().getChild(0)).getT1());
        ruleDto.setThenStatements(parse(0, new TreeMap<>(), null, "", Collections.emptyList(), null, exp.getAST().getChild(1)).getT1());
        if(exp.getAST().getChild(2) != null) {
            ruleDto.setElseStatements(parse(0, new TreeMap<>(), null, "", Collections.emptyList(), null, exp.getAST().getChild(2)).getT1());
        }

        return ruleDto;
    }

    /**
     * Validates whether a string is a syntactically correct SpEL rule expression.
     * <p>
     * Attempts to parse the rule using Spring's SpEL parser. Returns true if parsing succeeds,
     * indicating valid SpEL syntax. Returns false if parsing throws a ParseException, indicating
     * invalid syntax.
     * </p>
     * <p>
     * Note: This method only validates SpEL syntax, not semantic correctness or whitelist compliance.
     * For security validation of literal values, see {@link #validateRuleValue(String)}.
     * </p>
     *
     * @param rule the SpEL expression string to validate
     * @return true if the rule has valid SpEL syntax, false otherwise
     * @see org.springframework.expression.spel.standard.SpelExpressionParser#parseExpression(String)
     */
    public static boolean isRuleValid(String rule) {
        SpelExpressionParser parser = new SpelExpressionParser();
        try {
            parser.parseExpression(rule);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Converts a SpEL ternary rule expression into an SQL CASE WHEN statement.
     * <p>
     * Parses the SpEL rule and generates a corresponding SQL SELECT statement using CASE WHEN syntax.
     * This enables direct database-level evaluation of rules that were originally defined in SpEL format.
     * </p>
     * <p>
     * The generated SQL follows the pattern:
     * {@code SELECT CASE WHEN condition THEN value1 ELSE value2 FROM tableName}
     * </p>
     *
     * @param rule the SpEL ternary expression (e.g., "status == 'active' ? 1 : 0")
     * @param tableName the database table name to use in the FROM clause
     * @return SQL SELECT statement with CASE WHEN logic, or empty string if rule is not a ternary expression
     * @see org.springframework.expression.spel.SpelNode#toStringAST()
     */
    public static String getSelect(String rule, String tableName) {
        SpelExpressionParser parser = new SpelExpressionParser();
        SpelExpression exp = parser.parseRaw(rule);
        SpelNode ast = exp.getAST();
        return ast.getChildCount() == 3 ? "SELECT CASE WHEN " + ast.getChild(0).toStringAST()
                + " THEN " + ast.getChild(1).toStringAST()
                + " ELSE " + ast.getChild(2).toStringAST() +
                " FROM " + tableName : "";
    }

    /**
     * Recursively parses a SpEL Abstract Syntax Tree (AST) node into rule components and JPA Criteria Predicate.
     * <p>
     * This is the core parsing method that traverses the SpEL AST recursively, extracting rule components
     * (fields, operators, values) and optionally building JPA Criteria API predicates for database queries.
     * The method handles different AST node types including method references, operators, property references,
     * and inline lists.
     * </p>
     * <p>
     * The method supports logical operators (AND, OR) and comparison operators (equals, notEquals, greaterThan,
     * lessThan, contains, in) by delegating to specialized handler methods.
     * </p>
     *
     * @param <R> the entity type for JPA Criteria Root
     * @param index the current statement index in the rule parts map
     * @param ruleParts the accumulated map of statement components (modified in place)
     * @param root the JPA Criteria Root for building predicates (null if not generating predicates)
     * @param fieldName the current field name being processed
     * @param values the list of values for IN operator clauses
     * @param entityManager the EntityManager for creating Criteria Builder (null if not generating predicates)
     * @param ast the SpEL AST node to parse recursively
     * @return Tuple2 containing the updated rule parts map and the generated JPA Predicate (null if entityManager is null)
     * @see #handleMethodReference(long, Map, Root, String, List, EntityManager, SpelNode)
     * @see #handleLogicalOperator(long, Map, Root, String, List, EntityManager, org.springframework.expression.spel.ast.Operator)
     * @see #handleOperands(long, Map, Root, EntityManager, org.springframework.expression.spel.ast.Operator)
     */
    public static<R> Tuple2<Map<Long, Map<RuleDto.StatementKey, Object>>, Predicate> parse(long index, Map<Long, Map<RuleDto.StatementKey, Object>> ruleParts, Root<R> root, String fieldName, List<String> values, EntityManager entityManager, SpelNode ast) {
        ruleParts.computeIfAbsent(index, k -> new TreeMap<>());

        if(ast instanceof MethodReference) {
            return handleMethodReference(index, ruleParts, root, fieldName, values, entityManager, ast);
        }

        if(ast instanceof org.springframework.expression.spel.ast.Operator) {
            org.springframework.expression.spel.ast.Operator operator = (org.springframework.expression.spel.ast.Operator) ast;
            if(EnumUtils.isValidEnum(LogicalOperator.class, operator.getOperatorName())) {
                return handleLogicalOperator(index, ruleParts, root, fieldName, values, entityManager, operator);
            } else {
                ruleParts.get(index).put(RuleDto.StatementKey.Operator, Operator.fromString(operator.getOperatorName()));
            }
            if (operator.getLeftOperand().getChildCount() == 0 && operator.getRightOperand().getChildCount() == 0) {
                return handleOperands(index, ruleParts, root, entityManager, operator);
            }
        }

        for (int i = 0; i < ast.getChildCount(); i++) {
            if(ast.getChild(i) instanceof PropertyOrFieldReference) {
//                handle property field reference
                PropertyOrFieldReference propertyOrFieldReference = (PropertyOrFieldReference)ast.getChild(i);
                fieldName = propertyOrFieldReference.getName();
                if(ast.getChild(i).getChildCount() == 0) {
                    return parse(index , ruleParts, root, fieldName, values, entityManager, ast.getChild(i + 1));
                }
            }
            if(ast.getChild(i) instanceof InlineList) {
//                handle inline list
                InlineList inlineList = (InlineList)ast.getChild(i);
                values = inlineList.getConstantValue()
                        .stream().map(o -> o.toString().replaceAll(VALUE_APOSTROPHE, ""))
                        .collect(Collectors.toList());
                return parse(index , ruleParts, root, fieldName, values, entityManager, ast.getChild(i + 1));
            }
            return parse(index, ruleParts, root, fieldName, values, entityManager, ast.getChild(i));
        }

        return Tuples.of(ruleParts, entityManager != null ? entityManager.getCriteriaBuilder().disjunction() : null);
    }

    /**
     * Handles comparison operators (equals, notEquals, greaterThan, lessThan) in the SpEL AST.
     * <p>
     * Extracts the field name from the left operand and the comparison value from the right operand.
     * If an EntityManager is provided, generates the corresponding JPA Criteria API predicate for
     * database query construction.
     * </p>
     *
     * @param <R> the entity type for JPA Criteria Root
     * @param index the current statement index
     * @param ruleParts the rule components map to populate
     * @param root the JPA Criteria Root for building predicates
     * @param entityManager the EntityManager for creating predicates (null to skip predicate generation)
     * @param operator the comparison operator AST node
     * @return Tuple2 with updated rule parts and generated JPA Predicate (null if entityManager is null)
     */
    private static <R> Tuple2<Map<Long, Map<RuleDto.StatementKey, Object>>, Predicate> handleOperands(long index, Map<Long, Map<RuleDto.StatementKey, Object>> ruleParts, Root<R> root, EntityManager entityManager, org.springframework.expression.spel.ast.Operator operator) {
        ruleParts.get(index).put(RuleDto.StatementKey.Field, operator.getLeftOperand().toStringAST());
        ruleParts.get(index).put(RuleDto.StatementKey.Value, operator.getRightOperand().toStringAST()
                .replaceAll(VALUE_APOSTROPHE, ""));
        if (entityManager == null) {
            return Tuples.of(ruleParts, null);
        }
        if (Operator.fromString(operator.getOperatorName()).equals(Operator.equals)) {
            return Tuples.of(
                    ruleParts,
                    entityManager.getCriteriaBuilder()
                            .equal(
                                    root.get(operator.getLeftOperand().toStringAST()),
                                    operator.getRightOperand().toStringAST().replaceAll(VALUE_APOSTROPHE, ""))
            );
        } else if (Operator.fromString(operator.getOperatorName()).equals(Operator.notEquals)) {
            return Tuples.of(
                    ruleParts,
                    entityManager.getCriteriaBuilder()
                            .notEqual(
                                    root.get(operator.getLeftOperand().toStringAST()),
                                    operator.getRightOperand().toStringAST().replaceAll(VALUE_APOSTROPHE, ""))
            );
        } else if (Operator.fromString(operator.getOperatorName()).equals(Operator.greaterThan)) {
            return Tuples.of(
                    ruleParts,
                    entityManager.getCriteriaBuilder()
                            .greaterThan(
                                    root.get(operator.getLeftOperand().toStringAST()),
                                    operator.getRightOperand().toStringAST().replaceAll(VALUE_APOSTROPHE, ""))
            );
        } else if (Operator.fromString(operator.getOperatorName()).equals(Operator.lessThan)) {
            return Tuples.of(
                    ruleParts,
                    entityManager.getCriteriaBuilder()
                            .lessThan(
                                    root.get(operator.getLeftOperand().toStringAST()),
                                    operator.getRightOperand().toStringAST().replaceAll(VALUE_APOSTROPHE, ""))
            );
        }
        return null;
    }

    /**
     * Handles logical operators (AND, OR) in the SpEL AST by recursively parsing left and right operands.
     * <p>
     * Creates a new statement index for the right operand and recursively parses both sides of the
     * logical operator. If an EntityManager is provided, combines the resulting predicates using
     * JPA Criteria Builder's and() or or() methods.
     * </p>
     *
     * @param <R> the entity type for JPA Criteria Root
     * @param index the current statement index for the left operand
     * @param ruleParts the rule components map to populate
     * @param root the JPA Criteria Root for building predicates
     * @param fieldName the current field name being processed
     * @param values the list of values for IN operator clauses
     * @param entityManager the EntityManager for creating predicates (null to skip predicate generation)
     * @param operator the logical operator (AND or OR) AST node
     * @return Tuple2 with updated rule parts and combined JPA Predicate (null if entityManager is null)
     */
    private static <R> Tuple2<Map<Long, Map<RuleDto.StatementKey, Object>>, Predicate> handleLogicalOperator(long index, Map<Long, Map<RuleDto.StatementKey, Object>> ruleParts, Root<R> root, String fieldName, List<String> values, EntityManager entityManager, org.springframework.expression.spel.ast.Operator operator) {
        long nextIndex = index + 1;
        ruleParts.computeIfAbsent(nextIndex, k -> new TreeMap<>());
        ruleParts.get(nextIndex).put(RuleDto.StatementKey.LogicalOperator, LogicalOperator.valueOf(operator.getOperatorName()));
        if(entityManager == null) {
            parse(index, ruleParts, root, fieldName, values, null, operator.getLeftOperand());
            parse(nextIndex, ruleParts, root, fieldName, values, null, operator.getRightOperand());
            return Tuples.of(ruleParts, null);
        }
        if (LogicalOperator.valueOf(operator.getOperatorName()).equals(LogicalOperator.and)) {
            return Tuples.of(
                    ruleParts,
                    entityManager.getCriteriaBuilder()
                            .and(
                                    parse(index, ruleParts, root, fieldName, values, entityManager, operator.getLeftOperand()).getT2(),
                                    parse(nextIndex, ruleParts, root, fieldName, values, entityManager, operator.getRightOperand()).getT2())
            );
        } else if (LogicalOperator.valueOf(operator.getOperatorName()).equals(LogicalOperator.or)) {
            return Tuples.of(
                    ruleParts,
                            entityManager.getCriteriaBuilder()
                                    .or(
                                            parse(index, ruleParts, root, fieldName, values, entityManager, operator.getLeftOperand()).getT2(),
                                            parse(nextIndex, ruleParts, root, fieldName, values, entityManager, operator.getRightOperand()).getT2())
            );
        }
        return null;
    }

    /**
     * Handles method references in the SpEL AST, specifically for contains and IN operators.
     * <p>
     * Processes method calls like {@code contains()} which map to SQL LIKE or IN operations depending
     * on the context. When the contains method is called on an inline list, it generates an IN predicate.
     * When called with a string argument, it generates a LIKE predicate with wildcards.
     * </p>
     *
     * @param <R> the entity type for JPA Criteria Root
     * @param index the current statement index
     * @param ruleParts the rule components map to populate
     * @param root the JPA Criteria Root for building predicates
     * @param fieldName the current field name being processed
     * @param values the list of values for IN operator clauses
     * @param entityManager the EntityManager for creating predicates (null to skip predicate generation)
     * @param ast the method reference AST node
     * @return Tuple2 with updated rule parts and generated JPA Predicate (null if not applicable)
     */
    private static <R> Tuple2<Map<Long, Map<RuleDto.StatementKey, Object>>, Predicate> handleMethodReference(long index, Map<Long, Map<RuleDto.StatementKey, Object>> ruleParts, Root<R> root, String fieldName, List<String> values, EntityManager entityManager, SpelNode ast) {
        MethodReference method = (MethodReference) ast;
        if(EnumUtils.isValidEnum(Operator.class, method.getName())) {
            if(method.getName().equals(Operator.contains.name()) && ast.getChild(0) instanceof PropertyOrFieldReference) {
//                    custom IN operator here
                ruleParts.get(index).put(RuleDto.StatementKey.Operator, Operator.in);
                ruleParts.get(index).put(RuleDto.StatementKey.Field, ast.getChild(0).toStringAST());
                ruleParts.get(index).put(RuleDto.StatementKey.Value, String.join(",", values));
                return Tuples.of(
                        ruleParts,
                        root != null ?
                                root.get(ast.getChild(0).toStringAST()).in(values)
                                : null
                );
            } else {
//                    contains operator => like
                ruleParts.get(index).put(RuleDto.StatementKey.Field, fieldName);
                ruleParts.get(index).put(RuleDto.StatementKey.Operator, Operator.valueOf(method.getName()));
                ruleParts.get(index).put(RuleDto.StatementKey.Value, ast.getChild(0).toStringAST().replaceAll(VALUE_APOSTROPHE, ""));
                return Tuples.of(
                        ruleParts,
                        entityManager != null ?
                                entityManager.getCriteriaBuilder()
                                        .like(
                                                root.get(fieldName),
                                                "%" + ast.getChild(0).toStringAST().replaceAll(VALUE_APOSTROPHE, "") + "%")
                                : null);
            }
        }
        return null;
    }

    /**
     * Concatenates structured statement components into a SpEL expression string.
     * <p>
     * Iterates through the statement map, building a SpEL expression by combining fields, operators,
     * and values. Handles special formatting for IN operator (inline list syntax) and validates all
     * literal values against the whitelist regex to prevent code injection.
     * </p>
     * <p>
     * The method enforces security by calling {@link #validateRuleValue(String)} on all literal values.
     * </p>
     *
     * @param statements the map of statement components indexed by position
     * @return concatenated SpEL expression string
     * @throws RuntimeException if any value fails whitelist validation
     * @see #validateRuleValue(String)
     */
    private static String concatRulePart(Map<Long, Map<RuleDto.StatementKey, Object>> statements) {
        StringBuilder ruleSB = new StringBuilder();
        for (Map.Entry<Long, Map<RuleDto.StatementKey, Object>> statementIndex : statements.entrySet()) {
            ruleSB.append(statementIndex.getValue().get(RuleDto.StatementKey.LogicalOperator) != null ?
                    LogicalOperator.valueOf(statementIndex.getValue().get(RuleDto.StatementKey.LogicalOperator).toString()).getSpel() : "");
            if(Operator.valueOf(statementIndex.getValue().get(RuleDto.StatementKey.Operator).toString()).equals(Operator.in)) {
                List<String> valuesList = Arrays.stream(statementIndex.getValue().get(RuleDto.StatementKey.Value).toString().split(","))
                        .map(String::trim).collect(Collectors.toList());
                valuesList.forEach(RuleSpelHelper::validateRuleValue);
                String inlineValuesList = valuesList.stream().map(s -> String.format("'%s'", s))
                        .collect(Collectors.joining(","));
                ruleSB.append(String.format("{%s}", inlineValuesList));
                ruleSB.append(String.format(Operator.valueOf(statementIndex.getValue().get(RuleDto.StatementKey.Operator).toString()).getSpel(),
                        statementIndex.getValue().get(RuleDto.StatementKey.Field)));
            } else {
                ruleSB.append(statementIndex.getValue().get(RuleDto.StatementKey.Field));
                validateRuleValue(statementIndex.getValue().get(RuleDto.StatementKey.Value).toString());
                ruleSB.append(String.format(Operator.valueOf(statementIndex.getValue().get(RuleDto.StatementKey.Operator).toString()).getSpel(),
                    statementIndex.getValue().get(RuleDto.StatementKey.Value)));
            }
        }
        return ruleSB.toString();
    }

    /**
     * Validates a literal value against the whitelist regex pattern to prevent code injection.
     * <p>
     * Ensures that rule values contain only safe characters (alphanumeric, spaces, commas, periods,
     * underscores, hyphens) as defined by {@link #VALUE_REGEX}. This is a critical security measure
     * to prevent malicious code injection through user-supplied rule values.
     * </p>
     *
     * @param value the literal value to validate
     * @throws RuntimeException with message "Rule statement value invalid!" if validation fails
     * @see #VALUE_REGEX
     */
    private static void validateRuleValue(String value) {
        if(!value.matches(VALUE_REGEX)) {
            throw new RuntimeException("Rule statement value invalid!");
        }
    }

}
