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

package com.openkoda.model.file;

import java.util.List;

/**
 * Marker interface for entities and DTOs that support file attachments in the OpenKoda platform.
 * <p>
 * This interface defines a dual representation pattern for file attachments, allowing implementors
 * to expose either full {@link File} entity objects via {@link #getFiles()} or lightweight
 * {@link Long} identifiers via {@link #getFilesId()}. This dual approach enables flexibility
 * in different application layers and usage contexts.
 * </p>
 *
 * <h3>Use Cases</h3>
 * <ul>
 *     <li>JPA entities implement this interface to expose file attachments with lazy loading support</li>
 *     <li>DTOs implement this interface for API serialization with minimal overhead</li>
 *     <li>Service layers can work polymorphically with any file-bearing object</li>
 * </ul>
 *
 * <h3>Implementation Flexibility</h3>
 * <p>
 * Implementors can choose which representation to populate based on their needs:
 * </p>
 * <ul>
 *     <li><b>Entity objects only</b>: Populate {@code files}, return null from {@code filesId}</li>
 *     <li><b>IDs only</b>: Populate {@code filesId}, return null from {@code files}</li>
 *     <li><b>Both representations</b>: Maintain consistency between both collections</li>
 * </ul>
 *
 * <p>
 * The interface does not enforce null-versus-empty-list semantics. Implementors decide whether
 * to return {@code null} or an empty collection when no files are attached.
 * </p>
 *
 * <h3>Performance Considerations</h3>
 * <p>
 * When backed by JPA lazy-loading associations (e.g., {@code @ManyToMany(fetch=LAZY)}),
 * calling {@link #getFiles()} may trigger database queries to hydrate the full entity graph.
 * For existence checks or lightweight operations, prefer {@link #getFilesId()} or
 * {@link #hasFiles()} to avoid unnecessary object materialization.
 * </p>
 *
 * <h3>Common Implementors</h3>
 * <ul>
 *     <li>{@link TimestampedEntityWithFiles}: Base class providing JPA mappings for file attachments</li>
 *     <li>Various DTO classes: Lightweight transfer objects for REST API serialization</li>
 * </ul>
 *
 * <p>
 * This interface enables polymorphic file handling across entity and DTO layers, allowing
 * controllers, services, and repositories to work with file attachments in a uniform manner
 * regardless of the underlying storage mechanism.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see File
 * @see TimestampedEntityWithFiles
 */
public interface EntityWithFiles {

    /**
     * Returns the list of {@link File} entity objects attached to this entity.
     * <p>
     * This method provides access to full file metadata including filename, content type,
     * size, and storage location. When backed by JPA associations (e.g., {@code @ManyToMany}),
     * calling this method may trigger lazy loading and execute database queries to hydrate
     * the file entities.
     * </p>
     *
     * <h3>Null Semantics</h3>
     * <p>
     * The return value may be {@code null} or an empty list depending on the implementation.
     * Callers should handle both cases. The {@link #hasFiles()} method provides a convenient
     * null-safe check for file presence.
     * </p>
     *
     * <h3>Performance Note</h3>
     * <p>
     * For lightweight existence checks or when only file identifiers are needed, prefer
     * {@link #getFilesId()} to avoid the overhead of loading full entity objects.
     * </p>
     *
     * @return list of {@link File} entity objects, or {@code null} if not initialized
     * @see #getFilesId()
     * @see #hasFiles()
     */
    List<File> getFiles();

    /**
     * Returns the list of file identifiers as {@link Long} values for files attached to this entity.
     * <p>
     * This method provides a lightweight alternative to {@link #getFiles()}, returning only the
     * primary key identifiers of attached files without loading full entity objects. This is
     * particularly useful for:
     * </p>
     * <ul>
     *     <li>Checking file attachment presence without triggering lazy loading</li>
     *     <li>API serialization where full file metadata is not required</li>
     *     <li>Bulk operations that only need to reference file IDs</li>
     *     <li>Performance-sensitive contexts where entity hydration is expensive</li>
     * </ul>
     *
     * <h3>Null Semantics</h3>
     * <p>
     * The return value may be {@code null} or an empty list depending on the implementation.
     * Some implementations initialize this collection to an empty list (e.g., {@code new ArrayList<>()})
     * to avoid null checks, while others may return {@code null} if not explicitly set.
     * Callers should handle both cases defensively.
     * </p>
     *
     * <h3>Relationship with getFiles()</h3>
     * <p>
     * Implementations may maintain consistency between {@code filesId} and {@code files} collections,
     * but this is not required by the interface contract. Some implementations may populate only
     * one representation depending on the usage context.
     * </p>
     *
     * @return list of file identifiers as {@link Long} values, or {@code null} if not initialized
     * @see #getFiles()
     * @see #hasFiles()
     */
    List<Long> getFilesId();

    /**
     * Checks whether this entity has any file attachments.
     * <p>
     * This default implementation provides a convenient null-safe check by calling
     * {@link #getFilesId()} and verifying that the result is both non-null and non-empty.
     * </p>
     *
     * <h3>Default Implementation</h3>
     * <pre>{@code
     * return getFilesId() != null && getFilesId().size() > 0;
     * }</pre>
     *
     * <h3>Performance Warning</h3>
     * <p>
     * <b>IMPORTANT:</b> This default implementation calls {@link #getFilesId()} twice:
     * once for the null check and once for the size check. If {@code getFilesId()} performs
     * expensive operations such as lazy loading or collection materialization, this can
     * result in repeated costly calls within a single invocation.
     * </p>
     *
     * <h3>Recommendation for Implementors</h3>
     * <p>
     * Implementations should consider overriding this method to provide optimized behavior,
     * especially when:
     * </p>
     * <ul>
     *     <li>File ID collection is lazily loaded from the database</li>
     *     <li>Collection access triggers expensive computations</li>
     *     <li>A direct field reference can avoid repeated method calls</li>
     *     <li>Alternative existence checks are available (e.g., checking {@code files} collection)</li>
     * </ul>
     *
     * <p>
     * For example, {@link TimestampedEntityWithFiles} overrides this method to check the
     * {@code files} collection directly, avoiding the potential double-call overhead.
     * </p>
     *
     * @return {@code true} if the entity has one or more file attachments, {@code false} otherwise
     * @see #getFilesId()
     * @see TimestampedEntityWithFiles#hasFiles()
     */
    default boolean hasFiles() {
        return getFilesId() != null && getFilesId().size() > 0;
    }

}
