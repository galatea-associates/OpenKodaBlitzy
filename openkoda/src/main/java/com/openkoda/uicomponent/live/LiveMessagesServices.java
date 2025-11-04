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

package com.openkoda.uicomponent.live;

import com.openkoda.controller.common.PageAttributes;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.service.WebsocketService;
import com.openkoda.core.service.email.EmailService;
import com.openkoda.model.User;
import com.openkoda.model.file.File;
import com.openkoda.model.task.Email;
import com.openkoda.repository.user.UserRepository;
import com.openkoda.uicomponent.MessagesServices;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link MessagesServices} interface providing email and WebSocket messaging operations for UI components.
 * <p>
 * This service acts as a facade for email composition and WebSocket communication, delegating to {@link EmailService}
 * and {@link WebsocketService} respectively. It provides template resolution, rendering capabilities, and attachment
 * handling with automatic conversion from List&lt;File&gt; to File[] array format.

 * <p>
 * Key features:
 * <ul>
 *   <li>Organization-scoped and user-scoped email sending</li>
 *   <li>Template-based email composition with default template support</li>
 *   <li>Scheduled email delivery with LocalDateTime scheduling</li>
 *   <li>File attachment handling with automatic type conversion</li>
 *   <li>WebSocket message delivery to users and channels</li>
 * </ul>

 * <p>
 * Thread-safety: This class is stateless and thread-safe. All operations delegate to injected Spring-managed services.

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see MessagesServices
 * @see EmailService
 * @see WebsocketService
 */
@Component
public class LiveMessagesServices implements MessagesServices {

    /**
     * Default email template name used when no specific template is specified.
     * This template is applied when sending simple text-based emails without custom template requirements.
     */
    private static final String DEFAULT_EMAIL_TEMPLATE = "default";
    
    /**
     * Base path for email template resources within the application resource directory.
     * All email templates are resolved relative to this path: frontend-resource/global/email/
     */
    private static final String EMAIL_TEMPLATE_PATH = "frontend-resource/global/email/";
    
    /**
     * Repository for User entity lookup operations. Used to resolve user IDs to User entities and retrieve email addresses.
     */
    @Inject
    UserRepository userRepository;
    
    /**
     * Core email service for email composition, template rendering, and delivery operations.
     * Handles both immediate and scheduled email sending with attachment support.
     */
    @Inject
    EmailService emailService;
    
    /**
     * WebSocket service for real-time message delivery to user channels and broadcast channels.
     * Enables push-based communication with connected clients.
     */
    @Inject
    WebsocketService websocketService;

    /**
     * Sends an organization-scoped email to a user with specified template and model data.
     * <p>
     * This method delegates to EmailService for organization-specific email delivery, ensuring proper
     * tenant isolation and organization branding.

     *
     * @param recipient the User entity representing the email recipient
     * @param emailTemplateName the name of the email template to use for rendering
     * @param model the PageModelMap containing template variables and data
     * @param orgId the organization ID for scoping the email to a specific tenant
     * @return the created Email entity with delivery status
     */
    public Email sendEmail(User recipient, String emailTemplateName, PageModelMap model, Long orgId) {
        return emailService.sendAndSaveOrganizationEmail(recipient, emailTemplateName, model, orgId);
    }

    /**
     * Sends a simple text email to a user identified by user ID using the default email template.
     * <p>
     * This convenience method looks up the user by ID, extracts their email address, and sends a message
     * using the DEFAULT_EMAIL_TEMPLATE. The message content is placed in the PageModelMap under the
     * "message" attribute for template rendering.

     *
     * @param userId the unique identifier of the user to send email to
     * @param subject the email subject line
     * @param message the email message content to be rendered in the template
     * @return the created Email entity with delivery status
     */
    public Email sendEmail(Long userId, String subject, String message) {
        PageModelMap model = new PageModelMap();
        model.put(PageAttributes.message, message);
        return sendEmail(userRepository.findOne(userId).getEmail(), subject, DEFAULT_EMAIL_TEMPLATE, model, null);
    }

    /**
     * Sends an email with file attachments to the specified email address using the default template.
     * <p>
     * This method supports attaching multiple files to the email. The message content is rendered
     * using the DEFAULT_EMAIL_TEMPLATE with the message placed in the model.

     *
     * @param email the recipient email address
     * @param subject the email subject line
     * @param message the email message content to be rendered in the template
     * @param filesToAttach the list of File entities to attach to the email, or null for no attachments
     * @return the created Email entity with delivery status
     */
    public Email sendEmail(String email, String subject, String message, List<File> filesToAttach) {
        PageModelMap model = new PageModelMap();
        model.put(PageAttributes.message, message);
        return sendEmail(email, subject, DEFAULT_EMAIL_TEMPLATE, model, filesToAttach);
    }
    
