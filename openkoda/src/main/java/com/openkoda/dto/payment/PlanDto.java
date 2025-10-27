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

package com.openkoda.dto.payment;

import com.openkoda.dto.CanonicalObject;
import com.openkoda.dto.OrganizationRelatedObject;

/**
 * Minimal plan carrier DTO for payment plan information.
 * <p>
 * This mutable POJO implements {@link CanonicalObject} and {@link OrganizationRelatedObject}
 * contracts to represent payment plan data within a multi-tenant context. The DTO contains
 * minimal fields for organization identification and plan name, suitable for data transfer
 * across application layers.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class PlanDto  implements CanonicalObject, OrganizationRelatedObject {

    /**
     * Multi-tenant organization identifier.
     * Associates this payment plan with a specific organization in the system.
     */
    public Long organizationId;
    
    /**
     * Name of the payment plan.
     * Human-readable identifier for the plan type or tier.
     */
    public String planName;

    /**
     * Default constructor for framework instantiation.
     * Creates an empty PlanDto with null fields.
     */
    public PlanDto() {
    }

    /**
     * Creates a PlanDto with organization identifier.
     * Initializes planName to an empty string.
     *
     * @param organizationId the organization identifier
     */
    public PlanDto(Long organizationId) {
        this.organizationId = organizationId;
        this.planName = "";
    }

    /**
     * Creates a PlanDto with full plan information.
     *
     * @param organizationId the organization identifier
     * @param planName the name of the payment plan
     */
    public PlanDto(Long organizationId, String planName) {
        this.organizationId = organizationId;
        this.planName = planName;
    }

    /**
     * Sets the organization identifier.
     *
     * @param organizationId the organization identifier to set
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Returns the organization identifier.
     *
     * @return the organization identifier, or null if not set
     */
    @Override
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Returns the payment plan name.
     *
     * @return the plan name, or null if not set
     */
    public String getPlanName() {
        return planName;
    }

    /**
     * Sets the payment plan name.
     *
     * @param planName the plan name to set
     */
    public void setPlanName(String planName) {
        this.planName = planName;
    }

    /**
     * Returns a formatted notification message summarizing this plan.
     * <p>
     * Generates a human-readable string containing the plan name and organization identifier
     * for use in notifications and audit logs.
     * </p>
     *
     * @return formatted summary string with planName and organizationId
     */
    @Override
    public String notificationMessage() {
        return String.format("Plan %s for organization %s.", planName, organizationId);
    }

}
