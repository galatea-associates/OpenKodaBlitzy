package com.openkoda.core.audit;

import com.openkoda.core.helper.DatesHelper;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.common.Audit;
import com.openkoda.model.common.AuditableEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hibernate.type.Type;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;

/**
 * Abstract base interceptor providing shared entity change detection logic for Hibernate audit trail.
 * <p>
 * Implements the core change detection algorithm used by AuditInterceptor to capture entity modifications 
 * during Hibernate flush operations. Provides template methods (onSave, onFlushDirty, onDelete) for 
 * subclasses to hook into entity lifecycle, and shared computeChanges implementation that compares 
 * before/after property values, formats changes as HTML, handles special types (arrays, timestamps, 
 * nulls), respects entity-provided ignore rules (AuditableEntity.ignorePropertiesInAudit), and identifies 
 * large content properties (AuditableEntity.contentProperties) for separate storage. Stores results in 
 * session-scoped auditMap as AuditedObjectState entries.
 * </p>
 * <p>
 * Implementation notes: Uses FastDateFormat 'dd/MM/yyyy HH:mm:ss' for java.util.Date formatting (line 18), 
 * DatesHelper.formatDateTimeEN for LocalDateTime (lines 108-109). HTML output format: 
 * 'from &lt;b&gt;oldValue&lt;/b&gt; to &lt;b&gt;newValue&lt;/b&gt;' (line 111). Treats null as '[no value]' 
 * (line 164). Collections are not audited (line 84-85).
 * </p>
 * <p>
 * Thread-safety: Abstract class with static FastDateFormat field (thread-safe). Implementations must 
 * maintain session-scoped auditMap.
 * </p>
 *
 * @see AuditInterceptor
 * @see AuditedObjectState
 * @see AuditableEntity#ignorePropertiesInAudit()
 * @see AuditableEntity#contentProperties()
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public abstract class PersistanceInterceptor implements LoggingComponentWithRequestId {

    /**
     * Thread-safe FastDateFormat for java.util.Date formatting in audit change descriptions, 
     * pattern 'dd/MM/yyyy HH:mm:ss'.
     */
    protected static final FastDateFormat auditDateFormat = FastDateFormat.getInstance("dd/MM/yyyy HH:mm:ss");
    
    /**
     * Lifecycle hook for entity insertion, invoked by Hibernate on INSERT operations.
     * <p>
     * Subclass must implement to capture new entity state and populate auditMap with AuditedObjectState 
     * entry. Called during Hibernate flush when new entity is persisted.
     * </p>
     *
     * @param auditMap session-scoped map to store AuditedObjectState entries, keyed by entity instance
     * @param entity entity being inserted (must implement AuditableEntity)
     * @param id generated or assigned entity ID
     * @param entityState array of property values for new entity
     * @param propertyNames array of property names matching entityState indices
     * @param types Hibernate Type metadata (typically unused in audit logic)
     * @return true to veto the operation, false to proceed (typically false for audit)
     */
    public abstract boolean onSave(Map<Object, AuditedObjectState> auditMap, Object entity, Object id, Object[] entityState,
            String[] propertyNames, Type[] types);

    /**
     * Lifecycle hook for entity deletion, invoked by Hibernate on DELETE operations.
     * <p>
     * Subclass must implement to capture deleted entity identifier and populate auditMap. entityState, 
     * propertyNames, types typically unused for DELETE auditing.
     * </p>
     *
     * @param auditMap session-scoped map to store AuditedObjectState entries
     * @param entity entity being deleted (must implement AuditableEntity)
     * @param id entity ID being deleted
     * @param entityState property values (unused in typical DELETE audit)
     * @param propertyNames property names (unused in typical DELETE audit)
     * @param types Hibernate Type metadata (unused)
     */
    public abstract void onDelete(Map<Object, AuditedObjectState> auditMap, Object entity, Object id, Object[] entityState,
            String[] propertyNames, Type[] types);

    /**
     * Lifecycle hook for entity updates, invoked by Hibernate when dirty entity is flushed during UPDATE operations.
     * <p>
     * Subclass must implement to compare currentState vs previousState arrays and delegate to computeChanges 
     * for diff generation. Called during flush when Hibernate detects entity modifications.
     * </p>
     *
     * @param auditMap session-scoped map to store AuditedObjectState entries
     * @param entity entity being updated
     * @param id entity ID
     * @param currentState array of current property values after modification
     * @param previousState array of previous property values before modification
     * @param propertyNames array of property names matching state array indices
     * @param types Hibernate Type metadata
     * @return true to veto the operation, false to proceed (typically false)
     */
    public abstract boolean onFlushDirty(Map<Object, AuditedObjectState> auditMap, Object entity, Object id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types);

    /**
     * Core change detection algorithm comparing before/after property arrays and populating auditMap with AuditedObjectState.
     * <p>
     * Iterates through property arrays comparing previousState[i] vs currentState[i]. Reports changes when: 
     * (1) non-blank strings differ, (2) null transitions to non-null or vice versa, (3) non-collection objects 
     * differ via equals(). Skips changes for: (1) blank string to blank string transitions (line 78-79), 
     * (2) Collections (line 84-85), (3) properties in entity.ignorePropertiesInAudit() (line 89-91). 
     * Handles special types: arrays formatted via Arrays.toString() (lines 103-106), LocalDateTime via 
     * DatesHelper.formatDateTimeEN (lines 107-110), Date via FastDateFormat (line 167). Stores large content 
     * separately via entity.contentProperties() check (lines 93-97). Formats HTML as 'from &lt;b&gt;old&lt;/b&gt; 
     * to &lt;b&gt;new&lt;/b&gt;' (line 111). Creates or merges AuditedObjectState in auditMap with EDIT 
     * operation (lines 120-126).
     * </p>
     * <p>
     * Usage note: Called by onFlushDirty implementations. Merges properties if entity already in auditMap 
     * (line 123-124).
     * </p>
     *
     * @param auditMap session-scoped map accumulating AuditedObjectState entries for transaction
     * @param entity entity being audited (must implement AuditableEntity)
     * @param currentState array of current property values
     * @param previousState array of previous property values (parallel to currentState)
     * @param propertyNames array of property names (parallel to state arrays)
     */
    public void computeChanges(Map<Object, AuditedObjectState> auditMap, Object entity, Object[] currentState,
            Object[] previousState, String[] propertyNames) {
        Map<String, String> properties = new HashMap<>();
        Map<String, Entry<String, String>> changes = new HashMap<>();
        String content = null;
        for (int i = 0; i < currentState.length; i++) {

            //discover when the property change needs to be reported
            boolean report = false;

            if (currentState[i] instanceof String && StringUtils.isBlank((String) currentState[i]) && StringUtils.isBlank((String) previousState[i])) {
                report = false;
            } else if (currentState[i] == null) {
                if (previousState[i] != null) {
                    report = true;
                }
            } else if (Collection.class.isAssignableFrom(currentState[i].getClass())) {
                report = false;
            } else if (!currentState[i].equals(previousState[i])) {
                report = true;
            }
            if (isIgnoredProperty(entity, propertyNames[i])) {
                report = false;
            }

            if (isContentProperty(entity, propertyNames[i])) {
                report = false;
                content = toString(currentState[i]);
                changes.put(propertyNames[i], new ImmutablePair<>(toString(previousState[i]), toString(currentState[i])));
            }

            //if property changed, prepare change description and keep
            if (report) {
                String previousValue = toString(previousState[i]);
                String currentValue = toString(currentState[i]);
                if (isArray(previousState[i])) {
                    previousValue = Arrays.toString((Object[]) previousState[i]);
                    currentValue = Arrays.toString((Object[]) currentState[i]);
//                        properties.put(propertyNames[i], "from <b>" + Arrays.toString((Object[]) previousState[i]) + "</b> to <b>" + Arrays.toString((Object[]) currentState[i]) + "</b>");
                } else if (isTimestamp(previousState[i])) {
                    previousValue = DatesHelper.formatDateTimeEN((LocalDateTime) previousState[i]);
                    currentValue = DatesHelper.formatDateTimeEN((LocalDateTime) currentState[i]);
                }
                properties.put(propertyNames[i], "from <b>" + previousValue + "</b> to <b>" + currentValue + "</b>");
                changes.put(propertyNames[i], new ImmutablePair<>(previousValue, currentValue));
            }
        }

        //if any changes, create an entry for the entities changes
        //if the entity is changed within the transaction before, the latest changes wins
        if (!properties.isEmpty() || StringUtils.isNotBlank(content)) {
            debug("[onFlushDirty] create an entry for the entities changes");
            if (!auditMap.containsKey(entity)) {
                auditMap.put(entity, new AuditedObjectState(properties, changes, content, Audit.AuditOperation.EDIT));
            } else {
                auditMap.get(entity).getProperties().putAll(properties);
                auditMap.get(entity).setContent(content);
            }
        }
    }

    /**
     * Checks if property should be excluded from audit trail per entity configuration.
     * <p>
     * Delegates to AuditableEntity.ignorePropertiesInAudit() to allow entities to exclude sensitive fields 
     * (e.g., passwords, secrets) from audit logs.
     * </p>
     *
     * @param entity entity being audited (cast to AuditableEntity at line 136)
     * @param propertyName property name to check
     * @return true if property is in entity's ignore list and should be omitted from audit, false otherwise
     * @see AuditableEntity#ignorePropertiesInAudit()
     */
    protected boolean isIgnoredProperty(Object entity, String propertyName) {
        debug("[isIgnoredProperty] {}", propertyName);
        AuditableEntity e = (AuditableEntity) entity;
        return e.ignorePropertiesInAudit().contains(propertyName);
    }


    /**
     * Checks if property contains large content that should be stored in Audit.content field separately.
     * <p>
     * Delegates to AuditableEntity.contentProperties() to identify large text fields (e.g., email bodies, 
     * document content) for storage in dedicated content column instead of inline change description.
     * </p>
     *
     * @param entity entity being audited (cast to AuditableEntity at line 147)
     * @param propertyName property name to check
     * @return true if property is content field and should be stored in Audit.content, false for inline display
     * @see AuditableEntity#contentProperties()
     */
    protected boolean isContentProperty(Object entity, String propertyName) {
        debug("[isContentProperty] {}", propertyName);
        AuditableEntity e = (AuditableEntity) entity;
        return e.contentProperties().contains(propertyName);
    }

    /**
     * Type check for array values requiring Arrays.toString() formatting.
     *
     * @param o object to check
     * @return true if o is array type, false otherwise
     */
    protected boolean isArray(Object o) {
        return o != null && o.getClass().isArray();
    }

    /**
     * Type check for LocalDateTime values requiring DatesHelper.formatDateTimeEN formatting.
     *
     * @param o object to check
     * @return true if o is LocalDateTime instance, false otherwise
     */
    protected boolean isTimestamp(Object o) {
        return o instanceof LocalDateTime;
    }
    
    /**
     * Safe null-handling toString with special formatting for Date types.
     * <p>
     * Converts object to string representation: null becomes '[no value]' (line 164), java.util.Date 
     * formatted via FastDateFormat 'dd/MM/yyyy HH:mm:ss' (line 167), all others via Object.toString().
     * </p>
     *
     * @param object object to convert, may be null
     * @return string representation or '[no value]' for null
     */
    protected String toString(Object object) {
        if (object == null) {
            return "[no value]";
        }
        if (Date.class.isAssignableFrom(object.getClass())) {
            return auditDateFormat.format((Date) object);
        }
        return object.toString();
    }
}