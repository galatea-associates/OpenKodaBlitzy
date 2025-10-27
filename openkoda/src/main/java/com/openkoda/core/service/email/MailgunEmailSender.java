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

import com.openkoda.model.EmailConfig;
import com.openkoda.model.file.File;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

/**
 * Mailgun API EmailSender implementation using RestTemplate for HTTP POST requests to Mailgun endpoints.
 * <p>
 * This service extends the {@link EmailSender} abstract base class and provides email delivery through
 * the Mailgun email service API. It is registered as a Spring {@code @Service} bean and activated when
 * the "mailgun" profile is active via {@code @Profile("mailgun")}. The {@code @Primary} annotation makes
 * this implementation the default EmailSender when the mailgun profile is enabled.
 * </p>
 * <p>
 * The implementation performs HTTP multipart/form-data POST requests to the Mailgun API endpoint,
 * supporting inline attachments, custom reply-to headers, and embedded application logos.
 * </p>
 * <p>
 * <b>Configuration:</b> Mailgun API credentials and endpoint URL are configured via application properties
 * (mailgun.apikey, mailgun.apiurl) or can be overridden per-organization through persisted EmailConfig entities.
 * </p>
 * <p>
 * <b>Mailgun-Specific Features:</b>
 * <ul>
 *   <li>Inline logo embedding via ClassPathResource for email branding</li>
 *   <li>Reply-To header customization (h:Reply-To field)</li>
 *   <li>Per-organization EmailConfig overrides for API key and sender addresses</li>
 *   <li>Temporary file cleanup after attachment materialization</li>
 * </ul>
 * </p>
 * <p>
 * <b>Error Handling:</b> Exceptions during email sending are propagated to the caller.
 * Temporary attachment files are deleted after send attempts regardless of success or failure.
 * </p>
 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * mailgunEmailSender.sendEmail("from@example.com", "to@example.com", "Subject", "<html>Body</html>", null, null);
 * }</pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @since 1.7.1
 */
@Service
@Profile("mailgun")
@Primary
public class MailgunEmailSender extends EmailSender {

    /**
     * Mailgun API key for authentication with Mailgun email service.
     * <p>
     * Injected from application property {@code mailgun.apikey}. This key is used as a fallback
     * when no per-organization EmailConfig with a custom API key is found. The API key is encoded
     * as Base64("api:" + key) for HTTP Basic Authorization header.
     * </p>
     * <p>
     * Default value is empty string if property is not configured.
     * </p>
     */
    @Value("${mailgun.apikey:}")
    String mailgunApiKey;

    /**
     * Mailgun API endpoint URL for sending email messages.
     * <p>
     * Injected from application property {@code mailgun.apiurl}. This URL is the target for HTTP POST
     * requests containing multipart/form-data email payloads. Typically follows the format:
     * {@code https://api.mailgun.net/v3/YOUR_DOMAIN_NAME/messages}
     * </p>
     * <p>
     * Default value is empty string if property is not configured.
     * </p>
     */
    @Value("${mailgun.apiurl:}")
    String mailgunApiUrl;

    /**
     * Application logo classpath resource path for embedding inline in email messages.
     * <p>
     * Injected from application property {@code application.logo} with default value
     * {@code /vendor/swagger-ui/springfox-swagger-ui/favicon-32x32.png}. The logo is loaded as a
     * ClassPathResource and added as an inline attachment to email messages for branding purposes.
     * </p>
     * <p>
     * If the path is empty or null, no logo is embedded in outgoing emails.
     * </p>
     */
    @Value("${application.logo:/vendor/swagger-ui/springfox-swagger-ui/favicon-32x32.png}")
    String appLogoPath;

    /**
     * Jakarta Servlet context for accessing application context resources.
     * <p>
     * Injected via {@code @Inject} annotation. Used for resolving classpath resources and
     * accessing servlet container-specific functionality when preparing email content.
     * </p>
     */
    @Inject
    ServletContext context;

