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

import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.common.ModelConstants;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Composite base repository interface providing ergonomic adapters for repositories without scope-based security.
 * <p>
 * This interface extends JPA repository methods that return {@code void} with methods returning values,
 * enabling use in chained lambda expressions and functional composition patterns. Unlike secured repositories,
 * this interface does not enforce privilege checks, making it suitable for internal system operations.
 * </p>
 * <p>
 * Combines functionality from {@link FunctionalRepositoryWithLongId}, {@link JpaSpecificationExecutor},
 * {@link ModelConstants}, and {@link HasSecurityRules} to provide a complete data access foundation
 * with Specification-based query support.
 * </p>
 *
 * @param <T> the entity type managed by this repository
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see JpaSpecificationExecutor
 * @see FunctionalRepositoryWithLongId
 * @see HasSecurityRules
 */
@NoRepositoryBean
public interface UnsecuredFunctionalRepositoryWithLongId<T> extends FunctionalRepositoryWithLongId<T>, JpaSpecificationExecutor<T>, ModelConstants, HasSecurityRules {

    /**
     * Finds an entity by its Long ID, returning null if not found.
     * <p>
     * This is a nullable adapter over {@code findById(id).orElse(null)}, designed for use
     * in lambda expressions where Optional handling is inconvenient.
     * </p>
     *
     * @param id the Long ID to search for
     * @return the entity with the given ID, or null if not found
     */
    default T findOne(Long id){
        return findById(id).orElse(null);
    }
    /**
     * Deletes an entity by its Long ID, returning true after successful deletion.
     * <p>
     * This boolean return adapter delegates to {@code deleteById} but returns a value for use
     * in chained lambda expressions. Always returns true after deletion completes.
     * </p>
     *
     * @param aLong the ID of the entity to delete
     * @return always returns true after deletion completes
     */
    default boolean deleteOne(Long aLong) {
        deleteById(aLong);
        return true;
    }

    /**
     * Deletes the entity with the given Long ID.
     * <p>
     * This abstract method must be implemented by concrete repository implementations
     * to provide entity deletion functionality.
     * </p>
     *
     * @param aLong the ID of the entity to delete
     */
    void deleteById(Long aLong);

}
