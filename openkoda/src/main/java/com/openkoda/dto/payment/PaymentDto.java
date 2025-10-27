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
 * Payment payload DTO representing a complete payment transaction.
 * <p>
 * This mutable POJO implements {@link CanonicalObject} and {@link OrganizationRelatedObject}
 * contracts to provide canonical object identification and multi-tenant organization scoping.
 * Uses {@link BigDecimal} for monetary precision in totalAmount, netAmount, and taxAmount fields.
 * </p>
 * <p>
 * Contains nested enums for payment provider ({@link PaymentProvider}), payment classification
 * ({@link PaymentType}), and lifecycle state ({@link PaymentStatus}). Supports product line items
 * as an array of {@link PaymentProductDto} objects.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * PaymentDto payment = new PaymentDto(new BigDecimal("99.99"), "Premium Plan", PaymentType.SUBSCRIPTION, "/success");
 * payment.setProvider(PaymentProvider.stripe);
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CanonicalObject
 * @see OrganizationRelatedObject
 * @see PaymentProductDto
 */
public class PaymentDto implements CanonicalObject, OrganizationRelatedObject {

    /**
     * Payment gateway provider enumeration.
     * <p>
     * Defines the payment processor used for transaction processing.
     * </p>
     *
     * @since 1.7.1
     */
    public enum PaymentProvider {
        /** HotPay payment gateway integration */
        hotpay,
        /** Stripe payment gateway integration */
        stripe,
        /** Mock provider for testing purposes */
        mock,
        /** No payment provider specified */
        none;
    }

    /**
     * Payment classification enumeration.
     * <p>
     * Categorizes the payment based on billing model and recurring nature.
     * </p>
     *
     * @since 1.7.1
     */
    public enum PaymentType {
        /** One-time single item purchase */
        SINGLE_ITEM,
        /** Recurring subscription payment */
        SUBSCRIPTION,
        /** Usage-based metered payment */
        PAY_AS_YOU_GO
    }

    /**
     * Payment lifecycle state enumeration.
     * <p>
     * Tracks the payment transaction status from creation through completion or failure.
     * </p>
     *
     * @since 1.7.1
     */
    public enum PaymentStatus {
        /** Payment created but not yet submitted */
        NEW,
        /** Payment submitted and awaiting confirmation */
        PENDING,
        /** Payment successfully completed */
        SUCCESS,
        /** Payment failed or rejected */
        FAILURE,
        /** Payment data corrupted or inconsistent */
        CORRUPTED
    }

    /** Payment record unique identifier */
    public Long id;

    /** Total payment amount including tax (uses BigDecimal for monetary precision) */
    public BigDecimal totalAmount;

    /** Net payment amount before tax (uses BigDecimal for monetary precision) */
    public BigDecimal netAmount;

    /** Tax portion of the payment (uses BigDecimal for monetary precision) */
    public BigDecimal taxAmount;

    /** Associated subscription plan identifier */
    public String planId;

    /** Associated subscription plan name */
    public String planName;

    /** Human-readable payment description */
    public String description;

    /** Array of payment line items representing purchased products */
    public PaymentProductDto[] products;

    /** Payment gateway provider used for transaction processing */
    public PaymentProvider provider;

    /** Payment classification based on billing model */
    public PaymentType paymentType;

    /** Current lifecycle state of the payment transaction */
    public PaymentStatus status;

    /** Currency code for the payment (e.g., USD, EUR, GBP) */
    public String currency;

    /** Multi-tenant organization identifier for organization-scoped payment */
    public Long organizationId;

    /** User identifier associated with this payment */
    public Long userId;

    /** Post-payment redirect URL for user navigation after transaction completion */
    public String redirectUrl;

    /**
     * Default no-argument constructor.
     * <p>
     * Creates an empty PaymentDto instance with all fields uninitialized.
     * </p>
     */
    public PaymentDto() {
    }

    /**
     * Convenience constructor for basic payment creation.
     * <p>
     * Initializes a payment with essential transaction details.
     * </p>
     *
     * @param totalAmount total payment amount including tax (must not be null)
     * @param description human-readable payment description
     * @param paymentType payment classification (SINGLE_ITEM, SUBSCRIPTION, or PAY_AS_YOU_GO)
     * @param redirectUrl post-payment redirect URL for user navigation
     */
    public PaymentDto(BigDecimal totalAmount, String description, PaymentType paymentType, String redirectUrl) {
        this.totalAmount = totalAmount;
        this.description = description;
        this.paymentType = paymentType;
        this.redirectUrl = redirectUrl;
    }

    /**
     * Full constructor with complete payment details.
     * <p>
     * Initializes a payment with all core transaction and user context information.
     * </p>
     *
     * @param id payment record unique identifier
     * @param totalAmount total payment amount including tax
     * @param description human-readable payment description
     * @param provider payment gateway provider (hotpay, stripe, mock, or none)
     * @param paymentType payment classification
     * @param status current lifecycle state of the payment
     * @param organizationId multi-tenant organization identifier
     * @param userId user identifier associated with this payment
     */
    public PaymentDto(Long id, BigDecimal totalAmount, String description, PaymentProvider provider, PaymentType paymentType, PaymentStatus status, Long organizationId, Long userId) {
        this.id = id;
        this.totalAmount = totalAmount;
        this.description = description;
        this.provider = provider;
        this.paymentType = paymentType;
        this.status = status;
        this.organizationId = organizationId;
        this.userId = userId;
    }

