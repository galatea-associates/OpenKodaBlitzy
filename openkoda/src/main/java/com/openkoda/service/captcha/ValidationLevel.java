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

package com.openkoda.service.captcha;


/**
 * Enumeration of reCAPTCHA validation strictness levels for configurable bot protection.
 * <p>
 * This enum defines threshold levels for reCAPTCHA score evaluation, allowing per-endpoint
 * configuration of bot detection strictness. The validation levels balance security requirements
 * against user experience, with higher strictness reducing false negatives (bots passing
 * verification) but potentially increasing false positives (legitimate users failing verification).

 * <p>
 * The validation behavior differs between reCAPTCHA v2 (checkbox-based) and v3 (score-based):

 * <ul>
 *   <li><b>none</b>: No validation performed - suitable for development/testing environments</li>
 *   <li><b>normal</b>: Balanced validation with moderate security (v3 score ≥ 0.5, v2 checkbox required)</li>
 *   <li><b>strict</b>: High security validation with stricter thresholds (v3 score ≥ 0.7, v2 checkbox required)</li>
 * </ul>
 * <p>
 * Configuration can be set globally via application properties or overridden per-endpoint:
 * <pre>{@code
 * // Per-endpoint override
 * captchaService.verify(token, ValidationLevel.strict);
 * }</pre>

 * <p>
 * Use case guidance:

 * <ul>
 *   <li><b>none</b>: Local development, automated testing, internal administrative tools</li>
 *   <li><b>normal</b>: Standard user-facing forms (registration, contact forms, comments)</li>
 *   <li><b>strict</b>: High-security endpoints (admin login, financial transactions, password resets)</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CaptchaService for usage in verification workflow
 */
public enum ValidationLevel {

    /**
     * Validation disabled - always passes verification regardless of reCAPTCHA response.
     * <p>
     * This level provides no bot protection and should only be used in development or testing
     * environments where reCAPTCHA verification would interfere with automated testing or
     * local development workflows.

     * <p>
     * <b>Security implication:</b> No protection against automated bots or abuse.

     * <p>
     * <b>Behavior:</b>

     * <ul>
     *   <li>reCAPTCHA v2: Ignores checkbox completion status</li>
     *   <li>reCAPTCHA v3: Ignores score value</li>
     *   <li>Always returns successful verification</li>
     * </ul>
     * <p>
     * <b>Use case:</b> Local development environments, integration testing, internal admin tools
     * without public exposure.

     */
    none,

    /**
     * Moderate validation with balanced security and usability (reCAPTCHA v3 score threshold ≥ 0.5).
     * <p>
     * This level provides standard bot protection suitable for most user-facing forms. The 0.5
     * threshold follows Google's recommended default for reCAPTCHA v3, balancing security against
     * false positive rates.

     * <p>
     * <b>False positive rate:</b> Approximately 1-2% of legitimate users may occasionally face
     * verification challenges, typically resolved by retrying.

     * <p>
     * <b>Behavior:</b>

     * <ul>
     *   <li>reCAPTCHA v3: Requires score ≥ 0.5 (Google recommended threshold)</li>
     *   <li>reCAPTCHA v2: Requires successful checkbox completion</li>
     *   <li>If Google servers unavailable: Verification may pass with warning (depending on configuration)</li>
     * </ul>
     * <p>
     * <b>Use case:</b> Standard user registration, contact forms, comment submission, newsletter
     * signup, public content submission.

     */
    normal,

    /**
     * Strict validation with high security requirements (reCAPTCHA v3 score threshold ≥ 0.7).
     * <p>
     * This level provides enhanced bot protection for high-security endpoints where false negatives
     * (bots passing verification) pose significant risk. The higher 0.7 threshold reduces bot
     * passage but increases false positive rates.

     * <p>
     * <b>False positive rate:</b> Approximately 5% of legitimate users may face verification
     * challenges, particularly users with unusual browsing patterns, VPN usage, or privacy-focused
     * browser configurations.

     * <p>
     * <b>Behavior:</b>

     * <ul>
     *   <li>reCAPTCHA v3: Requires score ≥ 0.7 (stricter than Google default)</li>
     *   <li>reCAPTCHA v2: Requires successful checkbox completion</li>
     *   <li>Verification always enforced regardless of Google server availability</li>
     * </ul>
     * <p>
     * <b>Use case:</b> Administrator login, financial transactions, password reset, API key
     * generation, sensitive data modification, account deletion.

     */
    strict

}
