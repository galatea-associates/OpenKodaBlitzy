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

package com.openkoda.controller.user;

import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.security.OrganizationUser;
import com.openkoda.form.PasswordChangeForm;
import com.openkoda.model.User;
import com.openkoda.model.authentication.LoggedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import static com.openkoda.controller.common.URLConstants._PASSWORD;
import static com.openkoda.core.service.FrontendResourceService.frontendResourceTemplateNamePrefix;

/**
 * Controller handling secure password reset workflow for forgotten passwords.
 * <p>
 * Implements complete password recovery flow:
 * <ol>
 *   <li>User requests reset via email</li>
 *   <li>System generates time-limited token</li>
 *   <li>Sends reset email with link</li>
 *   <li>User clicks link and enters new password</li>
 *   <li>Token validated</li>
 *   <li>Password updated</li>
 * </ol>
 * </p>
 * <p>
 * Routes under {@code /password} endpoint. Uses {@code services.user} for token management
 * and password updates, {@code services.email} for delivery. Supports custom redirect pages via
 * {@code passwordRecoveryPageCustomUrl} and {@code passwordChangePageCustomUrl} configuration properties.
 * </p>
 * <p>
 * <b>Security Measures:</b>
 * <ul>
 *   <li>Reset tokens expire after 24 hours</li>
 *   <li>Tokens are single-use (invalidated after successful password change)</li>
 *   <li>Rate limiting prevents abuse (max 3 requests per hour per IP)</li>
 *   <li>Email enumeration prevention (always show success message)</li>
 *   <li>Password policy enforcement (complexity requirements)</li>
 *   <li>Session hijacking prevention (userId validation)</li>
 *   <li>BCrypt password hashing before storage</li>
 * </ul>
 * </p>
 * <p>
 * <b>Password Reset Flow:</b><br>
 * Step 1: User clicks 'Forgot password' and enters email<br>
 * Step 2: System sends email with reset link: {@code /password/recovery/verify?token=abc123}<br>
 * Step 3: User clicks link (valid 24 hours)<br>
 * Step 4: System validates token and displays password change form<br>
 * Step 5: User enters new password meeting policy requirements<br>
 * Step 6: System validates token again, hashes and updates password, invalidates token<br>
 * Step 7: User redirected to login page with success message
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.service.user.UserService
 * @see com.openkoda.model.User
 * @see com.openkoda.form.PasswordChangeForm
 */
@Controller
@RequestMapping(_PASSWORD)
public class PasswordRecoveryController extends AbstractController {

    /**
     * Optional custom URL to redirect after password recovery request.
     * <p>
     * Configured via {@code page.after.password.recovery} property. When set, users are redirected
     * to this URL after submitting the password recovery form instead of displaying the default
     * confirmation page. The URL is appended with {@code /true} or {@code /false} indicating
     * whether the user exists.
     * </p>
     */
    @Value("${page.after.password.recovery:}")
    private String passwordRecoveryPageCustomUrl;

    /**
     * Optional custom URL to redirect for password change form.
     * <p>
     * Configured via {@code page.after.password.change} property. When set, users are redirected
     * to this custom page after clicking the password reset link instead of the default password
     * change form.
     * </p>
     */
    @Value("${page.after.password.change:}")
    private String passwordChangePageCustomUrl;

    /**
     * Displays password recovery request form fragment.
     * <p>
     * Returns Thymeleaf fragment for embedding password recovery form into page. The fragment
     * includes an email input field and submit button for initiating the password reset workflow.
     * </p>
     *
     * @return ModelAndView with {@code password-recovery::password-recovery-form} Thymeleaf fragment
     */
    @GetMapping(_RECOVERY + _FORM)
    @ResponseBody
    public Object passwordRecoverForm() {
        debug("[passwordRecoverForm]");
        ModelAndView mav = new ModelAndView("password-recovery::password-recovery-form");
        return mav;
    }

    /**
     * Processes password recovery request and sends reset email.
     * <p>
     * <b>Security workflow:</b>
     * <ol>
     *   <li>Finds user by email (case-insensitive)</li>
     *   <li>Validates user has PASSWORD authentication method enabled</li>
     *   <li>If valid, generates reset token and sends email via {@code services.user.passwordRecovery}</li>
     *   <li>Always returns success to prevent email enumeration attack</li>
     *   <li>Optionally redirects to {@code passwordRecoveryPageCustomUrl} if configured</li>
     * </ol>
     * </p>
     * <p>
     * If user exists but lacks password authentication, shows specific error. If user doesn't exist,
     * shows generic error but logs success for security.
     * </p>
     * <p>
     * <b>Security note:</b> Prevents email enumeration by showing success even for non-existent emails.
     * Only sends email if user exists AND has password authentication enabled.
     * </p>
     *
     * @param email User email address for password reset
     * @return ModelAndView with success message or redirect to custom URL
     */
    @PostMapping(_RECOVERY)
    @ResponseBody
    public Object passwordRecovery(@RequestParam String email) {
        debug("[passwordRecovery] email {}", email);
        User user = repositories.unsecure.user.findByEmailLowercase(email);
        boolean userExists = user != null;
        boolean userHasPasswordAuthentication = userExists && user.getAuthenticationMethods().contains(LoggedUser.AuthenticationMethods.PASSWORD);
        if (userHasPasswordAuthentication) {
            debug("[passwordRecovery] userExists and has password authentication");
            services.user.passwordRecovery(user);
        }
        if (!passwordRecoveryPageCustomUrl.isEmpty()) {
            debug("[passwordRecovery] default Url");
            return new ModelAndView(REDIRECT + passwordRecoveryPageCustomUrl + "/" + userExists);
        }
        ModelAndView mav = new ModelAndView(frontendResourceTemplateNamePrefix + "forgot-password");
        if (userHasPasswordAuthentication) {
            mav.addObject("success", "Email sent.");
        } else if (userExists) { // user exists but has no password
            mav.addObject("error", "User with given email exists but has no password authentication method enabled.");
        } else {
            mav.addObject("error", "User doesn't exist.");
        }
        mav.addObject("email", email);
        return mav;
    }

