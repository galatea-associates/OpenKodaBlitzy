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

package com.openkoda.core.customisation;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.type.StandardBasicTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hibernate FunctionContributor implementation that registers custom PostgreSQL functions
 * for use in HQL and JPQL queries within the OpenKoda framework.
 * <p>
 * This contributor enables server-side SQL operations that are not available in standard
 * JPQL, including PostgreSQL array operations and aggregation functions. The registered
 * functions can be called directly from HQL queries and are translated to native
 * PostgreSQL SQL during query execution.
 * </p>
 * <p>
 * Three custom functions are registered:
 * </p>
 * <ul>
 *   <li><b>string_agg</b> - PostgreSQL string aggregation function</li>
 *   <li><b>arrays_overlap</b> - Checks if two PostgreSQL arrays have common elements</li>
 *   <li><b>arrays_suffix</b> - Appends a suffix to each element in a PostgreSQL array</li>
 * </ul>
 * <p>
 * <b>Thread Safety:</b> This class is instantiated by Hibernate during bootstrap and
 * should be treated as a singleton. The inner function descriptor classes are stateless
 * and thread-safe for concurrent query execution.
 * </p>
 * <p>
 * <b>PostgreSQL Dependency:</b> All functions assume PostgreSQL database backend and
 * use PostgreSQL-specific syntax including array operators (&&) and type casting
 * (::varchar[], ::bigint[]).
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.hibernate.boot.model.FunctionContributor
 * @see org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor
 */
public class CustomFunctionContributor implements FunctionContributor {

