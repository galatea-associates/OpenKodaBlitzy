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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openkoda.model.common.TimestampedEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

 /*
 TODO: rewrite
  */
/**
 * JPA mapped superclass that combines {@link TimestampedEntity} with file attachment capabilities.
 * <p>
 * This abstract base class provides reusable file attachment mappings for concrete entities through
 * a dual persistence strategy: full {@link File} entity objects and lightweight {@link Long} IDs.
 * Both representations are persisted to the shared {@code file_reference} join table with an
 * {@code organization_related_entity_id} foreign key and {@code file_id} column.

 * <p>
 * The dual persistence approach serves different use cases:
 * <ul>
 *   <li>{@code files} - Full {@link File} entity objects mapped via {@code @ManyToMany}, suitable
 *       for accessing complete file metadata and content streams. Marked {@code @JsonIgnore} to
 *       prevent JSON serialization. Uses {@code fetch=LAZY} for performance optimization.</li>
 *   <li>{@code filesId} - Lightweight {@link Long} ID collection mapped via {@code @ElementCollection},
 *       suitable for existence checks and lightweight persistence operations. Initialized to empty
 *       {@link ArrayList} to avoid null checks.</li>
 * </ul>

 * <p>
 * Both collections maintain attachment order via {@code @OrderColumn(name="sequence")}, ensuring
 * files are retrieved in the sequence they were attached. The {@code files} field uses
 * {@code cascade={}} to prevent unintended file deletions when the parent entity is removed.

 * <p>
 * <strong>Important persistence semantics:</strong>
 * The {@code files} field may be {@code null} outside JPA-managed contexts or when not yet loaded,
 * while {@code filesId} is always initialized to an empty list. The join column is marked
 * {@code insertable=false, updatable=false} indicating non-owning side semantics.

 * <p>
 * Concrete entities extending this class inherit auditing fields from {@link TimestampedEntity}
 * (createdOn, updatedOn) and file attachment capabilities from {@link EntityWithFiles}.

 * <p>
 * <strong>Example usage:</strong>
 * <pre>{@code
 * public class Document extends TimestampedEntityWithFiles {
 *     // Document-specific fields
 * }
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see TimestampedEntity
 * @see EntityWithFiles
 * @see File
 */
@MappedSuperclass
public abstract class TimestampedEntityWithFiles extends TimestampedEntity implements EntityWithFiles {

