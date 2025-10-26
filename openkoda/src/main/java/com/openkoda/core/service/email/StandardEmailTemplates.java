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
 * Defines canonical Thymeleaf template identifiers for email composition across the application.
 * <p>
 * This interface provides standardized template path constants used by the email service layer
 * to render HTML emails. The {@code EMAIL_BASE} constant establishes the base path
 * (frontend-resource/global/email/) and all template constants follow the pattern of base + template name.
 * Templates are resolved by ThymeleafTemplateEngine and must exist in the application resources.
 * </p>
 * <p>
 * <b>Template Categories:</b>
 * </p>
 * <ul>
 *   <li><b>User Management:</b> INVITE_EXISTING, INVITE_NEW, WELCOME, PASSWORD_RECOVERY</li>
 *   <li><b>Notifications:</b> NOTIFICATION_ORGANIZATION_EMAIL, NOTIFICATION_USER_EMAIL</li>
 *   <li><b>Billing:</b> NEW_INVOICE_EMAIL, PAYMENT_FAILED_EMAIL, PAYMENT_IN_14_DAYS, SUBSCRIPTION_CONFIRMED</li>
 *   <li><b>Trial Management:</b> TRIAL_EXPIRED_EMAIL, TRIAL_WILL_EXPIRE_EMAIL</li>
 * </ul>
 * <p>
 * Example usage with EmailConstructor:
 * <pre>{@code
 * emailConstructor.sendEmail(StandardEmailTemplates.WELCOME, recipientEmail, contextMap);
 * }</pre>
 * </p>
 *
 * @since 1.7.1
 * @author OpenKoda Team
 * @see com.openkoda.core.service.email.EmailConstructor
 */

public interface StandardEmailTemplates {
    
    /**
     * Base path for all email templates in the application resource hierarchy.
     * <p>
     * This constant defines the root directory (frontend-resource/global/email/) where
     * Thymeleaf email templates are located. All template constants in this interface
     * extend this base path to form complete template identifiers.
     * </p>
     */
    String EMAIL_BASE = "frontend-resource/global/email/";
    
    /**
     * Template for inviting existing users to join an organization.
     * <p>
     * Used when an existing OpenKoda user is invited to become a member of a new organization.
     * The template includes organization details and acceptance instructions.
     * </p>
     */
    String INVITE_EXISTING = EMAIL_BASE + "invite-existing";
    
    /**
     * Template for inviting new users to register and join an organization.
     * <p>
     * Used when a new user (not yet registered) is invited to create an account and
     * join an organization. The template includes registration link and onboarding information.
     * </p>
     */
    String INVITE_NEW = EMAIL_BASE + "invite-new";
    
    /**
     * Template for new invoice notifications sent to organization administrators.
     * <p>
     * Used to notify users when a new invoice is generated for their organization.
     * The template includes invoice details, amount, and payment instructions.
     * </p>
     */
    String NEW_INVOICE_EMAIL = EMAIL_BASE + "new-invoice-email";
    
    /**
     * Template for organization-scoped notification emails.
     * <p>
     * Used for general notifications that apply to an entire organization, such as
     * system updates, policy changes, or organization-level events.
     * </p>
     */
    String NOTIFICATION_ORGANIZATION_EMAIL = EMAIL_BASE + "notification-organization-email";
    
    /**
     * Template for user-scoped notification emails.
     * <p>
     * Used for personal notifications directed to individual users, such as
     * task assignments, mention alerts, or user-specific system notifications.
     * </p>
     */
    String NOTIFICATION_USER_EMAIL = EMAIL_BASE + "notification-user-email";
    
    /**
     * Template for password recovery emails.
     * <p>
     * Used when a user requests to reset their password. The template includes
     * a secure password reset link with expiration time and security instructions.
     * </p>
     */
    String PASSWORD_RECOVERY = EMAIL_BASE + "password-recovery";
    
    /**
     * Template for payment failure alert emails.
     * <p>
     * Used to notify organization administrators when a payment attempt fails.
     * The template includes failure reason, retry instructions, and support contact information.
     * </p>
     */
    String PAYMENT_FAILED_EMAIL = EMAIL_BASE + "payment-failed-email";
    
    /**
     * Template for payment reminder emails sent 14 days before due date.
     * <p>
     * Used to remind organization administrators about upcoming payments.
     * The template includes payment amount, due date, and payment method options.
     * </p>
     */
    String PAYMENT_IN_14_DAYS = EMAIL_BASE + "payment-in-14-days";
    
    /**
     * Template for subscription confirmation emails.
     * <p>
     * Used to confirm successful subscription activation or renewal.
     * The template includes subscription details, billing cycle, and next payment date.
     * </p>
     */
    String SUBSCRIPTION_CONFIRMED = EMAIL_BASE + "subscription-confirmed";
    
    /**
     * Template for trial expiration notification emails.
     * <p>
     * Used to notify users when their trial period has ended.
     * The template includes upgrade options, pricing information, and data retention policy.
     * </p>
     */
    String TRIAL_EXPIRED_EMAIL = EMAIL_BASE + "trial-expired-email";
    
    /**
     * Template for trial expiration warning emails.
     * <p>
     * Used to warn users that their trial period will expire soon (typically sent a few days before expiration).
     * The template includes days remaining, upgrade call-to-action, and feature comparison.
     * </p>
     */
    String TRIAL_WILL_EXPIRE_EMAIL = EMAIL_BASE + "trial-will-expire-email";
    
    /**
     * Template for welcome emails sent to newly registered users.
     * <p>
     * Used for user onboarding immediately after successful registration.
     * The template includes getting started guide, key features overview, and support resources.
     * </p>
     */
    String WELCOME = EMAIL_BASE + "welcome";
}
