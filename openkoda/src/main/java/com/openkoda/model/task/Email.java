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

package com.openkoda.model.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openkoda.model.Organization;
import com.openkoda.model.common.AuditableEntityOrganizationRelated;
import com.openkoda.model.file.File;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Represents an email task entity for asynchronous email delivery within the OpenKoda platform.
 * <p>
 * This class extends {@link Task} and uses single-table inheritance with discriminator value "email"
 * to store email-specific information in the tasks table. Email entities are created when the application
 * needs to send email notifications, and are subsequently processed by the EmailSenderJob scheduler.
 * </p>
 * <p>
 * <strong>Email Workflow:</strong>
 * </p>
 * <ol>
 *   <li>Email entity created with recipient, subject, content, and optional attachments</li>
 *   <li>Entity persisted to database with pending status</li>
 *   <li>EmailSenderJob periodically queries for pending emails</li>
 *   <li>Email dispatched via configured SMTP server or Mailgun API</li>
 *   <li>Delivery status tracked via attempts counter and task status</li>
 * </ol>
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 *   <li>SMTP and Mailgun integration support for email dispatch</li>
 *   <li>HTML content rendering via content field (up to 65535 characters)</li>
 *   <li>Multiple file attachments via lazy-loaded files collection</li>
 *   <li>Organization scoping for multi-tenant email isolation</li>
 *   <li>Sender override capability via optional sender field</li>
 *   <li>Retry mechanism inherited from Task base class</li>
 * </ul>
 * <p>
 * <strong>Persistence Details:</strong>
 * </p>
 * <ul>
 *   <li>Table: tasks (shared with Task hierarchy via @DiscriminatorValue)</li>
 *   <li>File attachments: Stored in file_reference join table with organization_related_entity_id and file_id columns</li>
 *   <li>Organization association: ManyToOne relationship for tenant isolation</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * Email email = new Email("System", "user@example.com", "User Name", "<p>Welcome!</p>", "Welcome to OpenKoda");
 * email.setOrganizationId(orgId);
 * emailRepository.save(email);
 * }</pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.core.job.EmailSenderJob
 * @see Task
 * @see AuditableEntityOrganizationRelated
 */
@Entity
@DiscriminatorValue("email")
public class Email extends Task implements AuditableEntityOrganizationRelated {

    /**
     * Formula for generating reference string for organization-related entity auditing.
     * Inherited from {@link AuditableEntityOrganizationRelated}.
     */
    public static final String REFERENCE_FORMULA = DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA;
    
    /**
     * List of content properties used for search indexing.
     * Contains "content" field to enable full-text search of email body.
     */
    final static List<String> contentProperties = Arrays.asList("content");

    /**
     * Sender display name used in email "From" header.
     * Combined with email address to form "Name &lt;email&gt;" format via {@link #getFullFrom(String)}.
     */
    private String nameFrom;
    
    /**
     * Recipient email address (required).
     * Primary destination for email delivery.
     */
    private String emailTo;
    
    /**
     * Recipient display name used in email "To" header.
     * Combined with email address to form "Name &lt;email&gt;" format via {@link #getFullTo()}.
     */
    private String nameTo;

    /**
     * Email body content with HTML support.
     * Maximum length of 65535 characters (TEXT column type in database).
     * Supports HTML tags for rich formatting in email clients.
     */
    @Column(length = 65535)
    private String content;
    
    /**
     * Email subject line displayed in recipient's inbox.
     */
    private String subject;

    /**
     * Organization association for multi-tenant email isolation.
     * <p>
     * ManyToOne relationship to {@link Organization} entity providing tenant scope for this email.
     * Mapped to organization_id column in tasks table. Read-only association (insertable=false, updatable=false).
     * </p>
     * <p>
     * Note: TODO Rule 4.4 indicates this should use FetchType.LAZY for performance optimization.
     * Currently uses default EAGER fetching which may impact performance with large result sets.
     * </p>
     *
     * @see #organizationId
     */
    //TODO Rule 4.4: should be marked with FetchType = LAZY
    @JsonIgnore
    @ManyToOne(optional = true)
    @JoinColumn(nullable = true, insertable = false, updatable = false, name = ORGANIZATION_ID)
    private Organization organization;
    
