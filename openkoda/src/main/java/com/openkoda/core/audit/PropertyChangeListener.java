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

import com.openkoda.controller.common.SessionData;
import com.openkoda.core.helper.ApplicationContextProvider;
import com.openkoda.core.security.OrganizationUser;
import com.openkoda.core.service.SessionService;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.core.tracker.RequestIdHolder;
import com.openkoda.model.common.Audit;
import com.openkoda.model.common.AuditableEntity;
import com.openkoda.model.common.AuditableEntityOrganizationRelated;
import jakarta.inject.Inject;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

/**
 * Per-entity utility converting AuditedObjectState snapshots into persistent Audit domain objects for audit trail storage.
 * <p>
 * Constructs ready-to-persist Audit entities from entity snapshots captured during Hibernate lifecycle events. 
 * Resolves runtime dependencies (AuditChangeFactory, IpService) via ApplicationContextProvider to avoid circular 
 * injection issues. Extracts request metadata (IP address via IpService, request ID via RequestIdHolder), checks 
 * for spoofing context via SessionService.getSessionAttribute(SessionData.SPOOFING_USER), and populates Audit 
 * entity fields including operation type, entity identifiers, organization ID for multi-tenant entities, user ID, 
 * role IDs, and HTML change description. Instances are registered per entity class in AuditInterceptor.auditListeners map.
 * </p>
 * <p>
 * Called by AuditInterceptor.beforeTransactionCompletion to convert collected AuditedObjectState entries into 
 * Audit[] for batch persistence.
 * </p>
 * <p>
 * Thread-safety: Not thread-safe. Session-scoped lifecycle aligned with Hibernate session.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AuditInterceptor#beforeTransactionCompletion
 * @see AuditChangeFactory
 * @see IpService
 * @see AuditedObjectState
 */
public class PropertyChangeListener implements LoggingComponentWithRequestId {

    /**
     * Injected service for accessing session attributes (e.g., spoofing user ID).
     */
    @Inject
    SessionService sessionService;

    /**
     * Fully qualified entity class name (e.g., 'com.openkoda.model.Organization').
     */
    protected final String entityClass;
    
    /**
     * Human-readable entity label for audit display (e.g., 'Organization').
     */
    protected final String entityClassLabel;

    /**
     * Creates listener for specified entity class with display label.
     *
     * @param className Fully qualified entity class name for Audit.entityName field
     * @param entityClassLabel Human-readable label for Audit.entityKey field and change descriptions
     */
    public PropertyChangeListener(String className, String entityClassLabel) {
        this.entityClass = className;
        this.entityClassLabel = entityClassLabel;
    }

    /**
     * Prepares single-element Audit array from entity snapshot for batch persistence.
     * <p>
     * Main entry point called by AuditInterceptor.beforeTransactionCompletion. Delegates to createAudit 
     * and wraps result in array for compatibility with bulk save operations.
     * </p>
     *
     * @param entity Audited entity instance (must implement AuditableEntity)
     * @param user Optional authenticated user performing the operation
     * @param aos Immutable snapshot of entity state with properties, changes, operation type
     * @param userRoleIds Optional collection of user's role IDs for privilege tracking
     * @return Single-element array containing populated Audit entity ready for persistence
     */
    public Audit[] prepareAuditLogs(Object entity, Optional<OrganizationUser> user, AuditedObjectState aos, Optional<Collection<?>> userRoleIds) {
        debug("prepareAuditLogs {} {} {} {}", entity, user, aos, userRoleIds);
        Audit audit = createAudit(entity, user, aos, userRoleIds);
        return new Audit[]{audit};
    }

    /**
     * Creates Audit entity by casting to AuditableEntity and delegating to prepareAudit.
     *
     * @param entity Entity object to audit (cast to AuditableEntity at line 72)
     * @param user Optional authenticated user
     * @param aos Entity state snapshot
     * @param userRoleIds Optional role IDs
     * @return Populated Audit entity with all metadata and change description
     */
    protected Audit createAudit(Object entity, Optional<OrganizationUser> user, AuditedObjectState aos, Optional<Collection<?>> userRoleIds) {
        debug("[createAudit] {} {} {} {}", entity, user, aos, userRoleIds);
        AuditableEntity auditPrintableEntity = (AuditableEntity) entity;
        return prepareAudit(auditPrintableEntity, aos, user.isPresent() ? user.get() : null, userRoleIds.orElse(null), new Date());
    }

