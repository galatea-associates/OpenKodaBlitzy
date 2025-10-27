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

import com.openkoda.controller.ComponentProvider;
import com.openkoda.core.audit.IpService;
import com.openkoda.core.configuration.ReCaptchaConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;

import java.net.URI;
import java.util.regex.Pattern;

import static com.openkoda.controller.common.URLConstants.CAPTCHA_VERIFIED;
import static com.openkoda.controller.common.URLConstants.RECAPTCHA_TOKEN;
import static com.openkoda.service.captcha.ValidationLevel.*;

/**
 * Google reCAPTCHA v2/v3 verification service for bot protection on forms and API endpoints.
 * <p>
 * This service integrates with Google reCAPTCHA to prevent automated bot submissions on user-facing
 * forms and public APIs. It supports both reCAPTCHA v2 (checkbox/invisible) and v3 (score-based)
 * verification workflows with configurable enforcement levels.
 * </p>
 * <p>
 * <b>Verification Workflow:</b>
 * </p>
 * <ol>
 *   <li>Frontend generates reCAPTCHA token via widget interaction</li>
 *   <li>Token submitted to backend in request parameter 'RECAPTCHA_TOKEN'</li>
 *   <li>Service extracts token and client IP address</li>
 *   <li>Token validated with Google siteverify API</li>
 *   <li>Risk score evaluated against configured ValidationLevel</li>
 *   <li>Verification result stored in request attributes as CAPTCHA_VERIFIED</li>
 * </ol>
 * <p>
 * <b>reCAPTCHA Versions:</b>
 * </p>
 * <ul>
 *   <li><b>v2 Checkbox:</b> User clicks "I'm not a robot" checkbox</li>
 *   <li><b>v2 Invisible:</b> Challenge triggered automatically on form submit</li>
 *   <li><b>v3:</b> Invisible, score-based analysis (0.0-1.0, higher = more human-like)</li>
 * </ul>
 * <p>
 * <b>ValidationLevel Configuration:</b>
 * </p>
 * <ul>
 *   <li><b>strict:</b> Requires successful Google API verification (production recommended)</li>
 *   <li><b>normal:</b> Allows verification when Google servers are unreachable (graceful degradation)</li>
 *   <li><b>none:</b> Bypasses verification entirely (development/testing only)</li>
 * </ul>
 * <p>
 * <b>Common Use Cases:</b>
 * </p>
 * <ul>
 *   <li>Login forms (prevent credential stuffing attacks)</li>
 *   <li>Registration forms (prevent fake account creation)</li>
 *   <li>Contact forms (prevent spam submissions)</li>
 *   <li>Password reset endpoints (prevent enumeration attacks)</li>
 *   <li>Public REST APIs (rate limiting and bot detection)</li>
 * </ul>
 * <p>
 * <b>Configuration Properties:</b>
 * </p>
 * <ul>
 *   <li>captcha.site.key: Public site key embedded in HTML forms</li>
 *   <li>captcha.secret.key: Secret key for server-side API verification</li>
 *   <li>captcha.validation.level: Default enforcement level (strict/normal/none)</li>
 *   <li>captcha.version: reCAPTCHA version (v2/v3)</li>
 *   <li>captcha.timeout: HTTP timeout for Google API calls (default: 5000ms)</li>
 * </ul>
 * <p>
 * <b>Usage Example:</b>
 * </p>
 * <pre>{@code
 * @PostMapping("/register")
 * public Result register(@ModelAttribute RegistrationForm form, HttpServletRequest request) {
 *     captchaService.handleCaptcha(request);
 *     if (!(Boolean)request.getAttribute(CAPTCHA_VERIFIED)) {
 *         return Result.error("Captcha verification failed");
 *     }
 *     return userService.register(form);
 * }
 * }</pre>
 * <p>
 * <b>Thread Safety:</b> This service is stateless and thread-safe. RestTemplate instances
 * are thread-safe for concurrent use across multiple requests.
 * </p>
 * <p>
 * <b>Rate Limiting:</b> Google reCAPTCHA API has rate limits (1M requests/month on free tier).
 * Consider caching verification results for repeated requests from the same session.
 * </p>
 * <p>
 * <b>Google API Error Codes:</b>
 * </p>
 * <ul>
 *   <li>missing-input-secret: Secret key parameter is missing</li>
 *   <li>invalid-input-secret: Secret key is invalid or malformed</li>
 *   <li>missing-input-response: Response token parameter is missing</li>
 *   <li>invalid-input-response: Response token is invalid or has expired</li>
 *   <li>timeout-or-duplicate: Token has already been used or request timed out</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see GoogleResponse for API response structure
 * @see ValidationLevel for enforcement levels
 * @see ReCaptchaConfiguration for configuration properties
 * @see <a href="https://developers.google.com/recaptcha">Google reCAPTCHA Documentation</a>
 */
