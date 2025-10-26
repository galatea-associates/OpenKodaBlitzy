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

import com.openkoda.model.common.Audit;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Immutable snapshot of entity state capturing property changes for audit trail generation.
 * <p>
 * Captures entity property changes during Hibernate flush operations for conversion to persistent 
 * Audit entities. Stores three types of data: (1) properties map with HTML-formatted change 
 * descriptions for display, (2) changes map with before/after value pairs for programmatic access, 
 * (3) optional large content field for payloads. Constructed by PersistanceInterceptor.computeChanges 
 * during entity lifecycle events (onSave, onFlushDirty, onDelete). Maps are assigned by reference 
 * without defensive copies for performance.
 * </p>
 * <p>
 * Stored in session-scoped ConcurrentHashMap during transaction, converted to Audit entities in 
 * beforeTransactionCompletion by PropertyChangeListener.
 * </p>
 * <p>
 * Thread-safety note: Immutable references but maps are mutable. Not thread-safe. Session-scoped lifecycle.
 * </p>
 *
 * @see com.openkoda.core.audit.PersistanceInterceptor#computeChanges
 * @see com.openkoda.core.audit.PropertyChangeListener#prepareAuditLogs
 * @see com.openkoda.model.common.Audit.AuditOperation
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class AuditedObjectState {

   /**
    * Map of property name to HTML-formatted change description (e.g., 'from &lt;b&gt;old&lt;/b&gt; to &lt;b&gt;new&lt;/b&gt;') 
    * for display in audit UI.
    */
   private final Map<String, String> properties;

   /**
    * Map of property name to immutable (before, after) value pairs for programmatic change detection.
    */
   private final Map<String, Entry<String, String>> changes;

   /**
    * Type of database operation that triggered this audit: ADD, EDIT, or DELETE.
    */
   private Audit.AuditOperation operation;

   /**
    * Optional large content field for storing payloads identified by AuditableEntity.contentProperties, may be null.
    */
   private String content;


   /**
    * Creates audit state snapshot without large content field.
    * 
    * @param properties Map of property names to HTML-formatted change descriptions, assigned by reference
    * @param changes Map of property names to (oldValue, newValue) Entry pairs, assigned by reference
    * @param operation Database operation type: ADD, EDIT, or DELETE
    */
   public AuditedObjectState(Map<String, String> properties, Map<String, Entry<String, String>> changes, Audit.AuditOperation operation) {
      super();
      this.properties = properties;
      this.changes = changes;
      this.operation = operation;
   }

   /**
    * Creates audit state snapshot with optional large content field.
    * 
    * @param properties Map of property names to HTML-formatted change descriptions, assigned by reference
    * @param changes Map of property names to (oldValue, newValue) Entry pairs, assigned by reference
    * @param content Large content payload from entity's contentProperties, or null
    * @param operation Database operation type: ADD, EDIT, or DELETE
    */
   public AuditedObjectState(Map<String, String> properties, Map<String, Entry<String, String>> changes, String content, Audit.AuditOperation operation) {
      super();
      this.properties = properties;
      this.changes = changes;
      this.content = content;
      this.operation = operation;
   }

   /**
    * Returns the map of property names to HTML-formatted change descriptions.
    * 
    * @return Map of property names to HTML descriptions, never null
    */
   public Map<String, String> getProperties() {
      return properties;
   }

   /**
    * Returns the optional large content payload.
    * 
    * @return Large content payload or null if not set
    */
   public String getContent() {
      return content;
   }

   /**
    * Sets optional large content field.
    * 
    * @param content Large content payload to store
    */
   public void setContent(String content) {
        this.content = content;
    }

   /**
    * Returns the map of property names to before/after value pairs.
    * 
    * @return Map of property names to (before, after) value pairs, never null
    */
   public Map<String, Entry<String, String>> getChanges() {
      return changes;
   }

   /**
    * Updates the operation type.
    * 
    * @param operation Operation type to set
    */
   public void setOperation(Audit.AuditOperation operation) {
      this.operation = operation;
   }

   /**
    * Returns the database operation type that triggered this audit.
    * 
    * @return Database operation type: ADD, EDIT, or DELETE
    */
   public Audit.AuditOperation getOperation() {
      return operation;
   }
}
