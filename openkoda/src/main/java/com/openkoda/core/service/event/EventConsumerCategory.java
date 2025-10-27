package com.openkoda.core.service.event;

import com.openkoda.model.OptionWithLabel;

/**
 * Enum categorizing event consumers by functional domain for routing, filtering, and UI presentation.
 * <p>
 * This enumeration implements {@link OptionWithLabel} to support dropdown and select UI integration,
 * providing human-friendly labels for each category. Event consumers are organized into functional
 * domains to enable efficient event routing and filtering in the EventListenerService.
 * </p>
 * <p>
 * Categories include:
 * <ul>
 *   <li><b>INTEGRATION</b> - External API integrations (Trello, GitHub, Jira, Basecamp)</li>
 *   <li><b>BACKUP</b> - Backup operations (database backup, file backup, backup verification)</li>
 *   <li><b>MESSAGE</b> - Messaging and notifications (email, SMS, in-app messages)</li>
 *   <li><b>PUSH_NOTIFICATION</b> - Push notification delivery (mobile, browser push)</li>
 *   <li><b>ROLE_MODIFICATION</b> - Role and permission changes (privilege updates, role reconciliation)</li>
 *   <li><b>SERVER_SIDE_CODE</b> - Server-side code execution (GraalVM JavaScript, dynamic scripting)</li>
 * </ul>
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * EventListenerService.registerConsumer(eventName, consumer, EventConsumerCategory.INTEGRATION);
 * }</pre>
 * </p>
 *
 * @since 1.7.1
 * @author OpenKoda Team
 * @see com.openkoda.model.OptionWithLabel
 */
public enum EventConsumerCategory implements OptionWithLabel {
    /**
     * External API integration consumers (Trello, GitHub, Jira, Basecamp).
     * <p>
     * Used for event consumers that interact with third-party services and external APIs,
     * including OAuth-based integrations and REST API consumers.
     * </p>
     */
    INTEGRATION("Integration"),
    
    /**
     * Backup operation consumers (database backup, file backup, backup verification).
     * <p>
     * Used for event consumers that handle backup procedures, restoration processes,
     * and backup integrity verification tasks.
     * </p>
     */
    BACKUP("Backup"),
    
    /**
     * Message and notification consumers (email, SMS, in-app messages).
     * <p>
     * Used for event consumers that handle message delivery through various channels,
     * including email notifications, SMS alerts, and in-application messaging.
     * </p>
     */
    MESSAGE("Message"),
    
    /**
     * Push notification delivery consumers (mobile, browser push).
     * <p>
     * Used for event consumers that deliver push notifications to mobile devices
     * and web browsers for real-time user alerts.
     * </p>
     */
    PUSH_NOTIFICATION("Push Notification"),
    
    /**
     * Role and permission modification consumers (privilege updates, role reconciliation).
     * <p>
     * Used for event consumers that handle role-based access control changes,
     * privilege updates, and role reconciliation procedures.
     * </p>
     */
    ROLE_MODIFICATION("Role"),
    
    /**
     * Server-side code execution consumers (GraalVM JavaScript, dynamic scripting).
     * <p>
     * Used for event consumers that execute server-side code, including GraalVM JavaScript
     * flows and dynamic script execution within the polyglot context.
     * </p>
     */
    SERVER_SIDE_CODE("Server Side Code"),
    ;

    /**
     * Human-friendly category label for UI presentation.
     * <p>
     * This label is displayed in dropdowns, select fields, and configuration UIs
     * to provide a user-readable representation of the category.
     * </p>
     */
    private String label;

    /**
     * Constructs an EventConsumerCategory with the specified label.
     *
     * @param label Human-friendly category label for display in UI components
     */
    EventConsumerCategory(String label) {
        this.label = label;
    }

    /**
     * Returns the human-friendly category label for display in dropdowns and configuration UIs.
     * <p>
     * This method implements the {@link OptionWithLabel} interface, enabling the enum
     * to be used directly in UI select components and form fields.
     * </p>
     *
     * @return Human-friendly category label for display in dropdowns and configuration UIs
     */
    @Override
    public String getLabel() {
        return label;
    }
}
