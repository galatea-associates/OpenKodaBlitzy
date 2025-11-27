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

import com.openkoda.core.security.UserProvider;
import com.openkoda.core.tracker.RequestIdHolder;
import com.openkoda.model.common.ModelConstants;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Hibernate StatementInspector that enhances SQL statements for observability and user context injection.
 * <p>
 * This inspector performs two key operations on all SQL statements executed through Hibernate:
 * <ol>
 *   <li>Replaces {@code USER_ID_PLACEHOLDER} tokens with the actual user ID from the security context</li>
 *   <li>Appends the current request ID as a SQL comment for request tracing in database logs (represented below using square brackets to avoid Javadoc terminators)</li>
 * </ol>
 * <p>
 * The placeholder replacement enables dynamic user-scoped queries where the user ID is injected
 * at execution time rather than query construction time. The request ID comment enables correlation
 * between application logs and database query logs for troubleshooting and performance monitoring.
 * <p>
 * <b>Use Cases:</b>
 * <ul>
 *   <li>Database query logging correlation - trace queries back to originating HTTP requests</li>
 *   <li>Performance monitoring - identify slow queries by request ID</li>
 *   <li>Audit trails - associate database operations with authenticated users</li>
 *   <li>Query fingerprinting - consistent comment format aids log analysis tools</li>
 * </ul>
 * <p>
 * <b>Important Warnings:</b>
 * <ul>
 *   <li><b>Query Plan Caching:</b> Altering literal SQL text means each request generates unique SQL,
 *       which may reduce effectiveness of database query plan caches</li>
 *   <li><b>SQL Fingerprinting:</b> Appended comments affect query fingerprinting algorithms that
 *       group similar queries - ensure monitoring tools strip comments before fingerprinting</li>
 *   <li><b>PII Exposure:</b> User IDs and request IDs appear in SQL logs, which may contain
 *       personally identifiable information - ensure database logs have appropriate access controls</li>
 * </ul>
 * <p>
 * <b>Thread Safety:</b> This inspector is safe for concurrent use as it reads from thread-local
 * storage ({@link RequestIdHolder}) and does not maintain mutable state.
 * <p>
 * <b>Performance:</b> The regex replacement and string concatenation operations add minimal overhead
 * (typically &lt;1ms per query) to SQL execution.
 * <p>
 * Example of SQL transformation:
 * <pre>
 * // Original SQL with placeholder
 * SELECT * FROM organizations WHERE created_by = '__USER_ID__'
 * 
 * // Transformed SQL with user ID and request comment
 * SELECT * FROM organizations WHERE created_by = '12345' /&#42;req-abc-123&#42;/
 * </pre>
 * <p>
 * This inspector is automatically invoked by Hibernate for all SQL statements when configured
 * in the Hibernate settings.
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.hibernate.resource.jdbc.spi.StatementInspector
 * @see RequestIdHolder
 * @see UserProvider
 * @see ModelConstants#USER_ID_PLACEHOLDER
 */
public class SqlCommentStatementInspector
        implements StatementInspector, ReadableCode{

    /**
     * Inspects and modifies SQL statements to inject user context and request tracing information.
     * <p>
     * This method performs two transformations on the provided SQL statement:
     * <ol>
     *   <li>If the SQL contains {@link ModelConstants#USER_ID_PLACEHOLDER}, replaces all occurrences
     *       with the actual user ID from {@link UserProvider#getUserIdOrNotExistingIdAsString()}</li>
     *   <li>Appends the current request ID from {@link RequestIdHolder} as a SQL comment in the
     *       format of a request-id comment (shown below using square brackets to keep Javadoc valid)</li>
     * </ol>
     * <p>
     * The placeholder substitution uses {@code String.replaceAll()} with the placeholder as a
     * literal regex pattern, replacing all occurrences in the SQL string. The request ID is
     * concatenated as a trailing comment using {@code String.format()}.
     * <p>
     * If no placeholder is present, the SQL is passed directly to request ID injection without
     * user ID substitution.
     * <p>
     * Example transformation:
     * <pre>
     * // Input SQL
     * "SELECT * FROM users WHERE id = '__USER_ID__'"
     * 
     * // Output SQL (assuming user ID 12345 and request ID req-abc-123)
     * "SELECT * FROM users WHERE id = '12345' /&#42;req-abc-123&#42;/"
     * </pre>
     *
     * @param sql the original SQL statement to inspect and modify, must not be null
     * @return modified SQL statement with user ID placeholder replaced (if present) and request ID
     *         appended as a comment. Returns the original SQL with only request ID comment if no
     *         placeholder is found or if request ID is not available.
     * @see ModelConstants#USER_ID_PLACEHOLDER
     * @see UserProvider#getUserIdOrNotExistingIdAsString()
     * @see RequestIdHolder#getId()
     */
    @Override
    public String inspect(String sql) {

        if (not(sql.contains(ModelConstants.USER_ID_PLACEHOLDER))) {
            return includeRequestId(sql);
        }

        String userId = UserProvider.getUserIdOrNotExistingIdAsString();

        return includeRequestId(sql.replaceAll(ModelConstants.USER_ID_PLACEHOLDER, userId));
    }

    /**
     * Appends the current request ID as a SQL comment to enable request correlation in database logs.
     * <p>
     * This method retrieves the request ID from the thread-local {@link RequestIdHolder} and
     * appends it to the SQL statement in the format /&#42;request-id&#42;/. If no request ID
     * is available (empty or null), the SQL is returned unchanged.
     * <p>
     * The request ID comment enables correlation between application request logs and database
     * query logs, facilitating troubleshooting of performance issues and tracing query execution
     * back to specific HTTP requests or background jobs.
     * <p>
     * Example transformation:
     * <pre>
     * // Input: "SELECT * FROM users"
     * // Output: "SELECT * FROM users /&#42;req-abc-123&#42;/"
     * </pre>
     *
     * @param sql the SQL statement to which the request ID comment should be appended
     * @return SQL statement with request ID appended as a trailing comment, or the original SQL
     *         if no request ID is available in the current thread context
     * @see RequestIdHolder#getId()
     */
    private String includeRequestId(String sql) {
        return StringUtils.isNotEmpty(RequestIdHolder.getId()) ? String.format("%s /*%s*/", sql, RequestIdHolder.getId()) : sql;
    }
}