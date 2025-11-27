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

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.common.AuditableEntity;
import com.openkoda.model.common.AuditableEntityOrganizationRelated;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Stateless factory service formatting AuditedObjectState snapshots into human-readable HTML change descriptions for audit trail display.
 * <p>
 * Converts entity change snapshots into HTML-formatted audit descriptions for display in audit UI. Handles three operation types:
 * ADD (entity creation), EDIT (property updates), DELETE (entity removal). Uses StringUtils.splitByCharacterTypeCamelCase to generate
 * human-readable field labels from camel case property names. Returns composed HTML strings with bold labels and line breaks.
 * Does not render large content payloads (intentional design - only adds 'Content' label without payload body at line 119).
 * 
 * <p>
 * Stateless service with no fields. Contains minor HTML fragment inconsistencies: uses '&lt;br/&gt;' (lines 70, 81, 106) and
 * '&lt;/br&gt;' (line 118) variants. Debug tracing via LoggingComponentWithRequestId mixin.
 * 
 * <p>
 * Thread-safety: Stateless and thread-safe.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see AuditedObjectState
 * @see PropertyChangeListener
 * @see AuditableEntity#toAuditString()
 */
@Service
public class AuditChangeFactory implements LoggingComponentWithRequestId {

    /**
     * Creates HTML-formatted change description for audit log entry based on operation type.
     * <p>
     * Dispatches to operation-specific formatters: getAddChangeDescription for ADD, getEditChangeDescription for EDIT,
     * getDeleteChangeDescription for DELETE. Returns empty string for unknown operations (default case line 61).
     * 
     *
     * @param auditedObject   Entity being audited, must implement AuditableEntity.toAuditString() for EDIT/DELETE descriptions
     * @param aos             Immutable snapshot of entity state with properties, changes, operation type, and optional content
     * @param entityClassName Simple class name for display (e.g., 'Organization', 'User')
     * @return HTML-formatted change description with bold labels and &lt;br/&gt; line breaks, or empty string for unknown operation
     * @see AuditableEntityOrganizationRelated
     * @see PropertyChangeListener
     */
    public String createChange(AuditableEntity auditedObject, AuditedObjectState aos, String entityClassName) {
        debug("[createChange] {} {} {}", auditedObject, aos, entityClassName);
        StringBuilder change = new StringBuilder();
        change.append(entityClassName);
        switch (aos.getOperation()) {
            case ADD:
                return getAddChangeDescription(aos, entityClassName, change);
            case EDIT:
                return getEditChangeDescription(auditedObject, aos, entityClassName, change);
            case DELETE:
                return getDeleteChangeDescription(auditedObject, entityClassName, change);
            default:
                return "";
        }
    }

    /**
     * Formats ADD operation as 'EntityClass created with: &lt;properties&gt;'.
     *
     * @param aos         State snapshot containing properties map
     * @param entityClass Entity class name for display
     * @param change      StringBuilder initialized with entity class name
     * @return HTML string like 'Organization created with:&lt;br/&gt;&lt;b&gt;Name&lt;/b&gt; from &lt;b&gt;&lt;/b&gt; to &lt;b&gt;TenantCo&lt;/b&gt;&lt;br/&gt;'
     */
    private String getAddChangeDescription(AuditedObjectState aos, String entityClass, StringBuilder change) {
        debug("[getAddChangeDescription] entityClass: {}", entityClass);
        change.append(" created with:<br/>");
        writeProperties(aos, change);
        writeContent(aos, change);
        return change.toString();
    }

    /**
     * Formats EDIT operation as 'EntityClass [auditString] &lt;properties&gt;'.
     *
     * @param p           Audited entity providing toAuditString() identifier
     * @param aos         State snapshot containing changed properties
     * @param entityClass Entity class name for display
     * @param change      StringBuilder initialized with entity class name
     * @return HTML string like 'Organization TenantCo&lt;br/&gt;&lt;b&gt;Logo Id&lt;/b&gt; from &lt;b&gt;123&lt;/b&gt; to &lt;b&gt;456&lt;/b&gt;&lt;br/&gt;'
     */
    private String getEditChangeDescription(AuditableEntity p, AuditedObjectState aos, String entityClass, StringBuilder change) {
        debug("[getEditChangeDescription] entityClass: {}", entityClass);
        change.append(" ").append(p.toAuditString()).append("<br/>");
        writeProperties(aos, change);
        writeContent(aos, change);
        return change.toString();
    }

    /**
     * Formats DELETE operation as 'Deleted EntityClass [auditString]'.
     *
     * @param p           Deleted entity providing toAuditString() identifier
     * @param entityClass Entity class name for display
     * @param change      StringBuilder initialized with entity class name
     * @return HTML string like 'Deleted Organization TenantCo'
     */
    private String getDeleteChangeDescription(AuditableEntity p, String entityClass, StringBuilder change) {
        debug("[getDeleteChangeDescription] entityClass: {}", entityClass);
        change.append("Deleted ").append(entityClass).append(" ").append(p.toAuditString());
        return change.toString();
    }

    /**
     * Appends HTML-formatted property changes to StringBuilder with bold labels.
     * <p>
     * Returns early if properties map is empty (line 101-102). Uses getDefaultFieldLabel for human-readable field names.
     * 
     *
     * @param aos    State snapshot with properties map (propertyName -&gt; HTML change description)
     * @param change StringBuilder to append formatted properties
     */
    private void writeProperties(AuditedObjectState aos, StringBuilder change) {
        debug("[writeProperties] {} {}", aos, change);
        if(aos.getProperties().isEmpty()){
            return;
        }
        aos.getProperties().forEach((k, v) ->
                change.append("<b>").append(getDefaultFieldLabel(k))
                        .append("</b> ").append(v).append("<br/>")
        );
    }

    /**
     * Appends content label to StringBuilder but intentionally omits large payload body.
     * <p>
     * Does not render aos.getContent() payload - only adds 'Content' label at line 119. Design decision to avoid bloating
     * audit descriptions with large payloads.
     * 
     *
     * @param aos    State snapshot with optional content field
     * @param change StringBuilder to append content indicator
     */
    private void writeContent(AuditedObjectState aos, StringBuilder change) {
        debug("[writeContent] {} {}", aos, change);
        if (aos.getContent() == null) {
            return;
        }
        change.append("New values for properties:</br>");
        change.append("<b>Content</b> </br>");
    }

    /**
     * Creates a label for the given field by convention.
     * <p>
     * The routine is to split fieldName by camel case and make words upper case.
     * Example: Input 'organizationName' produces output 'Organization Name'.
     * 
     *
     * @param fieldName Camel case field name (e.g., 'logoId', 'firstName')
     * @return Human-readable label with spaces and title case (e.g., 'Logo Id', 'First Name')
     */
    private String getDefaultFieldLabel(String fieldName) {
        return StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(StringUtils.capitalize(fieldName)), ' ');
    }
}
