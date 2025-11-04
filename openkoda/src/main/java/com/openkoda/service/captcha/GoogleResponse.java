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

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Data transfer object for Google reCAPTCHA API verification response.
 * <p>
 * This class encapsulates the JSON response received from the Google reCAPTCHA siteverify API.
 * It uses Jackson {@code @JsonProperty} annotations to map between snake_case field names
 * used in the Google API and camelCase Java field names following standard naming conventions.

 * <p>
 * The response structure varies between reCAPTCHA v2 and v3:
 * <ul>
 *   <li>v2: Returns success, challenge_ts, hostname, and error-codes</li>
 *   <li>v3: Additionally includes score (0.0-1.0) and action fields</li>
 * </ul>

 * <p>
 * This DTO is designed for automatic deserialization by Spring's RestTemplate
 * via {@code RestTemplate.getForObject(..., GoogleResponse.class)}.

 * <p>
 * Example successful reCAPTCHA v3 response:
 * <pre>
 * {
 *   "success": true,
 *   "challenge_ts": "2023-08-15T10:30:45Z",
 *   "hostname": "example.com",
 *   "score": 0.9,
 *   "action": "login"
 * }
 * </pre>

 * <p>
 * Example failed response with error codes:
 * <pre>
 * {
 *   "success": false,
 *   "error-codes": ["invalid-input-response", "timeout-or-duplicate"]
 * }
 * </pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see CaptchaService for usage context
 * @see <a href="https://developers.google.com/recaptcha/docs/verify">Google reCAPTCHA Verification API</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "success",
        "challenge_ts",
        "hostname",
        "error-codes"
})
public class GoogleResponse {

    /**
     * True if the reCAPTCHA token is valid and not expired.
     * <p>
     * This field indicates whether the verification succeeded. When false, consult
     * the {@link #errorCodes} field for details about the failure reason.

     */
    @JsonProperty("success")
    private boolean success;
    
    /**
     * ISO 8601 timestamp indicating when the reCAPTCHA challenge was completed.
     * <p>
     * Format example: "2023-08-15T10:30:45Z". This field can be used to detect
     * token replay attacks by comparing against the current time.

     */
    @JsonProperty("challenge_ts")
    private String challengeTs;
    
    /**
     * The domain name where the reCAPTCHA was solved.
     * <p>
     * This should match your application's expected domain. Verification should
     * fail if the hostname does not match to prevent token theft across domains.

     */
    @JsonProperty("hostname")
    private String hostname;
    
    /**
     * Risk score for reCAPTCHA v3 ranging from 0.0 (likely bot) to 1.0 (likely human).
     * <p>
     * This field is only present for reCAPTCHA v3 responses. Scores closer to 1.0
     * indicate higher confidence that the user is legitimate. Typical threshold
     * values range from 0.5 to 0.7 depending on security requirements.

     */
    @JsonProperty("score")
    private float score;
    
    /**
     * The action name specified in the frontend reCAPTCHA widget.
     * <p>
     * This field is only present for reCAPTCHA v3 responses. Common action values
     * include "login", "submit", "register". This allows verification of the
     * context in which the reCAPTCHA was executed.

     */
    @JsonProperty("action")
    private String action;
    
    /**
     * Array of error codes from the Google API when verification fails (success=false).
     * <p>
     * Common error codes include:
     * <ul>
     *   <li>invalid-input-response: Token is invalid or expired</li>
     *   <li>timeout-or-duplicate: Token has already been used</li>
     *   <li>missing-input-response: Token was not provided</li>
     *   <li>invalid-input-secret: Secret key is invalid</li>
     * </ul>
     * This field is null when verification succeeds.

     */
    @JsonProperty("error-codes")
    private ErrorCode[] errorCodes;

    /**
     * Determines if the verification failure was due to client-side token issues.
     * <p>
     * This method inspects the error codes and returns true when any entry indicates
     * a client-side problem such as {@link ErrorCode#InvalidResponse} or
     * {@link ErrorCode#MissingResponse}. This helps distinguish client token errors
     * from server configuration issues like invalid secret keys or server connectivity problems.

     * <p>
     * Example usage:
     * <pre>
     * if (!response.isSuccess() &amp;&amp; response.hasClientError()) {
     *     throw new ValidationException("Invalid reCAPTCHA token");
     * }
     * </pre>

     *
     * @return true if error codes contain InvalidResponse or MissingResponse, false otherwise
     */
    @JsonIgnore
    public boolean hasClientError() {
        ErrorCode[] errors = getErrorCodes();
        if(errors == null) {
            return false;
        }
        for(ErrorCode error : errors) {
            switch(error) {
                case InvalidResponse:
                case MissingResponse:
                    return true;
            }
        }
        return false;
    }

    /**
     * Enumeration of known Google reCAPTCHA API error codes.
     * <p>
     * This enum maps Google API error tokens (kebab-case strings) to Java enum constants
     * for type-safe error handling. The static lookup map maintained in this enum enables
     * O(1) normalization during JSON deserialization via the {@link #forValue(String)} method.

     * <p>
     * Error codes are returned in the "error-codes" array field when verification fails.
     * Multiple error codes may be present in a single response.

     *
     * @since 1.7.1
     */
    enum ErrorCode {
        /**
         * The secret key was not provided in the verification request.
         * <p>
         * This indicates a server configuration error where the reCAPTCHA secret
         * was not included in the API call.

         */
        MissingSecret,
        
