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
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing subscription billing state and configuration.
 * <p>
 * This mutable POJO aggregates subscription billing information including current status,
 * billing cycle timestamps, pricing details, and plan information. Implements both
 * {@link CanonicalObject} for notification message generation and
 * {@link OrganizationRelatedObject} for multi-tenant organization association.
 * </p>
 * <p>
 * Uses {@link BigDecimal} for monetary amounts (nextAmount, price) to ensure precision
 * in financial calculations. Uses {@link LocalDateTime} for billing timestamps which are
 * timezone-less and represent local billing cycle boundaries.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * SubscriptionDto dto = new SubscriptionDto(orgId, "premium", "active");
 * dto.setPrice(new BigDecimal("99.99"));
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CanonicalObject
 * @see OrganizationRelatedObject
 */
public class SubscriptionDto implements CanonicalObject, OrganizationRelatedObject {

    /**
     * Unique subscription identifier from the payment provider.
     */
    public String subscriptionId;
    
    /**
     * Timestamp of the next scheduled billing event (timezone-less).
     */
    public LocalDateTime nextBilling;
    
    /**
     * Start timestamp of the current billing period (timezone-less).
     */
    public LocalDateTime currentBillingStart;
    
    /**
     * End timestamp of the current billing period (timezone-less).
     */
    public LocalDateTime currentBillingEnd;
    
    /**
     * Amount to be charged at the next billing event (monetary precision via BigDecimal).
     */
    public BigDecimal nextAmount;
    
    /**
     * Current subscription price per billing cycle (monetary precision via BigDecimal).
     */
    public BigDecimal price;
    
    /**
     * Short name of the subscription plan (e.g., "basic", "premium").
     */
    public String planName;
    
    /**
     * Full descriptive name of the subscription plan.
     */
    public String planFullName;
    
    /**
     * Current subscription status (e.g., "active", "cancelled", "past_due").
     */
    public String subscriptionStatus;
    
    /**
     * Currency code for monetary amounts (e.g., "USD", "EUR", "GBP").
     */
    public String currency;
    
    /**
     * Multi-tenant organization identifier associating this subscription with an organization.
     */
    public Long organizationId;

    /**
     * Constructs a fully-populated subscription DTO with all billing and plan details.
     *
     * @param subscriptionId unique subscription identifier from payment provider
     * @param nextBilling timestamp of next scheduled billing event
     * @param currentBillingStart start timestamp of current billing period
     * @param currentBillingEnd end timestamp of current billing period
     * @param nextAmount amount to be charged at next billing
     * @param price current subscription price per billing cycle
     * @param planName short name of subscription plan
     * @param planFullName full descriptive name of subscription plan
     * @param subscriptionStatus current subscription status
     * @param currency currency code for monetary amounts
     * @param organizationId multi-tenant organization identifier
     */
    public SubscriptionDto(String subscriptionId,
                           LocalDateTime nextBilling,
                           LocalDateTime currentBillingStart,
                           LocalDateTime currentBillingEnd,
                           BigDecimal nextAmount,
                           BigDecimal price,
                           String planName,
                           String planFullName,
                           String subscriptionStatus,
                           String currency,
                           Long organizationId) {
        this.subscriptionId = subscriptionId;
        this.nextBilling = nextBilling;
        this.currentBillingStart = currentBillingStart;
        this.currentBillingEnd = currentBillingEnd;
        this.nextAmount = nextAmount;
        this.price = price;
        this.planName = planName;
        this.planFullName = planFullName;
        this.subscriptionStatus = subscriptionStatus;
        this.currency = currency;
        this.organizationId = organizationId;
    }

    /**
     * Constructs a subscription DTO with only organization identifier set.
     * Other fields remain null and must be populated separately.
     *
     * @param organizationId multi-tenant organization identifier
     */
    public SubscriptionDto(Long organizationId){
        this.organizationId = organizationId;
    }

    /**
     * Constructs a subscription DTO with organization, plan name, and status.
     * Useful for creating partial subscription representations with minimal required fields.
     *
     * @param organizationId multi-tenant organization identifier
     * @param planName short name of subscription plan
     * @param subscriptionStatus current subscription status
     */
    public SubscriptionDto(Long organizationId,  String planName, String subscriptionStatus){
        this.organizationId = organizationId;
        this.planName = planName;
        this.subscriptionStatus = subscriptionStatus;
    }

