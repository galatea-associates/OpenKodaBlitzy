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

package com.openkoda.integration.model;

import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.PrivilegeGroup;

/**
 * Enumeration of integration module privileges for role-based access control.
 * <p>
 * This enum defines privilege tokens for external service integrations with numeric IDs,
 * groups, and visibility flags. Each privilege controls access to specific integration
 * capabilities like Slack, Microsoft Teams, GitHub, Jira, Basecamp, and Trello.
 * </p>
 * <p>
 * Numeric ID Computation: The effective privilege ID is calculated by multiplying the
 * base ID (passed to constructor) by the idOffset of 1000. For example, canIntegrateWithSlack
 * has base ID 1, resulting in effective ID 1000 (1 * 1000). This offset ensures integration
 * privileges occupy a distinct ID range (1000-1999) separate from other privilege categories.
 * </p>
 * <p>
 * Immutability and Thread-Safety: Enum constants are immutable and thread-safe by design.
 * The privilege ID and label are set once during enum initialization and never change.
 * </p>
 * <p>
 * Breaking Change Implications: Modifying enum constant names or IDs breaks existing
 * role assignments in the database. Changes to the idOffset value affect all privilege
 * IDs and require database migration. Adding new constants is backward-compatible.
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2019-10-02
 * @see PrivilegeBase
 * @see IntegrationPrivilegeName
 */
public enum IntegrationPrivilege implements PrivilegeBase, IntegrationPrivilegeName {

    /**
     * Grants permission to integrate with Slack messaging platform.
     * <p>
     * Purpose: Enables users to configure Slack webhooks, send notifications to Slack channels,
     * and manage Slack integration settings within their organization.
     * </p>
     * <p>
     * Effective ID: 1000 (base ID 1 * idOffset 1000)<br>
     * Privilege Group: null (integration privileges have no group assignment)<br>
     * Visibility: Public (shown in role configuration UI)<br>
     * Category: Integration
     * </p>
     */
    canIntegrateWithSlack(1, "Slack", _canIntegrateWithSlack),
    
    /**
     * Grants permission to integrate with Microsoft Teams collaboration platform.
     * <p>
     * Purpose: Enables users to configure Microsoft Teams webhooks, post messages to Teams
     * channels, and manage Teams integration settings within their organization.
     * </p>
     * <p>
     * Effective ID: 2000 (base ID 2 * idOffset 1000)<br>
     * Privilege Group: null (integration privileges have no group assignment)<br>
     * Visibility: Public (shown in role configuration UI)<br>
     * Category: Integration
     * </p>
     */
    canIntegrateWithMsTeams(2, "Ms Teams", _canIntegrateWithMsTeams),
    
    /**
     * Grants permission to integrate with GitHub version control platform.
     * <p>
     * Purpose: Enables users to configure GitHub OAuth, access repository data, manage
     * webhooks, and synchronize GitHub events within their organization.
     * </p>
     * <p>
     * Effective ID: 3000 (base ID 3 * idOffset 1000)<br>
     * Privilege Group: null (integration privileges have no group assignment)<br>
     * Visibility: Public (shown in role configuration UI)<br>
     * Category: Integration
     * </p>
     */
    canIntegrateWithGitHub(3, "GitHub", _canIntegrateWithGitHub),
    
    /**
     * Grants permission to integrate with Jira project management platform.
     * <p>
     * Purpose: Enables users to configure Jira OAuth, access issue data, create and update
     * Jira tickets, and manage Jira webhooks within their organization.
     * </p>
     * <p>
     * Effective ID: 4000 (base ID 4 * idOffset 1000)<br>
     * Privilege Group: null (integration privileges have no group assignment)<br>
     * Visibility: Public (shown in role configuration UI)<br>
     * Category: Integration
     * </p>
     */
    canIntegrateWithJira(4, "Jira", _canIntegrateWithJira),
    
    /**
     * Grants permission to integrate with Basecamp project management platform.
     * <p>
     * Purpose: Enables users to configure Basecamp OAuth, access project data, manage
     * to-dos and messages, and synchronize Basecamp events within their organization.
     * </p>
     * <p>
     * Effective ID: 5000 (base ID 5 * idOffset 1000)<br>
     * Privilege Group: null (integration privileges have no group assignment)<br>
     * Visibility: Public (shown in role configuration UI)<br>
     * Category: Integration
     * </p>
     */
    canIntegrateWithBasecamp(5, "Basecamp", _canIntegrateWithBasecamp),
    