    /**
     * Collection of {@link File} entity objects attached to this entity.
     * <p>
     * Mapped via {@code @ManyToMany} to the {@code file_reference} join table with columns:
     * <ul>
     *   <li>{@code organization_related_entity_id} - Foreign key to parent entity (non-owning side)</li>
     *   <li>{@code file_id} - Foreign key to {@link File} entity</li>
     *   <li>{@code sequence} - Order column maintaining attachment sequence</li>
     * </ul>

     * <p>
     * Uses {@code fetch=LAZY} for performance optimization - files are not loaded until accessed.
     * Empty cascade configuration {@code cascade={}} prevents unintended file deletions when parent
     * entity is removed. Marked {@code @JsonIgnore} to prevent JSON serialization of potentially
     * large file metadata in REST responses.

     * <p>
     * <strong>Null semantics:</strong> This field may be {@code null} outside JPA-managed contexts
     * or when not yet loaded. Prefer {@link #filesId} for lightweight existence checks and
     * {@link #getFilesId()} for avoiding lazy loading triggers.

     *
     * @see #filesId
     * @see File
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(
            name="file_reference",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT),
            inverseJoinColumns =  @JoinColumn(name = "file_id"),
            joinColumns = @JoinColumn(name = "organization_related_entity_id", insertable = false, updatable = false)
    )
    @JsonIgnore
    @OrderColumn(name="sequence")
    protected List<File> files;

    /**
     * Collection of file IDs ({@link Long}) attached to this entity.
     * <p>
     * Mapped via {@code @ElementCollection} to the same {@code file_reference} join table as
     * {@link #files}, providing a lightweight alternative to loading full {@link File} entities.
     * Persisted with columns:
     * <ul>
     *   <li>{@code organization_related_entity_id} - Foreign key to parent entity</li>
     *   <li>{@code file_id} - The file ID value (stored in {@code @Column})</li>
     *   <li>{@code sequence} - Order column maintaining attachment sequence</li>
     * </ul>

     * <p>
     * Initialized to empty {@link ArrayList} to avoid null checks in application code.
     * Uses {@code fetch=LAZY} to defer loading until accessed, though ID collections are
     * typically lighter weight than entity collections.

     * <p>
     * <strong>Preferred for lightweight operations:</strong> Use this field for existence checks
     * via {@link #hasFiles()} and lightweight persistence operations that don't require full file
     * metadata. Avoids triggering lazy loading of {@link File} entity objects.

     *
     * @see #files
     * @see #getFilesId()
     * @see #hasFiles()
     */
    @ElementCollection(fetch = FetchType.LAZY, targetClass = Long.class)
    @CollectionTable(name = "file_reference", joinColumns = @JoinColumn(name = "organization_related_entity_id"), foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @Column(name="file_id")
    @OrderColumn(name="sequence")
    protected List<Long> filesId = new ArrayList<>();

    /**
     * Sets the collection of file IDs attached to this entity.
     * <p>
     * This method sets the ID collection used for lightweight persistence operations.
     * The provided list replaces the current {@link #filesId} collection and will be
     * persisted to the {@code file_reference} table on entity save/update.

     * <p>
     * Note that setting {@code filesId} does not automatically populate or synchronize
     * the {@link #files} entity collection. Callers are responsible for maintaining
     * consistency between the two representations if both are used.

     *
     * @param filesId the list of file IDs to associate with this entity, may be empty or null
     */
    public void setFilesId(List<Long> filesId) {
        this.filesId = filesId;
    }

    /**
     * Returns the collection of {@link File} entity objects attached to this entity.
     * <p>
     * Retrieves full file metadata including filename, content type, size, storage location,
     * and access to content streams via {@link File#getContentStream()}. This method may
     * trigger lazy loading if the files have not been previously loaded within the current
     * persistence context.

     * <p>
     * <strong>Performance consideration:</strong> Prefer {@link #getFilesId()} for lightweight
     * operations such as existence checks or displaying file counts, as it avoids loading full
     * entity objects from the database.

     *
     * @return the list of {@link File} entities, or {@code null} if not loaded or no files attached
     * @see #getFilesId()
     * @see File#getContentStream()
     */
    public List<File> getFiles() {
        return files;
    }

    /**
     * Returns the collection of file IDs ({@link Long}) attached to this entity.
     * <p>
     * Provides a lightweight alternative to {@link #getFiles()} for operations that only
     * require file identifiers without full metadata. This method returns the initialized
     * {@link #filesId} collection which is never {@code null} (defaults to empty {@link ArrayList}).

     * <p>
     * Use this method for:
     * <ul>
     *   <li>Existence checks - determine if entity has attachments without loading entities</li>
     *   <li>Count operations - get number of attached files efficiently</li>
     *   <li>Reference storage - persist file associations by ID only</li>
     * </ul>

     *
     * @return the list of file IDs, never {@code null} (initialized to empty list)
     * @see #getFiles()
     * @see #hasFiles()
     */
    public List<Long> getFilesId() {
        return filesId;
    }

    /**
     * Checks whether this entity has any file attachments.
     * <p>
     * This implementation overrides the default {@link EntityWithFiles#hasFiles()} method
     * with an optimized version that checks the {@link #files} entity list directly rather
     * than the {@link #filesId} collection. This approach is more efficient when the files
     * collection is already loaded in the persistence context.

     * <p>
     * <strong>Implementation difference:</strong> The interface default implementation checks
     * {@code getFilesId() != null && getFilesId().size() > 0}, which may call {@code getFilesId()}
     * twice. This override checks {@code files != null && files.size() > 0} directly, avoiding
     * duplicate method calls.

     * <p>
     * Note that if {@link #files} is {@code null} (not yet loaded), this method returns {@code false}
     * even if the entity has file IDs in {@link #filesId}. This is acceptable for entities where
     * the files collection has not been initialized or loaded via JPA.

     *
     * @return {@code true} if the files collection is non-null and contains at least one file,
     *         {@code false} otherwise
     * @see EntityWithFiles#hasFiles()
     * @see #getFilesId()
     */
    public boolean hasFiles() {
        return files != null && files.size() > 0;
    }

}