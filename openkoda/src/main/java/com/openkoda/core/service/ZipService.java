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

package com.openkoda.core.service;

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.component.FrontendResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * In-memory ZIP archive assembly service for creating compressed archives.
 * <p>
 * Creates compressed ZIP archives from FrontendResource content, byte arrays, or InputStreams.
 * All operations use {@link ByteArrayOutputStream} for memory-resident zipping, making this service
 * suitable for moderate-sized content that can fit in heap memory.

 * <p>
 * This service implements {@link LoggingComponentWithRequestId} for request tracking and correlation
 * across distributed operations. All ZIP operations are logged with request IDs for traceability.

 * <p>
 * <b>Warning:</b> Large resources consume memory proportional to their size. Consider streaming
 * approaches for multi-megabyte archives to avoid heap exhaustion.

 * <p>
 * Example usage:
 * <pre>{@code
 * byte[] zipBytes = zipService.zipByteArray(content, "export.txt").toByteArray();
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FrontendResource
 * @see ZipOutputStream
 * @see ByteArrayOutputStream
 * @see LoggingComponentWithRequestId
 */
@Service
public class ZipService implements LoggingComponentWithRequestId {

    /**
     * Creates a ZIP archive containing HTML files from a list of FrontendResource objects.
     * <p>
     * This method processes each resource in the list and creates corresponding HTML entries in the ZIP archive.
     * Filenames are normalized by replacing spaces with underscores and appending the {@code .html} extension.
     * If a resource has draft content, an additional entry prefixed with {@code draft_} is included.

     * <p>
     * The method uses the platform default charset for encoding content. All content is written to an
     * in-memory {@link ByteArrayOutputStream}, which is returned even if an {@link IOException} occurs during
     * processing. Partial content may be present in the returned stream if an error occurs mid-operation.

     * <p>
     * <b>Note:</b> IOExceptions are caught and logged but not rethrown. The method returns the populated
     * {@link ByteArrayOutputStream} even on partial failure, which may contain incomplete ZIP data.

     *
     * @param resources the list of {@link FrontendResource} objects to include in the ZIP archive. Each resource's
     *                  content is written as a separate HTML file. Must not be null.
     * @return a {@link ByteArrayOutputStream} containing the ZIP archive content. Returns an empty or partially
     *         populated stream if an IOException occurs during processing.
     * @see FrontendResource
     * @see ZipOutputStream
     */
    public ByteArrayOutputStream zipFrontendResources(List<FrontendResource> resources) {
        debug("[zipFrontendResources]");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (FrontendResource resource : resources) {
                String fileName = resource.getName().replace(" ", "_") + ".html";
                zos.putNextEntry(new ZipEntry(fileName));
                zos.write(resource.getContent().getBytes(Charset.defaultCharset()));
                zos.closeEntry();
                if(resource.getDraftContent() != null) {
                    zos.putNextEntry(new ZipEntry("draft_" + fileName));
                    zos.write(resource.getDraftContent().getBytes(Charset.defaultCharset()));
                    zos.closeEntry();
                }
            }

        } catch (IOException e) {
            error("[zipFrontendResources]", e);
        }
        return baos;
    }

    /**
     * Creates a single-entry ZIP archive from an InputStream with the specified filename.
     * <p>
     * This method reads data from the provided {@link InputStream} in chunks and writes it to a ZIP entry.
     * The implementation uses a 1KB buffer for chunked reading, repeatedly checking {@code available()} and
     * writing blocks until the stream is exhausted. The {@code closeEntry()} method is called once after
     * the reading loop completes.

     * <p>
     * <b>Potential Issue:</b> The current implementation uses {@code read(byte[])} which returns the number
     * of bytes read, but this value is written directly to the ZIP stream. This may cause issues if the
     * actual bytes read differ from the buffer size.

     * <p>
     * IOExceptions are caught and logged but not rethrown. The method returns the {@link ByteArrayOutputStream}
     * even on failure, which may contain incomplete or empty ZIP data.

     *
     * @param bis the {@link InputStream} to read data from. Must not be null. The stream is not closed by this method.
     * @param inputFileName the name to use for the entry in the ZIP archive. Must not be null or empty.
     * @return a {@link ByteArrayOutputStream} containing the ZIP archive content. Returns an empty or partially
     *         populated stream if an IOException occurs during processing.
     * @see InputStream
     * @see ZipOutputStream
     * @see ZipEntry
     */
    public ByteArrayOutputStream zipByteInput(InputStream bis, String inputFileName) {
        debug("[zipByteInput] inputFileName: {}", inputFileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            int size = bis.available();
            while (size > 0) {
                size = size > 1024 ? 1024 : size;
                byte[] data = new byte[size];
                zos.putNextEntry(new ZipEntry(inputFileName));
                zos.write(bis.read(data));
                size = bis.available();
            }
            zos.closeEntry();


        } catch (IOException e) {
            error("[zipByteInput]", e);
        }
        return baos;
    }

    /**
     * Creates a single-entry ZIP archive from a byte array with the specified filename.
     * <p>
     * This is the simplest and most reliable method for creating ZIP archives from in-memory data.
     * The entire byte array is written to the ZIP entry in one operation, making it efficient and
     * predictable for data that is already loaded in memory.

     * <p>
     * IOExceptions are caught and logged but not rethrown. The method returns the {@link ByteArrayOutputStream}
     * even on failure, which may contain incomplete or empty ZIP data.

     *
     * @param data the byte array to write to the ZIP archive. Must not be null.
     * @param inputFileName the name to use for the entry in the ZIP archive. Must not be null or empty.
     * @return a {@link ByteArrayOutputStream} containing the ZIP archive content. Returns an empty or partially
     *         populated stream if an IOException occurs during processing.
     * @see ZipOutputStream
     * @see ZipEntry
     */
    public ByteArrayOutputStream zipByteArray(byte[] data, String inputFileName) {
        debug("[zipByteArray] inputFileName: {}", inputFileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(inputFileName));
            zos.write(data);
            zos.closeEntry();
        } catch (IOException e) {
            error("[zipByteArray]", e);
        }
        return baos;
    }
}