    /**
     * Grants permission to integrate with Trello task management platform.
     * <p>
     * Purpose: Enables users to configure Trello OAuth, access board and card data,
     * manage Trello webhooks, and synchronize Trello events within their organization.
     * </p>
     * <p>
     * Effective ID: 6000 (base ID 6 * idOffset 1000)<br>
     * Privilege Group: null (integration privileges have no group assignment)<br>
     * Visibility: Public (shown in role configuration UI)<br>
     * Category: Integration
     * </p>
     */
    canIntegrateWithTrello(6, "Trello", _canIntegrateWithTrello);

    /**
     * Computed privilege ID calculated as base ID multiplied by idOffset.
     * <p>
     * This value is set during enum initialization by multiplying the base ID
     * (passed to constructor) by the idOffset of 1000. The resulting IDs range
     * from 1000 to 6999, creating a distinct ID space for integration privileges.
     * Used for privilege matching in role assignments and security checks.
     * </p>
     */
    private Long id;
    
    /**
     * Human-readable display name for the privilege shown in the UI.
     * <p>
     * Examples: "Slack", "Ms Teams", "GitHub", "Jira", "Basecamp", "Trello".
     * This label appears in role configuration screens and privilege reports.
     * </p>
     */
    private String label;
    
    /**
     * Category classification for grouping related privileges in the UI.
     * <p>
     * All integration privileges use the default category "Integration".
     * This category helps organize privileges in administrative interfaces
     * and distinguishes integration capabilities from other privilege types.
     * </p>
     */
    private String category = "Integration";

    /**
     * Constructs an integration privilege with computed ID and label.
     * <p>
     * The constructor calculates the effective privilege ID by multiplying the base ID
     * by 1000, validates the enum constant name matches the canonical token, and stores
     * the display label. This ensures consistent privilege identification across the system.
     * </p>
     *
     * @param id the base privilege ID (1-6) that will be multiplied by idOffset (1000)
     *           to produce effective IDs in the range 1000-6000
     * @param label the human-readable display name shown in UI components, such as
     *              "Slack", "GitHub", or "Jira"
     * @param nameCheck the canonical privilege token string used to validate that the
     *                  enum constant name matches the expected naming convention defined
     *                  in IntegrationPrivilegeName interface
     */
    IntegrationPrivilege(long id, String label, String nameCheck) {
        this.id = id * this.idOffset();
        this.label = label;
        checkName(nameCheck);
    }

    /**
     * Returns the human-readable display label for this privilege.
     * <p>
     * The label is shown in role configuration UI, privilege reports, and
     * administrative interfaces to identify the integration capability.
     * </p>
     *
     * @return the display name such as "Slack", "GitHub", or "Jira"
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * Returns the privilege group for organizational purposes.
     * <p>
     * Integration privileges return null as they do not belong to a specific
     * privilege group. This distinguishes them from other privilege categories
     * that may be organized into hierarchical groups for UI presentation.
     * </p>
     *
     * @return null for all integration privileges (no group assignment)
     */
    @Override
    public PrivilegeGroup getGroup() {
        return null;
    }

    /**
     * Returns the category classification for UI grouping.
     * <p>
     * All integration privileges belong to the "Integration" category,
     * which helps organize and filter privileges in administrative screens
     * and distinguishes integration capabilities from other privilege types.
     * </p>
     *
     * @return the category string "Integration" for all integration privileges
     */
    @Override
    public String getCategory() {
        return category;
    }

    /**
     * Returns the computed privilege ID used for role assignments.
     * <p>
     * The ID is calculated during enum initialization as base ID multiplied by
     * idOffset (1000), producing values in the range 1000-6000. This ID is used
     * for privilege matching in security checks and database role assignments.
     * </p>
     *
     * @return the effective privilege ID (e.g., 1000 for Slack, 3000 for GitHub)
     */
    @Override
    public Long getId() {
        return id;
    }
    
    /**
     * Returns the ID multiplier used to compute effective privilege IDs.
     * <p>
     * The idOffset of 1000 ensures integration privileges occupy a distinct
     * ID range (1000-6999) separate from other privilege categories. This value
     * multiplies the base ID to produce the effective privilege ID.
     * </p>
     *
     * @return 1000 as the ID multiplier for all integration privileges
     */
    @Override
    public int idOffset() { return 1000; }
}
