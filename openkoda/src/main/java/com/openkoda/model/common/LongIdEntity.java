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

package com.openkoda.model.common;

import java.io.Serializable;

/**
 * Foundational interface defining the contract for all entities using Long as their primary key type.
 * <p>
 * This interface serves as the root of the entity hierarchy in OpenKoda, providing type-safe access 
 * to entity identifiers without coupling to specific entity implementations. All OpenKoda domain entities 
 * implement this interface either directly or transitively through base classes like {@link TimestampedEntity} 
 * or {@link OpenkodaEntity}.

 * <p>
 * By extending {@link Serializable}, this interface enables entity serialization for multiple use cases:
 * <ul>
 *   <li>HTTP session storage for web applications</li>
 *   <li>Distributed caching across application nodes</li>
 *   <li>Remote method invocation and inter-process communication</li>
 *   <li>Persistence of entity state to external storage</li>
 * </ul>

 * <p>
 * <strong>Entity Hierarchy:</strong><br>
 * This interface is extended by several specialized interfaces that add additional contracts:
 * <ul>
 *   <li>{@link AuditableEntity} - adds auditing capabilities with creation and modification timestamps</li>
 *   <li>{@link OrganizationRelatedEntity} - adds multi-tenancy support with organization scoping</li>
 *   <li>{@link SearchableEntity} - adds full-text search capabilities (often combined with organization scoping)</li>
 * </ul>

 * <p>
 * <strong>Typical Implementations:</strong><br>
 * Most entities extend from base classes that implement this interface:
 * <ul>
 *   <li>{@link TimestampedEntity} - provides automatic timestamp management</li>
 *   <li>{@link OpenkodaEntity} - primary application base class combining multiple concerns</li>
 * </ul>

 * <p>
 * <strong>Equality and HashCode Best Practices:</strong><br>
 * When implementing {@code equals()} and {@code hashCode()} for entities:
 * <ul>
 *   <li>For persistent entities (ID not null): compare based on ID value for consistency across sessions</li>
 *   <li>For transient entities (ID is null): use business key comparison or identity comparison</li>
 *   <li>Ensure hashCode remains stable after entity persistence to maintain collection integrity</li>
 * </ul>
 * Example:
 * <pre>{@code
 * if (getId() != null) {
 *     return getId().equals(other.getId());
 * }
 * }</pre>

 * <p>
 * <strong>ID Generation Patterns:</strong><br>
 * Entity IDs are typically generated using JPA strategies defined in {@link ModelConstants}:
 * <ul>
 *   <li>Sequence-based: {@code @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_ID_GENERATOR")}</li>
 *   <li>Identity-based: {@code @GeneratedValue(strategy = GenerationType.IDENTITY)} for certain databases</li>
 * </ul>

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see TimestampedEntity
 * @see OpenkodaEntity
 * @see AuditableEntity
 * @see OrganizationRelatedEntity
 * @see ModelConstants
 */
public interface LongIdEntity extends Serializable {
    /**
     * Returns the entity's primary key identifier.
     * <p>
     * The ID is a unique Long value that identifies this entity instance within the persistence context. 
     * This method provides type-safe access to the primary key without requiring knowledge of the specific 
     * entity implementation.

     * <p>
     * <strong>Null Handling:</strong><br>
     * This method returns {@code null} for transient (unsaved) entities that have not yet been persisted 
     * to the database. Once an entity is saved, the ID is assigned by the persistence provider and will 
     * remain non-null for the lifetime of the entity.

     * <p>
     * <strong>Typical ID Generation Implementations:</strong>
     * <ul>
     *   <li>Sequence-generated IDs: {@code @GeneratedValue(strategy = GenerationType.SEQUENCE)} - 
     *       ID assigned before INSERT using database sequence (PostgreSQL, Oracle)</li>
     *   <li>Identity-generated IDs: {@code @GeneratedValue(strategy = GenerationType.IDENTITY)} - 
     *       ID assigned after INSERT using auto-increment column (MySQL, PostgreSQL IDENTITY)</li>
     * </ul>

     * <p>
     * <strong>ID Immutability:</strong><br>
     * Once an ID is assigned during entity persistence, it should never change. The ID is critical for:
     * <ul>
     *   <li>Entity equality comparison across persistence contexts</li>
     *   <li>Maintaining referential integrity in relationships</li>
     *   <li>Cache key generation and lookup</li>
     *   <li>Audit trail consistency</li>
     * </ul>
     * Changing an entity's ID after persistence can lead to data corruption and cache inconsistencies.

     * <p>
     * Example usage:
     * <pre>{@code
     * Organization org = new Organization();
     * org.getId(); // returns null (transient entity)
     * organizationRepository.save(org);
     * org.getId(); // returns assigned ID (e.g., 12345L)
     * }</pre>

     *
     * @return the entity's primary key ID as a Long value, or {@code null} if the entity is transient 
     *         (not yet persisted to the database)
     */
    Long getId();
}
