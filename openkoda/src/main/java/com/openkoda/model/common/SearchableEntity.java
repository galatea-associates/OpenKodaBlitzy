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
 * Defines the contract for entities that maintain a full-text search index string.
 * <p>
 * Entities implementing this interface maintain an {@code indexString} column that enables efficient
 * full-text search capabilities across multiple entity fields. The index string contains concatenated
 * searchable text from various entity properties, allowing database queries to search across all relevant
 * data using a single indexed column.
 * </p>
 * <p>
 * The {@code indexString} is typically defined as a non-insertable column with a default empty string value:
 * <pre>
 * {@code @Column(name = INDEX_STRING_COLUMN, insertable = false)}
 * {@code @ColumnDefault("''")}
 * private String indexString;
 * </pre>
 * The column length is defined by {@code INDEX_STRING_COLUMN_LENGTH} from {@code ModelConstants}.
 * </p>
 * <p>
 * The index string is populated through one of three primary mechanisms:
 * <ol>
 *   <li>Database triggers that automatically update the index on INSERT or UPDATE operations</li>
 *   <li>Scheduled index update jobs using {@code indexUpdateSql} configured in {@code @SearchableRepository} 
 *       or {@code @SearchableRepositoryMetadata} annotations</li>
 *   <li>Application-level index population in the service layer</li>
 * </ol>
 * </p>
 * <p>
 * The populated index string enables SQL LIKE, ILIKE, or database-specific full-text search queries,
 * typically through repository methods. The {@code SearchableRepositoryMetadata} annotation configures
 * search behavior including the {@code includeInGlobalSearch} flag that controls whether the entity
 * appears in global search results.
 * </p>
 * <p>
 * {@code OpenkodaEntity} provides the base implementation with the {@code indexString} field, making
 * this interface automatically satisfied for most domain entities extending that base class.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.OpenkodaEntity
 * @see com.openkoda.core.repository.common.SearchableRepositoryMetadata
 * @see com.openkoda.core.repository.common.SearchableRepository
 * @see com.openkoda.model.common.ModelConstants#INDEX_STRING_COLUMN
 * @see com.openkoda.model.common.ModelConstants#INDEX_STRING_COLUMN_LENGTH
 * @see com.openkoda.core.repository.common.IndexStringColumn
 */
public interface SearchableEntity {

    /**
     * Returns the search index string containing concatenated searchable entity data.
     * <p>
     * The index string combines entity properties such as name, key identifiers, descriptions,
     * and related entity names into a single searchable text field. This enables efficient
     * full-text search queries across multiple entity attributes without complex joins or
     * multiple LIKE clauses.
     * </p>
     * <p>
     * The index string is typically populated automatically by the database using SQL formulas
     * defined in {@code searchIndexFormula} from {@code @SearchableRepositoryMetadata}, executed
     * via UPDATE statements during index refresh operations. The content format and included
     * fields vary by entity type based on which properties are most relevant for search.
     * </p>
     * <p>
     * Due to the {@code @ColumnDefault("''")} annotation on implementing classes, this method
     * typically returns a non-null value. However, the string may be empty if the index has not
     * yet been populated for a newly created entity.
     * </p>
     *
     * @return the search index string containing concatenated searchable text from this entity,
     *         may be an empty string if not yet populated but typically non-null due to column default
     */
    String getIndexString();

}
