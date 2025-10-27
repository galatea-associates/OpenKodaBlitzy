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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runtime repository-level annotation for configuring automatic search index updates.
 * <p>
 * This annotation marks Spring Data JPA repository interfaces whose entities should have
 * their search index strings updated automatically by scheduled indexing jobs. The annotation
 * is applied to repository interface types and provides SQL configuration for computing
 * index string values.
 * </p>
 * <p>
 * Repositories annotated with {@code @SearchableRepository} are discovered at runtime via
 * reflection by indexing jobs, which scan the classpath to identify searchable entities.
 * The annotation works in conjunction with entities implementing the {@code SearchableEntity}
 * interface to enable full-text search capabilities.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * @SearchableRepository(indexUpdateSql = "(name||' '||email)")
 * public interface UserRepository extends JpaRepository<User, Long> {
 * }
 * }</pre>
 * </p>
 * <p>
 * Processing workflow: Indexing jobs use reflection to find all {@code @SearchableRepository}
 * annotations, extract the {@code indexUpdateSql} value, and execute UPDATE statements to
 * populate the {@code index_string} column with computed search text.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SearchableEntity
 * @see com.openkoda.repository.SearchableRepositoryMetadata
 * @see com.openkoda.model.common.IndexStringColumn
 * @see com.openkoda.core.helper.ModelConstants#INDEX_STRING_COLUMN
 */
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface SearchableRepository {

    /**
     * SQL expression fragment used to populate the index_string column for full-text search.
     * <p>
     * This SQL fragment is inserted into UPDATE statements by indexing jobs to compute
     * searchable text for each entity row. The expression must be valid PostgreSQL syntax
     * and typically concatenates relevant entity fields using the {@code ||} string
     * concatenation operator.
     * </p>
     * <p>
     * Common patterns:
     * <ul>
     * <li><b>Simple ID indexing:</b> {@code "(''||id)"} - Converts entity ID to string (default)</li>
     * <li><b>Multiple fields:</b> {@code "(name||' '||description||' '||COALESCE(email,''))"} - Concatenates name, description, and email</li>
     * <li><b>Related entities:</b> {@code "(name||' '||(SELECT org.name FROM organization org WHERE org.id = organization_id))"} - Includes related organization name</li>
     * </ul>
     * </p>
     * <p>
     * <b>Important considerations:</b>
     * <ul>
     * <li>Use {@code COALESCE(field,'')} for nullable fields to avoid null propagation</li>
     * <li>The {@code ||} operator performs string concatenation in PostgreSQL</li>
     * <li>Prefix numeric values with empty string: {@code (''||id)} converts numbers to text</li>
     * <li>Complex expressions may include subqueries for related entity data</li>
     * </ul>
     * </p>
     * <p>
     * The generated UPDATE query pattern:
     * <pre>
     * UPDATE entity_table SET index_string = [indexUpdateSql] WHERE ...
     * </pre>
     * </p>
     *
     * @return SQL fragment for computing the index string value
     */
    String indexUpdateSql() default "(''||id)";

}