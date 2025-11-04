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

/**
 * Email sending infrastructure for OpenKoda with template processing and multiple delivery implementations.
 * <p>
 * This package provides a comprehensive email subsystem that supports Thymeleaf template-based email composition,
 * multiple sender implementations (SMTP, Mailgun, and development stub), per-organization SMTP configuration,
 * asynchronous delivery via background jobs, and file attachment support.
 * 
 *
 * <b>Package Structure</b>
 *
 * <b>Core Components</b>
 * <ul>
 * <li>{@link com.openkoda.core.service.email.EmailService} - High-level orchestration API for sending emails</li>
 * <li>{@link com.openkoda.core.service.email.EmailConstructor} - Thymeleaf template rendering and Email entity assembly</li>
 * <li>{@link com.openkoda.core.service.email.EmailSender} - Abstract base class for sender implementations with lifecycle management</li>
 * </ul>
 *
 * <b>Sender Implementations</b>
 * <ul>
 * <li>{@link com.openkoda.core.service.email.SmtpEmailSender} - Production SMTP implementation using Spring's JavaMailSender</li>
 * <li>{@link com.openkoda.core.service.email.MailgunEmailSender} - Mailgun HTTP API implementation via RestTemplate</li>
 * <li>{@link com.openkoda.core.service.email.FakeSysoutEmailSender} - Development/testing stub that logs to console</li>
 * </ul>
 *
 * <b>Supporting Components</b>
 * <ul>
 * <li>{@link com.openkoda.core.service.email.EmailConfigJavaMailSender} - JavaMailSender wrapper with database-backed SMTP configuration</li>
 * <li>{@link com.openkoda.core.service.email.EmailAttachment} - Attachment data provider interface</li>
 * <li>{@link com.openkoda.core.service.email.StandardEmailTemplates} - Canonical Thymeleaf template name constants</li>
 * </ul>
 *
 * <b>Email Workflow</b>
 *
 * <p>The typical email sending flow:</p>
 * <ol>
 * <li>Application calls {@code EmailService.sendAndSaveEmail()}</li>
 * <li>{@code EmailConstructor.prepareEmailWithTitleFromTemplate()} renders Thymeleaf template</li>
 * <li>Email entity is persisted to database</li>
 * <li>{@code EmailSenderJob} background job polls database for unsent emails</li>
 * <li>{@code EmailSender.sendMail()} delegates to appropriate sender implementation</li>
 * <li>SMTP/Mailgun/Fake sender delivers email based on active Spring profile</li>
 * </ol>
 *
 * <b>Configuration</b>
 *
 * <b>Spring Profile-Based Sender Selection</b>
 * <ul>
 * <li>{@code @Profile("local")} activates {@code FakeSysoutEmailSender} (prevents real delivery)</li>
 * <li>{@code @Profile("mailgun")} activates {@code MailgunEmailSender}</li>
 * <li>{@code @Profile("smtp")} or default activates {@code SmtpEmailSender}</li>
 * </ul>
 *
 * <b>Per-Organization SMTP Settings</b>
 * <p>
 * {@code EmailConfig} entities store organization-specific SMTP configuration including host, port,
 * credentials, and SSL/TLS settings. These override application.properties defaults at send time.
 * 
 *
 * <b>Application Properties</b>
 * <ul>
 * <li>{@code mail.from} - Default sender address</li>
 * <li>{@code mail.replyTo} - Default reply-to address</li>
 * <li>{@code mailgun.apikey} - Mailgun API key</li>
 * <li>{@code mailgun.apiurl} - Mailgun API endpoint URL</li>
 * <li>{@code base.url} - Application base URL for links in emails</li>
 * </ul>
 *
 * <b>Integration</b>
 *
 * <b>Thymeleaf Template Processing</b>
 * <p>
 * Email templates are processed by Thymeleaf {@code TemplateEngine} with model variables provided by the caller.
 * Templates are located in {@code frontend-resource/global/email/} directory and referenced using
 * {@code StandardEmailTemplates} constants.
 * 
 *
 * <b>File Attachments</b>
 * <p>
 * {@code EmailAttachment} implementations provide {@code byte[]} content for MIME attachments. File entities
 * from the {@code com.openkoda.model.file.File} domain are mapped to {@code Email.filesId} for attachment references.
 * Attachment URLs are materialized via {@code EmailSender.prepareTempAttachmentFile()} which downloads content
 * and creates temporary files for MIME composition.
 * 
 *
 * <b>Usage Example</b>
 *
 * <pre>{@code
 * emailService.sendAndSaveEmail(user, StandardEmailTemplates.WELCOME);
 * }</pre>
 *
 * <b>Operational Notes</b>
 *
 * <ul>
 * <li><b>Async Delivery:</b> Email sending is decoupled from request handling via database-backed queue,
 * allowing transaction boundaries and retry logic</li>
 * <li><b>Database-Backed Queue:</b> Emails are persisted before sending, enabling tracking, retry, and audit trail</li>
 * <li><b>Profile Conflicts:</b> {@code @Primary} annotations resolve sender conflicts when multiple profiles are active</li>
 * <li><b>Development Safety:</b> {@code FakeSysoutEmailSender} prevents accidental email delivery in local profile</li>
 * <li><b>Memory Considerations:</b> {@code EmailAttachment.getData()} returns full {@code byte[]} in memory,
 * which may impact performance for large attachments</li>
 * </ul>
 *
 * @since 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.core.service.email;