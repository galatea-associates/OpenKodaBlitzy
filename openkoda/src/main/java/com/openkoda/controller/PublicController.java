/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 and associated documentation files (the "Software"), to deal in the Software without restriction, 
including without limitation the rights to use, copy, modify, merge, publish, distribute, 
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software 
is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice 
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE 
AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS 
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES 
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.controller;

import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.core.security.UserProvider;
import com.openkoda.core.service.ValidationService;
import com.openkoda.form.RegisterUserForm;
import com.openkoda.model.Privilege;
import com.openkoda.model.Token;
import com.openkoda.model.User;
import com.openkoda.model.authentication.LoggedUser;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import reactor.util.function.Tuple2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.openkoda.core.service.FrontendResourceService.frontendResourceTemplateNamePrefix;
import static com.openkoda.core.service.event.ApplicationEvent.USER_VERIFIED;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Public-facing controller providing unauthenticated endpoints for registration, email verification, and diagnostics.
 * <p>
 * Handles public endpoints accessible without authentication including user registration with ReCaptcha validation,
 * email verification via token, verification link resending, login form display, and diagnostic endpoints for
 * session and access testing. This controller serves as the central location for all public actions that do not
 * require authentication, complementing specialized controllers like PasswordRecoveryController and FrontendResourceController.
 * </p>
 * <p>
 * The registration flow uses {@link ValidationService} for ReCaptcha validation, delegates to services.user flows
 * for user creation, and emits USER_VERIFIED events upon successful email verification. Flash attributes are used
 * for success and error messages across redirects. Servlet session is used for diagnostic endpoints.
 * </p>
 * <p>
 * Thread-safety: Stateless controller with session-safe operations. Each request is handled independently.
 * </p>
 * <p>
 * Security notes: All endpoints are public by design without @PreAuthorize restrictions. ReCaptcha validation
 * prevents automated bot registrations. Diagnostic endpoints (getSessionId, hasAccess) should be disabled in
 * production environments or restricted by IP address for security.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ValidationService
 * @see com.openkoda.controller.PasswordRecoveryController
 * @see com.openkoda.controller.FrontendResourceController
 */
@Controller
public class PublicController extends AbstractController implements HasSecurityRules {

    @Value("${page.after.register:/logout}")
    private String pageAfterRegister;

    @Value("${redirect.login.page.url:}")
    private String loginPageCustomUrl;

    @Inject
    ValidationService validationService;

    /**
     * Captures initial registration attempt and redirects to registration form with email pre-filled.
     * <p>
     * HTTP mapping: POST /register-attempt or /{languagePrefix}/register-attempt
     * </p>
     * <p>
     * Stores the provided email in flash attributes and redirects to the registration form,
     * allowing the form to pre-populate the email field for user convenience.
     * </p>
     *
     * @param languagePrefix Optional language prefix for internationalization (e.g., "en", "pl"), may be null
     * @param email Email address to pre-fill in registration form
     * @param redirectAttributes Spring MVC redirect attributes for flash scope data transfer
     * @return RedirectView to registration form with email in flash attributes
     */
    @RequestMapping(value = {_REGISTER + _ATTEMPT, "/{languagePrefix:" + LANGUAGEPREFIX + "$}" + _REGISTER + _ATTEMPT}, method = POST, consumes = "application/json", headers = "content-type=application/x-www-form-urlencoded")
    @ResponseBody
    public RedirectView registerAttempt(@PathVariable(value = "languagePrefix", required = false) String languagePrefix, String email, RedirectAttributes redirectAttributes) {
        debug("[registerAttempt]");
        RedirectView rv = new RedirectView((languagePrefix == null ? "" : "/" + languagePrefix) + _REGISTER);
        rv.setExposeModelAttributes(false);
        redirectAttributes.addFlashAttribute("registerAttemptEmail", email);
        return rv;
    }

