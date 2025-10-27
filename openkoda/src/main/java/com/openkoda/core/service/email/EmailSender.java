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

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.file.File;
import com.openkoda.model.task.Email;
import com.openkoda.repository.EmailConfigRepository;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Abstract base class providing lifecycle and orchestration management for email delivery implementations.
 * <p>
 * This class serves as the foundation for all email sending mechanisms in OpenKoda, managing the complete
 * email delivery lifecycle including start, fail, and complete state transitions. It implements the template
 * method pattern where {@link #sendMail(Email)} orchestrates the email sending workflow while delegating
 * the actual transport mechanism to subclass-specific implementations via {@link #sendEmail}.
 * </p>
 * <p>
 * The email sending lifecycle is managed as follows:
 * <ul>
 *   <li>Invokes {@link Email#start()} to mark email as in-progress</li>
 *   <li>Records sender class name via {@link Email#setSender(String)}</li>
 *   <li>Delegates to abstract {@link #sendEmail} method implemented by subclasses</li>
 *   <li>Invokes {@link Email#complete()} on successful delivery</li>
 *   <li>Invokes {@link Email#fail()} if exceptions occur during sending</li>
 * </ul>
 * </p>
 * <p>
 * Subclasses must implement {@link #sendEmail} to provide the actual email transport mechanism
 * (SMTP, REST API, message queue, etc.). All error handling and lifecycle state management is
 * handled by this base class.
 * </p>
 * <p>
 * Configuration is managed through Spring @Value properties for default sender addresses and
 * per-organization SMTP configuration via {@link EmailConfigRepository}.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @since 1.7.1
 */
public abstract class EmailSender implements LoggingComponentWithRequestId {

    /**
     * Default mail sender address used when email does not specify a from address.
     * <p>
     * Configured via {@code mail.from} property in application configuration.
     * Defaults to empty string if not configured.
     * </p>
     */
    @Value("${mail.from:}")
    protected String mailFrom;

    /**
     * Reply-to address used in sent emails to direct responses.
     * <p>
     * Configured via {@code mail.replyTo} property in application configuration.
     * Defaults to empty string if not configured.
     * </p>
     */
    @Value("${mail.replyTo:}")
    String replyTo;

    /**
     * Repository providing access to per-organization SMTP configuration entities.
     * <p>
     * Enables subclass implementations to retrieve organization-specific email server
     * settings, credentials, and connection parameters for multi-tenant email delivery.
     * </p>
     */
    @Inject protected EmailConfigRepository emailConfigRepository;
    
    /**
     * Orchestrates the complete email sending lifecycle with state management and error handling.
     * <p>
     * This method manages the email delivery workflow by transitioning the email through its
     * lifecycle states (started, completed, or failed) and delegating the actual transport
     * mechanism to the abstract {@link #sendEmail} method implemented by subclasses.
     * </p>
     * <p>
     * Lifecycle sequence:
     * <ol>
     *   <li>Invokes {@link Email#start()} to mark email as in-progress</li>
     *   <li>Records sender implementation class name via {@link Email#setSender(String)}</li>
     *   <li>Calls {@link #sendEmail} with prepared email parameters</li>
     *   <li>On success: Invokes {@link Email#complete()} to mark delivery successful</li>
     *   <li>On exception: Invokes {@link Email#fail()} to mark delivery failed and logs error</li>
     * </ol>
     * </p>
     * <p>
     * Error handling: All exceptions during email sending are caught, logged via {@code error()},
     * and result in the email being marked as failed. The email object is always returned with
     * its updated lifecycle state.
     * </p>
     *
     * @param email the email entity containing recipient, subject, content, and attachments
     * @return the same email object with updated lifecycle state (completed or failed)
     */
    public Email sendMail(Email email) {
        debug("[sendMail] {}", email);
        try {
            debug("[sendMail] {}", email);
            email.start();
            email.setSender(getClass().getSimpleName());
            if (sendEmail(email.getFullFrom(mailFrom), email.getFullTo(), email.getSubject(), email.getContent(),
                    email.getAttachmentURL(), email.getFiles())) {
                email.complete();
            }
        } catch (Exception e) {
            error(e, "[sendMail] {}", email);
            email.fail();
        }
        return email;
    }

    /**
     * Abstract method that subclasses must implement to provide the actual email transport mechanism.
     * <p>
     * This method is invoked by {@link #sendMail(Email)} with prepared email parameters and is
     * responsible for the concrete implementation of email delivery (SMTP, REST API, message queue, etc.).
     * Implementations should not manage lifecycle state transitions as this is handled by the base class.
     * </p>
     * <p>
     * Subclass responsibilities:
     * <ul>
     *   <li>Establish connection to email transport service</li>
     *   <li>Construct email message with provided parameters</li>
     *   <li>Handle attachments from URL or File entities</li>
     *   <li>Perform actual email transmission</li>
     *   <li>Return success status (exceptions are caught by base class)</li>
     * </ul>
     * </p>
     *
     * @param fullFrom the complete sender address with optional display name (e.g., "Name &lt;email@example.com&gt;")
     * @param fullTo the complete recipient address with optional display name
     * @param subject the email subject line
     * @param html the email body content in HTML format
     * @param attachmentURL optional URL to download attachment (may be null)
     * @param attachments optional list of File entities to attach (may be null or empty)
     * @return {@code true} if email was successfully sent, {@code false} otherwise
     */
    public abstract boolean sendEmail(String fullFrom, String fullTo, String subject, String html, String attachmentURL, List<File> attachments);


    /**
     * Downloads an attachment from a URL and creates a temporary file for email attachment.
     * <p>
     * This utility method performs synchronous HTTP download of the attachment using {@link RestTemplate},
     * extracts the filename from the Content-Disposition header, and writes the content to a temporary
     * file created via {@link Files#createTempFile(String, String)}.
     * </p>
     * <p>
     * Implementation details:
     * <ol>
     *   <li>Returns null immediately if attachmentURL is blank</li>
     *   <li>Performs GET request via RestTemplate to download attachment bytes</li>
     *   <li>Parses Content-Disposition header to extract filename</li>
     *   <li>Separates filename and extension for temp file creation</li>
     *   <li>Creates temporary file with original filename and extension</li>
     *   <li>Writes downloaded bytes to temporary file</li>
     * </ol>
     * </p>
     * <p>
     * Error handling: Any IOException during download or file creation is caught, logged via
     * {@code error()}, and results in null return value.
     * </p>
     *
     * @param attachmentURL the URL to download the attachment from (may be null or blank)
     * @return Path to the created temporary file, or null if URL is blank or download fails
     */
    protected Path prepareTempAttachmentFile(String attachmentURL) {
        debug("[prepareTempAttachmentFile] {}", attachmentURL);
        if (StringUtils.isBlank(attachmentURL)) {
            return null;
        }
        Path tmpFile = null;
        try {
            RestTemplate rTemplate = new RestTemplate();
            ResponseEntity<byte[]> tmp = rTemplate.getForEntity(attachmentURL, byte[].class);
            String fileName = StringUtils.substringAfterLast(tmp.getHeaders().get("Content-Disposition").get(0), "=");
            String extension = "." + StringUtils.substringAfterLast(fileName, ".");
            fileName = StringUtils.substringBeforeLast(fileName, ".");
            tmpFile = Files.createTempFile(fileName, extension);
            Files.write(tmpFile, tmp.getBody());
        } catch (IOException e) {
            error(e, "Error while creating temporary attachment for email from url({}):", attachmentURL);
        }
        return tmpFile;
    }
}
