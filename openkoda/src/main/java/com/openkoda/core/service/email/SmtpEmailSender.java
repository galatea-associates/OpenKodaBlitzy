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

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.openkoda.model.EmailConfig;
import com.openkoda.model.file.File;

import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import jakarta.servlet.ServletContext;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

/**
 * Production SMTP EmailSender implementation using JavaMailSender and standard SMTP protocol.
 * <p>
 * This service extends {@link EmailSender} abstract base and provides enterprise-grade email delivery
 * via Spring's {@link JavaMailSender} and {@link MimeMessage}/{@link MimeMessageHelper} for email composition.
 * Registered as Spring {@code @Service} bean with no profile restriction - can be used as default EmailSender
 * or activated explicitly in application configuration.
 * </p>
 * <p>
 * <b>Key Features:</b>
 * <ul>
 * <li>Per-organization email configuration with from/replyTo overrides via {@link EmailConfig}</li>
 * <li>UTF-8 MimeMessage composition with multipart support for attachments</li>
 * <li>URL-based and {@link File} entity attachment handling</li>
 * <li>Asynchronous email transport via {@link CompletableFuture}</li>
 * <li>Transaction isolation using {@code @Transactional(REQUIRES_NEW)} for sending</li>
 * </ul>
 * </p>
 * <p>
 * <b>Configuration:</b> SMTP settings configured in application properties (host, port, username, password, etc.).
 * Activated when JavaMailSender bean is available. Logo path customizable via {@code application.logo} property.
 * </p>
 * <p>
 * <b>Transaction Pattern:</b> The sendEmail() method composes the message synchronously then schedules
 * asynchronous transport via self-proxy invocation of sendEmailViaMailSender() to enable AOP transaction
 * handling isolated from caller's transaction context.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @since 1.7.1
 * @see EmailSender
 * @see JavaMailSender
 * @see MimeMessage
 * @see EmailConfig
 */
@Service
public class SmtpEmailSender extends EmailSender {

    /**
     * Application logo path injected from {@code application.logo} property.
     * <p>
     * Default value: {@code /vendor/swagger-ui/springfox-swagger-ui/favicon-32x32.png}
     * </p>
     * Used for email branding and visual identity in email templates.
     */
    @Value("${application.logo:/vendor/swagger-ui/springfox-swagger-ui/favicon-32x32.png}")
    String appLogoPath;

    /**
     * Servlet context for accessing web application resources.
     * Injected via {@code @Inject} for resource resolution and context information.
     */
    @Inject
    ServletContext context;

    /**
     * Core Spring mail transport for SMTP email delivery.
     * <p>
     * Configured via Spring Boot auto-configuration with SMTP properties from application configuration.
     * Handles MimeMessage transmission to SMTP server.
     * </p>
     */
    @Inject
    private JavaMailSender mailSender;

    /**
     * Spring MessageSource for internationalization and localization.
     * Used for resolving localized email content and messages.
     */
    @Inject
    private MessageSource messageSource;
    
    /**
     * Self-reference proxy for enabling AOP transaction handling.
     * <p>
     * Injected instance is a Spring AOP proxy that intercepts method calls to apply
     * {@code @Transactional} annotations. Direct invocation of transactional methods
     * bypasses AOP; invoking via this proxy ensures transaction demarcation.
     * </p>
     * Used in sendEmail() to invoke sendEmailViaMailSender() with REQUIRES_NEW transaction isolation.
     */
    @Inject SmtpEmailSender self;

