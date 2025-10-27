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

package com.openkoda.dto.web;

import com.openkoda.dto.OrganizationRelatedObject;

/**
 * Data Transfer Object representing a web page associated with a specific organization in a multi-tenant environment.
 * <p>
 * This DTO extends {@link WebPage} to add organization association and verification status,
 * implementing the {@link OrganizationRelatedObject} interface to provide organization identifier access.
 * It is designed as an intentionally mutable JavaBean for use with mapping and serialization frameworks.
 * </p>
 * <p>
 * The class provides no validation or lifecycle enforcement by design, delegating such concerns to
 * service layers and authorization code. All fields are publicly accessible to facilitate data binding
 * in presentation and persistence contexts.
 * </p>
 * <p>
 * This DTO is typically used to transfer web page data between service layers, controllers, and views
 * while maintaining the organization context required for multi-tenant operations.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see WebPage
 * @see OrganizationRelatedObject
 */
public class OrganizationWebPageDto extends WebPage implements OrganizationRelatedObject {

    /**
     * Organization identifier associating this web page with a specific organization in the multi-tenant system.
     * <p>
     * May be null when the web page is not yet associated with an organization or when the association is unset.
     * This field provides the organization context required for tenant-scoped operations and access control.
     * </p>
     */
    public Long organizationId;
    
    /**
     * Verification status flag indicating whether this web page has been verified.
     * <p>
     * Defaults to false when unset. The specific meaning of "verified" depends on the business context
     * but typically indicates that the web page has passed validation or approval processes.
     * </p>
     */
    public boolean verified;

    /**
     * Constructs an OrganizationWebPageDto with the specified URL, organization identifier, and verification status.
     * <p>
     * Calls {@code super(url)} to initialize the parent {@link WebPage} with the provided URL,
     * then sets the organization association and verification status.
     * </p>
     *
     * @param url the URL string for the web page, passed to the parent WebPage constructor
     * @param organizationId the organization identifier to associate with this web page; may be null if unset
     * @param verified the verification status flag; true if verified, false otherwise
     */
    public OrganizationWebPageDto(String url, Long organizationId, boolean verified){
        super(url);
        this.organizationId = organizationId;
        this.verified = verified;
    }

    /**
     * Returns the organization identifier associated with this web page.
     * <p>
     * This method implements the {@link OrganizationRelatedObject} contract, providing access to
     * the organization context for multi-tenant operations and authorization checks.
     * </p>
     *
     * @return the organization identifier, or null if no organization is associated with this web page
     */
    @Override
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Returns the verification status of this web page.
     * <p>
     * The verification status indicates whether this web page has been verified according to
     * business-specific criteria, typically validation or approval processes.
     * </p>
     *
     * @return true if the web page is verified, false otherwise
     */
    public boolean isVerified(){
        return verified;
    }

    /**
     * Sets the verification status of this web page.
     * <p>
     * This setter provides no validation or business logic enforcement, allowing direct modification
     * of the verification status as required by mapping frameworks and service layer operations.
     * </p>
     *
     * @param verified the new verification status; true to mark as verified, false otherwise
     */
    public void setVerified(boolean verified) {
        this.verified = verified;
    }

}
