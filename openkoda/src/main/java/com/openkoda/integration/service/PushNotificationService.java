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

package com.openkoda.integration.service;

import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.security.UserProvider;
import com.openkoda.core.service.email.StandardEmailTemplates;
import com.openkoda.dto.NotificationDto;
import com.openkoda.integration.controller.IntegrationComponentProvider;
import com.openkoda.integration.model.configuration.IntegrationModuleOrganizationConfiguration;
import com.openkoda.model.Organization;
import com.openkoda.model.User;
import com.openkoda.model.task.Email;
import com.openkoda.model.task.HttpRequestTask;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing push notification subscriptions and delivering notifications via WebSocket or external push services.
 * <p>
 * Converts NotificationDto payloads into asynchronous HttpRequestTask entries for Slack and MS Teams webhooks and into 
 * persisted Email entities for user or organization notifications. This service validates notification propagation flags,
 * reads organization webhook URLs from IntegrationModuleOrganizationConfiguration, and formats JSON payloads for different
 * external systems.
 * </p>
 * <p>
 * Key capabilities:
 * <ul>
 *   <li>Slack webhook integration with anchor tag extraction via StringUtils.substringBetween for rich message formatting</li>
 *   <li>MS Teams webhook integration with double-quote escaping via StringUtils.replace for JSON safety</li>
 *   <li>Email notification generation using templates NOTIFICATION_USER_EMAIL and NOTIFICATION_ORGANIZATION_EMAIL</li>
 *   <li>Persists HttpRequestTask via repositories.unsecure.httpRequest.save() for background worker processing</li>
 *   <li>Builds per-user or per-organization emails via services.emailConstructor.prepareEmail()</li>
 * </ul>
 * </p>
 * <p>
 * For organization emails, this service temporarily sets UserProvider.setConsumerAuthentication() to enumerate organization
 * members and then clears it with UserProvider.clearAuthentication(). All operations produce side effects through persisted
 * tasks and emails for background workers.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * NotificationDto notification = new NotificationDto();
 * notification.setPropagate(true);
 * pushNotificationService.createSlackPostMessageRequest(notification);
 * </pre>
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com), OpenKoda Team
 * @since 2019-07-03
 * @version 1.7.1
 */
@Service
public class PushNotificationService extends IntegrationComponentProvider {

    /**
     * Creates a Slack webhook push message request for organization notifications.
     * <p>
     * This method validates that the notification should propagate and is organization-scoped (not user-specific).
     * It retrieves the Slack webhook URL from the organization's integration configuration, formats the notification
     * message into Slack-compatible JSON, and persists an HttpRequestTask for asynchronous delivery by background workers.
     * </p>
     * <p>
     * JSON formatting includes anchor tag extraction when the message contains HTML links. The method uses 
     * StringUtils.substringBetween to extract link titles and hrefs, then constructs a Slack attachment with title_link.
     * All {@code <br/>} tags are removed from the message before JSON formatting.
     * </p>
     * <p>
     * Side effects:
     * <ul>
     *   <li>Persists HttpRequestTask via repositories.unsecure.httpRequest.save() if webhook URL is configured</li>
     *   <li>No action taken if notification.getPropagate() returns false or notification is user-specific</li>
     * </ul>
     * </p>
     *
     * @param notification DTO containing message, organization context, and propagation flag; must have organizationId set
     *                     and userId null for webhook delivery to occur
     */
    public void createSlackPostMessageRequest(NotificationDto notification) {
        debug("[createSlackPostRequest]");
        if(!notification.getPropagate()){
            return;
        }
        if (notification.getOrganizationId() != null && notification.getUserId() == null) {
            IntegrationModuleOrganizationConfiguration organizationConfiguration = integrationService.getOrganizationConfiguration(notification.getOrganizationId());
            String slackWebhookUrl = organizationConfiguration.getSlackWebhookUrl();
            if (slackWebhookUrl != null && !slackWebhookUrl.isEmpty()) {
                int linkStartIndex = notification.getMessage().indexOf("<a href=\"");
                String requestJson;
                if (linkStartIndex != -1) {
                    String linkTitle = StringUtils.substringBetween(notification.getMessage(), "\">", "</a>");
                    String linkHref = StringUtils.substringBetween(notification.getMessage(), "<a href=\"", "\">");
                    String msgWithRemovedAnchorTag = notification.getMessage().substring(0, notification.getMessage().indexOf("<a href=\""));
                    requestJson = String.format("{\"text\":\"%s\",\"attachments\":[{\"title\":\"%s\",\"title_link\":\"%s\"}]}",
                            msgWithRemovedAnchorTag, linkTitle, linkHref);
                } else {
                    requestJson = String.format("{\"text\":\"%s\"}", notification.getMessage());
                }
                requestJson = requestJson.replaceAll("<br/>", "");
                debug("Creating Slack push message for organization notification");
                HttpRequestTask httpRequestTask = new HttpRequestTask(slackWebhookUrl, requestJson);
                repositories.unsecure.httpRequest.save(httpRequestTask);
            }
        }
    }

