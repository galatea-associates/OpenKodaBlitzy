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

import com.openkoda.model.EmailConfig;

/**
 * Data transfer object for email and SMTP configuration settings.
 * <p>
 * This DTO provides configuration for multi-provider email delivery supporting
 * standard SMTP servers and Mailgun API integration. It encapsulates connection
 * parameters, authentication credentials, encryption settings, and sender information
 * used by email services for notification delivery.
 * 
 * <p>
 * Implements {@link CanonicalObject} for notification message formatting and
 * {@link OrganizationRelatedObject} for multi-tenant support. Used by email services,
 * notification delivery systems, and configuration management components.
 * 
 * <p>
 * <b>Security Note:</b> Password and API keys are stored as plain String values.
 * Callers are responsible for encryption and secure handling of sensitive credentials.
 * 
 * <p>
 * <b>Design Note:</b> The {@link #getOrganizationId()} method returns the {@code id}
 * field rather than a separate organizationId field. This unusual design means the
 * configuration ID doubles as the organization identifier.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see EmailConfig
 */
public class EmailConfigDto implements CanonicalObject, OrganizationRelatedObject {

    /**
     * Email configuration identifier.
     * <p>
     * Also serves as organizationId per the {@link #getOrganizationId()} implementation.
     * 
     */
    private Long id;
    
    /**
     * SMTP server hostname.
     * <p>
     * Example values: smtp.gmail.com, smtp.office365.com, mail.example.com
     * 
     */
    private String host;
    
    /**
     * SMTP server port number.
     * <p>
     * Typical values: 587 for TLS, 465 for SSL, 25 for plain SMTP.
     * 
     */
    private Integer port;
    
    /**
     * SMTP authentication username.
     * <p>
     * Used for authenticating with the SMTP server when {@code smtpAuth} is enabled.
     * 
     */
    private String username;
    
    /**
     * SMTP authentication password.
     * <p>
     * Stored as plain text. Encryption responsibility rests with the caller.
     * 
     */
    private String password;
    
    /**
     * Email protocol identifier.
     * <p>
     * Typically "smtp" for standard SMTP or "smtps" for SMTP over SSL.
     * 
     */
    private String protocol;
    
    /**
     * Flag enabling SSL/TLS encryption for SMTP connections.
     * <p>
     * When true, establishes encrypted connection to the SMTP server.
     * 
     */
    private Boolean ssl;
    
    /**
     * Flag requiring SMTP authentication.
     * <p>
     * When true, username and password are used to authenticate with the server.
     * 
     */
    private Boolean smtpAuth;
    
    /**
     * Flag enabling STARTTLS protocol upgrade.
     * <p>
     * When true, upgrades plain connection to encrypted TLS connection during handshake.
     * 
     */
    private Boolean starttls;
    
    /**
     * Default sender email address.
     * <p>
     * Used as the "From" header in outgoing emails.
     * 
     */
    private String from;
    
    /**
     * Default reply-to email address.
     * <p>
     * Used as the "Reply-To" header in outgoing emails.
     * 
     */
    private String replyTo;
    
    /**
     * Mailgun API key for Mailgun provider integration.
     * <p>
     * Stored as plain text. Encryption responsibility rests with the caller.
     * 
     */
    private String mailgunApiKey;
    
    /**
     * Constructs an empty EmailConfigDto.
     * <p>
     * No-argument constructor for serialization frameworks and manual property setting.
     * 
     */
    public EmailConfigDto() {
    }

    /**
     * Constructs an EmailConfigDto from an EmailConfig entity.
     * <p>
     * Mapping constructor that copies only the ID from the entity. Other fields
     * remain uninitialized and must be set separately.
     * 
     *
     * @param emailConfig the EmailConfig entity to extract the ID from
     */
    public EmailConfigDto(EmailConfig emailConfig) {
        this.id = emailConfig.getId();
    }

    /**
     * Constructs an EmailConfigDto with specified ID.
     * <p>
     * Minimal constructor for ID-only initialization. Other fields remain uninitialized.
     * 
     *
     * @param id the email configuration identifier
     */
    public EmailConfigDto(Long id) {
        this.id = id;
    }