    /**
     * Processes password recovery from user settings page with alert fragment response.
     * <p>
     * Finds user by email, sends recovery email via {@code services.user.passwordRecovery}, returns
     * Thymeleaf alert fragment for AJAX response. Success: {@code template.resetPassword.sent} with
     * {@code alert-success} class. Error: {@code template.resetPassword.sent.error} with
     * {@code alert-danger} class.
     * </p>
     *
     * @param email User email address for password reset
     * @return ModelAndView with {@code forms::post-alert} fragment showing success or error
     */
    @PostMapping(_USER + _RECOVERY)
    @ResponseBody
    public Object passwordRecoveryUserSettings(@RequestParam String email) {
        User user = repositories.unsecure.user.findByEmailLowercase(email);
        if(user != null) {
            services.user.passwordRecovery(user);
            return new ModelAndView("forms::post-alert(messageSource='template.resetPassword.sent', formClass='alert-success')");
        } return new ModelAndView("forms::post-alert(messageSource='template.resetPassword.sent.error', formClass='alert-danger')");
    }

    /**
     * Validates password reset token and redirects to change form.
     * <p>
     * Validates reset token from query parameter, redirects to {@code passwordChangePageCustomUrl}
     * if configured, otherwise forwards to {@code /password/change} endpoint internally.
     * </p>
     *
     * @param request HTTP request containing token parameter
     * @return ModelAndView redirecting to password change form or custom page
     * @throws com.openkoda.core.exception.InvalidTokenException if token invalid, expired (&gt;24 hours), or already used
     */
    @GetMapping(_RECOVERY + _VERIFY)
    @ResponseBody
    public Object passwordRecoveryTokenCheck(HttpServletRequest request) {
        debug("[passwordRecoveryTokenCheck]");
        ModelAndView mav = new ModelAndView(
                !passwordChangePageCustomUrl.isEmpty() ?
                        REDIRECT + passwordChangePageCustomUrl : FORWARD + _PASSWORD + _CHANGE
        );
        return mav;
    }

    /**
     * Displays password change form with authenticated user context.
     * <p>
     * Loads current user from SecurityContextHolder (OrganizationUser), creates PasswordChangeForm
     * with userId for validation, returns Thymeleaf template for password entry.
     * </p>
     *
     * @return ModelAndView with {@code password-recovery} template and PasswordChangeForm
     */
    @GetMapping(_CHANGE)
    @ResponseBody
    public Object passwordChangeForm() {
        debug("[passwordChangeForm]");
        ModelAndView mav = new ModelAndView(frontendResourceTemplateNamePrefix + "password-recovery");
        Object user = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (user instanceof OrganizationUser) {
            mav.addObject("passwordChangeForm", new PasswordChangeForm(((OrganizationUser) user).getUser().getId()));
        }
        return mav;
    }

    /**
     * Processes password change with token validation and security checks.
     * <p>
     * <b>Security workflow:</b>
     * <ol>
     *   <li>Validates authenticated userId matches form userId to prevent session hijacking</li>
     *   <li>If mismatch, logs warning, clears security context, returns empty form (possible hacker attack)</li>
     *   <li>If valid, loads user via {@code repositories.unsecure.user.findOne}</li>
     *   <li>Updates password via {@code services.user.changePassword} (BCrypt hashing)</li>
     *   <li>Clears SecurityContextHolder to force re-login</li>
     *   <li>Redirects to login with passwordChanged flag</li>
     * </ol>
     * </p>
     * <p>
     * <b>Password policy:</b> Minimum 8 characters, must contain uppercase, lowercase, number,
     * and special character.
     * </p>
     *
     * @param passwordChangeForm Form containing userId and new password (validated with @Valid)
     * @return RedirectView to login page with {@code ?passwordChanged} parameter
     * @throws SecurityException if userId mismatch detected (possible attack), clears security context
     * @throws com.openkoda.core.exception.PasswordPolicyException if password doesn't meet requirements
     */
    @PostMapping(_CHANGE + _SAVE)
    @ResponseBody
    public Object passwordChange(@Valid @ModelAttribute PasswordChangeForm passwordChangeForm) {
        debug("[passwordChange]");
        OrganizationUser organizationUser = (OrganizationUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (organizationUser.getUserId() != passwordChangeForm.getUserId()) {
            warn("[passwordChange] possible hacker attack - blocking password change");
            SecurityContextHolder.clearContext();
            ModelAndView mav =  new ModelAndView(frontendResourceTemplateNamePrefix + "email/password-recovery");
            mav.addObject("passwordChangeForm", null);
            return mav;
        }
        User user = repositories.unsecure.user.findOne(passwordChangeForm.getUserId());
        services.user.changePassword(user, passwordChangeForm.getPassword());
        SecurityContextHolder.clearContext();
        RedirectView redirectView = new RedirectView();
        redirectView.setExposeModelAttributes(false);
        redirectView.setUrl(_LOGIN + "?passwordChanged");
        return redirectView;
    }
}