    /**
     * Scalar organization ID for tenant scoping and write operations.
     * <p>
     * Primary field for setting organization association when creating/updating emails.
     * Mapped to organization_id column in tasks table. Nullable to support system-wide emails.
     * </p>
     *
     * @see #organization
     */
    @Column(nullable = true, name = ORGANIZATION_ID)
    private Long organizationId;

    /**
     * Optional external URL for file attachment reference.
     * <p>
     * Provides alternative attachment mechanism via URL instead of database-stored files.
     * Can reference cloud storage, CDN, or external document repositories.
     * </p>
     */
    private String attachmentURL;

    /**
     * Lazy-loaded collection of {@link File} attachments for this email.
     * <p>
     * ManyToMany relationship via file_reference join table with columns:
     * </p>
     * <ul>
     *   <li>organization_related_entity_id: References this email's ID (read-only)</li>
     *   <li>file_id: References attached File entity ID</li>
     *   <li>sequence: OrderColumn for maintaining attachment order</li>
     * </ul>
     * <p>
     * Foreign key constraint disabled (NO_CONSTRAINT) for flexibility.
     * Files are fetched lazily to optimize performance when email metadata is queried.
     * Use {@link #filesId} for write operations and file association management.
     * </p>
     *
     * @see #filesId
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(
            name="file_reference",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT),
            inverseJoinColumns =  @JoinColumn(name = "file_id"),
            joinColumns = @JoinColumn(name = "organization_related_entity_id", insertable = false, updatable = false)
    )
    @JsonIgnore
    @OrderColumn(name="sequence")
    protected List<File> files;

    /**
     * Collection of file IDs representing email attachments.
     * <p>
     * ElementCollection stored in file_reference table with columns:
     * </p>
     * <ul>
     *   <li>organization_related_entity_id: References this email's ID</li>
     *   <li>file_id: File identifier value</li>
     *   <li>sequence: OrderColumn for maintaining attachment order</li>
     * </ul>
     * <p>
     * This collection maps to the same file_reference table as {@link #files} but provides
     * scalar ID access for efficient file association without loading full File entities.
     * Foreign key constraint disabled (NO_CONSTRAINT) for operational flexibility.
     * Initialized to empty ArrayList for immediate use.
     * </p>
     *
     * @see #files
     */
    @ElementCollection(fetch = FetchType.LAZY, targetClass = Long.class)
    @CollectionTable(name = "file_reference", joinColumns = @JoinColumn(name = "organization_related_entity_id"), foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @Column(name="file_id")
    @OrderColumn(name="sequence")
    protected List<Long> filesId = new ArrayList<>();
    
    /**
     * Optional sender email address override.
     * <p>
     * When specified, overrides the default system sender configured in application properties.
     * Persisted to 'sender' column in tasks table. Nullable to use system default sender
     * when not explicitly set. Useful for organization-specific or branded email sending.
     * </p>
     */
    @Column(name = "sender", nullable = true)
    protected String sender = null;

    /**
     * Default no-argument constructor for Email entity.
     * <p>
     * Required by JPA for entity instantiation during database queries.
     * Creates an empty Email with all fields uninitialized except filesId (empty list).
     * Use setter methods or parameterized constructor to populate email data.
     * </p>
     */
    public Email() {
    }

    /**
     * Parameterized constructor for creating Email with core fields.
     * <p>
     * Convenience constructor for quick email creation with essential data.
     * Additional fields like organizationId, attachments, and sender must be
     * set separately via setter methods after construction.
     * </p>
     *
     * @param nameFrom sender display name for email "From" header
     * @param emailTo recipient email address (required)
     * @param nameTo recipient display name for email "To" header
     * @param content email body content with optional HTML formatting
     * @param subject email subject line
     */
    public Email(String nameFrom, String emailTo, String nameTo, String content, String subject) {
        this.nameFrom = nameFrom;
        this.emailTo = emailTo;
        this.nameTo = nameTo;
        this.content = content;
        this.subject = subject;
    }

    /**
     * Returns formatted sender address for SMTP email headers.
     * <p>
     * Combines sender display name with email address in RFC 5322 format: "Name &lt;email&gt;".
     * Used by EmailSenderJob when constructing email message headers for delivery.
     * </p>
     *
     * @param emailFrom sender email address to combine with nameFrom display name
     * @return formatted sender string as "Name &lt;email&gt;" for email headers
     * @see #getNameFrom()
     */
    public String getFullFrom(String emailFrom) {
        return String.format("%s <%s>", nameFrom, emailFrom);
    }

    /**
     * Returns formatted recipient address for SMTP email headers.
     * <p>
     * Combines recipient display name with email address in RFC 5322 format: "Name &lt;email&gt;".
     * Used by EmailSenderJob when constructing email message headers for delivery.
     * </p>
     *
     * @return formatted recipient string as "Name &lt;email&gt;" for email headers
     * @see #getEmailTo()
     * @see #getNameTo()
     */
    public String getFullTo() {
        return String.format("%s <%s>", nameTo, emailTo);
    }

    /**
     * Gets the sender display name used in email "From" header.
     *
     * @return sender display name, or null if not set
     * @see #getFullFrom(String)
     */
    public String getNameFrom() {
        return nameFrom;
    }

    /**
     * Sets the sender display name for email "From" header.
     *
     * @param nameFrom sender display name to set
     */
    public void setNameFrom(String nameFrom) {
        this.nameFrom = nameFrom;
    }

    /**
     * Gets the recipient email address.
     *
     * @return recipient email address (required field)
     * @see #getFullTo()
     */
    public String getEmailTo() {
        return emailTo;
    }

    /**
     * Sets the recipient email address.
     *
     * @param emailTo recipient email address (required)
     */
    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    /**
     * Gets the recipient display name used in email "To" header.
     *
     * @return recipient display name, or null if not set
     * @see #getFullTo()
     */
    public String getNameTo() {
        return nameTo;
    }

    /**
     * Sets the recipient display name for email "To" header.
     *
     * @param nameTo recipient display name to set
     */
    public void setNameTo(String nameTo) {
        this.nameTo = nameTo;
    }

    /**
     * Gets the email body content.
     *
     * @return email body content with optional HTML formatting, maximum 65535 characters
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the email body content.
     *
     * @param content email body content with optional HTML tags, maximum 65535 characters
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Gets the email subject line.
     *
     * @return email subject line
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets the email subject line.
     *
     * @param subject email subject line to display in recipient inbox
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Returns audit trail identifier for this email entity.
     * <p>
     * Provides human-readable string representation for audit logs and change tracking.
     * Format: "ID: {emailId}" where emailId is the primary key value.
     * </p>
     *
     * @return audit trail string containing email ID
     */
    @Override
    public String toAuditString() {
        return "ID: " + this.getId();
    }

    /**
     * Returns list of searchable content properties for indexing.
     * <p>
     * Identifies which entity fields should be included in full-text search indexing.
     * For Email entities, returns collection containing "content" field to enable
     * searching email body text.
     * </p>
     *
     * @return collection of property names for content indexing, containing "content"
     */
    @Override
    public Collection<String> contentProperties() {
        return contentProperties;
    }

    /**
     * Gets the organization associated with this email for tenant isolation.
     *
     * @return organization entity providing tenant scope, or null if system-wide email
     * @see #getOrganizationId()
     */
    public Organization getOrganization() {
        return organization;
    }

    /**
     * Sets the organization association for multi-tenant email scoping.
     * <p>
     * Note: This is a read-only association (insertable=false, updatable=false).
     * Use {@link #setOrganizationId(Long)} for write operations.
     * </p>
     *
     * @param organization organization entity to associate with this email
     * @see #setOrganizationId(Long)
     */
    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    /**
     * Gets the organization ID for tenant scoping.
     *
     * @return organization primary key, or null for system-wide emails
     * @see #getOrganization()
     */
    @Override
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Computed reference string for entity identification in audit trails.
     * <p>
     * Generated via @Formula using REFERENCE_FORMULA inherited from
     * {@link AuditableEntityOrganizationRelated}. Provides standardized
     * reference format for organization-related entities.
     * </p>
     */
    @Formula(REFERENCE_FORMULA)
    private String referenceString;

    /**
     * Gets the computed reference string for audit identification.
     *
     * @return reference string generated via database formula
     * @see #REFERENCE_FORMULA
     */
    @Override
    public String getReferenceString() {
        return referenceString;
    }

    /**
     * Sets the organization ID for tenant scoping and write operations.
     * <p>
     * Primary method for associating email with organization when creating or updating.
     * Set to null for system-wide emails not scoped to specific tenant.
     * </p>
     *
     * @param organizationId organization primary key, or null for system-wide
     * @see #setOrganization(Organization)
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Sets the external attachment URL reference.
     * <p>
     * Provides alternative to database-stored file attachments by referencing
     * external resources via URL (cloud storage, CDN, document repositories).
     * </p>
     *
     * @param attachmentURL external URL for attachment reference, or null for none
     */
    public void setAttachmentURL(String attachmentURL) {
        this.attachmentURL = attachmentURL;
    }

    /**
     * Gets the external attachment URL reference.
     *
     * @return external URL for attachment, or null if not set
     */
    public String getAttachmentURL() {
        return attachmentURL;
    }

    /**
     * Returns debug string representation of email entity.
     * <p>
     * Format: "{id}, {emailTo}, attempts:{attempts}" for logging and debugging.
     * Includes email ID, recipient address, and delivery attempt counter.
     * </p>
     *
     * @return string representation with ID, recipient, and attempts count
     */
    @Override
    public String toString() {
        return getId() + ", " + emailTo + ", attempts:" + getAttempts();
    }

    /**
     * Gets the lazy-loaded collection of file attachments.
     * <p>
     * Returns list of {@link File} entities attached to this email.
     * Collection is loaded lazily from database when first accessed.
     * For write operations, use {@link #getFilesId()} to manage associations.
     * </p>
     *
     * @return list of attached File entities, or null if not initialized
     * @see #getFilesId()
     */
    public List<File> getFiles() {
        return files;
    }

    /**
     * Sets the file attachments collection.
     * <p>
     * Note: For managing file associations, prefer using {@link #setFilesId(List)}
     * with file IDs to avoid unnecessary entity loading.
     * </p>
     *
     * @param files list of File entities to attach to this email
     * @see #setFilesId(List)
     */
    public void setFiles(List<File> files) {
        this.files = files;
    }

    /**
     * Gets the collection of file IDs representing attachments.
     * <p>
     * Returns list of file primary keys without loading full File entities.
     * Preferred for efficient file association management and queries.
     * </p>
     *
     * @return list of file IDs, initialized to empty ArrayList
     * @see #getFiles()
     */
    public List<Long> getFilesId() {
        return filesId;
    }

    /**
     * Sets the file attachment IDs collection.
     * <p>
     * Primary method for managing file attachments without loading full entities.
     * Updates file_reference table associations with provided file IDs.
     * </p>
     *
     * @param filesId list of file primary keys to attach to this email
     * @see #setFiles(List)
     */
    public void setFilesId(List<Long> filesId) {
        this.filesId = filesId;
    }

    /**
     * Gets the optional sender email address override.
     *
     * @return sender email override, or null to use system default
     */
    public String getSender() {
        return sender;
    }

    /**
     * Sets the sender email address override.
     * <p>
     * When set, overrides system-configured default sender for this email.
     * Useful for organization-specific or branded email sending.
     * Set to null to use application default sender configuration.
     * </p>
     *
     * @param sender sender email address override, or null for system default
     */
    public void setSender(String sender) {
        this.sender = sender;
    }
}