    /**
     * Returns the email configuration identifier.
     *
     * @return the configuration ID, or null if not set
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the email configuration identifier.
     *
     * @param id the configuration ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns a formatted notification message for this email configuration.
     * <p>
     * Formats the message as "EmailConfig {id}" for notification and logging purposes.
     * 
     *
     * @return formatted notification message containing the configuration ID
     */
    @Override
    public String notificationMessage() {
        return String.format("EmailConfig %s", id);
    }

    /**
     * Returns the organization identifier for this email configuration.
     * <p>
     * <b>Note:</b> This implementation returns the {@code id} field rather than
     * a separate organizationId field. This is an intentional design where the
     * configuration ID doubles as the organization identifier.
     * 
     *
     * @return the organization ID (same as configuration ID)
     */
    @Override
    public Long getOrganizationId() {
        return id;
    }

    /**
     * Returns the SMTP server hostname.
     *
     * @return the SMTP host, or null if not configured
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the SMTP server hostname.
     *
     * @param host the SMTP host to set (e.g., smtp.gmail.com)
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Returns the SMTP server port number.
     *
     * @return the SMTP port, or null if not configured
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Sets the SMTP server port number.
     *
     * @param port the SMTP port to set (typically 587, 465, or 25)
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Returns the SMTP authentication username.
     *
     * @return the SMTP username, or null if not configured
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the SMTP authentication username.
     *
     * @param username the SMTP username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the SMTP authentication password.
     *
     * @return the SMTP password in plain text, or null if not configured
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the SMTP authentication password.
     *
     * @param password the SMTP password to set (stored as plain text)
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the email protocol identifier.
     *
     * @return the protocol (typically "smtp" or "smtps"), or null if not configured
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Sets the email protocol identifier.
     *
     * @param protocol the protocol to set (typically "smtp" or "smtps")
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Returns whether SSL/TLS encryption is enabled.
     *
     * @return true if SSL/TLS is enabled, false otherwise, or null if not configured
     */
    public Boolean getSsl() {
        return ssl;
    }

    /**
     * Sets whether SSL/TLS encryption is enabled.
     *
     * @param ssl true to enable SSL/TLS encryption, false to disable
     */
    public void setSsl(Boolean ssl) {
        this.ssl = ssl;
    }

    /**
     * Returns whether SMTP authentication is required.
     *
     * @return true if SMTP authentication is required, false otherwise, or null if not configured
     */
    public Boolean getSmtpAuth() {
        return smtpAuth;
    }

    /**
     * Sets whether SMTP authentication is required.
     *
     * @param smtpAuth true to require SMTP authentication, false to disable
     */
    public void setSmtpAuth(Boolean smtpAuth) {
        this.smtpAuth = smtpAuth;
    }

    /**
     * Returns whether STARTTLS protocol upgrade is enabled.
     *
     * @return true if STARTTLS is enabled, false otherwise, or null if not configured
     */
    public Boolean getStarttls() {
        return starttls;
    }

    /**
     * Sets whether STARTTLS protocol upgrade is enabled.
     *
     * @param starttls true to enable STARTTLS, false to disable
     */
    public void setStarttls(Boolean starttls) {
        this.starttls = starttls;
    }

    /**
     * Returns the default sender email address.
     *
     * @return the "From" email address, or null if not configured
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the default sender email address.
     *
     * @param from the "From" email address to set
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Returns the default reply-to email address.
     *
     * @return the "Reply-To" email address, or null if not configured
     */
    public String getReplyTo() {
        return replyTo;
    }

    /**
     * Sets the default reply-to email address.
     *
     * @param replyTo the "Reply-To" email address to set
     */
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    /**
     * Returns the Mailgun API key.
     *
     * @return the Mailgun API key in plain text, or null if not configured
     */
    public String getMailgunApiKey() {
        return mailgunApiKey;
    }

    /**
     * Sets the Mailgun API key for Mailgun provider integration.
     *
     * @param mailgunApiKey the Mailgun API key to set (stored as plain text)
     */
    public void setMailgunApiKey(String mailgunApiKey) {
        this.mailgunApiKey = mailgunApiKey;
    }
}