    /**
     * Default no-argument constructor for framework instantiation and serialization.
     * All fields remain null and must be populated via setters.
     */
    public SubscriptionDto(){
    }

    /**
     * Returns the unique subscription identifier from the payment provider.
     *
     * @return subscription identifier, may be null
     */
    public String getSubscriptionId() {
        return subscriptionId;
    }

    /**
     * Sets the unique subscription identifier from the payment provider.
     *
     * @param subscriptionId subscription identifier to set
     */
    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    /**
     * Returns the timestamp of the next scheduled billing event.
     *
     * @return next billing timestamp (timezone-less), may be null
     */
    public LocalDateTime getNextBilling() {
        return nextBilling;
    }

    /**
     * Sets the timestamp of the next scheduled billing event.
     *
     * @param nextBilling next billing timestamp to set
     */
    public void setNextBilling(LocalDateTime nextBilling) {
        this.nextBilling = nextBilling;
    }

    /**
     * Returns the start timestamp of the current billing period.
     *
     * @return current billing period start timestamp (timezone-less), may be null
     */
    public LocalDateTime getCurrentBillingStart() {
        return currentBillingStart;
    }

    /**
     * Sets the start timestamp of the current billing period.
     *
     * @param currentBillingStart billing period start timestamp to set
     */
    public void setCurrentBillingStart(LocalDateTime currentBillingStart) {
        this.currentBillingStart = currentBillingStart;
    }

    /**
     * Returns the end timestamp of the current billing period.
     *
     * @return current billing period end timestamp (timezone-less), may be null
     */
    public LocalDateTime getCurrentBillingEnd() {
        return currentBillingEnd;
    }

    /**
     * Sets the end timestamp of the current billing period.
     *
     * @param currentBillingEnd billing period end timestamp to set
     */
    public void setCurrentBillingEnd(LocalDateTime currentBillingEnd) {
        this.currentBillingEnd = currentBillingEnd;
    }

    /**
     * Returns the amount to be charged at the next billing event.
     *
     * @return next billing amount with monetary precision, may be null
     */
    public BigDecimal getNextAmount() {
        return nextAmount;
    }

    /**
     * Sets the amount to be charged at the next billing event.
     *
     * @param nextAmount next billing amount to set
     */
    public void setNextAmount(BigDecimal nextAmount) {
        this.nextAmount = nextAmount;
    }

    /**
     * Returns the short name of the subscription plan.
     *
     * @return plan name (e.g., "basic", "premium"), may be null
     */
    public String getPlanName() {
        return planName;
    }

    /**
     * Sets the short name of the subscription plan.
     *
     * @param planName plan name to set
     */
    public void setPlanName(String planName) {
        this.planName = planName;
    }

    /**
     * Returns the full descriptive name of the subscription plan.
     *
     * @return full plan name, may be null
     */
    public String getPlanFullName() {
        return planFullName;
    }

    /**
     * Sets the full descriptive name of the subscription plan.
     *
     * @param planFullName full plan name to set
     */
    public void setPlanFullName(String planFullName) {
        this.planFullName = planFullName;
    }

    /**
     * Returns the current subscription status.
     *
     * @return subscription status (e.g., "active", "cancelled", "past_due"), may be null
     */
    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    /**
     * Sets the current subscription status.
     *
     * @param subscriptionStatus subscription status to set
     */
    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    /**
     * Returns the current subscription price per billing cycle.
     *
     * @return subscription price with monetary precision, may be null
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Sets the current subscription price per billing cycle.
     *
     * @param price subscription price to set
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * Returns the currency code for monetary amounts.
     *
     * @return currency code (e.g., "USD", "EUR", "GBP"), may be null
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Sets the currency code for monetary amounts.
     *
     * @param currency currency code to set
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Returns the multi-tenant organization identifier.
     *
     * @return organization identifier, may be null
     */
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the multi-tenant organization identifier.
     *
     * @param organizationId organization identifier to set
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Generates a formatted notification message summarizing the subscription state.
     * <p>
     * The message includes the plan name, current billing end date, subscription status,
     * and price. This implementation satisfies the {@link CanonicalObject} contract.
     * </p>
     *
     * @return formatted summary string with plan name, billing end date, status, and price
     */
    @Override
    public String notificationMessage() {
        return String.format("Subscription of %s plan, ends %tF. Status now is %s. Price: %.2f", planName, currentBillingEnd, subscriptionStatus, price);
    }
}
