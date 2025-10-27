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

package com.openkoda.repository;

import org.springframework.stereotype.Repository;

import com.openkoda.core.repository.common.FunctionalRepositoryWithLongId;
import com.openkoda.model.EmailConfig;

/**
 * Spring Data JPA repository managing EmailConfig entities for SMTP configuration.
 * <p>
 * This repository extends {@code FunctionalRepositoryWithLongId<EmailConfig>} to provide
 * comprehensive persistence operations for email configuration entities. EmailConfig stores
 * SMTP server settings including host, port, authentication credentials, and TLS configuration.
 * These settings enable the application's email notification services to deliver outbound mail
 * through configured SMTP servers.
 * </p>
 * <p>
 * The repository supports organization-scoped email configurations for multi-tenant deployments,
 * allowing each tenant to maintain independent SMTP settings. Email configurations are typically
 * managed through the administration interface and applied automatically by notification services
 * when sending emails on behalf of specific organizations.
 * </p>
 * <p>
 * Inherited operations from {@code FunctionalRepositoryWithLongId} include:
 * <ul>
 *   <li>{@code save(EmailConfig)} - Persist or update email configuration</li>
 *   <li>{@code findById(Long)} - Retrieve configuration by primary key</li>
 *   <li>{@code findAll()} - List all email configurations</li>
 *   <li>{@code delete(EmailConfig)} - Remove email configuration</li>
 *   <li>{@code count()} - Count total configurations</li>
 * </ul>
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * EmailConfig config = new EmailConfig();
 * config.setSmtpHost("smtp.example.com");
 * emailConfigRepository.save(config);
 * </pre>
 * </p>
 *
 * @author mboronski
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see EmailConfig
 * @see com.openkoda.core.repository.common.FunctionalRepositoryWithLongId
 */
@Repository
public interface EmailConfigRepository extends FunctionalRepositoryWithLongId<EmailConfig> {

}
