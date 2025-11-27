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

package com.openkoda.service.export.converter;

import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

/**
 * Generic interface defining contract for converting domain entities into YAML-oriented export artifacts.
 * <p>
 * This interface provides the foundation for serializing OpenKoda entities to portable YAML format
 * for export/import operations, backup procedures, and cross-environment data migration. Implementations
 * handle entity-to-DTO conversion, ZIP archive generation, and optional database upgrade script generation.

 * <p>
 * Typical workflow:
 * <pre>{@code
 * EntityToYamlConverter<MyEntity, MyDTO> converter = ...;
 * converter.addToZip(entity, zipOutputStream, existingEntries);
 * }</pre>

 * <p>
 * <b>Thread Safety:</b> Implementations are responsible for handling concurrency when multiple
 * entities are processed in parallel. Callers should consult the zipEntries Set to avoid duplicate
 * ZIP entries during concurrent operations.

 *
 * @param <T> the entity type to be converted (e.g., Organization, User, Form)
 * @param <D> the DTO type produced during conversion (e.g., OrganizationDto, UserDto)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see EntityToYamlConverterFactory
 * @see ZipOutputStream
 */
public interface EntityToYamlConverter<T, D> {

    /**
     * Serializes the given entity to YAML format and writes it to the provided ZIP output stream.
     * <p>
     * This method converts the entity into its DTO representation, serializes it to YAML, and adds
     * a corresponding ZIP entry to the output stream. Implementations must consult the zipEntries
     * Set to avoid creating duplicate entries when multiple entities reference the same artifact.

     * <p>
     * ZIP entry names typically follow the pattern: {@code entities/<entity-type>/<entity-id>.yaml}

     *
     * @param entity the domain entity to serialize (must not be null)
     * @param zipOut the ZIP output stream to write serialized data to (must be open and writable)
     * @param zipEntries the set of existing ZIP entry names to prevent duplicates (must not be null)
     * @return the DTO representation of the entity after serialization
     */
    D addToZip(T entity, ZipOutputStream zipOut, Set<String> zipEntries);
    
    /**
     * Persists the exported YAML artifacts for the given entity to durable storage.
     * <p>
     * This method writes the entity's YAML representation to the filesystem or other persistent
     * storage medium, updating the entity's metadata to reference the saved files. The method
     * returns the saved or updated entity instance with any changes to file paths or export
     * timestamps reflected in its state.

     * <p>
     * Storage location is typically determined by application configuration properties such as
     * {@code FILE_STORAGE_FILESYSTEM_PATH}.

     *
     * @param entity the entity whose export artifacts should be persisted (must not be null)
     * @return the saved or updated entity instance with file references populated
     */
    T saveToFile(T entity);
    
    /**
     * Removes persisted export artifacts associated with the given entity from durable storage.
     * <p>
     * This method deletes YAML files and related export artifacts from the filesystem or storage
     * backend, cleaning up resources after export operations are no longer needed. The entity's
     * metadata is updated to reflect the removal of file references.

     * <p>
     * This operation is typically invoked during entity deletion workflows or when re-exporting
     * an entity requires clearing previous export artifacts.

     *
     * @param entity the entity whose export artifacts should be removed (must not be null)
     * @return the entity instance with file references cleared
     */
    T removeExportedFiles(T entity);
    
    /**
     * Optional hook for appending database upgrade SQL fragments to the provided list.
     * <p>
     * This method allows converter implementations to contribute SQL statements that should be
     * executed during database schema upgrades when importing entities. The default implementation
     * is a no-op, returning without modification to the dbUpgradeEntries list.

     * <p>
     * Implementations that require database schema changes (e.g., creating tables for dynamic
     * entities, adding columns, or modifying constraints) should override this method and append
     * appropriate SQL fragments to dbUpgradeEntries.

     * <p>
     * SQL fragments are typically processed by {@code DbVersionService} during the upgrade workflow.

     *
     * @param entity the entity being exported/imported that may require schema changes (must not be null)
     * @param dbUpgradeEntries the mutable list to which SQL upgrade statements should be appended (must not be null)
     * @see com.openkoda.service.upgrade.DbVersionService
     */
    default void getUpgradeScript(T entity, List<String> dbUpgradeEntries) {
        return;
    }

}
