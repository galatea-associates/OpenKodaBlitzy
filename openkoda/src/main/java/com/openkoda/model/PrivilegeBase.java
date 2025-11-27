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

package com.openkoda.model;

import com.openkoda.model.common.LongIdEntity;
import org.springframework.util.Assert;

import static com.openkoda.core.security.HasSecurityRules.BB_CLOSE;
import static com.openkoda.core.security.HasSecurityRules.BB_OPEN;

/**
 * Base interface for privilege type definitions providing uniform privilege handling across static enums and dynamic runtime privileges.
 * <p>
 * Defines common contract for both {@link Privilege} enum (canonical system privileges) and {@link DynamicPrivilege} 
 * entities (runtime-defined custom privileges). Enables polymorphic privilege handling in authorization checks, 
 * role assignments, and permission evaluations. Provides default implementations for database value calculation 
 * (name wrapped in brackets), privilege naming conventions, and name validation via {@code checkName()} method.

 * <p>
 * This interface uses the Strategy pattern for privilege abstraction. Both static {@link Privilege} enum entries 
 * and dynamic {@link DynamicPrivilege} entity instances implement this interface, allowing the security system 
 * to handle all privilege types uniformly without runtime type checking.

 * <p>
 * Example usage:
 * <pre>
 * PrivilegeBase privilege = Privilege.canReadOrgData;
 * String dbValue = privilege.getDatabaseValue(); // Returns "[canReadOrgData]"
 * </pre>

 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 2019-01-31
 * @see Privilege for canonical privilege enumeration
 * @see DynamicPrivilege for runtime-defined privileges
 * @see Role for privilege assignments
 */
public interface PrivilegeBase extends LongIdEntity {

    /**
     * Returns human-readable display label for UI presentation.
     * <p>
     * Used in administrative interfaces, role management screens, and privilege selection dialogs 
     * to present a user-friendly description of the privilege. For {@link Privilege} enums, 
     * this is a predefined localization-ready label. For {@link DynamicPrivilege} entities, 
     * this is typically stored in the database.

     *
     * @return display label suitable for end-user presentation, never {@code null}
     */
    String getLabel();

    /**
     * Returns {@link PrivilegeGroup} categorization for privilege organization and UI grouping.
     * <p>
     * Privileges are organized into groups for administrative purposes, UI organization, 
     * and privilege management workflows. Common groups include data access, entity management, 
     * system administration, and integration privileges.

     *
     * @return privilege group for categorization, never {@code null}
     * @see PrivilegeGroup
     */
    PrivilegeGroup getGroup();

    /**
     * Returns category string for privilege classification.
     * <p>
     * Provides additional categorization beyond group-level organization. Used for fine-grained 
     * privilege filtering and organizational hierarchy in complex permission models.

     *
     * @return category string for classification, may be {@code null} for uncategorized privileges
     */
    String getCategory();

    /**
     * Returns privilege name/code as String.
     * <p>
     * Used for privilege lookups, serialization to database, and programmatic privilege references. 
     * For {@link Privilege} enums, this returns the enum constant name. For {@link DynamicPrivilege} 
     * entities, this returns the stored name field. The name must follow naming conventions validated 
     * by {@link #checkName(String)}.

     *
     * @return privilege name/code, never {@code null}
     */
    String name();

    /**
     * Returns whether privilege is hidden from standard UI displays.
     * <p>
     * Hidden privileges are used internally by the system and should not be directly assignable 
     * by administrators in typical privilege management interfaces. Default implementation returns 
     * {@code false}, making privileges visible by default.

     *
     * @return {@code true} if privilege should be hidden from standard UI, {@code false} otherwise
     */
    default boolean isHidden() { return false; };

    /**
     * Returns ID multiplier offset for privilege value calculation.
     * <p>
     * Enables privilege range partitioning and ID namespace management. Default implementation 
     * returns 0, but can be overridden to support multiple privilege ID ranges for different 
     * privilege types or organizational scopes.

     *
     * @return ID offset multiplier, default 0
     */
    default int idOffset() { return 0; };

    /**
     * Returns privilege value for database storage and serialization.
     * <p>
     * Default implementation wraps the privilege {@link #name()} in bracket delimiters 
     * (e.g., "[canReadOrgData]") using {@code BB_OPEN} and {@code BB_CLOSE} constants 
     * from {@code HasSecurityRules}. This format is used for privilege storage in role 
     * privilege strings and permission evaluation queries.

     * <p>
     * Implementations may override this to provide alternative serialization formats 
     * or to incorporate the {@link #idOffset()} in the value calculation.

     *
     * @return database storage value with bracket delimiters, never {@code null}
     * @see com.openkoda.core.security.HasSecurityRules
     */
    default String getDatabaseValue() {
        return BB_OPEN + name() + BB_CLOSE;
    }

    /**
     * Validates privilege name matches expected naming convention.
     * <p>
     * Default implementation checks if {@link #name()} equals the provided {@code nameCheck} 
     * parameter, throwing an {@link IllegalArgumentException} on mismatch. This prevents 
     * configuration drift and ensures privilege definitions remain consistent with their 
     * declared names. Typically used during system initialization to validate enum privilege 
     * definitions.

     *
     * @param nameCheck expected privilege name to validate against
     * @throws IllegalArgumentException if {@code nameCheck} does not match {@link #name()}
     */
    default void checkName(String nameCheck) {
        Assert.isTrue(this.name().equals(nameCheck), "Provided nameCheck have to be exactly the same as enum name.");
    }

    /**
     * Returns whether privilege can be removed from the system.
     * <p>
     * Core system privileges are non-removable to maintain security integrity. Custom 
     * {@link DynamicPrivilege} instances may override this to return {@code true}, allowing 
     * administrative deletion. Default implementation returns {@code false}, protecting 
     * privileges from accidental removal.

     *
     * @return {@code true} if privilege can be deleted, {@code false} for protected privileges
     */
    default boolean removable() {
        return false;
    }
    
    /**
     * Returns unique privilege ID used as database foreign key and privilege identifier.
     * <p>
     * Overrides {@link LongIdEntity#getId()} to provide privilege-specific ID handling. 
     * For {@link Privilege} enums, this returns the enum's ordinal-based ID. For 
     * {@link DynamicPrivilege} entities, this returns the database-generated ID. Default 
     * implementation returns {@code 0L} as a placeholder, but all concrete implementations 
     * must provide meaningful IDs.

     *
     * @return unique privilege identifier, {@code 0L} in default implementation
     */
    default @Override Long getId() {
        return 0l;
    }
}