    /**
     * Processes user registration with ReCaptcha validation and form validation.
     * <p>
     * HTTP mapping: POST /register or /{languagePrefix}/register
     * </p>
     * <p>
     * Validates ReCaptcha token via {@link ValidationService#isCaptchaVerified()}, checks form validation using
     * Jakarta Bean Validation annotations, then calls services.user.registerUserOrReturnExisting to create the user
     * account. If user already exists, returns registration form with appropriate error message indicating existing
     * authentication methods. On success, returns thank-you page view and sends verification email to the user.
     * </p>
     * <p>
     * Flow: ReCaptcha validation → Form validation → User registration → Email verification → Thank-you page
     * </p>
     * <p>
     * Authentication: No authentication required - public access for new user registration
     * </p>
     *
     * @param languagePrefix Optional language prefix for internationalization (e.g., "en", "pl"), may be null
     * @param registerUserForm Form object bound from request with validation constraints applied
     * @param request HttpServletRequest for accessing cookies and session data
     * @return ModelAndView with registration error page if validation fails or user exists,
     *         thank-you page on successful registration
     */
    @RequestMapping(value = {_REGISTER, "/{languagePrefix:" + LANGUAGEPREFIX + "$}" + _REGISTER}, method = POST, consumes = "application/json", headers = "content-type=application/x-www-form-urlencoded")
    @ResponseBody
    public Object registerUser(@PathVariable(value = "languagePrefix", required = false) String languagePrefix, @ModelAttribute("registerForm") @Valid RegisterUserForm registerUserForm, HttpServletRequest request) {
        debug("[registerUser]");
        String languagePrefix_ = (languagePrefix == null ? "" : languagePrefix + "/");
        languagePrefix = (languagePrefix == null ? "" : languagePrefix);
        if(!validationService.isCaptchaVerified()){
            ModelAndView mav = new ModelAndView(frontendResourceTemplateNamePrefix + languagePrefix_ + "register");
            mav.addObject(error.name, "ReCaptcha response unverified");
            return mav;
        }
        Tuple2<User, Boolean> userOrReturnExisting = services.user.registerUserOrReturnExisting(registerUserForm, request.getCookies(), languagePrefix, true);
        if (userOrReturnExisting.getT2()) {
            User existingUser = userOrReturnExisting.getT1();
            Set<LoggedUser.AuthenticationMethods> existingMethods = existingUser.getAuthenticationMethods();
            debug("[registerUser] User with given login {} already exists with auth methods: {}", registerUserForm.getLogin(), existingMethods);
            ModelAndView mav = new ModelAndView(frontendResourceTemplateNamePrefix + languagePrefix_ + "register");
            if (existingMethods.contains(LoggedUser.AuthenticationMethods.PASSWORD)) {
                mav.addObject(error.name, "User with given login already exists.");
            } else {
                mav.addObject(error.name, "User with given email exists with different authentication methods.");
            }
            return mav;
        }
        ModelAndView mav = new ModelAndView(frontendResourceTemplateNamePrefix + languagePrefix_ + "thank-you");
        mav.addObject(userEntity.name, repositories.unsecure.user.findByLogin(registerUserForm.getLogin()));
        return mav;
    }

    /**
     * Verifies user email address using token from verification email link.
     * <p>
     * HTTP mapping: GET /register/verify?token={base64UserIdToken}
     * </p>
     * <p>
     * Validates and invalidates the verification token via services.token.verifyAndInvalidateToken, checks that
     * the token has canVerifyAccount privilege, then enables the user account and their login credentials. Emits
     * USER_VERIFIED application event for downstream processing. Returns success or failure view based on token
     * validity and verification completion.
     * </p>
     * <p>
     * Token verification ensures one-time use - tokens are invalidated after successful verification or if expired.
     * </p>
     * <p>
     * Authentication: No authentication required - public access via email link with token parameter
     * </p>
     *
     * @param base64UserIdToken Base64-encoded verification token (UUID) from email link
     * @return ModelAndView with account-verification-success view if token valid and verification succeeds,
     *         account-verification-fail view if token invalid, expired, or verification fails
     */
    @GetMapping(_REGISTER + _VERIFY)
    @ResponseBody
    public Object verifyUser(@RequestParam(VERIFY_TOKEN) String base64UserIdToken) {
        debug("[verifyUser]");

        Token token = services.token.verifyAndInvalidateToken(base64UserIdToken);

        if(token != null && token.getPrivilegesSet().contains(Privilege.canVerifyAccount)) {
            debug("[verifyUser] Trying to verify user {} with token {}", token.getUserId(), token.getId());
            User user = token.getUser();
            user.setEnabled(true);
            user.getLoginAndPassword().setEnabled(true);
            repositories.unsecure.user.saveAndFlush(user);
            services.applicationEvent.emitEvent(USER_VERIFIED, user.getBasicUser());
            return new ModelAndView(frontendResourceTemplateNamePrefix + "account-verification-success");
        }

        if (token != null) {
            warn("[verifyUser] failed verification for {} with token {}", token.getUserId(), token.getId());
        } else {
            warn("[verifyUser] failed verification");
        }

        return new ModelAndView(frontendResourceTemplateNamePrefix + "account-verification-fail");
    }

    /**
     * Resends email verification link to user's email address.
     * <p>
     * HTTP mapping: POST /resend/verification
     * </p>
     * <p>
     * Generates and sends new verification email via services.user.resendAccountVerificationEmail for users who
     * did not receive or lost their original verification email. Returns view indicating whether resend was
     * successful or if email address was not found in system.
     * </p>
     * <p>
     * Authentication: No authentication required - public access for unverified users
     * </p>
     *
     * @param email Email address to resend verification link to
     * @return ModelAndView with resend-verification view containing success or failure status
     */
    @PostMapping(_RESEND + _VERIFICATION)
    @ResponseBody
    public Object resendVerificationLink(@RequestParam String email) {
        debug("[resendVerificationLink] email: {}", email);
        ModelAndView mav = new ModelAndView(frontendResourceTemplateNamePrefix + "resend-verification");
        mav.addObject("verification", services.user.resendAccountVerificationEmail(email));
        return mav;
    }

