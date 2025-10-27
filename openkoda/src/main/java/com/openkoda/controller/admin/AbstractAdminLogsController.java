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

import com.openkoda.core.controller.generic.AbstractController;
import com.openkoda.core.flow.Flow;
import com.openkoda.core.flow.PageModelMap;
import com.openkoda.form.LoggerForm;
import org.springframework.validation.BindingResult;

import java.util.stream.Collectors;

/**
 * Abstract base controller providing Flow-based helper methods for admin log viewing and logger configuration management.
 * <p>
 * This stateless abstract controller implements log viewing functionality ({@code getLogsFlow}, {@code getSettingsFlow})
 * and logger configuration persistence ({@code saveSettings}). It is designed for reuse by concrete controllers that
 * handle HTTP bindings and view resolution. All methods use the Flow pipeline pattern for composing PageModelMap results.
 * </p>
 * <p>
 * Implementing classes should take over HTTP binding and forming a result whereas this controller takes care of
 * actual implementation by delegating to {@link com.openkoda.service.log.LogConfigService} for log buffer access
 * and configuration persistence.
 * </p>
 * <p>
 * Thread-safety: This controller is stateless and thread-safe. All data is passed via Flow execution context.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AdminLogsController for concrete HTTP endpoint implementation
 * @see com.openkoda.service.log.LogConfigService for log configuration service
 * @see LoggerForm for logger configuration form binding
 */
public class AbstractAdminLogsController extends AbstractController {

    /**
     * Retrieves log entries from the in-memory debug log buffer for display.
     * <p>
     * Returns a PageModelMap containing the list of log entries captured by the logging subsystem.
     * The model is populated with key {@code logsEntryList} containing debug entries retrieved from
     * {@code services.logConfig.getDebugEntriesAsList()}. Used by concrete controllers to retrieve
     * the in-memory debug log buffer for display in admin UI.
     * </p>
     *
     * @return PageModelMap with model key {@code logsEntryList} populated with log entries list
     * @see com.openkoda.service.log.LogConfigService#getDebugEntriesAsList()
     */
    protected PageModelMap getLogsFlow() {
        debug("[getLogsFlow]");
        return Flow.init()
                .thenSet(logsEntryList, a -> services.logConfig.getDebugEntriesAsList())
                .execute();
    }


    /**
     * Retrieves current logger configuration for display in admin UI.
     * <p>
     * Returns a PageModelMap containing the current logger configuration populated into a LoggerForm.
     * The model is populated with key {@code loggerForm} containing current debug loggers and max entries
     * retrieved from {@code services.logConfig}. Used to display logger configuration UI with current settings.
     * </p>
     *
     * @return PageModelMap with model key {@code loggerForm} populated with current logger configuration
     * @see LoggerForm
     * @see com.openkoda.service.log.LogConfigService#getDebugLoggers()
     * @see com.openkoda.service.log.LogConfigService#getMaxEntries()
     */
    protected PageModelMap getSettingsFlow() {
        debug("[getSettingsFlow]");
        return Flow.init()
                .thenSet(loggerForm, a -> new LoggerForm(services.logConfig.getDebugLoggers(), services.logConfig.getMaxEntries()))
                .execute();
    }

    /**
     * Validates and persists logger configuration from admin UI.
     * <p>
     * Processes the submitted logger configuration form, validates the input, and persists the configuration.
     * The method returns a PageModelMap with validation results and available logger class names. Model keys
     * include {@code loggerForm} (input form data) and {@code logClassNamesList} (available logger class names
     * from {@code services.logConfig.getAvailableLoggers()}).
     * </p>
     * <p>
     * Execution flow: Validates form via {@code services.validation.validate()}, then saves configuration
     * via {@code services.logConfig.saveConfig()} with buffer size and logging class names from the form DTO.
     * </p>
     *
     * @param loggerFormData the logger configuration form containing buffer size and logging class names
     * @param br the Spring validation binding result for capturing validation errors
     * @return PageModelMap with model keys {@code loggerForm} and {@code logClassNamesList}, includes validation results
     * @see LoggerForm
     * @see com.openkoda.service.log.LogConfigService#saveConfig(Integer, String)
     * @see com.openkoda.service.log.LogConfigService#getAvailableLoggers()
     */
    protected PageModelMap saveSettings(LoggerForm loggerFormData, BindingResult br) {
        debug("[saveSettings]");
        return Flow.init(loggerForm, loggerFormData)
                .thenSet(logClassNamesList, a -> services.logConfig.getAvailableLoggers().stream().map(Class::getName).collect(Collectors.toList()))
                .then(a -> services.validation.validate(loggerFormData, br))
                .then(a -> services.logConfig.saveConfig(loggerFormData.dto.getBufferSize(), loggerFormData.dto.getLoggingClasses()))
                .execute();
    }
}
