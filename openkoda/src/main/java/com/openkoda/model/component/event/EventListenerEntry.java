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

package com.openkoda.model.component.event;

import com.openkoda.core.helper.NameHelper;
import com.openkoda.model.Organization;
import com.openkoda.model.PrivilegeNames;
import com.openkoda.model.common.ComponentEntity;
import com.openkoda.model.common.ModelConstants;
import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Formula;

/**
 * JPA entity persisting event listener registrations to the 'event_listener' table.
 * <p>
 * This entity extends {@link ComponentEntity} for multi-tenancy support and stores complete event
 * listener configuration including event identity (eventClassName, eventName, eventObjectType),
 * consumer signature (consumerClassName, consumerMethodName, consumerParameterClassName), and up
 * to 4 static parameters (staticData1-4) passed to the consumer method at runtime.
 * </p>
 * <p>
 * Event listeners are registered at application startup and reloaded from the database, then
 * dispatched at runtime when matching events occur. The unique constraint on event_name,
 * consumer_method_name, and all static_data columns prevents duplicate registrations.
 * </p>
 * <p>
 * Persistence details: Sequence-generated Long id with allocationSize=10 using
 * ORGANIZATION_RELATED_ID_GENERATOR. Unique constraint enforced on (event_name,
 * consumer_method_name, static_data_1, static_data_2, static_data_3, static_data_4).
 * </p>
 * <p>
 * Thread-safety: This is a JPA entity managed by Hibernate session. All operations require proper
 * transaction context.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ComponentEntity
 * @see Event
 * @see Consumer
 * @see com.openkoda.form.EventListenerForm
 */
