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

package com.openkoda.controller.admin;

import static com.openkoda.controller.common.URLConstants._HTML;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.form.EmailConfigForm;

/**
 * REST controller exposing HTTP endpoints for integration configuration management, specifically email/SMTP settings.
 * <p>
 * Concrete {@code @RestController} implementing admin integrations interface. Maps HTTP requests to Flow-based 
 * helper methods inherited from {@link AbstractIntegrationsController}. Provides integration configuration UI 
 * display ({@link #integrations()}) and email configuration persistence ({@link #emailConfig(EmailConfigForm, BindingResult)}). 
 * All endpoints require backend management privileges enforced via {@code @PreAuthorize}. Currently supports 
 * email/SMTP configuration (host, port, credentials), extensible for additional integration types.
 * 
 * <p>
 * <b>Request mapping:</b> Base path "/html" ({@code URLConstants._HTML})<br>
 * <b>Security:</b> Implements {@link HasSecurityRules} for privilege enforcement. All endpoints require 
 * {@code CHECK_CAN_MANAGE_BACKEND} privilege (backend system configuration)<br>
 * <b>Response types:</b> Returns HTML views via {@code ModelAndView} for UI display, HTMX fragments via 
 * {@code @ResponseBody} for AJAX form submission<br>
 * <b>Inheritance:</b> Extends {@link AbstractIntegrationsController} for Flow-based configuration helper methods 
 * ({@code getIntegrations}, {@code saveEmailConfig})<br>
 * <b>Integration context:</b> Manages system-wide integration configurations (non-tenant-specific), currently email/SMTP only<br>
 * <b>Thread-safety:</b> Stateless, thread-safe
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractIntegrationsController for Flow-based configuration implementation
 * @see com.openkoda.model.EmailConfig for email configuration entity
 * @see EmailConfigForm for form binding and validation
 */
@RestController
@RequestMapping(_HTML)
public class IntegrationsController extends AbstractIntegrationsController implements HasSecurityRules {

    /**
     * Displays integrations configuration UI with current email settings.
     * <p>
     * <b>HTTP mapping:</b> GET /html/integrations<br>
     * <b>Security:</b> {@code @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)} - requires backend management privilege<br>
     * <b>Model keys:</b>
     * <ul>
     *   <li>"emailConfig" - {@code EmailConfig} entity with SMTP host, port, username, password</li>
     *   <li>"emailConfigForm" - bound form for editing</li>
     * </ul>
     * <b>Used by:</b> Admin dashboard integrations configuration tab
     * 
     *
     * @return Object resolving to {@code ModelAndView} with view name "integrations". 
     *         Model populated by {@code getIntegrations()}
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @GetMapping(_INTEGRATIONS)
    public Object integrations() {
        debug("[integrations]");
        return getIntegrations()
                .mav("integrations");
    }

    /**
     * Persists email configuration changes via AJAX form submission.
     * <p>
     * <b>HTTP mapping:</b> POST /html/integrations/email-config<br>
     * <b>Security:</b> {@code @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)}<br>
     * <b>Response types:</b>
     * <ul>
     *   <li>On validation success: HTMX fragment "entity-forms::email-settings-form-success"</li>
     *   <li>On validation failure: HTMX fragment "entity-forms::email-settings-form-error"</li>
     * </ul>
     * <b>Model updates:</b> Model updated by {@code saveEmailConfig(form, br)} with validation results and 
     * persisted {@code emailConfig} entity<br>
     * <b>Used by:</b> AJAX form submission from email configuration panel
     * 
     * <p>
     * <b>Email config fields:</b>
     * <ul>
     *   <li>host: SMTP server hostname (e.g., smtp.gmail.com)</li>
     *   <li>port: SMTP server port (e.g., 587 for TLS, 465 for SSL, 25 for plain)</li>
     *   <li>username: SMTP authentication username</li>
     *   <li>password: SMTP authentication password</li>
     *   <li>Additional flags: TLS/SSL enablement, authentication method</li>
     * </ul>
     * 
     *
     * @param form submitted {@code EmailConfigForm} with SMTP settings (host, port, username, password, TLS/SSL flags)
     * @param br validation {@code BindingResult} for error tracking
     * @return Object resolving to HTMX fragment view
     */
    @PreAuthorize(CHECK_CAN_MANAGE_BACKEND)
    @PostMapping(_INTEGRATIONS + _EMAIL_CONFIG)
    public Object emailConfig(
            EmailConfigForm form,
            BindingResult br) {
        debug("[post] emailConfig [{}]", form);
        Object result = saveEmailConfig(form, br)
                .mav(ENTITY + "-" + FORMS + "::email-settings-form-success",
                ENTITY + "-" + FORMS + "::email-settings-form-error");
        return result;
    }
}
