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
import com.openkoda.core.security.OrganizationUserDetailsService;
import com.openkoda.core.security.UserProvider;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.*;
import com.openkoda.model.authentication.FacebookUser;
import com.openkoda.model.authentication.GoogleUser;
import com.openkoda.model.authentication.LDAPUser;
import com.openkoda.model.authentication.LoginAndPassword;
import com.openkoda.model.common.Audit;
import com.openkoda.model.common.AuditableEntity;
import com.openkoda.model.component.ControllerEndpoint;
import com.openkoda.model.component.Form;
import com.openkoda.model.component.FrontendResource;
import com.openkoda.model.component.ServerJs;
import com.openkoda.model.component.event.EventListenerEntry;
import com.openkoda.model.task.Email;
import com.openkoda.model.task.HttpRequestTask;
import com.openkoda.model.task.Task;
import com.openkoda.repository.admin.AuditRepository;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Map.Entry;

/**
 * Singleton Spring service implementing Hibernate auditing for configured entity types via lifecycle event interception.
 * <p>
 * Extends {@link PersistanceInterceptor} to capture entity changes (ADD, EDIT, DELETE) during Hibernate flush operations 
 * and persist to audit trail. Maintains registry (auditListeners) mapping 18 predefined auditable entity classes to 
 * {@link PropertyChangeListener} instances. Intercepts onSave (INSERT), onFlushDirty (UPDATE), onDelete (DELETE) 
 * lifecycle events, delegates to computeChanges for diff generation, accumulates {@link AuditedObjectState} entries 
 * in session-scoped auditMap. In beforeTransactionCompletion, converts accumulated state to Audit[] via 
 * PropertyChangeListener.prepareAuditLogs, persists with AuditRepository.saveAll + flush inside active transaction. 
 * Registry can be extended via {@link com.openkoda.core.customisation.BasicCustomisationService} for custom entity 
 * audit support.
 * </p>
 * <p>
 * <b>Predefined Auditable Entities</b> (lines 79-98): Task, Organization, UserRole, User, Role, Form 
 * (registered twice - line 84 and 98), ServerJs, OrganizationRole, GlobalRole, FrontendResource, ControllerEndpoint, 
 * Email, HttpRequestTask, EventListenerEntry, FacebookUser, GoogleUser, LDAPUser, LoginAndPassword, DynamicEntity.
 * </p>
 * <p>
 * <b>CRITICAL Transaction Safety Warning</b>: beforeTransactionCompletion executes inside active Hibernate transaction. 
 * Errors during audit persistence can rollback the entire transaction including audited changes. Audit save failures 
 * affect business logic commits.
 * </p>
 * <p>
 * <b>Thread Safety</b>: Singleton Spring @Service with mutable auditListeners registry. Registry modifications not 
 * synchronized (constructor initialization only). Called by session-scoped PropertyChangeInterceptor instances with 
 * per-session auditMap.
 * </p>
 *
 * @see PropertyChangeInterceptor Session-scoped Hibernate hook that delegates to this service
 * @see PersistanceInterceptor Abstract base providing computeChanges algorithm
 * @see AuditedObjectState Immutable snapshot of entity state
 * @see PropertyChangeListener Converts snapshots to persistent Audit entities
 * @see com.openkoda.core.customisation.BasicCustomisationService Extension mechanism for custom entities
 * @see <a href="https://docs.jboss.org/hibernate/orm/3.5/api/org/hibernate/Interceptor.html">Hibernate Interceptor API</a>
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
@Service
public class AuditInterceptor extends PersistanceInterceptor implements LoggingComponentWithRequestId {

    /**
     * Registry mapping auditable entity classes to PropertyChangeListener instances for Audit entity construction.
     * <p>
     * Populated in constructor with 18 predefined entity types. Extensible via registerAuditableClass for custom 
     * entities. Not synchronized - modifications only in constructor.
     * </p>
     */
    private Map<Class<? extends AuditableEntity>, PropertyChangeListener> auditListeners;

    /**
     * Injected repository for batch persisting Audit entities in beforeTransactionCompletion.
     */
    @Inject
    private AuditRepository auditRepository;
    
    /**
     * Injected service for resolving authenticated users (currently unused - UserProvider.getFromContext() used instead at line 238).
     */
    @Inject
    private OrganizationUserDetailsService userService;

    /**
     * Initializes audit interceptor with 18 predefined auditable entity types registered for change tracking.
     * <p>
     * Constructs empty HashMap for auditListeners, then registers 18 standard entity classes: Task, Organization, 
     * UserRole, User, Role, Form (twice - lines 84 and 98), ServerJs, OrganizationRole, GlobalRole, FrontendResource, 
     * ControllerEndpoint, Email, HttpRequestTask, EventListenerEntry, FacebookUser, GoogleUser, LDAPUser, 
     * LoginAndPassword, DynamicEntity. Each registration creates PropertyChangeListener with simple class name and 
     * human-readable label.
     * </p>
     * <p>
     * <b>Note</b>: Form class registered twice at lines 84 and 98 - likely unintentional duplicate. Registry 
     * extensible via registerAuditableClass after construction.
     * </p>
     */
    public AuditInterceptor() {
        //standard listeners, can be extended with @{@link com.openkoda.core.customisation.BasicCustomisationService}
        auditListeners = new HashMap<>();
        registerAuditableClass(Task.class, "Task");
        registerAuditableClass(Organization.class, "Organization");
        registerAuditableClass(UserRole.class, "User Role");
        registerAuditableClass(User.class, "User");
        registerAuditableClass(Role.class, "Role");
        registerAuditableClass(Form.class, "Form");
        registerAuditableClass(ServerJs.class, "Server Js");
        registerAuditableClass(OrganizationRole.class, "Organization Role");
        registerAuditableClass(GlobalRole.class, "Global Role");
        registerAuditableClass(FrontendResource.class, "Frontend Resource");
        registerAuditableClass(ControllerEndpoint.class, "Controller Endpoint");
        registerAuditableClass(Email.class, "Email");
        registerAuditableClass(HttpRequestTask.class, "Http Request Task");
        registerAuditableClass(EventListenerEntry.class, "Event Listener");
        registerAuditableClass(FacebookUser.class, "Facebook User");
        registerAuditableClass(GoogleUser.class, "Google User");
        registerAuditableClass(LDAPUser.class, "LDAP User");
        registerAuditableClass(LoginAndPassword.class, "Regular User");
        registerAuditableClass(DynamicEntity.class, "Dynamic Entity");
        registerAuditableClass(Form.class, "Form");
    }

    /**
     * Registers entity class for audit trail tracking by creating and storing PropertyChangeListener.
     * <p>
     * Creates PropertyChangeListener with class simple name and display label, stores in auditListeners map keyed 
     * by class. Allows extension of audit coverage beyond 18 predefined types. Called by constructor for standard 
     * entities and can be invoked by BasicCustomisationService for custom entities.
     * </p>
     * <p>
     * Usage example: {@code registerAuditableClass(CustomEntity.class, "Custom Entity")}
     * </p>
     *
     * @param c Entity class to audit (must extend AuditableEntity)
     * @param classLabel Human-readable label for audit display (e.g., "Organization", "User Role")
     * @return Created PropertyChangeListener instance stored in registry
     */
    public <T extends AuditableEntity> PropertyChangeListener registerAuditableClass(Class<T> c, String classLabel) {
        debug("[registerAuditableClass] {}", classLabel);
        PropertyChangeListener changeListener = new PropertyChangeListener(c.getSimpleName(), classLabel);
        auditListeners.put(c, changeListener);
        return changeListener;
    }

    /**
     * Removes entity class from audit registry, disabling change tracking.
     *
     * @param c Entity class to stop auditing
     */
    public void unregisterAuditableClass(Class c) {
        debug("[unregisterAuditableClass] {}", c);
        auditListeners.remove(c);
    }

    /**
     * Checks if entity class is registered for audit tracking.
     *
     * @param c Entity class to check
     * @return true if class has registered PropertyChangeListener, false otherwise
     */
    public boolean isAuditableClass(Class c) {
        debug("[isAuditableClass] {}", c);
        return auditListeners.containsKey(c);
    }

    /**
     * Hibernate lifecycle hook for UPDATE operations, captures entity property changes via computeChanges delegation.
     * <p>
     * Called by PropertyChangeInterceptor when Hibernate detects dirty entity during flush. Checks if entity class 
     * is registered (line 138), delegates to inherited computeChanges from PersistanceInterceptor for diff generation, 
     * populates auditMap with EDIT operation AuditedObjectState. Skips unregistered entity types.
     * </p>
     *
     * @param auditMap Session-scoped map accumulating AuditedObjectState entries
     * @param entity Entity being updated
     * @param id Entity primary key
     * @param currentState Array of current property values after modification
     * @param previousState Array of previous property values before modification
     * @param propertyNames Array of property names matching state indices
     * @param types Hibernate Type metadata (unused)
     * @return false to allow operation to proceed
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean onFlushDirty(Map<Object, AuditedObjectState> auditMap, Object entity, Object id, Object[] currentState, Object[] previousState, String[] propertyNames,
                                Type[] types) {
        debug("[onFlushDirty] entity {} id {}", entity, id);

        //skip if no listener for the entity type
        if (this.isEntitySpecificListenerRegistered(entity.getClass())) {
            computeChanges(auditMap, entity, currentState, previousState, propertyNames);
        }
        return false;
    }

    /**
     * Helper checking if PropertyChangeListener is registered for entity class.
     *
     * @param clazz Entity class to check
     * @return true if auditListeners contains non-null entry for class, false otherwise
     */
    private boolean isEntitySpecificListenerRegistered(Class clazz) {
        return auditListeners.get(clazz) != null;
    }

    /**
     * Hibernate lifecycle hook for INSERT operations, captures new entity properties for ADD operation audit.
     * <p>
     * Called by PropertyChangeInterceptor when new entity is persisted. Checks if entity class registered (line 169), 
     * iterates through entityState array building properties and changes maps. Skips: null values (line 174-176), 
     * Collections (line 177-179), blank strings (line 181-183), ignored properties via isIgnoredProperty (line 191-193). 
     * Handles content properties separately via isContentProperty (line 185-189). Creates AuditedObjectState with ADD 
     * operation and empty string as 'before' value in changes map (line 187, 195).
     * </p>
     * <p>
     * <b>Implementation Note</b>: Properties map contains simple string values (line 194), changes map contains 
     * ImmutablePair('', currentValue) pairs (line 195).
     * </p>
     *
     * @param auditMap Session-scoped map accumulating AuditedObjectState entries
     * @param entity Entity being inserted (must implement AuditableEntity)
     * @param id Generated or assigned entity ID
     * @param entityState Array of property values for new entity
     * @param propertyNames Array of property names matching entityState indices
     * @param types Hibernate Type metadata (unused)
     * @return false to allow operation to proceed
     */
    @Override
    @SuppressWarnings("unchecked")

    public boolean onSave(Map<Object, AuditedObjectState> auditMap, Object entity, Object id, Object[] entityState, String[] propertyNames, Type[] types) {
        debug("[onSave] entity {} id {}", entity, id);
        
        //skip if no listener for the entity type
        if (this.isEntitySpecificListenerRegistered(entity.getClass())) {
            Map<String, String> properties = new HashMap<>();
            Map<String, Entry<String, String>> changes = new HashMap<>();
            String content = null;
            for (int i = 0; i < entityState.length; i++) {
                if (entityState[i] == null) {
                    continue;
                }
                if (Collection.class.isAssignableFrom(entityState[i].getClass())) {
                    continue;
                }

                if (entityState[i] instanceof String && StringUtils.isBlank((String) entityState[i])) {
                    continue;
                }

                if (isContentProperty(entity, propertyNames[i])) {
                    content = toString(entityState[i]);
                    changes.put(propertyNames[i], new ImmutablePair<>("", toString(entityState[i])));
                    continue;
                }

                if (isIgnoredProperty(entity, propertyNames[i])) {
                    continue;
                }
                properties.put(propertyNames[i], toString(entityState[i]));
                changes.put(propertyNames[i], new ImmutablePair<>("", toString(entityState[i])));
            }
            auditMap.put(entity, new AuditedObjectState(properties, changes, content, Audit.AuditOperation.ADD));
            debug("[onSave] auditMap updated");
        }
        return false;
    }

    /**
     * Hibernate lifecycle hook for DELETE operations, creates minimal DELETE operation audit with empty properties.
     * <p>
     * Called by PropertyChangeInterceptor when entity is deleted. Checks if entity class registered (line 219), 
     * creates AuditedObjectState with empty properties and changes maps and DELETE operation (line 220). entityState, 
     * propertyNames, types are unused - DELETE audit only captures entity identity via PropertyChangeListener.getEntityId.
     * </p>
     * <p>
     * <b>Implementation Note</b>: Empty HashMaps at line 220 - DELETE operation only records "Deleted EntityClass 
     * [auditString]" via AuditChangeFactory.
     * </p>
     *
     * @param auditMap Session-scoped map accumulating AuditedObjectState entries
     * @param entity Entity being deleted (must implement AuditableEntity)
     * @param id Entity ID being deleted
     * @param entityState Array of property values before deletion (unused in DELETE audit)
     * @param propertyNames Array of property names (unused in DELETE audit)
     * @param types Hibernate Type metadata (unused)
     */
    @Override
    public void onDelete(Map<Object, AuditedObjectState> auditMap, Object entity, Object id, Object[] entityState, String[] propertyNames, Type[] types) {
        debug("[onDelete] entity {} id {}", entity, id);

        //skip if no listener for the entity type
        if (isEntitySpecificListenerRegistered(entity.getClass())) {
            auditMap.put(entity, new AuditedObjectState(new HashMap(), new HashMap(), Audit.AuditOperation.DELETE));
            debug("[onDelete] auditMap updated");
        }
    }


    /**
     * Converts accumulated AuditedObjectState entries to persistent Audit entities and saves within active transaction.
     * <p>
     * <b>CRITICAL</b> method called by PropertyChangeInterceptor just before transaction commit. For each entry in 
     * auditMap: (1) Resolves PropertyChangeListener from auditListeners by entity class (line 240), (2) Calls 
     * prepareAuditLogs to build Audit[] with metadata (user, IP, request ID, organization, change description), 
     * (3) Batch saves with auditRepository.saveAll (line 241), (4) Flushes to database (line 242). Finally clears 
     * auditMap (line 246). Resolves authenticated user via UserProvider.getFromContext() (line 238).
     * </p>
     * <p>
     * <b>RUNS INSIDE ACTIVE TRANSACTION</b> - audit save errors can rollback entire transaction including business changes.
     * </p>
     * <p>
     * <b>Important</b>: If multiple changes on same field during one session, the saved will be the last change on 
     * the entity, because of that there is a possibility that the old state differs from previous audit log new state.
     * </p>
     * <p>
     * <b>Transaction Warning</b>: Audit persistence failures throw exceptions that rollback surrounding transaction. 
     * Ensure auditRepository operations succeed or business data commits will be lost.
     * </p>
     * <p>
     * <b>Note</b>: auditMap.clear() at line 246 in finally block ensures cleanup even if audit save fails. Comment 
     * "do we need this?" suggests uncertainty - clearing is safe given session-scoped lifecycle.
     * </p>
     *
     * @param auditMap Session-scoped map with accumulated AuditedObjectState entries keyed by entity instance
     * @param tx Hibernate Transaction about to commit
     */
    public void beforeTransactionCompletion(Map<Object, AuditedObjectState> auditMap, Transaction tx) {
        try {
            debug("[beforeTransactionCompletion]");
            Optional<OrganizationUser> user = UserProvider.getFromContext();
            auditMap.forEach((k, v) -> {
                Audit[] audits = auditListeners.get(k.getClass()).prepareAuditLogs(k, user, v, user.map(OrganizationUser::getRolesInfo));
                auditRepository.saveAll(new ArrayList(Arrays.asList(audits)));
                auditRepository.flush();
            });
        } finally {
            // do we need this?
            auditMap.clear();
        }
    }
}
