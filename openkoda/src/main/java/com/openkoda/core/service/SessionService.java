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

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * RequestContextHolder-based HTTP session accessor with static legacy getInstance() pattern.
 * <p>
 * Provides thread-safe session attribute management within Spring MVC request scope by leveraging
 * Spring's RequestContextHolder to access the current HTTP session. This service implements
 * LoggingComponentWithRequestId for request correlation and debugging support.
 * 
 * <p>
 * The service supports both instance-based Spring bean injection and static access through
 * getInstance() for legacy code compatibility. All session operations check for request
 * context availability and gracefully handle scenarios where no HTTP request is active
 * (e.g., async threads, background jobs).
 * 
 * <p>
 * <strong>Warning:</strong> The {@link #getSessionAttribute(String)} method always creates
 * a session if one doesn't exist. Use {@link #getAttributeIfSessionExists(String)} to avoid
 * inadvertent session creation.
 * 
 *
 * @see RequestContextHolder
 * @see HttpSession
 * @see ServletRequestAttributes
 * @see LoggingComponentWithRequestId
 * @since 1.7.1
 * @author OpenKoda Team
 * @version 1.7.1
 */
@Service
public class SessionService implements LoggingComponentWithRequestId {

    /**
     * Static instance initialized via {@link #init()} for legacy static access pattern.
     * <p>
     * This field is populated by Spring during {@link PostConstruct} phase and enables
     * the {@link #getInstance()} static accessor method. The static pattern is maintained
     * for backward compatibility with existing code that cannot use dependency injection.
     * 
     */
    private static SessionService instance;

    /**
     * Retrieves the current HTTP session from RequestContextHolder.
     * <p>
     * This method checks RequestAttributes availability before attempting to access the session,
     * ensuring safe operation in contexts where no HTTP request is active (e.g., async threads,
     * background jobs, scheduled tasks). Returns null if no request context is available.
     * 
     * <p>
     * The {@code create} parameter controls session creation behavior per
     * {@link HttpSession} semantics:
     * <ul>
     *   <li>{@code true}: Returns existing session or creates a new one</li>
     *   <li>{@code false}: Returns existing session or null if none exists</li>
     * </ul>
     * 
     *
     * @param create if {@code true}, creates a session if one doesn't exist; if {@code false},
     *               returns null when no session exists
     * @return the current {@link HttpSession} instance, or null if no request context is available
     *         or no session exists (when {@code create} is false)
     * @see RequestContextHolder#getRequestAttributes()
     * @see ServletRequestAttributes#getRequest()
     * @see HttpSession
     */
    public HttpSession getSession(boolean create) {
        debug("[getSession]");
        boolean isSession = RequestContextHolder.getRequestAttributes() != null;
        if (!isSession) { return null; }
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attr.getRequest().getSession(create);
    }

    /**
     * Retrieves a named attribute from the HTTP session, always creating the session if absent.
     * <p>
     * <strong>Warning:</strong> This method always passes {@code create=true} to
     * {@link #getSession(boolean)}, which means it will create a new session if one doesn't
     * already exist. This may inadvertently create sessions for users who haven't authenticated
     * or performed any stateful operations.
     * 
     * <p>
     * To avoid unintended session creation, use {@link #getAttributeIfSessionExists(String)}
     * instead, which only retrieves attributes from existing sessions.
     * 
     *
     * @param id the name of the session attribute to retrieve
     * @return the attribute value, or null if the attribute doesn't exist
     * @see #getAttributeIfSessionExists(String)
     * @see HttpSession#getAttribute(String)
     */
    public Object getSessionAttribute(String id){
        return getSession(true).getAttribute(id);
    }

    /**
     * Safe attribute retrieval without creating a session.
     * <p>
     * This method retrieves a named attribute only if an HTTP session already exists. Unlike
     * {@link #getSessionAttribute(String)}, this method will not create a new session, making
     * it safe to call for users who may not have an active session yet.
     * 
     * <p>
     * Returns null in two cases:
     * <ul>
     *   <li>No HTTP session exists for the current request</li>
     *   <li>The session exists but the named attribute is not found</li>
     * </ul>
     * 
     *
     * @param id the name of the session attribute to retrieve
     * @return the attribute value if found, or null if the session doesn't exist or the
     *         attribute is not present
     * @see #getSessionAttribute(String)
     * @see HttpSession#getAttribute(String)
     */
    public Object getAttributeIfSessionExists(String id) {
        HttpSession s = getSession(false);
        return s != null ? s.getAttribute(id) : null;
    }

    /**
     * Conditionally sets a session attribute only if the session already exists.
     * <p>
     * This method sets a named attribute only when an HTTP session is already active. If no
     * session exists, the method returns without creating one, avoiding the session creation
     * side-effect present in direct {@link HttpSession#setAttribute(String, Object)} usage
     * on newly obtained sessions.
     * 
     * <p>
     * <strong>Note:</strong> This method returns {@code true} always for consistency, even when
     * the session doesn't exist and no attribute is actually set. The return value indicates
     * the method completed successfully, not whether the attribute was set.
     * 
     *
     * @param id the name of the session attribute to set
     * @param value the value to store in the session attribute
     * @return {@code true} always (indicates successful method execution, not attribute modification)
     * @see HttpSession#setAttribute(String, Object)
     */
    public boolean setAttributeIfSessionExists(String id, Object value) {
        HttpSession s = getSession(false);
        if (s != null) {
            s.setAttribute(id, value);
        }
        return true;
    }

    /**
     * Conditionally removes a session attribute only if the session exists.
     * <p>
     * This method removes a named attribute only when an HTTP session is already active.
     * If no session exists, the method returns without attempting removal, making it safe
     * to call in scenarios where session state is uncertain.
     * 
     * <p>
     * <strong>Note:</strong> This method returns {@code true} always for consistency, even when
     * the session doesn't exist and no attribute is actually removed. The return value indicates
     * the method completed successfully, not whether an attribute was removed.
     * 
     *
     * @param id the name of the session attribute to remove
     * @return {@code true} always (indicates successful method execution, not attribute removal)
     * @see HttpSession#removeAttribute(String)
     */
    public boolean removeAttribute(String id) {
        HttpSession s = getSession(false);
        if (s != null) {
            s.removeAttribute(id);
        }
        return true;
    }

    /**
     * Initializes the static instance reference for legacy static access pattern.
     * <p>
     * This method is invoked automatically by Spring during the {@link PostConstruct} phase
     * after dependency injection is complete. It populates the static {@link #instance} field
     * to enable the {@link #getInstance()} static accessor method.
     * 
     *
     * @see PostConstruct
     * @see #getInstance()
     */
    @PostConstruct void init() {
        instance = this;
    }

    /**
     * Static accessor to the Spring-managed singleton instance.
     * <p>
     * Returns the SessionService instance that was initialized during Spring ApplicationContext
     * startup. This static accessor enables legacy code to access session functionality without
     * requiring dependency injection.
     * 
     * <p>
     * <strong>Note:</strong> This method is only available after ApplicationContext initialization
     * is complete. Calling it before Spring bootstrap finishes will return null.
     * 
     *
     * @return the Spring-managed SessionService singleton instance
     * @see #init()
     */
    public final static SessionService getInstance() {
        return instance;
    }
}
