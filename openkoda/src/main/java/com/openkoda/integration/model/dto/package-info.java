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

/**
 * Data transfer objects for integration configuration form binding and validation.
 * <p>
 * This package contains lightweight mutable POJOs used for Spring MVC property binding,
 * Jackson serialization, and controller/service layer data transfer. Each DTO represents
 * configuration data for third-party service integrations including Basecamp, GitHub,
 * Jira, Microsoft Teams, Slack, and Trello.

 *
 * <b>JavaBean Pattern</b>
 * <p>
 * All DTOs in this package follow the JavaBean pattern with public fields, getters/setters,
 * and implicit no-argument constructors. This design enables automatic property binding in
 * Spring MVC controllers and seamless JSON serialization via Jackson ObjectMapper.

 *
 * <b>Thread-Safety Considerations</b>
 * <p>
 * DTOs are mutable and not thread-safe by design. They are intended for single-threaded
 * request processing within controller and service methods. Callers must enforce concurrency
 * control if DTOs are shared across threads or stored in shared caches.

 *
 * <b>Sensitive Field Handling</b>
 * <p>
 * DTOs contain sensitive credentials including webhook URLs, API keys, and authentication
 * tokens. Follow these security practices:

 * <ul>
 *   <li>Store credentials in secure secret management systems (Vault, AWS Secrets Manager)</li>
 *   <li>Avoid logging DTO contents in plaintext</li>
 *   <li>Redact sensitive fields in diagnostic outputs</li>
 *   <li>Use HTTPS for all data transmission</li>
 *   <li>Apply role-based access control to configuration endpoints</li>
 * </ul>
 *
 * <b>Usage Patterns</b>
 * <p>
 * DTOs serve three primary purposes in the integration layer:

 * <ul>
 *   <li><b>Form Binding:</b> Capture user input from Spring MVC controllers via @ModelAttribute</li>
 *   <li><b>JSON Serialization:</b> Convert between Java objects and JSON via Jackson for REST APIs</li>
 *   <li><b>Data Transfer:</b> Pass configuration data between controller, service, and repository layers</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * GitHubIntegrationDto dto = new GitHubIntegrationDto();
 * dto.setApiKey(secretsManager.getGitHubKey());
 * </pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.integration.controller
 * @see com.openkoda.integration.service
 */
package com.openkoda.integration.model.dto;