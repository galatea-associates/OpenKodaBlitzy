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

import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.form.LoggerForm;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import static com.openkoda.controller.common.URLConstants._HTML;
import static com.openkoda.controller.common.URLConstants._LOGS;

/**
 * REST controller exposing HTTP endpoints for admin log viewing, download, and logger configuration management.
 * <p>
 * Concrete {@code @RestController} implementing admin logging interface. Maps HTTP requests to Flow-based 
 * helper methods inherited from {@link AbstractAdminLogsController}. Provides log buffer download 
 * ({@link #downloadLogs()}), log viewing UI ({@link #showLogs()}), logger configuration retrieval 
 * ({@link #getSettings()}), and configuration persistence ({@link #setSettings(LoggerForm, BindingResult)}). 
 * All endpoints require elevated admin privileges enforced via {@code @PreAuthorize}.
 * 
 * <p>
 * <b>Request Mapping:</b> Base path "/html/logs" ({@code URLConstants._HTML + _LOGS})
 * 
 * <p>
 * <b>Security:</b> Implements {@link HasSecurityRules} for privilege enforcement. Log viewing requires 
 * {@code CHECK_CAN_READ_SUPPORT_DATA}, configuration changes require {@code CHECK_CAN_MANAGE_SUPPORT_DATA}.
 * 
 * <p>
 * <b>Response Types:</b> Returns JSON via {@code @ResponseBody} for download endpoint, HTML views via 
 * ModelAndView for UI endpoints, HTMX fragments for AJAX form submission.
 * 
 * <p>
 * <b>Admin-Specific Patterns:</b>
 * <ul>
 * <li>Log viewing: In-memory circular buffer managed by services.logConfig, configurable size</li>
 * <li>Logger configuration: Dynamic logger level adjustment without application restart</li>
 * <li>Privilege enforcement: Separate read (VIEW) vs write (MANAGE) privileges for logs and configuration</li>
 * <li>HTMX integration: POST endpoint returns view fragments for AJAX form updates (::logger-settings-form-success/error)</li>
 * <li>Flow delegation: All business logic delegated to abstract base class Flow helpers (getLogsFlow, getSettingsFlow, saveSettings)</li>
 * <li>View naming: Convention-based view names (logs-all, logs-settings) resolved by Thymeleaf</li>
 * <li>URL constants: Uses static imports from URLConstants (_HTML, _LOGS, _DOWNLOAD, _ALL, _SETTINGS)</li>
 * </ul>
 * <p>
 * <b>Privilege Requirements:</b>
 * <ul>
 * <li>{@code CHECK_CAN_READ_SUPPORT_DATA}: Required for viewing logs and configuration (GET endpoints)</li>
 * <li>{@code CHECK_CAN_MANAGE_SUPPORT_DATA}: Required for changing configuration (POST /settings)</li>
 * </ul>
 * <p>
 * <b>Thread-Safety:</b> Stateless, thread-safe.
 * 
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractAdminLogsController for Flow-based helper implementation
 * @see com.openkoda.core.service.LogConfigService for log configuration service
 */
@RestController
@RequestMapping({_HTML + _LOGS})
public class AdminLogsController extends AbstractAdminLogsController implements HasSecurityRules {

    /**
     * Downloads in-memory log buffer as text for offline log analysis.
     * <p>
     * <b>HTTP Mapping:</b> GET /html/logs/download
     * 
     * <p>
     * <b>Security:</b> {@code @PreAuthorize(CHECK_CAN_READ_SUPPORT_DATA)} - requires support data read privilege.
     * 
     * <p>
     * <b>Response Format:</b> Plain text with newline-delimited log entries. Used by admin UI "Download Logs" 
     * button for offline log analysis.
     * 
     *
     * @return String containing complete debug log buffer from services.logConfig.getDebugEntries()
     */
    @PreAuthorize(CHECK_CAN_READ_SUPPORT_DATA)
    @GetMapping(value = _DOWNLOAD)
    @ResponseBody
    public Object downloadLogs() {
        debug("[downloadLogs]");
        return services.logConfig.getDebugEntries();
    }

    /**
     * Displays log viewing UI with log entries list.
     * <p>
     * <b>HTTP Mapping:</b> GET /html/logs/all
     * 
     * <p>
     * <b>Security:</b> {@code @PreAuthorize(CHECK_CAN_READ_SUPPORT_DATA)} - requires support data read privilege.
     * 
     * <p>
     * Model populated by {@link AbstractAdminLogsController#getLogsFlow()} with key "logsEntryList" containing 
     * List of log entries. Returns {@code @ResponseBody} JSON for AJAX requests. Used by admin dashboard log 
     * viewer tab.
     * 
     *
     * @return Object resolving to ModelAndView with view name "logs-all"
     */
    @PreAuthorize(CHECK_CAN_READ_SUPPORT_DATA)
    @GetMapping(value = _ALL)
    @ResponseBody
    public Object showLogs() {
        debug("[showLogs]");
        return getLogsFlow()
                .mav(LOGS + "-" + ALL);
    }

    /**
     * Retrieves current logger configuration UI.
     * <p>
     * <b>HTTP Mapping:</b> GET /html/logs/settings
     * 
     * <p>
     * <b>Security:</b> {@code @PreAuthorize(CHECK_CAN_READ_SUPPORT_DATA)} - requires support data read privilege.
     * 
     * <p>
     * Model populated by {@link AbstractAdminLogsController#getSettingsFlow()} with key "loggerForm" containing 
     * {@link LoggerForm} (debugLoggers, maxEntries). Used by admin UI logger configuration panel.
     * 
     *
     * @return Object resolving to ModelAndView with view name "logs-settings"
     */
    @PreAuthorize(CHECK_CAN_READ_SUPPORT_DATA)
    @GetMapping(value = _SETTINGS)
    public Object getSettings() {
        debug("[getSettings]");
        return getSettingsFlow()
                .mav(LOGS + "-settings");
    }

    /**
     * Persists logger configuration changes via AJAX form submission.
     * <p>
     * <b>HTTP Mapping:</b> POST /html/logs/settings
     * 
     * <p>
     * <b>Security:</b> {@code @PreAuthorize(CHECK_CAN_MANAGE_SUPPORT_DATA)} - requires support data management 
     * privilege (higher privilege than read).
     * 
     * <p>
     * Model updated by {@link AbstractAdminLogsController#saveSettings(LoggerForm, BindingResult)} with 
     * validation results. Used by AJAX form submission from logger settings UI.
     * 
     *
     * @param loggerFormData validated {@link LoggerForm} with buffer size and logging class names
     * @param br validation {@link BindingResult} for error tracking
     * @return Object resolving to HTMX fragment "entity-forms::logger-settings-form-success" on validation 
     *         success, "entity-forms::logger-settings-form-error" on validation failure
     */
    @PreAuthorize(CHECK_CAN_MANAGE_SUPPORT_DATA)
    @PostMapping(value = _SETTINGS)
    public Object setSettings(@Valid LoggerForm loggerFormData, BindingResult br) {
        debug("[setSettings]");
        return saveSettings(loggerFormData, br)
                .mav(ENTITY + '-' + FORMS + "::logger-settings-form-success",
                        ENTITY + '-' + FORMS + "::logger-settings-form-error");
    }

}
