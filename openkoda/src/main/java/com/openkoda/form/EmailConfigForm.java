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

package com.openkoda.form;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;

import com.openkoda.core.form.AbstractEntityForm;
import com.openkoda.dto.EmailConfigDto;
import com.openkoda.model.EmailConfig;

/**
 * Form adapter for conditional validation of EmailConfig entity with SMTP or Mailgun configuration.
 * <p>
 * This form enforces either SMTP host or Mailgun API key validation, ensuring that
 * when SMTP configuration is provided (host), the required username, password, and from
 * fields are also present. When Mailgun is used, only the API key is required.
 * The form merges entity data using EMAIL_* field name constants via getSafeValue()
 * from the parent AbstractOrganizationRelatedEntityForm.

 * <p>
 * Validation Rules:
 * <ul>
 *   <li>Either SMTP host or Mailgun API key must be provided</li>
 *   <li>If SMTP host is provided: username, password, and from are required</li>
 *   <li>If Mailgun API key is provided: no additional fields required</li>
 * </ul>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see EmailConfig
 * @see EmailConfigDto
 * @see com.openkoda.core.form.AbstractEntityForm
 */
public class EmailConfigForm extends AbstractEntityForm<EmailConfigDto, EmailConfig> {

    /**
     * Constructs an EmailConfigForm with the provided DTO and entity for editing existing email configurations.
     * <p>
     * This constructor initializes the form with both the data transfer object containing
     * form field values and the persistent entity to be updated. The form uses the
     * emailConfigForm frontend mapping definition for field bindings and validation.

     *
     * @param emailDto the {@link EmailConfigDto} containing form field values for email configuration
     * @param entity the {@link EmailConfig} entity to be populated with validated form data
     */
    public EmailConfigForm(EmailConfigDto emailDto, EmailConfig entity) {
        super(emailDto, entity, FrontendMappingDefinitions.emailConfigForm);
    }

    /**
     * Constructs an empty EmailConfigForm for creating new email configurations.
     * <p>
     * This no-argument constructor initializes the form with default values,
     * suitable for binding new email configuration data from user input.
     * The form uses the emailConfigForm frontend mapping definition.

     */
    public EmailConfigForm() {  
        super(FrontendMappingDefinitions.emailConfigForm);
    }

    /**
     * Performs conditional validation requiring either SMTP host or Mailgun API key configuration.
     * <p>
     * This method enforces the following validation rules:
     * <ul>
     *   <li>At least one mail service must be configured: either SMTP (host) or Mailgun (mailgunApiKey)</li>
     *   <li>If SMTP host is provided, then username, password, and from address are required</li>
     *   <li>If Mailgun API key is provided without SMTP host, no additional fields are required</li>
     * </ul>
     * Validation failures are registered with the binding result using "not.empty" error codes
     * and default error messages.

     *
     * @param br the {@link BindingResult} to register validation errors
     * @return this form instance for method chaining
     * @see org.springframework.validation.BindingResult#rejectValue(String, String, String)
     */
    @Override
    public EmailConfigForm validate(BindingResult br) {
        if(StringUtils.isBlank(dto.getHost()) && StringUtils.isBlank(dto.getMailgunApiKey())) { br.rejectValue("dto.host", "not.empty", defaultErrorMessage); br.rejectValue("dto.mailgunApiKey", "not.empty", defaultErrorMessage); };
        if(StringUtils.isNotBlank(dto.getHost()) ) {
            if(StringUtils.isBlank(dto.getUsername())) { br.rejectValue("dto.username", "not.empty", defaultErrorMessage); };
            if(StringUtils.isBlank(dto.getPassword())) { br.rejectValue("dto.password", "not.empty", defaultErrorMessage); };
            if(StringUtils.isBlank(dto.getFrom())) { br.rejectValue("dto.from", "not.empty", defaultErrorMessage); };
        }

        return this;
    }

    /**
     * Transfers all EmailConfig entity fields to the form's EmailConfigDto for editing.
     * <p>
     * This method populates the form DTO with values from the persistent entity, including:
     * SMTP configuration (host, port, username, password, protocol, from, ssl, smtpAuth, starttls),
     * reply-to address, and Mailgun API key. All fields are transferred directly without transformation.

     *
     * @param entity the {@link EmailConfig} entity containing persisted email configuration
     * @return this form instance for method chaining
     */
    @Override
    public EmailConfigForm populateFrom(EmailConfig entity) {
        dto.setHost(entity.getHost());
        dto.setId(entity.getId());
        dto.setPort(entity.getPort());
        dto.setUsername(entity.getUsername());
        dto.setPassword(entity.getPassword());
        dto.setFrom(entity.getFrom());
        dto.setReplyTo(entity.getReplyTo());
        dto.setSmtpAuth(entity.getSmtpAuth());
        dto.setSsl(entity.getSsl());
        dto.setStarttls(entity.getStarttls());
        dto.setMailgunApiKey(entity.getMailgunApiKey());
        return this;
    }

    /**
     * Applies validated DTO data to the EmailConfig entity using EMAIL_* field name constants for field mapping.
     * <p>
     * This method uses getSafeValue() to merge form data with existing entity values,
     * applying EMAIL_HOST, EMAIL_PORT, EMAIL_USERNAME, EMAIL_PASSWORD, EMAIL_FROM,
     * EMAIL_REPLY_TO, EMAIL_SMTP_AUTH, EMAIL_SSL, EMAIL_STARTTLS, and EMAIL_MAILGUN_API_KEY
     * constants for safe field resolution. The getSafeValue method handles null values
     * and provides fallback behavior when form fields are not provided.

     *
     * @param entity the {@link EmailConfig} entity to be updated with validated form data
     * @return the updated entity instance
     */
    @Override
    protected EmailConfig populateTo(EmailConfig entity) {
        //entity.setName(getSafeValue(entity.getId(), NAME_));
        entity.setHost(getSafeValue(entity.getHost(), EMAIL_HOST));
        entity.setPort(getSafeValue(entity.getPort(), EMAIL_PORT));
        entity.setUsername(getSafeValue(entity.getUsername(), EMAIL_USERNAME));
        entity.setPassword(getSafeValue(entity.getPassword(), EMAIL_PASSWORD));
        entity.setFrom(getSafeValue(entity.getFrom(), EMAIL_FROM));
        entity.setReplyTo(getSafeValue(entity.getReplyTo(), EMAIL_REPLY_TO));
        entity.setSmtpAuth(getSafeValue(entity.getSmtpAuth(), EMAIL_SMTP_AUTH));
        entity.setSsl(getSafeValue(entity.getSsl(), EMAIL_SSL));
        entity.setStarttls(getSafeValue(entity.getStarttls(), EMAIL_STARTTLS));
        entity.setMailgunApiKey(getSafeValue(entity.getMailgunApiKey(), EMAIL_MAILGUN_API_KEY));
        return entity;
    }
}
