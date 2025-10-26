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

package com.openkoda.core.customisation;

import com.openkoda.model.common.LongIdEntity;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;


/**
 * Custom Hibernate ID generator implementing a hybrid identification strategy that supports both pre-assigned 
 * and sequence-generated primary keys.
 * <p>
 * This generator extends {@link SequenceStyleGenerator} to provide flexible ID assignment behavior:
 * when an entity already has an ID value assigned before persistence, that value is preserved; 
 * when no ID is present, a new value is generated from the configured database sequence.
 * </p>
 * <p>
 * This hybrid approach is essential for canonical entities where specific ID values must be guaranteed 
 * for system integrity. Common use cases include:
 * </p>
 * <ul>
 * <li>Predefined system roles with fixed IDs (e.g., ROLE_ADMIN with ID=1)</li>
 * <li>Canonical privilege definitions that must maintain consistent IDs across environments</li>
 * <li>Reference data entities that require deterministic identification</li>
 * <li>Multi-tenant scenarios where certain entities need synchronized IDs across tenants</li>
 * </ul>
 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * @Entity
 * public class Role extends LongIdEntity {
 *     @Id
 *     @GenericGenerator(name = "UseIdOrGenerate", 
 *                       strategy = "com.openkoda.core.customisation.UseIdOrGenerate")
 *     @GeneratedValue(generator = "UseIdOrGenerate")
 *     private Long id;
 *     
 *     // Pre-assign ID for canonical role
 *     Role adminRole = new Role();
 *     adminRole.setId(1L);  // ID preserved during save
 *     
 *     // Auto-generate ID for dynamic role
 *     Role customRole = new Role();
 *     // ID null - will be generated from sequence
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>Thread Safety:</b> This generator is thread-safe as it delegates to the parent 
 * {@link SequenceStyleGenerator} for sequence value generation and relies on Hibernate's 
 * session contract for concurrency management.
 * </p>
 * <p>
 * The generator requires entities to implement {@link LongIdEntity} to access the getId() method 
 * for checking pre-assigned values. When the entity's ID is null, generation is delegated to the 
 * parent SequenceStyleGenerator which retrieves the next value from the configured database sequence 
 * (typically GLOBAL_ID_GENERATOR with allocationSize=10 in OpenKoda).
 * </p>
 *
 * @see com.openkoda.model.Role Concrete usage example with predefined role IDs
 * @see SequenceStyleGenerator Parent class providing sequence-based generation
 * @see LongIdEntity Required interface for entities using this generator
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class UseIdOrGenerate extends SequenceStyleGenerator {
    /**
     * Generates a primary key value for the given entity using hybrid ID assignment logic.
     * <p>
     * This method implements conditional ID generation behavior:
     * </p>
     * <ol>
     * <li>If the entity object is null, throws HibernateException immediately</li>
     * <li>If the entity already has a non-null ID value, that existing ID is returned unchanged</li>
     * <li>If the entity's ID is null, delegates to parent SequenceStyleGenerator to fetch next sequence value</li>
     * </ol>
     * <p>
     * The method casts the entity to {@link LongIdEntity} to access the getId() method. This is safe 
     * because entities using this generator must implement LongIdEntity as specified in the 
     * {@code @GenericGenerator} configuration.
     * </p>
     * <p>
     * When sequence generation is triggered (ID is null), the parent class retrieves the next value 
     * from the database sequence configured for this generator, typically using the GLOBAL_ID_GENERATOR 
     * sequence with an allocation size of 10 for performance optimization.
     * </p>
     *
     * @param session the Hibernate session contract implementation providing access to the current 
     *                persistence context and database connection; used by parent generator for 
     *                sequence value retrieval. Must not be null.
     * @param obj the entity instance requiring ID generation; must be a non-null instance of 
     *            {@link LongIdEntity} or a class implementing that interface. The entity's current 
     *            ID value is inspected to determine whether to preserve existing ID or generate new one.
     * @return the primary key value to assign to the entity: either the entity's existing non-null ID 
     *         (preserving pre-assigned values), or a newly generated Long value from the database 
     *         sequence (when ID was null)
     * @throws HibernateException if obj parameter is null, wrapping a NullPointerException. 
     *                            May also be thrown by parent SequenceStyleGenerator if sequence 
     *                            access fails or session is invalid.
     */
    @Override
    public Object generate(SharedSessionContractImplementor session, Object obj) throws HibernateException {
        if (obj == null) throw new HibernateException(new NullPointerException());
        if ((((LongIdEntity) obj).getId()) == null) {
            return super.generate(session, obj);
        } else {
            return ((LongIdEntity) obj).getId();
        }
    }
}