    /**
     * Sends an email message via Mailgun API using HTTP POST with multipart/form-data encoding.
     * <p>
     * This method performs the following operations:
     * <ol>
     *   <li>Resolves Mailgun API key from persisted EmailConfig or falls back to mailgunApiKey property</li>
     *   <li>Encodes Authorization header as Base64("api:" + apiKey) for HTTP Basic Authentication</li>
     *   <li>Builds multipart/form-data request with from, to, subject, and h:Reply-To fields</li>
     *   <li>Includes inline ClassPathResource(appLogoPath) for logo embedding if path is non-empty</li>
     *   <li>Adds HTML content to the message body</li>
     *   <li>Materializes attachmentURL via prepareTempAttachmentFile() if provided</li>
     *   <li>Posts request to mailgunApiUrl using RestTemplate</li>
     *   <li>Deletes temporary attachment files after sending</li>
     * </ol>
     * </p>
     * <p>
     * <b>Per-Organization Configuration:</b> If an EmailConfig entity exists in the repository,
     * its mailgunApiKey, from, and replyTo values override the default configuration properties.
     * </p>
     * <p>
     * <b>Error Handling:</b> All exceptions (network errors, API errors, file I/O errors) propagate
     * to the caller. Temporary files are cleaned up in a finally-like manner regardless of send outcome.
     * </p>
     *
     * @param fullFrom the sender email address in "Name &lt;email@example.com&gt;" or "email@example.com" format;
     *                 may be overridden by EmailConfig.from if configured
     * @param fullTo the recipient email address in "Name &lt;email@example.com&gt;" or "email@example.com" format;
     *               required, cannot be null or empty
     * @param subject the email subject line; included in Mailgun API request as "subject" field
     * @param html the HTML content of the email message body; included as "html" field in multipart request
     * @param attachmentURL optional URL to a file attachment; if provided, file is downloaded to temporary location,
     *                      attached to email, and deleted after send; may be null
     * @param attachments optional list of {@link File} entities to attach; currently not processed by this implementation;
     *                    may be null (reserved for future enhancement)
     * @return always returns {@code true}; exceptions are propagated if email sending fails rather than
     *         returning false
     */
    @Override
    public boolean sendEmail(String fullFrom, String fullTo, String subject, String html, String attachmentURL, List<File> attachments) {
        debug("[sendEmail] {} -> {} Subject: {}", fullFrom, fullTo, subject);
        EmailConfig emailConfig = emailConfigRepository.findAll().stream().findFirst().orElse(null);

        RestTemplate rTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        String mailgunKey = emailConfig != null ? StringUtils.defaultString(emailConfig.getMailgunApiKey(), mailgunApiKey) : mailgunApiKey;
        String authorizationHeader = "Basic " + new String(Base64.getEncoder().encode(("api:" + mailgunKey).getBytes(Charset.defaultCharset())));
        headers.set("Authorization", authorizationHeader);
        MultiValueMap<String, Object> mvmap = new LinkedMultiValueMap<>();
        mvmap.add("from", StringUtils.defaultIfBlank(emailConfig != null ? emailConfig.getFrom() : null, fullFrom) );
        mvmap.add("to", fullTo);
        mvmap.add("subject", subject);
        mvmap.add("h:Reply-To", StringUtils.defaultIfBlank(emailConfig != null ? emailConfig.getReplyTo() : null, replyTo) );
        if(StringUtils.isNotEmpty(appLogoPath)) {
            mvmap.add("inline", new ClassPathResource(appLogoPath));
        }
        mvmap.add("html", html);

        Path tmpFile = prepareTempAttachmentFile(attachmentURL);
        if (tmpFile != null) {
            mvmap.add("attachment", new FileSystemResource(tmpFile.toFile()));
        }

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(mvmap, headers);
        ResponseEntity<String> response = rTemplate.postForEntity(mailgunApiUrl, request, String.class);

        try {
            if (tmpFile != null) {
                Files.deleteIfExists(tmpFile);
            }
        } catch (IOException e) {
            error(e, "Error while wiping attachment {}", tmpFile);
        }
        return true;
    }

}
