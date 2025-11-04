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

import org.springframework.validation.BindingResult;

import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.dto.EmailConfigDto;
import com.openkoda.form.EmailConfigForm;
import com.openkoda.model.EmailConfig;

/**
 * Abstract base controller providing Flow-based helper methods for integration configuration management.
 * <p>
 * This stateless abstract controller implements integration configuration UI workflows, specifically
 * for email SMTP settings. It provides email configuration retrieval via {@code findEmailConfigFlow}
 * and {@code getIntegrations}, as well as persistence through {@code saveEmailConfig} with validation.
 * 
 * <p>
 * The controller uses unsecure repository access as email configuration is system-wide rather than
 * tenant-scoped. It is designed for reuse by concrete controllers that handle HTTP bindings
 * (such as {@code @GetMapping} and {@code @PostMapping} annotations) and view resolution.
 * 
 * <p>
 * <b>Integration Context:</b> Currently supports email/SMTP configuration via the {@link EmailConfig}
 * entity, extensible for additional integration types such as OAuth providers or API keys.
 * 
 * <p>
 * <b>Thread-Safety:</b> This controller is stateless and thread-safe.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.controller.admin.IntegrationsController IntegrationsController for concrete HTTP endpoint implementation
 * @see EmailConfig Email configuration entity with SMTP host, port, and credentials
 * @see EmailConfigForm Form binding and validation for email configuration
 */
public class AbstractIntegrationsController extends AbstractController {

    /**
     * Retrieves email configuration for integrations UI display.
     * <p>
     * This method delegates to {@link #findEmailConfigFlow()} to execute the Flow pipeline that
     * fetches the system-wide email configuration and binds it to a form for display in the
     * integrations management UI.
     * 
     * <p>
     * Typical usage: Called by GET /integrations endpoint in concrete controller implementations.
     * 
     *
     * @return PageModelMap containing model keys "emailConfig" (the {@link EmailConfig} entity)
     *         and "emailConfigForm" (the bound {@link EmailConfigForm})
     * @see #findEmailConfigFlow()
     */
    protected PageModelMap getIntegrations(){
        return findEmailConfigFlow()
                .execute();
    }
    
    /**
     * Creates a Flow pipeline for email configuration retrieval.
     * <p>
     * This method initializes a Flow with the current controller context and chains operations to:
     * <ol>
     *   <li>Fetch the system-wide email configuration (or create a new instance if none exists)</li>
     *   <li>Bind the configuration to an {@link EmailConfigForm} with an empty {@link EmailConfigDto}</li>
     * </ol>
     * The Flow returns the first {@link EmailConfig} found in the system or creates a new instance,
     * as only a single system-wide email configuration is supported.
     * 
     * <p>
     * <b>Model Keys:</b>
     * <ul>
     *   <li>{@code emailConfig}: The fetched or newly created EmailConfig entity</li>
     *   <li>{@code emailConfigForm}: A new EmailConfigForm initialized with the fetched configuration</li>
     * </ul>
     * 
     *
     * @return Flow pipeline initialized with this controller context, typed as
     *         {@code Flow<Object, EmailConfigForm, AbstractIntegrationsController>}
     * @see EmailConfig
     * @see EmailConfigForm
     */
    protected Flow<Object, EmailConfigForm, AbstractIntegrationsController> findEmailConfigFlow() {
        debug("[findEmailConfigFlow]");
        return Flow.init(this)
                .thenSet(emailConfig, a -> repositories.unsecure.emailConfig.findAll().stream().findFirst().orElse(new EmailConfig()))
                .thenSet(emailConfigForm, a -> new EmailConfigForm(new EmailConfigDto(), a.result)); 
    }

    /**
     * Validates and persists email configuration submitted from the integrations UI.
     * <p>
     * This method executes a Flow pipeline that:
     * <ol>
     *   <li>Retrieves the existing system-wide email configuration (or creates new if none exists)</li>
     *   <li>Validates the submitted form using {@code services.validation.validateAndPopulateToEntity}</li>
     *   <li>Populates the entity from the validated form data</li>
     *   <li>Saves the updated configuration via {@code repositories.unsecure.emailConfig.save}</li>
     *   <li>Updates the model with the saved entity under the "emailConfig" key</li>
     * </ol>
     * The returned PageModelMap contains validation results and the updated email configuration,
     * suitable for rendering success or error view fragments (typically for HTMX/Thymeleaf AJAX forms).
     * 
     * <p>
     * <b>Validation Pattern:</b> Uses the validation service to perform Jakarta Bean Validation checks
     * and populate the entity. Validation errors are captured in the {@link BindingResult}.
     * 
     * <p>
     * <b>Repository Access:</b> Uses {@code repositories.unsecure} as email configuration is system-wide
     * and not tenant-scoped.
     * 
     *
     * @param form the submitted {@link EmailConfigForm} containing SMTP settings (host, port, username, password)
     * @param br the {@link BindingResult} for capturing validation errors
     * @return PageModelMap with validation results and the updated "emailConfig" model key containing
     *         the persisted {@link EmailConfig} entity
     * @see EmailConfigForm
     * @see EmailConfig
     * @see org.springframework.validation.BindingResult
     */
    protected PageModelMap saveEmailConfig(EmailConfigForm form, BindingResult br) {
        debug("[saveEmailConfig] emailConfig [{}, {}]", form.getDto().getId(), form.getDto().getHost());
        return Flow.init(emailConfigForm, form)
                .thenSet(emailConfigForm, a -> form)
                .then(a -> repositories.unsecure.emailConfig.findAll().stream().findFirst().orElse(new EmailConfig()))
                .then(a -> services.validation.validateAndPopulateToEntity(form, br,a.result))            
                .thenSet(emailConfig, a -> repositories.unsecure.emailConfig.save(a.result))                
                .execute();
    }
}
