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

package com.openkoda.core.service;

import com.openkoda.core.audit.IpService;
import com.openkoda.core.helper.ApplicationContextProvider;
import com.openkoda.core.security.UserProvider;
import com.openkoda.core.tracker.RequestIdHolder;
import com.openkoda.model.common.Audit;
import com.openkoda.repository.admin.AuditRepository;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Lightweight audit logging service that centralizes {@link Audit} model construction and persistence.
 * <p>
 * This service uses a {@link PostConstruct} static-instance pattern to provide convenient static helper
 * methods for audit logging from any context without requiring dependency injection. The service collects
 * contextual information including user ID from {@link UserProvider}, request ID from {@link RequestIdHolder},
 * and client IP address from {@link IpService} to create comprehensive audit records.

 * <p>
 * The static helper methods ({@link #createErrorAuditForException(Throwable, String)} and
 * {@link #createSimpleInfoAudit(String, String)}) allow audit logging without Spring bean injection,
 * making it useful for exception handlers, static utility classes, and other contexts where dependency
 * injection is unavailable. If the static instance is uninitialized, these methods gracefully degrade
 * by returning {@code true} without persisting the audit record.

 * <p>
 * Example usage:
 * <pre>{@code
 * AuditService.createErrorAuditForException(ex, "User login failed");
 * AuditService.createSimpleInfoAudit("Configuration loaded", configDetails);
 * }</pre>

 *
 * @see Audit
 * @see AuditRepository
 * @see UserProvider
 * @see RequestIdHolder
 * @see IpService
 * @see ApplicationContextProvider
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Service
public class AuditService {

    /**
     * Repository for persisting {@link Audit} entities to the database.
     * <p>
     * Used by static helper methods via the singleton {@link #instance} field to save audit records.

     */
    @Inject
    private AuditRepository auditRepository;

    /**
     * Static singleton instance initialized via {@link PostConstruct} lifecycle callback.
     * <p>
     * This pattern enables static helper methods to access Spring-managed dependencies without
     * requiring dependency injection at the call site. The instance is set by the {@link #init()}
     * method after Spring completes bean construction and injection.

     * <p>
     * If this field is {@code null} (service not yet initialized), static methods will return
     * {@code true} without persisting audit records, allowing graceful degradation in early
     * application lifecycle or test scenarios.

     */
    private static AuditService instance;

    /**
     * Lifecycle callback that assigns this service instance to the static {@link #instance} field.
     * <p>
     * This method is invoked by Spring immediately after dependency injection is completed, enabling
     * the static helper pattern used by {@link #createErrorAuditForException(Throwable, String)} and
     * {@link #createSimpleInfoAudit(String, String)}. By storing a reference to the Spring-managed
     * bean, these static methods can delegate to instance methods with access to injected dependencies.

     */
    @PostConstruct void init() {
        instance = this;
    }

    /**
     * Creates an ERROR severity audit record for an exception with full stack trace capture.
     * <p>
     * This static helper method constructs an {@link Audit} entity with severity {@link Audit.Severity#ERROR}
     * and operation {@link Audit.AuditOperation#BROWSE}. The exception stack trace is written to the
     * {@link Audit#content} field via {@link PrintWriter}, and the provided message is stored in the
     * {@link Audit#change} field. Contextual information is automatically collected:

     * <ul>
     * <li>User ID from {@link UserProvider#getUserIdOrNotExistingId()} (or {@code NOT_EXISTING_ID} sentinel if no user)</li>
     * <li>Request ID from {@link RequestIdHolder#getId()}</li>
     * <li>Client IP address resolved via {@link IpService} bean lookup through {@link ApplicationContextProvider}</li>
     * </ul>
     * <p>
     * If the static {@link #instance} is uninitialized (service not yet constructed), this method performs
     * a silent no-op and returns {@code true} without persisting the audit record. This graceful degradation
     * allows audit logging during early application lifecycle without causing failures.

     * <p>
     * Example usage:
     * <pre>{@code
     * try {
     *     authenticateUser(credentials);
     * } catch (AuthenticationException ex) {
     *     AuditService.createErrorAuditForException(ex, "User login failed");
     * }
     * }</pre>

     *
     * @param exception the exception to log (stack trace written to content field); may be {@code null}
     * @param message descriptive message stored in the change field (e.g., "User login failed")
     * @return always returns {@code true}, even if persistence is skipped due to uninitialized instance
     * @see Audit
     * @see AuditRepository#saveAudit(Audit)
     * @see UserProvider#getUserIdOrNotExistingId()
     * @see RequestIdHolder#getId()
     * @see IpService#getCurrentUserIpAddress()
     */
    public static boolean createErrorAuditForException(Throwable exception, String message) {
        Audit a = new Audit();
        String reqId = RequestIdHolder.getId();
        a.setSeverity(Audit.Severity.ERROR);
        a.setUserId(UserProvider.getUserIdOrNotExistingId());
        a.setOperation(Audit.AuditOperation.BROWSE);
        a.setRequestId(reqId);
        a.setIpAddress(getIpService().getCurrentUserIpAddress());
        a.setChange(message);
        if (exception != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            a.setContent(sw.toString());
        }
        if (instance != null) {
            instance.auditRepository.saveAudit(a);
        }
        return true;
    }

    /**
     * Convenience overload that creates an INFO severity audit with a message and no content.
     * <p>
     * This method delegates to {@link #createSimpleInfoAudit(String, String)} with {@code null} content,
     * simplifying audit logging when only a message is needed without additional detail.

     *
     * @param message descriptive message stored in the change field (e.g., "Configuration loaded")
     * @return always returns {@code true}, even if persistence is skipped due to uninitialized instance
     * @see #createSimpleInfoAudit(String, String)
     */
    public static boolean createSimpleInfoAudit(String message){
        return createSimpleInfoAudit(message, null);
    }

    /**
     * Creates an INFO severity audit record with a message and optional content.
     * <p>
     * This static helper method constructs an {@link Audit} entity with severity {@link Audit.Severity#INFO}.
     * The provided message is stored in the {@link Audit#change} field, and optional content is stored in
     * the {@link Audit#content} field. An empty request ID is explicitly set to ensure the
     * {@link Audit#indexString} update works correctly (special requirement for INFO audits without request context).

     * <p>
     * If the static {@link #instance} is uninitialized (service not yet constructed), this method performs
     * a silent no-op and returns {@code true} without persisting the audit record. This graceful degradation
     * allows audit logging during early application lifecycle without causing failures.

     * <p>
     * Example usage:
     * <pre>{@code
     * String details = loadConfiguration();
     * AuditService.createSimpleInfoAudit("Configuration loaded", details);
     * }</pre>

     *
     * @param message descriptive message stored in the change field (e.g., "Configuration loaded")
     * @param content optional additional detail stored in the content field; may be {@code null}
     * @return always returns {@code true}, even if persistence is skipped due to uninitialized instance
     * @see Audit
     * @see AuditRepository#saveAudit(Audit)
     */
    public static boolean createSimpleInfoAudit(String message, String content){
        Audit a = new Audit();
        a.setSeverity(Audit.Severity.INFO);
        a.setChange(message);
        a.setContent(content);
        a.setRequestId("");//otherwise indexString update doesn't work for this record
        if (instance != null) {
            instance.auditRepository.saveAudit(a);
        }
        return true;
    }

    /**
     * Private static helper that resolves the {@link IpService} bean from the Spring application context.
     * <p>
     * This method provides access to {@link IpService} without requiring dependency injection, enabling
     * static methods like {@link #createErrorAuditForException(Throwable, String)} to retrieve the current
     * user's IP address via {@link IpService#getCurrentUserIpAddress()}.

     *
     * @return the {@link IpService} bean instance from the Spring context
     * @see IpService
     * @see ApplicationContextProvider#getContext()
     */
    private static IpService getIpService() {
        return getContext().getBean(IpService.class);
    }

    /**
     * Private static helper that accesses the Spring {@link ApplicationContext} via {@link ApplicationContextProvider}.
     * <p>
     * This method provides the application context to other static helpers like {@link #getIpService()},
     * enabling bean lookups without dependency injection. The context is retrieved from the static
     * {@link ApplicationContextProvider} utility.

     *
     * @return the Spring {@link ApplicationContext} instance
     * @see ApplicationContextProvider#getContext()
     */
    private static ApplicationContext getContext() {
        return ApplicationContextProvider.getContext();
    }


}
