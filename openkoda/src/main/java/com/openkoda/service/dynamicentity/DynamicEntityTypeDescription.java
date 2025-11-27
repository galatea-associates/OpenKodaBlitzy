package com.openkoda.service.dynamicentity;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;

import java.util.List;

/**
 * Byte Buddy TypeDescription.Latent adapter for runtime dynamic entity type metadata.
 * <p>
 * Package-private minimal subclass of Byte Buddy's TypeDescription.Latent representing 
 * in-memory dynamic types. Provides lightweight, deterministic TypeDescription implementation 
 * for use by Byte Buddy during dynamic class generation pipeline.
 * 
 * 
 * <b>Byte Buddy Integration</b>
 * <p>
 * TypeDescription is Byte Buddy's core abstraction for representing Java types (classes, 
 * interfaces, arrays). TypeDescription.Latent is a concrete implementation that doesn't 
 * require actual {@code Class<?>} objects. This adapter extends Latent with OpenKoda-specific 
 * behavior:
 * 
 * <ul>
 *   <li>Returns null for declaring type (top-level generated classes)</li>
 *   <li>Returns empty annotation list (annotations added separately via builder)</li>
 * </ul>
 * 
 * <b>Design Rationale</b>
 * <p>
 * Separating type description from entity descriptor enables Byte Buddy integration without 
 * tight coupling to OpenKoda domain model. The Latent approach allows type metadata creation 
 * before actual bytecode generation.
 * 
 * 
 * <b>Usage Context</b>
 * <p>
 * Created by DynamicEntityDescriptor constructor and passed to DynamicEntityRegistrationService 
 * for Byte Buddy DynamicType.Builder operations. The typeDescription provides Byte Buddy with 
 * structural metadata (name, modifiers, superclass, interfaces) needed for bytecode generation.
 * 
 * 
 * <p><strong>Access Modifier:</strong> Package-private - only accessible within dynamicentity 
 * package by design</p>
 * 
 * <p><strong>Byte Buddy Compatibility:</strong> Compatible with Byte Buddy API as of version 
 * used in project dependencies</p>
 * 
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see net.bytebuddy.description.type.TypeDescription
 * @see net.bytebuddy.description.type.TypeDescription.Latent
 * @see DynamicEntityDescriptor
 * @see DynamicEntityRegistrationService
 */
class DynamicEntityTypeDescription extends TypeDescription.Latent {
    /**
     * Constructs a new DynamicEntityTypeDescription with specified type metadata.
     * <p>
     * Delegates to TypeDescription.Latent superclass constructor. Stores type metadata 
     * for subsequent Byte Buddy operations.
     * 
     * 
     * @param name fully qualified class name (e.g., 
     *             'com.openkoda.dynamicentity.generated.FormEntity_1234567890')
     * @param modifiers Java modifier flags (e.g., Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL)
     * @param superClass generic type descriptor for superclass (typically OpenkodaEntity.class)
     * @param interfaces list of generic interface descriptors this type implements 
     *                   (e.g., CanonicalObject)
     */
    DynamicEntityTypeDescription(final String name, final int modifiers, final Generic superClass, final List<? extends Generic> interfaces) {
        super(name, modifiers, superClass, interfaces);
    }

    /**
     * Returns the declaring type for this type description.
     * <p>
     * Overrides Latent behavior to explicitly return null. Indicates generated entities 
     * are not inner classes and have no declaring outer class.
     * 
     * 
     * @return null - generated classes are top-level (not nested/inner classes)
     */
    @Override
    public TypeDescription getDeclaringType() {
        return null;
    }

    /**
     * Returns the declared annotations for this type description.
     * <p>
     * Returns empty annotation list as type-level annotations (@Entity, @Table) are added 
     * separately during DynamicType.Builder operations in 
     * DynamicEntityRegistrationService.createDynamicEntityType().
     * 
     * 
     * @return empty AnnotationList - annotations added via Byte Buddy builder
     */
    @Override
    public AnnotationList getDeclaredAnnotations() {
        return new AnnotationList.Empty();
    }
}
