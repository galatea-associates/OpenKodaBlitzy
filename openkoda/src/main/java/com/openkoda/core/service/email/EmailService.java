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

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.core.security.UserProvider;
import com.openkoda.dto.CanonicalObject;
import com.openkoda.dto.OrganizationRelatedObject;
import com.openkoda.model.User;
import com.openkoda.model.file.File;
import com.openkoda.model.task.Email;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

/**
 * Singleton service that orchestrates email sending by preparing Email entities from templates and persisting them for asynchronous delivery.
 * <p>
 * This service extends {@link ComponentProvider} and provides high-level email API for the OpenKoda platform.
 * It prepares {@link Email} entities from templates via {@link EmailConstructor}, persists them to the database
 * via {@code repositories.unsecure.email}, with actual sending delegated to the {@link com.openkoda.core.job.EmailSenderJob}
 * background job for asynchronous delivery.

 * <p>
 * Supports template-based email composition, scheduled delivery via {@code setStartAfter(sendOn)},
 * organization-scoped messages via {@code setOrganizationId()}, and bulk sending to organization users
 * with authentication context switching.

 * <p>
 * <strong>Email Workflow:</strong>
 * <ol>
 *   <li>Template selection and variable binding</li>
 *   <li>{@link EmailConstructor} prepares Email entity with rendered content</li>
 *   <li>Email entity persisted to database via repository</li>
 *   <li>{@link com.openkoda.core.job.EmailSenderJob} periodically fetches pending emails</li>
 *   <li>{@link EmailSender} delivers emails via SMTP</li>
 * </ol>

 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * emailService.sendAndSaveEmail(user, "welcome-email", model);
 * emailService.sendAndSaveEmail(email, subject, "template", model, sendDate, attachments);
 * </pre>

 *
 * @author OpenKoda Team
 * @since 1.7.1
 * @see EmailConstructor
 * @see com.openkoda.core.job.EmailSenderJob
 * @see EmailSender
 * @see Email
 */
@Service
public class EmailService extends ComponentProvider {

    /**
     * Sends and saves an email to a user based on a template name.
     * <p>
     * Delegates to {@link EmailConstructor#prepareEmailWithTitleFromTemplate(User, String)} to prepare
     * the email entity, then persists it via {@code repositories.unsecure.email} for asynchronous delivery.

     *
     * @param recipient the user to receive the email
     * @param emailTemplateName the name of the email template to use
     * @return the saved Email entity ready for delivery
     */
    public Email sendAndSaveEmail(User recipient, String emailTemplateName) {
        debug("[sendAndSaveEmail] Sends {} to {}", emailTemplateName, recipient);
        return repositories.unsecure.email.save(services.emailConstructor.prepareEmailWithTitleFromTemplate(recipient, emailTemplateName));
    }
    /**
     * Sends and saves an email to a user based on a template with template variables.
     * <p>
     * Delegates to {@link EmailConstructor#prepareEmailWithTitleFromTemplate(User, String, String, PageModelMap)}
     * to prepare the email entity with template variables from the model, then persists it for asynchronous delivery.

     *
     * @param recipient the user to receive the email
     * @param emailTemplateName the name of the email template to use
     * @param model the PageModelMap containing template variables for email rendering
     * @return the saved Email entity ready for delivery
     */
    public Email sendAndSaveEmail(User recipient, String emailTemplateName, PageModelMap model) {
        debug("[sendAndSaveEmail] Sends {} to {}", emailTemplateName, recipient);
        return repositories.unsecure.email.save(services.emailConstructor.prepareEmailWithTitleFromTemplate(recipient, null, emailTemplateName, model));
    }

    /**
     * Sends and saves an email to a user with a custom subject line, template, and variables.
     * <p>
     * Delegates to {@link EmailConstructor#prepareEmailWithTitleFromTemplate(User, String, String, PageModelMap)}
     * to prepare the email entity with custom subject line and template variables, then persists it for asynchronous delivery.

     *
     * @param recipient the user to receive the email
     * @param subject the custom subject line for the email
     * @param emailTemplateName the name of the email template to use
     * @param model the PageModelMap containing template variables for email rendering
     * @return the saved Email entity ready for delivery
     */
    public Email sendAndSaveEmail(User recipient, String subject, String emailTemplateName, PageModelMap model) {
        debug("[sendAndSaveEmail] Sends {} to {}", emailTemplateName, recipient);
        return repositories.unsecure.email.save(services.emailConstructor.prepareEmailWithTitleFromTemplate(recipient, subject, emailTemplateName, model));
    }

    /**
     * Sends and saves an email to an email address with subject, template, variables, and attachments.
     * <p>
     * Delegates to {@link EmailConstructor#prepareEmailWithTitleFromTemplate(String, String, String, String, PageModelMap, File...)}
     * to prepare the email entity with custom subject, template variables, and file attachments, then persists it for asynchronous delivery.

     *
     * @param email the recipient email address as a string
     * @param subject the custom subject line for the email
     * @param emailTemplateName the name of the email template to use
     * @param model the PageModelMap containing template variables for email rendering
     * @param attachments varargs array of File attachments to include with the email
     * @return the saved Email entity ready for delivery
     */
    public Email sendAndSaveEmail(String email, String subject, String emailTemplateName, PageModelMap model, File... attachments) {
        debug("[sendAndSaveEmail] Sends {} to {}", emailTemplateName, email);
        return repositories.unsecure.email.save(services.emailConstructor.prepareEmailWithTitleFromTemplate(email, subject, email, emailTemplateName, model, attachments));
    }

