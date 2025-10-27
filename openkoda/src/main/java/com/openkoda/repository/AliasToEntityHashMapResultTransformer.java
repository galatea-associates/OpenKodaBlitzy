package com.openkoda.repository;

import org.hibernate.query.TypedTupleTransformer;
import org.hibernate.transform.ResultTransformer;

import java.util.LinkedHashMap;

/**
 * Thread-safe singleton result transformer converting Hibernate query tuples into insertion-order LinkedHashMap instances.
 * <p>
 * This class implements both Hibernate {@link TypedTupleTransformer} and legacy {@link ResultTransformer} interfaces
 * to provide compatibility across Hibernate versions. It converts tuple/native query rows into 
 * {@code LinkedHashMap<String, Object>} instances where column aliases become map keys and query result values
 * become map values.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>Skips null aliases to avoid polluting result maps with unnamed columns</li>
 *   <li>Pre-sizes maps to tuple length for optimal memory allocation and performance</li>
 *   <li>Thread-safe through immutable singleton pattern with serialization hook</li>
 *   <li>Preserves insertion order via LinkedHashMap for consistent column ordering</li>
 * </ul>
 * </p>
 * <p>
 * Primary usage location is {@link NativeQueries#runReadOnly(String)} which applies this transformer
 * to native SQL queries for flexible result mapping without entity class requirements.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * NativeQueryImpl query = session.createNativeQuery("SELECT id, name FROM users");
 * query.setResultTransformer(AliasToEntityHashMapResultTransformer.INSTANCE);
 * List&lt;LinkedHashMap&lt;String, Object&gt;&gt; results = query.list();
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see NativeQueries
 * @see LinkedHashMap
 * @see ResultTransformer
 * @see TypedTupleTransformer
 */
public class AliasToEntityHashMapResultTransformer implements ResultTransformer<LinkedHashMap<String,Object>>, TypedTupleTransformer<LinkedHashMap<String,Object>> {

    /**
     * Singleton instance for use across all query transformations.
     * <p>
     * Thread-safe singleton providing centralized access to the transformer.
     * Use this instance instead of creating new instances to minimize memory overhead.
     * Serialization preserves singleton uniqueness via {@link #readResolve()} hook.
     * </p>
     */
    public static final AliasToEntityHashMapResultTransformer INSTANCE = new AliasToEntityHashMapResultTransformer();

    /**
     * Disallow instantiation of AliasToEntityMapResultTransformer.
     * <p>
     * Private constructor enforces singleton pattern. Use {@link #INSTANCE} instead.
     * </p>
     */
    private AliasToEntityHashMapResultTransformer() {
    }

    /**
     * Returns the transformed result type for Hibernate type introspection.
     * <p>
     * This method informs Hibernate that transformation produces {@link LinkedHashMap} instances.
     * Used internally by Hibernate to validate result type compatibility and optimize query execution.
     * </p>
     *
     * @return {@code LinkedHashMap.class} indicating result type is LinkedHashMap
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class getTransformedType() {
        return LinkedHashMap.class;
    }

    /**
     * Transforms a query result tuple into a LinkedHashMap with column aliases as keys.
     * <p>
     * Converts a single row from a Hibernate query result into a map where each column alias
     * maps to its corresponding value from the tuple. Null aliases are skipped to avoid
     * polluting the result map with unnamed or computed columns without aliases.
     * </p>
     * <p>
     * The returned LinkedHashMap preserves insertion order, ensuring columns appear in the
     * same sequence as specified in the original query SELECT clause. Map is pre-sized to
     * tuple length for optimal performance.
     * </p>
     *
     * @param tuple query result row values in column order, must not be null
     * @param aliases column names or aliases corresponding to tuple values, must not be null, may contain null elements
     * @return populated LinkedHashMap with alias keys mapped to tuple values, entries with null aliases omitted
     */
    @Override
    public LinkedHashMap<String,Object> transformTuple(Object[] tuple, String[] aliases) {
        LinkedHashMap<String,Object> result = new LinkedHashMap<>( tuple.length );
        for ( int i = 0; i < tuple.length; i++ ) {
            String alias = aliases[i];
            if ( alias != null ) {
                result.put( alias, tuple[i] );
            }
        }
        return result;
    }

    /**
     * Serialization hook ensuring singleton pattern preservation across deserialization.
     * <p>
     * Invoked automatically during deserialization to replace the deserialized instance
     * with the canonical singleton {@link #INSTANCE}. This guarantees that only one instance
     * exists in the JVM even after serialization and deserialization cycles, maintaining
     * singleton semantics and enabling identity comparison via {@code ==} operator.
     * </p>
     *
     * @return the singleton instance {@link #INSTANCE}, never a new instance
     */
    private Object readResolve() {
        return INSTANCE;
    }
}
