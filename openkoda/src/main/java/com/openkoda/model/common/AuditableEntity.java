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

import java.util.Collection;
import java.util.Collections;

/**
 * Contract for entities that support audit trail generation.
 * <p>
 * Entities implementing this interface can generate audit trail strings that are persisted
 * in the {@link Audit} entity when changes occur. This interface extends {@link LongIdEntity}
 * to provide {@code getId()} for entity identification during auditing.
 * 
 * <p>
 * The auditing subsystem, implemented by {@link com.openkoda.core.audit.AuditInterceptor},
 * invokes {@link #toAuditString()} on entity changes during Hibernate session flush operations.
 * The generated audit string is stored in {@code Audit.change} (varchar 16380) or
 * {@code Audit.content} (TEXT) fields depending on property configuration.
 * 
 * <p>
 * Typical implementers include {@link OpenkodaEntity} and concrete entity classes that require
 * change tracking. Implementations should provide concise, human-readable descriptions including
 * entity type and key identifying information.
 * 
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see LongIdEntity
 * @see Audit
 * @see com.openkoda.core.audit.AuditInterceptor
 * @see OpenkodaEntity#toAuditString()
 */
public interface AuditableEntity extends LongIdEntity {
    /**
     * Generates the audit trail representation for this entity.
     * <p>
     * This method produces a human-readable string that is persisted in the {@link Audit}
     * entity's {@code change} or {@code content} fields when entity changes are detected.
     * The {@link com.openkoda.core.audit.AuditInterceptor} invokes this method during
     * Hibernate session flush operations.
     * 
     * <p>
     * Implementations should return a concise description including entity type and key
     * identifying information. For example: "Organization: TenantCo" or "User: john@example.com".
     * 
     *
     * @return string representation for the audit log, never null
     */
    String toAuditString();

    /**
     * Returns the collection of property names to exclude from the audit trail.
     * <p>
     * Properties returned by this method will not be included in the audit log when
     * entity changes are detected. This is useful for excluding sensitive fields such
     * as passwords, tokens, or API keys from audit records.
     * 
     * <p>
     * The default implementation returns an empty list, meaning all properties are
     * included in the audit trail. Override this method to specify properties that
     * should be excluded.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * @Override
     * public Collection<String> ignorePropertiesInAudit() {
     *     return Arrays.asList("password", "apiKey", "token");
     * }
     * }</pre>
     * 
     *
     * @return collection of property names to exclude from audit, never null
     */
    default Collection<String> ignorePropertiesInAudit() { return Collections.emptyList(); }

    /**
     * Returns the collection of property names to store in the {@code Audit.content} field.
     * <p>
     * Properties returned by this method will be stored in the {@link Audit} entity's
     * {@code content} field (TEXT column) instead of the {@code change} field (varchar 16380).
     * This is useful for large property values that exceed the varchar limit.
     * 
     * <p>
     * The default implementation returns an empty list, meaning all properties are stored
     * in the {@code Audit.change} field. Override this method to specify properties that
     * should be stored in the TEXT column.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * @Override
     * public Collection<String> contentProperties() {
     *     return Arrays.asList("largeDescription", "htmlContent");
     * }
     * }</pre>
     * 
     *
     * @return collection of property names for {@code Audit.content} storage, never null
     */
    default Collection<String> contentProperties() { return Collections.emptyList(); }
}
