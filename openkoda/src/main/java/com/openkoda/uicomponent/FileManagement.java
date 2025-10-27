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

package com.openkoda.uicomponent;

import com.openkoda.model.file.File;

import java.util.List;

/**
 * Lightweight service contract for file enumeration and ID retrieval.
 * <p>
 * Provides minimal file listing methods without full metadata retrieval.
 * Suitable for UI components requiring file references without loading complete File entities.
 * Implementations must handle pagination, authorization, and stable ordering.
 * Intended for scenarios where only file IDs or basic file records are needed.
 * </p>
 * <p>
 * Security Context: All methods enforce tenant-scoped access control based on the current user's
 * organization and privilege context. Returned files are filtered to only include those accessible
 * to the authenticated user.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.file.File
 */
public interface FileManagement {

    /**
     * Retrieves list of File entities accessible to current user within tenant context.
     * <p>
     * Returns File entities with metadata (id, filename, contentType, createdOn).
     * Implementation must enforce security scope (organization or user-level access),
     * apply tenant filtering via organizationId. No explicit ordering guarantee -
     * implementation may return in ID order or creation order.
     * </p>
     * <p>
     * Security: Must filter by current user's organization and privilege context.
     * Pagination not exposed in interface - implementations may limit result size
     * or return all accessible files.
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * List&lt;File&gt; files = fileManagement.getFiles();
     * // Returns [File{id=1, filename='doc.pdf'}, File{id=2, filename='image.png'}]
     * </pre>
     * </p>
     *
     * @return List of File entities accessible to current user, may be empty if no files accessible
     */
    List<File> getFiles();

    /**
     * Retrieves list of File IDs accessible to current user within tenant context.
     * <p>
     * Returns only file IDs without loading full File entity metadata.
     * More efficient than {@code getFiles()} when only IDs needed (e.g., for batch operations,
     * ID validation, existence checks). Must enforce same security and tenant filtering as getFiles().
     * </p>
     * <p>
     * Performance Note: More efficient than getFiles() - avoids loading filename, contentType,
     * blob references.
     * </p>
     * <p>
     * Use Cases:
     * <ul>
     *   <li>Populate dropdown/select options with file IDs</li>
     *   <li>Validate file ownership before operations</li>
     *   <li>Batch file ID collection without entity hydration</li>
     * </ul>
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * List&lt;Long&gt; fileIds = fileManagement.getFilesId();
     * // Returns [1L, 2L, 5L, 12L]
     * </pre>
     * </p>
     *
     * @return List of File IDs (Long) accessible to user, may be empty
     */
    List<Long> getFilesId();
}