    /**
     * Creates a Microsoft Teams webhook push message request for organization notifications.
     * <p>
     * This method validates that the notification should propagate and is organization-scoped (not user-specific).
     * It retrieves the MS Teams webhook URL from the organization's integration configuration, formats the notification
     * message into Teams-compatible JSON with proper quote escaping, and persists an HttpRequestTask for asynchronous
     * delivery by background workers.
     * </p>
     * <p>
     * JSON formatting includes double-quote escaping using StringUtils.replace to ensure valid JSON structure. All
     * double-quote characters in the message are escaped as {@code \"} to prevent JSON parsing errors.
     * </p>
     * <p>
     * Side effects:
     * <ul>
     *   <li>Persists HttpRequestTask via repositories.unsecure.httpRequest.save() if webhook URL is configured</li>
     *   <li>No action taken if notification.getPropagate() returns false or notification is user-specific</li>
     * </ul>
     * </p>
     *
     * @param notification DTO containing message, organization context, and propagation flag; must have organizationId set
     *                     and userId null for webhook delivery to occur
     */
    public void createMsTeamsPostMessageRequest(NotificationDto notification) {
        debug("[createMsTeamsPostMessageRequest]");
        if(!notification.getPropagate()) {
            return;
        }
        if (notification.getOrganizationId() != null && notification.getUserId() == null) {
            IntegrationModuleOrganizationConfiguration organizationConfiguration = integrationService.getOrganizationConfiguration(notification.getOrganizationId());
            String msTeamsWebhookUrl = organizationConfiguration.getMsTeamsWebhookUrl();
            if (msTeamsWebhookUrl != null && !msTeamsWebhookUrl.isEmpty()) {
                debug("Creating Ms Teams push message for organization notification");
                HttpRequestTask httpRequestTask = new HttpRequestTask(msTeamsWebhookUrl,
                        String.format("{\"text\":\"%s\"}", StringUtils.replace(notification.getMessage(), "\"", "\\\"")));
                repositories.unsecure.httpRequest.save(httpRequestTask);
            }
        }
    }

    /**
     * Creates email notifications for users or organizations based on notification scope.
     * <p>
     * This method handles both user-specific and organization-wide email notifications. It validates that the notification
     * should propagate, prepares email messages using standard email templates, and persists Email entities for delivery
     * by background email workers.
     * </p>
     * <p>
     * User-specific notifications (when userId is set):
     * <ul>
     *   <li>Creates a single email to the specified user</li>
     *   <li>Uses StandardEmailTemplates.NOTIFICATION_USER_EMAIL template</li>
     *   <li>Includes user name and email address from User entity</li>
     * </ul>
     * </p>
     * <p>
     * Organization-wide notifications (when organizationId is set and userId is null):
     * <ul>
     *   <li>Creates emails for all users in the organization</li>
     *   <li>Uses StandardEmailTemplates.NOTIFICATION_ORGANIZATION_EMAIL template</li>
     *   <li>Temporarily sets UserProvider.setConsumerAuthentication() to enumerate organization members</li>
     *   <li>Clears authentication with UserProvider.clearAuthentication() after enumeration</li>
     *   <li>Includes organization name as the sender name</li>
     * </ul>
     * </p>
     * <p>
     * The method builds a PageModelMap with notificationMessage, userEntity, and organizationEntity context for template
     * rendering. All prepared emails include attachment URLs if present in the notification DTO.
     * </p>
     * <p>
     * Side effects:
     * <ul>
     *   <li>Persists Email entities via repositories.unsecure.email.saveAll() for background worker processing</li>
     *   <li>Temporarily modifies UserProvider authentication context for organization member enumeration</li>
     *   <li>No action taken if notification.getPropagate() returns false or notification scope is global</li>
     * </ul>
     * </p>
     *
     * @param notification DTO containing message, subject, attachment URL, and scope (userId or organizationId);
     *                     must have either userId or organizationId set for email generation to occur
     */
    public void createEmailNotification(NotificationDto notification) {
        debug("[createEmailNotification]");
        if(!notification.getPropagate()){
            return;
        }
        List<Email> emailMsgs = new ArrayList<>();
        PageModelMap model = new PageModelMap();
        model.put(notificationMessage, notification.getMessage());
        if (notification.getUserId() != null) {
            debug("Notification is user specific.");
            User user = repositories.unsecure.user.findOne(notification.getUserId());
            model.put(userEntity, user);
            Email prepared = services.emailConstructor.prepareEmail(
                    user.getEmail(),
                    user.getName().isEmpty() ? user.getEmail() : user.getName(),
                    notification.getSubject(),
                    StandardEmailTemplates.NOTIFICATION_USER_EMAIL,
                    5,
                    model);
            prepared.setAttachmentURL(notification.getAttachmentURL());
            emailMsgs.add(prepared);
        } else if (notification.getOrganizationId() != null) {
            debug("Notification is organization specific.");
            Organization organization = repositories.unsecure.organization.findOne(notification.getOrganizationId());
            model.put(organizationEntity, organization);
            UserProvider.setConsumerAuthentication();
            for (User user : repositories.unsecure.userRole.getUsersInOrganization(organization.getId())) {
                model.put(userEntity, user);
                Email prepared = services.emailConstructor.prepareEmail(
                            user.getEmail(),
                            organization.getName(),
                            notification.getSubject(),
                        StandardEmailTemplates.NOTIFICATION_ORGANIZATION_EMAIL,
                            5,
                        model);
                prepared.setAttachmentURL(notification.getAttachmentURL());
                emailMsgs.add(prepared);
            }
            UserProvider.clearAuthentication();
        } else {
            debug("Notification is global - not creating email for that.");
        }

        if (!emailMsgs.isEmpty()) {
            repositories.unsecure.email.saveAll(emailMsgs);
        }
    }
}
