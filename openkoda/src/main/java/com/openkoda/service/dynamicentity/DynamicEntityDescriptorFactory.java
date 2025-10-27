package com.openkoda.service.dynamicentity;

import com.openkoda.core.form.FrontendMappingFieldDefinition;

import java.util.*;
import java.util.stream.Collectors;

import static com.openkoda.core.helper.NameHelper.*;

/**
 * Static factory and registry for DynamicEntityDescriptor instances with thread-safety constraints.
 * <p>
 * Singleton factory pattern with static HashMap registry. Creates descriptors from form metadata and maintains
 * global list of all generated descriptors. Provides accessor methods for retrieving registered descriptors
 * filtered by load state. Used during application startup to load dynamic entities into JPA context and
 * Byte Buddy class generation pipeline.
 * </p>
 * 
 * <p><b>Registry Pattern:</b></p>
 * <p>
 * Static Map&lt;String, DynamicEntityDescriptor&gt; keyed by entityKey provides central descriptor storage.
 * Factory method {@link #create(String, String, Collection, Long)} adds new descriptors. Accessor methods
 * {@link #instances()} and {@link #loadableInstances()} return descriptor lists for different use cases.
 * </p>
 * 
 * <p><b>Lifecycle Integration:</b></p>
 * <ol>
 * <li>Application startup: Forms loaded from database</li>
 * <li>{@code create()} called per form to build descriptors</li>
 * <li>{@code loadableInstances()} returns unloaded descriptors</li>
 * <li>DynamicEntityRegistrationService generates classes from descriptors</li>
 * <li>Descriptors marked as loaded via {@code setLoaded(true)}</li>
 * <li>{@code instances()} returns all descriptors for runtime queries</li>
 * </ol>
 * 
 * <p><b>Thread-Safety WARNING:</b></p>
 * <p>
 * Static registry shared across threads. NOT thread-safe during registration phase. External synchronization
 * required when calling {@code create()} concurrently. Read-only access via {@code instances()}/
 * {@code loadableInstances()} is thread-safe AFTER registration complete. Concurrent registration can cause
 * race conditions and duplicate descriptors.
 * </p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * DynamicEntityDescriptorFactory.create("UserProfile", "user_profiles", fields, null);
 * List<DynamicEntityDescriptor> unloaded = DynamicEntityDescriptorFactory.loadableInstances();
 * }</pre>
 * 
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see DynamicEntityDescriptor
 * @see com.openkoda.service.dynamicentity.DynamicEntityRegistrationService
 * @see com.openkoda.core.form.FrontendMappingFieldDefinition
 */
public class DynamicEntityDescriptorFactory {

    private static final Map<String /* formName */, DynamicEntityDescriptor> map = new HashMap<>();

    /**
     * Returns descriptors ready for Byte Buddy class generation that have not yet been loaded.
     * <p>
     * Filters descriptors where {@code isLoaded()} flag is false. Used by DynamicEntityRegistrationService
     * during {@code buildAndLoadDynamicClasses()} to identify which descriptors need JPA entity class generation
     * and registration. After class generation completes, descriptors are marked as loaded to prevent duplicate
     * processing.
     * </p>
     * <p><b>Thread-Safety:</b> Thread-safe for reads after registration phase complete.</p>
     * 
     * @return Filtered list of descriptors where {@code isLoadable() == true} (not yet loaded into JPA context)
     * @see DynamicEntityDescriptor#isLoadable()
     * @see com.openkoda.service.dynamicentity.DynamicEntityRegistrationService#buildAndLoadDynamicClasses()
     */
    public static List<DynamicEntityDescriptor> loadableInstances(){
        return map.values().stream().filter(DynamicEntityDescriptor::isLoadable).collect(Collectors.toList());
    }

    /**
     * Returns all registered descriptors regardless of load state.
     * <p>
     * Creates defensive copy as ArrayList to prevent external modification of internal registry. Includes both
     * loaded and unloaded descriptors. Used for runtime queries when complete descriptor inventory is needed,
     * such as administrative interfaces or validation processes.
     * </p>
     * <p><b>Thread-Safety:</b> Thread-safe for reads after registration phase complete.</p>
     * 
     * @return ArrayList copy of all registered descriptors (defensive copy prevents registry modification)
     * @see #loadableInstances()
     */
    public static List<DynamicEntityDescriptor> instances(){
        return new ArrayList<>(map.values());
    }

    /**
     * Creates new DynamicEntityDescriptor and registers in static map keyed by entity key.
     * <p>
     * Converts form name to entity and repository names using NameHelper conventions. Stores descriptor
     * keyed by {@code entityKey} for subsequent retrieval via {@link #getInstanceByEntityKey(String)}.
     * The descriptor encapsulates all metadata needed for Byte Buddy runtime class generation including
     * table name, field definitions with JPA annotations, and optional timestamp suffix for class versioning.
     * </p>
     * <p><b>Thread-Safety WARNING:</b> Not thread-safe. Callers must synchronize when calling concurrently
     * during application startup to prevent race conditions and duplicate descriptor registration.</p>
     * 
     * @param formName Form name from database, converted to entity class name via {@code NameHelper.toEntityClassName()}
     * @param tableName Database table name for the dynamic entity (maps to JPA {@code @Table} annotation)
     * @param fields Collection of field definitions with types, constraints, and JPA mapping metadata
     * @param timeMillis Optional timestamp suffix for class versioning, null for no suffix (used to generate unique class names)
     * @see com.openkoda.core.helper.NameHelper#toEntityClassName(String)
     * @see com.openkoda.core.helper.NameHelper#toEntityKey(String)
     * @see com.openkoda.core.helper.NameHelper#toRepositoryName(String)
     * @see DynamicEntityDescriptor
     */
    public static void create(String formName, String tableName, Collection<FrontendMappingFieldDefinition> fields, Long timeMillis){
        DynamicEntityDescriptor ded = new DynamicEntityDescriptor(toEntityClassName(formName), tableName, toEntityKey(formName), toRepositoryName(formName), fields, timeMillis);
        map.put(ded.getEntityKey(), ded);
    }

    /**
     * Direct registry lookup by entity key for descriptor retrieval.
     * <p>
     * Used for many_to_one field reference resolution during Byte Buddy class generation when dynamic entities
     * reference other dynamic entities. Entity key is computed from form name using {@code NameHelper.toEntityKey()}.
     * Returns null if no descriptor registered for the specified key.
     * </p>
     * <p><b>Thread-Safety:</b> Thread-safe for reads after registration phase complete.</p>
     * 
     * @param entityKey Entity key computed from form name via {@code NameHelper.toEntityKey()}
     * @return Descriptor for the entity key, or null if not registered in the static map
     * @see com.openkoda.core.helper.NameHelper#toEntityKey(String)
     * @see DynamicEntityDescriptor#getEntityKey()
     */
    public static DynamicEntityDescriptor getInstanceByEntityKey(String entityKey) {
        return map.get(entityKey);
    }
}
