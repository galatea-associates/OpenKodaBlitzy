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

import com.openkoda.model.file.File;
import jakarta.inject.Inject;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import java.util.List;

/**
 * Testing and development EmailSender implementation that writes email details to System.out/console
 * instead of actually sending emails.
 * <p>
 * This class provides a safe email sender for local development and testing environments that logs
 * complete email message details (sender, recipient, subject, HTML content, attachments) to the console
 * without performing any actual email delivery. This prevents accidental email delivery to real
 * recipients during development, debugging, or automated testing.

 * <p>
 * The sender is activated only when the Spring "local" profile is active ({@code @Profile("local")}),
 * ensuring that real email delivery mechanisms are used in production environments. When active, the
 * {@code @Primary} annotation makes this implementation the default EmailSender bean, taking precedence
 * over production senders like SmtpEmailSender or MailgunEmailSender.

 * <p>
 * This implementation extends the {@link EmailSender} abstract base class and overrides the
 * {@link #sendEmail(String, String, String, String, String, List)} method to log email details
 * via {@code info()} logging instead of delivering messages. All calls return {@code true} to
 * simulate successful delivery, allowing the email sending workflow to complete normally.

 * <p>
 * Usage notes:
 * <ul>
 * <li>Activated by including "local" in {@code spring.profiles.active} property</li>
 * <li>Useful for testing email composition without SMTP configuration</li>
 * <li>Logs full email content for visual verification in development</li>
 * <li>No actual network I/O or external service dependencies</li>
 * <li>Always returns {@code true} to simulate successful delivery</li>
 * </ul>

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @since 1.7.1
 * @see EmailSender
 * @see SmtpEmailSender
 * @see MailgunEmailSender
 */
@Service
@Profile("local")
@Primary
public class FakeSysoutEmailSender extends EmailSender {

    /**
     * Thymeleaf template engine injected for potential template rendering operations.
     * <p>
     * This dependency is currently unused in the fake sender implementation but is available
     * if template processing is needed for enhanced logging or debugging of email templates
     * during development.

     */
    @Inject
    private TemplateEngine templateEngine;

    /**
     * Logs email details to console instead of sending actual email.
     * <p>
     * This method simulates email sending by logging all message details to the application log
     * at INFO level. It writes two log entries: a compact summary showing sender, recipient, and
     * subject, followed by a detailed multi-line log containing the full HTML content and
     * attachment URL. No actual email delivery occurs, and no network connections are established.

     * <p>
     * The method always returns {@code true} to indicate "successful" send, allowing the email
     * sending workflow (Email entity lifecycle transitions, job completion) to proceed normally
     * as if real delivery had occurred.

     *
     * @param fullFrom the sender email address (e.g., "noreply@example.com" or "App Name &lt;noreply@example.com&gt;")
     * @param fullTo the recipient email address (e.g., "user@example.com" or "User Name &lt;user@example.com&gt;")
     * @param subject the email subject line
     * @param html the HTML email content body
     * @param attachmentUrl optional URL of an attachment to be included (may be null if no URL-based attachment)
     * @param attachments optional list of {@link File} entities representing file attachments (may be null or empty)
     * @return always returns {@code true} to simulate successful email delivery
     */
    @Override
    public boolean sendEmail(String fullFrom, String fullTo, String subject, String html, String attachmentUrl, List<File> attachments) {
        info("[sendEmail] from {} to {} subject: {}", fullFrom, fullTo, subject);
        info(String.format("%s -> %s\nSubject:%s\n%s\nAttachment: %s",
                fullFrom, fullTo, subject, html, attachmentUrl));
        return true;
    }

}
