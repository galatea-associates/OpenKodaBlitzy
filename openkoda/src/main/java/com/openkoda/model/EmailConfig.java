package com.openkoda.model;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Formula;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openkoda.model.common.AuditableEntity;
import com.openkoda.model.common.EntityWithRequiredPrivilege;
import com.openkoda.model.common.ModelConstants;
import com.openkoda.model.common.OrganizationRelatedEntity;
import com.openkoda.model.common.SearchableEntity;
import com.openkoda.model.common.TimestampedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

/**
 * Per-organization email transport configuration entity storing SMTP settings, API credentials, and mail provider configuration.
 * <p>
 * Persisted to email_config table. Stores organization-specific email delivery configuration including Mailgun API keys,
 * SMTP credentials (username/password), and mail server settings. Enables multi-tenant email delivery with per-organization
 * mail providers. Uses @DynamicUpdate for selective column updates. organizationId is non-updatable foreign key enforcing
 * one config per organization. Includes @Formula-derived referenceString for display and requiredReadPrivilege/requiredWritePrivilege
 * for access control.
 * 
 * <p>
 * Multi-tenancy: Each organization can have distinct email provider configuration. organizationId enforced as non-updatable
 * to prevent config reassignment.
 * 
 * <p>
 * Security: Stores sensitive credentials (mailgunApiKey, password). Should be encrypted at rest in production.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Organization
 */
@Entity
@DynamicUpdate
public class EmailConfig extends TimestampedEntity implements AuditableEntity, EntityWithRequiredPrivilege, SearchableEntity, OrganizationRelatedEntity {

    private static final long serialVersionUID = -2214746736070137804L;

    @Id
    @SequenceGenerator(name = GLOBAL_ID_GENERATOR, sequenceName = GLOBAL_ID_GENERATOR, initialValue = ModelConstants.INITIAL_GLOBAL_VALUE, allocationSize = 10)
    @GeneratedValue(generator = ModelConstants.GLOBAL_ID_GENERATOR, strategy = GenerationType.SEQUENCE)
    private Long id;
    
    /**
     * Mailgun REST API key for email delivery via Mailgun service.
     * Nullable if using SMTP instead.
     */
    @Column(nullable = true, updatable = true, name = "mailgunApiKey")
    private String mailgunApiKey;
    
    @Column(nullable = true, updatable = true, name = "host")
    private String host;

    @Column(nullable = true, updatable = true, name = "port")
    private Integer port;

    /**
     * SMTP authentication username for direct mail server connection.
     * Nullable if using Mailgun API.
     */
    @Column(nullable = true, updatable = true, name = "username")
    private String username;

    /**
     * SMTP authentication password for direct mail server connection.
     * Nullable if using Mailgun API.
     */
    @Column(nullable = true, updatable = true, name = "password")
    private String password;

    @Column(nullable = true, updatable = true, name = "protocol")
    private String protocol;

    @Column(nullable = true, updatable = true, name = "ssl")
    private Boolean ssl;

    @Column(nullable = true, updatable = true, name = "smtp_auth")
    private Boolean smtpAuth;

    @Column(nullable = true, updatable = true, name = "starttls")
    private Boolean starttls;

    @Column(nullable = true, updatable = true, name = "mail_from")
    private String from;

    @Column(nullable = true, updatable = true, name = "replyTo")
    private String replyTo;
    
    @JsonIgnore
    @OneToOne(optional = true)
    @JoinColumn(nullable = true, insertable = false, updatable = false, name = ORGANIZATION_ID)
    private Organization organization;
    
    /**
     * Database formula-derived display string for UI reference.
     */
    @Formula(DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA)
    private String referenceString;
    
    /**
     * Foreign key to organization entity.
     * Non-updatable after creation (Column updatable=false) to prevent config migration between tenants.
     */
    @Column(nullable = true, updatable = false, name = ORGANIZATION_ID)
    private Long organizationId;
    
    /**
     * Database formula-derived privilege token for email config read access control.
     */
    @Formula("( '" + PrivilegeNames._canReadBackend + "' )")
    private String requiredReadPrivilege;

    /**
     * Database formula-derived privilege token for email config write access control.
     */
    @Formula("( '" + PrivilegeNames._canManageBackend + "' )")
    private String requiredWritePrivilege;
    
    @Column(name = INDEX_STRING_COLUMN, length = INDEX_STRING_COLUMN_LENGTH, insertable = false)
    @ColumnDefault("''")
    private String indexString;
    
    private static final String NAME = "emailConfig";
    
    public EmailConfig() {
        
    }

    public EmailConfig(Long id) {
        super();
        this.id = id;
    }

    public EmailConfig(Long id, Long organizationId) {
        super();
        this.id = id;
        this.organizationId = organizationId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
     * Returns stable identifier for audit trail entries.
     *
     * @return the constant name "emailConfig" for audit logging
     */
    @Override
    public String toAuditString() {
        return NAME;
    }
    
    @Override
    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    @Override
    public String getRequiredWritePrivilege() {
        return requiredWritePrivilege;
    }

    @Override
    public Long getOrganizationId() {
        return this.organizationId;
    }

    @Override
    public String getReferenceString() {
        return this.referenceString;
    }

    @Override
    public String getIndexString() {
        return indexString;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Boolean getSsl() {
        return ssl;
    }

    public void setSsl(Boolean ssl) {
        this.ssl = ssl;
    }

    public Boolean getSmtpAuth() {
        return smtpAuth;
    }

    public void setSmtpAuth(Boolean smtpAuth) {
        this.smtpAuth = smtpAuth;
    }

    public Boolean getStarttls() {
        return starttls;
    }

    public void setStarttls(Boolean starttls) {
        this.starttls = starttls;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getMailgunApiKey() {
        return mailgunApiKey;
    }

    public void setMailgunApiKey(String mailgunApiKey) {
        this.mailgunApiKey = mailgunApiKey;
    }
}
