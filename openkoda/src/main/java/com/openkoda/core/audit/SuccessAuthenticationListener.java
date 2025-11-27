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

package com.openkoda.core.audit;

import com.openkoda.core.security.OrganizationUser;
import com.openkoda.core.service.event.ApplicationEventService;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.User;
import com.openkoda.repository.user.UserRepository;
import jakarta.inject.Inject;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.openkoda.core.service.event.ApplicationEvent.USER_LOGGED_IN;

/**
 * Spring ApplicationListener capturing successful authentication events for audit trail and user login tracking.
 * <p>
 * Listens for Spring Security AuthenticationSuccessEvent and updates User.lastLogin timestamp upon successful 
 * authentication. Extracts authenticated user from OrganizationUser principal or username string, persists login 
 * datetime to user entity, and emits USER_LOGGED_IN application event for downstream processing. Integrates 
 * authentication events into OpenKoda audit trail and notification systems.
 * 
 * <p>
 * Thread-safety: Stateless service with injected dependencies, thread-safe.
 * 
 *
 * @see AuthenticationSuccessEvent
 * @see OrganizationUser
 * @see ApplicationEventService
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
@Service
public class SuccessAuthenticationListener implements ApplicationListener<AuthenticationSuccessEvent>, LoggingComponentWithRequestId {

    /**
     * Repository for persisting User.lastLogin updates.
     */
    @Inject
    private UserRepository userRepository;

    /**
     * Service for emitting USER_LOGGED_IN application events.
     */
    @Inject
    private ApplicationEventService eventService;

    /**
     * Handles successful authentication by updating user's last login timestamp and emitting application event.
     * <p>
     * Verifies authentication is successful via event.getAuthentication().isAuthenticated(), extracts User entity 
     * from principal, sets lastLogin to current LocalDateTime, persists to database, and emits USER_LOGGED_IN 
     * event with BasicUser DTO. If user extraction fails, silently skips processing.
     * 
     * <p>
     * Implementation note: Runs synchronously in authentication thread. Database save may block briefly.
     * 
     *
     * @param event Spring Security authentication success event containing authenticated principal
     */
    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        debug("[onApplicationEvent]");
        if (event.getAuthentication().isAuthenticated()) {
            Optional<User> user = getUser(event);
            user.ifPresent(u -> {
                u.setLastLogin(LocalDateTime.now());
                userRepository.save(u);
                eventService.emitEvent(USER_LOGGED_IN, u.getBasicUser());
            });
        }
    }

    /**
     * Extracts User entity from authentication event principal with type-based resolution.
     * <p>
     * Attempts to extract User in two ways: (1) If principal is OrganizationUser, extracts via getUser() 
     * accessor. (2) If principal is String username, queries UserRepository.findByLogin(). Returns empty 
     * Optional if principal type unrecognized.
     * 
     * <p>
     * Note: String principal case assumes username-based authentication (line 79).
     * 
     *
     * @param event Authentication event containing principal object
     * @return Optional containing User entity if extraction successful, empty Optional otherwise
     */
    private Optional<User> getUser(AuthenticationSuccessEvent event) {
        debug("[getUser]");
        Object principal = event.getAuthentication().getPrincipal();
        if (principal instanceof OrganizationUser) {
            return Optional.of(((OrganizationUser) principal).getUser());
        }
        if (principal instanceof String) {
            return Optional.of(userRepository.findByLogin((String) principal));
        }
        return Optional.empty();
    }
}
