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

import com.openkoda.core.flow.LoggingComponent;
import com.openkoda.core.helper.ApplicationContextProvider;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session-scoped Hibernate Interceptor delegating entity lifecycle events to AuditInterceptor for audit trail capture.
 * <p>
 * Implements Hibernate Interceptor SPI to hook into entity lifecycle events (onSave, onFlushDirty, onDelete, 
 * beforeTransactionCompletion). Maintains per-session ConcurrentHashMap (auditMap) accumulating AuditedObjectState 
 * entries during transaction. Delegates actual change detection to singleton AuditInterceptor resolved at runtime 
 * via ApplicationContextProvider to avoid circular dependency. Hibernate creates one instance per session, ensuring 
 * auditMap is session-scoped and thread-isolated.
 * 
 * <p>
 * <b>CRITICAL Configuration:</b> Must be configured in application.properties:
 * {@code spring.jpa.properties.hibernate.ejb.interceptor.session_scoped=com.openkoda.core.audit.PropertyChangeInterceptor}
 * Without this property, audit trail will not function.
 * 
 * <p>
 * <b>Instance Lifecycle:</b> Created per Hibernate session, destroyed when session closes. auditMap accumulates 
 * changes during flush operations, cleared implicitly on transaction commit via beforeTransactionCompletion delegation.
 * 
 * <p>
 * <b>Thread Safety:</b> Each Hibernate session has its own instance, so auditMap is naturally thread-isolated. 
 * ConcurrentHashMap used for extra safety but not strictly necessary given session isolation.
 * 
 *
 * @see AuditInterceptor
 * @see <a href="https://docs.jboss.org/hibernate/orm/3.3/reference/en/html/events.html">Hibernate Interceptors
 * reference</a>
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
@Component
public class PropertyChangeInterceptor implements Interceptor, LoggingComponent {

   private static final long serialVersionUID = 1L;


   /**
    * Per-session concurrent map accumulating AuditedObjectState entries during transaction.
    * <p>
    * Keyed by entity instance identity, values contain property changes, operation type, and optional content. 
    * Populated during onSave/onFlushDirty/onDelete, consumed in beforeTransactionCompletion for Audit entity 
    * persistence. ConcurrentHashMap provides thread-safety although session-scoped instances are naturally 
    * thread-isolated.
    * 
    */
   private Map<Object, AuditedObjectState> auditMap = new ConcurrentHashMap<>();


   /**
    * Hibernate callback invoked during flush when dirty entity is detected, delegates to AuditInterceptor for change detection.
    * <p>
    * Called by Hibernate during session flush when entity modifications are detected. Passes auditMap and all entity 
    * state arrays to AuditInterceptor.onFlushDirty for diff computation via PersistanceInterceptor.computeChanges. 
    * Changes are accumulated in auditMap for later persistence in beforeTransactionCompletion.
    * 
    *
    * @param entity Entity being flushed (must implement AuditableEntity if auditable)
    * @param id Entity primary key
    * @param currentState Array of current property values after modification
    * @param previousState Array of previous property values before modification
    * @param propertyNames Array of property names matching state array indices
    * @param types Hibernate Type metadata for properties
    * @return false to allow operation to proceed (audit never vetoes)
    */
   @Override
   public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState, String[] propertyNames,
            Type[] types) {
      debug("[onFlushDirty]");
      return getAuditInterceptor().onFlushDirty( auditMap , entity , id , currentState , previousState , propertyNames , types );
   }

   /**
    * Hibernate callback invoked on entity insertion, delegates to AuditInterceptor to capture new entity state.
    * <p>
    * Called by Hibernate during session flush when new entity is persisted. Passes auditMap and entity state 
    * to AuditInterceptor.onSave for ADD operation recording.
    * 
    *
    * @param entity Entity being inserted
    * @param id Generated or assigned entity ID
    * @param state Array of property values for new entity
    * @param propertyNames Array of property names matching state indices
    * @param types Hibernate Type metadata
    * @return false to allow operation to proceed
    */
   @Override
   public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
      debug("[onSave]");
      return getAuditInterceptor().onSave( auditMap , entity , id , state , propertyNames , types );
   }

   /**
    * Hibernate callback invoked on entity deletion, delegates to AuditInterceptor to capture deletion event.
    * <p>
    * Called by Hibernate during session flush when entity is deleted. Passes auditMap and entity details to 
    * AuditInterceptor.onDelete for DELETE operation recording.
    * 
    *
    * @param entity Entity being deleted
    * @param id Entity ID being deleted
    * @param state Array of property values before deletion (may be unused)
    * @param propertyNames Array of property names
    * @param types Hibernate Type metadata
    */
   @Override
   public void onDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
      debug("[onDelete]");
      getAuditInterceptor().onDelete( auditMap , entity , id , state , propertyNames , types );
   }

   /**
    * Hibernate callback invoked before transaction commit, converts accumulated AuditedObjectState entries to persistent Audit entities.
    * <p>
    * Called by Hibernate just before transaction commits, while database connection is still open. If auditMap is 
    * non-empty (line 96), delegates to AuditInterceptor.beforeTransactionCompletion which converts each 
    * AuditedObjectState to Audit[] via PropertyChangeListener, batches with AuditRepository.saveAll, and flushes 
    * within the active transaction. Errors during audit persistence can affect the surrounding transaction.
    * 
    * <p>
    * <b>Implementation Note:</b> Empty auditMap check (line 96) avoids unnecessary delegation. Audit persistence 
    * occurs in same transaction as audited changes - failure rolls back both.
    * 
    *
    * @param tx Hibernate Transaction about to commit
    */
   @Override
   public void beforeTransactionCompletion(Transaction tx) {
      debug("[beforeTransactionCompletion]");
      if ( !auditMap.isEmpty() ) {
         getAuditInterceptor().beforeTransactionCompletion( auditMap , tx );
      }
   }

   /**
    * Resolves singleton AuditInterceptor bean at runtime to avoid circular dependency.
    * <p>
    * Uses ApplicationContextProvider static lookup to obtain AuditInterceptor from Spring context. Runtime 
    * resolution necessary because PropertyChangeInterceptor is instantiated by Hibernate outside Spring's bean 
    * lifecycle, preventing constructor injection.
    * 
    *
    * @return Singleton AuditInterceptor Spring bean with registered PropertyChangeListener instances
    */
   protected AuditInterceptor getAuditInterceptor() {
      return ApplicationContextProvider.getContext().getBean( AuditInterceptor.class );
   }

}
