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

package com.openkoda.repository;

import com.openkoda.model.DbVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository managing DbVersion entities for database schema migration tracking.
 * <p>
 * Extends {@link JpaRepository} providing standard CRUD operations for {@link DbVersion} entities.
 * This repository manages records that track applied database migrations with version numbers
 * (major, minor, build, revision) and execution timestamps. The primary use case is schema
 * upgrade orchestration where the {@link #findCurrentDbVersion()} method returns the latest
 * successfully applied migration.
 * </p>
 * <p>
 * Used by DbVersionService to check current schema state before applying new upgrades.
 * Version ordering is computed via the expression: (major * 1000 + minor * 100 + build * 10 + revision).
 * </p>
 *
 * @author mboronski
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see DbVersion
 * @see org.springframework.data.jpa.repository.JpaRepository
 */
@Repository
public interface DbVersionRepository extends JpaRepository<DbVersion, Long> {

    /**
     * Retrieves the most recently applied database version record.
     * <p>
     * Executes a custom JPQL query that filters for completed migrations ({@code done = true})
     * and orders results by a computed numeric expression combining version components:
     * {@code (major * 1000 + minor * 100 + build * 10 + revision)}. This ordering ensures
     * that version 2.1.0.0 is correctly identified as newer than 1.9.8.5.
     * </p>
     * <p>
     * Usage: DbVersionService calls this method to determine the current schema state
     * before applying new migrations. If no migrations have been applied, returns null.
     * </p>
     *
     * @return the latest {@link DbVersion} entity based on computed numeric ordering,
     *         or null if no completed migrations exist in the database
     * @see DbVersion
     */
    @Query("SELECT v FROM DbVersion v WHERE v.done = true ORDER BY (v.major  * 1000 + v.minor * 100 + v.build * 10 + v.revision) DESC LIMIT 1")
    DbVersion findCurrentDbVersion();
        
}
