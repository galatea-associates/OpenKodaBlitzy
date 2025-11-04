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

package com.openkoda.core.configuration;

import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.form.RegisterUserForm;
import com.openkoda.service.captcha.ValidationLevel;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class for Google reCAPTCHA integration properties and initialization.
 * <p>
 * Binds reCAPTCHA configuration from application properties including site key, secret key, and validation level.
 * Propagates the site key to form definitions during initialization for client-side reCAPTCHA widget rendering.
 * Annotated with {@code @Configuration} to enable Spring bean registration and {@code @PostConstruct} lifecycle
 * for initialization. The site key is distributed to {@link FrontendMappingDefinition} and {@link RegisterUserForm}
 * for client-side reCAPTCHA widget rendering.
 * 
 * <p>
 * Binds from properties:
 * <ul>
 * <li>{@code recaptcha.site-key}: Public key for client widget</li>
 * <li>{@code recaptcha.secret-key}: Private key for server validation</li>
 * <li>{@code recaptcha.validation}: Strictness level (normal or strict)</li>
 * </ul>
 * <p>
 * The {@link #init()} method executes after bean construction to propagate the site key to static fields
 * in form infrastructure.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ValidationLevel
 * @see FrontendMappingDefinition
 * @see RegisterUserForm
 */
@Configuration
public class ReCaptchaConfiguration {


    /**
     * Google reCAPTCHA site key (public key) for client-side widget rendering.
     * <p>
     * Injected from application property {@code recaptcha.site-key}. Used in HTML forms to initialize
     * the reCAPTCHA challenge widget. Empty string default allows application startup without reCAPTCHA configured.
     * 
     * <p>
     * Propagated to {@link FrontendMappingDefinition#siteKey} and {@link RegisterUserForm#siteKey}
     * in the {@link #init()} method.
     * 
     */
    @Value("${recaptcha.site-key:}")
    public String siteKey;

    /**
     * Google reCAPTCHA secret key (private key) for server-side response verification.
     * <p>
     * Injected from application property {@code recaptcha.secret-key}. Used by CaptchaService to verify
     * user responses against the Google reCAPTCHA API. Should be kept confidential. Empty string default
     * allows application startup without reCAPTCHA configured.
     * 
     */
    @Value("${recaptcha.secret-key:}")
    public String secretKey;

    /**
     * Validation strictness level for reCAPTCHA response verification.
     * <p>
     * Injected from application property {@code recaptcha.validation} with default {@code normal}.
     * Controls how strictly reCAPTCHA responses are validated. {@code ValidationLevel.NORMAL} accepts
     * standard responses, {@code ValidationLevel.STRICT} may enforce additional checks.
     * 
     *
     * @see ValidationLevel
     */
    @Value("${recaptcha.validation:normal}")
    public ValidationLevel validationLevel;

    /**
     * Initializes form definitions with reCAPTCHA site key after bean construction.
     * <p>
     * Annotated with {@code @PostConstruct} to execute after dependency injection completes.
     * Propagates the site key to static fields in {@link FrontendMappingDefinition} and
     * {@link RegisterUserForm} so forms can render reCAPTCHA widgets. Required because forms
     * use static fields for site key access across the application.
     * 
     * <p>
     * Executes automatically during Spring bean initialization phase, after {@code @Value}
     * properties are injected but before the bean is fully available.
     * 
     *
     * @see FrontendMappingDefinition
     * @see RegisterUserForm
     */
    @PostConstruct
    public void init() {
        FrontendMappingDefinition.siteKey = siteKey;
        RegisterUserForm.siteKey = siteKey;
    }

}