@Entity
@Table(name = "event_listener",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_name", "consumer_method_name", "static_data_1", "static_data_2", "static_data_3", "static_data_4"}))
public class EventListenerEntry extends ComponentEntity {

    /**
     * Inherited formula for computing reference string.
     * <p>
     * Uses DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA from ComponentEntity.
     * </p>
     */
    public static final String REFERENCE_FORMULA = DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA;

    /**
     * Sequence-generated primary key using ORGANIZATION_RELATED_ID_GENERATOR.
     * <p>
     * Allocation size of 10 for performance optimization during batch inserts.
     * </p>
     */
    @Id
    @SequenceGenerator(name = ORGANIZATION_RELATED_ID_GENERATOR, sequenceName = ORGANIZATION_RELATED_ID_GENERATOR, initialValue = ModelConstants.INITIAL_ORGANIZATION_RELATED_VALUE, allocationSize = 10)
    @GeneratedValue(generator = ModelConstants.ORGANIZATION_RELATED_ID_GENERATOR, strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Fully qualified class name of the event emitter.
     * <p>
     * Example: "com.openkoda.core.flow.PageModelMap"
     * </p>
     */
    @Column(name = "event_class_name")
    private String eventClassName;

    /**
     * Event identifier used for listener matching.
     * <p>
     * This name is matched against registered listeners to dispatch events at runtime.
     * </p>
     */
    @Column(name = "event_name")
    private String eventName;

    /**
     * Type of event object passed to the consumer method.
     * <p>
     * Fully qualified class name of the object passed as the event payload.
     * </p>
     */
    @Column(name = "event_object_type")
    private String eventObjectType;

    /**
     * Fully qualified class name of the consumer (event handler).
     * <p>
     * Example: "com.openkoda.service.notification.NotificationService"
     * </p>
     */
    @Column(name = "consumer_class_name")
    private String consumerClassName;

    /**
     * Method name invoked when the event is dispatched.
     * <p>
     * This method is called on the consumer class with the event object and static parameters.
     * </p>
     */
    @Column(name = "consumer_method_name")
    private String consumerMethodName;

    /**
     * Fully qualified parameter class name for the consumer method.
     * <p>
     * Specifies the type of the first parameter expected by the consumer method.
     * </p>
     */
    @Column(name = "consumer_parameter_class_name")
    private String consumerParameterClassName;

    /**
     * First static parameter passed to the consumer method at runtime.
     * <p>
     * Nullable. Part of the unique constraint with event_name and consumer_method_name.
     * </p>
     */
    @Column(name = "static_data_1")
    private String staticData1;

    /**
     * Second static parameter passed to the consumer method at runtime.
     * <p>
     * Nullable. Part of the unique constraint with event_name and consumer_method_name.
     * </p>
     */
    @Column(name = "static_data_2")
    private String staticData2;

    /**
     * Third static parameter passed to the consumer method at runtime.
     * <p>
     * Nullable. Part of the unique constraint with event_name and consumer_method_name.
     * </p>
     */
    @Column(name = "static_data_3")
    private String staticData3;

    /**
     * Fourth static parameter passed to the consumer method at runtime.
     * <p>
     * Nullable. Part of the unique constraint with event_name and consumer_method_name.
     * Up to 4 static parameters are supported.
     * </p>
     */
    @Column(name = "static_data_4")
    private String staticData4;

    /**
     * Database-generated index string for search and filtering.
     * <p>
     * Column defaults to empty string, insertable=false means value is managed by database.
     * </p>
     */
    @Column(name = INDEX_STRING_COLUMN, length = INDEX_STRING_COLUMN_LENGTH, insertable = false)
    @ColumnDefault("''")
    private String indexString;

    /**
     * Computed reference string via Formula annotation.
     * <p>
     * Uses DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA for consistent reference generation.
     * </p>
     */
    @Formula(REFERENCE_FORMULA)
    private String referenceString;

    /**
     * Computed required read privilege via Formula annotation.
     * <p>
     * Returns PrivilegeNames._canReadBackend for read access control.
     * </p>
     */
    @Formula("( '" + PrivilegeNames._canReadBackend + "' )")
    private String requiredReadPrivilege;

    /**
     * Computed required write privilege via Formula annotation.
     * <p>
     * Returns PrivilegeNames._canManageBackend for write access control.
     * </p>
     */
    @Formula("( '" + PrivilegeNames._canManageBackend + "' )")
    private String requiredWritePrivilege;

    /**
     * Returns the computed reference string for this event listener entry.
     *
     * @return the reference string computed via Formula annotation
     */
    @Override
    public String getReferenceString() {
        return referenceString;
    }

    /**
     * No-argument constructor required by JPA for entity instantiation.
     */
    public EventListenerEntry() {
    }

    /**
     * Creates event listener with event identity and consumer signature.
     * <p>
     * All static data parameters are set to null.
     * </p>
     *
     * @param eventClassName fully qualified class name of event emitter
     * @param eventName event identifier for listener matching
     * @param eventObjectType fully qualified class name of event object
     * @param consumerClassName fully qualified class name of consumer
     * @param consumerMethodName method name invoked when event is dispatched
     */
    public EventListenerEntry(String eventClassName, String eventName, String eventObjectType, String consumerClassName, String consumerMethodName) {
        this.eventClassName = eventClassName;
        this.eventName = eventName;
        this.eventObjectType = eventObjectType;
        this.consumerClassName = consumerClassName;
        this.consumerMethodName = consumerMethodName;
        this.staticData1 = null;
        this.staticData2 = null;
        this.staticData3 = null;
        this.staticData4 = null;
    }

    /**
     * Creates event listener with complete configuration including all static parameters.
     *
     * @param eventClassName fully qualified class name of event emitter
     * @param eventName event identifier for listener matching
     * @param eventObjectType fully qualified class name of event object
     * @param consumerClassName fully qualified class name of consumer
     * @param consumerMethodName method name invoked when event is dispatched
     * @param consumerParameterClassName fully qualified parameter class name
     * @param staticData1 first static parameter (nullable)
     * @param staticData2 second static parameter (nullable)
     * @param staticData3 third static parameter (nullable)
     * @param staticData4 fourth static parameter (nullable)
     */
    public EventListenerEntry(String eventClassName, String eventName, String eventObjectType, String consumerClassName, String consumerMethodName,
                              String consumerParameterClassName, String staticData1, String staticData2, String staticData3, String staticData4) {
        this.eventClassName = eventClassName;
        this.eventName = eventName;
        this.eventObjectType = eventObjectType;
        this.consumerClassName = consumerClassName;
        this.consumerMethodName = consumerMethodName;
        this.consumerParameterClassName = consumerParameterClassName;
        this.staticData1 = staticData1;
        this.staticData2 = staticData2;
        this.staticData3 = staticData3;
        this.staticData4 = staticData4;
    }

    /**
     * Copy constructor creating a new EventListenerEntry from an existing entry.
     * <p>
     * Copies all fields except id (new entity will get new id on persist).
     * </p>
     *
     * @param entry source entry to copy from
     */
    public EventListenerEntry(EventListenerEntry entry) {
        this.eventClassName = entry.getEventClassName();
        this.eventName = entry.getEventName();
        this.eventObjectType = entry.getEventObjectType();
        this.consumerClassName = entry.getConsumerClassName();
        this.consumerMethodName = entry.getConsumerMethodName();
        this.staticData1 = entry.getStaticData1();
        this.staticData2 = entry.getStaticData2();
        this.staticData3 = entry.getStaticData3();
        this.staticData4 = entry.getStaticData4();
    }

    /**
     * Sets the primary key id for this event listener entry.
     *
     * @param id the primary key value
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the fully qualified event class name.
     *
     * @return the event class name
     */
    public String getEventClassName() {
        return eventClassName;
    }

    /**
     * Returns the event class name, optionally in user-friendly format.
     * <p>
     * When userFriendly is true, delegates to NameHelper.getClassName() for shortened class name.
     * </p>
     *
     * @param userFriendly if true, returns shortened class name without package prefix
     * @return the event class name
     */
    public String getEventClassName(boolean userFriendly) {
        if (userFriendly) {
            return NameHelper.getClassName(getEventClassName());
        }
        return getEventClassName();
    }

    /**
     * Sets the fully qualified event class name.
     *
     * @param eventClassName the event class name
     */
    public void setEventClassName(String eventClassName) {
        this.eventClassName = eventClassName;
    }

    /**
     * Returns the event name identifier.
     *
     * @return the event name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Sets the event name identifier.
     *
     * @param eventName the event name
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Returns the fully qualified consumer class name.
     *
     * @return the consumer class name
     */
    public String getConsumerClassName() {
        return consumerClassName;
    }

    /**
     * Returns the consumer class name, optionally in user-friendly format.
     * <p>
     * When userFriendly is true, delegates to NameHelper.getClassName() for shortened class name.
     * </p>
     *
     * @param userFriendly if true, returns shortened class name without package prefix
     * @return the consumer class name
     */
    public String getConsumerClassName(boolean userFriendly) {
        if (userFriendly) {
            return NameHelper.getClassName(getConsumerClassName());
        }
        return getConsumerClassName();
    }

    /**
     * Sets the fully qualified consumer class name.
     *
     * @param consumerClassName the consumer class name
     */
    public void setConsumerClassName(String consumerClassName) {
        this.consumerClassName = consumerClassName;
    }

    /**
     * Returns the consumer method name.
     *
     * @return the consumer method name
     */
    public String getConsumerMethodName() {
        return consumerMethodName;
    }

    /**
     * Sets the consumer method name.
     *
     * @param consumerMethodName the consumer method name
     */
    public void setConsumerMethodName(String consumerMethodName) {
        this.consumerMethodName = consumerMethodName;
    }

    /**
     * Returns the first static parameter value.
     *
     * @return the first static parameter, may be null
     */
    public String getStaticData1() {
        return staticData1;
    }

    /**
     * Sets the first static parameter value.
     *
     * @param staticData1 the first static parameter
     */
    public void setStaticData1(String staticData1) {
        this.staticData1 = staticData1;
    }

    /**
     * Returns the second static parameter value.
     *
     * @return the second static parameter, may be null
     */
    public String getStaticData2() {
        return staticData2;
    }

    /**
     * Sets the second static parameter value.
     *
     * @param staticData2 the second static parameter
     */
    public void setStaticData2(String staticData2) {
        this.staticData2 = staticData2;
    }

    /**
     * Returns the third static parameter value.
     *
     * @return the third static parameter, may be null
     */
    public String getStaticData3() {
        return staticData3;
    }

    /**
     * Sets the third static parameter value.
     *
     * @param staticData3 the third static parameter
     */
    public void setStaticData3(String staticData3) {
        this.staticData3 = staticData3;
    }

    /**
     * Returns the fourth static parameter value.
     *
     * @return the fourth static parameter, may be null
     */
    public String getStaticData4() {
        return staticData4;
    }

    /**
     * Sets the fourth static parameter value.
     *
     * @param staticData4 the fourth static parameter
     */
    public void setStaticData4(String staticData4) {
        this.staticData4 = staticData4;
    }

    /**
     * Sets the database-generated index string.
     *
     * @param indexString the index string
     */
    public void setIndexString(String indexString) {
        this.indexString = indexString;
    }

    /**
     * Returns the fully qualified event object type class name.
     *
     * @return the event object type
     */
    public String getEventObjectType() {
        return eventObjectType;
    }

    /**
     * Returns the event object type, optionally in user-friendly format.
     * <p>
     * When userFriendly is true, delegates to NameHelper.getClassName() for shortened class name.
     * </p>
     *
     * @param userFriendly if true, returns shortened class name without package prefix
     * @return the event object type
     */
    public String getEventObjectType(boolean userFriendly) {
        if (userFriendly)
            return NameHelper.getClassName(getEventObjectType());
        return getEventObjectType();
    }

    /**
     * Sets the fully qualified event object type class name.
     *
     * @param eventObjectType the event object type
     */
    public void setEventObjectType(String eventObjectType) {
        this.eventObjectType = eventObjectType;
    }

    /**
     * Returns the database-generated index string.
     *
     * @return the index string
     */
    @Override
    public String getIndexString() {
        return indexString;
    }

    /**
     * Returns the primary key id.
     *
     * @return the id
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * Serializes event descriptor to comma-separated format.
     * <p>
     * Format: "eventClassName,eventName,eventObjectType" using StringUtils.join().
     * </p>
     *
     * @return comma-separated event descriptor string
     */
    public String getEventString() {
        return StringUtils.join(new String[] {eventClassName, eventName, eventObjectType}, ",");
    }

    /**
     * Computes canonical consumer method signature with parameter count.
     * <p>
     * Counts number of provided static parameters (0-4) and delegates to
     * Consumer.canonicalMethodName() for signature generation.
     * </p>
     *
     * @return canonical method name with parameter count
     */
    public String getConsumerString() {
        int n = staticData1 == null ? 0 : staticData2 == null ? 1 :staticData3 == null ? 2 :staticData4 == null ? 3 : 4;
        return Consumer.canonicalMethodName(consumerClassName, consumerMethodName, consumerParameterClassName, n);
    }

    /**
     * Returns the organization for multi-tenancy support.
     *
     * @return the organization
     */
    public Organization getOrganization() {
        return organization;
    }

    /**
     * Sets the organization for multi-tenancy support.
     *
     * @param organization the organization
     */
    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    /**
     * Returns the organization id foreign key.
     *
     * @return the organization id
     */
    @Override
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Sets the organization id foreign key.
     *
     * @param organizationId the organization id
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Returns the fully qualified consumer parameter class name.
     *
     * @return the consumer parameter class name
     */
    public String getConsumerParameterClassName() {
        return consumerParameterClassName;
    }

    /**
     * Sets the fully qualified consumer parameter class name.
     *
     * @param consumerParameterClassName the consumer parameter class name
     */
    public void setConsumerParameterClassName(String consumerParameterClassName) {
        this.consumerParameterClassName = consumerParameterClassName;
    }

    /**
     * Returns debug representation with all event listener fields.
     * <p>
     * Format: "EventListenerEntry{eventClassName='...', eventName='...', eventObjectType='...',
     * consumerClassName='...', consumerMethodName='...', staticData1='...', staticData2='...',
     * staticData3='...', staticData4='...'}"
     * </p>
     *
     * @return string representation for debugging
     */
    @Override
    public String toString() {
        return "EventListenerEntry{" +
                "eventClassName='" + eventClassName + '\'' +
                ", eventName='" + eventName + '\'' +
                ", eventObjectType='" + eventObjectType + '\'' +
                ", consumerClassName='" + consumerClassName + '\'' +
                ", consumerMethodName='" + consumerMethodName + '\'' +
                ", staticData1='" + staticData1 + '\'' +
                ", staticData2='" + staticData2 + '\'' +
                ", staticData3='" + staticData3 + '\'' +
                ", staticData4='" + staticData4 + '\'' +
                '}';
    }

    /**
     * Returns audit-friendly representation (ID only).
     *
     * @return audit string containing entity ID
     */
    @Override
    public String toAuditString() {
        return "ID: " + this.getId();
    }

    /**
     * Returns the computed required read privilege.
     *
     * @return PrivilegeNames._canReadBackend constant
     */
    @Override
    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    /**
     * Returns the computed required write privilege.
     *
     * @return PrivilegeNames._canManageBackend constant
     */
    @Override
    public String getRequiredWritePrivilege() {
        return requiredWritePrivilege;
    }
}
