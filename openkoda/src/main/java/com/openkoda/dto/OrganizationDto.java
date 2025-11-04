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

package com.openkoda.dto;

import com.openkoda.model.Organization;

/**
 * Organization data transfer object for multi-tenant routing and branding customization.
 * <p>
 * This DTO carries organization/tenant information used throughout the application for
 * tenant identification, database routing, and UI branding. It implements
 * {@link CanonicalObject} for standardized notification message formatting and
 * {@link OrganizationRelatedObject} for multi-tenant contract support.

 * <p>
 * OrganizationDto is used by organization services for tenant provisioning,
 * by tenant resolution mechanisms for routing requests to the correct datasource,
 * and by UI customization services for applying organization-specific branding.
 * The DTO includes branding fields for visual customization (colors, logo) and
 * feature flags for trial setup and dashboard personalization.

 * <p>
 * Example usage:
 * <pre>
 * OrganizationDto dto = new OrganizationDto(organization);
 * String brandColor = dto.getMainBrandColor();
 * </pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Organization
 * @see CanonicalObject
 * @see OrganizationRelatedObject
 */
public class OrganizationDto implements CanonicalObject, OrganizationRelatedObject {

    /**
     * No-argument constructor for serialization and framework use.
     * <p>
     * Creates an empty OrganizationDto with all fields uninitialized.
     * This constructor is required by serialization frameworks and ORM tools.

     */
    public OrganizationDto() {
    }

    /**
     * Mapping constructor that creates a DTO from an Organization entity.
     * <p>
     * Copies the id, name, and assignedDatasource fields from the entity.
     * Branding fields and feature flags are not copied and remain uninitialized.

     *
     * @param organization the Organization entity to map from
     */
    public OrganizationDto(Organization organization) {
        this.id = organization.getId();
        this.name = organization.getName();
        this.assignedDatasource = organization.getAssignedDatasource();
    }

    /**
     * Minimal constructor that creates a DTO with only an organization ID.
     * <p>
     * All other fields remain uninitialized. Useful for lightweight references
     * when only the organization identifier is needed.

     *
     * @param id the organization identifier
     */
    public OrganizationDto(Long id) {
        this.id = id;
    }

    /**
     * Organization/tenant identifier.
     * <p>
     * Maps to the Organization entity primary key. Used for tenant identification,
     * database routing, and multi-tenant isolation. This field is also returned by
     * {@link #getOrganizationId()} to satisfy the OrganizationRelatedObject contract.

     */
    public Long id;

    /**
     * Organization name for display and identification.
     * <p>
     * Human-readable organization name used in UI displays, notifications,
     * and administrative interfaces. This value is included in the notification
     * message format returned by {@link #notificationMessage()}.

     */
    public String name;

    /**
     * Assigned database datasource identifier for multi-database support.
     * <p>
     * Integer identifier indicating which datasource/database this organization's
     * data resides in. Used by the multi-tenancy system to route database operations
     * to the correct datasource when multiple databases are configured.
     * Null when using the default primary datasource.

     */
    public Integer assignedDatasource;

    /**
     * Trial setup status flag.
     * <p>
     * Boolean flag indicating whether this organization is in trial setup mode.
     * When true, trial-specific features or limitations may be applied during
     * organization provisioning and configuration.

     */
    public boolean setupTrial;

    /**
     * Dashboard personalization feature flag.
     * <p>
     * Boolean flag controlling whether users in this organization can personalize
     * their dashboards. When true, enables dashboard customization features in the UI.

     */
    public boolean personalizeDashboard;

    /**
     * Primary brand color for UI theming.
     * <p>
     * Hex color code (e.g., "#FF5733") used as the primary brand color in the
     * organization's UI theme. Applied to headers, buttons, and other primary
     * UI elements to match the organization's branding.

     */
    public String mainBrandColor;

    /**
     * Secondary brand color for UI theming.
     * <p>
     * Hex color code (e.g., "#33C3FF") used as the secondary brand color in the
     * organization's UI theme. Applied to accents, secondary buttons, and
     * complementary UI elements.

     */
    public String secondBrandColor;

    /**
     * File ID reference to organization logo image.
     * <p>
     * References the File entity ID of the uploaded organization logo.
     * The logo is displayed in the application header, login pages, and
     * other branding locations. Null when no custom logo is configured.

     */
    public Long logoId;