    /**
     * Constructs Audit entity with complete metadata: operation, entity identifiers, user context, IP, request ID, change description.
     * <p>
     * Populates Audit fields: (1) operation from aos, (2) entityName/entityKey from class/label fields, 
     * (3) entity/organization IDs via helper methods, (4) IP address via IpService runtime lookup, 
     * (5) request ID from RequestIdHolder, (6) user/role IDs if authenticated, (7) HTML change description 
     * via AuditChangeFactory, (8) spoofing indicator if user.isSpoofed(). Returns fully populated Audit 
     * ready for AuditRepository.saveAll.
     * </p>
     * <p>
     * Note: date parameter accepted but not used - Audit entity uses @CreatedDate for automatic timestamping.
     * </p>
     *
     * @param p Auditable entity providing ID and optional organization ID
     * @param aos State snapshot with operation, properties, changes, optional content
     * @param user Authenticated user or null for unauthenticated operations
     * @param userRoleIds User's role IDs for privilege auditing, or null
     * @param date Timestamp for audit record (currently unused in implementation)
     * @return Fully populated Audit entity
     */
    protected Audit prepareAudit(AuditableEntity p, AuditedObjectState aos, OrganizationUser user, Collection<?> userRoleIds, Date date) {
        debug("[prepareAudit] {} {} {} {} {}", p, aos, user, userRoleIds, date);
        Audit audit = new Audit();
        audit.setOperation(aos.getOperation());
        String changeDescription = getAuditChangeFactory().createChange(p, aos, entityClassLabel);
        audit.setEntityName(entityClass);
        audit.setEntityKey(entityClassLabel);
        audit.setSeverity(Audit.Severity.INFO);
        audit.setUserRoleIds(userRoleIds);
        audit.setEntityId(getEntityId(p));
        audit.setOrganizationId(getOrganizationId(p));
        audit.setIpAddress(getIpService().getCurrentUserIpAddress());
        audit.setRequestId(RequestIdHolder.getId());
        if (aos.getContent() != null) {
            audit.setContent(aos.getContent());
        }
        if (user != null && user.getUser() != null) {
            audit.setUserId(user.getUser().getId());
            if (user.isSpoofed()) {
                changeDescription = addSpoofInfo(changeDescription);
            }
        }
        audit.setChange(changeDescription);
        return audit;
    }


    /**
     * Prepends spoofing indicator to change description when user is spoofed.
     * <p>
     * Retrieves spoofing user ID from session via SessionService.getInstance().getSessionAttribute(SessionData.SPOOFING_USER) at line 109.
     * </p>
     *
     * @param changeDescription Original HTML change description
     * @return Change description prefixed with bold italic spoofing notice like '<b><i>Spoofed by user with id: 123</b></i></br>...' or original if not spoofed
     */
    private String addSpoofInfo(String changeDescription) {
        debug("[addSpoofInfo]");
        Long uId = (Long) SessionService.getInstance().getSessionAttribute(SessionData.SPOOFING_USER);
        changeDescription = "<b><i>Spoofed by user with id: " + uId + "</b></i></br>" + changeDescription;
        return changeDescription;
    }


    /**
     * Extracts organization ID from multi-tenant entities for tenant-scoped audit filtering.
     * <p>
     * Enables filtering audit logs by organization in multi-tenant deployments.
     * </p>
     *
     * @param entity Auditable entity to check for organization relationship
     * @return Organization ID if entity implements AuditableEntityOrganizationRelated, null for global entities
     */
    protected Long getOrganizationId(AuditableEntity entity) {
        debug("[getOrganizationId]");
        if (entity instanceof AuditableEntityOrganizationRelated) {
            AuditableEntityOrganizationRelated auditableEntityOrganizationRelated = (AuditableEntityOrganizationRelated) entity;
            return auditableEntityOrganizationRelated.getOrganizationId();
        }
        debug("[getOrganizationId] no AuditableEntity {} found", entity);
        return null;
    }

    /**
     * Extracts entity primary key for audit record linkage.
     *
     * @param p Auditable entity
     * @return Entity ID from p.getId()
     */
    protected Long getEntityId(AuditableEntity p) {
        return p.getId();
    }

    /**
     * Resolves AuditChangeFactory bean at runtime to avoid circular dependency.
     *
     * @return AuditChangeFactory singleton from ApplicationContext
     */
    private AuditChangeFactory getAuditChangeFactory() {
        return getContext().getBean(AuditChangeFactory.class);
    }

    /**
     * Resolves IpService bean at runtime to avoid circular dependency.
     *
     * @return IpService singleton from ApplicationContext
     */
    private IpService getIpService() {
        return getContext().getBean(IpService.class);
    }

    /**
     * Retrieves Spring ApplicationContext via static provider.
     *
     * @return Current ApplicationContext
     */
    private ApplicationContext getContext() {
        return ApplicationContextProvider.getContext();
    }

}