    /**
     * Alternative constructor with detailed amount breakdown and plan information.
     * <p>
     * Initializes a payment with separated tax calculation and subscription plan details.
     * </p>
     *
     * @param totalAmount total payment amount including tax
     * @param netAmount net payment amount before tax
     * @param taxAmount tax portion of the payment
     * @param planId associated subscription plan identifier
     * @param planName associated subscription plan name
     * @param description human-readable payment description
     * @param status current lifecycle state of the payment
     * @param currency currency code (e.g., USD, EUR)
     * @param organizationId multi-tenant organization identifier
     */
    public PaymentDto(BigDecimal totalAmount, BigDecimal netAmount, BigDecimal taxAmount, String planId, String planName, String description, PaymentStatus status, String currency, Long organizationId) {
        this.totalAmount = totalAmount;
        this.netAmount = netAmount;
        this.taxAmount = taxAmount;
        this.planId = planId;
        this.planName = planName;
        this.description = description;
        this.status = status;
        this.currency = currency;
        this.organizationId = organizationId;
    }

    /**
     * Gets the payment record unique identifier.
     *
     * @return payment record ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the payment record unique identifier.
     *
     * @param id payment record ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the total payment amount including tax.
     *
     * @return total payment amount as BigDecimal for monetary precision
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /**
     * Sets the total payment amount including tax.
     *
     * @param totalAmount total payment amount (should not be null)
     */
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * Gets the net payment amount before tax.
     *
     * @return net payment amount as BigDecimal for monetary precision
     */
    public BigDecimal getNetAmount() {
        return netAmount;
    }

    /**
     * Sets the net payment amount before tax.
     *
     * @param netAmount net payment amount (should not be null if tax calculation is used)
     */
    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    /**
     * Gets the tax portion of the payment.
     *
     * @return tax amount as BigDecimal for monetary precision
     */
    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    /**
     * Sets the tax portion of the payment.
     *
     * @param taxAmount tax amount (should not be null if tax calculation is used)
     */
    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    /**
     * Gets the associated subscription plan identifier.
     *
     * @return plan identifier, or null if not associated with a plan
     */
    public String getPlanId() {
        return planId;
    }

    /**
     * Sets the associated subscription plan identifier.
     *
     * @param planId plan identifier
     */
    public void setPlanId(String planId) {
        this.planId = planId;
    }

    /**
     * Gets the associated subscription plan name.
     *
     * @return human-readable plan name, or null if not associated with a plan
     */
    public String getPlanName() {
        return planName;
    }

    /**
     * Sets the associated subscription plan name.
     *
     * @param planName human-readable plan name
     */
    public void setPlanName(String planName) {
        this.planName = planName;
    }

    /**
     * Gets the human-readable payment description.
     *
     * @return payment description text
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the human-readable payment description.
     *
     * @param description payment description text
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the array of payment line items.
     *
     * @return array of PaymentProductDto representing purchased products, or null if no products
     */
    public PaymentProductDto[] getProducts() {
        return products;
    }

    /**
     * Sets the array of payment line items.
     *
     * @param products array of PaymentProductDto representing purchased products
     */
    public void setProducts(PaymentProductDto[] products) {
        this.products = products;
    }

    /**
     * Gets the payment gateway provider.
     *
     * @return payment gateway provider (hotpay, stripe, mock, or none)
     */
    public PaymentProvider getProvider() {
        return provider;
    }

    /**
     * Sets the payment gateway provider.
     *
     * @param provider payment gateway provider used for transaction processing
     */
    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }

    /**
     * Gets the payment classification.
     *
     * @return payment type (SINGLE_ITEM, SUBSCRIPTION, or PAY_AS_YOU_GO)
     */
    public PaymentType getPaymentType() {
        return paymentType;
    }

    /**
     * Sets the payment classification.
     *
     * @param paymentType payment classification based on billing model
     */
    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    /**
     * Gets the current lifecycle state of the payment.
     *
     * @return payment status (NEW, PENDING, SUCCESS, FAILURE, or CORRUPTED)
     */
    public PaymentStatus getStatus() {
        return status;
    }

    /**
     * Sets the current lifecycle state of the payment.
     *
     * @param status payment lifecycle state
     */
    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    /**
     * Gets the currency code for the payment.
     *
     * @return three-letter currency code (e.g., USD, EUR, GBP)
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Sets the currency code for the payment.
     *
     * @param currency three-letter currency code (e.g., USD, EUR, GBP)
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Gets the multi-tenant organization identifier.
     * <p>
     * Implements {@link OrganizationRelatedObject#getOrganizationId()} for organization-scoped operations.
     * </p>
     *
     * @return organization identifier for multi-tenant scoping
     */
    @Override
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the multi-tenant organization identifier.
     *
     * @param organizationId organization identifier for multi-tenant scoping
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Gets the user identifier associated with this payment.
     *
     * @return user identifier, or null if not associated with a specific user
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Sets the user identifier associated with this payment.
     *
     * @param userId user identifier
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Gets the post-payment redirect URL.
     *
     * @return URL for user navigation after transaction completion
     */
    public String getRedirectUrl() {
        return redirectUrl;
    }

    /**
     * Sets the post-payment redirect URL.
     *
     * @param redirectUrl URL for user navigation after transaction completion
     */
    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    /**
     * Generates a formatted notification message summary for this payment.
     * <p>
     * Implements {@link CanonicalObject#notificationMessage()} to provide a human-readable
     * payment summary including organization ID, total amount, currency, and plan name.
     * </p>
     *
     * @return formatted notification message: "Org {orgId}: payment {amount} {currency} for plan {planName}"
     */
    @Override
    public String notificationMessage() {
        return String.format("Org %s: payment %.2f %s for plan %s", organizationId, totalAmount, currency, planName);
    }
}
