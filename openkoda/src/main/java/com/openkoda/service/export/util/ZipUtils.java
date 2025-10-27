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

package com.openkoda.service.export.util;

import com.openkoda.core.flow.LoggingComponent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Spring component providing ZIP entry construction, streaming helpers, and YAML filename conventions for component export operations.
 * <p>
 * This stateless singleton service is injected into export converters and provides methods to write text content,
 * files, and URL resources as ZIP entries with UTF-8 encoding. The {@code addToZipFile} method converts IOExceptions
 * to RuntimeException for error propagation, while streaming methods ({@code addFileToZip}, {@code addURLFileToZip})
 * log errors but do not propagate them. Organization-scoped YAML filename generation follows the convention:
 * filePath + entityName + [_orgId] + .yaml for multi-tenant component exports.
 * </p>
 * <p>
 * Thread-safety: Stateless but ZipOutputStream parameter is not thread-safe - callers must synchronize access to
 * shared ZipOutputStream instances.
 * </p>
 * <p>
 * Resource management: Methods do NOT close the provided ZipOutputStream - caller is responsible for stream lifecycle.
 * </p>
 * <p>
 * Exception handling: {@code addToZipFile} throws RuntimeException on failure; {@code addFileToZip} and
 * {@code addURLFileToZip} log and swallow IOExceptions (inconsistent error semantics - callers should verify ZIP integrity).
 * </p>
 *
 * @see com.openkoda.service.export.ComponentExportService
 * @see com.openkoda.service.export.converter.AbstractEntityToYamlConverter
 * @see com.openkoda.core.flow.LoggingComponent
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Component
public class ZipUtils implements LoggingComponent {
    /**
     * Writes text content as a ZIP entry with UTF-8 encoding, validates non-empty content, and converts IOExceptions to RuntimeException.
     * <p>
     * Creates a new ZipEntry with the specified name, writes UTF-8 encoded bytes if content passes StringUtils.hasText
     * validation, and closes the entry. This method propagates errors as RuntimeException with diagnostic message
     * including the entry name for troubleshooting failed exports.
     * </p>
     * <p>
     * Side effects: Creates ZipEntry, writes UTF-8 bytes to zipOut, closes entry via zipOut.closeEntry().
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * zipUtils.addToZipFile(yamlContent, "config/form.yaml", zipOut);
     * </pre>
     * </p>
     *
     * @param content text content to write; must pass StringUtils.hasText validation (not null/empty/whitespace-only)
     * @param entryName ZIP entry path/name (e.g., "components/form/MyForm.yaml")
     * @param zipOut open ZipOutputStream to write entry; caller must keep stream open
     * @throws RuntimeException wrapping IOException with diagnostic message including entryName
     */
    public void addToZipFile(String content, String entryName, ZipOutputStream zipOut){
        debug("[addToZipFile]");

        try {
            ZipEntry zipEntry = new ZipEntry(entryName);
            zipOut.putNextEntry(zipEntry);
            if(StringUtils.hasText(content)) {
                zipOut.write(content.getBytes(StandardCharsets.UTF_8));
            }
            zipOut.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException("Zip export of " + entryName + " failed");
        }
    }

    /**
     * Streams file contents into ZIP entry using 1024-byte buffer, logs IOExceptions but does NOT propagate them.
     * <p>
     * Creates a new ZipEntry, opens FileInputStream, and streams file contents in 1024-byte chunks. Error handling
     * logs IOExceptions via LoggingComponent.error but does NOT rethrow them - creating silent failure risk. The
     * implementation does not consistently call zipOut.closeEntry() in all control flows which may leave ZIP archive
     * malformed on error. Resource management closes FileInputStream on completion.
     * </p>
     * <p>
     * Buffer size: Fixed 1024 bytes.
     * </p>
     *
     * @param file File to read and add to ZIP; must be readable
     * @param entryName ZIP entry path/name
     * @param zipOut open ZipOutputStream to write entry
     */
    public void addFileToZip(File file, String entryName, ZipOutputStream zipOut){
        debug("[addFileToZip]");

        try {
            ZipEntry zipEntry = new ZipEntry(entryName);
            zipOut.putNextEntry(zipEntry);
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            int length;
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
        } catch (IOException e) {
            error("[addFileToZip]", e);
        }
    }

    /**
     * Streams URL resource contents (network or classpath) into ZIP entry using 1024-byte buffer, logs IOExceptions but does NOT propagate them.
     * <p>
     * Creates a new ZipEntry, opens InputStream from url.openStream() (supporting http://, file://, jar:// protocols),
     * and streams resource contents in 1024-byte chunks. Error handling logs IOExceptions but does NOT rethrow them -
     * creating silent failure risk. Resource management closes InputStream obtained from url.openStream() on completion.
     * </p>
     * <p>
     * Buffer size: Fixed 1024 bytes.
     * </p>
     *
     * @param url URL to resource (supports url.openStream() - typically http://, file://, jar:// protocols)
     * @param entryName ZIP entry path/name
     * @param zipOut open ZipOutputStream to write entry
     */
    public void addURLFileToZip(URL url, String entryName, ZipOutputStream zipOut){
        debug("[addURLFileToZip]");

        try {
            ZipEntry zipEntry = new ZipEntry(entryName);
            zipOut.putNextEntry(zipEntry);
            InputStream is = url.openStream();
            byte[] bytes = new byte[1024];
            int length;
            while((length = is.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            is.close();
        } catch (IOException e) {
            error("[addURLFileToZip]", e);
        }
    }

    /**
     * Constructs YAML filename with optional organization ID suffix following convention: filePath + entityName + [_orgId] + .yaml.
     * <p>
     * Generates organization-scoped YAML file paths for multi-tenant component exports. If organizationId is non-null,
     * appends _organizationId suffix to the entity name for tenant isolation. Logs debug message at invocation for
     * export operation diagnostics.
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * setResourceFilePath("components/form/", "ContactForm", 123L)
     * // returns "components/form/ContactForm_123.yaml"
     * </pre>
     * </p>
     *
     * @param filePath directory path prefix (typically from FolderPathConstants)
     * @param entityName component name (without extension)
     * @param organizationId optional tenant organization ID; if non-null, appends _organizationId suffix
     * @return complete YAML file path string (e.g., "components/form/ContactForm_123.yaml" or "components/form/ContactForm.yaml")
     */
    public String setResourceFilePath(String filePath, String entityName, Long organizationId){
        debug("[setResourceFilePath]");

        return organizationId == null ? String.format("%s%s.yaml",filePath, entityName) : String.format("%s%s_%s.yaml", filePath, entityName, organizationId);
    }
}