    /**
     * Sends and saves an email with scheduled delivery at a specific date and time.
     * <p>
     * Delegates to {@link EmailConstructor#prepareEmailWithTitleFromTemplate(String, String, String, String, PageModelMap, File...)}
     * to prepare the email entity, then sets the scheduled delivery time via {@code setStartAfter(sendOn)} for delayed delivery.
     * The email is persisted and will be sent by {@link com.openkoda.core.job.EmailSenderJob} at or after the specified time.

     *
     * @param email the recipient email address as a string
     * @param subject the custom subject line for the email
     * @param emailTemplateName the name of the email template to use
     * @param model the PageModelMap containing template variables for email rendering
     * @param sendOn the LocalDateTime specifying when the email should be sent (null for immediate scheduling)
     * @param attachments varargs array of File attachments to include with the email
     * @return the saved Email entity ready for scheduled delivery
     */
    public Email sendAndSaveEmail(String email, String subject, String emailTemplateName, PageModelMap model, LocalDateTime sendOn, File... attachments) {
        debug("[sendAndSaveEmail] Sends {} to {}", emailTemplateName, email);
        Email emailToSend = services.emailConstructor.prepareEmailWithTitleFromTemplate(email, subject, email, emailTemplateName, model, attachments);
        if(sendOn != null) {
            emailToSend.setStartAfter(sendOn);
        }
        
        return repositories.unsecure.email.save(emailToSend);
    }
    
    /**
     * Sends and saves an organization-scoped email to a user with template variables.
     * <p>
     * Delegates to {@link EmailConstructor#prepareEmailWithTitleFromTemplate(User, String, PageModelMap)}
     * to prepare the email entity, then sets the organizationId via {@code setOrganizationId(orgId)} for
     * organization-scoped tracking and filtering. The email is persisted for asynchronous delivery.

     *
     * @param recipient the user to receive the email
     * @param emailTemplateName the name of the email template to use
     * @param model the PageModelMap containing template variables for email rendering
     * @param orgId the organization ID to associate with this email for scoped tracking
     * @return the saved Email entity ready for delivery
     */
    public Email sendAndSaveOrganizationEmail(User recipient, String emailTemplateName, PageModelMap model, Long orgId) {
        debug("[sendAndSaveEmail] Sends {} to {}", emailTemplateName, recipient);
        Email orgEmail = services.emailConstructor.prepareEmailWithTitleFromTemplate(recipient, emailTemplateName, model);
        orgEmail.setOrganizationId(orgId);
        return repositories.unsecure.email.save(orgEmail);
    }

    /**
     * Sends and saves an email using a CanonicalObject as template context.
     * <p>
     * Delegates to {@link EmailConstructor#prepareEmailWithTitleFromTemplate(String, String, CanonicalObject)}
     * to prepare the email entity using the CanonicalObject for template variable binding, then persists it for asynchronous delivery.

     *
     * @param object the CanonicalObject providing context data for the email template
     * @param templateName the name of the email template to use
     * @param email the recipient email address as a string
     * @return the saved Email entity ready for delivery
     */
    public Email sendAndSaveEmail(CanonicalObject object, String templateName, String email) {
        debug("[sendAndSaveEmail] Sends {} to {}", templateName, email);
        return repositories.unsecure.email.save(services.emailConstructor.prepareEmailWithTitleFromTemplate(email, templateName, object));
    }

    /**
     * Sends bulk emails to all users in an organization using a template.
     * <p>
     * Switches security context via {@link UserProvider#setConsumerAuthentication()} to enable privileged access,
     * fetches all users in the organization via {@code repositories.unsecure.userRole.getUsersInOrganization()},
     * sends email to each user via {@link #sendAndSaveEmail(User, String)}, then restores the original
     * security context via {@link UserProvider#clearAuthentication()}.

     *
     * @param object the OrganizationRelatedObject providing the organization ID
     * @param templateName the name of the email template to use for all recipients
     * @return true if bulk email operation completed successfully
     */
    public boolean sendEmailToAllInOrganization(OrganizationRelatedObject object, String templateName) {
        debug("[sendEmailToAllInOrganization] Sends {} to organization {} users", templateName, object.getOrganizationId());
        UserProvider.setConsumerAuthentication();
        repositories.unsecure.userRole.getUsersInOrganization(object.getOrganizationId())
                .forEach(user -> sendAndSaveEmail(user, templateName));
        UserProvider.clearAuthentication();
        return true;
    }

    /**
     * Sends bulk emails to users with a specific role in an organization using a template and variables.
     * <p>
     * Switches security context via {@link UserProvider#setConsumerAuthentication()} to enable privileged access,
     * fetches users with the specified role via {@code repositories.unsecure.userRole.getUsersInOrganizationWithRole()},
     * sends organization-scoped email to each user via {@link #sendAndSaveOrganizationEmail(User, String, PageModelMap, Long)},
     * then restores the original security context via {@link UserProvider#clearAuthentication()}.

     *
     * @param organizationId the organization ID to filter users by
     * @param templateName the name of the email template to use for all recipients
     * @param model the PageModelMap containing template variables for email rendering
     * @param roleName the role name to filter users by within the organization
     * @return true if bulk email operation completed successfully
     */
    public boolean sendEmailToUsersWithRoleInOrganization(Long organizationId, String templateName, PageModelMap model, String roleName) {
        debug("[sendEmailToUsersWithRoleInOrganization] Sends {} to organization {} users with role {}", templateName, organizationId, roleName);
        UserProvider.setConsumerAuthentication();
        repositories.unsecure.userRole.getUsersInOrganizationWithRole(organizationId, roleName)
                .forEach(user -> sendAndSaveOrganizationEmail(user, templateName, model, organizationId));
        UserProvider.clearAuthentication();
        return true;
    }
}