    /**
     * Mapping constructor that creates a DTO from an Organization entity with trial flag.
     * <p>
     * Copies the id and name fields from the entity and sets the setupTrial flag.
     * This constructor is typically used during trial organization provisioning
     * when the trial status needs to be explicitly set. Other fields remain uninitialized.

     *
     * @param organization the Organization entity to map from
     * @param setupTrial true if the organization is in trial setup mode, false otherwise
     */
    public OrganizationDto(Organization organization, boolean setupTrial) {
        this.id = organization.getId();
        this.name = organization.getName();
        this.setupTrial = setupTrial;
    }

    /**
     * Returns the organization name.
     *
     * @return the organization name, or null if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the organization name.
     *
     * @param name the organization name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the organization identifier.
     *
     * @return the organization ID, or null if not set
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the organization identifier.
     *
     * @param id the organization ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns a formatted notification message for this organization.
     * <p>
     * Implements {@link CanonicalObject#notificationMessage()} to provide
     * a standardized text representation used by notification and logging subsystems.
     * The message format is "Organization [name]".

     *
     * @return formatted notification message containing the organization name
     */
    @Override
    public String notificationMessage() {
        return String.format("Organization %s", name);
    }

    /**
     * Returns the organization identifier for multi-tenant operations.
     * <p>
     * Implements {@link OrganizationRelatedObject#getOrganizationId()} by returning
     * the id field. This self-referential implementation is intentional since this DTO
     * represents the organization itself, making the organization ID the same as the DTO's ID.

     *
     * @return the organization ID, or null if not set
     */
    @Override
    public Long getOrganizationId() {
        return id;
    }

    /**
     * Returns the assigned datasource identifier.
     *
     * @return the datasource identifier for multi-database routing, or null for default datasource
     */
    public Integer getAssignedDatasource() {
        return assignedDatasource;
    }

    /**
     * Sets the assigned datasource identifier.
     *
     * @param assignedDatasource the datasource identifier to set, or null to use default
     */
    public void setAssignedDatasource(Integer assignedDatasource) {
        this.assignedDatasource = assignedDatasource;
    }

    /**
     * Checks if the organization is in trial setup mode.
     *
     * @return true if in trial setup mode, false otherwise
     */
    public boolean isSetupTrial() {
        return setupTrial;
    }

    /**
     * Sets the trial setup mode flag.
     *
     * @param setupTrial true to enable trial setup mode, false otherwise
     */
    public void setSetupTrial(boolean setupTrial) {
        this.setupTrial = setupTrial;
    }

    /**
     * Checks if dashboard personalization is enabled for this organization.
     *
     * @return true if users can personalize dashboards, false otherwise
     */
    public boolean isPersonalizeDashboard() {
        return personalizeDashboard;
    }

    /**
     * Sets the dashboard personalization feature flag.
     *
     * @param personalizeDashboard true to enable dashboard personalization, false to disable
     */
    public void setPersonalizeDashboard(boolean personalizeDashboard) {
        this.personalizeDashboard = personalizeDashboard;
    }

    /**
     * Returns the primary brand color for UI theming.
     *
     * @return hex color code (e.g., "#FF5733"), or null if not set
     */
    public String getMainBrandColor() {
        return mainBrandColor;
    }

    /**
     * Sets the primary brand color for UI theming.
     *
     * @param mainBrandColor hex color code to set, or null to clear
     */
    public void setMainBrandColor(String mainBrandColor) {
        this.mainBrandColor = mainBrandColor;
    }

    /**
     * Returns the secondary brand color for UI theming.
     *
     * @return hex color code (e.g., "#33C3FF"), or null if not set
     */
    public String getSecondBrandColor() {
        return secondBrandColor;
    }

    /**
     * Sets the secondary brand color for UI theming.
     *
     * @param secondBrandColor hex color code to set, or null to clear
     */
    public void setSecondBrandColor(String secondBrandColor) {
        this.secondBrandColor = secondBrandColor;
    }

    /**
     * Returns the logo file identifier.
     *
     * @return the File entity ID of the organization logo, or null if not set
     */
    public Long getLogoId() {
        return logoId;
    }

    /**
     * Sets the logo file identifier.
     *
     * @param logoId the File entity ID to set, or null to clear
     */
    public void setLogoId(Long logoId) {
        this.logoId = logoId;
    }
}