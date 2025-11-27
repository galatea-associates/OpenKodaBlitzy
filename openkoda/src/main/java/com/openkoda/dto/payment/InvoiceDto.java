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
 * Data Transfer Object for invoice metadata and billing details used in payment processing.
 * <p>
 * This class implements {@link CanonicalObject} and {@link OrganizationRelatedObject} contracts
 * to provide standardized notification messaging and multi-tenant organization association.
 * It represents a mutable POJO design optimized for mapping and serialization between
 * payment service layers and external billing systems.

 * <p>
 * The class uses {@link BigDecimal} for monetary values (value and tax fields) to ensure
 * precision in financial calculations without floating-point rounding errors.

 * <p>
 * Example usage:
 * <pre>{@code
 * InvoiceDto invoice = new InvoiceDto();
 * invoice.setInvoiceIdentifier("INV-2024-001");
 * invoice.setBuyerCompanyName("Acme Corp");
 * invoice.setValue(new BigDecimal("1500.00"));
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CanonicalObject
 * @see OrganizationRelatedObject
 */
public class InvoiceDto implements CanonicalObject, OrganizationRelatedObject {

    /**
     * Seller's company name.
     * Identifies the entity issuing the invoice.
     */
    public String sellerCompanyName;
    
    /**
     * First line of seller's address.
     * Typically contains street address and building number.
     */
    public String sellerCompanyAddressLine1;
    
    /**
     * Second line of seller's address.
     * Optional field for additional address details such as suite or floor number.
     */
    public String sellerCompanyAddressLine2;
    
    /**
     * Seller's country.
     * Country name or code where the seller is located.
     */
    public String sellerCompanyCountry;
    
    /**
     * Seller's tax identification number.
     * Unique identifier for tax purposes (e.g., VAT number, EIN, or similar).
     */
    public String sellerCompanyTaxNo;

    /**
     * Buyer's company name.
     * Identifies the entity receiving the invoice and responsible for payment.
     */
    public String buyerCompanyName;
    
    /**
     * First line of buyer's address.
     * Typically contains street address and building number.
     */
    public String buyerCompanyAddressLine1;
    
    /**
     * Second line of buyer's address.
     * Optional field for additional address details such as suite or floor number.
     */
    public String buyerCompanyAddressLine2;
    
    /**
     * Buyer's country.
     * Country name or code where the buyer is located.
     */
    public String buyerCompanyCountry;
    
    /**
     * Buyer's tax identification number.
     * Unique identifier for tax purposes (e.g., VAT number, EIN, or similar).
     */
    public String buyerCompanyTaxNo;

    /**
     * Unique invoice identifier.
     * Used for tracking and referencing the invoice in billing systems.
     */
    public String invoiceIdentifier;

    /**
     * Description of invoiced item or service.
     * Provides details about what the invoice covers.
     */
    public String item;

    /**
     * Currency code for monetary values.
     * Standard three-letter currency code (e.g., USD, EUR, GBP) following ISO 4217.
     */
    public String currency;

    /**
     * Invoice total value.
     * Uses {@link BigDecimal} for monetary precision to avoid floating-point rounding errors.
     * Represents the total amount due excluding tax.
     */
    public BigDecimal value;

    /**
     * Tax amount.
     * Uses {@link BigDecimal} for monetary precision to avoid floating-point rounding errors.
     * Represents the total tax applicable to this invoice.
     */
    public BigDecimal tax;

    /**
     * Invoice creation timestamp.
     * Used as both issue date and sell date in this implementation.
     * Stored as {@link LocalDateTime} for timezone-independent representation.
     */
    public LocalDateTime createdOn;

    /**
     * Multi-tenant organization identifier.
     * Associates this invoice with a specific organization in the multi-tenant system.
     * Implements {@link OrganizationRelatedObject} contract.
     */
    public Long organizationId;

    /**
     * Returns the invoice issue date.
     * <p>
     * In this implementation, the issue date is the same as the creation timestamp.

     *
     * @return the invoice issue date as {@link LocalDateTime}, returns {@code createdOn} value
     */
    public LocalDateTime getIssueDate() {
        return createdOn;
    }

    /**
     * Returns the invoice sell date.
     * <p>
     * In this implementation, the sell date is the same as the creation timestamp.

     *
     * @return the invoice sell date as {@link LocalDateTime}, returns {@code createdOn} value
     */
    public LocalDateTime getSellDate() {
        return createdOn;
    }

    /**
     * Returns the invoice creation timestamp.
     *
     * @return the creation timestamp as {@link LocalDateTime}
     */
    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    /**
     * Sets the invoice issue date.
     * <p>
     * This method updates the {@code createdOn} field which serves as both issue date and sell date.

     *
     * @param createdOn the issue date to set
     */
    public void setIssueDate(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    /**
     * Sets the invoice sell date.
     * <p>
     * This method updates the {@code createdOn} field which serves as both issue date and sell date.

     *
     * @param createdOn the sell date to set
     */
    public void setSellDate(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    /**
     * Sets the invoice creation timestamp.
     *
     * @param createdOn the creation timestamp to set
     */
    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    /**
     * Returns the seller's company name.
     *
     * @return the seller's company name
     */
    public String getSellerCompanyName() {
        return sellerCompanyName;
    }

    /**
     * Sets the seller's company name.
     *
     * @param sellerCompanyName the seller's company name to set
     */
    public void setSellerCompanyName(String sellerCompanyName) {
        this.sellerCompanyName = sellerCompanyName;
    }

    /**
     * Returns the seller's tax identification number.
     *
     * @return the seller's tax identification number
     */
    public String getSellerCompanyTaxNo() {
        return sellerCompanyTaxNo;
    }

    /**
     * Sets the seller's tax identification number.
     *
     * @param sellerCompanyTaxNo the seller's tax identification number to set
     */
    public void setSellerCompanyTaxNo(String sellerCompanyTaxNo) {
        this.sellerCompanyTaxNo = sellerCompanyTaxNo;
    }

    /**
     * Returns the first line of seller's address.
     *
     * @return the first line of seller's address
     */
    public String getSellerCompanyAddressLine1() {
        return sellerCompanyAddressLine1;
    }

    /**
     * Sets the first line of seller's address.
     *
     * @param sellerCompanyAddressLine1 the first line of seller's address to set
     */
    public void setSellerCompanyAddressLine1(String sellerCompanyAddressLine1) {
        this.sellerCompanyAddressLine1 = sellerCompanyAddressLine1;
    }

    /**
     * Returns the second line of seller's address.
     *
     * @return the second line of seller's address
     */
    public String getSellerCompanyAddressLine2() {
        return sellerCompanyAddressLine2;
    }

    /**
     * Sets the second line of seller's address.
     *
     * @param sellerCompanyAddressLine2 the second line of seller's address to set
     */
    public void setSellerCompanyAddressLine2(String sellerCompanyAddressLine2) {
        this.sellerCompanyAddressLine2 = sellerCompanyAddressLine2;
    }

    /**
     * Returns the seller's country.
     *
     * @return the seller's country
     */
    public String getSellerCompanyCountry() {
        return sellerCompanyCountry;
    }

    /**
     * Sets the seller's country.
     *
     * @param sellerCompanyCountry the seller's country to set
     */
    public void setSellerCompanyCountry(String sellerCompanyCountry) {
        this.sellerCompanyCountry = sellerCompanyCountry;
    }

    /**
     * Returns the first line of buyer's address.
     *
     * @return the first line of buyer's address
     */
    public String getBuyerCompanyAddressLine1() {
        return buyerCompanyAddressLine1;
    }

    /**
     * Sets the first line of buyer's address.
     *
     * @param buyerCompanyAddressLine1 the first line of buyer's address to set
     */
    public void setBuyerCompanyAddressLine1(String buyerCompanyAddressLine1) {
        this.buyerCompanyAddressLine1 = buyerCompanyAddressLine1;
    }

    /**
     * Returns the second line of buyer's address.
     *
     * @return the second line of buyer's address
     */
    public String getBuyerCompanyAddressLine2() {
        return buyerCompanyAddressLine2;
    }

    /**
     * Sets the second line of buyer's address.
     *
     * @param buyerCompanyAddressLine2 the second line of buyer's address to set
     */
    public void setBuyerCompanyAddressLine2(String buyerCompanyAddressLine2) {
        this.buyerCompanyAddressLine2 = buyerCompanyAddressLine2;
    }

    /**
     * Returns the buyer's country.
     *
     * @return the buyer's country
     */
    public String getBuyerCompanyCountry() {
        return buyerCompanyCountry;
    }

    /**
     * Sets the buyer's country.
     *
     * @param buyerCompanyCountry the buyer's country to set
     */
    public void setBuyerCompanyCountry(String buyerCompanyCountry) {
        this.buyerCompanyCountry = buyerCompanyCountry;
    }

    /**
     * Returns the buyer's company name.
     *
     * @return the buyer's company name
     */
    public String getBuyerCompanyName() {
        return buyerCompanyName;
    }

    /**
     * Sets the buyer's company name.
     *
     * @param buyerCompanyName the buyer's company name to set
     */
    public void setBuyerCompanyName(String buyerCompanyName) {
        this.buyerCompanyName = buyerCompanyName;
    }

    /**
     * Returns the buyer's tax identification number.
     *
     * @return the buyer's tax identification number
     */
    public String getBuyerCompanyTaxNo() {
        return buyerCompanyTaxNo;
    }

    /**
     * Sets the buyer's tax identification number.
     *
     * @param buyerCompanyTaxNo the buyer's tax identification number to set
     */
    public void setBuyerCompanyTaxNo(String buyerCompanyTaxNo) {
        this.buyerCompanyTaxNo = buyerCompanyTaxNo;
    }

    /**
     * Returns the description of invoiced item or service.
     *
     * @return the item description
     */
    public String getItem() {
        return item;
    }

    /**
     * Sets the description of invoiced item or service.
     *
     * @param item the item description to set
     */
    public void setItem(String item) {
        this.item = item;
    }

    /**
     * Returns the currency code for monetary values.
     *
     * @return the currency code (e.g., USD, EUR, GBP)
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Sets the currency code for monetary values.
     *
     * @param currency the currency code to set (e.g., USD, EUR, GBP)
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Returns the invoice total value.
     *
     * @return the invoice value as {@link BigDecimal}
     */
    public BigDecimal getValue() {
        return value;
    }

    /**
     * Sets the invoice total value.
     *
     * @param value the invoice value to set as {@link BigDecimal}
     */
    public void setValue(BigDecimal value) {
        this.value = value;
    }

    /**
     * Returns the tax amount.
     *
     * @return the tax amount as {@link BigDecimal}
     */
    public BigDecimal getTax() {
        return tax;
    }

    /**
     * Sets the tax amount.
     *
     * @param tax the tax amount to set as {@link BigDecimal}
     */
    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    /**
     * Sets the multi-tenant organization identifier.
     *
     * @param organizationId the organization identifier to set
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Returns the multi-tenant organization identifier.
     * <p>
     * Implements {@link OrganizationRelatedObject#getOrganizationId()} contract.

     *
     * @return the organization identifier
     */
    @Override
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Returns the unique invoice identifier.
     *
     * @return the invoice identifier
     */
    public String getInvoiceIdentifier() {
        return invoiceIdentifier;
    }

    /**
     * Sets the unique invoice identifier.
     *
     * @param invoiceIdentifier the invoice identifier to set
     */
    public void setInvoiceIdentifier(String invoiceIdentifier) {
        this.invoiceIdentifier = invoiceIdentifier;
    }

    /**
     * Returns a formatted notification message summarizing the invoice.
     * <p>
     * Implements {@link CanonicalObject#notificationMessage()} contract.
     * The message includes buyer company name, issue date, invoice value, and identifier.

     *
     * @return formatted summary string containing buyer company name, issue date, value, and invoice identifier
     */
    @Override
    public String notificationMessage() {
        return String.format("Invoice for %s, issued: %tF. Stands for %.2f, with id: %s", buyerCompanyName, getIssueDate(), value, invoiceIdentifier);
    }
}