@Service("captchaService")
public class CaptchaService extends ComponentProvider {

    /**
     * ReCAPTCHA configuration containing secret key, site key, and validation level.
     * <p>
     * Configuration properties are loaded from application.properties with keys:
     * captcha.secret.key, captcha.site.key, captcha.validation.level, captcha.version.
     * </p>
     */
    @Autowired
    ReCaptchaConfiguration configuration;
    /**
     * Service for retrieving current user's IP address for verification requests.
     * <p>
     * IP address is included in Google API verification calls for anti-abuse measures
     * and helps Google detect patterns of fraudulent activity from specific IPs.
     * </p>
     */
    @Autowired
    IpService ipService;

    /**
     * Precompiled regex pattern for validating reCAPTCHA token format: [A-Za-z0-9_-]+.
     * <p>
     * Tokens must contain only alphanumeric characters, underscores, and hyphens.
     * This validation prevents unnecessary API calls for malformed tokens.
     * Thread-safe immutable Pattern instance compiled once at class load time.
     * </p>
     */
    private static Pattern RESPONSE_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");

    /**
     * RestTemplate instance for making HTTP calls to Google reCAPTCHA verification API.
     * <p>
     * Initialized in {@link #init()} method after bean construction via @PostConstruct.
     * Thread-safe for concurrent use across multiple verification requests.
     * </p>
     */
    private RestTemplate restTemplate;

    /**
     * Initializes RestTemplate instance after bean construction.
     * <p>
     * Post-construction initialization method that creates the RestTemplate used for
     * Google API calls. Called automatically by Spring container after all dependencies
     * are injected via @PostConstruct lifecycle callback.
     * </p>
     * <p>
     * RestTemplate is thread-safe and reused for all verification requests.
     * </p>
     */
    @PostConstruct
    private void init() {
        restTemplate = new RestTemplate();
    }

    /**
     * Handles reCAPTCHA verification for incoming HTTP request.
     * <p>
     * Primary entry point for reCAPTCHA verification. Extracts token from request
     * parameter 'RECAPTCHA_TOKEN', applies configured validation level, calls Google
     * verification API, and stores result in request attributes as 'CAPTCHA_VERIFIED'.
     * </p>
     * <p>
     * <b>Workflow Steps:</b>
     * </p>
     * <ol>
     *   <li>Read validation level from configuration</li>
     *   <li>Extract captchaToken from request parameter RECAPTCHA_TOKEN</li>
     *   <li>Short-circuit if validation level is 'none' (set CAPTCHA_VERIFIED=true)</li>
     *   <li>Call {@link #processResponse(String, String)} to verify with Google</li>
     *   <li>Compare passed level with configured level</li>
     *   <li>Store verification result in RequestContextHolder attributes</li>
     * </ol>
     * <p>
     * Request attribute CAPTCHA_VERIFIED is set with scope 0 (request scope) for access
     * by downstream controllers and filters.
     * </p>
     * <p>
     * <b>Usage Example:</b>
     * </p>
     * <pre>{@code
     * captchaService.handleCaptcha(request);
     * boolean verified = (Boolean)request.getAttribute(CAPTCHA_VERIFIED);
     * }</pre>
     *
     * @param request HttpServletRequest containing reCAPTCHA token in parameter 'RECAPTCHA_TOKEN'
     * @return Always returns true (actual verification result stored in request attributes)
     */
    public boolean handleCaptcha(HttpServletRequest request){
        ValidationLevel validation = configuration.validationLevel;
        String captchaToken = request.getParameter(RECAPTCHA_TOKEN);
        if(validation == none){
            RequestContextHolder.getRequestAttributes().setAttribute(CAPTCHA_VERIFIED, true, 0);
            return true;
        }
        ValidationLevel passedLevel = processResponse(captchaToken, ipService.getCurrentUserIpAddress());
        boolean isVerified = passedLevel.compareTo(validation)>=0;
        RequestContextHolder.getRequestAttributes().setAttribute(CAPTCHA_VERIFIED, isVerified, 0);
        return true;
    }

