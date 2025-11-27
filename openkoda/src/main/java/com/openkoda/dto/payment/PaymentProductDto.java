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

import java.math.BigDecimal;

/**
 * Payment line item DTO representing an individual product in a payment transaction.
 * <p>
 * This mutable POJO implements {@link CanonicalObject} for notification formatting
 * and {@link OrganizationRelatedObject} for multi-tenant organization scoping.
 * Designed for data transfer and serialization between layers without direct entity exposure.
 * 
 * <p>
 * Uses {@link BigDecimal} for price field to ensure monetary precision and avoid
 * floating-point rounding errors in financial calculations.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CanonicalObject
 * @see OrganizationRelatedObject
 */
public class PaymentProductDto implements CanonicalObject, OrganizationRelatedObject {

    /**
     * Payment product record identifier.
     * <p>
     * Primary key for the payment product entity.
     * May be null for new instances not yet persisted.
     * 
     */
    public Long id;

    /**
     * Product price using BigDecimal for monetary precision.
     * <p>
     * Ensures accurate financial calculations without floating-point rounding errors.
     * Should represent the total price for this line item in the payment.
     * 
     */
    public BigDecimal price;

    /**
     * Name or description of the product or service.
     * <p>
     * Human-readable label identifying what is being purchased.
     * Used in notifications and display formatting.
     * 
     */
    public String itemName;

    /**
     * Reference to the parent payment record identifier.
     * <p>
     * Foreign key linking this product line item to its containing payment transaction.
     * 
     */
    public Long paymentId;

    /**
     * Multi-tenant organization identifier.
     * <p>
     * Scopes this payment product to a specific organization for tenant isolation.
     * Required by {@link OrganizationRelatedObject} contract.
     * 
     */
    public Long organizationId;

    /**
     * Constructs a new PaymentProductDto with all fields initialized.
     * <p>
     * Full-argument constructor for easy fixture creation and complete initialization.
     * 
     *
     * @param id the payment product record identifier, may be null for new instances
     * @param price the product price as BigDecimal for monetary precision
     * @param itemName the name or description of the product or service
     * @param paymentId the reference to parent payment record identifier
     * @param organizationId the multi-tenant organization identifier
     */
    public PaymentProductDto(Long id, BigDecimal price, String itemName, Long paymentId, Long organizationId) {
        this.id = id;
        this.price = price;
        this.itemName = itemName;
        this.paymentId = paymentId;
        this.organizationId = organizationId;
    }

    /**
     * Returns a formatted notification message summarizing this payment product.
     * <p>
     * Implements {@link CanonicalObject#notificationMessage()} to provide
     * a human-readable summary including organization ID, item name, and price.
     * Format: "Org {organizationId}: Item {itemName} price {price}"
     * 
     *
     * @return formatted summary string with organizationId, itemName, and price
     */
    @Override
    public String notificationMessage() {
        return String.format("Org %s: Item %s price %.2f", organizationId, itemName, price);
    }

    /**
     * Returns the payment product record identifier.
     *
     * @return the id value, may be null for unpersisted instances
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the payment product record identifier.
     *
     * @param id the payment product identifier to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the product price.
     *
     * @return the price as BigDecimal for monetary precision
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Sets the product price.
     *
     * @param price the product price to set as BigDecimal
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * Returns the name or description of the product.
     *
     * @return the item name string
     */
    public String getItemName() {
        return itemName;
    }

    /**
     * Sets the name or description of the product.
     *
     * @param itemName the product name to set
     */
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    /**
     * Returns the parent payment record identifier.
     *
     * @return the payment identifier linking to the parent payment
     */
    public Long getPaymentId() {
        return paymentId;
    }

    /**
     * Sets the parent payment record identifier.
     *
     * @param paymentId the payment identifier to set
     */
    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    /**
     * Returns the multi-tenant organization identifier.
     * <p>
     * Implements {@link OrganizationRelatedObject#getOrganizationId()}
     * for tenant isolation and scoping.
     * 
     *
     * @return the organization identifier for tenant scoping
     */
    @Override
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the multi-tenant organization identifier.
     *
     * @param organizationId the organization identifier to set
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }
}
