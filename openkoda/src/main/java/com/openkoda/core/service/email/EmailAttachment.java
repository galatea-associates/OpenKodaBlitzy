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

package com.openkoda.core.service.email;

/**
 * Contract for email attachment data providers.
 * <p>
 * This interface provides an abstraction for email attachments used by MIME builders
 * and mail composition code within the email subsystem. Implementations supply both
 * the attachment filename and the binary content payload.

 * <p>
 * Typical implementations include:
 * <ul>
 *   <li>File-backed attachments reading from disk or classpath resources</li>
 *   <li>In-memory byte arrays for dynamically generated content</li>
 *   <li>Database-backed attachments streaming from BLOB columns</li>
 * </ul>

 * <p>
 * <b>Memory Considerations:</b> Since {@link #getData()} returns the full byte array,
 * all attachment content is loaded into memory. For large attachments (e.g., multi-megabyte
 * PDFs or high-resolution images), this can result in significant heap pressure. Consider
 * attachment size limits and memory capacity when implementing this interface.

 * <p>
 * Used by {@link SmtpEmailSender}, {@link MailgunEmailSender}, and other email sender
 * implementations to attach files to outgoing email messages.

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @since 1.7.1
 * @see SmtpEmailSender
 * @see MailgunEmailSender
 */
public interface EmailAttachment {

   /**
    * Returns the attachment filename as it should appear in the email.
    * <p>
    * This name is used as the attachment filename in the MIME message headers
    * (e.g., Content-Disposition: attachment; filename="report.pdf"). The returned
    * value should include the file extension to ensure proper handling by email
    * clients.

    *
    * @return the attachment filename (e.g., "invoice-2023.pdf", "logo.png"),
    *         should not be null or empty
    */
   String getName();

   /**
    * Returns the byte array content of the attachment.
    * <p>
    * This method provides the complete binary payload of the attachment. The returned
    * byte array is used directly by MIME message builders to construct the attachment
    * part of the email.

    * <p>
    * <b>In-Memory Payload Implications:</b> Since this method returns a full byte array
    * rather than a stream, the entire attachment content must fit in memory. For large
    * attachments, this can lead to:
    * <ul>
    *   <li>Increased heap memory consumption</li>
    *   <li>Potential OutOfMemoryError for very large files</li>
    *   <li>Performance impact from byte array copying</li>
    * </ul>
    * Implementations should consider imposing size limits or providing streaming
    * alternatives for large file support.

    *
    * @return the attachment binary content, should not be null
    */
   byte[] getData();

}