    /**
     * Registers custom PostgreSQL functions with Hibernate's function registry.
     * <p>
     * This method is called during Hibernate SessionFactory initialization to register
     * three custom functions that can be used in HQL queries:
     * </p>
     * <ol>
     *   <li><b>string_agg</b> - PostgreSQL's built-in string aggregation function that
     *       concatenates values with a delimiter. Returns a STRING type.</li>
     *   <li><b>arrays_suffix</b> - Custom function that appends a suffix to each element
     *       in a PostgreSQL array field.</li>
     *   <li><b>arrays_overlap</b> - Custom function that checks if two PostgreSQL arrays
     *       share any common elements using the && operator.</li>
     * </ol>
     *
     * @param functionContributions the Hibernate function contributions registry where
     *                              custom functions are registered for query translation
     */
    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().register("string_agg", new StandardSQLFunction("string_agg", StandardBasicTypes.STRING));
        functionContributions.getFunctionRegistry().register("arrays_suffix", new ArraysSuffixFunction());
        functionContributions.getFunctionRegistry().register("arrays_overlap", new StringArraysOverlapFunction());
    }

    /**
     * Custom Hibernate function descriptor that implements PostgreSQL array overlap checking
     * using the && operator to determine if two arrays share any common elements.
     * <p>
     * This function enables server-side security checks by comparing arrays of organization IDs
     * or privilege tokens. For example, it can verify if a user's organizations (represented as
     * an array) overlap with the organizations related to a specific entity, allowing efficient
     * privilege-based filtering directly in SQL queries.
     * </p>
     * <p>
     * The function accepts two parameters:
     * </p>
     * <ul>
     *   <li><b>arg1</b> - A database column containing a PostgreSQL array (typically bigint[] or varchar[])</li>
     *   <li><b>arg2</b> - A Java collection (ArrayList&lt;String&gt; or HashSet&lt;Long&gt;) from HQL query parameters</li>
     * </ul>
     * <p>
     * The implementation converts the Java collection into a PostgreSQL array literal and uses
     * the PostgreSQL && (overlap) operator for comparison. Returns true if arrays have at least
     * one element in common.
     * </p>
     * <p>
     * <b>HQL Usage Example:</b>
     * </p>
     * <pre>
     * SELECT e FROM Entity e WHERE arrays_overlap(e.organizationIds, :userOrgIds) = true
     * </pre>
     * <p>
     * <b>Generated SQL Pattern:</b>
     * </p>
     * <pre>
     * (ARRAY['org1','org2']::varchar[] && entity.organization_ids)
     * </pre>
     * <p>
     * <b>Security Use Case:</b> This function is critical for multi-tenant security,
     * allowing row-level access control based on organization membership without
     * requiring expensive application-side filtering.
     * </p>
     *
     * @see org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor
     */
    public class StringArraysOverlapFunction extends AbstractSqmSelfRenderingFunctionDescriptor{

        /**
         * Constructs the arrays_overlap function descriptor with validation for exactly two arguments.
         * <p>
         * Initializes the function with the name "arrays_overlap" and configures Hibernate's
         * argument validator to ensure exactly two parameters are provided in HQL queries.
         * </p>
         */
        public StringArraysOverlapFunction() {
            super("arrays_overlap", StandardArgumentsValidators.exactly(2), null, null);
        }

        /**
         * Renders the arrays_overlap function as native PostgreSQL SQL using the && operator.
         * <p>
         * This method translates the HQL function call into PostgreSQL-specific SQL syntax:
         * </p>
         * <pre>
         * (ARRAY[values]::type[] && column_name)
         * </pre>
         * <p>
         * The method handles two types of Java collections for the second argument:
         * </p>
         * <ul>
         *   <li>ArrayList&lt;String&gt; - Converted to varchar[] array with single-quoted values</li>
         *   <li>HashSet&lt;Long&gt; - Converted to bigint[] array with numeric values</li>
         * </ul>
         *
         * @param sqlAppender the SQL string builder where generated SQL fragments are appended
         * @param sqlAstArguments the list of SQL AST nodes representing function arguments
         *                        (arg1: database column, arg2: parameter value)
         * @param walker the SQL AST translator for rendering expressions into SQL
         * @throws IllegalArgumentException if arg2 is not an ArrayList or HashSet
         */
        @Override
        public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, SqlAstTranslator<?> walker) {
            final Expression arg1 = (Expression) sqlAstArguments.get(0);
            final QueryLiteral arg2 = (QueryLiteral) sqlAstArguments.get(1);

            sqlAppender.appendSql("(ARRAY[");
            if(arg2.getLiteralValue() instanceof ArrayList) {
                appendStringArrayList(sqlAppender, arg2);
            }
            else if(arg2.getLiteralValue() instanceof HashSet){
                appendLongHashSet(sqlAppender, arg2);
            } else {
                throw new IllegalArgumentException("The function cannot handle arg2 with type " + arg2.getLiteralValue().getClass());
            }
            walker.render(arg1, SqlAstNodeRenderingMode.DEFAULT);
            sqlAppender.appendSql( ")");

        }
        /**
         * Appends a PostgreSQL varchar array literal from an ArrayList of strings to the SQL.
         * <p>
         * Converts the ArrayList into PostgreSQL array syntax with type casting:
         * {@code ['value1','value2']::varchar[] &&}
         * </p>
         *
         * @param sqlAppender the SQL string builder for appending generated SQL
         * @param arg2 the query literal containing an ArrayList&lt;String&gt; to be converted
         */
        private void appendStringArrayList(SqlAppender sqlAppender, QueryLiteral arg2){
            sqlAppender.appendSql(getAsCommaSeparatedAndSingleQuotedString((ArrayList<String>) arg2.getLiteralValue()));
            sqlAppender.appendSql("]");
            sqlAppender.appendSql("::varchar[] && ");
        }

        /**
         * Appends a PostgreSQL bigint array literal from a HashSet of longs to the SQL.
         * <p>
         * Converts the HashSet into PostgreSQL array syntax with type casting:
         * {@code [1,2,3]::bigint[] &&}
         * </p>
         *
         * @param sqlAppender the SQL string builder for appending generated SQL
         * @param arg2 the query literal containing a HashSet&lt;Long&gt; to be converted
         */
        private void appendLongHashSet(SqlAppender sqlAppender, QueryLiteral arg2){
            sqlAppender.appendSql(getAsCommaSeparatedString((HashSet<Long>) arg2.getLiteralValue()));
            sqlAppender.appendSql("]");
            sqlAppender.appendSql("::bigint[] && ");
        }

        /**
         * Converts a HashSet of Long values into a comma-separated string for PostgreSQL array syntax.
         * <p>
         * Example: {1L, 2L, 3L} becomes "1,2,3"
         * </p>
         *
         * @param values the HashSet of Long values to convert
         * @return comma-separated string representation of the numeric values
         */
        private String getAsCommaSeparatedString(HashSet<Long> values){
            return String.join(",", values.stream().map(value -> String.valueOf(value)).collect(Collectors.toList()));
        }

        /**
         * Converts an ArrayList of strings into a comma-separated, single-quoted string
         * for PostgreSQL varchar array syntax.
         * <p>
         * Example: ["org1", "org2"] becomes "'org1','org2'"
         * </p>
         *
         * @param values the ArrayList of String values to convert
         * @return comma-separated string with each value wrapped in single quotes
         */
        private String getAsCommaSeparatedAndSingleQuotedString(ArrayList<String> values){
            return String.join(",", values.stream()
                    .map(value -> ("'" + value + "'"))
                    .collect(Collectors.toList()));
        }
    }

    /**
     * Custom Hibernate function descriptor that appends a suffix to each element in a
     * PostgreSQL array, returning a new array with transformed values.
     * <p>
     * This function is useful for transforming array values in queries, such as adding
     * prefixes or suffixes to privilege tokens, organization identifiers, or other
     * array-stored data. The operation is performed entirely on the database server
     * using PostgreSQL's array manipulation capabilities.
     * </p>
     * <p>
     * The function accepts two parameters:
     * </p>
     * <ul>
     *   <li><b>arg1</b> - A PostgreSQL array column or expression (varchar[] or text[])</li>
     *   <li><b>arg2</b> - A string value to append as suffix to each array element</li>
     * </ul>
     * <p>
     * <b>HQL Usage Example:</b>
     * </p>
     * <pre>
     * SELECT arrays_suffix(e.tags, '_archived') FROM Entity e
     * </pre>
     * <p>
     * <b>SQL Pattern Explanation:</b>
     * </p>
     * <p>
     * The function generates SQL that:
     * </p>
     * <ol>
     *   <li><b>Unnests</b> the array into individual rows: {@code unnest(array_column)}</li>
     *   <li><b>Concatenates</b> suffix to each element: {@code unnest(array) || suffix}</li>
     *   <li><b>Re-aggregates</b> results back into array: {@code array(select ...)}</li>
     *   <li><b>Type casts</b> to varchar for consistency: {@code ::varchar}</li>
     * </ol>
     * <p>
     * <b>Generated SQL Example:</b>
     * </p>
     * <pre>
     * (array(select (unnest(tags) || '_archived')::varchar))
     * </pre>
     * <p>
     * This approach leverages PostgreSQL's set-returning functions and array constructors
     * for efficient array transformation without requiring application-side processing.
     * </p>
     *
     * @see org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor
     */
    public class ArraysSuffixFunction extends AbstractSqmSelfRenderingFunctionDescriptor{

        /**
         * Constructs the arrays_suffix function descriptor with validation for exactly two arguments.
         * <p>
         * Initializes the function with the name "arrays_suffix" and configures Hibernate's
         * argument validator to ensure exactly two parameters (array column and suffix string)
         * are provided in HQL queries.
         * </p>
         */
        public ArraysSuffixFunction(){
            super("arrays_suffix", StandardArgumentsValidators.exactly(2), null, null);
        }

        /**
         * Renders the arrays_suffix function as native PostgreSQL SQL using array manipulation.
         * <p>
         * This method translates the HQL function call into a PostgreSQL SQL pattern that:
         * </p>
         * <ol>
         *   <li>Uses {@code unnest()} to expand the array into rows</li>
         *   <li>Applies the concatenation operator (||) to append the suffix</li>
         *   <li>Uses {@code array(select ...)} to reconstruct the array from rows</li>
         *   <li>Casts result to varchar for type consistency</li>
         * </ol>
         * <p>
         * <b>Generated SQL Pattern:</b>
         * </p>
         * <pre>
         * (array(select (unnest(array_column) || suffix_value)::varchar))
         * </pre>
         * <p>
         * Both arguments are rendered using the Hibernate SQL AST walker to properly
         * handle column references, bind parameters, and nested expressions.
         * </p>
         *
         * @param sqlAppender the SQL string builder where generated SQL fragments are appended
         * @param sqlAstArguments the list of SQL AST nodes representing function arguments
         *                        (arg1: array column/expression, arg2: suffix string)
         * @param walker the SQL AST translator for rendering expressions into SQL
         */
        @Override
        public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, SqlAstTranslator<?> walker) {
            final Expression arg1 = (Expression) sqlAstArguments.get(0);
            final Expression arg2 = (Expression) sqlAstArguments.get(1);

            sqlAppender.appendSql("(array(select (unnest(");
            walker.render(arg1, SqlAstNodeRenderingMode.DEFAULT);
            sqlAppender.appendSql(") || ");
            walker.render(arg2, SqlAstNodeRenderingMode.DEFAULT);
            sqlAppender.appendSql(")::varchar))");

        }
    }
}
