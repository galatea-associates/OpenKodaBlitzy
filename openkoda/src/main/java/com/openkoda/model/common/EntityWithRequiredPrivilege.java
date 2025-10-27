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

package com.openkoda.model.common;

/**
 * Interface that can be implemented by {@link jakarta.persistence.Entity} in order to provide
 * required read/write privilege for per-instance access control.
 * <p>
 * This interface enables fine-grained, row-level privilege enforcement by allowing entities to
 * specify the privileges required to read or write each specific instance. The privilege requirements
 * are typically implemented using JPA {@code @Formula} annotations that compute the required privilege
 * name dynamically based on the entity's properties and the current user context.
 * </p>
 * <p>
 * <b>Integration with SecureRepository:</b><br/>
 * The {@link com.openkoda.repository.SecureRepository} evaluates these privileges at query time,
 * adding WHERE clause filters to ensure users can only access entities for which they hold the
 * required privileges. This provides transparent, declarative access control at the repository layer.
 * </p>
 * <p>
 * <b>ModelConstants.USER_ID_PLACEHOLDER Token:</b><br/>
 * A special placeholder token {@code ModelConstants.USER_ID_PLACEHOLDER} can be used in native SQL
 * expressions within {@code @Formula} annotations. This token is automatically replaced with the
 * current user's ID at query execution time, enabling per-user privilege logic such as "user can
 * read their own data without additional privileges."
 * </p>
 * <p>
 * <b>@Formula Usage Pattern:</b><br/>
 * Computed fields annotated with {@code @Formula} return privilege names or SQL expressions that
 * evaluate to privilege names. The formula is executed in the database, allowing complex conditional
 * logic for privilege requirements. Common patterns include:
 * <ul>
 * <li>Constant privilege: Always require a specific privilege (e.g., {@code PrivilegeNames._readUserData})</li>
 * <li>Null for public access: Return NULL when no privilege is required (grants public or self-access)</li>
 * <li>Conditional privilege: Use CASE expressions to require privileges based on entity state or user identity</li>
 * </ul>
 * </p>
 * <p>
 * <b>Privilege Inheritance and Null Values:</b><br/>
 * When {@code getRequiredReadPrivilege()} or {@code getRequiredWritePrivilege()} returns {@code null},
 * no privilege is required for that operation, effectively granting public access. This is commonly used
 * for user-owned data where users can access their own records without explicit privileges, or for
 * truly public data accessible to all authenticated users.
 * </p>
 * <p>
 * <b>Use Cases:</b>
 * <ul>
 * <li><b>Per-user data access:</b> Users can read their own data without privileges, but need specific
 * privileges to read others' data (e.g., user profiles, personal settings)</li>
 * <li><b>Per-organization privileges:</b> Access controlled based on organization membership and
 * organization-specific roles</li>
 * <li><b>Role-based access control (RBAC):</b> Different privilege requirements for different entity
 * states or types within the same table</li>
 * <li><b>Hierarchical permissions:</b> Read access may be public/null while write access requires
 * specific management privileges</li>
 * </ul>
 * </p>
 * <p>
 * Typical implementation:
 * <pre>{@code
 * @Entity Class Person implements EntityWithRequiredPrivilege {
 *     ...
 *
 *     @Formula("( CASE id WHEN " + ModelConstants.USER_ID_PLACEHOLDER + " THEN NULL ELSE '" + PrivilegeNames._readUserData + "' END )")
 *     private String requiredReadPrivilege;
 *
 *     @Formula("( '" + PrivilegeNames._manageUserData + "' )")
 *     private String requiredWritePrivilege;
 *
 *     @Override public String getRequiredReadPrivilege() {
 *         return requiredReadPrivilege;
 *     }
 *
 *     @Override
 *     public String getRequiredWritePrivilege() {
 *         return requiredWritePrivilege;
 *     }
 *
 * }
 * }</pre>
 * </p>
 *
 * @see com.openkoda.model.common.ModelConstants#USER_ID_PLACEHOLDER
 * @see com.openkoda.core.security.PrivilegeNames
 * @see com.openkoda.repository.SecureRepository
 * @see com.openkoda.model.common.OpenkodaEntity
 * @see com.openkoda.model.Privilege
 * @see com.openkoda.model.PrivilegeBase
 * @since 1.7.1
 * @version 1.7.1
 * @author OpenKoda Team
 */
public interface EntityWithRequiredPrivilege {