    /**
     * Sends HTML email with optional attachments via SMTP protocol.
     * <p>
     * <b>Implementation Details:</b>
     * <ol>
     * <li>Resolves {@link EmailConfig} from repository for per-organization from/replyTo address overrides</li>
     * <li>Composes UTF-8 {@link MimeMessage} via {@link MimeMessageHelper} with multipart support</li>
     * <li>Sets subject, from (with EmailConfig override), replyTo, to, and HTML text content</li>
     * <li>Handles {@code attachmentURL} by calling prepareTempAttachmentFile() then adding FileSystemResource with MimeUtility-encoded filename</li>
     * <li>Processes {@code attachments} File list by reading content streams into ByteArrayResource and adding to message with filename and content-type</li>
     * <li>Schedules asynchronous transport via CompletableFuture.runAsync() delegating to proxied sendEmailViaMailSender() for transaction isolation</li>
     * </ol>
     * </p>
     * <p>
     * <b>Error Handling:</b> Catches MessagingException during composition and MailException during send.
     * All errors are logged; method always returns true to indicate processing attempted.
     * </p>
     * <p>
     * <b>Async Execution:</b> Email sending occurs asynchronously in separate thread to avoid blocking caller.
     * Transaction isolation via REQUIRES_NEW ensures email persistence independent of caller's transaction.
     * </p>
     *
     * @param fullFrom sender email address (may be overridden by EmailConfig.from if configured)
     * @param fullTo recipient email address
     * @param subject email subject line
     * @param html HTML content for email body (UTF-8 encoded)
     * @param attachmentURL optional URL to file for attachment (downloaded to temp file then attached), may be null
     * @param attachments optional list of File entities to attach (content streams read and attached), may be null or empty
     * @return always returns true indicating email processing attempted (errors logged, not thrown)
     * @see EmailConfig
     * @see #sendEmailViaMailSender(String, MimeMessage)
     * @see #prepareTempAttachmentFile(String)
     */
    @Override
    public boolean sendEmail(String fullFrom, String fullTo, String subject, String html, String attachmentURL, List<File> attachments) {
        debug("[sendEmail] {} -> {} Subject: {}", fullFrom, fullTo, subject);
        try {
            EmailConfig emailConfig = emailConfigRepository.findAll().stream().findFirst().orElse(null);

            final MimeMessage mimeMessage = mailSender.createMimeMessage();
            final MimeMessageHelper message = new MimeMessageHelper( mimeMessage , true , "UTF-8" );
            message.setSubject( subject );
            message.setFrom( StringUtils.defaultIfBlank(emailConfig != null ? emailConfig.getFrom() : null, fullFrom) );
            message.setReplyTo( StringUtils.defaultIfBlank(emailConfig != null ? emailConfig.getReplyTo() : null, replyTo) );
            message.addTo( fullTo );
            message.setText( html , true );

            if ( StringUtils.isNotBlank( attachmentURL ) ) {
                try {
                    Path tmpFile = prepareTempAttachmentFile(attachmentURL);
                    if (tmpFile != null) {
                        message.addAttachment( MimeUtility.encodeText( "attachment" , "UTF-8" , null ) , new FileSystemResource(tmpFile.toFile()) );
                    }
                } catch (UnsupportedEncodingException e) {
                    warn("[sendEmail]", e);
                }
            }

            if (attachments != null) {
                for (File f: attachments) {
                    try {
                        byte[] bytes = IOUtils.toByteArray(f.getContentStream());
                        ByteArrayResource r = new ByteArrayResource(bytes, f.getFilename());
                        message.addAttachment(f.getFilename(), r, f.getContentType());
                    } catch (Exception e) {
                        error(e, "Couldn't attach {} to email", f.getFilename());
                    }
                }
            }

            // invoke method via 'self' instance in order to handle this call by Spring's
            // AOP which handles Transactional annotation
            CompletableFuture.runAsync(() -> self.sendEmailViaMailSender(fullTo, mimeMessage));
        } catch (MessagingException e) {
            error("[sendEmail] Error sending email to {} : {}", fullTo, e);
        }
        return true;
    }

    /**
     * Sends the composed MimeMessage via JavaMailSender with transaction isolation.
     * <p>
     * <b>Transaction Isolation:</b> Annotated with {@code @Transactional(REQUIRES_NEW)} to execute
     * email sending in a new transaction separate from the caller's transaction context.
     * This ensures email delivery persistence is independent and prevents rollback propagation.
     * </p>
     * <p>
     * <b>AOP Invocation Pattern:</b> This method must be invoked via the {@code self} proxy reference
     * (not directly via {@code this}) to enable Spring AOP transaction handling. Direct invocation
     * bypasses the AOP proxy and ignores the @Transactional annotation.
     * </p>
     * <p>
     * <b>Implementation:</b>
     * <ul>
     * <li>Invokes {@code mailSender.send(mimeMessage)} to transmit via SMTP</li>
     * <li>Catches {@link MailException} and logs errors without throwing</li>
     * <li>Logs successful send at INFO level with recipient address</li>
     * </ul>
     * </p>
     *
     * @param fullTo recipient email address for logging purposes
     * @param mimeMessage fully composed {@link MimeMessage} ready for transmission
     * @see JavaMailSender#send(MimeMessage)
     */
    @Transactional(value = TxType.REQUIRES_NEW)
    void sendEmailViaMailSender(String fullTo, final MimeMessage mimeMessage) {
        try {
            mailSender.send( mimeMessage );
        } catch (MailException e) {
            error( "[sendEmail] Error sending email to {} : {}", fullTo, e );

        }
        info( "[sendEmail] Mail to {} sent", fullTo );
    }



}
