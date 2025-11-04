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

package com.openkoda.service.csv;

import com.openkoda.core.service.FileService;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.sql.rowset.serial.SerialBlob;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CSV file assembly service for generating comma-separated value exports.
 * <p>
 * This utility service provides functionality for creating CSV files from entity collections
 * with database-backed file storage integration. The service generates CSV-formatted content
 * from structured data and persists it as File entities using SerialBlob for binary storage.

 * <p>
 * Core functionality includes:
 * <ul>
 *   <li>CSV file assembly with automatic .csv extension handling</li>
 *   <li>Optional header row insertion for column labeling</li>
 *   <li>Database-backed storage via SerialBlob for generated content</li>
 *   <li>File model integration with MIME type and size tracking</li>
 * </ul>

 * <p>
 * Thread-safety notes: This service is stateless and thread-safe at the service level.
 * However, the createCSVByte method mutates the input data list by inserting headers
 * at index 0, requiring callers to be aware of this side-effect when sharing data
 * structures across threads.

 * <p>
 * Use cases: Data export workflows, report downloads, bulk data operations, and
 * administrative data extraction.

 * <p>
 * Current limitations: This implementation provides basic CSV generation without
 * RFC 4180 compliance. Special characters (commas, quotes, newlines) within cell
 * values are not escaped or quoted, which may result in malformed CSV output for
 * complex data. Null values are skipped in output. Character encoding uses platform
 * default via String.getBytes() without explicit charset specification.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.file.File
 * @see com.openkoda.core.service.FileService.StorageType
 */
@Service
public class CsvService implements LoggingComponentWithRequestId {

    /**
     * Creates a CSV File entity with database-backed content from structured data.
     * <p>
     * This method constructs a complete File entity populated with CSV-formatted content,
     * automatically appending the .csv extension if not present in the filename. The
     * generated content is stored as a SerialBlob for database persistence, with the
     * File entity configured for database storage type and text/csv MIME type.

     * <p>
     * The method delegates to createCSVByte for CSV serialization, which inserts the
     * provided headers as the first row in the data list. The resulting byte array
     * is wrapped in a SerialBlob and attached to the File entity along with size metadata.

     * <p>
     * Example usage in a controller:
     * <pre>{@code
     * List<List<Object>> data = Arrays.asList(Arrays.asList("John", 30), Arrays.asList("Jane", 25));
     * File csvFile = csvService.createCSV("users", data, "Name", "Age");
     * }</pre>

     *
     * @param filename the CSV filename (automatically appends .csv extension if missing)
     * @param data mutable list of rows where each row is a List of Object cell values;
     *             note that this list is modified by inserting headers at index 0
     * @param headers optional CSV column headers to be inserted as the first row;
     *                if no headers provided, the data is exported without a header row
     * @return populated File entity configured with MIME type "text/csv", StorageType.database,
     *         SerialBlob content, and computed file size
     * @throws IOException if CSV byte assembly encounters I/O errors during stream operations
     *                     (handled internally with fallback to empty byte array)
     * @throws SQLException if SerialBlob construction fails due to database driver issues
     */
    public com.openkoda.model.file.File createCSV(String filename, List<List<Object>> data, String... headers) throws IOException, SQLException {
        debug("[createCSV]");
        com.openkoda.model.file.File csvFile = new com.openkoda.model.file.File(
                StringUtils.endsWith(filename, ".csv") ? filename : filename + ".csv",
                "text/csv",
                FileService.StorageType.database
        );
        byte[] csvByte = createCSVByte(data, headers);
        csvFile.setContent(new SerialBlob(csvByte));
        csvFile.setSize(csvByte.length);
        return csvFile;
    }

    /**
     * Converts structured data to CSV-formatted byte array with optional headers.
     * <p>
     * This private helper method performs the core CSV serialization algorithm by:
     * <ol>
     *   <li>Inserting the provided headers as the first row in the data list (mutates input)</li>
     *   <li>Converting each row to comma-separated format via convertToCSVRow</li>
     *   <li>Joining rows with newline separators (\n)</li>
     *   <li>Encoding the resulting CSV string to bytes using platform default charset</li>
     * </ol>

     * <p>
     * Important side-effect: This method modifies the input data list by inserting
     * headers at index 0. Callers should be aware that the data parameter is mutated
     * during execution and will contain the header row upon return.

     * <p>
     * Error handling: IOException during ByteArrayOutputStream operations is caught
     * and logged, with an empty byte array returned as fallback. This ensures the
     * method always returns a valid byte array even in failure scenarios.

     * <p>
     * Character encoding note: Uses String.getBytes() without explicit charset parameter,
     * relying on the platform default encoding. For cross-platform compatibility,
     * UTF-8 encoding should be considered in future enhancements.

     *
     * @param data mutable list of rows where each row is a List of Object cell values;
     *             this list is modified by inserting headers at index 0 as a side-effect
     * @param headers CSV column headers to be inserted as the first row in the output;
     *                converted to List and prepended to the data structure
     * @return UTF-8 encoded CSV bytes representing all rows with newline separators,
     *         or empty byte array if I/O errors occur during serialization
     */
    private byte[] createCSVByte(List<List<Object>> data, String... headers) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            data.add(0, Arrays.asList(headers));
            String csvContent = data.stream()
                    .map(this::convertToCSVRow)
                    .collect(Collectors.joining("\n"));
            outputStream.write(csvContent.getBytes());

            return outputStream.toByteArray();

        } catch (IOException e) {
            error("[createCSVByte]", e);
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
    }

    /**
     * Converts a single row of data to comma-separated CSV format.
     * <p>
     * This private helper method serializes one row of data by concatenating cell
     * values with comma separators. Each cell value is converted to its string
     * representation via Object.toString(), with null values skipped entirely
     * (resulting in empty cells between commas).

     * <p>
     * The algorithm iterates through the row data, prepending a comma before each
     * cell value except the first, and appending non-null cell values to a
     * StringBuilder for efficient string construction.

     * <p>
     * Important limitations of this implementation:
     * <ul>
     *   <li>No RFC 4180 compliance: special characters are not escaped</li>
     *   <li>Commas within cell values will break CSV structure (not quoted)</li>
     *   <li>Quotes within cell values are not escaped with double-quotes</li>
     *   <li>Newlines within cell values will create malformed CSV output</li>
     *   <li>Null values are skipped, resulting in empty cells in output</li>
     * </ul>

     * <p>
     * For production use with complex data containing special characters, consider
     * using a standards-compliant CSV library such as Apache Commons CSV or OpenCSV.

     *
     * @param rowData list of cell values for one CSV row, where each Object is
     *                converted to string via toString() method; null values are
     *                skipped and result in empty cells in the CSV output
     * @return comma-separated string representation of the row with no quoting
     *         or escaping applied; empty string if rowData is empty
     */
    private String convertToCSVRow(List<Object> rowData) {
        StringBuilder csvRow = new StringBuilder();
        for (int i = 0; i < rowData.size(); i++) {
            if (i > 0) {
                csvRow.append(",");
            }
            if(rowData.get(i) != null) {
                csvRow.append(rowData.get(i));
            }
        }
        return csvRow.toString();
    }
}