    /**
     * Returns the privilege name required for read access to this specific entity instance,
     * or {@code null} if no privilege is required (public or self-access).
     * <p>
     * This method is typically implemented by returning a computed field annotated with
     * {@code @Formula}, which evaluates to a privilege name string at query time. The
     * {@link com.openkoda.repository.SecureRepository} uses this value to filter query
     * results, ensuring users can only retrieve entities for which they hold the required
     * read privilege.
     * </p>
     * <p>
     * <b>Common Implementation Patterns:</b>
     * </p>
     * <ol>
     * <li><b>Constant privilege requirement:</b> Always require a specific privilege
     * <pre>{@code
     * @Formula("( '" + PrivilegeNames._readUserData + "' )")
     * private String requiredReadPrivilege;
     * }</pre>
     * </li>
     * <li><b>Null for public/own-data access:</b> No privilege required for authenticated users
     * or for users accessing their own data
     * <pre>{@code
     * @Formula("( NULL )")
     * private String requiredReadPrivilege;
     * }</pre>
     * </li>
     * <li><b>Conditional privilege with user context:</b> Users can read their own data without
     * a privilege, but need a privilege to read others' data
     * <pre>{@code
     * @Formula("( CASE id WHEN " + ModelConstants.USER_ID_PLACEHOLDER + " THEN NULL ELSE '" + PrivilegeNames._readUserData + "' END )")
     * private String requiredReadPrivilege;
     * }</pre>
     * </li>
     * </ol>
     * <p>
     * <b>Query-Level Enforcement:</b><br/>
     * The {@code SecureRepository} adds a WHERE clause to queries that filters results based on
     * the current user's privileges. If this method returns a privilege name, only entities for
     * which the user holds that privilege will be included in query results. If this method returns
     * {@code null}, the entity is accessible to all users (subject to other query constraints).
     * </p>
     * <p>
     * <b>Runtime Evaluation:</b><br/>
     * The {@code @Formula} expression is evaluated in the database at query time, not in Java code.
     * This allows efficient filtering at the database level and supports dynamic privilege logic
     * using SQL expressions such as CASE statements, user ID comparisons, and organization checks.
     * </p>
     *
     * @return privilege name string required for read access (e.g., {@code "canReadUserData"}),
     *         or {@code null} if no privilege is required for read access
     * @see com.openkoda.repository.SecureRepository
     * @see com.openkoda.model.common.ModelConstants#USER_ID_PLACEHOLDER
     * @see com.openkoda.core.security.PrivilegeNames
     */
    String getRequiredReadPrivilege();

    /**
     * Returns the privilege name required for write access (update or delete operations) to this
     * specific entity instance, or {@code null} if no privilege is required (public write access).
     * <p>
     * This method is typically implemented by returning a computed field annotated with
     * {@code @Formula}, which evaluates to a privilege name string at query time. The
     * {@link com.openkoda.repository.SecureRepository} enforces this privilege before allowing
     * update or delete operations, throwing a security exception if the current user does not
     * hold the required privilege.
     * </p>
     * <p>
     * <b>Common Implementation Patterns:</b>
     * </p>
     * <ol>
     * <li><b>Constant privilege requirement:</b> Always require a specific management privilege
     * for write operations (most common pattern)
     * <pre>{@code
     * @Formula("( '" + PrivilegeNames._manageUserData + "' )")
     * private String requiredWritePrivilege;
     * }</pre>
     * </li>
     * <li><b>Null for unrestricted write access:</b> No privilege required (rare, typically only
     * for user-owned data where users can modify their own records)
     * <pre>{@code
     * @Formula("( NULL )")
     * private String requiredWritePrivilege;
     * }</pre>
     * </li>
     * <li><b>Conditional privilege based on entity state:</b> Different privileges required based
     * on entity properties or user context
     * <pre>{@code
     * @Formula("( CASE status WHEN 'DRAFT' THEN '" + PrivilegeNames._editDraft + "' ELSE '" + PrivilegeNames._editPublished + "' END )")
     * private String requiredWritePrivilege;
     * }</pre>
     * </li>
     * </ol>
     * <p>
     * <b>Write Operation Enforcement:</b><br/>
     * Unlike read privileges which filter query results, write privileges are checked before
     * allowing updates or deletes. The {@code SecureRepository} verifies the current user holds
     * the required privilege before executing the write operation. If the privilege check fails,
     * the operation is blocked and a security exception is thrown.
     * </p>
     * <p>
     * <b>Privilege Hierarchy:</b><br/>
     * Write privileges are typically more restrictive than read privileges. A common pattern is to
     * allow public or self-read access (null read privilege) while requiring explicit management
     * privileges for write operations. This implements the principle of least privilege where
     * viewing data is less restricted than modifying it.
     * </p>
     * <p>
     * <b>Runtime Evaluation:</b><br/>
     * The {@code @Formula} expression is evaluated in the database at query time, allowing
     * privilege requirements to be computed dynamically based on entity state, user identity,
     * organization membership, or other database-accessible factors.
     * </p>
     *
     * @return privilege name string required for write access (e.g., {@code "canManageUserData"}),
     *         or {@code null} if no privilege is required for write access
     * @see com.openkoda.repository.SecureRepository
     * @see com.openkoda.core.security.PrivilegeNames
     * @see com.openkoda.model.common.ModelConstants#USER_ID_PLACEHOLDER
     */
    String getRequiredWritePrivilege();
}