    /**
     * Displays login form fragment for AJAX or partial page updates.
     * <p>
     * HTTP mapping: GET /login/form
     * </p>
     * <p>
     * Returns Thymeleaf fragment view (login::login-form) for dynamic login form rendering. Optional error and
     * logout parameters indicate authentication failure or successful logout, respectively. These parameters are
     * passed to the view for displaying appropriate user feedback messages.
     * </p>
     * <p>
     * Authentication: No authentication required - public access for login form display
     * </p>
     *
     * @param error Optional error parameter indicating login failure, may be null
     * @param logout Optional logout parameter indicating successful logout, may be null
     * @return ModelAndView with login form fragment view and optional error/logout status parameters
     */
    @GetMapping(_LOGIN + _FORM)
    @ResponseBody
    public Object getLoginForm(@RequestParam(required = false) String error, @RequestParam(required = false) String logout) {
        debug("[getLoginForm]");
        ModelAndView mav = new ModelAndView(frontendResourceTemplateNamePrefix + "login::login-form");
        if (error != null) {
            mav.addObject("param.error", error);
        }
        if (logout != null) {
            mav.addObject("param.logout", logout);
        }
        return mav;
    }

    /**
     * Redirects home page requests to root application path.
     * <p>
     * HTTP mapping: GET /home
     * </p>
     * <p>
     * Simple redirect handler that forwards /home requests to the application root (/). Used for consistent
     * home page routing across the application.
     * </p>
     * <p>
     * Authentication: No authentication required - public access
     * </p>
     *
     * @return ModelAndView with redirect to application root "/"
     */
    @GetMapping(_HOME)
    public Object getHome(){
        debug("[getHome]");
        return new ModelAndView(REDIRECT + "/");
    }

    /**
     * Tests file access privileges for authenticated users (diagnostic endpoint).
     * <p>
     * HTTP mapping: GET /has-file-access?id={fileId}
     * </p>
     * <p>
     * Diagnostic endpoint that checks whether the current authenticated user has access to a specific file by
     * querying repositories.secure.file.findOne. Returns HTTP 403 (Forbidden) if user is not authenticated or
     * file access is denied, HTTP 200 (OK) if user has access. Prints all request cookies to console for
     * debugging session and authentication issues.
     * </p>
     * <p>
     * Security warning: This diagnostic endpoint should be disabled or IP-restricted in production environments
     * as it exposes cookie information and internal access control logic.
     * </p>
     * <p>
     * Authentication: Public access but checks authentication status internally for access decision
     * </p>
     *
     * @param fileId File identifier to check access for
     * @param request HttpServletRequest for cookie inspection
     * @param response HttpServletResponse for setting status code (200 or 403)
     */
    @GetMapping("/has-file-access")
    public void hasAccess(
            @RequestParam(ID) Long fileId,
            HttpServletRequest request,
            HttpServletResponse response) {
        for (Cookie c : request.getCookies()) {
            System.out.println(c.getName() + " " + c.getValue());
        }
        if (!UserProvider.isAuthenticated() || repositories.secure.file.findOne(fileId) == null ) {
            response.setStatus(403);
        } else {
            response.setStatus(200);
        }
    }

    /**
     * Returns current HTTP session identifier for debugging and diagnostics.
     * <p>
     * HTTP mapping: GET /session-id
     * </p>
     * <p>
     * Diagnostic endpoint that retrieves and returns the current HttpSession ID as plain text response. Useful
     * for troubleshooting session management issues, verifying load balancer session affinity (sticky sessions),
     * and debugging distributed session storage configurations.
     * </p>
     * <p>
     * Security warning: This diagnostic endpoint exposes session identifiers and should be disabled or
     * IP-restricted in production environments to prevent session enumeration attacks.
     * </p>
     * <p>
     * Authentication: No authentication required - public access for diagnostic purposes
     * </p>
     *
     * @param request HttpServletRequest to extract session from
     * @param response HttpServletResponse (unused but available for future enhancements)
     * @return ResponseEntity containing session ID as plain text string with HTTP 200 status
     */
    @GetMapping("/session-id")
    @ResponseBody()
    public ResponseEntity<String> getSessionId(HttpServletRequest request, HttpServletResponse response) {
        // Get the HttpSession object from the HttpServletRequest
        HttpSession session = request.getSession();
        
        // Get the session ID
        String sessionId = session.getId();
        
        // Return the session ID
        Map<String, String> map = new HashMap<>();
        map.put("sessionId", sessionId);
        return ResponseEntity.ok(sessionId);
    }

}
