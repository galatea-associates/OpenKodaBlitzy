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

package com.openkoda.core.repository.common;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Generic base repository interface for entities with {@code Long} ID type.
 * This interface locks the ID type to {@code Long} and inherits CRUD and paging semantics from Spring Data JPA.
 * It simplifies repository development by assuming both functional interface usage and {@code Long} ID.
 * <p>
 * By extending {@link JpaRepository}{@code <T, Long>}, this interface provides standard repository operations
 * such as save, findById, findAll, delete, and pagination support. It also adds a convenience method
 * {@link #findOne(Integer)} for repositories that need to accept {@code Integer} IDs.
 * </p>
 *
 * @param <T> the entity type managed by this repository
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see JpaRepository
 */
@NoRepositoryBean
public interface FunctionalRepositoryWithLongId<T> extends JpaRepository<T, Long> {
    /**
     * Example demonstrating Specification/Criteria pattern usage for ID-based queries.
     * This method can be uncommented for Specification-based query building when needed.
     */
//    default Specification<T> idSpecification(Long id) {
//        return (root, query, cb) -> cb.equal(root.get("id"), id);
//    }

    /**
     * Convenience method that accepts an {@code Integer} ID and returns the entity.
     * This method converts the {@code Integer} to {@code Long} and delegates to
     * {@link #findById(Object)}, returning {@code null} if no entity is found.
     * <p>
     * This is useful for code that passes {@code Integer} IDs or prefers nullable
     * returns instead of {@code Optional}.
     * </p>
     *
     * @param aLong the Integer ID to convert to Long and search for
     * @return the entity with the given ID, or {@code null} if no entity exists with that ID
     */
    default T findOne(Integer aLong) {
        return findById(aLong.longValue()).orElse(null);
    }
}
