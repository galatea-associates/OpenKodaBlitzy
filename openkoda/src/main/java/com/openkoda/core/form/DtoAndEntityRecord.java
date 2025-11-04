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

package com.openkoda.core.form;

import com.openkoda.model.common.LongIdEntity;

/**
 * Immutable record implementation of {@link DtoAndEntity} for passing DTO and entity pairs.
 * <p>
 * Provides a lightweight carrier for form processing pipelines where both the form DTO
 * representation and the persistent entity representation need to be passed together.
 * This record is commonly used in datalist suppliers and field converters where both
 * representations are needed to compute field values or populate dropdown options.
 * <p>
 * As a Java 17 record, this class automatically provides:
 * <ul>
 *   <li>Canonical constructor accepting dto and entity</li>
 *   <li>Public accessor methods: dto() and entity()</li>
 *   <li>Implementations of equals(), hashCode(), and toString()</li>
 *   <li>Immutability guarantees for the record components</li>
 * </ul>
 * <p>
 * Example usage in form processing:
 * <pre>{@code
 * DtoAndEntityRecord<UserDto, User> record = new DtoAndEntityRecord<>(formDto, persistedEntity);
 * }</pre>
 *
 * @param <D> the DTO type used for form binding and display
 * @param <E> the entity type extending {@link LongIdEntity} for database persistence
 * @param dto the form DTO representation holding form field values
 * @param entity the persistent entity representation for database operations
 * @see DtoAndEntity
 * @see AbstractEntityForm
 * @see FrontendMappingFieldDefinition
 * @since 1.7.1
 * @author OpenKoda Team
 */
public record DtoAndEntityRecord<D, E extends LongIdEntity> (D dto, E entity) implements DtoAndEntity<D, E> {}
