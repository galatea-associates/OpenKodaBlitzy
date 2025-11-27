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

package com.openkoda.core.configuration;

import com.openkoda.core.audit.PropertyChangeInterceptor;
import com.openkoda.core.flow.LoggingComponent;
import com.openkoda.core.security.OrganizationUser;
import com.openkoda.core.tracker.DebugLogsDecoratorWithRequestId;
import com.openkoda.model.common.TimestampedEntity.UID;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Spring configuration class for JPA auditing infrastructure and Hibernate interceptor setup.
 * <p>
 * Annotated with {@link Configuration} and {@link EnableJpaAuditing} to activate Spring Data JPA
 * auditing for {@link org.springframework.data.annotation.CreatedDate},
 * {@link org.springframework.data.annotation.LastModifiedDate},
 * {@link org.springframework.data.annotation.CreatedBy}, and
 * {@link org.springframework.data.annotation.LastModifiedBy} annotations on entities.
 * 
 * <p>
 * Registers three beans:
 * <ul>
 * <li>{@link #auditorProvider()} for capturing current user UID as auditor</li>
 * <li>{@link #logsProvider()} for request-correlated debug logging</li>
 * <li>{@link #hibernatePropertiesCustomizer(PropertyChangeInterceptor)} for injecting
 * PropertyChangeInterceptor into Hibernate SessionFactory</li>
 * </ul>
 * <p>
 * Enables comprehensive entity change tracking and audit trail generation throughout the application.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SpringSecurityAuditorAware
 * @see PropertyChangeInterceptor
 * @see org.springframework.data.jpa.repository.config.EnableJpaAuditing
 */
@Configuration
@EnableJpaAuditing
public class AuditConfig {

    /**
     * Nested AuditorAware implementation that extracts current user UID from Spring Security context.
     * <p>
     * Uses {@link SecurityContextHolder} to access the current {@link Authentication}. Casts principal
     * to {@link OrganizationUser} and retrieves UID containing username and user ID. Returns UNKNOWN
     * UID sentinel if security context is not available or principal is not of OrganizationUser type.
     * 
     * <p>
     * Thread-safe as SecurityContext is thread-local storage managed by Spring Security.
     * 
     *
     * @author OpenKoda Team
     * @version 1.7.1
     * @since 1.7.1
     * @see AuditorAware
     * @see SecurityContextHolder
     * @see OrganizationUser
     */
    public static class SpringSecurityAuditorAware implements AuditorAware<UID>, LoggingComponent {

        private static final UID UNKNOWN = new UID();

        /**
         * Retrieves the current auditor UID from Spring Security context for JPA auditing.
         * <p>
         * Extracts {@link Authentication} from {@link SecurityContextHolder}, verifies authentication
         * is valid, and casts principal to {@link OrganizationUser}. Returns UID containing username
         * and user ID for population of @CreatedBy and @LastModifiedBy entity fields.
         * 
         * <p>
         * Always returns {@link Optional#of(Object)} with either authenticated user UID or UNKNOWN
         * sentinel UID to ensure auditing fields are never null.
         * 
         *
         * @return Optional containing UID of authenticated user or UNKNOWN UID if no valid security context
         */
        public Optional<UID> getCurrentAuditor() {
            debug("[getCurrentAuditor]");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of(UNKNOWN);
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof OrganizationUser) {
                String name = ((OrganizationUser) authentication.getPrincipal()).getUsername();
                Long id = ((OrganizationUser) authentication.getPrincipal()).getUserId();

                return Optional.of(new UID(name, id));
            }

            return Optional.of(UNKNOWN);

        }
    }

    /**
     * Creates AuditorAware bean for JPA auditing to capture current user as entity modifier.
     * <p>
     * Returns {@link SpringSecurityAuditorAware} implementation (nested class) that maps
     * {@link SecurityContextHolder} Authentication to UID. Extracts {@link OrganizationUser}
     * from principal and returns UID containing username and user ID. Supplies UNKNOWN UID
     * sentinel if security context is not initialized.
     * 
     * <p>
     * Always returns Optional.of(UID) for auditing to ensure @CreatedBy and @LastModifiedBy
     * fields are never null.
     * 
     *
     * @return AuditorAware&lt;UID&gt; implementation for JPA @CreatedBy and @LastModifiedBy population
     * @see SpringSecurityAuditorAware
     * @see SecurityContextHolder
     * @see OrganizationUser
     */
    @Bean
    public AuditorAware<UID> auditorProvider() {
        return new SpringSecurityAuditorAware();
    }

    /**
     * Creates debug logging decorator with request ID correlation for audit subsystem.
     * <p>
     * Returns {@link DebugLogsDecoratorWithRequestId} for request-scoped logging with trace IDs.
     * Used by audit interceptors for diagnostic logging to correlate audit events with HTTP
     * requests and background jobs.
     * 
     *
     * @return DebugLogsDecoratorWithRequestId for audit logging with request correlation
     * @see DebugLogsDecoratorWithRequestId
     */
    @Bean
    public DebugLogsDecoratorWithRequestId logsProvider() {
        return new DebugLogsDecoratorWithRequestId();
    }

    /**
     * Creates HibernatePropertiesCustomizer that injects PropertyChangeInterceptor into Hibernate configuration.
     * <p>
     * Writes interceptor under hibernate.session_factory.interceptor property key. Interceptor is
     * bound at SessionFactory creation to capture onSave/onFlushDirty/onDelete events for audit
     * trail generation. Enables property-level change detection for auditable entities.
     * 
     *
     * @param interceptor Hibernate interceptor for entity property change capture and audit record creation
     * @return HibernatePropertiesCustomizer that configures SessionFactory with audit interceptor
     * @see PropertyChangeInterceptor
     * @see com.openkoda.core.audit.AuditInterceptor
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(PropertyChangeInterceptor interceptor) {
        return props -> props.put("hibernate.session_factory.interceptor", interceptor);
    }

}