    /**
     * Processes reCAPTCHA token by calling Google verification API.
     * <p>
     * Validates token format via {@link #responseSanityCheck(String)}, builds verification
     * URI with secret key and client IP, calls Google siteverify endpoint, and handles
     * connectivity errors gracefully.
     * </p>
     * <p>
     * <b>Verification URI Format:</b>
     * </p>
     * <pre>
     * https://www.google.com/recaptcha/api/siteverify?secret={key}&response={token}&remoteip={ip}
     * </pre>
     * <p>
     * <b>Error Handling:</b>
     * </p>
     * <ul>
     *   <li>Token format validation fails: returns ValidationLevel.none</li>
     *   <li>Google servers unreachable (ResourceAccessException): returns ValidationLevel.normal (graceful degradation)</li>
     *   <li>Other runtime exceptions: returns ValidationLevel.none</li>
     *   <li>Successful verification: returns ValidationLevel.strict</li>
     * </ul>
     * <p>
     * <b>Google API Parameters:</b>
     * </p>
     * <ul>
     *   <li>secret: Secret key from configuration</li>
     *   <li>response: reCAPTCHA token from client-side widget</li>
     *   <li>remoteip: Client IP address for anti-abuse measures</li>
     * </ul>
     *
     * @param response reCAPTCHA response token from client-side widget
     * @param clientIP Client IP address for verification request (anti-abuse)
     * @return ValidationLevel indicating verification result (strict=success, normal=connectivity issue, none=failure)
     * @see GoogleResponse for API response structure
     */
    private ValidationLevel processResponse(String response, String clientIP) {
        debug("[processResponse] {}", clientIP);
        GoogleResponse googleResponse;
        if(!responseSanityCheck(response)) {
            return none;
        }

        URI verifyUri = URI.create(String.format(
                "https://www.google.com/recaptcha/api/siteverify?secret=%s&response=%s&remoteip=%s",
                configuration.secretKey, response, clientIP));

        try{
            googleResponse = restTemplate.getForObject(verifyUri, GoogleResponse.class);
        } catch (ResourceAccessException e) {
            warn("Google recaptcha verification servers may be down",e);
            return normal;
        } catch (RuntimeException e) {
            warn("Unexpected exception",e);
            return none;
        }

        if(googleResponse.isSuccess()) {
            return strict;
        }
        return none;
    }

    /**
     * Validates reCAPTCHA token format before API call.
     * <p>
     * Performs quick validation of token format using regex pattern to avoid unnecessary
     * API calls for malformed tokens. Checks token is non-empty and matches allowed
     * character set [A-Za-z0-9_-]+.
     * </p>
     * <p>
     * This validation is a performance optimization that prevents API calls for obviously
     * invalid tokens, reducing latency and conserving API quota.
     * </p>
     *
     * @param response reCAPTCHA token to validate
     * @return true if token format is valid (non-empty and matches pattern), false otherwise
     */
    private boolean responseSanityCheck(String response) {
        return StringUtils.hasLength(response) && RESPONSE_PATTERN.matcher(response).matches();
    }

}