        /**
         * The secret key is invalid or does not match the site key.
         * <p>
         * This indicates a server configuration error with incorrect credentials.
         * Verify that the secret key matches the site key configured in Google reCAPTCHA admin.

         */
        InvalidSecret,
        
        /**
         * The reCAPTCHA response token was not provided by the client.
         * <p>
         * This indicates the client failed to include the reCAPTCHA token in the request,
         * typically due to missing form data or incorrect parameter names.

         */
        MissingResponse,
        
        /**
         * The reCAPTCHA response token is invalid or has expired.
         * <p>
         * This is the most common client error, indicating the token is malformed,
         * expired (tokens expire after 2 minutes), or has already been verified.

         */
        InvalidResponse,
        
        /**
         * The verification request was malformed.
         * <p>
         * This indicates incorrect request format or missing required parameters
         * in the API call to Google's verification endpoint.

         */
        BadRequest,
        
        /**
         * The response token has already been used or the request timed out.
         * <p>
         * reCAPTCHA tokens can only be verified once. This error occurs when attempting
         * to verify the same token multiple times, or when the verification request
         * to Google's servers timed out.

         */
        TimeoutOrDuplicate;

        private static Map<String, ErrorCode> errorsMap = new HashMap<>(6);

        static {
            errorsMap.put("missing-input-secret", MissingSecret);
            errorsMap.put("invalid-input-secret", InvalidSecret);
            errorsMap.put("missing-input-response", MissingResponse);
            errorsMap.put("bad-request", InvalidResponse);
            errorsMap.put("invalid-input-response", BadRequest);
            errorsMap.put("timeout-or-duplicate", TimeoutOrDuplicate);
        }

        /**
         * Converts a Google API error code string to the corresponding enum constant.
         * <p>
         * This method is used by Jackson during JSON deserialization to map kebab-case
         * error code strings from the Google API response to Java enum constants.
         * The {@link JsonCreator} annotation enables automatic invocation during
         * deserialization of the "error-codes" array.

         *
         * @param value the error code string from Google API (e.g., "invalid-input-response")
         * @return the corresponding ErrorCode enum constant, or null if the value is unrecognized
         */
        @JsonCreator
        public static ErrorCode forValue(String value) {
            return errorsMap.get(value.toLowerCase());
        }
    }

    /**
     * Returns the verification success status.
     *
     * @return true if reCAPTCHA verification succeeded, false otherwise
     */
    @JsonProperty("success")
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the verification success status.
     *
     * @param success the verification success status to set
     */
    @JsonProperty("success")
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Returns the ISO 8601 timestamp when the challenge was completed.
     *
     * @return the challenge completion timestamp in ISO 8601 format
     */
    @JsonProperty("challenge_ts")
    public String getChallengeTs() {
        return challengeTs;
    }

    /**
     * Sets the challenge completion timestamp.
     *
     * @param challengeTs the ISO 8601 timestamp to set
     */
    @JsonProperty("challenge_ts")
    public void setChallengeTs(String challengeTs) {
        this.challengeTs = challengeTs;
    }

    /**
     * Returns the hostname where the reCAPTCHA was solved.
     *
     * @return the domain name where verification occurred
     */
    @JsonProperty("hostname")
    public String getHostname() {
        return hostname;
    }

    /**
     * Sets the hostname where the reCAPTCHA was solved.
     *
     * @param hostname the domain name to set
     */
    @JsonProperty("hostname")
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Returns the reCAPTCHA v3 risk score.
     *
     * @return the risk score (0.0 = likely bot, 1.0 = likely human), or 0.0 if not present
     */
    @JsonProperty("score")
    public float getScore() {
        return score;
    }

    /**
     * Sets the reCAPTCHA v3 risk score.
     *
     * @param score the risk score to set
     */
    @JsonProperty("score")
    public void setScore(float score) {
        this.score = score;
    }

    /**
     * Returns the action name from the reCAPTCHA v3 widget.
     *
     * @return the action name (e.g., "login", "submit"), or null if not present
     */
    @JsonProperty("action")
    public String getAction() {
        return action;
    }

    /**
     * Sets the action name from the reCAPTCHA v3 widget.
     *
     * @param action the action name to set
     */
    @JsonProperty("action")
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Sets the array of error codes from the Google API.
     *
     * @param errorCodes the error codes to set, or null if verification succeeded
     */
    @JsonProperty("error-codes")
    public void setErrorCodes(ErrorCode[] errorCodes) {
        this.errorCodes = errorCodes;
    }

    /**
     * Returns the array of error codes from the Google API.
     *
     * @return the error codes array, or null if verification succeeded
     */
    @JsonProperty("error-codes")
    public ErrorCode[] getErrorCodes() {
        return errorCodes;
    }

}