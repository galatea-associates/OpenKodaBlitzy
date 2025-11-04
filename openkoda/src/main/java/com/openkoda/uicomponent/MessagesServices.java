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

package com.openkoda.uicomponent;

import com.openkoda.model.User;
import com.openkoda.model.file.File;
import com.openkoda.model.task.Email;
import com.openkoda.uicomponent.annotation.Autocomplete;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service contract for email delivery and WebSocket-based real-time messaging.
 * <p>
 * Provides overloaded sendEmail methods for immediate and scheduled delivery with plain text
 * or templated content using Thymeleaf templates. Supports file attachments for emails.
 * Offers WebSocket message delivery to specific users or broadcast channels for real-time
 * communication. All email operations create Email entity records for tracking and audit purposes.
 * All methods are annotated with {@link Autocomplete} for UI tooling metadata.
 * 
 * <p>
 * Implementation: LiveMessagesServices provides the concrete implementation of this interface,
 * delegating to EmailService for email operations and WebsocketService for WebSocket messaging.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Email
 * @see User
 * @see File
 */
public interface MessagesServices {

    /**
     * Sends immediate email with plain text message and optional file attachments.
     * <p>
     * Creates Email entity, queues for immediate delivery via emailService.sendAndSaveEmail.
     * Attachments are converted from List to File array. Email record is persisted with
     * delivery status for audit trail.
     * 
     * <p>
     * Example: {@code sendEmail("user@example.com", "Welcome", "Hello!", null)} sends
     * immediate plain text email without attachments.
     * 
     *
     * @param email recipient email address (validated format required)
     * @param subject email subject line
     * @param message plain text message body (HTML not supported in this variant)
     * @param attachments list of File entities to attach (may be null or empty for no attachments)
     * @return Email entity with status, scheduledAt timestamp, and tracking information
     */
    @Autocomplete(doc="Send an email message")
    Email sendEmail(String email, String subject, String message, List<File> attachments);
    
    /**
     * Sends scheduled email with plain text message at specified future time.
     * <p>
     * Creates Email entity with scheduledAt timestamp and queues for scheduled delivery.
     * Background job processes scheduled emails at configured interval. If sendOn is in the
     * past, delivers immediately.
     * 
     * <p>
     * Example: {@code sendEmail("user@example.com", "Reminder", "Meeting tomorrow", null,
     * LocalDateTime.now().plusHours(12))} schedules email for 12 hours from now.
     * 
     *
     * @param email recipient email address
     * @param subject email subject line
     * @param message plain text message body
     * @param attachments list of File entities to attach (may be null or empty)
     * @param sendOn future LocalDateTime for scheduled delivery (if past, sends immediately)
     * @return Email entity with scheduledAt set to sendOn and pending status
     */
    @Autocomplete(doc="Send a scheduled email message")
    Email sendEmail(String email, String subject, String message, List<File> attachments, LocalDateTime sendOn);
    
    /**
     * Sends immediate email using Thymeleaf template with model variables.
     * <p>
     * Constructs PageModelMap with model variables, resolves template path as EMAIL_TEMPLATE_PATH
     * plus resourceName, and renders template via emailService.sendAndSaveOrganizationEmail with
     * tenant context. Default template is used if resourceName is null or empty.
     * 
     * <p>
     * Example: {@code sendEmail("user@example.com", "Invoice", "invoice",
     * Map.of("total", 100), null)} renders the invoice template with total variable.
     * 
     *
     * @param email recipient email address
     * @param subject email subject line
     * @param resourceName Thymeleaf template name (e.g., "welcome", "invoice") - defaults to
     *                     "default" template if null
     * @param model map of template variables for Thymeleaf substitution (accessed as ${key} in template)
     * @param attachments list of File entities to attach
     * @return Email entity with rendered HTML content and delivery status
     */
    @Autocomplete(doc="Send an email message based on a template")
    Email sendEmail(String email, String subject, String resourceName, Map<String, Object> model, List<File> attachments);
    
    /**
     * Sends scheduled email using Thymeleaf template at specified future time.
     * <p>
     * Combines template rendering with scheduled delivery. Template is rendered immediately
     * but email is queued for future delivery at sendOn timestamp. Background job processes
     * scheduled emails at the specified time.
     * 
     * <p>
     * Example: {@code sendEmail("user@example.com", "Report", "monthly-report", reportData,
     * null, LocalDateTime.now().plusDays(1))} schedules templated email for tomorrow.
     * 
     *
     * @param email recipient email address
     * @param subject email subject line
     * @param resourceName Thymeleaf template name (defaults to "default" if null)
     * @param model map of template variables for rendering
     * @param attachments list of File entities to attach
     * @param sendOn future LocalDateTime for scheduled delivery
     * @return Email entity with scheduledAt set to sendOn, rendered content, and pending status
     */
    @Autocomplete(doc="Send a scheduled email message based on a template")
    Email sendEmail(String email, String subject, String resourceName, Map<String, Object> model, List<File> attachments, LocalDateTime sendOn);
    
    /**
     * Sends WebSocket message to specific user's active session(s).
     * <p>
     * Delegates to websocketService to send message to user's WebSocket sessions. Payload is
     * serialized to JSON via Jackson. If user has multiple browser tabs or sessions, message
     * is delivered to all active connections. Message sending is synchronized by websocketService
     * for thread-safety.
     * 
     * <p>
     * Example: {@code sendToWebsocketUser(user, "notifications",
     * Map.of("type", "alert", "message", "New comment"))} sends notification to user.
     * 
     *
     * @param user target User entity (message delivered to all user's active WebSocket connections)
     * @param channelName WebSocket channel or topic name for message routing (e.g., "notifications", "updates")
     * @param payload message payload object (serialized to JSON for transmission)
     * @return true if message sent to at least one active session, false if user has no active
     *         WebSocket connections
     */
    @Autocomplete(doc="Send message to a specific user through WebSocket")
    boolean sendToWebsocketUser(User user, String channelName, Object payload);
    
    /**
     * Broadcasts WebSocket message to all users subscribed to channel.
     * <p>
     * Broadcasts message to all WebSocket connections subscribed to channelName. Payload is
     * serialized to JSON. Suitable for system-wide announcements, live updates, and real-time
     * data feeds. All subscribed users receive the message regardless of organization or tenant
     * context, so authorization should be applied at subscription time.
     * 
     * <p>
     * Example: {@code sendToWebsocketChannel("live-updates",
     * Map.of("event", "deployment", "status", "complete"))} broadcasts to all subscribers.
     * 
     *
     * @param channelName WebSocket channel or topic name for broadcast (e.g., "global-announcements")
     * @param payload message payload object (serialized to JSON for transmission)
     * @return true if message sent to at least one subscriber, false if channel has no active subscribers
     */
    @Autocomplete(doc="Send message to a specific WebSocket channel")
    boolean sendToWebsocketChannel(String channelName, Object payload);

}