    /**
     * Schedules an email with attachments to be sent at a specific date and time using the default template.
     * <p>
     * This method allows for deferred email delivery by specifying a sendOn timestamp. The email will be
     * queued and delivered at the scheduled time. Uses DEFAULT_EMAIL_TEMPLATE for message rendering.

     *
     * @param email the recipient email address
     * @param subject the email subject line
     * @param message the email message content to be rendered in the template
     * @param attachments the list of File entities to attach to the email, or null for no attachments
     * @param sendOn the LocalDateTime timestamp when the email should be sent
     * @return the created Email entity with scheduled delivery status
     */
    @Override
    public Email sendEmail(String email, String subject, String message, List<File> attachments, LocalDateTime sendOn) {
        PageModelMap model = new PageModelMap();
        model.put(PageAttributes.message, message);
        return sendEmail(email, subject, DEFAULT_EMAIL_TEMPLATE, model, attachments, sendOn);
    }

    /**
     * Sends an email with custom template and model data, optionally including file attachments.
     * <p>
     * This method provides full control over template selection and model variables. The resourceName
     * is resolved relative to EMAIL_TEMPLATE_PATH. The messageModel Map is converted to PageModelMap
     * for template rendering.

     *
     * @param email the recipient email address
     * @param subject the email subject line
     * @param resourceName the name of the email template resource (without path prefix)
     * @param messageModel the Map of template variables and their values for rendering
     * @param attachments the list of File entities to attach to the email, or null for no attachments
     * @return the created Email entity with delivery status
     */
    @Override
    public Email sendEmail(String email, String subject, String resourceName, Map<String, Object> messageModel,
            List<File> attachments) {
        return sendEmail(email, subject, resourceName, messageModel, attachments, null);
    }

    /**
     * Sends a fully-configured email with custom template, model data, attachments, and scheduled delivery.
     * <p>
     * This is the most comprehensive email sending method, providing control over all aspects of email
     * composition and delivery. The template path is constructed by prepending EMAIL_TEMPLATE_PATH to
     * the resourceName. The messageModel Map is converted to PageModelMap, and the List&lt;File&gt; attachments
     * are converted to File[] array format required by EmailService.

     * <p>
     * Example usage:
     * <pre>
     * Map&lt;String, Object&gt; model = Map.of("userName", "John", "orderNumber", "12345");
     * sendEmail("user@example.com", "Order Confirmation", "order-confirmation", model, files, LocalDateTime.now());
     * </pre>

     *
     * @param email the recipient email address
     * @param subject the email subject line
     * @param resourceName the name of the email template resource (path prefix EMAIL_TEMPLATE_PATH is prepended)
     * @param messageModel the Map of template variables and their values for rendering
     * @param attachments the list of File entities to attach, or null for no attachments (converted to File[] internally)
     * @param sendOn the LocalDateTime timestamp when the email should be sent, or null for immediate delivery
     * @return the created Email entity with delivery or scheduled status
     */
    @Override
    public Email sendEmail(String email, String subject, String resourceName, Map<String, Object> messageModel,
            List<File> attachments, LocalDateTime sendOn) {
        PageModelMap model = new PageModelMap();
        model.putAll(messageModel);
        return emailService.sendAndSaveEmail(email, subject, EMAIL_TEMPLATE_PATH + resourceName, model, sendOn, attachments == null ? null : attachments.toArray(File[]::new));
    }

    /**
     * Sends a WebSocket message to a specific user's private channel.
     * <p>
     * This method delivers a message to a user-specific WebSocket channel, enabling targeted real-time
     * communication with individual connected users. The message is only delivered to the specified user's
     * active WebSocket sessions.

     *
     * @param user the target User entity to receive the WebSocket message
     * @param channelName the name of the user's WebSocket channel to send to
     * @param payload the message payload object to be serialized and delivered
     * @return true if the message was successfully sent, false otherwise
     */
    public boolean sendToWebsocketUser(User user, String channelName, Object payload) {
        return websocketService.sendToUserChannel(user, channelName, payload);
    }
    /**
     * Broadcasts a WebSocket message to all subscribers of a named channel.
     * <p>
     * This method sends a message to a broadcast WebSocket channel, delivering the payload to all connected
     * clients subscribed to the specified channel. Useful for system-wide notifications and multi-user updates.

     *
     * @param channelName the name of the broadcast WebSocket channel to send to
     * @param payload the message payload object to be serialized and broadcast to all subscribers
     * @return true if the message was successfully broadcast, false otherwise
     */
    public boolean sendToWebsocketChannel(String channelName, Object payload){
        return websocketService.sendToChannel(channelName, payload);
    }
}
