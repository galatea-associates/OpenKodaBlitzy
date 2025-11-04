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
 * Field-level annotation specifying SQL expression for populating search index columns.
 * <p>
 * This annotation is typically applied to entity fields (usually named {@code indexString})
 * to configure SQL expressions used for computing searchable content. The specified SQL
 * fragment is used by indexing jobs or database triggers to populate the annotated field
 * with concatenated searchable text from multiple entity columns.

 * <p>
 * Note: This annotation does not declare {@code @Target} or {@code @Retention} meta-annotations.
 * By Java defaults, it can be applied to any program element (though typically used on FIELD)
 * and has CLASS retention (not available at runtime unless consumers compile with explicit retention).
 * Typical usage should specify: {@code @Target(ElementType.FIELD), @Retention(RetentionPolicy.RUNTIME)}.

 * <p>
 * This annotation provides an alternative to repository-level {@link SearchableRepository}
 * and {@link SearchableRepositoryMetadata} annotations, allowing per-field index configuration.

 * <p>
 * Example usage:
 * <pre>{@code
 * @Column(name = "index_string", insertable = false)
 * @ColumnDefault("''")
 * @IndexStringColumn(indexUpdateSql = "(first_name||' '||last_name||' '||email)")
 * private String indexString;
 * }</pre>

 * <p>
 * Indexing jobs or application logic must interpret this annotation via reflection
 * to build UPDATE statements populating the search index. The typical UPDATE pattern is:
 * <pre>{@code
 * UPDATE entity_table SET index_string = (first_name||' '||last_name||' '||email) WHERE ...
 * }</pre>

 *
 * @see SearchableEntity#getIndexString()
 * @see SearchableRepository
 * @see SearchableRepositoryMetadata
 * @see ModelConstants#INDEX_STRING_COLUMN
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public @interface IndexStringColumn {

    /**
     * SQL expression fragment for computing the value of the annotated indexString column.
     * <p>
     * The SQL fragment is used in UPDATE statements by indexing jobs or database triggers
     * to populate the search index with searchable content. The expression uses PostgreSQL's
     * {@code ||} operator for string concatenation.

     * <p>
     * Common usage patterns:

     * <ul>
     *   <li><b>Simple ID indexing (default):</b> {@code "(''||id)"} - concatenates empty string with entity ID</li>
     *   <li><b>Multiple field concatenation:</b> {@code "(name||' '||description)"} - combines name and description</li>
     *   <li><b>Nullable field handling:</b> {@code "(name||' '||COALESCE(email,''))"} - uses COALESCE to avoid null propagation</li>
     *   <li><b>Related entity joins:</b> {@code "(name||' '||(SELECT org.name FROM organization org WHERE org.id = organization_id))"} - includes related entity data</li>
     * </ul>
     * <p>
     * The resulting index string is accessed at query time via {@link SearchableEntity#getIndexString()}.

     *
     * @return SQL fragment for index value computation
     */
    String indexUpdateSql() default "(''||id)";

}