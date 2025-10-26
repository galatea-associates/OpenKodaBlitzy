package com.openkoda.core.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple JavaBean-style holder for arbitrary key/value pairs in the cache model.
 * <p>
 * This class provides direct access to an internal Map without defensive copying.
 * Callers receive and modify the actual backing Map instance, allowing full mutation access.
 * This design supports efficient data sharing but requires careful coordination in concurrent environments.
 * </p>
 * <p>
 * The default backing Map is a standard HashMap with no internal synchronization.
 * Callers can supply alternative Map implementations via {@link #setModel(Map)} to change
 * concurrency or ordering behavior. For example, use ConcurrentHashMap for thread-safe operations
 * or LinkedHashMap to maintain insertion order.
 * </p>
 * <p>
 * Thread-safety: This class does not provide internal synchronization. Callers must coordinate
 * concurrent access if the ModelCache instance is shared across threads.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class ModelCache {
    /**
     * Backing map storing arbitrary key/value pairs.
     * <p>
     * Initialized to a HashMap with no synchronization. The {@link #getModel()} method
     * returns a direct reference to this map, allowing callers full mutation access without
     * defensive copying. Alternative Map implementations can be provided via {@link #setModel(Map)}.
     * </p>
     */
    private Map<String, Object> model = new HashMap<>();
    
    /**
     * Returns a direct reference to the internal backing map.
     * <p>
     * This method provides direct access to the internal map without creating a defensive copy.
     * Callers can freely add, remove, or modify entries in the returned map, and these changes
     * directly affect the ModelCache instance. This design supports efficient data sharing and
     * mutation but requires careful coordination in concurrent environments.
     * </p>
     *
     * @return direct reference to the internal map storing key/value pairs, never null
     */
    public Map<String, Object> getModel() {
        return model;
    }
    
    /**
     * Replaces the backing map with the provided Map instance.
     * <p>
     * This method allows callers to supply alternative Map implementations to change
     * concurrency or ordering behavior. For example, provide a ConcurrentHashMap for
     * thread-safe operations, a LinkedHashMap to maintain insertion order, or a TreeMap
     * for sorted key access. The provided map becomes the new backing storage for this
     * ModelCache instance.
     * </p>
     *
     * @param model the new map to use as backing storage; implementations like ConcurrentHashMap,
     *              LinkedHashMap, or TreeMap can be provided to control concurrency and ordering
     */
    public void setModel(Map<String, Object> model) {
        this.model = model;
    }
}