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
 * Carrier interface that provides access to both DTO and entity representations for form processing.
 * <p>
 * This interface enables form components to work with dual representations of data. The DTO is used
 * for form binding and display logic, while the entity provides access to the persistent domain model.
 * This separation allows forms to present simplified views while maintaining references to full entities
 * for database operations.
 * </p>
 * <p>
 * The interface is commonly used in datalist suppliers and field converters where both the display
 * representation (DTO) and the underlying persistent state (entity) are needed. For example, a
 * dropdown field may display DTO values while storing entity identifiers.
 * </p>
 * <p>
 * Example usage in a datalist function:
 * <pre>{@code
 * (dtoAndEntity, repo) -> repo.findById(dtoAndEntity.getEntity().getId())
 * }</pre>
 * </p>
 *
 * @param <D> the DTO type used for form binding and display
 * @param <E> the entity type extending LongIdEntity for database persistence
 * @since 1.7.1
 * @author OpenKoda Team
 * @see AbstractEntityForm
 * @see FrontendMappingFieldDefinition
 */
public interface DtoAndEntity<D, E extends LongIdEntity> {

    /**
     * Returns the DTO representation used for form binding and display.
     * <p>
     * The DTO provides a simplified view of the data suitable for presentation in forms and UI
     * components. It contains only the fields needed for display and user interaction.
     * </p>
     *
     * @return the DTO instance, or null if not available
     */
    default D getDto() {
        return null;
    }

    /**
     * Returns the persistent entity representation for database operations.
     * <p>
     * The entity provides access to the full domain model including relationships, computed fields,
     * and database-specific attributes. Use this method when you need to perform queries, updates,
     * or access entity identifiers.
     * </p>
     *
     * @return the entity instance extending LongIdEntity, or null if not available
     */
    default E getEntity() {
        return null;
    }
}
